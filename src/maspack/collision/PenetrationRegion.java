package maspack.collision;

import java.util.*;

import maspack.util.*;
import maspack.matrix.*;
import maspack.geometry.*;

/**
 * Describes a penetration region for an oriented triangular mesh. This is a
 * single connected region of the mesh that is on the "inside" of another
 * oriented mesh when the two meshes intersect. It is bounded by one or more
 * intersection contours, and contains a set of edges that are associated with
 * the intersection and a set of vertices that are inside the other mesh.  The
 * intersection between two meshes will generally result in one or more
 * penetration regions for each mesh. The number of regions on each mesh
 * may differ, and they will not necessarily shared the same contours.
 *
 * <p> In some cases the penetration region may intersect only a single face of
 * the intersecting mesh. The region is then referred to as a "single face
 * region", and its bounding contours all lie within the single face of the
 * intersecting mesh.
 */
public class PenetrationRegion {

   LinkedHashSet<Vertex3d> myInsideVertices;
   LinkedHashSet<Face> myInsideFaces;
   LinkedHashSet<HalfEdge> myInsideEdges;

   LinkedHashSet<IntersectionContour> myContours;

   private static double DOUBLE_PREC = 1e-16;
   //private static double EPS = 1e-10;
   
   Face mySingleFace;
   double mySingleFaceArea;
   
   private double myArea = -1;
   PolygonalMesh myMesh = null;
   boolean myClockwise = false;

   /**
    * Creates a new, empty penetration region for a specified mesh.
    *
    * @param mesh mesh to which the region belongs
    * @param clockwise if <code>true</code>, indicates that the
    * bounding contours for the region are oriented clockwise
    * with respect to the mesh.
    */
   public PenetrationRegion (PolygonalMesh mesh, boolean clockwise) {
      myInsideVertices = new LinkedHashSet<Vertex3d>();
      myInsideFaces = new LinkedHashSet<Face>();
      myInsideEdges = new LinkedHashSet<HalfEdge>();

      myContours = new LinkedHashSet<IntersectionContour>();
      
      mySingleFace = null;
      myMesh = mesh;
      myClockwise = clockwise;
   }

   /**
    * Returns the contours which bound this region.
    *
    * @return hash set of the bounding contours. Should not be modified.
    */
   public LinkedHashSet<IntersectionContour> getContours() {
      return myContours;
   }

   /**
    * Returns the number of contours bounding this region.
    * 
    * @return number of bounding contours
    */
   public int numContours() {
      return myContours.size();
   }

   /**
    * Returns the vertices of this region which are inside the intersecting
    * mesh.
    *
    * @return hash set of the inside vertices. Should not be modified.
    */
   public LinkedHashSet<Vertex3d> getInsideVertices() {
      return myInsideVertices;
   }
   
   /**
    * Returns the number of inside vertices in this region.
    * 
    * @return number of inside vertices
    */
   public int numInsideVertices() {
      return myInsideVertices.size();
   }

   /**
    * Returns <code>true</code> if a specified vertex is an inside
    * vertex of this region.
    * 
    * @param vtx vertex to test
    * @return <code>true</code> if <code>vtx</code> is inside
    */
   public boolean isInsideVertex (Vertex3d vtx) {
      return myInsideVertices.contains(vtx);
   }

   /**
    * Returns the faces of this region which are completely or partially
    * inside the intersecting mesh.
    *
    * @return hash set of the inside faces. Should not be modified.
    */
   public LinkedHashSet<Face> getInsideFaces() {
      return myInsideFaces;
   }
   
   /**
    * Returns the number of inside faces in this region.
    * 
    * @return number of inside faces
    */
   public int numInsideFaces() {
      return myInsideFaces.size();
   }

   /**
    * Returns <code>true</code> if a specified face is partly
    * or completely inside this region.
    * 
    * @param face face to test
    * @return <code>true</code> if <code>face</code> is partly or
    * completely inside
    */
   public boolean isInsideFace (Face face) {
      return getInsideFaces().contains(face);
   }

   /**
    * Returns the edges of this region which are completely or partially
    * inside the intersecting mesh. Note that only primary edges
    * are stored, so if <code>he</code> is an inside edge, 
    * <code>he.opposite</code> will not be.
    *
    * @return hash set of the inside edges. Should not be modified.
    */
   public LinkedHashSet<HalfEdge> getInsideEdges() {
      return myInsideEdges;
   }
   
