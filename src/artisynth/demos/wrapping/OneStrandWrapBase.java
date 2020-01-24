package artisynth.demos.wrapping;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JSeparator;

import artisynth.core.gui.ControlPanel;
import artisynth.core.mechmodels.DistanceGridComp;
import artisynth.core.mechmodels.Frame;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.MultiPointSpring;
import artisynth.core.mechmodels.MultiPointSpring.WrapSegment;
import artisynth.core.mechmodels.Particle;
import artisynth.core.mechmodels.Point;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.modelbase.ControllerBase;
import artisynth.core.modelbase.HasNumericState;
import artisynth.core.probes.DataFunction;
import artisynth.core.probes.NumericControlProbe;
import artisynth.core.probes.NumericInputProbe;
import artisynth.core.probes.NumericMonitorProbe;
import artisynth.core.workspace.RootModel;
import maspack.geometry.DistanceGrid;
import maspack.geometry.Feature;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.interpolation.Interpolation.Order;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.Vector3d;
import maspack.matrix.Vector3i;
import maspack.matrix.VectorNd;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.util.DataBuffer;
import maspack.util.PathFinder;

/**
 * Base class for models that test the interaction of
 * wrappable bodies with a single strand of a MultiPointSpring.
 * 
 * @author John E. Lloyd
 */
public abstract class OneStrandWrapBase extends RootModel {

   protected boolean pointsAttached = false;
   protected boolean collisionEnabled = false;
   protected double planeZ = -20;
   protected double myDensity = 0.2;
   protected static double size = 1.0;
   protected MechModel myMech;
   protected MultiPointSpring mySpring;
   protected int myNumKnots = 50;
   protected boolean myDynamic = false;
   protected double myWrapDamping = 10;
   protected int myMaxWrapIterations = 10;
   protected boolean myApplyVel = true;
   protected double myLeftKnotX = -size*3;
   protected double myLeftKnotY = -size;

   Particle myP0;
   Particle myP1;

   protected class RpyControlProbe extends NumericControlProbe {
      
      Frame myFrame;
      RotationMatrix3d myR0 = new RotationMatrix3d();
      double DTOR = Math.PI/180.0;

      RpyControlProbe (
         Frame frame, double[] data, double startTime, double stopTime) {
         super (3, data, NumericInputProbe.EXPLICIT_TIME, startTime, stopTime);
         myFrame = frame;
         myR0.set (myFrame.getRotation());
      }

      public void applyData (VectorNd vec, double t, double trel) {
         RotationMatrix3d R = new RotationMatrix3d();
         R.setRpy (DTOR*vec.get(0), DTOR*vec.get(1), DTOR*vec.get(2));
         R.mul (myR0);
         myFrame.setTargetOrientation (R.getAxisAngle());
      }
   }

   protected class PosControlProbe extends NumericControlProbe {
      
      Frame myFrame;
      double DTOR = Math.PI/180.0;
      Point3d myPos0 = new Point3d();

      PosControlProbe (
         Frame frame, double[] data, double startTime, double stopTime) {
         super (3, data, NumericInputProbe.EXPLICIT_TIME, startTime, stopTime);
         myFrame = frame;
         myPos0.set (frame.getPosition());
      }

      public void applyData (VectorNd vec, double t, double trel) {
         Point3d pos = new Point3d();
         pos.set (vec.get(0), vec.get(1), vec.get(2));
         pos.add (myPos0);
         myFrame.setTargetPosition (pos);
      }
   }

   protected class PanController extends ControllerBase {
      
      RootModel myRoot;
      double DTOR = Math.PI/180.0;
      Vector3d myEyeWorld0 = new Vector3d();
      double myPeriod = 1.0;
      Vector3d myAxis = Vector3d.Z_UNIT;
      double myStart;
      double myStop;

      public PanController (
         RootModel root, Vector3d eyeWorld0, double period, Vector3d axis,
         double startTime, double stopTime) {
         myRoot = root;
         myEyeWorld0 = new Vector3d(eyeWorld0);
         myAxis = new Vector3d(axis);
         myPeriod = period;
         myStart = startTime;
         myStop = stopTime;
      }

