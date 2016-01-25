/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import maspack.geometry.BVFeatureQuery.InsideQuery;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector2d;
import maspack.matrix.Vector3d;
import maspack.render.RenderableUtils;
import maspack.util.RandomGenerator;
import maspack.util.DoubleHolder;
import maspack.util.FunctionTimer;
import maspack.util.InternalErrorException;
import maspack.util.PathFinder;
import maspack.util.TestException;

public class BVFeatureQueryTest {

   PolygonalMesh myComplexMesh;
   PolygonalMesh myStarMesh;
   PolygonalMesh myTongueMesh;
   PolylineMesh myLineMesh;

   public static final double EPS = 1e-13;

   private PolygonalMesh loadMesh (String baseName) {
      String meshFileName =
         PathFinder.expand (
            "${srcdir PolygonalMesh}/sampleData/"+baseName);
      try {
         return new PolygonalMesh (new File (meshFileName));
      }
      catch (Exception e) {
         System.out.println (e);
         System.exit(1); 
      }
      return null;
   }

   public BVFeatureQueryTest() {
      myComplexMesh = loadMesh ("osCoxaeRight108v.obj");
      myStarMesh = loadMesh ("starMesh.obj");
      myTongueMesh = loadMesh ("tongueSurface.obj");
      myLineMesh = MeshFactory.createSphericalPolyline (8.0, 12, 12);
      myLineMesh.scale (1, 1, 2.5);

      RandomGenerator.setSeed (0x1234);
   }

   class NearestFaceInfo {
      Face myFace;
      double myDist;
      Point3d myNear;
      Vector2d myCoords;
      Vector3d myCoords3;

      NearestFaceInfo () {         
         myFace = null;
         myDist = -1;
         myNear = new Point3d();
         myCoords = new Vector2d();
         myCoords3 = new Vector3d();
      }

      NearestFaceInfo (NearestFaceInfo info) {
         myFace = info.myFace;
         myDist = info.myDist;
         myNear = new Point3d(info.myNear);
         myCoords = new Vector2d(info.myCoords);
         myCoords3 = new Vector3d(info.myCoords3);
      }

      double computeDistanceToPoint (
         Face face, Point3d pnt, TriangleIntersector intersector) {
         HalfEdge he0 = face.firstHalfEdge();
         HalfEdge he = he0;
         Point3d p0 = he.head.pnt;
         he = he.getNext();
         Point3d p1 = he.head.pnt;
         he = he.getNext();
         Point3d p2 = he.head.pnt;
         if (he.getNext() != he0) {
            throw new InternalErrorException ("Face is not triangular");
         }
         myFace = face;
         myDist = intersector.nearestpoint (p0, p1, p2, pnt, myNear, myCoords);
         return myDist;
      }

      double computeDistanceToRay (
         Face face, Point3d pnt, Vector3d dir, TriangleIntersector intersector) {
         HalfEdge he0 = face.firstHalfEdge();
         HalfEdge he = he0;
         Point3d p0 = he.head.pnt;
         he = he.getNext();
         Point3d p1 = he.head.pnt;
         he = he.getNext();
         Point3d p2 = he.head.pnt;
         if (he.getNext() != he0) {
            throw new InternalErrorException ("Face is not triangular");
         }
         myFace = face;
         Vector3d coords = new Vector3d();
         int rcode = intersector.intersect (p0, p1, p2, pnt, dir, coords);
         if (rcode == 0) {
            myDist = Double.POSITIVE_INFINITY;
         }
         else if (rcode == 1) {
            myDist = coords.x;
            myCoords3.set (coords);
            Vector2d uv = new Vector2d();
            uv.x = coords.y;
            uv.y = coords.z;
            face.computePoint (myNear, uv);
         }
         else {
            throw new InternalErrorException (
               "Triangle-ray intersector returns unexpected code " + rcode);
         }
         return myDist;
      }

      boolean lineFaceIsContained (List<NearestFaceInfo> list, double tol) {
         for (NearestFaceInfo ninfo : list) {
            if (ninfo.myFace == myFace) {
               if (!myNear.epsilonEquals (ninfo.myNear, tol)) {
                  throw new TestException (
                     "Nearest face has different nearestPoint: \n" +
                     myNear + "\nexpected:\n" + ninfo.myNear);
               }
               if (!myCoords3.epsilonEquals (ninfo.myCoords3, EPS)) {
                  throw new TestException (
                     "Nearest face has different coords: \n" +
                     myCoords3 + "\nexpected:\n" + ninfo.myCoords3);
               }
               return true;
            }
         }
         return false;
      }

      boolean pointFaceIsContained (List<NearestFaceInfo> list, double tol) {
         for (NearestFaceInfo ninfo : list) {
            if (ninfo.myFace == myFace) {
               if (!myNear.epsilonEquals (ninfo.myNear, tol)) {
                  throw new TestException (
                     "Nearest face has different nearestPoint: \n" +
                     myNear + "\nexpected:\n" + ninfo.myNear);
               }
               if (!myCoords.epsilonEquals (ninfo.myCoords, EPS)) {
                  throw new TestException (
                     "Nearest face has different coords: \n" +
                     myCoords + "\nexpected:\n" + ninfo.myCoords);
               }
               return true;
            }
         }
         return false;
      }
   }   

