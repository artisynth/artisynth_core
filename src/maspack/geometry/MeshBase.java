/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.awt.Color;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.CRC32;
import java.nio.ByteBuffer;

import maspack.matrix.AffineTransform3d;
import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.matrix.Vector;
import maspack.matrix.VectorTransformer3d;
import maspack.properties.HasProperties;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.Renderable;
import maspack.render.Renderer;
import maspack.render.Renderer.ColorInterpolation;
import maspack.render.Renderer.ColorMixing;
import maspack.render.Renderer.Shading;
import maspack.spatialmotion.SpatialInertia;
import maspack.util.InternalErrorException;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import maspack.geometry.io.GenericMeshWriter;
import maspack.geometry.io.GenericMeshReader;

/**
 * A "mesh" is a geometric object defined by a set of vertices, which are then
 * connected in some specific way.
 */
public abstract class MeshBase implements Renderable, Cloneable {
   
   public RigidTransform3d XMeshToWorld = new RigidTransform3d();
   protected boolean myXMeshToWorldIsIdentity = true;
   protected ArrayList<Vertex3d> myVertices = new ArrayList<Vertex3d>();

   protected int[] myIndexOffsets;

   protected ArrayList<Vector3d> myNormals;
   protected int[] myNormalIndices;
   protected boolean myAutoNormalsValidP = false;
   protected float[][] myRenderNormals;
   protected boolean myRenderNormalsValidP = false;
   
   protected ArrayList<float[]> myColors;
   protected int[] myColorIndices;

   protected ArrayList<Vector3d> myTextureCoords;
   protected int[] myTextureIndices;

   //protected int myAutoNormalCreation = -1;
   protected boolean myNormalsExplicitP = false;

   protected RenderProps myRenderProps = null;
   protected boolean isFixed = true;
   protected boolean myColorsFixed = true;
   protected boolean myTextureCoordsFixed = true;
   protected boolean myVertexColoringP = false;
   protected boolean myFeatureColoringP = false;
   
   int version = 0;          // used for detecting changes
   boolean modified = true;
   
   protected boolean myRenderBufferedP = false;
   RigidTransform3d myXMeshToWorldRender;
   protected Point3d myLocalMinCoords = new Point3d();
   protected Point3d myLocalMaxCoords = new Point3d();
   protected boolean myLocalBoundsValid = false;

   protected Point3d myWorldMinCoords = new Point3d();
   protected Point3d myWorldMaxCoords = new Point3d();
   protected boolean myWorldBoundsValid = false;
   protected double myWorldRadius = -1;

   protected ColorInterpolation myColorInterp = ColorInterpolation.RGB;
   protected ColorMixing myVertexColorMixing = ColorMixing.REPLACE;

   //protected int myWorldCoordCounter = 0;

   protected String myName;
   
   public void setName (String name) {
      myName = name;
   }

   public String getName () {
      return myName;
   }

   /**
    * Returns the interpolation method to be used for vertex-based coloring.
    * 
    * @return color interpolation method.
    */
   public ColorInterpolation getColorInterpolation() {
      return myColorInterp;
   }
   
   /**
    * Sets the interpolation method to be used for vertex-based coloring.
    * 
    * @param interp new color interpolation method
    * @return previous color interpolation method
    */
   public ColorInterpolation setColorInterpolation(ColorInterpolation interp) {
      ColorInterpolation prev = myColorInterp;
      myColorInterp = interp;
      return prev;
   }
   
   /**
    * Returns the color mixing method to be used for vertex-based coloring.
    * 
    * @return color mixing method.
    */
   public ColorMixing getVertexColorMixing() {
      return myVertexColorMixing;
   }
   
   /**
    * Sets the color mixing method to be used for vertex-based coloring.
    * 
    * @param cmix new color mixing method
    * @return previous color mixing method
    */
   public ColorMixing setVertexColorMixing(ColorMixing cmix) {
      ColorMixing prev = myVertexColorMixing;
      myVertexColorMixing = cmix;
      return prev;
   }
   
   /** 
    * Invalidates bounding box information. Can also be overriden to
    * mark any bounding volume hierarchies as invalid and in need of
    * updating.
    */
   protected void invalidateBoundingInfo () {
      //bvHierarchyValid = false;
      myLocalBoundsValid = false;
      myWorldBoundsValid = false;
      myWorldRadius = -1;
   }

   /** 
    * Invalidates bounding box information. Can also be overriden to
    * clear any bounding volume hierarchies so that they need to be rebuilt.
    */
   protected void clearBoundingInfo () {
      //bvHierarchyValid = false;
      myLocalBoundsValid = false;
      myWorldBoundsValid = false;
      myWorldRadius = -1;
   }
   
   /**
    * A version number that changes for ANY modifications,
    * including vertex position changes
    * @return version number
    */
   public int getVersion() {
      if (modified) {
         ++version;
         modified = false;
      }
      return version;
   }
   
   protected void notifyModified() {
      modified = true;
   }

   /**
    * Notifies this mesh that vertex positions have been modified, and cached
    * data dependent on these positions should be invalidated.
    */
   public void notifyVertexPositionsModified() {
      invalidateBoundingInfo();
      if (isFixed()) {
         notifyModified();
      }
      myAutoNormalsValidP = false;
   }
   
   /** 
    * Used internally and by subclasses to notify this mesh that its
    * topological structure has changed.
    */
   protected void notifyStructureChanged() {
      clearBoundingInfo();
      notifyModified();
      myIndexOffsets = null;
      // normals may be cleared too, but that will be handled elsewhere
      myAutoNormalsValidP = false;
   }

   /**
    * Invalidates any world coordinate data for this mesh. 
    */
   protected void invalidateWorldCoords() {
      myWorldBoundsValid = false;
      myWorldRadius = -1;
   }

   /**
    * Sets whether or not this mesh to is to be considered ``fixed''. A fixed
    * mesh is one for which the vertex coordinate values and normals are
    * considered constant. Rendering speeds can therefore be improved by
    * pre-allocating appropriate graphical display buffers.
    * 
    * <p>
    * By default, a mesh is set to be fixed unless the vertices are created
    * using referenced points (see {@link #addVertex addVertex}).
    * 
    * @param fixed
    * if true, sets this mesh to be considered fixed.
    * @see #isFixed
    */
   public void setFixed (boolean fixed) {
      if (fixed != isFixed()) {
         isFixed = fixed;
         notifyModified();
      }
   }

   /**
    * Returns whether or not this mesh is considered ``fixed'', as described in
    * {@link #setFixed setFixed}.
    * 
    * @return true if this mesh is fixed.
    */
   public boolean isFixed() {
      return isFixed;
   }


   private void augmentColors (int numc) {
      if (myColors == null) {
         myColors = new ArrayList<float[]>(numVertices());
      }
      float[] defaultColor = new float[] {0.5f, 0.5f, 0.5f, 1.0f};
      while (myColors.size() < numc) {
         myColors.add (copyColor (defaultColor));
      }
   }
   
   /**
    * Enables vertex coloring for this mesh. This creates a default color for
    * each existing vertex, and sets the color indices to the same as those
    * returned by {@link #createVertexIndices}.  The application can
    * subsequently set the colors for each vertex using the various
    * <code>setColor()</code> methods, or by accessing and modifying the
    * colors directly from {@link #getColors}. When vertex coloring is enabled,
    * the color and index sets are automatically adjusted whenever vertices or
    * features are added or removed.
    *
    * <p> 
    * Vertex coloring is disabled by {@link #clearColors}, {@link
    * #setFeatureColoringEnabled}, or any call to {@link #setColors}.
    */
   public void setVertexColoringEnabled () { 
      if (!myVertexColoringP) {
         augmentColors (numVertices());
         int[] idxs = createVertexIndices();
         if (!Arrays.equals (idxs, myColorIndices)) {
            myColorIndices = idxs;
         }
         myVertexColoringP = true;
         myFeatureColoringP = false;
         notifyModified();
      }
  }

   /**
    * Returns <code>true</code> if vertex coloring is enabled for this
    * mesh, as described for {@link #setVertexColoringEnabled}.
    *
    * @return <code>true</code> if vertex coloring is enabled.
    */
   public boolean getVertexColoringEnabled () {
      return myVertexColoringP;
   }

   /**
    * Returns {@code true} if the colors for this mesh are assigned on a
    * per-vertex basis. This will be true if vertex coloring is explicitly
    * enabled, <i>or</i> if the number of colors equals the number of vertices
    * and the vertex and color indexing is the same.
    */
   public boolean isVertexColored() {
      if (myVertexColoringP) {
         return true;
      }
      else if (myColors != null && myColors.size() == numVertices()) {
         int[] vertexIndices = createVertexIndices();
         return Arrays.equals (vertexIndices, myColorIndices);
      }
      else {
         return false;
      }
   }

   /**
    * Enables feature coloring for this mesh. This creates a default color for
    * each existing feature, and sets the color indices to the same as those
    * returned by {@link #createFeatureIndices}.  The application can
    * subsequently set the colors for each feature using the various
    * <code>setColor()</code> methods, or by accessing and modifying the
    * colors directly from {@link #getColors}. When feature coloring is
    * enabled, the color and index sets are automatically adjusted whenever
    * features are added or removed.
    *
    * <p> 
    * Feature coloring is disabled by {@link #clearColors}, {@link
    * #setVertexColoringEnabled}, or any call to {@link #setColors}.
    */
   public void setFeatureColoringEnabled () {
      if (!myFeatureColoringP) {
         augmentColors (numFeatures());
         int[] idxs = createFeatureIndices();
         if (!Arrays.equals (idxs, myColorIndices)) {
            myColorIndices = idxs;
         }
         myVertexColoringP = false;
         myFeatureColoringP = true;
         notifyModified(); 
      }
   }

   /**
    * Returns <code>true</code> if feature coloring is enabled for this
    * mesh, as described for {@link #setFeatureColoringEnabled}.
    *
    * @return <code>true</code> if feature coloring is enabled.
    */
   public boolean getFeatureColoringEnabled () {
      return myFeatureColoringP;
   }

   /**
    * Returns {@code true} if the colors for this mesh are assigned on a
    * per-feature basis. This will be true if feature coloring is explicitly
    * enabled, <i>or</i> if the number of colors equals the number of vertices
    * and the feature and color indexing is the same.
    */
   public boolean isFeatureColored() {
      if (myFeatureColoringP) {
         return true;
      }
      else if (myColors != null && myColors.size() == numFeatures()) {
         int[] featureIndices = createFeatureIndices();
         return Arrays.equals (featureIndices, myColorIndices);
      }
      else {
         return false;
      }
   }

   /**
    * {@inheritDoc}
    */
   public RenderProps getRenderProps() {
      return myRenderProps;
   }

   /**
    * Creates an appropriate RenderProps for this Mesh. If the supplied host is
    * non-null, then it is used to initialize any inherited properties. The
    * property name associated with the host is assumed to be "renderProps".
    * 
    * @param host
    * if non-null, is used to initialize inherited values
    * @return render properties appropriate for this mesh
    */
   public abstract RenderProps createRenderProps (HasProperties host);

   /**
    * {@inheritDoc}
    */
   public RenderProps createRenderProps() {
      return createRenderProps (null);
   }  

   /**
    * {@inheritDoc}
    */
   public void setRenderProps (RenderProps props) {
      if (props == null) {
         throw new IllegalArgumentException ("Render props cannot be null");
      }
      myRenderProps = createRenderProps();
      myRenderProps.set (props);
   }

