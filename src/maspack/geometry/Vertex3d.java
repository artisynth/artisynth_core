/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import maspack.matrix.*;
import maspack.util.Clonable;

import java.util.*;
import java.awt.*;

/**
 * Vertex for a 3D dimensional polyhedral object.
 */
public class Vertex3d extends Feature implements Clonable, Boundable {
   /**
    * 3D point associated with this vertex.
    */
   public Point3d pnt;
   private float[] myColor;
   public Point3d myRenderPnt;

   // cached value of pnt in world coordinates
   //private Point3d myWorldPnt;
   MeshBase myMesh;
   //public int myWorldCoordCnt = -1;
   public int uniqueIndex;
   static int nextUniqueIndex = 0;

   HalfEdgeNode incidentHedges;
   int idx;

   private class EdgeIterator implements Iterator<HalfEdge> {
      HalfEdgeNode heNode;

      EdgeIterator (HalfEdgeNode node) {
         heNode = node;
      }

      public boolean hasNext() {
         return heNode != null;
      }

      public HalfEdge next() throws NoSuchElementException {
         if (heNode == null) {
            throw new NoSuchElementException();
         }
         else {
            HalfEdge he = heNode.he;
            heNode = heNode.next;
            return he;
         }
      }

      public void remove()
         throws UnsupportedOperationException, IllegalStateException {
         throw new UnsupportedOperationException();
      }
   }

   /**
    * Returns an iterator for all the half-edges which are incident onto this
    * vertex. The iterator will return objects of type {@link HalfEdge HalfEdge}.
    * 
    * @return iterator over incident half-edges
    */
   public Iterator<HalfEdge> getIncidentHalfEdges() {
      return new EdgeIterator (incidentHedges);
   }

   

   /**
    * Returns the number of half-edges which are incident onto this
    * vertex.
    * 
    * @return number of incident half-edges
    */
   public int numIncidentHalfEdges() {
      int cnt = 0;
      for (HalfEdgeNode n = incidentHedges; n != null; n = n.next) {
         cnt++;
      }
      return cnt;
   }

   /**
    * Creates a new Vertex3d with a point initialized to (0,0,0) and an index
    * value of -1.
    */
   public Vertex3d() {
      super (VERTEX_3D);
      this.pnt = new Point3d();
      // this.faceList = null;
      this.incidentHedges = null;
      uniqueIndex = nextUniqueIndex++;
   }

   /**
    * Creates a new Vertex3d with point initialized to (0,0,0) and a specified
    * index value.
    * 
    * @param idx
    * desired index value
    */
   public Vertex3d (int idx) {
      this();
      this.idx = idx;
      uniqueIndex = nextUniqueIndex++;
   }

   /**
    * Creates a new Vertex3d with a specified point and index value. The
    * {@link #pnt pnt} field is set to refer directly to the supplied point.
    * 
    * @param pnt
    * point to use for the {@link #pnt pnt} field
    * @param idx
    * desired index value
    */
   public Vertex3d (Point3d pnt, int idx) {
      super (VERTEX_3D);
      this.pnt = pnt;
      // this.faceList = null;
      this.incidentHedges = null;
      this.idx = idx;
      uniqueIndex = nextUniqueIndex++;
   }
   
   /**
    * Creates a new Vertex3d with a specified point and index value.
    * 
    * @param x
    * vertex x coordinate
    * @param y
    * vertex y coordinate
    * @param z
    * vertex z coordinate
    */
   public Vertex3d(double x, double y, double z) {
      this(new Point3d(x,y,z),-1);
   }
   
   /**
    * Creates a new Vertex3d with a specified point and index value.
    * 
    * @param x
    * vertex x coordinate
    * @param y
    * vertex y coordinate
    * @param z
    * vertex z coordinate
    * @param idx
    * desired index value
    */
   public Vertex3d(double x, double y, double z, int idx) {
      this(new Point3d(x,y,z),idx);
   }

   /**
    * Creates a new Vertex3d with a specified point and an index value of -1.
    * The {@link #pnt pnt} field is set to refer directly to the supplied point.
    * 
    * @param pnt
    * point to use for the {@link #pnt pnt} field
    */
   public Vertex3d (Point3d pnt) {
      this (pnt, 0);
      uniqueIndex = nextUniqueIndex++;
   }

