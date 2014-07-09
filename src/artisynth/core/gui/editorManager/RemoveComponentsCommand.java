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

public class RemoveComponentsCommand implements Command {
   private String myName;
   private LinkedList<ModelComponent> myDeleteComps;
   private LinkedList<ModelComponent> myUpdateComps;
   private LinkedList<Deque<Object>> myUpdateUndoInfo;
   private LinkedList<MutableCompositeComponent<?>> myParents;
   private int[] myIndices;

   public RemoveComponentsCommand (
      String name, List<? extends ModelComponent> delete) {
      this (name, delete, null);
   }

   public RemoveComponentsCommand (
      String name,
      List<? extends ModelComponent> delete, 
      List<? extends ModelComponent> update) {
      myName = name;
      myDeleteComps = new LinkedList<ModelComponent>();
      myDeleteComps.addAll (delete);
      if (update != null) {
         myUpdateComps = new LinkedList<ModelComponent>();
         myUpdateComps.addAll (update);
      }
      else {
         myUpdateComps = null;
      }
   }

   public RemoveComponentsCommand (String name, ModelComponent comp) {
      LinkedList<ModelComponent> list = new LinkedList<ModelComponent>();
      list.add (comp);
      myName = name;
      myDeleteComps = list;
      myUpdateComps = null;
   }

   protected boolean undoIsEmpty (Deque<Object> undoInfo) {
      for (Object obj : undoInfo) {
         if (obj != ModelComponentBase.NULL_OBJ) {
            return false;
         }
      }
      return true;      
   }

   public void execute() {
      myIndices = new int[myDeleteComps.size()];
      myParents = ComponentUtils.removeComponents (myDeleteComps, myIndices);
      if (myUpdateComps != null) {
         myUpdateUndoInfo = new LinkedList<Deque<Object>>();
         for (ModelComponent c : myUpdateComps) {
            Deque<Object> undoInfo = new ArrayDeque<Object>();
            c.updateReferences (/*undo=*/false, undoInfo);
            if (undoIsEmpty (undoInfo)) {
               undoInfo = null;
            }
            myUpdateUndoInfo.add (undoInfo);
         }
      }
   }

   public void undo() {
      ComponentUtils.addComponentsInReverse (myDeleteComps, myIndices, myParents);
      if (myUpdateComps != null) {
         for (ModelComponent c : myUpdateComps) {
            Deque<Object> undoInfo = myUpdateUndoInfo.removeFirst();
            if (undoInfo != null) {
               c.updateReferences(/*undo=*/true, undoInfo);
            }
         }
      }
   }

   public String getName() {
      return myName;
   }
}
