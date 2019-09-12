package maspack.collision;

import java.util.*;
import java.io.*;

import maspack.util.BooleanHolder;
import maspack.matrix.*;
import maspack.collision.IntersectionPoint;
import maspack.geometry.*;
import maspack.render.RenderableUtils;
import maspack.util.*;

/**
 * A collider that determines the intersection between two meshes by first
 * identifying and tracing the intersection contours between the meshes, and
 * then using these to determine the interpenetration regions.
 */
public class SurfaceMeshIntersector {

   public static boolean writeErrorFiles = true;

   /**
    * Describes which penetration regions should be computed for
    * a particular mesh intersecting with another mesh.
    */
   public enum RegionType {
      /**
       * Do not compute any penetration regions
       */
      NONE,
      
      /**
       * Compute the penetration regions that are <i>inside</i>
       * the other mesh. 
       */
      INSIDE,
      
      /**
       * Compute the penetration regions that are <i>outside</i>
       * the other mesh. 
       */
      OUTSIDE,
   }

   private static boolean printContours = false;
   private static boolean useEmptySegmentProjection = true;

   private static final double EPS = 1e-10;
   private static final double DOUBLE_PREC = 2.0e-13;
//   boolean myCheckCoincidentOrdering = false;

   static boolean removeZeroAreaFaces = true;
   static boolean mergeCoincidentMips = true;
   static boolean addEmptyFaceVertices = true;

   boolean regionAddingDebug = false;
   boolean traceContourDebug = false;

   PolygonalMesh myMesh0;  // first mesh to be intersected
   PolygonalMesh myMesh1;  // second mesh to be intersected

   double myPositionTol = 0;
   double myAreaTol = 0;
   double myMaxLength = 0;

   public boolean mySilentP = true;
   public static boolean debug = false;
   public static boolean debug2 = false;
   public static RigidTransform3d debugTBW = null;

   public boolean triangulationError = false;
   public boolean coincidentError = false;

   class EdgeInfo {

      class PointData {
         IntersectionPoint pnt;
         double s;

         PointData (IntersectionPoint p) {
            s = project (p);
            pnt = p;
         }
         
         PointData (IntersectionPoint p, double s) {
            this.s = s;
            pnt = p;
         }
      }

      PointData[] myMips;
      PointData[] myXips;
      int myNumMips;
      int myNumXips;

      HalfEdge myEdge;
      Vector3d myUdir;
      double myUlen;
      Point3d myTail;

      private String rangeErrorMessage(int idx) {
         return "Index: "+idx+", numMips=" + myNumMips;
      }

      EdgeInfo (HalfEdge edge) {
         Point3d head = new Point3d();
         myMips = new PointData[8];
         myNumMips = 0;
         myEdge = edge;
         myUdir = new Vector3d();
         myTail = new Point3d();
         edge.getTail().getWorldPoint (myTail);
         edge.getHead().getWorldPoint (head);
         myUdir.sub (head, myTail);
         myUlen = myUdir.norm();
         myUdir.scale (1/myUlen);
      }

      private PointData[] resizeBuffer (
         PointData[] oldBuffer, int minCap) {
         
         int oldCap = oldBuffer.length;
         int newCap = oldCap + (oldCap >> 1);
         if (newCap < minCap) {
            newCap = minCap;
         }
         return Arrays.copyOf(oldBuffer, newCap);
      }

      private void ensureMipsCapacity (int minCap) {
         if (myMips.length < minCap) {
            myMips = resizeBuffer (myMips, minCap);
         }
      }

      int numMips() {
         return myNumMips;
      }

      IntersectionPoint getMip (int idx) {
         if (idx >= myNumMips) {
            throw new IndexOutOfBoundsException (rangeErrorMessage(idx));
         }
         return myMips[idx].pnt;
      }

      PointData getMipData (int idx) {
         if (idx >= myNumMips) {
            throw new IndexOutOfBoundsException (rangeErrorMessage(idx));
         }
         return myMips[idx];
      }

      void addMip (IntersectionPoint mip) {
         ensureMipsCapacity (myNumMips+1);
         myMips[myNumMips++] = new PointData(mip);
      }

      void addMip (int idx, IntersectionPoint mip, double s) {
         if (idx < 0 || idx > myNumMips) {
            throw new IndexOutOfBoundsException (rangeErrorMessage(idx));
         }
         ensureMipsCapacity (myNumMips+1);
         if (idx != myNumMips) {
            System.arraycopy (myMips, idx, myMips, idx+1, myNumMips-idx);
         }
         myMips[idx] = new PointData(mip, s);
         myNumMips++;
      }

      void setMip (int idx, IntersectionPoint mip) {
         if (idx >= myNumMips) {
            throw new IndexOutOfBoundsException (rangeErrorMessage(idx));
         }
         myMips[idx].pnt = mip;
      }

      IntersectionPoint removeMip (int idx) {
         if (idx >= myNumMips) {
            throw new IndexOutOfBoundsException (rangeErrorMessage(idx));
         }
         PointData data = myMips[idx];
         int numMoved = myNumMips-idx-1;
         if (numMoved > 0) {
            System.arraycopy (myMips, idx+1, myMips, idx, numMoved);
         }
         myMips[--myNumMips] = null;
         return data.pnt;
      }

      int indexOfMip (IntersectionPoint mip) {
         for (int i=0; i<myNumMips; i++) {
            if (myMips[i].pnt == mip) {
               return i;
            }
         }
         return -1;                    
      }

      boolean containsMip (IntersectionPoint mip) {
         return indexOfMip (mip) != -1;
      }

      void initializeXips() {
         myXips = new PointData[myNumMips];
         myNumXips = myNumMips;
         System.arraycopy (myMips, 0, myXips, 0, myNumMips);
      }

      void clearXips() {
         myXips = null;
         myNumXips = 0;
      }

      private void ensureXipsCapacity (int minCap) {
         if (myXips == null) {
            myXips = new PointData[8];
         }
         else if (myXips.length < minCap) {
            myXips = resizeBuffer (myXips, minCap);
         }
      }

      int numXips() {
         return myNumXips;
      }

      IntersectionPoint getXip (int idx) {
         if (idx >= myNumXips) {
            throw new IndexOutOfBoundsException (rangeErrorMessage(idx));
         }
         return myXips[idx].pnt;
      }

      PointData getXipData (int idx) {
         if (idx >= myNumXips) {
            throw new IndexOutOfBoundsException (rangeErrorMessage(idx));
         }
         return myXips[idx];
      }

      IntersectionPoint getXip (int idx, HalfEdge edge, boolean clockwise) {
         if (idx >= myNumXips) {
            throw new IndexOutOfBoundsException (rangeErrorMessage(idx));
         }
         boolean tailToHead = (edge==myEdge);
         if (clockwise) {
            tailToHead = !tailToHead;
         }
         if (tailToHead) {
            return myXips[idx].pnt;
         }
         else {
            return myXips[myNumXips-1-idx].pnt;
         }
      }

      void addXip (IntersectionPoint mip) {
         ensureXipsCapacity (myNumXips+1);
         myXips[myNumXips++] = new PointData(mip);
      }

      void addXip (int idx, IntersectionPoint mip, double s) {
         if (idx < 0 || idx > myNumXips) {
            throw new IndexOutOfBoundsException (rangeErrorMessage(idx));
         }
         ensureXipsCapacity (myNumXips+1);
         if (idx != myNumXips) {
            System.arraycopy (myXips, idx, myXips, idx+1, myNumXips-idx);
         }
         myXips[idx] = new PointData(mip, s);
         myNumXips++;
      }

      void setXip (int idx, IntersectionPoint mip) {
         if (idx >= myNumXips) {
            throw new IndexOutOfBoundsException (rangeErrorMessage(idx));
         }
         myXips[idx].pnt = mip;
      }

      IntersectionPoint removeXip (int idx) {
         if (idx >= myNumXips) {
            throw new IndexOutOfBoundsException (rangeErrorMessage(idx));
         }
         PointData data = myXips[idx];
         int numMoved = myNumXips-idx-1;
         if (numMoved > 0) {
            System.arraycopy (myXips, idx+1, myXips, idx, numMoved);
         }
         myXips[--myNumXips] = null;
         return data.pnt;
      }

      int indexOfXip (IntersectionPoint mip) {
         for (int i=0; i<myNumXips; i++) {
            if (myXips[i].pnt == mip) {
               return i;
            }
         }
         return -1;                    
      }

      int normalizedIndexOfXip (
         IntersectionPoint mip, HalfEdge edge, boolean clockwise) {

         for (int i=0; i<myNumXips; i++) {
            if (myXips[i].pnt == mip) {
               boolean reverse = (edge != myEdge);
               if (clockwise) {
                  reverse = !reverse;
               }
               return reverse ? myNumXips-1-i : i;
            }
         }
         return -1;                    
      }

      boolean containsXip (IntersectionPoint mip) {
         return indexOfXip (mip) != -1;
      }

      double distance (Point3d p) {
         Vector3d tmp = new Vector3d();
         tmp.sub (p, myTail);
         tmp.cross (myUdir, tmp);
         return tmp.norm();
      }

      double project (Point3d p, HalfEdge edge, boolean reverse) {
         Vector3d tmp = new Vector3d();
         tmp.sub (p, myTail);
         double s = tmp.dot(myUdir);
         if (!(reverse ^ (edge==myEdge))) {
            s = myUlen - s;
         }
         return s;
      }

      double project (Point3d p) {
         Vector3d tmp = new Vector3d();
         tmp.sub (p, myTail);
         return tmp.dot(myUdir);
      }

      double dot (Vector3d v, HalfEdge edge, boolean reverse) {
         double d = myUdir.dot(v);
         if (!(reverse ^ (edge==myEdge))) {
            d = -d;
         }
         return d;
      }

      double dot (Vector3d v, HalfEdge edge) {
         double d = myUdir.dot(v);
         if (edge != myEdge) {
            d = -d;
         }
         return d;
      }
   };

   public boolean getSilent() {
      return mySilentP;
   }
   
   public void setSilent (boolean silent) {
      mySilentP = silent;
   }
   
   /*
    * List of contours generated by the most recent intersection operation.
    */
   ArrayList<IntersectionContour> myContours;

   // returns the index of contour with respect to myContours
   private int getContourIndex (IntersectionContour c) {
      return myContours.indexOf(c);
   }

   // temporary intersection point used when building the contour
   IntersectionPoint myWorkPoint = new IntersectionPoint();

   /*
    * For each edge, remember all the intersection points, in order to
    * determine inside boundary vertices.
    */
   LinkedHashMap<HalfEdge,EdgeInfo> myEdgeInfos =
      new LinkedHashMap<HalfEdge,EdgeInfo>();
   
   FaceCalculator[] myFaceCalcs0;
   FaceCalculator[] myFaceCalcs1;

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

   private PolygonalMesh getMesh (int num) {
      if (num == 0) {
         return myMesh0;
      }
      else if (num == 1) {
         return myMesh1;
      }
      else {
         throw new InternalErrorException ("mesh number should be 0 or 1");
      }
   }

//   /**
//    * String representation of a half edge using its head and tail indices
//    */
//   public static String toString (HalfEdge he) {
//      return he.getTail().getIndex() + "-" + he.getHead().getIndex();
//   }

//   /**
//    * String representation of a half edge using its face indices
//    */
//   public static String toFaceString (HalfEdge he) {
//      Face f0 = he.getFace();
//      Face f1 = he.getOppositeFace();
//      String s0 = (f0 != null ? ""+f0.getIndex() : "NUL");
//      String s1 = (f1 != null ? ""+f1.getIndex() : "NUL");
//      return s0 + "-" + s1;
//   }

   private static String pad (String str, int len) {
      while (str.length() < len) {
         str += " ";
      }
      return str;
   }
   
   public String toString (IntersectionPoint p) {
      return toString (p, RigidTransform3d.IDENTITY);
   }
   
   /**
    * Comprehensive string representation of a mesh intersection point,
    * showing most relevant information. Used for debugging.
    * 
    * The string contains the following information:
    * 
    * 1) index of the point with respect to the contour
    * 2) the degeneracy code for the intersection (as a two digit hex number)
    * 3) 'C' if the point is coincident, or ' ' otherwise
    * 4) the point's edge and face, if edge belongs to mesh, or
    *    the point's face and edge
    * 5) the segment faces of the point with respect to the mesh and the
    *    other mesh (or blank if this is the last point of an open contour)
    * 6) the point's coordinates   
    */
   public String toString (IntersectionPoint p, RigidTransform3d T) {
      NumberFormat ifmt = new NumberFormat ("%3d");
      NumberFormat dfmt = new NumberFormat ("%2x");
      int dgen = p.getDegeneracies();
      String dstr = (dgen != 0 ? dfmt.format(dgen) : "  ");
      String prefix = 
         ifmt.format(p.contourIndex)+" "+dstr+
                     " "+(p.isCoincident() ? "C " : "  ");

      if (p.edge.getHead().getMesh() == myMesh0) {
         prefix += ("E" + pad(p.edge.faceStr(), 10) +
                    "F"+ pad(""+p.face.getIndex(), 10));
      }
      else {
         prefix += ("F"+ pad(""+p.face.getIndex(), 10) +
                    "E" + pad(p.edge.faceStr(), 10));                    
      }

      int idx = p.contourIndex;
      Face face0 = p.contour.findSegmentFace (idx, myMesh0);
      Face face1 = p.contour.findSegmentFace (idx, myMesh1);
      
      String faceStr0 = (face0 != null ? " F"+face0.getIndex() : "NULL");
      String faceStr1 = (face1 != null ? " F"+face1.getIndex() : "NULL");

      prefix += (" " + pad(faceStr0,6) + pad(faceStr1,6) + " ");

      Point3d pnt = new Point3d();
      pnt.transform (T, p);
      String str = prefix + pnt.toString ("%g");
      return str;
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
            if (traceContourDebug) {
               System.out.println ("Bad mip match!");
            }
            
            return false;
         }
         contour.isClosed = true;
         contour.isContinuable = false;
         return true;
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
    * If no contours are found the list will have a length of 0. Each
    * contour is composed of a list of {@link IntersectionPoint}s, the segments
    * between which comprise the boundary between the portions of each mesh
    * which are <i>inside</i> or <code>outside</code> the intersection
    * volume. The contours are oriented so that as they traverse the mesh (in
    * increasing point order), the inside portion of <code>mesh0</code> is to
    * the left (counterclockwise orientation), while the inside portion of
    * <code>mesh1</code> is to the right (clockwise orientation).
    *
    * <p>If both meshes are closed, the resulting intersection contours will be
    * closed. If one or both meshes are open, then one or more contours may
    * also be open.
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
      double maxRadius =
         Math.max(RenderableUtils.getRadius(mesh0),
                  RenderableUtils.getRadius(mesh1));
      myMaxLength = 2*maxRadius;
      myPositionTol = DOUBLE_PREC*maxRadius;
      myAreaTol = DOUBLE_PREC*maxRadius*maxRadius;

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

   /**
    * Finds either the <i>inside</i> or <i>outside</i> penetration regions for
    * the meshes and contours associated with the most recent call to either
    * {@link #findContours}, {@link
    * #findContoursAndRegions(PolygonalMesh,PolygonalMesh)}, or {@link
    * #findContoursAndRegions(PolygonalMesh,RegionType,PolygonalMesh,RegionType)}.
    * The type of region (inside, outside, or none) that should be computed for
    * each mesh is controlled by the arguments <code>regions0</code> and
    * <code>regions1</code>.  This method functions identically to {@link
    * #findContoursAndRegions(PolygonalMesh,RegionType,PolygonalMesh,RegionType)},
    * except that the meshes and contours are those associated with the last of
    * the aforementioned calls. If no such previous call was made, this method
    * throws an {@link IllegalStateException}. If the call did not result in an
    * intersection, this method returns <code>null</code>.
    * 
    * @param regions0 specifies which type of penetration regions should
    * be computed for the first mesh
    * @param regions1 specifies which type of penetration regions should
    * be computed for the second mesh
    * @return a <code>ContactInfo</code> structure containing the computed
    * region information, or <code>null</code> if no intersection was
    * previously found.
    */
   public ContactInfo findRegions (RegionType regions0, RegionType regions1) {
      
      if (myContours == null) {
         throw new IllegalStateException (
            "No previous call was made to generate intersection contours");
      }
      if (myContours.size() > 0) {
         return findRegions (
            myContours, myMesh0, regions0, myMesh1, regions1);
      }
      else {
         return null;
      }
   }

   /**
    * Finds the intersection contours between two meshes, as described by
    * {@link #findContours}, and then processes these further to determine
    * <i>inside</i> penetration regions on each mesh. This is
    * identical to the call
    * <pre>
    *   findContoursAndRegions (mesh0, true, mesh1, true);
    * </pre>
    * and for more information see the documentation for
    * {@link
    * #findContoursAndRegions(PolygonalMesh,RegionType,PolygonalMesh,RegionType)}.
    *
    * @param mesh0 first mesh 
    * @param mesh1 second mesh 
    * @return {@link ContactInfo} object containing the contours and regions,
    * or <code>null</code> if there is no intersection.
    */
   public ContactInfo findContoursAndRegions (
      PolygonalMesh mesh0, PolygonalMesh mesh1) {
      return findContoursAndRegions (
         mesh0, RegionType.INSIDE, mesh1, RegionType.INSIDE);
   }

