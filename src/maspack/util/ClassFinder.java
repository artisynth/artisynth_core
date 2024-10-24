/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC),
 * Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.util;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.HashSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

/**
 * Searches the classpath for classes that match supplied criteria
 * @author Antonio
 *
 */
public class ClassFinder {

   static Logger myLogger = null;
   static ClassLoader myClassLoader = null;
   
   /**
    * Explicitly sets the class loader to be used by this class. If
    * set to {@code null}, the current thread's context class loader will
    * be used instead.
    * 
    * @param loader new class loader
    */
   static public void setClassLoader (ClassLoader loader) {
      myClassLoader = loader;
   }

   /**
    * Returns the class loader to be used by this class. This is either
    * an explicit value set by {@link #setClassLoader}, or the context class
    * loader of the current thread.
    */
   static public ClassLoader getContextClassLoader() {
      if (myClassLoader != null) {
         return myClassLoader;
      }
      else {
         return Thread.currentThread().getContextClassLoader();
      }
   }

   /**
    * Invokes {@code Class.forName(name,initialize,loader)} using the context
    * class loader.
    */
   static public Class<?> forName (String name, boolean initialize) 
      throws ClassNotFoundException {
      return Class.forName (name, initialize, getContextClassLoader());
   }

   /**
    * Invokes {@code Class.forName(name,true,loader)} using the context class
    * loader.
    */
   static public Class<?> forName (String name) throws ClassNotFoundException {
      return Class.forName (name, true, getContextClassLoader());
   }
   
   /**
    * Class {@code loadClass(name)} for the context class loader.
    */
   static public Class<?> loadClass (String name) 
      throws ClassNotFoundException {
      return getContextClassLoader().loadClass (name);
   }

   static void setLogger(Logger logger) {
      myLogger = logger;
   }

   private static Logger getLogger() {
      if (myLogger == null) {
         return Logger.getSystemLogger();
      }
      return myLogger;
   }

   public static ArrayList<String> findClassNames (String pkg, Class<?> base) {
      return findClassNames (pkg, ".*", base, true);
   }

   public static ArrayList<String> findClassNames (
      String pkg, Class<?> base, boolean recursive) {
      return findClassNames (pkg, ".*", base, recursive);
   }

   public static ArrayList<String> findClassNames (
      String pkg, String regex, Class<?> base, boolean recursive) {
      ArrayList<String> clsNames = new ArrayList<String>();

      ArrayList<Class<?>> classList;

      try {
         classList = findClasses (pkg, regex, base, recursive);
         for (Class<?> cls : classList) {
            clsNames.add (cls.getName());
         }
      }
      catch (Exception e) {
         Logger logger = getLogger();
         logger.trace("Error loading classes");
         logger.trace(e);
      }
      return clsNames;
   }

   /**
    * Searches recursively for any class with the name "className" in pkg
    * 
    * @param pkg
    * The package to search
    * @param className
    * The name of the class to find
    * @return An array of results
    */
   public static ArrayList<Class<?>> findClass (String pkg, String className) {

      String regex = "((.*\\.)|^)" + className + "$";
      ArrayList<Class<?>> results = 
         findClasses(pkg, regex, Object.class, /*recursive=*/true);
      // all objects

      return results;
   }

