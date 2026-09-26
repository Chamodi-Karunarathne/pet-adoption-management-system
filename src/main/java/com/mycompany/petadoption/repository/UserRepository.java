package com.mycompany.petadoption.repository;

import static com.mycompany.petadoption.repository.Sql.*;

import com.mycompany.petadoption.model.Models.*;
import java.sql.*;
import java.util.*;

public final class UserRepository {
  private User map(ResultSet r) throws SQLException {
    return new User(
        r.getLong("id"),
        r.getString("name"),
        r.getString("email"),
        r.getString("phone"),
        Role.valueOf(r.getString("role")),
        r.getBoolean("active"));
  }

  public User find(Connection c, long id) throws SQLException {
    return list(c, "SELECT * FROM users WHERE id=?", this::map, id).stream()
        .findFirst()
        .orElseThrow(() -> new SecurityException("Please sign in again."));
  }

  public Credential credential(Connection c, String email) throws SQLException {
    return list(
            c,
            "SELECT * FROM users WHERE email=? FOR UPDATE",
            r ->
                new Credential(
                    map(r),
                    r.getString("password_hash"),
                    r.getInt("failed_attempts"),
                    r.getTimestamp("locked_until") == null
                        ? null
                        : r.getTimestamp("locked_until").toInstant()),
            email)
        .stream()
        .findFirst()
        .orElse(null);
  }

  public long create(Connection c, String name, String email, String phone, String hash, Role role)
      throws SQLException {
    return id(
        c,
        "INSERT INTO users(name,email,phone,password_hash,role) VALUES (?,?,?,?,?) RETURNING id",
        name,
        email,
        phone,
        hash,
        role.name());
  }

  public void failedLogin(Connection c, long id) throws SQLException {
    update(
        c,
        "UPDATE users SET failed_attempts=failed_attempts+1,locked_until=CASE WHEN"
            + " failed_attempts>=4 THEN now()+interval '5 minutes' ELSE NULL END WHERE id=?",
        id);
  }

  public void successfulLogin(Connection c, long id) throws SQLException {
    update(c, "UPDATE users SET failed_attempts=0,locked_until=NULL WHERE id=?", id);
  }

  public void profile(Connection c, long id, String name, String phone) throws SQLException {
    update(c, "UPDATE users SET name=?,phone=? WHERE id=?", name, phone, id);
  }

  public void password(Connection c, long id, String hash) throws SQLException {
    update(c, "UPDATE users SET password_hash=? WHERE id=?", hash, id);
  }

  public List<User> all(Connection c) throws SQLException {
    return list(c, "SELECT * FROM users ORDER BY name", this::map);
  }

  public void access(Connection c, long id, Role role, boolean active) throws SQLException {
    update(c, "UPDATE users SET role=?,active=? WHERE id=?", role.name(), active, id);
  }

  public void lockUsers(Connection c) throws SQLException {
    list(c, "SELECT pg_advisory_xact_lock(83192012)", r -> 0);
  }

  public long count(Connection c) throws SQLException {
    return id(c, "SELECT count(*) FROM users");
  }
}
