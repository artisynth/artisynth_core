package maspack.render.GL.GL2;

import javax.media.opengl.GL2;

import maspack.geometry.Polyline;
import maspack.geometry.PolylineMesh;
import maspack.geometry.Vertex3d;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.render.RenderProps;
import maspack.render.RenderProps.LineStyle;
import maspack.render.RenderProps.Shading;
import maspack.render.Renderer;
import maspack.render.GL.GLHSVShader;
import maspack.render.GL.GLSupport;

public class PolylineMeshRenderer {

   private static class PolylineMeshPrint {
      PolylineMesh mesh;
      int version;
      LineStyle style;
      int slices;
      double r;
      Shading shading;
      boolean useVertexColors;
      boolean useTextures;

      public PolylineMeshPrint(PolylineMesh mesh, LineStyle style, int slices, double r,
         Shading shading, boolean useVertexColors, boolean useTextures) {
         this.mesh = mesh;
         this.version = mesh.getVersion();
         this.style = style;
         this.slices = slices;
         this.r = r;
         this.shading = shading;
         this.useVertexColors = useVertexColors;
         this.useTextures = useTextures;
      }

      @Override
      public int hashCode() {
         return mesh.hashCode() + version + style.hashCode()*17 +
         + slices*3
         + GLSupport.hashCode(r)*83 + shading.hashCode()*41 
         + (useVertexColors? 11 : 17) + (useTextures ? 19 : 23);
      }

      @Override
      public boolean equals(Object obj) {
         if (obj == null || obj.getClass() != this.getClass()) {
            return false;
         }
         PolylineMeshPrint other = (PolylineMeshPrint)obj;
         if (other.mesh != mesh || other.version != version 
            || other.style != style || other.slices != slices
            || other.r != r 
            || other.shading != shading
            || other.useVertexColors != useVertexColors 
            || other.useTextures != useTextures) {
            return false;
         }
         return true;
      }
   }

   public PolylineMeshRenderer() {
   }

   public void renderCylinders(
      Renderer renderer, PolylineMesh mesh, RenderProps props, int flags) {
      if (renderer instanceof GL2Viewer) {
         renderCylinders((GL2Viewer)renderer, mesh, props, flags);
      }
   }

