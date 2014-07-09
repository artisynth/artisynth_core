/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import maspack.collision.ContactInfo;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.properties.PropertyList;
import maspack.render.GLRenderable;
import maspack.render.GLRenderer;
import maspack.render.HasRenderProps;
import maspack.render.RenderProps;

public abstract class CollisionHandler extends ConstrainerBase 
   implements HasRenderProps, GLRenderable {

   public static boolean useSignedDistanceCollider = false;
   
   public static boolean doBodyFaceContact = false;
   public static boolean reduceConstraints = false;
   public static boolean computeTimings = false;

   public static PropertyList myProps =
      new PropertyList (CollisionHandler.class, ConstrainerBase.class);

   static private RenderProps defaultRenderProps = new RenderProps();

   static {
      myProps.add (
         "renderProps * *", "render properties for this constraint",
         defaultRenderProps);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public abstract Collidable getCollidable(int idx);
   public abstract Collidable getCollidable0();
   public abstract Collidable getCollidable1();
   
   abstract void setLastContactInfo(ContactInfo info);
   abstract ContactInfo getLastContactInfo();
   
   abstract double getContactNormalLen();
   
   abstract void addLineSegment (Point3d p, Vector3d normal, double nrmlLen);
   abstract void clearLineSegments();
   
   public abstract double getCompliance();
   public abstract void setCompliance (double c);
   public abstract void setDamping (double d);
   public abstract double getDamping();
   
   public abstract double getPenetrationTol();
   public abstract void setPenetrationTol(double tol);
   
   public abstract double getRigidPointTolerance();
   public abstract void setRigidPointTolerance(double tol);
   public abstract double getRigidRegionTolerance();
   public abstract void setRigidRegionTolerance(double tol);
   
   public abstract void setFriction(double mu);
   public abstract double getFriction();
   
   public abstract void autoComputeCompliance (double collisionAccel, double penTol);
   
   abstract boolean isRigidBodyPair();
   
   abstract public void render (GLRenderer renderer, RenderProps props, int flags);
   
   abstract RigidBodyContact getRigidBodyContact();
   
   abstract void initialize();
   
   /**
    * Checks if this collision handler has any active contact constraints
    */
   public abstract boolean hasActiveContacts();
   
   // Rendering
   public abstract void setDrawIntersectionContours(boolean set);
   public abstract boolean isDrawIntersectionContours();
   
   public abstract void setDrawIntersectionFaces(boolean set);
   public abstract boolean isDrawIntersectionFaces();

   public abstract void setDrawIntersectionPoints(boolean set);
   public abstract boolean isDrawIntersectionPoints();
   
}
