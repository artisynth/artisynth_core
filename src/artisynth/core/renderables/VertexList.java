/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.renderables;

import java.util.Iterator;
import java.util.LinkedList;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import artisynth.core.modelbase.RenderableComponentList;
import artisynth.core.util.ScalableUnits;
import maspack.matrix.Point3d;
import maspack.properties.PropertyList;
import maspack.render.PointRenderProps;
import maspack.render.RenderKeyImpl;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.RenderableUtils;
import maspack.render.Renderer;
import maspack.render.Renderer.PointStyle;
import maspack.render.Renderer.Shading;
import maspack.render.GL.GL2.GL2Object;
import maspack.render.GL.GL2.GL2Primitive.PrimitiveType;
import maspack.render.GL.GL2.GL2VersionedObject;
import maspack.render.GL.GL2.GL2Viewer;
import maspack.util.BooleanHolder;

public class VertexList<P extends VertexComponent> extends RenderableComponentList<P>
implements ScalableUnits {

   protected static final long serialVersionUID = 1;

   RenderKeyImpl vKey;
   int version;
   boolean vchanged;

   private static class VListPrint {
      int version;
      PointStyle style;
      int pList;

      public VListPrint(PointStyle style, int pointDisplayList, int version) {
         this.version = version;
         this.style = style;
         this.pList = pointDisplayList;
      }

      @Override
      public int hashCode() {
         return version*31 + style.hashCode()*17 + pList;
      }

      @Override
      public boolean equals(Object obj) {
         if (this == obj) {
            return true;
         }
         if (obj == null || getClass() != obj.getClass()) {
            return false;
         }
         VListPrint other = (VListPrint)obj;
         if (version != other.version || style != other.style || pList != other.pList) {
            return false;
         }
         return true;
      }

   }

   public static PropertyList myProps =
   new PropertyList (VertexList.class, RenderableComponentList.class);

   static {
      myProps.get ("renderProps").setDefaultValue (new PointRenderProps());
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public VertexList (Class<P> type) {
      this (type, null, null);
   }

   public VertexList (Class<P> type, String name, String shortName) {
      super (type, name, shortName);
      setRenderProps (createRenderProps());
      vchanged = true;
      version = 0;
      vKey = new RenderKeyImpl ();
   }

   @Override
   protected void notifyStructureChanged(Object comp, boolean stateIsChanged) {
      super.notifyStructureChanged(comp, stateIsChanged);
      vchanged = true;
   }

   private VListPrint getFingerPrint(PointStyle style, int pointDisplayList) {
      if (vchanged) {
         version++;
         vchanged = false;
      }
      return new VListPrint(style, pointDisplayList, version);
   }

   /* ======== Renderable implementation ======= */

   public RenderProps createRenderProps() {
      return RenderProps.createPointProps (this);
   }

   public void prerender (RenderList list) {
      for (int i = 0; i < size(); i++) {
         VertexComponent p = get (i);
         if (p.getRenderProps() != null) {
            list.addIfVisible (p);
         }
         else {
            p.prerender (list);
         }
      }
      version++;
   }

   public boolean rendersSubComponents() {
      return true;
   }

   public void render (Renderer renderer, int flags) {

      if (!(renderer instanceof GL2Viewer)) {
         return;
      }
      GL2Viewer viewer = (GL2Viewer)renderer;
      GL2 gl = viewer.getGL2();

      gl.glPushMatrix();

      RenderProps props = getRenderProps();

      boolean lastSelected = false;

      Shading savedShading = renderer.setPointShading (props);
      renderer.setPointColoring (props, isSelected());
      switch (props.getPointStyle()) {
         case POINT: {

            //renderer.setLightingEnabled (false);
            renderer.setPointSize (props.getPointSize());

            if (renderer.isSelecting()) {
               // don't worry about color in selection mode
               int i = 0;
               for (VertexComponent vc : this) {
                  if (vc.getRenderProps() == null) {
                     renderer.beginSelectionQuery (i);
                     gl.glBegin (GL2.GL_POINTS);
                     gl.glVertex3fv (vc.getRenderCoords(), 0);
                     gl.glEnd();
                     renderer.endSelectionQuery ();
                  }
                  i++;
               }
            } else {

               VListPrint vPrint = getFingerPrint(PointStyle.POINT, 0);
               BooleanHolder compile = new BooleanHolder(true);
               GL2VersionedObject gvo = viewer.getVersionedObject(gl, vKey, vPrint, compile);
               boolean useDisplayList = !renderer.isSelecting ();

               if (compile.value || useDisplayList) {
                  if (useDisplayList && compile.value) {
                     gvo.beginCompile (gl);      
                  }

                  gl.glBegin (GL2.GL_POINTS);
                  for (VertexComponent vc : this) {
                     if (vc.getRenderProps() == null) {

                        if (!isSelected()) {
                           // set selection color for vertices as needed
                           if (vc.isSelected() != lastSelected) {
                              renderer.setPointColoring (
                                 props, vc.isSelected());
                              lastSelected = vc.isSelected();
                           }
                        }

                        gl.glVertex3fv (vc.getRenderCoords(), 0);
                     }
                  }
                  gl.glEnd();

                  if (useDisplayList) {
                     gvo.endCompile (gl);
                     gvo.draw (gl);
                  }
               } else {
                  gvo.draw (gl);
               }
            }

            renderer.setPointSize(1);
            //renderer.setLightingEnabled(true);
            break;
         }
         case CUBE: {
            BooleanHolder compile = new BooleanHolder(true);
            boolean useDisplayList = !renderer.isSelecting();
            GL2VersionedObject gvo = null;

            if (useDisplayList) {
               GL2Object sphere = viewer.getPrimitive (gl, PrimitiveType.SPHERE); 
               VListPrint vPrint = getFingerPrint(PointStyle.POINT, sphere.hashCode());
               gvo = viewer.getVersionedObject(gl, vKey, vPrint, compile);
            }

            if (!useDisplayList || compile.value) {
               if (useDisplayList) {
                  gvo.beginCompile (gl);      
               }

               int i=0;
               double width = 2*props.getPointRadius();
               for (VertexComponent vc : this) {
                  if (vc.getRenderProps() == null) {
                     if (renderer.isSelecting()) {
                        renderer.beginSelectionQuery (i);
                        renderer.drawCube (vc.getRenderCoords(), width);
                        renderer.endSelectionQuery ();      
                     }  else {
                        if (!isSelected()) {
                           // set selection color for individual vertices as needed
                           if (vc.isSelected() != lastSelected) {
                              renderer.setPointColoring (
                                 props, vc.isSelected());
                              lastSelected = vc.isSelected();
                           }
                        }
                        renderer.drawCube (vc.getRenderCoords(), width);
                     }
                  }
                  i++;
               }

               if (useDisplayList) {
                  gvo.endCompile (gl);
                  gvo.draw (gl);
               }
            } else {
               gvo.draw (gl);
               int err = gl.glGetError();
               if (err != GL.GL_NO_ERROR) {
                  System.err.println("GL Error: " + err);
               }
            }
            break;
         }
         case SPHERE: {
            BooleanHolder compile = new BooleanHolder(true);
            boolean useDisplayList = !renderer.isSelecting();
            GL2VersionedObject gvo = null;

            if (useDisplayList) {
               GL2Object sphere = viewer.getPrimitive (gl, PrimitiveType.SPHERE); 
               VListPrint vPrint = getFingerPrint(PointStyle.POINT, sphere.hashCode());
               gvo = viewer.getVersionedObject(gl, vKey, vPrint, compile);
            }

            if (!useDisplayList || compile.value) {
               if (useDisplayList) {
                  gvo.beginCompile (gl);      
               }

               int i=0;
               double rad = props.getPointRadius();
               for (VertexComponent vc : this) {
                  if (vc.getRenderProps() == null) {
                     if (renderer.isSelecting()) {
                        renderer.beginSelectionQuery (i);
                        renderer.drawSphere (vc.getRenderCoords(), rad);
                        renderer.endSelectionQuery ();      
                     }  else {
                        if (!isSelected()) {
                           // set selection color for individual vertices as needed
                           if (vc.isSelected() != lastSelected) {
                              renderer.setPointColoring (
                                 props, vc.isSelected());
                              lastSelected = vc.isSelected();
                           }
                        }
                        renderer.drawSphere (vc.getRenderCoords(), rad);
                     }
                  }
                  i++;
               }

               if (useDisplayList) {
                  gvo.endCompile (gl);
                  gvo.draw (gl);
               }
            } else {
               gvo.draw (gl);
               int err = gl.glGetError();
               if (err != GL.GL_NO_ERROR) {
                  System.err.println("GL Error: " + err);
               }
            }
            break;
         }
      }
      renderer.setShading (savedShading);
      gl.glPopMatrix();

   }

   public void drawPoints (Renderer renderer,
      RenderProps props, Iterator<? extends VertexComponent> iterator) {

      if (!(renderer instanceof GL2Viewer)) {
         return;
      }
      GL2Viewer viewer = (GL2Viewer)renderer;
      GL2 gl = viewer.getGL2();

      gl.glPushMatrix();

      Shading savedShading = renderer.setPointShading(props);
      renderer.setPointColoring (props, /*selected=*/false);
      switch (props.getPointStyle()) {
         case POINT: {
            // draw regular points first
            renderer.setPointSize (props.getPointSize());
            if (renderer.isSelecting()) {
               // don't worry about color in selection mode
               int i = 0;
               while (iterator.hasNext()) {
                  VertexComponent pnt = iterator.next();
                  if (pnt.getRenderProps() == null) {
                     if (renderer.isSelectable (pnt)) {
                        renderer.beginSelectionQuery (i);
                        gl.glBegin (GL2.GL_POINTS);
                        gl.glVertex3fv (pnt.getRenderCoords(), 0);
                        gl.glEnd();
                        renderer.endSelectionQuery ();
                     }
                  }
                  i++;
               }
            }
            else {
               gl.glBegin (GL2.GL_POINTS);
               while (iterator.hasNext()) {
                  VertexComponent pnt = iterator.next();
                  if (pnt.getRenderProps() == null) {
                     renderer.setPointColoring (props, pnt.isSelected());
                     gl.glVertex3fv (pnt.getRenderCoords(), 0);
                  }
               }
               gl.glEnd();
            }
            renderer.setPointSize (1);
            break;
         }
         case CUBE: {
            int i = 0;
            double width = 2*props.getPointRadius();
            while (iterator.hasNext()) {
               VertexComponent pnt = iterator.next();
               if (pnt.getRenderProps() == null) {
                  if (renderer.isSelecting()) {
                     if (renderer.isSelectable (pnt)) {
                        renderer.beginSelectionQuery (i);
                        renderer.drawCube (pnt.getRenderCoords(), width);
                        renderer.endSelectionQuery ();      
                     }
                  }
                  else {
                     renderer.setPointColoring (props, pnt.isSelected());
                     renderer.drawCube (pnt.getRenderCoords(), width);
                  }
               }
               i++;
            }
            break;
         }
         case SPHERE: {
            int i = 0;
            double rad = props.getPointRadius();
            while (iterator.hasNext()) {
               VertexComponent pnt = iterator.next();
               if (pnt.getRenderProps() == null) {
                  if (renderer.isSelecting()) {
                     if (renderer.isSelectable (pnt)) {
                        renderer.beginSelectionQuery (i);
                        renderer.drawSphere (pnt.getRenderCoords(), rad);
                        renderer.endSelectionQuery ();      
                     }
                  }
                  else {
                     renderer.setPointColoring (props, pnt.isSelected());
                     renderer.drawSphere (pnt.getRenderCoords(), rad);
                  }
               }
               i++;
            }
            break;
         }
      }
      renderer.setShading (savedShading);

      gl.glPopMatrix();
   }

   /**
    * {@inheritDoc}
    */
   public boolean isSelectable() {
      return true;
   }

   public int numSelectionQueriesNeeded() {
      return size();
   }

   public void getSelection (LinkedList<Object> list, int qid) {
      if (qid >= 0 && qid < size()) {
         list.addLast (get (qid));
      }
   }

   //   public void transformGeometry (AffineTransform3dBase X) {
   //      transformGeometry (X, this, 0);
   //   }
   //
   //   public void transformGeometry (
   //      AffineTransform3dBase X, TransformableGeometry topObject, int flags) {
   //      for (int i = 0; i < size(); i++) {
   //         get (i).transformGeometry (X, topObject, flags);
   //      }
   //   }
   //
   //   public void transformGeometry (
   //      AffineTransform3dBase X, PolarDecomposition3d pd,
   //      Map<TransformableGeometry,Boolean> transformSet, int flags) {
   //      for (int i = 0; i < size(); i++) {
   //         get (i).transformGeometry (X, pd, transformSet, flags);
   //      }
   //   }
   //   
   //   public int getTransformableDescendants (
   //      List<TransformableGeometry> list) {
   //      return 0;
   //   }

   public void scaleDistance (double s) {
      for (int i = 0; i < size(); i++) {
         get (i).scaleDistance (s);
      }
      if (myRenderProps != null) {
         RenderableUtils.cloneRenderProps (this);
         myRenderProps.scaleDistance (s);
      }
   }

   public void scaleMass (double s) {
      // nothing
   }

   @Override
   public void updateBounds(Point3d pmin, Point3d pmax) {
      for (VertexComponent c : this) {
         c.updateBounds(pmin, pmax);
      }
   }

}
