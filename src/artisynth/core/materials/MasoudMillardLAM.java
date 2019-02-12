package artisynth.core.materials;

import maspack.properties.PropertyList;

public class MasoudMillardLAM extends AxialMuscleMaterial {
   
   
   protected double normFiberLen = 1;
   protected double myPenAngleLit = 0;
   protected double mySarcomereLenLit = 1.8;
   protected double optimalSarcomereLen = 2.8;
   protected double myFMTratioLit = 0.5; // FascicleLength/MuscleTendonLength

   public MasoudMillardLAM () {
      setOptLength (1);
      setMyFMTratioLit (1);
   }

   public MasoudMillardLAM (double penAngleLit, double sarcomereLenLit,
   double fiberRatio) {
      setPenAngleLit (penAngleLit);
      setMySarcomereLenLit (sarcomereLenLit);
      setMyFMTratioLit (fiberRatio);
   }

   public static PropertyList myProps = new PropertyList (
      MasoudMillardLAM.class, AxialMuscleMaterial.class);

   static {
      myProps.addReadOnly ("normFiberLen *", "Normalized Fiber Length");
      myProps.add ("myFMTratioLit * *", "Normalized Fiber Length", 0);
      myProps.add ("mySarcomereLenLit * *", "Normalized Fiber Length", 0);
      // myProps.add ("myFMTratioLit * *", "Normalized Fiber Length", null);
   }

   public double getNormFiberLen () {
      return normFiberLen;
   }

   public PropertyList getAllPropertyInfo () {
      return myProps;
   }

   public void setPenAngleLit (double penAngleLit) {
      myPenAngleLit = penAngleLit;
   }

   public double getPenAngleLit () {
      return myPenAngleLit;
   }

   public void setMySarcomereLenLit (double sarcomereLenLit) {
      mySarcomereLenLit = sarcomereLenLit;
   }

   public double getMySarcomereLenLit () {
      return mySarcomereLenLit;
   }

   public void setMyFMTratioLit (double FMTratioLit) {
      myFMTratioLit = FMTratioLit;
   }

   public double getMyFMTratioLit () {
      return myFMTratioLit;
   }

   @Override
   public double computeF (double l, double ldot, double l0, double excitation) {
      double passive = 0, active = 0;
      myOptLength =
         l0 * myFMTratioLit * optimalSarcomereLen / mySarcomereLenLit;
      double fiberLen =
         l - l0
         * (1 - myFMTratioLit * Math.cos (Math.toRadians (myPenAngleLit)));

      // active part
      if (myOptLength != 0)
         normFiberLen = fiberLen / myOptLength; // Assuming
                                                // tendonRatio to
                                                // be
                                                // tendonSlackLength/myOptLength.
      double x_0 = 0.4441;
      double x_1 = 0.58705;
      double x_2 = 0.7083;
      double x_3 = 0.9524;
      double x_4 = 1.056;
      double x_5 = 1.8123;

      double y_0 = 0;
      double y_1 = 0.405224;
      double y_2 = 0.7802;
      double y_3 = 1;
      double y_4 = 1;
      double y_5 = 0;

      if (normFiberLen >= x_0 && normFiberLen < x_1) {
         active = (y_1 - y_0) / (x_1 - x_0) * (normFiberLen - x_0) + y_0;
      }
      else if (normFiberLen >= x_1 && normFiberLen < x_2) {
         active = (y_2 - y_1) / (x_2 - x_1) * (normFiberLen - x_1) + y_1;
      }
      else if (normFiberLen >= x_2 && normFiberLen < x_3) {
         active = (y_3 - y_2) / (x_3 - x_2) * (normFiberLen - x_2) + y_2;
      }
      else if (normFiberLen >= x_3 && normFiberLen < x_4) {
         active = (y_4 - y_3) / (x_4 - x_3) * (normFiberLen - x_3) + y_3;
      }
      else if (normFiberLen >= x_4 && normFiberLen < x_5) {
         active = (y_5 - y_4) / (x_5 - x_4) * (normFiberLen - x_4) + y_4;
      }

      // passive part
      x_0 = 1;
      x_1 = 1.207;
      x_2 = 1.398;
      x_3 = 1.7;

      y_0 = 0;
      y_1 = 0.05303;
      y_2 = 0.2603;
      y_3 = 1;

      if (normFiberLen >= x_0 && normFiberLen < x_1) {
         passive =
            ((y_1 - y_0) / (x_1 - x_0) * (normFiberLen - x_0) + y_0)
            * myPassiveFraction;
      }
      else if (normFiberLen >= x_1 && normFiberLen < x_2) {
         passive =
            ((y_2 - y_1) / (x_2 - x_1) * (normFiberLen - x_1) + y_1)
            * myPassiveFraction;
      }
      else if (normFiberLen >= x_2 && normFiberLen < x_3) {
         passive =
            ((y_3 - y_2) / (x_3 - x_2) * (normFiberLen - x_2) + y_2)
            * myPassiveFraction;
      }
      else if (normFiberLen >= x_3) {
         passive =
            ((1 - 0.350054) / (1.7 - 1.47252) * (normFiberLen - 1.7) + 1)
            * myPassiveFraction;
      }

      return forceScaling
      * (myMaxForce * (active * excitation + passive) + myDamping * ldot);
   }

