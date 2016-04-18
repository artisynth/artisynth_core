/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.renderables;

import java.awt.Font;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import maspack.geometry.Rectangle2d;
import maspack.matrix.AffineTransform3d;
import maspack.matrix.AxisAngle;
import maspack.matrix.Vector2d;
import maspack.matrix.VectorNd;
import maspack.properties.PropertyList;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;
import maspack.render.IsRenderable;
import maspack.render.LineFaceRenderProps;
import maspack.render.RenderObject;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.render.Renderer.ColorMixing;
import maspack.render.color.ColorMap;
import maspack.render.color.ColorMapBase;
import maspack.render.color.HueColorMap;
import maspack.util.DoubleInterval;
import maspack.util.NumberFormat;


/**
 * Color bar implementation, as a 2D renderable component.  Allow
 * you to draw labels/tick marks and adjust the color scheme.
 * @author Antonio
 *
 */
public class ColorBar extends TextComponentBase {

   public static Rectangle2d defaultLoc = new Rectangle2d(40, 0, 20, 0);
   public static Rectangle2d defaultNormLoc = new Rectangle2d(0, 0.1, 0.05, 0.8);
   //   public static CornerRef defaultLocationRef = CornerRef.BOTTOM_RIGHT;
   public static Vector2d defaultTickFraction = new Vector2d(0.2, 0.2);
   public static double defaultTextSize = 12;
   public static Vector2d defaultTextOffset = new Vector2d(5, 5);
   public static String defaultNumberFormat = "%g";
   public static VerticalAlignment defaultVAlignment = VerticalAlignment.CENTRE;
   public static HorizontalAlignment defaultHAlignment = HorizontalAlignment.RIGHT;
   public static DoubleInterval defaultInterval = new DoubleInterval(0,1);

   private VectorNd myLabelPos;
   private ArrayList<String> myLabelText;
   private NumberFormat myNumberFormat;

   public static ColorMapBase defaultColorMap = new HueColorMap();
   public static int defaultBarDivisions = 36;   
   public static boolean defaultHorizontal = false;

   private Rectangle2d myLoc;      // 4d rectangle
   private Rectangle2d myNormLoc;  
   // private CornerRef myLocRef;

   private int nBarDivisions;
   private ColorMapBase myColorMap;
   private PropertyMode myColorMapMode = PropertyMode.Inherited;
   private DoubleInterval myValueRange;
   private boolean horizontal;
   private Vector2d myTickFractions;
   Vector2d myTextOffset;

   RenderObject myRenderObject;

   public static PropertyList myProps = new PropertyList(
      ColorBar.class, TextComponentBase.class);
   static {
      myProps.add("locationOverride", 
         "display location [x y width height], > 0 overrides normalized location", 
         defaultLoc);
      myProps.add("normalizedLocation", "normalized location", defaultNormLoc, "[0,1]");
      // myProps.add("locationReference", "reference origin for coordinates", defaultLocationRef);
      myProps.add("horizontal isHorizontal *", "horizontal or vertical bar", defaultHorizontal);
      myProps.addInheritable("colorMap", "color map", defaultColorMap, "CE");
      myProps.add("valueRange", "range of values for interpolation", defaultInterval);
      myProps.add("tickFraction", 
         "fractional length of tick mark on the label side/opposite side", defaultTickFraction);
      myProps.add("labelPositions", "label positions", new VectorNd(), "[0,1]");
      myProps.add("labels getLabelSet parseLabelSet", "set of labels", "");
      myProps.add("textOffset", "offset for label text", defaultTextOffset);
      myProps.add("numberFormat getNumberFormatString setNumberFormatString", "number format", defaultNumberFormat);
   }
   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public ColorBar() {
      setDefaults();
   }

   public ColorBar(ColorMapBase cmap) {
      this();
      myColorMap = cmap;
   }

   public static RenderProps createDefaultRenderProps() {
      return new LineFaceRenderProps();
   }

   @Override
   public RenderProps createRenderProps() {
      RenderProps rprops = createDefaultRenderProps();
      PropertyUtils.updateInheritedProperties (rprops, this, "renderProps");
      return rprops;
   }

