package artisynth.core.materials;

import artisynth.core.modelbase.ContactPoint;
import artisynth.core.modelbase.FieldPropertyList;
import artisynth.core.modelbase.ScalarFieldComponent;
import maspack.matrix.Vector3d;

/**
 * Implementation of non-linear elastic foundation compliance model from:
 * Bei, Y. and Fregly, B. J. (2004). Multibody dynamic simulation of knee 
 * contact mechanics. Medical engineering and physics, 26(9), 777-789.
 * 
 * @author stavness
 */
public class LinearElasticContact extends ElasticContactBase {

   protected static double DEFAULT_NU = 0.33;
   double myPoissonsRatio = DEFAULT_NU;

   protected static double DEFAULT_E = 500000;
   double myYoungsModulus = DEFAULT_E;
   ScalarFieldComponent myYoungsModulusField = null;

   double K;
   double myStiffnessMultiplier;

   public static FieldPropertyList myProps =
      new FieldPropertyList (
         LinearElasticContact.class, ElasticContactBase.class);

   public FieldPropertyList getAllPropertyInfo() {
      return myProps;
   }

   static {
      myProps.addWithField (
         "YoungsModulus", "Young's elasticty modulus of elasticity", DEFAULT_E);
      myProps.add (
         "PoissonsRatio", "Poisson's ratio", DEFAULT_NU);
   }

   /**
    * Need no-args constructor for scanning
    */
   public LinearElasticContact () {
   }

   /**
    * Constructs a new LinearElasticContact material with specified properties.
    *
    * @param youngsModulus Young's modulus (the "stiffness")
    * @param poissonsRatio Poissons ratio 
    * @param dampingFactor multiplier for damping forces
    * @param thickness thickness of the foundation layer
    */
   public LinearElasticContact (
      double youngsModulus, double poissonsRatio,
      double dampingFactor, double thickness) {
//      handler = h;
      myThickness = thickness;
      myYoungsModulus = youngsModulus;
      myDampingFactor = dampingFactor;
      myPoissonsRatio = poissonsRatio;
      updateStiffnessMultiplier ();
   }

   protected void updateStiffnessMultiplier() {
      myStiffnessMultiplier =
         (1-myPoissonsRatio)/((1+myPoissonsRatio)*(1-2*myPoissonsRatio));      
   }
   
   /**
    * {@inheritDoc}
    */
   @Override
   public void computeResponse (
      double[] fres, double dist, ContactPoint cpnt0, ContactPoint cpnt1,
      Vector3d normal, double contactArea, int flags) {
      
      double area = getContactArea (cpnt0, normal, contactArea);

      double h = getThickness (cpnt0, cpnt1);
      double E = getYoungsModulus(cpnt0, cpnt1);
      double Ka = E*myStiffnessMultiplier*area;
      if ((flags & TWO_WAY_CONTACT) != 0) {
         Ka *= 0.5;
      }
      
      if (myUseLogDistance) {
         double r = myMinThicknessRatio;
         if ((1+dist/h) < r) {
            // linear zone near layer boundary
            double slope = Ka/(h*r);
            double fmin = Ka*Math.log(r);
            fres[0] = fmin + slope*(dist-h*(r-1));
            fres[1] = 1/slope;              
         }
         else {
            fres[0] = Ka*Math.log (1+dist/h); // force (distance is negative)
            fres[1] = (h+dist)/Ka; // compliance
         }
      }
      else {
         fres[0] = Ka*(dist/h); // force (distance is negative)
         fres[1] = h/Ka; // compliance
      }
      fres[2] = computeDamping (fres[0], fres[1]);
   }

   /**
    * Queries Poisson's ratio for this material.
    * 
    * @return Poission's ratio
    */
   public double getPoissonsRatio () {
      return myPoissonsRatio;
   }

   /**
    * Sets Poisson's ratio for this material.
    * 
    * @param nu new value of Poission's ratio
    */
   public void setPoissonsRatio (double nu) {
      myPoissonsRatio = nu;
      updateStiffnessMultiplier ();
   }

   /**
    * Queries Young's modulus for this material.
    *
    * @return Young's modulus
    */
   public double getYoungsModulus () {
      return myYoungsModulus;
   }

   /**
    * Sets Young's modulus for this material.
    * 
    * @param E new value for Young's modulus
    */
   public void setYoungsModulus (double E) {
      this.myYoungsModulus = E;
   }

   /**
    * Internal method to find Young's modulus in case it is bound to a
    * field. Works the same as {@link #getThickness(ContactPoint,ContactPoint)}.
    */
   protected double getYoungsModulus (ContactPoint cpnt0, ContactPoint cpnt1) {
      if (myYoungsModulusField == null) {
         return getYoungsModulus();
      }
      else {
         return getScalarFieldValue (myYoungsModulusField, cpnt0, cpnt1);
      }
   }

   /**
    * Queries the field, if any, associated with Young's modulus.
    *
    * @return Young's modulus field, or {@code null}
    */
   public ScalarFieldComponent getYoungsModulusField() {
      return myYoungsModulusField;
   }

   /**
    * Binds Young's modulus to a field, or unbinds it if {@code field} is
    * {@code null}.
    *
    * @param field field to bind to, or {@code null}
    */
   public void setYoungsModulusField (ScalarFieldComponent field) {
      myYoungsModulusField = field;
   }
   
   /**
    * {@inheritDoc}
    */
   public LinearElasticContact clone() {
      return (LinearElasticContact)super.clone();
   }
   
}
