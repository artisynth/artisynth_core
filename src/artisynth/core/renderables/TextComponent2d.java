/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.renderables;

import java.awt.Font;
import java.awt.geom.Rectangle2D;

import maspack.matrix.Point2d;
import maspack.matrix.RigidTransform3d;
import maspack.properties.PropertyDesc;
import maspack.properties.PropertyList;
import maspack.render.FaceRenderProps;
import maspack.render.IsRenderable;
import maspack.render.Renderer;

/**
 * Allows adding 2D text to a root model
 * 
 * @author Antonio, July 7, 2013
 * 
 */
public class TextComponent2d extends TextComponentBase {

   public static int defaultFontSize = 12;
   public static double defaultTextSize = defaultFontSize;
   public static Point2d defaultPos = new Point2d(-1,-1);
   public static Point2d defaultNormPos = new Point2d(0,0);

   private String myText;
   private Point2d myPos; // absolute 2D point on screen
   private Point2d myNormPos; // relative position
   private double myOrientation;
   private double myTextSize; // scaling factor for font

   RigidTransform3d myTransform = new RigidTransform3d();
   double[] GLMatrix = new double[16];
   Point2d renderPos = new Point2d();
   float[] rgb = new float[3];

   public static PropertyList myProps = new PropertyList(
      TextComponent2d.class, TextComponentBase.class);

   static {
      // change default font size
      PropertyDesc info = myProps.get("fontSize");
      info.setDefaultValue(defaultFontSize);
      info = myProps.get("textSize");
      info.setDefaultValue(defaultTextSize);
      
      myProps.add("text", "text to display", "");
      myProps.add("positionOverride", 
         "display position override (if >0, overrides normalized position value)", 
         defaultPos);
      myProps.add("normalizedPosition", "normalized position", defaultNormPos);
      myProps.add("rotation", "rotation in degrees", 0, "[-180,180]");
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   protected void setDefaults() {
      myFont = new Font(defaultFontName, 0, defaultFontSize);
      myRenderProps = createDefaultRenderProps();
      hAlignment = defaultHAlignment;
      vAlignment = defaultVAlignment;
      myTextSize = defaultTextSize;
      myFontSize = defaultFontSize;
      
      myText = "";
      myPos = new Point2d(defaultPos);
      myNormPos = new Point2d(defaultNormPos);
      myOrientation = 0;
   }

   /**
    * Sets both name and text to the provided string
    * 
    * @param name
    */
   public TextComponent2d (String name) {
      setDefaults();
      setText(name);
      setName(name);
   }

   /**
    * Sets both name (if valid) and text to provided string.  
    * The position is assumed to be normalized in [0,1]x[0,1].
    * For pixel coordinate, use {@link #setPositionOverride(Point2d)}.
    */
   public TextComponent2d (String text, Point2d pos) {
      setDefaults();
      try {
         setName(text);
      } catch (IllegalArgumentException e) {
      }
      setText(text);
      setNormalizedPosition(pos);
   }

   /**
    * Main constructor, setting name, text and screen (pixel) position.
    * The position is assumed to be normalized in [0,1]x[0,1].
    * For pixel coordinate, use {@link #setPositionOverride(Point2d)}.
    */
   public TextComponent2d (String name, String text, Point2d pos) {
      setDefaults();
      setName(name);
      setText(text);
      setPositionOverride(pos);
   }

   /**
    * Sets the text to display
    */
   public void setText(String text) {
      myText = text;
   }

   public String getText() {
      return myText;
   }

   /**
    * Sets world position to display text.  If either x
    * or y is negative, then the normalized position
    * is used for that dimension
    */
   public void setPositionOverride(Point2d pos) {
      myPos.set(pos);
   }

   /**
    * Sets normalized position to display text,
    * (x,y) in [0,1]x[0,1]
    */
   public void setNormalizedPosition(Point2d pos) {
      myNormPos.x = Math.min(Math.max(pos.x, 0), 1);
      myNormPos.y = Math.min(Math.max(pos.y, 0), 1);
   }

   /**
    * Returns position by reference
    */
   public Point2d getPositionOverride() {
      return myPos;
   }
   
   /**
    * Returns normalized position by reference
    */
   public Point2d getNormalizedPosition() {
      return myNormPos;
   }

   /**
    * Gets the text angle
    */
   public double getRotation() {
      return myOrientation;
   }

   /**
    * Sets the text angle, in degrees
    */
   public void setRotation(double deg) {
      myOrientation = deg;
   }

   @Override
   public int getRenderHints() {
      return super.getRenderHints() 
         | IsRenderable.TRANSPARENT | IsRenderable.TWO_DIMENSIONAL;
   }

//   @Override
//   public void renderx(GLRenderer renderer, int flags) {
//      if (isSelectable() || !renderer.isSelecting()) {
//         render(renderer, 0);
//      }
//   }

   
   @Override
   public void render(Renderer renderer, int flags) {

      if (!isSelectable() && renderer.isSelecting()) {
         return;
      }
      
      FaceRenderProps rprops = (FaceRenderProps)getRenderProps();

      // text orientation computation
      Rectangle2D box = renderer.getTextBounds(myFont, myText, myTextSize);
      double t = box.getHeight ()+box.getY ();
      double vc = box.getCenterY ()+box.getHeight ()/2;

      double w = box.getWidth();
      
      // Position is assumed to be ([0,1], [0,1])
      int sw = renderer.getScreenWidth();
      int sh = renderer.getScreenHeight();
      renderPos.setZero();

      switch (hAlignment) {
         case CENTRE:
            renderPos.add(-w / 2, 0);
            break;
         case RIGHT:
            renderPos.add(-w, 0);
            break;
         default:
            break;
      }

      switch (vAlignment) {
         case CENTRE:
            renderPos.add(0, -vc);
            break;
         case TOP:
            renderPos.add(0, -t);
            break;
         case BOTTOM:
            // renderPos.add(0, -b);
            break;
      }

      // account for non-zero orientation
      double rad = Math.toRadians(myOrientation);
      double ctheta = Math.cos(rad);
      double stheta = Math.sin(rad);
      
      renderPos.rotate(ctheta, stheta, renderPos);
      
      if (myPos.x <= 0) {
         renderPos.x += myNormPos.x * sw;
      } else {
         renderPos.x += myPos.x;
      }
      if (myPos.y <= 0) {
         renderPos.y += myNormPos.y * sh;
      } else {
         renderPos.y += myPos.y;
      }

      myTransform.R.m00 = ctheta;
      myTransform.R.m10 = stheta;
      myTransform.R.m01 = -stheta;
      myTransform.R.m11 = ctheta;
      myTransform.p.set(renderPos.x, renderPos.y, 0);

      if (isSelected()) {
         renderer.getHighlightColor(rgb);
      } else {
         rprops.getFaceColor(rgb);
      }

      boolean saved2d = renderer.is2DRendering();
      if (!saved2d) {
         renderer.begin2DRendering(sw, sh);
      }

      renderer.pushModelMatrix();
      renderer.mulModelMatrix(myTransform);
      
      renderer.setColor(rgb[0], rgb[1], rgb[2], (float)rprops.getAlpha());
      final float[] ZERO = {0,0,0};
      renderer.drawText(myFont, myText, ZERO, myTextSize);

      renderer.popModelMatrix();

      if (!saved2d) {
         renderer.end2DRendering();
      }

   }

   @Override
   public boolean isSelectable() {
      return false;
   }

   public int numSelectionQueriesNeeded() {
      return -1;
   }


}
