package artisynth.core.materials;

import maspack.properties.*;
import maspack.interpolation.CubicHermiteSpline1d;

public class Thelen2003AxialTendon extends AxialTendonBase {

   
   // from Thelan paper:

   /*
     
    Tendon force-strain:
    --------------------
 
            FT_toe
    FT = ---------------- ( exp (k_toe epsT/epsT_toe) - 1)   epsT <= epsT_toe
           exp (k_toe) - 1

    FT = k_lin (epsT - epsT_toe) + FT_toe                    epsT > epsT_toe


    where

    FT = tendon force normalized to maximum isometric force,
    epsT = tendon strain
    epsT_toe = strain above which the tendon exhibits linear behavior,
    k_toe = exponential shape factor (default value 3)
    k_lin = linear scale factor

    FT_toe is ???
    
    Parameter summary in OpenSim:

    FmaxTendonStrain - tendon strain at max isometric muscle force
    FmaxMuscleStrain - passive muscle strain at max isometric muscl force (eps0M)
   */

   // Thelen2003 specific properties

   // tendon strain at maximum isometric muscle force
   public static double DEFAULT_FMAX_TENDON_STRAIN = 0.04;
   protected double myFmaxTendonStrain = DEFAULT_FMAX_TENDON_STRAIN;

   // fixed tendon parameters
   double myKToe = 3.0;
   double myExpKToe = Math.exp(myKToe);
   double myFToe = 0.33;
   double myEToe; // computed from FmaxTendonStrain
   double myKlin; // computed from FmaxTendonStrain

   public static PropertyList myProps =
      new PropertyList(Thelen2003AxialTendon.class,
                       AxialTendonBase.class);

   static {
      myProps.add (
         "fmaxTendonStrain",
         "tendon strain at max isometric muscle force",
         DEFAULT_FMAX_TENDON_STRAIN);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public Thelen2003AxialTendon() {
      setFmaxTendonStrain (DEFAULT_FMAX_TENDON_STRAIN);
   }

   public Thelen2003AxialTendon (
      double fmax, double tslack) {
      
      this();
      setMaxIsoForce (fmax);
      setTendonSlackLength (tslack);
   }

   // Thelen2003 specific properties

   public double getFmaxTendonStrain() {
      return myFmaxTendonStrain;
   }

   public void setFmaxTendonStrain (double fts) {
      myFmaxTendonStrain = fts;
      updateTendonConstants();
   }

   // util methods

   private void updateTendonConstants() {
      double e0 = getFmaxTendonStrain();

      double exp3   = Math.exp(3.0);
      myEToe = (99.0*e0*exp3) / (166.0*exp3 - 67.0);
      myKlin = 0.67/(e0 - myEToe);
   }

   /**
    * Compute normalized tendon force from normalized tendon length.  Adapted
    * from OpenSim code.
    */
   protected double computeTendonForce (double tln) {
      double x = tln-1;

      //Compute tendon force
      double fse = 0;
      if (x > myEToe) {
         fse = myKlin*(x-myEToe)+myFToe;
      }
      else if (x > 0.0) { 
         fse = (myFToe/(myExpKToe-1.0))*(Math.exp(myKToe*x/myEToe)-1.0);
      }
      else {
         fse = 0.0;
      }
      return fse;    
   }

   /**
    * Compute derivative of normalized tendon force from normalized tendon
    * length.  Adapted from OpenSim code.
    */
   protected double computeTendonForceDeriv (double tln) {
      double x = tln-1;

      //Compute tendon force
      double dft = 0;
      if (x > myEToe) {
         dft = myKlin;
      }
      else if (x >= 0.0) { 
         dft = 
            (myFToe/(myExpKToe-1.0)) *
            (myKToe/myEToe) * (Math.exp(myKToe*x/myEToe));
      }
      else{
         dft = 0;
      }

      return dft;
   }
}
