package maspack.collision;
 
import java.util.ArrayList;

import maspack.util.*;
import maspack.geometry.Face;
import maspack.geometry.HalfEdge;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.LineSegment;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.matrix.RigidTransform3d;
import maspack.render.Renderer;
import maspack.render.Renderer.DrawMode;

public class IntersectionContour extends ArrayList<IntersectionPoint> {
   private static final long serialVersionUID = 1L;

   private static final double DOUBLE_PREC = 1e-16;

   public boolean isClosed = false;
   public boolean isContinuable = true;
   // public boolean openMesh = false;
   public Face containingFace = null; // computed by findPenetrationRegion()
   public double singleFaceArea = 0;  // computed by findPenetrationRegion()
   boolean emptySegmentsMarked = false;

   public IntersectionContour () {
   }

   /**
    * Query if this contour is closed.
    * 
    * @return <code>true</code> if the contour is closed.
    */
   public boolean isClosed() {
      return isClosed;
   }
   
   void setClosed (boolean closed) {
      isClosed = closed;
   }

   /**
    * Returns <code>true</code> if this contour divides the specified mesh
    * into "inside" and "outside" regions. This will be true if
    *
    * <ul>
    * <li>the contour is closed;
    * <li>both end points of the contour lie on boundary edges of the mesh
    * </ul>
    *
    * @return <code>true</code> if this contour divides the mesh
    */
   public boolean dividesMesh (PolygonalMesh mesh) {
      if (isClosed()) {
         return true;
      }
      else {
         HalfEdge he0 = getFirst().edge;
         HalfEdge heL = getLast().edge;
         return (he0.opposite == null && edgeOnMesh(he0, mesh) &&
                 heL.opposite == null && edgeOnMesh(heL, mesh));
      }
   }
   
   public void reverse() {
      // int k = size();
      // int k2 = k / 2;
      // MeshIntersectionPoint tmp;
      // for (int i = 0; i < k2; i++) {
      //    tmp = get (i);
      //    set (i, get (--k));
      //    set (k, tmp);
      // }
      int ilast = size()-1;
      for (int ia=0; ia<size()/2; ia++) {
         int ib = ilast-ia;
         IntersectionPoint pa = get(ia);
         IntersectionPoint pb = get(ib);
         pb.contourIndex = ia;
         set (ia, pb);
         pa.contourIndex = ib;
         set (ib, pa);
      }
   }

   /**
    * Get with wrapping implemented for closed contours. If the contour
    * is open and the index is out of bounds, null is returned.
    * 
    * @param idx index of point to get
    * @return point at index idx
    */
   public IntersectionPoint getWrapped (int idx) {
      if (idx < 0) {
         if (isClosed) {
            idx = idx%size();
            if (idx != 0) {
               idx += size();
            }
         }
         else {
            return null;
         }
      }
      else if (idx >= size()) {
         if (isClosed) {
            idx = idx%size();
         }
         else {
            return null;
         }
      }
      return super.get(idx);
   }

   /**
    * Takes an arbitrary index value for a contour point returns either its
    * wrapped value (for closed contours), or checks its range and returns
    * either the index itself or -1 if it is out of range (for open
    * contours).
    * 
    * @param idx index value to check
    * @return wrapped value, or -1 is the contour is open and <code>idx</code>
    * is out of range
    */
   public int getWrappedIndex (int idx) {
      if (idx < 0) {
         if (isClosed) {
            idx = idx%size();
            if (idx != 0) {
               idx += size();
            }
         }
         else {
            idx = -1;
         }
      }
      else if (idx >= size()) {
         if (isClosed) {
            idx = idx%size();
         }
         else {
            idx = -1;
         }
      }
      return idx;
   }

   void render (Renderer renderer, int flags) {
      
      renderer.setLineWidth (44);
      //gl.glDisable (GL2.GL_LINE_STIPPLE);
      if (isClosed) {
         renderer.setColor (0f, 0f, 1f);
         renderer.beginDraw (DrawMode.LINE_LOOP);
      }
      else {
         renderer.setColor (1f, 0f, 1f);
         renderer.beginDraw (DrawMode.LINE_STRIP);
      }
      for (IntersectionPoint p : this) {
         renderer.addVertex (p);
      }
      renderer.endDraw();
      renderer.setLineWidth (1);
      renderer.setPointSize (1);
   }

