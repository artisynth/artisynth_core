package artisynth.core.materials;

import java.io.PrintWriter;
import java.io.IOException;
import java.util.Deque;
import artisynth.core.modelbase.*;
import artisynth.core.util.ScanToken;
import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix6d;
import maspack.matrix.SymmetricMatrix3d;
import maspack.properties.PropertyList;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;
import maspack.util.ReaderTokenizer;
import maspack.util.NumberFormat;

public class NeoHookeanMaterial extends FemMaterial {

   public static PropertyList myProps =
      new PropertyList (NeoHookeanMaterial.class, FemMaterial.class);

   protected static double DEFAULT_NU = 0.33;
   protected static double DEFAULT_E = 500000;

   private double myNu = DEFAULT_NU;
   private double myE = DEFAULT_E;
   private FieldPointFunction<Double> myEFxn;

   PropertyMode myNuMode = PropertyMode.Inherited;
   PropertyMode myEMode = PropertyMode.Inherited;

   private SymmetricMatrix3d myB;
   //private SymmetricMatrix3d myB2;

   static {
      myProps.addInheritable (
         "YoungsModulus:Inherited", "Youngs modulus", DEFAULT_E);
      myProps.addInheritable (
         "PoissonsRatio:Inherited", "Poissons ratio", DEFAULT_NU, "[-1,0.5]");
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public NeoHookeanMaterial (){
      myB = new SymmetricMatrix3d();
      //myB2 = new SymmetricMatrix3d();
   }
   
   public NeoHookeanMaterial(double E, double nu) {
      this();
      setYoungsModulus(E);
      setPoissonsRatio(nu);
   }

   public synchronized void setPoissonsRatio (double nu) {
      myNu = nu;
      myNuMode =
         PropertyUtils.propagateValue (this, "PoissonsRatio", myNu, myNuMode);
      notifyHostOfPropertyChange();
   }

   public double getPoissonsRatio() {
      return myNu;
   }

   public void setPoissonsRatioMode (PropertyMode mode) {
      myNuMode =
         PropertyUtils.setModeAndUpdate (this, "PoissonsRatio", myNuMode, mode);
   }

   public PropertyMode getPoissonsRatioMode() {
      return myNuMode;
   }

   public synchronized void setYoungsModulus (double E) {
      myE = E;
      myEMode =
         PropertyUtils.propagateValue (this, "YoungsModulus", myE, myEMode);
      notifyHostOfPropertyChange();
   }

   public double getYoungsModulus() {
      return myE;
   }

   public void setYoungsModulusMode (PropertyMode mode) {
      myEMode =
         PropertyUtils.setModeAndUpdate (this, "YoungsModulus", myEMode, mode);
   }

   public PropertyMode getYoungsModulusMode() {
      return myEMode;
   }

   public double getYoungsModulus (FieldPoint dp) {
      if (myEFxn == null) {
         return getYoungsModulus();
      }
      else {
         return myEFxn.eval (dp);
      }
   }

   public FieldPointFunction<Double> getYoungsModulusFunction() {
      return myEFxn;
   }
      
   public void setYoungsModulusFunction (FieldPointFunction<Double> func) {
      myEFxn = func;
      notifyHostOfPropertyChange();
   }
   
   public void setYoungsModulusField (
      Field<Double> field, boolean useRestPos) {
      myEFxn = FieldUtils.createFieldFunction (field, useRestPos);
      notifyHostOfPropertyChange();
   }

   public Field<Double> getYoungsModulusField () {
      return FieldUtils.getFieldFromFunction (myEFxn);
   }

   public void computeStress (
      SymmetricMatrix3d sigma, DeformedPoint def, Matrix3d Q,
      FemMaterial baseMat) {

      double J = def.getDetF();

      // express constitutive law in terms of Lama parameters
      double E = getYoungsModulus(def);
      double G = E/(2*(1+myNu)); // bulk modulus
      double lam = (E*myNu)/((1-2*myNu)*(1+myNu));
      double mu = G;

      computeLeftCauchyGreen (myB,def);

      sigma.scale (mu/J, myB);
      double diagTerm = (lam*Math.log(J)-mu)/J;
      sigma.m00 += diagTerm;
      sigma.m11 += diagTerm;
      sigma.m22 += diagTerm;
   }

   public void computeTangent (
      Matrix6d D, SymmetricMatrix3d stress, DeformedPoint def, 
      Matrix3d Q, FemMaterial baseMat) {

      double J = def.getDetF();

      computeLeftCauchyGreen (myB,def);

      // express constitutive law in terms of Lama parameters
      double E = getYoungsModulus(def);
      double G = E/(2*(1+myNu)); // bulk modulus
      double lam = (E*myNu)/((1-2*myNu)*(1+myNu));
      double mu = G;

      D.setZero();
      TensorUtils.addScaledIdentityProduct (D, lam/J);
      TensorUtils.addScaledIdentity (D, 2*(mu-lam*Math.log(J))/J);
      D.setLowerToUpper();
   }

   public boolean equals (FemMaterial mat) {
      if (!(mat instanceof NeoHookeanMaterial)) {
         return false;
      }
      NeoHookeanMaterial stvk = (NeoHookeanMaterial)mat;
      if (myNu != stvk.myNu ||
          myE != stvk.myE) {
         return false;
      }
      else {
         return super.equals (mat);
      }
   }

   public NeoHookeanMaterial clone() {
      NeoHookeanMaterial mat = (NeoHookeanMaterial)super.clone();
      mat.myB = new SymmetricMatrix3d();
      //mat.myB2 = new SymmetricMatrix3d();
      return mat;
   }

   public static void main (String[] args) {
      NeoHookeanMaterial mat = new NeoHookeanMaterial();

      Matrix3d Q = new Matrix3d();

      DeformedPointBase dpnt = new DeformedPointBase();
      dpnt.setF (new Matrix3d (1, 3, 5, 2, 1, 4, 6, 1, 2));

      Matrix6d D = new Matrix6d();
      SymmetricMatrix3d sig = new SymmetricMatrix3d();

      mat.setYoungsModulus (10);      
      mat.computeStressAndTangent (sig, D, dpnt, Q, 0.0);

      System.out.println ("sig=\n" + sig.toString ("%12.6f"));
      System.out.println ("D=\n" + D.toString ("%12.6f"));

   }

   @Override
   public void scaleDistance (double s) {
      if (s != 1) {
         super.scaleDistance (s);
         setYoungsModulus (myE/s);
      }
   }

   @Override
   public void scaleMass (double s) {
      if (s != 1) {
         super.scaleMass (s);
         setYoungsModulus (myE*s);
      }
   }

   public void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {
      super.writeItems (pw, fmt, ancestor);
      FieldUtils.writeFunctionInfo (
         pw, "YoungsModulusFunc", myEFxn, fmt, ancestor);
   }

   protected boolean scanItem (
      ReaderTokenizer rtok, Deque<ScanToken> tokens) throws IOException {
      rtok.nextToken();
      if (ScanWriteUtils.scanAttributeName (rtok, "YoungsModulusFunc")) {
         myEFxn = FieldUtils.scanFunctionInfo (
            rtok, "YoungsModulusFunc", tokens);
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected boolean postscanItem (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

       if (ScanWriteUtils.postscanAttributeName (
          tokens, "YoungsModulusFunc")) {
          myEFxn = FieldUtils.postscanFunctionInfo (tokens, ancestor);
          return true;
       }
       return super.postscanItem (tokens, ancestor);
   }       

}
