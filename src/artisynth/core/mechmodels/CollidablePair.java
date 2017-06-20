/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.CompositeComponentBase;

/**
 * Describes a pair of Collidable model components.
 */
public class CollidablePair {
   Collidable myComp0;
   Collidable myComp1;
   
   public CollidablePair (Collidable a, Collidable b) {
      myComp0 = a;
      myComp1 = b;
   }

   public CollidablePair (CollidablePair pair) {
      myComp0 = pair.myComp0;
      myComp1 = pair.myComp1;
   }

   public Collidable get (int cidx) {
      if (cidx == 0) {
         return myComp0;
      }
      else if (cidx == 1) {
         return myComp1;
      }
      else {
         throw new IllegalArgumentException (
            "collidable index must be 0 or 1");
      }
   }

   public Collidable getOther (Collidable c) {
      if (myComp0 == c) {
         return myComp1;
      }
      else if (myComp1 == c) {
         return myComp0;
      }
      else {
         return null;
      }
   }
   
   public boolean isExplicit() {
      return myComp0.getParent() != null && myComp1.getParent() != null;
   }
           
   public boolean equals (Object obj) {
      if (!(obj instanceof CollidablePair)){
         return false;
      }
      CollidablePair crp = (CollidablePair)obj;
      return crp.includesCollidables (myComp0, myComp1);
   }
   
   public boolean includesCollidables(Collidable a, Collidable b) {
     return ((myComp0 == a && myComp1 == b) || 
             (myComp0 == b && myComp1 == a) );
   }

   public int hashCode() {
      return (myComp0.hashCode() ^ myComp1.hashCode() );
   }

//   public static boolean isGeneric (Collidable c) {
//      return (c instanceof Collidable.DefaultCollidable);
//   }
//
//   public boolean isValidDefault() {
//      if (myCompA instanceof Collidable.Group &&
//          myCompB instanceof Collidable.Group) {
//         if (myCompA == Collidable.Self) {
//            if (myCompB == Collidable.Rigid || myCompB == Collidable.Self) {
//               return false;
//            }
//         }
//         else if (myCompB == Collidable.Self) {
//            if (myCompA == Collidable.Rigid) {
//               return false;
//            }
//         }
//         return true;
//      }
//      else {
//         return false;
//      }
//   }
   
   public String toString (CompositeComponent ref, Collidable c) {
      if (c instanceof Collidable.Group) {
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
      String out = toString(ref, myComp0);
      out += "-" + toString(ref, myComp1);
      return out; 
   }

   public String toString () {
      return toString (null);
   }

   /**
    * Create a name for this CollidablePair that is suitable to use as a
    * component name.
    */
   public String createComponentName (CompositeComponent ref) {
      String str = toString (ref);
      // substitute # for / since slashes are not allowed in component names
      return CompositeComponentBase.makeValidName(str.replace ('/', '#'));      
   }

}
