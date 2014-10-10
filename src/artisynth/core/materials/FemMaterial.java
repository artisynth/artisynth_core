package artisynth.core.materials;

import maspack.matrix.Matrix6d;
import maspack.matrix.Matrix3d;
import maspack.matrix.SymmetricMatrix3d;
import maspack.properties.PropertyList;
import maspack.properties.PropertyUtils;
import artisynth.core.util.PropertyChangeListener;
import artisynth.core.util.PropertyChangeEvent;

public abstract class FemMaterial extends MaterialBase {

   static Class<?>[] mySubClasses = new Class<?>[] {
      NullMaterial.class,
      LinearMaterial.class,
      StVenantKirchoffMaterial.class,
      MooneyRivlinMaterial.class,
      CubicHyperelastic.class,
      OgdenMaterial.class,
      FungMaterial.class,
      NeoHookeanMaterial.class,
      IncompNeoHookeanMaterial.class,
      IncompressibleMaterial.class
   };

   public static Class<?>[] getSubClasses() {
      return mySubClasses;
   }

   ViscoelasticBehavior myViscoBehavior;

   protected void notifyHostOfPropertyChange (String name) {
      if (myPropHost instanceof PropertyChangeListener) {
         ((PropertyChangeListener)myPropHost).propertyChanged (
            new PropertyChangeEvent (this, name));
      }
   }

   protected void notifyHostOfPropertyChange () {
      notifyHostOfPropertyChange ("???");
      // if (myPropHost instanceof FemModel) {
      //    ((FemModel)myPropHost).invalidateStressAndStiffness();
      //    ((FemModel)myPropHost).invalidateRestData();
      // }
      // else if (myPropHost instanceof FemElement) {
      //    ((FemElement)myPropHost).invalidateRestData();
      // }
   }

   public static PropertyList myProps =
      new PropertyList(FemMaterial.class, MaterialBase.class);

   static {
      myProps.add (
         "viscoBehavior", "visco elastic material behavior", null);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public ViscoelasticBehavior getViscoBehavior () {
      return myViscoBehavior;
   }

   public void setViscoBehavior (ViscoelasticBehavior veb) {
      if (veb != null) {
         ViscoelasticBehavior newVeb = veb.clone();
         PropertyUtils.updateCompositeProperty (
            this, "viscoBehavior", null, newVeb);
         myViscoBehavior = newVeb;
         notifyHostOfPropertyChange ("viscoBehavior");
      }
      else if (myViscoBehavior != null) {
         PropertyUtils.updateCompositeProperty (
            this, "viscoBehavior", myViscoBehavior, null);
         myViscoBehavior = null;
         notifyHostOfPropertyChange ("viscoBehavior");
      }
   }   

   /**
    * Computes the tangent stiffness matrix
    * @param D tangent stiffness, populated
    * @param stress the current stress tensor
    * @param def deformation information, includes deformation gradient and pressure
    * @param Q coordinate frame specifying directions of anisotropy
    * @param baseMat underlying base material (if any)
    */
   public abstract void computeTangent (
      Matrix6d D, SymmetricMatrix3d stress, SolidDeformation def, 
      Matrix3d Q, FemMaterial baseMat);
   
   /**
    * Computes the strain tensor given the supplied deformation
    * @param sigma strain tensor, populated
    * @param def deformation information, includes deformation gradient and pressure
    * @param Q coordinate frame specifying directions of anisotropy
    * @param baseMat underlying base material (if any)
    */
   public abstract void computeStress (
      SymmetricMatrix3d sigma, SolidDeformation def, Matrix3d Q,
      FemMaterial baseMat);

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

   public boolean isViscoelastic() {
	  return false;
   }
   
   public boolean equals (FemMaterial mat) {
      return true;
   }

   public boolean equals (Object obj) {
      if (obj instanceof FemMaterial) {
         FemMaterial mat = (FemMaterial)obj;
         if (PropertyUtils.equalValues (myViscoBehavior, mat.myViscoBehavior)) {
            return equals (mat);
         }
      }
      return false;
   }

   public FemMaterial clone() {
      FemMaterial mat = (FemMaterial)super.clone();
      mat.setViscoBehavior (myViscoBehavior);
      return mat;
   }
}
   
   
