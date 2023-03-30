package artisynth.core.opensim.components;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import artisynth.core.mechmodels.FrameMarker;
import artisynth.core.mechmodels.MultiPointSpring;
import artisynth.core.mechmodels.PointList;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.RenderableComponentList;
import maspack.matrix.Point3d;
import maspack.render.RenderProps;

public class Marker extends HasVisibleObjectOrAppearance 
   implements ModelComponentGenerator<ModelComponent> {
   
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
   
   @Override
   public FrameMarker createComponent (
      File geometryPath, ModelComponentMap componentMap) {
     
      String bodyOrSocketParentFrame = getBodyOrSocketParentFrame ();

      // get rigid body
      PhysicalFrame body = componentMap.findObjectByPathOrName (
         Body.class, this, bodyOrSocketParentFrame);
      if (body == null) { // try ground
         body = componentMap.findObjectByPathOrName (
            Ground.class, this, bodyOrSocketParentFrame);
      }
      RigidBody rb = (RigidBody)componentMap.get (body);
      if (rb == null) {
         System.err.println (
            "Failed to find marker body " + bodyOrSocketParentFrame);
         return null;
      }

      // add frame marker
      getName();
      String markerName = getName().replaceAll("[^a-zA-Z0-9]", "_");  
      FrameMarker fm = new FrameMarker (markerName);
      fm.setFrame (rb);
      fm.setLocation (location);
      // XXX deal with duplicate names

      return fm;
   }
   
   public String getBodyOrSocketParentFrame() {
      if (socket_parent_frame != null) {
         return socket_parent_frame;
      } 
      return body;
   }
   
}
