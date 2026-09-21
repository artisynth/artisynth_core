/**
 * Copyright (c) 2026, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.solvers;

import java.util.Arrays;
import java.util.Random;

import maspack.matrix.Matrix;
import maspack.matrix.SparseCRSMatrix;
import maspack.util.InternalErrorException;

/**
 * Generates synthetic sparse matrices whose sparsity patterns match those
 * arising from 2D and 3D finite element models on structured grids.
 *
 * <p>A grid of {@code nx X ny X nz} nodes is divided into elements of the
 * requested {@link ElemType}, and the matrix pattern is formed from the
 * resulting node adjacency: nodes {@code i} and {@code j} are coupled if and
 * only if they share an element. Each node carries {@code dofsPerNode} degrees
 * of freedom (3 for 3D solid mechanics, 2 for 2D), so each node coupling
 * expands into a dense {@code dofsPerNode X dofsPerNode} block, exactly as in a
 * real FEM stiffness matrix.
 *
 * <p>Numeric values are assembled from element matrices of the form
 * <pre>
 *   K_e = k (I - 1/m 1 1^T) (X) A
 * </pre>
 * where {@code m} is the number of element nodes and {@code A} is a fixed
 * random SPD {@code dofsPerNode X dofsPerNode} block. Each element matrix is
 * therefore symmetric positive semi-definite, and adding a positive diagonal
 * mass term makes the assembled matrix SPD, with every block entry non-zero.
 *
 * <p>Matrices are returned in the 1-based upper triangular CRS form accepted by
 * {@link DirectSolver#analyze(double[],int[],int[],int,int)}. A bordered KKT
 * form, which is symmetric indefinite, may be produced using {@link
 * #addConstraints addConstraints()}.
 */
public class FemMatrixGenerator {

   /**
    * Element type used to subdivide the grid.
    */
   public enum ElemType {
      /**
       * 3D hexahedra; 8 nodes each, giving a 27 node stencil in the grid
       * interior.
       */
      HEX (3),

      /**
       * 3D tetrahedra, formed by a Kuhn subdivision of each grid cell into 6
       * tets; 4 nodes each, giving a 15 node interior stencil.
       */
      TET (3),

      /**
       * 2D quadrilaterals; 4 nodes each, giving a 9 node interior stencil.
       */
      QUAD (2),

      /**
       * 2D triangles, formed by splitting each grid cell along its lower
       * left to upper right diagonal; 3 nodes each, giving a 7 node interior
       * stencil.
       */
      TRI (2);

      private int myDimen;

      ElemType (int dimen) {
         myDimen = dimen;
      }

      /**
       * Returns the spatial dimension (2 or 3) of this element type.
       *
       * @return element spatial dimension
       */
      public int getDimension() {
         return myDimen;
      }
   }

   /**
    * Sparse symmetric matrix in the 1-based upper triangular CRS form used by
    * {@link DirectSolver}.
    */
   public static class CRSMatrix {
      /**
       * Number of rows (and columns).
       */
      public int size;

      /**
       * 1-based row start offsets, of length {@code size+1}.
       */
      public int[] rowOffs;

      /**
       * 1-based column indices of the upper triangular non-zeros.
       */
      public int[] colIdxs;

      /**
       * Values of the upper triangular non-zeros.
       */
      public double[] vals;

      /**
       * Matrix type, either {@link Matrix#SPD} or {@link Matrix#SYMMETRIC}.
       */
      public int type;

      /**
       * Returns the number of upper triangular non-zeros.
       *
       * @return number of stored values
       */
      public int numVals() {
         return rowOffs[size]-1;
      }

      /**
       * Returns the number of non-zeros in the full symmetric matrix.
       *
       * @return number of non-zeros in the full matrix
       */
      public long numFullVals() {
         return 2L*numVals() - size;
      }

      /**
       * Returns a copy of the values, so that a solver may destroy them.
       *
       * @return copy of the value array
       */
      public double[] copyVals() {
         return Arrays.copyOf (vals, vals.length);
      }

      /**
       * Computes the residual {@code ||M x - b||} for this matrix.
       *
       * @param x solution vector
       * @param b right hand side
       * @return residual norm
       */
      public double residual (double[] x, double[] b) {
         return DirectSolver.residual (
            rowOffs, colIdxs, vals, size, x, b, /*symmetric=*/true);
      }

