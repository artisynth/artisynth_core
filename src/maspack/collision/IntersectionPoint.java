package maspack.collision;

import maspack.matrix.Point3d;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.Face;
import maspack.geometry.HalfEdge;
import maspack.geometry.Vertex3d;
import maspack.geometry.RobustPreds;
import maspack.util.DataBuffer;

public class IntersectionPoint extends Point3d {

   private static final long serialVersionUID = 1L;

   private static final int FACE_ON_MESH1 = 0x40000000;

   static final IntersectionPoint COINCIDENT = new IntersectionPoint();

   public IntersectionContour contour;
   int contourIndex = -1; // index giving the position along the contour
   public Face face;
   public HalfEdge edge;
   IntersectionPoint primaryCoincident;

   public int intersectionCode; // code for intersection that created this point

   static final int EMPTY_PROJECTED = 0x01;
   static final int EMPTY_BEGIN = 0x02;
   static final int EMPTY_END = 0x04;

   static final int EMPTY_SEG_BEGIN = (EMPTY_PROJECTED | EMPTY_BEGIN);
   static final int EMPTY_SEG_END = (EMPTY_PROJECTED | EMPTY_END);

   // Following attributes are transient and used when creating CSG methods:
   
   Vertex3d myVertex; // vertex associated with this mip
   Face effectiveFace0;
   Face effectiveFace1;
   HalfEdge nearEdge0;
   HalfEdge nearEdge1;
   int emptyMark0;
   int emptyMark1;

   static final int TRIANGLE_EDGE_MASK = 
   (RobustPreds.E01_ON_SEGMENT | 
    RobustPreds.E12_ON_SEGMENT | 
    RobustPreds.E20_ON_SEGMENT); 

   static final int SEGMENT_MASK = 
   (RobustPreds.TAIL_ON_TRIANGLE | 
    RobustPreds.HEAD_ON_TRIANGLE);

   public IntersectionPoint() {
   }

   public boolean isCoincident() {
      return primaryCoincident != null;
   }

   public boolean matches (IntersectionPoint p) {
      return face == p.face && edge == p.edge;
   }
   
   /**
    * Queries whether the edge head is inside the face, after accounting
    * for tie-breaking), for the intersection associated with this point.
    * 
    * @return <code>true</code> if the edge head is inside the face
    */
   public boolean headInsideFace() {
      return ((intersectionCode & RobustPreds.TAIL_OUTSIDE) != 0);
   }

   /**
    * Returns flags describing the degeneracies, if any, 
    * for intersection associated with this point.
    * 
    * @return intersection degeneracy flags
    */
   public int getDegeneracies() {
      return (intersectionCode & RobustPreds.DEGENERACY_MASK);
   }
   
   public PolygonalMesh getOtherMesh (PolygonalMesh mesh) {
      PolygonalMesh emesh = (PolygonalMesh)edge.getHead().getMesh();
      if (emesh == mesh) {
         return (PolygonalMesh)face.getMesh();
      }
      else {
         return emesh;
      }
   }

   public boolean onTriangleBorder() {
      return (intersectionCode & TRIANGLE_EDGE_MASK) != 0;
   }
   
   /**
    * If this intersection is degenerate and associated with one of the 
    * triangle vertices, return that vertex. Otherwise, return 
    * <code>null</code>.
    * 
    * @return degenerate triangle vertex or <code>null</code> 
    */
   public Vertex3d getTriangleVertex() {
      int tedges = (intersectionCode & TRIANGLE_EDGE_MASK);
      if (tedges != 0) {
         HalfEdge he = face.firstHalfEdge();
         if (tedges == RobustPreds.V0_ON_SEGMENT) {
            return he.getHead();
         }
         else if (tedges == RobustPreds.V1_ON_SEGMENT) {
            return he.getNext().getHead();
         }
         else if (tedges == RobustPreds.V2_ON_SEGMENT) {
            return he.getTail();
         }
      }
      return null;
   }
   
