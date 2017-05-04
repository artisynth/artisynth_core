/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.renderables;

import java.util.LinkedList;

import artisynth.core.modelbase.ComponentChangeEvent;
import artisynth.core.modelbase.RenderableComponentList;
import artisynth.core.modelbase.StructureChangeEvent;
import artisynth.core.util.ScalableUnits;
import maspack.matrix.Vector3d;
import maspack.properties.PropertyList;
import maspack.render.PointRenderProps;
import maspack.render.RenderList;
import maspack.render.RenderObject;
import maspack.render.RenderProps;
import maspack.render.RenderableUtils;
import maspack.render.Renderer;
import maspack.render.Renderer.PointStyle;
import maspack.render.Renderer.Shading;

public class VertexList<P extends VertexComponent> extends RenderableComponentList<P>
   implements ScalableUnits {

   protected static final long serialVersionUID = 1;

   public static PropertyList myProps =
   new PropertyList (VertexList.class, RenderableComponentList.class);

   static {
      myProps.get ("renderProps").setDefaultValue (new PointRenderProps());
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }
   
   private final int REG_GRP = 0;
   private final int SEL_GRP = 1;
   private RenderObject myRob = null;
   private boolean myRobValidP = false;

   public VertexList (Class<P> type, String name, String shortName) {
      super (type, name, shortName);
      setRenderProps (createRenderProps());
      myRob = null;
   }

   /* ======== Renderable implementation ======= */

   public RenderProps createRenderProps() {
      return RenderProps.createPointProps (this);
   }
   
   protected void buildRenderObject() {

      myRob = new RenderObject();
      myRob.createPointGroup();
      myRob.createPointGroup();
      for (int i=0; i<size(); i++) {
         VertexComponent pnt = get(i);
         myRob.addPosition (pnt.myRenderCoords);
         myRob.addVertex (i);
         if (pnt.getRenderProps() == null) {
            myRob.pointGroup (pnt.isSelected() ? SEL_GRP : REG_GRP);
            myRob.addPoint (i);            
         }
      }
      myRobValidP = true;
   }
   
   protected void updateRenderObject() {
      
      // delete old point groups, keep old vertices
      myRob.clearPrimitives ();
      myRob.createPointGroup(); // regular
      myRob.createPointGroup(); // selected
      for (int i=0; i<size(); i++) {
         VertexComponent pnt = get(i);
         if (pnt.getRenderProps() == null) {
            myRob.pointGroup (pnt.isSelected() ? SEL_GRP : REG_GRP);
            myRob.addPoint (i);            
         }
      }
   }

   public void invalidateRenderObject() {
      myRobValidP = false;
   }

   protected boolean renderObjectValid() {
      if (!myRobValidP) {
         return false;
      }
      // checks if selection has changed
      int kSel = 0;
      int kReg = 0;
      int numReg = myRob.numPoints(REG_GRP);
      int numSel = myRob.numPoints(SEL_GRP);
      int[] viReg = myRob.getPoints(REG_GRP);
      int[] viSel = myRob.getPoints(SEL_GRP);
      for (int i=0; i<size(); i++) {
         VertexComponent pnt = get(i);
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
         VertexComponent pnt = get (i);
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

   public void render (Renderer renderer, int flags) {
      RenderProps props = myRenderProps;
      if (renderer.isSelecting()) {
         PointStyle style = props.getPointStyle();
         if (style == PointStyle.POINT) {
            int size = props.getPointSize();
            if (size > 0) {
               renderer.setPointSize (size);
            }
            else {
               return;
            }
         }           
         for (int i=0; i<size(); i++) {
            VertexComponent pnt = get(i);        
            if (pnt.getRenderProps() == null && renderer.isSelectable (pnt)) {
               float[] v0 = pnt.myRenderCoords;
               renderer.beginSelectionQuery (i);
               switch (style) {
                  case POINT: {
                     renderer.drawPoint (v0);
                     break;
                  }
                  case CUBE: {
                     renderer.drawCube (v0, 2*props.getPointRadius ());
                  }
                  case SPHERE: {
                     renderer.drawSphere (v0, props.getPointRadius());
                     break;
                  }
               }
               renderer.endSelectionQuery ();
            }
         }
         if (style == PointStyle.POINT) {
            renderer.setPointSize (1);
         }
      }
      else if (myRob != null) {
         int numReg = myRob.numPoints(REG_GRP);
         int numSel = myRob.numPoints(SEL_GRP);

         // draw selected first
         if (numSel > 0) {
            drawPoints (renderer, SEL_GRP, props, /*selected=*/true);
         }
         if (numReg > 0) {
            drawPoints (renderer, REG_GRP, props, /*selected=*/false);
         }
      }
   }

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
      // nothing
   }

   public void notifyParentOfChange (ComponentChangeEvent e) {
      if (e instanceof StructureChangeEvent) {
         myRob = null;
      }
      super.notifyParentOfChange (e);
   }

   @Override
   public void updateBounds(Vector3d pmin, Vector3d pmax) {
      for (VertexComponent c : this) {
         c.updateBounds(pmin, pmax);
      }
   }

}
