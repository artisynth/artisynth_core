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

   public boolean useNewTransform = true;

//   public TransformComponentsCommand (
//      String name, LinkedList<ModelComponent> comps, 
//      AffineTransform3dBase X, int flags) {
//      myName = name;
//      myComponents = comps;
//      myX = GeometryTransformer.create (X);
//      myFlags = flags;
//   }

   public TransformComponentsCommand (
      String name, LinkedList<ModelComponent> comps, 
      GeometryTransformer gtr, int flags) {
      myName = name;
      myComponents = comps;
      myGtr = gtr;
      myFlags = flags;
   }

//   public void setTransform (AffineTransform3dBase X) {
//      myX = GeometryTransformer.create (X);
//   }

   public void setTransformer (GeometryTransformer gtr) {
      myGtr = gtr;
   }
   
   public GeometryTransformer getTransformer() {
      return myGtr;
   }

   public void setUndoWithInverse (boolean enable) {
      myUndoWithInverse = enable;
   }

//   private ArrayList<RigidTransform3d> getRelativePoses (
//      ConnectableBody body, List<ConnectableBody> bodies) {
//      ArrayList<RigidTransform3d> poses =
//         new ArrayList<RigidTransform3d>(bodies.size());
//
//      for (ConnectableBody bod : bodies) {
//         // X is the transform from bod to this body
//         System.out.println ("free body " + bod.getName());
//         if (bod instanceof RigidBody) {
//            RigidBody rbod = (RigidBody)bod;
//            RigidTransform3d X = new RigidTransform3d();
//            X.mulInverseLeft (rbod.getPose(), rbod.getPose());
//            poses.add (X);
//         }
//      }
//      return poses;
//   }
//   
//   private void setRelativePoses (
//      ConnectableBody body, List<ConnectableBody> bodies, 
//      ArrayList<RigidTransform3d> poses) {
//      
//      if (bodies.size() != poses.size()) {
//         throw new IllegalArgumentException (
//            "Error: poses and bodies have inconsistent sizes");
//      }
//      int i = 0;
//      RigidTransform3d X = new RigidTransform3d();
//      for (ConnectableBody bod : bodies) {
//         if (bod instanceof RigidBody) {
//            RigidBody rbod = (RigidBody)bod;
//            X.mul (rbod.getPose(), poses.get(i));
//            rbod.setPose (X);
//            i++;
//         }
//      }
//   }   

//   private void transform (AffineTransform3dBase X) {
//      if (useNewTransform) {
//         transformNew (X);
//      }
//      else {
//         transformOld (X);
//      }
//   }
//   
//   private void transform (GeometryTransformer X) {
//      transform (X);
//   }
   
