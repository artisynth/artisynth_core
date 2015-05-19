/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import maspack.geometry.io.WavefrontReader;
import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.Matrix3d;
import maspack.matrix.NumericalException;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.SymmetricMatrix3d;
import maspack.matrix.Vector3d;
import maspack.matrix.Vector4d;
import maspack.spatialmotion.SpatialInertia;
import maspack.properties.HasProperties;
import maspack.render.GLRenderer;
import maspack.render.Material;
import maspack.render.RenderProps;
import maspack.util.InternalErrorException;
import maspack.util.ListIndexComparator;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import maspack.util.ArraySupport;

/**
 * Implements a polygonal mesh consisting of vertices, faces, and half-edges.
 */
public class PolygonalMesh extends MeshBase {

   protected ArrayList<Face> myFaces = new ArrayList<Face>();
   protected int[] myFaceOrder;  // used for sorting/drawing faces

   // begin texture mapping stuff
   protected ArrayList<Vector3d> myTextureVertexList = new ArrayList<Vector3d>();
   protected ArrayList<int[]> myTextureIndices = null;

   /*
    * vertex normals saved if mesh is read in from a file with explicit vertex
    * normals. If vertex normals are defined they will be used in write(), but
    * explicit normals are not currently used in MeshRenderer (see
    * Vertex3d.computeNormal())
    */
   protected ArrayList<Vector3d> myNormalList = new ArrayList<Vector3d>();
   protected ArrayList<int[]> myNormalIndices = null;

   private Material myFaceMaterial = null;
   private Material myBackMaterial = null;

   protected boolean myTriQuadCountsValid = false;
   protected int myNumTriangles;
   protected int myNumQuads;

   //OBBTree obbtree = null;
   //private AjlBvTree bvHierarchy = null;
   //private boolean bvHierarchyValid = false;
   private BVTree myBVTree = null;
   private boolean myBVTreeUpdated = false;

   private SignedDistanceGrid sdGrid = null;

   // mesh subdivision data
   private int subdivisions = 0;

   private boolean myFaceNormalsValid = false;
   private boolean myRenderNormalsValid = false;

   protected MeshRenderer myMeshRenderer = null;

   /*
    * Set to true if this mesh may be subject to self intersections. If set, it
    * will be tested for self-intersections before it is collided with any other
    * mesh.
    */
   public boolean canSelfIntersect = false;

   /** 
    * Invalidates the bvHierarchy
    */
   protected void invalidateBoundingInfo () {
      super.invalidateBoundingInfo();
      myBVTreeUpdated = false;
   }

   /** 
    * Clears the bvHierarchy
    */
   protected void clearBoundingInfo () {
      super.clearBoundingInfo();
      myBVTree = null;
   }

   protected void updateTriQuadCounts() {
      if (!myTriQuadCountsValid) {
         myNumTriangles = 0;
         myNumQuads = 0;
         for (int i=0; i<myFaces.size(); i++) {
            int size = myFaces.get(i).numEdges();
            if (size == 3) {
               myNumTriangles++;
            }
            else if (size == 4) {
               myNumQuads++;               
            }
         }
         myTriQuadCountsValid = true;
      }
   }

   /**
    * {@inheritDoc}
    */
   public void notifyVertexPositionsModified() {
      super.notifyVertexPositionsModified();
      myFaceNormalsValid = false;
      myRenderNormalsValid = false;
   }

   //   /**
   //    * Invalidates face normal data for this mesh.
   //    */
   //   @SuppressWarnings("unused")
   //   private void invalidateFaceNormals() {
   //      myFaceNormalsValid = false;
   //      myRenderNormalsValid = false;
   //      //myWorldCoordCounter++; // any world normals or other data that were
   //                              // calculated are now invalid
   //   }

   public static int computedFaceNormals = 0;

   /**
    * Updates face normals if necessary.
    */
   public void updateFaceNormals() {
      if (!myFaceNormalsValid) {
         computeFaceNormals();
         computedFaceNormals++;
      }
   }

   /**
    * Updates face rendering normals if necessary.
    */
   public void updateRenderNormals() {
      if (!myRenderNormalsValid) {
         for (int i = 0; i < myFaces.size(); i++) {
            myFaces.get (i).computeRenderNormal();
         }
         myRenderNormalsValid = true;
      }
   }

   /**
    * Returns true if face normals are currently valid.
    */
   public boolean faceNormalsValid() {
      return myFaceNormalsValid;
   }

   /**
    * {@inheritDoc}
    */
   public RenderProps createRenderProps (HasProperties host) {
      return RenderProps.createMeshProps (host);
   }

   /**
    * Returns true if this mesh is triangular. A mesh is triangular if all its
    * faces are triangles.
    * 
    * @return true if this mesh is triangular
    */
   public boolean isTriangular() {
      updateTriQuadCounts();
      return myNumTriangles == getNumFaces();
   }

   /**
    * Returns true if this is a quad mesh. A mesh is quad if all its faces have
    * four sides.
    * 
    * @return true if this is a quad mesh
    */
   public boolean isQuad() {
      updateTriQuadCounts();
      return myNumQuads == getNumFaces();
   }

   /**
    * Prints out the vertices surrounding this face, in the (clockwise) order
    * order produced by adjacent half-edge traversing.
    */
   private void printIncidentVertices (Vertex3d vtx) {
      HashSet<HalfEdge> edges = new HashSet<HalfEdge>();
      ArrayList<HalfEdge> startingEdges = new ArrayList<HalfEdge>();
      Iterator<HalfEdge> it = vtx.getIncidentHalfEdges();
      while (it.hasNext()) {
         HalfEdge he = it.next();
         if (he.opposite == null) {
            // boundary edge; use this to start a traverse.
            startingEdges.add (he);
         }
         edges.add (he);
      }
      if (startingEdges.isEmpty()) {
         // if no boundary edges, any edge will do to start traverse
         startingEdges.add (vtx.firstIncidentHalfEdge());
      }
      for (int i=0; i<startingEdges.size(); i++) {
         HalfEdge he0 = startingEdges.get(i);
         HalfEdge heNext = null;
         for (HalfEdge he=he0; he != null; he = heNext) {
            edges.remove (he);
            System.out.println (" "+he.tail.getIndex());
            heNext = he.next.opposite;
            if (heNext == he0) {
               break;
            }
         }
      }
      for (HalfEdge he : edges) {
         // print remaining (non-manifold) edges
         System.out.println (" "+he.tail.getIndex()+" *");
      }
   }

   /**
    * Returns true if this mesh is manifold. A mesh is manifold if each edge is
    * adjacent to at most two faces; i.e., there are no redundant half-edges,
    * and all the faces connected to each vertex form a fan.
    * 
    * @return true if this mesh is manifold
    */
   public boolean isManifold() {
      HashSet<Vertex3d> adjacentVertices = new HashSet<Vertex3d>();
      // check to see all faces around each vertex form a fan
      for (int i=0; i<myVertices.size(); i++) {
         Vertex3d vtx = myVertices.get(i);

         Iterator<HalfEdge> it = vtx.getIncidentHalfEdges();
         if (it.hasNext()) {
            int nume = 0;
            HalfEdge he0 = vtx.firstIncidentHalfEdge();            
            adjacentVertices.clear();
            while (it.hasNext()) {
               HalfEdge he = it.next();
               if (adjacentVertices.contains(he.tail)) {
                  // there is more than one edge involving this tail
                  return false;
               }
               adjacentVertices.add (he.tail);
               if (he.opposite == null) {
                  // boundary edge; use this to start fan traverse.
                  // otherwise, any edge will do.
                  he0 = he;
               }
               nume++;
            }
            HalfEdge heNext = he0.next.opposite;
            int cnt = 1;
            while (heNext != null && heNext.head == vtx && heNext != he0) {
               heNext = heNext.next.opposite;
               cnt++;
            }
            if (cnt != nume) {
               // some half edges can't be reached by traverse, so not a fan
               // System.out.println ("vtx " + vtx.getIndex() + " "+nume+" "+cnt);
               return false;
            }
         }
      }
      return true;
   }
   
   /**
    */
   public ArrayList<Vertex3d> findNonManifoldVertices() {
      ArrayList<Vertex3d> nonManifold = new ArrayList<Vertex3d>();
      HashSet<Vertex3d> adjacentVertices = new HashSet<Vertex3d>();
      // check to see all faces around each vertex form a fan
      for (int i=0; i<myVertices.size(); i++) {
         Vertex3d vtx = myVertices.get(i);

         Iterator<HalfEdge> it = vtx.getIncidentHalfEdges();
         if (it.hasNext()) {
            int nume = 0;
            HalfEdge he0 = vtx.firstIncidentHalfEdge();            
            adjacentVertices.clear();
            while (it.hasNext()) {
               HalfEdge he = it.next();
               if (adjacentVertices.contains(he.tail)) {
                  // there is more than one edge involving this tail
                  System.out.println("More than one edge involving tail: " + he.tail.getIndex ());
                  
               }
               adjacentVertices.add (he.tail);
               if (he.opposite == null) {
                  // boundary edge; use this to start fan traverse.
                  // otherwise, any edge will do.
                  he0 = he;
               }
               nume++;
            }
            HalfEdge heNext = he0.next.opposite;
            int cnt = 1;
            while (heNext != null && heNext.head == vtx && heNext != he0) {
               heNext = heNext.next.opposite;
               cnt++;
            }
            if (cnt != nume) {
               // some half edges can't be reached by traverse, so not a fan
               System.out.println ("vtx " + vtx.getIndex() + " has multiple fans: "+nume+" vs "+cnt);
               nonManifold.add (vtx);
            }
         }
      }
      return nonManifold;
   }

   protected int numFaceEdges() {
      if (isTriangular()) {
         return 3;
      }
      else if (isQuad()) {
         return 4;
      }
      else {
         return -1;
      }
   }

   /**
    * Returns this mesh's faces. The faces are contained within an ArrayList,
    * each element of which is of type {@link maspack.geometry.Face Face}.
    * Modifying these elements will modify the mesh.
    * 
    * @return list of this mesh's faces.
    */
   public ArrayList<Face> getFaces() {
      return myFaces;
   }

   /**
    * Returns the number of face in this mesh.
    * 
    * @return number of faces in this mesh
    */
   public int getNumFaces() {
      return myFaces.size();
   }

   public int countEdges() {

      int ne = 0;

      // clear all half-edge visited
      for (Face f : myFaces) {
         HalfEdge he = f.he0;
         do {
            he.clearVisited();
            he = he.next;
         } while (he != f.he0);
      }

      // go through and actually count now
      for (Face f : myFaces) {
         HalfEdge he = f.he0;
         do {
            if (!he.isVisited()) {
               ne++;
               he.setVisited();
               if (he.opposite != null) {
                  he.opposite.setVisited();
               }
            }
            he = he.next;
         } while (he != f.he0);
      }

      return ne;
   }

   public SignedDistanceGrid getSignedDistanceGrid() {
      return sdGrid;
   }

   public SignedDistanceGrid getSignedDistanceGrid (
      double margin,Vector3d cellDivisions) {
      if (cellDivisions == null) {
         sdGrid = new SignedDistanceGrid (this, margin);
      }
      else
         sdGrid = new SignedDistanceGrid (this, margin, cellDivisions);
      return sdGrid;
   }


   // used for computing face normals
   static Vector3d del01 = new Vector3d();
   static Vector3d del02 = new Vector3d();

   /**
    * Creates an empty polygonal mesh.
    */
   public PolygonalMesh() {
      super();
   }

   /**
    * Creates a polygonal mesh and initializes it from an file in Alias
    * Wavefront obj format, as decribed for the method
    * {@link #write(PrintWriter,NumberFormat,boolean)}.
    * 
    * @param fileName
    * name of the file containing the mesh description
    */
   public PolygonalMesh (String fileName) throws IOException {
      this (new File (fileName));
   }

   /**
    * Creates a polygonal mesh and initializes it from an file in Alias
    * Wavefront obj format, as decribed for the method
    * {@link #write(PrintWriter,NumberFormat,boolean)}.
    * 
    * @param file
    * file containing the mesh description
    */
   public PolygonalMesh (File file) throws IOException {
      this();
      read (new BufferedReader (new FileReader (file)));
   }

   public PolygonalMesh (PolygonalMesh old) {
      super();
      for (Vertex3d v : old.myVertices) {
         addVertex (new Vertex3d (new Point3d (v.pnt), v.idx));
      }
      for (Face f : old.myFaces) {
         addFace (f.getVertexIndices());
      }
      setMeshToWorld (old.XMeshToWorld);
   }

   public void setMeshToWorld (RigidTransform3d X) {
      super.setMeshToWorld (X);
      //      if (obbtree != null) {
      //         obbtree.setBvhToWorld (X);
      //      }
      if (myBVTree != null) {
         myBVTree.setBvhToWorld (X);
      }
   }

   //   public void setFromWavefrontReader (WavefrontReader wfr) throws IOException {
   //      setFromWavefrontReader (wfr, null);
   //   }

