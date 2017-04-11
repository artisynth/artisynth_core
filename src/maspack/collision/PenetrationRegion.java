package maspack.collision;

import java.util.*;

import maspack.util.*;
import maspack.matrix.*;
import maspack.geometry.*;

/**
 * Describes a penetration region for an oriented triangular mesh. This is a
 * single connected region of the mesh that is either "inside" or "outside"
 * another oriented mesh when the two meshes intersect. It is bounded by 
 * one or more intersection contours, and contains a set of edges that are 
 * associated with the intersection and a set of vertices contained within
 * the region. The intersection between two meshes will generally result in 
 * one or more penetration regions for each mesh. The number of regions on 
 * each mesh may differ, and they will not necessarily share the same contours.
 *
 * <p> In some cases the penetration region may intersect only a single face of
 * the intersecting mesh. The region is then referred to as a "single face
 * region", and its bounding contours all lie within the single face of the
 * intersecting mesh.
 */
public class PenetrationRegion {

   LinkedHashSet<Vertex3d> myVertices;
   LinkedHashSet<Face> myFaces;
   LinkedHashSet<HalfEdge> myEdges;

   LinkedHashSet<IntersectionContour> myContours;
   
   Face mySingleFace;
   double mySingleFaceArea;
   
   private double myArea = -1;
   PolygonalMesh myMesh = null;
   boolean myClockwise = false;
   double myPositionTol = -1; // position tolerance used to create this region

   public static boolean debug = false;

   /**
    * Creates a new, empty penetration region for a specified mesh.
    *
    * @param mesh mesh to which the region belongs
    * @param clockwise if <code>true</code>, indicates that the
    * bounding contours for the region are oriented clockwise
    * with respect to the mesh.
    * @param posTol position tolerance used to create the region
    */
   public PenetrationRegion (
      PolygonalMesh mesh, boolean clockwise, double posTol) {

      myVertices = new LinkedHashSet<Vertex3d>();
      myFaces = new LinkedHashSet<Face>();
      myEdges = new LinkedHashSet<HalfEdge>();

      myContours = new LinkedHashSet<IntersectionContour>();
      
      mySingleFace = null;
      myMesh = mesh;
      myClockwise = clockwise;
      myPositionTol = posTol;
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
    * Returns the vertices contained in this region.
    *
    * @return hash set of the region vertices. Should not be modified.
    */
   public LinkedHashSet<Vertex3d> getVertices() {
      return myVertices;
   }
   
   /**
    * Returns the number of vertices contained in this region.
    * 
    * @return number of vertices in this region
    */
   public int numVertices() {
      return myVertices.size();
   }

   /**
    * Returns <code>true</code> if a specified vertex is contained in this
    * region.
    * 
    * @param vtx vertex to test
    * @return <code>true</code> if <code>vtx</code> is in this region
    */
   public boolean containsVertex (Vertex3d vtx) {
      return myVertices.contains(vtx);
   }

   /**
    * Returns the faces contained in this region.
    *
    * @return hash set of the region faces. Should not be modified.
    */
   public LinkedHashSet<Face> getFaces() {
      return myFaces;
   }
   
   /**
    * Returns the number of faces contained in this region.
    * 
    * @return number of faces in this region
    */
   public int numFaces() {
      return myFaces.size();
   }

   /**
    * Returns <code>true</code> if a specified face is contained in this
    * region.
    * 
    * @param face face to test
    * @return <code>true</code> if <code>face</code> is in this region
    */
   public boolean containsFace (Face face) {
      return getFaces().contains(face);
   }

   /**
    * Returns the edges contained in this region. Note that only primary edges
    * are stored, so if <code>he</code> is an inside edge,
    * <code>he.opposite</code> will not be.
    *
    * @return hash set of region edges. Should not be modified.
    */
   public LinkedHashSet<HalfEdge> getEdges() {
      return myEdges;
   }
   
   /**
    * Returns the number of edges contained in this region.
    * 
    * @return number of edges in this region
    */
   public int numEdges() {
      return myEdges.size();
   }

   /**
    * Returns <code>true</code> if a specified edge is contained in this
    * region. The test will be made using <code>he.getPrimary()</code>, so that
    * if <code>he</code> is inside, then <code>he.opposite</code> will also be
    * inside.
    * 
    * @param he edge to test
    * @return <code>true</code> if <code>he</code> 
    * or <code>he.opposite</code> is in this region
    */
   public boolean containsEdge (HalfEdge he) {
      return myEdges.contains(he.getPrimary());
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
    * contain the same contours, vertices, faces and edges.
    */
   public boolean equals (PenetrationRegion region) {

      if (!myVertices.equals (region.myVertices)) {
         return false;
      }
      if (!myEdges.equals(region.myEdges)) {
         return false;
      }
      if (!myFaces.equals(region.myFaces)) {
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
    * of the indices of the vertices and the indices of
    * the contours with respect to a reference list.
    */
   public String toString (List<IntersectionContour> contours) {
      String vertexStr = 
         ArraySupport.toString(Vertex3d.getIndices(myVertices));
      String contourStr = 
         ArraySupport.toString(getContourIndices(contours));
      return "[ vertices=[ "+vertexStr+" ] contours=[ "+contourStr+" ]]";
   }

   /**
    * Returns the surface area for this penetration region. This is the area on
    * this region's mesh that is bounded by the region's intersection
    * contours.
    * 
    * @return surface area of this penetration region.
    */
   public double getArea() {
      return myArea;
   }

   void setArea (double area) {
      myArea = area;
   }

   void addArea (double area) {
      myArea += area;
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

   /**
    * Returns the position tolerance that was used to create this region.  This
    * value is set by {@link SurfaceMeshIntersector} when the region is
    * created.
    *
    * @return position tolerance.
    */
   public double getPositionTol() {
      return myPositionTol;
   }

}
