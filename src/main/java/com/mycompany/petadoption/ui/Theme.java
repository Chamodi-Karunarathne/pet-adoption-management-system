package com.mycompany.petadoption.ui;

import com.formdev.flatlaf.FlatLightLaf;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

public final class Theme {
  public static final Color GREEN = new Color(35, 78, 63),
      INK = new Color(36, 49, 43),
      MUTED = new Color(96, 111, 102),
      BACKGROUND = new Color(247, 246, 240),
      LINE = new Color(224, 228, 220),
      ACCENT = new Color(186, 109, 77),
      ERROR = new Color(162, 53, 53),
      SUCCESS = new Color(44, 113, 75),
      WARNING = new Color(146, 101, 26);

  private Theme() {}

  public static void install() {
    FlatLightLaf.setup();
    UIManager.put("defaultFont", new Font("SansSerif", Font.PLAIN, 14));
    UIManager.put("Panel.background", BACKGROUND);
    UIManager.put("Label.foreground", INK);
    UIManager.put("Component.arc", 12);
    UIManager.put("Button.arc", 14);
    UIManager.put("TextComponent.arc", 12);
    UIManager.put("Component.focusColor", GREEN);
    UIManager.put("Button.default.background", GREEN);
    UIManager.put("Button.default.foreground", Color.WHITE);
    UIManager.put("Table.rowHeight", 36);
    UIManager.put("Table.selectionBackground", new Color(219, 234, 221));
    UIManager.put("Table.selectionForeground", INK);
    UIManager.put("ScrollBar.width", 10);
  }

  public static JPanel panel(LayoutManager layout) {
    JPanel p = new JPanel(layout);
    p.setOpaque(false);
    return p;
  }

  public static JLabel title(String text, int size) {
    JLabel l = new JLabel(text);
    l.putClientProperty("html.disable", true);
    l.setFont(l.getFont().deriveFont(Font.BOLD, (float) size));
    return l;
  }

  public static JLabel muted(String text) {
    JLabel l = new JLabel(text);
    l.putClientProperty("html.disable", true);
    l.setForeground(MUTED);
    return l;
  }

  public static JPanel card() {
    JPanel p = new JPanel(new BorderLayout(16, 16));
    p.setBackground(Color.WHITE);
    p.setBorder(new CompoundBorder(new LineBorder(LINE, 1, true), new EmptyBorder(20, 20, 20, 20)));
    return p;
  }

  public static JButton button(String text, Runnable action) {
    JButton b = new JButton(text);
    b.putClientProperty("html.disable", true);
    b.setMargin(new Insets(11, 18, 11, 18));
    b.addActionListener(e -> action.run());
    return b;
  }

  public static JButton primary(String text, Runnable action) {
    JButton b = button(text, action);
    b.setBackground(GREEN);
    b.setForeground(Color.WHITE);
    return b;
  }

  public static JTextField field(String placeholder) {
    JTextField f = new JTextField(24);
    f.putClientProperty("JTextField.placeholderText", placeholder);
    f.setMargin(new Insets(9, 10, 9, 10));
    return f;
  }

  public static JTextArea area(int rows) {
    JTextArea a = new JTextArea(rows, 30);
    a.setLineWrap(true);
    a.setWrapStyleWord(true);
    a.setMargin(new Insets(10, 10, 10, 10));
    return a;
  }

  public static JPanel column(Component... components) {
    JPanel p = new ColumnPanel();
    int y = 0;
    for (Component c : components) {
      var g = new GridBagConstraints();
      g.gridx = 0;
      g.gridy = y++;
      g.weightx = 1;
      g.fill = GridBagConstraints.HORIZONTAL;
      g.anchor = GridBagConstraints.NORTHWEST;
      g.insets = new Insets(0, 0, 12, 0);
      p.add(c, g);
    }
    return p;
  }

  public static JPanel fieldRow(String label, JComponent field) {
    JLabel l = new JLabel(label);
    l.setLabelFor(field);
    field.getAccessibleContext().setAccessibleName(label);
    return column(l, field);
  }

  public static JScrollPane scroll(Component c) {
    JScrollPane s = new JScrollPane(c);
    s.setBorder(null);
    s.getVerticalScrollBar().setUnitIncrement(22);
    return s;
  }

  public static JPanel flow(Component... c) {
    JPanel p = panel(new WrapLayout());
    for (Component child : c) p.add(child);
    return p;
  }

  private static final class ColumnPanel extends JPanel implements Scrollable {
    ColumnPanel() {
      super(new GridBagLayout());
      setOpaque(false);
    }

    public Dimension getPreferredScrollableViewportSize() {
      return getPreferredSize();
    }

    public int getScrollableUnitIncrement(Rectangle r, int orientation, int direction) {
      return 22;
    }

    public int getScrollableBlockIncrement(Rectangle r, int orientation, int direction) {
      return Math.max(22, r.height - 22);
    }

    public boolean getScrollableTracksViewportWidth() {
      return true;
    }

    public boolean getScrollableTracksViewportHeight() {
      return false;
    }
  }
}
