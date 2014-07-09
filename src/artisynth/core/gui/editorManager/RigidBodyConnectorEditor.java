/**
 * Copyright (c) 2014, by the Authors: John Lloyd (UBC), Tracy Wilkinson (UBC) and
 * ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */package artisynth.core.gui.editorManager;

import java.awt.Rectangle;
import java.util.LinkedList;

import artisynth.core.driver.Main;
import artisynth.core.mechmodels.FrameMarker;
import artisynth.core.mechmodels.MarkerPlanarConnector;
import artisynth.core.mechmodels.RigidBodyConnector;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.gui.selectionManager.SelectionManager;

/**
 * Provides editing actions for RigidBodyConnector
 */
public class RigidBodyConnectorEditor extends EditorBase {
   public RigidBodyConnectorEditor (Main main, EditorManager editManager) {
      super (main, editManager);
   }

   public void addActions (EditActionMap actions, SelectionManager selManager) {
      LinkedList<ModelComponent> selection = selManager.getCurrentSelection();

      if (containsSingleSelection (selection, RigidBodyConnector.class)) {
         if (selection.get (0) instanceof MarkerPlanarConnector) {
            actions.add (this, "Select constrained marker");
         }
      }
   }

   public void applyAction (
      String actionCommand, LinkedList<ModelComponent> selection,
      Rectangle popupBounds) {
      if (containsSingleSelection (selection, RigidBodyConnector.class)) {
         if (actionCommand == "Select constrained marker") {
            FrameMarker mkr =
               ((MarkerPlanarConnector)selection.get (0)).getFrameMarker ();
            if (mkr != null) {
               myMain.getSelectionManager ().clearSelections ();
               myMain.getSelectionManager ().addSelected (mkr);
            }
         }
      }
   }
}
