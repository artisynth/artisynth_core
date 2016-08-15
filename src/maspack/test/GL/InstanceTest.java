package maspack.test.GL;

import java.util.LinkedList;

import maspack.matrix.AxisAlignedRotation;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.render.RenderList;
import maspack.render.RenderObject;
import maspack.render.Renderer;
import maspack.render.Renderer.LineStyle;
import maspack.render.Renderer.PointStyle;
import maspack.test.GL.MultiViewer.SimpleSelectable;
import maspack.test.GL.MultiViewer.SimpleViewerApp;

public class InstanceTest extends GL2vsGL3Tester {
   
   @Override
   protected void addContent (MultiViewer mv) {
   
      SimpleSelectable renderable = new SimpleSelectable() {
         
         RenderObject robj = null;
         
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
            
            if (robj == null) {
               robj = new RenderObject ();
               float x = 0.5f;
               robj.addPoint (new float[]{-x,-x,-x});
               robj.addPoint (new float[]{ x,-x,-x});
               robj.addPoint (new float[]{-x, x,-x});
               robj.addPoint (new float[]{ x, x,-x});
               robj.addPoint (new float[]{-x,-x, x});
               robj.addPoint (new float[]{ x,-x, x});
               robj.addPoint (new float[]{-x, x, x});
               robj.addPoint (new float[]{ x, x, x});
               robj.addLine (0, 1);
               robj.addLine (1, 3);
               robj.addLine (3, 2);
               robj.addLine (2, 0);
               robj.addLine (4, 5);
               robj.addLine (5, 7);
               robj.addLine (7, 6);
               robj.addLine (6, 4);
               robj.createLineGroup ();
               robj.addLine (0, 4);
               robj.addLine (4, 5);
               robj.addLine (5, 1);
               robj.addLine (1, 0);
            }
            // renderer.drawPoints (robj, PointStyle.POINT, 10);
            renderer.setFrontColor (new float[] {0.8f,0.8f,0.8f,1.0f});
            
            renderer.drawPoints (robj, PointStyle.SPHERE, 0.1);
            renderer.drawLines (robj, 0, LineStyle.CYLINDER, 0.025);
            renderer.drawLines (robj, 1, LineStyle.SOLID_ARROW, 0.05);
            
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
      for (SimpleViewerApp app : mv.getWindows ()) {
         app.viewer.setAxialView (AxisAlignedRotation.X_Y);
      }
      
   }
   
   public static void main (String[] args) {
      InstanceTest tester = new InstanceTest();
      tester.run ();
   }

}
