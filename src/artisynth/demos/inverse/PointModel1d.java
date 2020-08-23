package artisynth.demos.inverse;

import java.awt.Color;
import java.io.IOException;

import artisynth.core.mechmodels.AxialSpring;
import artisynth.core.mechmodels.Muscle;
import artisynth.core.mechmodels.Particle;
import artisynth.core.materials.*;
import maspack.render.RenderProps;

public class PointModel1d extends PointModel {

   public static boolean omitFromMenu = false;

   public void build(String[] args) throws IOException {
      build (DemoType.Point1d);
      for (AxialSpring s : model.axialSprings()) {
	 if (s instanceof Muscle) {
	    Muscle m = (Muscle)s;
            m.setMaterial (new SimpleAxialMuscle (10, 0, 25));
	 }
      }      
   }

   public void setProperties() {
      double springK = 100.0;
      double passiveFraction = 0.5;
      double muscleD = 0.0;
      double pointDamping = 0.1;
      
      for (AxialSpring s : model.axialSprings()) {
	 if (s instanceof AxialSpring) {
	    if (s.getMaterial() instanceof LinearAxialMaterial) {
               LinearAxialMaterial mat = 
                  (LinearAxialMaterial)s.getMaterial().clone();
               mat.setStiffness(springK);
               s.setMaterial (mat);
	    }
	 }
	 if (s instanceof Muscle) {
	    Muscle m = (Muscle)s;
            System.out.println ("here");
            m.setMaterial (new SimpleAxialMuscle (1000, 0, 100));
	    // if (m.getMaterial() instanceof AxialMuscleMaterial) {
	    //    AxialMuscleMaterial mat = 
	    //       (AxialMuscleMaterial)m.getMaterial().clone();
	    //    mat.setPassiveFraction(passiveFraction);
	    //    mat.setDamping(muscleD);
	    //    m.setMaterial(mat);
	    // }
	 }
      }
      
      for (Particle p : model.particles()) {
	 p.setPointDamping(pointDamping);
      }
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
      model.setMaxColoredExcitation(1.0);
   }
}