   /**
    * Scans for all classes accessible from the current one within the supplied
    * package that are a child of the supplied Class T
    * 
    * @param pkg
    * string name of package
    * @param regex
    * only keep files matching the regex statement (i.e. ) i.e. regex =".*Demo$"
    * will keep only files ending with "Demo"
    * @param T
    * root class to search for
    * @param recursive
    * if {@code true}, recursively search subpackages (does not yet work for
    * JAR files)
    */
   public static ArrayList<Class<?>> findClasses(
      String pkg, String regex, Class<?> T, boolean recursive) {

      ClassLoader classLoader = getContextClassLoader();
      if (classLoader == null) {
         throw new InternalError ("Cannot find appropriate class loader");
      }
      String path = pkg.replace('.', '/'); // replace package structure with
      // folder structure
      // terminate with '/'
      if (!path.endsWith("/")) {
         path = path + "/";
      }

      Enumeration<URL> res;
      try {
         res = classLoader.getResources(path);
      }
      catch (IOException mue) {
         return new ArrayList<>();
      }
      // remove duplicated resources:
      HashSet<URL> resources = new HashSet<>();
      while (res.hasMoreElements()) {
         resources.add (res.nextElement());
      }

      // list of contained directories
      ArrayList<File> dirs = new ArrayList<File>(); 
      // list of contained jar files
      ArrayList<URL> jars = new ArrayList<URL>();   

      // need to do some shuffling to account for paths with spaces
      for (URL url : resources) {
         if ("file".equals(url.getProtocol())) {
            String dirName = getPathDecoded (url);

            // dirName ending in "/./" corresponds to a "." in the CLASSPATH,
            // which we want to ignore.
            if (!dirName.endsWith ("/./")) {
               dirs.add(new File(dirName));
            }
         }
         else if ("jar".equals(url.getProtocol())) {
            jars.add(url);
         }
      }
      ArrayList<Class<?>> classList = new ArrayList<Class<?>>();
      Pattern pattern = Pattern.compile(regex);
      for (File dir : dirs) {
         classList.addAll(findClasses(dir, pkg, pattern, T, recursive));
      }

      for (URL url : jars) {
         classList.addAll(findClasses(url, pkg, pattern, T));
      }
      return classList;
   }
   
   public static ArrayList<Class<?>> findClasses(Package pkg, Class<?> T) {
      return findClasses(pkg.getName (), ".*", T, true);
   }

   public static ArrayList<Class<?>> findClasses(String pkg, Class<?> T) {
      return findClasses(pkg, ".*", T, true);
   }

   public static ArrayList<Class<?>> findClasses (
      File dir, String pkg, String regex, Class<?> T) {
      Pattern p = Pattern.compile(regex);
      return findClasses(dir, pkg, p, T, true);
   }

   /**
    * Searches the directory 'dir', gathering classes of type T that match
    * regex.
    *
    * @param dir directory in which to search
    * @param pkg package name
    * @param regex regular expression used to filter class names
    * @param T base type that the class mush be an instance of
    * @param recursive if {@code true}, recursively search subdirectories
    */
   public static ArrayList<Class<?>> findClasses (
      File dir, String pkg, Pattern regex, Class<?> T, boolean recursive) {
      ArrayList<Class<?>> classList = new ArrayList<Class<?>>();

      if (!dir.exists()) {
         return classList;
      }
      // remove initial period
      if (pkg.startsWith(".")) {
         pkg = pkg.substring(1);
      }

      File[] files = dir.listFiles();
      for (File file : files) {
         if (recursive && file.isDirectory()) {
            classList.addAll(findClasses(
               file, pkg + "." + file.getName(), regex, T, true));
         } else if (file.getName().endsWith(".class")) {

            String className = file.getName();
            // remove extension
            className = className.substring(0, className.length() - 6); 
            className = pkg + "." + className;
            maybeAddClass(className, regex, T, classList);
         }
      }
      return classList;
   }

   private static boolean maybeAddClass (
      String className, Pattern regex, Class<?> base, List<Class<?>> out) {
      boolean added = false;

      if (regex.matcher(className).matches()) {

         // check if we can assign the found class to Class T
         // Must be in "try" blocks because will fail if class can't be
         // initialized
         try {
            Class<?> clz = forName (className, false);

            if (base.equals(Object.class)) {
               // don't bother checking if we're dealing with Object
               out.add(clz);
               added = true;
            } else {
               if (base.isAssignableFrom(clz)) {
                  out.add(clz);
                  added = true;
               }
            }

         } catch (Exception e) {
            Logger logger = getLogger();
            logger.debug(
               "Class " + className + "' could not be initialized: " +
               e.toString() + ", " + e.getMessage());
            logger.trace(e);
         } catch (Error err) {
            Logger logger = getLogger();
            logger.debug(
               "Error: Class " + className + "' could not be initialized: " +
               err.toString() + ", " + err.getMessage());
            logger.trace(err);
         }
      } // no regex match

      return added;
   }