   /**
    * Returns the number of inside edges in this region.
    * 
    * @return number of inside edges
    */
   public int numInsideEdges() {
      return myInsideEdges.size();
   }

   /**
    * Returns <code>true</code> if a specified edge is partly
    * or completely inside this region. The test will be made
    * using <code>he.getPrimary()</code>, so that if <code>he</code>
    * is inside, then <code>he.opposite</code> will also be inside.
    * 
    * @param he edge to test
    * @return <code>true</code> if <code>he</code> 
    * or <code>he.opposite</code> is partly or completely inside
    */
   public boolean isInsideEdge (HalfEdge he) {
      return myInsideEdges.contains(he.getPrimary());
   }
   
   /**
    * Returns an array of the indices of the contours of this region with
    * respect to another reference list of contours. Region contours which are
    * not contained within the reference list will be assigned an index of
    * -1. This method is used for testing and debugging.
    */
   int[] getContourIndices (List<IntersectionContour> referenceContours) {
      ArrayList<Integer> list = new ArrayList<Integer>();
      for (IntersectionContour c : myContours) {
         list.add (referenceContours.indexOf (c));
      }
      return ArraySupport.toIntArray (list);
   }

   /**
    * Returns the mesh associated with this region.
    * 
    * @return mesh associated with this region
    */
   public PolygonalMesh getMesh() {
      return myMesh;
   }
   
   /**
    * Indicates if the contours of this region are oriented clockwise
    * with respect to the region's mesh.
    * 
    * @return <code>true</code> if contours are oriented clockwise
    */
   public boolean hasClockwiseContours() {
      return myClockwise;
   }
   
   /**
    * Returns true if this region equals another. Two regions are equal if they
    * contain the same contours, inside vertices, and intersection edges.
    */
   public boolean equals (PenetrationRegion region) {

      if (!myInsideVertices.equals (region.myInsideVertices)) {
         return false;
      }
      if (!myInsideEdges.equals(region.myInsideEdges)) {
         return false;
      }
      if (!myContours.equals(region.myContours)) {
         return false;
      }
      return true;
   }

   /**
    * Convenience method that returns the first contour associated
    * with this region.
    */
   IntersectionContour getFirstContour() {
      Iterator<IntersectionContour> it = myContours.iterator();
      if (it.hasNext()) {
         return it.next();
      }
      else {
         return null;
      }
   }

   /**
    * Returns a string representation of this region consisting
    * of the indices of the inside vertices and the indices of
    * the contours with respect to a reference list.
    */
   public String toString (List<IntersectionContour> contours) {
      String vertexStr = 
         ArraySupport.toString(Vertex3d.getIndices(myInsideVertices));
      String contourStr = 
         ArraySupport.toString(getContourIndices(contours));
      return "[ vertices=[ "+vertexStr+" ] contours=[ "+contourStr+" ]]";
   }

   /**
    * Returns the surface area for this penetration region. This is the area on
    * this region's mesh that is bounded by the region's intersection
    * contours. If not already known, the area is computed and then cached
    * internally.
    * 
    * @return surface area of this penetration region.
    */
   public double getArea() {
      if (myArea == -1) {
         myArea = computeArea ();
      }
      return myArea;
   }
   
   /**
    * Cpmputes the surface area for this penetration region. This is the area
    * on this region's mesh that is bounded by the region's intersection
    * contours. This is the sum of the areas of all the faces that are
    * completely contained within the penetration region, plus all the partial
    * areas of faces that are traversed by the contour.
    */
   double computeArea () {

      // We want to sum the areas of all faces that are completely inside the
      // penetration region, plus the "inside" areas of faces that are
      // intersected by the contours and hence are partially inside the
      // penetration region.

      // To compute the "inside" area of a partially inside face, we actually
      // add up the area that is "outside" the contours and then subtract this
      // from the face's area. We do this because the outside areas for the
      // contours do not interfere with each other and so can be summed for
      // each face.
      double area = 0;
      // keep track of outside areas for each face:
      HashMap<Face,Double> faceAreas = new HashMap<Face,Double>();
      // for each contour, add the outside area on each face that it partially
      // intersects. 
      for (IntersectionContour c : myContours) {
         computePartialOutsideFaceAreas (c, myMesh, myClockwise, faceAreas);
      }
      // to get the final area, start by summing the negative outside areas.
      for (Map.Entry<Face,Double> entry : faceAreas.entrySet()) {
         Face face = entry.getKey();
         if (!isInsideFace (face)) {
            throw new InternalErrorException (
               "Visited face "+face.getIndex()+" is not an internal face");
         }
         double a = entry.getValue();
         area -= a;
      }
      // then add the area of all interior faces, partial and otherwise.
      for (Face face : getInsideFaces()) {
         area += face.computeArea();
      }
      return area;
   }

