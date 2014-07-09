/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import java.awt.Dimension;
import javax.swing.JLabel;

/**
 * Subclass of JLabel that gives us precise control over its width
 */
public class SizableLabel extends JLabel {
   int minWidth = 0;

   public SizableLabel() {
      super();
   }

   public SizableLabel (String text) {
      super (text);
   }

   public Dimension getMaximumSize() {
      return getPreferredSize();
   }

   public Dimension getMinimumSize() {
      return getPreferredSize();
   }

   public Dimension getPreferredSize() {
      Dimension size = new Dimension (super.getPreferredSize());
      if (size.width < minWidth) {
         size.width = minWidth;
      }
      return size;
   }

   public void setMinimumWidth (int w) {
      minWidth = w;
   }

   public int getPreferredWidth() {
      return super.getPreferredSize().width;
   }

}
