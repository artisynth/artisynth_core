/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.awt.Color;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import artisynth.core.materials.FrameMaterial;
import artisynth.core.materials.MaterialBase;
import artisynth.core.materials.MaterialChangeEvent;
import artisynth.core.materials.RotAxisFrameMaterial;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.CopyableComponent;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.ModelComponentBase;
import artisynth.core.modelbase.RenderableComponent;
import artisynth.core.modelbase.RenderableComponentBase;
import artisynth.core.modelbase.TransformGeometryContext;
import artisynth.core.modelbase.TransformableGeometry;
import artisynth.core.util.ScalableUnits;
import artisynth.core.util.ScanToken;
import maspack.geometry.GeometryTransformer;
import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.AxisAngle;
import maspack.matrix.Matrix;
import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix6d;
import maspack.matrix.Matrix6dBlock;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.SparseNumberedBlockMatrix;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.properties.PropertyList;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.spatialmotion.Twist;
import maspack.spatialmotion.Wrench;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;

public class FrameSpring extends Spring
   implements RenderableComponent, ScalableUnits, TransformableGeometry,
              CopyableComponent, ForceTargetComponent {

   protected Frame myFrameA;
   protected Frame myFrameB;
   protected boolean debug = false;

   /**
    * 1 is the attachment frame for A, and 2 is the attachment frame for B
    */
   protected RigidTransform3d myX1A = new RigidTransform3d();
   protected RigidTransform3d myX2B = new RigidTransform3d();

   protected double myStiffness;
   protected double myDamping;
   protected double myRotaryStiffness;
   protected double myRotaryDamping;

   protected Wrench myF = new Wrench();
   protected Wrench myFTmp = new Wrench();

   private Twist myVel1 = new Twist();
   private Twist myVel2 = new Twist();
   private Twist myVel21 = new Twist();

   protected int myBlk00Num;
   protected int myBlk01Num;
   protected int myBlk10Num;
   protected int myBlk11Num;

   // used for rendering computation
   protected double myAxisLength = 0;
   protected RigidTransform3d myRenderFrame = new RigidTransform3d();
   protected float[] myRenderPnt1 = new float[3];
   protected float[] myRenderPnt2 = new float[3];
   
   protected static final boolean DEFAULT_DRAW_FRAME_D = true;
   protected boolean myDrawFrameD = DEFAULT_DRAW_FRAME_D;
   protected static final boolean DEFAULT_DRAW_FRAME_C = true;
   protected boolean myDrawFrameC = DEFAULT_DRAW_FRAME_C;
   
   private Matrix3d myTmpM = new Matrix3d();
   // private RotationMatrix3d myRBA = new RotationMatrix3d();

   public static boolean mySymmetricJacobian = true;

   protected RigidTransform3d myX21 = new RigidTransform3d();
   protected RigidTransform3d myInitialX21 = new RigidTransform3d();

   public static PropertyList myProps =
      new PropertyList (FrameSpring.class, ModelComponentBase.class);

   protected FrameMaterial myMaterial;

   static {
      myProps.add ("renderProps * *", "renderer properties", null);
      myProps.add ("attachFrameA * *", "attachment for FrameA",
                   RigidTransform3d.IDENTITY);
      myProps.add ("attachFrameB * *", "attachment for FrameB",
                   RigidTransform3d.IDENTITY);
      myProps.add ("initialT21", "initial transform from frame 2 to frame 1",
                   RigidTransform3d.IDENTITY);
      myProps.add (
         "material", "spring material parameters", createDefaultMaterial(), "CE");
      myProps.add ("axisLength * *", "length of rendered frame axes", 1f);
      myProps.addReadOnly("springForce *", "The spring force");
      myProps.add (
         "drawFrameD", "if true, draw the D coordinate frame", 
         DEFAULT_DRAW_FRAME_D);
      myProps.add (
         "drawFrameC", "if true, draw the C coordinate frame", 
         DEFAULT_DRAW_FRAME_C);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public FrameSpring() {
      this (null);
   }

   public FrameSpring (String name) {
      super (name);
      setMaterial (createDefaultMaterial());
   }

   public FrameSpring (
      String name, double k, double kRot, double d, double dRot) {
      super (name);
      setMaterial (new RotAxisFrameMaterial (k, kRot, d, dRot));
      myStiffness = k;
      myDamping = d;
      myRotaryStiffness = kRot;
      myRotaryDamping = dRot;
   }

   public FrameSpring (String name, FrameMaterial mat) {
      super (name);
      setMaterial (mat);
   }

   public double getAxisLength() {
      return myAxisLength;
   }

   public void setAxisLength (double len) {
      myAxisLength = Math.max (0, len);
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
   
   public void setInitialT21 (RigidTransform3d X21) {
      myInitialX21.set (X21);
   }

   public RigidTransform3d getInitialT21 () {
      return myInitialX21;
   }

   /**
    * Set the initialX21 to the current value of the transform from
    * frame 2 to frame 1.
    */
   public void setInitialT21 () {
      myInitialX21.mulInverseBoth (myX1A, myFrameA.getPose());
      if (myFrameB != null) {
         myInitialX21.mul (myFrameB.getPose());
      }
      myInitialX21.mul (myX2B);
   }

   /* ======== Renderable implementation ======= */

   protected RenderProps myRenderProps = createRenderProps();

   public RenderProps getRenderProps() {
      return myRenderProps;
   }

   public void setRenderProps (RenderProps props) {
      myRenderProps =
         RenderableComponentBase.updateRenderProps (this, myRenderProps, props);
   }

   public RenderProps createRenderProps() {
      return RenderProps.createLineProps (this);
   }

   public void prerender (RenderList list) {
      // nothing to do
   }

   public void updateBounds (Vector3d pmin, Vector3d pmax) {
      myFrameA.getPose().p.updateBounds (pmin, pmax);
      if (myFrameB != null) {
         myFrameB.getPose().p.updateBounds (pmin, pmax);
      }
   }

   public int getRenderHints() {
      int code = 0;
      if (myRenderProps != null && myRenderProps.isTransparent()) {
         code |= TRANSPARENT;
      }
      return code;
   }

   public boolean isSelectable() {
      return true;
   }

   public int numSelectionQueriesNeeded() {
      return -1;
   }

   public void render (Renderer renderer, int flags) {

      myRenderFrame.set (myFrameA.myRenderFrame);
      myRenderFrame.mul (myX1A);
      myRenderFrame.p.get (myRenderPnt1);

      int lineWidth = myRenderProps.getLineWidth();
      if (myDrawFrameC && myAxisLength > 0) {
         renderer.drawAxes (
            myRenderFrame, myAxisLength, lineWidth, isSelected());
         
         if (myDrawFrameD) {
            // distinguish one from the other
            Point3d pnt = new Point3d();
            pnt.transform (myRenderFrame);
            renderer.setPointSize (myRenderProps.getPointSize ());
            
            pnt.scale (myAxisLength, Vector3d.X_UNIT);
            pnt.transform (myRenderFrame);
            renderer.setColor (Color.RED);
            renderer.drawPoint (pnt);
            
            pnt.scale (myAxisLength, Vector3d.Y_UNIT);
            pnt.transform (myRenderFrame);
            renderer.setColor (Color.GREEN);
            renderer.drawPoint (pnt);
            
            pnt.scale (myAxisLength, Vector3d.Z_UNIT);
            pnt.transform (myRenderFrame);
            renderer.setColor (Color.BLUE);
            renderer.drawPoint (pnt);
         }
      }

      if (myFrameB != null) {
         myRenderFrame.set (myFrameB.myRenderFrame);
      }
      else {
         myRenderFrame.setIdentity();
      }
      myRenderFrame.mul (myX2B);
      myRenderFrame.p.get (myRenderPnt2);
         
      if (myDrawFrameD && myAxisLength > 0) {
         renderer.drawAxes (
            myRenderFrame, myAxisLength, lineWidth, isSelected());
      }

      renderer.drawLine (
         myRenderProps, myRenderPnt1, myRenderPnt2, isSelected());
   }

   public void getSelection (LinkedList<Object> list, int qid) {
   }
   
   /* ======== End Renderable implementation ======= */

   public static FrameMaterial createDefaultMaterial() {
      return new RotAxisFrameMaterial();
   }

   public FrameMaterial getMaterial() {
      return myMaterial;
   }
   
   public <T extends FrameMaterial> void setMaterial (T mat) {
      FrameMaterial oldMat = myMaterial;
      T newMat = (T)MaterialBase.updateMaterial (
         this, "material", myMaterial, mat);
      myMaterial = newMat;
      // issue change event in case solve matrix symmetry or state has changed:
      MaterialChangeEvent mce = 
         MaterialBase.symmetryOrStateChanged ("material", newMat, oldMat);
      if (mce != null) {
         notifyParentOfChange (mce);
      }  
      //return newMat;
   }

   public void setRotaryStiffness (double k) {
     myRotaryStiffness = k;
   }

   public void setFrameA (Frame frame) {
      myFrameA = frame;
   }

   public Frame getFrameA() {
      return myFrameA;
   }

   public void setFrameB (Frame frame) {
      myFrameB = frame;
   }

   public Frame getFrameB() {
      return myFrameB;
   }

   public void setAttachFrameA (RigidTransform3d X1A) {
      myX1A = new RigidTransform3d (X1A);
   }

   public RigidTransform3d getAttachFrameA() {
      return myX1A;
   }

   public void setAttachFrameB (RigidTransform3d X2B) {
      myX2B = new RigidTransform3d (X2B);
   }

   public RigidTransform3d getAttachFrameB() {
      return myX2B;
   }

   public void setFrames (Frame frameA, Frame frameB, RigidTransform3d TDW) {
      RigidTransform3d TCA = new RigidTransform3d();
      RigidTransform3d TDB = new RigidTransform3d();
      
      TCA.mulInverseLeft(frameA.getPose(), TDW);
      if (frameB != null) {
         TDB.mulInverseLeft(frameB.getPose(), TDW);
      }
      else {
         TDB.set (TDW);
      }
      setFrames (frameA, TCA, frameB, TDB);
   }
   
   public void setFrames (Frame frameA, RigidTransform3d T1A,
                          Frame frameB, RigidTransform3d T2B) {
      myFrameA = frameA;
      myX1A = new RigidTransform3d (T1A);
      myFrameB = frameB;
      myX2B = new RigidTransform3d (T2B);
   }
   
   public Wrench getSpringForce() {
	   return myF;
   }

   private void computeRelativeDisplacements () {
      myX21.mulInverseBoth (myX1A, myFrameA.getPose());
      if (myFrameB != null) {
         myX21.mul (myFrameB.getPose());
      }
      myX21.mul (myX2B);

      myFrameA.getBodyVelocity (myVel1);
      myVel1.inverseTransform (myX1A);
      if (myFrameB != null) {
         myFrameB.getBodyVelocity (myVel2);
         myVel2.inverseTransform (myX2B);
         myVel2.transform (myX21.R);
         myVel21.sub (myVel2, myVel1);
      }
      else {
         myVel21.negate (myVel1);
      }
   }

   // Computes the force as seen in frame 1 and places the resukt in myF.
   protected void computeForces () {

      FrameMaterial mat = myMaterial;

      if (mat != null) { // just in case implementation allows null material ...
         computeRelativeDisplacements();
         mat.computeF (myF, myX21, myVel21, myInitialX21);
      }
   }
    
   public void applyForces (double t) {

      computeForces();

      myFTmp.transform (myX1A, myF);
      myFTmp.transform (myFrameA.getPose().R); // put into rotated world coords
      myFrameA.addForce (myFTmp);

      if (myFrameB != null) {
         // Sanchez, July 9 2013
         // Prevent changing myF in this function
         myFTmp.set(myF);
         myFTmp.inverseTransform (myX21.R);
         
         myFTmp.transform (myX2B, myFTmp);
         myFTmp.transform (myFrameB.getPose().R); // put into rotated world coords
         myFTmp.negate();
         myFrameB.addForce (myFTmp);
      }
   }

   private void setScaledCrossProd (Matrix3d M, double s, Vector3d u) {
      M.m00 = 0;
      M.m01 = -s*u.z;
      M.m02 =  s*u.y;

      M.m10 =  s*u.z;
      M.m11 = 0;
      M.m12 = -s*u.x;

      M.m20 = -s*u.y;
      M.m21 =  s*u.x;
      M.m22 = 0;
   }

   void computeU (Matrix3d U, AxisAngle axisAng) {

      if (Math.abs(axisAng.angle) < 1e-8) {
         U.setIdentity();
      }
      else {
      
         double ang = axisAng.angle;
         double a = ang/Math.tan (ang/2);
         double b = (2 - ang/Math.tan (ang/2));

         Vector3d u = axisAng.axis;
         U.outerProduct (u, u);
         U.scale (b);

         U.m00 += a;
         U.m11 += a;
         U.m22 += a;

         double vz = ang*u.z;
         double vy = ang*u.y;
         double vx = ang*u.x;

         if (!mySymmetricJacobian) {
            U.m01 += vz;
            U.m02 -= vy;
            U.m12 += vx;
            U.m10 -= vz;
            U.m20 += vy;
            U.m21 -= vx;
         }

         U.scale (0.5);
      }
   }

   public void addPosJacobian (SparseNumberedBlockMatrix M, double s) {
      addPosJacobianWorld (M, s);
   }

   private Matrix6dBlock getBlock (SparseNumberedBlockMatrix M, int num) {
      if (num == -1) {
         return null;
      }
      else {
         return (Matrix6dBlock)M.getBlockByNumber (num);
      }            
   }

   /**
    * Computes
    * <pre>
    *              [  0   B  ]
    * MR = MR + M1 [         ]
    *              [  0   0  ]
    * </pre>
    * and places the result in M.
    */
   private void postMul03Block (Matrix6d MR, Matrix6d M1, Matrix3d B) {
      double b00 = B.m00;
      double b01 = B.m01;
      double b02 = B.m02;
      double b10 = B.m10;
      double b11 = B.m11;
      double b12 = B.m12;
      double b20 = B.m20;
      double b21 = B.m21;
      double b22 = B.m22;

      MR.m03 += (M1.m00*b00+M1.m01*b10+M1.m02*b20);
      MR.m04 += (M1.m00*b01+M1.m01*b11+M1.m02*b21);
      MR.m05 += (M1.m00*b02+M1.m01*b12+M1.m02*b22);
      MR.m13 += (M1.m10*b00+M1.m11*b10+M1.m12*b20);
      MR.m14 += (M1.m10*b01+M1.m11*b11+M1.m12*b21);
      MR.m15 += (M1.m10*b02+M1.m11*b12+M1.m12*b22);
      MR.m23 += (M1.m20*b00+M1.m21*b10+M1.m22*b20);
      MR.m24 += (M1.m20*b01+M1.m21*b11+M1.m22*b21);
      MR.m25 += (M1.m20*b02+M1.m21*b12+M1.m22*b22);

      MR.m33 += (M1.m30*b00+M1.m31*b10+M1.m32*b20);
      MR.m34 += (M1.m30*b01+M1.m31*b11+M1.m32*b21);
      MR.m35 += (M1.m30*b02+M1.m31*b12+M1.m32*b22);
      MR.m43 += (M1.m40*b00+M1.m41*b10+M1.m42*b20);
      MR.m44 += (M1.m40*b01+M1.m41*b11+M1.m42*b21);
      MR.m45 += (M1.m40*b02+M1.m41*b12+M1.m42*b22);
      MR.m53 += (M1.m50*b00+M1.m51*b10+M1.m52*b20);
      MR.m54 += (M1.m50*b01+M1.m51*b11+M1.m52*b21);
      MR.m55 += (M1.m50*b02+M1.m51*b12+M1.m52*b22);
   }

   /**
    * Computes
    * <pre>
    *           [  0   [s tw.v] ]
    * M = M + D [               ]
    *           [  0   [s tw.w] ]
    * </pre>
    * and places the result in M.
    */
   private void postMulWrenchCross (Matrix6d M, Matrix6d D, double s, Twist tw) {

      double vx = s*tw.v.x;
      double vy = s*tw.v.y;
      double vz = s*tw.v.z;

      double wx = s*tw.w.x;
      double wy = s*tw.w.y;
      double wz = s*tw.w.z;

      M.m03 += (D.m01*vz-D.m02*vy) + (D.m04*wz-D.m05*wy);
      M.m04 += (D.m02*vx-D.m00*vz) + (D.m05*wx-D.m03*wz);
      M.m05 += (D.m00*vy-D.m01*vx) + (D.m03*wy-D.m04*wx);
      M.m13 += (D.m11*vz-D.m12*vy) + (D.m14*wz-D.m15*wy);
      M.m14 += (D.m12*vx-D.m10*vz) + (D.m15*wx-D.m13*wz);
      M.m15 += (D.m10*vy-D.m11*vx) + (D.m13*wy-D.m14*wx);
      M.m23 += (D.m21*vz-D.m22*vy) + (D.m24*wz-D.m25*wy);
      M.m24 += (D.m22*vx-D.m20*vz) + (D.m25*wx-D.m23*wz);
      M.m25 += (D.m20*vy-D.m21*vx) + (D.m23*wy-D.m24*wx);

      M.m33 += (D.m31*vz-D.m32*vy) + (D.m34*wz-D.m35*wy);
      M.m34 += (D.m32*vx-D.m30*vz) + (D.m35*wx-D.m33*wz);
      M.m35 += (D.m30*vy-D.m31*vx) + (D.m33*wy-D.m34*wx);
      M.m43 += (D.m41*vz-D.m42*vy) + (D.m44*wz-D.m45*wy);
      M.m44 += (D.m42*vx-D.m40*vz) + (D.m45*wx-D.m43*wz);
      M.m45 += (D.m40*vy-D.m41*vx) + (D.m43*wy-D.m44*wx);
      M.m53 += (D.m51*vz-D.m52*vy) + (D.m54*wz-D.m55*wy);
      M.m54 += (D.m52*vx-D.m50*vz) + (D.m55*wx-D.m53*wz);
      M.m55 += (D.m50*vy-D.m51*vx) + (D.m53*wy-D.m54*wx);
   }

   /**
    * Computes
    * <pre>
    *       [  I  -[v] ]
    * M = M [          ]
    *       [  0    I  ]
    * </pre>
    * and places the result in M.
    */
   private void postMulMoment (Matrix6d M, Vector3d v) {
      double x = v.x;
      double y = v.y;
      double z = v.z;

      M.m03 -= (M.m01*z-M.m02*y);
      M.m04 -= (M.m02*x-M.m00*z);
      M.m05 -= (M.m00*y-M.m01*x);
      M.m13 -= (M.m11*z-M.m12*y);
      M.m14 -= (M.m12*x-M.m10*z);
      M.m15 -= (M.m10*y-M.m11*x);
      M.m23 -= (M.m21*z-M.m22*y);
      M.m24 -= (M.m22*x-M.m20*z);
      M.m25 -= (M.m20*y-M.m21*x);

      M.m33 -= (M.m31*z-M.m32*y);
      M.m34 -= (M.m32*x-M.m30*z);
      M.m35 -= (M.m30*y-M.m31*x);
      M.m43 -= (M.m41*z-M.m42*y);
      M.m44 -= (M.m42*x-M.m40*z);
      M.m45 -= (M.m40*y-M.m41*x);
      M.m53 -= (M.m51*z-M.m52*y);
      M.m54 -= (M.m52*x-M.m50*z);
      M.m55 -= (M.m50*y-M.m51*x);
   }

   /**
    * Computes
    * <pre>
    *     [  I    0  ]
    * M = [          ] M
    *     [ [v]   I  ]
    * </pre>
    * and places the result in M.
    */
   private void preMulMoment (Matrix6d M, Vector3d v) {
      double x = v.x;
      double y = v.y;
      double z = v.z;

      M.m30 -= (M.m10*z-M.m20*y);
      M.m40 -= (M.m20*x-M.m00*z);
      M.m50 -= (M.m00*y-M.m10*x);
      M.m31 -= (M.m11*z-M.m21*y);
      M.m41 -= (M.m21*x-M.m01*z);
      M.m51 -= (M.m01*y-M.m11*x);
      M.m32 -= (M.m12*z-M.m22*y);
      M.m42 -= (M.m22*x-M.m02*z);
      M.m52 -= (M.m02*y-M.m12*x);

      M.m33 -= (M.m13*z-M.m23*y);
      M.m43 -= (M.m23*x-M.m03*z);
      M.m53 -= (M.m03*y-M.m13*x);
      M.m34 -= (M.m14*z-M.m24*y);
      M.m44 -= (M.m24*x-M.m04*z);
      M.m54 -= (M.m04*y-M.m14*x);
      M.m35 -= (M.m15*z-M.m25*y);
      M.m45 -= (M.m25*x-M.m05*z);
      M.m55 -= (M.m05*y-M.m15*x);
   }

   public void addPosJacobianWorld (SparseNumberedBlockMatrix M, double s) {

      FrameMaterial mat = myMaterial;
      if (mat == null) {
         // just in case implementation allows null material ...
         return;
      }

      Matrix6dBlock blk00 = getBlock (M, myBlk00Num);
      Matrix6dBlock blk11 = getBlock (M, myBlk11Num);
      Matrix6dBlock blk01 = getBlock (M, myBlk01Num);
      Matrix6dBlock blk10 = getBlock (M, myBlk10Num);
      
      computePosJacobianWorld (
         mat, s, s, blk00, blk01, blk10, blk11, mySymmetricJacobian);
   }

   private void computePosJacobianWorld (
      FrameMaterial mat, double sk, double sd,
      Matrix6dBlock blk00, Matrix6dBlock blk01, 
      Matrix6dBlock blk10, Matrix6dBlock blk11,
      boolean symmetric) {
      
      if (symmetric) {
         sd = 0;
      }

      Matrix6d JK = new Matrix6d();
      Matrix6d JD = null;
      RotationMatrix3d R1W = new RotationMatrix3d();

      RigidTransform3d XAW = myFrameA.getPose();
      RigidTransform3d XBW;
      if (myFrameB != null) {
         XBW = myFrameB.getPose();
      }
      else {
         XBW = RigidTransform3d.IDENTITY;
      }

      R1W.mul (XAW.R, myX1A.R);

      Vector3d p1 = new Vector3d();      
      Vector3d p2 = new Vector3d();
      Vector3d x21 = new Vector3d();
      p1.transform (XAW.R, myX1A.p);
      p2.transform (XBW.R, myX2B.p);

      Matrix3d T = myTmpM;
      
      computeRelativeDisplacements();

      Twist vel21 = (sd != 0.0 ? myVel21 : Twist.ZERO);

      x21.set (myX21.p);
      x21.transform (R1W);

      myFrameA.getVelocity (myVel1);
      if (myFrameB != null) {
         myFrameB.getVelocity (myVel2);
      }
      else {
         myVel2.setZero();
      }
      if (!symmetric) {
         // compute forces for assymetric force component
         mat.computeF (myF, myX21, vel21, myInitialX21);
         // map forces back to World coords
         myF.transform (R1W);

         if (sd != 0) {
            JD = new Matrix6d();
            mat.computeDFdu (
               JD, myX21, vel21, myInitialX21, /*symmetric=*/false);
            JD.transform (R1W);
         }
      }
      mat.computeDFdq (JK, myX21, vel21, myInitialX21, symmetric);
      JK.transform (R1W);
      JK.scale (-sk);
      vel21.transform (R1W);

      Matrix6d tmp00 = new Matrix6d();
      Matrix6d tmp11 = new Matrix6d();
      Matrix6d tmp01 = new Matrix6d();
      Matrix6d tmp10 = new Matrix6d();     
      
      if (blk00 != null) {
         tmp00.set (JK);
         postMulMoment (tmp00, p1);
      }
      if (blk11 != null) {
         tmp11.set (JK);
         postMulMoment (tmp11, p2);
      }    
      if (blk01 != null || blk10 != null) {
         JK.negate();
         tmp01.set (JK);
         postMulMoment (tmp01, p2);
         tmp10.set (JK);
         postMulMoment (tmp10, p1);
      }
    
      if (blk00 != null) {
         if (!symmetric) {
            
            // QK term
            postMulMoment (tmp00, x21);          

            // QF(0,0) term
            setScaledCrossProd (T, -sk, myF.f);
            tmp00.addSubMatrix03 (T);
            setScaledCrossProd (T, -sk, myF.m);
            tmp00.addSubMatrix33 (T);
            setScaledCrossProd (T, sk, p1);
            T.crossProduct (myF.f, T);
            tmp00.addSubMatrix33 (T);

            // QD term
            if (sd != 0) {
               postMulWrenchCross (tmp00, JD, sd, vel21);
               setScaledCrossProd (T, sd, p1);
               T.crossProduct (myVel1.w, T);
               postMul03Block (tmp00, JD, T);
            }
         }
         preMulMoment (tmp00, p1);
         blk00.add (tmp00);
      }
      if (blk11 != null) {
         if (!symmetric) {
            // QF(1,1) term
            setScaledCrossProd (T, -sk, p2);
            T.crossProduct (myF.f, T);
            tmp11.addSubMatrix33 (T);

            // QD term
            if (sd != 0) {
               setScaledCrossProd (T, sd, p2);
               T.crossProduct (myVel2.w, T);
               postMul03Block (tmp11, JD, T);
            }
            
         }
         preMulMoment (tmp11, p2);
         blk11.add (tmp11);
      }
      if (blk01 != null) {
         if (!symmetric) {
            // QD term
            if (sd != 0) {
               setScaledCrossProd (T, -sd, p2);
               T.crossProduct (myVel2.w, T);
               postMul03Block (tmp01, JD, T);
            }
            
         }
         preMulMoment (tmp01, p1);
         blk01.add (tmp01);
      }
      if (blk10 != null) {
         if (!symmetric) {
            // QK term
            postMulMoment (tmp10, x21);

            // QF(1,0) term
            setScaledCrossProd (T, sk, myF.f);
            tmp10.addSubMatrix03 (T);
            setScaledCrossProd (T, sk, myF.m);
            tmp10.addSubMatrix33 (T);

            // QD term
            if (sd != 0) {
               postMulWrenchCross (tmp10, JD, -sd, vel21);
               setScaledCrossProd (T, -sd, p1);
               T.crossProduct (myVel1.w, T);
               postMul03Block (tmp10, JD, T);
            }
         }
         preMulMoment (tmp10, p2);
         blk10.add (tmp10);
      }      
   }

   public void addVelJacobian (SparseNumberedBlockMatrix M, double s) {
      addVelJacobianWorld (M, s);
   }

   public void addVelJacobianWorld (SparseNumberedBlockMatrix M, double s) {

      FrameMaterial mat = myMaterial;
      if (mat == null) {
         // just in case implementation allows null material ...
         return;
      }

      Matrix6dBlock blk00 = getBlock (M, myBlk00Num);
      Matrix6dBlock blk11 = getBlock (M, myBlk11Num);
      Matrix6dBlock blk01 = getBlock (M, myBlk01Num);
      Matrix6dBlock blk10 = getBlock (M, myBlk10Num);

      computeVelJacobianWorld (mat, s, blk00, blk01, blk10, blk11);
   }

   public void computeVelJacobianWorld (
      FrameMaterial mat, double sd,
      Matrix6d blk00, Matrix6d blk01, 
      Matrix6d blk10, Matrix6d blk11) {

      Matrix6d D = new Matrix6d();
      RotationMatrix3d R1W = new RotationMatrix3d();

      RigidTransform3d XAW = myFrameA.getPose();
      RigidTransform3d XBW;
      if (myFrameB != null) {
         XBW = myFrameB.getPose();
      }
      else {
         XBW = RigidTransform3d.IDENTITY;
      }


      R1W.mul (XAW.R, myX1A.R);

      Vector3d p1 = new Vector3d();      
      Vector3d p2 = new Vector3d();

      p1.transform (XAW.R, myX1A.p);
      p2.transform (XBW.R, myX2B.p);

      // Matrix3d T = myTmpM;

      Matrix6d tmp00 = new Matrix6d();
      Matrix6d tmp11 = new Matrix6d();
      Matrix6d tmp01 = new Matrix6d();
      Matrix6d tmp10 = new Matrix6d();     
      
      computeRelativeDisplacements();
      mat.computeDFdu (D, myX21, myVel21, myInitialX21, mySymmetricJacobian);
      D.transform (R1W);
      D.scale (-sd);
      if (blk00 != null) {
         tmp00.set (D);
         postMulMoment (tmp00, p1);
         preMulMoment (tmp00, p1);
         blk00.add (tmp00);
      }
      if (blk11 != null) {
         tmp11.set (D);
         postMulMoment (tmp11, p2);
         preMulMoment (tmp11, p2);
         blk11.add (tmp11);
      }
      if (blk01 != null && blk10 != null) {
         D.negate();

         if (blk01 != null) {
            tmp01.set (D);
            postMulMoment (tmp01, p2);
            preMulMoment (tmp01, p1);
            blk01.add (tmp01);
         }
         if (blk10 != null) {
            tmp10.set (D);
            postMulMoment (tmp10, p1);
            preMulMoment (tmp10, p2);
            blk10.add (tmp10);
         }
      }      
   }

   public void addSolveBlocks (SparseNumberedBlockMatrix M) {
      int bi0 = myFrameA.getSolveIndex();
      int bi1 = myFrameB != null ? myFrameB.getSolveIndex() : -1;

      // System.out.println ("add solve blocks " + bi0 + " " + bi1);

      if (bi0 != -1 && bi1 != -1) {
         Matrix6dBlock blk01 = (Matrix6dBlock)M.getBlock (bi0, bi1);
         if (blk01 == null) {
            blk01 = new Matrix6dBlock();
            M.addBlock (bi0, bi1, blk01);
         }
         myBlk01Num = blk01.getBlockNumber();
         Matrix6dBlock blk10 = (Matrix6dBlock)M.getBlock (bi1, bi0);
         if (blk10 == null) {
            blk10 = new Matrix6dBlock();
            M.addBlock (bi1, bi0, blk10);
         }
         myBlk10Num = blk10.getBlockNumber();
      }
      else {
         //blk01 = null;
         //blk10 = null;
         myBlk01Num = -1;
         myBlk10Num = -1;
      }
      //blk00 = (bi0 != -1 ? (Matrix6dBlock)M.getBlock (bi0, bi0) : null);
      //blk11 = (bi1 != -1 ? (Matrix6dBlock)M.getBlock (bi1, bi1) : null);
      myBlk00Num = (bi0 != -1 ? M.getBlock(bi0, bi0).getBlockNumber() : -1);
      myBlk11Num = (bi1 != -1 ? M.getBlock(bi1, bi1).getBlockNumber() : -1);
   }      

   /* --- Begin MinimizeForceComponent interface (for inverse controller) --- */

   public int getForceSize() {
      return 6;
   }

   public void getForce (VectorNd minf, boolean staticOnly) {
      minf.setSize (6);

      FrameMaterial mat = myMaterial;
      if (mat != null) { // just in case implementation allows null material ...
         computeRelativeDisplacements();
         if (!staticOnly) {
            mat.computeF (myF, myX21, myVel21, myInitialX21);
         }
         else {
            mat.computeF (myF, myX21, Twist.ZERO, myInitialX21);
         }
         myFTmp.transform (myX1A, myF);
         myFTmp.transform (myFrameA.getPose().R); // put into rotated world coords
         minf.set (myFTmp);
      }
      else {
         minf.setZero();
      }
   }

   Matrix6dBlock getOrCreateBlock (SparseBlockMatrix J, Frame frame, int bi) {
      int bj;
      Matrix6dBlock blk = null;
      if (frame != null && (bj = frame.getSolveIndex()) != -1) {
         blk = (Matrix6dBlock)J.getBlock (bi, bj);
         if (blk == null) {
            blk = new Matrix6dBlock();
            J.addBlock (bi, bj, blk);
         }
      }     
      return blk;
   }

   public int addForcePosJacobian (
      SparseBlockMatrix J, double h, boolean staticOnly, int bi) {

      Matrix6dBlock blk00 = getOrCreateBlock (J, myFrameA, bi);
      Matrix6dBlock blk01 = getOrCreateBlock (J, myFrameB, bi);
      FrameMaterial mat = myMaterial;
      double sd = staticOnly ? 0.0 : h;
      if (mat != null) {
         // just in case implementation allows null material ...
         computePosJacobianWorld (
            mat, h, sd, blk00, blk01, null, null, /*symmetric=*/false);
      }
      return bi++;
   }

   public int addForceVelJacobian (
      SparseBlockMatrix J, double h, int bi) {

      Matrix6dBlock blk00 = getOrCreateBlock (J, myFrameA, bi);
      Matrix6dBlock blk01 = getOrCreateBlock (J, myFrameB, bi);
      FrameMaterial mat = myMaterial;
      if (mat != null) {
         computeVelJacobianWorld (mat, h, blk00, blk01, null, null);
      }
      return bi++;
   }

   /* --- End MinimizeForceComponent interface --- */

   /* --- TransformableGeometry --- */

   public void transformGeometry (AffineTransform3dBase X) {
      TransformGeometryContext.transform (this, X, 0);
   }

   public void addTransformableDependencies (
      TransformGeometryContext context, int flags) {
      // nothing to add
   }

   public void transformGeometry (
      GeometryTransformer gtr, TransformGeometryContext context, int flags) {

      // Quantities that may need to be changed include: X1A, X2B
      // 
      // Transforming a FrameSpring is similar to transforming a
      // BodyConnector. However, we do not currently handle cases where one or
      // both frames are transforming while the FrameSpring is not.  That's
      // because we don't have a quick way to tell what FrameSprings are
      // connected to a Frame.

      boolean frameATransforming = context.contains(myFrameA);
      boolean frameBTransforming = (myFrameB!=null && context.contains(myFrameA));

      if (!frameATransforming || !gtr.isRigid()) {
         // need to update myX1A
         if (!context.isTransformed(myFrameA)) {
            RigidTransform3d T1W = new RigidTransform3d();
            T1W.mul (myFrameA.getPose(), myX1A);
            gtr.transform (T1W);
            myX1A.mulInverseLeft (myFrameA.getPose(), T1W);
         }
         else {
            System.out.println (
               "WARNING: cannot apply non-rigid transforms to FrameSprings");
         }
      }
      if (!frameBTransforming || !gtr.isRigid()) {
         // need to update myX2B
         if (myFrameB == null) {
            gtr.transform (myX2B);
         }
         else if (!context.isTransformed(myFrameB)) {
            RigidTransform3d T2W = new RigidTransform3d();
            T2W.mul (myFrameB.getPose(), myX2B);
            gtr.transform (T2W);
            myX2B.mulInverseLeft (myFrameB.getPose(), T2W);
         }
         else {
            System.out.println (
               "WARNING: cannot apply non-rigid transforms to FrameSprings");
         }
      }
   }

   /* --- ScalableUnits --- */

   public void scaleDistance (double s) {
      if (myMaterial != null) {
         myMaterial.scaleDistance (s);
      }
      myRotaryStiffness *= (s * s);
      myRotaryDamping *= (s * s);
      if (myX1A != RigidTransform3d.IDENTITY) {
         myX1A.p.scale (s);
      }
      if (myX2B != RigidTransform3d.IDENTITY) {
         myX2B.p.scale (s);
      }
   }

   public void scaleMass (double s) {
      if (myMaterial != null) {
         myMaterial.scaleMass (s);
      }
      myStiffness *= s;
      myDamping *= s;
      myRotaryStiffness *= s;
      myRotaryDamping *= s;
   }

   public void setJacobianSymmetric (boolean symmetric) {
      mySymmetricJacobian = symmetric;
   }

   public boolean isJacobianSymmetric () {
      return mySymmetricJacobian;
   }

   public int getJacobianType() {
      if (mySymmetricJacobian) {
         return Matrix.SYMMETRIC;
      }
      else {
         return 0;
      }
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();     
      if (scanAndStoreReference (rtok, "frameA", tokens)) {
         return true;
      }
      else if (scanAndStoreReference (rtok, "frameB", tokens)) {
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      pw.println ("frameA=" +
                  ComponentUtils.getWritePathName (ancestor, myFrameA));
      pw.println ("frameB=" +
                  ComponentUtils.getWritePathName (ancestor, myFrameB));
      super.writeItems (pw, fmt, ancestor);
   }

   protected boolean postscanItem (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (postscanAttributeName (tokens, "frameA")) {
         setFrameA (postscanReference (tokens, Frame.class, ancestor));
         return true;
      }
      else if (postscanAttributeName (tokens, "frameB")) {
         setFrameB (postscanReference (tokens, Frame.class, ancestor));
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }

   @Override
   public void getHardReferences (List<ModelComponent> refs) {
      super.getHardReferences (refs);
      if (myFrameA != null) {
         refs.add (myFrameA);
      }
      if (myFrameB != null) {
         refs.add (myFrameB);
      }
   }

   /**
    * {@inheritDoc}
    */
   public boolean isDuplicatable() {
      // can't return true until we finish implementing copy function,
      // which means we need to make Frames copyable
      return false;
   }

   /**
    * {@inheritDoc}
    */
   public boolean getCopyReferences (
      List<ModelComponent> refs, ModelComponent ancestor) {
      if (myFrameA != null) {
         if (!ComponentUtils.addCopyReferences (refs, myFrameA, ancestor)) {
            return false;
         }
      }
      if (myFrameB != null) {
         if (!ComponentUtils.addCopyReferences (refs, myFrameB, ancestor)) {
            return false;
         }
      }
      return true;
   }

     @Override
   public ModelComponent copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      FrameSpring comp =
         (FrameSpring)super.copy (flags, copyMap);
      if (myRenderProps != null) {
         comp.setRenderProps (myRenderProps);
      }

      if (myX1A != RigidTransform3d.IDENTITY) {
         comp.myX1A = new RigidTransform3d (myX1A);
      }
      if (myX2B != RigidTransform3d.IDENTITY) {
         comp.myX2B = new RigidTransform3d (myX2B);
      }
      comp.myF = new Wrench();
      comp.myFTmp = new Wrench();

      comp.myVel1 = new Twist();
      comp.myVel2 = new Twist();
      comp.myVel21 = new Twist();

      comp.myRenderFrame = new RigidTransform3d();
      comp.myRenderPnt1 = new float[3];
      comp.myRenderPnt2 = new float[3];
   
      comp.myTmpM = new Matrix3d();
      // comp.myRBA = new RotationMatrix3d();

      comp.myX21 = new RigidTransform3d();

      comp.myMaterial = (FrameMaterial)myMaterial.clone();
      return comp;
   } 

}
