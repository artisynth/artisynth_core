/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.util.*;
import java.io.PrintStream;

import maspack.fileutil.NativeLibraryManager;
import maspack.matrix.*;
import maspack.util.ArraySort;

public class TetgenTessellator {

   private native int doBuildFromPoints (
      long handle, double[] pntCoords, int numPnts);
   private native int doBuildFromMeshAndPoints (
      long handle, double[] pntCoords, int numPnts, 
      int[] faceIndices, int numFaces, int numIndices,
      double quality,
      double[] includeCoords, int includeCoordsOffset,
      int numIncludePnts);
   private native int doBuildFromMesh (
      long handle, double[] pntCoords, int numPnts, 
      int[] faceIndices, int numFaces, int numIndices, 
      double quality);
   
   private native int doRefineMesh(
      long handle, double[] nodeCoords, int numNodes,
      int[] tetIndices, int numTets, double quality);
   
   private native int doRefineMeshAddPoints(
      long handle, double[] nodeCoords, int numNodes,
      int[] tetIndices, int numTets, double quality,
      double[] addCoords, int numAddPnts );
   
   private native int doGetNumHullFaces (long handle);
   private native int doGetNumTets (long handle);
   private native void doGetHullFaces (long handle, int[] faces);
   private native void doGetTets (long handle, int[] tets);
   private native int doGetNumPoints (long handle);
   private native void doGetPoints (long handle, double[] pntCoords);
   private native long doCreateTessellator();
   private native void doDeleteTessellator(long handle);

   private long myHandle = 0;
   private static boolean myNativeSupportLoaded = false;
   private int myDimen = -1;

   private double myCharLength = 0;
   private int[] myMaxVtxs = new int[3]; // index of max coord in each direction
   private int[] myMinVtxs = new int[3]; // index of max coord in each direction
   private Vector3d myMax = new Vector3d(); // max bounding box coordinates
   private Vector3d myMin = new Vector3d(); // min bounding box coordinates

   private RigidTransform3d myReduceX = new RigidTransform3d();
   private int[] myReducedIndices;
   private double[] myReducedCoords;

   public TetgenTessellator() {

      if (!myNativeSupportLoaded) {
         try {
            NativeLibraryManager.load ("TetgenJNI.1.5.1.0");
            myNativeSupportLoaded = true;
         }
         catch (UnsatisfiedLinkError e) {
            System.out.println (e.getMessage());
            throw new UnsupportedOperationException (
               "Can't load native library \"TetgenJNI\"");
         }
      }
   }

   private void updateBounds (double[] coords, int numc) {
      Double inf = Double.POSITIVE_INFINITY;

      myMaxVtxs[0] = myMaxVtxs[1] = myMaxVtxs[2] = 0;
      myMinVtxs[0] = myMinVtxs[1] = myMinVtxs[2] = 0;

      if (numc == 0) {
         myCharLength = 0;
         myMax.setZero();
         myMin.setZero();
      }
      else {
         myMax.set (coords[0], coords[1], coords[2]);
         myMin.set (coords[0], coords[1], coords[2]);
      }
      for (int i=1; i<numc; i++) {
         double x = coords[i*3  ];
         double y = coords[i*3+1];
         double z = coords[i*3+2];
         if (x > myMax.x) {
            myMax.x = x;
            myMaxVtxs[0] = i;
         }
         else if (x < myMin.x) {
            myMin.x = x;
            myMinVtxs[0] = i;
         }
         if (y > myMax.y) {
            myMax.y = y;
            myMaxVtxs[1] = i;
         }
         else if (y < myMin.y) {
            myMin.y = y;
            myMinVtxs[1] = i;
         }
         if (z > myMax.z) {
            myMax.z = z;
            myMaxVtxs[2] = i;
         }
         else if (z < myMin.z) {
            myMin.z = z;
            myMinVtxs[2] = i;
         }
      }
      myCharLength = Math.max(myMax.x-myMin.x, myMax.y-myMin.y);
      myCharLength = Math.max(myMax.z-myMin.z, myCharLength); 
   }

