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
import maspack.widgets.BooleanSelector;
import maspack.widgets.LabeledComponentPanel;

/**
 * File chooser with an extra panel for selecting options related
 * to saving components or models to files.
 */
public class ModelFileChooser extends JFileChooser {

   private static final long serialVersionUID = 1L;
   
   BooleanSelector mySaveWayPointData;
   BooleanSelector myCoreCompsOnly;

   private int getLastJPanelIndex (Container comp) {
      for (int i=comp.getComponentCount()-1; i >= 0; i--) {
         if (comp.getComponent(i) instanceof JPanel) {
            return i;
         }
      }
      return -1;
   }   

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

   public ModelFileChooser (File modelFile) {
      build (modelFile);
   }

   public ModelFileChooser (
      File modelFile, boolean coreCompsOnly) {

      addCoreCompsOnly (coreCompsOnly);
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
      
      if (mySaveWayPointData != null || myCoreCompsOnly != null) {
         LabeledComponentPanel panel = new LabeledComponentPanel();

         if (mySaveWayPointData != null) {
            panel.addWidget (mySaveWayPointData);
         }
         if (myCoreCompsOnly != null) {
            panel.addWidget (myCoreCompsOnly);
         }
         
         panel.setBorder (
            BorderFactory.createEtchedBorder (EtchedBorder.LOWERED));
         
         // Add this panel to the last JPanel, in the location just before *its*
         // last JPanel (which contains the Save and Cancel buttons)
         int lastPanelIndex = getLastJPanelIndex (this);
         if (lastPanelIndex != -1) {
            JPanel lastPanel = (JPanel)getComponent(lastPanelIndex);
            int checkBoxIndex = getLastJPanelIndex (lastPanel);
            lastPanel.add (panel, checkBoxIndex);
            lastPanel.add (new Box.Filler(
                              new Dimension(1,10),
                              new Dimension(1,10),
                              new Dimension(1,10)), checkBoxIndex);
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
}