   class NearestEdgeInfo {
      Boundable myEdge;
      double myDist;
      Point3d myNear;
      DoubleHolder myCoord;

      NearestEdgeInfo () {         
         myEdge = null;
         myDist = -1;
         myNear = new Point3d();
         myCoord = new DoubleHolder();
      }

      NearestEdgeInfo (NearestEdgeInfo info) {
         myEdge = info.myEdge;
         myDist = info.myDist;
         myNear = new Point3d(info.myNear);
         myCoord = new DoubleHolder(info.myCoord.value);
      }

      double computeDistanceToPoint (HalfEdge he, Point3d pnt) {
         myEdge = he;
         myDist = computeDistance (he.tail.pnt, he.head.pnt, pnt);
         return myDist;
      }

      double computeDistanceToPoint (LineSegment seg, Point3d pnt) {
         myEdge = seg;
         myDist = computeDistance (seg.myVtx0.pnt, seg.myVtx1.pnt, pnt);
         return myDist;
      }

      double computeDistance (Point3d p0, Point3d p1, Point3d pnt) {
         Vector3d del = new Vector3d();
         Vector3d vec = new Vector3d();

         vec.sub (p1, p0);
         del.sub (pnt, p1);
         if (del.dot (vec) >= 0) {
            myCoord.value = 1;
            myNear.set (p1);
            return del.norm();
         }
         del.sub (pnt, p0);
         if (del.dot (vec) <= 0) {
            myCoord.value = 0;
            myNear.set (p0);
            return del.norm();
         }
         else {
            double vmagSqr = vec.normSquared();
            double dmagSqr = del.normSquared();
            double s = del.dot (vec)/vmagSqr;
            myCoord.value = s;
            myNear.combine (1-s, p0, s, p1);
            return myNear.distance (pnt);
         }
      }

      boolean edgeFaceIsContained (List<NearestEdgeInfo> list, double tol) {
         for (NearestEdgeInfo einfo : list) {
            if (edgesEqual (einfo.myEdge, myEdge)) {
               if (!myNear.epsilonEquals (einfo.myNear, tol)) {
                  throw new TestException (
                     "Nearest edge has different nearestPoint: \n" +
                     myNear + "\nexpected:\n" + einfo.myNear);
               }
               if (Math.abs (myCoord.value-einfo.myCoord.value) > EPS) {
                  throw new TestException (
                     "Nearest edge has different coords: \n" +
                     myCoord.value + "\nexpected:\n" + einfo.myCoord.value);
               }
               return true;
            }
         }
         return false;
      }

      boolean edgesEqual (Boundable edge0, Boundable edge1) {
         if (edge0 instanceof LineSegment) {
            LineSegment seg0 = (LineSegment)edge0;
            if (edge1 instanceof LineSegment) {
               LineSegment seg1 = (LineSegment)edge1;
               return (seg0.myVtx0 == seg1.myVtx0 && seg0.myVtx1 == seg1.myVtx1);
            }
            else {
               return false;
            }
         }
         else if (edge0 instanceof HalfEdge) {
            return edge0 == edge1;
         }
         else {
            return false;
         }
      }         

      String edgeString () {
         Vertex3d v0;
         Vertex3d v1;
         if (myEdge instanceof LineSegment) {
            LineSegment seg = (LineSegment)myEdge;
            v0 = seg.myVtx0;
            v1 = seg.myVtx1;
         }
         else if (myEdge instanceof HalfEdge) {
            HalfEdge he = (HalfEdge)myEdge;
            v0 = he.tail;
            v1 = he.head;
         }
         else {
            return null;
         }
         return "("+v0.getIndex()+"-"+v1.getIndex()+")";
      }
   }


   boolean debug = false;

   private ArrayList<NearestFaceInfo> getNearestFacesToPoint (
      PolygonalMesh mesh, Point3d pnt, double tol) {

      Point3d loc = new Point3d();
      loc.inverseTransform (mesh.getMeshToWorld(), pnt);
      
      ArrayList<Face> faces = mesh.getFaces();
      NearestFaceInfo[] faceInfo = new NearestFaceInfo[faces.size()];
      TriangleIntersector intersector = new TriangleIntersector();
      double mind = Double.POSITIVE_INFINITY;
      for (int i=0; i<faces.size(); i++) {
         NearestFaceInfo ninfo = new NearestFaceInfo();
         faceInfo[i] = ninfo;
         double d = ninfo.computeDistanceToPoint (faces.get(i), loc, intersector);
         ninfo.myNear.transform (mesh.getMeshToWorld());
         if (d < mind) {
            mind = d;
         }
      }
      ArrayList<NearestFaceInfo> nearestFaces =
         new ArrayList<NearestFaceInfo>();
      for (int i=0; i<faces.size(); i++) {
         if (Math.abs(faceInfo[i].myDist-mind) < tol) {
            nearestFaces.add (faceInfo[i]);
         }
      }
      return nearestFaces;
   }

