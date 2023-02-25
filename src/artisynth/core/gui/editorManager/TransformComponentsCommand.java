/**
 * Copyright (c) 2014, by the Authors: John Lloyd (UBC), Tracy Wilkinson (UBC) and
 * ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.gui.editorManager;

import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.*;
import maspack.geometry.GeometryTransformer;
import maspack.geometry.RigidTransformer;
import maspack.geometry.GeometryTransformer.UndoState;
import maspack.util.*;
import maspack.matrix.*;

import java.util.*;

public class TransformComponentsCommand implements Command, Clonable {
   private String myName;
   private LinkedList<ModelComponent> myComponents;
   private int myFlags = 0;

   private GeometryTransformer myGtr;

   private boolean myUndoWithInverse = true;

   public TransformComponentsCommand (
      String name, LinkedList<ModelComponent> comps, 
      GeometryTransformer gtr, int flags) {
      myName = name;
      myComponents = comps;
      myGtr = gtr;
      myFlags = flags;
   }

   public void setTransformer (GeometryTransformer gtr) {
      myGtr = gtr;
   }
   
   public GeometryTransformer getTransformer() {
      return myGtr;
   }

   public void setUndoWithInverse (boolean enable) {
      myUndoWithInverse = enable;
   }

   private TransformGeometryContext createContext (
      List<ModelComponent> comps) {

      TransformGeometryContext context = new TransformGeometryContext ();

      for (ModelComponent c : myComponents) {
         if (c instanceof TransformableGeometry) {
            TransformableGeometry tg = (TransformableGeometry)c;
            context.add (tg);
         }
      }
      return context;
   }
   
   public void transform (GeometryTransformer gtr) {
      TransformGeometryContext context = createContext (myComponents);
      context.apply (gtr, myFlags);
   }
   
   public void execute() {
      transform (myGtr);
   }

   public void undo() {
      if (myUndoWithInverse) {
         GeometryTransformer invGtr = myGtr.getInverse();
         transform (invGtr);
      }
      else {
         myGtr.setUndoState (UndoState.RESTORING);
         transform (myGtr);
      }
   }

   public String getName() {
      return myName;
   }

   public TransformComponentsCommand clone() throws CloneNotSupportedException {
      TransformComponentsCommand cmd = (TransformComponentsCommand)super.clone();
      cmd.myComponents = (LinkedList<ModelComponent>)myComponents.clone();
      return cmd;
   }
}
