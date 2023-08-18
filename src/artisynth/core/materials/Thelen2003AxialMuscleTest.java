package artisynth.core.materials;

import maspack.util.*;
import maspack.matrix.*;

public class Thelen2003AxialMuscleTest extends UnitTest {

   double DOUBLE_PREC = 1e-15;   
   // in Thelen paper this is 2, but leads to derivative discontinuity at v = 0
   double AFMUL = 1;

   Thelen2003AxialMuscle myMuscle;

   Thelen2003AxialMuscleTest() {
      myMuscle = new Thelen2003AxialMuscle();
   }

   /**
    * OpenSim code to compute velocity/force relationship.  Used to check our
    * force/velocity calculations.
    */
   double calcdlceN (double act, double fal, double actFalFv) {

      //The variable names have all been switched to closely match 
      //with the notation in Thelen 2003.
      double dlceN = 0.0;      //contractile element velocity    
      double af   = myMuscle.getAf();

      double a    = act;
      double afl  = a*fal; //afl = a*fl
      double Fm   = actFalFv;     //Fm = a*fl*fv    
      double flen = myMuscle.getFlen();
      // double Fmlen_afl = flen*afl;

      double dlcedFm = 0.0; //partial derivative of contractile element
      // velocity w.r.t. Fm

      double b = 0;
      double db= 0;

      double Fm_asyC = 0;           //Concentric contraction asymptote
      double Fm_asyE = afl*flen;    
      //Eccentric contraction asymptote
      double asyE_thresh = myMuscle.getFvLinearExtrapThreshold();

      //If fv is in the appropriate region, use 
      //Thelen 2003 Eqns 6 & 7 to compute dlceN
      if (Fm > Fm_asyC && Fm < Fm_asyE*asyE_thresh) {

         if( Fm <= afl ){        //Muscle is concentrically contracting
            b = afl + Fm/af;
            db= 1/af;
         }else{                    //Muscle is eccentrically contracting
            b = (AFMUL*(1+1/af)*(afl*flen-Fm))/(flen-1); 
            db= (AFMUL*(1+1/af)*(-1))/(flen-1); 
         }

         dlceN = (0.25 + 0.75*a)*(Fm-afl)/b; 
         //Scaling by VMAX is left out, and is post multiplied outside 
         //of the function
      }
      else {  //Linear extrapolation
         double Fm0 = 0.0; //Last Fm value from the Thelen curve

         //Compute d and db/dFm from Eqn 7. of Thelen2003
         //for the last
         if(Fm <= Fm_asyC){ //Concentrically contracting
            Fm0 = Fm_asyC;
            b = afl + Fm0/af;
            db= 1/af;               
         }else{             //Eccentrically contracting
            Fm0 = asyE_thresh*Fm_asyE;
            b = (AFMUL*(1+1/af)*(afl*flen-Fm0))/(flen-1); 
            db= (AFMUL*(1+1/af)*(-1))/(flen-1); 
         }

         //Compute the last dlceN value that falls in the region where
         //Thelen 2003 Eqn. 6 is valid
         double dlce0 = (0.25 + 0.75*a)*(Fm0-afl)/b;

         //Compute the dlceN/dfm of Eqn. 6 of Thelen 2003 at the last
         //valid point
         dlcedFm = (0.25 + 0.75*a)*(1)/b 
            - ((0.25 + 0.75*a)*(Fm0-afl)/(b*b))*db;

         //Linearly extrapolate Eqn. 6 from Thelen 2003 to compute
         //the new value for dlceN/dFm
         dlceN = dlce0 + dlcedFm*(Fm-Fm0);            
      }
            
      return dlceN;
   }