   /**
    * Finds the intersection contours between two meshes, as described by
    * {@link #findContours}, and then processes these further to determine
    * either the <i>inside</i> or <i>outside</i> penetration regions for
    * each mesh. These are the sub-regions of the mesh which are either
    * <code>inside</code> or <code>outside</code> of the intersection
    * volume. The type of region (inside, outside, or none) that should
    * be computed for each mesh is controlled by the arguments 
    * <code>regions0</code> and <code>regions1</code>. 
    * Contours that do not partition a mesh into distinct regions are ignored.
    *
    * <p>The region information for each mesh, along with the generated
    * contours, is returned in a {@link ContactInfo} object, for which the
    * methods {@link ContactInfo#getContours}, {@link ContactInfo#getRegions
    * getRegions(0)}, and {@link ContactInfo#getRegions getRegions(1)} can be
    * used to query the contours and the respective regions for
    * <code>mesh0</code> and <code>mesh1</code>. 
    * 
    * <p>If no intersection contours are discovered between the two
    * meshes, the contour list returned in the {@link ContactInfo}
    * will have zero length.
    *
    * @param mesh0 first mesh 
    * @param regions0 specifies which type of penetration regions should
    * be computed for <code>mesh0</code>
    * @param mesh1 second mesh 
    * @param regions1 specifies which type of penetration regions should
    * be computed for <code>mesh1</code>
    * @return {@link ContactInfo} object containing the contours and regions
    */
   public ContactInfo findContoursAndRegions (
      PolygonalMesh mesh0, RegionType regions0,
      PolygonalMesh mesh1, RegionType regions1) {
      
      ArrayList<IntersectionContour> contours = findContours (mesh0, mesh1);
      return findRegions (contours, mesh0, regions0, mesh1, regions1);
   }

   ContactInfo findRegions (
      ArrayList<IntersectionContour> contours,
      PolygonalMesh mesh0, RegionType regions0,
      PolygonalMesh mesh1, RegionType regions1) {

      ContactInfo cinfo = new ContactInfo (mesh0, mesh1);
      cinfo.myContours = contours;
      cinfo.myRegions0 = null;
      cinfo.myRegionsType0 = regions0;
      cinfo.myRegions1 = null;
      cinfo.myRegionsType1 = regions1;

      if (regions0 != RegionType.NONE) {
         if (regionAddingDebug) {
            System.out.println ("Finding regions on mesh0");
         }
         cinfo.myRegions0 = new ArrayList<PenetrationRegion>();
         findPenetrationRegions (
            cinfo.myRegions0, contours, myMesh0, myMesh1, 
            regions0 == RegionType.OUTSIDE);

      }
      if (regions1 != RegionType.NONE) {
         if (regionAddingDebug) {
            System.out.println ("Finding regions on mesh1");
         }
         cinfo.myRegions1 = new ArrayList<PenetrationRegion>();
         findPenetrationRegions (
            cinfo.myRegions1, contours, myMesh1, myMesh0, 
            regions1 == RegionType.INSIDE);
      }
      return cinfo;
   }      

   /**
      State transitions as we track points within a face:

      Start, on edge (effectiveFace == face)
          off edge
              nothing
          on empty segment
              effectiveFace = segment.opFace
          on full segment
              nothing

      On edge (effectiveFace == face)
          off edge
              if (lastp not on lastEdge) {
                  check for empty contact and add exit/enter if necessary
              }
          on empty segment
              if (nextEdge == lastEdge) {
                  if (lastp on lastEdge) {
                     remove lastp from lastEdge;
                  }
                  else {
                     add lastp to lastEdge;
                  }
              }
              else { //nextEdge != lastEdge
                  
              }
              effectiveFace = segment.opFace
          on full segment
              nothing

      Off edge (effectiveFace == face)
          on edge

      On empty segment (effectiveFace == segment.opFace)
          off edge
              effectiveFace = face
          on empty segment
              effectiveFace = segment.opFace
          on full segment
              effectiveFace = face

      On full segment (effectiveFace == face)
          off edge
              nothing
          on empty segment
              effectiveFace = segment.opFace
          on full segment
              nothing
   */
   void markEmptySegMip (
      IntersectionPoint mip, HalfEdge edge, boolean clockwise) {

      Face face = edge.getFace();
      PolygonalMesh mesh = (PolygonalMesh)face.getMesh();
      int meshNum = (mesh == myMesh0 ? 0 : 1);

      EdgeInfo einfo = getEdgeInfo (edge.getPrimary());
      if (mip.edge.getHead().getMesh() != mesh) {
         boolean tailToHead = (edge == einfo.myEdge);
         if (clockwise) {
            tailToHead = !clockwise;
         }
         EdgeInfo.PointData xdata = null;
         double s = einfo.project(mip);
         double slo = 0;
         double shi = einfo.myUlen;
         int idx;
         if (tailToHead) {
            for (idx=0; idx<einfo.numXips(); idx++) {
               shi = einfo.getXipData(idx).s;
               if (shi-myPositionTol >= s) {
                  break;
               }
               slo = shi;
            }
            if (idx==einfo.numXips()) {
               shi = einfo.myUlen;
            }
         }
         else {
            for (idx=einfo.numXips()-1; idx>=0; idx--) {
               slo = einfo.getXipData(idx).s;
               if (slo+myPositionTol <= s) {
                  break;
               }
               shi = slo;
            }
            if (idx == -1) {
               slo = 0;
            }
            idx++;
         }
         if (slo+myPositionTol < s && shi-myPositionTol > s) {
            mip.setEmptyMark (meshNum, IntersectionPoint.EMPTY_PROJECTED);
            if (useEmptySegmentProjection) {
               einfo.addXip (idx, mip, s);
            }
         }
      }
   }

   void markEmptySegBegin (
      IntersectionPoint mip, HalfEdge edge, boolean clockwise) {

      Face face = edge.getFace();
      PolygonalMesh mesh = (PolygonalMesh)face.getMesh();
      int meshNum = (mesh == myMesh0 ? 0 : 1);
      int mark = IntersectionPoint.EMPTY_BEGIN;

      EdgeInfo einfo = getEdgeInfo (edge.getPrimary());
      if (mip.edge.getHead().getMesh() != mesh) {
         boolean tailToHead = (edge == einfo.myEdge);
         if (!clockwise) {
            tailToHead = !tailToHead;
         }
         EdgeInfo.PointData xdata = null;
         double s = einfo.project(mip);
         int idx;
         if (tailToHead) {
            double si = 0;
            for (idx=0; idx<einfo.numXips(); idx++) {
               double sx = einfo.getXipData(idx).s;
               if (sx-myPositionTol >= s) {
                  break;
               }
               si = sx;
            }
            if (s > si+myPositionTol) {
               mark |= IntersectionPoint.EMPTY_PROJECTED;
            }
         }
         else {
            double si = einfo.myUlen;
            for (idx=einfo.numXips()-1; idx>=0; idx--) {
               double sx = einfo.getXipData(idx).s;
               if (sx+myPositionTol <= s) {
                  break;
               }
               si = sx;
            }
            idx++;
            if (s < si-myPositionTol) {
               mark |= IntersectionPoint.EMPTY_PROJECTED;
            }
         }
         if (useEmptySegmentProjection) {
            einfo.addXip (idx, mip, s);
         }
         mip.setNearEdge (meshNum, edge);
      }
      if (useEmptySegmentProjection) {
         mip.setEmptyMark (meshNum, mark);
      }
   }

   void markEmptySegEnd (
      IntersectionPoint mip, HalfEdge edge, boolean clockwise) {

      Face face = edge.getFace();
      PolygonalMesh mesh = (PolygonalMesh)face.getMesh();
      int meshNum = (mesh == myMesh0 ? 0 : 1);
      int mark = IntersectionPoint.EMPTY_END;

      EdgeInfo einfo = getEdgeInfo (edge.getPrimary());
      if (mip.edge.getHead().getMesh() != mesh) {
         boolean tailToHead = (edge == einfo.myEdge);
         if (!clockwise) {
            tailToHead = !tailToHead;
         }
         EdgeInfo.PointData xdata = null;
         double s = einfo.project(mip);
         int idx;
         if (!tailToHead) {
            double si = 0;
            for (idx=0; idx<einfo.numXips(); idx++) {
               double sx = einfo.getXipData(idx).s;
               if (sx-myPositionTol >= s) {
                  break;
               }
               si = sx;
            }
            if (s > si+myPositionTol) {
               mark |= IntersectionPoint.EMPTY_PROJECTED;
            }
         }
         else {
            double si = einfo.myUlen;
            for (idx=einfo.numXips()-1; idx>=0; idx--) {
               double sx = einfo.getXipData(idx).s;
               if (sx+myPositionTol <= s) {
                  break;
               }
               si = sx;
            }
            idx++;
            if (s < si-myPositionTol) {
               mark |= IntersectionPoint.EMPTY_PROJECTED;
            }
         }
         if (useEmptySegmentProjection) {
            einfo.addXip (idx, mip, s);
         }
         mip.setNearEdge (meshNum, edge);
      }
      if (useEmptySegmentProjection) {
         mip.setEmptyMark (meshNum, mark);
      }
   }

   private void findEmptySegments (
      PenetrationRegion region, PolygonalMesh mesh, RegionType type) {
 
      FaceCalculator[] faceCalcs;
      boolean clockwise;
      int meshNum;

      boolean debug = false;

      if (mesh == myMesh0) {
         faceCalcs = myFaceCalcs0;
         clockwise = (type == RegionType.OUTSIDE);
         meshNum = 0;
      }
      else {
         faceCalcs = myFaceCalcs1;
         clockwise = (type == RegionType.INSIDE);
         meshNum = 1;
      }
      if (debug) {
         System.out.println ("mesh=" + mesh + " meshNum=" + meshNum);
      }

      for (IntersectionContour c : region.myContours) {
         if (debug) {
            System.out.println (
               "contour " + getContourIndex(c) + " size=" + c.size() + 
               " containingFace="+
               (c.containingFace!=null ? c.containingFace.getIndex() : "null"));
         }
         IntersectionPoint mip0;

         //debug = (getContourIndex(c)==2 && meshNum == 1);

         int numSegs;
         if (c.isClosed()) {
            mip0 = c.firstFaceEntryPoint (mesh, null);
            if (mip0 == null) {
               mip0 = backupToNonCoincident(c.get(0));
               // XXX HACK: if containingFace != null, then we add an
               // additional loop interation to process the final segment
               numSegs = c.size()+1;
            }
            else {
               numSegs = c.size();
            }
         }
         else {
            mip0 = c.get(0);
            numSegs = c.size()-1;
         }

         Face face = c.findSegmentFace (mip0, mesh);
         Face effectiveFace = face;
         FaceCalculator fcalc = faceCalcs[face.getIndex()];
         fcalc.initializeForEdgeCalcs (clockwise, false);

         HalfEdge lastEdge = null;
         if (c.containingFace == null) {
            lastEdge = getPointEdge (mip0, face);
         }
         else {
            lastEdge = fcalc.findNearEdge (mip0);
         }
         IntersectionPoint prevp = null;
         IntersectionPoint lastp = mip0;
         IntersectionPoint mip = mip0;

         boolean marked = false;         
         for (int i=0; i<numSegs; i++) {
            mip = mip.next();

            Face segFace = c.findSegmentFace (mip, mesh);
            
            if (debug) {
               System.out.println (
                  "mip=" + mip.contourIndex +
                  " face=" + segFace.getIndex() +
                  " " + mip.toString ("%8.3f") +
                  " lastEdge=" + (lastEdge!=null ? lastEdge.vertexStr():"null"));
            }
            if (segFace == face && mip.myVertex == lastp.myVertex) {
               continue;
            }
            if (mip.myVertex != lastp.myVertex) {
               if (lastEdge == null) {
                  // Off edge
                  lastEdge = fcalc.findNearEdge (mip);
               }
               else {
                  HalfEdge nextEdge;
                  if (fcalc.onEdge (mip, lastEdge)) {
                     nextEdge = lastEdge;
                  }
                  else {
                     nextEdge = fcalc.findNearEdge (mip);
                  }
                  if (debug) {
                     System.out.println (
                        "nextEdge="+(nextEdge!=null ? nextEdge.vertexStr():"null"));
                  }
                  
                  boolean emptySegment = false;
                  if (nextEdge != null && fcalc.onEdge (lastp, nextEdge)) {
                     Vector3d d10 = new Vector3d();
                     d10.sub (mip, lastp);
                     emptySegment = fcalc.dotEdge (d10, nextEdge) < 0;
                     if (clockwise) {
                        emptySegment = !emptySegment;
                     }
                  }
                  if (nextEdge != null && emptySegment) {
                     Face nextFace = nextEdge.getOppositeFace();
                     if (effectiveFace != nextFace) {
                        if (effectiveFace == face &&
                            // XXX HACK: if containingFace != null, then we add
                            // an additional loop interation to process the
                            // final segment, but then means we can ENTER on
                            // the last iteration or we'll get redundant ENTERs
                            (c.containingFace==null || i<numSegs-1)) {
                           if (debug) {
                              System.out.println (
                                 "ENTERING face " + nextFace.getIndex() +
                                 " edge=" + nextEdge.vertexStr() +
                                 " lastp=" + lastp.contourIndex +
                                 " mip=" + mip.contourIndex);
                              System.out.println ("mip=" + mip.toString("%8.3f"));
                              System.out.println ("lst=" + lastp.toString("%8.3f"));
                           }
                           markEmptySegBegin (lastp, nextEdge, clockwise);
                           marked = true;
                        }
                        else {
                           // corner point; leaving effectiveFace and entering
                           // next face; no need to do anything
                           // System.out.println (
                           //   "CORNER between " + effectiveFace.getIndex() +
                           //   " and " + nextFace.getIndex());
                           //   
                           // XXX do we want to add point anyway?
                        }
                        effectiveFace = nextFace;
                     }
                     else {
                        // continuing empty segment on nextFace
                        if (debug) {
                           System.out.println (
                              "CONTINUING face " + effectiveFace.getIndex());
                        }
                        markEmptySegMip (lastp, nextEdge, clockwise);
                     }
                  }
                  else {
                     if (effectiveFace != face) {
                        if (debug) {
                           System.out.println (
                              "LEAVING2 face " + effectiveFace.getIndex());
                        }
                        markEmptySegEnd (lastp, lastEdge, clockwise);
                        effectiveFace = face;
                     }
                     else {
                        // XXX test here for empty enter/exit
                     }
                  }
                  lastEdge = nextEdge;
               }
            }
            if (segFace != face && segFace != null) {
               // exiting face; entering segFace
               face = segFace;
               effectiveFace = face;
               fcalc = faceCalcs[face.getIndex()];
               fcalc.initializeForEdgeCalcs (clockwise, false);
               lastEdge = getPointEdge (mip, face);
            }
            prevp = lastp;
            lastp = mip;
         }
         if (c.containingFace != null && c.containingFace.getMesh() == mesh) {
            c.emptySegmentsMarked = marked;
         }
      }
   }

   private class VertexDistancePair {
      Vertex3d myVtx;
      double myDist;

      VertexDistancePair (Vertex3d vtx, double dist) {
         myVtx = vtx;
         myDist = dist;
      }
   }

   private boolean vertexIsCoincident (
      Vertex3d vtx, IntersectionPoint mip,
      Point3d wpnt, VertexDistancePair nearest) {

      vtx.getWorldPoint (wpnt);
      double dist = wpnt.distance (mip);
      if (dist <= myPositionTol) {
         if (dist < nearest.myDist) {
            nearest.myVtx = vtx;
            nearest.myDist = dist;
         }
         return true;
      }
      else {
         return false;
      }
   }

   private void addVertexToMap (
      Vertex3d vtx, Vertex3d newVtx, HashMap<Vertex3d,Vertex3d> map) {

      map.put (vtx, newVtx);
   }

   Vertex3d createNewVertex (
      IntersectionPoint mip,
      HashMap<Vertex3d,Vertex3d> map, BooleanHolder isCoincident) {

      Vertex3d newVtx = null;

      Vertex3d headVtx = null;
      Vertex3d tailVtx = null;
      Vertex3d face0Vtx = null;
      Vertex3d face1Vtx = null;
      Vertex3d face2Vtx = null;

      VertexDistancePair nearest =
         new VertexDistancePair(null, Double.POSITIVE_INFINITY);
      Point3d wpnt = new Point3d();
      Vertex3d vtx;

      // check the edge first
      vtx = mip.edge.getHead();
      if (vertexIsCoincident(vtx, mip, wpnt, nearest)) {
         headVtx = vtx;
      }
      vtx = mip.edge.getTail();
      if (vertexIsCoincident (vtx, mip, wpnt, nearest)) {
         tailVtx = vtx;
      }

      HalfEdge he0 = mip.face.firstHalfEdge();
      vtx = he0.getHead();
      if (vertexIsCoincident (vtx, mip, wpnt, nearest)) {
         face0Vtx = vtx;
      }
      vtx = he0.getNext().getHead();
      if (vertexIsCoincident (vtx, mip, wpnt, nearest)) {
         face1Vtx = vtx;
      }
      vtx = he0.getTail();
      if (vertexIsCoincident (vtx, mip, wpnt, nearest)) {
         face2Vtx = vtx;
      }

      if (nearest.myVtx != null) {
         if ((newVtx = map.get (nearest.myVtx)) == null) {
            nearest.myVtx.getWorldPoint (wpnt);
            newVtx = new Vertex3d (wpnt);
         }
         if (headVtx != null) {
            addVertexToMap (headVtx, newVtx, map);
         }
         if (tailVtx != null) {
            addVertexToMap (tailVtx, newVtx, map);
         }
         if (face0Vtx != null) {
            addVertexToMap (face0Vtx, newVtx, map);
         }
         if (face1Vtx != null) {
            addVertexToMap (face1Vtx, newVtx, map);
         }
         if (face2Vtx != null) {
            addVertexToMap (face2Vtx, newVtx, map);
         }
         isCoincident.value = true;
      }
      else {
         newVtx = new Vertex3d (mip);
         isCoincident.value = false;
      }
      return newVtx;
   }

