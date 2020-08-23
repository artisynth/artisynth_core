package artisynth.core.materials;

import artisynth.core.materials.AxialMaterial;
import maspack.interpolation.CubicHermiteSpline1d;
import maspack.properties.*;

public class Hill3ElemMuscleRigidTendon extends AxialMaterial {

   public static double DEFAULT_OPT_FIBRE_LENGTH = 0.1;  //optimal fiber length
   public static double DEFAULT_MAX_FORCE = 1000;  //max isometric force
   public static double DEFAULT_TENDON_SLACK_LENGTH = 0;  //Tendon slack length
   public static double DEFAULT_PENNATION_ANGLE = 0; //in degree
   public static double DEFAULT_DAMPING = 0.0;
   public static double DEFAULT_FORCE_SCALING = 1;

   protected double myOptFibreLength = DEFAULT_OPT_FIBRE_LENGTH; 
   protected double myMaxForce = DEFAULT_MAX_FORCE;
   protected double myTendonSlackLength = DEFAULT_TENDON_SLACK_LENGTH;
   protected double myPennationAngle = DEFAULT_PENNATION_ANGLE; //in degree
   protected double myDamping = DEFAULT_DAMPING;
   protected double myForceScaling = DEFAULT_FORCE_SCALING;

   protected PropertyMode myOptFibreLengthMode = PropertyMode.Inherited;
   protected PropertyMode myMaxForceMode = PropertyMode.Inherited;
   protected PropertyMode myTendonSlackLengthMode = PropertyMode.Inherited;
   protected PropertyMode myPennationAngleMode = PropertyMode.Inherited;
   protected PropertyMode myDampingMode = PropertyMode.Inherited;
   protected PropertyMode myForceScalingMode = PropertyMode.Inherited;

   //the force length curves are Hermite Splines created using the knots stored
   //in these two values
   protected static double[] activForceLengthKnotValues= {
      0.0, 0.0, 0.0,
      0.4441, 0.0, 0.0032,
      0.485499, 0.049428, 2.704085,
      0.604897, 0.468734, 3.621026,
      0.7083,   0.7802, 1.675,
      0.755184, 0.829206, 0.892252,
      0.884248, 0.942572, 0.869613,
      0.950937, 0.999124, 0.203163,
      0.974018, 1.0, 0.0,
      1.0, 1.0, 0.0,
      1.039163, 1.0, 0.0,
      1.048153, 1.0, -0.202392,
      1.076168, 0.979828, -0.913958,
      1.119493, 0.930727, -1.363428,
      1.332344, 0.640401, -1.414611,
      1.696577, 0.132416, -1.405918,
      1.8123, 0.0, -0.035,
      2.0, 0.0, 0.0};
   
   protected static double[] passivForceLengthKnotValues= {
      1.0, 0.0, 0.0,
      1.207, 0.05303, 0.3944,
      1.398, 0.25762, 1.62926,
      1.4984, 0.448, 2.2334,
      1.5876, 0.6763, 2.7346,
      1.6574, 0.8669, 2.7756,
      1.7, 1.0, 2.7577};

   public static CubicHermiteSpline1d activForceLengthCurve =
      new CubicHermiteSpline1d (activForceLengthKnotValues);

   public static CubicHermiteSpline1d passivForceLengthCurve =
      new CubicHermiteSpline1d (passivForceLengthKnotValues);

   public Hill3ElemMuscleRigidTendon() {
   }

   public Hill3ElemMuscleRigidTendon(
      double maxIsoForce, double optimalFibreLen,
      double tendonSlackLen, double penAngle /*Degree*/) {

      setMaxForce (maxIsoForce);
      setOptFibreLength (optimalFibreLen);
      setTendonSlackLength (tendonSlackLen);
      setPennationAngle (penAngle);
   }

   public static PropertyList myProps = new PropertyList (
      Hill3ElemMuscleRigidTendon.class, AxialMaterial.class);