   private int checkDimensionality (
      RigidTransform3d X, double[] coords, int nump) {

      X.setIdentity();
      if (nump < 1) {
         return -1;
      }
      else if (nump == 1) {
         return 0;
      }

      double tolerance = 1e-10*myCharLength;

      int vtx0, vtx1, vtx2;
      double max = 0;
      int kmax = 0;

      for (int k=0; k<3; k++) {
         double diff = coords[3*myMaxVtxs[k]+k] - coords[3*myMinVtxs[k]+k];
         if (diff > max) {
            max = diff;
            kmax = k;
         }
      }

      vtx0 = myMaxVtxs[kmax];
      vtx1 = myMinVtxs[kmax];

      X.p.set ((coords[3*vtx0  ]+coords[3*vtx1  ])/2,
         (coords[3*vtx0+1]+coords[3*vtx1+1])/2,
         (coords[3*vtx0+2]+coords[3*vtx1+2])/2);

      // set third vertex to be the vertex farthest from
      // the line between vtx0 and vtx1
      Vector3d u01 = new Vector3d();
      Vector3d u02 = new Vector3d();
      Vector3d diff = new Vector3d();
      Point3d pnt = new Point3d();
      Vector3d xprod = new Vector3d();
      double maxSqr = -1;
      subtractCoords (u01, coords, vtx1, vtx0);
      u01.normalize();
      X.R.setXDirection (u01);
      if (nump == 2) {
         return 1;
      }
      vtx2 = 0;
      for (int i=0; i<nump; i++) {
         getCoords (pnt, coords, i);
         pnt.inverseTransform (X);
         double lenSqr = pnt.y*pnt.y + pnt.z*pnt.z;
         if (lenSqr > maxSqr && /*paranoid: */ i != vtx0 && i != vtx1) {
            maxSqr = lenSqr; 
            vtx2 = i;
         }
      }
      if (Math.sqrt(maxSqr) <= tolerance) {
         return 1;
      }

      subtractCoords (u02, coords, vtx2, vtx0);
      u02.normalize();
      X.R.setXYDirections (u01, u02);
      if (nump == 3) {
         return 2;
      }
      double maxDist = -1;
      for (int i=0; i<nump; i++) {
         getCoords (pnt, coords, i);
         pnt.inverseTransform (X);
         double dist = Math.abs (pnt.z);
         if (dist > maxDist && i != vtx0 && i != vtx1 && i != vtx2) {
            maxDist = dist;
         }
      }
      if (maxDist <= tolerance) {
         return 2;
      }
      else {
         X.setIdentity();
         return 3;
      }
   }

   private void setCoords (double[] coords, int idx, Point3d pos) {
      coords[3*idx  ] = pos.x;
      coords[3*idx+1] = pos.y;
      coords[3*idx+2] = pos.z;
   }

   private void getCoords (Point3d res, double[] coords, int idx) {
      res.x = coords[3*idx  ];
      res.y = coords[3*idx+1];
      res.z = coords[3*idx+2];
   }

   private void subtractCoords (
      Vector3d res, double[] coords, int idxA, int idxB) {

      res.x = coords[idxA*3  ] - coords[idxB*3  ];
      res.y = coords[idxA*3+1] - coords[idxB*3+1];
      res.z = coords[idxA*3+2] - coords[idxB*3+2];
   }

   private void buildFrom1DPoints (double[] coords, int nump) {

      RigidTransform3d X = myReduceX;
      Point3d pnt = new Point3d();

      myReducedCoords = new double[nump];
      int[] idxs = new int[nump];
      for (int i=0; i<nump; i++) {
         getCoords (pnt, coords, i);
         pnt.inverseTransform (X);
         idxs[i] = i;
         myReducedCoords[i] = pnt.x;
      }
      double[] tmpx = new double[nump];
      for (int i=0; i<nump; i++) {
         tmpx[i] = myReducedCoords[i];
      }


      ArraySort.quickSort (tmpx, idxs);
      myReducedIndices = idxs;
   }

