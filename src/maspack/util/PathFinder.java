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

public class PathFinder {

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

   public static String findSourceDir (String className) {
      Class<?> cls = getClassForName(className);
      return findSourceDir (cls);
   }


   public static String findSourceDir (Object obj) {
      if (obj instanceof Class<?>) {
         return findSourceDir ((Class<?>)obj);
      }
      else if (obj instanceof String) {
         return findSourceDir ((String)obj);
      }
      else {
         return findSourceDir (obj.getClass());
      }
   }

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
         StringBuilder srcDirName = new StringBuilder();
         srcDirName.append ("src");
         for (int k=0; k<pkgNames.length; k++) {
            srcDirName.append (SEP+pkgNames[k]);
         }
         if (dir.getParentFile() != null) {
            File srcDir = new File (dir.getParentFile(), srcDirName.toString());
            if (srcDir.isDirectory()) {
               return srcDir.toString();
            }
         }
      }
      throw new IllegalArgumentException (
         "Can't find source directory for '"+cls.getName()+"'");
   }

   public static String findClassDir (String className) {
      Class<?> cls = getClassForName(className);
      return doGetClassDir(cls).toString();
   }
      
   public static String findClassDir (Class<?> cls) {
      return doGetClassDir(cls).toString();
   }
      
   public static String findClassDir (Object obj) {
      if (obj instanceof Class<?>) {
         return findClassDir((Class<?>)obj);
      }
      return findClassDir (obj.getClass());
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
