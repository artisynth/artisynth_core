/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import maspack.properties.CompositeProperty;
import maspack.properties.HasProperties;
import maspack.properties.Property;
import maspack.properties.PropertyInfo;
import maspack.properties.PropertyList;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;
import maspack.util.Clonable;
import maspack.util.IndentingPrintWriter;
import maspack.util.InternalErrorException;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import maspack.util.Scannable;

public class TextureProps implements CompositeProperty, Scannable, Clonable {
   private static final PropertyMode INHERITED = PropertyMode.Inherited;

   public enum TextureMode {
      DECAL, REPLACE, MODULATE, BLEND
   };

   public enum NormalMode {
      BUMP, NORMAL
   }

   private PropertyInfo myInfo;
   private HasProperties myHost;

   /**
    * {@inheritDoc}
    */
   public HasProperties getPropertyHost() {
      return myHost;
   }

   /**
    * {@inheritDoc}
    */
   public PropertyInfo getPropertyInfo() {
      return myInfo;
   }

   /**
    * {@inheritDoc}
    */
   public void setPropertyHost (HasProperties host) {
      myHost = host;
   }

   /**
    * {@inheritDoc}
    */
   public void setPropertyInfo (PropertyInfo info) {
      myInfo = info;
   }

   // Enabled
   // Normal Map enabled
   // Mode
   // Texture FileName
   // Normal map FileName

   protected boolean myTextureEnabledP;
   protected static boolean defaultTextureEnabledP = false;
   protected PropertyMode myTextureEnabledMode;

   // normal map
   protected boolean myNormalEnabledP;
   protected static boolean defaultNormalEnabledP = false;
   protected PropertyMode myNormalEnabledMode;

   protected TextureMode myTextureMode;
   protected static TextureMode defaultTextureMode = TextureMode.MODULATE;
   protected PropertyMode myTextureModeMode;

   protected String myTextureFileName;
   protected static String defaultTextureFileName = "";
   protected PropertyMode myTextureFileNameMode;

   protected String myNormalFileName;
   protected static String defaultNormalFileName = "";
   protected PropertyMode myNormalFileNameMode;

   protected float myNormalScale;
   protected PropertyMode myNormalScaleMode;
   protected static float defaultNormalScale = 1;

   protected NormalMode myNormalMode;
   protected PropertyMode myNormalModeMode;
   protected static NormalMode defaultNormalMode = NormalMode.NORMAL;

   public TextureProps() {
      setDefaultModes();
      setDefaultValues();
   }

   public TextureProps (TextureProps props) {
      this();
      set (props);
   }

   public static PropertyList myProps = new PropertyList (TextureProps.class);

   static {
      myProps.addInheritable (
         "textureEnabled:Inherited isTextureEnabled *", "texturing is enabled", defaultTextureEnabledP);
      myProps.addInheritable (
         "textureMode:Inherited * *", "texturing coloring mode", defaultTextureMode);
      myProps.addInheritable (
         "textureFileName * *", "name of the texture file", defaultTextureFileName);
      myProps.addInheritable (
         "normalEnabled:Inherited isNormalEnabled *", "normal map is enabled", defaultNormalEnabledP);
      myProps.addInheritable (
         "normalMode:Inherited * *", "bump/normal mode", defaultNormalMode);
      myProps.addInheritable (
         "normalFileName * *", "name of the normal map file", defaultNormalFileName);
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
      myTextureEnabledMode = INHERITED;
      myNormalEnabledMode = INHERITED;
      myTextureModeMode = INHERITED;
      myTextureFileNameMode = INHERITED;
      myNormalFileNameMode = INHERITED;
   }

   protected void setDefaultValues() {
      myTextureEnabledP = defaultTextureEnabledP;
      myTextureMode = defaultTextureMode;
      myTextureFileName = defaultTextureFileName;
      myNormalEnabledP = defaultNormalEnabledP;
      myNormalMode = defaultNormalMode;
      myNormalFileName = defaultNormalFileName;
      myNormalScale = defaultNormalScale;
   }

   // XXX update this
   protected void clearTextureData() {

   }

   // enabled

   public boolean isTextureEnabled() {
      return myTextureEnabledP;
   }

   public void setTextureEnabled (boolean enabled) {
      if (myTextureEnabledP != enabled) {
         myTextureEnabledP = enabled;
         clearTextureData();
      }
      myTextureEnabledMode =
      PropertyUtils.propagateValue (this, "textureEnabled", enabled, myTextureEnabledMode);
   }

   public PropertyMode getTextureEnabledMode() {
      return myTextureEnabledMode;
   }

   public void setTextureEnabledMode (PropertyMode mode) {
      myTextureEnabledMode =
      PropertyUtils.setModeAndUpdate (this, "textureEnabled", myTextureEnabledMode, mode);
   }

   // mode

   public TextureMode getTextureMode() {
      return myTextureMode;
   }

   public void setTextureMode (TextureMode m) {
      if (myTextureMode != m) {
         myTextureMode = m;
         clearTextureData();
      }
      myTextureModeMode = PropertyUtils.propagateValue (this, "textureMode", m, myTextureModeMode);
   }

   public PropertyMode getTextureModeMode() {
      return myTextureModeMode;
   }

   public void setTextureModeMode (PropertyMode mode) {
      myTextureModeMode =
      PropertyUtils.setModeAndUpdate (this, "textureMode", myTextureModeMode, mode);
   }

