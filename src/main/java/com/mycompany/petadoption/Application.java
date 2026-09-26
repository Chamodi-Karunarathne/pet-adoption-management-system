package com.mycompany.petadoption;

import com.mycompany.petadoption.config.*;
import com.mycompany.petadoption.service.AuthService;
import com.mycompany.petadoption.ui.*;
import java.nio.file.Path;
import javax.swing.*;

public final class Application {
  private Application() {}

  public static void main(String[] args) {
    AppConfig config = AppConfig.load();
    Database db = new Database(config);
    if (args.length > 0) {
      try {
        switch (args[0]) {
          case "--migrate" -> {
            db.migrate();
            System.out.println("PostgreSQL schema is ready (version 1).");
          }
          case "--demo" -> {
            db.check();
            String password = required("PET_DEMO_PASSWORD");
            DemoSetup.install(db, password.toCharArray());
            System.out.println(
                "Demo installed. Accounts: admin@woof.example and adopter@woof.example. Password"
                    + " supplied through PET_DEMO_PASSWORD.");
          }
          case "--bootstrap-admin" -> {
            db.check();
            new AuthService(db)
                .bootstrap(
                    required("PET_ADMIN_NAME"),
                    required("PET_ADMIN_EMAIL"),
                    required("PET_ADMIN_PHONE"),
                    required("PET_ADMIN_PASSWORD").toCharArray());
            System.out.println("Administrator created.");
          }
          case "--import-log", "--check-log" -> {
            if (args.length != 2)
              throw new IllegalArgumentException("Supply the adoption log path.");
            System.out.println(
                "Legacy log records: "
                    + LegacyImport.log(db, Path.of(args[1]), args[0].equals("--check-log")));
          }
          case "--import-derby", "--check-derby" ->
              System.out.println(LegacyImport.derby(db, args[0].equals("--check-derby")));
          default ->
              throw new IllegalArgumentException(
                  "Commands: --migrate, --demo, --bootstrap-admin, --check-log PATH, --import-log"
                      + " PATH, --check-derby, --import-derby");
        }
      } catch (Exception e) {
        System.err.println(
            e instanceof IllegalArgumentException
                ? e.getMessage()
                : "Operation failed ("
                    + e.getClass().getSimpleName()
                    + "). Check database configuration, schema and migration instructions.");
        System.exit(1);
      }
      return;
    }
    Theme.install();
    SwingUtilities.invokeLater(
        () -> {
          JFrame loading = new JFrame("woof. — Starting");
          loading.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
          JPanel content =
              Theme.column(Theme.title("woof.", 40), Theme.muted("Connecting to your shelter…"));
          content.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
          loading.setContentPane(content);
          loading.pack();
          loading.setLocationRelativeTo(null);
          loading.setVisible(true);
          new SwingWorker<Boolean, Void>() {
            protected Boolean doInBackground() throws Exception {
              db.check();
              return true;
            }

            protected void done() {
              try {
                get();
                MainFrame frame = new MainFrame(db, config);
                loading.dispose();
                frame.setVisible(true);
              } catch (Exception e) {
                content.removeAll();
                content.setLayout(new java.awt.BorderLayout(16, 16));
                JTextArea help =
                    new JTextArea(
                        "The shelter database is unavailable.\n\n"
                            + "Set PET_DB_URL, PET_DB_USER and PET_DB_PASSWORD\n"
                            + "or copy config/application.example.properties to\n"
                            + "config/application.properties.\n\n"
                            + "Run --migrate once with the database owner account.\n"
                            + "See README.md for local review setup.");
                help.setEditable(false);
                content.add(help);
                content.add(Theme.button("Close", loading::dispose), java.awt.BorderLayout.SOUTH);
                loading.pack();
                loading.setLocationRelativeTo(null);
              }
            }
          }.execute();
        });
  }

  private static String required(String name) {
    String v = System.getenv(name);
    if (v == null || v.isBlank())
      throw new IllegalArgumentException("Set " + name + " before running this command.");
    return v;
  }
}
