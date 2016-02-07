package maspack.collision;
 
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import maspack.geometry.Face;
import maspack.geometry.HalfEdge;
import maspack.geometry.Vertex3d;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.render.Renderer;
import maspack.render.Renderer.VertexDrawMode;

public class MeshIntersectionContour extends ArrayList<MeshIntersectionPoint> {
   private static final long serialVersionUID = 1L;
   //private static int maxContourPoints = 500;

   public MeshIntersectionPoint workPoint;
   public boolean isClosed = false;
   public boolean isContinuable = true;
   public boolean hasInterior;
   public boolean openMesh = false;
   boolean isInverting = false;
   public boolean isNested = false;
   public Vector3d normal;
   public boolean edgeRegion;
   public SurfaceMeshIntersector intersector;

   /*
    * List of edges from each region that are adjacent to an inside face.
    */
   public LinkedHashSet<HalfEdge> insideEdges0, insideEdges1;

   /*
    * List of faces from each region that are adjacent to an inside vertex.
    */
   public LinkedHashSet<Face> insideFaces0, insideFaces1;

   /*
    * List of vertices from each region that have penetrated into the other
    * region.
    */
   public ArrayList<Vertex3d> insideVertices0, insideVertices1;

   public MeshIntersectionContour (SurfaceMeshIntersector anIntersector) {
      insideEdges0 = new LinkedHashSet<HalfEdge>();
      insideEdges1 = new LinkedHashSet<HalfEdge>();
      insideFaces0 = new LinkedHashSet<Face>();
      insideFaces1 = new LinkedHashSet<Face>();
      insideVertices0 = new ArrayList<Vertex3d>();
      insideVertices1 = new ArrayList<Vertex3d>();
      intersector = anIntersector;
      workPoint = new MeshIntersectionPoint();
   }

   public boolean addWorkPoint() {
      if (add (workPoint)) {
         workPoint = new MeshIntersectionPoint();
         return true;
      }
      return false;
   }

//    public double getMaxContourPoints() {
//       return maxContourPoints;
//    }

//    public void setMaxContourPoints (int anInt) {
//       maxContourPoints = anInt;
//    }

   /*
    * Add a new intersection point to this contour.
    * 
    * Return true if mip is a new intersection and the add is successful, or if
    * this contour already contains this intersection and is thereby closed. If
    * the latter, and the intersection was not this contour's first, remove the
    * initial mips up to but not including the first occurrence of this
    * intersection and make them into a separate non-closed contour.
    * 
    * Answer false on failure. Fail if the edge-face intersection has already
    * been found in another contour.
    */
   public boolean add (MeshIntersectionPoint mip) {
      MeshIntersectionContour otherContour = null;
      HashMap<Face,MeshIntersectionContour> edgeIntersections =
         intersector.edgeFaceIntersections.get (mip.edge);
      if (edgeIntersections != null)
         otherContour = edgeIntersections.get (mip.face);
      if (otherContour == null) { // new edge-face combination
         if (edgeIntersections == null) {
            edgeIntersections = new HashMap<Face,MeshIntersectionContour>();
            intersector.edgeFaceIntersections.put (mip.edge, edgeIntersections);
         }
         edgeIntersections.put (mip.face, this);
//          if (size() >= maxContourPoints) {
//             maxContourPoints *= 2;
//             super.ensureCapacity( maxContourPoints );
//          }
         mip.contour = this;
         super.add (mip);
         /*
          * When tracing along a contour, the contour remembers the current
          * edgeRegion. The edge region of each new point is set to the current
          * value as it is added to the contour. The tracing code calls
          * switchEdgeRegion when it detects an edge region switch.
          */
         mip.edgeRegion = edgeRegion;
         /*
          * Give the edge a chance to remember the closest intersecting face to
          * each vertex.
          */
         intersector.setEdgeIntersectionPoints (mip);
         return true;
      }
      if (otherContour == this) {
         if (!mip.matches (get (0))) {
            intersector.setDegenerate();
            return false;
         }
         isClosed = true;
         isContinuable = false;
         return true;
      }
      if (size() > 0)
         intersector.setDegenerate();
      return false;
   }

