package com.mycompany.petadoption.ui;

import com.mycompany.petadoption.service.*;
import java.awt.*;
import java.util.function.Consumer;
import javax.swing.*;

public final class AuthPanel extends JPanel {
  private final AuthService auth;
  private final Consumer<Session> onLogin;
  private boolean register;

  public AuthPanel(AuthService auth, Consumer<Session> onLogin) {
    this.auth = auth;
    this.onLogin = onLogin;
    setLayout(new BorderLayout());
    build();
  }

  private void build() {
    removeAll();
    JPanel story = Theme.panel(new BorderLayout(0, 20));
    story.setBackground(Theme.GREEN);
    story.setOpaque(true);
    story.setBorder(BorderFactory.createEmptyBorder(36, 36, 36, 36));
    JLabel brand = Theme.title("woof.", 42);
    brand.setForeground(Color.WHITE);
    story.add(brand, BorderLayout.NORTH);
    story.add(new PhotoPanel("/images/welcome.jpg", 430, 430));
    JLabel tagline = Theme.title("Every companion deserves a home.", 20);
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
            Theme.title(register ? Strings.get("register") : Strings.get("login"), 30),
            Theme.muted(
                register
                    ? "Start your adoption journey."
                    : "Welcome back. Your next chapter starts here."));
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
        Theme.primary(register ? Strings.get("register") : Strings.get("login"), () -> {});
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
                          register = false;
                          build();
                          Ui.message(this, "Your account is ready. Sign in to continue.");
                        });
                  } else Ui.work(this, submit, () -> auth.login(em, pw), onLogin);
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
    JPanel box =
        Theme.column(
            fields,
            inputs,
            submit,
            switchMode,
            Theme.fieldRow("Language / Langue", language),
            Theme.muted("Adopter and administrator accounts sign in here."));
    form.add(box);
    JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, story, Theme.scroll(form));
    split.setResizeWeight(.46);
    split.setDividerSize(0);
    split.setBorder(null);
    add(split);
    revalidate();
    repaint();
  }
}
