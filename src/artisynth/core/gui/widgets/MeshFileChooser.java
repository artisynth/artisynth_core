package artisynth.core.gui.widgets;

import java.awt.*;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.imageio.ImageIO;

import java.io.*;
import java.util.*;
import java.util.List;

import maspack.widgets.*;
import artisynth.core.util.*;

/**
 * File chooser specialized for surface mesh files
 */
public class MeshFileChooser extends JFileChooser {

   ArrayList<FileFilter> myMeshFilters;
   ArrayList<FileFilter> myAllFilters;
   
   static class AllFilesFilter extends FileFilter {
      public boolean accept (File file) {
         return true;
      }
      
      public String getDescription () {
         return "All Files";
      }
   }

   private ArrayList<FileFilter> createMeshFilters (
      boolean forReading) {
      ArrayList<FileFilter> filters = new ArrayList<>();
      if (forReading) {
         filters.add (
            new ExtensionFileFilter (
               "Standard mesh files (*.obj,*.stl,*.ply,*.gts)",
               "obj", "stl", "ply", "gts"));        
      }
      filters.add (
         new ExtensionFileFilter ("Wavefront file (*.obj)", "obj"));
      filters.add (
         new ExtensionFileFilter ("STL file (*.stl)", "stl"));
      filters.add (
         new ExtensionFileFilter ("PLY file (*.ply)", "ply"));
      filters.add (
         new ExtensionFileFilter (
            "GNU triangulated surface file (*.gts)", "gts"));
      filters.add (
         new ExtensionFileFilter ("Xyz file (*.xyz)", "xyz"));
      filters.add (
         new ExtensionFileFilter ("Xyz binary file (*.xyzb)", "xyzb"));
      if (forReading) {
         filters.add (new AllFilesFilter());
      }
      return filters;
   }

   private String getFileExtension (File file) {
      if (file == null) {
         return null;
      }
      String fileName = file.getName();
      int dotIdx = fileName.lastIndexOf ('.');
      if (dotIdx != -1) {
         return fileName.substring (dotIdx+1);
      }
      else {
         return null;
      }
   }
   
   public MeshFileChooser (File file) {
      this (file, /*forReading=*/true, /*fileExtensions=*/null);
   }

   public MeshFileChooser (File file, boolean forReading) {
      this (file, forReading, /*fileExtensions=*/null);
   }

   public MeshFileChooser (
      File file, boolean forReading, List<ExtensionFileFilter> extraFilters) {
      super();
      
      if (file == null) {
         setCurrentDirectory(ArtisynthPath.getWorkingDir());
      }
      else {
         setCurrentDirectory (file);
         setSelectedFile (file);
      }
      for (FileFilter ff : getChoosableFileFilters()) {
         removeChoosableFileFilter(ff);
      }     
      if (!forReading) {
         setApproveButtonText("Save");
      }
      else {
         setApproveButtonText("Load");
      }

      // create and add file filters
      myMeshFilters = createMeshFilters(forReading);
      myAllFilters = new ArrayList<>();

      if (extraFilters != null) {
         myAllFilters.addAll (extraFilters);
      }
      myAllFilters.addAll (myMeshFilters);
      for (FileFilter filter : myAllFilters) {
         addChoosableFileFilter(filter);
      }

      // set the current filter, based on either the current file or a default
      // value
      FileFilter filter = null;
      if (file != null) {
         for (FileFilter f : getChoosableFileFilters()) {
            if (f.accept (file)) {
               filter = f;
            }
         }
      }
      if (filter == null) {
         filter = getChoosableFileFilters()[0];
      }
      setFileFilter(filter);
   }

   public File getSelectedFileWithExtension() {
      File file = getSelectedFile();
      if (getFileExtension(file) == null) {
         String ext = ((ExtensionFileFilter)getFileFilter()).getExtensions()[0];
         file =  new File(file.getPath() + "." + ext.toLowerCase());
      }
      return file;
   }

   /**
    * Returns the extension that should be associated with the selected
    * file. If the selected file is non-null and has extension, then that
    * extension is returned. Otherwise, returns the lowercase version of the
    * extension associated with the currently selected file filter.
    *
    * @return extension to be associated with the selected file
    */
   public String getSelectedFileExtension() {
      String ext = getFileExtension (getSelectedFile());
      if (ext == null) {
         ext = ((ExtensionFileFilter)getFileFilter()).getExtensions()[0];
         ext = ext.toLowerCase();
      }
      return ext;
   }

//   public boolean isValidFile (File file) {
//      String ext = getFileExtension (file);
//      if (ext != null) {
//         for (ExtensionFileFilter filter : myAllFilters) {
//            if (filter.containsExtension (ext)) {
//               return true;
//            }
//         }
//      }
//      return false;      
//   }
//
//   public int showValidatedDialog (Component comp, String approveButtonText) {
//      int returnVal = showDialog (comp, approveButtonText);
//      if (returnVal == JFileChooser.APPROVE_OPTION) {
//         File file = getSelectedFileWithExtension();
//         if (!isValidFile (file)) {
//            GuiUtils.showError (
//               comp, "Invalid file extension ." + getFileExtension(file));
//            returnVal = JFileChooser.ERROR_OPTION;
//         }
//      }
//      return returnVal;
//   }

   // public static void main (String[] args) {
   //    for (String s : MeshIO.getWriterFileSuffixes()) {
   //       System.out.println (s);
   //    }
   //    System.out.println ("");
   //    for (String s : MeshIO.getWriterFormatNames()) {
   //       System.out.println (s);
   //    }
      
   // }
}
