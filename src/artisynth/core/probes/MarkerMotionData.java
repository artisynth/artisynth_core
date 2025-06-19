package artisynth.core.probes;

import java.io.*;
import java.util.*;
import maspack.util.*;
import maspack.matrix.*;

/**
 * Data structure that contains motion data for a set of markers over a series
 * of frames.
 */
public class MarkerMotionData {

   protected ArrayList<String> myMarkerLabels;
   protected DynamicDoubleArray myFrameTimes;
   protected ArrayList<ArrayList<Point3d>> myMarkerData;

   /**
    * Creates an empty MarkerMotionData structure.
    */
   public MarkerMotionData() {
      myMarkerLabels = new ArrayList<>();
      myFrameTimes = new DynamicDoubleArray();
      myMarkerData = new ArrayList<>();
   }


   public void setMarkerLabels (Collection<String> labels) {
      myMarkerLabels.clear();
      for (String s : labels) {
         myMarkerLabels.add (s);
      }
      myFrameTimes.clear();
      myMarkerData.clear();
   }

   public void addFrame (double time, Collection<Vector3d> positions) {
      if (numMarkers() == 0) {
         throw new IllegalStateException ("Markers have not yet been set");
      }
      if (positions.size() != numMarkers()) {
         throw new IllegalArgumentException (
            "Number of supplied positions " + positions.size() +
            " != number of markers " + numMarkers());
      }
      myFrameTimes.add (time);
      ArrayList<Point3d> pnts = new ArrayList<>(numMarkers());
      for (Vector3d p : positions) {
         pnts.add (new Point3d(p));
      }
      myMarkerData.add (pnts);      
   }

   public ArrayList<Point3d> getMarkerPositions (int frameIdx) {
      if (frameIdx >= numFrames()) {
         throw new IllegalArgumentException (
            "Frame number "+frameIdx+" not in the range 0 to "+(numFrames()-1));
      }
      return myMarkerData.get(frameIdx);
   }

   public Point3d getMarkerPosition (int frameIdx, int mkrIdx) {
      if (mkrIdx >= numMarkers()) {
         throw new IllegalArgumentException (
            "Marker index "+mkrIdx+" not in the range 0 to "+(numMarkers()-1));
      }
      return getMarkerPositions(frameIdx).get(mkrIdx);
   }

   public Vector3d getMarkerPosition (int frameIdx, String label) {
      int mkrIdx = getMarkerIndex (label);
      if (mkrIdx == -1) {
         return null;
      }
      return getMarkerPosition (frameIdx, mkrIdx);
   }

   public double getFrameTime (int num) {
      if (num >= numFrames()) {
         throw new IllegalArgumentException (
            "Frame number "+num+" not in the range 0 to "+(numFrames()-1));
      }
      return myFrameTimes.get(num);      
   }

   public int numFrames() {
      return myFrameTimes.size();
   }
   
   public int numMarkers() {
      return myMarkerLabels.size();
   }

   public ArrayList<String> getMarkerLabels() {
      return myMarkerLabels;
   }

   public int getMarkerIndex (String label) {
      return myMarkerLabels.indexOf (label);
   }

}
