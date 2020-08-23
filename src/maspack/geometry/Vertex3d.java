/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;

import maspack.matrix.Matrix3d;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.util.Clonable;
import maspack.util.InternalErrorException;
import maspack.util.ArraySupport;

/**
 * Vertex for a 3D dimensional polyhedral object.
 */
public class Vertex3d extends Feature implements Clonable, Boundable {
   
   // John Lloyd, June, 2020.  If groupHalfEdgesByHardEdge == true, then when
   // sorting the half edges incident on a vertex, isHard() is used to
   // delineate edge groupings along with open edges. This appears to be
   // obsolete, and has the side effect that for a closed mesh, the incident
   // half edges are not necessarily sorted counter-clockwise about the normal.
   public static boolean groupHalfEdgesByHardEdge = false; 
   
   /**
    * 3D point associated with this vertex.
    */
   public Point3d pnt;
   public Point3d myRenderPnt;

   // cached value of pnt in world coordinates
   //private Point3d myWorldPnt;
   MeshBase myMesh;
   //public int myWorldCoordCnt = -1;
   //public int uniqueIndex;
   //static int nextUniqueIndex = 0;

   protected HalfEdgeNode incidentHedges;
   protected boolean hedgesSorted = false; 
   int hedgesModCount = 0;
   int idx;

   private class EdgeIterator implements Iterator<HalfEdge> {
      HalfEdgeNode heNode;
      int expectedModCount;

