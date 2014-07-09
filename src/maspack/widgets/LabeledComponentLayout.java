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

public class LabeledComponentLayout implements LayoutManager {
   int mySpacing = 4;
   boolean myStretchableP = false;

   public int getSpacing() {
      return mySpacing;
   }

   public void setSpacing (int s) {
      mySpacing = s;
   }

   public boolean isStretchable() {
      return myStretchableP;
   }

   public void setStretchable (boolean enable) {
      myStretchableP = enable;
   }

   private void addInsets (Dimension dim, Insets insets) {
      dim.width += (insets.left + insets.right);
      dim.height += (insets.top + insets.bottom);
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
      addInsets (totalSize, parent.getInsets());
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
      addInsets (totalSize, parent.getInsets());
      if (comps.length > 0) {
         totalSize.height += (mySpacing * (comps.length + 1));
      }
      return totalSize;
   }

   private int addVec (int[] vec) {
      int s = 0;
      for (int i=0; i<vec.length; i++){
         s += vec[i];
      }
      return s;
   }

   private void stretchHeights (int[] heights, int[] maxes, int maxHeight) {
      // distribute extra into heights into either it is all used up
      // or we can't grow any more
      boolean heightIncreasing = false;
      do {
         int totalHeight = 0;
         int adjustableHeight = 0;
         for (int i=0; i<heights.length; i++) {
            totalHeight += heights[i];
            if (heights[i] < maxes[i]) {
               adjustableHeight += heights[i];
            }
         }
         heightIncreasing = false;
         if (totalHeight < maxHeight && adjustableHeight != 0) {

            int extraHeight = maxHeight-totalHeight;
            for (int i=0; i<heights.length; i++) {
               if (heights[i] < maxes[i]) {
                  int h = heights[i] +
                     (int)((extraHeight*heights[i])/(double)adjustableHeight);
                  h = Math.min (h, maxes[i]);
                  if (h != heights[i]) {
                     heights[i] = h;
                     heightIncreasing = true;
                  }
               }
            }
         }
      }
      while (heightIncreasing);
   }

   public void layoutContainer (Container parent) {
      Insets insets = parent.getInsets();
      int x = insets.left;
      int y = insets.top + mySpacing;
      int totalWidth = parent.getWidth() - (insets.left + insets.right);
      int totalHeight = parent.getHeight() - (insets.top + insets.bottom);
      Component[] comps = parent.getComponents();

      int[] heights = new int[comps.length];
      int[] maxes = new int[comps.length];
      int nvisible = 0;

      // first get the preferred and maximum height for all components
      for (int i = 0; i < comps.length; i++) {
         if (comps[i].isVisible()) {
            Dimension prefSize = comps[i].getPreferredSize();
            Dimension maxSize = comps[i].getMaximumSize();
            heights[i] = prefSize.height;
            maxes[i] = maxSize.height;
            nvisible++;
         }
         else {
            heights[i] = 0;
            maxes[i] = 0;
         }
      }
      stretchHeights (heights, maxes, totalHeight-mySpacing*(nvisible+1));
      for (int i = 0; i < comps.length; i++) {
         if (comps[i].isVisible()) {
            Dimension prefSize = comps[i].getPreferredSize();
            Dimension maxSize = comps[i].getMaximumSize();
            int width = Math.min (totalWidth, maxSize.width);
            comps[i].setBounds (x, y, width, heights[i]);
            y += heights[i] + mySpacing;
         }
      }
   }

   public static void main (String[] args) {
      JFrame frame = new JFrame ("layout test");
      JPanel panel = new JPanel();
      frame.getContentPane().add (panel);
      BooleanSelector attachment = new BooleanSelector ("attachment");
      attachment.setBorder (BorderFactory.createLineBorder (Color.RED));
      panel.add (attachment);
      BooleanSelector collision = new BooleanSelector ("collision");
      collision.setBorder (BorderFactory.createLineBorder (Color.RED));
      panel.add (collision);
      panel.add (new StringSelector (
         "integrator", new String[] { "ForwardEuler", "SymplecticEuler" }));
      panel.add (new DoubleField ("maxStepSize"));
      panel.setLayout (new LabeledComponentLayout());
      //LabeledComponent.equalizeLabelWidths (panel.getComponents());
      frame.pack();
      frame.setVisible (true);
   }
}
