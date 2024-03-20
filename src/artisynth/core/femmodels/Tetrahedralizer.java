package artisynth.core.femmodels;

import java.util.*;
import maspack.matrix.*;
import maspack.geometry.*;
import maspack.util.*;

/**
 * Class to subdivide hexes and wedges into tetrahedra. The subdivisions used
 * in this paper are based on those described in ``How to subdivide pyramids,
 * prisms, and hexahedra into tetrahedra'', by Dompierre, et al., International
 * Meshing Roundtable Conference, 1999.
 */
public class Tetrahedralizer {

   /**
    * Wedge subdivision flag to use the diagonal 1-3 on face (0,1,3,4).
    */
   public static final int DIAG_13 = 0x0001;

   /**
    * Wedge subdivision flag to use the diagonal 2-4 on face (1,2,5,4).
    */
   public static final int DIAG_24 = 0x0002;
   
   /**
    * Hex or wedge subdivision flag to use the diagonal 0-5 on face (0,4,5,1)
    * for hexes or on face (0,2,5,3) for wedges.
    */
   public static final int DIAG_05 = 0x0004;
   
   /**
    * Hex subdivision flag to use the diagonal 0-2 on face (0,1,2,3).
    */
   public static final int DIAG_02 = 0x0008;

   /**
    * Hex subdivision flag to use the diagonal 0-7 on face (0,3,7,4).
    */
   public static final int DIAG_07 = 0x0010;

   /**
    * Hex subdivision flags to use diagonals 0-2, 0-7, and 0-5 on all faces
    * adjacent to node 0.
    */
   public static final int ZERO_DIAGS = (DIAG_02|DIAG_07|DIAG_05);

   /**
    * Hex subdivision flag requiring that the diagonals on face (0,1,2,3) 
    * its opposite are aligned in opposing directions.
    */
   public static final int F0123_CROSS = 0x0100;

   /**
    * Hex subdivision flag requiring that the diagonals on face (0,3,7,4) and
    * its opposite are aligned in opposing directions.
    */
   public static final int F0374_CROSS = 0x0200;

   /**
    * Hex subdivision flag requiring that the diagonals on face (0,4,5,1) and
    * its opposite are aligned in opposing directions.
    */
   public static final int F0451_CROSS = 0x0400;

   /**
    * Hex subdivision flag requiring that the diagonals on all faces have
    * opposing directions to those on their opposite face.
    */
   public static final int ALL_CROSS =
      (F0123_CROSS|F0374_CROSS|F0451_CROSS);

   /**
    * Repositionings that place vertices 0, 1, 2, ... into the "0" position.
    */
   private static int[][] myRotatedVidxs = new int[][] {
      new int[] { 0, 1, 2, 3, 4, 5, 6, 7 },
      new int[] { 1, 5, 6, 2, 0, 4, 7, 3 },
      new int[] { 2, 1, 5, 6, 3, 0, 4, 7 },
      new int[] { 3, 0, 1, 2, 7, 4, 5, 6 },
      new int[] { 4, 0, 3, 7, 5, 1, 2, 6 },
      new int[] { 5, 4, 7, 6, 1, 0, 3, 2 },
      new int[] { 6, 5, 4, 7, 2, 1, 0, 3 },
      new int[] { 7, 4, 0, 3, 6, 5, 1, 2 },
   };