//   private void transformOld (AffineTransform3dBase X) {
//      CompositeComponent currentParent = null;
//      
//      ArrayList<ArrayList<ConnectableBody>> allFreeBodies = null;
//      ArrayList<ArrayList<RigidTransform3d>> allFreePoses = null;
//      MechModel mechMod = null;
//
//      boolean articulated = ((myFlags & TransformableGeometry.ARTICULATED) != 0);
//
//      for (ModelComponent c : myComponents) {
//         if (c instanceof TransformableGeometry) {
//            TransformableGeometry tg = (TransformableGeometry)c;
//            if (articulated && c instanceof RigidBody) {
//               RigidBody bod = (RigidBody)c;
//
//               if (allFreeBodies == null) {
//                  allFreeBodies = new ArrayList<ArrayList<ConnectableBody>>();
//                  allFreePoses = new ArrayList<ArrayList<RigidTransform3d>>();
//               }
//               ArrayList<ConnectableBody> freeBods = new ArrayList<ConnectableBody>();
//               BodyConnector.findFreeAttachedBodies (
//                  bod, freeBods, /*rejectSelected*/true);
//               ArrayList<RigidTransform3d> freePoses =
//                  getRelativePoses (bod, freeBods);
//               allFreeBodies.add (freeBods);
//               allFreePoses.add (freePoses);
//               if (bod.getGrandParent() instanceof MechModel) {
//                  mechMod = (MechModel)bod.getGrandParent();
//               }
//            }
//            
//            // XXX: Sanchez, Jan 2013
//            // check if marker and parent are transformed
//            if (c instanceof FemMarker) {
//               FemMarker mk = (FemMarker)c;
//               if (myComponents.contains(mk.getFemModel())) {
//                  continue;
//               }
//            } else if (c instanceof FrameMarker) {
//               FrameMarker mk = (FrameMarker)c;
//               if (myComponents.contains(mk.getFrame())) {
//                  continue;
//               }
//            }
//            
//            tg.transformGeometry (X, tg, myFlags);
//            if (c.getParent() != currentParent) {
//               if (currentParent != null) {
//                  currentParent.notifyParentOfChange (new GeometryChangeEvent (
//                     null, X));
//               }
//               currentParent = c.getParent();
//            }
//         }
//      }
//      if (articulated) {
//         if (mechMod != null) {
//            mechMod = MechModel.topMechModel (mechMod);
//            mechMod.projectRigidBodyPositionConstraints();
//         }
//         int k = 0;
//         for (ModelComponent c : myComponents) {
//            if (c instanceof RigidBody) {
//               RigidBody bod = (RigidBody)c;
//               setRelativePoses (
//                  bod, allFreeBodies.get(k), allFreePoses.get(k));
//               k++;
//            }
//         }
//      }
//      if (currentParent != null) {
//         currentParent.notifyParentOfChange (new GeometryChangeEvent (null, X));
//      }
//
//   }

//   private void transformNew (AffineTransform3dBase X) {
//      CompositeComponent currentParent = null;
//      
//      ArrayList<ArrayList<ConnectableBody>> allFreeBodies = null;
//      ArrayList<ArrayList<RigidTransform3d>> allFreePoses = null;
//      MechModel mechMod = null;
//
//      boolean articulated = ((myFlags & TransformableGeometry.ARTICULATED) != 0);
//      System.out.println ("articulated=" + articulated);
//      TransformGeometryContext context = new TransformGeometryContext ();
//
//      for (ModelComponent c : myComponents) {
//         if (c instanceof TransformableGeometry) {
//            TransformableGeometry tg = (TransformableGeometry)c;
//            if (articulated && c instanceof RigidBody) {
//               RigidBody bod = (RigidBody)c;
//
//               if (allFreeBodies == null) {
//                  allFreeBodies = new ArrayList<ArrayList<ConnectableBody>>();
//                  allFreePoses = new ArrayList<ArrayList<RigidTransform3d>>();
//               }
//               ArrayList<ConnectableBody> freeBods = new ArrayList<ConnectableBody>();
//               BodyConnector.findFreeAttachedBodies (
//                  bod, freeBods, /*rejectSelected*/true);
//               ArrayList<RigidTransform3d> freePoses =
//                  getRelativePoses (bod, freeBods);
//               allFreeBodies.add (freeBods);
//               allFreePoses.add (freePoses);
//               if (bod.getGrandParent() instanceof MechModel) {
//                  mechMod = (MechModel)bod.getGrandParent();
//               }
//            }
//            context.put (tg);
//         }
//      }
//
//      if (allFreeBodies != null) {
//         for (ArrayList<ConnectableBody> list : allFreeBodies) {
//            for (ConnectableBody b : list) {
//               if (b instanceof TransformableGeometry) {
//                  context.put ((TransformableGeometry)b);
//               }
//            }
//         }
//      }
//
//      if (X instanceof RigidTransform3d) {
//         context.apply (new RigidTransformer((RigidTransform3d)X), myFlags); 
//      }
//      else {
//         context.apply (new AffineTransformer((AffineTransform3d)X), myFlags); 
//      }
//
//      for (ModelComponent c : myComponents) {
//         if (c instanceof TransformableGeometry) {
//            if (c.getParent() != currentParent) {
//               if (currentParent != null) {
//                  currentParent.notifyParentOfChange (
//                     new GeometryChangeEvent (null, X));
//               }
//               currentParent = c.getParent();
//            }
//         }
//      }
//   
//      if (articulated) {
//         if (mechMod != null) {
//            mechMod = MechModel.topMechModel (mechMod);
//            mechMod.projectRigidBodyPositionConstraints();
//         }
//         // int k = 0;
//         // for (ModelComponent c : myComponents) {
//         //    if (c instanceof RigidBody) {
//         //       RigidBody bod = (RigidBody)c;
//         //       setRelativePoses (
//         //          bod, allFreeBodies.get(k), allFreePoses.get(k));
//         //       k++;
//         //    }
//         // }
//      }
//      if (currentParent != null) {
//         currentParent.notifyParentOfChange (new GeometryChangeEvent (null, X));
//      }
//
//   }

   
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
   
