/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

import java.awt.Color;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

import javax.media.opengl.GL2;

import maspack.properties.CompositeProperty;
import maspack.properties.HasProperties;
import maspack.properties.Property;
import maspack.properties.PropertyInfo;
import maspack.properties.PropertyList;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;
import maspack.util.ArraySupport;
import maspack.util.Clonable;
import maspack.util.IndentingPrintWriter;
import maspack.util.InternalErrorException;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import maspack.util.Scannable;

public class RenderProps implements CompositeProperty, Scannable, Clonable {

   public enum Shading {
      FLAT, GOURARD, PHONG, NONE
   };

   public enum PointStyle {
      SPHERE, POINT
   };

   public enum LineStyle {
      LINE, CYLINDER, SOLID_ARROW, ELLIPSOID
   };

   public enum Faces {
      BACK, FRONT, FRONT_AND_BACK, NONE
   };

   public enum Props {
      Visible,
      Alpha,
      zOrder,
      Shading,
      Shininess,
      FaceStyle,
      FaceColor,
      FaceColorDiffuse,
      FaceColorAmbient,
      FaceColorSpecular,
      FaceColorEmission,
      BackColor,
      BackColorDiffuse,
      BackColorAmbient,
      BackColorSpecular,
      BackColorEmission,
      DrawEdges,
      EdgeColor,
      EdgeWidth,
      TextureProps,
      LineStyle,
      LineColor,
      LineColorDiffuse,
      LineColorAmbient,
      LineColorSpecular,
      LineColorEmission,
      LineWidth,
      LineRadius,
      LineSlices,
      PointStyle,
      PointColor,
      PointColorDiffuse,
      PointColorAmbient,
      PointColorSpecular,
      PointColorEmission,
      PointSize,
      PointRadius,
      PointSlices
   };

   PropertyInfo myPropInfo;
   HasProperties myPropHost;

   public PropertyInfo getPropertyInfo() {
      return myPropInfo;
   }

   public void setPropertyInfo (PropertyInfo info) {
      // if (info == null)
      // { throw new IllegalArgumentException ("info is null");
      // }
      myPropInfo = info;
   }

   public HasProperties getPropertyHost() {
      return myPropHost;
   }

   public void setPropertyHost (HasProperties newParent) {
      myPropHost = newParent;
   }

   protected boolean myVisibleP;
   protected PropertyMode myVisibleMode;
   protected static boolean defaultVisibleP = true;

   protected int myLineWidth;
   protected PropertyMode myLineWidthMode;
   protected static int defaultLineWidth = 1;

   protected int myPointSize;
   protected PropertyMode myPointSizeMode;
   protected static int defaultPointSize = 1;

   protected float[] myFaceColor = new float[3];
   protected PropertyMode myFaceColorMode;
   protected static Color defaultFaceColor = new Color (0.5f, 0.5f, 0.5f);
   protected static final Color defaultColorAmbient = 
      new Color(
         Material.default_ambient[0],
         Material.default_ambient[1],
         Material.default_ambient[2],
         Material.default_ambient[3]);

   protected static final Color defaultColorDiffuse = 
      new Color(
         Material.default_diffuse[0],
         Material.default_diffuse[1],
         Material.default_diffuse[2],
         Material.default_diffuse[3]);
   protected static final Color defaultColorSpecular = 
      new Color(
         Material.default_specular[0],
         Material.default_specular[1],
         Material.default_specular[2],
         Material.default_specular[3]);
   protected static final Color defaultColorEmission = 
      new Color(
         Material.default_emission[0],
         Material.default_emission[1],
         Material.default_emission[2],
         Material.default_emission[3]);

   protected float myAlpha;
   protected PropertyMode myAlphaMode;
   protected static float defaultAlpha = 1f;

   protected int myZOrder;
   protected PropertyMode myZOrderMode;
   protected static int defaultZOrder = 0;

   protected Shading myShading;
   protected PropertyMode myShadingMode;
   protected static Shading defaultShading = Shading.FLAT;

   protected float myShininess;
   protected PropertyMode myShininessMode;
   protected static float defaultShininess = 32f;

   protected PointStyle myPointStyle;
   protected PropertyMode myPointStyleMode;
   protected static PointStyle defaultPointStyle = PointStyle.POINT;

   protected double myPointRadius;
   protected PropertyMode myPointRadiusMode;
   protected static double defaultPointRadius = 1;

   protected int myPointSlices;
   protected PropertyMode myPointSlicesMode;
   protected static int defaultPointSlices = 32;

   protected LineStyle myLineStyle;
   protected PropertyMode myLineStyleMode;
   protected static LineStyle defaultLineStyle = LineStyle.LINE;

   protected double myLineRadius;
   protected PropertyMode myLineRadiusMode;
   protected static double defaultLineRadius = 1;

   protected int myLineSlices;
   protected PropertyMode myLineSlicesMode;
   protected static int defaultLineSlices = 32;

   protected boolean myDrawEdgesP;
   protected PropertyMode myDrawEdgesMode;
   protected static boolean defaultDrawEdgesP = false;

   protected int myEdgeWidth;
   protected PropertyMode myEdgeWidthMode;
   protected static int defaultEdgeWidth = 1;

   protected float[] myEdgeColor = null;
   protected PropertyMode myEdgeColorMode;
   protected static Color defaultEdgeColor = null;

   protected Faces myFaceStyle;
   protected PropertyMode myFaceStyleMode;
   protected static Faces defaultFaceStyle = Faces.FRONT;

   protected float[] myBackColor = null;
   protected PropertyMode myBackColorMode;
   protected static Color defaultBackColor = null;

   protected float[] myLineColor = new float[3];
   protected PropertyMode myLineColorMode;
   protected static Color defaultLineColor = new Color (0.5f, 0.5f, 0.5f);

   protected float[] myPointColor = new float[3];
   protected PropertyMode myPointColorMode;
   protected static Color defaultPointColor = new Color (0.5f, 0.5f, 0.5f);

   protected TextureProps myTextureProps;
   protected static TextureProps defaultTextureProps = null;

   protected Material myFaceMaterial = null;
   protected Material myBackMaterial = null;
   protected Material myLineMaterial = null;
   protected Material myPointMaterial = null;

   public static PropertyList myProps = new PropertyList (RenderProps.class);

   protected static PropertyMode INHERITED = PropertyMode.Inherited;
   protected static PropertyMode INACTIVE = PropertyMode.Inactive;
   protected static PropertyMode EXPLICIT = PropertyMode.Explicit;

   protected boolean myTexturePropsInactive = true;

   static {
      myProps.addInheritable (
         "visible:Inherited isVisible *", "object is visible", defaultVisibleP);
      myProps.addInheritable (
         "zOrder:Inherited", "z display order", defaultZOrder);
      myProps.addInheritable (
         "alpha:Inherited", "alpha transparency", defaultAlpha, "[0,1]");
      myProps.addInheritable (
         "shading:Inherited", "shading style", defaultShading);
      myProps.addInheritable (
         "shininess:Inherited", "specular shininess", defaultShininess, "[0,Inf]");
     
      myProps.addInheritable (
         "faceStyle:Inherited", "draw front/back of faces", defaultFaceStyle);
      myProps.addInheritable (
         "faceColor:Inherited", "color for faces", defaultFaceColor);
      myProps.addInheritable (
         "backColor:Inherited", "back face color", defaultBackColor);
      myProps.addInheritable (
         "drawEdges:Inherited", "draw mesh edges", defaultDrawEdgesP);
      myProps.add (
         "textureProps", "texture mapping properties", defaultTextureProps);
      
      myProps.addInheritable (
         "edgeColor:Inherited", "edge color (mainly for meshes)", 
         defaultEdgeColor);     
      myProps.addInheritable (
         "edgeWidth:Inherited", "edge width (pixels)", defaultEdgeWidth);     
      
      //myProps.createGroup("Faces...", 
      //   "faceStyle", "faceColor", "backColor", "drawEdges", "textureProps");
      
      myProps.addInheritable (
         "lineStyle:Inherited",
         "render as pixel-based lines or solid cylinders", defaultLineStyle);
      myProps.addInheritable (
         "lineColor:Inherited", "color for drawing lines and edges",
         defaultLineColor);
      myProps.addInheritable (
         "lineWidth:Inherited", "line width (pixels)", defaultLineWidth, "[0,Inf]");
      myProps.addInheritable (
         "lineRadius:Inherited", "radius of lines rendered as cylinders",
         defaultLineRadius, "[0,Inf]");
      myProps.addInheritable (
         "lineSlices:Inherited",
         "number of slices for lines rendered as cylinders",
         defaultLineSlices, "[3,Inf]");
      //myProps.createGroup("Lines...", 
      //  "lineStyle", "lineColor", "lineWidth", "lineRadius", "lineSlices");

      myProps.addInheritable (
         "pointStyle:Inherited",
         "render as pixel-based points or solid spheres", defaultPointStyle);
      myProps.addInheritable (
         "pointColor:Inherited", "color for drawing points", defaultPointColor);
      myProps.addInheritable (
         "pointSize:Inherited", "point size (pixels)", defaultPointSize);
      myProps.addInheritable (
         "pointRadius:Inherited", "radius of points rendered as spheres",
         defaultPointRadius);
      myProps.addInheritable (
         "pointSlices:Inherited",
         "number of slices for points rendered as spheres",
         defaultPointSlices);
      //myProps.createGroup("Points...", 
      //  "pointStyle", "pointColor", "pointSize", "pointRadius", "pointSlices");
   }
   
   private int taperedEllipsoidDisplayList = 0;
   private int sphereDisplayList = 0;
   private int meshDisplayList = 0;
   private int edgeDisplayList = 0;

   public RenderProps() {
      setDefaultModes();
      setDefaultValues();
   }

   public RenderProps (RenderProps props) {
      this();
      set (props);
   }

   // property visible

   public boolean isVisible() {
      return myVisibleP;
   }

   public void setVisible (boolean visible) {
      myVisibleP = visible;
      myVisibleMode =
         PropertyUtils.propagateValue (this, "visible", visible, myVisibleMode);
   }

   public PropertyMode getVisibleMode() {
      return myVisibleMode;
   }

   public void setVisibleMode (PropertyMode mode) {
      myVisibleMode =
         PropertyUtils.setModeAndUpdate (this, "visible", myVisibleMode, mode);
   }

   // property lineWidth

   public int getLineWidth() {
      return myLineWidth;
   }

   public void setLineWidth (int width) {
      if (myLineWidth != width) {
         myLineWidth = width;
         clearMeshDisplayList();
      }
      myLineWidthMode =
         PropertyUtils.propagateValue (
            this, "lineWidth", width, myLineWidthMode);
   }

   public PropertyMode getLineWidthMode() {
      return myLineWidthMode;
   }

   public void setLineWidthMode (PropertyMode mode) {
      myLineWidthMode =
         PropertyUtils.setModeAndUpdate (
            this, "lineWidth", myLineWidthMode, mode);
   }

   // property pointSize

   public int getPointSize() {
      return myPointSize;
   }

   public void setPointSize (int size) {
      if (myPointSize != size) {
         myPointSize = size;
         clearMeshDisplayList();
      }
      myPointSizeMode =
         PropertyUtils.propagateValue (
            this, "pointSize", size, myPointSizeMode);
   }

   public PropertyMode getPointSizeMode() {
      return myPointSizeMode;
   }