   /**
    * Get decoded path from url
    * @param url
    * @return decoded path
    */
   private static String getPathDecoded(URL url) {
      String path;
      try {
         URI uri = url.toURI ();
         path = uri.getPath ();
      }
      catch (URISyntaxException e) {
         try {
            path = URLDecoder.decode (url.getPath (), "UTF-8");
         } catch (UnsupportedEncodingException e1) {
            path = url.getPath ();
         }
      }
      return path;
   }

   /**
    * Searches through all "subdirectories" of a URL, gathering classes of type
    * T that match regex
    */
   public static ArrayList<Class<?>> findClasses (
      URL url, String pkg, Pattern regex, Class<?> T) {

      ArrayList<Class<?>> classList = new ArrayList<Class<?>>();

      // remove initial period
      if (pkg.startsWith(".")) {
         pkg = pkg.substring(1);
      }

      if ("file".equals(url.getProtocol())) {
         File file = new File(getPathDecoded(url));
         return findClasses (file, pkg, regex, T, true);
      } 
      else if ("jar".equals(url.getProtocol())) {

         JarFile jar = null;
         JarEntry jarEntry = null;

         try {
            JarURLConnection connection = (
               JarURLConnection)(url.openConnection());
            jar = connection.getJarFile();
            jarEntry = connection.getJarEntry();
         }
         catch (IOException ioe) {
            Logger logger = getLogger();
            logger.debug("Unable to process jar: " + url.toString());
            logger.trace(ioe);
            return classList;
         }

         if (jarEntry.getName().endsWith(".class")) {
            String className = jarEntry.getName();
            // remove extension
            className = className.substring(0, className.length() - 6);
            maybeAddClass(className, regex, T, classList);
         }
         else {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
               JarEntry entry = entries.nextElement();
               if (entry.getName().startsWith(jarEntry.getName())) {
                  // we have the Jar entry
                  if (entry.getName().endsWith(".class")) {
                     String className = entry.getName();
                     // remove extension
                     className = className.substring(0, className.length() - 6);
                     className = className.replace('/', '.');
                     maybeAddClass(className, regex, T, classList);
                  }
               }
            }
         }
      }
      return classList;
   }

   /**
    * Queries whether a package exists, as indicating by the class loader being
    * able to find resources for it.
    *
    * @return true if the package is known to the class loader
    */
   public static boolean packageExists (String pkgname) {
      ClassLoader classLoader = getContextClassLoader();
      if (classLoader == null) {
         throw new InternalError ("Cannot find appropriate class loader");
      }
      // replace package structure with  folder structure
      String path = pkgname.replace('.', '/'); 
      // terminate with '/'
      if (!path.endsWith("/")) {
         path = path + "/";
      }
      return classLoader.getResource(path) != null;
   }
   
   /**
    * Find the class directory for a particular package, if it exists, This
    * will only be true if the package exists and is loaded from a file system
    * directory instead of a JAR file.
    *
    * @return package directory, or {@code null} if not found
    */
   public static File findPackageDirectory (String pkgname) {
      ClassLoader classLoader = getContextClassLoader();
      if (classLoader == null) {
         throw new InternalError ("Cannot find appropriate class loader");
      }
      // replace package structure with  folder structure
      String path = pkgname.replace('.', '/'); 
      // terminate with '/'
      if (!path.endsWith("/")) {
         path = path + "/";
      }
      URL url = classLoader.getResource(path);
      if (url != null && "file".equals(url.getProtocol())) {
         File file = new File(getPathDecoded(url));
         if (file.isDirectory()) { // just to be sure
            return file;
         }
      }
      return null;
   }
   
   public static void main (String[] args) {
      System.out.println ("package artisynth.models.fem_jaw exists=" +
         packageExists ("artisynth.models.fem_jaw"));
   }

}
