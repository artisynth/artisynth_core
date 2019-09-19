/**
 * Copyright (c) 2014, by the Authors: John Lloyd (UBC), Tracy Wilkinson (UBC) and
 * ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */package artisynth.core.gui.editorManager;

import java.awt.Rectangle;
import java.awt.Frame;
import java.util.*;

import maspack.matrix.Point3d;
import maspack.widgets.GuiUtils;
import maspack.widgets.OptionPanel;
import artisynth.core.driver.Main;
import artisynth.core.gui.*;
import artisynth.core.mechmodels.*;
import artisynth.core.modelbase.*;
import artisynth.core.gui.selectionManager.SelectionManager;

import javax.swing.*;

/**
 * Provides editing actions for MechModel
 */
public class MechModelEditor extends EditorBase {

   // MechModel myEditCollisionsModel = null;
   // EditCollisionsAgent myEditCollisionsAgent = null;

   public MechModelEditor (Main main, EditorManager editManager) {
      super (main, editManager);
   }

   private MechModel getAttachmentModel (DynamicComponent c) {
      DynamicAttachment at = c.getAttachment();
      if (at instanceof DynamicAttachmentComp) {
         CompositeComponent gp =
            ComponentUtils.getGrandParent((DynamicAttachmentComp)at);
         if (gp instanceof MechModel) {
            return (MechModel)gp;
         }
      }
      return null;
   }

   public static MechModel lowestCommonModel (
      LinkedList<? extends ModelComponent> comps) {
      ModelComponent ancestor =
         ComponentUtils.findCommonAncestor (comps);
      while (ancestor != null) {
         if (ancestor instanceof MechModel) {
            return (MechModel)ancestor;
         }
         else {
            ancestor = ancestor.getParent();
         }
      }
      return null;
   }

   /**
    * Returns true if the selection list contains a set of components
    &amp; for which collisions can be set.
    */
   public boolean containsCollidableSelection (
      LinkedList<ModelComponent> selection) {
      
      int numRigidBodies = 0;
      for (ModelComponent c : selection) {
         if (!(c instanceof Collidable)) {
            return false;
         }
         else if (c instanceof RigidBody) {
            numRigidBodies++;
         }
      }
      // if the selection is only rigid bodies, we must have at least two
      System.out.println ("num bodies=" + numRigidBodies);
      if (numRigidBodies == selection.size()) {
         return numRigidBodies >= 2;
      }
      else {
         return true;
      }
   }

   public void addActions (EditActionMap actions, SelectionManager selManager) {
      LinkedList<ModelComponent> selection = selManager.getCurrentSelection();

      boolean hasAttachedParticles = false;
      MechModel attachedParticlesModel = null;
      int numCollidables = 0;
      int numDeformables = 0;

      for (ModelComponent c : selection) {
         if (c instanceof Particle && ((Particle)c).isAttached()) {
            MechModel m = getAttachmentModel ((Particle)c);
            if (m != null) {
               if (attachedParticlesModel == null) {
                  attachedParticlesModel = m;
                  hasAttachedParticles = true;
               }
               else if (m != attachedParticlesModel) {
                  hasAttachedParticles = false;
                  break;
               }
            }
         }
         if (c instanceof Collidable) {
            numCollidables++;
            if (!(c instanceof RigidBody)) {
               numDeformables++;
            }
         }
      }
      if (containsSingleSelection (selection, MechModel.class)) {
         MechModel mechMod = (MechModel)selection.get(0);
         actions.add (this, "Add FemModel ...", EXCLUSIVE);
         actions.add (this, "Add RigidBodyConnectors ...", EXCLUSIVE);
         actions.add (this, "Add FrameMarkers ...", EXCLUSIVE);
         actions.add (this, "Add AxialSprings ...", EXCLUSIVE);
         actions.add (this, "Add Particles ...", EXCLUSIVE);
         actions.add (this, "Add RigidBody ...", EXCLUSIVE);
         actions.add (this, "Attach Particles ...", EXCLUSIVE);
         actions.add (this, "Set default collisions ...", EXCLUSIVE);
         int flag = 0;
         CollisionManager colmanager = mechMod.getCollisionManager();
         if (colmanager.numBehaviors() == 0) {
            flag = DISABLED;
         }
         actions.add (this, "Remove collision overrides", flag);
      }
      else if (containsSingleSelection (selection, RigidBody.class)) {
         actions.add (this, "Add FrameMarkers ...", EXCLUSIVE);
      }
      else if (containsDoubleSelection (selection, Point.class)) {
         if (MechModel.lowestCommonModel (
                selection.get (0), selection.get (1)) != null) {
            actions.add (this, "Add AxialSpring ...", EXCLUSIVE);
            actions.add (this, "Compute distance");
         }
      }
      else if (containsDoubleSelection (selection, RigidBody.class) &&
               MechModel.lowestCommonModel (
                  selection.get (0), selection.get (1)) != null) {
         actions.add (this, "Add BodyConnector ...", EXCLUSIVE);
         actions.add (this, "Add FrameSpring");
      }
      else if (hasAttachedParticles) {
         actions.add (this, "Detach particles");
      }
      if (numCollidables == selection.size()) {
         if (numCollidables > 1) {
            actions.add (this, "Set collisions ...");
         }
         else if (numDeformables == 1) {
            actions.add (this, "Set self collision ...");            
         }
      }
   }

