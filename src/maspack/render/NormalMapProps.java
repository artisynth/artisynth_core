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

public class NormalMapProps extends TexturePropsBase {

   protected float myNormalScale;
   protected PropertyMode myNormalScaleMode;
   protected static float defaultNormalScale = 1;

   public NormalMapProps() {
      setDefaultModes();
      setDefaultValues();
   }

   public NormalMapProps (NormalMapProps props) {
      this();
      set (props);
   }

   public static PropertyList myProps = new PropertyList (NormalMapProps.class, TexturePropsBase.class);

   static {
      myProps.addInheritable (
         "normalScale * *", "scale for x and y components of the Normal map", defaultNormalScale);
   }

   public Property getProperty (String name) {
      return PropertyList.getProperty (name, this);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   protected void setDefaultModes() {
      super.setDefaultModes ();
      myNormalScaleMode = INHERITED;
   }

   protected void setDefaultValues() {
      super.setDefaultValues ();
      myNormalScale = defaultNormalScale;
   }

   // bump scale
   public float getNormalScale() {
      return myNormalScale;
   }

   public void setNormalScale (float m) {
      if (m != myNormalScale) {
         myNormalScale = m;
      }
      myNormalScaleMode =
      PropertyUtils.propagateValue (this, "normalScale", m, myNormalScaleMode);
   }

   public PropertyMode getNormalScaleMode() {
      return myNormalScaleMode;
   }

   public void setNormalScaleMode (PropertyMode mode) {
      myNormalScaleMode =
      PropertyUtils.setModeAndUpdate (this, "normalScale", myNormalScaleMode, mode);
   }

   public void set (NormalMapProps props) {
      super.set (props);
      myNormalScale = props.myNormalScale;
      myNormalScaleMode = props.myNormalScaleMode;
   }

   public boolean equals (NormalMapProps props) {
      return (super.equals (props) && 
      myNormalScale == props.myNormalScale &&
      myNormalScaleMode == props.myNormalScaleMode);
   }

   public boolean equals (TexturePropsBase obj) {
      if (obj instanceof NormalMapProps) {
         return equals ((NormalMapProps)obj);
      } else {
         return false;
      }
   }

   public boolean equals (Object obj) {
      if (obj instanceof NormalMapProps) {
         return equals ((NormalMapProps)obj);
      }
      else {
         return false;
      }
   }

   public String toString() {
      return (super.toString ()
      + ", NormalScale=" + myNormalScale);
   }

   public NormalMapProps clone() {
      NormalMapProps props = (NormalMapProps)super.clone();
      myNormalScale = props.myNormalScale;
      myNormalScaleMode = props.myNormalScaleMode;
      return props;
   }
}
