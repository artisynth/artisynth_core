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

public class PyramidElement extends FemElement3d {

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
            createIntegrationPoints (new PyramidElement());
      }
      return myDefaultIntegrationPoints;
   }

   public IntegrationPoint3d getWarpingPoint() {
      if (myWarpingPoint == null) {
         myWarpingPoint = IntegrationPoint3d.create (
            this, 0, 0, -0.5, 128/27.0);
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

   private static double[] myIntegrationCoords;

   public int numIntegrationPoints() {
      return myIntegrationCoords.length/4;
   }

   private static double[] myNodeCoords = new double[] 
      {
         -1, -1, -1,
         +1, -1, -1,
         +1,  1, -1,
         -1,  1, -1,
         +0,  0,  1
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
      0.20,
      0.20,
      0.20,
      0.20,
      0.20
   };

   public double[] getNodeMassWeights () {
      return myNodeMassWeights;
   }

   private static MatrixNd myNodalExtrapolationMatrix = null;

   public MatrixNd getNodalExtrapolationMatrix() {
      if (myNodalExtrapolationMatrix == null) {
         // For now, just use integration point values at corresponding nodes
         myNodalExtrapolationMatrix = new MatrixNd (5, 5);
         myNodalExtrapolationMatrix.setIdentity();
      }
      return myNodalExtrapolationMatrix;
   }

   // Shape functions are the same as those used by FEBio for
   // pentahedral elements:
   //
   public double getN (int i, Vector3d coords) {
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
               "Shape function index must be in range [0,"+(numNodes()-1)+"]");
      }
   }

   public void getdNds (Vector3d dNds, int i, Vector3d coords) {
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
               "Shape function index must be in range [0,"+(numNodes()-1)+"]");
      }
   }

   public PyramidElement() {
      myNodes = new FemNode3d[5];
   }

   /**
    * Creates a PyramidElement from five nodes. The first four nodes should
    * describe the bottom face of the element, arranged clockwise about
    * the outward-directed normal. The last node is the top node.
    */
   public PyramidElement (FemNode3d n1, FemNode3d n2, FemNode3d n3,
                          FemNode3d n4, FemNode3d n5) {

      myNodes = new FemNode3d[5];

      myNodes[0] = n1;
      myNodes[1] = n2;
      myNodes[2] = n3;
      myNodes[3] = n4;
      myNodes[4] = n5;
   }

   public PyramidElement (FemNode3d[] nodes) {
      if (nodes.length != 5) {
         throw new IllegalArgumentException ("nodes must have length 5");
      }
      myNodes = nodes.clone();
   }

   /**
    * Create a tesselation for a cubic configuration of nodes, using a 3
    * pyramids. The first four particles should define a single
    * counter-clockwise face, while the last four should give the corresponding
    * (clockwise) nodes for the opposite face. The parameter
    * <code>apexNode</code> defines the node where all three pyramid
    * top nodes meet. This corner will be opposite the one where all
    * three quad faces meet.
    */
   public static PyramidElement[] createCubeTesselation (
      FemNode3d p0, FemNode3d p1, FemNode3d p2, FemNode3d p3, FemNode3d p4,
      FemNode3d p5, FemNode3d p6, FemNode3d p7, int apexNode) {

      PyramidElement[] elems = new PyramidElement[3];
      switch (apexNode) {
         case 0: {
            elems[0] = new PyramidElement (p6, p2, p3, p7, p0);
            elems[1] = new PyramidElement (p6, p5, p1, p2, p0);
            elems[2] = new PyramidElement (p6, p7, p4, p5, p0);
            break;
         }
         case 1: {
            elems[0] = new PyramidElement (p7, p6, p2, p3, p1);
            elems[1] = new PyramidElement (p7, p3, p0, p4, p1);
            elems[2] = new PyramidElement (p7, p4, p5, p6, p1);
            break;
         }
         case 2: {
            elems[0] = new PyramidElement (p4, p5, p6, p7, p2);
            elems[1] = new PyramidElement (p4, p0, p1, p5, p2);
            elems[2] = new PyramidElement (p4, p7, p3, p0, p2);
            break;
         }
         case 3: {
            elems[0] = new PyramidElement (p5, p1, p2, p6, p3);
            elems[1] = new PyramidElement (p5, p4, p0, p1, p3);
            elems[2] = new PyramidElement (p5, p6, p7, p4, p3);
            break;
         }
         case 4: {
            elems[0] = new PyramidElement (p2, p3, p7, p6, p4);
            elems[1] = new PyramidElement (p2, p1, p0, p3, p4);
            elems[2] = new PyramidElement (p2, p6, p5, p1, p4);
            break;
         }
         case 5: {
            elems[0] = new PyramidElement (p3, p2, p1, p0, p5);
            elems[1] = new PyramidElement (p3, p0, p4, p7, p5);
            elems[2] = new PyramidElement (p3, p7, p6, p2, p5);
            break;
         }
         case 6: {
            elems[0] = new PyramidElement (p0, p3, p2, p1, p6);
            elems[1] = new PyramidElement (p0, p1, p5, p4, p6);
            elems[2] = new PyramidElement (p0, p4, p7, p3, p6);
            break;
         }
         case 7: {
            elems[0] = new PyramidElement (p1, p0, p3, p2, p7);
            elems[1] = new PyramidElement (p1, p2, p6, p5, p7);
            elems[2] = new PyramidElement (p1, p5, p4, p0, p7);
            break;
         }
         default:{
            throw new IllegalArgumentException (
               "apexNode must be in the range [0-7]");
         }
      }
      return elems;
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
         2,   1, 2,
         2,   2, 3,
         2,   3, 0,   
         2,   0, 4,
         2,   1, 4,
         2,   2, 4,
         2,   3, 4,   
      };

   static int[] myFaceIdxs = new int[] 
      {
         4,   0, 3, 2, 1,
         3,   0, 1, 4,
         3,   1, 2, 4,
         3,   2, 3, 4,
         3,   3, 0, 4,
      };

   static int[] myTriangulatedFaceIdxs = new int[] 
      {
         0, 3, 2, 
         0, 2, 1,
         0, 1, 4,
         1, 2, 4,
         2, 3, 4,
         3, 0, 4,
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
    * Cpmputes the exact volume of the pyramid
    *
    * @return element volume
    */
   private double computeVolume (
      Point3d p0, Point3d p1, Point3d p2,
      Point3d p3, Point3d p4) {

      double vol = 0;
      // to compute the volume without bias, we take the average volume of two
      // complementary two-tetrahedra tesselations.
      vol += TetElement.computeVolume (p0, p1, p2, p4);
      vol += TetElement.computeVolume (p0, p2, p3, p4);

      vol += TetElement.computeVolume (p3, p1, p2, p4);
      vol += TetElement.computeVolume (p0, p1, p3, p4);

      return vol /= 2;
   }

   public boolean isInside (Point3d pnt) {
      Point3d p0 = myNodes[0].getPosition();
      Point3d p1 = myNodes[1].getPosition();
      Point3d p2 = myNodes[2].getPosition();
      Point3d p3 = myNodes[3].getPosition();
      Point3d p4 = myNodes[4].getPosition();

      // XXX should fix this - we really need the compute the natural coords
      // and test them

      return (TetElement.isInside (pnt, p0, p1, p2, p4) ||
              TetElement.isInside (pnt, p0, p2, p3, p4));
   }
}
