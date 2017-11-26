/**
 * Copyright (c) 2014, by the Authors: John Lloyd (UBC), Tracy Wilkinson (UBC) and
 * ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */package artisynth.core.gui.editorManager;

import java.awt.Rectangle;

import artisynth.core.driver.Main;
import artisynth.core.mechmodels.MeshComponent;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.renderables.EditablePolygonalMeshComp;
import artisynth.core.modelbase.*;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.MeshBase;
import maspack.render.*;
import artisynth.core.gui.selectionManager.SelectionManager;

import java.util.*;

/**
 * This class is responsible for actions associated with a MeshComopnent.
 */
public class MeshBodyEditor extends EditorBase {
   public MeshBodyEditor (Main main, EditorManager editManager) {
      super (main, editManager);
   }

    public void addActions (EditActionMap actions, SelectionManager selManager) {
      LinkedList<ModelComponent> selection = selManager.getCurrentSelection();

      if (containsMultipleSelection (selection, MeshComponent.class)) {
         if (containsSingleSelection (selection, MeshComponent.class)) {
            MeshComponent body = (MeshComponent)selection.get (0);
            actions.add (this, "Save local mesh as ...");
            actions.add (this, "Save world mesh as ...");
            if (body.getMesh() instanceof PolygonalMesh &&
                body.getGrandParent() instanceof MechModel) {           
               actions.add (this, "Add mesh inspector");
            }
         }
      }
   }

   public void applyAction (
      String actionCommand, LinkedList<ModelComponent> selection,
      Rectangle popupBounds) {
      if (containsMultipleSelection (selection, MeshComponent.class)) {
         if (containsSingleSelection (selection, MeshComponent.class)) {
            if (actionCommand == "Save local mesh as ...") {
               MeshComponent body = (MeshComponent)selection.get (0);
               MeshBase mesh = body.getMesh();
               EditorUtils.saveMesh (mesh, null);
            }
            else if (actionCommand == "Save world mesh as ...") {
               MeshComponent body = (MeshComponent)selection.get (0);
               MeshBase mesh = body.getMesh();
               EditorUtils.saveMesh (
                  mesh, mesh != null ? mesh.getMeshToWorld() : null);
            }
            else if (actionCommand == "Add mesh inspector") {
               MeshComponent body = (MeshComponent)selection.get (0);
               MechModel mech = (MechModel)body.getGrandParent();
               EditablePolygonalMeshComp editMesh =
                  new EditablePolygonalMeshComp ((PolygonalMesh)body.getMesh());
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
