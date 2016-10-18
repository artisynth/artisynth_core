/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.util.*;
import java.io.*;

import artisynth.core.modelbase.*;
import artisynth.core.util.*;

public class CollisionBehaviorList extends ComponentList<CollisionBehavior> {
   
   protected static final long serialVersionUID = 1;
   HashMap<CollidablePair,CollisionBehavior> myMap;

   public CollisionBehaviorList () {
      this (null, null);
   }
   
   public CollisionBehaviorList (String name, String shortName) {
      super (CollisionBehavior.class, name, shortName);
      myMap = new HashMap<CollidablePair,CollisionBehavior>();
   }

   public boolean hasParameterizedType() {
      return false;
   }

   @Override 
   public boolean add (CollisionBehavior behav) {
      if (super.add (behav)) {
         if (behav.getCollidablePair() != null) {
            myMap.put (behav.getCollidablePair(), behav);
         }
         return true;
      }
      else {
         return false;
      }
   }

   @Override 
   public void addComponents (ModelComponent[] comps, int[] indices, int num) {
      super.addComponents (comps, indices, num);
      for (ModelComponent c : comps) {
         if (c instanceof CollisionBehavior) {
            CollisionBehavior behav = (CollisionBehavior)c;
            if (behav.getCollidablePair() != null) {
               myMap.put (behav.getCollidablePair(), behav);
            }
         }
      }
   }

   @Override 
   public boolean remove (Object comp) {
      if (comp instanceof CollisionBehavior) {
         CollisionBehavior behav = (CollisionBehavior)comp;
         if (super.remove (behav)) {
            if (behav.getCollidablePair() != null) {
               myMap.remove (behav.getCollidablePair());
            }
            return true;
         }
      }
      return false;
   }

   @Override 
   public CollisionBehavior remove (int idx) {
      CollisionBehavior behav = super.remove (idx);
      if (behav.getCollidablePair() != null) {
         myMap.remove (behav.getCollidablePair());
      }
      return behav;
   }

   @Override 
   public void removeComponents (
      ModelComponent[] comps, int[] indices, int num) {
      super.removeComponents (comps, indices, num);
      for (ModelComponent c : comps) {
         if (c instanceof CollisionBehavior) {
            CollisionBehavior behav = (CollisionBehavior)c;
            if (behav.getCollidablePair() != null) {
               myMap.remove (behav.getCollidablePair());
            }
         }
      }
   }

   public CollisionBehavior get (CollidablePair pair) {
      return myMap.get (pair);
   }
   
   @Override 
   public void postscan (
      Deque<ScanToken> tokens, CompositeComponent ancestor) 
      throws IOException {
      
      super.postscan (tokens, ancestor);
      myMap.clear();
      for (CollisionBehavior behav : this) {
         myMap.put (behav.getCollidablePair(), behav);
      }
   }

   @Override
   public void removeAll() {
      super.removeAll();
      myMap.clear();
   }
}

