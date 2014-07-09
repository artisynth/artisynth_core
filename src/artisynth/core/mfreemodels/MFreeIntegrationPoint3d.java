/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mfreemodels;

import java.util.ArrayList;
import java.util.List;

import maspack.matrix.Matrix3d;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import artisynth.core.femmodels.IntegrationPoint3d;
import artisynth.core.mechmodels.PointState;

public class MFreeIntegrationPoint3d extends IntegrationPoint3d implements MFreePoint3d {

   ArrayList<MFreeNode3d> myDependentNodes;
   PointState myState;
   Point3d myRestPosition;
   int myID;
   
   public MFreeIntegrationPoint3d() {
      super(0);
      myState = new PointState();
      myRestPosition = new Point3d();
      myID = -1;
      setPressureWeights(new VectorNd(new double[]{1}));  // default pressure weights?
   }
   
   public int getID() {
      return myID;
   }
   
   public void setID(int num) {
      myID = num;
   }
   
   public MFreeIntegrationPoint3d(Point3d pos, List<MFreeNode3d> deps) {
      this(deps,new VectorNd(deps.size()));
   }
   
   public MFreeIntegrationPoint3d(List<MFreeNode3d> deps, VectorNd coords) {
      this();
      setDependentNodes(deps,coords);
   }

   public ArrayList<MFreeNode3d> getDependentNodes() {
      return myDependentNodes;
   }

   public void setDependentNodes(List<MFreeNode3d> nodes, VectorNd coords) {
      myDependentNodes = new ArrayList<MFreeNode3d>();
      myDependentNodes.addAll(nodes);
      super.init(myDependentNodes.size(), 1);
      setNodeCoordinates(coords);
      updatePosAndVelState();
   }

   public Point3d getPosition() {
      return myState.getPos();
   }
   
   public Point3d getRestPosition() {
      return myRestPosition;
   }
   
   public VectorNd getNodeCoordinates() {
      return getShapeWeights();
   }
   
   public int getNodeCoordIdx(MFreeNode3d node) {
      return myDependentNodes.indexOf(node);
   }
   
   public double getShapeCoordinate(MFreeNode3d node) {
      int idx = getNodeCoordIdx(node);
      if (idx < 0) {
         return 0;
      }
      return getShapeWeights().get(idx);
   }
   
   @Override 
   public Vector3d getCoords() {
      // meaningless
      return null;
   }

   public void setNodeCoordinates(VectorNd coords) {
      
      setShapeWeights(coords);
      updateRestPosition();
      updatePosState();
      updateVelState();
   }

   public void updatePosState() {
      myState.setPos(Point3d.ZERO);
      for (int i=0; i<myDependentNodes.size(); i++) {
         myState.scaledAddPos(N.get(i),myDependentNodes.get(i).getFalsePosition());
      }
   }

   public void updateVelState() {
      myState.setVel(Vector3d.ZERO);
      for (int i=0; i<myDependentNodes.size(); i++) {
         myState.scaledAddVel(N.get(i),myDependentNodes.get(i).getFalseVelocity());
      }
   }
   
   public void updatePosAndVelState() {
      updatePosState();
      updateVelState();
   }
   
   /** 
    * Create an integration point for a given element.
    *
    * @param elem element to create the integration point for
    * @param s0 first coordinate value
    * @param s1 second coordinate value
    * @param s2 third coordinate value
    * @param w weight 
    * @return new integration point
    */
   public static MFreeIntegrationPoint3d create (List<MFreeNode3d> dependentNodes, VectorNd shapeN, ArrayList<Vector3d> shapeGrad, double w) {
      
      int nnodes = dependentNodes.size();
      
      MFreeIntegrationPoint3d ipnt = new MFreeIntegrationPoint3d(dependentNodes, shapeN);
      ipnt.setWeight(w);
      
      for (int i=0; i<nnodes; i++) {
         ipnt.setShapeGrad (i, shapeGrad.get(i));
      }
      return ipnt;
   }
   
   public void computeJacobian () {
      myJ.setZero();
      for (int i=0; i<myDependentNodes.size(); i++) {
         Point3d pos = myDependentNodes.get(i).getFalsePosition();
         Vector3d dNds = GNs[i];
         myJ.addOuterProduct (pos.x, pos.y, pos.z, 
            dNds.x, dNds.y, dNds.z);
      }
      
   }
   
   public void computeJacobianAndGradient (Matrix3d invJ0) {
      
      myJ.setZero();
      for (int i=0; i<myDependentNodes.size(); i++) {
         Vector3d pos = myDependentNodes.get(i).getFalsePosition();
         Vector3d dNds = GNs[i];
         myJ.addOuterProduct (pos.x, pos.y, pos.z, dNds.x, dNds.y, dNds.z);
      }
      
      if (invJ0 != null) {
         F.mul (myJ, invJ0);
      } else {
         F.set(myJ);
      }      
      detF = F.determinant();
   }
   
   public void computeJacobianAndGradient() {
      computeJacobianAndGradient(null);
   }

   public void computeGradientForRender (Matrix3d Fmat, 
      Matrix3d invJ0) {

      // compute J in Fmat
      Fmat.setZero();
      for (int i=0; i<myDependentNodes.size(); i++) {
         Point3d pos = myDependentNodes.get(i).getFalsePosition();
         Vector3d dNds = GNs[i];
         Fmat.addOuterProduct (pos.x,pos.y,pos.z, dNds.x, dNds.y, dNds.z);
      }      
      if (invJ0 != null) {
         Fmat.mul (invJ0);
      }
   }
   
   public void computeGradientForRender (Matrix3d Fmat) {
      computeGradientForRender(Fmat, null);
   }
   
   public void updateRestPosition() {
      myRestPosition.setZero();
      for (int i=0; i<myDependentNodes.size(); i++) {
         myRestPosition.scaledAdd(N.get(i), myDependentNodes.get(i).getRestPosition());
      }
   }
   
   public void computePosition (Point3d pos) {
      double[] Nbuf = N.getBuffer();
      for (int i=0; i<myDependentNodes.size(); i++) {
         pos.scaledAdd (Nbuf[i], myDependentNodes.get(i).getFalsePosition());
      }
   }

   public double getDetJ() {
      return myJ.determinant();
   }
   
   public boolean reduceDependencies(double tol) {
      
      boolean changed = false;
      for (int i=0; i<myDependentNodes.size(); i++) {
         if (Math.abs(N.get(i)) <= tol) {
            changed = true;
            N.set(i, 0);
         }
      }   
      N.scale(1.0/N.sum()); // re-sum to one
      return changed;
   }
   
   
}
