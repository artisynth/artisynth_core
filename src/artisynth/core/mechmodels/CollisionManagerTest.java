package artisynth.core.mechmodels;

import java.util.*;

import maspack.util.*;
import maspack.matrix.*;
import maspack.geometry.*;
import maspack.render.*;

import artisynth.core.mechmodels.*;
import artisynth.core.mechmodels.Collidable.Collidability;
import artisynth.core.modelbase.*;
import artisynth.core.femmodels.*;
import artisynth.core.workspace.*;

/**
 * Test jig for the CollisionManager. Note that this does *not* test actual
 * collision handling; it just tests the CollisionManager's management
 * of what components collide with what.
 */
public class CollisionManagerTest extends UnitTest {

   MechModel myMech;
   MechModel mySubMech;
   HashMap<CollidablePair,CollisionBehavior> myDefaults =
      new HashMap<CollidablePair,CollisionBehavior>();
   HashMap<CollidablePair,CollisionBehavior> myBehaviorMap =
      new HashMap<CollidablePair,CollisionBehavior>();
   LinkedHashMap<CollidablePair,CollisionBehavior> myOverrides =
      new LinkedHashMap<CollidablePair,CollisionBehavior>();      

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
      fem.addMesh (FemMesh.createSurface (name, fem, elems));
   }

   RigidCompositeBody createCompositeBody (String name, double rad) {
      
      // create and add the composite body and plate
      PolygonalMesh ball1 = MeshFactory.createIcosahedralSphere (rad, 1);
      ball1.transform (new RigidTransform3d (2*rad, 0, 0));
      PolygonalMesh ball2 = MeshFactory.createIcosahedralSphere (rad, 1);
      ball2.transform (new RigidTransform3d (-2*rad, 0, 0));
      PolygonalMesh axis = MeshFactory.createCylinder (rad/4, 2.5*rad, 12);
      axis.transform (new RigidTransform3d (0, 0, 0, 0, Math.PI/2, 0));

      RigidCompositeBody body = new RigidCompositeBody (name);
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
      RenderProps.setVisible (fem1.getSurfaceFemMesh(), false);
      fem1.transformGeometry (new RigidTransform3d (0, 0, 1));
      addSubMesh (fem1, "sub1", new int[] { 5, 6, 9, 10 });
      addSubMesh (fem1, "sub2", new int[] { 53, 54, 57, 58 });
      FemModel3d fem2 = FemFactory.createHexGrid (null, 1, 1, 1, 4, 4, 4);
      fem1.setSurfaceRendering (FemModel3d.SurfaceRender.Shaded);
      fem2.setName ("fem2");
      RenderProps.setVisible (fem2.getSurfaceFemMesh(), false);
      fem2.transformGeometry (new RigidTransform3d (-2, 0, 1));
      addSubMesh (fem2, "sub1", new int[] { 5, 6, 9, 10 });
      addSubMesh (fem2, "sub2", new int[] { 53, 54, 57, 58 });

      RigidCompositeBody bod = createCompositeBody ("comp", 0.3);
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
      RenderProps.setVisible (fem3.getSurfaceFemMesh(), false);
      fem3.transformGeometry (new RigidTransform3d (0, 0, 1));
      addSubMesh (fem3, "sub1", new int[] { 5, 6, 9, 10 });
      addSubMesh (fem3, "sub2", new int[] { 53, 54, 57, 58 });

      mech.addRigidBody (ball1);
      mech.addRigidBody (ball2);
      mech.addModel (fem3);
      return mech;
   }

   boolean isBody (ModelComponent c) {
      return (c instanceof CollidableBody &&
              // XXX hack until RigidBody and RigidCompositeBody are merged
              !(c instanceof RigidCompositeBody));
   }

   ArrayList<CollidableBody> getAllBodies (MechModel mech) {
      ArrayList<Collidable> allCollidables = new ArrayList<Collidable>(100);
      ArrayList<CollidableBody> list = new ArrayList<CollidableBody>(100);
      mech.getCollidables (allCollidables, /*level=*/0);
      // return only collidable bodies
      for (Collidable c : allCollidables) {
         if (isBody(c)) {
            list.add ((CollidableBody)c);
         }
      }
      return list;
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


   private CollidablePair getDefaultPair (Collidable a, Collidable b) {
      if (a.isDeformable() && b.isDeformable()) {
         return CollisionManager.DEFORMABLE_DEFORMABLE;
      }
      else if (a.isDeformable() || b.isDeformable()) {
         return CollisionManager.DEFORMABLE_RIGID;
      }
      else {
         return CollisionManager.RIGID_RIGID;
      }
   }


   MechModel lowestCommonModel (ModelComponent a, ModelComponent b) {
      ModelComponent ancestor = ComponentUtils.findCommonAncestor(a, b);
      while (ancestor != null) {
         if (ancestor instanceof MechModel) {
            return (MechModel)ancestor;
         }
         ancestor = ancestor.getParent();
      }
      return null;
   }

   MechModel nearestMechAncestor (ModelComponent a) {
      ModelComponent ancestor = a.getParent();
      while (ancestor != null) {
         if (ancestor instanceof MechModel) {
            return (MechModel)ancestor;
         }
         ancestor = ancestor.getParent();
      }
      return null;
   }

   boolean hasCollidableAncestor (Collidable a, Collidable b) {
      ModelComponent comp = ComponentUtils.findCommonAncestor(a, b);
      while (comp != null) {
         if (comp instanceof Collidable) {
            return true;
         }
         comp = comp.getParent();
      }
      return false;
   }

   CollisionBehavior getPrimaryBehavior (CollidableBody  a, CollidableBody b) {

      MechModel mech = lowestCommonModel (a, b);
      if (mech == myMech) {
         return myDefaults.get (getDefaultPair (a, b));
      }
      else {
         return mech.getCollisionBehavior (a, b);
      }
   }

   private void setBehavior (
      CollidableBody a, CollidableBody b, CollisionBehavior behavior) {
      CollidablePair pair = new CollidablePair (a, b);
      if (behavior == null || !behavior.isEnabled()) {
         myBehaviorMap.remove (pair);
      }
      else {
         myBehaviorMap.put (pair, behavior);
      }
   }

   boolean isInternallyCollidable (Collidable c) {
      return (c.getCollidable() == Collidability.ALL ||
              c.getCollidable() == Collidability.INTERNAL);
   }
 
   boolean isExternallyCollidable (Collidable c) {
      return (c.getCollidable() == Collidability.ALL ||
              c.getCollidable() == Collidability.EXTERNAL);
   }

  private void applyBehavior (
      Collidable a, Collidable b, CollisionBehavior behavior) {
      if (isBody(a) && isBody(b)) {
         if (hasCollidableAncestor(a, b)) {
            if (isInternallyCollidable(a) && isInternallyCollidable(b)) {
               setBehavior ((CollidableBody)a, (CollidableBody)b, behavior);
            }
         }
         else {
            if (isExternallyCollidable(a) && isExternallyCollidable(b)) {
               setBehavior ((CollidableBody)a, (CollidableBody)b, behavior);
            }
         }
      }
      else if (a == b) {
         ArrayList<CollidableBody> bodies = getSubBodies (a);
         for (int i=0; i<bodies.size(); i++) {
            CollidableBody ai = bodies.get(i);
            for (int j=i+1; j<bodies.size(); j++) {
               CollidableBody aj = bodies.get(j);
               if (isInternallyCollidable(ai) && isInternallyCollidable(aj)) {
                  setBehavior (ai, aj, behavior);
               }
               else {
                  setBehavior (ai, aj, new CollisionBehavior());
               }
            }
         }
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
         for (int i=0; i<bodiesA.size(); i++) {
            CollidableBody ai = bodiesA.get(i);
            if (isExternallyCollidable (ai)) {
               for (int j=0; j<bodiesB.size(); j++) {
                  CollidableBody bj = bodiesB.get(j);
                  if (isExternallyCollidable (bj)) {
                     setBehavior (ai, bj, behavior);
                  }
               }
            }
         }
      }
   }

   private void updateBehaviorMap() {
      myBehaviorMap.clear();

      ArrayList<Collidable> collidables = new ArrayList<Collidable>();
      myMech.getCollidables(collidables, 0);
      for (int i=0; i<collidables.size(); i++) {
         Collidable ci = collidables.get(i);
         if (isBody(ci)) {
            for (int j=0; j<collidables.size(); j++) {
               Collidable cj = collidables.get(j);
               if (isBody(cj)) {
                  CollidableBody bi = (CollidableBody)ci;
                  CollidableBody bj = (CollidableBody)cj;
                  if (!hasCollidableAncestor (bi, bj) &&
                      isExternallyCollidable (bi) &&
                      isExternallyCollidable (bj)) {
                     CollisionBehavior behavior = getPrimaryBehavior (bi, bj);
                     setBehavior (bi, bj, behavior);
                  }
               }
            }
         }
         else {
            if (nearestMechAncestor(ci) == myMech &&
                ci.isDeformable()) {
               CollisionBehavior behav =
                  myDefaults.get(CollisionManager.DEFORMABLE_SELF);
               applyBehavior (ci, ci, behav);
            }
         }
      }
      for (Map.Entry<CollidablePair,CollisionBehavior> entry :
              myOverrides.entrySet()) {
         CollidablePair pair = entry.getKey();
         applyBehavior (pair.myCompA, pair.myCompB, entry.getValue());
      }
   }

   private boolean equal (CollisionBehavior b0, CollisionBehavior b1) {
      if (b0 == null && b1 == null) {
         return true;
      }
      else if ((b0 == null && b1 != null) || (b0 != null && b1 == null)) {
         return false;
      }
      else {
         return b0.equals (b1);
      }
   }

   CollisionBehavior getMapBehavior ( CollidableBody a, CollidableBody b) {
      return getMapBehavior (myBehaviorMap, a, b);
   }

   CollisionBehavior getMapBehavior (
      HashMap<CollidablePair,CollisionBehavior> map,
      CollidableBody a, CollidableBody b) {

      CollisionBehavior behavior = map.get (new CollidablePair (a, b));
      if (behavior == null) {
         return new CollisionBehavior();
      }
      else {
         return behavior;
      }
   }


   private CollisionBehavior getBehavior (CollidablePair pair) {
      Collidable a = pair.myCompA;
      Collidable b = pair.myCompB;

      if (a == b) {
         if (isBody(a)) {
            return new CollisionBehavior (false, 0);
         }
         else {
            ArrayList<CollidableBody> bodies = getSubBodies (a);
            CollisionBehavior behavior0 = null;
            for (int i=0; i<bodies.size(); i++) {
               CollidableBody ai = bodies.get(i);
               for (int j=i+1; j<bodies.size(); j++) {
                  CollidableBody aj = bodies.get(j);
                  if (isInternallyCollidable(ai) && isInternallyCollidable(aj)) {
                     CollisionBehavior behavior = getMapBehavior (ai, aj);
                     if (behavior0 == null) {
                        behavior0 = behavior;
                     }
                     else {
                        if (!behavior.equals(behavior0)) {
                           return null;
                        }
                     }
                  }
               }
            }
            return behavior0;
         }
      }
      else if (isBody(a) && isBody(b)) {
         CollisionBehavior behav = myBehaviorMap.get(pair);
         if (behav == null) {
            return new CollisionBehavior();
         }
         else {
            return behav;
         }
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
         CollisionBehavior behavior0 = null;
         for (int i=0; i<bodiesA.size(); i++) {
            CollidableBody bi = bodiesA.get(i);
            if (isExternallyCollidable (bi)) {
               for (int j=0; j<bodiesB.size(); j++) {
                  CollidableBody bj = bodiesB.get(j);
                  if (isExternallyCollidable (bj)) {
                     CollisionBehavior behavior = getMapBehavior (bi, bj);
                     if (behavior0 == null) {
                        behavior0 = behavior;
                     }
                     else {
                        if (!behavior.equals(behavior0)) {
                           return null;
                        }
                     }
                  }
               }
            }
         }
         if (behavior0 == null) {
            return new CollisionBehavior();
         }
         else {
            return behavior0;
         }
      }
   }

   private void checkBehaviors() {
      ArrayList<Collidable> collidables = new ArrayList<Collidable>();
      myMech.getCollidables(collidables, 0);
      for (int i=0; i<collidables.size(); i++) {
         Collidable ai = collidables.get(i);
         for (int j=0; j<collidables.size(); j++) {
            Collidable bj = collidables.get(j);
            CollidablePair pair = new CollidablePair (ai, bj);
            CollisionBehavior behav = myMech.getCollisionBehavior (ai, bj);
            CollisionBehavior check = getBehavior (pair);
            if (!equal (check, behav)) {
               throw new TestException (
                  "getBehavior for "+pair+":\n"+
                  "expected " + check + " got " + behav);
            }
         }
      }
   }

   private void printName (String name, int maxlen) {
      System.out.print (name);
      int res = maxlen-name.length()+1;
      for (int i=0; i<res; i++) {
         System.out.print (" ");
      }
   }

   void printMechMap () {
      printMap (myMech);
   }

   void printTestMap () {
      printMap (myBehaviorMap);
   }

   void printMap (Object obj) {
      
      MechModel mech = myMech;
      if (obj instanceof MechModel) {
         mech = (MechModel)obj;
      }
      ArrayList<CollidableBody> bodies = getAllBodies (mech);

      int maxlen = 0;
      for (int i=0; i<bodies.size(); i++) {
         int len = ComponentUtils.getPathName (bodies.get(i)).length();
         if (len > maxlen) {
            maxlen = len;
         }
      }
      for (int i=0; i<bodies.size(); i++) {
         CollidableBody ci = bodies.get(i);
         printName (ComponentUtils.getPathName (ci), maxlen);
         for (int j=0; j<bodies.size(); j++) {
            CollidableBody cj = bodies.get(j);
            CollisionBehavior behavior;
            if (obj instanceof MechModel) {
               behavior = ((MechModel)obj).getCollisionBehavior (ci, cj);
            }
            else {
               HashMap<CollidablePair,CollisionBehavior> map =
                  (HashMap<CollidablePair,CollisionBehavior>)obj;
               behavior = getMapBehavior (map, ci, cj);
            }
            if (behavior.isEnabled()) {
               System.out.print (" " + (int)behavior.getFriction());
            }
            else {
               System.out.print (" .");
            }
         }
         System.out.println ("");
      }
   }

   public void setDefaultBehavior (
      CollidablePair pair, boolean enabled, double mu) {
      setDefaultBehavior (myMech, pair, enabled, mu);
   }

   public void setDefaultBehavior (
      MechModel mech, CollidablePair pair, boolean enabled, double mu) {

      CollisionBehavior behavior = new CollisionBehavior (enabled, mu);
      mech.setDefaultCollisionBehavior (
         pair.myCompA, pair.myCompB, enabled, mu);
      CollisionBehavior check = 
         mech.getDefaultCollisionBehavior(pair.myCompA, pair.myCompB);
      azzert ("getDefaultBehavior", check.equals (behavior));
      if (mech == myMech) {
         myDefaults.put (pair, behavior);
      }
      updateBehaviorMap();
      testMap (myBehaviorMap);
      checkBehaviors();
      testMap (mech.getCollisionManager().myBehaviorMap);
   }

   public void setBehavior (
      Collidable a, Collidable b, boolean enabled, double mu) {
      setBehavior (myMech, a, b, enabled, mu);
   }

   public void setBehavior (
      MechModel mech, Collidable a, Collidable b, boolean enabled, double mu) {

      CollisionBehavior behavior = new CollisionBehavior (enabled, mu);
      // see if we can actually set the behavior for this collidable pair
      boolean settable = true;
      if (hasCollidableAncestor (a,b)) {
         // then a and b need to be internally collidable
         if (!isInternallyCollidable (a) || !isInternallyCollidable (b)) {
            settable = false;
         }
      }
      else {
         // then a and b need to be externally collidable
         if (!isExternallyCollidable (a) || !isExternallyCollidable (b)) {
            settable = false;
         }
      }
      CollisionBehavior prev = mech.getCollisionBehavior(a, b);
      mech.setCollisionBehavior (a, b, enabled, mu);
      CollisionBehavior check = mech.getCollisionBehavior(a, b);
      if (settable) {
         azzert ("getBehavior", check.equals(behavior));
      }
      else {
         azzert ("getBehavior", equal (prev, check));
      }
      if (mech == myMech) {
         myOverrides.put (new CollidablePair(a, b), behavior);
      }
      updateBehaviorMap();
      testMap (myBehaviorMap);
      checkBehaviors();
      testMap (mech.getCollisionManager().myBehaviorMap);
   }

   public void clearBehavior (Collidable a, Collidable b) {
      clearBehavior (myMech, a, b);
   }

   public void clearBehavior (MechModel mech, Collidable a, Collidable b) {

      mech.clearCollisionBehavior (a, b);
      myOverrides.remove (new CollidablePair(a, b));
      updateBehaviorMap();
      testMap (myBehaviorMap);
      checkBehaviors();
      testMap (mech.getCollisionManager().myBehaviorMap);
   }

   public void clearBehaviors () {
      clearBehaviors (myMech);
   }

   public void clearBehaviors (MechModel mech) {

      mech.clearCollisionBehaviors();
      myOverrides.clear();
      updateBehaviorMap();
      testMap (myBehaviorMap);
      checkBehaviors();
      testMap (myMech.getCollisionManager().myBehaviorMap);
   }

   private String getName (Collidable c) {
      if (CollidablePair.isGeneric(c)) {
         return c.toString();
      }
      else {
         return ComponentUtils.getPathName(c);
      }
   }

   private String getName (CollidablePair pair) {
      return getName(pair.myCompA) + "-" + getName(pair.myCompB);
   }      

   // Make sure behavior map is well constructed - no entries with null or
   // behaviors with isEnabled() == false, and that the CollidablePair query
   // works properly.
   public void testMap (HashMap<CollidablePair,CollisionBehavior> map) {
      for (Map.Entry<CollidablePair,CollisionBehavior> entry : map.entrySet()) {
         CollidablePair pair = entry.getKey();
         if (entry.getValue() == null) {
            throw new TestException ("Value for "+getName(pair) + " is null");
         }
         else if (!entry.getValue().isEnabled()) {
            throw new TestException ("Value for "+getName(pair) + " is false");
         }
         CollisionBehavior behavior =
            map.get(new CollidablePair (pair.myCompA, pair.myCompB));
         if (behavior == null) {
            throw new TestException ("Query for "+getName(pair) + " is null");
         }
         else if (!behavior.equals(entry.getValue())) {
            throw new TestException ("Query for "+getName(pair) + " is unequal");
         }
         CollisionBehavior transposed =
            map.get(new CollidablePair (pair.myCompB, pair.myCompA));
         if (behavior != transposed) {
            throw new TestException (
               "Query for "+getName(pair) + " not tranpose invariant");
         }
      }
   }

   private CollisionBehavior getBehaviorFromString (String str, int idx) {
      if (idx >= str.length()) {
         return new CollisionBehavior (false, 0);
      }
      else {
         int c = str.charAt(idx);
         if (c == '.') {
            return new CollisionBehavior (false, 0);
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

   private void verify (String str) {
      ArrayList<CollidableBody> bodies = getAllBodies (myMech);
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
            if (!check.equals (behavior)) {
               throw new TestException (
                  "Unexpected behavior for "+
                  new CollidablePair(ci, cj)+": "+behavior+", expected "+check);
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

   CollisionBehavior getBehavior (String name0, String name1) {
      Collidable col0 = findCollidable(name0);
      Collidable col1 = findCollidable(name1);
      return myMech.getCollisionBehavior (col0, col1);
   }

   @Override
   public void test() {
      myMech = createMechModel ("top");
      setDefaultBehavior (CollisionManager.DEFORMABLE_DEFORMABLE, true, 1);

      //top/models/fem1/meshes/surface  . . . 1 1 1 . . . . .
      //top/models/fem1/meshes/sub1     . . . 1 1 1 . . . . .
      //top/models/fem1/meshes/sub2     . . . 1 1 1 . . . . .
      //top/models/fem2/meshes/surface  1 1 1 . . . . . . . .
      //top/models/fem2/meshes/sub1     1 1 1 . . . . . . . .
      //top/models/fem2/meshes/sub2     1 1 1 . . . . . . . .
      //top/rigidBodies/comp/meshes/0   . . . . . . . . . . .
      //top/rigidBodies/comp/meshes/1   . . . . . . . . . . .
      //top/rigidBodies/comp/meshes/2   . . . . . . . . . . .
      //top/rigidBodies/ball            . . . . . . . . . . .
      //top/rigidBodies/base            . . . . . . . . . . .

      Collidable fem1 = findCollidable ("models/fem1");
      Collidable fem2 = findCollidable ("models/fem2");
      Collidable comp = findCollidable ("rigidBodies/comp");

      Collidable surf1 = findCollidable ("models/fem1/meshes/surface");
      FemMesh    sub11 = (FemMesh)findCollidable ("models/fem1/meshes/sub1");
      FemMesh    sub12 = (FemMesh)findCollidable ("models/fem1/meshes/sub2");
      Collidable surf2 = findCollidable ("models/fem2/meshes/surface");
      FemMesh    sub21 = (FemMesh)findCollidable ("models/fem2/meshes/sub1");
      FemMesh    sub22 = (FemMesh)findCollidable ("models/fem2/meshes/sub2");
      Collidable comp0 = findCollidable ("rigidBodies/comp/meshes/0");
      Collidable comp1 = findCollidable ("rigidBodies/comp/meshes/1");
      Collidable comp2 = findCollidable ("rigidBodies/comp/meshes/2");
      Collidable ball = findCollidable ("rigidBodies/ball");
      Collidable base = findCollidable ("rigidBodies/base");

      verify (". . . 1 . . . . . . . "+  // surf1
              "  . . . . . . . . . . "+  // sub11
              "    . . . . . . . . . "+  // sub12
              "      . . . . . . . . "+  // surf2
              "        . . . . . . . "+  // sub21
              "          . . . . . . "); // sub22
      setDefaultBehavior (CollisionManager.DEFORMABLE_SELF, true, 2);

      verify (". . . 1 . . . . . . . "+  // surf1
              "  . 2 . . . . . . . . "+  // sub11
              "    . . . . . . . . . "+  // sub12
              "      . . . . . . . . "+  // surf2
              "        . 2 . . . . . "+  // sub21
              "          . . . . . . "); // sub22
      setDefaultBehavior (CollisionManager.DEFORMABLE_RIGID, true, 3);

      verify (". . . 1 . . 3 3 3 3 3 "+  // surf1
              "  . 2 . . . . . . . . "+  // sub11
              "    . . . . . . . . . "+  // sub12
              "      . . . 3 3 3 3 3 "+  // surf2
              "        . 2 . . . . . "+  // sub21
              "          . . . . . . "+  // sub22
              "            . . . . . "+  // comp0
              "              . . . . "+  // comp1
              "                . . . "+  // comp2
              "                  . . "+  // ball
              "                    . "); // base

      setDefaultBehavior (CollisionManager.RIGID_RIGID, true, 2);

      verify (". . . 1 . . 3 3 3 3 3 "+  // surf1
              "  . 2 . . . . . . . . "+  // sub11
              "    . . . . . . . . . "+  // sub12
              "      . . . 3 3 3 3 3 "+  // surf2
              "        . 2 . . . . . "+  // sub21
              "          . . . . . . "+  // sub22
              "            . . . 2 2 "+  // comp0
              "              . . 2 2 "+  // comp1
              "                . 2 2 "+  // comp2
              "                  . 2 "+  // ball
              "                    . "); // base
      setDefaultBehavior (CollisionManager.DEFORMABLE_SELF, false,0);
      setDefaultBehavior (CollisionManager.DEFORMABLE_RIGID, false,0);
      setDefaultBehavior (CollisionManager.RIGID_RIGID, false,0); 

      verify (". . . 1 . . . . . . . "+  // surf1
              "  . . . . . . . . . . "+  // sub11
              "    . . . . . . . . . "+  // sub12
              "      . . . . . . . . "+  // surf2
              "        . . . . . . . "+  // sub21
              "          . . . . . . "); // sub22

      setDefaultBehavior (CollisionManager.DEFORMABLE_SELF, true,2);
      setDefaultBehavior (CollisionManager.DEFORMABLE_RIGID, true,3);
      setDefaultBehavior (CollisionManager.RIGID_RIGID, true,2);
      verify (". . . 1 . . 3 3 3 3 3 "+  // surf1
              "  . 2 . . . . . . . . "+  // sub11
              "    . . . . . . . . . "+  // sub12
              "      . . . 3 3 3 3 3 "+  // surf2
              "        . 2 . . . . . "+  // sub21
              "          . . . . . . "+  // sub22
              "            . . . 2 2 "+  // comp0
              "              . . 2 2 "+  // comp1
              "                . 2 2 "+  // comp2
              "                  . 2 "+  // ball
              "                    . "); // base
      
      setBehavior (ball, base, true, 4);
      setBehavior (ball, fem1, true, 4);
      setBehavior (comp, fem2, true, 5);
      setBehavior (surf1, comp, true, 5);
      setBehavior (surf2, sub22, true, 5);

      verify (". . . 1 . . 5 5 5 4 3 "+  // surf1
              "  . 2 . . . . . . . . "+  // sub11
              "    . . . . . . . . . "+  // sub12
              "      . . . 5 5 5 3 3 "+  // surf2
              "        . 2 . . . . . "+  // sub21
              "          . . . . . . "+  // sub22
              "            . . . 2 2 "+  // comp0
              "              . . 2 2 "+  // comp1
              "                . 2 2 "+  // comp2
              "                  . 4 "+  // ball
              "                    . "); // base

      clearBehavior (ball, base);
      clearBehavior (ball, fem1);
      setBehavior (fem1, base, true, 4);
      setBehavior (sub11, sub12, false, 0);
      setBehavior (fem2, base, false, 0);
      setBehavior (fem2, fem2, true, 7);

      verify (". . . 1 . . 5 5 5 3 4 "+  // surf1
              "  . . . . . . . . . . "+  // sub11
              "    . . . . . . . . . "+  // sub12
              "      . . . 5 5 5 3 . "+  // surf2
              "        . 7 . . . . . "+  // sub21
              "          . . . . . . "+  // sub22
              "            . . . 2 2 "+  // comp0
              "              . . 2 2 "+  // comp1
              "                . 2 2 "+  // comp2
              "                  . 2 "+  // ball
              "                    . "); // base

      clearBehaviors();
      verify (". . . 1 . . 3 3 3 3 3 "+  // surf1
              "  . 2 . . . . . . . . "+  // sub11
              "    . . . . . . . . . "+  // sub12
              "      . . . 3 3 3 3 3 "+  // surf2
              "        . 2 . . . . . "+  // sub21
              "          . . . . . . "+  // sub22
              "            . . . 2 2 "+  // comp0
              "              . . 2 2 "+  // comp1
              "                . 2 2 "+  // comp2
              "                  . 2 "+  // ball
              "                    . "); // base

      mySubMech = createSubModel("sub");
      myMech.addModel (mySubMech);
      updateBehaviorMap();
      checkBehaviors();

      FemModel3d fem3 =
         (FemModel3d)myMech.findComponent ("models/sub/models/fem3");

      Collidable surf3 = (Collidable)fem3.findComponent ("meshes/surface");
      Collidable sub31 = (Collidable)fem3.findComponent ("meshes/sub1");
      Collidable sub32 = (Collidable)fem3.findComponent ("meshes/sub2");
      Collidable ball1 = (Collidable)mySubMech.findComponent("rigidBodies/ball1");
      Collidable ball2 = (Collidable)mySubMech.findComponent("rigidBodies/ball2");

      // reset Collidability of FEM sub meshes so that they
      // can collide with anything
      sub11.setCollidable (Collidability.ALL);
      sub12.setCollidable (Collidability.ALL);
      sub21.setCollidable (Collidability.ALL);
      sub22.setCollidable (Collidability.ALL);
      updateBehaviorMap();

      verify (". . . 1 1 1 1 . . 3 3 3 3 3 3 3 "+ // surf1
              "  . 2 1 1 1 1 . . 3 3 3 3 3 3 3 "+ // sub11
              "    . 1 1 1 1 . . 3 3 3 3 3 3 3 "+ // sub12
              "      . . . 1 . . 3 3 3 3 3 3 3 "+ // surf2 
              "        . 2 1 . . 3 3 3 3 3 3 3 "+ // sub21
              "          . 1 . . 3 3 3 3 3 3 3 "+ // sub22
              "            . . . . . 3 3 3 3 3 "+ // surf3
              "              . . . . . . . . . "+ // sub31
              "                . . . . . . . . "+ // sub32
              "                  . . 2 2 2 2 2 "+ // ball1
              "                    . 2 2 2 2 2 "+ // ball2
              "                      . . . 2 2 "+ // comp0
              "                        . . 2 2 "+ // comp1
              "                          . 2 2 "+ // comp2
              "                            . 2 "+ // ball
              "                              . ");// base

      setBehavior (fem3, fem3, true, 7);
      setDefaultBehavior (mySubMech, CollisionManager.DEFORMABLE_RIGID, true, 2);
      setBehavior (mySubMech, ball1, ball2, true, 6);
      setBehavior (surf3, ball1, true, 8);
      setBehavior (ball, sub31, true, 7);

      verify (". . . 1 1 1 1 . . 3 3 3 3 3 3 3 "+ // surf1
              "  . 2 1 1 1 1 . . 3 3 3 3 3 3 3 "+ // sub11
              "    . 1 1 1 1 . . 3 3 3 3 3 3 3 "+ // sub12
              "      . . . 1 . . 3 3 3 3 3 3 3 "+ // surf2 
              "        . 2 1 . . 3 3 3 3 3 3 3 "+ // sub21
              "          . 1 . . 3 3 3 3 3 3 3 "+ // sub22
              "            . . . 8 2 3 3 3 3 3 "+ // surf3
              "              . 7 . . . . . . . "+ // sub31
              "                . . . . . . . . "+ // sub32
              "                  . 6 2 2 2 2 2 "+ // ball1
              "                    . 2 2 2 2 2 "+ // ball2
              "                      . . . 2 2 "+ // comp0
              "                        . . 2 2 "+ // comp1
              "                          . 2 2 "+ // comp2
              "                            . 2 "+ // ball
              "                              . ");// base


   }

   public static void main (String[] args) {
      CollisionManagerTest tester = new CollisionManagerTest(); 
      tester.runtest();
   }
}
