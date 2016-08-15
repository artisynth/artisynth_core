package maspack.test.GL;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.render.GL.GL3.GL3Utilities;
import maspack.render.MeshRenderProps;
import maspack.render.Renderer.Shading;
import maspack.util.Logger;
import maspack.util.Logger.LogLevel;

public class GLSparkleDebug extends GL3Tester {
   
   protected void addContent(MultiViewer mv) {
      
      Color colors[] = {Color.RED, Color.GREEN, Color.BLUE, Color.CYAN,
                        Color.GRAY, Color.MAGENTA, Color.ORANGE,
                        Color.PINK, Color.YELLOW,
                        Color.RED.darker().darker(), Color.GREEN.darker().darker(), 
                        Color.BLUE.darker().darker(), Color.CYAN.darker().darker(),
                        Color.GRAY.darker().darker(), Color.MAGENTA.darker().darker(), 
                        Color.ORANGE.darker().darker(), Color.PINK.darker().darker(), 
                        Color.YELLOW.darker().darker(),
                        Color.RED.brighter().brighter(), Color.GREEN.brighter().brighter(), 
                        Color.BLUE.brighter().brighter(), Color.CYAN.brighter().brighter(),
                        Color.GRAY.brighter().brighter(), Color.MAGENTA.brighter().brighter(), 
                        Color.ORANGE.brighter().brighter(), Color.PINK.brighter().brighter(), 
                        Color.YELLOW.brighter().brighter()};
      
      PolygonalMesh mesh = MeshFactory.createIcosahedralSphere (1, 0);
      mesh.setFeatureColoringEnabled ();
      for (int i=0; i<mesh.numColors (); ++i) {
         mesh.setColor (i, colors[i % colors.length]);
      }
      mesh.setRenderProps(new MeshRenderProps());
      mesh.getRenderProps().setShading(Shading.FLAT);
      
      
      mv.addRenderable(mesh);
   };
   
   public static void main(String[] args) {
      
      Logger.getSystemLogger().setLogLevel(LogLevel.ALL);
      GL3Utilities.debug = false;
      
      final GLSparkleDebug sparkle = new GLSparkleDebug();
      sparkle.rot.getWindows().get(0).viewer.setShading(Shading.FLAT);
      
      sparkle.rot.getWindows().get(0).viewer.getCanvas().addKeyListener(new KeyListener() {
         
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
               sparkle.rot.rerender();
            }
         }
      });;
      sparkle.run();
   }

}
