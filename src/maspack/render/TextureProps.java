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
import java.util.Deque;

import maspack.properties.CompositeProperty;
import maspack.properties.HasProperties;
import maspack.properties.Property;
import maspack.properties.PropertyInfo;
import maspack.properties.PropertyList;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;
import maspack.util.ArraySupport;
import maspack.util.IndentingPrintWriter;
import maspack.util.InternalErrorException;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import maspack.util.Scannable;
import maspack.util.Clonable;

public class TextureProps implements CompositeProperty, Scannable, Clonable {
   private static final PropertyMode INHERITED = PropertyMode.Inherited;

   public enum Mode {
      DECAL, REPLACE, MODULATE, BLEND
   };

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
   // Mode
   // FileName
   // SphereMapping
   // Automatic
   // SCoords
   // TCoords

   protected boolean myEnabledP;
   protected static boolean defaultEnabledP = false;
   protected PropertyMode myEnabledMode;

   protected Mode myMode;
   protected static Mode defaultMode = Mode.MODULATE;
   protected PropertyMode myModeMode;

   protected String myFileName;
   protected static String defaultFileName = "";
   protected PropertyMode myFileNameMode;

   protected boolean mySphereMappingP;
   protected static boolean defaultSphereMappingP = false;
   protected PropertyMode mySphereMappingMode;

   protected boolean myAutomaticP;
   protected static boolean defaultAutomaticP = false;
   protected PropertyMode myAutomaticMode;

   protected double[] mySCoords;
   protected static double[] defaultSCoords = new double[] { 0.01, 0, 0, 0 };
   protected PropertyMode mySCoordsMode;

   protected double[] myTCoords;
   protected static double[] defaultTCoords = new double[] { 0, 0.01, 0, 0 };
   protected PropertyMode myTCoordsMode;