   /**
    * Computes a normal for this vertex by taking the average of all the
    * associated face normals.
    * 
    * @param nrm
    * returns the computed normal
    * @return false if no faces normals are found
    */
   public boolean computeNormal (Vector3d nrm) {
      boolean faceNormalsFound = false;
      nrm.set (0, 0, 0);
      for (HalfEdgeNode node = incidentHedges; node != null; node = node.next) {
         Face face = node.he.face;
         if (face != null) {
            nrm.add (face.getNormal());
            faceNormalsFound = true;
         }
      }
      if (faceNormalsFound) {
         nrm.normalize();
      }
      return faceNormalsFound;
   }

   /**
    * Computes a normal for this vertex by averaging the cross products of all
    * the incident half edges.
    * 
    * @param nrm
    * returns the computed normal
    * @return false if no incident halt edges are present
    */
   public boolean computeAngleWeightedNormal (Vector3d nrm) {
      boolean hasNormal = (incidentHedges != null);
      nrm.set (0, 0, 0);
      for (HalfEdgeNode node = incidentHedges; node != null; node = node.next) {
         HalfEdge he = node.he;
         nrm.angleWeightedCrossAdd (he.tail.pnt, he.head.pnt, he.next.head.pnt);
      }
      if (hasNormal) {
         nrm.normalize();
      }
      return hasNormal;
   }

   public boolean computeRenderNormal (Vector3d nrm) {
      boolean faceNormalsFound = false;
      nrm.set (0, 0, 0);
      for (HalfEdgeNode node = incidentHedges; node != null; node = node.next) {
         Face face = node.he.face;
         if (face != null) {
            nrm.add (face.getRenderNormal());
            faceNormalsFound = true;
         }
      }
      if (faceNormalsFound) {
         nrm.normalize();
      }
      return faceNormalsFound;
   }

   /**
    * Adds a half-edge to the list of half-edges incident onto this vertex.
    * 
    * @param he
    * HalfEdgeNode containing the half edge in question
    */
   public void addIncidentHalfEdge (HalfEdge he) {
      HalfEdgeNode node = new HalfEdgeNode (he);
      node.next = incidentHedges;
      incidentHedges = node;
   }

   /**
    * Removes a half-edge from the list of half-edges incident onto this vertex.
    * 
    * @param hedge
    * half-edge to remove
    * @return true if the half-edge was present; false otherwise
    */
   public boolean removeIncidentHalfEdge (HalfEdge hedge) {
      HalfEdgeNode prevNode = null;
      for (HalfEdgeNode node = incidentHedges; node != null; node = node.next) {
         if (node.he == hedge) { // remove the node and return the node
            if (prevNode == null) {
               incidentHedges = node.next;
            }
            else {
               prevNode.next = node.next;
            }
            return true;
         }
         prevNode = node;
      }
      return false;
   }

   /**
    * Returns the first incident half-edge listed for this vertex, or null if
    * there are no incident half-edges.
    * 
    * @return first incident half-edge
    */
   public HalfEdge firstIncidentHalfEdge() {
      if (incidentHedges != null) {
         return incidentHedges.he;
      }
      else {
         return null;
      }
   }

   /**
    * Looks for an incident half-edge with a given tail.
    */
   HalfEdge findOppositeHalfEdge (Vertex3d tail) {
      for (HalfEdgeNode node = incidentHedges; node != null; node = node.next) {
         if (node.he.tail == tail) {
            return node.he;
         }
      }
      return null;
   }

   /**
    * Check if a particular half-edge is incident to this vertex.
    */
   boolean hasIncidentHalfEdge (HalfEdge he) {
      for (HalfEdgeNode node = incidentHedges; node != null; node = node.next) {
         if (node.he == he) {
            return true;
         }
      }
      return false;
   }

//   /**
//    * Checks that the specified pnt is within the Voronoi region defined by the
//    * half-edges incident on this vertex.
//    * 
//    * @param pnt
//    * point to test for inclusion in Voronoi region
//    * @return true if point is within Voronoi region
//    */
//   public boolean voronoiCheck (Point3d pnt) {
//      double dhp_x = pnt.x - pnt.x;
//      double dhp_y = pnt.y - pnt.y;
//      double dhp_z = pnt.z - pnt.z;
//
//      for (HalfEdgeNode node = incidentHedges; node != null; node = node.next) {
//         HalfEdge incoming = node.he;
//         if (incoming.dot (dhp_x, dhp_y, dhp_z) < 0) {
//            return false;
//         }
//         HalfEdge outgoing = incoming.next;
//         if (outgoing != null && outgoing.opposite == null) {
//            if (outgoing.dot (dhp_x, dhp_y, dhp_z) > 0) {
//               return false;
//            }
//         }
//      }
//      return true;
//   }

