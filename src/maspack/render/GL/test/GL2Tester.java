package maspack.render.GL.test;

public class GL2Tester extends MultiViewerTesterBase {

   @Override
   protected void createViewers (MultiViewer mv) {
      mv.addGL2Viewer("GL2 Viewer", 670, 30, 640, 480);
      mv.syncViews();

      // adjust all windows to a specific size
      mv.setWindowSizes(640, 480);
   }

   public static void main(String[] args) {

      GL2Tester tester = new GL2Tester();
      tester.run();

   }

}