   /**
    * Create a tesselation for a cubic configuration of nodes, using either 5
    * or 6 tets as determined by the flags in {@code code}, which should be
    * some combination of {@link #DIAG_02}, {@link #DIAG_07}, {@link #DIAG_05},
    * {@link #F0123_CROSS}, {@link #F0374_CROSS}, or {@link #F0451_CROSS}.  The
    * first four nodes should define a single counter-clockwise face, while the
    * last four should give the corresponding (clockwise) nodes for the
    * opposite face.
    */
   public TetElement[] subdivideHex (FemNode3d[] nodes, int code) {
      VectorNi diagCnt = parseCode (code);
      int[][] tetIdxs = getTetIndicesForHex (diagCnt);
      TetElement[] tets = new TetElement[tetIdxs.length];
      for (int i=0; i<tetIdxs.length; i++) {
         int[] idxs = tetIdxs[i];
         for (int j=0; j<4; j++) {
            tets[i] = new TetElement (
               nodes[idxs[0]], nodes[idxs[1]], nodes[idxs[2]], nodes[idxs[3]]);
         }
      }
      return tets;
   }

   /**
    * Create a tesselation for a cubic configuration of nodes, using either 5
    * or 6 tets as determined by the flags in {@code code}, which should be
    * some combination of {@link #DIAG_02}, {@link #DIAG_07}, {@link #DIAG_05},
    * {@link #F0123_CROSS}, {@link #F0374_CROSS}, or {@link #F0451_CROSS}.  The
    * first four nodes should define a single counter-clockwise face, while the
    * last four should give the corresponding (clockwise) nodes for the
    * opposite face.
    */
   public TetElement[] subdivideHex (
      FemNode3d n0, FemNode3d n1, FemNode3d n2, FemNode3d n3,
      FemNode3d n4, FemNode3d n5, FemNode3d n6, FemNode3d n7, int code) {
      return subdivideHex (
         new FemNode3d[] {n0, n1, n2, n3, n4, n5, n6, n7}, code);
   }

   /**
    * Create a tesselation for a cubic configuration of nodes, using a
    * Freudenthal cut. The first four nodes should define a single
    * counter-clockwise face, while the last four should give the corresponding
    * (clockwise) nodes for the opposite face. If {@code allOnX} is {@code
    * true}, it means that nodes n0, n2, n5, and n7 form the internal
    * tetrahedron of the Freudenthal cut. Otherwise, this tetrahedron is formed
    * from nodes n1, n3, n4 and n7.
    */
   public TetElement[] subdivideHex (
      FemNode3d n0, FemNode3d n1, FemNode3d n2, FemNode3d n3,
      FemNode3d n4, FemNode3d n5, FemNode3d n6, FemNode3d n7, boolean allOnX) {
      return subdivideHex (
         new FemNode3d[] {n0, n1, n2, n3, n4, n5, n6, n7},
         allOnX ? ALL_CROSS|ZERO_DIAGS : ALL_CROSS);
   }

   /**
    * Create a tesselation for a wedge configuration of nodes. The first three
    * nodes should define a single counter-clockwise face, while the last three
    * should give the corresponding (clockwise) nodes for the opposite
    * face. The tesselation type is controlled by a code <code>type</code>,
    * which is an ored combination of {@link #DIAG_13}, {@link #DIAG_24}, and
    * {@link #DIAG_05} describing the diagonal directions on faces (0,1,3,4),
    * (1,2,5,4), and (0,2,5,3).  Not all diagnonal combinations are possible,
    * so bit patterns 0x0 and {@code (DIAG_13|DIAG_24|DIAG_05)} are illegal.
    */
   public TetElement[] subdivideWedge (
      FemNode3d p0, FemNode3d p1, FemNode3d p2,
      FemNode3d p3, FemNode3d p4, FemNode3d p5, int type) {
      TetElement[] tets = new TetElement[3];

      switch (type) {
         case DIAG_13: {
            tets[0] = new TetElement (p0, p2, p1, p3);
            tets[1] = new TetElement (p2, p5, p1, p3);
            tets[2] = new TetElement (p1, p5, p4, p3);
            break;
         }
         case DIAG_24: {
            tets[0] = new TetElement (p0, p2, p1, p4);
            tets[1] = new TetElement (p0, p3, p2, p4);
            tets[2] = new TetElement (p2, p3, p5, p4);
            break;
         }
         case DIAG_13|DIAG_24: {
            tets[0] = new TetElement (p0, p2, p1, p3);
            tets[1] = new TetElement (p1, p2, p4, p3);
            tets[2] = new TetElement (p2, p5, p4, p3);
            break;
         }
         case DIAG_05: {
            tets[0] = new TetElement (p0, p2, p1, p5);
            tets[1] = new TetElement (p1, p4, p0, p5);
            tets[2] = new TetElement (p0, p4, p3, p5);
            break;
         }
         case DIAG_13|DIAG_05: {
            tets[0] = new TetElement (p0, p2, p1, p5);
            tets[1] = new TetElement (p0, p1, p3, p5);
            tets[2] = new TetElement (p1, p4, p3, p5);
            break;
         }
         case DIAG_24|DIAG_05: {
            tets[0] = new TetElement (p0, p2, p1, p4);
            tets[1] = new TetElement (p2, p0, p5, p4);
            tets[2] = new TetElement (p0, p3, p5, p4);
            break;
         }
         default: {
            throw new IllegalArgumentException (
               "Illegal or unknown configuration type: " + type);
         }
      }
      return tets;
   }

