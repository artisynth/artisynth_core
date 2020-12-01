/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.util;

import java.io.File;
import java.util.Arrays;
import javax.swing.filechooser.FileFilter;

public class ExtensionFileFilter extends FileFilter {
   String description;
   String extensions[];

   public ExtensionFileFilter(String description, String extension) {
      this(description, new String[] { extension });
   }

   public ExtensionFileFilter(String description, String... extensions) {
      if (extensions.length == 0) {
         throw new IllegalArgumentException (
            "Must specify at least one file name extension");
      }
      if (description == null) {
         this.description = extensions[0] + "{ " + extensions.length + "} ";
      } 
      else {
         this.description = description;
      }
      this.extensions = (String[]) extensions.clone();
      toLower(this.extensions);
   }

   private void toLower(String array[]) {
      for (int i = 0, n = array.length; i < n; i++) {
         array[i] = array[i].toLowerCase();
      }
   }

   public String[] getExtensions() {
      return Arrays.copyOf (extensions, extensions.length);
   }

   public String getDescription() {
      return description;
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
   
   public boolean containsExtension (String fileExt) {
      fileExt = fileExt.toLowerCase();
      for (String ext : extensions) {
         if (fileExt.equals (ext)) {
            return true;
         }
      }
      return false;
   }
   
   public boolean accept(File file) {
      if (file.isDirectory()) {
         return true;
      } 
      else {
         String fileExt = getFileExtension (file);
         if (fileExt != null && containsExtension (fileExt)) {
            return true;
         }
         return false;
      }
   }
}