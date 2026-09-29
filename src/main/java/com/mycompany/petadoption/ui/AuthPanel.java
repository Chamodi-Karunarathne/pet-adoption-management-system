package com.mycompany.petadoption.ui;

import com.mycompany.petadoption.model.Models.Role;
import com.mycompany.petadoption.service.*;
import java.awt.*;
import java.util.function.Consumer;
import javax.swing.*;

public final class AuthPanel extends JPanel {
  private final AuthService auth;
  private final Consumer<Session> onLogin;
  private boolean register;
  private Role role = Role.USER;
  private long generation;

  public AuthPanel(AuthService auth, Consumer<Session> onLogin) {
    this.auth = auth;
    this.onLogin = onLogin;
    setLayout(new BorderLayout());
    build();
  }

  private void build() {
    long view = ++generation;
    Role selectedRole = role;
    removeAll();
    JPanel story = Theme.panel(new BorderLayout(0, 20));
    story.setBackground(Theme.GREEN);
    story.setOpaque(true);
    story.setBorder(BorderFactory.createEmptyBorder(36, 36, 36, 36));
    JLabel brand = Theme.title("woof.", 42);
    brand.setForeground(Color.WHITE);
    story.add(brand, BorderLayout.NORTH);
    story.add(new PhotoPanel("/images/welcome.jpg", 430, 430));
    JLabel tagline =
        Theme.title(Strings.get(role == Role.ADMIN ? "admin.tagline" : "user.tagline"), 20);
    tagline.setForeground(Color.WHITE);
    story.add(tagline, BorderLayout.SOUTH);
    JPanel form = Theme.panel(new GridBagLayout());
    form.setBorder(BorderFactory.createEmptyBorder(28, 40, 28, 40));
    JTextField name = Theme.field("Your name"),
        email = Theme.field("you@example.com"),
        phone = Theme.field("+94 ...");
    JPasswordField password = new JPasswordField(24);
    password.setMargin(new Insets(9, 10, 9, 10));
    JPasswordField confirm = new JPasswordField(24);
    confirm.setMargin(new Insets(9, 10, 9, 10));
    JPanel fields =
        Theme.column(
            Theme.title(
                register
                    ? Strings.get("register")
                    : Strings.get(role == Role.ADMIN ? "admin.heading" : "user.heading"),
                30),
            Theme.muted(
                register
                    ? "Start your adoption journey."
                    : Strings.get(role == Role.ADMIN ? "admin.subtitle" : "user.subtitle")));
    JPanel inputs =
        register
            ? Theme.column(
                Theme.fieldRow(Strings.get("name"), name),
                Theme.fieldRow(Strings.get("email"), email),
                Theme.fieldRow(Strings.get("phone"), phone),
                Theme.fieldRow(Strings.get("password") + " (12–128 characters)", password),
                Theme.fieldRow("Confirm password", confirm))
            : Theme.column(
                Theme.fieldRow(Strings.get("email"), email),
                Theme.fieldRow(Strings.get("password"), password));
    JButton submit =
        Theme.primary(
            register
                ? Strings.get("register")
                : Strings.get(role == Role.ADMIN ? "admin.submit" : "user.submit"),
            () -> {});
    submit.addActionListener(
        e ->
            Ui.action(
                this,
                () -> {
                  String n = name.getText(), em = email.getText(), ph = phone.getText();
                  char[] pw = password.getPassword();
                  if (register) {
                    char[] confirmation = confirm.getPassword();
                    boolean same = java.util.Arrays.equals(pw, confirmation);
                    java.util.Arrays.fill(confirmation, '\0');
                    if (!same) {
                      java.util.Arrays.fill(pw, '\0');
                      throw new IllegalArgumentException("Passwords do not match.");
                    }
                    Ui.work(
                        this,
                        submit,
                        () -> {
                          auth.register(n, em, ph, pw);
                          return true;
                        },
                        v -> {
                          if (view != generation) return;
                          register = false;
                          build();
                          Ui.message(this, "Your account is ready. Sign in to continue.");
                        });
                  } else
                    Ui.work(
                        this,
                        submit,
                        () -> auth.login(em, pw, selectedRole),
                        session -> {
                          if (view != generation) auth.logout(session);
                          else onLogin.accept(session);
                        });
                  password.setText("");
                  confirm.setText("");
                }));
    password.addActionListener(e -> submit.doClick());
    confirm.addActionListener(e -> submit.doClick());
    JButton switchMode =
        Theme.button(
            register ? "Already a member? Sign in" : "New here? Create an account",
            () -> {
              register = !register;
              build();
            });
    JComboBox<String> language = new JComboBox<>(new String[] {"English", "Français"});
    language.setSelectedIndex(Strings.get("login").equals("Sign in") ? 0 : 1);
    language.addActionListener(
        e -> {
          Strings.french(language.getSelectedIndex() == 1);
          build();
        });
    JButton userAccess = Theme.button(Strings.get("user.access"), () -> selectRole(Role.USER));
    JButton adminAccess = Theme.button(Strings.get("admin.access"), () -> selectRole(Role.ADMIN));
    for (JButton access : new JButton[] {userAccess, adminAccess}) {
      boolean selected = access == (role == Role.ADMIN ? adminAccess : userAccess);
      access.setBackground(selected ? Theme.GREEN : Theme.BACKGROUND);
      access.setForeground(selected ? Color.WHITE : Theme.INK);
      access
          .getAccessibleContext()
          .setAccessibleDescription(selected ? "Selected sign-in role" : "Switch sign-in role");
    }
    JPanel accessOptions = Theme.panel(new GridLayout(1, 2, 12, 0));
    accessOptions.add(userAccess);
    accessOptions.add(adminAccess);
    JPanel box =
        Theme.column(
            accessOptions,
            fields,
            inputs,
            submit,
            role == Role.USER ? switchMode : Theme.muted(Strings.get("admin.provisioning")),
            Theme.fieldRow("Language / Langue", language),
            Theme.muted(Strings.get(role == Role.ADMIN ? "admin.scope" : "user.scope")));
    form.add(box);
    JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, story, Theme.scroll(form));
    split.setResizeWeight(.46);
    split.setDividerSize(0);
    split.setBorder(null);
    add(split);
    revalidate();
    repaint();
  }

  private void selectRole(Role next) {
    role = next;
    register = false;
    build();
  }
}
