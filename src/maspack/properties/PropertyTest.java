/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.properties;

import java.util.*;
import maspack.util.*;

class PropertyTest extends TestHierarchy {
   HashMap<String,TestNode> nodeMap = new HashMap<String,TestNode>();

   public PropertyTest() {
      super();
   }

   public void test() {
      newPropMap = recordAllProperties (myRoot);
      checkValues ("style", myRoot, new Object[] { 3, 3, 4, 4, 3, 3, 3, 1, 1,
                                                  1, 1 }, 0);

      checkChanges (new Object[] {});

      myRoot.setStyle (2);
      checkChanges (new Object[] { "root", "M style", 2, END, "M1", "M style",
                                  2, END, "T4", "M style", 2, END, "M5",
                                  "M style", 2, END, "S6", "M style", 2, END });

      M7.setStyle (7);
      checkChanges (new Object[] { "M7", "M style", 7, END, "M8", "M style", 7,
                                  END, "S9", "M style", 7, END, "T10",
                                  "M style", 7, END });

      M1.setStyle (13);
      checkChanges (new Object[] { "M1", "M style", 13, END, "T4", "M style",
                                  13, END, "M5", "M style", 13, END, "S6",
                                  "M style", 13, END, });
      M1.setStyleMode (PropertyMode.Inactive);
      checkChanges (new Object[] { "M1", "M style", 2, END, "T4", "M style", 2,
                                  END, "M5", "M style", 2, END, "S6",
                                  "M style", 2, END, });
      M1.setStyle (5);
      checkChanges (new Object[] { "M1", "M style", 5, END, });
      M7.setStyleMode (PropertyMode.Inherited);
      checkChanges (new Object[] { "M7", "M style", 2, END, "M8", "M style", 2,
                                  END, "S9", "M style", 2, END, "T10",
                                  "M style", 2, END });
      M1.setColorMode (PropertyMode.Explicit);
      checkChanges (new Object[] { "S2", "M color", red, END, "S6", "M color",
                                  red, END });
      S2.setColor (white);
      checkChanges (new Object[] { "S2", "M color", white, END, "M3",
                                  "M color", white, END, });
      S2.setColorMode (PropertyMode.Inherited);
      checkChanges (new Object[] { "S2", "M color", red, END, "M3", "M color",
                                  red, END });
      M1.setColorMode (PropertyMode.Inherited);
      checkChanges (new Object[] { "S2", "M color", white, END, "S6",
                                  "M color", white, END });

      M7.getRenderInfo().setShineMode (PropertyMode.Explicit);
      checkChanges (new Object[] {});

      myRoot.getRenderInfo().setShineMode (PropertyMode.Explicit);
      myRoot.getRenderInfo().setShine (50.0);

      checkChanges (new Object[] { "root", "M renderInfo.shine", 50.0, END,
                                  "M1", "M renderInfo.shine", 50.0, END, "S2",
                                  "M renderInfo.shine", 50.0, END, "M3",
                                  "M renderInfo.shine", 50.0, END, "T4",
                                  "M renderInfo.shine", 50.0, END, "M5",
                                  "M renderInfo.shine", 50.0, END, "S6",
                                  "M renderInfo.shine", 50.0, END });

      TestRenderInfo renderInfo7 = M7.getRenderInfo();
      M7.setRenderInfo (null);
      checkChanges (new Object[] { "M7", "A renderInfo", null, END, "M8",
                                  "M renderInfo.shine", 50.0, END, "S9",
                                  "M renderInfo.shine", 50.0, END, "T10",
                                  "M renderInfo.shine", 50.0, END });

      M7.setRenderInfo (renderInfo7);
      checkChanges (new Object[] { "M7", "A renderInfo.color", gray,
                                  "A renderInfo.shine", 100.0,
                                  "A renderInfo.width", 1,
                                  "A renderInfo.textureFile", null,
                                  "R renderInfo", null, END, "M8",
                                  "M renderInfo.shine", 100.0, END, "S9",
                                  "M renderInfo.shine", 100.0, END, "T10",
                                  "M renderInfo.shine", 100.0, END });

      M7.setRenderInfo (null);
      renderInfo7.setShineMode (PropertyMode.Inherited);
      M7.setRenderInfo (renderInfo7);

      checkChanges (new Object[] { "M7", "M renderInfo.shine", 50.0, END, "M8",
                                  "M renderInfo.shine", 50.0, END, "S9",
                                  "M renderInfo.shine", 50.0, END, "T10",
                                  "M renderInfo.shine", 50.0, END });

      renderInfo7.setShine (100.0);
      checkChanges (new Object[] { "M7", "M renderInfo.shine", 100.0, END,
                                  "M8", "M renderInfo.shine", 100.0, END, "S9",
                                  "M renderInfo.shine", 100.0, END, "T10",
                                  "M renderInfo.shine", 100.0, END });

      renderInfo7.setShineMode (PropertyMode.Inherited);
      checkChanges (new Object[] { "M7", "M renderInfo.shine", 50.0, END, "M8",
                                  "M renderInfo.shine", 50.0, END, "S9",
                                  "M renderInfo.shine", 50.0, END, "T10",
                                  "M renderInfo.shine", 50.0, END });

      myRoot.getRenderInfo().setWidth (5);
      checkChanges (new Object[] { "root", "M renderInfo.width", 5, END, });

      myRoot.removeChild (M7);
      checkChanges (new Object[] { "M7 removed", "M8 removed", "S9 removed",
                                  "T10 removed", });

      newPropMap = oldPropMap;
      renderInfo7.setShine (40);

      M7.getRenderInfo().setWidthMode (PropertyMode.Inherited);
      S9.getRenderInfo().setWidthMode (PropertyMode.Inherited);

      myRoot.addChild (M7);
      checkChanges (new Object[] { "M7", "M renderInfo.shine", 40.0,
                                  "M renderInfo.width", 5, END, "M8",
                                  "M renderInfo.shine", 40.0, END, "S9",
                                  "M renderInfo.shine", 40.0,
                                  "M renderInfo.width", 5, END, "T10",
                                  "M renderInfo.shine", 40.0, END });

      TestRenderInfo info1 = new TestRenderInfo();
      TestRenderInfo info2 = new TestRenderInfo();
      TestRenderInfo info3 = new TestRenderInfo();
      info1.setColor (red);
      M1.getMaterial().setRenderInfo (info1);
      M3.getMaterial().setRenderInfo (info3);

      checkChanges (new Object[] { "M1", "A material.renderInfo.color", red,
                                  "A material.renderInfo.shine", 100.0,
                                  "A material.renderInfo.width", 1,
                                  "A material.renderInfo.textureFile", null,
                                  "R material.renderInfo", null, END, "M3",
                                  "A material.renderInfo.color", red,
                                  "A material.renderInfo.shine", 100.0,
                                  "A material.renderInfo.width", 1,
                                  "A material.renderInfo.textureFile", null,
                                  "R material.renderInfo", null, END, });

      info2.setColor (green);
      S2.getMaterial().setRenderInfo (info2);
      checkChanges (new Object[] { "S2", "A material.renderInfo.color", green,
                                  "A material.renderInfo.shine", 100.0,
                                  "A material.renderInfo.width", 1,
                                  "A material.renderInfo.textureFile", null,
                                  "R material.renderInfo", null, END, "M3",
                                  "M material.renderInfo.color", green, END, });

      info1.setShineMode (PropertyMode.Inactive);
      info1.setShine (123);
      info1.setWidth (11);

      checkChanges (new Object[] { "M1", "M material.renderInfo.shine", 123.0,
                                  "M material.renderInfo.width", 11, END, });

      info2.setWidthMode (PropertyMode.Inactive);
      checkChanges (new Object[] { "S2", "M material.renderInfo.width", 11 });

      info3.setWidthMode (PropertyMode.Inherited);
      checkChanges (new Object[] { "M3", "M material.renderInfo.width", 11 });

      info1.setShineMode (PropertyMode.Explicit);
      checkChanges (new Object[] { "S2", "M material.renderInfo.shine", 123.0,
                                  END, "M3", "M material.renderInfo.shine",
                                  123.0 });

      info2.setColorMode (PropertyMode.Inactive);
      checkChanges (new Object[] { "S2", "M material.renderInfo.color", red,
                                  END, "M3", "M material.renderInfo.color",
                                  red, END, });

      info2.setColor (blue);
      checkChanges (new Object[] { "S2", "M material.renderInfo.color", blue,
                                  END, });

      info3.setColor (white);
      checkChanges (new Object[] { "M3", "M material.renderInfo.color", white,
                                  END, });

      info3.setColorMode (PropertyMode.Inherited);
      checkChanges (new Object[] { "M3", "M material.renderInfo.color", red,
                                  END, });

      info1.setColor (white);
      checkChanges (new Object[] { "M1", "M material.renderInfo.color", white,
                                  END, "S2", "M material.renderInfo.color",
                                  white, END, "M3",
                                  "M material.renderInfo.color", white, END, });

      // printAllProperties (System.out, newPropMap);

      TestRenderInfo rinfo = new TestRenderInfo();
      rinfo.setShine (60.0);
      rinfo.setColor (green);
      rinfo.setWidthMode (PropertyMode.Inherited);

      M1.getRenderInfo().setWidth (7);
      T4.getRenderInfo().set (rinfo);
      PropertyUtils.updateCompositeProperty (T4.getRenderInfo());

      checkChanges (new Object[] { "M1", "M renderInfo.width", 7, END, "T4",
                                  "M renderInfo.color", green,
                                  /* */"M renderInfo.shine", 60.0,
                                  /* */"M renderInfo.width", 7, END, "M5",
                                  "M renderInfo.color", green,
                                  /* */"M renderInfo.shine", 60.0, END, "S6",
                                  "M renderInfo.color", green,
                                  /* */"M renderInfo.shine", 60.0, END });

   }