   public void renderCylinders(
      GL2Viewer viewer, PolylineMesh mesh, RenderProps props, int flags) {

      GL2 gl = viewer.getGL2();
      GLSupport.checkAndPrintGLError(gl);
      
      gl.glPushMatrix();
      if (mesh.isRenderBuffered()) {
         viewer.mulTransform (mesh.getXMeshToWorldRender());
      }
      else {
         viewer.mulTransform (mesh.XMeshToWorld);
      }  

      int[] savedShadeModel = new int[1];
      gl.glGetIntegerv(GL2.GL_SHADE_MODEL, savedShadeModel, 0);
      boolean reenableLighting = false;
      boolean selected = (flags & Renderer.SELECTED) != 0;

      if (props.getLineColor() != null && !viewer.isSelecting()) {
         viewer.setMaterialAndShading(
            props, props.getLineMaterial(), selected);
      }
      boolean cull = gl.glIsEnabled(GL2.GL_CULL_FACE);
      if (cull) {
         gl.glDisable(GL2.GL_CULL_FACE);
      }

      boolean useVertexColors = !viewer.isSelecting() && (flags & Renderer.VERTEX_COLORING) != 0 && !selected;

      Shading shading = props.getShading();
      if (useVertexColors) {
         gl.glEnable(GL2.GL_COLOR_MATERIAL);
         gl.glColorMaterial(GL2.GL_FRONT, GL2.GL_DIFFUSE);
         reenableLighting = viewer.isLightingEnabled();
         if (reenableLighting) {
            viewer.setLightingEnabled(false);
         }
         gl.glShadeModel(GL2.GL_SMOOTH);
         shading = Shading.NONE;
      }

      boolean useHSVInterpolation =(flags & Renderer.HSV_COLOR_INTERPOLATION) != 0;
      useHSVInterpolation = false;
      if (useVertexColors && useHSVInterpolation) {
         useHSVInterpolation = setupHSVInterpolation(gl);
      }

      boolean useDisplayList = false;
      int displayList = 0;
      boolean compile = true;
      GLSupport.checkAndPrintGLError(gl);
      
      if (!(viewer.isSelecting() && useVertexColors)) {
         useDisplayList = true;
         PolylineMeshPrint linePrint = new PolylineMeshPrint(mesh, LineStyle.CYLINDER,
            props.getLineSlices(),
            props.getLineRadius(), shading, useVertexColors, false);
         DisplayListKey lineKey = new DisplayListKey(mesh, 0);
         displayList = viewer.getDisplayList(gl, lineKey, linePrint);
         if (displayList < 0) {
            displayList = -displayList;
            compile = true;
            useDisplayList = true;
         } else {
            compile = false;
            useDisplayList = (displayList > 0);
         }
      }

      if (!useDisplayList || compile) {
         if (useDisplayList) {
            gl.glNewList (displayList, GL2.GL_COMPILE);
         }

         // draw all the cylinders
         boolean useRenderVtxs = mesh.isRenderBuffered() && !mesh.isFixed();
         float[] posa = new float[3];
         float[] posb = new float[3];
         float[] postmp = posb;
         int nslices = props.getLineSlices();
         double r = props.getLineRadius();

         for (int i = 0; i < mesh.getNumLines(); i = i + 1 + mesh.getRenderSkip()) {
            Polyline line = mesh.getPolyLine(i);

            Vertex3d[] vtxs = line.getVertices();
            Point3d pnta = useRenderVtxs ? vtxs[0].myRenderPnt : vtxs[0].pnt;
            mesh.getFloatPoint(pnta, posa);

            for (int k = 1; k < line.numVertices(); k++) {
               Point3d pntb = useRenderVtxs ? vtxs[k].myRenderPnt : vtxs[k].pnt;
               mesh.getFloatPoint(pntb, posb);

               if (useVertexColors) {
                  drawColoredCylinder(
                     gl, nslices, r, r, posa, vtxs[k - 1].getColorArray(),
                     posb, vtxs[k].getColorArray(), false);

               } else {
                  viewer.drawCylinder(props, posa, posb, true);
               }
               postmp = posa;
               posa = posb;
               posb = postmp;
            }
         }

         if (useDisplayList) {
            gl.glEndList();
            gl.glCallList(displayList);
         }
      } else {
         gl.glCallList(displayList);
      }

      if (useVertexColors) {
         gl.glDisable(GL2.GL_COLOR_MATERIAL);
         if (reenableLighting) {
            viewer.setLightingEnabled(true);
         }
      }
      if (useVertexColors && useHSVInterpolation) {
         // turn off special HSV interpolating shader
         gl.glUseProgramObjectARB(0);
      }

      if (cull) {
         gl.glEnable(GL2.GL_CULL_FACE);
      }
      viewer.restoreShading(props);
      gl.glShadeModel(savedShadeModel[0]);

      gl.glPopMatrix();
      GLSupport.checkAndPrintGLError(gl);
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
      GL2Viewer.mulTransform (gl, Xtmp);

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

      double topr = top/base;
      float dr = (float)(1.0/Math.sqrt(2+topr*topr-2*topr));
      
      // draw sides
      gl.glBegin(GL2.GL_QUAD_STRIP);

      double c1,s1;
      for (int i = 0; i <= nslices; i++) {
         c1 = cosBuff[i];
         s1 = sinBuff[i];
         gl.glNormal3d(dr*c1, dr*s1, (1-topr)*dr);
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

   public void renderLines(Renderer renderer, PolylineMesh mesh, RenderProps props, int flags) {
      if (renderer instanceof GL2Viewer) {
         renderLines((GL2Viewer)renderer, mesh, props, flags);
      }
   }

   public void renderLines(GL2Viewer viewer, PolylineMesh mesh, RenderProps props, int flags) {

      GL2 gl = viewer.getGL2();
      GLSupport.checkAndPrintGLError(gl);
      
      gl.glPushMatrix();
      if (mesh.isRenderBuffered()) {
         viewer.mulTransform (mesh.getXMeshToWorldRender());
      }
      else {
         viewer.mulTransform (mesh.XMeshToWorld);
      }  

      boolean reenableLighting = false;
      int[] savedLineWidth = new int[1];
      gl.glGetIntegerv(GL2.GL_LINE_WIDTH, savedLineWidth, 0);
      int[] savedShadeModel = new int[1];
      gl.glGetIntegerv(GL2.GL_SHADE_MODEL, savedShadeModel, 0);

      gl.glLineWidth(props.getLineWidth());

      boolean selected = ((flags & Renderer.SELECTED) != 0);

      if (props.getLineColor() != null && !viewer.isSelecting()) {
         reenableLighting = viewer.isLightingEnabled();

         viewer.setLightingEnabled(false);
         float[] color;
         if ((flags & Renderer.SELECTED) != 0) {
            color = new float[3];
            viewer.getSelectionColor().getRGBColorComponents(color);
         } else {
            color = props.getLineColorArray();
         }
         float alpha = (float)props.getAlpha();
         viewer.setColor(color[0], color[1], color[2], alpha);
      }

      boolean useVertexColors = !viewer.isSelecting() && (flags & Renderer.VERTEX_COLORING) != 0 && !selected;
      boolean useHSVInterpolation = (flags & Renderer.HSV_COLOR_INTERPOLATION) != 0;
      useHSVInterpolation = false;
      if (useVertexColors && useHSVInterpolation) {
         useHSVInterpolation = setupHSVInterpolation(gl);
      }
      
      Shading shading = props.getShading();

      if (useVertexColors) {
         reenableLighting = viewer.isLightingEnabled();
         viewer.setLightingEnabled(false);
         gl.glShadeModel(GL2.GL_SMOOTH);
         shading = Shading.NONE;
      }

      boolean useDisplayList = false;
      int displayList = 0;
      boolean compile = true;
      
      if (!(viewer.isSelecting() && useVertexColors)) {
         useDisplayList = true;
         PolylineMeshPrint linePrint = new PolylineMeshPrint(mesh, LineStyle.LINE, 0, 0,
            shading, useVertexColors, false);
         DisplayListKey lineKey = new DisplayListKey(mesh, 0);
         displayList = viewer.getDisplayList(gl, lineKey, linePrint);
         if (displayList < 0) {
            displayList = -displayList;
            compile = true;
            useDisplayList = true;
         } else {
            compile = false;
            useDisplayList = (displayList > 0);
         }
      }

      if (!useDisplayList || compile) {
         if (useDisplayList) {
            gl.glNewList (displayList, GL2.GL_COMPILE);
         }

         boolean useRenderVtxs = mesh.isRenderBuffered() && !mesh.isFixed();
         for (int i = 0; i < mesh.getNumLines(); i = i + 1 + mesh.getRenderSkip()) {
            Polyline line = mesh.getPolyLine(i);
            gl.glBegin(GL2.GL_LINE_STRIP);
            Vertex3d[] vtxs = line.getVertices();
            for (int k = 0; k < line.numVertices(); k++) {
               Point3d pnt = useRenderVtxs ? vtxs[k].myRenderPnt : vtxs[k].pnt;

               if (useVertexColors) {
                  setVertexColor(gl, vtxs[k], useHSVInterpolation);
               }
               gl.glVertex3d(pnt.x, pnt.y, pnt.z);
            }
            gl.glEnd();
         }
         
         if (useDisplayList) {
            gl.glEndList();
            gl.glCallList(displayList);
         }
      } else {
         gl.glCallList(displayList);
      }
      GLSupport.checkAndPrintGLError(gl);

      if (useVertexColors && useHSVInterpolation) {
         // turn off special HSV interpolating shader
         gl.glUseProgramObjectARB(0);
      }
      
      if (reenableLighting) {
         viewer.setLightingEnabled(true);
      }
      gl.glLineWidth(savedLineWidth[0]);
      gl.glShadeModel(savedShadeModel[0]);

      gl.glPopMatrix();
   }

   float[] myColorBuf = new float[4];
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

}
