/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC), Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.util.ArrayList;

import javax.media.opengl.GL2;

import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.render.GLHSVShader;
import maspack.render.GLRenderer;
import maspack.render.GLRenderer.SelectionHighlighting;
import maspack.render.GLSupport;
import maspack.render.Material;
import maspack.render.RenderProps;
import maspack.render.Texture;
import maspack.render.TextureLoader;
import maspack.render.RenderProps.Shading;
import maspack.render.TextureProps;
import maspack.util.FunctionTimer;
import maspack.util.InternalErrorException;

public class MeshRenderer {

   // Extra render flags used internally. Starting numbering from 0x80000000 downwards
   // so as to not conflict with public flags in GLRenderer.

   /** 
    * Flag requesting that the renderer is in selection mode.
    */   
   static int IS_SELECTING = 0x80000000;
   
   /** 
    * Flag requesting that mesh edges should be drawn.
    */
   static int DRAW_EDGES = 0x40000000;
 
   // these flags are added to the flags defined in GLRenderer, so
   // we start with the high bits to make sure there is no overlap ...
   private static int COMPUTE_VERTEX_NORMALS = 0x8000;
   
   FunctionTimer timer = new FunctionTimer();
   // temporary storage, used in drawFacesRaw, for half edges used when
   // rendering a face
   HalfEdge[] myEdges = new HalfEdge[2];
   int myEdgeCnt = 0;
   int myLastEdgeCnt = 0;

   private int unpackEdges (ArrayList<Face> faces, int idx, boolean mergeQuads) {
      Face face = faces.get(idx);
      unpackEdges (face);
      if (mergeQuads && (face.myFlags & Face.FIRST_QUAD_TRIANGLE) != 0) {
         if (idx+1 == faces.size()) {
            throw new InternalErrorException (
               "End of face list while expecting another face for quad merge");
         }
         unpackExtraQuadEdges (faces.get(++idx));         
      }
      return idx;
   }

   private void unpackEdges (Face face) {
      HalfEdge he0 = face.he0;
      HalfEdge he = he0;
      myEdgeCnt = 0;
      do {
         if (myEdgeCnt >= myEdges.length) {
            growEdgeStorage();
         }
         myEdges[myEdgeCnt++] = he;
         he = he.next;
      }
      while (he != he0);
   }

   // Unpack a second set of quad edges from another triangular face. It is
   // assumed that the quad v0, v1, v2, v3 is formed from two triangles v0, v1,
   // v2 and v0, v2, v3. Hence we replace the first edge with that of the
   // second triangle, and append the last half edge.
   private void unpackExtraQuadEdges (Face face) {
      if (myEdgeCnt != 3) {
         throw new InternalErrorException ("First face not a triangle");
      }
      HalfEdge he0 = face.he0;
      myEdges[0] = he0;
      HalfEdge he = he0.next.next;
      myEdges[myEdgeCnt++] = he;
      if (he.next != he0) {
         throw new InternalErrorException ("Second face not a triangle");
      }
   }

   private void growEdgeStorage () {
      HalfEdge[] newEdges = new HalfEdge[2*myEdges.length];
      for (int i=0; i<myEdges.length; i++) {
         newEdges[i] = myEdges[i];
      }
      myEdges = newEdges;
   }      

   public MeshRenderer() {
   }

   private int getTextureMode (TextureProps tprops) {
      switch (tprops.getMode()) {
         case DECAL:
            return GL2.GL_DECAL;
         case REPLACE:
            return GL2.GL_REPLACE;
         case MODULATE:
            return GL2.GL_MODULATE;
         case BLEND:
            return GL2.GL_BLEND;
         default: {
            throw new InternalErrorException (
               "unimplement texture mode " + tprops.getMode());
         }
      }
   }