   @Override
   protected void setDefaults() {

      setFont( new Font(defaultFontName, 0, defaultFontSize));
      myRenderProps = createDefaultRenderProps();
      hAlignment = defaultHAlignment;
      vAlignment = defaultVAlignment;
      myTextSize = defaultTextSize;

      myLoc = new Rectangle2d(defaultLoc);
      myNormLoc = new Rectangle2d(defaultNormLoc);
      // myLocRef = defaultLocationRef;

      myColorMap = defaultColorMap;
      myValueRange = new DoubleInterval(defaultInterval);
      nBarDivisions = defaultBarDivisions;

      myTickFractions = new Vector2d(defaultTickFraction);
      myLabelPos = new VectorNd(0);
      myLabelText = new ArrayList<String>();
      myTextOffset = new Vector2d(defaultTextOffset);

      myNumberFormat = new NumberFormat(defaultNumberFormat);
      myRenderObject = null;
   }

   public RenderObject buildRenderObject(int divisions, ColorMap cmap, VectorNd labelPos, Vector2d tickWidths) {
      RenderObject out = new RenderObject();

      out.ensurePositionCapacity (6*divisions+2);
      out.ensureVertexCapacity (6*divisions+2);
      out.ensureColorCapacity (divisions+3);
      out.ensureTriangleCapacity (2*divisions);
      out.ensureLineCapacity (2+2*divisions);

      // for color interpolation
      float[] rgb = new float[3];

      // add positions and colors
      for (int i=0; i<=divisions; ++i) {
         cmap.getRGB (i/divisions, rgb);
         out.addColor (rgb[0], rgb[1], rgb[2], 1f);
         out.addPosition (0, i/divisions, 0);
         out.addPosition (1, i/divisions, 0);
      }

      // line color
      int lineColor = out.addColor (0,0,0,1);

      // tick inner positions
      for (int i=0; i<labelPos.size (); ++i) {
         float y = (float)(labelPos.get (i));
         out.addPosition ((float)(tickWidths.x), y, 0);
         out.addPosition ((float)(tickWidths.y), y, 0);
      }

      // color bar vertices
      int vidx  = 0;
      out.addVertex (0, -1, 0, -1);
      out.addVertex (1, -1, 0, -1);
      for (int i=1; i<=divisions; ++i) {
         vidx = out.addVertex (2*i, -1, i, -1);
         out.addVertex (2*i+1, -1, i, -1);
         out.addTriangle (vidx-2, vidx, vidx-1);
         out.addTriangle (vidx-1, vidx, vidx+1);
      }

      // border vertices
      out.setCurrentColor (lineColor);
      vidx = out.addVertex (0);
      out.addVertex (1);
      out.addVertex (2*divisions);
      out.addVertex (2*divisions+1);
      out.addLine (vidx, vidx+1);
      out.addLine (vidx, vidx+2);
      out.addLine (vidx+2, vidx+3);
      out.addLine (vidx+1, vidx+3);

      // tick vertices
      int tickPos = 2*divisions+2;
      for (int i=0; i<labelPos.size (); ++i) {
         vidx = out.addVertex (2*i);
         out.addVertex (tickPos++);
         out.addLine (vidx, vidx+1);
         vidx = out.addVertex (2*i+1);
         out.addVertex (tickPos++);
         out.addLine (vidx, vidx+1);
      }

      return out;
   }

