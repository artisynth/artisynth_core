/**
 * Copyright (c) 2014, by the Authors: John Lloyd (UBC), Tracy Wilkinson (UBC) and
 * ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.gui.editorManager;

import java.io.*;
import java.awt.Rectangle;
import java.awt.Window;
import javax.swing.*;

import artisynth.core.modelbase.*;
import artisynth.core.workspace.RootModel;
import artisynth.core.driver.Main;
import artisynth.core.probes.*;
import artisynth.core.util.ArtisynthPath;
import artisynth.core.util.MatlabInterface;
import artisynth.core.util.MatlabInterfaceException;
import maspack.widgets.GuiUtils;
import artisynth.core.gui.selectionManager.SelectionManager;
import artisynth.core.gui.widgets.ProbeExportChooser;

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
         boolean allHaveExportFiles = true;
         boolean allNumeric = true;
         for (ModelComponent c : selection) {
            Probe p = (Probe)c;
            if (!p.hasAttachedFile()) {
               allHaveFiles = false;
               break;
            }
            if (p.getExportFileName() == null) {
               allHaveExportFiles = false;
               break;
            }
            if (!(p instanceof NumericProbeBase)) {
               allNumeric = false;
            }
         }
         if (allHaveFiles) {
            actions.add (this, "Save data");
         }
         if (selection.size() == 1) {
            actions.add (this, "Save data as ...");
         }
         if (allHaveFiles) {
            actions.add (this, "Load data");
         }
         if (selection.size() == 1) {
            actions.add (this, "Load data from ...");
         }
         if (allHaveExportFiles) {
            actions.add (this, "Export data");
         }
         if (selection.size() == 1) {
            actions.add (this, "Export data as ...");
         }
         if (allNumeric && myMain.hasMatlabConnection()) {
            actions.add (this, "Save to MATLAB");
            actions.add (this, "Load from MATLAB");
         }
      }
   }

   private void showError (String msg, Probe probe, Exception e) {
      showError (myMain.getMainFrame(), msg, probe, e);
   }

   private static void showError (
      Window win, String msg, Probe probe, Exception e) {
      String probeName = ComponentUtils.getPathName (probe);
      msg += " probe "+probeName;
      if (e != null) {
         msg += ":\n" + e.getMessage();
      }
      GuiUtils.showError (win, msg);
   }

   public void applyAction (
      String actionCommand, LinkedList<ModelComponent> selection,
      Rectangle popupBounds) {
      if (containsMultipleSelection (selection, Probe.class)) {
         if (actionCommand.equals ("Save data")) {
            for (ModelComponent c : selection) {
               if (!saveData ((Probe)c, myMain.getMainFrame())) {
                  break;
               }
            }
         }
         else if (actionCommand.equals ("Save data as ...")) {
            saveDataAs ((Probe)selection.get(0), myMain.getMainFrame());
         }
         else if (actionCommand.equals ("Load data")) {
            for (ModelComponent c : selection) {
               if (!loadData ((Probe)c, myMain.getMainFrame())) {
                  break;
               }
            }
            myMain.rerender();
         }
         else if (actionCommand.equals ("Load data from ...")) {
            loadDataFrom ((Probe)selection.get(0), myMain.getMainFrame());
            myMain.rerender();
         }
         else if (actionCommand.equals ("Export data")) {
            for (ModelComponent c : selection) {
               Probe probe = (Probe)c;
               if (!export (probe, myMain.getMainFrame())) {
                  break;
               }
            }
         }
         else if (actionCommand.equals ("Export data as ...")) {
            exportAs ((Probe)selection.get(0), myMain.getMainFrame());
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
            myMain.rerender();
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
         GuiUtils.showError (win, errMsg);
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
         GuiUtils.showError (win, errMsg);
         return false;
      }
      else {
         return true;
      }
   }

   // load a probe
   static public boolean loadData (Probe probe, Window win) {
      try {
         probe.load();
         return true;
      }
      catch (IOException e) {
         showError (win, "Error loading", probe, e);
         probe.setAttachedFileName (null);
         return false;
      }
   }

   static public void loadDataFrom (Probe probe, Window win) {
      File current = null;
      String workspace = new String (ArtisynthPath.getWorkingDirPath());
      // workspace = workspace.substring(0, (workspace.length() - 2));

      current = probe.getAttachedFile();

      if (current == null || !current.exists()) {
         current = new File (workspace);
      }
      File file = chooseFile (win, current, JFileChooser.FILES_ONLY, "Load");
      if (file != null) {
         String probePath = Probe.getPathFromFile (file);
         System.out.println ("Workspace: " + workspace);
         System.out.println ("Selected file address: " + probePath);
         String oldFileName = probe.getAttachedFileName();

         try {
            probe.setAttachedFileName (probePath);
            probe.load();
            //updateProbeDisplays();
            //myController.requestUpdateDisplay();
         }
         catch (IOException e) {
            // Back out
            showError (win, "Error loading", probe, e);
            probe.setAttachedFileName (oldFileName);
         }
      }
   }

   private static File chooseFile (
      Window win, File current, int SelectionMode, String ApproveButtonText) {
      JFileChooser chooser = new JFileChooser();
      if (current != null) {
         chooser.setCurrentDirectory (current);
         if (!current.isDirectory()) {
            chooser.setSelectedFile (current);
         }
      }
      chooser.setFileSelectionMode (SelectionMode);
      if (chooser.showDialog (win, ApproveButtonText) ==
          JFileChooser.APPROVE_OPTION) {
         File file = chooser.getSelectedFile();
         if (ApproveButtonText == "Load" && !file.canRead()) {
            GuiUtils.showError (
               win, "File "+file+" does not exist or cannot be read");
            file = null;
         }
         return file;
      }
      else {
         return null;
      }
   }

   static public boolean saveData (Probe probe, Window win) {
      try {
         probe.save();
         return true;
      }
      catch (IOException e) {
         showError (win, "Error saving", probe, e);
         probe.setAttachedFileName (null);
         return false;
      }
   }

   static public boolean saveAllInputData (RootModel root, Window win) {
      for (Probe probe : root.getInputProbes()) {
         if (!saveData (probe, win)) {
            return false;
         }
      }
      return true;
   }

   static public boolean saveAllOutputData (RootModel root, Window win) {
      for (Probe probe : root.getOutputProbes()) {
         if (!saveData (probe, win)) {
            return false;
         }
      }
      return true;
   }

   static public boolean saveWayPointData (RootModel root, Window win) {
      WayPointProbe wayPoints = root.getWayPoints();
      try {
         wayPoints.save();
      }
      catch (IOException e) {
         GuiUtils.showError (
            win, 
            "Error saving waypoints to \"" + wayPoints.getAttachedFile() +
            "\":\n" + e.getMessage());
         wayPoints.setAttachedFileName (null);
         return false;
      }
      return true;
   }

   static public void saveDataAs (Probe probe, Window win) {
      File current = null;
      String workspace = new String (ArtisynthPath.getWorkingDirPath());

      current = probe.getAttachedFile();
      if (current == null) {
         current = new File (workspace);
      }
      File file = chooseFile (win, current, JFileChooser.FILES_ONLY, "Save As");
      if (file != null) {
         if (file.exists() && !GuiUtils.confirmOverwrite (win, file)) {
            return;
         }
         String probePath = Probe.getPathFromFile (file);
         System.out.println ("Selected file path: " + probePath);
         String oldFileName = probe.getAttachedFileName();
         try {
            probe.setAttachedFileName (probePath);
            probe.save();
         }
         catch (IOException e) {
            // Back out
            showError (win, "Error saving", probe, e);
            probe.setAttachedFileName (oldFileName);
         }
      }
   }

   static public boolean export (Probe probe, Window win) {

      File file = probe.getExportFile();
      if (file != null) {
         String ext = ArtisynthPath.getFileExtension(file).toLowerCase();
         Probe.ExportProps props = probe.getExportProps (ext);
         if (props == null) {
            // shouldn't happen
            showError (win, "Error: unsupported file extension "+ext+" for ",
                       probe, null);
            probe.setExportFileName (null);
            return false;
         }
         else {
            try {
               probe.export (file, props);
            }
            catch (Exception e) {
               showError (win, "Error exporting", probe, e);
               probe.setExportFileName (null);
               return false;
            }
         }
      }
      return true;
   }

   static public void exportAs (Probe probe, Window win) {
      ProbeExportChooser chooser = new ProbeExportChooser (probe);
      if (chooser.showDialog (win, "Export data as") ==
          JFileChooser.APPROVE_OPTION) {
         String oldPath = probe.getExportFileName();
         File file = chooser.getSelectedFile();
         String ext = ArtisynthPath.getFileExtension (file);
         if (ext == null) {
            ext = chooser.getSelectedExt();
            file = new File (file.getAbsolutePath()+"."+ext);
         }
         ext = ext.toLowerCase();
         Probe.ExportProps props = probe.getExportProps (ext);
         if (props == null) {
            chooser.showUnsupportedExtensionError (win, ext);
         }
         if (file.exists() && !GuiUtils.confirmOverwrite(win, file)) {
            return;
         }
         try {
            probe.export (file, props);
            probe.setExportFileName (Probe.getPathFromFile(file));
         }
         catch (Exception e) {
            showError (win, "Error exporting", probe, e);
            probe.setExportFileName (oldPath);
         }         
      }
   }

   private static File selectFile (
      String approveMsg, File existingFile, Window win) {
      File dir = ArtisynthPath.getWorkingDir(); // is always non-null

      JFileChooser chooser = new JFileChooser(dir);
      if (existingFile != null) {
         chooser.setSelectedFile (existingFile);
         chooser.setCurrentDirectory (existingFile);
      }
      int retval = chooser.showDialog (win, approveMsg);
      return ((retval == JFileChooser.APPROVE_OPTION) ?
         chooser.getSelectedFile() : null);
   }

   public static boolean saveProbes (Main main, Window win) {
      File probeFile = main.getProbesFile();
      if (probeFile != null) {
         try {
            main.saveProbesFile(probeFile);
            return true;
         }
         catch (IOException e) {
            GuiUtils.showError (win, "Error writing "+probeFile.getPath(), e);
            main.setProbesFile (null);
         }
      }
      return false;
   }

   public static boolean saveProbesAs (Main main, Window win) {
      File probeFile = selectFile("Save As", main.getProbesFile(), win);
      if (probeFile != null) {
         if (probeFile.exists()) {
            if (!GuiUtils.confirmOverwrite (win, probeFile)) {
               return false;
            }
         }
         try {
            main.saveProbesFile (probeFile);
            main.setProbesFile (probeFile);
            return true;
         }
         catch (IOException e) {
            GuiUtils.showError (win, "Error writing "+probeFile.getPath(), e);
         }
      }
      return false;
   }

   public static boolean loadProbes (Main main, Window win) {
      File probeFile = selectFile("Load", main.getProbesFile(), win);
      if (probeFile != null) {
         try {
            main.loadProbesFile(probeFile);
            main.setProbesFile (probeFile);
            return true;
         }
         catch (IOException e) {
            GuiUtils.showError (win, "Error reading "+probeFile.getPath(), e);
         }
      }
      return false;
   }

   
}
