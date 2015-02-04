/**
 * Copyright (c) 2014, by the Authors: John Lloyd (UBC), Tracy Wilkinson (UBC) and
 * ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */package artisynth.core.gui.editorManager;

import java.awt.Frame;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;

import maspack.util.*;
import maspack.widgets.BooleanSelector;
import maspack.widgets.DoubleField;
import maspack.widgets.LabeledControl;
import maspack.widgets.OptionPanel;
import maspack.widgets.PropertyDialog;
import maspack.widgets.PropertyPanel;
import maspack.widgets.ValueChangeEvent;
import maspack.widgets.ValueChangeListener;
import maspack.properties.*;
import artisynth.core.driver.*;
import artisynth.core.gui.*;
import artisynth.core.gui.widgets.*;
import artisynth.core.mechmodels.*;
import artisynth.core.modelbase.*;

/**
 * Responsible for editing collsion settings within a MechModel.
 */
public class SetCollisionsDialog extends PropertyDialog
   implements ValueChangeListener {

   protected MechModel myMechModel;
   protected JButton mySetButton;

   protected DoubleField myFrictionField;
   protected BooleanSelector myEnabledField;

   private LinkedList<CollidablePair> myCollisionPairs;

   public SetCollisionsDialog (
      Frame owner, MechModel mech, ArrayList<Collidable> collidables) {
      super (owner, collidables.size() == 1 ?
             "Set self collision" : "Set collisions");
      setPanel (new PropertyPanel());
      myMechModel = mech;
      createDisplay();
      myCollisionPairs = createCollidablePairs (collidables);
      updateFieldValues();
      setModal (true);
   }

   protected void createDisplay() {
      myEnabledField = new BooleanSelector ("enabled", false);
      myEnabledField.setVoidValueEnabled (true);
      myEnabledField.addValueChangeListener (this);
      addWidget (myEnabledField);
      myFrictionField = new DoubleField ("friction", 0);
      myFrictionField.setVoidValueEnabled (true);
      myFrictionField.setRange (0, Double.POSITIVE_INFINITY);
      myFrictionField.addValueChangeListener (this);
      addWidget (myFrictionField);
      addWidget (new JSeparator());
      OptionPanel panel = new OptionPanel ("Set Cancel", this);
      addWidget (panel);
      mySetButton = panel.getButton ("Set");
      pack();
   }

   static LinkedList<CollidablePair> createCollidablePairs (
      ArrayList<Collidable> collidables) {
      LinkedList<CollidablePair> pairs = new LinkedList<CollidablePair>();
      if (collidables.size() == 1) {
         Collidable c0 = collidables.get(0);
         // self collision case
         pairs.add (new CollidablePair (c0, c0));
      }
      else {
         for (int i=0; i<collidables.size(); i++) {
            for (int j=i+1; j<collidables.size(); j++) {
               Collidable ci = collidables.get(i);
               Collidable cj = collidables.get(j);
               // ??? doesn't look like ci can equal cj 
               if ((ci == cj && !(ci instanceof RigidBody)) ||
                   (ci != cj)) {
                  pairs.add (new CollidablePair (ci, cj));
               }
            }
         }
      }
      return pairs;
   }

   public RemoveAddCommand createCommand() {
      CollisionBehavior behavior = new CollisionBehavior();
      behavior.setFriction (myFrictionField.getDoubleValue());
      behavior.setEnabled (myEnabledField.getBooleanValue());
      // LinkedList<CollidablePair> pairs =
      //    new LinkedList<CollidablePair>();
      // for (CollidablePair pair : myCollisionPairs) {
      //    pairs.add (pair);
      // }
      LinkedList<CollisionComponent> addList =
         new LinkedList<CollisionComponent>();
      LinkedList<CollisionComponent> removeList =
         new LinkedList<CollisionComponent>();
      CollisionManager colmanager = myMechModel.getCollisionManager();
      for (CollidablePair pair : myCollisionPairs) {
         CollisionComponent comp = colmanager.getCollisionComponent (pair);
         if (comp != null) {
            removeList.add (comp);
         }
         addList.add (new CollisionComponent (pair, behavior));
      }

      return new RemoveAddCommand (
         "Set collisions", removeList, addList,
         colmanager.collisionComponents());
      //    "Set collisions", myMechModel, pairs, behavior);
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

   private void setWidgetValue (LabeledControl widget, Object value) {
      widget.maskValueChangeListeners(true);
      widget.setValue (value);
      widget.maskValueChangeListeners(false);
   }

   private void updateSetButton() {
      mySetButton.setEnabled (
         !myEnabledField.valueIsVoid() &&
         !myFrictionField.valueIsVoid());
   }

   private void updateFieldValues () {
      Object friction = null;
      Object enabled = null;
      boolean first = true;
      for (CollidablePair pair : myCollisionPairs) {
         CollisionBehavior behavior =
            myMechModel.getCollisionBehavior (pair.getA(), pair.getB());
         System.out.println (
            "response " + pair.getA().getName()+
            " "+pair.getB().getName()+" "+behavior.isEnabled());
         if (first) {
            friction = behavior.getFriction();
            enabled = behavior.isEnabled();
            first = false;
         }
         else {
            if (!friction.equals (behavior.getFriction())) {
               friction = Property.VoidValue;
            }
            if (!enabled.equals (behavior.isEnabled())) {
               enabled = Property.VoidValue;
            }
         }
      }
      setWidgetValue (myEnabledField, enabled);
      setWidgetValue (myFrictionField, friction);
      updateSetButton ();
   }

   public void valueChange (ValueChangeEvent e) {
      updateSetButton();
   }
}