   //   public void setFromWavefrontReader (WavefrontReader wfr, String groupName)
   //      throws IOException {
   //
   //      if (groupName == null) {
   //         String[] nameList = wfr.getPolyhedralGroupNames();
   //         if (nameList.length > 0) {
   //            groupName = nameList[0];
   //         }
   //         else {
   //            // will result in a null mesh since 'default' not a polyhedral group
   //            groupName = "default";
   //         }
   //      }
   //      if (!wfr.hasGroup (groupName)) {
   //         throw new IllegalArgumentException ("Group '"+groupName+"' unknown");
   //      }
   //      wfr.setGroup (groupName);
   //
   //      ArrayList<Point3d> vtxList = new ArrayList<Point3d>();
   //      int[][] indices = wfr.getLocalFaceIndicesAndVertices (vtxList);
   //
   //      for (int i=0; i<vtxList.size(); i++) {
   //         // add by reference since points have already been copied 
   //         addVertex (vtxList.get(i), /* byReference= */true);
   //      }
   //      if (indices != null) {
   //         for (int k=0; k<indices.length; k++) {
   //            addFace (indices[k]);
   //         }
   //      }
   //      myTextureVertexList = new ArrayList<Vector3d>();
   //      setTextureIndices (
   //         wfr.getLocalTextureIndicesAndVertices (myTextureVertexList));
   //      myNormalList = new ArrayList<Vector3d>();
   //      setNormalIndices (wfr.getLocalNormalIndicesAndVertices (myNormalList));
   //      setName (groupName.equals ("default") ? null : groupName);
   //      if (wfr.getRenderProps() != null) {
   //         setRenderProps (wfr.getRenderProps());
   //      }
   //   }

   /**
    * Reads the contents of this mesh from a Reader. The input is assumed to be
    * supplied in Alias Wavefront obj format, as described for the method
    * {@link #write(PrintWriter,NumberFormat,boolean)}.
    * 
    * @param reader
    * supplied the input description of the mesh
    */
   public void read (Reader reader) throws IOException {
      read (reader, false);
   }

   /**
    * Reads the contents of this mesh from a ReaderTokenizer. The input is
    * assumed to be supplied in Alias Wavefront obj format, as described for
    * the method {@link #write(PrintWriter,NumberFormat,boolean)}.
    * 
    * @param rtok
    * tokenizer supplying the input description of the mesh
    * @param zeroIndexed
    * if true, the index numbering for mesh vertices starts at 0. Otherwise,
    * numbering starts at 1.
    */
   public void read (ReaderTokenizer rtok, boolean zeroIndexed)
      throws IOException {

      clear();
      WavefrontReader wfr = new WavefrontReader(rtok);
      if (zeroIndexed) {
         wfr.setZeroIndexed (true);
      }
      wfr.readMesh (this);
   }

   /**
    * Sets the vertex points and faces associated with this mesh.
    * 
    * @param pnts
    * points from which the vertices are formed
    * @param faceIndices
    * integer arrays giving the indices of each face. Each index should
    * correspond to a particular point in pnts.
    * @throws IllegalArgumentException
    * if a vertex index is out of bounds
    */
   public void set (Point3d[] pnts, int[][] faceIndices) {
      set (pnts, faceIndices, /* byReference= */false);
   }


   /**
    * Adds a face to this mesh. A face is described by indices which specify, in
    * counter-clockwise order, the vertices which describe this face.
    * 
    * @param indices
    * integer array giving the vertex indices of the face. Each index should
    * correspond to a vertex presently associated with this mesh.
    * @throws IllegalArgumentException
    * if a vertex index is out of bounds
    * @return the created Face object
    */
   public Face addFace (int[] indices) {
      Face face = new Face (myFaces.size());
      Vertex3d[] vtxList = new Vertex3d[indices.length];
      for (int i = 0; i < indices.length; i++) {
         int idx = indices[i];
         if (idx < 0 || idx >= myVertices.size()) {
            throw new IllegalArgumentException (
               "Face vertex index "+idx+" out of bounds, face number "+face.idx);
         }
         vtxList[i] = (Vertex3d)myVertices.get (idx);
      }
      face.set (vtxList, indices.length, /* connect= */true);
      myFaces.add (face);
      if (indices.length == 3) {
         myNumTriangles++;
      } else if (indices.length == 4) {
         myNumQuads++;
      }
      // myTriQuadCountsValid = false;
      notifyStructureChanged();
      return face;
   }

   /**
    * Adds a face to this mesh. A face is described by an array of vertices
    * arranged in counter-clockwise order with respect to the face's normal.
    * 
    * @param vertices
    * vertices comprising this faces. Each vertex should be presently contained
    * in this mesh.
    * @throws IllegalArgumentException
    * if any vertices are not contained within this mesh
    * @return the created Face object
    */
   public Face addFace (Vertex3d[] vertices) {
      Face face = new Face (myFaces.size());
      for (int i = 0; i < vertices.length; i++) {
         int idx = vertices[i].getIndex();
         if (idx < 0 || idx >= myVertices.size() ||
            myVertices.get (idx) != vertices[i]) {
            throw new IllegalArgumentException ("Vertex not present in mesh");
         }
      }
      face.set (vertices, vertices.length, /* connect= */true);
      myFaces.add (face);
      if (vertices.length == 3) {
         myNumTriangles++;
      } else if (vertices.length == 4) {
         myNumQuads++;
      }
      //myTriQuadCountsValid = false;
      notifyStructureChanged();
      return face;
   }

   /**
    * Adds a triangular face to this mesh.  The face is described by three
    * vertices arranged in counter-clockwise order with respect to the
    * face's normal.
    * 
    * @param v0 first vertex
    * @param v1 second vertex
    * @param v2 third vertex
    * @throws IllegalArgumentException
    * if any vertices are not contained within this mesh
    * @return the created Face object
    */
   public Face addFace (Vertex3d v0, Vertex3d v1, Vertex3d v2) {
      return addFace (new Vertex3d[] { v0, v1, v2 });
   }

   public Face addFace(Vertex3d v0, Vertex3d v1, Vertex3d v2, Vertex3d v3) {
      return addFace(new Vertex3d[] {v0,v1,v2,v3});
   }

   /**
    * Removes a face from this mesh.
    * 
    * @param face
    * face to remove
    * @return false if the face does not belong to this mesh.
    */
   public boolean removeFace (Face face) {
      
      if (isFastRemoval()) {
         
         int idx = face.getIndex();
         int last = myFaces.size()-1;
         if (idx >= 0 && idx <= last) {
            myFaces.set(idx, myFaces.get(last));
            myFaces.get(idx).setIndex(idx);
            myFaces.remove(last);
            
            int nv = face.numVertices ();
            if (nv == 3) {
               myNumTriangles--;
            } else if (nv == 4) {
               myNumQuads--;
            }
            //myTriQuadCountsValid = false;
            face.disconnect();
            face.setIndex(-1);
            notifyStructureChanged();
            return true;
         } else {
            return false;
         }
         
      } else {
         if (myFaces.remove (face)) {
            face.disconnect();
            // reindex faces which occur after this one
            for (int i=face.getIndex(); i<myFaces.size(); i++) {
               myFaces.get(i).setIndex (i);
            }
            //myTriQuadCountsValid = false;
            int nv = face.numVertices ();
            if (nv == 3) {
               myNumTriangles--;
            } else if (nv == 4) {
               myNumQuads--;
            }
            notifyStructureChanged();
            return true;
         } else {
            return false;
         }
      }
   }
   
   /**
    * Removes a face from this mesh, potentially changing order of faces
    */
   public boolean removeFaceFast (Face face) {

      int idx = face.getIndex();
      int last = myFaces.size()-1;
      if (idx >= 0 && idx <= last) {
         myFaces.set(idx, myFaces.get(last));
         myFaces.get(idx).setIndex(idx);
         myFaces.remove(last);
         int nv = face.numVertices ();
         if (nv == 3) {
            myNumTriangles--;
         } else if (nv == 4) {
            myNumQuads--;
         }
         //myTriQuadCountsValid = false;
         face.disconnect();
         face.setIndex(-1);
         notifyStructureChanged();
         return true;
      } else {
         return false;
      }

   }

   /**
    * Removes a set of faces from this mesh, as indicated by a collection.
    *
    * @param faces Collection of faces to remove
    */
   public boolean removeFaces (Collection<Face> faces) {
      ArrayList<Integer> deleteIdxs = new ArrayList<Integer>();
      for (Face f : faces) {
         if (myFaces.get(f.getIndex()) == f) {
            deleteIdxs.add (f.getIndex());
         }
      }
      if (deleteIdxs.size() > 0) {
         Collections.sort (deleteIdxs);
         ArrayList<Face> newFaceList =
            new ArrayList<Face>(myFaces.size()-deleteIdxs.size());
         int k = 0;
         for (int i=0; i<myFaces.size(); i++) {
            Face f = myFaces.get(i);
            if (i == deleteIdxs.get(k)) {
               // delete f
               while (i == deleteIdxs.get(k) && k < deleteIdxs.size()-1) {
                  k++;
               }
               f.disconnect();
            }
            else {
               f.setIndex (newFaceList.size());
               newFaceList.add (f);
            }
         }
         myFaces = newFaceList;
         myTriQuadCountsValid = false;
         notifyStructureChanged();
         return true;
      }
      else {
         return false;
      }
   }

   //   private void printFaceIndices (Face face) {
   //      int[] idxs = face.getVertexIndices();
   //      for (int i=0; i<idxs.length; i++) {
   //         System.out.print (" "+idxs[i]);
   //      }
   //      System.out.println ("");      
   //   }
   //
   //   private boolean checkForRepeatedIndices (int[] idxs) {
   //      if (idxs[0] == idxs[1] || idxs[0] == idxs[2] || idxs[1] == idxs[2]) {
   //         System.out.println ("repeated indices");
   //         for (int i=0; i<idxs.length; i++) {
   //            System.out.print (" "+idxs[i]);
   //         }
   //         System.out.println ("");         
   //         return true;
   //      }
   //      else {
   //         return false;
   //      }
   //   }          

   private boolean disconnectFaceIfDegenerate (Face face) {

      HalfEdge he = face.firstHalfEdge();
      HalfEdge hn = he.next;
      if (hn.next == he) {
         // face has only two edges, so disconnect

         // this is an unrolled call to face.disconnect(), where we also attach
         // adjacent faces, and set he.opposite and hn.opposite to null, so
         // that when face.disconnect() is called later by removeFaces(), it
         // won't damage adjacent structures
         he.head.removeIncidentHalfEdge (he);
         hn.head.removeIncidentHalfEdge (hn);

         HalfEdge heOpp = he.opposite;
         HalfEdge hnOpp = hn.opposite;

         if (heOpp != null) {
            heOpp.opposite = hnOpp;
            he.opposite = null;
         }
         if (hnOpp != null) {
            hnOpp.opposite = heOpp;
            hn.opposite = null;
         }
         // make sure heOpp and hnOpp are primary opposites
         if (heOpp.isPrimary() == hnOpp.isPrimary()) {
            hnOpp.setPrimary (!hnOpp.isPrimary());
         }
         return true;
      }
      else {
         return false;
      }
   }

   private void collapseEdge (
      HalfEdge he, LinkedList<Face> deadFaces, boolean debug) {

      HalfEdge te = he.opposite; // incident edge for tail, if any

      if (debug) {
         System.out.println ("collapsing "+edgeStr(he));
      }

      Vertex3d head = he.head;
      Vertex3d tail = he.tail;

      HalfEdge hprev = he.face.getPreviousEdge (he);
      HalfEdge hnext = he.next;

      head.removeIncidentHalfEdge (he);
      tail.removeIncidentHalfEdge (hprev);
      head.addIncidentHalfEdge (hprev);
      hprev.next = hnext;
      hprev.head = head;
      if (he.face.he0 == he) {
         he.face.he0 = hprev;
      }

      if (te != null) {
         HalfEdge tprev = te.face.getPreviousEdge(te);
         HalfEdge tnext = te.next;

         tail.removeIncidentHalfEdge (te);
         tprev.next = tnext;
         tnext.tail = head;
         if (te.face.he0 == te) {
            te.face.he0 = tprev;
         }
      }

      // process remaining edges connected to tail
      Iterator<HalfEdge> it = tail.getIncidentHalfEdges();
      while (it.hasNext()) {
         HalfEdge ee = it.next();

         head.addIncidentHalfEdge (ee);
         ee.head = head;
         if (ee.tail == head) {
            throw new InternalErrorException (
               "reassigned tail edge (ee) has head == tail");
         }
         ee.next.tail = head;
         if (ee.next.head == head) {
            throw new InternalErrorException (
               "reassigned tail edge (ee.next) has head == tail");
         }
      }

      if (disconnectFaceIfDegenerate (he.face)) {
         deadFaces.add (he.face);
      }

      if (te != null && disconnectFaceIfDegenerate (te.face)) {
         deadFaces.add (te.face);
      }
   }

   /**
    * Returns true if a half edge is collapsible onto it's head.  This requires
    * that the collapse does not add adjacent vertices to the head that it
    * already posseses.
    */
   private boolean halfEdgeIsCollapsible (HalfEdge he) {

      HashSet<Vertex3d> headVertices = new HashSet<Vertex3d>();

      Vertex3d head = he.head;
      Vertex3d tail = he.tail;
      HalfEdge te = he.opposite; // incident edge for tail, if any
      HalfEdge hprev = he.face.getPreviousEdge (he);
      HalfEdge tnext = (te != null ? te.next : null);

      Iterator<HalfEdge> it = head.getIncidentHalfEdges();
      while (it.hasNext()) {
         Vertex3d vtx = it.next().tail;
         if (vtx != hprev.tail && (tnext == null || tnext.head != vtx)) {
            headVertices.add (vtx);
         }
      }
      it = tail.getIncidentHalfEdges();
      while (it.hasNext()) {
         if (headVertices.contains (it.next().tail)) {
            return false;
         }
      }
      return true;
   }

