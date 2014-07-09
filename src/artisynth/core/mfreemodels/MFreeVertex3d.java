/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mfreemodels;

import java.util.ArrayList;
import java.util.List;

import maspack.geometry.Vertex3d;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import artisynth.core.mechmodels.PointState;

public class MFreeVertex3d extends Vertex3d implements MFreePoint3d {

   public static double DEFAULT_COORDINATE_TOLERANCE = 1e-8;
   
   ArrayList<MFreeNode3d> myDependentNodes;
   VectorNd myNodeCoords;
   PointState myState;
   private Point3d myRestPosition;
   
   public MFreeVertex3d(ArrayList<MFreeNode3d> dependentNodes, VectorNd coords) {
      myState = new PointState();
      myRestPosition = new Point3d();
      setDependentNodes(dependentNodes, coords);
      setPosition(myRestPosition);
   }
  
   public ArrayList<MFreeNode3d> getDependentNodes() {
      return myDependentNodes;
   }

   public void setDependentNodes(List<MFreeNode3d> nodes, VectorNd coords) {
      myDependentNodes = new ArrayList<MFreeNode3d>(nodes.size());
      myDependentNodes.addAll(nodes);
      myNodeCoords = new VectorNd(coords);
      reduceDependencies(DEFAULT_COORDINATE_TOLERANCE);
      
      updateRestPosition();
   }
   
   public void updateRestPosition() {
      myRestPosition.setZero();
      for (int i=0; i<myDependentNodes.size(); i++) {
         myRestPosition.scaledAdd(myNodeCoords.get(i), myDependentNodes.get(i).getRestPosition());
      }
   }
   
   public Point3d getRestPosition() {
      return myRestPosition;
   }

   public VectorNd getNodeCoordinates() {
      return myNodeCoords;
   }

   public void setNodeCoordinates(VectorNd coords) {
      myNodeCoords.set(coords);
      updateRestPosition();
   }

   public void updatePosState() {
      myState.setPos(Point3d.ZERO);
      for (int i=0; i<myDependentNodes.size(); i++) {
         myState.scaledAddPos(myNodeCoords.get(i), myDependentNodes.get(i).getFalsePosition());
      }
      pnt.set(myState.getPos());
   }

   public void updateVelState() {
      myState.setVel(Vector3d.ZERO);
      for (int i=0; i<myDependentNodes.size(); i++) {
         myState.scaledAddVel(myNodeCoords.get(i), myDependentNodes.get(i).getFalseVelocity());
      }      
   }
   
   public void updatePosAndVelState() {
      updatePosState();
      updateVelState();
   }
   
   @Override
   public MFreeVertex3d clone() {
      MFreeVertex3d vtx = (MFreeVertex3d)super.clone();
      
      vtx.myDependentNodes = new ArrayList<MFreeNode3d>(myDependentNodes);
      vtx.myNodeCoords = new VectorNd(myNodeCoords);
      vtx.updateRestPosition();

      return vtx;
   }
   
   public boolean reduceDependencies(double tol) {
      ArrayList<MFreeNode3d> oldNodes = myDependentNodes;
      VectorNd oldWeights = myNodeCoords;
            
      boolean changed = false;
      int nNodes=0;
      for (int i=0; i<oldNodes.size(); i++) {
         if (Math.abs(oldWeights.get(i)) > tol) {
            nNodes++;
         } else {
            changed = true;
         }
      }
      
      if (!changed) {
         return false;
      }
      
      myDependentNodes = new ArrayList<MFreeNode3d>(nNodes);
      myNodeCoords = new VectorNd(nNodes);
      
      int idx = 0;
      for (int i=0; i<oldNodes.size(); i++) {
         if (Math.abs(oldWeights.get(i)) > tol) {
            myDependentNodes.add(oldNodes.get(i));
            myNodeCoords.set(idx++, oldWeights.get(i));
         }
      }
      
      myNodeCoords.scale(1.0/myNodeCoords.sum()); // re-sum to one
      
      return true;
      
   }
   
}
