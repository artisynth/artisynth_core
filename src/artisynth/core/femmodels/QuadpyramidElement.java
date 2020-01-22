/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import maspack.matrix.*;
import maspack.render.Renderer;
import maspack.render.RenderProps;

public class QuadpyramidElement extends FemElement3d {

   private static IntegrationPoint3d[] myDefaultIntegrationPoints;
   private static IntegrationPoint3d myWarpingPoint;
   private static FemElementRenderer myRenderer;

   static final int NUM_NODES = 13;

   public boolean isLinear() {
      return false;
   }
   
   public IntegrationPoint3d[] getIntegrationPoints() {
      if (myDefaultIntegrationPoints == null) {
         myDefaultIntegrationPoints = 
            createIntegrationPoints (new QuadpyramidElement());
      }
      return myDefaultIntegrationPoints;
   }

   public IntegrationPoint3d getWarpingPoint() {
      if (myWarpingPoint == null) {
         
         // create special warping point based on linear pyramid element
         int nnodes = numNodes();
         int npvals = numPressureVals();
         
         IntegrationPoint3d pnt = new IntegrationPoint3d(nnodes, npvals);
         pnt.setWeight(1);
         pnt.setNumber(numIntegrationPoints());
         
         VectorNd shapeWeights = new VectorNd(nnodes);
         VectorNd pressureWeights = new VectorNd(npvals);
         Vector3d coords = new Vector3d();
         Vector3d dNds = new Vector3d();
         
         // use pyramid shape functions
         coords.set(0, 0, -0.5);
         pnt.setCoords(Double.NaN, Double.NaN, Double.NaN);
         for (int i=0; i<5; ++i) {
            shapeWeights.set(i, computeLinearPyramidN(i, coords));
            computeLinearPyramiddNds(dNds, i, coords);
            pnt.setShapeGrad(i, dNds);
         }
         for (int i=5; i<nnodes; ++i) {
            shapeWeights.set(i, 0);
            pnt.setShapeGrad(i, Vector3d.ZERO);
         }
         pnt.setShapeWeights (shapeWeights);

         // pressure weights
         for (int i=0; i<npvals; i++) {
            pressureWeights.set (i, getH (i, coords));
         }
         pnt.setPressureWeights (pressureWeights);
         pnt.myElemClass = ElementClass.VOLUMETRIC;
         
         myWarpingPoint = pnt;
      }
      return myWarpingPoint;
   }  

   public boolean coordsAreInside (Vector3d coords) {
      double s0 = coords.x;
      double s1 = coords.y;
      double s2 = coords.z;

      return (s0 >= -1 && s0 <= 1 && s1 >= -1 && s1 <= 1 && s2 >= -1 && s2 <= 1);
   }

   private static double computeLinearPyramidN (int i, Vector3d coords) {
      double s0 = coords.x;
      double s1 = coords.y;
      double s2 = coords.z;

      switch (i) {
         case 0: return 0.125*(1-s0)*(1-s1)*(1-s2);
         case 1: return 0.125*(1+s0)*(1-s1)*(1-s2);
         case 2: return 0.125*(1+s0)*(1+s1)*(1-s2);
         case 3: return 0.125*(1-s0)*(1+s1)*(1-s2);
         case 4: return 0.5*(1+s2);
         default:
            throw new IllegalArgumentException (
               "Shape function index must be in range [0,4]");
      }
   }

   private static void computeLinearPyramiddNds (Vector3d dNds, int i, Vector3d coords) {
      double s0 = coords.x;
      double s1 = coords.y;
      double s2 = coords.z;

      switch (i) {
         case 0: dNds.set (
            -0.125*(1-s1)*(1-s2), -0.125*(1-s0)*(1-s2), -0.125*(1-s0)*(1-s1));
            break;
         case 1: dNds.set (
            +0.125*(1-s1)*(1-s2), -0.125*(1+s0)*(1-s2), -0.125*(1+s0)*(1-s1));
            break;
         case 2: dNds.set (
            +0.125*(1+s1)*(1-s2), +0.125*(1+s0)*(1-s2), -0.125*(1+s0)*(1+s1));
            break;
         case 3: dNds.set (
            -0.125*(1+s1)*(1-s2), +0.125*(1-s0)*(1-s2), -0.125*(1-s0)*(1+s1));
            break;
         case 4: dNds.set (0, 0, 0.5);
            break;
         default:
            throw new IllegalArgumentException (
               "Shape function index must be in range [0,5]");
      }
   }
   
   private static double[] myIntegrationCoords;

   public int numIntegrationPoints() {
      return myIntegrationCoords.length/4;
   }

