/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.renderables;

import java.awt.Font;
import java.awt.geom.Rectangle2D;

import maspack.matrix.AffineTransform3d;
import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.AxisAngle;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.RotationMatrix3d;
import maspack.properties.PropertyDesc;
import maspack.properties.PropertyList;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.render.GL.GLRenderable;
import artisynth.core.util.TransformableGeometry;

import com.jogamp.opengl.util.awt.TextRenderer;


/**
 * Allows adding text to a root model, with the option of
 * having the text follow the viewer.
 * 
 * @author Antonio, July 7, 2013
 *
 */
public class TextComponent3d extends TextComponentBase implements 
   TransformableGeometry {

   public static int defaultFontSize = 64;
   public static double defaultTextSize = 1.0;
   public static boolean defaultByReference = false;
   
   protected String myText;
   protected Point3d myPos;
   protected AxisAngle myOrientation;
   private boolean followEye;
   
   // intermediate variables
   RigidTransform3d myTransform = new RigidTransform3d();
   private double[] GLMatrix = new double[16];
   RotationMatrix3d rEye = new RotationMatrix3d();
   Point3d renderPos = new Point3d();
   double[] xdir = new double[3];
   float[] rgb = new float[3];
   
   public static PropertyList myProps = new PropertyList(
      TextComponent3d.class, TextComponentBase.class);

   
   static {
      // change default font size
      PropertyDesc info = myProps.get("fontSize");
      info.setDefaultValue(defaultFontSize);
      info = myProps.get("textSize");
      info.setDefaultValue(defaultTextSize);
      
      myProps.add("text", "text to display", "");
      myProps.add("position", "display position", Point3d.ZERO);
      myProps.add("orientation", "orientation relative to world", AxisAngle.IDENTITY);
      
      myProps.add("followEye isFollowingEye *", "text relative to eye", true);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }
   
   protected void setDefaults() {
      myFont = new Font(defaultFontName, 0, defaultFontSize);
      myTextRenderer = new TextRenderer(myFont);
      myRenderProps = createDefaultRenderProps();
      hAlignment = defaultHAlignment;
      vAlignment = defaultVAlignment;
      
      myText = "";
      myPos = new Point3d();
      myOrientation = new AxisAngle();
      myTextSize = defaultTextSize;
      followEye = true;
      
   }
   
   /**
    * Sets both name and text to the provided string
    * @param name
    */
   public TextComponent3d(String name) {
      setDefaults();
      setText(name);
      setName(name);
   }
   
   /**
    * Sets both name (if valid) and text to provided string
    */
   public TextComponent3d(String text, Point3d pos) {
      setDefaults();
      try {
         setName(text);
      } catch (IllegalArgumentException e) {
      }
      setText(text);
      setPosition(pos, defaultByReference);
   }
   
   /**
    * Main constructor
    */
   public TextComponent3d(String name, String text, Point3d pos) {
      setDefaults();
      setName(name);
      setText(text);
      setPosition(pos, defaultByReference);
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
    * Sets the position to display text
    */
   public void setPosition(Point3d pos) {
      setPosition(pos, defaultByReference);
   }
   
   /**
    * Sets world position to display text.  If byRef is true, then
    * sets position by reference.
    */
   public void setPosition(Point3d pos, boolean byRef) {
      if (!byRef) {
         myPos.set(pos);
      } else {
         myPos = pos;
      }
   }
   
   /**
    * Returns position by reference
    */
   public Point3d getPosition() {
      return myPos;
   }
   
   /**
    * Orientation w.r.t. world.  If we are following
    * the viewer, this is usually the identity.
    * 
    */
   public void setOrientation(AxisAngle orient) {
      myOrientation.set(orient);
   }
   
   public AxisAngle getOrientation() {
      return myOrientation;
   }
   
//   @Override
//   public void renderx(GLRenderer renderer, int flags) {
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
      
      RenderProps rprops = getRenderProps();
      rprops.getFaceColor(rgb);
      
      if (isSelected()) {
         renderer.getSelectionColor().getRGBColorComponents(rgb);   
      }
      
      float fTextSize = (float)(myTextSize/getFontSize());
      
      // text orientation computation
      Rectangle2D box = myTextRenderer.getBounds(myText);
      double w = box.getWidth() * fTextSize;
      // for consistency, assume line top as 3/4 font size
      double t = myTextSize*0.75;
      double vc = myTextSize* 0.25;
      
      rEye.set(renderer.getEyeToWorld().R);
      if (followEye) {
         rEye.getColumn(0, xdir);
      } else {
         xdir[0] = 1; xdir[1] = 0; xdir[2] = 0;
      }
      
      renderPos.setZero();
      switch(hAlignment) {
         case CENTRE:
            renderPos.add(-xdir[0]*w/2, -xdir[1]*w/2, -xdir[2]*w/2);
            break;
         case RIGHT:
            renderPos.add(-xdir[0]*w, -xdir[1]*w, -xdir[2]*w);
            break;
         default:
            break; 
      }
      
      if (followEye) {
         rEye.getColumn(1, xdir);
      } else {
         xdir[0] = 0; xdir[1] = 1; xdir[2] = 0;
      }
      switch(vAlignment) {
         case CENTRE:
            renderPos.add(-xdir[0]*vc, -xdir[1]*vc, -xdir[2]*vc);
            break;
         case TOP:
            renderPos.add(-xdir[0]*t, -xdir[1]*t, -xdir[2]*t);
            break;
         default:
      }
      // account for non-zero orientation
      myTransform.setRotation(myOrientation);
      
      if (followEye) {
         myTransform.R.mul(rEye, myTransform.R);
         renderPos.inverseTransform(rEye);
      }
      renderPos.transform(myTransform.R);
      renderPos.add(myPos);
      myTransform.p.set(renderPos);
      
      renderer.pushModelMatrix();
      renderer.mulTransform(myTransform);
      
      myTextRenderer.begin3DRendering();            
      myTextRenderer.setColor(rgb[0], rgb[1], rgb[2], (float)rprops.getAlpha());
      myTextRenderer.draw3D(myText, 0,0,0,fTextSize);
      myTextRenderer.end3DRendering();
      
      renderer.popModelMatrix();
      
   }
   
   @Override
   public int getRenderHints() {
      return GLRenderable.TRANSLUCENT; // for transparent background
   }   
   
   /**
    * Checks whether the text is following the viewer's eye
    */
   public boolean isFollowingEye() {
      return followEye;
   }
   
   /**
    * Sets whether to have the text follow the user's eye
    */
   public void setFollowEye(boolean set) {
      followEye = set;
   }

   @Override
   public void updateBounds(Point3d pmin, Point3d pmax) {
      myPos.updateBounds(pmin, pmax);
   }
   
   @Override
   public void transformGeometry(AffineTransform3dBase X) {
      transformGeometry(X, this, 0);
   }

   @Override
   public void transformGeometry(AffineTransform3dBase X,
      TransformableGeometry topObject, int flags) {
      
      RigidTransform3d Xpose = new RigidTransform3d();
      AffineTransform3d Xlocal = new AffineTransform3d();
      
      // read rotation off myTransform to account for viewer rotation
      Xpose.setRotation(myOrientation);
      if (followEye) {
         Xpose.R.mul(rEye, Xpose.R);
      }
      Xpose.setTranslation(myPos);
      
      Xpose.mulAffineLeft (X, Xlocal.A);
      myTransform.set(Xpose);
      if (followEye) {
         myTransform.R.mulInverseLeft(rEye, Xpose.R);
      }
      myOrientation.set(myTransform.R);
      myPos.set(Xpose.p);
      
   }
   
   @Override
   public boolean isSelectable() {
      return true;
   }
   
   public int numSelectionQueriesNeeded() {
      return -1;
   }

   /**
    * Sets the base font size.  This should be used only to adjust
    * the resolution of the font.  For controlling the text size,
    * use {@link #setTextSize(double)}.
    * @param size
    */
   @Override
   public void setFontSize(int size) {
      super.setFontSize(size);
   }

}
