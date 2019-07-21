package artisynth.core.femmodels;

import artisynth.core.modelbase.FieldPoint;
import maspack.matrix.Point3d;

public class NodalFieldPoint implements FieldPoint {

   private FemNode myNode;
   private int[] myNodeNumbers;
   private double[] myNodeWeights;

   NodalFieldPoint () {
      myNodeNumbers = new int[1];
      myNodeWeights = new double[] { 1.0 };         
   }

   public void setNode (FemNode node) {
      myNode = node;
      myNodeNumbers[0] = node.getNumber();
   }

   public int availableInfo() {
      return NODAL_INFO;
   }

   public Point3d getSpatialPos() {
      return myNode.getPosition();
   }

   public Point3d getRestPos() {
      return myNode.getRestPosition();
   }

   public int getPointIndex() {
      return -1;
   }

   public int[] getNodeNumbers() {
      return myNodeNumbers;
   }

   public double[] getNodeWeights() {
      return myNodeWeights;
   }
   
   public int getElementType() {
      return -1;
   }

   public int getElementNumber() {
      return -1;
   }

   public int getElementSubIndex() {
      return -1;
   }

}
