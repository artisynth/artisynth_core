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
import artisynth.core.mechmodels.MultiPointSpring;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.gui.selectionManager.SelectionManager;

/**
 * This class is responsible for actions associated with a MultiPointSpring.
 */
public class MultiPointSpringEditor extends EditorBase {
   public MultiPointSpringEditor (Main main, EditorManager editManager) {
      super (main, editManager);
   }

   public void addActions (EditActionMap actions, SelectionManager selManager) {
      LinkedList<ModelComponent> selection = selManager.getCurrentSelection();

      if (containsMultipleSelection (selection, MultiPointSpring.class)) {
         boolean hasVisible = false;
         boolean hasInvisible = false;
         for (ModelComponent c : selection) {
            MultiPointSpring spr = (MultiPointSpring)c;
            hasVisible |= spr.hasVisibleWrappables();
            hasInvisible |= spr.hasInvisibleWrappables();
         }
         if (hasVisible) {
            actions.add (this, "Hide wrappables");
         }
         if (hasInvisible) {
            actions.add (this, "Show wrappables");
         }
      }
   }

   public void applyAction (
      String actionCommand, LinkedList<ModelComponent> selection,
      Rectangle popupBounds) {
      if (containsMultipleSelection (selection, MultiPointSpring.class)) {
         if (actionCommand == "Show wrappables") {
            for (ModelComponent c : selection) {
               ((MultiPointSpring)c).setWrappablesVisible (true);
            }
            myMain.rerender();
         }
         else if (actionCommand == "Hide wrappables") {
            for (ModelComponent c : selection) {
               ((MultiPointSpring)c).setWrappablesVisible (false);
            }
            myMain.rerender();
         }
      }
   }
}
