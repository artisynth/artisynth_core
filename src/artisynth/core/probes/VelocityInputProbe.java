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
 * Probe for specifying the velocities of a set of Point or Frame
 * components. Velocity information is specified in world coordinates and is
 * attached to either the component's {@code velocity} or {@code
 * targetVelocity} property, depending on availability and the setting of the
 * constructor's {@code useTargetProps} argument. For components with orientation
 * (i.e., Frame), the velocity will be a 6-vector (Twist) giving both
 * translational and angular velocity.
 */
public class VelocityInputProbe extends NumericInputProbe {

   private static final double DOUBLE_PREC = 1e-16;

   /**
    * No-args constructor needed for scanning.
    */
   public VelocityInputProbe () {
   }

   /**
    * Constructs a VelocityInputProbe for a single Point or Frame as specified
    * by {@code comp}. Data must be added after the constructor is called.
    *
    * @param name if non-null, gives the name of the probe
    * @param comp specifies the Point or Frame
    * @param useTargetProps if {@code true}, causes the probe to bind to the
    * component's {@code targetVelocity} property instead of
    * {@code velocity}
    * @param startTime start time of the probe
    * @param stopTime stop time of the probe
    */
   public VelocityInputProbe (
      String name, ModelComponent comp, 
      boolean useTargetProps, double startTime, double stopTime) {
      setModelFromComponent (comp);
      setInputProperties (
         findVelocityProps(new ModelComponent[] { comp }, useTargetProps));
      initFromStartStopTime (name, startTime, stopTime);
   }

   /**
    * Constructs a VelocityInputProbe for a list of Point or Frame components
    * specified by {@code comps}. Data must be added after the constructor is
    * called.
    * 
    * @param name if non-null, gives the name of the probe
    * @param comps specifies the Point or Frame components
    * @param useTargetProps if {@code true}, causes the probe to bind to the
    * component's {@code targetVelocity} property instead of
    * {@code velocity}
    * @param startTime start time of the probe
    * @param stopTime stop time of the probe
    */
   public VelocityInputProbe (
      String name, Collection<? extends ModelComponent> comps,
      boolean useTargetProps, double startTime, double stopTime) {
      ModelComponent[] carray =
         comps.toArray(new ModelComponent[0]);
      setModelFromComponent (carray[0]);
      setInputProperties (findVelocityProps (carray, useTargetProps));
      initFromStartStopTime (name, startTime, stopTime);
   }

   /**
    * Constructs a VelocityInputProbe for a single Point or Frame,
    * as specified by {@code comp}. Probe data, plus its interpolation method
    * and start and stop times, are specified in a probe file.
    * 
    * @param name if non-null, gives the name of the probe
    * @param comp specifies the Point or Frame
    * @param useTargetProps if {@code true}, causes the probe to bind to the
    * component's {@code targetVelocity} property instead of
    * {@code velocity}
    * @param filePath path name of the probe data file
    * @throws IOException if a file I/O error occurs
    */
   public VelocityInputProbe (String name, ModelComponent comp, 
      boolean useTargetProps, String filePath) throws IOException {
      setModelFromComponent (comp);
      setInputProperties (
         findVelocityProps(
            new ModelComponent[] { comp }, useTargetProps));
      initFromFile (name, filePath);
   }