   /**
    * Computes the area of this contour with respect to a plane formed
    * by computing the centroid and then summing the cross products of
    * rays from the centroid to adjacent contour points.
    * 
    * @return planar area
    */
   public double computePlanarArea() {
      Vector3d areaVec = new Vector3d();
      double pointTol = computeLength()*1e-10; // reject closer points
      fitPlane (areaVec, /*centroid=*/null, pointTol);
      return 0.5*areaVec.norm();
   }
   
   /**
    * Computes the centroid of this contour, rejecting points that are
    * very close together.
    * 
    * @param centroid returns the centroid
    */
   public void computeCentroid (Vector3d centroid) {
      centroid.setZero();
      if (size() > 0) {
         double pointTol = computeLength()*1e-10;
         Point3d plast = get(0);
         centroid.set (plast);
         int nump = 1;
         boolean closeToFirst = false;
         for (int i=1; i<size(); i++) {
            Point3d p = get(i);
            if (isClosed() && i==size()-1) {
               // in case contour is closed, check closeness to first point too
               closeToFirst = (p.distance(get(0)) < pointTol);
            }
            if (p.distance(plast) >= pointTol && !closeToFirst) {
               centroid.add (p);
               plast = p;
               nump++;
            }
         }
         centroid.scale (1.0/nump);      
      }
   }
   
   /**
    * Computes the length of this contour
    * 
    * @return contour length
    */
   public double computeLength() {
      double length = 0;
      if (size() >= 2) {
         Point3d pa = get(0);
         for (int i=1; i<size(); i++) {
            Point3d pb = get(i);
            length += pa.distance(pb);
            pa = pb;
         }
         if (isClosed) {
            length += pa.distance(get(0));
         }
      }
      return length;      
   }

   static boolean edgeOnMesh (HalfEdge edge, PolygonalMesh mesh) {
      return edge.getHead().getMesh() == mesh;
   }

   static Face getOppositeFace (HalfEdge he) {
      if (he.opposite != null) {
         return he.opposite.getFace();
      }
      else {
         return null;
      }
   }

   /**
    * Computes whether or not this contour is oriented clockwise
    * with respect to the penetrating region for mesh0.
    *
    * @return true of contour is clockwise with respect to mesh0.
    */
   boolean isClockwise (PolygonalMesh mesh0, PolygonalMesh mesh1) {

      int csize = size();

      // find the largest contour segment
      Vector3d dir = new Vector3d();
      Vector3d xprod = new Vector3d();

      double dmax = 0;
      boolean clockwise = true;
      int nsegs = isClosed ? csize : csize-1;
      for (int i=0; i<nsegs; i++) {

         IntersectionPoint pa = get((i)%csize);
         IntersectionPoint pb = get((i+1)%csize);

         Face face0 = findSegmentFace (pa, pb, mesh0);     
         Face face1 = findSegmentFace (pa, pb, mesh1);

         dir.sub (pb, pa);
         xprod.cross (face1.getWorldNormal(), dir);
         double dot = face0.getWorldNormal().dot(xprod);
         if (Math.abs(dot) > dmax) {
            dmax = Math.abs(dot);
            clockwise = (dot < 0);
         }
      }
      return clockwise;
   }

   /**
    * Finds the face on the specified mesh that is traversed by the contour
    * between the points at indices <code>(idx, idx+1)</code>. If
    * <code>idx</code> equals <code>size()-1</code> (i.e., the last point),
    * then the method returns the face between the indices <code>(size()-1,
    * 0)</code> if the contour is closes, and <code>null</code> otherwise.
    */
   Face findSegmentFace (int idx, PolygonalMesh mesh) {
      IntersectionPoint p0, p1;
      if (isClosed() || idx >= 0 && idx < size()-1) {
         p0 = getWrapped(idx);
         p1 = getWrapped(idx+1);
         return findSegmentFace (p0, p1, mesh);
      }
      else {
         return null;
      }
   }

