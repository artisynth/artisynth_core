package artisynth.core.mechmodels;

import java.util.*;

import maspack.util.*;
import maspack.matrix.*;
import maspack.geometry.*;
import maspack.render.*;

import artisynth.core.mechmodels.*;
import artisynth.core.mechmodels.Collidable.Collidability;
import artisynth.core.mechmodels.Collidable.Group;
import artisynth.core.modelbase.*;
import artisynth.core.femmodels.*;
import artisynth.core.workspace.*;

/**
 * Test jig for the CollisionManager. Note that this does *not* test actual
 * collision handling; it just tests the CollisionManagerNew's management
 * of what components collide with what.
 */
public class CollisionManagerTest extends UnitTest {

   boolean printMaps = false;

   MechModel myMech;
   MechModel mySubMech;
   HashMap<CollidablePair,CollisionBehavior> myBehaviorMap =
      new HashMap<CollidablePair,CollisionBehavior>();
   CollisionBehavior[] myDefaults;
   
   private int RIGID_RIGID = 0;
   private int DEFORMABLE_RIGID = 1;
   private int DEFORMABLE_DEFORMABLE = 2;
   private int DEFORMABLE_SELF = 3;

   public CollisionManagerTest() {
      myDefaults = new CollisionBehavior[4];
      for (int i=0; i<4; i++) {
         myDefaults[i] = new CollisionBehavior();
      }
   }

   void verifyDefaults (Group g0, Group g1, int chkIdx) {
      CollisionBehavior behav = myMech.getDefaultCollisionBehavior (g0, g1);
      CollisionBehavior check = myDefaults[chkIdx];
      if (!check.equals (behav)) {
         throw new TestException (
            "Default behavior "+(new CollidablePair(g0,g1))+": expected "+
            toStr(check)+", got "+toStr(behav));
      }      
   }
   
   void verifyDefaults() {
      verifyDefaults (Collidable.Rigid, Collidable.Rigid, 0);
      verifyDefaults (Collidable.Deformable, Collidable.Rigid, 1);
      verifyDefaults (Collidable.Rigid, Collidable.Deformable, 1);
      verifyDefaults (Collidable.Deformable, Collidable.Deformable, 2);
      verifyDefaults (Collidable.Deformable, Collidable.Self, 3);
      verifyDefaults (Collidable.Self, Collidable.Deformable, 3);
   }

   private void azzert (String msg, boolean condition) {
      if (!condition) {
         throw new TestException (
            "Assertion failed: " + (msg == null ? "" :msg));
      }
   }

   public void addSubMesh (FemModel3d fem, String name, int[] nums) {
      ArrayList<FemElement3d> elems = new ArrayList<FemElement3d>();
      for (int n : nums) {
         elems.add (fem.getElements().getByNumber(n));
      }
      FemMeshComp comp = FemMeshComp.createVolumetricSurface (name, fem, elems);
      fem.addMeshComp (comp);
   }

   RigidBody createCompositeBody (String name, double rad) {
      
      // create and add the composite body and plate
      PolygonalMesh ball1 = MeshFactory.createIcosahedralSphere (rad, 1);
      ball1.transform (new RigidTransform3d (2*rad, 0, 0));
      PolygonalMesh ball2 = MeshFactory.createIcosahedralSphere (rad, 1);
      ball2.transform (new RigidTransform3d (-2*rad, 0, 0));
      PolygonalMesh axis = MeshFactory.createCylinder (rad/4, 2.5*rad, 12);
      axis.transform (new RigidTransform3d (0, 0, 0, 0, Math.PI/2, 0));

      RigidBody body = new RigidBody (name);
      body.setDensity (10);
      body.addMesh (ball1);
      body.addMesh (ball2);
      body.addMesh (axis);
      return body;
   }

   public MechModel createMechModel (String name) {
      MechModel mech = new MechModel (name);

      double density = 1000;

      RigidBody ball = RigidBody.createSphere ("ball", 0.5, density, 20);
      ball.transformGeometry (new RigidTransform3d (2, 0, 1));
      RigidBody base = RigidBody.createBox ("base", 6, 2, 0.5, density);
      base.transformGeometry (new RigidTransform3d (0, 0, 0));

      FemModel3d fem1 = FemFactory.createHexGrid (null, 1, 1, 1, 4, 4, 4);
      fem1.setSurfaceRendering (FemModel3d.SurfaceRender.Shaded);
      fem1.setName ("fem1");
      RenderProps.setVisible (fem1.getSurfaceMeshComp(), false);
      fem1.transformGeometry (new RigidTransform3d (0, 0, 1));
      addSubMesh (fem1, "sub1", new int[] { 5, 6, 9, 10 });
      addSubMesh (fem1, "sub2", new int[] { 53, 54, 57, 58 });
      FemModel3d fem2 = FemFactory.createHexGrid (null, 1, 1, 1, 4, 4, 4);
      fem1.setSurfaceRendering (FemModel3d.SurfaceRender.Shaded);
      fem2.setName ("fem2");
      RenderProps.setVisible (fem2.getSurfaceMeshComp(), false);
      fem2.transformGeometry (new RigidTransform3d (-2, 0, 1));
      addSubMesh (fem2, "sub1", new int[] { 5, 6, 9, 10 });
      addSubMesh (fem2, "sub2", new int[] { 53, 54, 57, 58 });

      RigidBody bod = createCompositeBody ("comp", 0.3);
      bod.transformGeometry (new RigidTransform3d (0, 0, 3));
      mech.addRigidBody (bod);

      mech.addRigidBody (ball);
      mech.addRigidBody (base);
      mech.addModel (fem1);
      mech.addModel (fem2);
      return mech;
   }

   public MechModel createSubModel (String name) {
      MechModel mech = new MechModel (name);

      double density = 1000;

      RigidBody ball1 = RigidBody.createSphere ("ball1", 0.5, density, 20);
      ball1.transformGeometry (new RigidTransform3d (2, 0, 5));
      RigidBody ball2 = RigidBody.createSphere ("ball2", 0.5, density, 20);
      ball2.transformGeometry (new RigidTransform3d (-2, 0, 5));

      FemModel3d fem3 = FemFactory.createHexGrid (null, 1, 1, 1, 4, 4, 4);
      fem3.setSurfaceRendering (FemModel3d.SurfaceRender.Shaded);
      fem3.setName ("fem3");
      RenderProps.setVisible (fem3.getSurfaceMeshComp(), false);
      fem3.transformGeometry (new RigidTransform3d (0, 0, 1));
      addSubMesh (fem3, "sub1", new int[] { 5, 6, 9, 10 });
      addSubMesh (fem3, "sub2", new int[] { 53, 54, 57, 58 });

      mech.addRigidBody (ball1);
      mech.addRigidBody (ball2);
      mech.addModel (fem3);
      return mech;
   }

   boolean isBody (ModelComponent c) {
      return (c instanceof CollidableBody);
   }

   ArrayList<CollidableBody> getSubBodies (Collidable c) {
      ArrayList<CollidableBody> list = new ArrayList<CollidableBody>();
      recursivelyGetSubBodies (list, c);
      return list;
   }

   void recursivelyGetSubBodies (
      ArrayList<CollidableBody> list, ModelComponent c) {
      if (c instanceof CompositeComponent) {
         CompositeComponent comp = (CompositeComponent)c;
         for (int i=0; i<comp.numComponents(); i++) {
            ModelComponent child = comp.get(i);
            if (isBody (child)) {
               list.add ((CollidableBody)child);
            }
            else {
               recursivelyGetSubBodies (list, child);
            }
         }
      }
   }

   private Group getGroup (Collidable c) {
      if (c.isDeformable()) {
         return Collidable.Deformable;
      }
      else {
         return Collidable.Rigid;
      }
   }

   Collidable nearestCollidableAncestor (Collidable c0, Collidable c1) {
      Collidable ancestor0 = c0.getCollidableAncestor();
      Collidable ancestor1 = c1.getCollidableAncestor();
      if (ancestor0 == ancestor1) {
         return ancestor0;
      }
      else {
         return null;
      }
   }

//   CollisionBehavior getPrimaryBehavior (CollidableBody  a, CollidableBody b) {
//
//      MechModel mech = lowestCommonModel (a, b);
//      if (mech == myMech) {
//         return myDefaults.get (getDefaultPair (a, b));
//      }
//      else {
//         return mech.getCollisionBehavior (a, b);
//      }
//   }
//
   boolean isInternallyCollidable (Collidable c) {
      return (c.getCollidable() == Collidability.ALL ||
              c.getCollidable() == Collidability.INTERNAL);
   }
 
