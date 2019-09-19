/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

import maspack.util.*;
import java.util.*;

public class ComponentTest extends UnitTest {
   
   private void doAssert (String msg, boolean pred) {
      if (!pred) {
         throw new TestException ("assertion failed: " + msg);
      }
   }

   private class TestComp extends ModelComponentBase {
      TestComp (String name) {
         super();
         setName (name);
      }
   }

   private class TestList extends ComponentList<TestComp> {
      TestList (String name) {
         super (TestComp.class, name);
      }

      TestList (String name, String shortName) {
         super (TestComp.class, name, shortName);
      }
   }

   private void printList (TestList list) {
      int i = 0;
      for (TestComp comp : list) {
         System.out.println ("" + i + ": " + comp.getName() + " "
         + comp.getNumber() + " idx=" + list.indexOf (comp));
         i++;
      }
   }

   private void checkList (TestList list) {
      String listName = list.getName();
      for (TestComp comp : list) {
         String name = comp.getName();
         int num = comp.getNumber();
         if (name != null) {
            TestComp c = (TestComp)list.get (name);
            if (c != comp) {
               throw new TestException ("component name '" + name
               + "' improperly mapped in '" + listName + "'; got " + c);
            }
         }
         TestComp c = (TestComp)list.get (Integer.toString (num));
         if (c != comp || c.getNumber() != num) {
            throw new TestException ("component " + num
            + " not mapped by number string in '" + listName + "'; got " + c);
         }
         c = (TestComp)list.getByNumber (num);
         if (c != comp) {
            printList (list);
            throw new TestException ("component " + num
            + " improperly mapped in '" + listName + "'; got " + c);
         }
      }
      // check that the maximum number is in fact the current highest number
      int maxNum = -1;
      for (TestComp comp : list) {
         if (comp.getNumber() > maxNum) {
            maxNum = comp.getNumber();
         }
      }
      if ((maxNum + 1) != list.getNumberLimit()) {
         throw new TestException ("Max component number is "
         + list.getNumberLimit() + ", expected " + maxNum);
      }
      // check path names
      CompositeComponent root = list.getParent();
      if (root != null) {
         for (TestComp comp : list) {
            String path = ComponentUtils.getPathName (root, comp);
            if (root.findComponent (path) != comp) {
               throw new TestException ("Cannot find component with path '"
               + path + "'");
            }
            path = ComponentUtils.getWritePathName (root, comp);
            // strip quotes of write path
            if (path.startsWith("\"")) {
               path = path.substring (1);
            }
            if (path.endsWith("\"")) {
               path = path.substring (0, path.length()-1);
            }
            if (root.findComponent (path) != comp) {
               throw new TestException (
                  "Cannot find component with write path '" + path + "'");
            }
         }
      }
      for (int i = 0; i < list.size(); i++) {
         int idx = list.indexOf (list.get (i));
         if (idx != i) {
            throw new TestException ("Index " + idx
            + " reported for component at " + i);
         }
      }
   }

   private void checkRemoval (TestList list, TestComp... comps) {
      for (TestComp comp : comps) {
         String name = comp.getName();
         int num = comp.getNumber();

         if (name != null && list.get (name) != null) {
            throw new TestException ("component '" + name
            + "' still mapped by '" + list.getName() + "'");
         }
         if (list.getByNumber (num) != null) {
            throw new TestException ("component " + num + " still mapped by '"
            + list.getName() + "'");
         }
      }
      checkList (list);
   }

   private void checkNames (TestList list, String... names) {
      String[] listNames = new String[list.size()];
      int k = 0;
      for (TestComp comp : list) {
         listNames[k++] = comp.getName();
      }
      if (!Arrays.equals (listNames, names)) {
         throw new TestException ("list '" + list.getName() + " has names\n"
         + Arrays.toString (listNames) + "\nexpected\n"
         + Arrays.toString (names));
      }
      checkList (list);
   }

   private void checkNumbers (TestList list, Integer... nums) {
      Integer[] listNums = new Integer[list.size()];
      int k = 0;
      for (TestComp comp : list) {
         listNums[k++] = new Integer (comp.getNumber());
      }
      if (!Arrays.equals (listNums, nums)) {
         throw new TestException ("list '" + list.getName() + " has numbers\n"
         + Arrays.toString (listNums) + "\nexpected\n" + Arrays.toString (nums));
      }
      checkList (list);
   }

