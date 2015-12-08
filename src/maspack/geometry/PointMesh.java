/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import maspack.geometry.io.WavefrontReader;
import maspack.geometry.io.XyzbReader;
import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.properties.HasProperties;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.render.GL.GL2.PointMeshRenderer;
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

   public ArrayList<Vector3d> getNormals() {
      return myNormalList;
   }

   public void setNormals (ArrayList<Vector3d> normals) {
      myNormalList.clear();
      for (int i=0; i<normals.size(); i++) {
         Vector3d nrm = new Vector3d(normals.get(i));
         double mag = nrm.norm();
         if (mag != 0) {
            nrm.scale (1/mag);
         }
         myNormalList.add (nrm);
      }
   }
   
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
    * Creates a point mesh and initializes it from an file in Alias
    * Wavefront obj format, as decribed for the method
    * {@link #write(PrintWriter,NumberFormat,boolean)}.
    * 
    * @param file
    * file containing the mesh description
    */
   public PointMesh (File file) throws IOException {
      this();
      read (new BufferedReader (new FileReader (file)));
   }
   
   public PointMesh (PointMesh old) {
      super();
      for (Vertex3d v : old.myVertices) {
         addVertex (new Vertex3d (new Point3d (v.pnt), v.idx));
      }
      copyNormals (old);
      copyColors (old);
      
      setMeshToWorld (old.XMeshToWorld);
   }

   public double getNormalRenderLen() {
      return myNormalRenderLen;
   }

   public void setNormalRenderLen (double len) {
      if (myNormalRenderLen != len) {
         myNormalRenderLen = len;
         notifyModified();  // XXX maybe this should be tracked separately
      }
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
      for (Vector3d vn : myNormalList) {
         vn.transform (X);
         vn.normalize();
      }
   }


   public void readBinary (File file) throws IOException {
      // BinaryInputStream in = 
      //    new BinaryInputStream (
      //       new BufferedInputStream (new FileInputStream (file)));
      // in.setLittleEndian (true);
      // readBinary (in);

      XyzbReader reader = new XyzbReader (file);
      reader.setLittleEndian (true);
      reader.readMesh (this);
   }

   // private double readFloat (BinaryInputStream in) throws IOException {
   //    int bytes = Integer.reverseBytes (in.readInt());
   //    return (double)Float.intBitsToFloat (bytes);
   // }

   // public void readBinary (BinaryInputStream in) throws IOException {
   //    XyzbReader reader = new XyzbReader();
   //    reader.read (this, in);

   //    // clear();
   //    // myNormals = new ArrayList<Vector3d>();
   //    // boolean done = false;
   //    // while (!done) {
   //    //    try {
   //    //       double px = readFloat(in);
   //    //       double py = readFloat(in);
   //    //       double pz = readFloat(in);
   //    //       double nx = readFloat(in);
   //    //       double ny = readFloat(in);
   //    //       double nz = readFloat(in);
   //    //       //System.out.println ("pnt " + px + " "+py+" "+pz);
   //    //       //System.out.println ("nrm " + nx + " "+ny+" "+nz);
   //    //       addVertex (new Point3d (px, py, pz), /*byReference=*/false);
   //    //       myNormals.add (new Vector3d (nx, ny, nz));
   //    //    }
   //    //    catch (EOFException e) {
   //    //       done = true;
   //    //    }
   //    // }
   // }

//   public void setFromWavefrontReader (WavefrontReader wfr) throws IOException {
//      setFromWavefrontReader (wfr, null);
//   }

//   public void setFromWavefrontReader (WavefrontReader wfr, String groupName)
//      throws IOException {
//
//      if (groupName == null) {
//         String[] nameList = wfr.getGroupNames();
//         if (nameList.length > 0) {
//            groupName = nameList[0];
//         }
//         else {
//            groupName = "default";
//         }
//      }
//      if (!wfr.hasGroup (groupName)) {
//         throw new IllegalArgumentException ("Group '"+groupName+"' unknown");
//      }
//      wfr.setGroup (groupName);
//
//      List<Point3d> vtxList = Arrays.asList(wfr.getVertexPoints());
//      
//      for (int i=0; i<vtxList.size(); i++) {
//         // add by reference since points have already been copied 
//         addVertex (vtxList.get(i), /* byReference= */true);
//      }
//      
//      Vector3d[] nrms = wfr.getVertexNormals ();
//      if (nrms != null) {
//         List<Vector3d> normalList = Arrays.asList(wfr.getVertexNormals());
//         myNormals = new ArrayList<Vector3d>(normalList);
//      } else {
//         myNormals.clear();
//      }
//
//      setName (groupName.equals ("default") ? null : groupName);
//      if (wfr.getRenderProps() != null) {
//         setRenderProps (wfr.getRenderProps());
//      }
//   }

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
      set (pnts, nrms, /* byReference= */false);
   }

   /**
    * Sets the vertex points and line associated with this mesh.
    * 
    * @param pnts
    * points from which the vertices are formed
    * @param nrms
    * (optional) if non-null, gives vectors from which the normals are formed.
    * @param byReference
    * if true, then the supplied points and normals are not copied but instead referred to
    * directly by the vertex structures. The mesh will track any changes to the
    * points and {@link #isFixed isFixed} will return false.
    * @throws IllegalArgumentException if nrms is non-null and does not have the
    * same size as pnts.
    */
   protected void set (Point3d[] pnts, Vector3d[] nrms, boolean byReference) {
      clear();
      if (nrms != null && nrms.length == 0) {
         nrms = null;
      }
      if (nrms != null && nrms.length != pnts.length) {
         throw new IllegalArgumentException ("If non-null, nrm must have the same size as pnts");
      }
      for (int i=0; i<pnts.length; i++) {
         addVertex (pnts[i], byReference);
      }
      if (nrms != null) {
         myNormalList = new ArrayList<Vector3d>(pnts.length);
         myNormalIndices = new ArrayList<int[]>(pnts.length);
         for (int i=0; i<pnts.length; i++) {
            if (byReference) {
               myNormalList.add (nrms[i]);
            }
            else {
               myNormalList.add (new Vector3d(nrms[i]));
            }
            myNormalIndices.add(new int[]{i});
         }
      }
      notifyModified();
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
      if (myNormalList != null) {
         for (Vector3d nrm : myNormalList) {
            pw.println ("vn " + fmt.format (nrm.x) + " " + fmt.format (nrm.y) + " " +
                        fmt.format (nrm.z));
         }
      }
      pw.flush();
   }

   /**
    * Clears this mesh (makes it empty).
    */
   public void clear() {
      // verts = null;
      super.clear();
      clearNormals();
   }

   public void render (Renderer renderer, RenderProps props, int flags) {
      if (myMeshRenderer == null) {
         myMeshRenderer = new PointMeshRenderer();
      }
      if (hasVertexColoring () && ((flags & Renderer.SELECTED) == 0)) {
         flags |= Renderer.VERTEX_COLORING;
         flags |= Renderer.HSV_COLOR_INTERPOLATION;
      }
      
      myMeshRenderer.render (renderer, this, props, flags);
   }

   /** 
    * Creates a copy of this mesh.
    */
   public PointMesh copy() {
      myBVTree = null;
      myBVTreeValid = false;
      return (PointMesh)super.copy();
   }

   /** 
    * Creates a copy of this mesh using a specific set of vertices.
    */
   public PointMesh copyWithVertices (ArrayList<? extends Vertex3d> vtxs) {
      PointMesh mesh = new PointMesh();

      if (vtxs.size() < myVertices.size()) {
         throw new IllegalArgumentException (
            "Number of supplied vertices="+vtxs.size()+", need "+myVertices.size());
      }
      for (int i=0; i<vtxs.size(); i++) {
         mesh.addVertex (vtxs.get(i));
      }
      mesh.setMeshToWorld (XMeshToWorld);
      
      mesh.copyNormals (this);
      copyColors(this);
      
      if (myRenderProps != null) {
         mesh.setRenderProps (myRenderProps);
      }
      else {
         mesh.myRenderProps = null;
      }
      mesh.setFixed (isFixed());
      mesh.setRenderBuffered (isRenderBuffered());
      mesh.setName(getName());
      return mesh;
   }

   /** 
    * Adds copies of the vertices of another mesh to this mesh.  If the other
    * mesh contains normal information, this will be added as well providing
    * this mesh already contains normal information or is empty. Otherwise,
    * normal information for this mesh will be cleared.
    * 
    * @param mesh Mesh to be added to this mesh
    */
   public void addMesh (PointMesh mesh) {

      for (int i = 0; i < mesh.getNumVertices(); i++) {
         Point3d p = mesh.getVertices().get(i).getPosition();
         //	 p.transform(X);
         addVertex(p);
      }

      boolean iHasNormals = (hasNormals() && myNormalIndices.size() == getNumVertices());
      boolean sheHasNormals = (mesh.hasNormals() && mesh.myNormalIndices.size() == mesh.getNumVertices());
      if (sheHasNormals && iHasNormals) {
         int vnoff = myNormalList.size();
         for (int i=0; i<mesh.myNormalList.size(); i++) {
            Vector3d vn = new Vector3d (mesh.myNormalList.get (i));
            myNormalList.add (vn);
         }
         for (int i=0; i<mesh.myNormalIndices.size(); i++) {
            myNormalIndices.add(new int[]{mesh.myNormalIndices.get(i)[0]+vnoff});
         }
      }
      else {
         clearNormals();
      }
      
      boolean iHasColors = (hasColors() && myColorIndices.size() == getNumVertices());
      boolean sheHasColors = (mesh.hasColors() && mesh.myColorIndices.size() == mesh.getNumVertices());
      if (sheHasColors && iHasColors) {
         int vcoff = myColorList.size();
         for (int i=0; i<mesh.myColorList.size(); i++) {
            myColorList.add(mesh.myColorList.get(i));
         }
         for (int i=0; i<mesh.myColorIndices.size(); i++) {
            myColorIndices.add(new int[]{mesh.myColorIndices.get(i)[0]+vcoff});
         }
      }
      else {
         clearColors();
      }
   }

   public AABBTree getBVTree() {
       if (myBVTree == null || !myBVTreeValid) {
          myBVTree = new AABBTree();
          myBVTree.setMaxLeafElements (8);
          int numElems = getNumVertices();
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
   
   @Override
   public void setNormalIndices(int[][] indices) {
      if (indices != null) {
         if (indices.length < myVertices.size()) {
            throw new IllegalArgumentException (
               "indices length " + indices.length +
               " less than number of vertices " + myVertices.size());
         }
         for (int i=0; i<myVertices.size(); i++) {
            if (indices[i] == null) {
               System.out.println (
                  "Warning: some vertices have normals, others don't; " +
                  "ignoring normals");               
               indices = null;
               break;
            }
         }
      }
      if (indices != null) {
         ArrayList<int[]> newNormalIndices =
            new ArrayList<int[]>(myVertices.size());
            for (int i=0; i<myVertices.size(); i++) {
               newNormalIndices.add (new int[]{indices[i][0]});
            }
            this.myNormalIndices = newNormalIndices;
      }
      else {
         this.myNormalIndices = null;
         this.myNormalList.clear();
      }
      notifyModified();
   }
   
   @Override
   public void setColorIndices(int[][] indices) {
      if (indices != null) {
         if (indices.length < myVertices.size()) {
            throw new IllegalArgumentException (
               "indices length " + indices.length +
               " less than number of vertices " + myVertices.size());
         }
         for (int i=0; i<myVertices.size(); i++) {
            if (indices[i] == null) {
               System.out.println (
                  "Warning: some vertices have colors, others don't; " +
                  "ignoring colors");               
               indices = null;
               break;
            }
         }
      }
      if (indices != null) {
         ArrayList<int[]> newColorIndices =
            new ArrayList<int[]>(myVertices.size());
            for (int i=0; i<myVertices.size(); i++) {
               newColorIndices.add (new int[]{indices[i][0]});
            }
            this.myColorIndices = newColorIndices;
      }
      else {
         this.myColorIndices = null;
         this.myColorList.clear();
      }
      notifyModified();
   }

}
