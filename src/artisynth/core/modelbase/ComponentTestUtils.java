package artisynth.core.modelbase;

import java.io.*;
import java.util.regex.Pattern;

import artisynth.core.util.*;
import maspack.util.*;

public class ComponentTestUtils {

   private static void saveComponent (
      File file, ModelComponent comp, String fmtStr)
      throws IOException {
      PrintWriter pw = null;
      try {
         pw = ArtisynthIO.newIndentingPrintWriter (file);
         ScanWriteUtils.writeComponent (
            pw, new NumberFormat(fmtStr), comp, comp);
      }
      catch (IOException e) {
         throw e;
      }
      finally {
         if (pw != null) {
            pw.close();
         }
      }
   }

   public static boolean savedFilesAreEqual (
      ModelComponent comp0, ModelComponent comp1, boolean printDifference) {

      String fileName0 = "_comp0.art";
      if (comp0.getName() != null) {
         fileName0 = "_" + comp0.getName() + fileName0;
      }
      String fileName1 = "_comp1.art";
      if (comp1.getName() != null) {
         fileName1 = "_" + comp1.getName() + fileName1;
      }
      File file0 = new File(fileName0);
      File file1 = new File(fileName1);

      try {
         saveComponent (file0, comp0, "%g");
         saveComponent (file1, comp1, "%g");
         String compareError = compareArtFiles (
            fileName0, fileName1, printDifference);
         if (compareError != null) {
            if (printDifference) {
               System.out.println (compareError);
            }
            return false;
         }
         else {
            file0.delete();
            file1.delete();
            return true;
         }
      }
      catch (IOException e) {
         e.printStackTrace(); 
         return false;
      }
   }

   // any pattern matching zero or more "[" followed by size=[ x y ], followed by zero or more "]"
   static Pattern sizeLine = Pattern.compile ("\\s*(\\[\\s*)*size=\\[\\s*[0-9]+\\s+[0-9]+\\s*\\]\\s*(\\]\\s*)*");
   static Pattern locationLine = Pattern.compile ("\\s*(\\[\\s*)*location=\\[\\s*[0-9]+\\s+[0-9]+\\s*\\]\\s*(\\]\\s*)*");

   // allows lines of the form "size=[ xxx yyy ]" to match regardless of the
   // numbers, because the UI can make control panel size hard to repeat
   // exactly
   private static boolean isSizeLine (String line) {
      return sizeLine.matcher (line).matches ();
   }
   
   // allows lines of the form "location=[ xxx yyy ]" to match regardless of the
   // numbers, because the UI can make control panel location hard to repeat
   // exactly
   private static boolean isLocationLine (String line) {
      return locationLine.matcher (line).matches ();
   }

   private static void closeQuietly(Reader reader) {
      if (reader != null) {
         try {
            reader.close();
         } catch (Exception e) {
         }
      }
   }

   private static boolean linesMatch (String line0, String line1) {
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

   public static String compareArtFiles (
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
}
