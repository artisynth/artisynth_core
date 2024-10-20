/**
 * Copyright (c) 2014, by the Authors: John Lloyd (UBC), Tracy Wilkinson (UBC) and
 * ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */package artisynth.core.gui.editorManager;

import java.util.*;

public class EditActionMap {
   LinkedHashMap<String,ActionDesc> myMap;

   private class ActionDesc {
      int flags;
      EditorBase editor;
      String[] subactions;

      ActionDesc (EditorBase editor, int flags, Collection<String> subactions) {
         this.flags = flags;
         this.editor = editor;
         if (subactions != null) {
            this.subactions = subactions.toArray(new String[0]);
         }
      }
   };

   public EditActionMap() {
      myMap = new LinkedHashMap<String,ActionDesc>();
   }

   public void clear() {
      myMap.clear();
   }

   public void add (
      EditorBase editor, String name, int flags, 
      Collection<String> subactions) {
      if (myMap.get (name) != null) {
         System.out.printf (
            "Warning: editing action '%s' multiply defined\n", name);
      }
      ActionDesc desc = new ActionDesc (editor, flags, subactions);
      myMap.put (name, desc);
   }

   public void add (EditorBase editor, String name, int flags) {
      add (editor, name, flags, /*subactions*/null);
   }

   public void add (EditorBase editor, String name) {
      add (editor, name, /*flags*/0, /*subactions*/null);
   }

   public int size() {
      return myMap.size();
   }

   public String[] getActionNames() {
      return (String[])myMap.keySet().toArray (new String[0]);
   }

   public EditorBase getEditor (String name) {
      ActionDesc desc = myMap.get (name);
      if (desc != null) {
         return desc.editor;
      }
      else {
         return null;
      }
   }

   public int getFlags (String name) {
      ActionDesc desc = myMap.get (name);
      if (desc != null) {
         return desc.flags;
      }
      else {
         return 0;
      }
   }
   
   public String[] getSubActions (String name) {
      ActionDesc desc = myMap.get (name);
      if (desc != null) {
         return desc.subactions;
      }
      else {
         return null;
      }
   }
}