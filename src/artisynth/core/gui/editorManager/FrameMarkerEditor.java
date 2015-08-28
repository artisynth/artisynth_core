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
import maspack.matrix.Vector3d;
import artisynth.core.driver.Main;
import artisynth.core.mechmodels.FrameMarker;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.PlanarConnector;
import artisynth.core.mechmodels.RigidBody;
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
            if (mkr.getFrame () instanceof RigidBody && mkr.getGrandParent () instanceof MechModel) {
               PlanarConnector pc = new PlanarConnector ();
               pc.set ((RigidBody)mkr.getFrame(), mkr.getLocation (), Vector3d.Z_UNIT);
               pc.setPlaneSize (getDefaultPlaneSize ());
               ((MechModel)mkr.getGrandParent ()).addBodyConnector (pc);
            }
            else {
               System.out.println("Unable to create PlanarConnector from selected FrameMarker");
            }
         }
      }
   }
   
   private double getDefaultPlaneSize() {
      return 0.5 * myMain.getViewer ().estimateRadiusAndCenter (null);
   }
}
