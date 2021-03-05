/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.util.List;
import java.util.Set;

import maspack.geometry.PolygonalMesh;
import maspack.geometry.Vertex3d;
import maspack.geometry.DistanceGrid;

/**
 * Indicates a Collidable that is capable of actual collision handling.  Within
 * a hierarchy of Collidable objects, the leaf nodes should be instances of
 * CollidableBody.
 */
public interface CollidableBody extends Collidable {

   /**
    * Returns the mass of the body.
    *
    * @return mass of the body
    */
   public double getMass();

   /**
    * Returns the mesh that should be used for computing collisions, or
    * <code>null</code> if there is no such mesh. If this method
    * returns <code>null</code>, then no collisions will
    * be performed for this collidable, regardless any default or explicit
    * collision behaviors that have been arranged by the system.
    * 
    * @return collision mesh for this collidable.
    */
   public PolygonalMesh getCollisionMesh();

   /**
    * Collects the contact masters for a particular mesh vertex. The
    * masters should be appended to <code>mlist</code>.  The vertex
    * should be a vertex of the mesh returned by {@link #getCollisionMesh()}.
    * 
    * @param mlist collected master components
    * @param vtx vertex for which the master components are requested
    */
   public void collectVertexMasters (List<ContactMaster> mlist, Vertex3d vtx);
   
   /**
    * Returns true if this Collidable contains a specified contact master
    * component.
    * 
    * @param comp component to test for
    * @return <code>true</code> if <code>comp</code> is contained in
    * this Collidable
    */
   public boolean containsContactMaster (CollidableDynamicComponent comp);
   
   //public boolean requiresContactVertexInfo();
   
   /**
    * Returns <code>true</code> if a collision between this Collidable
    * and <code>other</code> should be allowed for the contact point
    * <code>cpnt</code>. In making this decision, this method may
    * refer to <code>attachedVertices</code>, which supplies a list
    * of vertices on this Collidable which are <i>attached</i> in some way 
    * to the other Collidable.
    * 
    * @param cpnt contact point being tested
    * @param other opposing collidable
    * @param attachedVertices list of vertices attached to <code>other</code>.
    * @return <code>true</code> if the collision should be allowed
    */
   public boolean allowCollision (
      ContactPoint cpnt, Collidable other, Set<Vertex3d> attachedVertices);
   
   public int getCollidableIndex();
   
   public void setCollidableIndex (int idx);
   
   /**
    * Returns <code>true</code> if this Collidable supports a signed
    * distance grid that can be used with a SignedDistanceCollider. At
    * present, this will only be true for non-deformable bodies.
    * 
    * @return <code>true</code> if this Collidable supports a signed
    * distance grid
    */
   public boolean hasDistanceGrid();
   
   /**
    * Returns a {@link DistanceGridComp} object that in turn contains
    * a signed distance grid that can be used with a SignedDistanceCollider, 
    * or <code>null</code> if this Collidable
    * does not support a signed distance grid (i.e., if 
    * {@link #hasDistanceGrid} returns <code>false</code>).
    */
   public DistanceGridComp getDistanceGridComp();

}
