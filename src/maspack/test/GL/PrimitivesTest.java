package maspack.test.GL;

import java.awt.Color;
import java.util.LinkedList;

import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.render.Renderer.Shading;
import maspack.test.GL.MultiViewer.SimpleSelectable;

public class PrimitivesTest extends GL2vsGL3Tester {
   
   @Override
   protected void addContent (MultiViewer mv) {
   
      SimpleSelectable renderable = new SimpleSelectable() {
         
         RenderProps props = new RenderProps();
         
         @Override
         public void updateBounds (Vector3d pmin, Vector3d pmax) {
            Point3d.X_UNIT.updateBounds (pmin, pmax);
            Point3d.Y_UNIT.updateBounds (pmin, pmax);
            Point3d.Z_UNIT.updateBounds (pmin, pmax);
            Point3d.NEG_X_UNIT.updateBounds (pmin, pmax);
            Point3d.NEG_Y_UNIT.updateBounds (pmin, pmax);
            Point3d.NEG_Z_UNIT.updateBounds (pmin, pmax);
         }
         
         @Override
         public void render (Renderer renderer, int flags) {
            
            
            props.setFaceColor (Color.CYAN);
            renderer.setFaceColoring (props, false);
            renderer.setColor (props.getFaceColorF());
            
            Shading savedShading = renderer.setShading (Shading.NONE);
            renderer.setPointSize(20f);
            renderer.drawPoint (new float[] {0.1f,0.2f,0.3f});
            
            renderer.setLineWidth (20f);
            renderer.drawLine (new float[]{-0.1f,-0.2f,-0.3f}, new float[]{-0.3f,-0.2f,-0.1f});
            
            renderer.setShading (savedShading);
            renderer.drawTriangle (new float[]{0.1f,0.1f,-0.1f}, new float[]{0.3f,0.1f,-0.1f}, new float[]{0.1f,0.3f,-0.1f});
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
         public void getSelection (LinkedList<Object> list, int qid) {}
         
         @Override
         public void setSelected (boolean set) {}
         
         @Override
         public boolean isSelected () {
            // TODO Auto-generated method stub
            return false;
         }
      };
      
      mv.addRenderable (renderable);
      
   }
   
   public static void main (String[] args) {
      PrimitivesTest tester = new PrimitivesTest();
      tester.run ();
   }

}
