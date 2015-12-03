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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.AffineTransform3d;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.properties.HasProperties;
import maspack.render.GLRenderer;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.Renderable;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;

/**
 * A "mesh" is a geometric object defined by a set of vertices, which are then
 * connected in some specific way.
 */
public abstract class MeshBase implements Renderable {
   
   private boolean fastRemoval = false;
   
   public RigidTransform3d XMeshToWorld = new RigidTransform3d();
   protected boolean myXMeshToWorldIsIdentity = true;
   protected ArrayList<Vertex3d> myVertices = new ArrayList<Vertex3d>();

   protected RenderProps myRenderProps = null;
   public boolean isFixed = true;
   public boolean useVertexColoring = false;
   
   // Allow ability to directly control display list
   // even for non-fixed mesh (e.g. when model is paused,
   // but rotating view)
   public boolean myUseDisplayList = false;  
   public boolean myDisplayListValid = false;

   protected boolean myRenderBufferedP = false;
   RigidTransform3d myXMeshToWorldRender;
   protected Point3d myLocalMinCoords = new Point3d();
   protected Point3d myLocalMaxCoords = new Point3d();
   protected boolean myLocalBoundsValid = false;

   protected Point3d myWorldMinCoords = new Point3d();
   protected Point3d myWorldMaxCoords = new Point3d();
   protected boolean myWorldBoundsValid = false;

   //protected int myWorldCoordCounter = 0;

   protected String myName;
   
   public void setName (String name) {
      myName = name;
   }

   public String getName () {
      return myName;
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
   }

   /** 
    * Invalidates bounding box information. Can also be overriden to
    * clear any bounding volume hierarchies so that they need to be rebuilt.
    */
   protected void clearBoundingInfo () {
      //bvHierarchyValid = false;
      myLocalBoundsValid = false;
      myWorldBoundsValid = false;
   }
   
   /**
    * Notifies this mesh that vertex positions have been modified, and cached
    * data dependent on these positions should be invalidated.
    */
   public void notifyVertexPositionsModified() {
      invalidateBoundingInfo();
      clearDisplayList();
   }
   
   /** 
    * Used internally and by subclasses to notify this mesh that its
    * topological structure has changed.
    */
   protected void notifyStructureChanged() {
      clearBoundingInfo();
   }

   /**
    * Invalidates any world coordinate data for this mesh. 
    */
   protected void invalidateWorldCoords() {
      myWorldBoundsValid = false;
   }

