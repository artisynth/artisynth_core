package artisynth.demos.tutorial;

import java.io.IOException;
import maspack.matrix.*;

import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.*;
import artisynth.core.gui.*;

public class SimpleMuscleWithController extends SimpleMuscleWithPanel
{
   private class PointMover extends ControllerBase {

      Point myPnt;      // point to be moved
      Point3d myPos0;   // initial point position

      public PointMover (Point pnt) {
         myPnt = pnt;
         myPos0 = new Point3d (pnt.getPosition());
      }

      public void apply (double t0, double t1) {
         double ang = Math.PI*t1/2;            // angle associated with time t1
         Point3d pos = new Point3d (myPos0);   
         pos.x += 0.5*Math.sin (ang);          // compute position for t1 ...
         pos.z += 0.5*(1-Math.cos (ang));
         myPnt.setTargetPosition (pos);        // ... and the set point's target
      }
   }

   public void build (String[] args) throws IOException {
      super.build (args);

      addController (new PointMover (p1));
      // increase model bounding box for the viewer
      mech.setBounds (-1, 0, -1, 1, 0, 1);
   }

}
