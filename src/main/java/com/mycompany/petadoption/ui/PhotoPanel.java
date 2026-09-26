package com.mycompany.petadoption.ui;

import com.mycompany.petadoption.util.Images;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.*;

public final class PhotoPanel extends JPanel {
  private BufferedImage image;

  public PhotoPanel(String path, int width, int height) {
    image = Images.read(path);
    setPreferredSize(new Dimension(width, height));
    setMinimumSize(new Dimension(120, 120));
    setBackground(new Color(227, 233, 218));
    getAccessibleContext().setAccessibleName("Pet photograph");
  }

  @Override
  protected void paintComponent(Graphics graphics) {
    super.paintComponent(graphics);
    var g = (Graphics2D) graphics.create();
    g.setRenderingHint(
        RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    if (image != null) {
      double scale =
          Math.max(
              (double) getWidth() / image.getWidth(), (double) getHeight() / image.getHeight());
      int w = (int) (image.getWidth() * scale), h = (int) (image.getHeight() * scale);
      g.drawImage(image, (getWidth() - w) / 2, (getHeight() - h) / 2, w, h, null);
    } else {
      g.setColor(Theme.MUTED);
      g.setFont(getFont().deriveFont(Font.BOLD, 22f));
      String text = "A friend awaits";
      g.drawString(
          text,
          Math.max(10, (getWidth() - g.getFontMetrics().stringWidth(text)) / 2),
          getHeight() / 2);
    }
    g.dispose();
  }
}
