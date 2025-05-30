/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.util;

import org.python.util.*;
import org.python.core.*;
import org.python.core.packagecache.*;
import java.io.*;
import java.util.Properties;
import java.net.URL;

import maspack.util.*;

public class JythonInit {
   PyStringMap myBaseLocals = null;
   
   public static Properties createDefaultProperties () {
      // Jython 2.7: turn off site import since required files unavailable
      Options.importSite = false;
      Properties props = new Properties();
      File jythonCacheDir = new File(ArtisynthPath.getCacheDir(), "jython");
      props.setProperty (
         RegistryKey.PYTHON_CACHEDIR, jythonCacheDir.toString());
      props.setProperty (RegistryKey.PYTHON_IO_ENCODING, "utf-8");
      return props;
   }
   
   // private constructor ensures that we can't create an external instance of
   // this class
   private JythonInit() {
      String initFileName = "matrixBindings.py";
      Properties props = createDefaultProperties();
      PySystemState.initialize(null, props);
      InputStream bindings = null;
      try {
         URL url = ArtisynthPath.getRelativeResource (this, initFileName);
         bindings = url.openStream();
         // Don't initialize, since state should already have been initialized ...
         //PySystemState.initialize();
         PythonInterpreter interp = new PythonInterpreter();
         interp.execfile (bindings);
         myBaseLocals = (PyStringMap)interp.getLocals();
      }
      catch (IOException e) {
         throw new InternalErrorException (
            "Cannot find Jython initialization file " + initFileName);
      }
      catch (Exception e) {
         throw new InternalErrorException (
            "Cannot initialize Jython bindings:\n" + e.getMessage());
      }
      finally {
         try {
            bindings.close();
         }
         catch (Exception e) {
            // ignore
         }
      }
   }

   private static boolean myInitDone = false;
   private static JythonInit myInit = null;

   /**
    * Returns true if Jython is available on this system and false otherwise.
    * 
    * @return true if Jython is available
    */
   public static boolean jythonIsAvailable() {
      // assume it is available since we provide the jar file with the distro
      return true;
      //return System.getenv ("JYTHON_HOME") != null;
   }

   private static void initialize() {
      try {
         myInit = new JythonInit();
      }
      catch (Exception e) {
         System.out.println ("Error: " + e);
         e.printStackTrace();
      }
      myInitDone = true;
   }

   public static void init() {
      if (!myInitDone) {
         initialize();
      }
   }

   /**
    * Returns a copy of the base set of local symbols used by ArtiSynth jython,
    * including an import of maspack.matrix and the definitions needed for
    * matrix operator overloading.
    */
   public static PyStringMap getArtisynthLocals() {
      if (!myInitDone) {
         initialize();
      }
      if (myInit != null) {
         return myInit.myBaseLocals;
      }
      else {
         return null;
      }
   }

}
