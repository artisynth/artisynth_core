package artisynth.core.probes;

import java.util.*;
import java.io.*;

import artisynth.core.mechmodels.*;
import artisynth.core.modelbase.*;

import maspack.matrix.*;
import maspack.geometry.*;
import maspack.util.*;
import maspack.properties.*;
import maspack.interpolation.*;

/**
 * Probe for specifying the positions and poses of a set of Point, Frame, and
 * FixedMeshBody components. Translational position information is attached to
 * either the component's {@code position} or {@code targetPosition} property,
 * depending on availability and the value of the {@code useTargetProps} argument
 * in the probe's constructor. For components with orientation (Frame and
 * FixedMeshBody), the rotation information is likewise attached to the either
 * the components's {@code orientation} or {@code targetOrientation}
 * property. However, within the probe data, the orientation is represented in
 * a manner specifed by a {@link RotationRep} object, using either 3 or 4
 * numbers. Rotation information is interpolated as curves on the surface of
 * SO(3), as originally described by Ken Shoemake in his SIGGRAPH 1985 paper
 * "Animating Rotation with Quaternion Curves".
 */
public class PositionInputProbe extends NumericInputProbe {

   HashMap<ModelComponent,Integer> myCompOffsetMap = null;

   private void updateCompOffsetMap() {
      if (myCompOffsetMap == null) {
         myCompOffsetMap = getPositionCompOffsetMap ();
      }
   }

   /**
    * No-args constructor needed for scanning.
    */
   public PositionInputProbe () {
   }

   /**
    * Constructs a PositionInputProbe for a single Point, Frame, or FixedMeshBody,
    * as specified by {@code comp}. Data must be added after the constructor is
    * called.
    *
    * @param name if non-null, gives the name of the probe
    * @param comp specifies the Point, Frame, or FixedMeshBody
    * @param rotRep rotation representation. Can be null if {@code comp}
    * is a Point.
    * @param useTargetProps if {@code true}, causes the probe to bind to the
    * component's {@code targetPosition} and {@code targetOrientation}
    * properties (if available), instead of {@code position} and {@code
    * orientation}
    * @param startTime start time of the probe
    * @param stopTime stop time of the probe
    */
   public PositionInputProbe (
      String name, ModelComponent comp, RotationRep rotRep,
      boolean useTargetProps, double startTime, double stopTime) {
      setModelFromComponent (comp);
      setInputProperties (
         findPositionPropsAndOffsets(
            new ModelComponent[] { comp }, rotRep, useTargetProps));
      initFromStartStopTime (name, startTime, stopTime);
   }

   /**
    * Constructs a PositionInputProbe for a list of Point, Frame, or FixedMeshBody
    * components specified by {@code comps}. Data must be added after the
    * constructor is called.
    * 
    * @param name if non-null, gives the name of the probe
    * @param comps specifies the Point, Frame, or FixedMeshBody components
    * @param rotRep rotation representation. Can be null if all of the components
    * are points.
    * @param useTargetProps if {@code true}, causes the probe to bind to the
    * component's {@code targetPosition} and {@code targetOrientation}
    * properties (if available), instead of {@code position} and {@code
    * orientation}
    * @param startTime start time of the probe
    * @param stopTime stop time of the probe
    */
   public PositionInputProbe (
      String name, Collection<? extends ModelComponent> comps, 
      RotationRep rotRep, boolean useTargetProps, 
      double startTime, double stopTime) {
      ModelComponent[] carray =
         comps.toArray(new ModelComponent[0]);
      setModelFromComponent (carray[0]);
      setInputProperties (
         findPositionPropsAndOffsets (carray, rotRep, useTargetProps));
      initFromStartStopTime (name, startTime, stopTime);
   }
   
   /**
    * Constructs a PositionInputProbe for a single Point, Frame, or FixedMeshBody,
    * as specified by {@code comp}. Probe data, plus its interpolation method
    * and start and stop times, are specified in a probe file.
    * 
    * @param name if non-null, gives the name of the probe
    * @param comp specifies the Point, Frame, or FixedMeshBody
    * @param rotRep rotation representation. Can be null if {@code comp} is a
    * Point.
    * @param useTargetProps if {@code true}, causes the probe to bind to the
    * component's {@code targetPosition} and {@code targetOrientation}
    * properties (if available), instead of {@code position} and {@code
    * orientation}
    * @param filePath path name of the probe data file
    * @throws IOException if a file I/O error occurs
    */
   public PositionInputProbe (
      String name, ModelComponent comp, RotationRep rotRep,
      boolean useTargetProps, String filePath) throws IOException {
      setModelFromComponent (comp);
      setInputProperties (
         findPositionPropsAndOffsets(
            new ModelComponent[] { comp }, rotRep, useTargetProps));
      initFromFile (name, filePath);
   }

