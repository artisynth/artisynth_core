/**
 * Copyright (c) 2014, by the Authors: John Lloyd (UBC), Tracy Wilkinson (UBC) and
 * ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */package artisynth.core.gui.editorManager;

import java.awt.event.ActionEvent;
import java.util.HashMap;

import maspack.util.InternalErrorException;
import artisynth.core.driver.Main;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.modelbase.ComponentList;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.MutableCompositeComponent;
import artisynth.core.modelbase.Model;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.workspace.RootModel;

/**
 * Responsible for adding MechModels to a RootModel
 */
public class MechModelAgent extends AddComponentAgent<MechModel> {
   private RootModel myRootModel;

   private static HashMap<Class,ModelComponent> myPrototypeMap;
   private static RootModel myLastRootModel = null;

   protected void initializePrototype (ModelComponent comp, Class type) {
      if (type == MechModel.class) {
      }
      else {
         throw new InternalErrorException ("unimplemented type " + type);
      }
   }

   public MechModelAgent (Main main, RootModel rootmodel) {
      super (main, (ComponentList<Model>)rootmodel.models(), rootmodel);
      myRootModel = rootmodel;
   }

   protected void createDisplay() {
      createDisplayFrame ("Add MechModel");
      addComponentType (MechModel.class, new String[] {});
      addBasicProps (MechModel.class,
                     new String[] {"name", "maxStepSize", "integrator" });

      // createComponentList (
      //    new ComponentListWidget<Model> (
      //       myRootModel.models(), myRootModel));
      createPropertyFrame("Model properties");
      // createSeparator();
      createOptionPanel ("Add Cancel");
   }

   protected HashMap<Class,ModelComponent> getPrototypeMap() {
      RootModel root = myMain.getRootModel();
      if (root != null && root != myLastRootModel) {
         myPrototypeMap = new HashMap<Class,ModelComponent>();
         myLastRootModel = root;
      }
      return myPrototypeMap;
   }

   private void createAndAddMechModel() {
      MechModel model = new MechModel();
      setProperties (model, getPrototypeComponent (myComponentType));
      // update properties in the prototype as well ...
      setProperties (myPrototype, myPrototype);

      addComponent (new AddComponentsCommand (
         "add MechModel", model, (MutableCompositeComponent<?>)myRootModel.models()));

   }

   public void actionPerformed (ActionEvent e) {
      String cmd = e.getActionCommand();
      // if (cmd.equals ("Add+Done")) {
      //    createAndAddMechModel();
      //    myMain.rerender();
      //    setInitialState();
      //    super.actionPerformed (new ActionEvent (
      //       e.getSource(), e.getID(), "Done"));
      // }
      if (cmd.equals ("Add")) {
         createAndAddMechModel();
         myMain.rerender();
         myDisplay.setVisible (false);
         dispose();  
      }
      else {
         super.actionPerformed (e);
      }
   }

   protected boolean isContextValid() {
      return (ComponentUtils.withinHierarchy (
                 myRootModel, myMain.getRootModel()));
   }

   protected void resetState() {
   }

   protected void setInitialState() {
   }

}
