package com.mycompany.petadoption.ui;

import com.mycompany.petadoption.model.Models.Report;
import java.awt.*;
import java.sql.SQLException;
import java.util.concurrent.*;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public final class Ui {
  private Ui() {}

  public static <T> void work(Component owner, JButton button, Callable<T> task, Consumer<T> done) {
    if (button != null) button.setEnabled(false);
    owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    new SwingWorker<T, Void>() {
      @Override
      protected T doInBackground() throws Exception {
        return task.call();
      }

      @Override
      protected void done() {
        owner.setCursor(Cursor.getDefaultCursor());
        if (button != null) button.setEnabled(true);
        try {
          T value = get();
          if (owner.isDisplayable()) done.accept(value);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
          error(owner, e.getCause());
        } catch (RuntimeException e) {
          error(owner, e);
        }
      }
    }.execute();
  }

  public static void error(Component owner, Throwable e) {
    String message;
    if (e instanceof IllegalArgumentException || e instanceof SecurityException)
      message = e.getMessage();
    else if (e instanceof SQLException sql) {
      message =
          switch (sql.getSQLState() == null ? "" : sql.getSQLState()) {
            case "23505" ->
                "This record already exists, or you already have an active request. Refresh and"
                    + " check your entries.";
            case "23503" -> "A selected record changed or is still in use. Refresh and try again.";
            case "23514", "22001", "22003" ->
                "One or more values are outside the allowed limits. Check your entries.";
            default ->
                "The database request could not be completed. Check the connection and try again.";
          };
      System.err.println("Database operation failed (SQL state " + sql.getSQLState() + ").");
    } else {
      message = "The action could not be completed. Please try again.";
      System.err.println("Operation failed: " + e.getClass().getSimpleName());
    }
    JOptionPane.showMessageDialog(owner, message, "Please check", JOptionPane.WARNING_MESSAGE);
  }

  public static void action(Component owner, Runnable action) {
    try {
      action.run();
    } catch (RuntimeException e) {
      error(owner, e);
    }
  }

  public static boolean confirm(Component owner, String text) {
    return JOptionPane.showConfirmDialog(
            owner,
            text,
            "Confirm action",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE)
        == JOptionPane.OK_OPTION;
  }

  public static JTable table(Report report) {
    var model =
        new DefaultTableModel(report.columns().toArray(), 0) {
          @Override
          public boolean isCellEditable(int r, int c) {
            return false;
          }
        };
    report.rows().forEach(row -> model.addRow(row.toArray()));
    JTable t = new JTable(model);
    t.setDefaultRenderer(
        Object.class,
        new javax.swing.table.DefaultTableCellRenderer() {
          @Override
          public Component getTableCellRendererComponent(
              JTable table, Object value, boolean selected, boolean focused, int row, int column) {
            putClientProperty("html.disable", true);
            Component result =
                super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            if (!selected)
              setForeground(
                  switch (String.valueOf(value)) {
                    case "AVAILABLE", "APPROVED", "Healthy", "Active" -> Theme.SUCCESS;
                    case "PENDING", "ON_HOLD", "LOW STOCK" -> Theme.WARNING;
                    case "REJECTED", "Inactive" -> Theme.ERROR;
                    default -> Theme.INK;
                  });
            setToolTipText(value == null ? null : value.toString());
            return result;
          }
        });
    t.setAutoCreateRowSorter(true);
    t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    t.setRowHeight(38);
    t.getTableHeader().setReorderingAllowed(false);
    return t;
  }

  public static JDialog dialog(Component owner, String title, Component content) {
    JDialog d =
        new JDialog(
            SwingUtilities.getWindowAncestor(owner), title, Dialog.ModalityType.APPLICATION_MODAL);
    d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    JPanel p = Theme.panel(new BorderLayout());
    p.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
    p.add(content);
    d.setContentPane(p);
    d.pack();
    Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
    d.setSize(
        Math.min(Math.max(540, d.getWidth()), screen.width - 100),
        Math.min(d.getHeight(), screen.height - 100));
    d.setLocationRelativeTo(owner);
    return d;
  }

  public static void message(Component owner, String text) {
    JOptionPane.showMessageDialog(owner, text, "Woof", JOptionPane.INFORMATION_MESSAGE);
  }
}
