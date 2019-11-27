package artisynth.demos.wrapping;

import java.awt.Color;

import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.MultiPointSpring;
import artisynth.core.mechmodels.MultiPointSpring.WrapKnot;
import artisynth.core.mechmodels.MultiPointSpring.WrapSegment;
import artisynth.core.mechmodels.Particle;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.RigidCylinder;
import artisynth.core.mechmodels.Wrappable;
import artisynth.core.modelbase.StepAdjustment;
import artisynth.core.probes.NumericMonitorProbe;
import artisynth.core.workspace.DriverInterface;
import artisynth.core.workspace.RootModel;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.matrix.Vector3i;
import maspack.render.RenderProps;
import maspack.render.Renderer;

/**
 * Model that uses a cylinder to test the errors in MultiPointSpring
 * wrapping that arise from knot discretization.
 * 
 * @author John E. Lloyd
 */
public class KnotDensityTest extends RootModel {

   //private Vector3d myParticleVel = new Vector3d(-2, 0, 2);
   private Vector3d myP1Vel = new Vector3d(0, 0.3, -0.4); // (0, 0.3, -0.4); 
   private Vector3d myP0Vel = new Vector3d(0, 0.3, 0.4); // (0, 0.3, 0.4);

   MechModel myMech;
   RigidBody myWrappable;
   MultiPointSpring mySpring;
   Particle myP0;
   Particle myP1;

   double mySize = 1.0;
   double myDensity = 1000;   
   double[] myLengths = new double[100];

   public void build (String[] args) {
      myMech = new MechModel ("mechMod");
      addModel (myMech);

      boolean sideOffset = true;
      boolean meshBased = false;
      int numKnots = 20;
      for (int i=0; i<args.length; i++) {
         if (args[i].equals ("-meshBased")) {
            meshBased = true;
         }
         else if (args[i].equals ("-numKnots")) {
            if (i == args.length-1) {
               System.out.println (
                  "Warning: option -numKnots needs an argument");
            }
            else {
               numKnots = Integer.valueOf(args[++i]);
            }
         }
         else if (args[i].equals ("-noOffset")) {
            sideOffset = false;
         }
         else {
            System.out.println ("Warning: unrecognized option " + args[i]);
         }
      }
      double n0 = 4.3;

      if (sideOffset) {
         myP0 = new Particle (0.1, mySize/2, -n0*mySize, -n0*mySize);
         myP1 = new Particle (0.1, -mySize/2, mySize, -mySize);
         myP0Vel = new Vector3d(0, 0.3, 0.4);
         myP1Vel = new Vector3d(0, 0.3, -0.4);
      }
      else {
         myP0 = new Particle (0.1, mySize/2, 0, -n0*mySize);
         myP1 = new Particle (0.1, -mySize/2, 0, -mySize);
         myP0Vel = new Vector3d(0, 0, 0.4);
         myP1Vel = new Vector3d(0, 0, -0.4);
      }

      myP0.setDynamic (false);
      myMech.addParticle (myP0);
      myP1.setDynamic (false);
      myMech.addParticle (myP1);

      mySpring = new MultiPointSpring ("spring", 1, 0.1, 0);
      mySpring.addPoint (myP0);
      mySpring.setSegmentWrappable (numKnots);
      mySpring.addPoint (myP1);      

      myMech.addMultiPointSpring (mySpring);

      mySpring.setDrawKnots (true);
      mySpring.setDrawABPoints (true);
      mySpring.setWrapDamping (10);
      mySpring.setMaxWrapIterations (200);
      mySpring.setContactingKnotsColor (Color.MAGENTA);
      mySpring.setContactStiffness (100);

      RenderProps.setPointStyle (myMech, Renderer.PointStyle.SPHERE);
      RenderProps.setPointRadius (myMech, mySize / 10);
      RenderProps.setLineStyle (myMech, Renderer.LineStyle.CYLINDER);
      RenderProps.setLineRadius (myMech, mySize / 30);
      RenderProps.setLineColor (myMech, Color.red);     

      PolygonalMesh cylinderMesh =
         MeshFactory.createCylinder (mySize/2, 5*mySize, 100);

      RigidBody cylinder;

      if (meshBased) {
         cylinder = new RigidBody ("cylinder", cylinderMesh,null, myDensity, 1.0);
      }
      else {
         cylinder = new RigidCylinder ("cylinder", mySize/2, 5*mySize, myDensity);
      }

      cylinder.setDistanceGridRes (new Vector3i(10, 10, 10));
      cylinder.setPose (new RigidTransform3d (0, 0, 0, 0, 0, Math.PI/2));
      mySpring.addWrappable ((Wrappable)cylinder);
      myMech.addRigidBody (cylinder);
      //cylinder.setDynamic (false);
      RenderProps.setAlpha (cylinder, 0.5);


      cylinder.setDynamic (false);
      myWrappable = cylinder;

      mySpring.initializeSegment (
         0, new Point3d[] {
            new Point3d (0.9*mySize/2, 0, mySize),
            new Point3d (0, 0, mySize),
            new Point3d (-0.9*mySize/2, 0, mySize),
         });
      mySpring.updateWrapSegments();

      RenderProps.setPointRadius (mySpring, 0.03);
      RenderProps.setLineRadius (mySpring, 0.015);
      mySpring.setDrawKnots (true);

      addControlPanel (TwoStrandWrapBase.createControlPanel (
                          myMech, myWrappable, mySpring));

      addPerformanceProbes();
      //addBreakPoint (0.45);
      //addBreakPoint (1.28);
      addBreakPoint (2.11);
      addBreakPoint (2.93);
      addBreakPoint (3.75);
      addBreakPoint (4.56);
      addBreakPoint (5.37);

   }

