package maspack.collision;

import maspack.matrix.*;
import maspack.collision.IntersectionPoint;
import maspack.geometry.*;
import maspack.util.*;

import java.util.*;

/**
 * A collider that determines the intersection between two meshes by first
 * identifying and tracing the intersection contours between the meshes,
 * and then using these to determine the interpenetration regions.
 */
public class SurfaceMeshIntersector {

   private static final double EPS = 1e-10;

   PolygonalMesh myMesh0;  // first mesh to be intersected
   PolygonalMesh myMesh1;  // second mesh to be intersected

   /*
    * Temporary reference to the list of generated contours, used to
    * find the index of contour, for debugging purposes only.
    */
   ArrayList<IntersectionContour> myContours;

   // returns the index of contour with respect to myContours
   private int getContourIndex (IntersectionContour c) {
      return myContours.indexOf(c);
   }

   ArrayList<PenetrationRegion> myRegions0; // penetration regions on mesh0
   ArrayList<PenetrationRegion> myRegions1; // penetration regions on mesh1

   // temporary intersection point used when building the contour
   IntersectionPoint myWorkPoint = new IntersectionPoint();

   /*
    * For each edge, remember all the intersection points, in order to
    * determine inside boundary vertices.
    */
   LinkedHashMap<HalfEdge,ArrayList<IntersectionPoint>> myEdgeMips =
      new LinkedHashMap<HalfEdge,ArrayList<IntersectionPoint>>();

   /*
    * For each edge-face pair between which an intersection has been found,
    * remember the contour. This allows duplicate intersections to be prevented.
    */
   protected HashMap<EdgeFacePair,IntersectionContour> 
      myEdgeFaceIntersections = 
      new HashMap<EdgeFacePair,IntersectionContour>();
   
   /**
    * Class to identity of unqiue edge-face pair and allow it to
    * be used as a hash key.
    */
   static class EdgeFacePair {
      HalfEdge myEdge;
      Face myFace;
      
      EdgeFacePair() {
      }
      
      EdgeFacePair (HalfEdge edge, Face face) {
         set (edge, face);
      }
      
      public void set (HalfEdge edge, Face face) {
         myEdge = edge;
         myFace = face;
      }
      
      public boolean equals (Object obj) {
         if (obj instanceof EdgeFacePair) {
            EdgeFacePair other = (EdgeFacePair)obj;
            return other.myEdge == myEdge && other.myFace == myFace;
         }
         else {
            return false;
         }
      }
      
      public int hashCode() {
         return myEdge.hashCode() + myFace.hashCode();
      }
   }

   /**
    * String representation of a half edge using its head and tail indices
    */
   public static String toString (HalfEdge he) {
      return he.getTail().getIndex() + "-" + he.getHead().getIndex();
   }

   private static String pad (String str, int len) {
      while (str.length() < len) {
         str += " ";
      }
      return str;
   }

   /**
    * Comprehensive string representation of a mesh intersection point,
    * showing most relevant information. Used for debugging.
    * 
    * The string contains the following information:
    * 
    * 1) index of the point with respect to the contour
    * 2) 'C' if the point is coincident, or ' ' otherwise
    * 3) the point's edge and face, if edge belongs to mesh, or
    *    the point's face and edge
    * 4) the segment faces of the point with respect to the mesh and the
    *    other mesh (or blank if this is the last point of an open contour)
    * 5) the point's coordinates   
    */
   public static String toString (IntersectionPoint p, PolygonalMesh mesh) {
      NumberFormat fmt = new NumberFormat ("%3d");
      String prefix = 
          fmt.format(p.contourIndex)+(p.isCoincident ? " C " : "   ");

      PolygonalMesh otherMesh = null;
      if (p.edge.getHead().getMesh() == mesh) {
         otherMesh = (PolygonalMesh)p.face.getMesh();
      }
      else {
         otherMesh = (PolygonalMesh)p.edge.getHead().getMesh();
      }
      int pk = p.contourIndex;
      
      if (p.edge.getHead().getMesh() == mesh) {
         prefix += ("E" + pad(toString(p.edge), 8) +
                    "F"+ pad(""+p.face.getIndex(), 8));
      }
      else {
         prefix += ("F"+ pad(""+p.face.getIndex(), 8) +
                    "E" + pad(toString(p.edge), 8));                    
      }

      Face face0 = p.contour.findSegmentFace (pk, mesh);
      Face face1 = p.contour.findSegmentFace (pk, otherMesh);
      
      if (face0 != null) {
         prefix += (pad("  F"+face0.getIndex(),6) +
                    pad(" F"+face1.getIndex(),5) + " ");
      }
      else {
         prefix += "            ";
      }
      return prefix + p.toString ("%g");
   }
   
   /*
    * Add a new intersection point <code>mip</code> to a contour.
    * 
    * Return true if mip is a new intersection and the add is successful, or if
    * the contour already contains this intersection and is thereby closed. 
    *
    * Answer false on failure. Fail if the edge-face intersection has already
    * been found in another contour.
    */   
   private boolean addContourPoint (
      IntersectionPoint mip, IntersectionContour contour) {
      
      EdgeFacePair edgeFace = new EdgeFacePair (mip.edge, mip.face);
      IntersectionContour otherContour = 
         myEdgeFaceIntersections.get (edgeFace);
      
      if (otherContour == null) { // new edge-face combination
         myEdgeFaceIntersections.put (edgeFace, contour);
         mip.contour = contour;
         mip.contourIndex = contour.size();
         contour.add (mip);
         /*
          * Give the edge a chance to remember the closest intersecting face to
          * each vertex.
          */
         addIntersectionToEdge (mip);
         return true;
      }
      if (otherContour == contour) {
         if (!mip.matches (contour.get (0))) {
            // Comment in earlier version of code, not sure what this means:
            //
            // "If contour already contains this intersection but intersection 
            // is not the contour's first point, remove the initial mips up to
            // but not including the first occurrence of this intersection and 
            // make them into a separate non-closed contour."
            //
            // However, the call to setDegenearte() does nothing of the
            // sort and in fact appears to do nothing at all.
            setDegenerate();
            return false;
         }
         contour.isClosed = true;
         contour.isContinuable = false;
         return true;
      }
      if (contour.size() > 0) {
         setDegenerate();
      }
      return false;      
   }
   
   /**
    * Remove the internal data associated with an intersection point
    * <code>mip</code>.
    */
   private void removeContourPoint (IntersectionPoint mip) {
      EdgeFacePair edgeFace = new EdgeFacePair (mip.edge, mip.face);

      myEdgeFaceIntersections.remove (edgeFace);
      removeIntersectionFromEdge (mip);
   }
   
