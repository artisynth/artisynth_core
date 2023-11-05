package artisynth.core.gui.widgets;

import java.awt.Container;
import java.awt.Dimension;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;

import artisynth.core.util.ArtisynthPath;
import artisynth.core.util.ExtensionFileFilter;
import artisynth.core.gui.widgets.PanelFileChooser;
import artisynth.core.femmodels.NodeNumberWriter;
import maspack.widgets.BooleanSelector;
import maspack.widgets.*;
import maspack.util.GenericFileFilter;

/**
 * File chooser for files containing node numbers.
 */
public class NodeNumberFileChooser extends PanelFileChooser {

   private static final long serialVersionUID = 1L;

   ExtensionFileFilter myTxtFilter;
   BooleanSelector myAddHeaderComment;
   BooleanSelector myUseBrackets;
   IntegerField myMaxColumns;

   public NodeNumberFileChooser (File existingFile) {
      build (existingFile);
   }

   public NodeNumberFileChooser (
      File existingFile, int maxCols, int flags) {

      boolean useBrackets = ((flags & NodeNumberWriter.USE_BRACKETS) != 0);
      boolean addHeader = ((flags & NodeNumberWriter.ADD_HEADER_COMMENT) != 0);

      myMaxColumns = new IntegerField ("Max columns:", maxCols);
      myUseBrackets = new BooleanSelector ("Use brackets:", useBrackets);
      myAddHeaderComment = new BooleanSelector ("Add header:", addHeader);

      build (existingFile);
   }

   protected void build (File existingFile) {
      myTxtFilter = new ExtensionFileFilter ("Text file (*.txt)", "txt");
      addChoosableFileFilter (myTxtFilter);
      if (existingFile != null) {
         setSelectedFile (existingFile);
         String ext = ExtensionFileFilter.getFileExtension (existingFile);
         if (ext != null && ext.toLowerCase().equals ("txt")) {
            setFileFilter (myTxtFilter);
         }
      }
      else {
         setFileFilter (myTxtFilter);
      }
      if (myMaxColumns != null) {
         PropertyPanel panel = createPropertyPanel();
         panel.addWidget (myMaxColumns);
         panel.addWidget (myUseBrackets);
         panel.addWidget (myAddHeaderComment);
         setApproveButtonText("Save as");      
      }
      else {
         setApproveButtonText("Load");      
      }
   }

   public int getMaxColumns() {
      if (myMaxColumns != null) {
         return myMaxColumns.getIntValue();
      }
      else {
         return 0;
      }
   }

   public int getFlags() {
      int flags = 0;
      if (myUseBrackets != null && myUseBrackets.getBooleanValue()) {
         flags |= NodeNumberWriter.USE_BRACKETS;
      }
      if (myAddHeaderComment != null && myAddHeaderComment.getBooleanValue()) {
         flags |= NodeNumberWriter.ADD_HEADER_COMMENT;
      }
      return flags;
   }

   public boolean isTxtFilterSelected() {
      return getFileFilter() == myTxtFilter;
   }

   /**
    * Fix problems with '.' in the path.
    */
   public File getFixedSelectedFile() {
      File file = getSelectedFile();
      File parent = file.getParentFile();
      if (parent.getName().equals(".")) {
         if (parent.getParent() == null) {
            parent = parent.getAbsoluteFile();
         }
         if (parent.getName().equals(".")) {
            parent = parent.getParentFile();
         }
         file = new File (parent, file.getName());
      }
      return file;
   }

   /**
    * Returns the selected file, appending a .art extension if there is no
    * extension and the .art filter is selected.
    */
   public File getSelectedFileWithExtension() {
      File file = getFixedSelectedFile();
      if (file.getName().indexOf ('.') == -1 && isTxtFilterSelected()) {
         file = new File(file.getPath() + ".txt");
      }
      return file;
   }
}