   boolean isExternallyCollidable (Collidable c) {
      return (c.getCollidable() == Collidability.ALL ||
              c.getCollidable() == Collidability.EXTERNAL);
   }

   CollisionBehavior getDominantBehavior (
      CollisionBehavior behav0, CollisionBehavior behav1,
      CollisionManager cm) {

      if (behav0 != null && behav1 != null) {
         if (cm.myBehaviors.indexOf(behav0) > cm.myBehaviors.indexOf(behav1)) {
            return behav0;
         }
         else {
            return behav1;
         }
      }
      else {
         if (behav0 != null) {
            return behav0;
         }
         else {
            return behav1;
         }
      }
   }         

   CollisionBehavior getGroupBehavior (
      CollidableBody cb, Collidable.Group group, CollisionManager cm) {
      
      ArrayList<CollidablePair> pairs = new ArrayList<CollidablePair>();
      pairs.add (new CollidablePair (cb, group));
      pairs.add (new CollidablePair (cb, Collidable.AllBodies));
      pairs.add (new CollidablePair (cb, Collidable.All));
      Collidable ancestor = cb.getCollidableAncestor();
      if (ancestor != null) {
         pairs.add (new CollidablePair (ancestor, group));
         pairs.add (new CollidablePair (ancestor, Collidable.AllBodies));
         pairs.add (new CollidablePair (ancestor, Collidable.All));
      }
      CollisionBehavior behav = null;
      for (CollidablePair pair : pairs) {
         behav = getDominantBehavior (behav, cm.myBehaviors.get(pair), cm);
      }
      return behav;
   }

   CollisionBehavior getOverrideBehavior (
      CollidableBody cb0, CollidableBody cb1, CollisionManager cm) {

      ArrayList<CollidablePair> pairs = new ArrayList<CollidablePair>();
      pairs.add (new CollidablePair (cb0, cb1));
      Collidable ancestor0 = cb0.getCollidableAncestor();
      Collidable ancestor1 = cb1.getCollidableAncestor();
      if (ancestor0 != null) {
         pairs.add (new CollidablePair (ancestor0, cb1));
      }
      if (ancestor1 != null) {
         pairs.add (new CollidablePair (cb0, ancestor1));
      }
      if (ancestor0 != null && ancestor1 != null) {
         pairs.add (new CollidablePair (ancestor0, ancestor1));
      }
      CollisionBehavior behav = null;
      for (CollidablePair pair : pairs) {
         behav = getDominantBehavior (behav, cm.myBehaviors.get(pair), cm);
      }
      return behav;
   }      

//   CollisionBehavior getRigidBehavior (
//      CollidableBody cb, CollisionManagerNew cm) {
//      
//      ArrayList<CollidablePair> pairs = new ArrayList<CollidablePair>();
//      pairs.add (new CollidablePair (cb, Collidable.Rigid));
//      pairs.add (new CollidablePair (cb, Collidable.AllBodies));
//      pairs.add (new CollidablePair (cb, Collidable.All));
//      Collidable ancestor = cb.getCollidableAncestor();
//      if (ancestor != null) {
//         pairs.add (new CollidablePair (ancestor, Collidable.Rigid));
//         pairs.add (new CollidablePair (ancestor, Collidable.AllBodies));
//         pairs.add (new CollidablePair (ancestor, Collidable.All));
//      }
//      CollisionBehavior behav = null;
//      for (CollidablePair pair : pairs) {
//         behav = getDominantBehavior (behav, cm.myBehaviors.get (pair), cm);
//      }
//      return behav;
//   }

   private void setBehaviorMap (
      CollidableBody cb0, CollidableBody cb1, CollisionManager cm) {

      CollisionBehavior behav;
      CollidablePair pair = new CollidablePair (cb0, cb1);
      Collidable ancestor = nearestCollidableAncestor (cb0, cb1);
      if (ancestor != null) {
         // check for self collision
         if (ancestor.isDeformable() &&
             CollisionManager.isInternallyCollidable (cb0) &&
             CollisionManager.isInternallyCollidable (cb1)) {
            behav = cm.myBehaviors.get (pair);
            if (behav != null) {
               myBehaviorMap.put (pair, behav);
               return;
            }
            behav = cm.myBehaviors.get (
               new CollidablePair (ancestor, Collidable.Self));
            if (behav != null) {
               myBehaviorMap.put (pair, behav);
               return;
            }
            behav = cm.myBehaviors.get (
               new CollidablePair (ancestor, Collidable.All));
            if (behav != null) {
               myBehaviorMap.put (pair, behav);
               return;
            }
            myBehaviorMap.put (
               pair, cm.getDefaultBehavior (
                  Collidable.Deformable, Collidable.Self));
         }
      }
      else {
         // check for external collision
         if (CollisionManager.isExternallyCollidable (cb0) &&
             CollisionManager.isExternallyCollidable (cb1)) {
            behav = getOverrideBehavior (cb0, cb1, cm);
            if (behav != null) {
               myBehaviorMap.put (pair, behav);
               return;
            }
            if (cb0.isDeformable() && cb1.isDeformable()) {
               // both deformable
               behav = getDominantBehavior (
                  getGroupBehavior (cb0, Collidable.Deformable, cm),
                  getGroupBehavior (cb1, Collidable.Deformable, cm), cm);
            }
            else if (cb0.isDeformable() && !cb1.isDeformable()) {
               // one deformable, one rigid
               behav = getDominantBehavior (
                  getGroupBehavior (cb0, Collidable.Rigid, cm),
                  getGroupBehavior (cb1, Collidable.Deformable, cm), cm);
            }
            else if (!cb0.isDeformable() && cb1.isDeformable()) {
               // one deformable, one rigid
               behav = getDominantBehavior (
                  getGroupBehavior (cb0, Collidable.Deformable, cm),
                  getGroupBehavior (cb1, Collidable.Rigid, cm), cm);
            }
            else {
               // both rigid
               behav = getDominantBehavior (
                  getGroupBehavior (cb0, Collidable.Rigid, cm),
                  getGroupBehavior (cb1, Collidable.Rigid, cm), cm);
            }
            if (behav == null) {
               behav = cm.getDefaultBehavior (getGroup(cb0), getGroup(cb1));
            }
            myBehaviorMap.put (pair, behav);
            return;
         }
      }
   }

//   private ArrayList<CollidableBody> getAllCollidableBodies() {
//      ArrayList<Collidable> collidables = new ArrayList<Collidable>();
//      myMech.getCollidables (collidables, 0);
//      ArrayList<CollidableBody> cbodies = new ArrayList<CollidableBody>();
//      for (Collidable c : collidables) {
//         if (c instanceof CollidableBody) {
//            cbodies.add ((CollidableBody)c);
//         }
//      }
//      return cbodies;
//   }

   private void updateBehaviorMap() {
      myBehaviorMap.clear();
      ArrayList<CollidableBody> cbodies = myMech.getCollidableBodies();
      for (int i=0; i<cbodies.size(); i++) {
         CollidableBody cbi = cbodies.get(i);
         for (int j=i+1; j<cbodies.size(); j++) {
            CollidableBody cbj = cbodies.get(j);
            MechModel mech = MechModel.lowestCommonModel (cbi, cbj);
            setBehaviorMap (cbi, cbj, mech.getCollisionManager());
         }
      }
   }

//   private boolean equal (CollisionBehavior b0, CollisionBehavior b1) {
//      if (b0 == null && b1 == null) {
//         return true;
//      }
//      else if ((b0 == null && b1 != null) || (b0 != null && b1 == null)) {
//         return false;
//      }
//      else {
//         return b0.equals (b1);
//      }
//   }

   CollisionBehavior getMapBehavior ( CollidableBody c0, CollidableBody c1) {
      return getMapBehavior (myBehaviorMap, c0, c1);
   }

   CollisionBehavior getMapBehavior (
      HashMap<CollidablePair,CollisionBehavior> map,
      CollidableBody c0, CollidableBody c1) {

      return map.get (new CollidablePair (c0, c1));
   }

