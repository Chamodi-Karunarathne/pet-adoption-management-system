package com.mycompany.petadoption.ui;

import com.mycompany.petadoption.model.Models.*;
import com.mycompany.petadoption.model.Models.Choice;
import com.mycompany.petadoption.repository.ReportRepository;
import com.mycompany.petadoption.service.*;
import com.mycompany.petadoption.util.Validation;
import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

public final class InventoryPanel extends JPanel {
  private final AdminService service;
  private final Session session;
  private final JPanel content = Theme.panel(new BorderLayout());
  private List<Stock> stocks = List.of();
  private List<Choice> categories = List.of();
  private JTable table;
  private final JTextField search = Theme.field("Search items or categories");

  public InventoryPanel(AdminService service, Session session) {
    this.service = service;
    this.session = session;
    setLayout(new BorderLayout(0, 18));
    setOpaque(false);
    add(
        Theme.column(
            Theme.muted("Supplies and resources. Every quantity change is recorded in the ledger."),
            Theme.flow(
                search,
                Theme.button("Search", this::filter),
                Theme.primary("+ Add item", this::addItem),
                Theme.button("+ Category", this::category),
                Theme.button("Refresh", this::reload))),
        BorderLayout.NORTH);
    add(content);
    add(
        Theme.flow(
            Theme.primary("Adjust stock", () -> Ui.action(this, this::adjust)),
            Theme.button(
                "Movement history",
                () ->
                    Ui.work(
                        this,
                        null,
                        () -> service.report(session, ReportRepository.Type.STOCK_HISTORY),
                        r -> ReportsPanel.preview(this, "Inventory movement history", r)))),
        BorderLayout.SOUTH);
    search.addActionListener(e -> filter());
    SwingUtilities.invokeLater(this::reload);
  }

  private void reload() {
    Ui.work(
        this,
        null,
        () -> new Object[] {service.inventory(session), service.categories(session)},
        data -> {
          @SuppressWarnings("unchecked")
          List<Stock> s = (List<Stock>) data[0];
          @SuppressWarnings("unchecked")
          List<Choice> c = (List<Choice>) data[1];
          stocks = s;
          categories = c;
          var rows = new ArrayList<List<Object>>();
          for (var item : s)
            rows.add(
                List.of(
                    item.name(),
                    item.category(),
                    item.quantity(),
                    item.unit(),
                    item.reorderLevel(),
                    item.quantity() <= item.reorderLevel() ? "LOW STOCK" : "Healthy"));
          table =
              Ui.table(
                  new Report(
                      List.of(
                          "Item", "Category", "Available", "Unit", "Reorder at", "Stock status"),
                      rows));
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

  private void addItem() {
    JTextField name = Theme.field("Item name"),
        unit = Theme.field("kg, bags, pieces…"),
        level = Theme.field("0");
    JComboBox<Choice> category = new JComboBox<>(categories.toArray(Choice[]::new));
    JButton save = Theme.primary("Create item", () -> {});
    JDialog d =
        Ui.dialog(
            this,
            "New inventory item",
            Theme.column(
                Theme.fieldRow("Name", name),
                Theme.fieldRow("Category", category),
                Theme.fieldRow("Unit", unit),
                Theme.fieldRow("Reorder level", level),
                Theme.muted("New items start at zero. Record opening stock as an adjustment."),
                save));
    save.addActionListener(
        e ->
            Ui.action(
                d,
                () -> {
                  Choice c = (Choice) category.getSelectedItem();
                  if (c == null)
                    throw new IllegalArgumentException("Add an inventory category first.");
                  String n = name.getText(), u = unit.getText();
                  int l =
                      Validation.integer(level.getText(), "Reorder level", 0, Integer.MAX_VALUE);
                  Ui.work(
                      d,
                      save,
                      () -> {
                        service.item(session, n, c.id(), u, l);
                        return true;
                      },
                      v -> {
                        d.dispose();
                        reload();
                      });
                }));
    d.setVisible(true);
  }

  private void category() {
    String name = JOptionPane.showInputDialog(this, "New inventory category");
    if (name != null)
      Ui.work(
          this,
          null,
          () -> {
            service.category(session, name);
            return true;
          },
          v -> reload());
  }

  private void adjust() {
    if (table == null || table.getSelectedRow() < 0)
      throw new IllegalArgumentException("Select an inventory item first.");
    Stock item = stocks.get(table.convertRowIndexToModel(table.getSelectedRow()));
    JTextField delta = Theme.field("Positive to receive, negative to use"),
        reason = Theme.field("Reason for this change");
    JButton save = Theme.primary("Record stock change", () -> {});
    JDialog d =
        Ui.dialog(
            this,
            "Adjust " + item.name(),
            Theme.column(
                Theme.title(item.name(), 24),
                Theme.muted("Available: " + item.quantity() + " " + item.unit()),
                Theme.fieldRow("Quantity change (+ / −)", delta),
                Theme.fieldRow("Reason", reason),
                save));
    save.addActionListener(
        e ->
            Ui.action(
                d,
                () -> {
                  int value =
                      Validation.integer(
                          delta.getText(),
                          "Quantity change",
                          -Integer.MAX_VALUE,
                          Integer.MAX_VALUE);
                  String r = reason.getText();
                  Ui.work(
                      d,
                      save,
                      () -> {
                        service.adjust(session, item.id(), value, r);
                        return true;
                      },
                      v -> {
                        d.dispose();
                        reload();
                      });
                }));
    d.setVisible(true);
  }
}
