package artisynth.core.opensim.components;

import maspack.util.DoubleInterval;

public class ConditionalPathPoint extends PathPoint {

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
