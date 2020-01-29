package artisynth.core.opensim.components;

import maspack.util.DoubleInterval;

public class Coordinate extends OpenSimObject {

   private MotionType motion_type;
   private double default_value;
   private DoubleInterval range;
   private double default_speed_value;
   boolean clamped;
   boolean locked;
   FunctionBase prescribed_function;
   boolean prescribed;
   boolean is_free_to_satisfy_constraints;
   
   public static enum MotionType {
      TRANSLATIONAL,
      ROTATIONAL,
      COUPLED
   }
   
   public Coordinate() {
      motion_type = MotionType.ROTATIONAL;
      range = new DoubleInterval ();
      default_value = 0;
      default_speed_value = 0;
      clamped = false;
      locked = false;
      prescribed_function = null;
      prescribed = false;
      is_free_to_satisfy_constraints = true;
   }
   
   public void setRange(DoubleInterval range) {
      this.range = range;
   }
   
   public void setRange (double minValue, double maxValue) {
      range.set (minValue, maxValue);
   }
   
   public DoubleInterval getRange() {
      return range;
   }
   
   public void setDefaultValue(double val) {
      default_value = val;
   }
   
   public double getDefaultValue() {
      return default_value;
   }
   
   public void setDefaultSpeedValue(double val) {
      default_speed_value = val;
   }
   
   public double getDefaultSpeedValue() {
      return default_speed_value;
   }
   
   public void setMotionType(String type) {
      type = type.trim ();
      if ("rotational".equalsIgnoreCase (type)) {
         setMotionType(MotionType.ROTATIONAL);
      } else if ("translational".equalsIgnoreCase (type)) {
         setMotionType(MotionType.TRANSLATIONAL);
      } else if ("coupled".equalsIgnoreCase (type)) {
         setMotionType (MotionType.COUPLED);
      } else {
         // unknown
         System.out.println("Unknown motion type: " + type);
         setMotionType((MotionType)null);
      }
   }
   
   public void setMotionType(MotionType type) {
      motion_type = type;
   }
   
   public MotionType getMotionType() {
      return motion_type;
   }
   
   public boolean getClamped() {
      return clamped;
   }
   
   public void setClamped(boolean set) {
      clamped = set;
   }
   
   public boolean getLocked() {
      return locked;
   }
   
   public void setLocked(boolean set) {
      locked = set;
   }
   
   public boolean getPrescribed() {
      return prescribed;
   }
   
   public void setPrescribed(boolean set) {
      prescribed = set;
   }
   
   public void setPrescribedFunction(FunctionBase f) {
      prescribed_function = f;
      prescribed_function.setParent (this);
   }
   
   public FunctionBase getPrescribedFunction() {
      return prescribed_function;
   }
   
   public boolean isFreeToSatisfyConstraints() {
      return is_free_to_satisfy_constraints;
   }
   
   public void setFreeToSatisfyConstraints(boolean set) {
      is_free_to_satisfy_constraints = set;
   }
   
   @Override
   public Coordinate clone () {
      
      Coordinate out = (Coordinate)super.clone ();
      out.setMotionType (motion_type);
      out.setDefaultValue (default_value);
      if (range != null) {
         out.setRange (range);
      }
      out.setDefaultSpeedValue (default_speed_value);
      out.setClamped (clamped);
      out.setLocked (locked);
      if (prescribed_function != null) {
         out.setPrescribedFunction (prescribed_function.clone ());
      }
      out.setPrescribed (prescribed);
      
      return out;
   }
   
}