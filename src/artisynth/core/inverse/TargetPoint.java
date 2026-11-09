/**
 * Copyright (c) 2014, by the Authors: Ian Stavness (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.inverse;

import java.util.List;

import artisynth.core.mechmodels.Point;
import artisynth.core.modelbase.*;
import maspack.geometry.GeometryTransformer;
import maspack.matrix.Point3d;
import maspack.matrix.*;
import maspack.properties.*;

/**
 * A specialized Point class used to store and render the target position for
 * points being tracked by the tracking controller.
 */
public class TargetPoint extends Point implements TrackingTarget {

   public static final double DEFAULT_WEIGHT = 1.0;
   protected double myWeight = DEFAULT_WEIGHT;

   public static final Vector3d DEFAULT_SUB_WEIGHTS = new Vector3d (1, 1, 1);
   protected Vector3d mySubWeights = new Vector3d (DEFAULT_SUB_WEIGHTS);

   public static PropertyList myProps =
      new PropertyList (TargetPoint.class, ModelComponentBase.class);

   static {
      myProps.add ("renderProps * *", "render properties", null);
      myProps.add ("position * *", "position state", Point3d.ZERO, "%.8g");
      myProps.add ("velocity * *", "velocity state", Vector3d.ZERO, "%.8g");
      myProps.add(
         "weight", "weighting used for this target", DEFAULT_WEIGHT);
      myProps.add(
         "subWeights", "coordinate level sub weights for this target",
         DEFAULT_SUB_WEIGHTS);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public double getWeight() {
      return myWeight;
   }

   public void setWeight (double w) {
      myWeight = w;
   }

   public Vector3d getSubWeights() {
      return new Vector3d (mySubWeights);
   }

   public void setSubWeights (Vector3d w) {
      mySubWeights.set (w);
   }

   public void setSubWeights (VectorNd w) {
      mySubWeights.set (new Vector3d(w));
   }

   public int getTargetSize() {
      return 3;
   }

   // set whenever the velocity is set via setVelocity(); used by the
   // tracking controller to determine whether the target velocity has been
   // supplied for the current step
   protected boolean myVelocitySet = false;

   /**
    * {@inheritDoc}
    *
    * <p>Also records that the velocity has been explicitly set, so that the
    * tracking controller will use it instead of estimating it.
    */
   @Override
   public void setVelocity (Vector3d vel) {
      super.setVelocity (vel);
      myVelocitySet = true;
   }

   /**
    * Queries whether the velocity has been set via {@code setVelocity()}
    * since the last call to {@link #clearVelocitySet}.
    */
   boolean isVelocitySet() {
      return myVelocitySet;
   }

   /**
    * Clears the flag indicating that the velocity has been set.
    */
   void clearVelocitySet() {
      myVelocitySet = false;
   }

   public TargetPoint () {
      super.setDynamic (false);
   }

   public TargetPoint (Point3d pnt) {
      super (pnt);
      super.setDynamic (false);
      myTarget.setTargetPos (pnt);
   }
   
   @Override
   protected void setDynamic (boolean dynamic) {
      // prevent setting as dynamic
   }

   public void transformGeometry (
      GeometryTransformer gtr, TransformGeometryContext context, int flags) {
      super.transformGeometry (gtr, context, flags);
      // XXX should actually transform target pos; here assuming target==actual
      if (myTarget != null) {
         myTarget.setTargetPos (getPosition());
      }
   }

   public Point getSourceComp() {
      return getSourcePoint();
   }         

   private Point getSourcePoint() {
      CompositeComponent ccomp = getGrandParent();
      if (ccomp instanceof MotionTargetTerm) {
         MotionTargetTerm motionTerm = (MotionTargetTerm)ccomp;
         int idx = getParent().indexOf(this);
         if (motionTerm.mySourcePoints.size() > idx) {
            return motionTerm.getSourcePoint (idx);
         }
      }
      return null;
   }

   @Override
   public void getHardReferences (List<ModelComponent> refs) {
      Point point = getSourcePoint();
      if (point != null) {
         refs.add (point);
      }
   }
}
