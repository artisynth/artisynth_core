/**
 * Copyright (c) 2014, by the Authors: John Lloyd (UBC), Tracy Wilkinson (UBC) and
 * ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */package artisynth.core.gui.editorManager;

import java.awt.Rectangle;
import java.util.LinkedList;

import maspack.matrix.RigidTransform3d;
import artisynth.core.driver.Main;
import artisynth.core.mechmodels.FrameMarker;
import artisynth.core.mechmodels.MarkerPlanarConnector;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.gui.selectionManager.SelectionManager;

/**
 * Provides editing actions for FrameMarker
 */
public class FrameMarkerEditor extends EditorBase {
   public FrameMarkerEditor (Main main, EditorManager editManager) {
      super (main, editManager);
   }

   public void addActions (EditActionMap actions, SelectionManager selManager) {
      LinkedList<ModelComponent> selection = selManager.getCurrentSelection();

      if (containsSingleSelection (selection, FrameMarker.class)) { 
         if (((FrameMarker)selection.get(0)).getGrandParent() instanceof 
             MechModel) {
            actions.add (this, "Add PlanarConnector");
         }
      }
   }

   public void applyAction (
      String actionCommand, LinkedList<ModelComponent> selection,
      Rectangle popupBounds) {
      if (containsSingleSelection (selection, FrameMarker.class)) {
         if (actionCommand == "Add PlanarConnector") {
            FrameMarker mkr = (FrameMarker)selection.get (0);
            RigidTransform3d XPW = new RigidTransform3d ();
            XPW.p.set (mkr.getPosition ());
            MarkerPlanarConnector mpc = new MarkerPlanarConnector (mkr, XPW);
            mpc.setPlaneSize (getDefaultPlaneSize ());
            ((MechModel)mkr.getGrandParent ()).addRigidBodyConnector (mpc);
         }
      }
   }
   
   private double getDefaultPlaneSize() {
      return 0.5 * myMain.getViewer ().estimateRadiusAndCenter (null);
   }
}
