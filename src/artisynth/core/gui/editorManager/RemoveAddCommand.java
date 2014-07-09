/**
 * Copyright (c) 2014, by the Authors: John Lloyd (UBC), Tracy Wilkinson (UBC) and
 * ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */package artisynth.core.gui.editorManager;

import artisynth.core.modelbase.*;
import maspack.util.*;
import java.util.*;

public class RemoveAddCommand implements Command {
   private String myName;

   private LinkedList<ModelComponent> myRemoveList;
   private LinkedList<MutableCompositeComponent<?>> myRemoveParents;
   private int[] myRemoveIndices;

   private LinkedList<ModelComponent> myAddList;
   private LinkedList<MutableCompositeComponent<?>> myAddParents;

   public static RemoveAddCommand createRemoveCommand (
      String name,
      Collection<? extends ModelComponent> removeList) {
      return new RemoveAddCommand (
         name, removeList, null, (MutableCompositeComponent<?>)null);
   }

   public static RemoveAddCommand createRemoveCommand (
      String name,
      ModelComponent removeComp) {
      return new RemoveAddCommand (
         name, removeComp, null, null);
   }

   public static RemoveAddCommand createAddCommand (
      String name,
      Collection<? extends ModelComponent> addList,
      Collection<MutableCompositeComponent<?>> addParents) {
      
      return new RemoveAddCommand (
         name, null, addList, addParents);
   }

   public static RemoveAddCommand createAddCommand (
      String name,
      Collection<? extends ModelComponent> addList,
      MutableCompositeComponent<?> addParent) {
      
      return new RemoveAddCommand (
         name, null, addList, addParent);
   }

   public static RemoveAddCommand createRemoveAddCommand (
      String name,
      Collection<? extends ModelComponent> removeList,
      Collection<? extends ModelComponent> addList,
      Collection<MutableCompositeComponent<?>> addParents) {
      return new RemoveAddCommand (
         name, removeList, addList, addParents);
   }

   public static RemoveAddCommand createRemoveAddCommand (
      String name,
      Collection<? extends ModelComponent> removeList,
      Collection<? extends ModelComponent> addList,
      MutableCompositeComponent<?> addParent) {
      return new RemoveAddCommand (
         name, removeList, addList, addParent);
   }

   public static RemoveAddCommand createRemoveAddCommand (
      String name,
      ModelComponent removeComp,
      ModelComponent addComp,
      MutableCompositeComponent<?> addParent) {
      return new RemoveAddCommand (
         name, removeComp, addComp, addParent);
   }

   public RemoveAddCommand (
      String name,
      Collection<? extends ModelComponent> removeList, 
      Collection<? extends ModelComponent> addList, 
      Collection<MutableCompositeComponent<?>> addParents) {

      myName = name;
      if (removeList != null && removeList.size() != 0) {
         myRemoveList = new LinkedList<ModelComponent>();
         myRemoveList.addAll (removeList);
      }
      else {
         myRemoveList = null;
      }
      if (addList != null && addList.size() != 0) {
         if (addList.size() != addParents.size()) {
            throw new IllegalArgumentException (
               "add list size "+addList.size()+
               " != parent list size "+addParents.size());
         }
         myAddList = new LinkedList<ModelComponent>();
         myAddParents = new LinkedList<MutableCompositeComponent<?>>();
         myAddList.addAll (addList);
         myAddParents.addAll (addParents);
      }
      else {
         myAddList = null;
      }
   }

   public RemoveAddCommand (
      String name,
      Collection<? extends ModelComponent> removeList, 
      Collection<? extends ModelComponent> addList, 
      MutableCompositeComponent<?> addParent) {

      myName = name;
      if (removeList != null && removeList.size() != 0) {
         myRemoveList = new LinkedList<ModelComponent>();
         myRemoveList.addAll (removeList);
      }
      else {
         myRemoveList = null;
      }
      if (addList != null && addList.size() != 0) {
         myAddList = new LinkedList<ModelComponent>();
         myAddParents = new LinkedList<MutableCompositeComponent<?>>();
         for (ModelComponent c : addList) {
            myAddList.add (c);
            myAddParents.add (addParent);
         }
      }
      else {
         myAddList = null;
      }
   }

   public RemoveAddCommand (
      String name,
      ModelComponent removeComp, 
      ModelComponent addComp, 
      MutableCompositeComponent<?> addParent) {

      myName = name;
      if (removeComp != null) {
         myRemoveList = new LinkedList<ModelComponent>();
         myRemoveList.add (removeComp);
      }
      else {
         myRemoveList = null;
      }
      if (addComp != null) {
         myAddList = new LinkedList<ModelComponent>();
         myAddParents = new LinkedList<MutableCompositeComponent<?>>();
         myAddList.add (addComp);
         myAddParents.add (addParent);
      }
      else {
         myAddList = null;
      }
   }

   public void execute() {
      if (myRemoveList != null) {
         myRemoveIndices = new int[myRemoveList.size()];
         myRemoveParents =
            ComponentUtils.removeComponents (myRemoveList, myRemoveIndices);
      }
      if (myAddList != null) {
         ComponentUtils.addComponents (myAddList, null, myAddParents);
      }
   }

   public void undo() {
      if (myAddList != null) {
         ComponentUtils.removeComponents (myAddList, null);
      }
      if (myRemoveList != null) {
         ComponentUtils.addComponentsInReverse (
            myRemoveList, myRemoveIndices, myRemoveParents);
      }
   }

   public String getName() {
      return myName;
   }
}
