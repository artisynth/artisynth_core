/**
 * Copyright (c) 2014, by the Authors: John Lloyd (UBC), Tracy Wilkinson (UBC) and
 * ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */package artisynth.core.gui.editorManager;

import artisynth.core.modelbase.*;
import artisynth.core.femmodels.*;
import artisynth.core.mechmodels.*;
import maspack.util.*;
import java.util.*;

public class MoveFibresCommand implements Command {
   private String myName;
   private LinkedList<Muscle> myFibres;
   private LinkedList<MutableCompositeComponent<?>> myParents;
   private MuscleBundle myBundle;
   private int[] myIndices;

   public MoveFibresCommand (String name, MuscleBundle bundle,
   LinkedList<Muscle> fibres) {
      myName = name;
      myBundle = bundle;
      myFibres = fibres;
   }

   public void execute() {
      myIndices = new int[myFibres.size()];
      myParents = ComponentUtils.removeComponents (myFibres, myIndices);

      // expand myBundle into a list giving the parent for each
      // fibre to be added
      LinkedList<MutableCompositeComponent<?>> myBundleParent =
         new LinkedList<MutableCompositeComponent<?>>();
      for (int i = 0; i < myFibres.size(); i++) {
         myBundleParent.add (myBundle.getFibres());
      }
      ComponentUtils.addComponents (myFibres, null, myBundleParent);
   }

   public void undo() {
      ComponentUtils.removeComponents (myFibres, null);
      ComponentUtils.addComponentsInReverse (myFibres, myIndices, myParents);
   }

   public String getName() {
      return myName;
   }
}