   private CollisionBehavior getBehavior (CollidablePair pair) {
      Collidable a = pair.myComp0;
      Collidable b = pair.myComp1;

      if (a == b) {
         if (isBody(a)) {
            return null;
         }
         else {
            ArrayList<CollidableBody> bodies = getSubBodies (a);
            CollisionBehavior behavior = null;
            boolean behaviorSet = false;
            for (int i=0; i<bodies.size(); i++) {
               CollidableBody ci = bodies.get(i);
               if (isInternallyCollidable(ci)) {
                  for (int j=i+1; j<bodies.size(); j++) {
                     CollidableBody aj = bodies.get(j);
                     if (isInternallyCollidable(aj)) {
                        CollisionBehavior behav = getMapBehavior (ci, aj);
                        if (!behaviorSet) {
                           behavior = behav;
                           behaviorSet = true;
                        }
                        else {
                           if (behavior != behav) {
                              return null;
                           }
                        }
                     }
                  }
               }
            }
            return behavior;
         }
      }
      else if (isBody(a) && isBody(b)) {
         return myBehaviorMap.get(pair);
      }
      else {
         ArrayList<CollidableBody> bodiesA;
         ArrayList<CollidableBody> bodiesB;
         if (isBody(a)) {
            bodiesA = new ArrayList<CollidableBody>();
            bodiesA.add ((CollidableBody)a);
         }
         else {
            bodiesA = getSubBodies (a);
         }
         if (isBody(b)) {
            bodiesB = new ArrayList<CollidableBody>();
            bodiesB.add ((CollidableBody)b);
         }
         else {
            bodiesB = getSubBodies (b);
         }
         CollisionBehavior behavior = null;
         boolean behaviorSet = false;
         for (int i=0; i<bodiesA.size(); i++) {
            CollidableBody bi = bodiesA.get(i);
            if (isExternallyCollidable (bi)) {
               for (int j=0; j<bodiesB.size(); j++) {
                  CollidableBody cj = bodiesB.get(j);
                  if (isExternallyCollidable (cj)) {
                     CollisionBehavior behav = getMapBehavior (bi, cj);
                     if (!behaviorSet) {
                        behavior = behav;
                        behaviorSet = true;
                     }
                     else {
                        if (behavior != behav) {
                           return null;
                        }
                     }
                  }
               }
            }
         }
         return behavior;
      }
   }

   private void checkHandlerTable () {
      CollisionManager cm = myMech.getCollisionManager();
      ArrayList<CollidableBody> cbodies = myMech.getCollidableBodies();
      cm.updateConstraints (0, /*flags=*/CollisionManager.CONTACT_TEST_MODE);
      // call again to test rebuilding the handler table
      cm.updateConstraints (0, /*flags=*/CollisionManager.CONTACT_TEST_MODE);

      if (printMaps) {
         System.out.println ("handlers:");
         printHandlerMap();
      }
      for (int i=0; i<cbodies.size(); i++) {
         CollidableBody cbi = cbodies.get(i);
         for (int j=i+1; j<cbodies.size(); j++) {
            CollidableBody cbj = cbodies.get(j);
            MechModel mech = MechModel.lowestCommonModel (cbi, cbj);
            CollisionHandler handler =
               mech.getCollisionManager().getHandlerTable().get (cbi, cbj);
            CollisionBehavior behav = myMech.getActingCollisionBehavior(cbi,cbj);
            CollidablePair pair = new CollidablePair (cbi, cbj);
            if (handler == null) {
               if (behav != null && behav.isEnabled()) {
                  throw new TestException (
                     "Response for "+pair+" is empty, expecting behavior "+
                     toStr(behav));                     
               }
            }
            else {
               if (handler.myBehavior != behav) {
                  System.out.println ("");
                  printHandlerMap();
                  throw new TestException (
                     "Response for "+pair+" has behavior "+
                     toStr(handler.myBehavior)+", expecting "+toStr(behav));
               }
            }
         }
      }
   }

   private void checkBehaviors() {
      ArrayList<Collidable> collidables = new ArrayList<Collidable>();
      myMech.getCollidables(collidables, 0);
      for (int i=0; i<collidables.size(); i++) {
         Collidable ci = collidables.get(i);
         for (int j=0; j<collidables.size(); j++) {
            Collidable cj = collidables.get(j);
            CollidablePair pair = new CollidablePair (ci, cj);
            CollisionBehavior behav = myMech.getActingCollisionBehavior (ci, cj);
            CollisionBehavior check = getBehavior (pair);
            if (check != behav) {
               throw new TestException (
                  "getBehavior for "+pair+":\n"+
                  "expected " + toStr(check) + " got " + toStr(behav));
            }
         }
      }
   }

   private boolean nameEquals (Collidable c, String name) {
      String str = ComponentUtils.getPathName (c);
      return str.equals (name);
   }

   private boolean pairMatches (
      CollidablePair pair, CollidableBody cb0, CollidableBody cb1) {

      Collidable c0 = pair.get(0);
      Collidable c1 = pair.get(1);

      Collidable an0 = cb0.getCollidableAncestor();
      Collidable an1 = cb1.getCollidableAncestor();

      if (c1 instanceof Group) {
         Group g1 = (Group)c1;

         if (an0 == an1 && an0 != null) {
            return ((an0 == c0 || cb0 == c0 || cb1 == c0) && g1.includesSelf());
         }
         else {
            if (g1.includesRigid()) {
               if (isBody (c0)) {
                  if ((cb0 == c0 && !cb1.isDeformable()) ||
                      (cb1 == c0 && !cb0.isDeformable())) {
                     return true;
                  }
               }
               else {
                  if ((an0 == c0 && !cb1.isDeformable()) ||
                      (an1 == c0 && !cb0.isDeformable())) {
                     return true;
                  }
               }
            }
            if (g1.includesDeformable()) {
               if (isBody (c0)) {
                  if ((cb0 == c0 && cb1.isDeformable()) ||
                      (cb1 == c0 && cb0.isDeformable())) {
                     return true;
                  }
               }
               else {
                  if ((an0 == c0 && cb1.isDeformable()) ||
                      (an1 == c0 && cb0.isDeformable())) {
                     return true;
                  }
               }
            }
         }
         return false;
      }
      else {
         if (isBody (c0) && isBody (c1)) {
            return ((c0 == cb0 && c1 == cb1) || (c0 == cb1 && c1 == cb0));
         }
         else if (isBody (c0)) {
            return ((c0 == cb0 && c1 == an1) || (c0 == an1 && c1 == cb0));
         }
         else if (isBody (c1)) {
            return ((c0 == an0 && c1 == cb1) || (c0 == an1 && c1 == cb0));
         }
         else {
            return ((c0 == an0 && c1 == an1) || (c0 == an1 && c1 == an0));
         }
      }
   }

   private void checkResponse (CollisionResponse resp, CollisionManager cm) {
      HashSet<CollisionHandler> handlers = new HashSet<CollisionHandler>();
      handlers.addAll (resp.getHandlers());

      if (handlers.size() != resp.getHandlers().size()) {
         System.out.println ("raw handlers=");
         for (CollisionHandler ch : resp.getHandlers()) {
            System.out.println ("  "+ch.getCollidablePair());
         }
         System.out.println ("handlers=");
         for (CollisionHandler ch : handlers) {
            System.out.println ("  "+ch.getCollidablePair());
         }
         throw new TestException (
            "Repeated handlers for response " + resp.getCollidablePair());
      }
      HashSet<CollisionHandler> expected = new HashSet<CollisionHandler>();
      ArrayList<CollisionHandler> allHandlers = new ArrayList<CollisionHandler>();
      cm.collectHandlers (allHandlers);
      for (CollisionHandler ch : allHandlers) {
         CollidableBody cb0 = ch.getCollidable(0);
         CollidableBody cb1 = ch.getCollidable(1);
         if (pairMatches (resp.getCollidablePair(), cb0, cb1)) {
            expected.add (ch);
         }
      }
      if (!expected.equals (handlers)) {
         System.out.println ("handlers=");
         for (CollisionHandler ch : handlers) {
            System.out.println ("  "+ch.getCollidablePair());
         }
         System.out.println ("expected=");
         for (CollisionHandler ch : expected) {
            System.out.println ("  "+ch.getCollidablePair());
         }
         throw new TestException (
            "Unexpected handlers for response " + resp.getCollidablePair());
      }
   }      

