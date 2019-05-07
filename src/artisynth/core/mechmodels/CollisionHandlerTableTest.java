package artisynth.core.mechmodels;

import java.util.*;
import maspack.util.*;

import artisynth.core.mechmodels.Collidable.Group;

/**
 * Unit tester for CollisionHandlerTable
 */
public class CollisionHandlerTableTest extends UnitTest {

   CollisionHandlerTableTest() {
   }   

   private class HashMapHandlerTable {
      // collision handler map implemented using HashMap for timing comparison
      HashMap<CollidablePair,CollisionHandler> myMap;

      public HashMapHandlerTable() {
         myMap = new HashMap<CollidablePair,CollisionHandler>();
      }

      public void initialize() {
         myMap.clear();
      }

      public CollisionHandler put (
         CollidableBody col0, CollidableBody col1, CollisionBehavior behav) {

         CollisionHandler ch = 
            new CollisionHandler (null, col0, col1, behav, null);
         myMap.put (new CollidablePair (col0, col1), ch);
         return ch;
      }

      public CollisionHandler get (
         CollidableBody col0, CollidableBody col1) {
         return myMap.get (new CollidablePair (col0, col1));
      }
   }

   private ArrayList<CollidableBody> createBodies (int numb, int startIdx) {
      ArrayList<CollidableBody> bodies = new ArrayList<CollidableBody>();
      for (int i=0; i<numb; i++) {
         RigidBody body = RigidBody.createBox (null, 1.0, 1.0, 1.0, 1.0);
         body.setName ("body"+(i+startIdx));
         body.setCollidableIndex (i+startIdx);
         bodies.add (body);
      }
      return bodies;
   }

   private void reindexBodies (ArrayList<CollidableBody> bodies) {
      int k = 0;
      for (CollidableBody cbody : bodies) {
         cbody.setCollidableIndex (k++);
      }
   }

   private ArrayList<CollidablePair> createRandomPairs (
      int nump, ArrayList<CollidableBody> bodies) {

      ArrayList<CollidablePair> pairs = new ArrayList<CollidablePair>();
      int maxi = bodies.size()-1;
      for (int i=0; i<nump; i++) {
         CollidableBody col0 = bodies.get(RandomGenerator.nextInt (0, maxi));
         CollidableBody col1 = bodies.get(RandomGenerator.nextInt (0, maxi));
         pairs.add (new CollidablePair (col0, col1));
      }
      return pairs;
   }

   public void test() {
      testPutGet();
      testReinitialize();
   }

   public void testPutGet() {
      ArrayList<CollidableBody> bodies = createBodies (200, 0);
      ArrayList<CollidablePair> pairs = createRandomPairs (1000, bodies);

      RandomGenerator.setSeed (0x1234);
      CollisionHandlerTable table = new CollisionHandlerTable(null);
      HashMapHandlerTable map = new HashMapHandlerTable();

      CollisionBehavior behav = new CollisionBehavior();
      table.initialize (bodies);
      for (int i=0; i<pairs.size(); i++) {
         CollidableBody col0 = (CollidableBody)pairs.get(i).get(0);
         CollidableBody col1 = (CollidableBody)pairs.get(i).get(1);

         CollisionHandler ch = table.get (col0, col1);
         if (ch == null) {
            table.put (col0, col1, behav, null);
            CollisionHandler ch0 = table.get (col0, col1);
            CollisionHandler ch1 = table.get (col1, col0);
            if (ch0 == null || ch0 != ch1) {
               throw new TestException (
                  "ERROR in table: "+ch0+" "+ch1+" i=" + i);
            }
         }
         CollisionHandler chx = map.get (col0, col1);
         if (chx == null) {
            map.put (col0, col1, behav);
            CollisionHandler ch0 = map.get (col0, col1);
            CollisionHandler ch1 = map.get (col1, col0);
            if (ch0 == null || ch0 != ch1) {
               throw new TestException (
                  "ERROR in map: "+ch0+" "+ch1);
            }
         }
         if ((ch==null) != (chx==null)) {
            throw new TestException (
               "error: ch=" + ch + " chx="+chx);
         }
      }
   }

   public void testReinitialize() {
      
      int tsize1 = 200;
      
      ArrayList<CollidableBody> bodies = createBodies (tsize1, 0);
      ArrayList<CollidablePair> pairs = createRandomPairs (tsize1*5, bodies);

      RandomGenerator.setSeed (0x1234);
      CollisionHandlerTable table = new CollisionHandlerTable(null);
      table.initialize (bodies);
      putTest (table, bodies, pairs);

      // now delete some bodies 
      HashSet<CollidableBody> removed = new HashSet<CollidableBody>();
      for (int i=0; i<tsize1/2; i++) {
         CollidableBody cbody = 
            bodies.remove (RandomGenerator.nextInt (0, bodies.size()-1));
         removed.add (cbody);
      }
      // and add some bodies
      bodies.addAll (createBodies (tsize1/2, tsize1));
      reindexBodies (bodies);

      ArrayList<CollisionHandler> handlers = new ArrayList<CollisionHandler>();
      table.collectHandlers (handlers);

      HashSet<CollisionHandler> expected = new HashSet<CollisionHandler>();
      for (CollisionHandler ch : handlers) {
         if (!removed.contains(ch.getCollidable(0)) &&
             !removed.contains(ch.getCollidable(1))) {
            expected.add (ch);
         }
      }
      // and reinitialize
      table.setHandlerActivity (true);
      table.reinitialize (bodies);
      handlers.clear();
      table.collectHandlers (handlers);
      HashSet<CollisionHandler> result = new HashSet<CollisionHandler>();
      result.addAll (handlers);
      if (!expected.equals (result)) {
         System.out.println ("Expected:");
         for (CollisionHandler ch : expected) {
            System.out.println (ch.getCollidablePair());
         }
         System.out.println ("Result:");
         for (CollisionHandler ch : result) {
            System.out.println (ch.getCollidablePair());
         }
         throw new TestException (
            "handlers not preserved under reinitialize");
      }
      for (CollisionHandler ch : handlers) {
         CollisionHandler chx = table.get (
            ch.getCollidable(0), ch.getCollidable(1));
         if (chx != ch) {
            throw new TestException (
               "Get fails after reinitialize for pair " +
               ch.getCollidablePair());
         }
      }
   }

