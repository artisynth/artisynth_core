/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import java.util.LinkedList;
import java.util.HashSet;

import maspack.properties.PropertyList;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.render.RenderObject;
import maspack.render.Renderer.LineStyle;
import maspack.render.Renderer.Shading;
import artisynth.core.modelbase.*;
import artisynth.core.util.ClassAliases;

public class FemElement3dList extends RenderableComponentList<FemElement3d> {
   protected static final long serialVersionUID = 1;

   private RenderObject myEdgeRob = null;
   private RenderObject myWidgetRob = null;
   private HashSet<QuadEdgeDesc> myQuadEdges = null;
   private byte[] myRobFlags = null;

   private static int DEFAULT_ELEMENT_WIDGET_SIZE = 0;
   private static PropertyMode DEFAULT_ELEMENT_WIDGET_SIZE_MODE =
      PropertyMode.Inherited;
   double myElementWidgetSize = DEFAULT_ELEMENT_WIDGET_SIZE;
   PropertyMode myElementWidgetSizeMode = DEFAULT_ELEMENT_WIDGET_SIZE_MODE;

   private class EdgeDesc {
      int myVidx0;
      int myVidx1;
      int myHashCode;

      public EdgeDesc (int vidx0, int vidx1) {
         if (vidx0 > vidx1) {
            myVidx0 = vidx1;
            myVidx1 = vidx0;
         }
         else {
            myVidx0 = vidx0;
            myVidx1 = vidx1;
         }
         myHashCode = 27644437*(myVidx1+myVidx0) + myVidx0;
      }

      @Override
      public int hashCode() {
         return myHashCode;
      }

      @Override
      public boolean equals (Object obj) {
         if (obj instanceof EdgeDesc) {
            EdgeDesc other = (EdgeDesc)obj;
            return myVidx0 == other.myVidx0 && myVidx1 == other.myVidx1;
         }
         else {
            return false;
         }
      }

      @Override
      public String toString() {
         return "(" + myVidx0 + "," + myVidx1 + ")";
      }
   }

   private class QuadEdgeDesc extends EdgeDesc {
      int myVidxm;
      int myPidx0;

      public QuadEdgeDesc (int vidx0, int vidx1) {
         super (vidx0, vidx1);
      }  

      public void addCurve (RenderObject r, int vidxm) {
         myVidxm = vidxm;
         myPidx0 = FemElementRenderer.addQuadEdge (
            r, myVidx0, myVidxm, myVidx1);
      }

      public void updateCurve (RenderObject r) {
         FemElementRenderer.updateQuadEdge (
            r, myVidx0, myVidxm, myVidx1, myPidx0);
      }
   }
   
