/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.apps;

import java.io.*;

import argparser.ArgParser;
import argparser.StringHolder;

/**
 * Base class for mesh conversion applications.
 */
public abstract class MeshConverter {

   protected ArgParser myArgParser;
   protected StringHolder myOutputName;
   protected StringHolder myInputName;
   protected String myInSuffix;
   protected String myOutSuffix;

   public MeshConverter (String inSuffix, String outSuffix) {
  
      if (!inSuffix.startsWith (".")) {
         inSuffix = "." + inSuffix;
      }
      if (!outSuffix.startsWith (".")) {
         outSuffix = "." + outSuffix;
      }
      myInSuffix = inSuffix;
      myOutSuffix = outSuffix;
      myOutputName = new StringHolder();
      myInputName = new StringHolder();
      String synopsis = "[options] <inputFile"+inSuffix+">";
      myArgParser = new ArgParser (synopsis);
      myOutputName = new StringHolder();
      myArgParser.addOption ("-out %s #output file name", myOutputName);    
   }

   public abstract void convert (File inputFile, File outputFile)
      throws IOException;

   public void convert () throws IOException {
      convert (new File (myInputName.value), new File (myOutputName.value));
   }

   public void parseArgs (String[] args) {

      int idx = 0;
      while (idx < args.length) {
         try {
            idx = myArgParser.matchArg (args, idx);
            if (myArgParser.getUnmatchedArgument() != null) {
               String fileName = myArgParser.getUnmatchedArgument();
               if (myInputName.value == null) {
                  myInputName.value = fileName;
               }
               else {
                  System.out.println ("Ignoring extra argument "+fileName);
               }
            }
         }
         catch (Exception e) {
            // malformed or erroneous argument
            myArgParser.printErrorAndExit (e.getMessage());
         }
      } 
      if (myInputName.value == null) {
         myArgParser.printErrorAndExit ("Error: no input file specified");
      }
      if (myOutputName.value == null) {
         String outputName = (new File(myInputName.value)).getName();
         if (outputName.endsWith (myInSuffix)) {
            outputName = outputName.substring (
               0, outputName.length()-myInSuffix.length());
         }
         myOutputName.value = outputName+myOutSuffix;
      }
   }

}
