package maspack.collision;

//import maspack.geometry.AjlBvTree;
import maspack.collision.MeshIntersectionPoint;
import maspack.geometry.BVNode;
import maspack.geometry.BVTree;
import maspack.geometry.Boundable;
import maspack.geometry.RobustPreds;
import maspack.geometry.Vertex3d;
import maspack.geometry.HalfEdge;
import maspack.geometry.Face;
import maspack.geometry.PolygonalMesh;
//import maspack.geometry.AjlBvNode;
import maspack.util.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;


public class SurfaceMeshIntersector {
   public PolygonalMesh mesh0;
   public PolygonalMesh mesh1;


   //CollisionScenario scenario = new CollisionScenario();

   /*
    * Each contour is a list of MeshIntersectionPoints, each of which represents
    * the intersection of a HalfEdge and a Face.
    */
   public ArrayList<MeshIntersectionContour> contours;

   // While analyzing contour interiors, add an entry for every pair of nested contours found.
   public LinkedHashSet<MeshContourPair> nestedContours;
   
   /*
    * For each edge, remember all the intersection points, in order to determine
    * inside boundary vertices.
    */
   LinkedHashMap<HalfEdge,ArrayList<MeshIntersectionPoint>> edgeMips =
      new LinkedHashMap<HalfEdge,ArrayList<MeshIntersectionPoint>>();

   /*
    * For each edge-face pair between which an intersection has been found,
    * remember the contour. This allows duplicate intersections to be prevented.
    */
   protected HashMap<HalfEdge,HashMap<Face,MeshIntersectionContour>> edgeFaceIntersections =
      new HashMap<HalfEdge,HashMap<Face,MeshIntersectionContour>>();

   /*
    * For convenience, points to the list of edgeEdgeContacts in the current
    * contactInfo.
    */
   ArrayList<EdgeEdgeContact> edgeEdgeContacts;


   // boolean treesEqual (AjlBvNode node0, BVNode node1) {
   //    AjlBvNode child0 = node0.getChild0();
   //    AjlBvNode child1 = node0.getChild1();
   //    int numChildren0 = 0;
   //    if (child0 != null) {
   //       numChildren0++;
   //    }
   //    if (child1 != null) {
   //       numChildren0++;
   //    }
   //    if (node1.numChildren() != numChildren0) {
   //       System.out.println (
   //          node0.myNumber+
   //          ": num childen differ: Ajl="+numChildren0+
   //          " BV="+node1.numChildren());
   //       return false;
   //    }
   //    if (numChildren0 > 0) {
   //       BVNode childBV = node1.getFirstChild();
   //       if (child0 != null) {
   //          if (!treesEqual (child0, childBV)) {
   //             return false;
   //          }
   //          childBV = childBV.getNext();
   //       }
   //       if (child1 != null) {
   //          if (!treesEqual (child1, childBV)) {
   //             return false;
   //          }
   //       }
   //    }
   //    return true;
   // }

   // private void setPrimaryEdges (PolygonalMesh mesh) {
   //    for (Face face : mesh.getFaces()) {
   //       HalfEdge he0 = face.firstHalfEdge();
   //       HalfEdge he = he0;
   //       do {
   //          he.isPrimary();
   //          he = he.getNext();
   //       }
   //       while (he != he0);
   //    }
   // }

   // private void testPrimaryEdges (PolygonalMesh mesh) {
   //    for (Face face : mesh.getFaces()) {
   //       HalfEdge he0 = face.firstHalfEdge();
   //       HalfEdge he = he0;
   //       do {
   //          if (he.isPrimary() == he.opposite.isPrimary()) {
   //             throw new InternalErrorException (
   //                "he and opp have identical primary settings: " +
   //                he.isPrimary() + ", mesh=" + mesh);
   //          }
   //          he = he.getNext();
   //       }
   //       while (he != he0);
   //    }
   // }


   public static long renderTime = -1;