   private void mapCoincidentVertices (
      IntersectionPoint mip, Vertex3d newVtx, HashMap<Vertex3d,Vertex3d> map) {

      Vertex3d vtx;
      Point3d wpnt = new Point3d();

      // check the edge first
      vtx = mip.edge.getHead();
      vtx.getWorldPoint (wpnt);
      if (wpnt.distance (mip) <= myPositionTol) {
         addVertexToMap (vtx, newVtx, map);
      }
      vtx = mip.edge.getTail();
      vtx.getWorldPoint (wpnt);
      if (wpnt.distance (mip) <= myPositionTol) {
         addVertexToMap (vtx, newVtx, map);
      }

      // then check the face
      HalfEdge he0 = mip.face.firstHalfEdge();
      HalfEdge he = he0;
      do {
         vtx = he.getHead();
         vtx.getWorldPoint (wpnt);
         if (wpnt.distance (mip) <= myPositionTol) {
            addVertexToMap (vtx, newVtx, map);
         }
         he = he.getNext();
      }
      while (he != he0);
   }

   private HashMap<Vertex3d,Vertex3d> createCSGVertices (
      ContactInfo cinfo, ArrayList<Vertex3d> contourVtxs) {

      HashMap<Vertex3d,Vertex3d> vertexMap =
         createContourVertices (cinfo.myContours, contourVtxs);
      for (int meshNum=0; meshNum<2; meshNum++) {
         for (PenetrationRegion r : cinfo.getRegions(meshNum)) {
            findEmptySegments (
               r, getMesh(meshNum), cinfo.getRegionsType(meshNum));
            for (Vertex3d vtx : r.myVertices) {
               if (vertexMap.get (vtx) == null) {
                  //System.out.println ("adding inside vertex");
                  vertexMap.put (vtx, new Vertex3d(vtx.getWorldPoint()));
               }
            }
         }
      }
      return vertexMap;
   }

   /**
    * Starting with an intersection point mip, back up until we find a point
    * whose preceeding point is *not* coincident, or is null
    */
   IntersectionPoint backupToNonCoincident (IntersectionPoint mip) {
      IntersectionPoint mip0 = mip;
      IntersectionPoint prev = mip.prev();
      if (prev != null && prev.distance (mip0) <= myPositionTol) {
         do {
            mip = prev;
            prev = mip.prev();
            if (prev == null || prev.distance(mip0) > myPositionTol) {
               return mip;
            }
         }
         while (mip != mip0);
      }
      return mip0;
   }

   /**
    * Creates mesh vertices corresponding to each contour intersection point in
    * a ContactInfo structure. These vertices can then be used in the formation
    * of a CSG mesh associated with the intersection. The vertex associated
    * with each intersection point is stored in the point's
    * <code>myVertex</code> field. A single common vertex is created for points
    * which are very close together. Also, for points which are very close to
    * an original vertex on one of the intersecting meshes, the newly created
    * vertex is projected onto the original vertex and an association from the
    * original to the new vertex is recorded in a vertex-vertex
    * <code>HashMap</code> which is returned by this method. Other new vertices
    * which are not associated with original vertices are stored in the
    * argument <code>contourVtxs</code>.
    */
   private HashMap<Vertex3d,Vertex3d> createContourVertices (
      ArrayList<IntersectionContour> contours, ArrayList<Vertex3d> contourVtxs) {
      
      // Create vertices for each contour ...

      boolean debug = false;

      LinkedHashMap<Vertex3d,Vertex3d> vertexMap =
         new LinkedHashMap<Vertex3d,Vertex3d>();
      for (IntersectionContour c : contours) {

         IntersectionPoint mip = c.get(0);         
         if (c.isClosed()) {
            // If the contour is closed, back up until we find a point whose
            // preceeding point is *not* coincident. There must be such a
            // point, because otherwise the contour would consist of a single
            // set of coincident points and would have been eliminated.
            mip = backupToNonCoincident(c.get(0));
            // IntersectionPoint prev = mip.prev();
            // if (prev.distance (mip0) <= myPositionTol) {
            //    do {
            //       mip = prev;
            //       prev = mip.prev();
            //    }
            //    while (prev.distance (mip0) <= myPositionTol && mip != mip0);
            // }  
            
         }
         
         // Create a new vertex at the starting intersection point. 
         BooleanHolder vtxIsCoincident = new BooleanHolder();
         Vertex3d newVtx = createNewVertex (mip, vertexMap, vtxIsCoincident);
         if (!vtxIsCoincident.value) {
            contourVtxs.add (newVtx);
         }
         mip.myVertex = newVtx;
         mip.effectiveFace0 = c.findSegmentFace (mip, myMesh0);
         mip.effectiveFace1 = c.findSegmentFace (mip, myMesh1);
         mip.nearEdge0 = null;
         mip.nearEdge1 = null;
         mip.clearEmptyMarks();
         for (int i=1; i<c.size(); i++) {
            mip = mip.next();
            
            // Question: do we want to cluster based on lastp, or newVtx.pnt?
            if (mip.distance(newVtx.pnt) > myPositionTol) {
               if (debug) {
                  System.out.println ("mip "+mip.contourIndex+" new vertex");
               }
               newVtx = createNewVertex (mip, vertexMap, vtxIsCoincident);
               if (!vtxIsCoincident.value) {
                  contourVtxs.add (newVtx);
               }
            }
            else {
               if (debug) {
                  System.out.println ("mip "+mip.contourIndex+" old vertex");
               }
               if (vtxIsCoincident.value) {
                  mapCoincidentVertices (mip, newVtx, vertexMap);
               }
            }
            mip.myVertex = newVtx;
            mip.effectiveFace0 = c.findSegmentFace (mip, myMesh0);
            mip.effectiveFace1 = c.findSegmentFace (mip, myMesh1);
            mip.nearEdge0 = null;
            mip.nearEdge1 = null;
            mip.clearEmptyMarks();
         }
      }
      return vertexMap;
   }

   Vertex3dList createPolyFromContour (IntersectionContour contour) {
      Vertex3dList poly = new Vertex3dList(/*closed=*/true);
      Vertex3d curVtx = null;
      IntersectionPoint mip = contour.get(0);
      if (contour.isClosed()) {
         // back up until previous mip has a different vertex
         IntersectionPoint prev = mip.prev();
         if (prev.myVertex == mip.myVertex) {
            IntersectionPoint mip0 = contour.get(0);
            do {
               mip = prev;
               prev = mip.prev();
            }
            while (prev.myVertex == mip.myVertex && mip != mip0);
         }
      }
      for (int i=0; i<contour.size(); i++) {
         if (mip.myVertex != curVtx) {
            curVtx = mip.myVertex;
            poly.add (curVtx);
         }
         mip = mip.next();
      }
      return poly;
   }

   /**
    * Find the polyhedral boundary of the CSG (constructive solid geometry)
    * intersection of two meshes. The resulting mesh will be empty if
    * <code>mesh0</code> and <code>mesh1</code> do not intersect.
    *
    * @param mesh0 first intersecting mesh
    * @param mesh1 second intersecting mesh
    * @return mesh describing the intersection boundary, or <code>null</code>
    * if no such mesh exists.
    */
   public PolygonalMesh findIntersection (
      PolygonalMesh mesh0, PolygonalMesh mesh1) {
      
      ContactInfo cinfo = findContoursAndRegions (
         mesh0, RegionType.INSIDE, mesh1, RegionType.INSIDE);
      return createCSGMesh (cinfo);
   }

   /**
    * Find the polyhedral boundary of the CSG (constructive solid geometry)
    * union of two meshes.
    *
    * @param mesh0 first intersecting mesh
    * @param mesh1 second intersecting mesh
    * @return mesh describing the union boundary.
    */
   public PolygonalMesh findUnion (
      PolygonalMesh mesh0, PolygonalMesh mesh1) {
      
      ContactInfo cinfo = findContoursAndRegions (
         mesh0, RegionType.OUTSIDE, mesh1, RegionType.OUTSIDE);
      return createCSGMesh (cinfo);
   }

   /**
    * Find the polyhedral boundary of the CSG (constructive solid geometry)
    * difference of two meshes, given by
    * <pre>
    * mesh0 - mesh1
    * </pre>
    * The resulting mesh will be empty if
    * <code>mesh0</code> is entirely inside <code>mesh1</code>.
    *
    * @param mesh0 first intersecting mesh
    * @param mesh1 second intersecting mesh
    * @return mesh describing the difference boundary.
    */
    public PolygonalMesh findDifference01 (
      PolygonalMesh mesh0, PolygonalMesh mesh1) {
      
      ContactInfo cinfo = findContoursAndRegions (
         mesh0, RegionType.OUTSIDE, mesh1, RegionType.INSIDE);
      return createCSGMesh (cinfo);
   }

   /**
    * Find the polyhedral boundary of the CSG (constructive solid geometry)
    * difference of two meshes, given by
    * <pre>
    * mesh1 - mesh0
    * </pre>
    * The resulting mesh will be empty if
    * <code>mesh1</code> is entirely inside <code>mesh0</code>.
    *
    * @param mesh0 first intersecting mesh
    * @param mesh1 second intersecting mesh
    * @return mesh describing the difference boundary.
    */
   public PolygonalMesh findDifference10 (
      PolygonalMesh mesh0, PolygonalMesh mesh1) {
      
      ContactInfo cinfo = findContoursAndRegions (
         mesh0, RegionType.INSIDE, mesh1, RegionType.OUTSIDE);
      return createCSGMesh (cinfo);
   }

   /**
    * Assuming that the boundaries of <code>mesh0</code> and <code>mesh1</code>
    * do not intersect, determines is <code>mesh0</code>
    * is inside <code>mesh1</code>.
    */
   private boolean isInside (PolygonalMesh mesh0, PolygonalMesh mesh1) {
      if (!mesh1.isClosed()) {
         // can't be "inside" a nonclosed mesh
         return false;
      }
      Face face0 = mesh0.getFace(0);
      // compute a test point just inside of the centroid of face0
      Point3d pnt = new Point3d();
      Vector3d nrm = new Vector3d();
      face0.computeWorldCentroid(pnt);
      face0.getWorldNormal(nrm);
      pnt.scaledAdd (-myPositionTol, nrm);
      return BVFeatureQuery.isInsideOrientedMesh (mesh1, pnt);
   }

   /**
    * Computes the boundary mesh formed by the union of the penetration
    * regions associated with two meshes. The meshes, intersection
    * contours, and penetration regions are described in the supplied 
    * {@link ContactInfo} object. These meshes and contours must also
    * correspond to those associated with the most recent intersection
    * contours computed by this intersector.
    * 
    * <p>Whether or not the penetration regions for each mesh are 
    * "inside" or "outside" the opposite mesh determines the type
    * of CSG volume produced:
    * 
    * <table summary="">
    *   <tr>
    *     <th>mesh0 regions</th>
    *     <th>mesh1 regions</th>
    *     <th>CSG volume</th>
    *   </tr>
    *   <tr>
    *     <td>inside</td>
    *     <td>inside</td>
    *     <td>intersection</td>
    *   </tr>
    *   <tr>
    *     <td>outside</td>
    *     <td>outside</td>
    *     <td>union</td>
    *   </tr>
    *   <tr>
    *     <td>outside</td>
    *     <td>inside</td>
    *     <td>difference (mesh0-mesh1)</td>
    *   </tr>
    *   <tr>
    *     <td>inside</td>
    *     <td>outside</td>
    *     <td>difference (mesh1-mesh0)</td>
    *   </tr>
    * </table>
    * @param cinfo meshes, contours and penetration regions from
    * which the CSG boundary is to be constructed
    * @return bounding mesh for resulting CSG volume
    * @throws IllegalStateException if the contours and meshes 
    * do not match the contours most recently produced by this intersector
    */
   public PolygonalMesh createCSGMesh (ContactInfo cinfo) {

      if (cinfo.myMesh0 != myMesh0 ||
          cinfo.myMesh1 != myMesh1 ||
          cinfo.myContours != myContours) {
         throw new IllegalStateException (
            "Meshes and/or contours contained in <code>cinfo</code> do not "+
            "match the contours most recently produced by this intersector");
      }
      
      PolygonalMesh csgMesh;      

      RegionType regions0 = cinfo.getRegionsType(0);
      RegionType regions1 = cinfo.getRegionsType(1);     
      if (cinfo.myContours.size() == 0) {
         // what to do here depends on the operation
         if (regions0 == RegionType.INSIDE && 
             regions1 == RegionType.INSIDE) {
            // intersection
            if (isInside (myMesh0, myMesh1)) {
               csgMesh = myMesh0.copy();
            }
            else if (isInside (myMesh1, myMesh0)) {
               csgMesh = myMesh1.copy();
            }
            else {
               csgMesh = new PolygonalMesh();
            }
         }
         else if (regions0 == RegionType.OUTSIDE && 
                  regions1 == RegionType.OUTSIDE) {
            // union
            csgMesh = myMesh0.copy();
            csgMesh.addMesh (myMesh1, /*respectTransforms=*/true);
         }
         else if (regions0 == RegionType.OUTSIDE && 
                  regions1 == RegionType.INSIDE) {
            // difference mesh0 - mesh1
            if (isInside (myMesh0, myMesh1)) {
               csgMesh = new PolygonalMesh();
            }
            else {
               csgMesh = new PolygonalMesh (myMesh0);
               if (isInside (myMesh1, myMesh0)) {
                  PolygonalMesh inside = myMesh1.copy();
                  inside.flip();
                  csgMesh.addMesh (inside, /*respectTransforms=*/true);
               }              
            }
         }
         else { // regions0 == INSIDE && regions1 == OUTSIDE
            // difference mesh1 - mesh0
            if (isInside (myMesh1, myMesh0)) {
               csgMesh = new PolygonalMesh();
            }
            else {
               csgMesh = new PolygonalMesh (myMesh1);
               if (isInside (myMesh0, myMesh1)) {
                  PolygonalMesh inside = myMesh0.copy();
                  inside.flip();
                  csgMesh.addMesh (inside, /*respectTransforms=*/true);
               }
            }            
         }
         return csgMesh;
      }
//      if (cinfo.myContours == null || cinfo.myContours.size() == 0) {
//         return null;
//      }
      csgMesh = new PolygonalMesh();
      
      triangulationError = false;
      // map from exiting vertices on either mesh to new intersection mesh
      // vertices

      // create the xips
      for (EdgeInfo einfo : myEdgeInfos.values()) {
         einfo.initializeXips();
      }

      // create the vertices
      ArrayList<Vertex3d> contourVtxs = new ArrayList<Vertex3d>();
      HashMap<Vertex3d,Vertex3d> vertexMap =
         createCSGVertices (cinfo, contourVtxs);

      for (Vertex3d vtx : vertexMap.values()) {
         csgMesh.addVertex (vtx);
      }
      for (Vertex3d vtx : contourVtxs) {
         csgMesh.addVertex (vtx);
      }

      if (printContours) {
         for (IntersectionContour c : cinfo.myContours) {
            System.out.println ("contour " + getContourIndex(c));
            for (IntersectionPoint p : c) {
               System.out.println (toString (p));
            }
         }
      }

      // create the faces. Allocate vertex space for faces based on the
      // approximation that there are two triangles per vertex
      ArrayList<Vertex3d> triVtxs =
         new ArrayList<Vertex3d>(csgMesh.numVertices()*6);

      HashMap<Face,ArrayList<IntersectionContour>> faceContourMap =
         new HashMap<Face,ArrayList<IntersectionContour>>();

      ArrayList<Vertex3dList> innerHoles = new ArrayList<Vertex3dList>();
      ArrayList<Vertex3dList> outerHoles = new ArrayList<Vertex3dList>();

      boolean clockwise0 = (regions0 == RegionType.OUTSIDE);
      boolean clockwise1 = (regions1 == RegionType.INSIDE);
      for (int meshNum=0; meshNum<2; meshNum++) {
         triVtxs.clear();
         boolean clockwise = (meshNum==0 ? clockwise0 : clockwise1);
         boolean flipFaces = 
            ((meshNum == 1 && clockwise0 && clockwise1) ||
             (meshNum == 0 && !clockwise0 && !clockwise1));
         //System.out.println (
         // "mesh "+meshNum+" numr="+cinfo.getPenetrationRegions(meshNum).size());
         for (PenetrationRegion r : cinfo.getRegions(meshNum)) {
            faceContourMap.clear();
            for (IntersectionContour c : r.myContours) {
               Face face = c.containingFace;
               if (face != null) {
                  ArrayList<IntersectionContour> contours =
                     faceContourMap.get(face);
                  if (contours == null) {
                     contours = new ArrayList<IntersectionContour>();
                     faceContourMap.put (face, contours);
                  }
                  contours.add (c);
               }
            }
            for (Face face : r.myFaces) {
               ArrayList<IntersectionContour> contours =
                  faceContourMap.get(face);
               if (contours != null) {
                  innerHoles.clear();
                  outerHoles.clear();
                  for (IntersectionContour c : contours) {
                     Vertex3dList hole = createPolyFromContour (c);
                     if (clockwise) {
                        hole.reverse();
                     }
                     if (c.singleFaceArea < 0) {
                        // if empty segments found, then assume hole has been
                        // connected to the face, and so is no longer a hole
                        if (!c.emptySegmentsMarked) {
                           innerHoles.add (hole);
                        }
                     }
                     else {
                        if (!c.emptySegmentsMarked) {
                           outerHoles.add (hole);
                        }
                     }
                  }
                  triangulateFace (
                     triVtxs, face, r, clockwise,
                     outerHoles, innerHoles, vertexMap);
               }
               else {
                  triangulateFace (
                     triVtxs, face, r, clockwise,
                     null, null, vertexMap);
               }
            }
         }
         for (int i=0; i<triVtxs.size(); i+=3) {
            if (flipFaces) {
               csgMesh.addFace (
                  triVtxs.get(i), triVtxs.get(i+2), triVtxs.get(i+1));
            }
            else {
               csgMesh.addFace (
                  triVtxs.get(i), triVtxs.get(i+1), triVtxs.get(i+2));
            }
         }
      }
      return csgMesh;
   }

