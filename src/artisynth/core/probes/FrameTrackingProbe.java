package artisynth.core.probes;

import java.util.*;
import java.io.*;

import artisynth.core.mechmodels.*;
import maspack.matrix.*;
import maspack.numerics.*;

/**
 * Controls the target position of a frame based on marker data.
 */
public class FrameTrackingProbe extends NumericControlProbe {

   protected Frame myFrame;
   protected ArrayList<FrameMarker> myMarkers;
   protected RigidPoseEstimator myEstimator;

   class PoseFunction implements DataFunction {

      public void eval (VectorNd vec, double t, double trel) {
         RigidPoseEstimator estimator = getEstimator();
         RigidTransform3d TFW = new RigidTransform3d();
         estimator.estimatePose (TFW, vec);
         myFrame.setTargetPose (TFW);
      }
   }

   public FrameTrackingProbe (Frame frame, Collection<FrameMarker> mkrs) {
      if (mkrs.size() < 3) {
         throw new IllegalArgumentException (
            "Must have at least 3 marker positions");
      }
      setVsize (3*mkrs.size());
      myMarkers = new ArrayList<>();
      myMarkers.addAll (mkrs);
      myFrame = frame;
      setDataFunction (new PoseFunction());
   }

   public FrameTrackingProbe (
      Frame frame, Collection<FrameMarker> mkrs,
      double startTime, double stopTime) {

      this (frame, mkrs);
      setStartTime (startTime);
      setStopTime (stopTime);
   }

   protected RigidPoseEstimator getEstimator() {
      if (myEstimator == null) {
         // initialize estimator
         ArrayList<Vector3d> localPoints = new ArrayList<>(myMarkers.size());
         for (FrameMarker mkr : myMarkers) {
            localPoints.add (mkr.getLocation());
         }
         myEstimator = new RigidPoseEstimator (localPoints);
      }
      return myEstimator;
   }

   public void setData (String fileName) throws IOException {
      setAttachedFileName (fileName);
      load(/*setTimes=*/true);
   }

   public void setData (double[] data, double timeStep) {
      myNumericList.clear();
      addData (data, timeStep);      
   }
   
}
