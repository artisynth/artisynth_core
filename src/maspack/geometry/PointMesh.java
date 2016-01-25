/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.media.opengl.GL2;

import jogamp.opengl.glu.error.Error;
import maspack.geometry.io.WavefrontReader;
import maspack.geometry.io.XyzbReader;
import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.properties.HasProperties;
import maspack.render.GLRenderer;
import maspack.render.RenderProps;
import maspack.render.RenderProps.PointStyle;
import maspack.render.RenderProps.Shading;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;

/**
 * Implements a mesh consisting of a set of points and possibly normals.
 */
public class PointMesh extends MeshBase {

   public static boolean useDisplayListsIfPossible = true;
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

   public double getNormalRenderLen() {
      return myNormalRenderLen;
   }

   public void setNormalRenderLen (double len) {
      if (myNormalRenderLen != len) {
         myNormalRenderLen = len;
         clearDisplayList();
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
      int savedPointSize = renderer.getPointSize();
      int savedLineWidth = renderer.getLineWidth();
      RenderProps.Shading savedShadeModel = renderer.getShadeModel();

      renderer.setPointSize( props.getPointSize());
      
      Shading shading = props.getShading();
      boolean selected = ((flags & GLRenderer.SELECTED) != 0);
      
      if (props.getPointColor() != null && !renderer.isSelecting()) {
         if (shading != Shading.NONE) {
            renderer.setMaterial(
               props.getPointMaterial(), selected);
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
    		  && !(renderer.isSelecting() && useVertexColors) ) {
    		    //&& !(props.getPointStyle()==PointStyle.SPHERE)) {
         useDisplayList = true;
         displayList = props.getMeshDisplayList();
      }
         
      if (props.getPointStyle() == PointStyle.SPHERE) {
         useDisplayList = false;
      }

      if (!useDisplayList || displayList < 1) {
         if (useDisplayList) {
            renderer.validateInternalDisplayLists(props);
            displayList = props.allocMeshDisplayList (gl);
            if (displayList > 0) {
               gl.glNewList (displayList, GL2.GL_COMPILE);
            }
         }
         
         boolean useRenderVtxs = isRenderBuffered() && !isFixed();
         float[] coords = new float[3];
         int[] colorIndices = getColorIndices();
         ArrayList<float[]> colors = getColors();
         switch (props.getPointStyle()) {
            case SPHERE: {
               for (int i=0; i<myVertices.size(); i++) {
                  Vertex3d vtx = myVertices.get(i);
                  Point3d pnt = useRenderVtxs ? vtx.myRenderPnt : vtx.pnt;
                  pnt.get(coords);
                  
                  if (useVertexColors) {
                     float[] pointColor = colors.get(colorIndices[i]);
                     renderer.updateMaterial(props, props.getPointMaterial(),
                        pointColor, selected);
                  }
                  renderer.checkAndPrintGLError();
                  renderer.drawSphere(props, coords);
                  renderer.checkAndPrintGLError();
               }
               break;
            }
            case POINT:
               gl.glBegin (GL2.GL_POINTS);
               ArrayList<Vector3d> normals = getNormals();
               int numn = normals != null ? normals.size() : 0;
               Vector3d zDir = renderer.getZDirection();
               for (int i=0; i<myVertices.size(); i++) {
                  Vertex3d vtx = myVertices.get(i);
                  Point3d pnt = useRenderVtxs ? vtx.myRenderPnt : vtx.pnt;
                  
                  if (shading != Shading.NONE) {
                     if (i < numn) {
                        Vector3d nrm = myNormals.get(i);
                        gl.glNormal3d (nrm.x, nrm.y, nrm.z);
                     } else {
                        gl.glNormal3d(zDir.x, zDir.y, zDir.z);
                     }
                  }
                  gl.glVertex3d (pnt.x, pnt.y, pnt.z);
               }
               gl.glEnd ();
         }
         

         // render normals
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
            renderer.setLineWidth (1);
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
            renderer.checkAndPrintGLError();
            gl.glCallList (displayList);
            renderer.checkAndPrintGLError();
         }
      }
      else {
         gl.glCallList (displayList);
      }

      if (reenableLighting) {
         renderer.setLightingEnabled (true);
      }
      renderer.setPointSize (savedPointSize);
      renderer.setLineWidth (savedLineWidth);
      renderer.setShadeModel (savedShadeModel);

      gl.glPopMatrix();
      
      renderer.checkAndPrintGLError();
      
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

   /** 
    * Adds copies of the vertices of another mesh to this mesh.  If the other
    * mesh contains normal information, this will be added as well providing
    * this mesh already contains normal information or is empty. Otherwise,
    * normal information for this mesh will be cleared.
    * 
    * @param mesh Mesh to be added to this mesh
    */
   public void addMesh (PointMesh mesh) {
      super.addMesh (mesh);
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

}
