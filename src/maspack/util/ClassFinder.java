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
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.regex.Pattern;

public class ClassFinder {

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
         System.out.println("Error loading classes");
         e.printStackTrace();
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
   public static ArrayList<Class<?>> findClass(String pkg, String className)
      throws ClassNotFoundException, IOException {

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
      Class<?> T)
      throws ClassNotFoundException, IOException {

      //ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
      ClassLoader classLoader = ClassFinder.class.getClassLoader();
      if (classLoader == null) {
         throw new InternalError ("Cannot find appropriate class loader");
      }
      String path = pkg.replace('.', '/'); // replace package structure with
                                           // folder structure

      Enumeration<URL> res = classLoader.getResources(path);
      ArrayList<File> dirs = new ArrayList<File>(); // list of contained
                                                    // directories

      // need to do some shuffling to account for paths with spaces
      while (res.hasMoreElements()) {
         URL url = res.nextElement();
         String dirName = path;
         try {
            dirName = url.toURI().getPath();
         } catch (URISyntaxException e) {
            continue;
         }
         dirs.add(new File(dirName));
      }

      ArrayList<Class<?>> classList = new ArrayList<Class<?>>();
      Pattern pattern = Pattern.compile(regex);
      for (File dir : dirs) {
         classList.addAll(findClasses(dir, pkg, pattern, T));
      }
      return classList;
   }

   public static ArrayList<Class<?>> findClasses(String pkg, Class<?> T)
      throws ClassNotFoundException, IOException {
      return findClasses(pkg, ".*", T);
   }

   public static ArrayList<Class<?>> findClasses(File dir, String pkg,
      String regex, Class<?> T) throws ClassNotFoundException {
      Pattern p = Pattern.compile(regex);
      return findClasses(dir, pkg, p, T);
   }

   /**
    * Searches through all subdirectories, gathering classes of type T that
    * match regex
    */
   public static ArrayList<Class<?>> findClasses(File dir, String pkg,
      Pattern regex, Class<?> T) throws ClassNotFoundException {
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
            if (regex.matcher(className).matches()) {

               // check if we can assign the found class to Class T
               // Must be in "try" blocks because will fail if class can't be
               // initialized
               try {
                  if (T.equals(Object.class)) { // don't bother checking if
                                                // we're dealing with Object
                     classList.add(Class.forName(className));
                  } else if (T.isAssignableFrom(Class.forName(className))) {
                     classList.add(Class.forName(className));
                  }

               } catch (Exception e) {
                  System.out.println(
                     "Class  " + className + "' could not be initialized: " +
                     e.toString() + ", " + e.getMessage());
               } catch (Error err) {
                  System.out.println(
                     "Class  " + className + "' could not be initialized: " +
                     err.toString() + ", " + err.getMessage());
               }
            }
         }
      }
      return classList;
   }

   /**
    * Searches through all subdirectories, gathering classes of type T
    */
   public static ArrayList<Class<?>> findClasses(File dir, String pkg,
      Class<?> T) throws ClassNotFoundException {
      return findClasses(dir, pkg, ".*", T);
   }

}