   public void setPointSizeMode (PropertyMode mode) {
      myPointSizeMode =
         PropertyUtils.setModeAndUpdate (
            this, "pointSize", myPointSizeMode, mode);
   }

   protected boolean colorsEqual (float[] vals, Color color) {
      if (color == null) {
         return vals == null;
      }
      else {
         if (vals == null) {
            return false;
         }
         float r = color.getRed() / 255f;
         float g = color.getGreen() / 255f;
         float b = color.getBlue() / 255f;

         return (r == vals[0] && g == vals[1] && b == vals[2]);
      }
   }

   protected boolean colorsEqual (float[] vals, float[] color) {
      if (color == null) {
         return vals == null;
      }
      else {
         if (vals == null) {
            return false;
         }
         return (color[0]==vals[0] && color[1]==vals[1] && color[2]==vals[2]);
      }
   }

   /**
    * Since materials are allocated on demand, assume they are unequal
    * only if both are non-null and also unequal internally.
    */
   protected boolean materialsEqual (Material mat0, Material mat1) {
      return (mat0 == null || mat1 == null || mat0.equals (mat1));
   }

   protected boolean doSetColor (float[] result, Color color) {
      boolean changed = false;

      float r = color.getRed() / 255f;
      float g = color.getGreen() / 255f;
      float b = color.getBlue() / 255f;


      if (result[0] != r) {
         result[0] = r;
         changed = true;
      }
      if (result[1] != g) {
         result[1] = g;
         changed = true;
      }
      if (result[2] != b) {
         result[2] = b;
         changed = true;
      }
      return changed;
   }

   protected boolean doSetColor (float[] result, float[] color) {
      boolean changed = false;

      if (result[0] != color[0]) {
         result[0] = color[0];
         changed = true;
      }
      if (result[1] != color[1]) {
         result[1] = color[1];
         changed = true;
      }
      if (result[2] != color[2]) {
         result[2] = color[2];
         changed = true;
      }
      return changed;
   }

   protected void updateMaterial(Material mat, Color c, float alpha, float shine) {
      if (mat != null) {
         mat.setDiffuse(c);
         mat.setAlpha(alpha);
         mat.setShininess(shine);
      }
   }

   private void updateMaterial(Material mat, float[] c, float alpha, float shine) {
      if (mat != null) {
         if (c != null) {
            mat.setDiffuse(c);
         }
         mat.setAlpha(alpha);
         mat.setShininess(shine);
      }
   }

   // property faceColor

   public Color getFaceColor() {
      return new Color (myFaceColor[0], myFaceColor[1], myFaceColor[2]);
   }

   public float[] getFaceColorArray() {
      return myFaceColor;
   }

   public void setFaceColor (Color color) {
      if (doSetColor (myFaceColor, color)) {
         updateMaterial(myFaceMaterial, color, myAlpha, myShininess);
      }
      if (myBackColor == null && myBackMaterial != null) {
         myBackMaterial.setDiffuse(myFaceColor);
      }
      myFaceColorMode =
         PropertyUtils.propagateValue (
            this, "faceColor", color, myFaceColorMode);
   }

   public void getFaceColor (float[] color) {
      color[0] = myFaceColor[0];
      color[1] = myFaceColor[1];
      color[2] = myFaceColor[2];
   }

   public PropertyMode getFaceColorMode() {
      return myFaceColorMode;
   }

   public void setFaceColorMode (PropertyMode mode) {

      if (mode != myFaceColorMode) {
         myFaceColorMode = PropertyUtils.setModeAndUpdate (
            this, "faceColor", myFaceColorMode, mode);
      }

   }

   // property faceColorDiffuse

   public Color getFaceColorDiffuse() {
      return getFaceColor();
   }

   public float[] getFaceColorDiffuseArray() {
      if (myFaceMaterial != null) {
         return myFaceMaterial.getDiffuse();
      } 

      float[] out = Arrays.copyOf(myFaceColor, 4);
      out[3] = myAlpha;
      return out;
   }

   public void setFaceColorDiffuse (Color color) {
      setFaceColor(color);
   }

   public void getFaceColorDiffuse (float[] color) {
      getFaceColor(color);
      if (color.length > 3) {
         color[3] = myAlpha;
      }
   }

   public PropertyMode getFaceColorDiffuseMode() {
      return getFaceColorMode();
   }

   public void setFaceColorDiffuseMode (PropertyMode mode) {
      setFaceColorMode(mode);
   }


   private static Color getColorAmbient(Material mat, boolean createIfNotExist) {
      float color[];
      if (mat != null) {
         color  = mat.getAmbient();
         return new Color (color[0], color[1], color[2]);
      } else if (createIfNotExist) {
         color = Material.default_ambient;
         return new Color (color[0], color[1], color[2]);
      }
      return null;
   }

   private static  void getColorAmbient (Material mat, float[] color) {
      float[] fc;
      if (mat != null) {
         fc = mat.getAmbient();
      } else {
         fc = Material.default_ambient;
      }
      color[0] = fc[0];
      color[1] = fc[1];
      color[2] = fc[2];
      if (color.length > 3) {
         color[3] = fc[3];
      }
   }

   private static Color getColorSpecular(Material mat, boolean createIfNotExist) {
      float color[];
      if (mat != null) {
         color  = mat.getSpecular();
         return new Color (color[0], color[1], color[2]);
      } else if (createIfNotExist){
         color = Material.default_specular;
         return new Color (color[0], color[1], color[2]);
      }
      return null;
      
   }

   private static void getColorSpecular (Material mat, float[] color) {
      float[] fc;
      if (mat != null) {
         fc = mat.getSpecular();
      } else {
         fc = Material.default_specular;
      }
      color[0] = fc[0];
      color[1] = fc[1];
      color[2] = fc[2];
      if (color.length > 3) {
         color[3] = fc[3];
      }
   }

   private static Color getColorEmission(Material mat, boolean createIfNotExist) {
      float color[];
      if (mat != null) {
         color  = mat.getEmission();
         return new Color (color[0], color[1], color[2]);
      } else if (createIfNotExist){
         color = Material.default_emission;
         return new Color (color[0], color[1], color[2]);
      }
      return null;
   }

   private static void getColorEmission (Material mat, float[] color) {
      float[] fc;
      if (mat != null) {
         fc = mat.getEmission();
      } else {
         fc = Material.default_emission;
      }
      color[0] = fc[0];
      color[1] = fc[1];
      color[2] = fc[2];
      if (color.length > 3) {
         color[3] = fc[3];
      }
   }

   private Material createAmbientMaterial(Color ambient, float[] diffuse) {
      Material mat = new Material();
      mat.setAmbient(ambient);
      mat.setDiffuse(diffuse);
      mat.setAlpha(myAlpha);
      mat.setShininess(myShininess);
      return mat;
   }

   private Material createSpecularMaterial(Color spec, float[] diffuse) {
      Material mat = new Material();
      mat.setSpecular(spec);
      mat.setDiffuse(diffuse);
      mat.setAlpha(myAlpha);
      mat.setShininess(myShininess);
      return mat;
   }

   private Material createEmissionMaterial(Color emission, float[] diffuse) {
      Material mat = new Material();
      mat.setEmission(emission);
      mat.setDiffuse(diffuse);
      mat.setAlpha(myAlpha);
      mat.setShininess(myShininess);
      return mat;
   }

   // property faceColorAmbient
   public Color getFaceColorAmbient() {
      return getColorAmbient(myFaceMaterial, true);
   }

   public float[] getFaceColorAmbientArray() {
      if (myFaceMaterial != null) {
         return myFaceMaterial.getAmbient();
      } else {
         return Arrays.copyOf(Material.default_ambient, 4);
      }
   }

   public void setFaceColorAmbient (Color color) {
      if (myFaceMaterial == null) {
         myFaceMaterial = createAmbientMaterial(color, myFaceColor);
      } else {
         myFaceMaterial.setAmbient(color);
      }
      myFaceColorMode =
         PropertyUtils.propagateValue (
            this, "faceColorAmbient", color, myFaceColorMode);
   }

   public void getFaceColorAmbient (float[] color) {
      getColorAmbient(myFaceMaterial, color);
   }

   public PropertyMode getFaceColorAmbientMode() {
      return getFaceColorMode();
   }

   public void setFaceColorAmbientMode (PropertyMode mode) {
      if (mode != myFaceColorMode) {
         myFaceColorMode = PropertyUtils.setModeAndUpdate (
            this, "faceColorAmbient", myFaceColorMode, mode);
      }
   }

   // property faceColorSpecular
   public Color getFaceColorSpecular() {
      return getColorSpecular(myFaceMaterial, true);
   }

   public float[] getFaceColorSpecularArray() {
      if (myFaceMaterial != null) {
         return myFaceMaterial.getSpecular();
      } else {
         return Arrays.copyOf(Material.default_specular, 4);
      }
   }

   public void setFaceColorSpecular (Color color) {
      if (myFaceMaterial == null) {
         myFaceMaterial = createSpecularMaterial(color, myFaceColor);
      } else {
         myFaceMaterial.setSpecular(color);
      }
      myFaceColorMode =
         PropertyUtils.propagateValue (
            this, "faceColorSpecular", color, myFaceColorMode);
   }

   public void getFaceColorSpecular (float[] color) {
      getColorSpecular(myFaceMaterial, color);
   }

   public PropertyMode getFaceColorSpecularMode() {
      return getFaceColorMode();
   }

   public void setFaceColorSpecularMode (PropertyMode mode) {
      if (mode != myFaceColorMode) {
         myFaceColorMode = PropertyUtils.setModeAndUpdate (
            this, "faceColorSpecular", myFaceColorMode, mode);
      }
   }

   // property faceColorEmission
   public Color getFaceColorEmission() {
      return getColorEmission(myFaceMaterial, true);
   }

   public float[] getFaceColorEmissionArray() {
      if (myFaceMaterial != null) {
         return myFaceMaterial.getEmission();
      } else {
         return Arrays.copyOf(Material.default_emission, 4);
      }
   }

   public void setFaceColorEmission (Color color) {
      if (myFaceMaterial == null) {
         myFaceMaterial = createEmissionMaterial(color, myFaceColor);
      } else {
         myFaceMaterial.setEmission(color);
      }
      myFaceColorMode =
         PropertyUtils.propagateValue (
            this, "faceColorEmission", color, myFaceColorMode);
   }

   public void getFaceColorEmission (float[] color) {
      getColorEmission(myFaceMaterial, color);
   }

   public PropertyMode getFaceColorEmissionMode() {
      return getFaceColorMode();
   }

   public void setFaceColorEmissionMode (PropertyMode mode) {
      if (mode != myFaceColorMode) {
         myFaceColorMode = PropertyUtils.setModeAndUpdate (
            this, "faceColorEmission", myFaceColorMode, mode);
      }
   }

   // property alpha

   protected boolean doSetAlpha (float a) {
      if (myAlpha != a) {
         myAlpha = a;
         updateMaterial(myFaceMaterial, myFaceColor, a, myShininess);
         updateMaterial(myBackMaterial, myBackColor, a, myShininess);
         updateMaterial(myLineMaterial, myLineColor, a, myShininess);
         updateMaterial(myPointMaterial, myPointColor, a, myShininess);
         return true;
      }
      else {
         return false;
      }
   }

   public double getAlpha() {
      return myAlpha;
   }

   public void setAlpha (double a) {
      doSetAlpha ((float)a);
      myAlphaMode =
         PropertyUtils.propagateValue (this, "alpha", myAlpha, myAlphaMode);
   }

