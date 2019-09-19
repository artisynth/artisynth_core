/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;

import maspack.geometry.io.WavefrontReader;
import maspack.geometry.io.XyzbReader;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.properties.HasProperties;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.spatialmotion.SpatialInertia;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;

/**
 * Implements a mesh consisting of a set of points and possibly normals.
 */
public class PointMesh extends MeshBase {

   PointMeshRenderer myMeshRenderer = null;

   protected AABBTree myBVTree = null;
   protected boolean myBVTreeValid = false;
   // if > 0, causes normals to be rendered
   protected double myNormalRenderLen = 0;

   /**
    * {@inheritDoc}
    */
   public RenderProps createRenderProps (HasProperties host) {
      return RenderProps.createPointLineProps (host);
   }

   /**
    * Creates an empty point mesh.
    */
   public PointMesh() {
      super();
   }

   /**
    * Creates a point mesh and initializes it from a file, with the file format
    * being inferred from the file name suffix.
    * 
    * @param fileName
    * name of the file containing the mesh description
    * @throws IOException if an I/O error occurred or if the file
    * format is not compatible with point meshes
    */
   public PointMesh (String fileName) throws IOException {
      this (new File (fileName));
   }

   /**
    * Creates a point mesh and initializes it from a file, with the file format
    * being inferred from the file name suffix
    * 
    * @param file
    * file containing the mesh description
    * @throws IOException if an I/O error occurred or if the file
    * format is not compatible with point meshes
    */
   public PointMesh (File file) throws IOException {
      this();
      read (file);
   }

   public double getNormalRenderLen() {
      return myNormalRenderLen;
   }

   public void setNormalRenderLen (double len) {
      if (myNormalRenderLen != len) {
         myNormalRenderLen = len;
         notifyModified();
      }
   }

   public void readBinary (File file) throws IOException {
      XyzbReader reader = new XyzbReader (file);
      reader.setLittleEndian (true);
      reader.readMesh (this);
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
      wfr.close();
   }

   /**
    * Sets the vertex points and normals associated with this mesh.
    * 
    * @param pnts
    * points from which the vertices are formed
    * @param nrms
    * (optional) if non-null, gives vectors from which the normals are formed.
    * @throws IllegalArgumentException if nrms is non-null and does not have the
    * same size as pnts.
    */
   public void set (Point3d[] pnts, Vector3d[] nrms) {
      clear();
      if (nrms != null && nrms.length == 0) {
         nrms = null;
      }
      if (nrms != null && nrms.length != pnts.length) {
         throw new IllegalArgumentException ("If non-null, nrm must have the same size as pnts");
      }
      for (int i=0; i<pnts.length; i++) {
         addVertex (pnts[i]);
      }
      if (nrms != null) {
         ArrayList<Vector3d> nlist = new ArrayList<Vector3d>(nrms.length);
         for (int i=0; i<nrms.length; i++) {
            nlist.add (nrms[i]);
         }
         setNormals (nlist, /*indices=*/null);
      }
   }

   /**
    * Writes this mesh to a PrintWriter, using an Alias Wavefront "obj" file as
    * described for {@link #write(PrintWriter,NumberFormat,boolean)}. Index
    * numbering starts at one, and the format used to print vertex coordinates
    * is specified by a C <code>printf</code> style format string contained in
    * the parameter <code>fmtStr</code>. For a description of the format
    * string syntax, see {@link maspack.util.NumberFormat NumberFormat}. Good
    * default choices for <code>fmtStr</code> are either <code>"%g"</code> (full
    * precision), or <code>"%.Ng"</code>, where <i>N</i> is the number of
    * desired significant figures.
    * 
    * @param pw
    * PrintWriter to write this mesh to
    * @param fmtStr
    * string specifying format for writing the vertex coordinates
    */
   public void write (PrintWriter pw, String fmtStr) throws IOException {
      write (pw, new NumberFormat(fmtStr), false);
   }

   /**
    * Writes this mesh to a PrintWriter, using an Alias Wavefront "obj" file
    * format. Vertices are printed first, each starting with the letter "v" and
    * followed by x, y, and z coordinates. Normals, if present, are printed
    * next, starting with the letter "vn" and followed by x, y, and z
    * coordinates.
    * 
    * <p>
    * The format used to print vertex coordinates is specified by a
    * {@link maspack.util.NumberFormat NumberFormat}.
    * 
    * @param pw
    * PrintWriter to write this mesh to
    * @param fmt
    * (optional) format for writing the vertex and normals coordinates. If <code>null</code>,
    * a format of <code>"%.8g"</code> is assumed.
    * @param zeroIndexed
    * if true, index numbering for mesh vertices starts at 0. Otherwise,
    * numbering starts at 1.
    */
   public void write (PrintWriter pw, NumberFormat fmt, boolean zeroIndexed)
      throws IOException {
      if (fmt == null) {
         fmt = new NumberFormat ("%.8g");
      }
      for (Vertex3d vertex : myVertices) {
         Point3d pnt = vertex.pnt;
         pw.println ("v " + fmt.format (pnt.x) + " " + fmt.format (pnt.y) + " " +
                     fmt.format (pnt.z));
      }
      if (myNormals != null) {
         for (Vector3d nrm : myNormals) {
            pw.println ("vn " + fmt.format (nrm.x) + " " + fmt.format (nrm.y) + " " +
                        fmt.format (nrm.z));
         }
      }
      pw.flush();
   }

