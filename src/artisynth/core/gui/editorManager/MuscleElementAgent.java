/**
 * Copyright (c) 2014, by the Authors: John Lloyd (UBC), Tracy Wilkinson (UBC) and
 * ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */package artisynth.core.gui.editorManager;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.*;

import javax.swing.*;

import artisynth.core.mechmodels.Muscle;
import artisynth.core.modelbase.*;
import artisynth.core.workspace.RootModel;
import artisynth.core.femmodels.*;
import artisynth.core.driver.Main;
import artisynth.core.gui.*;
import artisynth.core.gui.selectionManager.*;
import artisynth.core.gui.widgets.*;
import maspack.matrix.Vector3d;
import maspack.render.*;
import maspack.widgets.LabeledControl;
import maspack.widgets.PropertyPanel;
import maspack.properties.*;
import maspack.geometry.DelaunayInterpolator;

/**
 * Responsible for adding Muscles to a Model.
 */
public class MuscleElementAgent extends AddComponentAgent<MuscleElementDesc> {

   //   private ComponentList<MuscleBundle> myBundleList = null;
   // private boolean myInitializeBundle = false;
   private MuscleBundle myBundle;
   private FemModel3d myFemModel;
   private MuscleElementDescList myList;
   // private PropertyPanel myBundlePanel;
   private PropertyPanel myControlPanel;
   private JButton myAddButton;

   private InheritableProperty myModelWidgetSizeProp;
   private double mySavedModelWidgetSize;
   private PropertyMode mySavedModelWidgetMode;
   private InheritableProperty myBundleWidgetSizeProp;
   private double mySavedBundleWidgetSize;
   private PropertyMode mySavedBundleWidgetMode;

   private RerenderListener myRerenderListener;
   private HashSet<FemElement3dBase> myEligibleElements;
   private HashMap<FemElement3dBase,MuscleElementDesc> myElementMap;
   private DelaunayInterpolator myInterpolator;
   private Vector3d[] myFibreRestDirections;

   private static HashMap<Class,ModelComponent> myPrototypeMap;
   private static RootModel myLastRootModel = null;

   protected void initializePrototype (ModelComponent comp, Class type) {
      if (type == MuscleElementDesc.class) {
         MuscleElementDesc desc = (MuscleElementDesc)comp;
      }
   }   

   private class ElementFilter implements SelectionFilter {
      public boolean objectIsValid (
         ModelComponent c, java.util.List<ModelComponent> currentSelections) {
         return (c instanceof FemElement3d || c instanceof MuscleElementDesc);
      }
   }

   protected void setInitialState() {
           
   }

   protected void resetState() {

   }

   protected HashMap<Class,ModelComponent> getPrototypeMap() {
      RootModel root = myMain.getRootModel();
      if (root != null && root != myLastRootModel) {
         myPrototypeMap = new HashMap<Class,ModelComponent>();
         myLastRootModel = root;
      }
      return myPrototypeMap;
   }

   public MuscleElementAgent (
      Main main, MuscleBundle bundle, FemModel3d model) {
     
      super (main, bundle.getElements(), model);
      myRerenderListener = new RerenderListener();
      myList = bundle.getElements();
      myBundle = bundle;
      myFemModel = model;

      myElementMap = new HashMap<FemElement3dBase,MuscleElementDesc>();
      myEligibleElements = new LinkedHashSet<>();
      initializeEligibleElements();
      myInterpolator = myBundle.getFibreRestDistanceInterpolator();
      myFibreRestDirections = myBundle.getFibreRestDirections();
   }

   private void initializeEligibleElements () {
      myElementMap.clear();
      addToElementMap (myBundle.getElements());

      myEligibleElements.clear();
      myEligibleElements.addAll (
         getEligibleElements (mySelectionManager.getCurrentSelection()));
   }

   private void addToElementMap (Collection<MuscleElementDesc> list) {
      for (MuscleElementDesc d : list) {
         myElementMap.put (d.getElement(), d);         
      }
   }

   // /**
   //  * Used to indicate that the muscle bundle was newly created and this agent
   //  * should provide widgets to set its name and render properties. bundleList
   //  * should be the component list to which the bundle was added. A null
   //  * bundleList will disable bundle initialization. Should be called before
   //  * show() in order to have any effect.
   //  */
   // public void setInitializeBundle (
   //    ComponentList<MuscleBundle> bundleList) {
     
   //    myBundleList = bundleList;
   //    myInitializeBundle = (bundleList != null);
   // }

   // private void initializeBundle () {
     
   //    // find the most recently added bundle in the list and use
   //    // that as a reference
   //    for (int i=myBundleList.size()-1; i>=0; i--) {
   //       MuscleBundle b = myBundleList.get(i);
        
   //       if (b != myBundle) {
   //          myBundle.setRenderProps(b.getRenderProps()); 
   //          return;
   //       }
   //    }
   //    // otherwise, have to initialize in a more ad-hoc way
   //    RenderProps.setLineRadius (myBundle, getDefaultLineRadius());
   // }


