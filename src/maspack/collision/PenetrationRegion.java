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
    * Returns the surface area for this penetration region. If not
    * already known, the area is computed and then cached internally.
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
    * Computes the area for this mesh.
    */
   double computeArea () {

      double area = 0;
      HashMap<Face,Double> faceAreas = new HashMap<Face,Double>();
      for (IntersectionContour c : myContours) {
         computeAreaForContour (c, myMesh, !myClockwise, faceAreas);
      }
      for (Map.Entry<Face,Double> entry : faceAreas.entrySet()) {
         Face face = entry.getKey();
         if (!isInsideFace (face)) {
            throw new InternalErrorException (
               "Visited face "+face.getIndex()+" is not an internal face");
         }
         double a = entry.getValue();
         area -= a;
      }
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
      IntersectionPoint last;  // most recent point that involvedthis face
      double area; // cumulative area involving this face

      void setFirst (IntersectionPoint first) {
         this.first = first;
         this.last = null;
         this.area = 0;
      }
   }

   private double computeBoundaryArea (
      Face face, IntersectionPoint plast,
      IntersectionPoint pnew, boolean clockwiseContour) {

      HalfEdge he0 = face.firstHalfEdge();
      HalfEdge he1 = he0.getNext();
      HalfEdge he2 = he1.getNext();

      HalfEdge lastEdge = SurfaceMeshIntersector.getPointEdge (plast, face);
      HalfEdge newEdge = SurfaceMeshIntersector.getPointEdge (pnew, face);

      if (debugFace == face.getIndex()) {
         System.out.println (
            "compute boundary area clockwise=" + clockwiseContour);
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
      return clockwiseContour ? -area : area;
   }

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

   private void getWorldPoint (Point3d pnt, Vertex3d vtx) {
      MeshBase mesh = vtx.getMesh();
      if (!mesh.meshToWorldIsIdentity()) {
         pnt.transform (mesh.getMeshToWorld(), vtx.pnt);
      }
      else {
         pnt.set (vtx.pnt);
      }
   }

   private void addArea (HashMap<Face,Double> faceAreas, Face face, double a) {
      Double area = faceAreas.get(face);
      if (area == null) {
         faceAreas.put (face, a);
      }
      else {
         faceAreas.put (face, a+area);
      }
   }

   private static int debugFace = -1;

   private FaceInfo getInfo (Face face, HashMap<Face,FaceInfo> map) {
      FaceInfo finfo = map.get(face);
      if (finfo == null) {
         finfo = new FaceInfo();
         map.put (face, finfo);
      }
      return finfo;
   }

   private boolean isFaceOutside (
      Face face, IntersectionPoint pe, PolygonalMesh mesh) {

      Point3d centroid = new Point3d();
      face.computeWorldCentroid(centroid);

      return SurfaceMeshIntersector.isPointOutside (
         centroid, face, pe, mesh);
   }

   private double computeAreaForContour (
      IntersectionContour contour, PolygonalMesh mesh,
      boolean clockwise, HashMap<Face,Double> faceAreas) {
      
      int csize = contour.size();
      double area = 0;

      int kstart = -1;
      Face lastFace = contour.findSegmentFace (csize-1, mesh);
      for (int k=0; k<csize; k++) {
         Face face = contour.findSegmentFace (k, mesh);
         if (lastFace != face) {
            kstart = k;
            break;
         }
         lastFace = face;
      }
      Point3d p0 = new Point3d();
      if (kstart == -1) {
         // contour is entirely on lastFace
         //System.out.println ("contour in on one face");
         getWorldPoint (p0, lastFace.firstHalfEdge().getHead());
         for (int i=0; i<csize; i++) {
            IntersectionPoint pa = contour.get(i);
            IntersectionPoint pb = contour.getWrapped(i+1);
            area += computeArea (lastFace, p0, pa, pb);
         }
         if (clockwise) {
            area = -area;
         }
         if (area < 0) {
            area += lastFace.computeArea();
         }
         addArea (faceAreas, lastFace, area);
         return area;
      }
      else {
         //System.out.println ("contour crosses faces");
         // contour crosses more than one face
         HashMap<Face,FaceInfo> infoMap = new HashMap<Face,FaceInfo>();
         Face face = contour.findSegmentFace (kstart, mesh);         
         FaceInfo finfo = getInfo (face, infoMap);
         finfo.setFirst (contour.get(kstart));
         getWorldPoint (p0, face.firstHalfEdge().getHead());   
         int k = kstart;
         for (int i=0; i<csize; i++) {
            IntersectionPoint pa = contour.getWrapped(k);
            IntersectionPoint pb = contour.getWrapped(k+1);
            double a = computeArea (face, p0, pa, pb);
            if (clockwise) {
               a = -a;
            }
            if (face.getIndex() == debugFace) {
               System.out.println ("face" + debugFace + " area=" + a);
               System.out.println (" p0=" + p0.toString ("%12.8f"));
               System.out.println (" pa=" + pa.toString ("%12.8f"));
               System.out.println (" pb=" + pb.toString ("%12.8f"));
            }
            finfo.area += a;
            area += a;
            Face nextFace = contour.findSegmentFace (k+1, mesh); 
            if (nextFace != face) {
               // exiting face
               finfo.last = pb;
               if (i < csize-1) {
                  // entering new face
                  finfo = getInfo (nextFace, infoMap);
                  if (finfo.last == null) {
                     finfo.setFirst (pb);
                  }
                  else {
                     double ba = computeBoundaryArea (
                        nextFace, finfo.last, pb, clockwise); 
                     if (nextFace.getIndex() == debugFace) {
                        System.out.println (
                           "face" + debugFace + " boundary area=" + ba);
                     }
                     finfo.area += ba;
                     area += ba;
                  }
                  getWorldPoint (p0, nextFace.firstHalfEdge().getHead());
                  face = nextFace;
               }
            }
            k++;
         }
         for (Map.Entry<Face,FaceInfo> entry : infoMap.entrySet()) {
            finfo = entry.getValue();
            face = entry.getKey();
            double ba = computeBoundaryArea (
               face, finfo.last, finfo.first, clockwise);
            if (face.getIndex() == debugFace) {
               System.out.println (
                  "face" + debugFace + " last boundary area=" + ba);
            }
            finfo.area += ba;
            double a = finfo.area;
            double r = mesh.getRadius();
            double atol = DOUBLE_PREC*r*r;
            if (a < atol) {
               if (a < -atol || isFaceOutside (face, finfo.first, mesh)) {
                  //if (a < 0 || (a == 0 && !reallyInside(face))) {
                  a += face.computeArea();
               }
            }
            area += a;
            addArea (faceAreas, face, a);
         }
      }
      return area;
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
         IntersectionPoint pa = c.getWrapped(-1);
         for (IntersectionPoint pb : c) {
            faces.add (c.findSegmentFace (pa, pb, myMesh));
            pa = pb;
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
