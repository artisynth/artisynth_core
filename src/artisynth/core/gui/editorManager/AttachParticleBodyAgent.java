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
import artisynth.core.modelbase.*;

/**
 * Responsible for attaching particles to each other within a mech model
 */
public class AttachParticleBodyAgent 
   extends AddComponentAgent<DynamicAttachmentComp> {
   protected MechModel myAncestor;
   protected ComponentList<DynamicAttachmentComp> myList;
   private boolean myProjectPoints = false;
   private RigidBody myBody;
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

   public AttachParticleBodyAgent (Main main, MechModel model, RigidBody body) {
      super (main, model.attachments(), model);
      myList = model.attachments();
      myAncestor = model;
      myBody = body;
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
         new ParticleFrameAttachmentList (myList, myAncestor));
      createSeparator();
      // createTypeSelector ("Spring type");
      // createPropertyFrame(null);
      // createSeparator();
      //      createProgressBox();
      createInstructionBox();
      if (myBody.getMesh() != null) {
         createProjectPointsToggle();
      }
      createOptionPanel ("Add Done");
   }

   protected void createProjectPointsToggle() {
      myProjectPointsToggle = new JCheckBox ("project points onto body");
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
      
      if (myProjectPoints) {
         PolygonalMesh mesh = myBody.getMesh();
         //OBBTree obbt = mesh.getObbtree();
         Point3d proj = new Point3d();
         Vector2d coords = new Vector2d();
         //TriangleIntersector isect = new TriangleIntersector();
         for (ModelComponent c : list) {
            if (c instanceof Particle) {
               Particle p = (Particle)c;
               BVFeatureQuery.getNearestFaceToPoint (
                  proj, coords, mesh, p.getPosition());
               //               p.setPosition (proj);
               proj.inverseTransform (mesh.getMeshToWorld());
               attachments.add (new PointFrameAttachment (myBody, p, proj));
               parents.add (myList);
            }
         }
      }
      else {
         for (ModelComponent c : list) {
            if (c instanceof Particle) {
               attachments.add (new PointFrameAttachment (myBody, (Particle)c));
               parents.add (myList);
            }
         }
      }
      if (attachments.size() > 0) {
         if (attachedOrContainLoops (attachments)) {
            mySelectionManager.clearSelections();
            return;
         }
         else {
            mySelectionMasked = true;
            addComponent (new AddComponentsCommand (
                             "attach particles to body", attachments, parents));
            mySelectionMasked = false;
         }
      }
         //      myParticle.setPosition (myTarget.getPosition());
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

class ParticleFrameView extends SubListView<DynamicAttachmentComp> {

   public ParticleFrameView (ListView<DynamicAttachmentComp> view) {
      super (view);
   }         

   public boolean isMember (Object obj) {
      if (!(obj instanceof DynamicAttachmentComp)) {
         return false;
      }
      DynamicAttachmentComp ac = (DynamicAttachmentComp)obj;
      return (ac.numMasters() == 1 && ac.getMasters()[0] instanceof Frame);
   }
}      

class ParticleFrameAttachmentList
   extends ComponentListWidget<DynamicAttachmentComp> {

   ParticleFrameAttachmentList (
      ComponentListView<DynamicAttachmentComp> list,
      CompositeComponent ancestor) {
      super (new ParticleFrameView(list), ancestor);
   }
}
