/**
 * Copyright (c) 2014, by the Authors: John Lloyd (UBC), Tracy Wilkinson (UBC) and
 * ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.gui.editorManager;

import java.awt.Rectangle;

import artisynth.core.modelbase.*;
import artisynth.core.driver.Main;
import artisynth.core.probes.*;
import artisynth.core.gui.selectionManager.SelectionManager;

import java.util.*;

/**
 * This class is responsible for actions associated with Probes.
 */
public class ProbeEditor extends EditorBase {
   public ProbeEditor (Main main, EditorManager editManager) {
      super (main, editManager);
   }

   public void addActions (EditActionMap actions, SelectionManager selManager) {
      LinkedList<ModelComponent> selection = selManager.getCurrentSelection();

      if (containsMultipleSelection (selection, Probe.class)) {
         boolean allHaveFiles = true;
         for (ModelComponent c : selection) {
            Probe p = (Probe)c;
            if (!p.hasAttachedFile()) {
               allHaveFiles = false;
               break;
            }
         }
         if (allHaveFiles) {
            actions.add (this, "Save data");
         }
      }
   }

   public void applyAction (
      String actionCommand, LinkedList<ModelComponent> selection,
      Rectangle popupBounds) {
      if (containsMultipleSelection (selection, Probe.class)) {
         if (actionCommand == "Save data") {
            try {
               for (ModelComponent c : selection) {
                  ((Probe)c).save();
               }
            }
            catch (Exception e) {
               System.out.println ("Error saving data for probe:");
               System.out.println (e);
            }
         }
      }
   }
}
