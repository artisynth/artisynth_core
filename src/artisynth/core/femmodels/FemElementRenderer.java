package artisynth.core.femmodels;

import java.util.ArrayList;
import maspack.util.*;
import maspack.matrix.*;
import maspack.render.*;
import maspack.render.Renderer.Shading;

public class FemElementRenderer {

   private RenderObject myRob;
   private int myNumEdgePos; // number of positions used for edges

   // number of edge segments (between any two nodes) that should to be used to
   // render the edges of a quadratic element.
   static final int numQuadEdgeSegs = 5;
   ArrayList<QuadEdge> myQuadEdges = new ArrayList<QuadEdge>();

   public FemElementRenderer (FemElement3d elem) {
      buildRenderObject (elem);
   }

   // Utility functions that are also used elsewhere ...

   public static int addQuadEdge (
      RenderObject r, int vidx0, int vidxm, int vidx1) {

      int nsegs = numQuadEdgeSegs;
      
      int pidx0 = (nsegs > 1 ? r.numPositions() : -1);
      int[] vidxs = new int[2*nsegs+1];
      vidxs[0] = vidx0;
      for (int i=1; i<nsegs; i++) {
         vidxs[i] = r.vertex (0f, 0f, 0f);
      }
      vidxs[nsegs] = vidxm;
      for (int i=nsegs+1; i<2*nsegs; i++) {
         vidxs[i] = r.vertex (0f, 0f, 0f);
      }
      vidxs[2*nsegs] = vidx1;
      r.addLineStrip (vidxs);
      return pidx0;
   }

   public static void updateQuadEdge (
      RenderObject r, int vidx0, int vidxm, int vidx1, int pidx0) {

      int nsegs = numQuadEdgeSegs;
      if (nsegs > 1) {
         float[] pos0 = r.getPosition (vidx0);
         float[] posm = r.getPosition (vidxm);
         float[] pos1 = r.getPosition (vidx1);
         int k = pidx0;
         for (int i=1; i<2*nsegs; i++) {
            if (i != nsegs) {
               float[] pos = r.getPosition (k++);
               float s = (i-nsegs)/(float)nsegs;
               float w0 = 0.5f*(s-1f)*s;
               float wm = (1f-s*s);
               float w1 = 0.5f*(s+1f)*s;
               pos[0] = w0*pos0[0] + wm*posm[0] + w1*pos1[0];
               pos[1] = w0*pos0[1] + wm*posm[1] + w1*pos1[1];
               pos[2] = w0*pos0[2] + wm*posm[2] + w1*pos1[2];
            }
         }
      }
   }

   // public static void addWidgetFaces (RenderObject r, FemElement3d elem) {
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
   // }

   public static void addWidgetFaces (RenderObject r, FemElement3d elem) {
      int p0idx = r.numPositions();
      // add positions for storing widget vertices
      for (int j=0; j<elem.numNodes(); j++) {
         r.addPosition (0, 0, 0);
      }
      // get the triangle indices for the faces associated with the element
      int[] fidxs = FemUtilities.triangulateFaceIndices (
         elem.getFaceIndices());
      int nidx = r.numNormals(); // normal index
      for (int i=0; i<fidxs.length; i += 3) {
         r.addNormal (0, 0, 0); // add face normal
         int v0idx = r.addVertex (p0idx+fidxs[i  ], nidx);
         int v1idx = r.addVertex (p0idx+fidxs[i+1], nidx);
         int v2idx = r.addVertex (p0idx+fidxs[i+2], nidx);
         r.addTriangle (v0idx, v1idx, v2idx);
         nidx++;
      }      
      double size = elem.getElementWidgetSize();
      FemElementRenderer.updateWidgetPositions (r, elem, size, p0idx);
   }


   public static int updateWidgetPositions (
      RenderObject r, FemElement3d elem, double size, int idx) {

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

      float s = (float)size;
      for (int j=0; j<enodes.length; j++) {
         float[] coords = enodes[j].myRenderCoords;
         float dx = coords[0]-cx;
         float dy = coords[1]-cy;
         float dz = coords[2]-cz;
         r.setPosition (idx++, cx+s*dx, cy+s*dy, cz+s*dz);
      }
      return idx;
   }

