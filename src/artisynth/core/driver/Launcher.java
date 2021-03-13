/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.driver;

import java.util.*;
import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import artisynth.core.util.ArtisynthPath;
import artisynth.core.util.ArtisynthIO;
import maspack.util.ReaderTokenizer;
import maspack.fileutil.NativeLibraryManager.SystemType;
import maspack.fileutil.*;
 
/**
 * Class to launch an Artisynth application. We use a launcher so that we can
 * set-up our own class loader that references all the needed .jar files, as
 * well as external class path entries (such as from other projects) as
 * described in the file $ARTISYNTH_HOME/EXTCLASSPATH.
 */
public class Launcher {

   public static String DEFAULT_MAIN_CLASS_NAME = "artisynth.core.driver.Main";
   private static String myRemoteHost = // where to try and get missing files
      "http://www.artisynth.org/files/lib/";

   public static boolean useJOGL2 = true;
   private String myMainClassName;

   public Launcher (String mainClassName) {
      myMainClassName = mainClassName;
   }

   public Launcher () {
      this (DEFAULT_MAIN_CLASS_NAME);
   }
   
   public Object launch (String[] args) {

      boolean updateLibs = false;
      String classpath = null;
      ArrayList<String> mainArgs = new ArrayList<>();
      if (args != null) {
         for (int i=0; i<args.length; i++) {
            if (args[i].equals ("-updateLibs")) {
               updateLibs = true;
            }
            else if (args[i].equals ("-cp") || args[i].equals ("-classpath")) {
               if (i == args.length-1) {
                  System.err.println (
                     "Error: option '"+args[i]+"' needs an additional argument");
               }
               classpath = args[++i];
            }
            else {
               mainArgs.add (args[i]);
            }
         }
      }
      // arguments to pass to ArtiSynth Main class
      args = mainArgs.toArray (new String[0]);
      verifyLibraries (updateLibs);

      ClassLoader loader =
         new URLClassLoader (getClasspathURLs(classpath), null);

      // ensure that other classes in this thread use this class loader ...
      Thread.currentThread().setContextClassLoader(loader);

      try {
         Class<?> mainClass = loader.loadClass(myMainClassName);
         Method main = mainClass.getMethod("main", new Class[] {
               String[].class
            });
         int modifiers = main.getModifiers();
         if (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers)) {
            main.invoke(null, (Object)args);
         } else {
            throw new NoSuchMethodException();
         }
         Method getMain = mainClass.getMethod("getMain");
         modifiers = getMain.getModifiers();
         Object mainObj = null;
         if (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers)) {
            mainObj = getMain.invoke(null);
         } else {
            throw new NoSuchMethodException();
         }
         return mainObj;
      } catch (Exception e) {
         System.err.println("Error running class " + myMainClassName);
         e.printStackTrace();
         return null;
      }
   }
 
   public static void main(String[] args) {
      Launcher launcher = new Launcher();
      launcher.launch(args);
   }

   public static Object dolaunch (String[] args) {
      Launcher launcher = new Launcher();
      return launcher.launch(args);
   }

   /**
    * Add a jar or class directory file to the file list if it is not already
    * present. Presence is checked by seeing if the file exists in
    * <code>fileSet</code>, which is HashSet of canonical (and hence unique)
    * paths for all files in the list.
    */    
   private void addFileIfNeeded (
      LinkedList<File> files, HashSet<File> fileSet, File file) {
      try {
         File canonicalFile = file.getCanonicalFile();
         if (!fileSet.contains (canonicalFile)) {
            files.add (file);
            fileSet.add (canonicalFile);
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   /**
    * Add a jar or class directory file (specified by a path) to the file list
    * if it is not already present. Presence is checked by seeing if the file
    * exists in <code>fileSet</code>, which is HashSet of canonical (and hence
    * unique) paths for all files in the list.
    */    
   private void addFileIfNeeded (
      LinkedList<File> files, HashSet<File> fileSet, String path) {
      addFileIfNeeded (files, fileSet, new File(path));
   }

   /**
    * Returns a list of all the jar files in a specified directory.
    */
   private File[] getJarFiles (String dirpath) {
      File dir = new File (dirpath);
      if (dir.isDirectory()) {
         return dir.listFiles (
            new FilenameFilter () {
               public boolean accept (File dir, String name) {
                  return name.endsWith (".jar");
               }
            });
      }
      else {
         return new File[0];
      }
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

   /**
    * Performs environment variable substitution on a string. Variables are
    * expected to begin with '$'. If no value is found for a variable, no
    * substitution occurs and the variable is simply removed from the string.
    */   
   private static String subEnvironmentVariables (String str) {
      if (str.indexOf ("$") == -1) {
         return str;
      }
      else {
         StringBuilder builder = new StringBuilder();
         int varStartIdx = -1;
         for (int i=0; i<str.length(); i++) {
            char c = str.charAt(i);
            if (varStartIdx != -1) {
               // parsing a variable
               if ((i == varStartIdx+1 && !isEnvVariableStart(c)) ||
                   (i > varStartIdx+1 && !isEnvVariablePart(c))) {
                  // parsing concludes
                  if (i > varStartIdx+1) {
                     String var = str.substring (varStartIdx+1, i);
                     String value = System.getenv (var);
                     if (value != null) {
                        builder.append (value);
                     }
                  }
                  varStartIdx = -1;               
               }
            }
            if (varStartIdx == -1) {
               if (c == '$') {
                  varStartIdx = i;
               }
               else {
                  builder.append (c);
               }
            }
         }
         return builder.toString();
      }
   }

   private static String SEP = File.separator;
   private static String PSEP = File.pathSeparator;


   private static String[] readAuxFilesFromLine (BufferedReader reader)
      throws IOException {
      
      String line = reader.readLine();
      if (line == null) {
         return null;
      }
      // clip out comments
      int ci = line.indexOf ('#');
      if (ci != -1) {
         line = line.substring (0, ci);
      }
      // make sure line contains more than whitespace ...
      for (int i=0; i<line.length(); i++) {
         if (!Character.isWhitespace (line.charAt(i))) {
            return line.split (File.pathSeparator);
         }
      }
      // otherwise, return 0 items ...
      return new String[0];
   }      

   /**
    * Returns a list of external jar files and class directories as specified
    * in the file $ARTISYNTH_HOME/EXTCLASSPATH.
    */
   private static File[] getExtFiles (String homeDirPath) {
      LinkedList<File> pfiles = new LinkedList<File>();
      File file = new File (homeDirPath+SEP+"EXTCLASSPATH");
      if (file.canRead()) {
         BufferedReader reader = null;
         try {
            reader = new BufferedReader (new FileReader (file));
            String[] auxFiles;
            while ((auxFiles=readAuxFilesFromLine (reader)) != null) {
               for (String auxFile : auxFiles) {
                  File pfile = new File (subEnvironmentVariables(auxFile));
                  if (pfile.canRead()) {
                     pfiles.add (pfile);
                  }                  
               }
            }
         }
         catch (Exception ignore) {
         }
         finally {
            if (reader != null) {
               try { reader.close();
               }
               catch (Exception e) {
                  // ignore
               }
            }
         }
      }
      return pfiles.toArray (new File[0]);      
   }

   /**
    * Returns a list of the path names of the external jar files and class
    * directories as specified in the file $ARTISYNTH_HOME/EXTCLASSPATH.
    */
   public static String[] getExtFilePathNames (String homeDirPath) {
      File[] files = getExtFiles (homeDirPath);
      String[] pathNames = new String[files.length];
      for (int i=0; i<pathNames.length; i++) {
         pathNames[i] = files[i].toString();
      }
      return pathNames;
   }

   /**
    * Verifies or updates jar files and some native libraries, based on the
    * contents of the file $ARTISYNTH_HOME/lib/LIBRARIES.
    */
   public static void verifyLibraries (boolean update) {

      LibraryInstaller installer = new LibraryInstaller();
      File libFile = ArtisynthPath.getHomeRelativeFile ("lib/LIBRARIES", ".");
      if (!libFile.canRead()) {
         System.out.println ("Warning: can't access "+libFile);
         libFile = null;
      }
      else {
         try {
            installer.readLibs (libFile);
         }
         catch (Exception e) {
            System.out.println (e.getMessage());
            libFile = null;
         }
      }
      if (libFile != null) {
         installer.clearNativeLibs();
         boolean allOK = true;
         try {
            allOK &= installer.verifyJars (update);
            allOK &= installer.verifyNativeLibs (update);
         }
         catch (Exception e) {
            if (installer.isConnectionException (e)) {
               System.out.println (e.getMessage());
            }
            else {
               e.printStackTrace(); 
            }
            System.exit(1);
         }
         if (!allOK) {
            System.out.println (
               "Error: can't find or install all required libraries");
            System.exit(1); 
         }
      }
   }   

   /**
    * Obtains all the jar file and class directory entries corresponding to the
    * specified classpath, or the current CLASSPATH setting, if classpath is
    * null.
    */
   private void addClassPathFiles (LinkedList<File> files, String classpath) {
      if (classpath == null) {
         classpath = System.getProperty ("java.class.path");
      }
      if (classpath != null) {
         String[] entries = classpath.split (PSEP);
         for (int i=0; i<entries.length; i++) {
            String entry = entries[i];
            if (entry.endsWith (SEP+"*")) {
               File[] jarFiles =
                  getJarFiles(entry.substring(0,entry.length()-2));
               for (File file : jarFiles) {
                  files.add (file);
               }
            }
            else if (entry.endsWith (".jar")) {
               files.add (new File (entry));
            }
            else if (entry.length() > 0) { // avoid null string
               files.add (new File (entry));
            }
         }
      }
   }

   /**
    * Obtains URLs for all the jar files and class directories that should be
    * used by the class loader.
    */
   private URL[] getClasspathURLs (String classpath) {
      LinkedList<File> files = new LinkedList<File>();
      LinkedHashSet<File> fileSet = new LinkedHashSet<File>();

      String ahome = ArtisynthPath.getHomeDir();

      addClassPathFiles (files, classpath);

      try {
         for (File file : files) {
            fileSet.add (file.getCanonicalFile());
         }
      }
      catch (Exception e) {
         // ignore files that can't be converted to canonical form
      }
      addFileIfNeeded (files, fileSet, ahome+SEP+"classes");

      File[] jarFiles = getJarFiles (ahome+SEP+"lib");
      for (File file : jarFiles) {
         addFileIfNeeded (files, fileSet, file);
      }

      File[] auxFiles = getExtFiles (ahome);
      for (File file : auxFiles) {
         addFileIfNeeded (files, fileSet, file);
      }

      LinkedList<URL> urls = new LinkedList<URL>();
      for (File file : files) {
         try {
            urls.add (file.toURI().toURL());
         }
         catch (Exception e) {
            System.out.println ("Warning: cannot create URL for "+file);
            // ignore
         }
      }
      return urls.toArray (new URL[0]);
   }

   private static String libPathName = "java.library.path";

   /**
    * Try adding a load library directory to the java.library.path property if
    * it is not already present. This is a very system-specific hack: if
    * java.library.path is changed, the class loader needs to be reset so that
    * it will re-read the java.library.path when the next load call is made.
    * Also, if libraries in the native directory refer to other libraries in
    * that directory, then loading may still fail, because subsequent loads are
    * done by the OS which uses the system load library path settings directly.
    */
   private boolean addLibraryPathIfNecessary (String lldir) {

      String dirName = null;
      try {
         dirName = (new File(lldir)).getCanonicalPath();
      }
      catch (Exception e) {
         return false;
      }
      
      String ldpath = System.getProperty(libPathName, "");
   
      String[] dirs = ldpath.split (PSEP);
      for (String dir : dirs) {
         if (dir.length() > 0) {
            String canonicalName = null;
            try {
               canonicalName = (new File(dir)).getCanonicalPath();
            }
            catch (Exception e) {
               // ignore
            }
            if (canonicalName != null && canonicalName.equals (dirName)) {
               return false;
            }
         }
      }
      // library path not found, so add it
      System.setProperty (libPathName, lldir+PSEP+ldpath);

      // Now reset the "sys_paths" variable in the ClassLoader to cause it to
      // (on demand) reinitialize the load library path from the libPathName
      // property. This was described at
      //
      // blog.cedarsoft.com/2010/11/setting-java-library-path-programmatically
      //
      try {
         Field fieldSysPath = ClassLoader.class.getDeclaredField( "sys_paths" );
         if (fieldSysPath != null) {
            fieldSysPath.setAccessible( true );
            fieldSysPath.set( null, null );
            return true;
         }
      }
      catch (Exception e) {
         // ignore
      }
      return false;
   }

}

