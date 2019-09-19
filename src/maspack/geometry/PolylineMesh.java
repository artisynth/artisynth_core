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
import java.util.List;

import maspack.geometry.io.WavefrontReader;
import maspack.matrix.Point3d;
import maspack.properties.HasProperties;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.spatialmotion.SpatialInertia;
import maspack.util.ArraySupport;
import maspack.util.FunctionTimer;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;

/**
 * Implements a mesh consisting of a set of polylines.
 */
public class PolylineMesh extends MeshBase {
   protected ArrayList<Polyline> myLines = new ArrayList<Polyline>();

   PolylineMeshRenderer myMeshRenderer = null;

   protected AABBTree myBVTree = null;
   protected boolean myBVTreeValid = false;
   protected int renderSkip = 0;   // render every 1+skip lines
   
   @Override 
   public void invalidateBoundingInfo() {
      super.invalidateBoundingInfo();
      myBVTreeValid = false;
   }

   @Override 
   public void clearBoundingInfo() {
      super.clearBoundingInfo();
      myBVTree = null;
   }

   /**
    * {@inheritDoc}
    */
   public RenderProps createRenderProps (HasProperties host) {
      return RenderProps.createLineProps (host);
   }

   /**
    * Returns this mesh's lines. The line are contained within an ArrayList,
    * each element of which is an array giving the vertex indices for the line.
    * 
    * @return list of this mesh's lines.
    */
   public ArrayList<Polyline> getLines() {
      return myLines;
   }

   /**
    * Returns the number of lines in this mesh.
    * 
    * @return number of lines in this mesh
    */
   public int numLines() {
      return myLines.size();
   }

   /**
    * Creates an empty polyline mesh.
    */
   public PolylineMesh() {
      super();
   }

   /**
    * Creates a polyline mesh and initializes it from a file, with the file
    * format being inferred from the file name extension.
    * 
    * @param fileName
    * name of the file containing the mesh description
    * @throws IOException if an I/O error occurred or if the file
    * type is not compatible with polyline meshes
    */
   public PolylineMesh (String fileName) throws IOException {
      this (new File (fileName));
   }

   /**
    * Creates a polyline mesh and initializes it from a file, with the file
    * format being inferred from the file name extension.
    * 
    * @param file
    * file containing the mesh description
    * @throws IOException if an I/O error occurred or if the file
    * type is not compatible with polyline meshes
    */
   public PolylineMesh (File file) throws IOException {
      this();
      read (file);
   }

