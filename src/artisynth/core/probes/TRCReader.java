package artisynth.core.probes;

import java.io.*;
import java.util.*;

import artisynth.core.mechmodels.Point;
import maspack.util.*;
import maspack.matrix.*;

/**
 * Reads a TRC (Track-Row-Column) marker data file. This file format is defined
 * by the Motion Analysis Corporation and described in the appendix of the
 * manual for their EVaRT software.
 *
 * <p>Once read, marker data is exposed via a MarkerMotionData structure.
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

   /**
    * Creates a TRCReader for an specified input stream.
    *
    * @param is inout stream for reading the TRC data
    */
   public TRCReader (InputStream is) {
      myIstream = is;
   }

   /**
    * Creates a TRCReader for the indicated file.
    *
    * @param file TRC file to be read
    */
   public TRCReader (File file) throws IOException {
      this (new FileInputStream (file));
      myFile = file;
   }

   /**
    * Creates a TRCReader for the indicated file path.
    *
    * @param filePath path name of the TRC file to be read
    */
   public TRCReader (String filePath) throws IOException {
      this (new File(filePath));
   }

   public MarkerMotionData getMotionData() {
      return myMotionData;
   }

   /**
    * @deprecated Use {@link #numFrames} instead.
    */
   public int getNumFrames() {
      return numFrames();
   }
   
   /**
    * Returns the number of frames in the TRC data, or 0 if {@link #readData}
    * has not yet been called.
    *
    * @return number of frames read
    */   
   public int numFrames() {
      return myMotionData.numFrames();
   }

   /**
    * @deprecated Use {@link #numMarkers} instead.
    */
   public int getNumMarkers() {
      return numMarkers();
   }
   
   /**
    * Returns the number of markers in the TRC data, or 0 if {@link #readData}
    * has not yet been called.
    *
    * @return number of markers
    */
   public int numMarkers() {
      return myMotionData.numMarkers();
   }

   /**
    * Returns a list of the marker labels in the TRC data, or an empty list if
    * {@link #readData} has not yet been called.
    *
    * @return list of marker labels
    */
   public ArrayList<String> getMarkerLabels() {
      return myMotionData.getMarkerLabels();
   }

   /**
    * Returns the time of the {@code fidx}-th frame in the TRC data. Note that
    * {@code fidx} is 0-based, and so will be one less than the corresponding
    * frame number in the data. An exception will be thrown if if {@link
    * #readData} has not yet been called.
    *
    * @return frame time at index {@code fidx}
    */
   public double getFrameTime (int fidx) {
      return myMotionData.getFrameTime (fidx);
   }

   /**
    * Returns the position of the {@code midx}-th marker in the {@code fidx}-th
    * frame of the TRC data. Note that {@code midx} and {@code fidx} are both
    * 0-based, and so {@code fidx} will be one less than the corresponding
    * frame number in the data. An exception will be thrown if {@link
    * #readData} has not yet been called.
    *
    * @return position of marker {@code idx} in frame {@code fidx}. Should not
    * be modified.
    */
   public Point3d getMarkerPosition (int fidx, int midx) {
      return myMotionData.getMarkerPosition (fidx, midx);
   }

   /**
    * Returns the position of the named marker in the {@code fidx}-th frame of
    * the TRC data. Note that {@code fidx} is 0-based, and so will be one less
    * than the corresponding frame number in the data. {@code null} will be
    * returned if the marker is not found or {@link #readData} has not yet been
    * called.
    *
    * @return position of marker {@code idx} in frame {@code fidx}. Should not
    * be modified.
    */
   public Point3d getMarkerPosition (int fidx, String label) {
      return myMotionData.getMarkerPosition (fidx, label);
   }

   /**
    * Returns all the marker positions in the {@code fidx}-th frame of
    * the TRC data. Note that {@code fidx} is 0-based, and so will be one less
    * than the corresponding frame number in the data. An exception will be
    * thrown if {@link #readData} has not yet been called.
    *
    * @return all marker positions for frame {@code fidx}. Should not
    * be modified.
    */
   public ArrayList<Point3d> getMarkerPositions (int fidx) {
      return myMotionData.getMarkerPositions (fidx);
   }

   /**
    * Returns all the marker positions in the {@code fidx}-th frame of the TRC
    * data, loading the data into the composite vector {@code mpos}.  If
    * necessary, {@code mpos} will be resized to 3 m, where m is the number of
    * markers. Note that {@code fidx} is 0-based, and so will be one less than
    * the corresponding frame number in the data. An exception will be thrown
    * if {@link #readData} has not yet been called.
    *
    * @param mpos returns all marker positions for frame {@code fidx}. Is
    * resized if necessary.
    */
   public void getMarkerPositions (VectorNd mpos, int fidx) {
      ArrayList<Point3d> positions = getMarkerPositions(fidx);
      int m = numMarkers();
      if (mpos.size() != 3*m) {
         mpos.setSize (3*m);
      }
      for (int i=0; i<m; i++) {
         mpos.setSubVector (i*3, positions.get(i));
      }
   }

   /**
    * Returns the index of the indicated marker in the TRC data. -1 will be
    * returned if the marker is not found or {@link #readData} has not yet been
    * called.
    *
    * @param label name of the desired marker
    * @return index of the marker within the data
    */
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

   /**
    * Reads the TRC data from the file or input stream. Must be called after
    * the reader is created in order actually read in the data.
    */
   public void readData() throws IOException {
      try {
         BufferedReader reader =
            new BufferedReader (new InputStreamReader (myIstream));
         scanFileHeader (reader);
         ReaderTokenizer rtok = new ReaderTokenizer (reader);
         rtok.eolIsSignificant (true);
         int numFrames = scanDataHeader (reader);
         scanPositionData (reader, numFrames);
      }
      catch (IOException e) {
         throw e;
      }
      finally {
         if (myFile != null) {
            myIstream.close();
         }
      }
   }

   /**
    * If the reader was created with an input stream instead of a file, closes
    * the input stream. (Streams created for files are closed automatically
    * inside {@link readData}.
    */
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

   private void addProbeData (PositionInputProbe probe, int[] midxs) {
      probe.clearData();
      if (getNumFrames() > 0) {
         double t0 = getFrameTime (0);
         int npnts = midxs.length;
         for (int fidx=0; fidx<getNumFrames(); fidx++) {
            VectorNd vec = new VectorNd(3*npnts);
            for (int i=0; i<npnts; i++) {
               int midx = midxs[i];
               Vector3d pos = getMarkerPosition (fidx, midx);
               vec.setSubVector (3*i, pos);
            }
            probe.addData (getFrameTime(fidx)-t0, vec);
         }
      }
   }

   /**
    * Constructs a PositionInputProbe for the specified points based on the TRC
    * data. The argument {@code labels} gives the labels of the TRC marker data
    * that should be used for each point; if {@code labels} is {@code null},
    * then each point's name is used instead. If the specified label (or point
    * name) is not found in the TRC data, an exception is thrown.
    *
    * <p>If the first TRC frame does not have a time of 0, this initial time
    * will be subtracted from the time for the probe data (so that the probe
    * data will begin at time 0).
    * 
    * @param name if non-null, gives the name of the probe
    * @param points points to be controlled by the probe
    * @param labels if non-{@code null}, gives labels of the TRC marker data
    * that should be used for the points.
    * @param useTargetProps if {@code true}, causes the probe to
    * bind to the point's {@code targetPosition} property instead of {@code
    * position}
    * @param startTime start time of the probe
    * @param stopTime stop time of the probe
    */
   public PositionInputProbe createInputProbeUsingLabels (
      String name, Collection<? extends Point> points, List<String> labels,
      boolean useTargetProps, double startTime, double stopTime) {

      if (labels != null) {
         if (points.size() != labels.size()) {
            throw new IllegalArgumentException (
               "points and labels have different sizes: " + points.size() +
               " vs. " + labels.size());
         }
      }
      int[] midxs = new int[points.size()];
      // find the indices of the points in the data
      int i = 0;
      for (Point point : points) {
         String label;
         if (labels != null) {
            label = labels.get(i);
            if (label == null) {
               throw new IllegalArgumentException (
                  ""+i+"-th label is null");
            }
         }
         else {
            label = point.getName();
            if (label == null) {
               throw new IllegalArgumentException (
                  ""+i+"-th point is unnamed and so cannot be located");
            }
         }
         int midx = getMarkerIndex (label);
         if (midx == -1) {
            throw new IllegalArgumentException (
               "label "+label+" for "+i+"-th point not found in the TRC data");
         }
         midxs[i++] = midx;
      }
      // create the probe and add the data
      PositionInputProbe probe = new PositionInputProbe (
         name, points, /*rotRep*/null, useTargetProps, startTime, stopTime);
      addProbeData (probe, midxs);
      return probe;
   }

   /**
    * Constructs a PositionInputProbe for the specified points based on the TRC
    * data. The data for each of the {@code n} points is taken from the data
    * for the first {@code n} markers, regardless of their labels.  The number
    * of markers in the TRC data must therefore equal or exceed {@code n}.  If
    * the first TRC frame does not have a time of 0, this initial time will be
    * subtracted from the time for the probe data (so that the probe data will
    * begin at time 0).
    * 
    * @param name if non-null, gives the name of the probe
    * @param points points to be controlled by the probe
    * @param useTargetProps if {@code true}, causes the probe to
    * bind to the point's {@code targetPosition} property instead of {@code
    * position}
    * @param startTime start time of the probe
    * @param stopTime stop time of the probe
    */
   public PositionInputProbe createInputProbe (
      String name, Collection<? extends Point> points,
      boolean useTargetProps, double startTime, double stopTime) {

      if (points.size() > getNumMarkers()) {
         throw new IllegalArgumentException (
            "TRC data has only "+getNumMarkers()+" markers; "+
            "insufficient for "+points.size()+" points");
      }
      int[] midxs = new int[points.size()];
      for (int i=0; i<points.size(); i++) {
         midxs[i] = i;
      }
      // create the probe and add the data
      PositionInputProbe probe = new PositionInputProbe (
         name, points, /*rotRep*/null, useTargetProps, startTime, stopTime);
      addProbeData (probe, midxs);
      return probe;
   }

   /**
    * Constructs a PositionInputProbe for the specified points based on data in
    * the supplied TRC file. The start time is set to 0 and the stop time is
    * inferred from the TRC data. Otherwise the probe is constructed in the
    * same manner as described for {@link
    * #createInputProbeUsingLabels(String,Collection,List,boolean,double,double)}.
    *
    * @param name if non-null, gives the name of the probe
    * @param points points to be controlled by the probe
    * @param labels if non-{@code null}, gives labels of the TRC marker data
    * that should be used for the points.
    * @param trcFile TRC file providing the probe data.
    * @param useTargetProps if {@code true}, causes the probe to
    * bind to the point's {@code targetPosition} property instead of {@code
    * position}
    */
   public static PositionInputProbe createInputProbeUsingLabels (
      String name, Collection<? extends Point> points, List<String> labels,
      File trcFile, boolean useTargetProps) throws IOException {
      
      TRCReader reader = new TRCReader (trcFile);
      reader.readData();
      double duration = 0;
      int numf = reader.getNumFrames();
      if (numf > 1) {
         duration = reader.getFrameTime(numf-1) - reader.getFrameTime(0);
      }
      return reader.createInputProbeUsingLabels (
         name, points, labels, useTargetProps, /*start*/0, /*stop*/duration);
   }

   /**
    * Constructs a PositionInputProbe for the specified points based on data in
    * the supplied TRC file.  The start time is set to 0 and the stop time is
    * inferred from the TRC data. Otherwise the probe is constructed in the
    * same manner as described for {@link
    * #createInputProbe(String,Collection,boolean,double,double)}.
    * 
    * @param name if non-null, gives the name of the probe
    * @param points points to be controlled by the probe
    * @param trcFile TRC file providing the probe data.
    * @param useTargetProps if {@code true}, causes the probe to
    * bind to the point's {@code targetPosition} property instead of {@code
    * position}
    */
   public static PositionInputProbe createInputProbe (
      String name, Collection<? extends Point> points, File trcFile,
      boolean useTargetProps) throws IOException {
      
      TRCReader reader = new TRCReader (trcFile);
      reader.readData();
      double duration = 0;
      int numf = reader.getNumFrames();
      if (numf > 1) {
         duration = reader.getFrameTime(numf-1) - reader.getFrameTime(0);
      }
      return reader.createInputProbe (
         name, points, useTargetProps, /*start*/0, /*stop*/duration);
   }

}
      