   /**
    * Constructs a PositionInputProbe for a list of Point, Frame, or FixedMeshBody
    * components specified by {@code comps}. Probe data, plus its interpolation
    * method and start and stop times, are specified in a probe file.
    * 
    * @param name if non-null, gives the name of the probe
    * @param comps specifies the Point, Frame, or FixedMeshBody components
    * @param rotRep rotation representation. Can be null if all of the
    * components are points.
    * @param useTargetProps if {@code true}, causes the probe to bind to the
    * component's {@code targetPosition} and {@code targetOrientation}
    * properties (if available), instead of {@code position} and {@code
    * orientation}
    * @param filePath path name of the probe data file
    * @throws IOException if a file I/O error occurs
    */
   public PositionInputProbe (
      String name, Collection<? extends ModelComponent> comps, 
      RotationRep rotRep, boolean useTargetProps, String filePath) 
      throws IOException {
      ModelComponent[] carray =
         comps.toArray(new ModelComponent[0]);
      setModelFromComponent (carray[0]);
      setInputProperties (
         findPositionPropsAndOffsets (carray, rotRep, useTargetProps));
      initFromFile (name, filePath);
   }

   /**
    * Apply a rigid or affine transform to all the point and rotation data in
    * this list.
    *
    * @param X transform to apply
    */
   public void transformData (AffineTransform3dBase X) {
      getNumericList().transformPositionData (X);
   }

   /**
    * Returns a list of the position components associated with this probe, in
    * the order that they appear in the probe's data.
    *
    * @return list of components associated with this probe
    */
   public ArrayList<ModelComponent> getPositionComponents() {
      ArrayList<ModelComponent> comps = new ArrayList<>();
      updateCompOffsetMap();
      comps.addAll (myCompOffsetMap.keySet());
      return comps;
   }

   /**
    * Sets position data for a specific point controlled by this probe.  If no
    * data currently exists at the specified time, a new data knot is
    * created. Otherwise, the existing is overwritten at the location
    * corresponding to the point.
    *
    * @param point point for which the data should be set
    * @param time time at which the data should be set
    * @param pos position data for the point
    */
   public void setPointData (Point point, double time, Vector3d pos) {
      updateCompOffsetMap();
      Integer off = myCompOffsetMap.get(point);
      if (off == null) {
         throw new IllegalArgumentException (
            "Point component not associated with this list");
      }
      NumericListKnot knot = findOrAddKnot (time);
      knot.v.setSubVector (off, pos);
   }

   /**
    * Sets position data for a specific frame controlled by this probe.  If no
    * data currently exists at the specified time, a new data knot is
    * created. Otherwise, the existing is overwritten at the location
    * corresponding to the frame.
    *
    * @param frame frame for which the data should be set
    * @param time time at which the data should be set
    * @param TFW transform from frame to world, specifying the position, or
    * pose, of the frame. This will be turned into internal data based on the
    * setting of {@link #getRotationRep}.
    */
   public void setFrameData (Frame frame, double time, RigidTransform3d TFW) {
      updateCompOffsetMap();
      Integer off = myCompOffsetMap.get(frame);
      if (off == null) {
         throw new IllegalArgumentException (
            "Frame component not associated with this list");
      }
      NumericListKnot knot = findOrAddKnot (time);
      knot.v.setSubVector (off, TFW.p);
      off += 3;
      // map TFW.R into a quaternion
      Quaternion q = new Quaternion();
      q.set (TFW.R);
      // then turn the quaternion data into appropriate position info based on
      // the rotation rep and possible nearby reference data.
      double[] ref = null;
      NumericListKnot near = knot.getPrev();
      if (near == null) {
         near = knot.getNext();
      }
      if (near != null) {
         ref = near.v.getBuffer();
      }
      q.get (knot.v.getBuffer(), ref, off, myRotationRep, /*scale*/1);
   }

 }
