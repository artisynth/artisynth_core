/**
 * Copyright (c) 2014, by the Authors: John Lloyd (UBC), Tracy Wilkinson (UBC) and
 * ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */package artisynth.core.gui.editorManager;

import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.*;
import maspack.util.*;
import java.util.*;

public class DetachParticlesCommand implements Command {
   private String myName;
   private LinkedList<Particle> myParticles;
   private DynamicComponent[] myMasters;
   private MechModel myMechModel;

   public DetachParticlesCommand (
      String name, LinkedList<Particle> particles, MechModel mechModel) {
      myName = name;
      myParticles = particles;
      myMechModel = mechModel;
      myMasters = new DynamicComponent[particles.size()];
   }

   public void execute() {
      int i = 0;
      for (Particle p : myParticles) {
         DynamicAttachment a = p.getAttachment();
         if (a.numMasters() == 1) {
            DynamicComponent m = a.getMasters()[0];
            if (m instanceof Particle || m instanceof RigidBody) {
               myMechModel.detachPoint (p);
               myMasters[i] = m;
            }
         }
         i++;
      }
   }

   public void undo() {
      int i = 0;
      for (Particle p : myParticles) {
         ModelComponent master = myMasters[i];
         if (master instanceof Particle) {
            myMechModel.attachPoint (p, (Particle)master); 
         }
         else if (master instanceof RigidBody) {
            myMechModel.attachPoint (p, (RigidBody)master); 
         }
         else {
            // do nothing
         }
         i++;
      }
   }

   public String getName() {
      return myName;
   }
}
