package com.mycompany.petadoption.repository;

import static com.mycompany.petadoption.repository.Sql.*;

import com.mycompany.petadoption.model.Models.*;
import java.sql.*;
import java.util.*;

public final class InventoryRepository {
  private Stock map(ResultSet r) throws SQLException {
    return new Stock(
        r.getLong("id"),
        r.getString("name"),
        r.getLong("category_id"),
        r.getString("category"),
        r.getString("unit"),
        r.getInt("quantity"),
        r.getInt("reorder_level"));
  }

  public List<Stock> all(Connection c) throws SQLException {
    return list(
        c,
        "SELECT i.*,c.name category FROM inventory i JOIN inventory_categories c ON"
            + " c.id=i.category_id ORDER BY i.name",
        this::map);
  }

  public Stock lock(Connection c, long id) throws SQLException {
    return list(
            c,
            "SELECT i.*,c.name category FROM inventory i JOIN inventory_categories c ON"
                + " c.id=i.category_id WHERE i.id=? FOR UPDATE OF i",
            this::map,
            id)
        .stream()
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Item not found."));
  }

  public List<Choice> categories(Connection c) throws SQLException {
    return list(
        c,
        "SELECT * FROM inventory_categories ORDER BY name",
        r -> new Choice(r.getLong("id"), r.getString("name")));
  }

  public void category(Connection c, String name) throws SQLException {
    update(c, "INSERT INTO inventory_categories(name) VALUES (?)", name);
  }

  public long create(Connection c, String name, long category, String unit, int level)
      throws SQLException {
    return id(
        c,
        "INSERT INTO inventory(name,category_id,unit,reorder_level) VALUES (?,?,?,?) RETURNING id",
        name,
        category,
        unit,
        level);
  }

  public void adjust(Connection c, Stock stock, int delta, int balance, long actor, String reason)
      throws SQLException {
    update(c, "UPDATE inventory SET quantity=? WHERE id=?", balance, stock.id());
    update(
        c,
        "INSERT INTO inventory_transactions(item_id,actor_id,delta,balance,reason) VALUES"
            + " (?,?,?,?,?)",
        stock.id(),
        actor,
        delta,
        balance,
        reason);
  }
}
