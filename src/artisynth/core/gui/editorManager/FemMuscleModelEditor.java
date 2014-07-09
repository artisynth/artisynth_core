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
import java.util.LinkedList;

import javax.swing.JMenuItem;

import maspack.widgets.GuiUtils;
import artisynth.core.driver.Main;
import artisynth.core.femmodels.FemMuscleModel;
import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.*;
import artisynth.core.femmodels.*;
import artisynth.core.gui.selectionManager.SelectionManager;

public class FemMuscleModelEditor extends EditorBase {

   public FemMuscleModelEditor (Main main, EditorManager editManager) {
      super (main, editManager);
   }

   public void addActions (EditActionMap actions, SelectionManager selManager) {
      LinkedList<ModelComponent> selection = selManager.getCurrentSelection();

      if (containsSingleSelection (selection, FemMuscleModel.class)) {
         actions.add (this, "Add MuscleBundle ...", EXCLUSIVE);
      }
   }

   private void addMuscleBundle (FemMuscleModel tissue, 
                                 Rectangle popupBounds) {
      MuscleBundle bundle = new MuscleBundle();
      ComponentList<MuscleBundle> bundleList = tissue.getMuscleBundles();

      UndoManager undoManager = myMain.getUndoManager();
      AddComponentsCommand cmd =
         new AddComponentsCommand ("add MuscleBundle", bundle, bundleList);
      undoManager.saveStateAndExecute (cmd);

      MuscleFibreAgent agent = new MuscleFibreAgent (myMain, bundle, tissue);
      agent.setContinuousAdd (true);
      agent.setInitializeBundle (bundleList);
      agent.show (popupBounds);
   }

   public void applyAction (
      String actionCommand, LinkedList<ModelComponent> selection,
      Rectangle popupBounds) {
      if (containsSingleSelection (selection, FemMuscleModel.class)) {
         if (actionCommand == "Add MuscleBundle ...") {
            if (myEditManager.acquireEditLock()) {
               addMuscleBundle ((FemMuscleModel)selection.get (0), popupBounds);
            }
         }
      }
   }
}