      public void apply (double t0, double t1) {
         if (t1 > myStart && t1 <= myStop) {
            double ang = 2*Math.PI*(t1-myStart)/myPeriod;
            RotationMatrix3d R = new RotationMatrix3d();
            R.setAxisAngle (myAxis, ang);
            Point3d eye = new Point3d(myEyeWorld0);
            Point3d center = new Point3d(getViewerCenter());
            eye.transform (R);
            eye.sub (center);
            myRoot.setViewerEye (eye);
         }
      }
   }

   protected int matchArg (String[] args, int i) {
      int nm = 1;
      if (args[i].equals ("-dynamic")) {
         myDynamic = true;
      }
      else if (args[i].equals ("-static")) {
         myDynamic = false;
      }
      else if (args[i].equals ("-applyVel")) {
         myApplyVel = true;
      }
      else if (args[i].equals ("-ignoreVel")) {
         myApplyVel = false;
      }
      else if (args[i].equals ("-numKnots")) {
         if (i == args.length-1) {
            System.out.println (
               "WARNING: -numKnots needs another argument");
         }
         else {
            myNumKnots = Integer.valueOf (args[++i]);
            nm++;
         }
      }
      else if (args[i].equals ("-wrapDamping")) {
         if (i == args.length-1) {
            System.out.println (
               "WARNING: -wrapDamping needs another argument");
         }
         else {
            myWrapDamping = Double.valueOf (args[++i]);
            nm++;
         }
      }
      else if (args[i].equals ("-maxWrapIterations")) {
         if (i == args.length-1) {
            System.out.println (
               "WARNING: -maxWrapIterations needs another argument");
         }
         else {
            myMaxWrapIterations = Integer.valueOf (args[++i]);
            nm++;
         }
      }
      else {
         nm = 0;
      }
      return nm;
   }

   protected void parseArgs (String[] args) {
      int i = 0;
      while (i<args.length) {
         int nm = matchArg (args, i);
         if (nm == 0) {
            System.out.println (
               "WARNING: unknown argument '"+args[i]+"'");
            i++;
         }
         else {
            i += nm;
         }
      }
   }

   protected static class SegmentEnergy
      implements DataFunction {

      MultiPointSpring mySpr;
      int mySegIdx;

      SegmentEnergy (MultiPointSpring spr, int segIdx) {
         mySpr = spr;
         mySegIdx = segIdx;
      }

      public void eval (VectorNd vec, double t, double trel) {
         WrapSegment wseg = (WrapSegment)mySpr.getSegment(mySegIdx);
         vec.set (0, wseg.computeEnergy());
      }
   }

   protected static class SegmentEnergyDiff
      implements DataFunction, HasNumericState {

      MultiPointSpring mySpr;
      int mySegIdx;
      double myLastEnergy;

      SegmentEnergyDiff (MultiPointSpring spr, int segIdx) {
         mySpr = spr;
         mySegIdx = segIdx;
         myLastEnergy = -1;
      }

      public void eval (VectorNd vec, double t, double trel) {
         WrapSegment wseg = (WrapSegment)mySpr.getSegment(mySegIdx);
         double energy = wseg.computeEnergy();
         if (myLastEnergy != -1) {
            vec.set (0, energy-myLastEnergy);
         }
         else {
            vec.set (0, 0.0);
         }
         myLastEnergy = energy;
      }

      public void advanceState (double t0, double t1) {
      }

      public void getState (DataBuffer data) {
         data.dput (myLastEnergy);
      }
      
      public void setState (DataBuffer data) {
         myLastEnergy = data.dget();
      }
      
      public boolean hasState() {
         return true;
      }      
   }

   protected static class ABDeflection
      implements DataFunction {

      MultiPointSpring mySpr;
      int mySegIdx;
      int myCoordIdx;

      public ABDeflection (MultiPointSpring spr, int segIdx, int coordIdx) {
         mySpr = spr;
         mySegIdx = segIdx;
         myCoordIdx = coordIdx;
      }

      public void eval (VectorNd vec, double t, double trel) {
         WrapSegment wseg = (WrapSegment)mySpr.getSegment(mySegIdx);
         Point p = wseg.getABPoint(0);
         double val = 0;
         if (p != null) {
            val = p.getPosition().get(myCoordIdx);
         }
         vec.set (0, val);
      }
   }