   Face findSegmentFace (IntersectionPoint mip, PolygonalMesh mesh) {
      IntersectionPoint next = mip.next();
      if (next == null) {
         return null;
      }
      else {
         return findSegmentFace (mip, next, mesh);
      }
   }
   
//   Face getSegmentFace (int idx, PolygonalMesh mesh) {
//      IntersectionPoint p;
//      if (isClosed()) {
//         p = getWrapped(idx);
//      }
//      else {
//         p = get(idx);
//      }
//      return p.getSegmentFace (mesh);
//   }
//
   /**
    * Finds the face on the specified mesh that is traversed by the contour
    * between the adjacent points <code>pa</code> and <code>pb</code>.
    */
   Face findSegmentFace (
      IntersectionPoint pa, IntersectionPoint pb, PolygonalMesh mesh) {
      if (edgeOnMesh (pa.edge, mesh)) {
         if (edgeOnMesh (pb.edge, mesh)) {
            if (pa.edge.getFace() == pb.edge.getFace()) {
               return pa.edge.getFace();
            }
            else if (pa.edge.getFace() == getOppositeFace (pb.edge)) {
               return pa.edge.getFace();
            }
            else if (getOppositeFace (pa.edge) ==  pb.edge.getFace()) {
               return pb.edge.getFace();
            }
            else if (getOppositeFace (pa.edge) == getOppositeFace (pb.edge)) {
               return getOppositeFace (pa.edge);
            }
            else {
               System.out.println ("NO COMMON Face edge/edge");
            }
         }
         else {
            if (pb.face == pa.edge.getFace() || 
                pb.face == getOppositeFace (pa.edge)) {
               return pb.face;
            }
            else {
               System.out.println ("NO COMMON Face edge/face");
            }
         }
      }
      else {
         if (edgeOnMesh (pb.edge, mesh)) {
            if (pa.face == pb.edge.getFace() || 
                pa.face == getOppositeFace (pb.edge)) {
               return pa.face;
            }
            else {
               System.out.println ("NO COMMON Face face/edge");
            }
         }
         else {
            if (pa.face == pb.face) {
               return pa.face;
            }
            else {
               System.out.println ("NO COMMON Face face/face");               
            }
         }
      }
      return null;
   }

   /**
    * Computes the area of this contour projected onto a plane defined by a
    * normal vector.  p0 provides a base point for area calculations.
    */
   public double computePlanarArea (
      Vector3d nrm, Point3d p0, boolean clockwiseContour) {

      double area = 0;
      switch (nrm.maxAbsIndex()) {
         case 0: {
            area = computePlanarAreaYZ(p0)/nrm.x; 
            break;
         }
         case 1: {
            area = computePlanarAreaZX(p0)/nrm.y;
            break;
         }
         case 2: {
            area = computePlanarAreaXY(p0)/nrm.z;
            break;
         }
         default: {
            throw new InternalErrorException (
               "Vector3d.maxAbsIndex() returned "+nrm.maxAbsIndex());
         }
      }
      if (clockwiseContour) {
         area = -area;
      }
      return area;
   }
   
   private double computePlanarAreaYZ (Point3d p0) {

      double area = 0;
      IntersectionPoint p = get(size()-1);
      double del1y = p.y-p0.y;
      double del1z = p.z-p0.z;
      for (int k=0; k<size(); k++) {
         p = get(k);
         double del2y = p.y-p0.y;
         double del2z = p.z-p0.z;
         area += (del1y*del2z - del1z*del2y);
         del1y = del2y;
         del1z = del2z;
      }
      return area/2;
   }  

   private double computePlanarAreaZX (Point3d p0) {

      double area = 0;
      IntersectionPoint p = get(size()-1);
      double del1z = p.z-p0.z;
      double del1x = p.x-p0.x;
      for (int k=0; k<size(); k++) {
         p = get(k);
         double del2z = p.z-p0.z;
         double del2x = p.x-p0.x;
         area += (del1z*del2x - del1x*del2z);
         del1z = del2z;
         del1x = del2x;
      }
      return area/2;
   }  

