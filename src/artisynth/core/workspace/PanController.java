package artisynth.core.workspace;

import java.io.PrintWriter;
import java.io.IOException;
import java.util.Deque;

import artisynth.core.util.ScanToken;
import artisynth.core.modelbase.*;
import maspack.matrix.*;
import maspack.util.ReaderTokenizer;
import maspack.util.NumberFormat;

/**
 * A controller that rotates the viewpoint of a root model
 * about a specified axis.
 */
public class PanController extends ControllerBase {

   double DTOR = Math.PI/180.0;

   RootModel myRoot;
   Vector3d myViewerEye0 = new Vector3d();
   Point3d myViewerCenter = new Point3d();
   double myPeriod = 1.0;
   Vector3d myAxis = new Vector3d(Vector3d.Z_UNIT);
   double myStart;
   double myStop;
   // eye, center and up need to be initialized
   boolean myViewInitialized = false;

   /**
    * Creates an empty PanController. Used for scan/write.
    */
   public PanController() {
   }

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

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      if (myRoot != null) {
         pw.println (
            "root="+ComponentUtils.getWritePathName (ancestor,myRoot));
      }
      pw.print ("viewerEye0=");
      myViewerEye0.write (pw, fmt, /*withBrackets=*/true);
      pw.println ("");
      pw.print ("viewerCenter=");
      myViewerCenter.write (pw, fmt, /*withBrackets=*/true);
      pw.println ("");
      pw.print ("axis=");
      myAxis.write (pw, fmt, /*withBrackets=*/true);
      pw.println ("");
      pw.println ("period=" + myPeriod);
      pw.println ("start=" + myStart);
      pw.println ("stop=" + myStop);
      pw.println ("viewInitialized=" + myViewInitialized);
      super.writeItems (pw, fmt, ancestor);
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAndStoreReference (rtok, "root", tokens)) {
         return true;
      }
      else if (scanAttributeName (rtok, "viewerEye0")) {
         myViewerEye0.scan (rtok);
         return true;
      }
      else if (scanAttributeName (rtok, "viewerCenter")) {
         myViewerCenter.scan (rtok);
         return true;
      }
      else if (scanAttributeName (rtok, "axis")) {
         myAxis.scan (rtok);
         return true;
      }
      else if (scanAttributeName (rtok, "period")) {
         myPeriod = rtok.scanNumber();
         return true;
      }
      else if (scanAttributeName (rtok, "start")) {
         myStart = rtok.scanNumber();
         return true;
      }
      else if (scanAttributeName (rtok, "stop")) {
         myStop = rtok.scanNumber();
         return true;
      }
      else if (scanAttributeName (rtok, "viewInitialized")) {
         myViewInitialized = rtok.scanBoolean();
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected boolean postscanItem (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
      
      if (postscanAttributeName (tokens, "root")) {
         myRoot = 
            postscanReference (tokens, RootModel.class, ancestor);
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }

}
