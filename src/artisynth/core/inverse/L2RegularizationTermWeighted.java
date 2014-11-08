package artisynth.core.inverse;

import artisynth.core.modelbase.ControllerBase;
import maspack.matrix.MatrixNd;
import maspack.matrix.VectorNd;
import maspack.properties.PropertyList;

/**
 * Extension of the L2RegularisationTerm that computes different weights
 * for each of the excitation sources, based on how well they contribute
 * to the desired motion. The weights are recomputed for every call to
 * getTerm, i.e. every time the tracking controller's apply is called.
 * @author Teun Bartelds
 * @since  2014-10-07
 */
public class L2RegularizationTermWeighted extends L2RegularizationTerm {
   
   protected TrackingController myController;
   public static final double defaultWeight = 1e-4;
   
   MatrixNd J = new MatrixNd (); //Jacobian matrix mapping muscle activations to target velocities
   VectorNd v = new VectorNd (); //velocity
   VectorNd w = new VectorNd (); //weights of the individual excitation sources
   
   protected boolean isEnabled = true;
   protected boolean isNormalized = false;
   protected double param = 1;
   
   public static PropertyList myProps =
      new PropertyList(L2RegularizationTermWeighted.class, L2RegularizationTerm.class);
   
   static {
      myProps.add ("isEnabled * *", "enable/disable dynamic weights",true);
      myProps.add ("isNormalized * *", "enable/disable normalization of the dynamic weights", true);
      myProps.add ("param * *", "parameter to tune the dynamic weights", 1);
   }
   
   public L2RegularizationTermWeighted (TrackingController controller) {
      this (controller,defaultWeight);
   }
   
   public L2RegularizationTermWeighted (TrackingController controller, double weight) {
      super (weight);
      myController = controller;
   }
   
   @Override
   protected void compute (double t0, double t1) {
      H.setIdentity();
      H.scale(Math.sqrt(myWeight));
      if (isEnabled) {
         computeWeights();
         H.mulDiagonalLeft (w);
      }
      //System.out.println("L2Dynamic (t1=" + t1 + "):\n" + H);
   }
   
   public PropertyList getAllPropertyInfo() {
      return myProps;
   }
   
   public void computeWeights () {
      myController.getMotionTerm ().reGetTerm (J,v);
      VectorNd dot = new VectorNd (v.size ());
      dot.mulTranspose (J,v);
      w = applyMapping(dot); // apply (non-linear) mapping to obtain weights
   }
   
   /*
    * Apply a monotonic decreasing function that maps any real value to [0,1]
    * Returns a set of weights that penalizes low activations 
    */
   // TODO multiple maps: exponential (softmax), squared, higher powers?
   private VectorNd applyMapping (VectorNd x) {
      double[] w = new double[x.size()];
      double sum = 0;
      for (int i = 0; i < w.length; i++) {
         w[i] = Math.exp (-x.get(i)*param);
         sum += w[i];
      }
      if (isNormalized) {
         for (int i = 0; i < w.length; i++) {
            w[i] /= sum;
         }
      }
      for (int i = 0; i < w.length; i++) {
         //w[i] *= w.length;
         //w[i] = Math.sqrt (w[i]);
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
}
