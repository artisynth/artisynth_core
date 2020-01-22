package artisynth.demos.wrapping;

import java.awt.Color;

import artisynth.core.materials.LinearAxialMaterial;
import artisynth.core.mechmodels.RigidCylinder;
import artisynth.core.modelbase.StepAdjustment;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.properties.PropertyList;
import maspack.render.RenderProps;

/**
 * Model to test the dynamic response of MultiPointSpring wrapping,
 * by measuring the frequency with which a cylinder bounces off
 * two wrappable strands.
 * 
 * @author John E. Lloyd
 */
public class CylinderBounceTest extends TwoStrandWrapBase {

   //private Vector3d myParticleVel = new Vector3d(-2, 0, 2);
   private Vector3d myParticleVel = null; //new Vector3d(-1, 0.5, 1);
   PosProbe myPosProbe;
   RigidCylinder myCylinder;

   public static PropertyList myProps =
      new PropertyList (CylinderBounceTest.class, TwoStrandWrapBase.class);

   static VectorNd myZeroVec = new VectorNd(7);

   static {
      myProps.add ("strandTension", "tension in the strand", 90);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public void build (String[] args) {
      myGap = 1.0;
      mySpringStiffness = 10.0;
      mySpringDamping = 0;
      myLeftKnotX = -2*size;
      myRightKnotX = 2*size;
      //myLeftKnotX = -8*size;
      myNumKnots0 = 50;
      myNumKnots1 = 50;

      parseArgs (args);

      super.build (args);

      myCylinder = new RigidCylinder (
         "cylinder", size/2, size*5, myDensity, 100);
      myCylinder.setPose (new RigidTransform3d (0, 0, 1.5*size, 0, 0, Math.PI/2));
      RenderProps.setFaceColor (myCylinder, new Color (238, 232, 170));
      myMech.addRigidBody (myCylinder);
      //myCylinder.setDynamic (false);
      RenderProps.setAlpha (myCylinder, 0.5);

      mySpring.addWrappable (myCylinder);
      myCylinder.setDynamic (true);

      addBreakPoint (10.00);

      RenderProps.setPointRadius (mySpring, 0.03);
      RenderProps.setLineRadius (mySpring, 0.015);
      mySpring.setDrawKnots (true);

      createControlPanel (myMech);
      myCylinder.setFrameDamping (0);

      myPosProbe = addPosProbe (myCylinder);
      //addPerformanceProbes();
   }

   public double getStrandTension() {
      if (mySpring.getMaterial() instanceof LinearAxialMaterial) {
         LinearAxialMaterial lmat = (LinearAxialMaterial)mySpring.getMaterial();
         return 9*lmat.getStiffness();
      }
      else {
         return 0;
      }
   }

   public void setStrandTension (double F) {
      if (mySpring.getMaterial() instanceof LinearAxialMaterial) {
         LinearAxialMaterial lmat = (LinearAxialMaterial)mySpring.getMaterial();
         lmat.setStiffness(F/9.0);
      }
   }
   public void initialize (double t) {
      super.initialize(t);
      double F = getStrandTension();
      if (F != 0) {
         double offset = -(myCylinder.getMass()*9.8)/(2*F);
         System.out.println (
            "F, expected period=" +
            F + "  " + 
            2*Math.PI/Math.sqrt(2*F/myCylinder.getMass()));
         myPosProbe.setEstimatePeriod (true, 1+offset);
      }
      else {
         myPosProbe.setEstimatePeriod (false, 0);
      }
   }

   public StepAdjustment advance (double t0, double t1, int flags) {
      double h = t1-t0;

      if (myParticleVel != null) {
         Point3d pos = new Point3d();

         if (t0 > 0) {
            myP0.getPosition (pos);
            pos.scaledAdd (h, myParticleVel);
            myP0.setPosition (pos);
         }
      }
      StepAdjustment sa = super.advance (t0, t1, flags);
      return sa;
   }


}


