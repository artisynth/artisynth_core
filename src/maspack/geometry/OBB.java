/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.util.ArrayList;

import maspack.matrix.Matrix3d;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.SymmetricMatrix3d;
import maspack.matrix.Vector3d;
import maspack.render.Renderer;
import maspack.render.Renderer.DrawMode;
import maspack.render.Renderer.Shading;
import maspack.render.RenderList;
import maspack.util.InternalErrorException;
import quickhull3d.QuickHull3D;

public class OBB extends BVNode {

   private final static double EPS = 1e-13;
   private final static double INF = Double.POSITIVE_INFINITY;

   public static double defaultTolerance = 0;
   RigidTransform3d myX;
   Vector3d myHalfWidths;

   /**
    * Method used to generate an OBB from a set of boundables
    */
   public enum Method {
      /**
       * Use the convex hull of the boundables to generate the OBB
       */
      ConvexHull,

      /**
       * Use the covariance of the boundables to generate the OBB
       */
      Covariance,

      /**
       * Use the points of the boundables to generate the OBB
       */
      Points
   };

   public OBB() {
      myX = new RigidTransform3d();
      myHalfWidths = new Vector3d();
      this.myNext = null;
   }

   public OBB (double wx, double wy, double wz, RigidTransform3d X) {
      this();
      setWidths (wz, wy, wz);
      setTransform (X);
   }

   public OBB (double wx, double wy, double wz) {
      this();
      setWidths (wz, wy, wz);
      setTransform (RigidTransform3d.IDENTITY);
   }

   public OBB (Vector3d widths, RigidTransform3d X) {
      this();
      setWidths (widths);
      setTransform (X);
   }

   public OBB (MeshBase mesh) {
      this();
      set (mesh, 0, Method.ConvexHull);
   }

   // public OBBNode (OBB obb, OBBNode parent) {
   //    this();
   //    this.obb = obb;
   //    setParent (parent);
   // }

   // public OBB getObb() {
   //    return obb;
   // }

   // public void setObb (OBB obb) {
   //    this.obb = obb;
   // }

   public double getRadius() {
      return myHalfWidths.norm();
   }

   public void getCenter (Vector3d center) {
      center.set (myX.p);
   }

   /**
    * Sets the widths for this OBB. These are the extents of the OBB along each
    * of its principal axes.
    *
    * @param wx new width value for the 'x' principal axis
    * @param wy new width value for the 'y' principal axis
    * @param wz new width value for the 'z' principal axis
    */
   public void setWidths (double wx, double wy, double wz) {
      myHalfWidths.x = wx / 2;
      myHalfWidths.y = wy / 2;
      myHalfWidths.z = wz / 2;
   }
   
   public void setHalfWidths(Vector3d hw) {
      myHalfWidths.set(hw);
   }

   /**
    * Sets the widths for this OBB. These are the extents of the OBB along each
    * of its principal axes.
    *
    * @param widths specifies new width values for this OBB
    */
   public void setWidths (Vector3d widths) {
      setWidths (widths.x, widths.y, widths.z);
   }

   /**
    * Gets the widths for this OBB. These are the extents of the OBB along each
    * of its principal axes.
    *
    * @param widths returns the width values for this OBB
    */
   public void getWidths (Vector3d widths) {
      widths.x = 2 * myHalfWidths.x;
      widths.y = 2 * myHalfWidths.y;
      widths.z = 2 * myHalfWidths.z;
   }
   
   /**
    * Sets the half-widths for this OBB. These are 1/2 the extents of the
    * OBB along each of its principal axes.
    *
    * @param halfWidths returns the half-width values for this OBB
    */
   public void getHalfWidths (Vector3d halfWidths) {
      halfWidths.set(myHalfWidths);
   }

   /**
    * Gets the half-widths for this OBB. These are 1/2 the extents of the
    * OBB along each of its principal axes.
    *
    * @return half-width values for this OBB (should not be modified).
    */
   public Vector3d getHalfWidths () {
      return myHalfWidths;
   }

   /**
    * Sets the transform associated with this OBB. This is the
    * transform that maps from the coordinate from of the OBB
    * into world coordinates.
    *
    * @param XObbToRef new transform value
    */
   public void setTransform (RigidTransform3d XObbToRef) {
      myX.set (XObbToRef);
   }

   /**
    * Gets the transform associated with this OBB. This is the
    * transform that maps from the coordinate from of the OBB
    * into world coordinates.
    *
    * @param XObbToRef returns the transform value
    */
   public void getTransform (RigidTransform3d XObbToRef) {
      XObbToRef.set (myX);
   }

   /**
    * Returns the transform associated with this OBB. The returned
    * result should not be modified.
    *
    * @return transform associated with this OBB.
    */
   public RigidTransform3d getTransform () {
      return myX;
   }

