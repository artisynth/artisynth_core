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

public class ScaledFemMaterial extends FemMaterial {
   
   protected static FemMaterial DEFAULT_BASE_MATERIAL = new LinearMaterial();
   protected static double DEFAULT_SCALING = 1.0;

   static ArrayList<Class<?>> myBaseClasses;
   static {
      myBaseClasses = new ArrayList<>(FemMaterial.mySubclasses.size());
      for (Class<?> clazz : FemMaterial.mySubclasses) {
         if (!ScaledFemMaterial.class.isAssignableFrom (clazz)) {
            myBaseClasses.add (clazz);
         }
      }
   }

   FemMaterial myBaseMaterial = DEFAULT_BASE_MATERIAL;
   double myScaling = DEFAULT_SCALING;
   ScalarFieldComponent myScalingField;

   public static FieldPropertyList myProps =
      new FieldPropertyList(ScaledFemMaterial.class, FemMaterial.class);

   static {
      myProps.addWithField (
         "scaling", "scaling factor for the base material", DEFAULT_SCALING);
      PropertyDesc desc = myProps.add (
         "baseMaterial",
         "base material providing the elasticity", DEFAULT_BASE_MATERIAL);
      desc.setAllowedTypes (myBaseClasses);
   }

   public FieldPropertyList getAllPropertyInfo() {
      return myProps;
   }

   public ScaledFemMaterial() {
      setBaseMaterial (DEFAULT_BASE_MATERIAL);
      setScaling (DEFAULT_SCALING);
   }

   public ScaledFemMaterial (FemMaterial baseMat, double scaling) {
      setBaseMaterial (baseMat);
      setScaling (scaling);
   }

   public double getScaling() {
      return myScaling;
   }

   public void setScaling (double scaling) {
      myScaling = scaling;
      notifyHostOfPropertyChange();
   }

   public double getScaling (FemFieldPoint dp) {
      if (myScalingField == null) {
         return getScaling();
      }
      else {
         return myScalingField.getValue (dp);
      }
   }

   public ScalarFieldComponent getScalingField() {
      return myScalingField;
   }
      
   public void setScalingField (ScalarFieldComponent func) {
      myScalingField = func;
      notifyHostOfPropertyChange();
   }

   /**
    * If possible, initializes the baseMaterial property in this
    * ScaledFemMaterial from another FemMaterial.
    *
    * <p>This method is called via reflection in the CompositePropertyPanel
    * code, to help initialize the ScaledFemMaterial from any previous
    * material that had been selected. It returns an array of the names of the
    * properties that were set, if any. 
    */
   public String[] initializePropertyValues (FemMaterial mat) {
      if (mat instanceof ScaledFemMaterial) {
         setBaseMaterial (((ScaledFemMaterial)mat).getBaseMaterial());
      }
      else {
         setBaseMaterial (mat);
      }
      // baseMaterial property was set
      return new String[] {"baseMaterial"};
   }

   protected void doSetBaseMaterial (FemMaterial baseMat) {
      myBaseMaterial = (FemMaterial)MaterialBase.updateMaterial (
         this, "baseMaterial", myBaseMaterial, baseMat);
   }

   public void setBaseMaterial (FemMaterial baseMat) {
      if (baseMat == null) {
         throw new IllegalArgumentException ("Base material cannot be null");
      }
      else if (baseMat instanceof ScaledFemMaterial) {
         throw new IllegalArgumentException (
            "Base material cannot be another ScaledFemMaterial");
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

   public boolean equals (FemMaterial mat) {
      if (mat instanceof ScaledFemMaterial) {
         ScaledFemMaterial smat = (ScaledFemMaterial)mat;
         return (myScaling == smat.myScaling &&
                 myBaseMaterial.equals (smat.myBaseMaterial));
      }
      else {
         return false;
      }
   }

   public ScaledFemMaterial clone() {
      ScaledFemMaterial smat = (ScaledFemMaterial)super.clone();
      smat.doSetBaseMaterial (myBaseMaterial);
      return smat;
   }

   public boolean hasState() {
      return myBaseMaterial.hasState();
   }

   public MaterialStateObject createStateObject() {
      return myBaseMaterial.createStateObject();
   }

   public void advanceState (MaterialStateObject state, double t0, double t1) {
      if (myBaseMaterial.hasState()) {
         myBaseMaterial.advanceState (state, t0, t1);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void computeStressAndTangent (
      SymmetricMatrix3d sigma, Matrix6d D, DeformedPoint def, 
      Matrix3d Q, double excitation, MaterialStateObject state) {

      double scaling = getScaling (def);
      IncompressibleMaterialBase imat =
         myBaseMaterial.getIncompressibleComponent();
      if (imat != null) {
         imat.computeDevStressAndTangent (
            sigma, D, def, Q, excitation, state);
         sigma.scale (scaling);
         double p = def.getAveragePressure();
         imat.addPressureStress (sigma, p);
         if (D != null) {
            D.scale (scaling);
            imat.addPressureTangent (D, p);
         }  
      }
      else {
         myBaseMaterial.computeStressAndTangent (
            sigma, D, def, Q, excitation, state);
         sigma.scale (scaling);
         if (D != null) {
            D.scale (scaling);
         }  
      }
   }
   
   public double computeStrainEnergyDensity (
      DeformedPoint def, Matrix3d Q, double excitation, 
      MaterialStateObject state) {

      double scaling = getScaling (def);
      IncompressibleMaterialBase imat =
         myBaseMaterial.getIncompressibleComponent();
      if (imat != null) {
         double W = scaling*imat.computeDevStrainEnergy (
            def, Q, excitation, state);
         W += imat.computeU (imat.getBulkModulus(def), def.getAverageDetF());
         return W;
      }
      else {
         return scaling*myBaseMaterial.computeStrainEnergyDensity (
            def, Q, excitation, state);
      }
   }
   
   public boolean isIncompressible() {
      return myBaseMaterial.isIncompressible();
   }

   public boolean isLinear() {
      return myBaseMaterial.isLinear();
   }

   public boolean isCorotated() {
      return myBaseMaterial.isCorotated();
   }

   public IncompressibleMaterialBase getIncompressibleComponent() {
      return myBaseMaterial.getIncompressibleComponent();
   }
   
   
}
