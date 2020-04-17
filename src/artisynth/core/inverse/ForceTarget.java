package artisynth.core.inverse;

import java.io.*;
import java.util.Deque;

import artisynth.core.mechmodels.BodyConnector;
import artisynth.core.mechmodels.PlanarConnector;
import artisynth.core.mechmodels.SphericalJointBase;
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
 * Contains information about a single body connector whose constraint forces
 * are to be controlled using the ForceTargetTerm.
 * 
 * @author Ian Stavness, Benedikt Sagl
 */
public class ForceTarget extends RenderableComponentBase {
   
   public static final double DEFAULT_WEIGHT = 1.0;
   protected double myWeight = DEFAULT_WEIGHT;
   
   protected VectorNd myTargetLambda = null;
   protected BodyConnector myConnector;

   public static final double DEFAULT_ARROW_SIZE = 1d;
   double arrowSize = DEFAULT_ARROW_SIZE;

   static private RenderProps defaultRenderProps = new LineRenderProps ();

   public static PropertyList myProps =
      new PropertyList (ForceTarget.class, RenderableComponentBase.class);

   static {
      myProps.add (
         "renderProps * *", "render properties for this component",
         defaultRenderProps);
      myProps.add ("targetLambda", "force targets", null);
      myProps.add ("arrowSize", "arrow size", DEFAULT_ARROW_SIZE);
      myProps.add(
         "weight", "weighting used for this target", DEFAULT_WEIGHT);
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

   public ForceTarget () {
      setRenderProps (createRenderProps ());
   }

   public ForceTarget (BodyConnector con, VectorNd targetLambda) {
      this();
      if (con.numBilateralConstraints () != targetLambda.size ()) {
         throw new IllegalArgumentException (
            "Wrong target size ("+targetLambda.size ()+
            ", expecting "+ con.numBilateralConstraints ());
      }
      myTargetLambda = new VectorNd(targetLambda);
      setConnector (con);
   }

   public VectorNd getTargetLambda () {
      return myTargetLambda;
   }

   public void setTargetLambda (VectorNd lam) {
      myTargetLambda = lam;
   }

   public void setConnector (BodyConnector connector) {
      if (connector instanceof PlanarConnector ||
          connector instanceof SphericalJointBase) {
         myConnector = connector;
      }
      else {
         throw new IllegalArgumentException (
            "Connector type "+connector.getClass()+" is not supported");
      }
   }

   public int numBilateralConstraints() {
      if (myConnector != null) {
         return myConnector.numBilateralConstraints();
      }
      else {
         return 0;
      }
   }
   
   public BodyConnector getConnector () {
      return myConnector;
   }

   public int addForceJacobian (SparseBlockMatrix J, int bi, int solve_index) {
      MatrixBlock blk = null;
      if (myConnector instanceof PlanarConnector) {
         blk = new Matrix1x1Block ();
         blk.set (0, 0, 1d);
         J.addBlock (bi, solve_index, blk);
      }
      else if (myConnector instanceof SphericalJointBase) {
         blk = new Matrix3x3DiagBlock (1d, 1d, 1d);
         J.addBlock (bi, solve_index, blk);
      }
      else {
         throw new InternalErrorException (
            "Unsupported connector type: "+myConnector.getClass());
      }
      return bi++;
   }
   
   float[] start = new float[3];
   float[] end = new float[3];
   Vector3d startvec = new Vector3d ();
   Vector3d endvec = new Vector3d ();
   
   private void set (float[] dest, Vector3d src) {
      dest[0] = (float)src.get (0);
      dest[1] = (float)src.get (1);
      dest[2] = (float)src.get (2);
   }
   
   @Override
   public void render (Renderer renderer, int flags) {
      renderer.drawArrow (getRenderProps (), start, end, true, isSelected ());
   }

   @Override
   public void prerender (RenderList list) {
      super.prerender (list);
      if (myConnector instanceof PlanarConnector) {
         RigidTransform3d TDW = myConnector.getCurrentTDW ();
         startvec = TDW.p;
         endvec.transform (TDW, Vector3d.Z_UNIT);
         endvec.scale (arrowSize/endvec.norm ());
         endvec.scale (myTargetLambda.get (0));
         endvec.add (startvec);
         set (start, startvec);
         set (end, endvec);
      }
      else if (myConnector instanceof SphericalJointBase) {
         startvec = myConnector.getCurrentTCW ().p;
         endvec.x = myTargetLambda.get (0) * arrowSize;
         endvec.y = myTargetLambda.get (1) * arrowSize;
         endvec.z = myTargetLambda.get (2) * arrowSize;
         endvec.add (startvec);
         set (start, startvec);
         set (end, endvec);
      }
      else {
         throw new InternalErrorException (
            "Unsupported connector type: "+myConnector.getClass());
      }
   }
   
   public void setArrowSize (double size) {
      arrowSize = size;
   }

   public double getArrowSize () {
      return arrowSize;
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      if (myConnector != null) {
         pw.println (
            "connector="+ComponentUtils.getWritePathName (ancestor,myConnector));
      }
      super.writeItems (pw, fmt, ancestor);
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAndStoreReference (rtok, "connector", tokens)) {
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected boolean postscanItem (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
      
      if (postscanAttributeName (tokens, "connector")) {
         setConnector (
            postscanReference (tokens, BodyConnector.class, ancestor));
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }
}