      /**
       * Converts this matrix to a {@link SparseCRSMatrix}, for use with the
       * {@code Matrix} based solver methods. Only the upper triangle is set,
       * as expected by {@link DirectSolver#analyze(Matrix,int,int)}.
       *
       * @return matrix in {@code SparseCRSMatrix} form
       */
      public SparseCRSMatrix toSparseCRSMatrix() {
         SparseCRSMatrix S = new SparseCRSMatrix (size, size);
         S.setCRSValues (
            vals, colIdxs, rowOffs, numVals(), size,
            Matrix.Partition.UpperTriangular);
         return S;
      }
   }

   private int myNumNodes;
   private int myNumElems;

   /**
    * Returns the number of grid nodes used by the most recent call to {@link
    * #create create()}.
    *
    * @return number of nodes
    */
   public int numNodes() {
      return myNumNodes;
   }

   /**
    * Returns the number of elements used by the most recent call to {@link
    * #create create()}.
    *
    * @return number of elements
    */
   public int numElems() {
      return myNumElems;
   }

   /**
    * Creates a matrix for a cubic (or square) grid with approximately {@code
    * size} rows. The grid resolution is chosen so that the resulting matrix is
    * as close as possible to the requested size.
    *
    * @param type element type
    * @param size desired matrix size
    * @return generated matrix
    */
   public CRSMatrix createForSize (ElemType type, int size) {
      int dofs = type.getDimension();
      int numNodes = Math.max (8, size/dofs);
      int res;
      if (type.getDimension() == 3) {
         res = (int)Math.round (Math.cbrt (numNodes));
      }
      else {
         res = (int)Math.round (Math.sqrt (numNodes));
      }
      res = Math.max (2, res);
      if (type.getDimension() == 3) {
         return create (type, res, res, res, dofs);
      }
      else {
         return create (type, res, res, 1, dofs);
      }
   }

   /**
    * Creates a matrix for a grid of {@code nx X ny X nz} nodes. For 2D element
    * types, {@code nz} is ignored.
    *
    * @param type element type
    * @param nx number of nodes along x
    * @param ny number of nodes along y
    * @param nz number of nodes along z (3D only)
    * @param dofsPerNode degrees of freedom per node
    * @return generated matrix
    */
   public CRSMatrix create (
      ElemType type, int nx, int ny, int nz, int dofsPerNode) {

      if (type.getDimension() == 2) {
         nz = 1;
      }
      if (nx < 2 || ny < 2 || nz < 1) {
         throw new IllegalArgumentException (
            "grid resolution must be >= 2 in each used dimension");
      }
      if (dofsPerNode < 1) {
         throw new IllegalArgumentException ("dofsPerNode must be >= 1");
      }
      int[][] elems = createElements (type, nx, ny, nz);
      myNumNodes = nx*ny*nz;
      myNumElems = elems.length;

      int[][] adjacency = createAdjacency (elems, myNumNodes);
      CRSMatrix M = createPattern (adjacency, myNumNodes, dofsPerNode);
      assembleValues (M, elems, dofsPerNode);
      return M;
   }

   // ------------------------------------------------------------------
   // element creation
   // ------------------------------------------------------------------

   private int nodeIdx (int i, int j, int k, int nx, int ny) {
      return i + nx*(j + ny*k);
   }

   private int[][] createElements (ElemType type, int nx, int ny, int nz) {
      switch (type) {
         case HEX: {
            return createHexElements (nx, ny, nz);
         }
         case TET: {
            return createTetElements (nx, ny, nz);
         }
         case QUAD: {
            return createQuadElements (nx, ny);
         }
         case TRI: {
            return createTriElements (nx, ny);
         }
         default: {
            throw new UnsupportedOperationException (
               "Unimplemented element type " + type);
         }
      }
   }

   // corner offsets of a grid cell, indexed by the bits of the corner number:
   // bit 0 -> x, bit 1 -> y, bit 2 -> z
   private int cellNode (int corner, int i, int j, int k, int nx, int ny) {
      return nodeIdx (
         i + (corner & 0x1), j + ((corner>>1) & 0x1),
         k + ((corner>>2) & 0x1), nx, ny);
   }

   private int[][] createHexElements (int nx, int ny, int nz) {
      int[][] elems = new int[(nx-1)*(ny-1)*(nz-1)][];
      int ne = 0;
      for (int k=0; k<nz-1; k++) {
         for (int j=0; j<ny-1; j++) {
            for (int i=0; i<nx-1; i++) {
               int[] elem = new int[8];
               for (int c=0; c<8; c++) {
                  elem[c] = cellNode (c, i, j, k, nx, ny);
               }
               elems[ne++] = elem;
            }
         }
      }
      return elems;
   }

