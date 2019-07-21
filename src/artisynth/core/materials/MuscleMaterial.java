package artisynth.core.materials;

import java.io.*;
import java.util.*;

import artisynth.core.modelbase.*;
import artisynth.core.util.*;
import maspack.matrix.Matrix6d;
import maspack.matrix.Matrix3d;
import maspack.matrix.SymmetricMatrix3d;
import maspack.matrix.Vector3d;
import maspack.util.*;

/**
 * Base class for Muscle materials. These are similiar to regular materials,
 * except that they add to the stress and tangent term, and the associated
 * methods also accept an excitation and a direction.
 */
public abstract class MuscleMaterial extends FemMaterial {

   static Class<?>[] mySubClasses = new Class[] {
      GenericMuscle.class,
      SimpleForceMuscle.class,
      BlemkerMuscle.class,
      FullBlemkerMuscle.class,
      InactiveMuscle.class
   };

   public static Class<?>[] getSubClasses() {
      return mySubClasses;
   }

   public static double DEFAULT_EXCITATION = 0.0;
   private double myExcitation = DEFAULT_EXCITATION;
   public static Vector3d DEFAULT_REST_DIR = new Vector3d();
   private Vector3d myRestDir = new Vector3d(DEFAULT_REST_DIR);
   private VectorFieldPointFunction<Vector3d> myRestDirFxn;
   
   public static FunctionPropertyList myProps =
      new FunctionPropertyList(MuscleMaterial.class, MaterialBase.class);

   static {
      myProps.add (
         "excitation", "muscle excitation value", DEFAULT_EXCITATION);
      myProps.add (
         "restDir", "rest activation direction", DEFAULT_REST_DIR);
   }

   public FunctionPropertyList getAllPropertyInfo() {
      return myProps;
   }

   public double getExcitation() {
      return myExcitation;
   }

   public void setExcitation (double ex) {
      myExcitation = ex;
   }

   public Vector3d getRestDir() {
      return myRestDir;
   }

   public void setRestDir (Vector3d dir) {
      myRestDir = new Vector3d(dir);
   }

   public Vector3d getRestDir (FieldPoint dp) {
      if (myRestDirFxn == null) {
         return getRestDir();
      }
      else {
         return myRestDirFxn.eval (dp);
      }
   }

   public VectorFieldPointFunction<Vector3d> getRestDirFunction() {
      return myRestDirFxn;
   }
      
   public void setRestDirFunction (VectorFieldPointFunction<Vector3d> func) {
      myRestDirFxn = func;
      notifyHostOfPropertyChange();
   }
   
   public void setRestDirField (
      VectorField<Vector3d> field, boolean useRestPos) {
      myRestDirFxn = FieldUtils.setFunctionFromField (field, useRestPos);
      notifyHostOfPropertyChange();
   }

   public VectorField<Vector3d> getRestDirField () {
      return FieldUtils.getFieldFromFunction (myRestDirFxn);
   }

   /** 
    * Hook to notify associated components of change in parameters.
    */
   protected void notifyHostOfPropertyChange() {
   }

   public void computeStressAndTangent (
      SymmetricMatrix3d sigma, Matrix6d D, DeformedPoint def, 
      Matrix3d Q, double excitation, MaterialStateObject state) {
      computeStressAndTangent (
         sigma, D, def, getRestDir(def), getExcitation(), state);
   }

   public abstract void computeStressAndTangent (
      SymmetricMatrix3d sigma, Matrix6d D, DeformedPoint def, 
      Vector3d dir, double excitation, MaterialStateObject state);
    
   public boolean equals (MuscleMaterial mat) {
      return true;
   }

   public boolean equals (Object obj) {
      if (obj instanceof MuscleMaterial) {
         return equals ((MuscleMaterial)obj);
      }
      else {
         return false;
      }
   }

   public MuscleMaterial clone() {
      MuscleMaterial mat = (MuscleMaterial)super.clone();
      return mat;
   }

   /**
    * Returns true if this material is defined for a deformation gradient
    * with a non-positive determinant.
    */
   public boolean isInvertible() {
      return false;
   }

   public void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {
      super.writeItems (pw, fmt, ancestor);
      FieldUtils.writeVectorFunctionInfo (
         pw, "restDirFxn", myRestDirFxn, fmt, ancestor);
   }

   protected boolean scanItem (
      ReaderTokenizer rtok, Deque<ScanToken> tokens) throws IOException {
      rtok.nextToken();
      if (ScanWriteUtils.scanAttributeName (rtok, "restDirFxn")) {
         myRestDirFxn = FieldUtils.scanVectorFunctionInfo (
            rtok, "restDirFxn", tokens);
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected boolean postscanItem (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

       if (ScanWriteUtils.postscanAttributeName (
          tokens, "restDirFxn")) {
          myRestDirFxn = FieldUtils.postscanVectorFunctionInfo (
             tokens, ancestor);
          return true;
       }
       return super.postscanItem (tokens, ancestor);
   }
   
}

  
