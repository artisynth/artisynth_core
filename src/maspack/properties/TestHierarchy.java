/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.properties;

import java.util.*;
import java.io.*;
import maspack.util.*;
import java.awt.Color;

/**
 * Artificial properties used for testing:
 * 
 * double stiffness; double damping; double modulus; String file; int order; int
 * width; Color color; Vector3d dir; Material double density double stiffness
 * double damping RenderInfo Color color; String textureFile; double shine; int
 * width;
 * 
 * and here is the hierarchy:
 * 
 * myRoot /\ / \ M1 M7 /\ |\ / \ | \ S2 T4 M8 S9 / /\ | / / \ | M3 M5 S6 T10
 */

public class TestHierarchy {
   TestNode myRoot;
   MatNode M1;
   SlewNode S2;
   MatNode M3;
   TestNode T4;
   MatNode M5;
   SlewNode S6;
   MatNode M7;
   MatNode M8;
   SlewNode S9;
   TestNode T10;

   TestNode T11;

   HashMap<String,TestNode> nodeMap = new HashMap<String,TestNode>();

   Object END = Void.TYPE;

   static Color red = new Color (1f, 0, 0);
   static Color gray = new Color (0.5f, 0.5f, 0.5f);
   static Color white = new Color (1f, 1f, 1f);
   static Color green = new Color (0f, 1f, 0f);
   static Color blue = new Color (0f, 0f, 1f);

   public TestNode getRoot() {
      return myRoot;
   }

   private class Changes {
      PropTreeCell added = new PropTreeCell();
      PropTreeCell removed = new PropTreeCell();
      PropTreeCell modified = new PropTreeCell();

      Changes (PropTreeCell oldSet, PropTreeCell newSet) {
         set (oldSet, newSet);

      }

      public void set (PropTreeCell oldSet, PropTreeCell newSet) {
         added.removeAllChildren();
         removed.removeAllChildren();
         modified.removeAllChildren();

         // for each child in the old set ...
         for (PropTreeCell oldChild = oldSet.myFirstChild; oldChild != null; oldChild =
            oldChild.next) {
            boolean found = false;

            PropTreeCell newChild =
               newSet.findMatchingChild (oldChild.getInfo());
            if (newChild != null) {
               // handle modified case
               if (newChild.hasChildren() && oldChild.hasChildren()) {
                  Changes subChanges = new Changes (oldChild, newChild);
                  if (subChanges.added.hasChildren()) {
                     added.addChild (subChanges.added);
                     subChanges.added.setData (newChild.myData);
                  }
                  if (subChanges.removed.hasChildren()) {
                     removed.addChild (subChanges.removed);
                     subChanges.removed.setData (newChild.myData);
                  }
                  if (subChanges.modified.hasChildren()) {
                     modified.addChild (subChanges.modified);
                     subChanges.modified.setData (newChild.myData);
                  }
               }
               else if (newChild.hasChildren()) {
                  removed.addChild (PropTreeCell.copyTree (oldChild));
                  added.addChild (PropTreeCell.copyTree (newChild));
               }
               else if (oldChild.hasChildren()) {
                  removed.addChild (PropTreeCell.copyTree (oldChild));
                  added.addChild (PropTreeCell.copyTree (newChild));
               }
               else if (!PropertyUtils.equalValues (
                  newChild.getValue(), oldChild.getValue())) {
                  modified.addChild (PropTreeCell.copyTree (newChild));
               }
               else { // not changed
               }
            }
            else {
               removed.addChild (new PropTreeCell (oldChild.myData));
            }
         }
         // for each child in the new set ...
         for (PropTreeCell newChild = newSet.myFirstChild; newChild != null; newChild =
            newChild.next) {
            PropTreeCell oldChild =
               oldSet.findMatchingChild (newChild.getInfo());
            if (oldChild == null) {
               added.addChild (new PropTreeCell (newChild.myData));
            }
         }
      }

