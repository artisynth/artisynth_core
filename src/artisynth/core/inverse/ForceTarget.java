package artisynth.core.inverse;

import maspack.matrix.Matrix1x1Block;
import maspack.matrix.Matrix3x3Block;
import maspack.matrix.Matrix3x3DiagBlock;
import maspack.matrix.MatrixBlock;
import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.VectorNd;
import maspack.properties.HasProperties;
import maspack.properties.Property;
import maspack.properties.PropertyList;
import artisynth.core.mechmodels.PlanarConnector;
import artisynth.core.mechmodels.RigidBodyConnector;
import artisynth.core.mechmodels.SphericalJoint;
import artisynth.core.mechmodels.RigidBodyConnector;
import artisynth.core.modelbase.ModelComponentBase;

/**
 * Force Target base class
 * 
 * @author Ian Stavness, Benedikt Sagl
 *
 */
public class ForceTarget extends ModelComponentBase implements HasProperties {
   protected VectorNd myTargetLambda = null;
   protected RigidBodyConnector myConstraint;
//   public static double[] lam = { 0 };
//   public static VectorNd DEFAULT_FORCE_TARGET = new VectorNd (lam);

   public static PropertyList myProps = new PropertyList (ForceTarget.class);

   static {
      myProps.add ("MyTargetForce", "force targets", null);
   }
   
   public PropertyList getAllPropertyInfo () {
      return myProps;
   }

   public ForceTarget () {
   }

   public ForceTarget (VectorNd lam, RigidBodyConnector con) {
      myTargetLambda = new VectorNd(lam);
      myConstraint = con;
      setName (con.getName ()+"_target");
   }

   public void setMyTargetForce (VectorNd lam) {
      myTargetLambda = lam;
   }

   public VectorNd getMyTargetForce () {
      return myTargetLambda;
   }

   public void setConstraint (RigidBodyConnector cons) {
      myConstraint = cons;
   }

   public RigidBodyConnector getConstraint () {
      return myConstraint;
   }

   public String getConstraintName () {
      return myConstraint.getName ();
   }

   public int addForceJacobian (SparseBlockMatrix J, int bi, int solve_index) {
      MatrixBlock blk = null;
      if (myConstraint instanceof PlanarConnector) {
         blk = new Matrix1x1Block ();
         blk.set (0, 0, 1d);
         J.addBlock (bi, solve_index, blk);
      }
      else if (myConstraint instanceof SphericalJoint) {
         blk = new Matrix3x3DiagBlock (1d, 1d, 1d);
         J.addBlock (bi, solve_index, blk);
      }
      else {
         System.err.println("ForceTarget.addForceJacobian: unsupported constraint type: "+myConstraint.getClass ());
      }
      return bi++;
   }
}
