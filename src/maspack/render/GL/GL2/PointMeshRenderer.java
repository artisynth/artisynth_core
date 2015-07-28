package maspack.render.GL.GL2;

import java.awt.Color;

import maspack.geometry.PointMesh;
import maspack.geometry.Vertex3d;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.render.RenderProps;
import maspack.render.RenderProps.PointStyle;
import maspack.render.RenderProps.Shading;
import maspack.render.Renderer;
import maspack.render.GL.GLSupport;
import maspack.render.GL.GL2.DisplayListManager.DisplayListPassport;

import javax.media.opengl.GL2;

public class PointMeshRenderer {

   private static final int POINT_KEY_ID = 0;
   private static final int NORMAL_KEY_ID = 1;
   
   private static class NormalMeshPrint {
      PointMesh mesh;
      int version;
      double normalLen;

      public NormalMeshPrint(PointMesh mesh, double normalLen) {
         this.mesh = mesh;
         this.version = mesh.getVersion();
         this.normalLen = normalLen;
      }

      @Override
      public int hashCode() {
         return mesh.hashCode() + version + Double.hashCode(normalLen)*67;
      }

      @Override
      public boolean equals(Object obj) {
         if (obj == null || obj.getClass() != this.getClass()) {
            return false;
         }
         NormalMeshPrint other = (NormalMeshPrint)obj;
         if (other.mesh != mesh || other.version != version || 
         this.normalLen != other.normalLen) {
            return false;
         }
         return true;
      }
   }

   private static class PointMeshPrint {
      PointMesh mesh;
      int version;
      int sphereDL;
      PointStyle style;
      double r;
      Shading shading;
      boolean useVertexColors;
      boolean useTextures;

      public PointMeshPrint(PointMesh mesh, PointStyle style, double r,
         int sphereDL, Shading shading, boolean useVertexColors, 
         boolean useTextures) {
         this.mesh = mesh;
         this.version = mesh.getVersion();
         this.style = style;
         this.r = r;
         this.sphereDL = sphereDL;
         this.shading = shading;
         this.useVertexColors = useVertexColors;
         this.useTextures = useTextures;
      }

      @Override
      public int hashCode() {
         return mesh.hashCode() + version + style.hashCode()*17 + sphereDL*7
         + Double.hashCode(r)*83 + shading.hashCode()*41 
         + (useVertexColors? 11 : 17) + (useTextures ? 19 : 23);
      }

      @Override
      public boolean equals(Object obj) {
         if (obj == null || obj.getClass() != this.getClass()) {
            return false;
         }
         PointMeshPrint other = (PointMeshPrint)obj;
         if (other.mesh != mesh || other.version != version || 
            other.style != style || other.r != r ||
            sphereDL != other.sphereDL  || other.shading != shading || 
            other.useVertexColors != useVertexColors || 
            other.useTextures != useTextures) {
            return false;
         }
         return true;
      }
   }
   
   public PointMeshRenderer() {
   }

