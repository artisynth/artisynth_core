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

   public static int defaultFontSize = 32;
   public static double defaultTextSize = defaultFontSize;
   public static Point2d defaultPos = new Point2d(0, 0);

   private String myText;
   private Point2d myPos; // absolute 2D point on screen
   private double myOrientation;
   private double myTextSize; // scaling factor for font

   RigidTransform3d myTransform = new RigidTransform3d();
   double[] GLMatrix = new double[16];
   Point2d renderPos = new Point2d();

   public static PropertyList myProps = new PropertyList(
      TextComponent2d.class, TextComponentBase.class);

   static {
      // change default font size
      myProps.add("text", "text to display", "");
      myProps.add("position", "display position", defaultPos);
      myProps.add("rotation", "rotation in degrees", 0, "[-180,180]");
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   protected void setDefaults() {
      setFont ( new Font(defaultFontName, 0, defaultFontSize));
      myRenderProps = createDefaultRenderProps();
      hAlignment = defaultHAlignment;
      vAlignment = defaultVAlignment;
      myTextSize = defaultTextSize;
      
      myText = "";
      myPos = new Point2d(defaultPos);
      myOrientation = 0;
   }
   
   public TextComponent2d() {
      setDefaults ();
   }

   /**
    * Sets both name and text to the provided string
    * 
    * @param str name and text string
    */
   public TextComponent2d (String str) {
      setDefaults();
      setText(str);
      setName(str);
   }

   /**
    * Sets both name (if valid) and text to provided string.
    */
   public TextComponent2d (String text, Point2d pos) {
      setDefaults();
      try {
         setName(text);
      } catch (IllegalArgumentException e) {
      }
      setText(text);
      setPosition(pos);
   }

   /**
    * Main constructor, setting name, text and screen position;
    */
   public TextComponent2d (String name, String text, Point2d pos) {
      setDefaults();
      setName(name);
      setText(text);
      setPosition(pos);
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
    * Sets world position to display text.  If a coordinate
    * has a value in [0, 1], then it is assumed to be 
    * normalized, with (0,0) in the bottom-left corner, and
    * (1,1) the top-right.
    */
   public void setPosition(Point2d pos) {
      myPos.set(pos);
   }

   /**
    * Returns position by reference
    */
   public Point2d getPosition() {
      return myPos;
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
   
   @Override
   public void render(Renderer renderer, int flags) {

      if (!isSelectable() && renderer.isSelecting()) {
         return;
      }
      
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
      
      if (myPos.x <= 1) {
         renderPos.x += myPos.x * sw;
      } else {
         renderPos.x += myPos.x;
      }
      if (myPos.y <= 1) {
         renderPos.y += myPos.y * sh;
      } else {
         renderPos.y += myPos.y;
      }

      myTransform.R.m00 = ctheta;
      myTransform.R.m10 = stheta;
      myTransform.R.m01 = -stheta;
      myTransform.R.m11 = ctheta;
      myTransform.p.set(renderPos.x, renderPos.y, 0);

      FaceRenderProps rprops = (FaceRenderProps)getRenderProps();
      renderer.setFaceColoring (rprops, (flags & Renderer.HIGHLIGHT) != 0);

//      boolean saved2d = renderer.is2DRendering();
//      if (!saved2d) {
//         renderer.begin2DRendering(sw, sh);
//      }

      renderer.pushModelMatrix();
      renderer.mulModelMatrix(myTransform);
      
      final float[] ZERO = {0,0,0};
      renderer.drawText(myFont, myText, ZERO, myTextSize);

      renderer.popModelMatrix();

//      if (!saved2d) {
//         renderer.end2DRendering();
//      }

   }

   @Override
   public boolean isSelectable() {
      return false;
   }

   public int numSelectionQueriesNeeded() {
      return -1;
   }


}