   private void loadTexture (GL2 gl, TextureProps tprops) {
      if (tprops.getFileName() == null) {
         tprops.setTexture (null);
         return;
      }

      TextureLoader loader = new TextureLoader (gl);
      try {
         Texture texture = loader.getTexture (tprops.getFileName());
         tprops.setTexture (texture);
      }
      catch (java.io.IOException e) {
         System.out.println (
            "Texture image file not found: " + tprops.getFileName() + " " + e);
         return;
      }

      gl.glTexParameteri (GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_REPEAT);
      gl.glTexParameteri (GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_REPEAT);
      gl.glTexParameteri (
         GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
      gl.glTexParameteri (
         GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR);
   }

   // Take care of inits for texture mapping. If doing automatic texture
   // mapping this is all you need to do. If not, the texture coordinates
   // are set in drawFaces.
   private void drawFaceTextures (GL2 gl, TextureProps tprops) {
      Texture texture = tprops.getTexture();
      int[] vals = new int[2];
      gl.glGetIntegerv (GL2.GL_POLYGON_MODE, vals, 0);
      // vals[0] has values GL_FILL or GL_LINE.
      // vals[0] front face vals[1] back face.
      // They should alwyas be the same
      if (vals[0] == GL2.GL_LINE) {
         gl.glDisable (GL2.GL_TEXTURE_2D);
      }
      else {
         gl.glEnable (GL2.GL_TEXTURE_2D);
      }

      if (tprops.isAutomatic()) {
         gl.glEnable (GL2.GL_TEXTURE_GEN_S);
         gl.glEnable (GL2.GL_TEXTURE_GEN_T);
         if (tprops.isSphereMappingEnabled()) {
            gl.glTexGeni (GL2.GL_S, GL2.GL_TEXTURE_GEN_MODE, GL2.GL_SPHERE_MAP);
            gl.glTexGeni (GL2.GL_T, GL2.GL_TEXTURE_GEN_MODE, GL2.GL_SPHERE_MAP);
         }
         else {
            gl.glTexGeni (GL2.GL_S, GL2.GL_TEXTURE_GEN_MODE, GL2.GL_OBJECT_LINEAR);
            gl.glTexGeni (GL2.GL_T, GL2.GL_TEXTURE_GEN_MODE, GL2.GL_OBJECT_LINEAR);
         }

         gl.glTexGendv (GL2.GL_S, GL2.GL_OBJECT_PLANE, tprops.getSCoords(), 0);
         gl.glTexGendv (GL2.GL_T, GL2.GL_OBJECT_PLANE, tprops.getTCoords(), 0);
      }

      gl.glTexEnvi (
         GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, getTextureMode (tprops));
      texture.bind (gl);
   }

   public static boolean useDisplayListsIfPossible = true;

   // private HalfEdge lastHardEdge (HalfEdge he0) {
   //    if (he0.myHardP) {
   //       return he0;
   //    }
   //    HalfEdge lastHard = null;
   //    HalfEdge he = he0.next.opposite;
   //    int cnt = 0;
   //    while (he != null && he != he0) {
   //       if (he.myHardP) {
   //          lastHard = he;
   //       }
   //       he = he.next.opposite;
   //       cnt++;
   //    }
   //    return lastHard;
   // }

   // private void computeVertexNormal (
   //    Vector3d nrm, HalfEdge he0, boolean useRenderNormals) {
   //    nrm.setZero();
   //    if (useRenderNormals) {
   //       nrm.add (he0.face.getRenderNormal());
   //       HalfEdge he = he0.next.opposite;
   //       while (he != null && he != he0 && !he.myHardP) {
   //          nrm.add (he.face.getRenderNormal());
   //          he = he.next.opposite;
   //       }
   //    }
   //    else {
   //       nrm.add (he0.face.getNormal());
   //       HalfEdge he = he0.next.opposite;
   //       while (he != null && he != he0 && !he.myHardP) {
   //          nrm.add (he.face.getNormal());
   //          he = he.next.opposite;
   //       }
   //    }
   //    nrm.normalize();
   //}

   //private static NumberFormat fmt = new NumberFormat ("%6.3f");

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

   float[] myColorBuf = new float[3];

   private void setVertexColor (GL2 gl, Vertex3d vtx, boolean useHSV) {
      float[] color = vtx.getColorArray();
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

   private void drawFacesRaw (
      GL2 gl, PolygonalMesh mesh, TextureProps textureProps, int flags) {
      // need to use begin/end polygon
      Vector3d nrm;
      Vector3d[] nrms = null;
      Vector3d vtxNrm = new Vector3d();
      int[] shadingModel = new int[1];

      gl.glGetIntegerv (GL2.GL_SHADE_MODEL, shadingModel, 0);

      boolean computeVertexNormals = (flags & COMPUTE_VERTEX_NORMALS) != 0;
      boolean useRenderNormals = mesh.isRenderBuffered() && !mesh.isFixed();
      boolean useVertexColors = (flags & GLRenderer.VERTEX_COLORING) != 0;
      boolean useHSVInterpolation =
         (flags & GLRenderer.HSV_COLOR_INTERPOLATION) != 0;
      useHSVInterpolation =false;
      boolean useTextureCoords =
         (textureProps != null && textureProps.isEnabled() &&
          !textureProps.isAutomatic() && mesh.myTextureIndices != null);
      // merge quad triangles if we are using smooth shading
      boolean mergeQuadTriangles = (shadingModel[0] == GL2.GL_SMOOTH);

      if ((flags & IS_SELECTING) != 0) {
         // don't set color while selecting
         useVertexColors = false;
      }
      
      if (computeVertexNormals) {
         nrms = new Vector3d[mesh.getNumVertices()];
         for (int v = 0; v < mesh.getNumVertices(); v++) {
            nrms[v] = new Vector3d();
            if (useRenderNormals) {
               mesh.getVertices().get (v).computeRenderNormal (nrms[v]);
            }
            else {
               mesh.getVertices().get (v).computeNormal (nrms[v]);
            }
         }
      }
      //int i = 0; // i is index of face
      if (useVertexColors && useHSVInterpolation) {
         useHSVInterpolation = setupHSVInterpolation (gl);
      }

      myLastEdgeCnt = 0;
      ArrayList<Face> faceList = mesh.getFaces();
      int[] faceOrder = mesh.getFaceOrder();
      int faceIdx;
      
      for (int i=0; i<faceList.size(); i++) {

         if (faceOrder == null) {
            faceIdx = i;
         } else {
            faceIdx = faceOrder[i];
         }
         faceIdx = unpackEdges (faceList, faceIdx, mergeQuadTriangles);
         Face face = faceList.get (faceIdx);
         
         // set the appropriate glBegin, depending on how many edges we
         // have. For triangles (or quads), the glBegin stays in force until we
         // are no longer rendering triangles (or quads).
         if (myEdgeCnt > 4) {
            gl.glBegin (GL2.GL_POLYGON);
         }
         else if (myLastEdgeCnt != myEdgeCnt) {
            if (myLastEdgeCnt == 3 || myLastEdgeCnt == 4) {
               // was rendering triangles or quads, so close previous glBegin.
               gl.glEnd();
            }
            if (myEdgeCnt == 3) {
               gl.glBegin (GL2.GL_TRIANGLES);
            }
            else {
               gl.glBegin (GL2.GL_QUADS);
            }
         }

         if (!computeVertexNormals) {
            Vector3d faceNrm;
            if (useRenderNormals) {
               faceNrm = face.getRenderNormal();
            }
            else {
               faceNrm = face.getNormal();
            }
            gl.glNormal3d (faceNrm.x, faceNrm.y, faceNrm.z);
         }

         for (int edgeIdx=0; edgeIdx<myEdgeCnt; edgeIdx++) {
            HalfEdge he = myEdges[edgeIdx];

            Vertex3d vtx = he.head;
            Point3d pnt = useRenderNormals ? vtx.myRenderPnt : vtx.pnt;

            if (computeVertexNormals) {
               HalfEdge lastHard = he.lastHardEdge();
               if (lastHard != null) {
                  lastHard.computeVertexNormal (vtxNrm, useRenderNormals);
                  gl.glNormal3d (vtxNrm.x, vtxNrm.y, vtxNrm.z);
               }
               else {
                  nrm = nrms[he.head.idx];
                  gl.glNormal3d (nrm.x, nrm.y, nrm.z);
               }
            }

            if (useTextureCoords) {
               int iv = mesh.myTextureIndices.get(faceIdx)[edgeIdx];
               // BUG: can be null. iv is index into texture list
               Vector3d vtext = (Vector3d)mesh.myTextureVertexList.get (iv);
               double sss = vtext.x;
               double ttt = vtext.y;
               gl.glTexCoord2f ((float)sss, (float)(1 - ttt));
            }
            if (useVertexColors) {
               setVertexColor (gl, vtx, useHSVInterpolation);
            }
            gl.glVertex3d (pnt.x, pnt.y, pnt.z);
         }
         if (myEdgeCnt > 4){
            // glBegin was set to POLYGON, so we close this now
            gl.glEnd();
         }
         myLastEdgeCnt = myEdgeCnt;
      }
      if (myLastEdgeCnt == 3 || myLastEdgeCnt == 4) {
         // was rendering triangles or quads, so close previous glBegin.
         gl.glEnd();
      }
      
      if (useVertexColors && useHSVInterpolation) {
         // turn off special HSV interpolating shader
         gl.glUseProgramObjectARB (0);
      }
   }

   private void drawEdges(GL2 gl, RenderProps props, 
      PolygonalMesh mesh, int flags) {
      
      boolean useDisplayList = false;
      int displayList = 0;

      boolean selecting = (flags & IS_SELECTING) != 0;
      boolean useVertexColors = (flags & GLRenderer.VERTEX_COLORING) != 0;
      
      // timer.start();

      if (useDisplayListsIfPossible && mesh.isUsingDisplayList() 
         // John Lloyd, 6-Oct-2013, not sure why display lists were disabled
         // when selecting - this certainly slowed things down
    	   //
    	   // Response: Antonio Sanchez: 11-Oct-2013
         // ... because display lists sometimes set color, e.g. when
         // rendering stress/strain.  This breaks the new selection
         // routine which depends on each object having a particular
         // unique colour.
         && !(selecting && useVertexColors) ) {  
         useDisplayList = true;
         displayList = props.getEdgeDisplayList();
      }
      if (!useDisplayList || displayList < 1) {
         if (useDisplayList) {
            displayList = props.allocEdgeDisplayList (gl);
            if (displayList > 0) {
               gl.glNewList (displayList, GL2.GL_COMPILE);
            }
         }
         
         drawEdgesRaw (gl, mesh, flags);
         
         if (useDisplayList && displayList > 0) {
            gl.glEndList();
            gl.glCallList (displayList);
         }
      }
      else {
         gl.glCallList (displayList);
      }
   }
   
   private void drawEdgesRaw (
      GL2 gl, PolygonalMesh mesh, int flags) {
      
      // need to use begin/end polygon
      int[] shadingModel = new int[1];

      gl.glGetIntegerv (GL2.GL_SHADE_MODEL, shadingModel, 0);

      boolean useRenderNormals = mesh.isRenderBuffered() && !mesh.isFixed();
      boolean useVertexColors = (flags & GLRenderer.VERTEX_COLORING) != 0;
      boolean useHSVInterpolation =
         (flags & GLRenderer.HSV_COLOR_INTERPOLATION) != 0;
      useHSVInterpolation =false;
      // XXX not clear when we want to merge quad triangles when drawing edges
      boolean mergeQuadTriangles = false;

      // int i = 0; // i is index of face
      if (useVertexColors && useHSVInterpolation) {
         useHSVInterpolation = setupHSVInterpolation (gl);
      }
      
      ArrayList<Face> faceList = mesh.getFaces();
      for (int faceIdx=0; faceIdx<faceList.size(); faceIdx++) {
         faceIdx = unpackEdges (faceList, faceIdx, mergeQuadTriangles);
         gl.glBegin (GL2.GL_LINE_LOOP);
         for (int edgeIdx=0; edgeIdx<myEdgeCnt; edgeIdx++) {
            HalfEdge he = myEdges[edgeIdx];

            Vertex3d vtx = he.head;
            Point3d pnt = useRenderNormals ? vtx.myRenderPnt : vtx.pnt;

            if (useVertexColors) {
               setVertexColor (gl, vtx, useHSVInterpolation);
            }
            gl.glVertex3d (pnt.x, pnt.y, pnt.z);
         }
         gl.glEnd();
      }
      if (useVertexColors && useHSVInterpolation) {
         // turn off special HSV interpolating shader
         gl.glUseProgramObjectARB (0);
      }
   }

   private void drawFaces (
      GL2 gl, PolygonalMesh mesh, RenderProps props, int flags) {
      boolean useDisplayList = false;
      int displayList = 0;

      boolean drawEdges = (flags & DRAW_EDGES) != 0;
      boolean selecting = (flags & IS_SELECTING) != 0;
      boolean useVertexColors = (flags & GLRenderer.VERTEX_COLORING) != 0;

      // timer.start();
      if (useDisplayListsIfPossible && mesh.isUsingDisplayList()
         // John Lloyd, 6-Oct-2013, not sure why display lists were disabled
         // when selecting - this certainly slowed things down
         // && !selecting*/) {  
         // Response: AntonioSanchez: 7-Oct-2013
    	 // ... because display lists sometimes set color, e.g. when
    	 // rendering stress/strain.  This breaks the new selection
    	 // routine which depends on each object having a particular
    	 // unique colour
         && !(selecting && useVertexColors)) {
    	  
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

         TextureProps textureProps = props.getTextureProps();
         if (textureProps != null && textureProps.isEnabled()) {
            // if not automatic check that explicit texture indices are okay
            if (!textureProps.isAutomatic() &&
                (mesh.myTextureIndices == null ||
                 mesh.myTextureIndices.size() != mesh.getNumFaces())) {
               textureProps.setEnabled (false);
               props.setTextureProps (textureProps);
            }
            else {
               if (textureProps.getTexture() == null &&
                   textureProps.textureFileExists()) {
                  loadTexture (gl, textureProps);
               }

               if (textureProps.getTexture() != null)
                  drawFaceTextures (gl, textureProps);

            }
         }

         byte[] savedCullFaceEnabled = new byte[1];
         int[] savedCullFaceMode = new int[1];

         gl.glGetBooleanv (GL2.GL_CULL_FACE, savedCullFaceEnabled, 0);
         gl.glGetIntegerv (GL2.GL_CULL_FACE_MODE, savedCullFaceMode, 0);

         RenderProps.Faces faces = props.getFaceStyle();
         if (drawEdges && faces == RenderProps.Faces.NONE) {
            faces = RenderProps.Faces.FRONT_AND_BACK;
         }
         switch (faces) {
            case FRONT_AND_BACK: {
               gl.glDisable (GL2.GL_CULL_FACE);
               break;
            }
            case FRONT: {
               gl.glCullFace (GL2.GL_BACK);
               break;
            }
            case BACK: {
               gl.glCullFace (GL2.GL_FRONT);
               break;
            }
            default:
               break;
         }
         drawFacesRaw (gl, mesh, textureProps, flags);

         if (savedCullFaceEnabled[0] != 0) {
            gl.glEnable (GL2.GL_CULL_FACE);
         }
         else {
            gl.glDisable (GL2.GL_CULL_FACE);
         }
         gl.glCullFace (savedCullFaceMode[0]);

         if (textureProps != null) {
            gl.glDisable (GL2.GL_TEXTURE_2D);
            if (textureProps.isAutomatic()) {
               gl.glDisable (GL2.GL_TEXTURE_GEN_S);
               gl.glDisable (GL2.GL_TEXTURE_GEN_T);
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

      // timer.stop();
      // System.out.println (
      // (!useDisplayList || displayList < 1) ? "SLOW " : "FAST");
      // System.out.println (timer.resultUsec(1));

   }

   public void renderEdges (
      GLRenderer renderer, PolygonalMesh mesh, RenderProps props, int flags) {
      
      if (mesh.getNumVertices() == 0) {
         return;
      }
      
      GL2 gl = renderer.getGL2().getGL2();
      gl.glPushMatrix();
      if (mesh.isRenderBuffered()) {
         renderer.mulTransform (mesh.myXMeshToWorldRender);
      }
      else {
         renderer.mulTransform (mesh.XMeshToWorld);
      }

      // boolean translucent = false;

      if (renderer.isSelecting()) {
         flags |= IS_SELECTING;
      }

      boolean reenableLighting = false;
      int[] savedLineWidth = new int[1];
      gl.glGetIntegerv (GL2.GL_LINE_WIDTH, savedLineWidth, 0);
      int[] savedShadeModel = new int[1];
      gl.glGetIntegerv (GL2.GL_SHADE_MODEL, savedShadeModel, 0);

      gl.glLineWidth (props.getEdgeWidth());

      if (!renderer.isSelecting()) {
         reenableLighting = renderer.isLightingEnabled();
         renderer.setLightingEnabled (false);
         float[] color;
         if ((flags & GLRenderer.SELECTED) != 0 &&
            props.getFaceStyle() == RenderProps.Faces.NONE) {
            color = myColorBuf;
            renderer.getSelectionColor().getRGBColorComponents (color);
         } else {
            color = props.getEdgeColorArray();
            if (color == null) {
               color = props.getLineColorArray();
            }
         }
         renderer.setColor (color);
      }
      if ((flags & GLRenderer.VERTEX_COLORING) != 0 &&
          !renderer.isSelecting()) {
         // smooth shading is needed to get line colors to interpolate
         gl.glShadeModel (GL2.GL_SMOOTH);
      } else {
         gl.glShadeModel (GL2.GL_FLAT);
      }
         
      drawEdges (gl, props, mesh, flags);
      //gl.glPolygonMode (GL2.GL_FRONT_AND_BACK, GL2.GL_LINE);
      //drawFaces (gl, mesh, props, flags | GLRenderer.DRAW_EDGES);
      //gl.glPolygonMode (GL2.GL_FRONT_AND_BACK, GL2.GL_FILL);

      if (reenableLighting) {
         renderer.setLightingEnabled (true);
      }
      gl.glLineWidth (savedLineWidth[0]);
      gl.glShadeModel (savedShadeModel[0]);

      gl.glPopMatrix();
      
   }
   
   public void render (
      GLRenderer renderer, PolygonalMesh mesh, RenderProps props,
      int flags) {
      if (mesh.getNumVertices() == 0) {
         return;
      }
      
      if ((flags & GLRenderer.CLEAR_MESH_DISPLAY_LISTS) > 0) {
         props.clearMeshDisplayList();
      }
      
      if (mesh.isRenderBuffered() && !mesh.isFixed()) {
         mesh.updateRenderNormals();
      }
      else {
         mesh.updateFaceNormals();
      }
      GL2 gl = renderer.getGL2();
      gl.glPushMatrix();
      if (mesh.isRenderBuffered()) {
         renderer.mulTransform (mesh.myXMeshToWorldRender);
      }
      else {
         renderer.mulTransform (mesh.XMeshToWorld);
      }

      boolean translucent = false;
      // boolean saveTransparencyEnabled = false;

      Shading shading = props.getShading();

      if (!renderer.isSelecting()) {
         translucent = isTranslucent(props);
         // if ((translucent = isTranslucent (props))) {
         //    it shouldn't normally be necessary to enable transparency, since
         //    a transparent mesh should be rendered by the viewer in a section
         //    where transparency is already enabled. Doing this just to be
         //    sure ...
         //    saveTransparencyEnabled = renderer.isTransparencyEnabled();
         //    renderer.setTransparencyEnabled (true);
         // }

         if (shading != Shading.NONE) {
            Material faceMat = mesh.getFaceMaterial();
            if (faceMat == null) {
               faceMat = props.getFaceMaterial();
            }
            Material backMat = mesh.getBackMaterial();
            if (backMat == null) {
               backMat = props.getBackMaterial();
            }
            if (backMat == null) {
               backMat = faceMat;
            }

            if (translucent && (flags & GLRenderer.SORT_FACES) > 0) {
               mesh.sortFaces(renderer.getZDirection());
            }
         
            if ((flags & GLRenderer.SELECTED) != 0 &&
                (renderer.getSelectionHighlighting() ==
                 SelectionHighlighting.Color)) {
               renderer.getSelectionMaterial().apply (gl, GL2.GL_FRONT_AND_BACK);
            }
            else if (faceMat == backMat) {
               faceMat.apply (gl, GL2.GL_FRONT_AND_BACK);
               gl.glLightModelf (GL2.GL_LIGHT_MODEL_TWO_SIDE, 1);
            }
            else {
               faceMat.apply (gl, GL2.GL_FRONT);
               backMat.apply (gl, GL2.GL_BACK);
               gl.glLightModelf (GL2.GL_LIGHT_MODEL_TWO_SIDE, 1);
            }
         }
      }

      if (renderer.isSelecting()) {
         flags |= IS_SELECTING;
      }

      if (props.getDrawEdges()) {
         boolean reenableLighting = false;
         int[] savedLineWidth = new int[1];
         gl.glGetIntegerv (GL2.GL_LINE_WIDTH, savedLineWidth, 0);
         int[] savedShadeModel = new int[1];
         gl.glGetIntegerv (GL2.GL_SHADE_MODEL, savedShadeModel, 0);

         gl.glLineWidth (props.getEdgeWidth());

         if (!renderer.isSelecting()) {
            reenableLighting = renderer.isLightingEnabled();
            renderer.setLightingEnabled (false);
            float[] color;
            if ((flags & GLRenderer.SELECTED) != 0 &&
                props.getFaceStyle() == RenderProps.Faces.NONE) {
               color = myColorBuf;
               renderer.getSelectionColor().getRGBColorComponents (color);
            }
            else {
               color = props.getEdgeColorArray();
               if (color == null) {
                  color = props.getLineColorArray();
               }
            }
            renderer.setColor (color);
         }
         if ((flags & GLRenderer.VERTEX_COLORING) != 0 &&
             !renderer.isSelecting()) {
            // smooth shading is needed to get line colors to interpolate
            gl.glShadeModel (GL2.GL_SMOOTH);
         }
         else {
            gl.glShadeModel (GL2.GL_FLAT);
         }
              
         drawEdges(gl, props, mesh, flags);
         //gl.glPolygonMode (GL2.GL_FRONT_AND_BACK, GL2.GL_LINE);
         //drawFaces (gl, mesh, props, flags | GLRenderer.DRAW_EDGES);
         //gl.glPolygonMode (GL2.GL_FRONT_AND_BACK, GL2.GL_FILL);

         if (reenableLighting) {
            renderer.setLightingEnabled (true);
         }
         gl.glLineWidth (savedLineWidth[0]);
         gl.glShadeModel (savedShadeModel[0]);
      }

      if (props.getFaceStyle() != RenderProps.Faces.NONE) {
         int[] savedShadeModel = new int[1];
         gl.glGetIntegerv (GL2.GL_SHADE_MODEL, savedShadeModel, 0);

         if (shading == Shading.NONE) {
            renderer.setLightingEnabled (false);
            renderer.setColor (
               props.getFaceColorArray(), (flags & GLRenderer.SELECTED) != 0);
         }
         else if (((shading != Shading.FLAT) ||
                   (flags & GLRenderer.VERTEX_COLORING) != 0) &&
                  !renderer.isSelecting()) {
            gl.glShadeModel (GL2.GL_SMOOTH);
            if ((flags & GLRenderer.VERTEX_COLORING) == 0){
               flags |= COMPUTE_VERTEX_NORMALS;
            }
         }
         else { // shading == Shading.FLAT
            gl.glShadeModel (GL2.GL_FLAT);
         }

         if (props.getDrawEdges()) {
            gl.glEnable (GL2.GL_POLYGON_OFFSET_FILL);
            gl.glPolygonOffset (1f, 1f);
         }
         if ((flags & GLRenderer.VERTEX_COLORING) != 0) {
            renderer.setLightingEnabled (false);
         }
         drawFaces (gl, mesh, props, flags);
         if ((flags & GLRenderer.VERTEX_COLORING) != 0) {
            renderer.setLightingEnabled (true);
         }
         if (props.getDrawEdges()) {
            gl.glDisable (GL2.GL_POLYGON_OFFSET_FILL);
         }
         if (shading == Shading.NONE) {
            renderer.setLightingEnabled (true);
         }
         gl.glShadeModel (savedShadeModel[0]);
      }

      if (!renderer.isSelecting()) {
         if (props.getBackMaterial() != null) {
            gl.glLightModelf (GL2.GL_LIGHT_MODEL_TWO_SIDE, 1f);
         }
         if (translucent) {
            //renderer.setTransparencyEnabled (saveTransparencyEnabled);
         }
      }

      gl.glPopMatrix();
   }
   
   
   public boolean isTranslucent (RenderProps props) {
      Material mat = props.getFaceMaterial();
      return mat.isTranslucent();
      // return (myFrontMaterial.isTranslucent() ||
      // myBackMaterial.isTranslucent());
   }

}
