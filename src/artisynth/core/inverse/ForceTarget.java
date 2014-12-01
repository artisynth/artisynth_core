package artisynth.core.inverse;

import maspack.matrix.VectorNd;
import artisynth.core.mechmodels.Constrainer;
import artisynth.core.mechmodels.PlanarConnector;

public class ForceTarget {
protected VectorNd target_lambda;
protected PlanarConnector con;

public ForceTarget()
{}

public ForceTarget(VectorNd lam, PlanarConnector cons)
{
  target_lambda=lam;
  con=cons;
}

public void setTargetLambda(VectorNd lam)
{
   target_lambda=lam;   
}

public VectorNd getTargetLambda()
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

}
