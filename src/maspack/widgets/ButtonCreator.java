/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import java.awt.Dimension;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JButton;

public class ButtonCreator {
   public final static boolean BUTTON_ENABLED = true;
   public final static boolean BUTTON_DISABLED = false;

   public final static Dimension SMALL_BUTTON_SIZE = new Dimension(25,25);
   public final static Dimension LARGE_BUTTON_SIZE = new Dimension(32,32);

   /****************************************************************************
    * section name: buttons section section to create iconic and generic buttons
    * with text
    * 
    ***************************************************************************/

   /**
    * Create iconic button
    * @return iconic button
    */
   public static JButton createIconicButton (
      Icon icon, String actionCommand, String toolTip, boolean enabled,
      boolean large, ActionListener actionListener) {
      Dimension dim = (large ? LARGE_BUTTON_SIZE : SMALL_BUTTON_SIZE);
      return createGenericButton (
         icon, null, actionCommand, toolTip, enabled, dim, actionListener);
   }

   /**
    * to create button with text
    *
    * @return text button
    */

   public static JButton createTextButton (
      String buttonText, String actionCommand, String toolTip, boolean enabled,
      boolean large, ActionListener actionListener) {
      Dimension dim = (large ? LARGE_BUTTON_SIZE : SMALL_BUTTON_SIZE);
      return createGenericButton (
         null, buttonText, actionCommand, toolTip, 
         enabled, dim, actionListener);
   }

   /**
    * generic button making function used by: createIconicButton(), and
    * createTextButton()
    *
    * @return generic button
    */

   public static JButton createGenericButton (
      Icon icon, String buttonText, String actionCommand, String toolTip,
      boolean enabled, Dimension dim, ActionListener actionListenerObject) {
      JButton button = new JButton();
      button.setActionCommand (actionCommand);
      button.setToolTipText (toolTip);
      if (actionListenerObject != null) {
         button.addActionListener (actionListenerObject);
      }
      button.setEnabled (enabled);

      if (icon != null) {
         if (dim == null) {
            button.setIcon (icon);
            button.setMinimumSize (SMALL_BUTTON_SIZE);
            button.setMaximumSize (SMALL_BUTTON_SIZE);
            button.setPreferredSize (SMALL_BUTTON_SIZE);
         }
         else {
            button.setIcon (icon);
            button.setMinimumSize (dim);
            button.setMaximumSize (dim);
            button.setPreferredSize (dim);
         }
         // button.setContentAreaFilled (false);
      }
      else
         button.setText (buttonText);
      return button;
   }

}
