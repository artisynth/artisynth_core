/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.media.opengl.GL2;

import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.matrix.AffineTransform3dBase;
import maspack.properties.HasProperties;
import maspack.render.GLRenderer;
import maspack.render.RenderProps;
import maspack.render.RenderProps.Shading;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import maspack.util.BinaryInputStream;
import maspack.geometry.io.*;

/**
 * Implements a mesh consisting of a set of points and possibly normals.
 */
public class PointMesh extends MeshBase {

   public static boolean useDisplayListsIfPossible = true;
   protected AABBTree myBVTree = null;
   protected boolean myBVTreeValid = false;
   // if > 0, causes normals to be rendered
   protected double myNormalRenderLen = 0;

   protected ArrayList<Vector3d> myNormals = new ArrayList<Vector3d>();

   /**
    * {@inheritDoc}
    */
   public int getNumNormals() {
      if (myNormals == null) {
         return 0;
      }
      else {
         return myNormals.size();
      }
   }

   /**
    * {@inheritDoc}
    */
   public Vector3d getNormal (int idx) {
      if (myNormals == null) {
         throw new ArrayIndexOutOfBoundsException ("idx="+idx+", size=0");
      }
      else {
         return myNormals.get(idx);
      }
   }

   public ArrayList<Vector3d> getNormals() {
      return myNormals;
   }

   public void setNormals (ArrayList<Vector3d> normals) {
      myNormals.clear();
      for (int i=0; i<normals.size(); i++) {
         Vector3d nrm = new Vector3d(normals.get(i));
         double mag = nrm.norm();
         if (mag != 0) {
            nrm.scale (1/mag);
         }
         myNormals.add (nrm);
      }
   }

   public void clearNormals() {
      myNormals.clear();
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
      setMeshToWorld (old.XMeshToWorld);
   }

   public double getNormalRenderLen() {
      return myNormalRenderLen;
   }

