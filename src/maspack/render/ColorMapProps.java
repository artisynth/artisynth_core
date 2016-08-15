/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

import maspack.properties.Property;
import maspack.properties.PropertyList;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;
import maspack.render.Renderer.ColorMixing;

public class ColorMapProps extends TextureMapProps {

   protected ColorMixing myColorMixing;
   protected static ColorMixing defaultColorMixing = ColorMixing.MODULATE;
   protected PropertyMode myColorMixingMode;
   protected boolean myDiffuseColoring;
   protected PropertyMode myDiffuseColoringMode;
   protected static boolean defaultDiffuseColoring = true;
   protected boolean mySpecularColoring;
   protected PropertyMode mySpecularColoringMode;
   protected static boolean defaultSpecularColoring = true;
   protected boolean myEmissionColoring;
   protected PropertyMode myEmissionColoringMode;
   protected static boolean defaultEmissionColoring = false;

   public ColorMapProps() {
      setDefaultModes();
      setDefaultValues();
   }

   public ColorMapProps (ColorMapProps props) {
      this();
      set (props);
   }

   public ColorMapProps (TextureContent content) {
      this();
      setContent (content);
   }
   
   public static PropertyList myProps = 
      new PropertyList (ColorMapProps.class, TextureMapProps.class);

   static {
      myProps.addInheritable (
         "colorMixing:Inherited", 
         "how color map should mix with underlying color", defaultColorMixing);
      myProps.addInheritable (
         "diffuseColoring:Inherited", 
         "apply color map to diffuse/ambient color", defaultDiffuseColoring);
      myProps.addInheritable (
         "specularColoring:Inherited", 
         "apply color map to specular color", defaultSpecularColoring);
      myProps.addInheritable (
         "emissionColoring:Inherited", 
         "apply color map to emission color", defaultEmissionColoring);
   }

   public Property getProperty (String name) {
      return PropertyList.getProperty (name, this);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   protected void setDefaultModes() {
      super.setDefaultModes();
      myColorMixingMode = INHERITED;
      myDiffuseColoringMode = INHERITED;
      mySpecularColoringMode = INHERITED;
      myEmissionColoringMode = INHERITED;
   }

   protected void setDefaultValues() {
      super.setDefaultValues();
      myColorMixing = defaultColorMixing;
      myDiffuseColoring = defaultDiffuseColoring;
      mySpecularColoring = defaultSpecularColoring;
      myEmissionColoring = defaultEmissionColoring;
   }
   
   // mixing
   public ColorMixing getColorMixing() {
      return myColorMixing;
   }

   public void setColorMixing (ColorMixing m) {
      if (myColorMixing != m) {
         myColorMixing = m;
      }
      myColorMixingMode = PropertyUtils.propagateValue (
         this, "colorMixing", m, myColorMixingMode);
   }

   public PropertyMode getColorMixingMode() {
      return myColorMixingMode;
   }

   public void setColorMixingMode (PropertyMode mode) {
      myColorMixingMode =
      PropertyUtils.setModeAndUpdate (
         this, "colorMixing", myColorMixingMode, mode);
   }
   
   // diffuse
   public boolean getDiffuseColoring() {
      return myDiffuseColoring;
   }

   public void setDiffuseColoring (boolean set) {
      myDiffuseColoring = set;
      myDiffuseColoringMode = PropertyUtils.propagateValue (this, "diffuseColoring", set, myDiffuseColoringMode);
   }

   public PropertyMode getDiffuseColoringMode() {
      return myDiffuseColoringMode;
   }

   public void setDiffuseColoringMode (PropertyMode mode) {
      myDiffuseColoringMode =
      PropertyUtils.setModeAndUpdate (this, "diffuseColoring", myDiffuseColoringMode, mode);
   }
   
   // specular
   public boolean getSpecularColoring() {
      return mySpecularColoring;
   }

   public void setSpecularColoring (boolean set) {
      mySpecularColoring = set;
      mySpecularColoringMode = PropertyUtils.propagateValue (this, "specularColoring", set, mySpecularColoringMode);
   }

   public PropertyMode getSpecularColoringMode() {
      return mySpecularColoringMode;
   }

   public void setSpecularColoringMode (PropertyMode mode) {
      mySpecularColoringMode =
      PropertyUtils.setModeAndUpdate (this, "specularColoring", mySpecularColoringMode, mode);
   }
   
   // emission
   public boolean getEmissionColoring() {
      return myEmissionColoring;
   }

   public void setEmissionColoring (boolean set) {
      myEmissionColoring = set;
      myEmissionColoringMode = PropertyUtils.propagateValue (this, "emissionColoring", set, myEmissionColoringMode);
   }

   public PropertyMode getEmissionColoringMode() {
      return myEmissionColoringMode;
   }

   public void setEmissionColoringMode (PropertyMode mode) {
      myEmissionColoringMode =
      PropertyUtils.setModeAndUpdate (this, "emissionColoring", myEmissionColoringMode, mode);
   }
   
   public void set (ColorMapProps props) {
      super.set (props);
      myColorMixing = props.myColorMixing;
      myColorMixingMode = props.myColorMixingMode;
      myDiffuseColoring = props.myDiffuseColoring;
      myDiffuseColoringMode = props.myDiffuseColoringMode;
      mySpecularColoring = props.mySpecularColoring;
      mySpecularColoringMode = props.mySpecularColoringMode;
      myEmissionColoring = props.myEmissionColoring;
      myEmissionColoringMode = props.myEmissionColoringMode;
   }

   public boolean equals (ColorMapProps props) {
      return ( super.equals (props) &&  
         myColorMixing == props.myColorMixing &&
         myColorMixingMode == props.myColorMixingMode &&  
         myDiffuseColoring == props.myDiffuseColoring &&
         myDiffuseColoringMode == props.myDiffuseColoringMode &&  
         mySpecularColoring == props.mySpecularColoring &&
         mySpecularColoringMode == props.mySpecularColoringMode &&
         myEmissionColoring == props.myEmissionColoring &&
         myEmissionColoringMode == props.myEmissionColoringMode);
   }

   public boolean equals (TextureMapProps obj) {
      if (obj instanceof ColorMapProps) {
         return equals ((ColorMapProps)obj);
      } else {
         return false;
      }
   }
   
   public boolean equals (Object obj) {
      if (obj instanceof ColorMapProps) {
         return equals ((ColorMapProps)obj);
      } else {
         return false;
      }
   }

   public String toString() {
      return (super.toString ()
      + ", colorMixing=" + myColorMixing
      + ", diffuseColoring=" + myDiffuseColoring
      + ", specularColoring=" + mySpecularColoring
      + ", emissionColoring=" + myEmissionColoring);
   }

   public ColorMapProps clone() {
      ColorMapProps props = (ColorMapProps)super.clone();
      myColorMixing = props.myColorMixing;
      myColorMixingMode = props.myColorMixingMode;
      myDiffuseColoring = props.myDiffuseColoring;
      myDiffuseColoringMode = props.myDiffuseColoringMode;  
      mySpecularColoring = props.mySpecularColoring;
      mySpecularColoringMode = props.mySpecularColoringMode;
      myEmissionColoring = props.myEmissionColoring;
      myEmissionColoringMode = props.myEmissionColoringMode;
      return props;
   }

}