   private void checkResponses (MechModel mech) {
      CollisionManager cm = mech.getCollisionManager();
      for (CollisionResponse resp : cm.responses()) {
         checkResponse (resp, cm);
      }
   }

   private CollisionHandler getHandler (
      CollidableBody cb0, CollidableBody cb1) {

      MechModel mech = MechModel.lowestCommonModel(cb0, cb1);
      CollisionManager cm = mech.getCollisionManager();
      return cm.getHandlerTable().get (cb0, cb1);
   }

   private boolean hasCommonAncestor (Collidable c0, Collidable c1) {
      return CollisionManager.nearestCommonCollidableAncestor(c0,c1) != null;
   }

   private void checkCollisionResponse (
      Collidable c0, ArrayList<CollidableBody> cbodies, Collidable c1) {

      CollisionResponse resp = myMech.getCollisionResponse (c0, c1);

      HashSet<CollisionHandler> expectedHandlers =
         new HashSet<CollisionHandler>();
      HashSet<CollisionHandler> responseHandlers =
         new HashSet<CollisionHandler>();

      Group g1 = null;
      if (c1 instanceof Group) {
         g1 = (Group)c1;
      }
      if (c0 == c1 || (g1 != null && g1.includesSelf())) {
         // check for self intersection handlers
         if (c0.isDeformable() && c0.isCompound()) {
            ArrayList<CollidableBody> ibods0 =
               CollisionManager.getInternallyCollidableBodies (c0);
            for (int i=0; i<ibods0.size(); i++) {
               CollidableBody cbi = ibods0.get(i);
               for (int j=i+1; j<ibods0.size(); j++) {
                  CollidableBody cbj = ibods0.get(j);
                  CollisionHandler ch = getHandler (cbi, cbj);
                  if (ch != null) {
                     expectedHandlers.add (ch);
                  }
               }
            }
         }
      }
      if (g1 != null) {
         if (g1.includesRigid() || g1.includesDeformable()) {
            ArrayList<CollidableBody> ebods0 =
               CollisionManager.getExternallyCollidableBodies (c0);
            for (int i=0; i<ebods0.size(); i++) {
               CollidableBody cbi = ebods0.get(i);
               for (int j=0; j<cbodies.size(); j++) {
                  CollidableBody cbj = cbodies.get(j);  
                  if (!hasCommonAncestor (cbi, cbj)) {
                     // filter out internal collisions
                     if ((g1.includesRigid() && !cbj.isDeformable()) ||
                         (g1.includesDeformable() && cbj.isDeformable())) {
                        CollisionHandler ch = getHandler (cbi, cbj);
                        if (ch != null) {
                           expectedHandlers.add (ch);
                        }
                     }
                  }
               }
            }
         }
      }
      else {
         if (hasCommonAncestor (c0, c1)) {
            // both c0 and c1 must be bodies - just check for internal
            // collisions
            CollisionHandler ch = getHandler (
               (CollidableBody)c0, (CollidableBody)c1);
            if (ch != null) {
               expectedHandlers.add (ch);
            }
         }
         else {
            ArrayList<CollidableBody> ebods0 =
               CollisionManager.getExternallyCollidableBodies (c0);
            ArrayList<CollidableBody> ebods1 =
               CollisionManager.getExternallyCollidableBodies (c1);
            for (int i=0; i<ebods0.size(); i++) {
               CollidableBody cbi = ebods0.get(i);
               for (int j=0; j<ebods1.size(); j++) {
                  CollidableBody cbj = ebods1.get(j);
                  if (!hasCommonAncestor (cbi, cbj)) {
                     CollisionHandler ch = getHandler (cbi, cbj);
                     if (ch != null) {
                        expectedHandlers.add (ch);
                     }
                  }
               }
            }
         }
      }
      ArrayList<CollisionHandler> handlers = resp.getHandlers();
      responseHandlers.addAll (handlers);
      if (handlers.size() != responseHandlers.size()) {
         System.out.println ("handlers:");
         for (CollisionHandler ch : handlers) {
            System.out.println (" "+ch.getCollidablePair());
         }
         throw new TestException (
            "Redundant response handlers detected for " +
            new CollidablePair (c0, c1));
      }
      if (!expectedHandlers.equals (responseHandlers)) {
         System.out.println ("responseHandlers:");
         for (CollisionHandler ch : responseHandlers) {
            System.out.println (" "+ch.getCollidablePair());
         }
         System.out.println ("expectedHandlers:");
         for (CollisionHandler ch : expectedHandlers) {
            System.out.println (" "+ch.getCollidablePair());
         }
         throw new TestException (
            "Response handlers differ from expected handlers for " +
             new CollidablePair (c0, c1));
         
      }
   }

//   private void checkCollisionResponse() {
//      ArrayList<Collidable> collidables = new ArrayList<Collidable>();
//      ArrayList<CollidableBody> cbodies = myMech.getCollidableBodies();
//      myMech.getCollidables(collidables, 0);
//      for (int i=0; i<collidables.size(); i++) {
//         Collidable ci = collidables.get(i);
//         checkCollisionResponse (ci, cbodies, Collidable.All);
//         checkCollisionResponse (ci, cbodies, Collidable.AllBodies);
//         checkCollisionResponse (ci, cbodies, Collidable.Rigid);
//         checkCollisionResponse (ci, cbodies, Collidable.Deformable);
//         checkCollisionResponse (ci, cbodies, Collidable.Self);
//         for (int j=0; j<collidables.size(); j++) {
//            Collidable cj = collidables.get(j);
//            checkCollisionResponse (ci, cbodies, cj);
//         }
//      }
//   }

   private void printName (String name, int maxlen) {
      System.out.print (name);
      int res = maxlen-name.length()+1;
      for (int i=0; i<res; i++) {
         System.out.print (" ");
      }
   }

   private static enum MapCode {
      TEST,
      BEHAVIORS,
      HANDLERS
   };

   void printBehaviorMap () {
      printMap (myMech, MapCode.BEHAVIORS);
   }

   void printHandlerMap () {
      printMap (myMech, MapCode.HANDLERS);
   }

   void printTestMap () {
      printMap (myMech, MapCode.TEST);
   }

   void printMap (MechModel mech, MapCode code) {
      
      ArrayList<CollidableBody> bodies = mech.getCollidableBodies();

      int maxlen = 0;
      for (int i=0; i<bodies.size(); i++) {
         int len = ComponentUtils.getPathName (bodies.get(i)).length();
         if (len > maxlen) {
            maxlen = len;
         }
      }
      String[] lines = new String[bodies.size()];
      for (int i=0; i<bodies.size(); i++) {
         StringBuilder stb = new StringBuilder();
         CollidableBody ci = bodies.get(i);
         stb.append (ComponentUtils.getPathName(ci));
         // pad name 
         while (stb.length() < maxlen) {
            stb.append (' ');
         }
         for (int j=0; j<bodies.size(); j++) {
            CollidableBody cj = bodies.get(j);
            CollisionBehavior behavior = null;
            switch (code) {
               case BEHAVIORS: {
                  behavior = mech.getActingCollisionBehavior (ci, cj);
                  break;
               }
               case HANDLERS: {
                  CollisionManager cm = mech.getCollisionManager();
                  CollisionHandler ch = cm.getHandlerTable().get (ci, cj);
                  if (ch != null) {
                     behavior = ch.getBehavior();
                  }
                  else {
                     behavior = null;
                  }
                  break;
               }
               case TEST: {
                  behavior = getMapBehavior (myBehaviorMap, ci, cj);
                  break;
               }
            }
            if (behavior != null && behavior.isEnabled()) {
               stb.append (" " + (int)behavior.getFriction());
            }
            else {
               stb.append (" .");
            }
         }
         lines[i] = stb.toString();
      }
      for (String line : lines) {
         System.out.println (line);
      }
   }