   public int doMergeCloseVertices (double dsqr) {

      LinkedList<Vertex3d> vertexRemoveList = new LinkedList<Vertex3d>();

      Point3d pnt = new Point3d(); // for recomputing vertex positions
      HashSet<Vertex3d> deadVertices = new HashSet<Vertex3d>();
      LinkedList<Face> deadFaces = new LinkedList<Face>();
      for (int i=0; i<myVertices.size(); i++) {

         //System.out.println ("vertex " + i);

         Vertex3d vtx = myVertices.get(i);
         if (!deadVertices.contains (vtx)) {
            vertexRemoveList.clear();
            Iterator<HalfEdge> it = vtx.getIncidentHalfEdges();
            while (it.hasNext()) {
               HalfEdge he = it.next();
               if (he.lengthSquared() <= dsqr) {
                  vertexRemoveList.add (he.tail);
               }
            }
            boolean debug = false;

            if (!vertexRemoveList.isEmpty()) {
               pnt.set (vtx.pnt);
               int removeCnt = 0;
               int rejectCnt = 0;

               while (!vertexRemoveList.isEmpty()) {
                  Vertex3d vrm = vertexRemoveList.poll();
                  HalfEdge he = vtx.findOppositeHalfEdge (vrm);
                  if (halfEdgeIsCollapsible (he)) {
                     // we can remove this vertex right now
                     collapseEdge (he, deadFaces, debug);                     
                     deadVertices.add (vrm);
                     pnt.add (vrm.pnt);
                     removeCnt++;
                     rejectCnt = 0;
                  }
                  else {
                     // otherwise, put it back on list
                     System.out.println ("Vertex being rejected, i="+i);
                     vertexRemoveList.offer (vrm);
                     rejectCnt++;
                  }
                  // if we haven't removed one vertex after cycling through
                  // the list, we are done
                  if (rejectCnt == vertexRemoveList.size()) {
                     break;
                  }
               }
               // reset vertex position to avergae of removed vertices
               pnt.scale (1/(double)(1+removeCnt));
               vtx.pnt.set (pnt);
               if (!vertexRemoveList.isEmpty()) {
                  System.out.println (
                     "WARNING: could not merge "+vertexRemoveList.size()+
                     " vertices because of topology issues");
               }
            }
         }
      }

      removeVertices (deadVertices);
      removeFaces (deadFaces);
      return deadVertices.size();
   }

   /**
    * Merge vertices that are within a prescribed distance of each other.
    * Setting this distance to zero will cause only duplicate vertices to be
    * merge.
    *
    * The algorithm used by this method determines vertex distances by
    * examining edge lengths, and then collapsing edges whose distances
    * are less than or equal to <code>dist</code>. Thus vertices which
    * are isolated, or not otherwise connected by edges, will be unaffected.
    *
    * <p>The implementation of this algorithm is not perfect.  If the mesh is
    * not closed, and merging results in the alignment of one or more boundary
    * edges, these edges will not be connected.  Moreover, the mesh topology
    * may sometimes may it impossible to merge an adjacent vertex without
    * corrupting the mesh topology; in these cases, merges will not be done and
    * a warning will be printed.
    *
    * @param dist distance within which vertices should be merged.
    */
   public int mergeCloseVertices (double dist) {
      double dsqr = dist*dist;
      int total = 0;
      int nv = 0;
      while ((nv = doMergeCloseVertices (dsqr)) > 0) {
         total += nv;
      }

      return total;
   }                

   private void addFaceVertices (HashMap<Vertex3d,Integer> vertices, Face face) {
      HalfEdge he0 = face.firstHalfEdge();
      HalfEdge he = he0;
      do {
         vertices.put (he.head, he.head.getIndex());
         he = he.getNext();
      }
      while (he != he0);      
   }

   private void collectConnectedComponents (
      HashMap<Vertex3d,Integer> vertices, HashSet<Face> faces, Face face) {

      LinkedList<Face> queue = new LinkedList<Face>();

      faces.add (face);
      addFaceVertices (vertices, face);
      queue.offer (face);
      while (!queue.isEmpty()) {
         Face f = queue.poll();
         HalfEdge he0 = f.firstHalfEdge();
         HalfEdge he = he0;
         do {
            Face opface = (he.opposite != null ? he.opposite.face : null);
            if (opface != null && !faces.contains (opface)) {
               faces.add (opface);
               addFaceVertices (vertices, opface);
               queue.offer (opface);
            }
            he = he.getNext();
         }
         while (he != he0);
      }
   }

   public int numDisconnectedVertices () {
      int num = 0;
      for (int i=0; i<myVertices.size(); i++) {
         Vertex3d v = myVertices.get(i);
         if (v.firstIncidentHalfEdge() == null) {
            num++;
         }
      }
      return num;
   }

   public int removeDisconnectedVertices () {
      ArrayList<Vertex3d> deadVertices = new ArrayList<Vertex3d>();
      for (int i=0; i<myVertices.size(); i++) {
         Vertex3d v = myVertices.get(i);
         if (v.firstIncidentHalfEdge() == null) {
            deadVertices.add (v);
         }
      }
      removeVertices (deadVertices);
      return deadVertices.size();
   }

   private void partitionConnectedComponents (
      ArrayList<HashMap<Vertex3d,Integer>> vertexMaps, 
      ArrayList<HashSet<Face>> faceSets) {

      boolean[] faceIsMarked = new boolean [getNumFaces()];

      for (int i=0; i<myFaces.size(); i++) {
         if (!faceIsMarked[i]) {
            HashMap<Vertex3d,Integer> vertices = new HashMap<Vertex3d,Integer>();
            HashSet<Face> faces = new HashSet<Face>();
            collectConnectedComponents (vertices, faces, myFaces.get(i));
            for (Face f : faces) {
               faceIsMarked[f.getIndex()] = true;
            }
            faceSets.add (faces);
            vertexMaps.add (vertices);
         }
      }
   }

   public int removeDisconnectedFaces () {

      ArrayList<HashMap<Vertex3d,Integer>> vertexMaps =
         new ArrayList<HashMap<Vertex3d,Integer>>();
      ArrayList<HashSet<Face>> faceSets =
         new ArrayList<HashSet<Face>>();

      partitionConnectedComponents (vertexMaps, faceSets);

      if (faceSets.size() > 1) {
         int maxfaces = 0;
         int maxIdx = -1;
         for (int i=0; i<faceSets.size(); i++) {
            if (faceSets.get(i).size() > maxfaces) {
               maxfaces = faceSets.get(i).size();
               maxIdx = i;
            }
         }
         LinkedList<Face> deletedFaces = new LinkedList<Face>();
         LinkedList<Vertex3d> deletedVertices = new LinkedList<Vertex3d>();

         // collect all vertices and faces the need to be deleted
         for (int i=0; i<faceSets.size(); i++) {
            if (i != maxIdx) {
               deletedFaces.addAll (faceSets.get(i));
               deletedVertices.addAll (vertexMaps.get(i).keySet());
            }
         }
         removeFaces (deletedFaces);
         removeVertices (deletedVertices);
         return deletedFaces.size();
      }
      else {
         return 0;
      }
   }

   /**
    * Copies this mesh into a set of connected meshes and returns these as an
    * array. If this mesh is already fully connected, no meshes are produced
    * and <code>null</code> is returned. At present, the copied meshes do not
    * preserve face normal or texture information.
    */
   public PolygonalMesh[] partitionIntoConnectedMeshes() {

      ArrayList<HashMap<Vertex3d,Integer>> vertexMaps =
         new ArrayList<HashMap<Vertex3d,Integer>>();
      ArrayList<HashSet<Face>> faceSets =
         new ArrayList<HashSet<Face>>();

      partitionConnectedComponents (vertexMaps, faceSets);

      int numMeshes = faceSets.size();
      if (numMeshes == 1) {
         return null;
      }
      PolygonalMesh[] meshes = new PolygonalMesh [numMeshes];
      for (int i=0; i<numMeshes; i++) {
         PolygonalMesh mesh = new PolygonalMesh();
         int idx = 0;

         HashMap<Vertex3d,Integer> vmap = vertexMaps.get(i);
         for (Vertex3d vtx : vmap.keySet()) {
            mesh.addVertex (vtx.pnt);
            vmap.put (vtx, idx++);
         }
         for (Face face : faceSets.get(i)) {
            Vertex3d[] faceVtxs = face.getVertices();
            int[] idxs = new int[faceVtxs.length];
            for (int j=0; j<faceVtxs.length; j++) {
               idxs[j] = vmap.get(faceVtxs[j]);
            }
            mesh.addFace (idxs);
         }
         mesh.setMeshToWorld (XMeshToWorld);
         meshes[i] = mesh;
      }
      return meshes;
   }

   private void checkIndexConsistency() {
      for (int i=0; i<myFaces.size(); i++) {
         if (myFaces.get(i).getIndex() != i) {
            throw new InternalErrorException (
               "Face "+i+" has index " + myFaces.get(i).getIndex());
         }
      }
      for (int i=0; i<myVertices.size(); i++) {
         if (myVertices.get(i).getIndex() != i) {
            throw new InternalErrorException (
               "Face "+i+" has index " + myVertices.get(i).getIndex());
         }
      }
   }

   private String edgeStr (HalfEdge he) {
      if (he == null) {
         return "null";
      }
      else {
         return he.tail.getIndex() + "->" + he.head.getIndex();
      }
   }

   /**
    * Make sure that this mesh is consistent, by checking that:
    *
    * (1) vertex and face indices are consistent
    * (2) every face half edge is registered with it's head vertex
    * (3) every half edge's opposite, if non-null, points back to itself
    * (4) every half edge's head is the next half-edge's tail
    * (5) every half edge's face is valid
    * (6) every half edge's head and tail are different
    * (7) every vertex indicent half edge refers to a valid face
    */
   public void checkConsistency() {
      checkIndexConsistency();
      for (int i=0; i<myFaces.size(); i++) {
         Face face = myFaces.get(i);
         HalfEdge he0 = face.firstHalfEdge();
         HalfEdge he = he0;

         do {
            HalfEdge heNext = he.getNext();
            if (he.head != heNext.tail) {
               throw new InternalErrorException (
                  "Face "+i+", edge "+edgeStr(he)+
                  ": head not equal to tail of next edge");
            }
            if (he.head == he.tail) {
               throw new InternalErrorException (
                  "Face "+i+", edge "+edgeStr(he)+
                  ": head and tail are the same");
            }
            if (he.opposite != null) {
               HalfEdge heOpp = he.opposite;
               if (heOpp.opposite != he) {
                  throw new InternalErrorException (
                     "Face "+i+", edge "+edgeStr(he)+
                     ": opposite edge doesn't point back to edge");
               }
               if (heOpp.uOppositeP == he.uOppositeP) {
                  throw new InternalErrorException (
                     "Face "+i+", edge "+edgeStr(he)+
                     ": opposite edge has same uOppositeP setting: " +
                     he.uOppositeP);
               }
               if (heOpp.tail != he.head || heOpp.head != he.tail) {
                  throw new InternalErrorException (
                     "Face "+i+", edge "+edgeStr(he)+
                     ": opposite edge is "+edgeStr(heOpp));
               }
            }
            if (!he.head.hasIncidentHalfEdge (he)) {
               throw new InternalErrorException (
                  "Face "+i+", edge "+edgeStr(he)+
                  ": head is missing incident reference");
            }
            if (he.face != face) {
               throw new InternalErrorException (
                  "Face "+i+", edge "+edgeStr(he)+
                  ": face reference is "+ he.face.getIndex());
            }
            he = heNext;
         }
         while (he != he0);
      }
      for (int i=0; i<myVertices.size(); i++) {
         Vertex3d vtx = myVertices.get(i);
         Iterator<HalfEdge> it = vtx.getIncidentHalfEdges();
         int k = 0;
         while (it.hasNext()) {
            HalfEdge he = it.next();
            Face face = he.face;
            if (myFaces.get(face.getIndex()) != face) {
               throw new InternalErrorException (
                  "Vertex "+i+", incident edge "+k+
                  ": face not contained in mesh");
            }
            k++;
         }
      }
   }

   /**
    * Sets the vertex points and faces associated with this mesh.
    * 
    * @param pnts
    * points from which the vertices are formed
    * @param faceIndices
    * integer arrays giving the indices of each face. Each index should
    * correspond to a particular point in pnts.
    * @param byReference
    * if true, then the supplied points are not copied but instead referred to
    * directly by the vertex structures. The mesh will track any changes to the
    * points and {@link #isFixed isFixed} will return false.
    * @throws IllegalArgumentException
    * if a vertex index is out of bounds
    */
   protected void set (Point3d[] pnts, int[][] faceIndices, boolean byReference) {
      clear();
      for (int i = 0; i < pnts.length; i++) {
         addVertex (pnts[i], byReference);
      }
      for (int k = 0; k < faceIndices.length; k++) {
         try {
            addFace (faceIndices[k]);
         }
         catch (IllegalArgumentException e) {
            clear();
            throw e;
         }
      }
   }
   