   static double[] myNodeCoords = new double[] 
      {
         -1, -1, -1,   // node 0  <=> Quadhex 4
         +1, -1, -1,   // node 1  <=> Quadhex 5
         +1,  1, -1,   // node 2  <=> Quadhex 6
         -1,  1, -1,   // node 3  <=> Quadhex 7
         +0,  0,  1,   // node 4  <=> Quadhex 1 + 2 + 3 + 4

         +0, -1, -1,   // node 5  <=> Quadhex 12
         +1,  0, -1,   // node 6  <=> Quadhex 13
         +0,  1, -1,   // node 7  <=> Quadhex 14
         -1,  0, -1,   // node 8  <=> Quadhex 15

         -1, -1,  0, // node 9  <=> Quadhex 16
         +1, -1,  0, // node 10 <=> Quadhex 17
         +1,  1,  0, // node 11 <=> Quadhex 18
         -1,  1,  0, // node 12 <=> Quadhex 19
      };         

   static {
      double a = (8/5.0)*Math.sqrt(2/15.0);
      double b = -2.0/3.0;
      double c = 2.0/5.0;
      double w1 = 81/100.0;
      double w2 = 125/27.0;
      myIntegrationCoords = new double[]
         {
             -a,  -a, b,  w1,
             +a,  -a, b,  w1,
             +a,   a, b,  w1,
             -a,   a, b,  w1,
              0,   0, c,  w2,
         };
   }

   public double[] getIntegrationCoords () {
      return myIntegrationCoords;
   }

   public double[] getNodeCoords() {
      return myNodeCoords;
   }

   private static double[] myNodeMassWeights = new double[] {
      // bottom corner nodes
      0.0352132,
      0.0352132,
      0.0352132,
      0.0352132,

      // tip node
      0.0250585,

      // bottom edge nodes
      0.143303,
      0.143303,
      0.143303,
      0.143303,

      // side edge nodes
      0.0652196,
      0.0652196,
      0.0652196,
      0.0652196,
   };

   public double[] getNodeMassWeights () {
      return myNodeMassWeights;
   }

   private static MatrixNd myNodalExtrapolationMatrix = null;

   public MatrixNd getNodalExtrapolationMatrix() {
      if (myNodalExtrapolationMatrix == null) {
         // set the transform parameters that map the integration
         // points onto nodes 0-4.
         double sxy = 1.0/(8/5.0)*Math.sqrt(2/15.0);
         double sz = 15.0/8;
         double bz = 2/15.0;
         Vector3d[] ncoords = getScaledNodeCoords (1, null);
         for (int i=0; i<ncoords.length; i++) {
            Vector3d v = ncoords[i];
            v.x = sxy*v.x;
            v.y = sxy*v.y;
            v.z = sz*v.z + bz;
         }
         myNodalExtrapolationMatrix =
            createNodalExtrapolationMatrix (
               ncoords, numIntegrationPoints(), new PyramidElement());
      }
      return myNodalExtrapolationMatrix;
   }

   // Shape functions are obtained from
   // pentahedral elements:
   //
   public double getN (int i, Vector3d coords) {
      double s = coords.x;
      double t = coords.y;
      double r = coords.z;
      double q = (1-r)/2;

      switch (i) {
         // case  0: return 0.125*(1-s)*(1-t)*(1+r)*(-s-t+r-2);
         // case  1: return 0.125*(1+s)*(1-t)*(1+r)*( s-t+r-2);
         // case  2: return 0.125*(1+s)*(1+t)*(1+r)*( s+t+r-2);
         // case  3: return 0.125*(1-s)*(1+t)*(1+r)*(-s+t+r-2);

         // bottom corner
         case  0: return -q*(1-s)*(1-t)*(1+q*(s+t))/4;
         case  1: return -q*(1+s)*(1-t)*(1+q*(t-s))/4;
         case  2: return -q*(1+s)*(1+t)*(1-q*(t+s))/4;
         case  3: return -q*(1-s)*(1+t)*(1-q*(t-s))/4;

         // tip
         case  4: return r*(1+r)/2;

         // bottom edges
         case  5: return q*q*(1-t)*(1-s*s)/2;
         case  6: return q*q*(1+s)*(1-t*t)/2;
         case  7: return q*q*(1+t)*(1-s*s)/2;
         case  8: return q*q*(1-s)*(1-t*t)/2;

         // side edges
         case  9: return q*(1-q)*(1-s-t+s*t);
         case 10: return q*(1-q)*(1+s-t-s*t);
         case 11: return q*(1-q)*(1+s+t+s*t);
         case 12: return q*(1-q)*(1-s+t-s*t);
         default:
            throw new IllegalArgumentException (
               "Shape function index must be in range [0,"+(numNodes()-1)+"]");
      }
   }