   public PropertyMode getAlphaMode() {
      return myAlphaMode;
   }

   public void setAlphaMode (PropertyMode mode) {
      myAlphaMode =
         PropertyUtils.setModeAndUpdate (this, "alpha", myAlphaMode, mode);
   }

   public int getZOrder() {
      return myZOrder;
   }

   public void setZOrder (int order) {
      myZOrder = order;
      myZOrderMode =
         PropertyUtils.propagateValue (this, "zOrder", myZOrder, myZOrderMode);
   }

   public PropertyMode getZOrderMode() {
      return myZOrderMode;
   }

   public void setZOrderMode (PropertyMode mode) {
      myZOrderMode =
         PropertyUtils.setModeAndUpdate (this, "zOrder", myZOrderMode, mode);
   }

   public boolean isTransparent() {
      return myAlpha < 1;
   }

   // property shading

   public Shading getShading() {
      return myShading;
   }

   public void setShading (Shading shading) {
      if (myShading != shading) {
         myShading = shading;
         clearMeshDisplayList();
      }
      myShadingMode =
         PropertyUtils.propagateValue (this, "shading", shading, myShadingMode);
   }

   public PropertyMode getShadingMode() {
      return myShadingMode;
   }

   public void setShadingMode (PropertyMode mode) {
      myShadingMode =
         PropertyUtils.setModeAndUpdate (this, "shading", myShadingMode, mode);
   }

   protected boolean doSetShininess (float s) {
      if (myShininess != s) {
         myShininess = s;
         updateMaterial(myFaceMaterial, myFaceColor, myAlpha, myShininess);
         updateMaterial(myBackMaterial, myBackColor, myAlpha, myShininess);
         updateMaterial(myLineMaterial, myLineColor, myAlpha, myShininess);
         updateMaterial(myPointMaterial, myPointColor, myAlpha, myShininess);
         return true;
      }
      else {
         return false;
      }
   }

   // property shininess

   public float getShininess() {
      return myShininess;
   }

   public void setShininess (float s) {
      doSetShininess ((float)s);
      myShininessMode =
         PropertyUtils.propagateValue (this, "shininess", s, myShininessMode);
   }

   public PropertyMode getShininessMode() {
      return myShininessMode;
   }

   public void setShininessMode (PropertyMode mode) {
      myShininessMode =
         PropertyUtils.setModeAndUpdate (
            this, "shininess", myShininessMode, mode);
   }

   public Material getFaceMaterial() {
      if (myFaceMaterial == null) {
         myFaceMaterial =
            Material.createDiffuse (myFaceColor, myAlpha, myShininess);
      }
      return myFaceMaterial;
   }

   // property pointStyle

   public PointStyle getPointStyle() {
      return myPointStyle;
   }

   public void setPointStyle (PointStyle style) {
      myPointStyle = style;
      myPointStyleMode =
         PropertyUtils.propagateValue (
            this, "pointStyle", style, myPointStyleMode);
   }

   public PropertyMode getPointStyleMode() {
      return myPointStyleMode;
   }

   public void setPointStyleMode (PropertyMode mode) {
      myPointStyleMode =
         PropertyUtils.setModeAndUpdate (
            this, "pointStyle", myPointStyleMode, mode);
   }

   // property pointRadius

   public double getPointRadius() {
      return myPointRadius;
   }

   public void setPointRadius (double r) {
      myPointRadius = r;
      myPointRadiusMode =
         PropertyUtils.propagateValue (
            this, "pointRadius", r, myPointRadiusMode);
   }

   public PropertyMode getPointRadiusMode() {
      return myPointRadiusMode;
   }

   public void setPointRadiusMode (PropertyMode mode) {
      myPointRadiusMode =
         PropertyUtils.setModeAndUpdate (
            this, "pointRadius", myPointRadiusMode, mode);
   }

   // property pointSlices

   public int getPointSlices() {
      return myPointSlices;
   }

   public void setPointSlices (int num) {
      if (myPointSlices != num) {
         clearSphereDisplayList();
         myPointSlices = num;
      }
      myPointSlicesMode =
         PropertyUtils.propagateValue (
            this, "pointSlices", num, myPointSlicesMode);
   }

   public PropertyMode getPointSlicesMode() {
      return myPointSlicesMode;
   }

   public void setPointSlicesMode (PropertyMode mode) {
      myPointSlicesMode =
         PropertyUtils.setModeAndUpdate (
            this, "pointSlices", myPointSlicesMode, mode);
   }

   // property lineStyle

   public LineStyle getLineStyle() {
      return myLineStyle;
   }

   public void setLineStyle (LineStyle style) {
      myLineStyle = style;
      myLineStyleMode =
         PropertyUtils.propagateValue (
            this, "lineStyle", style, myLineStyleMode);
   }

   public PropertyMode getLineStyleMode() {
      return myLineStyleMode;
   }

   public void setLineStyleMode (PropertyMode mode) {
      myLineStyleMode =
         PropertyUtils.setModeAndUpdate (
            this, "lineStyle", myLineStyleMode, mode);
   }

   // property lineRadius

   public double getLineRadius() {
      return myLineRadius;
   }

   public void setLineRadius (double r) {
      myLineRadius = r;
      myLineRadiusMode =
         PropertyUtils.propagateValue (
            this, "lineRadius", r, myLineRadiusMode);
   }

   public PropertyMode getLineRadiusMode() {
      return myLineRadiusMode;
   }

   public void setLineRadiusMode (PropertyMode mode) {
      myLineRadiusMode =
         PropertyUtils.setModeAndUpdate (
            this, "lineRadius", myLineRadiusMode, mode);
   }

   // property lineSlices

   public int getLineSlices() {
      return myLineSlices;
   }

   public void setLineSlices (int num) {
      if (myLineSlices != num) {
         clearTaperedEllipsoidDisplayList();
         myLineSlices = num;
      }
      myLineSlicesMode =
         PropertyUtils.propagateValue (
            this, "lineSlices", num, myLineSlicesMode);
   }

   public PropertyMode getLineSlicesMode() {
      return myLineSlicesMode;
   }

   public void setLineSlicesMode (PropertyMode mode) {
      myLineSlicesMode =
         PropertyUtils.setModeAndUpdate (
            this, "lineSlices", myLineSlicesMode, mode);
   }

   // property drawEdges

   public boolean getDrawEdges() {
      return myDrawEdgesP;
   }

   public void setDrawEdges (boolean enable) {
      if (myDrawEdgesP != enable) {
         myDrawEdgesP = enable;
         clearMeshDisplayList();
      }
      myDrawEdgesMode =
         PropertyUtils.propagateValue (
            this, "drawEdges", enable, myDrawEdgesMode);
   }

   public PropertyMode getDrawEdgesMode() {
      return myDrawEdgesMode;
   }

   public void setDrawEdgesMode (PropertyMode mode) {
      myDrawEdgesMode =
         PropertyUtils.setModeAndUpdate (
            this, "drawEdges", myDrawEdgesMode, mode);
   }

   // property edgeWidth

   public int getEdgeWidth() {
      return myEdgeWidth;
   }

   public void setEdgeWidth (int width) {
      myEdgeWidth = width;
      myEdgeWidthMode =
         PropertyUtils.propagateValue (
            this, "edgeWidth", width, myEdgeWidthMode);
   }

   public PropertyMode getEdgeWidthMode() {
      return myEdgeWidthMode;
   }

   public void setEdgeWidthMode (PropertyMode mode) {
      myEdgeWidthMode =
         PropertyUtils.setModeAndUpdate (
            this, "edgeWidth", myEdgeWidthMode, mode);
   }

   // property edgeColor

   public Color getEdgeColor() {
      if (myEdgeColor != null) {
         return new Color (myEdgeColor[0], myEdgeColor[1], myEdgeColor[2]);
      }
      else {
         return null;
      }
   }

   public float[] getEdgeOrLineColorArray() {
      return (myEdgeColor != null ? myEdgeColor : myLineColor);
   }         

   public float[] getEdgeColorArray() {
      return myEdgeColor;
   }

   private void doSetEdgeColor (Color color) {
      if (color == null) {
         myEdgeColor = null;
      }
      else {
         myEdgeColor = color.getRGBColorComponents (null);
      }
   }

   private void doSetEdgeColor (float[] rgb) {
      if (rgb == null) {
         myEdgeColor = null;
      }
      else {
         if (myEdgeColor == null) {
            myEdgeColor = new float[3];
         }
         doSetColor (myEdgeColor, rgb);
      }
   }

   public void setEdgeColor (Color color) {
      doSetEdgeColor (color);
      myEdgeColorMode =
         PropertyUtils.propagateValue (
            this, "edgeColor", color, myEdgeColorMode);
   }

   public void getEdgeColor (float[] color) {
      if (myEdgeColor == null) {
         color[0] = myEdgeColor[0];
         color[1] = myEdgeColor[1];
         color[2] = myEdgeColor[2];
      }
      else {
         color[0] = myLineColor[0];
         color[1] = myLineColor[1];
         color[2] = myLineColor[2];
      }
   }

   public PropertyMode getEdgeColorMode() {
      return myEdgeColorMode;
   }

   public void setEdgeColorMode (PropertyMode mode) {
      if (mode != myEdgeColorMode) {
         myEdgeColorMode =
            PropertyUtils.setModeAndUpdate (
               this, "edgeColor", myEdgeColorMode, mode);
      }
   }

   // property faceStyle

   public Faces getFaceStyle() {
      return myFaceStyle;
   }

   public void setFaceStyle (Faces mode) {
      if (myFaceStyle != mode) {
         myFaceStyle = mode;
         clearMeshDisplayList();
      }
      myFaceStyleMode =
         PropertyUtils.propagateValue (this, "faceStyle", mode, myFaceStyleMode);
   }

   public PropertyMode getFaceStyleMode() {
      return myFaceStyleMode;
   }

   public void setFaceStyleMode (PropertyMode mode) {
      myFaceStyleMode =
         PropertyUtils.setModeAndUpdate (
            this, "faceStyle", myFaceStyleMode, mode);
   }

   protected boolean doSetBackColor (Color color) {
      boolean changed = false;
      if (color == null) {
         if (myBackColor != null) {
            myBackColor = null;
            changed = true;
         }
      }
      else {
         if (myBackColor == null) {
            myBackColor = new float[3];
            doSetColor (myBackColor, color);
            changed = true;
         }
         else {
            changed = doSetColor (myBackColor, color);
         }
      }
      return changed;
   }

   protected boolean doSetBackColor (float[] color) {
      boolean changed = false;
      if (color == null) {
         if (myBackColor != null) {
            myBackColor = null;
            changed = true;
         }
      }
      else {
         if (myBackColor == null) {
            myBackColor = new float[3];
            doSetColor (myBackColor, color);
            changed = true;
         }
         else {
            changed = doSetColor (myBackColor, color);
         }
      }
      return changed;
   }

   // property backColor

   public Color getBackColor() {
      if (myBackColor != null) {
         return new Color (myBackColor[0], myBackColor[1], myBackColor[2]);
      }
      else {
         return null;
      }
   }

   public float[] getBackColorArray() {
      return myBackColor;
   }

   public void setBackColor (Color color) {
      if (doSetBackColor (color)) {
         if (color != null) {
            updateMaterial(myBackMaterial, color, myAlpha, myShininess);
         }
      }
      myBackColorMode =
         PropertyUtils.propagateValue (
            this, "backColor", color, myBackColorMode);
   }

