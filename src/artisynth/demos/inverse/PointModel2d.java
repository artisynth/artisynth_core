package artisynth.demos.inverse;

import java.awt.Color;
import java.io.IOException;

import artisynth.core.mechmodels.AxialSpring;
import artisynth.core.mechmodels.Muscle;
import maspack.render.RenderProps;


public class PointModel2d extends PointModel {

   public void build(String[] args) throws IOException {
      build (DemoType.Point2d);

      addWayPoint (0.2);
   }
   
   public void addTrackingController() {
      super.addTrackingController();
//      addMonitor(new ComplianceReporter(model, center));
//      addController(new QuasistaticController(model));

      for (AxialSpring s : model.axialSprings ()) {
         if (s instanceof Muscle) {
            ((Muscle)s).setExcitationColor (Color.RED);
            RenderProps.setLineColor (s, new Color(0, 0, 219));
         }
      }
      model.setMaxColoredExcitation(0.1);
   }
}
