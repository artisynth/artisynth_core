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

public class BumpMapProps extends TexturePropsBase {
   
   protected float myBumpScale;
   protected PropertyMode myBumpScaleMode;
   protected static float defaultBumpScale = 1;
   
   public BumpMapProps() {
      setDefaultModes();
      setDefaultValues();
   }

   public BumpMapProps (BumpMapProps props) {
      this();
      set (props);
   }

   public static PropertyList myProps = new PropertyList (BumpMapProps.class, TexturePropsBase.class);

   static {
      myProps.addInheritable (
         "bumpScale * *", "scale for x and y components of the Bump map", defaultBumpScale);
   }

   public Property getProperty (String name) {
      return PropertyList.getProperty (name, this);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   protected void setDefaultModes() {
      super.setDefaultModes ();
      myBumpScaleMode = INHERITED;
   }

   protected void setDefaultValues() {
      super.setDefaultValues ();
      myBumpScale = defaultBumpScale;
   }
   
   // bump scale
   public float getBumpScale() {
      return myBumpScale;
   }

   public void setBumpScale (float m) {
      if (m != myBumpScale) {
         myBumpScale = m;
      }
      myBumpScaleMode =
      PropertyUtils.propagateValue (this, "bumpScale", m, myBumpScaleMode);
   }

   public PropertyMode getBumpScaleMode() {
      return myBumpScaleMode;
   }

   public void setBumpScaleMode (PropertyMode mode) {
      myBumpScaleMode =
      PropertyUtils.setModeAndUpdate (this, "bumpScale", myBumpScaleMode, mode);
   }

   public void set (BumpMapProps props) {
      super.set (props);
      myBumpScale = props.myBumpScale;
      myBumpScaleMode = props.myBumpScaleMode;
   }

   public boolean equals (BumpMapProps props) {
      return (super.equals (props) && 
         myBumpScale == props.myBumpScale &&
         myBumpScaleMode == props.myBumpScaleMode);
   }

   public boolean equals (TexturePropsBase obj) {
      if (obj instanceof BumpMapProps) {
         return equals ((BumpMapProps)obj);
      } else {
         return false;
      }
   }
   
   public boolean equals (Object obj) {
      if (obj instanceof BumpMapProps) {
         return equals ((BumpMapProps)obj);
      }
      else {
         return false;
      }
   }

   public String toString() {
      return (super.toString ()
      + ", BumpScale=" + myBumpScale);
   }

   public BumpMapProps clone() {
      BumpMapProps props = (BumpMapProps)super.clone();
      myBumpScale = props.myBumpScale;
      myBumpScaleMode = props.myBumpScaleMode;
      return props;
   }
}
