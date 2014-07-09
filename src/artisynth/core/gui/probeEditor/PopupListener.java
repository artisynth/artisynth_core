/**
 * Copyright (c) 2014, by the Authors: Johnty Wang (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.gui.probeEditor;

import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;
import javax.swing.JPopupMenu;

//TODO: clean up this (no longer need most of the functionality in this class)
public class PopupListener extends MouseAdapter {
   // possibly make these colors public? or at least settable from the outside?
   public static final Color activeColor = Color.RED;
   public static final Color inactiveColor = Color.LIGHT_GRAY;
   private static final Color completedColor = Color.GREEN;

   private Color normalColor; // this changes according to whether its active,
                              // inactive, or complete.

   JPopupMenu popup;
   JPanel myPane;

   PopupListener (JPopupMenu popupMenu, JPanel pane) {
      popup = popupMenu;
      myPane = pane;
      normalColor = inactiveColor;
   }

   public void mouseEntered (MouseEvent e) {
      changeBackground (hoverColor (normalColor));
   }

   public void mouseExited (MouseEvent e) {
      changeBackground (normalColor);
   }

   public void mouseClicked (MouseEvent e) {
   }

   public void mousePressed (MouseEvent e) {
      maybeShowPopup (e);
   }

   public void mouseReleased (MouseEvent e) {
      maybeShowPopup (e);
   }

   public void setNormalColor (Color color) {
      normalColor = color;
      changeBackground (normalColor);
   }

   private void maybeShowPopup (MouseEvent e) {
      if (popup != null) {
         if (e.isPopupTrigger()) {
            popup.show (e.getComponent(), e.getX(), e.getY());
         }
      }
   }

   private void changeBackground (Color color) {
      myPane.setBackground (color);
   }

   private Color hoverColor (Color inColor) {
      int red = Math.max (inColor.getRed() - 50, 0);
      int green = Math.max (inColor.getGreen() - 50, 0);
      int blue = Math.max (inColor.getBlue() - 50, 0);
      return new Color (red, green, blue);
   }
}