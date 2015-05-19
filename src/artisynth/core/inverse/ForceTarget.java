package artisynth.core.inverse;

import maspack.matrix.Matrix1x1Block;
import maspack.matrix.Matrix3x3DiagBlock;
import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.VectorNd;
import maspack.properties.HasProperties;
import maspack.properties.Property;
import maspack.properties.PropertyList;
import artisynth.core.mechmodels.PlanarConnector;
import artisynth.core.modelbase.ModelComponentBase;
/**
 * Force Target base class
 * 
 * @author Ian Stavness, Benedikt Sagl
 *
 */
public class ForceTarget extends ModelComponentBase implements HasProperties {
protected VectorNd target_lambda;
protected PlanarConnector con;
public static PropertyList myProps= new PropertyList(ForceTarget.class);
public static double[] lam={0};
public static VectorNd DEFAULT_FT= new VectorNd (lam);
protected String name;
static{
myProps.add ("MyTargetForce","force targets", DEFAULT_FT);
myProps.add ("Name","Name","");
}
public ForceTarget()
{}

public ForceTarget(VectorNd lam, PlanarConnector cons)
{
  target_lambda=lam;
  con=cons;
  setName(cons.getName ());
}

public void setMyTargetForce(VectorNd lam)
{
   target_lambda=lam;   
}

public VectorNd getMyTargetForce()
{
   return target_lambda;   
}

public void setConstrainer(PlanarConnector cons)
{
   con=cons;
}
public PlanarConnector getConstrainer()
{
   return con;
}
public PropertyList getAllPropertyInfo() {
   return myProps;
}

public Property getProperty(String pathName) {
   return PropertyList.getProperty(pathName, this);
}
public String getConstraintName()
{
   return con.getName ();
}
public String getName()
{
   return name;
}
public void setName(String nom)
{
   name=nom;
}

public int addForceJacobian (SparseBlockMatrix J, int bi, int solve_index) {
   
   Matrix1x1Block blk = new Matrix1x1Block();
   double[] val={1};
   blk.set(val);
   J.addBlock (bi, solve_index, blk);
   return bi++;
}
}

