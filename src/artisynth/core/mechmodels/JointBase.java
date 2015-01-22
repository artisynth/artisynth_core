/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.VectorNd;
import maspack.properties.*;
import maspack.render.*;

public abstract class JointBase extends RigidBodyConnector  {

   protected double myAxisLength;
   protected static final double defaultAxisLength = 1;
   protected static final boolean defaultDrawFrame = false;
   protected RigidTransform3d myRenderFrame = new RigidTransform3d();
   protected boolean myDrawFrame = defaultDrawFrame;

   public static PropertyList myProps =
      new PropertyList (SolidJoint.class, RigidBodyConnector.class);

   protected static RenderProps defaultRenderProps (HasProperties host) {
      RenderProps props = RenderProps.createPointLineProps (host);
      return props;
   }

   public void setDefaultValues() {
      super.setDefaultValues();
      setRenderProps (defaultRenderProps (null));
   }

   protected static VectorNd ZERO_VEC = new VectorNd(6);

   static {
      myProps.add (
         "axisLength", "length of the axis for this joint",
         defaultAxisLength);
      myProps.add (
         "drawFrame", "if true, draw the D coordinate frame", defaultDrawFrame);
      myProps.add (
         "renderProps * *", "renderer properties", defaultRenderProps (null));
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public boolean getDrawFrame() {
      return myDrawFrame;
   }

   public void setDrawFrame (boolean draw) {
      myDrawFrame = draw;
   }

   // public RigidTransform3d getCurrentXDW() {
   //    RigidTransform3d TDW = new RigidTransform3d();
   //    TDW.set (getXDB());
   //    if (myBodyB != null) {
   //       TDW.mul (myBodyB.getPose(), TDW);
   //    }
   //    return TDW;
   // }
   
   public void updateBounds (Point3d pmin, Point3d pmax) {
      Point3d pend = new Point3d();
      Vector3d del = new Vector3d (myAxisLength, myAxisLength, myAxisLength);
      pend.set (getCurrentTDW().p);
      pend.add (del);
      pend.updateBounds (pmin, pmax);
      pend.scaledAdd (-2, del);
      pend.updateBounds (pmin, pmax);
   }

   public RenderProps createRenderProps() {
      return defaultRenderProps (this);
   }

    public void prerender (RenderList list) {
      RigidTransform3d TDW = getCurrentTDW();
      myRenderFrame.set (TDW);
   }

   public void render (GLRenderer renderer, int flags) {
      if (myDrawFrame) {
         renderer.drawAxes (
            myRenderProps, myRenderFrame, myAxisLength, isSelected());
      }
   }

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