      public String toString() {
         return (" added:\n" + added.treeString() + " removed:\n"
         + removed.treeString() + " modified:\n" + modified.treeString());
      }

      private void error (String context, String msg) {
         System.out.println (context + " current change state:");
         System.out.println (toString());
         throw new TestException (context + " " + msg);
      }

      public int check (Object[] changeList, int offset, String context) {
         PropTreeCell checkAdded = PropTreeCell.copyTree (added);
         PropTreeCell checkRemoved = PropTreeCell.copyTree (removed);
         PropTreeCell checkModified = PropTreeCell.copyTree (modified);

         int idx = offset;
         while (idx < changeList.length) {
            String desc;
            if (changeList[idx] == END) {
               return idx + 1;
            }
            else if (idx >= changeList.length - 1) {
               throw new IllegalArgumentException ("premature end of input");
            }
            if (!(changeList[idx] instanceof String)) {
               throw new IllegalArgumentException (
                  "even entries of changeList must be strings");
            }
            desc = (String)changeList[idx++];
            String[] splits = desc.split ("\\s+");
            if (splits.length != 2) {
               throw new IllegalArgumentException (
                  "invalid change description '" + desc + "'");
            }
            String pathName = splits[1];
            Object value = changeList[idx++];
            if (splits[0].equals ("A")) {
               PropTreeCell cell = checkAdded.getDescendant (pathName);
               if (cell == null) {
                  error (context, desc + ": not found");
               }
               else if (!PropertyUtils.equalValues (cell.getValue(), value)) {
                  error (context, desc + ": value=" + cell.getValue()
                  + ", expecting " + value);
               }
               else {
                  checkAdded.removeDescendant (cell);
               }
            }
            else if (splits[0].equals ("R")) {
               PropTreeCell cell = checkRemoved.getDescendant (pathName);
               if (cell == null) {
                  error (context, desc + ": not found");
               }
               else {
                  checkRemoved.removeDescendant (cell);
               }
            }
            else if (splits[0].equals ("M")) {
               PropTreeCell cell = checkModified.getDescendant (pathName);
               if (cell == null) {
                  error (context, desc + ": not found");
               }
               else if (!PropertyUtils.equalValues (cell.getValue(), value)) {
                  error (context, desc + ": value=" + cell.getValue()
                  + ", expecting " + value);
               }
               else {
                  checkModified.removeDescendant (cell);
               }
            }
            else {
               throw new IllegalArgumentException ("Illegal code " + splits[0]);
            }
         }
         if (checkAdded.hasChildren()) {
            error (context, "unexpected additions:\n"
            + checkAdded.treeString());
         }
         if (checkRemoved.hasChildren()) {
            error (context, "unexpected removal:\n"
            + checkRemoved.treeString());
         }
         if (checkModified.hasChildren()) {
            error (context, "unexpected modification:\n"
            + checkModified.treeString());
         }
         return idx;
      }

      public boolean equals (Object obj) {
         if (obj instanceof Changes) {
            Changes changes = (Changes)obj;
            return (added.equals (changes.added) &&
                    removed.equals (changes.removed) &&
                    modified.equals (changes.modified));
         }
         else {
            return false;
         }
      }

      public boolean isEmpty() {
         return (!added.hasChildren() &&
                 !removed.hasChildren() &&
                 !modified.hasChildren());
      }

   }

   public void recordAllProperties (
      HashMap<String,PropTreeCell> map, TestNode node) {

      PropTreeCell leafProps = new PropTreeCell();
      leafProps.addLeafProperties (node);
      map.put (node.getName(), leafProps);
      Iterator<TestNode> it = node.getChildren();
      while (it.hasNext()) {
         TestNode child = it.next();
         recordAllProperties (map, child);
      }
   }

   public void printAllProperties (PrintStream os, Map<String,PropTreeCell> map) {
      for (Map.Entry<String,PropTreeCell> entry : map.entrySet()) {
         os.println (entry.getKey() + ":");
         entry.getValue().printTree (os);
      }
   }

