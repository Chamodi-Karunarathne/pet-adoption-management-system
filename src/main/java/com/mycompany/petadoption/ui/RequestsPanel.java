package com.mycompany.petadoption.ui;

import com.mycompany.petadoption.model.Models.*;
import com.mycompany.petadoption.service.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

public final class RequestsPanel extends JPanel {
  private final AdoptionService service;
  private final AdminService reports;
  private final Session session;
  private final boolean admin;
  private final JComboBox<String> status =
      new JComboBox<>(
          new String[] {"All statuses", "PENDING", "APPROVED", "REJECTED", "WITHDRAWN"});
  private final JPanel content = Theme.panel(new BorderLayout());
  private List<AdoptionRequest> requests = List.of();
  private JTable table;
  private long generation;

  public RequestsPanel(
      AdoptionService service, AdminService reports, Session session, boolean admin) {
    this.service = service;
    this.reports = reports;
    this.session = session;
    this.admin = admin;
    setLayout(new BorderLayout(0, 18));
    setOpaque(false);
    add(Theme.flow(status, Theme.button("Refresh", this::reload)), BorderLayout.NORTH);
    add(content);
    JPanel actions =
        Theme.flow(
            Theme.primary(
                admin ? "Review application" : "View application",
                () -> Ui.action(this, () -> details(selected()))));
    if (!admin)
      actions.add(
          Theme.button(
              "Withdraw request",
              () ->
                  Ui.action(
                      this,
                      () -> {
                        AdoptionRequest a = selected();
                        if (Ui.confirm(this, "Withdraw your application for " + a.pet() + "?"))
                          Ui.work(
                              this,
                              null,
                              () -> {
                                service.withdraw(session, a.id());
                                return true;
                              },
                              v -> reload());
                      })));
    actions.add(
        Theme.button(
            "Adoption receipt",
            () ->
                Ui.action(
                    this,
                    () -> {
                      var a = selected();
                      Ui.work(
                          this,
                          null,
                          () -> reports.receipt(session, a.id()),
                          r -> ReportsPanel.preview(this, "Adoption receipt", r));
                    })));
    add(actions, BorderLayout.SOUTH);
    status.addActionListener(e -> reload());
    SwingUtilities.invokeLater(this::reload);
  }

  private AdoptionRequest selected() {
    if (table == null || table.getSelectedRow() < 0)
      throw new IllegalArgumentException("Select an application first.");
    return requests.get(table.convertRowIndexToModel(table.getSelectedRow()));
  }

  private void reload() {
    String filter = status.getSelectedIndex() == 0 ? null : (String) status.getSelectedItem();
    long version = ++generation;
    Ui.work(
        this,
        null,
        () -> service.list(session, filter),
        rows -> {
          if (version != generation) return;
          requests = rows;
          var data = new ArrayList<List<Object>>();
          for (var a : rows)
            data.add(
                List.of(
                    a.pet(), a.applicant(), a.status(), a.created().replace('T', ' '), a.note()));
          table =
              Ui.table(
                  new Report(
                      List.of("Pet", "Applicant", "Status", "Submitted", "Decision note"), data));
          content.removeAll();
          content.add(Theme.scroll(table));
          if (rows.isEmpty())
            content.add(Theme.muted("No applications match this filter."), BorderLayout.NORTH);
          content.revalidate();
          content.repaint();
        });
  }

  private void details(AdoptionRequest a) {
    JTextArea text = Theme.area(13);
    text.setEditable(false);
    text.setText(
        "Pet: "
            + a.pet()
            + " ("
            + a.species()
            + ")\nApplicant: "
            + a.applicant()
            + "\nContact: "
            + a.email()
            + "\nStatus: "
            + a.status()
            + "\n\nAbout the applicant\n"
            + a.motivation()
            + "\n\nHousing & other animals\n"
            + a.housing()
            + "\n\nDecision\n"
            + a.note());
    JTextArea note = Theme.area(3);
    JButton approve = Theme.primary("Approve adoption", () -> {}),
        reject = Theme.button("Reject application", () -> {});
    JPanel review = Theme.column(Theme.title("A home for " + a.pet(), 25), Theme.scroll(text));
    if (admin && a.status() == ApplicationStatus.PENDING)
      review =
          Theme.column(
              review,
              Theme.fieldRow("Decision note (visible to applicant)", Theme.scroll(note)),
              Theme.flow(approve, reject));
    JDialog d = Ui.dialog(this, "Application details", Theme.scroll(review));
    approve.addActionListener(
        e -> {
          if (Ui.confirm(
              d,
              "Approve this adoption? The pet will be marked adopted and other pending applications"
                  + " for this pet will be closed.")) decide(d, approve, a, true, note.getText());
        });
    reject.addActionListener(
        e -> {
          if (Ui.confirm(d, "Reject this application with the entered note?"))
            decide(d, reject, a, false, note.getText());
        });
    d.setVisible(true);
  }

  private void decide(JDialog d, JButton button, AdoptionRequest a, boolean approve, String note) {
    Ui.work(
        d,
        button,
        () -> {
          service.decide(session, a.id(), approve, note);
          return true;
        },
        v -> {
          d.dispose();
          reload();
        });
  }
}
