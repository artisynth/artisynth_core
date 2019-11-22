/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.util.*;

import maspack.matrix.*;
import maspack.util.*;
import maspack.geometry.ICPRegistration.Prealign;

/**
 * Class to perform interative closest point (ICP) registration of one mesh
 * onto another.
 */
public class ICPRegistration {

   protected static double EPSILON = 1e-10;

   static public boolean myProfiling = true;
   static public double myDistanceTime;
   static public double myAdjustTime;
   static public double myPCATime;

   /**
    * Specifies whether to prealign the mesh using principal component analysis
    * (PCA), and if so, which of the resulting PCA axes to try.
    */
   public enum Prealign {
      NONE,     // do not prealign with PCA
      PCA_ALL,  // try all PCA axis combinations
      PCA_0,    // try the first PCA axis (for testing only)
      PCA_1,    // try the second PCA axis (for testing only)
      PCA_2,    // try the third PCA axis (for testing only)
      PCA_3     // try the fourth PCA axis (for testing only)
   };

   static boolean myDualDistancingEnabled = false;
   static private int MAX_FLIPS = 24;

   static protected class VertexDistInfo {
      
      public Vertex3d myVertex;
      public Point3d myPnt;
      public Vector3d myNrm;
      public double myOffset;

      VertexDistInfo () {
         myVertex = null;
         myPnt = new Point3d();
         myNrm = new Vector3d();
      }

      VertexDistInfo (Vertex3d vtx) {
         myVertex = vtx;
         myPnt = new Point3d();
         myNrm = new Vector3d();
      }
   }

   public boolean isDualDistancingEnabled () {
      return myDualDistancingEnabled;
   }

   public void setDualDistancingEnabled (boolean enable) {
      myDualDistancingEnabled = enable;
   }

   protected static int myMaxVertices = 500;
   protected int myMaxIters = 100;
   protected VertexDistInfo[] myDistInfo = null;
   protected int myNumMesh2Dists = 0;
   protected int myNumMesh1Dists = 0;

   private boolean[] createRandomIndices (int num, int max) {
      // assumes that num is less that half the size of max
      boolean[] marked = new boolean[max];
      int cnt = 0;
      Random randGen = RandomGenerator.get();
      while (cnt < num) {
         int idx = randGen.nextInt (max-1);
         if (!marked[idx]) {
            marked[idx] = true;
            cnt++;
         }
      }
      return marked;
   }      

   private double computeMesh2Distances (
      AffineTransform3d X, PolygonalMesh mesh1, double rad1) {
      
      BVFeatureQuery query = new BVFeatureQuery();
      Vector2d coords = new Vector2d();
      Point3d nearest = new Point3d();
      double distSum = 0;
      for (int i=0; i<myNumMesh2Dists; i++) {
         VertexDistInfo info = myDistInfo[i];
         
         info.myPnt.transform (X, info.myVertex.pnt);
         Face face = query.nearestFaceToPoint (
            nearest, coords, mesh1, info.myPnt);
//         Face face = obbtree.nearestFace (
//            info.myPnt, null, nearest, coords, intersector);
         
         double d = info.myPnt.distance (nearest);
         if (d < EPSILON*rad1) {
            System.out.println ("setting face normal");
            info.myNrm.set (face.getNormal());
         }
         else {
            info.myNrm.sub (info.myPnt, nearest);
            info.myNrm.scale (1/d);
         }
         info.myOffset = info.myPnt.dot(info.myNrm) - nearest.dot(info.myNrm);
         distSum += d;
      }
      return distSum;
   }

   private double computeMesh1Distances (
      AffineTransform3d X, AffineTransform3d Xinv,
      PolygonalMesh mesh2, double rad2) {
      
      BVFeatureQuery query = new BVFeatureQuery();
      Vector2d coords = new Vector2d();
      Point3d nearest = new Point3d();
      Point3d pnt = new Point3d();
      Vector3d nrm = new Vector3d();
      double distSum = 0;
      for (int i=0; i<myNumMesh1Dists; i++) {
         VertexDistInfo info = myDistInfo[myNumMesh2Dists+i];
         
         pnt.transform (Xinv, info.myVertex.pnt);
         Face face = query.nearestFaceToPoint (nearest, coords, mesh2, pnt);
//         Face face = obbtree.nearestFace (
//            pnt, null, nearest, coords, intersector);
         
         double d = pnt.distance (nearest);
         if (d < EPSILON*rad2) {
            System.out.println ("setting face normal");
            nrm.set (face.getNormal());
         }
         else {
            nrm.sub (pnt, nearest);
            nrm.scale (1/d);
         }
         nrm.transform (X);
         double nlen = nrm.norm();
         info.myNrm.scale (-1/nlen, nrm);
         d *= nlen;
         info.myPnt.transform (X, nearest);
         info.myOffset =
            info.myPnt.dot(info.myNrm) - info.myVertex.pnt.dot(info.myNrm);
         distSum += d;
      }
      return distSum;
   }

