/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.util.Map;

import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.render.Renderer;
import maspack.spatialmotion.SphericalCoupling;
import artisynth.core.modelbase.CopyableComponent;
import artisynth.core.modelbase.ModelComponent;

/**
 * Auxiliary class used to solve constrained rigid body problems.
 */
public class SphericalJointBase extends JointBase 
   implements CopyableComponent {

   public SphericalJointBase() {
      setDefaultValues();
      myCoupling = new SphericalCoupling ();
      myCoupling.setBreakSpeed (1e-8);
      myCoupling.setBreakAccel (1e-8);
      myCoupling.setContactDistance (1e-8);
   }

   public void render (Renderer renderer, int flags) {
      super.render (renderer, flags);
      float[] coords =
         new float[] { (float)myRenderFrameD.p.x, (float)myRenderFrameD.p.y,
                      (float)myRenderFrameD.p.z };
      renderer.drawPoint (myRenderProps, coords, isSelected());
   }

    @Override
   public ModelComponent copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      SphericalJointBase copy = (SphericalJointBase)super.copy (flags, copyMap);
      copy.myCoupling = new SphericalCoupling ();
      copy.setAxisLength (myAxisLength);
      copy.setRenderProps (getRenderProps());
      //copy.setBodies (copy.myBodyA, getTCA(), copy.myBodyB, getTDB());
      return copy;
   }

}
