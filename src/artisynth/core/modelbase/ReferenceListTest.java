package artisynth.core.modelbase;

import java.util.*;
import maspack.util.*;
import maspack.matrix.*;
import artisynth.core.modelbase.ComponentTest.TestComp;

public class ReferenceListTest extends UnitTest {

   String contentString (TestComp[] comps) {
      StringBuilder sb = new StringBuilder();
      for (TestComp c : comps) {
         sb.append (c.getName() + " ");
      }
      return sb.toString();
   }

   void checkContents (ReferenceList<TestComp> rlist, TestComp... comps) {
      boolean fail = false;
      ArrayList<TestComp> contents = new ArrayList<>();
      rlist.getReferences (contents);
      if (comps.length != rlist.size()) {
         fail = true;
      }
      else {
         for (int i=0; i<comps.length; i++) {
            if (contents.get(i) != comps[i]) {
               fail = true;
               break;
            }
         }
      }
      if (fail) {
         throw new TestException (
            "Unexpected list contents:\n" +
            "Expected: "+contentString(comps)+"\n" +
            "Actual:   "+contentString(contents.toArray(new TestComp[0])));
      }
   }

   public void test() {
      TestComp[] comps = new TestComp[4];
      for (int i=0; i<comps.length; i++) {
         comps[i] = new TestComp("comp" + i);
      }
      ReferenceList<TestComp> rlist = new ReferenceList<>("reflist"); 
      rlist.addReference (comps[0]);
      rlist.addReference (comps[1]);
      rlist.addReference (comps[2]);
      checkContents (rlist, comps[0], comps[1], comps[2]);
      
      rlist.removeReference (comps[1]);
      checkContents (rlist, comps[0], comps[2]);
      checkEquals ("index of comps[0]", rlist.indexOfReference(comps[0]), 0);
      checkEquals ("index of comps[2]", rlist.indexOfReference(comps[2]), 1);
      checkEquals ("index of comps[1]", rlist.indexOfReference(comps[1]), -1);

      rlist.removeAllReferences ();
      checkContents (rlist);

      rlist.addReferences (Arrays.asList(comps));
      checkContents (rlist, comps);

      rlist.addReferences (Arrays.asList(comps));
      rlist.removeReferences (Arrays.asList(comps));
      checkContents (rlist);

      rlist.addReferences (Arrays.asList(comps));
      rlist.addReference (comps[2]);
      rlist.addReference (comps[2]);

      rlist.removeReferences (Arrays.asList (new TestComp[] { comps[2]}));
      checkContents (rlist, comps[0], comps[1], comps[3]);
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);
      ReferenceListTest tester = new ReferenceListTest();
      tester.runtest();
   }

}