   /**
    * Sets the vertex points and faces associated with this mesh.
    * 
    * @param pnts
    * set of values (x,y,z) from which the vertices are formed
    * @param faceIndices
    * integer arrays giving the indices of each face. Each index should
    * correspond to a particular point in pnts.
    * @throws IllegalArgumentException
    * if a vertex index is out of bounds
    */
   public void set(double[][] pnts, int[][] faceIndices) {
      clear();
      for (int i = 0; i < pnts.length; i++) {
         addVertex (pnts[i][0], pnts[i][1], pnts[i][2]);
      }
      for (int k = 0; k < faceIndices.length; k++) {
         try {
            addFace (faceIndices[k]);
         }
         catch (IllegalArgumentException e) {
            clear();
            throw e;
         }
      }
   }

   protected boolean ctrlPntsEqual (Vector4d[] pnts, int base, int inc, int num) {
      for (int i = 1; i < num; i++) {
         int k = base + i * inc;
         if (pnts[base].x != pnts[k].x || pnts[base].y != pnts[k].y ||
            pnts[base].z != pnts[k].z) {
            return false;
         }
      }
      return true;
   }

   protected void collapseIndices (int[] idxGrid, int base, int inc, int num) {
      for (int i = 1; i < num; i++) {
         idxGrid[base + i * inc] = idxGrid[base];
      }
   }

   /**
    * Writes this mesh to a File, using an Alias Wavefront "obj" file
    * format. Behaves the same as {@link
    * #write(java.io.PrintWriter,maspack.util.NumberFormat,boolean,boolean)}
    * with <code>zeroIndexed</code> and <code>facesClockwise</code> set to
    * false.
    * 
    * @param file
    * File to write this mesh to
    * @param fmtStr
    * format string for writing the vertex coordinates. If <code>null</code>,
    * a format of <code>"%.8g"</code> is assumed.
    */
   public void write (File file, String fmtStr) throws IOException {
      if (fmtStr == null) {
         fmtStr = "%.8g";
      }
      NumberFormat fmt = new NumberFormat (fmtStr);
      PrintWriter pw =
         new PrintWriter (new BufferedWriter (new FileWriter (file)));
      write (pw, fmt, /*zeroIndexed=*/false, /*facesClockwise=*/false);
   }

   /**
    * Writes this mesh to a PrintWriter, using an Alias Wavefront "obj" file
    * format. Behaves the same as
    * {@link #write(java.io.PrintWriter,maspack.util.NumberFormat,boolean,boolean)}
    * with <code>facesClockwise</code> set to false.
    * 
    * @param pw
    * PrintWriter to write this mesh to
    * @param fmt
    * format for writing the vertex coordinates. If <code>null</code>,
    * a format of <code>"%.8g"</code> is assumed.
    * @param zeroIndexed
    * if true, index numbering for mesh vertices starts at 0. Otherwise,
    * numbering starts at 1.
    */
   public void write (
      PrintWriter pw, NumberFormat fmt, boolean zeroIndexed) throws IOException {
      write (pw, fmt, zeroIndexed, /*facesClockwise=*/false);
   }


   /**
    * Writes this mesh to a PrintWriter, using an Alias Wavefront "obj" file
    * format. Vertices are printed first, each starting with the letter "v" and
    * followed by x, y, and z coordinates. Faces are printed next, starting with
    * the letter "f" and followed by a list of integers which gives the indices
    * of that face's vertices in counter-clockwise order. For example, a mesh
    * consisting of a simple tetrahedron might be written like this:
    * 
    * <pre>
    *    v 0.0 0.0 0.0
    *    v 1.0 0.0 0.0
    *    v 0.0 1.0 0.0
    *    v 0.0 0.0 1.0
    *    f 1 2 3
    *    f 0 2 1
    *    f 0 3 2
    *    f 0 1 3
    * </pre>
    * 
    * <p>
    * The format used to print vertex coordinates is specified by a
    * {@link maspack.util.NumberFormat NumberFormat}.
    * 
    * @param pw
    * PrintWriter to write this mesh to
    * @param fmt
    * format for writing the vertex coordinates. If <code>null</code>,
    * a format of <code>"%.8g"</code> is assumed.
    * @param zeroIndexed
    * if true, index numbering for mesh vertices starts at 0. Otherwise,
    * numbering starts at 1.
    * @param facesClockwise
    * if true, face indices are written clockwise instead of the
    * usual counter-clockwise convention.
    */
   public void write (
      PrintWriter pw, NumberFormat fmt,
      boolean zeroIndexed, boolean facesClockwise)
         throws IOException {
      if (fmt == null) {
         fmt = new NumberFormat ("%.8g");
      }
      boolean writeTextureInfo =
         myTextureVertexList.size() > 0 && myTextureIndices != null;
         boolean writeNormals =
            myNormalList.size() > 0 && myNormalIndices != null;
            for (Vertex3d vertex : myVertices) {
               Point3d pnt = vertex.pnt;
               pw.println ("v " + fmt.format (pnt.x) + " " + fmt.format (pnt.y) + " " +
                  fmt.format (pnt.z));
            }
            if (writeNormals) {
               for (Vector3d vn : myNormalList) {
                  pw.println ("vn " + fmt.format (vn.x) + " " + fmt.format (vn.y) +
                     " " + fmt.format (vn.z));
               }
            }
            if (writeTextureInfo) {
               for (Vector3d vt : myTextureVertexList) {
                  pw.println ("vt " + fmt.format (vt.x) + " " + fmt.format (vt.y) +
                     " " + fmt.format (vt.z));
               }
            }
            int faceCnt = 0;
            for (Face face : myFaces) {
               int idxCnt = 0;
               pw.print ("f");
               Vertex3d[] vtxList = new Vertex3d[face.numEdges()];
               int k = 0;
               HalfEdge he = face.he0;
               do {
                  vtxList[k++] = he.head;
                  he = he.next;
               }
               while (he != face.he0);
               if (facesClockwise) {
                  // reverse vertex list
                  for (k=1; k<=(vtxList.length-1)/2; k++) {
                     int l = vtxList.length-k;
                     Vertex3d tmp = vtxList[l];
                     vtxList[l] = vtxList[k];
                     vtxList[k] = tmp;
                  }
               }
               for (k=0; k<vtxList.length; k++) {
                  Vertex3d vtx = vtxList[k];
                  if (zeroIndexed) {
                     pw.print (" " + (vtx.idx));
                     if (writeTextureInfo) {
                        pw.print ("/" + (myTextureIndices.get(faceCnt)[idxCnt]));
                     }
                     if (writeNormals) {
                        if (!writeTextureInfo) {
                           pw.print ("/");
                        }
                        pw.print ("/" + (myNormalIndices.get(faceCnt)[idxCnt]));
                     }
                  }
                  else {
                     pw.print (" " + (vtx.idx + 1));
                     if (writeTextureInfo) {
                        pw.print ("/" + (myTextureIndices.get(faceCnt)[idxCnt] + 1));
                     }
                     if (writeNormals) {
                        if (!writeTextureInfo) {
                           pw.print ("/");
                        }
                        pw.print ("/" + (myNormalIndices.get(faceCnt)[idxCnt] + 1));
                     }
                  }
                  idxCnt++;
               }
               pw.println ("");
               faceCnt++;
            }
            pw.flush();
   }

   /* Write the mesh to a file in the .poly format for tetgen. */
   public void writePoly (String nodeString) throws Exception {
      FileOutputStream fos = new FileOutputStream (nodeString);
      PrintStream ps = new PrintStream (new BufferedOutputStream (fos));
      writePoly (ps);
      fos.close();
   }

   public void writePoly (PrintStream ps) throws Exception {
      ArrayList<Vertex3d> verts = myVertices;
      ps.println (verts.size() + " 3 0 0 0");
      for (Vertex3d v : verts) {
         Point3d pnt = v.pnt;
         ps.println (v.idx + " " + pnt.x + " " + pnt.y + " " + pnt.z);
      }
      ArrayList<Face> fcs = myFaces;
      ps.println (fcs.size() + " 1");
      for (Face f : fcs) {
         ps.println ("1 0 1");
         if (f.numVertices() != 3)
            throw new RuntimeException();
         ps.print (f.numVertices());
         HalfEdge he = f.he0;
         do {
            ps.print (" " + (he.head.idx));
            he = he.next;
         }
         while (he != f.he0);
         ps.println ("");
      }
      ps.println (0);
      ps.println (0);
      ps.flush();
   }

   // void updateBvHierarchy() {
   //    if (bvHierarchy != null) {
   //       // SurfaceMeshCollider.collisionMetrics.bvTime -= System.nanoTime();
   //       bvHierarchy.updateBounds();
   //       // SurfaceMeshCollider.collisionMetrics.bvTime += System.nanoTime();
   //       bvHierarchyValid = true;
   //    }
   // }

   //   void updateBVTree() {
   //      if (myBVTree != null) {
   //         // SurfaceMeshCollider.collisionMetrics.bvTime -= System.nanoTime();
   //         myBVTree.update();
   //         // SurfaceMeshCollider.collisionMetrics.bvTime += System.nanoTime();
   //         myBVTreeUpdated = true;
   //      }
   //   }

   /**
    * Applies an affine transformation to the vertices of this mesh. The
    * topology of the mesh remains unchanged.
    * 
    * @param X
    * affine transformation
    */
   public void transform (AffineTransform3dBase X) {
      super.transform (X);
      for (Face f : myFaces) {
         f.updateNormalAndEdges();
      }
      for (Vector3d vn : myNormalList) {
         vn.transform (X);
         vn.normalize();
      }
   }

   /**
    * Applies an inverse affine transformation to the vertices of this mesh. The
    * topology of the mesh remains unchanged.
    * 
    * @param X
    * affine transformation
    */
   public void inverseTransform (AffineTransform3dBase X) {
      super.inverseTransform (X);
      for (Face f : myFaces) {
         f.updateNormalAndEdges();
      }
      for (Vector3d vn : myNormalList) {
         vn.transform (X);
         vn.normalize();
      }
   }

   /**
    * {@inheritDoc}
    */
   public int getRenderHints() {
      int code = 0;
      if (myRenderProps != null && myRenderProps.getAlpha() != 1) {
         code |= TRANSLUCENT;
      }
      return code;
   }

   /**
    * Clears this mesh (makes it empty).
    */
   public void clear() {
      super.clear();
      myFaces.clear();
      myNumTriangles = myNumQuads = 0;
      myTriQuadCountsValid = true;
   }

   public void render (GLRenderer renderer, RenderProps props, int flags) {
      if (myMeshRenderer == null) {
         myMeshRenderer = new MeshRenderer();
      }
      if (isUsingVertexColoring () && ((flags & GLRenderer.SELECTED) == 0)) {
         flags |= GLRenderer.VERTEX_COLORING;
         flags |= GLRenderer.HSV_COLOR_INTERPOLATION;
      }
      myMeshRenderer.render (renderer, this, props, flags);
   }

   /**
    * Generates and returns a subdivided version of this mesh interpolated using
    * cubic patches.
    * 
    * @return The subdivision mesh. Null is returned
    */
   public PolygonalMesh getSubdivisionMesh() {
      return createSubdivisionMesh (subdivisions);
   }

   private PolygonalMesh createSubdivisionMesh (int subdivisions) {
      PolygonalMesh mesh = new PolygonalMesh();

      for (Object faceobject : myFaces) {
         Face face = (Face)faceobject;

         int vertex0 = mesh.myVertices.size();

         double d = 1.0 / (subdivisions + 1);
         for (int i = 0; i <= (subdivisions + 1); i++)
            for (int j = 0; j <= (subdivisions - i + 1); j++) {
               double u = i * d;
               double v = j * d;

               SubdivisionVertex3d vertex =
                  new SubdivisionVertex3d (face, u, v);

               mesh.addVertex (vertex);
            }

         int i0 = vertex0;

         for (int i = 0; i <= subdivisions; i++) {
            int i1 = i0 + (subdivisions + 2 - i);

            for (int j = 0; j <= subdivisions - i - 1; j++) {
               mesh.addFace (new int[] { i0 + j, i1 + j, i0 + j + 1 });
               mesh.addFace (new int[] { i0 + j + 1, i1 + j, i1 + j + 1 });
            }

            mesh.addFace (new int[] { i0 + subdivisions - i,
               i1 + subdivisions - i,
               i0 + subdivisions - i + 1 });

            i0 = i1;
         }
      }

      updateSubdivisionMesh (mesh);

      return mesh;
   }

   /**
    * Updates a subdivision mesh.
    * 
    * @param mesh
    * Must be a mesh generated by calling getSubdivisionMesh on this mesh.
    */
   public void updateSubdivisionMesh (PolygonalMesh mesh) {
      if (mesh != null) {
         TrianglePatch patch = null;
         Face lastFace = null;

         Vector3d[] normals = new Vector3d[myVertices.size()];

         for (int v = 0; v < normals.length; v++) {
            ((Vertex3d)myVertices.get (v)).computeNormal (normals[v] =
               new Vector3d());
         }

         for (Object vertexobject : mesh.myVertices) {
            SubdivisionVertex3d vertex = (SubdivisionVertex3d)vertexobject;

            double w = 1.0 - vertex.u - vertex.v;

            HalfEdge he = vertex.f.he0;
            Vertex3d v0 = he.head;
            he = he.next;
            Vertex3d v1 = he.head;
            Vertex3d v2 = he.next.head;

            Vector3d n0 = normals[v0.idx], n1 = normals[v1.idx], n2 =
               normals[v2.idx];

            if (vertex.f != lastFace)
               patch = new TrianglePatch (v0.pnt, n0, v1.pnt, n1, v2.pnt, n2);
            lastFace = vertex.f;

            vertex.normal.scale (vertex.u, n0);
            vertex.normal.scaledAdd (vertex.v, n1, vertex.normal);
            vertex.normal.scaledAdd (w, n2, vertex.normal);
            vertex.normal.normalize();

            vertex.pnt.scale (vertex.u, v0.pnt);
            vertex.pnt.scaledAdd (vertex.v, v1.pnt, vertex.pnt);
            vertex.pnt.scaledAdd (w, v2.pnt, vertex.pnt);
            patch.interpolate (vertex.pnt, vertex.u, vertex.v, w);
         }
      }
   }

