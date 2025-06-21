package artisynth.core.probes;

import java.util.*;
import java.io.*;

import artisynth.core.mechmodels.*;
import artisynth.core.modelbase.*;

import maspack.matrix.*;
import maspack.util.*;
import maspack.properties.*;
import maspack.interpolation.*;

/**
 * Probe for recording the positions and poses of a set of Point, Frame, and
 * FixedMeshBody components. Translational position information is read from
 * the component's {@code position} property. For components with orientation
 * (Frame and FixedMeshBody), the rotation information is read from the
 * components's {@code orientation} property, but is represented within the
 * probe data in a manner specifed by a {@link RotationRep} object, using
 * either 3 or 4 numbers. Rotation information is interpolated as curves on
 * SO(3), as originally described by Ken Shoemake in his SIGGRAPH 1985 paper
 * "Animating Rotation with Quaternion Curves".
 */
public class PositionOutputProbe extends NumericOutputProbe {

   HashMap<ModelComponent,Integer> myCompOffsetMap = null;

   private void updateCompOffsetMap() {
      if (myCompOffsetMap == null) {
         myCompOffsetMap = getPositionCompOffsetMap ();
      }
   }

   /**
    * No-args constructor needed for scanning.
    */
   public PositionOutputProbe () {
   }

   /**
    * Constructs a PositionOutputProbe for a single Point, Frame, or
    * FixedMeshBody, as specified by {@code comp}. The update interval is set
    * to {@code [-1}, meaning that updates will occur every simulation step.
    * 
    * @param name if non-null, gives the name of the probe
    * @param comp specifies the Point, Frame, or FixedMeshBody
    * @param rotRep rotation representation. Can be null if {@code comp}
    * is a Point.
    * @param startTime start time of the probe
    * @param stopTime stop time of the probe
    */
   public PositionOutputProbe (
      String name, ModelComponent comp, RotationRep rotRep,
      double startTime, double stopTime) {
      this (name, comp, rotRep, /*fileName*/null,
            startTime, stopTime, /*interval*/-1);
   }

   /**
    * Constructs a PositionOutputProbe for a list of Point, Frame, or
    * FixedMeshBody components specified by {@code comps}. The update interval
    * is set to {@code [-1}, meaning that updates will occur every simulation
    * step.
    *
    * @param name if non-null, gives the name of the probe
    * @param comps specifies the Point, Frame, or FixedMeshBody components
    * @param rotRep rotation representation. Can be null if all of the components
    * are points.
    * @param startTime start time of the probe
    * @param stopTime stop time of the probe
    */
   public PositionOutputProbe (
      String name, Collection<? extends ModelComponent> comps, 
      RotationRep rotRep, double startTime, double stopTime) {
      this (name, comps, rotRep, /*fileName*/null,
            startTime, stopTime, /*interval*/-1);
   }
   
   /**
    * Constructs a PositionOutputProbe for a single Point, Frame, or
    * FixedMeshBody, as specified by {@code comp}.
    * 
    * @param name if non-null, gives the name of the probe
    * @param comp specifies the Point, Frame, or FixedMeshBody
    * @param rotRep rotation representation. Can be null if {@code comp}
    * is a Point.
    * @param fileName if non-null, specifies the attached file to which probe
    * data can be saved
    * @param startTime start time of the probe
    * @param stopTime stop time of the probe
    * @param interval probe update interval. If set to {@code -1}, updates will
    * occur every simulation step.
    */
   public PositionOutputProbe (
      String name, ModelComponent comp, RotationRep rotRep,
      String fileName, double startTime, double stopTime, double interval) {
      setOutputProperties (
         findPositionPropsAndOffsets(
            new ModelComponent[] { comp }, rotRep, /*targetProps*/false));
      if (fileName != null) {
         setAttachedFileName (fileName);
      }
      if (name != null) {
         setName (name);
      }
      setUpdateInterval (interval);
      setStartTime (startTime);
      setStopTime (stopTime);
   }

   /**
    * Constructs a PositionOutputProbe for a list of Point, Frame, or
    * FixedMeshBody components specified by {@code comps}.
    *
    * @param name if non-null, gives the name of the probe
    * @param comps specifies the Point, Frame, or FixedMeshBody components
    * @param rotRep rotation representation. Can be null if all of the components
    * are points.
    * @param fileName if non-null, specifies the attached file to which probe
    * data can be saved
    * @param startTime start time of the probe
    * @param stopTime stop time of the probe
    * @param interval probe update interval. If set to {@code -1}, updates will
    * occur every simulation step.
    */
   public PositionOutputProbe (
      String name, Collection<? extends ModelComponent> comps, RotationRep rotRep,
      String fileName, double startTime, double stopTime, double interval) {
      ModelComponent[] carray =
         comps.toArray(new ModelComponent[0]);
      setOutputProperties (
         findPositionPropsAndOffsets (carray, rotRep, /*targetProps*/false));
      if (name != null) {
         setName (name);
      }
      if (fileName != null) {
         setAttachedFileName (fileName);
      }
      setUpdateInterval (interval);
      setStartTime (startTime);
      setStopTime (stopTime);
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
      
}
