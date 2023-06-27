package artisynth.core.opensim.components;

import java.io.*;
import artisynth.core.mechmodels.*;
import maspack.spatialmotion.RigidBodyConstraint.MotionType;
import maspack.util.DoubleInterval;

public class ConditionalPathPoint extends PathPoint {

   private static final double RTOD = 180/Math.PI;
   private static final double DTOR = Math.PI/180;

   DoubleInterval range;
   String coordinate;
   
   public ConditionalPathPoint() {
      range = new DoubleInterval();
      coordinate = null;
   }
   
   public void setRange(double min, double max) {
      range.set (min, max);
   }
   
   public void setRange(DoubleInterval range) {
      this.range.set (range);
   }
   
   public DoubleInterval getRange() {
      return range;
   }
   
   public String getCoordinate() {
      return coordinate;
   }
   
   public void setCoordinate(String coord) {
      coordinate = coord;
   }

   public JointConditionalMarker createComponent (
      File geometryPath, ModelComponentMap componentMap) {

      JointCoordinateHandle ch =
         CoordinateHandle.createJCH (coordinate, this, componentMap);
      if (ch == null) {
         return null;
      }
      DoubleInterval mkrRange = range;
      if (ch.getMotionType() == MotionType.ROTARY) {
         mkrRange = new DoubleInterval (
            RTOD*range.getLowerBound(), RTOD*range.getUpperBound());
      }
      return new JointConditionalMarker (ch, mkrRange);
   }
   
   @Override
   public ConditionalPathPoint clone () {
      ConditionalPathPoint cpp = (ConditionalPathPoint)super.clone ();
      if (range != null) {
         cpp.setRange (range.clone ());
      }
      cpp.setCoordinate (coordinate);
      return cpp;
   }

}
