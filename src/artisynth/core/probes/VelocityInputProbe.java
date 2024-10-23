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
 * FixedMeshBody components. Velocity information is attached to each
 * components's {@code velocity} property and is specified in world
 * coordinates. For components with orientation (Frame and FixedMeshBody), the
 * velocity will be a 6-vector (Twist) giving both translational and angular
 * velocity.
 */
public class VelocityInputProbe extends NumericInputProbe {

   /**
    * No-args constructor needed for scanning.
    */
   public VelocityInputProbe () {
   }

   /**
    * Constructs a VelocityInputProbe for a single Point, Frame, or FixedMeshBody,
    * as specified by {@code comp}. Data must be added after the constructor is
    * called.
    *
    * @param name if non-null, gives the name of the probe
    * @param comp specifies the Point, Frame, or FixedMeshBody
    * @param startTime start time of the probe
    * @param stopTime stop time of the probe
    */
   public VelocityInputProbe (
      String name, ModelComponent comp, double startTime, double stopTime) {
      setModelFromComponent (comp);
      setInputProperties (
         findVelocityProps(new ModelComponent[] { comp }));
      initFromStartStopTime (name, startTime, stopTime);
   }

   /**
    * Constructs a VelocityInputProbe for a list of Point, Frame, or FixedMeshBody
    * components specified by {@code comps}. Data must be added after the
    * constructor is called.
    * 
    * @param name if non-null, gives the name of the probe
    * @param startTime start time of the probe
    * @param stopTime stop time of the probe
    */
   public VelocityInputProbe (
      String name, Collection<? extends ModelComponent> comps,
      double startTime, double stopTime) {
      ModelComponent[] carray =
         comps.toArray(new ModelComponent[0]);
      setModelFromComponent (carray[0]);
      setInputProperties (findVelocityProps (carray));
      initFromStartStopTime (name, startTime, stopTime);
   }

   /**
    * Constructs a VelocityInputProbe for a single Point, Frame, or FixedMeshBody,
    * as specified by {@code comp}. Probe data, plus its interpolation method
    * and start and stop times, are specified in a probe file.
    * 
    * @param name if non-null, gives the name of the probe
    * @param comp specifies the Point, Frame, or FixedMeshBody
    * @param filePath path name of the probe data file
    * @throws IOException if a file I/O error occurs
    */
   public VelocityInputProbe (String name, ModelComponent comp, String fileName)
      throws IOException {
      setModelFromComponent (comp);
      setInputProperties (
         findVelocityProps(
            new ModelComponent[] { comp }));
      initFromFile (name, fileName);
   }

   /**
    * Constructs a VelocityInputProbe for a list of Point, Frame, or FixedMeshBody
    * components specified by {@code comps}. Probe data, plus its interpolation
    * method and start and stop times, are specified in a probe file.
    * 
    * @param name if non-null, gives the name of the probe
    * @param comps specifies the Point, Frame, or FixedMeshBody components
    * @param filePath path name of the probe data file
    * @throws IOException if a file I/O error occurs
    */
   public VelocityInputProbe (
      String name, Collection<? extends ModelComponent> comps, String fileName)
      throws IOException {
      ModelComponent[] carray =
         comps.toArray(new ModelComponent[0]);
      setModelFromComponent (carray[0]);
      setInputProperties (findVelocityProps (carray));
      initFromFile (name, fileName);
   }

   /**
    * Creates a VelocityInputProbe by numerically differentiating the position
    * data in a source VelocityInputProbe or VelocityOutputProbe.  This
    * differentation is done by estimating the (linear) derivative at the mid
    * point between each knot, and then interpolating these values between the
    * mid points.
    *
    * @param probe PositionInputProbe or PositionOutputProbe containing
    * the position data to differentiate
    * @param interval spacing for the probe knot points
    */
   public static VelocityInputProbe createNumeric (
      String name, NumericProbeBase probe, double interval) {
      return create (name, probe, interval, /*useInterpolation*/false);
   }

   /**
    * Creates a VelocityInputProbe by differentiating the position data in a
    * source PositionInputProbe or PositionOutputProbe.  This differentiation
    * is performed on the function defined by the source probe's interpolation
    * method, as returned by by {@link #getInterpolation}.
    *
    * @param probe PositionInputProbe or PositionOutputProbe containing
    * the position data to differentiate
    * @param interval spacing for the probe knot points
    */
   public static VelocityInputProbe createInterpolated (
      String name, NumericProbeBase probe, double interval) {
      return create (name, probe, interval, /*useInterpolation*/true);
   }

   protected static VelocityInputProbe create (
      String name, NumericProbeBase probe, 
      double interval, boolean interpolated) {
      // find the components for the position probe
      if (!(probe instanceof PositionInputProbe) &&
          !(probe instanceof PositionOutputProbe)) {
         throw new IllegalArgumentException (
            "probe must be a PositionInputProbe or a PositionOutputProbe");
      }
      ArrayList<ModelComponent> comps = new ArrayList<>();
      for (Property prop : probe.myPropList) {
         HasProperties host = prop.getHost();
         if (host instanceof Point) {
            comps.add ((ModelComponent)host);
         }
         else if (host instanceof Frame || host instanceof FixedMeshBody) {
            if (prop.getName().equals ("position")) {
               comps.add ((ModelComponent)host);
            }
            // ignore second orientation property
         }
         else {
            throw new IllegalArgumentException (
               "position probe references component type "+
               host.getClass()+" which is not supported for VelocityInputProbe");
         }
      }
      double startTime = probe.getStartTime();
      double stopTime = probe.getStopTime();
      VelocityInputProbe vip = new VelocityInputProbe (
         name, comps, startTime, stopTime);
      double scale = probe.getScale();
      vip.setScale (scale);
      // now set the data by differentiating the NumericList
      NumericList nlist = probe.getNumericList();
      double tend = (stopTime-startTime)/scale;
      double tinc = interval/scale;
      VectorNd vel = new VectorNd(vip.getVsize());
      double tloc = 0;
      while (tloc <= tend) {
         if (interpolated) {
            nlist.interpolateDeriv (vel, tloc);
         }
         else {
            nlist.numericalDeriv (vel, tloc);
         }
         vip.addData (tloc, vel);         
         // make sure we include the end point 
         if (tloc < tend && tloc+tinc > tend) {
            tloc = tend;
         }
         else {
            tloc += tinc;
         }
      }
      return vip;
   }
}
