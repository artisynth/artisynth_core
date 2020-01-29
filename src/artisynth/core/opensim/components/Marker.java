package artisynth.core.opensim.components;

import maspack.matrix.Point3d;

public class Marker extends HasVisibleObject {
   
   private String body;
   private Point3d location;
   private boolean fixed;       //Is Marker Fixed
   
   public Marker () {
      body = null;
      location = null;
      fixed = false;
   }
     
   public void setBody(String body) {
      this.body = body;
   }
   
   public String getBody() {
      return body;
   }   
   
   public void setFixed(boolean state) {
      fixed = state;
   }
   
   public boolean isFixed() {
      return fixed;
   }
   
   public void setLocation(Point3d pos) {
      location = pos;
   }
   
   public Point3d getLocation() {
      return location;
   }
   
   public String toString() {
      String ret = body+":["+location.x+", "+location.y+", "+location.z+"]";
      return ret;
   }
  
   @Override
   public Marker clone () {
      Marker marker = (Marker)super.clone ();
      marker.setBody (body);
      if (location != null) {
         marker.setLocation (location.clone ());
      }
      marker.setFixed (fixed);
      return marker;
   }
   
}