   /**
    * Set default values which should mirror those in myMech
    */
   void setCheckDefaults (Group g0, Group g1, boolean enabled, double mu) {
      CollisionBehavior behav = new CollisionBehavior (enabled, mu);
      if ((g0.includesSelf() && g1.includesDeformable()) ||
          (g1.includesSelf() && g0.includesDeformable())) {
         myDefaults[DEFORMABLE_SELF].set (behav);
      }
      if (g0.includesRigid() && g1.includesRigid()) {
         myDefaults[RIGID_RIGID].set (behav);
      }
      if (g0.includesDeformable() && g1.includesDeformable()) {
         myDefaults[DEFORMABLE_DEFORMABLE].set (behav);
      }
      if ((g0.includesDeformable() && g1.includesRigid()) ||
          (g1.includesDeformable() && g0.includesRigid())) {
         myDefaults[DEFORMABLE_RIGID].set (behav);
      }
   }

//   public void setDefaultBehavior (
//      CollidablePair pair, boolean enabled, double mu) {
//
//      setDefaultBehavior (myMech, pair, enabled, mu);
//      setCheckDefaults (pair, enabled, mu);
//      verifyDefaults();
//   }

   public void setDefaultBehavior (
      Group g0, Group g1, boolean enabled, double mu) {

      setDefaultBehavior (myMech, g0, g1, enabled, mu);
      setCheckDefaults (g0, g1, enabled, mu);
      verifyDefaults();
   }

   void runChecks() {
      updateBehaviorMap();
      if (printMaps) {
         System.out.println ("behaviors:");
         printBehaviorMap();
      }
      //System.out.println ("test:");
      //printTestMap();
      testMap (myBehaviorMap);
      checkBehaviors();
      checkHandlerTable();
      checkResponses(myMech);      
      if (mySubMech != null) {
         checkResponses (mySubMech);
      }
   }

   public void setDefaultBehavior (
      MechModel mech, Group g0, Group g1, boolean enabled, double mu) {
      mech.setDefaultCollisionBehavior (g0, g1, enabled, mu);
      runChecks();
   }

   public CollisionBehavior setBehavior (
      Collidable a, Collidable b, boolean enabled, double mu) {
      return setBehavior (myMech, a, b, enabled, mu);
   }

   public CollisionBehavior setBehavior (
      MechModel mech, Collidable a, Collidable b, boolean enabled, double mu) {

      CollisionBehavior behavior = new CollisionBehavior (enabled, mu);
      mech.setCollisionBehavior (a, b, behavior);
      CollisionBehavior check = mech.getCollisionBehavior(a, b);
      azzert ("getBehavior", check == behavior);
      runChecks();
      return behavior;
   }

   public void clearBehavior (Collidable a, Collidable b) {
      clearBehavior (myMech, a, b);
   }

   public void clearBehavior (MechModel mech, Collidable a, Collidable b) {

      mech.clearCollisionBehavior (a, b);
      runChecks();
   }

   public void clearBehaviors () {
      clearBehaviors (myMech);
   }

   public void clearBehaviors (MechModel mech) {

      mech.clearCollisionBehaviors();
      runChecks();
   }

   public void setResponses (Collidable[] cols) {
      setResponses (myMech, cols);
   }

   public void setResponses (MechModel mech, Collidable[] cols) {
      for (int i=0; i<cols.length; i++) {
         Collidable ci = cols[i];
         
         mech.setCollisionResponse (ci, Collidable.All);
         mech.setCollisionResponse (ci, Collidable.Rigid);
         mech.setCollisionResponse (ci, Collidable.Deformable);
         mech.setCollisionResponse (ci, Collidable.AllBodies);
         if (ci.isCompound() && ci.isDeformable()) {
            mech.setCollisionResponse (ci, Collidable.Self);
         }
         for (int j=i+1; j<cols.length; j++) {
            Collidable cj = cols[j];
            if (!ModelComponentBase.recursivelyContains (ci, cj) &&
                !ModelComponentBase.recursivelyContains (cj, ci)) {
               mech.setCollisionResponse (ci, cj);
            }
         }
      }
   }

   public void clearResponses () {
      clearResponses (myMech);
   }
   
   public void clearResponses (MechModel mech) {
      mech.clearCollisionResponses();
   }
   
   public CollisionBehavior getActingBehavior (Collidable a, Collidable b) {
      return myMech.getActingCollisionBehavior (a, b);
   }

   private String getName (Collidable c) {
      if (c instanceof Collidable.Group) {
         return c.toString();
      }
      else {
         return ComponentUtils.getPathName(c);
      }
   }

   private String getName (CollidablePair pair) {
      return getName(pair.myComp0) + "-" + getName(pair.myComp1);
   }      

   // Make sure behavior map is well constructed - no entries with null,
   // and the CollidablePair query works properly.
   public void testMap (HashMap<CollidablePair,CollisionBehavior> map) {
      for (Map.Entry<CollidablePair,CollisionBehavior> entry : map.entrySet()) {
         CollidablePair pair = entry.getKey();
         if (entry.getValue() == null) {
            throw new TestException ("Value for "+getName(pair) + " is null");
         }
         CollisionBehavior behavior =
            map.get(new CollidablePair (pair.myComp0, pair.myComp1));
         if (behavior == null) {
            throw new TestException ("Query for "+getName(pair) + " is null");
         }
         CollisionBehavior transposed =
            map.get(new CollidablePair (pair.myComp1, pair.myComp0));
         if (behavior != transposed) {
            throw new TestException (
               "Query for "+getName(pair) + " not tranpose invariant");
         }
      }
   }

   private CollisionBehavior getBehaviorFromString (String str, int idx) {
      if (idx >= str.length()) {
         return new CollisionBehavior (false, -1);
      }
      else {
         int c = str.charAt(idx);
         if (c == '.') {
            return new CollisionBehavior (false, -1);
         }
         else if (Character.isDigit(c)) {
            return new CollisionBehavior (true, c-'0');
         }
         else {
            throw new InternalErrorException (
               "Unexpected character '"+c+"'");
         }
      }
   }
   
   static String toStr (CollisionBehavior behav) {
      if (behav == null) {
         return "null";
      }
      else {
         return
            "("+behav.isEnabled()+","+behav.getFriction()+" "+
            behav.getFrictionMode()+")";
      }
   }

   private void verify (String str) {
      ArrayList<CollidableBody> bodies = myMech.getCollidableBodies();
      int idx = 0;
      for (int i=0; i<bodies.size(); i++) {
         CollidableBody ci = bodies.get(i);
         for (int j=i; j<bodies.size(); j++) {
            CollidableBody cj = bodies.get(j);
            CollisionBehavior behavior = getMapBehavior (ci, cj);
            while (idx < str.length() &&
                   Character.isWhitespace(str.charAt(idx))) {
               idx++;
            }
            CollisionBehavior check = getBehaviorFromString (str, idx);
            if ((behavior == null && check.isEnabled()) ||
                (behavior != null && !check.equals (behavior))) {
               throw new TestException (
                  "Unexpected behavior for "+new CollidablePair(ci, cj)+": "+
                  toStr(behavior)+", expected "+toStr(check));
            }
            if (idx < str.length()) {
               idx++;
            }
         }
      }
      checkBehaviors();
   }

   Collidable findCollidable (String name) {
      ModelComponent c = myMech.findComponent (name);
      if (c == null) {
         throw new InternalErrorException ("Can't find "+name);
      }
      else {
         return (Collidable)c;
      }
   }

//   CollisionBehavior getBehavior (String name0, String name1) {
//      Collidable col0 = findCollidable(name0);
//      Collidable col1 = findCollidable(name1);
//      return myMech.getCollisionBehavior (col0, col1);
//   }
   
   void testSetDefaults() {
      Group groups[] = new Group[] {
         Collidable.All,
         Collidable.Rigid, 
         Collidable.Deformable, 
         Collidable.AllBodies,
         Collidable.Self
      };
      double cnt = 0.0;
      for (int i=0; i<groups.length; i++) {
         Group gi = groups[i];
         for (int j=0; j<groups.length; j++) {
            Group gj = groups[j];
            if (!(gi==Collidable.Self && gj==Collidable.Rigid) &&
                !(gi==Collidable.Rigid && gj==Collidable.Self) &&
                !(gi==Collidable.Self && gj==Collidable.Self)) {
               setDefaultBehavior (gi, gj, true, cnt++);
            }
         }
      }
      setDefaultBehavior (Collidable.Rigid, Collidable.Rigid, false, 0);
      setDefaultBehavior (Collidable.Deformable, Collidable.Rigid, false, 0);
      setDefaultBehavior (Collidable.Deformable, Collidable.Deformable, false, 0);
      setDefaultBehavior (Collidable.Deformable, Collidable.Self, false, 0);
   }
   