   /**
    * Computes an adjustment <code>dX</code> to the current AffineTransform3d
    * <code>X</code> such that <code>dX</code> moves all the points in such a
    * way as to minimize their distance to the surface. Specifically, we want
    * to move each point by a displacement <code>dp_i</code> such that the
    * projection of this displacement onto the normal <code>n_i</code> arising
    * from the nearest surface point equals the negative of the offset
    * <code>o_i</code> from the surface:
    * <pre>
    * n_i^T dp_i = -o_i
    * </pre>
    * Now, <code>dp_i</code> is determined by <code>dX p_i</code>,
    * and <code>dX</code> is itself described by <code>n</code> independent
    * coordinates <code>y</code>, where <code>n</code> is either 3, 6, 7, or 12:
    * <dl>
    *    <dt>3</dt>
    *    <dd>corresponds to translation only, with <code>y0:y2</code>
    *    describing the translation parameters;</dd>
    *    <dt>6</dt>
    *    <dd>corresponds to rotation and translation, with <code>y3:y5</code>
    *    describing the incremental yaw, pitch and roll angles;</dd>
    *    <dt>7</dt>
    *    <dd>corresponds to rotation, translation and scaling, with
    *    <code>y6</code> 
    *    describing the incremental scaling;</dd>
    *    <dt>12</dt>
    *    <dd>corresponds to a full affine transform, with <code>y0:y2</code>
    *    describing the translation parameters and <code>y3:y11</code>
    *    the coefficients of the 3 x 3 affine matrix.</dd>
    * </dl>
    * For cases 3, 6, and 7, <code>dX</code> takes the general form
    * <pre> 
    *      [  y6  -y5   y4   y0  ]
    * dX = [  y5   y6  -y3   y1  ]
    *      [ -y4   y3   y6   y2  ]
    *      [  0    0    0    1   ]
    * </pre>
    * where <code>yi = 0</code> whenever {@code i >= n}.
    *
    *<p> 
    * For each of the above cases, the relation
    * <code>n_i^T dp_i = -o_i</code> can be expressed as
    * <pre>
    * a_i^T y = -o_i
    * </pre>
    * where <code>a_i</code> is a n-vector formed from <code>n_i</code>
    * and <code>p_i</code>.
    *
    *<p> 
    * Assembling these into a single matrix equation for all
    * points, we end up with the linear
    * least squares problem
    * <pre>
    * A y = -o
    * </pre>
    * where <code>A</code> is formed from individual rows <code>a_i^T</code>
    * and <code>o</code> is formed from the aggregate of <code>o_i</code>.
    * This least squares problem can be solved directly using the normal
    * equations approach:
    * <pre>
    * A^T A y = -A^T o
    * </pre>
    * We use this instead of QR decomposition for reasons of speed.
    * 
    * @param dX returns the computed adjustment transform
    * @param ndists number of points being adjusted
    * @param n number of independent coordinates in dX. 3 corrresponds
    * to translation only, 6 to a rigid transform, 7 to a rigid
    * transform plus scaling, and 9 to a general affine transform.
    */
   public void computeAdjustment (
      AffineTransform3d dX, int ndists, int n) {

      MatrixNd M = new MatrixNd(n,n); // forms A^T A
      VectorNd b = new VectorNd(n);   // forms -A^T o
      VectorNd y = new VectorNd(n);   // computes parameters for dX
      double[] a = new double[n];     // row a_i of A
      CholeskyDecomposition chol = new CholeskyDecomposition();

      Vector3d pxn = new Vector3d();
      for (int k=0; k<ndists; k++) {
         VertexDistInfo info = myDistInfo[k];
         Vector3d nrm = info.myNrm;
         Point3d pnt = info.myPnt;
         double off = info.myOffset;

         pxn.cross (pnt, info.myNrm);
         a[0] = nrm.x;
         a[1] = nrm.y;
         a[2] = nrm.z;
         if (n == 12) {
            // full affine transform
            a[3] = nrm.x*pnt.x;
            a[4] = nrm.x*pnt.y;
            a[5] = nrm.x*pnt.z;
            a[6] = nrm.y*pnt.x;
            a[7] = nrm.y*pnt.y;
            a[8] = nrm.y*pnt.z;
            a[9] = nrm.z*pnt.x;
            a[10] = nrm.z*pnt.y;
            a[11] = nrm.z*pnt.z;
         }
         else {
            pxn.cross (pnt, info.myNrm);
            if (n > 3) {
               a[3] = pxn.x;
               a[4] = pxn.y;
               a[5] = pxn.z;
            }
            if (n > 6) {
               a[6] = pnt.dot(nrm);
            }
         }
         for (int i=0; i<n; i++) {
            b.add (i, -a[i]*off);
            for (int j=0; j<n; j++) {
               M.add (i, j, a[i]*a[j]);
            }
         }
      }
      chol.factor (M);
      chol.solve (y, b);
      RotationMatrix3d R = new RotationMatrix3d();
      //System.out.println ("y=" + y);
      dX.p.set (y.get(0), y.get(1), y.get(2));
      if (n == 12) {
         dX.A.set (
            y.get(3)+1, y.get(4), y.get(5),
            y.get(6), y.get(7)+1, y.get(8),
            y.get(9), y.get(10), y.get(11)+1);
      }
      else {
         if (n > 3) {
            R.setRpy (y.get(5), y.get(4), y.get(3));
            dX.A.set (R);
         }
         if (n > 6) {
            dX.A.scale (1+y.get(6));
         }
      }
   }

