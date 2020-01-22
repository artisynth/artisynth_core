/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import java.util.*;

import artisynth.core.mechmodels.*;
import maspack.matrix.*;
import maspack.util.*;
import maspack.render.Renderer;
import maspack.render.RenderProps;

public class QuadwedgeElement extends FemElement3d {

   private static IntegrationPoint3d[] myDefaultIntegrationPoints;
   private static IntegrationPoint3d myWarpingPoint;
   private static FemElementRenderer myRenderer;

   private static Matrix2d myPressureWeightMatrix;

   public boolean isLinear() {
      return false;
   }
   
   public IntegrationPoint3d[] getIntegrationPoints() {
      if (myDefaultIntegrationPoints == null) {
         myDefaultIntegrationPoints = 
            createIntegrationPoints (new QuadwedgeElement());
      }
      return myDefaultIntegrationPoints;
   }

   public IntegrationPoint3d getWarpingPoint() {
      if (myWarpingPoint == null) {
         // create special warping point based on linear wedge element
         int nnodes = numNodes();
         int npvals = numPressureVals();
         
         IntegrationPoint3d pnt = new IntegrationPoint3d(nnodes, npvals);
         pnt.setWeight(1);
         pnt.setNumber(numIntegrationPoints());
         
         VectorNd shapeWeights = new VectorNd(nnodes);
         VectorNd pressureWeights = new VectorNd(npvals);
         Vector3d coords = new Vector3d();
         Vector3d dNds = new Vector3d();
         
         // use wedge shape functions
         coords.set(1/3.0, 1/3.0, 0);
         pnt.setCoords(Double.NaN, Double.NaN, Double.NaN);
         for (int i=0; i<6; ++i) {
            shapeWeights.set(i, computeLinearWedgeN(i, coords));
            computeLinearWedgedNds(dNds, i, coords);
            pnt.setShapeGrad(i, dNds);
         }
         for (int i=6; i<nnodes; ++i) {
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
   
   private static double computeLinearWedgeN (int i, Vector3d coords) {
      double s1 = coords.x;
      double s2 = coords.y;
      double r = coords.z;

      switch (i) {
         case 0: return 0.5*(1-s1-s2)*(1-r);
         case 1: return 0.5*s1*(1-r);
         case 2: return 0.5*s2*(1-r);
         case 3: return 0.5*(1-s1-s2)*(1+r);
         case 4: return 0.5*s1*(1+r);
         case 5: return 0.5*s2*(1+r);
         default:
            throw new IllegalArgumentException (
               "Shape function index must be in range [0,5]");
      }
   }

   private static void computeLinearWedgedNds (Vector3d dNds, int i, Vector3d coords) {
      double s1 = coords.x;
      double s2 = coords.y;
      double r = coords.z;

      switch (i) {
         case 0: dNds.set ( -0.5*(1-r), -0.5*(1-r), -0.5*(1-s1-s2)); break;
         case 1: dNds.set (  0.5*(1-r),          0, -0.5*s1); break;
         case 2: dNds.set (          0,  0.5*(1-r), -0.5*s2); break;
         case 3: dNds.set ( -0.5*(1+r), -0.5*(1+r),  0.5*(1-s1-s2)); break;
         case 4: dNds.set (  0.5*(1+r),          0,  0.5*s1); break;
         case 5: dNds.set (          0,  0.5*(1+r),  0.5*s2); break;
         default:
            throw new IllegalArgumentException (
               "Shape function index must be in range [0,5]");
      }
   }

   public boolean coordsAreInside (Vector3d coords) {
      double s1 = coords.x;
      double s2 = coords.y;
      double  r = coords.z;

      return (s1 >= 0 && s1 <= 1 && s2 >= 0 && s2 <= 1 && r >= -1 && r <= 1 &&
              (s1+s2) >= 0 && (s1+s2) <= 1 );
   }

   private static double[] myIntegrationCoords;

   public int numIntegrationPoints() {
      return myIntegrationCoords.length/4;
   }

   static double[] myNodeCoords = new double[] 
      {
         0, 0, -1,
         1, 0, -1,
         0, 1, -1,
         0, 0,  1,
         1, 0,  1,
         0, 1,  1,

         0.5, 0,   -1,
         0.5, 0.5, -1,
         0,   0.5, -1,

         0.5, 0,    1,
         0.5, 0.5,  1,
         0,   0.5,  1,

         0, 0,  0,
         1, 0,  0,
         0, 1,  0,
      };         


   static {
      double a = 1.0/6.0;
      double b = 2.0/3.0;
      double q = Math.sqrt(3/5.0); // quadrature point
      double w1 = 5/54.0;
      double w2 = 8/54.0;
      myIntegrationCoords = new double[]
         {
             a,  a, -q,  w1,
             b,  a, -q,  w1,
             a,  b, -q,  w1,
             a,  a,  q,  w1,
             b,  a,  q,  w1,
             a,  b,  q,  w1,
             a,  a,  0,  w2,
             b,  a,  0,  w2,
             a,  b,  0,  w2,
         };
   }

   public double[] getIntegrationCoords () {
      return myIntegrationCoords;
   }

   public double[] getNodeCoords() {
      return myNodeCoords;
   }

   private static double[] myNodeMassWeights = new double[] {
      // corner nodes
      0.0294118,
      0.0294118,
      0.0294118,
      0.0294118,
      0.0294118,
      0.0294118,

      // side edge nodes
      0.0784314,
      0.0784314,
      0.0784314,
      0.0784314,
      0.0784314,
      0.0784314,

      // middle edge nodes
      0.117647,
      0.117647,
      0.117647
   };

   public double[] getNodeMassWeights () {
      return myNodeMassWeights;
   }

   private static MatrixNd myNodalExtrapolationMatrix = null;

   public MatrixNd getNodalExtrapolationMatrix() {
      if (myNodalExtrapolationMatrix == null) {
         // nodal coordinates for the wedge are a hybrid of barycentric
         // and euclidean. See the comment in QuadtetElement on transforming
         // barycentric coordinates to reflect scaling.
         double s0 = 2; // triangles are scaled by 2
         double s1 = 1/Math.sqrt(3/5.0);
         Vector3d[] ncoords = getScaledNodeCoords (1, null);
         for (int i=0; i<ncoords.length; i++) {
            Vector3d v = ncoords[i];
            v.x = v.x*s0 + (1-s0)/3;
            v.y = v.y*s0 + (1-s0)/3;
            v.z = v.z*s1;
         }
         myNodalExtrapolationMatrix =
            createNodalExtrapolationMatrix (
               ncoords, numIntegrationPoints(), this);
      }
      return myNodalExtrapolationMatrix;
   }

   // Shape functions are the same as those used by FEBio for
   // pentahedral elements:
   //
   public double getN (int i, Vector3d coords) {
      if (i < 0 || i >= numNodes()) {
         throw new IllegalArgumentException (
            "Shape function index must be in range [0,"+(numNodes()-1)+"]");
      }
      double s1 = coords.x;
      double s2 = coords.y;
      double r = coords.z;

      switch (i) {
         // corners
         case 0: return -0.5*(1-s1-s2)*(1-r)*(2*s1+2*s2+r);
         case 1: return 0.5*s1*(1-r)*(2*s1-r-2);
         case 2: return 0.5*s2*(1-r)*(2*s2-r-2);
         case 3: return -0.5*(1-s1-s2)*(1+r)*(2*s1+2*s2-r);
         case 4: return 0.5*s1*(1+r)*(2*s1+r-2);
         case 5: return 0.5*s2*(1+r)*(2*s2+r-2);

         // side edges
         case 6: return 2*s1*(1-s1-s2)*(1-r);
         case 7: return 2*s1*s2*(1-r);
         case 8: return 2*s2*(1-s1-s2)*(1-r);
         case 9: return 2*s1*(1-s1-s2)*(1+r);
         case 10: return 2*s1*s2*(1+r);
         case 11: return 2*s2*(1-s1-s2)*(1+r);

         // middle edges
         case 12: return (1-s1-s2)*(1-r*r);
         case 13: return s1*(1-r*r);
         case 14: return s2*(1-r*r);
         default:
            throw new IllegalArgumentException (
               "Shape function index must be in range [0,"+(numNodes()-1)+"]");
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int numPressureVals() {
      return 2;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public double getH (int i, Vector3d coords) {

      double r = coords.z;

      switch (i) {
         case  0: return 0.5*(1-r);
         case  1: return 0.5*(1+r);
         // case  0: return 1;
         // case  1: return s;
         // case  2: return t;
         // case  3: return r;
         default: {
            throw new IllegalArgumentException (
               "Shape function index must be in range [0,1]");
         }
      }
   }

   @Override
   public Matrix getPressureWeightMatrix () {
      if (myPressureWeightMatrix == null) {
         myPressureWeightMatrix = (Matrix2d)createPressureWeightMatrix();
      }
      return myPressureWeightMatrix;
   }

   public void getdNds (Vector3d dNds, int i, Vector3d coords) {
      double s1 = coords.x;
      double s2 = coords.y;
      double r = coords.z;
      double s0 = 1-s1-s2;

      switch (i) {
         case 0: dNds.set ((1-r)*(4*s1+4*s2+r-2)/2,
                           (1-r)*(4*s1+4*s2+r-2)/2,
                           s0*(2*s1+2*s2+2*r-1)/2); break;
         case 1: dNds.set ((1-r)*(4*s1-r-2)/2, 0, -s1*(2*s1-2*r-1)/2); break;
         case 2: dNds.set (0, (1-r)*(4*s2-r-2)/2, -s2*(2*s2-2*r-1)/2); break;
         case 3: dNds.set ((1+r)*(4*s1+4*s2-r-2)/2,
                           (1+r)*(4*s1+4*s2-r-2)/2,
                           s0*(1-2*s1-2*s2+2*r)/2); break;
         case 4: dNds.set ((1+r)*(4*s1+r-2)/2, 0, s1*(2*s1+2*r-1)/2); break;
         case 5: dNds.set (0, (1+r)*(4*s2+r-2)/2, s2*(2*s2+2*r-1)/2); break;

         case 6: dNds.set (2*(s0-s1)*(1-r),     -2*s1*(1-r), -2*s1*s0); break;
         case 7: dNds.set (     2*s2*(1-r),      2*s1*(1-r), -2*s1*s2); break;
         case 8: dNds.set (    -2*s2*(1-r), 2*(s0-s2)*(1-r), -2*s2*s0); break;
         case 9: dNds.set (2*(s0-s1)*(1+r),     -2*s1*(1+r), 2*s1*s0); break;
         case 10: dNds.set (    2*s2*(1+r),      2*s1*(1+r), 2*s1*s2); break;
         case 11: dNds.set (   -2*s2*(1+r), 2*(s0-s2)*(1+r), 2*s2*s0); break;
         case 12: dNds.set (         r*r-1,           r*r-1, -2*r*s0); break;
         case 13: dNds.set (         1-r*r,               0, -2*r*s1); break;
         case 14: dNds.set (             0,           1-r*r, -2*r*s2); break;

         default:
            throw new IllegalArgumentException (
               "Shape function index must be in range [0,"+(numNodes()-1)+"]");
      }
   }


   public static FemNode3d[] getQuadraticNodes (WedgeElement wedge) {
      return getQuadraticNodes (
         wedge.getNodes()[0], wedge.getNodes()[1],
         wedge.getNodes()[2], wedge.getNodes()[3],
         wedge.getNodes()[4], wedge.getNodes()[5]);
   }

   public static FemNode3d[] getQuadraticNodes (
      FemNode3d n0, FemNode3d n1, FemNode3d n2,
      FemNode3d n3, FemNode3d n4, FemNode3d n5) {
   
      FemNode3d quadraticNodes[] = new FemNode3d[9];

      quadraticNodes[0] = createEdgeNode (n0, n1);
      quadraticNodes[1] = createEdgeNode (n1, n2);
      quadraticNodes[2] = createEdgeNode (n2, n0);

      quadraticNodes[3] = createEdgeNode (n3, n4);
      quadraticNodes[4] = createEdgeNode (n4, n5);
      quadraticNodes[5] = createEdgeNode (n5, n3);

      quadraticNodes[6] = createEdgeNode (n0, n3);
      quadraticNodes[7] = createEdgeNode (n1, n4);
      quadraticNodes[8] = createEdgeNode (n2, n5);

      return quadraticNodes;
   }
   
   private void init (FemNode3d[] nodes) {
      myNodes = new FemNode3d[15];
      for (int i=0; i<15; i++) {
         myNodes[i] = nodes[i];
      }
   }   

   public QuadwedgeElement() {
      myNodes = new FemNode3d[15];
   }

  /**
    * Create a QuadwedgeElement based on the NODE POSITIONS of a given
    * WedgeElement (i.e. it does not inherit any other attributes of the
    * WedgeElement). Takes the 6 nodes of the given WedgeElement along with the
    * 9 given quadraticNodes as the middle nodes to create a 15 node quadratic
    * wedge.
    * 
    * @param wedge
    * A wedge element
    * @param quadraticNodes
    * the 9 edge nodes
    */
   public QuadwedgeElement (WedgeElement wedge, FemNode3d[] quadraticNodes) {
      FemNode3d nodes[] = new FemNode3d[15];
      for (int i=0; i<6; i++) {
         nodes[i] = wedge.getNodes()[i];
      }
      for (int i=6; i<15; i++) {
         nodes[i] = quadraticNodes[i-6];
      }
      init (nodes);
   }

   public QuadwedgeElement (FemNode3d[] nodes) {
      init (nodes);
   }

   static int[] myEdgeIdxs = new int[] 
      {
         3,   0, 6, 1,
         3,   0, 8, 2,
         3,   1, 7, 2,
         3,   3, 9, 4,
         3,   3, 11, 5,
         3,   4, 10, 5,
         3,   0, 12, 3,
         3,   1, 13, 4,
         3,   2, 14, 5
      };

   static int[] myFaceIdxs = new int[] 
      {
         6,   0, 8, 2, 7, 1, 6, 
         6,   3, 9, 4, 10, 5, 11,
         8,   0, 6, 1, 13, 4, 9, 3, 12,
         8,   1, 7, 2, 14, 5, 10, 4, 13,
         8,   2, 8, 0, 12, 3, 11, 5, 14,
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

}