   static {
      myProps.addInheritable (
         "optFibreLength", "optimal length", DEFAULT_OPT_FIBRE_LENGTH);
      myProps.addInheritable (
         "maxForce",
         "maximum force, PCSA*unit tension of musxle, exp: PCSA(cm2)*60(N/cm2))",
         DEFAULT_MAX_FORCE);
      myProps.addInheritable (
         "tendonSlackLength", "tendon slack length", DEFAULT_TENDON_SLACK_LENGTH);
      myProps.addInheritable (
         "pennationAngle", "Pennation angle", DEFAULT_PENNATION_ANGLE);
      myProps.addInheritable (
         "damping", "linear damping", DEFAULT_DAMPING);
      myProps.addInheritable (
         "forceScaling", "force scaling", DEFAULT_FORCE_SCALING);
   }

   @Override
   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public double getOptFibreLength() {
      return myOptFibreLength;
   }

   public synchronized void setOptFibreLength(double optL) {
      myOptFibreLength = optL;
      myOptFibreLengthMode =
         PropertyUtils.propagateValue(
            this, "optFibreLength", myOptFibreLength, myOptFibreLengthMode);
      notifyHostOfPropertyChange();
   }

   public PropertyMode getOptFibreLengthMode() {
      return myOptFibreLengthMode;
   }

   public void setOptFibreLengthMode(PropertyMode mode) {
      myOptFibreLengthMode =
         PropertyUtils.setModeAndUpdate(
            this, "optFibreLength", myOptFibreLengthMode, mode);
   }

   public double getMaxForce() {
      return myMaxForce;
   }

   public synchronized void setMaxForce(double maxF) {
      myMaxForce = maxF;
      myMaxForceMode =
         PropertyUtils.propagateValue(
            this, "maxForce", myMaxForce, myMaxForceMode);
      notifyHostOfPropertyChange();
   }

   public PropertyMode getMaxForceMode() {
      return myMaxForceMode;
   }

   public void setMaxForceMode(PropertyMode mode) {
      myMaxForceMode =
         PropertyUtils.setModeAndUpdate(
            this, "maxForce", myMaxForceMode, mode);
   }

   public double getTendonSlackLength() {
      return myTendonSlackLength;
   }

   public synchronized void setTendonSlackLength(double maxF) {
      myTendonSlackLength = maxF;
      myTendonSlackLengthMode =
         PropertyUtils.propagateValue(
            this, "tendonSlackLength",
            myTendonSlackLength, myTendonSlackLengthMode);
      notifyHostOfPropertyChange();
   }

   public PropertyMode getTendonSlackLengthMode() {
      return myTendonSlackLengthMode;
   }

   public void setTendonSlackLengthMode(PropertyMode mode) {
      myTendonSlackLengthMode =
         PropertyUtils.setModeAndUpdate(
            this, "tendonSlackLength",
            myTendonSlackLengthMode, mode);
   }

   public double getPennationAngle() {
      return myPennationAngle;
   }

   public synchronized void setPennationAngle(double maxF) {
      myPennationAngle = maxF;
      myPennationAngleMode =
         PropertyUtils.propagateValue(
            this, "pennationAngle", myPennationAngle, myPennationAngleMode);
      notifyHostOfPropertyChange();
   }

   public PropertyMode getPennationAngleMode() {
      return myPennationAngleMode;
   }

   public void setPennationAngleMode(PropertyMode mode) {
      myPennationAngleMode =
         PropertyUtils.setModeAndUpdate(
            this, "pennationAngle", myPennationAngleMode, mode);
   }

   public double getDamping() {
      return myDamping;
   }

   public synchronized void setDamping(double d) {
      myDamping = d;
      myDampingMode =
         PropertyUtils.propagateValue(
            this, "damping", myDamping, myDampingMode);
      notifyHostOfPropertyChange();
   }

   public PropertyMode getDampingMode() {
      return myDampingMode;
   }

   public void setDampingMode(PropertyMode mode) {
      myDampingMode =
         PropertyUtils.setModeAndUpdate(
            this, "damping", myDampingMode, mode);
   }

   public double getForceScaling() {
      return myForceScaling;
   }

