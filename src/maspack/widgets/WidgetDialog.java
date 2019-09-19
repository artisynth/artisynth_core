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

import maspack.matrix.Vector2d;
import maspack.matrix.VectorNd;
import maspack.util.InternalErrorException;
import maspack.util.StringHolder;

public class WidgetDialog extends JDialog
   implements ActionListener, ValueChangeListener {
   protected PropertyPanel myPanel;
   protected boolean myDisposed = false;
   protected String myConfirmCmd = null;
   protected Validator myValidator = null;

   private int myRetValue = OptionPanel.CANCEL_OPTION;

   public static interface Validator {
      public String validateSettings (PropertyPanel panel);
   }

   public Validator getValidator() {
      return myValidator;
   }

   public void setValidator (Validator validator) {
      myValidator = validator;
   }

   public void valueChange (ValueChangeEvent e) {
      if (myConfirmCmd == null) {
         doDone();
      }
   }   

   public WidgetDialog (Frame owner, String name, String confirmCmd) {
      super (owner, name);
      initialize (confirmCmd);
   }

   public WidgetDialog (Dialog owner, String name, String confirmCmd) {
      super (owner, name);
      initialize (confirmCmd);
   }

   public static WidgetDialog createDialog (
      Window owner, String name, String confirmCmd) {

      if (owner instanceof Dialog) {
         return new WidgetDialog ((Dialog)owner, name, confirmCmd);
      }
      else if (owner instanceof Frame) {
         return new WidgetDialog ((Frame)owner, name, confirmCmd);
      }
      else {
         throw new InternalErrorException ("Unsupported window type: " + owner);
      }
   }

   protected void initialize (String confirmCmd) {
      setModal (true);
      setDefaultCloseOperation (DISPOSE_ON_CLOSE);
      getContentPane().setLayout (
         new BoxLayout (getContentPane(), BoxLayout.Y_AXIS));

      myPanel = new PropertyPanel();
      getContentPane().add (myPanel);
      JSeparator sep = new JSeparator();
      sep.setAlignmentX (Component.CENTER_ALIGNMENT);
      getContentPane().add (GuiUtils.createBoxFiller());
      getContentPane().add (sep);      

      String options = "Cancel";
      if (confirmCmd != null) {
         options = confirmCmd + " " + options;
      }
      myConfirmCmd = confirmCmd;
      OptionPanel optionPanel = new OptionPanel (options, this);
      optionPanel.setAlignmentX (Component.CENTER_ALIGNMENT);
      getContentPane().add (optionPanel);
      pack();
   }

   public void addWidget (Component comp) {
      myPanel.addWidget (comp);
      if (comp instanceof LabeledControl) {
         LabeledControl ctrl = (LabeledControl)comp;
         ctrl.addValueChangeListener (this);
      }
      pack();
   }

   private void doDone() {
      if (myValidator != null) {
         String errMsg = myValidator.validateSettings (myPanel);
         if (errMsg != null) {
            GuiUtils.showError (this, errMsg);
            return;
         }
      }
      myRetValue = OptionPanel.OK_OPTION;
      setVisible (false);
      dispose();
   }

   public void actionPerformed (ActionEvent e) {
      String cmd = e.getActionCommand();
      if (cmd.equals (myConfirmCmd)) {
         doDone();
      }
      else if (cmd.equals ("Cancel")) {
         setVisible (false);
         dispose();
      }
   }

   public int getReturnValue() {
      return myRetValue;
   }

   public void dispose() {
      if (!myDisposed) {
         myPanel.dispose();
         myDisposed = true;
      }
      super.dispose();
   }

}