   static int numCoincidentPnts = 0;
   //static int maxCoincidentMips = 0;
   //static int numCoincidentMips = 0;

   private void printFace (Face face) {
      HalfEdge he0 = face.firstHalfEdge();
      HalfEdge he = he0;
      do {
         System.out.println (
            he.getHead().getIndex()+" " +
            he.getHead().getWorldPoint().toString("%12.8f"));
         he = he.getNext();
      }
      while (he != he0);
   }


   private void printFaceMips (Face face) {
      HalfEdge he0 = face.firstHalfEdge();
      HalfEdge he = he0;
      do {
         EdgeInfo einfo = myEdgeInfos.get(he.getPrimary());
         System.out.println (
            "  " + he.vertexStr() +
            " " + (he == he.getPrimary() ?  ">" : "<"));
         NumberFormat fmt = new NumberFormat ("%3d");
         if (einfo != null) {
            int kstart = 0;
            int kinc = 1;
            int kend = einfo.numMips();
            if (he.getPrimary() != he) {
               kstart = einfo.numMips()-1;
               kinc = -1;
               kend = -1;
            }
            for (int k=kstart; k!=kend; k += kinc) {
               IntersectionPoint p = einfo.getMip(k);
               boolean entering = (p.findSegmentFace (face.getMesh()) == face);
               System.out.println (
                  "   " + fmt.format(p.contourIndex) + 
                  " " + p.toString("%12.8f") +
                  " " + getContourIndex(p.contour) + 
                  " " + (entering ? "E" : "X") + 
                  " " + (p.isCoincident() ? "C" : " "));
            }
         }
         he = he.getNext();
      }
      while (he != he0);
   }

   private void printFaceXips (Face face) {
      HalfEdge he0 = face.firstHalfEdge();
      HalfEdge he = he0;
      PolygonalMesh mesh = (PolygonalMesh)face.getMesh();
      int meshNum = (mesh == myMesh0 ? 0 : 1);
      do {
         EdgeInfo einfo = myEdgeInfos.get(he.getPrimary());
         System.out.println (
            "  " + he.vertexStr() +
            " " + (he == he.getPrimary() ?  ">" : "<"));
         NumberFormat fmt = new NumberFormat ("%3d");
         if (einfo != null) {
            for (int k=0; k<einfo.numXips(); k++) {
               IntersectionPoint p = einfo.getXip(k, he, /*clockwise=*/false);
               char[] desc = new char[] {' ', ' ', ' '};
               int emptyMark = p.getEmptyMark (meshNum);
               if (p.findSegmentFace (mesh) == face) {
                  if (he.getPrimary() == p.edge) {
                     desc[0] = 'E';
                  }
                  if ((emptyMark & IntersectionPoint.EMPTY_PROJECTED) != 0) {
                     desc[1] = 'P';
                  }
                  if ((emptyMark & IntersectionPoint.EMPTY_BEGIN) != 0) {
                     desc[2] = 'B';
                  }
                  else if ((emptyMark & IntersectionPoint.EMPTY_END) != 0) {
                     desc[2] = 'E';
                  }
               }
               else {
                  if (he.getPrimary() == p.edge) {
                     desc[0] = 'X';
                  }
                  if ((emptyMark & IntersectionPoint.EMPTY_PROJECTED) != 0) {
                     desc[1] = 'O';
                  }
                  if ((emptyMark & IntersectionPoint.EMPTY_BEGIN) != 0) {
                     desc[2] = 'B';
                  }
                  else if ((emptyMark & IntersectionPoint.EMPTY_END) != 0) {
                     desc[2] = 'E';
                  }
               }
               System.out.println (
                  "   " + fmt.format(p.contourIndex) + 
                  " " + p.toString("%12.8f") +
                  " " + getContourIndex(p.contour) + 
                  " " + new String(desc) + 
                  " " + (p.isCoincident() ? "C" : " "));
            }
         }
         he = he.getNext();
      }
      while (he != he0);
   }

   int[] findCoincidentBounds (
      IntersectionPoint p, EdgeInfo einfo) {
      
      int k = einfo.indexOfMip(p);
      int lo = k;
      while (lo > 0 &&
             einfo.getMip(lo-1).primaryCoincident == p.primaryCoincident) {
         lo--;
      }
      int hi = k;
      while (hi < einfo.numMips()-1 &&
             einfo.getMip(hi+1).primaryCoincident == p.primaryCoincident) {
         hi++;
      }
      return new int[] {lo, hi};
   }

   private int determineEdgeDirection (
      IntersectionPoint p0, IntersectionPoint p1, boolean convex, int idx) {

      if (idx%2 != 0) {
         convex = !convex;
      }
      if (convex) {
         return p0.headInsideFace() ? -1 : 1;
      }
      else {
         return p0.headInsideFace() ? 1 : -1;
      }
   }

   private int coincidentDirection (
      IntersectionPoint p0, IntersectionPoint p1, boolean debug) {

      int convex = commonEdgeConvexityTest (p0, p1, debug);
      if (convex == -1) {
         convex = commonVertexConvexityTest (p0, p1, debug);
      }
      if (convex != -1) {
         if (convex == 1) {
            // convex
            return p0.headInsideFace() ? 1 : -1;
         }
         else {
            // not convex
            return p0.headInsideFace() ? -1 : 1;
         }
      }
      else {
         return distanceDirectionTest (p0, p1, debug);
      }
   }

    /**
    * Sorts the coincident points associated with p. All coincident points
    * associated with p are found by locating all coincident points along the
    * edge that have the same 'coincidentGroup'. These points are then examined
    * in contour order, and then convexity tests are applied to see if this
    * contour ordering corresponds to a positive or negative direction along
    * the edge.  The points are then repositioned along the edge accordingly.
    *
    * <p>The convexity tests are applied to adjacent points, with the most
    * robust tests (but possibly indeterminant) tests applied first, until a
    * definite result is found.
    *
    * <p>Note that if there are more than two coincident points, it is not
    * generally possible to do this ordering until the entire contour is
    * generated, because the convexity tests need to be applied between
    * adjacent pairs, and we don't know the correct adjacency relationships
    * until all points have been added.
    */
   void sortCoincidentPoints (IntersectionPoint p) {

      HalfEdge edge = p.edge;
      EdgeInfo einfo = myEdgeInfos.get(p.edge);
      // find the lohi indices of the coincident points
      int[] lohi = findCoincidentBounds (p, einfo);
      IntersectionPoint plo = p;

      boolean debug = false; // p.edge.faceStr().equals("34-37");
      
      if (debug) {
         System.out.println ("Sort coincident "+edge.faceStr());
         System.out.println ("lohi=" + lohi[0]+" " + lohi[1]);
      }

      
      int numc = lohi[1]-lohi[0]+1;
      int lo = lohi[0];
      IntersectionPoint[] points = new IntersectionPoint[numc];

      if (numc == 2) {
         points[0] = einfo.getMip(lo);
         points[1] = einfo.getMip(lo+1);
      }
      else {
         int cnt = 1;
         for (IntersectionPoint prev = p.prev(); prev!=null; prev = prev.prev()) {
            if (prev.primaryCoincident == p.primaryCoincident) {
               plo = prev;
               cnt++;
            }
            // equal wouldn't work here, as coincident points not always equal:
            if (prev.distance (p) > myPositionTol) {
               break;
            }
         }
         if (debug) {
            System.out.println ("plo=" + plo.contourIndex);
         }
         IntersectionPoint phi = p;
         for (IntersectionPoint next = p.next(); next!=null; next = next.next()) {
            if (next.primaryCoincident == p.primaryCoincident) {
               phi = next;
               cnt++;
            }
            // equal wouldn't work here, as coincident points not always equal:
            if (next.distance (p) > myPositionTol) {
               break;
            }
         }
         if (debug) {
            System.out.println ("phi=" + phi.contourIndex);
         }
         if (cnt != numc) {
            System.out.println (
               "ERROR: cnt=" + cnt + " lohi=["+lohi[0]+","+lohi[1]+"]");
            if (false) {
               System.out.println ("problem written to contactTestFail.out");
               try {
                  SurfaceMeshIntersectorTest.writeProblem (
                     "contactTestFail.out", myMesh0, myMesh1, null);
               }
               catch (Exception e) {
                  e.printStackTrace();
               }
            }
         }
         if (!mySilentP && (debug || cnt > 2)) {
            System.out.println ("HIGH CNT=" + cnt);
         }
         
         int k = 0;
         IntersectionPoint primary = p.primaryCoincident;
         for (IntersectionPoint q=plo; k<cnt; q=q.next()) {
            if (q.primaryCoincident == primary) {
               //q.primaryCoincident = IntersectionPoint.COINCIDENT;
               points[k++] = q;
            }
         }
      }

      int direction = 0;
      for (int i=0; i<numc-1; i++) {
         int res = commonEdgeConvexityTest (points[i], points[i+1], debug);
         if (res != -1) {
            direction = determineEdgeDirection (points[i], points[i+1], res==1, i);
            if (debug) System.out.println (
               "commonEdge res="+res+" mips "+points[i].contourIndex+" "+
               points[i+1].contourIndex);
            break;
         }
      }
      if (direction == 0) {
         for (int i=0; i<numc-1; i++) {
            int res = commonVertexConvexityTest (points[i], points[i+1], debug);
            if (res != -1) {
               direction = determineEdgeDirection (points[i], points[i+1], res==1, i);
            if (debug) System.out.println (
               "commonVertex res="+res+" mips "+points[i].contourIndex+" "+
               points[i+1].contourIndex);
               break;
            }
         }
      }
      if (direction == 0) {
         int res = distanceConvexityTest (points[0], points[1], debug);
         if (debug) System.out.println (
            "distanceConvexity res="+res+" mips "+points[0].contourIndex+" "+
            points[1].contourIndex);
         direction = determineEdgeDirection (points[0], points[1], res==1, 0);
      }

      for (int k=0; k<numc; k++) {
         IntersectionPoint q;
         if (direction==1) {
            q = points[numc-1-k];
         }
         else {
            q = points[k];
         }         
         einfo.setMip (lo+k, q);
         q.primaryCoincident = IntersectionPoint.COINCIDENT;
      }
   }

   void checkCoincidentPoints (IntersectionContour c) {
      for (IntersectionPoint p : c) {
         if (p.primaryCoincident != null &&
             p.primaryCoincident != IntersectionPoint.COINCIDENT) {
            sortCoincidentPoints (p);
         }
      }
   }