   /*
    * Finds and returns a list of the intersection contours between two meshes.
    * If no intersection exists the list will be empty.
    *
    * @param mesh0 first mesh 
    * @param mesh1 second mesh 
    * @return list of the intersection contours
    */
   public ArrayList<IntersectionContour> findContours (
      PolygonalMesh mesh0, PolygonalMesh mesh1) {
      if (mesh0 != mesh1) {
         if (mesh0.canSelfIntersect) {
            if (findContours (mesh0, mesh0).size() > 0) {
               throw new RuntimeException ("self-intersecting mesh");
            }
         }
         if (mesh1.canSelfIntersect) {
            if (findContours (mesh1, mesh1).size() > 0) {
               throw new RuntimeException ("self-intersecting mesh");
            }
         }
      }

      this.myMesh0 = mesh0;
      this.myMesh1 = mesh1;
      mesh0.updateFaceNormals();
      mesh1.updateFaceNormals();
      
      if (!mesh0.isTriangular() | !mesh1.isTriangular()) {
         throw new RuntimeException ("collision with non-triangular mesh");
      }
     
      // Use the meshes' bounding hierarchies to find candidate nodes where
      // triangles may be overlapping.
      BVTree bvh0 = mesh0.getBVTree();
      BVTree bvh1 = mesh1.getBVTree();
      ArrayList<BVNode> nodes0 = new ArrayList<BVNode>();
      ArrayList<BVNode> nodes1 = new ArrayList<BVNode>();
      bvh0.intersectTree (nodes0, nodes1, bvh1);

      // Look for overlapping triangles and use these as the starting point for
      // tracing the intersection contours. If no contours are found,
      // intersected will be false.
      return findIntersectionContours (nodes0, nodes1);
   }

   /*
    * Finds the intersection contours between two meshes, and then processes
    * these contours further to determine the interpenetration regions on each
    * mesh. The contours and region information is returned in a
    * <code>ContactInfo</code> object. The method returns false if
    * no mesh intersection is found.
    *
    * @param cinfo returns the contact information 
    * @param mesh0 first mesh 
    * @param mesh1 second mesh 
    * @return <code>true</code> if a mesh intersection is found.
    */
   public boolean findContoursAndRegions (
      ContactInfo cinfo, PolygonalMesh mesh0, PolygonalMesh mesh1) {
      ArrayList<IntersectionContour> contours = findContours (mesh0, mesh1);
      cinfo.myContours = contours;
      if (contours.size() > 0) {
         cinfo.myRegions0 = new ArrayList<PenetrationRegion>();
         cinfo.myRegions1 = new ArrayList<PenetrationRegion>();
         myContours = cinfo.myContours;
         findPenetrationRegions (
            cinfo.myRegions0, contours, myMesh0, myMesh1, false);
         findPenetrationRegions (
            cinfo.myRegions1, contours, myMesh1, myMesh0, true);
      }
      return contours.size() > 0;
   }

   /**
    * Examine corresponding nodes pairs in the lists nodes0 and nodes1
    * for intersecting triangles. When a new intersecting triangle
    * is found, use this as the starting point for tracing a new
    * intersection contour.
    */
   ArrayList<IntersectionContour> findIntersectionContours (
      ArrayList<BVNode> nodes0, ArrayList<BVNode> nodes1) {
      ArrayList<IntersectionContour> contours =
         new ArrayList<IntersectionContour>();
      myEdgeFaceIntersections.clear();
      myEdgeMips.clear();

      for (int i = 0; i < nodes0.size(); i++) {
         BVNode node0 = nodes0.get (i);
         BVNode node1 = nodes1.get (i);
         int nc = findIntersectionContours (
            contours, node1.getElements(), node0.getElements(),
            /*edgeOnMesh0=*/true);
         // Find and add new contours.  next line is probably unnecessary if
         // mesh0 == mesh1.
         if (nc == 0) {
            findIntersectionContours (
               contours, node0.getElements(), node1.getElements(),
               /*edgeOnMesh0=*/false);
         }
      }
      // make each contour counterClockwise with respect to mesh0
      for (IntersectionContour c : contours) {
         if (c.isClockwise (myMesh0, myMesh1)) {
            c.reverse();
         }
      }
      return contours;
   }

   /**
    * Look for intersecting triangles in elems0 and elems1. If a new
    * intersection is found, use this as the starting point for tracing a new
    * intersection contour.
    */
   public int findIntersectionContours (
      ArrayList<IntersectionContour> contours, 
      Boundable[] elems0, Boundable[] elems1, boolean edgeOnMesh0) {

      int nfound = 0;
      EdgeFacePair edgeFacePair = new EdgeFacePair();
      for (Boundable elem1 : elems1) {
         if (elem1 instanceof Face) {
            Face f1 = (Face)elem1;
            HalfEdge he0 = f1.firstHalfEdge();
            HalfEdge he = he0;
            do {
               if (he.isPrimary()) {
                  for (Boundable elem0 : elems0) {
                     if (elem0 instanceof Face) {
                        Face f0 = (Face)elem0;
                        edgeFacePair.set (he, f0);
                        // check edgeFaceIntersections to see if this edge/face
                        // pair is already accounted for
                        if (myEdgeFaceIntersections.get (edgeFacePair) == null) {
                           //if (robustIntersectionWithFace (
                           //     he, f0, myWorkPoint, edgeOnMesh0)) {
                           if (intersectEdgeFace (
                              he, f0, myWorkPoint, edgeOnMesh0)) {
                              IntersectionContour c =
                                 findIntersectionContour (he, f0, edgeOnMesh0);
                              if (c != null) {
                                 contours.add (c);
                                 nfound++;
                              }
                           }
                        }
                     }
                  }
               }
               he = he.getNext();
            }
            while (he != he0);
         }
      }
      return nfound;
   }

   /*
    * Given a Face and a HalfEdge from two PolygonalMeshes that intersect at an
    * initial MeshIntersectionPoint, trace their intersection contour. Return a
    * MeshIntersectionContour of MeshIntersectionPoints. Stop tracing when: -
    * the contour is closed, - a duplicate intersection point is encountered -
    * the maximum number of points (maxContourPoints) is exceeded, or - the edge
    * of the mesh surface is encountered.
    */
   private IntersectionContour findIntersectionContour (
      HalfEdge edge, Face f, boolean edgeOnMesh0) {

      IntersectionContour contour = new IntersectionContour();
      IntersectionPoint mip = myWorkPoint;
      if (!addContourPoint (myWorkPoint, contour)) {
         return null; // May be rejected if it's a duplicate intersection.
      }
      myWorkPoint = new IntersectionPoint();

      // There are two possible directions to trace. First choose the one
      // associated with this half edge.
      Face edgeFace = mip.edge.getFace();
      if (edgeFace != null) {
         traceIntersectionContour (contour, edgeFace, edgeOnMesh0);
      }
      if (contour.isContinuable) {
         // The contour encountered a mesh edge and is open. Continue the trace
         // in the opposite direction.
         HalfEdge opposite = mip.edge.opposite;
         if (opposite != null) {
            edgeFace = opposite.getFace();
            if (edgeFace != null) {
               contour.reverse();
               traceIntersectionContour (contour, edgeFace, edgeOnMesh0);
            }
         }
      }
      if (contour.size() == 1) {
         removeContourPoint (contour.get(0));
         return null;
      }
      return contour;
   }

