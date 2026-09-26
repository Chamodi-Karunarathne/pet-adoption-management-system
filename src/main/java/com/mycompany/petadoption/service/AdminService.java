package com.mycompany.petadoption.service;

import com.mycompany.petadoption.config.Database;
import com.mycompany.petadoption.model.Models.*;
import com.mycompany.petadoption.repository.*;
import com.mycompany.petadoption.util.*;
import java.sql.SQLException;
import java.util.*;

public final class AdminService {
  private final Database db;
  private final AuthService auth;
  private final InventoryRepository inventory = new InventoryRepository();
  private final UserRepository users = new UserRepository();
  private final ReportRepository reports = new ReportRepository();

  public AdminService(Database db, AuthService auth) {
    this.db = db;
    this.auth = auth;
  }

  public List<Stock> inventory(Session s) throws SQLException {
    return db.transaction(
        c -> {
          auth.require(c, s, Role.ADMIN);
          return inventory.all(c);
        });
  }

  public List<Choice> categories(Session s) throws SQLException {
    return db.transaction(
        c -> {
          auth.require(c, s, Role.ADMIN);
          return inventory.categories(c);
        });
  }

  public void category(Session s, String name) throws SQLException {
    String n = Validation.text(name, "Category", 80);
    db.transaction(
        c -> {
          auth.require(c, s, Role.ADMIN);
          inventory.category(c, n);
          return null;
        });
  }

  public void item(Session s, String name, long category, String unit, int reorder)
      throws SQLException {
    String n = Validation.text(name, "Item name", 120), u = Validation.text(unit, "Unit", 30);
    if (reorder < 0) throw new IllegalArgumentException("Reorder level cannot be negative.");
    db.transaction(
        c -> {
          auth.require(c, s, Role.ADMIN);
          inventory.create(c, n, category, u, reorder);
          return null;
        });
  }

  public void adjust(Session s, long item, int delta, String reason) throws SQLException {
    String r = Validation.text(reason, "Stock change reason", 500);
    if (delta == 0) throw new IllegalArgumentException("Quantity change cannot be zero.");
    db.transaction(
        c -> {
          User u = auth.require(c, s, Role.ADMIN);
          Stock stock = inventory.lock(c, item);
          long balance = (long) stock.quantity() + delta;
          if (balance < 0 || balance > Integer.MAX_VALUE)
            throw new IllegalArgumentException(
                "This adjustment exceeds the available quantity or supported maximum.");
          inventory.adjust(c, stock, delta, (int) balance, u.id(), r);
          return null;
        });
  }

  public List<User> users(Session s) throws SQLException {
    return db.transaction(
        c -> {
          auth.require(c, s, Role.ADMIN);
          return users.all(c);
        });
  }

  public void createUser(
      Session s, String name, String email, String phone, char[] password, Role role)
      throws SQLException {
    String n = Validation.text(name, "Name", 100),
        e = Validation.email(email),
        p = Validation.phone(phone);
    try {
      String hash = Passwords.hash(password);
      db.transaction(
          c -> {
            auth.require(c, s, Role.ADMIN);
            users.create(c, n, e, p, hash, Objects.requireNonNull(role));
            return null;
          });
    } finally {
      Arrays.fill(password, '\0');
    }
  }

  public void access(Session s, long id, Role role, boolean active) throws SQLException {
    db.transaction(
        c -> {
          users.lockUsers(c);
          User admin = auth.require(c, s, Role.ADMIN);
          if (admin.id() == id)
            throw new IllegalArgumentException(
                "You cannot change your own role or deactivate your own account.");
          users.find(c, id);
          users.access(c, id, Objects.requireNonNull(role), active);
          return null;
        });
  }

  public Report report(Session s, ReportRepository.Type type) throws SQLException {
    return db.transaction(
        c -> {
          auth.require(c, s, Role.ADMIN);
          return reports.report(c, type);
        });
  }

  public Map<String, Long> dashboard(Session s) throws SQLException {
    return db.transaction(
        c -> {
          User u = auth.require(c, s, null);
          return reports.dashboard(c, u.role() == Role.ADMIN ? null : u.id());
        });
  }

  public Report receipt(Session s, long application) throws SQLException {
    return db.transaction(
        c -> {
          User u = auth.require(c, s, null);
          Report r = reports.receipt(c, application, u.id(), u.role() == Role.ADMIN);
          if (r.rows().isEmpty())
            throw new IllegalArgumentException(
                "A completed adoption receipt is not available for this account.");
          return r;
        });
  }
}