   private void buildFrom2DPoints (double[] coords, int nump) {

      RigidTransform3d X = myReduceX;
      Point3d pnt = new Point3d();
      // use twice as many coordinates for creating a 3D convex hull from which
      // the Delaunay triangulation will be extracted.
      double[] xcoords = new double[6*nump];
      myReducedCoords = new double[3*nump];

      double maxz = 0;
      for (int i=0; i<nump; i++) {
         getCoords (pnt, coords, i);
         pnt.inverseTransform (X);
         pnt.z = 0;
         setCoords (myReducedCoords, i, pnt);
         pnt.z = (pnt.x*pnt.x + pnt.y*pnt.y)/myCharLength;
         if (pnt.z > maxz) {
            maxz = pnt.z;
         }
         setCoords (xcoords, i, pnt);
      }
      for (int i=0; i<nump; i++) {
         getCoords (pnt, coords, i);
         pnt.inverseTransform (X);
         pnt.z = 2*maxz;
         setCoords (xcoords, i+nump, pnt);
      }
      if (doBuildFromPoints (myHandle, xcoords, 2*nump) < 0) {
         throw new IllegalArgumentException ("Error creating tesselation");
      }      
      int numAllFaces = doGetNumHullFaces(myHandle);
      int[] allFaces = new int[3*numAllFaces];
      if (numAllFaces > 0) {
         doGetHullFaces (myHandle, allFaces);
      }
      int numFaces = 0;
      // use only those faces associated with the orginal pnts
      for (int k=0; k<3*numAllFaces; k += 3) {
         if (allFaces[k] < nump && allFaces[k+1] < nump && allFaces[k+2] < nump) {
            numFaces++;
         }
      }
      int[] faces = new int[3*numFaces];
      int kk = 0;
      for (int k=0; k<3*numAllFaces; k += 3) {
         if (allFaces[k] < nump && allFaces[k+1] < nump && allFaces[k+2] < nump) {
            faces[kk  ] = allFaces[k  ];
            faces[kk+1] = allFaces[k+1];
            faces[kk+2] = allFaces[k+2];
            kk += 3;
         }
      }
      myReducedIndices = faces;
   }

   public int buildFromPoints (Point3d[] pnts) {

      int nump = pnts.length;
      double[] coords = new double[3*nump];

      for (int i=0; i<nump; i++) {
         setCoords (coords, i, pnts[i]);
      }
      return buildFromPoints (coords);
   }

   public int buildFromPoints (double[] coords) {
      if (myHandle == 0) {
         myHandle = doCreateTessellator();
      }
      int nump = coords.length/3;
      updateBounds (coords, nump);

      myReducedIndices = null;
      myReducedCoords = null;
      myDimen = -1;

      int dimen = checkDimensionality (myReduceX, coords, nump);
      if (dimen == 0) {
         myReducedCoords = new double[3];
         for (int i=0; i<3; i++) {
            myReducedCoords[i] = coords[i];
         }
         myReducedIndices = new int[0];
      }
      else if (dimen == 1) {
         buildFrom1DPoints (coords, nump);
      }
      else if (dimen == 2) {
         buildFrom2DPoints (coords, nump);
      }
      else {
         if (doBuildFromPoints (myHandle, coords, nump) < 0) {
            throw new IllegalArgumentException ("Error creating tesselation");
         }
      }
      myDimen = dimen;
      return dimen;
   }
  
   public void refineMesh(double[] nodeCoords, int numNodes, int[] tetIndices, int numTets,
      double quality) {
      myDimen = 3;        // assume a 3-D mesh
      if (myHandle == 0) {
         myHandle = doCreateTessellator();
      }
     
      if (doRefineMesh(myHandle, nodeCoords, numNodes,
         tetIndices, numTets, quality) < 0) {
         throw new IllegalArgumentException ("Error creating tesselation");
      }
   }
   
   public void refineMesh(double[] nodeCoords, int numNodes, int[] tetIndices, int numTets,
      double quality, double[] addCoords, int numAddPoints) {
      myDimen = 3;        // assume a 3-D mesh
      if (myHandle == 0) {
         myHandle = doCreateTessellator();
      }
     
      if (doRefineMeshAddPoints(myHandle, nodeCoords, numNodes,
         tetIndices, numTets, quality, addCoords, numAddPoints) < 0) {
         throw new IllegalArgumentException ("Error creating tesselation");
      }
      
   }

