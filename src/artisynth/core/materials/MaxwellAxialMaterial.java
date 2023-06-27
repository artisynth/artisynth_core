package artisynth.core.materials;

import maspack.properties.*;
import maspack.util.*;
import artisynth.core.modelbase.*;

public class MaxwellAxialMaterial
   extends AxialMaterial implements HasNumericState {

   protected static double DEFAULT_STIFFNESS = 0;
   protected static double DEFAULT_DAMPING = 0;
   protected static double DEFAULT_SPRING_LENGTH = 0;

   protected double myStiffness = DEFAULT_STIFFNESS; // stiffness
   protected double myDamping = DEFAULT_DAMPING; // damping
   protected double mySpringLength = DEFAULT_SPRING_LENGTH;

   protected PropertyMode myStiffnessMode = PropertyMode.Inherited;
   protected PropertyMode myDampingMode = PropertyMode.Inherited;

   protected double myH;       // current time step size
   protected double mySpringLengthPrev;// previous value of SpringLength

   public static PropertyList myProps =
      new PropertyList (MaxwellAxialMaterial.class, AxialMaterial.class);

   static {
      myProps.addInheritable (
         "stiffness", "linear stiffness",DEFAULT_STIFFNESS );
      myProps.addInheritable (
         "damping", "linear damping", DEFAULT_DAMPING);
      myProps.add (
         "springLength", "spring length", DEFAULT_SPRING_LENGTH);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public double getStiffness() {
      return myStiffness;
   }

   public synchronized void setStiffness (double E) {
      myStiffness = E;
      myStiffnessMode =
         PropertyUtils.propagateValue (
            this, "stiffness", myStiffness, myStiffnessMode);
      notifyHostOfPropertyChange();
   }

   public PropertyMode getStiffnessMode() {
      return myStiffnessMode;
   }

   public void setStiffnessMode (PropertyMode mode) {
      myStiffnessMode =
         PropertyUtils.setModeAndUpdate (
            this, "stiffness", myStiffnessMode, mode);
   }

   public double getDamping() {
      return myDamping;
   }

   public synchronized void setDamping (double d) {
      myDamping = d;
      myDampingMode =
         PropertyUtils.propagateValue (
            this, "damping", myDamping, myDampingMode);
      notifyHostOfPropertyChange();
   }

   public PropertyMode getDampingMode() {
      return myDampingMode;
   }

   public void setDampingMode (PropertyMode mode) {
      myDampingMode =
         PropertyUtils.setModeAndUpdate (
            this, "damping", myDampingMode, mode);
   }

   public double getSpringLength() {
      return mySpringLength;
   }

   public void setSpringLength (double l) {
      mySpringLength = l;
      notifyHostOfPropertyChange();
   }

   public MaxwellAxialMaterial (){
   }

   public MaxwellAxialMaterial (double k, double d, double sprLen) {
      setStiffness (k);
      setDamping (d);
      setSpringLength (sprLen);
   }

   public double computeF (
      double l, double ldot, double l0, double excitation) {

      // solve for kLength velocity:
      double k = myStiffness;
      double d = myDamping;
      
      // Note: myH will be 0 on very first call to applyForces(), before first
      // step actually begin
      if (myH != 0) {
         // estimate kldot and use this to update mySpringLength for the
         // current step size
         double lkdot = (d*ldot - k*(mySpringLengthPrev-l0))/(k*myH+d);
         mySpringLength = mySpringLengthPrev + myH*lkdot;
      }
      
      // System.out.printf (
      //    "h=%g lkprev=%g lk=%g ldot=%g F=%g\n",
      //    myH, mySpringLengthPrev, mySpringLength,
      //    ldot, k*mySpringLength);

      return k*(mySpringLength-l0);
   }

   public double computeDFdl (
      double l, double ldot, double l0, double excitation) {
      return 0;
   }

   public double computeDFdldot (
      double l, double ldot, double l0, double excitation) {

      double k = myStiffness;
      double d = myDamping;
      return myH*d/(k*myH+d);
   }

   public boolean isDFdldotZero() {
      return myDamping == 0;
   }

   public boolean equals (AxialMaterial mat) {
      if (!(mat instanceof MaxwellAxialMaterial)) {
         return false;
      }
      MaxwellAxialMaterial linm = (MaxwellAxialMaterial)mat;
      if (myStiffness != linm.myStiffness ||
          myDamping != linm.myDamping) {
         return false;
      }
      else {
         return super.equals (mat);
      }
   }

   public MaxwellAxialMaterial clone() {
      MaxwellAxialMaterial mat = (MaxwellAxialMaterial)super.clone();
      return mat;
   }

   @Override
   public void scaleDistance (double s) {
      super.scaleDistance (s);
   }

   @Override
   public void scaleMass (double s) {
      super.scaleMass (s);
      myStiffness *= s;
      myDamping *= s;
   }

   // implementation of HasNumericState

   public boolean hasState() {
      return true;
   }

   public void setState (DataBuffer data) {
      mySpringLength = data.dget();
      // minor hack: store myH so that it's reset to 0 when time is reset to 0
      myH = data.dget();
   }

   public void getState (DataBuffer data) {
      data.dput(mySpringLength);
      data.dput(myH);
   }

   public void advanceState (double t0, double t1) {
      myH = (t1 - t0); // store step size for updating springLength in computeF()
      mySpringLengthPrev = mySpringLength;
   }
   
   /**
    * {@inheritDoc}
    */
   public boolean requiresAdvance() {
      return true;
   }   

}