   protected Texture myTexture = null;

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
         "enabled:Inherited isEnabled *", "texturing is enabled",defaultEnabledP);
      myProps.addInheritable (
         "mode:Inherited * *", "texturing mode", defaultMode);
      myProps.addInheritable (
         "fileName * *", "name of the texture file", defaultFileName);
      myProps.addInheritable (
         "sphereMapping:Inherited isSphereMappingEnabled setSphereMappingEnabled",
         "use spherical mapping for automatic texturing",
         defaultSphereMappingP);
      myProps.addInheritable (
         "automatic:Inherited isAutomatic *", "automatic texturing enabled",
         defaultAutomaticP);
      myProps.addInheritable (
         "sCoords:Inherited * *", "s coordinates for automatic texturing",
         defaultSCoords);
      myProps.addInheritable (
         "tCoords:Inherited * *", "t coordinates for automatic texturing",
         defaultTCoords);
   }

   public Property getProperty (String name) {
      return getAllPropertyInfo().getProperty (name, this);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   protected void setDefaultModes() {
      myEnabledMode = INHERITED;
      myModeMode = INHERITED;
      myFileNameMode = INHERITED;
      mySphereMappingMode = INHERITED;
      myAutomaticMode = INHERITED;
      mySCoordsMode = INHERITED;
      myTCoordsMode = INHERITED;
   }

   protected void setDefaultValues() {
      myEnabledP = defaultEnabledP;
      myMode = defaultMode;
      myFileName = defaultFileName;
      mySphereMappingP = defaultSphereMappingP;
      myAutomaticP = defaultAutomaticP;
      mySCoords = defaultSCoords;
      myTCoords = defaultTCoords;
   }

   public void clearTextureData() {
      myTexture = null;
      if (myHost instanceof RenderProps) {
         ((RenderProps)myHost).clearMeshDisplayList();
      }
   }

   // enabled

   public boolean isEnabled() {
      return myEnabledP;
   }

   public void setEnabled (boolean enabled) {
      if (myEnabledP != enabled) {
         myEnabledP = enabled;
         clearTextureData();
      }
      myEnabledMode =
         PropertyUtils.propagateValue (this, "enabled", enabled, myEnabledMode);
   }

   public PropertyMode getEnabledMode() {
      return myEnabledMode;
   }

   public void setEnabledMode (PropertyMode mode) {
      myEnabledMode =
         PropertyUtils.setModeAndUpdate (this, "enabled", myEnabledMode, mode);
   }

   // mode

   public Mode getMode() {
      return myMode;
   }

   public void setMode (Mode m) {
      if (myMode != m) {
         myMode = m;
         clearTextureData();
      }
      myModeMode = PropertyUtils.propagateValue (this, "mode", m, myModeMode);
   }

   public PropertyMode getModeMode() {
      return myModeMode;
   }

   public void setModeMode (PropertyMode mode) {
      myModeMode =
         PropertyUtils.setModeAndUpdate (this, "mode", myModeMode, mode);
   }

   // fileName

   public String getFileName() {
      return myFileName;
   }

   public void setFileName (String name) {
      if (!name.equals (myFileName)) {
         myFileName = name;
         clearTextureData();
      }
      myFileNameMode =
         PropertyUtils.propagateValue (this, "fileName", name, myFileNameMode);
   }

   public boolean textureFileExists() {
      return ((new File (myFileName)).isFile());
   }

   public Texture getTexture() {
      return myTexture;
   }

   public void setTexture (Texture texture) {
      myTexture = texture;
   }

   public PropertyMode getFileNameMode() {
      return myFileNameMode;
   }

   public void setFileNameMode (PropertyMode mode) {
      myFileNameMode =
         PropertyUtils.setModeAndUpdate (this, "fileName", myFileNameMode, mode);
   }

   // sphereMapping

   public boolean isSphereMappingEnabled() {
      return mySphereMappingP;
   }

   public void setSphereMappingEnabled (boolean enabled) {
      if (mySphereMappingP != enabled) {
         mySphereMappingP = enabled;
         clearTextureData();
      }
      mySphereMappingMode =
         PropertyUtils.propagateValue (
            this, "sphereMapping", enabled, mySphereMappingMode);
   }

   public PropertyMode getSphereMappingMode() {
      return mySphereMappingMode;
   }

   public void setSphereMappingMode (PropertyMode mode) {
      mySphereMappingMode =
         PropertyUtils.setModeAndUpdate (
            this, "sphereMapping", mySphereMappingMode, mode);
   }

   // automatic

   public boolean isAutomatic() {
      return myAutomaticP;
   }

   public void setAutomatic (boolean enabled) {
      if (myAutomaticP != enabled) {
         myAutomaticP = enabled;
         clearTextureData();
      }
      myAutomaticMode =
         PropertyUtils.propagateValue (
            this, "automatic", enabled, myAutomaticMode);
   }

   public PropertyMode getAutomaticMode() {
      return myAutomaticMode;
   }

   public void setAutomaticMode (PropertyMode mode) {
      myAutomaticMode =
         PropertyUtils.setModeAndUpdate (
            this, "automatic", myAutomaticMode, mode);
   }

   // SCoords

   public void setSCoords (double[] s) {
      if (!ArraySupport.equals (mySCoords, s)) {
         mySCoords = ArraySupport.copy (s);
         clearTextureData();
      }
      mySCoordsMode =
         PropertyUtils.propagateValue (this, "SCoords", s, mySCoordsMode);
   }

   public double[] getSCoords() {
      return mySCoords;
   }

   public PropertyMode getSCoordsMode() {
      return mySCoordsMode;
   }

   public void setSCoordsMode (PropertyMode mode) {
      mySCoordsMode =
         PropertyUtils.setModeAndUpdate (this, "SCoords", mySCoordsMode, mode);
   }

   // TCoords

   public void setTCoords (double[] t) {
      if (!ArraySupport.equals (myTCoords, t)) {
         myTCoords = ArraySupport.copy (t);
         clearTextureData();
      }
      myTCoordsMode =
         PropertyUtils.propagateValue (this, "TCoords", t, myTCoordsMode);
   }

   public double[] getTCoords() {
      return myTCoords;
   }

   public PropertyMode getTCoordsMode() {
      return myTCoordsMode;
   }

   public void setTCoordsMode (PropertyMode mode) {
      myTCoordsMode =
         PropertyUtils.setModeAndUpdate (this, "TCoords", myTCoordsMode, mode);
   }

   public void set (TextureProps props) {
      myEnabledP = props.myEnabledP;
      myEnabledMode = props.myEnabledMode;

      myMode = props.myMode;
      myModeMode = props.myModeMode;

      myFileName = new String (props.myFileName);
      myFileNameMode = props.myFileNameMode;

      mySphereMappingP = props.mySphereMappingP;
      mySphereMappingMode = props.mySphereMappingMode;

      myAutomaticP = props.myAutomaticP;
      myAutomaticMode = props.myAutomaticMode;

      mySCoords = ArraySupport.copy (props.mySCoords);
      mySCoordsMode = props.mySCoordsMode;

      myTCoords = ArraySupport.copy (props.myTCoords);
      myTCoordsMode = props.myTCoordsMode;
   }

   public boolean equals (TextureProps props) {
      return (myEnabledP == props.myEnabledP &&
              myEnabledMode == props.myEnabledMode && 
              myMode == props.myMode &&
              myModeMode == props.myModeMode &&
              myFileName.equals (props.myFileName) &&
              myFileNameMode == props.myFileNameMode &&
              mySphereMappingP == props.mySphereMappingP &&
              mySphereMappingMode == props.mySphereMappingMode &&
              myAutomaticP == props.myAutomaticP &&
              myAutomaticMode == props.myAutomaticMode &&
              ArraySupport.equals (mySCoords, props.mySCoords) &&
              mySCoordsMode == props.mySCoordsMode &&
              ArraySupport.equals (myTCoords, props.myTCoords) &&
              myTCoordsMode == props.myTCoordsMode);
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

   // /**
   // * Pass this set of texture properties into the specified mesh.
   // *
   // * @param mesh Mesh on which to set texture properties.
   // */
   // public void setMeshTexture (PolygonalMesh mesh)
   // {
   // PolygonalMeshRenderer renderer = mesh.getRenderer();

   // if (myEnabledP && myFileName != null)
   // { renderer.setEnableTextureMapping (true, myFileName);
   // switch (myMode)
   // { case DECAL:
   // { renderer.setTextureMode (PolygonalMeshRenderer.GL_DECAL);
   // break;
   // }
   // case REPLACE:
   // { renderer.setTextureMode (PolygonalMeshRenderer.GL_REPLACE);
   // break;
   // }
   // case MODULATE:
   // { renderer.setTextureMode (PolygonalMeshRenderer.GL_MODULATE);
   // break;
   // }
   // case BLEND:
   // { renderer.setTextureMode (PolygonalMeshRenderer.GL_BLEND);
   // break;
   // }
   // default:
   // { throw new InternalErrorException (
   // "unknown texture mode " + myMode);
   // }
   // }
   // if (myAutomaticP)
   // {
   // if (renderer.getSphereMapTextureMapping() != mySphereMappingP)
   // { renderer.setSphereMapTextureMapping(mySphereMappingP);
   // }
   // if (!renderer.getAutomaticTextureMapping())
   // { renderer.setAutomaticTextureMappingParmameters (
   // mySCoords, myTCoords);
   // }
   // }
   // else
   // { renderer.setAutomaticTextureMapping(false);
   // }
   // }
   // else
   // { renderer.setEnableTextureMapping (false);
   // }
   // }

   public String toString() {
      return ("enabled=" + myEnabledP + ", " + "mode=" + myMode + ", " +
              "fileName=" + myFileName + ", " +
              "sphereMapping=" + mySphereMappingP +
              ", " + "automatic=" + myAutomaticP + ", " +
              "sCoords=" + mySCoords[0] + " " + mySCoords[1] +
              " " + mySCoords[2] + " " + mySCoords[3] + ", " +
              "tCoords=" + myTCoords[0] + " " + myTCoords[1] +
              " " + myTCoords[2] + " " + myTCoords[3]);
   }

   public TextureProps clone() {
      TextureProps props = null;
      try {
         props = (TextureProps)super.clone();
      }
      catch (CloneNotSupportedException e) {
         throw new InternalErrorException ("Cannot clone super in TextureProps");
      }

      // myEnabledMode = new ModeObject(myEnabledMode);
      // myModeMode = new ModeObject(myModeMode);
      // myFileNameMode = new ModeObject(myFileNameMode);
      // mySphereMappingMode = new ModeObject(mySphereMappingMode);
      // myAutomaticMode = new ModeObject(myAutomaticMode);
      // mySCoordsMode = new ModeObject(mySCoordsMode);
      // myTCoordsMode = new ModeObject(myTCoordsMode);

      props.mySCoords = ArraySupport.copy (props.mySCoords);
      props.myTCoords = ArraySupport.copy (props.myTCoords);
      return props;
   }

   public static void main (String[] args) {
      TextureProps props = new TextureProps();
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