   /*
    * Main interface for collision -- called by CollisionPair. Returns a
    * ContactInfo populated with the results of the collision analysis. Returns
    * null if there is no collision. mesh0, mesh1 are the candidate colliding
    * surface meshes, which can be the same mesh.
    * 
    * isRigidBodyRigidBody should be specified as true only if mesh0 and mesh1
    * both represent rigid bodies. This controls the type of data returned in
    * ContactInfo.
    */
   public boolean findContours (PolygonalMesh mesh0, PolygonalMesh mesh1) {
      if (mesh0 != mesh1) {
         if (mesh0.canSelfIntersect)
            if (findContours (mesh0, mesh0))
               throw new RuntimeException ("self-intersecting mesh");
         if (mesh1.canSelfIntersect)
            if (findContours (mesh1, mesh1))
               throw new RuntimeException ("self-intersecting mesh");
      }

      // for (Vertex3d v: mesh0.getVertices())
      // System.out.println(v.getWorldPoint());
      // for (Vertex3d v: mesh1.getVertices())
      // System.out.println(v.getWorldPoint());

      // Collect timing data.
      long time = System.nanoTime();
      SurfaceMeshCollider.collisionMetrics.cullTime -= time;
      this.mesh0 = mesh0;
      this.mesh1 = mesh1;
      mesh0.updateFaceNormals();
      mesh1.updateFaceNormals();
      if (!mesh0.isTriangular() | !mesh1.isTriangular())
         throw new RuntimeException ("collision with non-triangular mesh");

      // Find pairs of non-disjoint leaf nodes from the bounding volume
      // hierarchies.
      //scenario.resetLeafNodes();
      // Get nodes0, nodes1 before calling getIntersectingNodes, as they may be
      // swapped.

      // setPrimaryEdges (mesh0);
      // setPrimaryEdges (mesh1);

      // testPrimaryEdges (mesh0);
      // testPrimaryEdges (mesh1);

      // ArrayList<AjlBvNode> nodes0 = scenario.intersectingLeafNodesThis;
      // ArrayList<AjlBvNode> nodes1 = scenario.intersectingLeafNodesOther;
      // AjlBvTree bvh0 = mesh0.getBvHierarchy();
      // AjlBvTree bvh1 = mesh1.getBvHierarchy();
      // bvh0.getIntersectingNodes (bvh1, scenario);

      BVTree bvh0 = mesh0.getBVTree();
      BVTree bvh1 = mesh1.getBVTree();
      ArrayList<BVNode> nodes0 = new ArrayList<BVNode>();
      ArrayList<BVNode> nodes1 = new ArrayList<BVNode>();
      bvh0.intersectTree (nodes0, nodes1, bvh1);

      // AjlBvTree ajl0 =  mesh0.getBvHierarchy();
      // AjlBvTree ajl1 =  mesh1.getBvHierarchy();
      // int numAjl0 = ajl0.numberNodes();
      // int numAjl1 = ajl1.numberNodes();
      // int numBvh0 = bvh0.numberNodes();
      // int numBvh1 = bvh1.numberNodes();

      // if (!treesEqual (ajl0.getRoot(), bvh0.getRoot())) {
      //    System.out.println ("treesEqual0=false");
      // }
      // if (!treesEqual (ajl1.getRoot(), bvh1.getRoot())) {
      //    System.out.println ("treesEqual1=false");
      //    ajl1.printNumLeafFaces ("ajl1 ");
      //    bvh1.printNumLeafFaces ("bvh1 ");
      // }

      /*
       * Find contours of intersections between edges and faces from each pair
       * of non-disjoint bounding volumes.
       */
      boolean intersected = findIntersectionContours (nodes0, nodes1);
      time = System.nanoTime();
      SurfaceMeshCollider.collisionMetrics.scanTime += time;
      if (intersected) {
         SurfaceMeshCollider.collisionMetrics.regionTime -= time;
         analyzeContours();
         time = System.nanoTime();
         SurfaceMeshCollider.collisionMetrics.regionTime += time;
         // for (MeshIntersectionContour contour : contours) System.out.printf("contact area = %e\n", contour.getArea());
      }
      return intersected;
   }

