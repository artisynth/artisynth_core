/**
 * Copyright (c) 2014, by the Authors: John Lloyd (UBC), Tracy Wilkinson (UBC) and
 * ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */package artisynth.core.gui.editorManager;

import java.awt.Rectangle;
import java.util.LinkedList;

import maspack.render.GL.GLClipPlane;
import maspack.render.GL.GLViewer;
import maspack.geometry.PolygonalMesh;
import artisynth.core.driver.Main;
import artisynth.core.femmodels.*;
import artisynth.core.gui.selectionManager.SelectionManager;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.ModelComponent;

/**
 * This class is responsible for knowing which editing actions can
 * be performed on a FemMeshComp.
 */
public class FemMeshCompEditor extends EditorBase {

   public void addActions (EditActionMap actions, SelectionManager selManager) {
      LinkedList<ModelComponent> selection = selManager.getCurrentSelection();

      if (containsSingleSelection (selection, FemMeshComp.class)) {
         FemMeshComp mcomp = (FemMeshComp)selection.get (0);
         if (mcomp.getMesh() instanceof PolygonalMesh) {
            actions.add (this, "Rebuild as surface mesh");
         }
      }
      //if (containsMultipleCommonParentSelection(selection,FemElement3d.class)) {
      //   actions.add (this, "Build surface mesh for selected elements");
      //}
   }

   public void applyAction (
      String actionCommand, LinkedList<ModelComponent> selection,
      Rectangle popupBounds) {
      if (containsSingleSelection (selection, FemMeshComp.class)) {
         FemMeshComp mcomp = (FemMeshComp)selection.get (0);         
         if (actionCommand == "Rebuild as surface mesh") {
            rebuildAsSurfaceMesh (mcomp);
         }
      }
      // if (actionCommand == "Build surface mesh for selected elements") {
      //    FemModel3d mod =
      //          (FemModel3d)ComponentUtils.getGrandParent(selection.get(0));
      //    rebuildSurfaceMeshForSelectedElements (mod);
      // }
   }

   private class ClippedElementFilter implements FemModel.ElementFilter {
      GLClipPlane[] myPlanes;

      public ClippedElementFilter (GLClipPlane[] planes) {
         myPlanes = planes;
      }

      public boolean elementIsValid (FemElement e) {
         if (myPlanes != null && myPlanes.length > 0) {
            for (int i=0; i<myPlanes.length; i++) {
               if (myPlanes[i].isClippingEnabled()) {
                  FemNode[] nodes = e.getNodes();
                  for (int k=0; k<nodes.length; k++) {
                     if (myPlanes[i].isClipped (nodes[k].getPosition())) {
                        return false;
                     }
                  }
               }
            }
         }
         return true;
      }
   }

   private void rebuildAsSurfaceMesh (FemMeshComp mcomp) {
      GLViewer v = myMain.getViewer();
      mcomp.createSurface (new ClippedElementFilter (v.getClipPlanes()));
   }

   // private class SelectedElementFilter extends FemModel.ElementFilter {

   //    public boolean elementIsValid (FemElement e) {
   //       return e.isSelected();
   //    }
   // }

   // private void rebuildSurfaceMeshForSelectedElements (FemModel3d model) {
   //    model.createSurfaceMesh (new SelectedElementFilter());
   // }

   public FemMeshCompEditor (Main main, EditorManager editManager) {
      super (main, editManager);
   }
}
