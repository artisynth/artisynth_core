/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.matrix.MatrixNd;
import maspack.render.Renderer;
import maspack.render.RenderProps;
import maspack.util.InternalErrorException;
import artisynth.core.modelbase.ModelComponent;

public class HexElement extends FemElement3d {

   private int myParity = -1;
   private HexElement[] myNeighbors;

   private ArrayList<TetElement> myTetra;

   private static IntegrationPoint3d[] myDefaultIntegrationPoints;
   private static double[] myDefaultIntegrationCoords;
   
   private static IntegrationPoint3d myWarpingPoint;
   private static FemElementRenderer myRenderer;
   
   private IntegrationPoint3d[] myIntegrationPoints = null;
   private boolean myIPointsMapToNodes = true;
   
   /**
    * {@inheritDoc}
    */ 
   public boolean integrationPointsMapToNodes() {
      return myIPointsMapToNodes;
   }
   
   public IntegrationPoint3d[] getDefaultIntegrationPoints() {
      return myDefaultIntegrationPoints;
   }

   public void setIntegrationPoints (
      IntegrationPoint3d[] ipnts, MatrixNd nodalExtrapMat) {
      myIPointsMapToNodes = mapIPointsToNodes (ipnts, nodalExtrapMat, myNodes);
      setIntegrationPoints (ipnts, nodalExtrapMat, myIPointsMapToNodes);
   }
   
   public void setIntegrationPoints (
      IntegrationPoint3d[] ipnts,  MatrixNd nodalExtrapMat, 
      boolean mapToNodes) {
      myIntegrationPoints = ipnts;
      myIPointsMapToNodes = mapToNodes;
      myNodalExtrapolationMatrix = new MatrixNd (nodalExtrapMat);
      myIntegrationData = null;
      //clearState();  // trigger re-creating integration data
   }
   
   public IntegrationPoint3d[] getIntegrationPoints() {
      
      if (myIntegrationPoints == null) {  
         // set (maybe construct) default integration points
         if (myDefaultIntegrationPoints == null) {
            if (myDefaultIntegrationCoords == null) {
               myDefaultIntegrationCoords = INTEGRATION_COORDS_GAUSS_8;
            }
            myDefaultIntegrationPoints = 
               createIntegrationPoints (
                  new HexElement(), myDefaultIntegrationCoords);
         }
         myIntegrationPoints = myDefaultIntegrationPoints;
      }
      
      return myIntegrationPoints;
   }

   public IntegrationPoint3d getWarpingPoint() {
      if (myWarpingPoint == null) {
         myWarpingPoint = IntegrationPoint3d.create (this, 0, 0, 0, 8);
         myWarpingPoint.setNumber (numIntegrationPoints());
      }
      return myWarpingPoint;
   }  

   public boolean coordsAreInside (Vector3d coords) {
      double s0 = coords.x;
      double s1 = coords.y;
      double s2 = coords.z;

      return (s0 >= -1 && s0 <= 1 && s1 >= -1 && s1 <= 1 && s2 >= -1 && s2 <= 1);
   }
   
   public static final double[] INTEGRATION_COORDS_GAUSS_8;
   public static final double[] INTEGRATION_COORDS_GAUSS_27;
   public static final double[] INTEGRATION_COORDS_GAUSS_64;
   public static final double[] INTEGRATION_COORDS_LOBATTO_8;
   public static final double[] INTEGRATION_COORDS_LOBATTO_27;
   public static final double[] INTEGRATION_COORDS_LOBATTO_64;
   
   public static final MatrixNd NODAL_EXTRAPOLATION_8;
   public static final MatrixNd NODAL_EXTRAPOLATION_27;
   public static final MatrixNd NODAL_EXTRAPOLATION_64;

   public int numIntegrationPoints() {
      if (myIntegrationPoints != null) {
         return myIntegrationPoints.length;
      }
      return myDefaultIntegrationCoords.length/4;
   }


   private static double[] myNodeCoords = new double[] 
      {
         -1, -1,  1,
         +1, -1,  1,
         +1,  1,  1,
         -1,  1,  1,
         -1, -1, -1,
         +1, -1, -1,
         +1,  1, -1,
         -1,  1, -1,
         };  
   
   private static double[] myNodeMassWeights = new double[] {
      0.125,
      0.125,
      0.125,
      0.125,
      0.125,
      0.125,
      0.125,
      0.125
   };

