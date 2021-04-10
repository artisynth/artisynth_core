package maspack.test.GL;

import java.awt.Color;
import java.awt.event.*;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.JFrame;
import javax.swing.event.MouseInputAdapter;
import java.io.File;

import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.render.GL.GL3.GL3Utilities;
import maspack.render.GL.GL3.GL3Viewer;
import maspack.render.*;
import maspack.render.GL.*;
import maspack.properties.*;
import maspack.widgets.*;
import maspack.matrix.*;
import maspack.render.Renderer.Shading;
import maspack.util.Logger;
import maspack.util.PathFinder;
import maspack.util.Logger.LogLevel;

/**
 * Added March 2021 to help find the red bleed problem that has started to
 * occur under GL3 on certain GPUS.
 */
public class GLRedBleedDebug extends GL3Tester implements ActionListener {

   Renderable myRenderable;
   JFrame myFrame;
   GLViewer myViewer;

     private static File[] debugShaders = {
        new File(PathFinder.getSourceRelativePath(
                    GLRedBleedDebug.class, "vertex_flat.glsl")),
        new File(PathFinder.getSourceRelativePath(
                    GLRedBleedDebug.class, "fragment_flat.glsl"))
     };

   public GLRedBleedDebug() {
      
      MultiViewer.SimpleViewerApp app = rot.getWindows().get(0);
      myFrame = app.frame;
      myViewer = app.viewer;
      myViewer.setShading(Shading.FLAT);
      //myViewer.setBackgroundColor(Color.WHITE);
      myViewer.autoFitOrtho();

      // addjust lights:

      for (int i=0; i<myViewer.numLights(); i++) {
         myViewer.getLight(i).setAmbient (0, 0, 0, 1);
         //myViewer.getLight(i).setDiffuse (1, 0, 0, 1);
      }
      

      ((GL3Viewer)myViewer).setShaderOverride (new Integer(1), debugShaders);

      myViewer.addMouseInputListener (new MouseInputAdapter() {
         public void mousePressed (MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON3) {
               displayPopup (e);
            }
         }
      });
      
      myViewer.getCanvas().addKeyListener(
         new KeyListener() {
         
         private void printStuff(KeyEvent e) {
            //            System.out.print(e.getKeyChar());
            //            System.out.print(" ");
            //            System.out.print(e.getKeyCode());
            //            System.out.print(" ");
            //            System.out.print(e.getExtendedKeyCode());
            //            System.out.print("\n");
         }
         
         @Override
         public void keyTyped(KeyEvent e) {
            printStuff(e);
         }
         
         @Override
         public void keyReleased(KeyEvent e) {
            printStuff(e);
         }
         
         @Override
         public void keyPressed(KeyEvent e) {
            printStuff(e);
            if (e.getKeyCode() == KeyEvent.VK_F5) {
               // re-render
               System.out.println ("rerender");
               rot.rerender();
            }
         }
         });

   }


   class PointRenderable extends RenderableBase {

      public RenderProps createRenderProps() {
         return new RenderProps();
      }

      public void render (Renderer renderer, int flags) {
         float[] origin = new float[3];
         RenderProps props = getRenderProps();
         renderer.setShading (props.getShading());
         renderer.drawPoint (props, origin, false);
      }
   }

   class QuadRenderable extends RenderableBase {

      public RenderProps createRenderProps() {
         return new RenderProps();
      }

      public void render (Renderer renderer, int flags) {
         float[] origin = new float[3];
         RenderProps props = getRenderProps();

         Vector3d p0 = new Vector3d (-1, 0, -1);
         Vector3d p1 = new Vector3d ( 1, 0, -1);
         Vector3d p2 = new Vector3d ( 1, 0,  1);
         Vector3d p3 = new Vector3d (-1, 0,  1);

         renderer.setColor (new Color (0.5f, 0.5f, 0.5f));
         renderer.setSpecular (new float[] {0.f, 0.f, 0.f});
         renderer.setShading (Shading.FLAT);
         renderer.drawTriangle (p0, p1, p2);
         renderer.drawTriangle (p0, p2, p3);
      }
   }

   class GridRenderable extends RenderableBase {

      PolygonalMesh myMesh;

      GridRenderable (int nx, int nz) {
         myMesh = MeshFactory.createRectangle (
            2.0, 2.0, nx, nz, /*texture=*/false);
         myMesh.transform (new RigidTransform3d (0, 0, 0, 0, 0, Math.PI/2));
      }

      public RenderProps createRenderProps() {
         return new RenderProps();
      }

      public void prerender (RenderList list) {
         myMesh.prerender (list);
      }

      public void render (Renderer renderer, int flags) {
         float[] origin = new float[3];
         RenderProps props = getRenderProps();

         //renderer.setColor (new Color (0.5f, 0.5f, 0.5f));
         renderer.setSpecular (new float[] {0.f, 0.f, 0.f});
         renderer.setShading (Shading.FLAT);
         myMesh.render (renderer, flags);
      }
   }

   protected void addContent(MultiViewer mv) {
      //myRenderable = new PointRenderable();
      //myRenderable = new QuadRenderable();
      myRenderable = new GridRenderable(10, 10);
      RenderProps.setSphericalPoints (myRenderable, 1.0, Color.GRAY);
      mv.addRenderable(myRenderable);
   };

   private void displayPopup (MouseEvent evt) {
      JPopupMenu popup = new JPopupMenu();
      JMenuItem item = new JMenuItem ("Edit render props");
      item.addActionListener (this);
      item.setActionCommand ("Edit render props");
      popup.add (item);

      popup.setLightWeightPopupEnabled (false);
      popup.show (evt.getComponent(), evt.getX(), evt.getY());
   }
   
   public static void main(String[] args) {
      
      Logger.getSystemLogger().setLogLevel(LogLevel.ALL);
      GL3Utilities.debug = false;
      
      final GLRedBleedDebug gldebug = new GLRedBleedDebug();
      gldebug.run();
   }

   public void actionPerformed (ActionEvent e) {
      String cmd = e.getActionCommand();
      if (cmd.equals ("Edit render props")) {
         RenderProps props = myRenderable.getRenderProps();
         PropertyDialog dialog =
            new PropertyDialog ("Edit render props", new RenderPropsPanel (
               PropertyUtils.createProperties (props)), "OK Cancel");
         dialog.locateRight (myFrame);
         dialog.addGlobalValueChangeListener (new ValueChangeListener() {
            public void valueChange (ValueChangeEvent e) {
               myViewer.rerender();
            }
         });
         dialog.setVisible (true);
      }
   }

}