   public void getBackColor (float[] color) {
      if (myBackColor == null) {
         color[0] = myFaceColor[0];
         color[1] = myFaceColor[1];
         color[2] = myFaceColor[2];
      }
      else {
         color[0] = myBackColor[0];
         color[1] = myBackColor[1];
         color[2] = myBackColor[2];
      }
   }

   public PropertyMode getBackColorMode() {
      return myBackColorMode;
   }

   public void setBackColorMode (PropertyMode mode) {

      if (mode != myBackColorMode) {
         myBackColorMode = PropertyUtils.setModeAndUpdate (
            this, "backColor", myBackColorMode, mode);
      }

   }

   // property backColorDiffuse

   public Color getBackColorDiffuse() {
      return getBackColor();
   }

   public float[] getBackColorDiffuseArray() {
      if (myBackColor == null) {
         return null;
      } else if (myBackMaterial != null) {
         return myBackMaterial.getDiffuse();
      } else if (myBackColor != null) {
         float[] out = Arrays.copyOf(myBackColor, 4);
         out[3] = myAlpha;
         return out;
      }
      return null;
   }

   public void setBackColorDiffuse (Color color) {
      setBackColor(color);
   }

   public void getBackColorDiffuse (float[] color) {
      getBackColor(color);
      if (color.length > 3) {
         color[3] = myAlpha;
      }
   }

   public PropertyMode getBackColorDiffuseMode() {
      return getBackColorMode();
   }

   public void setBackColorDiffuseMode (PropertyMode mode) {
      setBackColorMode(mode);
   }

   // property backColorAmbient
   public Color getBackColorAmbient() {
      if (myBackColor != null) {
         return getColorAmbient(myBackMaterial, false);
      }
      return null;
   }

   public float[] getBackColorAmbientArray() {
      if (myBackColor == null) {
         return null;
      } else if (myBackMaterial != null) {
         return myBackMaterial.getAmbient();
      } else {
         return null;
      }
   }

   public void setBackColorAmbient (Color color) {
      if (color == null) {
         // set default
         if (myBackMaterial != null) {
            myBackMaterial.setAmbient(defaultColorAmbient);
         }
      } else if (myBackMaterial == null) {
         if (myBackColor != null) {
            myBackMaterial = createAmbientMaterial(color, myBackColor);
         } else {
            myBackMaterial = createAmbientMaterial(color, myFaceColor);
         }
      } else {
         myBackMaterial.setAmbient(color);
      }
      myBackColorMode =
         PropertyUtils.propagateValue (
            this, "backColorAmbient", color, myBackColorMode);
   }

   public void getBackColorAmbient (float[] color) {
      if (myBackColor != null) {
         getColorAmbient(myBackMaterial, color);
      } else {
         getColorAmbient(myFaceMaterial, color);
      }
   }

   public PropertyMode getBackColorAmbientMode() {
      return getBackColorMode();
   }

   public void setBackColorAmbientMode (PropertyMode mode) {
      if (mode != myBackColorMode) {
         myBackColorMode = PropertyUtils.setModeAndUpdate (
            this, "backColorAmbient", myBackColorMode, mode);
      }
   }

   // property backColorSpecular
   public Color getBackColorSpecular() {
      if (myBackColor != null) {
         return getColorSpecular(myBackMaterial, true);
      }
      return null;
   }

   public float[] getBackColorSpecularArray() {
      if (myBackMaterial != null) {
         return myBackMaterial.getSpecular();
      } else if (myBackColor != null){
         return Arrays.copyOf(Material.default_specular, 4);
      }
      return null;
   }

   public void setBackColorSpecular (Color color) {
      if (color == null) {
         // set default
         if (myBackMaterial != null) {
            myBackMaterial.setSpecular(defaultColorAmbient);
         }
      } else if (myBackMaterial == null) {
         if (myBackColor != null) {
            myBackMaterial = createSpecularMaterial(color, myBackColor);
         } else {
            myBackMaterial = createSpecularMaterial(color, myFaceColor);
         }
      } else {
         myBackMaterial.setSpecular(color);
      }
      myBackColorMode =
         PropertyUtils.propagateValue (
            this, "backColorSpecular", color, myBackColorMode);
   }

   public void getBackColorSpecular (float[] color) {
      if (myBackColor != null) {
         getColorSpecular(myBackMaterial, color);
      } else {
         getColorSpecular(myFaceMaterial, color);
      }
   }

   public PropertyMode getBackColorSpecularMode() {
      return getBackColorMode();
   }

   public void setBackColorSpecularMode (PropertyMode mode) {
      if (mode != myBackColorMode) {
         myBackColorMode = PropertyUtils.setModeAndUpdate (
            this, "backColorSpecular", myBackColorMode, mode);
      }
   }

   // property backColorEmission
   public Color getBackColorEmission() {
      if (myBackColor != null) {
         return getColorEmission(myBackMaterial, true);
      }
      return null;
   }

   public float[] getBackColorEmissionArray() {
      if (myBackMaterial != null) {
         return myBackMaterial.getEmission();
      } else {
         return null;
      }
   }

   public void setBackColorEmission (Color color) {
      if (color == null) {
         // set default
         if (myBackMaterial != null) {
            myBackMaterial.setEmission(defaultColorAmbient);
         }
      } else if (myBackMaterial == null) {
         if (myBackColor != null) {
            myBackMaterial = createEmissionMaterial(color, myBackColor);
         } else {
            myBackMaterial = createEmissionMaterial(color, myFaceColor);
         }
      } else {
         myBackMaterial.setEmission(color);
      }
      myBackColorMode =
         PropertyUtils.propagateValue (
            this, "backColorEmission", color, myBackColorMode);
   }

   public void getBackColorEmission (float[] color) {
      if (myBackColor != null) {
         getColorEmission(myBackMaterial, color);
      } else {
         getColorEmission(myFaceMaterial, color);
      }
   }

   public PropertyMode getBackColorEmissionMode() {
      return getBackColorMode();
   }

   public void setBackColorEmissionMode (PropertyMode mode) {
      if (mode != myBackColorMode) {
         myBackColorMode = PropertyUtils.setModeAndUpdate (
            this, "backColorEmission", myBackColorMode, mode);
      }
   }

   // property lineColor

   public Color getLineColor() {
      return new Color (myLineColor[0], myLineColor[1], myLineColor[2]);
   }

   public float[] getLineColorArray() {
      return myLineColor;
   }

   public void setLineColor (Color color) {
      if (doSetColor (myLineColor, color)) {
         updateMaterial(myLineMaterial, color, myAlpha, myShininess);
      }
      myLineColorMode =
         PropertyUtils.propagateValue (
            this, "lineColor", color, myLineColorMode);
   }

   public void getLineColor (float[] color) {
      color[0] = myLineColor[0];
      color[1] = myLineColor[1];
      color[2] = myLineColor[2];
   }

   public PropertyMode getLineColorMode() {
      return myLineColorMode;
   }

   public void setLineColorMode (PropertyMode mode) {
      if (mode != myLineColorMode) {
         myLineColorMode = PropertyUtils.setModeAndUpdate (
            this, "lineColor", myLineColorMode, mode);
      }
   }

   // property lineColorDiffuse

   public Color getLineColorDiffuse() {
      return getLineColor();
   }

   public float[] getLineColorDiffuseArray() {
      if (myLineMaterial != null) {
         return myLineMaterial.getDiffuse();
      } 

      float[] out = Arrays.copyOf(myLineColor, 4);
      out[3] = myAlpha;
      return out;
   }

   public void setLineColorDiffuse (Color color) {
      setLineColor(color);
   }

   public void getLineColorDiffuse (float[] color) {
      getLineColor(color);
      if (color.length > 3) {
         color[3] = myAlpha;
      }
   }

   public PropertyMode getLineColorDiffuseMode() {
      return getLineColorMode();
   }

   public void setLineColorDiffuseMode (PropertyMode mode) {
      setLineColorMode(mode);
   }

   // property lineColorAmbient
   public Color getLineColorAmbient() {
      return getColorAmbient(myLineMaterial, true);
   }

   public float[] getLineColorAmbientArray() {
      if (myLineMaterial != null) {
         return myLineMaterial.getAmbient();
      } else {
         return Arrays.copyOf(Material.default_ambient, 4);
      }
   }

   public void setLineColorAmbient (Color color) {
      if (myLineMaterial == null) {
         myLineMaterial = createAmbientMaterial(color, myLineColor);
      } else {
         myLineMaterial.setAmbient(color);
      }
      myLineColorMode =
         PropertyUtils.propagateValue (
            this, "lineColorAmbient", color, myLineColorMode);
   }

   public void getLineColorAmbient (float[] color) {
      getColorAmbient(myLineMaterial, color);
   }

   public PropertyMode getLineColorAmbientMode() {
      return getLineColorMode();
   }

   public void setLineColorAmbientMode (PropertyMode mode) {
      if (mode != myLineColorMode) {
         myLineColorMode = PropertyUtils.setModeAndUpdate (
            this, "lineColorAmbient", myLineColorMode, mode);
      }
   }

   // property lineColorSpecular
   public Color getLineColorSpecular() {
      return getColorSpecular(myLineMaterial, true);
   }

   public float[] getLineColorSpecularArray() {
      if (myLineMaterial != null) {
         return myLineMaterial.getSpecular();
      } else {
         return Arrays.copyOf(Material.default_specular, 4);
      }
   }

   public void setLineColorSpecular (Color color) {
      if (myLineMaterial == null) {
         myLineMaterial = createSpecularMaterial(color, myLineColor);
      } else {
         myLineMaterial.setSpecular(color);
      }
      myLineColorMode =
         PropertyUtils.propagateValue (
            this, "lineColorSpecular", color, myLineColorMode);
   }

   public void getLineColorSpecular (float[] color) {
      getColorSpecular(myLineMaterial, true);
   }

   public PropertyMode getLineColorSpecularMode() {
      return getLineColorMode();
   }

   public void setLineColorSpecularMode (PropertyMode mode) {
      if (mode != myLineColorMode) {
         myLineColorMode = PropertyUtils.setModeAndUpdate (
            this, "lineColorSpecular", myLineColorMode, mode);
      }
   }

   // property lineColorEmission
   public Color getLineColorEmission() {
      return getColorEmission(myLineMaterial, true);
   }

   public float[] getLineColorEmissionArray() {
      if (myLineMaterial != null) {
         return myLineMaterial.getEmission();
      } else {
         return Arrays.copyOf(Material.default_emission, 4);
      }
   }

   public void setLineColorEmission (Color color) {
      if (myLineMaterial == null) {
         myLineMaterial = createEmissionMaterial(color, myLineColor);
      } else {
         myLineMaterial.setEmission(color);
      }
      myLineColorMode =
         PropertyUtils.propagateValue (
            this, "lineColorEmission", color, myLineColorMode);
   }

   public void getLineColorEmission (float[] color) {
      getColorEmission(myLineMaterial, color);
   }

   public PropertyMode getLineColorEmissionMode() {
      return getLineColorMode();
   }

   public void setLineColorEmissionMode (PropertyMode mode) {
      if (mode != myLineColorMode) {
         myLineColorMode = PropertyUtils.setModeAndUpdate (
            this, "lineColorEmission", myLineColorMode, mode);
      }
   }

   // property pointColor

