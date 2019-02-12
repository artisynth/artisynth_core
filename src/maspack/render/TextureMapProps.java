/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

import java.awt.Color;
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

public abstract class TextureMapProps implements CompositeProperty, Scannable, Clonable {
   protected static final PropertyMode INHERITED = PropertyMode.Inherited;

   private PropertyInfo myInfo;
   private HasProperties myHost;

   public enum TextureFilter {
      NEAREST, 
      LINEAR,
      NEAREST_MIPMAP_NEAREST, 
      LINEAR_MIPMAP_NEAREST, 
      NEAREST_MIPMAP_LINEAR, 
      LINEAR_MIPMAP_LINEAR
   }

   public enum TextureWrapping {
      REPEAT,
      MIRRORED_REPEAT,
      CLAMP_TO_EDGE,
      CLAMP_TO_BORDER
   }

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
   // FileName
   protected boolean myEnabledP;
   protected static boolean defaultEnabledP = false;
   protected PropertyMode myEnabledMode;

   protected TextureContent myContent;
   
   protected String myFileName;
   protected static String defaultFileName = "";
   protected PropertyMode myFileNameMode;

   protected TextureWrapping mySWrapping;
   protected static TextureWrapping defaultSWrapping = TextureWrapping.REPEAT;
   protected PropertyMode mySWrappingMode;

   protected TextureWrapping myTWrapping;
   protected static TextureWrapping defaultTWrapping = TextureWrapping.REPEAT;
   protected PropertyMode myTWrappingMode;

   protected TextureFilter myMinFilter;
   protected static TextureFilter defaultMinFilter = TextureFilter.NEAREST_MIPMAP_LINEAR;
   protected PropertyMode myMinFilterMode;

   protected TextureFilter myMagFilter;
   protected static TextureFilter defaultMagFilter = TextureFilter.LINEAR;
   protected PropertyMode myMagFilterMode;
   
   protected Color myBorderColor;
   protected static Color defaultBorderColor = new Color (0, 0, 0, 0);  // see-through black
   protected PropertyMode myBorderColorMode;

   public TextureMapProps() {
      setDefaultModes();
      setDefaultValues();
   }

   public TextureMapProps (TextureMapProps props) {
      this();
      set (props);
   }

   public static PropertyList myProps = new PropertyList (TextureMapProps.class);

   static {
      myProps.addInheritable (
         "enabled:Inherited isEnabled *", "texturing is enabled", defaultEnabledP);
      myProps.addInheritable (
         "fileName * *", "name of the texture file", defaultFileName);
      myProps.addInheritable (
         "sWrapping * *", "wrapping of s texture coordinate", defaultSWrapping);
      myProps.addInheritable (
         "tWrapping * *", "wrapping of t texture coordinate", defaultTWrapping);
      myProps.addInheritable (
         "minFilter * *", "minifying filter", defaultMinFilter);
      myProps.addInheritable (
         "magFilter * *", "magnifying filter", defaultMagFilter);
      myProps.addInheritable (
         "borderColor * *", "border color for clamping", defaultBorderColor);
   }

