/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import maspack.render.*;
import maspack.matrix.*;

/**
 * Nodes are arranged clockwise WRT p0
 */
public class TetElement extends FemElement3d {

   //   private StiffnessWarper3d myWarper = null;

   private static IntegrationPoint3d[] myDefaultIntegrationPoints;
   private static FemElementRenderer myRenderer;

   public IntegrationPoint3d[] getIntegrationPoints() {
      if (myDefaultIntegrationPoints == null) {
         myDefaultIntegrationPoints = 
            createIntegrationPoints (new TetElement());
      }
      return myDefaultIntegrationPoints;
   }

   public IntegrationPoint3d getWarpingPoint() {
      return getIntegrationPoints()[0];
   }

   public double getConditionNum() {
      IntegrationPoint3d wpnt = getWarpingPoint();
      Vector3d[] GNs = wpnt.getGNs();
      FemNode3d[] nodes = getNodes();
      IntegrationData3d wdata = getWarpingData();
      
      // compute J0
      Matrix3d J0 = new Matrix3d();
      J0.setZero();
      for (int i=0; i<nodes.length; i++) {
         Vector3d pos = nodes[i].getLocalRestPosition();
         Vector3d dNds = GNs[i];
         J0.addOuterProduct (pos.x, pos.y, pos.z, dNds.x, dNds.y, dNds.z);
      }
      
      double conditionNum = J0.infinityNorm()*wdata.myInvJ0.infinityNorm();
      
      return conditionNum;
   }

   public boolean isInverted() {
      return (getVolume() * getRestVolume() < 0);
   }

   public TetElement() {
      myNodes = new FemNode3d[4];
   }

   /**
    * Creates a new TetraHedral element from four nodes. The first three nodes
    * should define a clockwise arrangement about a particular face.
    */
   public TetElement (FemNode3d p0, FemNode3d p1, FemNode3d p2, FemNode3d p3) {
      this();
      setNodes (p0, p1, p2, p3);
   }
   
   /**
    * Creates a new TetraHedral element from four nodes. The first three nodes
    * should define a clockwise arrangement about a particular face.
    */
   public TetElement(FemNode3d[] nodes) {     
      if (nodes.length != 4) {
         throw new IllegalArgumentException ("nodes must have length 4");
      }
      myNodes = nodes.clone();
   }

   /**
    * Sets the nodes of a TetraHedral element. The first three nodes should
    * define a clockwise arrangement about a particular face.
    */
   public void setNodes (FemNode3d p0, FemNode3d p1, FemNode3d p2, FemNode3d p3) {
      myNodes[0] = p0;
      myNodes[1] = p1;
      myNodes[2] = p2;
      myNodes[3] = p3;
      invalidateRestData();
   }

   /**
    * Create a tesselation for a cubic configuration of nodes, using a
    * Freudenthal cut. The first four particles should define a single
    * counter-clockwise face, while the last four should give the corresponding
    * (clockwise) nodes for the opposite face. If the tesselation is even, it
    * means that vertices p1, p3, p4, and p6 form the internal tetrahedron of
    * the Freudenthal cut. Otherwise, this tetrahedron is formed from vertices
    * p0, p2, p5, and p7.
    */
   public static TetElement[] createCubeTesselation (
      FemNode3d p0, FemNode3d p1, FemNode3d p2, FemNode3d p3, FemNode3d p4,
      FemNode3d p5, FemNode3d p6, FemNode3d p7, boolean even) {
      TetElement[] elems = new TetElement[5];
      if (even) {
         elems[0] = new TetElement (p0, p1, p4, p3);
         elems[1] = new TetElement (p2, p3, p6, p1);
         elems[2] = new TetElement (p5, p4, p1, p6);
         elems[3] = new TetElement (p7, p6, p3, p4);
         elems[4] = new TetElement (p1, p6, p4, p3);
      }
      else {
         elems[0] = new TetElement (p1, p0, p2, p5);
         elems[1] = new TetElement (p3, p0, p7, p2);
         elems[2] = new TetElement (p4, p0, p5, p7);
         elems[3] = new TetElement (p6, p2, p7, p5);
         elems[4] = new TetElement (p0, p2, p5, p7);
      }
      return elems;
   }

