package artisynth.core.femmodels;

import java.util.ArrayList;
import java.util.List;

import maspack.matrix.Point3d;
import maspack.render.FeatureIndexArray;
import maspack.render.RenderObject;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.render.Renderer.Shading;

public class FemElementRenderer {

   private RenderObject myRob;
   private int myWidgetPos0Idx; // position for widget node 0

   // number of edge segments (between any two nodes) that should to be used to
   // render the edges of a quadratic element.
   static final int numQuadEdgeSegs = 5;
  
   ArrayList<EdgeCurve> myEdges = new ArrayList<EdgeCurve>();
   ArrayList<WidgetFacePatch> myFaces = new ArrayList<WidgetFacePatch>();

   public FemElementRenderer (FemElement3dBase elem) {
      myRob = new RenderObject ();
      addEdges (myRob, null, myEdges, elem, numQuadEdgeSegs);
      myWidgetPos0Idx = myRob.numPositions ();
      addWidgetFaces (myRob, null, myFaces, elem, numQuadEdgeSegs);
   }

   // Utility functions that are also used elsewhere ...

   public static int addQuadEdge (
      RenderObject r, int vidx0, int vidxm, int vidx1) {
      return addQuadEdge(r, null, numQuadEdgeSegs, vidx0, vidxm, vidx1);
   }
   
   public static int addQuadEdge (
      RenderObject r, FeatureIndexArray lines, 
      int vidx0, int vidxm, int vidx1) {
      return addQuadEdge (r, lines, numQuadEdgeSegs, vidx0, vidxm, vidx1);
   }
   
   public static int addQuadEdge (
      RenderObject r, FeatureIndexArray lines, 
      int nsegs, int vidx0, int vidxm, int vidx1) {
      
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
      
      if (lines != null) {
         for (int i=1; i<vidxs.length; ++i) {
            lines.addVertex (vidxs[i-1]);
            lines.addVertex (vidxs[i]);
         }
      }
      
      return pidx0;
   }

   public static void updateQuadEdge (
      RenderObject r, int vidx0, int vidxm, int vidx1, int pidx0) {
      updateQuadEdge (r, numQuadEdgeSegs, vidx0, vidxm, vidx1, pidx0);
   }
   
