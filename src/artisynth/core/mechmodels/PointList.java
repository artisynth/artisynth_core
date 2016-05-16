/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.util.LinkedList;

import artisynth.core.modelbase.ComponentChangeEvent;
import artisynth.core.modelbase.RenderableComponentList;
import artisynth.core.modelbase.StructureChangeEvent;
import artisynth.core.util.ScalableUnits;
import maspack.properties.PropertyList;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;
import maspack.render.PointRenderProps;
import maspack.render.RenderList;
import maspack.render.RenderObject;
import maspack.render.RenderProps;
import maspack.render.RenderableUtils;
import maspack.render.Renderer;
import maspack.render.Renderer.PointStyle;
import maspack.render.Renderer.Shading;

public class PointList<P extends Point> extends RenderableComponentList<P>
implements ScalableUnits {
   protected static final long serialVersionUID = 1;
   private double myPointDamping;
   private PropertyMode myPointDampingMode = PropertyMode.Inherited;

   private RenderObject myRob = null;

   public static PropertyList myProps =
      new PropertyList (PointList.class, RenderableComponentList.class);

   static {
      myProps.addInheritable (
         "pointDamping", "intrinsic damping force", 0.0, "%.8f");
      myProps.get ("renderProps").setDefaultValue (new PointRenderProps());
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public double getPointDamping() {
      return myPointDamping;
   }

   public void setPointDamping (double d) {
      myPointDamping = d;
      myPointDampingMode =
         PropertyUtils.propagateValue (
            this, "pointDamping", d, myPointDampingMode);
   }

   public PropertyMode getPointDampingMode() {
      return myPointDampingMode;
   }

   public void setPointDampingMode (PropertyMode mode) {
      myPointDampingMode =
         PropertyUtils.setModeAndUpdate (
            this, "pointDamping", myPointDampingMode, mode);
   }

   public PointList (Class<P> type) {
      this (type, null, null);
   }

   public PointList (Class<P> type, String name) {
      super (type, name);
      setRenderProps (createRenderProps());
   }

   public PointList (Class<P> type, String name, String shortName) {
      super (type, name, shortName);
      setRenderProps (createRenderProps());
   }

   // public PointList (Class type, String name, String shortName,
   // CompositeComponent parent)
   // {
   // super (type, name, shortName, parent);
   // setRenderProps(createRenderProps());
   // }

   /* ======== Renderable implementation ======= */

   public RenderProps createRenderProps() {
      return RenderProps.createPointProps (this);
   }

   private final int REG_GRP = 0;
   private final int SEL_GRP = 1;

   protected void buildRenderObject() {

      myRob = new RenderObject();
      myRob.createPointGroup();
      myRob.createPointGroup();
      for (int i=0; i<size(); i++) {
         Point pnt = get(i);
         myRob.addPosition (pnt.myRenderCoords);
         myRob.addVertex (i);
         if (pnt.getRenderProps() == null) {
            myRob.pointGroup (pnt.isSelected() ? SEL_GRP : REG_GRP);
            myRob.addPoint (i);            
         }
      }
   }
   
   protected void updateRenderObject() {
      
      // delete old point groups, keep old vertices
      myRob.clearPrimitives ();
      myRob.createPointGroup(); // regular
      myRob.createPointGroup(); // selected
      for (int i=0; i<size(); i++) {
         Point pnt = get(i);
         if (pnt.getRenderProps() == null) {
            myRob.pointGroup (pnt.isSelected() ? SEL_GRP : REG_GRP);
            myRob.addPoint (i);            
         }
      }
   }

   protected boolean renderObjectValid() {
      // checks if selection has changed
      int kSel = 0;
      int kReg = 0;
      int numReg = myRob.numPoints(REG_GRP);
      int numSel = myRob.numPoints(SEL_GRP);
      int[] viReg = myRob.getPoints(REG_GRP);
      int[] viSel = myRob.getPoints(SEL_GRP);
      for (int i=0; i<size(); i++) {
         Point pnt = get(i);
         if (pnt.getRenderProps() == null) {
            if (pnt.isSelected()) {
               if (kSel >= numSel || viSel[kSel++] != i) {
                  return false;
               }
            }
            else {
               if (kReg >= numReg || viReg[kReg++] != i) {
                  return false;
               }
            }
         }
      }
      if (kSel != numSel || kReg != numReg) {
         return false;
      }
      return true;
   }

   public void prerender (RenderList list) {
      
      // maybe update render object
      if (myRob == null) {
         buildRenderObject ();
      }
      else if (!renderObjectValid()) {
         // only update point groups
         updateRenderObject();
      }
      
      // assume positions have been modified
      myRob.notifyPositionsModified();      
      for (int i = 0; i < size(); i++) {
         Point pnt = get (i);
         if (pnt.getRenderProps() != null) {
            list.addIfVisible (pnt);
         }
         else {
            pnt.prerender (list);
         }
      }
   }

   private void drawPoints (
      Renderer renderer, int gidx, RenderProps props, boolean selected) {
   
      Shading savedShading = renderer.setPointShading (props);
      renderer.setPointColoring (props, selected);
      PointStyle style = props.getPointStyle ();
      switch (style) {
         case POINT: {
            int size = props.getPointSize();
            if (size > 0) {
               //renderer.setLightingEnabled (false);
               //renderer.setColor (props.getPointColorArray(), selected);
               renderer.drawPoints (myRob, gidx, PointStyle.POINT, size);
               //renderer.setLightingEnabled (true);
            }
            break;
         }
         case CUBE:
         case SPHERE: {
            double rad = props.getPointRadius();
            if (rad > 0) {
               //Shading savedShading = renderer.getShadeModel();
               //renderer.setPointLighting (props, selected);
               renderer.drawPoints (myRob, gidx, style, rad);
               //renderer.setShadeModel(savedShading);
            }
            break;
         }
      }
      renderer.setShading(savedShading);
   }
   
   private void drawPointsSelecting (Renderer renderer, int gidx, RenderProps props) {
   
      PointStyle style = props.getPointStyle ();
      double size = 0;
      switch (style) {
         case POINT: {
            size = props.getPointSize();
            break;
         }
         case CUBE:
         case SPHERE: 
         default: {
            size = props.getPointRadius();
            break;
         }
      }
      
      if (size > 0) {
         int[] points = myRob.getPoints (gidx);
         for (int i=0; i<points.length; ++i) {
            renderer.beginSelectionQuery (points[i]);
            renderer.drawPoints (myRob, gidx, i, 1, style, size);
            renderer.endSelectionQuery ();
         }
      }
   }

   public void render (Renderer renderer, int flags) {
      RenderProps props = myRenderProps;
      if (myRob != null) {
         int numReg = myRob.numPoints(REG_GRP);
         int numSel = myRob.numPoints(SEL_GRP);

         // draw selected first
         if (renderer.isSelecting ()) {
            if (numSel > 0) {
               drawPointsSelecting (renderer, SEL_GRP, props);
            }
            if (numReg > 0) {
               drawPointsSelecting (renderer, REG_GRP, props);
            }
         } else {
            if (numSel > 0) {
               drawPoints (renderer, SEL_GRP, props, /*selected=*/true);
            }
            if (numReg > 0) {
               drawPoints (renderer, REG_GRP, props, /*selected=*/false);
            }
         }

      }
   }

   // public void prerender (RenderList list) {
   //    for (int i = 0; i < size(); i++) {
   //       Point pnt = get (i);
   //       if (pnt.getRenderProps() != null) {
   //          list.addIfVisible (pnt);
   //       }
   //       else {
   //          pnt.prerender (list);
   //       }
   //    }
   // }

   // public void render (Renderer renderer, int flags) {
   //    renderer.drawPoints (myRenderProps, iterator());
   // }

   public boolean rendersSubComponents() {
      return true;
   }

   /**
    * {@inheritDoc}
    */
   public boolean isSelectable() {
      return true;
   }

   public int numSelectionQueriesNeeded() {
      return size();
   }

   public void getSelection (LinkedList<Object> list, int qid) {
      if (qid >= 0 && qid < size()) {
         list.addLast (get (qid));
      }
   }
   
   public void scaleDistance (double s) {
      for (int i = 0; i < size(); i++) {
         get (i).scaleDistance (s);
      }
      if (myRenderProps != null) {
         RenderableUtils.cloneRenderProps (this);
         myRenderProps.scaleDistance (s);
      }
   }

   public void scaleMass (double s) {
      myPointDamping *= s;
      for (int i = 0; i < size(); i++) {
         get (i).scaleMass (s);
      }
   }

   public void notifyParentOfChange (ComponentChangeEvent e) {
      if (e instanceof StructureChangeEvent) {
         myRob = null;
      }
      super.notifyParentOfChange (e);
   }

}
