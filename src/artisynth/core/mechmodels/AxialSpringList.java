/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.awt.Color;
import java.io.*;

import maspack.render.RenderList;
import maspack.render.*;
import maspack.render.Renderable;
import maspack.render.RenderProps.LineStyle;
import maspack.render.RenderProps.Shading;
import maspack.util.*;
import artisynth.core.modelbase.*;
import maspack.render.*;
import maspack.properties.*;
import artisynth.core.util.*;
import java.util.*;

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
      myRob.setPositionsDynamic (true);
      myRob.commit();
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
      int idxSel = 0;
      int idxReg = 0;
      int numReg = myRob.numLines(REG_GRP);
      int numSel = myRob.numLines(SEL_GRP);
      int vidx = 0;
      for (int i=0; i<size(); i++) {
         AxialSpring spr = get(i);
         if (spr.getRenderProps() == null) {
            if (spr.isSelected()) {
               if (idxSel >= numSel) {
                  return false;
               }
               int[] line = myRob.getLine(SEL_GRP,idxSel++);
               if (line[0] != vidx || line[1] != vidx+1) {
                  return false;
               }
            }
            else {
               if (idxReg >= numReg) {
                  return false;
               }
               int[] line = myRob.getLine(REG_GRP,idxReg++);
               if (line[0] != vidx || line[1] != vidx+1) {
                  return false;
               }
            }
         }
         vidx += 2;
      }
      if (idxSel != numSel || idxReg != numReg) {
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
      switch (style) {
         case LINE: {
            int width = props.getLineWidth();
            if (width > 0) {
               renderer.setLightingEnabled (false);
               renderer.setColor (props.getLineColorArray(), selected);
               renderer.drawLines (myRob, LineStyle.LINE, width);
               renderer.setLightingEnabled (true);
            }
            break;
         }
         case ELLIPSOID:
         case SOLID_ARROW:
         case CYLINDER: {
            double rad = props.getLineRadius();
            if (rad > 0) {
               Shading savedShading = renderer.getShadeModel();
               renderer.setLineLighting (props, selected);
               renderer.drawLines (myRob, style, rad);
               renderer.setShadeModel(savedShading);
            }
            break;
         }
      }
   }

   public void render (Renderer renderer, int flags) {
      RenderProps props = myRenderProps;
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
                  case ELLIPSOID: {
                     renderer.drawTaperedEllipsoid (props, v0, v1);
                     break;
                  }
                  case SOLID_ARROW: {
                     renderer.drawSolidArrow (props, v0, v1, /*capped=*/true);
                     break;
                  }
                  case CYLINDER: {
                     renderer.drawCylinder (props, v0, v1);
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
