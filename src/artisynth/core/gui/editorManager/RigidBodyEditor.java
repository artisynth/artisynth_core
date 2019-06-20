/**
 * Copyright (c) 2014, by the Authors: John Lloyd (UBC), Tracy Wilkinson (UBC) and
 * ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */package artisynth.core.gui.editorManager;

import java.awt.Rectangle;

import artisynth.core.driver.Main;
import artisynth.core.mechmodels.DistanceGridComp;
import artisynth.core.mechmodels.FrameMarker;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.renderables.EditablePolygonalMeshComp;
import artisynth.core.modelbase.*;
import maspack.geometry.PolygonalMesh;
import maspack.render.*;
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
            actions.add (this, "Save local mesh as ...");
            actions.add (this, "Save world mesh as ...");
            if (body.getGrandParent() instanceof MechModel) {
               actions.add (this, "Attach particles ...", EXCLUSIVE);
            }
            if (body.getSurfaceMesh() != null) {
               actions.add (this, "Add mesh inspector");
            }
         }
         // actions for controlling distance grid visibility 
         boolean oneDistanceGridVisible = false;
         boolean oneDistanceGridInvisible = false;
         for (ModelComponent c : selection) {
            DistanceGridComp gcomp = ((RigidBody)c).getDistanceGridComp();
            if (gcomp != null) {
               if (RenderableComponentBase.isVisible (gcomp) &&
                   gcomp.getRenderGrid()) {
                  oneDistanceGridVisible = true;
               }
               else {
                  oneDistanceGridInvisible = true;
               }
            }
         }
         if (oneDistanceGridVisible) {
            actions.add (this, "Set distance grid invisible");
         }
         if (oneDistanceGridInvisible) {
            actions.add (this, "Set distance grid visible");
         }

         // // actions for controlling grid surface rendering
         // boolean oneGridSurfaceRenderingOn = false;
         // boolean oneGridSurfaceRenderingOff = false;
         // for (ModelComponent c : selection) {
         //    if (((RigidBody)c).getGridSurfaceRendering()) {
         //       oneGridSurfaceRenderingOn = true;
         //    }
         //    else {
         //       oneGridSurfaceRenderingOff = true;
         //    }
         // }
         // if (oneGridSurfaceRenderingOn) {
         //    actions.add (this, "Disable grid surface rendering");
         // }
         // if (oneGridSurfaceRenderingOff) {
         //    actions.add (this, "Enable grid surface rendering");
         // }
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
         else if (actionCommand == "Set distance grid visible") {
            LinkedList<ModelComponent> list =
               (LinkedList<ModelComponent>)selection.clone();
            for (ModelComponent c : list) {
               DistanceGridComp gcomp = ((RigidBody)c).getDistanceGridComp();
               RenderableComponentBase.setVisible (gcomp, true);
               gcomp.setRenderGrid (true);
            }
            myMain.rerender();
         }
         else if (actionCommand == "Set distance grid invisible") {
            LinkedList<ModelComponent> list =
               (LinkedList<ModelComponent>)selection.clone();
            for (ModelComponent c : list) {
               DistanceGridComp gcomp = ((RigidBody)c).getDistanceGridComp();
               gcomp.setRenderGrid (false);
            }
            myMain.rerender();
         }
         // else if (actionCommand == "Enable grid surface rendering") {
         //    LinkedList<ModelComponent> list =
         //       (LinkedList<ModelComponent>)selection.clone();
         //    for (ModelComponent c : list) {
         //       ((RigidBody)c).setGridSurfaceRendering (true);
         //    }
         //    myMain.rerender();
         // }
         // else if (actionCommand == "Disable grid surface rendering") {
         //    LinkedList<ModelComponent> list =
         //       (LinkedList<ModelComponent>)selection.clone();
         //    for (ModelComponent c : list) {
         //       ((RigidBody)c).setGridSurfaceRendering (false);
         //    }
         //    myMain.rerender();
         // }
         if (containsSingleSelection (selection, RigidBody.class)) {
            if (actionCommand == "Edit geometry and inertia ...") {
               if (myEditManager.acquireEditLock()) {
                  RigidBody body = (RigidBody)selection.get (0);
                  RigidBodyGeometryAgent agent =
                     new RigidBodyGeometryAgent (myMain, body);
                  agent.show (popupBounds);
               }
            }
            else if (actionCommand == "Save local mesh as ...") {
               RigidBody body = (RigidBody)selection.get (0);
               PolygonalMesh mesh = body.getMesh();
               EditorUtils.saveMesh (mesh, null);
            }
            else if (actionCommand == "Save world mesh as ...") {
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
            else if (actionCommand == "Add mesh inspector") {
               RigidBody body = (RigidBody)selection.get (0);
               MechModel mech = (MechModel)body.getGrandParent();
               EditablePolygonalMeshComp editMesh =
                  new EditablePolygonalMeshComp (body.getSurfaceMesh());
               double size = RenderableUtils.getRadius (editMesh);
               RenderProps.setVisible (editMesh, true);
               RenderProps.setPointStyle (editMesh, Renderer.PointStyle.SPHERE);
               RenderProps.setPointRadius (editMesh, 0.05*size);
               mech.addRenderable (editMesh);
            }
         }
      }
   }
}
