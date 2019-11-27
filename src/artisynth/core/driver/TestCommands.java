/**
 * Copyright (c) 2015, by the Authors: John E Lloyd (UBC) and ArtiSynth
 * Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.driver;

import java.io.*;
import java.util.regex.Pattern;

import maspack.matrix.*;
import maspack.util.*;

import artisynth.core.util.*;
import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.*;
import artisynth.core.workspace.RootModel;

/**
 * Implements different types of commands for doing integrated testing of
 * ArtiSynth models and components.
 */
public class TestCommands {

   Main myMain;

   private void delay (int msec) {
      try {
         Thread.sleep (1000);
      }
      catch (Exception e) {
      }
   }

   private MechModel findMechModel (RootModel rootModel) {
      ModelComponent comp = rootModel.findComponent ("models/0");
      if (comp instanceof MechModel) {
         return (MechModel)comp;
      }
      else {
         return null;
      }
   }

   public String testSaveAndLoad (
      String baseFileName, String fmtStr, double tsim, double hsim) {

      RootModel rootModel = myMain.getRootModel();
      if (rootModel == null) {
         return ("No root model loaded");
      }
      try {
         System.out.println (
            "TESTING saveAndLoad for " + rootModel.getName() + " ...");
         String saveFileName = baseFileName + ".art";
         String checkFileName = baseFileName + ".chk";
         String stateFile0Name = rootModel.getName()+"_testdata0.txt";
         String stateFile1Name = rootModel.getName()+"_testdata1.txt";

         File saveFile = new File (saveFileName);
         File checkFile = new File (checkFileName);
         
         System.out.println ("saving model to "+saveFileName+" ...");
         myMain.saveModelFile (saveFile, fmtStr, false, false);

         if (tsim > 0) {
            MechModel mech = findMechModel (rootModel);
            if (mech != null) {
               System.out.println ("running model for "+tsim+" seconds ...");
               mech.setPrintState ("%g", hsim);
               mech.openPrintStateFile (stateFile0Name);
               mech.writePrintStateHeader ("saveAndLoad");
               myMain.play (tsim);
               myMain.waitForStop();
            }
         }

         System.out.println ("loading model from "+saveFileName+" ...");
         myMain.loadModelFile (saveFile);
         rootModel = myMain.getRootModel();
         System.out.println ("saving model to "+checkFileName+" ...");
         myMain.saveModelFile (checkFile, fmtStr, false, false);

         System.out.println (
            "comparing files "+saveFileName+ " and " +checkFileName+" ...");
         String compareError = ComponentTestUtils.compareArtFiles (
            saveFileName, checkFileName, true);
         if (compareError != null) {
            System.out.println ("FAILED");
            return compareError;
         }
         else {
            saveFile.delete();
            checkFile.delete();
         }

         if (tsim > 0) {
            MechModel mech = findMechModel (rootModel);
            if (mech != null) {
               System.out.println (
                  "running restored model for "+tsim+" seconds ...");
               mech.setPrintState ("%g", hsim);
               mech.openPrintStateFile (stateFile1Name);
               mech.writePrintStateHeader ("saveAndLoad");
               myMain.play (tsim);
               myMain.waitForStop();

               CompareStateFiles comparator = new CompareStateFiles();
               System.out.println (
                  "comparing run state files "+stateFile0Name+
                  " and " +stateFile1Name+" ...");
               if (!comparator.compareFiles (
                      stateFile0Name, stateFile1Name, -1)) {
                  StringWriter sw = new StringWriter();
                  comparator.printMaxErrors(new PrintWriter(sw));
                  System.out.println ("FAILED");
                  return sw.toString();
               }
               else {
                  File stateFile0 = new File (stateFile0Name);
                  File stateFile1 = new File (stateFile1Name);
                  stateFile0.delete();
                  stateFile1.delete();
               }
            }
         }

         System.out.println ("OK");
      }
      catch (Exception e) {
         e.printStackTrace(); 
         return "IO Exception";
      }
      return null;
   }

   private static long maxAsciiStateFileSize = 0;
   private static long maxBinaryStateFileSize = 0;
   private static boolean printIOTimes = false;

   public static void testSaveLoadState (ComponentState state) {
      ComponentState testState = state.duplicate();
      FunctionTimer timer = new FunctionTimer();
      StringBuilder msg = new StringBuilder();
      if (!testState.equals (state, msg)) {
         System.out.println (msg);
         throw new TestException (
            "duplicated state does not equals state for " + state);
      }
      // test binary save/load
      String binaryFileName = "__stateTest__.dat";
      try {
         File file = new File(binaryFileName);
         DataOutputStream dos =
            new DataOutputStream (
               new BufferedOutputStream(new FileOutputStream (file)));
         timer.start();
         state.writeBinary (dos);
         dos.close();
         timer.stop();
         if (printIOTimes) {
            System.out.println (
               "writeBinary time="+timer.result(1)+", size="+file.length());
         }
         DataInputStream dis =
            new DataInputStream (
               new BufferedInputStream(new FileInputStream (file)));
         if (file.length() > maxBinaryStateFileSize) {
            maxBinaryStateFileSize = file.length();
            System.out.println (
               "maxBinaryStateFileSize=" + maxBinaryStateFileSize);
         }
         timer.start();
         testState.readBinary (dis);
         timer.stop();
         if (printIOTimes) {
            System.out.println (
               "readBinary time="+timer.result(1)+", size="+file.length());
         }         
         dis.close();
         file.delete();
      }
      catch (Exception e) {
         e.printStackTrace(); 
         throw new TestException (
            "I/O error occurred in binary save/load: " + e);
      }
      msg = new StringBuilder();
      if (!testState.equals (state, msg)) {
         System.out.println (msg);
         throw new TestException (
            "saved binary state does not equals state");
      }
      // test binary save/load
      String asciiFileName = "__stateTest__.txt";
      try {
         File file = new File(asciiFileName);
         PrintWriter pw = ArtisynthIO.newIndentingPrintWriter (file);
         timer.start();
         state.write (pw, new NumberFormat("%g"), null);
         pw.close();
         timer.stop();
         if (printIOTimes) {
            System.out.println (
               "writeAscii time="+timer.result(1)+", size="+file.length());
         }   
         ReaderTokenizer rtok = ArtisynthIO.newReaderTokenizer (file);
         if (file.length() > maxAsciiStateFileSize) {
            maxAsciiStateFileSize = file.length();
            System.out.println (
               "maxAsciiStateFileSize=" + maxAsciiStateFileSize);
         }
         timer.start();
         testState.scan (rtok, null);
         timer.stop();
         if (printIOTimes) {
            System.out.println (
               "readAscii time="+timer.result(1)+", size="+file.length());
         }   
         rtok.close();
         file.delete();
      }
      catch (Exception e) {
         throw new TestException (
            "I/O error occurred in ascii save/load: " + e);
      }
      msg = new StringBuilder();
      if (!testState.equals (state, msg)) {
         System.out.println (msg);
         throw new TestException (
            "saved ascii state does not equals state");
      }
   }

   public TestCommands (Main main) {
      myMain = main;
   }

}
