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
import maspack.properties.*;

public class WidgetDialog extends JDialog
   implements ActionListener, ValueChangeListener {
   protected PropertyPanel myPanel;
   protected OptionPanel myOptionPanel = null;   
   protected boolean myDisposed = false;
   protected String myConfirmCmd = null;
   protected Validator myValidator = null;
   protected Validator myIgnoreValidator = null;

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

   public Validator getIgnoreValidator() {
      return myIgnoreValidator;
   }

   public void setIgnoreValidator (Validator validator) {
      myIgnoreValidator = validator;
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
      myOptionPanel = new OptionPanel (options, this);
      myOptionPanel.setAlignmentX (Component.CENTER_ALIGNMENT);
      getContentPane().add (myOptionPanel);
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
   
   public LabeledComponent getWidget (String label) {
      return myPanel.getWidget (label);
   }

   public LabeledComponentBase addWidget (HasProperties host, String name) {
      return myPanel.addWidget (host, name);
   }

   public LabeledComponentBase addWidget (
      HasProperties host, String name, double min, double max) {
      return myPanel.addWidget (host, name, min, max);
   }

   public LabeledComponentBase addWidget (
      String labelText, HasProperties host, String name) {
      return myPanel.addWidget (labelText, host, name);
   }

   public LabeledComponentBase addWidget (
      String labelText, HasProperties host, String name, double min, double max) {
      return myPanel.addWidget (labelText, host, name, min, max);
   }

   private void doDone() {
      if (myIgnoreValidator != null) {
         String errMsg = myIgnoreValidator.validateSettings (myPanel);
         if (errMsg != null) {
            // like validator, but just ignore request
            return;
         }
      }
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

   public PropertyPanel getPanel() {
      return myPanel;
   }

   public OptionPanel getOptionPanel() {
      return myOptionPanel;
   }

}