   /**
    * Returns the spatial transform this mesh. At present, the spatial transform
    * is used only for rendering.
    * 
    * @param X
    * returns the transform from mesh to world coordinates
    */
   public void getMeshToWorld (RigidTransform3d X) {
      X.set (XMeshToWorld);
   }

   // XXX may want to remove, once mesh renderers move into geometry
   public RigidTransform3d getXMeshToWorldRender() {
      return myXMeshToWorldRender;
   }

   // XXX may want to remove, once mesh renderers move into geometry
   public float[] getRenderNormal (int idx) {
      return myRenderNormals[idx];
   }

   /**
    * Returns true if the mesh to world transform is the identity.
    * 
    * @return true if the spatial transform is the identity.
    */
   public boolean meshToWorldIsIdentity() {
      return myXMeshToWorldIsIdentity;
   }

   /**
    * Returns a pointer to the current mesh to world transform. This should
    * <i>not</i> be modified by the caller.
    * 
    * @return current mesh to world transform
    */
   public RigidTransform3d getMeshToWorld() {
      return XMeshToWorld;
   }

   /**
    * Sets the spatial transform this mesh. At present, the spatial transform is
    * used only for rendering. If the transform is the identity, then
    * {@link #meshToWorldIsIdentity meshToWorldIsIdentity} will subsequently
    * return true.
    * 
    * @param X
    * transform from mesh to world coordinates
    */
   public void setMeshToWorld (RigidTransform3d X) {
      XMeshToWorld.set (X);
      myXMeshToWorldIsIdentity = X.equals (RigidTransform3d.IDENTITY);
      invalidateWorldCoords();
   }
   
   public void transformToWorld (Point3d pnt) {
      if (!myXMeshToWorldIsIdentity) {
         pnt.transform (XMeshToWorld);
      }
   }

   public void transformToLocal (Point3d pnt) {
      if (!myXMeshToWorldIsIdentity) {
         pnt.inverseTransform (XMeshToWorld);
      }
   }

   public void transformToWorld (Vector3d pnt) {
      if (!myXMeshToWorldIsIdentity) {
         pnt.transform (XMeshToWorld.R);
      }
   }

   public void transformToLocal (Vector3d pnt) {
      if (!myXMeshToWorldIsIdentity) {
         pnt.inverseTransform (XMeshToWorld.R);
      }
   }

   /**
    * Returns this mesh's vertices. The vertices are contained within an
    * ArrayList, each element of which is of type
    * {@link maspack.geometry.Vertex3d Vertex3d}. Modifying these elements will
    * modify the mesh.
    * 
    * @return list of this mesh's vertices.
    */
   public ArrayList<Vertex3d> getVertices() {
      return myVertices;
   }

   /**
    * Returns the number of vertices in this mesh.
    * 
    * @return number of vertices in this mesh
    */
   public int numVertices() {
      return myVertices.size();
   }

   /**
    * Returns the idx-th vertex in this mesh.
    *
    * @param idx index of the desired vertex
    * @return idx-th vertex
    */
   public Vertex3d getVertex (int idx) {
      return myVertices.get(idx);
   }

   /**
    * Returns the number of features in this mesh. What a feature is depends on
    * the mesh type: faces for PolygonalMesh, lines for PolylineMesh, points
    * (which map to vertices) for PointMesh.
    *
    * @return number of features in this mesh
    */
   public abstract int numFeatures();

   /** 
    * Returns the minimum and maximum coordinates for this mesh, in mesh local
    * coordinates.
    * 
    * @param pmin (optional) minimum point of the bounding box 
    * @param pmax (optional) maximum point of the bounding box 
    */   
   public void getLocalBounds (Vector3d pmin, Vector3d pmax) {
      if (!myLocalBoundsValid) {
         recomputeLocalBounds();
      }
      if (pmin != null) {
         pmin.set (myLocalMinCoords);         
      }
      if (pmax != null) {
         pmax.set (myLocalMaxCoords);
      }
   }

   /** 
    * Returns the minimum and maximum coordinates for this mesh, in world
    * coordinates.
    * 
    * @param pmin (optional) minimum point of the bounding box 
    * @param pmax (optional) maximum point of the bounding box 
    */   
   public void getWorldBounds (Vector3d pmin, Vector3d pmax) {
      if (!myWorldBoundsValid) {
         recomputeWorldBounds();
      }
      if (pmin != null) {
         pmin.set (myWorldMinCoords);         
      }
      if (pmax != null) {
         pmax.set (myWorldMaxCoords);
      }
   }

   /**
    * Creates an empty polyline mesh.
    */
   public MeshBase() {
      myRenderProps = createRenderProps();
   }


   /**
    * Reads the contents of this mesh from a Reader. The input format
    * depends on the specific mesh type.
    * 
    * @param reader
    * supplies the input description of the mesh
    */
   public void read (Reader reader) throws IOException {
      read (new ReaderTokenizer (reader), false);
   }

   /**
    * Reads the contents of this mesh from a Reader. The input format
    * depends on the specific mesh type.
    * 
    * @param reader
    * supplies the input description of the mesh
    * @param zeroIndexed
    * if true, the index numbering for mesh vertices starts at 0. Otherwise,
    * numbering starts at 1.
    */
   public void read (Reader reader, boolean zeroIndexed) throws IOException {
      read (new ReaderTokenizer (reader), zeroIndexed);
   }

   /**
    * Reads the contents of this mesh from a ReaderTokenizer. The input format
    * depends on the specific mesh type.
    * 
    * @param rtok tokenizer providing the input
    * @param zeroIndexed
    * if true, the index numbering for mesh vertices starts at 0. Otherwise,
    * numbering starts at 1.
    */
   public abstract void read (ReaderTokenizer rtok, boolean zeroIndexed)
      throws IOException;

   protected void recomputeLocalBounds() {
      Point3d pmin = myLocalMinCoords;
      Point3d pmax = myLocalMaxCoords;
      if (myVertices.size() == 0) {
         pmin.set (0, 0, 0);
         pmax.set (0, 0, 0);
      }
      else {
         double inf = Double.POSITIVE_INFINITY;
         pmin.set (inf, inf, inf);
         pmax.set (-inf, -inf, -inf);
      }
      for (Vertex3d vertex : myVertices) {
         Point3d pnt = vertex.pnt;
         if (pnt.x > pmax.x) {
            pmax.x = pnt.x;
         }
         if (pnt.x < pmin.x) {
            pmin.x = pnt.x;
         }
         if (pnt.y > pmax.y) {
            pmax.y = pnt.y;
         }
         if (pnt.y < pmin.y) {
            pmin.y = pnt.y;
         }
         if (pnt.z > pmax.z) {
            pmax.z = pnt.z;
         }
         if (pnt.z < pmin.z) {
            pmin.z = pnt.z;
         }
      }
      myLocalBoundsValid = true;
   }

   private static double inf = Double.POSITIVE_INFINITY;

   protected void recomputeWorldBounds() {
      Point3d pnt = new Point3d();
      Point3d pmin = myWorldMinCoords;
      Point3d pmax = myWorldMaxCoords;
      if (myVertices.size() == 0) {
         pmin.set (0, 0, 0);
         pmax.set (0, 0, 0);
      }
      else {
         pmin.set (inf, inf, inf);
         pmax.set (-inf, -inf, -inf);
      }
      for (Vertex3d vertex : myVertices) {
         if (!myXMeshToWorldIsIdentity) {
            pnt.transform (XMeshToWorld, vertex.pnt);
         }
         else {
            pnt.set (vertex.pnt);
         }
         if (pnt.x > pmax.x) {
            pmax.x = pnt.x;
         }
         if (pnt.x < pmin.x) {
            pmin.x = pnt.x;
         }
         if (pnt.y > pmax.y) {
            pmax.y = pnt.y;
         }
         if (pnt.y < pmin.y) {
            pmin.y = pnt.y;
         }
         if (pnt.z > pmax.z) {
            pmax.z = pnt.z;
         }
         if (pnt.z < pmin.z) {
            pmin.z = pnt.z;
         }
      }
      myWorldBoundsValid = true;
   }

   /**
    * Adds a vertex to the set of vertices associated with this mesh. The
    * {@link maspack.geometry.Vertex3d#pnt pnt} field of the vertex should be
    * non-null. The index of the vertex will be set to reflect it's position in
    * the list of vertices.
    * 
    * @param vtx
    * vertex to add
    * @see #addVertex(Point3d,boolean)
    * @see #addVertex(Point3d)
    */
   public void addVertex (Vertex3d vtx) {
      if (vtx.pnt == null) {
         throw new IllegalArgumentException ("No point defined for vertex");
      }
      vtx.setIndex (myVertices.size());
      vtx.setMesh (this);
      myVertices.add (vtx);
      if (myVertexColoringP) {
         myColors.add (new float[] {0.5f, 0.5f, 0.5f, 1.0f});               
         myColorIndices = null; // will be rebuilt on demand
      }
      notifyStructureChanged();
   }
   
   /**
    * Removes a vertex from this mesh.
    *
    * @param vtx
    * vertex to remove
    * @return true if the vertex was present
    */
   public boolean removeVertexFast (Vertex3d vtx) {
      int idx = vtx.getIndex();
      int last = myVertices.size()-1;
      if (idx >= 0 && idx <= last ) {
         myVertices.set(idx, myVertices.get(last));
         myVertices.get(idx).setIndex(idx);
         myVertices.remove(last);
         if (myVertexColoringP) {
            myColors.remove (last);
            myColorIndices = null; // will be rebuilt on demand
         }

         vtx.setMesh (null);
         vtx.setIndex(-1);
         notifyStructureChanged();
         return true;
      } else {
         return false;
      }
   }

   /**
    * Removes a vertex from this mesh.
    *
    * @param vtx
    * vertex to remove
    * @return true if the vertex was present
    */
   public boolean removeVertex (Vertex3d vtx) {
      
      if (myVertices.remove (vtx)) {
         if (myVertexColoringP) {
            myColors.remove (vtx.idx);
            myColorIndices = null; // will be rebuilt on demand
         }
         // reindex subsequent vertices
         
         for (int i=vtx.getIndex(); i<myVertices.size(); i++) {
            myVertices.get(i).setIndex (i);
         }
         vtx.setMesh (null);
         notifyStructureChanged();
         return true;
      }
      else {
         return false;
      }
   }
   
   /**
    * Removes a set of vertices from this mesh, as indicated by a collection.
    *
    * @param vertices Collection of vertices to remove
    */
   public ArrayList<Integer> removeVertices (Collection<Vertex3d> vertices) {
      ArrayList<Integer> deleteIdxs = new ArrayList<Integer>();
      for (Vertex3d v : vertices) {
         if (myVertices.get(v.getIndex()) == v) {
            deleteIdxs.add (v.getIndex());
         }
      }
      if (deleteIdxs.size() > 0) {
         Collections.sort (deleteIdxs);
         // remove non-unique vertices
         for (int i=1; i<deleteIdxs.size(); ) {
            if (deleteIdxs.get(i) == deleteIdxs.get(i-1)) {
               deleteIdxs.remove (i);
            }
            else {
               i++;
            }
         }
         ArrayList<Vertex3d> newVertexList =
            new ArrayList<Vertex3d>(myVertices.size()-deleteIdxs.size());
         int k = 0;
         for (int i=0; i<myVertices.size(); i++) {
            Vertex3d v = myVertices.get(i);
            if (k < deleteIdxs.size() && i == deleteIdxs.get(k)) {
               // delete v
               v.setMesh (null);
               k++;
            }
            else {
               v.setIndex (newVertexList.size());
               newVertexList.add (v);
            }
         }
         myVertices = newVertexList;
         notifyStructureChanged();
         return deleteIdxs;
      }
      else {
         return null;
      }
   }