   // Kuhn subdivision: each cell is cut into the 6 tets formed by the paths
   // that walk from corner 000 to corner 111 by incrementing x, y and z in
   // each of the 6 possible orders. The result is conforming across cells.
   private static final int[][] KUHN_AXES = new int[][] {
      {0,1,2}, {0,2,1}, {1,0,2}, {1,2,0}, {2,0,1}, {2,1,0}
   };

   private int[][] createTetElements (int nx, int ny, int nz) {
      int[][] elems = new int[6*(nx-1)*(ny-1)*(nz-1)][];
      int ne = 0;
      for (int k=0; k<nz-1; k++) {
         for (int j=0; j<ny-1; j++) {
            for (int i=0; i<nx-1; i++) {
               for (int[] axes : KUHN_AXES) {
                  int[] elem = new int[4];
                  int corner = 0;
                  elem[0] = cellNode (corner, i, j, k, nx, ny);
                  for (int a=0; a<3; a++) {
                     corner |= (1<<axes[a]);
                     elem[a+1] = cellNode (corner, i, j, k, nx, ny);
                  }
                  elems[ne++] = elem;
               }
            }
         }
      }
      return elems;
   }

   private int[][] createQuadElements (int nx, int ny) {
      int[][] elems = new int[(nx-1)*(ny-1)][];
      int ne = 0;
      for (int j=0; j<ny-1; j++) {
         for (int i=0; i<nx-1; i++) {
            int[] elem = new int[4];
            for (int c=0; c<4; c++) {
               elem[c] = cellNode (c, i, j, 0, nx, ny);
            }
            elems[ne++] = elem;
         }
      }
      return elems;
   }

   private int[][] createTriElements (int nx, int ny) {
      int[][] elems = new int[2*(nx-1)*(ny-1)][];
      int ne = 0;
      for (int j=0; j<ny-1; j++) {
         for (int i=0; i<nx-1; i++) {
            int n00 = nodeIdx (i,   j,   0, nx, ny);
            int n10 = nodeIdx (i+1, j,   0, nx, ny);
            int n01 = nodeIdx (i,   j+1, 0, nx, ny);
            int n11 = nodeIdx (i+1, j+1, 0, nx, ny);
            // a single fixed diagonal is used for every cell; alternating the
            // diagonal would give every node all 8 of its neighbors, making
            // the stencil identical to that of QUAD
            elems[ne++] = new int[] { n00, n10, n11 };
            elems[ne++] = new int[] { n00, n11, n01 };
         }
      }
      return elems;
   }

   // ------------------------------------------------------------------
   // pattern creation
   // ------------------------------------------------------------------

   /**
    * Forms, for each node, the sorted list of nodes it is coupled to,
    * including itself.
    */
   private int[][] createAdjacency (int[][] elems, int numNodes) {
      // count the (duplicated) couplings for each node, then bin them into a
      // single flat array before sorting and removing duplicates
      int[] cnts = new int[numNodes+1];
      for (int[] elem : elems) {
         for (int n : elem) {
            cnts[n] += elem.length;
         }
      }
      for (int i=0; i<numNodes; i++) {
         cnts[i]++; // the node itself
      }
      int[] offs = new int[numNodes+1];
      int total = 0;
      for (int i=0; i<numNodes; i++) {
         offs[i] = total;
         total += cnts[i];
      }
      offs[numNodes] = total;
      int[] flat = new int[total];
      int[] pos = Arrays.copyOf (offs, numNodes);
      for (int i=0; i<numNodes; i++) {
         flat[pos[i]++] = i;
      }
      for (int[] elem : elems) {
         for (int n : elem) {
            int p = pos[n];
            for (int m : elem) {
               flat[p++] = m;
            }
            pos[n] = p;
         }
      }
      int[][] adjacency = new int[numNodes][];
      for (int i=0; i<numNodes; i++) {
         int beg = offs[i];
         int end = offs[i+1];
         Arrays.sort (flat, beg, end);
         int num = 0;
         for (int k=beg; k<end; k++) {
            if (k == beg || flat[k] != flat[k-1]) {
               flat[beg+num++] = flat[k];
            }
         }
         adjacency[i] = Arrays.copyOfRange (flat, beg, beg+num);
      }
      return adjacency;
   }

