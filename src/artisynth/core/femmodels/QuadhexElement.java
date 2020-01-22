/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import maspack.matrix.*;
import maspack.render.*;

public class QuadhexElement extends FemElement3d {

   private static int myNumIntPoints = 14;

   static double[] myIntegrationCoords8;
   static double[] myIntegrationCoords14;
   static double[] myIntegrationCoords27;

   private static IntegrationPoint3d[] myDefaultIntegrationPoints = null;
   private static IntegrationPoint3d myWarpingPoint;
   private static FemElementRenderer myRenderer;
   private static Matrix4d myPressureWeightMatrix;

   public boolean isLinear() {
      return false;
   }
   
   public IntegrationPoint3d[] getIntegrationPoints() {
      if (myDefaultIntegrationPoints == null) {
         myDefaultIntegrationPoints = 
            createIntegrationPoints (new QuadhexElement());
      }
      return myDefaultIntegrationPoints;
   }

   public IntegrationPoint3d getWarpingPoint() {
      if (myWarpingPoint == null) {
         
         // create special warping point based on linear hex element
         int nnodes = numNodes();
         int npvals = numPressureVals();
         
         IntegrationPoint3d pnt = new IntegrationPoint3d(nnodes, npvals);
         pnt.setWeight(1);
         pnt.setNumber(numIntegrationPoints());
         
         VectorNd shapeWeights = new VectorNd(nnodes);
         VectorNd pressureWeights = new VectorNd(npvals);
         Vector3d coords = new Vector3d();
         Vector3d dNds = new Vector3d();
         
         // use hex shape functions
         coords.set(0, 0, 0);
         pnt.setCoords(Double.NaN, Double.NaN, Double.NaN);
         for (int i=0; i<8; ++i) {
            shapeWeights.set(i, computeLinearHexN(i, coords));
            computeLinearHexdNds(dNds, i, coords);
            pnt.setShapeGrad(i, dNds);
         }
         for (int i=8; i<nnodes; ++i) {
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
   
   // sign of node coordinate k at node i
   private static double nodeSgn (int i, int k) {
      double c = myNodeCoords[i*3+k];
      return c < 0 ? -1 : 1;
   }
   
   private static double computeLinearHexN (int i, Vector3d coords) {
      if (i < 0 || i >= 8) {
         throw new IllegalArgumentException (
            "Shape function index must be in range [0,7]");
      }
      double s0 = coords.x;
      double s1 = coords.y;
      double s2 = coords.z;

      return 0.125*(1+nodeSgn(i,0)*s0)*(1+nodeSgn(i,1)*s1)*(1+nodeSgn(i,2)*s2);

   }
   
   private static void computeLinearHexdNds (Vector3d dNds, int i, Vector3d coords) {
      if (i < 0 || i >= 8) {
         throw new IllegalArgumentException (
            "Shape function index must be in range [0,"+(8-1)+"]");
      }
      double s0 = coords.x;
      double s1 = coords.y;
      double s2 = coords.z;

      double sgn0 = nodeSgn(i,0);
      double sgn1 = nodeSgn(i,1);
      double sgn2 = nodeSgn(i,2);

      dNds.x = 0.125*(sgn0)*(1+sgn1*s1)*(1+sgn2*s2);
      dNds.y = 0.125*(1+sgn0*s0)*(sgn1)*(1+sgn2*s2);
      dNds.z = 0.125*(1+sgn0*s0)*(1+sgn1*s1)*(sgn2);
   }

   static double[] myNodeCoords = new double[] 
      {
         -1, -1,  1,  // node 0
         +1, -1,  1,  // node 1
         +1,  1,  1,  // node 2
         -1,  1,  1,  // node 3
         -1, -1, -1,  // node 4
         +1, -1, -1,  // node 5
         +1,  1, -1,  // node 6
         -1,  1, -1,  // node 7

         +0, -1,  1,  // node 8
         +1,  0,  1,  // node 9
         +0,  1,  1,  // node 10
         -1,  0,  1,  // node 11

         +0, -1, -1,  // node 12
         +1,  0, -1,  // node 13
         +0,  1, -1,  // node 14
         -1,  0, -1,  // node 15

         -1, -1,  0,  // node 16
         +1, -1,  0,  // node 17
         +1,  1,  0,  // node 18
         -1,  1,  0,  // node 19
      };         

   static {
      double q = 1/Math.sqrt(3); // quadrature point
      myIntegrationCoords8 = new double[]
         {
            -q, -q,  q,  1,
            +q, -q,  q,  1,
            +q,  q,  q,  1,
            -q,  q,  q,  1,
            -q, -q, -q,  1,
            +q, -q, -q,  1,
            +q,  q, -q,  1,
            -q,  q, -q,  1,
         };
   }

   static {
      double q = 0.758686910639328;
      double wc = 0.335180055401662;

      double r = 0.795822425754222;
      double wf = 0.886426592797784;

      myIntegrationCoords14 = new double[]
         {
            -q, -q,  q,  wc,
            +q, -q,  q,  wc,
            +q,  q,  q,  wc,
            -q,  q,  q,  wc,
            -q, -q, -q,  wc,
            +q, -q, -q,  wc,
            +q,  q, -q,  wc,
            -q,  q, -q,  wc,
            -r,  0,  0,  wf,
            +r,  0,  0,  wf,
             0, -r,  0,  wf,
             0, +r,  0,  wf,
             0,  0, -r,  wf,
             0,  0, +r,  wf,
         };
   }   

   static {
      double q = Math.sqrt(3/5.0);
      double w1 = (5/9.0)*(5/9.0)*(5/9.0);
      double w2 = (8/9.0)*(5/9.0)*(5/9.0);
      double w3 = (8/9.0)*(8/9.0)*(5/9.0);
      double w4 = (8/9.0)*(8/9.0)*(8/9.0);

      myIntegrationCoords27 = new double[]
         {
            -q, -q,  q,  w1,
            +q, -q,  q,  w1,
            +q,  q,  q,  w1,
            -q,  q,  q,  w1,
            -q, -q, -q,  w1,
            +q, -q, -q,  w1,
            +q,  q, -q,  w1,
            -q,  q, -q,  w1,

            +0, -q,  q,  w2,
            +q,  0,  q,  w2,
            +0,  q,  q,  w2,
            -q,  0,  q,  w2,

            +0, -q, -q,  w2,
            +q,  0, -q,  w2,
            +0,  q, -q,  w2,
            -q,  0, -q,  w2,

            -q, -q,  0,  w2,
            +q, -q,  0,  w2,
            +q,  q,  0,  w2,
            -q,  q,  0,  w2,

            +0,  0,  q,  w3,             
            +0,  0, -q,  w3, 
            +0, -q,  0,  w3, 
            +q,  0,  0,  w3, 
            +0,  q,  0,  w3, 
            -q,  0,  0,  w3, 

            +0,  0,  0,  w4
         };
   }   

   public static FemNode3d[] getQuadraticNodes (HexElement hex) {
      return getQuadraticNodes (
         hex.getNodes()[0], hex.getNodes()[1],
         hex.getNodes()[2], hex.getNodes()[3],
         hex.getNodes()[4], hex.getNodes()[5],
         hex.getNodes()[6], hex.getNodes()[7]);
   }

   public static FemNode3d[] getQuadraticNodes (
      FemNode3d n0, FemNode3d n1, FemNode3d n2, FemNode3d n3,
      FemNode3d n4, FemNode3d n5, FemNode3d n6, FemNode3d n7) {

      FemNode3d quadraticNodes[] = new FemNode3d[12];

      quadraticNodes[0] = createEdgeNode (n0, n1);
      quadraticNodes[1] = createEdgeNode (n1, n2);
      quadraticNodes[2] = createEdgeNode (n2, n3);
      quadraticNodes[3] = createEdgeNode (n3, n0);

      quadraticNodes[4] = createEdgeNode (n4, n5);
      quadraticNodes[5] = createEdgeNode (n5, n6);
      quadraticNodes[6] = createEdgeNode (n6, n7);
      quadraticNodes[7] = createEdgeNode (n7, n4);

      quadraticNodes[8] = createEdgeNode (n0, n4);
      quadraticNodes[9] = createEdgeNode (n1, n5);
      quadraticNodes[10] = createEdgeNode (n2, n6);
      quadraticNodes[11] = createEdgeNode (n3, n7);

      return quadraticNodes;
   }
   
   private void init (FemNode3d[] nodes) {
      myNodes = new FemNode3d[20];
      for (int i=0; i<20; i++) {
         myNodes[i] = nodes[i];
      }
   }

   /**
    * Create a QuadhexElement based on the NODE POSITIONS of a given
    * HexElement (i.e. it does not inherit any other attributes of the
    * HexElement). Takes the 8 nodes of the given HexElement along with the 12
    * given quadraticNodes as the middle nodes to create a 20 node quadratic
    * hexahedron.
    * 
    * @param hex
    * A hexahedral element
    * @param quadraticNodes
    * the 12 edge nodes
    */
   public QuadhexElement (HexElement hex, FemNode3d[] quadraticNodes) {
      FemNode3d nodes[] = new FemNode3d[20];
      for (int i=0; i<8; i++) {
         nodes[i] = hex.getNodes()[i];
      }
      for (int i=8; i<20; i++) {
         nodes[i] = quadraticNodes[i-8];
      }
      init (nodes);
   }

   public QuadhexElement (FemNode3d[] nodes) {
      init (nodes);
   }
   
   public QuadhexElement() {
      myNodes = new FemNode3d[20];
   }

   public boolean coordsAreInside (Vector3d coords) {
      double s0 = coords.x;
      double s1 = coords.y;
      double s2 = coords.z;

      return (s0 >= -1 && s0 <= 1 && s1 >= -1 && s1 <= 1 && s2 >= -1 && s2 <= 1);
   }

   public int numIntegrationPoints() {
      return myNumIntPoints;
   }

   public double[] getIntegrationCoords () {
      if (myNumIntPoints == 8) {
         return myIntegrationCoords8;
      }
      else if (myNumIntPoints == 14) {
         return myIntegrationCoords14;         
      }
      else {
         return myIntegrationCoords27;
      }
   }

   private static MatrixNd myNodalExtrapolationMatrix8 = null;
   static {
      myNodalExtrapolationMatrix8 = new MatrixNd (20,8);
      myNodalExtrapolationMatrix8.set (new double[] 
      {
       1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 
       0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 
       0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 
       0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 
       0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 
       0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 
       0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 
       0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 
       0.5, 0.5, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 
       0.0, 0.5, 0.5, 0.0, 0.0, 0.0, 0.0, 0.0, 
       0.0, 0.0, 0.5, 0.5, 0.0, 0.0, 0.0, 0.0, 
       0.5, 0.0, 0.0, 0.5, 0.0, 0.0, 0.0, 0.0, 
       0.0, 0.0, 0.0, 0.0, 0.5, 0.5, 0.0, 0.0, 
       0.0, 0.0, 0.0, 0.0, 0.0, 0.5, 0.5, 0.0, 
       0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.5, 0.5, 
       0.0, 0.0, 0.0, 0.0, 0.5, 0.0, 0.0, 0.5,
       0.5, 0.0, 0.0, 0.0, 0.5, 0.0, 0.0, 0.0, 
       0.0, 0.5, 0.0, 0.0, 0.0, 0.5, 0.0, 0.0, 
       0.0, 0.0, 0.5, 0.0, 0.0, 0.0, 0.5, 0.0, 
       0.0, 0.0, 0.0, 0.5, 0.0, 0.0, 0.0, 0.5
      });
   }

   private static MatrixNd myNodalExtrapolationMatrix14 = null;
   static {
      myNodalExtrapolationMatrix14 = new MatrixNd (20, 14);
      myNodalExtrapolationMatrix14.set (new double[] 
      {
       1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
       0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 
       0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 
       0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 
       0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 
       0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 
       0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 
       0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 
       0.5, 0.5, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 
       0.0, 0.5, 0.5, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 
       0.0, 0.0, 0.5, 0.5, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 
       0.5, 0.0, 0.0, 0.5, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 
       0.0, 0.0, 0.0, 0.0, 0.5, 0.5, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 
       0.0, 0.0, 0.0, 0.0, 0.0, 0.5, 0.5, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 
       0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.5, 0.5, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 
       0.0, 0.0, 0.0, 0.0, 0.5, 0.0, 0.0, 0.5, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
       0.5, 0.0, 0.0, 0.0, 0.5, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 
       0.0, 0.5, 0.0, 0.0, 0.0, 0.5, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 
       0.0, 0.0, 0.5, 0.0, 0.0, 0.0, 0.5, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 
       0.0, 0.0, 0.0, 0.5, 0.0, 0.0, 0.0, 0.5, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0
      });
   }

   private static MatrixNd myNodalExtrapolationMatrix27;   

   public MatrixNd getNodalExtrapolationMatrix() {
      if (myNumIntPoints == 27) {
         if (myNodalExtrapolationMatrix27 == null) {
            myNodalExtrapolationMatrix27 = new MatrixNd(20, 27);
            myNodalExtrapolationMatrix27.setIdentity();
         }
         return myNodalExtrapolationMatrix27;
      }
      else if (myNumIntPoints == 14) {
         return myNodalExtrapolationMatrix14;
      }
      else {
         return myNodalExtrapolationMatrix8;
      }
   }

   public double getN (int i, Vector3d coords) {

      double s = coords.x;
      double t = coords.y;
      double r = coords.z;

      switch (i) {
         case  0: return 0.125*(1-s)*(1-t)*(1+r)*(-s-t+r-2);
         case  1: return 0.125*(1+s)*(1-t)*(1+r)*( s-t+r-2);
         case  2: return 0.125*(1+s)*(1+t)*(1+r)*( s+t+r-2);
         case  3: return 0.125*(1-s)*(1+t)*(1+r)*(-s+t+r-2);
         case  4: return 0.125*(1-s)*(1-t)*(1-r)*(-s-t-r-2);
         case  5: return 0.125*(1+s)*(1-t)*(1-r)*( s-t-r-2);
         case  6: return 0.125*(1+s)*(1+t)*(1-r)*( s+t-r-2);
         case  7: return 0.125*(1-s)*(1+t)*(1-r)*(-s+t-r-2);
         case  8: return 0.250*(1-s*s)*(1-t)*(1+r);
         case  9: return 0.250*(1+s)*(1-t*t)*(1+r);
         case 10: return 0.250*(1-s*s)*(1+t)*(1+r);
         case 11: return 0.250*(1-s)*(1-t*t)*(1+r);
         case 12: return 0.250*(1-s*s)*(1-t)*(1-r);
         case 13: return 0.250*(1+s)*(1-t*t)*(1-r);
         case 14: return 0.250*(1-s*s)*(1+t)*(1-r);
         case 15: return 0.250*(1-s)*(1-t*t)*(1-r);
         case 16: return 0.250*(1-s)*(1-t)*(1-r*r);
         case 17: return 0.250*(1+s)*(1-t)*(1-r*r);
         case 18: return 0.250*(1+s)*(1+t)*(1-r*r);
         case 19: return 0.250*(1-s)*(1+t)*(1-r*r);
         default: {
            throw new IllegalArgumentException (
               "Shape function index must be in range [0,"+(numNodes()-1)+"]");
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int numPressureVals() {
      return 4;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public double getH (int i, Vector3d coords) {

      double s = coords.x;
      double t = coords.y;
      double r = coords.z;

      switch (i) {
         // case  0: return 0.25*(1-t)*(1-r);
         // case  1: return 0.25*(1+t)*(1-r);
         // case  2: return 0.25*(1+s)*(1+r);
         // case  3: return 0.25*(1-s)*(1+r);

         case  0: return 1;
         case  1: return s;
         case  2: return t;
         case  3: return r;
         default: {
            throw new IllegalArgumentException (
               "Shape function index must be in range [0,3]");
         }
      }
   }

   @Override
   public Matrix getPressureWeightMatrix () {
      if (myPressureWeightMatrix == null) {
         myPressureWeightMatrix = (Matrix4d)createPressureWeightMatrix();
      }
      return myPressureWeightMatrix;
   }

   public void getdNds (Vector3d dNds, int i, Vector3d coords) {
      double s = coords.x;
      double t = coords.y;
      double r = coords.z;
      double u;

      switch (i) {
         case 0:
            u = (s+t-r+1);
            dNds.set ((1-t)*(1+r)*(s+u), (1-s)*(1+r)*(t+u), (1-s)*(1-t)*(r-u));
            dNds.scale (0.125);
            break;
         case 1:
            u = (s-t+r-1);
            dNds.set ((1-t)*(1+r)*(s+u), (1+s)*(1+r)*(t-u), (1+s)*(1-t)*(r+u));
            dNds.scale (0.125);
            break;
         case 2:
            u = (s+t+r-1);
            dNds.set ((1+t)*(1+r)*(s+u), (1+s)*(1+r)*(t+u), (1+s)*(1+t)*(r+u));
            dNds.scale (0.125);
            break;
         case 3:
            u = (s-t-r+1);
            dNds.set ((1+t)*(1+r)*(s+u), (1-s)*(1+r)*(t-u), (1-s)*(1+t)*(r-u));
            dNds.scale (0.125);
            break;
         case 4:
            u = (s+t+r+1);
            dNds.set ((1-t)*(1-r)*(s+u), (1-s)*(1-r)*(t+u), (1-s)*(1-t)*(r+u));
            dNds.scale (0.125);
            break;
         case 5:
            u = (s-t-r-1);
            dNds.set ((1-t)*(1-r)*(s+u), (1+s)*(1-r)*(t-u), (1+s)*(1-t)*(r-u));
            dNds.scale (0.125);
            break;
         case 6:
            u = (s+t-r-1);
            dNds.set ((1+t)*(1-r)*(s+u), (1+s)*(1-r)*(t+u), (1+s)*(1+t)*(r-u));
            dNds.scale (0.125);
            break;
         case 7:
            u = (s-t+r+1);
            dNds.set ((1+t)*(1-r)*(s+u), (1-s)*(1-r)*(t-u), (1-s)*(1+t)*(r+u));
            dNds.scale (0.125);
            break;
         case 8:
            dNds.set (-.5*s*(1-t)*(1+r), -.25*(1-s*s)*(1+r), .25*(1-s*s)*(1-t));
            break;
         case 9:
            dNds.set (.25*(1-t*t)*(1+r), -.5*t*(1+s)*(1+r), .25*(1+s)*(1-t*t));
            break;
         case 10:
            dNds.set (-.5*s*(1+t)*(1+r), .25*(1-s*s)*(1+r), .25*(1-s*s)*(1+t));
            break;
         case 11:
            dNds.set (-.25*(1-t*t)*(1+r), -.5*t*(1-s)*(1+r), .25*(1-s)*(1-t*t));
            break;
         case 12:
            dNds.set (-.5*s*(1-t)*(1-r), -.25*(1-s*s)*(1-r), -.25*(1-s*s)*(1-t));
            break;
         case 13:
            dNds.set (.25*(1-t*t)*(1-r), -.5*t*(1+s)*(1-r), -.25*(1+s)*(1-t*t));
            break;
         case 14:
            dNds.set (-.5*s*(1+t)*(1-r), .25*(1-s*s)*(1-r), -.25*(1-s*s)*(1+t));
            break;
         case 15:
            dNds.set (-.25*(1-t*t)*(1-r), -.5*t*(1-s)*(1-r), -.25*(1-s)*(1-t*t));
            break;
         case 16:
            dNds.set (-.25*(1-t)*(1-r*r), -.25*(1-s)*(1-r*r), -.5*r*(1-s)*(1-t));
            break;
         case 17:
            dNds.set (.25*(1-t)*(1-r*r), -.25*(1+s)*(1-r*r), -.5*r*(1+s)*(1-t));
            break;
         case 18:
            dNds.set (.25*(1+t)*(1-r*r), .25*(1+s)*(1-r*r), -.5*r*(1+s)*(1+t));
            break;
         case 19:
            dNds.set (-.25*(1+t)*(1-r*r), .25*(1-s)*(1-r*r), -.5*r*(1-s)*(1+t));
            break;
         default: {
            throw new IllegalArgumentException (
               "Shape function index must be in range [0,"+(numNodes()-1)+"]");
         }
      }
   }
   
   public double[] getNodeCoords () {
      return myNodeCoords;
   }

   private static double[] myNodeMassWeights = new double[] {
      // masses obtained by scaling the consistent mass matrix diagonal:
      // // corner nodes
      // 0.0282258,
      // 0.0282258,
      // 0.0282258,
      // 0.0282258,
      // 0.0282258,
      // 0.0282258,
      // 0.0282258,
      // 0.0282258,

      // // side nodes
      // 0.0645161,
      // 0.0645161,
      // 0.0645161,
      // 0.0645161,
      // 0.0645161,
      // 0.0645161,
      // 0.0645161,
      // 0.0645161,
      // 0.0645161,
      // 0.0645161,
      // 0.0645161,
      // 0.0645161

      // masses obtained by summing the consistent mass matrix:
      // corner nodes
      -0.125,
      -0.125,
      -0.125,
      -0.125,
      -0.125,
      -0.125,
      -0.125,
      -0.125,

      // side nodes
      0.16666666666666666,
      0.16666666666666666,
      0.16666666666666666,
      0.16666666666666666,
      0.16666666666666666,
      0.16666666666666666,
      0.16666666666666666,
      0.16666666666666666,
      0.16666666666666666,
      0.16666666666666666,
      0.16666666666666666,
      0.16666666666666666
   };

   public double[] getNodeMassWeights () {
      return myNodeMassWeights;
   }

   static int[] myEdgeIdxs = new int[] 
      {
         3,   0,  8,  1,
         3,   1,  9,  2,
         3,   2, 10,  3,
         3,   3, 11,  0,
         3,   4, 12,  5,
         3,   5, 13,  6,
         3,   6, 14,  7,
         3,   7, 15,  4,
         3,   0, 16,  4,
         3,   1, 17,  5,
         3,   2, 18,  6,
         3,   3, 19,  7,
      };

   static int[] myFaceIdxs = new int[] 
      {
         8,   0,  8,  1,  9,  2, 10,  3, 11,
         8,   1, 17,  5, 13,  6, 18,  2,  9,
         8,   5, 12,  4, 15,  7, 14,  6, 13,
         8,   4, 16,  0, 11,  3, 19,  7, 15,
         8,   3, 10,  2, 18,  6, 14,  7, 19,
         8,   0, 16,  4, 12,  5, 17,  1,  8
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
      if (nodes.length != 8) {
         throw new IllegalArgumentException (
            "Expecting 8 nodes, got " + nodes.length);
      }
      FemNode3d[][] triangles = new FemNode3d[6][3];

      setTriangle (triangles[0], nodes[0], nodes[1], nodes[7]);
      setTriangle (triangles[1], nodes[1], nodes[2], nodes[3]);
      setTriangle (triangles[2], nodes[3], nodes[4], nodes[5]);
      setTriangle (triangles[3], nodes[7], nodes[5], nodes[6]);
      if (nodes[7].distance(nodes[3]) < nodes[1].distance(nodes[5])) {
         setTriangle (triangles[4], nodes[1], nodes[3], nodes[7]);
         setTriangle (triangles[5], nodes[7], nodes[3], nodes[5]);
      }
      else {
         setTriangle (triangles[4], nodes[1], nodes[5], nodes[7]);
         setTriangle (triangles[5], nodes[1], nodes[3], nodes[5]);
      }
      return triangles;
   }   

}