   private int assignDistVertices (PolygonalMesh mesh, int idx) {
      int numv = mesh.numVertices();
      int maxv = myDualDistancingEnabled ? myMaxVertices/2 : myMaxVertices;

      int numd;

      if (numv <= maxv) {
         // just assign one distance per vertex
         numd = numv;
         for (int i=0; i<numd; i++) {
            myDistInfo[idx++].myVertex = mesh.getVertices().get(i);
         }
      }
      else {
         numd = maxv;
         // too many vertices; need to subsample
         if (maxv >= numv-maxv) {
            // faster to mark vertices for exclusion
            boolean[] marked = createRandomIndices (numv-maxv, numv);
            for (int i=0; i<marked.length; i++) {
               if (!marked[i]) {
                  myDistInfo[idx++].myVertex = mesh.getVertices().get(i);
               }
            }
         }
         else {
            // faster to mark vertices for inclusion
            boolean[] marked = createRandomIndices (maxv, numv);
            for (int i=0; i<marked.length; i++) {
               if (marked[i]) {
                  myDistInfo[idx++].myVertex = mesh.getVertices().get(i);
               }
            }
         }
      }      
      return numd;
   }

   protected void allocateDistInfo (PolygonalMesh mesh1, PolygonalMesh mesh2) {
      if (myDistInfo == null) {
         myDistInfo = new VertexDistInfo[myMaxVertices];
         for (int i=0; i<myMaxVertices; i++) {
            myDistInfo[i] = new VertexDistInfo();
         }
      }
      myNumMesh2Dists = assignDistVertices (mesh2, 0);
      if (myDualDistancingEnabled) {
         myNumMesh1Dists = assignDistVertices (mesh1, myNumMesh2Dists);
      }
   }

   /**
    * Based on "Non-rigid Transformations for Musculoskeletal Model", by Petr
    * Kellnhofer.
    */
   public static double computePCA (
      Matrix3d J, Vector3d c, MeshBase mesh) {

      Vector3d d = new Vector3d();
      double maxdSqr = 0;

      mesh.computeCentroid (c);

      J.setZero();
      int numv = mesh.numVertices();
      for (int i=0; i<numv; i++) {
         Vertex3d vtx = mesh.getVertices().get(i);
         d.sub (vtx.pnt, c);
         J.addOuterProduct (d, d);
         double dsqr = d.normSquared();
         if (dsqr > maxdSqr) {
            maxdSqr = dsqr;
         }
      }
      J.scale (1/(double)numv);
      return Math.sqrt (maxdSqr);

   }

