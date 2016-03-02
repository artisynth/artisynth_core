/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.renderables;

import java.awt.Font;
import java.awt.geom.Rectangle2D;
import java.awt.*;
import java.util.ArrayList;

import javax.media.opengl.GL2;

import maspack.geometry.Rectangle;
import maspack.matrix.Vector2d;
import maspack.matrix.VectorNd;
import maspack.properties.PropertyList;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;
import maspack.render.Renderer;
import maspack.render.LineFaceRenderProps;
import maspack.render.RenderProps;
import maspack.render.GL.GLRenderable;
import maspack.render.GL.GL2.GL2Viewer;
import maspack.render.color.ColorMapBase;
import maspack.render.color.HueColorMap;
import maspack.util.DoubleInterval;
import maspack.util.NumberFormat;

import com.jogamp.opengl.util.awt.TextRenderer;


/**
 * Color bar implementation, as a 2D renderable component.  Allow
 * you to draw labels/tick marks and adjust the color scheme.
 * @author Antonio
 *
 */
public class ColorBar extends TextComponentBase {

   //   public enum CornerRef {
   //      TOP_LEFT,
   //      TOP_RIGHT,
   //      BOTTOM_LEFT,
   //      BOTTOM_RIGHT
   //   }
   
   public static Rectangle defaultLoc = new Rectangle(40, 0, 20, 0);
   public static Rectangle defaultNormLoc = new Rectangle(0, 0.1, 0.05, 0.8);
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

   private Rectangle myLoc;      // 4d rectangle
   private Rectangle myNormLoc;  
   // private CornerRef myLocRef;
   
   private int nBarDivisions;
   private ColorMapBase myColorMap;
   private PropertyMode myColorMapMode = PropertyMode.Inherited;
   private DoubleInterval myValueRange;
   private boolean horizontal;
   private Vector2d myTickFractions;
   Vector2d myTextOffset;
   
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
      setDefaults();
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

      myFont = new Font(defaultFontName, 0, defaultFontSize);
      myTextRenderer = new TextRenderer(myFont);
      myRenderProps = createDefaultRenderProps();
      hAlignment = defaultHAlignment;
      vAlignment = defaultVAlignment;
      myTextSize = defaultTextSize;

      myLoc = new Rectangle(defaultLoc);
      myNormLoc = new Rectangle(defaultNormLoc);
      // myLocRef = defaultLocationRef;
      
      myColorMap = defaultColorMap;
      myValueRange = new DoubleInterval(defaultInterval);
      nBarDivisions = defaultBarDivisions;
      
      myTickFractions = new Vector2d(defaultTickFraction);
      myLabelPos = new VectorNd(0);
      myLabelText = new ArrayList<String>();
      myTextOffset = new Vector2d(defaultTextOffset);
      
      myNumberFormat = new NumberFormat(defaultNumberFormat);
   }

