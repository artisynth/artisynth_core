/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import javax.swing.filechooser.FileFilter;

public class GenericFileFilter extends FileFilter {

   String[] exts;
   String desc;
   
   public GenericFileFilter(List<String> exts, String description) {
      this.exts = exts.toArray(new String[exts.size()]);
      desc = description;
   }
   
   public GenericFileFilter(String[] exts, String description) {
      this.exts = Arrays.copyOf(exts, exts.length);
      desc = description;
   }
   
   public GenericFileFilter (String ext, String description) {
      exts = new String[]{ext};
      desc = description;
   }
   
   public String addExtension(String fn) {
      String ext = getExtension(fn);
      
      if (ext == null || !isValidExtension(ext)) {
         fn = fn + "." + exts[0];
      }
      
      return fn;
   }
   
   public static String getExtension(File f) {
      String s = f.getName();
      return getExtension(s);
   }
   
   public static String getExtension(String s) {
      int i = s.lastIndexOf('.');
      String ext = null;
      
      if (i > 0 &&  i < s.length() - 1) {
         ext = s.substring(i+1).toLowerCase();
      }
      return ext;
   }
   
   public boolean isValidExtension(String ext) {
      for (String str : exts) {
         if (str.equalsIgnoreCase(ext)) {
            return true;
         }
      }
      return false;
   }
   
   @Override
   public boolean accept(File f) {
      
      if (f.isDirectory()) {
         return true;
      }
      
      String ext = getExtension(f);
      if (isValidExtension(ext)) {
         return true;
      }
      return false;
   }

   @Override
   public String getDescription() {
      return desc;
   }

}