   public void registerPCA (
      AffineTransform3d X, PolygonalMesh mesh1, PolygonalMesh mesh2) {
      
      double[] dlist = new double[4];
      AffineTransform3d[] Xlist = new AffineTransform3d[4];
      IntHolder nposes = new IntHolder();

      allocateDistInfo (mesh1, mesh2);
      doRegisterPCA (Xlist, dlist, mesh1, mesh2, nposes);
      X.set (Xlist[0]);
   }


   protected double computeMeshRadius (PolygonalMesh mesh) {

      Vector3d c = new Vector3d();
      Vector3d d = new Vector3d();
      double maxdSqr = 0;

      mesh.computeCentroid (c);

      int numv = mesh.numVertices();
      for (int i=0; i<numv; i++) {
         Vertex3d vtx = mesh.getVertices().get(i);
         d.sub (vtx.pnt, c);
         double dsqr = d.normSquared();
         if (dsqr > maxdSqr) {
            maxdSqr = dsqr;
         }
      }
      return Math.sqrt (maxdSqr);
   }

   /**
    * Compute the maximum extent of a mesh along its three PCA axes.
    */
   static void computePCASize (
      RotationMatrix3d R1W, Vector3d c, Vector3d w, MeshBase mesh) {

      Point3d pnt = new Point3d();
      double huge = Double.MAX_VALUE;
      Vector3d max = new Vector3d (-huge, -huge, -huge);
      Vector3d min = new Vector3d ( huge,  huge,  huge);

      for (int i=0; i<mesh.numVertices(); i++) {
         pnt.inverseTransform (R1W, mesh.getVertices().get(i).pnt);
         pnt.updateBounds (min, max);
      }
      c.add (max, min);
      c.scale (0.5);
      c.transform (R1W);
      w.sub (max, min);
   }

   static double sigTol = 1.5;

   public void addAxisFlips (
      LinkedList<RotationMatrix3d> flips,
      RotationMatrix3d R, Vector3d axis, int n) {

      RotationMatrix3d R0 = new RotationMatrix3d(R);
      flips.add (R0);
      double ang = 0;
      for (int i=1; i<n; i++) {
         ang += 2*Math.PI/n;
         RotationMatrix3d RX = new RotationMatrix3d(R);
         RX.mulAxisAngle (axis, ang);
         flips.add (RX);
      }
   }

   public RotationMatrix3d[] computeFlips (Vector3d sig) {
      LinkedList<RotationMatrix3d> flips = new LinkedList<RotationMatrix3d>();
      if (sig.x/sig.z < sigTol) {
         RotationMatrix3d R = new RotationMatrix3d();
         // try all 24 positions
         int ndivs = 4;
         addAxisFlips (flips, R, Vector3d.X_UNIT, ndivs);
         R.setAxisAngle (0, 1, 0, Math.PI/2);
         addAxisFlips (flips, R, Vector3d.X_UNIT, ndivs);
         R.setAxisAngle (0, 1, 0, Math.PI);
         addAxisFlips (flips, R, Vector3d.X_UNIT, ndivs);
         R.setAxisAngle (0, 1, 0, -Math.PI/2);
         addAxisFlips (flips, R, Vector3d.X_UNIT, ndivs);
         R.setAxisAngle (0, 0, 1, Math.PI/2);
         addAxisFlips (flips, R, Vector3d.X_UNIT, ndivs);
         R.setAxisAngle (0, 0, 1, -Math.PI/2);
         addAxisFlips (flips, R, Vector3d.X_UNIT, ndivs);
      }
      else if (sig.x/sig.y < sigTol) {
         RotationMatrix3d R = new RotationMatrix3d();
         addAxisFlips (flips, R, Vector3d.Z_UNIT, 4);
         R.setAxisAngle (0, 1, 0, Math.PI);
         addAxisFlips (flips, R, Vector3d.Z_UNIT, 4);
      }
      else if (sig.y/sig.z < sigTol) {
         RotationMatrix3d R = new RotationMatrix3d();
         addAxisFlips (flips, R, Vector3d.X_UNIT, 4);
         R.setAxisAngle (0, 1, 0, Math.PI);
         addAxisFlips (flips, R, Vector3d.X_UNIT, 4);
      }
      else {
         flips.add (RotationMatrix3d.IDENTITY);
         flips.add (new RotationMatrix3d (1, 0, 0, Math.PI));
         flips.add (new RotationMatrix3d (0, 1, 0, Math.PI));
         flips.add (new RotationMatrix3d (0, 0, 1, Math.PI));
      }
      return flips.toArray(new RotationMatrix3d[0]);
   }

