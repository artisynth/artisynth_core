package artisynth.core.inverse;

import artisynth.core.mechmodels.BodyConnector;
import artisynth.core.mechmodels.PlanarConnector;
import artisynth.core.mechmodels.SphericalJoint;
import artisynth.core.modelbase.RenderableComponentBase;
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

/**
 * Force Target base class
 * 
 * @author Ian Stavness, Benedikt Sagl
 *
 */
public class ForceTarget extends RenderableComponentBase implements HasProperties {
   protected VectorNd myTargetLambda = null;
   protected BodyConnector myConnector;
//   public static double[] lam = { 0 };
//   public static VectorNd DEFAULT_FORCE_TARGET = new VectorNd (lam);

   public static final double DEFAULT_ARROW_SIZE = 1d;
   double arrowSize = DEFAULT_ARROW_SIZE;

   static private RenderProps defaultRenderProps = new LineRenderProps ();

   public static PropertyList myProps = new PropertyList (ForceTarget.class);

   static {
      myProps.add (
         "renderProps * *", "render properties for this component",
         defaultRenderProps);
      myProps.add ("targetLambda", "force targets", null);
      myProps.add ("arrowSize * *", "arrow size", DEFAULT_ARROW_SIZE);
   }
   
   public PropertyList getAllPropertyInfo () {
      return myProps;
   }

   public ForceTarget () {
      setRenderProps (createRenderProps ());
   }

   public ForceTarget (BodyConnector con) { 
      this(con, new VectorNd (con.numBilateralConstraints ()));
   }
      
   public ForceTarget (BodyConnector con, VectorNd targetLambda) {
      this();
      if (con.numBilateralConstraints () != targetLambda.size ()) {
         System.err.println("provided target wrong size, got "+targetLambda.size ()+
            " expecting "+ con.numBilateralConstraints ());
      }
      myTargetLambda = new VectorNd(targetLambda);
      myConnector = con;
      setName (con.getName ()+"_target");
   }

   public void setTargetLambda (VectorNd lam) {
      myTargetLambda = lam;
   }

   public VectorNd getTargetLambda () {
      return myTargetLambda;
   }

   public void setConstraint (BodyConnector connector) {
      myConnector = connector;
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
      else if (myConnector instanceof SphericalJoint) {
         blk = new Matrix3x3DiagBlock (1d, 1d, 1d);
         J.addBlock (bi, solve_index, blk);
      }
      else {
         System.err.println("ForceTarget.addForceJacobian: unsupported connector type: "+myConnector.getClass ());
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
      else if (myConnector instanceof SphericalJoint) {
         startvec = myConnector.getCurrentTCW ().p;
         endvec.x = myTargetLambda.get (0) * arrowSize;
         endvec.y = myTargetLambda.get (1) * arrowSize;
         endvec.z = myTargetLambda.get (2) * arrowSize;
         endvec.add (startvec);
         set (start, startvec);
         set (end, endvec);
      }
   }
   
   public void setArrowSize (double size) {
      arrowSize = size;
   }

   public double getArrowSize () {
      return arrowSize;
   }


}