   private ArrayList<NearestFaceInfo> getNearestFacesToRay (
      PolygonalMesh mesh, Point3d pnt, Vector3d dir, double tol) {

      Point3d lpnt = new Point3d();
      Vector3d ldir = new Vector3d();
      lpnt.inverseTransform (mesh.getMeshToWorld(), pnt);
      ldir.inverseTransform (mesh.getMeshToWorld(), dir);
      
      ArrayList<Face> faces = mesh.getFaces();
      NearestFaceInfo[] faceInfo = new NearestFaceInfo[faces.size()];
      TriangleIntersector intersector = new TriangleIntersector();
      double mind = Double.POSITIVE_INFINITY;
      for (int i=0; i<faces.size(); i++) {
         NearestFaceInfo ninfo = new NearestFaceInfo();
         faceInfo[i] = ninfo;
         double d = ninfo.computeDistanceToRay (
            faces.get(i), lpnt, ldir, intersector);
         ninfo.myNear.transform (mesh.getMeshToWorld());
         if (d >= 0 && d < Double.POSITIVE_INFINITY) {
            if (d < mind) {
               mind = d;
            }
         }
      }
      ArrayList<NearestFaceInfo> nearestFaces = new ArrayList<NearestFaceInfo>();
      if (mind < Double.POSITIVE_INFINITY) {
         for (int i=0; i<faces.size(); i++) {
            double d = faceInfo[i].myDist;
            if (d >= 0 && d < Double.POSITIVE_INFINITY) {
               if (Math.abs(d-mind) < tol) {
                  nearestFaces.add (faceInfo[i]);
               }
            }
         }
      }
      return nearestFaces;
   }

   private ArrayList<NearestEdgeInfo> getNearestEdgesToPoint (
      MeshBase mesh, Point3d pnt, double tol) {

      Point3d loc = new Point3d();
      loc.inverseTransform (mesh.getMeshToWorld(), pnt);

      NearestEdgeInfo[] edgeInfo;
      double mind = Double.POSITIVE_INFINITY;

      if (mesh instanceof PolygonalMesh) {
         ArrayList<Face> faces = ((PolygonalMesh)mesh).getFaces();
         int numEdges = 0;
         for (int i=0; i<faces.size(); i++) {
            numEdges += faces.get(i).numEdges();
         }
         edgeInfo = new NearestEdgeInfo[numEdges];
         int k = 0;
         for (int i=0; i<faces.size(); i++) {
            Face face = faces.get(i);
            HalfEdge he0 = face.firstHalfEdge();
            HalfEdge he = he0;
            do {
               NearestEdgeInfo einfo = new NearestEdgeInfo();
               double d = einfo.computeDistanceToPoint (he, loc);
               einfo.myNear.transform (mesh.getMeshToWorld());
               if (d < mind) {
                  mind = d;
               }
               edgeInfo[k++] = einfo;
               he = he.getNext();
            }
            while (he != he0);
         }
      }
      else if (mesh instanceof PolylineMesh) {
         int numEdges = 0;
         ArrayList<Polyline> lines = ((PolylineMesh)mesh).getLines();
         for (Polyline line : lines) {
            numEdges += line.numVertices()-1;
         }
         edgeInfo = new NearestEdgeInfo[numEdges];
         int k = 0;
         for (int i=0; i<lines.size(); i++) {
            Polyline line = lines.get(i);
            Vertex3d[] vtxs = line.getVertices();
            for (int j=0; j<line.numVertices()-1; j++) {
               NearestEdgeInfo einfo = new NearestEdgeInfo();
               double d = einfo.computeDistanceToPoint (
                  new LineSegment (vtxs[j], vtxs[j+1]), loc);
               einfo.myNear.transform (mesh.getMeshToWorld());
               if (d < mind) {
                  mind = d;
               }
               edgeInfo[k++] = einfo;               
            }            
         }
      }
      else {
         // nothing - no edges will be FOUND
         edgeInfo = new NearestEdgeInfo[0];
      }
      ArrayList<NearestEdgeInfo> nearestEdges =
         new ArrayList<NearestEdgeInfo>();
      for (int i=0; i<edgeInfo.length; i++) {
         if (Math.abs(edgeInfo[i].myDist-mind) < tol) {
            nearestEdges.add (edgeInfo[i]);
         }
      }
      return nearestEdges;
   }

   private ArrayList<Vertex3d> getNearestVerticesToPoint (
      MeshBase mesh, Point3d pnt) {

      Point3d loc = new Point3d();
      loc.inverseTransform (mesh.getMeshToWorld(), pnt);

      ArrayList<Vertex3d> verts = mesh.getVertices();
      double mind = Double.POSITIVE_INFINITY;
      for (int i=0; i<verts.size(); i++) {
         double d = verts.get(i).pnt.distance (loc);
         if (d < mind) {
            mind = d;
         }
      }
      ArrayList<Vertex3d> nearestVerts = new ArrayList<Vertex3d>();
      for (int i=0; i<verts.size(); i++) {
         double d = verts.get(i).pnt.distance (loc);
         if (d == mind) {
            nearestVerts.add (verts.get(i));
         }
      }      
      return nearestVerts;
   }

