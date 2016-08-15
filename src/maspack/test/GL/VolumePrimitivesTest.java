package maspack.test.GL;

import java.awt.Color;
import java.util.LinkedList;

import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.render.Renderer.FaceStyle;
import maspack.test.GL.MultiViewer.SimpleSelectable;

public class VolumePrimitivesTest extends GL2vsGL3Tester {
   
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
            props.setBackColor (Color.MAGENTA);
            renderer.setFaceColoring (props, /*highlight=*/false);
            renderer.setColor (props.getFaceColorF());
            renderer.setBackColor (props.getBackColorF());
            renderer.setFaceStyle (FaceStyle.FRONT_AND_BACK);
            
            //            renderer.drawSphere (new float[] {0.1f,0.2f,0.3f}, 0.05);
            //            renderer.drawSpindle (new float[]{-0.1f,-0.2f,-0.3f}, new float[]{-0.3f,-0.2f,-0.1f}, 0.05);
            //            renderer.drawSpindle (new float[]{0.1f,-0.2f,0.3f}, new float[]{0.3f,-0.2f,0.1f}, 0.25);
            //            renderer.drawCylinder (new float[]{-0.1f,0.2f,-0.3f}, new float[]{-0.3f,0.2f,-0.1f}, 0.3, true);
            //            renderer.drawCylinder (new float[]{0.1f,0.2f,-0.3f}, new float[]{-0.3f,-0.2f,-0.3f}, 0.2, true);
            //            renderer.drawCone (new float[]{-0.1f,0.2f,0.3f}, new float[]{0.3f,0.2f,-0.1f}, 0.1, 0, false);
            //            renderer.drawCone (new float[]{-0.3f,0.2f,0.3f}, new float[]{0.5f,0.2f,-0.1f}, 0, 0.2, true);
            //            renderer.drawSphere (new float[]{-0.1f,-0.2f,-0.3f}, 0.2);
            //            renderer.drawSphere (new float[] {0.1f,0.2f,0.3f}, 0.02);
            //            renderer.drawSphere (new float[] {0.1f,0.2f,0.6f}, 0.03);
            //            renderer.drawSphere (new float[] {0.1f,0.2f,0.8f}, 0.04);
            //            renderer.drawSphere (new float[] {0.1f,0.2f,1f}, 0.05);
            //            renderer.drawCone (new float[]{0.1f,0.1f,-0.1f}, new float[]{0.3f,0.1f,-0.1f}, 0.05, 0.02, true);
            renderer.drawCube (new float[]{-0.2f, -0.2f, -0.2f}, 0.5);
            
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
      VolumePrimitivesTest tester = new VolumePrimitivesTest();
      tester.run ();
   }

}