   // boolean findIntersectionContoursAjl (
   //    ArrayList<AjlBvNode> nodes0, ArrayList<AjlBvNode> nodes1) {
   //    contours = new ArrayList<MeshIntersectionContour>();
   //    edgeFaceIntersections.clear();
   //    edgeMips.clear();
   //    boolean answer = false;
   //    for (int i = 0; i < nodes0.size(); i++) {
   //       AjlBvNode node0 = nodes0.get (i);
   //       AjlBvNode node1 = nodes1.get (i);
   //       // edgeRegion=true corresponds to node0, mesh0
   //       boolean nodesIntersected =
   //          findIntersectionContours (node1.faces, node0.faces, true);
   //       // Find and add new contours.  next line is probably unnecessary if
   //       // mesh0 == mesh1.
   //       if (!nodesIntersected)
   //          nodesIntersected =
   //             findIntersectionContours (node0.faces, node1.faces, false);
   //       answer = answer | nodesIntersected;
   //    }
   //    return answer;
   // }

   boolean findIntersectionContours (
      ArrayList<BVNode> nodes0, ArrayList<BVNode> nodes1) {
      contours = new ArrayList<MeshIntersectionContour>();
      edgeFaceIntersections.clear();
      edgeMips.clear();
      boolean answer = false;
      for (int i = 0; i < nodes0.size(); i++) {
         BVNode node0 = nodes0.get (i);
         BVNode node1 = nodes1.get (i);
         // edgeRegion=true corresponds to node0, mesh0
         boolean nodesIntersected =
            findIntersectionContours (
               node1.getElements(), node0.getElements(), true);
         // Find and add new contours.  next line is probably unnecessary if
         // mesh0 == mesh1.
         if (!nodesIntersected)
            nodesIntersected =
               findIntersectionContours (
                  node0.getElements(), node1.getElements(), false);
         answer = answer | nodesIntersected;
      }
      return answer;
   }

   // public boolean findIntersectionContours (
   //    Face[] faces, HalfEdge[] edges, boolean initialEdgeRegion) {
   //    boolean gotOne = false;
   //    for (HalfEdge e : edges) {
   //       HalfEdge ep = e.getPrimary();
   //       if (!e.isPrimary()) {
   //          System.out.println ("Not primary!!!");
   //       }
   //       for (Face f : faces) {
   //          if (ep.intersectionWithFace (f)) {
   //             gotOne =
   //                gotOne | findIntersectionContour (ep, f, initialEdgeRegion);
   //          }
   //       }
   //    }
   //    return gotOne;
   // }

   // public boolean findIntersectionContours (
   //    Face[] faces0, Face[] faces1, boolean initialEdgeRegion) {
   //    boolean gotOne = false;
   //    for (Face f1 : faces1) {
   //       HalfEdge he0 = f1.firstHalfEdge();
   //       HalfEdge he = he0;
   //       do {
   //          if (he.isPrimary()) {
   //             for (Face f0 : faces0) {
   //                if (he.intersectionWithFace (f0)) {
   //                   gotOne |= findIntersectionContour (
   //                      he, f0, initialEdgeRegion);
   //                }
   //             }
   //          }
   //          he = he.getNext();
   //       }
   //       while (he != he0);
   //    }
   //    return gotOne;
   // }