   /**
    * Returns the index value associated with this vertex.
    * 
    * @return index value
    */
   public int getIndex() {
      return idx;
   }

   /**
    * Sets the index value for this vertex.
    * 
    * @param idx
    * new index value
    */
   public void setIndex (int idx) {
      this.idx = idx;
   }

   // Dec 9, 2008. John Lloyd: removed hashCode/equals override, since it was
   // causing confusion. For now equals (Object obj) should return true only if
   // the objects are identical. If equals based on contents are required, then
   // one should create a subclass.

   // /**
   // * Hash code of Vertex3d is computed as:
   // * pnt.hashCode() * {@link #getIndex()}.
   // *
   // * @return hash code of this vertex based on Point3d hashCode() and
   // * {@link #getIndex()}
   // */
   // @Override
   // public int hashCode()
   // {
   // return this.pnt.hashCode() * (this.idx + 31);
   // }

   // /**
   // * Two vertices are equal, if their Point3d and indexes are equal.
   // *
   // * @param o object for comparison
   // * @return true if vertices have same index and spatial coordinates
   // */
   // @Override
   // public boolean equals(Object o)
   // {
   // if (o == this)
   // return true;

   // if (o == null)
   // return false;

   // if (!(o instanceof Vertex3d))
   // return false;

   // Vertex3d other = (Vertex3d) o;

   // return ((this.idx == other.idx) && this.pnt.equals(other.pnt));
   // }

   /**
    * Sets a color for this vertex, or removes the color if null is specified.
    * 
    * @param color
    * color to set for this vertex.
    */
   public void setColor (Color color) {
      if (color != null) {
         myColor = new float[4];
         myColor = color.getColorComponents (myColor);
         myColor[3] = 1;
      }
      else {
         myColor = null;
      }
   }
   
   public void setColor(Color color, float alpha) {
      if (color != null) {
         myColor = new float[4];
         myColor = color.getColorComponents(myColor);
         myColor[3] = alpha;
      } else {
         myColor = null;
      }
   }

   public void setColor(float r, float g, float b) {
      setColor(r, g, b, 1);
   }
   
   public void setColor (float r, float g, float b, float a) {
      if (myColor == null) {
         myColor = new float[4];
      }
      myColor[0] = r;
      myColor[1] = g;
      myColor[2] = b; 
      myColor[3] = a;
   }

   public void setColor (double r, double g, double b) {
      setColor( (float)r, (float)g, (float)b, 1);
   }
   
   public void setColor(double r, double g, double b, double a) {
      setColor( (float)r, (float)g, (float)b, (float)a);   
   }

   /** 
    * Sets the vertex color based on hue, saturation, and value (brightness).
    * 
    * @param h hue (in the range 0-1)
    * @param s saturation (in the range 0-1)
    * @param b brightness (in the range 0-1)
    */
   public void setColorHSV (double h, double s, double b) {
      setColorHSV(h,s,b,1);
   }
   
   /** 
    * Sets the vertex color based on hue, saturation, and value (brightness).
    * 
    * @param h hue (in the range 0-1)
    * @param s saturation (in the range 0-1)
    * @param b brightness (in the range 0-1)
    * @param a alpha (in the range of 0-1)
    */
   public void setColorHSV (double h, double s, double b, double a) {
      double c = b*s;
      if (h < 0) {
         h = 0;
      }
      else if (h > 1) {
         h = 1;
      }
      double hp = 6*h;
      double m = b-c;
      switch ((int)hp) {
         case 0: {
            setColor (c+m, hp+m, m, a);
            break;
         }
         case 1:{
            setColor (2-hp+m, c+m, m, a);
            break;
         }
         case 2: {
            setColor (m, c+m, hp-2+m, a);
            break;
         }
         case 3:{
            setColor (m, 4-hp+m, c+m, a);
            break;
         }
         case 4: {
            setColor (hp-4+m, m, c+m, a);
            break;
         }
         case 5:
            // 6 will appear if h == 1
         case 6: {
            setColor (c+m, m, 6-hp+m, a);
            break;
         }
      }
      
   }

