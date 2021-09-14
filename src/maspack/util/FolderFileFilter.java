/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

import java.io.File;
import javax.swing.filechooser.FileFilter;

public class FolderFileFilter extends FileFilter {

   String myDescription;

   public FolderFileFilter (String desc) {
      myDescription = desc;
   }

   @Override
   public boolean accept (File f) {
      return f.isDirectory();
   }
   
   @Override
   public String getDescription() {
      return myDescription;
   }
}
