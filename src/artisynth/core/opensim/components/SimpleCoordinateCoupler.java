package artisynth.core.opensim.components;

import java.util.*;

import maspack.matrix.*;
import maspack.spatialmotion.*;
import maspack.render.*;
import maspack.render.Renderer.AxisDrawStyle;

import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.*;
import artisynth.core.opensim.customjoint.*;
import artisynth.core.mechmodels.MechSystem.ConstraintInfo;

public class SimpleCoordinateCoupler extends ConstrainerBase {

   OpenSimCustomJoint myJoint0;
   int myCoordNum0;
   OpenSimCustomJoint myJoint1;
   int myCoordNum1;

   double myOffset;
   double myLambda;

   public SimpleCoordinateCoupler() {
   }

   public SimpleCoordinateCoupler (
      OpenSimCustomJoint joint0, int coordNum0,
      OpenSimCustomJoint joint1, int coordNum1) {

      myJoint0 = joint0;
      myCoordNum0 = coordNum0;
      myJoint1 = joint1;
      myCoordNum1 = coordNum1;

      double coord0 = myJoint0.getCoordinate(myCoordNum0);
      double coord1 = myJoint1.getCoordinate(myCoordNum1);
      myOffset = coord0-coord1;
      System.out.println ("coord0=" + coord0);
      System.out.println ("coord1=" + coord1);
      System.out.println ("offset=" + myOffset);
   }

   public void getBilateralSizes (VectorNi sizes) {
      sizes.append (1);
   }

   public int addBilateralConstraints (
      SparseBlockMatrix GT, VectorNd dg, int numb) {

      Matrix6x1 GC = new Matrix6x1();
      int bj = GT.numBlockCols();
      Wrench wr;
      wr = myJoint0.getCoordinateWrenchG (myCoordNum0);
      myJoint0.computeConstraintMatrixA (GC, wr, 1);
      myJoint0.addMasterBlocks (GT, bj, GC, myJoint0.getFrameAttachmentA());
      myJoint0.computeConstraintMatrixB (GC, wr, -1);
      myJoint0.addMasterBlocks (GT, bj, GC, myJoint0.getFrameAttachmentB());

      wr = myJoint1.getCoordinateWrenchG (myCoordNum1);
      myJoint1.computeConstraintMatrixA (GC, wr, -1);
      myJoint1.addMasterBlocks (GT, bj, GC, myJoint1.getFrameAttachmentA());
      myJoint1.computeConstraintMatrixB (GC, wr, 1);
      myJoint1.addMasterBlocks (GT, bj, GC, myJoint1.getFrameAttachmentB());

      if (dg != null) {
         dg.set (numb, 0);
      }
      return numb + 1;
   }

   public int getBilateralInfo (ConstraintInfo[] ginfo, int idx) {
      ConstraintInfo gi = ginfo[idx];
      double coord0 = myJoint0.getCoordinate(myCoordNum0);
      double coord1 = myJoint1.getCoordinate(myCoordNum1);
      gi.dist = (coord0-coord1);
      gi.compliance = 0;
      gi.damping = 0;
      gi.force = 0;
      idx++;
      return idx;
   }

   public int setBilateralForces (VectorNd lam, double s, int idx) {
      myLambda = s*lam.get (idx++);
      return idx;
   }

   public int getBilateralForces (VectorNd lam, int idx) {
      lam.set (idx++, myLambda);
      return idx;
   }
   
   public void zeroForces() {
      myLambda = 0;
   }

   public double updateConstraints (double t, int flags) {
      // nothing to do here, since the needed quantities are updated in the
      // joint components.
      return 0;
   }
   
   public void getConstrainedComponents (List<DynamicComponent> list) {
      myJoint0.getConstrainedComponents (list);
      myJoint1.getConstrainedComponents (list);
   }

   /* --- begin (empty) renderable implementation --- */

   public void updateBounds (Vector3d pmin, Vector3d pmax) {
   }

   public RenderProps createRenderProps() {
      return new RenderProps();
   }

   public void prerender (RenderList list) {
   }

   public void render (Renderer renderer, int flags) {
   }

   /* --- end renderable implementation --- */

}
