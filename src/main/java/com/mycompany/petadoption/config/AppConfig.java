package com.mycompany.petadoption.config;

import java.io.IOException;
import java.nio.file.*;
import java.util.Properties;

public record AppConfig(String url, String user, String password, String currency) {
  @Override
  public String toString() {
    return "AppConfig[database configuration redacted, currency=" + currency + "]";
  }

  public static AppConfig load() {
    Properties p = new Properties();
    Path file = Path.of(System.getProperty("pet.config", "config/application.properties"));
    if (Files.exists(file))
      try (var in = Files.newInputStream(file)) {
        p.load(in);
      } catch (IOException e) {
        throw new IllegalStateException("Cannot read application configuration.", e);
      }
    return new AppConfig(
        value(p, "db.url", "PET_DB_URL"),
        value(p, "db.user", "PET_DB_USER"),
        value(p, "db.password", "PET_DB_PASSWORD"),
        p.getProperty("app.currency", "LKR"));
  }

  private static String value(Properties p, String key, String env) {
    return System.getenv().getOrDefault(env, p.getProperty(key, ""));
  }
}
