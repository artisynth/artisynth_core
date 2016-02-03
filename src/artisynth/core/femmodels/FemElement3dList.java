/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import java.util.LinkedList;

import javax.media.opengl.GL2;

import maspack.properties.PropertyList;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;
import maspack.render.RenderList;
import maspack.render.Material;
import maspack.render.RenderProps;
import maspack.render.RenderProps.LineStyle;
import maspack.render.Renderer;
import maspack.render.RenderObject;
import maspack.render.GL.GL2.GL2Viewer;
import artisynth.core.modelbase.*;
import artisynth.core.util.ClassAliases;

public class FemElement3dList extends RenderableComponentList<FemElement3d> {
   protected static final long serialVersionUID = 1;

   private RenderObject myRob = null;
   private byte[] myRobFlags = null;

   private static int DEFAULT_ELEMENT_WIDGET_SIZE = 0;
   private static PropertyMode DEFAULT_ELEMENT_WIDGET_SIZE_MODE = PropertyMode.Inherited;
   double myElementWidgetSize = DEFAULT_ELEMENT_WIDGET_SIZE;
   PropertyMode myElementWidgetSizeMode = DEFAULT_ELEMENT_WIDGET_SIZE_MODE;
   
   static PropertyList myProps = new PropertyList(FemElement3dList.class, RenderableComponentList.class);
   static {
         myProps.addInheritable (
            "elementWidgetSize:Inherited",
            "size of rendered widget in each element's center",
            DEFAULT_ELEMENT_WIDGET_SIZE, "[0,1]");
   }
   @Override
   public PropertyList getAllPropertyInfo() {
      return myProps;
   }
   
   public void setElementWidgetSize (double size) {
      myElementWidgetSize = size;
      myElementWidgetSizeMode = 
         PropertyUtils.propagateValue (
            this, "elementWidgetSize",
            myElementWidgetSize, myElementWidgetSizeMode);
   }

   public double getElementWidgetSize () {
      return myElementWidgetSize;
   }

   public void setElementWidgetSizeMode (PropertyMode mode) {
      myElementWidgetSizeMode =
         PropertyUtils.setModeAndUpdate (
            this, "elementWidgetSize", myElementWidgetSizeMode, mode);
   }

   public PropertyMode getElementWidgetSizeMode() {
      return myElementWidgetSizeMode;
   }
   
   public FemElement3dList () {
      this (null, null);
   }

   public FemElement3dList (String name, String shortName) {
      super (FemElement3d.class, name, shortName);
      setRenderProps (createRenderProps());
   }
   
   public boolean hasParameterizedType() {
      return false;
   }   

   // public FemElement3dList (String name, String shortName,
   // CompositeComponent parent)
   // {
   // super (FemElement3d.class, name, shortName, parent);
   // setRenderProps(createRenderProps());
   // }

   /* ======== Renderable implementation ======= */

   public RenderProps createRenderProps() {
      return RenderProps.createLineFaceProps (this);
   }

   boolean addElementsInPrerender = true;

   private ComponentList<? extends FemNode> getNodeList () {
      if (getParent() instanceof FemModel3d) {
         return ((FemModel3d)getParent()).getNodes();
      }
      else {
         return null;
      }
   }

   private static final int REG_GRP = 0;
   private static final int SEL_GRP = 1;
   private static final int INV_GRP = 2;
   private static final int GRP_MASK = 0x3;
   private static final int HAS_WIDGET = 0x80;
   private static final int IS_LIST_RENDERED = 0x40;

   protected void addEdgeLines (
      RenderObject r, FemElement3d elem,
      ComponentList<? extends FemNode> nodes) {
      
      int[] idxs = elem.getEdgeIndices();
      FemNode[] enodes = elem.getNodes();
      int n = 2;
      for (int i=0; i<idxs.length; i+=(n+1)) {
         n = idxs[i];
         if (n == 2) {
            int vidx0 = nodes.indexOf(enodes[idxs[i+1]]);
            int vidx1 = nodes.indexOf(enodes[idxs[i+2]]);
            r.addLine (vidx0, vidx1);
         }
      }
   }

   protected void addWidget (RenderObject r, FemElement3d elem) {
      FemNode[] enodes = elem.getNodes();
      int p0idx = r.numPositions();
      for (int j=0; j<enodes.length; j++) {
         r.addPosition (0, 0, 0);
      }
      int[] fidxs = FemUtilities.triangulateFaceIndices (
         elem.getFaceIndices());
      int nidx = r.numNormals(); // normal index
      for (int i=0; i<fidxs.length; i += 3) {
         r.addNormal (0, 0, 0);
         int v0idx = r.addVertex (p0idx+fidxs[i  ], nidx);
         int v1idx = r.addVertex (p0idx+fidxs[i+1], nidx);
         int v2idx = r.addVertex (p0idx+fidxs[i+2], nidx);
         r.addTriangle (v0idx, v1idx, v2idx);
         nidx++;
      }      
      updateWidgetPos (r, elem, p0idx);
   }