   public synchronized void setForceScaling(double fScaling) {
      myForceScaling = fScaling;
      myForceScalingMode =
         PropertyUtils.propagateValue(
            this, "forceScaling", myForceScaling, myForceScalingMode);
      notifyHostOfPropertyChange();
   }

   public PropertyMode getForceScalingMode() {
      return myForceScalingMode;
   }

   public void setForceScalingMode(PropertyMode mode) {
      myForceScalingMode =
         PropertyUtils.setModeAndUpdate(
            this, "forceScaling", myForceScalingMode, mode);
   }

   public double computeF (
      double l, double ldot, double l0, double excitation) {

      double L = myOptFibreLength;
      double h = L*Math.sin(Math.toRadians (myPennationAngle));
      double t = h/(l-myTendonSlackLength);        // tangent pen angle
      double c = 1/Math.sqrt(1+t*t);               // cosine  pen angle
      double s = c*t;                              // sine pen angle
      double n = (l-myTendonSlackLength)/(c*L);    // normalized fibre length

      double fa = activForceLengthCurve.evalY (n); // active force
      double fp = passivForceLengthCurve.evalY (n);// passive force

      double F =
         myForceScaling*c*(myMaxForce*(excitation*fa + fp) + myDamping*ldot);

      if (F < 0) {
         F = 0;
      }
      return F;
   }

   public double computeDFdl (
      double l, double ldot, double l0, double excitation) {

      double L = myOptFibreLength;
      double h = L*Math.sin(Math.toRadians (myPennationAngle));
      double t = h/(l-myTendonSlackLength);        // tangent pen angle
      double c = 1/Math.sqrt(1+t*t);               // cosine  pen angle
      double s = c*t;                              // sine pen angle
      double n = (l-myTendonSlackLength)/(c*L);    // normalized fibre length

      double fa = activForceLengthCurve.evalY (n); // active force
      double fp = passivForceLengthCurve.evalY (n);// passive force

      double dcdl = s*s/(n*L);                     // derivative of c
      double dndl = c/L;                           // derivative of n
      double dfadl = activForceLengthCurve.evalDy(n)*dndl;  // derivative of fa
      double dfpdl = passivForceLengthCurve.evalDy(n)*dndl; // derivative of fp

      double dFdl = myForceScaling*(
         dcdl*(myMaxForce*(excitation*fa + fp) + myDamping*ldot) +
         c*(myMaxForce*(excitation*dfadl + dfpdl)));

      return dFdl;
   }

   public double computeDFdldot (
      double l, double ldot, double l0, double excitation) {

      double h = myOptFibreLength*Math.sin(Math.toRadians (myPennationAngle));
      double t = h/(l-myTendonSlackLength);         // tangent pen angle
      double c = 1/Math.sqrt(1+t*t);                // cosine  pen angle

      return myForceScaling*c*myDamping;
   }


   public boolean isDFdldotZero () {
      return myDamping == 0;
   }


   public boolean equals(AxialMaterial mat) {
      if (!(mat instanceof Hill3ElemMuscleRigidTendon)) {
         return false;
      }
      Hill3ElemMuscleRigidTendon hillm = (Hill3ElemMuscleRigidTendon)mat;
      if (myMaxForce != hillm.myMaxForce ||
          myOptFibreLength != hillm.myOptFibreLength ||
          myPennationAngle != hillm.myPennationAngle ||
          myDamping != hillm.myDamping ||
          myTendonSlackLength != hillm.myTendonSlackLength||
          myForceScaling != hillm.myForceScaling) {
         return false;
      }
      else {
         return super.equals(mat);
      }
   }

   public Hill3ElemMuscleRigidTendon clone() {
      Hill3ElemMuscleRigidTendon mat = (Hill3ElemMuscleRigidTendon)super.clone();
      return mat;
   }

   public void scaleDistance(double s) {
      super.scaleDistance(s);
      myMaxForce *= s;
      myTendonSlackLength *= s;
      myOptFibreLength *= s;
   }

   public void scaleMass(double s) {
      super.scaleMass(s);
      myMaxForce *= s;
      myDamping *= s;
   }

}
