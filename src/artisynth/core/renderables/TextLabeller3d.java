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
import java.util.Iterator;
import java.util.List;

import javax.media.opengl.GL2;

import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.Point2d;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.RotationMatrix3d;
import maspack.properties.PropertyDesc;
import maspack.properties.PropertyList;
import maspack.render.FaceRenderProps;
import maspack.render.GLRenderable;
import maspack.render.GLRenderer;
import maspack.render.GLSupport;
import artisynth.core.mechmodels.Point;
import artisynth.core.mechmodels.PointList;

import com.jogamp.opengl.util.awt.TextRenderer;


public class TextLabeller3d extends TextComponentBase {

   private static class LabelItem {
      public int id;
      public String text;
      public Point3d pos;
      public AffineTransform3dBase trans;
      
      public LabelItem(String txt, Point3d p, int idx) {
         this(txt,p, RigidTransform3d.IDENTITY, idx);
      }
      
      public LabelItem(String txt, Point3d p, AffineTransform3dBase tr, int idx) {
         id = idx;
         pos = p;
         text = txt;
         trans = tr;
      }
   }
   private int nextLabelId = 0;
   
   public static int defaultFontSize = 64;
   public static double defaultTextSize = 12.0;
   public static Point2d defaultTextOffset = new Point2d(0, 0);
   public static boolean defaultByReference = true;
   
   public static PropertyList myProps = new PropertyList(
      TextLabeller3d.class, TextComponentBase.class);

   
   protected Point2d myTextOffset;
   ArrayList<LabelItem> myItems;
   
   static {
      // change default font size
      PropertyDesc info = myProps.get("fontSize");
      info.setDefaultValue(defaultFontSize);
      info = myProps.get("textSize");
      info.setDefaultValue(defaultTextSize);
      myProps.add("textOffset", "space to offset text", Point2d.ZERO);
   }
   
   // intermediate variables
   RigidTransform3d myTransform = new RigidTransform3d();
   private double[] GLMatrix = new double[16];
   RotationMatrix3d rEye = new RotationMatrix3d();
   Point3d renderPos = new Point3d();
   double[] xdir = new double[3];
   double[] ydir = new double[3];
   float[] rgb = new float[3];

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }
   
   public TextLabeller3d() {
      setDefaults();
   }
   
   public TextLabeller3d(String name) {
      setName(name);
      setDefaults();
   }
   
   @Override
   protected void setDefaults() {
      myFont = new Font(defaultFontName, 0, defaultFontSize);
      myTextRenderer = new TextRenderer(myFont);
      myRenderProps = createDefaultRenderProps();
      hAlignment = defaultHAlignment;
      vAlignment = defaultVAlignment;
      
      myTextSize = defaultTextSize;
      myTextOffset = new Point2d(defaultTextOffset);
      myItems = new ArrayList<LabelItem>();
      
   }