// private TransformGeometryContext createContext (
// List<ModelComponent> comps, boolean articulated) {
// 
// articulated = false;
// TransformGeometryContext context = new TransformGeometryContext ();
//
// ArrayList<ConnectableBody> freeBods = new ArrayList<ConnectableBody>();
// ArrayList<BodyConnector> connectors = new ArrayList<BodyConnector>();
// 
// for (ModelComponent c : myComponents) {
//    if (c instanceof TransformableGeometry) {
//       TransformableGeometry tg = (TransformableGeometry)c;
//       if (articulated && c instanceof RigidBody) {
//          RigidBody bod = (RigidBody)c;
//          BodyConnector.findFreeAttachedBodies (
//             bod, freeBods, connectors, context);
//       }
//       context.put (tg);
//    }
// }
//
//      for (ConnectableBody b : freeBods) {
//         if (b instanceof TransformableGeometry) {
//            context.put ((TransformableGeometry)b);
//         }
//      }
//      for (BodyConnector c : connectors) {
//         if (c instanceof TransformableGeometry) {
//            context.put ((TransformableGeometry)c);
//         }
//      }
//     return context;
//   }
      
   // private boolean isTransforming (
   //    ModelComponent comp, TransformGeometryContext context) {
   //    return (comp instanceof TransformableGeometry &&
   //            context.contains ((TransformableGeometry)comp));
   // }

   // private void notifyParentsOfChange (TransformGeometryContext context) {
   //    HashSet<CompositeComponent> parents = 
   //       new LinkedHashSet<CompositeComponent>();
   //    for (TransformableGeometry tg : context.getTransformables()) {
   //       if (tg instanceof ModelComponent) {
   //          CompositeComponent parent = ((ModelComponent)tg).getParent();
   //          if (!isTransforming(parent, context)) {
   //             parents.add (parent);
   //          }
   //       }
   //    }
   //    GeometryChangeEvent e = new GeometryChangeEvent (null, myX);
   //    for (CompositeComponent parent : parents) {
   //       parent.componentChanged (e);
   //    }
   // }

   public void transform (GeometryTransformer gtr) {
      TransformGeometryContext context = createContext (myComponents);
      context.apply (gtr, myFlags);
//      boolean articulated = 
//      ((myFlags & TransformableGeometry.ARTICULATED) != 0);
//
//      TransformGeometryContext context = 
//         createContext (myComponents, articulated);
//      context.apply (gtr, myFlags); 
//      if (articulated && myComponents.size() > 0) {
//         MechModel topMech = MechModel.topMechModel (myComponents.get(0));
//         if (topMech != null) {
//            //System.out.println ("would have projected here");
//            //topMech.projectRigidBodyPositionConstraints();
//         }
//      }
      //notifyParentsOfChange (context); // now done in TransformGeometryContext
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