   /**
    * Expands the node adjacency into an upper triangular CRS pattern with
    * {@code dofs} degrees of freedom per node.
    */
   private CRSMatrix createPattern (
      int[][] adjacency, int numNodes, int dofs) {

      int size = numNodes*dofs;
      long numVals = 0;
      for (int i=0; i<numNodes; i++) {
         for (int j : adjacency[i]) {
            if (j > i) {
               numVals += dofs*dofs;
            }
            else if (j == i) {
               numVals += dofs*(dofs+1)/2;
            }
         }
      }
      if (numVals > Integer.MAX_VALUE) {
         throw new IllegalArgumentException (
            "matrix requires "+numVals+" values, which exceeds int capacity");
      }
      CRSMatrix M = new CRSMatrix();
      M.size = size;
      M.type = Matrix.SPD;
      M.rowOffs = new int[size+1];
      M.colIdxs = new int[(int)numVals];
      M.vals = new double[(int)numVals];

      int k = 0;
      for (int i=0; i<numNodes; i++) {
         int[] adj = adjacency[i];
         for (int a=0; a<dofs; a++) {
            M.rowOffs[i*dofs+a] = k+1;
            for (int j : adj) {
               if (j < i) {
                  continue;
               }
               int bbeg = (j == i ? a : 0);
               for (int b=bbeg; b<dofs; b++) {
                  M.colIdxs[k++] = j*dofs+b+1;
               }
            }
         }
      }
      M.rowOffs[size] = k+1;
      return M;
   }

   // ------------------------------------------------------------------
   // value assembly
   // ------------------------------------------------------------------

   /**
    * Returns a random SPD {@code dofs X dofs} block, used as the material
    * coupling between the degrees of freedom of a node pair.
    */
   private double[] createCouplingBlock (int dofs) {
      Random rand = new Random (0x1234);
      double[] B = new double[dofs*dofs];
      for (int i=0; i<dofs*dofs; i++) {
         B[i] = rand.nextDouble()-0.5;
      }
      double[] A = new double[dofs*dofs];
      for (int a=0; a<dofs; a++) {
         for (int b=0; b<dofs; b++) {
            double sum = 0;
            for (int c=0; c<dofs; c++) {
               sum += B[c*dofs+a]*B[c*dofs+b];
            }
            A[a*dofs+b] = sum + (a == b ? dofs : 0);
         }
      }
      return A;
   }

   private int findIdx (CRSMatrix M, int row, int col) {
      int lo = M.rowOffs[row]-1;
      int hi = M.rowOffs[row+1]-2;
      int col1 = col+1;
      while (lo <= hi) {
         int mid = (lo+hi) >>> 1;
         if (M.colIdxs[mid] < col1) {
            lo = mid+1;
         }
         else if (M.colIdxs[mid] > col1) {
            hi = mid-1;
         }
         else {
            return mid;
         }
      }
      throw new InternalErrorException (
         "entry ("+row+","+col+") not present in the pattern");
   }

   /**
    * Assembles the element matrices, plus a diagonal mass term, into the
    * previously created pattern.
    */
   private void assembleValues (
      CRSMatrix M, int[][] elems, int dofs) {

      double[] A = createCouplingBlock (dofs);
      double stiffness = 1.0;
      double mass = 1e-3;

      for (int[] elem : elems) {
         int m = elem.length;
         for (int ei=0; ei<m; ei++) {
            for (int ej=0; ej<m; ej++) {
               int ni = elem[ei];
               int nj = elem[ej];
               if (nj < ni) {
                  continue; // upper triangle only
               }
               double coeff = stiffness*((ni == nj ? 1.0 : 0.0) - 1.0/m);
               for (int a=0; a<dofs; a++) {
                  int bbeg = (ni == nj ? a : 0);
                  for (int b=bbeg; b<dofs; b++) {
                     int idx = findIdx (M, ni*dofs+a, nj*dofs+b);
                     M.vals[idx] += coeff*A[a*dofs+b];
                  }
               }
            }
         }
      }
      // add the mass term along the diagonal
      for (int i=0; i<M.size; i++) {
         M.vals[M.rowOffs[i]-1] += mass;
      }
   }

   // ------------------------------------------------------------------
   // KKT form
   // ------------------------------------------------------------------