   /**
    * Constructs a VelocityInputProbe for a list of Point or Frame
    * components specified by {@code comps}. Probe data, plus its interpolation
    * method and start and stop times, are specified in a probe file.
    * 
    * @param name if non-null, gives the name of the probe
    * @param comps specifies the Point or Frame components
    * @param useTargetProps if {@code true}, causes the probe to bind to the
    * component's {@code targetVelocity} property instead of
    * {@code velocity}
    * @param filePath path name of the probe data file
    * @throws IOException if a file I/O error occurs
    */
   public VelocityInputProbe (
      String name, Collection<? extends ModelComponent> comps, 
      boolean useTargetProps, String filePath) throws IOException {
      ModelComponent[] carray =
         comps.toArray(new ModelComponent[0]);
      setModelFromComponent (carray[0]);
      setInputProperties (findVelocityProps (carray, useTargetProps));
      initFromFile (name, filePath);
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
    * {@link Frame} components. For frame-based components, the {@code
    * orientation} property must be associated with rotational subvectors.
    *
    * @param name if non-null, gives the name of the probe
    * @param source PositionInputProbe or PositionOutputProbe containing
    * the position data to differentiate
    * @param useTargetProps if {@code true}, causes the probe to bind to the
    * component's {@code targetVelocity} property instead of
    * @param interval knot time spacing interval, or {@code -1} if knot
    * times should be determined from the source probe
    * @return created velocity probe
    */
   public static VelocityInputProbe createNumeric (
      String name, NumericProbeBase source,
      boolean useTargetProps, double interval) {
      ArrayList<ModelComponent> comps = extractSourcePositionComps (source);
      return create (
         name, comps, source, useTargetProps, interval,
         /*useInterpolation*/false);
   }

   /**
    * @deprecated Use {@link
    * #createNumeric(String,NumericProbeBase,boolean,double)} instead.  This
    * method calls that method with {@code useTargetProps} set to {@code false},
    */
   public static VelocityInputProbe createNumeric (
      String name, NumericProbeBase source, double interval) {
      return createNumeric (name, source, /*targetProps*/false, interval);
   }

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
    * the {@link Frame} components. For frame-based components, the {@code
    * orientation} property must be associated with rotational subvectors.
    *
    * @param name if non-null, gives the name of the probe
    * @param source PositionInputProbe or PositionOutputProbe containing
    * the position data to differentiate
    * @param useTargetProps if {@code true}, causes the probe to bind to the
    * component's {@code targetVelocity} property instead of
    * @param interval knot time spacing interval, or {@code -1} if knot
    * times should be determined from the source probe
    * @return created velocity probe
    */
   public static VelocityInputProbe createInterpolated (
      String name, NumericProbeBase source,
      boolean useTargetProps, double interval) {
      ArrayList<ModelComponent> comps = extractSourcePositionComps (source);
      return create (
         name, comps, source, useTargetProps, interval, 
         /*useInterpolation*/true);
   }

   /**
    * @deprecated Use {@link
    * #createInterpolated(String,NumericProbeBase,boolean,double)} instead.  This
    * method calls that method with {@code useTargetProps} set to {@code false},
    */
   public static VelocityInputProbe createInterpolated (
      String name, NumericProbeBase source, double interval) {
      return createInterpolated (name, source, /*targetProps*/false, interval);
   }

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
            // point components must be connected to their 'position' or
            // 'targetPosition' properties
            if (!prop.getName().equals ("position") &&
                !prop.getName().equals ("targetPosition")) {
               throw new IllegalArgumentException (
                  "source probe references Point property '"+prop.getName()+"'");
            }
            comps.add ((ModelComponent)host);
            off += 3;
         }
         else if (host instanceof Frame) {
            // frame-based components must be connected to their 'position' and
            // 'orientation' properties, in succession, and the 'orientation'
            // property must be associated with a rotation subvector at the
            // correct offset.
            boolean propsValid = true;
            if ((!prop.getName().equals ("position") &&
                 !prop.getName().equals ("targetPosition")) ||
                // ensure there's another prop attached to same host
                (++i >= source.myPropList.size()) ||
                ((prop=source.myPropList.get(i)).getHost() != host) ||
                // ensure other prop accesses orientation or targetOrientation
                (!prop.getName().equals ("orientation") &&
                 !prop.getName().equals ("targetOrientation"))) {
              throw new IllegalArgumentException (
                 "source probe does not reference successive 'position' (or "+
                 "'targetPosition') and 'orientation' (or 'targetOrientation') " +
                 "properties for frame-based component");
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
      NumericProbeBase source, boolean useTargetProps,
      double interval, boolean interpolated) {

      double startTime = source.getStartTime();
      double stopTime = source.getStopTime();
      VelocityInputProbe vprobe = new VelocityInputProbe (
         name, comps, useTargetProps, startTime, stopTime);
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
         double ttol = 1e-12*(stopTime-startTime);

         double tloc = 0;
         while (tloc <= tend) {
            if (interpolated) {
               nlist.interpolateDeriv (vel, tloc);
            }
            else {
               nlist.numericalDeriv (vel, tloc);
            }
            vprobe.addData (tloc, vel);
            if (tloc == tend) {
               break;
            }
            else {
               tloc += tinc;
               // snap to tend to avoid very small knot spacings
               if (tloc >= tend-ttol) {
                  tloc = tend;
               }
            }
         }
      }
      return vprobe;
   }

   /**
    * Apply a rigid or affine transform to all the vector data in this list.
    *
    * @param X transform to apply
    */
   public void transformData (AffineTransform3dBase X) {
      getNumericList().transformVectorData (X);
   }

   /**
    * Returns a list of the position components associated with this probe, in
    * the order that they appear in the probe's data.
    *
    * @return list of components associated with this probe
    */
   public ArrayList<ModelComponent> getPositionComponents() {
      return getPositionCompsForVelocity();
   }


}
