package com.mycompany.petadoption.ui;

import com.mycompany.petadoption.model.Models.User;
import com.mycompany.petadoption.service.*;
import java.awt.*;
import javax.swing.*;

public final class ProfilePanel extends JPanel {
  public ProfilePanel(AuthService auth, Session session, User user, Runnable refreshed) {
    setLayout(new BorderLayout());
    setOpaque(false);
    JTextField name = Theme.field("Name"), phone = Theme.field("Phone");
    name.setText(user.name());
    phone.setText(user.phone());
    JButton save = Theme.primary("Save profile", () -> {});
    JPasswordField current = new JPasswordField(24),
        next = new JPasswordField(24),
        confirm = new JPasswordField(24);
    JButton password = Theme.button("Change password", () -> {});
    JPanel card = Theme.card();
    card.add(
        Theme.column(
            Theme.title("Your details", 26),
            Theme.muted(user.email() + " · " + user.role()),
            Theme.fieldRow("Full name", name),
            Theme.fieldRow("Phone number", phone),
            save,
            new JSeparator(),
            Theme.title("Password & security", 21),
            Theme.fieldRow("Current password", current),
            Theme.fieldRow("New password (12–128 characters)", next),
            Theme.fieldRow("Confirm new password", confirm),
            password));
    JPanel wrapper = Theme.panel(new BorderLayout());
    wrapper.add(card, BorderLayout.NORTH);
    add(Theme.scroll(wrapper));
    save.addActionListener(
        e -> {
          String n = name.getText(), p = phone.getText();
          Ui.work(
              this,
              save,
              () -> {
                auth.profile(session, n, p);
                return true;
              },
              v -> {
                refreshed.run();
                Ui.message(this, "Profile updated.");
              });
        });
    password.addActionListener(
        e ->
            Ui.action(
                this,
                () -> {
                  char[] old = current.getPassword(),
                      pw = next.getPassword(),
                      confirmation = confirm.getPassword();
                  boolean same = java.util.Arrays.equals(pw, confirmation);
                  java.util.Arrays.fill(confirmation, '\0');
                  if (!same) {
                    java.util.Arrays.fill(old, '\0');
                    java.util.Arrays.fill(pw, '\0');
                    throw new IllegalArgumentException("New passwords do not match.");
                  }
                  Ui.work(
                      this,
                      password,
                      () -> {
                        auth.password(session, old, pw);
                        return true;
                      },
                      v ->
                          Ui.message(
                              this, "Password updated. Other sessions have been signed out."));
                  current.setText("");
                  next.setText("");
                  confirm.setText("");
                }));
  }
}
