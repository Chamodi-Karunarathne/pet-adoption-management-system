package com.mycompany.petadoption.repository;

import com.mycompany.petadoption.model.Models.Report;
import java.sql.*;
import java.util.*;

public final class ReportRepository {
  public enum Type {
    AVAILABLE_PETS("Available pets"),
    APPLICATIONS("Adoption applications"),
    ADOPTIONS("Completed adoptions"),
    DISTRIBUTION("Pets by species and status"),
    INVENTORY("Inventory"),
    STOCK_HISTORY("Inventory movements"),
    USERS("Account summary"),
    LEGACY("Legacy records awaiting reconciliation");
    private final String label;

    Type(String label) {
      this.label = label;
    }

    @Override
    public String toString() {
      return label;
    }
  }

  public Report report(Connection c, Type type) throws SQLException {
    String query =
        switch (type) {
          case AVAILABLE_PETS ->
              "SELECT p.name,s.name AS species,b.name AS breed,p.birth_date,p.sex,p.fee FROM pets p"
                  + " JOIN breeds b ON b.id=p.breed_id JOIN species s ON s.id=b.species_id WHERE"
                  + " p.status='AVAILABLE' ORDER BY p.name";
          case APPLICATIONS ->
              "SELECT a.id,u.name AS applicant,p.name AS pet,a.status,a.created_at,a.decision_note"
                  + " FROM adoption_applications a JOIN users u ON u.id=a.user_id JOIN pets p ON"
                  + " p.id=a.pet_id ORDER BY a.created_at DESC";
          case ADOPTIONS ->
              "SELECT d.id,u.name AS adopter,u.phone,p.name AS pet,b.name AS"
                  + " breed,d.fee,d.adopted_at FROM adoptions d JOIN adoption_applications a ON"
                  + " a.id=d.application_id JOIN users u ON u.id=a.user_id JOIN pets p ON"
                  + " p.id=d.pet_id JOIN breeds b ON b.id=p.breed_id ORDER BY d.adopted_at DESC";
          case DISTRIBUTION ->
              "SELECT s.name AS species,p.status,count(*) AS total FROM pets p JOIN breeds b ON"
                  + " b.id=p.breed_id JOIN species s ON s.id=b.species_id GROUP BY s.name,p.status"
                  + " ORDER BY s.name,p.status";
          case INVENTORY ->
              "SELECT i.name,c.name AS category,i.unit,i.quantity,i.reorder_level FROM inventory i"
                  + " JOIN inventory_categories c ON c.id=i.category_id ORDER BY i.name";
          case STOCK_HISTORY ->
              "SELECT t.created_at,i.name,u.name AS changed_by,t.delta,t.balance,t.reason FROM"
                  + " inventory_transactions t JOIN inventory i ON i.id=t.item_id JOIN users u ON"
                  + " u.id=t.actor_id ORDER BY t.created_at DESC";
          case USERS ->
              "SELECT role,active,count(*) AS total FROM users GROUP BY role,active ORDER BY role";
          case LEGACY ->
              "SELECT source,source_key,payload,imported_at FROM legacy_records ORDER BY source,id";
        };
    return query(c, query);
  }

  public Report query(Connection c, String query, Object... args) throws SQLException {
    try (var s = c.prepareStatement(query)) {
      s.setQueryTimeout(15);
      for (int i = 0; i < args.length; i++) s.setObject(i + 1, args[i]);
      try (var r = s.executeQuery()) {
        var columns = new ArrayList<String>();
        var rows = new ArrayList<List<Object>>();
        var meta = r.getMetaData();
        for (int i = 1; i <= meta.getColumnCount(); i++)
          columns.add(meta.getColumnLabel(i).replace('_', ' '));
        while (r.next()) {
          var row = new ArrayList<Object>();
          for (int i = 1; i <= columns.size(); i++) row.add(Objects.toString(r.getObject(i), ""));
          rows.add(row);
        }
        return new Report(columns, rows);
      }
    }
  }

  public Map<String, Long> dashboard(Connection c, Long user) throws SQLException {
    Map<String, Long> values = new LinkedHashMap<>();
    if (user == null) {
      values.put(
          "Pets in care",
          Sql.id(c, "SELECT count(*) FROM pets WHERE status IN ('AVAILABLE','ON_HOLD')"));
      values.put("Available pets", Sql.id(c, "SELECT count(*) FROM pets WHERE status='AVAILABLE'"));
      values.put(
          "Pending requests",
          Sql.id(c, "SELECT count(*) FROM adoption_applications WHERE status='PENDING'"));
      values.put("Completed adoptions", Sql.id(c, "SELECT count(*) FROM adoptions"));
      values.put(
          "Low stock items",
          Sql.id(c, "SELECT count(*) FROM inventory WHERE quantity<=reorder_level"));
    } else {
      values.put(
          "Available companions", Sql.id(c, "SELECT count(*) FROM pets WHERE status='AVAILABLE'"));
      values.put(
          "My pending requests",
          Sql.id(
              c,
              "SELECT count(*) FROM adoption_applications WHERE user_id=? AND status='PENDING'",
              user));
      values.put(
          "My adoptions",
          Sql.id(
              c,
              "SELECT count(*) FROM adoption_applications WHERE user_id=? AND status='APPROVED'",
              user));
    }
    return values;
  }

  public Report receipt(Connection c, long application, long user, boolean admin)
      throws SQLException {
    return query(
        c,
        "SELECT d.id AS receipt,u.name AS adopter,u.phone,p.name AS pet,s.name AS species,b.name AS"
            + " breed,p.birth_date,d.fee,d.adopted_at FROM adoptions d JOIN adoption_applications a"
            + " ON a.id=d.application_id JOIN users u ON u.id=a.user_id JOIN pets p ON"
            + " p.id=d.pet_id JOIN breeds b ON b.id=p.breed_id JOIN species s ON s.id=b.species_id"
            + " WHERE a.id=? AND (a.user_id=? OR ?)",
        application,
        user,
        admin);
  }
}