   void checkEdgeMipsParameterValue() {
      for (HalfEdge edge : myEdgeInfos.keySet()) {
         EdgeInfo einfo = myEdgeInfos.get(edge);
         Vector3d udir = new Vector3d();
         Point3d head = new Point3d();
         Point3d tail = new Point3d();
         edge.getHead().getWorldPoint (head);
         edge.getTail().getWorldPoint (tail);
         udir.sub (head, tail);
         double ulen = udir.norm();
         udir.scale (1/ulen);
         Vector3d r = new Vector3d();
         double[] svals = new double[einfo.numMips()];
         for (int i=0; i<einfo.numMips(); i++) {
            IntersectionPoint mip = einfo.getMip(i);
            r.sub (mip, tail);
            double s = r.dot(udir);
            if (s < 0) {
               s = 0;
            }
            else if (s > ulen) {
               s = ulen;
            }
            svals[i] = s;
         }
         for (int i=1; i<einfo.numMips(); i++) {
            IntersectionPoint mip = einfo.getMip(i);
            if (svals[i] < svals[i-1] &&
                (einfo.getMip(i).primaryCoincident != 
                 einfo.getMip(i-1).primaryCoincident)) {
               if (!mySilentP) {
                  System.out.println (
                     "MIP s decreasing: " + svals[i-1] + " vs " + svals[i]);
               }
            }
            else if (svals[i] == svals[i-1] && 
                     (einfo.getMip(i).primaryCoincident != 
                      einfo.getMip(i-1).primaryCoincident)) {
               if (!mySilentP) {
                  System.out.println (
                     "MIP s equal: " + svals[i-1] + " vs " + svals[i]);
               }
            }
         }
      }
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
      myEdgeInfos.clear();
      myFaceCalcs0 = new FaceCalculator[myMesh0.numFaces()];
      myFaceCalcs1 = new FaceCalculator[myMesh1.numFaces()];

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
      myContours = contours; // myContours is used for debugging
      // make each contour counterClockwise with respect to mesh0
      for (IntersectionContour c : contours) {
         if (c.isClockwise (myMesh0, myMesh1)) {
            c.reverse();
         }
         checkCoincidentPoints (c);
      }
      if (true) {
         checkEdgeMipsParameterValue();
      }
      if (false) {
         for (IntersectionContour c : myContours) {
            System.out.println ("contour " + getContourIndex(c));
            c.printCornerPoints ("", "%18.14f,", null);
            for (IntersectionPoint p : c) {
               //System.out.println (toString (p));
            }
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
                              he.getPrimary(), f0, myWorkPoint, edgeOnMesh0)) {
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
      if (traceContourDebug) {
         System.out.println ("isContinuable=" + contour.isContinuable);
         System.out.println ("isClosed=" + contour.isClosed());
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
         if (traceContourDebug) {
            System.out.println ("single point or coincident contour");
         }         
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
               // contour.openMesh = true;
               edgeFace = null;
            }
            else {
               edgeFace = edge.getFace();
            }
         }
         else {
            edge = edgeIntersectingFace (otherFace, edgeFace, contour);
            if (edge == null) {
               if (traceContourDebug) {
                  System.out.println ("edgeFace set to null");
               }
               edgeFace = null;
            }
            else {
               edge = edge.opposite;
               if (edge == null) {
                  // contour.openMesh = true;
                  edgeFace = null;
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

   long tot = 0;

//   private boolean circledAround (
//      IntersectionPoint mip, IntersectionPoint lastAdded) {
//
//      for (IntersectionPoint p=mip.prev(); p!=lastAdded; p=p.prev()) {
//         if (p.distance(lastAdded) > myPositionTol) {
//            return true;
//         }
//      }
//      return false;
//   }
//
   
   EdgeInfo getEdgeInfo (HalfEdge edge) {
      EdgeInfo einfo = myEdgeInfos.get (edge);
      if (einfo == null) {
         einfo = new EdgeInfo(edge);
         myEdgeInfos.put (edge, einfo);
      }
      return einfo;
   }
   /*
    * Remember all the intersection points for the edge, in sorted order of
    * increasing distance from edge.tail. Mark coincident einfo.
    */
   void addIntersectionToEdge (IntersectionPoint mip) {
      EdgeInfo einfo = getEdgeInfo (mip.edge);
      numEdgeAdds++;
      if (einfo.numMips() == 0) {
         einfo.addMip (mip);
         return;
      }
      int idx;   // insertion index within mips list
      int q = 0; // comparision variable between mips
      int firstCoincidentIdx = -1;
      EdgeInfo.PointData mdata = null;
      double s = einfo.project(mip);
      for (idx=0; idx<einfo.numMips(); idx++) {
         mdata = einfo.getMipData (idx);
         if (mdata.s < s-myPositionTol) {
            q = 1;
         }
         else if (mdata.s > s+myPositionTol) {
            q = -1;
         }
         else {
            long t0 = System.nanoTime();
            q = RobustPreds.closestIntersection (
               mip.face, mip.edge, mdata.pnt.face);
            long t1 = System.nanoTime();
            tot += (t1-t0);
            numClosestIntersectionCalls++;
         }
         if (q == 0) {
            if (firstCoincidentIdx == -1) {
               firstCoincidentIdx = idx;
            }
         }
         if (q < 0) {
            break;
         }
      }
      if (firstCoincidentIdx != -1) {
         IntersectionPoint m = einfo.getMip(firstCoincidentIdx);
         if (!m.isCoincident()) {
            // first conincident pair
            m.primaryCoincident = m;
         }
         mip.primaryCoincident = m.primaryCoincident;
      }

         
      einfo.addMip (idx, mip, s);
      if (debug) {
         //System.out.println ("Face mips");
         //printFaceMips (mip.edge.getFace());
      }
   }



   private HalfEdge findEdgeForVertex (Face face, Vertex3d vtx) {
      HalfEdge he0 = face.firstHalfEdge();
      HalfEdge he = he0;
      do {
         if (he.getHead() == vtx) {
            return he;
         }
         he = he.getNext();
      }
      while (he != he0);
      return null;
   }

//   private double distanceToEdge (HalfEdge he, Vertex3d vtx) {
//      return LineSegment.distance (
//         he.getTail().pnt, he.getHead().pnt, vtx.pnt);
//   }
//
   private double distanceToFace (Point3d pnt, Face face, boolean debug) {
      Point3d near = new Point3d();
      face.nearestWorldPointTriangle (near, pnt);
      return near.distance (pnt);
   }

   private int distanceConvexityTest (
      IntersectionPoint p0, IntersectionPoint p1, boolean debug) {

      Point3d head = new Point3d();
      Point3d tail = new Point3d();
      p0.edge.getHead().getWorldPoint (head);
      p0.edge.getTail().getWorldPoint (tail);

      double distHeadFace0 = distanceToFace (head, p0.face, debug);
      double distTailFace0 = distanceToFace (tail, p0.face, debug);
      double distHeadFace1 = distanceToFace (head, p1.face, debug);
      double distTailFace1 = distanceToFace (tail, p1.face, debug);

      if (debug) {
         System.out.println (
            " faces: " + p0.face.getIndex() + " " + p1.face.getIndex());
         System.out.println (
            " distances: head0=" + distHeadFace0 + " head1=" + distHeadFace1 +
            " tail0=" + distTailFace0 + " tail1=" + distTailFace1);
      }
      

      if (Math.abs(distHeadFace0-distHeadFace1) >
          Math.abs(distTailFace0-distTailFace1)) {
         // head has largest distance differential, so base decision on it
         if (distHeadFace0 < distHeadFace1) {
            // head is closer to face0
            if (debug) {
               System.out.println (
                  "CONVEX "+p0.edge.faceStr()+
                  ": face distance head closer to face0 "+p0.face.getIndex());
            }
            return p0.headInsideFace() ? 0 : 1;        
         }
         else {
            // head is closer to face1
            if (debug) {
               System.out.println (
                  "CONVEX "+p0.edge.faceStr()+
                  ": face distance head closer to face1 "+p1.face.getIndex());
            }
            return p1.headInsideFace() ? 0 : 1;        
         }
      }
      else {
         // tail has largest distance differential, so base decision on it
         if (distTailFace0 < distTailFace1) {
            // tail is closer to face0
            if (debug) {
               System.out.println (
                  "CONVEX "+p0.edge.faceStr()+
                  ": face distance tail closer to face0 "+p0.face.getIndex());
            }
            return p0.headInsideFace() ? 1 : 0;            
         }
         else {
            // tail is closer to face1
            if (debug) {
               System.out.println (
                  "CONVEX "+p0.edge.faceStr()+
                  ": face distance tail closer to face1 "+p1.face.getIndex());
            }
            return p1.headInsideFace() ? 1 : 0;       
         }
      }
   }

   private int distanceDirectionTest (
      IntersectionPoint p0, IntersectionPoint p1, boolean debug) {

      Point3d head = new Point3d();
      Point3d tail = new Point3d();
      p0.edge.getHead().getWorldPoint (head);
      p0.edge.getTail().getWorldPoint (tail);

      double distHeadFace0 = distanceToFace (head, p0.face, debug);
      double distTailFace0 = distanceToFace (tail, p0.face, debug);
      double distHeadFace1 = distanceToFace (head, p1.face, debug);
      double distTailFace1 = distanceToFace (tail, p1.face, debug);

      if (debug) {
         System.out.println (
            " faces: " + p0.face.getIndex() + " " + p1.face.getIndex());
         System.out.println (
            " distances: head0=" + distHeadFace0 + " head1=" + distHeadFace1 +
            " tail0=" + distTailFace0 + " tail1=" + distTailFace1);
      }
      

      if (Math.abs(distHeadFace0-distHeadFace1) >
          Math.abs(distTailFace0-distTailFace1)) {
         // head has largest distance differential, so base decision on it
         if (distHeadFace0 < distHeadFace1) {
            // head is closer to face0
            if (debug) {
               System.out.println (
                  "DistanceDirection "+p0.edge.faceStr()+
                  ": face distance head closer to face0 "+p0.face.getIndex());
            }
            return -1;
         }
         else {
            // head is closer to face1
            if (debug) {
               System.out.println (
                  "DistanceDirection "+p0.edge.faceStr()+
                  ": face distance head closer to face1 "+p1.face.getIndex());
            }
            return 1;
         }
      }
      else {
         // tail has largest distance differential, so base decision on it
         if (distTailFace0 < distTailFace1) {
            // tail is closer to face0
            if (debug) {
               System.out.println (
                  "DistanceDirection "+p0.edge.faceStr()+
                  ": face distance tail closer to face0 "+p0.face.getIndex());
            }
            return 1;
         }
         else {
            // tail is closer to face1
            if (debug) {
               System.out.println (
                  "DistanceDirection "+p0.edge.faceStr()+
                  ": face distance tail closer to face1 "+p1.face.getIndex());
            }
            return -1;
         }
      }
   }



   /**
    * See if the faces associated with p0 and p1 have a common edge, and if
    * they do, determine convexity by using an orientation test between p0.face
    * and a non-adjacent vertex on p1.face. Return 1 if the faces are convex
    * and 0 if they are not, or -1 if convexity cannot be determined (i.e., the
    * faces do not have a common edge).
    */
   private int commonEdgeConvexityTest (
      IntersectionPoint p0, IntersectionPoint p1, boolean debug) {

      // start by seeing if faces should have a common edge
      HalfEdge he0 = p0.face.firstHalfEdge();
      HalfEdge he = he0;
      do {
         if (he.opposite != null && he.opposite.getFace() == p1.face) {
            // common edge found - find non-adjacent vertex on opposite face
            // and check convexity using that
            Vertex3d nonAdjacentVtx = he.opposite.getNext().getHead();
            boolean inside = vertexInsideFace (nonAdjacentVtx, p0.face, myMesh0);
            if (debug) {
               System.out.println (
                  "CONVEX "+p0.edge.faceStr()+
                  ": common edge " + inside);
            }
            return inside ? 1 : 0;
         }
         he = he.getNext();
      }
      while (he != he0);
      return -1;
   }

   /**
    * See if the faces associated with p0 and p1 have a common vertex, and then
    * if one face is completely the plane of the other, use that to establish
    * convexity. Return 1 if the faces are convex and 0 if they are not, or -1
    * if convexity cannot be determined (i.e., the faces do not have a common
    * vertex or one is not completely inside the plane of the other.
    */
   private int commonVertexConvexityTest (
      IntersectionPoint p0, IntersectionPoint p1, boolean debug) {

      HalfEdge he0 = p0.face.firstHalfEdge();
      HalfEdge he = he0;
      if (debug) {
         System.out.println ("Looking for common vertex");
      }
      HalfEdge commonVtxEdge1 = null;
      HalfEdge commonVtxEdge0 = null;
      do {
         commonVtxEdge1 = findEdgeForVertex (p1.face, he.getHead());
         if (commonVtxEdge1 != null) {
            commonVtxEdge0 = he;
            break;
         }
         he = he.getNext();
      }
      while (he != he0);      

      if (commonVtxEdge1 == null) {
         return -1;
      }
         
      Vertex3d vtx1 = commonVtxEdge1.getNext().getHead();
      Vertex3d vtx2 = commonVtxEdge1.getNext().getNext().getHead();

      boolean inside1 = vertexInsideFace (vtx1, p0.face, myMesh0);
      boolean inside2 = vertexInsideFace (vtx2, p0.face, myMesh0);

      if (inside1 == inside2) {
         if (debug) {
            System.out.println (
               "CONVEX "+p0.edge.faceStr()+
               ": used vertex inside face " + inside1);
         }
         return inside1 ? 1 : 0;
      }

      // try with respect to p1.face

      Vertex3d vtx1_0 = commonVtxEdge0.getNext().getHead();
      Vertex3d vtx2_0 = commonVtxEdge0.getNext().getNext().getHead();

      boolean inside1_0 = vertexInsideFace (vtx1_0, p1.face, myMesh0);
      boolean inside2_0 = vertexInsideFace (vtx2_0, p1.face, myMesh0);

      if (inside1_0 == inside2_0) {
         if (debug) {System.out.println (
               "CONVEX "+p0.edge.faceStr()+
               ": resolved with other face " + inside1_0);
         }
         return inside1_0 ? 1 : 0;
      }
      return -1;
   }

   private boolean vertexInsideFace (Vertex3d vtx, Face face, MeshBase mesh) {

      HalfEdge he0 = face.firstHalfEdge();
      HalfEdge he = he0;
      Vertex3d v0 = he.getHead();
      he = he.getNext();
      Vertex3d v1 = he.getHead();
      he = he.getNext();
      Vertex3d v2 = he.getHead();

      return RobustPreds.orient3d (
         v0, v1, v2, vtx, vtx.getMesh()==mesh, /*worldCoords=*/true);
   }

   /**
    * Remove an intersection point <code>mip</code> from the set of
    * intersections associated with its edge.
    */
   void removeIntersectionFromEdge (IntersectionPoint mip) {
      EdgeInfo einfo = myEdgeInfos.get (mip.edge);
      if (einfo == null) {
         writeErrorFile (/*csg=*/null);
         throw new InternalErrorException (
            "No edge intersections found for edge + " + mip.edge.vertexStr());
      }
      int i = einfo.indexOfMip (mip);
      if (i == -1) {
         writeErrorFile (/*csg=*/null);
         throw new InternalErrorException (
            "Intersection point not found on edge + " + mip.edge.vertexStr());
      }         
//      if (mip.isCoincident) {
//         // XXX need to fix this
//      }
      einfo.removeMip (i);
//      if (einfo.isEmpty()) {
//         myEdgeInfos.remove (mip.edge);
//      }
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
   HalfEdge getPointEdge (IntersectionPoint pa, Face face) {
      PolygonalMesh mesh = (PolygonalMesh)face.getMesh();
      if (edgeOnMesh (pa.edge, mesh)) {
         if (pa.edge.getFace() == face) {
            return pa.edge;
         }
         else if (pa.edge.getOppositeFace() == face) {
            return pa.edge.opposite;
         }
         else {
            writeErrorFile (/*csg=*/null);
            throw new InternalErrorException (
               "Face edge not found for point " + pa);
         }
      }
      else {
         (new Throwable()).printStackTrace(); 
         System.out.println ("GETTING POINT EDGE FROM FACE");
         System.out.println ("face=" + face.getIndex());
         System.out.println ("mesh0=" + (face.getMesh() == myMesh0));
         System.out.println ("pa=" + pa.contourIndex);
         System.out.println (
            "pa.edge=" + pa.edge.faceStr());
         // convert pa to mesh local coordinates
         Point3d paLoc = new Point3d(pa);
         paLoc.inverseTransform (mesh.getMeshToWorld());
         HalfEdge he0 = face.firstHalfEdge();
         HalfEdge he = he0;
         HalfEdge heMin = null;
         double dmin = Double.POSITIVE_INFINITY;
         do {
            double d = LineSegment.distance (
               he.getHead().getWorldPoint(),
               he.getTail().getWorldPoint(), paLoc);
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
    * is directed from the tail to the head.
    * @return nearest intersection point along the "inside" direction of the edge
    */
   IntersectionPoint nearestInsideMip (
      IntersectionPoint p, HalfEdge edge, 
      Vertex3d insideVtx, PolygonalMesh mesh) {

      EdgeInfo einfo = myEdgeInfos.get (edge.getPrimary());

      boolean debug = debugMipEdge.equals (edge.vertexStr());

      if (einfo != null && einfo.numMips() > 0) {
         int k = einfo.indexOfMip (p);
         if (debug) {
            NumberFormat fmt = new NumberFormat ("%2d ");
            for (int j=0; j<einfo.numMips(); j++) {
               IntersectionPoint mp = einfo.getMip(j);
               String prefix = (j == k ? "* " : "  ");
               prefix += fmt.format (getContourIndex(mp.contour));
               prefix += (mp.isCoincident() ? "C " : "  ");
               System.out.println (prefix + mp.toString("%g"));
            }
            System.out.println ("k=" + k);
         }

         if (debug) {
            for (int ii=p.contourIndex-3; ii<p.contourIndex+4; ii++) {
               System.out.println (
                  toString (p.contour.getWrapped(ii)));
            }
         }

         if (k != -1) {
            if (edge.getPrimary().getHead() == insideVtx) { //p.headInsideFace() ^ !inside) {
               // search towards the head
               while (k < einfo.numMips()-1) {
                  IntersectionPoint mip = einfo.getMip(++k);
                  if (mip.contour.dividesMesh (mesh)) {
                     return mip;
                  }
               }
            }
            else {
               // search towards the tail
               while (k > 0) {
                  IntersectionPoint mip = einfo.getMip(--k);
                  if (mip.contour.dividesMesh (mesh)) {
                     return mip;
                  }                 
               }
            }
         }
         else {
            writeErrorFile (/*csg=*/null);
            throw new InternalErrorException (
               "intersection point not recorded by its edge");
         }
      }
      if (debug) {
         System.out.println ("No nearest inside mip");
      }
      return null;
   }

//   String insideVertexDebug = "";
   
//   static int inside = 0;
//   static int outside = 0;

//   /**
//    * Returns the vertex that is on the inside of an edge associated
//    * with an intersection point.
//    */
//   Vertex3d getInsideVertex (IntersectionPoint p, boolean inside) {
//      if (p.headInsideFace() ^ !inside) {
//         return p.edge.getHead();
//      }
//      else {
//         return p.edge.getTail();
//      }
//   }
   
   /**
    * For a given edge, return the nearest intersection point to a specified
    * vertex on that edge, or <code>null</code> if the edge contains no
    * intersection points.
    */
   IntersectionPoint nearestMipToVertex (
      HalfEdge edge, Vertex3d v, PolygonalMesh mesh) {
      EdgeInfo einfo = myEdgeInfos.get (edge);
      if (einfo != null && einfo.numMips() > 0) {
         if (v == edge.getTail()) {
            for (int k=0; k<einfo.numMips(); k++) {
               IntersectionPoint mip = einfo.getMip(k);
               if (mip.contour.dividesMesh (mesh)) {
                  return mip;
               }
            }
         }
         else {
            for (int k=einfo.numMips()-1; k>=0; k--) {
               IntersectionPoint mip = einfo.getMip(k);
               if (mip.contour.dividesMesh (mesh)) {
                  return mip;
               }
            }            
         }
      }
      return null;
   }

   static int maxCoincident = 0;
   static int numCoincident = 0;
   static int numMarkedCoincident = 0;
   //static int numFaceOutsideTests = 0;

   static int fcalcDebugIdx = -1; //3; // 10; //69;
   static boolean fcalcDebugMesh0 = true;

   private void writeErrorFile (CSG csg) {
      if (writeErrorFiles) {                    
         File tempFile;
         try {
            tempFile = File.createTempFile ("smi_error_", ".txt");
            PrintWriter pw = new IndentingPrintWriter (
               new PrintWriter (new BufferedWriter (new FileWriter (tempFile))));
            SurfaceMeshIntersectorTest.writeProblem (
               pw, myMesh0, myMesh1, null, csg);
            pw.close();
         }
         catch (IOException e) {
            System.out.println ("Error trying to write error file");
            e.printStackTrace();
            return;
         }
         System.err.println ("Error file written to "+tempFile);
      }
   }

   /**
    * Computes intersecting area information for a particular face. For
    * efficiency, this calculation is done with respect to the dominant plane
    * (in world coordinates) associated with the face - i.e., the plane that is
    * perpendicular to the largest component of the face normal.
    */
   class FaceCalculator extends Polygon3dCalc {

      Face myFace;
      IntersectionContour contour; // current contour
      double outsideArea; // computted outside area for this face
      
      HalfEdge myHe0;
      EdgeInfo myEdgeInfo0;
      HalfEdge myHe1;
      EdgeInfo myEdgeInfo1;
      HalfEdge myHe2;
      EdgeInfo myEdgeInfo2;

      boolean debug = false;

      boolean isDebugging (Face face) {
         return (face.getIndex() == fcalcDebugIdx &&
                 fcalcDebugMesh0 == (face.getMesh() == myMesh0));
      }      

      boolean myClockwise;

      public void initializeForEdgeCalcs (boolean clockwise, boolean debug) {
         myClockwise = clockwise;
         this.debug = debug;
         if (myEdgeInfo1 == null) {
            myHe1 = myHe0.getNext();
            myEdgeInfo1 = getEdgeInfo (myHe1.getPrimary());
            myHe2 = myHe1.getNext();
            myEdgeInfo2 = getEdgeInfo (myHe2.getPrimary());           
         }
      }
      
      public FaceCalculator (Face face) {
         this.myFace = face;
         debug =
            (face.getIndex() == fcalcDebugIdx &&
             fcalcDebugMesh0 == (face.getMesh() == myMesh0));
         setDominantPlane (face);

         myHe0 = face.firstHalfEdge();
         myEdgeInfo0 = getEdgeInfo (myHe0.getPrimary());
      }
      
      void setDominantPlane (Face face) {
         Vector3d nrm = new Vector3d();
         face.getWorldNormal (nrm);
         setPlane (nrm);
      }

      double addOutsideArea (Point3d p1, Point3d p2, boolean clockwise) {
         double a = computeArea (myEdgeInfo0.myTail, p1, p2);
         if (!clockwise) {
            a = -a;
         }
         if (debug) {
            if (p2 instanceof IntersectionPoint) {
               int idx = ((IntersectionPoint)p2).contourIndex;
               System.out.println (
                  "fcalc: adding to outside area, mip "+idx + ": " + a);
            }
            else {
               System.out.println ("fcalc: adding to outside area: " + a);
            }
         }
         outsideArea += a;
         return a;
      }

      IntersectionPoint nextEnteringPoint (
         HalfEdge edge, IntersectionPoint pstart, boolean clockwise) {

         EdgeInfo einfo = myEdgeInfos.get (edge.getPrimary());
         IntersectionPoint penter = null;
         if (einfo != null) {
            boolean tailToHead = clockwise;
            if (edge != edge.getPrimary()) {
               tailToHead = !tailToHead;
            }
            int kstart;
            if (pstart != null) {
               kstart = einfo.indexOfMip (pstart);
               if (kstart == -1) {
                  System.out.println (
                     "pstart=" + pstart.contourIndex + ", contour " +
                     getContourIndex(pstart.contour));
                  System.out.println ("face "+myFace.getIndex()+":");
                  printFace(myFace);
                  printFaceMips(myFace);
                  writeErrorFile (/*csg=*/null);
                  throw new InternalErrorException (
                     "Starting point "+pstart.contourIndex+
                     " not located on initial edge " + edge.vertexStr() +
                     " pstart.edge=" + pstart.edge.vertexStr());
               }
            }
            else {
               kstart = tailToHead ? -1 : einfo.numMips();
            }
            if (tailToHead) {
               for (int k=kstart+1; k<einfo.numMips(); k++) {
                  if (einfo.getMip(k).contour == contour) {
                     penter = einfo.getMip(k);
                     break;
                  }
               }
            }
            else {
               for (int k=kstart-1; k>=0; k--) {
                  if (einfo.getMip(k).contour == contour) {
                     penter = einfo.getMip(k);
                     break;
                  }
               }
            }
         }
         return penter;
      }

      void addBoundaryArea (
         IntersectionPoint pexit, IntersectionPoint pold, boolean clockwise) {

         HalfEdge edge = getPointEdge (pexit, myFace);
         if (debug) {
            System.out.println (
               "fcalc: adding boundary begin: pexit="+pexit.contourIndex +
               " edge=" + edge.vertexStr() + " clockwise=" + clockwise);
         }
         
         Point3d vpnt = new Point3d();
         Point3d vpntLast = new Point3d();

         // run around the face along the outside until we find the first
         // entering point on the current contour
         IntersectionPoint penter;
         Point3d plast = pexit;
         while ((penter=nextEnteringPoint(edge, pexit, clockwise)) == null) {
            Vertex3d vtx;
            if (clockwise) {
               vtx = edge.getHead();
               edge = edge.getNext();
            }
            else {
               // for triangular meshes, equivalent to getPrev()
               vtx = edge.getTail();
               edge = edge.getNext().getNext(); 
            }
            vtx.getWorldPoint (vpnt);
            if (debug) {
               System.out.println ("fcalc:   boundary vertex " + vtx.getIndex());
            }
            addOutsideArea (plast, vpnt, clockwise);
            vpntLast.set (vpnt);
            plast = vpntLast;
            pexit = null;
         }
         PolygonalMesh mesh = (PolygonalMesh)myFace.getMesh();
         if (contour.findSegmentFace (penter, mesh) != myFace) {
            System.out.println ("face "+myFace.getIndex()+":");
            printFace(myFace);
            printFaceMips(myFace);
            writeErrorFile (/*csg=*/null);
            throw new InternalErrorException (
               "Next contour boundary point " + penter.contourIndex +
               " not entering face "+myFace.getIndex()+
               " mesh0="+(myFace.getMesh()==myMesh0));
         }
         addOutsideArea (plast, penter, clockwise);
         if (debug) {
            System.out.println (
               "fcalc: adding boundary end: outsideArea=" + outsideArea);
         }
      }
      
      double edgeDistance (Point3d p, EdgeInfo einfo) {
         double a;
         switch (myPlane) {
            case YZ: {
               double ry = p.y - einfo.myTail.y;
               double rz = p.z - einfo.myTail.z;
               a = einfo.myUdir.y*rz - einfo.myUdir.z*ry;
               break;
            }
            case ZX: {
               double rz = p.z - einfo.myTail.z;
               double rx = p.x - einfo.myTail.x;
               a = einfo.myUdir.z*rx - einfo.myUdir.x*rz;
               break;
            }
            case XY: {
               double rx = p.x - einfo.myTail.x;
               double ry = p.y - einfo.myTail.y;
               a = einfo.myUdir.x*ry - einfo.myUdir.y*rx;
               break;
            }
            default: {
               throw new InternalErrorException (
                  "Plane has not been initialized");
            }
         }
         return Math.abs(a*myInvNrmMax);
      }
      
      HalfEdge nearestBoundingEdge (IntersectionPoint p) {

         double nearestDist = edgeDistance (p, myEdgeInfo0);
         HalfEdge nearestEdge = myHe0;
         double d = edgeDistance (p, myEdgeInfo1);
         if (d < nearestDist) {
            nearestDist = d;
            nearestEdge = myHe1;
         }
         d = edgeDistance (p, myEdgeInfo2);
         if (d < nearestDist) {
            nearestDist = d;
            nearestEdge = myHe2;
         }
         return nearestEdge;
      }

      boolean onEdge (IntersectionPoint p, HalfEdge edge) {
         return edgeDistance (p, getLocalEdgeInfo (edge)) < myPositionTol;
      }

      boolean onEdge (IntersectionPoint p, EdgeInfo einfo) {
         return edgeDistance (p, einfo) < myPositionTol;
      }

      HalfEdge findNearEdge (IntersectionPoint p) {

         double nearDist = myPositionTol;
         HalfEdge nearEdge = null;

         double d = edgeDistance (p, myEdgeInfo0);
         if (d <= myPositionTol) {
            nearDist = d;
            nearEdge = myHe0;
         }
         d = edgeDistance (p, myEdgeInfo1);
         if ((nearEdge == null && d < myPositionTol) || (d < nearDist)) {
            nearDist = d;
            nearEdge = myHe1;
         }
         d = edgeDistance (p, myEdgeInfo2);
         if ((nearEdge == null && d < myPositionTol) || (d < nearDist)) {
            nearDist = d;
            nearEdge = myHe2;
         }
         return nearEdge;
      }

      EdgeInfo getLocalEdgeInfo (HalfEdge edge) {
         if (edge == myHe0) {
            return myEdgeInfo0;
         }
         else if (edge == myHe1) {
            return myEdgeInfo1;
         }
         else { // (edge == myHe2) 
            return myEdgeInfo2;
         }
      }

      double dotEdge (Vector3d vec, HalfEdge edge) {
         return getLocalEdgeInfo(edge).dot (vec, edge);
      }

      HalfEdge followingBoundary (
         IntersectionPoint p0, IntersectionPoint p1,
         HalfEdge lastEdge, boolean clockwise) {

         Vector3d d10 = new Vector3d();
         HalfEdge curEdge;
         if (onEdge (p1, lastEdge)) {
            d10.sub (p1, p0);
            if (getLocalEdgeInfo(lastEdge).dot (d10, lastEdge, clockwise) < 0) {
               return lastEdge;
            }
         }
         else if ((curEdge = findNearEdge (p1)) != null) {
            d10.sub (p1, p0);
            if (getLocalEdgeInfo(curEdge).dot (d10, curEdge, clockwise) < 0) {
               return curEdge;
            }
         }
         return null;
      }

      boolean projectsToEdge (
         IntersectionPoint p, HalfEdge edge, 
         double se, double sx, double posTol) {
         if (nearestBoundingEdge(p) == edge) {
            double s = edgeProjection (edge, p);
            return (s > se+posTol && s < sx-posTol);
         }
         else {
            return false;
         }
      }

      double edgeProjection (HalfEdge edge, IntersectionPoint p) {
         
         Vector3d tmp = new Vector3d();
         double s;
         if (edge == myHe0) { 
            s = myEdgeInfo0.project (p, myHe0, !myClockwise);
         }
         else if (edge == myHe1) { 
            s = myEdgeInfo1.project (p, myHe1, !myClockwise);
         }
         else {
            s = myEdgeInfo2.project (p, myHe2, !myClockwise);
         }
         return s;
      }

      double edgeLength (HalfEdge edge) {
         if (edge == myHe0) { 
            return myEdgeInfo0.myUlen;
         }
         else if (edge == myHe1) { 
            return myEdgeInfo1.myUlen;
         }
         else {
            return myEdgeInfo2.myUlen;
         }         
      }

      IntersectionPoint findNearestEnteringXip (
         HalfEdge edge, PenetrationRegion region, double posTol) {

         PolygonalMesh mesh = (PolygonalMesh)myFace.getMesh();

         if (debug) {
            System.out.println (
               "  finding enter for "+edge.vertexStr()+
               " clockwise="+myClockwise);
         }

         // start with current edge; see if there is an entering point all the
         // way at the end
         EdgeInfo einfo =
            myEdgeInfos.get(edge.getPrimary());

         if (einfo != null) {
            boolean nearestToTail = myClockwise;
            IntersectionPoint efound = null;
            if (edge != edge.getPrimary()) {
               nearestToTail = !nearestToTail;
            }
            if (nearestToTail) {
               for (int k=0; k<einfo.numXips(); k++) {
                  IntersectionPoint mip = einfo.getXip(k);
                  if (edgeProjection (edge, mip) > posTol) {
                     break;
                  }
                  if (mip.findSegmentFace (mesh) == myFace) {
                     efound = mip;
                  }
               }
            }
            else {
               for (int k=einfo.numXips()-1; k>=0; k--) {
                  IntersectionPoint mip = einfo.getXip(k);
                  if (edgeProjection (edge, mip) > posTol) {
                     break;
                  }
                  if (mip.findSegmentFace (mesh) == myFace) {
                     efound = mip;
                  }
               }
            }
            if (efound != null) {
               return efound;
            }
         }

         // try going back along the edges
         HalfEdge emptyEdge = edge;
         for (int i=0; i<3; i++) {
            // note the the edge corresponding to i==2 will be myHe0.
            // should we check projection?
            emptyEdge = getNextEdge (emptyEdge, myClockwise);
            einfo = myEdgeInfos.get(emptyEdge.getPrimary());
            if (einfo != null && einfo.numXips() > 0) {
               boolean nearestToTail = !myClockwise;
               if (emptyEdge != emptyEdge.getPrimary()) {
                  nearestToTail = !nearestToTail;
               }
               IntersectionPoint mip =
                  nearestToTail ? einfo.getXip(0) : einfo.getXip(einfo.numXips()-1);
               if (mip.findSegmentFace (mesh) == myFace) {
                  if (debug) {
                     System.out.println ("  found mip " + mip.contourIndex);
                  }
                  return mip;
               }
               else {
                  if (debug) {
                     System.out.println (
                        "  no mip found");
                  }
                  return null;
               }
            }
         }

         // see if the region has a nested contour completely inside the face
         double ulen = edgeLength (edge);
         for (IntersectionContour c : region.myContours) {
            if (c.containingFace == myFace && c.singleFaceArea < 0) {
               // the inside contour proceeds around the inner boundary of the
               // face in a direction opposite to that indicated by clockwise
               IntersectionPoint emip0 = c.get(0);
               IntersectionPoint emip = emip0;
               IntersectionPoint efound = null;
               // search forwards until we find a mip that projects to the edge
               do {
                  if (projectsToEdge (emip, edge, 0, ulen, posTol)) {
                     efound = emip;
                     break;
                  }
                  emip = emip.next();
               }
               while (emip != emip0);
               if (efound != null) {
                  // if we do find a mip that projects to the edge, search
                  // backwards until we find one that doesn't. That's the
                  // entering point
                  emip0 = efound;
                  emip = emip0;
                  do {
                     emip = emip.prev();
                     if (!projectsToEdge (emip, edge, 0, ulen, posTol)) {
                        return emip;
                     }
                  }
                  while (emip != emip0);              
                  // don't think we should get here, since the completely
                  // nested contour must have points near the other edges
                  return efound;
               }
            }
         }
         return null;
      }

      void setContour (IntersectionContour c) {
         this.contour = c;
         //this.outsideArea = 0;
      }
   }

   /**
    * Gets a <code>FaceCalculator</code> object for a specified
    * <code>face</code> from <code>map</code>, allocating a new object if one
    * does not yet exist.
    */
   FaceCalculator getFaceCalculator (
      Face face, FaceCalculator[] faceCalcs) {
      FaceCalculator fcalc = faceCalcs[face.getIndex()];
      if (fcalc == null) {
         fcalc = new FaceCalculator(face);
         faceCalcs[face.getIndex()] = fcalc;
      }
      return fcalc;
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
    * @param clockwise <code>true</code> if intersection contours are
    * oriented clockwise with respect to <code>mesh0</code>
    * @return penetration region associated with <code>contour</code>
    */
   PenetrationRegion findPenetrationRegion (
      IntersectionContour contour, PolygonalMesh mesh0, PolygonalMesh mesh1,
      boolean clockwise) {

      PenetrationRegion region =
         new PenetrationRegion (mesh0, clockwise, myPositionTol);
      Deque<IntersectionContour> contoursToTrace =
         new ArrayDeque<IntersectionContour>();
      region.myContours.add (contour);
      contoursToTrace.offerLast (contour);
      Deque<Vertex3d> verticesToTrace = new ArrayDeque<Vertex3d>();

      FaceCalculator[] faceCalcs = 
         (mesh0 == myMesh0 ? myFaceCalcs0 : myFaceCalcs1);
      boolean[] visitedFaces = new boolean[mesh0.numFaces()];

      IntersectionContour c;      
      while ((c = contoursToTrace.pollFirst()) != null) {

         boolean debug = false; //(getContourIndex(c) == 0 && mesh0 == myMesh0);

         int kenter = c.getFirstFaceEntryIndex(mesh0, /*face=*/null);
         if (kenter == -1) {
            // contour is not open and is confined to a single face
            //region.myInsideFaces.add (lastFace);
            Face face = c.findSegmentFace (0, mesh0);
            region.mySingleFace = face;
            c.containingFace = face;

            FaceCalculator fcalc =
               getFaceCalculator (face, faceCalcs);
            
            fcalc.setContour (c);
            if (!visitedFaces[face.getIndex()]) {
               visitedFaces[face.getIndex()] = true;
               fcalc.outsideArea = 0;
            }           
            for (int i=0; i<c.size(); i++) {
               IntersectionPoint pa = c.get(i);
               IntersectionPoint pb = c.getWrapped(i+1);
               fcalc.addOutsideArea (pa, pb, clockwise);
            }
            region.mySingleFaceArea = -fcalc.outsideArea;
            c.singleFaceArea = region.mySingleFaceArea;
            if (fcalc.outsideArea <= 0) {
               fcalc.outsideArea += face.computeArea();
            }
         }
         else {
            // Go along the contour, looking for face transitions. At each
            // transition, examine the associated edge for adjacent vertices or
            // contours.
            IntersectionPoint pentry = null; // point where the contour enters
                                             // a face
            FaceCalculator fcalc = null;
            Face lastFace = c.findSegmentFace (kenter-1, mesh0);

            int k = kenter;
            for (int i=0; i<c.size(); i++) {
               IntersectionPoint pa = c.getWrapped(k);
               IntersectionPoint pb = c.getWrapped(k+1);                

               Face face = c.findSegmentFace (k, mesh0);

               if (lastFace != face) {
                  boolean headInsideFace;
                  HalfEdge faceEdge;
                  if (face != null) {
                     // we are leaving lastFace and entering face
                     //region.myInsideFaces.add (face);
                     faceEdge = getPointEdge (pa, face);
                     pentry = pa;
                     fcalc = getFaceCalculator (face, faceCalcs);
                     if (fcalc.contour != c) {
                        fcalc.setContour (c);
                     }
                     if (!visitedFaces[face.getIndex()]) {
                        visitedFaces[face.getIndex()] = true;
                        fcalc.outsideArea = 0;
                     }
                     headInsideFace = clockwise;
                  }
                  else {
                     // Last point of an open contour. Choose faceEdge using 
                     // lastFace
                     faceEdge = getPointEdge (pa, lastFace);
                     headInsideFace = !clockwise;
                  }
                  Vertex3d insideVtx;
                  if (headInsideFace) {
                     insideVtx = faceEdge.getHead();
                  }
                  else {
                     insideVtx = faceEdge.getTail();                     
                  }
                  EdgeInfo einfo =
                     myEdgeInfos.get (faceEdge.getPrimary());
                  if (einfo != null && einfo.indexOfMip(pa) != -1) {
                     region.myEdges.add (faceEdge.getPrimary());
                     if (debug) debugMipEdge = faceEdge.vertexStr();
                     IntersectionPoint insideMip = 
                        nearestInsideMip (pa, faceEdge, insideVtx, mesh0);
                     if (debug) {
                        debugMipEdge = ""; // DBG
                        System.out.println (" insideMip=" + insideMip);
                     }

                     if (insideMip == null) {
                        if (region.myVertices.add (insideVtx)) {
                           verticesToTrace.offerLast (insideVtx);
                        }
                     }           
                     else {
                        String rname = getRegionName(region);
                        if (region.myContours.add(insideMip.contour)) {
                           if (regionAddingDebug) {
                              System.out.println (
                                 "added "+getContourIndex(insideMip.contour)+
                                 " to "+rname);
                           }
                           contoursToTrace.offerLast (insideMip.contour);
                        }
                     }
                  }
               }
               if (face != null) {
                  fcalc.addOutsideArea (pa, pb, clockwise);
                  Face nextFace = c.findSegmentFace (k+1, mesh0);
                  if (nextFace != face) {
                     // exiting face
                     // compute area contribution for face boundary from exit to
                     // entry
                     fcalc.addBoundaryArea (pb, pentry, clockwise);
                  }
               }
               lastFace = face;
               k = c.getWrappedIndex (k+1);
            }
         }
         Vertex3d vtx;
         while ((vtx = verticesToTrace.pollFirst()) != null) {
            Iterator<HalfEdge> incidentEdges = vtx.getIncidentHalfEdges();
            while (incidentEdges.hasNext()) {
               HalfEdge edge = incidentEdges.next().getPrimary();
               if (region.myEdges.add (edge)) {
                  visitedFaces[edge.getFace().getIndex()] = true;
                  if (edge.opposite != null) {
                     visitedFaces[edge.opposite.getFace().getIndex()] = true;
                  }
                  Vertex3d v = edge.head;
                  if (v == vtx) {
                     v = edge.tail;
                  }
                  IntersectionPoint mip = nearestMipToVertex (edge, vtx, mesh0);
                  if (mip != null) {
                     String rname = getRegionName(region);                     
                     if (region.myContours.add(mip.contour)) {
                        if (regionAddingDebug) {
                           System.out.println (
                              "added (v) "+getContourIndex(mip.contour)+
                              " to "+rname);
                        }
                        contoursToTrace.offerLast (mip.contour);
                     }
                  }
                  else {
                     if (region.myVertices.add (v)) {
                        verticesToTrace.offerLast (v);
                     }                    
                  }
               }
            }
         }
      }
      double regionArea = 0;

      for (int i=0; i<visitedFaces.length; i++) {
      //for (FaceCalculator fcalc : faceCalcMap.values()) {
         if (visitedFaces[i]) {
            Face face = mesh0.getFace(i);
            double insideArea = face.computeArea();
            FaceCalculator fcalc = faceCalcs[i]; 
            if (fcalc != null) {
               insideArea = face.computeArea() - fcalc.outsideArea;
               fcalc.outsideArea = 0;
            }
            regionArea += insideArea;
            double atol = (removeZeroAreaFaces ? myAreaTol : 0);
            if (insideArea >= atol) {
               region.myFaces.add (face);            
            }
         }
      }
      region.setArea (regionArea);
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
         if (r.mySingleFace == null && r.myFaces.contains (face)) {
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
      PenetrationRegion region =
         new PenetrationRegion(mesh, clockwise, myPositionTol);
      region.myVertices.addAll (mesh.getVertices());
      for (Face face : mesh.getFaces()) {
         region.myFaces.add (face);
         HalfEdge he0 = face.firstHalfEdge();
         HalfEdge he = he0;
         do {
            region.myEdges.add (he.getPrimary());
            he = he.getNext();
         }
         while (he != he0);
      }
      region.setArea (mesh.computeArea());
      return region;
   }
   /**
    * For debugging: create a name for a penetration region based on the
    * indices of all its contours surrounded by square brackets.
    */
   private String getRegionName (PenetrationRegion r) {
      String str = "[ ";
      for (IntersectionContour c : r.myContours) {
         str += ""+getContourIndex(c) + " ";
      }
      return str + "]";
   }

   /**
    * Get a test point that is inside a planar contour (with the plane normal
    * given by nrm), for use in testing its relationship with other contours.
    */
   Point3d createTestPoint (
      IntersectionContour c, Vector3d nrm, boolean clockwise) {

      Point3d pt = new Point3d();
      IntersectionPoint p0 = c.get(0);
      IntersectionPoint p1 = null;
      IntersectionPoint p2 = null;

      IntersectionPoint p;
      for (p = p0.next(); p != p0 && p != null; p = p.next()) {
         if (p.distance(p0) > myPositionTol) {
            p1 = p;
            break;
         }
      }
      if (p1 != null) {
         for (p = p1.next(); p != p0 && p != null; p = p.next()) {
            if (LineSegment.distance (p0, p, p1) > myPositionTol) {
               p2 = p;
               break;
            }
         }
      }
      if (p1 == null || p2 == null) {
         // can't find a corner point - just use p0
         return new Point3d (p0);
      }
      else {
         // compute a small point interior to the corner
         Vector3d xprod = new Vector3d();
         Vector3d dir = new Vector3d();
         Vector3d d01 = new Vector3d();
         Vector3d d12 = new Vector3d();
         d01.sub (p1, p0);
         d12.sub (p2, p1);
         xprod.cross (d01, d12);
         if (xprod.dot (nrm) > 0) {
            dir.sub (d12, d01);            
         }
         else {
            dir.sub (d01, d12); 
         }
         if (clockwise) {
            dir.negate();
         }
         Point3d pr = new Point3d(p1);
         pr.scaledAdd (100*myPositionTol,dir);
         return pr;
      }
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
      int numCheck, boolean clockwise) {

      double dtol = EPS*face.computeCircumference();

      // if (face.getIndex()==2) {
      //    System.out.println ("checking " + getName(region));
      // }

      // The method works as follows: For each of the nested regions being
      // checked, we select a test point that is inside the region.  We then
      // use NearestPolygon3dFeature to find the nearest feature to the test
      // point among all of the polygonal segments formed from the contours of
      // <code>region</code> that cross the face. This nearest feature can then
      // be queried to see if the test point is inside or outside, given the
      // contour orientation specified by <code>clockwiseContour</code>.

      // Create and initialize nearest feature objects for each region of
      // <code>nested</code> being checked.
      NearestPolygon3dFeature[] nearestFeats =
         new NearestPolygon3dFeature[numCheck];
      for (int i=0; i<numCheck; i++) {
         PenetrationRegion r = nested.get(i);
         NearestPolygon3dFeature nfeat = new NearestPolygon3dFeature();
         nearestFeats[i] = nfeat;
         Vector3d nrm = face.getWorldNormal();
         Point3d testp = createTestPoint (r.getFirstContour(), nrm, clockwise);
         nfeat.init (testp, nrm, dtol);
      }
      PolygonalMesh mesh = (PolygonalMesh)face.getMesh();

      // Now find the nearest feature to the test point among all the contours
      // of <code>region</code>:
      for (IntersectionContour c : region.myContours) {
         int csize = c.size();

         // Try to find the first point where the contour enters this face and
         // store its index in kenter:
         int kenter = c.getFirstFaceEntryIndex (mesh, face);
         if (kenter == -1) {
            // kenter was not found, so if lastFace == face, the contour lies
            // entirely within face and so we check the test point against the
            // entire contour.
            if (!c.isClosed()) {
               // Contour should be closed - check so that calling
               // getWrapped(k) will work OK.
               writeErrorFile (/*csg=*/null);
               throw new InternalErrorException (
                  "Contour entirely within face but contour is not closed");
            }
            if (c.findSegmentFace (0, mesh) == face) {
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
            Face lastFace = null;
            for (int j=0; j<csize; j++) {
               for (int i=0; i<numCheck; i++) {
                  if (j==0) {
                     // contour c entering face for the first time
                     nearestFeats[i].restart ();
                     nearestFeats[i].advance (c.get(k));
                     lastFace = face;
                  }
                  else {
                     Face segFace = c.findSegmentFace (k, mesh);
                     if (lastFace == face) {
                        // continuing on the face 
                        nearestFeats[i].advance (c.get(k));
                     }
                     else if (segFace == face) {
                        // reentering the face
                        nearestFeats[i].restart ();
                        nearestFeats[i].advance (c.get(k));
                     }
                     else {
                        // not on the face
                     }
                     lastFace = segFace;
                  }
               }
               if (++k == csize) {
                  if (!c.isClosed()) {
                     // end of contour, so we are done
                     break;
                  }
                  k = 0;
               }
            }
         }
      }
      ArrayList<PenetrationRegion> contained =
         new ArrayList<PenetrationRegion>();
      for (int i=0; i<numCheck; i++) {
         NearestPolygon3dFeature nfeat = nearestFeats[i];
         boolean isContained;
         if (nfeat.numVertices() == 1) {
            writeErrorFile (/*csg=*/null);
            throw new InternalErrorException (
               "region's contours intersect face "+face.getIndex()+
               " at only one point.");

            // Former code from when we though it might be possible for a face
            // to interect a mesh at only one point: Then try to use the local
            // intersection geometry around this point (returned by
            // nfeat.getVertex(0)) to determine if the test point is inside or
            // outside the penetration region.

            //isContained = !isPointOutside (
            //   nfeat.getPoint(), face, 
            //   (IntersectionPoint)nfeat.getVertex(0), mesh, 
            //   clockwiseContour, myPositionTol);
         }
         else if (nfeat.isOutside (clockwise) != -1) {
            // XXX should check distance and redo if it is -1
            isContained = (nfeat.isOutside (clockwise) == 0);
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
      ArrayList<PenetrationRegion> nested, boolean clockwise) {
      
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
            region, face, nested, numCheck, clockwise);
      if (contained.size() > 0) {
         nested.removeAll (contained);
         for (PenetrationRegion r : contained) {
            String rname = getRegionName(region);
            if (region.myContours.add (r.getFirstContour())) {
               region.addArea (r.mySingleFaceArea);
               // if (region.myTestArea < 0) {
               //    System.out.println (
               //       "region.testArea=" + region.myTestArea +
               //       " r.testArea=" + r.myTestArea +
               //       " r.mySingleFaceArea=" + r.mySingleFaceArea);
               // }
               
               if (regionAddingDebug) {
                  System.out.println (
                     "added (nested) " + getContourIndex(r.getFirstContour()) +
                     " to " + rname);
               }
            }
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
    * Describes CSG operations.
    */
   public enum CSG {
      INTERSECTION,
      UNION,
      DIFFERENCE01,
      DIFFERENCE10,
      NONE
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
    * @param clockwise <code>true</code> if the intersection contours
    * are oriented clockwise with respect to <code>mesh0</code>
    */  
   void processSingleFaceRegions (
      ArrayList<PenetrationRegion> regions, Face face,
      ArrayList<PenetrationRegion> singleFaceRegions,
      PolygonalMesh mesh0, boolean clockwise) {

      // initially, each single-face face region consists of a closed contour
      // located within its single face. We use the sign of the area of each of
      // these regions to separated them into those which are either "isolated"
      // (postive) or "nested" (negative). Isolated regions surround their own
      // penetration area and may contain nested regions. For nested regions,
      // the penetration area is on their outside.
      ArrayList<PenetrationRegion> nested = new ArrayList<PenetrationRegion>();
      ArrayList<PenetrationRegion> isolated = new ArrayList<PenetrationRegion>();

      int initialRegionsSize = regions.size();
      
      for (PenetrationRegion r : singleFaceRegions) {
         if (r.mySingleFaceArea < 0) {
            nested.add (r);
         }
         else {
            isolated.add (r);
         }
      }

      if (regionAddingDebug) {
         System.out.println (
            "process single face regions for face "+ face.getIndex());
         System.out.println ("nested:");
         for (PenetrationRegion r : nested) {
            System.out.println (" "+ getRegionName(r));
         }
         System.out.println ("isolated:");
         for (PenetrationRegion r : isolated) {
            System.out.println (" "+ getRegionName(r));
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
            addContainedRegions (r, face, nested, clockwise);           
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
            if (initialRegionsSize > 0) {
               writeErrorFile (/*csg=*/null);
               throw new InternalErrorException (
                  "No regions reference a face with nested contours");
            }
            // create a region that contains the whole mesh
            PenetrationRegion r = 
               createWholeMeshRegion (mesh0, clockwise);
            //System.out.println ("creating whole mesh region");
            regions.add (r);
            faceContainingRegions.add (r);
         }
         if (faceContainingRegions.size() == 1) {
            // just one region - add the contours to it
            PenetrationRegion region = faceContainingRegions.get(0);

            for (PenetrationRegion r : nested) {
               String rname = getRegionName(region);
               if (region.myContours.add (r.getFirstContour())) {
                  region.addArea (r.mySingleFaceArea);
                  if (Math.abs(r.mySingleFaceArea+face.computeArea()) <
                      myAreaTol) {
                     // then the nested contour fills the whole face
                     // and so the face should be removed from the region
                     region.myFaces.remove (face);
                  }
                  if (regionAddingDebug) {
                     System.out.println (
                        "added (nested) " + getContourIndex(r.getFirstContour()) +
                        " to single region " + rname);
                  }
               }
            }
         }
         else {
            // multiple regions. Need to determine which nested region
            // belongs to which
            for (PenetrationRegion r : faceContainingRegions) {
               addContainedRegions (r, face, nested, clockwise);
            }
            if (nested.size() > 0) {
               writeErrorFile (/*csg=*/null);
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
      PolygonalMesh mesh0, PolygonalMesh mesh1,
      boolean clockwise) {

      int oldSize = regions.size();
      // contour which have yet to be used for finding regions:
      LinkedList<IntersectionContour> unusedContours = 
         new LinkedList<IntersectionContour>();
      // container to store single-face regions
      LinkedHashMap<Face,ArrayList<PenetrationRegion>> singleFaceRegions =
         new LinkedHashMap<Face,ArrayList<PenetrationRegion>>();

      for (IntersectionContour c : contours) {
         if (c.dividesMesh (mesh0)) {
            unusedContours.add (c);
         }
      }
      while (!unusedContours.isEmpty()) {
         IntersectionContour contour = unusedContours.removeFirst();
         PenetrationRegion region =
            findPenetrationRegion (
               contour, mesh0, mesh1, clockwise);
         // DBG System.out.println ("found region " + getName(region));
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
               writeErrorFile (/*csg=*/null);
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
            // 

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
                  regions, entry.getKey(), entry.getValue(), mesh0, clockwise);
            }
         }
         catch (Exception e) {
            e.printStackTrace(); 
            throw e;
         }
      }
      return regions.size()-oldSize;
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
                  Point3d wpnt = new Point3d();
                  if (debug2) {
                     wpnt.set (myWorkPoint);
                     if (debugTBW != null) {
                     }
                  }
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

   static public int numRegularCalls = 0;
   static public int numRobustCalls = 0;
   static public int numEdgeAdds;
   static public int numClosestIntersectionCalls;
   static public int numRobustClosestIntersectionCalls;

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
      
      int intersects = RobustPreds.intersectEdgeTriangle (
         mip, he, face, myMaxLength, edgeOnMesh0, /*worldCoords=*/true);
//      int intersects = face.intersectsEdge (he, mip, myMaxLength);
//      numRegularCalls++;
//      if (intersects == -1) {
//         intersects = 
//            RobustPreds.intersectEdgeFace (he, face, mip, edgeOnMesh0);
//         numRobustCalls++;
//      }
      if (intersects == 0) {
         return false;
      }
      else {
         mip.edge = he;
         mip.face = face;
         mip.intersectionCode = intersects;
         return true;     
      }
   }

   // Triangulation code:

   IntersectionPoint nextEdgeXip (
      IntersectionPoint mip, HalfEdge edge,
      boolean clockwise, boolean debug) {
      
      // tailToHead means that we are moving in the tail to head
      // direction with respect to the edge associated with mips
      boolean tailToHead = (edge.getPrimary() == edge);
      if (clockwise) {
         tailToHead = !tailToHead;
      }
      HalfEdge pedge = edge.getPrimary();
      EdgeInfo einfo = myEdgeInfos.get (pedge);
      int k = einfo.indexOfXip(mip);

      if (tailToHead) {
         if (k < einfo.numXips()-1) {
            return einfo.getXip(k+1);
         }
      }
      else {
         if (k > 0) {
            return einfo.getXip(k-1);
         }
      }
      return null;
   }

   public Vertex3d addEmptyFaceVertices (
      Vertex3dList poly, HalfEdge edge, 
      IntersectionPoint emip, IntersectionPoint xmip,
      PenetrationRegion region, 
      Vertex3d curVtx, boolean clockwise, boolean debug) {
      
      if (!addEmptyFaceVertices) {
         return curVtx;
      }
      // FaceEdgeCalculator fcalc =
      //    new FaceEdgeCalculator (edge.opposite, region, clockwise, debug);

      Face emptyFace = edge.opposite.getFace();
      PolygonalMesh mesh = (PolygonalMesh)emptyFace.getMesh();
      int meshNum = (region.myMesh == myMesh0 ? 0 : 1);
      
      FaceCalculator fcalc;
      if (mesh == myMesh0) {
         fcalc = getFaceCalculator (emptyFace, myFaceCalcs0);
      }
      else {
         fcalc = getFaceCalculator (emptyFace, myFaceCalcs1);
      }
      fcalc.initializeForEdgeCalcs (clockwise, debug);

      if (debug) {
         System.out.println (
            " EmptyFace opposite " + edge.opposite.vertexStr() +
            " clockwise=" + clockwise + 
            " emip=" + (emip==null ? "null" : emip.contourIndex));
      }
      
      double se = 0.0;
      double sx;
      if (emip != null) {
         se = fcalc.edgeProjection (edge.opposite, emip);
      }
      else {
         emip = fcalc.findNearestEnteringXip (
            edge.opposite, region, myPositionTol);
      }
      if (xmip != null) {
         sx = fcalc.edgeProjection (edge.opposite, xmip);
      }
      else {
         sx = fcalc.edgeLength (edge.opposite);
      }

      if (emip != null) {
         IntersectionPoint emip0 = emip;
         emip = emip.next();
         while (emip != null && emip != emip0 &&
                //(emip.findSegmentFace(mesh) == emptyFace ||
                (emip.getEffectiveFace(meshNum) == emptyFace ||
                emip.distance(emip0) <= myPositionTol)) {

            if (fcalc.projectsToEdge (emip, edge.opposite, se, sx, myPositionTol)) {
               if (emip.myVertex != curVtx) {
                  if (debug) {
                     System.out.println (
                        "   ADD extra vertex "+emip.contourIndex+
                        " "+emip.toString ("%12.8f"));
                  }
                  curVtx = emip.myVertex;
                  poly.add (curVtx);
               }
            }
            emip = emip.next();
         }
      }
      return curVtx;
   }

   private Vertex3d getNewVertex (
      HalfEdge edge, HashMap<Vertex3d,Vertex3d> vertexMap, boolean clockwise) {
      Vertex3d vtx;
      if (clockwise) {
         vtx = edge.getTail();
      }
      else {
         vtx = edge.getHead();
      }
      Vertex3d newVtx = vertexMap.get(vtx);
      if (newVtx == null) {
         Face face = edge.getFace();
         writeErrorFile (/*csg=*/null);
         throw new InternalErrorException (
            "No new vertex found for "+vtx.getIndex()+
            " face=" + face.getIndex() +
            " mesh0=" + (face.getMesh()==myMesh0));
      }
      else {
         return newVtx;
      }
   }

   boolean traceFaceContinue (
      IntersectionPoint mip, Face face, int meshNum) {
      return (mip.getEffectiveFace(meshNum) == face &&
              (mip.getEmptyMark(meshNum) & IntersectionPoint.EMPTY_BEGIN) == 0);
   }

   boolean traceFaceBegin (
      IntersectionPoint mip, Face face, int meshNum) {

      if (mip.getEffectiveFace(meshNum) == face) {
         if (mip.edge.getFace() == face || mip.edge.getOppositeFace() == face) {
            return (mip.getEmptyMark(meshNum) & IntersectionPoint.EMPTY_BEGIN) == 0;
         }
         else {
            return (mip.getEmptyMark(meshNum) & IntersectionPoint.EMPTY_END) != 0;
         }
      }
      
      // if (mip.edge.getFace() == face || mip.edge.getOppositeFace() == face) {
      //    return mip.getEffectiveFace(meshNum) == face;
      // }
      // else {
      //    return (mip.getEmptyMark(meshNum) & IntersectionPoint.EMPTY_END) != 0;
      // }
      return false;
   }

   HalfEdge getExtendedPointEdge (IntersectionPoint pa, Face face) {
      PolygonalMesh mesh = (PolygonalMesh)face.getMesh();
      if (edgeOnMesh (pa.edge, mesh)) {
         if (pa.edge.getFace() == face) {
            return pa.edge;
         }
         else if (pa.edge.getOppositeFace() == face) {
            return pa.edge.opposite;
         }
         else {
            writeErrorFile (/*csg=*/null);
            throw new InternalErrorException (
               "Face edge not found for point " + pa +
               ", face=" + face.getIndex() + " mesh0=" + (mesh==myMesh0));
         }
      }
      else {
         int meshNum = (mesh == myMesh0 ? 0 : 1);
         return pa.getNearEdge (meshNum);
      }
   }

   Vertex3dList traceFacePolygon (
      IntersectionPoint mip0, Face face, PenetrationRegion region,
      boolean clockwise, Polygon3dCalc calc, 
      HashSet<IntersectionPoint> visitedMips,
      HashMap<Vertex3d,Vertex3d> vertexMap) {

      Vertex3dList poly = new Vertex3dList(/*closed=*/true);
      PolygonalMesh mesh = (PolygonalMesh)face.getMesh();
      int meshNum = (region.myMesh == myMesh0 ? 0 : 1);
      HalfEdge edge; // = getPointEdge (mip0, face);

      // XXX handle coincident points in nearestInsideMip

      IntersectionPoint mip = mip0;

      boolean debug = isDebugFace (face);

      if (debug) {
         System.out.println (
            "TRACE face poly " + face.getIndex() +
            " clockwise=" + clockwise);
      }

      Vertex3d firstVtx = null;
      Vertex3d curVtx = null;      
      do {
         IntersectionContour c = mip.contour;
         if (debug) {
            System.out.println (
               " entering "+mip.toString("%12.7f") + " " + mip.isCoincident());
            System.out.println (
               " visiting "+mip.contourIndex +
               " on " + getContourIndex(mip.contour));
         }
         if (!visitedMips.add (mip)) {
            System.out.println ("faceMips for " + face.getIndex() + ":");
            printFaceXips (face);
               for (IntersectionContour con : myContours) {
                  System.out.println ("contour "+getContourIndex(con)+":");
                  for (IntersectionPoint p : con) {
                     System.out.println (toString (p));
                  }
               }
            writeErrorFile (/*csg=*/null);  
            throw new InternalErrorException (
               "Mip "+mip.contourIndex+
               " already visited on face " + face.getIndex() +
               " mesh0=" + (mesh==myMesh0));
         }

         //while ((segFace=c.findSegmentFace(k, mesh)) == face) {
         while (traceFaceContinue (mip, face, meshNum)) {
            if (mip.myVertex != curVtx) {
               curVtx = mip.myVertex;
               poly.add (curVtx);
               if (debug) {
                  System.out.println (
                     " ADD " + mip.contourIndex +
                     " " + curVtx.pnt.toString ("%20.16f"));
               }
               if (firstVtx == null) {
                  firstVtx = curVtx;
               }
            }
            mip = mip.next();
         }
         if (mip.myVertex != curVtx) {
            curVtx = mip.myVertex;
            poly.add (curVtx);
            if (debug) {
               System.out.println (
                  " ADD " + mip.contourIndex +
                  " " + curVtx.pnt.toString ("%20.16f"));
            }
         }
         if (debug) {
            System.out.println (" exiting at "+mip.contourIndex);
         }
         visitedMips.add (mip);
         edge = getExtendedPointEdge (mip, face);
         EdgeInfo einfo = myEdgeInfos.get (edge.getPrimary());         
         int mi0 = einfo.normalizedIndexOfXip (mip, edge, clockwise);
         IntersectionPoint nextMip = null;

         while (nextMip == null) {
            for (int mi=mi0+1; mi<einfo.numXips(); mi++) {
               IntersectionPoint p = einfo.getXip (mi, edge, clockwise);
               int emptyMark;
               if (debug) {
                  System.out.println (
                     " mip "+p.contourIndex+" mark=" + p.getEmptyMark(meshNum));
               }
               if ((emptyMark=p.getEmptyMark(meshNum)) != 0) {
                  if (p.findSegmentFace(mesh) != face) {
                     if ((emptyMark & IntersectionPoint.EMPTY_PROJECTED) != 0) {
                        if (p.myVertex != curVtx) {
                           curVtx = p.myVertex;
                           poly.add (curVtx);
                           if (debug) {
                              System.out.println (
                                 " ADD P(a) " + p.contourIndex +
                                 " " + curVtx.pnt.toString ("%20.16f"));
                           }
                        }
                     }
                  }
                  else {
                     if ((emptyMark & IntersectionPoint.EMPTY_END) != 0) {
                        nextMip = p;
                        break;
                     }
                     else if ((emptyMark & IntersectionPoint.EMPTY_BEGIN) != 0) {
                        // XXX don't really like this but it seems to work.  If
                        // we encouter EMPTY_BEGIN while following the edges in
                        // the inside direction, we assume this means that the
                        // entire polygon that has been built up is in fact
                        // infinitesimal and contained within and empty
                        // segment. Hence we just clear the polygon and return;
                        // the empty polygon will be discarded.
                        if (debug) {
                           System.out.println (
                              "SUB POLY inside empty region, mip " +
                              p.contourIndex + " on "+getContourIndex(p.contour));
                        }
                        poly.clear();
                        return poly;
                     }
                  }
               }
               else {
                  nextMip = p;
                  break;
               }
            }
               
            if (!useEmptySegmentProjection) {
               if (edge.opposite != null &&
                   !region.myFaces.contains(edge.opposite.getFace())) {
                  curVtx = addEmptyFaceVertices (
                     poly, edge, mip, nextMip, region, curVtx,
                     clockwise, debug);
               }
            }

            if (nextMip == null) {
               Vertex3d newVtx = getNewVertex (edge, vertexMap, clockwise);
               if (newVtx != curVtx && newVtx != firstVtx) {
                  // only add if unique from previously added vertices 
                  curVtx = newVtx;
                  poly.add (curVtx);
                  if (debug) {
                     Vertex3d oldVtx =
                        (clockwise ? edge.getTail() : edge.getHead());
                     System.out.println (
                        " ADD vtx(a) "+oldVtx.getIndex()+
                        ":" + oldVtx.getWorldPoint().toString("%20.16f"));
                  }
               }
               edge = getNextEdge (edge, clockwise);
               einfo = myEdgeInfos.get (edge.getPrimary());
               mip = null;
               mi0 = -1;
            }
         }
         mip = nextMip;
      }
      while (mip != mip0);
      if (debug) {
         System.out.println ("TRACE face poly end, size=" + poly.size());
      }
      // check if we added an extra vertex when the mesh was closed
      if (poly.get(0) == poly.get(poly.size()-1)) {
         poly.remove (poly.getNode(poly.size()-1));
      }
      if (clockwise) {
         if (debug) {
            System.out.println ("reversing");
         }
         poly.reverse();
      }
      return poly;
   }

   private int debugFaceIdx = -1; //148; // 625;
   private int debugOpFaceIdx = -1; //118; //115; //34; // 625;
   private boolean debugMesh0 = false;

   private boolean isDebugFace (Face face) {
      return ((face.getIndex() == debugFaceIdx) &&
              ((face.getMesh()==myMesh0) == debugMesh0));
   }

   HalfEdge getNextEdge (HalfEdge he, boolean clockwise) {
      if (!clockwise) {
         he = he.getNext();
      }
      else {
         // for a triangular mesh, this is the same as "getPrev()"
         he = he.getNext().getNext();
      }
      return he;
   }

   Vertex3dList createTrianglePoly (
      Face face, PenetrationRegion region, HashMap<Vertex3d,Vertex3d> vertexMap,
      boolean clockwise, boolean debug) {
      
      Vertex3dList poly = new Vertex3dList(/*closed=*/true);
      Vertex3d curVtx = null;
      Vertex3d firstVtx = null;
      HalfEdge edge0 = face.firstHalfEdge();                              
      HalfEdge edge = edge0; 
      EdgeInfo einfo = myEdgeInfos.get (edge.getPrimary());

      PolygonalMesh mesh = (PolygonalMesh)face.getMesh();
      int meshNum = (mesh == myMesh0 ? 0 : 1);

      do {
         if (einfo != null) {
            for (int mi=0; mi<einfo.numXips(); mi++) {
               IntersectionPoint p = einfo.getXip (mi, edge, clockwise);
               int emptyMark;
               if ((emptyMark=p.getEmptyMark(meshNum)) != 0) {
                  if (p.findSegmentFace(mesh) != face &&
                      (emptyMark & IntersectionPoint.EMPTY_PROJECTED) != 0) {
                     if (p.myVertex != curVtx) {
                        curVtx = p.myVertex;
                        poly.add (curVtx);
                        if (debug) {
                           System.out.println (
                              " ADD P(b) " + p.contourIndex +
                              " " + curVtx.pnt.toString ("%20.16f"));
                        }
                        if (firstVtx == null) {
                           firstVtx = curVtx;
                        }
                     }
                  }
               }
            }
         }

         if (!useEmptySegmentProjection) {
            if (edge.opposite != null &&
                !region.myFaces.contains(edge.opposite.getFace())) {
               curVtx = addEmptyFaceVertices (
                  poly, edge, null, null, region, curVtx,
                  clockwise, debug);
            }
         }

         Vertex3d newVtx = getNewVertex (edge, vertexMap, clockwise);         
         if (newVtx != curVtx && newVtx != firstVtx) {
            curVtx = newVtx;
            if (debug) {
               Vertex3d oldVtx =
                  (clockwise ? edge.getTail() : edge.getHead());
               System.out.println (
                  " ADD vtx(c) " + oldVtx.getIndex() +
                  " " + curVtx.pnt.toString ("%20.16f"));
            }
            poly.add (curVtx);
         }
         edge = getNextEdge (edge, clockwise);
         einfo = myEdgeInfos.get (edge.getPrimary());
      }
      while (edge != edge0);
      if (clockwise) {
         poly.reverse();
      }
      return poly;
   }

   void triangulateFace (
      ArrayList<Vertex3d> triVtxs, Face face,
      PenetrationRegion region, boolean clockwise,
      ArrayList<Vertex3dList> outerHoles, ArrayList<Vertex3dList> innerHoles,
      HashMap<Vertex3d,Vertex3d> vertexMap) {

      PolygonalMesh mesh = (PolygonalMesh)face.getMesh();
      int meshNum = (region.myMesh == myMesh0 ? 0 : 1);
      Polygon3dCalc calc =
         new Polygon3dCalc (face.getWorldNormal(), myPositionTol);

      HalfEdge he0 = face.firstHalfEdge();
      HalfEdge he = he0;

      boolean debug = isDebugFace(face);
      int oldSize = triVtxs.size();

      if (debug) {
         System.out.println ("TRIANGULATING " + debugFaceIdx);
         System.out.println ("normal=" + face.getWorldNormal());
         System.out.println ("tol=" + myPositionTol);

         Vertex3d v0 = he0.getHead();
         Vertex3d v1 = he0.getNext().getHead();
         Vertex3d v2 = he0.getTail();
         System.out.println (
            " vtx "+v0.getIndex() + " inside=" + region.myVertices.contains(v0));
         System.out.println (
            " vtx "+v1.getIndex() + " inside=" + region.myVertices.contains(v1));
         System.out.println (
            " vtx "+v2.getIndex() + " inside=" + region.myVertices.contains(v2));
         int[] idxs = SurfaceMeshIntersectorTest.getFaceIndices(region.myFaces);
         ArraySort.sort (idxs);
         ArraySupport.print ("region faces: ", idxs);

         printFace (face);         
         printFaceXips (face);
         if (debugOpFaceIdx != -1) {
            System.out.println ("Face "+debugOpFaceIdx+":");
            Face opFace =
               (debugMesh0 ? myMesh0.getFace(debugOpFaceIdx) :
                myMesh1.getFace(debugOpFaceIdx));
            printFaceXips (opFace);
         }

      }

      HashSet<IntersectionPoint> visitedMips = new HashSet<IntersectionPoint>();

      ArrayList<Vertex3dList> outerPolys = null;

      do {
         EdgeInfo einfo = myEdgeInfos.get (he.getPrimary());
         if (debug) {
            System.out.println (
               "checking edge" + he.vertexStr() + " " + (he == he.getPrimary()));
         }
         if (einfo != null && einfo.numXips() > 0) {
            if (debug) {
               System.out.print ("mips:");
               for (int k=0; k<einfo.numXips(); k++) {
                  System.out.print (
                     " "+einfo.getXip(k).contourIndex+
                     "("+getContourIndex(einfo.getXip(k).contour)+")");
               }
               System.out.println ("");
            }
            for (int k=0; k<einfo.numXips(); k++) {
               IntersectionPoint mip = einfo.getXip(k, he, clockwise);
               if (region.myContours.contains (mip.contour) &&
                   !visitedMips.contains (mip) &&
                   //mip.contour.findSegmentFace (mip.contourIndex,mesh)==face) {
                   traceFaceBegin (mip, face, meshNum)) {
                  // entering face - follow contour to the end
                  if (debug) {
                     System.out.println ("entering face at "+mip.contourIndex);
                  }
                  Vertex3dList poly = 
                     traceFacePolygon (
                        mip, face, region, clockwise,
                        calc, visitedMips, vertexMap);
                  if (poly.size() > 2) {
                     if (outerPolys == null) {
                        outerPolys = new ArrayList<Vertex3dList>();
                     }
                     outerPolys.add (poly);
                  }
               }
               else if (debug) {
                  if (!region.myContours.contains (mip.contour)) {
                     System.out.println (
                        "region does not contain contour " +
                        getContourIndex(mip.contour));
                     System.out.print ("region contours:");
                     for (IntersectionContour c : region.myContours) {
                        System.out.print (" " + getContourIndex(c));
                     }
                     System.out.println ("");                        
                  }
                  if (visitedMips.contains (mip)) {
                     System.out.println ("mip was already visited");
                  }
                  System.out.println (
                     "trace face begin=" +
                     traceFaceBegin (mip, face, meshNum));
               }
            }
         }
         he = he.getNext();
      }
      while (he != he0);

      if (outerPolys == null && outerHoles == null) {
         // create a single polygon for the outer face
         Vertex3dList poly = createTrianglePoly (
            face, region, vertexMap, clockwise, debug);
         outerPolys = new ArrayList<Vertex3dList>();
         outerPolys.add (poly);
      }
      else if (outerHoles != null && outerHoles.size() > 0) {
         if (outerPolys == null) {
            outerPolys = new ArrayList<Vertex3dList>();
         }
         outerPolys.addAll (outerHoles);
      }

      if (outerPolys == null) {
         Vertex3dList poly = createTrianglePoly (
            face, region, vertexMap, clockwise, debug);
         outerPolys = new ArrayList<Vertex3dList>();
         outerPolys.add (poly);
      }

      if (innerHoles != null && outerPolys.size() > 1) {
         // multiple outer polys, so need to find out which hole belongs to
         // which
         HashMap<Vertex3dList,ArrayList<Vertex3dList>> polyHoleMap =
            new HashMap<Vertex3dList,ArrayList<Vertex3dList>>();
         ArrayList<Vertex3dList> containedHoles = null;

         for (Vertex3dList hole : innerHoles) {
            Polygon3dFeature nfeat = new Polygon3dFeature();
            // XXX ideally we want an interior point, in case the
            // outer point touches one of the contours
            Point3d ph = hole.get(0).pnt;
            double dmin = Double.POSITIVE_INFINITY;
            Vertex3dList nearestOuter = null;
            for (Vertex3dList outer : outerPolys) {
               calc.nearestFeature (nfeat, ph, outer, /*side=*/0);
               if (nfeat.getDistance() < dmin) {
                  dmin = nfeat.getDistance();
                  nearestOuter = outer;
               }
            }
            containedHoles = polyHoleMap.get(nearestOuter);
            if (containedHoles == null) {
               containedHoles = new ArrayList<Vertex3dList>();
               polyHoleMap.put (nearestOuter, containedHoles);
            }
            containedHoles.add (hole);
         }
         for (Vertex3dList outer : outerPolys) {
            containedHoles = polyHoleMap.get(outer);
            if (!calc.triangulate (triVtxs, outer, containedHoles)) {
               System.out.println (
                  "Can't triangulate: face " + face.getIndex() +
                  " mesh0=" + (face.getMesh() == myMesh0));
               triangulationError = true;
            }
         }
      }
      else {
         // no inner holes, or just one outer poly 
         for (Vertex3dList outer : outerPolys) {
            calc.debug = debug;
            if (debug) {
               System.out.println (
                  "outer size=" + outer.size());
               for (Vertex3dNode vn : outer) {
                  Vertex3d vtx = vn.getVertex();
                  System.out.println (
                     "  "+vtx.getIndex()+" "+vtx.pnt.toString("%12.8f"));
               }
            }
            if (outer.size() == 0) {
               writeErrorFile (/*csg=*/null);
               throw new InternalErrorException (
                  "outer polygon has size 0, face "+face.getIndex());
            }
            if (!calc.triangulate (triVtxs, outer, innerHoles)) {
               System.out.println (
                  "Can't triangulate: face " + face.getIndex() +
                  " mesh0=" + (face.getMesh() == myMesh0));
               triangulationError = true;

               System.out.println ("outer poly");
               for (Vertex3dNode n : outer) {
                  System.out.println (n.getVertex().pnt);
               }
               for (IntersectionContour c : region.myContours) {
                  System.out.println ("contour " + getContourIndex(c));
                  for (IntersectionPoint p : c) {
                     System.out.println (toString (p));
                  }
               }
               if (innerHoles != null) {
                  for (int hi=0; hi<innerHoles.size(); hi++) {
                     System.out.println ("hole " + hi);
                     for (Vertex3dNode n : innerHoles.get(hi)) {
                        System.out.println (n.getVertex().pnt);
                     }
                  }
               }
            }
         }
      }

      if (debug) {
         System.out.println (
            "face: ADDED " + (triVtxs.size()-oldSize)/3 + 
            " at " +  (oldSize)/3 + " numOuter=" + outerPolys.size());
      }
   }

}