   /**
    * Queries whether this intersection point is degenerate such that
    * it lies on the specified triangle edge. This includes both the edge 
    * itself as well as its end-point vertices. The edge may be specified 
    * using either the actual triangle edge or its opposite.
    * 
    * @param edge face edge which the point must lie on.
    * @return <code>true</code> if the point lies on the edge.
    */
   public boolean onTriangleEdge (HalfEdge edge) {
      HalfEdge he = face.firstHalfEdge();
      if (he == edge || he.getPrimary() == edge) {
         return (intersectionCode & RobustPreds.E01_ON_SEGMENT) != 0;
      }
      he = he.getNext();
      if (he == edge || he.getPrimary() == edge) {
         return (intersectionCode & RobustPreds.E12_ON_SEGMENT) != 0;
      }
      he = he.getNext();
      if (he == edge || he.getPrimary() == edge) {
         return (intersectionCode & RobustPreds.E20_ON_SEGMENT) != 0;
      }
      return false;
   }
   
   /**
    * If this point and the specified intersection point <code>p1</code>
    * are degenerate such that they both lie on a common triangle edge, 
    * return that edge. Otherwise, return null. If both points lie on
    * a vertex, this method will return null as well.
    *
    * @param p1 point to query for common edge
    * @return common edge, if any
    */
   public HalfEdge getCommonEdge (IntersectionPoint p1) {
      int tedges = (intersectionCode & TRIANGLE_EDGE_MASK);
      tedges &= (p1.intersectionCode & TRIANGLE_EDGE_MASK);
      HalfEdge he = face.firstHalfEdge();
      if (tedges == RobustPreds.E01_ON_SEGMENT) {
         return he;
      }
      he = he.getNext();
      if (tedges == RobustPreds.E12_ON_SEGMENT) {
         return he;
      }
      he = he.getNext();
      if (tedges == RobustPreds.E20_ON_SEGMENT) {
         return he;
      }
      return null;      
   }
   
   /**
    * If this intersection is degenerate and associated with one of the 
    * triangle edges (but not a vertex), return that edge. Otherwise, return 
    * <code>null</code>.
    * 
    * @return degenerate triangle edge or <code>null</code> 
    */
   public HalfEdge getTriangleEdge() {
      int tedges = (intersectionCode & TRIANGLE_EDGE_MASK);
      if (tedges != 0) {
         HalfEdge he = face.firstHalfEdge();
         if (tedges == RobustPreds.E01_ON_SEGMENT) {
            return he;
         }
         he = he.getNext();
         if (tedges == RobustPreds.E12_ON_SEGMENT) {
            return he;
         }
         he = he.getNext();
         if (tedges == RobustPreds.E20_ON_SEGMENT) {
            return he;
         }
      }
      return null;
   }

   /**
    * If this intersection is degenerate and associated with one of the 
    * segment vertices (but not both), return that vertex. Otherwise, return 
    * <code>null</code>.
    * 
    * @return degenerate segment vertex or <code>null</code> 
    */
   public Vertex3d getSegmentVertex() {
      int dseg = (intersectionCode & SEGMENT_MASK);
      if (dseg != 0) {
         if (dseg == RobustPreds.TAIL_ON_TRIANGLE) {
            return edge.getTail();
         }
         else if (dseg == RobustPreds.HEAD_ON_TRIANGLE) {
            return edge.getHead();
         }
      }
      return null;
   }

   Vertex3d getInsideVertex() {
      return (headInsideFace() ? edge.getHead() : edge.getTail());
   }

   Vertex3d getOutsideVertex() {
      return (headInsideFace() ? edge.getTail() : edge.getHead());
   }

    public IntersectionPoint next() {
      if (contourIndex == -1) {
         return null;
      }
      else if (contourIndex == contour.size()-1) {
         return contour.isClosed() ? contour.get(0) : null;
      }
      else {
         return contour.get(contourIndex+1);
      }
   }

   public IntersectionPoint prev() {
      if (contourIndex == -1) {
         return null;
      }
      else if (contourIndex == 0) {
         return contour.isClosed() ? contour.get(contour.size()-1) : null;
      }
      else {
         return contour.get(contourIndex-1);
      }
   }

   public Face findSegmentFace (PolygonalMesh mesh) {
      return contour.findSegmentFace (this, mesh);
   }

   public Face getEffectiveFace (int meshNum) {
      if (meshNum == 0) {
         return effectiveFace0;
      }
      else {
         return effectiveFace1;
      }
   }

