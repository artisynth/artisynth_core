package maspack.test.GL;

import java.awt.Color;
import java.util.LinkedList;

import maspack.matrix.AxisAlignedRotation;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.render.Renderer.FaceStyle;
import maspack.render.Renderer.Shading;
import maspack.test.GL.MultiViewer.SimpleSelectable;
import maspack.test.GL.MultiViewer.SimpleViewerApp;

public class TriangleLightingTest extends GL2vsGL3Tester {
   
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
            props.setFaceColor (Color.WHITE);
            props.setBackColor (Color.WHITE);
            renderer.setFaceColoring (props, /*highlight=*/false);
            renderer.setShading (Shading.SMOOTH);
            renderer.setFaceStyle (FaceStyle.FRONT_AND_BACK);
            renderer.setColor (props.getFaceColorF());
            renderer.setBackColor (props.getBackColorF ());

            renderer.drawTriangle (new float[]{1,1,0}, new float[]{-1,1,0}, new float[]{-1,-1,0});
            renderer.drawTriangle (new float[]{-1,-1,0}, new float[]{1,-1,0}, new float[]{1,1,0});
            
            renderer.setDepthOffset (1);
            renderer.drawAxes (RigidTransform3d.IDENTITY, 1, 1, false);
            renderer.setDepthOffset (0);
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
      
      mv.setAxialView (AxisAlignedRotation.X_Y);
      
   }
   
   public static void main (String[] args) {
      TriangleLightingTest tester = new TriangleLightingTest();
      tester.run ();
   }

}