   /**
    * Use PCA to determine the OBB for a mesh.
    */ 
   public static void computeOBBFromPCA (OBB obb, MeshBase mesh) {

      Matrix3d J = new Matrix3d();
      Vector3d c = new Vector3d();
      Vector3d w = new Vector3d();
      // XOW transforms from the OBB of th mesh to world
      RigidTransform3d XOW = new RigidTransform3d();

      SVDecomposition3d svd = new SVDecomposition3d();

      computePCA (J, c, mesh);

      // because J is SPD, the svd yields J = R S R^T, where S is diagonal
      // and R gives the orientation of the principal axes with respect to
      // world coordinates.
      svd.factor (J);
      XOW.R.set (svd.getU());

      computePCASize (XOW.R, c, w, mesh);
      XOW.p.set (c);
      if (XOW.R.determinant() < 0) {
         // flip x axis to correct
         XOW.R.m00 = -XOW.R.m00;
         XOW.R.m10 = -XOW.R.m10;
         XOW.R.m20 = -XOW.R.m20;
      }
      obb.setTransform (XOW);
      obb.setWidths (w);
   }

   double doRegisterPCA (
      AffineTransform3d[] Xlist, double[] dlist,
      PolygonalMesh mesh1, PolygonalMesh mesh2, IntHolder nposes) {

      Matrix3d J1 = new Matrix3d();
      Vector3d c1 = new Vector3d();
      Vector3d w1 = new Vector3d();
      // X1W transforms from principal coords of body 1 to world
      RigidTransform3d X1W = new RigidTransform3d();
      double rad1  = 0;

      SVDecomposition3d svd = new SVDecomposition3d();

      rad1 = computePCA (J1, c1, mesh1);

      // because J1 is SPD, the svd yields J1 = R S R^T, where S is diagonal
      // and R gives the orientation of the principal axes with respect to
      // world coordinates.
      svd.factor (J1);
      X1W.R.set (svd.getU());
      computePCASize (X1W.R, c1, w1, mesh1);
      rad1 = w1.maxElement();
      X1W.p.set (c1);
      if (X1W.R.determinant() < 0) {
         // flip x axis to correct
         X1W.R.m00 = -X1W.R.m00;
         X1W.R.m10 = -X1W.R.m10;
         X1W.R.m20 = -X1W.R.m20;
      }
      
      Vector3d sig = new Vector3d();
      svd.getS (sig);

      Matrix3d J2 = new Matrix3d();
      Vector3d c2 = new Vector3d();
      Vector3d w2 = new Vector3d();
      // X1W transforms from principal coords of body 2 to world
      RigidTransform3d X2W = new RigidTransform3d();
      double rad2 = 0;

      rad2 = computePCA (J2, c2, mesh2);

      svd.factor (J2);
      X2W.R.set (svd.getU());
      computePCASize (X2W.R, c2, w2, mesh2);
      rad2 = w2.maxElement();
      X2W.p.set (c2);
      if (X2W.R.determinant() < 0) {
         // flip x axis to correct
         X2W.R.m00 = -X2W.R.m00;
         X2W.R.m10 = -X2W.R.m10;
         X2W.R.m20 = -X2W.R.m20;
      }

      System.out.println ("sig=" + sig);
      RotationMatrix3d[] flips = computeFlips (sig);

      RigidTransform3d X21 = new RigidTransform3d();
      RigidTransform3d XRF = new RigidTransform3d();
      RotationMatrix3d[] Rots = new RotationMatrix3d[flips.length];

      for (int i=0; i<flips.length; i++) {
         // X21 = X1W * inv(X2W), so if we modify X2W to X2W' = X2W * XRF,
         // then
         // X21 = X1W * inv(XRF) * inv(X2W)
         XRF.R.set (flips[i]);
         X21.mulInverseRight (X1W, XRF);
         X21.mulInverseRight (X21, X2W);
         Rots[i] = new RotationMatrix3d (X21.R);
      }

      double minDist = Double.MAX_VALUE;
      int minFlip = -1;

      for (int i=0; i<flips.length; i++) {
         Xlist[i] = new AffineTransform3d();
         computeTransform (Xlist[i], Rots[i], c1, c2, rad1/rad2);
         if (i == 0) {
            System.out.println (
               "scale="+(rad1/rad2)+" det=" + Xlist[i].A.determinant());
         }
         
         dlist[i] = computeMesh2Distances (Xlist[i], mesh1, rad1);
         dlist[i] /= myNumMesh2Dists;
      }
      // // sort by smallest distance
      // for (int i=0; i<4; i++) {
      //    for (int j=i+1; j<4; j++) {
      //       if (dlist[i] > dlist[j]) {
      //          double dtmp = dlist[i];
      //          dlist[i] = dlist[j];
      //          dlist[j] = dtmp;
      //          AffineTransform3d Xtmp = Xlist[i];
      //          Xlist[i] = Xlist[j];
      //          Xlist[j] = Xtmp;
      //       }
      //    }
      // }
      // for (int i=0; i<flips.length; i++) {
      //    System.out.println ("d=" + dlist[i]);
      // }
      System.out.println ("num poses=" + flips.length);
      //computeTransform (X, X21.R, flips[minFlip], c1, c2, rad1/rad2);
      nposes.value = flips.length;
      return rad1;
   }

