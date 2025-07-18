package artisynth.core.driver;

import java.awt.Container;
import java.awt.Dimension;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;

import artisynth.core.util.ArtisynthPath;
import artisynth.core.gui.widgets.PanelFileChooser;
import artisynth.core.modelbase.ModelComponent.FilePathSaveType;
import maspack.widgets.BooleanSelector;
import maspack.widgets.EnumSelector;
import maspack.widgets.PropertyPanel;
import maspack.util.GenericFileFilter;

/**
 * File chooser with an extra panel for selecting options related
 * to saving components or models to files.
 */
public class ModelFileChooser extends PanelFileChooser {

   private static final long serialVersionUID = 1L;

   GenericFileFilter myArtFilter;
   BooleanSelector mySaveWayPointData;
   BooleanSelector myCoreCompsOnly;
   EnumSelector myFilePathSaveType;

   protected void addSaveWayPointData (boolean saveWayPointData) {
      mySaveWayPointData =
          new BooleanSelector (
             "Save waypoint data:", saveWayPointData);     
   }

   protected void addCoreCompsOnly (boolean coreCompsOnly) {
      myCoreCompsOnly =
         new BooleanSelector (
            "Core components only:", coreCompsOnly);
   }

   protected void addFilePathSaveType (FilePathSaveType type) {
      myFilePathSaveType =
         new EnumSelector (
            "File path save type:", type, FilePathSaveType.values());
   }

   public ModelFileChooser (File modelFile) {
      build (modelFile);
   }

   public ModelFileChooser (
      File modelFile, boolean coreCompsOnly) {

      addCoreCompsOnly (coreCompsOnly);
      build (modelFile);
   }

   public ModelFileChooser (
      File modelFile, FilePathSaveType type) {

      addFilePathSaveType (type);
      build (modelFile);
   }

   public ModelFileChooser (
      File modelFile, boolean coreCompsOnly, boolean saveWayPointData) {

      addSaveWayPointData (saveWayPointData);
      addCoreCompsOnly (coreCompsOnly);
      build (modelFile);
   }

   protected void build (File modelFile) {
      if (modelFile == null) {
         setCurrentDirectory (ArtisynthPath.getWorkingDir());
      }
      else {
         setCurrentDirectory (modelFile);
         setSelectedFile (modelFile);
      }
      setApproveButtonText("Save");

      myArtFilter = new GenericFileFilter (
         "art", "ArtiSynth model files (*.art)");
      addChoosableFileFilter (myArtFilter);
      if (modelFile == null || myArtFilter.fileExtensionMatches (modelFile)) {
         setFileFilter (myArtFilter);
      }
      if (mySaveWayPointData != null ||
          myCoreCompsOnly != null ||
          myFilePathSaveType != null) {
         PropertyPanel panel = createPropertyPanel();

         if (mySaveWayPointData != null) {
            panel.addWidget (mySaveWayPointData);
         }
         if (myCoreCompsOnly != null) {
            panel.addWidget (myCoreCompsOnly);
         }
         if (myFilePathSaveType != null) {
            panel.addWidget (myFilePathSaveType);
         }
      }
   }

   public boolean getSaveWayPointData() {
      if (mySaveWayPointData != null) {
         return (Boolean)mySaveWayPointData.getValue();
      }
      else {
         return false;
      }
   }

   public void setSaveWayPointData (boolean enable) {
      if (mySaveWayPointData != null) {
         mySaveWayPointData.setValue(enable);
      }
   }

   public boolean getCoreCompsOnly() {
      if (myCoreCompsOnly != null) {
         return (Boolean)myCoreCompsOnly.getValue();
      }
      else {
         return false;
      }
   }      

   public void setCoreCompsOnly (boolean enable) {
      if (myCoreCompsOnly != null) {
         myCoreCompsOnly.setValue(enable);
      }
   }

   public FilePathSaveType getFilePathSaveType() {
      if (myFilePathSaveType != null) {
         return (FilePathSaveType)myFilePathSaveType.getValue();
      }
      else {
         return FilePathSaveType.ABSOLUTE;
      }
   }      

   public void setFilePathSaveType (FilePathSaveType type) {
      if (myFilePathSaveType != null) {
         myFilePathSaveType.setValue(type);
      }
   }

   public void setFilePathSaveTypeEnabled (boolean enable) {
      if (myFilePathSaveType != null) {
         myFilePathSaveType.setEnabledAll (enable);
      }
   }

   public boolean isArtFilterSelected() {
      return getFileFilter() == myArtFilter;
   }

   /**
    * Returns the selected file, appending a .art extension if there is no
    * extension and the .art filter is selected.
    */
   public File getSelectedFileWithExtension() {
      File file = getSelectedFile();
      if (file.getName().indexOf ('.') == -1 && isArtFilterSelected()) {
         file = new File(file.getPath() + ".art");
      }
      return file;
   }
}