   public void buildFromMeshAndPoints(PolygonalMesh mesh, double quality, Point3d[] includePoints) {
      myDimen = 3;        // assume a 3-D mesh
      if (myHandle == 0) {
         myHandle = doCreateTessellator();
      }
      ArrayList<Vertex3d> vtxs = mesh.getVertices();
      int numPnts = vtxs.size();
      double[] pntCoords = new double[3*numPnts];
      for (int i=0; i<numPnts; i++) {
         Point3d pos = vtxs.get(i).pnt;
         pntCoords[3*i  ] = pos.x;
         pntCoords[3*i+1] = pos.y;
         pntCoords[3*i+2] = pos.z;
      }
      ArrayList<Face> faces = mesh.getFaces();
      int numIndices = 0;
      for (int i=0; i<faces.size(); i++) {
         numIndices += faces.get(i).numEdges();
      }
      int[] faceIndices = new int[numIndices+faces.size()];
      int k = 0;
      for (int i=0; i<faces.size(); i++) {
         Face face = faces.get(i);
         faceIndices[k++] = face.numEdges();
         HalfEdge he0 = face.firstHalfEdge();
         HalfEdge he = he0;
         do {
            faceIndices[k++] = he.getHead().getIndex();
            he = he.getNext();
         }
         while (he != he0);
      }
      updateBounds (pntCoords, numPnts);

      double[] includeCoords = null;
      if (includePoints != null) {
         int numIncludePnts = includePoints.length;
         includeCoords = new double[3*numIncludePnts];

         for (int i=0; i<numIncludePnts; i++) {
            includeCoords[3*i] = includePoints[i].x;
            includeCoords[3*i+1] = includePoints[i].y;
            includeCoords[3*i+2] = includePoints[i].z;
         }
         updateBounds(includeCoords, numIncludePnts);

         if (doBuildFromMeshAndPoints (myHandle, pntCoords, numPnts,
            faceIndices, faces.size(), numIndices, quality, 
            includeCoords, 0, numIncludePnts) < 0) {
            throw new IllegalArgumentException ("Error creating tesselation");
         }
      } else {
         if (doBuildFromMesh (myHandle, pntCoords, numPnts,
            faceIndices, faces.size(), numIndices,
            quality) < 0) {
            throw new IllegalArgumentException ("Error creating tesselation");
         }
      }


   }

   public void buildFromMesh (PolygonalMesh mesh, double quality) {
      buildFromMeshAndPoints(mesh, quality, null);
   }
   
   public void buildFromMeshAndPoints(double[] pntCoords, int[] faceIndices, 
      int numFaces, double quality, double[] addCoords, int numAddPoints) {
      buildFromMeshAndPoints(pntCoords, faceIndices, numFaces, quality, addCoords, numAddPoints, 0);
   }
   
   public void buildFromMeshAndPoints(double[] pntCoords, int[] faceIndices, 
      int numFaces, double quality, double[] addCoords, int addCoordsOffset, int numAddPoints) {
      myDimen = 3;        // assume a 3-D mesh
      
      int k = 0;
      int numf = 0;
      while (k < faceIndices.length) {
         int nc = faceIndices[k++];
         k += nc;
         numf++;
         if (k > faceIndices.length) {
            throw new IllegalArgumentException (
               "Inconsistent face index format");
         }
      }
      if (numf != numFaces) {
         throw new IllegalArgumentException (
            "Number of faces inconsistent with face indices");
      }
      if (myHandle == 0) {
         myHandle = doCreateTessellator();
      }
      int numIndices = faceIndices.length - numFaces;
      if (doBuildFromMeshAndPoints(myHandle, pntCoords, pntCoords.length/3,
         faceIndices, numFaces, numIndices, quality, addCoords, 
         addCoordsOffset, numAddPoints) < 0) {
         throw new IllegalArgumentException ("Error creating tesselation");
      }
   }

   public void buildFromMesh (
      double[] pntCoords, int[] faceIndices, int numFaces, double quality) {
      buildFromMeshAndPoints(pntCoords, faceIndices, numFaces, quality, null, 0);
   }

   public int[] getHullFaces() {
      int[] faces;
      if (myDimen <= 1) {
         faces = new int[0];
      }
      else if (myDimen == 2) {
         faces = new int[myReducedIndices.length];
         for (int k=0; k<faces.length; k++) {
            faces[k] = myReducedIndices[k];
         }
      }
      else {
         int numFaces = doGetNumHullFaces (myHandle);
         faces = new int[3*numFaces];
         if (numFaces > 0) {
            doGetHullFaces (myHandle, faces);
         }
      }
      return faces;
   }