   private void computeTransform (
      AffineTransform3d X, RotationMatrix3d R,
      Vector3d c1, Vector3d c2, double scale) {

      X.A.set (R);
      X.p.transform (R, c2);
      X.p.scale (-scale);
      X.p.add (c1);
      X.A.scale (scale);
   }

   public double computeInertia (
      Matrix3d J, Vector3d c, PolygonalMesh mesh) {

      Vector3d mov1 = new Vector3d();
      Vector3d mov2 = new Vector3d();
      Vector3d pov = new Vector3d();

      double vol = mesh.computeVolumeIntegrals (mov1, mov2, pov);

      c.scale (1.0 / vol, mov1); // center of volume

      J.m00 = mov2.y + mov2.z;
      J.m11 = mov2.x + mov2.z;
      J.m22 = mov2.x + mov2.y;
      J.m01 = -pov.z;
      J.m02 = -pov.y;
      J.m12 = -pov.x;
      J.m10 = J.m01;
      J.m20 = J.m02;
      J.m21 = J.m12;

      J.m00 -= vol*(c.y*c.y + c.z*c.z);
      J.m11 -= vol*(c.x*c.x + c.z*c.z);
      J.m22 -= vol*(c.x*c.x + c.y*c.y);
      J.m01 += vol*(c.x*c.y);
      J.m02 += vol*(c.x*c.z);
      J.m12 += vol*(c.y*c.z);
      J.m10 = J.m01;
      J.m20 = J.m02;
      J.m21 = J.m12;

      return Math.pow (vol, 1/3.0);
   }

   public void registerInertia (
      AffineTransform3d X, PolygonalMesh mesh1, PolygonalMesh mesh2) {

      Matrix3d J1 = new Matrix3d();
      Vector3d c1 = new Vector3d();
      // R1D transforms from diagonal space to world space
      RigidTransform3d X1W = new RigidTransform3d();
      double rad1 = 0;

      Vector3d sig = new Vector3d();

      SVDecomposition3d svd = new SVDecomposition3d();

      rad1 = computeInertia (J1, c1, mesh1);
      System.out.println ("rad1=" + rad1);

      svd.factor (J1);
      X1W.R.set (svd.getU());
      X1W.p.set (c1);
      svd.getS (sig);
      System.out.println ("svd1=" + sig);
      System.out.println ("R1=\n" + X1W.R.toString ("%9.5f"));

      Matrix3d J2 = new Matrix3d();
      Vector3d c2 = new Vector3d();
      // X2W transforms from diagonal space to world space
      RigidTransform3d X2W = new RigidTransform3d();
      double rad2 = 0;

      rad2 = computeInertia (J2, c2, mesh2);

      svd.factor (J2);
      X2W.R.set (svd.getU());
      X2W.p.set (c2);

      svd.getS (sig);
      System.out.println ("svd2=" + sig);
      System.out.println ("R2=\n" + X2W.R.toString ("%9.5f"));

      RigidTransform3d X21 = new RigidTransform3d();
      X21.mulInverseRight (X1W, X2W);

      X.A.set (X21.R);
      X.p.set (X21.p);
      //X.A.scale (rad1/rad2);
   }

