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
import maspack.matrix.MatrixBlockBase;
import maspack.matrix.Point3d;
import maspack.matrix.SparseNumberedBlockMatrix;
import maspack.matrix.Vector3d;
import maspack.properties.PropertyList;
import maspack.render.GLRenderable;
import maspack.render.GLRenderer;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import artisynth.core.materials.AxialMaterial;
import artisynth.core.materials.AxialMuscleMaterial;
import artisynth.core.materials.LinearAxialMaterial;
import artisynth.core.materials.MaterialBase;
import artisynth.core.modelbase.DynamicActivityChangeEvent;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.RenderableComponent;
import artisynth.core.modelbase.RenderableComponentBase;
import artisynth.core.util.ScalableUnits;

/**
 * Base class for springs based on two or more points
 */
public abstract class PointSpringBase extends Spring
   implements RenderableComponent, ScalableUnits {

   public static boolean myIgnoreCoriolisInJacobian = true;
   public static boolean useMaterial = true;

   public static PropertyList myProps =
      new PropertyList (PointSpringBase.class, Spring.class);

   protected AxialMaterial myMaterial;
   
   private static double DEFAULT_REST_LENGTH = 0;
   
   protected double myRestLength = DEFAULT_REST_LENGTH;
   
   static {
      myProps.add ("renderProps * *", "renderer properties", null);
//      myProps.add ("stiffness * *", "linear spring stiffness", 0);
//      myProps.add ("damping * *", "linear spring damping", 0);
      myProps.add (
         "restLength", "rest length of the spring", DEFAULT_REST_LENGTH);
      myProps.addReadOnly ("length *", "current spring length");
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
   
   public void setMaterial (AxialMaterial mat) {
      myMaterial = (AxialMaterial)MaterialBase.updateMaterial (
         this, "material", myMaterial, mat);
      // issue DynamicActivityChange in case solve matrix symmetry has changed:
      notifyParentOfChange (DynamicActivityChangeEvent.defaultEvent);
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

//   public double getStiffness() {
//      if (myMaterial instanceof LinearAxialMaterial) {
//         return ((LinearAxialMaterial)myMaterial).getStiffness();
//      }
//      else {
//         return 0;
//      }
//   }
//
//   public void setStiffness (double k) {
//      if (myMaterial instanceof LinearAxialMaterial) {
//         ((LinearAxialMaterial)myMaterial).setStiffness(k);
//      }
//      else {
//         setMaterial (new LinearAxialMaterial (k, 0));
//      }
//   }

//   public double getDamping() {
//      if (myMaterial instanceof LinearAxialMaterial) {
//         return ((LinearAxialMaterial)myMaterial).getDamping();
//      }
//      else {
//         return 0;
//      }
//   }
//
//   public void setDamping (double d) {
//      if (myMaterial instanceof LinearAxialMaterial) {
//         ((LinearAxialMaterial)myMaterial).setDamping(d);
//      }
//      else {
//         setMaterial (new LinearAxialMaterial (0, d));
//      }
//   }
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

   public abstract void updateBounds (Point3d pmin, Point3d pmax);

   public int getRenderHints() {
      int code = 0;
      if (myRenderProps != null && myRenderProps.getAlpha() != 1) {
         code |= TRANSLUCENT;
      }
      return code;
   }

   public boolean isSelectable() {
      return true;
   }

   public int numSelectionQueriesNeeded() {
      return -1;
   }

   public abstract void render (GLRenderer renderer, int flags);

   public void getSelection (LinkedList<Object> list, int qid) {
   }
   
   /* ======== End renderable implementation ======= */

   /**
    * Computes the force magnitude acting along the unit vector from the first
    * to the second particle.
    * 
    * @return force magnitude
    */
   public double computeF (double l, double ldot) {
      AxialMaterial mat = getEffectiveMaterial();
      if (mat != null) {
         return mat.computeF (l, ldot, myRestLength, 0);
      }
      else {
         return 0;
      }
   }

   /**
    * Computes the derivative of spring force magnitude (acting along the unit
    * vector from the first to the second particle) with respect to spring
    * length.
    * 
    * @return force magnitude derivative with respect to length
    */
   public double computeDFdl (double l, double ldot) {
      AxialMaterial mat = getEffectiveMaterial();
      if (mat != null) {
         return mat.computeDFdl (l, ldot, myRestLength, 0);
      }
      else {
         return 0;
      }
   }


   /**
    * Computes the derivative of spring force magnitude (acting along the unit
    * vector from the first to the second particle)with respect to the time
    * derivative of spring length.
    * 
    * @return force magnitude derivative with respect to length time derivative
    */
   public double computeDFdldot (double l, double ldot) {
      AxialMaterial mat = getEffectiveMaterial();
      if (mat != null) {
         return mat.computeDFdldot (l, ldot, myRestLength, 0);
      }
      else {
         return 0;
      }
   }

   protected class SegmentData {
      public Point pnt0;
      public Point pnt1;
      MatrixBlock blk00;
      MatrixBlock blk11;
      MatrixBlock blk01;
      MatrixBlock blk10;
      int blk00Num;
      int blk11Num;
      int blk01Num;
      int blk10Num;      
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

      void addVelJacobian (
         SparseNumberedBlockMatrix M, double s, double dFdldot, Matrix3d T) {
         computeForceVelocityJacobian (T, dFdldot);
         T.scale (s);
         addToJacobianBlocks (M, T);
      }

      void addPosJacobian (
         SparseNumberedBlockMatrix M, double s, double F, double dFdl, 
         double dFdldot, double len, Matrix3d T) {
         computeForcePositionJacobian (T, F, dFdl, dFdldot, len);
         T.scale (s);
         addToJacobianBlocks (M, T);
      }

      void addToJacobianBlocks (SparseNumberedBlockMatrix S, Matrix3d M) {

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
         Matrix3d T, double F, double dFdl, double dFdldot, double totalLen) {

         if (len == 0) {
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
         if (!myIgnoreCoriolisInJacobian && dFdldot != 0) {
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

      public void computeForceVelocityJacobian (Matrix3d T, double dFdldot) {
         if (len == 0 || dFdldot == 0) {
            T.setZero();
            return;
         }
         T.outerProduct (uvec, uvec);
         T.scale (-dFdldot);
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
