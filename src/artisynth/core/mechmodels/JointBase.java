/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.awt.Color;
import java.util.Map;

import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.VectorNd;
import maspack.properties.*;
import maspack.render.*;

import artisynth.core.modelbase.*;

public abstract class JointBase extends BodyConnector  {

   protected double myAxisLength;
   protected static final double DEFAULT_AXIS_LENGTH = 1;
   
   protected RigidTransform3d myRenderFrameD = new RigidTransform3d();
   protected RigidTransform3d myRenderFrameC = new RigidTransform3d();
   protected static final boolean DEFAULT_DRAW_FRAME_D = false;
   protected boolean myDrawFrameD = DEFAULT_DRAW_FRAME_D;
   protected static final boolean DEFAULT_DRAW_FRAME_C = false;
   protected boolean myDrawFrameC = DEFAULT_DRAW_FRAME_C;
   
   public static PropertyList myProps =
      new PropertyList (SolidJoint.class, BodyConnector.class);

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
         DEFAULT_AXIS_LENGTH);
      myProps.add (
         "drawFrameD", "if true, draw the D coordinate frame", 
         DEFAULT_DRAW_FRAME_D);
      myProps.add (
         "drawFrameC", "if true, draw the C coordinate frame", 
         DEFAULT_DRAW_FRAME_C);
      myProps.add (
         "renderProps * *", "renderer properties", defaultRenderProps (null));
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public boolean getDrawFrameD() {
      return myDrawFrameD;
   }

   public void setDrawFrameD (boolean draw) {
      myDrawFrameD = draw;
   }

   
   public void setDrawFrameC(boolean draw) {
      myDrawFrameC = draw;
   }
   
   public boolean getDrawFrameC() {
      return myDrawFrameC;
   }
   
   // public RigidTransform3d getCurrentXDW() {
   //    RigidTransform3d TDW = new RigidTransform3d();
   //    TDW.set (getXDB());
   //    if (myBodyB != null) {
   //       TDW.mul (myBodyB.getPose(), TDW);
   //    }
   //    return TDW;
   // }
   
   public void updateBounds (Vector3d pmin, Vector3d pmax) {
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
      myRenderFrameD.set (TDW);
      RigidTransform3d TCW = getCurrentTCW();
      myRenderFrameC.set (TCW);
   }

   public void render (Renderer renderer, int flags) {
      int lineWidth = myRenderProps.getLineWidth();
      
      if (myDrawFrameD) {
         renderer.drawAxes (
            myRenderFrameD, myAxisLength, lineWidth, isSelected());
      }
      
      if (myDrawFrameC) {
         // second frame
         renderer.drawAxes (
            myRenderFrameC, myAxisLength, lineWidth, isSelected());
      }
      
      if (myDrawFrameC && myDrawFrameD) {
         // distinguish one from the other
         Point3d pnt = new Point3d();
         pnt.transform (myRenderFrameC);
         renderer.setPointSize (myRenderProps.getPointSize ());
         
         pnt.scale (myAxisLength, Vector3d.X_UNIT);
         pnt.transform (myRenderFrameC);
         renderer.setColor (Color.RED);
         renderer.drawPoint (pnt);
         
         pnt.scale (myAxisLength, Vector3d.Y_UNIT);
         pnt.transform (myRenderFrameC);
         renderer.setColor (Color.GREEN);
         renderer.drawPoint (pnt);
         
         pnt.scale (myAxisLength, Vector3d.Z_UNIT);
         pnt.transform (myRenderFrameC);
         renderer.setColor (Color.BLUE);
         renderer.drawPoint (pnt);
      }
   }

   public RigidTransform3d getRenderFrame() {
      return myRenderFrameD;
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
   @Override
   public ModelComponent copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      JointBase copy = (JointBase)super.copy (flags, copyMap);
      copy.myRenderFrameD = new RigidTransform3d (myRenderFrameD);
      copy.myRenderFrameC = new RigidTransform3d (myRenderFrameC);
      return copy;
   }
   

}
