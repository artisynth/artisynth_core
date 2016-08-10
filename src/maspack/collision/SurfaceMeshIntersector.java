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
 *
 */
public class SurfaceMeshIntersector {

   private static final double EPS = 1e-10;

   PolygonalMesh myMesh0;  // first mesh to be intersected
   PolygonalMesh myMesh1;  // second mesh to be intersected

   /*
    * List of contours. Each contour is a list of MeshIntersectionPoints, each
    * of which represents the intersection of a HalfEdge and a Face.
    */
   ArrayList<IntersectionContour> myContours;

   ArrayList<PenetrationRegion> myRegions0; // penetration regions on mesh0
   ArrayList<PenetrationRegion> myRegions1; // penetration regions on mesh1
   
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

   public ArrayList<IntersectionPoint> getEdgeMips (HalfEdge he) {
      return myEdgeMips.get(he);
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
    */
   public static String toString (IntersectionPoint p, PolygonalMesh mesh) {
      NumberFormat fmt = new NumberFormat ("%3d");
      String prefix = fmt.format(p.contourIndex)+(p.isCoincident ? " C " : "   ");

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

      prefix += (pad("  F"+face0.getIndex(),6) +
                 pad(" F"+face1.getIndex(),5) + " ");
      return prefix + p.toString ("%g");
   }
   
   private boolean addWorkPoint (IntersectionContour contour) {
      if (contour.add (myWorkPoint)) {
         myWorkPoint = new IntersectionPoint();
         return true;
      }
      else {
         return false;
      }
   }
   
//   private boolean addContourPoint (
//      MeshIntersectionPoint mip, MeshIntersectionContour contour) {
//      return addContourPointNew (mip, contour);
//      if (contour.add (myWorkPoint)) {
//         myWorkPoint = new MeshIntersectionPoint();
//         return true;
//      }
//      else {
//         return false;
//      }
//   }
   
   /*
    * Add a new intersection point to a contour.
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
         setEdgeIntersectionPoints (mip);
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
   
   /*
    * Main entry points. Finds the intersection contours between mesh0
    * and mesh1, and then analyzes them to find the penetration regions.
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

   public boolean findContoursAndRegions (
      ContactInfo cinfo, PolygonalMesh mesh0, PolygonalMesh mesh1) {
      cinfo.myContours = findContours (mesh0, mesh1);
      if (cinfo.myContours.size() > 0) {
         cinfo.myRegions0 = new ArrayList<PenetrationRegion>();
         cinfo.myRegions1 = new ArrayList<PenetrationRegion>();
         myContours = cinfo.myContours;
         createRegions (cinfo.myRegions0, myMesh0, myMesh1, false);
         createRegions (cinfo.myRegions1, myMesh1, myMesh0, true);
      }
      return cinfo.myContours.size() > 0;
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
                           if (he.intersectionWithFace (f0)) {
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

      if (!robustIntersectionWithFace (
         edge, f, myWorkPoint, edgeOnMesh0)) {
         return null;
      }
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
      return contour;
      /*
       * XXX bug with open contours in AJL code, for now suppress warning
       *
      else {
    	 if (!contour.openMesh) {	// open contours due to open meshes are to be ignored. 
            mesh0.dumpToFile("mesh0.msh");
            mesh1.dumpToFile("mesh1.msh");
            SurfaceMeshCollider.collisionMetrics.openContours++;
            System.out.println (
               "SurfaceMeshIntersector.traceIntersectionContour - open contour");
             throw new RuntimeException("open contour");
    	 }
      }
      */
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
               System.out.println (
                  "SurfaceMeshIntersector.traceIntersectionContour - open mesh");
               contour.openMesh = true;
               edgeFace = null; // throw new RuntimeException("open mesh");
            }
            else {
               edgeFace = edge.getFace();
            }
         }
         else {
            //contour.switchEdgeRegion();
            edge = edgeIntersectingFace (otherFace, edgeFace, contour);
            if (edge == null) {
               edgeFace = null;
            }
            else {
               edge = edge.opposite;
               if (edge == null) {
                  System.out.println (
                     "SurfaceMeshIntersector.traceIntersectionContour - open mesh");
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
   void setEdgeIntersectionPoints (IntersectionPoint mip) {
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
         System.out.println ("getting edge from face");
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

   IntersectionPoint nearestInsideMip (
      IntersectionPoint p, HalfEdge edge, boolean clockwiseContour) {

      ArrayList<IntersectionPoint> mips = myEdgeMips.get (edge.getPrimary());

      if (mips != null && mips.size() > 0) {
         // clockwise implies that the head of edge is on the inside
         boolean headIsInside = clockwiseContour;
         if (edge.getPrimary() != edge) {
            headIsInside = !headIsInside;
         }
         int k = mips.indexOf (p);
         if (debugMipEdge.equals (toString(edge))) {
            NumberFormat fmt = new NumberFormat ("%2d ");
            for (int j=0; j<mips.size(); j++) {
               IntersectionPoint mp = mips.get(j);
               String prefix = (j == k ? "* " : "  ");
               prefix += fmt.format (myContours.indexOf(mp.contour));
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
               if (pPlus2.edge == p.edge && pPlus2.isCoincident) {
                  int kPlus2 = mips.indexOf (pPlus2);
                  if (kPlus2 == k+1 || kPlus2 == k-1) {
                     return pPlus2;
                  }
               }
               IntersectionPoint pBack2 = c.getWrapped(p.contourIndex-2); 
               if (pBack2.edge == p.edge && pBack2.isCoincident) {
                  int kBack2 = mips.indexOf (pBack2);
                  if (kBack2 == k+1 || kBack2 == k-1) {
                     return pBack2;
                  }
               }
            }
            if (headIsInside) {
               if (k < mips.size()-1) {
                  return mips.get(k+1);
               }
            }
            else {
               if (k > 0) {
                  return mips.get(k-1);
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
    * Returns the vertex that is on the inside as we *enter* a face.
    */
   Vertex3d getInsideVertex (
      IntersectionPoint p, HalfEdge edge, boolean clockwiseContour) {

      /**
         In non-degenerate situations, the answer would be this:
         
         return (clockwiseContour ? faceEdge.getHead() : faceEdge.getTail());
      */

      boolean debug = insideVertexDebug.equals (toString(edge));

      ArrayList<IntersectionPoint> mips = myEdgeMips.get (edge.getPrimary());

      Vertex3d v = null;
      if (mips != null && mips.size() > 0) {
         // clockwise implies that the head of edge is on the inside
         boolean headIsInside = clockwiseContour;
         if (edge.getPrimary() != edge) {
            headIsInside = !headIsInside;
            edge = edge.getPrimary();
            if (debug) {
               System.out.println ("primary is " + toString(edge));
            }
            
         }
         if (debug) {
            System.out.println (
               "headInside=" + headIsInside + " clockwise=" + clockwiseContour);
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

   IntersectionPoint nearestMipToVertex (HalfEdge edge, Vertex3d v) {
      ArrayList<IntersectionPoint> mips = myEdgeMips.get (edge);
      if (mips != null && mips.size() > 0) {
         return (v == edge.getTail() ? mips.get(0) : mips.get(mips.size()-1));
      }
      else {
         return null;
      }
   }

   PenetrationRegion createRegion (
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
         region.myInsideFaces.add (lastFace);

         region.mySingleFace = lastFace;
         
         for (int k=0; k<c.size(); k++) {
            IntersectionPoint mip = c.get(k);
            Face face = c.findSegmentFace (k, mesh0);
            
            if (lastFace != face) {
               // we are leaving lastFace and entering face
               region.myInsideFaces.add (face);
               HalfEdge faceEdge = getPointEdge (mip, face);

               region.myInsideEdges.add (faceEdge.getPrimary());
               IntersectionPoint insideMip =
                  nearestInsideMip (mip, faceEdge, clockwiseContour);
               if (insideMip == null) {
                  Vertex3d v = getInsideVertex (mip, faceEdge, clockwiseContour);
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
                  IntersectionPoint mip = nearestMipToVertex (edge, vtx);
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

   int numNestedRegions (ArrayList<PenetrationRegion> regions) {
      int numNested = 0;
      for (PenetrationRegion r : regions) {
         if (r.mySingleFaceArea < 0) {
            numNested++;
         }
      }
      return numNested;
   }         

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

   static boolean isPointOutside (
      Point3d p0, Face face, IntersectionPoint pe, PolygonalMesh mesh) {

      PolygonalMesh otherMesh = pe.getOtherMesh (mesh);

      int ke = pe.contourIndex;
      IntersectionContour contour = pe.contour;
      IntersectionPoint pp = contour.getWrapped(ke-1);
      IntersectionPoint pa = pp;
      IntersectionPoint pb = pe;
      Face face0 = contour.findSegmentFace (pa, pb, otherMesh);
      Face face1 = null;
      int kb=ke+1;
      // advance forward and try to find a second face. Stop only if we run out
      // of contour, or if we move more than two segments away from the face.
      // Should change this to be "move to a segment that has no feature
      // connection with the face".

      int kf=kb; // kf is the most recent kb associated with face.
      //double dtol = EPS*mesh.getRadius();
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
      while (pb != pp && kb-kf <= 2); // pb.distance(pe) < dtol);

      Point3d p0loc = new Point3d();
      if (face0 != face1) {

         //System.out.println ("isPointOutside TWO FACE");
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
         //System.out.println ("isPointOutside ONE FACE");
         Vector3d nrm0 = new Vector3d();
         face0.getWorldNormal (nrm0);
         p0loc.sub (p0, pe);
         return p0loc.dot (nrm0) >= 0;
      }
   }  


   private void print (String msg, NearestPolygon3dFeature nfeat) {
      if (msg != null) {
         System.out.println (msg);
      }
      System.out.println ("  nverts=" + nfeat.numVertices());
      if (nfeat.numVertices() > 0) {
         System.out.println ("  pa=" + nfeat.getVertex(0).toString ("%12.8f"));
      }
      if (nfeat.numVertices() > 1) {
         System.out.println ("  pb=" + nfeat.getVertex(1).toString ("%12.8f"));
      }
      if (nfeat.numVertices() > 2) {
         System.out.println ("  pc=" + nfeat.getVertex(2).toString ("%12.8f"));
      }
   }

   private String getName (PenetrationRegion r) {
      String str = "[ ";
      for (IntersectionContour c : r.myContours) {
         str += ""+myContours.indexOf(c) + " ";
      }
      return str + "]";
   }

   public ArrayList<PenetrationRegion> getContainedRegions (
      PenetrationRegion region, Face face,
      ArrayList<PenetrationRegion> nested,
      int numCheck, boolean clockwiseContour) {

      double dtol = EPS*face.computeCircumference();
      NearestPolygon3dFeature[] nearestFeats =
         new NearestPolygon3dFeature[numCheck];
      for (int i=0; i<numCheck; i++) {
         PenetrationRegion r = nested.get(i);
         NearestPolygon3dFeature nfeat = new NearestPolygon3dFeature();
         nearestFeats[i] = nfeat;
         nfeat.init (r.getFirstContour().get(0), face.getWorldNormal(), dtol);
      }
      PolygonalMesh mesh = (PolygonalMesh)face.getMesh();
      for (IntersectionContour c : region.myContours) {
         int csize = c.size();

         int kstart = -1;
         Face lastFace = c.findSegmentFace (csize-1, mesh);
         for (int k=0; k<csize; k++) {
            Face segFace = c.findSegmentFace (k, mesh);
            if (segFace == face && lastFace != face) {
               kstart = k;
               break;
            }
            lastFace = segFace;
         }
         if (kstart == -1) {
            if (lastFace == face) {
               for (int k=0; k<=csize; k++) {
                  for (int i=0; i<numCheck; i++) {
                     if (k==0) {
                        nearestFeats[i].restart (c.get(0));
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
            int k = kstart;
            for (int j=0; j<csize; j++) {
               for (int i=0; i<numCheck; i++) {
                  if (j==0) {
                     nearestFeats[i].restart (c.getWrapped(k));
                     lastFace = face;
                  }
                  else {
                     Face segFace = c.findSegmentFace (k, mesh);
                     if (lastFace == face) {
                        nearestFeats[i].advance (c.getWrapped(k));
                     }
                     else if (segFace == face) {
                        nearestFeats[i].restart (c.getWrapped(k));
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

   public void addContainedRegions (
      PenetrationRegion region, Face face,
      ArrayList<PenetrationRegion> nested, boolean clockwiseContour) {
      
      if (nested.isEmpty()) {
         return;
      }
      int numCheck = nested.size();
      if (region.mySingleFaceArea > 0) {
         int k = 0;
         while (k < nested.size() &&
                Math.abs(nested.get(k).mySingleFaceArea) <= region.mySingleFaceArea) {
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

   void processSingleFaceRegions (
      ArrayList<PenetrationRegion> regions, Face face,
      ArrayList<PenetrationRegion> singleFaceRegions,
      PolygonalMesh mesh0, boolean clockwiseContour) {

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
         // remaining nested regions must belong to face containing regions.

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
               // System.out.println (
               //    " adding fcr region " + getName(r) + " to " + getName(region));
               region.myContours.add (r.getFirstContour());
            }
         }
         else {
            // multiple regions. Need to determine which nested region
            // belongs to which
            for (PenetrationRegion region : faceContainingRegions) {
               addContainedRegions (
                  region, face, nested, clockwiseContour);
            }
            if (nested.size() > 0) {
               throw new InternalErrorException (
                  "Cannot find parent regions for some nested regions");
            }
         }
      }
   }

   int createRegions (
      ArrayList<PenetrationRegion> regions, 
      PolygonalMesh mesh0, PolygonalMesh mesh1, boolean clockwiseContour) {

      int oldSize = regions.size();
      HashSet<IntersectionContour> unusedContours = 
         new HashSet<IntersectionContour>();
      // container to store single face regions
      HashMap<Face,ArrayList<PenetrationRegion>> singleFaceRegions =
         new HashMap<Face,ArrayList<PenetrationRegion>>();

      unusedContours.addAll (myContours);
      for (IntersectionContour contour : myContours) {
         if (unusedContours.contains(contour)) {
            PenetrationRegion region =
               createRegion (contour, mesh0, mesh1, clockwiseContour);
            unusedContours.removeAll (region.myContours);
            if (region.mySingleFace != null) {
               // handle single face regions separately
               Face face = region.mySingleFace;
               if (region.myContours.size() != 1) {
                  throw new InternalErrorException (
                     "Single face region has "+region.myContours.size()+
                     " contours instead of 1");
               }
               IntersectionContour c = region.getFirstContour();
               // there's probably a faster way to determine nesting than by
               // computing area. For instance, let (pa,pb) be two points on the
               // contour and nrm be the face normal. Then right = (pb - pa) X
               // defines the right turn direction as the contour moves along the
               // face. Now if inrm is the normal of the face on the other mesh,
               // the contour should be isolated if right.dot (inrm) < 0 for
               // clockwise contours and right.dot (inrm) > 0 for counter
               // clockwise ones.
               double area = c.computeFaceArea (face, clockwiseContour);
               // if (area < 0) {
               //    System.out.println (
               //       "Found nested contour "+contours.indexOf(c)+" "+ area);
               // }
               // else {
               //    System.out.println (
               //       "Found isolated contour "+contours.indexOf(c)+" "+ area);
               // }
               region.mySingleFaceArea = area;
               ArrayList<PenetrationRegion> regionsOnFace =
                  singleFaceRegions.get(face);
               if (regionsOnFace == null) {
                  regionsOnFace = new ArrayList<PenetrationRegion>();
                  singleFaceRegions.put(face, regionsOnFace);
               }
               regionsOnFace.add (region);
            }
            else {
               regions.add (region);
            }
         }
      }

      if (singleFaceRegions.size() > 0) {

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

//   void createRegions() {
//      myRegions0 = createRegions (myMesh0, myMesh1, false);
//      myRegions1 = createRegions (myMesh1, myMesh0, true);
//   }

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
            if (robustIntersectionWithFace (he.getPrimary(),
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
         if (robustIntersectionWithFace (he.getPrimary(),
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

   /*
    * Test for intersection using adaptive exact arithmetic and SOS tiebreaking.
    */
   public boolean robustIntersectionWithFace (
      HalfEdge he, Face face, IntersectionPoint mip, boolean edgeOnMesh0) {

      HalfEdge he0 = face.firstHalfEdge();
      Vertex3d v = he0.tail;
      if (v == he.head)
         return false;
      if (v == he.tail)
         return false;
      v = he0.head;
      if (v == he.head)
         return false;
      if (v == he.tail)
         return false;
      v = he0.getNext().head;
      if (v == he.head)
         return false;
      if (v == he.tail)
         return false;
      // face.updateWorldCoordinates();

      if (!RobustPreds.intersectEdgeFace (he, face, mip, edgeOnMesh0)) {
         return false;
      }

      mip.edge = he;
      mip.face = face;
      mip.edgeOnMesh0 = edgeOnMesh0;
      return true;
   }

}
