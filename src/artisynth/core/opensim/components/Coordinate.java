package artisynth.core.opensim.components;

import java.io.*;
import artisynth.core.modelbase.ScanWriteUtils;
import maspack.util.*;

public class Coordinate extends OpenSimObject implements Scannable {

   private static final double INF = Double.POSITIVE_INFINITY;

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
         out.setRange (new DoubleInterval(range));
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

   private boolean scanAttributeName (
      ReaderTokenizer rtok, String name) throws IOException {
      return ScanWriteUtils.scanAttributeName (rtok, name);
   }

   public void scan (ReaderTokenizer rtok, Object ref) throws IOException {
      rtok.scanToken ('[');
      while (rtok.nextToken() != ']') {
         if (scanAttributeName (rtok, "name")) {
            setName (rtok.scanQuotedString ('"'));
         }
         else if (scanAttributeName (rtok, "motion_type")) {
            motion_type = rtok.scanEnum (MotionType.class);
         }
         else if (scanAttributeName (rtok, "default_value")) {
            default_value = rtok.scanNumber();
         }
         else if (scanAttributeName (rtok, "range")) {
            range.scan (rtok, ref);
         }
         else if (scanAttributeName (rtok, "clamped")) {
            clamped = rtok.scanBoolean();
         }
         else if (scanAttributeName (rtok, "locked")) {
            locked = rtok.scanBoolean();
         }
         else {
            throw new IOException (
               "Expecting attribute name, got "+rtok);
         }
      }
   }

   public void write (PrintWriter pw, NumberFormat fmt, Object ref)
      throws IOException {

      pw.println ("[ name=\"" + getName() + "\"");
      IndentingPrintWriter.addIndentation (pw, 2);
      if (motion_type != MotionType.ROTATIONAL) {
         pw.println ("motion_type=" + motion_type);
      }
      if (default_value != 0) {
         pw.println ("default_value=" + fmt.format(default_value));
      }
      if (!range.equals (new DoubleInterval(-INF, INF))) {
         pw.print ("range=");
         range.write (pw, fmt, ref);
      }
      if (clamped) {
         pw.println ("clamped=" + clamped);
      }
      if (locked) {
         pw.println ("locked=" + locked);
      }
      IndentingPrintWriter.addIndentation (pw, -2);
      pw.println ("]");
   }

   public boolean isWritable() {
      return true;
   }
   
}