   /*
    * contour is an ArrayList of MeshIntersectionPoints with one element. The
    * first element is the intersection of edge and otherFace. edgeFace is an
    * adjacent edge to otherFace. It provides the initial search direction. Find
    * successive points in the intersection to continue the contour until it is
    * closed or an edge is encountered or it becomes too large.
    *
    * <p>
    * The contour has flags isClosed and isContinuable which are set
    * when points are added to it.
    *
    * The basic idea is this: given an intersection edge0/Face1 between Face0
    * and Face1, find the next intersection by intersecting edge0.opposite with
    * Face1 and discarding the intersection involving edge0. That will yield
    * either an intersection edgex/Face1 or edgey/edge.opposite, where edgex !=
    * edge0 is an edge of edge.opposite, or edgey is an edge of Face1.
    */
   private void traceIntersectionContour (
      IntersectionContour contour, 
      Face anEdgeFace, boolean edgeOnMesh0) {
      //SurfaceMeshCollider.collisionMetrics.walkTime -= System.nanoTime();
      IntersectionPoint mip = contour.get (contour.size()-1);
      Face edgeFace = anEdgeFace;
      Face otherFace = mip.face;
      Face tmpFace;
      HalfEdge edge = mip.edge;
      HalfEdge nextEdge = null;
      do {
         nextEdge =
            differentEdgeIntersectingFace (edgeFace, otherFace, edge, contour);
         if (nextEdge != null) {
            edge = nextEdge.opposite; // Move to next edge and face in the same
                                      // mesh radiating from the same vertex.
            if (edge == null) {
               //System.out.println (
               //   "SurfaceMeshIntersector.traceIntersectionContour - open mesh");
               contour.openMesh = true;
               edgeFace = null; // throw new RuntimeException("open mesh");
            }
            else {
               edgeFace = edge.getFace();
            }
         }
         else {
            edge = edgeIntersectingFace (otherFace, edgeFace, contour);
            if (edge == null) {
               edgeFace = null;
            }
            else {
               edge = edge.opposite;
               if (edge == null) {
                  //System.out.println (
//                  /   "SurfaceMeshIntersector.traceIntersectionContour - open mesh");
                  contour.openMesh = true;
                  edgeFace = null; // throw new RuntimeException("open mesh");
               }
               else {
                  tmpFace = edge.getFace(); // Chain-linked triangles - swap
                                            // meshes and continue.
                  otherFace = edgeFace;
                  edgeFace = tmpFace;
               }
            }
         }
      }
      while (edgeFace != null && contour.isContinuable);
   }

   /*
    * Remember all the intersection points for the edge, in sorted order of
    * increasing distance from edge.tail. Mark coincident mips.
    */
   void addIntersectionToEdge (IntersectionPoint mip) {
      ArrayList<IntersectionPoint> mips = myEdgeMips.get (mip.edge);
      if (mips == null) {
         mips = new ArrayList<IntersectionPoint>();
         mips.add (mip);
         myEdgeMips.put (mip.edge, mips);
         return;
      }
      int i = -1;
      int q = 0;
      boolean found;
      IntersectionPoint m = null;
      do {
         found = ++i < mips.size();
         if (found) {
            m = mips.get (i);
            q = RobustPreds.closestIntersection (mip.face, mip.edge, m.face);
         }
      }
      while (found && q > 0); // aho modified
      if (found) {
         if (q == 0) {
            m.isCoincident = true;
            mip.isCoincident = true;
         }
         mips.add (i, mip);
      }
      else {
         mips.add (mip);
      }
   }

   /**
    * Remove an intersection point <code>mip</code> from the set of
    * intersections associated with its edge.
    */
   void removeIntersectionFromEdge (IntersectionPoint mip) {
      ArrayList<IntersectionPoint> mips = myEdgeMips.get (mip.edge);
      if (mips == null) {
         throw new InternalErrorException (
            "No edge intersections found for edge + " + mip.edge.indexStr());
      }
      int i = mips.indexOf (mip);
      if (i == -1) {
         throw new InternalErrorException (
            "Intersection point not found on edge + " + mip.edge.indexStr());
      }         
      if (mip.isCoincident) {
         // XXX need to fix this
      }
      mips.remove (i);
      if (mips.isEmpty()) {
         myEdgeMips.remove (mip.edge);
      }
   }
   
   /**
    * Check if an edge belongs to a specified mesh.
    */
   static boolean edgeOnMesh (HalfEdge edge, PolygonalMesh mesh) {
      return edge.getHead().getMesh() == mesh;
   }

   /**
    * Return the edge of a face that contains an intersection point.
    */
   static HalfEdge getPointEdge (IntersectionPoint pa, Face face) {
      PolygonalMesh mesh = (PolygonalMesh)face.getMesh();
      if (edgeOnMesh (pa.edge, mesh)) {
         if (pa.edge.getFace() == face) {
            return pa.edge;
         }
         else if (pa.edge.getOppositeFace() == face) {
            return pa.edge.opposite;
         }
         else {
            throw new InternalErrorException (
               "Face edge not found for point " + pa);
         }
      }
      else {
         // convert pa to mesh local coordinates
         Point3d paLoc = new Point3d(pa);
         paLoc.inverseTransform (mesh.getMeshToWorld());
         HalfEdge he0 = face.firstHalfEdge();
         HalfEdge he = he0;
         HalfEdge heMin = null;
         double dmin = Double.POSITIVE_INFINITY;
         do {
            double d = LineSegment.distance (
               he.getHead().pnt, he.getTail().pnt, paLoc);
            if (d < dmin) {
               heMin = he;
               dmin = d;
            }
            he = he.getNext();
         }
         while (he != he0);
         return heMin;         
      }
   }

   String debugMipEdge = "";

