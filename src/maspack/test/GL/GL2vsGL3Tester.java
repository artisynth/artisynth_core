package maspack.test.GL;

import maspack.geometry.PolygonalMesh;

public class GL2vsGL3Tester extends MultiViewerTesterBase {

   @Override
   protected void createViewers (MultiViewer mv) {
      mv.addGL2Viewer("GL2 Viewer", 30, 30, 640, 480);
      mv.addGL3Viewer("GL3 Viewer", 670, 30, 640, 480);
      mv.syncViews();

      // adjust all windows to a specific size
      mv.setWindowSizes(640, 480);
   }

   @Override
   protected void addContent (MultiViewer mv) {
      addCube(mv);
      addAxes(mv);
      addTransRotator(mv);
      addCylinder(mv);

      PolygonalMesh bunny = loadStanfordBunny();
      addStanfordBunnies(mv, bunny);
      addSolidBunny(mv, bunny);
      addHalfBunny(mv, bunny);
      
      mv.autoFitViewers ();
   }
   
   public static void main(String[] args) {

      GL2vsGL3Tester tester = new GL2vsGL3Tester();
      tester.run();

   }

}