   /**
    * Sets this OBB to contain the points for a mesh. It does this by computing
    * the OBB using the faces of the convex hull of the mesh points.
    *
    * @param mesh mesh to fit the OBB to
    * @param margin extra distance by with the OBB should be grown. If
    * less than 0, a default margin is computed.
    * @param method method used to compute the OBB
    */
   public void set (MeshBase mesh, double margin, Method method) {
      if (margin < 0) {
         margin = 1e-8*mesh.getRadius();
      }
      ArrayList<Vertex3d> verts = mesh.getVertices();
      Point3d[] pnts = new Point3d[verts.size()];
      for (int i=0; i<verts.size(); i++) {
         pnts[i] = verts.get(i).pnt;
      }
      set (pnts, margin, method);
   }

   private double[] unpackPointsIntoArray (Point3d[] pnts) {
      double[] parray = new double[3*pnts.length];
      for (int i=0; i<pnts.length; i++) {
         Point3d pnt = pnts[i];
         parray[3*i+0] = pnt.x;
         parray[3*i+1] = pnt.y;
         parray[3*i+2] = pnt.z;
      }
      return parray;
   }      

   public void set (Point3d[] pnts, double margin, Method method) {

      Matrix3d C = new Matrix3d();
      Point3d cent = new Point3d();
      Point3d max = new Point3d (-INF, -INF, -INF);
      Point3d min = new Point3d (INF, INF, INF);
 
      switch (method) {
         case ConvexHull: {
            quickhull3d.Point3d[] hullPnts =
               computeConvexHullAndCovariance (
                  C, cent, unpackPointsIntoArray (pnts), pnts.length);
            setTransform (C, cent);
            computeBoundsFromConvexHullPoints (
               min, max, hullPnts, hullPnts.length);
            break;
         }
         case Points: {
            computeCovarianceFromPoints (C, cent, pnts);
            setTransform (C, cent);
            computeBoundsFromPoints (min, max, pnts);
            break;
         }
         default:
            throw new UnsupportedOperationException (
               "Method " + method + " not implemented for points");
      }
      
      // set half widths from max/min
      setWidthsAndCenter (min, max, margin);
      
      
   }     

   /**
    * Returns the axes of the box, sorted in order from largest to smallest.
    * 
    */
   public void getSortedAxes (Vector3d[] axes) {
      int j0, j1, j2;

      Vector3d hw = myHalfWidths;

      if (hw.x >= hw.y) {
         if (hw.y >= hw.z) {
            j0 = 0;
            j1 = 1;
            j2 = 2;
         }
         else if (hw.x >= hw.z) { // output x, z, y
            j0 = 0;
            j1 = 2;
            j2 = 1;
         }
         else { // ouput z, x, y
            j0 = 2;
            j1 = 0;
            j2 = 1;
         }
      }
      else {
         if (hw.x >= hw.z) { // output y, x, z
            j0 = 1;
            j1 = 0;
            j2 = 2;
         }
         else if (hw.y >= hw.z) { // output y, z, x
            j0 = 1;
            j1 = 2;
            j2 = 0;
         }
         else { // output z, y, x
            j0 = 2;
            j1 = 1;
            j2 = 0;
         }
      }

      myX.R.getColumn (j0, axes[0]);
      myX.R.getColumn (j1, axes[1]);
      myX.R.getColumn (j2, axes[2]);
   }

   private double triangleArea (
      quickhull3d.Point3d p0, quickhull3d.Point3d p1, quickhull3d.Point3d p2) {
      double d1x = p1.x - p0.x;
      double d1y = p1.y - p0.y;
      double d1z = p1.z - p0.z;

      double d2x = p2.x - p0.x;
      double d2y = p2.y - p0.y;
      double d2z = p2.z - p0.z;

      double x = (d1y * d2z - d1z * d2y);
      double y = (d1z * d2x - d1x * d2z);
      double z = (d1x * d2y - d1y * d2x);

      return Math.sqrt (x * x + y * y + z * z) / 2;
   }

   protected HashedPointSet createPointSetForOBB (Boundable[] elems, int num) {

      HashedPointSet pointSet = new HashedPointSet(2*num);

      for (int i=0; i<num; i++) {
         Boundable elem = elems[i];
         int n = elem.numPoints();
         if (n <= 0) {
            throw new IllegalArgumentException (
               "Cannot create OBB: boundable type "+elem.getClass()+
               " does not contain any points");
         }
         for (int j=0; j<n; j++) {
            pointSet.add (elem.getPoint (j));
         }
      }
      return pointSet;
   }

   public void set (Boundable[] elems, int num, double margin, Method method) {

      Matrix3d C = new Matrix3d();
      Point3d cent = new Point3d();
      Point3d max = new Point3d (-INF, -INF, -INF);
      Point3d min = new Point3d (INF, INF, INF);

      switch (method) {
         case ConvexHull: {
            HashedPointSet pointSet = createPointSetForOBB (elems, num);
            quickhull3d.Point3d[] hullPnts =
               computeConvexHullAndCovariance (
                  C, cent, pointSet.getPointsAsDoubleArray(), pointSet.size());
            setTransform (C, cent);
            computeBoundsFromConvexHullPoints (
               min, max, hullPnts, hullPnts.length);
            break;
         }
         case Covariance: {
            computeCovarianceFromElements (C, cent, elems, num);
            setTransform (C, cent);
            computeBoundsFromElements (min, max, elems, num);
            break;
         }
         case Points: {
            HashedPointSet pointSet = createPointSetForOBB (elems, num);
            Point3d[] pnts = pointSet.getPoints();
            computeCovarianceFromPoints (C, cent, pnts);
            setTransform (C, cent);
            computeBoundsFromPoints (min, max, pnts);
            break;
         }
         default:
            throw new InternalErrorException (
               "Unimplemented method " + method);
      }
      setWidthsAndCenter (min, max, margin);
   }

