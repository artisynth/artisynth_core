/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.properties;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Artificial property-containing class used for testing.
 */
public class TestNode implements HierarchyNode, HasProperties {
   String myName;
   int myStyle;
   PropertyMode myStyleMode;
   TestRenderInfo myRenderInfo;

   ArrayList<TestNode> myChildren;
   TestNode myParent;

   public static PropertyList myProps = new PropertyList (TestNode.class);

   protected static String DEFAULT_NAME = null;
   protected static int DEFAULT_STYLE = 3;

   static TestRenderInfo defaultRenderInfo() {
      return new TestRenderInfo();
   }

   protected void setDefaultValues() {
      setName (DEFAULT_NAME);
      setStyle (DEFAULT_STYLE);
      myStyleMode = PropertyMode.Inherited;
      setRenderInfo (defaultRenderInfo());
   }

   static {
      myProps.add ("name * *", "name for this component", null, "NE");
      myProps.addReadOnly ("rand *", "a random number");
      myProps.addInheritable ("style", "style for this node", DEFAULT_STYLE);
      myProps.add ("renderInfo", "render information", defaultRenderInfo());
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public TestNode (String name) {
      myChildren = new ArrayList<TestNode>();
      setDefaultValues();
      setName (name);
   }

   public Property getProperty (String name) {
      return PropertyList.getProperty (name, this);
   }

   public Object getPropertyValue (String name) {
      return getAllPropertyInfo().get (name).getValue (this);
   }

   public void setPropertyValue (String name, Object value) {
      getAllPropertyInfo().get (name).setValue (this, value);
   }

   public double getRand() {
      return 0x12345;
   }

   public void setName (String name) {
      myName = name;
   }

   public String getName() {
      return myName;
   }

   public int getStyle() {
      return myStyle;
   }

   // public void setStyle (int style)
   // {
   // myStyle = style;
   // if (myStyleModeObject != null)
   // { myStyleModeObject.propagateValue (this, "style", myStyle);
   // }
   // }

   // public PropertyMode getStyleMode()
   // {
   // return myStyleModeObject.get();
   // }

   // public void setStyleMode (PropertyMode mode)
   // {
   // myStyleModeObject.setAndUpdate (this, "style", mode);
   // }

   // public ModeObject getStyleModeObject()
   // {
   // return myStyleModeObject;
   // }

   public void setStyle (int style) {
      myStyle = style;
      myStyleMode =
         PropertyUtils.propagateValue (this, "style", myStyle, myStyleMode);
   }

   public PropertyMode getStyleMode() {
      return myStyleMode;
   }

   public void setStyleMode (PropertyMode mode) {
      myStyleMode =
         PropertyUtils.setModeAndUpdate (this, "style", myStyleMode, mode);
   }

   public TestNode getParent() {
      return myParent;
   }

   public boolean hasChildren() {
      return !myChildren.isEmpty();
   }

   public Iterator<TestNode> getChildren() {
      return myChildren.iterator();
   }

   public void removeChild (TestNode node) {
      myChildren.remove (node);
      node.myParent = null;
   }

   public void addChild (TestNode node) {
      node.myParent = this;
      myChildren.add (node);
      PropertyUtils.updateAllInheritedProperties (node);
   }

   public TestRenderInfo getRenderInfo() {
      return myRenderInfo;
   }

   public void setRenderInfo (TestRenderInfo info) {
      if (info != myRenderInfo) {
         PropertyUtils.updateCompositeProperty (
            this, "renderInfo", myRenderInfo, info);
         myRenderInfo = info;
      }
   }

}