   public void applyAction (
      String actionCommand, LinkedList<ModelComponent> selection,
      Rectangle popupBounds) {

      // if (selection.size() == 2)
      // { if (actionCommand == "Get path")
      // { System.out.println (CompositeUtils.getCompactPathNameX (
      // selection.get(0), selection.get(1)));
      // }
      // }
      if (containsSingleSelection (selection, MechModel.class)) {
         MechModel model = (MechModel)selection.get (0);
         if (actionCommand == "Add RigidBodyConnectors ...") {
            if (myEditManager.acquireEditLock()) {
               RigidBodyConnectorAgent agent =
                  new RigidBodyConnectorAgent (myMain, model);
               agent.show (popupBounds);
            }
         }
         else if (actionCommand == "Add AxialSprings ...") {
            if (myEditManager.acquireEditLock()) {
               AxialSpringAgent<AxialSpring> agent =
                  new AxialSpringAgent<AxialSpring> (
                     myMain, (ComponentList<AxialSpring>)model.axialSprings(),
                     model);
               agent.show (popupBounds);
            }
         }
         else if (actionCommand == "Add Particles ...") {
            if (myEditManager.acquireEditLock()) {
               ParticleAgent agent = new ParticleAgent (myMain, model);
               agent.show (popupBounds);
            }
         }
         else if (actionCommand == "Add RigidBody ...") {
            if (myEditManager.acquireEditLock()) {
               RigidBodyAgent agent =
                  new RigidBodyAgent (myMain, model);
               agent.show (popupBounds);
            }
         }
         else if (actionCommand == "Add FemModel ...") {
            if (myEditManager.acquireEditLock()) {
               FemModel3dAgent agent =
                  new FemModel3dAgent (
                     myMain,
                     (ComponentList<MechSystemModel>)model.models(),
                     model);
               agent.show (popupBounds);
            }
         }
         else if (actionCommand == "Add FrameMarkers ...") {
            if (myEditManager.acquireEditLock()) {
               FrameMarkerAgent agent = new FrameMarkerAgent (myMain, model);
               agent.show (popupBounds);
            }
         }
         else if (actionCommand == "Attach Particles ...") {
            if (myEditManager.acquireEditLock()) {
               AttachParticleAgent agent =
                  new AttachParticleAgent (myMain, model);
               agent.show (popupBounds);
            }
         }
         else if (actionCommand == "Set default collisions ...") {
            Frame frame = myMain.getMainFrame();
            DefaultCollisionsDialog dialog =
               new DefaultCollisionsDialog (frame, model);
            GuiUtils.locateCenter (dialog, frame);
            dialog.setVisible(true);
            if (dialog.getReturnValue() == OptionPanel.OK_OPTION) {
               Command cmd = dialog.createCommand();
               myMain.getUndoManager().saveStateAndExecute (cmd);               
            }
         }
         else if (actionCommand == "Remove collision overrides") {
            LinkedList<CollisionBehavior> comps = new
            LinkedList<CollisionBehavior>();
            CollisionManager cm = model.getCollisionManager();
            ComponentList<CollisionBehavior> behavs = cm.behaviors();
            // First numDefaultPairs() behaviors are reserved - don't delete
            for (int i=cm.numDefaultPairs(); i<behavs.size(); i++) {
               comps.add (behavs.get(i));
            }
            Command cmd = RemoveAddCommand.createRemoveCommand (
               "remove collision overrides", comps);
            myMain.getUndoManager().saveStateAndExecute (cmd);               
         }
      }
      else if (containsSingleSelection (selection, RigidBody.class)) {
         if (actionCommand == "Add FrameMarkers ...") {
            if (myEditManager.acquireEditLock()) {
               RigidBody body = (RigidBody)selection.get (0);
               MechModel mech = MechModel.nearestMechModel (body);
               if (mech == null) {
                  System.out.println (
                     "RigidBody does not have an ancestor MechModel");
               }
               else {
                  FrameMarkerAgent agent = new FrameMarkerAgent (myMain, mech);
                  agent.show (popupBounds);
               }
            }
         }
      }
      else if (containsDoubleSelection (selection, Point.class)) {
         Point pointB = (Point)selection.get (0);
         Point pointA = (Point)selection.get (1);
         if (actionCommand == "Add AxialSpring ...") {
            if (myEditManager.acquireEditLock()) {
               MechModel mechMod = MechModel.lowestCommonModel (pointB, pointA);
               AxialSpringAgent agent =
                  new AxialSpringAgent (myMain, mechMod.axialSprings(), mechMod);
               agent.show (popupBounds);
               agent.setPoints (pointA, pointB);
            }
         }
         else if (actionCommand == "Compute distance") {
            System.out.printf ("distance = %g\n", pointB.distance (pointA));
         }
      }
      else {
         if (actionCommand == "Add BodyConnector ...") {
            if (myEditManager.acquireEditLock()) {
               RigidBody bodyB = (RigidBody)selection.get (0);
               RigidBody bodyA = (RigidBody)selection.get (1);
               RigidBodyConnectorAgent agent =
                  new RigidBodyConnectorAgent (
                     myMain, MechModel.lowestCommonModel (
                        bodyB, bodyA));
               agent.show (popupBounds);
               agent.setBodies (bodyA, bodyB);
            }
         }
         else if (actionCommand == "Add FrameSpring") {
            RigidBody bodyB = (RigidBody)selection.get (0);
            RigidBody bodyA = (RigidBody)selection.get (1);
            FrameSpring s = new FrameSpring (null);
            s.setFrameA (bodyA);
            s.setFrameB (bodyB);
            MechModel mechMod = MechModel.lowestCommonModel (bodyB, bodyA);
            mechMod.addFrameSpring (s);
         }
         else if (actionCommand == "Set collisions ...") {
            setCollisions (selection);
         }
         else if (actionCommand == "Set self collision ...") {
            setCollisions (selection);
         }
         // else if (actionCommand == "Unset collisions") {
         //    unsetCollisions (selection);
         // }
         else if (actionCommand == "Detach particles") {
            LinkedList<Particle> attached = new LinkedList<Particle>();
            for (ModelComponent c : selection) {
               if (c instanceof Particle && ((Particle)c).isAttached()) {
                  attached.add ((Particle)c);
               }
            }
            if (attached.size() > 0) {
               MechModel mechMod = getAttachmentModel (attached.get(0));
               DetachParticlesCommand cmd = new DetachParticlesCommand (
                  "Detach particles", attached, mechMod);
               myMain.getUndoManager().saveStateAndExecute (cmd);
            }
         }
      }
   }

