package com.mycompany.petadoption.repository;

import java.sql.*;
import java.util.*;

/** Small JDBC helper: every statement/result is scoped, all values are bound parameters. */
public final class Sql {
  private Sql() {}

  @FunctionalInterface
  public interface Mapper<T> {
    T map(ResultSet r) throws SQLException;
  }

  public static <T> List<T> list(Connection c, String sql, Mapper<T> mapper, Object... args)
      throws SQLException {
    try (var s = c.prepareStatement(sql)) {
      bind(s, args);
      s.setQueryTimeout(15);
      try (var r = s.executeQuery()) {
        var values = new ArrayList<T>();
        while (r.next()) values.add(mapper.map(r));
        return values;
      }
    }
  }

  public static int update(Connection c, String sql, Object... args) throws SQLException {
    try (var s = c.prepareStatement(sql)) {
      bind(s, args);
      s.setQueryTimeout(15);
      return s.executeUpdate();
    }
  }

  public static long id(Connection c, String sql, Object... args) throws SQLException {
    return list(c, sql, r -> r.getLong(1), args).getFirst();
  }

  private static void bind(PreparedStatement s, Object[] args) throws SQLException {
    for (int i = 0; i < args.length; i++) s.setObject(i + 1, args[i]);
  }
}
