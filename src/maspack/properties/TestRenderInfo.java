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

public class TestRenderInfo implements CompositeProperty, Cloneable {
   Color myColor;
   PropertyMode myColorMode;
   double myShine;
   PropertyMode myShineMode;
   int myWidth;
   PropertyMode myWidthMode;
   String myTextureFile;

   PropertyInfo myPropInfo;
   HasProperties myPropHost;

   public static PropertyList myProps = new PropertyList (TestRenderInfo.class);

   static Color defaultColor() {
      return new Color (0.5f, 0.5f, 0.5f);
   }

   static double DEFAULT_SHINE = 100;
   static int DEFAULT_WIDTH = 1;
   static String DEFAULT_TEXTURE_FILE = null;

   protected void setDefaultValues() {
      myColorMode = PropertyMode.Inherited;
      myShineMode = PropertyMode.Inherited;
      myWidthMode = PropertyMode.Inherited;
      myProps.setDefaultValuesAndModes (this);
      // setColor (defaultColor());

      // setShine (DEFAULT_SHINE);

      // setWidth (DEFAULT_WIDTH);

      // setTextureFile (DEFAULT_TEXTURE_FILE);
   }

   static {
      myProps.addInheritable (
         "color:Inherited * *", "rendering color", defaultColor());
      myProps.addInheritable (
         "shine:Inherited * *", "rendering shine", DEFAULT_SHINE);
      myProps.addInheritable ("width * *", "line width", DEFAULT_WIDTH);
      myProps.add ("textureFile * *", "texture file", DEFAULT_TEXTURE_FILE);
   };

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public TestRenderInfo() {
      setDefaultValues();
   }

   public Property getProperty (String name) {
      return PropertyList.getProperty (name, this);
   }

   public PropertyInfo getPropertyInfo() {
      return myPropInfo;
   }

   public void setPropertyInfo (PropertyInfo info) {
      if (info == null) {
         throw new IllegalArgumentException ("info is null");
      }
      myPropInfo = info;
   }

   public HasProperties getPropertyHost() {
      return myPropHost;
   }

   public void setPropertyHost (HasProperties newParent) {
      if (newParent != null && !(newParent instanceof HierarchyNode) &&
          !(newParent instanceof CompositeProperty)) {
         throw new IllegalArgumentException (
            "parent not a HierarchyNode or HasInheritableProperties");
      }
      myPropHost = newParent;
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

   public double getShine() {
      return myShine;
   }

   public void setShine (double shine) {
      myShine = shine;
      myShineMode =
         PropertyUtils.propagateValue (this, "shine", myShine, myShineMode);
   }

   public PropertyMode getShineMode() {
      return myShineMode;
   }

   public void setShineMode (PropertyMode mode) {
      myShineMode =
         PropertyUtils.setModeAndUpdate (this, "shine", myShineMode, mode);
   }

   public int getWidth() {
      return myWidth;
   }

   public void setWidth (int width) {
      myWidth = width;
      myWidthMode =
         PropertyUtils.propagateValue (this, "width", myWidth, myWidthMode);
   }

   public PropertyMode getWidthMode() {
      return myWidthMode;
   }

   public void setWidthMode (PropertyMode mode) {
      myWidthMode =
         PropertyUtils.setModeAndUpdate (this, "width", myWidthMode, mode);
   }

   public String getTextureFile() {
      return myTextureFile;
   }

   public void setTextureFile (String textureFile) {
      myTextureFile = textureFile;
   }

   public void set (TestRenderInfo info) {
      myColor = info.myColor;
      myColorMode = info.myColorMode;
      myShine = info.myShine;
      myShineMode = info.myShineMode;
      myWidth = info.myWidth;
      myWidthMode = info.myWidthMode;
      myTextureFile = info.myTextureFile;
   }

   public Object clone() throws CloneNotSupportedException {
      TestRenderInfo info = (TestRenderInfo)super.clone();
      info.myPropInfo = null;
      info.myPropHost = null;
      return info;
   }
}
