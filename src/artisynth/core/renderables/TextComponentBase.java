/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.renderables;

import java.awt.Color;
import java.awt.Font;

import artisynth.core.modelbase.RenderableComponentBase;
import maspack.properties.PropertyList;
import maspack.properties.PropertyUtils;
import maspack.render.FaceRenderProps;
import maspack.render.RenderProps;


/**
 * Base class for text components, setting font/alignment
 * @author Antonio
 */
public abstract class TextComponentBase extends RenderableComponentBase {
   
   public enum HorizontalAlignment {
      LEFT, CENTRE, RIGHT
   }
   
   public enum VerticalAlignment {
      TOP, CENTRE, BOTTOM
   }
   
   public static int defaultFontSize = 32;
   public static double defaultTextSize = defaultFontSize;
   public static String defaultFontName = Font.SANS_SERIF;
   public static Font defaultFont = new Font(defaultFontName, 0, defaultFontSize);
   public static HorizontalAlignment defaultHAlignment = HorizontalAlignment.LEFT;
   public static VerticalAlignment defaultVAlignment = VerticalAlignment.BOTTOM;
   
   protected HorizontalAlignment hAlignment = defaultHAlignment;
   protected VerticalAlignment vAlignment = defaultVAlignment;
   protected Font myFont;
   protected double myTextSize = defaultTextSize;
   
   public static PropertyList myProps = new PropertyList(
      TextComponentBase.class, RenderableComponentBase.class);
   
   static {
      myProps.add(
         "renderProps * *", "render properties for this component",
         createDefaultRenderProps());
      
      myProps.add("font", "font", defaultFont);
      myProps.add("textSize", "text size", defaultTextSize);
      myProps.add("horizontalAlignment", "horizontal alignment", defaultHAlignment);
      myProps.add("verticalAlignment", "vertical alignment", defaultVAlignment);

   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }
   
   protected abstract void setDefaults();
   
   public static RenderProps createDefaultRenderProps() {
      return new FaceRenderProps();
   }
   
   @Override
   public RenderProps createRenderProps() {
      RenderProps rprops = createDefaultRenderProps();
      PropertyUtils.updateInheritedProperties (rprops, this, "renderProps");
      return rprops;
   }
   
   public Font getFont() {
      return myFont;
   }
   
   /**
    * Explicitly set the font for this text component
    * @param font the base font.  The font size affects the
    * resolution of the displayed text.  It should be at least 32pt.
    */
   public void setFont(Font font) {
      myFont = font;
   }
   
   /**
    * Sets the size of the text
    */
   public void setTextSize(double size) {
      myTextSize = size;
   }
   
   /**
    * Gets the size of the text
    */
   public double getTextSize() {
      return myTextSize;
   }
   
   /**
    * Gets the horizontal alignment
    */
   public HorizontalAlignment getHorizontalAlignment() {
      return hAlignment;
   }

   /**
    * Sets the horizontal alignment, one of {@code LEFT, CENTRE, RIGHT}
    */
   public void setHorizontalAlignment(HorizontalAlignment hAlignment) {
      this.hAlignment = hAlignment;
   }

   /** 
    * Gets the vertical alignment
    */
   public VerticalAlignment getVerticalAlignment() {
      return vAlignment;
   }

   /**
    * Sets the vertical alignment, one of {@code TOP, CENTRE, BOTTOM}
    */
   public void setVerticalAlignment(VerticalAlignment vAlignment) {
      this.vAlignment = vAlignment;
   }
   
   @Override
   /**
    * 2D text objects will interfere with 3D display if selected 
    */
   public boolean isSelectable() {
      return false;
   }
   
   public int numSelectionQueriesNeeded() {
      return -1;
   }
   
   public void setTextColor(Color c) {
      RenderProps.setFaceColor(this, c);
   }

   
}
