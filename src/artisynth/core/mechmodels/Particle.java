/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import artisynth.core.modelbase.*;
import maspack.geometry.GeometryTransformer;
import maspack.matrix.*;
import maspack.properties.PropertyList;
import maspack.util.*;
import artisynth.core.util.*;

import java.io.*;
import java.util.List;
import java.util.Map;

public class Particle extends Point implements PointAttachable {
   protected double myMass;
   protected double myEffectiveMass;

   public static PropertyList myProps =
      new PropertyList (Particle.class, Point.class);

   static {
      myProps.add ("mass * *", "particle's mass", 1.0);
      myProps.add (
         "dynamic isDynamic", "true if component is dynamic (non-parametric)",
         true);
      // myProps.get("mass").setAutoWrite(false);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

//   /**
//    * Returns the constraint for this particle, if any. The constraint is a
//    * one-DOF restriction on particle movement, usually imparted by surface
//    * contact.
//    * 
//    * @return constraint for this particle. Should not be modified by the
//    * caller.
//    */
//   public Vector3d getConstraint() {
//      return myConstraintSet ? myConstraint : null;
//   }
//
//   /**
//    * Sets a constraint for this particle. The specified constraint is copied
//    * internally, and passing a null value will cause the constraint to be
//    * cleared.
//    * 
//    * @param c
//    * constraint value.
//    */
//   public void setConstraint (Vector3d c) {
//      if (c != null) {
//         if (myConstraint == null) {
//            myConstraint = new Vector3d();
//         }
//         myConstraint.set (c);
//         myConstraintSet = true;
//      }
//      else {
//         myConstraintSet = false;
//      }
//   }

   public Particle() {
      this (1);
   }

   public Particle (double m) {
      super();
      setMass (m);
      setDynamic (true);
   }

   public Particle (double m, Point3d p) {
      this (m);
      setPosition (p);
   }

   public Particle (double m, double x, double y, double z) {
      this (m);
      setPosition (x, y, z);
   }

   public Particle (String name, double m, double x, double y, double z) {
      this (m);
      setName (name);
      setPosition (x, y, z);
   }

   public double getMass() {
      return myMass;
   }

   public double getMass (double t) {
      return getMass();
   }

   public void getMass (Matrix M, double t) {
      doGetMass (M, myMass);
   }
   
   public double getEffectiveMass() {
      return myEffectiveMass; 
   }

   public void getEffectiveMass (Matrix M, double t) {
      doGetMass (M, myEffectiveMass);
   }

   public int mulInverseEffectiveMass (
      Matrix M, double[] a, double[] f, int idx) {
      double minv = 1/myEffectiveMass;
      a[idx++] = minv*f[idx];
      a[idx++] = minv*f[idx];
      a[idx++] = minv*f[idx];
      return idx;
   }

   public void resetEffectiveMass() {
      myEffectiveMass = myMass;
   }

   public void addEffectiveMass (double m) {
      myEffectiveMass += m;
   }

   public void getInverseMass (Matrix Minv, Matrix M) {
      if (!(Minv instanceof Matrix3d)) {
         throw new IllegalArgumentException ("Minv not instance of Matrix3d");
      }
      if (!(M instanceof Matrix3d)) {
         throw new IllegalArgumentException ("M not instance of Matrix3d");
      }
      double inv = 1/((Matrix3d)M).m00;
      ((Matrix3d)Minv).setDiagonal (inv, inv, inv);
   }

   public void setMass (double m) {
      //myEffectiveMass += (m - myMass);
      myMass = m;
   }
   
   public void applyGravity (Vector3d gacc) {
      myForce.scaledAdd (myMass, gacc, myForce);
   }


   public int applyPosImpulse (double[] delx, int idx) {
      Vector3d pos = myState.pos;
      pos.x += delx[idx++];
      pos.y += delx[idx++];
      pos.z += delx[idx++];
      return idx;
   }

//   public int getVelDerivative (double[] dxdt, int idx) {
//      return getVelDerivative (dxdt, idx, MechModel.getEffectiveMass (this));
//   }

   public int getVelDerivative (double[] dxdt, int idx, double mass) {
      if (!MechModel.isActive (this)) {
         dxdt[idx++] = 0.0;
         dxdt[idx++] = 0.0;
         dxdt[idx++] = 0.0;
      }
      else {
         dxdt[idx++] = myForce.x / mass;
         dxdt[idx++] = myForce.y / mass;
         dxdt[idx++] = myForce.z / mass;
      }
      return idx;
   }
   public void scaleMass (double s) {
      myMass *= s;
      // myEffectiveMass *= s;
      myPointDamping *= s;
   }

   public void setDynamic (boolean dynamic) {
      super.setDynamic (dynamic);
   }

//   public void transformGeometry (
//      GeometryTransformer gtr, TransformGeometryContext context, int flags) {
//      super.transformGeometry (gtr, context, flags);
//   }

   public void scaleDistance (double s) {
      super.scaleDistance (s);
      // Should we send a GEOMETRY_CHANGED event? Don't for now ...
   }

   /**
    * {@inheritDoc}
    */
   public boolean isDuplicatable() {
      return true;
   }

   public PointParticleAttachment createPointAttachment (Point pnt) {
      PointParticleAttachment ppa = new PointParticleAttachment (this, pnt);
      if (DynamicAttachmentWorker.containsLoop (ppa, pnt, null)) {
         throw new IllegalArgumentException (
            "attachment contains loop");
      }
      return ppa;
   }

   /**
    * {@inheritDoc}
    */
   public boolean getCopyReferences (
      List<ModelComponent> refs, ModelComponent ancestor) {
      return true;
   }
   
  public ModelComponent copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      Particle comp = (Particle)super.copy (flags, copyMap);
      comp.myMass = myMass;
      // comp.myEffectiveMass = myMass
      return comp;
   }


}