   private void computeCovarianceFromPoints (
      Matrix3d C, Point3d cent, Point3d[] pnts) {

      for (Point3d pnt : pnts) {
         C.addOuterProduct (pnt, pnt);
         cent.add (pnt);
      }
      double size = pnts.length;
      cent.scale (1/size);
      C.addScaledOuterProduct (-size, cent, cent);
   }      

   private void computeBoundsFromPoints (
      Point3d min, Point3d max, Point3d[] pnts) {

      Vector3d xpnt = new Point3d();
      for (Point3d pnt : pnts) {
         xpnt.inverseTransform (myX, pnt);
         xpnt.updateBounds (min, max);
      }
   }

   private void computeCovarianceFromElements (
      Matrix3d C, Point3d cent, Boundable[] elems, int num) {
      
      Matrix3d Celem = new Matrix3d();
      Point3d centElem = new Point3d();

      double size = 0;
      for (int i=0; i<num; i++) {
         Boundable elem = elems[i];
         double s = elem.computeCovariance (Celem);
         if (s < 0) {
            throw new IllegalArgumentException (
               "Cannot create OBB: boundable type "+elem.getClass()+
               " does not implement computeCovariance()");
         }
         elem.computeCentroid (centElem);
         C.add (Celem);
         size += s;
         cent.scaledAdd (s, centElem);
      }
      cent.scale (1.0/size);
      C.addScaledOuterProduct (-size, cent, cent);
   }      

   private void computeBoundsFromElements (
      Point3d min, Point3d max, Boundable[] elems, int num) {

      Vector3d xpnt = new Point3d();
      for (int i=0; i<num; i++) {
         Boundable elem = elems[i];
         int n = elem.numPoints();
         if (n <= 0) {
            throw new IllegalArgumentException (
               "Cannot create OBB: boundable type "+elem.getClass()+
               " does not contain any points");
         }
         for (int j=0; j<n; j++) {
            xpnt.inverseTransform (myX, elem.getPoint(j));
            xpnt.updateBounds (min, max);
         }
      }
   }

//   protected void setUsingConvexHullOfPoints (
//      double[] pnts, int npnts, double margin) {
//
//      Matrix3d C = new Matrix3d();
//      Point3d cent = new Point3d();
//
//      quickhull3d.Point3d[] hullPnts =
//         computeConvexHullAndCovariance (C, cent, pnts, npnts);
//
//      setTransform (C, cent);
//      Point3d max = new Point3d (-INF, -INF, -INF);
//      Point3d min = new Point3d (INF, INF, INF);
//      computeBoundsFromConvexHullPoints (min, max, hullPnts, hullPnts.length);
//      setWidthsAndCenter (min, max, margin);
//   }
//
   private void computeBoundsFromConvexHullPoints (
      Point3d min, Point3d max, quickhull3d.Point3d[] pnts, int num) {

      Vector3d xpnt = new Point3d();
      for (int i=0; i<num; i++) {
         xpnt.set (pnts[i].x, pnts[i].y, pnts[i].z);
         xpnt.inverseTransform (myX);
         xpnt.updateBounds (min, max);
      }
   }

   /**
    * Sets the transform for this OBB from a covariance matrix and centroid.
    */
   private void setTransform (Matrix3d C, Point3d cent) {

      SymmetricMatrix3d Csym =
         new SymmetricMatrix3d (C.m00, C.m11, C.m22, C.m01, C.m02, C.m12);

      Matrix3d U = new Matrix3d();
      Vector3d sig = new Vector3d();
      Matrix3d V = new Matrix3d();

      // Might want to do this factorization with SVDecomposition3d, but
      // SymmetricMatrix3d.getSVD() is a bit faster
      Csym.getSVD (U, sig, V);
      if (U.determinant() < 0) {
         U.m02 = -U.m02;
         U.m12 = -U.m12;
         U.m22 = -U.m22;
      }

      // Code to handle degenetrate eigenvalues
      // Handle degeneracies corresponding to equal-length axes in the
      // inertia ellipsoid. If any two axes have similar lengths, then
      // U is redefined solely using the direction of the remaining
      // axis.
      //
      // In determining whether axes have similar lengths, we use
      // the fact that the moment of inertia of an ellipsoid
      // with unit mass is given by
      //
      // sig.x = 1/5 (b^2 + c^2)
      // sig.y = 1/5 (a^2 + c^2)
      // sig.z = 1/5 (a^2 + b^2)
      //
      // along with the fact that if a and b are close together, then
      //
      // a^2 - b^2 ~= 2 b (a-b)
      // 
      // a^2 + b^2 ~= 2 a^2 ~= 2 b^2
      //
      // and so
      //
      // (a - b) ~= (a^2 - b^2) / 2 b
      // ~= (a^2 - b^2) / (sqrt(2) * sqrt(2 b^2))
      // ~= (a^2 - b^2) / (sqrt(2) * sqrt(a^2 + b^2))
      boolean xout = false, yout = false, zout = false;
      sig.scale (5);

      double tol = sig.infinityNorm()*1e-6;

      if (Math.abs (sig.x - sig.y) / Math.sqrt (2 * sig.z) < tol) {
         // eliminate x and y
         xout = yout = true;
      }
      if (Math.abs (sig.x - sig.z) / Math.sqrt (2 * sig.y) < tol) {
         // eliminate x and z
         xout = zout = true;
      }
      if (Math.abs (sig.z - sig.y) / Math.sqrt (2 * sig.x) < tol) {
         // eliminate z and y
         zout = yout = true;
      }
      if (zout && xout && yout) {
         U.setIdentity();
      }
      else if (zout || yout || xout) {
         Vector3d zdir = new Vector3d();
         RotationMatrix3d R = new RotationMatrix3d();

         if (xout && yout) {
            U.getColumn (2, zdir);
         }
         else if (xout && zout) {
            U.getColumn (1, zdir);
         }
         else if (zout && yout) {
            U.getColumn (0, zdir);
         }
         R.setZDirection (zdir);
         U.set (R);
      }

      myX.R.set (U);
      myX.p.set (cent);
   }


