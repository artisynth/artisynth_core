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
 * File chooser specialized for image files
 */
public class ImageFileChooser extends JFileChooser {

   ArrayList<ExtensionFileFilter> myImageFilters;
   ArrayList<ExtensionFileFilter> myAllFilters;

   private ArrayList<ExtensionFileFilter> createImageFilters() {
      ArrayList<ExtensionFileFilter> filters = new ArrayList<>();
      filters.add (
         new ExtensionFileFilter ("PNG image (*.png)", "png"));
      filters.add (
         new ExtensionFileFilter ("JPEG image (*.jpg, *.jpeg)", "jpg", "jpeg"));
      filters.add (
         new ExtensionFileFilter ("GIF image (*.gif)", "gif"));
      filters.add (
         new ExtensionFileFilter ("Bitmap image (*.bmp)", "bmp"));
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
   
   public ImageFileChooser (File file) {
      this (file, /*fileExtensions=*/null);
   }

   public ImageFileChooser (File file, List<ExtensionFileFilter> extraFilters) {
      super();
      
      setApproveButtonText("Save");
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

      // create and add file filters
      myImageFilters = createImageFilters();
      myAllFilters = new ArrayList<>();

      if (extraFilters != null) {
         myAllFilters.addAll (extraFilters);
      }
      myAllFilters.addAll (myImageFilters);
      for (ExtensionFileFilter filter : myAllFilters) {
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

   public boolean isImageFile (File file) {
      String ext = getFileExtension (file);
      if (ext != null) {
         for (ExtensionFileFilter filter : myImageFilters) {
            if (filter.containsExtension (ext)) {
               return true;
            }
         }
      }
      return false;      
   }

   public boolean isValidFile (File file) {
      String ext = getFileExtension (file);
      if (ext != null) {
         for (ExtensionFileFilter filter : myAllFilters) {
            if (filter.containsExtension (ext)) {
               return true;
            }
         }
      }
      return false;      
   }

   public int showValidatedDialog (Component comp, String approveButtonText) {
      int returnVal = showDialog (comp, approveButtonText);
      if (returnVal == JFileChooser.APPROVE_OPTION) {
         File file = getSelectedFileWithExtension();
         if (!isValidFile (file)) {
            GuiUtils.showError (
               comp, "Invalid file extension ." + getFileExtension(file));
            returnVal = JFileChooser.ERROR_OPTION;
         }
      }
      return returnVal;
   }

   public static void main (String[] args) {
      for (String s : ImageIO.getWriterFileSuffixes()) {
         System.out.println (s);
      }
      System.out.println ("");
      for (String s : ImageIO.getWriterFormatNames()) {
         System.out.println (s);
      }
      
   }
}
