package artisynth.demos.inverse;

import java.io.IOException;


public class PointModel2d extends PointModel {

   public void build(String[] args) throws IOException {
      build (DemoType.Point2d);
   }
   
   public void addTrackingController() {
      super.addTrackingController();
//      addMonitor(new ComplianceReporter(model, center));
//      addController(new QuasistaticController(model));

      model.setMaxColoredExcitation(0.1);
   }
}