   private LinkedList<ModelComponent> componentList (ModelComponent... comps) {
      LinkedList<ModelComponent> list = new LinkedList<ModelComponent>();
      for (ModelComponent c : comps) {
         list.add (c);
      }
      return list;
   }

   private void checkConnectedVia (
      ModelComponent comp1, ModelComponent comp2,
      ModelComponent viacomp, boolean value) {
      
      String msg =
         "areConnectedVia " +
         (comp1.getName()+","+comp2.getName()+","+viacomp.getName());
      boolean result =
         ComponentUtils.areConnectedVia (comp1, comp2, viacomp);
      if (value != result) {
         throw new TestException (
            msg + " yielded " + result + ", expected " + value);
      }
   }

   private boolean containedIn (ModelComponent comp, ModelComponent[] comps) {
      for (ModelComponent c : comps) {
         if (c == comp) {
            return true;
         }
      }
      return false;
   }

   private void checkConnectedVia (
      ModelComponent comp1, ModelComponent comp2,
      ModelComponent[] allComps, ModelComponent... viacomps) {

      checkConnectedVia (comp1, comp2, comp1, true);
      checkConnectedVia (comp1, comp2, comp2, true);
      for (ModelComponent comp : viacomps) {
         checkConnectedVia (comp1, comp2, comp, true);
         checkConnectedVia (comp2, comp1, comp, true);
      }
      for (ModelComponent comp : allComps) {
         if (comp != comp1 && comp != comp2 && !containedIn (comp, viacomps)) {
            checkConnectedVia (comp1, comp2, comp, false);
            checkConnectedVia (comp2, comp1, comp, false);
         }
      }
   }

   private void checkNotConnected (
      ModelComponent comp1, ModelComponent comp2, ModelComponent[] allComps) {
      
      for (ModelComponent comp : allComps) {
         checkConnectedVia (comp1, comp2, comp, false);
         checkConnectedVia (comp2, comp1, comp, false);
      }
   }

   public void testAreConnectedVia() {

      ComponentList<ModelComponent> A, B, C, D, J, K;
      TestComp E, F, G, H, I, L;

      A = new ComponentList<ModelComponent> (ModelComponent.class, "A");
      B = new ComponentList<ModelComponent> (ModelComponent.class, "B");
      C = new ComponentList<ModelComponent> (ModelComponent.class, "C");

      D = new ComponentList<ModelComponent> (ModelComponent.class, "D");
      J = new ComponentList<ModelComponent> (ModelComponent.class, "J");
      K = new ComponentList<ModelComponent> (ModelComponent.class, "K");

      E = new TestComp ("E");
      F = new TestComp ("F");

      G = new TestComp ("G");
      H = new TestComp ("H");
      I = new TestComp ("I");

      L = new TestComp ("L");

      ModelComponent[] allComps = new ModelComponent[] {
         A, B, C, D, E, F, G, H, I, J, K, L};

      A.add (B);
      A.add (C);
      B.add (D);
      B.add (E);
      C.add (F);
      C.add (G);
      D.add (H);
      D.add (I);

      J.add (K);
      K.add (L);

      checkConnectedVia (I, E, D, true);
      checkConnectedVia (I, E, C, false);
      checkConnectedVia (I, D, D, true);

      checkConnectedVia (E, F, allComps, A, B, C);
      checkConnectedVia (D, E, allComps, B);
      checkConnectedVia (J, L, allComps, K);
      checkConnectedVia (F, G, allComps, C);
      checkConnectedVia (I, G, allComps, D, B, A, C);
      checkConnectedVia (I, A, allComps, D, B);
   }

