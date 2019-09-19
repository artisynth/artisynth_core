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
import maspack.widgets.GuiUtils;
import maspack.properties.*;

import artisynth.core.driver.*;
import artisynth.core.gui.*;
import artisynth.core.gui.selectionManager.*;
import artisynth.core.mechmodels.*;
import artisynth.core.modelbase.*;

/**
 * Responsible for attaching particles to each other within a mech model
 */
public class AttachParticleAgent
   extends AddComponentAgent<DynamicAttachmentComp> {
   protected MechModel myAncestor;
   protected ComponentList<DynamicAttachmentComp> myList;
   //   private boolean myContinuousAdd = false;
   private Particle myParticle;
   private Particle myTarget;
   //   private JCheckBox myContinuousAddToggle;

   private static HashMap<Class,ModelComponent> myPrototypeMap;
   //   private static RootModel myLastRootModel = null;

   /**
    * Not used because we don't have property panels for attachments.
    */
   protected void initializePrototype (ModelComponent comp, Class type) {
   }

   protected void setInitialState() {
      setState (State.SelectingParticle);
   }

   protected void resetState() {
      setState (State.Complete);
   }

   private class ParticleFilter implements SelectionFilter {
      public boolean objectIsValid (
         ModelComponent c, java.util.List<ModelComponent> currentSelections) {
         return (c instanceof Particle &&
                 !((Particle)c).isAttached() &&
                 ComponentUtils.withinHierarchy (c,(ModelComponent)myAncestor) &&
                 (myState != State.SelectingTarget || c != myParticle));
      }
   }

   private enum State {
      SelectingParticle, SelectingTarget, Confirm, Complete
   };

   private State myState = State.Complete;

   private void setState (State state) {
      switch (state) {
         case SelectingParticle: {
            myInstructionBox.setText ("Select particle to be attached");
            myAddButton.setText ("Stop");
            myAddButton.setActionCommand ("Stop");
            myProgressBox.setText ("");
            installSelectionFilter (new ParticleFilter());
            break;
         }
         case SelectingTarget: {
            myInstructionBox.setText ("Select particle to attach to");
            myAddButton.setText ("Stop");
            myAddButton.setActionCommand ("Stop");
            myProgressBox.setText (getParticleName (myParticle));
            installSelectionFilter (new ParticleFilter());
            break;
         }
         case Complete: {
            myInstructionBox.setText (
               "Click 'Attach' to attach another particle");
            myAddButton.setText ("Attach");
            myAddButton.setActionCommand ("Add");
            myProgressBox.setText ("");
            uninstallSelectionFilter();
            myParticle = null;
            myTarget = null;
            break;
         }
         case Confirm: {
            myInstructionBox.setText ("Hit OK to confirm, Done to cancel/quit");
            myAddButton.setText ("OK");
            myAddButton.setActionCommand ("OK");
            myProgressBox.setText (
               getParticleName (myParticle) + " - " +
               getParticleName (myTarget));
            uninstallSelectionFilter();
            break;
         }
         default: {
            throw new InternalErrorException ("Unhandled state " + state);
         }
      }
      myState = state;
   }

   public AttachParticleAgent (Main main, MechModel model) {
      super (main, model.attachments(), model);
      myList = model.attachments();
      myAncestor = model;
   }

   public void setParticless (Particle particleA, Particle particleB) {
      myParticle = particleA;
      myTarget = particleB;
      setState (State.Confirm);
   }

   protected void createDisplay() {
      createDisplayFrame ("Attach particles");

      addComponentType (PointParticleAttachment.class);

      createComponentList (
         "Existing attachments:", 
         new ParticleAttachmentList (myList, myAncestor));
      createSeparator();
      // createTypeSelector ("Spring type");
      // createPropertyFrame(null);
      // createSeparator();
      createProgressBox();
      createInstructionBox();
      //      createContinuousAddToggle();
      createOptionPanel ("Add Done");
   }

   private String getParticleName (Particle particle) {
      return ComponentUtils.getPathName (myAncestor, particle);
   }

   @Override
   public void selectionChanged (SelectionEvent e) {
      ModelComponent c = e.getLastAddedComponent();
      if (myState == State.SelectingParticle) {
         if (c instanceof Particle) {
            myParticle = (Particle)c;
            setState (State.SelectingTarget);
         }
      }
      else if (myState == State.SelectingTarget) {
         if (c instanceof Particle) {
            myTarget = (Particle)c;
            createAndAddAttachment();
            setState (State.SelectingParticle);
         }
      }
   }

   /**
    * Boiler plate - not actively used here because this
    * editing agent does not have default properties.
    */
   protected HashMap<Class,ModelComponent> getPrototypeMap() {
      if (myPrototypeMap == null) {
         myPrototypeMap = new HashMap<Class,ModelComponent>();
      }
      return myPrototypeMap;
   }

   private void createAndAddAttachment() {
      DynamicAttachmentComp ac =
         new PointParticleAttachment (myTarget, myParticle);
      uninstallSelectionFilter();
      if (myParticle.isAttached()) {
         GuiUtils.showError (myDisplay, "Particle already attached");         
         mySelectionManager.clearSelections();
      }
      else if (DynamicAttachmentWorker.containsLoop (ac, myParticle, null)) {
         GuiUtils.showError (myDisplay, "Attachment contains a loop");
         mySelectionManager.clearSelections();
      }
      else {
         addComponent (new AddComponentsCommand (
                          "attach particle-particle", ac, myList));
      }
      //      myParticle.setPosition (myTarget.getPosition());
   }

   public void actionPerformed (ActionEvent e) {
      String cmd = e.getActionCommand();
      if (cmd.equals ("Stop")) {
         setState (State.Complete);
         myMain.rerender();
      }
      else if (cmd.equals ("OK")) {
         createAndAddAttachment();
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

class ParticleParticleView extends SubListView<DynamicAttachmentComp> {

   public ParticleParticleView (ListView<DynamicAttachmentComp> view) {
      super (view);
   }         

   public boolean isMember (Object obj) {
      if (!(obj instanceof DynamicAttachmentComp)) {
         return false;
      }
      DynamicAttachmentComp ac = (DynamicAttachmentComp)obj;
      return (ac.numMasters() == 1 && ac.getMasters()[0] instanceof Particle);
   }
}      

class ParticleAttachmentList extends ComponentListWidget<DynamicAttachmentComp> {

   ParticleAttachmentList (
      ComponentListView<DynamicAttachmentComp> list,
      CompositeComponent ancestor) {
      super (new ParticleParticleView(list), ancestor);
   }

}
