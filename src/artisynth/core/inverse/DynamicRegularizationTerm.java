package artisynth.core.inverse;

import maspack.matrix.MatrixNd;
import maspack.matrix.VectorNd;
import maspack.properties.PropertyList;
import maspack.util.NumberFormat;

/**
 * Extension of the L2RegularisationTerm that computes different weights
 * for each of the excitation sources, based on how well they contribute
 * to the desired motion. The weights are recomputed for every call to
 * getQP, i.e. every time the tracking controller's apply is called.
 * @author Teun Bartelds
 * @since  2014-10-07
 */
public class DynamicRegularizationTerm extends QPTermBase {
   
   public enum Mapping {
      EXPONENTIAL,
      MONOMIAL,
      SIGMOID
   }
   
   protected TrackingController myController;
   public static final double defaultWeight = 1e-4;
   
   MatrixNd Hm = new MatrixNd ();   //Jacobian matrix mapping muscle activations to target velocities
   VectorNd vbar = new VectorNd (); //velocity
   VectorNd w = new VectorNd ();    //weights of the individual excitation sources
   
   protected boolean isEnabled = true;
   protected boolean isNormalized = true;
   protected Mapping mapping = Mapping.EXPONENTIAL;
   protected double param = 5;
   protected double sigmoidMean = 0.5;
   
   public static PropertyList myProps =
      new PropertyList(DynamicRegularizationTerm.class, QPTermBase.class);
   
   static {
      myProps.add ("isEnabled * *", "enable/disable dynamic weights, N.B. if disabled the term behaves like a standard L2RegularizationTerm",true);
      myProps.add ("isNormalized * *", "enable/disable normalization of the dynamic weights", true); 
      myProps.add ("mapping * *","select mapping function to compute the dynamic weights",Mapping.MONOMIAL);
      myProps.add ("param * *", "parameter to tune the dynamic weights", 1);
      myProps.add ("sigmoidMean * *", "parameter to position the mean of the sigmoid function", 0.5,"[0,1]");
   }
   
   public DynamicRegularizationTerm (TrackingController controller) {
      this (controller,defaultWeight);
   }
   
   public DynamicRegularizationTerm (TrackingController controller, double weight) {
      super (weight);
      myController = controller;
   }
     
   public PropertyList getAllPropertyInfo() {
      return myProps;
   }
   
   public void computeWeights (double dt) {
      myController.getMotionTerm ().reGetTerm (Hm,vbar);
      Hm.scale(1/dt);           // makes results independent of the time step
      vbar.scale(1/dt);         // makes results independent of the time step
      VectorNd alpha = new VectorNd (Hm.colSize ());
      alpha.mulTranspose (Hm,vbar);
      System.out.println("\n");
      System.out.println(alpha.toString ("%8.2f"));
      w = applyMapping(alpha); // apply (non-linear) mapping to obtain weights
      System.out.println(w.toString ("%8.2f"));
   }
   
   /*
    * Apply a monotonic decreasing function that maps any real value to [0,1]
    * Returns a set of weights that penalizes low activations 
    */
   // TODO multiple maps: exponential (softmax), squared, higher powers?
   private VectorNd applyMapping (VectorNd x) {
      double[] w = new double[x.size()];
      double sum = 0;
      int n = w.length;
      
      switch (mapping) {
         case EXPONENTIAL:
            System.out.println("Applying exponential mapping");
            double min = x.minElement ();
            for (int i=0; i<n; i++) {
               w[i] = x.get(i) - min; // w(i) > 0 for numeric precision of exponential
               w[i] = Math.exp (-w[i]*param*1e-6);
               sum += w[i];
            }
            break;
         case MONOMIAL:
            System.out.println("Applying monomial mapping");
            double max = x.maxElement ();
            for (int i=0; i<n; i++) {
               w[i] = Math.pow(-x.get(i) + max, param);
               sum += w[i];
            }
            break;
         case SIGMOID:
            System.out.println("Applying sigmoid mapping");
            VectorNd sorted = new VectorNd();
            sorted.sort (x);
            /*
             * This part is a little hacky.. trying to position
             * the transition point of the sigmoid function at
             * some sort of median location. 
             */
            double l = sorted.get((int)Math.floor (sigmoidMean*(n-1)));
            double h = sorted.get((int)Math.ceil (sigmoidMean*(n-1)));
            double frac = sigmoidMean*(n-1) - Math.floor (sigmoidMean*(n-1));
            double mean = l + frac*(h-l);
            
            for (int i=0; i<n; i++) {
               w[i] = 1/( 1 + Math.exp( param*(x.get(i)-mean) ));
               sum += w[i];
            }
      }

      if (isNormalized) {
         for (int i=0; i<n; i++) {
            w[i] *= n/sum;
         }
      }
      return new VectorNd (w);
   }
   
   public void setIsEnabled (boolean enabled) {
      isEnabled = enabled;
   }
   
   public boolean getIsEnabled () {
      return isEnabled;
   }
   
   public void setIsNormalized (boolean normalized) {
      isNormalized = normalized;
   }
   
   public boolean getIsNormalized () {
      return isNormalized;
   }
   
   public void setParam (double parameter) {
      param = parameter;
   }
   
   public double getParam () {
      return param;
   }
   
   public void setSigmoidMean (double mean) {
      sigmoidMean = mean;
   }
   
   public double getSigmoidMean () {
      return sigmoidMean;
   }
   
   public void setMapping (Mapping m) {
      mapping = m;
      switch (m) {
         case EXPONENTIAL:
            param = 5;
            break;
         case MONOMIAL:
            param = 0.5;
            break;
         case SIGMOID:
            param = 1e-3;
      }
   }
   
   public Mapping getMapping () {
      return mapping;
   }
   
   @Override
   protected void compute (double t0, double t1) {
      Q.setIdentity();
      if (isEnabled) {
         computeWeights(t1-t0);
         Q.mulDiagonalLeft (w);
      }
      Q.scale(myWeight);
   }
}