   void testSetDefaultBehaviorArgs (Group g0, Group g1) {
      try {
         myMech.setDefaultCollisionBehavior (g0, g1, false, 0);
      }
      catch (IllegalArgumentException e) {
         return;
      }
      throw new TestException (
         "Should have failed with args " + new CollidablePair(g0, g1));
   }
   
   void testGetDefaultBehaviorArgs (Group g0, Group g1) {
      try {
         myMech.getDefaultCollisionBehavior (g0, g1);
      }
      catch (IllegalArgumentException e) {
         return;
      }
      throw new TestException (
         "Should have failed with args " + new CollidablePair(g0, g1));
   }
   
   void testGetCollisionResponseArgs (Collidable c0, Collidable c1) {
      try {
         myMech.getCollisionResponse (c0, c1);
      }
      catch (IllegalArgumentException e) {
         return;
      }
      throw new TestException (
         "Should have failed with args " + new CollidablePair(c0, c1));
   }
   
   void testSetBehaviorArgs (Collidable c0, Collidable c1) {
      try {
         myMech.setCollisionBehavior (c0, c1, false, 0);
      }
      catch (IllegalArgumentException e) {
         return;
      }
      throw new TestException (
         "Should have failed with args " + new CollidablePair(c0, c1));
   }
   
   void testSetBehaviorArgs (
      Collidable c0, Collidable c1, CollisionBehavior behav) {
      try {
         myMech.setCollisionBehavior (c0, c1, behav);
      }
      catch (IllegalArgumentException e) {
         return;
      }
      throw new TestException (
         "Should have failed with repeated behavior");
   }
   