   /**
    * Given a mesh intersection point associated with an edge, find the
    * nearest intersection point along the "inside" direction of the
    * associated edge. If no such point is found, return <code>null</code>.
    * "Inside" direction means the direction facing into the penetration
    * volume between the two meshes.
    * 
    * @param p intersection point being queried
    * @param edge edge on which the intersection point lies
    * @param mesh mesh to which the intersection point belongs
    * @param headIsInside <code>true</code> if the inside direction
    * is directed from the tail to the head.
    * @return
    */
   IntersectionPoint nearestInsideMip (
      IntersectionPoint p, HalfEdge edge, 
      PolygonalMesh mesh, boolean headIsInside) {

      ArrayList<IntersectionPoint> mips = myEdgeMips.get (edge.getPrimary());

      if (mips != null && mips.size() > 0) {
         // clockwise implies that the head of edge is on the inside
         if (edge.getPrimary() != edge) {
            headIsInside = !headIsInside;
         }
         int k = mips.indexOf (p);
         if (debugMipEdge.equals (toString(edge))) {
            NumberFormat fmt = new NumberFormat ("%2d ");
            for (int j=0; j<mips.size(); j++) {
               IntersectionPoint mp = mips.get(j);
               String prefix = (j == k ? "* " : "  ");
               prefix += fmt.format (getContourIndex(mp.contour));
               prefix += (mp.isCoincident ? "C " : "  ");
               System.out.println (prefix + mp.toString("%g"));
            }
            System.out.println ("k=" + k);
         }

         if (k != -1) {
            if (p.isCoincident) {
               // see if stepping two steps forward or back along the contour
               // returns us to one of the coincident points. If so, just
               // return that coincident point.
               IntersectionContour c = p.contour;
               IntersectionPoint pPlus2 = c.getWrapped(p.contourIndex+2); 
               // pPlus2 could be null if contour is open
               if (pPlus2 != null && pPlus2.edge == p.edge && pPlus2.isCoincident) {
                  int kPlus2 = mips.indexOf (pPlus2);
                  if (kPlus2 == k+1 || kPlus2 == k-1) {
                     return pPlus2;
                  }
               }
               IntersectionPoint pBack2 = c.getWrapped(p.contourIndex-2);
               // pBack2 could be null if contour is open
               if (pBack2 != null && pBack2.edge == p.edge && pBack2.isCoincident) {
                  int kBack2 = mips.indexOf (pBack2);
                  if (kBack2 == k+1 || kBack2 == k-1) {
                     return pBack2;
                  }
               }
            }
            if (headIsInside) {
               while (k < mips.size()-1) {
                  IntersectionPoint mip = mips.get(++k);
                  if (mip.contour.dividesMesh (mesh)) {
                     return mip;
                  }
               }
            }
            else {
               while (k > 0) {
                  IntersectionPoint mip = mips.get(--k);
                  if (mip.contour.dividesMesh (mesh)) {
                     return mip;
                  }                 
               }
            }
         }
         else {
            throw new InternalErrorException (
               "intersection point not recorded by its edge");
         }
      }
      if (debugMipEdge.equals (toString(edge))) {
         System.out.println ("No nearest inside mip");
      }
      return null;
   }


   /**
    * Returns true if v is strictly on the inside of face.
    */
   private boolean vertexStrictlyInside (Face face, Vertex3d v) {
      HalfEdge he = face.firstHalfEdge();
      Vertex3d v0 = he.tail;
      Vertex3d v1 = he.head;
      Vertex3d v2 = he.getNext().head;
      boolean vertexOnMesh0 = (face.getMesh() == myMesh1);
      return !RobustPreds.orient3d (v0, v2, v1, v, vertexOnMesh0);
   }
   
   String insideVertexDebug = "";

   /**
    * Returns the vertex that is on the inside of an edge associated
    * with an intersection point.
    */
   Vertex3d getInsideVertex (
      IntersectionPoint p, HalfEdge edge, boolean headIsInside) {

      /**
         In non-degenerate situations, the answer would be this:
         
         return (headIsInside ? faceEdge.getHead() : faceEdge.getTail());
      */

      boolean debug = insideVertexDebug.equals (toString(edge));

      ArrayList<IntersectionPoint> mips = myEdgeMips.get (edge.getPrimary());

      Vertex3d v = null;
      if (mips != null && mips.size() > 0) {
         // clockwise implies that the head of edge is on the inside
         if (edge.getPrimary() != edge) {
            headIsInside = !headIsInside;
            edge = edge.getPrimary();
            if (debug) {
               System.out.println ("primary is " + toString(edge));
            }
            
         }
         if (headIsInside) {
            // make sure head is really inside. If so, return head
            v = edge.getHead();
            int ke = mips.size()-1;
            if (mips.get(ke).isCoincident) {
               while (ke-1 >= 0 && mips.get(ke-1).isCoincident) {
                  ke--;
               }
            }
            for (int k=mips.size()-1; k>=ke; k--) {
               if (!vertexStrictlyInside (mips.get(k).face, v)) {
                  return null;
               }
            }
         }
         else {
            // make sure tail is really inside. If so, return tail
            v = edge.getTail();
            int ke = 0;
            if (mips.get(ke).isCoincident) {
               while (ke+1 < mips.size() && mips.get(ke+1).isCoincident) {
                  ke++;
               }
            }
            for (int k=0; k<=ke; k++) {
               if (!vertexStrictlyInside (mips.get(k).face, v)) {
                  return null;
               }
            }
         }
      }
      return v;
   }

   /**
    * For a given edge, return the nearest intersection point to a specified
    * vertex on that edge, or <code>null</code> if the edge contains no
    * intersection points.
    */
   IntersectionPoint nearestMipToVertex (
      HalfEdge edge, Vertex3d v, PolygonalMesh mesh) {
      ArrayList<IntersectionPoint> mips = myEdgeMips.get (edge);
      if (mips != null && mips.size() > 0) {
         if (v == edge.getTail()) {
            for (int k=0; k<mips.size(); k++) {
               IntersectionPoint mip = mips.get(k);
               if (mip.contour.dividesMesh (mesh)) {
                  return mip;
               }
            }
         }
         else {
            for (int k=mips.size()-1; k>=0; k--) {
               IntersectionPoint mip = mips.get(k);
               if (mip.contour.dividesMesh (mesh)) {
                  return mip;
               }
            }            
         }
      }
      return null;
   }