   /**
    * Smoothly interpolate a face using barycentric coordinates.
    * 
    * @param f
    * The face to interpolate.
    * @param u
    * The weighting of the first vertice.
    * @param v
    * The weighting of the second vertice.
    */
   public void interpolate (Point3d p, Face f, double u, double v) {
      HalfEdge he = f.he0;
      Vertex3d v0 = he.head;
      he = he.next;
      Vertex3d v1 = he.head;
      Vertex3d v2 = he.next.head;

      Vector3d n0tmp = new Vector3d(), n1tmp = new Vector3d(), n2tmp =
         new Vector3d();
      v0.computeNormal (n0tmp);
      v1.computeNormal (n1tmp);
      v2.computeNormal (n2tmp);

      TrianglePatch patch =
         new TrianglePatch (v0.pnt, n0tmp, v1.pnt, n1tmp, v2.pnt, n2tmp);

      patch.interpolate (p, u, v, (1.0 - u - v));
   }

   public int getSubdivisions() {
      return subdivisions;
   }

   /**
    * Setting this generates a new mesh therefore any previous meshes are
    * invalidated and should be discarded.
    * 
    * @param subdivisions
    * The number of times to subdivide each triangle.
    */
   public void setSubdivisions (int subdivisions) {
      this.subdivisions = subdivisions;
   }

   private boolean isOpen (HalfEdge he) {
      HalfEdgeNode hen = he.tail.incidentHedges;

      while (hen != null) {
         if (hen.he.head == he.tail && hen.he.tail == he.head)
            return false;
         hen = hen.next;
      }

      return true;
   }

   /**
    * Sets normal indices for this mesh. If <code>indices</code> is not
    * <code>null</code>, it is assumed to supply one set of normal indices per
    * face, each with the same number of indices as the face itself.
    * Otherwise, if <code>indices</code> is <code>null</code>, the normal
    * information is cleared.
    * 
    * @param indices
    * indices is matrix NxV, where N is number of faces and V is number of
    * vertices in one face.
    */
   public void setNormalIndices (int[][] indices) {
      if (indices != null) {
         if (indices.length < myFaces.size()) {
            throw new IllegalArgumentException (
               "indices length " + indices.length +
               " less than number of faces " + myFaces.size());
         }
         for (int i=0; i<myFaces.size(); i++) {
            if (indices[i] == null) {
               System.out.println (
                  "Warning: some faces have normals, others don't; " +
                  "ignoring normals");               
               indices = null;
               break;
            }
         }
      }
      if (indices != null) {
         ArrayList<int[]> newNormalIndices =
            new ArrayList<int[]>(myFaces.size());
            for (int i=0; i<myFaces.size(); i++) {
               int numv = myFaces.get(i).numVertices();
               if (indices[i].length < numv) {
                  throw new IllegalArgumentException (
                     "indices for face " + i + " are insufficient in number");
               }
               newNormalIndices.add (copyWithOffset (indices[i], 0));
            }
            this.myNormalIndices = newNormalIndices;
      }
      else {
         this.myNormalIndices = null;
         this.myNormalList.clear();
      }
   }

   public ArrayList<int[]> getNormalIndices() {
      return this.myNormalIndices;
   }

   /**
    * Computes vertex normals using an area-weighted average of the triangles
    * formed by the half edges incident of each vertex.  The normals and normal
    * indices for this mesh will be reset. One normal will be computed per
    * vertex, except in situations where the vertex has incident half edges
    * that are open or hard, in which case extra normals will be computed.
    */
   public void computeVertexNormals () {

      // Each half edge will be associated with a normal for its head vertex.
      HashMap<HalfEdge,Integer> normalIndexMap = new HashMap<HalfEdge,Integer>();
      myNormalList.clear();
      int nrmIdx = 0;
      for (Vertex3d vtx : myVertices) {
         // Compute the normals for all the half-edges indicent on each vertex.
         // If none of the half-edges are open or hard, then only one normal
         // will be computed for the vertex, and will be shared by all the
         // half-edges. Otherwise, extra normals will be computed for the
         // sub-regions delimited by open or hard edges.
         Iterator<HalfEdge> it = vtx.getIncidentHalfEdges();
         while (it.hasNext()) {
            HalfEdge he = it.next();
            if (he.opposite == null || he.isHard() || !it.hasNext()) {
               // start computing normal from this edge
               Vector3d nrm = new Vector3d();
               he.computeVertexNormal (nrm, /*useRenderNormals=*/false);
               HalfEdge he0 = he;
               do {
                  normalIndexMap.put (he, nrmIdx);
                  he = he.getNext().opposite;
               }
               while (he != null && he != he0 && !he.isHard());
               myNormalList.add (nrm);
               nrmIdx++;
            }
         }
      }

      // Now assign the normal indices for each face. These are the indices of
      // the normals associated with each of the face's half edges.
      ArrayList<int[]> normalIndices = new ArrayList<int[]>();
      for (Face face : myFaces) {
         int[] idxs = new int[face.numEdges()];
         HalfEdge he0 = face.firstHalfEdge();
         HalfEdge he = he0;
         int k = 0;
         do {
            Integer idx = normalIndexMap.get (he);
            if (idx == null) {
               throw new InternalErrorException (
                  "Normal not computed for halfEge on face "+face.getIndex());
            }
            idxs[k++] = idx;
            he = he.getNext();
         }
         while (he != he0);
         normalIndices.add (idxs);
      }
      myNormalIndices = normalIndices;
   }

   /**
    * Sets list of normals
    * 
    * @param normals
    */
   public void setNormalList (ArrayList<Vector3d> normals) {
      myNormalList.clear();
      for (int i=0; i<normals.size(); i++) {
         Vector3d nrm = new Vector3d(normals.get(i));
         nrm.normalize();
         myNormalList.add (nrm);
      }
   }

   public ArrayList<Vector3d> getNormalList() {
      return this.myNormalList;
   }

   /**
    * {@inheritDoc}
    */
   public int getNumNormals() {
      if (myNormalList == null) {
         return 0;
      }
      else {
         return myNormalList.size();
      }
   }

   /**
    * {@inheritDoc}
    */
   public Vector3d getNormal (int idx) {
      if (myNormalList == null) {
         throw new ArrayIndexOutOfBoundsException ("idx="+idx+", size=0");
      }
      else {
         return myNormalList.get(idx);
      }
   }

   /**
    * Sets texture indices for this mesh. If <code>indices</code> is not
    * <code>null</code>, it is assumed to supply one set of texture indices per
    * face, each with the same number of indices as the face itself.
    * Otherwise, if <code>indices</code> is <code>null</code>, the texture
    * information is cleared.
    * 
    * @param indices
    * indices is matrix NxV, where N is number of faces and V is number of
    * vertices in one face.
    */
   public void setTextureIndices (int[][] indices) {
      if (indices != null) {
         if (indices.length < myFaces.size()) {
            throw new IllegalArgumentException (
               "indices length " + indices.length +
               " less than number of faces " + myFaces.size());
         }
         for (int i=0; i<myFaces.size(); i++) {
            if (indices[i] == null) {
               System.out.println (
                  "Warning: some faces have texture coords, others don't; " +
                  "ignoring texture");
               indices = null;
               break;
            }
         }
      }
      if (indices != null) {      
         ArrayList<int[]> newTextureIndices =
            new ArrayList<int[]>(myFaces.size());
            for (int i=0; i<myFaces.size(); i++) {
               int numv = myFaces.get(i).numVertices();
               if (indices[i].length < numv) {
                  throw new IllegalArgumentException (
                     "indices for face " + i + " are insufficient in number");
               }
               newTextureIndices.add (copyWithOffset (indices[i], 0));
            }
            this.myTextureIndices = newTextureIndices; 
      }
      else {
         this.myTextureIndices = null;
         this.myTextureVertexList.clear();
      }
   }

   public ArrayList<int[]> getTextureIndices() {
      return myTextureIndices;
   }

   /**
    * Sets texture mapping vertices.
    * 
    * @param textureVertices
    */
   public void setTextureVertices (ArrayList<Vector3d> textureVertices) {
      this.myTextureVertexList = textureVertices;
   }

   public ArrayList<Vector3d> getTextureVertices() {
      return this.myTextureVertexList;
   }

   /**
    * Extends the first open edge loop found.
    * 
    * @param amount
    * The amount to extend it by.
    */
   public void extendOpenEdges (double amount) {
      // Vertex3d v0;

      HalfEdge he0 = null;
      boolean found = false;

      for (Object fo : myFaces) {
         Face f = (Face)fo;

         he0 = f.he0;
         do {
            if (isOpen (he0)) {
               found = true;
               break;
            }

            he0 = he0.next;
         }
         while (he0 != f.he0);

         if (found)
            break;
      }

      Vector3d i = new Vector3d(), j = new Vector3d();

      HalfEdge he = he0;

      ArrayList<Vertex3d> iverts = new ArrayList<Vertex3d>(), overts =
         new ArrayList<Vertex3d>();

      do {
         HalfEdgeNode hen = he.tail.incidentHedges;

         while (hen != null) {
            if (isOpen (hen.he)) {
               break;
            }

            hen = hen.next;
         }

         he.computeUnitVec (i);
         hen.he.computeUnitVec (j);

         double angleFactor = 1.0 - i.angle (j) / Math.PI;

         i.add (j);
         i.cross (he.face.getNormal());
         i.normalize();

         Point3d p = new Point3d();
         p.scaledAdd (amount * angleFactor, i, he.tail.pnt);
         iverts.add (he.tail);
         overts.add (addVertex (p));

         he = hen.he;
      }
      while (he != he0);

      Vertex3d lastiv = iverts.get (iverts.size() - 1), lastov =
         overts.get (overts.size() - 1);

      for (int v = 0; v < iverts.size(); v++) {
         Vertex3d iv = iverts.get (v), ov = overts.get (v);

         addFace (new int[] { lastiv.idx, iv.idx, ov.idx });
         addFace (new int[] { ov.idx, lastov.idx, lastiv.idx });

         lastiv = iv;
         lastov = ov;
      }
   }

   //   public OBBTree getObbtree() {
   //      if (obbtree == null) {
   //         // MeshCollider.collisionMetrics.bvTime -= System.nanoTime();
   //         obbtree = new OBBTree (this, 2);
   //         obbtree.setBvhToWorld (XMeshToWorld);
   //         // MeshCollider.collisionMetrics.bvTime += System.nanoTime();
   //      }
   //      return obbtree;
   //   }

   public Material getFaceMaterial() {
      return myFaceMaterial;
   }

   public void setFaceMaterial (Material mat) {
      if (myFaceMaterial != mat) {
         myFaceMaterial = mat;
      }
   }

   public Material getBackMaterial() {
      return myBackMaterial;
   }

   public void setBackMaterial (Material mat) {
      if (myBackMaterial != mat) {
         myBackMaterial = mat;
      }
   }

   /**
    * Returns the maximum cosine of the triangle formed from a set of three
    * vertices (specified by index values).
    */
   private double maxCosine (int idx0, int idx1, int idx2) {
      Vector3d u01 = new Vector3d();
      Vector3d u12 = new Vector3d();
      Vector3d u20 = new Vector3d();

      u01.sub (myVertices.get (idx1).pnt, myVertices.get (idx0).pnt);
      u01.normalize();
      u12.sub (myVertices.get (idx2).pnt, myVertices.get (idx1).pnt);
      u12.normalize();
      u20.sub (myVertices.get (idx0).pnt, myVertices.get (idx2).pnt);
      u20.normalize();

      double maxCos = u20.dot (u01);
      double c = u01.dot (u12);
      if (c > maxCos) {
         maxCos = c;
      }
      c = u12.dot (u20);
      if (c > maxCos) {
         maxCos = c;
      }
      return maxCos;
   }

   private int[] bestChord (int[] idxs) {
      if (idxs.length < 3) {
         throw new InternalErrorException ("less than three indices specified");
      }
      else if (idxs.length == 3) {
         return new int[] { 0, 1, 2 };
      }
      else if (idxs.length == 4) {
         double cos301 = maxCosine (idxs[3], idxs[0], idxs[1]);
         double cos012 = maxCosine (idxs[0], idxs[1], idxs[2]);
         if (cos301 < cos012) {
            return new int[] { 3, 0, 1 };
         }
         else {
            return new int[] { 0, 1, 2 };
         }
      }
      else {
         double minCos = Double.POSITIVE_INFINITY;
         int i_min = 0;
         int i_prev, i_next;
         for (int i = 0; i < idxs.length; i++) {
            i_prev = (i == 0 ? idxs.length - 1 : i - 1);
            i_next = (i == idxs.length - 1 ? 0 : i + 1);
            double cos = maxCosine (idxs[i_prev], idxs[i], idxs[i_next]);
            if (cos < minCos) {
               i_min = i;
               minCos = cos;
            }
         }
         i_prev = (i_min == 0 ? idxs.length - 1 : i_min - 1);
         i_next = (i_min == idxs.length - 1 ? 0 : i_min + 1);
         return new int[] { i_prev, i_min, i_next };
      }
   }