   private double doRegisterICP (
      AffineTransform3d X,
      PolygonalMesh mesh1, double rad1,
      PolygonalMesh mesh2, double rad2, int npar) {

      AffineTransform3d dX = new AffineTransform3d();
      AffineTransform3d Xlast = new AffineTransform3d();
      AffineTransform3d Xinv = new AffineTransform3d();

      double lastDist = 0;
      double firstDist = 0;
      double dist = 0;
      Xlast.set (X);
      Xinv.invert (X);

      double t0 = 0;
      double t1 = 0;

      for (int i=0; i<myMaxIters; i++) {

         if (myProfiling) {
            t0 = 1e-9*System.nanoTime();
         }

         int ndists = myNumMesh2Dists;         
         dist = computeMesh2Distances (X, mesh1, rad1);
         if (myDualDistancingEnabled) {
            dist += computeMesh1Distances (X, Xinv, mesh2, rad2);
            ndists += myNumMesh1Dists;
         }
         dist /= ndists;

         //System.out.println ("    iter " + i+ " dist=" + dist);
         if (i > 0) {
            if (dist > lastDist) {
               X.set (Xlast);
               dist = lastDist;
               //System.out.println ("    Distance diverging, halting");
               break;
            }
            else if (dist < 1e-3*firstDist || dist < 1e-8*rad1) {
               //System.out.println ("    Distance below limit, halting");
               break;
            }
         }
         else {
            firstDist = dist;
         }

         if (myProfiling) {
            t1 = 1e-9*System.nanoTime();
            myDistanceTime += (t1-t0);
            t0 = t1;
         }

         computeAdjustment (dX, ndists, npar);
         //System.out.println ("dX=\n" + dX);

         if (myProfiling) {
            t1 = 1e-9*System.nanoTime();
            myAdjustTime += (t1-t0);
         }
         
         Xlast.set (X);
         X.mul (dX, X);
         Xinv.invert (X);
         lastDist = dist;
      }
      return dist;
   }

   /**
    * Computes an affine transform that tries to register mesh2 onto mesh1,
    * using an interative closest point (ICP) approach. This method has the
    * same functionality as {@link
    * #registerICP(AffineTransform3d,PolygonalMesh,PolygonalMesh,Prealign,int[])},
    * only with {@code align} set to {@link Prealign#PCA_ALL} and {@code npar}
    * specified as a variable argument list.
    * 
    * @param X returns the resulting transform
    * @param mesh1 target mesh
    * @param mesh2 mesh to register
    * @param npar DOFs to use in the final registration, as well as
    * any preliminary registrations
    */
   public void registerICP (
      AffineTransform3d X, PolygonalMesh mesh1,
      PolygonalMesh mesh2, int... npar) {

      registerICP (X, mesh1, mesh2, Prealign.PCA_ALL, npar);
   }