   @Override
   public double computeDFdl (
      double l, double ldot, double l0, double excitation) {
      double active_dFdl = 0.0, passive_dFdl = 0.0;
      myOptLength =
         l0 * myFMTratioLit * optimalSarcomereLen / mySarcomereLenLit;
      double fiberLen =
         l - l0
         * (1 - myFMTratioLit * Math.cos (Math.toRadians (myPenAngleLit)));
      // active part
      if (myOptLength != 0)
         normFiberLen = fiberLen / myOptLength; // Assuming
                                                // tendonRatio to
                                                // be
                                                // tendonSlackLength/myOptLength.
      double x_0 = 0.4441;
      double x_1 = 0.58705;
      double x_2 = 0.84;
      double x_3 = 1;
      double x_4 = 1.43115;
      double x_5 = 1.8123;

      double y_0 = 0;
      double y_1 = 0.405224;
      double y_2 = 0.905224;
      double y_3 = 1;
      double y_4 = 0.5;
      double y_5 = 0;

      // active part
      if (normFiberLen >= x_0 && normFiberLen < x_1) {
         active_dFdl = (y_1 - y_0) / (x_1 - x_0) / myOptLength;
      }
      else if (normFiberLen >= x_1 && normFiberLen < x_2) {
         active_dFdl = (y_2 - y_1) / (x_2 - x_1) / myOptLength;
      }
      else if (normFiberLen >= x_2 && normFiberLen < x_3) {
         active_dFdl = (y_3 - y_2) / (x_3 - x_2) / myOptLength;
      }
      else if (normFiberLen >= x_3 && normFiberLen < x_4) {
         active_dFdl = (y_4 - y_3) / (x_4 - x_3) / myOptLength;
      }
      else if (normFiberLen >= x_4 && normFiberLen < x_5) {
         active_dFdl = (y_5 - y_4) / (x_5 - x_4) / myOptLength;
      }

      // passive part
      x_0 = 1;
      x_1 = 1.207;
      x_2 = 1.398;
      x_3 = 1.7;

      y_0 = 0;
      y_1 = 0.05303;
      y_2 = 0.2603;
      y_3 = 1;

      if (normFiberLen >= x_0 && normFiberLen < x_1) {
         passive_dFdl =
            (y_1 - y_0) / (x_1 - x_0) * myPassiveFraction / myOptLength;
      }
      else if (normFiberLen >= x_1 && normFiberLen < x_2) {
         passive_dFdl =
            (y_2 - y_1) / (x_2 - x_1) * myPassiveFraction / myOptLength;
      }
      else if (normFiberLen >= x_2 && normFiberLen < x_3) {
         passive_dFdl =
            (y_3 - y_2) / (x_3 - x_2) * myPassiveFraction / myOptLength;
      }
      else if (normFiberLen >= x_3) {
         passive_dFdl =
            (1 - 0.350054) / (1.7 - 1.47252) * myPassiveFraction / myOptLength;
      }

      return forceScaling * myMaxForce
      * (active_dFdl * excitation + passive_dFdl);
   }

   @Override
   public double computeDFdldot (
      double l, double ldot, double l0, double excitation) {
      return forceScaling * myDamping;
   }

   @Override
   public boolean isDFdldotZero () {
      return myDamping == 0;
   }
}
