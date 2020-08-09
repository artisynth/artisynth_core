package artisynth.core.mechmodels;

import java.util.ArrayList;

import maspack.matrix.*;

/**
 * Methods to support the computation of stiffness matrices where the
 * orientation derivatives for Frames are represented with respect to
 * yaw-pitch-roll instead of angular displacement. The latter have the
 * advantage of being symmetric if they arise solely from conservative forces.
 */
public class YPRStiffnessUtils {

   private static void preMulCTranspose (MatrixBlock blk, Matrix3d C) {
      if (blk instanceof Matrix6d) {
         Matrix6d M = (Matrix6d)blk;
         Matrix3d T = new Matrix3d();
         M.getSubMatrix30 (T);
         T.mulTransposeLeft (C, T);
         M.setSubMatrix30 (T);
         M.getSubMatrix33 (T);
         T.mulTransposeLeft (C, T);
         M.setSubMatrix33 (T);
      }
      else if (blk instanceof Matrix6x3) {
         Matrix6x3 M = (Matrix6x3)blk;
         Matrix3d T = new Matrix3d();
         M.getSubMatrix30 (T);
         T.mulTransposeLeft (C, T);
         M.setSubMatrix30 (T);
      }
      else {
      }
   }

   private static void postMulC (MatrixBlock blk, Matrix3d C) {
      if (blk instanceof Matrix6d) {
         Matrix6d M = (Matrix6d)blk;
         Matrix3d T = new Matrix3d();
         M.getSubMatrix03 (T);
         T.mul (C);
         M.setSubMatrix03 (T);
         M.getSubMatrix33 (T);
         T.mul (C);
         M.setSubMatrix33 (T);
      }
      else if (blk instanceof Matrix3x6) {
         Matrix3x6 M = (Matrix3x6)blk;
         Matrix3d T = new Matrix3d();
         M.getSubMatrix03 (T);
         T.mul (C);
         M.setSubMatrix03 (T);
      }
      else {
      }
   }

   private static void addD (MatrixBlock blk, Matrix3d D) {
      if (blk instanceof Matrix6d) {
         Matrix6d M = (Matrix6d)blk;
         Matrix3d T = new Matrix3d();
         M.getSubMatrix33 (T);
         T.add (D);
         M.setSubMatrix33 (T);
      }
      else {
      }
   }

   /**
    * Converts a system force vector to one that uses yaw-pitch-roll
    * displacements.
    *
    * @param fr converted force vector
    * @param f0 original force vector (can be the same as fr)
    * @param mech MechModel associated with the system
    */
   public static void convertActiveForcesToYPR (
      VectorNd fr, VectorNd f0, MechModel mech) {

      if (f0.size() != mech.getActiveVelStateSize()) {
         throw new IllegalArgumentException (
            "f0.size "+ f0.size() +
            " not equal to active velocity size "+mech.getActiveVelStateSize());
      }
      fr.set (f0); // default - just copy
      int k = 0;
      Vector3d fx = new Vector3d();
      for (int bi=0; bi<mech.numActiveComponents(); bi++) {
         DynamicComponent dc = mech.getDynamicComponents().get(bi);
         if (dc instanceof Frame && dc.getVelStateSize() == 6) {
            Matrix3d C = createC((Frame)dc);
            fr.getSubVector (k+3, fx);
            C.mulTranspose (fx, fx);
            fr.setSubVector (k+3, fx);
         }
         k += dc.getVelStateSize();
      }
   }

   /**
    * Converts a system velocity vector from one that uses yaw-pitch-roll
    * displacements.
    *
    * @param vr converted velocities
    * @param v0 original velocites (can be the same as vr)
    * @param mech MechModel associated with the system
    */
   public static void convertActiveVelocitiesFromYPR (
      VectorNd vr, VectorNd v0, MechModel mech) {

      if (v0.size() != mech.getActiveVelStateSize()) {
         throw new IllegalArgumentException (
            "v0.size "+ v0.size() +
            " not equal to active velocity size "+mech.getActiveVelStateSize());
      }
      vr.set (v0); // default - just copy
      int k = 0;
      Vector3d vx = new Vector3d();
      for (int bi=0; bi<mech.numActiveComponents(); bi++) {
         DynamicComponent dc = mech.getDynamicComponents().get(bi);
         if (dc instanceof Frame && dc.getVelStateSize() == 6) {
            Matrix3d C = createC((Frame)dc);
            vr.getSubVector (k+3, vx);
            C.mul (vx, vx);
            vr.setSubVector (k+3, vx);
         }
         k += dc.getVelStateSize();
      }
   }

