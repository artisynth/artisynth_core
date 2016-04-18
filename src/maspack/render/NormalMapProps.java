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

public class NormalMapProps extends TextureMapProps {

   protected float myScaling;
   protected PropertyMode myScalingMode;
   protected static float defaultScaling = 1;

   public NormalMapProps() {
      setDefaultModes();
      setDefaultValues();
   }

   public NormalMapProps (NormalMapProps props) {
      this();
      set (props);
   }

   public static PropertyList myProps = new PropertyList (NormalMapProps.class, TextureMapProps.class);

   static {
      myProps.addInheritable (
         "scaling", 
         "scale for x and y components of the normal map", defaultScaling);
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

   // bump scale
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

   public void set (NormalMapProps props) {
      super.set (props);
      myScaling = props.myScaling;
      myScalingMode = props.myScalingMode;
   }

   public boolean equals (NormalMapProps props) {
      return (super.equals (props) && 
      myScaling == props.myScaling &&
      myScalingMode == props.myScalingMode);
   }

   public boolean equals (TextureMapProps obj) {
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
      + ", scaling=" + myScaling);
   }

   public NormalMapProps clone() {
      NormalMapProps props = (NormalMapProps)super.clone();
      myScaling = props.myScaling;
      myScalingMode = props.myScalingMode;
      return props;
   }
}
