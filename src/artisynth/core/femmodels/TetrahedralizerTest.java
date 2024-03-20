package artisynth.core.femmodels;

import java.util.*;
import maspack.matrix.*;
import maspack.geometry.*;
import maspack.util.*;

/**
 * Test class for tetrahedralizer
 */
public class TetrahedralizerTest extends UnitTest {

   class Edge {
      int num0;
      int num1;

      Edge (int num0, int num1) {
         this.num0 = num0;
         this.num1 = num1;
      }

      public boolean equals (Object obj) {
         if (obj instanceof Edge) {
            Edge edge = (Edge)obj;
            return ((edge.num0==num0 && edge.num1==num1) ||
                    (edge.num0==num1 && edge.num1==num0));
         }
         else {
            return false;
         }
      }

      @Override
      public int hashCode() {
         return num0 + num1;
      }
   }

   class VecCompare implements Comparator<VectorNi> {
      public int compare (VectorNi vec0, VectorNi vec1) {
         for (int i=0; i<vec0.size(); i++) {
            if (vec0.get(i) < vec1.get(i)) {
               return -1;
            }
            else if (vec0.get(i) > vec1.get(i)) {
               return 1;
            }
         }
         return 0;
      }
   }

   Point3d[] myBaseCoords;

   Point3d[] getBaseCoords() {
      if (myBaseCoords == null) {
         double[] coords = (new HexElement()).getNodeCoords();
         myBaseCoords = new Point3d[coords.length/3];
         for (int i=0; i<myBaseCoords.length; i++) {
            myBaseCoords[i] = new Point3d (
               coords[i*3+0], coords[i*3+1], coords[i*3+2]);
         }
      }
      return myBaseCoords;
   };           

   public TetrahedralizerTest() {
   }

   FemModel3d createSingleHexFem() { 
      FemModel3d fem = new FemModel3d();
      FemNode3d[] nodes = new FemNode3d[8];
      Point3d[] baseCoords = getBaseCoords();
      for (int i=0; i<8; i++) {
         nodes[i] = new FemNode3d(baseCoords[i]);
         fem.addNode (nodes[i]);
      }
      HexElement hex = new HexElement (
         nodes[0], nodes[1], nodes[2], nodes[3], 
         nodes[4], nodes[5], nodes[6], nodes[7]);
      return fem;
  }

   void printAllCodes() {
      Tetrahedralizer tetzer = new Tetrahedralizer();
      for (int code=0; code<64; code++) {
         VectorNi diagCnt = tetzer.parseCode(code);
         StringBuilder sb = new StringBuilder();
         for (int i=0; i<3; i++) {

            boolean onZero = false;
            boolean cross = false;
            switch (i) {
               case 0: {
                  onZero = ((code & Tetrahedralizer.DIAG_02) != 0);
                  cross =  ((code & Tetrahedralizer.F0123_CROSS) != 0);
                  break;
               }
               case 1:{
                  onZero = ((code & Tetrahedralizer.DIAG_07) != 0);
                  cross =  ((code & Tetrahedralizer.F0374_CROSS) != 0);
                     break;
               }
               case 2: {
                  onZero = ((code & Tetrahedralizer.DIAG_05) != 0);
                  cross =  ((code & Tetrahedralizer.F0451_CROSS) != 0);
                  break;
               }
            }
            if (onZero) {
               sb.append (" ON ");
            }
            else {
               sb.append (" OFF");
            }
            if (cross) {
               sb.append (" X ");
            }
            else {
               sb.append (" / ");
            }
         }
         int num3 = 0;
         for (int i=0; i<8; i++) {
            if (diagCnt.get(i) == 3) {
               num3++;
            }
         }
         if (num3 == 0) {
            sb.append (" ILLEGAL");
         }
         System.out.println (diagCnt + " " + sb.toString());
      }
   }

   boolean codeIsLegal (VectorNi diagCnt) {
      int num3 = 0;
      for (int i=0; i<8; i++) {
         if (diagCnt.get(i) == 3) {
            num3++;
         }
      }
      return num3 > 0;
   }

