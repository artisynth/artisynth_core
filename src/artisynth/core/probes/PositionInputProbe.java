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
 * Probe for specifying the positions and poses of a set of Point, Frame, and
 * FixedMeshBody components. Translational position information is attached to
 * the component's {@code position} property. For components with orientation
 * (Frame and FixedMeshBody), the rotation information is attached to the
 * components's {@code orientation} property, but is represented within the
 * probe data in a manner specifed by a {@link RotationRep} object, using
 * either 3 or 4 numbers. Rotation information is interpolated as curves on the
 * surface of SO(3), as originally described by Ken Shoemake in his SIGGRAPH
 * 1985 paper "Animating Rotation with Quaternion Curves".
 */
public class PositionInputProbe extends NumericInputProbe {

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
    * @param rotRep rotation representation
    * @param startTime start time of the probe
    * @param stopTime stop time of the probe
    */
   public PositionInputProbe (
      String name, ModelComponent comp, RotationRep rotRep,
      double startTime, double stopTime) {
      setModelFromComponent (comp);
      setInputProperties (
         findPositionPropsAndOffsets(
            new ModelComponent[] { comp }, rotRep));
      initFromStartStopTime (name, startTime, stopTime);
   }

   /**
    * Constructs a PositionInputProbe for a list of Point, Frame, or FixedMeshBody
    * components specified by {@code comps}. Data must be added after the
    * constructor is called.
    * 
    * @param name if non-null, gives the name of the probe
    * @param comps specifies the Point, Frame, or FixedMeshBody components
    * @param rotRep rotation representation
    * @param startTime start time of the probe
    * @param stopTime stop time of the probe
    */
   public PositionInputProbe (
      String name, Collection<? extends ModelComponent> comps, RotationRep rotRep,
      double startTime, double stopTime) {
      ModelComponent[] carray =
         comps.toArray(new ModelComponent[0]);
      setModelFromComponent (carray[0]);
      setInputProperties (findPositionPropsAndOffsets (carray, rotRep));
      initFromStartStopTime (name, startTime, stopTime);
   }
   
   /**
    * Constructs a PositionInputProbe for a single Point, Frame, or FixedMeshBody,
    * as specified by {@code comp}. Probe data, plus its interpolation method
    * and start and stop times, are specified in a probe file.
    * 
    * @param name if non-null, gives the name of the probe
    * @param comp specifies the Point, Frame, or FixedMeshBody
    * @param rotRep rotation representation
    * @param filePath path name of the probe data file
    * @throws IOException if a file I/O error occurs
    */
   public PositionInputProbe (
      String name, ModelComponent comp, RotationRep rotRep, String filePath)
      throws IOException {
      setModelFromComponent (comp);
      setInputProperties (
         findPositionPropsAndOffsets(
            new ModelComponent[] { comp }, rotRep));
      initFromFile (name, filePath);
   }

   /**
    * Constructs a PositionInputProbe for a list of Point, Frame, or FixedMeshBody
    * components specified by {@code comps}. Probe data, plus its interpolation
    * method and start and stop times, are specified in a probe file.
    * 
    * @param name if non-null, gives the name of the probe
    * @param comps specifies the Point, Frame, or FixedMeshBody components
    * @param rotRep rotation representation
    * @param filePath path name of the probe data file
    * @throws IOException if a file I/O error occurs
    */
   public PositionInputProbe (
      String name, Collection<? extends ModelComponent> comps, RotationRep rotRep,
      String filePath) throws IOException {
      ModelComponent[] carray =
         comps.toArray(new ModelComponent[0]);
      setModelFromComponent (carray[0]);
      setInputProperties (findPositionPropsAndOffsets (carray, rotRep));
      initFromFile (name, filePath);
   }
      
}
