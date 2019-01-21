/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mfreemodels;

import java.util.ArrayList;
import java.util.Arrays;

import artisynth.core.femmodels.FemNode3d;
import artisynth.core.femmodels.IntegrationPoint3d;
import artisynth.core.mechmodels.PointState;
import maspack.matrix.Matrix3d;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;

public class MFreeIntegrationPoint3d extends IntegrationPoint3d implements MFreePoint3d {

   FemNode3d[] myDependentNodes;
   PointState myState;
   Point3d myRest;

   public MFreeIntegrationPoint3d() {
      super(0);
      myState = new PointState();
      myRest = new Point3d();
   }

   public MFreeIntegrationPoint3d(FemNode3d[] deps, VectorNd coords) {
      this();
      setDependentNodes(deps,coords);
   }

   public FemNode3d[] getDependentNodes() {
      return myDependentNodes;
   }

   public void setDependentNodes(FemNode3d[] nodes, VectorNd coords) {
      myDependentNodes = Arrays.copyOf(nodes, nodes.length);
      super.init(myDependentNodes.length, 1);
      setPressureWeights(new VectorNd(new double[]{1}));  // XXX default pressure weights?
      setNodeCoordinates(coords);
      updateSlavePos();
      computeRestPosition(myRest);
      setCoords(myRest.x, myRest.y, myRest.z);
   }

   public Point3d getPosition() {
      return myState.getPos();
   }

   public void computeRestPosition(Vector3d rest) {
      double[] Nbuf = N.getBuffer();
      rest.setZero();
      for (int i=0; i<myDependentNodes.length; i++) {
         rest.scaledAdd (Nbuf[i], myDependentNodes[i].getRestPosition());
      }
   }
   
   @Override
   public Point3d getRestPosition() {
      return myRest;
   }

   public VectorNd getNodeCoordinates() {
      return getShapeWeights();
   }

   public int getNodeCoordIdx(FemNode3d node) {
      for (int i=0; i<myDependentNodes.length; ++i) {
         if (node == myDependentNodes[i]) {
            return i;
         }
      }
      return -1;
   }

   public double getShapeCoordinate(FemNode3d node) {
      int idx = getNodeCoordIdx(node);
      if (idx < 0) {
         return 0;
      }
      return getShapeWeights().get(idx);
   }

   public void setNodeCoordinates(VectorNd coords) {
      setShapeWeights(coords);
      computeRestPosition(myRest);
      updatePosState();
      updateVelState();
   }

   public void updatePosState() {
      myState.setPos(Point3d.ZERO);
      for (int i=0; i<myDependentNodes.length; i++) {
         myState.scaledAddPos(N.get(i),myDependentNodes[i].getPosition());
      }
   }

   public void updateVelState() {
      myState.setVel(Vector3d.ZERO);
      for (int i=0; i<myDependentNodes.length; i++) {
         myState.scaledAddVel(N.get(i),myDependentNodes[i].getVelocity());
      }
   }

   public void updateSlavePos() {
      updatePosState();
      updateVelState();
   }

   /** 
    * Create an integration point for a given element.
    */
   public static MFreeIntegrationPoint3d create (MFreeNode3d[] dependentNodes, VectorNd shapeN, ArrayList<Vector3d> shapeGrad, double w) {

      int nnodes = dependentNodes.length;
      MFreeIntegrationPoint3d ipnt = new MFreeIntegrationPoint3d(dependentNodes, shapeN);
      ipnt.setWeight(w);

      for (int i=0; i<nnodes; i++) {
         ipnt.setShapeGrad (i, shapeGrad.get(i));
      }
      return ipnt;
   }

//   public void computeJacobian () {
//      myJ.setZero();
//      for (int i=0; i<myDependentNodes.length; i++) {
//         Point3d pos = myDependentNodes[i].getPosition();
//         Vector3d dNds = GNs[i];
//         myJ.addOuterProduct (pos.x, pos.y, pos.z, 
//            dNds.x, dNds.y, dNds.z);
//      }
//
//   }
   
//   public double computeJacobian (Matrix3d J) {
//      J.setZero();
//      for (int i=0; i<myDependentNodes.length; i++) {
//         Vector3d pos = myDependentNodes[i].getPosition();
//         Vector3d dNds = GNs[i];
//         J.addOuterProduct (pos.x, pos.y, pos.z, dNds.x, dNds.y, dNds.z);
//      }
//      return J.determinant();
//   }
//   
//   public double computeGradient (Matrix3d F, Matrix3d invJ0) {
//      computeJacobian (F);
//      if (invJ0 != null) {
//         F.mul (F, invJ0);
//      }
//      return F.determinant();      
//   }
   
//   public void computeJacobianAndGradient (Matrix3d invJ0) {
//
//      myJ.setZero();
//      for (int i=0; i<myDependentNodes.length; i++) {
//         Vector3d pos = myDependentNodes[i].getPosition();
//         Vector3d dNds = GNs[i];
//         myJ.addOuterProduct (pos.x, pos.y, pos.z, dNds.x, dNds.y, dNds.z);
//      }
//
//      if (invJ0 != null) {
//         F.mul (myJ, invJ0);
//      } else {
//         F.set(myJ);
//      }      
//      detF = F.determinant();
//   }

//   public void computeJacobianAndGradient() {
//      computeJacobianAndGradient(null);
//   }

//   public void computeGradientForRender (Matrix3d Fmat, 
//      Matrix3d invJ0) {
//
//      // compute J in Fmat
//      Fmat.setZero();
//      for (int i=0; i<myDependentNodes.length; i++) {
//         Point3d pos = myDependentNodes[i].getPosition();
//         Vector3d dNds = GNs[i];
//         Fmat.addOuterProduct (pos.x,pos.y,pos.z, dNds.x, dNds.y, dNds.z);
//      }      
//      if (invJ0 != null) {
//         Fmat.mul (invJ0);
//      }
//   }
//
//   public void computeGradientForRender (Matrix3d Fmat) {
//      computeGradientForRender(Fmat, null);
//   }

   public void computePosition (Point3d pos) {
      double[] Nbuf = N.getBuffer();
      for (int i=0; i<myDependentNodes.length; i++) {
         pos.scaledAdd (Nbuf[i], myDependentNodes[i].getPosition());
      }
   }
//
//   public double getDetJ() {
//      return myJ.determinant();
//   }

   public boolean reduceDependencies(double tol) {

      int ndeps = 0;
      boolean changed = false;
      for (int i=0; i<myDependentNodes.length; i++) {
         if (Math.abs(N.get(i)) <= tol) {
            changed = true;
            N.set(i, 0);
         } else {
            if (changed) {
               myDependentNodes[ndeps] = myDependentNodes[i];
               N.set(ndeps, N.get(i));
            }
            ++ndeps;
         }
      }
      if (changed) {
         myDependentNodes = Arrays.copyOf(myDependentNodes, ndeps);
         N.setSize(ndeps);
         N.scale(1.0/N.sum()); // re-sum to one   
      }

      return changed;
   }
   
   @Override
   public void setNumber(int num) {
      super.setNumber(num);
   }


}
