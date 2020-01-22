/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.util;

import java.io.File;
import java.io.FileFilter;
import java.io.InputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Iterator;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.URI;

public class ArtisynthPath {
   private static File myWorkingDir = null;

   private static String myHomeDir = null;
   private static boolean myHomeDirSet = false;
  // private static String myArtisynthPath = null;

   private static boolean myBaseResourceInitialized = false;
   // private static URL myBaseResource = null;
   private static String myBaseClassResource = null;
   private static String myBaseSourceResource = null;

   private ArtisynthPath() {
   }

   private String getInferredPath() {
      URL url = getClass().getResource ("ArtisynthPath.class");
      if (url == null) {
         return null;
      }
      // URLs may be encoded in interesting ways, such as using %20 to
      // indicate ' ', so we use a URI to to the decoding. The exact
      // details are still a bit obscure to me ...
      URI uri = null;
      try {
         uri = url.toURI();
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      if (uri == null || uri.getPath() == null) {
         return null;
      }
      File file = new File (uri.getPath());
      // ARTISYNTH_HOME/classes should be four levels up ...
      for (int k = 0; k < 4; k++) {
         file = file.getParentFile();
      }
      // ... and the parent of this should be ARTISYNTH_HOME:
      String path = file.getParent();
      return path;
   }

   private static int numDotsInStr (String str) {
      int num = 0;
      for (int i=0; i<str.length(); i++) {
         if (str.charAt(i) == '.') {
            num++;
         }
      }
      return num;
   }

   static private String getSrcPath (Class<?> clazz, boolean rootOnly) {
      URL url = clazz.getResource (clazz.getSimpleName()+".class");
      if (url == null) {
         return null;
      }
      URI uri = null;
      try {
         uri = url.toURI();
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      if (uri == null) {
         return null;
      }
      String path = uri.toString();
      if (!path.startsWith ("file:")) {
         return null;
      }
      
      File file = new File(uri.getSchemeSpecificPart ());
      path = file.getAbsolutePath ();
      int nseps = 1;
      if (rootOnly) {
         nseps++;
      }
      if (clazz.getPackage() != null) {
         nseps += numDotsInStr(clazz.getPackage().getName()) + 1;
      }
      // start at the end of the path and back up by nseps separators
      int idx = path.length()-1;
      int cnt = 0;
      int lastIdx = -1;
      while (idx >= 0) {
         if (path.charAt (idx) == File.separatorChar) {
            if (cnt == 0) {
               lastIdx = idx;
            }
            cnt++;
            if (cnt == nseps) {
               break;
            }
         }
         idx--;
      }
      if (cnt != nseps) {
         return null;
      }
      if (rootOnly) {
         return path.substring (0, idx);
      }
      else {
         String fullPath = path.substring (0, idx+1);
         //System.out.println ("path=" + fullPath);
         final String classes = File.separator + "classes" + File.separator;
         final String src = File.separator + "src" + File.separator;
         if (fullPath.endsWith (classes)) {
            return fullPath.substring(0, fullPath.length()-9)+ src +
               path.substring (idx+1, lastIdx);
         }
         else {
            return path.substring (0, lastIdx);
         }
      }
   }     

   static private void initBaseResource() {
      myBaseResourceInitialized = true;
      ArtisynthPath ap = new ArtisynthPath();
      URL url = ap.getClass().getResource ("ArtisynthPath.class");
      if (url == null) {
         return;
      }
      URI uri = null;
      try {
         uri = url.toURI();
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      if (uri == null) {
         return;
      }
      String path = uri.toString();
      int idx = path.length();
      for (int i = 0; i < 4; i++) {
         if (idx < 1) {
            return;
         }
         idx = path.lastIndexOf ('/', idx - 1);
      }
      if (idx == -1) {
         return;
      }
      myBaseClassResource = path.substring (0, idx + 1);
      if (myBaseClassResource.endsWith ("/classes/")) {
         myBaseSourceResource =
            myBaseClassResource.substring (0, myBaseClassResource.length() - 9)
            + "/src/";
      }
   }

   /**
    * Returns a URL resource specified by an absolute path with respect to the
    * ArtiSynth package hierarchy.
    * 
    * <p>
    * The path should be a standard UNIX-style path, with <code>/</code> as a
    * file separator and parent directories delimited by <code>..</code>. It
    * may optionally begin with a <code>/</code> character.
    * 
    * <p>
    * If the source file hierarchy is separate from the class file hierarchy,
    * then the resource is first sought with respect to the source file
    * hierarchy. If the requested resource cannot be found with respect to
    * either a source file hierarchy or the class file hierarchy, then
    * <code>null</code> is returned.
    * 
    * @param abspath
    * absolute path for the resource.
    * @throws IllegalArgumentException
    * if the path is improperly specified.
    */
   public static URL getResource (String abspath) {
      if (!myBaseResourceInitialized) {
         initBaseResource();
      }
      if (abspath.length() == 0) {
         throw new IllegalArgumentException ("abspath has zero length");
      }
      String path = parsePath ("", abspath);
      URL url = null;
      if (myBaseSourceResource != null) {
         try {
            URI uri = new URI (myBaseSourceResource + path);
            url = uri.toURL();
         }
         catch (Exception e) {
            url = null;
         }
      }
      if (url == null && myBaseClassResource != null) {
         try {
            URI uri = new URI (myBaseClassResource + path);
            url = uri.toURL();
         }
         catch (Exception e) {
            url = null;
         }
      }
      return url;
   }

   /**
    * Returns a URL resource specified by a path relative to a given reference
    * object.
    * 
    * <p>
    * The path should be a standard UNIX-style path, with <code>/</code> as a
    * file separator and parent directories delimited by <code>..</code>. It
    * should <i>not</i> begin with a <code>/</code>.
    * 
    * <p>
    * The reference object must be a associated with class contained within the
    * ArtiSynth package hierarchy. If the reference object is itself a Class
    * object, then the associated class is the object itself. The location of
    * the resource within the package hierarchy is given by appending the
    * specified path to the class package path.
    * 
    * <p>
    * If the source file hierarchy is separate from the class file hierarchy,
    * then the resource is first sought with respect to the source file
    * hierarchy. If the requested resource cannot be found with respect to
    * either a source file hierarchy or the class file hierarchy, then
    * <code>null</code> is returned.
    * 
    * @param refObj
    * reference object or class
    * @param relpath
    * resource path
    * @return URL for the resource, or null if no resource was found
    * @throws IllegalArgumentException
    * if the path is improperly specified.
    */
   public static URL getRelativeResource (Object refObj, String relpath) {
      if (relpath.length() != 0 && relpath.charAt (0) == '/') {
         throw new IllegalArgumentException ("relpath must not begin with '/'");
      }
      Class<?> cls =
         (refObj instanceof Class ? (Class<?>)refObj : refObj.getClass());
      Package pkg = cls.getPackage();
      if (pkg == null) {
         return null;
      }
      String fullpath = parsePath (pkg.getName().replace ('.', '/'), relpath);
      return getResource (fullpath);
   }

   /**
    * Finds the Artisynth temporary directory.  Uses the ARTISYNTH_TMP 
    * environment variable, if defined.  Otherwise, a "tmp/" directory will be 
    * created in the ARTISYNTH_HOME directory.
    * 
    * @see ArtisynthPath#getHomeDir()
    * @return Artisynth tmp directory
    */
   public static File getTempDir() {
      File tmp = null;
      
      String tmpEnv  = System.getenv ("ARTISYNTH_TMP");
      if (tmpEnv != null) {
         // tmp = findfile("tmp/");
         tmp = new File(tmpEnv);
      } else {
         tmp = new File(ArtisynthPath.getHomeDir() + "/tmp/");   
      }
      
      if (!tmp.exists()) {
         tmp.mkdirs();
      }
      
      return tmp;
   }
   
   /**
    * Finds the Artisynth cache directory.  Uses the ARTISYNTH_CACHE 
    * environment variable if defined.  Otherwise, a ".cache/" directory will be created
    * in the ARTISYNTH_TMP directory.
    * 
    * @see #getTempDir()
    * @return Artisynth cache directory
    */
   public static File getCacheDir() {
      File cache = null;
      
      String cacheEnv  = System.getenv ("ARTISYNTH_CACHE");
      if (cacheEnv != null) {
    	  //         cache = findFile(".cache/");
    	  //      } else {
         cache = new File(cacheEnv);
      }
      if (cache == null) {
         cache = new File(ArtisynthPath.getTempDir() + "/.cache/");
      }
      if (!cache.exists()) {
         cache.mkdirs();
      }
      
      return cache;
   }
  
   
   /**
    * Returns the Artisynth home directory, as specified by the ARTISYNTH_HOME
    * environment variable. If this variable is not set, an attempt is made to
    * infer the home location from the location of this class, and if that is
    * not successful, "." is returned.
    * 
    * @return Artisynth home directory
    */
   public static String getHomeDir() {
      if (!myHomeDirSet) {
         // first try getting ArtiSynthHome from the environment
         myHomeDir = System.getenv ("ARTISYNTH_HOME");
         if (myHomeDir == null) { // otherwise, try getting it automatically
            myHomeDir = (new ArtisynthPath()).getInferredPath();
            if (myHomeDir == null) {
               System.out.println (
                 "Warning: ARTISYNTH_HOME can't be inferred; defaulting to '.'");
               myHomeDir = ".";
            }
         }
         if (!myHomeDir.startsWith (File.separator)) {
            // expand the home directory into a full path
            File dir = new File (myHomeDir);
            String fullPath;
            try {
               fullPath = dir.getCanonicalPath();
            }
            catch (IOException e) {
               fullPath = dir.getAbsolutePath();
               System.err.println (
                  "Warning: could obtain canonical path for home directory:\n"
                  + myHomeDir);
            }
            myHomeDir = fullPath;
         }
         // System.out.println ("ARTISYNTH_HOME="+myHomeDir);
         myHomeDirSet = true;
      }
      return myHomeDir;
   }

   /**
    * Returns the Artisynth home directory, as specified by the ARTISYNTH_HOME
    * environment variable. If ARTISYNTH_HOME is not set, <code>alternate</code>
    * is returned instead.
    * 
    * @param alternate
    * returns this if ARTISYNTH_HOME is not set
    * @return Artisynth home directory
    */
   private static String getHomeDir (String alternate) {
      String dir = getHomeDir();
      return dir != null ? dir : alternate;
   }

   /**
    * Returns a file specified relative to the Artisynth home directory, as
    * specified by the environment variable ARTISYNTH_HOME. If ARTISYNTH_HOME is
    * not set, <code>alternate</code> is used instead. Any instances of either
    * the Unix or Windows file separator characters (i.e., <code>'/'</code> or
    * <code>'\'</code>) are mapped to the file separator character for the
    * current system.
    * 
    * @param relpath
    * file path name relative to ARTISYNTH_HOME
    * @param alternate
    * used if ARTISYNTH_HOME is not set
    * @return file relative to ARTISYNTH_HOME
    */
   public static File getHomeRelativeFile (String relpath, String alternate) {
      String dir = getHomeDir (alternate);
      if (dir == null) {
         return null;
      }
      String pathName = convertToLocalSeparators (relpath);
      File file;

      if (dir.charAt (dir.length() - 1) == File.separatorChar) {
         file = new File (dir + pathName);
      }
      else {
         file = new File (dir + File.separator + pathName);
      }
      return file;
   }

   /**
    * Returns a file path specified relative to the <i>root</i> directory 
    * associated with a class. This method works by climbing the
    * class resource hierarchy. The root directory is assumed to be one level
    * above the top directory for the class hierarchy.  For example, if the
    * class is <code>artisynth.core.workspace.RootModel</code>,
    * and its class file is located in 
    * <pre>
    *    /users/home/joe/artisynth_core/classes/artisynth/core/modelbase
    * </pre>
    * then the root directory is
    * <pre>
    *    /users/home/joe/artisynth_core
    * </pre>
    *
    * <p>The class is determined from <code>classObj</code>, either directly,
    * if <code>classObj</code> is an instance of <code>Class</code>, or by
    * calling <code>classObj.getClass()</code>. The class must be associated
    * with a package.
    *
    * <p>If the relative path specified by <code>relpath</code> is
    * <code>null</code>, then the path for the root directory itself is
    * returned.  Within the returned file path, instances of either the Unix or
    * Windows file separator characters (i.e., <code>'/'</code> or
    * <code>'\'</code>) are mapped to the file separator character for the
    * current system.
    * 
    * @param classObj
    * object used to determine the class
    * @param relpath
    * path relative to the root directory
    * @return file path specified relative to the root directory
    */
   public static String getRootRelativePath (Object classObj, String relpath) {
      Class<?> cls =
         (classObj instanceof Class ? (Class<?>)classObj : classObj.getClass());
      String rootPath = getSrcPath (cls, /*root=*/true);
      if (relpath != null) {
         rootPath += '/' + relpath;
      }
      return convertToLocalSeparators (rootPath);
   }

   /**
    * Returns a File specified relative to the <i>root</i> directory associated
    * with a class. This method is a wrapper for {@link #getRootRelativePath
    * getRootRelativePath()}; see that method for more information about how the
    * file path is created.
    * 
    * @param classObj
    * object used to determine the class
    * @param relpath
    * path relative to the root directory
    * @return file specified relative to the root directory
    */
   public static File getRootRelativeFile (Object classObj, String relpath) {
      String path = getSrcRelativePath (classObj, relpath);
      if (path == null) {
         return null;
      }
      else {
         return new File(path);
      }
   }

   /**
    * Creates a full path from a path specified relative to the Artisynth home
    * directory, as specified by the environment variable ARTISYNTH_HOME. If
    * ARTISYNTH_HOME is not set, <code>alternate</code> is used instead. Any
    * instances of either the Unix or Windows file separator characters (i.e.,
    * <code>'/'</code> or <code>'\'</code>) are mapped to the file
    * separator character for the current system.
    * 
    * @param relpath
    * file path name relative to ARTISYNTH_HOME
    * @param alternate
    * used if ARTISYNTH_HOME is not set
    * @return full path
    */
   public static String getHomeRelativePath (String relpath, String alternate) {
      String dir = getHomeDir (alternate);
      if (dir == null) {
         return null;
      }
      String pathName = convertToLocalSeparators (relpath);
      String totalPath;
      if (dir.charAt (dir.length() - 1) == File.separatorChar) {
         totalPath = dir + pathName;
      }
      else {
         totalPath = dir + File.separator + pathName;
      }
      return totalPath;
   }

   static String parsePath (String base, String relpath) {
      if (relpath.length() == 0) {
         return base;
      }
      StringBuilder builder = new StringBuilder (base);
      int idx0 = 0;
      int idx1;
      if (relpath.charAt (0) == '/') {
         idx0 = 1;
         builder.setLength (0);
      }
      while ((idx1 = relpath.indexOf ('/', idx0)) != -1) {
         if (idx1 == idx0) {
            throw new IllegalArgumentException (
               "path contains repeated slashes \"//\"");
         }
         else if (idx1 - idx0 == 1 && relpath.charAt (idx0) == '.') {
            // eat the sequence; do nothing
         }
         else if (idx1 - idx0 == 2 && relpath.charAt (idx0) == '.'
         && relpath.charAt (idx0 + 1) == '.') { // try to go up in the sequence
            int slashIdx = builder.lastIndexOf ("/");
            if (slashIdx == -1) {
               throw new IllegalArgumentException (
                  "path ascends higher than package");
            }
            builder.setLength (slashIdx);
         }
         else {
            if (builder.length() != 0) {
               builder.append ('/');
            }
            builder.append (relpath.substring (idx0, idx1));
         }
         idx0 = idx1 + 1;
      }
      if (idx0 < relpath.length()) {
         if (builder.length() != 0) {
            builder.append ('/');
         }
         builder.append (relpath.substring (idx0, relpath.length()));
      }
      return builder.toString();
   }

   /**
    * Returns a complete file path from a path relative to the source directory
    * for a specified class. This method works by climbing the class's resource
    * hierarchy. For this method to work, the root directory of the source file
    * hierarchy must be named <code>src</code>, and if classes are stored in a
    * separate file hierarchy, the root directory of that must be named
    * <code>classes</code> and must be a sibling of <code>src</code>.
    *
    * <p>The class is determined from <code>classObj</code>, either directly,
    * if <code>classObj</code> is an instance of <code>Class</code>, or by
    * calling <code>classObj.getClass()</code>. The class must be associated
    * with a package.
    *
    * <p>If the relative path specified by <code>relpath</code> is
    * <code>null</code>, then the path for the source directory itself is
    * returned. Within the returned file path, instances of either the Unix or
    * Windows file separator characters (i.e., <code>'/'</code> or
    * <code>'\'</code>) are mapped to the file separator character for the
    * current system.
    *
    * 
    * @param classObj
    * object used to determine the class
    * @param relpath
    * path relative to class's source directory
    * @return file path specified relative to the source directory
    */
   public static String getSrcRelativePath (Object classObj, String relpath) {
      Class<?> cls =
         (classObj instanceof Class ? (Class<?>)classObj : classObj.getClass());
      String srcPath = getSrcPath (cls, /*root=*/false);
      if (relpath != null) {
         srcPath += File.separatorChar + relpath;
      }
      return convertToLocalSeparators (srcPath);
   }

   /**
    * Returns a File specified relative to the source directory for a specified
    * class. This method is a wrapper for {@link #getSrcRelativePath
    * getSrcRelativePath()}; see that method for more information about how the
    * file path is created.
    * 
    * @param classObj
    * object uses to determine the class
    * @param relpath
    * path relative to class's source directory
    * @return file specified relative to the source directory
    */
   public static File getSrcRelativeFile (Object classObj, String relpath) {
      String path = getSrcRelativePath (classObj, relpath);
      if (path == null) {
         return null;
      }
      else {
         return new File(path);
      }
   }
   
   public static URL findResource (String relpath) {
      URL[] found = findResources (relpath);
      if (found == null) {
         return null;
      }
      else {
         return found[0];
      }
   }
   
   public static File findFile (String relpath) {
      File[] found = findFiles (relpath);
      if (found == null) {
         return null;
      }
      else {
         return found[0];
      }
   }
      
   private static File[] findFiles (LinkedList<String> dirList, String relpath) {
      String pathName = convertToLocalSeparators (relpath);
      LinkedList<File> found = new LinkedList<File>();
      
      // first check if relpath is absolute
      File file = new File(pathName);
      if (file.canRead()) {
	 found.add(file);
      }
      
      for (String dir : dirList) {
         file = new File (dir, pathName);
         if (file.canRead()) {
            found.add (file);
         }
      }
      if (found.size() == 0) {
         return null;
      }
      else {
         return found.toArray (new File[0]);
      }
   }

   private static class FileMatcher implements FileFilter {

      String myPattern;

      FileMatcher (String pattern) {
         myPattern = pattern;
      }

      public boolean accept(File file) {
         return file.getName().matches (myPattern);
      }
   }

   private static File[] findFilesMatching (
      LinkedList<String> dirList, String pattern) {
      //String pathName = convertToLocalSeparators (relpath);
      LinkedList<File> found = new LinkedList<File>();
      FileMatcher matcher = new FileMatcher (pattern);
      for (String dirName : dirList) {
         File dir = new File(dirName);
         if (dir.isDirectory()) {
            for (File f : dir.listFiles (matcher)) {
               found.add (f);
            }
         }
      }
      return found.toArray (new File[0]);
   }

   /** 
    * If "." is present in this list, and there are other directories in this
    * list which happen to point to the (current working) directory, then
    * remove "." from the list. This prevents "." from overriding settings.
    */
   private static void removeDotIfPresentElsewhere (LinkedList<String> dirList) {
      boolean hasDot = false;
      for (String dir : dirList) {
         if (dir.equals (".")) {
            hasDot = true;
            break;
         }
      }
      if (hasDot) {
         String dotPath = null;
         try {
            dotPath = (new File(".")).getCanonicalPath();
         }
         catch (Exception e) {
            // assume dot isn't here if we can't get the path
            return;
         }
         boolean removeDot = false;
         for (String dir : dirList) {
            if (!dir.equals (".")) {
               try {
                  if ((new File(dir)).getCanonicalPath().equals (dotPath)) {
                     removeDot = true;
                     break;
                  }
               }
               catch (Exception e) {
               }
            }
         }
         if (removeDot) {
            Iterator<String> it = dirList.iterator();
            while (it.hasNext()) {
               String dir = it.next();
               if (dir.equals (".")) {
                  it.remove();
               }
            }
         }
      }
   }

   private static LinkedList<String> getPathDirectoryNames() {
      LinkedList<String> dirList = new LinkedList<String>();
      if (System.getenv ("ARTISYNTH_PATH") != null) {
         String path = System.getenv ("ARTISYNTH_PATH");
         for (String dir : path.split (File.pathSeparator)) {
            dirList.add (dir);
         }
      }
      else {
         dirList.add (".");
         if (System.getenv ("HOME") != null) {
            dirList.add (System.getenv ("HOME"));
         }
         if (ArtisynthPath.getHomeDir() != null) {
            dirList.add (ArtisynthPath.getHomeDir());
         }
      }
      removeDotIfPresentElsewhere (dirList);
      return dirList;
   }

   /**
    * Searches for files. Initially, we look among the directories listed
    * in the environment variable ARTISYNTH_PATH, and the first readable file
    * found is returned. If ARTISYNTH_PATH is not set, then we check the current
    * working directory, the user's home directory, and finally the Artisynth
    * home directory.
    * 
    * @param relpath
    * name of the desired file relative to the directories being searched.
    * @return an array of the files found, or null if no file was found
    */
   public static File[] findFiles (String relpath) {
      // LinkedList<String> dirList = new LinkedList<String>();
      // if (System.getenv ("ARTISYNTH_PATH") != null) {
      //    String path = System.getenv ("ARTISYNTH_PATH");
      //    for (String dir : path.split (File.pathSeparator)) {
      //       dirList.add (dir);
      //    }
      // }
      // else {
      //    dirList.add (".");
      //    if (System.getenv ("HOME") != null) {
      //       dirList.add (System.getenv ("HOME"));
      //    }
      //    if (ArtisynthPath.getHomeDir() != null) {
      //       dirList.add (ArtisynthPath.getHomeDir());
      //    }
      // }
      // removeDotIfPresentElsewhere (dirList);
      
      return findFiles (getPathDirectoryNames(), relpath);
   }

   public static File[] findFilesMatching (String pattern) {
      return findFilesMatching (getPathDirectoryNames(), pattern);
   }

   /**
    * Searches for resources. Initially, we look among the directories listed
    * in the environment variable ARTISYNTH_PATH, and the first readable file
    * found is returned. If ARTISYNTH_PATH is not set, then we check the current
    * working directory, the user's home directory, and finally the Artisynth
    * home directory.
    * 
    * @param relpath
    * name of the desired resource relative to the directories being searched.
    * @return an array of the resources found, or null if no resource was found
    */
   public static URL[] findResources (String relpath) {
      LinkedList<URL> urls = new LinkedList<URL>();

      File[] files = findFiles (relpath);
      if (files != null) {
         for (int i=0; i<files.length; i++) {
            try {
               urls.add (files[i].toURI().toURL());
            }
            catch (MalformedURLException e) {
               // just continue ...
            }
         }
      }
      if (urls.size() == 0) {
         URL url = getResource (relpath);
         if (url != null) {
            urls.add (url);
         }               
      }
      if (urls.size() == 0) {
         return null;
      }
      else {
         return urls.toArray (new URL[0]);
      }
   }

   /**
    * Gets the current working directory. By default, this is whatever directory
    * corresponds to ".".
    * 
    * @return current working directory
    */
   public static File getWorkingDir() {
      if (myWorkingDir == null) {
         myWorkingDir = new File (".");
      }
      return myWorkingDir;
   }

   /**
    * Gets the canonical path name for the working directory. If for some reason
    * this can't be determined, a warning is printed and the absolute path name
    * is returned.
    * 
    * @return canonical path name for working directory
    */
   public static String getWorkingDirPath() {
      if (myWorkingDir == null) {
         myWorkingDir = new File (".");
      }
      String path = null;
      try {
         path = myWorkingDir.getCanonicalPath();
      }
      catch (IOException e) {
         path = myWorkingDir.getAbsolutePath();
         System.err.println (
            "Warning: could obtain canonical path for working directory:\n"
            + path);
         e.printStackTrace();
      }
      return path;
   }

   /**
    * Sets the current working directory. If <code>null</code> is specified,
    * then the current working directory is set to whatever directory
    * corresponds to ".".
    * 
    * @param dir
    * new working directory
    * @throws IllegalArgumentException
    * if <i>dir</i> is not a directory
    */
   public static void setWorkingDir (File dir) {
      if (dir != null && !dir.isDirectory()) {
         throw new IllegalArgumentException ("File " + dir.getPath()
         + " is not a directory");
      }
      myWorkingDir = dir;
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

   /**
    * If {@code file} is located beneath the directory {@code dir}, return its
    * relative path with respect to {@code dir}. Otherwise, return its absolute
    * path.
    *
    * @param dir directory whose path is to be checked against {@code file}
    * @param file file whose path is sought
    * @return relative or absolute path for {@code file}
    */
   public static String getRelativeOrAbsolutePath (File dir, File file) {
      String dirPath;
      String filePath;

      try {
         dirPath = dir.getCanonicalPath();
      }
      catch (Exception e) {
         System.out.println ("Warning: cannot get canonical path for "+dir);
         dirPath = dir.getAbsolutePath();
      }
      try {
         filePath = file.getCanonicalPath();
      }
      catch (Exception e) {
         System.out.println ("Warning: cannot get canonical path for "+file);
         filePath = file.getAbsolutePath();
      }
      if (filePath.startsWith (dirPath)) {
         return filePath.substring (dirPath.length()+1);
      }
      else {
         return filePath;
      }
   }

   public static String getFileExtension (File file) {
      String name = file.getName();
      int dotIndex = name.lastIndexOf('.');
      if (dotIndex == -1) {
         return null;
      }
      else {
         return name.substring (dotIndex+1);
      }
   }

   public static boolean filesAreTheSame (File f1, File f2) {
      String path1 = null;
      String path2 = null;
      try {
         path1 = f1.getCanonicalPath();
         path2 = f2.getCanonicalPath();
      }
      catch (Exception e) {
         return false;
      }
      return path1.equals (path2);           
   }

   public static void main (String[] args) {
      // URL url = getResource (ArtisynthPath.class, "../util/./Makefile");
      // URL url = getResource (ArtisynthPath.class, "Makefile");
      URL url = getResource ("/artisynth");
      System.out.println (url);
      InputStream str = null;
      try {
         str = url.openStream();
      }
      catch (Exception e) {
         str = null;
      }
      System.out.println (str);
      System.out.println ("HOME=" + System.getenv ("HOME"));
      System.out.println (". exists=" + (new File (".")).exists());

      System.out.println (getSrcPath (ScanToken.class, true));
      System.out.println (getSrcPath (ScanToken.class, false));
   }
}
