/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import maspack.geometry.io.WavefrontReader;
import maspack.geometry.io.WavefrontWriter;
import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.Matrix3d;
import maspack.matrix.NumericalException;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.SymmetricMatrix3d;
import maspack.matrix.Vector3d;
import maspack.matrix.Vector2d;
import maspack.matrix.Vector3i;
import maspack.matrix.Vector4d;
import maspack.matrix.VectorTransformer3d;
import maspack.properties.HasProperties;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.spatialmotion.SpatialInertia;
import maspack.util.ArraySupport;
import maspack.util.InternalErrorException;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;

/**
 * Implements a polygonal mesh consisting of vertices, faces, and half-edges.
 */
public class PolygonalMesh extends MeshBase {

   protected ArrayList<Face> myFaces = new ArrayList<Face>();

   protected boolean myMultiAutoNormalsP = true;

   protected boolean myTriQuadCountsValid = false;
   protected int myNumTriangles;
   protected int myNumQuads;

   protected int myNumHardEdges = -1;

   //OBBTree obbtree = null;
   //private AjlBvTree bvHierarchy = null;
   //private boolean bvHierarchyValid = false;
   private BVTree myBVTree = null;
   private boolean myBVTreeUpdated = false;
   
   // topological properties
   private boolean myTopologyPredicatesValid = false;
   private boolean myHasNonManifoldEdges = false;
   private boolean myHasNonManifoldVertices = false;
   private boolean myHasOpenEdges = false;
   private boolean myHasIsolatedVertices = false;

   // code flags for degeneracies:

   public static final int NON_MANIFOLD_EDGES = 0x01;
   public static final int NON_MANIFOLD_VERTICES = 0x02;
   public static final int OPEN_EDGES = 0x04;
   public static final int ISOLATED_VERTICES = 0x08;

   public static final int ALL_DEGENERACIES =
      (NON_MANIFOLD_EDGES |
       NON_MANIFOLD_VERTICES |
       OPEN_EDGES |
       ISOLATED_VERTICES);

   private DistanceGrid sdGrid = null;

   // mesh subdivision data
   private int subdivisions = 0;

   private boolean myFaceNormalsValid = false;
   private boolean myRenderNormalsValid = false;

   protected PolygonalMeshRenderer myMeshRenderer = null;

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
   
   protected void invalidateTriQuadCounts() {
      myTriQuadCountsValid = false;
   }

   /**
    * {@inheritDoc}
    */
   public void notifyVertexPositionsModified() {
      super.notifyVertexPositionsModified();
      myFaceNormalsValid = false;
      myRenderNormalsValid = false;
   }