   // private void getNearestFaceFromObbtree (
   //    NearestFaceInfo ninfo, OBBTree obbTree, Point3d pnt) {
   //    TriangleIntersector ti = new TriangleIntersector();      
   //    ninfo.myFace = obbTree.nearestFace (
   //       pnt, null, ninfo.myNear, ninfo.myCoords, ti);
   // }

   // private void getNearestFaceIntersectedByRayFromObbtree (
   //    NearestFaceInfo ninfo, OBBTree obbTree, Point3d pnt, Vector3d dir) {
   //    //TriangleIntersector ti = new TriangleIntersector();   
   //    BVFeatureQuery query = new BVFeatureQuery();
   //    Vector3d coords = new Vector3d();
   //    //ninfo.myFace = obbTree.intersect (pnt, dir, coords, ti);
   //    ninfo.myFace = query.nearestFaceAlongRay (
   //       null, coords, obbTree, pnt, dir);
   //    if (ninfo.myFace != null) {
   //       ninfo.myDist = coords.x;
   //       ninfo.myCoords.x = coords.y;
   //       ninfo.myCoords.y = coords.z;
   //       ninfo.myFace.computePoint (ninfo.myNear, ninfo.myCoords);
   //    }
   // }

   private void nearestFaceTest (
      PolygonalMesh mesh, BVTree bvh, 
      RigidTransform3d X, Point3d center, double diameter) {

      BVFeatureQuery query = new BVFeatureQuery();

      mesh.setMeshToWorld (X);
      bvh.setBvhToWorld (X);

      int testcnt = 1000;
      Point3d pnt = new Point3d();
      Vector3d dir = new Vector3d();
      NearestFaceInfo ninfo = new NearestFaceInfo();
      NearestFaceInfo rinfo = new NearestFaceInfo();
      double tol = EPS*(center.norm()+diameter);

      for (int i=0; i<testcnt; i++) {
         pnt.setRandom();
         pnt.scale (2*diameter);
         pnt.add (center);
         pnt.transform (X, pnt);
         dir.setRandom();
         dir.normalize();

         ArrayList<NearestFaceInfo> nearestFaces =
            getNearestFacesToPoint (mesh, pnt, tol);
         ninfo.myFace = query.nearestFaceToPoint (
            ninfo.myNear, ninfo.myCoords, bvh, pnt);
         if (!ninfo.pointFaceIsContained (nearestFaces, tol)) {
            System.out.println ("Computed face:");
            System.out.println (
               "Face "+ ninfo.myFace.getIndex() + " near: " +
               ninfo.myNear.toString ("%10.6f") + ", coords " + ninfo.myCoords);
            System.out.println ("Possible faces:");
            for (int k=0; k<nearestFaces.size(); k++) {
               NearestFaceInfo ni = nearestFaces.get(k);
               System.out.println (
                  "Face "+ ni.myFace.getIndex() + " near: " +
                  ni.myNear.toString ("%10.6f")); 
            }
            throw new TestException (
               "Face "+ninfo.myFace.getIndex()+
               " computed as nearest does not match brute force calculation");
         } 

         ArrayList<NearestFaceInfo> nearestRayFaces =
            getNearestFacesToRay (mesh, pnt, dir, tol);

         rinfo.myFace = query.nearestFaceAlongRay (
            rinfo.myNear, rinfo.myCoords3, bvh, pnt, dir);
         if (rinfo.myFace == null) {
            if (nearestRayFaces.size() != 0) {
               System.out.println ("Possible faces:");
               for (int k=0; k<nearestRayFaces.size(); k++) {
                  NearestFaceInfo ni = nearestRayFaces.get(k);
                  System.out.printf (
                     " Face %d dist=%g near=%s\n", 
                     ni.myFace.getIndex(), ni.myDist,
                     ni.myNear.toString ("%10.6f")); 
               }
               throw new TestException (
                  "nearestFaceIntersectedByRay returned null; "+
                  "brute force did not");
            }
         }
         else if (!rinfo.lineFaceIsContained (nearestRayFaces, tol)) {
            System.out.println ("Computed face:");
            System.out.println (
               "Face "+ rinfo.myFace.getIndex() +
               " near: " + rinfo.myNear.toString ("%10.6f") +
               ", coords " + rinfo.myCoords3);
            System.out.println ("Possible faces:");
            for (int k=0; k<nearestRayFaces.size(); k++) {
               NearestFaceInfo ni = nearestRayFaces.get(k);
               System.out.println (
                  "Face "+ ni.myFace.getIndex() +
                  " near: " + ni.myNear.toString ("%10.6f") + 
                  ", coords " + ni.myCoords3);
            }
            throw new TestException (
               "Face "+rinfo.myFace.getIndex()+
               " computed as nearest does not match brute force calculation");
         } 

         // NearestFaceInfo oinfo = new NearestFaceInfo();        
         // if (bvh instanceof OBBTree) {
         //    OBBTree obbTree = (OBBTree)bvh;
         //    getNearestFaceFromObbtree (oinfo, obbTree, pnt);
         //    if (oinfo.myFace != ninfo.myFace) {
         //       if (!ninfo.myNear.epsilonEquals (oinfo.myNear, tol)) {
         //          System.out.println ("Query and OBBtree results differ:");
         //          System.out.println (
         //             "Face "+ninfo.myFace.getIndex()+
         //             ", near="+ ninfo.myNear.toString ("%10.6f"));
         //          System.out.println (
         //             "Face "+oinfo.myFace.getIndex()+
         //             ", near="+ oinfo.myNear.toString ("%10.6f"));
         //       }
         //    }
         //    if (!oinfo.pointFaceIsContained (nearestFaces, tol)) {
         //       System.out.println ("Computed face from Obbtree:");
         //       System.out.println (
         //          "Face "+ oinfo.myFace.getIndex() + " near: " +
         //          oinfo.myNear.toString ("%10.6f"));             
         //       System.out.println ("Possible faces:");
         //       for (int k=0; k<nearestFaces.size(); k++) {
         //          NearestFaceInfo ni = nearestFaces.get(k);
         //          System.out.println (
         //             "Face "+ ni.myFace.getIndex() + " near: " +
         //             ni.myNear.toString ("%10.6f")); 
         //       }
         //       throw new TestException (
         //          "Face "+oinfo.myFace.getIndex()+
         //          " computed as nearest does not match brute force calculation");
         //    }
         //    getNearestFaceIntersectedByRayFromObbtree (oinfo, obbTree, pnt, dir);
         //    if ((oinfo.myFace==null) != (rinfo.myFace==null)) {
         //       if (oinfo.myFace!=null) {
         //          System.out.println ("obbTree dist=" + oinfo.myDist);
         //       }
         //       else if (rinfo.myFace!=null) {
         //          System.out.println ("query dist=" + rinfo.myDist);
         //       }
         //       throw new TestException (
         //          "Different results computing nearest face to ray via query "+
         //          "(face="+rinfo.myFace+") and OBBTree (face="+oinfo.myFace+")");
         //    }
         //    if (rinfo.myFace != oinfo.myFace) {
         //       if (!rinfo.myNear.epsilonEquals (oinfo.myNear, tol)) {
         //          System.out.println ("Query and OBBtree results differ:");
         //          System.out.println (
         //             "Face "+rinfo.myFace.getIndex()+
         //             ", near="+ rinfo.myNear.toString ("%10.6f"));
         //          System.out.println (
         //             "Face "+oinfo.myFace.getIndex()+
         //             ", near="+ oinfo.myNear.toString ("%10.6f"));
         //       }
         //    }
         // }
      }
   }