   /**
    * OpenSim code to compute the deriative of the velocity/force
    * relationship. Used to check our force/velocity calculations.
    */
   double calcDdlceDaFalFv(double aAct, double aFal, double aFalFv) {

      //The variable names have all been switched to closely match with 
      //the notation in Thelen 2003.
      // double dlceN = 0.0;      //contractile element velocity    
      double af   = myMuscle.getAf();

      double a    = aAct;
      double afl  = aAct*aFal;  //afl = a*fl
      double Fm   = aFalFv;    //Fm = a*fl*fv    
      double flen = myMuscle.getFlen();
      // double Fmlen_afl = flen*aAct*aFal;

      double dlcedFm = 0.0; //partial derivative of contractile element 
      //velocity w.r.t. Fm

      double b = 0;
      double db= 0;

      double Fm_asyC = 0;           //Concentric contraction asymptote
      double Fm_asyE = aAct*aFal*flen;    
      //Eccentric contraction asymptote
      double asyE_thresh = myMuscle.getFvLinearExtrapThreshold();

      //If fv is in the appropriate region, use 
      //Thelen 2003 Eqns 6 & 7 to compute dlceN
      if (Fm > Fm_asyC && Fm < Fm_asyE*asyE_thresh){

         if( Fm <= afl ){        //Muscle is concentrically contracting
            b = afl + Fm/af;
            db= 1/af;
         }else{                    //Muscle is eccentrically contracting
            b = (AFMUL*(1+1/af)*(afl*flen-Fm))/(flen-1); 
            db= (AFMUL*(1+1/af)*(-1))/(flen-1); 
         }

         //This variable may have future use outside this function
         dlcedFm = (0.25 + 0.75*a)*(1)/b - ((0.25 + 0.75*a)*(Fm-afl)/(b*b))*db;
      }
      else {  //Linear extrapolation
         double Fm0 = 0.0; //Last Fm value from the Thelen curve

         //Compute d and db/dFm from Eqn 7. of Thelen2003
         //for the last
         if(Fm <= Fm_asyC){ //Concentrically contracting
            Fm0 = Fm_asyC;
            b = afl + Fm0/af;
            db= 1/af;               
         }else{             //Eccentrically contracting
            Fm0 = asyE_thresh*Fm_asyE;
            b = (AFMUL*(1+1/af)*(afl*flen-Fm0))/(flen-1); 
            db= (AFMUL*(1+1/af)*(-1))/(flen-1); 
         }

            
         //Compute the dlceN/dfm of Eqn. 6 of Thelen 2003 at the last
         //valid point
         dlcedFm = (0.25 + 0.75*a)*(1)/b 
            - ((0.25 + 0.75*a)*(Fm0-afl)/(b*b))*db;
          
      }
            
      return dlcedFm;
   }  

   /**
    * OpenSim code to compute the force/velocity curve. Used to check out
    * calculations.
    *
    * <p>For some reason OpenSim performed this computation iteratively, even
    * though it is quite easy to do symbolically.
    */
   double calcfvInv(double aAct,double aFal,double dlceN,
                    double tolerance, int maxIterations) {

      double result = Double.NaN;
      double ferr=1;
      double iter= 0;

      double dlceN1 = 0;
      double dlceN1_d_Fm = 0;
      double fv = 1;
      double aFalFv = fv*aAct*aFal;
      double delta_aFalFv = 0;

      while(Math.abs(ferr) >= tolerance && iter < maxIterations) {
         dlceN1 = calcdlceN(aAct,aFal, aFalFv);
         ferr   = dlceN1-dlceN;
         dlceN1_d_Fm = calcDdlceDaFalFv(aAct,aFal,aFalFv);
            
         if(Math.abs(dlceN1_d_Fm) > DOUBLE_PREC) {
            delta_aFalFv = -ferr/(dlceN1_d_Fm);
            aFalFv = aFalFv + delta_aFalFv;
         }
         iter = iter+1;
      }
      
      if(Math.abs(ferr) < tolerance){
         result = Math.max(0.0, aFalFv/(aAct*aFal));
         return result;
      }

      throw new NumericalException (
         "Solver for force-velocity multiplier failed to converge.");
   }

   /**
    * Tests the computation of a force/velocity curve, with specified
    * activation {@code a}, over {@code cnt+1} samples from {@code vmin} to
    * {@code vmax}.
    */
   void testForceVel (double vmin, double vmax, double a, int cnt) {
      double dv = (vmax-vmin)/cnt;
      double maxFvErr = 0;
      double maxFvErrV = 0;
      double maxDfvErr = 0;
      double maxDfvErrV = 0;
      for (int i=0; i<=cnt; i++) {
         double v = vmin + i*dv;
         double fal = 1.0;
         double fv = myMuscle.computeForceVelocity (v, a);
         double fvChk = calcfvInv (a, fal, v, 1e-8, 100);
         double err = Math.abs(fv-fvChk);
         if (err > maxFvErr) {
            maxFvErr = err;
            maxFvErrV = v;
         }
         double dfv = myMuscle.computeDForceVelocity (v, a);
         double dfvChk;
         if (fv < 0) {
            dfvChk = 0;
         }
         else {
            dfvChk = 1/(a*calcDdlceDaFalFv (a, 1, a*fv));
         }
         err = 2*(dfv-dfvChk)/Math.abs(dfv+dfvChk);
         if (err > maxDfvErr) {
            maxDfvErr = err;
            maxDfvErrV = v;
         }

      }
      if (maxFvErr > 1e-13) {
         throw new TestException (
            "forceVel computation: maxFvErr=" + maxFvErr + ", v=" + maxFvErrV);
      }
      if (maxDfvErr > 5e-7) {
         throw new TestException (
            "forceVel computation: maxDfvErr=" + maxDfvErr + ", v=" + maxDfvErrV);
      }
   }

