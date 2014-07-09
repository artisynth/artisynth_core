package artisynth.demos.inverse;

import java.io.IOException;


public class PointModel2d extends PointModel {

   public PointModel2d() throws IOException {
      super();
   }

   public PointModel2d(String name) throws IOException {
      super(name, DemoType.Point2d);
   }

   
   public void addTrackingController() {
      super.addTrackingController();
//      addMonitor(new ComplianceReporter(model, center));
//      addController(new QuasistaticController(model));

      model.setMaxColoredExcitation(0.1);
   }
}
