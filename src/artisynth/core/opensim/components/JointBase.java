package artisynth.core.opensim.components;

import java.io.File;
import java.util.ArrayList;

import artisynth.core.mechmodels.RigidBody;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.RenderableComponentList;
import maspack.matrix.AxisAngle;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;

public abstract class JointBase extends OpenSimObject implements ModelComponentGenerator<ModelComponent>{

   public static class BodyAndTransform {
      OpenSimObject body;
      RigidTransform3d transform;
      
      public BodyAndTransform(OpenSimObject body, RigidTransform3d transform) {
         this.body = body;
         this.transform = transform;
      }
      
      public BodyAndTransform() {
         body = null;
         transform = new RigidTransform3d();
      }
   }
   
   // OpenSim 3.0
   private String parent_body;
   private Point3d location_in_parent;
   private AxisAngle orientation_in_parent;
   private Point3d location;
   private AxisAngle orientation;
   private boolean reverse;  // whether the joint transform defines parent->child or child->parent
   private CoordinateSet coordinateSet;
   
   // OpenSim 4.0
   private CoordinateList coordinates;  // OpenSim 4.0 list of coordinates
   private FrameList frames;
   private String socket_parent_frame;
   private String socket_child_frame;
   

   public JointBase() {
      parent_body = null;
      location_in_parent = null;
      orientation_in_parent  = null;
      location = null;
      orientation = null;
      reverse = false;
      coordinateSet = null;
      coordinates = null;
      frames = null;
      socket_parent_frame = null;
      socket_child_frame = null;
   }
   
   public void setParentBody(String body) {
      parent_body = body;
   }
   
   public String getParentBody() {
      return parent_body;
   }
   
   public void setLocationInParent(Point3d loc) {
      location_in_parent = loc;
   }
   
   public Point3d getLocationInParent() {
      return location_in_parent;
   }
   
   public void setOrientationInParent(AxisAngle orient) {
      orientation_in_parent = orient;
   }
   
   public AxisAngle getOrientationInParent() {
      return orientation_in_parent;
   }
   
   /**
    * Joint pose relative to parent body (OpenSim 3)
    * @return joint pose
    */
   public RigidTransform3d getJointTransformInParent() {
      Point3d loc = location_in_parent;
      if (loc == null) {
         loc = Point3d.ZERO;
      }
      
      AxisAngle orient = orientation_in_parent;
      if (orient == null) {
         orient = AxisAngle.IDENTITY;
      }
      return new RigidTransform3d(loc, orient);
   }
   
   private BodyAndTransform findBodyAndTransform(String path, ModelComponentMap componentMap) {
      BodyAndTransform out = new BodyAndTransform ();
      
      OpenSimObject obj = componentMap.findObjectByPath (this, path);
      while (obj != null) {
         
         if (obj instanceof Body || obj instanceof Ground) {
            out.body = obj;
            return out;
         }
         
         if (obj instanceof PhysicalOffsetFrame) {
            PhysicalOffsetFrame pof = (PhysicalOffsetFrame)obj;
            out.transform.mul(pof.getOffsetTransform(), out.transform);
            obj = componentMap.findObjectByPath (pof, pof.socket_parent);
         } else {
            System.out.println ("Don't know how to backtrack from " + obj.getClass () + " " + obj.getName ());
            return null;
         }
      }
      
      return out;
   }
   
   public BodyAndTransform findParentBodyAndTransform(ModelComponentMap componentMap) {
      return findBodyAndTransform (socket_parent_frame, componentMap);
   }
   
   public BodyAndTransform findChildBodyAndTransform(ModelComponentMap componentMap) {
      return findBodyAndTransform (socket_child_frame, componentMap);
   }
   
   public void setLocation(Point3d loc) {
      location = loc;
   }
   
   public Point3d getLocation() {
      return location;
   }
   
   /**
    * Joint pose relative to child body
    * @return joint pose
    */
   public RigidTransform3d getJointTransformInChild() {
      Point3d loc = location;
      if (loc == null) {
         loc = Point3d.ZERO;
      }
      
      AxisAngle orient = orientation;
      if (orient == null) {
         orient = AxisAngle.IDENTITY;
      }
      return new RigidTransform3d(loc, orient);
   }
   
   public void setOrientation(AxisAngle orient) {
      orientation = orient;
   }
   
   public AxisAngle getOrientation() {
      return orientation;
   }
   
   public void setReverse(boolean set) {
      reverse = set;
   }
   
   public boolean getReverse() {
      return reverse;
   }
   
   /**
    * Works for both CoordinateSet (OpenSim 3) and
    * CoordinateList (OpenSim 4)
    * @return array of coordinates
    */
   public ArrayList<Coordinate> getCoordinateArray() {
      if (coordinates != null) {
         return coordinates.objects ();
      }
      return coordinateSet.objects ();
   }
   
   public CoordinateSet getCoordinateSet() {
      return coordinateSet;
   }
   
   public void setCoordinateSet(CoordinateSet cs) {
      coordinateSet = cs;
      coordinateSet.setParent (this);
   }
   
