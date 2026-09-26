package com.mycompany.petadoption.ui;

import java.awt.*;

/** Flow toolbar whose preferred height includes wrapped rows when the window narrows. */
final class WrapLayout extends FlowLayout {
  WrapLayout() {
    super(FlowLayout.LEFT, 10, 6);
  }

  @Override
  public Dimension preferredLayoutSize(Container target) {
    synchronized (target.getTreeLock()) {
      int width = target.getWidth();
      if (width <= 0) return super.preferredLayoutSize(target);
      Insets insets = target.getInsets();
      int available = Math.max(1, width - insets.left - insets.right - 2 * getHgap());
      int rowWidth = 0, rowHeight = 0, totalHeight = getVgap(), maxWidth = 0;
      for (Component c : target.getComponents())
        if (c.isVisible()) {
          Dimension d = c.getPreferredSize();
          int gap = rowWidth == 0 ? 0 : getHgap();
          if (rowWidth > 0 && rowWidth + gap + d.width > available) {
            maxWidth = Math.max(maxWidth, rowWidth);
            totalHeight += rowHeight + getVgap();
            rowWidth = 0;
            rowHeight = 0;
            gap = 0;
          }
          rowWidth += gap + d.width;
          rowHeight = Math.max(rowHeight, d.height);
        }
      return new Dimension(
          Math.max(maxWidth, rowWidth) + insets.left + insets.right + 2 * getHgap(),
          totalHeight + rowHeight + getVgap() + insets.top + insets.bottom);
    }
  }
}
