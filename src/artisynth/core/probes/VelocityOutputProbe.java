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
 * Probe for recording the velocities of a set of Point, Frame, and
 * FixedMeshBody components. Velocity information is read from each
 * components's {@code velocity} property and is specified in world
 * coordinates. For components with orientation (Frame and FixedMeshBody), the
 * velocity will be a 6-vector (Twist) giving both translational and angular
 * velocity.
 */
public class VelocityOutputProbe extends NumericOutputProbe {

   /**
    * No-args constructor needed for scanning.
    */
   public VelocityOutputProbe () {
   }

   /**
    * Constructs a VelocityOutputProbe for a single Point, Frame, or
    * FixedMeshBody, as specified by {@code comp}. The update interval is set
    * to {@code [-1}, meaning that updates will occur every simulation step.
    * 
    * @param name if non-null, gives the name of the probe
    * @param comp specifies the Point, Frame, or FixedMeshBody
    * @param startTime start time of the probe
    * @param stopTime stop time of the probe
    */
   public VelocityOutputProbe (
      String name, ModelComponent comp, double startTime, double stopTime) {
      this (name, comp, /*fileName*/null, startTime, stopTime, /*interval*/-1);
   }

   /**
    * Constructs a VelocityOutputProbe for a list of Point, Frame, or
    * FixedMeshBody components specified by {@code comps}. The update interval
    * is set to {@code [-1}, meaning that updates will occur every simulation
    * step.
    *
    * @param name if non-null, gives the name of the probe
    * @param comps specifies the Point, Frame, or FixedMeshBody components
    * @param startTime start time of the probe
    * @param stopTime stop time of the probe
    */
   public VelocityOutputProbe (
      String name, Collection<? extends ModelComponent> comps,
      double startTime, double stopTime) {
      this (name, comps, /*fileName*/null, startTime, stopTime, /*interval*/-1);
   }

   /**
    * Constructs a VelocityOutputProbe for a single Point, Frame, or
    * FixedMeshBody, as specified by {@code comp}.
    * 
    * @param name if non-null, gives the name of the probe
    * @param comp specifies the Point, Frame, or FixedMeshBody
    * @param fileName if non-null, specifies the attached file to which probe
    * data can be saved
    * @param startTime start time of the probe
    * @param stopTime stop time of the probe
    * @param interval probe update interval. If set to {@code -1}, updates will
    * occur every simulation step.
    */
   public VelocityOutputProbe (
      String name, ModelComponent comp, String fileName,
      double startTime, double stopTime, double interval) {
      setOutputProperties (
         findVelocityProps (new ModelComponent[] { comp }));
      if (fileName != null) {
         setAttachedFileName (fileName);
      }
      setUpdateInterval (interval);
      setStartTime (startTime);
      setStopTime (stopTime);
      if (name != null) {
         setName (name);
      }
   }

   /**
    * Constructs a VelocityOutputProbe for a list of Point, Frame, or
    * FixedMeshBody components specified by {@code comps}.
    *
    * @param name if non-null, gives the name of the probe
    * @param comps specifies the Point, Frame, or FixedMeshBody components
    * @param fileName if non-null, specifies the attached file to which probe
    * data can be saved
    * @param startTime start time of the probe
    * @param stopTime stop time of the probe
    * @param interval probe update interval. If set to {@code -1}, updates will
    * occur every simulation step.
    */
   public VelocityOutputProbe (
      String name, Collection<? extends ModelComponent> comps, String fileName,
      double startTime, double stopTime, double interval) {
      ModelComponent[] carray =
         comps.toArray(new ModelComponent[0]);
      setOutputProperties (findVelocityProps (carray));
      if (fileName != null) {
         setAttachedFileName (fileName);
      }
      setUpdateInterval (interval);
      setStartTime (startTime);
      setStopTime (stopTime);
      if (name != null) {
         setName (name);
      }
   }
}