   VectorNi parseCode (int code) {
      VectorNi diagCnt = new VectorNi(8);

      int[][] allFaceIdxs = new int[][] {
         new int[] { 0, 1, 2, 3 },
         new int[] { 0, 3, 7, 4 },
         new int[] { 0, 4, 5, 1 }};

      int[][] allOppfIdxs = new int[][] {
         new int[] { 4, 5, 6, 7 },
         new int[] { 1, 2, 6, 5 },
         new int[] { 3, 7, 6, 2 }};
             
      for (int i=0; i<3; i++) {
         int[] faceIdxs = allFaceIdxs[i];
         int[] oppfIdxs = allOppfIdxs[i];

         boolean onZero = false;
         boolean cross = false;
         switch (i) {
            case 0: {
               onZero = ((code & DIAG_02) != 0);
               cross =  ((code & F0123_CROSS) != 0);
               break;
            }
            case 1:{
               onZero = ((code & DIAG_07) != 0);
               cross =  ((code & F0374_CROSS) != 0);
               break;
            }
            case 2: {
               onZero = ((code & DIAG_05) != 0);
               cross =  ((code & F0451_CROSS) != 0);
               break;
            }
         }
         
         int idx0, idx1, idx2, idx3;
         if (onZero) {
            idx0 = 0;
            idx1 = 2;
         }
         else {
            idx0 = 1;
            idx1 = 3;
         }
         if (!cross) {
            idx2 = idx0;
            idx3 = idx1;
         }
         else {
            idx2 = (idx0 == 0 ? 1 : 0);
            idx3 = (idx0 == 0 ? 3 : 2);
         }
         diagCnt.add (faceIdxs[idx0], 1);
         diagCnt.add (faceIdxs[idx1], 1);
         diagCnt.add (oppfIdxs[idx2], 1);
         diagCnt.add (oppfIdxs[idx3], 1);
      }
      return diagCnt;
   }

