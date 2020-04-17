/**
 * Copyright (c) 2014, by the Authors: John Lloyd (UBC), Tracy Wilkinson (UBC) and
 * ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */package artisynth.core.gui.editorManager;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;

import artisynth.core.driver.Main;
import artisynth.core.mechmodels.FrameMarker;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.MechSystemBase;
import artisynth.core.mechmodels.MuscleExciter;
import artisynth.core.modelbase.*;
import maspack.geometry.PolygonalMesh;
import artisynth.core.gui.selectionManager.SelectionManager;
import artisynth.core.inverse.TrackingController;

import java.util.*;

/**
 * This class is responsible for actions associated with a MuscleExciter.
 */
public class TrackingControllerEditor extends EditorBase {
   public TrackingControllerEditor (Main main, EditorManager editManager) {
      super (main, editManager);
   }

   public void addActions (EditActionMap actions, SelectionManager selManager) {
      LinkedList<ModelComponent> selection = selManager.getCurrentSelection();

      if (containsSingleSelection (selection, TrackingController.class)) {
         TrackingController controller = (TrackingController)selection.get (0);
         actions.add (this, "Edit tracking targets ...", EXCLUSIVE);
//         // XXX currently editing muscles through 
//         MechSystemBase mech = controller.getMech ();
//         if (mech.getClass () == MechModel.class) {
//            actions.add (this, "Edit muscles ...", EXCLUSIVE);
//         }
      }
   }

   public void applyAction (
      String actionCommand, LinkedList<ModelComponent> selection,
      Rectangle popupBounds) {
      if (containsSingleSelection (selection, TrackingController.class)) {
         TrackingController controller = (TrackingController)selection.get (0);
         if (actionCommand == "Edit tracking targets ...") {
            if (myEditManager.acquireEditLock()) {
               MotionTargetComponentAgent agent =
                  new MotionTargetComponentAgent (myMain, controller);
               agent.show (popupBounds);
            }
         }
//         else if (actionCommand == "Edit muscles ...") {
//            if (myEditManager.acquireEditLock()) {
//               ExcitationTargetAgent agent =
//                  new ExcitationTargetAgent (myMain, controller.getMuscleExciter());
//               agent.show (popupBounds);
//            }
//         }
      }
   }
}
