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

public class AddComponentsCommand implements Command {
   private String myName;
   private LinkedList<ModelComponent> myComponents;
   private LinkedList<MutableCompositeComponent<?>> myParents;

   public AddComponentsCommand (
      String name, LinkedList<ModelComponent> comps, 
      LinkedList<MutableCompositeComponent<?>> parents) {
      myName = name;
      myComponents = comps;
      myParents = parents;
   }

   public AddComponentsCommand (
      String name, LinkedList<? extends ModelComponent> comps, 
      MutableCompositeComponent<?> parent) {
      myName = name;
      myComponents = new LinkedList<ModelComponent>();
      myParents = new LinkedList<MutableCompositeComponent<?>>();
      for (ModelComponent c : comps) {
         myComponents.add (c);
         myParents.add (parent);
      }
   }

   public AddComponentsCommand (
      String name, ModelComponent comp, MutableCompositeComponent<?> parent) {
      myName = name;
      myComponents = new LinkedList<ModelComponent>();
      myComponents.add (comp);
      myParents = new LinkedList<MutableCompositeComponent<?>>();
      myParents.add (parent);
   }

   public void execute() {
      ComponentUtils.addComponents (myComponents, null, myParents);
   }

   public void undo() {
      ComponentUtils.removeComponents (myComponents, null);
   }

   public String getName() {
      return myName;
   }

   /**
    * Returns the components added by this command.
    *
    * @return added components (read-only)
    */
   public LinkedList<ModelComponent> getComponents() {
      return myComponents;
   }
}
