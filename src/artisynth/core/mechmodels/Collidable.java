/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

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
      
   };
   
   CollisionData createCollisionData();
   public boolean isCollidable();
   public double getMass();
   
   public static Collidable Default = new DefaultCollidable("Default");
   public static Collidable RigidBody = new DefaultCollidable("RigidBody");
   public static Collidable Deformable = new DefaultCollidable("Deformable");
   public static Collidable Self = new DefaultCollidable("Self");
}