   @Override
   public void render(Renderer renderer, int flags) {

      if (!isSelectable() && renderer.isSelecting()) {
         return;
      }

      int screenWidth = renderer.getScreenWidth();
      int screenHeight = renderer.getScreenHeight();
      boolean saved2d = renderer.is2DRendering();
      if (!saved2d) {
         renderer.begin2DRendering(screenWidth, screenHeight);
      }

      // smooth shading
      Renderer.Shading savedShadeModel = renderer.getShading();
      renderer.setShading (Renderer.Shading.SMOOTH);

      double x0 = myLoc.x;
      double y0 = myLoc.y;

      // override if !=0, wrap if < 0
      if (x0 == 0) {
         x0 = myNormLoc.x * screenWidth;
      } else if (x0 < 0) {
         x0 = screenWidth + x0;
      }
      if (y0 == 0) {
         y0 = myNormLoc.y * screenHeight;
      } else if (y0 < 0) {
         y0 = screenHeight-y0;
      }

      double bwidth = myLoc.width;
      double bheight = myLoc.height;
      if (bwidth <= 0) {
         bwidth = myNormLoc.width*screenWidth;
      }
      if (bheight <= 0) {
         bheight = myNormLoc.height*screenHeight;
      }

      RenderObject robj = myRenderObject;
      if (robj == null) {
         robj = buildRenderObject (nBarDivisions, myColorMap, myLabelPos, myTickFractions);
         myRenderObject = robj;
      }

      renderer.pushModelMatrix ();

      // transform so that the colorbar occupies correct location
      AffineTransform3d trans = new AffineTransform3d();
      if (horizontal) {
         trans.setRotation (AxisAngle.ROT_Z_90);
      }
      trans.applyScaling (bwidth/screenWidth, bheight/screenHeight, 1);
      trans.setTranslation (x0, y0, 0);

      LineFaceRenderProps props = (LineFaceRenderProps)getRenderProps();
      float savedLineWidth = renderer.getLineWidth();
      renderer.setLineWidth(props.getLineWidth());
      renderer.setLineColoring (props, false);

      renderer.setVertexColorMixing (ColorMixing.NONE);
      renderer.drawLines (robj, 0);
      renderer.setVertexColorMixing (ColorMixing.REPLACE);
      renderer.drawTriangles (robj, 0);

      renderer.popModelMatrix ();

      // return line width
      renderer.setLineWidth(savedLineWidth);

      // labels
      int nLabels = Math.min(myLabelPos.size(), myLabelText.size());
      if (nLabels > 0) {
         float[] rgb = new float[3];
         props.getFaceColor(rgb);
         double tx, ty;

         //         // for consistency, assume line top as 3/4 font size
         //         double t = myTextSize*0.75;
         //         double vc = myTextSize* 0.25;
         //         double b = myTextSize*0.25;

         renderer.setColor(rgb[0], rgb[1], rgb[2], 1);
         float[] loc = new float[3];
         for (int i=0; i<nLabels; i++) {
            tx = 0;
            ty = 0;

            // text orientation computation
            String label = myLabelText.get(i);
            Rectangle2D box = renderer.getTextBounds (myFont, label, myTextSize);
            double w = box.getWidth();
            double h = box.getHeight ();
            double b = box.getY ();
            double vc = b+h/2;
            double t = h + b;
            
            if (horizontal) {
               switch(hAlignment) {
                  case LEFT:
                     tx = x0 + myTextOffset.x;
                     break;
                  case CENTRE:
                     tx = x0 - w/2;
                     break;
                  case RIGHT:
                     tx = x0 - myTextOffset.x - w;
                     break;
               }
               tx += bwidth*myLabelPos.get(i);

               switch(vAlignment) {
                  case BOTTOM:
                     ty = y0-myTextOffset.y-t;
                     break;
                  case CENTRE:
                     ty = y0-vc+bheight/2;
                     break;
                  case TOP:
                     ty = y0 + bheight + myTextOffset.y+b;
                     break;
               }
            } else {
               switch(hAlignment) {
                  case LEFT:
                     tx = x0 - myTextOffset.x-w;
                     break;
                  case CENTRE:
                     tx = x0 -w/2 + bwidth/2;
                     break;
                  case RIGHT:
                     tx = x0 + myTextOffset.x + bwidth;
                     break;
               }

               switch(vAlignment) {
                  case BOTTOM:
                     ty = y0+myTextOffset.y;
                     break;
                  case CENTRE:
                     ty = y0-vc;
                     break;
                  case TOP:
                     ty = y0 - myTextOffset.y - t;
                     break;
               }
               ty += bheight*myLabelPos.get(i);
            }

            loc[0] = (float)tx;
            loc[1] = (float)ty;
            renderer.drawText (myFont, label, loc, myTextSize);           
         }
      }


      renderer.setShading (savedShadeModel);

      if (!saved2d) {
         renderer.end2DRendering();
      }

   }

