package artisynth.demos.wrapping;

import java.awt.Color;

import javax.swing.JSeparator;

import artisynth.core.driver.Main;
import artisynth.core.gui.ControlPanel;
import artisynth.core.mechmodels.DistanceGridComp;
import artisynth.core.mechmodels.Frame;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.MultiPointSpring;
import artisynth.core.mechmodels.MultiPointSpring.WrapSegment;
import artisynth.core.mechmodels.Particle;
import artisynth.core.mechmodels.Point;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.modelbase.HasNumericState;
import artisynth.core.probes.DataFunction;
import artisynth.core.probes.NumericMonitorProbe;
import artisynth.core.probes.NumericOutputProbe;
import artisynth.core.workspace.RootModel;
import maspack.matrix.VectorNd;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.util.DataBuffer;

/**
 * Base class for models that test the interaction of
 * wrappable bodies with two strands of a MultiPointSpring.
 * 
 * @author John E. Lloyd
 */
public abstract class TwoStrandWrapBase extends RootModel {

   protected boolean pointsAttached = false;
   protected boolean collisionEnabled = false;
   protected double planeZ = -20;
   protected double myDensity = 0.2;
   protected static double size = 1.0;
   protected static double myGap = 2*size;
   protected static double mySpringStiffness = 1.0;
   protected static double mySpringDamping = 0.1;
   protected static double myWrapDamping = 10;
   protected static int myMaxWrapIterations = 10;
   protected MechModel myMech;
   protected MultiPointSpring mySpring;
   protected int myNumKnots0 = 50;
   protected int myNumKnots1 = 50;
   protected double myLeftKnotX = -size*3;
   protected double myRightKnotX = size*3;

   Particle myP0;
   Particle myP1;
   Particle myP2;
   Particle myP3;

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

//   protected static class ContactEnergy
//      implements DataFunction {
//
//      MultiPointSpring mySpr;
//      int mySegIdx;
//
//      ContactEnergy (MultiPointSpring spr, int segIdx) {
//         mySpr = spr;
//         mySegIdx = segIdx;
//      }
//
//      public void eval (VectorNd vec, double t, double trel) {
//         WrapSegment wseg = (WrapSegment)mySpr.getSegment(mySegIdx);
//         vec.set (0, wseg.computeContactEnergy());
//      }
//   }

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

//   protected static class SpringEnergyDiff
//      implements DataFunction, HasNumericState {
//
//      MultiPointSpring mySpr;
//      int mySegIdx;
//      double myLastEnergy;
//
//      SpringEnergyDiff (MultiPointSpring spr, int segIdx) {
//         mySpr = spr;
//         mySegIdx = segIdx;
//         myLastEnergy = -1;
//      }
//
//      public void eval (VectorNd vec, double t, double trel) {
//         WrapSegment wseg = (WrapSegment)mySpr.getSegment(mySegIdx);
//         double energy = wseg.computeEnergy() - wseg.computeContactEnergy();
//         if (myLastEnergy != -1) {
//            vec.set (0, energy-myLastEnergy);
//         }
//         else {
//            vec.set (0, 0.0);
//         }
//         myLastEnergy = energy;
//      }
//
//      public void advanceState (double t0, double t1) {
//      }
//
//      public void getState (DataBuffer data) {
//         data.dput (myLastEnergy);
//      }
//
//      public void setState (DataBuffer data) {
//         myLastEnergy = data.dget();
//      }
//      
//      public boolean hasState() {
//         return true;
//      }
//   }

   protected static class ABDeflection
      implements DataFunction {

      MultiPointSpring mySpr;
      int mySegIdx;
      int myABIdx;
      int myCoordIdx;

      public ABDeflection (
         MultiPointSpring spr, int segIdx, int ABIdx, int coordIdx) {
         mySpr = spr;
         mySegIdx = segIdx;
         myABIdx = ABIdx;
         myCoordIdx = coordIdx;
      }

      public void eval (VectorNd vec, double t, double trel) {
         WrapSegment wseg = (WrapSegment)mySpr.getSegment(mySegIdx);
         Point p = wseg.getABPoint(myABIdx);
         double val = 0;
         if (p != null) {
            val = p.getPosition().get(myCoordIdx);
         }
         vec.set (0, val);
      }
   }

//   protected static class SegmentLength
//      implements DataFunction {
//
//      MultiPointSpring mySpr;
//      int mySegIdx;
//
//      public SegmentLength (MultiPointSpring spr, int segIdx) {
//         mySpr = spr;
//         mySegIdx = segIdx;
//      }
//
//      public void eval (VectorNd vec, double t, double trel) {
//         WrapSegment wseg = (WrapSegment)mySpr.getSegment(mySegIdx);
//         vec.set (0, wseg.computeStrandLength());
//      }
//   }

//   protected static class SegmentLengthDiff
//      implements DataFunction, HasNumericState {
//
//      MultiPointSpring mySpr;
//      int mySegIdx;
//      double myLastLength;
//
//      public SegmentLengthDiff (MultiPointSpring spr, int segIdx) {
//         mySpr = spr;
//         mySegIdx = segIdx;
//         myLastLength = -1;
//      }
//
//      public void eval (VectorNd vec, double t, double trel) {
//         WrapSegment wseg = (WrapSegment)mySpr.getSegment(mySegIdx);
//         double length = wseg.computeStrandLength();
//         if (myLastLength != -1) {
//            vec.set (0, length-myLastLength);
//         }
//         else {
//            vec.set (0, 0);
//         }
//         myLastLength = length;
//      }
//
//      public void advanceState (double t0, double t1) {
//      }
//
//      public void getState (DataBuffer data) {
//         data.dput (myLastLength);
//      }
//
//      public void setState (DataBuffer data) {
//         myLastLength = data.dget();
//      }
//      
//      public boolean hasState() {
//         return true;
//      }
//   }

