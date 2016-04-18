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

   public enum FontStyle {
      PLAIN, BOLD, ITALIC, BOLD_ITALIC
   };
   
   public enum HorizontalAlignment {
      LEFT, CENTRE, RIGHT
   }
   
   public enum VerticalAlignment {
      TOP, CENTRE, BOTTOM
   }
   
   public static int defaultFontSize = 32;
   public static double defaultTextSize = defaultFontSize;
   public static String defaultFontName = Font.SANS_SERIF;
   public static HorizontalAlignment defaultHAlignment = HorizontalAlignment.LEFT;
   public static VerticalAlignment defaultVAlignment = VerticalAlignment.BOTTOM;
   
   protected HorizontalAlignment hAlignment;
   protected VerticalAlignment vAlignment;
   protected Font myFont;
   protected double myTextSize;
   
   public static PropertyList myProps = new PropertyList(
      TextComponentBase.class, RenderableComponentBase.class);
   
   static {
      myProps.add(
         "renderProps * *", "render properties for this component",
         createDefaultRenderProps());
      
      myProps.add("fontFamily", "font name", defaultFontName);
      myProps.add("fontStyle", "font style", FontStyle.PLAIN);
      myProps.add("fontSize", "font size", defaultFontSize);
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
   
   /**
    * Explicitly set the font for this text component
    */
   public void setFont(Font font) {
      myFont = font;
   }
   
   /**
    * Returns the base font size
    */
   public int getFontSize() {
      return myFont.getSize();
   }

   /**
    * Sets the font size.  This is only used to control
    * text resolution.  The actual size of the displayed
    * text should be controlled by the "text size".
    * @see #setTextSize(double)
    */
   public void setFontSize(int size) {
      if (size != myFont.getSize()) {
         myFont = myFont.deriveFont((float)size);
      }
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
    * Returns the font family
    */
   public String getFontFamily() {
      return myFont.getFamily();
   }

   /**
    * Sets the font family for the displayed text
    */
   public void setFontFamily(String family) {
      if (!myFont.getFamily().equals(family)) {
         Font nfont = new Font(family, myFont.getStyle(), myFont.getSize());
         if (nfont != null) {
            myFont = nfont;
         }
      }
   }
   
   /**
    * Sets the font style to be one of 
    * {@code PLAIN, BOLD, ITALIC, BOLD_ITALIC}.
    */
   public void setFontStyle(FontStyle style) {
      int flags = 0;
      switch (style) {
         case BOLD:
            flags = Font.BOLD;
            break;
         case BOLD_ITALIC:
            flags = Font.BOLD | Font.ITALIC;
            break;
         case ITALIC:
            flags = Font.ITALIC;
            break;
         case PLAIN:
            flags = Font.PLAIN;
            break;
         default:
            break;
      }
      
      if (flags != myFont.getStyle()) {
         myFont = myFont.deriveFont(flags);
      }
   }
   
   /**
    * Gets font style, BOLD, ITALIC, BOLD_ITALIC, or PLAIN
    */
   public FontStyle getFontStyle() {
      int style = myFont.getStyle();
      
      if ( (style & Font.BOLD) != 0) {
         if ((style & Font.ITALIC) != 0) {
            return FontStyle.BOLD_ITALIC;
         }
         return FontStyle.BOLD;
      } else if ( (style & Font.ITALIC) != 0) {
         return FontStyle.ITALIC;
      }
      return FontStyle.PLAIN;
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
