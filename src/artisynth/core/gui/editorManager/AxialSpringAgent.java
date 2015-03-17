/**
 * Copyright (c) 2014, by the Authors: John Lloyd (UBC), Tracy Wilkinson (UBC) and
 * ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */package artisynth.core.gui.editorManager;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Component;
import java.awt.Font;
import java.awt.Cursor;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;

import maspack.render.*;
import maspack.geometry.*;
import maspack.matrix.*;
import maspack.util.*;
import maspack.widgets.StringField.*;
import maspack.properties.*;
import artisynth.core.driver.*;
import artisynth.core.gui.*;
import artisynth.core.gui.selectionManager.*;
import artisynth.core.mechmodels.*;
import artisynth.core.modelbase.*;
import artisynth.core.workspace.RootModel;

/**
 * Responsible for adding AxialSprings to a MechModel.
 */
public class AxialSpringAgent<C extends AxialSpring> extends
AddComponentAgent<C> {
   protected CompositeComponent myAncestor;
   protected ComponentList<C> myList;
   private boolean myContinuousAdd = false;
   private Point myPointA;
   private Point myPointB;
   private JCheckBox myContinuousAddToggle;
   private LinkedList<ModelComponent> myTmpList =
      new LinkedList<ModelComponent>();
   private CompositeState mySaveState = null;

   private static HashMap<Class,ModelComponent> myPrototypeMap;
   private static RootModel myLastRootModel = null;

   protected void initializePrototype (ModelComponent comp, Class type) {
      if (type == AxialSpring.class) {
         AxialSpring axial = (AxialSpring)comp;
         RenderProps.setLineRadius (axial, getDefaultLineRadius());
      }
      else if (type == Muscle.class) {
         Muscle muscle = (Muscle)comp;
         RenderProps.setLineRadius (muscle, getDefaultLineRadius());
      }
   }

   protected void setInitialState() {
      setState (State.SelectingPointA);
   }

   protected void resetState() {
      setState (State.Complete);
   }

   private class PointFilter implements SelectionFilter {
      public boolean objectIsValid (
         ModelComponent c, java.util.List<ModelComponent> currentSelections) {
         return (c instanceof Point &&
                 ComponentUtils.withinHierarchy (c, myAncestor) &&
                 (myState != State.SelectingPointB || c != myPointA));
      }
   }

   private enum State {
      SelectingPointA, SelectingPointB, Confirm, Complete
   };

   private State myState = State.Complete;

   private void setState (State state) {
      switch (state) {
         case SelectingPointA: {
            myInstructionBox.setText ("Select first point");
            myAddButton.setText ("Stop");
            myAddButton.setActionCommand ("Stop");
            myProgressBox.setText ("");
            installSelectionFilter (new PointFilter());
            myContinuousAddToggle.setEnabled (false);
            myTmpList.clear();
            break;
         }
         case SelectingPointB: {
            if (myContinuousAdd) {
               myInstructionBox.setText (
                  "Select next point or click 'Stop' to finish");
            }
            else {
               myInstructionBox.setText ("Select second point");
            }
            myAddButton.setText ("Stop");
            myAddButton.setActionCommand ("Stop");
            System.out.println ("setting state B");
            myProgressBox.setText (getPointName (myPointA));
            installSelectionFilter (new PointFilter());
            myContinuousAddToggle.setEnabled (false);
            break;
         }
         case Complete: {
            myInstructionBox.setText (" ");
            myAddButton.setText ("Add");
            myAddButton.setActionCommand ("Add");
            myProgressBox.setText ("");
            uninstallSelectionFilter();
            myContinuousAddToggle.setEnabled (true);
            myPointA = null;
            myPointB = null;
            myTmpList.clear();
            break;
         }
         case Confirm: {
            myInstructionBox.setText ("Hit OK to confirm, Done to cancel/quit");
            myAddButton.setText ("OK");
            myAddButton.setActionCommand ("OK");
            myProgressBox.setText (getPointName (myPointA) + " - "
            + getPointName (myPointB));
            uninstallSelectionFilter();
            myContinuousAddToggle.setEnabled (false);
            myTmpList.clear();
            break;
         }
         default: {
            throw new InternalErrorException ("Unhandled state " + state);
         }
      }
      myState = state;
   }

   public AxialSpringAgent (Main main, ComponentList<C> list,
   CompositeComponent ancestor) {
      super (main, list, ancestor);
      myList = list;
      myAncestor = ancestor;
   }

   public void setPoints (Point pointA, Point pointB) {
      myPointA = pointA;
      myPointB = pointB;
      setState (State.Confirm);
   }

   protected void createDisplay() {
      createDisplayFrame ("Add AxialSprings");

      addComponentType (AxialSpring.class, new String[] {"name"} );
      addComponentType (Muscle.class, new String[] { "excitation", "name" });
      addBasicProps (Muscle.class, new String[] { "renderProps", "muscleType"
         });

      createComponentList ("Existing axial springs:",
                           new AxialSpringListWidget (myList, myAncestor));
      createSeparator();
      createNameField();
      createTypeSelector ("Spring type");

      createPropertyFrame ("Default TYPE propeties:");
      // createSeparator();
      createProgressBox();
      createInstructionBox();
      createContinuousAddToggle();
      createOptionPanel ("Add Done");
   }

   protected void createContinuousAddToggle() {
      myContinuousAddToggle = new JCheckBox ("add continuously");
      myContinuousAddToggle.setSelected (myContinuousAdd);
      myContinuousAddToggle.addActionListener (this);
      addToContentPane (myContinuousAddToggle);
   }

   public void setContinuousAdd (boolean continuous) {
      if (continuous != myContinuousAdd) {
         myContinuousAdd = continuous;
         if (myContinuousAddToggle != null
             && myContinuousAddToggle.isSelected() != continuous) {
            myContinuousAddToggle.setSelected (continuous);
         }
      }
   }

   public boolean getContinuousAdd() {
      return myContinuousAdd;
   }

   private String getPointName (Point point) {
      return ComponentUtils.getPathName (myAncestor, point);
   }

   @Override
   public void selectionChanged (SelectionEvent e) {
      ModelComponent c = e.getLastAddedComponent();
      if (myState == State.SelectingPointA) {
         if (c instanceof Point) {
            myPointA = (Point)c;
            setState (State.SelectingPointB);
         }
      }
      else if (myState == State.SelectingPointB) {
         if (c instanceof Point) {
            myPointB = (Point)c;
            createAndAddSpring();
            if (!myContinuousAdd) {
               setState (State.Complete);
            }
            else {
               myPointA = myPointB;
               myPointB = null;
               myProgressBox.setText (getPointName (myPointA));
            }
         }
      }
   }

   protected HashMap<Class,ModelComponent> getPrototypeMap() {
      RootModel root = myMain.getRootModel();
      if (root != null && root != myLastRootModel) {
         myPrototypeMap = new HashMap<Class,ModelComponent>();
         myLastRootModel = root;
      }
      return myPrototypeMap;
   }

   private void createAndAddSpring() {
      AxialSpring spring;

      try {
         spring = (AxialSpring)myComponentType.newInstance();
      }
      catch (Exception e) {
         throw new InternalErrorException ("Cannot create instance of "
         + myComponentType + " or cast it to AxialSpring");
      }
      spring.setFirstPoint (myPointA);
      spring.setSecondPoint (myPointB);
      spring.setName (getNameFieldValue());
      myNameField.setValue (null);
      setProperties (spring, getPrototypeComponent (myComponentType));
      // update properties in the prototype as well ...
      setProperties (myPrototype, myPrototype);

      if (myContinuousAdd) {
         if (myTmpList.size() == 0) {
            mySaveState = myUndoManager.getModelState();
         }
         myTmpList.add (spring);
         myList.add ((C)spring);
         clearNameField();
         myMain.rerender(); // XXX shouldn't need this
      }
      else {
         uninstallSelectionFilter();
         addComponent (new AddComponentsCommand (
            "add AxialSpring", spring, myList));
      }
   }

   private void registerContinuousUndoCommand() {
      LinkedList<MutableCompositeComponent<?>> parents =
         new LinkedList<MutableCompositeComponent<?>>();
      LinkedList<ModelComponent> comps =
         (LinkedList<ModelComponent>)myTmpList.clone();
      for (int i = 0; i < comps.size(); i++) {
         parents.add (myList);
      }
      myUndoManager.addCommand (new AddComponentsCommand (
         "add AxialSprings", comps, parents), mySaveState);
      // select all springs just added
      uninstallSelectionFilter();
      mySelectionManager.clearSelections();
      for (ModelComponent c : comps) {
         mySelectionManager.addSelected (c);
      }
   }

   public void actionPerformed (ActionEvent e) {
      if (e.getSource() == myContinuousAddToggle) {
         System.out.println ("Toggle");
         setContinuousAdd (myContinuousAddToggle.isSelected());
      }
      else if (e.getActionCommand() == "Stop") {
         if (myContinuousAdd && myTmpList.size() > 0) {
            registerContinuousUndoCommand();
         }
         setState (State.Complete);
         myMain.rerender();
      }
      else if (e.getActionCommand() == "OK") {
         createAndAddSpring();
         setState (State.Complete);
         myMain.rerender();
      }
      else {
         super.actionPerformed (e);
      }
   }

   protected boolean isContextValid() {
      return (ComponentUtils.withinHierarchy (
                 myAncestor, myMain.getRootModel()));
   }

}

class AxialSpringListWidget<C extends AxialSpring>
   extends ComponentListWidget<C> {
   AxialSpringListWidget (
      ComponentListView<C> list, CompositeComponent ancestor) {
      super (list, ancestor);
   }

   @Override
   protected String getName (AxialSpring comp, CompositeComponent ancestor) {
      Point pointA = comp.getFirstPoint();
      Point pointB = comp.getSecondPoint();
      return (ComponentUtils.getPathName (ancestor, pointA) + " - "
              + ComponentUtils.getPathName (ancestor, pointB));
   }
}