   public void render (Renderer renderer, PointMesh mesh, RenderProps props, int flags ) {

      if (renderer instanceof GL2Viewer) {
         GL2Viewer viewer = (GL2Viewer)renderer;

         GL2 gl = viewer.getGL2();

         gl.glPushMatrix();
         if (mesh.isRenderBuffered()) {
            viewer.mulTransform (mesh.getXMeshToWorldRender());
         }
         else {
            viewer.mulTransform (mesh.XMeshToWorld);
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
         if (renderer.isSelecting()) {
            shading = Shading.NONE;
         }
         boolean selected = ((flags & Renderer.SELECTED) != 0);

         if (props.getPointColor() != null && !viewer.isSelecting()) {
            if (shading != Shading.NONE) {
               viewer.updateMaterial(props,
                  props.getPointMaterial(), selected);
            }
            else {
               reenableLighting = viewer.isLightingEnabled();
               viewer.setLightingEnabled (false);
               float[] color;
               if ((flags & Renderer.SELECTED) != 0) {
                  color = new float[3];
                  viewer.getSelectionColor().getRGBColorComponents (color);
               }
               else {
                  color = props.getPointColorArray();
               }
               float alpha = (float)props.getAlpha();
               viewer.setColor (color[0], color[1], color[2], alpha);
            }
         }

         boolean useDisplayList = false;
         PointMeshPrint pointPrint = null;
         DisplayListPassport pointPP = null;
         DisplayListKey pointKey = new DisplayListKey(mesh, POINT_KEY_ID);
         boolean compile = false;
         
         boolean useVertexColors = !selected && (flags & Renderer.VERTEX_COLORING) != 0;
         PointStyle pointStyle = props.getPointStyle();

         if ( !(viewer.isSelecting() && useVertexColors) ) {            
            int sphereDL = -1;
            double sphereRad = 0;
            if (pointStyle == PointStyle.SPHERE) {
               sphereDL = viewer.getSphereDisplayList(gl, props.getPointSlices());
               sphereRad = props.getPointRadius();
            }

            pointPrint = new PointMeshPrint(mesh, props.getPointStyle(), sphereRad,
               sphereDL, shading, useVertexColors, false);
            pointPP = viewer.getDisplayListPassport(gl, pointKey);
            if (pointPP == null) {
               // allocate new display list
               pointPP = viewer.allocateDisplayListPassport(gl, pointKey, pointPrint);
               compile = true;
            } else {
               // check passport
               compile = !(pointPP.compareExchangeFingerPrint(pointPrint));
            }
            useDisplayList = true;
         }
        
         if (!useDisplayList || compile) {
            if (useDisplayList && pointPP != null) {
               gl.glNewList (pointPP.getList(), GL2.GL_COMPILE);
            }

            boolean useRenderVtxs = mesh.isRenderBuffered() && !mesh.isFixed();
            float[] coords = new float[3];
            switch (props.getPointStyle()) {
               case SPHERE: {
                  float [] pointColor = new float[4];
                  for (int i=0; i<mesh.getNumVertices(); i++) {
                     Vertex3d vtx = mesh.getVertex(i);
                     Point3d pnt = useRenderVtxs ? vtx.myRenderPnt : vtx.pnt;
                     pnt.get(coords);

                     if (useVertexColors) {
                        Color c = mesh.getVertexColor(i);
                        c.getColorComponents(pointColor);
                        viewer.updateMaterial(props, props.getPointMaterial(),
                           pointColor, selected);
                     }
                     viewer.drawSphere(props, coords);
                  }
                  break;
               }
               case POINT:
                  gl.glBegin (GL2.GL_POINTS);
                  int numn = mesh.getNumNormals();
                  Vector3d zDir = viewer.getZDirection();
                  for (int i=0; i<mesh.getNumVertices(); i++) {
                     Vertex3d vtx = mesh.getVertex(i);
                     Point3d pnt = useRenderVtxs ? vtx.myRenderPnt : vtx.pnt;

                     if (shading != Shading.NONE && mesh.hasNormals()) {
                        if (i < numn) {
                           Vector3d nrm = mesh.getNormal(i);
                           gl.glNormal3d (nrm.x, nrm.y, nrm.z);
                        } else {
                           gl.glNormal3d(zDir.x, zDir.y, zDir.z);
                        }
                     }
                     gl.glVertex3d (pnt.x, pnt.y, pnt.z);
                  }
                  gl.glEnd ();
            }

            if (useDisplayList && pointPP != null) {
               gl.glEndList();
               gl.glCallList (pointPP.getList());
            }
         } else {
            gl.glCallList (pointPP.getList());
         }
         GLSupport.checkAndPrintGLError(gl);

         // render normals
         useDisplayList = false;
         NormalMeshPrint nrmPrint = null;
         DisplayListPassport nrmPP = null;
         DisplayListKey nrmKey = new DisplayListKey(mesh, NORMAL_KEY_ID);
         compile = false;
         
         double normalLen = mesh.getNormalRenderLen();

         if (mesh.hasNormals() && normalLen > 0) {
            if (props.getLineColor() != null && !viewer.isSelecting()) {
               if (shading != Shading.NONE) {
                  viewer.setMaterial(
                     props.getLineMaterial(), (flags & Renderer.SELECTED) != 0);
               }
               else {
                  float[] color = props.getLineColorArray();
                  float alpha = (float)props.getAlpha();
                  viewer.setColor (color[0], color[1], color[2], alpha);
               }
            }
            gl.glLineWidth (1);

            if ( !(viewer.isSelecting() && useVertexColors) ) {            
               nrmPrint = new NormalMeshPrint(mesh, normalLen);
               nrmPP = viewer.getDisplayListPassport(gl, nrmKey);
               if (nrmPP == null) {
                  // allocate new display list
                  nrmPP = viewer.allocateDisplayListPassport(gl, nrmKey, nrmPrint);
                  compile = true;
               } else {
                  // check passport
                  compile = !(nrmPP.compareExchangeFingerPrint(nrmPrint));
               }
               useDisplayList = true;
            }

            if (!useDisplayList || compile) {
               
               if (useDisplayList && nrmPP != null) {
                  gl.glNewList (nrmPP.getList(), GL2.GL_COMPILE);
               }
               
               boolean useRenderVtxs = mesh.isRenderBuffered() && !mesh.isFixed();

               gl.glBegin (GL2.GL_LINES);
               for (int i=0; i<mesh.getNumVertices(); i++) {
                  Vertex3d vtx = mesh.getVertex(i);
                  Point3d pnt = useRenderVtxs ? vtx.myRenderPnt : vtx.pnt;
                  Vector3d nrm = mesh.getNormal(i);
                  double s = normalLen;
                  gl.glVertex3d (pnt.x, pnt.y, pnt.z);
                  gl.glVertex3d (pnt.x+s*nrm.x, pnt.y+s*nrm.y, pnt.z+s*nrm.z);
               }
               gl.glEnd ();

               if (useDisplayList && nrmPP != null) {
                  gl.glEndList();
                  gl.glCallList (nrmPP.getList());
               }
            } else {
               gl.glCallList (nrmPP.getList());
            }
            GLSupport.checkAndPrintGLError(gl);

         }

         if (reenableLighting) {
            viewer.setLightingEnabled (true);
         }
         gl.glPointSize (savedPointSize[0]);
         gl.glLineWidth (savedLineWidth[0]);
         gl.glShadeModel (savedShadeModel[0]);

         gl.glPopMatrix();

         GLSupport.checkAndPrintGLError(gl);
      }


   }

}
