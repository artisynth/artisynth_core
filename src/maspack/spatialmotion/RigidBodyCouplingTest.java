package maspack.spatialmotion;

import java.util.*;

import maspack.util.*;
import maspack.matrix.*;

import maspack.spatialmotion.RigidBodyConstraint.MotionType;

public class RigidBodyCouplingTest extends UnitTest {

   public void setCoordinateRanges (RigidBodyCoupling coupling) {
      int numc = coupling.numCoordinates();
      for (int i=0; i<numc; i++) {
         if (coupling.getCoordinateMotionType(i) == MotionType.ROTARY) {
            // use +/- 60 deg to stay clear of singularities
            coupling.setCoordinateRange (
               i, new DoubleInterval (-Math.PI/3, Math.PI/3));
         }
         else {
            coupling.setCoordinateRange (
               i, new DoubleInterval (-0.5, 0.5));
         }
      }
   }

   public VectorNd getRandomCoordinates (RigidBodyCoupling coupling) {
      int numc = coupling.numCoordinates();
      VectorNd coords = new VectorNd (numc);
      for (int i=0; i<numc; i++) {
         DoubleInterval range = coupling.getCoordinateRange (i);
         coords.set (
            i, RandomGenerator.nextDouble(
               range.getLowerBound(), range.getUpperBound()));
      }
      return coords;
   }

   protected ArrayList<Twist> copyCoordinateTwists (RigidBodyCoupling coupling) {
      ArrayList<Twist> twists = new ArrayList<>();
      ArrayList<Twist> tcopy = new ArrayList<>();
      coupling.getCoordinateTwists (twists);
      for (Twist tw : twists) {
         tcopy.add (new Twist(tw));
      }
      return tcopy;
   }

   protected ArrayList<Twist> computeNumericCoordinateTwists (
      RigidBodyCoupling coupling, VectorNd coords0) {

      int numc = coupling.numCoordinates();
      ArrayList<Twist> twists = new ArrayList<>();
      VectorNd coords = new VectorNd();
      RigidTransform3d TCD0 = new RigidTransform3d();
      RigidTransform3d TCD = new RigidTransform3d();
      RigidTransform3d TN0 = new RigidTransform3d();
      coupling.setCoordinateValues (coords0, TCD0);
      double h = 1e-8;
      for (int i=0; i<numc; i++) {
         coords.set (coords0);
         coords.set (i, coords0.get(i)+h);
         coupling.setCoordinateValues (coords, TCD);
         Twist tw = new Twist();
         // find twist from TCD0 to TCD
         TN0.mulInverseLeft (TCD0, TCD);
         tw.set (TN0);
         tw.scale (1/h);
         twists.add (tw);
      }
      return twists;
   }

   public void testTwistsAndWrenches (RigidBodyCoupling coupling) {
      int nsamps = 100;
      setCoordinateRanges (coupling);
      int numc = coupling.numCoordinates();
      for (int i=0; i<nsamps; i++) {
         VectorNd coords = getRandomCoordinates (coupling);
         RigidTransform3d TCD = new RigidTransform3d ();
         coupling.setCoordinateValues (coords, TCD);
         // call update constrainst with zero error and velocity
         coupling.updateConstraints (
            TCD, TCD, Twist.ZERO, Twist.ZERO, /*updateEngaged*/false);
         ArrayList<Twist> twists = copyCoordinateTwists (coupling);
         ArrayList<Twist> tcheck =
            computeNumericCoordinateTwists (coupling, coords);
         for (int idx=0; idx<numc; idx++) {
            checkEquals (
               "coordinate twist "+idx+" for coupling "+coupling,
               twists.get(idx), tcheck.get(idx), 1e-7);
         }
      }
   }

   public void test (RigidBodyCoupling coupling) {
      coupling.initializeConstraintInfo();
      int numc = coupling.numCoordinates();
      if (numc > 0) {
         testTwistsAndWrenches (coupling);
      }
   }

   public void test() {

      // hinge
      HingeCoupling hinge;
      hinge = new HingeCoupling();
      hinge.setThetaClockwise (false);
      test (hinge);
      hinge = new HingeCoupling();
      hinge.setThetaClockwise (true);
      test (hinge);

      // cylinder
      CylindricalCoupling cylinder;
      cylinder = new CylindricalCoupling();
      cylinder.setThetaClockwise (false);
      test (cylinder);
      cylinder = new CylindricalCoupling();
      cylinder.setThetaClockwise (true);
      test (cylinder);
      
      // gimbal
      GimbalCoupling gimbal;
      gimbal = new GimbalCoupling(GimbalCoupling.AxisSet.ZYX);
      gimbal.setUseRDC(false);
      test (gimbal);
      gimbal = new GimbalCoupling(GimbalCoupling.AxisSet.ZYX);
      gimbal.setUseRDC(true);
      test (gimbal);
      gimbal = new GimbalCoupling(GimbalCoupling.AxisSet.XYZ);
      gimbal.setUseRDC(false);
      test (gimbal);
      gimbal = new GimbalCoupling(GimbalCoupling.AxisSet.XYZ);
      gimbal.setUseRDC(true);
      test (gimbal);

      // universal
      UniversalCoupling universal;
      for (int i=0; i<10; i++) {
         double skew = 0;
         if (i > 0) {
            skew = RandomGenerator.nextDouble (-Math.PI/4, Math.PI);
         }
         universal = new UniversalCoupling(skew, UniversalCoupling.AxisSet.ZY);
         universal.setUseRDC(false);
         test (universal);
         universal = new UniversalCoupling(skew, UniversalCoupling.AxisSet.ZY);
         universal.setUseRDC(true);
         test (universal);
         universal = new UniversalCoupling(skew, UniversalCoupling.AxisSet.XY);
         universal.setUseRDC(false);
         test (universal);
         universal = new UniversalCoupling(skew, UniversalCoupling.AxisSet.XY);
         universal.setUseRDC(true);
         test (universal);
      }

      // slotted hinge
      test (new SlottedHingeCoupling());

      // slider
      test (new SliderCoupling());

      // planar translation
      test (new PlanarTranslationCoupling());

      // full planar
      test (new FullPlanarCoupling());

      // free
      test (new FreeCoupling(GimbalCoupling.AxisSet.ZYX));
      test (new FreeCoupling(GimbalCoupling.AxisSet.XYZ));

      // fixed axis
      test (new FixedAxisCoupling());

      // ellipsoid
      for (int i=0; i<10; i++) {
         double alpha = 0;
         if (i > 0) {
            alpha = RandomGenerator.nextDouble (-Math.PI/4, Math.PI/4);
         }
         double a = RandomGenerator.nextDouble (0.5, 3.0);
         double b = RandomGenerator.nextDouble (0.5, 3.0);
         double c = RandomGenerator.nextDouble (0.5, 3.0);
         test (new EllipsoidCoupling (a, b, c, alpha, /*openSim=*/false));
         test (new EllipsoidCoupling (a, b, c, alpha, /*openSim=*/true));
      }
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);
      RigidBodyCouplingTest tester = new RigidBodyCouplingTest();
      tester.runtest();
   }

}
