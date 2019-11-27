package artisynth.demos.wrapping;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenuItem;

import artisynth.core.gui.ControlPanel;
import artisynth.core.mechmodels.DistanceGridComp;
import artisynth.core.mechmodels.MultiPointSpring.WrapSegment;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.RigidCylinder;
import artisynth.core.mechmodels.RigidEllipsoid;
import artisynth.core.mechmodels.RigidSphere;
import artisynth.core.mechmodels.RigidTorus;
import artisynth.core.mechmodels.Wrappable;
import artisynth.core.modelbase.StepAdjustment;
import artisynth.core.probes.NumericProbeBase;
import artisynth.core.util.ArtisynthIO;
import artisynth.core.workspace.DriverInterface;
import maspack.geometry.DistanceGrid;
import maspack.geometry.DistanceGrid.*;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.interpolation.NumericList;
import maspack.interpolation.NumericListKnot;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.matrix.Vector3i;
import maspack.properties.PropertyList;
import maspack.render.RenderProps;
import maspack.spatialmotion.SpatialInertia;
import maspack.spatialmotion.Twist;
import maspack.spatialmotion.Wrench;
import maspack.util.PathFinder;
import maspack.util.ReaderTokenizer;
import maspack.widgets.GuiUtils;

/**
 * Model that tests MultiPointSpring wrapping in a static setting,
 * with a single strand moving around a stationary object.
 * 
 * @author John E. Lloyd
 */
public class GeneralWrapTest extends OneStrandWrapBase {

   // use high mesh resolution when using grid on analytic, to get a better grid
   static public boolean highMeshRes = false;
   static public boolean highGridRes = false;

   public enum GeometryType {
      CYLINDER,
      SPHERE,
      ELLIPSOID,
      DUMBBELL,
      TORUS,
      TALUS,
      PHALANX,
      HIP,
      HUMERUS,
      FEMUR;

      boolean isAnalytic() {
         switch (this) {
            case CYLINDER:
            case SPHERE:
            case ELLIPSOID:
            case TORUS: {
               return true;
            }
            default: {
               return false;
            }
         }
      }
   };

   static final double DEFAULT_LAMBDA = 0.33;
   static final double DEFAULT_MU = -0.2;
   static final int DEFAULT_ITERS = 10;

   static final double DTOR = Math.PI/180.0;

   double myLambda = DEFAULT_LAMBDA;
   double myMu = DEFAULT_MU;
   int myIters = DEFAULT_ITERS;
   
   GeometryType myGeometryType = GeometryType.CYLINDER;
   //protected ForceMonitorProbe myForceProbe = null;
   protected ForceProbe myForceProbe = null;
   protected MomentProbe myMomentProbe = null;
   boolean myUseMesh = false;
   RigidBody myBody;

   public static PropertyList myProps =
      new PropertyList (GeneralWrapTest.class, OneStrandWrapBase.class);