   public Color getPointColor() {
      return new Color (myPointColor[0], myPointColor[1], myPointColor[2]);
   }

   public float[] getPointColorArray() {
      return myPointColor;
   }

   public void setPointColor (Color color) {
      if (doSetColor (myPointColor, color)) {
         myPointMaterial = null;
      }
      myPointColorMode =
         PropertyUtils.propagateValue (
            this, "pointColor", color, myPointColorMode);
   }

   public void getPointColor (float[] color) {
      color[0] = myPointColor[0];
      color[1] = myPointColor[1];
      color[2] = myPointColor[2];
   }

   public void setPointColor (float[] color) {
      if (doSetColor (myPointColor, color)) {
         myPointMaterial = null;
      }
      myPointColorMode =
         PropertyUtils.propagateValue (
            this, "pointColor", color, myPointColorMode);
   }

   public PropertyMode getPointColorMode() {
      return myPointColorMode;
   }

   public void setPointColorMode (PropertyMode mode) {
      if (mode != myPointColorMode) {
         myPointColorMode =
            PropertyUtils.setModeAndUpdate (
               this, "pointColor", myPointColorMode, mode);
      }
   }

   // property pointColorDiffuse

   public Color getPointColorDiffuse() {
      return getPointColor();
   }

   public float[] getPointColorDiffuseArray() {
      if (myPointMaterial != null) {
         return myPointMaterial.getDiffuse();
      } 

      float[] out = Arrays.copyOf(myPointColor, 4);
      out[3] = myAlpha;
      return out;
   }

   public void setPointColorDiffuse (Color color) {
      setPointColor(color);
   }

   public void getPointColorDiffuse (float[] color) {
      getPointColor(color);
      if (color.length > 3) {
         color[3] = myAlpha;
      }
   }

   public PropertyMode getPointColorDiffuseMode() {
      return getPointColorMode();
   }

   public void setPointColorDiffuseMode (PropertyMode mode) {
      setPointColorMode(mode);
   }

   // property pointColorAmbient
   public Color getPointColorAmbient() {
      return getColorAmbient(myPointMaterial, true);
   }

   public float[] getPointColorAmbientArray() {
      if (myPointMaterial != null) {
         return myPointMaterial.getAmbient();
      } else {
         return Arrays.copyOf(Material.default_ambient, 4);
      }
   }

   public void setPointColorAmbient (Color color) {
      if (myPointMaterial == null) {
         myPointMaterial = createAmbientMaterial(color, myPointColor);
      } else {
         myPointMaterial.setAmbient(color);
      }
      myPointColorMode =
         PropertyUtils.propagateValue (
            this, "pointColorAmbient", color, myPointColorMode);
   }

   public void getPointColorAmbient (float[] color) {
      getColorAmbient(myPointMaterial, color);
   }

   public PropertyMode getPointColorAmbientMode() {
      return getPointColorMode();
   }

   public void setPointColorAmbientMode (PropertyMode mode) {
      if (mode != myPointColorMode) {
         myPointColorMode =
            PropertyUtils.setModeAndUpdate (
               this, "pointColorAmbient", myPointColorMode, mode);
      }
   }

   // property pointColorSpecular
   public Color getPointColorSpecular() {
      return getColorSpecular(myPointMaterial, true);
   }

   public float[] getPointColorSpecularArray() {
      if (myPointMaterial != null) {
         return myPointMaterial.getSpecular();
      } else {
         return Arrays.copyOf(Material.default_specular, 4);
      }
   }

   public void setPointColorSpecular (Color color) {
      if (myPointMaterial == null) {
         myPointMaterial = createSpecularMaterial(color, myPointColor);
      } else {
         myPointMaterial.setSpecular(color);
      }
      myPointColorMode =
         PropertyUtils.propagateValue (
            this, "pointColorSpecular", color, myPointColorMode);
   }

   public void getPointColorSpecular (float[] color) {
      getColorSpecular(myPointMaterial, color);
   }

   public PropertyMode getPointColorSpecularMode() {
      return getPointColorMode();
   }

   public void setPointColorSpecularMode (PropertyMode mode) {
      if (mode != myPointColorMode) {
         myPointColorMode =
            PropertyUtils.setModeAndUpdate (
               this, "pointColorSpecular", myPointColorMode, mode);
      }
   }

   // property pointColorEmission
   public Color getPointColorEmission() {
      return getColorEmission(myPointMaterial, true);
   }

   public float[] getPointColorEmissionArray() {
      if (myPointMaterial != null) {
         return myPointMaterial.getEmission();
      } else {
         return Arrays.copyOf(Material.default_emission, 4);
      }
   }

   public void setPointColorEmission (Color color) {
      if (myPointMaterial == null) {
         myPointMaterial = createEmissionMaterial(color, myPointColor);
      } else {
         myPointMaterial.setEmission(color);
      }
      myPointColorMode =
         PropertyUtils.propagateValue (
            this, "pointColorEmission", color, myPointColorMode);
   }

   public void getPointColorEmission (float[] color) {
      getColorEmission(myPointMaterial, color);
   }

   public PropertyMode getPointColorEmissionMode() {
      return getPointColorMode();
   }

   public void setPointColorEmissionMode (PropertyMode mode) {
      if (mode != myPointColorMode) {
         myPointColorMode =
            PropertyUtils.setModeAndUpdate (
               this, "pointColorEmission", myPointColorMode, mode);
      }
   }


   public Material getBackMaterial() {
      if (myBackMaterial == null && myBackColor != null) {
         myBackMaterial =
            Material.createDiffuse (myBackColor, myAlpha, myShininess);
      } else if (myBackColor == null) {
         return null;
      }
      return myBackMaterial;
   }

   public Material getLineMaterial() {
      if (myLineMaterial == null) {
         myLineMaterial =
            Material.createDiffuse (myLineColor, myAlpha, myShininess);
      }
      return myLineMaterial;
   }

   public Material getPointMaterial() {
      if (myPointMaterial == null) {
         myPointMaterial =
            Material.createDiffuse (myPointColor, myAlpha, myShininess);
      }
      return myPointMaterial;
   }

   public TextureProps getTextureProps() {
      return myTextureProps;
   }

   public void setTextureProps (TextureProps props) {
      if (getAllPropertyInfo().get ("textureProps") == null) {
         return;
      }
      if (props == null) {
         PropertyUtils.updateCompositeProperty (
            this, "textureProps", myTextureProps, null);
         myTextureProps = null;
      }
      else {
         if (myTextureProps == null) {
            myTextureProps = new TextureProps();
            myTextureProps.set (props);
            PropertyUtils.updateCompositeProperty (
               this, "textureProps", null, myTextureProps);
         }
         else {
            myTextureProps.set (props);
            PropertyUtils.updateCompositeProperty (myTextureProps);
         }
      }
      clearMeshDisplayList();
   }

