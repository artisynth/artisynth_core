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
import maspack.render.Renderer.ColorMixing;
import maspack.util.IndentingPrintWriter;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;

public class DiffuseTextureProps extends TextureProps {

   protected ColorMixing myTextureMixing;
   protected static ColorMixing defaultTextureMixing = ColorMixing.MODULATE;
   protected PropertyMode myTextureMixingMode;

   public DiffuseTextureProps() {
      setDefaultModes();
      setDefaultValues();
   }

   public DiffuseTextureProps (DiffuseTextureProps props) {
      this();
      set (props);
   }

   public static PropertyList myProps = new PropertyList (DiffuseTextureProps.class, TextureProps.class);

   static {
      myProps.addInheritable (
         "textureMixing:Inherited * *", "texture coloring mode", defaultTextureMixing);
   }

   public Property getProperty (String name) {
      return PropertyList.getProperty (name, this);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   protected void setDefaultModes() {
      super.setDefaultModes();
      myTextureMixingMode = INHERITED;
   }

   protected void setDefaultValues() {
      super.setDefaultValues();
      myTextureMixing = defaultTextureMixing;
   }
   
   // mode
   public ColorMixing getTextureMixing() {
      return myTextureMixing;
   }

   public void setTextureMixing (ColorMixing m) {
      if (myTextureMixing != m) {
         myTextureMixing = m;
      }
      myTextureMixingMode = PropertyUtils.propagateValue (this, "textureMixing", m, myTextureMixingMode);
   }

   public PropertyMode getTextureMixingMode() {
      return myTextureMixingMode;
   }

   public void setTextureMixingMode (PropertyMode mode) {
      myTextureMixingMode =
      PropertyUtils.setModeAndUpdate (this, "textureMixing", myTextureMixingMode, mode);
   }

   public void set (DiffuseTextureProps props) {
      super.set (props);
      myTextureMixing = props.myTextureMixing;
      myTextureMixingMode = props.myTextureMixingMode;
   }

   public boolean equals (DiffuseTextureProps props) {
      return ( super.equals (props) &&  
         myTextureMixing == props.myTextureMixing &&
         myTextureMixingMode == props.myTextureMixingMode);
   }

   public boolean equals (TextureProps obj) {
      if (obj instanceof DiffuseTextureProps) {
         return equals ((DiffuseTextureProps)obj);
      } else {
         return false;
      }
   }
   
   public boolean equals (Object obj) {
      if (obj instanceof DiffuseTextureProps) {
         return equals ((DiffuseTextureProps)obj);
      } else {
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
      + ", textureMode=" + myTextureMixing);
   }

   public DiffuseTextureProps clone() {
      DiffuseTextureProps props = (DiffuseTextureProps)super.clone();
      myTextureMixing = props.myTextureMixing;
      myTextureMixingMode = props.myTextureMixingMode;
      return props;
   }

   public static void main (String[] args) {
      DiffuseTextureProps props = new DiffuseTextureProps();
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
