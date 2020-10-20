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
   Vector3d myAxis = new Vector3d(Vector3d.Z_UNIT);
   double myStart;
   double myStop;
   // eye, center and up need to be initialized
   boolean myViewInitialized = false;

   /**
    * Create a new PanController.
    *
    * @param root RootModel that is being viewed
    * @param viewerEye0 initial eye position
    * @param viewerCenter center about which the camera should move
    * @param period number of seconds required for a complete revolution
    * @param axis axis about which the camera should spin (usually
    * the ``up'' direction, such as Vector3d.Z_UNIT).
    * @param startTime time at the which the controller should start
    * @param stopTime time at the which the controller should stop
    */
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
      myViewInitialized = true;
   }

   /**
    * Create a new PanController. The initial eye position, center and up
    * direction will be initialized from the root model when the controller
    * first starts.
    *
    * @param root RootModel that is being viewed
    * @param period number of seconds required for a complete revolution
    * @param startTime time at the which the controller should start
    * @param stopTime time at the which the controller should stop
    */
   public PanController (
      RootModel root, double period, double startTime, double stopTime) {
      myRoot = root;
      myPeriod = period;
      myStart = startTime;
      myStop = stopTime;
      myViewInitialized = false;
   }

   public void apply (double t0, double t1) {
      if (t1 >= myStart && t1 <= myStop) {
         if (!myViewInitialized) {
            myViewerEye0.set (myRoot.getViewerEye());
            myViewerCenter.set (myRoot.getViewerCenter());
            myAxis.set (myRoot.getViewerUp());
            myViewInitialized = true;
         }
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