   public void attach (DriverInterface di) {
      getMainViewer().setOrthographicView(true);
      setViewerCenter (new Point3d(0, 0, 0));
      setViewerEye (new Point3d(0, -13, 0));
   }

   protected void addPerformanceProbes() {

      NumericMonitorProbe probe = 
         new NumericMonitorProbe (1, null, 0, 10.0, 0.01);
      probe.setName ("AB deflection");
      //probe.setDataFunction (new SegmentEnergy (mySpring, 0));
      probe.setDataFunction (
         new TwoStrandWrapBase.ABDeflection (mySpring, 0, 0, 1));
      addOutputProbe (probe);

//      probe = 
//         new NumericMonitorProbe (1, null, 0, 10.0, 0.01);
//      probe.setName ("Length");
//      probe.setDataFunction (
//         new TwoStrandWrapBase.SegmentLength (mySpring, 0));
//      addOutputProbe (probe);

//      probe = 
//         new NumericMonitorProbe (1, null, 0, 10.0, 0.01);
//      probe.setName ("Length diff");
//      probe.setDataFunction (
//         new TwoStrandWrapBase.SegmentLengthDiff (mySpring, 0));
//      addOutputProbe (probe);

      probe = 
         new NumericMonitorProbe (1, null, 0, 10.0, 0.01);
      probe.setName ("Energy");
      probe.setDataFunction (
         new TwoStrandWrapBase.SegmentEnergy (mySpring, 0));
      addOutputProbe (probe);

      probe = 
         new NumericMonitorProbe (1, null, 0, 10.0, 0.01);
      probe.setName ("Energy diff");
      probe.setDataFunction (
         new TwoStrandWrapBase.SegmentEnergyDiff (mySpring, 0));
      addOutputProbe (probe);

//      probe = 
//         new NumericMonitorProbe (1, null, 0, 10.0, 0.01);
//      probe.setName ("Spring energy diff");
//      probe.setDataFunction (
//         new TwoStrandWrapBase.SpringEnergyDiff (mySpring, 0));
//      addOutputProbe (probe);

//      probe = 
//         new NumericMonitorProbe (1, null, 0, 10.0, 0.01);
//      probe.setName ("Contact energy");
//      probe.setDataFunction (
//         new TwoStrandWrapBase.ContactEnergy (mySpring, 0));
      addOutputProbe (probe);

   }   

   public StepAdjustment advance (double t0, double t1, int flags) {
      double h = t1-t0;

      if (t0 > 0) {
         if (myP1Vel != null) {
            Point3d pos = new Point3d();
            myP1.getPosition (pos);
            pos.scaledAdd (h, myP1Vel);
            myP1.setPosition (pos);
         }
         if (myP0Vel != null) {
            Point3d pos = new Point3d();
            myP0.getPosition (pos);
            pos.scaledAdd (h, myP0Vel);
            myP0.setPosition (pos);
         }
      }
      StepAdjustment sa = super.advance (t0, t1, flags);
      WrapSegment seg = (WrapSegment)mySpring.getSegment(0);
      WrapKnot prev = null;
      int numk = seg.numKnots();
      System.out.println ("");
      for (int i=0; i<=numk; i++) {
         WrapKnot knot = i < numk ? seg.getKnot(i) : null;
         Point3d p1 = 
            new Point3d((knot != null ? knot.myPos : myP1.getPosition()));
         Point3d p0 = 
            new Point3d((prev != null ? prev.myPos : myP0.getPosition()));
         double len = p0.distance(p1);

         myLengths[i] = len;
         if (knot != null && knot.inContact()) {
            System.out.printf ("%8.5f len=%g",
                               knot.getContactDistance(), len);
         }
         else {
            System.out.printf ("         len=%g", len);
         }
         
         
         if (knot != null && knot.inContact()) {
            if (prev != null && prev.inContact()) {
               Vector3d xprod = new Vector3d();
               p0.y = 0;
               p1.y = 0;
               xprod.cross (p0, p1);
               double ang = Math.asin (xprod.norm()/(p0.norm()*p1.norm()));
               System.out.printf (" ang=%g", Math.toDegrees(ang));
            }
         }
         System.out.println ("");
         prev = knot;
      }
      double dlen = Math.abs(myLengths[0]-myLengths[numk]);
      double alen = Math.abs(myLengths[0]+myLengths[numk])/2;
      System.out.println ("ratio=" + dlen/alen);
      return sa;
   }


}


