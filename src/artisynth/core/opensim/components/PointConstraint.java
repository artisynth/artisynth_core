package artisynth.core.opensim.components;

import java.io.*;

import artisynth.core.modelbase.ModelComponent;
import artisynth.core.mechmodels.*;
import maspack.util.DoubleInterval;
import maspack.matrix.Point3d;
import maspack.matrix.*;
import maspack.util.*;

public class PointConstraint extends ConstraintBase {

   String body_1; // body name only (OpenSim 3)
   String socket_body_1; // path to frame (OpenSim 4)
   Point3d location_body_1;
   String body_2;
   String socket_body_2;
   Point3d location_body_2;

   public String getBody1() {
      return body_1;
   }

   public void setBody1 (String str) {
      body_1 = str;
   }

   public String getSocketBody1() {
      return socket_body_1;
   }

   public void setSocketBody1 (String str) {
      socket_body_1 = str;
   }

   public Point3d getLocationBody1() {
      return location_body_1;
   }

   public void setLocationBody1 (Point3d set) {
      location_body_1 = set;
   }

   public String getBody2() {
      return body_2;
   }

   public void setBody2 (String str) {
      body_2 = str;
   }

   public String getSocketBody2() {
      return socket_body_2;
   }

   public void setSocketBody2 (String str) {
      socket_body_2 = str;
   }

   public Point3d getLocationBody2() {
      return location_body_2;
   }

   public void setLocationBody2 (Point3d set) {
      location_body_2 = set;
   }
   
   private boolean isGround (RigidBody body) {
      return !body.isDynamic();
   }

   @Override
   public SphericalJoint createComponent (
      File geometryPath, ModelComponentMap componentMap) {

      RigidBody bodyA = findBody (1, componentMap);
      RigidBody bodyB = findBody (2, componentMap);

      if (isGround(bodyA) && isGround(bodyB)) {
         System.out.printf (
            "PointConstraint '%s': both bodies are groumd");
         return null;
      }

      RigidTransform3d TCA = new RigidTransform3d();
      TCA.p.set (location_body_1);
      RigidTransform3d TDB = new RigidTransform3d();
      TDB.p.set (location_body_2);
      if (isGround(bodyA)) {
         // swap bodies A and B and make B null
         bodyA = bodyB;
         bodyB = null;
         RigidTransform3d T = TCA; TCA = TDB; TDB = T; 
      }
      else if (isGround(bodyB)) {
         // set body B to null
         bodyB = null;
      }
      SphericalJoint jnt = new SphericalJoint (bodyA, TCA, bodyB, TDB);
      jnt.setName (getName());
      return jnt;
   }

   private RigidBody findBody (int num, ModelComponentMap componentMap) {
      String bodyOrSocketParentFrame = getBodyOrSocketParentFrame(num);
      PhysicalFrame frame = componentMap.findObjectByPathOrName (
         Body.class, this, bodyOrSocketParentFrame);
      if (frame == null) { // try ground
         frame = componentMap.findObjectByPathOrName (
            Ground.class, this, bodyOrSocketParentFrame);
      }
      RigidBody body = (RigidBody)componentMap.get (frame);
      if (body == null) {
         System.out.printf (
            "PointConstraint '%s': failed to find body %d '%s'\n", 
            getName(), num, bodyOrSocketParentFrame);
         return null;
      }     
      return body;
   }      

   private String getBodyOrSocketParentFrame (int num) {
      if (num == 1) {
         return body_1 != null ? body_1 : socket_body_1;
      }
      else if (num == 2) {
         return body_2 != null ? body_2 : socket_body_2;
      }
      else {
         throw new InternalErrorException (
            "Body number is "+num+"; must be 1 or 2");
      }
   }

   @Override
   public PointConstraint clone () {
      PointConstraint cpp =
         (PointConstraint)super.clone ();
      if (location_body_1 != null) {
         cpp.location_body_1 = new Point3d (location_body_1);
      }
      if (location_body_2 != null) {
         cpp.location_body_2 = new Point3d (location_body_2);
      }
      return cpp;
   }
}
