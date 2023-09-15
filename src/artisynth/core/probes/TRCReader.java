package artisynth.core.probes;

import java.io.*;
import java.util.*;
import maspack.util.*;
import maspack.matrix.*;

/**
 * Reads a TRC (Track-Row-Column) marker data file. This file format is defined
 * by the Motion Analysis Corporation and described in the appendix of the
 * manual for their EVaRT software.
 */
public class TRCReader {
   
   protected InputStream myIstream;
   protected File myFile;

   protected String myFileTypeLabel;
   protected int myFileTypeNumber;
   protected String myFileTypeDescriptor;
   protected String myFilePath;

   protected double myDataRate;
   protected double myCameraRate;
   protected String myUnits;
   protected double myOrigDataRate;
   protected int myOrigDataStartFrame;
   protected int myOrigNumFrames;

   protected MarkerMotionData myMotionData = new MarkerMotionData();

   public MarkerMotionData getMotionData() {
      return myMotionData;
   }

   public int getNumFrames() {
      return myMotionData.numFrames();
   }
   
   public int getNumMarkers() {
      return myMotionData.numMarkers();
   }

   public ArrayList<String> getMarkerLabels() {
      return myMotionData.getMarkerLabels();
   }

   public double getFrameTime (int num) {
      return myMotionData.getFrameTime (num);
   }

   public Vector3d getMarkerPosition (int num, int mkr) {
      return myMotionData.getMarkerPosition (num, mkr);
   }

   public Vector3d getMarkerPosition (int num, String label) {
      return myMotionData.getMarkerPosition (num, label);
   }

   public int getMarkerIndex (String label) {
      return myMotionData.getMarkerIndex (label);
   }
   
   private void closeQuietly(InputStream in) {
      if (in != null) {
         try {
            in.close();
         } catch (IOException e) {}
      }
   }

   public TRCReader (InputStream is) {
      myIstream = is;
   }

   public TRCReader (File file) throws IOException {
      this (new FileInputStream (file));
      myFile = file;
   }

   protected void scanFileHeader (BufferedReader reader) throws IOException {
      String line = reader.readLine();
      String[] toks = line.split ("\t");
      if (toks.length < 2) {
         throw new IOException (
"Line 1: expecting a tab-delimited file header with at least 2 entries");
      }
      myFileTypeLabel = toks[0];
      myFileTypeNumber = scanInt (toks[1], 1, 2);
      if (toks.length >= 3) {
         myFileTypeDescriptor = toks[2];
      }
      if (toks.length >= 4) {
         myFilePath = toks[3].trim();
      }
   }

   protected int scanInt (String str, int line, int entry) throws IOException {
      int value;
      try {
         value = Integer.parseInt(str);
      }
      catch (NumberFormatException e) {
         throw new IOException (
            "Line "+line+", entry "+entry+
            ": integer value expected, got '"+str+"'");
      }
      return value;
   }

   protected double scanDouble (String str, int line, int entry)
      throws IOException {
      double value;
      try {
         value = Double.parseDouble(str);
      }
      catch (NumberFormatException e) {
         throw new IOException (
            "Line "+line+", entry "+entry+
            ": double value expected, got '"+str+"'");
      }
      return value;
   }

