package com.mycompany.petadoption;

import static org.junit.jupiter.api.Assertions.*;

import com.mycompany.petadoption.config.*;
import com.mycompany.petadoption.ui.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;
import javax.imageio.ImageIO;
import javax.swing.*;

/** Opt-in real Swing smoke checks. Invoked by the PostgreSQL suite, never production startup. */
final class GuiSmoke {
  private GuiSmoke() {}

  static void run(Database db, String password) throws Exception {
    assertFalse(GraphicsEnvironment.isHeadless());
    MainFrame frame =
        edt(
            () -> {
              Theme.install();
              MainFrame f = new MainFrame(db, AppConfig.load());
              f.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
              f.setVisible(true);
              return f;
            });
    try {
      capture(frame, "01-login");
      click(frame, "New here? Create an account");
      await(() -> has(frame, "Create account"));
      capture(frame, "02-register");
      click(frame, "Already a member? Sign in");
      login(frame, "admin@test.example", password);
      await(() -> has(frame, "Pet management"));
      await(() -> labels(frame).contains("2"));
      capture(frame, "03-admin-dashboard");
      click(frame, "Pet management");
      await(() -> tableRows(frame) == 2);
      capture(frame, "04-pet-management");
      edt(
          () -> {
            tables(frame).getFirst().setRowSelectionInterval(0, 0);
            return null;
          });
      click(frame, "Edit pet");
      JDialog editor = dialog("Edit ");
      capture(editor, "05-pet-editor");
      close(editor);
      click(frame, "Adoption requests");
      await(() -> has(frame, "Review application"));
      capture(frame, "06-adoption-management");
      click(frame, "Inventory");
      await(() -> has(frame, "Adjust stock"));
      capture(frame, "07-inventory");
      click(frame, "+ Add item");
      JDialog item = dialog("New inventory");
      capture(item, "08-inventory-editor");
      close(item);
      click(frame, "People & access");
      await(() -> tableRows(frame) == 3);
      capture(frame, "09-accounts");
      click(frame, "+ Create account");
      JDialog account = dialog("Create account");
      capture(account, "10-account-editor");
      close(account);
      click(frame, "Reports");
      await(() -> has(frame, "Generate report"));
      click(frame, "Generate report");
      await(() -> tableRows(frame) == 2);
      capture(frame, "11-reports");
      click(frame, "Sign out");
      await(() -> has(frame, "New here? Create an account"));
      login(frame, "user@test.example", password);
      await(() -> has(frame, "Find a companion"));
      assertFalse(edt(() -> has(frame, "People & access")));
      capture(frame, "12-user-dashboard");
      click(frame, "Find a companion");
      await(() -> has(frame, "Meet Milo"));
      capture(frame, "13-browse-pets");
      click(frame, "Meet Milo");
      JDialog details = dialog("Meet Milo");
      capture(details, "14-pet-details");
      click(details, "Apply to adopt Milo");
      JDialog application = dialog("Adoption application");
      capture(application, "15-application-form");
      edt(
          () -> {
            var text = all(application, JTextArea.class);
            text.get(0).setText("Experienced carer with time for daily walks and training.");
            text.get(1).setText("Secure home, fenced garden, no other animals.");
            all(application, JCheckBox.class).getFirst().setSelected(true);
            return null;
          });
      click(application, "Submit application");
      JDialog message = dialog("Woof");
      click(message, "OK");
      await(() -> !message.isShowing());
      click(frame, "My applications");
      await(() -> tableRows(frame) == 1);
      capture(frame, "16-my-applications");
      click(frame, "My profile");
      await(() -> has(frame, "Save profile"));
      capture(frame, "17-profile");
      edt(
          () -> {
            frame.setSize(980, 720);
            return null;
          });
      capture(frame, "18-small-window");
      edt(
          () -> {
            frame.setSize(1280, 840);
            return null;
          });
      click(frame, "Sign out");
      await(() -> has(frame, "New here? Create an account"));
      login(frame, "admin@test.example", password);
      await(() -> has(frame, "Adoption requests"));
      click(frame, "Adoption requests");
      await(() -> tableRows(frame) == 1);
      edt(
          () -> {
            tables(frame).getFirst().setRowSelectionInterval(0, 0);
            return null;
          });
      click(frame, "Review application");
      JDialog review = dialog("Application details");
      capture(review, "19-application-review");
      edt(
          () -> {
            all(review, JTextArea.class)
                .getLast()
                .setText("Home and care arrangements reviewed. Approved.");
            return null;
          });
      click(review, "Approve adoption");
      JDialog confirmation = dialog("Confirm action");
      click(confirmation, "OK");
      await(() -> !review.isShowing());
      await(
          () ->
              tableRows(frame) == 1
                  && "APPROVED".equals(tables(frame).getFirst().getValueAt(0, 2).toString()));
      edt(
          () -> {
            tables(frame).getFirst().setRowSelectionInterval(0, 0);
            return null;
          });
      click(frame, "Adoption receipt");
      JDialog receipt = dialog("Adoption receipt");
      capture(receipt, "20-receipt");
      close(receipt);
    } finally {
      edt(
          () -> {
            for (Window w : Window.getWindows())
              if (w == frame || w.getOwner() == frame) w.dispose();
            return null;
          });
    }
  }

