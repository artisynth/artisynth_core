/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.awt.Color;
import java.util.Map;

import maspack.matrix.RigidTransform3d;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.Vector3d;
import maspack.matrix.Point3d;
import maspack.properties.HasProperties;
import maspack.properties.PropertyList;
import maspack.render.Renderer;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.RenderableUtils;
import maspack.spatialmotion.FixedAxisCoupling;
import maspack.util.DoubleInterval;
import artisynth.core.mechmodels.*;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CopyableComponent;
import artisynth.core.modelbase.ModelComponent;

/**
 * Experimental 5 DOF joint in which rotation is restricted to 2 DOF.  This is
 * essentially universal joint with no translational constraint and no joint
 * limits.
 */
public class FixedAxisJoint extends JointBase implements CopyableComponent {
  
   public FixedAxisJoint() {
      setDefaultValues();
      setCoupling (new FixedAxisCoupling());
   }

   public FixedAxisJoint (
      RigidBody bodyA, RigidTransform3d TCA,
      RigidBody bodyB, RigidTransform3d XDB) {
      this();
      setBodies (bodyA, TCA, bodyB, XDB);
   }
   
   public FixedAxisJoint (
      RigidBody bodyA, RigidBody bodyB, RigidTransform3d TDW) {
      this();
      RigidTransform3d TCA = new RigidTransform3d();
      RigidTransform3d XDB = new RigidTransform3d();
      
      TCA.mulInverseLeft(bodyA.getPose(), TDW);
      XDB.mulInverseLeft(bodyB.getPose(), TDW);
      
      setBodies(bodyA, TCA, bodyB, XDB);
   }

   /* --- begin Renderable implementation --- */

   public void updateBounds (Vector3d pmin, Vector3d pmax) {
      super.updateBounds (pmin, pmax);
      double rad = myShaftLength;
      if (rad > 0) {
         Vector3d center = getCurrentTDW().p;
         RenderableUtils.updateSphereBounds (pmin, pmax, center, rad);
      }
   }

   private void computeRollAxisEndPoints (Point3d p0, Point3d p1, double slen) {

      RigidTransform3d TDW = myRenderFrameD;
      Vector3d uW = new Vector3d(); // joint axis vector in world coords

      // first set p0 to contact center in world coords
      p0.set (TDW.p);
      // now get axis unit vector in world coords
      uW.set (TDW.R.m02, TDW.R.m12, TDW.R.m22);
      p0.scaledAdd (-0.5 * slen, uW, p0);
      p1.scaledAdd (slen, uW, p0);
   }

   private void computePitchAxisEndPoints (Point3d p0, Point3d p1, double slen) {

      RigidTransform3d TCW = myRenderFrameC;
      Vector3d uW = new Vector3d(); // joint axis vector in world coords

      // first set p0 to contact center in world coords
      p0.set (TCW.p);
      // now get axis unit vector in world coords
      uW.set (TCW.R.m01, TCW.R.m11, TCW.R.m21);
      p0.scaledAdd (-0.5 * slen, uW, p0);
      p1.scaledAdd (slen, uW, p0);
   }

   public void render (Renderer renderer, int flags) {
      super.render (renderer, flags);

      double slen = myShaftLength;
      if (slen > 0) {
         Point3d p0 = new Point3d();
         Point3d p1 = new Point3d();
         renderer.setFaceColoring (myRenderProps, isSelected());
         double r = getEffectiveShaftRadius();
         computeRollAxisEndPoints (p0, p1, slen);
         renderer.drawCylinder (p0, p1, r, /*capped=*/true);
         computePitchAxisEndPoints (p0, p1, slen);
         renderer.drawCylinder (p0, p1, r, /*capped=*/true);
      }
   }

   /* --- end Renderable implementation --- */
}
