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
import artisynth.core.modelbase.ComponentChangeEvent;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.ComponentChangeEvent.Code;
import artisynth.core.workspace.RootModel;
import artisynth.core.gui.selectionManager.SelectionManager;

/**
 * Provides editing actions for MechModel
 */
public class RootModelEditor extends EditorBase {
   public RootModelEditor (Main main, EditorManager editManager) {
      super (main, editManager);
   }

   public void addActions (EditActionMap actions, SelectionManager selManager) {
      LinkedList<ModelComponent> selection = selManager.getCurrentSelection();

      if (containsSingleSelection (selection, RootModel.class)) {
         actions.add (this, "Add MechModel ...", EXCLUSIVE);
         actions.add (this, "Set current state as default");
      }
   }

   public void applyAction (
      String actionCommand, LinkedList<ModelComponent> selection,
      Rectangle popupBounds) {
      if (containsSingleSelection (selection, RootModel.class)) {
         if (actionCommand == "Add MechModel ...") {
            if (myEditManager.acquireEditLock()) {
               MechModelAgent agent =
                  new MechModelAgent (myMain, (RootModel)selection.get (0));
               agent.show (popupBounds);
            }
         }
         else if (actionCommand == "Set current state as default") {
            myMain.componentChanged (
               new ComponentChangeEvent (Code.STRUCTURE_CHANGED));
         }
      }
   }
}