   private static Matrix3d createC (Frame frame) {
      double[] rpy = new double[3];
      frame.getPose().R.getRpy (rpy);

      Matrix3d C = new Matrix3d();

      double cp = Math.cos(rpy[0]);
      double sp = Math.sin(rpy[0]);
      double ct = Math.cos(rpy[1]);
      double st = Math.sin(rpy[1]);

      C.m00 = ct*cp;
      C.m01 = -sp;
      C.m10 = ct*sp;
      C.m11 = cp;
      C.m20 = -st;
      C.m22 = 1.0;
      return C;
   }

   private static Matrix3d createD (Frame frame, Vector3d m) {
      double[] rpy = new double[3];
      frame.getPose().R.getRpy (rpy);

      Matrix3d D = new Matrix3d();

      double cp = Math.cos(rpy[0]);
      double sp = Math.sin(rpy[0]);
      double ct = Math.cos(rpy[1]);
      double st = Math.sin(rpy[1]);

      // Matrix3d dCTp = new Matrix3d();
      // Matrix3d dCTr = new Matrix3d();

      // dCTp.m00 = -st*cp;
      // dCTp.m01 = -st*sp;
      // dCTp.m02 = -ct;

      // dCTr.m00 = -ct*sp;
      // dCTr.m01 = ct*cp;
      // dCTr.m10 = -cp;
      // dCTr.m11 = -sp;

      // System.out.println ("dCTp=\n" + dCTp.toString("%12.8f"));
      // System.out.println ("dCTr=\n" + dCTr.toString("%12.8f"));

      D.m01 = -st*cp*m.x - st*sp*m.y - ct*m.z;
      D.m02 = -ct*sp*m.x + ct*cp*m.y;
      D.m12 = -cp*m.x - sp*m.y;
      return D;
   }


   /**
    * Converts, in place, a stiffness matrix {@code K} and a (optional) force
    * vector {@code f} from an angular displacement formulation to one using
    * yaw-pitch-roll displacements. If {@code f} is not {@code null}, it
    * supplies the forces used for the conversion. Otherwise, these force are
    * obtained directly from the components.
    *
    * @param K matrix to convert
    * @param f if not {@code null}, force vector to convert
    * @param comps dynamic components associated with each matrix block
    * entry
    */
   public static void convertStiffnessToYPR (
      SparseBlockMatrix K, VectorNd f,
      ArrayList<DynamicComponent> comps) {
      
      if (f.size() != K.rowSize()) {
         throw new IllegalArgumentException (
            "f size "+f.size()+" incompatible with K size "+K.getSize());
      }
      if (comps.size() != K.numBlockRows()) {
         throw new IllegalArgumentException (
            "comps size "+comps.size()+" incompatible with K block size "+
            K.numBlockRows() + " X " + K.numBlockCols());
      }

      // first identify all blocks that belong to frames and compute the C and
      // D matrices for them
      Matrix3d[] Cmats = new Matrix3d[K.numBlockCols()];
      Matrix3d[] Dmats = new Matrix3d[K.numBlockCols()];
      Vector3d m = new Vector3d(); // moment vector
      for (int bi=0; bi<K.numBlockRows(); bi++) {
         DynamicComponent dc = comps.get(bi);
         // check vel state size to eliminate deformable bodies
         if (dc instanceof Frame && dc.getVelStateSize() == 6) {
            Frame frame = (Frame)dc;
            //System.out.println ("frame=" + frame.getName());
            Cmats[bi] = createC (frame);
            if (f != null) {
               // use f to obtain forces used for creating the derivative
               // matrix D, and then modify D itself
               int moffset = K.getBlockRowOffset(bi)+3;
               f.getSubVector (moffset, m);
               Dmats[bi] = createD (frame, m);
               Cmats[bi].mulTranspose (m, m);
               f.setSubVector (moffset, m);
            }
            else {
               m.set (frame.getForce().m);
               Dmats[bi] = createD (frame, m);
            }
            // System.out.println ("m=" + m.toString("%12.2f"));
            // System.out.println ("D=\n" + Dmats[bi].toString("%12.6f"));
            // System.out.println ("C=\n" + Cmats[bi].toString("%12.8f"));
         }
      }

      for (int bi=0; bi<K.numBlockRows(); bi++) {
         MatrixBlock blk;
         for (blk = K.firstBlockInRow(bi); blk != null; blk =blk.next()) {
            int bj = blk.getBlockCol();
            if (Cmats[bi] != null) {
               preMulCTranspose (blk, Cmats[bi]);
            }
            if (Cmats[bj] != null) {
               postMulC (blk, Cmats[bj]);
            }
            if (Cmats[bi] != null && bi == bj) {
               addD (blk, Dmats[bi]);
            }
         }
      }
   }
      

}