   public boolean containsVertex(Vertex3d vtx) {
      return (myVertices.contains(vtx));
   }

   /**
    * Adds a point to the set of vertices associated with this mesh. Equivalent
    * to calling {@link #addVertex(Point3d,boolean) addVertex(pnt,false)}.
    * 
    * @param pnt
    * vertex point to add
    * @return vertex object containing the point
    * @see #addVertex(Point3d,boolean)
    */
   public Vertex3d addVertex (Point3d pnt) {
      return addVertex (pnt, false);
   }

   /**
    * Adds a point to the set of vertices associated with this mesh.
    * 
    * @param x
    * vertex x coordinate
    * @param y
    * vertex y coordinate
    * @param z
    * vertex z coordinate
    * @return vertex object containing the point
    */
   public Vertex3d addVertex (double x, double y, double z) {
      return addVertex (new Point3d (x, y, z), false);
   }

   /**
    * Adds a point to the set of vertices associated with this mesh.
    * 
    * @param pnt
    * vertex point to add
    * @param byReference
    * if true, then the supplied Point3d argument is not copied but is referred
    * to directly by the vertex structure. The mesh will track any changes to
    * this point and {@link #isFixed isFixed} will return false.
    * @return vertex object containing the point
    * @see #addVertex(Point3d)
    */
   public Vertex3d addVertex (Point3d pnt, boolean byReference) {
      Vertex3d vtx;
      if (byReference) {
         vtx = new Vertex3d (pnt);
      }
      else {
         vtx = new Vertex3d (new Point3d (pnt));
      }
      addVertex (vtx);
      return vtx;
   }

   /**
    * Reads this mesh from a file, with the file format being inferred from the
    * file name suffix.
    * 
    * @param file
    * file containing the mesh description
    * @throws IOException if an I/O error occurred or if the file
    * type is not compatible with polygonal meshes
    */
   public void read (File file) throws IOException {
      clear();      
      GenericMeshReader.readMesh (file, this);
   }

   /**
    * Reads this mesh from a file, with the file format being inferred from the
    * file name suffix.
    * 
    * <p>For Alias Wavefront obj files (with extension {@code .obj}), {@code
    * zeroIndexed} can be used to indicate that the numbering of vertex indices
    * starts at 0. Otherwise, numbering starts at 1.
    * 
    * @param file
    * file containing the mesh description
    * @param zeroIndexed
    * if true, index numbering for mesh vertices starts at 0. Otherwise,
    * numbering starts at 1.
    * @throws IOException if an I/O error occurred or if the file
    * type is not compatible with polygonal meshes
    */
   public void read (File file, boolean zeroIndexed) throws IOException {
      clear();      
      GenericMeshReader reader = new GenericMeshReader (file);
      reader.setZeroIndexed (true);
      reader.readMesh (this);
   }

   /**
    * Writes this mesh to a File, with the file format being inferred from the
    * file name suffix.
    * 
    * @param file
    * File to write this mesh to
    * @throws IOException if an I/O error occurred or if the file format is not
    * compatible with the mesh type
    */
   public void write (File file) throws IOException {
      GenericMeshWriter writer = new GenericMeshWriter (file);
      writer.setFormat ("%g");
      writer.writeMesh (this);
   }

   /**
    * Writes this mesh to a File, with the file format being inferred from the
    * file name suffix.
    *
    * <p>For text file formats, the optional argument {@code
    * fmtStr} supplies a C <code>printf</code> style format string used for
    * printing the vertex coordinates. For a description of the format string
    * syntax, see {@link maspack.util.NumberFormat NumberFormat}. Good default
    * choices for <code>fmtStr</code> are either <code>"%g"</code> (full
    * precision), or <code>"%.Ng"</code>, where <i>N</i> is the number of
    * desired significant figures. If {@code fmtStr} is {@code null}, full
    * precision is assumed.
    *
    * <p>For Alias Wavefront obj files (with extension {@code .obj}), {@code
    * zeroIndexed} can be used to request that the numbering of vertex indices
    * starts at 0. Otherwise, numbering starts at 1.
    * 
    * @param file
    * File to write this mesh to
    * @param fmtStr optional format string for mesh vertices
    * @param zeroIndexed
    * if true, index numbering for mesh vertices starts at 0. Otherwise,
    * numbering starts at 1.
    * @throws IOException if an I/O error occurred or if the file format is not
    * compatible with the mesh type
    */
   public void write (File file, String fmtStr, boolean zeroIndexed)
      throws IOException {
      GenericMeshWriter writer = new GenericMeshWriter (file);
      if (fmtStr != null) {
         writer.setFormat (fmtStr);
      }
      writer.setZeroIndexed (true);
      writer.writeMesh (this);
   }

   /**
    * Writes this mesh to a PrintWriter. The exact output format will depend on
    * the specific mesh type. Index numbering starts at one, and the format
    * used to print vertex coordinates is specified by a C <code>printf</code>
    * style format string contained in the parameter <code>fmtStr</code>. For a
    * description of the format string syntax, see {@link
    * maspack.util.NumberFormat NumberFormat}. Good default choices for
    * <code>fmtStr</code> are either <code>"%g"</code> (full precision), or
    * <code>"%.Ng"</code>, where <i>N</i> is the number of desired significant
    * figures.
    * 
    * @param pw
    * PrintWriter to write this mesh to
    * @param fmtStr
    * string specifying the format for writing the vertex coordinates
    */
   public void write (PrintWriter pw, String fmtStr) throws IOException {
      write (pw, new NumberFormat(fmtStr), false);
   }

   /**
    * Writes this mesh to a PrintWriter. The exact output format will
    * depend on the specific mesh type.
    * 
    * @param pw
    * PrintWriter to write this mesh to
    * @param fmt
    * format for writing the vertex coordinates
    * @param zeroIndexed
    * if true, index numbering for mesh vertices starts at 0. Otherwise,
    * numbering starts at 1.
    */
   public abstract void write (
      PrintWriter pw, NumberFormat fmt, boolean zeroIndexed) throws IOException;

   /**
    * Scales the vertices of this mesh. The topology remains unchanged.
    * 
    * @param s
    * scale factor
    */
   public void scale (double s) {
      for (Vertex3d vertex : myVertices) {
         vertex.pnt.scale (s);
      }
      notifyVertexPositionsModified();
   }
   
   /**
    * Scales the vertices of this mesh in the given directions. 
    * The topology remains unchanged.
    * 
    * @param sx
    * scaling factor in the x direction
    * @param sy
    * scaling factor in the y direction
    * @param sz
    * scaling factor in the z direction
    */
   public void scale (double sx, double sy, double sz) {
      for (Vertex3d vertex : myVertices) {
         vertex.pnt.scale (sx, sy, sz);
      }
      if (hasExplicitNormals()) {
         ArrayList<Vector3d> normals = getNormals();
         if (normals != null) {
            for (Vector3d n : normals) {
               n.x *= 1/sx;
               n.y *= 1/sy;
               n.z *= 1/sz;
               n.normalize();
            }
         }
      }
      else {
         clearNormals();
      }
      notifyVertexPositionsModified();
   }   

   /*
    * Return the dimensions of the smallest axis-aligned box containing the
    * vertices of the mesh, in mesh coordinates.
    */
   public Point3d size() {
      double inf = Double.POSITIVE_INFINITY;
      Point3d pmin = new Point3d (inf, inf, inf);
      Point3d pmax = new Point3d (-inf, -inf, -inf);
      updateBounds (pmin, pmax);
      pmin.sub (pmax, pmin);
      return pmin;
   }

   /**
    * Computes an approximate radius for this mesh, determined as
    * half the diagonal of the corners of its bounding box.
    *
    * @return approximate radius for this mesh
    */
   public double getRadius () {
      if (myWorldRadius == -1) {
         // compute world radius
         if (myVertices.size() > 0) {
            if (!myWorldBoundsValid) {
               recomputeWorldBounds();
            }
            myWorldRadius = 0.5*myWorldMinCoords.distance (myWorldMaxCoords);
         }
         else {
            myWorldRadius = 0;
         }
      }
      return myWorldRadius;
   }

   /**
    * {@inheritDoc}
    */
   public void updateBounds (Vector3d pmin, Vector3d pmax) {
      if (myVertices.size() > 0) {
         if (!myWorldBoundsValid) {
            recomputeWorldBounds();
         }
         myWorldMinCoords.updateBounds (pmin, pmax);
         myWorldMaxCoords.updateBounds (pmin, pmax);
      }
   }

   /**
    * Translates the vertices of this mesh. The topology of the mesh remains
    * unchanged.
    * 
    * @param off
    * offset to add to the mesh vertices
    */
   public void translate (Vector3d off) {
      for (Vertex3d vertex : myVertices) {
         vertex.pnt.add (off);
      }
      notifyVertexPositionsModified();
   }

   /**
    * Translates the vertices of this mesh so that its origin coincides
    * with the centroid. The topology of the mesh remains unchanged.
    * 
    * @return the resulting rigid transform that was applied to the mesh
    */
   public RigidTransform3d translateToCentroid () {
      Vector3d off = new Vector3d();
      computeCentroid (off);
      off.negate();
      translate (off);
      return new RigidTransform3d (off.x, off.y, off.z);
   }

   /**
    * Transforms this mesh so that its center and orientation are aligned
    * with its oriented bounding box (OBB) as computed by {@link #computeOBB()}.
    *
    * @return the resulting rigid transform that was applied to the mesh
    */
   public RigidTransform3d transformToOBB () {
      OBB obb = computeOBB();
      RigidTransform3d T = new RigidTransform3d();
      T.invert (obb.getTransform());
      transform (T);
      return T;
   }
   
   /**
    * Transforms this mesh so that its center and orientation are aligned
    * with its oriented bounding box (OBB) as computed by 
    * {@link #computeOBB(OBB.Method)}.
    * 
    * @param method method used to compute the OBB.
    * @return the resulting rigid transform that was applied to the mesh
    */
   public RigidTransform3d transformToOBB (OBB.Method method) {
      OBB obb = computeOBB(method);
      RigidTransform3d T = new RigidTransform3d();
      T.invert (obb.getTransform());
      transform (T);
      return T;
   }
   
   /**
    * Applies an affine transformation to the vertices of this mesh. The
    * topology of the mesh remains unchanged.
    * 
    * @param X
    * affine transformation
    */
   public void transform (AffineTransform3dBase X) {
      for (Vertex3d vertex : myVertices) {
         vertex.pnt.transform (X);
      }
      if (myNormalsExplicitP) {
         Matrix3d A = new Matrix3d(X.getMatrix());
         if (X instanceof AffineTransform3d) {
            A.invert();
            A.transpose();
         }
         for (int i=0; i<myNormals.size(); i++) {
            Vector3d nrm = myNormals.get(i);
            A.mul (nrm, nrm);
            nrm.normalize();
         }
      }
      else {
         clearNormals(); // auto normals will be regenerated
      }
      invalidateBoundingInfo();
      notifyModified();
   }