//   @Override
//   public void renderx(GLRenderer renderer, int flags) {
//      if (isSelectable() || !renderer.isSelecting()) {
//         render(renderer, 0);
//      }
//   }
   
   @Override
   public void render(GLRenderer renderer, int flags) {
    
      if (!isSelectable() && renderer.isSelecting()) {
         return;
      }
      
      FaceRenderProps rprops = (FaceRenderProps)getRenderProps();
      rprops.getFaceColor(rgb);
      GL2 gl = renderer.getGL2().getGL2();
      float fTextSize = (float)(myTextSize/getFontSize());
      Point3d tmp = new Point3d();
      
      // for consistency, assume line top as 3/4 font size
      double t = myTextSize*0.75;
      double vc = myTextSize* 0.25;
      
      rEye.set(renderer.getEyeToWorld().R);
      rEye.getColumn(0, xdir);
      rEye.getColumn(1, ydir);
      myTransform.R.set(rEye);
      
      myTextRenderer.setColor(rgb[0], rgb[1], rgb[2], (float)rprops.getAlpha());
      myTextRenderer.begin3DRendering();
      
      for (LabelItem label : myItems) {
         renderPos.setZero();
         
         // text orientation computation
         Rectangle2D box = myTextRenderer.getBounds(label.text);
         double w = box.getWidth() * fTextSize;
         
         switch(hAlignment) {
            case LEFT:
               renderPos.add(xdir[0]*(myTextOffset.x), 
                  xdir[1]*(myTextOffset.x), 
                  xdir[2]*(myTextOffset.x));
               break;
            case CENTRE:
               renderPos.add(-xdir[0]*w/2, -xdir[1]*w/2, -xdir[2]*w/2);
               break;
            case RIGHT:
               renderPos.add(-xdir[0]*(w+myTextOffset.x) , 
                  -xdir[1]*(w+myTextOffset.x), 
                  -xdir[2]*(w+myTextOffset.x));
               break;
            default:
               break; 
         }
         
       
         switch(vAlignment) {
            case BOTTOM:
               renderPos.add(ydir[0]*(myTextOffset.y), 
                  ydir[1]*(myTextOffset.y), 
                  ydir[2]*(myTextOffset.y));
               break;
            case CENTRE:
               renderPos.add(-ydir[0]*vc, -ydir[1]*vc, -ydir[2]*vc);
               break;
            case TOP:
               renderPos.add(-ydir[0]*(t+myTextOffset.y), 
                  -ydir[1]*(t+myTextOffset.y), 
                  -ydir[2]*(t+myTextOffset.y));
               break;
            default:
         }
        
         //renderPos.transform(rEye);
         tmp.set (label.pos);
         tmp.transform (label.trans);
         renderPos.add(tmp);
         myTransform.p.set(renderPos);
         
         gl.glPushMatrix();
         GLSupport.transformToGLMatrix (GLMatrix, myTransform);
         gl.glMultMatrixd (GLMatrix, 0);
         
         myTextRenderer.draw3D(label.text, 0,0,0, fTextSize);
         myTextRenderer.flush();
         
         gl.glPopMatrix();
      }

      myTextRenderer.end3DRendering();
      
   }
   
   @Override
   public int getRenderHints() {
      return GLRenderable.TRANSLUCENT; // for clear background
   }
   
   /**
    * Returns the text scaling
    */
   public double getTextSize() {
      return myTextSize;
   }
   
   /**
    * Sets space to add to the left/bottom
    */
   public void setTextOffset(Point2d offset) {
      myTextOffset.set(offset);
   }
   
   public void setTextOffset(double xOffset, double yOffset) {
      myTextOffset.set(xOffset, yOffset);
   }
   
   /**
    * Sets space to add to the left/bottom
    */
   public Point2d getTextOffset() {
      return myTextOffset;
   }

   /**
    * Adds a label to draw
    * @param text text to display
    * @param pos position of text in 3D world
    * @param byRef if true, sets the point by reference
    * @return in ID number which can be used to remove the label
    */
   public int addItem(String text, Point3d pos, boolean byRef) {
      if (!byRef) {
         pos = new Point3d(pos);
      }
      LabelItem item = new LabelItem(text, pos, nextLabelId++);
      myItems.add(item);
      return (nextLabelId-1);
   }
   
   /**
    * Adds a label to draw
    * @param text text to display
    * @param pos position of text in 3D world
    * @param trans transform to apply to position when rendering
    * @param byRef if true, sets the point/transform by reference
    * @return in ID number which can be used to remove the label
    */
   public int addItem(String text, Point3d pos, AffineTransform3dBase trans, boolean byRef) {
      if (!byRef) {
         pos = new Point3d(pos);
         trans = trans.clone ();
      }
      LabelItem item = new LabelItem(text, pos, trans, nextLabelId++);
      myItems.add(item);
      return (nextLabelId-1);
   }
   
   /**
    * Removes a label based on its ID number
    */
   public boolean removeItem(int id) {
      
      Iterator<LabelItem> lit = myItems.iterator();
      while (lit.hasNext()) {
         LabelItem item = lit.next();
         if (item.id == id) {
            lit.remove();
            return true;
         }
      }
      return false;
   }
   
   /**
    * Clears all text items
    */
   public void clearItems() {
      myItems.clear();
   }
   
   public void labelPoints(List<? extends Point> pnts) {
      for (Point pnt : pnts) {
         String text = pnt.getName();
         if (text == null) {
            text = "" + pnt.getNumber();
         }
         addItem(text, pnt.getPosition(), true);
      }
   }
   
   public void labelPoints(PointList<? extends Point> pnts) {
      for (Point pnt : pnts) {
         String text = pnt.getName();
         if (text == null) {
            text = "" + pnt.getNumber();
         }
         addItem(text, pnt.getPosition(), true);
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