   public void setNormalRenderLen (double len) {
      if (myNormalRenderLen != len) {
         myNormalRenderLen = len;
         clearDisplayList();
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
      for (Vector3d vn : myNormals) {
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
      for (Vector3d vn : myNormals) {
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
         myNormals = new ArrayList<Vector3d>(pnts.length);
         for (int i=0; i<pnts.length; i++) {
            if (byReference) {
               myNormals.add (nrms[i]);
            }
            else {
               myNormals.add (new Vector3d(nrms[i]));
            }
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

   /**
    * Clears this mesh (makes it empty).
    */
   public void clear() {
      // verts = null;
      super.clear();
      
      if (myNormals != null) {
         myNormals.clear();
      }
   }

   public void render (GLRenderer renderer, RenderProps props, int flags) {
      GL2 gl = renderer.getGL2().getGL2();

      gl.glPushMatrix();
      if (isRenderBuffered()) {
         renderer.mulTransform (myXMeshToWorldRender);
      }
      else {
         renderer.mulTransform (XMeshToWorld);
      } 

      boolean reenableLighting = false;
      int[] savedPointSize = new int[1];
      gl.glGetIntegerv (GL2.GL_POINT_SIZE, savedPointSize, 0);
      int[] savedLineWidth = new int[1];
      gl.glGetIntegerv (GL2.GL_LINE_WIDTH, savedLineWidth, 0);
      int[] savedShadeModel = new int[1];
      gl.glGetIntegerv (GL2.GL_SHADE_MODEL, savedShadeModel, 0);

      gl.glPointSize( props.getPointSize());
      
      Shading shading = props.getShading();

      if (props.getPointColor() != null && !renderer.isSelecting()) {
         if (shading != Shading.NONE) {
            renderer.setMaterial(
               props.getPointMaterial(), (flags & GLRenderer.SELECTED) != 0);
         }
         else {
            reenableLighting = renderer.isLightingEnabled();
            renderer.setLightingEnabled (false);
            float[] color;
            if ((flags & GLRenderer.SELECTED) != 0) {
               color = new float[3];
               renderer.getSelectionColor().getRGBColorComponents (color);
            }
            else {
               color = props.getPointColorArray();
            }
            float alpha = (float)props.getAlpha();
            renderer.setColor (color[0], color[1], color[2], alpha);
         }
      }

      boolean useDisplayList = false;
      int displayList = 0;
      boolean useVertexColors = (flags & GLRenderer.VERTEX_COLORING) != 0;
      
      if (useDisplayListsIfPossible && isUsingDisplayList() 
    		  && !(renderer.isSelecting() && useVertexColors)) {
         useDisplayList = true;
         displayList = props.getMeshDisplayList();
      }
         
      if (!useDisplayList || displayList < 1) {
         if (useDisplayList) {
            displayList = props.allocMeshDisplayList (gl);
            if (displayList > 0) {
               gl.glNewList (displayList, GL2.GL_COMPILE);
            }
         }
         
         boolean useRenderVtxs = isRenderBuffered() && !isFixed();
         gl.glBegin (GL2.GL_POINTS);
         int numn = getNumNormals();
         for (int i=0; i<myVertices.size(); i++) {
            Vertex3d vtx = myVertices.get(i);
            Point3d pnt = useRenderVtxs ? vtx.myRenderPnt : vtx.pnt;
            if (i < numn) {
               Vector3d nrm = myNormals.get(i);
               gl.glNormal3d (nrm.x, nrm.y, nrm.z);
            }
            gl.glVertex3d (pnt.x, pnt.y, pnt.z);
         }
         gl.glEnd ();

         if (myNormals != null && myNormalRenderLen > 0) {
            if (props.getLineColor() != null && !renderer.isSelecting()) {
               if (shading != Shading.NONE) {
                  renderer.setMaterial(
                     props.getLineMaterial(), (flags & GLRenderer.SELECTED) != 0);
               }
               else {
                  float[] color = props.getLineColorArray();
                  float alpha = (float)props.getAlpha();
                  renderer.setColor (color[0], color[1], color[2], alpha);
               }
            }
            gl.glLineWidth (1);
            gl.glBegin (GL2.GL_LINES);
            for (int i=0; i<myVertices.size(); i++) {
               Vertex3d vtx = myVertices.get(i);
               Point3d pnt = useRenderVtxs ? vtx.myRenderPnt : vtx.pnt;
               Vector3d nrm = myNormals.get(i);
               double s = myNormalRenderLen;
               gl.glVertex3d (pnt.x, pnt.y, pnt.z);
               gl.glVertex3d (pnt.x+s*nrm.x, pnt.y+s*nrm.y, pnt.z+s*nrm.z);
            }
            gl.glEnd ();
         }
         if (useDisplayList && displayList > 0) {
            gl.glEndList();
            gl.glCallList (displayList);
         }
      }
      else {
         gl.glCallList (displayList);
      }

      if (reenableLighting) {
         renderer.setLightingEnabled (true);
      }
      gl.glPointSize (savedPointSize[0]);
      gl.glLineWidth (savedLineWidth[0]);
      gl.glShadeModel (savedShadeModel[0]);

      gl.glPopMatrix();
   }

   /** 
    * Creates a copy of this mesh.
    */
   public PointMesh copy() {
      myBVTree = null;
      myBVTreeValid = false;
      return (PointMesh)super.copy();
   }

   private void copyNormals (PointMesh old) {
      
      myNormals.clear();
      myNormals.ensureCapacity (old.myNormals.size());
      for (int i=0; i<old.myNormals.size(); i++) {
         myNormals.add (new Vector3d (old.myNormals.get(i)));
      }
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

      boolean hasNormals = (myNormals.size() == getNumVertices());

      int voff = myVertices.size();
      for (int i = 0; i < mesh.getNumVertices(); i++) {
         Point3d p = mesh.getVertices().get(i).getPosition();
         //	 p.transform(X);
         addVertex(p);
      }

      if (mesh.myNormals.size() == mesh.getNumVertices() && hasNormals) {
         int vnoff = myNormals.size();
         for (int i=0; i<mesh.myNormals.size(); i++) {
            Vector3d vn = new Vector3d (mesh.myNormals.get (i));
            myNormals.add (vn);
         }
      }
      else {
         clearNormals();
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
      PointMesh mesh = (PointMesh)base;

      if (myNormals.size() != mesh.myNormals.size()) {
         return false;
      }
      for (int i=0; i<myNormals.size(); i++) {
         if (!myNormals.get(i).epsilonEquals (mesh.myNormals.get(i), eps)) {
            return false;
         }
      }
      return true;
   }

}