   /**
    * Applies an inverse affine transformation to the vertices of this mesh, in
    * local mesh coordinates. The topology of the mesh remains unchanged.
    * 
    * @param X
    * affine transformation
    */
   public void inverseTransform (AffineTransform3dBase X) {
      for (Vertex3d vertex : myVertices) {
         vertex.pnt.inverseTransform (X);
      }
      if (myNormalsExplicitP) {
         Matrix3d A = new Matrix3d(X.getMatrix());
         A.transpose();
         for (int i=0; i<myNormals.size(); i++) {
            Vector3d nrm = myNormals.get(i);
            A.mul (nrm, nrm);
            nrm.normalize();
         }
      }
      else {
         clearNormals(); // auto normals will be regenerated
      }
      invalidateBoundingInfo();
      notifyModified();
   }

   /**
    * Applies a transform to the vertices and normals of this mesh, in
    * local mesh coordinates. The topology of the mesh remains unchanged.
    * 
    * @param trans transformation to apply
    */
   public void transform (VectorTransformer3d trans) {
      for (Vertex3d vertex : myVertices) {
         trans.transformPnt (vertex.pnt, vertex.pnt);
      }
      if (myNormalsExplicitP) {
         for (int i=0; i<myNormals.size(); i++) {
            Vector3d nrm = myNormals.get(i);
            trans.transformCovec (nrm, nrm);
            nrm.normalize();
         }
      }
      else {
         clearNormals(); // auto normals will be regenerated
      }
      invalidateBoundingInfo();
      notifyModified();
   }

   /**
    * Applies an inverse transform to the vertices and normals of this mesh, in
    * local mesh coordinates. The topology of the mesh remains unchanged.
    * 
    * @param trans transformation to apply
    */
   public void inverseTransform (VectorTransformer3d trans) {
      for (Vertex3d vertex : myVertices) {
         trans.inverseTransformPnt (vertex.pnt, vertex.pnt);
      }
      if (myNormalsExplicitP) {
         for (int i=0; i<myNormals.size(); i++) {
            Vector3d nrm = myNormals.get(i);
            trans.inverseTransformCovec (nrm, nrm);
            nrm.normalize();
         }
      }
      else {
         clearNormals(); // auto normals will be regenerated
      }
      invalidateBoundingInfo();
      notifyModified();
   }

   // /**
   //  * Applies a geometric transformation to the vertices of this mesh, in local
   //  * mesh coordinates. The topology of the mesh remains unchanged.
   //  *
   //  * @param X transformer
   //  */
   // public void transform (GeometryTransformer X) {
   //    for (Vertex3d vertex : myVertices) {
   //       X.transformPnt (vertex.pnt);
   //    }
   //    notifyVertexPositionsModified();
   // }

   // /**
   //  * Applies a geometric transformation to both the vertices of this mesh and
   //  * the mesh's mesh-to-world transform TMW, in world coordinates. The mesh
   //  * vertex positions <code>p</code> are modified to accomodate that of part
   //  * of the transformation not provided by the change to TMW. Specifically,
   //  * <pre>
   //  * p' = TMWnew X (TMW p) 
   //  * </pre>
   //  * where <code>X( )</code> indicates the
   //  * transform applied by <code>X</code> and TMWnew is the transformed
   //  * value of <code>TMW</code>.
   //  * 
   //  * @param X transformer
   //  */
   // public void transformWorld (GeometryTransformer X) {

   //    RigidTransform3d TMWnew = new RigidTransform3d();
   //    X.transform (TMWnew, XMeshToWorld);

   //    Point3d p = new Point3d();
   //    for (Vertex3d vertex : myVertices) {
   //       // Would be nice to do this in one operation for cases where X is a
   //       // uniform affine transform. Can't do that though and still get the
   //       // right answer when X is working in "undo" mode
   //       p.transform (XMeshToWorld, vertex.pnt);
   //       X.transformPnt (p);
   //       p.inverseTransform (TMWnew);
   //       vertex.pnt.set (p);
   //    }
   //    setMeshToWorld (TMWnew);
   //    notifyVertexPositionsModified();
   // }

   /**
    * {@inheritDoc}
    */
   public int getRenderHints() {
      return 0;
   }
   
   protected void clearAttributes() {
      clearNormals();
      clearTextureCoords();
      clearColors();
   }

   protected void adjustAttributesForNewFeature () {
      clearNormals();
      clearTextureCoords();
      if (myVertexColoringP) {
         // will be recomputed on demand
         myColorIndices = null;  
      }
      else if (myFeatureColoringP) {
         myColors.add (new float[] {0.5f, 0.5f, 0.5f, 1.0f}); 
         myColorIndices = null; // will be recomputed on demand
      }
      else {
         clearColors();
      }
   }

   private int[] createCulledIndices (int[] indices, int fidx) {
      int[] indexOffs = getFeatureIndexOffsets();
      int delcnt = 0;
      if (fidx+1 < indexOffs.length) {
         delcnt = indexOffs[fidx+1] - indexOffs[fidx];
      }
      int[] newIndices = new int[indices.length-delcnt];
      int k = 0;
      for (int i=0; i<indexOffs[fidx]; i++) {
         newIndices[k++] = indices[i];
      }
      if (delcnt > 0) {
         for (int i=indexOffs[fidx+1]; i<indices.length; i++) {
            newIndices[k++] = indices[i];
         }
      }
      return newIndices;
   }

   private int[] createCulledIndices (int[] indices, List<Integer> fidxs) {
      int[] indexOffs = getFeatureIndexOffsets();
      int delcnt = 0;
      for (int fidx : fidxs) {
         delcnt += (indexOffs[fidx+1] - indexOffs[fidx]);
      }
      int[] newIndices = new int[indices.length-delcnt];
      int k = 0;
      int lasti = 0;
      for (int fidx : fidxs) {
         for (int i=lasti; i<indexOffs[fidx]; i++) {
            newIndices[k++] = indices[i];            
         }
         lasti = indexOffs[fidx+1];
      }
      for (int i=lasti; i<indices.length; i++) {
         newIndices[k++] = indices[i];            
      }
      return newIndices;
   }

   protected void adjustAttributesForRemovedFeature (int fidx) {
      if (myNormalsExplicitP) {
         myNormalIndices = createCulledIndices (myNormalIndices, fidx);
      }
      else {
         clearNormals();
      }
      if (myColors != null) {
         if (myVertexColoringP) {
            myColorIndices = null; // will be recomputed on demand
         }
         else if (myFeatureColoringP) {
            myColors.remove (fidx);
            myColorIndices = null; // will be recomputed on demand
         }
         else {
            myColorIndices = createCulledIndices (getColorIndices(), fidx);
         }
      }
      if (myTextureCoords != null) {
         myTextureIndices =
            createCulledIndices (myTextureIndices, fidx);
      }
   }

   protected void adjustAttributesForRemovedFeatures (List<Integer> fidxs) {
      // fidxs contains the indices of the feature to be removed, sorted into
      // ascending order
      if (myNormalsExplicitP) {
         myNormalIndices = createCulledIndices (myNormalIndices, fidxs);
      }
      else {
         clearNormals();
      }
      if (myColors != null) {
         if (myVertexColoringP) {
            myColorIndices = null; // will be recomputed on demand
         }
         else if (myFeatureColoringP) {
            ArrayList<float[]> newColors =
               new ArrayList<float[]>(myColors.size()-fidxs.size());
            int k = 0;
            for (int i=0; i<myColors.size(); i++) {
               float[] color = myColors.get(i);
               if (i == fidxs.get(k)) {
                  // delete color
                  while (i == fidxs.get(k) && k < fidxs.size()-1) {
                     // loop just in case fidxs contains redundant numbers
                     k++;
                  }   
               }
               else {
                  newColors.add (color);
               }
            }
            myColors = newColors;               
            myColorIndices = null; // will be recomputed on demand
         }
         else {
            myColorIndices = createCulledIndices (getColorIndices(), fidxs);
         }
      }
      if (myTextureCoords != null) {
         myTextureIndices =
            createCulledIndices (myTextureIndices, fidxs);
      }
   }

   protected void adjustAttributesForClearedFeatures() {
      if (myNormalsExplicitP) {
         myNormalIndices = new int[0];
      }
      else {
         clearNormals();
      }
      if (myColors != null) {
         if (myVertexColoringP) {
            myColorIndices = null; // will be recomputed on demand
         }
         else if (myFeatureColoringP) {
            myColors = new ArrayList<float[]>();
            myColorIndices = null; // will be recomputed on demand
         }
         else {
            myColorIndices = new int[0];
         }
      }
      if (myTextureCoords != null) {
         myTextureIndices = new int[0];
      }
   }

   /**
    * Clears this mesh (makes it empty).
    */
   public void clear() {
      // verts = null;
      for (Vertex3d vtx : myVertices) {
         vtx.setMesh (null);
      }
      myVertices.clear();
      clearNormals();
      clearColors();
      clearTextureCoords();
      notifyStructureChanged();
      myLocalMinCoords.set (0, 0, 0);
      myLocalMaxCoords.set (0, 0, 0);
      myWorldMinCoords.set (0, 0, 0);
      myWorldMaxCoords.set (0, 0, 0);
      notifyModified();
   }

   /**
    * Returns true if this mesh is empty.
    * 
    * @return true if mesh is empty
    */
   public boolean isEmpty() {
      return myVertices.size() == 0;
   }

   public void getSelection (LinkedList<Object> list, int qid) {
   }
   
   public boolean isSelectable() {
      return false;
   }

   public int numSelectionQueriesNeeded() {
      return -1;
   }

   public void prerender (RenderList list) {
      prerender (myRenderProps);
   }
   
   public void prerender (RenderProps props) {
      saveRenderInfo (myRenderProps);      
   }

   protected int[] copyWithOffset (int[] idxs, int off) {
      int[] newIdxs = new int[idxs.length];
      for (int i=0; i<idxs.length; i++) {
         newIdxs[i] = idxs[i]+off;
      }
      return newIdxs;
   }
   
   protected void printIdxs (String name, int[] idxs) {
      System.out.print (name + ":");
      for (int i=0; i<idxs.length; i++) {
         System.out.print (" " + idxs[i]);
      }
      System.out.println ("");
   }

   public void setRenderBuffered (boolean enable) {
      if (enable) { // do a save render info right away to be ready for the
                     // next render
         saveRenderInfo(myRenderProps);
      }
      // set myRenderBufferedP last because otherwise render might occur
      // before saveRenderInfo is complete
      myRenderBufferedP = enable;
   }

   public boolean isRenderBuffered() {
      return myRenderBufferedP;
   }

   protected boolean maybeRebuildVertexRenderNormals() {
      if (!myRenderNormalsValidP) {
         ArrayList<Vector3d> normals = getNormals();
         synchronized (this) {
            if (normals == null) {
               myRenderNormals = null;
            }
            else {
               myRenderNormals = new float[normals.size()][];
               for (int i=0; i<myRenderNormals.length; i++) {
                  myRenderNormals[i] = new float[3];
               }               
            }
         }
         myRenderNormalsValidP = true;
         return true;
      }
      else {
         return false;
      }
   }

   protected void updateVertexRenderNormals() {
      ArrayList<Vector3d> normals = getNormals();
      if (normals != null) {
         for (int i=0; i<normals.size(); i++) {
            Vector3d nrm = normals.get(i);
            float[] vec = myRenderNormals[i];
            vec[0] = (float)nrm.x;
            vec[1] = (float)nrm.y;
            vec[2] = (float)nrm.z;
         }
      }
   }      

   public void saveRenderInfo (RenderProps props) {
      if (myXMeshToWorldRender == null) {
         myXMeshToWorldRender = new RigidTransform3d();
      }
      myXMeshToWorldRender.set (XMeshToWorld);
      if (!isFixed) {
         for (int i = 0; i < myVertices.size(); i++) {
            myVertices.get (i).saveRenderInfo();
         }
      }
      if (props.getShading() != Shading.FLAT) {
         if (maybeRebuildVertexRenderNormals() || !isFixed()) {
            updateVertexRenderNormals();
         }
      }
   }

