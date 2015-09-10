/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CompositeComponent;

/**
 * Describes a pair of Collidable model components.
 */
public class CollidablePair {
   Collidable myCompA;
   Collidable myCompB;
   
   public CollidablePair (Collidable a, Collidable b) {
      myCompA = a;
      myCompB = b;
   }

   public CollidablePair (CollidablePair pair) {
      myCompA = pair.myCompA;
      myCompB = pair.myCompB;
   }

   public Collidable getA() {
      return myCompA;
   }
        
   public Collidable getB() {
      return myCompB;
   }
   
   public boolean isExplicit() {
      return !isGeneric(myCompA) && !isGeneric(myCompB);
   }
           
   public boolean equals (Object obj) {
      if (!(obj instanceof CollidablePair)){
         return false;
      }
      CollidablePair crp = (CollidablePair)obj;
      return crp.includesCollidables (myCompA, myCompB);
   }
   
   public boolean includesCollidables(Collidable a, Collidable b) {
     return ((myCompA == a && myCompB == b) || 
             (myCompA == b && myCompB == a) );
   }

   public int hashCode() {
      return (myCompA.hashCode() ^ myCompB.hashCode() );
   }

   public static boolean isGeneric (Collidable c) {
      return (c == Collidable.RigidBody || 
              c == Collidable.Deformable ||
              c == Collidable.Default);
   }

   public boolean isValidDefault() {
      if (isGeneric(myCompA) && isGeneric(myCompB)) {
         return true;
      }
      else if ((myCompA==Collidable.Self && myCompB==Collidable.Deformable) ||
               (myCompB==Collidable.Self && myCompA==Collidable.Deformable)) {
         return true;
      }
      else {
         return false;
      }
   }

   public String toString (CompositeComponent ref, Collidable c) {
      if (isGeneric (c)) {
         return c.toString();
      }
      else if (ref != null) {
         return ComponentUtils.getPathName (ref, c);
      }
      else {
         return ComponentUtils.getPathName (c);
      }
   }

   public String toString (CompositeComponent ref) {
      String out = toString(ref, getA());
      out += "-" + toString(ref, getB());
      return out; 
   }

   public String toString () {
      return toString (null);
   }

}
