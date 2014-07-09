/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.apps;

import maspack.properties.HasProperties;
import maspack.properties.HostList;
import maspack.properties.TestHierarchy;
import maspack.properties.TestNode;
import maspack.util.*;
import maspack.widgets.PropertyDialog;

import javax.swing.*;

import java.util.*;

public class PropertyGuiTest {
   public static void main (String[] args) {
      TestHierarchy test = new TestHierarchy();

      LinkedList<TestNode> nodeList = new LinkedList<TestNode>();

      for (int i = 0; i < args.length; i++) {
         TestNode node = test.getNode (args[i]);
         if (node == null) {
            System.err.println ("Node '" + args[i] + "' not found");
            System.exit (1);
         }
         nodeList.add (node);
      }
      //test.M1.getMaterial().setStiffness (123.0);

      System.out.println ("Properties:");
      test.printAllProperties (
         System.out, test.recordAllProperties (test.getRoot()));

      if (nodeList.size() == 0) {
         System.err.println (
"Usage: java maspack.apps.PropertyGuiTest <NodeName1> <NodeName2> ... ");
         System.exit (1);
      }
      else if (nodeList.size() == 1) {
         HasProperties host = nodeList.get (0);
         PropertyDialog dialog =
            new PropertyDialog ("prop panel test", host, "OK Cancel");
         dialog.setVisible (true);
         while (dialog.isVisible()) {
            try {
               Thread.sleep (100);
            }
            catch (Exception e) { // 
            }
         }
         dialog.dispose();
      }
      else {
         HostList hostList =
            new HostList ((HasProperties[])nodeList.toArray (new TestNode[0]));
         PropertyDialog dialog =
            new PropertyDialog ("prop panel test", hostList, "OK Cancel");
         dialog.setVisible (true);
         while (dialog.isVisible()) {
            try {
               Thread.sleep (100);
            }
            catch (Exception e) { // 
            }
         }
         dialog.dispose();
      }
   }
}