   private void nearestVertexAndEdge (
      MeshBase mesh, BVTree bvh, 
      RigidTransform3d X, Point3d center, double diameter) {

      BVFeatureQuery query = new BVFeatureQuery();
      
      mesh.setMeshToWorld (X);
      bvh.setBvhToWorld (X);

      int testcnt = 1000;
      Point3d pnt = new Point3d();
      Vector3d dir = new Vector3d();
      NearestEdgeInfo einfo = new NearestEdgeInfo();
      double tol = EPS*(center.norm()+diameter);

      for (int i=0; i<testcnt; i++) {
         pnt.setRandom();
         pnt.scale (2*diameter);
         pnt.add (center);
         pnt.transform (X, pnt);
         dir.setRandom();
         dir.normalize();

         ArrayList<Vertex3d> nearestVertices =
            getNearestVerticesToPoint (mesh, pnt);
         Vertex3d vtx = query.nearestVertexToPoint (bvh, pnt);
         if (!nearestVertices.contains (vtx)) {
            System.out.println ("Computed vertex:");
            System.out.println (
               "Vertex "+ vtx.getIndex());
            System.out.println ("Possible vertices:");
            for (int k=0; k<nearestVertices.size(); k++) {
               System.out.println (
                  "Vertex "+ nearestVertices.get(i).getIndex());
            }
            throw new TestException (
               "Vertex "+vtx.getIndex()+
               " computed as nearest does not match brute force calculation");
         } 

         ArrayList<NearestEdgeInfo> nearestEdges =
            getNearestEdgesToPoint (mesh, pnt, tol);

         einfo.myEdge = query.nearestEdgeToPoint (
            einfo.myNear, einfo.myCoord, bvh, pnt);
         if (einfo.myEdge == null) {
            if (nearestEdges.size() > 0) {
               throw new TestException (
                  "No nearest edge found, but brute force discovered " +
                  nearestEdges.size());
            }
         }
         else {
            if (!einfo.edgeFaceIsContained (nearestEdges, tol)) {
               System.out.println ("Computed face:");
               System.out.println ("Edge "+ einfo.edgeString() +
                                   " near: " + einfo.myNear.toString ("%10.6f") +
                                   ", coord " + einfo.myCoord.value +
                                   ", d=" + einfo.myDist);
               System.out.println ("Possible edges:");
               for (int k=0; k<nearestEdges.size(); k++) {
                  NearestEdgeInfo ei = nearestEdges.get(k);
                  System.out.println (
                     "Edge "+ ei.edgeString() +
                     " near: " + ei.myNear.toString ("%10.6f") + 
                     ", coords " + ei.myCoord.value +
                     ", d=" + ei.myDist);
               }
               throw new TestException (
                  "Edge "+einfo.edgeString()+
                  " computed as nearest does not match brute force calculation");
            } 
         }
      }
   }

