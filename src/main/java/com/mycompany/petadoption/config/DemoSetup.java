package com.mycompany.petadoption.config;

import com.mycompany.petadoption.model.Models.Role;
import com.mycompany.petadoption.repository.*;
import com.mycompany.petadoption.util.Passwords;
import java.sql.SQLException;
import java.util.Arrays;

/** Explicit opt-in demo data. Never called during normal startup. */
public final class DemoSetup {
  private DemoSetup() {}

  public static void install(Database db, char[] password) throws SQLException {
    try {
      String adminHash = Passwords.hash(password), userHash = Passwords.hash(password);
      db.transaction(
          c -> {
            Sql.list(c, "SELECT pg_advisory_xact_lock(83192012)", r -> 0);
            var users = new UserRepository();
            if (users.count(c) > 0 || Sql.id(c, "SELECT count(*) FROM pets") > 0)
              throw new IllegalArgumentException(
                  "Demo installation requires an empty database. Existing data is never"
                      + " overwritten.");
            Database.script(c, "/db/demo.sql");
            long admin =
                users.create(
                    c,
                    "Shelter Administrator",
                    "admin@woof.example",
                    "+94 770000001",
                    adminHash,
                    Role.ADMIN);
            users.create(
                c, "Demo Adopter", "adopter@woof.example", "+94 770000002", userHash, Role.USER);
            var inventory = new InventoryRepository();
            for (var item : inventory.all(c))
              inventory.adjust(c, item, 20, 20, admin, "Demo opening stock");
            return null;
          });
    } finally {
      Arrays.fill(password, '\0');
    }
  }
}
