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

public class TextureMapProps extends TexturePropsBase {

   protected ColorMixing myTextureColorMixing;
   protected static ColorMixing defaultTextureMixing = ColorMixing.MODULATE;
   protected PropertyMode myTextureColorMixingMode;
   protected boolean myDiffuseColoring;
   protected PropertyMode myDiffuseColoringMode;
   protected static boolean defaultDiffuseColoring = true;
   protected boolean mySpecularColoring;
   protected PropertyMode mySpecularColoringMode;
   protected static boolean defaultSpecularColoring = true;

   public TextureMapProps() {
      setDefaultModes();
      setDefaultValues();
   }

   public TextureMapProps (TextureMapProps props) {
      this();
      set (props);
   }

   public static PropertyList myProps = new PropertyList (TextureMapProps.class, TexturePropsBase.class);

   static {
      myProps.addInheritable (
         "textureColorMixing:Inherited * *", "texture coloring mode", defaultTextureMixing);
      myProps.addInheritable (
         "diffuseColoring:Inherited * *", "apply texture to diffuse/ambient color", defaultDiffuseColoring);
      myProps.addInheritable (
         "specularColoring:Inherited * *", "apply texture to specular color", defaultSpecularColoring);
   }

   public Property getProperty (String name) {
      return PropertyList.getProperty (name, this);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   protected void setDefaultModes() {
      super.setDefaultModes();
      myTextureColorMixingMode = INHERITED;
      myDiffuseColoringMode = INHERITED;
      mySpecularColoringMode = INHERITED;
   }

   protected void setDefaultValues() {
      super.setDefaultValues();
      myTextureColorMixing = defaultTextureMixing;
      myDiffuseColoring = defaultDiffuseColoring;
      mySpecularColoring = defaultSpecularColoring;
   }
   
   // mixing
   public ColorMixing getTextureColorMixing() {
      return myTextureColorMixing;
   }

   public void setTextureColorMixing (ColorMixing m) {
      if (myTextureColorMixing != m) {
         myTextureColorMixing = m;
      }
      myTextureColorMixingMode = PropertyUtils.propagateValue (this, "textureColorMixing", m, myTextureColorMixingMode);
   }

   public PropertyMode getTextureColorMixingMode() {
      return myTextureColorMixingMode;
   }

   public void setTextureColorMixingMode (PropertyMode mode) {
      myTextureColorMixingMode =
      PropertyUtils.setModeAndUpdate (this, "textureColorMixing", myTextureColorMixingMode, mode);
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
   
   public void set (TextureMapProps props) {
      super.set (props);
      myTextureColorMixing = props.myTextureColorMixing;
      myTextureColorMixingMode = props.myTextureColorMixingMode;
      myDiffuseColoring = props.myDiffuseColoring;
      myDiffuseColoringMode = props.myDiffuseColoringMode;
      mySpecularColoring = props.mySpecularColoring;
      mySpecularColoringMode = props.mySpecularColoringMode;
   }

   public boolean equals (TextureMapProps props) {
      return ( super.equals (props) &&  
         myTextureColorMixing == props.myTextureColorMixing &&
         myTextureColorMixingMode == props.myTextureColorMixingMode &&  
         myDiffuseColoring == props.myDiffuseColoring &&
         myDiffuseColoringMode == props.myDiffuseColoringMode &&  
         mySpecularColoring == props.mySpecularColoring &&
         mySpecularColoringMode == props.mySpecularColoringMode);
   }

   public boolean equals (TexturePropsBase obj) {
      if (obj instanceof TextureMapProps) {
         return equals ((TextureMapProps)obj);
      } else {
         return false;
      }
   }
   
   public boolean equals (Object obj) {
      if (obj instanceof TextureMapProps) {
         return equals ((TextureMapProps)obj);
      } else {
         return false;
      }
   }

   public String toString() {
      return (super.toString ()
      + ", textureColorMixing=" + myTextureColorMixing
      + ", diffuseColoring=" + myDiffuseColoring
      + ", specularColoring=" + mySpecularColoring);
   }

   public TextureMapProps clone() {
      TextureMapProps props = (TextureMapProps)super.clone();
      myTextureColorMixing = props.myTextureColorMixing;
      myTextureColorMixingMode = props.myTextureColorMixingMode;
      myDiffuseColoring = props.myDiffuseColoring;
      myDiffuseColoringMode = props.myDiffuseColoringMode;  
      mySpecularColoring = props.mySpecularColoring;
      mySpecularColoringMode = props.mySpecularColoringMode;
      return props;
   }

}
