/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Locates file paths relative to the class and source directories for Java
 * objects, and also expands path names using environment variables.
 * 
 * The methods for locating class and source directories work by climbing the
 * class's resource hierarchy. For finding source directories, it is assumed
 * that the root source directory is located relative to the parent of the root
 * class directory, via one of the paths specified by {@link
 * #getSourceRootPaths()}. By default, this list includes {@code "src"}, {@code
 * "source"}, and {@code "bin"}. Additional paths can be added using
 * {@link #addSourceRootPath}, or the entire list can be set using
 * {@link #setSourceRootPaths}.
 * 
 * @author lloyd
  */
public class PathFinder {

   private static ArrayList<String> mySourceRootPaths = new ArrayList<String>();
   
   static {
      mySourceRootPaths.add ("src");
      mySourceRootPaths.add ("source");
      mySourceRootPaths.add ("bin");
   }
   
   private PathFinder() {
   }

   /**
    * Returns true if a character is a legal starting character for an
    * environment variable.
    */
   private static boolean isEnvVariableStart (int c) {
      return (Character.isLetter (c) || c == '_');
   }

   /**
    * Returns true if a character is a legal non-starting character for an
    * environment variable.
    */
   private static boolean isEnvVariablePart (int c) {
      return (Character.isLetterOrDigit (c) || c == '_');
   }

   private static int indexOfWhitespace (String str, int idx0) {
      for (int i=idx0; i<str.length(); i++) {
         if (Character.isWhitespace (str.charAt(i))) {
            return i;
         }
      }
      return -1;
   }
   
   private static String extractWord (String str, int idx0) {
      int idx = idx0;
      int len = str.length();
      while (idx < len && Character.isWhitespace (str.charAt(idx))) {
         idx++;
      }
      int idxw = idx;
      while (idx < len && !Character.isWhitespace (str.charAt(idx))) {
         idx++;
      }
      return str.substring (idxw, idx);
   }

   private static Class<?> getClassForName (String className) {
      // start by trying to find class for name directly, in case it is
      // fully qualified.
      Class<?> cls = null;
      try {
         cls = Class.forName (className);
      }
      catch (Exception eignore) {
      }
      if (cls != null) {
         return cls;
      }
      else {
         // otherwise, assume className is a simple name and check for it in
         // all current packages
         Package[] packages = Package.getPackages();
         for (int i=0; i<packages.length; i++) {
            try {
               cls = Class.forName (packages[i].getName()+"."+className);
            }
            catch (Exception eignore) {
            }
            if (cls != null) {
               return cls;
            }
         }
      }
      throw new IllegalArgumentException (
         "Can't find class for name '"+className+"'");
   }
      
   private static String SEP = File.separator;

   private static File doGetClassDir (Class<?> cls) {
      URL url = cls.getResource (cls.getSimpleName()+".class");
      // URLs may be encoded in interesting ways, such as using %20 to indicate
      // ' ', so we use a URI to to the decoding. The exact details are still a
      // bit obscure to me ...
      URI uri = null;
      try {
         if (url != null) {
            uri = url.toURI();
         }
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      if (uri != null) {
         String path = uri.getPath();
         if (path != null) {
            File dir = new File (path);
            dir = dir.getParentFile();
            if (dir != null && dir.isDirectory()) {
               return dir;
            }
         }
      }
      throw new IllegalArgumentException (
         "Can't find class directory for '"+cls.getName()+"'");
   }

   /**
    * Returns a list of the paths used for detecting root source
    * directories relative to the parent of the root class directory.
    * 
    * @return list of the source root paths
    */
   public static ArrayList<String> getSourceRootPaths() {
      ArrayList<String> list = new ArrayList<String>();
      list.addAll (mySourceRootPaths);
      return list;
   }
   
   /**
    * Sets the list of the paths used for detecting root source
    * directories relative to the parent of the root class directory.
    * 
    * @param paths new list of the source root paths
    */
   public static void setSourceRootPaths(Collection<String> paths) {
      mySourceRootPaths.clear();
      mySourceRootPaths.addAll (paths);
   }
   
   /**
    * Adds a path to the list used for detecting root source directories 
    * relative to the parent of the root class directory.
    * 
    * @param path source root path to add
    */
   public static void addSourceRootPath (String path) {
      if (!mySourceRootPaths.contains(path)) {
         mySourceRootPaths.add (path);
      }
   }
   
   /**
    * Returns the path of the source directory for a class identified by name.
    * The name should be either a fully qualified class name, or a simple name
    * that can be located with respect to the packages obtained via {@link
    * Package#getPackages()}.
    * 
    * <p>This method works by climbing the class's resource hierarchy, and
    * assumes that the root source directory is located relative to the parent
    * of the root class directory by one of the paths listed in {@link
    * #getSourceRootPaths()}.
    * 
    * @param className name of the class
    * @return path to the source directory
    */
   public static String findSourceDir (String className) {
      Class<?> cls = getClassForName(className);
      return findSourceDir (cls);
   }

   /**
    * Returns the path of the source directory for a Java class identified by
    * {@code classObj}. The class is determined either directly, if
    * <code>classObj</code> is an instance of <code>Class</code>; by name, if
    * it is a {@code String}; or otherwise by calling
    * <code>classObj.getClass()</code>. The class must be associated with a
    * package.
    * 
    * <p>This method works by climbing the class's resource hierarchy, and
    * assumes that the root source directory is located relative to the parent
    * of the root class directory by one of the paths listed in {@link
    * #getSourceRootPaths()}.
    * 
    * @param classObj identifies the Java class
    * @return path to the source directory
    */
   public static String findSourceDir (Object classObj) {
      if (classObj instanceof Class<?>) {
         return findSourceDir ((Class<?>)classObj);
      }
      else if (classObj instanceof String) {
         return findSourceDir ((String)classObj);
      }
      else {
         return findSourceDir (classObj.getClass());
      }
   }

   /**
    * Returns the path of the source directory for a specific class.
    * 
    * <p>This method works by climbing the class's resource hierarchy, and
    * assumes that the root source directory is located relative to the parent
    * of the root class directory by one of the paths listed in {@link
    * #getSourceRootPaths()}.
    * 
    * @param cls identifies the class
    * @return path to the source directory
    */
   public static String findSourceDir (Class<?> cls) {
      File classDir = doGetClassDir (cls);
      if (new File (classDir, cls.getSimpleName()+".java").exists()) {
         return classDir.toString();
      }
      Package pkg = cls.getPackage();
      if (pkg != null) {
         String[] pkgNames = pkg.getName().split ("\\.");
         File dir = new File (classDir.toString());
         for (int k=0; k<pkgNames.length; k++) {
            dir = dir.getParentFile();
         }  
         // iterate over the list of source dir names until we find a source
         // directory
         for (String sname : mySourceRootPaths) {
            StringBuilder srcDirName = new StringBuilder();
            srcDirName.append (sname);
            for (int k=0; k<pkgNames.length; k++) {
               srcDirName.append (SEP+pkgNames[k]);
            }
            if (dir.getParentFile() != null) {
               File srcDir =
                  new File (dir.getParentFile(), srcDirName.toString());
               if (srcDir.isDirectory()) {
                  return srcDir.toString();
               }
            }
         }
      }
      throw new IllegalArgumentException (
         "Can't find source directory for '"+cls.getName()+"'");
   }

   /**
    * Returns the path of the class directory for a class identified by name.
    * The name should be either a fully qualified class name, or a simple
    * name that can be located with respect to the packages obtained via {@link
    * Package#getPackages()}.
    *
    * <p>This method works by climbing the class's resource hierarchy.
    * 
    * @param className name of the class
    * @return path to the class directory
    */
   public static String findClassDir (String className) {
      Class<?> cls = getClassForName(className);
      return doGetClassDir(cls).toString();
   } 

   /**
    * Returns the path of the class directory for a specific class.
    * 
    * <p>This method works by climbing the class's resource hierarchy.
    * 
    * @param cls identifies the class
    * @return path to the class directory
    */     
   public static String findClassDir (Class<?> cls) {
      return doGetClassDir(cls).toString();
   }

   /**
    * Returns the path of the class directory for a Java class identified by
    * {@code classObj}. The class is determined either directly, if
    * <code>classObj</code> is an instance of <code>Class</code>; by name, if
    * it is a {@code String}; or otherwise by calling
    * <code>classObj.getClass()</code>. The class must be associated with a
    * package.
    * 
    * <p>This method works by climbing the class's resource hierarchy.
    * 
    * @param classObj identifies the Java class
    * @return path to the class directory
    */   
   public static String findClassDir (Object classObj) {
      if (classObj instanceof Class<?>) {
         return findClassDir((Class<?>)classObj);
      }
      else if (classObj instanceof String) {
         return findClassDir ((String)classObj);
      }
      else {
         return findClassDir (classObj.getClass());
      }
   }
      
   private static String expandVariable (String var) {
      int idxws = indexOfWhitespace (var, 0);
      if (idxws != -1) {
         String cmd = var.substring (0, idxws);
         String arg = extractWord (var, idxws);
         if (cmd.equals ("classdir")) {
            if (!arg.equals ("")) {
               return findClassDir (arg);
            }
         }
         else if (cmd.equals ("srcdir")) {
            if (!arg.equals ("")) {
               return findSourceDir (arg);
            }
         }
         else {
            throw new IllegalArgumentException (
              "Unrecognized command '"+cmd+"' in '${}' expression"); 
         }
         throw new IllegalArgumentException (
            "No argument following '"+cmd+"' in '${}' expression");
      }
      else {
         String value = System.getenv (var);
         if (value == null) {
            value = System.getProperty (var, null);
         }
         if (value == null) {
            return "";
         }
         else {
            return value;
         }
      }
   }

   /**
    * Returns a complete file path from a path relative to the source directory
    * for a specified class. The class is determined from
    * <code>classObj</code>, either directly, if <code>classObj</code> is an
    * instance of <code>Class</code>; by name, if it is a {@code String}; or
    * otherwise by calling <code>classObj.getClass()</code>. The class must be
    * associated with a package.
    *
    * <p>This method works by climbing the class's resource hierarchy, and
    * assumes that the root source directory is located relative to the parent
    * of the root class directory by one of the paths listed in {@link
    * #getSourceRootPaths()}.
    *
    * <p>If the relative path specified by <code>relpath</code> is
    * <code>null</code>, then the path for the source directory itself is
    * returned. Within the returned file path, instances of either the Unix or
    * Windows file separator characters (i.e., <code>'/'</code> or
    * <code>'\'</code>) are mapped to the file separator character for the
    * current system.
    * 
    * @param classObj
    * object used to deterine the class
    * @param relpath
    * path relative to class's source directory
    * @return expanded file path
    */
   public static String getSourceRelativePath (Object classObj, String relpath) {
      String srcPath = findSourceDir (classObj);
      if (relpath != null) {
         srcPath += File.separatorChar + relpath;
      }
      return convertToLocalSeparators(srcPath);
   }

   /**
    * Returns a complete file path from a path relative to the class
    * directory for a specified class. The class is determined from
    * <code>classObj</code>, either directly, if <code>classObj</code> is an
    * instance of <code>Class</code>, or by calling
    * <code>classObj.getClass()</code>; by name, if it is a {@code String}; or
    * otherwise by calling <code>classObj.getClass()</code>. The class must be
    * associated with a package.
    *
    * <p>This method works by climbing the class's resource hierarchy.
    *
    * <p>If the relative path specified by <code>relpath</code> is
    * <code>null</code>, then the path for the class directory itself is
    * returned.  Within the returned file path, instances of either the Unix or
    * Windows file separator characters (i.e., <code>'/'</code> or
    * <code>'\'</code>) are mapped to the file separator character for the
    * current system.
    * 
    * @param classObj
    * object used to deterine the class
    * @param relpath
    * path relative to class's class directory
    * @return expanded file path
    */
   public static String getClassRelativePath (Object classObj, String relpath) {
      String srcPath = findClassDir (classObj);
      if (relpath != null) {
         srcPath += File.separatorChar + relpath;
      }
      return convertToLocalSeparators(srcPath);
   }

   /**
    * Expands a path name to include environment and special variables
    * identified by a dollar sign ({@code '$'}). Valid expansions include:
    * <pre>
    *  $ENV_VAR          - value of the environment variable ENV_VAR
    *  ${srcdir CLASS}   - source directory of class identified by CLASS
    *  ${classdir CLASS} - class directory of class identified by CLASS
    *  $$                - a dollar sign '$'
    * </pre>
    * In the above description, {@code CLASS} is either a fully qualified
    * class name, or a simple name that can be located with respect to
    * the packages obtained via {@link Package#getPackages()}.
    *
    * @param path path name to expand
    * @return expanded path name
    */
   public static String expand (String path) {
      if (path.indexOf ("$") == -1) {
         return path;
      }
      else {
         StringBuilder newstr = new StringBuilder();

         char c;
         int idx = 0;
         int len = path.length();
         while (idx < len) {
            c = path.charAt (idx++);
            if (c == '$') {
               if (idx < len) {
                  c = path.charAt (idx);
                  if (c == '$') {
                     idx++;
                     newstr.append ('$');
                  }
                  else if (c == '{') {
                     idx++;
                     int idx0 = idx;
                     while (idx < len && path.charAt(idx) != '}') {
                        idx++;
                     }
                     if (idx == len) {
                        throw new IllegalArgumentException (
                           "Missing '}' in '${}' expression");
                     }
                     newstr.append (expandVariable (path.substring (idx0, idx)));
                     idx++; // swallow trailing '}'
                  }
                  else if (isEnvVariableStart(c)) {
                     int idx0 = idx++;
                     while (idx < len && isEnvVariablePart (path.charAt(idx))) {
                        idx++;
                     }
                     newstr.append (expandVariable (path.substring (idx0, idx)));
                  }
               }
            }
            else {
               newstr.append (c);
            }            
         }
         return newstr.toString();
      }
   }

   /**
    * Tries to convert file separation characters in a path name to ones
    * appropriate for the local system. In particular, on Unix, the Windows
    * separation character <code>'\'</code> will be converted to
    * <code>'/'</code>, and on Windows, <code>'/'</code> will be converted
    * to a <code>'\'</code>.
    * 
    * @return converted path name
    */
   public static String convertToLocalSeparators (String pathName) {
      if (File.separatorChar == '\\') {
         return convertToWindowsSeparators (pathName);
      }
      else {
         return convertToUnixSeparators (pathName);
      }
   }

   /**
    * Tries to convert file separation characters in a path name to ones
    * appropriate for Unix. In particular, the Windows separation character
    * <code>'\'</code> will be converted to <code>'/'</code>.
    * 
    * @return converted path name
    */
   public static String convertToUnixSeparators (String pathName) {
      if (pathName == null) {
         return null;
      }
      else {
         return pathName.replace ('\\', '/');
      }
   }

   /**
    * Tries to convert file separation characters in a path name to ones
    * appropriate for Windows. In particular, the Unix separation character
    * <code>'/'</code> will be converted to <code>'\'</code>.
    * 
    * @return converted path name
    */
   public static String convertToWindowsSeparators (String pathName) {
      if (pathName == null) {
         return null;
      }
      else {
         return pathName.replace ('/', '\\');
      }
   }

}