   @Override
   public void test() {
      myMech = createMechModel ("top");
      CollisionManager cm = myMech.getCollisionManager();
      CollisionBehavior cb;
      
      //top/models/fem1/meshes/surface  . . . 1 1 1 . . .
      //top/models/fem1/meshes/sub1     . . . 1 1 1 . . .
      //top/models/fem1/meshes/sub2     . . . 1 1 1 . . .
      //top/models/fem2/meshes/surface  1 1 1 . . . . . .
      //top/models/fem2/meshes/sub1     1 1 1 . . . . . .
      //top/models/fem2/meshes/sub2     1 1 1 . . . . . .
      //top/rigidBodies/comp            . . . . . . . . .
      //top/rigidBodies/ball            . . . . . . . . .
      //top/rigidBodies/base            . . . . . . . . .

      Collidable fem1 = findCollidable ("models/fem1");
      Collidable fem2 = findCollidable ("models/fem2");
      Collidable comp = findCollidable ("rigidBodies/comp");
      //RigidMeshComp comp2 = findCollidable ("rigidBodies/comp/meshes/2");
      //RigidMeshComp comp1 = findCollidable ("rigidBodies/comp/meshes/1");
      //RigidMeshComp comp0 = findCollidable ("rigidBodies/comp/meshes/0");

      Collidable surf1 = findCollidable ("models/fem1/meshes/surface");
      FemMeshComp sub11 = (FemMeshComp)findCollidable ("models/fem1/meshes/sub1");
      FemMeshComp sub12 = (FemMeshComp)findCollidable ("models/fem1/meshes/sub2");
      Collidable surf2 = findCollidable ("models/fem2/meshes/surface");
      FemMeshComp sub21 = (FemMeshComp)findCollidable ("models/fem2/meshes/sub1");
      FemMeshComp sub22 = (FemMeshComp)findCollidable ("models/fem2/meshes/sub2");
      Collidable ball = findCollidable ("rigidBodies/ball");
      Collidable base = findCollidable ("rigidBodies/base");

      testSetDefaults();
      
      // test exceptions
      
      testGetDefaultBehaviorArgs (Collidable.All, Collidable.Rigid);
      testGetDefaultBehaviorArgs (Collidable.AllBodies, Collidable.Rigid);
      testGetDefaultBehaviorArgs (Collidable.Rigid, Collidable.All);
      testGetDefaultBehaviorArgs (Collidable.Rigid, Collidable.AllBodies);
      testGetDefaultBehaviorArgs (Collidable.Rigid, Collidable.Self);
      testGetDefaultBehaviorArgs (Collidable.Self, Collidable.Rigid);
      testGetDefaultBehaviorArgs (Collidable.Self, Collidable.Self);

      testSetDefaultBehaviorArgs (Collidable.Rigid, Collidable.Self);
      testSetDefaultBehaviorArgs (Collidable.Self, Collidable.Rigid);
      testSetDefaultBehaviorArgs (Collidable.Self, Collidable.Self);

      testGetCollisionResponseArgs (Collidable.Rigid, Collidable.Rigid);
      //testGetCollisionResponseArgs (Collidable.Rigid, base);

      RigidBody straybox = RigidBody.createBox ("base", 6, 2, 0.5, 1);
      
      testSetBehaviorArgs (Collidable.Rigid, Collidable.Rigid);
      testSetBehaviorArgs (ball, Collidable.Self);
      testSetBehaviorArgs (ball, ball);
      testSetBehaviorArgs (ball, straybox);
      testSetBehaviorArgs (straybox, ball);
      testSetBehaviorArgs (comp, Collidable.Self);
      testSetBehaviorArgs (comp, comp);
      testSetBehaviorArgs (fem1, sub11);
      testSetBehaviorArgs (sub11, fem1);
      testSetBehaviorArgs (Collidable.Rigid, ball);
      testSetBehaviorArgs (Collidable.Rigid, fem1);
      
      setDefaultBehavior (
         Collidable.Deformable, Collidable.Deformable, true, 1);

      setResponses (myMech, new Collidable[] {
            fem1, fem2, comp,
            surf1, sub11, sub12, surf2, sub21, sub22, ball, base
         });

      verify (". . . 1 . . . . . "+  // surf1
              "  . . . . . . . . "+  // sub11
              "    . . . . . . . "+  // sub12
              "      . . . . . . "+  // surf2
              "        . . . . . "+  // sub21
              "          . . . . "); // sub22
      setDefaultBehavior (Collidable.Deformable, Collidable.Self, true, 2);
      
      verify (". . . 1 . . . . . "+  // surf1
              "  . 2 . . . . . . "+  // sub11
              "    . . . . . . . "+  // sub12
              "      . . . . . . "+  // surf2
              "        . 2 . . . "+  // sub21
              "          . . . . "); // sub22
      setDefaultBehavior (Collidable.Deformable, Collidable.Rigid, true, 3);

      verify (". . . 1 . . 3 3 3 "+  // surf1
              "  . 2 . . . . . . "+  // sub11
              "    . . . . . . . "+  // sub12
              "      . . . 3 3 3 "+  // surf2
              "        . 2 . . . "+  // sub21
              "          . . . . "+  // sub22
              "            . . . "+  // comp
              "              . . "+  // ball
              "                . "); // base

      setDefaultBehavior (Collidable.Rigid, Collidable.Rigid, true, 2);

      verify (". . . 1 . . 3 3 3 "+  // surf1
              "  . 2 . . . . . . "+  // sub11
              "    . . . . . . . "+  // sub12
              "      . . . 3 3 3 "+  // surf2
              "        . 2 . . . "+  // sub21
              "          . . . . "+  // sub22
              "            . 2 2 "+  // comp
              "              . 2 "+  // ball
              "                . "); // base
      setDefaultBehavior (Collidable.Deformable, Collidable.Self, false,0);
      setDefaultBehavior (Collidable.Deformable, Collidable.Rigid, false,0);
      setDefaultBehavior (Collidable.Rigid, Collidable.Rigid, false,0); 

      verify (". . . 1 . . . . . . . "+  // surf1
              "  . . . . . . . . . . "+  // sub11
              "    . . . . . . . . . "+  // sub12
              "      . . . . . . . . "+  // surf2
              "        . . . . . . . "+  // sub21
              "          . . . . . . "); // sub22

      setDefaultBehavior (Collidable.Deformable, Collidable.Self, true,2);
      setDefaultBehavior (Collidable.Deformable, Collidable.Rigid, true,3);
      setDefaultBehavior (Collidable.Rigid, Collidable.Rigid, true,2);
      verify (". . . 1 . . 3 3 3 "+  // surf1
              "  . 2 . . . . . . "+  // sub11
              "    . . . . . . . "+  // sub12
              "      . . . 3 3 3 "+  // surf2
              "        . 2 . . . "+  // sub21
              "          . . . . "+  // sub22
              "            . 2 2 "+  // comp
              "              . 2 "+  // ball
              "                . "); // base
      
      setBehavior (ball, base, true, 4);
      setBehavior (ball, fem1, true, 4);
      setBehavior (comp, fem2, true, 5);
      setBehavior (surf1, comp, true, 5);
      CollisionBehavior behav = setBehavior (surf2, sub22, true, 5);
      testSetBehaviorArgs (surf2, sub22, behav);

      // make sure resetting a behavior does not change the behavior list size
      int numb = cm.numBehaviors();
      setBehavior (sub22, surf2, true, 5);
      azzert ("behavior count changed", numb==cm.numBehaviors());      

      verify (". . . 1 . . 5 4 3 "+  // surf1
              "  . 2 . . . . . . "+  // sub11
              "    . . . . . . . "+  // sub12
              "      . . . 5 3 3 "+  // surf2
              "        . 2 . . . "+  // sub21
              "          . . . . "+  // sub22
              "            . 2 2 "+  // comp
              "              . 4 "+  // ball
              "                . "); // base

      clearBehavior (ball, base);
      clearBehavior (ball, fem1);
      setBehavior (fem1, base, true, 4);
      setBehavior (sub11, sub12, false, 0);
      setBehavior (fem2, base, false, 0);
      setBehavior (fem2, fem2, true, 7);

      verify (". . . 1 . . 5 3 4 "+  // surf1
              "  . . . . . . . . "+  // sub11
              "    . . . . . . . "+  // sub12
              "      . . . 5 3 . "+  // surf2
              "        . 7 . . . "+  // sub21
              "          . . . . "+  // sub22
              "            . 2 2 "+  // comp
              "              . 2 "+  // ball
              "                . "); // base

      clearBehaviors();
      verify (". . . 1 . . 3 3 3 "+  // surf1
              "  . 2 . . . . . . "+  // sub11
              "    . . . . . . . "+  // sub12
              "      . . . 3 3 3 "+  // surf2
              "        . 2 . . . "+  // sub21
              "          . . . . "+  // sub22
              "            . 2 2 "+  // comp
              "              . 2 "+  // ball
              "                . "); // base

      setDefaultBehavior (Collidable.All, Collidable.All, false, 0);
      setBehavior (sub11, Collidable.All, true, 1);
      setBehavior (comp, Collidable.Rigid, true, 2);
      setBehavior (fem2, Collidable.Self, true, 3);
      numb = cm.numBehaviors();
      setBehavior (fem2, fem2, true, 3);
      // make sure fem2, fem2 maps onto fem2, Self
      azzert ("behavior count changed", numb==cm.numBehaviors()); 
      setBehavior (surf1, surf2, true, 5);

      verify (". . . 5 . . . . . "+  // surf1
              "  . . . . . . . . "+  // sub11
              "    . . . . . . . "+  // sub12
              "      . . . . . . "+  // surf2
              "        . 3 . . . "+  // sub21
              "          . . . . "+  // sub22
              "            . 2 2 "+  // comp
              "              . . "+  // ball
              "                . "); // base

      clearBehaviors();
      sub11.setCollidable (Collidability.ALL);
      sub12.setCollidable (Collidability.ALL);
      setBehavior (fem1, Collidable.All, true, 0);
      setBehavior (comp, Collidable.Rigid, true, 2);
      //setBehavior (comp2, Collidable.AllBodies, true, 3);
      //setBehavior (comp2, surf2, true, 4);

      verify (". . . 0 . . 0 0 0 "+  // surf1
              "  . 0 0 . . 0 0 0 "+  // sub11
              "    . 0 . . 0 0 0 "+  // sub12
              "      . . . . . . "+  // surf2
              "        . . . . . "+  // sub21
              "          . . . . "+  // sub22
              "            . 2 2 "+  // comp
              "              . . "+  // ball
              "                . "); // base

      clearBehaviors();
      setBehavior (fem1, Collidable.AllBodies, true, 0);
      setBehavior (comp, Collidable.Deformable, true, 2);
      //setBehavior (comp2, Collidable.Deformable, true, 3);
      setBehavior (sub11, sub12, true, 4);

      verify (". . . 0 . . 2 0 0 "+  // surf1
              "  . 4 0 . . 2 0 0 "+  // sub11
              "    . 0 . . 2 0 0 "+  // sub12
              "      . . . 2 . . "+  // surf2
              "        . . . . . "+  // sub21
              "          . . . . "+  // sub22
              "            . . . "+  // comp
              "              . . "+  // ball
              "                . "); // base

      sub21.setCollidable (Collidability.ALL);
      sub22.setCollidable (Collidability.ALL);
      updateBehaviorMap();

      verify (". . . 0 0 0 2 0 0 "+  // surf1
              "  . 4 0 0 0 2 0 0 "+  // sub11
              "    . 0 0 0 2 0 0 "+  // sub12
              "      . . . 2 . . "+  // surf2
              "        . . 2 . . "+  // sub21
              "          . 2 . . "+  // sub22
              "            . . . "+  // comp
              "              . . "+  // ball
              "                . "); // base

      clearBehaviors();
      setBehavior (fem1, Collidable.Rigid, true, 0);
      setBehavior (fem2, Collidable.Deformable, true, 2);
      setBehavior (comp, Collidable.AllBodies, true, 3);
      setBehavior (base, Collidable.AllBodies, true, 7);

      verify (". . . 2 2 2 3 0 7 "+  // surf1
              "  . . 2 2 2 3 0 7 "+  // sub11
              "    . 2 2 2 3 0 7 "+  // sub12
              "      . . . 3 . 7 "+  // surf2
              "        . . 3 . 7 "+  // sub21
              "          . 3 . 7 "+  // sub22
              "            . 3 7 "+  // comp
              "              . 7 "+  // ball
              "                . "); // base

      cb = setBehavior (comp, Collidable.All, true, 3);
      setBehavior (base, Collidable.All, true, 7);
      azzert ("unexpected handler", getActingBehavior(fem1,comp) == cb);
      azzert ("unexpected handler", getActingBehavior(fem2,comp) == cb);

      verify (". . . 2 2 2 3 0 7 "+  // surf1
              "  . . 2 2 2 3 0 7 "+  // sub11
              "    . 2 2 2 3 0 7 "+  // sub12
              "      . . . 3 . 7 "+  // surf2
              "        . . 3 . 7 "+  // sub21
              "          . 3 . 7 "+  // sub22
              "            . 3 7 "+  // comp
              "              . 7 "+  // ball
              "                . "); // base

      setBehavior (comp, fem1, true, 8);
      cb = setBehavior (fem1, fem1, true, 5);
      azzert ("unexpected handler", getActingBehavior(fem1,fem1) == cb);

      verify (". . . 2 2 2 8 0 7 "+  // surf1
              "  . 5 2 2 2 8 0 7 "+  // sub11
              "    . 2 2 2 8 0 7 "+  // sub12
              "      . . . 3 . 7 "+  // surf2
              "        . . 3 . 7 "+  // sub21
              "          . 3 . 7 "+  // sub22
              "            . 3 7 "+  // comp
              "              . 7 "+  // ball
              "                . "); // base

      clearBehaviors();
      setBehavior (surf1, Collidable.Rigid, true, 1);
      setBehavior (sub11, Collidable.Deformable, true, 2);
      setBehavior (sub12, Collidable.AllBodies, true, 3);
      setBehavior (surf2, Collidable.All, true, 4);
      setBehavior (base, Collidable.Deformable, true, 5);
      //cb = setBehavior (comp2, Collidable.Rigid, true, 6);
      //setBehavior (comp1, Collidable.All, true, 7);

      //azzert ("unexpected handler", getActingBehavior(comp2,ball) == cb);
      azzert ("expected null behavior", getActingBehavior(fem1,fem2) == null);

      cb = myMech.getCollisionBehavior (base, Collidable.Deformable);
      azzert ("unexpected handler", getActingBehavior(base,fem1) == cb);
      azzert ("unexpected handler", getActingBehavior(fem2,base) == cb);

      verify (". . . 4 . . 1 1 5 "+  // surf1
              "  . . 4 2 2 . . 5 "+  // sub11
              "    . 4 3 3 3 3 5 "+  // sub12
              "      . . . 4 4 5 "+  // surf2
              "        . . . . 5 "+  // sub21
              "          . . . 5 "+  // sub22
              "            . . . "+  // comp
              "              . . "+  // ball
              "                . "); // base

      setBehavior (base, Collidable.Rigid, true, 8);

      verify (". . . 4 . . 1 1 5 "+  // surf1
              "  . . 4 2 2 . . 5 "+  // sub11
              "    . 4 3 3 3 3 5 "+  // sub12
              "      . . . 4 4 5 "+  // surf2
              "        . . . . 5 "+  // sub21
              "          . . . 5 "+  // sub22
              "            . . 8 "+  // comp
              "              . 8 "+  // ball
              "                . "); // base

      setBehavior (comp, surf2, true, 0);
      setBehavior (surf1, fem2, true, 9);

      verify (". . . 9 9 9 1 1 5 "+  // surf1
              "  . . 4 2 2 . . 5 "+  // sub11
              "    . 4 3 3 3 3 5 "+  // sub12
              "      . . . 0 4 5 "+  // surf2
              "        . . . . 5 "+  // sub21
              "          . . . 5 "+  // sub22
              "            . . 8 "+  // comp
              "              . 8 "+  // ball
              "                . "); // base

      setBehavior (sub12, Collidable.AllBodies, true, 3);

      verify (". . . 9 9 9 1 1 5 "+  // surf1
              "  . . 4 2 2 . . 5 "+  // sub11
              "    . 3 3 3 3 3 3 "+  // sub12
              "      . . . 0 4 5 "+  // surf2
              "        . . . . 5 "+  // sub21
              "          . . . 5 "+  // sub22
              "            . . 8 "+  // comp
              "              . 8 "+  // ball
              "                . "); // base

      sub21.setCollidable (Collidability.INTERNAL);
      sub22.setCollidable (Collidability.INTERNAL);

      clearBehaviors();
      setDefaultBehavior (
         Collidable.Deformable, Collidable.Deformable, true, 1);
      setDefaultBehavior (Collidable.Deformable, Collidable.Self, true,2);
      setDefaultBehavior (Collidable.Deformable, Collidable.Rigid, true,3);
      setDefaultBehavior (Collidable.Rigid, Collidable.Rigid, true,2);

      mySubMech = createSubModel("sub");
      myMech.addModel (mySubMech);
      updateBehaviorMap();
      checkBehaviors();

      FemModel3d fem3 =
         (FemModel3d)myMech.findComponent ("models/sub/models/fem3");

      Collidable surf3 = (Collidable)fem3.findComponent ("meshes/surface");
      FemMeshComp sub31 = (FemMeshComp)fem3.findComponent ("meshes/sub1");
      FemMeshComp sub32 = (FemMeshComp)fem3.findComponent ("meshes/sub2");
      Collidable ball1 = (Collidable)mySubMech.findComponent("rigidBodies/ball1");
      Collidable ball2 = (Collidable)mySubMech.findComponent("rigidBodies/ball2");

      // reset Collidability of FEM sub meshes so that they
      // can collide with anything

      sub11.setCollidable (Collidability.ALL);
      sub12.setCollidable (Collidability.ALL);
      sub21.setCollidable (Collidability.ALL);
      sub22.setCollidable (Collidability.ALL);
      runChecks();
      
      testSetBehaviorArgs (ball1, ball2);

      clearResponses();
      setResponses (myMech, new Collidable[] {
            fem1, fem2, fem3, comp,
            surf1, sub11, sub12, surf2, sub21, sub22,
            surf3, sub31, sub32, ball1, ball2, ball, base
         });

      setResponses (mySubMech, new Collidable[] {
            fem3, surf3, sub31, sub32, ball1, ball2});

      verify (". . . 1 1 1 1 . . 3 3 3 3 3 "+ // surf1
              "  . 2 1 1 1 1 . . 3 3 3 3 3 "+ // sub11
              "    . 1 1 1 1 . . 3 3 3 3 3 "+ // sub12
              "      . . . 1 . . 3 3 3 3 3 "+ // surf2 
              "        . 2 1 . . 3 3 3 3 3 "+ // sub21
              "          . 1 . . 3 3 3 3 3 "+ // sub22
              "            . . . . . 3 3 3 "+ // surf3
              "              . . . . . . . "+ // sub31
              "                . . . . . . "+ // sub32
              "                  . . 2 2 2 "+ // ball1
              "                    . 2 2 2 "+ // ball2
              "                      . 2 2 "+ // comp
              "                        . 2 "+ // ball
              "                          . ");// base

      setBehavior (mySubMech, fem3, fem3, true, 7);
      setDefaultBehavior (
         mySubMech, Collidable.Deformable, Collidable.Rigid, true, 2);
      setBehavior (mySubMech, ball1, ball2, true, 6);
      setBehavior (mySubMech, surf3, ball1, true, 8);
      setBehavior (ball, sub31, true, 7);

      verify (". . . 1 1 1 1 . . 3 3 3 3 3 "+ // surf1
              "  . 2 1 1 1 1 . . 3 3 3 3 3 "+ // sub11
              "    . 1 1 1 1 . . 3 3 3 3 3 "+ // sub12
              "      . . . 1 . . 3 3 3 3 3 "+ // surf2 
              "        . 2 1 . . 3 3 3 3 3 "+ // sub21
              "          . 1 . . 3 3 3 3 3 "+ // sub22
              "            . . . 8 2 3 3 3 "+ // surf3
              "              . 7 . . . . . "+ // sub31
              "                . . . . . . "+ // sub32
              "                  . 6 2 2 2 "+ // ball1
              "                    . 2 2 2 "+ // ball2
              "                      . 2 2 "+ // comp
              "                        . 2 "+ // ball
              "                          . ");// base

      clearBehavior (mySubMech, ball1, surf3);
      clearBehavior (mySubMech, ball1, ball2);
      setBehavior (mySubMech, ball1, Collidable.All, true, 0);
      setBehavior (ball1, Collidable.All, true, 9);

      verify (". . . 1 1 1 1 . . 9 3 3 3 3 "+ // surf1
              "  . 2 1 1 1 1 . . 9 3 3 3 3 "+ // sub11
              "    . 1 1 1 1 . . 9 3 3 3 3 "+ // sub12
              "      . . . 1 . . 9 3 3 3 3 "+ // surf2 
              "        . 2 1 . . 9 3 3 3 3 "+ // sub21
              "          . 1 . . 9 3 3 3 3 "+ // sub22
              "            . . . 0 2 3 3 3 "+ // surf3
              "              . 7 . . . . . "+ // sub31
              "                . . . . . . "+ // sub32
              "                  . 0 9 9 9 "+ // ball1
              "                    . 2 2 2 "+ // ball2
              "                      . 2 2 "+ // comp
              "                        . 2 "+ // ball
              "                          . ");// base

      sub31.setCollidable (Collidability.ALL);
      sub32.setCollidable (Collidability.ALL);
      runChecks();

      setBehavior (mySubMech, fem3, fem3, true, 5);

      verify (". . . 1 1 1 1 1 1 9 3 3 3 3 "+ // surf1
              "  . 2 1 1 1 1 1 1 9 3 3 3 3 "+ // sub11
              "    . 1 1 1 1 1 1 9 3 3 3 3 "+ // sub12
              "      . . . 1 1 1 9 3 3 3 3 "+ // surf2 
              "        . 2 1 1 1 9 3 3 3 3 "+ // sub21
              "          . 1 1 1 9 3 3 3 3 "+ // sub22
              "            . . . 0 2 3 3 3 "+ // surf3 
              "              . 5 0 2 3 7 3 "+ // sub31 
              "                . 0 2 3 3 3 "+ // sub32 
              "                  . 0 9 9 9 "+ // ball1 
              "                    . 2 2 2 "+ // ball2 
              "                      . 2 2 "+ // comp
              "                        . 2 "+ // ball
              "                          . ");// base

   }

   public static void main (String[] args) {
      CollisionManagerTest tester = new CollisionManagerTest(); 
      tester.runtest();
   }
}
