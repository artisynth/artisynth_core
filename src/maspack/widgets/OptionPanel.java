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
import java.util.*;

import maspack.util.InternalErrorException;

/**
 * Provides a panel containing controlling options
 */
public class OptionPanel extends JPanel {
   private LinkedHashMap<String,JButton> myButtonMap;
   private ActionListener myActionListener;

   public static final int OK_OPTION = 1;
   public static final int CANCEL_OPTION = 2;
   public static final int SET_OPTION = 3;
   public static final int CLEAR_OPTION = 4;

   private static ImageIcon liveUpdateIcon =
      GuiUtils.loadIcon (OptionPanel.class, "icons/grayLiveUpdate.png");
   private static ImageIcon noLiveUpdateIcon =
      GuiUtils.loadIcon (OptionPanel.class, "icons/grayNoLiveUpdate.png");

   private JButton myLiveUpdateButton = null;
   private boolean myLiveUpdateEnabled = true;

   public OptionPanel (String options, ActionListener actionl) {
      super();
      setBorder (BorderFactory.createEmptyBorder (5, 5, 5, 5));
      setLayout (new BoxLayout (this, BoxLayout.X_AXIS));
      add (Box.createRigidArea (new Dimension (20, 10)));
      add (Box.createHorizontalGlue());
      add (Box.createHorizontalGlue());
      add (Box.createRigidArea (new Dimension (20, 10)));

      myActionListener = actionl;

      myButtonMap = new LinkedHashMap<String,JButton>();
      if (options != null && containsNonWhitespace (options)) {
         String[] opts = options.split ("\\s+");
         for (int i = 0; i < opts.length; i++) {
            addButton (opts[i]);
         }
      }
      else {
         add (Box.createRigidArea (new Dimension (200, 10)), 1);
      }
   }

   private int numRegularButtons() {
      int num = getComponentCount() - 4; // 2 glue components
      if (myLiveUpdateButton != null) {
         num -= 2;
      }
      if (num > 1) {
         num -= (num - 1) / 2; // don't count filler
      }
      return num;
   }

   private int getComponentIdx (int buttonIdx) {
      int idx = 2 + buttonIdx * 2;
      if (myLiveUpdateButton != null) {
         idx += 2;
      }
      return idx;
   }

   public JButton addButton (String name) {
      return addButton (name, numRegularButtons());
   }

   public JButton addButton (String name, int buttonIdx) {
      JButton button;
      if (name.equals ("LiveUpdate")) {
         ImageIcon icon =
            myLiveUpdateEnabled ? liveUpdateIcon : noLiveUpdateIcon;
         button = new JButton (icon);
         GuiUtils.setFixedSize (button, new Dimension (
            icon.getIconWidth(), icon.getIconHeight()));
         button.setContentAreaFilled (false);
         myLiveUpdateButton = button;
         add (Box.createRigidArea (new Dimension (5, 0)), 0);
         add (button, 1);
         // change myLiveUpdateEnabled so setLiveUpdateEnabled
         // will be called.
         myLiveUpdateEnabled = false;
         setLiveUpdateEnabled (true);
      }
      else {
         button = new JButton (name);
         int idx = getComponentIdx (buttonIdx);
         if (numRegularButtons() > 0) {
            Component filler = Box.createRigidArea (new Dimension (5, 0));
            if (buttonIdx < numRegularButtons()) {
               add (filler, idx);
            }
            else {
               add (filler, idx - 1);
            }
         }
         add (button, idx);
      }
      myButtonMap.put (name, button);

      button.addActionListener (myActionListener);
      button.setActionCommand (name);
      if (isVisible()) {
         revalidate();
         repaint();
      }
      return button;
   }

   public boolean removeButton (String name) {
      JButton button = getButton (name);
      if (button == null) {
         return false;
      }
      if (name.equals ("LiveUpdate")) {
         if (myLiveUpdateButton == null) {
            throw new InternalErrorException (
               "liveUpdateButton not found in component list");
         }
         remove (0);
         remove (0);
         myLiveUpdateButton = null;
      }
      else {
         Component[] comps = getComponents();
         int idx = 0;
         while (idx < comps.length && comps[idx] != button) {
            idx++;
         }
         if (idx == comps.length) {
            throw new InternalErrorException (
               "button not found in component list");
         }
         int numRegs = numRegularButtons();
         remove (idx); // remove the button
         if (numRegs > 1) {
            if (idx == getComponentIdx (0)) { // remove following spacer
               remove (idx);
            }
            else { // remove preceding spacer
               remove (idx - 1);
            }
         }
      }
      myButtonMap.remove (name);
      if (isVisible()) {
         revalidate();
         repaint();
      }
      return true;
   }

   private boolean containsNonWhitespace (String str) {
      for (int i = 0; i < str.length(); i++) {
         if (!Character.isWhitespace (str.charAt (i))) {
            return true;
         }
      }
      return false;
   }


   public JButton[] getButtons() {
      return myButtonMap.values().toArray (new JButton[0]);
   }

   public JButton getButton (String name) {
      return myButtonMap.get (name);
   }

   public void setLiveUpdateEnabled (boolean enable) {
      JButton button = myLiveUpdateButton;
      if (myLiveUpdateButton == null) {
         throw new IllegalStateException ("live update button has not been set");
      }
      if (enable != myLiveUpdateEnabled) {
         if (enable) {
            button.setIcon (liveUpdateIcon);
            button.setToolTipText ("disable live updating");
         }
         else {
            button.setIcon (noLiveUpdateIcon);
            button.setToolTipText ("enable live updating");
         }
         myLiveUpdateEnabled = enable;
      }
   }

   public boolean isLiveUpdateEnabled() {
      return myLiveUpdateEnabled;
   }

   public Dimension getMaximumSize() {
      Dimension dim =
         new Dimension (Integer.MAX_VALUE, getPreferredSize().height);
      return dim;
   }

   public String getButtonString() {
      StringBuilder buf = new StringBuilder();
      int cnt = 0;
      for (String name : myButtonMap.keySet()) {
         if (cnt > 0) {
            buf.append (' ');
         }
         buf.append (name);
         cnt++;
      }
      return buf.toString();
   }
}
