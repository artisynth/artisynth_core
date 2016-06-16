package artisynth.demos.test;

import java.awt.Color;
import java.util.*;

import maspack.util.*;
import maspack.matrix.*;
import maspack.geometry.*;
import maspack.widgets.*;
import maspack.properties.*;
import maspack.render.*;
import maspack.render.Renderer.Shading;
import maspack.render.Renderer.FaceStyle;
import maspack.render.Renderer.DrawMode;
import maspack.render.Renderer.PointStyle;
import maspack.render.Renderer.PointStyle;
import maspack.render.Renderer.ColorInterpolation;
import artisynth.core.mechmodels.*;
import artisynth.core.materials.*;
import artisynth.core.modelbase.*;
import artisynth.core.femmodels.*;
import artisynth.core.workspace.*;
import artisynth.core.gui.*;

public class DrawModeTest extends RootModel {

   private static class MyRenderable extends RenderableComponentBase {

      public static PropertyList myProps =
         new PropertyList (MyRenderable.class, RenderableComponentBase.class);

      static {
         myProps.add ("renderProps", "render properties", null);
      }

      public PropertyList getAllPropertyInfo() {
         return myProps;
      }

      public MyRenderable() {
         setRenderProps (createRenderProps());
      }

      public void updateBounds (Point3d min, Vector3d max) {
         (new Point3d(0, 0, 0)).updateBounds (min, max);
         (new Point3d(1, 0, 1)).updateBounds (min, max);
      }

      public void drawSquare (Renderer renderer) {
         // the corners of the square
         Vector3d p0 = new Vector3d (0, 0, 0);
         Vector3d p1 = new Vector3d (1, 0, 0);
         Vector3d p2 = new Vector3d (1, 0, 1);
         Vector3d p3 = new Vector3d (0, 0, 1);


         renderer.setPointSize (6);
         renderer.setColor (Color.WHITE);
         renderer.setShading (Shading.NONE);

         renderer.beginDraw (DrawMode.POINTS);
         renderer.addVertex (p0); 
         renderer.addVertex (p1); 
         renderer.addVertex (p2); 
         renderer.addVertex (p3); 
         renderer.endDraw();

         renderer.setLineWidth (3);
         renderer.setColor (Color.RED);

         renderer.beginDraw (DrawMode.LINES);
         renderer.addVertex (p0); 
         renderer.addVertex (p1); 
         renderer.addVertex (p1); 
         renderer.addVertex (p2); 
         renderer.addVertex (p2); 
         renderer.addVertex (p3); 
         renderer.addVertex (p3); 
         renderer.addVertex (p0); 
         renderer.endDraw();

         renderer.setShading (Shading.FLAT);
      }

      public void drawOpenTetColored (Renderer renderer) {
         // the corners of the tetrahedron
         Vector3d p0 = new Vector3d (0, 0, 0);
         Vector3d p1 = new Vector3d (1, 0, 0);
         Vector3d p2 = new Vector3d (0, 1, 0);
         Vector3d p3 = new Vector3d (0, 0, 1);

         renderer.setColorInterpolation (ColorInterpolation.HSV);
         
         // render both sides of each triangle:
         renderer.setFaceStyle (FaceStyle.FRONT_AND_BACK); 

         renderer.beginDraw (DrawMode.TRIANGLES);
         // first triangle
         renderer.setNormal (0, 0, -1);
         renderer.setColor (Color.RED);
         renderer.addVertex (p0); 
         renderer.setColor (Color.BLUE);
         renderer.addVertex (p2);
         renderer.setColor (Color.GREEN);
         renderer.addVertex (p1);
         // second triangle
         renderer.setNormal (-1, 0, 0);
         renderer.setColor (Color.RED);
         renderer.addVertex (p0); 
         renderer.setColor (Color.CYAN);
         renderer.addVertex (p3);
         renderer.setColor (Color.BLUE);
         renderer.addVertex (p2);
         // third triangle
         renderer.setNormal (0, -1, 0);
         renderer.setColor (Color.RED);
         renderer.addVertex (p0);
         renderer.setColor (Color.GREEN);
         renderer.addVertex (p1);
         renderer.setColor (Color.CYAN);
         renderer.addVertex (p3);
         renderer.endDraw();

         renderer.setFaceStyle (FaceStyle.FRONT); // restore default
      }

      public void drawOpenTet (Renderer renderer) {
         // the corners of the tetrahedron
         Vector3d p0 = new Vector3d (0, 0, 0);
         Vector3d p1 = new Vector3d (1, 0, 0);
         Vector3d p2 = new Vector3d (0, 1, 0);
         Vector3d p3 = new Vector3d (0, 0, 1);

         //renderer.setColorInterpolation (ColorInterpolation.HSV);
         
         // render both sides of each triangle:
         renderer.setFaceStyle (FaceStyle.FRONT_AND_BACK); 

         renderer.beginDraw (DrawMode.TRIANGLES);
         // first triangle
         renderer.setNormal (0, 0, -1);
         renderer.addVertex (p0); 
         renderer.addVertex (p2);
         renderer.addVertex (p1);
         // second triangle
         renderer.setNormal (-1, 0, 0);
         renderer.addVertex (p0); 
         renderer.addVertex (p3);
         renderer.addVertex (p2);
         // third triangle
         renderer.setNormal (0, -1, 0);
         renderer.addVertex (p0);
         renderer.addVertex (p1);
         renderer.addVertex (p3);
         renderer.endDraw();

         renderer.setFaceStyle (FaceStyle.FRONT); // restore default
      }

      public void render (Renderer renderer, int flags) {
         drawSquare (renderer);
         //drawOpenTet (renderer);
         //drawOpenTetColored (renderer);
      }
   }

   public void build (String[] args) {
      addRenderable (new MyRenderable());
   }
}
