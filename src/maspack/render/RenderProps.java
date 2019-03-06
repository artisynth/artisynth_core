/**
F * Copyright (c) 2014, by the Authors: John E Lloyd (UBC), C Antonio Sanchez
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

import java.awt.Color;
import java.io.IOException;
import java.io.PrintWriter;

import maspack.render.Renderer.FaceStyle;
import maspack.render.Renderer.Shading;
import maspack.render.Renderer.PointStyle;
import maspack.render.Renderer.LineStyle;
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

   public enum Props {
      Visible,
      Alpha,
      zOrder,
      Shading,
      Shininess,
      Ambience,
      FaceStyle,
      FaceColor,
      BackColor,
      DrawEdges,
      EdgeColor,
      EdgeWidth,
      ColorMapProps,
      NormalMapProps,
      BumpMapProps,
      LineStyle,
      LineColor,
      LineWidth,
      LineRadius,
      LineSlices,
      PointStyle,
      PointColor,
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
   
   protected float[] mySpecular = null;
   protected PropertyMode mySpecularMode;
   protected static Color defaultSpecular = null;
   
   protected PointStyle myPointStyle;
   protected PropertyMode myPointStyleMode;
   protected static PointStyle defaultPointStyle = PointStyle.POINT;

   protected double myPointRadius;
   protected PropertyMode myPointRadiusMode;
   protected static double defaultPointRadius = 1;

//   protected int myPointSlices;
//   protected PropertyMode myPointSlicesMode;
//   protected static int defaultPointSlices = 32;

   protected LineStyle myLineStyle;
   protected PropertyMode myLineStyleMode;
   protected static LineStyle defaultLineStyle = LineStyle.LINE;

   protected double myLineRadius;
   protected PropertyMode myLineRadiusMode;
   protected static double defaultLineRadius = 1;

//   protected int myLineSlices;
//   protected PropertyMode myLineSlicesMode;
//   protected static int defaultLineSlices = 32;

   protected boolean myDrawEdgesP;
   protected PropertyMode myDrawEdgesMode;
   protected static boolean defaultDrawEdgesP = false;

   protected int myEdgeWidth;
   protected PropertyMode myEdgeWidthMode;
   protected static int defaultEdgeWidth = 1;

   protected float[] myEdgeColor = null;
   protected PropertyMode myEdgeColorMode;
   protected static Color defaultEdgeColor = null;

   protected FaceStyle myFaceStyle;
   protected PropertyMode myFaceStyleMode;
   protected static FaceStyle defaultFaceStyle = FaceStyle.FRONT;

   protected float[] myBackColor = null;
   protected PropertyMode myBackColorMode;
   protected static Color defaultBackColor = null;

   protected float[] myLineColor = new float[3];
   protected PropertyMode myLineColorMode;
   protected static Color defaultLineColor = new Color (0.5f, 0.5f, 0.5f);

   protected float[] myPointColor = new float[3];
   protected PropertyMode myPointColorMode;
   protected static Color defaultPointColor = new Color (0.5f, 0.5f, 0.5f);

   protected ColorMapProps myColorMapProps;
   protected static ColorMapProps defaultColorMapProps = null;
   
   protected NormalMapProps myNormalMapProps;
   protected static NormalMapProps defaultNormalMapProps = null;

   protected BumpMapProps myBumpMapProps;
   protected static BumpMapProps defaultBumpMapProps = null;
   
   public static PropertyList myProps = new PropertyList (RenderProps.class);

   protected static PropertyMode INHERITED = PropertyMode.Inherited;
   protected static PropertyMode INACTIVE = PropertyMode.Inactive;
   protected static PropertyMode EXPLICIT = PropertyMode.Explicit;

//   protected boolean myColorMapPropsInactive = true;
//   protected boolean myNormalMapPropsInactive = true;
//   protected boolean myBumpMapPropsInactive = true;

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
         "specular:Inherited", "specular color", defaultSpecular);
      myProps.addInheritable (
         "faceStyle:Inherited", "draw front/back of faces", defaultFaceStyle);
      myProps.addInheritable (
         "faceColor:Inherited", "color for faces", defaultFaceColor);
      myProps.addInheritable (
         "backColor:Inherited", "back face color", defaultBackColor);
      myProps.addInheritable (
         "drawEdges:Inherited", "draw mesh edges", defaultDrawEdgesP);
      myProps.add (
         "colorMap", "color texture map properties", defaultColorMapProps);
      myProps.add (
         "normalMap", "normal texture map properties", defaultNormalMapProps);
      myProps.add (
         "bumpMap", "bump texture map properties", defaultBumpMapProps);
      myProps.addInheritable (
         "edgeColor:Inherited", "edge color (mainly for meshes)", 
         defaultEdgeColor);     
      myProps.addInheritable (
         "edgeWidth:Inherited", "edge width (pixels)", defaultEdgeWidth);     
      
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
//      myProps.addInheritable (
//         "lineSlices:Inherited",
//         "number of slices for lines rendered as cylinders",
//         defaultLineSlices, "[3,Inf]");

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
//      myProps.addInheritable (
//         "pointSlices:Inherited",
//         "number of slices for points rendered as spheres",
//         defaultPointSlices);

   }
   
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


   protected void doSetColor (float[] result, Color color) {
      color.getRGBColorComponents (result);
   }

   private float[] getColor (float[] dst, float[] src) {
      if (dst == null) {
         dst = new float[3];
      }
      dst[0] = src[0];
      dst[1] = src[1];
      dst[2] = src[2];
      return dst;
   }
   
   // property faceColor

   public Color getFaceColor() {
      return new Color (myFaceColor[0], myFaceColor[1], myFaceColor[2]);
   }

   /**
    * Returns the internal float array used to store the face color RGB 
    * values.
    * 
    * @return internal face color array. Should not be modified.
    */
   public float[] getFaceColorF() {
      return myFaceColor;
   }

   public void setFaceColor (float[] rgb) {
      setFaceColor (toColor(rgb));
   }
   
   public void setFaceColor (Color color) {
      doSetColor (myFaceColor, color);
      myFaceColorMode =
         PropertyUtils.propagateValue (
            this, "faceColor", color, myFaceColorMode);
   }

   /**
    * Returns the RGB values for the face color as a float array. 
    * The application may supply the array via the argument <code>rgb</code>. 
    * Otherwise, if <code>rgb</code> is <code>null</code>, an array
    * will be allocated.
    *    
    * @param rgb optional storage for returning the RGB values
    * @return RGB values for the face color
    */
   public float[] getFaceColor (float[] rgb) {
      return getColor (rgb, myFaceColor);
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

   // property alpha

   protected boolean doSetAlpha (float a) {
      if (myAlpha != a) {
         myAlpha = a;
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

   // property specular 
   
   public Color getSpecular() {
      if (mySpecular != null) {
         return new Color (
            mySpecular[0], mySpecular[1], mySpecular[2]);
      }
      else {
         return null;
      }
   }

   /**
    * Returns the internal float array used to store the specular RGB
    * values. This will be <code>null</code> if the specular color is not set.
    * 
    * @return internal specular color array. Should not be modified.
    */
   public float[] getSpecularF() {
      return mySpecular;
   }

   protected void doSetSpecular (Color color) {
      if (color == null) {
         if (mySpecular != null) {
            mySpecular = null;
         }
      }
      else {
         if (mySpecular == null) {
            mySpecular = new float[3];
         }
         doSetColor (mySpecular, color);
      }
   }

   public void setSpecular (float[] rgb) {
      setSpecular (toColor (rgb));
   }

   public void setSpecular (Color color) {
      doSetSpecular (color);
      mySpecularMode =
         PropertyUtils.propagateValue (
            this, "specular", color, mySpecularMode);
   }

   /**
    * Returns the RGB values for the specular color as a float array,
    * or <code>null</code> if no specular color is set. The application may 
    * supply the array via the argument <code>rgb</code>. Otherwise, 
    * if <code>rgb</code> is <code>null</code> and the specular color
    * is not <code>null</code>, an array will be allocated.
    *    
    * @param rgb optional storage for returning the RGB values
    * @return RGB values for the specular color, or <code>null</code>
    * if no specular color is set
    */

   public float[] getSpecular (float[] rgb) {
      if (mySpecular == null) {
         return null;
      }
      return getColor (rgb, mySpecular);
   }

   public PropertyMode getSpecularMode() {
      return mySpecularMode;
   }

   public void setSpecularMode (PropertyMode mode) {
      if (mode != mySpecularMode) {
         mySpecularMode =
            PropertyUtils.setModeAndUpdate (
               this, "specular", mySpecularMode, mode);
      }
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

//   // property pointSlices
//
//   public int getPointSlices() {
//      return myPointSlices;
//   }
//
//   public void setPointSlices (int num) {
//      if (myPointSlices != num) {
//         myPointSlices = num;
//      }
//      myPointSlicesMode =
//         PropertyUtils.propagateValue (
//            this, "pointSlices", num, myPointSlicesMode);
//   }
//
//   public PropertyMode getPointSlicesMode() {
//      return myPointSlicesMode;
//   }
//
//   public void setPointSlicesMode (PropertyMode mode) {
//      myPointSlicesMode =
//         PropertyUtils.setModeAndUpdate (
//            this, "pointSlices", myPointSlicesMode, mode);
//   }

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

//   // property lineSlices
//
//   public int getLineSlices() {
//      return myLineSlices;
//   }
//
//   public void setLineSlices (int num) {
//      if (myLineSlices != num) {
//         myLineSlices = num;
//      }
//      myLineSlicesMode =
//         PropertyUtils.propagateValue (
//            this, "lineSlices", num, myLineSlicesMode);
//   }
//
//   public PropertyMode getLineSlicesMode() {
//      return myLineSlicesMode;
//   }
//
//   public void setLineSlicesMode (PropertyMode mode) {
//      myLineSlicesMode =
//         PropertyUtils.setModeAndUpdate (
//            this, "lineSlices", myLineSlicesMode, mode);
//   }

   // property drawEdges

   public boolean getDrawEdges() {
      return myDrawEdgesP;
   }

   public void setDrawEdges (boolean enable) {
      if (myDrawEdgesP != enable) {
         myDrawEdgesP = enable;
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

   public float[] getEdgeOrLineColorF() {
      return (myEdgeColor != null ? myEdgeColor : myLineColor);
   }         

   /**
    * Returns the internal float array used to store the edge color RGB 
    * values. This will be <code>null</code> if the edge color is not set.
    * 
    * @return internal edge color array. Should not be modified.
    */
   public float[] getEdgeColorF() {
      return myEdgeColor;
   }

   protected void doSetEdgeColor (Color color) {
      if (color == null) {
         if (myEdgeColor != null) {
            myEdgeColor = null;
         }
      }
      else {
         if (myEdgeColor == null) {
            myEdgeColor = new float[3];
         }
         doSetColor (myEdgeColor, color);
      }
   }

   public void setEdgeColor (float[] rgb) {
      setEdgeColor (toColor(rgb));
   }

   public void setEdgeColor (Color color) {
      doSetEdgeColor (color);
      myEdgeColorMode =
         PropertyUtils.propagateValue (
            this, "edgeColor", color, myEdgeColorMode);
   }

   /**
    * Returns the RGB values for the edge color as a float array,
    * or <code>null</code> if no edge color is set. The application may 
    * supply the array via the argument <code>rgb</code>. Otherwise, 
    * if <code>rgb</code> is <code>null</code> and the edge color
    * is not <code>null</code>, an array will be allocated.
    *    
    * @param rgb optional storage for returning the RGB values
    * @return RGB values for the edge color, or <code>null</code>
    * if no edge color is set
    */
   public float[] getEdgeColor (float[] rgb) {
      if (myEdgeColor == null) {
         return null;
      }
      return getColor (rgb, myEdgeColor);
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

   public FaceStyle getFaceStyle() {
      return myFaceStyle;
   }

   public void setFaceStyle (FaceStyle mode) {
      if (myFaceStyle != mode) {
         myFaceStyle = mode;
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
   
   // private float[] toFloat (Color color) {
   //    return color != null ? color.getRGBColorComponents(null) : null;
   // }

   private Color toColor (float[] rgb) {
      return rgb != null ? new Color(rgb[0], rgb[1], rgb[2]) : null;
   }

   protected void doSetBackColor (Color color) {
      if (color == null) {
         if (myBackColor != null) {
            myBackColor = null;
         }
      }
      else {
         if (myBackColor == null) {
            myBackColor = new float[3];
         }
         doSetColor (myBackColor, color);
      }
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

   /**
    * Returns the internal float array used to store the back color RGB 
    * values. This will be <code>null</code> if the back color is not set.
    * 
    * @return internal back color array. Should not be modified.
    */
   public float[] getBackColorF() {
      return myBackColor;
   }

   public void setBackColor (float[] rgb) {
      setBackColor (toColor(rgb));
   }

   public void setBackColor (Color color) {
      doSetBackColor (color);
      myBackColorMode =
         PropertyUtils.propagateValue (
            this, "backColor", color, myBackColorMode);
   }

   /**
    * Returns the RGB values for the back color as a float array,
    * or <code>null</code> if no back color is set. The application may 
    * supply the array via the argument <code>rgb</code>. Otherwise, 
    * if <code>rgb</code> is <code>null</code> and the back color
    * is not <code>null</code>, an array will be allocated.
    *    
    * @param rgb optional storage for returning the RGB values
    * @return RGB values for the back color, or <code>null</code>
    * if no back color is set
    */
   public float[] getBackColor (float[] rgb) {
      if (myBackColor == null) {
         return null;
      }
      return getColor (rgb, myBackColor);
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

   // property lineColor

   public Color getLineColor() {
      return new Color (myLineColor[0], myLineColor[1], myLineColor[2]);
   }

   /**
    * Returns the internal float array used to store the line color RGB 
    * values.
    * 
    * @return internal line color array. Should not be modified.
    */
   public float[] getLineColorF() {
      return myLineColor;
   }

   public void setLineColor (float[] rgb) {
      setLineColor (toColor(rgb));
   }

   public void setLineColor (Color color) {
      doSetColor (myLineColor, color);
      myLineColorMode =
         PropertyUtils.propagateValue (
            this, "lineColor", color, myLineColorMode);
   }

   /**
    * Returns the RGB values for the line color as a float array. 
    * The application may supply the array via the argument <code>rgb</code>. 
    * Otherwise, if <code>rgb</code> is <code>null</code>, an array
    * will be allocated.
    *    
    * @param rgb optional storage for returning the RGB values
    * @return RGB values for the line color
    */
   public float[] getLineColor (float[] rgb) {
      return getColor (rgb, myLineColor);
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

   // property pointColor

   public Color getPointColor() {
      return new Color (myPointColor[0], myPointColor[1], myPointColor[2]);
   }

   /**
    * Returns the internal float array used to store the point color RGB 
    * values.
    * 
    * @return internal point color array. Should not be modified.
    */
   public float[] getPointColorF() {
      return myPointColor;
   }

   public void setPointColor (float[] rgb) {
      setPointColor (toColor(rgb));
   }

   /**
    * Returns the RGB values for the face color as a float array. 
    * The application may supply the array via the argument <code>rgb</code>. 
    * Otherwise, if <code>rgb</code> is <code>null</code>, an array
    * will be allocated.
    *    
    * @param rgb optional storage for returning the RGB values
    * @return RGB values for the face color
    */
   public float[] getPointColor (float[] rgb) {
      return getColor (rgb, myPointColor);
   }

   public void setPointColor (Color color) {
      doSetColor (myPointColor, color);
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

   public ColorMapProps getColorMap() {
      return myColorMapProps;
   }

   public void setColorMap (ColorMapProps props) {
      if (getAllPropertyInfo().get ("colorMap") == null) {
         return;
      }
      if (props == null) {
         PropertyUtils.updateCompositeProperty (
            this, "colorMap", myColorMapProps, null);
         myColorMapProps = null;
      }
      else {
         if (myColorMapProps == null) {
            myColorMapProps = new ColorMapProps();
            myColorMapProps.set (props);
            PropertyUtils.updateCompositeProperty (
               this, "colorMap", null, myColorMapProps);
         }
         else {
            myColorMapProps.set (props);
            PropertyUtils.updateCompositeProperty (myColorMapProps);
         }
      }
   }
   
   public NormalMapProps getNormalMap() {
      return myNormalMapProps;
   }

   public void setNormalMap (NormalMapProps props) {
      if (getAllPropertyInfo().get ("normalMap") == null) {
         return;
      }
      if (props == null) {
         PropertyUtils.updateCompositeProperty (
            this, "normalMap", myNormalMapProps, null);
         myNormalMapProps = null;
      }
      else {
         if (myNormalMapProps == null) {
            myNormalMapProps = new NormalMapProps();
            myNormalMapProps.set (props);
            PropertyUtils.updateCompositeProperty (
               this, "normalMap", null, myNormalMapProps);
         }
         else {
            myNormalMapProps.set (props);
            PropertyUtils.updateCompositeProperty (myNormalMapProps);
         }
      }
   }
   
   public BumpMapProps getBumpMap() {
      return myBumpMapProps;
   }

   public void setBumpMap (BumpMapProps props) {
      if (getAllPropertyInfo().get ("bumpMap") == null) {
         return;
      }
      if (props == null) {
         PropertyUtils.updateCompositeProperty (
            this, "bumpMap", myBumpMapProps, null);
         myBumpMapProps = null;
      }
      else {
         if (myBumpMapProps == null) {
            myBumpMapProps = new BumpMapProps();
            myBumpMapProps.set (props);
            PropertyUtils.updateCompositeProperty (
               this, "bumpMap", null, myBumpMapProps);
         }
         else {
            myBumpMapProps.set (props);
            PropertyUtils.updateCompositeProperty (myBumpMapProps);
         }
      }
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
      //myPointSlicesMode = INHERITED;
      myLineStyleMode = INHERITED;
      myLineRadiusMode = INHERITED;
      //myLineSlicesMode = INHERITED;
      myDrawEdgesMode = INHERITED;
      myEdgeWidthMode = INHERITED;
      myEdgeColorMode = INHERITED;
      myFaceStyleMode = INHERITED;
      myLineColorMode = INHERITED;
      myPointColorMode = INHERITED;
      myBackColorMode = INHERITED;
      mySpecularMode = INHERITED;
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
      doSetSpecular (defaultSpecular);
      myPointStyle = defaultPointStyle;
      myPointRadius = defaultPointRadius;
      //myPointSlices = defaultPointSlices;
      myLineStyle = defaultLineStyle;
      myLineRadius = defaultLineRadius;
      //myLineSlices = defaultLineSlices;
      myDrawEdgesP = defaultDrawEdgesP;
      myEdgeWidth = defaultEdgeWidth;
      doSetEdgeColor (defaultEdgeColor);
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
      myColorMapProps = defaultColorMapProps;
      myNormalMapProps = defaultNormalMapProps;
      myBumpMapProps = defaultBumpMapProps;
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
      if (r.mySpecular != null) {
         mySpecular = getColor (mySpecular, r.mySpecular);
      }
      else {
         mySpecular = null;
      }
      mySpecularMode = r.mySpecularMode;

      myFaceStyle = r.myFaceStyle;
      myFaceStyleMode = r.myFaceStyleMode;
      getColor (myFaceColor, r.myFaceColor);
      myFaceColorMode = r.myFaceColorMode;
      if (r.myBackColor != null) {
         myBackColor = getColor (myBackColor, r.myBackColor);
      }
      else {
         myBackColor = null;
      }
      myBackColorMode = r.myBackColorMode;
      myDrawEdgesP = r.myDrawEdgesP;
      myDrawEdgesMode = r.myDrawEdgesMode;
      myEdgeWidth = r.myEdgeWidth;
      myEdgeWidthMode = r.myEdgeWidthMode;
      if (r.myEdgeColor != null) {
         myEdgeColor = getColor (myEdgeColor, r.myEdgeColor);
      }
      else {
         myEdgeColor = null;
      }
      myEdgeColorMode = r.myEdgeColorMode;
      
      //myColorMapPropsInactive = r.myColorMapPropsInactive;
      setColorMap (r.myColorMapProps);
      //myNormalMapPropsInactive = r.myNormalMapPropsInactive;
      setNormalMap (r.myNormalMapProps);
      //myBumpMapPropsInactive = r.myBumpMapPropsInactive;
      setBumpMap (r.myBumpMapProps);
      
      myLineStyle = r.myLineStyle;
      myLineStyleMode = r.myLineStyleMode;
      getColor (myLineColor, r.myLineColor);
      myLineColorMode = r.myLineColorMode;
      myLineWidth = r.myLineWidth;
      myLineWidthMode = r.myLineWidthMode;
      myLineRadius = r.myLineRadius;
      myLineRadiusMode = r.myLineRadiusMode;
//      myLineSlices = r.myLineSlices;
//      myLineSlicesMode = r.myLineSlicesMode;

      myPointStyle = r.myPointStyle;
      myPointStyleMode = r.myPointStyleMode;
      getColor (myPointColor, r.myPointColor);
      myPointColorMode = r.myPointColorMode;
      myPointSize = r.myPointSize;
      myPointSizeMode = r.myPointSizeMode;
      myPointRadius = r.myPointRadius;
      myPointRadiusMode = r.myPointRadiusMode;
//      myPointSlices = r.myPointSlices;
//      myPointSlicesMode = r.myPointSlicesMode;
   }

   protected boolean equalsOrBothNull(Object a, Object b) {
      if (a == b) {
         return true;
      } else if (a == null || b == null) {
         return false;
      }
      return a.equals (b);
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
      if (mySpecularMode != r.mySpecularMode) {
         return false;
      }
      else if (mySpecularMode == EXPLICIT) {
         if ((mySpecular == null) != (r.mySpecular == null)) {
            return false;
         }
         else if (mySpecular != null &&
                  !ArraySupport.equals (mySpecular, r.mySpecular)) {
            return false;
         }
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
//      if (myPointSlicesMode != r.myPointSlicesMode) {
//         return false;
//      }
//      else if (myPointSlicesMode == EXPLICIT &&
//         myPointSlices != r.myPointSlices) {
//         return false;
//      }
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
//      if (myLineSlicesMode != r.myLineSlicesMode) {
//         return false;
//      }
//      else if (myLineSlicesMode == EXPLICIT &&
//         myLineSlices != r.myLineSlices) {
//         return false;
//      }
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
      }
      if (myPointColorMode != r.myPointColorMode) {
         return false;
      }
      else if (myPointColorMode == EXPLICIT) {
         if (!ArraySupport.equals (myPointColor, r.myPointColor)) {
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
      }
//      if (myColorMapPropsInactive != r.myColorMapPropsInactive) {
//         return false;
//      }
//      else if (!myColorMapPropsInactive && !equalsOrBothNull (r.myColorMapProps, myColorMapProps)) {\
      if (!equalsOrBothNull (r.myColorMapProps, myColorMapProps)) {
         return false;
      }
//      if (myNormalMapPropsInactive != r.myNormalMapPropsInactive) {
//         return false;
//      }
//      else if (!myNormalMapPropsInactive && !equalsOrBothNull(myNormalMapProps, r.myNormalMapProps)) {
      if (!equalsOrBothNull(myNormalMapProps, r.myNormalMapProps)) {
         return false;
      }
//      if (myBumpMapPropsInactive != r.myBumpMapPropsInactive) {
//         return false;
//      }
//      else if (!myBumpMapPropsInactive && !equalsOrBothNull(myBumpMapProps, r.myBumpMapProps)) {
      if (!equalsOrBothNull(myBumpMapProps, r.myBumpMapProps)) {
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
      buf.append ("Specular=" + colorString (mySpecular) + " " +
         mySpecularMode + ", ");
      buf.append ("FaceStyle=" + myFaceStyle + " " + myFaceStyleMode + ", ");
      buf.append ("FaceColor=" + colorString (myFaceColor) + " " +
         myFaceColorMode + ", ");
      buf.append ("BackColor=" + colorString (myBackColor) + " " +
      myBackColorMode + ", ");
      buf.append ("DrawEdges=" + myDrawEdgesP + " " + myDrawEdgesMode + ", ");
      buf.append ("EdgeWidth=" + myEdgeWidth + " " + myEdgeWidthMode + ", ");
      buf.append ("EdgeColor=" + colorString (myEdgeColor) + " " +
         myEdgeColorMode + ", ");
      buf.append ("ColorMapProps=" + myColorMapProps + ", ");
      buf.append ("NormalMapProps=" + myNormalMapProps + ", ");
      buf.append ("BumpMapProps=" + myBumpMapProps + ", ");
      buf.append ("LineStyle=" + myLineStyle + " " + myLineStyleMode + ", ");
      buf.append ("LineColor=" + colorString (myLineColor) + " " +
         myLineColorMode + ", ");
      buf.append ("LineWidth=" + myLineWidth + " " + myLineWidthMode + ", ");
      buf.append ("LineRadius=" + myLineRadius + " " +
         myLineRadiusMode + ", ");
//      buf.append ("LineSlices=" + myLineSlices + " " +
//         myLineSlicesMode + ", ");
      buf.append ("PointStyle=" + myPointStyle + " " + myPointStyleMode + ", ");
      buf.append ("PointColor=" + colorString (myPointColor) + " " +
         myPointColorMode + ", ");
      buf.append ("PointSize=" + myPointSize + " " + myPointSizeMode + ", ");
      buf.append ("PointRadius=" + myPointRadius + " " + myPointRadiusMode +
         ", ");
//      buf.append ("PointSlices=" + myPointSlices + " " + myPointSlicesMode);
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
      if (props.mySpecular != null) {
         props.mySpecular = new float[3];
      }
      if (props.myBackColor != null) {
         props.myBackColor = new float[3];
      }
      if (props.myEdgeColor != null) {
         props.myEdgeColor = new float[3];
      }
      
      props.myFaceColor = new float[3];
      props.myLineColor = new float[3];
      props.myPointColor = new float[3];
      
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

   private static ColorMapProps createAndAssignColorMapProps (RenderProps props) {
      ColorMapProps tprops = props.getColorMap();
      if (tprops == null) {
         tprops = new ColorMapProps();
         props.setColorMap (tprops);
      }
      return tprops;
   }
   
   private static NormalMapProps createAndAssignNormalMapProps (RenderProps props) {
      NormalMapProps tprops = props.getNormalMap();
      if (tprops == null) {
         tprops = new NormalMapProps();
         props.setNormalMap (tprops);
      }
      return tprops;
   }

   private static BumpMapProps createAndAssignBumpMapProps (RenderProps props) {
      BumpMapProps tprops = props.getBumpMap();
      if (tprops == null) {
         tprops = new BumpMapProps();
         props.setBumpMap (tprops);
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

   public static void setSpecular (Renderable r, Color color) {
      RenderProps props = createAndAssignProps (r);
      props.setSpecular (color);
      r.setRenderProps (props);
   }

   public static void setSpecular (Renderable r, float[] rgb) {
      RenderProps props = createAndAssignProps (r);
      props.setSpecular (rgb);
      r.setRenderProps (props);
   }

   public static void setSpecularMode (Renderable r, PropertyMode mode) {
      RenderProps props = createAndAssignProps (r);
      props.setSpecularMode (mode);
      r.setRenderProps (props);
   }

   public static void setFaceStyle (Renderable r, FaceStyle style) {
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

   public static void setFaceColor (Renderable r, float[] rgb) {
      RenderProps props = createAndAssignProps (r);
      props.setFaceColor (rgb);
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

   public static void setBackColor (Renderable r, float[] rgb) {
      RenderProps props = createAndAssignProps (r);
      props.setBackColor (rgb);
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

   public static void setEdgeColor (Renderable r, float[] rgb) {
      RenderProps props = createAndAssignProps (r);
      props.setEdgeColor (rgb);
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

   public static void setLineColor (Renderable r, float[] rgb) {
      RenderProps props = createAndAssignProps (r);
      props.setLineColor (rgb);
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

//   public static void setLineSlices (Renderable r, int nslices) {
//      RenderProps props = createAndAssignProps (r);
//      props.setLineSlices (nslices);
//      r.setRenderProps (props);
//   }
//
//   public static void setLineSlicesMode (Renderable r, PropertyMode mode) {
//      RenderProps props = createAndAssignProps (r);
//      props.setLineSlicesMode (mode);
//      r.setRenderProps (props);
//   }
//
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

   public static void setPointColor (Renderable r, float[] rgb) {
      RenderProps props = createAndAssignProps (r);
      props.setPointColor (rgb);
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

//   public static void setPointSlices (Renderable r, int nslices) {
//      RenderProps props = createAndAssignProps (r);
//      props.setPointSlices (nslices);
//      r.setRenderProps (props);
//   }
//
//   public static void setPointSlicesMode (Renderable r, PropertyMode mode) {
//      RenderProps props = createAndAssignProps (r);
//      props.setPointSlicesMode (mode);
//      r.setRenderProps (props);
//   }
//
   public static void setCubePoints (Renderable r, double width, Color color) {
      RenderProps props = createAndAssignProps (r);
      props.setPointStyle (PointStyle.CUBE);
      props.setPointRadius (width/2);
      if (color != null) {
         props.setPointColor (color);
      }
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

   public static void setSpindleLines (
      Renderable r, double rad, Color color) {

      RenderProps props = createAndAssignProps (r);
      props.setLineStyle (LineStyle.SPINDLE);
      props.setLineRadius (rad);
      if (color != null) {
         props.setLineColor (color);
      }
      r.setRenderProps (props);
   }

   /**
    * Sets the render properties for a renderable to be consistent with either
    * wireframe or solid face rendering. For wireframe rendering, the
    * <code>faces</code>, <code>drawEdges</code> and <code>shading</code>
    * properties are set to <code>Faces.NONE</code>, <code>true</code>, and
    * <code>Shading.NONE</code>, respectively. For solid frame rendering, they
    * are set to <code>Faces.FRONT</code>, <code>false</code>, and
    * <code>Shading.FLAT</code>.
    *
    * @param r Renderable whose properties are to be set
    * @param enable if <code>true</code>, enable wireframe rendering.
    */
   public static void setWireFrame (Renderable r, boolean enable) {
      Shading shading = enable ?  Shading.NONE : Shading.FLAT;
      setWireFrame (r, enable, shading);
   }

   /**
    * Sets the render properties for a renderable to be consistent with either
    * wireframe or solid face rendering. For wireframe rendering, the
    * <code>faces</code> and <code>drawEdges</code> properties are set to
    * <code>Faces.NONE</code> and <code>true</code>, respectively. For solid
    * frame rendering, they are set to <code>Faces.FRONT</code> and
    * <code>false</code>.  The <code>shading</code> property is set to the
    * value of the <code>shading</code> argument.
    *
    * @param r Renderable whose properties are to be set
    * @param enable if <code>true</code>, enable wireframe rendering.
    * @param shading new value for the <code>shading</code> property
    */
   public static void setWireFrame (
      Renderable r, boolean enable, Shading shading) {

      RenderProps props = createAndAssignProps (r);
      if (enable) {
         props.setFaceStyle (FaceStyle.NONE);
         props.setDrawEdges (true);
      }
      else {
         props.setFaceStyle (FaceStyle.FRONT);
         props.setDrawEdges (false);
      }
      props.setShading (shading);
      r.setRenderProps (props);      
   }      

   /**
    * Enable or disable all texture-based properties, such as color-map,
    * bump-map, and normal-map
    */
   public static void setTextureEnabled(Renderable r, boolean enabled) {
      setColorMapEnabled (r, enabled);
      setBumpMapEnabled (r, enabled);
      setNormalMapEnabled (r, enabled);
   }
   
   public static void setColorMap (Renderable r, ColorMapProps tprops) {
      RenderProps props = createAndAssignProps (r);
      props.setColorMap (tprops);
      r.setRenderProps (props);
   }

   public static void setColorMapEnabled (Renderable r, boolean enabled) {
      RenderProps props = createAndAssignProps (r);
      ColorMapProps tprops = createAndAssignColorMapProps(props);
      tprops.setEnabled (enabled);
      r.setRenderProps (props);
   }

   public static void setColorMapEnabledMode (Renderable r, PropertyMode mode) {
      RenderProps props = createAndAssignProps (r);
      ColorMapProps tprops = createAndAssignColorMapProps (props);
      tprops.setEnabledMode (mode);
      r.setRenderProps (props);
   }

   public static void setColorMapFileName (Renderable r, String fileName) {
      RenderProps props = createAndAssignProps (r);
      ColorMapProps tprops = createAndAssignColorMapProps (props);
      tprops.setFileName (fileName);
      r.setRenderProps (props);
   }

   public static void setColorMapFileNameMode (Renderable r, PropertyMode mode) {
      RenderProps props = createAndAssignProps (r);
      ColorMapProps tprops = createAndAssignColorMapProps (props);
      tprops.setFileNameMode (mode);
      r.setRenderProps (props);
   }
   
   public static void setNormalMap (Renderable r, NormalMapProps nprops) {
      RenderProps props = createAndAssignProps (r);
      props.setNormalMap (nprops);
      r.setRenderProps (props);
   }

   public static void setNormalMapEnabled (Renderable r, boolean enabled) {
      RenderProps props = createAndAssignProps (r);
      NormalMapProps tprops = createAndAssignNormalMapProps(props);
      tprops.setEnabled (enabled);
      r.setRenderProps (props);
   }

   public static void setNormalMapEnabledMode (Renderable r, PropertyMode mode) {
      RenderProps props = createAndAssignProps (r);
      NormalMapProps tprops = createAndAssignNormalMapProps (props);
      tprops.setEnabledMode (mode);
      r.setRenderProps (props);
   }

   public static void setNormalMapFileName (Renderable r, String fileName) {
      RenderProps props = createAndAssignProps (r);
      NormalMapProps tprops = createAndAssignNormalMapProps (props);
      tprops.setFileName (fileName);
      r.setRenderProps (props);
   }

   public static void setNormalMapFileNameMode (Renderable r, PropertyMode mode) {
      RenderProps props = createAndAssignProps (r);
      NormalMapProps tprops = createAndAssignNormalMapProps (props);
      tprops.setFileNameMode (mode);
      r.setRenderProps (props);
   }
   
   public static void setBumpMap (Renderable r, BumpMapProps bprops) {
      RenderProps props = createAndAssignProps (r);
      props.setBumpMap (bprops);
      r.setRenderProps (props);
   }

   public static void setBumpMapEnabled (Renderable r, boolean enabled) {
      RenderProps props = createAndAssignProps (r);
      BumpMapProps tprops = createAndAssignBumpMapProps(props);
      tprops.setEnabled (enabled);
      r.setRenderProps (props);
   }

   public static void setBumpMapEnabledMode (Renderable r, PropertyMode mode) {
      RenderProps props = createAndAssignProps (r);
      BumpMapProps tprops = createAndAssignBumpMapProps (props);
      tprops.setEnabledMode (mode);
      r.setRenderProps (props);
   }

   public static void setBumpMapFileName (Renderable r, String fileName) {
      RenderProps props = createAndAssignProps (r);
      BumpMapProps tprops = createAndAssignBumpMapProps (props);
      tprops.setFileName (fileName);
      r.setRenderProps (props);
   }

   public static void setBumpMapFileNameMode (Renderable r, PropertyMode mode) {
      RenderProps props = createAndAssignProps (r);
      BumpMapProps tprops = createAndAssignBumpMapProps (props);
      tprops.setFileNameMode (mode);
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