  private static void login(MainFrame f, String email, String password) throws Exception {
    edt(
        () -> {
          var fields = all(f, JTextField.class);
          fields.stream()
              .filter(t -> "Email address".equals(t.getAccessibleContext().getAccessibleName()))
              .findFirst()
              .orElseThrow()
              .setText(email);
          all(f, JPasswordField.class).getFirst().setText(password);
          return null;
        });
    click(f, "Sign in");
  }

  private static void click(Container c, String text) throws Exception {
    JButton b =
        edt(
            () ->
                all(c, JButton.class).stream()
                    .filter(x -> x.getText().equals(text))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Missing button: " + text)));
    SwingUtilities.invokeLater(b::doClick);
  }

  private static boolean has(Container c, String text) {
    return all(c, JButton.class).stream().anyMatch(b -> text.equals(b.getText()));
  }

  private static List<String> labels(Container c) {
    return all(c, JLabel.class).stream().map(JLabel::getText).toList();
  }

  private static List<JTable> tables(Container c) {
    return all(c, JTable.class);
  }

  private static int tableRows(Container c) {
    var list = tables(c);
    return list.isEmpty() ? -1 : list.getFirst().getRowCount();
  }

  private static JDialog dialog(String prefix) throws Exception {
    await(
        () ->
            Arrays.stream(Window.getWindows())
                .anyMatch(
                    w ->
                        w instanceof JDialog d
                            && d.isShowing()
                            && d.getTitle().startsWith(prefix)));
    return edt(
        () ->
            Arrays.stream(Window.getWindows())
                .filter(
                    w -> w instanceof JDialog d && d.isShowing() && d.getTitle().startsWith(prefix))
                .map(w -> (JDialog) w)
                .findFirst()
                .orElseThrow());
  }

  private static void close(JDialog d) throws Exception {
    edt(
        () -> {
          d.dispose();
          return null;
        });
  }

  private static void capture(Window w, String name) throws Exception {
    Thread.sleep(180);
    Path folder = Path.of(".local", "screenshots");
    Files.createDirectories(folder);
    BufferedImage image =
        edt(
            () -> {
              BufferedImage i =
                  new BufferedImage(w.getWidth(), w.getHeight(), BufferedImage.TYPE_INT_RGB);
              var g = i.createGraphics();
              w.paint(g);
              g.dispose();
              return i;
            });
    ImageIO.write(image, "png", folder.resolve(name + ".png").toFile());
  }

  private static <T extends Component> List<T> all(Container parent, Class<T> type) {
    List<T> result = new ArrayList<>();
    for (Component c : parent.getComponents()) {
      if (type.isInstance(c)) result.add(type.cast(c));
      if (c instanceof Container child) result.addAll(all(child, type));
    }
    return result;
  }

  private static void await(BooleanSupplier condition) throws Exception {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
    while (System.nanoTime() < deadline) {
      if (edt(condition::getAsBoolean)) return;
      Thread.sleep(100);
    }
    throw new AssertionError("UI condition timed out");
  }

  private static <T> T edt(Callable<T> action) throws Exception {
    if (SwingUtilities.isEventDispatchThread()) return action.call();
    FutureTask<T> f = new FutureTask<>(action);
    SwingUtilities.invokeAndWait(f);
    return f.get();
  }
}