   public int putTest (
      CollisionHandlerTable table, ArrayList<CollidableBody> bodies,
      ArrayList<CollidablePair> pairs) {

      CollisionBehavior behav = new CollisionBehavior();
      int puts = 0;
      for (int i=0; i<pairs.size(); i++) {
         CollidableBody col0 = (CollidableBody)pairs.get(i).get(0);
         CollidableBody col1 = (CollidableBody)pairs.get(i).get(1);
         if (table.get (col0, col1) == null) {
            table.put (col0, col1, behav, null);
            puts++;
         }
      }
      return puts;
   }

   public int putTest (
      HashMapHandlerTable map, ArrayList<CollidableBody> bodies,
      ArrayList<CollidablePair> pairs) {

      CollisionBehavior behav = new CollisionBehavior();
      int puts = 0;
      for (int i=0; i<pairs.size(); i++) {
         CollidableBody col0 = (CollidableBody)pairs.get(i).get(0);
         CollidableBody col1 = (CollidableBody)pairs.get(i).get(1);
         if (map.get (col0, col1) == null) {
            map.put (col0, col1, behav);
            puts++;
         }
      }
      return puts;
   }

   public int getTest (
      CollisionHandlerTable table, ArrayList<CollidableBody> bodies,
      ArrayList<CollidablePair> pairs) {
      int gets = 0;
      for (int i=0; i<pairs.size(); i++) {
         CollidableBody col0 = (CollidableBody)pairs.get(i).get(0);
         CollidableBody col1 = (CollidableBody)pairs.get(i).get(1);
         if (table.get (col0, col1) == null) {
            gets++;
         }
      }
      return gets;
   }

   public int getTest (
      HashMapHandlerTable map, ArrayList<CollidableBody> bodies,
      ArrayList<CollidablePair> pairs) {
      int gets = 0;
      for (int i=0; i<pairs.size(); i++) {
         CollidableBody col0 = (CollidableBody)pairs.get(i).get(0);
         CollidableBody col1 = (CollidableBody)pairs.get(i).get(1);
         if (map.get (col0, col1) == null) {
            gets++;
         }
      }
      return gets;
   }

   public void timing() {
      FunctionTimer timer = new FunctionTimer();

      int numb = 200;
      ArrayList<CollidableBody> bodies = new ArrayList<CollidableBody>();
      for (int i=0; i<numb; i++) {
         bodies.add (RigidBody.createBox (null, 1.0, 1.0, 1.0, 1.0));
      }
      int numt = 1000;

      CollisionHandlerTable table = new CollisionHandlerTable(null);
      HashMapHandlerTable map = new HashMapHandlerTable();
      ArrayList<CollidablePair> pairs = createRandomPairs (numt, bodies);

      for (int i=0; i<1000; i++) {
         table.initialize (bodies);
         putTest (table, bodies, pairs);
         putTest (map, bodies, pairs);
      }

      RandomGenerator.setSeed (0x1234);      

      
      table.initialize (bodies);
      map.initialize();
      timer.start();
      int puts = putTest (table, bodies, pairs);
      timer.stop();
      System.out.println ("put(table): "+timer.result(numt)+" puts=" + puts);
      
      RandomGenerator.setSeed (0x1234);      
      timer.start();
      puts = putTest (map, bodies, pairs);
      timer.stop();
      System.out.println ("put(hashMap): "+timer.result(numt)+" puts=" + puts);

      numt = 10000;
      pairs = createRandomPairs (numt, bodies);

      timer.start();
      int gets = getTest (table, bodies, pairs);
      timer.stop();
      System.out.println ("get(table): "+timer.result(numt)+" gets=" + gets);
      
      RandomGenerator.setSeed (0x1234);      
      timer.start();
      gets = getTest (map, bodies, pairs);
      timer.stop();
      System.out.println ("get(hasMap): "+timer.result(numt)+" gets=" + gets);
   }

   public static void main (String[] args) {

      CollisionHandlerTableTest tester = new CollisionHandlerTableTest();
      RandomGenerator.setSeed (0x1234);

      boolean dotiming = false;

      for (int i=0; i<args.length; i++) {
         if (args[i].equals ("-timing")) {
            dotiming = true;
         }
         else {
            System.err.println (
"Usage: java artisynth.core.mechmodels.CollisionHandlerTableTest [-timing]");
            System.exit(1);
         }
      }      
      if (dotiming) {
         tester.timing();
      }
      else {
         tester.runtest();
      }
   }
}

