package artisynth.core.modelbase;

import maspack.matrix.Vector3d;

public interface FieldPoint {

   Vector3d getRestPos();
   Vector3d getSpatialPos();
   
   int getElementNumber();
   int getPointIndex();
   double[] getNodeWeights();
   int[] getNodeNumbers();   
}
