/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.util.LinkedList;

import artisynth.core.modelbase.ComponentChangeEvent;
import artisynth.core.modelbase.StructureChangeEvent;
import maspack.render.RenderList;
import maspack.render.RenderObject;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.render.Renderer.LineStyle;
import maspack.render.Renderer.Shading;

public class AxialSpringList<S extends AxialSpring> extends PointSpringList<S> {

   protected static final long serialVersionUID = 1;

   private RenderObject myRob = null;

   public AxialSpringList (Class<S> type) {
      this (type, null, null);
   }
   
   public AxialSpringList (Class<S> type, String name, String shortName) {
      super (type, name, shortName);
      setRenderProps (createRenderProps());
   }

   public int numSelectionQueriesNeeded() {
      return size();
   }

   private final int REG_GRP = 0;
   private final int SEL_GRP = 1;

   protected void buildRenderObject() {

      myRob = new RenderObject();
      myRob.createLineGroup();
      myRob.createLineGroup();
      int vidx = 0;
      for (int i=0; i<size(); i++) {
         AxialSpring spr = get(i);
         myRob.addPosition (spr.myPnt0.myRenderCoords);
         myRob.addVertex (vidx);
         myRob.addPosition (spr.myPnt1.myRenderCoords);
         myRob.addVertex (vidx+1);
         if (spr.getRenderProps() == null) {
            myRob.lineGroup (spr.isSelected() ? SEL_GRP : REG_GRP);
            myRob.addLine (vidx, vidx+1);            
         }
         vidx += 2;
      }
   }

   protected void updateRenderObject() {

     // delete old line groups, keep old vertices
      myRob.clearPrimitives ();
      myRob.createLineGroup(); // regular
      myRob.createLineGroup(); // selected
      int vidx = 0;
      for (int i=0; i<size(); i++) {
         AxialSpring spr = get(i);
         if (spr.getRenderProps() == null) {
            myRob.lineGroup (spr.isSelected() ? SEL_GRP : REG_GRP);
            myRob.addLine (vidx, vidx+1);            
         }
         vidx += 2;
      }
   }

   protected boolean renderObjectValid() {
      int kSel = 0;
      int kReg = 0;
      int numReg = 2*myRob.numLines(REG_GRP);
      int numSel = 2*myRob.numLines(SEL_GRP);
      int[] viReg = myRob.getLines (REG_GRP);
      int[] viSel = myRob.getLines (SEL_GRP);
      int vidx = 0;
      for (int i=0; i<size(); i++) {
         AxialSpring spr = get(i);
         if (spr.getRenderProps() == null) {
            if (spr.isSelected()) {
               if (kSel >= numSel) {
                  return false;
               }
               if (viSel[kSel++] != vidx || viSel[kSel++] != vidx+1) {
                  return false;
               }
            }
            else {
               if (kReg >= numReg) {
                  return false;
               }
               if (viReg[kReg++] != vidx || viReg[kReg++] != vidx+1) {
                  return false;
               }
            }
         }
         vidx += 2;
      }
      if (kSel != numSel || kReg != numReg) {
         return false;
      }
      return true;
   }

   public void prerender (RenderList list) {
      if (myRob == null) {
         buildRenderObject();
      }
      else if (!renderObjectValid()) {
         updateRenderObject();
      }
      myRob.notifyPositionsModified();      
      for (int i=0; i<size(); i++) {
         PointSpringBase spr = get (i);
         if (spr.getRenderProps() != null) {
            list.addIfVisible (spr);
         }
         else {
            // spring will be rendered directly by this list, but call
            // prerender directly because we may still need to set things there
            spr.prerender (list);
         }
      }
   }

   private void drawLines (
      Renderer renderer, RenderProps props, boolean selected) {
   
      LineStyle style = props.getLineStyle();
      Shading savedShading = renderer.setLineShading(props);
      renderer.setLineColoring (props, selected);
      switch (style) {
         case LINE: {
            int width = props.getLineWidth();
            if (width > 0) {
               //renderer.setLightingEnabled (false);
               //renderer.setColor (props.getLineColorArray(), selected);
               renderer.drawLines (myRob, LineStyle.LINE, width);
               //renderer.setLightingEnabled (true);
            }
            break;
         }
         case SPINDLE:
         case SOLID_ARROW:
         case CYLINDER: {
            double rad = props.getLineRadius();
            if (rad > 0) {
               //Shading savedShading = renderer.getShadeModel();
               //renderer.setLineLighting (props, selected);
               renderer.drawLines (myRob, style, rad);
               //renderer.setShadeModel(savedShading);
            }
            break;
         }
      }
      renderer.setShading(savedShading);
   }

   public void render (Renderer renderer, int flags) {
      render (renderer, myRenderProps, flags);
   }
   
   public void render (Renderer renderer, RenderProps props, int flags) {
      if (renderer.isSelecting()) {
         LineStyle style = props.getLineStyle();
         if (style == LineStyle.LINE) {
            int width = props.getLineWidth();
            if (width > 0) {
               renderer.setLineWidth (width);
            }
            else {
               return;
            }
         }
         double rad = props.getLineRadius();
         for (int i=0; i<size(); i++) {
            AxialSpring spr = get(i);        
            if (spr.getRenderProps() == null && renderer.isSelectable (spr)) {
               float[] v0 = spr.myPnt0.myRenderCoords;
               float[] v1 = spr.myPnt1.myRenderCoords;
               renderer.beginSelectionQuery (i);
               switch (style) {
                  case LINE: {
                     renderer.drawLine (v0, v1);
                     break;
                  }
                  case SPINDLE: {
                     renderer.drawSpindle (v0, v1, rad);
                     break;
                  }
                  case SOLID_ARROW: {
                     renderer.drawArrow (
                        v0, v1, rad, /*capped=*/true);
                     break;
                  }
                  case CYLINDER: {
                     renderer.drawCylinder (v0, v1, rad, /*capped=*/false);
                     break;
                  }
               }
               renderer.endSelectionQuery ();
            }
         }
         if (style == LineStyle.LINE) {
            renderer.setLineWidth (1);
         }
      }
      else if (myRob != null) {
         int numReg = myRob.numLines(REG_GRP);
         int numSel = myRob.numLines(SEL_GRP);

         if (numReg > 0) {
            myRob.lineGroup (REG_GRP);
            drawLines (renderer, props, /*selected=*/false);
         }
         if (numSel > 0) {
            myRob.lineGroup (SEL_GRP);
            drawLines (renderer, props, /*selected=*/true);
         }
      }
   }

   // public void prerender (RenderList list) {
   //    for (int i=0; i<size(); i++) {
   //       PointSpringBase spr = get (i);
   //       if (spr.getRenderProps() != null) {
   //          list.addIfVisible (spr);
   //       }
   //       else {
   //          // spring will be rendered directly by this list, but call
   //          // prerender directly because we may still need to set things there
   //          spr.prerender (list);
   //       }
   //    }
   // }

   // public void render (Renderer renderer, int flags) {
   //    renderer.drawLines (myRenderProps, iterator());
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

   public void getSelection (LinkedList<Object> list, int qid) {
      if (qid >= 0 && qid < size()) {
         list.addLast (get (qid));
      }
   }

   public void notifyParentOfChange (ComponentChangeEvent e) {
      if (e instanceof StructureChangeEvent) {
         myRob = null;
      }
      super.notifyParentOfChange (e);
   }

}
