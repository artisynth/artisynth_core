package artisynth.core.inverse;

import maspack.matrix.VectorNd;

/**
 * Adds a cost proportional to the sum of the 
 * current values of the activations
 * -- used for l-2 regularization of delta-activations
 */
public class L2RegularizationTermForDeltas extends QPTermBase {

   TrackingController myController;
   VectorNd act = new VectorNd ();
   public static final double defaultWeight = 1e-3;
   
   public L2RegularizationTermForDeltas(TrackingController tcon) {
      this(tcon, defaultWeight);

   }
   
   public L2RegularizationTermForDeltas(TrackingController tcon, double weight) {
      super(weight);
      myController = tcon;
   }

   @Override
   protected void compute (double t0, double t1) {
      Q.setIdentity ();
      Q.scale (myWeight);
      myController.getExcitations (act, 0);
      for (int i=0; i<P.size(); i++) {
         P.set(i,myWeight*act.get (i));
      }
   }
   
   @Override
   public void setSize (int size) {
      super.setSize (size);
      act.setSize (size);
   }
}
