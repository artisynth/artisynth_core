/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.util.LinkedList;
import java.util.Map;

import maspack.matrix.Matrix3d;
import maspack.matrix.MatrixBlock;
import maspack.matrix.Matrix1x3;
import maspack.matrix.Matrix1x3Block;
import maspack.matrix.Matrix3x3Block;
import maspack.matrix.MatrixBlockBase;
import maspack.matrix.SparseNumberedBlockMatrix;
import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.Vector3d;
import maspack.properties.PropertyList;
import maspack.render.Renderer;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.util.DataBuffer;
import artisynth.core.materials.AxialMaterial;
import artisynth.core.materials.AxialMuscleMaterial;
import artisynth.core.materials.LinearAxialMaterial;
import artisynth.core.materials.MaterialBase;
import artisynth.core.materials.MaterialChangeEvent;
import artisynth.core.modelbase.DynamicActivityChangeEvent;
import artisynth.core.modelbase.HasNumericState;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.RenderableComponent;
import artisynth.core.modelbase.RenderableComponentBase;
import artisynth.core.util.ScalableUnits;

/**
 * Base class for springs based on two or more points
 */
public abstract class PointSpringBase extends Spring
   implements RenderableComponent, ScalableUnits, HasNumericState {

   public static boolean myIgnoreCoriolisInJacobian = true;
   public static boolean useMaterial = true;

   public static PropertyList myProps =
      new PropertyList (PointSpringBase.class, Spring.class);

   protected AxialMaterial myMaterial;
   // if myMaterial implements HasNumericState, myStateMat is set to its value
   // as a cached reference for use in implementing HasNumericState
   protected HasNumericState myStateMat;
   
   private static double DEFAULT_REST_LENGTH = 0;
   
   protected double myRestLength = DEFAULT_REST_LENGTH;
   
   static {
      myProps.add ("renderProps * *", "renderer properties", null);
//      myProps.add ("stiffness * *", "linear spring stiffness", 0);
//      myProps.add ("damping * *", "linear spring damping", 0);
      myProps.add (
         "restLength", "rest length of the spring", DEFAULT_REST_LENGTH);
      myProps.addReadOnly ("length *", "current spring length");
      myProps.addReadOnly ("lengthDot *", "current spring length time derivative");
      myProps.add (
         "material", "spring material parameters", createDefaultMaterial(), "CE");
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   protected void setDefaultValues() {
      setRestLength (DEFAULT_REST_LENGTH);
      setMaterial (createDefaultMaterial());
   }

   public PointSpringBase (String name) {
      super (name);
   }

   public static AxialMaterial createDefaultMaterial() {
      // allow null materials
      //return new LinearAxialMaterial();
      return null;
   }

   public AxialMaterial getMaterial() {
      return myMaterial;
   }

   public void setLinearMaterial (double k, double d) {
      setMaterial (new LinearAxialMaterial (k, d));
   }
   
   public <T extends AxialMaterial> void setMaterial (T mat) {
      AxialMaterial oldMat = myMaterial;
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

   public AxialMaterial getEffectiveMaterial() {
      if (myMaterial != null) {
         return myMaterial;
      }
      ModelComponent parent = getParent();
      if (parent instanceof PointSpringList) {
         return ((PointSpringList<?>)parent).getMaterial();
      }
      else {
         return null;
      }
   }

   public abstract double getLength();
   
   public abstract double getLengthDot();
   
   /**
    * Sets the rest length of the spring from the current point locations
    * @return the new rest length
    */
   public abstract double setRestLengthFromPoints();
   
   public double getRestLength() {
      return myRestLength;
   }

   public void setRestLength (double l) {
      myRestLength = l;
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
      return RenderProps.createPointLineProps (this);
   }

   public void prerender (RenderList list) {
      // nothing to do
   }

   public abstract void updateBounds (Vector3d pmin, Vector3d pmax);

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

   public abstract void render (Renderer renderer, int flags);

   public void getSelection (LinkedList<Object> list, int qid) {
   }
   
   /* ======== End renderable implementation ======= */

   /**
    * Computes the tension F acting along the unit vector from the first to the
    * second particle.
    *
    * @param l spring length
    * @param ldot spring length derivative
    * @return force tension
    */
   public double computeF (double l, double ldot) {
      return computeF (l, ldot, 0);
   }

   /**
    * Computes the tension F acting along the unit vector from the first to the
    * second particle.
    *
    * @param l spring length
    * @param ldot spring length derivative
    * @param ex excitation applied to the muscle material
    * @return force tension
    */
   public double computeF (double l, double ldot, double ex) {
      AxialMaterial mat = getEffectiveMaterial();
      if (mat != null) {
         return mat.computeF (l, ldot, myRestLength, ex);
      }
      else {
         return 0;
      }
   }

   /**
    * Computes the derivative of the tension F (acting along the unit vector
    * from the first to the second particle) with respect to spring length.
    * 
    * @param l spring length
    * @param ldot spring length derivative
    * @return force tension derivative with respect to length
    */
   public double computeDFdl (double l, double ldot) {
      return computeDFdl (l, ldot, 0);
   }

   /**
    * Computes the derivative of the tension F (acting along the unit vector
    * from the first to the second particle) with respect to spring length.
    * 
    * @param l spring length
    * @param ldot spring length derivative
    * @param ex excitation applied to the muscle material
    * @return force tension derivative with respect to length
    */
   public double computeDFdl (double l, double ldot, double ex) {
      AxialMaterial mat = getEffectiveMaterial();
      if (mat != null) {
         return mat.computeDFdl (l, ldot, myRestLength, ex);
      }
      else {
         return 0;
      }
   }

   /**
    * Computes the derivative of the tension F (acting along the unit vector
    * from the first to the second particle)with respect to the time derivative
    * of spring length.
    * 
    * @param l spring length
    * @param ldot spring length derivative
    * @return force tension derivative with respect to length time derivative
    */
   public double computeDFdldot (double l, double ldot) {
      return computeDFdldot (l, ldot, 0);
   }

   /**
    * Computes the derivative of the tension F (acting along the unit vector
    * from the first to the second particle)with respect to the time derivative
    * of spring length.
    * 
    * @param l spring length
    * @param ldot spring length derivative
    * @param ex excitation applied to the muscle material
    * @return force tension derivative with respect to length time derivative
    */
   public double computeDFdldot (double l, double ldot, double ex) {
      AxialMaterial mat = getEffectiveMaterial();
      if (mat != null) {
         return mat.computeDFdldot (l, ldot, myRestLength, ex);
      }
      else {
         return 0;
      }
   }

   protected class SegmentData {
      public Point pnt0;
      public Point pnt1;
      protected MatrixBlock blk00;
      protected MatrixBlock blk11;
      protected MatrixBlock blk01;
      protected MatrixBlock blk10;
      protected int blk00Num;
      protected int blk11Num;
      protected int blk01Num;
      protected int blk10Num;      
      public Vector3d uvec;
      public double len;
      public Matrix3d P;
      public boolean isActive;

      public SegmentData (Point p0, Point p1) {
         pnt0 = p0;
         pnt1 = p1;
         uvec = new Vector3d();
         P = new Matrix3d();
         isActive = true;
      }

      public double updateU () {
         uvec.sub (pnt1.getPosition(), pnt0.getPosition());
         len = uvec.norm();
         if (len != 0) {
            uvec.scale (1 / len);
         }
         return len;
      }

      // assumes that uvec is up to date
      public double getLengthDot () {
         Vector3d vel1 = pnt1.getVelocity();
         Vector3d vel0 = pnt0.getVelocity();
         double dvx = vel1.x-vel0.x;
         double dvy = vel1.y-vel0.y;
         double dvz = vel1.z-vel0.z;
         return uvec.x*dvx + uvec.y*dvy + uvec.z*dvz;
      } 

      // assumes that uvec is up to date
      public void updateP () {
         P.outerProduct (uvec, uvec);
         P.negate();
         P.m00 += 1;
         P.m11 += 1;
         P.m22 += 1;
      }

      public void addSolveBlocks (SparseNumberedBlockMatrix M) {
         int bi0 = pnt0.getSolveIndex();
         int bi1 = pnt1.getSolveIndex();

         if (bi0 != -1 && bi1 != -1) {
            if ((blk01 = M.getBlock (bi0, bi1)) == null) {
               blk01 = MatrixBlockBase.alloc (3, 3);
               M.addBlock (bi0, bi1, blk01);
            }
            blk01Num = blk01.getBlockNumber();
            if ((blk10 = M.getBlock (bi1, bi0)) == null) {
               blk10 = MatrixBlockBase.alloc (3, 3);
               M.addBlock (bi1, bi0, blk10);
            }
            blk10Num = blk10.getBlockNumber();
         }
         else {
            blk01 = null;
            blk10 = null;
            blk01Num = -1;
            blk10Num = -1;
         }
         blk00 = (bi0 != -1 ? M.getBlock (bi0, bi0) : null);
         blk11 = (bi1 != -1 ? M.getBlock (bi1, bi1) : null);
         blk00Num = (bi0 != -1 ? M.getBlock(bi0, bi0).getBlockNumber() : -1);
         blk11Num = (bi1 != -1 ? M.getBlock(bi1, bi1).getBlockNumber() : -1);
      }

      protected void addVelJacobian (
         SparseNumberedBlockMatrix M, double s, double dFdldot, Matrix3d T) {
         computeForceVelocityJacobian (T, dFdldot);
         T.scale (s);
         addToJacobianBlocks (M, T);
      }

      protected void addPosJacobian (
         SparseNumberedBlockMatrix M, double s, double F, double dFdl, 
         double dFdldot, double len, Matrix3d T) {
         computeForcePositionJacobian (
            T, F, dFdl, dFdldot, len, myIgnoreCoriolisInJacobian);
         T.scale (s);
         addToJacobianBlocks (M, T);
      }

      protected void addMinForcePosJacobian (
         SparseBlockMatrix J, double s, double dFdl, double dFdldot,
         double len, boolean staticOnly, int bi) {

         Matrix1x3 K = new Matrix1x3();
         computeTensionPositionJacobian (
            K, dFdl, dFdldot, len, /*symmetric=*/false);
         K.scale (s);
         addToMinForceJacobianBlocks (J, K, bi);
      }

      protected void addMinForceVelJacobian (
         SparseBlockMatrix J, double s, double dFdldot, int bi) {

         Matrix1x3 D = new Matrix1x3();
         computeTensionVelocityJacobian (D, dFdldot);
         D.scale (s);
         addToMinForceJacobianBlocks (J, D, bi);
      }

      protected void addToMinForceJacobianBlocks (
         SparseBlockMatrix J, Matrix1x3 K, int bi) {

         int bj;
         if ((bj = pnt0.getSolveIndex()) != -1) {
            MatrixBlock blk = J.getBlock (bi, bj);
            if (blk == null) {
               blk = new Matrix1x3Block();
               J.addBlock (bi, bj, blk);
            }
            blk.add (K);
         }
         if ((bj = pnt1.getSolveIndex()) != -1) {
            MatrixBlock blk = J.getBlock (bi, bj);
            if (blk == null) {
               blk = new Matrix1x3Block();
               J.addBlock (bi, bj, blk);
            }
            blk.sub (K);
         }
      }

      protected void addMinForceJacobian (
         SparseBlockMatrix J, double F, double dFdl, double dFdldot,
         double len, double h, boolean staticOnly, int bi) {

         Matrix3d K = new Matrix3d();
         Matrix3d D = null;
         int bj = 0;
         computeForcePositionJacobian (
            K, F, dFdl, dFdldot, len, /*symmetric=*/false);
         K.scale (h);
         if (!staticOnly) {
            D = new Matrix3d();
            computeForceVelocityJacobian (D, dFdldot);
         }
         if ((bj = pnt0.getSolveIndex()) != -1) {
            MatrixBlock blk = J.getBlock (bi, bj);
            if (blk == null) {
               blk = new Matrix3x3Block();
               J.addBlock (bi, bj, blk);
            }
            blk.add (K);
            if (D != null) {
               blk.add (D);
            }
         }
         if ((bj = pnt1.getSolveIndex()) != -1) {
            MatrixBlock blk = J.getBlock (bi, bj);
            if (blk == null) {
               blk = new Matrix3x3Block();
               J.addBlock (bi, bj, blk);
            }
            blk.sub (K);
            if (D != null) {
               blk.sub (D);
            }
         }
      }

      protected void addToJacobianBlocks (
         SparseNumberedBlockMatrix S, Matrix3d M) {

         if (blk00Num != -1) {
            S.getBlockByNumber(blk00Num).add (M);
         }
         if (blk11Num != -1) {
            S.getBlockByNumber(blk11Num).add (M);
         }               
         if (blk01Num != -1) {
            S.getBlockByNumber(blk01Num).sub (M);
         }
         if (blk10Num != -1) {
            S.getBlockByNumber(blk10Num).sub (M);
         }
      }

      void computeForcePositionJacobian (
         Matrix3d T, double F, double dFdl, double dFdldot,
         double totalLen, boolean symmetric) {

         if (totalLen == 0 || len == 0) {
            T.setZero();
            return;
         }

         // components of the open product u * u':
         double uxx = uvec.x * uvec.x;
         double uyy = uvec.y * uvec.y;
         double uzz = uvec.z * uvec.z;
         double uxy = uvec.x * uvec.y;
         double uxz = uvec.x * uvec.z;
         double uyz = uvec.y * uvec.z;

         // Now compute T = (-dFdldot * u * (vel1-vel0)' - F I) / length
         if (!symmetric && dFdldot != 0) {
            Vector3d vel1 = pnt1.getVelocity();
            Vector3d vel0 = pnt0.getVelocity();

            double scale = -dFdldot/totalLen;
            double dvx = scale*(vel1.x-vel0.x);
            double dvy = scale*(vel1.y-vel0.y);
            double dvz = scale*(vel1.z-vel0.z);

            T.m00 = uvec.x*dvx;
            T.m10 = uvec.y*dvx;
            T.m20 = uvec.z*dvx;
            T.m01 = uvec.x*dvy;
            T.m11 = uvec.y*dvy;
            T.m21 = uvec.z*dvy;
            T.m02 = uvec.x*dvz;
            T.m12 = uvec.y*dvz;
            T.m22 = uvec.z*dvz;
         }
         else {
            T.setZero();
         }
         T.m00 -= F / totalLen;
         T.m11 -= F / totalLen;
         T.m22 -= F / totalLen;

         // form the product T * (I - u u')
         double m00 = -T.m00 * (uxx - 1) - T.m01 * uxy - T.m02 * uxz;
         double m11 = -T.m10 * uxy - T.m11 * (uyy - 1) - T.m12 * uyz;
         double m22 = -T.m20 * uxz - T.m21 * uyz - T.m22 * (uzz - 1);

         double m01 = -T.m00 * uxy - T.m01 * (uyy - 1) - T.m02 * uyz;
         double m02 = -T.m00 * uxz - T.m01 * uyz - T.m02 * (uzz - 1);
         double m12 = -T.m10 * uxz - T.m11 * uyz - T.m12 * (uzz - 1);

         double m10 = -T.m10 * (uxx - 1) - T.m11 * uxy - T.m12 * uxz;
         double m20 = -T.m20 * (uxx - 1) - T.m21 * uxy - T.m22 * uxz;
         double m21 = -T.m20 * uxy - T.m21 * (uyy - 1) - T.m22 * uyz;

         // finally, add -dFdl* u * u' to final result

         if (dFdl != 0) {
            T.m00 = m00 - dFdl * uxx;
            T.m11 = m11 - dFdl * uyy;
            T.m22 = m22 - dFdl * uzz;

            T.m01 = m01 - dFdl * uxy;
            T.m02 = m02 - dFdl * uxz;
            T.m12 = m12 - dFdl * uyz;

            T.m10 = m10 - dFdl * uxy;
            T.m20 = m20 - dFdl * uxz;
            T.m21 = m21 - dFdl * uyz;
         }
         else {
            T.m00 = m00;
            T.m11 = m11;
            T.m22 = m22;

            T.m01 = m01;
            T.m02 = m02;
            T.m12 = m12;

            T.m10 = m10;
            T.m20 = m20;
            T.m21 = m21;
         }
      }      

      void computeForceVelocityJacobian (Matrix3d T, double dFdldot) {
         if (len == 0 || dFdldot == 0) {
            T.setZero();
            return;
         }
         T.outerProduct (uvec, uvec);
         T.scale (-dFdldot);
      }

      void computeTensionPositionJacobian (
         Matrix1x3 T, double dFdl, double dFdldot,
         double totalLen, boolean symmetric) {

         if (totalLen == 0 || len == 0) {
            T.setZero();
            return;
         }

         // components of the open product u * u':
         double uxx = uvec.x * uvec.x;
         double uyy = uvec.y * uvec.y;
         double uzz = uvec.z * uvec.z;
         double uxy = uvec.x * uvec.y;
         double uxz = uvec.x * uvec.z;
         double uyz = uvec.y * uvec.z;

         // Now compute T = (-dFdldot * (vel1-vel0)' - F I) / length
         if (!symmetric && dFdldot != 0) {
            Vector3d vel1 = pnt1.getVelocity();
            Vector3d vel0 = pnt0.getVelocity();

            double scale = -dFdldot/totalLen;
            T.m00 = scale*(vel1.x-vel0.x);
            T.m01 = scale*(vel1.y-vel0.y);
            T.m02 = scale*(vel1.z-vel0.z);
         }
         else {
            T.setZero();
         }

         // form the product T * (I - u u')
         double m00 = -T.m00 * (uxx - 1) - T.m01 * uxy - T.m02 * uxz;
         double m01 = -T.m00 * uxy - T.m01 * (uyy - 1) - T.m02 * uyz;
         double m02 = -T.m00 * uxz - T.m01 * uyz - T.m02 * (uzz - 1);

         // finally, add -dFdl * u' to final result

         if (dFdl != 0) {
            T.m00 = m00 - dFdl * uvec.x;
            T.m01 = m01 - dFdl * uvec.y;
            T.m02 = m02 - dFdl * uvec.z;
         }
         else {
            T.m00 = m00;
            T.m01 = m01;
            T.m02 = m02;
         }
      }      

      public void computeTensionVelocityJacobian (Matrix1x3 T, double dFdldot) {
         if (len == 0 || dFdldot == 0) {
            T.setZero();
            return;
         }
         T.scale (-dFdldot, uvec);
      }
   }

   public float[] getRenderColor() {
      return null;
   }
   
   public void scaleDistance (double s) {
      myRestLength *= s;
   }

   public void scaleMass (double s) {
      // nothing to do here
   }
   
   public static void setStiffness (PointSpringBase s, double k) {
      if (s.getMaterial() instanceof LinearAxialMaterial) {
         LinearAxialMaterial mat = 
            (LinearAxialMaterial)s.getMaterial().clone();
         mat.setStiffness (k);
         s.setMaterial (mat);
      }
      else {
         System.out.println (
            "Warning: setStiffness(): no stiffness in spring material");
      }
   }
   
   public static void setDamping (PointSpringBase s, double d) {
      if (s.getMaterial() instanceof LinearAxialMaterial) {
         LinearAxialMaterial mat = 
            (LinearAxialMaterial)s.getMaterial().clone();
         mat.setDamping (d);
         s.setMaterial (mat);
      }
      else if (s.getMaterial() instanceof AxialMuscleMaterial) {
         AxialMuscleMaterial mat = 
            (AxialMuscleMaterial)s.getMaterial().clone();
         mat.setDamping (d);
         s.setMaterial (mat);
      }
      else {
         System.out.println (
            "Warning: setDamping(): no damping in spring material");
      }
   }   

   public static void setMaxForce (PointSpringBase s, double maxf) {
      if (s.getMaterial() instanceof AxialMuscleMaterial) {
         AxialMuscleMaterial mat = 
            (AxialMuscleMaterial)s.getMaterial().clone();
         mat.setMaxForce (maxf);
         s.setMaterial (mat);
      }
      else {
         System.out.println (
            "Warning: setMaxForce(): no maxForce in spring material");
      }
   }   

   public static double getMaxForce (PointSpringBase s) {
      if (s.getMaterial() instanceof AxialMuscleMaterial) {
         return ((AxialMuscleMaterial)s.getMaterial()).getMaxForce();
      }
      else {
         return 0;
      }
   }   

   /* --- Begin HasNumericState interface --- */

   public void advanceState (double t0, double t1) {
      if (myStateMat != null) {
         myStateMat.advanceState (t0, t1);
      }
   }
   
   public void getState (DataBuffer data) {
      if (myStateMat != null) {
         myStateMat.getState (data);
      }
   }

   public void setState (DataBuffer data) {
      if (myStateMat != null) {
         myStateMat.setState (data);
      }
   }

   public boolean hasState() {
      return (myStateMat != null);
   }
   
   /* --- End HasNumericState interface --- */
  
   public ModelComponent copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      PointSpringBase comp = (PointSpringBase)super.copy (flags, copyMap);
      comp.setRestLength (myRestLength);
      if (myRenderProps != null) {
         comp.setRenderProps (myRenderProps);
      }      
      return comp;
   }   

}