   private double computePlanarAreaXY (Point3d p0) {

      double area = 0;
      IntersectionPoint p = get(size()-1);
      double del1x = p.x-p0.x;
      double del1y = p.y-p0.y;
      for (int k=0; k<size(); k++) {
         p = get(k);
         double del2x = p.x-p0.x;
         double del2y = p.y-p0.y;
         area += (del1x*del2y - del1y*del2x);
         del1x = del2x;
         del1y = del2y;
      }
      return area/2;
   }

   public ArrayList<IntersectionPoint> getCornerPoints() {
      return getCornerPoints (computeLength()*100000*DOUBLE_PREC);
   }

   /**
    * Return the subset of points that form the corners of this contour.
    */
   public ArrayList<IntersectionPoint> getCornerPoints(double tol) {
      ArrayList<IntersectionPoint> pnts0 =
         new ArrayList<IntersectionPoint>();

      // first find points that aren't too close together. Need
      // to handle this slightly differently for open and closed contours
      IntersectionPoint pprev;
      int kstart;
      if (isClosed) {
         pprev = get(size()-1);
         kstart = 0;
      }
      else {
         pprev = get(0);
         pnts0.add (pprev);
         kstart = 1;
      }
      for (int k=kstart; k<size(); k++) {
         IntersectionPoint p = get(k);
         if (p.distance(pprev) > tol) {
            pnts0.add (p);
            pprev = p;
         }
      }      
      // if less than three points, just return the points
      if (pnts0.size() < 3) {
         return pnts0;
      }
      else {
         // now find those points that actually form corners. Again,
         // slightly different for open and closed contours
         ArrayList<IntersectionPoint> pnts1 =
            new ArrayList<IntersectionPoint>();

         if (isClosed) {
            pprev = pnts0.get(pnts0.size()-1);
            for (int k=0; k<pnts0.size(); k++) {
               IntersectionPoint p = pnts0.get(k);
               Point3d pnext = pnts0.get((k+1)%pnts0.size());
               // see if p is a corner point
               if (LineSegment.distance (pprev, pnext, p) > tol) {
                  pnts1.add (pnts0.get(k));
               }
               pprev = p;
            }
         }
         else {
            pprev = pnts0.get(0);
            pnts1.add (pprev);
            for (int k=1; k<pnts0.size()-1; k++) {
               IntersectionPoint p = pnts0.get(k);
               Point3d pnext = pnts0.get(k+1);
               // see if p is a corner point
               if (LineSegment.distance (pprev, pnext, p) > tol) {
                  pnts1.add (pnts0.get(k));
               }
               pprev = p;
            }
            pnts1.add (pnts0.get(pnts0.size()-1));
         }
         return pnts1;
      }
   }

   public void printCornerPoints (
      String name, String fmt, RigidTransform3d T) {

      if (name != null) {
         System.out.println (name);
      }
      double tol = computeLength()*100*DOUBLE_PREC;
      ArrayList<? extends Point3d> corners = getCornerPoints(tol);
      Point3d p = new Point3d();
      for (int i=0; i<corners.size(); i++) {
         p.set (corners.get(i));
         if (T != null) {
            p.transform (T);
         }
         System.out.println (p.toString(fmt));
      }      
   }