   @Override
   public int getRenderHints() {
      return super.getRenderHints()
      | IsRenderable.TWO_DIMENSIONAL;
   }

   public Rectangle2d getNormalizedLocation() {
      return myNormLoc;
   }

   public void setNormalizedLocation(Rectangle2d pos) {
      myNormLoc.x = Math.min(Math.max(pos.x, 0), 1);
      myNormLoc.y = Math.min(Math.max(pos.y, 0), 1);
      myNormLoc.width = Math.min(Math.max(pos.width, 0), 1);
      myNormLoc.height = Math.min(Math.max(pos.height, 0), 1);
   }

   public Rectangle2d getLocationOverride() {
      return myLoc;
   }

   public void setLocationOverride(Rectangle2d pos) {
      myLoc.x = pos.x;
      myLoc.y = pos.y;
      myLoc.width = pos.width;
      myLoc.height = pos.height;
   }

   public void setLocationOverride(double x, double y, double width, double height) {
      myLoc.x = x;
      myLoc.y = y;
      myLoc.width = width;
      myLoc.height = height;
   }

   public void setNormalizedLocation(double x, double y, double width, double height) {
      myNormLoc.x = x;
      myNormLoc.y = y;
      myNormLoc.width = width;
      myNormLoc.height = height;
   }

   public boolean isHorizontal() {
      return horizontal;
   }

   public void setHorizontal(boolean set) {
      horizontal = set;
   }

   public ColorMapBase getColorMap() {
      return myColorMap;
   }

   public void setColorMap(ColorMapBase colorMap) {
      myColorMap = colorMap;
      myRenderObject = null;
      myColorMapMode =
      PropertyUtils.propagateValue (
         this, "colorMap", colorMap, myColorMapMode);
   }

   public PropertyMode getColorMapMode() {
      return myColorMapMode;
   }

   public void setColorMapMode(PropertyMode mode) {
      if (mode != myColorMapMode) {
         myColorMapMode = PropertyUtils.setModeAndUpdate (
            this, "colorMap", myColorMapMode, mode);
      }
   }

   //   public void setLocationReference(CornerRef ref) {
   //      myLocRef = ref;
   //   }
   //
   //   public CornerRef getLocationReference() {
   //      return myLocRef;
   //   }

   public VectorNd getLabelPositions() {
      return myLabelPos;
   }

   public synchronized void setLabelPositions(VectorNd pos) {
      myLabelPos.set(pos);
      myRenderObject = null;
   }

   public synchronized void setLabels(ArrayList<String> text) {
      myLabelText.clear();
      myLabelText.addAll(text);
   }

   public ArrayList<String> getLabels() {
      return myLabelText;
   }

   public void setLabel(int idx, String text) {
      myLabelText.set(idx, text);
   }

   public void parseLabelSet(String str) {
      ArrayList<String> labels = new ArrayList<String>(myLabelPos.size());

      if (str == null || str.length() == 0) {
         return;
      }

      char[] carray = str.toCharArray();
      int idx = 0;
      while (idx >= 0) {
         idx = parseNext(carray, idx, labels);
      }
      setLabels(labels);
   }

   public int parseNext(char[] carray, int idx, ArrayList<String> parsed) {

      while (idx < carray.length && (carray[idx] == ' ' || carray[idx] == ',')) {
         idx++;
      }
      if (idx == carray.length) {
         return -1;  // done
      }

      boolean quoted = false;
      if (carray[idx] == '"') {
         quoted = true;
         idx++;
      }

      StringBuilder sb=new StringBuilder();
      if (!quoted) {
         while(idx < carray.length && carray[idx] != ' ' && carray[idx] != ',') {
            sb.append(carray[idx++]);
         }
         if (idx == carray.length) {
            if (sb.length() > 0) {
               parsed.add(sb.toString());
            }
            return -1;  // done
         }
      } else {
         while(idx < carray.length && carray[idx] != '"') {
            sb.append(carray[idx++]);
         }
         if (idx == carray.length) {
            if (sb.length() > 0) {
               parsed.add(sb.toString());
            }
            return -1;  // done
         }
      }
      idx++;

      if (sb.length() > 0) {
         parsed.add(sb.toString());
      }

      if (idx >= carray.length) {
         return -1;
      }
      return idx;
   }

