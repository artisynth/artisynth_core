package artisynth.core.gui.widgets;

import java.awt.Container;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.io.File;

import java.util.*;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;
import javax.swing.filechooser.FileFilter;

import artisynth.core.util.ArtisynthPath;
import artisynth.core.util.ExtensionFileFilter;
import artisynth.core.probes.*;
import artisynth.core.probes.Probe.*;
import maspack.widgets.*;
import maspack.properties.*;

/**
 * Chooses the file to import a numeric probe, with extra fields to specify the
 * time step.
 */
public class ProbeImportChooser extends PanelFileChooser
   implements ActionListener, PropertyChangeListener, ValueChangeListener {

   private static final long serialVersionUID = 1L;

   BooleanSelector myTimeDataIncluded;
   DoubleField myTimeStep;

   ExtensionFileFilter myCurrentFilter;
   Probe myProbe;

   public void actionPerformed (ActionEvent e) {
      // nothing to do right now
   }

   public void propertyChange(PropertyChangeEvent evt) {
      if (JFileChooser.FILE_FILTER_CHANGED_PROPERTY.equals (
             evt.getPropertyName())) {
         ExtensionFileFilter newFilter = null;
         if (getFileFilter() instanceof ExtensionFileFilter) {
            newFilter = (ExtensionFileFilter)getFileFilter();
         }
         if (myCurrentFilter != newFilter) {
            myCurrentFilter = newFilter;
         }
      }
   }

   public void valueChange (ValueChangeEvent evt) {
      if (evt.getSource() == myTimeDataIncluded) {
         myTimeStep.setEnabledAll (!myTimeDataIncluded.getBooleanValue());
      }
      else if (evt.getSource() == myTimeStep) {
         double step = myTimeStep.getDoubleValue();
         if (step < 0) {
            myTimeStep.maskValueChangeListeners (true);
            myTimeDataIncluded.maskValueChangeListeners (true);
            myTimeStep.setValue (0.01);
            myTimeDataIncluded.setValue (true);
            myTimeDataIncluded.maskValueChangeListeners (false);
            myTimeStep.maskValueChangeListeners (false);
            myTimeStep.setEnabledAll (false);
         }
      }
   }

   private File stripFileExtension (File file) {
      String pathName = file.getAbsolutePath();
      int dotIndex = pathName.lastIndexOf(".");
      if (dotIndex == -1) {
         return file;
      }
      else {
         return new File(pathName.substring(0, dotIndex));
      }
   }

   public double getTimeStep() {
      if (myTimeStep != null) {
         return myTimeStep.getDoubleValue();
      }
      else {
         return -1;
      }
   }

   public void setTimeStep (double step) {
      if (myTimeStep != null) {
         myTimeStep.setValue (step);
      }
   }

   public boolean getTimeDataIncluded() {
      if (myTimeDataIncluded != null) {
         return myTimeDataIncluded.getBooleanValue();
      }
      else {
         return false;
      }
   }

   public void setTimeDataIncluded (boolean included) {
      if (myTimeDataIncluded != null) {
         myTimeDataIncluded.setValue (included);
      }
   }

   public ProbeImportChooser (Probe probe, double timeStep) {
      
      File file = probe.getImportFile();
      String currentExt = null;
      ImportExportFileInfo[] fileInfo = probe.getImportFileInfo();
      myProbe = probe;

      if (file == null) {
         file = probe.getAttachedFile();
      }
      if (file == null) {
         setCurrentDirectory (ArtisynthPath.getWorkingDir());
      }
      else {
         currentExt = ArtisynthPath.getFileExtension(file);
         file = stripFileExtension (file);
         setCurrentDirectory (file);
         setSelectedFile (file);
      }
      setFileSelectionMode(JFileChooser.FILES_ONLY);
      setAcceptAllFileFilterUsed(false);

      addActionListener (this);
      addPropertyChangeListener (this);

      for (ImportExportFileInfo info : fileInfo) {
         String description =
            info.getDescription() + " (*." + info.getExt() + ")";
         ExtensionFileFilter filter =
            new ExtensionFileFilter (description, info.getExt());
         if (file != null && currentExt != null &&
             currentExt.equalsIgnoreCase (info.getExt())){
            myCurrentFilter = filter;
         }
         addChoosableFileFilter (filter);
      }
      if (myCurrentFilter != null) {
         setFileFilter (myCurrentFilter);
      }
      else {
         myCurrentFilter = (ExtensionFileFilter)getFileFilter();
      }

      myTimeDataIncluded =
         new BooleanSelector ("Time data included:", timeStep <= 0);
      myTimeDataIncluded.addValueChangeListener (this);
      myTimeStep = 
         new DoubleField ("Time step:", timeStep <= 0 ? 0.01 : timeStep);
      myTimeStep.addValueChangeListener (this);
      myTimeStep.setEnabledAll (timeStep > 0);

      PropertyPanel panel = createPropertyPanel();
      panel.addWidget (myTimeDataIncluded);
      panel.addWidget (myTimeStep);
   }

   public String getSelectedExt() {
      if (getFileFilter() instanceof ExtensionFileFilter) {
         return ((ExtensionFileFilter)getFileFilter()).getExtensions()[0];
      }
      else {
         return null;
      }
   }
   
   // public void showUnsupportedExtensionError (Component comp, String ext) {
   //    StringBuilder msg = new StringBuilder();
   //    msg.append ("File extension type '."+ext+"' is unsupported\n");
   //    msg.append ("Please use one of: ");
   //    ImportExportFileInfo[] fileInfo = myProbe.getExportFileInfo();
   //    for (int i=0; i<fileInfo.length; i++) {
   //       msg.append ("."+fileInfo[i].getExt());
   //       if (i<fileInfo.length-1) {
   //          msg.append (", ");
   //       }
   //    }
   //    GuiUtils.showError (comp, msg.toString());
   // }
}