   public void timing() {
      // Test adding a child when inherited properties under the
      // child need to be set too

      FunctionTimer timer = new FunctionTimer();
      int cnt = 100000;
      Integer value = new Integer (4);
      PropertyInfo info = M1.getProperty ("style").getInfo();
      timer.start();
      for (int i = 0; i < cnt; i++) {
         PropertyUtils.setInheritedValue (info, M1, value);
      }
      timer.stop();
      System.out.println ("set inherited time = " + timer.result (cnt));

      Property style = M1.getProperty ("style");
      Property name = M1.getProperty ("name");

      timer.start();
      for (int i = 0; i < cnt; i++) {
         Object obj = style.get();
      }
      timer.stop();
      System.out.println ("regular get time = " + timer.result (cnt));

      timer.start();
      for (int i = 0; i < cnt; i++) {
         Object obj = M1.getPropertyValue ("style");
      }
      timer.stop();
      System.out.println ("named get time = " + timer.result (cnt));

      Integer ival = new Integer (1234);
      timer.start();
      for (int i = 0; i < cnt; i++) {
         style.set (ival);
      }
      timer.stop();
      System.out.println ("regular set time (inherited prop) = "
      + timer.result (cnt));

      timer.start();
      for (int i = 0; i < cnt; i++) {
         M1.setPropertyValue ("style", ival);
      }
      timer.stop();
      System.out.println ("named set time (inherited prop) = "
      + timer.result (cnt));

      timer.start();
      for (int i = 0; i < cnt; i++) {
         name.set ("foo");
      }
      timer.stop();
      System.out.println ("regular set time = " + timer.result (cnt));

      timer.start();
      for (int i = 0; i < cnt; i++) {
         M1.setPropertyValue ("name", "foo");
      }
      timer.stop();
      System.out.println ("named set time = " + timer.result (cnt));

      timer.start();
      for (int i = 0; i < cnt; i++) {
         M1.setName ("foo");
      }
      timer.stop();
      System.out.println ("dedicated set time = " + timer.result (cnt));
   }

   public static void main (String[] args) {
      boolean doTiming = false;
      for (int i = 0; i < args.length; i++) {
         if (args[i].equals ("-timing")) {
            doTiming = true;
         }
         else {
            System.out.println ("Unknown option " + args[i]);
            System.exit (1);
         }
      }
      PropertyTest tester = new PropertyTest();
      if (doTiming) {
         tester.timing();
         return;
      }
      try {
         tester.test();
      }
      catch (Exception e) {
         e.printStackTrace();
         System.exit (1);
      }
      System.out.println ("\nPassed\n");
   }
}
