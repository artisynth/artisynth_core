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
 * Chooses the file to export a numeric probe, with extra fields to select the
 * format and whether or not time data should be saved.
 */
public class ProbeExportChooser extends JFileChooser
   implements ActionListener, PropertyChangeListener {

   private static final long serialVersionUID = 1L;

   ExtensionFileFilter myCurrentFilter;
   Probe myProbe;
   JPanel myLastPanel;
   int myPropPanelIndex;
   PropertyPanel myPropPanel;
   ArrayList<LabeledControl> myControls = new ArrayList<>();
   ArrayList<Object> mySavedValues = new ArrayList<>();

   private int getLastJPanelIndex (Container comp) {
      for (int i=comp.getComponentCount()-1; i >= 0; i--) {
         if (comp.getComponent(i) instanceof JPanel) {
            return i;
         }
      }
      return -1;
   }   

   public void actionPerformed (ActionEvent e) {
      if (e.getActionCommand().equals(JFileChooser.CANCEL_SELECTION)) {
         restorePropertyValues();
      }
   }

   public void propertyChange(PropertyChangeEvent evt) {
      if (JFileChooser.FILE_FILTER_CHANGED_PROPERTY.equals (
             evt.getPropertyName())) {
         ExtensionFileFilter newFilter = null;
         if (getFileFilter() instanceof ExtensionFileFilter) {
            newFilter = (ExtensionFileFilter)getFileFilter();
         }
         if (myCurrentFilter != newFilter) {
            updatePropPanel (newFilter.getExtensions()[0]);
            myCurrentFilter = newFilter;
         }
      }
   }

   private File replaceFileExtension (File file, String ext) {
      String pathName = file.getAbsolutePath();
      int dotIndex = pathName.lastIndexOf(".");
      if (dotIndex == -1) {
         return new File(pathName + "." + ext);
      }
      else {
         return new File(pathName.substring(0, dotIndex) + "." + ext);
      }
   }

   protected void restorePropertyValues() {
      for (int i=0; i<myControls.size(); i++) {
         myControls.get(i).setValue (mySavedValues.get(i));
      }
   }

   private void updatePropPanel (String extension) {
      if (myLastPanel != null) {
         restorePropertyValues();
         myControls.clear();
         mySavedValues.clear();

         if (myPropPanel != null) {
            myLastPanel.remove (myPropPanelIndex);
            myLastPanel.remove (myPropPanelIndex);
         }
         ExportProps props = myProbe.getExportProps (extension);
         if (props != null) {
            PropertyPanel propPanel = new PropertyPanel();
            for (PropertyInfo info : props.getAllPropertyInfo()) {
               Property prop = props.getProperty (info.getName());
               LabeledComponentBase widget = propPanel.addWidget (prop);
               if (widget != null) {
                  widget.setLabelText (info.getDescription()+": ");
                  if (widget instanceof LabeledControl) {
                     LabeledControl control = (LabeledControl)widget;
                     myControls.add (control);
                     mySavedValues.add (control.getValue());
                  }
               }
            }
            propPanel.setBorder (
               BorderFactory.createEtchedBorder (EtchedBorder.LOWERED));  

            myLastPanel.add (propPanel, myPropPanelIndex);
            myLastPanel.add (new Box.Filler(
                                new Dimension(1,10),
                                new Dimension(1,10),
                                new Dimension(1,10)), myPropPanelIndex);
            myPropPanel = propPanel;
         }
         validate();
         repaint();
      }
   }

   public ProbeExportChooser (Probe probe) {
      
      File file = probe.getExportFile();
      ImportExportFileInfo[] fileInfo = probe.getExportFileInfo();
      myProbe = probe;

      if (file == null) {
         if (probe.getAttachedFile() != null) {
            file = replaceFileExtension (
               probe.getAttachedFile(), fileInfo[0].getExt());
         }
      }
      if (file == null) {
         setCurrentDirectory (ArtisynthPath.getWorkingDir());
      }
      else {
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
         if (file != null &&
             ArtisynthPath.getFileExtension(file).equalsIgnoreCase (
                info.getExt())){
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

      int lastPanelIndex = getLastJPanelIndex (this);
      if (lastPanelIndex != -1) {
         myLastPanel = (JPanel)getComponent(lastPanelIndex);
         myPropPanelIndex = getLastJPanelIndex (myLastPanel);
      }
      updatePropPanel (myCurrentFilter.getExtensions()[0]);
   }

   public String getSelectedExt() {
      if (getFileFilter() instanceof ExtensionFileFilter) {
         return ((ExtensionFileFilter)getFileFilter()).getExtensions()[0];
      }
      else {
         return null;
      }
   }
   
   public void showUnsupportedExtensionError (Component comp, String ext) {
      StringBuilder msg = new StringBuilder();
      msg.append ("File extension type '."+ext+"' is unsupported\n");
      msg.append ("Please use one of: ");
      ImportExportFileInfo[] fileInfo = myProbe.getExportFileInfo();
      for (int i=0; i<fileInfo.length; i++) {
         msg.append ("."+fileInfo[i].getExt());
         if (i<fileInfo.length-1) {
            msg.append (", ");
         }
      }
      GuiUtils.showError (comp, msg.toString());
   }
   

}