   private void nearestFaceTest (PolygonalMesh mesh) {

      // //NearestFeatureQuery query = new NearestFeatureQuery();
      // double inf = Double.POSITIVE_INFINITY;
      // Point3d max = new Point3d (-inf, -inf, -inf);
      // Point3d min = new Point3d ( inf,  inf,  inf);
      // Point3d center = new Point3d();
      // Point3d widths = new Point3d();
      // mesh.updateBounds (min, max);
      // center.add (min, max);
      // center.scale (0.5);
      // widths.sub (max, min);
      // widths.scale (0.5);
      // double diameter = max.distance (min);

      RigidTransform3d X = new RigidTransform3d();

      OBBTree obbTree = new OBBTree (mesh, 2);
      AABBTree aabbTree = new AABBTree (mesh);

      Point3d center = new Point3d();
      double diameter = 2*aabbTree.getRadius();
      aabbTree.getCenter (center);

      nearestFaceTest (mesh, obbTree, X, center, diameter);
      nearestFaceTest (mesh, aabbTree, X, center, diameter);
      X.setRandom();
      nearestFaceTest (mesh, obbTree, X, center, diameter);
      nearestFaceTest (mesh, aabbTree, X, center, diameter);
      X.setRandom();
      nearestFaceTest (mesh, obbTree, X, center, diameter);
      nearestFaceTest (mesh, aabbTree, X, center, diameter);
      X.setRandom();
      nearestFaceTest (mesh, obbTree, X, center, diameter);
      nearestFaceTest (mesh, aabbTree, X, center, diameter);
   }

   private void nearestFaceTiming (PolygonalMesh mesh) {

      RigidTransform3d X = new RigidTransform3d();

      OBBTree obbTree = new OBBTree (mesh, 2);
      AABBTree aabbTree = new AABBTree (mesh);

      Point3d center = new Point3d();
      double diameter = 2*aabbTree.getRadius();
      aabbTree.getCenter (center);

      X.setRandom();

      BVFeatureQuery query = new BVFeatureQuery();

      mesh.setMeshToWorld (X);
      obbTree.setBvhToWorld (X);
      aabbTree.setBvhToWorld (X);

      int numcases = 100;
      int timingcnt = 1000;
      Point3d pnt = new Point3d();
      Vector3d dir = new Vector3d();

      Point3d near = new Point3d();
      Vector2d coords = new Vector2d();
      Vector3d duv = new Vector3d();

      FunctionTimer obbFaceTimer = new FunctionTimer();
      FunctionTimer aabbFaceTimer = new FunctionTimer();
      //FunctionTimer oldFaceTimer = new FunctionTimer();
      FunctionTimer obbRayTimer = new FunctionTimer();
      FunctionTimer aabbRayTimer = new FunctionTimer();
      //FunctionTimer oldRayTimer = new FunctionTimer();
      TriangleIntersector ti = new TriangleIntersector();

      for (int i=0; i<numcases; i++) {
         pnt.setRandom();
         pnt.scale (2*diameter);
         pnt.add (center);
         pnt.transform (X, pnt);
         dir.setRandom();
         dir.normalize();

         obbFaceTimer.restart();
         for (int j=0; j<timingcnt; j++) {
            query.nearestFaceToPoint (near, coords, obbTree, pnt);
         }
         obbFaceTimer.stop();
         aabbFaceTimer.restart();
         for (int j=0; j<timingcnt; j++) {
            query.nearestFaceToPoint (near, coords, aabbTree, pnt);
         }
         aabbFaceTimer.stop();
         // oldFaceTimer.restart();
         // for (int j=0; j<timingcnt; j++) {
         //    obbTree.nearestFace (pnt, null, near, coords, ti);
         // }
         // oldFaceTimer.stop();

         obbRayTimer.restart();
         for (int j=0; j<timingcnt; j++) {
            query.nearestFaceAlongRay (near, duv, obbTree, center, dir);
         }
         obbRayTimer.stop();
         aabbRayTimer.restart();
         for (int j=0; j<timingcnt; j++) {
            query.nearestFaceAlongRay (near, duv, aabbTree, center, dir);
         }
         aabbRayTimer.stop();
         // oldRayTimer.restart();
         // for (int j=0; j<timingcnt; j++) {
         //    obbTree.intersect (center, dir, duv, ti);
         // }
         // oldRayTimer.stop();
      }
      int cnt = numcases*timingcnt;
      System.out.println (
         "nearestFace with OBB: " + obbFaceTimer.result(cnt));
      System.out.println (
         "nearestFace with AABB: " + aabbFaceTimer.result(cnt));
      // System.out.println (
      //    "nearestFace with old OBB: " + oldFaceTimer.result(cnt));
      System.out.println (
         "nearestRay with OBB: " + obbRayTimer.result(cnt));
      System.out.println (
         "nearestRay with AABB: " + aabbRayTimer.result(cnt));
      // System.out.println (
      //    "nearestRay with old OBB: " + oldRayTimer.result(cnt));
   }

