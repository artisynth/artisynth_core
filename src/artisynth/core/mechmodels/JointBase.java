/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.VectorNd;
import maspack.properties.PropertyList;

public abstract class JointBase extends RigidBodyConnector  {

   protected double myAxisLength;
   protected static final double defaultAxisLength = 1;
   protected RigidTransform3d myRenderFrame = new RigidTransform3d();
   
   public static PropertyList myProps =
      new PropertyList (SolidJoint.class, RigidBodyConnector.class);

   protected static VectorNd ZERO_VEC = new VectorNd(6);

   static {
      myProps.add (
         "axisLength", "length of the axis for this joint",
         defaultAxisLength);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   // public RigidTransform3d getCurrentXDW() {
   //    RigidTransform3d XDW = new RigidTransform3d();
   //    XDW.set (getXDB());
   //    if (myBodyB != null) {
   //       XDW.mul (myBodyB.getPose(), XDW);
   //    }
   //    return XDW;
   // }
   
   public RigidTransform3d getRenderFrame() {
      return myRenderFrame;
   }
   
   public void getPosition(Point3d pos) {
      pos.set(getCurrentTDW().p);
   }
   
   public double getAxisLength() {
      return myAxisLength;
   }

   public void setAxisLength (double len) {
      myAxisLength = len;
   }
   
   public void scaleDistance (double s) {
      super.scaleDistance (s);
      myAxisLength *= s;
      myRenderProps.scaleDistance (s);
   }
   

}