   static {
      double q = 1/Math.sqrt(3); // quadrature point
      INTEGRATION_COORDS_GAUSS_8 = new double[]
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
            
      q = Math.sqrt(3.0/4.0);
      INTEGRATION_COORDS_GAUSS_27 = createHexIntegrationCoords(
         new double[] {-q, 0, q}, 
         new double[] {5.0/9.0, 8.0/9.0, 5.0/9.0} ); 
      
      double q1 = 1.0/35*(Math.sqrt(525-70*Math.sqrt(30)));
      double q2 = 1.0/35*(Math.sqrt(525+70*Math.sqrt(30)));
      double a1 = (18+Math.sqrt(30))/36;
      double a2 = (18-Math.sqrt(30))/36;
      INTEGRATION_COORDS_GAUSS_64 = createHexIntegrationCoords(
         new double[] {-q2, -q1, q1, q2}, 
         new double[] { a2,  a1, a1, a2} ); 
      
      INTEGRATION_COORDS_LOBATTO_8 = createHexIntegrationCoords(
         new double[] {-1, 1},
         new double[] {1, 1});
      
      INTEGRATION_COORDS_LOBATTO_27 = createHexIntegrationCoords(
         new double[]{-1, 0, 1}, 
         new double[] {1.0/3.0, 4.0/3.0, 1.0/3.0} );
      
      q = 1.0/Math.sqrt(5);
      INTEGRATION_COORDS_LOBATTO_64 = createHexIntegrationCoords(
         new double[]{-1, -q, q, 1}, 
         new double[] {1.0/6.0, 5.0/6.0, 5.0/6.0, 1.0/6.0} );
      
      NODAL_EXTRAPOLATION_8 = createTensorProductExtrapolation(2);
      NODAL_EXTRAPOLATION_27 = createTensorProductExtrapolation(3);
      NODAL_EXTRAPOLATION_64 = createTensorProductExtrapolation(4);
      
      
//       myIntegrationCoords = new double[]
//          {
//             0, -1,  0,  4/3.0,
//             1,  0,  0,  4/3.0,
//             0,  1,  0,  4/3.0,
//            -1,  0,  0,  4/3.0,
//             0,  0,  1,  4/3.0,
//             0,  0, -1,  4/3.0,
//          };
//       myIntegrationCoords = new double[]
//          {
//             0,  0,  0,  8,
//          };
      
      myDefaultIntegrationCoords = INTEGRATION_COORDS_GAUSS_8;
   }

   // sign of node coordinate k at node i
   private static double nodeSgn (int i, int k) {
      double c = myNodeCoords[i*3+k];
      return c < 0 ? -1 : 1;
   }

   public double[] getIntegrationCoords () {
      return myDefaultIntegrationCoords;
   }

   public double[] getNodeCoords () {
      return myNodeCoords;
   }

   public double[] getNodeMassWeights () {
      return myNodeMassWeights;
   }

   private static MatrixNd myNodalExtrapolationMatrix = null;
   
   public void setNodalExtrapolationMatrix (MatrixNd NX) {
      myNodalExtrapolationMatrix = new MatrixNd(NX);
   }
   
   public MatrixNd getNodalExtrapolationMatrix() {
      if (myNodalExtrapolationMatrix == null) {
         Vector3d[] ncoords = getScaledNodeCoords (Math.sqrt(3), null);
         myNodalExtrapolationMatrix =
            createNodalExtrapolationMatrix (ncoords, 8, new HexElement());
         
         // For now, just use integration point values at corresponding nodes
         myNodalExtrapolationMatrix = new MatrixNd (8, 8);
         myNodalExtrapolationMatrix.setIdentity();
      }
      return myNodalExtrapolationMatrix;
   }

   public double getN (int i, Vector3d coords) {
      if (i < 0 || i >= numNodes()) {
         throw new IllegalArgumentException (
            "Shape function index must be in range [0,"+(numNodes()-1)+"]");
      }
      double s0 = coords.x;
      double s1 = coords.y;
      double s2 = coords.z;

      return 0.125*(1+nodeSgn(i,0)*s0)*(1+nodeSgn(i,1)*s1)*(1+nodeSgn(i,2)*s2);

   }