   public static void updateQuadEdge (
      RenderObject r, int nsegs, int vidx0, int vidxm, int vidx1, int pidx0) {

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
         r.notifyPositionsModified ();
      }
   }
   
   /**
    * Adds quadratic triangular face
    * 
    * @param r render object
    * @param faces list of face indices to populate
    * @param nsegs number of segments along each half-edge
    * @param p0 position index of v0
    * @param p1 position index of v1
    * @param p2 position index of v2
    * @param pm01 position index of the mid vertex between v0 and v1
    * @param pm12 position index of the mid vertex between v1 and v2
    * @param pm20 position index of the mid vertex between v2 and v0
    * @return index of first created vertex
    */
   public static int addQuadFace (
      RenderObject r, FeatureIndexArray faces, int nsegs,
      int p0, int p1, int p2, int pm01, int pm12, int pm20) {
      
      int vidx0 = r.numVertices ();
      
      int[][] vidxs = new int[2*nsegs+1][];

      for (int i=0; i<2*nsegs+1; ++i) {
         vidxs[i] = new int[2*nsegs+1-i];
         for (int j=0; j<2*nsegs+1-i; ++j) {
            // add a normal for each vertex for smooth shading
            r.addNormal (0, 0, 0);
            // vertices
            if (i == 0 && j == 0) {
               vidxs[i][j] = r.addVertex(p0);
            } else if (i == 0 && j == nsegs) {
               vidxs[i][j] = r.addVertex(pm20);
            } else if (i == 0 && j == 2*nsegs) {
               vidxs[i][j] = r.addVertex(p2);
            } else if (i == nsegs && j == 0) {
               vidxs[i][j] = r.addVertex(pm01);
            } else if (i == nsegs && j == nsegs) {
               vidxs[i][j] = r.addVertex(pm12);
            } else if (i == 2*nsegs && j == 0) {
               vidxs[i][j] = r.addVertex(p1);
            } else {
               vidxs[i][j] = r.vertex (0f, 0f, 0f);
            }
         }
      }
      
      // add faces
      for (int i=0; i<2*nsegs; ++i) {
         for (int j=0; j<2*nsegs-i-1; ++j) {
            r.addTriangle (vidxs[i][j], vidxs[i+1][j], vidxs[i][j+1]);
            r.addTriangle (vidxs[i+1][j], vidxs[i+1][j+1], vidxs[i][j+1]);
         }
         r.addTriangle (vidxs[i][2*nsegs-i-1], vidxs[i+1][2*nsegs-i-1], vidxs[i][2*nsegs-i]);
      }
      
      if (faces != null) {
         // add to faces
         for (int i=0; i<2*nsegs; ++i) {
            for (int j=0; j<2*nsegs-i-1; ++j) {
               faces.addVertex (vidxs[i][j]);
               faces.addVertex (vidxs[i+1][j]);
               faces.addVertex (vidxs[i][j+1]);
               faces.addVertex (vidxs[i+1][j]);
               faces.addVertex (vidxs[i+1][j+1]);
               faces.addVertex (vidxs[i][j+1]);
            }
            faces.addVertex (vidxs[i][2*nsegs-i-1]);
            faces.addVertex (vidxs[i+1][2*nsegs-i-1]);
            faces.addVertex (vidxs[i][2*nsegs-i]);
         }
      }
      
      return vidx0;
      
   }
   
   /**
    * Updates positions and normals of a quadratic triangular face
    * 
    * @param r render object
    * @param nsegs number of segments along each half-edge
    * @param p0 position index of v0
    * @param p1 position index of v1
    * @param p2 position index of v2
    * @param pm01 position index of the mid vertex between v0 and v1
    * @param pm12 position index of the mid vertex between v1 and v2
    * @param pm20 position index of the mid vertex between v2 and v0
    * @param vidx0 vertex index of first vertex in patch to update
    */
   public static void updateQuadFace (
      RenderObject r, int nsegs,
      int p0, int p1, int p2, int pm01, int pm12, int pm20, int vidx0) {

      float[] pos0 = r.getPosition (p0);
      float[] pos1 = r.getPosition (p1);
      float[] pos2 = r.getPosition (p2);
      float[] posm01 = r.getPosition (pm01);
      float[] posm12 = r.getPosition (pm12);
      float[] posm20 = r.getPosition (pm20);
      
      int k = vidx0;
      for (int i=0; i<2*nsegs+1; ++i) {
         float s = 0.5f*i/nsegs;
         for (int j=0; j<2*nsegs+1-i; ++j) {
            float t = 0.5f*j/nsegs;
            float u = 1.0f-s-t;
            
            // interpolate face position
            if (     !(i == 0 && j == 0) 
                  && !(i == 0 && j == nsegs) 
                  && !(i == 0 && j == 2*nsegs) 
                  && !(i == nsegs && j == 0)
                  && !(i == nsegs && j == nsegs)
                  && !(i == 2*nsegs && j == 0) ) {
               
               float[] pos = r.getVertexPosition (k);
               float w0 = u*(2*u-1);
               float w1 = s*(2*s-1);
               float w2 = t*(2*t-1);
               float wm01 = 4*s*u;
               float wm12 = 4*s*t;
               float wm20 = 4*t*u;
               
               pos[0] = w0*pos0[0] + w1*pos1[0] + w2*pos2[0] + wm01*posm01[0] + wm12*posm12[0] + wm20*posm20[0];
               pos[1] = w0*pos0[1] + w1*pos1[1] + w2*pos2[1] + wm01*posm01[1] + wm12*posm12[1] + wm20*posm20[1];
               pos[2] = w0*pos0[2] + w1*pos1[2] + w2*pos2[2] + wm01*posm01[2] + wm12*posm12[2] + wm20*posm20[2];
            }

            // d/ds
            float ws0 = -4*u+1;
            float ws1 = 4*s-1;
            float ws2 = 0;
            float wsm01 = 4*(u - s);
            float wsm12 = 4*t;
            float wsm20 = -4*t;
            float ns0 = ws0*pos0[0] + ws1*pos1[0] + ws2*pos2[0] + wsm01*posm01[0] + wsm12*posm12[0] + wsm20*posm20[0];
            float ns1 = ws0*pos0[1] + ws1*pos1[1] + ws2*pos2[1] + wsm01*posm01[1] + wsm12*posm12[1] + wsm20*posm20[1];
            float ns2 = ws0*pos0[2] + ws1*pos1[2] + ws2*pos2[2] + wsm01*posm01[2] + wsm12*posm12[2] + wsm20*posm20[2];
            
            // d/dt
            float wt0 = -4*u+1;
            float wt1 = 0;
            float wt2 = 4*t-1;
            float wtm01 = -4*s;
            float wtm12 = 4*s;
            float wtm20 = 4*(u - t);
            float nt0 = wt0*pos0[0] + wt1*pos1[0] + wt2*pos2[0] + wtm01*posm01[0] + wtm12*posm12[0] + wtm20*posm20[0];
            float nt1 = wt0*pos0[1] + wt1*pos1[1] + wt2*pos2[1] + wtm01*posm01[1] + wtm12*posm12[1] + wtm20*posm20[1];
            float nt2 = wt0*pos0[2] + wt1*pos1[2] + wt2*pos2[2] + wtm01*posm01[2] + wtm12*posm12[2] + wtm20*posm20[2];
            
            // cross n = ns x nt
            float[] nrm = r.getVertexNormal (k);
            float nx = ns1*nt2-ns2*nt1;
            float ny = ns2*nt0-ns0*nt2;
            float nz = ns0*nt1-ns1*nt0;
            nrm[0] = nx;
            nrm[1] = ny;
            nrm[2] = nz;
            
            ++k;
         }
      }
      r.notifyNormalsModified ();
      if (nsegs > 1) {
         r.notifyPositionsModified ();
      }
   }

   /**
    * Adds a quadratic quadrilateral face
    * @param r render object
    * @param faces list of face indices to populate
    * @param nsegs number of segments along each half edge
    * @param p0 position index of v0
    * @param p1 position index of v1
    * @param p2 position index of v2
    * @param p3 position index of v3
    * @param pm01 position index of the mid vertex between v0 and v1
    * @param pm12 position index of the mid vertex between v1 and v2
    * @param pm23 position index of the mid vertex between v2 and v3
    * @param pm30 position index of the mid vertex between v3 and v0
    * @return index of first created vertex
    */
   public static int addQuadFace (
      RenderObject r, FeatureIndexArray faces, int nsegs,
      int p0, int p1, int p2, int p3, int pm01, int pm12, int pm23, int pm30) {
      
      int vidx0 = r.numVertices ();
      
      int[][] vidxs = new int[2*nsegs+1][2*nsegs+1];

      for (int i=0; i<2*nsegs+1; ++i) {
         for (int j=0; j<2*nsegs+1; ++j) {
            
            r.addNormal (0f, 0f, 0f);
            
            // duplicate vertices
            if (i == 0 && j == 0) {
               vidxs[i][j] = r.addVertex (p0);
            } else if (i == 0 && j == nsegs) {
               vidxs[i][j] = r.addVertex(pm30);
            } else if (i == 0 && j == 2*nsegs) {
               vidxs[i][j] = r.addVertex(p3);
            } else if (i == nsegs && j == 0) {
               vidxs[i][j] = r.addVertex(pm01);
            } else if (i == nsegs && j == 2*nsegs) {
               vidxs[i][j] = r.addVertex(pm23);
            } else if (i == 2*nsegs && j == 0) {
               vidxs[i][j] = r.addVertex(p1);
            } else if (i == 2*nsegs && j == nsegs) {
               vidxs[i][j] = r.addVertex (pm12);
            } else if (i == 2*nsegs && j == 2*nsegs) {
               vidxs[i][j] = r.addVertex(p2);
            } else {
               vidxs[i][j] = r.vertex (0f, 0f, 0f);
            }
         }
      }
      
      // add faces
      for (int i=0; i<2*nsegs; ++i) {
         for (int j=0; j<2*nsegs; ++j) {
            r.addTriangle (vidxs[i][j], vidxs[i+1][j], vidxs[i][j+1]);
            r.addTriangle (vidxs[i+1][j], vidxs[i+1][j+1], vidxs[i][j+1]);
         }
      }
      
      if (faces != null) {
         // add to faces
         for (int i=0; i<2*nsegs; ++i) {
            for (int j=0; j<2*nsegs; ++j) {
               faces.addVertex (vidxs[i][j]);
               faces.addVertex (vidxs[i+1][j]);
               faces.addVertex (vidxs[i][j+1]);
               faces.addVertex (vidxs[i+1][j]);
               faces.addVertex (vidxs[i+1][j+1]);
               faces.addVertex (vidxs[i][j+1]);
            }
         }
      }
      
      return vidx0;
      
   }
   
   /**
    * Updates positions and normals for quadratic quadrilateral patch
    * 
    * @param r render object
    * @param nsegs numver of segments along each half edge
    * @param p0 position index of v0
    * @param p1 position index of v1
    * @param p2 position index of v2
    * @param p3 position index of v3
    * @param pm01 position index of the mid vertex between v0 and v1
    * @param pm12 position index of the mid vertex between v1 and v2
    * @param pm23 position index of the mid vertex between v2 and v3
    * @param pm30 position index of the mid vertex between v3 and v0
    * @param vidx0 vertex index of first vertex in patch to update
    */
   public static void updateQuadFace (
      RenderObject r, int nsegs, int p0, int p1, int p2, int p3, int pm01, int pm12, int pm23, int pm30, int vidx0) {

      float[] pos0 = r.getPosition (p0);
      float[] pos1 = r.getPosition (p1);
      float[] pos2 = r.getPosition (p2);
      float[] pos3 = r.getPosition (p3);
      float[] posm01 = r.getPosition (pm01);
      float[] posm12 = r.getPosition (pm12);
      float[] posm23 = r.getPosition (pm23);
      float[] posm30 = r.getPosition (pm30);

      int k = vidx0;
      for (int i=0; i<2*nsegs+1; ++i) {
         float s = (float)i/nsegs-1.0f;
         for (int j=0; j<2*nsegs+1; ++j) {
            float t = (float)j/nsegs-1.0f;

            // interpolate faces
            if (     !(i == 0 && j == 0) 
            && !(i == 0 && j == nsegs) 
            && !(i == 0 && j == 2*nsegs) 
            && !(i == nsegs && j == 0)
            && !(i == nsegs && j == 2*nsegs)
            && !(i == 2*nsegs && j == 0)
            && !(i == 2*nsegs && j == nsegs)
            && !(i == 2*nsegs && j == 2*nsegs)) {

               float[] pos = r.getVertexPosition (k);

               float wm01 = 0.5f*(1-s*s)*(1-t);
               float wm12 = 0.5f*(1+s)*(1-t*t);
               float wm23 = 0.5f*(1-s*s)*(1+t);
               float wm30 = 0.5f*(1-s)*(1-t*t);
               float w0 = 0.25f*(1-s)*(1-t) - 0.5f*(wm01 + wm30);
               float w1 = 0.25f*(1+s)*(1-t) - 0.5f*(wm01 + wm12);
               float w2 = 0.25f*(1+s)*(1+t) - 0.5f*(wm12 + wm23);
               float w3 = 0.25f*(1-s)*(1+t) - 0.5f*(wm23 + wm30);

               pos[0] = w0*pos0[0] + w1*pos1[0] + w2*pos2[0] + w3*pos3[0] + wm01*posm01[0] + wm12*posm12[0] + wm23*posm23[0] + wm30*posm30[0];
               pos[1] = w0*pos0[1] + w1*pos1[1] + w2*pos2[1] + w3*pos3[1] + wm01*posm01[1] + wm12*posm12[1] + wm23*posm23[1] + wm30*posm30[1];
               pos[2] = w0*pos0[2] + w1*pos1[2] + w2*pos2[2] + w3*pos3[2] + wm01*posm01[2] + wm12*posm12[2] + wm23*posm23[2] + wm30*posm30[2];
            }

            // d/ds
            float wsm01 = -s*(1-t);
            float wsm12 = 0.5f*(1-t*t);
            float wsm23 = -s*(1+t);
            float wsm30 = -0.5f*(1-t*t); 
            float ws0 = -0.25f*(1-t) - 0.5f*(wsm01 + wsm30);
            float ws1 = 0.25f*(1-t) - 0.5f*(wsm01 + wsm12);
            float ws2 = 0.25f*(1+t) - 0.5f*(wsm12 + wsm23);
            float ws3 = -0.25f*(1+t) - 0.5f*(wsm23 + wsm30);
            float ns0 = ws0*pos0[0] + ws1*pos1[0] + ws2*pos2[0] + ws3*pos3[0] + wsm01*posm01[0] + wsm12*posm12[0] + wsm23*posm23[0] + wsm30*posm30[0];
            float ns1 = ws0*pos0[1] + ws1*pos1[1] + ws2*pos2[1] + ws3*pos3[1] + wsm01*posm01[1] + wsm12*posm12[1] + wsm23*posm23[1] + wsm30*posm30[1];
            float ns2 = ws0*pos0[2] + ws1*pos1[2] + ws2*pos2[2] + ws3*pos3[2] + wsm01*posm01[2] + wsm12*posm12[2] + wsm23*posm23[2] + wsm30*posm30[2];

            // d/dt
            float wtm01 = -0.5f*(1-s*s);
            float wtm12 = -(1+s)*t;
            float wtm23 = 0.5f*(1-s*s);
            float wtm30 = -(1-s)*t; 
            float wt0 = -0.25f*(1-s) - 0.5f*(wtm01 + wtm30);
            float wt1 = -0.25f*(1+s) - 0.5f*(wtm01 + wtm12);
            float wt2 = 0.25f*(1+s) - 0.5f*(wtm12 + wtm23);
            float wt3 = 0.25f*(1-s) - 0.5f*(wtm23 + wtm30);
            float nt0 = wt0*pos0[0] + wt1*pos1[0] + wt2*pos2[0] + wt3*pos3[0] + wtm01*posm01[0] + wtm12*posm12[0] + wtm23*posm23[0] + wtm30*posm30[0];
            float nt1 = wt0*pos0[1] + wt1*pos1[1] + wt2*pos2[1] + wt3*pos3[1] + wtm01*posm01[1] + wtm12*posm12[1] + wtm23*posm23[1] + wtm30*posm30[1];
            float nt2 = wt0*pos0[2] + wt1*pos1[2] + wt2*pos2[2] + wt3*pos3[2] + wtm01*posm01[2] + wtm12*posm12[2] + wtm23*posm23[2] + wtm30*posm30[2];

            // cross n = ns x nt
            float[] nrm = r.getVertexNormal (k);
            float nx = ns1*nt2-ns2*nt1;
            float ny = ns2*nt0-ns0*nt2;
            float nz = ns0*nt1-ns1*nt0;
            nrm[0] = nx;
            nrm[1] = ny;
            nrm[2] = nz;
            ++k;
         }
      }
      
      r.notifyNormalsModified ();
      if (nsegs > 1) {
         r.notifyPositionsModified ();
      }
   }

   /**
    * Adds flat triangulated faces
    * @param r render object
    * @param elem element
    */
   public static void addWidgetFaces (RenderObject r, FemElement3dBase elem) {
      addWidgetFaces(r, null, elem); 
   }
   
   /**
    * Adds flat triangulated faces
    * @param r render object
    * @param faces face index list to populate
    * @param elem element
    */
   public static void addWidgetFaces (RenderObject r, 
      FeatureIndexArray faces, FemElement3dBase elem) {
      
      // add positions for storing widget vertices, one for each node
      int p0idx = r.numPositions();
      for (int j=0; j<elem.numNodes(); j++) {
         r.addPosition (0, 0, 0);
      }
      
      double size = elem.getElementWidgetSize();
      FemElementRenderer.updateWidgetPositions (r, elem, size, p0idx);
      
      int[] fidxs = elem.getFaceIndices ();
      int[] tris = FemUtilities.triangulateFaceIndices (fidxs);
      
      TriangulatedFacePatch face = new TriangulatedFacePatch (p0idx, fidxs.length, tris);
      face.addFaces (r, faces);
      
   }

   /**
    * Updates widget node locations
    * @param r render object
    * @param elem element
    * @param size widget size
    * @param idx position of first widget node
    * @return position index after widget nodes
    */
   public static int updateWidgetPositions (
      RenderObject r, FemElement3dBase elem, double size, int idx) {

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

   /**
    * Update widget node positions with rest coordinates
    * @param r render object to update
    * @param elem element to update
    * @param size widget size
    * @param idx position index for first node
    * @return position index after last node
    */
   public static int updateWidgetRestPositions (
      RenderObject r, FemElement3d elem, double size, int idx) {

      FemNode3d[] enodes = elem.getNodes();

      // compute center point
      float cx = 0;
      float cy = 0;
      float cz = 0;
      for (int j=0; j<enodes.length; j++) {
         Point3d rest = enodes[j].myRest;
         cx += rest.x;
         cy += rest.y;
         cz += rest.z;
      }
      cx /= enodes.length;
      cy /= enodes.length;
      cz /= enodes.length;

      float s = (float)size;
      for (int j=0; j<enodes.length; j++) {
         Point3d rest = enodes[j].myRest;
         float dx = (float)rest.x-cx;
         float dy = (float)rest.y-cy;
         float dz = (float)rest.z-cz;
         r.setPosition (idx++, cx+s*dx, cy+s*dy, cz+s*dz);
      }
      return idx;
   }

   /**
    * Updates normals for flat triangulated widgets
    * @param r render object to update
    * @param tgrp rendering group
    */
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
   
   /**
    * Patch of triangles for widget face
    */
   public static interface WidgetFacePatch {
      /**
       * Adds triangles for widget face
       * @param r render object to add to
       */
      public void addFaces(RenderObject r);
      
      /**
       * adds triangles for widget face
       * @param r render object to add to
       * @param faces face index list to add to (ignored if null)
       */
      public void addFaces(RenderObject r, FeatureIndexArray faces);
      
      /**
       * Updates positions and normals of patch vertices
       * @param r render object to update
       */
      public void updateFaces(RenderObject r);
   }
   
   /**
    * Triangle face patch
    */
   public static class TriFacePatch implements WidgetFacePatch {

      int myP0;
      int myP1;
      int myP2;
      int myVidx0;
      
      public TriFacePatch(int p0, int p1, int p2) {
         myP0 = p0;
         myP1 = p1;
         myP2 = p2;
         myVidx0 = -1;
      }
      
      @Override
      public void addFaces (RenderObject r) {
         addFaces(r, null);
      }

      @Override
      public void addFaces (RenderObject r, FeatureIndexArray faces) {
         int nidx = r.addNormal (0, 0, 0);
         myVidx0 = r.addVertex (myP0, nidx);
         r.addVertex (myP1, nidx);
         r.addVertex (myP2, nidx);
         r.addTriangle (myVidx0, myVidx0+1, myVidx0+2);
         
         if (faces != null) {
            faces.addVertex (myVidx0);
            faces.addVertex (myVidx0+1);
            faces.addVertex (myVidx0+2);
         }
      }

      @Override
      public void updateFaces (RenderObject r) {
         // update normal
         float[] nrm = r.getVertexNormal (myVidx0);
         float[] p0 = r.getVertexPosition (myVidx0);
         float[] p1 = r.getVertexPosition (myVidx0+1);
         float[] p2 = r.getVertexPosition (myVidx0+2);

         float ax = p1[0]-p0[0];
         float ay = p1[1]-p0[1];
         float az = p1[2]-p0[2];
         float bx = p2[0]-p0[0];
         float by = p2[1]-p0[1];
         float bz = p2[2]-p0[2];
         float nx = ay*bz-az*by;
         float ny = az*bx-ax*bz;
         float nz = ax*by-ay*bx;

         nrm[0] = nx;
         nrm[1] = ny;
         nrm[2] = nz;
         r.notifyNormalsModified ();
      }
      
   }
   
   /**
    * Quadrilateral face patch
    */
   public static class QuadFacePatch implements WidgetFacePatch {

      int myP0;
      int myP1;
      int myP2;
      int myP3;
      int myVidx0;
      int mySegs;
      
      public QuadFacePatch(int p0, int p1, int p2,  int p3, int nsegs) {
         myP0 = p0;
         myP1 = p1;
         myP2 = p2;
         myP3 = p3;
         myVidx0 = -1;
         mySegs = nsegs;
      }
      
      @Override
      public void addFaces (RenderObject r) {
         addFaces(r, null);
      }

      @Override
      public void addFaces (RenderObject r, FeatureIndexArray faces) {
         
         myVidx0 = r.numVertices ();
         
         int nsegs = mySegs;
         int[][] vidxs = new int[nsegs+1][nsegs+1];

         for (int i=0; i<nsegs+1; ++i) {
            for (int j=0; j<nsegs+1; ++j) {
               
               r.addNormal (0f, 0f, 0f);
               // duplicate vertices
               if (i == 0 && j == 0) {
                  vidxs[i][j] = r.addVertex (myP0);
               } else if (i == 0 && j == nsegs) {
                  vidxs[i][j] = r.addVertex(myP3);
               } else if (i == nsegs && j == 0) {
                  vidxs[i][j] = r.addVertex(myP1);
               } else if (i == nsegs && j == nsegs) {
                  vidxs[i][j] = r.addVertex(myP2);
               } else {
                  vidxs[i][j] = r.vertex (0f, 0f, 0f);
               }
            }
         }
         
         // add faces
         for (int i=0; i<nsegs; ++i) {
            for (int j=0; j<nsegs; ++j) {
               r.addTriangle (vidxs[i][j], vidxs[i+1][j], vidxs[i][j+1]);
               r.addTriangle (vidxs[i+1][j], vidxs[i+1][j+1], vidxs[i][j+1]);
            }
         }
         
         if (faces != null) {
            // add to faces
            for (int i=0; i<nsegs; ++i) {
               for (int j=0; j<nsegs; ++j) {
                  faces.addVertex (vidxs[i][j]);
                  faces.addVertex (vidxs[i+1][j]);
                  faces.addVertex (vidxs[i][j+1]);
                  faces.addVertex (vidxs[i+1][j]);
                  faces.addVertex (vidxs[i+1][j+1]);
                  faces.addVertex (vidxs[i][j+1]);
               }
            }
         }
      }

      @Override
      public void updateFaces (RenderObject r) {
         
         float[] pos0 = r.getPosition (myP0);
         float[] pos1 = r.getPosition (myP1);
         float[] pos2 = r.getPosition (myP2);
         float[] pos3 = r.getPosition (myP3);
         
         int k = myVidx0;
         int nsegs = mySegs;
         
         for (int i=0; i<nsegs+1; ++i) {
            float s = 2.0f*i/nsegs-1.0f;
            for (int j=0; j<nsegs+1; ++j) {
               float t = 2.0f*j/nsegs-1.0f;

               // interpolate faces
               if (!(i == 0 && j == 0) 
                  && !(i == 0 && j == nsegs) 
                  && !(i == nsegs && j == 0)
                  && !(i == nsegs && j == nsegs)) {

                  float[] pos = r.getVertexPosition (k);
                  float w0 = 0.25f*(1-s)*(1-t);
                  float w1 = 0.25f*(1+s)*(1-t);
                  float w2 = 0.25f*(1+s)*(1+t);
                  float w3 = 0.25f*(1-s)*(1+t);

                  pos[0] = w0*pos0[0] + w1*pos1[0] + w2*pos2[0] + w3*pos3[0];
                  pos[1] = w0*pos0[1] + w1*pos1[1] + w2*pos2[1] + w3*pos3[1];
                  pos[2] = w0*pos0[2] + w1*pos1[2] + w2*pos2[2] + w3*pos3[2];
               }

               // d/ds
               float ws0 = -0.25f*(1-t);
               float ws1 = 0.25f*(1-t);
               float ws2 = 0.25f*(1+t);
               float ws3 = -0.25f*(1+t);
               float ns0 = ws0*pos0[0] + ws1*pos1[0] + ws2*pos2[0] + ws3*pos3[0];
               float ns1 = ws0*pos0[1] + ws1*pos1[1] + ws2*pos2[1] + ws3*pos3[1];
               float ns2 = ws0*pos0[2] + ws1*pos1[2] + ws2*pos2[2] + ws3*pos3[2];

               // d/dt
               float wt0 = -0.25f*(1-s);
               float wt1 = -0.25f*(1+s);
               float wt2 = 0.25f*(1+s);
               float wt3 = 0.25f*(1-s);
               float nt0 = wt0*pos0[0] + wt1*pos1[0] + wt2*pos2[0] + wt3*pos3[0];
               float nt1 = wt0*pos0[1] + wt1*pos1[1] + wt2*pos2[1] + wt3*pos3[1];
               float nt2 = wt0*pos0[2] + wt1*pos1[2] + wt2*pos2[2] + wt3*pos3[2];

               // cross n = ns x nt
               float[] nrm = r.getVertexNormal (k);
               float nx = ns1*nt2-ns2*nt1;
               float ny = ns2*nt0-ns0*nt2;
               float nz = ns0*nt1-ns1*nt0;
               nrm[0] = nx;
               nrm[1] = ny;
               nrm[2] = nz;
               ++k;
            }

         }
         r.notifyNormalsModified ();
         if (nsegs > 1) {
            r.notifyPositionsModified ();
         }
      }
      
   }
   
   /**
    * Patch of specified triangles
    */
   public static class TriangulatedFacePatch implements WidgetFacePatch {

      int[] myTris;
      int myP0;
      int myNumPoints;
      int myVidx0;
      
      public TriangulatedFacePatch(int p0, int numPoints, int[] tris) {
         myTris = tris;
         myP0 = p0;
         myNumPoints = numPoints;
         myVidx0 = -1;
      }
      
      @Override
      public void addFaces (RenderObject r) {
         addFaces(r, null);
      }

      @Override
      public void addFaces (RenderObject r, FeatureIndexArray faces) {
         myVidx0 = r.numVertices ();
         // add triangular faces
         for (int i=0; i<myTris.length; i+=3) {
            int p0 = myP0 + myTris[i];
            int p1 = myP0 + myTris[i+1];
            int p2 = myP0 + myTris[i+2];
            int nidx = r.addNormal (0, 0, 0);
            int v0 = r.addVertex (p0, nidx);
            r.addVertex (p1, nidx);
            r.addVertex (p2, nidx);
            r.addTriangle (v0, v0+1, v0+2);
         }
         
         if (faces != null) {
            int vidx = myVidx0;
            for (int i=0; i<myTris.length; ++i) {
               faces.addVertex (vidx++);
            }
         }
      }

      @Override
      public void updateFaces (RenderObject r) {
         
         // update normals
         for (int i=0; i<myTris.length; i+=3) {
            float[] nrm = r.getVertexNormal (myVidx0 + i);
            float[] p0 = r.getVertexPosition (myVidx0 + i);
            float[] p1 = r.getVertexPosition (myVidx0 + i + 1);
            float[] p2 = r.getVertexPosition (myVidx0 + i + 2);
      
            float ax = p1[0]-p0[0];
            float ay = p1[1]-p0[1];
            float az = p1[2]-p0[2];
            float bx = p2[0]-p0[0];
            float by = p2[1]-p0[1];
            float bz = p2[2]-p0[2];
            float nx = ay*bz-az*by;
            float ny = az*bx-ax*bz;
            float nz = ax*by-ay*bx;
      
            nrm[0] = nx;
            nrm[1] = ny;
            nrm[2] = nz;
         }
         r.notifyNormalsModified ();
      }
   }
   
   /**
    * Quadratic triangle
    */
   public static class QuadraticTriFacePatch implements WidgetFacePatch {
      int myP0;
      int myP1;
      int myP2;
      int myPm01;
      int myPm12;
      int myPm20;
      int myVidx0;
      int mySegs;
      
      public QuadraticTriFacePatch (int v0, int v1, int v2, int vm01, int vm12, int vm20, int nsegs) {
         myP0 = v0;
         myP1 = v1;
         myP2 = v2;
         myPm01 = vm01;
         myPm12 = vm12;
         myPm20 = vm20;
         mySegs = nsegs;
         myVidx0 = -1;
      }
      
      public void addFaces (RenderObject r) {
         addFaces(r, null);
      }

      public void addFaces (RenderObject r, FeatureIndexArray faces) {
         myVidx0 = addQuadFace (r, faces, mySegs, myP0, myP1, myP2, myPm01, myPm12, myPm20);
      }

      public void updateFaces (RenderObject r) {
         updateQuadFace(r, mySegs, myP0, myP1, myP2, myPm01, myPm12, myPm20, myVidx0);
      }
      
   }
   
   /**
    * Quadratic quadrilateral
    *
    */
   public static class QuadraticQuadFacePatch implements WidgetFacePatch {
      int myP0;
      int myP1;
      int myP2;
      int myP3;
      int myPm01;
      int myPm12;
      int myPm23;
      int myPm30;
      int myVidx0;
      int mySegs;
      
      public QuadraticQuadFacePatch (int v0, int v1, int v2, int v3, int vm01, int vm12, int vm23, int vm30, int nsegs) {
         myP0 = v0;
         myP1 = v1;
         myP2 = v2;
         myP3 = v3;
         myPm01 = vm01;
         myPm12 = vm12;
         myPm23 = vm23;
         myPm30 = vm30;
         myVidx0 = -1;
         mySegs = nsegs;
      }
      
      public void addFaces (RenderObject r) {
         addFaces(r, null);
      }

      public void addFaces (RenderObject r, FeatureIndexArray faces) {
         myVidx0 = addQuadFace (r, faces, mySegs, myP0, myP1, myP2, myP3, myPm01, myPm12, myPm23, myPm30);
      }

      public void updateFaces (RenderObject r) {
         updateQuadFace(r, mySegs, myP0, myP1, myP2, myP3, myPm01, myPm12, myPm23, myPm30, myVidx0);
      }
      
   }
   
   /**
    * Element edge curve
    */
   public static interface EdgeCurve {
      /**
       * Adds line segments for this curve
       * @param r render object to add to
       */
      public void addCurve(RenderObject r);
      
      /**
       * Adds line segments for this curve
       * @param r render object to add to
       * @param lines line segment list to add to
       */
      public void addCurve(RenderObject r, FeatureIndexArray lines);
      
      /**
       * Updates positions for this curve
       * @param r render object to update
       */
      public void updateCurve(RenderObject r);
   }
   
   public static class LinearEdgeCurve implements EdgeCurve {
      int myV0;
      int myV1;
      
      public LinearEdgeCurve(int v0, int v1) {
         myV0 = v0;
         myV1 = v1;
      }
      
      @Override
      public void addCurve (RenderObject r) {
         addCurve(r, null);
      }
      
      @Override
      public void addCurve (RenderObject r, FeatureIndexArray lines) {
         r.addLine (myV0, myV1);
         if (lines != null) {
            lines.addVertex (myV0);
            lines.addVertex (myV1);
         }
      }
      
      @Override
      public void updateCurve (RenderObject r) {
         // nothing, based solely on widget node locations
      }
      
   }
   
   /**
    * Quadratic edge
    */
   public static class QuadEdgeCurve implements EdgeCurve {
      
      int myVidx0;
      int myVidx1;    
      int myVidxm;
      int myPidx0;
      int mySegs;

      public QuadEdgeCurve (int vidx0, int vidxm, int vidx1, int nsegs) {
         myVidx0 = vidx0;
         myVidxm = vidxm;
         myVidx1 = vidx1;
         mySegs = nsegs;
      }
      
      public void addCurve (RenderObject r) {
         addCurve(r, null);
      }

      public void addCurve (RenderObject r, FeatureIndexArray lines) {
         myPidx0 = addQuadEdge (r, lines, mySegs, myVidx0, myVidxm, myVidx1);
      }

      public void updateCurve (RenderObject r) {
         updateQuadEdge (r, mySegs, myVidx0, myVidxm, myVidx1, myPidx0);
      }
   }
   
   public static void addEdges(RenderObject r, FeatureIndexArray lines, 
      List<EdgeCurve> curves, FemElement3dBase elem, int nsegs) {
      
      r.addNormal (1, 0, 0); // dummy normal for lines
      
      // element nodes
      int ev0 = r.numVertices ();
      for (int i=0; i<elem.numNodes(); i++) {
         r.vertex (0, 0, 0);
      }
         
      int[] eidxs = elem.getEdgeIndices();
      int numv = 0;
      for (int i=0; i<eidxs.length; i+=(numv+1)) {
         numv = eidxs[i];
         if (numv == 2) {
            int vidx0 = ev0+eidxs[i+1];
            int vidx1 = ev0+eidxs[i+2];
            LinearEdgeCurve curve = new LinearEdgeCurve (vidx0, vidx1);
            curve.addCurve (r, lines);
            curves.add (curve);
         }
         else if (numv == 3) {
            int vidx0 = ev0+eidxs[i+1];
            int vidxm = ev0+eidxs[i+2];
            int vidx1 = ev0+eidxs[i+3];
            // r.addLine (vidx0, vidxm);
            // r.addLine (vidxm, vidx1);
            QuadEdgeCurve quad = new QuadEdgeCurve (vidx0, vidxm, vidx1, numQuadEdgeSegs);
            quad.addCurve (r);
            curves.add (quad);
         }
      }
   }
   
   public static void addWidgetFaces(RenderObject r, FeatureIndexArray tris, 
      List<WidgetFacePatch> patches, FemElement3dBase elem, int nsegs) {
      
      // widget node locations
      int wp0 = r.numPositions ();
      for (int i=0; i<elem.numNodes (); ++i) {
         r.addPosition (0, 0, 0);
      }
      
      int[] fidxs = elem.getFaceIndices ();
      int numv = 0; 
      for (int i=0; i<fidxs.length; i+=(numv+1)) {
         numv = fidxs[i];
         if (numv == 3) {
            int p0 = wp0+fidxs[i+1];
            int p1 = wp0+fidxs[i+2];
            int p2 = wp0+fidxs[i+3];
            
            TriFacePatch patch = new TriFacePatch (p0, p1, p2);
            patch.addFaces (r, tris);
            patches.add (patch);
         }
         else if (numv == 4) {
            int p0 = wp0+fidxs[i+1];
            int p1 = wp0+fidxs[i+2];
            int p2 = wp0+fidxs[i+3];
            int p3 = wp0+fidxs[i+4];
            
            QuadFacePatch quad = new QuadFacePatch (p0, p1, p2, p3, nsegs);
            quad.addFaces (r, tris);
            patches.add (quad);
         } else if (numv == 6) {
            int p0 = wp0+fidxs[i+1];
            int p1 = wp0+fidxs[i+3];
            int p2 = wp0+fidxs[i+5];
            int pm01 = wp0 + fidxs[i+2];
            int pm12 = wp0 + fidxs[i+4];
            int pm20 = wp0 + fidxs[i+6];
            
            QuadraticTriFacePatch patch = new QuadraticTriFacePatch (p0, p1, p2, pm01, pm12, pm20, nsegs);
            patch.addFaces (r, tris);
            patches.add (patch);
         } else if (numv == 8) {
            int p0 = wp0+fidxs[i+1];
            int p1 = wp0+fidxs[i+3];
            int p2 = wp0+fidxs[i+5];
            int p3 = wp0+fidxs[i+7];
            int pm01 = wp0 + fidxs[i+2];
            int pm12 = wp0 + fidxs[i+4];
            int pm23 = wp0 + fidxs[i+6];
            int pm30 = wp0 + fidxs[i+8];
            
            QuadraticQuadFacePatch patch = new QuadraticQuadFacePatch (p0, p1, p2, p3, pm01, pm12, pm23, pm30, nsegs);
            patch.addFaces (r, tris);
            patches.add (patch);
         } else {
            // triangulate
            int[] tidxs = new int[3*(numv-2)];
            int idx = 0;
            for (int j=2; j<numv; ++j) {
               tidxs[idx++] = 0;
               tidxs[idx++] = j-1;
               tidxs[idx++] = j;
            }
            
            TriangulatedFacePatch patch = new TriangulatedFacePatch (wp0, numv, tidxs);
            patch.addFaces (r, tris);
            patches.add (patch);
         }
      }
      
      r.setTransient (true);
      
   }

   public static void updateEdgePositions (
      RenderObject r, List<EdgeCurve> edges, FemElement3dBase elem,
      int ep0) {

      FemNode[] nodes = elem.getNodes();
      for (int i=0; i<nodes.length; i++) {
         r.setPosition (ep0+i, nodes[i].myRenderCoords);
      }
      
      if (edges.size() > 0) {
         for (int i=0; i<edges.size(); i++) {
            edges.get(i).updateCurve (r);
         }
      }
      r.notifyPositionsModified();
   }
   
   static void updateWidgets(RenderObject r, List<WidgetFacePatch> patches, 
      FemElement3dBase elem, double size, int wp0) {
      
      // update nodes
      updateWidgetPositions (r, elem, size, wp0);
      
      for (WidgetFacePatch patch : patches) {
         patch.updateFaces (r);
      }
   }

   public void renderWidget (
      Renderer renderer, FemElement3dBase elem, double size, RenderProps props) {

      RenderObject r = myRob;      

      updateWidgets(r, myFaces, elem, size, myWidgetPos0Idx);

      //      updateWidgetPositions (r, elem, size, myWidgetPos0Idx);
      //      updateWidgetNormals (r, /*group=*/0);

      
      if (!renderer.isSelecting()) {
         float[] color = props.getFaceColorF();
         if (elem.isInverted()) {
            color = FemModel3d.myInvertedColor;
         }
         renderer.setFaceColoring (props, color, elem.isSelected());
      }
      Shading oldShading = renderer.getShading ();
      renderer.setShading (props.getShading ());
      renderer.drawTriangles (r, /*group=*/0);
      renderer.setShading (oldShading);
   }

   public void renderRestWidget (
      Renderer renderer, FemElement3dBase elem, double size, RenderProps props) {

      RenderObject r = myRob;      

      updateWidgets(r, myFaces, elem, size, myWidgetPos0Idx);
      
      //      updateWidgetRestPositions (r, elem, size, myWidgetPos0Idx);
      //      updateWidgetNormals (r, /*group=*/0);

      if (!renderer.isSelecting()) {
         float[] color = props.getFaceColorF();
         if (elem.isInverted()) {
            color = FemModel3d.myInvertedColor;
         }
         renderer.setFaceColoring (props, color, elem.isSelected());
      }
      renderer.drawTriangles (r, /*group=*/ 0);
   }

   public void render (
      Renderer renderer, FemElement3dBase elem, RenderProps props) {

      RenderObject r = myRob;      
      updateEdgePositions (r, myEdges, elem, 0);

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