   protected int updateWidgetPos (RenderObject r, FemElement3d elem, int idx) {
      FemNode[] enodes = elem.getNodes();

      // compute center point
      float cx = 0;
      float cy = 0;
      float cz = 0;
      for (int j=0; j<enodes.length; j++) {
         float[] coords = enodes[j].myRenderCoords;
         cx += coords[0];
         cy += coords[1];
         cz += coords[2];
      }
      cx /= enodes.length;
      cy /= enodes.length;
      cz /= enodes.length;

      float ws = (float)elem.getElementWidgetSize();
      for (int j=0; j<enodes.length; j++) {
         float[] coords = enodes[j].myRenderCoords;
         float dx = coords[0]-cx;
         float dy = coords[1]-cy;
         float dz = coords[2]-cz;
         r.setPosition (idx++, cx+ws*dx, cy+ws*dy, cz+ws*dz);
      }
      return idx;
   }

   private byte getRobFlags (FemElement3d elem) {
      if (elem.getRenderProps() != null) {
         return (byte)0;
      }
      else {
         byte flags = IS_LIST_RENDERED;
         if (elem.isSelected()) {
            flags |= SEL_GRP;
         }
         else if (elem.isInverted()) {
            flags |= INV_GRP;
         }
         if (elem.getElementWidgetSize() > 0) {
            flags |= HAS_WIDGET;
         }
         return flags;
      }
   }

   protected void buildRenderObject() {

      RenderObject r = new RenderObject();
      r.createLineGroup();
      r.createLineGroup();
      r.createTriangleGroup();
      r.createTriangleGroup();
      r.createTriangleGroup();
      int vidx = 0;

      ComponentList<? extends FemNode> nodes = getNodeList();
      if (nodes == null) {
         // XXX what to do?
         return;
      }
      r.addNormal (1, 0, 0); // dummy normal for lines
      for (int i=0; i<nodes.size(); i++) { 
         FemNode node = nodes.get(i);
         r.addPosition (node.myRenderCoords);            
         r.addVertex (i, 0);
      }
      myRobFlags = new byte[size()];
      boolean hasWidgets = false;
      for (int i=0; i<size(); i++) {
         FemElement3d elem = get (i);
         byte flags = getRobFlags(elem);
         if ((flags & IS_LIST_RENDERED) != 0) {
            int group = (flags & GRP_MASK);
            r.lineGroup (group == INV_GRP ? REG_GRP : group);
            addEdgeLines (r, elem, nodes);
            if ((flags & HAS_WIDGET) != 0) {
               r.triangleGroup (group);
               addWidget (r, elem);
               hasWidgets = true;
            }
            myRobFlags[i] = flags;            
         }
      }
      r.setPositionsDynamic (true);
      if (hasWidgets) {
         r.setNormalsDynamic (true);
      }
      r.commit();
      myRob = r;
   }

   private boolean selectionChanged () {
      for (int i = 0; i < size(); i++) {
         FemElement3d elem = get(i);
         if (getRobFlags(elem) != myRobFlags[i]) {
            return true;
         }
      }
      return false;
   }

   private static final int BUILD = 1;
   private static final int COLOR_UPDATE = 2;

   private int renderObjectNeedsUpdating () {
      if (myRob == null) {
         return BUILD;
      }
      int rcode = 0;
      for (int i = 0; i < size(); i++) {
         byte flags = getRobFlags(get(i));
         if (flags != myRobFlags[i]) {
            // if IS_LIST_RENDERED or HAS_WIDGET differ, need to rebuild.
            // Otherwise, just a color change, and can just rebuilt the groups
            if ((flags | ~GRP_MASK) != (myRobFlags[i] | ~GRP_MASK)) {
               return BUILD;
            }
            else {
               rcode = COLOR_UPDATE;
            }
         }
      }
      return rcode;
   }

   public void prerenderNew (RenderList list) {

      ComponentList<? extends FemNode> nodes = getNodeList();
      if (nodes == null) {
        // XXX what to do?
        return;
      }

      // add elements with their own renderProps inside prerender, as
      // usual. We add the selected elements first, so that they
      // will render first and be more visible.
      for (int i = 0; i < size(); i++) {
         FemElement3d elem = get (i);
         if (elem.isSelected() && elem.getRenderProps() != null) {
            list.addIfVisible (elem);
         }
      }
      for (int i = 0; i < size(); i++) {
         FemElement3d elem = get (i);
         if (!elem.isSelected() && elem.getRenderProps() != null) {
            list.addIfVisible (elem);
         }
      }

      int update = renderObjectNeedsUpdating();
      if (update != 0) {
         // for now just rebuild, even if only colors need changing
         buildRenderObject();
      }
      int pidx = nodes.size();
      for (int i=0; i<size(); i++) {
         FemElement3d elem = get (i);
         byte flags = getRobFlags(elem);
         if ((flags & HAS_WIDGET) != 0) {
            pidx = updateWidgetPos (myRob, elem, pidx);
         }
      }

      FemElementRenderer.computeTriangleNormals (myRob, REG_GRP);
      FemElementRenderer.computeTriangleNormals (myRob, SEL_GRP);
      FemElementRenderer.computeTriangleNormals (myRob, INV_GRP);
      myRob.notifyPositionsModified();      
   }  

