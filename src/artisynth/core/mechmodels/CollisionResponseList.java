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

public class CollisionResponseList extends ComponentList<CollisionResponse> {
   
   protected static final long serialVersionUID = 1;
   HashMap<CollidablePair,CollisionResponse> myMap;

   public CollisionResponseList () {
      this (null, null);
   }
   
   public CollisionResponseList (String name, String shortName) {
      super (CollisionResponse.class, name, shortName);
      myMap = new HashMap<CollidablePair,CollisionResponse>();
   }

   public boolean hasParameterizedType() {
      return false;
   }

   @Override 
   public boolean add (CollisionResponse behav) {
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
         if (c instanceof CollisionResponse) {
            CollisionResponse behav = (CollisionResponse)c;
            if (behav.getCollidablePair() != null) {
               myMap.put (behav.getCollidablePair(), behav);
            }
         }
      }
   }

   @Override 
   public boolean remove (Object comp) {
      if (comp instanceof CollisionResponse) {
         CollisionResponse behav = (CollisionResponse)comp;
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
   public CollisionResponse remove (int idx) {
      CollisionResponse behav = super.remove (idx);
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
         if (c instanceof CollisionResponse) {
            CollisionResponse behav = (CollisionResponse)c;
            if (behav.getCollidablePair() != null) {
               myMap.remove (behav.getCollidablePair());
            }
         }
      }
   }

   public CollisionResponse get (CollidablePair pair) {
      return myMap.get (pair);
   }
   
   @Override 
   public void postscan (
      Deque<ScanToken> tokens, CompositeComponent ancestor) 
      throws IOException {
      
      super.postscan (tokens, ancestor);
      myMap.clear();
      for (CollisionResponse behav : this) {
         myMap.put (behav.getCollidablePair(), behav);
      }
   }

   @Override
   public void removeAll() {
      super.removeAll();
      myMap.clear();
   }
}