   /**
    * Sets the widths and adjusts the center for this OBB, given
    * a set of bounds with respect to the box's orientation.
    */
   protected void setWidthsAndCenter (Vector3d min, Vector3d max, double margin) {

      myHalfWidths.sub (max, min);
      myHalfWidths.scale (0.5);
      myHalfWidths.add(margin, margin, margin); // add tolerance

      Vector3d cor = new Vector3d();
      cor.add (max, min);
      cor.scale (0.5);
      cor.transform (myX.R);

      myX.p.add (cor);
   }

   protected quickhull3d.Point3d[] computeConvexHullAndCovariance (
      Matrix3d C, Point3d cent, double[] pnts, int n) {
      if (pnts.length < 3 * n) {
         throw new IllegalArgumentException ("input array not long enough");
      }

      // System.out.println("setting obb " + pnts.length + " " + n);

      quickhull3d.Point3d[] hullVtxs;
      double rad = 0;
      
      C.setZero();
      cent.setZero();

      try {
         // compute convex hull
         QuickHull3D chull = new QuickHull3D();
         chull.build (pnts, n);
         chull.triangulate();
         // System.out.println("triangulated convex hull");
         hullVtxs = chull.getVertices();
         int[][] hullFaces = chull.getFaces();
         // System.out.println("got convex hull mesh");

         // do a rough estimate of the radius of the hull
         Point3d pmin = new Point3d();
         Point3d pmax = new Point3d();
         Point3d p = new Point3d();
         for (int i = 0; i < hullVtxs.length; i++) {
            p.set (hullVtxs[i].x, hullVtxs[i].y, hullVtxs[i].z);
            p.updateBounds (pmin, pmax);
         }
         rad = pmin.distance (pmax) / 2;

         double totalArea = 0;
         Point3d pc = new Point3d(); // face centroid
         for (int i = 0; i < hullFaces.length; i++) {
            int[] idxs = hullFaces[i];
            quickhull3d.Point3d p0 = hullVtxs[idxs[0]];
            quickhull3d.Point3d p1 = hullVtxs[idxs[1]];
            quickhull3d.Point3d p2 = hullVtxs[idxs[2]];

            double area = triangleArea (p0, p1, p2);
            totalArea += area;

            pc.x = (p0.x + p1.x + p2.x) / 3;
            pc.y = (p0.y + p1.y + p2.y) / 3;
            pc.z = (p0.z + p1.z + p2.z) / 3;

            C.m00 += area * (9*pc.x*pc.x + p0.x*p0.x + p1.x*p1.x + p2.x*p2.x);
            C.m11 += area * (9*pc.y*pc.y + p0.y*p0.y + p1.y*p1.y + p2.y*p2.y);
            C.m22 += area * (9*pc.z*pc.z + p0.z*p0.z + p1.z*p1.z + p2.z*p2.z);

            C.m01 += area * (9*pc.x*pc.y + p0.x*p0.y + p1.x*p1.y + p2.x*p2.y);
            C.m02 += area * (9*pc.x*pc.z + p0.x*p0.z + p1.x*p1.z + p2.x*p2.z);
            C.m12 += area * (9*pc.y*pc.z + p0.y*p0.z + p1.y*p1.z + p2.y*p2.z);
            cent.scaledAdd (area, pc, cent);
         }
         C.m10 = C.m01;
         C.m20 = C.m02;
         C.m21 = C.m12;

         cent.scale (1 / totalArea);

         C.scale (1 / (12 * totalArea));

         C.addScaledOuterProduct (-1, cent, cent);
      }
      catch (Exception e) {
         // in case the convex hull calculation fails, simply compute
         // the covariance and centroid based on the points themselves
         hullVtxs = new quickhull3d.Point3d[n];
         Point3d pnt = new Point3d();

         for (int k=0; k<n; k++) {
            int base = 3*k;
            pnt.set (pnts[base], pnts[base+1], pnts[base+2]);
            hullVtxs[k] = new quickhull3d.Point3d (pnt.x, pnt.y, pnt.z);
            C.addOuterProduct (pnt, pnt);
            cent.add (pnt);
         }
         double size = n;
         cent.scale (1/size);
         C.addScaledOuterProduct (-size, cent, cent);
      }
      return hullVtxs;
   }

