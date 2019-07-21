package artisynth.core.materials;

import artisynth.core.modelbase.*;
import java.util.*;

import maspack.matrix.Matrix6d;
import maspack.matrix.Matrix3d;
import maspack.matrix.SymmetricMatrix3d;
import maspack.util.InternalErrorException;
import maspack.util.DynamicArray;
import maspack.util.DataBuffer;
import maspack.properties.*;

public class ViscoelasticMaterial extends FemMaterial {
   
   protected static FemMaterial DEFAULT_BASE_MATERIAL =
      new NeoHookeanMaterial();
   protected static QLVBehavior DEFAULT_VISCO_BEHAVIOR =
      new QLVBehavior();

   ViscoelasticBehavior myViscoBehavior = 
      new QLVBehavior (DEFAULT_VISCO_BEHAVIOR);

   protected static class ViscoMaterialState implements MaterialStateObject {

      MaterialStateObject baseState;
      ViscoelasticState viscoState;

      public void getState (DataBuffer data) {
         if (baseState != null) {
            baseState.getState (data);
         }
         viscoState.getState (data);
      }

      public void setState (DataBuffer data) {
         if (baseState != null) {
            baseState.setState (data);
         }
         viscoState.setState (data);
      }
      
      public boolean hasState() {
         return true;
      }
   }

   static ArrayList<Class<?>> myBaseClasses;
   static {
      myBaseClasses = new ArrayList<>(FemMaterial.mySubclasses.size());
      for (Class<?> clazz : FemMaterial.mySubclasses) {
         if (!LinearMaterialBase.class.isAssignableFrom (clazz) &&
             !ScaledFemMaterial.class.isAssignableFrom (clazz) &&
             !ViscoelasticMaterial.class.isAssignableFrom (clazz)) {
            myBaseClasses.add (clazz);
         }
      }
   }

   FemMaterial myBaseMaterial = DEFAULT_BASE_MATERIAL;

   public static FunctionPropertyList myProps =
      new FunctionPropertyList(ViscoelasticMaterial.class, FemMaterial.class);

   static {
      PropertyDesc desc = myProps.add (
         "baseMaterial",
         "base material providing the elasticity", DEFAULT_BASE_MATERIAL);
      desc.setAllowedTypes (myBaseClasses);
      myProps.add (
         "viscoBehavior", "visco elastic material behavior",
         DEFAULT_VISCO_BEHAVIOR);
   }

   public FunctionPropertyList getAllPropertyInfo() {
      return myProps;
   }

   public ViscoelasticMaterial() {
      setBaseMaterial (DEFAULT_BASE_MATERIAL);
      setViscoBehavior (DEFAULT_VISCO_BEHAVIOR);
   }

   public ViscoelasticMaterial (FemMaterial baseMat, ViscoelasticBehavior behav) {
      setBaseMaterial (baseMat);
      setViscoBehavior (behav);
   }

   /**
    * If possible, initializes the baseMaterial property in this
    * ViscoelasticMaterial from another FemMaterial.
    *
    * <p>This method is called via reflection in the CompositePropertyPanel
    * code, to help initialize the ViscoelasticMaterial from any previous
    * material that had been selected. It returns an array of the names of the
    * properties that were set, if any. 
    */
   public String[] initializePropertyValues (FemMaterial mat) {
      if (!mat.isLinear()) {
         if (mat instanceof ViscoelasticMaterial) {
            setBaseMaterial (((ViscoelasticMaterial)mat).getBaseMaterial());
         }
         else {
            setBaseMaterial (mat);
         }
         // baseMaterial property was set
         return new String[] {"baseMaterial"};
      }
      else {
         // no properties were set
         return new String[0];
      }
   }

   protected void doSetBaseMaterial (FemMaterial baseMat) {
      myBaseMaterial = (FemMaterial)MaterialBase.updateMaterial (
         this, "baseMaterial", myBaseMaterial, baseMat);
   }

   public void setBaseMaterial (FemMaterial baseMat) {
      if (baseMat == null) {
         throw new IllegalArgumentException ("Base material cannot be null");
      }
      else if (baseMat instanceof ViscoelasticMaterial) {
         throw new IllegalArgumentException (
            "Base material cannot be another ViscoelasticMaterial");
      }
      else if (baseMat.isLinear()) {
         throw new IllegalArgumentException (
            "Base material cannot be a linear material");
      }
      // don't do equality check unless equals also tests for function settings
      //if (!baseMat.equals (myBaseMaterial)) {
         FemMaterial old = myBaseMaterial;
         doSetBaseMaterial (baseMat);         
         notifyHostOfPropertyChange ("baseMaterial", baseMat, old);
      //}
   }