   private void createControls() {
      Property prop = myFemModel.getProperty("elementWidgetSize");
      if (prop != null && prop instanceof InheritableProperty) {
         myModelWidgetSizeProp = (InheritableProperty)prop;
         mySavedModelWidgetSize = (Double)myModelWidgetSizeProp.get();
         mySavedModelWidgetMode = myModelWidgetSizeProp.getMode();
         myModelWidgetSizeProp.set (0.5);
         LabeledControl widget =
            (LabeledControl)myContentPane.addWidget (prop);
         widget.setLabelText ("modelElementSize");
         widget.addValueChangeListener (myRerenderListener);
      }
      else {
         System.out.println (
            "Warning: property 'elementWidgetSize' not found in FemModel3d");
      }
      prop = myBundle.getProperty("elementWidgetSize");
      if (prop != null && prop instanceof InheritableProperty) {
         myBundleWidgetSizeProp = (InheritableProperty)prop;
         mySavedBundleWidgetSize = (Double)myBundleWidgetSizeProp.get();
         mySavedBundleWidgetMode = myBundleWidgetSizeProp.getMode();
         myBundleWidgetSizeProp.set (0.6);
         LabeledControl widget =
            (LabeledControl)myContentPane.addWidget (prop);
         widget.setLabelText ("bundleElementSize");
         widget.addValueChangeListener (myRerenderListener);
      }
      else {
         System.out.println (
            "Warning: property 'elementWidgetSize' not found in MuscleBundle");
      }    
   }

   // private void createBundleWidgets() { 
     
   //    myBundlePanel = new PropertyPanel ();
   //    LabeledWidget widget;
   //    widget = (LabeledWidget)myBundlePanel.addWidget (
   //       myBundle.getProperty("name"));
   //    widget.setLabelText ("bundle name");
   //    widget = (LabeledWidget)myBundlePanel.addWidget (
   //       myBundle.getProperty("renderProps"));
   //    widget.setLabelText ("bundle renderProps");
   //    myBundlePanel.setAlignmentX (Component.LEFT_ALIGNMENT);
   //    myContentPane.addWidget (myBundlePanel);
   // }

   protected void createDisplay () { 
     
      createDisplayFrame ("Add Muscle Elements");

      addComponentType (MuscleElementDesc.class, new String[] 
         { "excitation" });

      //addBasicProps (Muscle.class, new String[] { "maxForce", "muscleType" 
      //   });            

      // if (myInitializeBundle) {
      //    initializeBundle ();
      //    createBundleWidgets();
      // }

      createComponentList (
         "Elements associated with bundle:",
         new MuscleElementDescListWidget(myList, myAncestor));
      //      createSeparator();
      //      createTypeSelector();
      //createPropertyFrame ("Element Properties");
      //      createSeparator();
      createControls();
      //createProgressBox();
      createInstructionBox();
      myInstructionBox.setText ("Select elements to add to the bundle");
      //createContinuousAddToggle();
      createOptionPanel ("Add Done"); 
      installSelectionFilter (new ElementFilter());
      myAddButton = myOptionPanel.getButton ("Add");
      myAddButton.setEnabled (myEligibleElements.size() > 0);
   }

   public void dispose() {
      if (myBundleWidgetSizeProp != null) {
         if (mySavedBundleWidgetMode == PropertyMode.Explicit) {
            myBundleWidgetSizeProp.set (mySavedBundleWidgetSize);
         }
         else {
            myBundleWidgetSizeProp.setMode (mySavedBundleWidgetMode);
         }
      }
      if (myModelWidgetSizeProp != null) {
         if (mySavedModelWidgetMode == PropertyMode.Explicit) {
            myModelWidgetSizeProp.set (mySavedModelWidgetSize);
         }
         else {
            myModelWidgetSizeProp.setMode (mySavedModelWidgetMode);
         }
      }
      uninstallSelectionFilter();
      myMain.rerender();
      super.dispose();
   }

   private void addElements (HashSet<FemElement3dBase> set) {
      LinkedList<ModelComponent> descList =
         new LinkedList<ModelComponent>();

      for (FemElement3dBase e : set) {
         MuscleElementDesc desc = new MuscleElementDesc();
         desc.setElement (e);
         if (myInterpolator != null) {
            desc.interpolateDirection (myInterpolator, myFibreRestDirections);
         }
         else {
            desc.setDirection (Vector3d.X_UNIT);
         }
         descList.add (desc);
      }
      AddComponentsCommand cmd =
         new AddComponentsCommand (
            "add element descs", descList, myBundle.getElements());
      myUndoManager.saveStateAndExecute (cmd);
      mySelectionManager.clearSelections();
      myEligibleElements.clear(); // paranoid; should happen with clear selections
   }

   public void actionPerformed (ActionEvent e) {
      String cmd = e.getActionCommand();
      if (cmd.equals ("Add")) {
         addElements (myEligibleElements);
      }
      else {
         super.actionPerformed (e);
      }
   }

   protected boolean isContextValid() {
     
      return (ComponentUtils.withinHierarchy (
                 myBundle, myMain.getRootModel()));
   }

   LinkedList<FemElement3d> getEligibleElements (List<ModelComponent> comps) {
      LinkedList<FemElement3d> list = new LinkedList<FemElement3d>();
      for (ModelComponent c : comps) {
         if (c instanceof FemElement3d) {
            FemElement3d e = (FemElement3d)c;
            if (e.getGrandParent() == myFemModel &&
                myElementMap.get (e) == null) {
               list.add (e);
            }
         }
      }
      return list;
   }

   @Override
      public void selectionChanged (SelectionEvent e) {
      myEligibleElements.addAll (
         getEligibleElements (e.getAddedComponents()));
      myEligibleElements.removeAll (
         getEligibleElements (e.getRemovedComponents()));
      myAddButton.setEnabled (myEligibleElements.size() > 0);
   }

   @Override
      public void componentChanged (ComponentChangeEvent e) {
      initializeEligibleElements();
      super.componentChanged (e);
   }
}

class MuscleElementDescListWidget<C extends MuscleElementDesc>
   extends ComponentListWidget<C> {
   MuscleElementDescListWidget (
      ComponentListView<C> list, CompositeComponent ancestor) {
      super (list, ancestor);
   }

   @Override
   protected String getName (
      MuscleElementDesc comp, CompositeComponent ancestor) {
      FemElement elem = comp.getElement();
      return ComponentUtils.getPathName (ancestor, elem);
   }
}