   public boolean containsPoint (Point3d pnt) {
      Point3d p = new Point3d(pnt);
      p.inverseTransform(myX);
      Vector3d hw = myHalfWidths;
      boolean status = 
         (-hw.x <= p.x && p.x <= hw.x && 
          -hw.y <= p.y && p.y <= hw.y && 
          -hw.z <= p.z && p.z <= hw.z);
      return status;
   }

   public boolean intersectsSphere (Point3d pnt, double r) {
      Point3d p = new Point3d(pnt);
      p.inverseTransform(myX);
      Vector3d hw = myHalfWidths;
      boolean status =
         (-hw.x <= p.x+r && p.x-r <= hw.x && 
          -hw.y <= p.y+r && p.y-r <= hw.y && 
          -hw.z <= p.z+r && p.z-r <= hw.z);
      return status;
   }
   
   public boolean intersectsLine (
      double[] lam, Point3d origin, Vector3d dir, double min, double max) {

      Point3d lorigin = new Point3d();
      Vector3d ldir = new Vector3d();

      lorigin.inverseTransform (myX, origin);
      ldir.inverseTransform (myX, dir);
      Vector3d hw = myHalfWidths;

      double dist0, dist1, invdir;

      if (ldir.x == 0) {
         if (-hw.x - lorigin.x > 0 || hw.x - lorigin.x < 0) {
            return false;
         }
      }
      else {
         invdir = 1.0 / ldir.x;
         dist0 = ((-hw.x) - lorigin.x) * invdir;
         dist1 = (hw.x - lorigin.x) * invdir;

         if (ldir.x >= 0) {
            if (dist0 > min)
               min = dist0;
            if (dist1 < max)
               max = dist1;
         }
         else {
            if (dist1 > min)
               min = dist1;
            if (dist0 < max)
               max = dist0;
         }
         if (min > max)
            return false;
      }

      if (ldir.y == 0) {
         if (-hw.y - lorigin.y > 0 || hw.y - lorigin.y < 0) {
            return false;
         }         
      }
      else {
         invdir = 1.0 / ldir.y;
         dist0 = ((-hw.y) - lorigin.y) * invdir;
         dist1 = (hw.y - lorigin.y) * invdir;
         if (ldir.y >= 0) {
            if (dist0 > min)
               min = dist0;
            if (dist1 < max)
               max = dist1;
         }
         else {
            if (dist1 > min)
               min = dist1;
            if (dist0 < max)
               max = dist0;
         }
         if (min > max)
            return false;
      }

      if (ldir.z == 0) { 
         if (-hw.z - lorigin.z > 0 || hw.z - lorigin.z < 0) {
            return false;
         }         
      }
      else {
         invdir = 1.0 / ldir.z;
         dist0 = ((-hw.z) - lorigin.z) * invdir;
         dist1 = (hw.z - lorigin.z) * invdir;
         if (ldir.z >= 0) {
            if (dist0 > min)
               min = dist0;
            if (dist1 < max)
               max = dist1;
         }
         else {
            if (dist1 > min)
               min = dist1;
            if (dist0 < max)
               max = dist0;
         }

         if (min > max)
            return false;
      }

      if (lam != null) {
         lam[0] = min;
         lam[1] = max;
      }
      return true;
   }

   // public void update () {
   //    update (defaultTolerance);
   // }

   /**
    * Update a set of half-widths to accommodate a single point <code>p</code>
    * within a prescribed margin for coordinate frame of this box.
    *
    * @param hw half-widths to update
    * @param p point to accommodate
    * @param margin extra space margin
    */
   private void updateForPoint (Vector3d hw, Point3d p, double margin) {
      // transform x, y, z of p into coordinates bx, by, bz of the coordinate
      // frame of this box.
      RotationMatrix3d R = myX.R;
      double x = p.x - myX.p.x;
      double y = p.y - myX.p.y;
      double z = p.z - myX.p.z;
      double bx = R.m00*x + R.m10*y + R.m20*z;
      double by = R.m01*x + R.m11*y + R.m21*z;
      double bz = R.m02*x + R.m12*y + R.m22*z;

      double del;

      // now use bx, by, bz to update the bounds
      if ((del = bx+margin-hw.x) > 0) {
         hw.x += del;
      }
      else if ((del = -bx+margin-hw.x) > 0) {
         hw.x += del;
      }
      if ((del = by+margin-hw.y) > 0) {
         hw.y += del;
      }
      else if ((del = -by+margin-hw.y) > 0) {
         hw.y += del;
      }
      if ((del = bz+margin-hw.z) > 0) {
         hw.z += del;
      }
      else if ((del = -bz+margin-hw.z) > 0) {
         hw.z += del;
      }
   }
   