   protected int scanDataHeader (BufferedReader reader) throws IOException {
      String line = reader.readLine();
      String[] labels = line.split ("\t");
      line = reader.readLine();
      String[] values = line.split ("\t");
      if (labels.length != values.length) {
         throw new IOException (
            "Number of labels on line 2 differs from number of values on line 3");
      }
      int numMarkers = 0;
      int numFrames = 0;
      for (int i=0; i<labels.length; i++) {
         String label = labels[i].trim();
         if (label.equalsIgnoreCase ("DataRate")) {
            myDataRate = scanDouble (values[i], 3, i+1);
         }
         else if (label.equalsIgnoreCase ("CameraRate")) {
            myCameraRate = scanDouble (values[i], 3, i+1);
         }
         else if (label.equalsIgnoreCase ("NumFrames")) {
            numFrames = scanInt (values[i], 3, i+1);
         }
         else if (label.equalsIgnoreCase ("NumMarkers")) {
            numMarkers = scanInt (values[i], 3, i+1);
         }
         else if (label.equalsIgnoreCase ("Units")) {
            myUnits = values[i].trim();
         }
         else if (label.equalsIgnoreCase ("OrigDataRate")) {
            myOrigDataRate = scanDouble (values[i], 3, i+1);
         }
         else if (label.equalsIgnoreCase ("OrigDataStartFrame")) {
            myOrigDataStartFrame = scanInt (values[i], 3, i+1);
         }
         else if (label.equalsIgnoreCase ("OrigNumFrames")) {
            myOrigNumFrames = scanInt (values[i], 3, i+1);
         }
         else {
            System.out.println (
               "WARNING, TRCReader: unrecognized data label '"+label+
               "', ignoring");
         }              
      }
      line = reader.readLine();
      labels = line.split ("\t");
      // ignore first two labels
      ArrayList<String> mkrLabels = new ArrayList<>();
      for (int i=2; i<labels.length; i += 3) {
         String label = labels[i].trim();
         if (label.length() == 0) {
            throw new IOException (
               "Line 4: label for marker"+(i+1)+" is empty");
         }
         mkrLabels.add (label);
      }
      if (mkrLabels.size() != numMarkers) {
         System.out.println (
            "WARNING, TRCReader: num marker labels ("+mkrLabels.size()+
            ") != specified number of markers ("+numMarkers+"), assuming "+
            mkrLabels.size());
      }
      myMotionData.setMarkerLabels (mkrLabels);
      line = reader.readLine(); // read and ignore line 5
      return numFrames;
   }

   protected void scanPositionData (
      BufferedReader reader, int numFrames) throws IOException {
      String line;

      int numMarkers = myMotionData.numMarkers();
      int lineno = 6;
      while ((line=reader.readLine()) != null) {
         line = line.trim();
         if (line.length() != 0) {
            String[] fields = line.split("\t");
            ArrayList<Vector3d> positions = new ArrayList<>();
            if (fields.length != 2+numMarkers*3) {
               throw new IOException (
                  "Line "+lineno+": detected "+fields.length+
                  " fields; expected "+(2+numMarkers*3));
            }
            double time = scanDouble (fields[1], lineno, 2);
            int k = 2; // field index, advances by 3 per position
            for (int j=0; j<numMarkers; j++) {
               Vector3d pos = new Vector3d();
               pos.x = scanDouble (fields[k++], lineno, k);
               pos.y = scanDouble (fields[k++], lineno, k);
               pos.z = scanDouble (fields[k++], lineno, k);
               positions.add (pos);
            }
            myMotionData.addFrame (time, positions);
         }
         lineno++;
      }
      if (myMotionData.numFrames() != numFrames) {
         System.out.println (
            "WARNING, TRCReader: reader "+myMotionData.numFrames() +
            " frames; expected "+numFrames);
      }
   }

   public void readData() throws IOException {
      BufferedReader reader =
         new BufferedReader (new InputStreamReader (myIstream));
      scanFileHeader (reader);
      ReaderTokenizer rtok = new ReaderTokenizer (reader);
      rtok.eolIsSignificant (true);
      int numFrames = scanDataHeader (reader);
      scanPositionData (reader, numFrames);
   }

   public void close() {
      closeQuietly(myIstream);
   }
   
   @Override
   protected void finalize() throws Throwable {
      super.finalize();
      close();
   }

   public static void main (String[] args) {
      String fileName = "test.trc";
      if (args.length == 1) {
         fileName = args[0];
      }
      else if (args.length != 0) {
         System.out.println ("Usage: TRCReader [<fileName>]");
      }
      try {
         TRCReader reader = new TRCReader (new File (fileName));
         reader.readData();
         System.out.println ("read " + reader.getNumFrames() + " frames");
         System.out.println ("marker labels:");
         for (String s : reader.getMarkerLabels()) {
            System.out.println (" " + s);
         }
      }
      catch (Exception e) {
         e.printStackTrace(); 
      }
   }
  
}
