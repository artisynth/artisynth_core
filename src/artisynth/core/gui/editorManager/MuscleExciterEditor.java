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
import artisynth.core.mechmodels.MuscleExciter;
import artisynth.core.modelbase.*;
import maspack.geometry.PolygonalMesh;
import artisynth.core.gui.selectionManager.SelectionManager;

import java.util.*;

/**
 * This class is responsible for actions associated with a MuscleExciter.
 */
public class MuscleExciterEditor extends EditorBase {
   public MuscleExciterEditor (Main main, EditorManager editManager) {
      super (main, editManager);
   }

   public void addActions (EditActionMap actions, SelectionManager selManager) {
      LinkedList<ModelComponent> selection = selManager.getCurrentSelection();

      if (containsSingleSelection (selection, MuscleExciter.class)) {
         actions.add (this, "Edit targets ...", EXCLUSIVE);
      }
   }

   public void applyAction (
      String actionCommand, LinkedList<ModelComponent> selection,
      Rectangle popupBounds) {
      if (containsSingleSelection (selection, MuscleExciter.class)) {
         MuscleExciter exciter = (MuscleExciter)selection.get (0);
         if (actionCommand == "Edit targets ...") {
            if (myEditManager.acquireEditLock()) {
               ExcitationTargetAgent agent =
                  new ExcitationTargetAgent (myMain, exciter);
               agent.show (popupBounds);
            }
         }
      }
   }
}