   private void pointInsideTiming (PolygonalMesh mesh) {

      RigidTransform3d X = new RigidTransform3d();

      OBBTree obbTree = new OBBTree (mesh, 2);
      AABBTree aabbTree = new AABBTree (mesh);

      Point3d center = new Point3d();
      double diameter = 2*aabbTree.getRadius();
      aabbTree.getCenter (center);

      X.setRandom();

      BVFeatureQuery query = new BVFeatureQuery();

      mesh.setMeshToWorld (X);
      obbTree.setBvhToWorld (X);
      aabbTree.setBvhToWorld (X);

      int numcases = 100;
      int timingcnt = 1;
      Point3d pnt = new Point3d();

      FunctionTimer obbOrientedTimer = new FunctionTimer();
      FunctionTimer aabbOrientedTimer = new FunctionTimer();
      FunctionTimer obbInsideTimer = new FunctionTimer();
      FunctionTimer aabbInsideTimer = new FunctionTimer();

      for (int i=0; i<numcases; i++) {
         
         Random rand = RandomGenerator.get();
         int vidx = rand.nextInt(mesh.numVertices());
         pnt.setRandom();
         pnt.scale (0.001*diameter);
         pnt.add (mesh.getVertex(vidx).pnt);
         pnt.transform (X, pnt);

         boolean inside;
         obbOrientedTimer.restart();
         for (int j=0; j<timingcnt; j++) {
            inside = query.isInsideOrientedMesh (obbTree, pnt, 0);
         }
         obbOrientedTimer.stop();
         aabbOrientedTimer.restart();
         for (int j=0; j<timingcnt; j++) {
            query.isInsideOrientedMesh (aabbTree, pnt, 0);
         }
         aabbOrientedTimer.stop();

         obbInsideTimer.restart();
         for (int j=0; j<timingcnt; j++) {
            InsideQuery res;
            res = query.isInsideMesh (mesh, obbTree, pnt, 0);
         }
         obbInsideTimer.stop();
         aabbInsideTimer.restart();
         for (int j=0; j<timingcnt; j++) {
            query.isInsideMesh (mesh, aabbTree, pnt, 0);
         }
         aabbInsideTimer.stop();
      }
      int cnt = numcases*timingcnt;
      System.out.println (
         "isInsideOriented with OBB: " + obbOrientedTimer.result(cnt));
      System.out.println (
         "isInsideOriented with AABB: " + aabbOrientedTimer.result(cnt));
      System.out.println (
         "isInside with OBB: " + obbInsideTimer.result(cnt));
      System.out.println (
         "isInside with AABB: " + aabbInsideTimer.result(cnt));
   }

   private boolean pointInsideCheck (
      Point3d pnt, double tol, PolygonalMesh mesh, BVTree bvh,
      BVFeatureQuery query, TriangleIntersector intersector) {

      boolean isInside = query.isInsideOrientedMesh (bvh, pnt, tol);
      boolean isInsideCheck;
      
      // boolean isInsideCheck = obbTree.isInside (pnt, intersector, tol);

      // if (isInside != isInsideCheck) {
      //    throw new TestException (
      //       "query.isInsideMesh returned "+isInside+
      //       ", OBBTree.isInside returned "+isInsideCheck);
      // }

      InsideQuery status = query.isInsideMesh (mesh, bvh, pnt, tol);
      // int statusCheck = obbTree.isInsideRayTest (pnt, intersector, tol, 100);
      // if (status != statusCheck) {
      //    System.out.println ("pnt=" + pnt);
      //    throw new TestException (
      //       "query.isInsideMeshRayCast returned  "+status+
      //          ", OBB.isInsideRayTest returned "+statusCheck);
      // }
      if (status == InsideQuery.UNSURE) {
         System.out.println ("isInsideMesh() did not converge");
         return false;
      }
      else {
         isInsideCheck = (status == InsideQuery.INSIDE);
         if (isInside != isInsideCheck) {
            System.out.println ("pnt=" + pnt);
            System.out.println ("last case: " + query.lastCase);
            throw new TestException (
               "query.isInsideMesh returned  "+isInside+
               ", OBB.isInsideRayTest returned "+isInsideCheck);
         }
      }
      return isInside;
   }

   boolean printInsideOutsideCounts = false;

   private void pointInsideTest (
      PolygonalMesh mesh, BVTree bvh, RigidTransform3d X,
      double radius, Point3d center) {

      int numtrials = 10000;
      Point3d pnt = new Point3d();
      double tol = EPS*(center.norm()+radius);

      TriangleIntersector intersector = new TriangleIntersector();
      BVFeatureQuery query = new BVFeatureQuery();

      mesh.setMeshToWorld (X);
      bvh.setBvhToWorld (X);

      tol = 0;

      int numInside = 0;
      for (int i=0; i<numtrials; i++) {
         pnt.setRandom();
         pnt.scale (2*radius);
         pnt.add (center);
         pnt.transform (X, pnt);

         if (pointInsideCheck (
                pnt, tol, mesh, bvh, query, intersector)) {
            numInside++;

         }
      }
      double avgEdgeLength = mesh.computeAverageEdgeLength();

      int numv = mesh.numVertices();
      for (int i=0; i<numtrials/numv; i++) {
         for (int j=0; j<numv; j++) {
            pnt.setRandom();
            pnt.scale (avgEdgeLength/10);
            pnt.add (mesh.getVertex(j).pnt);
            pnt.transform (X, pnt);

            if (pointInsideCheck (
                   pnt, tol, mesh, bvh, query, intersector)) {
               numInside++;
            }
         }
      }

      if (printInsideOutsideCounts) {
         System.out.println ("numInside=" + numInside);
         System.out.println ("numOutside=" + (numtrials-numInside));
         System.out.println ("cases: V=" + query.numVertexCases +
                             " E=" + query.numEdgeCases +
                             " F=" + query.numFaceCases);
      }
   }

