package artisynth.core.materials;

import maspack.matrix.Matrix6d;
import maspack.matrix.Matrix3d;
import maspack.matrix.SymmetricMatrix3d;
import maspack.matrix.VectorNd;
import maspack.properties.PropertyList;
import maspack.util.ArraySupport;
import maspack.util.InternalErrorException;

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
   private int myN = 1; // one plus index of maximum non-zero gamma

   static {
      myProps.add (
         "Tau", "Tau", DEFAULT_TAU, "D6");
      myProps.add (
         "Gamma", "Gamma", DEFAULT_GAMMA, "D6");
  }//               if (i==0 && k==0) {
// System.out.println ("QLVstate:");
// System.out.println (((QLVState)state).toString("%10.2f"));
//}

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   protected void updateN() {
      int maxi = -1;
      for (int i=0; i<N_MAX; i++) {
         if (myGamma[i] != 0) {
            maxi = i;
         }
      }
      myN = maxi+1;
   }
   
   public synchronized void setGamma (VectorNd gamma) {
      for (int i=0; i<gamma.size() && i<N_MAX; i++) {
         myGamma[i] = gamma.get(i);
      }
      updateN();
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
      updateN();
      notifyHostOfPropertyChange();      
   }

   public synchronized void setTau (VectorNd tau) {
      for (int i=0; i<tau.size() && i<N_MAX; i++) {
         myTau[i] = tau.get(i);//               if (i==0 && k==0) {
//       System.out.println ("QLVstate:");
//       System.out.println (((QLVState)state).toString("%10.2f"));
//    }
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
   }//               if (i==0 && k==0) {
// System.out.println ("QLVstate:");
// System.out.println (((QLVState)state).toString("%10.2f"));
//}

   public QLVBehavior () {
      myTau = new double[N_MAX];
      myGamma = new double[N_MAX];
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

   public QLVBehavior (QLVBehavior behav) {
      this();
      set (behav);
   }

   public void set (QLVBehavior behav) {
      for (int i=0; i<N_MAX; i++) {
         myTau[i] = behav.myTau[i];
         myGamma[i] = behav.myGamma[i];
      }
      updateN();
      notifyHostOfPropertyChange();
   }

   public void advanceState (
      ViscoelasticState state, double t0, double t1) {

      QLVState qlvstate = (QLVState)state;      
      double h = t1 - t0;
      qlvstate.myH = h;
      
      SymmetricMatrix3d deltaS = new SymmetricMatrix3d();
      deltaS.sub (qlvstate.mySSave, qlvstate.mySPrev);
      if (t0 >= 0) {
         qlvstate.mySPrev.set (qlvstate.mySSave);
      }

      double[] b = qlvstate.myB;
      double[] aHprev = qlvstate.myAHPrev;

      if (b.length != myN) {
         throw new InternalErrorException (
            "behavior has n value of "+myN+", state has "+myN);
      }

      for (int i=0; i<myN; i++) {
         double g = Math.exp(- h / myTau[i]);
         double gH;
         int idx = i*N_MAX;

         gH = g*(b[i]*deltaS.m00 + aHprev[idx]);
         aHprev[idx++] = gH;
         gH = g*(b[i]*deltaS.m11 + aHprev[idx]);
         aHprev[idx++] = gH;
         gH = g*(b[i]*deltaS.m22 + aHprev[idx]);
         aHprev[idx++] = gH;
         gH = g*(b[i]*deltaS.m01 + aHprev[idx]);
         aHprev[idx++] = gH;
         gH = g*(b[i]*deltaS.m02 + aHprev[idx]);
         aHprev[idx++] = gH;
         gH = g*(b[i]*deltaS.m12 + aHprev[idx]);
         aHprev[idx++] = gH;
        
         b[i] = (1.0 - g) / ( h / myTau[i] );
      }
   }

   public void computeStress (
      SymmetricMatrix3d sigma, DeformedPoint def, ViscoelasticState state) {

      QLVState qlvstate = (QLVState)state;

      double h = qlvstate.myH;
      if (h == 0) {
         return;
      }

      // compute second P-K stress from Cauchy stress sigma
      SymmetricMatrix3d S = new SymmetricMatrix3d();
      FemMaterial.cauchyToSecondPKStress (S, sigma, def);

      double[] aHprev = qlvstate.myAHPrev;
      double[] b = qlvstate.myB;

      SymmetricMatrix3d deltaS = new SymmetricMatrix3d();
      deltaS.sub (S, qlvstate.mySPrev);

      qlvstate.mySSave.set (S);
      S.scale(myGamma0);

      for (int i=0; i<myN; i++) {
         double H00 = b[i]*deltaS.m00 + aHprev[i*N_MAX  ];
         double H11 = b[i]*deltaS.m11 + aHprev[i*N_MAX+1];
         double H22 = b[i]*deltaS.m22 + aHprev[i*N_MAX+2];
         double H01 = b[i]*deltaS.m01 + aHprev[i*N_MAX+3];
         double H02 = b[i]*deltaS.m02 + aHprev[i*N_MAX+4];
         double H12 = b[i]*deltaS.m12 + aHprev[i*N_MAX+5];

         double gamma = myGamma[i];
         S.m00 += gamma*H00;
         S.m11 += gamma*H11;
         S.m22 += gamma*H22;
         S.m01 += gamma*H01;
         S.m02 += gamma*H02;
         S.m12 += gamma*H12;
      }

      S.m10 = S.m01;
      S.m20 = S.m02;
      S.m21 = S.m12;

      FemMaterial.secondPKToCauchyStress (sigma, S, def);
  }

   public void computeTangent (Matrix6d D, ViscoelasticState state) {
      
      QLVState qlvstate = (QLVState)state;
      double h = qlvstate.myH;
      if (h == 0) {
         return;
      }

      double scaleFactor = myGamma0;
      double[] b = qlvstate.myB;	   
      for (int i=0; i<myN; i++) {
         //double g = Math.exp(- h / myTau[i]);
         //double s = (1.0 - g) / (h / myTau[i]);
         scaleFactor += myGamma[i] * b[i];
      }
      D.scale(scaleFactor);
   }

   public void computeStressAndTangent (
      SymmetricMatrix3d sigma, Matrix6d D, DeformedPoint def, 
      ViscoelasticState state) {
      
      QLVState qlvstate = (QLVState)state;

      double h = qlvstate.myH;
      if (h == 0) {
         return;
      }

      // compute second P-K stress from Cauchy stress sigma
      SymmetricMatrix3d S = new SymmetricMatrix3d();
      FemMaterial.cauchyToSecondPKStress (S, sigma, def);

      double[] aHprev = qlvstate.myAHPrev;
      double[] b = qlvstate.myB;

      SymmetricMatrix3d deltaS = new SymmetricMatrix3d();
      deltaS.sub (S, qlvstate.mySPrev);

      qlvstate.mySSave.set (S);
      S.scale(myGamma0);

      for (int i=0; i<myN; i++) {
         double H00 = b[i]*deltaS.m00 + aHprev[i*N_MAX  ];
         double H11 = b[i]*deltaS.m11 + aHprev[i*N_MAX+1];
         double H22 = b[i]*deltaS.m22 + aHprev[i*N_MAX+2];
         double H01 = b[i]*deltaS.m01 + aHprev[i*N_MAX+3];
         double H02 = b[i]*deltaS.m02 + aHprev[i*N_MAX+4];
         double H12 = b[i]*deltaS.m12 + aHprev[i*N_MAX+5];

         double gamma = myGamma[i];
         S.m00 += gamma*H00;
         S.m11 += gamma*H11;
         S.m22 += gamma*H22;
         S.m01 += gamma*H01;
         S.m02 += gamma*H02;
         S.m12 += gamma*H12;
      }

      S.m10 = S.m01;
      S.m20 = S.m02;
      S.m21 = S.m12;

      FemMaterial.secondPKToCauchyStress (sigma, S, def);

      if (D != null) {
         double tangentScale = myGamma0;
         for (int i=0; i<myN; i++) {
            tangentScale += myGamma[i] * b[i];
         }
         D.scale(tangentScale);
      }
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
      return new QLVState(myN);
   }

   public ViscoelasticBehavior clone() {
      QLVBehavior veb = (QLVBehavior)super.clone();
      veb.myGamma = ArraySupport.copy (myGamma);
      veb.myTau = ArraySupport.copy (myTau);
      return veb;
   }

   public MaterialStateObject createStateObject() {
      return new QLVState (myN);
   }
 }