   public void setEffectiveFace (int meshNum, Face face) {
      if (meshNum == 0) {
         effectiveFace0 = face;
      }
      else {
         effectiveFace1 = face;
      }
   }

   public HalfEdge getNearEdge (int meshNum) {
      if (meshNum == 0) {
         return nearEdge0;
      }
      else {
         return nearEdge1;
      }
   }

   public void setNearEdge (int meshNum, HalfEdge edge) {
      if (meshNum == 0) {
         nearEdge0 = edge;
      }
      else {
         nearEdge1 = edge;
      }
   }

   public int getEmptyMark (int meshNum) {
      if (meshNum == 0) {
         return emptyMark0;
      }
      else {
         return emptyMark1;
      }
   }

   public void setEmptyMark (int meshNum, int mark) {
      if (meshNum == 0) {
         emptyMark0 = mark;
      }
      else {
         emptyMark1 = mark;
      }
   }

   public void clearEmptyMarks() {
      emptyMark0 = 0;
      emptyMark1 = 0;
   }

   public IntersectionPoint clone () {
      IntersectionPoint pnt = (IntersectionPoint)super.clone();
      pnt.myVertex = null;
      return pnt;
   }

   static DataBuffer.Offsets getStateSize () {
      return new DataBuffer.Offsets (4, 3, 0);
   }

   void getState (
      DataBuffer data, PolygonalMesh mesh0, PolygonalMesh mesh1) {
      
      int code = intersectionCode;
      if (edge.head.getMesh() == mesh0) {
         code |= FACE_ON_MESH1;
      }
      data.zput (code);
      data.zput (face.getIndex());
      data.zput (edge.getIndex());
      if (primaryCoincident == null) {
         data.zput (-1);
      }
      else if (primaryCoincident == IntersectionPoint.COINCIDENT) {
         data.zput (-2);
      }
      else {
         data.zput (primaryCoincident.contourIndex);
      }
      data.dput (this);      
   }

   void setState (DataBuffer data, PolygonalMesh mesh0, PolygonalMesh mesh1) {
      data.dget (this);
      int code = data.zget();
      if ((code & FACE_ON_MESH1) != 0) {
         face = mesh1.getFace (data.zget());
         edge = mesh0.getHalfEdge (data.zget());
      }
      else {
         face = mesh0.getFace (data.zget());
         edge = mesh1.getHalfEdge (data.zget());
      }
      intersectionCode = (code & ~FACE_ON_MESH1);
      int pcidx = data.zget();
      if (pcidx == -2) {
         primaryCoincident = IntersectionPoint.COINCIDENT;
      }
      else if (pcidx >= 0) {
         primaryCoincident = contour.get(pcidx);
      }      
   }

   public boolean equals (IntersectionPoint mip, StringBuilder msg) {
      if (!super.equals (mip)) {
         if (msg != null) msg.append ("point position differs\n");
         return false;
      }
      if (face != mip.face) {
         if (msg != null) msg.append ("face differs\n");
         return false;
      }
      if (edge != mip.edge) {
         if (msg != null) msg.append ("edge differs\n");
         return false;
      }
      if (intersectionCode != mip.intersectionCode) {
         if (msg != null) msg.append ("intersectionCode differs\n");
         return false;
      }
      if (contourIndex != mip.contourIndex) {
         if (msg != null) msg.append ("contourIndex differs\n");
         return false;
      }
      if ((primaryCoincident == null) != (mip.primaryCoincident == null)) {
         if (msg != null) msg.append ("primaryCoincident == null differs\n");
         return false;
      }
      if (primaryCoincident != null) {
         if ((primaryCoincident == COINCIDENT) !=
             (mip.primaryCoincident == COINCIDENT)) {
            if (msg != null) {
               msg.append ("primaryCoincident == COINCIDENT differs\n");
            }
            return false;
         }
         if (primaryCoincident != COINCIDENT) {
            if (primaryCoincident.contourIndex !=
                mip.primaryCoincident.contourIndex) {
               if (msg != null) {
                  msg.append ("primaryCoincident.contourIndex differs\n");
               }
               return false;
            }
         }
      }
      return true;
   }
}