   public CoordinateList getCoordinates() {
      return coordinates;
   }
   
   public void setCoordinates(CoordinateList coords) {
      coordinates = coords;
      coordinates.setParent (this);
   }
   
   public FrameList getFrames() {
      return frames;
   }
   
   public void setFrames(FrameList frames) {
      this.frames = frames;
      this.frames.setParent (this);
   }
   
   public String getSocketParentFrame() {
      return socket_parent_frame;
   }
   
   public void setSocketParentFrame(String path) {
      socket_parent_frame = path;
   }
   
   public String getSocketChildFrame() {
      return socket_child_frame;
   }
   
   public void setSocketChildFrame(String path) {
      socket_child_frame = path;
   }
   
   @Override
   public JointBase clone () {
      
      JointBase jb = (JointBase)super.clone ();
      
      jb.setParentBody (parent_body);
      if (location_in_parent != null) {
         jb.setLocationInParent (location_in_parent.clone ());
      }
      if (orientation_in_parent != null) {
         jb.setOrientationInParent (orientation_in_parent.clone());
      }
      if (location != null) {
         jb.setLocation (location.clone ());
      }
      if (orientation != null) {
         jb.setOrientation (orientation.clone());
      }
      jb.setReverse (reverse);
      if (coordinateSet != null) {
         jb.setCoordinateSet (coordinateSet.clone ());
      }
      
      if (coordinates != null) {
         jb.setCoordinates (coordinates.clone ());
      }
      
      if (frames != null) {
         jb.setFrames (frames.clone ());
      }
      
      return jb;
   }
   
   protected abstract artisynth.core.mechmodels.JointBase createJoint(RigidBody parent,
      RigidTransform3d TJP, RigidBody child, RigidTransform3d TJC);
   
   @Override
   public ModelComponent createComponent (
      File geometryPath, ModelComponentMap componentMap) {
      
      // holder for joint and frames
      // RenderableComponentList<ModelComponent> jointRoot = new RenderableComponentList<>(ModelComponent.class, getName());
      // componentMap.put (this, jointRoot);
      
      // create frames first in case referred to within joint
      FrameList frames = getFrames ();
      if (frames != null) {
         // TODO: figure out why this doesn't work
         //         RenderableComponentList<RigidBody> jframes = new RenderableComponentList<>(RigidBody.class, "frames");
         //         jointRoot.add (jframes);
         //         for (Frame frame : frames) {
         //            RigidBody f = frame.createComponent (geometryPath, componentMap);
         //            jframes.add (f);
         //         }
         
         // Alternate hack: add null frames so we can find offsets later
         for (Frame frame : frames) {
            componentMap.put(frame, null);
         }
      }
      
      // OpenSim 4.0
      RigidBody parentRB = null;
      RigidBody childRB = null;
      RigidTransform3d TJP = null;  // pose of joint in parent
      RigidTransform3d TJC = null;  // pose of joint in child
      
      String socket_parent_frame = getSocketParentFrame ();
      if (socket_parent_frame != null) {
         String socket_child_frame = getSocketChildFrame ();
         
         // TODO: fix this, doesn't seem to work to attach directly to PhysicalOffsetFrame
         //         OpenSimObject parentFrame = componentMap.findObjectByPath (this, socket_parent_frame);
         //         parentRB = (RigidBody)componentMap.get (parentFrame);
         //         TJP = RigidTransform3d.IDENTITY;
         //         
         //         OpenSimObject childFrame = componentMap.findObjectByPath (this, socket_child_frame);
         //         childRB = (RigidBody)componentMap.get (childFrame);
         //         TJC = RigidTransform3d.IDENTITY;
         
         // HACK: find first connected body and relative transform
         BodyAndTransform parent = findParentBodyAndTransform (componentMap);
         BodyAndTransform child = findChildBodyAndTransform (componentMap);
         
         parentRB = (RigidBody)componentMap.get (parent.body);
         TJP = parent.transform;
         childRB = (RigidBody)componentMap.get (child.body);
         TJC = child.transform;
         
      } else {
         
         // OpenSim 3.0
   
         // find parent and child
         Body parentBody = componentMap.findObjectByName (Body.class, getParentBody ());
         parentRB = (RigidBody)componentMap.get (parentBody);
         
         OpenSimObject parent = getParent ();
         if (parent instanceof Joint) {
            OpenSimObject grandParent = parent.getParent ();
            if (grandParent instanceof Body) {
               childRB = (RigidBody)componentMap.get (grandParent);     
            }
         }
         
         if (reverse) {
            RigidBody tmp = parentRB;
            parentRB = childRB;
            childRB = tmp;
            TJP = getJointTransformInChild ();
            TJC = getJointTransformInParent ();
         } else {
            TJP = getJointTransformInParent ();
            TJC = getJointTransformInChild ();
         }
      }
      
      if (parentRB == null || childRB == null) {
         return null;
      }
      
      // create and add joint
      artisynth.core.mechmodels.JointBase joint = createJoint (parentRB, TJP, childRB, TJC);  
      // jointRoot.add (joint);
      
      // return jointRoot;
      return joint;
   }

}
