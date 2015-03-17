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

   private static double myGamma0 = 1.0;
   private double[] myGamma;
   private double[] myTau;
   private double myTangentScale;

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

      QLVState qlvstate = (QLVState)state;      
      double h = t1 - t0;
      qlvstate.myH = h;
      
      SymmetricMatrix3d deltaSigma = new SymmetricMatrix3d();
      deltaSigma.sub (qlvstate.mySigmaSave, qlvstate.mySigmaPrev);
      if (t0 >= 0) {
         qlvstate.mySigmaPrev.set (qlvstate.mySigmaSave);
      }

      double[] S = qlvstate.myS;
      double[] GHPrev = qlvstate.myGHPrev;

      myTangentScale = myGamma0;
      for (int i=0; i<N_MAX; i++) {
         double g = Math.exp(- h / myTau[i]);
         double gH;
         int idx = i*6;

         gH = g*(S[i]*deltaSigma.m00 + GHPrev[idx]);
         GHPrev[idx++] = gH;
         gH = g*(S[i]*deltaSigma.m11 + GHPrev[idx]);
         GHPrev[idx++] = gH;
         gH = g*(S[i]*deltaSigma.m22 + GHPrev[idx]);
         GHPrev[idx++] = gH;
         gH = g*(S[i]*deltaSigma.m01 + GHPrev[idx]);
         GHPrev[idx++] = gH;
         gH = g*(S[i]*deltaSigma.m02 + GHPrev[idx]);
         GHPrev[idx++] = gH;
         gH = g*(S[i]*deltaSigma.m12 + GHPrev[idx]);
         GHPrev[idx++] = gH;
        
         S[i] = (1.0 - g) / ( h / myTau[i] );
         myTangentScale += myGamma[i]*S[i];
      }
   }

   public void computeStress (
      SymmetricMatrix3d sigma, ViscoelasticState state) {

      QLVState qlvstate = (QLVState)state;

      double h = qlvstate.myH;
      if (h == 0) {
         return;
      }
      double[] GHPrev = qlvstate.myGHPrev;
      double[] S = qlvstate.myS;

      SymmetricMatrix3d deltaSigma = new SymmetricMatrix3d();
      deltaSigma.sub (sigma, qlvstate.mySigmaPrev);

      qlvstate.mySigmaSave.set (sigma);
      sigma.scale(myGamma0);

      for (int i=0; i<N_MAX; i++) {
         double H00 = S[i]*deltaSigma.m00 + GHPrev[i*6  ];
         double H11 = S[i]*deltaSigma.m11 + GHPrev[i*6+1];
         double H22 = S[i]*deltaSigma.m22 + GHPrev[i*6+2];
         double H01 = S[i]*deltaSigma.m01 + GHPrev[i*6+3];
         double H02 = S[i]*deltaSigma.m02 + GHPrev[i*6+4];
         double H12 = S[i]*deltaSigma.m12 + GHPrev[i*6+5];

         double gamma = myGamma[i];
         sigma.m00 += gamma*H00;
         sigma.m11 += gamma*H11;
         sigma.m22 += gamma*H22;
         sigma.m01 += gamma*H01;
         sigma.m02 += gamma*H02;
         sigma.m12 += gamma*H12;
      }

      sigma.m10 = sigma.m01;
      sigma.m20 = sigma.m02;
      sigma.m21 = sigma.m12;
  }

   public double getTangentScale () {
      return myTangentScale;
   }

   public void computeTangent (Matrix6d c, ViscoelasticState state) {
      
      QLVState qlvstate = (QLVState)state;
      double h = qlvstate.myH;
      if (h == 0) {
         return;
      }

      double scaleFactor = myGamma0;
      double[] S = qlvstate.myS;	   
      for (int i=0; i<N_MAX; i++) {
         //double g = Math.exp(- h / myTau[i]);
         //double s = (1.0 - g) / (h / myTau[i]);
         scaleFactor += myGamma[i] * S[i];
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