   /**
    * Finds the penetration region on <code>mesh0</code> that is associated
    * with a single specific contour. As the region is traversed, other
    * contours may be found to be associated with it; these may be determined
    * by querying the {@link PenetrationRegion#getContours} method for the
    * returned region.
    *
    * @param contour contour 
    * @param mesh0 mesh on which the region resides
    * @param mesh1 other mesh which is intersecting <code>mesh0</code>
    * @param clockwiseContour <code>true</code> if intersection contours are
    * oriented clockwise with respect to <code>mesh0</code>
    * @return penetration region associated with <code>contour</code>
    */
   PenetrationRegion findPenetrationRegion (
      IntersectionContour contour, PolygonalMesh mesh0, PolygonalMesh mesh1,
      boolean clockwiseContour) {

      PenetrationRegion region = new PenetrationRegion(mesh0, clockwiseContour);
      Deque<IntersectionContour> contoursToTrace =
         new ArrayDeque<IntersectionContour>();
      region.myContours.add (contour);
      contoursToTrace.offerLast (contour);
      Deque<Vertex3d> verticesToTrace = new ArrayDeque<Vertex3d>();

      IntersectionContour c;      
      while ((c = contoursToTrace.pollFirst()) != null) {

         Face lastFace = c.findSegmentFace (c.size()-1, mesh0);
         if (lastFace != null) {
            // lastFace will not be null if the contour is closed
            region.myInsideFaces.add (lastFace);
            region.mySingleFace = lastFace;
         }
         
         for (int k=0; k<c.size(); k++) {
            IntersectionPoint mip = c.get(k);
            Face face = c.findSegmentFace (k, mesh0);
            
            if (lastFace != face) {

               HalfEdge faceEdge;
               boolean headIsInside;
               if (face != null) {
                  // we are leaving lastFace and entering face
                  region.myInsideFaces.add (face);
                  faceEdge = getPointEdge (mip, face);
                  // headIsInside is clockwise since we are *entering* 
                  // face through faceEdge
                  headIsInside = clockwiseContour;
               }
               else {
                  // Last point of an open contour. Choose faceEdge using 
                  // lastFace
                  faceEdge = getPointEdge (mip, lastFace);
                  // headIsInside is !clockwise since we are *exiting* 
                  // lastFace through faceEdge
                  headIsInside = !clockwiseContour;                  
               }
               region.myInsideEdges.add (faceEdge.getPrimary());
               IntersectionPoint insideMip =
                  nearestInsideMip (mip, faceEdge, mesh0, headIsInside);
               if (insideMip == null) {
                  Vertex3d v = getInsideVertex (mip, faceEdge, headIsInside);
                  if (v != null) {
                     if (region.myInsideVertices.add (v)) {
                        verticesToTrace.offerLast (v);
                     }
                  }
               }           
               else {
                  if (region.myContours.add(insideMip.contour)) {
                     contoursToTrace.offerLast (insideMip.contour);
                  }
               }
               region.mySingleFace = null;
            }
            lastFace = face;
         }
         Vertex3d vtx;
         while ((vtx = verticesToTrace.pollFirst()) != null) {
            Iterator<HalfEdge> incidentEdges = vtx.getIncidentHalfEdges();
            while (incidentEdges.hasNext()) {
               HalfEdge edge = incidentEdges.next().getPrimary();
               if (region.myInsideEdges.add (edge)) {
                  region.myInsideFaces.add (edge.getFace());
                  if (edge.opposite != null) {
                     region.myInsideFaces.add (edge.opposite.getFace());
                  }
                  Vertex3d v = edge.head;
                  if (v == vtx) {
                     v = edge.tail;
                  }
                  IntersectionPoint mip = nearestMipToVertex (edge, vtx, mesh0);
                  if (mip != null) {
                     if (region.myContours.add(mip.contour)) {
                        contoursToTrace.offerLast (mip.contour);
                     }
                  }
                  else {
                     if (region.myInsideVertices.add (v)) {
                        verticesToTrace.offerLast (v);
                     }                    
                  }
               }
            }
         }
      }
      return region;
   }

   /**
    * From a list of regions, find and return those which both contain
    * <code>face</code> and which are not themselves single-face regions.
    */
   ArrayList<PenetrationRegion> getFaceContainingRegions (
      ArrayList<PenetrationRegion> regions, Face face) {

      ArrayList<PenetrationRegion> regionsContainingFace =
         new ArrayList<PenetrationRegion>();

      for (PenetrationRegion r : regions) {
         if (r.mySingleFace == null && r.myInsideFaces.contains (face)) {
            regionsContainingFace.add (r);
         }
      }
      return regionsContainingFace;
   }

   /**
    * Creates a region consisting of and entire mesh. This is used in
    * situations where there is a single-face contour whose "inside" is
    * actually the rest of the mesh.
    */
   PenetrationRegion createWholeMeshRegion (
      PolygonalMesh mesh, boolean clockwise) {
      PenetrationRegion region = new PenetrationRegion(mesh, clockwise);
      region.myInsideVertices.addAll (mesh.getVertices());
      for (Face face : mesh.getFaces()) {
         region.myInsideFaces.add (face);
         HalfEdge he0 = face.firstHalfEdge();
         HalfEdge he = he0;
         do {
            region.myInsideEdges.add (he.getPrimary());
            he = he.getNext();
         }
         while (he != he0);
      }
      return region;
   }

   /**
    * Use the local intersection geometry around the intersection point
    * <code>pe</code> to see if a point <code>p0</code> is outside or inside
    * the penetration region.
    *
    * @param p0 point being tested
    * @param face face on mesh associated with the intersection point
    * @param pe intersection point
    * @param mesh mesh containing <code>face</code>
    * @return <code>true</code> if <code>p0</code> is outside the penetration
    * region.
    */
   static boolean isPointOutside (
      Point3d p0, Face face, IntersectionPoint pe, PolygonalMesh mesh) {

      // get the other mesh being intersected with mesh
      PolygonalMesh otherMesh = pe.getOtherMesh (mesh);

      int ke = pe.contourIndex;
      IntersectionContour contour = pe.contour; // contour associated with pe
      IntersectionPoint pp = contour.getWrapped(ke-1); // previous contour point

      IntersectionPoint pa = pp;
      IntersectionPoint pb = pe;

      // The basic idea is to try and find two nearest faces, face0 and face1,
      // that are on the *other* mesh intersecting <code>mesh</code>.  Then we
      // try to determine if p0 is inside or outside the local geometry
      // determined by these two faces.

      Face face0 = contour.findSegmentFace (pa, pb, otherMesh);
      Face face1 = null;
      int kb=ke+1;
      // advance forward and try to find the second face and store this in
      // <code>face1</code>. Stop only if we run out of contour, or if we move
      // more than two segments away from the face.  Should change this to be
      // "move to a segment that has no feature connection with the face".

      int kf=kb; // kf is the most recent kb associated with face.
      do {
         pa = pb;
         pb = contour.getWrapped(kb);
         face1 = contour.findSegmentFace (pa, pb, otherMesh);
         if (face1 != face0) {
            break;
         }
         Face nextFace = contour.findSegmentFace (pa, pb, mesh);
         if (nextFace == face) {
            kf = kb;
         }
         kb++;
      }
      while (pb != pp && kb-kf <= 2);

      Point3d p0loc = new Point3d();
      if (face0 != face1) {
         // found two faces, use to see if p0 is inside or outside otherMesh
         Vector3d nrm0 = new Vector3d();
         Vector3d nrm1 = new Vector3d();
         Vector3d nrmf = new Vector3d();
         face.getWorldNormal (nrmf);
         face0.getWorldNormal (nrm0);
         face1.getWorldNormal (nrm1);
         
         p0loc.sub (p0, pa);
         Vector3d xprod = new Vector3d();
         xprod.cross (nrm0, nrm1);
         if (xprod.dot (nrmf) >= 0) {
            // interior region is convex. p0 is outside if it is
            // outside either face plane
            return (p0loc.dot(nrm0) >= 0 || p0loc.dot(nrm1) >= 0);
         }

         else {
            // interior region is concave. p0loc is outside if it is
            // outside both face planes
            return (p0loc.dot(nrm0) >= 0 && p0loc.dot(nrm1) >= 0);
         }
      }
      else {
         // could not find two faces. Just use face0 to see if p0 is inside or
         // outside otherMesh.
         Vector3d nrm0 = new Vector3d();
         face0.getWorldNormal (nrm0);
         p0loc.sub (p0, pe);
         return p0loc.dot (nrm0) >= 0;
      }
   }  