   public Property getProperty (String name) {
      return PropertyList.getProperty (name, this);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   protected void setDefaultModes() {
      myVisibleMode = INHERITED;
      myLineWidthMode = INHERITED;
      myPointSizeMode = INHERITED;
      myFaceColorMode = INHERITED;
      myAlphaMode = INHERITED;
      myZOrderMode = INHERITED;
      myShadingMode = INHERITED;
      myShininessMode = INHERITED;
      myPointStyleMode = INHERITED;
      myPointRadiusMode = INHERITED;
      myPointSlicesMode = INHERITED;
      myLineStyleMode = INHERITED;
      myLineRadiusMode = INHERITED;
      myLineSlicesMode = INHERITED;
      myDrawEdgesMode = INHERITED;
      myEdgeWidthMode = INHERITED;
      myEdgeColorMode = INHERITED;
      myFaceStyleMode = INHERITED;
      myLineColorMode = INHERITED;
      myPointColorMode = INHERITED;
      myBackColorMode = INHERITED;
   }

   protected void setDefaultValues() {
      myVisibleP = defaultVisibleP;
      myLineWidth = defaultLineWidth;
      myPointSize = defaultPointSize;
      doSetColor (myFaceColor, defaultFaceColor);
      myAlpha = defaultAlpha;
      myZOrder = defaultZOrder;
      myShading = defaultShading;
      myShininess = defaultShininess;
      myPointStyle = defaultPointStyle;
      myPointRadius = defaultPointRadius;
      myPointSlices = defaultPointSlices;
      myLineStyle = defaultLineStyle;
      myLineRadius = defaultLineRadius;
      myLineSlices = defaultLineSlices;
      myDrawEdgesP = defaultDrawEdgesP;
      myEdgeWidth = defaultEdgeWidth;
      if (defaultEdgeColor == null) {
         myEdgeColor = null;
      }
      else {
         doSetEdgeColor (defaultEdgeColor);
      }
      myFaceStyle = defaultFaceStyle;
      if (myLineColor == null) {
         myLineColor = new float[3];
      }
      doSetColor (myLineColor, defaultLineColor);
      if (myPointColor == null) {
         myPointColor = new float[3];
      }
      doSetColor (myPointColor, defaultPointColor);
      doSetBackColor (defaultBackColor);
      myTextureProps = defaultTextureProps;

      myFaceMaterial = null;
      myBackMaterial = null;
      myLineMaterial = null;
      myPointMaterial = null;
   }

   public void set (RenderProps r) {
      myVisibleP = r.myVisibleP;
      myVisibleMode = r.myVisibleMode;
      doSetAlpha (r.myAlpha);
      myAlphaMode = r.myAlphaMode;
      myZOrder = r.myZOrder;
      myZOrderMode = r.myZOrderMode;
      myShading = r.myShading;
      myShadingMode = r.myShadingMode;
      myShininess = r.myShininess;
      myShininessMode = r.myShininessMode;

      myFaceStyle = r.myFaceStyle;
      myFaceStyleMode = r.myFaceStyleMode;
      if (r.myFaceMaterial == null) {
         doSetColor (myFaceColor, r.myFaceColor);
         myFaceMaterial = null;
      } else {
         setFaceMaterial(r.myFaceMaterial);
      }
      myFaceColorMode = r.myFaceColorMode;
      if (r.myBackMaterial == null) {
         doSetBackColor (r.myBackColor);  
         myBackMaterial = null;
      } else {
         setBackMaterial(r.myBackMaterial);
      }
      myBackColorMode = r.myBackColorMode;
      myDrawEdgesP = r.myDrawEdgesP;
      myDrawEdgesMode = r.myDrawEdgesMode;
      myEdgeWidth = r.myEdgeWidth;
      myEdgeWidthMode = r.myEdgeWidthMode;
      if (r.myEdgeColor == null) {
         myEdgeColor = null;
      }
      else {
         doSetEdgeColor (r.myEdgeColor);
      }
      myEdgeColorMode = r.myEdgeColorMode;
      myTexturePropsInactive = r.myTexturePropsInactive;
      setTextureProps (r.myTextureProps);

      myLineStyle = r.myLineStyle;
      myLineStyleMode = r.myLineStyleMode;
      if (r.myLineMaterial == null) {
         doSetColor (myLineColor, r.myLineColor);
         myLineMaterial = null;
      } else {
         setLineMaterial(r.myLineMaterial);
      }
      myLineColorMode = r.myLineColorMode;
      myLineWidth = r.myLineWidth;
      myLineWidthMode = r.myLineWidthMode;
      myLineRadius = r.myLineRadius;
      myLineRadiusMode = r.myLineRadiusMode;
      myLineSlices = r.myLineSlices;
      myLineSlicesMode = r.myLineSlicesMode;

      myPointStyle = r.myPointStyle;
      myPointStyleMode = r.myPointStyleMode;
      if (r.myPointMaterial == null) {
         doSetColor (myPointColor, r.myPointColor);
         myPointMaterial = null;
      } else {
         setPointMaterial(r.myPointMaterial);
      }

      myPointColorMode = r.myPointColorMode;
      myPointSize = r.myPointSize;
      myPointSizeMode = r.myPointSizeMode;
      myPointRadius = r.myPointRadius;
      myPointRadiusMode = r.myPointRadiusMode;
      myPointSlices = r.myPointSlices;
      myPointSlicesMode = r.myPointSlicesMode;

      clearAllDisplayLists();
      
      //taperedEllipsoidDisplayList = r.taperedEllipsoidDisplayList;
      //sphereDisplayList = r.sphereDisplayList;    
      //meshDisplayList = 0; // clear mesh display list
      //edgeDisplayList = 0;

   }

   public void setFaceMaterial(Material mat) {
      if (mat == null) {
         myFaceMaterial = null;
      } else {
         if (myFaceMaterial != null) {
            myFaceMaterial.set(mat);
         } else {
            myFaceMaterial = new Material(mat);
         }
         doSetColor(myFaceColor, myFaceMaterial.getDiffuse());
      }
   }

   public void setBackMaterial(Material mat) {
      if (mat == null) {
         myBackMaterial = null;
      } else {
         if (myBackMaterial != null) {
            myBackMaterial.set(mat);
         } else {
            myBackMaterial = new Material(mat);
         }
         doSetBackColor(myBackMaterial.getDiffuse());
      }
   }

   public void setPointMaterial(Material mat) {
      if (mat == null) {
         myPointMaterial = null;
      } else {
         if (myPointMaterial != null) {
            myPointMaterial.set(mat);
         } else {
            myPointMaterial = new Material(mat);
         }
         doSetColor(myPointColor, myPointMaterial.getDiffuse());
      }
   }

   public void setLineMaterial(Material mat) {
      if (mat == null) {
         myLineMaterial = null;
      } else {
         if (myLineMaterial != null) {
            myLineMaterial.set(mat);
         } else {
            myLineMaterial = new Material(mat);
         }
         doSetColor(myLineColor, myLineMaterial.getDiffuse());
      }
   }

   protected boolean texturePropsEqual (TextureProps props) {

      if (props == null) {
         return myTextureProps == null;
      }
      else if (myTextureProps == null) {
         return false;
      }
      else {
         return myTextureProps.equals (props);
      }
   }

   public boolean equals (RenderProps r) {
      if (myVisibleMode != r.myVisibleMode) {
         return false;
      }
      else if (myVisibleMode == EXPLICIT && myVisibleP != r.myVisibleP) {
         return false;
      }
      if (myLineWidthMode != r.myLineWidthMode) {
         return false;
      }
      else if (myLineWidthMode == EXPLICIT && myLineWidth != r.myLineWidth) {
         return false;
      }
      if (myPointSizeMode != r.myPointSizeMode) {
         return false;
      }
      else if (myPointSizeMode == EXPLICIT && myPointSize != r.myPointSize) {
         return false;
      }
      if (myFaceColorMode != r.myFaceColorMode) {
         return false;
      }
      else if (myFaceColorMode == EXPLICIT) {
         if (!ArraySupport.equals (myFaceColor, r.myFaceColor)) {
            return false;
         }
         else if (!materialsEqual (myFaceMaterial, r.myFaceMaterial)) {
            return false;
         }
      }
      if (myAlphaMode != r.myAlphaMode) {
         return false;
      }
      else if (myAlphaMode == EXPLICIT && myAlpha != r.myAlpha) {
         return false;
      }
      if (myZOrderMode != r.myZOrderMode) {
         return false;
      }
      else if (myZOrderMode == EXPLICIT && myZOrder != r.myZOrder) {
         return false;
      }
      if (myShadingMode != r.myShadingMode) {
         return false;
      }
      else if (myShadingMode == EXPLICIT && myShading != r.myShading) {
         return false;
      }
      if (myShininessMode != r.myShininessMode) {
         return false;
      }
      else if (myShininessMode == EXPLICIT && myShininess != r.myShininess) {
         return false;
      }
      if (myPointStyleMode != r.myPointStyleMode) {
         return false;
      }
      else if (myPointStyleMode == EXPLICIT && myPointStyle != r.myPointStyle) {
         return false;
      }
      if (myPointRadiusMode != r.myPointRadiusMode) {
         return false;
      }
      else if (myPointRadiusMode == EXPLICIT &&
         myPointRadius != r.myPointRadius) {
         return false;
      }
      if (myPointSlicesMode != r.myPointSlicesMode) {
         return false;
      }
      else if (myPointSlicesMode == EXPLICIT &&
         myPointSlices != r.myPointSlices) {
         return false;
      }
      if (myLineStyleMode != r.myLineStyleMode) {
         return false;
      }
      else if (myLineStyleMode == EXPLICIT && myLineStyle != r.myLineStyle) {
         return false;
      }
      if (myLineRadiusMode != r.myLineRadiusMode) {
         return false;
      }
      else if (myLineRadiusMode == EXPLICIT &&
         myLineRadius != r.myLineRadius) {
         return false;
      }
      if (myLineSlicesMode != r.myLineSlicesMode) {
         return false;
      }
      else if (myLineSlicesMode == EXPLICIT &&
         myLineSlices != r.myLineSlices) {
         return false;
      }
      if (myDrawEdgesMode != r.myDrawEdgesMode) {
         return false;
      }
      else if (myDrawEdgesMode == EXPLICIT && myDrawEdgesP != r.myDrawEdgesP) {
         return false;
      }
      if (myEdgeWidthMode != r.myEdgeWidthMode) {
         return false;
      }
      else if (myEdgeWidthMode == EXPLICIT && myEdgeWidth != r.myEdgeWidth) {
         return false;
      }
      if (myEdgeColorMode != r.myEdgeColorMode) {
         return false;
      }
      else if (myEdgeColorMode == EXPLICIT) {
         if ((myEdgeColor == null) != (r.myEdgeColor == null)) {
            return false;
         }
         else if (myEdgeColor != null &&
                  !ArraySupport.equals (myEdgeColor, r.myEdgeColor)) {
            return false;
         }
      }
      if (myFaceStyleMode != r.myFaceStyleMode) {
         return false;
      }
      else if (myFaceStyleMode == EXPLICIT && myFaceStyle != r.myFaceStyle) {
         return false;
      }
      if (myLineColorMode != r.myLineColorMode) {
         return false;
      }
      else if (myLineColorMode == EXPLICIT) {
         if (!ArraySupport.equals (myLineColor, r.myLineColor)) {
            return false;
         }
         else if (!materialsEqual (myLineMaterial, r.myLineMaterial)) {
            return false;
         }
      }
      if (myPointColorMode != r.myPointColorMode) {
         return false;
      }
      else if (myPointColorMode == EXPLICIT) {
         if (!ArraySupport.equals (myPointColor, r.myPointColor)) {
            return false;
         }
         else if (!materialsEqual (myPointMaterial, r.myPointMaterial)) {
            return false;
         }
      }
      if (myBackColorMode != r.myBackColorMode) {
         return false;
      }
      else if (myBackColorMode == EXPLICIT) {
         if ((myBackColor == null) != (r.myBackColor == null)) {
            return false;
         }
         else if (myBackColor != null &&
                  !ArraySupport.equals (myBackColor, r.myBackColor)) {
            return false;
         }
         else if (!materialsEqual (myBackMaterial, r.myBackMaterial)) {
            return false;
         }
      }
      if (myTexturePropsInactive != r.myTexturePropsInactive) {
         return false;
      }
      else if (!myTexturePropsInactive && !texturePropsEqual (r.myTextureProps)) {
         return false;
      }
      return true;
   }

   public static boolean equals (RenderProps r1, RenderProps r2) {
      if (r1 == null && r2 == null) {
         return true;
      }
      else if (r1 != null && r2 != null) {
         return r1.equals (r2);
      }
      else {
         return false;
      }
   }

   public boolean equals (Object obj) {
      if (obj instanceof RenderProps) {
         return equals ((RenderProps)obj);
      }
      else {
         return false;
      }
   }

   public boolean isWritable() {
      return true;
   }

   public void write (PrintWriter pw, NumberFormat fmt, Object ref)
      throws IOException {

      pw.println ("[ ");
      IndentingPrintWriter.addIndentation (pw, 2);
      getAllPropertyInfo().writeNonDefaultProps (this, pw, fmt);
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

   public int getTaperedEllipsoidDisplayList (GL2 gl) {
      if (taperedEllipsoidDisplayList < 1) {
         Object key =
            DisplayListManager.createKey ("taperedEllipsoid", myLineSlices);
         taperedEllipsoidDisplayList =
            DisplayListManager.getSharedList (gl, key);
      }
      return taperedEllipsoidDisplayList;
   }

   public int allocTaperedEllipsoidDisplayList (GL2 gl) {
      Object key =
         DisplayListManager.createKey ("taperedEllipsoid", myLineSlices);
      taperedEllipsoidDisplayList =
         DisplayListManager.allocSharedList (gl, key);
      return taperedEllipsoidDisplayList;
   }

   public void clearTaperedEllipsoidDisplayList() {
      if (taperedEllipsoidDisplayList > 0) {
         Object key =
            DisplayListManager.createKey ("taperedEllipsoid", myLineSlices);
         DisplayListManager.freeSharedList (key);
         taperedEllipsoidDisplayList = 0;
         clearMeshDisplayList();  // mesh might be dependent on tapered ellipsoid
      }
   }

   public int getSphereDisplayList (GL2 gl) {
      if (sphereDisplayList < 1) {
         Object key = DisplayListManager.createKey ("sphere", myPointSlices);
         sphereDisplayList = DisplayListManager.getSharedList (gl, key);
      }
      return sphereDisplayList;
   }

   public int allocSphereDisplayList (GL2 gl) {
      Object key = DisplayListManager.createKey ("sphere", myPointSlices);
      sphereDisplayList = DisplayListManager.allocSharedList (gl, key);
      return sphereDisplayList;
   }

   public void clearSphereDisplayList() {
      if (sphereDisplayList > 0) {
         Object key = DisplayListManager.createKey ("sphere", myPointSlices);
         DisplayListManager.freeSharedList (key);
         sphereDisplayList = 0;
         clearMeshDisplayList();  // mesh might be depending on old sphere display list
      }
   }

   public int getMeshDisplayList() {
      return meshDisplayList;
   }

   public int allocMeshDisplayList (GL2 gl) {
      meshDisplayList = DisplayListManager.allocList (gl);
      return meshDisplayList;
   }

   public void clearMeshDisplayList() {
      if (meshDisplayList > 0) {
         DisplayListManager.freeList (meshDisplayList);
         meshDisplayList = 0;
      }
      
      // also clear edges
      clearEdgeDisplayList();
   }
   
   public void clearEdgeDisplayList() {
      if (edgeDisplayList > 0) {
         DisplayListManager.freeList(edgeDisplayList);
         edgeDisplayList = 0;
      }
   }
   
   public int allocEdgeDisplayList (GL2 gl) {
      edgeDisplayList = DisplayListManager.allocList (gl);
      return edgeDisplayList;
   }
   
   public int getEdgeDisplayList() {
      return edgeDisplayList;
   }

   public void clearAllDisplayLists() {
      clearMeshDisplayList();
      clearEdgeDisplayList();
      clearSphereDisplayList();
      clearTaperedEllipsoidDisplayList();
   }

   private static RenderProps initRenderProps (
      RenderProps props, HasProperties host) {
      if (host != null) {
         PropertyUtils.updateInheritedProperties (props, host, "renderProps");
      }
      return props;
   }

   /**
    * Creates a new RenderProps. If the supplied host is non-null, then it is
    * used to initialize any inherited properties. The property name associated
    * with the host is assumed to be "renderProps".
    * 
    * @param host
    * if non-null, is used to initialize inherited values
    * @return render properties appropriate for lines
    */
   public static RenderProps createRenderProps (HasProperties host) {
      return initRenderProps (new RenderProps(), host);
   }

   /**
    * Creates a new LineRenderProps. If the supplied host is non-null, then it
    * is used to initialize any inherited properties. The property name
    * associated with the host is assumed to be "renderProps".
    * 
    * @param host
    * if non-null, is used to initialize inherited values
    * 
    * @return render properties appropriate for lines
    */
   public static RenderProps createLineProps (HasProperties host) {
      return initRenderProps (new LineRenderProps(), host);
   }

   /**
    * Creates a new LineFaceRenderProps. If the supplied host is non-null, then it
    * is used to initialize any inherited properties. The property name
    * associated with the host is assumed to be "renderProps".
    * 
    * @param host
    * if non-null, is used to initialize inherited values
    * 
    * @return render properties appropriate for lines
    */
   public static RenderProps createLineFaceProps (HasProperties host) {
      return initRenderProps (new LineFaceRenderProps(), host);
   }

   /**
    * Creates a new LineEdgeRenderProps. If the supplied host is non-null, then it
    * is used to initialize any inherited properties. The property name
    * associated with the host is assumed to be "renderProps".
    * 
    * @param host
    * if non-null, is used to initialize inherited values
    * 
    * @return render properties appropriate for lines
    */
   public static RenderProps createLineEdgeProps (HasProperties host) {
      return initRenderProps (new LineEdgeRenderProps(), host);
   }

   /**
    * Creates a new PointLineRenderProps. If the supplied host is non-null, then
    * it is used to initialize any inherited properties. The property name
    * associated with the host is assumed to be "renderProps".
    * 
    * @param host
    * if non-null, is used to initialize inherited values
    * 
    * @return render properties appropriate for lines
    */
   public static RenderProps createPointLineProps (HasProperties host) {
      return initRenderProps (new PointLineRenderProps(), host);
   }

   /**
    * Creates a new PointFaceRenderProps. If the supplied host is non-null, then
    * it is used to initialize any inherited properties. The property name
    * associated with the host is assumed to be "renderProps".
    * 
    * @param host
    * if non-null, is used to initialize inherited values
    * 
    * @return render properties appropriate for lines
    */
   public static RenderProps createPointFaceProps (HasProperties host) {
      return initRenderProps (new PointFaceRenderProps(), host);
   }

   /**
    * Creates a new PointRenderProps. If the supplied host is non-null, then it
    * is used to initialize any inherited properties. The property name
    * associated with the host is assumed to be "renderProps".
    * 
    * @param host
    * if non-null, is used to initialize inherited values
    * 
    * @return render properties appropriate for lines
    */
   public static RenderProps createPointProps (HasProperties host) {
      return initRenderProps (new PointRenderProps(), host);
   }

   /**
    * Creates a new MeshRenderProps. If the supplied host is non-null, then it
    * is used to initialize any inherited properties. The property name
    * associated with the host is assumed to be "renderProps".
    * 
    * @param host
    * if non-null, is used to initialize inherited values
    * 
    * @return render properties appropriate for lines
    */
   public static RenderProps createMeshProps (HasProperties host) {
      return initRenderProps (new MeshRenderProps(), host);
   }

   /**
    * Creates a new FaceRenderProps. If the supplied host is non-null, then it
    * is used to initialize any inherited properties. The property name
    * associated with the host is assumed to be "renderProps".
    * 
    * @param host
    * if non-null, is used to initialize inherited values
    * 
    * @return render properties appropriate for lines
    */
   public static RenderProps createFaceProps (HasProperties host) {
      return initRenderProps (new FaceRenderProps(), host);
   }

   /**
    * Scales the properties that are associated with distance. This includes the
    * sphere radius and cylinder radius.
    * 
    * @param s
    * scale factor
    */
   public void scaleDistance (double s) {
      if (myPointRadiusMode == EXPLICIT) {
         setPointRadius (myPointRadius * s);
      }
      if (myLineRadiusMode == EXPLICIT) {
         setLineRadius (myLineRadius * s);
      }
   }

   public void dispose() {
      clearAllDisplayLists();
   }

   protected void finalize() throws Throwable {
      dispose();
      super.finalize();
   }

   protected String colorString (float[] color) {
      if (color == null) {
         return "null";
      }
      else {
         return ("(" + (int)(255 * color[0]) + "," + (int)(255 * color[1]) +
            "," + (int)(255 * color[2]) + ")");
      }
   }

   public String toString() {
      StringBuffer buf = new StringBuffer (1024);
      buf.append ("Visible=" + myVisibleP + " " + myVisibleMode + ", ");
      buf.append ("Alpha=" + myAlpha + " " + myAlphaMode + ", ");
      buf.append ("ZOrder=" + myZOrder + " " + myZOrderMode + ", ");
      buf.append ("Shading=" + myShading + " " + myShadingMode + ", ");
      buf.append ("Shininess=" + myShininess + " " + myShininessMode + ", ");
      buf.append ("FaceStyle=" + myFaceStyle + " " + myFaceStyleMode + ", ");
      buf.append ("FaceColor=" + colorString (myFaceColor) + " " +
         myFaceColorMode + ", ");
      buf.append("FaceMaterial=" + myFaceMaterial + " " +
         myFaceColorMode);
      buf.append ("BackColor=" + colorString (myBackColor) + " " +
         myBackColorMode + ", ");
      buf.append("BackMaterial=" + myBackMaterial + " " +
         myBackColorMode);
      buf.append ("DrawEdges=" + myDrawEdgesP + " " + myDrawEdgesMode + ", ");
      buf.append ("EdgeWidth=" + myEdgeWidth + " " + myEdgeWidthMode + ", ");
      buf.append ("EdgeColor=" + colorString (myEdgeColor) + " " +
         myEdgeColorMode + ", ");
      buf.append ("TextureProps=" + myTextureProps + ", ");
      buf.append ("LineStyle=" + myLineStyle + " " + myLineStyleMode + ", ");
      buf.append ("LineColor=" + colorString (myLineColor) + " " +
         myLineColorMode + ", ");
      buf.append("LineMaterial=" + myLineMaterial + " " +
         myLineColorMode);
      buf.append ("LineWidth=" + myLineWidth + " " + myLineWidthMode + ", ");
      buf.append ("LineRadius=" + myLineRadius + " " +
         myLineRadiusMode + ", ");
      buf.append ("LineSlices=" + myLineSlices + " " +
         myLineSlicesMode + ", ");
      buf.append ("PointStyle=" + myPointStyle + " " + myPointStyleMode + ", ");
      buf.append ("PointColor=" + colorString (myPointColor) + " " +
         myPointColorMode + ", ");
      buf.append("PointMaterial=" + myPointMaterial + " " +
         myPointColorMode + ", ");
      buf.append ("PointSize=" + myPointSize + " " + myPointSizeMode + ", ");
      buf.append ("PointRadius=" + myPointRadius + " " + myPointRadiusMode +
         ", ");
      buf.append ("PointSlices=" + myPointSlices + " " + myPointSlicesMode);
      return buf.toString();
   }
   
   public RenderProps copy() {
      return clone();
   }

   public RenderProps clone() {
      RenderProps props = null;
      try {
         props = (RenderProps)super.clone();
      }
      catch (CloneNotSupportedException e) {
         throw new InternalErrorException ("cannot clone super in RenderProps");
      }

      props.myPropInfo = null;
      props.myPropHost = null;

      // create new objects
      if (props.myBackColor != null) {
         props.myBackColor = new float[3];
      }
      if (props.myBackMaterial != null) {
         props.myBackMaterial = new Material();
      }
      if (props.myEdgeColor != null) {
         props.myEdgeColor = new float[3];
      }
      
      props.myFaceColor = new float[3];
      if (props.myFaceMaterial != null) {
         props.myFaceMaterial = new Material();
      }
      
      props.myLineColor = new float[3];
      if (props.myLineMaterial != null) {
         props.myLineMaterial = new Material();
      }
      
      props.myPointColor = new float[3];
      if (props.myPointMaterial != null) {
         props.myPointMaterial = new Material();
      }
      
      props.set(this);
      return props;
   }

   protected static RenderProps createAndAssignProps (Renderable r) {
      RenderProps props = r.createRenderProps();
      if (r.getRenderProps() != null) {
         props.set (r.getRenderProps());
      }
      return props;
   }

   private static TextureProps createAndAssignTextureProps (RenderProps props) {
      TextureProps tprops = props.getTextureProps();
      if (tprops == null) {
         tprops = new TextureProps();
         props.setTextureProps (tprops);
      }
      return tprops;
   }

   public static void setVisible (Renderable r, boolean visible) {
      RenderProps props = createAndAssignProps (r);
      props.setVisible (visible);
      r.setRenderProps (props);
   }

   public static void setVisibleMode (Renderable r, PropertyMode mode) {
      RenderProps props = createAndAssignProps (r);
      props.setVisibleMode (mode);
      r.setRenderProps (props);
   }

   public static void setShading (Renderable r, Shading shading) {
      RenderProps props = createAndAssignProps (r);
      props.setShading (shading);
      r.setRenderProps (props);
   }

   public static void setShadingMode (Renderable r, PropertyMode mode) {
      RenderProps props = createAndAssignProps (r);
      props.setShadingMode (mode);
      r.setRenderProps (props);
   }

   public static void setAlpha (Renderable r, double alpha) {
      RenderProps props = createAndAssignProps (r);
      props.setAlpha (alpha);
      r.setRenderProps (props);
   }

   public static void setAlphaMode (Renderable r, PropertyMode mode) {
      RenderProps props = createAndAssignProps (r);
      props.setAlphaMode (mode);
      r.setRenderProps (props);
   }

   public static void setZOrder(Renderable r, int order) {
      RenderProps props = createAndAssignProps (r);
      props.setZOrder (order);
      r.setRenderProps (props);
   }

   public static void setZOrderMode (Renderable r, PropertyMode mode) {
      RenderProps props = createAndAssignProps (r);
      props.setZOrderMode (mode);
      r.setRenderProps (props);
   }

   public static void setShininess (Renderable r, double shininess) {
      RenderProps props = createAndAssignProps (r);
      props.setShininess ((float)shininess);
      r.setRenderProps (props);
   }

   public static void setShininessMode (Renderable r, PropertyMode mode) {
      RenderProps props = createAndAssignProps (r);
      props.setShininessMode (mode);
      r.setRenderProps (props);
   }

   public static void setFaceStyle (Renderable r, Faces style) {
      RenderProps props = createAndAssignProps (r);
      props.setFaceStyle (style);
      r.setRenderProps (props);
   }

   public static void setFaceStyleMode (Renderable r, PropertyMode mode) {
      RenderProps props = createAndAssignProps (r);
      props.setFaceStyleMode (mode);
      r.setRenderProps (props);
   }

   public static void setFaceColor (Renderable r, Color color) {
      RenderProps props = createAndAssignProps (r);
      props.setFaceColor (color);
      r.setRenderProps (props);
   }

   public static void setFaceColorMode (Renderable r, PropertyMode mode) {
      RenderProps props = createAndAssignProps (r);
      props.setFaceColorMode (mode);
      r.setRenderProps (props);
   }

   public static void setBackColor (Renderable r, Color color) {
      RenderProps props = createAndAssignProps (r);
      props.setBackColor (color);
      r.setRenderProps (props);
   }

   public static void setBackColorMode (Renderable r, PropertyMode mode) {
      RenderProps props = createAndAssignProps (r);
      props.setBackColorMode (mode);
      r.setRenderProps (props);
   }

   public static void setDrawEdges (Renderable r, boolean drawEdges) {
      RenderProps props = createAndAssignProps (r);
      props.setDrawEdges (drawEdges);
      r.setRenderProps (props);
   }

   public static void setDrawEdgesMode (Renderable r, PropertyMode mode) {
      RenderProps props = createAndAssignProps (r);
      props.setDrawEdgesMode (mode);
      r.setRenderProps (props);
   }

   public static void setEdgeWidth (Renderable r, int width) {
      RenderProps props = createAndAssignProps (r);
      props.setEdgeWidth (width);
      r.setRenderProps (props);
   }

   public static void setEdgeWidthMode (Renderable r, PropertyMode mode) {
      RenderProps props = createAndAssignProps (r);
      props.setEdgeWidthMode (mode);
      r.setRenderProps (props);
   }

   public static void setEdgeColor (Renderable r, Color color) {
      RenderProps props = createAndAssignProps (r);
      props.setEdgeColor (color);
      r.setRenderProps (props);
   }

   public static void setEdgeColorMode (Renderable r, PropertyMode mode) {
      RenderProps props = createAndAssignProps (r);
      props.setEdgeColorMode (mode);
      r.setRenderProps (props);
   }

   public static void setLineStyle (Renderable r, LineStyle style) {
      RenderProps props = createAndAssignProps (r);
      props.setLineStyle (style);
      r.setRenderProps (props);
   }

   public static void setLineStyleMode (Renderable r, PropertyMode mode) {
      RenderProps props = createAndAssignProps (r);
      props.setLineStyleMode (mode);
      r.setRenderProps (props);
   }

   public static void setLineColor (Renderable r, Color color) {
      RenderProps props = createAndAssignProps (r);
      props.setLineColor (color);
      r.setRenderProps (props);
   }

   public static void setLineColorMode (Renderable r, PropertyMode mode) {
      RenderProps props = createAndAssignProps (r);
      props.setLineColorMode (mode);
      r.setRenderProps (props);
   }

   public static void setLineWidth (Renderable r, int width) {
      RenderProps props = createAndAssignProps (r);
      props.setLineWidth (width);
      r.setRenderProps (props);
   }

   public static void setLineWidthMode (Renderable r, PropertyMode mode) {
      RenderProps props = createAndAssignProps (r);
      props.setLineWidthMode (mode);
      r.setRenderProps (props);
   }

   public static void setLineRadius (Renderable r, double radius) {
      RenderProps props = createAndAssignProps (r);
      props.setLineRadius (radius);
      r.setRenderProps (props);
   }

   public static void setLineRadiusMode (Renderable r, PropertyMode mode) {
      RenderProps props = createAndAssignProps (r);
      props.setLineRadiusMode (mode);
      r.setRenderProps (props);
   }

   public static void setLineSlices (Renderable r, int nslices) {
      RenderProps props = createAndAssignProps (r);
      props.setLineSlices (nslices);
      r.setRenderProps (props);
   }

   public static void setLineSlicesMode (Renderable r, PropertyMode mode) {
      RenderProps props = createAndAssignProps (r);
      props.setLineSlicesMode (mode);
      r.setRenderProps (props);
   }

   public static void setPointStyle (Renderable r, PointStyle style) {
      RenderProps props = createAndAssignProps (r);
      props.setPointStyle (style);
      r.setRenderProps (props);
   }

   public static void setPointStyleMode (Renderable r, PropertyMode mode) {
      RenderProps props = createAndAssignProps (r);
      props.setPointStyleMode (mode);
      r.setRenderProps (props);
   }

   public static void setPointColor (Renderable r, Color color) {
      RenderProps props = createAndAssignProps (r);
      props.setPointColor (color);
      r.setRenderProps (props);
   }

   public static void setPointColorMode (Renderable r, PropertyMode mode) {
      RenderProps props = createAndAssignProps (r);
      props.setPointColorMode (mode);
      r.setRenderProps (props);
   }

   public static void setPointSize (Renderable r, int size) {
      RenderProps props = createAndAssignProps (r);
      props.setPointSize (size);
      r.setRenderProps (props);
   }

   public static void setPointSizeMode (Renderable r, PropertyMode mode) {
      RenderProps props = createAndAssignProps (r);
      props.setPointSizeMode (mode);
      r.setRenderProps (props);
   }

   public static void setPointRadius (Renderable r, double radius) {
      RenderProps props = createAndAssignProps (r);
      props.setPointRadius (radius);
      r.setRenderProps (props);
   }

   public static void setPointRadiusMode (Renderable r, PropertyMode mode) {
      RenderProps props = createAndAssignProps (r);
      props.setPointRadiusMode (mode);
      r.setRenderProps (props);
   }

   public static void setPointSlices (Renderable r, int nslices) {
      RenderProps props = createAndAssignProps (r);
      props.setPointSlices (nslices);
      r.setRenderProps (props);
   }

   public static void setPointSlicesMode (Renderable r, PropertyMode mode) {
      RenderProps props = createAndAssignProps (r);
      props.setPointSlicesMode (mode);
      r.setRenderProps (props);
   }

   public static void setSphericalPoints (Renderable r, double rad, Color color) {
      RenderProps props = createAndAssignProps (r);
      props.setPointStyle (PointStyle.SPHERE);
      props.setPointRadius (rad);
      if (color != null) {
         props.setPointColor (color);
      }
      r.setRenderProps (props);
   }

   public static void setCylindricalLines (
      Renderable r, double rad, Color color) {

      RenderProps props = createAndAssignProps (r);
      props.setLineStyle (LineStyle.CYLINDER);
      props.setLineRadius (rad);
      if (color != null) {
         props.setLineColor (color);
      }
      r.setRenderProps (props);
   }

   public static void setEllipsoidalLines (
      Renderable r, double rad, Color color) {

      RenderProps props = createAndAssignProps (r);
      props.setLineStyle (LineStyle.ELLIPSOID);
      props.setLineRadius (rad);
      if (color != null) {
         props.setLineColor (color);
      }
      r.setRenderProps (props);
   }

   public static void setTextureEnabled (Renderable r, boolean enabled) {
      RenderProps props = createAndAssignProps (r);
      TextureProps tprops = props.getTextureProps();
      if (tprops == null) {
         tprops = new TextureProps();
         props.setTextureProps (tprops);
      }
      tprops.setEnabled (enabled);
      r.setRenderProps (props);
   }

   public static void setTextureEnabledMode (Renderable r, PropertyMode mode) {
      RenderProps props = createAndAssignProps (r);
      TextureProps tprops = createAndAssignTextureProps (props);
      tprops.setEnabledMode (mode);
      r.setRenderProps (props);
   }

   public static void setTextureMode (Renderable r, TextureProps.Mode mode) {
      RenderProps props = createAndAssignProps (r);
      TextureProps tprops = createAndAssignTextureProps (props);
      tprops.setMode (mode);
      r.setRenderProps (props);
   }

   public static void setTextureModeMode (Renderable r, PropertyMode mode) {
      RenderProps props = createAndAssignProps (r);
      TextureProps tprops = createAndAssignTextureProps (props);
      tprops.setModeMode (mode);
      r.setRenderProps (props);
   }

   public static void setTextureFileName (Renderable r, String fileName) {
      RenderProps props = createAndAssignProps (r);
      TextureProps tprops = createAndAssignTextureProps (props);
      tprops.setFileName (fileName);
      r.setRenderProps (props);
   }

   public static void setTextureFileNameMode (Renderable r, PropertyMode mode) {
      RenderProps props = createAndAssignProps (r);
      TextureProps tprops = createAndAssignTextureProps (props);
      tprops.setFileNameMode (mode);
      r.setRenderProps (props);
   }

   public static void setTextureSphereMappingEnabled (
      Renderable r, boolean enabled) {
      RenderProps props = createAndAssignProps (r);
      TextureProps tprops = createAndAssignTextureProps (props);
      tprops.setSphereMappingEnabled (enabled);
      r.setRenderProps (props);
   }

   public static void setTextureSphereMappingMode (
      Renderable r, PropertyMode mode) {
      RenderProps props = createAndAssignProps (r);
      TextureProps tprops = createAndAssignTextureProps (props);
      tprops.setSphereMappingMode (mode);
      r.setRenderProps (props);
   }

   public static void setTextureAutomatic (Renderable r, boolean enabled) {
      RenderProps props = createAndAssignProps (r);
      TextureProps tprops = createAndAssignTextureProps (props);
      tprops.setAutomatic (enabled);
      r.setRenderProps (props);
   }

   public static void setTextureAutomaticMode (Renderable r, PropertyMode mode) {
      RenderProps props = createAndAssignProps (r);
      TextureProps tprops = createAndAssignTextureProps (props);
      tprops.setAutomaticMode (mode);
      r.setRenderProps (props);
   }

   public static void setTextureSCoords (Renderable r, double[] scoords) {
      RenderProps props = createAndAssignProps (r);
      TextureProps tprops = createAndAssignTextureProps (props);
      tprops.setSCoords (scoords);
      r.setRenderProps (props);
   }

   public static void setTextureSCoordsMode (Renderable r, PropertyMode mode) {
      RenderProps props = createAndAssignProps (r);
      TextureProps tprops = createAndAssignTextureProps (props);
      tprops.setSCoordsMode (mode);
      r.setRenderProps (props);
   }

   public static void setTextureTCoords (Renderable r, double[] tcoords) {
      RenderProps props = createAndAssignProps (r);
      TextureProps tprops = createAndAssignTextureProps (props);
      tprops.setSCoords (tcoords);
      r.setRenderProps (props);
   }

   public static void setTextureTCoordsMode (Renderable r, PropertyMode mode) {
      RenderProps props = createAndAssignProps (r);
      TextureProps tprops = createAndAssignTextureProps (props);
      tprops.setSCoordsMode (mode);
      r.setRenderProps (props);
   }
   
   /**
    * Creates a set of render properties and initializes for use with
    * the specified host
    * 
    * @param host host object that has render properties
    * @param props the properties to copy and assign
    * @return the created render properties
    */
   public static RenderProps createAndInitRenderProps(HasRenderProps host, 
      RenderProps props) {
      if (props == null) {
         host.setRenderProps(null);
      } else {
         props = props.clone(); // create clone
            
         // propagate values
         if (host instanceof HasProperties) {
            RenderProps.initRenderProps(props, (HasProperties)host);
            PropertyUtils.updateCompositeProperty (
               (HasProperties)host, "renderProps", null, props);
         }
      }
      return props;
   }

}
