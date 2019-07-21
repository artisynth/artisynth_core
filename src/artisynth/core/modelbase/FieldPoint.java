package artisynth.core.modelbase;

import maspack.matrix.Point3d;

public interface FieldPoint {

   static int NODAL_INFO = 0x1;
   static int ELEMENT_INFO = 0x2;
   static int SUBELEM_INFO = (0x4 | ELEMENT_INFO);
   static int ALL_INFO = (NODAL_INFO | ELEMENT_INFO);

   int availableInfo();

   Point3d getRestPos();
   Point3d getSpatialPos();

   double[] getNodeWeights();
   int[] getNodeNumbers();   
   
   int getElementType();
   int getElementNumber();
   int getElementSubIndex();
   int getPointIndex();
}