   public PolylineMesh (PolylineMesh old) {
      this();
      for (Vertex3d v : old.myVertices) {
         addVertex (new Vertex3d (new Point3d (v.pnt), v.idx));
      }
      for (Polyline line : old.myLines) {
         addLine (line.getVertexIndices());
      }
      setMeshToWorld (old.XMeshToWorld);
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
    * Sets the vertex points and line associated with this mesh.
    * 
    * @param pnts
    * points from which the vertices are formed
    * @param lineIndices
    * integer arrays giving the indices of each line. Each index should
    * correspond to a particular point in pnts.
    * @throws IllegalArgumentException
    * if a vertex index is out of bounds
    */
   public void set (Point3d[] pnts, int[][] lineIndices) {
      set (pnts, lineIndices, /* byReference= */false);
   }

   /**
    * Adds a line to this mesh. A line is described by indices which specify
    * its vertices.
    * 
    * @param indices
    * integer array giving the vertex indices of the line. Each index should
    * correspond to a vertex presently associated with this mesh.
    * @throws IllegalArgumentException
    * if a vertex index is out of bounds
    * @return the created line object
    */
   public Polyline addLine (int[] indices) {
      Polyline line = new Polyline (myLines.size());
      Vertex3d[] vtxList = new Vertex3d[indices.length];
      for (int i = 0; i < indices.length; i++) {
         int idx = indices[i];
         if (idx < 0 || idx >= myVertices.size()) {
            throw new IllegalArgumentException (
               "Line vertex index "+idx+" out of bounds, face number "+line.idx);
         }
         vtxList[i] = (Vertex3d)myVertices.get (idx);
      }
      line.set (vtxList, indices.length);
      myLines.add (line);
      adjustAttributesForNewFeature();
      notifyStructureChanged();
      return line;
   }

   /**
    * Adds a line to this mesh. A face is described by an array of vertices
    * which comprise the line.
    * 
    * @param vertices
    * vertices comprising this line. Each vertex should be presently contained
    * in this mesh.
    * @throws IllegalArgumentException
    * if any vertices are not contained within this mesh
    * @return the created line object
    */
   public Polyline addLine (Vertex3d[] vertices) {
      Polyline line = new Polyline (myLines.size());
      for (int i = 0; i < vertices.length; i++) {
         int idx = vertices[i].getIndex();
         if (idx < 0 || idx >= myVertices.size() ||
             myVertices.get (idx) != vertices[i]) {
            throw new IllegalArgumentException ("Vertex not present in mesh");
         }
      }
      line.set (vertices, vertices.length);
      myLines.add (line);
      adjustAttributesForNewFeature();
      notifyStructureChanged();
      return line;
   }

   /**
    * Adds a line to this mesh. 
    * 
    * @param line
    * Polyline to add to the mesh
    * @return the created line object
    */
   public Polyline addLine (Polyline line) {
      // add missing vertices
      for (Vertex3d vtx : line.getVertices()) {
         if (!myVertices.contains(vtx)) {
            addVertex(vtx);
         }
      }
      line.idx = myLines.size();
      myLines.add (line);
      adjustAttributesForNewFeature();
      notifyStructureChanged();
      return line;
   }
   
   /**
    * Removes a line from this mesh.
    * 
    * @param line
    * line to remove
    * @return false if the line does not belong to this mesh.
    */
   public boolean removeLine (Polyline line) {
      if (myLines.remove (line)) {
         adjustAttributesForRemovedFeature (line.idx);
         notifyStructureChanged();
         return true;
      }
      else {
         return false;
      }
   }

   public Polyline getLine(int i) {
      return myLines.get(i);
   }

   /**
    * Sets the vertex points and line associated with this mesh.
    * 
    * @param pnts
    * points from which the vertices are formed
    * @param lineIndices
    * integer arrays giving the indices of each line. Each index should
    * correspond to a particular point in pnts.
    * @param byReference
    * if true, then the supplied points are not copied but instead referred to
    * directly by the vertex structures. The mesh will track any changes to the
    * points and {@link #isFixed isFixed} will return false.
    * @throws IllegalArgumentException
    * if a vertex index is out of bounds
    */
   protected void set (Point3d[] pnts, int[][] lineIndices, boolean byReference) {
      clear();
      for (int i = 0; i < pnts.length; i++) {
         addVertex (pnts[i], byReference);
      }
      for (int k = 0; k < lineIndices.length; k++) {
         try {
            addLine (lineIndices[k]);
         }
         catch (IllegalArgumentException e) {
            clear();
            throw e;
         }
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
    * followed by x, y, and z coordinates. Lines are printed next, starting
    * with the letter "f" and followed by a list of integers which gives the
    * indices of that line's vertices. An example of a simple three
    * point line is:
    * 
    * <pre>
    *    v 1.0 0.0 0.0
    *    v 0.0 1.0 0.0
    *    v 0.0 0.0 1.0
    *    l 0 1 2
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
      // int lineCnt = 0;
      for (Polyline line : myLines) {
         // int idxCnt = 0;
         pw.print ("l");
         int[] idxs = line.getVertexIndices();
         for (int i=0; i<idxs.length; i++) {
            if (zeroIndexed) {
               pw.print (" " + (idxs[i]));
            }
            else {
               pw.print (" " + (idxs[i] + 1));
            }
         }
         pw.println ("");
      }
      pw.flush();
   }

   /**
    * Clears this mesh (makes it empty).
    */
   public void clear() {
      // verts = null;
      super.clear();
      myLines.clear();
   }

   FunctionTimer timer = new FunctionTimer();
   
   public void prerender (RenderProps props) {
      super.prerender (props);
      
      if (myMeshRenderer == null) {
         myMeshRenderer = new PolylineMeshRenderer (this);
      }
      myMeshRenderer.prerender(props);
   }

   public void render(Renderer renderer, RenderProps props, int flags) {
      if (myMeshRenderer == null) {
         throw new IllegalStateException (
            "render() called before prerender()");
      }
      myMeshRenderer.render(renderer, props, flags);
   }
   
   /** 
    * Creates a copy of this mesh.
    */
   public PolylineMesh copy() {
      myBVTree = null;
      myBVTreeValid = false;
      return (PolylineMesh)super.copy();
   }

   public PolylineMesh clone() {
      PolylineMesh mesh = (PolylineMesh)super.clone();

      mesh.myLines = new ArrayList<Polyline>();
      for (int i=0; i<numLines(); i++) {
         mesh.addLine (myLines.get(i).getVertexIndices());
      }
      mesh.myBVTree = null;
      mesh.myBVTreeValid = false;

      return mesh;
   }

   public void replaceVertices (List<? extends Vertex3d> vtxs) {
      super.replaceVertices (vtxs);
      // replace the lines
      ArrayList<int[]> lineIdxs = new ArrayList<int[]>();
      for (int i=0; i<numLines(); i++) {
         lineIdxs.add (myLines.get(i).getVertexIndices());
      }
      myLines.clear();
      for (int i=0; i<lineIdxs.size(); i++) {
         addLine (lineIdxs.get(i));
      }
   }

//   /** 
//    * Creates a copy of this mesh using a specific set of vertices.
//    */
//   public PolylineMesh copyWithVertices (ArrayList<? extends Vertex3d> vtxs) {
//      PolylineMesh mesh = new PolylineMesh();
//
//      if (vtxs.size() < myVertices.size()) {
//         throw new IllegalArgumentException (
//            "Number of supplied vertices="+vtxs.size()+", need "+myVertices.size());
//      }
//      for (int i=0; i<vtxs.size(); i++) {
//         mesh.addVertex (vtxs.get(i));
//      }
//      mesh.setMeshToWorld (XMeshToWorld);
//      for (int i = 0; i < numLines(); i++) {
//         mesh.addLine (myLines.get (i).getVertexIndices());
//      }
//
//      if (myRenderProps != null) {
//         mesh.setRenderProps (myRenderProps);
//      }
//      else {
//         mesh.myRenderProps = null;
//      }
//      mesh.setFixed (isFixed());
//      mesh.setRenderBuffered (isRenderBuffered());
//      mesh.setName(getName());
//      return mesh;
//   }
   
   public void addMesh (PolylineMesh mesh) {
      addMesh (mesh, /*respectTransforms=*/false);
   }
   
   /** 
    * Adds copies of the vertices and lines of another mesh to this mesh.
    * 
    * @param mesh Mesh to be added to this mesh
    */
   public void addMesh (PolylineMesh mesh, boolean respectTransforms) {

      int voff = myVertices.size();
      super.addMesh (mesh, respectTransforms);
      for (int i = 0; i < mesh.numLines(); i++) {
         addLine (copyWithOffset (
                     mesh.getLines().get(i).getVertexIndices(), voff));
      }
   }

   public AABBTree getBVTree() {
      if (myBVTree == null) {
         myBVTree = new AABBTree (this, 8);
         myBVTree.setBvhToWorld (XMeshToWorld);
         myBVTreeValid = true;
      }
      else if (!myBVTreeValid) {
         myBVTree.update();
         myBVTreeValid = true;
      }
      return myBVTree;
   }
   
   /**
    * Number of lines to skip while rendering
    */
   public void setRenderSkip(int skip) {
      if (skip >=0) {
         renderSkip = skip;
      } else {
         skip = 0;
      }
   }
   
   public int getRenderSkip() {
      return renderSkip;
   }

   /**
    * Tests to see if a mesh equals this one. The meshes are equal if they are
    * both PolylineMeshes, and their transforms, vertices, and lines
    * are equal (within <code>eps</code>).
    */
   public boolean epsilonEquals (MeshBase base, double eps) {
      if (!(base instanceof PolylineMesh)) {
         return false;
      }
      if (!super.epsilonEquals (base, eps)) {
         return false;
      }
      PolylineMesh mesh = (PolylineMesh)base;
      if (myLines.size() != mesh.myLines.size()) {
         return false;
      }
      for (int i=0; i<myLines.size(); i++) {
         int[] idxs0 = myLines.get(i).getVertexIndices();
         int[] idxs1 = mesh.myLines.get(i).getVertexIndices();
         if (!ArraySupport.equals (idxs0, idxs1)) {
            return false;
         }
      }
      return true;
   }

   public int[] createVertexIndices() {
      int numi = 0;
      for (int i=0; i<myLines.size(); i++) {
         numi += myLines.get(i).numVertices();
      }
      int[] indices = new int[numi];
      int k = 0;
      for (int i=0; i<myLines.size(); i++) {
         Polyline line = myLines.get(i);
         for (int j=0; j<line.numVertices(); j++) {
            indices[k++] = line.getVertex(j).getIndex();
         }
      }     
      return indices;
   }

   public int numFeatures() {
      return myLines.size();
   }

   protected int[] createFeatureIndexOffsets() {
      int[] offsets = new int[numLines()+1];
      int k = 0;
      offsets[0] = 0;
      for (int i=0; i<myLines.size(); i++) {
         k += myLines.get(i).numVertices();
         offsets[i+1] = k;
      }
      return offsets;         
   }

   protected int[] createDefaultIndices() {
      int[] indexOffs = getFeatureIndexOffsets();
      int[] indices = new int[indexOffs[indexOffs.length-1]];
      int k = 0;
      for (int i=0; i<myLines.size(); i++) {
         Polyline line = myLines.get(i);
         for (int j=0; j<line.numVertices(); j++) {
            indices[k++] = line.getVertex(j).getIndex();
         }
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

   /**
    * Computes a spatial inertia for this mesh, given a mass and a mass
    * distribution.  Only {@link MassDistribution#POINT} and {@link
    * MassDistribution#LENGTH} distributions are supported.
    *
    * @param mass overall mass
    * @param dist how the mass is distributed across the features
    * @throws IllegalArgumentException if the distribution is not compatible
    * with the available mesh features.
    */  
   public SpatialInertia createInertia (double mass, MassDistribution dist) {
      if (dist == MassDistribution.DEFAULT) {
         dist = MassDistribution.LENGTH;
      }
      if (dist == MassDistribution.LENGTH) {
         SpatialInertia M = new SpatialInertia();

         int nsegs = 0;
         for (Polyline line : myLines) {
            nsegs += (line.numVertices()-1);
         }
         double[] lens = new double[nsegs];
         double totalLength = 0;
         int k = 0;
         for (Polyline line : myLines) {
            for (int i=0; i<line.numVertices()-1; i++) {
               Point3d p0 = line.getVertex(i).getPosition();
               Point3d p1 = line.getVertex(i+1).getPosition();
               double len = p0.distance (p1);
               totalLength += len;
               lens[k++] = len;
            }
         }
         k = 0;
         for (Polyline line : myLines) {
            for (int i=0; i<line.numVertices()-1; i++) {
               Point3d p0 = line.getVertex(i).getPosition();
               Point3d p1 = line.getVertex(i+1).getPosition();
               M.addLineSegmentInertia (mass*lens[k++]/totalLength, p0, p1);
            }
         }
         return M;
      }
      else {
         return super.createInertia (mass, dist);
      }
   }

   /**
    * {@inheritDoc}
    */
   public boolean supportsMassDistribution (MassDistribution dist) {
      if (dist == MassDistribution.POINT || dist == MassDistribution.LENGTH) {
         return true;
      }
      else {
         return false;
      }
   }  
}