   /**
    * For debugging: create a name for a penetration region based on the
    * indices of all its contours surrounded by square brackets.
    */
   private String getName (PenetrationRegion r) {
      String str = "[ ";
      for (IntersectionContour c : r.myContours) {
         str += ""+getContourIndex(c) + " ";
      }
      return str + "]";
   }

   /**
    * Given the first <code>numCheck</code> regions of a list
    * <code>nested</code> of single-face nested regions belonging to
    * <code>face</code>, find and return those which are contained within
    * <code>region</code>.
    */
   ArrayList<PenetrationRegion> getContainedRegions (
      PenetrationRegion region, Face face,
      ArrayList<PenetrationRegion> nested,
      int numCheck, boolean clockwiseContour) {

      double dtol = EPS*face.computeCircumference();


      // The method works as follows: For each of the nested regions being
      // checked, we select the first point on its contour as a test point.  We
      // then use NearestPolygon3dFeature to find the nearest feature to the
      // test point among all of the polygonal segments formed from the
      // contours of <code>region</code> that cross the face. This nearest
      // feature can then be queried to see if the test point is inside or
      // outside, given the contour orientation specified by
      // <code>clockwiseContour</code>.

      // Create and initialize nearest feature objects for each region of
      // <code>nested</code> being checked.
      NearestPolygon3dFeature[] nearestFeats =
         new NearestPolygon3dFeature[numCheck];
      for (int i=0; i<numCheck; i++) {
         PenetrationRegion r = nested.get(i);
         NearestPolygon3dFeature nfeat = new NearestPolygon3dFeature();
         nearestFeats[i] = nfeat;
         nfeat.init (r.getFirstContour().get(0), face.getWorldNormal(), dtol);
      }
      PolygonalMesh mesh = (PolygonalMesh)face.getMesh();

      // Now find the nearest feature to the test point among all the contours
      // of <code>region</code>:
      for (IntersectionContour c : region.myContours) {
         int csize = c.size();

         // Try to find the first point where the contour enters this face and
         // store its index in kenter:
         int kenter = -1;
         Face lastFace = c.findSegmentFace (csize-1, mesh);
         for (int k=0; k<csize; k++) {
            Face segFace = c.findSegmentFace (k, mesh);
            if (segFace == face && lastFace != face) {
               kenter = k;
               break;
            }
            lastFace = segFace;
         }
         if (kenter == -1) {
            // kenter was not found, so if lastFace == face, the contour lies
            // entirely within face and so we check the test point against the
            // entire contour:
            if (lastFace == face) {
               for (int k=0; k<=csize; k++) {
                  for (int i=0; i<numCheck; i++) {
                     if (k==0) {
                        nearestFeats[i].restart ();
                        nearestFeats[i].advance (c.get(0));
                     }
                     else if (k<csize) {
                        nearestFeats[i].advance (c.getWrapped(k));
                     }
                     else {
                        nearestFeats[i].close ();
                     }
                  }
               }
            }
         }
         else {
            // Starting at kenter, check the test point against those polygonal
            // segments that occur when the contour crosses face:
            int k = kenter;
            for (int j=0; j<csize; j++) {
               for (int i=0; i<numCheck; i++) {
                  if (j==0) {
                     // contour c entering face for the first time
                     nearestFeats[i].restart ();
                     nearestFeats[i].advance (c.getWrapped(k));
                     lastFace = face;
                  }
                  else {
                     Face segFace = c.findSegmentFace (k, mesh);
                     if (lastFace == face) {
                        // continuing on the face 
                        nearestFeats[i].advance (c.getWrapped(k));
                     }
                     else if (segFace == face) {
                        // reentering the face
                        nearestFeats[i].restart ();
                        nearestFeats[i].advance (c.getWrapped(k));
                     }
                     else {
                        // not on the face
                     }
                     lastFace = segFace;
                  }
               }   
               k = (k+1)%csize;                  
            }
         }
      }
      ArrayList<PenetrationRegion> contained =
         new ArrayList<PenetrationRegion>();
      for (int i=0; i<numCheck; i++) {
         NearestPolygon3dFeature nfeat = nearestFeats[i];
         boolean isContained;
         if (nfeat.numVertices() == 1) {
            // special case: occurs when the region's contours intersect the
            // face at only one point. Then try to use the local intersection
            // geometry around this point (returned by nfeat.getVertex(0)) to
            // determine if the test point is inside or outside the penetration
            // region.
            isContained = !isPointOutside (
               nfeat.getPoint(), face, 
               (IntersectionPoint)nfeat.getVertex(0), mesh);
         }
         else if (nfeat.isOutside (clockwiseContour) != -1) {
            // XXX should check distance and redo if it is 0
            isContained = (nfeat.isOutside (clockwiseContour) == 0);
         }
         else {
            System.out.println ("isOutside not defined");
            isContained = false;
         }
         if (isContained) {
            contained.add (nested.get(i));
         }
      }
      return contained;
   }