   public HashMap<String,PropTreeCell> recordAllProperties (TestNode node) {
      HashMap<String,PropTreeCell> map =
         new LinkedHashMap<String,PropTreeCell>();
      recordAllProperties (map, node);
      return map;
   }

   int checkValues (String name, TestNode node, Object[] vals, int offset) {
      Property prop = node.getProperty (name);
      if (prop != null) {
         if (!PropertyUtils.equalValues (prop.get(), vals[offset])) {
            throw new TestException (node.getName() + "/" + name + "="
            + prop.get() + ", expecting " + vals[offset]);
         }
         offset++;
      }
      Iterator<TestNode> it = node.getChildren();
      while (it.hasNext()) {
         TestNode child = it.next();
         offset = checkValues (name, child, vals, offset);
      }
      return offset;
   }

   public TestHierarchy() {
      myRoot = addNode (new TestNode ("root"));

      M1 = (MatNode)addNode (new MatNode ("M1"));
      S2 = (SlewNode)addNode (new SlewNode ("S2"));
      M3 = (MatNode)addNode (new MatNode ("M3"));
      T4 = addNode (new TestNode ("T4"));
      M5 = (MatNode)addNode (new MatNode ("M5"));
      S6 = (SlewNode)addNode (new SlewNode ("S6"));
      M7 = (MatNode)addNode (new MatNode ("M7"));
      M8 = (MatNode)addNode (new MatNode ("M8"));
      S9 = (SlewNode)addNode (new SlewNode ("S9"));
      T10 = addNode (new TestNode ("T10"));

      T11 = addNode (new TestNode ("T11"));

      myRoot.setStyleMode (PropertyMode.Explicit);
      myRoot.addChild (M1);
      myRoot.addChild (M7);
      M1.addChild (S2);
      M1.addChild (T4);

      S2.setStyleMode (PropertyMode.Explicit);
      S2.setStyle (4);
      S2.addChild (M3);
      T4.addChild (M5);
      T4.addChild (S6);

      M7.setStyleMode (PropertyMode.Explicit);
      M7.setStyle (1);
      M7.addChild (M8);
      M7.addChild (S9);

      S9.addChild (T10);

   }

   public TestNode addNode (TestNode node) {
      nodeMap.put (node.getName(), node);
      return node;
   }

   public TestNode getNode (String name) {
      return nodeMap.get (name);
   }

   HashMap<String,PropTreeCell> oldPropMap;
   HashMap<String,PropTreeCell> newPropMap;
   HashMap<String,PropTreeCell> savedPropMap;

   private void updatePropMaps() {
      oldPropMap = newPropMap;
      newPropMap = recordAllProperties (myRoot);
   }

