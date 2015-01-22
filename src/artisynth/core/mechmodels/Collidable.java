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
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.ModelComponentBase;

/**
 * Indicates a model component that can collide with other Collidables.
 */
public interface Collidable extends ModelComponent {

   class DefaultCollidable extends ModelComponentBase implements Collidable {
      
      DefaultCollidable (String name) {
         myName = name;
      }

      public String toString() {
         return myName;
      }
      
      public CollisionData createCollisionData() {
         return  null;
      }

      @Override
      public double getMass() {
         return 0;
      }
      
      @Override
      public boolean isCollidable () {
         return false;
      }
      
      @Override
      public PolygonalMesh getCollisionMesh() {
         return null;
      }

//      @Override
//      public void getContactMasters (
//         List<ContactMaster> mlist, double weight, ContactPoint cpnt) {
//      }

      @Override
      public void getVertexMasters (List<ContactMaster> mlist, Vertex3d vtx) {
      }
      
      @Override
      public boolean containsContactMaster (CollidableDynamicComponent comp) {
         return false;      
      }     

//      @Override
//      public boolean requiresContactVertexInfo() {
//         return false;
//      }
      
      @Override
      public boolean allowCollision (
         ContactPoint cpnt, Collidable other, Set<Vertex3d> attachedVertices) {
         return false;
      }
      
   };
   
   CollisionData createCollisionData();
   public boolean isCollidable();
   public double getMass();
   
   /**
    * Returns the mesh that should be used for computing collisions
    * 
    * @return collision mesh
    */
   public PolygonalMesh getCollisionMesh();

   /**
    * Returns all the contact master components associated with a particular
    * mesh vertex. Information for each contact master should be appended
    * to <code>mlist</code>. The list should not be cleared. The vertex
    * should be a vertex of the mesh returned by {@link getCollisionMesh}.
    * 
    * @param mlist collected master component information
    * @param vtx vertex for which the master components are requested
    */
   public void getVertexMasters (List<ContactMaster> mlist, Vertex3d vtx);
   
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
   
   public static Collidable Default = new DefaultCollidable("Default");
   public static Collidable RigidBody = new DefaultCollidable("RigidBody");
   public static Collidable Deformable = new DefaultCollidable("Deformable");
   public static Collidable Self = new DefaultCollidable("Self");
}
