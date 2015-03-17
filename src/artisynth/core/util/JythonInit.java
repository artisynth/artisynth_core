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
import java.net.URL;

import maspack.util.*;

public class JythonInit {
   PyStringMap myBaseLocals = null;

   // private constructor ensures that we can't create an external instance of
   // this class
   private JythonInit() {
      String initFileName = "matrixBindings.py";
      InputStream bindings = null;
      try {
         URL url = ArtisynthPath.getRelativeResource (this, initFileName);
         bindings = url.openStream();
      }
      catch (IOException e) {
         throw new InternalErrorException (
            "Cannot find Jython initialization file " + initFileName);
      }
      // Don't initialize, since state should already have been initialized ...
      //PySystemState.initialize();
      PythonInterpreter interp = new PythonInterpreter();
      interp.execfile (bindings);

      myBaseLocals = (PyStringMap)interp.getLocals();
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