   /**
    * Sets whether or not this mesh to is to be considered ``fixed''. A fixed
    * mesh is one for which the vertex coordinate values are considered
    * constant. Rendering speeds can therefore be improved by pre-allocating
    * appropriate graphical display buffers.
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
      isFixed = fixed;
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
   

   /**
    * Sets whether or not this mesh to is to use per-vertex coloring. 
    * 
    * <p>
    * By default, a mesh is set not to use per-vertex coloring.
    * 
    * @param vertexColoring
    * if true, sets this mesh to use per-vertex coloring.
    * @see #isUsingVertexColoring
    */
   public void setUseVertexColoring (boolean vertexColoring) {
      useVertexColoring = vertexColoring;
   }
   
   
   /**
    * Returns whether or not this mesh is using per-vertex coloring, as described in
    * {@link #setUseVertexColoring setUseVertexColoring}.
    * 
    * @return true if this mesh is using per-vertex coloring.
    */
   public boolean isUsingVertexColoring () {
      return useVertexColoring;
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
   public int getNumVertices() {
      return myVertices.size();
   }

   /**
    * Returns the number of normals in this mesh.
    * 
    * @return number of normals in this mesh
    */
   public abstract int getNumNormals();

   /**
    * Returns the idx-th normal in this mesh.
    *
    * @param idx index of the desired normal
    * @return idx-th normal
    */
   public abstract Vector3d getNormal (int idx);

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
      
      if (fastRemoval) {
         return removeVertex(vtx);
      } else {
     
         if (myVertices.remove (vtx)) {
            
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
   }
   
   /**
    * Removes a set of vertices from this mesh, as indicated by a collection.
    *
    * @param vertices Collection of vertices to remove
    */
   public boolean removeVertices (Collection<Vertex3d> vertices) {
      ArrayList<Integer> deleteIdxs = new ArrayList<Integer>();
      for (Vertex3d v : vertices) {
         if (myVertices.get(v.getIndex()) == v) {
            deleteIdxs.add (v.getIndex());
         }
      }
      if (deleteIdxs.size() > 0) {
         Collections.sort (deleteIdxs);
         ArrayList<Vertex3d> newVertexList =
            new ArrayList<Vertex3d>(myVertices.size()-deleteIdxs.size());
         int k = 0;
         for (int i=0; i<myVertices.size(); i++) {
            Vertex3d v = myVertices.get(i);
            if (i == deleteIdxs.get(k)) {
               // delete v
               v.setMesh (null);
               while (i == deleteIdxs.get(k) && k < deleteIdxs.size()-1) {
                  k++;
               }
            }
            else {
               v.setIndex (newVertexList.size());
               newVertexList.add (v);
            }
         }
         myVertices = newVertexList;
         notifyStructureChanged();
         return true;
      }
      else {
         return false;
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
      if (myVertices.size() > 0) {
         if (!myWorldBoundsValid) {
            recomputeWorldBounds();
         }
         return 0.5*myWorldMinCoords.distance (myWorldMaxCoords);
      }
      else {
         return 0;
      }
   }

   /**
    * {@inheritDoc}
    */
   public void updateBounds (Point3d pmin, Point3d pmax) {
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
      notifyVertexPositionsModified();
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
      notifyVertexPositionsModified();
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

   /**
    * Clears this mesh (makes it empty).
    */
   public void clear() {
      // verts = null;
      for (Vertex3d vtx : myVertices) {
         vtx.setMesh (null);
      }
      myVertices.clear();
      notifyStructureChanged();
      myLocalMinCoords.set (0, 0, 0);
      myLocalMaxCoords.set (0, 0, 0);
      myWorldMinCoords.set (0, 0, 0);
      myWorldMaxCoords.set (0, 0, 0);
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
      saveRenderInfo();
   }

   protected int[] copyWithOffset (int[] idxs, int off) {
      int[] newIdxs = new int[idxs.length];
      for (int i=0; i<idxs.length; i++) {
         newIdxs[i] = idxs[i]+off;
      }
      return newIdxs;
   }
   
   /**
    * Gives control over display lists even if not fixed mesh
    * (for rendering while paused and mesh isn't changing)
    */
   public void setUseDisplayList(boolean set) {
      myUseDisplayList = set;
   }

   public void clearDisplayList() {
      if (myRenderProps != null) {
         myRenderProps.clearMeshDisplayList();
      }
      myDisplayListValid = false;
   }
   
   public void clearDisplayList(RenderProps props) {
      props.clearMeshDisplayList();
      myDisplayListValid = false;
   }
   
   public boolean isDisplayListValid(RenderProps rprops) {
      if (isFixed) {
         return (rprops.getMeshDisplayList() > 0);
      }
      return (myDisplayListValid && (rprops.getMeshDisplayList() > 0));
   }
   
   public boolean isUsingDisplayList() {
      return (myUseDisplayList || isFixed);
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
         saveRenderInfo();
      }
      // set myRenderBufferedP last because otherwise render might occur
      // before saveRenderInfo is complete
      myRenderBufferedP = enable;
   }

   public boolean isRenderBuffered() {
      return myRenderBufferedP;
   }

   public void saveRenderInfo() {
      if (myXMeshToWorldRender == null) {
         myXMeshToWorldRender = new RigidTransform3d();
      }
      myXMeshToWorldRender.set (XMeshToWorld);
      if (!isFixed) {
         for (int i = 0; i < myVertices.size(); i++) {
            myVertices.get (i).saveRenderInfo();
         }
      }
   }

   /** 
    * Creates a copy of this mesh.
    */
   public MeshBase copy() {
      ArrayList<Vertex3d> vtxs = new ArrayList<Vertex3d>();
      for (int i = 0; i < myVertices.size(); i++) {
         vtxs.add (myVertices.get(i).copy());
      }
      return copyWithVertices (vtxs);
   }

   /** 
    * Creates a copy of this mesh using a specific set of vertices.
    */
   public abstract MeshBase copyWithVertices (ArrayList<? extends Vertex3d> vtxs);

   public void computeCentroid (Vector3d centroid) {
      centroid.setZero();
      for (int i=0; i<myVertices.size(); i++) {
         centroid.add (myVertices.get(i).pnt);
      }
      centroid.scale (1.0/myVertices.size());
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
   public void render (GLRenderer renderer, int flags) {
      render (renderer, myRenderProps, flags);
   }

   public abstract void render (
      GLRenderer renderer, RenderProps props, int flags);
      
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
      return true;
   }
   
   public void setFastRemoval(boolean set) {
      fastRemoval = set;
   }
   
   public boolean isFastRemoval() {
      return fastRemoval;
   }
   
   public void setVertexColor(int i, Color color) {
      myVertices.get (i).setColor(color);
   }
   
   public void setVertexColor(int i, Color color, float alpha) {
      myVertices.get(i).setColor(color, alpha);
   }
   
   public void setVertexColor(int i, double r, double g, double b) {
      myVertices.get(i).setColor(r, g, b, 1);
   }
   
   public void setVertexColor(int i, double r, double g, double b, double a) {
      myVertices.get(i).setColor(r, g, b, a);
   }
   
   public void setVertexColor(int i, float r, float g, float b) {
      myVertices.get(i).setColor(r, g, b, 1);
   }
   
   public void setVertexColor(int i, float r, float g, float b, float a) {
      myVertices.get(i).setColor(r, g, b, a);
   }
   
   public void setVertexColorHSV (int i, double h, double s, double b) {
      myVertices.get(i).setColorHSV(h,s,b,1);
   }

   public void setVertexColorHSV (int i, double h, double s, double b, double a) {
      myVertices.get(i).setColorHSV(h,s,b,1);
   }

   public Color getVertexColor(int i) {
      return myVertices.get(i).getColor();
   }
   
   public float[] getVertexColorArray(int i) {
      return myVertices.get(i).getColorArray();
   }


}