   public void getdNds (Vector3d dNds, int i, Vector3d coords) {
      double s = coords.x;
      double t = coords.y;
      double r = coords.z;
      double q = (1-r)/2;
      double u;

      switch (i) {
         case 0:
            dNds.set (q*(1-t)*(1+q*(2*s+t-1))/4, q*(1-s)*(1+q*(2*t+s-1))/4,
                      (1-s)*(1-t)*(1+2*q*(s+t))/8);
            break;
         case 1:
            dNds.set (-q*(1-t)*(1+q*(-2*s+t-1))/4, q*(1+s)*(1+q*(2*t-s-1))/4,
                      (1+s)*(1-t)*(1+2*q*(t-s))/8);
            break;
         case 2:
            dNds.set (-q*(1+t)*(1+q*(-2*s-t-1))/4, -q*(1+s)*(1+q*(-2*t-s-1))/4,
                      (1+s)*(1+t)*(1-2*q*(t+s))/8);
            break;
         case 3:
            dNds.set (q*(1+t)*(1+q*(2*s-t-1))/4, -q*(1-s)*(1+q*(-2*t+s-1))/4,
                      (1-s)*(1+t)*(1+2*q*(s-t))/8);
            break;
         case 4:
            dNds.set (0, 0, 0.5+r);
            break;
         case 5:
            dNds.set (-s*q*q*(1-t), -q*q*(1-s*s)/2, -q*(1-t)*(1-s*s)/2);
            break;
         case 6:
            dNds.set (q*q*(1-t*t)/2, -t*q*q*(1+s), -q*(1+s)*(1-t*t)/2);
            break;
         case 7:
            dNds.set (-s*q*q*(1+t), q*q*(1-s*s)/2, -q*(1+t)*(1-s*s)/2);
            break;
         case 8:
            dNds.set (-q*q*(1-t*t)/2, -t*q*q*(1-s), -q*(1-s)*(1-t*t)/2);
            break;
         case 9:
            u = q*(1-q);
            dNds.set (u*(t-1), u*(s-1), (2*q-1)*(1-s-t+s*t)/2);
            break;
         case 10:
            u = q*(1-q);
            dNds.set (u*(1-t), -u*(1+s), (2*q-1)*(1+s-t-s*t)/2);
            break;
         case 11:
            u = q*(1-q);
            dNds.set (u*(1+t), u*(1+s), (2*q-1)*(1+s+t+s*t)/2);
            break;
         case 12:
            u = q*(1-q);
            dNds.set (-u*(1+t), u*(1-s), (2*q-1)*(1-s+t-s*t)/2);
            break;
         default:
            throw new IllegalArgumentException (
               "Shape function index must be in range [0,"+(numNodes()-1)+"]");
      }
   }

//    public void setFaceNeighbors (QuadpyramidElement[] nbrs) {
//       myNeighbors = nbrs;
//    }

//    public QuadpyramidElement[] getFaceNeighbors() {
//       return myNeighbors;
//    }


   public static FemNode3d[] getQuadraticNodes (PyramidElement pyramid) {
      return getQuadraticNodes (
         pyramid.getNodes()[0], pyramid.getNodes()[1],
         pyramid.getNodes()[2], pyramid.getNodes()[3],
         pyramid.getNodes()[4]);
   }

   public static FemNode3d[] getQuadraticNodes (
      FemNode3d n0, FemNode3d n1, FemNode3d n2,
      FemNode3d n3, FemNode3d n4) {
   
      FemNode3d quadraticNodes[] = new FemNode3d[8];

      quadraticNodes[0] = createEdgeNode (n0, n1);
      quadraticNodes[1] = createEdgeNode (n1, n2);
      quadraticNodes[2] = createEdgeNode (n2, n3);
      quadraticNodes[3] = createEdgeNode (n3, n0);

      quadraticNodes[4] = createEdgeNode (n0, n4);
      quadraticNodes[5] = createEdgeNode (n1, n4);
      quadraticNodes[6] = createEdgeNode (n2, n4);
      quadraticNodes[7] = createEdgeNode (n3, n4);

      return quadraticNodes;
   }

   private void init (FemNode3d[] nodes) {
      myNodes = new FemNode3d[NUM_NODES];
      for (int i=0; i<NUM_NODES; i++) {
         myNodes[i] = nodes[i];
      }
   }   

   public QuadpyramidElement() {
      myNodes = new FemNode3d[NUM_NODES];
   }