   public FemMaterial getBaseMaterial() {
      return myBaseMaterial;
   }

   public ViscoelasticBehavior getViscoBehavior () {
      return myViscoBehavior;
   }

   /**
    * Allows setting of viscoelastic behaviour
    * @param veb visco-elastic behaviour
    */
   public void setViscoBehavior (ViscoelasticBehavior veb) {
      if (veb == null) {
         throw new IllegalArgumentException (
            "Viscoelastic behavior material cannot be null");
      }
      ViscoelasticBehavior newVeb = veb.clone();
      PropertyUtils.updateCompositeProperty (
         this, "viscoBehavior", myViscoBehavior, newVeb);
      myViscoBehavior = newVeb;
      notifyHostOfPropertyChange ("viscoBehavior");
   }

   protected void notifyHostOfPropertyChange() {
      if (myPropHost instanceof FemMaterial) {
         ((FemMaterial)myPropHost).notifyHostOfPropertyChange ("viscoBehavior");
      }
   }

   public boolean equals (FemMaterial mat) {
      if (mat instanceof ViscoelasticMaterial) {
         ViscoelasticMaterial vem = (ViscoelasticMaterial)mat;
         return (myViscoBehavior.equals (vem.myViscoBehavior) &&
                 myBaseMaterial.equals (vem.myBaseMaterial));
      }
      else {
         return false;
      }
   }

   public ViscoelasticMaterial clone() {
      ViscoelasticMaterial vem = (ViscoelasticMaterial)super.clone();
      vem.doSetBaseMaterial (myBaseMaterial);
      vem.setViscoBehavior (myViscoBehavior);
      return vem;
   }

   public boolean hasState() {
      return true;
   }

   public MaterialStateObject createStateObject() {
      ViscoMaterialState state = new ViscoMaterialState();
      if (myBaseMaterial.hasState()) {
         state.baseState = myBaseMaterial.createStateObject();
      }
      state.viscoState = myViscoBehavior.createState();
      return state;
   }

   public void advanceState (MaterialStateObject state, double t0, double t1) {
      if (!(state instanceof  ViscoMaterialState)) {
         throw new InternalErrorException (
            "state "+state+" is not an instance of ViscoMaterialState");          
      }
      ViscoMaterialState vmstate = (ViscoMaterialState)state;
      if (myBaseMaterial.hasState()) {
         myBaseMaterial.advanceState (vmstate.baseState, t0, t1);
      }
      myViscoBehavior.advanceState (vmstate.viscoState, t0, t1);
   }

   /**
    * {@inheritDoc}
    */
   public void computeStressAndTangent (
      SymmetricMatrix3d sigma, Matrix6d D, DeformedPoint def, 
      Matrix3d Q, double excitation, MaterialStateObject state) {

      if (!(state instanceof  ViscoMaterialState)) {
         throw new InternalErrorException (
            "state "+state+" is not an instance of ViscoMaterialState");          
      }
      ViscoMaterialState vmstate = (ViscoMaterialState)state;

      IncompressibleMaterialBase imat =
         myBaseMaterial.getIncompressibleComponent();
      if (imat != null) {
         imat.computeDevStressAndTangent (
            sigma, D, def, Q, excitation, state);
         myViscoBehavior.computeStressAndTangent (
            sigma, D, def, vmstate.viscoState);
        double p = def.getAveragePressure();
        imat.addPressureStress (sigma, p);
        if (D != null) {
           imat.addPressureTangent (D, p);
        }  
      }
      else {
         myBaseMaterial.computeStressAndTangent (
            sigma, D, def, Q, excitation, state);
         myViscoBehavior.computeStressAndTangent (
            sigma, D, def, vmstate.viscoState);
      }
   }
   
   public boolean isIncompressible() {
      return myBaseMaterial.isIncompressible();
   }

   public IncompressibleMaterialBase getIncompressibleComponent() {
      return myBaseMaterial.getIncompressibleComponent();
   }
   
   
}
   
//   import artisynth.core.modelbase.ComponentChangeEvent.Code;
// 
