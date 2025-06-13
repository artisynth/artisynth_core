package artisynth.core.mechmodels;

import java.io.PrintWriter;
import java.io.IOException;
import java.util.Deque;
import java.util.ArrayList;
import java.util.List;

import artisynth.core.util.ScanToken;
import artisynth.core.util.ObjectToken;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CompositeComponent;
import maspack.matrix.Vector3d;
import maspack.util.ReaderTokenizer;
import maspack.util.NumberFormat;

import maspack.matrix.SparseNumberedBlockMatrix;
import maspack.spatialmotion.*;
import maspack.spatialmotion.RigidBodyConstraint.MotionType;
import maspack.properties.*;
import maspack.util.*;
import artisynth.core.modelbase.*;
import artisynth.core.opensim.customjoint.OpenSimCustomJoint;

/**
 * Handle for accessing values and operations specific to a joint coordinate.
 * Its methods map onto corresponding JointBase methods involving a specific
 * coordinate index.
 */
public class JointCoordinateHandle {

   JointBase myJoint;
   int myIdx;
   
   private static final double RTOD = 180.0/Math.PI;
   private static final double DTOR = Math.PI/180.0;

   public JointCoordinateHandle () {
   }

   public JointCoordinateHandle (JointBase joint, int idx) {
      myJoint = joint;
      myIdx = idx;
   }

   public JointCoordinateHandle (JointCoordinateHandle ch) {
      set (ch);
   }

   public void set (JointCoordinateHandle ch) {
      myJoint = ch.myJoint;
      myIdx = ch.myIdx;
   }

   public JointBase getJoint() {
      return myJoint;
   }

   public int getIndex() {
      return myIdx;
   }

   public double getValue() {
      return myJoint.getCoordinate (myIdx);
   }

   public void setValue (double value) {
      myJoint.setCoordinate (myIdx, value);
   }

   public double getStoredValue() {
      return myJoint.getCoordinateValue (myIdx);
   }

   public DoubleInterval getValueRange() {
      return myJoint.getCoordinateRange(myIdx);
   }
   
   public double getValueDeg() {
      double value = getValue();
      if (myJoint.getCoordinateMotionType(myIdx) == MotionType.ROTARY) {
         value *= RTOD;
      }
      return value;
   }
 
   public double getStoredValueDeg() {
      double value = getStoredValue();
      if (myJoint.getCoordinateMotionType(myIdx) == MotionType.ROTARY) {
         value *= RTOD;
      }
      return value;
   }
 
   public void setValueDeg(double value) {
      myJoint.setCoordinateDeg (myIdx, value);
   }

   public Range getValueRangeDeg() {
      return myJoint.getCoordinateRangeDeg(myIdx);
   }

   public double getSpeed() {
      return myJoint.getCoordinateSpeed (myIdx);
   }

   public double getSpeedDeg() {
      double speed = myJoint.getCoordinateSpeed (myIdx);
      if (myJoint.getCoordinateMotionType(myIdx) == MotionType.ROTARY) {
         speed *= RTOD;
      }
      return speed;
   }

   public void applyForce (double f) {
      myJoint.applyCoordinateForce (myIdx, f);
   }

   public void addSolveBlocks (SparseNumberedBlockMatrix M) {
      myJoint.addCoordinateSolveBlocks (M, myIdx);
   }

   public void addVelJacobian (SparseNumberedBlockMatrix M, double s) {
      myJoint.addCoordinateVelJacobian (M, myIdx, s);
   }

   public MotionType getMotionType() {
      return myJoint.getCoordinateMotionType (myIdx);
   }

   public String getCoordinateName() {
      return myJoint.getCoordinateName (myIdx);
   }

   public Wrench getWrench() {
      return myJoint.getCoupling().getCoordinateWrench (myIdx);
   }

   protected void write (PrintWriter pw, CompositeComponent ancestor)
      throws IOException {
      
      String jointPath = ComponentUtils.getWritePathName (ancestor, myJoint);
      pw.println ("[ "+myIdx+" "+jointPath+"]");
   }

   protected void scan (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.scanToken ('[');
      myIdx = rtok.scanInteger();
      if (!ScanWriteUtils.scanAndStoreReference (rtok, tokens)) {
         throw new IOException ("joint component path expected, got "+rtok);
      }
      rtok.scanToken (']');
   }

   protected void postscan (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
      
      myJoint = ScanWriteUtils.postscanReference (
         tokens, JointBase.class, ancestor);
   }
   
   static void writeHandles (
      PrintWriter pw, List<JointCoordinateHandle> handles,
      CompositeComponent ancestor) throws IOException {
      
      if (handles.size() == 0) {
         pw.println ("[ ]");
      }
      else {
         pw.println ("[");
         IndentingPrintWriter.addIndentation (pw, 2);
         for (JointCoordinateHandle ch : handles) {
            pw.println (
               ch.myIdx + " " +
               ComponentUtils.getWritePathName (ancestor, ch.myJoint));
         }
         IndentingPrintWriter.addIndentation (pw, -2);
         pw.println ("]");
      }
   }

   static void writeHandles (
      PrintWriter pw, JointCoordinateHandle[] handles,
      CompositeComponent ancestor) throws IOException {
      
      if (handles.length == 0) {
         pw.println ("[ ]");
      }
      else {
         pw.println ("[");
         IndentingPrintWriter.addIndentation (pw, 2);
         for (JointCoordinateHandle ch : handles) {
            pw.println (
               ch.myIdx + " " +
               ComponentUtils.getWritePathName (ancestor, ch.myJoint));
         }
         IndentingPrintWriter.addIndentation (pw, -2);
         pw.println ("]");
      }
   }

   static void scanHandles (
      ReaderTokenizer rtok, Deque<ScanToken> tokens) throws IOException {

      DynamicIntArray indices = new DynamicIntArray();
      rtok.scanToken ('[');
      tokens.offer (ScanToken.BEGIN); // begin token
      while (rtok.nextToken() != ']') {
         if (!rtok.tokenIsInteger()) {
            throw new IOException (
               "Expected joint coordinate index, got " + rtok);
         }
         indices.add ((int)rtok.lval);
         if (!ScanWriteUtils.scanAndStoreReference (rtok, tokens)) {
            throw new IOException (
               "Expected joint path name, got " + rtok);
         }
      }
      tokens.offer (ScanToken.END); // terminator token
      tokens.offer (new ObjectToken(indices));
   }

   static ArrayList<JointCoordinateHandle> postscanHandles (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
      JointBase[] joints = ScanWriteUtils.postscanReferences (
            tokens, JointBase.class, ancestor);
      DynamicIntArray indices = (DynamicIntArray)tokens.poll().value();
      ArrayList<JointCoordinateHandle> handles = new ArrayList<>();
      int i = 0;
      for (JointBase joint : joints) {
         handles.add (new JointCoordinateHandle (joint, indices.get(i++)));
      }
      return handles;
   }

   /**
    * Checks if this coordinate handle equals another. Handles are equal if
    * they refer to the same joint and the same coordinate index.
    */
   public boolean equals (JointCoordinateHandle jch) {
      return (myJoint == jch.myJoint && myIdx == jch.myIdx);
   }

}