   private MechModel getCollisionsModel (LinkedList<ModelComponent> selection) {
      
      Frame frame = myMain.getMainFrame();
      MechModel mech = lowestCommonModel (selection);
      if (mech == null) {
         GuiUtils.showError (
            frame, "Internal error: no common model for components");
         return null;
      }
      return mech;
      // // if an Edit Collisions agent is open, use it's model if possible:
      // if (myEditCollisionsAgent != null && myEditCollisionsAgent.isVisible()) {
      //    if (mech != myEditCollisionsModel &&
      //        !ComponentUtils.isAncestorOf (myEditCollisionsModel, mech)) {
      //       GuiUtils.showError (
      //          frame, 
      //          "Components not descendents of current Edit Collisons model");
      //       return null;
      //    }
      //    else {
      //       return myEditCollisionsModel;
      //    }
      // }
      // else {
      //    return mech;
      // }

   }

   private void setCollisions (LinkedList<ModelComponent> selection) {
      ArrayList<Collidable> collidables =
         new ArrayList<Collidable>(selection.size());
      for (ModelComponent c : selection) {
         collidables.add ((Collidable)c);
      }
      Frame frame = myMain.getMainFrame();
      MechModel mech = getCollisionsModel (selection);
      if (mech == null) {
         return;
      }
      if (collidables.size() > 1) {
         for (int i=0; i<collidables.size(); i++) {
            Collidable ci = collidables.get(i);
            for (int j=i+1; j<collidables.size(); j++) {
               Collidable cj = collidables.get(j);
               if (ModelComponentBase.recursivelyContains (ci, cj) ||
                   ModelComponentBase.recursivelyContains (cj, ci)) {
                  GuiUtils.showError (
                     frame, "One or more collidables is a descendant of another");
                  return;
               }
            }
         }
      }
      SetCollisionsDialog dialog =
         new SetCollisionsDialog (frame, mech, collidables);
      GuiUtils.locateCenter (dialog, frame);
      dialog.setVisible(true);
      if (dialog.getReturnValue() == OptionPanel.OK_OPTION) {
         Command cmd = dialog.createCommand();
         myMain.getUndoManager().saveStateAndExecute (cmd);               
      }
   }
}
