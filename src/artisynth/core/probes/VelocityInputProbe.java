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

   private static final double DOUBLE_PREC = 1e-16;

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
    * @param comps specifies the Point, Frame, or FixedMeshBody components
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
    * data in a source probe. The source probe must be either a {@link
    * PositionInputProbe}, {@link PositionOutputProbe}, or a probe with an
    * equivalent property/component composition. The differentation is done by
    * estimating the (linear) derivative at the mid point between each knot,
    * and then interpolating these values between the mid points.
    *
    * <p>To have a property/component composition equivalent to a {@code
    * PositionOutputProbe} or {@code PositionOutputProbe}, the source probe
    * must reference the {@code position} properties of {@link Point}
    * components, or the {@code position} and {@code orientation} properties of
    * the frame-based components {@link Frame} or {@link FixedMeshBody}. For
    * frame-based components, the {@code orientation} property must be
    * associated with rotational subvectors.
    *
    * @param name if non-null, gives the name of the probe
    * @param source PositionInputProbe or PositionOutputProbe containing
    * the position data to differentiate
    * @param interval knot time spacing interval, or {@code -1} if knot
    * times should be determined from the source probe
    * @return created velocity probe
    */
   public static VelocityInputProbe createNumeric (
      String name, NumericProbeBase source, double interval) {
      ArrayList<ModelComponent> comps = extractSourcePositionComps (source);
      return create (name, comps, source, interval, /*useInterpolation*/false);
   }

   // /**
   //  * Creates a VelocityInputProbe for a list of components specified by {@code
   //  * comps} by numerically differentiating the position data in a source
   //  * probe. The components must be instances of Point, Frame, or
   //  * FixedMeshBody, and the source probe must have a vector size and rotation
   //  * subvector structure, if appropriate, that is compatible with a position
   //  * probe for the component list. The differentation is done by estimating
   //  * the (linear) derivative at the mid point between each knot, and then
   //  * interpolating these values between the mid points.
   //  *
   //  * @param name if non-null, gives the name of the probe
   //  * @param comps specifies the Point, Frame, or FixedMeshBody components
   //  * @param source PositionInputProbe or PositionOutputProbe containing
   //  * the position data to differentiate
   //  * @param interval knot time spacing interval, or {@code -1} if knot times
   //  * should be determined from the source probe
   //  * @return created velocity probe
   //  */
   // public static VelocityInputProbe createNumeric (
   //    String name, Collection<? extends ModelComponent> comps,
   //    NumericProbeBase source, double interval) {
   //    return create (name, comps, source, interval, /*useInterpolation*/false);
   // }

   /**
    * Creates a VelocityInputProbe by differentiating the position data in a
    * source probe. The source probe must be either a {@link
    * PositionInputProbe}, {@link PositionOutputProbe}, or a probe with an
    * equivalent property/component composition. The differentiation is
    * performed on the function defined by the source probe's interpolation
    * method, as returned by by {@link #getInterpolation}.
    *
    * <p>To have a property/component composition equivalent equivalent to a
    * {@code PositionOutputProbe} or {@code PositionOutputProbe}, the source
    * probe must reference the {@code position} properties of {@link Point}
    * components, or the {@code position} and {@code orientation} properties of
    * the frame-based components {@link Frame} or {@link FixedMeshBody}. For
    * frame-based components, the {@code orientation} property must be
    * associated with rotational subvectors.
    *
    * @param name if non-null, gives the name of the probe
    * @param source PositionInputProbe or PositionOutputProbe containing
    * the position data to differentiate
    * @param iinterval knot time spacing interval, or {@code -1} if knot
    * times should be determined from the source probe
    * @return created velocity probe
    */
   public static VelocityInputProbe createInterpolated (
      String name, NumericProbeBase source, double interval) {
      ArrayList<ModelComponent> comps = extractSourcePositionComps (source);
      return create (name, comps, source, interval, /*useInterpolation*/true);
   }

   // /**
   //  * Creates a VelocityInputProbe for a list of components specified by {@code
   //  * comps} by numerically differentiating the position data in a source
   //  * probe. The components must be instances of Point, Frame, or
   //  * FixedMeshBody, and the source probe must have a vector size and rotation
   //  * subvector structure, if appropriate,that is compatible with a position
   //  * probe for the component list. The differentiation is performed on the
   //  * function defined by the source probe's interpolation method, as returned
   //  * by by {@link #getInterpolation}.
   //  *
   //  * @param name if non-null, gives the name of the probe
   //  * @param comps specifies the Point, Frame, or FixedMeshBody components
   //  * @param source PositionInputProbe or PositionOutputProbe containing
   //  * the position data to differentiate
   //  * @param iinterval knot time spacing interval, or {@code -1} if knot
   //  * times should be determined from the source probe
   //  * @return created velocity probe
   //  */
   // public static VelocityInputProbe createInterpolated (
   //    String name, Collection<? extends ModelComponent> comps,
   //    NumericProbeBase source, double interval) {
   //    return create (name, comps, source, interval, /*useInterpolation*/true);
   // }

   protected static ArrayList<ModelComponent> extractSourcePositionComps (
      NumericProbeBase source) {

      RotationRep rotRep = source.getRotationRep();
      int[] rotSubvecOffs = source.getRotationSubvecOffsets();

      ArrayList<ModelComponent> comps = new ArrayList<>();
      int off = 0; // property offset
      int frameCnt = 0; // rotation subvector count
      for (int i=0; i<source.myPropList.size(); i++) {
         Property prop = source.myPropList.get(i);
         HasProperties host = prop.getHost();
         if (host instanceof Point) {
            // point components must be connected to their 'position'
            // properties
            if (!prop.getName().equals ("position")) {
               throw new IllegalArgumentException (
                  "source probe references Point property '"+prop.getName()+"'");
            }
            comps.add ((ModelComponent)host);
            off += 3;
         }
         else if (host instanceof Frame || host instanceof FixedMeshBody) {
            // frame-based components must be connected to their 'position' and
            // 'orientation' properties, in succession, and the 'orientation'
            // property must be associated with a rotation subvector at the
            // correct offset.
            if (!prop.getName().equals ("position") ||
                (++i >= source.myPropList.size()) ||
                ((prop=source.myPropList.get(i)).getHost() != host) ||
                (!prop.getName().equals ("orientation"))) {
              throw new IllegalArgumentException (
                 "source probe does not reference successive 'position' and "+
                 "'orientation' properties for frame-based component");
            }
            off += 3;
            if (rotRep == null || rotSubvecOffs == null ||
                frameCnt >= rotSubvecOffs.length ||
                rotSubvecOffs[frameCnt] != off) {
              throw new IllegalArgumentException (
                 "source probe does not contain correct rotation subvector " +
                 "for the "+frameCnt+"-th frame-based component");
            }
            comps.add ((ModelComponent)host);
            off += rotRep.size();
            frameCnt++;
         }
         else {
            throw new IllegalArgumentException (
               "source probe references component type "+
               host.getClass()+" which is not supported for VelocityInputProbe");
         }
      }
      return comps;
   }

   protected static VelocityInputProbe create (
      String name, Collection<? extends ModelComponent> comps,
      NumericProbeBase source, double interval, boolean interpolated) {

      double startTime = source.getStartTime();
      double stopTime = source.getStopTime();
      VelocityInputProbe vprobe = new VelocityInputProbe (
         name, comps, startTime, stopTime);
      //checkVectorStructure (comps, source);
      double scale = source.getScale();
      vprobe.setScale (scale);
      // now set the data by differentiating the NumericList
      NumericList nlist = source.getNumericList();
      VectorNd vel = new VectorNd(vprobe.getVsize());
      if (interval == -1) {
         NumericListKnot knot;
         for (knot=nlist.getFirst(); knot!=null; knot=knot.getNext()) {
            double tloc = knot.t;
            if (interpolated) {
               nlist.interpolateDeriv (vel, tloc);
            }
            else {
               nlist.numericalDeriv (vel, tloc);
            }
            vprobe.addData (tloc, vel);         
         }
      }
      else {
         double tend = (stopTime-startTime)/scale;
         double tinc = interval/scale;
         double ttol = 100*DOUBLE_PREC*(stopTime-startTime);

         double tloc = 0;
         while (tloc <= tend) {
            if (interpolated) {
               nlist.interpolateDeriv (vel, tloc);
            }
            else {
               nlist.numericalDeriv (vel, tloc);
            }
            vprobe.addData (tloc, vel);
            // make sure we include the end point while avoiding very small
            // knot spacings
            if (tloc < tend && tloc+tinc > tend-ttol) {
               tloc = tend;
            }
            else {
               tloc += tinc;
            }
         }
      }
      return vprobe;
   }
}
