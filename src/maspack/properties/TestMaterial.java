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

public class TestMaterial implements CompositeProperty {
   double myDensity;
   PropertyMode myDensityMode;
   double myStiffness;
   PropertyMode myStiffnessMode;
   double myDamping;
   PropertyMode myDampingMode;
   TestRenderInfo myRenderInfo;
   String myFile;
   int myActivity;

   PropertyInfo myPropInfo;
   HasProperties myPropHost;

   static double DEFAULT_DENSITY = 1.0;
   static double DEFAULT_STIFFNESS = 10.0;
   static double DEFAULT_DAMPING = 0.1;
   static String DEFAULT_FILE = null;
   static int DEFAULT_ACTIVITY = 0;

   protected void setDefaultValues() {
      myDensityMode = PropertyMode.Inherited;
      myStiffnessMode = PropertyMode.Inherited;
      myDampingMode = PropertyMode.Inherited;
      myProps.setDefaultValuesAndModes (this);
   }

   static TestRenderInfo defaultRenderInfo() {
      return null; // FINISH
   }

   public static PropertyList myProps = new PropertyList (TestMaterial.class);

   static {
      myProps.addInheritable (
         "density:Explicit * *", "material density", DEFAULT_DENSITY);
      myProps.addInheritable (
         "stiffness:Inherited * *", "material stiffness", DEFAULT_STIFFNESS);
      myProps.addInheritable (
         "damping:Inherited * *", "material damping", DEFAULT_DAMPING);
      myProps.add (
         "renderInfo * *", "rendering information", defaultRenderInfo());
      myProps.addReadOnly ("quotient *", "density-stiffness quotient");
      myProps.add ("file * *", "file for storing data", DEFAULT_FILE);
      myProps.add ("activity * *", "material activity", DEFAULT_ACTIVITY);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public TestMaterial() {
      setDefaultValues();
   }

   public Property getProperty (String name) {
      return PropertyList.getProperty (name, this);
   }

   public PropertyInfo getPropertyInfo() {
      return myPropInfo;
   }

   public void setPropertyInfo (PropertyInfo info) {
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

   public double getDensity() {
      return myDensity;
   }

   public void setDensity (double d) {
      myDensity = d;
      myDensityMode =
         PropertyUtils.propagateValue (
            this, "density", myDensity, myDensityMode);
   }

   public PropertyMode getDensityMode() {
      return myDensityMode;
   }

   public void setDensityMode (PropertyMode mode) {
      myDensityMode =
         PropertyUtils.setModeAndUpdate (this, "density", myDensityMode, mode);
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

   public double getQuotient() {
      return 0;
   }

   public String getFile() {
      return myFile;
   }

   public void setFile (String file) {
      myFile = file;
   }

   public int getActivity() {
      return myActivity;
   }

   public void setActivity (int a) {
      myActivity = a;
   }

   public Object clone() throws CloneNotSupportedException {
      TestMaterial mat = (TestMaterial)super.clone();
      mat.myPropInfo = null;
      mat.myPropHost = null;
      if (mat.myRenderInfo == null) {
         setRenderInfo ((TestRenderInfo)mat.myRenderInfo.clone());
      }
      return mat;
   }

}