   /**
    * Create a tesselation for a wedge configuration of nodes. The first three
    * particles should define a single counter-clockwise face, while the last
    * three should give the corresponding (clockwise) nodes for the opposite
    * face. The tesselation type is controlled by a code <code>type</code>,
    * which is an integer in which bits 0, 1, and 2 describe whether the side
    * edge of the tetrahedron directly beneath edges p0-p1, p1-p2, and p2-p0 is
    * rising or falling as the edge is traversed in a counter-clockwise
    * fashion. Not all edges can be rising or falling, and so bit patterns 0x0
    * and 0x7 are illegal.
    */
   public static TetElement[] createWedgeTesselation (
      FemNode3d p0, FemNode3d p1, FemNode3d p2,
      FemNode3d p3, FemNode3d p4, FemNode3d p5, int type) {
      TetElement[] elems = new TetElement[3];

      switch (type) {
         case 0x1: { /* R, F, F */ 
            elems[0] = new TetElement (p0, p2, p1, p3);
            elems[1] = new TetElement (p2, p5, p1, p3);
            elems[2] = new TetElement (p1, p5, p4, p3);
            break;
         }
         case 0x2: { /* F, R, F */ 
            elems[0] = new TetElement (p0, p2, p1, p4);
            elems[1] = new TetElement (p0, p3, p2, p4);
            elems[2] = new TetElement (p2, p3, p5, p4);
            break;
         }
         case 0x3: { /* R, R, F */ 
            elems[0] = new TetElement (p0, p2, p1, p3);
            elems[1] = new TetElement (p1, p2, p4, p3);
            elems[2] = new TetElement (p2, p5, p4, p3);
            break;
         }
         case 0x4: { /* F, F, R */ 
            elems[0] = new TetElement (p0, p2, p1, p5);
            elems[1] = new TetElement (p1, p4, p0, p5);
            elems[2] = new TetElement (p0, p4, p3, p5);
            break;
         }
         case 0x5: { /* R, F, R */ 
            elems[0] = new TetElement (p0, p2, p1, p5);
            elems[1] = new TetElement (p0, p1, p3, p5);
            elems[2] = new TetElement (p1, p4, p3, p5);
            break;
         }
         case 0x6: { /* F, R, R */ 
            elems[0] = new TetElement (p0, p2, p1, p4);
            elems[1] = new TetElement (p2, p0, p5, p4);
            elems[2] = new TetElement (p0, p3, p5, p4);
            break;
         }
         default: {
            throw new IllegalArgumentException (
               "Illegal or unknown configuration type: " + type);
         }
      }
      return elems;
   }

   public boolean coordsAreInside (Vector3d coords) {
      double s1 = coords.x;
      double s2 = coords.y;
      double s3 = coords.z;
      double s0 = 1 - s1 - s2 - s3;

      return (s0 >= 0 && s1 >= 0 && s2 >= 0 && s3 >= 0);
   }

   /**
    * Shape functions for the tet are
    *
    * N_0 = 1 - s1 - s2 - s3
    * N_1 = s1
    * N_2 = s2
    * N_3 = s3
    */
   private static int myNumIntPoints = 1;

   public int numIntegrationPoints() {
      return myNumIntPoints;
   }

   private static double[] myIntegrationCoords = new double[] {
      0.25, 0.25, 0.25, 1/6.0,
   };

   public double[] getIntegrationCoords () {
      return myIntegrationCoords;
   }

   private static MatrixNd myNodalExtrapolationMatrix = null;

   public MatrixNd getNodalExtrapolationMatrix() {
      if (myNodalExtrapolationMatrix == null) {
         myNodalExtrapolationMatrix = new MatrixNd (1, 4);
         myNodalExtrapolationMatrix.set (new double[] { 1, 1, 1, 1 });
      }
      return myNodalExtrapolationMatrix;
   }