   /**
    * Stores per-face information that is used when computing the region's area.
    */
   private class FaceInfo {
      IntersectionPoint first; // first point seen that involved this face
      double area; // cumulative area involving this face

      void setFirst (IntersectionPoint first) {
         this.first = first;
         this.area = 0;
      }
   }

   /**
    * Compute the face area contribution formed by the boundary of the face
    * lying between the points plast and pnew, oriented either clockwise or
    * counter-clockwise depending on whether <code>clockwise</code> is
    * <code>true</code>. The area contribution is formed by adding the area of
    * triangles formed by the first face vertex (p0) and the boundary line
    * segments. Since these areas will be zero except for any boundary segment
    * that lies on the edge opposite p0, we need to compute at most one such
    * triangle area when a boundary segment does in fact lie on the opposite
    * edge.
    */
   private double computeBoundaryArea (
      Face face, IntersectionPoint plast, IntersectionPoint pnew) {

      HalfEdge he0 = face.firstHalfEdge();
      HalfEdge he1 = he0.getNext();
      HalfEdge he2 = he1.getNext();

      HalfEdge lastEdge = SurfaceMeshIntersector.getPointEdge (plast, face);
      HalfEdge newEdge = SurfaceMeshIntersector.getPointEdge (pnew, face);

      if (debugFace == face.getIndex()) {
         System.out.println ("compute boundary area");
         System.out.println ("  plast=" + plast.toString("%12.8f"));
         System.out.println ("   pnew=" + pnew.toString("%12.8f"));
      }

      Point3d p0 = new Point3d();
      getWorldPoint (p0, he0.getHead());

      double area = 0;
      if (lastEdge == he2) {
         Point3d px = new Point3d();
         if (newEdge == he1) {
            getWorldPoint (px, he1.getHead());
         }
         else if (newEdge == he0) {
            getWorldPoint (px, he2.getHead());
         }
         else {
            px.set (pnew);
         }
         area = computeArea (face, p0, plast, px);
      }
      else if (newEdge == he2) {
         Point3d px = new Point3d();
         if (lastEdge == he1) {
            getWorldPoint (px, he1.getHead());        
         }
         else { // lastEdge == he0
            getWorldPoint (px, he2.getHead());        
         }
         area = computeArea (face, p0, px, pnew);
      }
      else {
         area = 0;
      }
      return area;
   }

   /**
    * Computes the area of the triangle p0, p1, p2 located in the plane
    * of the indicated face. The area will be positive if the triangle
    * point are arranged counter-clockwise.
    */
   private double computeArea (Face face, Point3d p0, Point3d p1, Point3d p2) {
      Vector3d nrm = new Vector3d(face.getNormal());
      MeshBase mesh = face.getMesh();
      if (!mesh.meshToWorldIsIdentity()) {
         nrm.transform (mesh.getMeshToWorld());
      }
      Vector3d del10 = new Vector3d();
      Vector3d del20 = new Vector3d();
      Vector3d xprod = new Vector3d();
      del10.sub (p1, p0);
      del20.sub (p2, p0);
      xprod.cross(del10, del20);
      return xprod.dot (nrm)/2;
   }  

   /**
    * Gets the postion associated with a vertex, transforming it into world
    * coordinates if necessary.
    */
   private void getWorldPoint (Point3d pnt, Vertex3d vtx) {
      MeshBase mesh = vtx.getMesh();
      if (!mesh.meshToWorldIsIdentity()) {
         pnt.transform (mesh.getMeshToWorld(), vtx.pnt);
      }
      else {
         pnt.set (vtx.pnt);
      }
   }

