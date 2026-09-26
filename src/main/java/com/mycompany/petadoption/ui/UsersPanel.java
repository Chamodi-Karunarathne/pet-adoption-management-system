package com.mycompany.petadoption.ui;

import com.mycompany.petadoption.model.Models.*;
import com.mycompany.petadoption.service.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

public final class UsersPanel extends JPanel {
  private final AdminService service;
  private final Session session;
  private List<User> users = List.of();
  private JTable table;
  private final JPanel content = Theme.panel(new BorderLayout());
  private final JTextField search = Theme.field("Search people");

  public UsersPanel(AdminService service, Session session) {
    this.service = service;
    this.session = session;
    setLayout(new BorderLayout(0, 18));
    setOpaque(false);
    add(
        Theme.flow(
            search,
            Theme.button("Search", this::filter),
            Theme.primary("+ Create account", this::create),
            Theme.button("Refresh", this::reload)),
        BorderLayout.NORTH);
    add(content);
    add(
        Theme.flow(Theme.button("Manage access", () -> Ui.action(this, this::access))),
        BorderLayout.SOUTH);
    search.addActionListener(e -> filter());
    SwingUtilities.invokeLater(this::reload);
  }

  private void reload() {
    Ui.work(
        this,
        null,
        () -> service.users(session),
        list -> {
          users = list;
          var rows = new ArrayList<List<Object>>();
          for (User u : list)
            rows.add(
                List.of(
                    u.name(), u.email(), u.phone(), u.role(), u.active() ? "Active" : "Inactive"));
          table = Ui.table(new Report(List.of("Name", "Email", "Phone", "Role", "Access"), rows));
          content.removeAll();
          content.add(Theme.scroll(table));
          filter();
          content.revalidate();
          content.repaint();
        });
  }

  private void filter() {
    if (table != null) {
      @SuppressWarnings("unchecked")
      var sorter =
          (javax.swing.table.TableRowSorter<javax.swing.table.DefaultTableModel>)
              table.getRowSorter();
      sorter.setRowFilter(
          RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(search.getText())));
    }
  }

  private void create() {
    JTextField name = Theme.field("Full name"),
        email = Theme.field("Email"),
        phone = Theme.field("Phone");
    JPasswordField password = new JPasswordField(24);
    JComboBox<Role> role = new JComboBox<>(Role.values());
    role.setSelectedItem(Role.USER);
    JButton save = Theme.primary("Create account", () -> {});
    JDialog d =
        Ui.dialog(
            this,
            "Create account",
            Theme.column(
                Theme.fieldRow("Full name", name),
                Theme.fieldRow("Email", email),
                Theme.fieldRow("Phone", phone),
                Theme.fieldRow("Password (12–128 characters)", password),
                Theme.fieldRow("Role", role),
                save));
    save.addActionListener(
        e -> {
          String n = name.getText(), em = email.getText(), ph = phone.getText();
          char[] pw = password.getPassword();
          Role r = (Role) role.getSelectedItem();
          Ui.work(
              d,
              save,
              () -> {
                service.createUser(session, n, em, ph, pw, r);
                return true;
              },
              v -> {
                d.dispose();
                reload();
              });
          password.setText("");
        });
    d.setVisible(true);
  }

  private void access() {
    if (table == null || table.getSelectedRow() < 0)
      throw new IllegalArgumentException("Select an account first.");
    User u = users.get(table.convertRowIndexToModel(table.getSelectedRow()));
    JComboBox<Role> role = new JComboBox<>(Role.values());
    role.setSelectedItem(u.role());
    JCheckBox active = new JCheckBox("Account is active", u.active());
    JButton save = Theme.primary("Save access", () -> {});
    JDialog d =
        Ui.dialog(
            this,
            "Manage " + u.name(),
            Theme.column(
                Theme.title(u.name(), 25),
                Theme.muted(u.email()),
                Theme.fieldRow("Role", role),
                active,
                Theme.muted("Your own access cannot be changed here."),
                save));
    save.addActionListener(
        e -> {
          Role r = (Role) role.getSelectedItem();
          boolean a = active.isSelected();
          if (Ui.confirm(d, "Save these access changes for " + u.name() + "?"))
            Ui.work(
                d,
                save,
                () -> {
                  service.access(session, u.id(), r, a);
                  return true;
                },
                v -> {
                  d.dispose();
                  reload();
                });
        });
    d.setVisible(true);
  }
}