  /**
    * Update the half-widths of this node to accommodate a single point
    * <code>p</code> within a prescribed margin.
    *
    * @param p point to accommodate
    * @param margin extra space margin
    */
   public boolean updateForPoint (Point3d p, double margin) {
      // transform x, y, z of p into coordinates bx, by, bz of the coordinate
      // frame of this box.
      boolean modified = false;
      RotationMatrix3d R = myX.R;
      double x = p.x - myX.p.x;
      double y = p.y - myX.p.y;
      double z = p.z - myX.p.z;
      double bx = R.m00*x + R.m10*y + R.m20*z;
      double by = R.m01*x + R.m11*y + R.m21*z;
      double bz = R.m02*x + R.m12*y + R.m22*z;

      double del;

      Vector3d hw = myHalfWidths;

      // now use bx, by, bz to update the bounds
      if ((del = bx+margin-hw.x) > 0) {
         hw.x += del;
         modified = true;
      }
      else if ((del = -bx+margin-hw.x) > 0) {
         hw.x += del;
         modified = true;
      }
      if ((del = by+margin-hw.y) > 0) {
         hw.y += del;
         modified = true;
      }
      else if ((del = -by+margin-hw.y) > 0) {
         hw.y += del;
         modified = true;
      }
      if ((del = bz+margin-hw.z) > 0) {
         hw.z += del;
         modified = true;
      }
      else if ((del = -bz+margin-hw.z) > 0) {
         hw.z += del;
         modified = true;
      }
      return modified;
   }
   
   public boolean update (double margin) {
      // Could be worth optimizing this by keeping a list of points
      // in addition to the list of elements, because of point
      // repetition among the elements, but if there are
      // only 1-2 elements, then the repitition is not likely
      // to be very large.      
      boolean modified = false;
      for (int i=0; i<myElements.length; i++) {
         Boundable elem = myElements[i];
         for (int j=0; j<elem.numPoints(); j++) {
            for (OBB node=this; node != null; node=(OBB)node.getParent()) {
               modified |= node.updateForPoint (elem.getPoint (j), margin);
            }
         }
      }
      
      return modified;
   }

   public boolean isContained (Boundable[] boundables, double tol) {
      Vector3d hw = new Vector3d();
      double eps = myHalfWidths.norm()*EPS;
      for (int i=0; i<boundables.length; i++) {
         Boundable elem = boundables[i];
         for (int j=0; j<elem.numPoints(); j++) {
            if (!intersectsSphere (elem.getPoint(j), -(tol-eps))) {
               return false;
            }
         }
      }
      return true;
   }

   public double distanceToPoint (Point3d pnt) {
      // first convert point to the frame of the OBB
      RotationMatrix3d R = myX.R;
      double x = pnt.x - myX.p.x;
      double y = pnt.y - myX.p.y;
      double z = pnt.z - myX.p.z;

      double px = R.m00*x + R.m10*y + R.m20*z;
      double py = R.m01*x + R.m11*y + R.m21*z;
      double pz = R.m02*x + R.m12*y + R.m22*z;

      // now compare transformed point directly to half widths
      Vector3d hw = myHalfWidths;

      if ((x = px-hw.x) > 0) {
         if ((y = py-hw.y) > 0) {
            if ((z = pz-hw.z) > 0) {
               return Math.sqrt (x*x + y*y + z*z);
            }
            else if ((z = pz+hw.z) >= 0) {
               return Math.sqrt (x*x + y*y);               
            }
            else { // pz+hw.z < 0
               return Math.sqrt (x*x + y*y + z*z);
            }
         }
         else if ((y = py+hw.y) > 0) {
            if ((z = pz-hw.z) > 0) {
               return Math.sqrt (x*x + z*z);
            }
            else if ((z = pz+hw.z) >= 0) {
               return x;
            }
            else { // pz+hw.z < 0
               return Math.sqrt (x*x + z*z);
            }
         }
         else { // py+hw.y < 0
            if ((z = pz-hw.z) > 0) {
               return Math.sqrt (x*x + y*y + z*z);
            }
            else if ((z = pz+hw.z) >= 0) {
               return Math.sqrt (x*x + y*y);               
            }
            else { // pz+hw.z < 0
               return Math.sqrt (x*x + y*y + z*z);
            }
         }
      }
      else if ((x = px+hw.x) >= 0) {
         if ((y = py-hw.y) > 0) {
            if ((z = pz-hw.z) > 0) {
               return Math.sqrt (y*y + z*z);
            }
            else if ((z = pz+hw.z) >= 0) {
               return y;
            }
            else { // pz+hw.z < 0
               return Math.sqrt (y*y + z*z);
            }
         }
         else if ((y = py+hw.y) > 0) {
            if ((z = pz-hw.z) > 0) {
               return z;
            }
            else if ((z = pz+hw.z) >= 0) {
               return 0;
            }
            else { // pz+hw.z < 0
               return -z;
            }
         }
         else { // py+hw.y < 0
            if ((z = pz-hw.z) > 0) {
               return Math.sqrt (y*y + z*z);
            }
            else if ((z = pz+hw.z) >= 0) {
               return -y;
            }
            else { // pz+hw.z < 0
               return Math.sqrt (y*y + z*z);
            }
         }
      }
      else { // px+hw.x < 0
         if ((y = py-hw.y) > 0) {
            if ((z = pz-hw.z) > 0) {
               return Math.sqrt (x*x + y*y + z*z);
            }
            else if ((z = pz+hw.z) >= 0) {
               return Math.sqrt (x*x + y*y);               
            }
            else { // pz+hw.z < 0
               return Math.sqrt (x*x + y*y + z*z);
            }
         }
         else if ((y = py+hw.y) > 0) {
            if ((z = pz-hw.z) > 0) {
               return Math.sqrt (x*x + z*z);
            }
            else if ((z = pz+hw.z) >= 0) {
               return -x;
            }
            else { // pz+hw.z < 0
               return Math.sqrt (x*x + z*z);
            }
         }
         else { // py+hw.y < 0
            if ((z = pz-hw.z) > 0) {
               return Math.sqrt (x*x + y*y + z*z);
            }
            else if ((z = pz+hw.z) >= 0) {
               return Math.sqrt (x*x + y*y);               
            }
            else { // pz+hw.z < 0
               return Math.sqrt (x*x + y*y + z*z);
            }
         }
      }
   }

