///**
// * Copyright (c) 2014, by the Authors: Ian Stavness (UBC)
// *
// * This software is freely available under a 2-clause BSD license. Please see
// * the LICENSE file in the ArtiSynth distribution directory for details.
// */
//package artisynth.core.inverse;
//
//import maspack.matrix.MatrixNd;
//import maspack.matrix.VectorNd;
//import artisynth.core.mechmodels.MechSystemBase;
//
//public class StaticMotionTargetTerm extends LeastSquaresTermBase {
//
//   boolean debug = false;
//   boolean enabled = true;
//
//   protected MechSystemBase myMech;
//   protected MotionTerm myMotionTerm;
//   
//   protected VectorNd myCurrentVel = null;
//   protected VectorNd myTargetVel = null;
//   protected VectorNd myTargetWeights = null;
//   protected int myTargetVelSize;
//   
//   
//   public StaticMotionTargetTerm(TrackingController opt) {     
//      super();
//      myMech = opt.getMech();
//      myMotionTerm = new MotionTerm(opt);
//      int n = myMech.getActiveVelStateSize();
//      myTargetVel = new VectorNd(n);
//      myCurrentVel = new VectorNd(n);
//   }
//   
//   public int getTargetSize() {
//      return myMech.getActiveVelStateSize();
//   }
//   
//   public int getTerm (
//      MatrixNd H, VectorNd b, int rowoff, double t0, double t1) {
//      myTargetVel.setZero();
//      myMech.getActiveVelState(myCurrentVel);
//      return myMotionTerm.getTerm (
//         H, b, rowoff, t0, t1, myTargetVel, myCurrentVel, null, null);
//   }
//   
//   public void setTargetWeights(VectorNd wgts) {
//      if (wgts.size() == getTargetSize()) {
//         myTargetWeights.set(wgts);
//      }
//   }
//   
//   public VectorNd getTargetWeights() {
//      VectorNd wgts = new VectorNd(myTargetWeights.size());
//      
//      for (int i=0; i<myTargetWeights.size(); i++) {
//         wgts.set(i,myTargetWeights.get(i));
//      }
//      return wgts;
//      
//   }
//   
//   public void setWeight(double w) {
//      super.setWeight(w);
//      myMotionTerm.setWeight(myWeight);
//   }
//
//   public void dispose() {
//      myMotionTerm.dispose();
//   }   
//   
//}