   public Property getProperty (String name) {
      return PropertyList.getProperty (name, this);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   protected void setDefaultModes() {
      myEnabledMode = INHERITED;
      myFileNameMode = INHERITED;
      mySWrappingMode = INHERITED;
      myTWrappingMode = INHERITED;
      myMinFilterMode = INHERITED;
      myMagFilterMode = INHERITED;
      myBorderColorMode = INHERITED;
   }

   protected void setDefaultValues() {
      myEnabledP = defaultEnabledP;
      myFileName = defaultFileName;
      mySWrapping = defaultSWrapping;
      myTWrapping = defaultTWrapping;
      myMinFilter = defaultMinFilter;
      myMagFilter = defaultMagFilter;
      myBorderColor = defaultBorderColor;
      myContent = null;
   }

   // enabled
   public boolean isEnabled() {
      return myEnabledP;
   }

   public void setEnabled (boolean enabled) {
      if (myEnabledP != enabled) {
         myEnabledP = enabled;
      }
      myEnabledMode =
      PropertyUtils.propagateValue (this, "enabled", enabled, myEnabledMode);
   }

   public PropertyMode getEnabledMode() {
      return myEnabledMode;
   }

   public void setEnabledMode (PropertyMode mode) {
      myEnabledMode = PropertyUtils.setModeAndUpdate (this, "enabled", myEnabledMode, mode);
   }
   
   public void setContent(TextureContent content) {
      setFileName (defaultFileName);
      myContent = content.acquire();  // keep reference
   }
  
   /**
    * Grab a copy of the content (raw or file)
    * @return copy of the content
    */
   public TextureContent getContent() {
      return myContent;
   }

   // texture fileName
   public String getFileName() {
      return myFileName;
   }

   public void setFileName (String name) {
      if ( (name == null && myFileName != null) || !name.equals (myFileName)) {
        
         if (myContent != null) {
            myContent.release ();
         }
         
         myFileName = name;
         if (name == null) {
            myContent = null;
         } else {
            myContent = (new TextureContentFile (myFileName)).acquire ();
         }
         
      }
      myFileNameMode = PropertyUtils.propagateValue (this, "fileName", name, myFileNameMode);
   }

   public boolean textureFileExists() {
      return ((new File (myFileName)).isFile());
   }

   public PropertyMode getFileNameMode() {
      return myFileNameMode;
   }

   public void setFileNameMode (PropertyMode mode) {
      myFileNameMode =
      PropertyUtils.setModeAndUpdate (this, "fileName", myFileNameMode, mode);
   }
   
   // s-wrapping
   public TextureWrapping getSWrapping() {
      return mySWrapping;
   }

   public void setSWrapping (TextureWrapping wrapping) {
      if (mySWrapping != wrapping) {
         mySWrapping = wrapping;
      }
      mySWrappingMode = PropertyUtils.propagateValue (this, "sWrapping", wrapping, mySWrappingMode);
   }

   public PropertyMode getSWrappingMode() {
      return mySWrappingMode;
   }

   public void setSWrappingMode (PropertyMode mode) {
      mySWrappingMode = PropertyUtils.setModeAndUpdate (this, "sWrapping", mySWrappingMode, mode);
   }
   
   // t-wrapping
   public TextureWrapping getTWrapping() {
      return myTWrapping;
   }

   public void setTWrapping (TextureWrapping wrapping) {
      if (myTWrapping != wrapping) {
         myTWrapping = wrapping;
      }
      myTWrappingMode = PropertyUtils.propagateValue (this, "tWrapping", wrapping, myTWrappingMode);
   }

   public PropertyMode getTWrappingMode() {
      return myTWrappingMode;
   }

   public void setTWrappingMode (PropertyMode mode) {
      myTWrappingMode = PropertyUtils.setModeAndUpdate (this, "tWrapping", myTWrappingMode, mode);
   }   
   
   // minifying-filter
   public TextureFilter getMinFilter() {
      return myMinFilter;
   }

   public void setMinFilter (TextureFilter filter) {
      if (myMinFilter != filter) {
         myMinFilter = filter;
      }
      myMinFilterMode = PropertyUtils.propagateValue (this, "minFilter", filter, myMinFilterMode);
   }

   public PropertyMode getMinFilterMode() {
      return myMinFilterMode;
   }

   public void setMinFilterMode (PropertyMode mode) {
      myMinFilterMode = PropertyUtils.setModeAndUpdate (this, "minFilter", myMinFilterMode, mode);
   }

   // magnifying-filter
   public TextureFilter getMagFilter() {
      return myMagFilter;
   }

   public void setMagFilter (TextureFilter filter) {
      // remove mipmap for magnifying filter
      switch (filter) {
         case LINEAR:
         case LINEAR_MIPMAP_LINEAR:
         case NEAREST_MIPMAP_LINEAR:
            filter = TextureFilter.LINEAR;
            break;
         case NEAREST:
         case LINEAR_MIPMAP_NEAREST:
         case NEAREST_MIPMAP_NEAREST:
            filter = TextureFilter.NEAREST;
            break;
      }
      
      if (myMagFilter != filter) {
         myMagFilter = filter;
      }
      myMagFilterMode = PropertyUtils.propagateValue (this, "magFilter", filter, myMagFilterMode);
   }

   public PropertyMode getMagFilterMode() {
      return myMagFilterMode;
   }

   public void setMagFilterMode (PropertyMode mode) {
      myMagFilterMode = PropertyUtils.setModeAndUpdate (this, "magFilter", myMagFilterMode, mode);
   }
   
   // border color
   public Color getBorderColor() {
      return myBorderColor;
   }

   public void setBorderColor(Color c) {
      if (myBorderColor != c) {
         myBorderColor = c;
      }
      myBorderColorMode = PropertyUtils.propagateValue (this, "borderColor", myBorderColor, myBorderColorMode);
   }

   public PropertyMode getBorderColorMode() {
      return myBorderColorMode;
   }

   public void setBorderColorMode (PropertyMode mode) {
      myBorderColorMode = PropertyUtils.setModeAndUpdate (this, "borderColor", myBorderColorMode, mode);
   }
   
   public void set (TextureMapProps props) {
      myEnabledP = props.myEnabledP;
      myEnabledMode = props.myEnabledMode;
      
      myFileName = props.myFileName;
      myFileNameMode = props.myFileNameMode;

      if (props.myContent != null) {
         myContent = props.myContent.acquire ();
      } else {
         myContent = null;
      }
      
      mySWrapping = props.mySWrapping;
      mySWrappingMode = props.mySWrappingMode;
      myTWrapping = props.myTWrapping;
      myTWrappingMode = props.myTWrappingMode;
      
      myMinFilter = props.myMinFilter;
      myMinFilterMode = props.myMinFilterMode;
      myMagFilter = props.myMagFilter;
      myMagFilterMode = props.myMagFilterMode;
      
      myBorderColor = props.myBorderColor;
      myBorderColorMode = props.myBorderColorMode;
   }

   public boolean equals (TextureMapProps props) {
      return (myEnabledP == props.myEnabledP &&
      myEnabledMode == props.myEnabledMode && 
      myContent == props.myContent && 
      myFileName.equals (props.myFileName) &&
      myFileNameMode == props.myFileNameMode &&
      mySWrapping == props.mySWrapping &&
      mySWrappingMode == props.mySWrappingMode &&
      myTWrapping == props.myTWrapping &&
      myTWrappingMode == props.myTWrappingMode &&
      myMinFilter == props.myMinFilter &&
      myMinFilterMode == props.myMinFilterMode &&
      myMagFilter == props.myMagFilter &&
      myMagFilterMode == props.myMagFilterMode && 
      myBorderColor.equals (props.myBorderColor) && 
      myBorderColorMode == props.myBorderColorMode);
   }
   
   public void dispose() {
      if (myContent != null) {
         myContent.release ();
         myContent = null;
      }
   }
   
   @Override
   protected void finalize() {
     dispose ();
   };

   public boolean equals (Object obj) {
      if (obj instanceof TextureMapProps) {
         return equals ((TextureMapProps)obj);
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
      getAllPropertyInfo().writeNonDefaultProps (this, pw, fmt, ref);
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
      return ("enabled=" + myEnabledP
              + ", fileName=" + myFileName
              + ", sWrapping=" + mySWrapping
              + ", tWrapping=" + myTWrapping
              + ", minFilter=" + myMinFilter
              + ", magFilter=" + myMagFilter
              + ", borderColor=" + myBorderColor);
   }

   public TextureMapProps clone() {
      TextureMapProps props = null;
      try {
         props = (TextureMapProps)super.clone();
      }
      catch (CloneNotSupportedException e) {
         throw new InternalErrorException ("Cannot clone super in TextureProps");
      }
      set(props);
      return props;
   }

}