//   @Override
//   public synchronized void renderx(GLRenderer renderer, int flags) {
//      if (isSelectable() || !renderer.isSelecting()) {
//         render(renderer, 0);
//      }
//   }
//
   
   @Override
   public void render(Renderer renderer, int flags) {

      if (!isSelectable() && renderer.isSelecting()) {
         return;
      }
      
      if (!(renderer instanceof GL2Viewer)) {
         return;
      }
      GL2Viewer viewer = (GL2Viewer)renderer;
      GL2 gl = viewer.getGL2();
      
      int screenWidth = renderer.getScreenWidth();
      int screenHeight = renderer.getScreenHeight();
      boolean saved2d = renderer.is2DRendering();
      if (!saved2d) {
         renderer.begin2DRendering(screenWidth, screenHeight);
      }

      // smooth shading
      Renderer.Shading savedShadeModel = renderer.getShading();
      renderer.setShading (Renderer.Shading.GOURAUD);

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

      float[] rgb = new float[3];

      double du;
      if (horizontal) {
         du = bwidth/nBarDivisions;
      } else {
         du = bheight/nBarDivisions;
      }

      // draw bar
      gl.glBegin(GL2.GL_QUAD_STRIP);
      for (int i=0; i<=nBarDivisions; i++) {

         myColorMap.getRGB((double)i/nBarDivisions, rgb);
         renderer.setColor (rgb);
         if (horizontal) {
            gl.glVertex2d(x0+i*du, y0);
            gl.glVertex2d(x0+i*du, y0+bheight);   
         } else {
            gl.glVertex2d(x0+bwidth, y0+i*du);
            gl.glVertex2d(x0, y0+i*du);
         }
      }
      gl.glEnd();

      LineFaceRenderProps props = (LineFaceRenderProps)getRenderProps();
      // draw border and ticks
      if (props.getLineWidth() > 0) {
         
         float savedLineWidth = renderer.getLineWidth();
         renderer.setLineWidth(props.getLineWidth());
         
         props.getLineColor(rgb);
         
         renderer.setColor (rgb);
         
         // border
         gl.glBegin(GL2.GL_LINE_STRIP);
         gl.glVertex2d(x0, y0);
         gl.glVertex2d(x0+bwidth, y0);
         gl.glVertex2d(x0+bwidth, y0+bheight);
         gl.glVertex2d(x0, y0+bheight);
         gl.glVertex2d(x0, y0);
         gl.glEnd();
         
         // ticks
         double t1 = 0;
         double t2 = 0;
         double tx,ty;
         if (horizontal) {
            if (vAlignment == VerticalAlignment.TOP) {
               t2 = myTickFractions.x*bheight;
               t1 = myTickFractions.y*bheight;
            } else {
               t1 = myTickFractions.x*bheight;
               t2 = myTickFractions.y*bheight;
            }
            
            gl.glBegin(GL2.GL_LINES);
            for (int i=0; i<myLabelPos.size(); i++) {
               // bottom/top
               tx = x0 + myLabelPos.get(i)*bwidth;
               ty = y0;
               gl.glVertex2d(tx, ty);
               gl.glVertex2d(tx, ty+t1);
               gl.glVertex2d(tx, ty+bheight);
               gl.glVertex2d(tx, ty+bheight-t2);
            }
            gl.glEnd();
            
         } else {
            if (hAlignment == HorizontalAlignment.LEFT) {
               t1 = myTickFractions.x*bwidth;
               t2 = myTickFractions.y*bwidth;
            } else {
               t2 = myTickFractions.x*bwidth;
               t1 = myTickFractions.y*bwidth;
            }
            
            gl.glBegin(GL2.GL_LINES);
            for (int i=0; i<myLabelPos.size(); i++) {
               // bottom/top
               tx = x0;
               ty = y0 + myLabelPos.get(i)*bheight;
               gl.glVertex2d(tx, ty);
               gl.glVertex2d(tx+t1, ty);
               gl.glVertex2d(tx+bwidth, ty);
               gl.glVertex2d(tx+bwidth-t2, ty);
            }
            gl.glEnd();
         }
         
         // return line width
         renderer.setLineWidth(savedLineWidth);
      }
      
      // labels
      int nLabels = Math.min(myLabelPos.size(), myLabelText.size());
      if (nLabels > 0) {
         
         props.getFaceColor(rgb);
         double tx, ty;
         float fTextSize = (float)(myTextSize/getFontSize());
         // for consistency, assume line top as 3/4 font size
         double t = myTextSize*0.75;
         double vc = myTextSize* 0.25;
         double b = myTextSize*0.25;
         
         myTextRenderer.setColor(rgb[0], rgb[1], rgb[2], 1);
         myTextRenderer.begin3DRendering();
         for (int i=0; i<nLabels; i++) {
            tx = 0;
            ty = 0;
            
            // text orientation computation
            String label = myLabelText.get(i);
            Rectangle2D box = myTextRenderer.getBounds(label);
            double w = box.getWidth() * fTextSize;
            
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
          
            myTextRenderer.draw3D(label, (float)tx, (float)ty, 0, fTextSize);           
         }
         myTextRenderer.end3DRendering();
         
         
      }
      
      
      renderer.setShading (savedShadeModel);

      if (!saved2d) {
         renderer.end2DRendering();
      }

   }

   @Override
   public int getRenderHints() {
      return super.getRenderHints()
         | GLRenderable.TWO_DIMENSIONAL;
   }

   public Rectangle getNormalizedLocation() {
      return myNormLoc;
   }

   public void setNormalizedLocation(Rectangle pos) {
      myNormLoc.x = Math.min(Math.max(pos.x, 0), 1);
      myNormLoc.y = Math.min(Math.max(pos.y, 0), 1);
      myNormLoc.width = Math.min(Math.max(pos.width, 0), 1);
      myNormLoc.height = Math.min(Math.max(pos.height, 0), 1);
   }

   public Rectangle getLocationOverride() {
      return myLoc;
   }

   public void setLocationOverride(Rectangle pos) {
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