   int[][] getTetIndicesForHex (VectorNi diagCnt) {
      int num3 = 0;
      int num2 = 0;
      int num1 = 0;
      int first3 = -1;
      for (int i=0; i<8; i++) {
         switch (diagCnt.get(i)) {
            case 1: {
               num1++;
               break;
            }
            case 2: {
               num2++;
               break;
            }
            case 3: {
               num3++;
               if (first3 == -1) {
                  first3 = i;
               }
               break;
            }
         }
      }
      int[] vtxs;
      int[][] tets = null;
      if (num3 == 0) {
         throw new IllegalArgumentException (
            "Illegal configuration: one vertex must have 3 incident diagonals");
      }
      else if (num3 == 4) {
         // can split into 5 tets
         vtxs = myRotatedVidxs[0];
         if ((first3 % 2) == 0) {
            tets = new int[][] {
               new int[] {1, 0, 2, 5},
               new int[] {3, 0, 7, 2},
               new int[] {4, 0, 5, 7},
               new int[] {6, 2, 7, 5},
               new int[] {0, 2, 5, 7},
            };
         }
         else {
            tets = new int[][] {
               new int[] {0, 1, 4, 3},
               new int[] {2, 3, 6, 1},
               new int[] {5, 4, 1, 6},
               new int[] {7, 6, 3, 4},
               new int[] {1, 6, 4, 3},
            };
         }
      }
      else if (num3 == 2 && num2 == 2) {
         if (first3 < 6) {
            vtxs = myRotatedVidxs[first3];
         }
         else {
            throw new InternalErrorException (
               "Unknown configuration: first3=" + first3);
         }
         vtxs = rotateDiagonal (vtxs, diagCnt, 1);
         tets = new int[][] {
            new int[] {0, 2, 7, 3},
            new int[] {0, 1, 7, 2},
            new int[] {1, 6, 7, 2},
            new int[] {0, 7, 5, 4},
            new int[] {0, 7, 1, 5},
            new int[] {1, 7, 6, 5},
         };
      }
      else if (num3 == 1 && num2 == 3) {
         vtxs = myRotatedVidxs[first3];  
         vtxs = rotateDiagonal (vtxs, diagCnt, 0);    
         tets = new int[][] {
            new int[] {0, 3, 2, 6},
            new int[] {0, 4, 7, 6},
            new int[] {0, 7, 3, 6},
            new int[] {0, 1, 5, 2},
            new int[] {0, 4, 6, 5},
            new int[] {0, 6, 2, 5},
         };
      }
      else if (num3 == 2 && num1 == 6) {
         if (first3 < 4) {
            vtxs = myRotatedVidxs[first3];
         }
         else {
            throw new InternalErrorException (
               "Unknown configuration: first3=" + first3);
         }
         tets = new int[][] {
            new int[] {0, 5, 4, 6},
            new int[] {0, 4, 7, 6},
            new int[] {0, 7, 3, 6},
            new int[] {0, 2, 6, 3},
            new int[] {1, 2, 6, 0},
            new int[] {1, 6, 5, 0},
         };
      }
      else {
         throw new InternalErrorException (
            "Unknown configuration: num3="+num3+" num2="+num2+" num1=" + num1);
      }
      for (int i=0; i<tets.length; i++) {
         int[] tidxs = tets[i];
         for (int j=0; j<4; j++) {
            tidxs[j] = vtxs[tidxs[j]];
         }
      }
      return tets;
   }

   /**
    * Rotate the vertices about the 0-th vertex diagonal so that the diagonal
    * count at vertex 1 equals {@code cnt}.
    */
   int[] rotateDiagonal (int[] vidxs, VectorNi diagCnt, int cnt) {
      if (diagCnt.get(vidxs[1]) == cnt) {
         // no need to do anything - already there
         return vidxs;
      }
      else if (diagCnt.get(vidxs[4]) == cnt) {
         return rotateAbout06 (vidxs, /*positive=*/false);
      }
      else if (diagCnt.get(vidxs[3]) == cnt) {
         return rotateAbout06 (vidxs, /*positive=*/true);
      }
      else {
         throw new InternalErrorException (
            "cnt "+cnt+" not found on rotated vertices 1, 3, or 4");
      }
   }

   int[] rotateAbout06 (int[] vidxs, boolean positive) {
      int[] rot;
      if (positive) {
         rot = new int[] {0, 3, 7, 4, 1, 2, 6, 5 };
      }
      else {
         rot = new int[] {0, 4, 5, 1, 3, 7, 6, 2 };
      }
      int[] ridxs = new int[8];
      for (int i=0; i<8; i++) {
         ridxs[i] = vidxs[rot[i]];
      }
      return ridxs;
   }

   public Tetrahedralizer() {
   }
}
