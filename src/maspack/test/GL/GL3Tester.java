package maspack.test.GL;

public class GL3Tester extends MultiViewerTesterBase {

   @Override
   protected void createViewers (MultiViewer mv) {
      mv.addGL3Viewer("GL3 Viewer", 670, 30, 640, 480);
      mv.syncViews();

      // adjust all windows to a specific size
      mv.setWindowSizes(640, 480);
   }

   public static void main(String[] args) {

      GL3Tester tester = new GL3Tester();
      tester.run();

   }

}
