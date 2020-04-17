package artisynth.core.inverse;

import java.io.*;
import java.util.Deque;

import artisynth.core.mechmodels.*;
import artisynth.core.modelbase.*;
import artisynth.core.util.*;
import maspack.matrix.Matrix1x1Block;
import maspack.matrix.Matrix3x3DiagBlock;
import maspack.matrix.MatrixBlock;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.properties.HasProperties;
import maspack.properties.PropertyList;
import maspack.render.LineRenderProps;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.util.*;

/**
 * Contains information a single force effector target for the
 * ForceMinimizationTerm.
 * 
 * @author John E Lloyd
 */
public class ForceEffectorTarget extends ModelComponentBase {
   
   public static final boolean DEFAULT_STATIC_ONLY = true;
   protected boolean myStaticOnly = DEFAULT_STATIC_ONLY;

   public static final double DEFAULT_WEIGHT = 1.0;
   protected double myWeight = DEFAULT_WEIGHT;

   protected VectorNd mySubWeights = null;

   protected ForceTargetComponent myForceComp;
   protected VectorNd myTargetForce = null;
   

   public static PropertyList myProps =
      new PropertyList (ForceEffectorTarget.class, ModelComponentBase.class);

   static {
      myProps.add(
         "staticOnly", "only control static forces", DEFAULT_STATIC_ONLY);
      myProps.add(
         "weight", "weighting used for this target", DEFAULT_WEIGHT);
      myProps.add (
         "subWeights", 
         "per-constraint subweights for this target", null);
      myProps.add (
         "targetForce", "force targets", null);
   }
   
   public PropertyList getAllPropertyInfo () {
      return myProps;
   }

   public double getWeight() {
      return myWeight;
   }

   public void setWeight (double w) {
      myWeight = w;
   }

   public boolean getStaticOnly() {
      return myStaticOnly;
   }

   public void setStaticOnly (boolean enable) {
      myStaticOnly = enable;
   }

   private void initializeSubWeights (ForceTargetComponent comp) {
      mySubWeights = new VectorNd (comp.getForceSize());
      for (int i=0; i<mySubWeights.size(); i++) {
         mySubWeights.set (i, 1.0);
      }
   }

   public VectorNd getSubWeights() {
      if (mySubWeights != null) {
         return new VectorNd (mySubWeights);
      }
      else {
         return null;
      }
   }

   public void setSubWeights (VectorNd weights) {
      if (mySubWeights != null) {
         if (mySubWeights.size() != weights.size()) {
            throw new IllegalArgumentException (
               "weights has size "+weights.size()+
               ", expected "+mySubWeights.size());
         }
         mySubWeights.set (weights);
      }
      else {
         mySubWeights = new VectorNd(weights);
      }
   }

   public ForceEffectorTarget () {
      super();
   }

   public ForceEffectorTarget (
      ForceTargetComponent comp, boolean staticOnly) {
      setForceComp (comp);
      setStaticOnly (staticOnly);
      initializeSubWeights (comp);
      myTargetForce = new VectorNd (comp.getForceSize());
   }

   public void setForceComp (ForceTargetComponent comp) {
      myForceComp = comp;
   }
   
   public ForceTargetComponent getForceComp () {
      return myForceComp;
   }

   public VectorNd getTargetForce () {
      return myTargetForce;
   }

   public void setTargetForce (VectorNd force) {
      myTargetForce = force;
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      if (myForceComp != null) {
         pw.println (
            "forceComp="+ComponentUtils.getWritePathName (
               ancestor,myForceComp));
      }
      super.writeItems (pw, fmt, ancestor);
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAndStoreReference (rtok, "forceComp", tokens)) {
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected boolean postscanItem (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
      
      if (postscanAttributeName (tokens, "forceComp")) {
         setForceComp (
            postscanReference (tokens, ForceTargetComponent.class, ancestor));
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }
}