   /**
    * Returns the color of this vertex, or null if no color has been set.
    * 
    * @return color for this vertex
    */
   public Color getColor() {
      if (myColor == null) {
         return null;
      }
      else {
         return new Color (myColor[0], myColor[1], myColor[2], myColor[3]);
      }
   }

   public float[] getColorArray() {
      return myColor;
   }

   public void saveRenderInfo() {
      if (myRenderPnt == null) {
         myRenderPnt = new Point3d();
      }
      myRenderPnt.x = pnt.x;
      myRenderPnt.y = pnt.y;
      myRenderPnt.z = pnt.z;
   }

   /**
    * Returns the point value of this vertex in world coordinates. The returned
    * value should not be modified.
    * 
    * @return point value in world coordinates
    */
   public Point3d getWorldPoint() {
      if (myMesh == null || myMesh.myXMeshToWorldIsIdentity) {
         return pnt;
      }
      else {
         Point3d wpnt = new Point3d(pnt);
         wpnt.transform (myMesh.XMeshToWorld);
         return wpnt;
      }
      // else {
      //    if (myWorldCoordCnt != myMesh.myWorldCoordCounter) {
      //       if (myWorldPnt == null) {
      //          myWorldPnt = new Point3d();
      //       }
      //       myWorldPnt.transform (myMesh.getMeshToWorld(), pnt);
      //       myWorldCoordCnt = myMesh.myWorldCoordCounter;
      //    }
      //    return myWorldPnt;
      // }
   }

   /**
    * Returns the point value of this vertex in world coordinates. The returned
    * value should not be modified.
    */
   public void getWorldPoint(Point3d pnt) {
      if (myMesh == null || myMesh.myXMeshToWorldIsIdentity) {
         pnt.set(this.pnt);
      }
      else {
         pnt.transform (myMesh.XMeshToWorld, this.pnt);
      }
   }
   
   public MeshBase getMesh() {
      return myMesh;
   }

   public void setMesh (MeshBase mesh) {
      myMesh = mesh;
   }
   
   // implemented so it keeps subclass type
   public Vertex3d interpolate(double s, Vertex3d vb) {
      Point3d pos = new Point3d(getPosition());
      pos.interpolate(s, vb.getPosition());
      Vertex3d vtx = copy();
      vtx.setPosition(pos);
      return vtx;
   }

   public Point3d getPosition() {
      return pnt;
   }
   
   public void setPosition(Point3d pos) {
      if (pnt == null) {
         pnt = new Point3d();
      }
      pnt.set(pos);
   }
   
   public Vertex3d copy() {
      return clone();
   }
   
   public Vertex3d clone() {
      Vertex3d vtx = null;
      try {
          vtx = (Vertex3d)super.clone();
          vtx.pnt = new Point3d(pnt);
          vtx.myRenderPnt = null;
          //vtx.myWorldPnt = null;
          vtx.myMesh = null;
          //vtx.myWorldCoordCnt = -1;
          vtx.uniqueIndex = -1;
          vtx.incidentHedges = null;
          vtx.idx = idx;
          
          if (myColor != null) {
             vtx.myColor = new float[myColor.length];
             System.arraycopy (myColor, 0, vtx.myColor, 0, myColor.length);
          }        
      } catch (CloneNotSupportedException e) {}
      
      return vtx;
   }

   // implementation of IndexedPointSet

   public int numPoints() {
      return 1;
   }

   public Point3d getPoint (int idx) {
      switch (idx) {
         case 0: return pnt;
         default: {
            throw new ArrayIndexOutOfBoundsException ("idx=" + idx);
         }
      }
   }

   public void updateBounds (Point3d min, Point3d max) {
      pnt.updateBounds (min, max);
   }

   public void computeCentroid (Vector3d centroid) {
      centroid.set (pnt);
   }
   
   public double computeCovariance (Matrix3d C) {
      C.outerProduct (pnt, pnt);
      return 1;
   }
   
}
