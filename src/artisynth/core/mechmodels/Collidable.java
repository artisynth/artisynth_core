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

   /**
    * Indicates the collision types that are enabled for this Collidable.
    * These include internal (self) collisions (involving the sub-collidables
    * of a common ancestor) and external collisions (among collidables that do
    * not share a common ancestor).
    */
   public enum Collidability {

      /**
       * All collisions disabled: the Collidable will not collide with
       * anything.
       */
      OFF,

      /**
       * Internal (self) collisions enabled: the Collidable may only collide
       * with other Collidables with which it shares a common ancestor.
       */
      INTERNAL,

      /**
       * External collisions enabled: the Collidable may only collide with
       * other Collidables with which it does <i>not</i>share a common
       * ancestor.
       */
      EXTERNAL,

      /**
       * All collisions (both self and external) enabled: the Collidable may
       * collide with any other Collidable.
       */
      ALL      
   }

   /**
    * Special Collidable subclass used to denote generic groups of
    * Collidables.
    */
   public class Group extends ModelComponentBase 
      implements Collidable {

      boolean myIncludesDeformable = false;
      boolean myIncludesRigid = false;
      boolean myIncludesSelf = false;

      private Group (
         String name,
         boolean deformable, boolean rigid, boolean self) {
         myName = name;
         myIncludesDeformable = deformable;
         myIncludesRigid = rigid;
         myIncludesSelf = self;
      }

      public String toString() {
         return myName;
      }
      
      @Override      
      public Collidability getCollidable() {
         return Collidability.OFF;   
      }
      
      @Override
      public Collidable getCollidableAncestor() {
         return null;
      }
      
      @Override
      public boolean isCompound() {
         return false;
      }

      public boolean includesSelf() {
         return myIncludesSelf;
      }
      
      public boolean includesRigid() {
         return myIncludesRigid;
      }
      
      public boolean includesDeformable() {
         return myIncludesDeformable;
      }
      
      public boolean isSelfInstance (Collidable c) {
         if (c.isDeformable() && c.isCompound()) {
            return myIncludesSelf;
         }
         else {
            return false;
         }
      }
      
      public boolean isBodyInstance (Collidable c) {
         if (c.isDeformable()) {
            return myIncludesDeformable;
         }
         else {
            return myIncludesRigid;
         }
      }

      @Override
      public boolean isDeformable() {
         // XXX is this right?
         return myIncludesDeformable;
      }
   };
   
   /**
    * Returns the {@link Collidability} of this Collidable. This provides
    * control over whether external and/or internal collisions are enabled for
    * this Collidable. This setting takes precedence over default and
    * explicitly requested collision behaviors.
    *
    * <p>Note that for collisions to actually occur, they still need to be
    * enabled through either a default or explicit collision behavior in the
    * MechModel.
    *
    * @return Collidability of this collidable.
    */
   public Collidability getCollidable();
   
   /**
    * Returns the most immediate Collidable ancestor of this Collidable,
    * if any. Otherwise, return <code>null</code>.
    * 
    * @return immediate Collidable ancestor, or <code>null</code>.
    */
   public Collidable getCollidableAncestor();

   /**
    * Queries whether or not this collidable has sub-collidables.
    *
    * @return <code>true</code> if this collidable has sub-collidables
    */
   public boolean isCompound();
   
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
    * Group specifying self-collision
    */  
   public static Collidable.Group Self =
      new Group("Self", false, false, true);

   /**
    * Group specifying all rigid collidable bodies
    */
   public static Collidable.Group Rigid =
      new Group("Rigid", false, true, false);
   
   /**
    * Group specifying all deformable collidable bodies
    */
   public static Collidable.Group Deformable =
      new Group("Deformable", true, false, false);
   
   /**
    * Group specifying all rigid and deformable collidable bodies
    */
   public static Collidable.Group AllBodies =
      new Group("AllBodies", true, true, false);

   /**
    * Group specifying all collidable bodies and self-collision
    */
   public static Collidable.Group All =
      new Group("All", true, true, true);
}