   /**
    * Code is modified from "An Efficient and Robust Ray-Box Intersection
    * Algorithm", Amy Williams, Steve Barrus, R. Keith Morley, Peter Shirley,
    * University of Utah.
    */
   public double distanceAlongLine (
      Point3d origin, Vector3d dir, double min, double max) {

      // first convert origin and direction to the frame of the OBB
      RotationMatrix3d R = myX.R;
      double x = origin.x - myX.p.x;
      double y = origin.y - myX.p.y;
      double z = origin.z - myX.p.z;

      double ox = R.m00*x + R.m10*y + R.m20*z;
      double oy = R.m01*x + R.m11*y + R.m21*z;
      double oz = R.m02*x + R.m12*y + R.m22*z;

      x = dir.x;
      y = dir.y;
      z = dir.z;

      double dx = R.m00*x + R.m10*y + R.m20*z;
      double dy = R.m01*x + R.m11*y + R.m21*z;
      double dz = R.m02*x + R.m12*y + R.m22*z;

      // now compare transformed point directly to half widths
      Vector3d hw = myHalfWidths;

      double tmin, tmax;

      if (dx == 0) {
         if (-hw.x - ox > 0 ||  hw.x - ox < 0) {
            return INF;
         }
      }
      else {
         double divx = 1/dx;
         if (divx >= 0) {
            tmin = (-hw.x - ox)*divx;
            tmax = ( hw.x - ox)*divx;
         }
         else {
            tmin = ( hw.x - ox)*divx;
            tmax = (-hw.x - ox)*divx;
         }
         if (tmin > min) {
            min = tmin;
         }
         if (tmax < max) {
            max = tmax;
         }
         if (min > max) {
            return INF;
         }
      }

      if (dy == 0) {
         if (-hw.y - oy > 0 ||  hw.y - oy < 0) {
            return INF;
         }
      }
      else {
         double divy = 1/dy;
         if (divy >= 0) {
            tmin = (-hw.y - oy)*divy;
            tmax = ( hw.y - oy)*divy;
         }
         else {
            tmin = ( hw.y - oy)*divy;
            tmax = (-hw.y - oy)*divy;
         }
         if (tmin > min) {
            min = tmin;
         }
         if (tmax < max) {
            max = tmax;
         }
         if (min > max) {
            return INF;
         }
      }

      if (dz == 0) {
         if (-hw.z - oz > 0 ||  hw.z - oz < 0) {
            return INF;
         }
      }
      else {
         double divz = 1/dz;
         if (divz >= 0) {
            tmin = (-hw.z - oz)*divz;
            tmax = ( hw.z - oz)*divz;
         }
         else {
            tmin = ( hw.z - oz)*divz;
            tmax = (-hw.z - oz)*divz;
         }
         if (tmin > min) {
            min = tmin;
         }
         if (tmax < max) {
            max = tmax;
         }
         if (min > max) {
            return INF;
         }
      }
      if (min > 0) {
         return min;
      }
      else if (max < 0) {
         return -max;
      }
      else {
         return 0;
      }
   }

   public void updateBounds (Vector3d pmin, Vector3d pmax) {

      Vector3d hw = myHalfWidths;

      Vector3d xEdge = new Vector3d (2 * hw.x, 0, 0);
      Vector3d yEdge = new Vector3d (0, 2 * hw.y, 0);
      Vector3d zEdge = new Vector3d (0, 0, 2 * hw.z);

      xEdge.transform (myX.R);
      yEdge.transform (myX.R);
      zEdge.transform (myX.R);

      Point3d p = new Point3d (-hw.x, -hw.y, -hw.z);
      p.transform (myX);
      p.updateBounds (pmin, pmax);
      p.add (yEdge);
      p.updateBounds (pmin, pmax);
      p.add (zEdge);
      p.updateBounds (pmin, pmax);
      p.sub (yEdge);
      p.updateBounds (pmin, pmax);

      p.add (xEdge);
      p.updateBounds (pmin, pmax);
      p.sub (zEdge);
      p.updateBounds (pmin, pmax);
      p.add (yEdge);
      p.updateBounds (pmin, pmax);
      p.add (zEdge);
      p.updateBounds (pmin, pmax);
   }

