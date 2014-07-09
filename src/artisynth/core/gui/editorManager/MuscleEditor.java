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
import artisynth.core.mechmodels.Muscle;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.gui.selectionManager.SelectionManager;

/**
 * This class is responsible for actions associated with a Muscle.
 */
public class MuscleEditor extends EditorBase {
   public MuscleEditor (Main main, EditorManager editManager) {
      super (main, editManager);
   }

   public void addActions (EditActionMap actions, SelectionManager selManager) {
      LinkedList<ModelComponent> selection = selManager.getCurrentSelection();

      if (containsMultipleSelection (selection, Muscle.class)) {
         actions.add (this, "Reset length props");
      }
   }

   public void applyAction (
      String actionCommand, LinkedList<ModelComponent> selection,
      Rectangle popupBounds) {
      if (containsMultipleSelection (selection, Muscle.class)) {
         if (actionCommand == "Reset length props") {
            for (ModelComponent c : selection) {
               ((Muscle)c).resetLengthProps();
            }
         }

      }
   }
}
