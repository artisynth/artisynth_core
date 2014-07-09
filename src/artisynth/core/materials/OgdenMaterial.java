package artisynth.core.materials;

import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix6d;
import maspack.matrix.SymmetricMatrix3d;
import maspack.matrix.Vector3d;
import maspack.properties.PropertyList;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;

public class OgdenMaterial extends IncompressibleMaterial {

   public static PropertyList myProps =
      new PropertyList (OgdenMaterial.class, IncompressibleMaterial.class);

   protected static double DEFAULT_MU1    = 300000.0;
   protected static double DEFAULT_MU2    = 0.0;
   protected static double DEFAULT_MU3    = 0.0;
   protected static double DEFAULT_MU4    = 0.0;
   protected static double DEFAULT_MU5    = 0.0;
   protected static double DEFAULT_MU6    = 0.0;
   protected static double DEFAULT_ALPHA1 = 2;
   protected static double DEFAULT_ALPHA2 = 2;
   protected static double DEFAULT_ALPHA3 = 2;
   protected static double DEFAULT_ALPHA4 = 2;
   protected static double DEFAULT_ALPHA5 = 2;
   protected static double DEFAULT_ALPHA6 = 2;

   private int Nmax         = 6;
   private double[] myMu    = {
      DEFAULT_MU1, DEFAULT_MU2, DEFAULT_MU3,
      DEFAULT_MU4, DEFAULT_MU5, DEFAULT_MU6} ;
   private double[] myAlpha = {
      DEFAULT_ALPHA1, DEFAULT_ALPHA2, DEFAULT_ALPHA3,
      DEFAULT_ALPHA4, DEFAULT_ALPHA5, DEFAULT_ALPHA6} ;

   PropertyMode myMu1Mode    = PropertyMode.Inherited;
   PropertyMode myMu2Mode    = PropertyMode.Inherited;
   PropertyMode myMu3Mode    = PropertyMode.Inherited;
   PropertyMode myMu4Mode    = PropertyMode.Inherited;
   PropertyMode myMu5Mode    = PropertyMode.Inherited;
   PropertyMode myMu6Mode    = PropertyMode.Inherited;
   PropertyMode myAlpha1Mode = PropertyMode.Inherited;
   PropertyMode myAlpha2Mode = PropertyMode.Inherited;
   PropertyMode myAlpha3Mode = PropertyMode.Inherited;
   PropertyMode myAlpha4Mode = PropertyMode.Inherited;
   PropertyMode myAlpha5Mode = PropertyMode.Inherited;
   PropertyMode myAlpha6Mode = PropertyMode.Inherited;

   private SymmetricMatrix3d myB;
   private SymmetricMatrix3d myB2;
   private SymmetricMatrix3d myTmp;
   
