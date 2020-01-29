package artisynth.core.opensim.components;

import maspack.matrix.Point3d;

public class PathPoint extends HasVisibleObject {
   private String body;
   private Point3d location;
   
   public PathPoint() {
      body = null;
      location = new Point3d();
   }
   
   public void setBody(String name) {
      body = name;
   }
   
   public void setLocation(Point3d position) {
      location = new Point3d(position);
   }
 
   public String getBody() {
      return body;
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
      if (location != null) {
         pp.setLocation (location.clone ());
      }
      return pp;
   }
}
