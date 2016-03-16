package artisynth.demos.test;

import java.awt.Color;
import java.util.*;

import maspack.util.*;
import maspack.matrix.*;
import maspack.geometry.*;
import maspack.widgets.*;
import maspack.properties.*;
import maspack.render.*;
import maspack.render.GL.GL2.*;
import maspack.render.Renderer.Shading;
import maspack.render.Renderer.FaceStyle;
import maspack.render.Renderer.DrawMode;
import maspack.render.Renderer.LineStyle;
import maspack.render.Renderer.PointStyle;
import maspack.render.Renderer.ColorInterpolation;
import artisynth.core.mechmodels.*;
import artisynth.core.materials.*;
import artisynth.core.modelbase.*;
import artisynth.core.femmodels.*;
import artisynth.core.workspace.*;
import artisynth.core.gui.*;

public class DrawOpenTet extends RootModel {

   private static class OpenTetRenderable extends RenderableComponentBase {

      public static PropertyList myProps =
         new PropertyList (OpenTetRenderable.class, RenderableComponentBase.class);

      static {
         myProps.add ("renderProps", "render properties", null);
      }

      public PropertyList getAllPropertyInfo() {
         return myProps;
      }

      public OpenTetRenderable() {
         setRenderProps (createRenderProps());
      }

      public void updateBounds (Point3d min, Vector3d max) {
         (new Point3d(0, 0, 0)).updateBounds (min, max);
         (new Point3d(1, 0, 1)).updateBounds (min, max);
      }

      public void render (Renderer renderer, int flags) {
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
   }

   public void build (String[] args) {
      addRenderable (new OpenTetRenderable());
   }
}