   static {
      myProps.add (
         "smoothIters", "number of smoothing iterations", DEFAULT_ITERS);
      myProps.add (
         "smoothLambda", "lambda parameter for smoothing", DEFAULT_LAMBDA);
      myProps.add (
         "smoothMu", "mu parameter for smoothing", DEFAULT_MU);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public int getSmoothIters() {
      return myIters;
   }

   public void setSmoothIters (int iters) {
      myIters = iters;
   }

   public double getSmoothLambda() {
      return myLambda;
   }

   public void setSmoothLambda (double lambda) {
      myLambda = lambda;
   }

   public double getSmoothMu() {
      return myMu;
   }

   public void setSmoothMu (double mu) {
      myMu = mu;
   }

   protected int matchArg (String[] args, int i) {
      int nm = super.matchArg(args, i);
      if (nm == 0) {
         if (args[i].equals ("-geo")) {
            nm++;
            if (i == args.length-1) {
               System.out.println (
                  "WARNING: -geo needs another argument");
            }
            else {
               myGeometryType = GeometryType.valueOf (args[++i]);
               nm++;
            }
         }
         else if (args[i].equals ("-mesh")) {
            nm++;
            myUseMesh = true;
         }
      }
      return nm;
   }

   public static RigidBody createGeometry (
      GeometryType gtype, double size, double density, boolean useMesh) {
      
      RigidBody body = null;
      switch (gtype) {
         case CYLINDER: {
            if (useMesh) {
               int res = highMeshRes ? 400 : 48;
               PolygonalMesh mesh =
                  MeshFactory.createCylinder (size/2, size*5, res);
               body = new RigidBody ("cylinder", mesh, null, density, 1.0);
            }
            else {
               body = new RigidCylinder (
                  "cylinder", size/2, size*5, density, 64);
            }
            if (highGridRes) {
               body.setDistanceGridRes (new Vector3i (20, 20, 20));
            }
            else {
               body.setDistanceGridRes (new Vector3i (10, 10, 10));
            }
            body.setPose (new RigidTransform3d (
                             0, 0, 1.5*size, 0, 0, Math.PI/2));
            break;
         }
         case SPHERE: {
            if (useMesh) {
               int res = highMeshRes ? 400 : 48;
               PolygonalMesh mesh =
                  MeshFactory.createSphere (2*size, res);
               body = new RigidBody ("sphere", mesh, null, density, 1.0);
            }
            else {
               body = new RigidSphere (
                  "sphere", 2*size, density, 100);
            }
            if (highGridRes) {
               body.setDistanceGridRes (new Vector3i (20, 20, 20));
            }
            else {
               body.setDistanceGridRes (new Vector3i (10, 10, 10));
            }
            body.setPose (new RigidTransform3d (
                                  0, 0, 3.0*size, 0, 0, 0));
            break;
         }
         case ELLIPSOID: {
            if (useMesh) {
               int res = highMeshRes ? 400 : 48;
               PolygonalMesh mesh =
                  MeshFactory.createSphere (size, res);
               mesh.scale (1.0, 4.0, 2/3.0);
               body = new RigidBody ("ellipsoid", mesh, null, density, 1.0);
            }
            else {
               body = new RigidEllipsoid (
                  "ellipsoid", size, 4*size, 2/3.0*size, density, 100);
            }
            if (highGridRes) {
               body.setDistanceGridRes (new Vector3i (20, 20, 20));
            }
            else {
               body.setDistanceGridRes (new Vector3i (12, 12, 12));
            }
            body.setPose (new RigidTransform3d (
                             0, 0, 1.5*size, 0, 0, 0));
            break;
         }
         case TORUS: {
            if (useMesh) {
               int res = highMeshRes ? 400 : 100;
               PolygonalMesh mesh =
                  MeshFactory.createTorus (1.5*size, size/2, res, res/2);
               body = new RigidBody ("torus", mesh, null, density, 1.0);
            }
            else {
               body = new RigidTorus (
                  "torus", 1.5*size, size/2, density, 100, 50);
            }
            if (highGridRes) {
               body.setDistanceGridRes (new Vector3i (60, 60, 20));
            }
            else {
               body.setDistanceGridRes (new Vector3i (30, 30, 10));
            }
            body.setPose (new RigidTransform3d (
                              0, 0, 0, 0, DTOR*90.0, 0));
            //grid.smooth (0.33, -0.2, 30);
            break;
         }
         case DUMBBELL: {
            PolygonalMesh mesh = createDumbbell (size);
            body = new RigidBody ("dumbbell", mesh, null, density, 1.0);
            body.setDistanceGridRes (new Vector3i (30, 30, 10));
            body.setPose (new RigidTransform3d (
                             0, 0, 1.5, DTOR*90.0, 0, 0));
            useMesh = true;
            break;
         }
         case TALUS: {
            PolygonalMesh mesh = createBoneMesh (
               "TalusLeft", 0.05, new Vector3i (20, 20, 20));
            body = new RigidBody ("talus", mesh, null, density, 1.0);
            body.setDistanceGridRes (new Vector3i (20, 20, 20));
            body.setPose (new RigidTransform3d (
                             0, 0, 1.5, Math.PI, 0, DTOR*90.0));
            useMesh = true;
            break;
         }
         case HIP: {
            PolygonalMesh mesh = createBoneMesh (
               "OsCoxaeLeft", 0.015, null);
            body = new RigidBody ("hip", mesh, null, density, 1.0);
            body.setDistanceGridRes (new Vector3i (50, 50, 50));
            body.setPose (new RigidTransform3d (
                             0, -0.5, 1.5, -DTOR*120, 0, DTOR*90.0));
            useMesh = true;
            break;
         }
         case PHALANX: {
            PolygonalMesh mesh = createBoneMesh (
               "FP1ProximalRight", 0.1, null);
            body = new RigidBody ("phalanx", mesh, null, density, 1.0);
            body.setDistanceGridRes (new Vector3i (20, 20, 20));
            body.setPose (new RigidTransform3d (
                             0, 0, 1.5, DTOR*135, 0, DTOR*90.0));
            useMesh = true;
            break;
         }
         case HUMERUS: {
            PolygonalMesh mesh = createBoneMesh (
               "HumerusLeft", 0.1, null);
            body = new RigidBody ("humerus", mesh, null, density, 1.0);
            body.setDistanceGridRes (new Vector3i (20, 20, 20));
            body.setPose (new RigidTransform3d (
                             0, 0, 1.5, Math.PI, 0, DTOR*90.0));
            useMesh = true;
            break;
         }
         case FEMUR: {
            PolygonalMesh mesh = createBoneMesh (
               "SmoothFemurRight", 0.1, null);
            body = new RigidBody ("femur", mesh, null, density, 0.15);
            body.setDistanceGridRes (new Vector3i (80, 80, 80));
            body.setPose (new RigidTransform3d (
                             0, 0, -1, 0, Math.PI/2, 0));
            useMesh = true;
            break;
         }
         default: {
         }
      }
      RenderProps.setPointRadius (body, 0);
      RenderProps.setLineRadius (body, 0);
      RenderProps.setFaceColor (body, new Color (238, 232, 170)); // bone color
      return body;
   }

   public void build (String[] args) {
      DistanceGrid.DEFAULT_DISTANCE_METHOD = DistanceMethod.BVH;

      //myLeftKnotX = -8*size;
      myNumKnots = 100;

      parseArgs (args);

      double[] p0Trajectory = 
        new double[] {
            1.0, 0.0, 0.2, 2.5, 
            1.5, -1.0, 0.35, 1.5, 
            2.0, 0.0, 0.4, 0.5, 
            3.0, 2.0, 0.6, 1.5,
            6.0, 2.0, 0.6, 1.5,
            7.0, 0.0, 0.4, 0.5,
            7.5, -1.0, 0.25, 1.5,
            8.0, 0.0, 0.0, 2.5,
            9.0, 1.5, 0.0, 1.75 };

      double[] sphereP0Trajectory = 
        new double[] {
            1.0, 3.0, 0.2, 3.5, 
            1.5, 3.0, 0.35, 2.5, 
            2.0, 3.0, 0.4, 1.0, 
            3.0, 3.0, 1.5, 4.0,
            6.0, 3.0, -1.5, 2.5,
            7.0, 3.0, 0.4, 1.0,
            7.5, 3.0, 0.25, 2.5,
            8.0, 3.0, 0.0, 4.0 };

      double[] posTrajectory = 
         new double[] {
            0.0, 0.0, 0.0, 0.0,
            2.0, 0.0, 0.0, 0.0,
            2.5, 0.0, 0.0, 0.0,
            3.0, 0.1, 0.0, -0.1,
            3.5, 0.5, 0.0, -0.5,
            4.5, -1.0, 0.0, 0.0,
            5.5, 0.0, 0.0,  0.5,
            6.5, 0.5, 0.0, 0.5,
            7.5, 1.0, 0.0, 1.0,
            8.5, 2.0, 0.0, -1.0,
            //           8.99, 0.0, 0.0, 0.0,
            9.0, 0.0, 0.0, 0.0,
            10.0, 0.0, 0.0, 0.0 };

      double[] dumbbellPosTrajectory = 
         new double[] {
            0.0, 0.0, 0.0, 0.0,
            2.0, 0.0, 0.0, 0.0,
            2.5, 0.0, 0.0, 0.0,
            3.0, 0.1, 0.0, -0.1,
            3.5, 0.5, 0.0, -0.5,
            4.5, -1.0, 1.2, 0.0,
            5.5, 0.0, 1.2,  0.5,
            6.5, 0.5, 0.0, 0.5,
            7.5, 1.0, 0.0, 1.0,
            8.5, 2.0, 0.0, -1.0,
            //           8.99, 0.0, 0.0, 0.0,
            9.0, 0.0, 0.0, 0.0,
            10.0, 0.0, 0.0, 0.0 };

      double[] torusPosTrajectory =
          new double[] {
            0.0, 0.0, 0.0, 0.0,
            2.5, 0.0, 0.0, 0.0,
            3.0, 0.1, 0.0, -0.1,
            3.5, 0.5, 0.0, -0.5,
            4.5, -1.0, 0.0, 0.0,
            5.5, 0.5, 0.0,  0.5,
            6.5, -1.5, 0.0, 0.0,
            7.5, 0.0, 0.0, 0.5,
            8.5, 0.5, 0.0, -1.0,
            //8.99, 0.0, 0.0, 0.0,
            9.0, 0.0, 0.0, 0.0,
            10.0, 0.0, 0.0, 0.0 };        

      double[] talusP0Trajectory = 
         new double[] {
            1.0, 1.0, 0.0, 3.5, 
            1.5, -1.75, 0.0, 3.5,
            2.0, -2.00, 0.0, 1.5,
            2.5, -2.00, 0.0, 1.5,
            4.5, -2.50, 0.0, 1.5,
            5.0, -2.50, 0.0, 3.75,
            5.5, 2.00, 0.0, 3.75,
            6.0, 2.00, 0.0, -1.00,
            6.5, -2.00, 0.0, -1.00,
            7.0, -2.00, 0.0, 3.5,
            7.5, 2.00, 0.0, 3.5,
            8.0, 2.00, 0.0, -1,
            8.5, -2.00, 0.0, -1,
            9.0, -2.00, 0.0, 3.5,
            10.0, -2.00, 0.0, 3.5 };

      double[] hipP0Trajectory = 
         new double[] {
            0.5, 0.0, 0.0, 0.5, 
            1.0, -2.0, 0.0, 0.5, 
            1.5, -2.0, 0.0, 3.0, 
            2.0, 0.5, -0.6, 2.0,
            2.5, 2.5, 0.1, 1.75,
            3.0, 2.5, -0.25, 0.5,
            3.5, 2.5, -0.25, 0.5,
            5.5, 2.5, -0.25, 0.5,
            10.0, 2.5, -0.25, 0.5 };

      double[] spherePosTrajectory =
          new double[] {
            0.0, 0.0, 0.0, 0.0,
            2.5, 0.0, 0.0, 0.0,
            3.0, -0.1, 0.0, 0.1,
            3.5, -0.5, 0.0, 0.5,
            4.5, 0.0, 0.0, 0.0,
            5.5, -1, 0.0, 1.0,
            6.5, 0.5, 0.0, 0.0,
            7.5, 0.0, 0.0, 0.0,
            8.5, 0.0, 0.0, 0.0,
            9.0, 0.0, 0.0, 0.0,
            10.0, 0.0, 0.0, 0.0 };        

      double[] rpyTrajectory = 
         new double[] {
            0.0, 0.0, 0.0, 0.0,
            3.0, 0.0, 0.0, 0.0,
            3.01, 0.0, 0.0, 0.0,
            3.5, 30.0, 0.0, -20.0,
            4.5, -30.0, 0.0, -20.0,
            5.5, -30.0, 0.0, 20.0,
            6.5, 30.0, 0.0, 20.0,
            6.99, 0.0, 0.0, 0.0,
            7.0, 0.0, 0.0, 0.0,
            10.0, 0.0, 0.0, 0.0 };

      double[] torusRpyTrajectory =
           new double[] {
            0.0, 0.0, 0.0, 0.0,
            3.0, 0.0, 0.0, 0.0,
            3.01, 0.0, 0.0, 0.0,
            3.5, .0, 30.0, -20.0,
            4.5, 0.0, -30.0, -20.0,
            5.5, 0.0, -30.0, 20.0,
            6.5, 0.0, 30.0, 20.0,
            6.99, 0.0, 0.0, 0.0,
            7.0, 0.0, 0.0, 0.0,
            10.0, 0.0, 0.0, 0.0 };

      double[] phalanxRpyTrajectory = 
         new double[] {
            0.0, 0.0, 0.0, 0.0,
            3.0, 0.0, 0.0, 0.0,
            3.01, 0.0, 0.0, 0.0,
            3.5, 30.0, 0.0, -20.0,
            4.5, -20.0, 0.0, -20.0,
            5.5, -20.0, 0.0, 18.0,
            6.5, 30.0, 0.0, 18.0,
            6.99, 0.0, 0.0, 0.0,
            7.0, 0.0, 0.0, 0.0,
            10.0, 0.0, 0.0, 0.0 };


      switch (myGeometryType) {
         case ELLIPSOID: {
            myLeftKnotY = -size/2;
            break;
         }
         case SPHERE: {
            p0Trajectory = sphereP0Trajectory;
            rpyTrajectory = null;
            posTrajectory = spherePosTrajectory;
            myLeftKnotY = 0;
            break;
         }
         case TORUS: {
            posTrajectory = torusPosTrajectory;
            rpyTrajectory = torusRpyTrajectory;
            break;
         }
         case DUMBBELL: {
            posTrajectory = dumbbellPosTrajectory;
            break;
         }
         case PHALANX: {
            rpyTrajectory = phalanxRpyTrajectory;
            break;
         }
         case TALUS: {
            p0Trajectory = talusP0Trajectory;
            myLeftKnotY = 0;
            break;
         }
         case HIP: {
            p0Trajectory = hipP0Trajectory;
            posTrajectory = torusPosTrajectory;
            myWrapDamping = 20;
            myMaxWrapIterations = 20;
            myLeftKnotY = 0;
            break;
         }
         default:
            // nothing to do
      }

      super.build (args);

      if (!myGeometryType.isAnalytic()) {
         myUseMesh = true;
      }
      myBody = createGeometry (myGeometryType, size, myDensity, myUseMesh);
      if (myUseMesh) {
         myBody.setGridSurfaceRendering (true);
      }

      myMech.addRigidBody (myBody);
      //cylinder.setDynamic (false);
      //RenderProps.setAlpha (myBody, 0.5);

      mySpring.addWrappable ((Wrappable)myBody);
      // call to ensure that max wrap displacement is computed in advance:
      mySpring.setMaxWrapDisplacement(-1);

      myBody.setDynamic (myDynamic);
      if (!myDynamic) {
         myBody.setFrameDamping (0);
         myBody.setRotaryDamping (0);
      }

      RenderProps.setPointRadius (mySpring, 0.03);
      RenderProps.setLineRadius (mySpring, 0.025);
      mySpring.setDrawKnots (false);
      mySpring.setContactingKnotsColor (Color.MAGENTA);
      mySpring.setWrapDamping (myWrapDamping);
      mySpring.setProfiling (true);
      mySpring.setConvergenceTol (1e-8);
      mySpring.setMaxWrapIterations (20);

      createControlPanel();

      //addPerformanceProbes();

      if (rpyTrajectory != null) {
         addFrameRpyInput (myBody, rpyTrajectory);
      }
      if (posTrajectory != null) {
         addFramePosInput (myBody, posTrajectory);
      }
      if (p0Trajectory != null) {
         addPointPosInput (myP0, p0Trajectory);
      }
      myForceProbe = addForceProbe (myBody);
      myMomentProbe = addMomentProbe (myBody);
      
      //addBreakPoint (5.63);
      addBreakPoint (10.00);

      // try {
      //    IndentingPrintWriter pw =
      //       ArtisynthIO.newIndentingPrintWriter ("mygrid.txt");
      //    DistanceGrid grid = myBody.getDistanceGrid();
      //    if (grid != null) {
      //       grid.write (pw, new NumberFormat("%g"), null);
      //       pw.close();         
      //    }
      // }
      // catch (Exception e) {
      // }
   }

   void createControlPanel () {
      ControlPanel panel = createControlPanel (myMech);
      panel.addWidget (this, "smoothIters");
      panel.addWidget (this, "smoothMu");
      panel.addWidget (this, "smoothLambda");
      addControlPanel (panel);
   }

   boolean[] inContact = new boolean[1001];
   double interval = 0.01;

   void printDevInfo (PrintWriter pw) {
      // double frange = myForceProbe.getForceRange();
      // double stdDev = myForceProbe.getStdDev();
      // double maxDev = myForceProbe.getMaxDev();
      // double maxDevTime = myForceProbe.getMaxDevTime();
      pw.print (myGeometryType);
      if (myUseMesh) {
         pw.print (" mesh");
      }
      double wrapDamping = mySpring.getWrapDamping();
      int maxWrapIterations = mySpring.getMaxWrapIterations();
      if (wrapDamping != 10 || maxWrapIterations != 10) {
         pw.print (" "+wrapDamping+" "+maxWrapIterations);
      }
      pw.println ("");            
      // pw.printf ("std %g (%g)\n", stdDev/frange, stdDev);
      // pw.printf ("max %g (%g) at %g\n", maxDev/frange, maxDev, maxDevTime);
      double convergedFraction =
         mySpring.getConvergedCount()/(double)mySpring.getContactCount();
      pw.printf (
         "avgTime %6.1f usec iters=%5d contactCalcs=%5d converged: %5.3f stuck=%d/%d falseStuck=%d\n",
         mySpring.getProfileTimeUsec(),
         mySpring.getIterationCount(),
         mySpring.getUpdateContactsCount(),
         convergedFraction,
         mySpring.totalStuck,
         mySpring.getContactCount(),
         mySpring.totalFalseStuck);
                 
   }

   public StepAdjustment advance (double t0, double t1, int flags) {
      if (t0 == 0) {
         mySpring.setProfiling (true); // reset profiling info
      }

      StepAdjustment sa = super.advance (t0, t1, flags);

      if (t1 <= 10.0) {
         WrapSegment seg = (WrapSegment)mySpring.getSegment(0);
         inContact[(int)Math.round(t1/interval)] = seg.inContact();
      }

      if (t1 == 10.0 && myForceProbe != null) {
         //myForceProbe.updateDeviations (inContact, interval, 4, 0.25);
         PrintWriter pw = new PrintWriter (System.out);
         printDevInfo (pw);
         pw.flush();

         System.out.println (
            "culled: "+(DistanceGrid.culledQueries/
                        (double)DistanceGrid.totalQueries));

         computeForceErrors();

         // myForceProbe.printMaxDevInfo ("maxdev.txt");

         // try {
         //    pw = new PrintWriter (new FileWriter ("devinfo.txt", true));
         //    printDevInfo (pw);
         //    pw.close();
         // }
         // catch (Exception e) {
         //    e.printStackTrace();
         // }
      }
      
      return sa;
   }

   protected void addPanController () {
      Point3d eye = new Point3d(getViewerEye());
      eye.y *= 0.55;
      setViewerEye (eye);
      eye.add (getViewerCenter());
      addController (new PanController (this, eye, 10.0, Vector3d.Z_UNIT, 0, 10));
      
   }

   public void attach (DriverInterface driver) {
      addPanController();
   }

   void smooth() {
      if (myUseMesh) {
         DistanceGridComp gcomp = myBody.getDistanceGridComp();
         DistanceGrid grid = new DistanceGrid(gcomp.getGrid());
         //grid.smooth (myLambda, myMu, myIters);
         grid.smooth (myIters);
         gcomp.setGrid (grid);
      }
   }

   public boolean getMenuItems(List<Object> items) {
      JMenuItem item =
         GuiUtils.createMenuItem (this, "smooth", "smooth the mesh");
      items.add (item);
      if (!myUseMesh) {
         item.setEnabled (false);
      }
      return true;
   }   

   public void actionPerformed(ActionEvent event) {
      if (event.getActionCommand().equals ("smooth")) {
         smooth();
         rerender();
      }
   } 

   public Vector3d[] loadFileForceValues (String fileName) {
      ArrayList<Vector3d> values = new ArrayList<>();
      String path = PathFinder.getSourceRelativePath (this, "data/"+fileName);
      try {
         ReaderTokenizer rtok = ArtisynthIO.newReaderTokenizer (path);
         while (rtok.nextToken() != ReaderTokenizer.TT_EOF) {
            rtok.pushBack();
            Vector3d f = new Vector3d();
            f.x = rtok.scanNumber();
            rtok.scanToken (',');
            f.y = rtok.scanNumber();
            rtok.scanToken (',');
            f.z = rtok.scanNumber();
            values.add (f);
         }
      }
      catch (Exception e) {
         System.out.println ("Error reading " + fileName);
         e.printStackTrace();
      }
      return values.toArray(new Vector3d[0]);
   }

   public Vector3d[] loadProbeForceValues (NumericProbeBase probe) {
      NumericList list = probe.getNumericList();
      NumericListKnot knot;
      if (list.getVectorSize() != 3) {
         System.out.println (
            "Probe must have vector size of 3, has " + list.getVectorSize());
         return null;
      }
      Vector3d[] values = new Vector3d[list.getNumKnots()];
      int k = 0;
      for (knot=list.getFirst(); knot!=null; knot=knot.getNext()) {
         values[k++] = new Vector3d (knot.v);
      }
      return values;
   }

   protected void computeForceErrors() {
      
      String forceRefFile = null;
      String momentRefFile = null;
      switch (myGeometryType) {
         case SPHERE: {
            forceRefFile = "sphereForceRef.csv";
            momentRefFile = "sphereMomentRef.csv";
            break;
         }
         case CYLINDER: {
            forceRefFile = "cylinderForceRef.csv";
            momentRefFile = "cylinderMomentRef.csv";
            break;
         }
         case TORUS: {
            forceRefFile = "torusForceRef.csv";
            momentRefFile = "torusMomentRef.csv";
            break;
         }
         case ELLIPSOID: {
            forceRefFile = "ellipsoidForceRef.csv";
            momentRefFile = "ellipsoidMomentRef.csv";
            break;
         }
         default: {
         }
      }
      if (forceRefFile != null) {
         computeForceErrors (
            myBody, myForceProbe, myMomentProbe, forceRefFile, momentRefFile);
      }
   }


   public void computeForceErrors (
      RigidBody body,
      NumericProbeBase forceProbe, NumericProbeBase momentProbe, 
      String forceRefFileName, String momentRefFileName) {

      Vector3d[] forceRef = loadFileForceValues (forceRefFileName);
      Vector3d[] momentRef = loadFileForceValues (momentRefFileName);
      Vector3d[] force = loadProbeForceValues (forceProbe);
      Vector3d[] moment = loadProbeForceValues (momentProbe);

      int numv = forceRef.length;
      numv = Math.min(momentRef.length, numv);
      numv = Math.min(force.length, numv);
      numv = Math.min(moment.length, numv);
      if (numv != forceRef.length) {
         System.out.println (
            "Warning: value sets have different lengthsl; using minimum "+numv);
      }
      SpatialInertia MI = new SpatialInertia();
      myBody.getInertia(MI);
      MI.scale (1/MI.getMass());

      double maxF = 0;
      double maxErr = 0;
      double avgErr = 0;
      Twist tw = new Twist();
      Wrench wr = new Wrench();
      Wrench we = new Wrench();

      Vector3d maxf = new Vector3d();
      Vector3d maxm = new Vector3d();
      
      Vector3d maxE = new Vector3d();
      Vector3d avgE = new Vector3d();

      Vector3d maxM = new Vector3d();
      Vector3d avgM = new Vector3d();

      for (int k=0; k<numv; k++) {
         wr.f.set (forceRef[k]);
         wr.m.set (momentRef[k]);
         MI.mulInverse (tw, wr);
         maxF = Math.max (maxF, Math.sqrt(wr.dot(tw)));
         
         we.set (wr);
         we.f.sub (force[k]);
         we.m.sub (moment[k]);
         MI.mulInverse (tw, we);
         double err = Math.sqrt(we.dot(tw));

         Vector3d fabs = new Vector3d();
         fabs.absolute(forceRef[k]);
         Vector3d mabs = new Vector3d();
         mabs.absolute(momentRef[k]);

         maxf.max (fabs);
         maxm.max (mabs);

         Vector3d E = new Vector3d();
         
         E.sub (force[k], forceRef[k]);
         E.absolute();

         avgE.add (E);
         maxE.max (E);

         Vector3d M = new Vector3d();

         M.sub (moment[k], momentRef[k]);
         M.absolute();

         avgM.add (M);
         maxM.max (M);

         maxErr = Math.max (maxErr, err);         
         avgErr += err;
      }
      avgErr /= numv;
      avgE.scale (1/(double)numv);
      avgM.scale (1/(double)numv);

      maxE.scale (1/maxf.maxElement());
      avgE.scale (1/maxf.maxElement());
      maxM.scale (1/maxm.maxElement());
      avgM.scale (1/maxm.maxElement());
      System.out.println ("maxF="+maxF);
      if (maxF != 0) {
         avgErr /= maxF;
         maxErr /= maxF;
      }
      System.out.println ("maxE=" + maxE.toString("%9.6f") + " avgE=" + avgE.toString("%9.6f"));
      System.out.println ("maxM=" + maxM.toString("%9.6f") + " avgM=" + avgM.toString("%9.6f"));
      System.out.printf ("force error: max=%9.6f avg=%9.6f\n", maxErr, avgErr);
   }

}

