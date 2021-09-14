package artisynth.core.driver;

import java.io.*;
import java.util.*;
import java.awt.event.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

import maspack.util.*;
import maspack.widgets.*;
import maspack.properties.*;

import artisynth.core.driver.ModelScriptInfo.InfoType;
import artisynth.core.util.*;

/**
 * Describes the model that should be loaded when ArtiSynth starts up, if any.
 */
public class StartupModel implements Scannable {
   
   ModelScriptInfo myModelInfo;
   File myWaypointsFile;

   File myConfigFile; // file from which information should be saved and loaded

   public StartupModel (File configFile) {
      myConfigFile = configFile;
   }

   public File getWaypointsFile () {
      return myWaypointsFile;
   }

   public void setWaypointsFile (File file) {
      myWaypointsFile = file;
   }

   public ModelScriptInfo getModelInfo() {
      return myModelInfo;
   }

   public void setModelInfo (ModelScriptInfo info) {
      myModelInfo = info;
   }

   public File getConfigFile() {
      return myConfigFile;
   }

   public void loadOrCreate() {
      if (myConfigFile == null) {
         return;
      }
      if (myConfigFile.exists()) {
         if (myConfigFile.canRead()) {
            load();
         }
         else {
            System.out.println (
               "WARNING: startup model file "+myConfigFile+
               " is not readable");
         }
      }
      else {
         save();
      }
   }

   private boolean load() {
      return ArtisynthIO.load (
         this, myConfigFile, "startup model"); 
   }

   public boolean save() {
      if (!ArtisynthIO.save (this, myConfigFile, "startup model"
         )) {
         myConfigFile = null;
         return false;
      }
      else {
         return true;
      }
   }

   @Override
   public void scan (ReaderTokenizer rtok, Object ref) throws IOException {
      myWaypointsFile = null;
      rtok.wordChar ('.');
      rtok.scanToken ('[');
      String modelName = null;
      String[] buildArgs = null;
      String shortName = null;
      InfoType modelType = null;
      while (rtok.nextToken() != ']') {
         if (rtok.tokenIsWord ("model")) {
            rtok.scanToken ('=');
            rtok.nextToken();
            if (rtok.tokenIsWord()) {
               modelName = rtok.sval;
               modelType = InfoType.CLASS;
               shortName = RootModelManager.getLeafName (modelName);
            }
            else if (rtok.tokenIsQuotedString('"')) {
               modelName = rtok.sval;
               modelType = InfoType.FILE;
               shortName = (new File(modelName)).getName();
            }
            else {
               throw new IOException (
                  "Unexpected token scanning startup model: " + rtok);
            }
         }
         else if (rtok.tokenIsWord ("buildArgs")) {
            rtok.scanToken ('=');
            buildArgs = ModelScriptInfo.splitArgs(rtok.scanQuotedString('"'));
         }
         else if (rtok.tokenIsWord ("waypointsFile")) {
            rtok.scanToken ('=');
            myWaypointsFile = new File(rtok.scanQuotedString('"'));
         }
         else {
            throw new IOException (
               "Unexpected token scanning start configuration: " + rtok);
         }
      }
      if (modelName != null) {
         myModelInfo = new ModelScriptInfo (
            modelType, modelName, shortName, buildArgs);
      }
   }

   public void writeItems (PrintWriter pw, NumberFormat fmt, Object ref)
      throws IOException {
      if (myModelInfo != null) {
         if (myModelInfo.getType() == InfoType.FILE) {
            String fileStr = ReaderTokenizer.getQuotedString (
               myModelInfo.getClassNameOrFile(), '"');
            pw.println ("model=" + fileStr);
         }
         else {
            pw.println ("model=" + myModelInfo.getClassNameOrFile());
         }
         pw.println ("buildArgs=\"" + myModelInfo.getArgsString()+"\"");
      }
      if (myWaypointsFile != null) {
         String fileStr = ReaderTokenizer.getQuotedString (
            myWaypointsFile.toString(), '"');
         pw.println ("waypointsFile=" + fileStr);
      }
   }

   @Override
   public void write (PrintWriter pw, NumberFormat fmt, Object ref)
      throws IOException {
      IndentingPrintWriter.printOpening (pw, "[ ");
      IndentingPrintWriter.addIndentation (pw, 2);
      writeItems (pw, fmt, ref);
      IndentingPrintWriter.addIndentation (pw, -2);
      pw.println ("]");
   }

   @Override
   public boolean isWritable () {
      return true;
   }

}
