package artisynth.core.materials;

import maspack.properties.PropertyList;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;

public abstract class AxialMuscleMaterial extends AxialMaterial {

   public static double DEFAULT_MAX_FORCE = 1.0;
   public static double DEFAULT_OPT_LENGTH = 0.0;
   public static double DEFAULT_MAX_LENGTH = 1.0;
   public static double DEFAULT_PASSIVE_FRACTION = 0.0;
   public static double DEFAULT_TENDON_RATIO = 0.0;
   public static double DEFAULT_DAMPING = 0;
   public static double DEFAULT_SCALING = 1000.0;

   protected double myMaxForce = DEFAULT_MAX_FORCE; // max active isometric force
   protected double myOptLength = DEFAULT_OPT_LENGTH; // length at zero force
   protected double myMaxLength = DEFAULT_MAX_LENGTH; // length at max force
   protected double myPassiveFraction = DEFAULT_PASSIVE_FRACTION; // max passive force expressed as fraction of maxForce
   protected double myTendonRatio = DEFAULT_TENDON_RATIO;
   protected double myDamping = DEFAULT_DAMPING; // damping
   protected double forceScaling = DEFAULT_SCALING;

   protected PropertyMode myMaxForceMode = PropertyMode.Inherited;
   protected PropertyMode myOptLengthMode = PropertyMode.Inherited;
   protected PropertyMode myMaxLengthMode = PropertyMode.Inherited;
   protected PropertyMode myPassiveFractionMode = PropertyMode.Inherited;
   protected PropertyMode myTendonRatioMode = PropertyMode.Inherited;
   protected PropertyMode myDampingMode = PropertyMode.Inherited;
   protected PropertyMode myForceScalingMode = PropertyMode.Inherited;

   // constant parameters
   // percentage of optimal fibre length
   public static final double maxStretch = 1.5;
   // percentage of optimal fibre length
   public static final double minStretch = 0.5;

   public static PropertyList myProps =
      new PropertyList(AxialMuscleMaterial.class, AxialMaterial.class);

   static {
      myProps.addInheritable(
         "maxForce", "maximum force", DEFAULT_MAX_FORCE);
      myProps.addInheritable(
         "passiveFraction", "passive fraction", DEFAULT_PASSIVE_FRACTION);
      myProps.addInheritable(
         "optLength", "optimal length", DEFAULT_OPT_LENGTH);
      myProps.addInheritable(
         "maxLength", "maximum length", DEFAULT_MAX_LENGTH);
      myProps.addInheritable(
         "tendonRatio", "tendon to fiber ratio", DEFAULT_TENDON_RATIO);
      myProps.addInheritable(
         "damping", "linear damping", DEFAULT_DAMPING);
      myProps.addInheritable(
         "forceScaling", "force scaling", DEFAULT_SCALING);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
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

   public double getPassiveFraction() {
      return myPassiveFraction;
   }

   public synchronized void setPassiveFraction(double passiveFraction) {
      myPassiveFraction = passiveFraction;
      myPassiveFractionMode =
         PropertyUtils.propagateValue(
            this, "passiveFraction", myPassiveFraction, myPassiveFractionMode);
      notifyHostOfPropertyChange();
   }

   public PropertyMode getPassiveFractionMode() {
      return myPassiveFractionMode;
   }

   public void setPassiveFractionMode(PropertyMode mode) {
      myPassiveFractionMode =
         PropertyUtils.setModeAndUpdate(
            this, "passiveFraction", myPassiveFractionMode, mode);
   }

   public double getOptLength() {
      return myOptLength;
   }

   public synchronized void setOptLength(double optL) {
      myOptLength = optL;
      myOptLengthMode =
         PropertyUtils.propagateValue(
            this, "optLength", myOptLength, myOptLengthMode);
      notifyHostOfPropertyChange();
   }

   public PropertyMode getOptLengthMode() {
      return myOptLengthMode;
   }

   public void setOptLengthMode(PropertyMode mode) {
      myOptLengthMode =
         PropertyUtils.setModeAndUpdate(
            this, "optLength", myOptLengthMode, mode);
   }

   public double getMaxLength() {
      return myMaxLength;
   }

   public synchronized void setMaxLength(double maxL) {
      myMaxLength = maxL;
      myMaxLengthMode =
         PropertyUtils.propagateValue(
            this, "maxLength", myMaxLength, myMaxLengthMode);
      notifyHostOfPropertyChange();
   }

   public PropertyMode getMaxLengthMode() {
      return myMaxLengthMode;
   }

   public void setMaxLengthMode(PropertyMode mode) {
      myMaxLengthMode =
         PropertyUtils.setModeAndUpdate(
            this, "maxLength", myMaxLengthMode, mode);
   }

   public double getTendonRatio() {
      return myTendonRatio;
   }

   public synchronized void setTendonRatio(double tendonRatio) {
      myTendonRatio = tendonRatio;
      myTendonRatioMode =
         PropertyUtils.propagateValue(
            this, "tendonRatio", myTendonRatio, myTendonRatioMode);
      notifyHostOfPropertyChange();
   }

   public PropertyMode getTendonRatioMode() {
      return myTendonRatioMode;
   }

   public void setTendonRatioMode(PropertyMode mode) {
      myTendonRatioMode =
         PropertyUtils.setModeAndUpdate(
            this, "tendonRatio", myTendonRatioMode, mode);
   }

   public double getForceScaling() {
      return forceScaling;
   }

   public synchronized void setForceScaling(double fScaling) {
      forceScaling = fScaling;
      myForceScalingMode =
         PropertyUtils.propagateValue(
            this, "forceScaling", forceScaling, myForceScalingMode);
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

   public AxialMuscleMaterial () {
   }

   public void setAxialMuscleMaterialProps(double maxF, double optL, 
      double maxL, double passiveFraction, double tendonRatio, 
      double damping, double forceScaling) {
      setMaxForce(maxF);
      setOptLength(optL);
      setMaxLength(maxL);
      setPassiveFraction(passiveFraction);
      setTendonRatio(tendonRatio);
      setDamping(damping);
      setForceScaling(forceScaling);
   }

   public boolean equals(AxialMaterial mat) {
      if (!(mat instanceof AxialMuscleMaterial)) {
         return false;
      }
      AxialMuscleMaterial linm = (AxialMuscleMaterial)mat;
      if (myMaxForce != linm.myMaxForce ||
         myOptLength != linm.myOptLength ||
         myMaxLength != linm.myMaxLength ||
         myDamping != linm.myDamping ||
         forceScaling != linm.forceScaling) {
         return false;
      }
      else {
         return super.equals(mat);
      }
   }

   public AxialMuscleMaterial clone() {
      AxialMuscleMaterial mat = (AxialMuscleMaterial)super.clone();
      return mat;
   }

   public void scaleDistance(double s) {
      super.scaleDistance(s);
      forceScaling *= s;
      myOptLength *= s;
      myMaxLength *= s;
   }

   public void scaleMass(double s) {
      super.scaleMass(s);
      forceScaling *= s;
      myDamping *= s;
   }

   static Class<?>[] mySubClasses = new Class<?>[] {
      ConstantAxialMuscle.class,
      LinearAxialMuscle.class,
      PeckAxialMuscle.class,
      PaiAxialMuscle.class,
      BlemkerAxialMuscle.class
   };

   public static Class<?>[] getSubClasses() {
      return mySubClasses;
   }
}
