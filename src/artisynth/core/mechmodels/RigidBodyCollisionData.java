/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import maspack.geometry.PolygonalMesh;
import maspack.geometry.Vertex3d;
import maspack.matrix.Point3d;

public class RigidBodyCollisionData implements CollisionData {

   PolygonalMesh myMesh;
   RigidBody myBody;
   Point3d _loc;

   public RigidBodyCollisionData(RigidBody body, PolygonalMesh mesh) {
      myBody = body;
      myMesh = mesh;
   }
   
   public RigidBodyCollisionData(RigidCompositeBody body, int meshIdx) {
      myBody = body;
      if (meshIdx >= 0) {
         myMesh = (PolygonalMesh)body.getCollisionMesh(meshIdx);
      } else {
         myMesh = (PolygonalMesh)body.getMesh();
      }
   }

   @Override
   public PolygonalMesh getMesh() {
      return myMesh;
   }

   @Override
   public RigidBody getComponent() {
      return myBody;
   }
   
   public RigidBody getBody() {
      return myBody;
   }

   @Override
   public int addConstraintInfo(Vertex3d vtx, Point3d constraintLoc,
      double weight, DeformableContactConstraint con) {
      
      _loc.inverseTransform(myBody.getPose(), constraintLoc);
      con.addFrame(this.getBody(), weight, _loc);
      return 1;
   }

   @Override
   public void markMasters(Vertex3d vtx, boolean marked) {
      myBody.setMarked(marked);
   }

   @Override
   public int numActiveUnmarkedMasters(Vertex3d vtx) {
      if (myBody.isActive() && !myBody.isMarked()) {
         return 1;
      }
      return 0;
   }

}
