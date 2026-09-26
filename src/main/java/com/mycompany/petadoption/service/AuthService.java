package com.mycompany.petadoption.service;

import com.mycompany.petadoption.config.Database;
import com.mycompany.petadoption.model.Models.*;
import com.mycompany.petadoption.repository.UserRepository;
import com.mycompany.petadoption.util.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class AuthService {
  private final Database db;
  private final UserRepository users = new UserRepository();
  private final Map<Session, Instant> sessions = new ConcurrentHashMap<>();
  private final String dummy = Passwords.hash("not-a-real-account-password".toCharArray());

  public AuthService(Database db) {
    this.db = db;
  }

  public Session login(String email, char[] password) throws SQLException {
    try {
      String normalized = Validation.email(email);
      User user =
          db.transaction(
              c -> {
                Credential credential = users.credential(c, normalized);
                if (credential == null) {
                  Passwords.verify(password, dummy);
                  return null;
                }
                boolean valid = Passwords.verify(password, credential.hash());
                if (!credential.user().active()
                    || (credential.lockedUntil() != null
                        && credential.lockedUntil().isAfter(Instant.now()))) return null;
                if (!valid) {
                  users.failedLogin(c, credential.user().id());
                  return null;
                }
                users.successfulLogin(c, credential.user().id());
                return credential.user();
              });
      if (user == null)
        throw new IllegalArgumentException(
            "Sign-in failed. Check your email and password, or wait five minutes if attempts were"
                + " exceeded.");
      Session session = new Session(user.id());
      sessions.put(session, Instant.now().plusSeconds(8 * 3600));
      return session;
    } finally {
      Arrays.fill(password, '\0');
    }
  }

  public void register(String name, String email, String phone, char[] password)
      throws SQLException {
    try {
      String n = Validation.text(name, "Name", 100),
          e = Validation.email(email),
          p = Validation.phone(phone);
      String hash = Passwords.hash(password);
      db.transaction(
          c -> {
            users.create(c, n, e, p, hash, Role.USER);
            return null;
          });
    } finally {
      Arrays.fill(password, '\0');
    }
  }

  public User require(Connection c, Session session, Role role) throws SQLException {
    Instant expiry = session == null ? null : sessions.get(session);
    if (expiry == null || expiry.isBefore(Instant.now())) {
      if (session != null) sessions.remove(session);
      throw new SecurityException("Your session has ended. Please sign in again.");
    }
    User user = users.find(c, session.userId);
    if (!user.active() || (role != null && user.role() != role))
      throw new SecurityException("You do not have access to this action.");
    return user;
  }

  public User current(Session s) throws SQLException {
    return db.transaction(c -> require(c, s, null));
  }

  public void logout(Session s) {
    if (s != null) sessions.remove(s);
  }

  public void profile(Session s, String name, String phone) throws SQLException {
    String n = Validation.text(name, "Name", 100), p = Validation.phone(phone);
    db.transaction(
        c -> {
          User u = require(c, s, null);
          users.profile(c, u.id(), n, p);
          return null;
        });
  }

  public void password(Session s, char[] oldPassword, char[] next) throws SQLException {
    try {
      String hash = Passwords.hash(next);
      db.transaction(
          c -> {
            User u = require(c, s, null);
            Credential cr = users.credential(c, u.email());
            if (!Passwords.verify(oldPassword, cr.hash()))
              throw new IllegalArgumentException("Your current password is incorrect.");
            users.password(c, u.id(), hash);
            return null;
          });
      sessions.keySet().removeIf(other -> other.userId == s.userId && other != s);
    } finally {
      Arrays.fill(oldPassword, '\0');
      Arrays.fill(next, '\0');
    }
  }

  /** Local operator-only bootstrap command; unavailable through registration or UI. */
  public void bootstrap(String name, String email, String phone, char[] password)
      throws SQLException {
    String n = Validation.text(name, "Name", 100),
        e = Validation.email(email),
        p = Validation.phone(phone);
    try {
      String hash = Passwords.hash(password);
      db.transaction(
          c -> {
            users.lockUsers(c);
            if (users.count(c) != 0)
              throw new IllegalArgumentException(
                  "Bootstrap is only available for an empty users table.");
            users.create(c, n, e, p, hash, Role.ADMIN);
            return null;
          });
    } finally {
      Arrays.fill(password, '\0');
    }
  }
}
