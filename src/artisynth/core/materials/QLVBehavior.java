package artisynth.core.materials;

import maspack.matrix.Matrix6d;
import maspack.matrix.SymmetricMatrix3d;
import maspack.matrix.VectorNd;
import maspack.properties.PropertyList;
import maspack.util.ArraySupport;

public class QLVBehavior extends ViscoelasticBehavior {

   public static PropertyList myProps =
      new PropertyList (QLVBehavior.class, ViscoelasticBehavior.class);

   protected static VectorNd DEFAULT_GAMMA = 
      new VectorNd (new double[] {0.9, 0, 0, 0, 0, 0});
   protected static VectorNd DEFAULT_TAU = 
      new VectorNd (new double[] {2, 1, 1, 1, 1, 1 });

   public static int N_MAX = 6;

   private double[] myGamma;
   private double[] myTau;

   static {
      myProps.add (
         "Tau", "Tau", DEFAULT_TAU, "D6");
      myProps.add (
         "Gamma", "Gamma", DEFAULT_GAMMA, "D6");
  }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }
   
   public synchronized void setGamma (VectorNd gamma) {
      for (int i=0; i<gamma.size() && i<N_MAX; i++) {
         myGamma[i] = gamma.get(i);
      }
      notifyHostOfPropertyChange();
   }

   public synchronized void setGamma (
      double gamma0, double gamma1, double gamma2, 
      double gamma3, double gamma4, double gamma5) {

      myGamma[0] = gamma0;
      myGamma[1] = gamma1;
      myGamma[2] = gamma2;
      myGamma[3] = gamma3;
      myGamma[4] = gamma4;
      myGamma[5] = gamma5;
   }

   public synchronized void setTau (VectorNd tau) {
      for (int i=0; i<tau.size() && i<N_MAX; i++) {
         myTau[i] = tau.get(i);
      }
      notifyHostOfPropertyChange();
   }

   public synchronized void setTau (
      double tau0, double tau1, double tau2, 
      double tau3, double tau4, double tau5) {

      myTau[0] = tau0;
      myTau[1] = tau1;
      myTau[2] = tau2;
      myTau[3] = tau3;
      myTau[4] = tau4;
      myTau[5] = tau5;
   }

   public VectorNd getGamma() {
      return new VectorNd (myGamma);
   }

   public VectorNd getTau() {
      return new VectorNd (myTau);
   }

   public QLVBehavior () {
      myTau = new double[6];
      myGamma = new double[6];
      DEFAULT_TAU.get (myTau);
      DEFAULT_GAMMA.get (myGamma);
   }
   
   public QLVBehavior (
      double gamma0, double gamma1, double gamma2,
      double gamma3, double gamma4, double gamma5,
      double tau0, double tau1, double tau2,
      double tau3, double tau4, double tau5) {

      this();
      setGamma (gamma0, gamma1, gamma2, gamma3, gamma4, gamma5);
      setTau (tau0, tau1, tau2, tau3, tau4, tau5);
   }

   public void advanceState (
      ViscoelasticState state, double t0, double t1) {
      
      double h = t1 - t0;
      QLVState qlvstate = (QLVState)state;
      
      qlvstate.myH = t1 - t0;       
      qlvstate.mySigmaPrev.set (qlvstate.mySigmaSave);
      SymmetricMatrix3d deltaSigma = qlvstate.myDeltaSigma;
      double[] S = qlvstate.myS;
      double[] GHPrev = qlvstate.myGHPrev;
      for (int n=0; n<N_MAX; n++) {
         double g = Math.exp(- h / myTau[n]);
         double gH;
         int idx = n*6;

         gH = g*(S[n]*deltaSigma.m00 + GHPrev[idx]);
         GHPrev[idx++] = gH;
         gH = g*(S[n]*deltaSigma.m11 + GHPrev[idx]);
         GHPrev[idx++] = gH;
         gH = g*(S[n]*deltaSigma.m22 + GHPrev[idx]);
         GHPrev[idx++] = gH;
         gH = g*(S[n]*deltaSigma.m01 + GHPrev[idx]);
         GHPrev[idx++] = gH;
         gH = g*(S[n]*deltaSigma.m02 + GHPrev[idx]);
         GHPrev[idx++] = gH;
         gH = g*(S[n]*deltaSigma.m12 + GHPrev[idx]);
         GHPrev[idx++] = gH;
        
         S[n] = (1.0 - g) / ( h / myTau[n] );
      }
   }

   public void computeStress (
      SymmetricMatrix3d sigma, ViscoelasticState state) {

      QLVState qlvstate = (QLVState)state;

      double h = qlvstate.myH;
      if (h == 0) {
         return;
      }
      SymmetricMatrix3d sigmaPrev = qlvstate.mySigmaPrev;
      SymmetricMatrix3d deltaSigma = qlvstate.myDeltaSigma;
      double[] GHPrev = qlvstate.myGHPrev;
      double[] S = qlvstate.myS;

      double myGamma0 = 1.0;

      deltaSigma.sub (sigma, sigmaPrev);

      qlvstate.mySigmaSave.set (sigma);
      sigma.scale(myGamma0);

      for (int n=0; n<N_MAX; n++) {
         double H00 = S[n]*deltaSigma.m00 + GHPrev[n*6  ];
         double H11 = S[n]*deltaSigma.m11 + GHPrev[n*6+1];
         double H22 = S[n]*deltaSigma.m22 + GHPrev[n*6+2];
         double H01 = S[n]*deltaSigma.m01 + GHPrev[n*6+3];
         double H02 = S[n]*deltaSigma.m02 + GHPrev[n*6+4];
         double H12 = S[n]*deltaSigma.m12 + GHPrev[n*6+5];

         double a = myGamma[n];
         sigma.m00 += a*H00;
         sigma.m11 += a*H11;
         sigma.m22 += a*H22;
         sigma.m01 += a*H01;
         sigma.m02 += a*H02;
         sigma.m12 += a*H12;
      }

      sigma.m10 = sigma.m01;
      sigma.m20 = sigma.m02;
      sigma.m21 = sigma.m12;
  }

   public void computeTangent (Matrix6d c, ViscoelasticState state) {
      
      QLVState qlvstate = (QLVState)state;
      double h = qlvstate.myH;
      if (h == 0) {
         return;
      }

      double myGamma0 = 1.0;

      double scaleFactor = myGamma0;
      double[] S = qlvstate.myS;	   
      for (int n=0; n<N_MAX; n++) {
         //double g = Math.exp(- h / myTau[n]);
         //double s = (1.0 - g) / (h / myTau[n]);
         scaleFactor += myGamma[n] * S[n];
      }
      c.scale(scaleFactor);
   }
   
   public boolean equals (ViscoelasticBehavior veb) {
      if (!(veb instanceof QLVBehavior)) {
         return false;
      }
      QLVBehavior qlv = (QLVBehavior)veb;

      // XXX note that you can't use == for arrays
      if (!ArraySupport.equals (myGamma, qlv.myGamma) ||
          !ArraySupport.equals (myTau, qlv.myTau)) {
         return false;
      }
      else {
         return super.equals (veb);
      }
   }

   public ViscoelasticState createState() {
      return new QLVState();
   }

   public ViscoelasticBehavior clone() {
      QLVBehavior veb = (QLVBehavior)super.clone();
      veb.myGamma = ArraySupport.copy (myGamma);
      veb.myTau = ArraySupport.copy (myTau);
      return veb;
   }
   
 }
