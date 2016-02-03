package artisynth.core.femmodels;

import maspack.util.*;
import maspack.matrix.*;
import maspack.render.*;
import maspack.render.RenderProps.Shading;

public class FemElementRenderer {

   private RenderObject myRob;
   private AffineTransform3d myX;

   public FemElementRenderer (FemElement3d elem) {
      buildRenderObject (elem);
      myX = new AffineTransform3d();
   }

   static void addPositions (RenderObject r, int n) {

      for (int i=0; i<n; i++) {
         r.addPosition (0, 0, 0);
      }
   }

   void buildRenderObject (FemElement3d elem) {

      RenderObject r = new RenderObject();
      for (int i=0; i<elem.numNodes(); i++) {
         r.addPosition (0, 0, 0);
      }
      int[] fidxs = FemUtilities.triangulateFaceIndices (
         elem.getFaceIndices());
         
      r.addNormal (1, 0, 0); // dummy normal for lines
      r.setPositionsDynamic (true);
      r.setNormalsDynamic (true);

      int[] eidxs = elem.getEdgeIndices();
      int numv = 0;
      for (int i=0; i<eidxs.length; i+=(numv+1)) {
         numv = eidxs[i];
         int[] vidxs = new int[numv];
         int k = i+1;
         for (int j=0; j<numv; j++) {
            vidxs[j] = r.addVertex (eidxs[k++], 0);
         }
         r.addLineStrip (vidxs);
      }
      int nidx = r.numNormals(); // normal index
      for (int i=0; i<fidxs.length; i += 3) {
         r.addNormal (0, 0, 0);
         int v0idx = r.addVertex (fidxs[i  ], nidx);
         int v1idx = r.addVertex (fidxs[i+1], nidx);
         int v2idx = r.addVertex (fidxs[i+2], nidx);
         r.addTriangle (v0idx, v1idx, v2idx);
         nidx++;
      }
      r.commit();
      myRob = r;
   }

   void updatePositions (RenderObject r, FemElement3d elem) {

      FemNode[] nodes = elem.getNodes();
      for (int i=0; i<nodes.length; i++) {
         r.setPosition (i, nodes[i].myRenderCoords);
      }
   }

   public static void computeTriangleNormals (RenderObject r, int tgrp) {
      int numt = r.numTriangles(tgrp);
      for (int i=0; i<numt; i++) {
         int[] idxs = r.getTriangle (tgrp, i);
         float[] p0 = r.getVertexPosition (idxs[0]);
         float[] p1 = r.getVertexPosition (idxs[1]);
         float[] p2 = r.getVertexPosition (idxs[2]);
         float[] nrm = r.getVertexNormal (idxs[0]);

         float ax = p1[0]-p0[0];
         float ay = p1[1]-p0[1];
         float az = p1[2]-p0[2];
         float bx = p2[0]-p0[0];
         float by = p2[1]-p0[1];
         float bz = p2[2]-p0[2];
         float nx = ay*bz-az*by;
         float ny = az*bx-ax*bz;
         float nz = ax*by-ay*bx;
         float mag = (float)Math.sqrt (nx*nx + ny*ny + nz*nz);
         if (mag > 0) {
            nx /= mag;
            ny /= mag;
            nz /= mag;
         }
         nrm[0] = nx;
         nrm[1] = ny;
         nrm[2] = nz;
      }
      r.notifyNormalsModified();
   }

   void updateNormals (RenderObject r, FemElement3d elem) {

      computeTriangleNormals (r, /*triangle group=*/0);
   }

   void render (
      Renderer renderer, FemElement3d elem, RenderProps props) {

      RenderObject r = myRob;      
      updatePositions (r, elem);

      renderer.setLightingEnabled (false);
      renderer.setLineWidth (props.getLineWidth());
      renderer.setColor (props.getLineColorArray(), elem.isSelected());
      renderer.drawLines (r);
      renderer.setLineWidth (1);
      renderer.setLightingEnabled (true);
      
      Shading shading = props.getShading ();
      boolean restoreLighting = false;
      if (shading == Shading.NONE) {
         renderer.setLightingEnabled (false);
         restoreLighting = true;
      }

      double s = elem.getElementWidgetSize();
      if (s > 0) {
         updateNormals (r, elem);

         if (!renderer.isSelecting()) {
            Material mat = props.getFaceMaterial();
            if (elem.isInverted()) {
               mat = FemModel3d.myInvertedMaterial;
            }
            renderer.setMaterial (mat, elem.isSelected());
         }
         if (s != 1.0) {
            float cx = 0;
            float cy = 0;
            float cz = 0;

            // compute the centroid of the nodes
            FemNode[] nodes = elem.getNodes();
            int nnodes = nodes.length;
            for (int i=0; i<nnodes; i++) {
               FemNode n = nodes[i];
               cx += n.myRenderCoords[0];
               cy += n.myRenderCoords[1];
               cz += n.myRenderCoords[2];
            }
            cx /= nnodes;
            cy /= nnodes;
            cz /= nnodes;  

            renderer.pushModelMatrix();
            renderer.translateModelMatrix (cx*(1-s), cy*(1-s), cz*(1-s));
            renderer.scaleModelMatrix (s);
            renderer.drawTriangles (r);
            renderer.popModelMatrix();
         }
         else {
            renderer.drawTriangles (r);
         }
      }
      
      if (restoreLighting) {
         renderer.setLightingEnabled (true);
      }
   }

}