   public void prerender (RenderProps props) {
      super.prerender (props);
      if (myMeshRenderer == null) {
         myMeshRenderer = new PointMeshRenderer (this);
      }
      myMeshRenderer.prerender (props);
   }

   public void render (Renderer renderer, RenderProps props, int flags) {
      if (myMeshRenderer == null) {
         throw new IllegalStateException (
            "render() called before prerender()");
      }
      myMeshRenderer.render (renderer, props, flags);
   }

   /** 
    * Creates a copy of this mesh.
    */
   public PointMesh copy() {
      myBVTree = null;
      myBVTreeValid = false;
      return (PointMesh)super.copy();
   }

   public PointMesh clone() {
      PointMesh mesh = (PointMesh)super.clone();
      mesh.myBVTree = null;
      mesh.myBVTreeValid = false;
      return mesh;
   }
   
   public void addMesh (PointMesh mesh) {
      addMesh (mesh, /*respectTransforms=*/false);
   }

   /** 
    * Adds copies of the vertices of another mesh to this mesh.  If the other
    * mesh contains normal information, this will be added as well providing
    * this mesh already contains normal information or is empty. Otherwise,
    * normal information for this mesh will be cleared.
    * 
    * @param mesh Mesh to be added to this mesh
    */
   public void addMesh (PointMesh mesh, boolean respectTransforms) {
      super.addMesh (mesh, respectTransforms);
   }

   public AABBTree getBVTree() {
       if (myBVTree == null || !myBVTreeValid) {
          myBVTree = new AABBTree();
          myBVTree.setMaxLeafElements (8);
          int numElems = numVertices();
          Boundable[] elements = 
             myVertices.toArray(new Boundable[numElems]);
          
          myBVTree.build (elements, numElems);
          myBVTreeValid = true;
       }
       return myBVTree;
    }

   /**
    * Tests to see if a mesh equals this one. The meshes are equal if they are
    * both PolylineMeshes, and their transforms, vertices, and normals
    * are equal (within <code>eps</code>).
    */
   public boolean epsilonEquals (MeshBase base, double eps) {
      if (!(base instanceof PointMesh)) {
         return false;
      }
      if (!super.epsilonEquals (base, eps)) {
         return false;
      }
      return true;
   }

   public int[] createVertexIndices() {
      int[] indices = new int[numVertices()];
      for (int i=0; i<indices.length; i++) {
         indices[i] = i;
      }
      return indices;
   }
   
   public int numFeatures() {
      return numVertices();
   }

   protected int[] createFeatureIndexOffsets() {
      int[] offsets = new int[numVertices()+1];
      for (int i=0; i<offsets.length; i++) {
         offsets[i] = i;
      }
      return offsets;         
   }

   protected int[] createDefaultIndices() {
      int[] indexOffs = getFeatureIndexOffsets();
      int[] indices = new int[indexOffs[indexOffs.length-1]];
      for (int i=0; i<numVertices(); i++) {
         indices[i] = getVertex(i).getIndex();
      }
      return indices;
   }

   public boolean hasAutoNormalCreation() {
      return false;
   }

   /**
    * {@inheritDoc}
    */
   public boolean getWriteNormals() {
      return hasExplicitNormals();
   }

   protected void autoGenerateNormals() {
   }

   protected void autoUpdateNormals() {
   }

   public void addVertex (Vertex3d vtx) {
      super.addVertex (vtx);
      adjustAttributesForNewFeature();
   }

   public boolean removeVertex (Vertex3d vtx) {
      boolean removed = super.removeVertex (vtx);
      if (removed) {
         adjustAttributesForRemovedFeature (vtx.idx);
      }
      return removed;
   }

   public boolean removeVertexFast (Vertex3d vtx) {
      boolean removed = super.removeVertexFast (vtx);
      if (removed) {
         adjustAttributesForRemovedFeature (vtx.idx);
      }
      return removed;
   }

   public ArrayList<Integer> removeVertices (Collection<Vertex3d> vertices) {
      ArrayList<Integer> removed = super.removeVertices (vertices);
      if (removed != null) {
         adjustAttributesForRemovedFeatures (removed);
      }
      return removed;
   }

   /**
    * Computes a spatial inertia for this mesh, given a mass which
    * is assumed to be distributed uniformly across its vertices.
    *
    * @param mass overall mass
    */  
   public SpatialInertia createInertia (double mass) {
      return super.createInertia (mass, MassDistribution.POINT);
   }

   /**
    * Computes a spatial inertia for this mesh, given a mass and a mass
    * distribution.  Only the {@link MassDistribution#POINT} distribution is 
    * supported.
    *
    * @param mass overall mass
    * @param dist how the mass is distributed across the features
    * @throws IllegalArgumentException if the distribution is not compatible
    * with the available mesh features.
    */  
   public SpatialInertia createInertia (double mass, MassDistribution dist) {
      return super.createInertia (mass, dist);
   }
   
}