   public void switchEdgeRegion() {
      edgeRegion = !edgeRegion;
   }

   public void reverse() {
      int k = size();
      int k2 = k / 2;
      MeshIntersectionPoint tmp;
      for (int i = 0; i < k2; i++) {
         tmp = get (i);
         set (i, get (--k));
         set (k, tmp);
      }
   }

   /* Allow index wraparound for finding convex hull */
   public MeshIntersectionPoint get (int i) {
      int sz = size();
      if (i < 0)
         return super.get (sz + i); // aho modified
      if (i >= sz)
         return super.get (i - sz);
      return super.get (i);
   }

   void addInsideVertex(ArrayList<Vertex3d> insideVertices, Vertex3d vertex, LinkedHashMap<Vertex3d, MeshIntersectionContour> vertexContours) {
      MeshIntersectionContour otherContour = vertexContours.get (vertex);
      if (otherContour != null) {
         if (otherContour != this) {
            intersector.nestedContours.add (new MeshContourPair (
               this, otherContour));
            isNested = true;
            otherContour.isNested = true;
         }
      }
      else {
         vertexContours.put (vertex, this);
         insideVertices.add (vertex);
      }
   }
   
   /*
    * We are iterating over all the intersection points that form the contour,
    * calling this method once for every opposing face intersected by this edge.
    * 
    * The edge remembers the intersection point nearest to either vertex, which
    * gives us the nearest opposing face.
    * 
    * If either vertex is inside its nearest opposing face, add it as a boundary
    * vertex.
    */
   public void findInsideBoundaryFeatures (HashSet<HalfEdge> boundaryEdges, LinkedHashMap<Vertex3d, MeshIntersectionContour> vertexContours) {
      if (!isClosed) {
         intersector.setDegenerate();
         return;
      }

      MeshIntersectionPoint firstMip = get (0);
      if (firstMip.edge.head.getMesh() ==
          firstMip.face.getEdge (0).head.getMesh()) {
         throw new RuntimeException ("self-intersecting mesh");
      }

      for (MeshIntersectionPoint mip : this) {
         HalfEdge edge = mip.edge;
         boundaryEdges.add (edge);
         ArrayList<Vertex3d> insideVertices;
         if (mip.edgeRegion) {
            insideFaces0.add (edge.getFace());
            insideFaces0.add (edge.opposite.getFace());
            insideFaces1.add (mip.face);
            insideEdges0.add (edge);
            insideVertices = insideVertices0;
         }
         else {
            insideFaces1.add (edge.getFace());
            insideFaces1.add (edge.opposite.getFace());
            insideFaces0.add (mip.face);
            insideEdges1.add (edge);
            insideVertices = insideVertices1;
         }
         ArrayList<MeshIntersectionPoint> mips =
            intersector.edgeMips.get (edge);
         
         while (!mips.isEmpty() && mips.get (0).isCoincident)
            mips.remove (0);
         while (!mips.isEmpty() && mips.get (mips.size() - 1).isCoincident)
            mips.remove (mips.size() - 1);
         
         if (!mips.isEmpty()) {
            boolean v0Inside =
               mip == mips.get (0) & mip.isVertexInsideFace (edge.tail);
            boolean v1Inside =
               mip == mips.get (mips.size() - 1)
               & mip.isVertexInsideFace (edge.head);
            if (v0Inside) {
               if (v1Inside)
                  intersector.setDegenerate();
               addInsideVertex(insideVertices, edge.tail, vertexContours);
            }
            else {
               if (v1Inside) {
                  addInsideVertex(insideVertices, edge.head, vertexContours);
               }
            }
         }
      }
   }