   protected void addPerformanceProbes() {
      
      NumericMonitorProbe probe = 
         new NumericMonitorProbe (1, null, 0, 10.0, 0.01);
      probe.setName ("AB deflection");
      //probe.setDataFunction (new SegmentEnergy (mySpring, 0));
      probe.setDataFunction (new ABDeflection (mySpring, 0, 1));
      addOutputProbe (probe);

      probe = 
         new NumericMonitorProbe (1, null, 0, 10.0, 0.01);
      probe.setName ("Energy diff");
      probe.setDataFunction (new SegmentEnergyDiff (mySpring, 0));
      addOutputProbe (probe);
   }

   protected void addPointPosInput (Point pnt, double[] pdata) {
      NumericInputProbe probe =
         new NumericInputProbe (
            pnt, "targetPosition", 0, 10);
      probe.setName ("point pos");
      probe.addData (pdata, NumericInputProbe.EXPLICIT_TIME);
      probe.setInterpolationOrder (Order.Cubic);
      addInputProbe (probe);
   }

   protected void addFramePosInput (Frame frame, double[] pdata) {
      PosControlProbe probe =
         new PosControlProbe (frame, pdata, 0, 10);
      probe.setName ("frame pos");
      probe.setInterpolationOrder (Order.Cubic);
      addInputProbe (probe);
   }

   protected void addFrameRpyInput (Frame frame, double[] pdata) {
      RpyControlProbe probe =
         new RpyControlProbe (frame, pdata, 0, 10);
      probe.setName ("frame rpy");
      probe.setInterpolationOrder (Order.Cubic);
      addInputProbe (probe);
   }

   protected MomentProbe addMomentProbe (Frame frame) {
      MomentProbe probe = new MomentProbe (frame, null, 0, 10, -1);
      probe.setName ("frame moment");
      addOutputProbe (probe);
      return probe;
   }

   protected ForceProbe addForceProbe (Frame frame) {
      ForceProbe probe = new ForceProbe (frame, null, 0, 10, -1);
      probe.setName ("frame force");
      addOutputProbe (probe);
      return probe;
   }

   public void build (String[] args) {
      myMech = new MechModel ("mechMod");

      myMech.setGravity (0, 0, -9.8);
      myMech.setFrameDamping (1.0);
      myMech.setRotaryDamping (0.1);

      myP0 = new Particle (0.1, size*3, 0, size / 2);
      myP0.setDynamic (false);
      myMech.addParticle (myP0);

      myP1 = new Particle (0.1, myLeftKnotX, myLeftKnotY, size / 2);
      myP1.setDynamic (false);
      myMech.addParticle (myP1);

      mySpring = new MultiPointSpring ("spring", 1, 0.1, 0);
      mySpring.addPoint (myP0);
      if (myNumKnots > 0) {
         mySpring.setSegmentWrappable (myNumKnots);
      }
      mySpring.addPoint (myP1);

      //mySpring.setWrapDamping (1.0);
      //mySpring.setWrapStiffness (10);
      //mySpring.setWrapH (0.01);
      myMech.addMultiPointSpring (mySpring);

      mySpring.setDrawKnots (false);
      mySpring.setDrawABPoints (true);
      mySpring.setWrapDamping (myWrapDamping);
      mySpring.setMaxWrapIterations (myMaxWrapIterations);

      addModel (myMech);

      RenderProps.setPointStyle (myMech, Renderer.PointStyle.SPHERE);
      RenderProps.setPointRadius (myMech, size / 10);
      RenderProps.setLineStyle (myMech, Renderer.LineStyle.CYLINDER);
      RenderProps.setLineRadius (myMech, size / 30);
      RenderProps.setLineColor (myMech, Color.red);

      //createControlPanel (myMech);
   }

