package com.mycompany.petadoption.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.*;

public final class Database {
  private final AppConfig config;

  public Database(AppConfig config) {
    this.config = config;
  }

  public Connection open() throws SQLException {
    if (!config.url().startsWith("jdbc:postgresql:"))
      throw new SQLException("PostgreSQL configuration required.");
    var p = new java.util.Properties();
    p.setProperty("user", config.user());
    p.setProperty("password", config.password());
    p.setProperty("connectTimeout", "5");
    p.setProperty("socketTimeout", "20");
    p.setProperty("ApplicationName", "Woof Pet Adoption");
    return DriverManager.getConnection(config.url(), p);
  }

  @FunctionalInterface
  public interface Work<T> {
    T run(Connection c) throws SQLException;
  }

  public <T> T transaction(Work<T> work) throws SQLException {
    try (Connection c = open()) {
      c.setAutoCommit(false);
      try {
        T result = work.run(c);
        c.commit();
        return result;
      } catch (SQLException | RuntimeException e) {
        c.rollback();
        throw e;
      }
    }
  }

  public void check() throws SQLException {
    try (Connection c = open();
        var s = c.prepareStatement("SELECT version FROM schema_version WHERE version=1");
        var r = s.executeQuery()) {
      if (!r.next()) throw new SQLException("Schema migration required.");
    }
  }

  public void migrate() throws SQLException {
    transaction(
        c -> {
          try (var lock = c.prepareStatement("SELECT pg_advisory_xact_lock(83192011)")) {
            lock.execute();
          }
          boolean exists;
          try (var s = c.prepareStatement("SELECT to_regclass('schema_version')");
              var r = s.executeQuery()) {
            r.next();
            exists = r.getString(1) != null;
          }
          if (!exists) script(c, "/db/V001__schema.sql");
          return null;
        });
    check();
  }

  public static void script(Connection c, String resource) throws SQLException {
    try (var in = Database.class.getResourceAsStream(resource)) {
      if (in == null) throw new IOException("Missing migration resource");
      String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
      try (var s = c.prepareStatement(sql)) {
        s.execute();
      }
    } catch (IOException e) {
      throw new SQLException("Cannot load database script", e);
    }
  }
}