   void checkChanges (Object[] objs) {
      updatePropMaps();

      int idx = 0;

      // make copies so we can delete entries as we process things
      LinkedHashMap<String,PropTreeCell> newMap =
         new LinkedHashMap<String,PropTreeCell> (newPropMap);
      LinkedHashMap<String,PropTreeCell> oldMap =
         new LinkedHashMap<String,PropTreeCell> (oldPropMap);

      PropTreeCell oldSet;
      PropTreeCell newSet;

      while (idx < objs.length) {
         if (!(objs[idx] instanceof String)) {
            throw new IllegalArgumentException ("node name expected, idx "
            + idx);
         }
         String nodeName = (String)objs[idx++];
         if (nodeName.endsWith (" removed")) {
            nodeName = nodeName.substring (0, nodeName.indexOf (' '));
            if (newMap.get (nodeName) != null) {
               throw new TestException ("node " + nodeName + " was not removed");
            }
            if (oldMap.get (nodeName) == null) {
               throw new TestException ("node " + nodeName
               + " not originally present");
            }
            oldMap.remove (nodeName);
         }
         else {
            if ((newSet = newMap.get (nodeName)) == null) {
               throw new TestException ("node " + nodeName + " not present");
            }
            oldSet = oldMap.get (nodeName);
            Changes changes = new Changes (oldSet, newSet);
            idx = changes.check (objs, idx, nodeName + ":");
            newMap.remove (nodeName);
            if (oldSet != null) {
               oldMap.remove (nodeName);
            }
         }
      }
      // all remaining property entries should be equal
      for (String nodeName : newMap.keySet()) {
         oldSet = oldMap.get (nodeName);
         if (oldSet == null) {
            throw new TestException ("node " + nodeName
            + " appeared unexpectedly");
         }
         newSet = newMap.get (nodeName);
         Changes changes = new Changes (oldSet, newSet);
         if (!changes.isEmpty()) {
            System.out.println ("oldSet:\n" + oldSet.treeString());
            System.out.println ("newSet:\n" + newSet.treeString());
            System.out.println ("changes:\n" + changes);
            throw new TestException (nodeName + ": unexpected property changes");
         }
         oldMap.remove (nodeName);
      }
      if (!oldMap.isEmpty()) {
         throw new TestException ("nodes disappered unexpectedly");
      }
   }
}


class MatNode extends TestNode {
   TestMaterial myMaterial;
   double myModulus;
   int myOrder;
   Color myColor;
   PropertyMode myColorMode;

   static TestMaterial defaultMaterial() {
      return new TestMaterial();
   }

   static double DEFAULT_MODULUS = 2.0;
   static int DEFAULT_ORDER = 1;

   static Color defaultColor() {
      return new Color (1f, 0, 0);
   }

   protected void setDefaultValues() {
      super.setDefaultValues();
      setMaterial (defaultMaterial());
      setModulus (DEFAULT_MODULUS);
      setOrder (DEFAULT_ORDER);
      setColor (defaultColor());
      myColorMode = PropertyMode.Inherited;
   }

   public static PropertyList myProps =
      new PropertyList (MatNode.class, TestNode.class);

   static {
      myProps.add ("material * *", "material for this node", defaultMaterial());
      myProps.add ("modulus * *", "modulus for the node", DEFAULT_MODULUS);
      myProps.add ("order", "order for this node", DEFAULT_ORDER);
      myProps.addInheritable ("color", "color for this node", defaultColor());
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   MatNode (String name) {
      super (name);
   }

   public TestMaterial getMaterial() {
      return myMaterial;
   }

   public void setMaterial (TestMaterial material) {
      if (myMaterial != material) {
         PropertyUtils.updateCompositeProperty (
            this, "material", myMaterial, material);
         myMaterial = material;
      }
   }

   public double getModulus() {
      return myModulus;
   }

   public void setModulus (double modulus) {
      myModulus = modulus;
   }

   public int getOrder() {
      return myOrder;
   }

   public void setOrder (int order) {
      myOrder = order;
   }

   public Color getColor() {
      return myColor;
   }

   // public void setColor (Color color)
   // {
   // myColor = color;
   // if (myColorModeObject != null)
   // { myColorModeObject.propagateValue (this, "color", myColor);
   // }
   // }

   // public PropertyMode getColorMode()
   // {
   // return myColorModeObject.get();
   // }

   // public void setColorMode (PropertyMode mode)
   // {
   // myColorModeObject.setAndUpdate (this, "color", mode);
   // }

   public void setColor (Color color) {
      myColor = color;
      myColorMode =
         PropertyUtils.propagateValue (this, "color", myColor, myColorMode);
   }

   public PropertyMode getColorMode() {
      return myColorMode;
   }

   public void setColorMode (PropertyMode mode) {
      myColorMode =
         PropertyUtils.setModeAndUpdate (this, "color", myColorMode, mode);
   }

}

class SlewNode extends TestNode {
   TestMaterial myMaterial;

