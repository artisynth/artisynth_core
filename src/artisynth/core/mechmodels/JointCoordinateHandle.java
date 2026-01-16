package artisynth.core.mechmodels;

import java.io.PrintWriter;
import java.io.IOException;
import java.util.Deque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

   /**
    * No-args constructor needed for scan/write.
    */
   public JointCoordinateHandle () {
   }

   /**
    * Creates a handle for a specific coordinate.
    *
    * @param joint joint containing the coordinate
    * @param idx index of the coordinate within the joint
    */
   public JointCoordinateHandle (JointBase joint, int idx) {
      myJoint = joint;
      if (idx >= joint.numCoordinates()) {
         throw new IllegalArgumentException (
            "'idx'=" + idx + ", which is {@code >=} the number of " +
            "joint coordinates ("+joint.numCoordinates()+")");
      }
      myIdx = idx;
   }

   /**
    * Creates a copy of a handle for a specfic coordinate.
    *
    * @param ch coordinate handle to be copied
    */
   public JointCoordinateHandle (JointCoordinateHandle ch) {
      set (ch);
   }

   /**
    * Sets this coordinate handle from another.
    *
    * @param ch handle to be copied
    */
   public void set (JointCoordinateHandle ch) {
      myJoint = ch.myJoint;
      myIdx = ch.myIdx;
   }

   /**
    * Queries the joint associated with the coordinate.
    *
    * @return coordinate's jpint
    */
   public JointBase getJoint() {
      return myJoint;
   }

   /**
    * Queries the index of the coordinate with respect to its joint.
    *
    * @return coordinate index
    */
   public int getIndex() {
      return myIdx;
   }

   /**
    * Returns the name of the coordinate, or {@code null} if the coordinate
    * is unnamed.
    * 
    * @return name of the coordinate or {@code null}.
    */
   public String getName() {
      return myJoint.getCoordinateName (myIdx);
   }

   /**
    * Queries the current value of the coordinate, recomputing it if
    * necessary. This is equivalent to calling {@link
    * JointBase#getCoordinate(int) joint.getCoordinate(idx)}.
    *
    * @return coordinate value
    */
   public double getValue() {
      return myJoint.getCoordinate (myIdx);
   }

   /**
    * Queries the current value of the coordinate, recomputing it if necessary,
    * and converting the value of rotational coordinates to degrees.
    *
    * @return coordinate value (in degrees for rotational coordinates)
    */
   public double getValueDeg() {
      double value = getValue();
      if (myJoint.getCoordinateMotionType(myIdx) == MotionType.ROTARY) {
         value *= RTOD;
      }
      return value;
   }
 
   /**
    * Sets the value of the coordinate. This is equivalent to calling {@link
    * JointBase#setCoordinate(int,double) joint.setCoordinate(idx,value)}.
    *
    * @param value new coordinate value
    */
   public void setValue (double value) {
      myJoint.setCoordinate (myIdx, value);
   }

   /**
    * Sets the value of the coordinate, converting the value for rotational
    * coordinates to degrees. This is equivalent to calling {@link
    * JointBase#setCoordinateDeg(int,double)
    * joint.setCoordinateDeg(idx,value)}.
    *
    * @param value new coordinate value (in degrees for rotational coordinates)
    */
   public void setValueDeg(double value) {
      myJoint.setCoordinateDeg (myIdx, value);
   }

   /**
    * Queries the current stored value of the coordinate (with no attempt to
    * recompute it). This is equivalent to calling {@link
    * JointBase#getCoordinateValue(int) joint.getCoordinateValue(idx)}.
    *
    * @return stored coordinate value.
    */
   public double getStoredValue() {
      return myJoint.getCoordinateValue (myIdx);
   }

   /**
    * Queries the current stored value of the coordinate (with no attempt to
    * recompute it) and converts the value of rotational coordinates to
    * degrees.
    *
    * @return stored coordinate value (in degrees for rotational coordinates)
    */
   public double getStoredValueDeg() {
      return myJoint.getCoordinateValueDeg (myIdx);
   }
 
   /**
    * Queries the range for the coordinate. This is equivalent to calling
    * {@link JointBase#getCoordinateRange(int) joint.getCoordinateRange(idx)}.
    *
    * @return coordinate range
    */
   public DoubleInterval getValueRange() {
      return myJoint.getCoordinateRange(myIdx);
   }

   /**
    * Sets the range for the coordinate. This is equivalent to calling
    * {@link JointBase#setCoordinateRange(int,DoubleInterval) 
    * joint.setCoordinateRange(idx,range)}.
    *
    * @param range new coordinate range
    */
   public void setValueRange (DoubleInterval range) {
      myJoint.setCoordinateRange(myIdx, range);
   }

   /**
    * Queries the range for the coordinate, converting the values to degrees
    * for rotational coordinates. This is equivalent to calling {@link
    * JointBase#getCoordinateRangeDeg(int) joint.getCoordinateRangeDeg(idx)}.
    *
    * @return coordinate range (in degrees for rotational coordinates)
    */
   public DoubleInterval getValueRangeDeg() {
      return myJoint.getCoordinateRangeDeg(myIdx);
   }

   /**
    * Sets the range for the coordinate, converting the values from degrees
    * for rotational coordinates. This is equivalent to calling
    * {@link JointBase#setCoordinateRangeDeg(int,DoubleInterval) 
    * joint.setCoordinateRangeDeg(idx,range)}.
    *
    * @param range new coordinate range
    */
   public void setValueRangeDeg (DoubleInterval range) {
      myJoint.setCoordinateRangeDeg (myIdx, range);
   }

   /**
    * Clips the specified coordinate value to its range.
    *
    * @param value coordinate value  
    * @return value clipped to its range
    */
   public double clipToRange (double value) {
      return myJoint.getCoordinateRange(myIdx).clipToRange (value);
   }

   /**
    * Clips the specified coordinate value to its range. For rotational
    * coordinates, the value is given in degrees.
    *
    * @param value coordinate value 
    * @return value clipped to its range
    */
   public double clipToRangeDeg (double value) {
      return myJoint.getCoordinateRangeDeg(myIdx).clipToRange (value);
   }

   /**
    * Returns the amount by which the current coordinate's value exceeds its
    * range, or 0 if the coordinate is within range. For rotary joints, values
    * are returned in radians.
    *
    * @return current range violation, or 0 if within range
    */
   public double getRangeViolation() {
      DoubleInterval range = getValueRange();
      double value = getValue();
      if (value < range.getLowerBound()) {
         return value - range.getLowerBound();
      }
      else if (value > range.getUpperBound()) {
         return value - range.getUpperBound();
      }
      else {
         return 0;
      }
   }

   /**
    * Returns the amount by which the current coordinate's value exceeds its
    * range, or 0 if the coordinate is within range. For rotary joints, values
    * are returned in degrees.
    *
    * @return current range violation, or 0 if within range
    */
   public double getRangeViolationDeg() {
      DoubleInterval range = getValueRangeDeg();
      double value = getValueDeg();
      if (value < range.getLowerBound()) {
         return value - range.getLowerBound();
      }
      else if (value > range.getUpperBound()) {
         return value - range.getUpperBound();
      }
      else {
         return 0;
      }
   }

   /**
    * Queries the current speed of the coordinate. This is equivalent to
    * calling {@link JointBase#getCoordinateSpeed(int)
    * joint.getCoordinateSpeed(idx)}.
    *
    * @return coordinate's speed
    */
   public double getSpeed() {
      return myJoint.getCoordinateSpeed (myIdx);
   }

   /**
    * Queries the current speed of the coordinate, converting the value for
    * rotational coordinates to degrees/sec.
    *
    * @return coordinate's speed (in degrees/sec for rotational coordinates)
    */
   public double getSpeedDeg() {
      double speed = myJoint.getCoordinateSpeed (myIdx);
      if (myJoint.getCoordinateMotionType(myIdx) == MotionType.ROTARY) {
         speed *= RTOD;
      }
      return speed;
   }

   /**
    * Queries the limit engagement status for the coordinate.
    *
    * @return coordinate's limit engagement
    */
   public int getLimitEngagement() {
      return myJoint.getCoordinateLimitEngagement (myIdx);
   }

   /**
    * Applies a generalized force to the coordinate. This is equivalent to
    * calling {@link JointBase#applyCoordinateForce(int,double)
    * joint.applyCoordinateForce(idx,f)}.
    *
    * @param f coordinate force to apply
    */
   public void applyForce (double f) {
      myJoint.applyCoordinateForce (myIdx, f);
   }

   /**
    * Used internally to add blocks to a system solve matrix that are
    * associated with the coordinate's generalized force.
    * 
    * @param M system solve matrix
    */
   public void addSolveBlocks (SparseNumberedBlockMatrix M) {
      myJoint.addCoordinateSolveBlocks (M, myIdx);
   }
   
   /**
    * Used internally to add a term to the system solve matrix corresponding
    * to the coordinate's generalized force.
    * 
    * @param M system solve matrix
    * @param s force scale factor
    */
   public void addForceJacobian (SparseNumberedBlockMatrix M, double s) {
      myJoint.addCoordinateForceJacobian (M, myIdx, s);
   }

   /**
    * Queries the motion type for the coordinate.
    * 
    * @return coordinate motion type
    */
   public MotionType getMotionType() {
      return myJoint.getCoordinateMotionType (myIdx);
   }

   /**
    * Queries whether or not the coordinate is locked.
    * 
    * @return {@code true} if the coordinate is locked
    */  
   public boolean isLocked() {
      return myJoint.isCoordinateLocked (myIdx);
   }
   
   /**
    * Sets whether the coordinate is locked, meaning that it will hold its 
    * current value. This is equivalent to 
    * calling {@link JointBase#setCoordinateLocked(int,boolean) 
    * joint.setCoordinateLocked(idx,locked)}.
    * 
    * @param locked if {@code true}, locks the coordinate
    */
   public void setLocked (boolean locked) {
      myJoint.setCoordinateLocked (myIdx, locked);
   }

   /**
    * Queries the locked value for this coordinate. This
    * is the value that the coordinate is set at if it is locked.
    * 
    * @return coordinate's locked value.
    */
   public double getLockedValue() {
      return myJoint.getCoupling().getCoordinateLockedValue (myIdx);
   }
   
   /**
    * Sets the locked value for this coordinate. By default, the
    * locked value is set to the coordinate's current value when it is
    * locked, but in some special cases, it may be desirable to change this
    * afterwards.
    * 
    * @param value new locked value for the coordinate
    */
   public void setLockedValue (double value) {
      myJoint.getCoupling().setCoordinateLockedValue (myIdx, value);
   }
   
   /**
    * Queries the range limit compliance for this coordinate.
    * 
    * @return coordinate's range limit compliance
    */
   public double getCompliance() {
      return myJoint.getCoupling().getRangeLimitCompliance (myIdx);
   }
   
   /**
    * Sets the range limit compliance for this coordinate.
    * 
    * @param c new range limit compliance
    */
   public void setCompliance (double c) {
      myJoint.getCoupling().setRangeLimitCompliance (myIdx, c);
   }

   /**
    * Returns the wrench used to apply the generalized force
    * associated with this coordinate. The wrench is represented in the
    * joint's G frame.
    * 
    * @return coordinate force wrench, in the joints's G frame.
    */
   public Wrench getWrench() {
      return myJoint.getCoupling().getCoordinateWrench (myIdx);
   }

   /* ---- I/O methods ---- */

   public void write (PrintWriter pw, CompositeComponent ancestor)
      throws IOException {
      
      String jointPath = ComponentUtils.getWritePathName (ancestor, myJoint);
      pw.println ("[ "+myIdx+" "+jointPath+"]");
   }

   public void scan (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.scanToken ('[');
      myIdx = rtok.scanInteger();
      if (!ScanWriteUtils.scanAndStoreReference (rtok, tokens)) {
         throw new IOException ("joint component path expected, got "+rtok);
      }
      rtok.scanToken (']');
   }

   public void postscan (
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

   @Override
   public boolean equals (Object obj) {
      if (obj instanceof JointCoordinateHandle) {
         return equals ((JointCoordinateHandle)obj);
      }
      else {
         return false;
      }
   }      

   @Override
   public int hashCode () {
      return myJoint.hashCode() + 1031*myIdx;
   }


}
