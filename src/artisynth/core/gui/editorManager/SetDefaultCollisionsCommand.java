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
import artisynth.core.mechmodels.CollidablePair;
import artisynth.core.mechmodels.CollisionBehavior;
import artisynth.core.mechmodels.Collidable;
import maspack.util.*;
import java.util.*;

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
         CollisionBehavior oldBehav =
            myMechMod.getDefaultCollisionBehavior (pair.getA(), pair.getB());
         myOldBehaviors.put (
            new CollidablePair (pair), new CollisionBehavior (oldBehav));
         CollisionBehavior newBehav = myNewBehaviors.get (pair);
         myMechMod.setDefaultCollisionBehavior (
            pair.getA(), pair.getB(), newBehav);
      }
   }

   public void undo() {

      for (CollidablePair pair : myOldBehaviors.keySet()) {
         CollisionBehavior oldBehav = myOldBehaviors.get (pair);
         myMechMod.setDefaultCollisionBehavior (
            pair.getA(), pair.getB(), oldBehav);
      }
   }

   public String getName() {
      return myName;
   }
}