   /**
    * Computes an affine transform that tries to register {@code mesh2} onto
    * {@code mesh1}, using an interative closest point (ICP) approach.  The
    * resulting transform is returned in {@code X}. The {@code npar} argument
    * can be used to constrain the transform to rigid, rigid + scaling, or full
    * affine (rigid + scaling + shearing), as described below.
    *
    * <p>The {@code align} argument can be used to specify whether or not to
    * try prealigning the orientation using principal component analysis:
    * {@link Prealign#NONE} specifies no prealignment, while {@link
    * Prealign#PCA_ALL} specifies trying all possible PCA axis combinations for
    * the best fit.
    *
    * <p>The argument {@code npar} contains integers specifying the number of
    * DOFs to be used in both the final registration transform, as well as any
    * preliminary registrations that should be applied first. The last integer
    * specifies the DOFs in the final transformation, while any previous
    * integers specify the preliminary registrations (in order). The integers
    * are restricted to the following values:
    * 
    * <ul>
    * <li> 3: translation only
    * <li> 6: rigid transform
    * <li> 7: rigid transform + scaling
    * <li> 12: full affine transform (rigid + scaling + shearing)
    * </ul>
    *
    * Preliminary registrations (usually with lower DOFs) can sometimes help
    * improve the final result. For example, an {@code npar} containing the
    * sequence
    * 
    * <pre>
    * 3, 7
    * </pre>
    * 
    * would instruct the method to first perform a translational registration,
    * and then finish with a final registration consisting of a rigid transform
    * plus scaling. Likewise, an {@code npar} containing the sequence
    * 
    * <pre>
    * 6, 12
    * </pre>
    * 
    * would instruct the method to first perform a rigid registration, and then
    * finish with a final full affine registration. Repeating numbers in
    * {@code npar} simply requests that the same registration be performed
    * again.
    * 
    * @param X returns the resulting transform
    * @param mesh1 target mesh
    * @param mesh2 mesh to register
    * @param align specifies whether or not to use PCA for prealignment
    * @param npar DOFs to use in the final registration, as well as
    * any preliminary registrations
    */
   public void registerICP (
      AffineTransform3d X, PolygonalMesh mesh1, PolygonalMesh mesh2,
      Prealign align, int[] npar) {

      for (int i=0; i<npar.length; i++) {
         if (npar[i] != 0 && npar[i] != 3 && npar[i] != 6 &&
             npar[i] != 7 && npar[i] != 12) {
            throw new IllegalArgumentException (
               "npar must be 0, 3, 6, 7, or 12");
         }
      }

      AffineTransform3d[] Xlist;
      double[] dlist;
      int dlistSize;

      double t0 = 0;
      double t1 = 0;

      if (myProfiling) {
         t0 = 1e-9*System.nanoTime();
      }

      allocateDistInfo (mesh1, mesh2);
      double rad1, rad2;
      int fixedChoice = -1;
      
      if (align == Prealign.NONE) {
         Xlist = new AffineTransform3d[1];
         Xlist[0] = new AffineTransform3d();
         Xlist[0].setIdentity();
         dlist = new double[1];
         dlistSize = 1;
         rad1 = computeMeshRadius (mesh1);
         rad2 = computeMeshRadius (mesh2);
         fixedChoice = 0;
      }
      else {
         double[] dPCA = new double[MAX_FLIPS];
         AffineTransform3d[] XPCA = new AffineTransform3d[MAX_FLIPS];      
         IntHolder nposes = new IntHolder();

         rad1 = doRegisterPCA (XPCA, dPCA, mesh1, mesh2, nposes);
         rad2 = computeMeshRadius (mesh2);
         switch (align) {
            case PCA_0: fixedChoice = 0; break;
            case PCA_1: fixedChoice = 1; break;
            case PCA_2: fixedChoice = 2; break;
            case PCA_3: fixedChoice = 3; break;
         }
         dlistSize = nposes.value;

         if (fixedChoice != -1) {
            Xlist = new AffineTransform3d[1];
            Xlist[0] = XPCA[fixedChoice];
            dlist = new double[1];
            dlist[0] = dPCA[fixedChoice];               
            dlistSize = 1;
         }
         else {
            double dmin = Double.MAX_VALUE;
            for (int i=0; i<dlistSize; i++) {
               if (dPCA[i] < dmin) {
                  dmin = dPCA[i];
               }
            }
            int qualifyCnt = 0;
            for (int i=0; i<dlistSize; i++) {
               if (dPCA[i] <= 2*dmin) {
                  qualifyCnt++;
               }
            }
            Xlist = new AffineTransform3d[qualifyCnt];
            dlist = new double[qualifyCnt];
            int k = 0;
            for (int i=0; i<dlistSize; i++) {
               if (dPCA[i] <= 2*dmin) {
                  Xlist[k] = XPCA[i];
                  dlist[k] = dPCA[i];
                  k++;
               }
            }            
         }
      }

      if (myProfiling) {
         t1 = 1e-9*System.nanoTime();
         myPCATime += (t1-t0);
      }

      double dmin = Double.MAX_VALUE;
      for (int k=0; k<Xlist.length; k++) {
         double d = dlist[k];
         System.out.println ("Initial d=" + d);
         for (int l=0; l<npar.length; l++) {
            if (npar[l] != 0) {
               //System.out.println (" npar=" + npar[l] + ":");
               d = doRegisterICP (Xlist[k], mesh1, rad1, mesh2, rad2, npar[l]);
            }
         }
         if (Xlist[k].getMatrix().determinant() < 0) {
            System.out.println (
               "Warning: negative determinant for "+k+" on exit");
         }
         if (d < dmin) {
            dmin = d;
            X.set (Xlist[k]);
         }
      }
   }
}
