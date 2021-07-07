package artisynth.core.opensim.components;

import artisynth.core.materials.AxialMaterial;
import artisynth.core.materials.LigamentAxialMaterial;
import artisynth.core.materials.UWLigamentMaterial;
import artisynth.core.opensim.components.ForceSpringBase;
import artisynth.core.opensim.components.Ligament;
import artisynth.core.opensim.components.Millard2012EquilibriumMuscle;
/** 
 * The ligament model used in the University of Wisconsin knee model.
 * Based on: Blankevoort, L., and Huiskes, R. (1991). Ligament-bone 
 * interaction in a three-dimensional model of the knee. 
 * Journal of Biomechanical Engineering, 113(3), 263-269.
 * 
 * implementation adapted from UW SimTk plugin: 
 * https://simtk.org/home/uwmodels
 * 
 * Notes:
 * - the restLength property in the associated spring is used as
 * the referenceLength in the UW formulation, where referenceLength
 * is the length of the ligament when the joint is in the reference 
 * position (full extension).
 * 
 */
public class WISCO_Ligament extends ForceSpringBase {

   protected double referenceStrain =
      UWLigamentMaterial.DEFAULT_REFERENCE_STRAIN;

   protected double linearStiffness =
      UWLigamentMaterial.DEFAULT_LINEAR_STIFFNESS;

   protected double ligamentTransitionStrain =
      UWLigamentMaterial.DEFAULT_LIGAMENT_TRANSITION_STRAIN;

   protected double normalizedDamping =
      UWLigamentMaterial.DEFAULT_NORMALIZED_DAMPING;

   protected double maxForce = 
      UWLigamentMaterial.DEFAULT_MAX_FORCE;

   protected double tendonSlackLength =
      UWLigamentMaterial.DEFAULT_TENDON_SLACK_LENGTH;
   
   public void setReferenceStrain(double rS){
      referenceStrain = rS;
   }
   
   public double getReferenceStrain(){
      return referenceStrain;
   }
   
   public void setLinearStiffness(double lS){
      linearStiffness = lS;
   }
   
   public double getLinearStiffness(){
      return linearStiffness;
   }
   
   public void setLigamentTransitionStrain(double lTS){
      ligamentTransitionStrain = lTS;
   }
   
   public double getLigamentTransitionStrain(){
      return ligamentTransitionStrain;
   }
   
   public void setNormalizedDamping(double d){
      normalizedDamping = d;
   }
   
   public double getNormalizedDamping(){
      return normalizedDamping;
   }
   
   public void setMaxForce(double f){
      maxForce = f;
   }
   
   public double getMaxForce(){
      return maxForce;
   }
   
   public void setTendonSlackLength(double sl){
      tendonSlackLength = sl;
   }
   
   public double getTendonSlackLength(){
      return tendonSlackLength;
   }
   
   @Override
   public WISCO_Ligament clone () {
      return (WISCO_Ligament)super.clone ();
   }
   
   
   @Override
   public AxialMaterial createMaterial () {
     /* AxialMaterial mat = new UWLigamentMaterial ();
      ((UWLigamentMaterial)mat).setReferenceStrain (referenceStrain); 
      ((UWLigamentMaterial)mat).setLinearStiffness (linearStiffness);
      ((UWLigamentMaterial)mat).setLigamentTransitionStrain (ligamentTransitionStrain);
      ((UWLigamentMaterial)mat).setNormalizedDamping (normalizedDamping);
      ((UWLigamentMaterial)mat).setMaxForce (maxForce);
      ((UWLigamentMaterial)mat).setTendonSlackLength (tendonSlackLength);      */
      
      
      AxialMaterial mat = new LigamentAxialMaterial();
      ((LigamentAxialMaterial)mat).setCompStiffness (0);
      ((LigamentAxialMaterial)mat).setElongStiffness (linearStiffness*2);
      ((LigamentAxialMaterial)mat).setDamping (0.01);
      
      return mat;
   }
   
   
}
