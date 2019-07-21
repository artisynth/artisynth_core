package artisynth.core.femmodels;

import maspack.matrix.*;
import artisynth.core.materials.DeformedPointBase;
import artisynth.core.femmodels.FemElement.ElementClass;

public class FemDeformedPoint extends DeformedPointBase {

   protected Matrix3d myJ;

   protected boolean myRestPosValid = false;
   protected boolean mySpatialPosValid = false;

   // both of these are used for computing rest and spatial positions on demand
   protected IntegrationPoint3d myIpnt;
   protected FemElement3dBase myElem;

   public FemDeformedPoint() {
      super();
      myNodeNumbers = new int[0];
      myNodeWeights = new double[0];
      myJ = new Matrix3d();
   }

   public Matrix3d getJ() {
      return myJ;
   }
   
   public void setCoordsOnly (
      IntegrationPoint3d ipnt, IntegrationData3d idat,
      FemElement3dBase elem, int idx) {

      myElem = elem;
      if (elem.getElementClass() == ElementClass.VOLUMETRIC) {
         myElemType = 0;
      }
      else {
         myElemType = 1;
      }
      myElemNum = elem.getNumber();
      myElemSubIndex = ipnt.getNumber();
      myIpnt = ipnt;
      myPointIdx = elem.getIntegrationIndex() + idx;
      myP = 0; // assume 0 by default

      FemNode3d[] nodes = (FemNode3d[])elem.getNodes();
      if (nodes.length != myNodeNumbers.length) {
         myNodeNumbers = new int[nodes.length];
         myNodeWeights = new double[nodes.length];
      }
      VectorNd N = ipnt.getShapeWeights();
      for (int i=0; i<nodes.length; i++) {
         myNodeNumbers[i] = nodes[i].getNumber();
         myNodeWeights[i] = N.get(i);
      }
      myRestPosValid = false;
      mySpatialPosValid = false;
   }

   public void setFromIntegrationPoint (
      IntegrationPoint3d ipnt, IntegrationData3d idat,
      RotationMatrix3d R, FemElement3dBase elem, int idx) {

      myElem = elem;
      if (elem.getElementClass() == ElementClass.VOLUMETRIC) {
         myElemType = 0;
      }
      else {
         myElemType = 1;
      }
      myElemNum = elem.getNumber();
      myElemSubIndex = ipnt.getNumber();
      myIpnt = ipnt;
      myPointIdx = elem.getIntegrationIndex() + idx;
      myP = 0; // assume 0 by default

      FemNode3d[] nodes = (FemNode3d[])elem.getNodes();
      if (nodes.length != myNodeNumbers.length) {
         myNodeNumbers = new int[nodes.length];
         myNodeWeights = new double[nodes.length];
      }
      ipnt.computeJacobian (myJ, nodes);
      myF.mul (myJ, idat.myInvJ0);
      if (elem.getPlasticDeformation() != null) {
         myF.mulInverse (elem.getPlasticDeformation());
      }
      myDetF = myF.determinant();      
      VectorNd N = ipnt.getShapeWeights();
      for (int i=0; i<nodes.length; i++) {
         myNodeNumbers[i] = nodes[i].getNumber();
         myNodeWeights[i] = N.get(i);
      }
      setR (R);

      myRestPosValid = false;
      mySpatialPosValid = false;
   }
   
   public void setFromRestPoint (
      IntegrationPoint3d ipnt, IntegrationData3d idat,
      RotationMatrix3d R, FemElement3dBase elem, int idx) {

      myElem = elem;
      if (elem.getElementClass() == ElementClass.VOLUMETRIC) {
         myElemType = 0;
      }
      else {
         myElemType = 1;
      }
      myElemNum = elem.getNumber();
      myElemSubIndex = ipnt.getNumber();
      myIpnt = ipnt;
      myPointIdx = elem.getIntegrationIndex() + idx;
      myP = 0; // assume 0 by default

      FemNode3d[] nodes = (FemNode3d[])elem.getNodes();
      if (nodes.length != myNodeNumbers.length) {
         myNodeNumbers = new int[nodes.length];
         myNodeWeights = new double[nodes.length];
      }
      ipnt.computeRestJacobian (myJ, nodes); //.invert (idat.myInvJ0);
      VectorNd N = ipnt.getShapeWeights();
      for (int i=0; i<nodes.length; i++) {
         myNodeNumbers[i] = nodes[i].getNumber();
         myNodeWeights[i] = N.get(i);
      }
      if (elem.getPlasticDeformation() != null) {
         // compute the strain E associated with the plastic deformation ...
         Matrix3d E = new Matrix3d();
         E.setSymmetric(elem.getPlasticDeformation());
         E.m00 -= 1;
         E.m11 -= 1;
         E.m22 -= 1;
         // ... and then set myF to correspond to I - E
         myF.sub (Matrix3d.IDENTITY, E);
      }
      else {
         myF.setIdentity();
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
