/**
 * Copyright (c) 2014, by the Authors: John Lloyd (UBC), Tracy Wilkinson (UBC) and
 * ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.gui.editorManager;

import java.awt.Rectangle;
import java.awt.Window;

import artisynth.core.modelbase.*;
import artisynth.core.driver.Main;
import artisynth.core.probes.*;
import artisynth.core.util.MatlabInterface;
import artisynth.core.util.MatlabInterfaceException;
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
         boolean allNumeric = true;
         for (ModelComponent c : selection) {
            Probe p = (Probe)c;
            if (!p.hasAttachedFile()) {
               allHaveFiles = false;
               break;
            }
            if (!(p instanceof NumericProbeBase)) {
               allNumeric = false;
            }
         }
         if (allHaveFiles) {
            actions.add (this, "Save data");
         }
         if (allNumeric && myMain.hasMatlabConnection()) {
            actions.add (this, "Save to MATLAB");
            actions.add (this, "Load from MATLAB");
         }
      }
   }

   
   static public boolean saveToMatlab (
      NumericProbeBase np, MatlabInterface mi, Window win) {

      String errMsg = null;
      try {
         np.saveToMatlab (mi, /*matlabName=*/null);
      }
      catch (MatlabInterfaceException e) {
         e.printStackTrace();
         errMsg = "Error saving to MATLAB: "+e.getMessage();
      }
      if (errMsg != null) {
         EditorUtils.showError (win, errMsg);
         return false;
      }
      else {
         return true;
      }
   }
   
   static public boolean loadFromMatlab (
      NumericProbeBase np, MatlabInterface mi, Window win) {

      String errMsg = null;
      try {
         if (!np.loadFromMatlab (mi, /*matlabName=*/null)) {
            errMsg = "MATLAB variable '"+np.getMatlabName()+"' not found";           
         }
      }
      catch (MatlabInterfaceException e) {
         e.printStackTrace();
         errMsg = "Error loading from to MATLAB: "+e.getMessage();
      }  
      if (errMsg != null) {
         EditorUtils.showError (win, errMsg);
         return false;
      }
      else {
         return true;
      }
   }  
   
   public void applyAction (
      String actionCommand, LinkedList<ModelComponent> selection,
      Rectangle popupBounds) {
      if (containsMultipleSelection (selection, Probe.class)) {
         if (actionCommand.equals ("Save data")) {
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
         else if (actionCommand.equals ("Save to MATLAB")) {
            MatlabInterface mi = myMain.getMatlabConnection();
            for (ModelComponent c : selection) {
               if (c instanceof NumericProbeBase) {
                  NumericProbeBase np = (NumericProbeBase)c;
                  if (!saveToMatlab (np, mi, myMain.getMainFrame())) {
                     break;
                  }
               }
            }
         }
         else if (actionCommand.equals ("Load from MATLAB")) {
            MatlabInterface mi = myMain.getMatlabConnection();
            for (ModelComponent c : selection) {
               if (c instanceof NumericProbeBase) {
                  NumericProbeBase np = (NumericProbeBase)c;
                  if (!loadFromMatlab (np, mi, myMain.getMainFrame())) {
                     break;
                  }
               }
            }
         }
      }
   }
}
