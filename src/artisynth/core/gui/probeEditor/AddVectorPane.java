/**
 * Copyright (c) 2014, by the Authors: Johnty Wang (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.gui.probeEditor;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Vector;
import java.util.concurrent.CyclicBarrier;

import javax.management.ValueExp;
import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import maspack.widgets.GuiUtils;
import maspack.widgets.IntegerField;
import maspack.widgets.StringField;
import maspack.widgets.ValueChangeEvent;
import maspack.widgets.ValueChangeListener;

//TODO: 'upgrade' text fields to StringField eventually?
/**
 * @author Johnty
 * @version 0.1 Class for representing a probe channel in the probe editor
 * dialog
 */

class AddVectorPane extends JPanel implements ActionListener,
ValueChangeListener, MouseListener {
   static final long serialVersionUID = 1l;

   private Color normalColor;
   private JPopupMenu popup;
   private StringField ChannelLabel;
   // private int ChannelID;
   private IntegerField DimensionBox;
   private NumericProbeEditor myParent;
   private String oldChannelName;
   private int oldDim;
   private boolean isHighlighted;

   public AddVectorPane (NumericProbeEditor parent, String name) {
      myParent = parent;
      normalColor = Color.LIGHT_GRAY;
      this.setLayout (new BoxLayout (this, BoxLayout.X_AXIS));
      // this.setBackground(normalColor);
      this.setAlignmentX (LEFT_ALIGNMENT);
      // this.setBorder(BorderFactory.createLineBorder(parent.getBackground(),1));
      this.setBorder (BorderFactory.createEtchedBorder());
      GuiUtils.setFixedSize (this, 85, 35);

      ChannelLabel = new StringField ("", 1);
      GuiUtils.setFixedSize (ChannelLabel, 36, 25);
      // ChannelLabel.setBackground(this.getBackground());

      DimensionBox = new IntegerField ("", NumericProbeEditor.DEFAULT_DIM);
      // TODO: ensure numeric values?

      GuiUtils.setFixedSize (DimensionBox, 32, 25);
      DimensionBox.getTextField().setHorizontalAlignment (JTextField.TRAILING);
      // DimensionBox.setBackground(this.getBackground());

      int ID = 0;
      if (name == null) {
         ChannelLabel.setValue (("ch" + ID));
      }
      else {
         ChannelLabel.setValue (name);
         oldChannelName = name;
      }
      placeInternalComponents();
      // System.out.println("created probe channel track
      // "+this.getChannelName());
      this.createPopupMenu();

      DimensionBox.addValueChangeListener (this);
      ChannelLabel.addValueChangeListener (this);
      this.addMouseListener (this);
   }

   public void setAsOutput() {
      DimensionBox.getTextField().setEditable (false);
      ChannelLabel.setVisible (false);
      this.remove (ChannelLabel);
   }

   public void setDim (int dim) {
      DimensionBox.setValue (dim);
      oldDim = dim;
      DimensionBox.invalidate();
      DimensionBox.repaint();
   }

   public void clearDimBox() {
      DimensionBox.getTextField().setText ("0");
   }

   /**
    * 
    * @return dimensions as currently defined in the text field. 0 if invalid
    */
   public int getDim() {
      return DimensionBox.getIntValue();
   }

   public boolean dimSet() {
      if (getDim() < 1) {
         return false;
      }
      return true;
   }

   public void setChannelName (String name) {
      ChannelLabel.setValue (name);
   }

   public void setOldName (String name) {
      oldChannelName = name;
   }

   public String getChannelName() {
      return ChannelLabel.getStringValue();
   }

   //
   // for an output probe, we don't need to show the name of this variable
   //
   public void hideName() {
      this.remove (ChannelLabel);
      this.revalidate();
      this.repaint();
   }

   public String getOldName() {
      return oldChannelName;
   }

   // public int getChannelID()
   // {
   // return ChannelID;
   // }
   private void placeInternalComponents() {
      this.add (ChannelLabel);
      this.add (Box.createHorizontalGlue());
      this.add (Box.createRigidArea (new Dimension (5, 0)));
      this.add (DimensionBox);
      this.add (Box.createRigidArea (new Dimension (5, 0)));
   }

   public JPanel getPane() {
      return this;
   }

   void updateAppearance() {
      Color color =
         NumericProbeEditor.getBuildComponentColor (
            this, /* complete= */true, isHighlighted);
      if ((color == null && isBackgroundSet()) ||
          (color != null && (!isBackgroundSet() ||
                             !color.equals (getBackground())))) {
         setBackground (color);
         DimensionBox.setBackgroundAll (color);
         ChannelLabel.setBackground (color);
      }
   }

   public void setHighlight (boolean highlight) {
      isHighlighted = highlight;
      updateAppearance();
   }

   public void valueChange (ValueChangeEvent v) {
      if (v.getSource().getClass().isAssignableFrom (IntegerField.class)) {
         // dim change

         if (DimensionBox.getIntValue() != oldDim) {
            // System.out.println("integer value; dim change");
            myParent.actionPerformed (new ActionEvent (this, 0, "Resized"));
            oldDim = DimensionBox.getIntValue();
         }
      }
      else {
         // System.out.println("Not an integer value; must be variable name");
         myParent.actionPerformed (new ActionEvent (this, 0, "Renamed"));
      }
      // System.out.println("value change from vecPane: " +
      // v.getSource().getClass().getName());
   }

   public void actionPerformed (ActionEvent e) {
      // three things that can trigger action events:
      // 1.  channel name field
      // 2.  dimensions filed
      // 3.  context menu

      if (e.getActionCommand() == "Delete") {
         // System.out.println("Removing channel "+ this.getChannelName());
         myParent.actionPerformed (new ActionEvent (this, 0, "Delete Channel"));
      }
      else {
         if ((StringField)e.getSource() == ChannelLabel) {
            // System.out.println("from name field");
            myParent.actionPerformed (new ActionEvent (this, 0, "Renamed"));
         }
         else {
            // System.out.println("from dim field");
            if (getDim() == 0) {
               GuiUtils.showError (
                  myParent, "Please enter non-zero integer value!");
               setDim (oldDim);
               // System.out.println("invalid! setting back to "+oldDim);
            }
            else if (getDim() == oldDim) // same as before, so do nothing
            {
               // System.out.println("new dim same as before, so do nothing.");
            }
            else // this is a real size change, so we send it back up to the
                  // parent for processing
            {
               setDim (getDim());
               // System.out.println("ok. setting dim to "+getDim());
               myParent.actionPerformed (new ActionEvent (this, 0, "Resized"));
            }
         }
      }

   }

   public void createPopupMenu() {
      JMenuItem menuItem;

      // Create the popup menu.
      popup = new JPopupMenu();
      menuItem = new JMenuItem ("Delete");
      menuItem.addActionListener (this);
      popup.add (menuItem);
      // Add listener to the text area so the popup menu can come up.
   }

   // mouse handling code:
   public void mouseEntered (MouseEvent e) {
      // setHighlight(true);
      // myParent.actionPerformed(new ActionEvent(this, 0, "MouseEnteredVec"));
   }

   public void mouseExited (MouseEvent e) {
      // setHighlight(false);
      // myParent.actionPerformed(new ActionEvent(this, 0, "MouseExitedVec"));
   }

   public void mouseClicked (MouseEvent e) {
   }

   public void mousePressed (MouseEvent e) {
      maybeShowPopup (e);
   }

   public void mouseReleased (MouseEvent e) {
      maybeShowPopup (e);
   }

   private void maybeShowPopup (MouseEvent e) {
      if (popup != null) {
         if (e.isPopupTrigger()) {
            popup.show (e.getComponent(), e.getX(), e.getY());
         }
      }
   }

}
