/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.util.Map;

import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.matrix.RigidTransform3d;
import maspack.render.Renderer;
import maspack.render.RenderableUtils;
import maspack.spatialmotion.SphericalCoupling;
import maspack.properties.*;
import artisynth.core.modelbase.CopyableComponent;
import artisynth.core.modelbase.ModelComponent;

/**
 * Auxiliary class used to solve constrained rigid body problems.
 */
public class SphericalJointBase extends JointBase 
   implements CopyableComponent {

   public static final double DEFAULT_JOINT_RADIUS = 0;
   protected double myJointRadius = DEFAULT_JOINT_RADIUS;

   public static PropertyList myProps =
      new PropertyList (SphericalJointBase.class, JointBase.class);

   static {
      myProps.add (
         "jointRadius",
         "radius used for rendering the joint", DEFAULT_JOINT_RADIUS);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   /**
    * Returns a radius used for rendering this joint as a sphere. See {@link
    * #getJointRadius} for details.
    *
    * @return joint rendering radius
    */
   public double getJointRadius() {
      return myJointRadius;
   }

   /**
    * Sets a radius used for rendering this joint as a sphere.  The default
    * value is 0. Setting a value of -1 will invoke a legacy rendering method,
    * in which the joint is rendered using point rendering properties.
    *
    * @param r joint rendering radius
    */
   public void setJointRadius (double r) {
      myJointRadius = r;
   }

   /* --- begin Renderable implementation --- */

   public void updateBounds (Vector3d pmin, Vector3d pmax) {
      super.updateBounds (pmin, pmax);
      if (myJointRadius > 0) {
         Vector3d center = getCurrentTDW().p;
         RenderableUtils.updateSphereBounds (pmin, pmax, center, myJointRadius);
      }
   }

   public void render (Renderer renderer, int flags) {
      super.render (renderer, flags);
      if (myJointRadius != 0) {
         float[] coords =
            new float[] { (float)myRenderFrameD.p.x, (float)myRenderFrameD.p.y,
                          (float)myRenderFrameD.p.z };
         if (myJointRadius < 0) {
            // legacy rendering as a point
            renderer.drawPoint (myRenderProps, coords, isSelected());
         }
         else if (myJointRadius > 0) {
            renderer.setFaceColoring (myRenderProps, isSelected());
            renderer.drawSphere (coords, myJointRadius);
         }
      }
   }

   /* --- end Renderable implementation --- */

    @Override
   public ModelComponent copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      SphericalJointBase copy = (SphericalJointBase)super.copy (flags, copyMap);
      copy.setAxisLength (myAxisLength);
      copy.setRenderProps (getRenderProps());
      //copy.setBodies (copy.myBodyA, getTCA(), copy.myBodyB, getTDB());
      return copy;
   }

}