   private int[] getChord (int[] chord, int[] indices) {
      int[] cindices = new int[3];
      cindices[0] = indices[chord[0]];
      cindices[1] = indices[chord[1]];
      cindices[2] = indices[chord[2]];
      return cindices;
   }

   private int[] removeIndex (int idx, int[] indices) {
      int[] newIndices = new int[indices.length - 1];
      int k = 0;
      for (int j=0; j<indices.length; j++) {
         if (j != idx) {
            newIndices[k++] = indices[j];
         }
      }
      return newIndices;
   }   

   // checks if face is convex and separates convex/reflex vertices
   private static boolean isFaceConvex(Face face, ArrayList<Vertex3d> convex,
      ArrayList<Vertex3d> reflex) {

      boolean isConvex = true;

      HalfEdge he = face.he0;
      Vertex3d vtx0 = he.head;  he = he.next;
      Vertex3d vtx1 = he.head;  he = he.next;
      Vertex3d vtx2;

      Vector3d v1 = new Vector3d();
      Vector3d v2 = new Vector3d();
      Vector3d normal = face.getNormal();

      HalfEdge heStop = he;
      do {
         vtx2 = he.head;

         v1.sub(vtx0.pnt, vtx1.pnt);
         v2.sub(vtx2.pnt, vtx1.pnt);
         v2.cross(v1);

         // non-convex
         if (v2.dot(normal) < 0) {
            reflex.add(vtx1);
            isConvex = false;
         } else {
            convex.add(vtx1);
         }

         // queue for next
         vtx0 = vtx1;
         vtx1 = vtx2;
         he = he.next;
      } while (he != heStop);

      return isConvex;

   }

   /**
    * Modifies this mesh to ensure that all faces are triangles.
    */
   public void triangulate() {

      updateTriQuadCounts();
      int estNewFaces = 2*(myFaces.size()-myNumTriangles);

      ArrayList<Face> newFaceList =
         new ArrayList<Face>(myNumTriangles+estNewFaces);

      ArrayList<int[]> newFaceIndices = new ArrayList<int[]>(estNewFaces);
      ArrayList<int[]> newNormalIndices = null;
      ArrayList<int[]> newTextureIndices = null;

      if (myTextureIndices != null) {
         newTextureIndices = new ArrayList<int[]>(myNumTriangles+estNewFaces);
      }
      if (myNormalIndices != null) {
         newNormalIndices = new ArrayList<int[]>(myNumTriangles+estNewFaces);
      }

      for (int i=0; i<myFaces.size(); i++) {
         Face face = myFaces.get(i);
         int numEdges = face.numEdges();
         if (numEdges == 3) {
            newFaceList.add (face);
            if (myTextureIndices != null) {
               newTextureIndices.add (myTextureIndices.get(i));
            }
            if (myNormalIndices != null) {
               newNormalIndices.add (myNormalIndices.get(i));
            }
         } else {

            face.disconnect();
            int tidxs[] = null;
            int nidxs[] = null;
            int idxs[] = face.getVertexIndices();
            if (myTextureIndices != null) {
               tidxs = myTextureIndices.get(i);
            }
            if (myNormalIndices != null) {
               nidxs = myNormalIndices.get(i);
            }

            ArrayList<Vertex3d> convex = new ArrayList<Vertex3d>(idxs.length);
            ArrayList<Vertex3d> reflex = new ArrayList<Vertex3d>(idxs.length);
            if (isFaceConvex(face, convex, reflex)) {

               // only works for convex face
               while (idxs.length > 3) {
                  // find the indices of the best chord triangle, add the
                  // corresponding face to the new face list, and remove
                  // the chord from the index set
                  int[] chordIdxs = bestChord (idxs);
                  newFaceIndices.add (getChord (chordIdxs, idxs));
                  idxs = removeIndex (chordIdxs[1], idxs);

                  // add the corresponding chord triangle for the texture and
                  // normal coordinates, if present, and remove the chord from
                  // these indices too
                  if (myTextureIndices != null) {
                     newTextureIndices.add (getChord (chordIdxs, tidxs));
                     tidxs = removeIndex (chordIdxs[1], tidxs);
                  }
                  if (myNormalIndices != null) {
                     newNormalIndices.add (getChord (chordIdxs, nidxs));
                     nidxs = removeIndex (chordIdxs[1], nidxs);
                  }
               }
            } else {

               // resort to ear-clipping
               // System.out.println("Face is not convex, resorting to ear-clipping");

               Vector3d v1 = new Vector3d();
               Vector3d v2 = new Vector3d();
               Vector3d vp = new Vector3d();
               Vector3d normal = face.getNormal();
               int[] chordIdxs = new int[3];

               while (idxs.length > 3) {

                  Vertex3d vtxPrev = myVertices.get(idxs[idxs.length-2]);
                  Vertex3d vtx = myVertices.get(idxs[idxs.length-1]);
                  Vertex3d vtxNext;

                  // search for next ear
                  int earIdx = -1;
                  for (int j=0; j<idxs.length; j++) {
                     vtxNext = myVertices.get(idxs[j]);

                     // only consider if convex
                     v1.sub(vtxPrev.pnt, vtx.pnt);
                     v2.sub(vtxNext.pnt, vtx.pnt);
                     v2.cross(v1);

                     if (v2.dot(normal)>=0) {

                        double dot11 = v1.dot(v1);
                        double dot12 = v1.dot(v2);
                        double dot22 = v2.dot(v2);

                        double dot1p;
                        double dot2p;

                        // loop through reflex vertices and check if inside
                        boolean ear = true;
                        for (Vertex3d vtxr : reflex) {
                           if (vtxr != vtxPrev && vtxr != vtx && vtxr != vtxNext) {

                              // determine barycentric coords
                              vp.sub(vtxr.pnt, vtx.pnt);
                              dot1p = v1.dot(vp);
                              dot2p = v2.dot(vp);

                              // Compute barycentric coordinates
                              double invDenom = 1 / (dot11 * dot22 - dot12 * dot12);
                              double u = (dot22 * dot1p - dot12 * dot2p) * invDenom;
                              double v = (dot11 * dot2p - dot12 * dot1p) * invDenom;

                              // Check if point is in triangle
                              if ( (u >= 0) && (v >= 0) && (u + v < 1) ) {
                                 ear = false;
                                 break;
                              }
                           }
                        }

                        if (ear) {
                           // found an ear, so exit loop
                           earIdx = (j-1+idxs.length) % idxs.length;

                           chordIdxs[2] = j;
                           chordIdxs[1] = earIdx;
                           chordIdxs[0] = (j-2+idxs.length) % idxs.length;
                           break;
                        }
                     }

                     vtxPrev = vtx;
                     vtx = vtxNext;
                  }

                  if (earIdx < 0) {
                     System.err.println("Error: I can't figure out how" +
                        " to split this non-convex face (Index: " + face.getIndex() + ")");
                     earIdx = 0;
                     chordIdxs[0] = idxs.length-1;
                     chordIdxs[1] = 0;
                     chordIdxs[2] = 1;
                  } 

                  // remove face
                  newFaceIndices.add (getChord(chordIdxs, idxs));
                  idxs = removeIndex (chordIdxs[1], idxs);

                  // add the corresponding chord triangle for the texture and
                  // normal coordinates, if present, and remove the chord from
                  // these indices too
                  if (myTextureIndices != null) {
                     newTextureIndices.add (getChord (chordIdxs, tidxs));
                     tidxs = removeIndex (chordIdxs[1], tidxs);
                  }
                  if (myNormalIndices != null) {
                     newNormalIndices.add (getChord (chordIdxs, nidxs));
                     nidxs = removeIndex (chordIdxs[1], nidxs);
                  }


               }

            }

            // last triplet
            newFaceIndices.add (idxs);
            if (myTextureIndices != null) {
               newTextureIndices.add (tidxs);
            }
            if (myNormalIndices != null) {
               newNormalIndices.add (nidxs);
            }
         }
      }

      myFaces = newFaceList;
      // need to reindex these faces because they might have moved around
      for (int i=0; i<myFaces.size(); i++) {
         myFaces.get(i).setIndex (i);
      }
      for (int[] idxs : newFaceIndices) {
         addFace (idxs);
      }

      if (myTextureIndices != null) {
         myTextureIndices = newTextureIndices;
         // for (int[] idxs : newTextureIndices) {
         //    myTextureIndices.add (idxs);
         // }
      }
      if (myNormalIndices != null) {
         myNormalIndices = newNormalIndices;
         // for (int[] idxs : newNormalIndices) {
         //    myNormalIndices.add (idxs);
         // }
      }

      myNumTriangles = myFaces.size();
      myNumQuads = 0;
      myTriQuadCountsValid = true;
      notifyStructureChanged();
      checkIndexConsistency();
   }

   public void setHardEdge (Vertex3d v0, Vertex3d v1, boolean hard) {
      // check all edges attached to v0
      HalfEdge he = null;
      Iterator<HalfEdge> it = v0.getIncidentHalfEdges();
      while (it.hasNext()) {
         he = it.next();
         if (he.getTail() == v1) {
            break;
         }
      }
      if (he == null) {
         it = v1.getIncidentHalfEdges();
         while (it.hasNext()) {
            he = it.next();
            if (he.getTail() == v0) {
               break;
            }
         }
      }
      if (he == null) {
         throw new IllegalArgumentException (
            "Edge between vertices " + v0.getIndex() +
            " " + v1.getIndex() + " not found");
      }
      he.setHard (hard);
      if (he.opposite != null) {
         he.opposite.setHard (hard);
      }
   }

   void computeFaceNormals() {
      for (int i = 0; i < myFaces.size(); i++) {
         myFaces.get (i).computeNormal();
      }
      myFaceNormalsValid = true;
   }

   public double checkFaceNormals() {
      double maxErr = 0;
      Vector3d tmp = new Vector3d();
      for (int i = 0; i < myFaces.size(); i++) {
         Face face = myFaces.get (i);
         face.computeNormal (tmp);
         tmp.transform (XMeshToWorld);
         tmp.sub (face.getWorldNormal());
         double err = tmp.norm();
         if (err > maxErr) {
            maxErr = err;
         }
      }
      return maxErr;
   }

   public int numDegenerateFaces () {
      int num = 0;
      for (Face face : getFaces()) {
         Vector3d nrm = face.getNormal();
         if (nrm.containsNaN()) {
            num++;
         }
      }
      return num;
   }

   public void checkForDegenerateFaces () {

      for (Face face : getFaces()) {
         Vector3d nrm = face.getNormal();
         if (nrm.containsNaN()) {
            System.out.println ("face "+face.getIndex()+" badly formed");
            for (int i=0; i<3; i++) {
               Vertex3d v = face.getVertex(i);
               System.out.println (" " + v + " " + v.pnt + " " +
                  v.numIncidentHalfEdges());
            }

         }
      }
   }       

   /** 
    * Creates a copy of this mesh.
    */
   public PolygonalMesh copy() {
      return (PolygonalMesh)super.copy();
   }

   /** 
    * Creates a copy of this mesh using a specific set of vertices.
    */
   public PolygonalMesh copyWithVertices (ArrayList<? extends Vertex3d> vtxs) {
      PolygonalMesh mesh = new PolygonalMesh();

      if (vtxs.size() < myVertices.size()) {
         throw new IllegalArgumentException (
            "Number of supplied vertices="+
            vtxs.size()+", need "+myVertices.size());
      }
      for (int i=0; i<vtxs.size(); i++) {
         mesh.addVertex (vtxs.get(i));
      }
      mesh.setMeshToWorld (XMeshToWorld);
      for (int i = 0; i < getNumFaces(); i++) {
         mesh.addFace (myFaces.get (i).getVertexIndices());
      }
      for (int i = 0; i < myNormalList.size(); i++) {
         Vector3d vn = new Vector3d (myNormalList.get (i));
         mesh.myNormalList.add (vn);
      }

      if (myNormalIndices != null) {
         mesh.myNormalIndices = new ArrayList<int[]>(myNormalIndices.size());
         for (int i=0; i<myNormalIndices.size(); i++) {
            mesh.myNormalIndices.add (
               copyWithOffset (myNormalIndices.get(i), 0));
         }
      }

      for (int i = 0; i < myTextureVertexList.size(); i++) {
         Vector3d tv = new Vector3d (myTextureVertexList.get (i));
         mesh.myTextureVertexList.add (tv);
      }

      if (myTextureIndices != null) {
         mesh.myTextureIndices = new ArrayList<int[]>(myTextureIndices.size());
         for (int i=0; i<myTextureIndices.size(); i++) {
            mesh.myTextureIndices.add (
               copyWithOffset (myTextureIndices.get(i), 0));
         }
      }

      if (myRenderProps != null) {
         mesh.setRenderProps (myRenderProps);
      }
      else {
         mesh.myRenderProps = null;
      }
      if (myFaceMaterial != null) {
         mesh.setFaceMaterial (new Material (myFaceMaterial));
      }
      if (myBackMaterial != null) {
         mesh.setBackMaterial (new Material (myBackMaterial));
      }
      mesh.setFixed (isFixed());
      mesh.setRenderBuffered (isRenderBuffered());
      mesh.setName(getName());
      return mesh;
   }