   /**
    * Prints {@code cnt+1} points of a force/velocity curve, with specified
    * activation {@code a}, from {@code vmin} to {@code vmax}.  Each line
    * consists of the normalized velocity {@code v}, the force/velocity scale
    * value {@code fv}, and a check value computed from OpenSim code.
    */
   void printForceVel (double vmin, double vmax, double a, int cnt) {
      double dv = (vmax-vmin)/cnt;
      for (int i=0; i<=cnt; i++) {
         double v = vmin + i*dv;
         double fal = 1.0;
         double fv = myMuscle.computeForceVelocity (v, a);
         double fvChk = calcfvInv (a, fal, v, 1e-8, 100);
         System.out.printf ("%8.5f %8.5f %8.5f\n", v, fv, fvChk);
      }
   }

   /**
    * Prints {@code cnt+1} points of the derivative of a force/velocity curve,
    * with specified activation {@code a}, from {@code vmin} to {@code vmax}.
    * Each line consists of the normalized velocity {@code v}, the derivative
    * of the force/velocity curve, and a check value computed from OpenSim
    * code.
    */
   void printDForceVel (double vmin, double vmax, double a, int cnt) {
      double dv = (vmax-vmin)/cnt;
      for (int i=0; i<=cnt; i++) {
         double v = vmin + i*dv;
         double fal = 1.0;
         double fv = myMuscle.computeForceVelocity (v, a);
         double dfv = myMuscle.computeDForceVelocity (v, a);
         double dfvChk;
         if (fv <= 0) {
            dfvChk = 0;
         }
         else {
            dfvChk = 1/(a*calcDdlceDaFalFv (a, fal, a*fv));
         }
         System.out.printf ("%8.5f %8.5f %8.5f\n", v, dfv, dfvChk);
      }
   }

   void printVelForce (double fvmin, double fvmax, double a, int cnt) {
      double dfv = (fvmax-fvmin)/cnt;
      for (int i=0; i<=cnt; i++) {
         double fv = fvmin + i*dfv;
         double fal = 1.0;
         double v = calcdlceN (a, fal, a*fal*fv);
         System.out.printf ("%8.5f %8.5f\n", fv, v);
      }
   }

   void printTendonParams() {
      double e0 = myMuscle.getFmaxTendonStrain();
      double t1   = Math.exp(0.3e1);
      double eToe = (0.99e2*e0*t1) / (0.166e3*t1 - 0.67e2);
      t1 = Math.exp(0.3e1);
      double klin = (0.67e2/0.100e3) 
         * 1.0/(e0 - (0.99e2*e0*t1) / (0.166e3*t1 - 0.67e2));
      System.out.println ("etoe/e0=" + eToe/e0);
      System.out.println ("klin*e0=" + klin*e0);
   }

   public void test() {
      // WARNING: there is a derivative discontinuity at vn = -(0.25*0.75*a),
      // so the test might fail if the sampling hits that on the "wrong" side.
      testForceVel (-1.2, 1.2, /*a=*/1.0, 24);
      testForceVel (-1.2, 1.2, /*a=*/0.5, 240);
      testForceVel (-1.2, 1.2, /*a=*/0.1, 240);
      testForceVel (-1.2, 1.2, /*a=*/0.01, 240);
   }

   public static void main (String[] args) {
      Thelen2003AxialMuscleTest tester = new Thelen2003AxialMuscleTest();
      tester.runtest();
      //tester.printTendonParams();
      //tester.printForceVel (-1.2, 1.2, 0.5, 24);
      //tester.printDForceVel (-1.2, 1.2, 0.5, 24);
   }
}
