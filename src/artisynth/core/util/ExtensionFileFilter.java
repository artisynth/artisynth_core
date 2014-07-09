/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.util;

import java.io.File;
import javax.swing.filechooser.FileFilter;

public class ExtensionFileFilter extends FileFilter {
   String description;
   String extensions[];

   public ExtensionFileFilter(String description, String extension) {
      this(description, new String[] { extension });
   }

   public ExtensionFileFilter(String description, String extensions[]) {
      if (description == null) {
         this.description = extensions[0] + "{ " + extensions.length + "} ";
      } else {
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
      String[] exts = new String[extensions.length];
      for (int i=0; i<exts.length; i++) {
         exts[i] = extensions[i];
      }
      return exts;
   }

   public String getDescription() {
      return description;
   }

   public String getExtension (File file) {
      if (file.isDirectory()) {
         return null;
      }
      String path = file.getAbsolutePath().toLowerCase();
      for (int i = 0, n = extensions.length; i < n; i++) {
         String extension = extensions[i];
         if ((path.endsWith(extension) &&
              (path.charAt(path.length() - extension.length() - 1)) == '.')) {
            return extension;
         }
      }
      return null;
   }

   public boolean accept(File file) {
      if (file.isDirectory()) {
         return true;
      } else {
         return getExtension(file) != null;
      }
   }
}