   public void testComponentLists() {
      TestComp A, B, C, D, E, F, G, H, I, J, K, L, M, N, O;

      A = new TestComp ("compA");
      B = new TestComp ("compB");
      C = new TestComp ("compC");

      D = new TestComp (null);
      E = new TestComp (null);
      F = new TestComp (null);

      G = new TestComp ("compG");
      H = new TestComp ("compH");
      I = new TestComp ("compI");

      J = new TestComp ("compJ");
      K = new TestComp ("compK");
      L = new TestComp ("compL");

      M = new TestComp (null);
      N = new TestComp (null);
      O = new TestComp (null);

      TestList listA = new TestList ("listA", "a");
      TestList listB = new TestList ("listB", "b");

      ComponentList<TestList> rootList =
         new ComponentList<TestList> (TestList.class, "root", "r");

      rootList.add (listA);
      rootList.add (listB);

      listA.add (A);
      listA.add (C);
      listA.add (B);
      listA.add (E);
      listA.add (F);
      listA.add (D);

      checkNames (listA, "compA", "compC", "compB", null, null, null);
      checkNumbers (listA, 0, 1, 2, 3, 4, 5);

      // check that setName updated list properly
      E.setName ("compE");
      F.setName ("compF");
      doAssert ("name 'compE' not mapped", listA.get ("compE") == E);
      checkNames (listA, "compA", "compC", "compB", "compE", "compF", null);
      E.setName (null);
      doAssert ("name 'compE' still mapped", listA.get ("compE") == null);

      // check that component nunbers are reused after deletion
      listA.remove (E);
      checkNames (listA, "compA", "compC", "compB", "compF", null);
      checkNumbers (listA, 0, 1, 2, 4, 5);
      listA.remove (F);
      checkRemoval (listA, E, F);
      checkNumbers (listA, 0, 1, 2, 5);

      listA.add (G);
      listA.add (H);
      checkNames (listA, "compA", "compC", "compB", null, "compG", "compH");
      checkNumbers (listA, 0, 1, 2, 5, 4, 3);

      listA.remove (D);
      listA.remove (B);
      listA.remove (C);
      listA.remove (H);
      listA.remove (A);
      checkNames (listA, "compG");
      checkNumbers (listA, 4);

      TestComp[] comps = new TestComp[] { A, H, C, B, D };
      listA.addComponents (comps, new int[] { 0, 5, 1, 2, 3 }, 5);
      checkNames (listA, "compA", "compC", "compB", null, "compG", "compH");
      checkNumbers (listA, 0, 1, 2, 5, 4, 3);

      listA.remove (3);
      listA.remove (2);
      listA.remove (1);
      listA.remove (2);
      listA.remove (0);
      checkNames (listA, "compG");
      checkNumbers (listA, 4);

      listA.addComponents (comps, new int[] { 0, 5, 1, 2, 3 }, 5);
      checkNames (listA, "compA", "compC", "compB", null, "compG", "compH");
      checkNumbers (listA, 0, 1, 2, 5, 4, 3);

      // check removal and readdition
      int[] indices = new int[100];
      comps = new TestComp[] { G, C, D };
      listA.removeComponents (comps, indices, 3);

      checkNames (listA, "compA", "compB", "compH");
      checkNumbers (listA, 0, 2, 3);
      checkRemoval (listA, G, C, D);

      listA.addComponents (comps, indices, 3);
      checkNames (listA, "compA", "compC", "compB", null, "compG", "compH");
      // numbers are changed because components are not added back
      // in exact reverse order
      checkNumbers (listA, 0, 1, 2, 4, 5, 3);

      // remove one element from start of list
      comps = new TestComp[] { A };
      listA.removeComponents (comps, indices, 1);
      checkNames (listA, "compC", "compB", null, "compG", "compH");
      checkNumbers (listA, 1, 2, 4, 5, 3);

      listA.addComponents (comps, indices, 1);
      checkNames (listA, "compA", "compC", "compB", null, "compG", "compH");
      checkNumbers (listA, 0, 1, 2, 4, 5, 3);

      // remove one element from end of list
      comps = new TestComp[] { H };
      listA.removeComponents (comps, indices, 1);
      checkNames (listA, "compA", "compC", "compB", null, "compG");
      checkNumbers (listA, 0, 1, 2, 4, 5);

      listA.addComponents (comps, indices, 1);
      checkNames (listA, "compA", "compC", "compB", null, "compG", "compH");
      checkNumbers (listA, 0, 1, 2, 4, 5, 3);

      // now test ComponentUtils.removeComponents
      LinkedList<ModelComponent> clist;
      LinkedList<MutableCompositeComponent<?>> parents;

      clist = componentList (G, C, B);
      parents = ComponentUtils.removeComponents (clist, indices);

      checkNames (listA, "compA", null, "compH");
      checkNumbers (listA, 0, 4, 3);
      ComponentUtils.addComponentsInReverse (clist, indices, parents);

      checkNames (listA, "compA", "compC", "compB", null, "compG", "compH");
      checkNumbers (listA, 0, 1, 2, 4, 5, 3);

      comps = new TestComp[] { J, K, L, M, N, O };
      listB.addComponents (comps, null, 6);
      checkNames (listB, "compJ", "compK", "compL", null, null, null);
      checkNumbers (listB, 0, 1, 2, 3, 4, 5);

      listB.removeAll();
      checkNames (listB);
      checkNumbers (listB);

      listB.addComponents (comps, null, 6);
      checkNames (listB, "compJ", "compK", "compL", null, null, null);
      checkNumbers (listB, 0, 1, 2, 3, 4, 5);

      listB.removeComponents (comps, null, 6);
      checkNames (listB);
      checkNumbers (listB);
      listB.addComponents (comps, null, 6);
      checkNames (listB, "compJ", "compK", "compL", null, null, null);
      checkNumbers (listB, 5, 4, 3, 2, 1, 0);

      clist = componentList (C, D, K, H, G, M, L);
      parents = ComponentUtils.removeComponents (clist, indices);

      checkNames (listA, "compA", "compB");
      checkNumbers (listA, 0, 2);
      checkNames (listB, "compJ", null, null);
      checkNumbers (listB, 5, 1, 0);

      ComponentUtils.addComponentsInReverse (clist, indices, parents);

      checkNames (listA, "compA", "compC", "compB", null, "compG", "compH");
      checkNumbers (listA, 0, 1, 2, 4, 5, 3);
      checkNames (listB, "compJ", "compK", "compL", null, null, null);
      checkNumbers (listB, 5, 4, 3, 2, 1, 0);

      listA.resetNumbersToIndices();
      checkNames (listA, "compA", "compC", "compB", null, "compG", "compH");
      checkNumbers (listA, 0, 1, 2, 3, 4, 5);

      listB.resetNumbersToIndices();
      checkNames (listB, "compJ", "compK", "compL", null, null, null);
      checkNumbers (listB, 0, 1, 2, 3, 4, 5);

      listA.setZeroBasedNumbering (false);
      checkNames (listA, "compA", "compC", "compB", null, "compG", "compH");
      checkNumbers (listA, 1, 2, 3, 4, 5, 6);

      listA.setZeroBasedNumbering (true);
      checkNames (listA, "compA", "compC", "compB", null, "compG", "compH");
      checkNumbers (listA, 0, 1, 2, 3, 4, 5);

      listA.clear();
      listA.setZeroBasedNumbering (false);
      listA.add (G);
      listA.add (H);
      checkNames (listA, "compG", "compH");
      checkNumbers (listA, 1, 2);

      listA.setZeroBasedNumbering (true);
      checkNames (listA, "compG", "compH");
      checkNumbers (listA, 0, 1);

      // make sure dynamic resizing of numberMap and nameStack works
      for (int i = 0; i < 1000; i++) {
         listA.add (new TestComp (null));
      }
      checkList (listA);
      for (int i = 0; i < 1000; i++) {
         listA.remove (listA.get (0));
      }
      checkList (listA);
      for (int i = 0; i < 100; i++) {
         listA.add (new TestComp (null));
      }
      Random randGen = new Random (0x1234);
      while (listA.size() > 0) {
         int i = (Math.abs (randGen.nextInt()) % listA.getNumberLimit());
         TestComp c = listA.getByNumber (i);
         if (c != null) {
            listA.remove (c);
         }
         checkList (listA);
      }

   }

   public void test() {

      ModelComponent comp = new TestComp("foo");
      Package pack = comp.getClass().getPackage();
      System.out.println ("pack=" + pack);
      System.out.println ("impl title=" + pack.getImplementationTitle());
      System.out.println ("impl vendor=" + pack.getImplementationVendor());
      System.out.println ("impl version=" + pack.getImplementationVersion());
      System.out.println ("spec title=" + pack.getSpecificationTitle());
      System.out.println ("spec vendor=" + pack.getSpecificationVendor());
      System.out.println ("spec version=" + pack.getSpecificationVersion());
      System.out.println ("name=" + pack.getName());

      testComponentLists();
      testAreConnectedVia();
   }

   public static void main (String[] args) {
      ComponentTest tester = new ComponentTest();
      tester.runtest();
   }
}