   /**
    * Creates a bordered KKT matrix
    * <pre>
    *   [ M  G^T ]
    *   [ G   0  ]
    * </pre>
    * from a stiffness matrix {@code M}, where {@code G} is a sparse constraint
    * matrix with {@code numCons} rows, each coupling all the degrees of
    * freedom of a small number of randomly selected nodes. The result is
    * symmetric indefinite, and so has type {@link Matrix#SYMMETRIC}.
    *
    * @param M stiffness matrix, left unmodified
    * @param numCons number of constraint rows
    * @param nodesPerCon number of nodes coupled by each constraint
    * @param dofsPerNode degrees of freedom per node in {@code M}
    * @return bordered KKT matrix
    */
   public CRSMatrix addConstraints (
      CRSMatrix M, int numCons, int nodesPerCon, int dofsPerNode) {

      if (numCons <= 0) {
         return M;
      }
      int numNodes = M.size/dofsPerNode;
      if (nodesPerCon > numNodes) {
         nodesPerCon = numNodes;
      }
      Random rand = new Random (0x5678);
      // constraint rows, as sorted lists of the nodes they couple
      int[][] conNodes = new int[numCons][];
      for (int c=0; c<numCons; c++) {
         // Each constraint couples a contiguous run of nodes, starting at a
         // randomly chosen one. Contiguous node indices are spatially adjacent
         // in the grid, so the constraint is local, as it would be for
         // contact. Coupling unrelated nodes instead would produce fill far
         // beyond anything a real constraint set generates.
         int start = rand.nextInt (numNodes - nodesPerCon + 1);
         int[] nodes = new int[nodesPerCon];
         for (int i=0; i<nodesPerCon; i++) {
            nodes[i] = start+i;
         }
         conNodes[c] = nodes;
      }
      // Invert conNodes to give, for each node, the constraints which touch
      // it, stored as a flat array with per-node offsets. Scanning every
      // constraint for every row instead would be O(size*numCons), which
      // dominates everything else once numCons grows with the matrix size.
      int[] nodeConOffs = new int[numNodes+1];
      for (int c=0; c<numCons; c++) {
         for (int n : conNodes[c]) {
            nodeConOffs[n+1]++;
         }
      }
      for (int n=0; n<numNodes; n++) {
         nodeConOffs[n+1] += nodeConOffs[n];
      }
      int[] nodeCons = new int[nodeConOffs[numNodes]];
      int[] nodePos = Arrays.copyOf (nodeConOffs, numNodes);
      for (int c=0; c<numCons; c++) {
         // constraints are visited in increasing order, so each node's list
         // comes out sorted, as required for the CRS column indices
         for (int n : conNodes[c]) {
            nodeCons[nodePos[n]++] = c;
         }
      }
      int size = M.size + numCons;
      long numVals = M.numVals() + numCons;  // + zero (2,2) diagonal
      for (int n=0; n<numNodes; n++) {
         numVals += (long)(nodeConOffs[n+1]-nodeConOffs[n])*dofsPerNode;
      }
      if (numVals > Integer.MAX_VALUE) {
         throw new IllegalArgumentException (
            "matrix requires "+numVals+" values, which exceeds int capacity");
      }
      CRSMatrix K = new CRSMatrix();
      K.size = size;
      K.type = Matrix.SYMMETRIC;
      K.rowOffs = new int[size+1];
      K.colIdxs = new int[(int)numVals];
      K.vals = new double[(int)numVals];

      int k = 0;
      for (int i=0; i<M.size; i++) {
         K.rowOffs[i] = k+1;
         int end = M.rowOffs[i+1]-1;
         for (int p=M.rowOffs[i]-1; p<end; p++) {
            K.colIdxs[k] = M.colIdxs[p];
            K.vals[k] = M.vals[p];
            k++;
         }
         // G^T entries, in increasing constraint order
         int node = i/dofsPerNode;
         for (int p=nodeConOffs[node]; p<nodeConOffs[node+1]; p++) {
            K.colIdxs[k] = M.size+nodeCons[p]+1;
            K.vals[k] = rand.nextDouble()-0.5;
            k++;
         }
      }
      for (int c=0; c<numCons; c++) {
         K.rowOffs[M.size+c] = k+1;
         K.colIdxs[k] = M.size+c+1;
         K.vals[k] = 0;   // zero (2,2) block
         k++;
      }
      K.rowOffs[size] = k+1;
      return K;
   }
}
