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

   // any pattern matching zero or more "[" followed by size=[ x y ], followed by zero or more "]"
   static Pattern sizeLine = Pattern.compile ("\\s*(\\[\\s*)*size=\\[\\s*[0-9]+\\s+[0-9]+\\s*\\]\\s*(\\]\\s*)*");
   static Pattern locationLine = Pattern.compile ("\\s*(\\[\\s*)*location=\\[\\s*[0-9]+\\s+[0-9]+\\s*\\]\\s*(\\]\\s*)*");
   
   // allows lines of the form "size=[ xxx yyy ]" to match regardless of the
   // numbers, because the UI can make control panel size hard to repeat
   // exactly
   private boolean isSizeLine (String line) {
      //      ReaderTokenizer rtok =
      //         new ReaderTokenizer (new StringReader (line));
      //      try {
      //         rtok.scanWord ("size");
      //         rtok.scanToken ('=');
      //         rtok.scanToken ('[');
      //         rtok.scanNumber();
      //         rtok.scanNumber();
      //         rtok.scanToken (']');
      //      }
      //      catch (Exception e) {
      //         return false;
      //      }
      //      return true; 
      return sizeLine.matcher (line).matches ();
   }
   
   // allows lines of the form "location=[ xxx yyy ]" to match regardless of the
   // numbers, because the UI can make control panel location hard to repeat
   // exactly
   private boolean isLocationLine (String line) {
      //      ReaderTokenizer rtok =
      //         new ReaderTokenizer (new StringReader (line));
      //      try {
      //         rtok.scanWord ("location");
      //         rtok.scanToken ('=');
      //         rtok.scanToken ('[');
      //         rtok.scanNumber();
      //         rtok.scanNumber();
      //         rtok.scanToken (']');
      //      }
      //      catch (Exception e) {
      //         return false;
      //      }
      //      return true;
      return locationLine.matcher (line).matches ();
   }

   private void closeQuietly(Reader reader) {
      if (reader != null) {
         try {
            reader.close();
         } catch (Exception e) {
         }
      }
   }

   private boolean linesMatch (String line0, String line1) {
      if (line0.equals(line1)) {
         return true;
      }
      else if (isSizeLine (line0) && isSizeLine (line1)) {
         return true;
      } 
      else if  (isLocationLine (line0) && isLocationLine (line1)) {
         return true;
      }
      else {
         return false;
      }
   }

   private String compareFiles (
      String saveFileName, String checkFileName, boolean showDifferingLines) 
      throws IOException {

      LineNumberReader reader0 =
         new LineNumberReader (
            new BufferedReader (new FileReader (saveFileName)));
      LineNumberReader reader1 =
         new LineNumberReader (
            new BufferedReader (new FileReader (checkFileName)));

      String line0, line1;
      while ((line0 = reader0.readLine()) != null) {
         line1 = reader1.readLine();
         if (line1 == null) {
            reader0.close();
            reader1.close();
            return (
               "check file '"+checkFileName+
               "' ends prematurely, line "+reader1.getLineNumber());
         }
         else if (!linesMatch(line0, line1)) {
            reader0.close();
            reader1.close();
            String msg =
               "save and check files '"+saveFileName+"' and '"+checkFileName+
               "' differ at line "+reader0.getLineNumber();
            if (showDifferingLines) {
               msg += ":\n"+line0+"\nvs.\n"+line1;
            }
            return msg;
         }
      }
      closeQuietly(reader0);
      closeQuietly(reader1);
      return null;
   }

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
         myMain.saveModelFile (saveFile, fmtStr);

         if (tsim > 0) {
            MechModel mech = findMechModel (rootModel);
            if (mech != null) {
               System.out.println ("running model for "+tsim+" seconds ...");
               mech.setPrintState ("%g", hsim);
               mech.openPrintStateFile (stateFile0Name);
               myMain.play (tsim);
               myMain.waitForStop();
            }
         }

         System.out.println ("loading model from "+saveFileName+" ...");
         myMain.loadModelFile (saveFile);
         rootModel = myMain.getRootModel();
         System.out.println ("saving model from "+checkFileName+" ...");
         myMain.saveModelFile (checkFile, fmtStr);

         System.out.println (
            "comparing files "+saveFileName+ " and " +checkFileName+" ...");
         String compareError = compareFiles (saveFileName, checkFileName, true);
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

   public TestCommands (Main main) {
      myMain = main;
   }

}
