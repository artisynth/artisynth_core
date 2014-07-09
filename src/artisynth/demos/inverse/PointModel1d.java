package artisynth.demos.inverse;

import java.io.IOException;

import artisynth.core.mechmodels.AxialSpring;
import artisynth.core.mechmodels.Muscle;
import artisynth.core.mechmodels.Particle;
import artisynth.core.materials.*;

public class PointModel1d extends PointModel {

   public PointModel1d() throws IOException {
      super();
   }

   public PointModel1d(String name) throws IOException {
      super(name, DemoType.Point1d);
//      setProperties();
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
	    if (m.getMaterial() instanceof AxialMuscleMaterial) {
	       AxialMuscleMaterial mat = 
	          (AxialMuscleMaterial)m.getMaterial().clone();
	       mat.setPassiveFraction(passiveFraction);
	       mat.setDamping(muscleD);
	       m.setMaterial(mat);
	    }
	 }
      }
      
      for (Particle p : model.particles()) {
	 p.setPointDamping(pointDamping);
      }
   }
   

}
