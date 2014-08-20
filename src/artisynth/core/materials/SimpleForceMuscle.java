package artisynth.core.materials;

import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix3dBase;
import maspack.matrix.Matrix6d;
import maspack.matrix.SymmetricMatrix3d;
import maspack.matrix.Vector3d;
import maspack.properties.PropertyList;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;

/**
 * A muscle material with no passive force that simply adds a constant force
 * proportional to the excitation signal. Used mostly for debugging. Note that
 * a constant force does *not* mean constant stress - as the cross sectional
 * area in the force direction changes, the stress needs to vary as well in
 * order to keep the force constant.
 */
public class SimpleForceMuscle extends MuscleMaterial {

   protected static double DEFAULT_MAX_STRESS = 3e4; // \sigma_{max} arbitrary 
   
   /*
    * default muscle parameters from Blemker et al., 2005, J Biomech, 38:657-665  
    */
   protected static double DEFAULT_MAX_LAMBDA = 1.4; // \lambda^* 

   protected double myMaxStress = DEFAULT_MAX_STRESS;

   protected PropertyMode myMaxStressMode = PropertyMode.Inherited;

   protected Vector3d myTmp = new Vector3d();
   protected Matrix3d myMat = new Matrix3d();

   // Set this true to keep the tangent matrix continuous (and symmetric) at
   // lam = lamOpt, at the expense of slightly negative forces for lam < lamOpt
   protected static boolean myZeroForceBelowLamOptP = false;
   protected static boolean myZeroForceBelowNegativeJ = true;

   public SimpleForceMuscle() {
      super();
   }

   public SimpleForceMuscle (double maxStress) {
      this();
      setMaxStress (maxStress);
   }

   public static PropertyList myProps =
      new PropertyList (SimpleForceMuscle.class, MuscleMaterial.class);   