   /**
    * Fits this contour to a plane by first computing the centroid,
    * and then forming a normal by summing the cross products of
    * adjacent rays between the centroid and the contour points.
    * This sum is returned in the optional argument <code>areaVec</code>. 
    * 
    * <p>When computing the centroid, points closer to each other
    * than <code>pointTol</code> are ignored. The points that are
    * actually used are collected into a separate list that is
    * returned by this method.
    * 
    * @param areaVec if non-null, returns the computed area vector.
    * Normalizing this vector gives the normal for the plane, while
    * the length of this vector gives twice the area of the contour 
    * with respect to the plane.
    * @param centroid if non-null, returns the computed centroid.
    * @param pointTol minimum distance between contour points used for
    * computing the plane
    * @return list of contours points with those closer than 
    * <code>pointTol</code> removed.
    */
   public ArrayList<IntersectionPoint> fitPlane (
      Vector3d areaVec, Vector3d centroid, double pointTol) {

      ArrayList<IntersectionPoint> points =
         new ArrayList<IntersectionPoint>(size());

      if (centroid == null) {
         centroid = new Vector3d();
      }
      else {
         centroid.setZero();
      }
      if (areaVec == null) {
         areaVec = new Vector3d();
      }
      else {
         areaVec.setZero();
      }
      
      IntersectionPoint plast = get(0);
      centroid.set (plast);
      points.add (plast);
      boolean closeToFirst = false;
      for (int i=1; i<size(); i++) {
         IntersectionPoint p = get(i);
         if (isClosed() && i==size()-1) {
            // in case contour is closed, check closeness to first point too
            closeToFirst = (p.distance(get(0)) < pointTol);
         }
         if (p.distance(plast) >= pointTol && !closeToFirst) {
            centroid.add (p);
            points.add (p);
            plast = p;
         }
      }
      int nump = points.size();
      centroid.scale (1.0/nump);
      if (nump >= 3) {
         Vector3d ray0 = new Vector3d();
         Vector3d ray1 = new Vector3d();
         ray0.sub (points.get(nump-1), centroid);
         for (Point3d p : points) {
            ray1.sub (p, centroid);
            areaVec.crossAdd (ray0, ray1, areaVec);
            ray0.set (ray1);
         }
      }
      return points;
   }

   /**
    * Computes the area of this contour with respect to a single
    * face. It is assumed that the contour is entirely parallel
    * to the face. If not, the area is the projection of the contour
    * onto the face's plane.
    */
   double computeFaceArea (Face face, boolean clockwiseContour) {

      Point3d p0 = new Point3d();
      face.firstHalfEdge().getHead().getWorldPoint (p0);

      return computePlanarArea (face.getWorldNormal(), p0, clockwiseContour);
   }

   /**
    * Returns the first point of this contour, or null if the contour is empty.
    *
    * @return first point of this contour, or <code>null</code>
    */
   public IntersectionPoint getFirst() {
      if (size() > 0) {
         return get(0);
      }
      else {
         return null;
      }
   }
   
   /**
    * Returns the next intersection point after <code>p</code> on this
    * contour. If the contour is open and <code>p</code> is the last
    * point, returns <code>null</code>.
    */
   public IntersectionPoint getNext (IntersectionPoint p) {
      return getWrapped (p.contourIndex+1);
   }

   /**
    * Returns the previous intersection point after <code>p</code> on this
    * contour. If the contour is open and <code>p</code> is the first
    * point, returns <code>null</code>.
    */
   public IntersectionPoint getPrev (IntersectionPoint p) {
      return getWrapped (p.contourIndex-1);
   }

   /**
    * Returns the last point of this contour, or null if the contour is empty.
    *
    * @return last point of this contour, or <code>null</code>
    */
   public IntersectionPoint getLast() {
      if (size() > 0) {
         return get(size()-1);
      }
      else {
         return null;
      }
   }

   /**
    * Returns an index at which the indicated face first appears as a segment
    * face for the indicated mesh, or -1 if no such starting index is found,
    * which means either that the segment face does not appear at all, or
    * appears along the whole contour.
    */
   public int findSegmentFaceStart (Face face, PolygonalMesh mesh) {
      Face lastFace = findSegmentFace (size()-1, mesh);
      for (int k=0; k<size(); k++) {
         Face segFace = findSegmentFace (k, mesh);
         if (segFace == face && lastFace != face) {
            return k;
         }
         lastFace = segFace;
      }
      return -1;
   }

//   void setSegmentFaces (PolygonalMesh mesh0, PolygonalMesh mesh1) {
//      for (int i=0; i<size(); i++) {
//         IntersectionPoint p = get(i);
//         p.myMesh0 = mesh0;
//         p.myFace0 = findSegmentFace (i, mesh0);
//         p.myFace1 = findSegmentFace (i, mesh1);
//      }
//   }

