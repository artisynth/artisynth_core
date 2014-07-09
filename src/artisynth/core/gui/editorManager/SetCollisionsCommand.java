/**
 * Copyright (c) 2014, by the Authors: John Lloyd (UBC), Tracy Wilkinson (UBC) and
 * ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */package artisynth.core.gui.editorManager;

import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.CollisionComponent;
import artisynth.core.mechmodels.CollisionManager;
import artisynth.core.mechmodels.CollidablePair;
import artisynth.core.mechmodels.CollisionBehavior;
import artisynth.core.mechmodels.Collidable;
import maspack.util.*;
import java.util.*;

public class SetCollisionsCommand implements Command {
   private String myName;
   private MechModel myMech;
   private ArrayList<CollidablePair> myPairs;
   private CollisionBehavior myBehavior;
   private ArrayList<CollisionBehavior> mySavedBehaviors;
   private LinkedList<CollisionComponent> myAddedComponents;

   private void init (
      String name, MechModel mech,
      LinkedList<CollidablePair> pairs, CollisionBehavior behavior) {

      myName = name;
      myMech = mech;
      myPairs = new ArrayList<CollidablePair>(pairs.size());
      myBehavior = new CollisionBehavior (behavior);

      for (CollidablePair p : pairs) {
         myPairs.add (p);
      }
   }

   public SetCollisionsCommand (
      String name, MechModel mech, 
      LinkedList<CollidablePair> pairs, CollisionBehavior behavior) {

      init (name, mech, pairs, behavior);
   }

   public SetCollisionsCommand (
      String name, MechModel mech,
      CollidablePair pair, CollisionBehavior behavior) {

      LinkedList<CollidablePair> pairs = new LinkedList<CollidablePair>();
      pairs.add (pair);
      init (name, mech, pairs, behavior);
   }

   public void execute() {

      mySavedBehaviors = new ArrayList<CollisionBehavior>(myPairs.size());
      myAddedComponents = new LinkedList<CollisionComponent>();
      
      CollisionManager colmanager = myMech.getCollisionManager();
      for (int i=0; i<myPairs.size(); i++) {
         // either add a new component or set an existing one
         CollisionComponent comp = 
            colmanager.getCollisionOverride (myPairs.get(i));
         CollisionBehavior savedBehavior = null;
         if (comp == null) {
            myAddedComponents.add (
               new CollisionComponent (myPairs.get(i), myBehavior));
         }
         else {
            CollidablePair pair = myPairs.get(i);
            savedBehavior = new CollisionBehavior(comp.getBehavior());
            // use setCollisions instead of simply setting the component
            // so that we will get a structure changed event.
            myMech.setCollisionBehavior (pair.getA(), pair.getB(), myBehavior);
         }
         mySavedBehaviors.add (savedBehavior);
      }     
      if (myAddedComponents.size() > 0) {
         ComponentUtils.addComponents (
            myAddedComponents, null, colmanager.collisionComponents());
      }
   }

   public void undo() {
      if (myAddedComponents.size() > 0) {
         ComponentUtils.removeComponents (myAddedComponents, null);
      }
      for (int i=0; i<mySavedBehaviors.size(); i++) {
         if (mySavedBehaviors.get(i) != null) {
            CollidablePair pair = myPairs.get(i);
            myMech.setCollisionBehavior (
               pair.getA(), pair.getB(), mySavedBehaviors.get(i));
         }
      }
   }

   public String getName() {
      return myName;
   }
}