   double myStiffness;
   public PropertyMode myStiffnessMode;
   double myDamping;
   PropertyMode myDampingMode;
   int myOrder;
   PropertyMode myOrderMode;
   Color myColor;
   PropertyMode myColorMode;

   static TestMaterial defaultMaterial() {
      return new TestMaterial();
   }

   static double DEFAULT_STIFFNESS = 1000.0;
   static int DEFAULT_DAMPING = 0;
   static int DEFAULT_ORDER = 7;

   static Color defaultColor() {
      return new Color (1f, 1f, 1f);
   }

   protected void setDefaultValues() {
      super.setDefaultValues();
      setMaterial (defaultMaterial());
      setStiffness (DEFAULT_STIFFNESS);
      myStiffnessMode = PropertyMode.Inherited;
      setDamping (DEFAULT_DAMPING);
      myDampingMode = PropertyMode.Inherited;
      setOrder (DEFAULT_ORDER);
      myOrderMode = PropertyMode.Inherited;
      setColor (defaultColor());
      myColorMode = PropertyMode.Inherited;
   }

   public static PropertyList myProps =
      new PropertyList (SlewNode.class, TestNode.class);

   static {
      myProps.add ("material * *", "material for this node", defaultMaterial());
      myProps.addInheritable (
         "stiffness * *", "stiffness for this node", DEFAULT_STIFFNESS);
      myProps.addInheritable (
         "damping * *", "damping for this node", DEFAULT_DAMPING);
      myProps.addInheritable ("order * *", "order for this node", DEFAULT_ORDER);
      myProps.addInheritable ("color", "color for this node", defaultColor());
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   SlewNode (String name) {
      super (name);
   }

   public TestMaterial getMaterial() {
      return myMaterial;
   }

   public void setMaterial (TestMaterial material) {
      if (myMaterial != material) {
         PropertyUtils.updateCompositeProperty (
            this, "material", myMaterial, material);
         myMaterial = material;
      }
   }

   public double getStiffness() {
      return myStiffness;
   }

   public void setStiffness (double k) {
      myStiffness = k;
      myStiffnessMode =
         PropertyUtils.propagateValue (
            this, "stiffness", myStiffness, myStiffnessMode);
   }

   public PropertyMode getStiffnessMode() {
      return myStiffnessMode;
   }

   public void setStiffnessMode (PropertyMode mode) {
      myStiffnessMode =
         PropertyUtils.setModeAndUpdate (
            this, "stiffness", myStiffnessMode, mode);
   }

   public double getDamping() {
      return myDamping;
   }

   public void setDamping (double d) {
      myDamping = d;
      myDampingMode =
         PropertyUtils.propagateValue (
            this, "damping", myDamping, myDampingMode);
   }

   public PropertyMode getDampingMode() {
      return myDampingMode;
   }

   public void setDampingMode (PropertyMode mode) {
      myDampingMode =
         PropertyUtils.setModeAndUpdate (this, "damping", myDampingMode, mode);
   }

   public int getOrder() {
      return myOrder;
   }

   public void setOrder (int order) {
      myOrder = order;
      myOrderMode =
         PropertyUtils.propagateValue (this, "order", myOrder, myOrderMode);
   }

   public PropertyMode getOrderMode() {
      return myOrderMode;
   }

   public void setOrderMode (PropertyMode mode) {
      myOrderMode =
         PropertyUtils.setModeAndUpdate (this, "order", myOrderMode, mode);
   }

   public Color getColor() {
      return myColor;
   }

   public void setColor (Color color) {
      myColor = color;
      myColorMode =
         PropertyUtils.propagateValue (this, "color", myColor, myColorMode);
   }

   public PropertyMode getColorMode() {
      return myColorMode;
   }

   public void setColorMode (PropertyMode mode) {
      myColorMode =
         PropertyUtils.setModeAndUpdate (this, "color", myColorMode, mode);
   }

}