   public boolean findIntersectionContours (
      Boundable[] elems0, Boundable[] elems1, boolean initialEdgeRegion) {
      boolean gotOne = false;
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
                        if (he.intersectionWithFace (f0)) {
                           gotOne |= findIntersectionContour (
                              he, f0, initialEdgeRegion);
                        }
                     }
                  }
               }
               he = he.getNext();
            }
            while (he != he0);
         }
      }
      return gotOne;
   }

   /*
    * Given a Face and a HalfEdge from two PolygonalMeshes that intersect at an
    * initial MeshIntersectionPoint, trace their intersection contour. Return an
    * MeshIntersectionContour of MeshIntersectionPoints. Stop tracing when: -
    * the contour is closed, - a duplicate intersection point is encountered -
    * the maximum number of points (maxContourPoints) is exceeded, or - the edge
    * of the mesh surface is encountered.
    */
   private boolean findIntersectionContour (
      HalfEdge edge, Face f, boolean initialEdgeRegion) {
      MeshIntersectionContour contour = new MeshIntersectionContour (this);
      if (!robustIntersectionWithFace (edge, f, contour.workPoint))
         return false;
      contour.edgeRegion = initialEdgeRegion;
      MeshIntersectionPoint mip = contour.workPoint;
      if (!contour.addWorkPoint())
         return false; // May be rejected if it's a duplicate intersection.

      long t = System.nanoTime();
      SurfaceMeshCollider.collisionMetrics.scanTime += t;
      SurfaceMeshCollider.collisionMetrics.traceTime -= t;

      // There are two possible directions to trace. First choose the one
      // associated with this half edge.
      Face edgeFace = mip.edge.getFace();
      if (edgeFace != null)
         traceIntersectionContour (contour, edgeFace);
      if (contour.isContinuable) {
         // The contour encountered a mesh edge and is open. Continue the trace
         // in the opposite direction.
         HalfEdge opposite = mip.edge.opposite;
         if (opposite != null) {
            edgeFace = opposite.getFace();
            if (edgeFace != null) {
               contour.reverse();
               contour.edgeRegion = initialEdgeRegion;
               traceIntersectionContour (contour, edgeFace);
            }
         }
      }
      if (contour.isClosed) {
         contours.add (contour);
      }
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
      t = System.nanoTime();
      SurfaceMeshCollider.collisionMetrics.traceTime += t;
      SurfaceMeshCollider.collisionMetrics.scanTime -= t;
      return contour.isClosed;
   }

   /*
    * contour is an ArrayList of MeshIntersectionPoints with one element. The
    * first element is the intersection of edge and otherFace. edgeFace is an
    * adjacent edge to otherFace. It provides the initial search direction. Find
    * successive points in the intersection to continue the contour until it is
    * closed or an edge is encountered or it becomes too large.
    * 
    * The contour has flags isClosed, isContinuable, and isFull which are set
    * when points are added to it.
    */
   private void traceIntersectionContour (
      MeshIntersectionContour contour, Face anEdgeFace) {
      SurfaceMeshCollider.collisionMetrics.walkTime -= System.nanoTime();
      MeshIntersectionPoint mip = contour.get (contour.size() - 1);
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
            contour.switchEdgeRegion();
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
      if (!contour.isClosed)
         SurfaceMeshCollider.collisionMetrics.openHalfContours++;
      SurfaceMeshCollider.collisionMetrics.walkTime += System.nanoTime();
   }

   /*
    * Remember all the intersection points for the edge, in sorted order of
    * increasing distance from edge.tail. Mark coincident mips.
    */
   void setEdgeIntersectionPoints (MeshIntersectionPoint mip) {
      ArrayList<MeshIntersectionPoint> mips = edgeMips.get (mip.edge);
      if (mips == null) {
         mips = new ArrayList<MeshIntersectionPoint>();
         mips.add (mip);
         edgeMips.put (mip.edge, mips);
         return;
      }
      int i = -1;
      int q = 0;
      boolean found;
      MeshIntersectionPoint m = null;
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
      /*
       * for (i=0; i<mips.size()-1; i++) if
       * (RobustPreds.closestIntersection(mips.get(i).face, mips.get(i).edge,
       * mips.get(i+1).face) != -1) throw new RuntimeException("bad mips");
       */
   }

   /*
    * Analyze the intersection contours to determine inside vertices, faces, and
    * edges for each region.
    */
   void analyzeContours() {
      // Initialize data structures used to hold the results.
      LinkedHashSet<HalfEdge> boundaryEdges = new LinkedHashSet<HalfEdge>();
      nestedContours = new LinkedHashSet<MeshContourPair>();
      LinkedHashMap<Vertex3d, MeshIntersectionContour> vertexContours = new LinkedHashMap<Vertex3d, MeshIntersectionContour>(); 
      for (MeshIntersectionContour contour : contours)
         contour.findInsideBoundaryFeatures (boundaryEdges, vertexContours);
      for (MeshIntersectionContour contour : contours)
         contour.findRegionInteriors (boundaryEdges, vertexContours);
      if (!nestedContours.isEmpty()) 
         combineNestedContours();
   }

   public void combineNestedContours() {
      int nContours = contours.size();
      for (int i=0; i<nContours; i++) {
         MeshIntersectionContour ci = contours.get(i);
         if (ci.isNested) {
            for (int j=0; j<nContours; j++) {
               if (i != j) {
                  MeshIntersectionContour cj = contours.get(j);
                  if (cj.isNested) {
                     if (nestedContours.contains(new MeshContourPair(ci, cj))) {
                        MeshIntersectionContour contourKeep, contourDiscard;
                        if (
                           (ci.size() >= cj.size())
                           || (
                              (i < j) && (ci.size() == cj.size())
                               )
                            ) {
                           contourKeep = ci;
                           contourDiscard = cj;
                        } else {
                           contourKeep = cj;
                           contourDiscard = ci;
                        }
                        contourKeep.insideFaces0.addAll(contourDiscard.insideFaces0);
                        contourDiscard.insideFaces0.clear();
                        contourKeep.insideFaces1.addAll(contourDiscard.insideFaces1);
                        contourDiscard.insideFaces1.clear();

                        contourKeep.insideEdges0.addAll(contourDiscard.insideEdges0);
                        contourDiscard.insideEdges0.clear();
                        contourKeep.insideEdges1.addAll(contourDiscard.insideEdges1);
                        contourDiscard.insideEdges1.clear();

                        contourKeep.insideVertices0.addAll(contourDiscard.insideVertices0);
                        contourDiscard.insideVertices0.clear();
                        contourKeep.insideVertices1.addAll(contourDiscard.insideVertices1);
                        contourDiscard.insideVertices1.clear();
                     }
                  }
               }
            }
         }
      }
   }
   
   public void setDegenerate() {
      ContactInfo contactInfo = new ContactInfo (mesh0, mesh1);
      contactInfo.contours = contours;
      // if (SurfaceMeshCollider.renderContours)
      //    SurfaceMeshCollider.contactInfoRenderer.addContactInfo (contactInfo);
//      throw new RuntimeException ("degenerate intersection");
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
      Face face, Face otherFace,
      HalfEdge excludeEdge, MeshIntersectionContour contour) {
      HalfEdge he0 = face.firstHalfEdge();
      HalfEdge he = he0;
      do {
         if (he != excludeEdge & he != excludeEdge.opposite) {
            if (robustIntersectionWithFace (he.getPrimary(),
               otherFace, contour.workPoint)) {
               if (contour.addWorkPoint())
                  return he;
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
      Face face, Face otherFace, MeshIntersectionContour contour) {
      HalfEdge he0 = face.firstHalfEdge();
      HalfEdge he = he0;
      do {
         if (robustIntersectionWithFace (he.getPrimary(),
            otherFace, contour.workPoint)) {
            if (contour.addWorkPoint())
               return he;
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
      HalfEdge he, Face face, MeshIntersectionPoint mip) {

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

      if (!RobustPreds.intersectEdgeFace (he, face, mip))
         return false;
      mip.edge = he;
      mip.face = face;
      return true;
   }

}
