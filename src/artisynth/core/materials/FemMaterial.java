package artisynth.core.materials;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Deque;

import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.FunctionPropertyList;
import artisynth.core.modelbase.PropertyChangeListener;
import artisynth.core.modelbase.PropertyChangeEvent;
import artisynth.core.util.ScanToken;
import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix6d;
import maspack.matrix.SymmetricMatrix3d;
import maspack.properties.PropertyUtils;
import maspack.util.DynamicArray;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;

public abstract class FemMaterial extends MaterialBase 
   implements PropertyChangeListener {

   static DynamicArray<Class<?>> mySubclasses = new DynamicArray<>(
      new Class<?>[] {
      LinearMaterial.class,
      StVenantKirchoffMaterial.class,
      MooneyRivlinMaterial.class,
      CubicHyperelastic.class,
      OgdenMaterial.class,
      FungMaterial.class,
      NeoHookeanMaterial.class,
      IncompNeoHookeanMaterial.class,
      IncompressibleMaterial.class,
      ViscoelasticMaterial.class,
      ScaledFemMaterial.class,
      FullBlemkerMuscle.class,
      TransverseLinearMaterial.class,
      NullMaterial.class,
   });

   /**
    * Allow adding of classes (for use in control panels)
    * @param cls class to register
    */
   public static void registerSubclass(Class<? extends FemMaterial> cls) {
      if (!mySubclasses.contains(cls)) {
         mySubclasses.add(cls);
      }
   }

   public static Class<?>[] getSubClasses() {
      return mySubclasses.getArray();
   }

   protected void notifyHostOfPropertyChange () {
      notifyHostOfPropertyChange ("???");
   }

   public void propertyChanged (PropertyChangeEvent e) {
      // pass on property change events from subcomponents
      if (myPropHost instanceof PropertyChangeListener) {
         ((PropertyChangeListener)myPropHost).propertyChanged (e);
      }
   }
   
   public static FunctionPropertyList myProps =
      new FunctionPropertyList(FemMaterial.class, MaterialBase.class);

   public FunctionPropertyList getAllPropertyInfo() {
      return myProps;
   }

   // public ViscoelasticBehavior getViscoBehavior () {
   //    return myViscoBehavior;
   // }

   // /**
   //  * Allows setting of viscoelastic behaviour
   //  * @param veb visco-elastic behaviour
   //  */
   // public void setViscoBehavior (ViscoelasticBehavior veb) {
   //    if (veb != null) {
   //       ViscoelasticBehavior newVeb = veb.clone();
   //       PropertyUtils.updateCompositeProperty (
   //          this, "viscoBehavior", null, newVeb);
   //       myViscoBehavior = newVeb;
   //       notifyHostOfPropertyChange ("viscoBehavior");
   //    }
   //    else if (myViscoBehavior != null) {
   //       PropertyUtils.updateCompositeProperty (
   //          this, "viscoBehavior", myViscoBehavior, null);
   //       myViscoBehavior = null;
   //       notifyHostOfPropertyChange ("viscoBehavior");
   //    }
   // } 

   /**
    * Computes the current Cauchy stress and tangent stiffness matrix.
    * 
    * @param sigma returns the Cauchy stress
    * @param D optional; if non-{@code null}, returns the tangent matrix
    * @param def deformation information, including deformation gradient and 
    * pressure
    * @param Q coordinate frame specifying directions of anisotropy
    * @param excitation current excitation value
    * @param state material state information, or {@code null} if the
    * material does not have state.
    */
   public abstract void computeStressAndTangent (
      SymmetricMatrix3d sigma, Matrix6d D, DeformedPoint def, 
      Matrix3d Q, double excitation, MaterialStateObject state);
   
   /**
    * Returns true if this material is defined for a deformation gradient
    * with a non-positive determinant.
    */
   public boolean isInvertible() {
      return false;
   }

   public boolean isIncompressible() {
      return false;
   }
   
   public IncompressibleMaterialBase getIncompressibleComponent() {
      return null;
   }
   
   /**
    * Linear stress/stiffness response to deformation, allows tangent
    * to be pre-computed and stored.
    * 
    * @return true if linear response
    */
   public boolean isLinear() {
      return false;
   }
   
   /**
    * Deformation is computed by first removing a rotation component 
    * (either explicit or computed from strain)
    * 
    * @return true if material is corotated
    */
   public boolean isCorotated() {
	  return false;
   }
   
   public boolean equals (FemMaterial mat) {
      return true;
   }

   // public boolean equals (Object obj) {
   //    if (obj instanceof FemMaterial) {
   //       FemMaterial mat = (FemMaterial)obj;
   //       if (PropertyUtils.equalValues (myViscoBehavior, mat.myViscoBehavior)) {
   //          return equals (mat);
   //       }
   //    }
   //    return false;
   // }

   // public FemMaterial clone() {
   //    FemMaterial mat = (FemMaterial)super.clone();
   //    mat.setViscoBehavior (myViscoBehavior);
   //    return mat;
   // }

   /**
    * Computes the right Cauchy-Green tensor from the deformation gradient.
    */
   public void computeRightCauchyGreen (
      SymmetricMatrix3d C, DeformedPoint def) {
      C.mulTransposeLeft (def.getF());
   }

   /**
    * Computes the left Cauchy-Green tensor from the deformation gradient.
    */
   public void computeLeftCauchyGreen (
      SymmetricMatrix3d B, DeformedPoint def) {
      B.mulTransposeRight (def.getF());
   }

   /**
    * Computes the right deviatoric Cauchy-Green tensor from the deformation
    * gradient.
    */
   public void computeDevRightCauchyGreen (
      SymmetricMatrix3d CD, DeformedPoint def) {
      CD.mulTransposeLeft (def.getF());
      CD.scale (Math.pow(def.getDetF(), -2.0/3.0));
   }

   /**
    * Computes the left deviatoric Cauchy-Green tensor from the deformation
    * gradient.
    */
   public void computeDevLeftCauchyGreen (
      SymmetricMatrix3d BD, DeformedPoint def) {
      BD.mulTransposeRight (def.getF());
      BD.scale (Math.pow(def.getDetF(), -2.0/3.0));
   }

   /**
    * Computes the second Piola-Kirchoff stress tensor from the Cauchy stress,
    * according to the formula
    *
    * S = J F^{-1} sigma F^{-T}
    */
   public static void cauchyToSecondPKStress (
      SymmetricMatrix3d S, SymmetricMatrix3d sigma, DeformedPoint def) {

      Matrix3d Finv = new Matrix3d();
      Finv.fastInvert (def.getF());
      S.set (sigma);
      S.mulLeftAndTransposeRight (Finv);
      S.scale (def.getDetF());
   }

   /**
    * Computes the Cauchy stress from the second Piola-Kirchoff stress tensor,
    * according to the formula
    *
    * sigma = 1/J F sigma F^T
    */
   public static void secondPKToCauchyStress (
      SymmetricMatrix3d sigma, SymmetricMatrix3d S, DeformedPoint def) {

      sigma.set (S);
      sigma.mulLeftAndTransposeRight (def.getF());
      sigma.scale (1/def.getDetF());
   }

   public boolean hasState() {
      return false;
   }

   public MaterialStateObject createStateObject() {
      return null;
   }
   
   public void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {
      super.writeItems (pw, fmt, ancestor);
      getAllPropertyInfo().writePropertyFunctions (pw, this, fmt, ancestor);
   }

   protected boolean scanItem (
      ReaderTokenizer rtok, Deque<ScanToken> tokens) throws IOException {
      rtok.nextToken();
      if (getAllPropertyInfo().scanPropertyFunction (rtok, this, tokens)) {
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected boolean postscanItem (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (getAllPropertyInfo().postscanPropertyFunction (
             tokens, this, ancestor)) {
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }       

   public FemMaterial clone() {
      FemMaterial mat = (FemMaterial)super.clone();
      return mat;
   }
}