   public double getN (int i, Vector3d coords) {
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
               "Shape function index must be in range [0,"+(numNodes()-1)+"]");
         }
      }
   }

   public void getdNds (Vector3d dNds, int i, Vector3d coords) {
      switch (i) {
         case 0: dNds.set (-1, -1, -1); break;
         case 1: dNds.set ( 1,  0,  0); break;
         case 2: dNds.set ( 0,  1,  0); break;
         case 3: dNds.set ( 0,  0,  1); break;
         default: {
            throw new IllegalArgumentException (
               "Shape function index must be in range [0,"+(numNodes()-1)+"]");
         }
      }
   }


   private static double[] myNodeCoords = new double[] 
      {
         0, 0, 0,
         1, 0, 0,
         0, 1, 0,
         0, 0, 1
      };

   public double[] getNodeCoords () {
      return myNodeCoords;
   }

   private static double[] myNodeMassWeights = new double[] {
      0.250,
      0.250,
      0.250,
      0.250
   };

   public double[] getNodeMassWeights () {
      return myNodeMassWeights;
   }

   static int[] myEdgeIdxs = new int[]
      {
         2,   0, 1,
         2,   0, 2,
         2,   0, 3,
         2,   1, 2,
         2,   2, 3,
         2,   3, 1 
      };
      
   static int[] myFaceIdxs = new int[] 
      {
         3,   0, 2, 1,
         3,   0, 1, 3,
         3,   1, 2, 3, 
         3,   2, 0, 3
      };

   static int[] myTriangulatedFaceIdxs = new int[] 
      {
         0, 2, 1,
         0, 1, 3,
         1, 2, 3, 
         2, 0, 3
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

   /**
    * {@inheritDoc}
    */
   public double computeRestVolumes() {
      double vol = computeVolume (
            myNodes[0].myRest, myNodes[1].myRest,
            myNodes[2].myRest, myNodes[3].myRest);
      myRestVolumes[0] = vol;
      myRestVolume = vol;
      return 6*vol; // detJ0 is 6 * restVolume
   }

   /** 
    * Computes the volume of the tetrahedron formed by four FemNodes, where the
    * first three nodes are arranged clockwise about a single face, and
    * clockwise is with respect to the face's outward normal.  This method can
    * be used when creating TetElements to make sure that the defining nodes
    * are specified in the proper order. If they are not specified in the
    * correct order the volume will be negative.
    * 
    * @param n0 1st FEM node
    * @param n1 2nd FEM node
    * @param n2 3rd FEM node
    * @param n3 4th FEM node
    * @return volume of the tetrahedron. 
    */
   public static double computeVolume (
      FemNode3d n0, FemNode3d n1, FemNode3d n2, FemNode3d n3) {

      return computeVolume (n0.getPosition(), n1.getPosition(), 
                            n2.getPosition(), n3.getPosition());

   }

   public static double computeVolume (
      Point3d p0, Point3d p1, Point3d p2, Point3d p3) {
      
      double m00 = p1.x - p0.x;
      double m10 = p1.y - p0.y;
      double m20 = p1.z - p0.z;

      double m01 = p2.x - p0.x;
      double m11 = p2.y - p0.y;
      double m21 = p2.z - p0.z;

      double m02 = p3.x - p0.x;
      double m12 = p3.y - p0.y;
      double m22 = p3.z - p0.z;

      return (m00*m11*m22 + m10*m21*m02 + m20*m01*m12 -
              m20*m11*m02 - m00*m21*m12 - m10*m01*m22)/6;
   }

   /**
    * {@inheritDoc}
    */
   public double computeVolumes() {
      myVolume = computeVolume (myNodes[0], myNodes[1], myNodes[2], myNodes[3]);
      myVolumes[0] = myVolume;
      return myVolume/getRestVolume();
   }

   /**
    * Compute the area-weighted normals for this tetrahedron. An area-weighted
    * normal is an outward-facing normal for a face, scaled by the area of that
    * face. The normals are returned in an array of four Vector3d objects, with
    * the i-th normal corresponding to the face opposite the i-th vertex.
    * 
    * @param anormals
    * returns the area-weight normals
    */
   public void getAreaWeightedNormals (Vector3d[] anormals) {
      if (anormals.length < 4) {
         throw new IllegalArgumentException (
            "result array 'anormals' is too small");
      }
      Point3d p0 = myNodes[0].getPosition();
      Point3d p1 = myNodes[1].getPosition();
      Point3d p2 = myNodes[2].getPosition();
      Point3d p3 = myNodes[3].getPosition();

      getWeightedNormal (anormals[0], p1, p2, p3);
      getWeightedNormal (anormals[1], p0, p3, p2);
      getWeightedNormal (anormals[2], p0, p1, p3);
      getWeightedNormal (anormals[3], p0, p2, p1);
   }

   private final void getWeightedNormal (
      Vector3d awn, Point3d p0, Point3d p1, Point3d p2) {
      double ux = p1.x - p0.x;
      double uy = p1.y - p0.y;
      double uz = p1.z - p0.z;

      double vx = p2.x - p0.x;
      double vy = p2.y - p0.y;
      double vz = p2.z - p0.z;

      awn.x = 0.5 * (uy * vz - uz * vy);
      awn.y = 0.5 * (uz * vx - ux * vz);
      awn.z = 0.5 * (ux * vy - uy * vx);
   }

   public double getIncompDerivative (Vector3d tmp1, Vector3d tmp2) {

      Point3d p0 = myNodes[0].getPosition();
      Point3d p1 = myNodes[1].getPosition();
      Point3d p2 = myNodes[2].getPosition();
      Point3d p3 = myNodes[3].getPosition();

      Vector3d v0 = myNodes[0].getVelocity();
      Vector3d v1 = myNodes[1].getVelocity();
      Vector3d v2 = myNodes[2].getVelocity();
      Vector3d v3 = myNodes[3].getVelocity();

      double d = 0;

      tmp1.sub (p3, p2);
      tmp2.cross (tmp1, v1);
      tmp1.sub (p1, p3);
      tmp2.crossAdd (tmp1, v2, tmp2);
      tmp1.sub (p2, p1);
      tmp2.crossAdd (tmp1, v3, tmp2);
      d += tmp2.dot (v0);

      tmp1.sub (p3, p0);
      tmp2.cross (tmp1, v2);
      tmp1.sub (p0, p2);
      tmp2.crossAdd (tmp1, v3, tmp2);
      d += tmp2.dot (v1);

      tmp1.sub (p1, p0);
      tmp2.cross (tmp1, v3);
      d += tmp2.dot (v2);

      return -d/3.0;
   }

   public boolean getMarkerCoordinates (
      VectorNd coords, Vector3d ncoords, Point3d pos, boolean checkInside) {
      if (coords.size() != 4) {
         throw new IllegalArgumentException (
            "coords should have size 4 for tetrahedral elements");
      }
      Vector3d del = new Vector3d();
      Matrix3d A = new Matrix3d();

      for (int j = 1; j < 4; j++) {
         del.sub (myNodes[j].getPosition(), myNodes[0].getPosition());
         A.setColumn (j - 1, del);
      }
      A.invert();
      del.sub (pos, myNodes[0].getPosition());
      del.mul (A, del);
      coords.set (0, 1 - del.x - del.y - del.z);
      coords.set (1, del.x);
      coords.set (2, del.y);
      coords.set (3, del.z);
      if (ncoords != null) {
         ncoords.set (del);
      }
      if (checkInside) {
         return isInside (coords);
      }
      else {
         return true;
      }
   }

   private boolean isInside (VectorNd coords) {
      return (coords.get (0) >= 0 && coords.get (1) >= 0 &&
              coords.get (2) >= 0 && coords.get (3) >= 0);      
   }
   
   public boolean isInside (Point3d pnt) {
      VectorNd v = new VectorNd (4);
      getMarkerCoordinates (v, null, pnt, false);
      return isInside (v);
   }

   public static boolean isInside (
      Point3d pnt, Point3d p0, Point3d p1, Point3d p2, Point3d p3) {

      // first, get the elements of the matrix whose columns are
      // the coordinate vectors p1-p0, p2-p0, p3-p0
      double m00 = p1.x-p0.x;
      double m10 = p1.y-p0.y;
      double m20 = p1.z-p0.z;

      double m01 = p2.x-p0.x;
      double m11 = p2.y-p0.y;
      double m21 = p2.z-p0.z;

      double m02 = p3.x-p0.x;
      double m12 = p3.y-p0.y;
      double m22 = p3.z-p0.z;

      // get the position of pnt WRT p0
      double px = pnt.x-p0.x;
      double py = pnt.y-p0.y;
      double pz = pnt.z-p0.z;

      // compute determinant of this matrix, so that we can
      // invert it to find the coordinates of the point with respect
      // to the coordinate vectors

      double det =
         m00*(m22*m11-m21*m12)-m10*(m22*m01-m21*m02)+m20*(m12*m01-m11*m02);
      
      double l1, l2, l3;

      l1 =  (m22*m11-m21*m12)*px-(m22*m01-m21*m02)*py+(m12*m01-m11*m02)*pz;
      if (l1*det < 0) {
         return false;
      }
      l2 = -(m22*m10-m20*m12)*px+(m22*m00-m20*m02)*py-(m12*m00-m10*m02)*pz;
      if (l2*det < 0) {
         return false;
      }
      l3 =  (m21*m10-m20*m11)*px-(m21*m00-m20*m01)*py+(m11*m00-m10*m01)*pz;
      if (l3*det < 0) {
         return false;
      }
      if (det >= 0) {
         return det-l1-l2-l3 >= 0;
      }
      else {
         return det-l1-l2-l3 <= 0;
      }
   }

   public boolean hasEdge (FemNode3d n0, FemNode3d n1) {
      return (containsNode (n0) &&
              containsNode (n1));
   }

   public boolean hasFace (FemNode3d n0, FemNode3d n1, FemNode3d n2) {
      return (containsNode (n0) &&
              containsNode (n1) && 
              containsNode (n2));
   }
   
}
