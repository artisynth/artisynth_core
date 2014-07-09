/**
 * Copyright (c) 2014, by the Authors: John Lloyd (UBC), Tracy Wilkinson (UBC) and
 * ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */package artisynth.core.gui.editorManager;

import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.*;
import artisynth.core.util.TransformableGeometry;
import artisynth.core.driver.Main;
import artisynth.core.femmodels.FemMarker;
import maspack.util.*;
import maspack.matrix.*;

import java.util.*;

public class TransformComponentsCommand implements Command, Clonable {
   private String myName;
   private LinkedList<ModelComponent> myComponents;
   private AffineTransform3dBase myX;
   private int myFlags = 0;

   public TransformComponentsCommand (
      String name, LinkedList<ModelComponent> comps, 
      AffineTransform3dBase X, int flags) {
      myName = name;
      myComponents = comps;
      myX = X;
      myFlags = flags;
   }

   public void setTransform (AffineTransform3dBase X) {
      myX = X;
   }

   private void transform (AffineTransform3dBase X) {
      CompositeComponent currentParent = null;
      
      ArrayList<ArrayList<RigidBody>> allFreeBodies = null;
      ArrayList<ArrayList<RigidTransform3d>> allFreePoses = null;
      MechModel mechMod = null;

      boolean articulated = ((myFlags & TransformableGeometry.ARTICULATED) != 0);

      for (ModelComponent c : myComponents) {
         if (c instanceof TransformableGeometry) {
            TransformableGeometry tg = (TransformableGeometry)c;
            if (articulated && c instanceof RigidBody) {
               RigidBody bod = (RigidBody)c;

               if (allFreeBodies == null) {
                  allFreeBodies = new ArrayList<ArrayList<RigidBody>>();
                  allFreePoses = new ArrayList<ArrayList<RigidTransform3d>>();
               }
               ArrayList<RigidBody> freeBods = new ArrayList<RigidBody>();
               bod.findFreeAttachedBodies (freeBods, /*rejectSelected*/true);
               ArrayList<RigidTransform3d> freePoses =
                  bod.getRelativePoses (freeBods);
               allFreeBodies.add (freeBods);
               allFreePoses.add (freePoses);
               if (bod.getGrandParent() instanceof MechModel) {
                  mechMod = (MechModel)bod.getGrandParent();
               }
            }
            
            // XXX: Sanchez, Jan 2013
            // check if marker and parent are transformed
            if (c instanceof FemMarker) {
               FemMarker mk = (FemMarker)c;
               if (myComponents.contains(mk.getFemModel())) {
                  continue;
               }
            } else if (c instanceof FrameMarker) {
               FrameMarker mk = (FrameMarker)c;
               if (myComponents.contains(mk.getFrame())) {
                  continue;
               }
            }
            
            tg.transformGeometry (X, tg, myFlags);
            if (c.getParent() != currentParent) {
               if (currentParent != null) {
                  currentParent.notifyParentOfChange (new GeometryChangeEvent (
                     null, X));
               }
               currentParent = c.getParent();
            }
         }
      }
      if (articulated) {
         if (mechMod != null) {
            mechMod = mechMod.topMechModel (mechMod);
            mechMod.projectRigidBodyPositionConstraints();
         }
         ArrayList<RigidBody> freeBodies = new ArrayList<RigidBody>();
         int k = 0;
         for (ModelComponent c : myComponents) {
            if (c instanceof RigidBody) {
               RigidBody bod = (RigidBody)c;
               bod.setRelativePoses (allFreeBodies.get(k), allFreePoses.get(k));
               k++;
            }
         }
      }
      if (currentParent != null) {
         currentParent.notifyParentOfChange (new GeometryChangeEvent (null, X));
      }

   }

   public void execute() {
      transform (myX);
   }

   public void undo() {
      AffineTransform3dBase Xinverse = myX.clone();
      Xinverse.invert();
      transform (Xinverse);
   }

   public String getName() {
      return myName;
   }

   public TransformComponentsCommand clone() throws CloneNotSupportedException {
      TransformComponentsCommand cmd = (TransformComponentsCommand)super.clone();
      cmd.myComponents = (LinkedList<ModelComponent>)myComponents.clone();
      cmd.myX = (AffineTransform3dBase)myX.clone();
      return cmd;
   }
}
