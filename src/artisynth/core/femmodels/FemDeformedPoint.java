package artisynth.core.femmodels;

import maspack.matrix.*;
import artisynth.core.materials.DeformedPointBase;

public class FemDeformedPoint extends DeformedPointBase {

   protected Matrix3d myJ;

   protected boolean myRestPosValid = false;
   protected boolean mySpatialPosValid = false;

   protected IntegrationPoint3d myIpnt;
   protected FemElement myElem;

   public FemDeformedPoint() {
      super();
      myNodeNumbers = new int[0];
      myNodeWeights = new double[0];
      myJ = new Matrix3d();
   }

   public Matrix3d getJ() {
      return myJ;
   }

   public void setFromIntegrationPoint (
      IntegrationPoint3d ipnt, IntegrationData3d idat,
      RotationMatrix3d R, FemElement elem, int idx) {

      myElem = elem;
      myElemNum = elem.getNumber();
      myIpnt = ipnt;
      myPointIdx = idx;
      myP = 0; // assume 0 by default

      myJ.setZero();
      FemNode3d[] nodes = (FemNode3d[])elem.getNodes();
      if (nodes.length != myNodeNumbers.length) {
         myNodeNumbers = new int[nodes.length];
         myNodeWeights = new double[nodes.length];
      }
      VectorNd N = ipnt.getShapeWeights();
      for (int i=0; i<nodes.length; i++) {
         Vector3d pos = nodes[i].getLocalPosition();
         Vector3d dNds = ipnt.GNs[i];
         myNodeNumbers[i] = nodes[i].getNumber();
         myNodeWeights[i] = N.get(i);
         myJ.addOuterProduct (pos.x, pos.y, pos.z, dNds.x, dNds.y, dNds.z);
      }
      myF.mul (myJ, idat.myInvJ0);
      if (elem.getPreStrain() != null) {
         myF.mul (elem.getPreStrain());
      }
      myDetF = myF.determinant();      
      setR (R);

      myRestPosValid = false;
      mySpatialPosValid = false;
   }
   
   public Point3d getSpatialPos() {
      if (!mySpatialPosValid) {
         myIpnt.computePosition (mySpatialPos, myElem.getNodes());
         mySpatialPosValid = false;
      }
      return mySpatialPos;
   }

   public Point3d getRestPos() {
      if (!myRestPosValid) {
         myIpnt.computeRestPosition (myRestPos, myElem.getNodes());
         myRestPosValid = false;
      }
      return myRestPos;
   }
   
}