   /** 
    * Adds copies of the vertices and faces of another mesh to this mesh.  If
    * the other mesh contains normal information, this will be added as well
    * providing this mesh already contains normal information or is
    * empty. Otherwise, normal information for this mesh will be cleared.
    * The same behavior is observed for texture information.
    * 
    * @param mesh Mesh to be added to this mesh
    */
   public void addMesh (PolygonalMesh mesh) {

      int voff = myVertices.size();
      for (int i = 0; i < mesh.getNumVertices(); i++) {
         Point3d p = mesh.getVertices().get(i).getPosition();
         //	 p.transform(X);
         addVertex(p);
      }

      if (mesh.myNormalIndices != null &&
         (myNormalIndices != null || myFaces.size() == 0)) {
         int vnoff = myNormalList.size();
         for (int i=0; i<mesh.myNormalList.size(); i++) {
            Vector3d vn = new Vector3d (mesh.myNormalList.get (i));
            myNormalList.add (vn);
         }
         for (int i=0; i<mesh.myNormalIndices.size(); i++) {
            if (myNormalIndices == null) {
               myNormalIndices = new ArrayList<int[]>();
            }
            myNormalIndices.add (
               copyWithOffset (mesh.myNormalIndices.get(i), vnoff));
         }
      }
      else {
         setNormalIndices (null);
      }

      if (mesh.myTextureIndices != null &&
         (myTextureIndices != null || myFaces.size() == 0)) {
         int vtoff = myTextureVertexList.size();
         for (int i=0; i<mesh.myTextureVertexList.size(); i++) {
            Vector3d vt = new Vector3d (mesh.myTextureVertexList.get (i));
            myTextureVertexList.add (vt);
         }
         if (myTextureIndices == null) {
            myTextureIndices = new ArrayList<int[]>();
         }
         for (int i=0; i<mesh.myTextureIndices.size(); i++) {
            myTextureIndices.add (
               copyWithOffset (mesh.myTextureIndices.get(i), vtoff));
         }
      }
      else {
         setTextureIndices (null);
      }

      // EDIT: Sanchez, May 2012
      // changed order to allow texture indices to be copied over
      for (int i = 0; i < mesh.getNumFaces(); i++) {
         addFace (copyWithOffset (
            mesh.getFaces().get(i).getVertexIndices(), voff));
      }
   }

   private final double SQR (double x) {
      return x * x;
   }

   private final double CUBE (double x) {
      return x * x * x;
   }

   /**
    * Computes the volume of this mesh, on the assumption that it is manifold
    * and closed. The code for this was taken from vclip, by Brian Mirtich. See
    * "Fast and Accurate Computation of Polyhedral Mass Properties," Brian
    * Mirtich, journal of graphics tools, volume 1, number 2, 1996.
    * 
    * @return closed volume of the mesh
    */
   public double computeVolume() {
      return computeVolumeIntegrals (null, null, null);
   }

   /**
    * Computes the surface area of this mesh.
    * 
    * @return surface area of the mesh
    */
   public double computeArea() {
      double area = 0;
      for (Face face : getFaces()) {
         Vector3d nrm = face.getNormal();
         if (!nrm.containsNaN()) {
            area += face.computeArea();
         }
      }
      return area;
   }

   /**
    * Computes the average edge length for this mesh.
    * 
    * @return average edge length of the mesh
    */
   public double computeAverageEdgeLength() {
      double len = 0;
      int nume =0;
      Vector3d del = new Vector3d();
      for (Face face : getFaces()) {
         HalfEdge he = face.he0;
         do {
            len += he.head.pnt.distance (he.tail.pnt);
            nume++;
            he = he.next;
         }
         while (he != face.he0);
      }
      return len/nume;
   }

   /**
    * Returns true if the mesh is closed. This is determined by checking that
    * all edges have a corresponding opposite edge. The mesh also needs to be
    * manifold.
    * 
    * @return true if the mesh is closed
    */
   public boolean isClosed() {
      if (!isManifold()) {
         return false;
      }
      for (Face face : myFaces) {
         HalfEdge he = face.he0;
         do {
            if (he.opposite == null) {
               return false;
            }
            if (he.opposite.face == null)
               return false;
            he = he.next;
         }
         while (he != face.he0);
      }
      return true;
   }

   public ArrayList<Face> findBorderFaces() {
      ArrayList<Face> border = new ArrayList<Face>();
      for (Face face : myFaces) {
         HalfEdge he = face.he0;
         do {
            if (he.opposite == null) {
               if (!border.contains(face)) {
                  border.add(face);
               }
            } else if (he.opposite.face == null) {
               if (!border.contains(face)) {
                  border.add(face);
               }
            }
            he = he.next;
         }
         while (he != face.he0);
      }
      return border;
   }

   public ArrayList<HalfEdge> findBorderEdges() {
      ArrayList<HalfEdge> border = new ArrayList<HalfEdge>();

      for (Face face : myFaces) {
         HalfEdge he = face.he0;
         do {
            if (he.opposite == null) {
               if (!border.contains(he)) {
                  border.add(he);
               }
            } else if (he.opposite.face == null) {
               if (!border.contains(he)) {
                  border.add(he);
               }
            }
            he = he.next;
         }
         while (he != face.he0);
      }
      return border;
   }

   /**
    * Computes the volume integrals of this mesh, on the assumption that it is
    * manifold and closed. The code for this was taken from vclip, by Brian
    * Mirtich. See "Fast and Accurate Computation of Polyhedral Mass
    * Properties," Brian Mirtich, journal of graphics tools, volume 1, number 2,
    * 1996.
    * 
    * @param mov1
    * if non-null, returns the first moment of volume
    * @param mov2
    * if non-null, returns the second moment of volume
    * @param pov
    * if non-null, returns the product of volume
    * @return closed volume of the mesh
    */
   public double computeVolumeIntegrals (
      Vector3d mov1, Vector3d mov2, Vector3d pov) {
      int a, b, c;
      // Edge e;
      // Face f;
      double a0, a1, da;
      double b0, b1, db; //al 
      double a0_2, a0_3, a0_4, b0_2, b0_3, b0_4;
      double a1_2, a1_3, b1_2, b1_3;
      double d, na, nb, nc, inv;
      double I, Ia, Ib, Iaa, Iab, Ibb, Iaaa, Iaab, Iabb, Ibbb;
      double Icc, Iccc, Ibbc, Icca;
      double C0, Ca, Caa, Caaa, Cb, Cbb, Cbbb;
      double Cab, Kab, Caab, Kaab, Cabb, Kabb;
      Vector3d v; //h, w;
      //Point3d cov;
      //RigidTransform3d X;

      if (mov1 != null) {
         mov1.setZero();
      }
      if (mov2 != null) {
         mov2.setZero();
      }
      if (pov != null) {
         pov.setZero();
      }

      //double rad_ = 0;
      double vol_ = 0.0;

      for (int i = 0; i < myFaces.size(); i++) {
         Face f = myFaces.get (i);

         // compute projection direction
         Vector3d nrml = f.getNormal();

         if (nrml.containsNaN()) {
            //System.out.println ("Warning: PolygonalMesh.computeVolumeIntegrals: face "+i+" is badly formed");
            // sanity check for badly formed meshes
            continue;
         }

         v = new Vector3d();
         v.set (Math.abs (nrml.x), Math.abs (nrml.y), Math.abs (nrml.z));
         c = (v.x >= v.y) ? ((v.x >= v.z) ? 0 : 2) : ((v.y >= v.z) ? 1 : 2);
         a = (c + 1) % 3;
         b = (c + 2) % 3;

         I = Ia = Ib = Iaa = Iab = Ibb = Iaaa = Iaab = Iabb = Ibbb = 0.0;

         // walk around face
         HalfEdge he0 = f.firstHalfEdge();
         HalfEdge he = he0;
         do {
            a0 = he.getTail().pnt.get (a);
            b0 = he.getTail().pnt.get (b);
            a1 = he.getHead().pnt.get (a);
            b1 = he.getHead().pnt.get (b);

            da = a1 - a0;

            db = b1 - b0;
            a0_2 = a0 * a0;
            a0_3 = a0_2 * a0;
            a0_4 = a0_3 * a0;
            b0_2 = b0 * b0;
            b0_3 = b0_2 * b0;
            b0_4 = b0_3 * b0;
            a1_2 = a1 * a1;
            a1_3 = a1_2 * a1;
            b1_2 = b1 * b1;
            b1_3 = b1_2 * b1;
            C0 = a1 + a0;
            Ca = a1 * C0 + a0_2;
            Caa = a1 * Ca + a0_3;
            Caaa = a1 * Caa + a0_4;
            Cb = b1 * (b1 + b0) + b0_2;
            Cbb = b1 * Cb + b0_3;
            Cbbb = b1 * Cbb + b0_4;
            Cab = 3 * a1_2 + 2 * a1 * a0 + a0_2;
            Kab = a1_2 + 2 * a1 * a0 + 3 * a0_2;
            Caab = a0 * Cab + 4 * a1_3;
            Kaab = a1 * Kab + 4 * a0_3;
            Cabb = 4 * b1_3 + 3 * b1_2 * b0 + 2 * b1 * b0_2 + b0_3;
            Kabb = b1_3 + 2 * b1_2 * b0 + 3 * b1 * b0_2 + 4 * b0_3;
            I += db * C0;
            Ia += db * Ca;
            Iaa += db * Caa;
            Iaaa += db * Caaa;
            Ib += da * Cb;
            Ibb += da * Cbb;
            Ibbb += da * Cbbb;
            Iab += db * (b1 * Cab + b0 * Kab);
            Iaab += db * (b1 * Caab + b0 * Kaab);
            Iabb += da * (a1 * Cabb + a0 * Kabb);
            he = he.getNext();
         }
         while (he != he0);

         I /= 2.0;
         Ia /= 6.0;
         Iaa /= 12.0;
         Iaaa /= 20.0;
         Ib /= -6.0;
         Ibb /= -12.0;
         Ibbb /= -20.0;
         Iab /= 24.0;
         Iaab /= 60.0;
         Iabb /= -60.0;

         d = -nrml.dot (f.firstHalfEdge().getHead().pnt);

         na = nrml.get (a);
         nb = nrml.get (b);
         nc = nrml.get (c);
         inv = 1.0 / nc;

         if (a == 0) {
            vol_ += inv * na * Ia;
         }
         else if (b == 0) {
            vol_ += inv * nb * Ib;
         }
         else {
            vol_ -= ((d * I + na * Ia + nb * Ib) / nc);
         }
         if (vol_ != vol_) {
            throw new NumericalException ("nrml=" + nrml + ", face " + i);
         }

         Icc =
            (SQR (na) * Iaa + 2 * na * nb * Iab + SQR (nb) * Ibb + d
               * (2 * (na * Ia + nb * Ib) + d * I))
               * SQR (inv);

         if (mov1 != null) {
            mov1.set (a, mov1.get (a) + inv * na * Iaa);
            mov1.set (b, mov1.get (b) + inv * nb * Ibb);
            mov1.set (c, mov1.get (c) + Icc);
         }

         Iccc =
            -(CUBE (na) * Iaaa + 3 * SQR (na) * nb * Iaab + 3 * na * SQR (nb)
               * Iabb + CUBE (nb) * Ibbb + 3
               * (SQR (na) * Iaa + 2 * na * nb * Iab + SQR (nb) * Ibb) * d + d * d
               * (3 * (na * Ia + nb * Ib) + d * I))
               * CUBE (inv);
         if (mov2 != null) {
            mov2.set (a, mov2.get (a) + inv * na * Iaaa);
            mov2.set (b, mov2.get (b) + inv * nb * Ibbb);
            mov2.set (c, mov2.get (c) + Iccc);
         }

         Ibbc = -(d * Ibb + na * Iabb + nb * Ibbb) * inv;
         Icca =
            (SQR (na) * Iaaa + 2 * na * nb * Iaab + SQR (nb) * Iabb + d
               * (2 * (na * Iaa + nb * Iab) + d * Ia))
               * SQR (inv);

         if (pov != null) {
            pov.set (c, pov.get (c) + inv * na * Iaab);
            pov.set (a, pov.get (a) + inv * nb * Ibbc);
            pov.set (b, pov.get (b) + Icca);
         }
      }

      if (mov1 != null) {
         mov1.scale (0.5);
      }
      if (mov2 != null) {
         mov2.scale (1.0 / 3.0);
      }
      if (pov != null) {
         pov.scale (0.5);
      }

      return vol_;
   }
   
   public RigidTransform3d computePrincipalAxes() {
      return computePrincipalAxes(this);
   }
   
   public static RigidTransform3d computePrincipalAxes(PolygonalMesh mesh) {

      Vector3d mov1 = new Vector3d();
      Vector3d mov2 = new Vector3d();
      Vector3d pov = new Vector3d();

      double vol = mesh.computeVolumeIntegrals(mov1, mov2, pov);
      double mass = vol;

      Point3d cov = new Point3d();
      cov.scale(1.0 / vol, mov1); // center of volume

      // [c], skew symmetric
      Matrix3d covMatrix = new Matrix3d(
         0, -cov.z, cov.y,
         cov.z, 0, -cov.x,
         -cov.y, cov.x, 0);
      // J
      Matrix3d J = new Matrix3d(
         (mov2.y + mov2.z), -pov.z, -pov.y,
         -pov.z, (mov2.x + mov2.z), -pov.x,
         -pov.y, -pov.x, (mov2.x + mov2.y));
      
      // Jc = J + m[c][c]
      Matrix3d Jc = new Matrix3d();
      Jc.mul(covMatrix, covMatrix);
      Jc.scale(mass);
      Jc.add(J);

      // Compute eigenvectors and eigenvlaues of Jc
      SymmetricMatrix3d JcSymmetric = new SymmetricMatrix3d(Jc);
      Vector3d lambda = new Vector3d();
      Matrix3d U = new Matrix3d();
      JcSymmetric.getEigenValues(lambda, U);

      // Construct the rotation matrix
      RotationMatrix3d R = new RotationMatrix3d();
      R.set(U);

      lambda.absolute();

      if (lambda.x > lambda.y && lambda.z > lambda.y) {
         R.rotateZDirection(new Vector3d(R.m01, R.m11, R.m21));
      } else if (lambda.x > lambda.z && lambda.y > lambda.z) {
         R.rotateZDirection(new Vector3d(R.m00, R.m10, R.m20));
      }

      return (new RigidTransform3d(cov, R));
   }