   /** 
    * {@inheritDoc}
    */
   protected void notifyStructureChanged() {
      super.notifyStructureChanged();
      myNumHardEdges = -1;
      myTopologyPredicatesValid = false;
   }

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
         if (isFixed()) {
            notifyModified();
         }
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
      return myNumTriangles == numFaces();
   }

   /**
    * Returns true if this is a quad mesh. A mesh is quad if all its faces have
    * four sides.
    * 
    * @return true if this is a quad mesh
    */
   public boolean isQuad() {
      updateTriQuadCounts();
      return myNumQuads == numFaces();
   }

   /**
    * Queries whether or not this mesh has isolated vertices. These
    * are vertices which are not connected to any edge.
    * 
    * @return <code>true</code> if this mesh has isolated vertices.
    */
   public boolean hasIsolatedVertices() {
      if (!myTopologyPredicatesValid) {
         updateTopologyPredicates(0);
      }
      return myHasIsolatedVertices;
   }
   
   /**
    * Queries whether or not this mesh has open edges. An open edge is an edge
    * which is connected to only a single face.
    * 
    * @return <code>true</code> if this mesh has open edges.
    */
   public boolean hasOpenEdges() {
      if (!myTopologyPredicatesValid) {
         updateTopologyPredicates(0);
      }
      return myHasOpenEdges;
   }
   
   /**
    * Queries whether or not this mesh has non-manifold edges. A non-manifold
    * edge is an edge that is connected to three or more faces. Note: in some
    * literature, a manifold edge is defined more strictly to require that it
    * is connected to exactly two faces. We relax this definition so that an
    * edge may have one or two faces. If any edges are associated with one
    * face, then {@link #hasOpenEdges} will return <code>true</code>.
    * 
    * @return <code>true</code> if this mesh has non-manifold edges.
    * @see #hasOpenEdges
    */
   public boolean hasNonManifoldEdges() {
      if (!myTopologyPredicatesValid) {
         updateTopologyPredicates(0);
      }
      return myHasNonManifoldEdges;
   }
   
   /**
    * Queries whether or not this mesh has non-manifold vertices. A non-vertex
    * is a vertex whose incident faces do not form a fan.  This fan may be
    * either open or closed. In some literature, a manifold vertex is defined
    * more strictly to require that the fan be closed. If any fans are
    * <i>not</i> closed, then {@link #hasOpenEdges} will return
    * <code>true</code>.
    *
    * <p>If {@link #hasNonManifoldEdges} returns <code>true</code>, then this
    * predicate will return <code>true</code> also.
    * 
    * @return <code>true</code> if this mesh has non-manifold vertices.
    * @see #hasOpenEdges
    * @see #hasNonManifoldEdges
    */
   public boolean hasNonManifoldVertices() {
      if (!myTopologyPredicatesValid) {
         updateTopologyPredicates(0);
      }
      return myHasNonManifoldVertices;
   }
   
   /**
    * Queries if this mesh is closed. This is the strictest topology predicate,
    * requiring that the mesh is both manifold (so that {@link #isManifold}
    * returns <code>true</code>), and also has no open edges, so that {@link
    * #hasOpenEdges} returns <code>false</code>.
    * 
    * @return true if the mesh is closed
    */
   public boolean isClosed() {
      if (!myTopologyPredicatesValid) {
         updateTopologyPredicates(0);
      }     
      return (!myHasOpenEdges && isManifold());
   }

   /**
    * Queries if this mesh is manifold. This is equivalent to {@link
    * #hasNonManifoldEdges}, {@link #hasNonManifoldVertices}, and {@link
    * #hasIsolatedVertices} all returning <code>false</code>.
    * 
    * <p>Note: some definitions of "manifold" also require that the mesh does
    * not self-intersect. This primitive does <i>not</i> check for this.
    * 
    * @return true if the mesh is manifold.
    */
   public boolean isManifold() {
      if (!myTopologyPredicatesValid) {
         updateTopologyPredicates(0);
      }     
      return (!myHasNonManifoldEdges && 
              !myHasNonManifoldVertices && 
              !myHasIsolatedVertices);
   }
   
   /**
    * Queries if this mesh is watertight. This is a more relaxed version of
    * <i>closed</i>, in which the mesh may not have any open edges but in which
    * it <i>may</i> have non-manifold vertices. It is therefore equivalent to
    * {@link #hasOpenEdges}, {@link #hasNonManifoldEdges}, and {@link
    * #hasIsolatedVertices} all returning <code>false</code>.
    * 
    * @return true if the mesh is watertight.
    */
   public boolean isWatertight() {
      if (!myTopologyPredicatesValid) {
         updateTopologyPredicates(0);
      }     
      return (!myHasOpenEdges && 
              !myHasNonManifoldEdges && 
              !myHasIsolatedVertices);
   }

   /**
    * Updates the topological predicates. If <code>printCode</code> is
    * non-zero, then it contains one or more of the following flags that will
    * cause degeneracies to be printed:
    *
    * <dl>
    * <li> {@link NON_MANIFOLD_EDGES}
    * <li> {@link NON_MANIFOLD_VERTICES}
    * <li> {@link OPEN_EDGES}
    * <li> {@link ISOLATED_VERTICES}
    * <dl>
    */
   private void updateTopologyPredicates (int printCode) {

      myHasNonManifoldVertices = false;
      myHasNonManifoldEdges = false;
      myHasOpenEdges = false;
      myHasIsolatedVertices = false;

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
                  if ((printCode & NON_MANIFOLD_EDGES) != 0) {
                     System.out.println (
                        "Multiple edges between "+he.head.getIndex()+
                        " and "+he.tail.getIndex());                     
                  }
                  myHasNonManifoldVertices = true;
                  myHasNonManifoldEdges = true;
               }
               adjacentVertices.add (he.tail);
               if (he.opposite == null) {
                  // boundary edge; use this to start fan traverse.
                  // otherwise, any edge will do.
                  if ((printCode & OPEN_EDGES) != 0) {
                     System.out.println (
                        "Edge "+he.vertexStr()+" is open");
                  }
                  myHasOpenEdges = true;
                  he0 = he;
               }
               nume++;
            }
            if (!myHasNonManifoldEdges) {
               HalfEdge heNext = he0.next.opposite;
               int cnt = 1;
               while (heNext != null && heNext.head == vtx && heNext != he0) {
                  heNext = heNext.next.opposite;
                  cnt++;
               }
               if (cnt != nume) {
                  // some half edges can't be reached by traverse, so not a fan
                  if ((printCode & NON_MANIFOLD_VERTICES) != 0) {
                     System.out.println (
                        "Faces incident to vertex "+vtx.getIndex()+
                        " are not a fan: num edges="+nume+
                        " num fan edges=" + cnt);
                  }
                  myHasNonManifoldVertices = true;
               }
            }
         }
         else {
            if ((printCode & ISOLATED_VERTICES) != 0) {
               System.out.println (
                  "Vertex "+vtx.getIndex()+" is isolated");
            }
            myHasIsolatedVertices = true;
         }
      }
      myTopologyPredicatesValid = true;
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
                  System.out.println(
                     "More than one edge involving tail: " + he.tail.getIndex());
                  
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
               System.out.println (
                  "vtx "+vtx.getIndex()+" has multiple fans: "+nume+" vs "+cnt);
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
    * Returns the number of faces in this mesh.
    * 
    * @return number of faces in this mesh
    */
   public int numFaces() {
      return myFaces.size();
   }

   /**
    * Returns the face with a specified index
    *
    * @param idx index of the face
    * @return indexed face
    */
   public Face getFace (int idx) {
      return myFaces.get(idx);
   }

   /**
    * Returns the half-edge with a specified index. The index is assumed to be
    * <pre>
    * 3 * faceIdx + edgeNum
    * </pre>
    * where {@code faceIdx} is the index for the edge's face, and {@code
    * edgeNum} is the edge's number with respect to the face (with {@code
    * face.firstHalfEdge()} corresponding to 0).
    *
    * @param idx index of the half-edge
    * @return indexed half-edge
    */
   public HalfEdge getHalfEdge (int idx) {
      Face face = getFace(idx/3);
      return face.getEdge (idx%3);
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

   public DistanceGrid getSignedDistanceGrid() {
      return sdGrid;
   }

   public DistanceGrid getSignedDistanceGrid (
      double margin,Vector3i cellDivisions) {
      if (cellDivisions == null) {
         sdGrid = new DistanceGrid (
            this.getFaces(), margin, new Vector3i(25,25,25), /*signed=*/true);
      }
      else
         sdGrid = new DistanceGrid (
            this.getFaces(), margin, cellDivisions, /*signed=*/true);
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
    * Creates a polygonal mesh and initializes it from a file,
    * with the file format being inferred from the file name suffix. 
    * 
    * @param fileName
    * name of the file containing the mesh description
    * @throws IOException if an I/O error occurred or if the file
    * type is not compatible with polygonal meshes
    */
   public PolygonalMesh (String fileName) throws IOException {
      this (new File (fileName));
   }

   /**
    * Creates a polygonal mesh and initializes it from a file,
    * with the file format being inferred from the file name suffix. 
    * 
    * @param file
    * file containing the mesh description
    * @throws IOException if an I/O error occurred or if the file
    * type is not compatible with polygonal meshes
    */
   public PolygonalMesh (File file) throws IOException {
      this();
      read (file);
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
    * Reads the contents of this mesh from a string. The string input is
    * assumed to be supplied in Alias Wavefront obj format, as described for
    * the method {@link #write(PrintWriter,NumberFormat,boolean)}.
    * 
    * @param input
    * supplied input description of the mesh
    */
   public void read (String input)  {
      try {
         read (new StringReader (input), false);
      }
      catch (Exception e) {
         throw new IllegalArgumentException (
            "Illegal mesh format: "+e.getMessage());
      }
   }

   /**
    * Reads the contents of this mesh from a string. The string input is
    * assumed to be supplied in Alias Wavefront obj format, as described for
    * the method {@link #write(PrintWriter,NumberFormat,boolean)}.
    * 
    * @param input
    * supplied input description of the mesh
    */
   public void read (String input, boolean zeroIndexed)  {
      try {
         read (new StringReader (input), zeroIndexed);
      }
      catch (Exception e) {
         throw new IllegalArgumentException (
            "Illegal mesh format: "+e.getMessage());
      }
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
      return addFace (indices, /*adjustAttributes=*/true);
   }

   protected Face addFace (int[] indices, boolean adjustAttributes) {
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
      if (adjustAttributes) {
         adjustAttributesForNewFeature ();
      }
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
      adjustAttributesForNewFeature ();
      notifyStructureChanged ();
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

   /**
    * Adds a triangular face to this mesh.  The face is described by three
    * vertex indices arranged in counter-clockwise order with respect to the
    * face's normal.
    * 
    * @param idx0 first vertex index
    * @param idx1 second vertex index
    * @param idx2 third vertex index
    * @throws IllegalArgumentException if any vertices are not contained within
    * this mesh
    * @return the created Face object
    */
   public Face addFace (int idx0, int idx1, int idx2) {
      return addFace (new int[] { idx0, idx1, idx2 });
   }

   /**
    * Adds a quad face to this mesh.  The face is described by four
    * vertices arranged in counter-clockwise order with respect to the
    * face's normal.
    * 
    * @param v0 first vertex
    * @param v1 second vertex
    * @param v2 third vertex
    * @param v3 fourth vertex
    * @throws IllegalArgumentException
    * if any vertices are not contained within this mesh
    * @return the created Face object
    */
   public Face addFace (Vertex3d v0, Vertex3d v1, Vertex3d v2, Vertex3d v3) {
      return addFace(new Vertex3d[] {v0,v1,v2,v3});
   }

   /**
    * Adds a quad face to this mesh.  The face is described by four
    * vertex indices arranged in counter-clockwise order with respect to the
    * face's normal.
    * 
    * @param idx0 first vertex index
    * @param idx1 second vertex index
    * @param idx2 third vertex index
    * @param idx3 fourth vertex index
    * @throws IllegalArgumentException if any vertices are not contained within
    * this mesh
    * @return the created Face object
    */
   public Face addFace (int idx0, int idx1, int idx2, int idx3) {
      return addFace (new int[] { idx0, idx1, idx2, idx3 });
   }

   /**
    * Removes a face from this mesh.
    * 
    * @param face
    * face to remove
    * @return false if the face does not belong to this mesh.
    */
   public boolean removeFace (Face face) {
      
      if (myFaces.remove (face)) {
         face.disconnect();
         int idx = face.getIndex();
         // reindex faces which occur after this one
         for (int i=idx; i<myFaces.size(); i++) {
            myFaces.get(i).setIndex (i);
         }
         //myTriQuadCountsValid = false;
         int nv = face.numVertices ();
         if (nv == 3) {
            myNumTriangles--;
         } else if (nv == 4) {
            myNumQuads--;
         }
         adjustAttributesForRemovedFeature (idx);
         notifyStructureChanged();
         return true;
      } else {
         return false;
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
         adjustAttributesForRemovedFeature (idx);
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
   public ArrayList<Integer> removeFaces (Collection<Face> faces) {
      ArrayList<Integer> deleteIdxs = new ArrayList<Integer>();
      for (Face f : faces) {
         if (myFaces.get(f.getIndex()) == f) {
            deleteIdxs.add (f.getIndex());
         }
      }
      if (deleteIdxs.size() > 0) {
         Collections.sort (deleteIdxs);
         // remove non-unique delete indices
         for (int i=1; i<deleteIdxs.size(); ) {
            if (deleteIdxs.get(i) == deleteIdxs.get(i-1)) {
               deleteIdxs.remove (i);
            }
            else {
               i++;
            }
         }
         ArrayList<Face> newFaceList =
            new ArrayList<Face>(myFaces.size()-deleteIdxs.size());
         int k = 0;
         for (int i=0; i<myFaces.size(); i++) {
            Face f = myFaces.get(i);
            if (k < deleteIdxs.size() && i == deleteIdxs.get(k)) {
               f.disconnect();
               k++;
            }
            else {
               f.setIndex (newFaceList.size());
               newFaceList.add (f);
            }
         }
         myFaces = newFaceList;
         myTriQuadCountsValid = false;
         adjustAttributesForRemovedFeatures (deleteIdxs);
         notifyStructureChanged();
         return deleteIdxs;
      }
      else {
         return null;
      }
   }

   /**
    * Removes a set of faces from this mesh, as indicated by a collection.
    *
    * @param faces Collection of faces to remove
    */
   public ArrayList<Integer> removeFacesX (Collection<Face> faces) {
      LinkedHashSet<Face> deleteFaces = new LinkedHashSet<>();
      deleteFaces.addAll (faces);
      ArrayList<Integer> deleteIdxs = new ArrayList<>();
      
      if (deleteFaces.size() > 0) {
         ArrayList<Face> newFaceList =
            new ArrayList<Face>(myFaces.size()-deleteFaces.size());
         int k = 0;
         for (int i=0; i<myFaces.size(); i++) {
            Face f = myFaces.get(i);
            if (deleteFaces.contains(f)) {
               deleteIdxs.add (f.getIndex());
               f.disconnect();
               k++;
            }
            else {
               f.setIndex (newFaceList.size());
               newFaceList.add (f);
            }
         }
         myFaces = newFaceList;
         myTriQuadCountsValid = false;
         adjustAttributesForRemovedFeatures (deleteIdxs);
         notifyStructureChanged();
         return deleteIdxs;
      }
      else {
         return null;
      }
   }

   public void clearFaces() {
      for (Face f : myFaces) {
         f.disconnect();
      }
      myFaces.clear();
      adjustAttributesForClearedFeatures();
      myNumTriangles = 0;
      myNumQuads = 0;
      myTriQuadCountsValid = false;
      notifyStructureChanged();
   }

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
         // adjust hardness. New edge can only be hard if it is
         // not open and one of the previous edges was hard
         if (heOpp != null && hnOpp != null) {
            if (heOpp.isHard() || hnOpp.isHard()) {
               heOpp.setHard (true);
               hnOpp.setHard (true);
            }
            // make sure heOpp and hnOpp are primary opposites
            if (heOpp.isPrimary() == hnOpp.isPrimary()) {
               hnOpp.setPrimary (!hnOpp.isPrimary());
            }
         }
         else {
            if (heOpp != null) {
               heOpp.setHard (false);
            }
            if (hnOpp != null) {
               hnOpp.setHard (false);  
            }
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

      // Process remaining edges connected to tail. Note that we have to access
      // these using tail.incidentHedges. If we call tail.getIncidentHedges()
      // instead, it will attempt to sort the incident half-edges, which will
      // fail because the half-edge linkage structure is in transition.
      HalfEdgeNode node = tail.incidentHedges;
      while (node != null) {
         HalfEdge ee = node.he;
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
         node = node.next;
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
      HashSet<Vertex3d> deadVerts = new HashSet<Vertex3d>();
      LinkedList<Face> deadFaces = new LinkedList<Face>();
      for (int i=0; i<myVertices.size(); i++) {

         Vertex3d vtx = myVertices.get(i);
         if (!deadVerts.contains (vtx)) {
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
                     deadVerts.add (vrm);
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
               // reset vertex position to average of removed vertices
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

      removeVertices (deadVerts);
      removeFaces (deadFaces);
      return deadVerts.size();
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
   
   /**
    * Merges adjacent faces whose normals satisfy n1.dot(n2) {@code >} cosLimit
    * 
    * @param cosLimit limit above which faces should be merged
    * @return true if modified, false otherwise
    */
   public boolean mergeCoplanarFaces(double cosLimit) {
      HashSet<HalfEdge> toMerge = new HashSet<>();
      for (Face f : myFaces) {
         HalfEdge he0 = f.he0;
         HalfEdge he = he0;
         do {
            if (he.isPrimary() && he.opposite != null) {
               if (he.getFace().getNormal().dot(he.opposite.getFace().getNormal()) > cosLimit) {
                  toMerge.add(he);
               }
            }
            he = he.next;
         } while (he != he0);
      }
      
      // go through and merge faces
      boolean modified = false;
      HashSet<Face> toUpdateFace = new HashSet<>();
      HashSet<Face> toRemoveFace = new HashSet<>();
      HashSet<Vertex3d> toRemoveVertex = new HashSet<>();
      
      if (toMerge.size() > 0) {
         
         for (HalfEdge he : toMerge) {
            
            Face f1 = he.getFace();
            Face f2 = he.getOppositeFace();
         
            if (f1 == f2) {
               
               // edge jutting out into nowhere
               if (he.next == he.opposite) {
                  HalfEdge heopp = he.opposite;
                  he.head.removeIncidentHalfEdge(he);
                  heopp.head.removeIncidentHalfEdge(heopp);
                  
                  // replace first half-edge on face
                  if (f1.he0 == he || f1.he0 == heopp) {
                     f1.he0 = heopp.next;
                  }
                  
                  // find previous half-edge
                  HalfEdge hprev = heopp.next;
                  while (hprev.next != he) {
                     hprev = hprev.next;
                  }
                  hprev.next = heopp.next;
                  
                  // form a loop
                  heopp.next = he;
                  
                  // remove if vertex no longer attached to anything
                  if (he.head.numIncidentHalfEdges() == 0) {
                     toRemoveVertex.add(he.head);
                  }
               } else if (he.opposite.next == he) {
                  
                  HalfEdge heopp = he.opposite;
                  he.head.removeIncidentHalfEdge(he);
                  heopp.head.removeIncidentHalfEdge(heopp);
                  
                  // replace first half-edge on face
                  if (f1.he0 == he || f1.he0 == heopp) {
                     f1.he0 = he.next;
                  }
                  
                  // find previous half-edge
                  HalfEdge hprev = he.next;
                  while (hprev.next != heopp) {
                     hprev = hprev.next;
                  }
                  hprev.next = he.next;
                  
                  // form a loop
                  he.next = heopp;
                  
                  // remove if vertex no longer attached to anything
                  if (heopp.head.numIncidentHalfEdges() == 0) {
                     toRemoveVertex.add(heopp.head);
                  }
               }
               
            } else {
               // System.out.println("merging " + f1.getIndex() + " & " + f2.getIndex());
               
               // replace first half-edge on face
               if (f1.he0 == he) {
                  f1.he0 = he.next;
               }
               if (f2.he0 == he.opposite) {
                  f2.he0 = he.opposite.next;
               }
               
               // find previous half-edges
               HalfEdge hprev = he.next;
               while (hprev.next != he) {
                  hprev = hprev.next;
               }
               HalfEdge ohprev = he.opposite.next;
               while (ohprev.next != he.opposite) {
                  ohprev = ohprev.next;
               }
               
               // adjust face on half-edges
               HalfEdge hh = he.opposite.next;
               do {
                  hh.face = f1;
                  hh = hh.next;
               } while (hh != he.opposite);
               
               // remove half-edge by connecting around it
               he.head.removeIncidentHalfEdge(he);
               he.opposite.head.removeIncidentHalfEdge(he.opposite);
               hprev.next = he.opposite.next;
               ohprev.next = he.next;
     
               he.face = f2;
               he.next = he.opposite;
               he.opposite.face = f2;
               he.opposite.next = he;
               f2.he0 = he;
               
               toRemoveFace.add(f2);
               toUpdateFace.add(f1);
            }
         }
         modified = true;
      }
      
      // remove vertices from straight lines
      if (modified) {
         for (Vertex3d vtx : myVertices) {
            Vector3d v1 = new Vector3d();
            Vector3d v2 = new Vector3d();
            
            int nhe = vtx.numIncidentHalfEdges();
            if (nhe == 0) {
               toRemoveVertex.add(vtx);
            } else if (nhe == 1) {
               // check if straight boundary edge
               HalfEdge he = vtx.firstIncidentHalfEdge();
               v1.sub(he.head.pnt, he.tail.pnt);
               v1.normalize();
               v2.sub(he.next.head.pnt, he.next.tail.pnt);
               v2.normalize();
               if (v1.dot(v2) > cosLimit) {
                  
                  // remove vtx and he
                  vtx.removeIncidentHalfEdge(he);
                  HalfEdge hprev = he.next;
                  while (hprev.next != he) {
                     hprev = hprev.next;
                  }
                  // connect he.next to hprev
                  hprev.next = he.next;
                  he.next.tail = hprev.head;
                  
                  // maybe replace first half-edge on face
                  Face f = he.getFace();
                  if (f.he0 == he) {
                     f.he0 = he.next;
                  }
                  
                  // create loop of one for removed half-edge
                  he.next = he;
                  
                  toRemoveVertex.add(vtx);
               }
            } else if (nhe == 2) {
   
               HalfEdge he = vtx.firstIncidentHalfEdge();
               // check of consistent line on both sides of vertex
               if (he.next.opposite != null && he.next.opposite.next == he.opposite) {
                  // check if closed straight edge
                  v1.sub(he.head.pnt, he.tail.pnt);
                  v1.normalize();
                  v2.sub(he.next.head.pnt, he.next.tail.pnt);
                  v2.normalize();
                  if (v1.dot(v2) > cosLimit) {
                     
                     HalfEdge he2 = he.next.opposite;
                     
                     // remove vtx and he
                     vtx.removeIncidentHalfEdge(he);
                     vtx.removeIncidentHalfEdge(he2);
                     
                     HalfEdge hprev = he.next;
                     while (hprev.next != he) {
                        hprev = hprev.next;
                     }
                     // connect he.next to hprev
                     hprev.next = he.next;
                     he.next.tail = hprev.head;
                     
                     HalfEdge hprev2 = he2.next;
                     while (hprev2.next != he2) {
                        hprev2 = hprev2.next;
                     }
                     // connect he2.next to hprev2
                     hprev2.next = he2.next;
                     he2.next.tail = hprev2.head;
                     // align new opposites
                     he.next.opposite = he2.next;
                     he2.next.opposite = he.next;
                     
                     // maybe replace first half-edge on face
                     Face f = he.getFace();
                     if (f.he0 == he) {
                        f.he0 = he.next;
                     }
                     
                     Face f2 = he2.getFace();
                     if (f2.he0 == he2) {
                        f2.he0 = he2.next;
                     }
                     
                     // create loop of one for removed half-edge
                     he.next = he;
                     he2.next = he2;
                     
                     toRemoveVertex.add(vtx);
                  }
               }
            }
         }
      }
      
      if (modified) {
         // remove faces and vertices
         removeVertices(toRemoveVertex);
         removeFaces(toRemoveFace);
         
         clearAttributes();  // XXX for now just clear attributes.  If we were diligent, we could re-adjust.
         myTriQuadCountsValid = false;
         notifyStructureChanged();
      }
      
      return modified;
   }

   /**
    * Remove adjacent faces whose normals satisfy n1.dot(n2) {@code <} cosLimit
    * 
    * @param cosLimit limit above which faces should be removed
    * @return true if modified, false otherwise
    */
   public boolean removeOppositeFaces(double cosLimit) {
      HashSet<Face> toRemoveFace = new HashSet<>();
      for (Face f : myFaces) {
         HalfEdge he0 = f.he0;
         HalfEdge he = he0;
         do {
            if (he.isPrimary() && he.opposite != null) {
               if (he.getFace().getNormal().dot(he.opposite.getFace().getNormal()) < cosLimit) {
                  toRemoveFace.add(he.getFace ());
                  toRemoveFace.add(he.getOppositeFace ());
               }
            }
            he = he.next;
         } while (he != he0);
      }
      
      if (toRemoveFace.size () > 0) {
         // remove faces and vertices
         removeFaces(toRemoveFace);
         removeDisconnectedVertices ();
         
         clearAttributes();  // XXX for now just clear attributes.  If we were diligent, we could re-adjust.
         myTriQuadCountsValid = false;
         notifyStructureChanged();
      }
      
      return modified;
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
      ArrayList<Vertex3d> deadVerts = new ArrayList<Vertex3d>();
      for (int i=0; i<myVertices.size(); i++) {
         Vertex3d v = myVertices.get(i);
         if (v.firstIncidentHalfEdge() == null) {
            deadVerts.add (v);
         }
      }
      removeVertices (deadVerts);
      return deadVerts.size();
   }

   private void partitionConnectedComponents (
      ArrayList<HashMap<Vertex3d,Integer>> vertexMaps, 
      ArrayList<HashSet<Face>> faceSets) {

      boolean[] faceIsMarked = new boolean [numFaces()];

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

//   /**
//    * Writes this mesh to a File, using an Alias Wavefront "obj" file
//    * format. Behaves the same as {@link
//    * #write(java.io.PrintWriter,maspack.util.NumberFormat,boolean,boolean)}
//    * with <code>zeroIndexed</code> and <code>facesClockwise</code> set to
//    * false.
//    * 
//    * @param file
//    * File to write this mesh to
//    * @param fmtStr
//    * format string for writing the vertex coordinates. If <code>null</code>,
//    * a format of <code>"%.8g"</code> is assumed.
//    */
//   public void write (File file, String fmtStr) throws IOException {
//      if (fmtStr == null) {
//         fmtStr = "%.8g";
//      }
//      NumberFormat fmt = new NumberFormat (fmtStr);
//      PrintWriter pw = null;
//      try {
//         pw = new PrintWriter (new BufferedWriter (new FileWriter (file)));
//         write (pw, fmt, /*zeroIndexed=*/false, /*facesClockwise=*/false);
//      }
//      catch (IOException e) {
//         throw e;
//      }
//      finally {
//         if (pw != null) {
//            pw.close();
//         }
//      }
//   }

//   /**
//    * Writes this mesh to a File, using an Alias Wavefront "obj" file
//    * format. Behaves the same as {@link
//    * #write(java.io.PrintWriter,maspack.util.NumberFormat,boolean,boolean)}
//    * with <code>zeroIndexed</code> and <code>facesClockwise</code> set to
//    * false.
//    * 
//    * @param file
//    * File to write this mesh to
//    * @param fmtStr
//    * format string for writing the vertex coordinates. If <code>null</code>,
//    * a format of <code>"%.8g"</code> is assumed.
//    * @param zeroIndexed
//    * if true, index numbering for mesh vertices starts at 0. Otherwise,
//    * numbering starts at 1.
//    */
//   public void write (File file, String fmtStr, boolean zeroIndexed)
//      throws IOException {
//      if (fmtStr == null) {
//         fmtStr = "%.8g";
//      }
//      NumberFormat fmt = new NumberFormat (fmtStr);
//      PrintWriter pw = null;
//      try {
//         pw = new PrintWriter (new BufferedWriter (new FileWriter (file)));
//         write (pw, fmt, zeroIndexed, /*facesClockwise=*/false);
//      }
//      catch (IOException e) {
//         throw e;
//      }
//      finally {
//         if (pw != null) {
//            pw.close();
//         }
//      }      
//   }

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

      WavefrontWriter ww = new WavefrontWriter ((OutputStream)null);
      if (fmt != null) {
         ww.setFormat (fmt);
      }
      if (zeroIndexed) {
         ww.setZeroIndexed (true);
      }
      if (facesClockwise) {
         ww.setFacesClockwise (true);
      }
      ww.writeMesh (pw, this);
      pw.flush();
   }

   /* Write the mesh to a file in the .poly format for tetgen. */
   public void writePoly (String nodeString) throws Exception {
      FileOutputStream fos = null;
      try {
         fos = new FileOutputStream (nodeString);
         PrintStream ps = new PrintStream (new BufferedOutputStream (fos));
         writePoly (ps);
      }
      catch (IOException e) {
         throw e;
      }
      finally {
         if (fos != null) {
            fos.close();
         }
      }
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
   
   /**
    * Translates the vertices of this mesh so that its origin coincides
    * with the center of volume. The topology of the mesh remains unchanged.
    *
    * @return the resulting rigid transform that was applied to the mesh
    */
   public RigidTransform3d translateToCenterOfVolume () {
      Vector3d off = new Vector3d();
      computeCentreOfVolume (off);
      off.negate();
      translate (off);
      return new RigidTransform3d (off.x, off.y, off.z);
   }
   
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
   }

   /**
    * Applies a vector transformation to the vertices of this mesh. The
    * topology of the mesh remains unchanged.
    * 
    * @param T
    * vector transformation
    */
   public void transform (VectorTransformer3d T) {
      super.transform (T);
      for (Face f : myFaces) {
         f.updateNormalAndEdges();
      }
   }

   /**
    * Applies an inverse vector transformation to the vertices of this mesh. The
    * topology of the mesh remains unchanged.
    * 
    * @param T
    * vector transformation
    */
   public void inverseTransform (VectorTransformer3d T) {
      super.inverseTransform (T);
      for (Face f : myFaces) {
         f.updateNormalAndEdges();
      }
   }

   /**
    * {@inheritDoc}
    */
   public int getRenderHints() {
      int code = 0;
      if (myRenderProps != null && myRenderProps.isTransparent()) {
         code |= TRANSPARENT;
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
   
   public void prerender (RenderProps props) {
      super.prerender (props);
      if (myMeshRenderer == null) {
         myMeshRenderer = new PolygonalMeshRenderer (this);
      }
      myMeshRenderer.prerender (props);
   }

   public void render (Renderer renderer, RenderProps props, int flags) {
      if (myMeshRenderer == null) {
         String errMsg = "render() called before prerender()";
         if (getName() != null) {
            errMsg += ", mesh '"+getName()+"'";
         }
         throw new IllegalStateException (errMsg);
      }
      myMeshRenderer.render (renderer, props, flags);
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
      HalfEdgeNode hen = he.tail.getIncidentHedges();

      while (hen != null) {
         if (hen.he.head == he.tail && hen.he.tail == he.head)
            return false;
         hen = hen.next;
      }

      return true;
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
         HalfEdgeNode hen = he.tail.getIncidentHedges();

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


   static int[] unpackIndices (int[] indices, int[] indexOffs, int fidx) {
      int k0 = indexOffs[fidx];
      int k1 = indexOffs[fidx+1];
      int[] findices = new int[k1-k0];
      for (int j=0; j<findices.length; j++) {
         findices[j] = indices[k0+j];
      }
      return findices;
   }

   static int[] packIndices (ArrayList<int[]> indices) {
      int len = 0;
      for (int i=0; i<indices.size(); i++) {
         len += indices.get(i).length;
      }
      int[] packed = new int[len];
      int k = 0;
      for (int i=0; i<indices.size(); i++) {
         int[] fidxs = indices.get(i);
         for (int j=0; j<fidxs.length; j++) {
            packed[k++] = fidxs[j];
         }
      }
      return packed;
   }

   /**
    * Modifies this mesh to ensure that all faces are triangles.
    */
   public void triangulate() {

      updateTriQuadCounts();

      int estNewFaces = 2*(myFaces.size()-myNumTriangles);

      ArrayList<Face> faceList = new ArrayList<Face>(myNumTriangles);
      ArrayList<int[]> normalList = null;
      ArrayList<int[]> textureList = null;
      ArrayList<int[]> colorList = null;

      ArrayList<int[]> newFaceIndices = new ArrayList<int[]>(estNewFaces);
      ArrayList<int[]> newNormalIndices = null;
      ArrayList<int[]> newTextureIndices = null;
      ArrayList<int[]> newColorIndices = null;

      int[] indexOffs = getFeatureIndexOffsets();

      if (myNormalsExplicitP) {
         newNormalIndices = new ArrayList<int[]>(estNewFaces);
         normalList = new ArrayList<int[]>(myNumTriangles+estNewFaces);
      }
      else {
         clearNormals();
      }
      if (myTextureCoords != null) {
         newTextureIndices = new ArrayList<int[]>(estNewFaces);
         textureList = new ArrayList<int[]>(myNumTriangles+estNewFaces);
      }
      if (myColors != null) {
         newColorIndices = new ArrayList<int[]>(estNewFaces);
         colorList = new ArrayList<int[]>(myNumTriangles+estNewFaces);
      }

      for (int i=0; i<myFaces.size(); i++) {
         Face face = myFaces.get(i);
         int numEdges = face.numEdges();
         if (numEdges == 3) {
            faceList.add (face);
            if (myNormalsExplicitP) {
               normalList.add (unpackIndices(myNormalIndices, indexOffs, i));
            }
            if (myTextureCoords != null) {
               textureList.add (unpackIndices(myTextureIndices, indexOffs, i));
            }
            if (myColors != null) {
               colorList.add (unpackIndices(getColorIndices(), indexOffs, i));
            }
         } else {

            face.disconnect();
            int tidxs[] = null;
            int nidxs[] = null;
            int cidxs[] = null;
            int idxs[] = face.getVertexIndices();
            if (myNormalsExplicitP) {
               nidxs = unpackIndices(myNormalIndices, indexOffs, i);
            }
            if (myTextureCoords != null) {
               tidxs = unpackIndices(myTextureIndices, indexOffs, i);
               
            }
            if (myColors != null) {
               cidxs = unpackIndices(getColorIndices(), indexOffs, i);
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
                  if (myNormalsExplicitP) {
                     newNormalIndices.add (getChord (chordIdxs, nidxs));
                     nidxs = removeIndex (chordIdxs[1], nidxs);
                  }
                  if (myTextureCoords != null) {
                     newTextureIndices.add (getChord (chordIdxs, tidxs));
                     tidxs = removeIndex (chordIdxs[1], tidxs);
                  }
                  if (myColors != null) {
                     newColorIndices.add (getChord (chordIdxs, cidxs));
                     cidxs = removeIndex (chordIdxs[1], cidxs);
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
                  if (myNormalsExplicitP) {
                     newNormalIndices.add (getChord (chordIdxs, nidxs));
                     nidxs = removeIndex (chordIdxs[1], nidxs);
                  }
                  if (myTextureCoords != null) {
                     newTextureIndices.add (getChord (chordIdxs, tidxs));
                     tidxs = removeIndex (chordIdxs[1], tidxs);
                  }
                  if (myColors != null) {
                     newColorIndices.add (getChord (chordIdxs, cidxs));
                     cidxs = removeIndex (chordIdxs[1], cidxs);
                  }
               }
            }

            // last triplet
            newFaceIndices.add (idxs);
            if (myNormalsExplicitP) {
               newNormalIndices.add (nidxs);
            }
            if (myTextureCoords != null) {
               newTextureIndices.add (tidxs);
            }
            if (myColors != null) {
               newColorIndices.add (cidxs);
            }
         }
      }

      myFaces = faceList;
      // need to reindex these faces because they might have moved around
      for (int i=0; i<myFaces.size(); i++) {
         myFaces.get(i).setIndex (i);
      }
      for (int[] idxs : newFaceIndices) {
         addFace (idxs, /*adjustAttributes=*/false);
      }

      if (myNormalsExplicitP) {
         normalList.addAll (newNormalIndices);
         myNormalIndices = packIndices (normalList);
      }
      if (myTextureCoords != null) {
         textureList.addAll (newTextureIndices);
         myTextureIndices = packIndices (textureList);
      }
      if (myColors != null) {
         colorList.addAll (newColorIndices);
         myColorIndices = packIndices (colorList);
      }

      myNumTriangles = myFaces.size();
      myNumQuads = 0;
      myTriQuadCountsValid = true;
      notifyStructureChanged();
      checkIndexConsistency();
   }

   /**
    * Returns a half edge (if any) connecting vertices v0 and v1.
    */
   protected HalfEdge getEdge (Vertex3d v0, Vertex3d v1) {
      // check all edges attached to v0
      HalfEdge he = null;
      HalfEdgeNode node;
      // OK to access incidentHedges without sorting them first;
      // i.e., we don't need to call getIncidentHedges().
      for (node=v0.incidentHedges; node != null; node=node.next) {
         he = node.he;
         if (he.getTail() == v1) {
            break;
         }
      }
      if (he == null) {
         for (node=v1.incidentHedges; node != null; node=node.next) {
            he = node.he;
            if (he.getTail() == v0) {
               break;
            }
         }
      }
      return he;
   }
   
   private void updateHardEdgeCount() {
      if (myNumHardEdges == -1) {
         int fullcnt = 0;
         for (int i=0; i<myFaces.size(); i++) {
            HalfEdge he0 = myFaces.get(i).firstHalfEdge();
            HalfEdge he = he0;
            do {
               if (validateHardEdge (he)) {
                  fullcnt++;
               }
               he = he.next;
            }
            while (he != he0);
         }
         myNumHardEdges = fullcnt/2;
      }
   }

   /**
    * Returns the number of hard edges currently in this mesh.
    *
    * @return number of hard edges
    */
   public int numHardEdges() {
      updateHardEdgeCount();
      return myNumHardEdges;
   }

   /**
    * Clears all the hard edges currently in this mesh.
    */
   public void clearHardEdges() {
      if (myNumHardEdges != 0) {
         for (int i=0; i<myFaces.size(); i++) {
            HalfEdge he0 = myFaces.get(i).firstHalfEdge();
            HalfEdge he = he0;
            do {
               if (he.isHard()) {
                  he.setHard(false);
               }
               he = he.next;
            }
            while (he != he0);
         }
         myNumHardEdges = 0;
      }
   }

   /**
    * Sets the edge between the vertices indexed by <code>vidx0</code> and
    * <code>vidx1</code> to be hard, as described for {@link
    * #hasHardEdge(Vertex3d,Vertex3d) hasHardEdge(v0,v1)}. If so such edge
    * exists, the method returns <code>false</code>.
    * 
    * @param vidx0 first vertex index
    * @param vidx1 second vertex index
    * @return <code>true</code> if the edge exists
    */
   public boolean setHardEdge (int vidx0, int vidx1, boolean hard) {
      int numv = numVertices();
      if (vidx0 < 0 || vidx0 >= numv) {
         throw new IndexOutOfBoundsException (
            "vidx0="+vidx0+", numVertices=" + numv);
      }
      if (vidx1 < 0 || vidx1 >= numv) {
         throw new IndexOutOfBoundsException (
            "vidx1="+vidx1+", numVertices=" + numv);
      }
      return setHardEdge (getVertex(vidx0), getVertex(vidx1), hard);
   }

   /**
    * Sets the edge between the vertices <code>v0</code> and <code>v1</code> to
    * be hard, as described for {@link #hasHardEdge(Vertex3d,Vertex3d)
    * hasHardEdge(v0,v1)}. If no such edge exists, or if the edge
    * corresponds only to a single half-edge, the method returns
    * <code>false</code>.
    * 
    * @param v0 first vertex
    * @param v1 second vertex
    * @return <code>true</code> if the edge exists
    */
   public boolean setHardEdge (Vertex3d v0, Vertex3d v1, boolean hard) {
      HalfEdge he = getEdge (v0, v1);
      if (he == null) {
         // edge not found
         return false;
      }
      else if (he.opposite == null) {
         if (he.isHard()) {
            he.setHard (false); // shouldn't be hard; fix
         }
         return false;
      }
      else {
         if (he.isHard() != hard) {
            he.setHard (hard);
            he.opposite.setHard (hard);
            if (!myNormalsExplicitP) {
               clearNormals(); // will need to regenerate
            }
            // invalidate hard edge count
            myNumHardEdges = -1;
         }
      }
      return true;
   }

   /**
    * Returns <code>true</code> if the edge between the vertices indexed by
    * <code>vidx0</code> and <code>vidx1</code> exists and is hard, as
    * described for {@link #hasHardEdge(Vertex3d,Vertex3d) hasHardEdge(v0,v1)}.
    *
    * @param vidx0 first vertex index
    * @param vidx1 second vertex index
    * @return <code>true</code> if the edge exists and is hard.
    */
   public boolean hasHardEdge (int vidx0, int vidx1) {
      int numv = numVertices();
      if (vidx0 < 0 || vidx0 >= numv) {
         throw new IndexOutOfBoundsException (
            "vidx0="+vidx0+", numVertices=" + numv);
      }
      if (vidx1 < 0 || vidx1 >= numv) {
         throw new IndexOutOfBoundsException (
            "vidx1="+vidx1+", numVertices=" + numv);
      }
      return hasHardEdge (getVertex(vidx0), getVertex(vidx1));
   }

   private boolean validateHardEdge (HalfEdge he) {
      if (he.opposite == null) {
         if (he.isHard()) {
            he.setHard (false); // shouldn't be hard; fix
         }
      }
      else if (he.isHard() != he.opposite.isHard()) {
         // both half edges should be hard; fix
         he.setHard (true);
         he.opposite.setHard (true);
      }
      return he.isHard();
   }
   
   /**
    * Returns <code>true</code> if the edge between vertices <code>v0</code>
    * and <code>v1</code> exists and is hard. A hard setting is used as a hint
    * that the edge is associated with an actual discontinuity in the surface
    * derivative, and that it may be desirable to render it crisply even when
    * using smooth shading.
    *
    * @param v0 first vertex
    * @param v1 second vertex
    * @return <code>true</code> if the edge exists and is hard.
    */
   public boolean hasHardEdge (Vertex3d v0, Vertex3d v1) {
      HalfEdge he = getEdge (v0, v1);
      return (he != null && validateHardEdge(he));
   }

   void computeFaceNormals() {
      for (int i = 0; i < myFaces.size(); i++) {
         myFaces.get (i).computeNormal();
      }
      myFaceNormalsValid = true;
      notifyModified();              
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

   public PolygonalMesh clone() {
      PolygonalMesh mesh = (PolygonalMesh)super.clone();

      mesh.myFaces = new ArrayList<Face>();
      for (int i=0; i<numFaces(); i++) {
         mesh.addFace (
            myFaces.get(i).getVertexIndices(), /*adjustAttributes=*/false);
      }
      
      mesh.myBVTree = null;
      mesh.myBVTreeUpdated = false;
      if (sdGrid != null) {
         System.out.println (
            "Warning: mesh copy not implemented for signed distance grids");
         mesh.sdGrid = null;
      }
      mesh.myMeshRenderer = null;
      if (numHardEdges() > 0) {
         // copy hard edge settings
         for (int i=0; i<myFaces.size(); i++) {
            HalfEdge he0 = myFaces.get(i).firstHalfEdge();
            HalfEdge heNew = mesh.myFaces.get(i).firstHalfEdge();
            HalfEdge he = he0;
            do {
               if (he.isHard()) {
                  heNew.setHard(true);
               }
               heNew = heNew.next;
               he = he.next;
            }
            while (he != he0);
         }        
      }
      mesh.myTriQuadCountsValid = false;
      return mesh;
   }

   public void replaceVertices (List<? extends Vertex3d> vtxs) {
      super.replaceVertices (vtxs);
      // replace the faces
      ArrayList<int[]> faceIdxs = new ArrayList<int[]>();
      for (int i=0; i<numFaces(); i++) {
         faceIdxs.add (myFaces.get(i).getVertexIndices());
      }
      myFaces.clear();
      for (int i=0; i<faceIdxs.size(); i++) {
         addFace (faceIdxs.get(i));
      }
   }
   
   public void addMesh (MeshBase mesh) {
      if (mesh instanceof PolygonalMesh) {
         addMesh((PolygonalMesh)mesh, false);  // use PolygonalMesh overloaded function
      } else {
         addMesh (mesh, /*respectTransforms=*/false);
      }
   }

   /** 
    * Adds copies of the vertices and faces of another mesh to this mesh.  If
    * the other mesh contains normal information, this will be added as well
    * providing this mesh already contains normal information or is
    * empty. Otherwise, normal information for this mesh will be cleared.  The
    * same behavior is observed for texture and color information.
    * 
    * @param mesh Mesh to be added to this mesh
    */
   public void addMesh (PolygonalMesh mesh, boolean respectTransforms) {

      int voff = myVertices.size();
      super.addMesh (mesh, respectTransforms);

      // EDIT: Sanchez, May 2012
      // changed order to allow texture indices to be copied over
      for (int i = 0; i < mesh.numFaces(); i++) {
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
   

//   /**
//    * Returns true if the mesh is closed. This is determined by checking that
//    * all edges have a corresponding opposite edge. The mesh also needs to be
//    * manifold.
//    * 
//    * @param debug if <code>true</code> and mesh is not closed, prints reason
//    * @return true if the mesh is closed
//    */
//   public boolean isClosed (boolean debug) {
//      if (cachedClosedValid) {
//         return cachedClosed;
//      }
//      cachedClosedValid = true;
//      cachedClosed = false;
//      if (!isManifold(debug)) {
//         return false;
//      }
//      for (Face face : myFaces) {
//         HalfEdge he = face.he0;
//         do {
//            if (he.opposite == null) {
//               if (debug) {
//                  System.out.println (
//                     "Open edge "+he.vertexStr()+" on face "+face.getIndex());
//               }
//               return false;
//            }
//            if (he.opposite.face == null) {
//               if (debug) {
//                  System.out.println (
//                     "Open edge (null face) "+he.toString()+
//                     " on face "+face.getIndex());
//               }
//               return false;
//            }
//            he = he.next;
//         }
//         while (he != face.he0);
//      }
//      cachedClosed = true;
//      return true;
//   }

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

      updateFaceNormals();

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

         if (nrml.containsNaN() || nrml.equals(Vector3d.ZERO)) {
            // just in case 
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
   
   public static RigidTransform3d computePrincipalAxes (PolygonalMesh mesh) {

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

   /**
    * If this mesh is triangular, returns the nearest distance to a point. This
    * method uses the default bounding volume hierarchy returned by {@link
    * #getBVTree}.
    *
    * @param pnt point for which distance should be computed (world coords)
    * @return distance to the mesh, or -1 if the no nearest mesh face is
    * found.
    * @throws IllegalArgumentException if this mesh is not triangular
    */
   public double distanceToPoint (Point3d pnt) {
      return distanceToPoint (new Point3d(), pnt);
   }

   /**
    * If this mesh is triangular, returns the nearest distance to a point. This
    * method uses the default bounding volume hierarchy returned by {@link
    * #getBVTree}.
    *
    * @param nearPnt if not <code>null</code>, returns the nearest
    * point on the mesh in world coordinates.
    * @param pnt point for which distance should be computed (world coords)
    * @return distance to the mesh, or -1 if the no nearest mesh face is
    * found.
    * @throws IllegalArgumentException if this mesh is not triangular
    */
   public double distanceToPoint (Point3d nearPnt, Point3d pnt) {
      if (!isTriangular()) {
         throw new IllegalArgumentException ("mesh is not triangular");
      }
      BVFeatureQuery query = new BVFeatureQuery();
      if (query.nearestFaceToPoint (
             nearPnt, null, getBVTree(), pnt) == null) {
         return -1;
      }
      else {
         return nearPnt.distance (pnt);
      }
   }

   /**
    * If this mesh is triangular, returns the nearest triangular face to a
    * point. This method uses the default bounding volume hierarchy
    * returned by {@link #getBVTree}.
    *
    * @param nearPnt if not <code>null</code>, returns the nearest
    * point on the face in world coordinates.
    * @param uv if not <code>null</code>, returns the UV coordinates of the
    * nearest point on the face. These are the barycentric coordinates with
    * respect to the second and third vertices.
    * @param pnt point for which the nearest face should be found.
    * @return the nearest face to the point, or <code>null</code>
    * if the mesh contains no faces.
    * @throws IllegalArgumentException if this mesh is not triangular
    */
   public Face nearestFaceToPoint (Point3d nearPnt, Vector2d uv, Point3d pnt) {
      if (!isTriangular()) {
         throw new IllegalArgumentException ("mesh is not triangular");
      }
      BVFeatureQuery query = new BVFeatureQuery();
      return query.nearestFaceToPoint (nearPnt, uv, getBVTree(), pnt);
   }

    /**
    * If this mesh is triangular and closed, determines if a point is inside or
    * outside the mesh. Being exactly ``on'' the mesh counts as outside. The
    * mesh normals are assumed to be pointing outwards. This method uses the
    * default bounding volume hierarchy returned by {@link #getBVTree}.
    *
    * The method works by counting the number of times a random ray cast from
    * the point intersects the mesh. Degeneracies are detected and result in
    * another ray being cast.  Whether or not the point is on the mesh is
    * determined using a numerical tolerance computed from the mesh's overall
    * dimensions. In rare cases, the method may be unable to determine whether
    * the point is inside or outside, in which case the method returns -1. In
    * such cases, or if one wishes to distinquish whether the point is outside
    * vs. ``on'' the mesh, it may be useful to call the underlying {@link
    * BVFeatureQuery} method {@link
    * BVFeatureQuery#isInsideMesh(PolygonalMesh,Point3d,double) isInsideMesh()}
    * directly, which offers more control.
    *
    * @param pnt point to check.
    * @return 1 if <code>pnt</code> is inside, 0 if outside, or -1 if the
    * method is can't find an answer.
    * @throws IllegalArgumentException if this mesh is not triangular
    */  
   public int pointIsInside (Point3d pnt) {
      if (!isTriangular()) {
         throw new IllegalArgumentException ("mesh is not triangular");
      }
      BVFeatureQuery query = new BVFeatureQuery();
      switch (query.isInsideMesh (this, getBVTree(), pnt, -1)) {
         case INSIDE: {
            return 1;
         }
         case OUTSIDE:
         case ON: {
            return 0;
         }
         default: {
            return -1;
         }
      }
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
      return true;
   }

   /**
    * Computes a spatial inertia for the volume defined by this mesh, assuming
    * a uniform density density. It is assumed that the mesh is
    * closed, although small holes in the mesh should not affect the
    * calculation that much.  It is also assumed that the faces are oriented
    * counter-clockwise about their outward-facing normal.
    *
    * @param M returns the computed spatial inertia
    * @param density
    * density of the volume
    * @return the volume of the mesh
    */
   public double computeInertia (SpatialInertia M, double density) {
      Point3d cov = new Point3d();
      SymmetricMatrix3d J = new SymmetricMatrix3d();
      double vol = computeUnitInertiaComps (cov, J);
      double mass = vol*density;
      J.scale (mass);
      M.set (mass, J, cov);
      return vol;
   }

   /**
    * Computes the volume, center of volume, and inertia tensor (with respect
    * to the center of volume) for this mesh, assuming a unit mass.
    *
    * @param cov returns the center of volume
    * @param J inertia tensor (with respect to the center of volume)
    * @return volume of the mesh
    */
   protected double computeUnitInertiaComps (Point3d cov, SymmetricMatrix3d J) {
      Vector3d mov1 = new Vector3d();
      Vector3d mov2 = new Vector3d();
      Vector3d pov = new Vector3d();

      double vol = computeVolumeIntegrals (mov1, mov2, pov);
      double invVol = 1.0/vol;

      cov.scale (invVol, mov1); // center of volume

      J.set (mov2.y+mov2.z, mov2.x+mov2.z, mov2.x+mov2.y,
             -pov.z, -pov.y, -pov.x); 
      J.scale (invVol);

      // J contains the mass[com][com] term; remove this:
      J.m00 -= (cov.z * cov.z + cov.y * cov.y);
      J.m11 -= (cov.z * cov.z + cov.x * cov.x);
      J.m22 -= (cov.y * cov.y + cov.x * cov.x);
      J.m01 += (cov.x * cov.y);
      J.m02 += (cov.x * cov.z);
      J.m12 += (cov.z * cov.y);
      J.m10 = J.m01;
      J.m20 = J.m02;
      J.m21 = J.m12;
      return vol;
   }

   /**
    * Computes the centre of volume of the mesh
    */
   public double computeCentreOfVolume (Vector3d c) {
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

   SpatialInertia createEdgeLengthInertia (double mass) {
      SpatialInertia M = new SpatialInertia();
      // first compute the total length of all edges
      double totalLength = 0;
      for (Face face : myFaces) {
         HalfEdge he0 = face.firstHalfEdge();
         HalfEdge he = he0;
         do {
            if (he.isPrimary()) {
               totalLength += he.length();
            }
            he = he.getNext();
         }
         while (he != he0);
      }
      // use this to determine the partial inertia for each edge
      for (Face face : myFaces) {
         HalfEdge he0 = face.firstHalfEdge();
         HalfEdge he = he0;
         do {
            if (he.isPrimary()) {
               double l = he.length();
               M.addLineSegmentInertia (
                  mass*l/totalLength, he.tail.pnt, he.head.pnt);
            }
            he = he.getNext();
         }
         while (he != he0);
      }
      return M;
   }

   SpatialInertia createAreaInertia (double mass) {
      SpatialInertia M = new SpatialInertia();
      // determine total number of triangles, to save the area for each
      int numt = 0;
      for (Face face : myFaces) {
         numt += face.numVertices()-2;
      }
      double[] areas = new double[numt];
      // compute area for each triangle, along with total area
      double totalArea = 0;
      int k = 0;
      for (Face face : myFaces) {
         HalfEdge he0 = face.firstHalfEdge();
         HalfEdge he = he0;
         Point3d p0 = he.tail.pnt;
         he = he.next;
         Point3d p1 = he.tail.pnt;
         he = he.next;
         do {
            Point3d p2 = he.tail.pnt;            
            double a = Face.computeTriangleArea (p0, p1, p2);
            areas[k++] = a;
            totalArea += a;
            p1 = p2;
            he = he.next;
         }
         while (he != he0);
      }
      // use these areas to determine the partial inertia for each triangle
      k = 0;
      for (Face face : myFaces) {
         HalfEdge he0 = face.firstHalfEdge();
         HalfEdge he = he0;
         Point3d p0 = he.tail.pnt;
         he = he.next;
         Point3d p1 = he.tail.pnt;
         he = he.next;
         do {
            Point3d p2 = he.tail.pnt;            
            M.addTriangleInertia (mass*areas[k++]/totalArea, p0, p1, p2);
            p1 = p2;
            he = he.next;
         }
         while (he != he0);
      }
      return M;
   }

   SpatialInertia createVolumeInertia (double mass) {
      SpatialInertia M = new SpatialInertia();
      Point3d cov = new Point3d();
      SymmetricMatrix3d J = new SymmetricMatrix3d();
      computeUnitInertiaComps (cov, J);
      J.scale (mass);
      M.set (mass, J, cov);
      return M;
   }

   /**
    * Computes a spatial inertia for this mesh, given a mass and a mass
    * distribution.  All distributions are supported.
    *
    * @param mass overall mass
    * @param dist how the mass is distributed across the features
    */   
   public SpatialInertia createInertia (double mass, MassDistribution dist) {
      if (dist == MassDistribution.DEFAULT) {
         dist = isClosed() ? MassDistribution.VOLUME : MassDistribution.AREA;
      }
      if (dist == MassDistribution.LENGTH) {
         return createEdgeLengthInertia (mass);
      }
      else if (dist == MassDistribution.AREA) {
         return createAreaInertia (mass);
      }
      else if (dist == MassDistribution.VOLUME) {
         return createVolumeInertia (mass);
      }
      else {
         return super.createInertia (mass, dist);
      }
   }   

   /**
    * {@inheritDoc}
    */
   public boolean supportsMassDistribution (MassDistribution dist) {
      switch (dist) {
         case POINT:
         case LENGTH:
         case AREA:
         case VOLUME:
            return true;
         default:
            return false;
      }
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
      double v = numVertices();
      double f = numFaces();
      double e = countEdges();
      double b = countBorders();

      double g = c-(v+f-e+b)/2;

      return g;
   }

   private void flipFeatureIndices (int[] indices) {
      int[] indexOffs = getFeatureIndexOffsets();
      // flip the indices for each face:
      for (int fi=0; fi<indexOffs.length-1; fi++) {
         // get lo and hi vertex indices for each face:
         int lo = indexOffs[fi];
         int hi = indexOffs[fi+1]-1;
         int numv = hi-lo+1;
         // now reverse indices:
         for (int i=0; i<numv/2; i++) {
            int tmpi = indices[lo+i];
            indices[lo+i] = indices[hi-i];
            indices[hi-i] = tmpi;
         }
      }      
   }

   public void flip() {
      for (Face f : myFaces) {
         f.flip(true);
      }
      // take care of normals:
      if (hasNormals()) {
         if (hasExplicitNormals()) {
            flipFeatureIndices (getNormalIndices());
         }
         else {
            // normals must have been automatically generated. Clear
            // then so that they will be rebuilt.
            clearNormals();
         }
      }
      // flip texture indices:
      if (hasTextureCoords()) {
         flipFeatureIndices (getTextureIndices());
      }
      // flip color indices:
      if (hasColors()) {
         flipFeatureIndices (getColorIndices());
      }
      notifyModified();
   }

   public int[] createVertexIndices() {
      int numi = 0;
      for (int i=0; i<myFaces.size(); i++) {
         numi += myFaces.get(i).numVertices();
      }
      int[] indices = new int[numi];
      int k = 0;
      for (int i=0; i<myFaces.size(); i++) {
         HalfEdge he0 = myFaces.get(i).firstHalfEdge();
         HalfEdge he = he0;
         do {
            indices[k++] = he.head.getIndex();
            he = he.getNext();
         }
         while (he != he0);
      }     
      return indices;
   }
   
   public int numFeatures() {
      return myFaces.size();
   }

   protected int[] createFeatureIndexOffsets() {
      int[] offsets = new int[numFaces()+1];
      int k = 0;
      offsets[0] = 0;
      for (int i=0; i<myFaces.size(); i++) {
         k += myFaces.get(i).numVertices();
         offsets[i+1] = k;
      }
      return offsets;         
   }

   protected int[] createDefaultIndices() {
      int[] indexOffs = getFeatureIndexOffsets();
      int[] indices = new int[indexOffs[indexOffs.length-1]];
      int k = 0;
      for (int i=0; i<myFaces.size(); i++) {
         HalfEdge he0 = myFaces.get(i).firstHalfEdge();
         HalfEdge he = he0;
         do {
            indices[k++] = he.head.getIndex();
            he = he.getNext();
         }
         while (he != he0);
      }
      return indices;
   }

   /**
    * {@inheritDoc}
    */
   public boolean hasAutoNormalCreation() {
      return true;
   }

   /**
    * {@inheritDoc}
    */
   public boolean getWriteNormals() {
      return (hasExplicitNormals() || 
              (numHardEdges() > 0 && myMultiAutoNormalsP));
   }
   
   /**
    * Enable or disable multiple vertex normals when normals are computed
    * automatically, as described for {@link #getMultipleAutoNormals()}.
    *
    * @param enable enables or disables automatic computation of
    * multiple vertex normals.
    */
   public void setMultipleAutoNormals (boolean enable) {
      if (myMultiAutoNormalsP != enable) {
         myMultiAutoNormalsP = enable;
         if (!myNormalsExplicitP) {
            clearNormals(); // will need to recompute
         }
      }
   }
   
   /**
    * Returns <code>true</code> if multiple vertex normals are allowed when
    * normals are computed automatically. If allowed, then a different normal
    * will be computed for each contiguous region of faces about the vertex
    * that is delimited by an open or hard edge (see {@link
    * #hasHardEdge(Vertex3d,Vertex3d)}). Note that vertices will be associated
    * with multiple normals <i>only</i> when the surrounding faces contain open
    * or hard edges.
    *
    * @return <code>true</code> if multiple vertex normals are allowed
    */
   public boolean getMultipleAutoNormals () {
      return myMultiAutoNormalsP;
   }

   /**
    * Computes vertex normals using an angle-weighted average of the normals
    * formed by the edges incident on each vertex. The normals and normal
    * indices for this mesh will be reset. One normal will be computed per
    * vertex, except in situations where the vertex has incident half edges
    * that are open or hard, in which case extra normals will be computed.
    */
   public void autoGenerateNormals () {
      ArrayList<Vector3d> normals = new ArrayList<Vector3d>();
      int[] indices = computeVertexNormals (normals, myMultiAutoNormalsP);
      myNormals = normals;
      myNormalIndices = indices;
   }
   
   /**
    * Sets edges to be "hard" based on angle between faces.
    * @param cosThreshold dot product between face normals is below this threshold, marks edge as hard
    */
   public void setHardEdgesFromFaceNormals(double cosThreshold) {
      updateFaceNormals();
      
      for (Face f : myFaces) {
         HalfEdge he0 = f.firstHalfEdge();
         HalfEdge he = he0;
         do {
            if (he.isPrimary()) {
               if (he.opposite == null) {
                  he.setHard(true);
               } else if (he.getFace().getNormal().dot(he.getOppositeFace().getNormal()) < cosThreshold) {
                  he.setHard(true);
                  he.opposite.setHard(true);
               } else {
                  he.setHard(false);
                  he.opposite.setHard(false);
               }
            }
            he = he.next;
         } while (he != he0);
      }
      myAutoNormalsValidP = false;
   }

   /**
    * Computes a set of vertex normals for this mesh, using an
    * angle-weighted average of the normals formed by the edges incident on
    * each vertex. If the angle-weighted average would result in a zero normal
    * (e.g. vertices on a straight line), then the adjacent face normals
    * are used.  If <code>multiNormals</code> is <code>true</code>, then
    * multiple normals may be computed for each vertex, with different normals
    * being computed for edge regions that are separated by open or hard
    * edges. Otherwise, only one normal is computed per vertex.
    * 
    * <p>If <code>normals</code> is passed in with zero size, then the normals
    * are computed and returned in new <code>Vector3d</code> objects that are
    * and added to it. Also, the method returns a set of computed normal
    * indices. This option is used for the initial creation of normals.
    *
    * <p>If <code>normals</code> is passed in with non-zero size, then it is
    * assumed to contain enough <code>Vector3d</code> objects to store all the
    * computed normals, and the method returns <code>null</code>.  This option
    * is used for updating normals.
    * 
    * @param normals returns the computed normals
    * @param multiNormals if <code>true</code>, then multiple normals
    * may be computed for each vertex
    * @return normals indices, if <code>normals</code> has zero size,
    * otherwise <code>null</code>.
    */
   public int[] computeVertexNormals (
      ArrayList<Vector3d> normals, boolean multiNormals) {

      boolean creatingNormals = (normals.size() == 0);
      if (multiNormals) {
         updateHardEdgeCount(); // make sure hard edges are properly set
      }
      
      HashMap<HalfEdge,Integer> normalIndexMap = null;
      if (creatingNormals) {
         // Each half edge will be associated with a normal for its head vertex.
         normalIndexMap = new HashMap<HalfEdge,Integer>();
      }

      // Start by allocating normals and determining the normal index
      // associated with each half-face
      int idx = 0;
      for (Vertex3d vtx : myVertices) {

         HalfEdgeNode node = vtx.getIncidentHedges();
         Vector3d nrm;
         while (node != null) {
            if (creatingNormals) {
               // create a new vector to store the normal
               nrm = new Vector3d();
               normals.add (nrm);
            }
            else {
               // use the existing normal vector
               nrm = normals.get(idx);
               nrm.setZero();
            }
            // Add the normal contributions for each vertex half edge. If we
            // are allows to compute multiple normals per vertex, stop if we
            // reach a normal boundary.
            do {
               HalfEdge he = node.he;
               nrm.angleWeightedCrossAdd (
                  he.tail.pnt, he.head.pnt, he.next.head.pnt);
               if (creatingNormals) {
                  normalIndexMap.put (node.he, idx);
               }
               node = node.next;
            }
            while (node != null &&
                   (!multiNormals || !vtx.isNormalBoundary(node.he)));
               
            double n2 = nrm.normSquared();
            if (n2 == 0) {
               // backup, just in case angle weighted normals fails
               vtx.computeAreaWeightedNormal(nrm);
               //nmag = nrm.norm();
            }
            nrm.normalize();
            //if (nmag > 0) {
            //   nrm.scale(1.0/nmag);
            //}
            idx++;
         }
      }
      
      if (creatingNormals) {
         // Now assign the normal indices for each face. These are the indices of
         // the normals associated with each of the face's half edges.

         int[] indexOffs = getFeatureIndexOffsets();
         int[] indices = new int[indexOffs[indexOffs.length-1]];

         int k = 0;
         for (Face face : myFaces) {
            HalfEdge he0 = face.firstHalfEdge();
            HalfEdge he = he0;
            do {
               Integer id = normalIndexMap.get (he);
               if (id == null) {
                  throw new InternalErrorException (
                     "Normal not computed for halfEge on face "+face.getIndex());
               }
               indices[k++] = id;
               he = he.getNext();
            }
            while (he != he0);
         }
         return indices;
      }
      else {
         return null;
      }
   }

   protected void autoUpdateNormals() {
      computeVertexNormals (myNormals, myMultiAutoNormalsP);
   }

   /**
    * Returns true if the normal structure associated with this HalfEdge
    * implies that it is hard.
    */
   private boolean hardEdgeInferredFromNormals (HalfEdge he) {
      if (he.opposite == null) {
         return false;
      }
      int[] indexOffs = getFeatureIndexOffsets();
      int off0 = indexOffs[he.face.getIndex()];
      HalfEdge op = he.opposite;
      int off1 = indexOffs[op.face.getIndex()];

      // index of head normal for face 0
      int headNrm0 = myNormalIndices[off0 + he.face.indexOfVertex (he.head)];
      // index of head normal for face 1
      int headNrm1 = myNormalIndices[off1 + op.face.indexOfVertex (he.head)];
      // index of tail normal for face 0
      int tailNrm0 = myNormalIndices[off0 + he.face.indexOfVertex (he.tail)];
      // index of tail normal for face 1
      int tailNrm1 = myNormalIndices[off1 + op.face.indexOfVertex (he.tail)];
      return headNrm0 != headNrm1 && tailNrm0 != tailNrm1;
   }

   /**
    *
    */
   public boolean setHardEdgesFromNormals() {

      HashSet<HalfEdge> hardSet = new HashSet<HalfEdge>(); 
      if (myNormals == null || myNormals.size() == numVertices()) {
         // can't be any hard edges. Leave myNormalsExplicitP unchanged,
         // since even if numNormals == numVertices, we can't assume
         // computeVertexNormals() will work properly: it will expect
         // numNormals > numVertices if it detects a vertex that needs
         // multiple normals.
         return false;
      }
      for (Vertex3d vtx : myVertices) {
         Iterator<HalfEdge> it = vtx.getIncidentHalfEdges();
         while (it.hasNext()) {
            HalfEdge he = it.next();
            if (hardEdgeInferredFromNormals (he)) {
               hardSet.add (he);
               he.setHard (true);
            }
         }
      }
      ArrayList<Vector3d> normals = new ArrayList<Vector3d>();      
      int[] indices = computeVertexNormals (normals, myMultiAutoNormalsP);
      
      // now see if the normals and indices equal those within the mesh, to
      // within 1e-8
      double eps = 1e-8;

      boolean normalsEqual = false;
      if (myNormals.size() == normals.size()) {
         int i;
         for (i=0; i<myNormals.size(); i++) {
            Vector3d nrm0 = myNormals.get(i);
            Vector3d nrm1 = normals.get(i);
            if (!nrm0.epsilonEquals (nrm1, eps)) {
               break;
            }
         }
         if (i == myNormals.size()) {
            normalsEqual = Arrays.equals (myNormalIndices, indices);
         }
      }
      if (!normalsEqual) {
         myNormalsExplicitP = true;
         // leave the half edges set
         return false;
      }
      else {
         myNormalsExplicitP = false;
         myAutoNormalsValidP = true; 
         return true;
      }
   }

   public void printDegenerateFaces() {
      double atol = computeArea()*Math.sqrt(2.0/numFaces())*1e-13;
      for (Face f : myFaces) {
         double area = f.computeArea();
         if (area < atol) {
            System.out.println ("face "+f.getIndex()+": "+f.vertexStr()+" "+area);
         }
      }
   }

   /**
    * Prints topological degeneracies associated with this mesh. Which
    * degeneracies to print are determined by the flags in
    * <code>printCode</code>:
    *
    * <dl>
    * <dt> {@link #NON_MANIFOLD_EDGES}
    * <dt> {@link #NON_MANIFOLD_VERTICES}
    * <dt> {@link #OPEN_EDGES}
    * <dt> {@link #ISOLATED_VERTICES}
    * </dl>
    */
   public void printDegeneracies (int printCode) {

      updateTopologyPredicates (printCode);

   }

   public void printDegeneracies() {

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
                  System.out.println (
                     "Multiple edges between "+he.head.getIndex()+
                     " and "+he.tail.getIndex());

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
               System.out.println (
                  "Vertex "+vtx.getIndex()+
                  " has open edges ("+nume+" vs "+cnt+")");
            }
         }
      }
      for (Face face : myFaces) {
         HalfEdge he = face.he0;
         do {
            if (he.opposite == null) {
               System.out.println (
                  "Open edge "+he.vertexStr()+" on face "+face.getIndex());
            }
            else if (he.opposite.face == null) {
               System.out.println (
                  "Open edge (null face) "+he.toString()+
                  " on face "+face.getIndex());
            }
            he = he.next;
         }
         while (he != face.he0);
      }

   }

   /**
    * Returns the estimated memory useage for this mesh, in bytes. The
    * following formula is used:
    * <pre>
    * numBytes = 84*F + 92*V + 64*H
    * </pre>
    * where F, V and H are the numbers of faces, vertices, and half edges,
    * respectively. This number is only a rough estimate and assumes that
    * references require 4 bytes and not 8.
    */
   public int estimateMemoryUsage() {
      int numHalfEdges = 0;
      for (Face f : myFaces) {
         numHalfEdges += f.numEdges();
      }
      return 84*numFaces() + 92*numVertices() + 64*numHalfEdges;      
   }

   
}