   /** 
    * Creates a copy of this mesh.
    */
   public MeshBase copy() {
      return clone();
   }

   /**
    * Creates a clone of this mesh.
    */
   public MeshBase clone() {
      MeshBase mesh = null;
      try {
         mesh = (MeshBase)super.clone();
      }
      catch (CloneNotSupportedException e) {
         throw new InternalErrorException ("Can't clone MeshBase: " + e);
      }
      
      mesh.myVertices = new ArrayList<Vertex3d>();
      for (int i=0; i<myVertices.size(); i++) {
         mesh.addVertex (myVertices.get(i).pnt);
      }
      mesh.XMeshToWorld = new RigidTransform3d (XMeshToWorld);
      mesh.myIndexOffsets = null; // will regenerate

      if (myNormals != null) {
         mesh.myNormals = new ArrayList<Vector3d>();
         for (int i=0; i<myNormals.size(); i++) {
            mesh.myNormals.add (new Vector3d(myNormals.get(i)));
         }
      }
      if (myNormalIndices != null) { // should be same as myNormals != null
         mesh.myNormalIndices =
            Arrays.copyOf (myNormalIndices, myNormalIndices.length);
      }
      mesh.myRenderNormalsValidP = false;

      if (myTextureCoords != null) {
         mesh.myTextureCoords = new ArrayList<Vector3d>();
         for (int i=0; i<myTextureCoords.size(); i++) {
            mesh.myTextureCoords.add (new Vector3d(myTextureCoords.get(i)));
         }
         mesh.myTextureIndices =
            Arrays.copyOf (myTextureIndices, myTextureIndices.length);
      }

      mesh.myVertexColoringP = myVertexColoringP;
      mesh.myFeatureColoringP = myFeatureColoringP;
      if (myColors != null) {
         mesh.myColors = new ArrayList<float[]>();
         for (int i=0; i<myColors.size(); i++) {
            mesh.myColors.add (copyColor (myColors.get(i)));
         }
         int[] colorIndices = getColorIndices();
         mesh.myColorIndices =
            Arrays.copyOf (colorIndices, colorIndices.length);
      }

      if (myRenderProps != null) {
         mesh.setRenderProps (myRenderProps);
      }
      else {
         mesh.myRenderProps = null;
      }
      mesh.setFixed (isFixed());
      mesh.setColorsFixed (isColorsFixed());
      mesh.setTextureCoordsFixed (isTextureCoordsFixed ());
      mesh.setColorInterpolation (getColorInterpolation());
      mesh.setRenderBuffered (isRenderBuffered());

      mesh.myLocalMinCoords = new Point3d();
      mesh.myLocalMaxCoords = new Point3d();
      mesh.myLocalBoundsValid = false;

      mesh.myWorldMinCoords = new Point3d();
      mesh.myWorldMaxCoords = new Point3d();
      mesh.myWorldBoundsValid = false;
      mesh.myWorldRadius = -1;
      
      mesh.myXMeshToWorldRender = null;
      
      mesh.setName(getName());
      return mesh;      
   }


   public void replaceVertices (List<? extends Vertex3d> vtxs) {

      if (vtxs.size() < myVertices.size()) {
         throw new IllegalArgumentException (
            "Number of supplied vertices="+vtxs.size()+
            ", need "+myVertices.size());
      }
      myVertices.clear();
      for (int i=0; i<vtxs.size(); i++) {
         addVertex (vtxs.get(i));
      }
   }

   /** 
    * Creates a copy of this mesh using a specific set of vertices.
    */
//   public abstract MeshBase copyWithVertices (ArrayList<? extends Vertex3d> vtxs);