   public void getdNds (Vector3d dNds, int i, Vector3d coords) {
      if (i < 0 || i >= numNodes()) {
         throw new IllegalArgumentException (
            "Shape function index must be in range [0,"+(numNodes()-1)+"]");
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

   static int[] myEdgeIdxs = new int[] 
      {
         2,   0, 1,
         2,   1, 2,
         2,   2, 3,
         2,   3, 0,   
         2,   4, 5,
         2,   5, 6,
         2,   6, 7,
         2,   7, 4, 
         2,   0, 4,
         2,   1, 5,
         2,   2, 6,
         2,   3, 7
      };

   static int[] myFaceIdxs = new int[] 
      {
         4,   0, 1, 2, 3,
         4,   1, 5, 6, 2, 
         4,   5, 4, 7, 6, 
         4,   4, 0, 3, 7,
         4,   0, 4, 5, 1,
         4,   3, 2, 6, 7
      };

   static int[] myTriangulatedFaceIdxs = new int[] 
      {
         0, 1, 2,
         0, 2, 3,
         1, 5, 6,
         1, 6, 2,
         5, 4, 7,
         5, 7, 6,
         4, 0, 3,
         4, 3, 7,
         0, 4, 5,
         0, 5, 1,
         3, 2, 6,
         3, 6, 7
      };

   public int[] getEdgeIndices() {
      return myEdgeIdxs;
   }

   public int[] getFaceIndices() {
      return myFaceIdxs;
   }

   public int[] getTriangulatedFaceIndices() {
      return myTriangulatedFaceIdxs;
   }

   public int getParity() {
      return myParity;
   }

   public void setParity (int parity) {
      myParity = parity;
   }

   public void setFaceNeighbors (HexElement[] nbrs) {
      myNeighbors = nbrs;
   }

   public HexElement[] getFaceNeighbors() {
      return myNeighbors;
   }

   public void addTetElement (TetElement e) {
      if (myTetra == null) {
         myTetra = new ArrayList<TetElement>();
      }
      myTetra.add (e);
   }

   public boolean removeTetElement (TetElement e) {
      boolean present = false;
      if (myTetra != null) {
         present = myTetra.remove (e);
      }
      return present;
   }

   public void clearTetElements() {
      if (myTetra != null) {
         myTetra.clear();
      }
   }

   public int numTetElements() {
      if (myTetra == null) {
         return 0;
      }
      else {
         return myTetra.size();
      }
   }

   public TetElement getTetElement (int i) {
      if (myTetra == null) {
         throw new ArrayIndexOutOfBoundsException ("no sub-tets present");
      }
      else {
         return myTetra.get (i);
      }
   }

   public TetElement[] tesselate (int parity) {
      clearTetElements();
      TetElement[] elems =
         TetElement.createCubeTesselation (
            myNodes[0], myNodes[1], myNodes[2], myNodes[3], myNodes[4],
            myNodes[5], myNodes[6], myNodes[7], parity != 0);
      myParity = parity;
      for (int i = 0; i < elems.length; i++) {
         addTetElement (elems[i]);
      }
      return elems;
   }

   public HexElement() {
      myNodes = new FemNode3d[8];
   }

   /**
    * Creates a HexElement from eight nodes. The first four nodes should
    * describe a single face of the element, arranged counter-clockwise about
    * the outward-directed normal. The last four nodes should describe the
    * corresponding nodes on the opposite face (these will be arranged
    * clockwise about that face's normal).
    */
   public HexElement (FemNode3d n0, FemNode3d n1, FemNode3d n2, FemNode3d n3,
                      FemNode3d n4, FemNode3d n5, FemNode3d n6, FemNode3d n7) {
      myNodes = new FemNode3d[8];

      myNodes[0] = n0;
      myNodes[1] = n1;
      myNodes[2] = n2;
      myNodes[3] = n3;
      myNodes[4] = n4;
      myNodes[5] = n5;
      myNodes[6] = n6;
      myNodes[7] = n7;

//       nodesRestPos = new Vector3d[8];
//       for (int i = 0; i < 8; i++) {
//          nodesRestPos[i] = new Vector3d (myNodes[i].getPosition());
//       }
   }

   public HexElement (FemNode3d[] nodes) {
      myNodes = nodes.clone();
//       nodesRestPos = new Vector3d[8];
//       for (int i = 0; i < 8; i++) {
//          nodesRestPos[i] = new Vector3d (myNodes[i].getPosition());
//       }
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

   static int[] edgeIdxs = new int[] 
      {
         0, 1,  1, 2,   2, 3,   3, 0,   
         4, 5,  5, 6,   6, 7,   7, 4, 
         0, 4,  1, 5,   2, 6,   3, 7
      };

   static int[] faceIdxs = new int[] 
      {
         0, 1, 2, 3,
         1, 5, 6, 2, 
         5, 4, 7, 6, 
         4, 0, 3, 7,
         0, 4, 5, 1,
         3, 2, 6, 7
      };

//    public void computeWarping() {
//       if (!myStiffnessValidP) {
//          updateStiffness();
//       }
//       System.out.println("warping.");
//       myWarper.computeWarping (myNodes[4], myNodes[6], myNodes[1], myNodes[3]);
//    }

//    public void updateStiffness() {
//       // System.out.println("updating stiffness: E="+myE+", nu="+myNu);

//       Material mat = getEffectiveMaterial();
//       if (mat instanceof LinearMaterial) {
//          if (myWarper == null){
//             myWarper = new StiffnessWarper3d(8);
//          }
//          LinearMaterial lmat = (LinearMaterial)mat;
//          myWarper.computeInitialStiffness (
//             this, lmat.getYoungsModulus(), lmat.getPoissonsRatio());
//          myWarper.setInitialJ (myNodes[4], myNodes[6], myNodes[1], myNodes[3]);
//       }
//       myStiffnessValidP = true;
//    }

//    private double tetraVol (Vector3d p0, Vector3d p1, Vector3d p2, Vector3d p3) {
//       double m00 = p1.x - p0.x;
//       double m10 = p1.y - p0.y;
//       double m20 = p1.z - p0.z;

//       double m01 = p2.x - p0.x;
//       double m11 = p2.y - p0.y;
//       double m21 = p2.z - p0.z;

//       double m02 = p3.x - p0.x;
//       double m12 = p3.y - p0.y;
//       double m22 = p3.z - p0.z;

//       // the mij define the elements of a 3x3 matrix. The volume
//       // is 1/6 the determinant of this matrix

//       double det =
//          (m00 * m11 * m22 + m10 * m21 * m02 + m20 * m01 * m12 - m20 * m11 * m02
//          - m00 * m21 * m12 - m10 * m01 * m22);

//       return det / 6;
//    }

   /** 
    * Computes the exact volume of a hexahedron; code taken from FEBio
    *
    * @return volume of the hexahedron
    */
   private static double computeVolume (
      Point3d p0, Point3d p1, Point3d p2, Point3d p3, 
      Point3d p4, Point3d p5, Point3d p6, Point3d p7) {

      double x1 = p0.x, y1 = p0.y, z1 = p0.z;
      double x2 = p1.x, y2 = p1.y, z2 = p1.z;
      double x3 = p2.x, y3 = p2.y, z3 = p2.z;
      double x4 = p3.x, y4 = p3.y, z4 = p3.z;
      double x5 = p4.x, y5 = p4.y, z5 = p4.z;
      double x6 = p5.x, y6 = p5.y, z6 = p5.z;
      double x7 = p6.x, y7 = p6.y, z7 = p6.z;
      double x8 = p7.x, y8 = p7.y, z8 = p7.z;

      double f12 = 1.0/12.0;

      double b0 = f12*(y2*((z6-z3)-(z4-z5))+y3*(z2-z4)+y4*((z3-z8)-(z5-z2))+
                     y5*((z8-z6)-(z2-z4))+y6*(z5-z2)+y8*(z4-z5));

      double b1 = f12*(y3*((z7-z4)-(z1-z6))+y4*(z3-z1)+y1*((z4-z5)-(z6-z3))+
                     y6*((z5-z7)-(z3-z1))+y7*(z6-z3)+y5*(z1-z6));

      double b2 = f12*(y4*((z8-z1)-(z2-z7))+y1*(z4-z2)+y2*((z1-z6)-(z7-z4))+
                     y7*((z6-z8)-(z4-z2))+y8*(z7-z4)+y6*(z2-z7));

      double b3 = f12*(y1*((z5-z2)-(z3-z8))+y2*(z1-z3)+y3*((z2-z7)-(z8-z1))+
                     y8*((z7-z5)-(z1-z3))+y5*(z8-z1)+y7*(z3-z8));

      double b4 = f12*(y8*((z4-z7)-(z6-z1))+y7*(z8-z6)+y6*((z7-z2)-(z1-z8))+
                     y1*((z2-z4)-(z8-z6))+y4*(z1-z8)+y2*(z6-z1));

      double b5 = f12*(y5*((z1-z8)-(z7-z2))+y8*(z5-z7)+y7*((z8-z3)-(z2-z5))+
                     y2*((z3-z1)-(z5-z7))+y1*(z2-z5)+y3*(z7-z2));

      double b6 = f12*(y6*((z2-z5)-(z8-z3))+y5*(z6-z8)+y8*((z5-z4)-(z3-z6))+
                     y3*((z4-z2)-(z6-z8))+y2*(z3-z6)+y4*(z8-z3));

      double b7 = f12*(y7*((z3-z6)-(z5-z4))+y6*(z7-z5)+y5*((z6-z1)-(z4-z7))+
                     y4*((z1-z3)-(z7-z5))+y3*(z4-z7)+y1*(z5-z4));

      // calculate the volume V= xi*B1[i] = yi*B2[i] = zi*B3[i] (sum over i)
      return -(x1*b0+x2*b1+x3*b2+x4*b3+x5*b4+x6*b5+x7*b6+x8*b7);
   }

   public static double computeVolume (
      FemNode3d n1, FemNode3d n2, FemNode3d n3, FemNode3d n4, FemNode3d n5,
      FemNode3d n6, FemNode3d n7, FemNode3d n8) {
      return computeVolume (n1.getPosition (), n2.getPosition (), n3
         .getPosition (), n4.getPosition (), n5.getPosition (), n6
         .getPosition (), n7.getPosition (), n8.getPosition ());
   }
   
  // public double computeVolumes () {
  //    double vol =
  //       computeVolume (
  //          myNodes[0].getPosition(), myNodes[1].getPosition(),
  //          myNodes[2].getPosition(), myNodes[3].getPosition(), 
  //          myNodes[4].getPosition(), myNodes[5].getPosition(), 
  //          myNodes[6].getPosition(), myNodes[7].getPosition());

  //    myVolume = vol;
  //    myVolumes[0] = vol;
  //    return vol;
  // }


   //   private double computeTetVolume (
   //      Point3d p0, Point3d p1, Point3d p2, Point3d p3, 
   //      Point3d p4, Point3d p5, Point3d p6, Point3d p7) {
   //
   //      double vol = 0;
   //
   //      vol += TetElement.computeVolume (p3, p0, p4, p1);
   //      vol += TetElement.computeVolume (p3, p1, p6, p2);
   //      vol += TetElement.computeVolume (p4, p5, p6, p1);
   //      vol += TetElement.computeVolume (p7, p4, p6, p3);
   //      vol += TetElement.computeVolume (p3, p4, p6, p1);
   //      return vol;
   //   }      

//    /**
//     * {@inheritDoc}
//     */
//    public double computeRestVolumes() {
//       /*
//        * To find volume of a hex element: Tesselate the hex into 5 tetrahedrons,
//        * then add up the volume of each tetrahedron.
//        */
//       double vol = 0;

// //       Vector3d p1 = myNodes[0].myRest;
// //       Vector3d p2 = myNodes[1].myRest;
// //       Vector3d p3 = myNodes[2].myRest;
// //       Vector3d p4 = myNodes[3].myRest;
// //       Vector3d p5 = myNodes[4].myRest;
// //       Vector3d p6 = myNodes[5].myRest;
// //       Vector3d p7 = myNodes[6].myRest;
// //       Vector3d p8 = myNodes[7].myRest;

// //       // NODES REORDERED
// //       vol += TetElement.computeVolume (p4, p1, p5, p2);
// //       vol += TetElement.computeVolume (p4, p2, p7, p3);
// //       vol += TetElement.computeVolume (p5, p6, p7, p2);
// //       vol += TetElement.computeVolume (p8, p5, p7, p4);
// //       vol += TetElement.computeVolume (p4, p5, p7, p2);

//       vol = computeVolume (myNodes[0].myRest, myNodes[1].myRest,
//                            myNodes[2].myRest, myNodes[3].myRest, 
//                            myNodes[4].myRest, myNodes[5].myRest, 
//                            myNodes[6].myRest, myNodes[7].myRest);
//       myRestVolumes[0] = vol;
//       return vol;
//     }

//    public double getVolume() {
//       /*
//        * To find volume of a hex element: Tesselate the hex into 5 tetrahedrons,
//        * then add up the volume of each tetrahedron.
//        */
//       double vol = 0;

//       Vector3d p1 = myNodes[0].getPosition();
//       Vector3d p2 = myNodes[1].getPosition();
//       Vector3d p3 = myNodes[2].getPosition();
//       Vector3d p4 = myNodes[3].getPosition();
//       Vector3d p5 = myNodes[4].getPosition();
//       Vector3d p6 = myNodes[5].getPosition();
//       Vector3d p7 = myNodes[6].getPosition();
//       Vector3d p8 = myNodes[7].getPosition();

//       vol += TetElement.computeVolume (p4, p1, p5, p2);
//       vol += TetElement.computeVolume (p4, p2, p7, p3);
//       vol += TetElement.computeVolume (p5, p6, p7, p2);
//       vol += TetElement.computeVolume (p8, p5, p7, p4);
//       vol += TetElement.computeVolume (p4, p5, p7, p2);

//       return vol;
//    }

//    public void addNodeStiffness (Matrix3d Kij, int i, int j, boolean warping) {
//       if (!myStiffnessValidP) {
//          updateStiffness();
//       }
//       myWarper.addNodeStiffness (Kij, i, j, warping);
//    }

//    public void addNodeRestForce (Vector3d frest, int i, boolean warping) {
//       if (!myStiffnessValidP) {
//          updateStiffness();
//       }
//       myWarper.addNodeRestForce (frest, i, warping);
//    }

//    public void addNodeForce (Vector3d f, int i, boolean warping) {
//       if (!myStiffnessValidP) {
//          updateStiffness();
//       }
//       myWarper.addNodeForce (f, i, myNodes, warping);
//    }


//    public void getMarkerCoordinates (VectorNd coords, Point3d pos) {
//       if (coords.size() != 8) {
//          throw new IllegalArgumentException (
//             "coords should have size 8 for hex elements");
//       }
//       if (!myStiffnessValidP) {
//          updateStiffness();
//       }

//       Vector3d p1 = myNodes[0].getPosition();
//       Vector3d p2 = myNodes[1].getPosition();
//       Vector3d p3 = myNodes[2].getPosition();
//       Vector3d p4 = myNodes[3].getPosition();
//       Vector3d p5 = myNodes[4].getPosition();
//       Vector3d p6 = myNodes[5].getPosition();
//       Vector3d p7 = myNodes[6].getPosition();
//       Vector3d p8 = myNodes[7].getPosition();

//       MatrixNd M = new MatrixNd (8, 8);

//       double MEntries[] =
//          { 1.0, p1.x, p1.y, p1.z, p1.x*p1.y, p1.x*p1.z, p1.y*p1.z,p1.x*p1.y*p1.z,
//            1.0, p2.x, p2.y, p2.z, p2.x*p2.y, p2.x*p2.z, p2.y*p2.z,p2.x*p2.y*p2.z,
//            1.0, p3.x, p3.y, p3.z, p3.x*p3.y, p3.x*p3.z, p3.y*p3.z,p3.x*p3.y*p3.z,
//            1.0, p4.x, p4.y, p4.z, p4.x*p4.y, p4.x*p4.z, p4.y*p4.z,p4.x*p4.y*p4.z, 
//            1.0, p5.x, p5.y, p5.z, p5.x*p5.y, p5.x*p5.z, p5.y*p5.z,p5.x*p5.y*p5.z,
//            1.0, p6.x, p6.y, p6.z, p6.x*p6.y, p6.x*p6.z, p6.y*p6.z,p6.x*p6.y*p6.z,
//            1.0, p7.x, p7.y, p7.z, p7.x*p7.y, p7.x*p7.z, p7.y*p7.z,p7.x*p7.y*p7.z,
//            1.0, p8.x, p8.y, p8.z, p8.x*p8.y, p8.x*p8.z, p8.y*p8.z,p8.x*p8.y*p8.z 
//          };

//       M.set (MEntries);

//       M.invert();

//       double coordsBuffer[] = coords.getBuffer();
//       coordsBuffer[0] = 1.0;
//       coordsBuffer[1] = pos.x;
//       coordsBuffer[2] = pos.y;
//       coordsBuffer[3] = pos.z;
//       coordsBuffer[4] = pos.x * pos.y;
//       coordsBuffer[5] = pos.x * pos.z;
//       coordsBuffer[6] = pos.y * pos.z;
//       coordsBuffer[7] = pos.x * pos.y * pos.z;

//       M.mulTranspose (coords, coords);
//    }

//   public boolean isInsideRobust(Point3d pnt) {
//      // check natural coordinates
//      Vector3d coords = new Vector3d();
//      getNaturalCoordinates(coords, pnt);
//      return coordsAreInside (coords);
//   }

   /**
    * Tests whether or not a point is inside an element.
    * Note: this routine divides the element into a 
    * set of Tets, making the assumption that faces
    * are piece-wise planes.  This isn't technically
    * correct.  Do avoid potential gaps between adjacent elements,
    * we split the hex in all possible ways
    *
    * @param pnt point to check if is inside
    * @return true if point is inside the element
    */
   public boolean isInside (Point3d pnt) {
      Point3d p1 = myNodes[0].getPosition();
      Point3d p2 = myNodes[1].getPosition();
      Point3d p3 = myNodes[2].getPosition();
      Point3d p4 = myNodes[3].getPosition();
      Point3d p5 = myNodes[4].getPosition();
      Point3d p6 = myNodes[5].getPosition();
      Point3d p7 = myNodes[6].getPosition();
      Point3d p8 = myNodes[7].getPosition();

      /*
       * Tesselate into 5 tetrahedrons. The point is inside the hex element if
       * it's inside any one of the tetrahedrons.
       */
      // NODES REORDERED
      boolean inside = (TetElement.isInside (pnt, p4, p1, p5, p2) ||
         TetElement.isInside (pnt, p4, p2, p7, p3) ||
         TetElement.isInside (pnt, p5, p6, p7, p2) ||
         TetElement.isInside (pnt, p8, p5, p7, p4) ||
         TetElement.isInside (pnt, p4, p5, p7, p2) 
         
         /* split the other way */
         ||
         TetElement.isInside (pnt, p1, p2, p6, p3) ||
         TetElement.isInside (pnt, p1, p3, p8, p4) ||
         TetElement.isInside (pnt, p6, p7, p8, p3) ||
         TetElement.isInside (pnt, p5, p6, p8, p1) ||
         TetElement.isInside (pnt, p1, p6, p8, p3)
         );
     
      return inside;
   }

//    private boolean isInsideSubTet (
//       Point3d pnt, Vector3d p1, Vector3d p2, Vector3d p3, Vector3d p4) {
//       if (A == null) {
//          A = new Matrix3d();
//       }
//       Vector3d v = new Vector3d();
//       v.sub (pnt, p1);

//       Vector3d temp = new Vector3d();

//       temp.sub (p2, p1);
//       A.setColumn (0, temp);
//       temp.sub (p3, p1);
//       A.setColumn (1, temp);
//       temp.sub (p4, p1);
//       A.setColumn (2, temp);

//       A.invert();
//       v.mul (A, v);

//       return (v.x >= 0 && v.y >= 0 && v.z >= 0 && (1.0 - v.x - v.y - v.z >= 0));

//    }

//    @Override
//    public FaceNodes3d[] getTriFaces() {
//       if (true) {
//          return getQuadFaces();
//       }
//       else {
         
//          FaceNodes3d[] faces = new FaceNodes3d[12];
//          if (myParity == 0) {
//             faces[0] = new FaceNodes3d (myNodes[0], myNodes[1], myNodes[3]);
//             faces[0] = new FaceNodes3d (myNodes[0], myNodes[1], myNodes[3]);
//             faces[1] = new FaceNodes3d (myNodes[1], myNodes[2], myNodes[3]);
//             faces[2] = new FaceNodes3d (myNodes[0], myNodes[4], myNodes[1]);
//             faces[3] = new FaceNodes3d (myNodes[1], myNodes[4], myNodes[5]);
//             faces[4] = new FaceNodes3d (myNodes[1], myNodes[5], myNodes[6]);
//             faces[5] = new FaceNodes3d (myNodes[1], myNodes[6], myNodes[2]);

//             faces[6] = new FaceNodes3d (myNodes[2], myNodes[6], myNodes[3]);
//             faces[7] = new FaceNodes3d (myNodes[3], myNodes[6], myNodes[7]);
//             faces[8] = new FaceNodes3d (myNodes[0], myNodes[3], myNodes[4]);
//             faces[9] = new FaceNodes3d (myNodes[3], myNodes[7], myNodes[4]);
//             faces[10] = new FaceNodes3d (myNodes[4], myNodes[6], myNodes[5]);
//             faces[11] = new FaceNodes3d (myNodes[4], myNodes[7], myNodes[6]);
//          }
//          else {
//             faces[0] = new FaceNodes3d (myNodes[0], myNodes[1], myNodes[2]);
//             faces[1] = new FaceNodes3d (myNodes[0], myNodes[2], myNodes[3]);
//             faces[2] = new FaceNodes3d (myNodes[0], myNodes[5], myNodes[1]);
//             faces[3] = new FaceNodes3d (myNodes[0], myNodes[4], myNodes[5]);
//             faces[4] = new FaceNodes3d (myNodes[1], myNodes[5], myNodes[2]);
//             faces[5] = new FaceNodes3d (myNodes[2], myNodes[5], myNodes[6]);

//             faces[6] = new FaceNodes3d (myNodes[2], myNodes[6], myNodes[7]);
//             faces[7] = new FaceNodes3d (myNodes[2], myNodes[7], myNodes[3]);
//             faces[8] = new FaceNodes3d (myNodes[0], myNodes[3], myNodes[7]);
//             faces[9] = new FaceNodes3d (myNodes[0], myNodes[7], myNodes[4]);
//             faces[10] = new FaceNodes3d (myNodes[4], myNodes[7], myNodes[5]);
//             faces[11] = new FaceNodes3d (myNodes[5], myNodes[7], myNodes[6]);
//          }
//          return faces;
//       }
//    }

   public FaceNodes3d[] getQuadFaces() {
      FaceNodes3d[] faces = new FaceNodes3d[6];
      for (int i=0; i<6; i++) {
         int i0 = faceIdxs[i*4  ];
         int i1 = faceIdxs[i*4+1];
         int i2 = faceIdxs[i*4+2];
         int i3 = faceIdxs[i*4+3];
         faces[i] = new FaceNodes3d (
            this, myNodes[i0], myNodes[i1], myNodes[i2], myNodes[i3]);
      }
      return faces;
   }

   /**
    * Return the parity required of a neighbour so that a tesselation of the
    * neighbor aligns with the tesselation of this element. If the specified
    * face does not belong to one or both elements, then -1 is returned.
    */
   private int getNeighborParity (FaceNodes3d face, HexElement nbr) {
      // find the index of the first face node in the HexElement and in the
      // neighboring element.

      FemNode[] hexNodes = myNodes;
      FemNode[] nbrNodes = nbr.getNodes();
      int hexIdx = -1;
      for (int i = 0; i < hexNodes.length; i++) {
         if (hexNodes[i] == face.nodes[0]) {
            hexIdx = i;
         }
      }
      if (hexIdx == -1) {
         return -1;
      }
      int nbrIdx = -1;
      for (int i = 0; i < nbrNodes.length; i++) {
         if (nbrNodes[i] == face.nodes[0]) {
            nbrIdx = i;
         }
      }
      if (nbrIdx == -1) {
         return -1;
      }
      if ((hexIdx == 1 || hexIdx == 3 || hexIdx == 4 || hexIdx == 6) ==
          (nbrIdx == 1 || nbrIdx == 3 || nbrIdx == 4 || nbrIdx == 6)) {
         // neighbor should have same parity
         return myParity;
      }
      else {
         return myParity == 1 ? 0 : 1;
      }
   }

   /**
    * Rescursively sets the partities of a nodes neighbors, starting with this
    * element, so as to try and obtain conforming tesselation. Returns the
    * number of elements, if any, whose parity code not be set properly.
    */
   public int setNeighborParities() {
      int badCnt = 0;
      LinkedList<HexElement> queue = new LinkedList<HexElement>();
      queue.addLast (this);
      while (!queue.isEmpty()) {
         HexElement hex = queue.removeFirst();

         FaceNodes3d[] faces = hex.getQuadFaces();
         HexElement[] nbrs = hex.getFaceNeighbors();
         for (int i = 0; i < faces.length; i++) {
            HexElement nbr = nbrs[i];
            if (nbr != null) {
               int nbrParity = hex.getNeighborParity (faces[i], nbr);
               if (nbr.getParity() == -1) {
                  nbr.setParity (nbrParity);
                  queue.addLast (nbr);
               }
               else if (nbr.getParity() != nbrParity) {
                  badCnt++;
               }
            }
         }
      }
      return badCnt;
   }

   private static class HexFaceInfo {
      HexElement hex;
      int faceIdx;

      HexFaceInfo (HexElement hex, int faceIdx) {
         this.hex = hex;
         this.faceIdx = faceIdx;
      }
   }

   public static void setParities (List<HexElement> hexes) {
      HashMap<FaceNodes3d,HexFaceInfo> faceMap =
         new HashMap<FaceNodes3d,HexFaceInfo>();

      for (HexElement hex : hexes) {
         HexElement[] nbrs = new HexElement[6];
         FaceNodes3d[] faces = hex.getQuadFaces();
         for (int k = 0; k < faces.length; k++) {
            HexFaceInfo nbrInfo = faceMap.get (faces[k]);
            if (nbrInfo == null) {
               faceMap.put (faces[k], new HexFaceInfo (hex, k));
            }
            else {
               nbrs[k] = nbrInfo.hex;
               if (nbrInfo.hex == hex) {
                  throw new InternalErrorException (
                     "neighbor and hex are the same, hex number "
                     + hex.getNumber());

               }
               nbrInfo.hex.getFaceNeighbors()[nbrInfo.faceIdx] = hex;
            }
         }
         hex.setParity (-1);
         hex.setFaceNeighbors (nbrs);
      }

      for (HexElement hex : hexes) {
         if (hex.getParity() == -1) {
            hex.setParity (1);
            hex.setNeighborParities();
         }
      }
   }

   public boolean hasEdge (FemNode3d n0, FemNode3d n1) {
      int idx0 = getNodeIndex (n0);
      int idx1 = getNodeIndex (n1);
      if (idx0 == -1 || idx1 == -1) {
         return false;
      }
      for (int i=0; i<24; i+=2) {
         int i0 = edgeIdxs[i];
         int i1 = edgeIdxs[i+1];
         if ((idx0 == i0 && idx1 == i1) || (idx0 == i1 && idx1 == i0)){
            return true;
         }
      }
      return false;
   }

   private boolean nodeInFace (int faceIdx, int nodeIdx) {
      for (int i=4*faceIdx; i<4*faceIdx+4; i++) {
         if (faceIdxs[i] == nodeIdx) {
            return true;
         }
      }
      return false;
   }

   public boolean hasFace (FemNode3d n0, FemNode3d n1,
                           FemNode3d n2, FemNode3d n3) {
      int idx0 = getNodeIndex (n0);
      int idx1 = getNodeIndex (n1);
      int idx2 = getNodeIndex (n2);
      int idx3 = getNodeIndex (n3);
      if (idx0 == -1 || idx1 == -1 || idx2 == -1 || idx3 == -1) {
         return false;
      }
      for (int i=0; i<6; i++) {
         if (nodeInFace (i, idx0) &&
             nodeInFace (i, idx1) &&
             nodeInFace (i, idx2) &&
             nodeInFace (i, idx3)) {
            return true;
         }
      }
      return false;
   }

   @Override
   public HexElement copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      HexElement e = (HexElement)super.copy (flags, copyMap);

      e.myParity = myParity;
      // TODO: copy doesn't preserve node neighbours
      e.myNeighbors = null;
      // copy doesn't preserve tetra; this might be OK
      e.myTetra = null;

      return e;
   }
   
   /**
    * Computes hexahedral integration coordinates using a tensor product 
    * of the supplied 1D quadrature rule.
    * 
    * @param locs 1D locations in [-1 1]
    * @param wgts associated weights
    * @return double array with [x1,y1,z1,w1, x2,y2,z2,w1, ...]
    */
   public static double[] createHexIntegrationCoords(double[] locs, double[] wgts) {
      
      int n = locs.length;
      double [] coords = new double[n*n*n*4];
      
      int idx = 0;
      for (int i=0; i<n; i++) {
         for (int j=0; j<n; j++) {
            for (int k=0; k<n; k++) {
               coords[idx++] = locs[i];
               coords[idx++] = locs[j];
               coords[idx++] = locs[k];
               coords[idx++] = wgts[i]*wgts[j]*wgts[k];
            }
         }
      }
      
      // re-order to align with nodes
      mapICoordsToNodes(coords);
      
      return coords;
      
   }
   
   public static boolean mapIPointsToNodes (
      IntegrationPoint3d[] ipnts, MatrixNd nodalInterp, FemNode3d[] nodes) {
      int nNodes = nodes.length;
      int nIPnts = ipnts.length;
      
      if (nIPnts < nNodes) {
         return false;
      }
      
      double dist, minDist;
      Point3d pos = new Point3d();
      int closest;
      for (int i=0; i<nNodes; i++) {
         minDist = Double.MAX_VALUE;
         closest = i;
         for (int j=i; j<nIPnts; j++) {
            ipnts[i].computePosition(pos, nodes);
            dist = pos.distance(nodes[i].getPosition());
            if (dist<minDist) {
               closest = j;
               minDist = dist;
            }
         }
         if (closest != i) {
            IntegrationPoint3d tmp;
            double tmpd;
            tmp = ipnts[closest];
            ipnts[closest] = ipnts[i];
            ipnts[i] = tmp;
            for (int j=0; j<nNodes; j++) {
               tmpd = nodalInterp.get (j, closest);
               nodalInterp.set (j, closest, nodalInterp.get (j, i));
               nodalInterp.set (j, i, tmpd);
            }
         }
      }

      return true;
   }
   
   public static boolean mapICoordsToNodes(double [] coords) {
      
      int nNodes = myNodeCoords.length/3;
      int nICoords = coords.length/4;
      
      if (nICoords < nNodes) {
         return false;
      }
      double dx,dy,dz;
      double dist, minDist;
      int closest;
      for (int i=0; i<nNodes; i++) {
         minDist = Double.MAX_VALUE;
         closest = i;
         for (int j=i; j<nICoords; j++) {
            dx = (coords[j*4]-myNodeCoords[i*3]);
            dy = (coords[j*4+1]-myNodeCoords[i*3+1]);
            dz = (coords[j*4+2]-myNodeCoords[i*3+2]);
            dist = dx*dx+dy*dy+dz*dz;
            if (dist<minDist) {
               closest = j;
               minDist = dist;
            }
         }
         if (closest != i) {
            double tmp;
            for (int j=0; j<4; j++) {
               tmp = coords[closest*4+j];
               coords[closest*4+j] = coords[i*4+j];
               coords[i*4+j] = tmp;;
            }
         }
      }

      return true;
   }
   
   static MatrixNd createTensorProductExtrapolation(int nx) {
      
      int nx2 = nx*nx;
      int nx3 = nx*nx2;
      
      double[] out = new double[nx3*8];
      
      out[0      + 4*nx3] = 1;   // node 4
      out[nx-1   + 5*nx3] = 1;   // node 5
      out[nx2-nx + 7*nx3] = 1;   // node 7
      out[nx2-1  + 6*nx3] = 1;   // node 6
      
      out[nx3-nx2 + 0*nx3] = 1;  // node 0       
      out[nx3-nx2+nx-1 + 1*nx3] = 1; // node 1
      out[nx3-nx + 3*nx3] = 1;   // node 3
      out[nx3-1  + 2*nx3] = 1;   // node 2
      
      MatrixNd NX = new MatrixNd (8, nx3);
      NX.set (out);
      return NX;
   }
   
   
}
