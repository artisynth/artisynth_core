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
import artisynth.core.gui.widgets.*;
import artisynth.core.mechmodels.*;
import artisynth.core.femmodels.*;
import artisynth.core.modelbase.*;

/**
 * Responsible for attaching particles to each other within a mech model
 */
public class AttachParticleFemAgent 
   extends AddComponentAgent<DynamicAttachmentComp> {
   protected MechModel myAncestor;
   protected ComponentList<DynamicAttachmentComp> myList;
   private boolean myProjectPoints = false;
   private FemModel3d myFem;
   private Particle myParticle;
   private JCheckBox myProjectPointsToggle;
   private boolean mySelectionMasked = false;

   private static HashMap<Class,ModelComponent> myPrototypeMap;
   //   private static RootModel myLastRootModel = null;

   /**
    * Not used because we don't have property panels for attachments.
    */
   protected void initializePrototype (ModelComponent comp, Class type) {
   }

   protected void setInitialState() {
      setState (State.SelectingParticles);
   }

   protected void resetState() {
      setState (State.Complete);
   }

   private class ParticleFilter implements SelectionFilter {
      public boolean objectIsValid (
         ModelComponent c, java.util.List<ModelComponent> currentSelections) {
         return (c instanceof Particle &&
                 // make sure particle is not a node of the FEM itself
                 !ComponentUtils.isAncestorOf (myFem, c) && 
                 !((Particle)c).isAttached() &&
                 ComponentUtils.withinHierarchy (c, (ModelComponent)myAncestor));
      }
   }

   private enum State {
      SelectingParticles, Confirm, Complete
   };

   private State myState = State.Complete;

   private void setState (State state) {
      switch (state) {
         case SelectingParticles: {
            myInstructionBox.setText ("Select particle to be attached");
            myAddButton.setText ("Stop");
            myAddButton.setActionCommand ("Stop");
            installSelectionFilter (new ParticleFilter());
            break;
         }
         case Complete: {
            myInstructionBox.setText (
               "Click 'Attach' to attach more particles");
            myAddButton.setText ("Attach");
            myAddButton.setActionCommand ("Add");
            uninstallSelectionFilter();
            break;
         }
         case Confirm: {
            myInstructionBox.setText ("Hit OK to confirm, Done to cancel/quit");
            myAddButton.setText ("OK");
            myAddButton.setActionCommand ("OK");
            uninstallSelectionFilter();
            break;
         }
         default: {
            throw new InternalErrorException ("Unhandled state " + state);
         }
      }
      myState = state;
   }

   public AttachParticleFemAgent (Main main, MechModel model, FemModel3d fem) {
      super (main, model.attachments(), model);
      myList = model.attachments();
      myAncestor = model;
      myFem = fem;
   }

//    public void setParticles (Particle particleA, Particle particleB) {
//       myParticle = particleA;
//       myTarget = particleB;
//       setState (State.Confirm);
//    }

   protected void createDisplay() {
      createDisplayFrame ("Attach particles to body");

      addComponentType (PointFrameAttachment.class);

      createComponentList (
         "Existing attachments:", 
         new ParticleFemAttachmentList (myList, myAncestor));
      createSeparator();
      // createTypeSelector ("Spring type");
      // createPropertyFrame(null);
      // createSeparator();
      //      createProgressBox();
      createInstructionBox();
      createProjectPointsToggle();
      createOptionPanel ("Add Done");
   }

   protected void createProjectPointsToggle() {
      myProjectPointsToggle = new JCheckBox ("project points onto surface");
      myProjectPointsToggle.setSelected (myProjectPoints);
      myProjectPointsToggle.addActionListener (this);
      addToContentPane (myProjectPointsToggle);
   }

   public void setProjectPoints (boolean project) {
      if (project != myProjectPoints) {
         myProjectPoints = project;
         if (myProjectPointsToggle != null &&
             myProjectPointsToggle.isSelected() != project) {
            myProjectPointsToggle.setSelected (project);
         }
      }
   }

   private String getName (ModelComponent comp) {
      return ComponentUtils.getPathName (myAncestor, comp);
   }

   @Override
   public void selectionChanged (SelectionEvent e) {
      if (!mySelectionMasked &&
          e.getAddedComponents().size() > 0 &&
          myState == State.SelectingParticles) {
         createAndAddAttachments (e.getAddedComponents());
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

   private void createAndAddAttachments(LinkedList<ModelComponent> list) {
      LinkedList<ModelComponent> attachments = new LinkedList<ModelComponent>();
      LinkedList<MutableCompositeComponent<?>> parents =
         new LinkedList<MutableCompositeComponent<?>>();

      for (ModelComponent c : list) {
         if (c instanceof Particle) {
            Point3d loc = new Point3d();
            Particle p = (Particle)c;
            FemElement3dBase elem;
            if (myProjectPoints) {
                elem = myFem.findNearestSurfaceElement (loc, p.getPosition());
            }
            else {
                elem = myFem.findNearestElement (loc, p.getPosition());
            }
            PointFem3dAttachment ax =
               PointFem3dAttachment.create (p, elem, loc, 1e-5);
            attachments.add (ax);
            parents.add (myList);
         }
      }
      if (attachments.size() > 0) {
         if (attachedOrContainLoops (attachments)) {
            mySelectionManager.clearSelections();
            return;
         }
         else {
            // int i = 0;
            // // update the attachments and set the point locations
            // for (ModelComponent c : attachments) {
            //    PointFem3dAttachment ax = (PointFem3dAttachment)c;
            //    i++;
            // }
            mySelectionMasked = true;
            addComponent (new AddComponentsCommand (
                             "attach particles to fem", attachments, parents));
            mySelectionMasked = false;
         }
      }
   }

   private boolean attachedOrContainLoops (
      LinkedList<ModelComponent> attachments) {
      
      LinkedList<DynamicAttachment> list = new LinkedList<DynamicAttachment>();
      for (ModelComponent c : attachments) {
         DynamicAttachment a = (DynamicAttachment)c;
         if (a.getSlave().isAttached()) {
            String path = ComponentUtils.getPathName(a.getSlave());
            GuiUtils.showError (myDisplay, "point "+path+" is attached");
            return true;
         }
         list.add (a);
      }
      if (DynamicAttachmentWorker.containsLoops (list)) {
         GuiUtils.showError (myDisplay, "attachments contain loops");
         return true;
      }
      return false;      
   }

   public void actionPerformed (ActionEvent e) {
      String cmd = e.getActionCommand();
      if (cmd.equals ("Stop")) {
         setState (State.Complete);
         myMain.rerender();
      }
//       else if (cmd.equals ("OK")) {
//          createAndAddAttachment();
//          setState (State.Complete);
//          myMain.rerender();
//       }
      else if (e.getSource() == myProjectPointsToggle) {
         setProjectPoints (myProjectPointsToggle.isSelected());
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

class ParticleFemView extends SubListView<DynamicAttachmentComp> {

   public ParticleFemView (ListView<DynamicAttachmentComp> view) {
      super (view);
   }         

   public boolean isMember (Object obj) {
      if (!(obj instanceof PointFem3dAttachment)) {
         return false;
      }
      PointFem3dAttachment a = (PointFem3dAttachment)obj;
      return (a.numMasters() >= 1);
   }
}      

class ParticleFemAttachmentList
   extends ComponentListWidget<DynamicAttachmentComp> {

   ParticleFemAttachmentList (
      ComponentListView<DynamicAttachmentComp> list,
      CompositeComponent ancestor) {
      super (new ParticleFemView(list), ancestor);
   }
}
