package artisynth.core.femmodels;

import maspack.util.*;
import maspack.matrix.*;

import java.util.*;

public class FemElement3dBaseTest extends UnitTest {

   ArrayList<FemElement3dBase> myElements;
   ArrayList<FemNode3d> myNodes;

   void createTestElements() {
      myElements = new ArrayList<FemElement3dBase>();
      myNodes = new ArrayList<FemNode3d>();
      // create a grid of nodes to be used to create elements
      for (int k=0; k<3; k++) {
         for (int j=0; j<3; j++) {
            for (int i=0; i<3; i++) {
               myNodes.add (new FemNode3d (i, j, k));
            }
         }
      }
      FemNode3d[] n = myNodes.toArray (new FemNode3d[0]);
      myElements.add (
         new TetElement (n[0], n[1], n[3], n[9]));
      myElements.add (
         new HexElement (n[0], n[3], n[4], n[1], n[9], n[12], n[13], n[10]));
      myElements.add (
         new WedgeElement (n[0], n[1], n[3], n[9], n[10], n[12]));
      myElements.add (
         new PyramidElement (n[0], n[1], n[4], n[3], n[9]));
      myElements.add (
         new QuadtetElement (
            n[0], n[2], n[6], n[18], n[1], n[3], n[9], n[4], n[10], n[12]));
      FemNode3d[] quadhexNodes = new FemNode3d[] {
         n[0], n[6], n[8], n[2], n[18], n[24], n[26], n[20],
         n[3], n[7], n[5], n[1], n[21], n[25], n[23], n[19],
         n[9], n[15], n[17], n[11]
      };
      myElements.add (new QuadhexElement (quadhexNodes));
      FemNode3d[] quadwedgeNodes = new FemNode3d[] {
         n[0], n[2], n[6], n[18], n[20], n[24],
         n[1], n[4], n[3], n[19], n[22], n[21], n[9], n[11], n[15]
      };
      myElements.add (new QuadwedgeElement (quadwedgeNodes));

      FemNode3d[] quadpyramidNodes = new FemNode3d[] {
         n[0], n[2], n[8], n[6], n[18],
         n[1], n[5], n[7], n[3], n[9], n[10], n[13], n[12]
      };
      myElements.add (new QuadpyramidElement (quadpyramidNodes));

      myElements.add (new ShellTriElement (n[0], n[3], n[1], 0.1, false));

      myElements.add (new ShellQuadElement (n[0], n[3], n[4], n[1], 0.1, false));
   }

   public FemElement3dBaseTest() {
      createTestElements();
   }

   void testContainsEdge (FemElement3dBase e) {
      int[] edgeIdxs = e.getEdgeIndices();
      int k = 0;
      FemNode3d[] nodes = e.getNodes();
      while (k < edgeIdxs.length) {
         int nv = edgeIdxs[k++];
         if (nv == 2) {
            FemNode3d n0 = nodes[edgeIdxs[k++]];
            FemNode3d n1 = nodes[edgeIdxs[k++]];
            if (!e.containsEdge (n0, n1) ||
                !e.containsEdge (n1, n0)) {
               throw new TestException ("edge not found for " + e);
            }
         }
         else {
            k += nv;
         }
      }
   }

   void testContainsEdge() {
      for (FemElement3dBase e : myElements) {
         testContainsEdge (e);
      }
   }      
   
   void testContainsFace (FemElement3dBase e) {
      int[] faceIdxs = e.getFaceIndices();
      int k = 0;
      FemNode3d[] nodes = e.getNodes();
      while (k < faceIdxs.length) {
         int nv = faceIdxs[k++];
         if (nv == 3) {
            FemNode3d n0 = nodes[faceIdxs[k++]];
            FemNode3d n1 = nodes[faceIdxs[k++]];
            FemNode3d n2 = nodes[faceIdxs[k++]];
            if (!e.containsFace (n0, n1, n2) ||
                !e.containsFace (n0, n2, n1) ||
                !e.containsFace (n2, n0, n1) ||
                !e.containsFace (n2, n1, n0)) {
               throw new TestException ("tet face not found for " + e);
            }
         }
         else if (nv == 4) {
            FemNode3d n0 = nodes[faceIdxs[k++]];
            FemNode3d n1 = nodes[faceIdxs[k++]];
            FemNode3d n2 = nodes[faceIdxs[k++]];
            FemNode3d n3 = nodes[faceIdxs[k++]];
            if (!e.containsFace (n0, n1, n2, n3) ||
                !e.containsFace (n0, n3, n2, n1) ||
                !e.containsFace (n3, n0, n1, n2) ||
                !e.containsFace (n3, n2, n1, n0)) {
               throw new TestException ("quad face not found for " + e);
            }
         }
         else {
            k += nv;
         }
      }
   }

   void testContainsFace() {
      for (FemElement3dBase e : myElements) {
         testContainsFace (e);
      }
   }      

   void testNodalExtrapolationMatrix (FemElement3dBase elem) {
      MatrixNd E = elem.getNodalExtrapolationMatrix();
      MatrixNd N = elem.getIntegrationShapeMatrix();
      MatrixNd P = new MatrixNd();
      if (E.rowSize() >= E.colSize()) {
         P.mul (N, E);
      }
      else {
         P.mul (E, N);
      }
      MatrixNd I = new MatrixNd(P.colSize(), P.colSize());      
      I.setIdentity();
      checkEquals ("extrapolation/shape matrix product", P, I, 1e-10);      
   }

   public void test() {
      testContainsEdge();
      testContainsFace();
      for (FemElement3dBase e : myElements) {
         testNodalExtrapolationMatrix (e);
      }
   }

   public static void main (String[] args) {
      FemElement3dBaseTest tester = new FemElement3dBaseTest();
      tester.runtest();
   }

}