   protected static ControlPanel createControlPanel (
      MechModel mech, RigidBody bod, MultiPointSpring spr) {
      
      ControlPanel panel = new ControlPanel ("options", "");
      panel.addWidget (mech, "integrator");
      panel.addWidget (mech, "maxStepSize");
      panel.addWidget (mech, "gravity");
      panel.addWidget (spr, "wrapStiffness");
      panel.addWidget (spr, "wrapDamping");
      panel.addWidget (spr, "contactStiffness");
      panel.addWidget (spr, "contactDamping");
      panel.addWidget (spr, "maxWrapIterations");
      panel.addWidget (spr, "drawKnots");
      panel.addWidget (spr, "sor");
      panel.addWidget (spr, "lineSearch");
      panel.addWidget (spr, "drawABPoints");
      panel.addWidget (spr, "debugLevel");
      panel.addWidget (spr, "profiling");
      panel.addWidget (spr, "length");
      if (bod != null) {
         DistanceGridComp gcomp = bod.getDistanceGridComp();
         panel.addWidget (new JSeparator());
         panel.addLabel ("distanceGrid:");
         panel.addWidget (gcomp, "resolution");
         panel.addWidget (gcomp, "maxResolution");
         panel.addWidget (gcomp, "fitWithOBB");
         panel.addWidget (gcomp, "surfaceDistance");
         panel.addWidget (bod, "gridSurfaceRendering");
         panel.addWidget (gcomp, "renderGrid");
         panel.addWidget (gcomp, "renderRanges");
         panel.addWidget (new JSeparator());
      }
      return panel;
   }

   protected ControlPanel createControlPanel (MechModel mech) {

      RigidBody bod = (RigidBody)mech.findComponent ("rigidBodies/0");
      MultiPointSpring spr =
         (MultiPointSpring)mech.findComponent ("multiPointSprings/0");
      ControlPanel panel = createControlPanel (mech, bod, spr);
      return panel;
   }

   protected static PolygonalMesh createDumbbell (double radius) {
      PolygonalMesh ball0 = MeshFactory.createIcosahedralSphere (radius, 1);
      ball0.transform (new RigidTransform3d (-2*radius, 0, 0));
      PolygonalMesh ball1 = MeshFactory.createIcosahedralSphere (radius, 1);
      ball1.transform (new RigidTransform3d (2*radius, 0, 0));
      PolygonalMesh bar = MeshFactory.createCylinder (radius/2, radius*4, 32);
      bar.transform (new RigidTransform3d (0, 0, 0, 0, Math.PI/2, 0));

      DistanceGrid grid = new DistanceGrid (new Vector3i (20, 20, 20));
      ArrayList<List<? extends Feature>> faceSets =
         new ArrayList<List<? extends Feature>>();     
      faceSets.add (bar.getFaces());
      faceSets.add (ball0.getFaces());
      faceSets.add (ball1.getFaces());

      grid.fitToFeatures (faceSets, 0.1, null, 0);
      grid.computeDistances (bar, /*signed=*/true);
      grid.computeUnion (ball0);
      grid.computeUnion (ball1);

      grid.computeDistances (
         grid.createDistanceSurface(), /*signed=*/true);
      grid.smooth (0.33, -0.2, 30);
      return grid.createQuadDistanceSurface(0, 5);
   }

   protected static PolygonalMesh createBoneMesh (
      String name, double scale, Vector3i smoothRes) {

      PolygonalMesh mesh = null;
      String path = PathFinder.getSourceRelativePath (
         OneStrandWrapBase.class, "geometry/" + name + ".obj");
      try {
         mesh = new PolygonalMesh (path);
      }
      catch (Exception e) {
         System.out.println ("Can't read "+path);
         System.exit(1);
      }
      mesh.scale (scale);
      mesh.translateToCenterOfVolume();

      if (smoothRes != null) {
         DistanceGrid grid = new DistanceGrid (smoothRes);
         grid.computeFromFeatures (
            mesh.getFaces(), 0.1, null, 0, /*signed=*/true);
         grid.smooth (0.33, -0.2, 30);
         return grid.createQuadDistanceSurface(0, 5);
      }
      else {
         return mesh;
      }
   }
}
