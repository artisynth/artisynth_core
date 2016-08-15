package maspack.test.GL;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.LinkedList;

import javax.swing.JFrame;

import maspack.matrix.AxisAlignedRotation;
import maspack.matrix.AxisAngle;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.render.RenderList;
import maspack.render.Renderer;
import maspack.render.Renderer.FaceStyle;
import maspack.render.Renderer.Shading;
import maspack.test.GL.MultiViewer.SimpleSelectable;
import maspack.render.TextImageStore;
import maspack.render.GL.GLViewer;

public class TextTester extends GL2vsGL3Tester {
   
   @Override
   protected void addContent (MultiViewer mv) {
      
      final HashMap<String,Font> fontMap = new HashMap<> ();
      for (Font font : GraphicsEnvironment.getLocalGraphicsEnvironment ().getAllFonts ()) {
         fontMap.put (font.getName (), font);
      }
      
      mv.addRenderable (new SimpleSelectable() {
         
         JFrame debugframe = null;
         
         @Override
         public void updateBounds (Vector3d pmin, Vector3d pmax) {
            Point3d p1 = new Point3d(-3, -3, -3);
            Point3d p2 = new Point3d(3, 3, 3);
            p1.updateBounds (pmin, pmax);
            p2.updateBounds (pmin, pmax);
         }
         
         @Override
         public void render (Renderer renderer, int flags) {
            
            if (debugframe == null) {
               TextImageStore store = ((GLViewer)renderer).getTextRenderer ().getImageStore (); 
               debugframe = TextImageStore.createDisplayFrame (store);
               debugframe.setVisible (true);
            }
            
            renderer.setShading (Shading.FLAT);
            renderer.setColor (Color.WHITE);
            renderer.setFaceStyle (FaceStyle.FRONT_AND_BACK);
            
            // renderer.drawSphere (Point3d.ZERO, 0.01);
            
            Font font = new Font(Font.SANS_SERIF, 0, 54);
            renderer.drawText (font, "Hello world! And goodnight to all.", new float[]{0.4f,0.3f,0.3f}, 0.25);
            
            renderer.setColor (Color.ORANGE);
            String text = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
            renderer.drawText (font, text, new float[]{-2f,0f,0f}, 0.25);

            Font comic = fontMap.get ("Comic Sans MS");
            if (comic == null) {
               comic = new Font(Font.MONOSPACED, Font.BOLD, 32);
            } else {
               comic = comic.deriveFont (Font.BOLD, 32);
            }
            renderer.setColor (Color.CYAN);
            text = "Cowabunga";
           
            Rectangle2D rect = renderer.getTextBounds (comic, text, 0.25);
            renderer.drawText (comic, text, new float[]{-(float)(rect.getWidth ()),0.5f,0f}, 0.25);
                          
            renderer.setColor (Color.MAGENTA);

            text = "Flip me!";
            font = new Font(Font.SERIF, Font.PLAIN, 64);
            rect = renderer.getTextBounds (font, text, 0.3);

            renderer.pushModelMatrix ();
            RigidTransform3d trans = new RigidTransform3d (new Vector3d(0, -0.4, 0), new AxisAngle(0, 1, 0, Math.toRadians (30)));
            renderer.mulModelMatrix (trans);

            renderer.setFaceStyle (FaceStyle.FRONT);
            renderer.drawText (font, text, Point3d.ZERO, 0.3);

            trans = new RigidTransform3d(Vector3d.ZERO, new AxisAngle(0, 1, 0, Math.PI));
            renderer.mulModelMatrix (trans);

            renderer.drawText (font, text, new Point3d(-rect.getWidth (), 0, 0), 0.3);
            
            trans = new RigidTransform3d(Vector3d.ZERO, new AxisAngle(0.1, 1, 0, Math.PI*1));
            renderer.mulModelMatrix (trans);
            
            renderer.setShading (Shading.NONE);
            renderer.setFaceStyle (FaceStyle.FRONT_AND_BACK);
            renderer.setColor (Color.GREEN);
            text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Aenean sagittis hendrerit consequat. Duis id purus neque. Nulla luctus pretium lorem vel convallis. Praesent non ex eu risus porttitor scelerisque vel et lorem. Maecenas ultricies imperdiet tortor, ac varius enim. Integer sagittis dui lacus. Nulla vitae egestas orci. Phasellus vel metus vitae nibh imperdiet imperdiet ut a urna.";
            drawMultiline(renderer, font, text, new float[] {-1.75f, -0.2f, -0.5f}, 0.1, 2);
            
            renderer.popModelMatrix ();
         }
         
         public void drawMultiline(Renderer renderer,
            Font font, String text, float[] loc, double size, double linelength) {
            
            float left = loc[0];
            float[] nloc = {loc[0], loc[1], loc[2]};
            
            Rectangle2D rect = renderer.getTextBounds (font, text, size);
            float lineheight = (float)(rect.getHeight ());
            
            String[] words = text.split (" ");
            int w = 0;
            while (w < words.length) {
               String word = words[w];
               nloc[0] += renderer.drawText (font, word, nloc, size);
               ++w;
               
               while (w < words.length && nloc[0] < left+linelength) {
                  word = " " + words[w];
                  rect = renderer.getTextBounds (font, word, size);
                  double ll = nloc[0]+rect.getWidth ();
                  if (ll < left+linelength) {
                     renderer.drawText (font, word, nloc, size);
                     ++w;
                  }
                  nloc[0] = (float)ll;
               }
               // newline
               nloc[0] = left;
               nloc[1] -= lineheight;
            }
            
         }
         
         @Override
         public void prerender (RenderList list) {}
         
         @Override
         public int getRenderHints () {
            return 0;
         }
         
         @Override
         public int numSelectionQueriesNeeded () {
            return 0;
         }
         
         @Override
         public boolean isSelectable () {
            return false;
         }
         
         @Override
         public void getSelection (LinkedList<Object> list, int qid) {
         }
         
         @Override
         public void setSelected (boolean set) { }
         
         @Override
         public boolean isSelected () {
            return false;
         }
      });
      
      
      mv.setAxialView (AxisAlignedRotation.X_Y);
   }

   public static void main (String[] args) {
      TextTester tester = new TextTester ();
      tester.run ();
   }
   
}
