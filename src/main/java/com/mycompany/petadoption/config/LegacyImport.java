package com.mycompany.petadoption.config;

import com.mycompany.petadoption.repository.Sql;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.sql.*;
import java.util.*;

/** Lossless staging only: legacy quantities do not imply identifiable individual animals. */
public final class LegacyImport {
  private LegacyImport() {}

  public static int log(Database db, Path path, boolean dryRun) throws Exception {
    List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
    if (dryRun) return lines.size();
    return db.transaction(
        c -> {
          int count = 0;
          for (int i = 0; i < lines.size(); i++)
            count += stage(c, "adoption-log", digest(i + ":" + lines.get(i)), lines.get(i));
          return count;
        });
  }

  public static Map<String, Integer> derby(Database db, boolean dryRun) throws Exception {
    String url = System.getenv("LEGACY_DB_URL");
    if (url == null || !url.startsWith("jdbc:derby:"))
      throw new IllegalArgumentException(
          "Set LEGACY_DB_URL, LEGACY_DB_USER and LEGACY_DB_PASSWORD. Add the Derby JDBC driver to"
              + " the migration classpath.");
    Map<String, Integer> counts = new LinkedHashMap<>();
    try (Connection source =
        DriverManager.getConnection(
            url, System.getenv("LEGACY_DB_USER"), System.getenv("LEGACY_DB_PASSWORD"))) {
      source.setReadOnly(true);
      source.setAutoCommit(false);
      source.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
      db.transaction(
          target -> {
            for (String table : List.of("CUSTOMERS", "STAFF", "PET", "STOCKS")) {
              int count = 0;
              // Identifiers are a fixed allowlist, never user input.
              try (var statement = source.prepareStatement("SELECT * FROM " + table);
                  var rows = statement.executeQuery()) {
                var meta = rows.getMetaData();
                Map<String, Integer> duplicates = new HashMap<>();
                while (rows.next()) {
                  StringBuilder json = new StringBuilder("{");
                  for (int i = 1; i <= meta.getColumnCount(); i++) {
                    if (i > 1) json.append(',');
                    json.append(quote(meta.getColumnName(i))).append(':');
                    String value = rows.getString(i);
                    json.append(value == null ? "null" : quote(value));
                  }
                  json.append('}');
                  String payload = json.toString();
                  String hash = digest(payload);
                  int occurrence = duplicates.merge(hash, 1, Integer::sum);
                  if (dryRun) count++;
                  else count += stage(target, "derby-" + table, hash + "-" + occurrence, payload);
                }
              }
              counts.put(table, count);
            }
            return null;
          });
      source.rollback();
    }
    return counts;
  }

  private static int stage(Connection c, String source, String key, String payload)
      throws SQLException {
    return Sql.update(
        c,
        "INSERT INTO legacy_records(source,source_key,payload) VALUES (?,?,?) ON"
            + " CONFLICT(source,source_key) DO NOTHING",
        source,
        key,
        payload);
  }

  private static String digest(String text) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  private static String quote(String value) {
    StringBuilder out = new StringBuilder("\"");
    for (char ch : value.toCharArray())
      switch (ch) {
        case '"' -> out.append("\\\"");
        case '\\' -> out.append("\\\\");
        case '\n' -> out.append("\\n");
        case '\r' -> out.append("\\r");
        case '\t' -> out.append("\\t");
        default -> {
          if (ch < 32) out.append(String.format("\\u%04x", (int) ch));
          else out.append(ch);
        }
      }
    return out.append('"').toString();
  }
}
