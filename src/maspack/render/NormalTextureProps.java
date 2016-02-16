/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

import java.io.IOException;
import java.io.PrintWriter;

import maspack.properties.Property;
import maspack.properties.PropertyList;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;
import maspack.util.IndentingPrintWriter;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;

public class NormalTextureProps extends TextureProps {
   private static final PropertyMode INHERITED = PropertyMode.Inherited;

   public enum NormalMode {
      BUMP, NORMAL
   }

   protected float myNormalScale;
   protected PropertyMode myNormalScaleMode;
   protected static float defaultNormalScale = 1;

   protected NormalMode myNormalMode;
   protected PropertyMode myNormalModeMode;
   protected static NormalMode defaultNormalMode = NormalMode.NORMAL;
   
   public NormalTextureProps() {
      setDefaultModes();
      setDefaultValues();
   }

   public NormalTextureProps (NormalTextureProps props) {
      this();
      set (props);
   }

   public static PropertyList myProps = new PropertyList (NormalTextureProps.class, TextureProps.class);

   static {
      myProps.addInheritable (
         "normalMode:Inherited * *", "bump/normal mode", defaultNormalMode);
      myProps.addInheritable (
         "normalScale * *", "scale for x and y components (bump) of the normal map", defaultNormalScale);
   }

   public Property getProperty (String name) {
      return PropertyList.getProperty (name, this);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   protected void setDefaultModes() {
      super.setDefaultModes ();
      myNormalModeMode = INHERITED;
      myNormalScaleMode = INHERITED;
   }

   protected void setDefaultValues() {
      super.setDefaultValues ();
      myNormalMode = defaultNormalMode;
      myNormalScale = defaultNormalScale;
   }
   // normal mode
   public NormalMode getNormalMode() {
      return myNormalMode;
   }

   public void setNormalMode (NormalMode m) {
      if (myNormalMode != m) {
         myNormalMode = m;
      }
      myNormalModeMode = PropertyUtils.propagateValue (this, "normalMode", m, myNormalModeMode);
   }

   public PropertyMode getNormalModeMode() {
      return myNormalModeMode;
   }

   public void setNormalModeMode (PropertyMode mode) {
      myNormalModeMode =
      PropertyUtils.setModeAndUpdate (this, "normalMode", myNormalModeMode, mode);
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

   public void set (NormalTextureProps props) {
      super.set (props);
      myNormalMode = props.myNormalMode;
      myNormalModeMode = props.myNormalModeMode;

      myNormalScale = props.myNormalScale;
      myNormalScaleMode = props.myNormalScaleMode;
   }

   public boolean equals (NormalTextureProps props) {
      return (super.equals (props) && 
         myNormalMode == props.myNormalMode &&
         myNormalModeMode == props.myNormalModeMode &&
         myNormalScale == props.myNormalScale &&
         myNormalScaleMode == props.myNormalScaleMode);
   }

   public boolean equals (TextureProps obj) {
      if (obj instanceof NormalTextureProps) {
         return equals ((NormalTextureProps)obj);
      } else {
         return false;
      }
   }
   
   public boolean equals (Object obj) {
      if (obj instanceof NormalTextureProps) {
         return equals ((NormalTextureProps)obj);
      }
      else {
         return false;
      }
   }

   /**
    * {@inheritDoc}
    */
   public boolean isWritable() {
      return true;
   }

   public void write (PrintWriter pw, NumberFormat fmt, Object ref)
   throws IOException {
      IndentingPrintWriter.printOpening (pw, "[ ");
      IndentingPrintWriter.addIndentation (pw, 2);
      myProps.writeNonDefaultProps (this, pw, fmt);
      IndentingPrintWriter.addIndentation (pw, -2);
      pw.println ("]");
   }

   public void scan (ReaderTokenizer rtok, Object ref) throws IOException {
      setDefaultValues();
      rtok.scanToken ('[');
      while (rtok.nextToken() != ']') {
         rtok.pushBack();
         if (!getAllPropertyInfo().scanProp (this, rtok)) {
            throw new IOException ("unexpected input: " + rtok);
         }
      }
   }

   public String toString() {
      return (super.toString ()
      + ", normalMode=" + myNormalMode
      + ", normalScale=" + myNormalScale);
   }

   public NormalTextureProps clone() {
      NormalTextureProps props = (NormalTextureProps)super.clone();
      myNormalMode = props.myNormalMode;
      myNormalModeMode = props.myNormalModeMode;

      myNormalScale = props.myNormalScale;
      myNormalScaleMode = props.myNormalScaleMode;
      return props;
   }

   public static void main (String[] args) {
      NormalTextureProps props = new NormalTextureProps();
      props.setFileName ("C:\\here\\we\\go");
      IndentingPrintWriter pw = new IndentingPrintWriter (System.out);
      try {
         props.write (pw, new NumberFormat ("%g"), null);
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      pw.flush();
   }

}
