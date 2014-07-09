/**
 * Copyright (c) 2014, by the Authors: John Lloyd (UBC), Tracy Wilkinson (UBC) and
 * ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */package artisynth.core.gui.editorManager;

import java.awt.Rectangle;

import artisynth.core.driver.Main;
import artisynth.core.mechmodels.FrameMarker;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.modelbase.*;
import maspack.geometry.PolygonalMesh;
import artisynth.core.gui.selectionManager.SelectionManager;

import java.util.*;

/**
 * This class is responsible for actions associated with a RigidBody.
 */
public class RigidBodyEditor extends EditorBase {
   public RigidBodyEditor (Main main, EditorManager editManager) {
      super (main, editManager);
   }

    public void addActions (EditActionMap actions, SelectionManager selManager) {
      LinkedList<ModelComponent> selection = selManager.getCurrentSelection();

      if (containsMultipleSelection (selection, RigidBody.class)) {
         actions.add (this, "Select markers");
         if (containsSingleSelection (selection, RigidBody.class)) {
            RigidBody body = (RigidBody)selection.get (0);
            actions.add (this, "Edit geometry and inertia ...", EXCLUSIVE);
            actions.add (this, "Save mesh as ...");
            if (body.getGrandParent() instanceof MechModel) {
               actions.add (this, "Attach particles ...", EXCLUSIVE);
            }
         }
      }
   }

   public void applyAction (
      String actionCommand, LinkedList<ModelComponent> selection,
      Rectangle popupBounds) {
      if (containsMultipleSelection (selection, RigidBody.class)) {
         if (actionCommand == "Select markers") {
            LinkedList<ModelComponent> list =
               (LinkedList<ModelComponent>)selection.clone();
            for (ModelComponent c : list) {
               FrameMarker[] mkrs = ((RigidBody)c).getFrameMarkers();
               for (int i = 0; i < mkrs.length; i++) {
                  myMain.getSelectionManager().addSelected (mkrs[i]);
               }
               myMain.getSelectionManager().removeSelected (c);
            }
         }
         if (containsSingleSelection (selection, RigidBody.class)) {
            if (actionCommand == "Edit geometry and inertia ...") {
               if (myEditManager.acquireEditLock()) {
                  RigidBody body = (RigidBody)selection.get (0);
                  RigidBodyGeometryAgent agent =
                     new RigidBodyGeometryAgent (myMain, body);
                  agent.show (popupBounds);
               }
            }
            else if (actionCommand == "Save mesh as ...") {
               RigidBody body = (RigidBody)selection.get (0);
               PolygonalMesh mesh = body.getMesh();
               EditorUtils.saveMesh (
                  mesh, mesh != null ? mesh.getMeshToWorld() : null);
            }
            else if (actionCommand == "Attach particles ...") {
               if (myEditManager.acquireEditLock()) {
                  RigidBody body = (RigidBody)selection.get (0);
                  // XXX should be more general than this ... what if mechModel
                  // is a sub model?
                  MechModel mech = (MechModel)body.getGrandParent();
                  myMain.getSelectionManager().clearSelections();
                  AttachParticleBodyAgent agent =
                     new AttachParticleBodyAgent (myMain, mech, body);
                  agent.show (popupBounds);
               }
            }
         }
      }
   }
}
