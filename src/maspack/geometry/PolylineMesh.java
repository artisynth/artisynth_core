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
import java.util.List;

import javax.media.opengl.GL2;

import maspack.geometry.io.WavefrontReader;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.properties.HasProperties;
import maspack.render.GLHSVShader;
import maspack.render.GLRenderer;
import maspack.render.GLSupport;
import maspack.render.GLViewer;
import maspack.render.RenderProps;
import maspack.util.FunctionTimer;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import maspack.util.ArraySupport;

/**
 * Implements a mesh consisting of a set of polylines.
 */
public class PolylineMesh extends MeshBase {
   protected ArrayList<Polyline> myLines = new ArrayList<Polyline>();

   public static boolean useDisplayListsIfPossible = true;
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
    * Creates a polygonal mesh and initializes it from an file in Alias
    * Wavefront obj format, as decribed for the method
    * {@link #write(PrintWriter,NumberFormat,boolean)}.
    * 
    * @param file
    * file containing the mesh description
    */
   public PolylineMesh (File file) throws IOException {
      this();
      read (new BufferedReader (new FileReader (file)));
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

//   public void setFromWavefrontReader (WavefrontReader wfr) throws IOException {
//      setFromWavefrontReader (wfr, null);
//   }

//   public void setFromWavefrontReader (WavefrontReader wfr, String groupName)
//      throws IOException {
//
//      if (groupName == null) {
//         String[] nameList = wfr.getPolylineGroupNames();
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
//      int[][] indices = wfr.getLocalLineIndicesAndVertices (vtxList);
//
//      for (int i=0; i<vtxList.size(); i++) {
//         // add by reference since points have already been copied 
//         addVertex (vtxList.get(i), /* byReference= */true);
//      }
//      if (indices != null) {
//         for (int k=0; k<indices.length; k++) {
//            addLine (indices[k]);
//         }
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

   public void render (GLRenderer renderer, RenderProps props, int flags) {
      GL2 gl = renderer.getGL2().getGL2();

      gl.glPushMatrix();
      if (isRenderBuffered()) {
         renderer.mulTransform (myXMeshToWorldRender);
      }
      else {
         renderer.mulTransform (XMeshToWorld);
      }  
      
//      // check display list
//      boolean useDisplayList = false;
//      int displayList = 0;
//
//      if (useDisplayListsIfPossible && isUsingDisplayList() && !renderer.isSelecting()) {
//         useDisplayList = true;
//         displayList = props.getMeshDisplayList();
//      }
//         
//      if (!useDisplayList || displayList < 1) {
//         if (useDisplayList) {
//            displayList = props.allocMeshDisplayList (gl);
//            if (displayList > 0) {
//               gl.glNewList (displayList, GL2.GL_COMPILE);
//            }
//         }
      
         switch (props.getLineStyle()) {
            case CYLINDER:
               renderCylinders(renderer, props, flags);
               break;
            case LINE:
            case ELLIPSOID:
            case SOLID_ARROW:
               renderLines(renderer, props, flags);
               break;
         }
//         if (useDisplayList && displayList > 0) {
//            gl.glEndList();
//            gl.glCallList (displayList);
//         }
//      }
//      else {
//         gl.glCallList (displayList);
//      }

      gl.glPopMatrix();
   }
   
   private boolean setupHSVInterpolation (GL2 gl) {
      // create special HSV shader to interpolate colors in HSV space
      int prog = GLHSVShader.getShaderProgram(gl);
      if (prog > 0) {
         gl.glUseProgramObjectARB (prog);
         return true;
      }
      else {
         // HSV interpolaation not supported on this system
         return false;
      }
   }
   
   float[] myColorBuf = new float[4];
   private void setVertexColor (GL2 gl, float[] color, boolean useHSV) {
      if (color != null) {
         if (useHSV) {
            // convert color to HSV representation
            GLSupport.RGBtoHSV (myColorBuf, color);
            gl.glColor4f (
               myColorBuf[0], myColorBuf[1], myColorBuf[2], myColorBuf[3]);
         }
         else {
            gl.glColor4f (
               color[0], color[1], color[2], color[3]);
         }
      }
   }
   
   protected void renderCylinders(GLRenderer renderer, RenderProps props, int flags) {
      
      GL2 gl = renderer.getGL2().getGL2();
      
      RenderProps.Shading savedShadeModel = renderer.getShadeModel();
      boolean reenableLighting = false;
      
      if (props.getLineColor() != null && !renderer.isSelecting()) {
         renderer.setMaterialAndShading (
            props, props.getLineMaterial(), (flags & GLRenderer.SELECTED) != 0);
      }
      boolean cull = gl.glIsEnabled(GL2.GL_CULL_FACE);
      if (cull) {
         gl.glDisable (GL2.GL_CULL_FACE);
      }

      boolean useDisplayList = false;
      int displayList = 0;
      boolean useVertexColors = (flags & GLRenderer.VERTEX_COLORING) != 0;
      
      if (useDisplayListsIfPossible && isUsingDisplayList() && 
    		  !(renderer.isSelecting() && useVertexColors) ) {
         useDisplayList = true;
         displayList = props.getMeshDisplayList();
      }
         
      if (!useDisplayList || displayList < 1) {
         if (useDisplayList) {
            displayList = props.allocMeshDisplayList (gl);
            if (displayList > 0) {
               renderer.validateInternalDisplayLists(props);
               gl.glNewList (displayList, GL2.GL_COMPILE);
            }
         }
         
         if ((flags & MeshRenderer.IS_SELECTING) != 0) {
            useVertexColors = false;
         }
         boolean useHSVInterpolation =
            (flags & GLRenderer.HSV_COLOR_INTERPOLATION) != 0;
         useHSVInterpolation =false;
         if (useVertexColors && useHSVInterpolation) {
            useHSVInterpolation = setupHSVInterpolation (gl);
         }
         if (useVertexColors) {
            gl.glEnable(GL2.GL_COLOR_MATERIAL);
            gl.glColorMaterial(GL2.GL_FRONT, GL2.GL_DIFFUSE);
            reenableLighting = renderer.isLightingEnabled();
            if (reenableLighting) {
               renderer.setLightingEnabled (false);
            }
            renderer.setShadeModel (RenderProps.Shading.FLAT);
         }
         
         // draw all the cylinders
         boolean useRenderVtxs = isRenderBuffered() && !isFixed();
         float[] posa = new float[3];
         float[] posb = new float[3];
         float[] postmp = posb;
         int nslices =  props.getLineSlices();
         double r = props.getLineRadius();

         int[] indexOffs = getFeatureIndexOffsets();
         ArrayList<float[]> colors = getColors();
         int[] colorIndices = getColorIndices();
         
         for (int i=0; i<myLines.size(); i=i+1+renderSkip) {
            Polyline line = myLines.get(i);
           
            Vertex3d[] vtxs = line.getVertices();
            Point3d pnta = useRenderVtxs ? vtxs[0].myRenderPnt : vtxs[0].pnt;
            getFloatPoint(pnta, posa);
            int lineOff = indexOffs[i];
            
            for (int k=1; k<line.numVertices(); k++) {
               Point3d pntb = useRenderVtxs ? vtxs[k].myRenderPnt : vtxs[k].pnt;
               getFloatPoint(pntb, posb);
               
               if (useVertexColors) {
                  float[] color0 = colors.get(colorIndices[lineOff+k-1]);
                  float[] color1 = colors.get(colorIndices[lineOff+k]);
                  drawColoredCylinder(
                     gl, nslices, r, r, posa, color0, posb, color1, false);
                  
               } else {
                  renderer.drawCylinder(props, posa, posb, true);
               }
               postmp = posa;
               posa = posb;
               posb = postmp;               
            }
         }
         
         if (useVertexColors) {
            gl.glDisable(GL2.GL_COLOR_MATERIAL);
            if (reenableLighting) {
               renderer.setLightingEnabled (true);
            }
         }
         if (useVertexColors && useHSVInterpolation) {
            // turn off special HSV interpolating shader
            gl.glUseProgramObjectARB (0);
         }
         
         if (useDisplayList && displayList > 0) {
            gl.glEndList();
            gl.glCallList (displayList);
         }
      }
      else {
         gl.glCallList (displayList);
      }

      if (cull) {
         gl.glEnable(GL2.GL_CULL_FACE);
      }
      renderer.restoreShading (props);
      renderer.setShadeModel (savedShadeModel);
      
   }
   
   private void getFloatPoint(Point3d pnt, float[] flt) {
      flt[0] = (float)pnt.x;
      flt[1] = (float)pnt.y;
      flt[2] = (float)pnt.z;
   }
   
   Vector3d utmp = new Vector3d();
   RigidTransform3d Xtmp = new RigidTransform3d();
   
   double[] cosBuff = {1, 0, -1, 0, 1};
   double[] sinBuff = {0, 1, 0, -1, 0};
   
   // draws colored cylinders, currently ignores lighting
   public void drawColoredCylinder (GL2 gl, int nslices, double base,
      double top, float[] coords0, float[] color0, float[] coords1, 
      float[] color1, boolean capped) {
      
      utmp.set (coords1[0] - coords0[0], coords1[1] - coords0[1], coords1[2]
         - coords0[2]);

      Xtmp.p.set (coords0[0], coords0[1], coords0[2]);
      Xtmp.R.setZDirection (utmp);
      gl.glPushMatrix();
      GLViewer.mulTransform (gl, Xtmp);

      double h = utmp.norm();
      
      // fill angle buffer
      if (nslices+1 != cosBuff.length) {
         cosBuff = new double[nslices+1];
         sinBuff = new double[nslices+1];
         cosBuff[0] = 1;
         sinBuff[0] = 0;
         cosBuff[nslices] = 1;
         sinBuff[nslices] = 0;
         for (int i=1; i<nslices; i++) {
            double ang = i / (double)nslices * 2 * Math.PI;
            cosBuff[i] = Math.cos(ang);
            sinBuff[i] = Math.sin(ang);
         }
      }
      
      // draw sides
      gl.glBegin(GL2.GL_QUAD_STRIP);

      double c1,s1;
      for (int i = 0; i <= nslices; i++) {
         c1 = cosBuff[i];
         s1 = sinBuff[i];
         gl.glNormal3d(-c1, -s1, 0);
         
         gl.glColor4fv (color0, 0);
         gl.glVertex3d (base * c1, base * s1, 0);
         
         gl.glColor4fv (color1, 0);
         gl.glVertex3d (top * c1, top * s1, h);
      }
      
      gl.glEnd();
      
      
      if (capped) { // draw top cap first
         gl.glColor4fv(color1, 0);
         if (top > 0) {
            gl.glBegin (GL2.GL_POLYGON);
            gl.glNormal3d (0, 0, 1);
            for (int i = 0; i < nslices; i++) {
               gl.glVertex3d (top * cosBuff[i], top * sinBuff[i], h);
            }
            gl.glEnd();
         }
         // now draw bottom cap
         gl.glColor4fv(color0, 0);
         if (base > 0) {
            gl.glBegin (GL2.GL_POLYGON);
            gl.glNormal3d (0, 0, -1);
            for (int i = 0; i < nslices; i++) {
               gl.glVertex3d (base * cosBuff[i], base * sinBuff[i], 0);
            }
            gl.glEnd();
         }
      }
      gl.glPopMatrix();
      
   }

   protected void renderLines(GLRenderer renderer, RenderProps props, int flags ) {
      
      GL2 gl = renderer.getGL2().getGL2();
      
      boolean reenableLighting = false;
      int savedLineWidth = renderer.getLineWidth();
      RenderProps.Shading savedShadeModel = renderer.getShadeModel();

      renderer.setLineWidth (props.getLineWidth());

      if (props.getLineColor() != null && !renderer.isSelecting()) {
         reenableLighting = renderer.isLightingEnabled();
         
         renderer.setLightingEnabled (false);
         float[] color;
         if ((flags & GLRenderer.SELECTED) != 0) {
            color = new float[3];
            renderer.getSelectionColor().getRGBColorComponents (color);
         }
         else {
            color = props.getLineColorArray();
         }
         float alpha = (float)props.getAlpha();
         renderer.setColor (color[0], color[1], color[2], alpha);
      }

      boolean useDisplayList = false;
      int displayList = 0;
      boolean useVertexColors = (flags & GLRenderer.VERTEX_COLORING) != 0;
      
      if (useDisplayListsIfPossible && isUsingDisplayList() && 
    		  !(renderer.isSelecting() && useVertexColors)) {
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
         
         boolean useHSVInterpolation =
            (flags & GLRenderer.HSV_COLOR_INTERPOLATION) != 0;
         useHSVInterpolation =false;
         if (useVertexColors && useHSVInterpolation) {
            useHSVInterpolation = setupHSVInterpolation (gl);
         }
         
         boolean useRenderVtxs = isRenderBuffered() && !isFixed();
         if (useVertexColors) {
            reenableLighting = renderer.isLightingEnabled();
            renderer.setLightingEnabled (false);
            renderer.setShadeModel (RenderProps.Shading.FLAT);
         }
         
         int[] indexOffs = getFeatureIndexOffsets();
         ArrayList<float[]> colors = getColors();
         int[] colorIndices = getColorIndices();
         
         for (int i=0; i<myLines.size(); i=i+1+renderSkip) {
            Polyline line = myLines.get(i);
            gl.glBegin (GL2.GL_LINE_STRIP);
            Vertex3d[] vtxs = line.getVertices();
            int lineOff = indexOffs[i];
            
            for (int k=0; k<line.numVertices(); k++) {
               Point3d pnt = useRenderVtxs ? vtxs[k].myRenderPnt : vtxs[k].pnt;

               if (useVertexColors) {
                  int cidx = colorIndices[lineOff+k];
                  float[] color = (cidx != -1 ? colors.get(cidx) : null);
                  setVertexColor (gl, color, useHSVInterpolation);
               }
               gl.glVertex3d (pnt.x, pnt.y, pnt.z);
            }
            gl.glEnd ();
            
            if (useVertexColors && useHSVInterpolation) {
               // turn off special HSV interpolating shader
               gl.glUseProgramObjectARB (0);
            }
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
      renderer.setLineWidth (savedLineWidth);
      renderer.setShadeModel (savedShadeModel);
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

   /** 
    * Adds copies of the vertices and lines of another mesh to this mesh.
    * 
    * @param mesh Mesh to be added to this mesh
    */
   public void addMesh (PolylineMesh mesh) {

      int voff = myVertices.size();
      super.addMesh (mesh);
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
   
}
