/**
 * Copyright (c) 2014, by the Authors: Ian Stavness (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.inverse;

import java.util.List;

import artisynth.core.mechmodels.*;
import artisynth.core.modelbase.*;
import maspack.geometry.GeometryTransformer;
import maspack.matrix.*;
import maspack.properties.*;

/**
 * A specialized Frame class used to store and render the target pose for
 * frames being tracked by the tracking controller. At present, this
 * is subclassed from RigidBody so that it can be given a mesh to render.
 */
public class TargetFrame extends RigidBody {

   public static final double DEFAULT_WEIGHT = 1.0;
   protected double myWeight = DEFAULT_WEIGHT;

   private static VectorNd createDefaultSubWeights() {
      VectorNd w = new VectorNd(6);
      for (int i=0; i<6; i++) {
         w.set (i, 1);
      }
      return w;
   }

   public static final VectorNd DEFAULT_SUB_WEIGHTS = createDefaultSubWeights();
   protected VectorNd mySubWeights = new VectorNd (DEFAULT_SUB_WEIGHTS);

   public static PropertyList myProps =
      new PropertyList (TargetFrame.class, ModelComponentBase.class);

   static {
      myProps.add (
         "renderProps * *", "render properties", null);
      myProps.add (
         "position", "position of the body coordinate frame",null,"NW");
      myProps.add (
         "orientation", "orientation of the body coordinate frame", null, "NW");
      myProps.add ("velocity * *", "velocity state", null, "NW");
      myProps.add (
         "axisLength * *", "length of rendered frame axes", 1f);
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

   public VectorNd getSubWeights() {
      return new VectorNd (mySubWeights);
   }

   public void setSubWeights (VectorNd w) {
      if (w.size() != 6) {
         throw new IllegalArgumentException (
            "specified subweights must have a size of 6");
      }
      mySubWeights.set (w);
   }

   public TargetFrame () {
      super.setDynamic (false);
   }

   public TargetFrame (RigidTransform3d X) {
      this();
      setPose(X);
   }
   
   /**
    * Cannot set target frame as dynamic
    */
   public void setDynamic (boolean dynamic) {
      // prevent setting as dynamic
   }

   public void transformGeometry (
      GeometryTransformer gtr, TransformGeometryContext context, int flags) {
      super.transformGeometry (gtr, context, flags);
      // XXX should actually transform target pos/rot; here assuming target==actual
      if (myTarget != null) {
         myTarget.setTargetPos (getPosition ());
         myTarget.setTargetRot (getOrientation ());
      }
   }   

   private Frame getSourceFrame() {
      CompositeComponent ccomp = getGrandParent();
      if (ccomp instanceof MotionTargetTerm) {
         MotionTargetTerm motionTerm = (MotionTargetTerm)ccomp;
         int idx = getParent().indexOf(this);
         if (motionTerm.mySourceFrames.size() > idx) {
            return motionTerm.getSourceFrame (idx);
         }
      }
      return null;
   }

   @Override
   public void getHardReferences (List<ModelComponent> refs) {
      Frame frame = getSourceFrame();
      if (frame != null) {
         refs.add (frame);
      }
   }


}
