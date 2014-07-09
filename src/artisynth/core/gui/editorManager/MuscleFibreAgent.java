/**
 * Copyright (c) 2014, by the Authors: John Lloyd (UBC), Tracy Wilkinson (UBC) and
 * ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */package artisynth.core.gui.editorManager;

import java.awt.Component;

import artisynth.core.mechmodels.Muscle;
import artisynth.core.modelbase.*;
import artisynth.core.femmodels.*;
import artisynth.core.driver.Main;
import artisynth.core.gui.*;
import maspack.render.*;
import maspack.widgets.LabeledComponent;
import maspack.widgets.LabeledWidget;
import maspack.widgets.PropertyPanel;

/**
 * Responsible for adding Muscles to a Model.
 */
public class MuscleFibreAgent extends AxialSpringAgent<Muscle> {

   private ComponentList<MuscleBundle> myBundleList = null;
   private boolean myInitializeBundle = false;
   private MuscleBundle myBundle;
   private PropertyPanel myBundlePanel;

   public MuscleFibreAgent (
      Main main, MuscleBundle bundle, CompositeComponent ancestor) {
     
      super (main, bundle.getFibres(), ancestor);
      myBundle = bundle;
   }

   /**
    * Used to indicate that the muscle bundle was newly created and this agent
    * should provide widgets to set its name and render properties. bundleList
    * should be the component list to which the bundle was added. A null
    * bundleList will disable bundle initialization. Should be called before
    * show() in order to have any effect.
    */
   public void setInitializeBundle (
      ComponentList<MuscleBundle> bundleList) {
     
      myBundleList = bundleList;
      myInitializeBundle = (bundleList != null);
   }

   private void initializeBundle () {
     
      // find the most recently added bundle in the list and use
      // that as a reference
      for (int i=myBundleList.size()-1; i>=0; i--) {
         MuscleBundle b = myBundleList.get(i);
        
         if (b != myBundle) {
            myBundle.setRenderProps(b.getRenderProps()); 
            return;
         }
      }
      // otherwise, have to initialize in a more ad-hoc way
      RenderProps.setLineRadius (myBundle, getDefaultLineRadius());
   }

   private void createBundleWidgets() { 
     
      myBundlePanel = new PropertyPanel ();
      LabeledWidget widget;
      widget = (LabeledWidget)myBundlePanel.addWidget (
         myBundle.getProperty("name"));
      widget.setLabelText ("bundle name");
      widget = (LabeledWidget)myBundlePanel.addWidget (
         myBundle.getProperty("renderProps"));
      widget.setLabelText ("bundle renderProps");
      myBundlePanel.setAlignmentX (Component.LEFT_ALIGNMENT);
      myContentPane.addWidget (myBundlePanel);
   }

   protected void createDisplay () { 
     
      createDisplayFrame ("Add Muscles");

      addComponentType (Muscle.class, new String[] 
         { "excitation" });

      addBasicProps (Muscle.class, new String[] { "maxForce", "muscleType" 
         });            

      if (myInitializeBundle) {
         initializeBundle ();
         createBundleWidgets();
      }

      createComponentList (
         new AxialSpringListWidget(myList, myAncestor));
      //      createSeparator();
      //      createTypeSelector();
      createSeparator();
      createNameField();
      createPropertyFrame ("Default fibre properties");
      //      createSeparator();
      createProgressBox();
      createInstructionBox();
      createContinuousAddToggle();
      createOptionPanel ("Add Done"); 
   }

   public void dispose() {
     
      super.dispose();
   }

   protected boolean isContextValid() {
     
      return (ComponentUtils.withinHierarchy (
                 myBundle, myMain.getRootModel()));
   }


}
