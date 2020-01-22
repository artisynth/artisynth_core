/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import maspack.matrix.*;
import maspack.render.*;

public class QuadtetElement extends FemElement3d {

   // interpolation matrix for doing parametric interpolation of
   // 3 points with a quadratic. This is used to draw the edges
   private static double mVals[] = { 2, -4, 2, -3, 4, -1, 1, 0, 0 };
   private static Matrix3d interpolationMatrix = new Matrix3d (mVals);

   // M is used to compute basis coordinates for marker points

   // Gaussian quadrature
   private final static double alpha = 0.58541020;
   private final static double beta = 0.13819660;

   public enum comp {
      x, y, z
   }

   private static IntegrationPoint3d[] myDefaultIntegrationPoints = null;
   private static IntegrationPoint3d myWarpingPoint;
   private static FemElementRenderer myRenderer;

   public boolean isLinear() {
      return false;
   }
   
   public IntegrationPoint3d[] getIntegrationPoints() {
      if (myDefaultIntegrationPoints == null) {
         myDefaultIntegrationPoints = 
            createIntegrationPoints(new QuadtetElement());
      }
      return myDefaultIntegrationPoints;
   }

   public IntegrationPoint3d getWarpingPoint() {
      if (myWarpingPoint == null) {
         
         // create special warping point based on linear tet element
         int nnodes = numNodes();
         int npvals = numPressureVals();
         
         IntegrationPoint3d pnt = new IntegrationPoint3d(nnodes, npvals);
         pnt.setWeight(1);
         pnt.setNumber(numIntegrationPoints());
         
         VectorNd shapeWeights = new VectorNd(nnodes);
         VectorNd pressureWeights = new VectorNd(npvals);
         Vector3d coords = new Vector3d();
         Vector3d dNds = new Vector3d();
         
         // use tet shape functions
         coords.set(0.25, 0.25, 0.25);
         pnt.setCoords(Double.NaN, Double.NaN, Double.NaN);
         for (int i=0; i<4; ++i) {
            shapeWeights.set(i, computeLinearTetN(i, coords));
            computeLinearTetdNds(dNds, i, coords);
            pnt.setShapeGrad(i, dNds);
         }
         for (int i=4; i<nnodes; ++i) {
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
   
   private static double computeLinearTetN(int i, Vector3d coords) {
      double s1 = coords.x;
      double s2 = coords.y;
      double s3 = coords.z;

      switch (i) {
         case 0: return 1 - s1 - s2 - s3;
         case 1: return s1;
         case 2: return s2;
         case 3: return s3;
         default: {
            throw new IllegalArgumentException (
               "Shape function index must be in range [0,3]");
         }
      }
   }
   
   /**
    * Computes the shape function gradient
    * @param dNds output
    * @param i    node index
    * @param coords isoparametric coordinates
    */
   private static void computeLinearTetdNds (Vector3d dNds, int i, Vector3d coords) {
      switch (i) {
         case 0: dNds.set (-1, -1, -1); break;
         case 1: dNds.set ( 1,  0,  0); break;
         case 2: dNds.set ( 0,  1,  0); break;
         case 3: dNds.set ( 0,  0,  1); break;
         default: {
            throw new IllegalArgumentException (
               "Shape function index must be in range [0,3]");
         }
      }
   }

   public static FemNode3d[] getQuadraticNodes (TetElement tet) {
      return getQuadraticNodes (
         tet.getNodes()[0], tet.getNodes()[1],
         tet.getNodes()[2], tet.getNodes()[3]);
   }

   public static FemNode3d[] getQuadraticNodes (
      FemNode3d n0, FemNode3d n1, FemNode3d n2, FemNode3d n3) {
      Point3d p0 = n0.getPosition();// new Point3d(0.0, 0.0, 0.0);
      Point3d p1 = n1.getPosition();// new Point3d(1.0, 1.0, 0.0);
      Point3d p2 = n2.getPosition();// new Point3d(-1.0, 1.0, 0.0);
      Point3d p3 = n3.getPosition();// new Point3d(0.0, 0.5, 1.0);

      Point3d p4 = new Point3d();
      p4.add (p0, p1);
      p4.scale (0.5);
      // System.out.println("p5: "+p5);

      Point3d p5 = new Point3d();
      p5.add (p1, p2);
      p5.scale (0.5);
      // System.out.println("p6: "+p6);

      Point3d p6 = new Point3d();
      p6.add (p0, p2);
      p6.scale (0.5);
      // System.out.println("p7: "+p7);

      Point3d p7 = new Point3d();
      p7.add (p0, p3);
      p7.scale (0.5);
      // System.out.println("p8: "+p8);

      Point3d p8 = new Point3d();
      p8.add (p1, p3);
      p8.scale (0.5);
      // System.out.println("p9: "+p9);

      Point3d p9 = new Point3d();
      p9.add (p2, p3);
      p9.scale (0.5);
      // System.out.println("p10: "+p10);

      FemNode3d quadraticNodes[] = new FemNode3d[6];
      quadraticNodes[0] = new FemNode3d (p4);
      quadraticNodes[1] = new FemNode3d (p5);
      quadraticNodes[2] = new FemNode3d (p6);
      quadraticNodes[3] = new FemNode3d (p7);
      quadraticNodes[4] = new FemNode3d (p8);
      quadraticNodes[5] = new FemNode3d (p9);

      return quadraticNodes;
   }

   public QuadtetElement() {
      myNodes = new FemNode3d[10];
   }

   /**
    * Create a quadratic element based on the NODE POSITIONS of a given
    * TetElement (ie. it does not inherit any other attributes of the
    * TetElement) Takes the 4 nodes of the given TetElement along with the 6
    * given quadraticNodes as the middle nodes to create a 10 node quadratic
    * tetrahedron.
    * 
    * @param tet
    * A tetrahedral element
    * @param quadraticNodes
    * 6 nodes as the middle nodes
    */
   public QuadtetElement (TetElement tet, FemNode3d[] quadraticNodes) {
      this (
         tet.getNodes()[0], tet.getNodes()[1], tet.getNodes()[2],
         tet.getNodes()[3], quadraticNodes[0], quadraticNodes[1],
         quadraticNodes[2], quadraticNodes[3], quadraticNodes[4],
         quadraticNodes[5]);
   }

    public QuadtetElement (FemNode3d[] nodes) {
      myNodes = new FemNode3d[10];
      for (int i=0; i<10; i++) {
         myNodes[i] = nodes[i];
      }
   }  

   public QuadtetElement (FemNode3d n1, FemNode3d n2, FemNode3d n3, FemNode3d n4,
                          FemNode3d n5, FemNode3d n6, FemNode3d n7, FemNode3d n8,
                          FemNode3d n9, FemNode3d n10) {
      myNodes = new FemNode3d[10];

      myNodes[0] = n1;
      myNodes[1] = n2;
      myNodes[2] = n3;
      myNodes[3] = n4;
      myNodes[4] = n5;
      myNodes[5] = n6;
      myNodes[6] = n7;
      myNodes[7] = n8;
      myNodes[8] = n9;
      myNodes[9] = n10;
   }

   public boolean coordsAreInside (Vector3d coords) {
      double s1 = coords.x;
      double s2 = coords.y;
      double s3 = coords.z;
      double s0 = 1 - s1 - s2 - s3;

      return (s0 >= 0 && s1 >= 0 && s2 >= 0 && s3 >= 0);
   }

   /**
    * Shape functions for the quad tet are
    *
    * N_0 = s0*(2*s0 - 1)
    * N_1 = s1*(2*s1 - 1)
    * N_2 = s2*(2*s2 - 1)
    * N_3 = s3*(2*s3 - 1)
    * N_4 = 4*s0*s1
    * N_5 = 4*s1*s2
    * N_6 = 4*s2*s0
    * N_7 = 4*s0*s3
    * N_8 = 4*s1*s3
    * N_9 = 4*s2*s3
    *
    * where s0 = 1 - s1 - s2 - s3
    */
   private static int myNumIntPoints = 4;

   public int numIntegrationPoints() {
      return myNumIntPoints;
   }

   private static double[] myIntegrationCoords = new double[] {
      beta,  beta,  beta,  0.25/6.0,
     alpha,  beta,  beta,  0.25/6.0,
      beta, alpha,  beta,  0.25/6.0,
      beta,  beta, alpha,  0.25/6.0
   };

   public double[] getIntegrationCoords () {
      return myIntegrationCoords;
   }

   private static MatrixNd myNodalExtrapolationMatrix = null;

   public MatrixNd getNodalExtrapolationMatrix() {
      if (myNodalExtrapolationMatrix == null) {
         // adjusting barycentric coordinates 'a' to reflect a scaling by s
         // is done by
         //
         // a' = s a + (1-s)/(n+1) e
         //
         // where a' is the new coordinates, n is the dimension of the
         // space, and e is a column vector of ones.
         //
         // In particular, for n=3, the coordinates (1, 0, 0) which describe the
         // first vertex of the simplex map onto (alpha, beta, beta), where
         //
         // alpha = (3 s + 1)/4, beta = (1-s)/4
         //          
         double s = 1/0.4472136;
         Vector3d offset = new Vector3d ((1-s)/4, (1-s)/4, (1-s)/4);
         Vector3d[] ncoords = getScaledNodeCoords (s, offset);
         myNodalExtrapolationMatrix =
            createNodalExtrapolationMatrix (ncoords, 4, new TetElement());
      }
      return myNodalExtrapolationMatrix;
   }

   public double getN (int i, Vector3d coords) {
      double s1 = coords.x;
      double s2 = coords.y;
      double s3 = coords.z;
      double s0 = 1 - s1 - s2 - s3;

      switch (i) {
         case 0: return s0*(2*s0 - 1);
         case 1: return s1*(2*s1 - 1);
         case 2: return s2*(2*s2 - 1);
         case 3: return s3*(2*s3 - 1);
         case 4: return 4*s0*s1;
         case 5: return 4*s1*s2;
         case 6: return 4*s2*s0;
         case 7: return 4*s0*s3;
         case 8: return 4*s1*s3;
         case 9: return 4*s2*s3;
         default: {
            throw new IllegalArgumentException (
               "Shape function index must be in range [0,"+(numNodes()-1)+"]");
         }
      }
   }

   public void getdNds (Vector3d dNds, int i, Vector3d coords) {
      double s1 = coords.x;
      double s2 = coords.y;
      double s3 = coords.z;
      double s0 = 1 - s1 - s2 - s3;

      switch (i) {
         case 0: dNds.set (  -4*s0+1,   -4*s0+1,   -4*s0+1); break;
         case 1: dNds.set (   4*s1-1,         0,         0); break;
         case 2: dNds.set (        0,    4*s2-1,         0); break;
         case 3: dNds.set (        0,         0,    4*s3-1); break;
         case 4: dNds.set (4*(s0-s1),     -4*s1,     -4*s1); break;
         case 5: dNds.set (     4*s2,      4*s1,         0); break;
         case 6: dNds.set (    -4*s2, 4*(s0-s2),     -4*s2); break;
         case 7: dNds.set (    -4*s3,     -4*s3, 4*(s0-s3)); break;
         case 8: dNds.set (     4*s3,         0,      4*s1); break;
         case 9: dNds.set (        0,      4*s3,      4*s2); break;
         default: {
            throw new IllegalArgumentException (
               "Shape function index must be in range [0,"+(numNodes()-1)+"]");
         }
      }
   }

   private static double[] myNodeCoords = new double[] 
      {
         0,   0,   0,
         1,   0,   0,
         0,   1,   0,
         0,   0,   1,
         0.5, 0,   0,
         0.5, 0.5, 0,
         0,   0.5, 0,
         0,   0,   0.5,
         0.5, 0,   0.5,
         0,   0.5, 0.5         
      };

   public double[] getNodeCoords () {
      return myNodeCoords;
   }

   private static double[] myNodeMassWeights = new double[] {
      // masses obtained by scaling the consistent mass matrix diagonal:
      // corner nodes
      0.0277778,
      0.0277778,
      0.0277778,
      0.0277778,

      // side nodes
      0.148148,
      0.148148,
      0.148148,
      0.148148,
      0.148148,
      0.148148

      // masses obtained by summing the consistent mass matrix:
      // // corner nodes
      // -0.035714285714286,
      // -0.035714285714286,
      // -0.035714285714286,
      // -0.035714285714286,

      // // side nodes
      // 0.142857142857143,
      // 0.142857142857143,
      // 0.142857142857143,
      // 0.142857142857143,
      // 0.142857142857143,
      // 0.142857142857143,
   };

   public double[] getNodeMassWeights () {
      return myNodeMassWeights;
   }

   static int[] myEdgeIdxs = new int[] 
      {
         3,   0, 4, 1,
         3,   0, 6, 2,
         3,   0, 7, 3,
         3,   1, 5, 2,
         3,   1, 8, 3,
         3,   2, 9, 3
      };

   static int[] myFaceIdxs = new int[] 
      {
         6,   0, 6, 2, 5, 1, 4,
         6,   1, 8, 3, 7, 0, 4,
         6,   2, 9, 3, 8, 1, 5,
         6,   3, 9, 2, 6, 0, 7
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
      if (nodes.length != 6) {
         throw new IllegalArgumentException (
            "Expecting 6 nodes, got " + nodes.length);
      }
      FemNode3d[][] triangles = new FemNode3d[4][3];

      if (nodes[1].distance(nodes[4]) < nodes[5].distance(nodes[3])) {
         setTriangle (triangles[0], nodes[0], nodes[1], nodes[5]);
         setTriangle (triangles[1], nodes[1], nodes[4], nodes[5]);
         setTriangle (triangles[2], nodes[1], nodes[3], nodes[4]);
         setTriangle (triangles[3], nodes[1], nodes[2], nodes[3]);
      }
      else {
         setTriangle (triangles[0], nodes[0], nodes[1], nodes[5]);
         setTriangle (triangles[1], nodes[1], nodes[3], nodes[5]);
         setTriangle (triangles[2], nodes[3], nodes[4], nodes[5]);
         setTriangle (triangles[3], nodes[1], nodes[2], nodes[3]);
      }
      return triangles;
   }   

}