   // texture fileName

   public String getTextureFileName() {
      return myTextureFileName;
   }

   public void setTextureFileName (String name) {
      if (!name.equals (myTextureFileName)) {
         myTextureFileName = name;
         clearTextureData();
      }
      myTextureFileNameMode =
      PropertyUtils.propagateValue (this, "textureFileName", name, myTextureFileNameMode);
   }

   public boolean textureFileExists() {
      return ((new File (myTextureFileName)).isFile());
   }

   public PropertyMode getTextureFileNameMode() {
      return myTextureFileNameMode;
   }

   public void setTextureFileNameMode (PropertyMode mode) {
      myTextureFileNameMode =
      PropertyUtils.setModeAndUpdate (this, "textureFileName", myTextureFileNameMode, mode);
   }

   // normal enabled

   public boolean isNormalEnabled() {
      return myNormalEnabledP;
   }

   public void setNormalEnabled (boolean enabled) {
      if (myNormalEnabledP != enabled) {
         myNormalEnabledP = enabled;
         clearTextureData();
      }
      myNormalEnabledMode =
      PropertyUtils.propagateValue (this, "normalEnabled", enabled, myNormalEnabledMode);
   }

   public PropertyMode getNormalEnabledMode() {
      return myNormalEnabledMode;
   }

   public void setNormalEnabledMode (PropertyMode mode) {
      myNormalEnabledMode =
      PropertyUtils.setModeAndUpdate (this, "normalEnabled", myNormalEnabledMode, mode);
   }

   // normal mode

   public NormalMode getNormalMode() {
      return myNormalMode;
   }

   public void setNormalMode (NormalMode m) {
      if (myNormalMode != m) {
         myNormalMode = m;
         clearTextureData();
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

   // normal map fileName

   public String getNormalFileName() {
      return myNormalFileName;
   }

   public void setNormalFileName (String name) {
      if (!name.equals (myNormalFileName)) {
         myNormalFileName = name;
         clearTextureData();
      }
      myNormalFileNameMode =
      PropertyUtils.propagateValue (this, "normalFileName", name, myNormalFileNameMode);
   }

   public boolean normalFileExists() {
      return ((new File (myNormalFileName)).isFile());
   }

   public PropertyMode getNormalFileNameMode() {
      return myNormalFileNameMode;
   }

   public void setNormalFileNameMode (PropertyMode mode) {
      myNormalFileNameMode =
      PropertyUtils.setModeAndUpdate (this, "normalFileName", myNormalFileNameMode, mode);
   }

   // bump scale

   public float getNormalScale() {
      return myNormalScale;
   }

   public void setNormalScale (float m) {
      if (m != myNormalScale) {
         myNormalScale = m;
         clearTextureData();
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

   public void set (TextureProps props) {
      myTextureEnabledP = props.myTextureEnabledP;
      myTextureEnabledMode = props.myTextureEnabledMode;

      myTextureMode = props.myTextureMode;
      myTextureModeMode = props.myTextureModeMode;

      myTextureFileName = props.myTextureFileName;
      myTextureFileNameMode = props.myTextureFileNameMode;

      myNormalEnabledP = props.myNormalEnabledP;
      myNormalEnabledMode = props.myNormalEnabledMode;

      myNormalMode = props.myNormalMode;
      myNormalModeMode = props.myNormalModeMode;

      myNormalFileName = props.myNormalFileName;
      myNormalFileNameMode = props.myNormalFileNameMode;

      myNormalScale = props.myNormalScale;
      myNormalScaleMode = props.myNormalScaleMode;
   }

   public boolean equals (TextureProps props) {
      return (myTextureEnabledP == props.myTextureEnabledP &&
         myTextureEnabledMode == props.myTextureEnabledMode && 
         myTextureMode == props.myTextureMode &&
         myTextureModeMode == props.myTextureModeMode &&
         myTextureFileName.equals (props.myTextureFileName) &&
         myTextureFileNameMode == props.myTextureFileNameMode &&
         myNormalEnabledP == props.myNormalEnabledP &&
         myNormalEnabledMode == props.myNormalEnabledMode &&
         myNormalMode == props.myNormalMode &&
         myNormalModeMode == props.myNormalModeMode &&
         myNormalFileName.equals (props.myNormalFileName) &&
         myNormalFileNameMode == props.myNormalFileNameMode) &&
         myNormalScale == props.myNormalScale &&
         myNormalScaleMode == props.myNormalScaleMode;
   }

   public boolean equals (Object obj) {
      if (obj instanceof TextureProps) {
         return equals ((TextureProps)obj);
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
      return ("textureEnabled=" + myTextureEnabledP
      + ", textureMode=" + myTextureMode 
      + ", textureFileName=" + myTextureFileName
      + ", normalEnabled=" + myNormalEnabledP
      + ", normalMode=" + myNormalMode
      + ", normalFileName=" + myNormalFileName
      + ", normalScale=" + myNormalScale);
   }

   public TextureProps clone() {
      TextureProps props = null;
      try {
         props = (TextureProps)super.clone();
      }
      catch (CloneNotSupportedException e) {
         throw new InternalErrorException ("Cannot clone super in TextureProps");
      }

      return props;
   }

   public static void main (String[] args) {
      TextureProps props = new TextureProps();
      props.setTextureFileName ("C:\\here\\we\\go");
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
