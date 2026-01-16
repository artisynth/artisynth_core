package artisynth.core.opensim.components;

import java.io.*;

import artisynth.core.modelbase.ModelComponent;
import artisynth.core.mechmodels.FrameMarker;
import artisynth.core.mechmodels.Point;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.materials.SimpleAxialMuscle;
import maspack.matrix.*;
import maspack.render.Renderer.LineStyle;
import maspack.render.RenderProps;

public class PointToPointActuator extends ForceBase {

   String bodyA;
   String bodyB;
   Point3d pointA;
   Point3d pointB;
   boolean points_are_global;
   double optimal_force;
   double min_control;
   double max_control;

   public PointToPointActuator() {
   }

   RigidBody findRigidBody (String bodyName, ModelComponentMap componentMap) {
      // find the body
      PhysicalFrame body = componentMap.findObjectByPathOrName (
         Body.class, this, bodyName);
      if (body == null) {
         System.err.println (
            "OpenSimParser: failed to find body " + bodyName);
         return null;
      }
      RigidBody rb = (RigidBody)componentMap.get (body);
      if (rb == null) {
         System.err.println(
            "OpenSimParser: failed to rigid body for " + bodyName);
         return null;
      }
      return rb;
   }

   Point createPoint (
      String bodyName, Point3d pos, ModelComponentMap componentMap) {
      if (bodyName != null) {
         // find the body
         PhysicalFrame body = componentMap.findObjectByPathOrName (
            Body.class, this, bodyName);
         if (body == null) {
            System.err.println (
               "OpenSimParser: failed to find body " + bodyName);
            return null;
         }
         RigidBody rb = (RigidBody)componentMap.get (body);
         if (rb == null) {
            System.err.println(
               "OpenSimParser: failed to rigid body for " + bodyName);
            return null;
         }
         FrameMarker fm = new FrameMarker();
         fm.setFrame (rb);
         Point3d loc = new Point3d(pos);
         if (points_are_global) {
            loc.inverseTransform (rb.getPose());
         }
         fm.setLocation (loc);
         return fm;
      }
      else {
         return new Point (pos);         
      }
   }

   public TwoPointActuator createComponent (
      File geometryPath, ModelComponentMap componentMap) {

      String compName = "PointToPointActuator " + getName();
      if (pointA == null || pointB == null) {
         System.err.println(
            "OpenSimParser: both points not specified for " + compName +
            "; ignoring");
         return null;
      }

      RigidBody rigidBodyA = null;
      Point3d locA = new Point3d (pointA);
      RigidBody rigidBodyB = null;
      Point3d locB = new Point3d (pointB);

      if (bodyA != null) {
         if ((rigidBodyA=findRigidBody (bodyA, componentMap)) == null) {
            System.err.println ("OpenSimParser: can't create " + compName);
            return null;
         }
      }
      if (bodyB != null) {
         if ((rigidBodyB=findRigidBody (bodyB, componentMap)) == null) {
            System.err.println ("OpenSimParser: can't create " + compName);
            return null;
         }
      }
      TwoPointActuator actuator = new TwoPointActuator (
         getName(), rigidBodyA, locA, rigidBodyB, locB);
      RenderProps.setLineStyle (actuator, LineStyle.CYLINDER);
      actuator.setForceScale (optimal_force);
      return null;
      //return actuator;
   }

}
      
