/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.gui.selectionManager;

import java.util.LinkedList;

import artisynth.core.modelbase.ModelComponent;

public class SelectionEvent {
   protected ModelComponent myComponent;
   protected LinkedList<ModelComponent> myAdded;
   protected LinkedList<ModelComponent> myRemoved;

   public SelectionEvent (ModelComponent c) {
      myComponent = c;
   }

   public SelectionEvent (LinkedList<ModelComponent> added,
   LinkedList<ModelComponent> removed) {
      if (added != null) {
         myAdded = added;
      }
      else {
         myAdded = new LinkedList<ModelComponent>();
      }
      if (removed != null) {
         myRemoved = removed;
      }
      else {
         myRemoved = new LinkedList<ModelComponent>();
      }
   }

   public LinkedList<ModelComponent> getAddedComponents() {
      return myAdded;
   }

   public int numAddedComponents() {
      return myAdded.size();
   }

   public LinkedList<ModelComponent> getRemovedComponents() {
      return myRemoved;
   }

   public int numRemovedComponents() {
      return myRemoved.size();
   }

   public ModelComponent getLastAddedComponent() {
      if (myAdded.size() > 0) {
         return myAdded.getLast();
      }
      else {
         return null;
      }
   }

   public ModelComponent getComponent() {
      return myComponent;
   }
}
