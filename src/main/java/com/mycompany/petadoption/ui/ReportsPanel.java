package com.mycompany.petadoption.ui;

import com.mycompany.petadoption.model.Models.Report;
import com.mycompany.petadoption.repository.ReportRepository.Type;
import com.mycompany.petadoption.service.*;
import com.mycompany.petadoption.util.Csv;
import java.awt.*;
import java.text.MessageFormat;
import javax.swing.*;

public final class ReportsPanel extends JPanel {
  private final AdminService service;
  private final Session session;
  private final JComboBox<Type> type = new JComboBox<>(Type.values());
  private final JPanel content = Theme.panel(new BorderLayout());
  private Report current;
  private JTable table;
  private long generation;
  private String generatedTitle;

  public ReportsPanel(AdminService service, Session session) {
    this.service = service;
    this.session = session;
    setLayout(new BorderLayout(0, 18));
    setOpaque(false);
    add(
        Theme.flow(
            type,
            Theme.primary("Generate report", this::load),
            Theme.button(
                "Export CSV",
                () ->
                    Ui.action(
                        this,
                        () -> {
                          if (current == null)
                            throw new IllegalArgumentException("Generate a report first.");
                          export(this, current);
                        })),
            Theme.button(
                "Print / Save PDF",
                () ->
                    Ui.action(
                        this,
                        () -> {
                          if (table == null)
                            throw new IllegalArgumentException("Generate a report first.");
                          print(this, table, generatedTitle);
                        }))),
        BorderLayout.NORTH);
    add(content);
    content.add(
        Theme.muted("Choose a report and generate a live database snapshot."), BorderLayout.NORTH);
  }

  private void load() {
    Type t = (Type) type.getSelectedItem();
    long version = ++generation;
    Ui.work(
        this,
        null,
        () -> service.report(session, t),
        r -> {
          if (version != generation) return;
          current = r;
          generatedTitle = t.toString();
          table = Ui.table(r);
          content.removeAll();
          content.add(Theme.scroll(table));
          content.add(
              Theme.muted(
                  r.rows().size()
                      + " records · Generated "
                      + java.time.LocalDateTime.now().withNano(0)),
              BorderLayout.SOUTH);
          content.revalidate();
          content.repaint();
        });
  }

  static void preview(Component owner, String title, Report report) {
    Report display = report;
    if (title.equals("Adoption receipt") && report.rows().size() == 1) {
      java.util.List<java.util.List<Object>> fields = new java.util.ArrayList<>();
      for (int i = 0; i < report.columns().size(); i++) {
        fields.add(java.util.List.of(report.columns().get(i), report.rows().getFirst().get(i)));
      }
      display = new Report(java.util.List.of("Adoption details", "Value"), fields);
    }
    JTable table = Ui.table(display);
    JPanel content = Theme.panel(new BorderLayout(0, 16));
    JScrollPane scroll = Theme.scroll(table);
    scroll.setPreferredSize(new Dimension(800, 370));
    content.add(scroll);
    content.add(
        Theme.flow(
            Theme.button("Export CSV", () -> export(owner, report)),
            Theme.button("Print / Save PDF", () -> print(owner, table, title))),
        BorderLayout.SOUTH);
    Ui.dialog(owner, title, content).setVisible(true);
  }

  private static void export(Component owner, Report report) {
    JFileChooser chooser = new JFileChooser();
    chooser.setSelectedFile(new java.io.File("woof-report.csv"));
    if (chooser.showSaveDialog(owner) != JFileChooser.APPROVE_OPTION) return;
    var path = chooser.getSelectedFile().toPath();
    if (java.nio.file.Files.exists(path) && !Ui.confirm(owner, "Replace the existing file?"))
      return;
    Ui.work(
        owner,
        null,
        () -> {
          Csv.write(path, report);
          return true;
        },
        v -> Ui.message(owner, "Report exported."));
  }

  private static void print(Component owner, JTable table, String title) {
    try {
      table.print(
          JTable.PrintMode.FIT_WIDTH,
          new MessageFormat("Woof — " + title),
          new MessageFormat("Page {0}"),
          true,
          null,
          true);
    } catch (java.awt.print.PrinterException e) {
      Ui.error(owner, e);
    }
  }
}
