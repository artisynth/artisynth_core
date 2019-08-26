/**
 * Copyright (c) 2014, by the Authors: John Lloyd (UBC), Tracy Wilkinson (UBC) and
 * ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */package artisynth.core.gui.editorManager;

import java.awt.Rectangle;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;

import maspack.render.GL.GLClipPlane;
import maspack.render.GL.GLViewer;
import artisynth.core.driver.Main;
import artisynth.core.femmodels.FemElement;
import artisynth.core.femmodels.FemModel.ElementFilter;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.femmodels.FemElement3d;
import artisynth.core.femmodels.FemElement3dBase;
import artisynth.core.femmodels.FemModel;
import artisynth.core.femmodels.FemMeshComp;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.FemNode;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.femmodels.HexElement;
import artisynth.core.gui.selectionManager.SelectionManager;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.ModelComponent;

/**
 * This class is responsible for knowing which components can be added to a
 * FemModel3d.
 * 
 */
public class FemModel3dEditor extends EditorBase {

   public void addActions (EditActionMap actions, SelectionManager selManager) {
      LinkedList<ModelComponent> selection = selManager.getCurrentSelection();

      if (containsSingleSelection (selection, FemModel3d.class)) {
         FemModel3d model = (FemModel3d)selection.get (0);
         actions.add (this, "Add FemMarkers ...", EXCLUSIVE);
         actions.add (this, "Rebuild surface mesh");
         actions.add (this, "Add new surface mesh");
         actions.add (this, "Save surface mesh ...");
         actions.add (this, "Save mesh as Ansys file...");
         actions.add (this, "Select isolated nodes");
         if (model.getGrandParent() instanceof MechModel) {
            actions.add (this, "Attach particles ...", EXCLUSIVE);
         }
      }
      else if (containsMultipleCommonParentSelection (
                  selection, HexElement.class)) {
         actions.add (this, "Subdivide elements");
      }
      if (containsMultipleCommonParentSelection (selection, FemElement3d.class)) {
         actions.add (this, "Rebuild surface mesh for selected elements");
         actions.add (this, "Add new surface mesh for selected elements");
      }
   }

   public void applyAction (
      String actionCommand, LinkedList<ModelComponent> selection,
      Rectangle popupBounds) {
      if (containsSingleSelection (selection, FemModel3d.class)) {
         FemModel3d model = (FemModel3d)selection.get (0);
         if (actionCommand == "Add FemMarkers ...") {
            if (myEditManager.acquireEditLock()) {
               Fem3dMarkerAgent agent = new Fem3dMarkerAgent (myMain, model);
               agent.show (popupBounds);
            }
         }
         else if (actionCommand == "Rebuild surface mesh") {
            rebuildSurfaceMesh (model);
         }
         else if (actionCommand == "Add new surface mesh") {
            addNewSurfaceMesh (model);
         }
         else if (actionCommand == "Save surface mesh ...") {
            EditorUtils.saveMesh (model.getSurfaceMesh(), /*Transform= */null);
         }
         else if (actionCommand == "Save mesh as Ansys file...") {
            EditorUtils.saveMeshAsAnsysFile (model);
         }
         else if (actionCommand == "Select isolated nodes") {
            selectIsolatedNodes (model);
         }
         else if (actionCommand == "Attach particles ...") {
            if (myEditManager.acquireEditLock()) {
               // XXX should be more general than this ... what if mechModel
               // is a sub model?
               MechModel mech = (MechModel)model.getGrandParent();
               myMain.getSelectionManager().clearSelections();
               AttachParticleFemAgent agent =
                  new AttachParticleFemAgent (myMain, mech, model);
               agent.show (popupBounds);
            }
         }
      }
      else if (containsMultipleCommonParentSelection (
                  selection, HexElement.class)) {
         if (actionCommand == "Subdivide elements") {
            FemModel3d mod =
               (FemModel3d)ComponentUtils.getGrandParent(selection.get(0));
            for (ModelComponent c : selection) {
               mod.subdivideHex ((HexElement)c);
            }
         }
      }
      if (actionCommand == "Rebuild surface mesh for selected elements") {
         FemModel3d mod =
               (FemModel3d)ComponentUtils.getGrandParent(selection.get(0));
         rebuildSurfaceMeshForSelectedElements (mod);
      }
      else if (actionCommand == "Add new surface mesh for selected elements") {
         FemModel3d mod =
               (FemModel3d)ComponentUtils.getGrandParent(selection.get(0));
         addNewSurfaceMeshForSelectedElements (mod);
      }
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

   private void rebuildSurfaceMesh (FemModel3d model) {
      GLViewer v = myMain.getViewer();
      model.createSurfaceMesh (new ClippedElementFilter (v.getClipPlanes()));
   }

   private void addNewSurfaceMesh (FemModel3d model) {
      GLViewer v = myMain.getViewer();
      ElementFilter efilter = new ClippedElementFilter (v.getClipPlanes());
      FemMeshComp mcomp = FemMeshComp.createSurface (null, model, efilter);
      mcomp.setSurfaceRendering (SurfaceRender.Shaded);
      model.addMeshComp (mcomp);
   }

   private void selectIsolatedNodes (FemModel3d model) {
      ArrayList<ModelComponent> isolated = new ArrayList<>();
      for (FemNode3d n : model.getNodes()) {
         List<FemElement3dBase> elems = n.getAdjacentElements();
         if (elems == null || elems.size() == 0) {
            isolated.add (n);
         }
      }
      myMain.getSelectionManager().clearSelections();
      myMain.getSelectionManager().addSelected (isolated);
   }

   private class SelectedElementFilter implements FemModel.ElementFilter {

      public boolean elementIsValid (FemElement e) {
         return e.isSelected();
      }
   }

   private void rebuildSurfaceMeshForSelectedElements (FemModel3d model) {
      model.createSurfaceMesh (new SelectedElementFilter());
   }

   private void addNewSurfaceMeshForSelectedElements (FemModel3d model) {
      ElementFilter efilter = new SelectedElementFilter();
      FemMeshComp mcomp = FemMeshComp.createSurface (null, model, efilter);
      mcomp.setSurfaceRendering (SurfaceRender.Shaded);
      model.addMeshComp (mcomp);
   }

   public FemModel3dEditor (Main main, EditorManager editManager) {
      super (main, editManager);
   }
}