   boolean isFaceDiagonal (int num0, int num1) {
      if (num0 > num1) {
         int tmp = num1; num1 = num0; num0 = tmp;
      }
      switch (num0) {
         case 0: {
            return num1 == 2 || num1 == 5 || num1 == 7;
         }
         case 1: {
            return num1 == 3 || num1 == 4 || num1 == 6;
         }
         case 2: {
            return num1 == 5 || num1 == 7;
         }
         case 3: {
            return num1 == 4 || num1 == 6;
         }
         case 4: {
            return num1 == 6;
         }
         case 5: {
            return num1 == 7;
         }
         default: {
            return false;
         }
      }
   }

   void checkAllCodes() {
      Tetrahedralizer tetzer = new Tetrahedralizer();
      FemModel3d fem = createSingleHexFem();
      FemNode3d[] femNodes = fem.getNodes().toArray(new FemNode3d[0]);
      for (int code=0; code<64; code++) {
         VectorNi diagCnt = tetzer.parseCode(code);
         if (codeIsLegal (diagCnt)) {
            TetElement[] tets = tetzer.subdivideHex (femNodes, code);
            VectorNi checkCnt = new VectorNi(8);
            HashSet<Edge> edges = new HashSet<>();
            for (TetElement tet : tets) {
               FemNode3d[] nodes = tet.getNodes();
               int[] edgeIdxs = tet.getEdgeIndices();
               for (int i=0; i<6; i++) {
                  int num0 = nodes[edgeIdxs[3*i+1]].getNumber();
                  int num1 = nodes[edgeIdxs[3*i+2]].getNumber();
                  if (isFaceDiagonal (num0, num1)) {
                     Edge edge = new Edge (num0, num1);
                     if (!edges.contains(edge)) {
                        edges.add (edge);
                        checkCnt.add (num0, 1);
                        checkCnt.add (num1, 1);
                     }
                  }
               }
            }
            if (!checkCnt.equals(diagCnt)) {
               throw new TestException (
                  "hex subdivision produces unexpected diagonal cnt: "+
                  diagCnt + " vs. expected " + checkCnt);
            }
         }
      }
   }

   ArrayList<VectorNi> getIndexSets() {
      ArrayList<VectorNi> indexSets = new ArrayList<>();
      Point3d[] baseCoords = getBaseCoords();
      for (AxisAlignedRotation rot : AxisAlignedRotation.values()) {
         // create a base FEM and transform it
         FemModel3d fem = createSingleHexFem();
         RigidTransform3d T = new RigidTransform3d();
         T.R.set (rot.getMatrix());
         fem.transformGeometry (T);
         VectorNi indices = new VectorNi(8);
         for (int i=0; i<8; i++) {
            FemNode3d node = fem.findNearestNode(baseCoords[i], 1e-8);
            indices.set (i, node.getNumber());
         }
         indexSets.add (indices);
      }
      // sort the indices by first vertex
      Collections.sort (indexSets, new VecCompare());
      return indexSets;
   }

   void printIndexSets() {
      ArrayList<VectorNi> indexSets = getIndexSets();
      for (VectorNi indices : indexSets) {
         System.out.println (indices);
      }
   }

   void printDiagAdjacency (int[] dcnt0) {
      ArrayList<VectorNi> dcnts = new ArrayList<>();
      for (VectorNi indices : getIndexSets()) {
         VectorNi dcnt = new VectorNi(8);
         for (int i=0; i<8; i++) {
            dcnt.set (indices.get(i), dcnt0[i]);
         }
         dcnts.add (dcnt);
      }
      Collections.sort (dcnts, new VecCompare());
      ArrayList<VectorNi> unique = new ArrayList<>();
      VectorNi prev = null;
      for (VectorNi dcnt : dcnts) {
         if (prev == null || !prev.equals(dcnt)) {
            unique.add (dcnt);
         }
         prev = dcnt;
      }
      for (VectorNi dcnt : unique) {
         System.out.println (dcnt);
      }
   }

   int[] myVmap = new int[] { -1, 0, 1, 5, 4, 3, 2, 6, 7};

   void printConvert (int[] vidxs) {
      for (int i=0; i<vidxs.length; i += 4) {
         System.out.printf (
            "new int[] {%d, %d, %d, %d},\n",
            myVmap[vidxs[i]], myVmap[vidxs[i+1]],
            myVmap[vidxs[i+2]], myVmap[vidxs[i+3]]);
      }
   }

   public void test() {
      checkAllCodes();
   }

   public static void main (String[] args) {
      TetrahedralizerTest tester = new TetrahedralizerTest();

      tester.runtest();
   }
}