   // public AjlBvTree getBvHierarchy() {
   //    if (bvHierarchy == null) {
   //       if (isFixed) {
   //          bvHierarchy = new AjlObbTree();
   //          bvHierarchy.set (this, 2);
   //       }
   //       else {
   //          bvHierarchy = new AjlAabbTree();
   //          bvHierarchy.set (this, 2);
   //       }
   //    }
   //    if (!bvHierarchyValid) {
   //       if (!(bvHierarchy instanceof AjlObbTree)) {
   //          updateBvHierarchy();
   //       }
   //       else {
   //          bvHierarchyValid = true;
   //       }
   //    }
   //    return bvHierarchy;
   // }

   public BVTree getBVTree() {
      if (myBVTree == null) {
         if (isFixed) {
            myBVTree = new OBBTree (this, 2);
         }
         else {
            myBVTree = new AABBTree (this);
         }
         myBVTree.setBvhToWorld (XMeshToWorld);
         myBVTreeUpdated = true;
      }
      else if (!myBVTreeUpdated) {
         myBVTree.update();
         myBVTreeUpdated = true;
      }
      return myBVTree;
   }

   public void clearBVTree() {
      myBVTree = null;
   }

   // Debug methods used by Andrew Larkin's collison code
   static void writeDoubleValue(PrintStream ps, double x1) {
      double x = x1;
      if (x<0) {
         ps.print("- ");
         x = -x;
      }
      Double d = new Double(x);
      long i = Double.doubleToRawLongBits(d);
      for (int k=1; k<=8; k++) {
         long j = i % 256;
         ps.print(j+" ");
         i = i / 256;
      }
   }

   // Debug methods used by Andrew Larkin's collison code
   public void writeWorld (PrintStream ps) throws IOException { 
      ps.println (myVertices.size()+" vertices");
      HashMap<Vertex3d, Integer> map = new HashMap<Vertex3d, Integer>(); 
      int k=0;
      for (Iterator<Vertex3d> it=myVertices.iterator(); it.hasNext();) {
         Vertex3d vert = (Vertex3d)it.next();
         if (map.get(vert) != null) {
            throw new RuntimeException("duplicate vertex");
         }
         map.put(vert, new Integer(++k));
         Point3d pnt = vert.getWorldPoint();
         writeDoubleValue(ps, pnt.x);
         writeDoubleValue(ps, pnt.y);
         writeDoubleValue(ps, pnt.z);
         ps.println (pnt.x+" "+pnt.y+" "+pnt.z);
      }
      ps.println (myFaces.size()+" faces");
      for (Iterator<Face> it=myFaces.iterator(); it.hasNext(); ) {
         Face face = (Face)it.next();
         HalfEdge he = face.he0;
         do {
            ps.print (map.get(he.head)+" ");
            he = he.next;
         } while (he != face.he0);
         ps.println ("");
      }
      ps.flush();
   }

   // Debug methods used by Andrew Larkin's collison code
   public void dumpToFile(String str) {
      FileOutputStream fos;
      try {
         fos = new FileOutputStream (str);
         writeWorld(new PrintStream(new BufferedOutputStream (fos)));
         fos.close();
      } catch (FileNotFoundException e) {
         e.printStackTrace();
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   /**
    * Returns the order of sorted faces, or null if faces have never been 
    * sorted.
    */
   public int[] getFaceOrder() {
      return myFaceOrder;
   }

   /**
    * "Sorts" faces according to the direction provided.  Note that the order of
    * faces is not actually changed.  Instead, an index array is created that
    * holds the sorted order.
    */
   public int[] sortFaces(Vector3d zdir) {

      ZOrderComparator zComparator = new ZOrderComparator(zdir);
      ListIndexComparator<Face> faceComparator = 
         new ListIndexComparator<Face>(myFaces, zComparator);

      Integer[] idxs = faceComparator.createIndexArray();
      Arrays.sort(idxs, faceComparator);
      myFaceOrder = new int[idxs.length];

      // unbox
      for (int i=0; i<idxs.length; i++) {
         myFaceOrder[i] = idxs[i];
      }

      return myFaceOrder;
   }

   // used for sorting faces for transparency
   private static class ZOrderComparator implements Comparator<Face> {

      Vector3d myDir = new Vector3d(0,1,0);
      Vector3d pdisp = new Vector3d();
      Point3d centroid1 = new Point3d();
      Point3d centroid2 = new Point3d();
      
      public ZOrderComparator(Vector3d zDir) {
         myDir.set(zDir);
      }

      public int compare(Face o1, Face o2) {
         o1.computeCentroid(centroid1);
         o2.computeCentroid(centroid2);
         pdisp.sub(centroid1,centroid2);
         double d = pdisp.dot(myDir);
         if (d > 0) {
            return 1;
         } else if (d < 0) {
            return -1;
         }
         return 0;
      }

   }

   private boolean vectorListsEqual (
      ArrayList<Vector3d> list0, ArrayList<Vector3d> list1, double eps) {

      if (list0.size() != list1.size()) {
         return false;
      }
      for (int i=0; i<list0.size(); i++) {
         if (!list0.get(i).epsilonEquals (list1.get(i), eps)) {
            return false;
         }
      }
      return true;
   }

   private boolean indexListsEqual (
      ArrayList<int[]> list0, ArrayList<int[]> list1) {

      if ((list0 != null) != (list1 != null)) {
         return false;
      }
      if (list0 != null) {
         if (list0.size() != list1.size()) {
            return false;
         }
         for (int i=0; i<list0.size(); i++) {
            int[] idxs0 = list0.get(i);
            int[] idxs1 = list1.get(i);
            if ((idxs0 != null) != (idxs1 != null)) {
               return false;
            }
            if (idxs0 != null) {
               if (!ArraySupport.equals (idxs0, idxs1)) {
                  return false;
               }
            }
         }
      }
      return true;
   }

   /**
    * Tests to see if a mesh equals this one. The meshes are equal if they are
    * both PolygonalMeshes, and their transforms, vertices, faces, normals
    * and texture coordinates are equal (within <code>eps</code>).
    */
   public boolean epsilonEquals (MeshBase base, double eps) {
      if (!(base instanceof PolygonalMesh)) {
         return false;
      }
      if (!super.epsilonEquals (base, eps)) {
         System.out.println ("base unequal");
         return false;
      }
      PolygonalMesh mesh = (PolygonalMesh)base;
      if (myFaces.size() != mesh.myFaces.size()) {
         System.out.println ("face sizes unequal");
         return false;
      }
      for (int i=0; i<myFaces.size(); i++) {
         int[] idxs0 = myFaces.get(i).getVertexIndices();
         int[] idxs1 = mesh.myFaces.get(i).getVertexIndices();
         if (!ArraySupport.equals (idxs0, idxs1)) {
            System.out.println ("faces unequal");
            return false;
         }
      }
      if (!vectorListsEqual (myNormalList, mesh.myNormalList, eps)) {
         System.out.println ("here1");
         return false;
      }
      if (!indexListsEqual (myNormalIndices, mesh.myNormalIndices)) {
         System.out.println ("here2");
         return false;
      }
      if (!vectorListsEqual (myTextureVertexList, mesh.myTextureVertexList,eps)) {

         return false;
      }
      if (!indexListsEqual (myTextureIndices, mesh.myTextureIndices)) {
         return false;
      }
      return true;
   }

   /**
    * Computes a spatial inertia for volume defined by this mesh, assuming a
    * constant density. It is assumed that the mesh is closed, although small
    * holes in the mesh should not affect the calculation that much.  Is is
    * also assumed that the faces are oriented counter-clockwise about their
    * outward-facing normal.
    *
    * The code for this was taken from vclip, by Brian Mirtich. See "Fast and
    * Accurate Computation of Polyhedral Mass Properties," Brian Mirtich,
    * journal of graphics tools, volume 1, number 2, 1996.
    *
    * @param M returns the computed spatial inertia
    * @param density
    * density of the volume
    * @return the volume of the mesh
    */
   public double computeInertia (SpatialInertia M, double density) {
      Vector3d mov1 = new Vector3d();
      Vector3d mov2 = new Vector3d();
      Vector3d pov = new Vector3d();

      double vol = computeVolumeIntegrals (mov1, mov2, pov);

      Point3d cov = new Point3d();
      cov.scale (1.0 / vol, mov1); // center of volume

      double mass = density*vol;
      SymmetricMatrix3d J =
         new SymmetricMatrix3d (
            mov2.y+mov2.z, mov2.x+mov2.z, mov2.x+mov2.y, -pov.z, -pov.y, -pov.x); 
      J.scale (density);
      // J contains the mass[com][com] term; remove this:
      J.m00 -= mass * (cov.z * cov.z + cov.y * cov.y);
      J.m11 -= mass * (cov.z * cov.z + cov.x * cov.x);
      J.m22 -= mass * (cov.y * cov.y + cov.x * cov.x);
      J.m01 += mass * cov.x * cov.y;
      J.m02 += mass * cov.x * cov.z;
      J.m12 += mass * cov.z * cov.y;
      J.m10 = J.m01;
      J.m20 = J.m02;
      J.m21 = J.m12;

      M.set (mass, J, cov);
      return vol;
   }
   
   /**
    * Computes the centre of volume of the mesh
    */
   public double computeCentreOfVolume (Point3d c) {
      Vector3d mov1 = new Vector3d();
      Vector3d mov2 = new Vector3d();
      Vector3d pov = new Vector3d();

      double vol = computeVolumeIntegrals (mov1, mov2, pov);
      c.scale (1.0 / vol, mov1); // center of volume

      return vol;
   }



   /**
    * Creates a spatial inertia for the volume defined by this mesh, assuming a
    * constant density. It is assumed that the mesh is closed, although small
    * holes in the mesh should not affect the calculation that much.  Is is
    * also assumed that the faces are oriented counter-clockwise about their
    * outward-facing normal.
    * 
    * @param density
    * density of the volume
    */
   public SpatialInertia createInertia (double density) {
      SpatialInertia M = new SpatialInertia();
      computeInertia (M, density);
      return M;
   }


   public boolean isBorderVertex(Vertex3d vtx) {

      Iterator<HalfEdge> hit = vtx.getIncidentHalfEdges();
      while (hit.hasNext()) {
         HalfEdge he = hit.next();
         if (he.opposite == null) {
            return true;
         }
      }

      return false;
   }

   private void vertexCrawl(Vertex3d vtx) {

      if (vtx.isVisited()) {
         return;
      }

      vtx.setVisited();

      // loop through umbrella(s) of vertices
      Iterator<HalfEdge> hit = vtx.getIncidentHalfEdges();
      while (hit.hasNext()) {
         HalfEdge he = hit.next();
         if (!he.tail.isVisited()) {
            vertexCrawl(he.tail);
         }
         HalfEdge next = he.next;
         if (next.opposite == null && !next.head.isVisited()) {
            vertexCrawl(next.head);
         }
      }


   }

   public int countConnected() {

      int nc = 0;
      for (Vertex3d vtx : myVertices ) {
         vtx.clearVisited();
      }


      for (Vertex3d vtx : myVertices) {
         if (!vtx.isVisited()) {
            nc++;
            // loop around, visiting all vertices
            vertexCrawl(vtx);
         }

      }

      return nc;
   }

   private void borderCrawl(HalfEdge he) {

      while (!he.isVisited()) {
         he.setVisited();

         // loop to next border
         he = he.next;
         while (he.opposite != null) {
            he = he.opposite;
            he = he.next;
         }
      }

   }

   public int countBorders() {

      // clear border HE visited
      // set non-border visited
      boolean opened = false;
      for (Face f : myFaces) {
         HalfEdge he = f.he0;
         do {

            if (he.opposite == null) {
               he.clearVisited();
               opened = true;
            } else {
               he.setVisited();
            }
            he = he.next;
         } while (he != f.he0);
      }

      if (!opened) {
         return 0;
      }

      int nb = 0;
      for (Vertex3d vtx : myVertices) {

         Iterator<HalfEdge> hit = vtx.getIncidentHalfEdges();
         while (hit.hasNext()) {

            HalfEdge he = hit.next();
            if (!he.isVisited()) {
               // we know it's a border
               borderCrawl(he);
               nb++;
            }
         }
      }

      return nb;
   }

   public double countGenus() {
      double c = countConnected();
      double v = getNumVertices();
      double f = getNumFaces();
      double e = countEdges();
      double b = countBorders();

      double g = c-(v+f-e+b)/2;

      return g;
   }

   public void flip() {
      for (Face f : myFaces) {
         f.flip(true);
      }
   }

}
