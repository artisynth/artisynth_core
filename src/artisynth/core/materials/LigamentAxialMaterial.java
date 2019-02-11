package artisynth.core.materials;

import maspack.properties.*;

public class LigamentAxialMaterial extends AxialMaterial {

   protected static double DEFAULT_STIFFNESS = 0;
   protected static double DEFAULT_DAMPING = 0;
   protected static double DEFAULT_REST_LENGTH = 0;   
   protected static double DEFAULT_L0_VARIATION_PERC = 0.01;		// 1% deviation by default

   protected double myElongStiffness = DEFAULT_STIFFNESS; // elongation stiffness
   protected double myCompStiffness = DEFAULT_STIFFNESS; // compression stiffness
   protected double myDamping = DEFAULT_DAMPING; // damping

   protected PropertyMode myElongStiffnessMode = PropertyMode.Inherited;
   protected PropertyMode myCompStiffnessMode = PropertyMode.Inherited;
   protected PropertyMode myDampingMode = PropertyMode.Inherited;

   protected double myL0variationPerc = DEFAULT_L0_VARIATION_PERC;		// percentage of variation of the range of l0 +/- (myL0variationPerc*l0) (/2) to perform the cubic blend
   
   public static PropertyList myProps =
      new PropertyList (LigamentAxialMaterial.class, AxialMaterial.class);

   static {
      myProps.addInheritable (
	         "ElongStiffness", "linear elongation stiffness",DEFAULT_STIFFNESS );
      myProps.addInheritable (
	         "CompStiffness", "linear compression stiffness",DEFAULT_STIFFNESS );
      myProps.addInheritable (
         "damping", "linear damping", DEFAULT_DAMPING);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public double getElongStiffness() {
      return myElongStiffness;
   }

   public synchronized void setElongStiffness (double E) {
      myElongStiffness = E;
      myElongStiffnessMode =
         PropertyUtils.propagateValue (
            this, "ElongStiffness", myElongStiffness, myElongStiffnessMode);
      notifyHostOfPropertyChange();
   }

   public PropertyMode getElongStiffnessMode() {
      return myElongStiffnessMode;
   }

   public void setElongStiffnessMode (PropertyMode mode) {
      myElongStiffnessMode =
         PropertyUtils.setModeAndUpdate (
            this, "ElongStiffness", myElongStiffnessMode, mode);
   }


   public double getCompStiffness() {
      return myCompStiffness;
   }

   public synchronized void setCompStiffness (double E) {
      myCompStiffness = E;
      myCompStiffnessMode =
         PropertyUtils.propagateValue (
            this, "CompStiffness", myCompStiffness, myCompStiffnessMode);
      notifyHostOfPropertyChange();
   }

   public PropertyMode getCompStiffnessMode() {
      return myCompStiffnessMode;
   }

   public void setCompStiffnessMode (PropertyMode mode) {
      myCompStiffnessMode =
         PropertyUtils.setModeAndUpdate (
            this, "CompStiffness", myCompStiffnessMode, mode);
   }

   public double getL0variationPerc() {
      return myL0variationPerc;
   }

   public synchronized void setL0variationPerc (double wPerc) {
      myL0variationPerc = wPerc;
      notifyHostOfPropertyChange();
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

   public LigamentAxialMaterial (){
   }

   public LigamentAxialMaterial (double kElong, double kComp, double d) {
      setElongStiffness (kElong);
      setCompStiffness (kComp);
      setDamping (d);
   }
   
   public double computeF (double l, double ldot, double l0, double excitation) {
      double w = myL0variationPerc*l0;
      double s = (l-l0+w/2)/w; // varies from 0 to 1 over blend window
      double kc = myCompStiffness;
      double ke = myElongStiffness;
      double Fc = kc*(l-l0);
      double Fe = ke*(l-l0);
     
      double FreturnValue = 0;
            
      if (s >= 1) {		// equivalent to l > l0+w/2
	 FreturnValue = Fe + myDamping*ldot;
     }
      else if (s <= 0) {		// equivalent to l < l0-w/2
	 FreturnValue = Fc + myDamping*ldot;
     }
//      else if (l < l0-w/2) {		// better to test: if (l > l0-w/2) equivalent to if (l < l0+w/2), no need for else
      else if (l < l0+w/2) {
	 // in the blend window
	 // Hermite cubic blend
	 double a3 =  2*Fc - 2*Fe + w*(kc+ke);
         double a2 = -3*Fc + 3*Fe - 2*w*kc - w*ke;
         double a1 =  w*kc;
         double a0 =  Fc;
         
         FreturnValue = ((a3*s + a2)*s + a1)*s + a0 + myDamping*ldot;
      }
      
      return FreturnValue;
   }

   public double computeDFdl (double l, double ldot, double l0, double excitation) {
      double w = myL0variationPerc*l0;
      double s = (l-l0+w/2)/w; // varies from 0 to 1 over blend window
      double kc = myCompStiffness;
      double ke = myElongStiffness;
      double Fc = kc*(l-l0);
      double Fe = ke*(l-l0);
      
      double DFdlReturnValue = 0;
      
      if (s >= 1) {
	 DFdlReturnValue = ke;
      }
      else if (s <= 0) {
	 DFdlReturnValue = kc;
      }
//      else if (l < l0-w/2) {
      else if (l < l0+w/2) {
	 // in the blend window
	 double a3 =  2*Fc - 2*Fe + w*(kc+ke);
         double a2 = -3*Fc + 3*Fe - 2*w*kc - w*ke;
         double a1 =  w*kc;
         
         DFdlReturnValue = ((3*a3*s + 2*a2)*s + a1)/w;
      }
      
      return DFdlReturnValue;
   } 
   
   public double computeDFdldot (
      double l, double ldot, double l0, double excitation) {
      return myDamping;
   }

   public boolean isDFdldotZero() {
      return myDamping == 0;
   }

   public boolean equals (AxialMaterial mat) {
      if (!(mat instanceof LigamentAxialMaterial)) {
         return false;
      }
      LigamentAxialMaterial linm = (LigamentAxialMaterial)mat;
      if (myElongStiffness != linm.myElongStiffness ||
          myDamping != linm.myDamping) {
         return false;
      }
      else {
         return super.equals (mat);
      }
   }

   public LigamentAxialMaterial clone() {
      LigamentAxialMaterial mat = (LigamentAxialMaterial)super.clone();
      return mat;
   }

   @Override
   public void scaleDistance (double s) {
      super.scaleDistance (s);
   }

   @Override
   public void scaleMass (double s) {
      super.scaleMass (s);
      myElongStiffness *= s;
      myDamping *= s;
   }
}