   public static void updateWidgetNormals (RenderObject r, int tgrp) {
      int numt = r.numTriangles(tgrp);

      if (numt > 0) {
         int[] vidxs = r.getTriangles(tgrp);
         int k = 0;
         for (int i=0; i<numt; i++) {
            float[] nrm = r.getVertexNormal (vidxs[k]);
            float[] p0 = r.getVertexPosition (vidxs[k++]);
            float[] p1 = r.getVertexPosition (vidxs[k++]);
            float[] p2 = r.getVertexPosition (vidxs[k++]);


            float ax = p1[0]-p0[0];
            float ay = p1[1]-p0[1];
            float az = p1[2]-p0[2];
            float bx = p2[0]-p0[0];
            float by = p2[1]-p0[1];
            float bz = p2[2]-p0[2];
            float nx = ay*bz-az*by;
            float ny = az*bx-ax*bz;
            float nz = ax*by-ay*bx;
            // Note: no need to normalize normals ....
            nrm[0] = nx;
            nrm[1] = ny;
            nrm[2] = nz;
         }
         r.notifyNormalsModified();
      }
   }

   private class QuadEdge {
      
      int myVidx0;
      int myVidx1;    
      int myVidxm;
      int myPidx0;

      public QuadEdge (int vidx0, int vidxm, int vidx1) {
         myVidx0 = vidx0;
         myVidxm = vidxm;
         myVidx1 = vidx1;
      }

      public void addCurve (RenderObject r) {
         myPidx0 = addQuadEdge (r, myVidx0, myVidxm, myVidx1);
      }

      public void updateCurve (RenderObject r) {
         updateQuadEdge (r, myVidx0, myVidxm, myVidx1, myPidx0);
      }
   }

   void buildRenderObject (FemElement3d elem) {

      RenderObject r = new RenderObject();
      
      r.addNormal (1, 0, 0); // dummy normal for lines
      for (int i=0; i<elem.numNodes(); i++) {
         r.vertex (0, 0, 0);
      }
         
      int[] eidxs = elem.getEdgeIndices();
      int numv = 0;
      for (int i=0; i<eidxs.length; i+=(numv+1)) {
         numv = eidxs[i];
         if (numv == 2) {
            int vidx0 = eidxs[i+1];
            int vidx1 = eidxs[i+2];
            r.addLine (vidx0, vidx1);
         }
         else if (numv == 3) {
            int vidx0 = eidxs[i+1];
            int vidxm = eidxs[i+2];
            int vidx1 = eidxs[i+3];
            // r.addLine (vidx0, vidxm);
            // r.addLine (vidxm, vidx1);
            QuadEdge quad = new QuadEdge (vidx0, vidxm, vidx1);
            quad.addCurve (r);
            myQuadEdges.add (quad);
         }
      }
      myNumEdgePos = r.numPositions();
      addWidgetFaces (r, elem);

      myRob = r;
   }

   void updateEdgePositions (RenderObject r, FemElement3d elem) {

      FemNode[] nodes = elem.getNodes();
      for (int i=0; i<nodes.length; i++) {
         r.setPosition (i, nodes[i].myRenderCoords);
      }
      if (myQuadEdges.size() > 0) {
         for (int i=0; i<myQuadEdges.size(); i++) {
            myQuadEdges.get(i).updateCurve (r);
         }
      }
      r.notifyPositionsModified();
   }

   void renderWidget (
      Renderer renderer, FemElement3d elem, double size, RenderProps props) {

      RenderObject r = myRob;      

      updateWidgetPositions (r, elem, size, myNumEdgePos);
      updateWidgetNormals (r, /*group=*/0);

      if (!renderer.isSelecting()) {
         float[] color = props.getFaceColorF();
         if (elem.isInverted()) {
            color = FemModel3d.myInvertedColor;
         }
         renderer.setFaceColoring (props, color, elem.isSelected());
      }
      renderer.drawTriangles (r);
   }

   void render (
      Renderer renderer, FemElement3d elem, RenderProps props) {

      RenderObject r = myRob;      
      updateEdgePositions (r, elem);

      Shading savedShading = renderer.setShading (Shading.NONE);
      renderer.setLineWidth (props.getLineWidth());
      renderer.setLineColoring (props, elem.isSelected());
      renderer.drawLines (r);
      renderer.setLineWidth (1);
      renderer.setShading (savedShading);

      double wsize = elem.getElementWidgetSize();
      if (wsize > 0) {
         renderWidget (renderer, elem, wsize, props);
      }
   }
}

