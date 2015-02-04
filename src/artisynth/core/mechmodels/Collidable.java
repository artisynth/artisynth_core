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
 * Collidables can be arranged hierarchically, for grouping purposes and to
 * support self-intersection. Only leaf nodes of the hierarchy do the actual
 * collision handling, and these should implement the sub-class CollidableBody.
 */
public interface Collidable extends ModelComponent {

   class DefaultCollidable extends ModelComponentBase implements Collidable {
      
      DefaultCollidable (String name) {
         myName = name;
      }

      public String toString() {
         return myName;
      }
      
      @Override
      public boolean isCollidable () {
         return false;
      }
      
      @Override
      public boolean isDeformable() {
         return false;
      }
      
      @Override
      public boolean allowSelfIntersection (Collidable col) {
         return false;
      }
   };
   
   /**
    * Returns <code>true</code> if this collidable is actually allowed to
    * collide with other collidables. This can be predicated on several
    * factors, such as the setting of a <code>collidable</code> property or
    * whether or not this collidable currently possesses a valid collision
    * mesh.  If this method returns <code>false</code>, then no collisions will
    * be performed for this collidable, regardless any default or explicit
    * collision behaviors that have been arranged by the system.
    *
    * @return <code>true</code> if collisions are allowed for this
    * collidable.
    */
   public boolean isCollidable();

   /**
    * Returns <code>true</code> if this collidable is deformable. Whether or
    * not a collidable is deformable determines how it responds to default
    * collision behaviors involving deformable and rigid collidables. Also,
    * self-collisions among sub-collidables of a collidable A are permitted
    * only if A is deformable.
    *
    * @return <code>true</code> if this collidable is deformable
    */
   public boolean isDeformable();
   
   /**
    * Returns <code>true</code> if <code>col</code> is a sub-collidable of
    * this collidable which is allowed to collide with other sub-collidables.
    * For a collidable A, self-collision is only permitted 
    * among its sub-collidables for which this method returns <code>true</code>.
    * @param col collidable to be tested
    * @return <code>true</code> if <code>col</code> is allowed to collide
    * with other sub-collidables.
    */
   public boolean allowSelfIntersection (Collidable col);

   
   public static Collidable Default = new DefaultCollidable("Default");
   public static Collidable RigidBody = new DefaultCollidable("RigidBody");
   public static Collidable Deformable = new DefaultCollidable("Deformable");
   public static Collidable Self = new DefaultCollidable("Self");
}