   public String getLabelSet() {
      StringBuilder out = new StringBuilder();

      if (myLabelText.size() == 0) {
         return "";
      }

      out.append("\""+ myLabelText.get(0) + "\"");
      for (int i=1; i<myLabelText.size(); i++) {
         out.append(", \"");
         out.append(myLabelText.get(i)); 
         out.append("\"");
      }
      out.append("");

      return out.toString();
   }

   public void setTickFraction(Vector2d frac) {
      myTickFractions.set(frac);
      myRenderObject = null;
   }

   public Vector2d getTickFraction() {
      return myTickFractions;
   }

   public Vector2d getTextOffset() {
      return myTextOffset;
   }

   public void setTextOffset(Vector2d offset) {
      myTextOffset.set(offset);
   }

   public void setLabels(VectorNd tickLocs, ArrayList<String> labels) {
      setLabelPositions(tickLocs);
      setLabels(labels);
   }

   public void populateLabels(double minVal, double maxVal, int nSections, NumberFormat fmt) {

      setValueRange(minVal, maxVal);
      double dx = 1.0/nSections;
      VectorNd ticks = new VectorNd(nSections+1);
      ArrayList<String> labels = new ArrayList<String>(nSections+1);

      for (int i=0; i<=nSections; i++) {
         double a = i*dx;
         ticks.set(i, a);
         labels.add(fmt.format( (1-a)*minVal + a*maxVal ));
      }

      setLabels(ticks, labels);

   }

   public void updateLabels(NumberFormat fmt) {
      updateLabels(myValueRange.getLowerBound(), myValueRange.getUpperBound(), fmt);
   }

   public void updateLabels() {
      updateLabels(myValueRange.getLowerBound(), myValueRange.getUpperBound(), myNumberFormat);
   }

   public void getColor(double value, double[] rgb) {
      double a = (value-myValueRange.getLowerBound())/myValueRange.getRange();
      a = Math.min(Math.max(0, a),1);
      myColorMap.getRGB(a, rgb);
   }

   public void updateLabels(double minVal, double maxVal, NumberFormat fmt) {

      setValueRange(minVal, maxVal);

      int nSections = myLabelPos.size()-1;
      ArrayList<String> labels = new ArrayList<String>(nSections+1);

      for (int i=0; i<=nSections; i++) {
         double a = myLabelPos.get(i);
         labels.add(fmt.format( (1-a)*minVal + a*maxVal ));
      }

      setLabels(labels);
   }

   public void updateLabels(double minVal, double maxVal) {
      updateLabels(minVal, maxVal, myNumberFormat);
   }

   public void populateLabels(double minVal, double maxVal, int nSections) {
      populateLabels(minVal, maxVal, nSections, myNumberFormat);
   }

   public void setValueRange(DoubleInterval range) {
      myValueRange.set(range);
   }

   public void setValueRange(double min, double max) {
      myValueRange.set(min,max);
   }

   public DoubleInterval getValueRange() {
      return myValueRange;
   } 

   @Override
   /**
    * If bar is horizontal, then sets alignment of text.  If bar is vertical,
    * places text on supplied alignment location.
    */
   public void setHorizontalAlignment(HorizontalAlignment hAlignment) {
      super.setHorizontalAlignment(hAlignment);
   }

   /**
    * If bar is horizontal, then places text at supplied alignment location. 
    * If vertical, sets alignment of text.
    */
   @Override
   public void setVerticalAlignment(VerticalAlignment vAlignment) {
      super.setVerticalAlignment(vAlignment);
   }

   public void setNumberFormatString(String fmt) {
      myNumberFormat.set(fmt);
   }

   public String getNumberFormatString() {
      return myNumberFormat.toString();
   }

   public void setNumberFormat(NumberFormat fmt) {
      myNumberFormat.set(fmt);
   }

   public void setNumberFormat(String fmtStr) {
      myNumberFormat.set(fmtStr);
   }

   public NumberFormat getNumberFormat() {
      return myNumberFormat;
   }   

}
