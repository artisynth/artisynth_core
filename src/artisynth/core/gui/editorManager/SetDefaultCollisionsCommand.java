/**
 * Copyright (c) 2014, by the Authors: John Lloyd (UBC), Tracy Wilkinson (UBC) and
 * ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */package artisynth.core.gui.editorManager;

import java.util.HashMap;

import artisynth.core.mechmodels.Collidable.Group;
import artisynth.core.mechmodels.CollidablePair;
import artisynth.core.mechmodels.CollisionBehavior;
import artisynth.core.mechmodels.MechModel;

public class SetDefaultCollisionsCommand implements Command {
   private String myName;
   private MechModel myMechMod;
   private HashMap<CollidablePair,CollisionBehavior> myNewBehaviors;
   private HashMap<CollidablePair,CollisionBehavior> myOldBehaviors;

   public SetDefaultCollisionsCommand (
      String name,
      HashMap<CollidablePair,CollisionBehavior> behaviors, MechModel mech) {

      myName = name;
      myMechMod = mech;
      myNewBehaviors = new HashMap<CollidablePair,CollisionBehavior>();
      myNewBehaviors.putAll (behaviors);
      myOldBehaviors = new HashMap<CollidablePair,CollisionBehavior>();
   }

   public void execute() {

      for (CollidablePair pair : myNewBehaviors.keySet()) {
         Group g0 = (Group)pair.get(0);
         Group g1 = (Group)pair.get(1);
         CollisionBehavior oldBehav =
            myMechMod.getDefaultCollisionBehavior (g0, g1);
         myOldBehaviors.put (
            new CollidablePair (pair), new CollisionBehavior (oldBehav));
         CollisionBehavior newBehav = myNewBehaviors.get (pair);
         myMechMod.setDefaultCollisionBehavior (g0, g1, newBehav);
      }
   }

   public void undo() {

      for (CollidablePair pair : myOldBehaviors.keySet()) {
         CollisionBehavior oldBehav = myOldBehaviors.get (pair);
         myMechMod.setDefaultCollisionBehavior (
            (Group)pair.get(0), (Group)pair.get(1), oldBehav);
      }
   }

   public String getName() {
      return myName;
   }
}