   /**
    * Create a QuadpyramidElement based on the NODE POSITIONS of a given
    * PyramidElement (i.e. it does not inherit any other attributes of the
    * PyramidElement). Takes the 5 nodes of the given PyramidElement along with the
    * 8 given quadraticNodes as the middle nodes to create a 13 node quadratic
    * pyramid.
    * 
    * @param pyramid
    * A pyramid element
    * @param quadraticNodes
    * the 8 edge nodes
    */
   public QuadpyramidElement (PyramidElement pyramid, FemNode3d[] quadraticNodes) {
      FemNode3d nodes[] = new FemNode3d[15];
      for (int i=0; i<5; i++) {
         nodes[i] = pyramid.getNodes()[i];
      }
      for (int i=5; i<NUM_NODES; i++) {
         nodes[i] = quadraticNodes[i-5];
      }
      init (nodes);
   }

   public QuadpyramidElement (FemNode3d[] nodes) {
      init (nodes);
   }

   static int[] myEdgeIdxs = new int[] 
      {
         // edges are arranged so that the apex (node 4) appears last in edges
         // which contain it. renderEdges() below relies on this.
         3,   0, 5, 1,
         3,   1, 6, 2,
         3,   2, 7, 3,
         3,   3, 8, 0,   
         3,   0, 9, 4,
         3,   1, 10, 4,
         3,   2, 11, 4,
         3,   3, 12, 4,   
      };

   static int[] myFaceIdxs = new int[] 
      {
         8,   0, 8, 3, 7, 2, 6, 1, 5,
         6,   0, 5, 1, 10, 4, 9,
         6,   1, 6, 2, 11, 4, 10,
         6,   2, 7, 3, 12, 4, 11,
         6,   3, 8, 0, 9, 4, 12,
      };

   static int[] myTriangulatedFaceIdxs;

   public int[] getEdgeIndices() {
      return myEdgeIdxs;
   }

   public int[] getFaceIndices() {
      return myFaceIdxs;
   }

   public int[] getTriangulatedFaceIndices() {
      if (myTriangulatedFaceIdxs == null) {
         myTriangulatedFaceIdxs =
            FemUtilities.triangulateFaceIndices (myFaceIdxs);
      }
      return myTriangulatedFaceIdxs;
   }

   public void render(Renderer renderer, RenderProps props, int flags) {
      // Note: proper edge rendering for a pyramid needs to be handled
      // differently - the coordinate interpolation along
      // the edges leading to the apex must be handled in a special way 
      // (because the shape functions are determined from condensation 
      // and the natural coordinates correspond to a cube).
      // See the commented out function renderEdges, below
      if (myRenderer == null) {
         myRenderer= new FemElementRenderer (this);
      }
      myRenderer.render (renderer, this, props);
   }

   public void renderWidget (
      Renderer renderer, double size, RenderProps props) {
      if (myRenderer == null) {
         myRenderer= new FemElementRenderer (this);
      }
      myRenderer.renderWidget (renderer, this, size, props);
   }

   public FemNode3d[][] triangulateFace (FaceNodes3d face) {
      FemNode3d[] nodes = face.getNodes();
      if (nodes.length == 8) {
         return FemUtilities.triangulate8NodeFace (face);
      }
      else if (nodes.length == 6) {
         return FemUtilities.triangulate6NodeFace (face);
      } 
      else {
         throw new IllegalArgumentException (
            "Expecting 6 or 8 nodes, got " + nodes.length);
      }
   } 

//   /** 
//    * Need to override renderEdges because the coordinate interpolation along
//    * the edges leading to the apex must be handled in a special way (because
//    * the shape functions are determined from condensation and the natural
//    * coordinates correspond to a cube).
//    */   
//   public void renderEdges (Renderer renderer, RenderProps props) {
//      
//      int[] idxs = getEdgeIndices();
//
//      Vector3d ncoords0 = new Vector3d();
//      Vector3d ncoords1 = new Vector3d();
//      int n = 3; // all edges have three nodes
//      for (int i=0; i<idxs.length; i+=(n+1)) {
//         getNodeCoords (ncoords0, idxs[i+1]);            
//         if (idxs[i+3] == 4) {
//            // if last node is apex, use middle node and extrapolate
//            getNodeCoords (ncoords1, idxs[i+2]);            
//            ncoords1.combine (-1, ncoords0, 2, ncoords1);
//         }
//         else {
//            getNodeCoords (ncoords1, idxs[i+3]);            
//         }
//         drawQuadEdge (
//            renderer, ncoords0, ncoords1, idxs[i+1], idxs[i+2], idxs[i+3]);
//      }
//   }

}