   protected void addPerformanceProbes() {
      
      NumericMonitorProbe probe = 
         new NumericMonitorProbe (1, null, 0, 10.0, 0.01);
      probe.setName ("AB deflection");
      //probe.setDataFunction (new SegmentEnergy (mySpring, 0));
      probe.setDataFunction (new ABDeflection (mySpring, 0, 0, 1));
      addOutputProbe (probe);

//      probe = 
//         new NumericMonitorProbe (1, null, 0, 10.0, 0.01);
//      probe.setName ("Length diff");
//      probe.setDataFunction (new SegmentLengthDiff (mySpring, 0));
//      addOutputProbe (probe);

      probe = 
         new NumericMonitorProbe (1, null, 0, 10.0, 0.01);
      probe.setName ("Energy diff");
      probe.setDataFunction (new SegmentEnergyDiff (mySpring, 0));
      addOutputProbe (probe);
   }

   protected class PosProbe extends NumericOutputProbe {
      Frame myFrame;
      boolean myEstimatePeriod;
      double myThreshold = 0;

      public void setEstimatePeriod (boolean enable, double thresh) {
         myEstimatePeriod = enable;
         myThreshold = thresh;
      }

      double estimatePeriod() {
         double[][] values = getOutput().getValues();
         double zprev = values[0][3];
         double tstart = -1;
         int cnt = 0;
         double period = 0;
         for (int k=1; k<values.length; k++) {
            double z = values[k][3] - myThreshold;
            if (zprev >= 0 && z < 0) {
               tstart = ((k-1) + zprev/(zprev-z))*getUpdateInterval();
            }
            else if (tstart != -1 && z >= 0 && zprev < 0) {
               double tend = ((k-1) + zprev/(zprev-z))*getUpdateInterval();
               period += (tend-tstart);
               cnt++;
            }
            zprev = z;            
         }
         return 2*period/cnt;
      }

      public void apply (double t0) {
         super.apply (t0);
         if (t0 == myStopTime) {
            if (myEstimatePeriod) {
               double period = estimatePeriod();
               System.out.println ("period=" + period);
            }
         }
      }
      
      public PosProbe (
         Frame frame, double startTime, double stopTime, double interval) {
         super (frame, "position", startTime, stopTime, interval);
      }
   }      

   protected PosProbe addPosProbe (Frame frame) {
      
      PosProbe probe = new PosProbe (frame, 0, 10.0, 0.001);
      probe.setName ("Frame position");
      //probe.setDataFunction (new SegmentEnergy (mySpring, 0));
      addOutputProbe (probe);
      return probe;
   }

   protected int matchArg (String[] args, int i) {
      int nm = 1;

      if (args[i].equals ("-numKnots0")) {
         if (i == args.length-1) {
            System.out.println (
               "WARNING: -numKnots0 needs another argument");
         }
         else {
            myNumKnots0 = Integer.valueOf (args[++i]);
            nm++;
         }
      }
      else if (args[i].equals ("-numKnots1")) {
         if (i == args.length-1) {
            System.out.println (
               "WARNING: -numKnots1 needs another argument");
         }
         else {
            myNumKnots1 = Integer.valueOf (args[++i]);
            nm++;
         }
      }
      else if (args[i].equals ("-springDamping")) {
         if (i == args.length-1) {
            System.out.println (
               "WARNING: -springDamping needs another argument");
         }
         else {
            mySpringDamping = Double.valueOf (args[++i]);
            nm++;
         }
      }
      else if (args[i].equals ("-springStiffness")) {
         if (i == args.length-1) {
            System.out.println (
               "WARNING: -springStiffness needs another argument");
         }
         else {
            mySpringStiffness = Double.valueOf (args[++i]);
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

   public void build (String[] args) {
      myMech = new MechModel ("mechMod");

      myMech.setGravity (0, 0, -9.8);
      myMech.setFrameDamping (1.0);
      myMech.setRotaryDamping (0.1);

      myP0 = new Particle (0.1, myRightKnotX, -myGap/2, size / 2);
      myP0.setDynamic (false);
      myMech.addParticle (myP0);

      myP1 = new Particle (0.1, myLeftKnotX, -myGap/2, size / 2);
      myP1.setDynamic (false);
      myMech.addParticle (myP1);

      myP2 = new Particle (0.1, myLeftKnotX, myGap/2, size / 2);
      myP2.setDynamic (false);
      myMech.addParticle (myP2);

      myP3 = new Particle (0.1, myRightKnotX, myGap/2, size / 2);
      myP3.setDynamic (false);
      myMech.addParticle (myP3);

      mySpring = new MultiPointSpring (
         "spring", mySpringStiffness, mySpringDamping, 0);
      mySpring.addPoint (myP0);
      if (myNumKnots0 > 0) {
         mySpring.setSegmentWrappable (myNumKnots0);
      }
      mySpring.addPoint (myP1);
      mySpring.addPoint (myP2);
      if (myNumKnots1 > 0) {
         mySpring.setSegmentWrappable (myNumKnots1);
      }
      mySpring.addPoint (myP3);
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
      panel.addWidget (spr, "maxWrapDisplacement");
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

   protected void createControlPanel (MechModel mech) {

      RigidBody bod = (RigidBody)mech.findComponent ("rigidBodies/0");
      MultiPointSpring spr =
         (MultiPointSpring)mech.findComponent ("multiPointSprings/0");
      ControlPanel panel = createControlPanel (mech, bod, spr);
      addControlPanel (panel);
      Main.getMain().arrangeControlPanels (this);
   }
}