   public void render (Renderer renderer, int flags) {

      renderer.setShading (Shading.NONE);

      Vector3d hw = myHalfWidths;
      renderer.pushModelMatrix();
      renderer.mulModelMatrix (myX);

      renderer.setColor (0, 0, 1);
      renderer.beginDraw (DrawMode.LINE_STRIP);
      renderer.addVertex (hw.x, hw.y, hw.z);
      renderer.addVertex (-hw.x, hw.y, hw.z);
      renderer.addVertex (-hw.x, -hw.y, hw.z);
      renderer.addVertex (hw.x, -hw.y, hw.z);
      renderer.addVertex (hw.x, hw.y, hw.z);
      renderer.endDraw();

      renderer.beginDraw (DrawMode.LINE_STRIP);
      renderer.addVertex (hw.x, hw.y, -hw.z);
      renderer.addVertex (-hw.x, hw.y, -hw.z);
      renderer.addVertex (-hw.x, -hw.y, -hw.z);
      renderer.addVertex (hw.x, -hw.y, -hw.z);
      renderer.addVertex (hw.x, hw.y, -hw.z);
      renderer.endDraw();

      renderer.setColor (0, 0, 1);
      renderer.beginDraw (DrawMode.LINES);
      renderer.addVertex (hw.x, hw.y, hw.z);
      renderer.addVertex (hw.x, hw.y, -hw.z);
      renderer.addVertex (-hw.x, hw.y, hw.z);
      renderer.addVertex (-hw.x, hw.y, -hw.z);
      renderer.addVertex (-hw.x, -hw.y, hw.z);
      renderer.addVertex (-hw.x, -hw.y, -hw.z);
      renderer.addVertex (hw.x, -hw.y, hw.z);
      renderer.addVertex (hw.x, -hw.y, -hw.z);
      renderer.endDraw();

      renderer.popModelMatrix();

      renderer.setShading (Shading.FLAT);
   }

   public void prerender (RenderList list) {
   }

   
   public boolean intersectsLineSegment(Point3d p1, Point3d p2) {

      Vector3d hw = myHalfWidths;
      Point3d p1t = new Point3d(p1);
      Point3d p2t = new Point3d(p2);
      p1t.inverseTransform(myX);
      p2t.inverseTransform(myX);
      
      // if either point is inside, return true
      if (Math.abs(p1t.x) <= hw.x && 
         Math.abs(p1t.y) <= hw.y && 
         Math.abs(p1t.z) <= hw.z) {
         return true;
      }
      if (Math.abs(p2t.x) <= hw.x && 
         Math.abs(p2t.y) <= hw.y && 
         Math.abs(p2t.z) <= hw.z) {
         return true;
      }
      
      // otherwise, intersect with edges
      double[] dir = {p2t.x-p1t.x, p2t.y-p1t.y, p2t.z-p1t.z};
      double[] min = {-hw.x, -hw.y, -hw.z};
      double[] max = {hw.x, hw.y, hw.z};
      double[] p1v = {p1t.x, p1t.y, p1t.z};
      double[] p2v = {p2t.x, p2t.y, p2t.z};

      double near = -INF;
      double far = INF;
      double t1, t2, tMin, tMax;
      
      // check line/plane intersections
      for (int i=0; i<3; i++) {
         if (dir[i] == 0) {
            if ( (min[i]-p1v[i] > 0) || (max[i]-p1v[i] < 0)) {
               return false;
            }
         } else {
            t1 = (min[i]-p1v[i]) / dir[i];
            t2 = (min[i]-p2v[i]) / dir[i];
            tMin = Math.min(t1, t2);
            tMax = Math.max(t1, t2);
            if (tMin > near) {
               near = tMin;
            }
            if (tMax < far) {
               far = tMax;
            }
            if (near > far) 
               return false;  
         }
      }
      
      if ((near >= 0 && near <= 1) || (far >= 0 && far <=1)){
         return true;
      }
      return false;
   }
   
   public boolean intersectsPlane(Vector3d n, double d) {

      // check plane has at least one corder on each side
      boolean up = false;
      boolean down = false;
      d = d-n.dot(myX.getOffset());   // adjust offset
      Vector3d nt = new Vector3d(n);
      nt.inverseTransform(myX);
      Vector3d hw = myHalfWidths;

      double[] x = {-hw.x, hw.x};
      double[] y = {-hw.y, hw.y};
      double[] z = {-hw.z, hw.z};
      double b = 0;
      for (int i=0; i<2; i++) {
         for  (int j=0; j<2; j++) {
            for (int k=0; k<2; k++) {
               b = nt.x*x[i] + nt.y*y[j] + nt.z*z[k] - d;
               up = up | (b >=0);
               down = down | (b<=0);
               if (up && down) {
                  return true;
               }
            }
         }
      }
      return false;
   }
   
   /**
    * {@inheritDoc}
    */
   public void scale (double s) {
      myHalfWidths.scale (s);
   }
   
}