   protected void drawEdges (
      Renderer renderer, RenderProps props, int group) {

      RenderObject r = myRob;
      if (r.numLines(group) > 0) {
         r.lineGroup (group);
         int width = props.getLineWidth();
         if (width > 0) {
            boolean selected = (group == SEL_GRP);
            renderer.setLightingEnabled (false);
            renderer.setColor (props.getLineColorArray(), selected);
            renderer.drawLines (myRob, LineStyle.LINE, width);
            renderer.setLightingEnabled (true);
         }
      }
   }

   protected void drawWidgets (
      Renderer renderer, RenderProps props, int group) {

      RenderObject r = myRob;
      if (r.numTriangles(group) > 0) {
         r.triangleGroup (group);
         Material mat = props.getFaceMaterial();
         boolean selected = (group == SEL_GRP);
         if (group == INV_GRP) {
            mat = FemModel3d.myInvertedMaterial;
         }
         renderer.setMaterial (mat, selected);
         renderer.drawTriangles (r);
      }
   }

   public void renderNew (Renderer renderer, int flags) {
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
            FemElement3d elem = get(i);        
            if (elem.getRenderProps() == null && renderer.isSelectable (elem)) {
               renderer.beginSelectionQuery (i);
               elem.render (renderer, myRenderProps, flags);
               renderer.endSelectionQuery ();
            }
         }
         if (style == LineStyle.LINE) {
            renderer.setLineWidth (1);
         }
      }
      else if (myRob != null) {
         drawEdges (renderer, props, SEL_GRP);
         drawEdges (renderer, props, REG_GRP);
         drawWidgets (renderer, props, SEL_GRP);
         drawWidgets (renderer, props, REG_GRP);
         drawWidgets (renderer, props, INV_GRP);
      }
   }

   public void renderOld (Renderer renderer, int flags) {
      // GL2 gl = renderer.getGL2().getGL2();
      // if (renderer.isSelecting()) {
      //    gl.glPushName (-1);
      // }
      dorender (renderer, /*selected=*/true);
      dorender (renderer, /*selected=*/false);
      // if (renderer.isSelecting()) {
      //    gl.glPopName();
      // }
   }

   public void prerender (RenderList list) {
      prerenderNew (list);
   }

   public void render (Renderer renderer, int flags) {
      renderNew (renderer, flags);
   }


   // protected void updateRenderObject() {

   //    RenderObject r = myRob;
   //    ComponentList<? extends FemNode> nodes = getNodeList();
   //    if (nodes == null) {
   //       // XXX what to do?
   //       return;
   //    }

   //   // delete old line groups, keep old vertices
   //    r.clearPrimitives ();
   //    r.createLineGroup(); // regular
   //    r.createLineGroup(); // selected
   //    for (int i=0; i<size(); i++) {
   //       FemElement3d elem = get (i);
   //       if (elem.getRenderProps() == null) {
   //          r.lineGroup (elem.isSelected() ? SEL_GRP : REG_GRP);
   //          addEdgeLines (r, elem, nodes);
   //       }
   //       myRobFlags[i] = getRobFlags(elem);
   //    }
   // }

   public void prerenderOld (RenderList list) {
      if (addElementsInPrerender) {
         // add elements with their own renderProps inside prerender, as
         // usual. We add the selected elements first, so that they
         // will render first and be more visible.
         for (int i = 0; i < size(); i++) {
            FemElement3d elem = get (i);
            if (elem.isSelected() && elem.getRenderProps() != null) {
               list.addIfVisible (elem);
            }
         }
         for (int i = 0; i < size(); i++) {
            FemElement3d elem = get (i);
            if (!elem.isSelected() && elem.getRenderProps() != null) {
               list.addIfVisible (elem);
            }
         }
      }
   }

   public boolean rendersSubComponents() {
      return true;
   }

   private void dorender (Renderer renderer, boolean selected) {

      boolean selecting = renderer.isSelecting();
      if (!addElementsInPrerender) {
         // we render all elements ourselves, taking care to render selected
         // elements first. This provides the maximum visibility for selected
         // elements, but means the transparency won't work properly.
         for (int i = 0; i < size(); i++) {
            FemElement3d elem = get (i);
            if (elem.isSelected() == selected &&
                elem.getRenderProps() != null &&
                elem.getRenderProps().isVisible()) {
               if (selecting) {
                  if (renderer.isSelectable (elem)) {
                     renderer.beginSelectionQuery (i);
                     elem.render (renderer, 0);
                     renderer.endSelectionQuery ();
                  }
               }
               else {            
                  elem.render (renderer, 0);
               }
            }
         }
      }

      if ( (myRenderProps.getLineWidth() > 0 &&
            myRenderProps.getLineStyle() == LineStyle.LINE) ||
         (myRenderProps.getLineRadius() > 0 &&
          myRenderProps.getLineStyle() == LineStyle.CYLINDER)) {
         switch (myRenderProps.getLineStyle()) {
            case LINE: {
               if (!selecting) {
                  renderer.setLightingEnabled (false);
               }
               // draw regular points first
               renderer.setLineWidth (myRenderProps.getLineWidth());
               renderer.setColor (myRenderProps.getLineColorArray(), false);
               for (int i = 0; i < size(); i++) {
                  FemElement3d elem = get (i);
                  if (elem.getRenderProps() == null &&
                      elem.isSelected() == selected) {
                     if (selecting) {
                        if (renderer.isSelectable (elem)) {
                           renderer.beginSelectionQuery (i);
                           elem.renderEdges (renderer, myRenderProps);
                           renderer.endSelectionQuery ();
                        }
                        
                     }
                     else {
                        renderer.updateColor (
                           myRenderProps.getLineColorArray(), elem.isSelected());
                        elem.renderEdges (renderer, myRenderProps);
                     }
                  }
               }
               renderer.setLineWidth (1);
               if (!selecting) {
                  renderer.setLightingEnabled (true);
               }
               break;
            }
            case CYLINDER: {
               // GLU glu = renderer.getGLU();
               renderer.setMaterialAndShading (
                  myRenderProps, myRenderProps.getLineMaterial(), false);
               for (int i = 0; i < size(); i++) {
                  FemElement3d elem = get (i);
                  if (elem.getRenderProps() == null &&
                      elem.isSelected() == selected) {
                     if (selecting) {
                        if (renderer.isSelectable (elem)) {
                           renderer.beginSelectionQuery (i);
                           elem.renderEdges (renderer, myRenderProps);
                           renderer.endSelectionQuery ();
                        }
                     }
                     else {
                        maspack.render.Material mat = 
                           myRenderProps.getLineMaterial();
                        renderer.updateMaterial (
                           myRenderProps, mat, null, elem.isSelected());
                        elem.renderEdges (renderer, myRenderProps);
                     }
                  }
               }
               renderer.restoreShading (myRenderProps);
               break;
            }
            default:
               break;
         }
      }
      renderer.setMaterialAndShading (
         myRenderProps, myRenderProps.getFaceMaterial(), false);
      for (int i = 0; i < size(); i++) {
         // double widgetSize;
         FemElement3d elem = get (i);
         if (elem.getRenderProps() == null &&
             elem.isSelected() == selected &&
             elem.getElementWidgetSize() > 0) {
            if (selecting) {
               if (renderer.isSelectable (elem)) {
                  renderer.beginSelectionQuery (size()+i);
                  elem.renderWidget (renderer, myRenderProps);
                  renderer.endSelectionQuery ();
               }
            }
            else {
               maspack.render.Material mat = myRenderProps.getFaceMaterial();
               if (elem.isInverted()) {
                  mat = FemModel3d.myInvertedMaterial;
               }
               renderer.updateMaterial (myRenderProps, mat, elem.isSelected());
               elem.renderWidget (renderer, myRenderProps);
            }
         }
      }
      renderer.restoreShading (myRenderProps);
   }

   /**
    * {@inheritDoc}
    */
   public boolean isSelectable() {
      return true;
   }

   public int numSelectionQueriesNeeded() {
      // May two selection queries per element, cases where we are rendering
      // the element's edges and widget in separate passes.
      return 2*size();
   }

   public void getSelection (LinkedList<Object> list, int qid) {
      if (qid >= 0 && qid < 2*size()) {
         // John Lloyd, April 13 2015: don't want to select the FEM as well
//         CompositeComponent p = getParent();
//         if (p instanceof GLRenderable) {
//            if (!list.contains(p)) {
//               list.addLast((GLRenderable)p);
//            }
//         }
         list.addLast (get (qid%size()));
      }
   }
   
   public FemElement3d newComponent (String classId)
      throws InstantiationException, IllegalAccessException {
      if (classId == null) {
         return null;
      }
      else {
         return (FemElement3d)ClassAliases.newInstance (
            classId, FemElement3d.class);
      }
   }
}