   /** 
    * Returns the vertex indices of all the tetrahedra in this tessallation.
    * If <code>idxs</code> is the array returned by this method, then the
    * length of <code>idxs</code> is four times the number of tetrahedra, with
    * the vertex indices of the first tetrahedron given by
    * <code>idxs[0]</code>, <code>idxs[1]</code>, <code>idxs[2]</code>, and
    * <code>idxs[3]</code>, etc.
    *
    * <p>
    * The vertices are ordered such that if v0 is the first vertex,
    * v1, v2, and v3 are arranged counter-clockwise about the outward-facing
    * normal of their face.
    * @return vertices for the tetrahedra in this tessellation.
    */
   public int[] getTets() {
      if (myDimen != 3) {
         return new int[0];
      }
      int numTets = doGetNumTets (myHandle);
      int[] tets = new int[4*numTets];
      if (numTets > 0) {
         doGetTets (myHandle, tets);
      }
      return tets;
   }

   public double[] getPointCoords() {
      double[] coords;
      if (myDimen == 0) {
         coords = new double[0];
      }
      else if (myDimen == 1) {
         coords = new double[3*myReducedCoords.length];         
         for (int k=0; k<coords.length; k += 3) {
            coords[k  ] = myReducedCoords[k/3];
            coords[k+1] = 0;
            coords[k+2] = 0;
         }
      }
      else if (myDimen == 2) {
         coords = new double[myReducedCoords.length];
         for (int k=0; k<coords.length; k++) {
            coords[k] = myReducedCoords[k];
         }
      }
      else {
         int numPnts = doGetNumPoints (myHandle);
         coords = new double[3*numPnts];
         if (numPnts > 0) {
            doGetPoints (myHandle, coords);
         }
      }
      return coords;
   }

   public double[] get1DCoords() {
      if (myDimen == 1) {
         double[] coords = new double[myReducedCoords.length];
         for (int k=0; k<coords.length; k++) {
            coords[k] = myReducedCoords[k];
         }
         return coords;
      }
      else {
         return new double[0];
      }
   }

   public int[] get1DIndices() {
      if (myDimen == 1) {
         int[] indices = new int[myReducedIndices.length];
         for (int k=0; k<indices.length; k++) {
            indices[k] = myReducedIndices[k];
         }
         return indices;
      }
      else {
         return new int[0];
      }
   }

   public Point3d[] getPoints() {
      double[] coords = getPointCoords();
      int nump = coords.length/3;
      Point3d[] pnts = new Point3d[nump];
      for (int i=0; i<nump; i++) {
         pnts[i] = new Point3d (coords[3*i], coords[3*i+1], coords[3*i+2]);
      }
      return pnts;
   }

   public double getCharacteristicLength() {
      return myCharLength;
   }      

   // assume face points are givne clockwise
   private double distanceToFace (
      Point3d p0, Point3d p1, Point3d p2, Point3d pos) {

      Vector3d d1 = new Vector3d();
      Vector3d d2 = new Vector3d();
      Vector3d nrm = new Vector3d();
      d1.sub (p1, p0);
      d2.sub (p2, p0);
      nrm.cross (d2, d1);
      nrm.normalize();
      d2.sub (pos, p0);
      return nrm.dot (d2);
   }

   public boolean checkConvexHull (PrintStream ps, double tol) {
      int[] faces = getHullFaces();
      Point3d[] pnts = getPoints();
      // make sure no points are above any of the faces ....

      for (int i=0; i<pnts.length; i++) {
         Point3d pnt = pnts[i];
         for (int k=0; k<faces.length; k += 3) {
            double dist = distanceToFace (
               pnts[faces[k]], pnts[faces[k+1]], pnts[faces[k+2]], pnt);
            if (dist > tol) {
               if (ps != null)  {
                  ps.println (
                     "Point "+i+" "+dist+" above face "+
                        faces[k]+" "+faces[k+1]+" "+faces[k+2]);
                  ps.println ("p0=" + pnts[faces[k]]);
                  ps.println ("p1=" + pnts[faces[k+1]]);
                  ps.println ("p2=" + pnts[faces[k+2]]);
                  ps.println ("pnt=" + pnt);
               }
               return false;
            }
         }
      }
      return true;
   }

   public int getDimension () {
      return myDimen;
   }

   public RigidTransform3d getReductionTransform() {
      return new RigidTransform3d (myReduceX);
   }

   public void dispose () {
      if (myHandle != 0) {
         doDeleteTessellator (myHandle);
         myHandle = 0;
      }
   }

   public void finalize () {
      dispose();
   }

}
