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

   static void setLogger(Logger logger) {
      myLogger = logger;
   }

   private static Logger getLogger() {
      if (myLogger == null) {
         return Logger.getSystemLogger();
      }
      return myLogger;
   }

   public static ArrayList<String> findClassNames(String pkg, Class<?> base) {
      return findClassNames(pkg, ".*", base);
   }

   public static ArrayList<String> findClassNames(String pkg, String regex, Class<?> base) {
      ArrayList<String> clsNames = new ArrayList<String>();

      ArrayList<Class<?>> classList;

      try {
         classList = findClasses(pkg, regex, base);
         for (Class<?> cls : classList) {
            clsNames.add(cls.getName());
         }
      } catch (Exception e) {
         Logger logger = getLogger();
         logger.trace("Error loading classes");
         logger.trace(e);
      }
      return clsNames;
   }

   /**
    * Searches for any class with the name "className" in pkg
    * 
    * @param pkg
    * The package to search
    * @param className
    * The name of the class to find
    * @return An array of results
    */
   public static ArrayList<Class<?>> findClass(String pkg, String className) {

      String regex = "((.*\\.)|^)" + className + "$";
      ArrayList<Class<?>> results = findClasses(pkg, regex, Object.class);
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
    */
   public static ArrayList<Class<?>> findClasses(String pkg, String regex,
      Class<?> T) {

      // ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
      ClassLoader classLoader = ClassFinder.class.getClassLoader();
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
      } catch (IOException mue) {
         return new ArrayList<>();
      }

      ArrayList<File> dirs = new ArrayList<File>(); // list of contained directories
      ArrayList<URL> jars = new ArrayList<URL>();   // list of contained jar files

      // need to do some shuffling to account for paths with spaces
      while (res.hasMoreElements()) {
         URL url = res.nextElement();

         if ("file".equals(url.getProtocol())) {
            String dirName = getPathDecoded (url);

            // dirName ending in "/./" corresponds to a "." in the CLASSPATH,
            // which we want to ignore.
            if (!dirName.endsWith ("/./")) {
               dirs.add(new File(dirName));
            }
         } else if ("jar".equals(url.getProtocol())) {
            jars.add(url);
         }
      }

      ArrayList<Class<?>> classList = new ArrayList<Class<?>>();
      Pattern pattern = Pattern.compile(regex);
      for (File dir : dirs) {
         classList.addAll(findClasses(dir, pkg, pattern, T));
      }

      for (URL url : jars) {
         classList.addAll(findClasses(url, pkg, pattern, T));
      }
      return classList;
   }
   
   public static ArrayList<Class<?>> findClasses(Package pkg, Class<?> T) {
      return findClasses(pkg.getName (), ".*", T);
   }

   public static ArrayList<Class<?>> findClasses(String pkg, Class<?> T) {
      return findClasses(pkg, ".*", T);
   }

   public static ArrayList<Class<?>> findClasses(File dir, String pkg,
      String regex, Class<?> T) {
      Pattern p = Pattern.compile(regex);
      return findClasses(dir, pkg, p, T);
   }

   /**
    * Searches through all subdirectories, gathering classes of type T that
    * match regex
    */
   public static ArrayList<Class<?>> findClasses(File dir, String pkg,
      Pattern regex, Class<?> T) {
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
         if (file.isDirectory()) {
            classList.addAll(findClasses(
               file, pkg + "." + file.getName(), regex, T));
         } else if (file.getName().endsWith(".class")) {

            String className = file.getName();
            className = className.substring(0, className.length() - 6); // remove
            // extension
            className = pkg + "." + className;
            maybeAddClass(className, regex, T, classList);
         }
      }
      return classList;
   }

   private static boolean maybeAddClass(String className, Pattern regex, Class<?> base, List<Class<?>> out) {
      boolean added = false;

      if (regex.matcher(className).matches()) {

         // check if we can assign the found class to Class T
         // Must be in "try" blocks because will fail if class can't be
         // initialized
         try {
            Class<?> clz = Class.forName(className, false, ClassFinder.class.getClassLoader());

            if (base.equals(Object.class)) { // don't bother checking if we're dealing with Object
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
    * Searches through all "subdirectories" of a URL, gathering classes of type T that
    * match regex
    */
   public static ArrayList<Class<?>> findClasses(URL url, String pkg,
      Pattern regex, Class<?> T) {

      ArrayList<Class<?>> classList = new ArrayList<Class<?>>();

      // remove initial period
      if (pkg.startsWith(".")) {
         pkg = pkg.substring(1);
      }

      if ("file".equals(url.getProtocol())) {
         File file = new File(getPathDecoded(url));
         return findClasses(file, pkg, regex, T);
      } else if ("jar".equals(url.getProtocol())) {

         JarFile jar = null;
         JarEntry jarEntry = null;

         try {
            JarURLConnection connection = (JarURLConnection)(url.openConnection());
            jar = connection.getJarFile();
            jarEntry = connection.getJarEntry();
         } catch (IOException ioe) {
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
         } else {
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

}
