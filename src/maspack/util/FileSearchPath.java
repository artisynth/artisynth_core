package maspack.util;

import java.io.*;
import java.util.*;

/**
 * Contains a list of directories in which to search for files that don't have
 * an absolute path.
 */
public class FileSearchPath {

   ArrayList<File> myDirs;
   
   public FileSearchPath () {
      myDirs = new ArrayList<>();
   }

   public FileSearchPath (File... dirs) {
      this();
      setDirectories (dirs);
   }

   public ArrayList<File> getDirectories() {
      return myDirs;
   }

   public void setDirectories (File[] dirs) {
      myDirs.clear();
      if (dirs != null) {
         for (int i=0; i<dirs.length; i++) {
            File dir = dirs[i];
            if (!dir.getName().equals(".") &&
                !dir.isAbsolute()) {
               throw new IllegalArgumentException (
                  "Directory "+dir+" is not absolute");
            }
            myDirs.add (dir);
         }
      }
   }

   public int numDirectories() {
      return myDirs.size();
   }

   public void addDirectory (File dir) {
      if (!dir.getName().equals(".") &&
          !dir.isAbsolute()) {
         throw new IllegalArgumentException (
            "Directory "+dir+" is not absolute");
      }
      myDirs.add (dir);
   }

   public static File getParentFile (File file) {
      if (!file.isAbsolute()) {
         file = new File(file.getAbsolutePath());
      }
      return file.getParentFile();
   }

   public void addDirectory (int idx, File dir) {
      if (!dir.getName().equals(".") &&
          !dir.isAbsolute()) {
         throw new IllegalArgumentException (
            "Directory "+dir+" is not absolute");
      }
      myDirs.add (idx, dir);
   }

   public File findFile (String path) {
      return findFile (new File(path));
   }

   public File findFile (File file) {
      if (file.isAbsolute()) {
         if (file.exists()) {
            return file;
         }
         else {
            return null;
         }
      }
      else {
         for (File dir : myDirs) {
            if (dir.getName().equals (".")) {
               dir = new File(dir.getAbsolutePath()).getParentFile();
            }
            File found = new File (dir, file.getPath());
            if (found.exists()) {
               return found;
            }
         }
         return null;
      }
   }
   
   /**
    * Find the path for a file. If the file can be expressed relative to one of
    * the search path directories, return the relative path. Otherwise, return
    * the absolute path.
    *
    * <p>In case the file can be expressed relative to more than one of the
    * search path directories, the shortest relative path is returned.
    * 
    * @param file file to find the path for
    * @return file path, either absolute or relative to one
    * of the search directories. 
    */
   public String findPath (File file) {
      if (file.isAbsolute()) {
         String path = file.getAbsolutePath();
         String minpath = null;
         for (File dir : myDirs) {
            String dirPath;
            if (dir.getName().equals (".")) {
               dirPath = new File(dir.getAbsolutePath()).getParent();
            }
            else {
               dirPath = dir.getAbsolutePath();
            }
            if (path.startsWith (dirPath)) {
               int dlen = dirPath.length();
               // make sure we are also at a file separator
               if (path.length() > dlen+1 && 
                   path.charAt(dlen) == File.separatorChar) { 
                  String relpath = path.substring(dlen+1);
                  if (minpath == null || relpath.length() < minpath.length()) {
                     minpath = relpath;
                  }
               }
            }
         }
         if (minpath != null) {
            return minpath;
         }
      }
      else {
         // if we can find the file, then the relative path is Ok
         if (findFile (file) != null) {
            return file.getPath();
         }
      }
      return file.getAbsolutePath();
   }
}