   /*
    * Handle nested and intersecting contours by allowing a vertex to be a
    * penetrating inside vertex for a maximum of one contour.
    */
   public void findRegionInteriors (HashSet<HalfEdge> boundaryEdges, LinkedHashMap<Vertex3d, MeshIntersectionContour> vertexContours) {
      int nVerts = insideVertices0.size();
      for (int i=0; i<nVerts; i++) {
            traceInsideVertex(insideVertices0.get(i), insideVertices0, insideFaces0, insideEdges0, boundaryEdges, vertexContours);
      }
      nVerts = insideVertices1.size();
      for (int i=0; i<nVerts; i++) {
            traceInsideVertex(insideVertices1.get(i), insideVertices1, insideFaces1, insideEdges1, boundaryEdges, vertexContours);
      }
   }

   /* Every edge that crosses a contour has already been added to boundaryEdges.
    * Given aVertex which is inside the region, walk through the mesh finding all adjacent vertices
    * which can be reached without traversing a boundary edge from any contour.
    * Having one set of boundary edges global to the scenario ensures we don't trace across an
    * inner or outer nested contour into a non-penetrating region.
    * In self-intersecting cases it is possible for two contours to cross, resulting in two overlapping
    * interpenetrating volumes.   The vertexContours dictionary restricts a vertex to 
    * to be considered penetrating for only one contour, and allows recognition of nested contours.
    */
   void traceInsideVertex (
      Vertex3d aVertex, ArrayList<Vertex3d> insideVertices,
      HashSet<Face> insideFaces, HashSet<HalfEdge> insideEdges,
      HashSet<HalfEdge> boundaryEdges, LinkedHashMap<Vertex3d, MeshIntersectionContour> vertexContours) {

      Vertex3d v;
      Iterator<HalfEdge> itEdges = aVertex.getIncidentHalfEdges();
      while (itEdges.hasNext()) {
         HalfEdge edge = itEdges.next().getPrimary();
         if (boundaryEdges.add (edge)) {
            insideEdges.add (edge);
            insideFaces.add (edge.getFace());
            insideFaces.add (edge.opposite.getFace());
            v = edge.head;
            if (v == aVertex)
               v = edge.tail;
            MeshIntersectionContour otherContour = vertexContours.get(v); 
            if (otherContour != null) {
               if (otherContour != this) {
            	  intersector.nestedContours.add(new MeshContourPair(otherContour, this));
            	  isNested = true;
            	  otherContour.isNested = true;
               }
            } else {
            	vertexContours.put(v, this);
            	insideVertices.add (v);
            	traceInsideVertex(v, insideVertices, insideFaces, insideEdges, boundaryEdges, vertexContours);
            }
         }
      }
   }

   void render (Renderer renderer, int flags) {
      
      renderer.setLineWidth (44);
      //gl.glDisable (GL2.GL_LINE_STIPPLE);
      if (isClosed) {
         renderer.setColor (0f, 0f, 1f);
      }
      else {
         renderer.setColor (1f, 0f, 1f);
      }

      renderer.beginDraw (VertexDrawMode.LINE_LOOP);

      for (MeshIntersectionPoint p : this) {
         renderer.addVertex (p);
      }
      renderer.endDraw();
      // System.out.println("contour end"+" err="+gl.glGetError());
      renderer.setLineWidth (1);

      renderer.setPointSize (1);
   }

   public double getArea() {
	      /* Compute the centroid of all the points. */
	int pSize = size();
	if (pSize < 3) return(0);
	Point3d centroid = new Point3d();
	for (MeshIntersectionPoint mip : this) centroid.add (mip);
	centroid.scale (1.0d / size());
	Vector3d cp = new Vector3d();
	Vector3d normalSum = new Vector3d();
	Vector3d rLast = new Vector3d();
	rLast.sub (get(pSize - 1), centroid);
	Vector3d r = new Vector3d();
	for (int i = 0; i < pSize; i++) {
		r.sub (get(i), centroid);
		cp.cross (r, rLast);
		normalSum.add(cp);
		Vector3d rTemp = rLast;
		rLast = r;
		r = rTemp;
	}
	return normalSum.norm() * 0.5;
   }
}
