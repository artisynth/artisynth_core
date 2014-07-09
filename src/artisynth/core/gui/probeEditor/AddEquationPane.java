/**
 * Copyright (c) 2014, by the Authors: Johnty Wang (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.gui.probeEditor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.TextEvent;
import java.awt.event.TextListener;

import javax.swing.*;

import maspack.widgets.GuiUtils;
import maspack.widgets.StringField;
import maspack.widgets.ValueChangeEvent;
import maspack.widgets.ValueChangeListener;

/**
 * @author Johnty
 * @version 0.1 Class for representing an equation editor in the probe editor
 */

class AddEquationPane extends JPanel implements MouseListener,
ValueChangeListener, ActionListener {
   static final long serialVersionUID = 1l;

   private Color normalColor;
   private StringField equationField;
   private int equationIndex;
   private JLabel dimensionLabel;
   private NumericProbeEditor myParent;
   private boolean isHighlighted = false;
   private JPopupMenu deletePopup = null;

   public AddEquationPane (NumericProbeEditor parent) {
      this (parent, "");
   }

   public AddEquationPane (NumericProbeEditor parent, String expr) {
      myParent = parent;
      equationIndex = 0;
      equationField = new StringField ("", 1);
      equationField.setValue (expr);

      equationField.setMaximumSize (new Dimension (1000, 25));
      equationField.setPreferredSize (new Dimension (40, 25));
      this.setAlignmentX (LEFT_ALIGNMENT);
      equationField.addValueChangeListener (this);
      this.addMouseListener (this);

      equationField.setStretchable (true);

      this.setLayout (new BoxLayout (this, BoxLayout.X_AXIS));
      this.setMaximumSize (new Dimension (1000, 35));
      this.setMinimumSize (new Dimension (40, 35));
      // this.setBorder(BorderFactory.createLineBorder(parent.getBackground(),1));
      this.setBorder (BorderFactory.createEtchedBorder());
      // this.add(Box.createRigidArea(new Dimension(15, 0)));
      this.add (equationField);
      this.add (Box.createRigidArea (new Dimension (5, 0)));

      updateAppearance();
   }

   private void createDimensionLabel() {
      dimensionLabel = new JLabel();
      GuiUtils.setFixedSize (dimensionLabel, 32, 25);
      dimensionLabel.setHorizontalAlignment (JLabel.RIGHT);
      add (dimensionLabel);
      add (Box.createRigidArea (new Dimension (10, 0)));
   }

   void setDimensionLabel (int i) {
      if (dimensionLabel == null) {
         createDimensionLabel();
      }
      dimensionLabel.setText (Integer.toString (i));
   }

   void clearDimensionLabel() {
      if (dimensionLabel == null) {
         createDimensionLabel();
      }
      dimensionLabel.setText ("");
   }

   private boolean isCompleted() {
      String str = equationField.getStringValue();
      return (str != null && !str.equals (""));
   }

   void updateAppearance() {

      Color color =
         NumericProbeEditor.getBuildComponentColor (
            this, isCompleted(), isHighlighted);
      if ((color == null && isBackgroundSet()) ||
          (color != null && (!isBackgroundSet() ||
                             !color.equals (getBackground())))) {
         setBackground (color);
         equationField.setBackground (color);
      }
   }

   public void setHighlight (boolean highlight) {
      isHighlighted = highlight;
      updateAppearance();
   }

   public void setEnabled (boolean enabled) {
      equationField.getTextField().setEditable (enabled);
   }

   public void valueChange (ValueChangeEvent v) {
      myParent.actionPerformed (new ActionEvent (this, 0, "Expression Changed"));
   }

   public void setEqText (String str) {
      System.out.println ("setEqText: " + str);
      equationField.removeValueChangeListener (this);
      equationField.setValue (str);
      equationField.addValueChangeListener (this);
      updateAppearance();
   }

   public String getEqText() {
      return equationField.getStringValue();
   }

   public void mouseEntered (MouseEvent e) {
      // System.out.println("entered");
      // myParent.actionPerformed(new ActionEvent(this, 0, "MouseEnteredEq"));
      // setHighlight(true);
   }

   public void mouseExited (MouseEvent e) {
      // System.out.println("exited");
      // myParent.actionPerformed(new ActionEvent(this, 0, "MouseExitedEq"));
      // setHighlight(false);
   }

   public void mouseClicked (MouseEvent e) {
      // System.out.println("clicked");
   }

   public void mousePressed (MouseEvent e) {
      maybeShowPopup (e);
   }

   public void mouseReleased (MouseEvent e) {
   }

   public void actionPerformed (ActionEvent e) {
      if (e.getActionCommand() == "Delete") {
         // System.out.println("Removing channel "+ this.getChannelName());
         myParent.actionPerformed (new ActionEvent (this, 0, "Delete Channel"));
      }
   }

   public void createDeletePopup() {
      JMenuItem menuItem;

      // Create the popup menu.
      deletePopup = new JPopupMenu();
      menuItem = new JMenuItem ("Delete");
      menuItem.addActionListener (this);
      deletePopup.add (menuItem);
      // Add listener to the text area so the deletePopup menu can come up.
   }

   private void maybeShowPopup (MouseEvent e) {
      if (deletePopup != null) {
         if (e.isPopupTrigger()) {
            deletePopup.show (e.getComponent(), e.getX(), e.getY());
         }
      }
   }

}