   private void pointInsideTest (PolygonalMesh mesh) {

      //NearestFeatureQuery query = new NearestFeatureQuery();
      Point3d center = new Point3d();
      double radius = RenderableUtils.getRadiusAndCenter (center, mesh);

      RigidTransform3d X = new RigidTransform3d();

      OBBTree obbTree = new OBBTree (mesh);
      AABBTree aabbTree = new AABBTree (mesh);

      pointInsideTest (mesh, obbTree, X, radius, center);
      pointInsideTest (mesh, aabbTree, X, radius, center);
      X.setRandom();
      pointInsideTest (mesh, obbTree, X, radius, center);
      pointInsideTest (mesh, aabbTree, X, radius, center);
   }

   private void pointInsideTests () {

      pointInsideTest (MeshFactory.createBox (1.0, 1.5, 2.0));
      pointInsideTest (MeshFactory.createSphere (1.0, 7));
      pointInsideTest (myComplexMesh);
      pointInsideTest (myTongueMesh);
      pointInsideTest (myStarMesh);

   }

   private void nearestVertexAndEdgeTest (MeshBase mesh) {

      //NearestFeatureQuery query = new NearestFeatureQuery();
      double inf = Double.POSITIVE_INFINITY;
      Point3d max = new Point3d (-inf, -inf, -inf);
      Point3d min = new Point3d ( inf,  inf,  inf);
      Point3d center = new Point3d();
      Point3d widths = new Point3d();
      mesh.updateBounds (min, max);
      center.add (min, max);
      center.scale (0.5);
      widths.sub (max, min);
      widths.scale (0.5);
      double diameter = max.distance (min);

      RigidTransform3d X = new RigidTransform3d();

      OBBTree obbTree = new OBBTree (mesh, 2);
      AABBTree aabbTree = new AABBTree (mesh, 2, 0.001*diameter);

      nearestVertexAndEdge (mesh, obbTree, X, center, diameter);
      nearestVertexAndEdge (mesh, aabbTree, X, center, diameter);
      X.setRandom();
      nearestVertexAndEdge (mesh, obbTree, X, center, diameter);
      //aabbTree.print();
      nearestVertexAndEdge (mesh, aabbTree, X, center, diameter);
      X.setRandom();
      nearestVertexAndEdge (mesh, obbTree, X, center, diameter);
      nearestVertexAndEdge (mesh, aabbTree, X, center, diameter);
      X.setRandom();
      nearestVertexAndEdge (mesh, obbTree, X, center, diameter);
      nearestVertexAndEdge (mesh, aabbTree, X, center, diameter);
   }

   public void test() {
      nearestFaceTest (MeshFactory.createBox (1.0, 1.5, 2.0));
      nearestFaceTest (MeshFactory.createSphere (1.0, 7));
      nearestFaceTest (myComplexMesh);

      nearestVertexAndEdgeTest (MeshFactory.createBox (1.0, 1.5, 2.0));
      nearestVertexAndEdgeTest (MeshFactory.createSphere (1.0, 7));
      nearestVertexAndEdgeTest (myComplexMesh);
      nearestVertexAndEdgeTest (MeshFactory.createRandomPointMesh (100, 5));
      nearestVertexAndEdgeTest (myLineMesh);
      pointInsideTests();
   }

   public void timing() {
      nearestFaceTiming (myComplexMesh);
      pointInsideTiming (myComplexMesh);
   }

   // Results of time trials compared with old OBBTree code:
   //
   // nearestFace with OBB: 3.50160043 usec
   // nearestFace with AABB: 3.48696832 usec
   // nearestFace with old OBB: 3.78526724 usec
   // nearestRay with OBB: 1.06221247 usec
   // nearestRay with AABB: 1.53430211 usec
   // nearestRay with old OBB: 1.64526574 usec

   public static void main (String[] args) {

      BVFeatureQueryTest tester = new BVFeatureQueryTest();
      boolean doTiming = false;
      boolean doTesting = true;

      for (int i=0; i<args.length; i++) {
         if (args[i].equals ("-timing")) {
            doTiming = true;
            doTesting = false;
         }
         else {
            System.out.println (
               "Usage: java "+tester.getClass().getName()+" [-timing]");
            System.exit(1);
         }
      }      

      try {
         if (doTesting) {
            tester.test();
            System.out.println ("\nPassed\n");
         }
         if (doTiming) {
            tester.timing();
         }
      }
      catch (Exception e) {
         e.printStackTrace(); 
         System.exit(1); 
      }

   }

}
