package artisynth.core.workspace;

import artisynth.core.modelbase.*;
import maspack.matrix.*;

/**
 * A controller that rotates the viewpoint of a root model
 * about a specified axis.
 */
public class PanController extends ControllerBase {

   RootModel myRoot;
   double DTOR = Math.PI/180.0;
   Vector3d myViewerEye0 = new Vector3d();
   Point3d myViewerCenter = new Point3d();
   double myPeriod = 1.0;
   Vector3d myAxis = Vector3d.Z_UNIT;
   double myStart;
   double myStop;

   public PanController (
      RootModel root, Vector3d viewerEye0, Vector3d viewerCenter, double period,
      Vector3d axis, double startTime, double stopTime) {
      myRoot = root;
      myViewerEye0 = new Vector3d(viewerEye0);
      myViewerCenter = new Point3d(viewerCenter);
      myAxis = new Vector3d(axis);
      myPeriod = period;
      myStart = startTime;
      myStop = stopTime;
   }

   public void apply (double t0, double t1) {
      if (t1 >= myStart && t1 <= myStop) {
         double ang = 2*Math.PI*(t1-myStart)/myPeriod;
         RotationMatrix3d R = new RotationMatrix3d();
         R.setAxisAngle (myAxis, ang);
         Point3d eye = new Point3d(myViewerEye0);
         eye.add (myViewerCenter);
         eye.transform (R);
         eye.sub (myViewerCenter);
         myRoot.setViewerCenter (myViewerCenter);
         myRoot.setViewerEye (eye);
      }
   }
}