   /**
    * Given a list <code>nested</code> of single-face nested regions belonging
    * to <code>face</code>, find those which are contained within
    * <code>region</code>. Those which are contained are removed from
    * <code>nested</code> and merged with <code>region</code>.
    */
   void addContainedRegions (
      PenetrationRegion region, Face face,
      ArrayList<PenetrationRegion> nested, boolean clockwiseContour) {
      
      if (nested.isEmpty()) {
         return;
      }
      int numCheck = nested.size();
      if (region.mySingleFaceArea > 0) {
         int k = 0;
         // if region is itself a single-faced region, need to check only those
         // nested regions whose area is <= region.mySingleFaceArea.
         while (k < nested.size() &&
                (Math.abs(nested.get(k).mySingleFaceArea) <=
                 region.mySingleFaceArea)) {
            k++;
         }
         numCheck = k;
      }
      if (numCheck == 0) {
         return;
      }
      
      ArrayList<PenetrationRegion> contained =
         getContainedRegions (
            region, face, nested, numCheck, clockwiseContour);
      if (contained.size() > 0) {
         nested.removeAll (contained);
         for (PenetrationRegion r : contained) {
            region.myContours.add (r.getFirstContour());
         }
      }
   }

   /**
    * Compares two vertices based on their indices.
    */
   static class VertexIndexComparator implements Comparator<Vertex3d> {
      public int compare (Vertex3d v0, Vertex3d v1) {
         
         if (v0.getIndex() < v1.getIndex()) {
            return -1;
         }
         else if (v0.getIndex() == v1.getIndex()) {
            return 0;
         }
         else {
            return 1;
         }
      }
   }

   /**
    * Compares two faces based on their indices.
    */
   static class FaceIndexComparator implements Comparator<Face> {
      public int compare (Face f0, Face f1) {
         
         if (f0.getIndex() < f1.getIndex()) {
            return -1;
         }
         else if (f0.getIndex() == f1.getIndex()) {
            return 0;
         }
         else {
            return 1;
         }
      }
   }

   /**
    * Comparator used to sort single faces regions by area.
    */
   private class RegionAreaComparator implements Comparator<PenetrationRegion> {

      public int compare (PenetrationRegion r0, PenetrationRegion r1) {
         if (r0.mySingleFace != null && r1.mySingleFace != null) {
            double a0 = Math.abs(r0.mySingleFaceArea);
            double a1 = Math.abs(r1.mySingleFaceArea);
            if (a0 < a1) {
               return -1;
            }
            else if (a0 == a1) {
               return 0;
            }
            else {
               return 1;
            }
         }
         else {
            // can't compare; assume equal
            return 0;
         }
      }
   }

   /**
    * Finish processing all the single-face regions associated with a specific
    * face, combining those which border the same region and then adding them
    * to the list <code>regions</code>.
    *
    * @param regions returns the processed region for this face
    * @param face face assiciated with the single-face regions
    * @param singleFaceRegions initial (and uncombined) single-face regions
    * for <code>face</code>
    * @param mesh0 first intersecting mesh
    * @param clockwiseContour <code>true</code> if the intersection contours
    * are oriented clockwise with respect to <code>mesh0</code>
    */
   void processSingleFaceRegions (
      ArrayList<PenetrationRegion> regions, Face face,
      ArrayList<PenetrationRegion> singleFaceRegions,
      PolygonalMesh mesh0, boolean clockwiseContour) {

      // initially, each single-face face region consists of a closed contour
      // located within its single face. We use the sign of the area of each of
      // these regions to separated them into those which are either "isolated"
      // (postive) or "nested" (negative). Isolated regions surround their own
      // penetration area and may contain nested regions. For nested regions,
      // the penetration area is on their outside.
      ArrayList<PenetrationRegion> nested = new ArrayList<PenetrationRegion>();
      ArrayList<PenetrationRegion> isolated = new ArrayList<PenetrationRegion>();

      for (PenetrationRegion r : singleFaceRegions) {
         if (r.mySingleFaceArea < 0) {
            nested.add (r);
         }
         else {
            isolated.add (r);
         }
      }

      if (isolated.size() > 0) {
         // For each isolated region, find any nested regions that it contains.
         // To prevent a nested region from being included in multiple isolated
         // regions, we sort the regions by increasing area and work from the
         // smallest to the largest.
         if (isolated.size() > 1) {
            Collections.sort (isolated, new RegionAreaComparator());
         }
         if (nested.size() > 1) {
            Collections.sort (nested, new RegionAreaComparator());
         }
         for (PenetrationRegion r : isolated) {
            addContainedRegions (r, face, nested, clockwiseContour);           
            regions.add (r);
         }
      }

      if (nested.size() > 0) {
         // remaining nested regions must belong to face containing regions;
         // i.e., regions that contain <code>face</code> but are not themselves
         // single-face regions.
         ArrayList<PenetrationRegion> faceContainingRegions =
            getFaceContainingRegions (regions, face);
         if (faceContainingRegions.size() == 0) {
            if (regions.size() > 0) {
               throw new InternalErrorException (
                  "No regions reference a face with nested contours");
            }
            // create a region that contains the whole mesh
            PenetrationRegion r = 
               createWholeMeshRegion (mesh0, clockwiseContour);
            //System.out.println ("creating whole mesh region");
            regions.add (r);
            faceContainingRegions.add (r);
         }
         if (faceContainingRegions.size() == 1) {
            // just one region - add the contours to it
            PenetrationRegion region = faceContainingRegions.get(0);
            for (PenetrationRegion r : nested) {
               region.myContours.add (r.getFirstContour());
            }
         }
         else {
            // multiple regions. Need to determine which nested region
            // belongs to which
            for (PenetrationRegion r : faceContainingRegions) {
               addContainedRegions (r, face, nested, clockwiseContour);
            }
            if (nested.size() > 0) {
               throw new InternalErrorException (
                  "Cannot find parent regions for some nested regions");
            }
         }
      }
   }

