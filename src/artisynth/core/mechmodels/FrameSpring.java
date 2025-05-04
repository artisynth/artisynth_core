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
import artisynth.core.modelbase.HasNumericState;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.ModelComponentBase;
import artisynth.core.modelbase.RenderableComponent;
import artisynth.core.modelbase.RenderableComponentBase;
import artisynth.core.modelbase.TransformGeometryContext;
import artisynth.core.modelbase.TransformGeometryAction;
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
import maspack.util.DataBuffer;
import maspack.util.ReaderTokenizer;

public class FrameSpring extends Spring
   implements RenderableComponent, ScalableUnits, TransformableGeometry,
              CopyableComponent, ForceTargetComponent, HasNumericState {

   public static boolean mySymmetricJacobian = true;

   protected Frame myFrameA;
   protected Frame myFrameB;
   protected boolean debug = false;

   /**
    * 1 is the attachment frame for A, and 2 is the attachment frame for B
    */
   protected RigidTransform3d myTCA = new RigidTransform3d();
   protected RigidTransform3d myTDB = new RigidTransform3d();

   protected double myStiffness;
   protected double myDamping;
   protected double myRotaryStiffness;
   protected double myRotaryDamping;

   protected Wrench myF = new Wrench();
   protected Wrench myFTmp = new Wrench();

   private Twist myVel20 = new Twist();

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

   protected static final boolean DEFAULT_USE_TRANSFORM_DC = true;
   protected boolean myUseTransformDC = DEFAULT_USE_TRANSFORM_DC;
   
   protected static final boolean DEFAULT_APPLY_REST_POSE = false;
   protected boolean myApplyRestPose = DEFAULT_APPLY_REST_POSE;
   
   private Matrix3d myTmpM = new Matrix3d();
   // private RotationMatrix3d myRBA = new RotationMatrix3d();

   protected RigidTransform3d myTD = new RigidTransform3d(); //net displacement
   protected RigidTransform3d myRestPose = new RigidTransform3d();
   protected boolean myHasRestPose = false;

   public static PropertyList myProps =
      new PropertyList (FrameSpring.class, ModelComponentBase.class);

   protected FrameMaterial myMaterial;
   // if myMaterial implements HasNumericState, myStateMat is set to its value
   // as a cached reference for use in implementing HasNumericState
   protected HasNumericState myStateMat;

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
      myProps.add (
         "useTransformDC",
         "compute forces use the transform D-to-C instead of C-to-D",
         DEFAULT_USE_TRANSFORM_DC);
      myProps.add (
         "applyRestPose",
         "automatically account for the rest pose in force calculations",
         DEFAULT_APPLY_REST_POSE);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public FrameSpring() {
      this (null);
   }

   public FrameSpring (String name) {
      super (name);
      setRenderProps (createRenderProps());
      setMaterial (createDefaultMaterial());
   }

   public FrameSpring (
      String name, double k, double kRot, double d, double dRot) {
      super (name);
      setRenderProps (createRenderProps());
      setMaterial (new RotAxisFrameMaterial (k, kRot, d, dRot));
      myStiffness = k;
      myDamping = d;
      myRotaryStiffness = kRot;
      myRotaryDamping = dRot;
   }

   public FrameSpring (String name, FrameMaterial mat) {
      super (name);
      setRenderProps (createRenderProps());
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

   public void setUseTransformDC (boolean enable) {
      myUseTransformDC = enable;
   }
   
   public boolean getUseTransformDC() {
      return myUseTransformDC;
   }

   /**
    * Sets whether or not this spring automatically applies the rest pose. If
    * {@code true}, then any specified rest pose will be automaticallu removed
    * from the displacement frame passed to the spring's {@link
    * FrameMaterial}. If {@code false}, then no modification will be made to
    * rhe displacement frame and the handling of the rest pose will be left to
    * the material. The value is currently {@code false}, but this will likely
    * be changed to {@code true} in the future.
    *
    * @param enable if {@code true}, any rest pose will be automatically
    * applied
    */
   public void setApplyRestPose (boolean enable) {
      myApplyRestPose = enable;
   }
   
   /**
    * Queries whether or not this spring automatically applies the
    * rest pose. See {@link #setApplyRestPose}.
    *
    * @return {@code true} if this spring applies the rest pose
    */
   public boolean getApplyRestPose() {
      return myApplyRestPose;
   }

   /**
    * @deprecated Use {@link #setRestPose} instead.
    */   
   public void setInitialT21 (RigidTransform3d T21) {
      setRestPose (T21);
   }

   /**
    * @deprecated Use {@link #getRestPose} instead.
    */   
   public RigidTransform3d getInitialT21 () {
      return getRestPose();
   }

   /**
    * @deprecated Use {@link #initializeRestPose} instead.
    */
   public void setInitialT21 () {
      initializeRestPose();
   }
 
   /**
    * @deprecated Use {@link #setRestPose} instead.
    */   
   public void setTDC0 (RigidTransform3d TDC0) {
      setRestPose (TDC0);
   }

   /**
    * @deprecated Use {@link #getRestPose} instead.
    */   
   public RigidTransform3d getTDC0 () {
      return getRestPose();
   }

   /**
    * @deprecated Use {@link #initializeRestPose} instead.
    */
   public void initializeTDC0 () {
      initializeRestPose();
   }

   /**
    * Sets the value of the rest pose. By default, this is the identity
    * transform. Otherwise, it defines an additional frame R between frames D
    * and C which is used to compute the frame forces.
    *
    * <p> If {@link #getUseTransformDC} returns {@code true}, the forces are
    * nominally generated by the transform {@code TDC} from frame D to C.  If
    * the rest pose is not the identity, then forces are instead defined by the
    * transform from D to R, and the rest pose itself represents the transform
    * from R to C.
    *
    * <p> If {@link #getUseTransformDC} returns {@code false}, the forces are
    * nominally generated by the transform {@code TCD} from frame C to D.  If
    * the rest pose is not the identity, then forces are instead defined by the
    * transform from C to R, and the rest pose itself represents the transform
    * from R to D.
    *
    * @param TR new rest pose value
    */
   public void setRestPose (RigidTransform3d TR) {
      myRestPose.set (TR);
      myHasRestPose = !myRestPose.isIdentity();
   }

   /**
    * Returns the transform that defines the rest pose. See {@link
    * #setRestPose} for more details.
    *
    * @return rest pose transform
    */
   public RigidTransform3d getRestPose () {
      return myRestPose;
   }

   /**
    * Initializes the rest pose such that the current displacement between
    * frames D and C will generate no forces. This entails setting the rest
    * pose to the transform from D to C (if {@link #getUseTransformDC} returns
    * {@code true}), or C to D (if {@link #getUseTransformDC} returns {@code
    * false}). See {@link #setRestPose}.
    */
   public void initializeRestPose () {
      computeNetDisplacement (myRestPose);
      myHasRestPose = !myRestPose.isIdentity();
   }

   private void computeNetDisplacement (RigidTransform3d TD) {
      if (myUseTransformDC) {
         TD.mulInverseBoth (myTCA, myFrameA.getPose());
         if (myFrameB != null) {
            TD.mul (myFrameB.getPose());
         }
         TD.mul (myTDB);
      }
      else {
         TD.invert (myTDB);
         if (myFrameB != null) {
            TD.mulInverseRight (TD, myFrameB.getPose());
         }
         TD.mul (myFrameA.getPose());
         TD.mul (myTCA);
      }
   }      

   /* ======== Renderable implementation ======= */

   protected RenderProps myRenderProps;

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
      myRenderFrame.mul (myTCA);
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
      myRenderFrame.mul (myTDB);
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
      if (mat instanceof HasNumericState) {
         // use getMaterial() since mat may have been copied
         myStateMat = (HasNumericState)getMaterial();
      }
      else {
         myStateMat = null;
      }     
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

   public void setAttachFrameA (RigidTransform3d T1A) {
      myTCA = new RigidTransform3d (T1A);
   }

   public RigidTransform3d getAttachFrameA() {
      return myTCA;
   }

   /**
    * Returns the current pose of the C frame, in world coordinates.
    * 
    * @return current pose of C
    */
   public RigidTransform3d getCurrentTCW() {
      RigidTransform3d TCW = new RigidTransform3d();
      getCurrentTCW (TCW);
      return TCW;
   }
 
   /**
    * Returns the current pose of the C frame, in world coordinates.
    * 
    * @param TCW returns the current pose of C
    */  
   public void getCurrentTCW (RigidTransform3d TCW) {
      TCW.mul (myFrameA.getPose(), myTCA);
   }

  public void setAttachFrameB (RigidTransform3d T2B) {
      myTDB = new RigidTransform3d (T2B);
   }

   public RigidTransform3d getAttachFrameB() {
      return myTDB;
   }

   /**
    * Returns the current pose of the D frame, in world coordinates.
    * 
    * @return current pose of D
    */
   public RigidTransform3d getCurrentTDW() {
      RigidTransform3d TDW = new RigidTransform3d();
      getCurrentTDW (TDW);
      return TDW;
   }

   /**
    * Returns the current pose of the D frame, in world coordinates.
    * 
    * @param TDW returns the current pose of D
    */  
   public void getCurrentTDW (RigidTransform3d TDW) {
      if (myFrameB != null) {
         TDW.mul (myFrameB.getPose(), myTDB);
      }
      else {
         TDW.set (TDW);
      }
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
      myTCA = new RigidTransform3d (T1A);
      myFrameB = frameB;
      myTDB = new RigidTransform3d (T2B);
   }
   
   public Wrench getSpringForce() {
	   return myF;
   }

   private void computeRelativeDisplacements () {
      // positions 
      computeNetDisplacement (myTD);
      if (myApplyRestPose && myHasRestPose) {
         myTD.mulInverseLeft (myRestPose, myTD);
      }

      // velocities
      Twist velA = new Twist();
      Twist velB = new Twist();
      myFrameA.getBodyVelocity (velA);
      velA.inverseTransform (myTCA);
      if (myFrameB != null) {
         myFrameB.getBodyVelocity (velB);
         velB.inverseTransform (myTDB);
      }
      if (myUseTransformDC) {
         if (myApplyRestPose && myHasRestPose) {
            velA.inverseTransform (myRestPose);
         }
         velB.transform (myTD.R);
         myVel20.sub (velB, velA);
      }
      else {
         if (myFrameB != null && myApplyRestPose && myHasRestPose) {
            velB.inverseTransform (myRestPose);
         }
         velA.transform (myTD.R);
         myVel20.sub (velA, velB);
      }
   }

   // Computes the force as seen in frame 1 and places the result in myF.
   protected void computeForces () {

      FrameMaterial mat = myMaterial;

      if (mat != null) { // just in case implementation allows null material ...
         computeRelativeDisplacements();
         mat.computeF (myF, myTD, myVel20, myRestPose);
      }
   }
    
   public void applyForces (double t) {
      computeForces();

      Wrench fA = new Wrench();
      Wrench fB = new Wrench();

      if (myUseTransformDC) {
         fA.set (myF);
         if (myApplyRestPose && myHasRestPose) {
            fA.transform (myRestPose);
         }
         if (myFrameB != null) {
            fB.inverseTransform (myTD.R, myF);
            fB.negate();
         }
      }
      else {
         fA.inverseTransform (myTD.R, myF);
         fA.negate();
         if (myFrameB != null) {
            fB.set (myF);
            if (myApplyRestPose && myHasRestPose) {
               fB.transform (myRestPose);
            }
         }
      }

      fA.transform (myTCA);
      fA.transform (myFrameA.getPose().R); // rotate to world coords
      myFrameA.addForce (fA);

      if (myFrameB != null) {
         fB.transform (myTDB);
         fB.transform (myFrameB.getPose().R); // rotate to world coords
         myFrameB.addForce (fB);
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
      
      if (myUseTransformDC) {
         computePosJacobianWorld (
            mat, s, s, blk00, blk01, blk10, blk11, mySymmetricJacobian);
      }
      else {
         computePosJacobianWorld (
            mat, s, s, blk11, blk10, blk01, blk00, mySymmetricJacobian);
      }
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
      RotationMatrix3d R0W = new RotationMatrix3d();

      RigidTransform3d TAW = myFrameA.getPose();
      RigidTransform3d TBW;
      if (myFrameB != null) {
         TBW = myFrameB.getPose();
      }
      else {
         TBW = RigidTransform3d.IDENTITY;
      }

      Point3d p0 = new Point3d();      
      Point3d p2 = new Point3d();
      Vector3d x20 = new Vector3d();
      Vector3d angVel1 = new Vector3d();
      Vector3d angVel2 = new Vector3d();

      if (myApplyRestPose && myHasRestPose) {
         p0.set (myRestPose.p);
      }
      if (myUseTransformDC) {
         R0W.mul (TAW.R, myTCA.R);
         p0.transform (myTCA);
         p0.transform (TAW.R);
         p2.transform (TBW.R, myTDB.p);
         angVel1.set (myFrameA.getVelocity().w);
         if (myFrameB != null) {
            angVel2.set (myFrameB.getVelocity().w);
         }        
      }
      else {
         R0W.mul (TBW.R, myTDB.R);
         p0.transform (myTDB);
         p0.transform (TBW.R);
         p2.transform (TAW.R, myTCA.p);
         angVel2.set (myFrameA.getVelocity().w);
         if (myFrameB != null) {
            angVel1.set (myFrameB.getVelocity().w);
         }
      }
      if (myApplyRestPose && myHasRestPose) {
         R0W.mul (myRestPose.R);
      }

      Matrix3d T = myTmpM;
      
      computeRelativeDisplacements();

      Twist vel20 = (sd != 0.0 ? myVel20 : Twist.ZERO);

      x20.set (myTD.p);
      x20.transform (R0W);

      if (!symmetric) {
         // compute forces for assymetric force component
         mat.computeF (myF, myTD, vel20, myRestPose);
         if (myApplyRestPose && myHasRestPose) {
            //myF.transform (myRestPose);
         }
         // map forces back to World coords
         myF.transform (R0W);
         if (sd != 0) {
            JD = new Matrix6d();
            mat.computeDFdu (
               JD, myTD, vel20, myRestPose, /*symmetric=*/false);
            if (myApplyRestPose && myHasRestPose) {
               // Vector3d p0 = new Vector3d(myRestPose.p);
               // p0.inverseTransform (myRestPose.R);
               // JD.crossProductTransform (p0, JD);
            }
            JD.transform (R0W);
         }
      }
      mat.computeDFdq (JK, myTD, vel20, myRestPose, symmetric);
      if (myApplyRestPose && myHasRestPose) {
         // Vector3d p0 = new Vector3d(myRestPose.p);
         // p0.inverseTransform (myRestPose.R);
         // //p0.negate();
         // JK.crossProductTransform (p0, JK);
         // System.out.println ("JK xprod=\n"+JK.toString ("%10.6f"));
         // System.out.println ("JK xprod symmetric=" + JK.isSymmetric (1e-10));
      }
      JK.transform (R0W);
      JK.scale (-sk);
      vel20.transform (R0W);

      Matrix6d tmp00 = new Matrix6d();
      Matrix6d tmp11 = new Matrix6d();
      Matrix6d tmp01 = new Matrix6d();
      Matrix6d tmp10 = new Matrix6d();     
      
      if (blk00 != null) {
         tmp00.set (JK);
         postMulMoment (tmp00, p0);
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
         postMulMoment (tmp10, p0);
      }
    
      if (blk00 != null) {
         if (!symmetric) {
            
            // QK term
            postMulMoment (tmp00, x20);          

            // QF(0,0) term
            setScaledCrossProd (T, -sk, myF.f);
            tmp00.addSubMatrix03 (T);
            setScaledCrossProd (T, -sk, myF.m);
            tmp00.addSubMatrix33 (T);
            setScaledCrossProd (T, sk, p0);
            T.crossProduct (myF.f, T);
            tmp00.addSubMatrix33 (T);

            // QD term
            if (sd != 0) {
               postMulWrenchCross (tmp00, JD, sd, vel20);
               setScaledCrossProd (T, sd, p0);
               T.crossProduct (angVel1, T);
               postMul03Block (tmp00, JD, T);
            }
         }
         preMulMoment (tmp00, p0);
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
               T.crossProduct (angVel2, T);
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
               T.crossProduct (angVel2, T);
               postMul03Block (tmp01, JD, T);
            }
            
         }
         preMulMoment (tmp01, p0);
         blk01.add (tmp01);
      }
      if (blk10 != null) {
         if (!symmetric) {
            // QK term
            postMulMoment (tmp10, x20);

            // QF(1,0) term
            setScaledCrossProd (T, sk, myF.f);
            tmp10.addSubMatrix03 (T);
            setScaledCrossProd (T, sk, myF.m);
            tmp10.addSubMatrix33 (T);

            // QD term
            if (sd != 0) {
               postMulWrenchCross (tmp10, JD, -sd, vel20);
               setScaledCrossProd (T, -sd, p0);
               T.crossProduct (angVel1, T);
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

      if (myUseTransformDC) {
         computeVelJacobianWorld (mat, s, blk00, blk01, blk10, blk11);
      }
      else {
         computeVelJacobianWorld (mat, s, blk11, blk10, blk01, blk00);
      }
   }

   public void computeVelJacobianWorld (
      FrameMaterial mat, double sd,
      Matrix6d blk00, Matrix6d blk01, 
      Matrix6d blk10, Matrix6d blk11) {
      Matrix6d D = new Matrix6d();
      RotationMatrix3d R0W = new RotationMatrix3d();

      RigidTransform3d TAW = myFrameA.getPose();
      RigidTransform3d TBW;
      if (myFrameB != null) {
         TBW = myFrameB.getPose();
      }
      else {
         TBW = RigidTransform3d.IDENTITY;
      }

      Point3d p0 = new Point3d();      
      Point3d p2 = new Point3d();
      if (myApplyRestPose && myHasRestPose) {
         p0.set (myRestPose.p);
      }     
      if (myUseTransformDC) {
         R0W.mul (TAW.R, myTCA.R);
         p0.transform (myTCA);
         p0.transform (TAW.R);
         p2.transform (TBW.R, myTDB.p);
      }
      else {
         R0W.mul (TBW.R, myTDB.R);
         p0.transform (myTDB);
         p0.transform (TBW.R);
         p2.transform (TAW.R, myTCA.p);
      }
      if (myApplyRestPose && myHasRestPose) {
         R0W.mul (myRestPose.R);
      }

      Matrix6d tmp00 = new Matrix6d();
      Matrix6d tmp11 = new Matrix6d();
      Matrix6d tmp01 = new Matrix6d();
      Matrix6d tmp10 = new Matrix6d();     
      
      computeRelativeDisplacements();
      mat.computeDFdu (D, myTD, myVel20, myRestPose, mySymmetricJacobian);
      if (myApplyRestPose && myHasRestPose) {
         // Vector3d p0 = new Vector3d(myRestPose.p);
         // p0.inverseTransform (myRestPose.R);
         // D.crossProductTransform (p0, D);
      }
      D.transform (R0W);
      D.scale (-sd);
      if (blk00 != null) {
         tmp00.set (D);
         postMulMoment (tmp00, p0);
         preMulMoment (tmp00, p0);
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
            preMulMoment (tmp01, p0);
            blk01.add (tmp01);
         }
         if (blk10 != null) {
            tmp10.set (D);
            postMulMoment (tmp10, p0);
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
            mat.computeF (myF, myTD, myVel20, myRestPose);
            if (myApplyRestPose && myHasRestPose) {
               myF.transform (myRestPose);
            }
         }
         else {
            mat.computeF (myF, myTD, Twist.ZERO, myRestPose);
            if (myApplyRestPose && myHasRestPose) {
               myF.transform (myRestPose);
            }
         }
         if (myUseTransformDC) {
            myFTmp.transform (myTCA, myF);
            myFTmp.transform (myFrameA.getPose().R); // rotate to world coords
         }
         else {
            myFTmp.transform (myTDB, myF);
            if (myFrameB != null) {
               myFTmp.transform (myFrameB.getPose().R); // rotate to world coords
            }
         }
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

   public int transformPriority() {
      return 1;
   }

   /**
    * Updates the TCA transform after frameA has been transformed.
    */
   private class UpdateTCA implements TransformGeometryAction {

      RigidTransform3d myTCW;

      UpdateTCA (RigidTransform3d TCW) {
         myTCW = new RigidTransform3d (TCW);
      }

      public void transformGeometry (
         GeometryTransformer gtr, TransformGeometryContext context, int flags) {
         myTCA.mulInverseLeft (myFrameA.getPose(), myTCW);
      }
   }

   /**
    * Updates the TDB transform after frameA has been transformed.
    */
   private class UpdateTDB implements TransformGeometryAction {

      RigidTransform3d myTDW;

      UpdateTDB (RigidTransform3d TDW) {
         myTDW = new RigidTransform3d (TDW);
      }

      public void transformGeometry (
         GeometryTransformer gtr, TransformGeometryContext context, int flags) {
         myTDB.mulInverseLeft (myFrameB.getPose(), myTDW);
      }
   }

   public void transformGeometry (AffineTransform3dBase T) {
      TransformGeometryContext.transform (this, T, 0);
   }

   public void addTransformableDependencies (
      TransformGeometryContext context, int flags) {
      // nothing to add
   }

   private RigidTransform3d getTXW (Frame frame, RigidTransform3d TXF) {
      RigidTransform3d TXW = new RigidTransform3d();
      TXW.mul (frame.getPose(), TXF);
      return TXW;
   }

   public void transformGeometry (
      GeometryTransformer gtr, TransformGeometryContext context, int flags) {
      // Quantities that may need to be changed include: T1A, T2B and
      // InitialT21.
      // 
      // Transforming a FrameSpring is similar to transforming a
      // BodyConnector. However, we do not currently handle cases where one or
      // both frames are transforming while the FrameSpring is not.  That's
      // because we don't have a quick way to tell what FrameSprings are
      // connected to a Frame.
      //
      // Because a FrameSpring has higher transforming priority than Frames, we
      // know that this method will be called before any of the associated
      // frames have been transformed.
      boolean frameATransforming = context.contains(myFrameA);
      boolean frameBTransforming = (myFrameB!=null && context.contains(myFrameB));

      if (!gtr.isRigid() && myHasRestPose) {
         // need to transform InitialT21
         if (gtr.isRestoring()) {
            myRestPose.set (gtr.restoreObject (myRestPose));
         }
         else {
            if (gtr.isSaving()) {
               gtr.saveObject (new RigidTransform3d (myRestPose));
            }
            RigidTransform3d T1W;
            if (myUseTransformDC) {
               T1W = getCurrentTCW();
            }
            else {
               T1W = getCurrentTDW();
            }
            RigidTransform3d T0W = new RigidTransform3d();
            T0W.mul (T1W, myRestPose);
            gtr.computeTransform (T1W);
            gtr.computeTransform (T0W);
            myRestPose.mulInverseLeft (T1W, T0W);
         }
         myHasRestPose = !myRestPose.isIdentity();
      }

      if (!frameATransforming || !gtr.isRigid()) {
         // need to update myTCA
         if (gtr.isRestoring()) {
            myTCA.set (gtr.restoreObject (myTCA));
         }
         else {
            if (gtr.isSaving()) {
               gtr.saveObject (new RigidTransform3d (myTCA));
            }
            RigidTransform3d TCW = getCurrentTCW();
            gtr.computeTransform (TCW);
            context.addAction (new UpdateTCA (TCW));
         }
      }
      if (!frameBTransforming || !gtr.isRigid()) {
         // need to update myTDB
         if (myFrameB == null) {
            gtr.computeTransform (myTDB);
         }
         else {
            if (gtr.isRestoring()) {
               myTDB.set (gtr.restoreObject (myTDB));
            }
            else {
               if (gtr.isSaving()) {
                  gtr.saveObject (new RigidTransform3d (myTDB));
               }
               RigidTransform3d TDW = getCurrentTDW();
               gtr.computeTransform (TDW);
               context.addAction (new UpdateTDB (TDW));
            }
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
      if (myTCA != RigidTransform3d.IDENTITY) {
         myTCA.p.scale (s);
      }
      if (myTDB != RigidTransform3d.IDENTITY) {
         myTDB.p.scale (s);
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

   /* --- Begin HasNumericState interface --- */

   /**
    * {@inheritDoc}
    */
   public boolean hasState() {
      return (myStateMat != null && myStateMat.hasState());
   }

   /**
    * {@inheritDoc}
    */
   public int getStateVersion() {
      if (myStateMat != null) {
         return myStateMat.getStateVersion();
      }
      else {
         return 0;
      }
   }

   /**
    * {@inheritDoc}
    */
   public void getState (DataBuffer data) {
      if (myStateMat != null) {
         myStateMat.getState (data);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void setState (DataBuffer data) {
      if (myStateMat != null) {
         myStateMat.setState (data);
      }
   }

   /**
    * {@inheritDoc}
    */
   public boolean requiresAdvance() {
      if (myStateMat != null) {
         return myStateMat.requiresAdvance();
      }
      else {
         return false;
      }
   }   
   
   /**
    * {@inheritDoc}
    */
   public void advanceState (double t0, double t1) {
      if (myStateMat != null) {
         myStateMat.advanceState (t0, t1);
      }
   }

   /**
    * {@inheritDoc}
    */
   public int numAuxVars () {
      if (myStateMat != null) {
         return myStateMat.numAuxVars();
      }
      else {
         return 0;
      }
   }

   /**
    * {@inheritDoc}
    */
   public int getAuxVarState (double[] buf, int idx) {
      if (myStateMat != null) {
         return myStateMat.getAuxVarState (buf, idx);
      }
      else {
         return idx;
      }
   }

   /**
    * {@inheritDoc}
    */
   public int setAuxVarState (double[] buf, int idx) {
      if (myStateMat != null) {
         return myStateMat.setAuxVarState (buf, idx);
      }
      else {
         return idx;
      }
   }

   /**
    * {@inheritDoc}
    */
   public int getAuxVarDerivative (double[] buf, int idx) {
      if (myStateMat != null) {
         return myStateMat.getAuxVarDerivative (buf, idx);
      }
      else {
         return idx;
      }
   }
   
   /* --- End HasNumericState interface --- */

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

      Frame frameA = (Frame)ComponentUtils.maybeCopy (flags, copyMap, myFrameA);  
      comp.setFrameA (frameA);

      Frame frameB = (Frame)ComponentUtils.maybeCopy (flags, copyMap, myFrameB);  
      comp.setFrameA (frameB);

      if (myTCA != RigidTransform3d.IDENTITY) {
         comp.myTCA = new RigidTransform3d (myTCA);
      }
      if (myTDB != RigidTransform3d.IDENTITY) {
         comp.myTDB = new RigidTransform3d (myTDB);
      }
      comp.myF = new Wrench();
      comp.myFTmp = new Wrench();
      comp.myVel20 = new Twist();

      comp.myRenderFrame = new RigidTransform3d();
      comp.myRenderPnt1 = new float[3];
      comp.myRenderPnt2 = new float[3];
   
      comp.myTmpM = new Matrix3d();
      // comp.myRBA = new RotationMatrix3d();

      comp.myTD = new RigidTransform3d();
      comp.myRestPose = new RigidTransform3d(myRestPose);
      comp.myHasRestPose = !comp.myRestPose.isIdentity();

      comp.myMaterial = (FrameMaterial)myMaterial.clone();
      return comp;
   } 

}
