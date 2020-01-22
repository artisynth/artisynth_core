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

public class WedgeElement extends FemElement3d {

   private static IntegrationPoint3d[] myDefaultIntegrationPoints;
   private static IntegrationPoint3d myWarpingPoint;
   private static FemElementRenderer myRenderer;

   /**
    * {@inheritDoc}
    */ 
   public boolean integrationPointsMapToNodes() {
      return true;
   }

   public IntegrationPoint3d[] getIntegrationPoints() {
      if (myDefaultIntegrationPoints == null) {
         myDefaultIntegrationPoints = 
            createIntegrationPoints (new WedgeElement());
      }
      return myDefaultIntegrationPoints;
   }

   public IntegrationPoint3d getWarpingPoint() {
      if (myWarpingPoint == null) {
         myWarpingPoint = IntegrationPoint3d.create (this, 1/3.0, 1/3.0, 0, 1);
         myWarpingPoint.setNumber(numIntegrationPoints());
      }
      return myWarpingPoint;
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

   private static double[] myNodeCoords = new double[] 
      {
         0, 0, -1,
         1, 0, -1,
         0, 1, -1,
         0, 0,  1,
         1, 0,  1,
         0, 1,  1,
      };         

   static {
      double a = 1.0/6.0;
      double b = 2.0/3.0;
      double q = 1/Math.sqrt(3); // quadrature point
      double w = 1.0/6.0;
      myIntegrationCoords = new double[]
         {
             a,  a, -q,  w,
             b,  a, -q,  w,
             a,  b, -q,  w,
             a,  a,  q,  w,
             b,  a,  q,  w,
             a,  b,  q,  w,
         };
   }

   public double[] getIntegrationCoords () {
      return myIntegrationCoords;
   }

   public double[] getNodeCoords() {
      return myNodeCoords;
   }

   private static double[] myNodeMassWeights = new double[] {
      1.0/6,
      1.0/6,
      1.0/6,
      1.0/6,
      1.0/6,
      1.0/6,
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
         double s0 = 2; // end triangles are scaled by 2
         double s1 = Math.sqrt(3);
         Vector3d[] ncoords = getScaledNodeCoords (1, null);
         for (int i=0; i<ncoords.length; i++) {
            Vector3d v = ncoords[i];
            v.x = v.x*s0 + (1-s0)/3;
            v.y = v.y*s0 + (1-s0)/3;
            v.z = v.z*s1;
         }
         myNodalExtrapolationMatrix =
            createNodalExtrapolationMatrix (ncoords, 6, this);
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
         case 0: return 0.5*(1-s1-s2)*(1-r);
         case 1: return 0.5*s1*(1-r);
         case 2: return 0.5*s2*(1-r);
         case 3: return 0.5*(1-s1-s2)*(1+r);
         case 4: return 0.5*s1*(1+r);
         case 5: return 0.5*s2*(1+r);
         default:
            throw new IllegalArgumentException (
               "Shape function index must be in range [0,"+(numNodes()-1)+"]");
      }
   }

   public void getdNds (Vector3d dNds, int i, Vector3d coords) {
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
               "Shape function index must be in range [0,"+(numNodes()-1)+"]");
      }
   }

   public WedgeElement() {
      myNodes = new FemNode3d[6];
   }

   /**
    * Creates a WedgeElement from six nodes. The first three nodes should
    * describe a single face of the element, arranged clockwise about
    * the outward-directed normal. The last three nodes should describe the
    * corresponding nodes on the opposite face (and will be arranged
    * counter-clockwise about that face's normal).
    */
   public WedgeElement (FemNode3d n1, FemNode3d n2, FemNode3d n3,
                        FemNode3d n4, FemNode3d n5, FemNode3d n6) {

      myNodes = new FemNode3d[6];

      myNodes[0] = n1;
      myNodes[1] = n2;
      myNodes[2] = n3;
      myNodes[3] = n4;
      myNodes[4] = n5;
      myNodes[5] = n6;
   }

   public WedgeElement (FemNode3d[] nodes) {
      if (nodes.length != 6) {
         throw new IllegalArgumentException ("nodes must have length 6");
      }
      myNodes = nodes.clone();
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

   static int[] myEdgeIdxs = new int[] 
      {
         2,   0, 1,
         2,   0, 2,
         2,   1, 2,
         2,   3, 4,
         2,   3, 5,
         2,   4, 5,
         2,   0, 3,
         2,   1, 4,
         2,   2, 5
      };

   static int[] myFaceIdxs = new int[] 
      {
         3,   0, 2, 1,
         3,   3, 4, 5,
         4,   0, 1, 4, 3,
         4,   1, 2, 5, 4,
         4,   2, 0, 3, 5
      };

   static int[] myTriangulatedFaceIdxs = new int[] 
      {
         0, 2, 1,
         3, 4, 5,
         0, 1, 4,
         0, 4, 3,
         1, 2, 5,
         1, 5, 4,
         2, 0, 3,
         2, 3, 5
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

   /** 
    * Computes the exact volume of the wedge
    *
    * @return element volume
    */
   private double computeVolume (
      Point3d p0, Point3d p1, Point3d p2,
      Point3d p3, Point3d p4, Point3d p5) {

      double vol = 0;
      // to compute the volume without bias, we take the average volume of two
      // complementary three-tetrahedra tesselations.
      vol += TetElement.computeVolume (p0, p1, p2, p4);
      vol += TetElement.computeVolume (p0, p3, p4, p5);
      vol += TetElement.computeVolume (p2, p4, p5, p0);

      vol += TetElement.computeVolume (p5, p4, p3, p1);
      vol += TetElement.computeVolume (p0, p3, p1, p2);
      vol += TetElement.computeVolume (p1, p5, p2, p3);

      return vol/2;
   }

   public boolean isInside (Point3d pnt) {
      Point3d p0 = myNodes[0].getPosition();
      Point3d p1 = myNodes[1].getPosition();
      Point3d p2 = myNodes[2].getPosition();
      Point3d p3 = myNodes[3].getPosition();
      Point3d p4 = myNodes[4].getPosition();
      Point3d p5 = myNodes[5].getPosition();

      return (TetElement.isInside (pnt, p0, p1, p2, p4) ||
              TetElement.isInside (pnt, p0, p3, p4, p5) ||
              TetElement.isInside (pnt, p2, p4, p5, p0));
   }
}
