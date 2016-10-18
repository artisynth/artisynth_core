/**
 * Copyright (c) 2014, by the Authors: John Lloyd (UBC), Tracy Wilkinson (UBC) and
 * ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */package artisynth.core.gui.editorManager;

import java.awt.Frame;
import java.awt.Dimension;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;

import maspack.util.*;
import maspack.widgets.BooleanSelector;
import maspack.widgets.DoubleField;
import maspack.widgets.LabeledComponent;
import maspack.widgets.OptionPanel;
import maspack.widgets.PropertyDialog;
import maspack.widgets.PropertyPanel;
import maspack.properties.*;
import artisynth.core.driver.*;
import artisynth.core.gui.*;
import artisynth.core.gui.widgets.*;
import artisynth.core.mechmodels.*;
import artisynth.core.modelbase.*;

/**
 * Responsible for editing collsion settings within a MechModel.
 */
public class DefaultCollisionsDialog extends PropertyDialog {

   protected MechModel myMechModel;
   protected JButton mySetButton;

   protected BooleanSelector myRREnabled;
   protected BooleanSelector myRDEnabled;
   protected BooleanSelector myDDEnabled;
   protected BooleanSelector myDSEnabled;

   protected DoubleField myRRFriction;
   protected DoubleField myRDFriction;
   protected DoubleField myDDFriction;
   protected DoubleField myDSFriction;

   public DefaultCollisionsDialog (Frame owner, MechModel mech) {
      super (owner, "Set default collisions");
      setPanel (new PropertyPanel());
      myMechModel = mech;
      createDisplay();
      updateFieldValues();
      setModal (true);
   }

   private void addWidgetHeader (LabeledComponent comp) {

      JLabel enableLabel = new JLabel("enable");
      JLabel frictionLabel = new JLabel("friction");

      int enableLabelWidth = enableLabel.getPreferredSize().width;
      int frictionLabelWidth = frictionLabel.getPreferredSize().width;
      int spacing = comp.getSpacing();

      int labelFieldWidth = comp.getMajorComponent(0).getPreferredSize().width;
      int enableFieldWidth = comp.getMajorComponent(1).getPreferredSize().width;
      int frictionFieldWidth = comp.getMajorComponent(2).getPreferredSize().width;

      JPanel header = new JPanel();
      header.setLayout (new BoxLayout (header, BoxLayout.X_AXIS));
      int w = (labelFieldWidth + 2*spacing + enableFieldWidth/2 -
               enableLabelWidth/2);
      header.add (Box.createRigidArea (new Dimension (w, 0)));
      header.add (enableLabel);
      w = (enableFieldWidth/2 + spacing + frictionFieldWidth/2 -
           enableLabelWidth/2 - frictionLabelWidth/2);
      header.add (Box.createRigidArea (new Dimension (w, 0)));
      header.add (frictionLabel);

      addWidget (header, 0);
   }

   protected void createDisplay() {
      myRREnabled = new BooleanSelector ("rigid-rigid", false);
      myRDEnabled = new BooleanSelector ("rigid-deformable", false);
      myDDEnabled = new BooleanSelector ("deformable-deformable", false);
      myDSEnabled = new BooleanSelector ("deformable-self", false);

      myRRFriction = new DoubleField ("", 0);
      myRDFriction = new DoubleField ("", 0);
      myDDFriction = new DoubleField ("", 0);
      myDSFriction = new DoubleField ("", 0);

      myRREnabled.addMajorComponent (myRRFriction);
      myRDEnabled.addMajorComponent (myRDFriction);
      myDDEnabled.addMajorComponent (myDDFriction);
      myDSEnabled.addMajorComponent (myDSFriction);

      addWidget (myRREnabled);
      addWidget (myRDEnabled);
      addWidget (myDDEnabled);
      addWidget (myDSEnabled);
      addWidget (new JSeparator());

      addWidgetHeader (myRREnabled);

      OptionPanel panel = new OptionPanel ("Set Cancel", this);
      addWidget (panel);
      mySetButton = panel.getButton ("Set");
      pack();
   }

   public SetDefaultCollisionsCommand createCommand() {

      LinkedHashMap<CollidablePair,CollisionBehavior> settings = 
         new LinkedHashMap<CollidablePair,CollisionBehavior>();

      updateSettings (
         settings, myRREnabled, Collidable.Rigid, Collidable.Rigid);
      updateSettings (
         settings, myRDEnabled, Collidable.Rigid, Collidable.Deformable);
      updateSettings (
         settings, myDDEnabled, Collidable.Deformable, Collidable.Deformable);
      updateSettings (
         settings, myDSEnabled, Collidable.Deformable, Collidable.Self);

      return new SetDefaultCollisionsCommand (
         "set default collisions", settings, myMechModel);
   }

   public void actionPerformed (ActionEvent e) {
      String cmd = e.getActionCommand();
      if (cmd.equals ("Set")) {
         myReturnValue = OptionPanel.OK_OPTION;
         setVisible (false);
         dispose();
      }
      else {
         super.actionPerformed (e);
      }
   }

   private void updateSettings (
      LinkedHashMap<CollidablePair,CollisionBehavior> settings,
      BooleanSelector enabledField, Collidable col0, Collidable col1) {

      DoubleField frictionField = (DoubleField)enabledField.getMajorComponent(2);
      boolean enabled = enabledField.getBooleanValue();
      double friction = frictionField.getDoubleValue();
      CollisionBehavior behavior = new CollisionBehavior(enabled, friction);
      settings.put (new CollidablePair (col0, col1), behavior);
   }

   private void updateWidget (
      BooleanSelector enabledField, 
      Collidable.Group col0, Collidable.Group col1) {

      DoubleField frictionField = (DoubleField)enabledField.getMajorComponent(2);
      CollisionBehavior behavior =
         myMechModel.getDefaultCollisionBehavior (col0, col1);
      enabledField.setValue (behavior.isEnabled());
      frictionField.setValue (behavior.getFriction());
   }

   private void updateFieldValues () {
      updateWidget (myRREnabled, Collidable.Rigid, Collidable.Rigid);
      updateWidget (myRDEnabled, Collidable.Rigid, Collidable.Deformable);
      updateWidget (myDDEnabled, Collidable.Deformable, Collidable.Deformable);
      updateWidget (myDSEnabled, Collidable.Deformable, Collidable.Self);
   }
}
