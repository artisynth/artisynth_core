package artisynth.core.opensim.components;

import maspack.matrix.Point3d;

public class PathPoint extends HasVisibleObjectOrAppearance {
   
   private String body;                 // body name only (OpenSim 3.0)
   private String socket_parent_frame;  // full body path (OpenSim 4.0)
   private Point3d location;
   
   public PathPoint() {
      body = null;
      socket_parent_frame = null;
      location = new Point3d();
   }
   
   public void setBody(String name) {
      body = name;
   }
   
   public String getBodyOrSocketParentFrame() {
      if (socket_parent_frame != null) {
         return socket_parent_frame;
      } 
      return body;
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
  
   public void setLocation(Point3d position) {
      location = new Point3d(position);
   }
   
   public Point3d getLocation() {
      return location;
   }
   
   public String toString() {
      String ret = body+":["+location.x+", "+location.y+", "+location.z+"]";
      return ret;
   }
   
   @Override
   public PathPoint clone () {
      PathPoint pp = (PathPoint)super.clone ();
      pp.setBody (body);
      pp.setSocketParentFrame (socket_parent_frame);
      if (location != null) {
         pp.setLocation (location.clone ());
      }
      return pp;
   }
}
