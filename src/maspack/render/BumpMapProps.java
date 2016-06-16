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

public class BumpMapProps extends TextureMapProps {
   
   protected float myScaling;
   protected PropertyMode myScalingMode;
   protected static float defaultScaling = 1;
   
   public BumpMapProps() {
      setDefaultModes();
      setDefaultValues();
   }

   public BumpMapProps (BumpMapProps props) {
      this();
      set (props);
   }

   public static PropertyList myProps = 
      new PropertyList (BumpMapProps.class, TextureMapProps.class);

   static {
      myProps.addInheritable (
         "scaling", 
         "scaling for depth components of the bump map", defaultScaling);
   }

   public Property getProperty (String name) {
      return PropertyList.getProperty (name, this);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   protected void setDefaultModes() {
      super.setDefaultModes ();
      myScalingMode = INHERITED;
   }

   protected void setDefaultValues() {
      super.setDefaultValues ();
      myScaling = defaultScaling;
   }
   
   // scaling
   public float getScaling() {
      return myScaling;
   }

   public void setScaling (float m) {
      if (m != myScaling) {
         myScaling = m;
      }
      myScalingMode =
      PropertyUtils.propagateValue (this, "scaling", m, myScalingMode);
   }

   public PropertyMode getScalingMode() {
      return myScalingMode;
   }

   public void setScalingMode (PropertyMode mode) {
      myScalingMode =
      PropertyUtils.setModeAndUpdate (this, "scaling", myScalingMode, mode);
   }

   public void set (BumpMapProps props) {
      super.set (props);
      myScaling = props.myScaling;
      myScalingMode = props.myScalingMode;
   }

   public boolean equals (BumpMapProps props) {
      return (super.equals (props) && 
         myScaling == props.myScaling &&
         myScalingMode == props.myScalingMode);
   }

   public boolean equals (TextureMapProps obj) {
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
      + ", scaling=" + myScaling);
   }

   public BumpMapProps clone() {
      BumpMapProps props = (BumpMapProps)super.clone();
      myScaling = props.myScaling;
      myScalingMode = props.myScalingMode;
      return props;
   }
}
