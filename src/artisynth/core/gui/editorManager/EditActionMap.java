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

      ActionDesc (EditorBase editor, int flags) {
         this.flags = flags;
         this.editor = editor;
      }
   };

   public EditActionMap() {
      myMap = new LinkedHashMap<String,ActionDesc>();
   }

   public void clear() {
      myMap.clear();
   }

   public void add (EditorBase editor, String name, int flags) {
      if (myMap.get (name) != null) {
         System.out.printf (
            "Warning: editing action '%s' multiply defined\n", name);
      }
      ActionDesc desc = new ActionDesc (editor, flags);
      myMap.put (name, desc);
   }

   public void add (EditorBase editor, String name) {
      if (myMap.get (name) != null) {
         System.out.printf (
            "Warning: editing action '%s' multiply defined\n", name);
      }
      ActionDesc desc = new ActionDesc (editor, 0);
      myMap.put (name, desc);
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
}