   protected int[] concatIndices (int[] indices0, int[] indices1, int ioff) {
      if (indices0 == null) {
         return Arrays.copyOf (indices1, indices1.length);
      }
      else {
         int len0 = indices0.length;
         int len1 = indices1.length;
         int[] newIndices = Arrays.copyOf (indices0, len0+len1);
         for (int i=0; i<len1; i++) {
            newIndices[len0+i] = indices1[i]+ioff;
         }
         return newIndices;
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
    * @param respectTransforms if <code>true</code>, transform the
    * vertex positions of <code>mesh</code> into the coordinate frame
    * of this mesh.
    */
   protected void addMesh (MeshBase mesh, boolean respectTransforms) {

      RigidTransform3d TMT = null; // transform from mesh to this
      if (respectTransforms && 
          (!meshToWorldIsIdentity() || !mesh.meshToWorldIsIdentity())) {
         TMT = new RigidTransform3d();
         TMT.mulInverseLeft (getMeshToWorld(), mesh.getMeshToWorld()); 
      }
      int voff = myVertices.size();
      Point3d pnt = new Point3d();
      for (int i = 0; i < mesh.numVertices(); i++) {
         pnt.set (mesh.getVertices().get(i).pnt);
         if (TMT != null) {
            pnt.transform (TMT);
         }
         addVertex(pnt);
      }

      if (mesh.myNormalsExplicitP &&
         (myNormalsExplicitP || voff == 0)) {
         int vnoff;
         if (myNormals == null) {
            vnoff = 0;
            myNormals = new ArrayList<Vector3d>();
         }
         else {
            vnoff = myNormals.size();
         }
         for (int i=0; i<mesh.myNormals.size(); i++) {
            myNormals.add (new Vector3d (mesh.myNormals.get (i)));
         }
         myNormalIndices = concatIndices (
            myNormalIndices, mesh.myNormalIndices, vnoff);
         myRenderNormalsValidP = false;
      }
      else {
         clearNormals();
      }

      if (mesh.myTextureCoords != null &&
         (myTextureCoords != null || voff == 0)) {
         int vtoff;
         if (myTextureCoords == null) {
            vtoff = 0;
            myTextureCoords = new ArrayList<Vector3d>();
         }
         else {
            vtoff = myTextureCoords.size();
         }
         for (int i=0; i<mesh.myTextureCoords.size(); i++) {
            myTextureCoords.add (new Vector3d (mesh.myTextureCoords.get (i)));
         }
         myTextureIndices = concatIndices (
            myTextureIndices, mesh.myTextureIndices, vtoff);
      }
      else {
         clearTextureCoords();
      }

      if (mesh.myColors != null &&
          (myColors != null || voff == 0)) {
         int vcoff;
         if (myColors == null) {
            vcoff = 0;
            myColors = new ArrayList<float[]>();
         }
         else {
            vcoff = myColors.size();
         }
         for (int i=0; i<mesh.myColors.size(); i++) {
            myColors.add (copyColor (mesh.myColors.get (i)));
         }
         myColorIndices = concatIndices (
            getColorIndices(), mesh.getColorIndices(), vcoff);
         // preserve vertex (or feature) coloring if the new colors are still
         // consistent with vertex (or feature) coloring
         if (myVertexColoringP) {
            int[] vertexIndices = createVertexIndices();
            myVertexColoringP =
               (myColors.size() == numVertices() &&
                Arrays.equals (vertexIndices, myColorIndices));
         }
         else if (myFeatureColoringP) {
            int[] featureIndices = createFeatureIndices();
            myFeatureColoringP =
               (myColors.size() == numFeatures() &&
                Arrays.equals (featureIndices, myColorIndices));
         }
      }
      else {
         clearColors();
      }
   }

   public void computeCentroid (Vector3d centroid) {
      centroid.setZero();
      for (int i=0; i<myVertices.size(); i++) {
         centroid.add (myVertices.get(i).pnt);
      }
      centroid.scale (1.0/myVertices.size());
   }
   
   /**
    * Computes the oriented bounding box (OBB) that most tightly contains
    * this mesh, using the specified method.
    * 
    * @param method method used to compute the OBB
    * @return OBB containing this mesh
    */
   public OBB computeOBB (OBB.Method method) {
      OBB obb = new OBB();
      obb.set (this, 0, method);
      return obb;
   }

   /**
    * Computes the oriented bounding box (OBB) that most tightly contains
    * this mesh, using the {@link OBB.Method#ConvexHull} method.
    * 
    * @return OBB containing this mesh
    */
   public OBB computeOBB () {
      return computeOBB (OBB.Method.ConvexHull);
   }

   /**
    * Returns the radius of this mesh. This is defined to be the maximum
    * distance of any vertex from the mesh's centroid.
    */
   public double computeRadius() {
      if (myVertices.size() == 0) {
         return 0;
      }
      Vector3d centroid = new Vector3d();
      computeCentroid (centroid);
      double maxDsqr = 0;
      for (int i=0; i<myVertices.size(); i++) {
         double dsqr = centroid.distanceSquared (myVertices.get(i).pnt);
         if (dsqr > maxDsqr) {
            maxDsqr = dsqr;
         }
      }
      return Math.sqrt (maxDsqr);
   }

   public double computeAverageRadius() {
      if (myVertices.size() == 0) {
         return 0;
      }
      Vector3d centroid = new Vector3d();
      computeCentroid (centroid);
      double radius = 0;
      for (int i=0; i<myVertices.size(); i++) {
         radius += centroid.distance (myVertices.get(i).pnt);
      }
      return radius/myVertices.size();
   }
   
   /**
    * {@inheritDoc}
    */
   public void render (Renderer renderer, int flags) {
      render (renderer, myRenderProps, flags);
   }

   public abstract void render (
      Renderer renderer, RenderProps props, int flags);
      
   /**
    * Base method for testing if two meshes are equal. Two MeshBase objects are
    * considered equal if their vertex coordinates and transforms are equal
    * within <code>eps</code>. This method, and its overrides, is used
    * in mesh unit tests.
    */
   public boolean epsilonEquals (MeshBase base, double eps) {
      if (!XMeshToWorld.epsilonEquals (base.XMeshToWorld, eps)) {
         return false;
      }
      if (myVertices.size() != base.myVertices.size()) {
         return false;
      }
      for (int i=0; i<myVertices.size(); i++) {
         Vertex3d vtx0 = myVertices.get(i);
         Vertex3d vtx1 = base.myVertices.get(i);
         if (!vtx0.pnt.epsilonEquals (vtx1.pnt, eps)) {
            return false;
         }
      }
      if (myNormalsExplicitP != base.myNormalsExplicitP) {
         return false;
      }
      if (myNormalsExplicitP) {
         if (myNormals.size() != base.myNormals.size()) {
            return false;
         }
         for (int i=0; i<myNormals.size(); i++) {
            Vector3d nrm0 = myNormals.get(i);
            Vector3d nrm1 = base.myNormals.get(i);
            if (!nrm0.epsilonEquals (nrm1, eps)) {
               return false;
            }
         }
         if (!Arrays.equals (myNormalIndices, base.myNormalIndices)) {
            return false;
         }
      }
      if ((myColors == null) != (base.myColors == null)) {
         return false;
      }
      if (myColors != null) {
         if (myColors.size() != base.myColors.size()) {
            return false;
         }
         for (int i=0; i<myColors.size(); i++) {
            float[] col0 = myColors.get(i);
            float[] col1 = base.myColors.get(i);
            for (int j=0; j<4; j++) {
               if (Math.abs(col0[j]-col1[j]) > eps) {
                  return false;
               }
            }
         }
         if (!Arrays.equals (getColorIndices(), base.getColorIndices())) {
            return false;
         }
      }
      if ((myTextureCoords == null) != (base.myTextureCoords == null)) {
         return false;
      }
      if (myTextureCoords != null) {
         if (myTextureCoords.size() != base.myTextureCoords.size()) {
            return false;
         }
         for (int i=0; i<myTextureCoords.size(); i++) {
            Vector3d tex0 = myTextureCoords.get(i);
            Vector3d tex1 = base.myTextureCoords.get(i);
            if (!tex0.epsilonEquals (tex1, eps)) {
               return false;
            }
         }
         if (!Arrays.equals (myTextureIndices, base.myTextureIndices)) {
            return false;
         }
      }
      return true;
   }
   
//   public void setVertexColor(int i, Color color) {
//      myVertices.get (i).setColor(color);
//   }
   
//   public void setVertexColor(int i, float r, float g, float b, float a) {
//      myVertices.get(i).setColor(r, g, b, a);
//   }
   
//   public Color getVertexColor(int i) {
//      return myVertices.get(i).getColor();
//   }

   /**
    * Returns the index offsets for each geometric feature (face, line segment,
    * etc.) associated with this mesh. The returned array maps each feature's
    * index to an offset into the arrays returned by {@link #getNormalIndices},
    * {@link #getColorIndices}, and {@link #getTextureIndices}, which can be
    * used to find the normal, color, or texture indices that correspond to the
    * vertices of the feature. So for example, if we have a
    * <code>PolygonalMesh</code> and we want to find the normals associated
    * with each of the three vertices of the (triangular) face at index
    * <code>fidx</code>, we could use the following code fragment:
    * <pre>
    * {@code
    *    ArrayList<Vector3d> normals = mesh.getNormals();
    *    int[] indices = mesh.getNormalIndices();
    *    int[] indexOffs = mesh.getFeatureIndexOffsets();
    *
    *    int offset = indexOffs[fidx];   
    *    Vector3d nrm0 = normals.get(indices[offset]);
    *    Vector3d nrm1 = normals.get(indices[offset+1]);
    *    Vector3d nrm2 = normals.get(indices[offset+2]);
    * }
    * </pre>
    * 
    * <p>The array returned by this method has a length of n+1,
    * where n is the number of mesh features. The last entry contains 
    * the total number of feature vertices. This is the length
    * of the index array that is returned by
    * {@link #getNormalIndices}, {@link #getColorIndices}, etc.,
    * as well as the length that must be used for the <code>indices</code>
    * argument (if specified) in 
    * {@link #setNormals}, {@link #setColors}, etc.
    * 
    * @return feature index offsets for this mesh
    */
   public int[] getFeatureIndexOffsets() {
      if (myIndexOffsets == null) {
         myIndexOffsets = createFeatureIndexOffsets();
      }
      return myIndexOffsets;
   }
   
   /**
    * Creates and returns the vertex indices associated with each geometric 
    * feature in this mesh.
    * 
    * <p>The indices are zero-based and specify the vertex indices for
    * each mesh feature. The number of indices and their structure hence 
    * depends on the mesh subclass: for {@link PolygonalMesh} meshes, 
    * this gives the vertices for each face; for {@link PolylineMesh}, the
    * vertices for each polyline, and for  {@link PointMesh}, the vertices
    * for each point. For example, assume that we have a
    * <code>PolygonalMesh</code> with four vertices and
    * four triangles arranged to form a tetrahedron, so that the vertex
    * indices for each triangular face are given by <code>(0, 2, 1)</code>, 
    * <code>(0, 3, 2)</code>, <code>(0, 1, 3)</code>, 
    * and <code>(1, 2, 3)</code>. Then the returned vertex
    * indices will be 
    * <pre>
    *   0 2 1  0 3 2  0 1 3  1 2 3
    * </pre>
    *
    * @return vertex indices for this mesh. Should not be modified.
    */
   public abstract int[] createVertexIndices();

   /**
    * Creates and returns a set of feature indices for this mesh.
    * 
    * <p>The indices are zero-based and specify, for each feature
    * vertex, the index of the feature. What the features are
    * depends on the mesh subclass: faces for {@link PolygonalMesh},
    * polyline for {@link PolylineMesh}, vertices themselves
    * for {@link PointMesh}. For example, assume that we have a
    * <code>PolygonalMesh</code> with four vertices and
    * four triangles arranged to form a tetrahedron. Then the returned feature
    * indices will be 
    * <pre>
    *   0 0 0  1 1 1  2 2 2  3 3 3 
    * </pre>
    *
    * @return feature indices for this mesh. Should not be modified.
    */
   public int[] createFeatureIndices() {
      int[] indexOffs = getFeatureIndexOffsets();
      int[] indices = new int[indexOffs[indexOffs.length-1]];
      int k = 0;
      for (int i=0; i<indexOffs.length-1; i++) {
         int numv = indexOffs[i+1]-indexOffs[i];
         for (int j=0; j<numv; j++) {
            indices[k++] = i;
         }
      }
      return indices;
   }

   protected abstract int[] createFeatureIndexOffsets();

   protected abstract int[] createDefaultIndices();

   /**
    * Returns <code>true</code> if this mesh automatically creates a default
    * set of normals if {@link #getNormals} is called and no normals have been
    * explicitly set using {@link #setNormals}.
    *
    * @return <code>true</code> if this mesh automatically creates normals.
    */
   public abstract boolean hasAutoNormalCreation();

   /**
    * Returns <code>true</code> if this mesh's normals should be written
    * to a file. This will be the case if the normals were explicitly
    * set, or if they were automatically created using information
    * (such as hard edges) that cannot be easily reconstructed from the
    * information written to standard file formats.
    *
    * @return <code>true</code> if the normals should be written to a file
    */
   public abstract boolean getWriteNormals();

   protected abstract void autoGenerateNormals();

   protected abstract void autoUpdateNormals();

   // public boolean setAutomaticNormalCreation (boolean enable) {
   //    if (!hasAutoNormalCreation()) {
   //       return false;
   //    }
   //    else {
   //       myAutoNormalCreation = (enable ? 1 : 0);
   //       return true;
   //    }
   // }

   // public boolean hasAutomaticNormalCreation () {
   //    if (myAutoNormalCreation == -1) {
   //       myAutoNormalCreation = hasAutoNormalCreation() ? 1 : 0;
   //    }
   //    return myAutoNormalCreation == 1 ? true : false;
   // }

   /**
    * Returns the vertex normals associated with this mesh, or
    * <code>null</code> if this mesh does not have normals. If no normal
    * information is currently defined and {@link #hasAutoNormalCreation()}
    * returns <code>true</code>, then a set of vertex normals and indices will
    * be generated automatically.
    *
    * <p>The set of normals is indexed by the indices returned by {@link
    * #getNormalIndices} to specify a single normal per feature vertex.
    *
    * <p>Vertex normals are used for shading computations during smooth mesh
    * rendering. If smooth mesh rendering is selected and there are no normals,
    * then shading will be disabled.
    *
    * @return vertex normals for this mesh, or <code>null</code> if there are
    * none.
    */
   public ArrayList<Vector3d> getNormals() {
      if (hasAutoNormalCreation() && !myNormalsExplicitP) {
         if (myNormalIndices == null) {
            autoGenerateNormals();
            myRenderNormalsValidP = false;
            myAutoNormalsValidP = true; 
            notifyModified();              
         }
         else if (!myAutoNormalsValidP) {
            autoUpdateNormals();
            myAutoNormalsValidP = true; 
            if (isFixed()) {
               notifyModified();                         
            }
         }
      }
      return myNormals;         
   }

   /**
    * Returns the number of normals associated with this mesh, or 0 if
    * no normals are defined.
    *
    * @return number of normals associated with this mesh.
    */
   public int numNormals() {
      if (myNormals == null) {
         if (hasAutoNormalCreation() && !myNormalsExplicitP) {
            autoGenerateNormals();
         }
      }
      return myNormals != null ? myNormals.size() : 0;
   }

   /**
    * Returns <code>true</code> if there are normals associated with
    * this mesh (or if they will be generated automatically).
    *
    * @return <code>true</code> if this mesh has normals
    */
   public boolean hasNormals() {
      if (myNormals != null) {
         return true;
      }
      else {
         return (hasAutoNormalCreation() && !myNormalsExplicitP);
      }
   }

   /**
    * Returns the normal corresponding to index <code>idx</code>. The returned
    * value is a reference that can be modified by the application. If normals
    * are automatically generated, then modifying the value may lead to
    * unexpected results.
    *
    * @param idx normal index
    * @return normal corresponding to idx
    * @throws IndexOutOfBoundsException if normals are not defined or
    * if the index is out of range.
    */
   public Vector3d getNormal (int idx) {
      ArrayList<Vector3d> normals = getNormals();
      if (normals == null) {
         throw new IndexOutOfBoundsException ("No normals defined for this mesh");
      }
      return normals.get(idx);
   }

   /**
    * Sets the normal corresponding to index <code>idx</code>. If normals are
    * automatically generated, then this may lead to unexpected results.
    *
    * @param idx normal index
    * @param nrml new normal value (should be normalized)
    * @throws IndexOutOfBoundsException if normals are not defined or
    * if the index is out of range.
    */
   public void setNormal (int idx, Vector3d nrml) {
      ArrayList<Vector3d> normals = getNormals();
      if (normals == null) {
         throw new IndexOutOfBoundsException ("No normals defined for this mesh");
      }
      normals.get(idx).set(nrml);
      if (isFixed()) {
         notifyModified();              
      }
   }

   /**
    * Returns the normal associated with the k-th vertex of the feature indexed
    * by <code>fidx</code>, or <code>null</code> if there is no such normal.
    *
    * @param fidx feature index
    * @param k vertex index relative to the feature
    * @return feature vertex color, or <code>null</code>
    */
   public Vector3d getFeatureNormal (int fidx, int k) {
      ArrayList<Vector3d> normals = getNormals();
      if (normals != null) {
         int[] indices = getNormalIndices();
         int[] indexOffs = getFeatureIndexOffsets();
         int idx = indices[indexOffs[fidx]+k];
         if (idx != -1) {
            return normals.get(idx);
         }
      }
      return null;
   }

   /**
    * Returns the normal indices associated with this mesh, or
    * <code>null</code> if this mesh does not have normals. If no normal
    * information is currently defined and {@link #hasAutoNormalCreation()}
    * returns <code>true</code>, then a set of vertex normals and indices will
    * be generated automatically.
    *
    * <p>The indices are zero-based and index into the list returned by {@link
    * #getNormals} to specific a normal per feature vertex. The number of
    * indices and their structure hences depends on the mesh subclass:
    * polygonal meshes have one index per vertex per face, polyline meshes will
    * have two indices per line segment, and point meshes will have one index
    * per vertex. For polygonal meshes, if there is one normal per vertex, then
    * the indexing structure will be the same as that for face vertices.  For
    * example, assume that we have a mesh with four vertices and four triangles
    * arranged to form a tetrahedron, so that the vertex indices for each
    * triangular face are given by <code>(0, 2, 1)</code>, <code>(0, 3,
    * 2)</code>, <code>(0, 1, 3)</code>, and <code>(1, 2, 3)</code>.  Then if
    * there is one normal per vertex, there should be 12 normal indices with
    * the values`
    * <pre>
    *  0 2 1 0 3 2 0 1 3 1 2 3
    * </pre>
    *
    * <p>In some cases, there may be no normal information for the vertices of
    * specific features. In such cases, the cooresponding index values will be
    * <code>-1</code>.
    *
    * @return normal indices for this mesh, or <code>null</code>. Must
    * not be modified.
    */
   public int[] getNormalIndices() {
      if (hasAutoNormalCreation() && !myNormalsExplicitP) {
         if (myNormalIndices == null) {
            autoGenerateNormals();
            myRenderNormalsValidP = false;
            myAutoNormalsValidP = true;               
         }
      }
      return myNormalIndices;         
   }

   /**
    * Explicitly sets the normals and associated indices for this mesh.  The
    * information supplied by <code>nrmls</code> and <code>indices</code> is
    * copied to internal structures that are subsequently returned by {@link
    * #getNormals} and {@link #getNormalIndices()}, respectively.
    * The argument <code>indices</code> specifies an index values into
    * <code>nrmls</code> for each vertex of each feature, as described for
    * {@link #getNormalIndices()}. If a feature vertex has no normal value, the
    * index should be specified as <code>-1</code>.
    * <code>indices</code> is <code>null</code>, then <code>nrmls</code> should
    * contain one normal per vertex and a default index set will be created,
    * appropriate to the mesh subclass.
    *
    * <p>If <code>normals</code> is <code>null</code>, then
    * normals are explicitly removed and subsequent calls to {@link
    * #getNormals} will return <code>null</code>.
    *
    * <p>After this call, {@link #hasExplicitNormals} will return
    * <code>true</code>.
    *
    * @param normals vertex normals to be set for this mesh
    * @param indices normal indices, or <code>null</code> if the indices are to be
    * automatically generated.
    */
   public void setNormals (List<Vector3d> normals, int[] indices) {
      if (normals == null) {
         myNormals = null;
         myNormalIndices = null;
      }
      else {
         ArrayList<Vector3d> newNormals = 
            new ArrayList<Vector3d>(normals.size());
         for (int i=0; i<normals.size(); i++) {
            newNormals.add (new Vector3d (normals.get(i)));
         }
         if (indices == null && normals.size() != numVertices()) {
            throw new IllegalArgumentException (
               "Number of normals must equal number of vertices when " +
            "indices argument is null");
         }
         int[] newIndices = createIndices (indices, normals.size());
         
         myNormals = newNormals;      
         myNormalIndices = newIndices;
      }
      myRenderNormalsValidP = false;
      myNormalsExplicitP = true;
      notifyModified();
   }

   /**
    * Returns <code>true</code> if vertex normal information has been
    * explicitly specified using {@link #setNormals}. If normals have been
    * explicitly set, then it is up to the application to update the normals
    * whenever the vertex positions are modified. Note that even if this
    * method returns <code>true</code>, it is still possible for the
    * mesh to not have any normals (i.e., 
    * {@link #hasNormals()} and {@link #numNormals()} may return
    * <code>false</code> and <code>0</code>). That's because 
    * <code>setNormals(null,null)</code> will <i>explicitly</i>
    * set the mesh to not have any normals.
    *
    * @return true if vertex normals have been explicitly set
    */
   public boolean hasExplicitNormals() {
      return myNormalsExplicitP;
   }

   /**
    * Clears any normals that have been explicitly set for the mesh using
    * {@link #setNormals}. After this call, {@link #hasExplicitNormals} will
    * return <code>false</code> and {@link #getNormals} and {@link
    * #getNormalIndices} will either return <code>null</code> or automatically
    * create new normals and indices, depending on whether
    * {@link #hasAutoNormalCreation} returns <code>true</code>.
    */
   public void clearNormals() {
      myNormals = null;
      myNormalIndices = null;
      myNormalsExplicitP = false;
      myRenderNormalsValidP = false;      
      notifyModified();
   }

   private int[] createIndices (int[] indices, int numAttributes) {

      int[] newIndices = null;
      if (indices == null) {
         newIndices = createDefaultIndices();
      }
      else {
         int[] indexOffs = getFeatureIndexOffsets();
         int reqLength = indexOffs[indexOffs.length-1];
         if (indices.length != reqLength) {
            throw new IllegalArgumentException (
               "indices.length=" + indices.length + ", expecting "+reqLength+
               " (num features * vertices per feature)");
         }
         newIndices = Arrays.copyOf (indices, reqLength);
      }
      for (int i=0; i<newIndices.length; i++) {
         int idx = newIndices[i];
         if (idx < 0 || idx >= numAttributes) {
            throw new IllegalArgumentException (
               "Attribute index "+idx+" out of range, num attributes=" +
               numAttributes + ", i=" + i);
         }
      }
      return newIndices;
   }

   protected float[] copyColor (float[] color) {
      float[] newc = Arrays.copyOf (color, 4);
      if (color.length < 4) {
         newc[3] = 1.0f; // default alpha value
      }
      return newc;
   }

   /**
    * Returns the colors associated with this mesh, or <code>null</code> if
    * this mesh does not have colors. This set of colors is indexed by the
    * indices returned by {@link #getColorIndices} to specify a single color
    * per feature vertex.
    *
    * <p>Each color in the returned list is described by a <code>float[]</code>
    * of length four giving the RGBA values in the range 0 to 1. The structure
    * of the list should not be modified, but individual <code>float[]</code>
    * objects can be changed to modify the corresponding color.
    *
    * @return colors associated with this mesh, or <code>null</code> if there
    * are none.
    */
   public ArrayList<float[]> getColors() {
      return myColors;
   }

   /**
    * Returns the number of colors associated with this mesh, or 0 if
    * no colors are defined.
    *
    * @return number of colors associated with this mesh.
    */
   public int numColors() {
      return myColors != null ? myColors.size() : 0;
   }

   /**
    * Returns <code>true</code> if there are colors associated
    * with this mesh.
    *
    * @return <code>true</code> if this mesh has colors
    */
   public boolean hasColors() {
      return myColors != null;
   }

   /**
    * Returns the color corresponding to index <code>idx</code>. The returned
    * value is a reference that can be modified by the application.
    *
    * @param idx color index
    * @return color corresponding to idx
    * @throws IndexOutOfBoundsException if colors are not defined or
    * if the index is out of range.
    */
   public float[] getColor (int idx) {
      if (myColors == null) {
         throw new IndexOutOfBoundsException ("No colors defined for this mesh");
      }
      return myColors.get(idx);
   }

   /**
    * Sets the color corresponding to index <code>idx</code>.
    *
    * @param idx color index
    * @param color new color value. Array must have a length {@code >=} 3 and
    * give the RGB (or RGBA values for length {@code >=} 4) in the range [0,
    * 1].
    * @throws IndexOutOfBoundsException if colors are not defined or
    * if the index is out of range.
    */
   public void setColor (int idx, float[] color) {
      if (myColors == null) {
         throw new IndexOutOfBoundsException ("No colors defined for this mesh");
      }
      if (color.length < 3) {
         throw new IllegalArgumentException ("Color value has < 3 entries");
      }
      float[] mycolor = myColors.get(idx);
      mycolor[0] = color[0];
      mycolor[1] = color[1];
      mycolor[2] = color[2];
      if (color.length > 3) {
         mycolor[3] = color[3];
      }
      else {
         mycolor[3] = 1f;
      }
      if (isColorsFixed()) {
         notifyModified();                    
      }
   }

   /**
    * Sets the color corresponding to index <code>idx</code>.
    * 
    * @param idx color index
    * @param color new color value
    * @throws IndexOutOfBoundsException if colors are not defined or
    * if the index is out of range.
    */
   public void setColor (int idx, Color color) {
      if (myColors == null) {
         throw new IndexOutOfBoundsException ("No colors defined for this mesh");
      }
      float[] mycolor = myColors.get(idx);
      color.getRGBComponents (mycolor);
      if (isColorsFixed()) {
         notifyModified();                    
      }
   }

   /**
    * Sets the color corresponding to index <code>idx</code>.
    * 
    * @param idx color index
    * @param r red (range [0,1])
    * @param g green (range [0,1])
    * @param b blue (range [0,1])
    * @param a alpha (range [0,1])
    * @throws IndexOutOfBoundsException if colors are not defined or
    * if the index is out of range.
    */
   public void setColor (int idx, float r, float g, float b, float a) {
      if (myColors == null) {
         throw new IndexOutOfBoundsException ("No colors defined for this mesh");
      }
      float[] mycolor = myColors.get(idx);
      mycolor[0] = r;
      mycolor[1] = g;
      mycolor[2] = b;
      mycolor[3] = a;
      if (isColorsFixed()) {
         notifyModified();                    
      }
   }

   /**
    * Sets the color corresponding to index <code>idx</code>.
    * 
    * @param idx color index
    * @param r red (range [0,1])
    * @param g green (range [0,1])
    * @param b blue (range [0,1])
    * @param a alpha (range [0,1])
    * @throws IndexOutOfBoundsException if colors are not defined or
    * if the index is out of range.
    */
   public void setColor (int idx, double r, double g, double b, double a) {
      if (myColors == null) {
         throw new IndexOutOfBoundsException ("No colors defined for this mesh");
      }
      float[] mycolor = myColors.get(idx);
      mycolor[0] = (float)r;
      mycolor[1] = (float)g;
      mycolor[2] = (float)b;
      mycolor[3] = (float)a;
      if (isColorsFixed()) {
         notifyModified();                    
      }
   }
   
   /**
    * Returns the color associated with the k-th vertex of the feature indexed
    * by <code>fidx</code>, or <code>null</code> if there is no such color.
    *
    * @param fidx feature index
    * @param k vertex index relative to the feature
    * @return feature vertex color, or <code>null</code>
    */
   public float[] getFeatureColor (int fidx, int k) {
      ArrayList<float[]> colors = getColors();
      if (colors != null) {
         int[] indices = getColorIndices();
         int[] indexOffs = getFeatureIndexOffsets();
         int idx = indices[indexOffs[fidx]+k];
         if (idx != -1) {
            return colors.get(idx);
         }
      }
      return null;
   }

   /**
    * Returns the color indices associated with this mesh, or <code>null</code>
    * if this mesh does not have colors. These index into the list returned by
    * {@link #getColors} to specify a color per feature vertex, using the same
    * indexing structure as that described for {@link #getNormalIndices}.
    *
    * @return color indices for this mesh, or <code>null</code>. Must not be
    * modified.
    */
  public int[] getColorIndices() {
     if (myColorIndices == null) {
        if (myVertexColoringP) {
           // just set color indices to vertex indices
           myColorIndices = createVertexIndices();
        }
        else if (myFeatureColoringP) {
           // just set color indices to feature indices
           myColorIndices = createFeatureIndices();
        }
     }
     return myColorIndices;
   }


   public boolean hasExplicitColors() {
      return myColors != null && !myVertexColoringP && !myFeatureColoringP;
   }

   /**
    * Explicitly sets the colors and associated indices for this mesh. The
    * information supplied by <code>colors</code> and <code>indices</code> is
    * copied to internal structures that are subsequently returned by {@link
    * #getColors} and {@link #getColorIndices}, respectively.  
    *
    * <p> Colors should be specified as <code>float[]</code> objects with a
    * length {@code >=} 3, indicating RGG values (or RGBA values for length
    * {@code >=} 4) in the range [0,1].
    *
    * <p>The argument <code>indices</code> specifies an index values into
    * <code>colors</code> for each vertex of each feature, as described for
    * {@link #getNormalIndices()}. If a feature vertex has no color value, the
    * index should be specified as <code>-1</code>.  If <code>indices</code> is
    * <code>null</code>, then <code>colors</code> should contain one color per
    * vertex and a default index set will be created, appropriate to the mesh
    * subclass.
    * 
    * <p>If <code>colors</code> is <code>null</code>, then
    * colors are explicitly removed and subsequent calls to {@link
    * #getColors} will return <code>null</code>.
    *
    * <p>Each entry in <code>colors</code> should be a <code>float[]</code> of
    * length three (or four) giving the RGB (or RGBA) values for the color in
    * the range 0 to 1.
    *
    * <p>Calling this method clears vertex or feature coloring, if either of
    * those had been set. However, {@link #isVertexColored} or {@link
    * #isFeatureColored} may still return true if colors and their indexing
    * match either.
    *
    * @param colors colors to be set for this mesh
    * @param indices color indices, or <code>null</code> if the indices are to
    * be automatically generated.
    */
   public void setColors (List<float[]> colors, int[] indices) {

      if (colors == null) {
         myColors = null;
         myColorIndices = null;
      }
      else {
         ArrayList<float[]> newColors = new ArrayList<float[]>(colors.size());
         for (int i=0; i<colors.size(); i++) {
            float[] c = colors.get(i);
            if (c == null) {
               throw new IllegalArgumentException (
                  "Null color value at index "+i);
            }
            if (c.length < 3) {
               throw new IllegalArgumentException (
                  "Color value at index "+i+" has < 3 entries");
            }
            newColors.add (copyColor (c));
         }
         if (indices == null && colors.size() != numVertices()) {
            throw new IllegalArgumentException (
               "Number of colors must equal number of vertices when " +
               "indices argument is null");
         }
         int[] newIndices = createIndices (indices, colors.size());

         myColors = newColors;      
         myColorIndices = newIndices;
      }
      myVertexColoringP = false;
      myFeatureColoringP = false;
      notifyModified();      
   }

   /**
    * Clears the color information for this mesh. After this call, {@link
    * #getColors} and {@link #getColorIndices} will both return
    * <code>null</code>.
    */
   public void clearColors() {
      myColors = null;
      myColorIndices = null;
      myVertexColoringP = false;
      myFeatureColoringP = false;
      notifyModified();
   }

   /**
    * Sets whether or not colors should be considered ``fixed''.  This is
    * used as a hint to determine how to cache rendering info for the mesh.
    */
   public void setColorsFixed(boolean set) {
      if (myColorsFixed != set) {
         myColorsFixed = set;
         notifyModified();
      }
   }
   
   /**
    * See {@link #setColorsFixed(boolean)}
    */
   public boolean isColorsFixed() {
      return myColorsFixed;
   }

   /**
    * Sets whether or not texture coordinates should be considered ``fixed''.  This is
    * used as a hint to determine how to cache rendering info for the mesh.
    */
   public void setTextureCoordsFixed(boolean set) {
      if (myTextureCoordsFixed != set) {
         myTextureCoordsFixed = set;
         notifyModified();
      }
   }
   
   /**
    * See {@link #setTextureCoordsFixed(boolean)}
    */
   public boolean isTextureCoordsFixed() {
      return myTextureCoordsFixed;
   }
   
   /**
    * Returns all the texture coordinates associated with this mesh, or
    * <code>null</code> if this mesh does not have texture coordinates. These
    * coordinates are indexed by the indices returned by {@link
    * #getTextureIndices} to specify a single coordinate per feature vertex.
    *
    * <p>The structure of this list should not be modified, but individual
    * entries can be changed to modify the corresponding coordinates.
    *
    * @return texture coordinates associated with this mesh, or
    * <code>null</code> if there are none.
    */
   public ArrayList<Vector3d> getTextureCoords() {
      return myTextureCoords;
   }

   /**
    * Returns the number of texture coordinates associated with this mesh, or 0 if
    * no texture coordinates are defined.
    *
    * @return number of texture coordinates associated with this mesh.
    */
   public int numTextureCoords() {
      return myTextureCoords != null ? myTextureCoords.size() : 0;
   }

   /**
    * Returns <code>true</code> if there are texture coordinates associated
    * with this mesh.
    *
    * @return <code>true</code> if this mesh has texture coordiates
    */
   public boolean hasTextureCoords() {
      return myTextureCoords != null;
   }

   /**
    * Returns the texture coordinate corresponding to index
    * <code>idx</code>. The returned value is a reference that can be modified
    * by the application.
    *
    * @param idx coordinate index
    * @return texture coordinate corresponding to <code>idx</code>
    * @throws IndexOutOfBoundsException if texture coordinates are not defined
    * or if the index is out of range.
    */
   public Vector3d getTextureCoords (int idx) {
      if (myTextureCoords == null) {
         throw new IndexOutOfBoundsException (
            "No texture coordinates defined for this mesh");
      }
      return myTextureCoords.get(idx);
   }

   /**
    * Sets the texture coordinate corresponding to index <code>idx</code>.
    *
    * @param idx coordinate index
    * @param coords new texture coordinates
    * @throws IndexOutOfBoundsException if texture coordinates are not defined
    * or if the index is out of range.
    */
   public void setTextureCoords (int idx, Vector3d coords) {
      if (myTextureCoords == null) {
         throw new IndexOutOfBoundsException (
            "No texture coordinate defined for this mesh");
      }
      myTextureCoords.get(idx).set(coords);
      notifyModified();              
   }

   /**
    * Returns the texture coordinate associated with the k-th vertex of the
    * feature indexed by <code>fidx</code>, or <code>null</code> if there is no
    * such coordinate.
    *
    * @param fidx feature index
    * @param k vertex index relative to the feature
    * @return feature vertex texture coordinate, or <code>null</code>
    */
   public Vector3d getFeatureTextureCoords (int fidx, int k) {
      ArrayList<Vector3d> coords = getTextureCoords();
      if (coords != null) {
         int[] indices = getTextureIndices();
         int[] indexOffs = getFeatureIndexOffsets();
         int idx = indices[indexOffs[fidx]+k];
         if (idx != -1) {
            return coords.get(idx);
         }
      }
      return null;
   }

   /**
    * Returns the texture indices associated with this mesh, or
    * <code>null</code> if this mesh does not have texture coordinates. These
    * index into the list returned by {@link #getTextureCoords} to specify a
    * coordinates per feature vertex, using the same indexing structure as that
    * described for {@link #getNormalIndices}.
    *
    * @return texture indices for this mesh, or <code>null</code>. Must not be
    * modified.
    */
   public int[] getTextureIndices() {
      return myTextureIndices;
   }

   /**
    * Explicitly sets the texture coordinates and associated indices for this
    * mesh. The information supplied by <code>coords</code> and
    * <code>indices</code> is copied to internal structures that are
    * subsequently returned by {@link #getTextureCoords} and {@link
    * #getTextureIndices}, respectively.  
    * The argument <code>indices</code> specifies an index values into
    * <code>coords</code> for each vertex of each feature, as described for
    * {@link #getNormalIndices()}. If a feature vertex has no texture coordinate
    * value, the index should be specified as <code>-1</code>.
    * If <code>indices</code> is
    * <code>null</code>, then <code>coords</code> should contain one coordinate
    * per vertex and a default index set will be created, appropriate to the
    * mesh subclass.
    *
    * <p>If <code>coords</code> is <code>null</code>, then
    * texture coordinates are explicitly removed and subsequent calls to {@link
    * #getTextureCoords} will return <code>null</code>.
    *
    * @param coords texture coordinates to be set for this mesh
    * @param indices texture indices, or <code>null</code> if the indices are to
    * be automatically generated.
    */
   public void setTextureCoords (List<Vector3d> coords, int[] indices) {

      if (coords == null) {
         myTextureCoords = null;
         myTextureIndices = null;
      }
      else {
         ArrayList<Vector3d> newCoords = new ArrayList<Vector3d>(coords.size());
         for (int i=0; i<coords.size(); i++) {
            newCoords.add (new Vector3d (coords.get(i)));
         }
         if (indices == null && coords.size() != numVertices()) {
            throw new IllegalArgumentException (
               "Number of coords must equal number of vertices when " +
               "indices argument is null");
         }
         int[] newIndices = createIndices (indices, coords.size());
         myTextureCoords = newCoords;      
         myTextureIndices = newIndices;
      }
      notifyModified();              
   }

   /**
    * Clears the texture information for this mesh. After this call, {@link
    * #getColors} and {@link #getColorIndices} will both return
    * <code>null</code>.
    */
   public void clearTextureCoords() {
      myTextureCoords = null;
      myTextureIndices = null;
      notifyModified();              
   }

   private void addChecksum (CRC32 crc, double val) {
      byte[] bytes = new byte[8];
      ByteBuffer.wrap(bytes).putDouble(val);
      crc.update (bytes);
   }

   private void addChecksum (CRC32 crc, Vector vec) {
      for (int i=0; i<vec.size(); i++) {
         addChecksum (crc, vec.get(i));
      }
   }
   private void addChecksum (CRC32 crc, Matrix mat) {
      for (int i=0; i<mat.rowSize(); i++) {
         for (int j=0; j<mat.colSize(); j++) {
            addChecksum (crc, mat.get(i,j));
         }
      }
   }

   /**
    * Applies a random perturbation to the vertex positions of this mesh.  The
    * magnitude of the pertubation is given by {@code rad} * {@code tol}, where
    * {@code rad} is an estimate of the mesh's ``radius'', as computed by
    * {@link #getRadius}, and {@code tol} is a supplied tolerance argument.
    *
    * <p>This method calls {@link #notifyVertexPositionsModified} after
    * applying the perturbation.
    *
    * @param tol controls the size of the perturbation
    */
   public void perturb (double tol) {
      double eps = getRadius()*tol/2;
      Point3d p = new Point3d();
      for (Vertex3d v : getVertices()) {
         p.setRandom (-eps, eps);
         p.add (v.getPosition());
         v.setPosition (p);
      }
      notifyVertexPositionsModified();
   }

   public long checksum() {
      CRC32 crc = new CRC32();
      for (Vertex3d vtx : getVertices()) {
         addChecksum (crc, vtx.pnt);
      }
      addChecksum (crc, XMeshToWorld);
      return crc.getValue();
   }

   /**
    * Computes a spatial inertia for this mesh, given a mass and a mass
    * distribution.  Not all meshes support all distributions. For example, all
    * meshes support the {@link MassDistribution#POINT} distribution (since all
    * meshes contain vertices), but only meshes with area features (such as
    * faces) support an {@link MassDistribution#AREA} distribution.
    *
    * @param mass overall mass
    * @param dist how the mass is distributed across the features
    * @throws IllegalArgumentException if the distribution is not compatible
    * with the available mesh features.
    */
   public SpatialInertia createInertia (double mass, MassDistribution dist) {
      if (dist == MassDistribution.DEFAULT) {
         dist = MassDistribution.POINT;
      }
      if (dist == MassDistribution.POINT) {
         SpatialInertia M = new SpatialInertia();
         double massPerVertex = mass/numVertices();
         for (Vertex3d vtx : getVertices()) {
            M.addPointMass (massPerVertex, vtx.getPosition());
         }
         return M;
      }
      else {
         throw new IllegalArgumentException (
            "Mass distribution "+dist+
            " not supported for mesh type "+getClass().getName());
      }
   }

   /**
    * Queries whether or not a given mass distrubution is supported for this
    * mesh type.
    *
    * @return {@code true} if the indicated mass distrubution is supported
    */
   public boolean supportsMassDistribution (MassDistribution dist) {
      if (dist == MassDistribution.POINT) {
         return true;
      }
      else {
         return false;
      }
   }

   /**
    * Returns a bounding volume tree to be used for proximity queries
    * involving this mesh.
    *
    * @return bounding volume tree
    */
   public abstract BVTree getBVTree();
}
