package artisynth.core.opensim.components;

import maspack.matrix.Point3d;

public class Marker extends HasVisibleObjectOrAppearance {
   
   private String body;                 // body name only (OpenSim 3.0)
   private String socket_parent_frame;  // full body path (OpenSim 4.0)
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
   
   public void setSocketParentFrame(String path) {
      socket_parent_frame = path;
   }
   
   public String getSocketParentFrame() {
      return socket_parent_frame;
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
      String ret = null;
      if (socket_parent_frame != null) {
         ret = socket_parent_frame+":["+location.x+", "+location.y+", "+location.z+"]";
      } else {
         ret = body+":["+location.x+", "+location.y+", "+location.z+"]";
      }
      return ret;
   }
  
   @Override
   public Marker clone () {
      Marker marker = (Marker)super.clone ();
      marker.setBody (body);
      marker.setSocketParentFrame (socket_parent_frame);
      if (location != null) {
         marker.setLocation (location.clone ());
      }
      marker.setFixed (fixed);
      return marker;
   }
   
}