   static {
      myProps.addInheritable (
         "maxStress", "maximum isometric stress", DEFAULT_MAX_STRESS);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public synchronized void setMaxStress (double maxStress) {
      myMaxStress = maxStress;
      myMaxStressMode =
         PropertyUtils.propagateValue (
            this, "maxStress", myMaxStress, myMaxStressMode);
      notifyHostOfPropertyChange();
   }

   public double getMaxStress() {
      return myMaxStress;
   }

   public void setMaxStressMode (PropertyMode mode) {
      myMaxStressMode =
         PropertyUtils.setModeAndUpdate (
            this, "maxStress", myMaxStressMode, mode);
   }

   public PropertyMode getMaxStressMode() {
      return myMaxStressMode;
   }

   /** 
    * Stress is computed from
    * <pre>
    * 2*W4*I4/J*(a (x) a - 1/3 I )
    * </pre>
    * 
    * @param
    * @return
    */
   private void addStress (
      Matrix3dBase sig, double J, double I4, double W4, Vector3d a) {
      
      // Stress is computed as
      //
      // 2*W4*I4/J*( a (x) a - 1/3 I )
      //
      
      if (myZeroForceBelowNegativeJ && J <= 0) {
         return;
      }

      double T00 = a.x*a.x;
      double T01 = a.x*a.y;
      double T02 = a.x*a.z;

      double T11 = a.y*a.y;
      double T12 = a.y*a.z;
      double T22 = a.z*a.z;

      double c = 2.0*W4*I4/J;

      // add to stress:

      sig.m00 += c*(T00 - 1/3.0);
      sig.m11 += c*(T11 - 1/3.0);
      sig.m22 += c*(T22 - 1/3.0);
      sig.m01 += c*T01;
      sig.m12 += c*T12;
      sig.m02 += c*T02;

      sig.m10 = sig.m01;
      sig.m20 = sig.m02;
      sig.m21 = sig.m12;
   }

   /** 
    * Stress is computed from
    * <pre>
    * 2*W4*I4/J*(a (x) a - 1/3 I )
    * </pre>
    * 
    * @param
    * @return
    */
   private void setStress (
      Matrix3dBase sig, double J, double I4, double W4, Vector3d a) {
      
      // Stress is computed as
      //
      // 2*W4*I4/J*( a (x) a - 1/3 I )
      //

      if (myZeroForceBelowNegativeJ && J <= 0) {
         return;
      }
      
      double T00 = a.x*a.x;
      double T01 = a.x*a.y;
      double T02 = a.x*a.z;

      double T11 = a.y*a.y;
      double T12 = a.y*a.z;
      double T22 = a.z*a.z;

      double c = 2.0*W4*I4/J;

      // add to stress:

      sig.m00 = c*(T00 - 1/3.0);
      sig.m11 = c*(T11 - 1/3.0);
      sig.m22 = c*(T22 - 1/3.0);
      sig.m01 = c*T01;
      sig.m12 = c*T12;
      sig.m02 = c*T02;

      sig.m10 = sig.m01;
      sig.m20 = sig.m02;
      sig.m21 = sig.m12;
   }

   public void computeStress (
      SymmetricMatrix3d sigma, double excitation, Vector3d dir0,
      SolidDeformation def, FemMaterial baseMat) {
      
      // Methods and naming conventions follow the paper "Finite element
      // implementation of incompressible, isotropic hyperelasticity", by
      // Weiss, Makerc, and Govindjeed, Computer Methods in Applied Mechanical
      // Engineering, 1996.

      double J = def.getDetF();
      if (myZeroForceBelowNegativeJ && J <= 0) {
         return;
      }
      
      Vector3d dir = myTmp;
      def.getF().mul (dir, dir0);
      double mag = dir.norm();
      dir.scale (1/mag);
      double lamd = mag*Math.pow(J, -1.0/3.0);
      double I4 = lamd*lamd;

      double W4 = 0.5*(excitation*myMaxStress)/lamd;
      
      setStress (sigma, J, I4, W4, dir);
   }

   public void computeTangent (
      Matrix6d D, SymmetricMatrix3d stress, double excitation, Vector3d dir0, 
      SolidDeformation def, FemMaterial baseMat) {

      double J = def.getDetF();
      if (myZeroForceBelowNegativeJ && J <= 0) {
         return;
      }
      
      Vector3d a = myTmp;
      def.getF().mul (a, dir0);
      double lam = a.norm();
      a.scale (1/lam);
      double lamd = lam*Math.pow(J, -1.0/3.0);
      double I4 = lamd*lamd;

      double W4  = 0.5*(myMaxStress*excitation)/lamd;
      double W44 = 0.5*(-W4)/(lamd*lamd);

      double w0 = W4*I4;
      double wa = W44*I4*I4;

      D.setZero();
      //
      // compute -2/3 (dev sigma (X) I)' - 4 wa/(3J) (a (X) a (X) I)'
      //
      myMat.outerProduct (a, a);
      myMat.scale (2*wa/J); // will be scaled again by -2/3 below
      addStress (myMat, J, I4, W4, a);
      myMat.scale (-2/3.0);
      TensorUtils.addSymmetricIdentityProduct (D, myMat);
      TensorUtils.addScaledIdentity (D, 4/3.0*w0/J);
      TensorUtils.addScaledIdentityProduct (D, 4/9.0*(wa-w0)/J);

      TensorUtils.addScaled4thPowerProduct (D, 4*wa/J, a);
   }

   @Override
   public boolean hasSymmetricTangent() {
      return true;
   }

   public boolean equals (MuscleMaterial mat) {
      if (!(mat instanceof SimpleForceMuscle)) {
         return false;
      }
      SimpleForceMuscle mrm = (SimpleForceMuscle)mat;
      if (myMaxStress != mrm.myMaxStress) {
         return false;
      }
      else {
         return super.equals (mat);
      }
   }

   public SimpleForceMuscle clone() {
      SimpleForceMuscle mat = (SimpleForceMuscle)super.clone();
      mat.myTmp = new Vector3d();
      mat.myMat = new Matrix3d();
      return mat;
   }

   @Override
   public void scaleDistance(double s) {
      if (s != 1) {
         super.scaleDistance(s);
         setMaxStress (myMaxStress/s);
      }            
   }

   @Override
   public void scaleMass(double s) {
      if (s != 1) {
         super.scaleMass(s);
         setMaxStress (myMaxStress*s);
      }
   }
   
}