   /**
    * Finds all the regions on mesh0 that are penetrating mesh1.
    *
    * <p>The method works by finding the penetration regions associated with
    * each intersection contour. During the process, some regions may be found
    * to be bordered by more than one contour. These are then removed from
    * further consideration. Some regions may also be determined to lie only on
    * a single face. These are then further processed to see if any are nested
    * inside each other and therefore need to be combined.
    * 
    * @param regions returns the found regions
    * @param contours intersection contours between the two meshes
    * @param mesh0 mesh for which the regions are to be found
    * @param mesh1 other mesh which is intersecting <code>mesh0</code>
    * @param clockwiseContour <code>true</code> if the contours are oriented
    * clockwise with respect to <code>mesh0</code>
    * @return the number of regions found
    */
   int findPenetrationRegions (
      ArrayList<PenetrationRegion> regions,
      ArrayList<IntersectionContour> contours,
      PolygonalMesh mesh0, PolygonalMesh mesh1, boolean clockwiseContour) {

      int oldSize = regions.size();
      // contour which have yet to be used for finding regions:
      LinkedList<IntersectionContour> unusedContours = 
         new LinkedList<IntersectionContour>();
      // container to store single-face regions
      HashMap<Face,ArrayList<PenetrationRegion>> singleFaceRegions =
         new HashMap<Face,ArrayList<PenetrationRegion>>();

      for (IntersectionContour c : contours) {
         if (c.dividesMesh (mesh0)) {
            unusedContours.add (c);
         }
      }
      while (!unusedContours.isEmpty()) {
         IntersectionContour contour = unusedContours.removeFirst();
         PenetrationRegion region =
            findPenetrationRegion (contour, mesh0, mesh1, clockwiseContour);
         if (region.myContours.size() > 1) {
            // remove other contours associated with the found region from
            // further consideration:
            unusedContours.removeAll (region.myContours);
         }
         if (region.mySingleFace != null) {
            // region is associated with only a single face. It may be bordered
            // by other single-face regions for the same face (which it either
            // contains or which enclose it), but findPenetrationRegion() will
            // not have been able to determine these. Instead, these will need
            // to be determined seperately by processSingleFaceRegions, below.
            Face face = region.mySingleFace;
            if (region.myContours.size() != 1) {
               // verify that findPenetrationRegion() did not find additional
               // contours for the region
               throw new InternalErrorException (
                  "single-face region has "+region.myContours.size()+
                  " contours instead of 1");
            }
            // Compute the area that the contour encloses on the face.  If
            // there are other contours confined to this face, the area will
            // used to help determine which contour encloses which.

            // Note: there may be a faster way to determine nesting than by
            // computing area. For instance, let (pa,pb) be two points on the
            // contour and nrm be the face normal. Then right = (pb - pa) X
            // defines the right turn direction as the contour moves along the
            // face. Now if inrm is the normal of the face on the other mesh,
            // the contour should be isolated if right.dot (inrm) < 0 for
            // clockwise contours and right.dot (inrm) > 0 for counter
            // clockwise ones.
            IntersectionContour c = region.getFirstContour();
            double area = c.computeFaceArea (face, clockwiseContour);            
            region.mySingleFaceArea = area; // store area for later use
            // add this region to the set of single-face regions associated
            // with the same face
            ArrayList<PenetrationRegion> regionsOnFace =
               singleFaceRegions.get(face);
            if (regionsOnFace == null) {
               regionsOnFace = new ArrayList<PenetrationRegion>();
               singleFaceRegions.put(face, regionsOnFace);
            }
            regionsOnFace.add (region);
         }
         else {
            // add to the list of regions
            regions.add (region);
         }
      }

      if (singleFaceRegions.size() > 0) {
         // process any single-face regions
         try {         
            for (Map.Entry<Face,ArrayList<PenetrationRegion>> entry :
                    singleFaceRegions.entrySet()) {
               processSingleFaceRegions (
                  regions, entry.getKey(), entry.getValue(),
                  mesh0, clockwiseContour);
            }
         }
         catch (Exception e) {
            e.printStackTrace(); 
            throw e;
         }
      }

      return regions.size()-oldSize;
   }

   public void setDegenerate() {
      // Not at all sure what this was supposed to do originally ...
//      ContactInfo contactInfo = new ContactInfo (myMesh0, myMesh1);
//      contactInfo.myContours = myContours;
   }

   /*
    * Return a HalfEdge of a Face which intersects another Face, and
    * add the new intersection point to the contour. Return null if no HalfEdge
    * of the Face intersects the other Face, or if no intersection point is
    * found that can be added to the contour (duplicate points will be rejected,
    * or the contour may be full). If excludeEdge is specified: - excludeEdge
    * must be a HalfEdge of the Face - only test the other two HalfEdges of
    * this Face for intersection, and return null if neither intersect.
    */
   private HalfEdge differentEdgeIntersectingFace (
      Face face, Face otherFace, HalfEdge excludeEdge, 
      IntersectionContour contour) {
      
      HalfEdge he0 = face.firstHalfEdge();
      boolean edgeOnMesh0 = (otherFace.getMesh() == myMesh1);
      HalfEdge he = he0;
      do {
         if (he != excludeEdge & he != excludeEdge.opposite) {
            if (intersectEdgeFace (he.getPrimary(),
               otherFace, myWorkPoint, edgeOnMesh0)) {
               if (addContourPoint (myWorkPoint, contour)) {
                  myWorkPoint = new IntersectionPoint();
                  return he;
               }
            }
         }
         he = he.getNext();
      }
      while (he != he0);
      return null;
   }

   /*
    * Return a HalfEdge of ta Face which intersects the another Face, and
    * add the new intersection point to the contour. Return null if no HalfEdge
    * of the Face intersects the other Face, or if no intersection point is
    * found that can be added to the contour (duplicate points will be rejected,
    * or the contour may be full).
    */
   public HalfEdge edgeIntersectingFace (
      Face face, Face otherFace, 
      IntersectionContour contour) {
      boolean edgeOnMesh0 = (otherFace.getMesh() == myMesh1);
      HalfEdge he0 = face.firstHalfEdge();
      HalfEdge he = he0;
      do {
         if (intersectEdgeFace (he.getPrimary(),
            otherFace, myWorkPoint, edgeOnMesh0)) {
            if (addContourPoint(myWorkPoint, contour)) {
               myWorkPoint = new IntersectionPoint();
               return he;
            }
         }
         he = he.getNext();
      }
      while (he != he0);
      return null;
   }

   /**
    * Intersects an edge and a face. If there is an intersection, the resulting
    * point is returned in <code>mip</code> and the method returns
    * <code>true</code>.
    * 
    * @param he edge to intersect
    * @param face face to intersect
    * @param mip returns intersection point if there is an intersection
    * @param edgeOnMesh0 if <code>true</code>, indicates that the edge belongs
    * to the first mesh being intersected.
    * @return <code>true</code> if the edge and face intersect.
    */
   private boolean intersectEdgeFace (
      HalfEdge he, Face face, IntersectionPoint mip, boolean edgeOnMesh0) {
      
      // Do an efficient calculation first. If the edge/face pair is too close
      // to degenerate situation to determine intersection correctly, -1 is
      // returned and the computation is performed again using robust
      // predicates.
      int intersects = face.intersectsEdge (he, mip);
      if (intersects == -1) {
         if (RobustPreds.intersectEdgeFace (he, face, mip, edgeOnMesh0)) {
            intersects = 1;
         }
         else {
            intersects = 0;
         }
      }
      if (intersects == 0) {
         return false;
      }
      else {
         mip.edge = he;
         mip.face = face;
         mip.edgeOnMesh0 = edgeOnMesh0;
         return true;     
      }
   }
}