   static PropertyList myProps =
      new PropertyList(FemElement3dList.class, RenderableComponentList.class);
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
      ComponentList<? extends FemNode> nodes, HashSet<EdgeDesc> edges) {
      
      int[] eidxs = elem.getEdgeIndices();
      FemNode[] enodes = elem.getNodes();
      int numv = 2;
      for (int i=0; i<eidxs.length; i+=(numv+1)) {
         numv = eidxs[i];
         if (numv == 2) {
            int vidx0 = nodes.indexOf(enodes[eidxs[i+1]]);
            int vidx1 = nodes.indexOf(enodes[eidxs[i+2]]);
            EdgeDesc edge = new EdgeDesc (vidx0, vidx1);
            if (!edges.contains (edge)) {
               r.addLine (vidx0, vidx1);
               edges.add (edge);
            }
         }
         else if (numv == 3) {
            int vidx0 = nodes.indexOf(enodes[eidxs[i+1]]);
            int vidx1 = nodes.indexOf(enodes[eidxs[i+3]]);
            QuadEdgeDesc edge = new QuadEdgeDesc (vidx0, vidx1);
            if (myQuadEdges == null) {
               myQuadEdges = new HashSet<QuadEdgeDesc>();
            }
            if (!myQuadEdges.contains (edge)) {
               int vidxm = nodes.indexOf(enodes[eidxs[i+2]]);
               edge.addCurve (r, vidxm);
               myQuadEdges.add (edge);
            }
         }
      }
   }

   // protected void addWidgetFaces (RenderObject r, FemElement3d elem) {
   //    FemNode[] enodes = elem.getNodes();
   //    int p0idx = r.numPositions();
   //    for (int j=0; j<enodes.length; j++) {
   //       r.addPosition (0, 0, 0);
   //    }
   //    int[] fidxs = FemUtilities.triangulateFaceIndices (
   //       elem.getFaceIndices());
   //    int nidx = r.numNormals(); // normal index
   //    for (int i=0; i<fidxs.length; i += 3) {
   //       r.addNormal (0, 0, 0);
   //       int v0idx = r.addVertex (p0idx+fidxs[i  ], nidx);
   //       int v1idx = r.addVertex (p0idx+fidxs[i+1], nidx);
   //       int v2idx = r.addVertex (p0idx+fidxs[i+2], nidx);
   //       r.addTriangle (v0idx, v1idx, v2idx);
   //       nidx++;
   //    }      
   //    double size = elem.getElementWidgetSize();
   //    //FemElementRenderer.updateWidgetPositions (r, elem, size, p0idx);
   // }

   // protected int updateWidgetPos (RenderObject r, FemElement3d elem, int idx) {
   //    FemNode[] enodes = elem.getNodes();

   //    // compute center point
   //    float cx = 0;
   //    float cy = 0;
   //    float cz = 0;
   //    for (int j=0; j<enodes.length; j++) {
   //       float[] coords = enodes[j].myRenderCoords;
   //       cx += coords[0];
   //       cy += coords[1];
   //       cz += coords[2];
   //    }
   //    cx /= enodes.length;
   //    cy /= enodes.length;
   //    cz /= enodes.length;

   //    float ws = (float)elem.getElementWidgetSize();
   //    for (int j=0; j<enodes.length; j++) {
   //       float[] coords = enodes[j].myRenderCoords;
   //       float dx = coords[0]-cx;
   //       float dy = coords[1]-cy;
   //       float dz = coords[2]-cz;
   //       r.setPosition (idx++, cx+ws*dx, cy+ws*dy, cz+ws*dz);
   //    }
   //    return idx;
   // }

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

   protected void buildRenderObjects() {

      RenderObject r = new RenderObject();
      r.createLineGroup();
      r.createLineGroup();
      // r.createTriangleGroup();
      // r.createTriangleGroup();
      // r.createTriangleGroup();
      myQuadEdges = null;
      ComponentList<? extends FemNode> nodes = getNodeList();
      if (nodes == null) {
         // XXX what to do?
         return;
      }

      // create positions and vertices for rendering lines
      for (int i=0; i<nodes.size(); i++) { 
         FemNode node = nodes.get(i);
         r.addVertex (r.addPosition (node.myRenderCoords));
      }
      // allocate per-element flag storage that will be used to determine when
      // the render object needs to be rebuilt
      myRobFlags = new byte[size()];

      boolean hasWidgets = false;
      // for each element, add edge lines, plus widget triangles if the element
      // has a non-zero widget size. Place these additions in the appropriate
      // group (REG_GRP, SEL_GRP,INV_GRP), each of which will be rendered with
      // a different color.
      HashSet<EdgeDesc> edges = new HashSet<EdgeDesc>();
      r.lineGroup (SEL_GRP);
      for (int i=0; i<size(); i++) {
         FemElement3d elem = get (i);
         if (elem.getRenderProps() == null) {
            if (elem.isSelected()) {
               addEdgeLines (r, elem, nodes, edges);
            }
         }
      }
      r.lineGroup (REG_GRP);
      for (int i=0; i<size(); i++) {
         FemElement3d elem = get (i);
         if (elem.getRenderProps() == null) {
            if (!elem.isSelected()) {
               addEdgeLines (r, elem, nodes, edges);
            }
            byte flags = getRobFlags(elem);   
            if ((flags & HAS_WIDGET) != 0) {
               hasWidgets = true;
            }
            myRobFlags[i] = flags;            
         }
      }
      r.setPositionsDynamic (true);
      myEdgeRob = r;

      if (hasWidgets) {
         r = new RenderObject();
         r.createTriangleGroup();
         r.createTriangleGroup();
         r.createTriangleGroup();
         for (int i=0; i<size(); i++) {
            FemElement3d elem = get (i);
            if (elem.getRenderProps() == null) {
               byte flags = getRobFlags(elem);
               if ((flags & HAS_WIDGET) != 0) {
                  r.triangleGroup (flags & GRP_MASK);
                  FemElementRenderer.addWidgetFaces (r, elem);
               }              
            }
         }
         r.setPositionsDynamic (true);
         r.setNormalsDynamic (true);
         myWidgetRob = r;
      }
      else {
         myWidgetRob = null;
      }
   }

   private static final int BUILD = 1;
   private static final int COLOR_UPDATE = 2;

   private int renderObjectsNeedUpdating () {
      if (myEdgeRob == null) {
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

   public void prerender (RenderList list) {

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

      int update = renderObjectsNeedUpdating();
      if (update != 0) {
         // for now just rebuild, even if only colors need changing
         buildRenderObjects();
      }
      if (myWidgetRob != null) {
         RenderObject r = myWidgetRob;
         int pidx = 0;
         for (int i=0; i<size(); i++) {
            FemElement3d elem = get (i);
            byte flags = getRobFlags(elem);
            if ((flags & HAS_WIDGET) != 0) {
               double wsize = elem.getElementWidgetSize();
               pidx = FemElementRenderer.updateWidgetPositions (
                  r, elem, wsize, pidx);
            }
         }
         FemElementRenderer.updateWidgetNormals (r, REG_GRP);
         FemElementRenderer.updateWidgetNormals (r, SEL_GRP);
         FemElementRenderer.updateWidgetNormals (r, INV_GRP);
         r.notifyPositionsModified();      
      }
      if (myQuadEdges != null) {
         for (QuadEdgeDesc quad : myQuadEdges) {
            quad.updateCurve (myEdgeRob);
         }
      }
      myEdgeRob.notifyPositionsModified();
   }  

   protected void drawEdges (
      Renderer renderer, RenderObject r, RenderProps props, int group) {

      if (r.numLines(group) > 0) {
         r.lineGroup (group);
         int width = props.getLineWidth();
         if (width > 0) {
            boolean selected = (group == SEL_GRP);
            Shading savedShading = renderer.setShading (Shading.NONE);
            renderer.setLineColoring (props, selected);
            renderer.drawLines (r, LineStyle.LINE, width);
            renderer.setShading (savedShading);
         }
      }
   }

   protected void drawWidgets (
      Renderer renderer, RenderObject r, RenderProps props, int group) {

      if (r.numTriangles(group) > 0) {
         r.triangleGroup (group);
         float[] color = props.getFaceColorF();
         boolean selected = (group == SEL_GRP);
         if (group == INV_GRP) {
            color = FemModel3d.myInvertedColor;
         }
         renderer.setFaceColoring (props, color, selected);
         renderer.drawTriangles (r);
      }
   }

   public void render (Renderer renderer, int flags) {
      RenderProps props = myRenderProps;
      if (renderer.isSelecting()) {
         // LineStyle style = props.getLineStyle();
         // if (style == LineStyle.LINE) {
         //    int width = props.getLineWidth();
         //    if (width > 0) {
         //       renderer.setLineWidth (width);
         //    }
         //    else {
         //       return;
         //    }
         // }
         for (int i=0; i<size(); i++) {
            FemElement3d elem = get(i);        
            if (elem.getRenderProps() == null && renderer.isSelectable (elem)) {
               renderer.beginSelectionQuery (i);
               elem.render (renderer, myRenderProps, flags);
               renderer.endSelectionQuery ();
            }
         }
         // if (style == LineStyle.LINE) {
         //    renderer.setLineWidth (1);
         // }
      }
      else {
         RenderObject r = myEdgeRob;
         if (r != null) {
            drawEdges (renderer, r, props, SEL_GRP);
            drawEdges (renderer, r, props, REG_GRP);
         }
         r = myWidgetRob;
         if (r != null) {
            drawWidgets (renderer, r, props, SEL_GRP);
            drawWidgets (renderer, r, props, REG_GRP);
            drawWidgets (renderer, r, props, INV_GRP);
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

   public void notifyParentOfChange (ComponentChangeEvent e) {
      if (e instanceof StructureChangeEvent) {
         myEdgeRob = null;
         myWidgetRob = null;
      }
      super.notifyParentOfChange (e);
   }
}