   /**
    * Returns the index of the point where the contour first enters the
    * specified <code>face</code> on <code>mesh</code>. If the contour does not
    * enter the face, or if it is entirely located on the face, the method
    * returns <code>-1</code>.
    *
    * <p>If <code>face</code> is <code>null</code>, then the methods returns
    * the first point where the contour first enters <i>any</i> face.  For open
    * contours, this is 0. If the contour is closed and entirely associated
    * with one face, there is no entry index and the method returns -1.
    */
   int getFirstFaceEntryIndex (PolygonalMesh mesh, Face face) {
      Face lastFace = findSegmentFace (size()-1, mesh);
      for (int k=0; k<size(); k++) {
         Face segFace = findSegmentFace (k, mesh);
         if (segFace == null) {
            return -1; // occurs only we are at the end of an open contour
         }
         if (segFace != lastFace) {
            if (face == null || segFace == face) {
               return k;
            }
         }
         lastFace = segFace;
      }
      return -1;
   }

   /**
    * Returns the point where the contour first enters the specified
    * <code>face</code> on <code>mesh</code>. If the contour does not enter the
    * face, or if it is entirely located on the face, the method returns
    * <code>-1</code>.
    *
    * <p>If <code>face</code> is <code>null</code>, then the methods returns
    * the first point where the contour first enters <i>any</i> face.  For open
    * contours, this is always the first point. If the contour is closed and
    * entirely associated with one face, there is no entry index and the method
    * returns <code>null</code>.
    */
   public IntersectionPoint firstFaceEntryPoint (
      PolygonalMesh mesh, Face face) {
      int idx = getFirstFaceEntryIndex (mesh, face);
      return idx != -1 ? get(idx) : null;
   }

   /**
    * Returns a point on the contour whose distance from the preceeding point
    * is either <code>null</code> (for open contours), or whose distance from
    * the preceeding point exceeds <code>tol</code>. On closed contours, the
    * latter is found by searching in reverse from the first point. If no such
    * point is found (i.e., all points have a distance {@code <=} <code>tol</code>
    * between them), <code>null</code> is returned.
    */
   public IntersectionPoint firstNonCoincidentPoint (double tol) {
      if (isClosed()) {
         IntersectionPoint mip0 = get(0);
         IntersectionPoint mip = mip0;
         do {
            IntersectionPoint prev = mip.prev();            
            if (prev.distance (mip) > tol) {
               return mip;
            }
            mip = prev;
         }
         while (mip != mip0);
         return null;
      }
      else {
         return get(0);
      }
   }

   public IntersectionContour copy() {
      IntersectionContour contour = new IntersectionContour();

      contour.isClosed = isClosed;
      contour.isContinuable = isContinuable;
//      contour.openMesh = openMesh;
      contour.containingFace = containingFace;
      contour.singleFaceArea = singleFaceArea;

      for (int i=0; i<size(); i++) {
         IntersectionPoint pcopy = get(i).clone();
         contour.add (pcopy);
         pcopy.contour = contour;
         pcopy.contourIndex = i;
      }
      return contour;
   }

   HalfEdge edgeFromIndex (int idx, PolygonalMesh mesh) {
      Face face = mesh.getFace(idx/3);
      return face.getEdge (idx%3);
   }

   public void getState (
      DataBuffer data, PolygonalMesh mesh0, PolygonalMesh mesh1) {
      
      data.zputBool (isClosed);
      data.zputBool (isContinuable);
      data.zput (size());
      for (int i=0; i<size(); i++) {
         get(i).getState (data, mesh0, mesh1);
      }
   }

   public void setState (
      DataBuffer data, PolygonalMesh mesh0, PolygonalMesh mesh1) {

      isClosed = data.zgetBool();
      isContinuable = data.zgetBool();
      clear();
      int size = data.zget();
      for (int i=0; i<size; i++) {
         IntersectionPoint mip = new IntersectionPoint();
         mip.contour = this;
         mip.contourIndex = i;
         add (mip);
      }
      for (int i=0; i<size; i++) {
         get(i).setState (data, mesh0, mesh1);
      }
   }

   public boolean equals (IntersectionContour contour, StringBuilder msg) {
      if (size() != contour.size()) {
         if (msg != null) msg.append ("contour size differs\n");
         return false;
      }
      for (int i=0; i<size(); i++) {
         if (!get(i).equals (contour.get(i), msg)) {
            if (msg != null) msg.append ("contour point "+i+" differs\n");
            return false;
         }
      }
      return true;
   }

}