   /**
    * Adds <code>area</code> to the existing area already stored for a
    * specified <code>face</code> in the map <code>faceAreas</code>.
    */
   private void addArea (HashMap<Face,Double> faceAreas, Face face, double area) {
      Double a = faceAreas.get(face);
      if (a == null) {
         faceAreas.put (face, area);
      }
      else {
         faceAreas.put (face, a+area);
      }
   }

   private static int debugFace = -1;

   /**
    * Gets a <code>FaceInfo</code> object for a specified <code>face</code>
    * from <code>map</code>, allocating a new object if one does not yet exist.
    */
   private FaceInfo getInfo (Face face, HashMap<Face,FaceInfo> map) {
      FaceInfo finfo = map.get(face);
      if (finfo == null) {
         finfo = new FaceInfo();
         map.put (face, finfo);
      }
      return finfo;
   }

   /**
    * Returns <code>true</code> if a specified <code>face</code> is outside
    */
   private boolean isFaceOutside (
      Face face, IntersectionPoint pe, PolygonalMesh mesh) {

      Point3d centroid = new Point3d();
      face.computeWorldCentroid(centroid);

      return SurfaceMeshIntersector.isPointOutside (
         centroid, face, pe, mesh);
   }

   /**
    * Computes the outside area on all the faces of mesh that are intersected
    * by a specific contour, and accumulates these areas in the map
    * <code>faceAreas</code>. The variable <code>clockwise</code> is used to
    * indicate whether the mesh is oriented clockwise with respect to the
    * inside area.
    */
   private void computePartialOutsideFaceAreas (
      IntersectionContour contour, PolygonalMesh mesh,
      boolean clockwise, HashMap<Face,Double> faceAreas) {

      int csize = contour.size();

      // Every adjacent pair of contour points, as indexed by (k,k+1), is
      // associated with a specific face on the mesh. Start by trying to find a
      // point where this face changes; i.e., the faces associated with (k-1,k)
      // and (k,k+1) are different. Store the index for this point in kenter.
      int kenter = -1;
      Face lastFace = contour.findSegmentFace (csize-1, mesh);
      for (int k=0; k<csize; k++) {
         Face face = contour.findSegmentFace (k, mesh);
         if (lastFace != face) {
            kenter = k;
            break;
         }
         lastFace = face;
      }
      Point3d p0 = new Point3d();
      if (kenter == -1) {
         // If no face transition point is found, then the contour is closed
         // and entirely on one face (which equals lastFace).  Compute the area
         // by summing the areas of the triangles formed by the first point on
         // the face (p0) and the edges of the contour.

         // System.out.println ("contour in on one face"); 
         getWorldPoint (p0, lastFace.firstHalfEdge().getHead());
         double area = 0;
         for (int i=0; i<csize; i++) {
            IntersectionPoint pa = contour.get(i);
            IntersectionPoint pb = contour.getWrapped(i+1);
            area += computeArea (lastFace, p0, pa, pb);
         }
         if (!clockwise) {
            // Result will be negative if clockwise, so correct for this
            area = -area;
         }
         if (area < 0) {
            // A negative area means that the outside area is actually outside
            // the contour, rather than inside
            area += lastFace.computeArea();
         }
         addArea (faceAreas, lastFace, area);
      }
      else {
         //System.out.println ("contour crosses faces");

         // Contour crosses more than one face, or is open.  Traverse the
         // contour starting at kenter, keeping track of which face the contour
         // is on. Every time the contour crosses a face, we compute the
         // corresponding partial outside area on the face. The sum of all
         // such areas forms the total outside area for the face.

         // Each partial outside area is the area of the polygon formed by the
         // intersection of the face with the (local) portion of the contour
         // crossing it. The edges of this polygon are formed by the portion of
         // the contour between the entry and exit points, plus the appropriate
         // portion of the face boundary between the exit and entry points.
         // The area is computed by summing the areas of triangles formed by
         // the first point on the face (p0) with the edges of this
         // polygon. The area contributions for the face boundary are computed
         // using <code>computeBoundaryArea()</code>.

         // As the computation proceeds, information for each face, including
         // partial areas and the first point where the contour enters the the
         // face, are accumulated in a map of FaceInfo structures:
         HashMap<Face,FaceInfo> infoMap = new HashMap<Face,FaceInfo>();
         Face face = contour.findSegmentFace (kenter, mesh);         
         FaceInfo finfo = getInfo (face, infoMap);
         finfo.setFirst (contour.get(kenter));

         IntersectionPoint pentry; // point where the contour enters a face
         pentry = finfo.first;

         getWorldPoint (p0, face.firstHalfEdge().getHead());   
         int k = kenter;
         int nedges; // total number of edges associated with the contour
         nedges = contour.isClosed() ? csize : csize-1;
         for (int i=0; i<nedges; i++) {
            IntersectionPoint pa = contour.getWrapped(k);
            IntersectionPoint pb = contour.getWrapped(k+1);
            // compute area contribution for this contour edge:
            double area = computeArea (face, p0, pa, pb);
            if (!clockwise) {
               // result will be negative if !clockwise, so correct for this
               area = -area;
            }
            if (face.getIndex() == debugFace) {
               System.out.println ("face" + debugFace + " area=" + area);
               System.out.println (" p0=" + p0.toString ("%12.8f"));
               System.out.println (" pa=" + pa.toString ("%12.8f"));
               System.out.println (" pb=" + pb.toString ("%12.8f"));
            }
            finfo.area += area;
            Face nextFace = contour.findSegmentFace (k+1, mesh); 
            if (nextFace != face) {
               // exiting face. 
               finfo = getInfo (face, infoMap);
               // compute area contribution for face boundary from exit to entry
               area = computeBoundaryArea (face, pb, pentry);
               if (!clockwise) {
                  // result will be negative if !clockwise, so correct for this
                  area = -area;
               }
               finfo.area += area;

               if (i < nedges-1) {
                  // entering new face, so reset variables accordingly
                  pentry = pb;
                  finfo = getInfo (nextFace, infoMap);
                  if (finfo.first == null) {
                     finfo.setFirst (pb);
                  }
                  getWorldPoint (p0, nextFace.firstHalfEdge().getHead());
                  face = nextFace;
               }
            }
            k++;
         }
         // For each face encountered by the contour, add the outside area for
         // this contour (stored in the <code>FaceInfo</code> map) to the total
         // outside area for all contours (stored in the
         // <code>faceAreas</code> map).
         for (Map.Entry<Face,FaceInfo> entry : infoMap.entrySet()) {
            finfo = entry.getValue();
            face = entry.getKey();
            double area = finfo.area;
            double rad = mesh.getRadius();
            double atol = DOUBLE_PREC*rad*rad; // tolerance for area value
            if (area < atol) {
               // computed area is negative
               if (area < -atol) {
                  // computed area is truely negative, in which case the
                  // outside area is actually faceArea - area
                  area += face.computeArea();
               }
               else {
                  // computed area is effectively zero. Check if the face is
                  // actually outside the intersection region. If it is, then
                  // add the face area to the outside area, so that when the
                  // outside area is negated and added to the face area (at the
                  // end of computeArea()), the resulting area will cancel out
                  // to zero.
                  if (isFaceOutside (face, finfo.first, mesh)) {
                     area += face.computeArea();                  
                  }
               }
            }
            addArea (faceAreas, face, area);
         }
      }
   }

   LinkedHashSet<Face> findInsideFaces() {
      LinkedHashSet<Face> faces = new LinkedHashSet<Face>();
      for (Vertex3d v : myInsideVertices) {
         Iterator<HalfEdge> it = v.getIncidentHalfEdges();
         while (it.hasNext()) {
            HalfEdge he = it.next();
            faces.add (he.getFace());
            if (he.opposite != null) {
               faces.add (he.opposite.getFace());
            }
         }
      }
      for (IntersectionContour c : myContours) {
         for (int k=0; k<c.size(); k++) {
            Face face = c.findSegmentFace (k, myMesh);
            if (face != null) {
               // face will be null when c is open and k is the last point
               faces.add (face);
            }
         }
      }
      return faces;
   }

   /**
    * Computes the area of this region projected onto a plane
    * defined by a normal <code>nrm</code>.
    */
   double computePlanarArea (
      Vector3d nrm, Point3d p0, boolean clockwiseContour) {
      double area = 0;
      for (IntersectionContour c : myContours) {
         area += c.computePlanarArea (nrm, p0, clockwiseContour);
      }
      return area;
   }

}
