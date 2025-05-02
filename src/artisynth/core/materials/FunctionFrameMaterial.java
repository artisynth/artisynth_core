package artisynth.core.materials;

import java.io.PrintWriter;
import java.io.IOException;
import java.util.Deque;

import artisynth.core.util.ScanToken;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.ScanWriteUtils;

import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix6d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.Vector3d;
import maspack.matrix.Vector3i;
import maspack.util.*;
import maspack.function.*;
import maspack.properties.PropertyList;
import maspack.spatialmotion.Twist;
import maspack.spatialmotion.Wrench;

/**
 * A FrameMaterial that is linear with respect to translation and X-Y-Z
 * rotation angles.
 */
public class FunctionFrameMaterial extends FrameMaterial {

   protected Diff1Function1x1Base[] myTransFxns = new Diff1Function1x1Base[3];
   protected Diff1Function1x1Base[] myRotFxns = new Diff1Function1x1Base[3];

   protected Vector3d myD = new Vector3d();
   protected Vector3d myRotD = new Vector3d();

   public static PropertyList myProps =
      new PropertyList (FunctionFrameMaterial.class, FrameMaterial.class);

   static {
      myProps.add ("damping * *", "linear spring damping", Vector3d.ZERO);
      myProps.add ("rotaryDamping * *", "linear spring damping", Vector3d.ZERO);
   }   

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }  

   public Diff1Function1x1 getTranslationalFunction (int idx) {
      if (idx < 0 || idx > 2) {
         throw new IllegalArgumentException (
            "function index "+idx+" must be in the range [0,2]");
      }
      return myTransFxns[idx];
   }

   public void setTranslationalFunctions (Diff1Function1x1Base fxn) {
      for (int i=0; i<3; i++) {
         myTransFxns[i] = fxn.clone();
      }
   }

   public void setTranslationalFunction (int idx, Diff1Function1x1Base fxn) {
      if (idx < 0 || idx > 2) {
         throw new IllegalArgumentException (
            "function index "+idx+" must be in the range [0,2]");
      }
      myTransFxns[idx] = fxn.clone();
   }

   public Vector3d getDamping() {
      return myD;
   }

   public void setDamping (double d) {
      myD.set (d, d, d);
   }

   public void setDamping (double dx, double dy, double dz) {
      myD.set (dx, dy, dz);
   }

   public void setDamping (Vector3d dvec) {
      myD.set (dvec);
   }

   public Diff1Function1x1 getRotaryFunction (int idx) {
      if (idx < 0 || idx > 2) {
         throw new IllegalArgumentException (
            "function index "+idx+" must be in the range [0,2]");
      }
      return myRotFxns[idx];
   }

   public void setRotaryFunctions (Diff1Function1x1Base fxn) {
      for (int i=0; i<3; i++) {
         myRotFxns[i] = fxn.clone();
      }
   }

   public void setRotaryFunction (int idx, Diff1Function1x1Base fxn) {
      if (idx < 0 || idx > 2) {
         throw new IllegalArgumentException (
            "function index "+idx+" must be in the range [0,2]");
      }
      myRotFxns[idx] = fxn.clone();
   }

   public Vector3d getRotaryDamping() {
      return myRotD;
   }

   public void setRotaryDamping (double d) {
      myRotD.set (d, d, d);
   }

   public void setRotaryDamping (double dx, double dy, double dz) {
      myRotD.set (dx, dy, dz);
   }

   public void setRotaryDamping (Vector3d dvec) {
      myRotD.set (dvec);
   }

   public FunctionFrameMaterial () {
      setTranslationalFunctions (new ConstantFunction1x1(0));
      setRotaryFunctions (new ConstantFunction1x1(0));
   }

   public FunctionFrameMaterial (
      Diff1Function1x1Base transFunc, Diff1Function1x1Base rotFunc,
      double d, double dr) {
      setTranslationalFunctions (transFunc);
      setRotaryFunctions (rotFunc);
      setDamping (d);
      setRotaryDamping (dr);
   }

   public void computeF (
      Wrench wr, RigidTransform3d X21, Twist vel21, 
      RigidTransform3d initialX21) {

      // translational forces
      Vector3d p = X21.p;
      wr.f.x = myTransFxns[0].eval (p.x);
      wr.f.y = myTransFxns[1].eval (p.y);
      wr.f.z = myTransFxns[2].eval (p.z);

      // get the x-y-z angles for computing the rotational forces
      RotationMatrix3d R = X21.R;
      double[] angs = new double[3];
      R.getXyz (angs);
      double cx = Math.cos(angs[0]);
      double sx = Math.sin(angs[0]);
      double cy = Math.cos(angs[1]);
      double sy = Math.sin(angs[1]);
      if (Math.abs(cy) < 1e-6) {
         // handle singularity
         cy = (cy > 0 ? 1e-6 : -1e-6);
      }

      // matrix Hinv maps angular velocity to x-y-z angle speeds
      double hi21 = -sx/cy;
      double hi22 = cx/cy;
      Matrix3d Hinv = new Matrix3d();
      Hinv.set (1, -sy*hi21, -sy*hi22, 0, cx, sx, 0, hi21, hi22);

      // compute generalized force f in angle space
      Vector3d f = new Vector3d();
      f.x = myRotFxns[0].eval (angs[0]);
      f.y = myRotFxns[1].eval (angs[1]);
      f.z = myRotFxns[2].eval (angs[2]);
      // map f to frame moment using transpose of Hinv
      Hinv.mulTranspose (wr.m, f);

      // damping forces - currently computing using angular velocity
      // instead of angle speeds
      Vector3d v = vel21.v;
      Vector3d w = vel21.w;

      wr.f.x += myD.x*v.x;
      wr.f.y += myD.y*v.y;
      wr.f.z += myD.z*v.z;

      wr.m.x += myRotD.x*w.x;
      wr.m.y += myRotD.y*w.y;
      wr.m.z += myRotD.z*w.z;
   }

   public void computeDFdq (
      Matrix6d Jq, RigidTransform3d X21, Twist vel21, 
      RigidTransform3d initialX21, boolean symmetric) {

      Jq.setZero();

      // translational stiffness
      Vector3d p = X21.p;
      Jq.m00 = myTransFxns[0].evalDeriv (p.x);
      Jq.m11 = myTransFxns[1].evalDeriv (p.y);
      Jq.m22 = myTransFxns[2].evalDeriv (p.z);

      // get the x-y-z angles for computing the rotational forces
      RotationMatrix3d R = X21.R;
      double[] angs = new double[3];
      R.getXyz (angs);
      double cx = Math.cos(angs[0]);
      double sx = Math.sin(angs[0]);
      double cy = Math.cos(angs[1]);
      double sy = Math.sin(angs[1]);
      double ty;
      if (Math.abs(cy) < 1e-6) {
         // handle singularity
         cy = (cy > 0 ? 1e-6 : -1e-6);
      }
      ty = sy/cy;

      // matrix Hinv maps angular velocity to x-y-z angle speeds
      double hi21 = -sx/cy;
      double hi22 = cx/cy;
      Matrix3d Hinv = new Matrix3d();
      Hinv.set (1, -sy*hi21, -sy*hi22, 0, cx, sx, 0, hi21, hi22);

      // compute generalized force f in angle space
      Vector3d f = new Vector3d();
      f.x = myRotFxns[0].eval (angs[0]);
      f.y = myRotFxns[1].eval (angs[1]);
      f.z = myRotFxns[2].eval (angs[2]);
      // compute generalized force derivatives in angle space
      Vector3d fderivs = new Vector3d();
      fderivs.x = myRotFxns[0].evalDeriv (angs[0]);
      fderivs.y = myRotFxns[1].evalDeriv (angs[1]);
      fderivs.z = myRotFxns[2].evalDeriv (angs[2]);

      // compute rotary stiffness from f, fderivs, and Hinv:
      Matrix3d Jr = new Matrix3d();
      Jr.transpose (Hinv);
      Jr.mulCols (fderivs);
      if (!symmetric) {
         // add force dependent terms
         Jr.m10 += cx*ty*f.x - sx*f.y - cx*f.z/cy;
         Jr.m11 += (1+ty*ty)*sx*f.x - sx*ty*f.z/cy;
         Jr.m20 += sx*ty*f.x + cx*f.y - sx*f.z/cy;
         Jr.m21 += -(1+ty*ty)*cx*f.x + cx*ty*f.z/cy;
      }
      Jr.mul (Hinv);

      Jq.setSubMatrix (3, 3, Jr);
   }

   public void computeDFdu (
      Matrix6d Ju, RigidTransform3d X21, Twist vel21, 
      RigidTransform3d initialX21, boolean symmetric) {

      Ju.setZero();

      Ju.m00 = myD.x;
      Ju.m11 = myD.y;
      Ju.m22 = myD.z;
      
      Ju.m33 = myRotD.x;
      Ju.m44 = myRotD.y;
      Ju.m55 = myRotD.z;
   }

   public boolean equals (FrameMaterial mat) {
      if (!(mat instanceof FunctionFrameMaterial)) {
         return false;
      }
      FunctionFrameMaterial fm = (FunctionFrameMaterial)mat;
      for (int i=0; i<3; i++) {
         if (!myTransFxns[i].equals (fm.myTransFxns[i])) {
            return false;
         }
         if (!myRotFxns[i].equals (fm.myRotFxns[i])) {
            return false;
         }
      }
      if (!myD.equals (fm.myD) ||
          !myRotD.equals (fm.myRotD)) {
         return false;
      }
      else {
         return super.equals (mat);
      }
   }

   public FunctionFrameMaterial clone() {
      FunctionFrameMaterial mat = (FunctionFrameMaterial)super.clone();
      for (int i=0; i<3; i++) {
         mat.myTransFxns[i] = myTransFxns[i].clone();
         mat.myRotFxns[i] = myRotFxns[i].clone();
      }
      mat.myD = new Vector3d (myD);
      mat.myRotD = new Vector3d (myRotD);
      return mat;
   }

   // Serialization methods

   public void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {
      super.writeItems (pw, fmt, ancestor);
      pw.println ("translationalFunctions=[");
      IndentingPrintWriter.addIndentation (pw, 2);
      for (Diff1Function1x1 fxn : myTransFxns) {
         FunctionUtils.write (pw, fxn, fmt);
      }
      IndentingPrintWriter.addIndentation (pw, -2);
      pw.println ("]");
      pw.println ("rotaryFunctions=[");
      IndentingPrintWriter.addIndentation (pw, 2);
      for (Diff1Function1x1 fxn : myRotFxns) {
         FunctionUtils.write (pw, fxn, fmt);
      }
      IndentingPrintWriter.addIndentation (pw, -2);
      pw.println ("]");
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {
      // if keyword is a property name, try scanning that
      rtok.nextToken();
      if (ScanWriteUtils.scanAttributeName (rtok, "translationalFunctions")) {
         rtok.scanToken ('[');
         for (int i=0; i<3; i++) {
            myTransFxns[i] =
               FunctionUtils.scan (rtok, Diff1Function1x1Base.class);
         }
         rtok.scanToken (']');
         return true;
      }
      else if (ScanWriteUtils.scanAttributeName (rtok, "rotaryFunctions")) {
         rtok.scanToken ('[');
         for (int i=0; i<3; i++) {
            myRotFxns[i] =
               FunctionUtils.scan (rtok, Diff1Function1x1Base.class);
         }
         rtok.scanToken (']');
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }


}
