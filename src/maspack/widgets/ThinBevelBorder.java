/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

public class ThinBevelBorder implements Border {
   private int myType;

   public ThinBevelBorder (int type) {
      if (type != BevelBorder.LOWERED && type != BevelBorder.RAISED) {
         throw new IllegalArgumentException ("Illegal border type " + type);
      }
      myType = type;
   }

   public void paintBorder (Component c, Graphics g, int x, int y, int w, int h) {
      Color topColor;
      Color bottomColor;

      if (myType == BevelBorder.LOWERED) {
         topColor = c.getBackground().darker();
         bottomColor = Color.WHITE;
      }
      else {
         topColor = Color.WHITE;
         bottomColor = c.getBackground().darker();
      }

      h--;
      w--;

      g.setColor (topColor);
      g.drawLine (x, y, x + w, y);
      g.drawLine (x, y, x, y + h);

      g.setColor (bottomColor);
      g.drawLine (x, y + h, x + w, y + h);
      g.drawLine (x + w, y, x + w, y + h);
   }

   public Insets getBorderInsets (Component c) {
      return new Insets (1, 1, 1, 1);
   }

   public boolean isBorderOpaque() {
      return true;
   }
}