      EdgeIterator (HalfEdgeNode node) {
         heNode = node;
         expectedModCount = hedgesModCount;
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
            if (expectedModCount != hedgesModCount) {
               throw new ConcurrentModificationException(
                  expectedModCount + " " + hedgesModCount);
            }
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
      sortHedgesIfNecessary();
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
      this.hedgesSorted = false;
      //uniqueIndex = nextUniqueIndex++;
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
      //uniqueIndex = nextUniqueIndex++;
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
      this.hedgesSorted = false;
      this.idx = idx;
      //uniqueIndex = nextUniqueIndex++;
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
      //uniqueIndex = nextUniqueIndex++;
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
      sortHedgesIfNecessary();
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
    * Computes a normal for this vertex by taking the average of all the
    * associated face normals, expressed in world coordinates
    * 
    * @param nrm
    * returns the computed normal
    * @return false if no faces normals are found
    */
   public boolean computeWorldNormal (Vector3d nrm) {
      boolean faceNormalsFound = false;
      sortHedgesIfNecessary();
      nrm.set (0, 0, 0);
      for (HalfEdgeNode node = incidentHedges; node != null; node = node.next) {
         Face face = node.he.face;
         if (face != null) {
            nrm.add (face.getWorldNormal());
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
      sortHedgesIfNecessary();      
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
   
   /**
    * Computes a normal for this vertex by averaging the face normals
    * of all adjacent faces, scaled by their face areas
    * 
    * @param nrm
    * returns the computed normal
    * @return false if no adjacent faces present
    */
   public boolean computeAreaWeightedNormal (Vector3d nrm) {
      sortHedgesIfNecessary();      
      boolean hasNormal = (incidentHedges != null);
      nrm.set (0, 0, 0);
      Vector3d fnrm = new Vector3d();
      for (HalfEdgeNode node = incidentHedges; node != null; node = node.next) {
         Face f = node.he.getFace();
         double a = f.computeArea();
         f.computeNormal(fnrm);
         nrm.scaledAdd(a, fnrm);
      }
      if (hasNormal) {
         nrm.normalize();
      }
      return hasNormal;
   }

//   /**
//    * Compute the normals for all the half-edges incident on this vertex.
//    * If none of the half-edges are open or hard, then only one normal
//    * is computed, which will be shared by all the half-edges. 
//    * Otherwise, extra normals will be computed for the 
//    * sub-regions delimited by open or hard edges.
//    *   
//    * @param nrms list of normal vectors where the results should be placed
//    * @param idx starting index into <code>nrms</code> for the result
//    * @return advanced index
//    */
//   public int computeAngleWeightedNormals (List<Vector3d> nrms, int idx) {
//      sortHedgesIfNecessary(); 
//      HalfEdgeNode node = incidentHedges;
//      int nrmSize = nrms.size ();
//      while (node != null) {
//         // XXX chance for index-out-of-bounds here
//         Vector3d nrm = null;
//         if (idx < nrmSize) {
//            nrm = nrms.get(idx++);
//            nrm.setZero();
//         } else {
//            System.err.println ("Vertex3d.computeAngleWeightedNormals(...): hack to prevent out of bounds index");
//            nrm = new Vector3d();
//            nrms.add (nrm);
//            ++nrmSize;
//         }
//         
//         do {
//            HalfEdge he = node.he;
//            nrm.angleWeightedCrossAdd (
//               he.tail.pnt, he.head.pnt, he.next.head.pnt);
//            node = node.next;
//         }
//         while (node != null && !isStartingEdge(node.he));
//         nrm.normalize();         
//      }
//      return idx;
//   }

   public boolean computeRenderNormal (Vector3d nrm) {
      boolean faceNormalsFound = false;
      sortHedgesIfNecessary();
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
    * This is done so that the half-edges are ordered in decreasing face
    * order.
    * 
    * @param he
    * HalfEdgeNode containing the half edge in question
    */
   public void addIncidentHalfEdge (HalfEdge he) {
      HalfEdgeNode node = new HalfEdgeNode (he);
      hedgesModCount++;
      node.next = incidentHedges;
      incidentHedges = node;
      hedgesSorted = false;
   }
   
   private class HalfEdgeFaceComparator implements Comparator<HalfEdge> {
      public int compare (HalfEdge he0, HalfEdge he1) {
         if (he0.face.idx == he1.face.idx) {
            return 0;
         }
         else if (he0.face.idx < he1.face.idx) {
            return -1;
         }
         else {
            return 1;
         }
      }
   }

   String edgeStr (HalfEdge he) {
      return he.tail.idx + "->" + he.head.idx;
   }

   /**
    * Returns true if he is a boundary half-edge for purposes of computing
    * normals.
    */
   public boolean isNormalBoundary (HalfEdge he) {
      return he.opposite == null || he.isHard();
   }

   /**
    * Start of a half edge group.
    */
   public boolean isGroupStart (HalfEdge he) {
      if (groupHalfEdgesByHardEdge) {
         return he.opposite == null || he.isHard();   
      }
      else {
         return he.opposite == null;
      }
   }
   
   /**
    * End of a half edge group.
    */  
   private boolean isGroupEnd (HalfEdge he) {
      if (groupHalfEdgesByHardEdge) {
         return he == null || he.isHard();
      }
      else {
         return he == null;
      }
   }

   /**
    * Sort the half edges into contiguous groups, with the starting edges of
    * each group ordered by increasing face number.  The reason we sort the
    * starting edges by face number is to make the ordering deterministic for a
    * given mesh geometry.
    */
   // Need to synchronize because this could be called from either the
   // simulation or rendering thread.
   protected synchronized void sortHedgesIfNecessary() {
      if (!hedgesSorted) {
         if (incidentHedges == null) {
            // no incident edges, so we are sorted by default
            hedgesSorted = true;
            return;
         }
         // go through all incident edges, and find the ones that start a
         // contiguous group (either open edges or those which are marked
         // hard). If there are no such edges, then we select as the (single)
         // starting edge the one whose face has the lowest index.
         HashSet<HalfEdge> marked = new HashSet<HalfEdge>();
         ArrayList<HalfEdge> startingHedges = new ArrayList<HalfEdge>();
         int cnt = 0;
         for (HalfEdgeNode node=incidentHedges; node!=null; node=node.next) {
            HalfEdge he = node.he;
            if (!marked.contains(he)) {
               if (isGroupStart (he)) {
                  startingHedges.add (he);
                  // Traverse through the half-edges contiguous to he, marking
                  // them. They can be discarded since none of them can be a
                  // boundary edge.
                  do {
                     marked.add (he);
                     he = he.next.opposite;
                  }                     
                  while (!isGroupEnd(he));
               }
               else {
                  // Traverse through the half-edges contiguous to he, marking
                  // them. They also can be discarded since none of them can be
                  // a boundary edge. However, they may form a loop, in which
                  // case we choose the choose the half-edge with the lowest
                  // face index as a starting edge
                  HalfEdge he0 = he;
                  HalfEdge minFaceHe = he;
                  int minFaceIdx = he0.face.idx;
                  do {
                     if (he.face.idx < minFaceIdx) {
                        minFaceIdx = he.face.idx;
                        minFaceHe = he;
                     }
                     marked.add (he);
                     he = he.next.opposite;
                  }                     
                  while (!isGroupEnd(he) && he != he0);
                  if (he == he0) {
                     startingHedges.add (minFaceHe);
                  }
               }
            }
            cnt++;
         }
         if (startingHedges.size() == 0) {
            throw new InternalErrorException (
               "No starting edges found for half-edges incident to vertex "+idx);
         }
         if (startingHedges.size() > 1) {
            // sort the starting hedges by face number
            Collections.sort (startingHedges, new HalfEdgeFaceComparator());
         }
         
         // Now rebuild the list of incident half edges in the prescribed order
         HalfEdgeNode prev = null;
         incidentHedges = null;
         for (HalfEdge start : startingHedges) {
            HalfEdge he = start;
            do {
               HalfEdgeNode node = new HalfEdgeNode (he);
               if (prev == null) {
                  incidentHedges = node;
               }
               else {
                  prev.next = node;
               }
               prev = node;
               node.next = null;
               he = he.next.opposite;
            }
            while (!isGroupEnd(he) && he != start);
         }
         if (numIncidentHalfEdges() != cnt) {
            throw new InternalErrorException (
               "Error sorting incident half-edges: started with " + cnt +
               ", ended with " + numIncidentHalfEdges());
         }
         hedgesSorted = true;
      }
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
            hedgesModCount++;
            if (prevNode == null) {
               incidentHedges = node.next;
            }
            else {
               prevNode.next = node.next;
            }
            hedgesSorted = false;
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
      sortHedgesIfNecessary();
      if (incidentHedges != null) {
         return incidentHedges.he;
      }
      else {
         return null;
      }
   }
   
   HalfEdgeNode getIncidentHedges() {
      sortHedgesIfNecessary();
      return incidentHedges;
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

//   /**
//    * Sets a color for this vertex, or removes the color if null is specified.
//    * 
//    * @param color
//    * color to set for this vertex.
//    */
//   public void setColor (Color color) {
//      if (color != null) {
//         myColor = new float[4];
//         myColor = color.getColorComponents (myColor);
//         myColor[3] = 1;
//      }
//      else {
//         myColor = null;
//      }
//   }
   
//   public void setColor(Color color, float alpha) {
//      if (color != null) {
//         myColor = new float[4];
//         myColor = color.getColorComponents(myColor);
//         myColor[3] = alpha;
//      } else {
//         myColor = null;
//      }
//   }

//   public void setColor(float r, float g, float b) {
//      setColor(r, g, b, 1);
//   }
   
//   public void setColor (float r, float g, float b, float a) {
//      if (myColor == null) {
//         myColor = new float[4];
//      }
//      myColor[0] = r;
//      myColor[1] = g;
//      myColor[2] = b; 
//      myColor[3] = a;
//   }

//   public void setColor (double r, double g, double b) {
//      setColor( (float)r, (float)g, (float)b, 1);
//   }
   
//   public void setColor(double r, double g, double b, double a) {
//      setColor( (float)r, (float)g, (float)b, (float)a);   
//   }

//   /** 
//    * Sets the vertex color based on hue, saturation, and value (brightness).
//    * 
//    * @param h hue (in the range 0-1)
//    * @param s saturation (in the range 0-1)
//    * @param b brightness (in the range 0-1)
//    */
//   public void setColorHSV (double h, double s, double b) {
//      setColorHSV(h,s,b,1);
//   }
   
//   /** 
//    * Sets the vertex color based on hue, saturation, and value (brightness).
//    * 
//    * @param h hue (in the range 0-1)
//    * @param s saturation (in the range 0-1)
//    * @param b brightness (in the range 0-1)
//    * @param a alpha (in the range of 0-1)
//    */
//   public void setColorHSV (double h, double s, double b, double a) {
//      double c = b*s;
//      if (h < 0) {
//         h = 0;
//      }
//      else if (h > 1) {
//         h = 1;
//      }
//      double hp = 6*h;
//      double m = b-c;
//      switch ((int)hp) {
//         case 0: {
//            setColor (c+m, hp+m, m, a);
//            break;
//         }
//         case 1:{
//            setColor (2-hp+m, c+m, m, a);
//            break;
//         }
//         case 2: {
//            setColor (m, c+m, hp-2+m, a);
//            break;
//         }
//         case 3:{
//            setColor (m, 4-hp+m, c+m, a);
//            break;
//         }
//         case 4: {
//            setColor (hp-4+m, m, c+m, a);
//            break;
//         }
//         case 5:
//            // 6 will appear if h == 1
//         case 6: {
//            setColor (c+m, m, 6-hp+m, a);
//            break;
//         }
//      }
//      
//   }

//   /**
//    * Returns the color of this vertex, or null if no color has been set.
//    * 
//    * @return color for this vertex
//    */
//   public Color getColor() {
//      if (myColor == null) {
//         return null;
//      }
//      else {
//         return new Color (myColor[0], myColor[1], myColor[2], myColor[3]);
//      }
//   }

//   public float[] getColorArray() {
//      return myColor;
//   }

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
      pnt.set(this.pnt);
      if (myMesh != null) {
         myMesh.transformToWorld (pnt);
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
          //vtx.uniqueIndex = -1;
          vtx.incidentHedges = null;
          vtx.hedgesSorted = false;
          vtx.hedgesModCount = 0;
          vtx.idx = idx;
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

   public void updateBounds (Vector3d min, Vector3d max) {
      pnt.updateBounds (min, max);
   }

   public void computeCentroid (Vector3d centroid) {
      centroid.set (pnt);
   }
   
   public double computeCovariance (Matrix3d C) {
      C.outerProduct (pnt, pnt);
      return 1;
   }
   
   public static int[] getIndices (Collection<Vertex3d> vtxs) {
      ArrayList<Integer> list = new ArrayList<Integer>();
      for (Vertex3d v : vtxs) {
         list.add (v.getIndex());
      }
      return ArraySupport.toIntArray (list);      
   }

   public double distance (Vector3d p) {
      return pnt.distance (p);
   }

   public double distance (Vertex3d vtx) {
      return pnt.distance (vtx.pnt);
   }

   @Override
   public void nearestPoint(Point3d nearest, Point3d pnt) {
      nearest.set(getPosition ());
   }

}