   static {
      myProps.addInheritable (
         "Alpha1:Inherited", "Alpha1", DEFAULT_ALPHA1);
      myProps.addInheritable (
         "Alpha2:Inherited", "Alpha2", DEFAULT_ALPHA2);
      myProps.addInheritable (
         "Alpha3:Inherited", "Alpha3", DEFAULT_ALPHA3);
      myProps.addInheritable (
         "Alpha4:Inherited", "Alpha4", DEFAULT_ALPHA4);
      myProps.addInheritable (
         "Alpha5:Inherited", "Alpha5", DEFAULT_ALPHA5);
      myProps.addInheritable (
         "Alpha6:Inherited", "Alpha6", DEFAULT_ALPHA6);
      myProps.addInheritable (
         "Mu1:Inherited", "Mu1", DEFAULT_MU1);
      myProps.addInheritable (
         "Mu2:Inherited", "Mu2", DEFAULT_MU2);
      myProps.addInheritable (
         "Mu3:Inherited", "Mu3", DEFAULT_MU3);
      myProps.addInheritable (
         "Mu4:Inherited", "Mu4", DEFAULT_MU4);
      myProps.addInheritable (
         "Mu5:Inherited", "Mu5", DEFAULT_MU5);
      myProps.addInheritable (
         "Mu6:Inherited", "Mu6", DEFAULT_MU6);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public OgdenMaterial () {
      myB   = new SymmetricMatrix3d();
      myB2  = new SymmetricMatrix3d();
      myTmp = new SymmetricMatrix3d();
   }
   
   public OgdenMaterial (double[] mu, double[] alpha, double kappa) {
      this();
      setMu1 (mu[0]);
      setMu2 (mu[1]);
      setMu3 (mu[2]);
      setMu4 (mu[3]);
      setMu5 (mu[4]);
      setMu6 (mu[5]);
      setAlpha1 (alpha[0]);
      setAlpha2 (alpha[1]);
      setAlpha3 (alpha[2]);
      setAlpha4 (alpha[3]);
      setAlpha5 (alpha[4]);
      setAlpha6 (alpha[5]);
      setBulkModulus (kappa);
   }

   public synchronized void setMu1 (double mu) {
      myMu[0] = mu;
      myMu1Mode =
         PropertyUtils.propagateValue (this, "Mu1", myMu[0], myMu1Mode);
      notifyHostOfPropertyChange();
   }

   public synchronized void setMu2 (double mu) {
      myMu[1] = mu;
      myMu2Mode =
         PropertyUtils.propagateValue (this, "Mu2", myMu[1], myMu2Mode);
      notifyHostOfPropertyChange();
   }

   public synchronized void setMu3 (double mu) {
      myMu[2] = mu;
      myMu2Mode =
         PropertyUtils.propagateValue (this, "Mu2", myMu[2], myMu2Mode);
      notifyHostOfPropertyChange();
   }

   public synchronized void setMu4 (double mu) {
      myMu[3] = mu;
      myMu2Mode =
         PropertyUtils.propagateValue (this, "Mu2", myMu[3], myMu2Mode);
      notifyHostOfPropertyChange();
   }

   public synchronized void setMu5 (double mu) {
      myMu[4] = mu;
      myMu2Mode =
         PropertyUtils.propagateValue (this, "Mu2", myMu[4], myMu2Mode);
      notifyHostOfPropertyChange();
   }

   public synchronized void setMu6 (double mu) {
      myMu[5] = mu;
      myMu6Mode =
         PropertyUtils.propagateValue (this, "Mu6", myMu[5], myMu6Mode);
      notifyHostOfPropertyChange();
   }

   public double getMu1() {
      return myMu[0];
   }

   public double getMu2() {
      return myMu[1];
   }

   public double getMu3() {
      return myMu[2];
   }
   
   public double getMu4() {
      return myMu[3];
   }
   
   public double getMu5() {
      return myMu[4];
   }
   
   public double getMu6() {
      return myMu[5];
   }
   
   public void setMu1Mode (PropertyMode mode) {
      myMu1Mode =
         PropertyUtils.setModeAndUpdate (this, "Mu1", myMu1Mode, mode);
   }

   public void setMu2Mode (PropertyMode mode) {
	  myMu2Mode =
	     PropertyUtils.setModeAndUpdate (this, "Mu2", myMu2Mode, mode);
   }

   public void setMu3Mode (PropertyMode mode) {
	  myMu3Mode =
	     PropertyUtils.setModeAndUpdate (this, "Mu3", myMu3Mode, mode);
   }

   public void setMu4Mode (PropertyMode mode) {
	  myMu4Mode =
         PropertyUtils.setModeAndUpdate (this, "Mu4", myMu4Mode, mode);
   }

   public void setMu5Mode (PropertyMode mode) {
      myMu5Mode =
         PropertyUtils.setModeAndUpdate (this, "Mu5", myMu5Mode, mode);
   }

   public void setMu6Mode (PropertyMode mode) {
	  myMu6Mode =
         PropertyUtils.setModeAndUpdate (this, "Mu6", myMu6Mode, mode);
   }

   public PropertyMode getMu1Mode() {
      return myMu1Mode;
   }

   public PropertyMode getMu2Mode() {
      return myMu2Mode;
   }

   public PropertyMode getMu3Mode() {
	  return myMu3Mode;
   }
   
   public PropertyMode getMu4Mode() {
	  return myMu4Mode;
   }

   public PropertyMode getMu5Mode() {
	  return myMu5Mode;
   }

   public PropertyMode getMu6Mode() {
	  return myMu6Mode;
   }

   public synchronized void setAlpha1 (double alpha) {
      myAlpha[0] = alpha;
      myAlpha1Mode =
         PropertyUtils.propagateValue (this, "Alpha1", myAlpha[0], myAlpha1Mode);
      notifyHostOfPropertyChange();
   }

   public synchronized void setAlpha2 (double alpha) {
      myAlpha[1] = alpha;
      myAlpha2Mode =
         PropertyUtils.propagateValue (this, "Alpha2", myAlpha[1], myAlpha2Mode);
      notifyHostOfPropertyChange();
   }

   public synchronized void setAlpha3 (double alpha) {
      myAlpha[2] = alpha;
      myAlpha3Mode =
         PropertyUtils.propagateValue (this, "Alpha3", myAlpha[2], myAlpha3Mode);
      notifyHostOfPropertyChange();
   }

   public synchronized void setAlpha4 (double alpha) {
      myAlpha[3] = alpha;
      myAlpha4Mode =
         PropertyUtils.propagateValue (this, "Alpha4", myAlpha[3], myAlpha4Mode);
      notifyHostOfPropertyChange();
   }

   public synchronized void setAlpha5 (double alpha) {
      myAlpha[4] = alpha;
      myAlpha5Mode =
         PropertyUtils.propagateValue (this, "Alpha5", myAlpha[4], myAlpha5Mode);
      notifyHostOfPropertyChange();
   }

   public synchronized void setAlpha6 (double alpha) {
      myAlpha[5] = alpha;
      myAlpha6Mode =
         PropertyUtils.propagateValue (this, "Alpha6", myAlpha[5], myAlpha6Mode);
      notifyHostOfPropertyChange();
   }

   public double getAlpha1() {
      return myAlpha[0];
   }

   public double getAlpha2() {
      return myAlpha[1];
   }

   public double getAlpha3() {
      return myAlpha[2];
   }

   public double getAlpha4() {
      return myAlpha[3];
   }

   public double getAlpha5() {
      return myAlpha[4];
   }

   public double getAlpha6() {
      return myAlpha[5];
   }

   public void setAlpha1Mode (PropertyMode mode) {
      myAlpha1Mode =
         PropertyUtils.setModeAndUpdate (this, "Alpha1", myAlpha1Mode, mode);
   }

   public void setAlpha2Mode (PropertyMode mode) {
      myAlpha2Mode =
         PropertyUtils.setModeAndUpdate (this, "Alpha2", myAlpha2Mode, mode);
   }

   public void setAlpha3Mode (PropertyMode mode) {
      myAlpha3Mode =
         PropertyUtils.setModeAndUpdate (this, "Alpha3", myAlpha3Mode, mode);
   }
   
   public void setAlpha4Mode (PropertyMode mode) {
      myAlpha4Mode =
         PropertyUtils.setModeAndUpdate (this, "Alpha4", myAlpha4Mode, mode);
   }
   
   public void setAlpha5Mode (PropertyMode mode) {
      myAlpha5Mode =
         PropertyUtils.setModeAndUpdate (this, "Alpha5", myAlpha5Mode, mode);
   }
   
   public void setAlpha6Mode (PropertyMode mode) {
     myAlpha6Mode =
         PropertyUtils.setModeAndUpdate (this, "Alpha6", myAlpha6Mode, mode);
   }

   public PropertyMode getAlpha1Mode() {
      return myAlpha1Mode;
   }
   
   public PropertyMode getAlpha2Mode() {
      return myAlpha2Mode;
   }
	   
   public PropertyMode getAlpha3Mode() {
      return myAlpha3Mode;
   }
	   
   public PropertyMode getAlpha4Mode() {
      return myAlpha4Mode;
   }
	   
   public PropertyMode getAlpha5Mode() {
      return myAlpha5Mode;
   }
	   
   public PropertyMode getAlpha6Mode() {
      return myAlpha6Mode;
   }
	   
   public void computeStress (
      SymmetricMatrix3d sigma, SolidDeformation def, Matrix3d Q,
      FemMaterial baseMat) {

      double J = def.getDetF();
      double avgp = def.getAveragePressure();

      sigma.setZero();

      // Calculate Deviatoric left Cauchy-Green tensor
      def.computeDevLeftCauchyGreen(myB);

      Vector3d principalStretch   = new Vector3d();
      Vector3d principalStretch2  = new Vector3d();
      Matrix3d principalDirection = new Matrix3d();
     
      // Calculate principal stretches and principal directions
      myB.getEigenValues(principalStretch2, principalDirection);
      for ( int i=0; i<3; i++) {
         principalStretch.set(i, Math.sqrt(principalStretch2.get(i)) );
      }

      // Calculate principal stresses
      for (int i=0; i<3; i++) {
         for (int n=0; n<Nmax; n++) {
            if (myMu[n] != 0) {
               sigma.set(
                  i, i, sigma.get(i,i) + 
                  myMu[n] / myAlpha[n] / J * 
                  (Math.pow(principalStretch.get(i), myAlpha[n])));
            }
         }
      }

      // Calculate stress tensor from principal stresses and directions
      sigma.mulLeftAndTransposeRight(principalDirection);
      
      sigma.deviator();
      
      sigma.m00 += avgp;
      sigma.m11 += avgp;
      sigma.m22 += avgp;

   }


   // See JC Simo and RL Taylor, Quasi-Incompressible Finite Elasticity in
   // Principal Stretches.  Continuum Basis and Numerical Algorithms, Comp
   // Methods Appl Mech Eng, 85, 273-310, 1991
   public void computeTangent (
      Matrix6d c, SymmetricMatrix3d stress, SolidDeformation def, 
      Matrix3d Q, FemMaterial baseMat) {
      
      double J = def.getDetF();

      double[][] principalStretchDevPow;
      principalStretchDevPow = new double[3][6];

      // Calculate left Cauchy-Green tensor
      def.computeLeftCauchyGreen(myB);
  
      // Calculate square of B
      myB2.mulTransposeLeft(myB);

      double p = def.getAveragePressure(); // average element pressure

      Vector3d principalStretch         = new Vector3d();
      Vector3d principalStretch2        = new Vector3d();
      Vector3d principalStretchDev      = new Vector3d();
      Matrix3d principalDirection       = new Matrix3d();

      // Calculate principal stretches and principal directions
      myB.getEigenValues (principalStretch2, principalDirection);

      for ( int i=0; i<3; i++ ) {
         principalStretch.set (i, Math.sqrt(principalStretch2.get(i)) );
      }
	  
      // Calculate deviatoric principal stretches
      principalStretchDev.scale (Math.pow(J, -1.0 / 3.0), principalStretch);

      // Calculate 1st and 3rd strain invariants
      double I1 = (principalStretch2.get(0) +
                   principalStretch2.get(1) +
                   principalStretch2.get(2));
      double I3 = (principalStretch2.get(0) *
                   principalStretch2.get(1) *
                   principalStretch2.get(2));

      // Raise deviatoric stretches to power of alpha1, alpha2, etc
      for ( int i=0; i<3; i++ ) {
         for (int n=0; n<Nmax; n++) {
            if (myMu[n] != 0) {
               principalStretchDevPow[i][n] =
                  Math.pow(principalStretchDev.get(i), myAlpha[n]);
            }
         }
      }

      SymmetricMatrix3d ma = new SymmetricMatrix3d();
      SymmetricMatrix3d mb = new SymmetricMatrix3d();

      // Constant for determining differences in principal stretches
      double smallNum = 1e-8;
      
      // Initialise elasticity tensor
      c.setZero();

      // Consider the three distinct cases
      if ( (Math.abs(principalStretch2.get(0) - principalStretch2.get(1)) <
            smallNum) && 
           (Math.abs(principalStretch2.get(0) - principalStretch2.get(2)) <
            smallNum) ) {
         
    	 // All three principal stretches are the same
    	  
         // gamma_AB in Eq. 2.66 of Simo and Taylor (1991)    	  
         double g = 0.0;
         for (int n=0; n<Nmax; n++) {
            if (myMu[n] != 0) {
               g += myMu[n] * principalStretchDevPow[0][n];
            }
         }
         
         // Eq. 2.71 of Simo and Taylor (1991)
         TensorUtils.addScaledIdentity (c, g / J);
         TensorUtils.addScaledIdentityProduct (c, - g / J / 3.0);
      }
      else if ((Math.abs(principalStretch2.get(0)-principalStretch2.get(1)) >=
                smallNum) && 
               (Math.abs(principalStretch2.get(0)-principalStretch2.get(2)) >=
                smallNum) && 
               (Math.abs(principalStretch2.get(1)-principalStretch2.get(2)) >=
                smallNum) ) {
	     
    	 // All three principal stretches are different

         for ( int i=0; i<3; i++) {
            int j = ( i + 1 ) % 3;
            int k = ( j + 1 ) % 3; 

            // D prime i - Eq. 2.46a of Simo and Taylor (1991)
            double Dpi = 8.0 * Math.pow(principalStretch.get(i), 3.0) - 
               2.0 * I1 * principalStretch.get(i) - 
               2.0 * I3 * Math.pow(principalStretch.get(i), -3.0);
            
            // D i - Eq. 2.16a of Simo and Taylor (1991)
            double Di = ( principalStretch2.get(i) - principalStretch2.get(j) ) *
               ( principalStretch2.get(i) - principalStretch2.get(k) );

            // the matrix mi - Eq. 2.15 of Simo and Taylor (1991)
            ma.set(myB2);
            ma.scaledAdd (-principalStretch2.get(k), myB);
            ma.scaledAdd (-principalStretch2.get(j), myB);
            ma.scaledAdd ( principalStretch2.get(j) * principalStretch2.get(k), 
                           SymmetricMatrix3d.IDENTITY);
            ma.scale (1.0 / Di);

            // beta_i in Eq. 2.65 of Simo and Taylor (1991)
            double beta = 0.0;
            for (int n=0; n<Nmax; n++) {
               if (myMu[n] != 0) {
                  beta += myMu[n] / myAlpha[n] * 
                     ( principalStretchDevPow[i][n]  - 
                       ( principalStretchDevPow[0][n] +
                         principalStretchDevPow[1][n] + 
                         principalStretchDevPow[2][n] ) / 3.0 );
               }
            }

            // Calculate dgm term in Eq 2.68 of Simo and Taylor (1991)
            TensorUtils.addTensorProduct4 (c, 2.0 * beta / J / Di, myB);
            TensorUtils.addTensorProduct  (c, -2.0 * beta / J / Di, myB);
            TensorUtils.addScaledIdentityProduct (c, I3 * 2.0 * beta / J / Di / 
                                                  principalStretch2.get(i));
            TensorUtils.addScaledIdentity (c, -I3 * 2.0 * beta / J / Di / 
                                           principalStretch2.get(i));
            TensorUtils.addSymmetricTensorProduct (
               c, 2.0 * beta / J / Di * principalStretch2.get(i), myB, ma);
            TensorUtils.addTensorProduct  (c, -1.0 * beta / J / Di * Dpi * 
                                           principalStretch.get(i), ma);
            TensorUtils.addSymmetricTensorProduct (
               c, -2.0 * beta / J / Di * I3 / principalStretch2.get(i), 
               SymmetricMatrix3d.IDENTITY, ma);
                         
            for ( int n=0; n<3; n++) {
               j = ( n + 1 ) % 3;              
               k = ( j + 1 ) % 3; 
                    
               Di = ( principalStretch2.get(n) - principalStretch2.get(j) ) * 
                  ( principalStretch2.get(n) - principalStretch2.get(k) );
                    
               // the matrix mi - Eq. 2.15 of Simo and Taylor (1991)
               mb.set(myB2);
               mb.scaledAdd (-principalStretch2.get(k), myB);
               mb.scaledAdd (-principalStretch2.get(j), myB);
               mb.scaledAdd ( principalStretch2.get(j)*principalStretch2.get(k), 
                              SymmetricMatrix3d.IDENTITY);
               mb.scale (1.0 / Di);
                    
               // gamma_AB in Eq. 2.66 of Simo and Taylor (1991)    	  
               double gab = 0.0;
               if ( i == n ) {
                  for (int m=0; m<Nmax; m++) {
                     if (myMu[m] != 0) {
                        gab += myMu[m]*(principalStretchDevPow[i][m] / 3.0 + 
                                        (principalStretchDevPow[0][m] + 
                                         principalStretchDevPow[1][m] + 
                                         principalStretchDevPow[2][m] ) / 9.0 );
                     }
                  }
               }
               else {
                  for (int m=0; m<Nmax; m++) {
                     if (myMu[m] != 0) {
                        gab += myMu[m]*(-principalStretchDevPow[i][m] / 3.0 - 
                                        principalStretchDevPow[n][m] / 3.0 +
                                        ( principalStretchDevPow[0][m] + 
                                          principalStretchDevPow[1][m] + 
                                          principalStretchDevPow[2][m] ) / 9.0 );
                     }
                  }
               }

               // 2nd term in Eq. 2.68 of Simo and Taylor (1991)
               TensorUtils.addSymmetricTensorProduct (c, gab / J / 2.0, ma, mb);
            }

         }
      }
      else {
         // two are the same
         int i;
         if ( (Math.abs(principalStretch2.get(0)-principalStretch2.get(1)) <=
               smallNum) ) {
            i = 2; 
         }
         else if (Math.abs(principalStretch2.get(0)-principalStretch2.get(2)) <=
                  smallNum) {
            i = 1;
         }
         else {
            i = 0;
         }
         int j = (i+1)%3;
         int k = (j+1)%3;
         
         // D prime i - Eq. 2.46a of Simo and Taylor (1991)
         double Dpi = 8.0 * Math.pow(principalStretch.get(i), 3) - 
            2.0 * I1 * principalStretch.get(i) - 
            2.0 * I3*Math.pow(principalStretch.get(i), -3);
         // D i - Eq. 2.16a of Simo and Taylor (1991)
         double Di = ( principalStretch2.get(i) - principalStretch2.get(j) ) * 
            ( principalStretch2.get(i) - principalStretch2.get(k) );
         
         double beta3 = 0.0;
         double beta1 = 0.0;
         double g33   = 0.0;
         double g11   = 0.0;
         double g13   = 0.0;
         for (int n=0; n<Nmax; n++) {

            if (myMu[n] != 0) {
               // beta_i in Eq. 2.65 of Simo and Taylor (1991)
               beta3 += myMu[n] / myAlpha[n] *  
                  (principalStretchDevPow[i][n]  - 
                   (principalStretchDevPow[0][n] +
                    principalStretchDevPow[1][n] + 
                    principalStretchDevPow[2][n] ) / 3.0 );
               beta1 += myMu[n] / myAlpha[n] * 
                  (principalStretchDevPow[j][n]  - 
                   (principalStretchDevPow[0][n] +
                    principalStretchDevPow[1][n] + 
                    principalStretchDevPow[2][n] ) / 3.0 );

               // gamma_AB in Eq. 2.66 of Simo and Taylor (1991)    	  
               g33 += myMu[n] * ( principalStretchDevPow[i][n] / 3.0 + 
                                  ( principalStretchDevPow[0][n] + 
                                    principalStretchDevPow[1][n] + 
                                    principalStretchDevPow[2][n] ) / 9.0 );  
             
               g11 += myMu[n] * ( principalStretchDevPow[j][n] / 3.0 + 
                                  ( principalStretchDevPow[0][n] + 
                                    principalStretchDevPow[1][n] + 
                                    principalStretchDevPow[2][n] ) / 9.0 );  
             
               g13 += myMu[n] * ( -principalStretchDevPow[i][n] / 3.0 - 
                                  principalStretchDevPow[j][n] / 3.0 + 
                                  ( principalStretchDevPow[0][n] + 
                                    principalStretchDevPow[1][n] + 
                                    principalStretchDevPow[2][n] ) / 9.0 );  
            }
         }
              
         // the matrix mi - Eq. 2.15 of Simo and Taylor (1991)
         ma.set (myB2);
         ma.scaledAdd (-principalStretch2.get(k), myB);
         ma.scaledAdd (-principalStretch2.get(j), myB);
         ma.scaledAdd ( principalStretch2.get(j) * principalStretch2.get(k),
                        SymmetricMatrix3d.IDENTITY);
         ma.scale (1.0 / Di);

         // Calculate dgm term in Eq. 2.48b and Eq 2.70 of Simo and Taylor (1991)
         TensorUtils.addTensorProduct4 (c, 2.0 * (beta3-beta1) / J / Di, myB);
         TensorUtils.addTensorProduct  (c, -2.0 * (beta3-beta1) / J / Di, myB);
         TensorUtils.addScaledIdentityProduct (
            c, 2.0 * (beta3-beta1) / J * I3 / Di / principalStretch2.get(i));
         TensorUtils.addScaledIdentity (
            c, -2.0 * (beta3-beta1) / J * I3 / Di / principalStretch2.get(i));
         TensorUtils.addSymmetricTensorProduct (
            c, 2.0 * (beta3-beta1) / J / Di * principalStretch2.get(i),myB,ma);
         TensorUtils.addTensorProduct  (
            c, -1.0 * (beta3-beta1) / J / Di * Dpi * principalStretch.get(i), ma);
         TensorUtils.addSymmetricTensorProduct (
            c, -2.0 * (beta3-beta1) / J / Di * I3 / principalStretch2.get(i), 
            SymmetricMatrix3d.IDENTITY, ma);

         // Calculate other terms in Eq 2.70 of Simo and Taylor (1991)
         TensorUtils.addScaledIdentity (c, -2.0 * beta1 / J);
         myTmp.set (SymmetricMatrix3d.IDENTITY);
         myTmp.scaledAdd (-1.0, ma);
         TensorUtils.addTensorProduct  (c, g11 / J, myTmp);
         TensorUtils.addTensorProduct  (c, g33 / J, ma);
         TensorUtils.addSymmetricTensorProduct (c, g13 / J, ma, myTmp);
      }
      
      c.m00 += - p;
      c.m11 += - p;
      c.m22 += - p;

      c.m01 += p;
      c.m02 += p;
      c.m12 += p;
      
      c.m33 += - p;
      c.m44 += - p;
      c.m55 += - p;

      c.setLowerToUpper();

   }

   public boolean equals (FemMaterial mat) {
      if (!(mat instanceof OgdenMaterial)) {
         return false;
      }
      OgdenMaterial stvk = (OgdenMaterial)mat;
      if (myMu != stvk.myMu ||
          myAlpha != stvk.myAlpha) {
         return false;
      }
      else {
         return super.equals (mat);
      }
   }

   public OgdenMaterial clone() {
      OgdenMaterial mat = (OgdenMaterial)super.clone();
      mat.myB = new SymmetricMatrix3d();
      return mat;
   }

   public static void main (String[] args) {
      OgdenMaterial mat = new OgdenMaterial();

      SolidDeformation def = new SolidDeformation();
      Matrix3d Q = new Matrix3d();
      def.setF (new Matrix3d (1.1, 0.1, 0.2, 0.3, 0.8, 0.23, 0.3, 0.1, 1.5));

      Matrix6d D = new Matrix6d();
      SymmetricMatrix3d sig = new SymmetricMatrix3d();
      //      double[] alpha = {2.0, 2.0, 2.0, 2.0, 2.0, 2.0};
      //      double[] mu = {200.0, 0.0, 0.0, 0.0, 0.0, 0.0};
            
      mat.computeStress (sig, def, Q, null);
      System.out.println ("sig=\n" + sig.toString ("%12.6f"));
      mat.computeTangent (D, sig, def, Q, null);

      System.out.println ("D=\n" + D.toString ("%12.6f"));

   }

   @Override
      public void scaleDistance (double s) {
      if (s != 1) {
         super.scaleDistance (s);
         setMu1 (myMu[0]/s);
         setMu2 (myMu[1]/s);
         setMu3 (myMu[2]/s);
         setMu4 (myMu[3]/s);
         setMu5 (myMu[4]/s);
         setMu6 (myMu[5]/s);
      }
   }

   @Override
      public void scaleMass (double s) {
      if (s != 1) {
         super.scaleMass (s);
         setMu1 (myMu[0]*s);
         setMu2 (myMu[1]*s);
         setMu3 (myMu[2]*s);
         setMu4 (myMu[3]*s);
         setMu5 (myMu[4]*s);
         setMu6 (myMu[5]*s);
      }
   }
   
   public boolean isIncompressible() {
      return true;
   }
}
