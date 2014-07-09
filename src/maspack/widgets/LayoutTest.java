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
import javax.swing.event.*;

public class LayoutTest extends JFrame implements LayoutManager {
   private JPanel myPanel;
   private int mySpacing = 4;

   public LayoutTest() {
      super ("LayoutTest");
      myPanel = new JPanel();
      getContentPane().add (myPanel);
      myPanel.add (new BooleanSelector ("attachment"));
      myPanel.add (new BooleanSelector ("collision"));
      myPanel.add (new StringSelector (
         "integrator", new String[] { "ForwardEuler", "SymplecticEuler" }));
      myPanel.add (new DoubleField ("maxStepSize"));
      myPanel.setLayout (this);
      //LabeledComponent.equalizeLabelWidths (myPanel.getComponents());
      pack();
   }

   public void addLayoutComponent (String name, Component comp) {
   }

   public void removeLayoutComponent (Component comp) {
   }

   public Dimension minimumLayoutSize (Container parent) {
      Dimension totalSize = new Dimension();
      int maxWidth = 0;
      Component[] comps = parent.getComponents();
      for (int i = 0; i < comps.length; i++) {
         if (comps[i].isVisible()) {
            Dimension size = comps[i].getMinimumSize();
            totalSize.height += size.height;
            if (size.width > maxWidth) {
               maxWidth = size.width;
            }
         }
      }
      totalSize.width = maxWidth;
      Insets insets = parent.getInsets();
      totalSize.width += (insets.left + insets.right);
      totalSize.height += (insets.top + insets.bottom);
      if (comps.length > 0) {
         totalSize.height += (mySpacing * (comps.length + 1));
      }
      return totalSize;
   }

   public Dimension preferredLayoutSize (Container parent) {
      Dimension totalSize = new Dimension();
      int maxWidth = 0;
      Component[] comps = parent.getComponents();
      for (int i = 0; i < comps.length; i++) {
         if (comps[i].isVisible()) {
            Dimension size = comps[i].getPreferredSize();
            totalSize.height += size.height;
            if (size.width > maxWidth) {
               maxWidth = size.width;
            }
         }
      }
      totalSize.width = maxWidth;
      Insets insets = parent.getInsets();
      totalSize.width += (insets.left + insets.right);
      totalSize.height += (insets.top + insets.bottom);
      if (comps.length > 0) {
         totalSize.height += (mySpacing * (comps.length + 1));
      }
      return totalSize;
   }

   public void layoutContainer (Container parent) {
      Insets insets = parent.getInsets();
      int x = insets.left;
      int y = insets.top + mySpacing;
      int totalWidth = parent.getWidth() - (insets.left + insets.right);
      Component[] comps = parent.getComponents();
      for (int i = 0; i < comps.length; i++) {
         if (comps[i].isVisible()) {
            Dimension prefSize = comps[i].getPreferredSize();
            Dimension maxSize = comps[i].getMaximumSize();
            int width = Math.min (totalWidth, maxSize.width);
            comps[i].setBounds (x, y, width, prefSize.height);
            y += prefSize.height + mySpacing;
         }
      }
   }

   public static void main (String[] args) {
      LayoutTest test = new LayoutTest();
      test.setVisible (true);
   }
}
