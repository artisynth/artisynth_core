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
import maspack.properties.HasProperties;
import maspack.properties.PropertyList;
import maspack.render.GLRenderer;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.spatialmotion.SphericalCoupling;
import artisynth.core.modelbase.CopyableComponent;
import artisynth.core.modelbase.ModelComponent;

/**
 * Auxiliary class used to solve constrained rigid body problems.
 */
public class SphericalJointBase extends JointBase 
   implements CopyableComponent {

   public static PropertyList myProps =
      new PropertyList (SphericalJointBase.class, JointBase.class);

   protected static RenderProps defaultRenderProps (HasProperties host) {
      RenderProps props = RenderProps.createPointProps (host);
      return props;
   }

   static {
      myProps.add (
         "renderProps * *", "renderer properties", defaultRenderProps (null));
   } 

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public void setDefaultValues() {
      super.setDefaultValues();
      setRenderProps (defaultRenderProps (null));
   }
   
   public SphericalJointBase() {
      setDefaultValues();
      myCoupling = new SphericalCoupling ();
      myCoupling.setBreakSpeed (1e-8);
      myCoupling.setBreakAccel (1e-8);
      myCoupling.setContactDistance (1e-8);
   }

   public void updateBounds (Point3d pmin, Point3d pmax) {
      RigidTransform3d TFW = getCurrentTFW();
      TFW.p.updateBounds (pmin, pmax);
   }

   public RenderProps createRenderProps() {
      return defaultRenderProps (this);
   }

    public void prerender (RenderList list) {
      RigidTransform3d XDW = getCurrentTDW();
      myRenderFrame.set (XDW);
   }

   public void render (GLRenderer renderer, int flags) {
      if (myAxisLength != 0) {
         renderer.drawAxes (
            myRenderProps, myRenderFrame, myAxisLength, isSelected());
      }
      float[] coords =
         new float[] { (float)myRenderFrame.p.x, (float)myRenderFrame.p.y,
                      (float)myRenderFrame.p.z };
      renderer.drawPoint (myRenderProps, coords, isSelected());
   }

    @Override
   public ModelComponent copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      SphericalJointBase copy = (SphericalJointBase)super.copy (flags, copyMap);
      copy.myCoupling = new SphericalCoupling ();
      copy.setAxisLength (myAxisLength);
      copy.setRenderProps (getRenderProps());
      copy.setBodies (copy.myBodyA, getTFA(), copy.myBodyB, getTDB());
      return copy;
   }

}
