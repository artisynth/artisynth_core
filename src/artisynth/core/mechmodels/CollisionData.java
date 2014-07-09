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

public interface CollisionData {

   /**
    * Gets the PolygonalMesh used for collisions
    * @return mesh used for collisions
    */
   PolygonalMesh getMesh();

   /**
    * Returns the component involved in the collision
    */
   Collidable getComponent();
   
   /**
    * Adds the point information from <code>this</code> object's vertex and weight to the
    * supplied constraint.
    * @param vtx vertex belonging to <code>this</code> object
    * @param constraintLoc actual world location of constraint
    * @param weight weight used in constraint
    * @param con the constraint to modify 
    * @return the number of constraints added or modified
    */
   public int addConstraintInfo(Vertex3d vtx, Point3d constraintLoc,
      double weight, DeformableContactConstraint con);
   
   /**
    * Mark the master dynamic components of vertex {@code vtx}
    * as being "used"
    */
   public void markMasters(Vertex3d vtx, boolean marked);
   
   /**
    * Count the number of "free" master components for vertex {@code vtx}.
    * Used for determining whether or not a collision should be allowed.
    * @param vtx
    */
   public int numActiveUnmarkedMasters(Vertex3d vtx);
   
}
