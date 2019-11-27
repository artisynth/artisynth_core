package artisynth.demos.wrapping;

import java.awt.Color;

import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.MultiPointSpring;
import artisynth.core.mechmodels.MultiPointSpring.SubSegment;
import artisynth.core.mechmodels.MultiPointSpring.WrapKnot;
import artisynth.core.mechmodels.MultiPointSpring.WrapSegment;
import artisynth.core.mechmodels.Particle;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.RigidCylinder;
import artisynth.core.mechmodels.Wrappable;
import artisynth.core.modelbase.StepAdjustment;
import artisynth.core.probes.NumericMonitorProbe;
import artisynth.core.probes.NumericOutputProbe;
import artisynth.core.workspace.DriverInterface;
import artisynth.core.workspace.RootModel;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.matrix.Vector3i;
import maspack.properties.PropertyList;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.spatialmotion.Wrench;

/**
 * Model that uses a cylinder to test the errors in MultiPointSpring
 * wrapping that arise from knot discretization.
 * 
 * @author John E. Lloyd
 */
public class KnotDensityTest3 extends RootModel {

   //private Vector3d myParticleVel = new Vector3d(-2, 0, 2);
   private Vector3d myP1Vel = new Vector3d(0, 0.3, -0.4); // (0, 0.3, -0.4); 
   private Vector3d myP0Vel = new Vector3d(0, 0.3, 0.4); // (0, 0.3, 0.4);

   MechModel myMech;
   RigidBody myWrappable;
   MultiPointSpring mySpring;
   Particle myP0;
   Particle myP1;

   double myRadius = 0.5;
   double myDensity = 1000;   
   double RTOD = 180/Math.PI;
   double DTOR = Math.PI/180;

   double myExactL = 0;
   Wrench myExactForce = new Wrench();

   public static PropertyList myProps =
      new PropertyList (KnotDensityTest3.class, RootModel.class);

   static {
      myProps.addReadOnly ("lengthError", "error in length calculation");
      myProps.addReadOnly ("newLengthError", "error in new length calculation");
      myProps.addReadOnly ("force", "force on the wrappable");
      myProps.addReadOnly ("moment", "moment on the wrappable");
      myProps.addReadOnly ("forceError", "error in force calculation");
      myProps.addReadOnly ("momentError", "error in moment calculation");
      myProps.addReadOnly ("knotForce", "knot force on the wrappable");
      myProps.addReadOnly ("knotMoment", "knot moment on the wrappable");
      myProps.addReadOnly ("knotForceError", "error in knot force calculation");
      myProps.addReadOnly ("knotMomentError", "error in knot moment calculation");
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }  

   protected void applyKnotForce (
      Wrench wr, WrapSegment wseg, int k0, int k1, RigidTransform3d TBW) {

      Point3d p0, p1;
      Point3d pk = new Point3d();
      Vector3d u01 = new Vector3d();
      Vector3d force = new Vector3d();
      int numk = wseg.numKnots();

      WrapKnot knot = wseg.getKnot(k0);
      p0 = knot.myPos;
      if (k1 < 0) {
         p1 = wseg.myPntA.getPosition();
      }
      else if (k1 == numk) {
         p1 = wseg.myPntB.getPosition();
      }
      else {
         p1 = wseg.getKnot(k1).myPos;
      }
      u01.sub (p1, p0);
      u01.normalize();
      force.scale (mySpring.getLength(), u01);
      wr.f.add (force);
      
      // make force perpendicular to contact normal for computing moment
      Vector3d nrml = knot.getContactNormal();
      force.scaledAdd (-u01.dot(nrml), nrml, u01);
      force.normalize();
      force.scale (mySpring.getLength());


      pk.sub (p0, TBW.p);
      pk.scaledAdd (-knot.getContactDistance(), nrml);

      // System.out.println ("force=" + force);
      // System.out.println (" nrml=" + nrml);
      // System.out.println ("   pk=" + pk);
      Vector3d m = new Vector3d();
      m.cross (pk, force);
      //System.out.println ("m=" + m);

      wr.m.crossAdd (pk, force, wr.m);     
   }

   protected Wrench computeKnotForce() {
      WrapSegment wseg = (WrapSegment)mySpring.getSegment(0);
      RigidTransform3d TBW = myWrappable.getPose();
      int numk = wseg.numKnots();
      // for (int k=0; k<numk; k++) {
      //    WrapKnot knot = wseg.getKnot(k);
      //    if (knot.inContact()) {
      //       applyKnotForce (wr, wseg, k, k+1, TBW);            
      //       applyKnotForce (wr, wseg, k, k-1, TBW);            
      //    }
      // }   
      
      Wrench wchk = new Wrench();
      boolean computedB = false;
      for (int k=0; k<numk; k++) {
         WrapKnot knot = wseg.getKnot(k);
         if (knot.inContact() && !computedB) {
            applyKnotForce (wchk, wseg, k, k-1, TBW);
            computedB = true;
         }
         else if (!knot.inContact() && computedB) {
            applyKnotForce (wchk, wseg, k-1, k, TBW);
            break;
         }
      }
      return wchk;
   }

   public Vector3d getForce() {
      Wrench wr = new Wrench();
      myWrappable.getForce (wr);
      return wr.f;
   }

   public Vector3d getMoment() {
      Wrench wr = new Wrench();
      myWrappable.getForce (wr);
      return wr.m;
   }

   public double getForceError() {
      Wrench wr = new Wrench();
      myWrappable.getForce (wr);
      Wrench err = new Wrench();
      err.sub (myExactForce, wr);
      return err.f.norm()/myExactForce.f.norm();
   }

   public double getMomentError() {
      Wrench wr = new Wrench();
      myWrappable.getForce (wr);
      Wrench err = new Wrench();
      err.sub (myExactForce, wr);
      return err.m.norm()/myExactForce.m.norm();
   }

   public Vector3d getKnotForce() {
      Wrench wr = computeKnotForce();
      return wr.f;
   }

   public Vector3d getKnotMoment() {
      Wrench wr = computeKnotForce();
      return wr.m;
   }

   public double getKnotForceError() {
      Wrench wr = computeKnotForce();
      Wrench err = new Wrench();
      err.sub (myExactForce, wr);
      return err.f.norm()/myExactForce.f.norm();
   }

   public double getKnotMomentError() {
      Wrench wr = computeKnotForce();
      Wrench err = new Wrench();
      err.sub (myExactForce, wr);
      return err.m.norm()/myExactForce.m.norm();
   }

   public double getLengthError () {
      double l = mySpring.getLength();
      return (Math.abs(l-myExactL)/myExactL);
   }         

   public double computeNewLength () {
      WrapSegment wseg = (WrapSegment)mySpring.getSegment(0);
      if (wseg.hasSubSegments()) {
         SubSegment prevSeg = null;
         double len = 0;
         for (SubSegment subSeg=wseg.firstSubSegment();
              subSeg!=null; subSeg=subSeg.getNext()) {

            len += subSeg.myPntA.distance (subSeg.myPntB);
            if (prevSeg != null) {
               Point3d pos;
               Point3d lastPos = new Point3d();
               lastPos.set (prevSeg.myPntA.getPosition());
               Point3d surfacePos = new Point3d();
               for (int k=prevSeg.getKa(); k<=subSeg.getKb(); k++) {
                  WrapKnot knot = wseg.getKnot(k);
                  if (knot.myPos.z >= 0) {
                     if (knot.inContact()) {
                        surfacePos.scaledAdd (
                           -knot.getContactDistance(), knot.getContactNormal(),
                           knot.myPos);
                        pos = surfacePos;
                        pos = knot.myPos;
                     }
                     else {
                        pos = knot.myPos;
                     }
                     len += pos.distance (lastPos);
                     lastPos.set (pos);
                  }
               }
               len += lastPos.distance (subSeg.myPntB.getPosition());
            }
            prevSeg = subSeg;
         } 
         return len;
      }
      else {
         return wseg.myPntA.distance (wseg.myPntB);
      }
   }         

   public double computeNewLengthFull () {
      WrapSegment wseg = (WrapSegment)mySpring.getSegment(0);
      if (wseg.hasSubSegments()) {
         double len = 0;
         SubSegment prevSeg = null;
         for (SubSegment subSeg=wseg.firstSubSegment();
              subSeg!=null; subSeg=subSeg.getNext()) {
            len += subSeg.myPntA.distance (subSeg.myPntB);
            if (prevSeg != null) {
               Point3d pos;
               Point3d lastPos = new Point3d();
               lastPos.set (prevSeg.myPntA.getPosition());
               Point3d surfacePos = new Point3d();
               for (int k=prevSeg.getKa(); k<=subSeg.getKb(); k++) {
                  WrapKnot knot = wseg.getKnot(k);
                  if (knot.inContact()) {
                     surfacePos.scaledAdd (
                        -knot.getContactDistance(), knot.getContactNormal(),
                        knot.myPos);
                     pos = surfacePos;
                  }
                  else {
                     pos = knot.myPos;
                  }
                  len += pos.distance (lastPos);
                  lastPos.set (pos);
               }
               len += lastPos.distance (subSeg.myPntB.getPosition());
            }
            prevSeg = subSeg;
         } 
         return len;
      }
      else {
         return wseg.myPntA.distance (wseg.myPntB);
      }
   }         

   public double getNewLengthError () {
      double l = computeNewLength();
      return (Math.abs(l-myExactL)/myExactL);
   }         

   public void build (String[] args) {
      myMech = new MechModel ("mechMod");
      addModel (myMech);

      myMech.setGravity (0, 0, 0);

      boolean sideOffset = true;
      boolean meshBased = false;
      int numKnots = 60;
      double z0 = -6.5; // -4.5
      double z1 = -0.5; // -2.5
      numKnots = 60;
      z0 = -2.5*Math.PI*myRadius;
      z1 = -2.5*Math.PI*myRadius;
      double speed = 0.4;
      for (int i=0; i<args.length; i++) {
         if (args[i].equals ("-meshBased")) {
            meshBased = true;
         }
         else if (args[i].equals ("-numk")) {
            if (i == args.length-1) {
               System.out.println (
                  "Warning: option -numk needs an argument");
            }
            else {
               numKnots = Integer.valueOf(args[++i]);
            }
         }
         else if (args[i].equals ("-z0")) {
            if (i == args.length-1) {
               System.out.println (
                  "Warning: option -z0 needs an argument");
            }
            else {
               z0 = Double.valueOf(args[++i]);
            }
         }
         else if (args[i].equals ("-z1")) {
            if (i == args.length-1) {
               System.out.println (
                  "Warning: option -z1 needs an argument");
            }
            else {
               z1 = Double.valueOf(args[++i]);
            }
         }
         else if (args[i].equals ("-speed")) {
            if (i == args.length-1) {
               System.out.println (
                  "Warning: option -speed needs an argument");
            }
            else {
               speed = Double.valueOf(args[++i]);
            }
         }
         else if (args[i].equals ("-noOffset")) {
            sideOffset = false;
         }
         else {
            System.out.println ("Warning: unrecognized option " + args[i]);
         }
      }

      myExactL = Math.abs(z0) + Math.abs(z1) + Math.PI*myRadius;
      myExactForce.f.z = -2*myExactL;

      if (sideOffset) {
         myP0 = new Particle (0.1, myRadius, -Math.PI/2*myRadius+z0, z0);
         myP1 = new Particle (0.1, -myRadius, Math.PI/2*myRadius-z1, z1);
         myP0Vel = new Vector3d(0, speed, speed);
         myP1Vel = new Vector3d(0, speed, -speed);

         myExactForce.m.z = -2*myRadius*myExactL;
         myExactL *= Math.sqrt(2);
      }
      else {
         myP0 = new Particle (0.1, myRadius, 0, z0);
         myP1 = new Particle (0.1, -myRadius, 0, z1);
         myP0Vel = new Vector3d(0, 0, speed);
         myP1Vel = new Vector3d(0, 0, -speed);
      }

      System.out.println ("myExactL=" + myExactL);
      System.out.println ("myExactForce=" + myExactForce);

      myP0.setDynamic (false);
      myMech.addParticle (myP0);
      myP1.setDynamic (false);
      myMech.addParticle (myP1);

      mySpring = new MultiPointSpring ("spring", 1, 0.0, 0);
      mySpring.addPoint (myP0);
      mySpring.setSegmentWrappable (numKnots);
      mySpring.setConvergenceTol (1e-8);
      mySpring.addPoint (myP1);      

      myMech.addMultiPointSpring (mySpring);

      mySpring.setDrawKnots (true);
      mySpring.setDrawABPoints (true);
      mySpring.setWrapDamping (10);
      mySpring.setMaxWrapIterations (200);
      mySpring.setContactingKnotsColor (Color.MAGENTA);
      mySpring.setContactStiffness (10);
      mySpring.setABPointColor (Color.GREEN);

      RenderProps.setPointStyle (myMech, Renderer.PointStyle.SPHERE);
      RenderProps.setPointRadius (myMech, myRadius/5);
      RenderProps.setLineStyle (myMech, Renderer.LineStyle.CYLINDER);
      RenderProps.setLineRadius (myMech, myRadius/15);
      RenderProps.setLineColor (myMech, Color.red);     

      PolygonalMesh cylinderMesh =
         MeshFactory.createCylinder (myRadius, 10*myRadius, 100);

      RigidBody cylinder;

      if (meshBased) {
         cylinder = new RigidBody ("cylinder", cylinderMesh,null, myDensity, 1.0);
      }
      else {
         cylinder = new RigidCylinder ("cylinder", myRadius, 6*myRadius, myDensity, 100);
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
            new Point3d (0.9*myRadius, 0, 2*myRadius),
            new Point3d (0, 0, 2*myRadius),
            new Point3d (-0.9*myRadius, 0, 2*myRadius),
         });
      mySpring.updateWrapSegments();

      RenderProps.setPointRadius (mySpring, 0.03);
      RenderProps.setLineRadius (mySpring, 0.015);
      mySpring.setDrawKnots (true);

      addControlPanel (TwoStrandWrapBase.createControlPanel (
                          myMech, myWrappable, mySpring));

      addDeflectionProbes();
      //addBreakPoint (4.00);
      
      addErrorProbes();

   }

   public void addDeflectionProbes() {
      NumericMonitorProbe probe;
      probe = new NumericMonitorProbe (1, null, 0, 16.0, 0.01);
      probe.setName ("AB deflection 0");
      //probe.setDataFunction (new SegmentEnergy (mySpring, 0));
      probe.setDataFunction (
         new TwoStrandWrapBase.ABDeflection (mySpring, 0, 0, 1));
      addOutputProbe (probe);

      probe = new NumericMonitorProbe (1, null, 0, 16.0, 0.01);
      probe.setName ("AB deflection 1");
      //probe.setDataFunction (new SegmentEnergy (mySpring, 0));
      probe.setDataFunction (
         new TwoStrandWrapBase.ABDeflection (mySpring, 0, 1, 1));
      addOutputProbe (probe);
   }

   protected void addErrorProbe (String name) {
      NumericOutputProbe probe = new NumericOutputProbe (this, name, 0, 16.0, -1);
      probe.setName (name);
      addOutputProbe (probe);
   }

   public void addErrorProbes() {
      addErrorProbe ("lengthError");
      addErrorProbe ("newLengthError");
      addErrorProbe ("force");
      addErrorProbe ("forceError");
      addErrorProbe ("moment");
      addErrorProbe ("momentError");
      addErrorProbe ("knotForce");
      addErrorProbe ("knotForceError");
      addErrorProbe ("knotMoment");
      addErrorProbe ("knotMomentError");
   }

   public void attach (DriverInterface di) {
      getMainViewer().setOrthographicView(true);
      setViewerCenter (new Point3d(0, 0, 0));
      setViewerEye (new Point3d(0, -13, 0));
   }

   double contactAngle (WrapSegment seg) {
      int numk = seg.numKnots();
      WrapKnot prev = null;
      double avgAng = 0;
      int avgCnt = 0;
      for (int i=0; i<numk; i++) {
         WrapKnot knot = seg.getKnot(i);
         if (knot.inContact() && prev != null && prev.inContact()) {
            Vector3d xprod = new Vector3d();
            Point3d p0 = new Point3d(prev.myPos);
            Point3d p1 = new Point3d(knot.myPos);
            p0.y = 0;
            p1.y = 0;
            xprod.cross (p0, p1);
            double ang = Math.asin (xprod.norm()/(p0.norm()*p1.norm()));
            avgAng += ang;
            avgCnt++;
         }
         prev = knot;
      }
      return avgAng/avgCnt;
   }

   double yprev;
   double rprev;
   double dprev;
   int turncnt = 0;
   double ystart;
   double rstart;
   double maxdev;
   double maxang;
   double maxr0;
   double maxr1;

   private double getContactForce (WrapSegment seg, int k) {
      WrapKnot knot = seg.getKnot(k);
      return -knot.getContactNormal().z*knot.getContactDistance()*10;
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
      double y = seg.getABPoint(0).getPosition().y;
      Point3d p0 = myP0.getPosition();
      Point3d p1 = seg.getKnot(0).myPos;
      double r = (p1.z-p0.z)/(p1.y-p0.y);

      int kq = -1;
      int kp = -1;
      // find p and q
      for (int k=0; k<seg.numKnots(); k++) {
         WrapKnot knot = seg.getKnot(k);
         if (knot.inContact() && kq == -1) {
            kq = k-1;
         }
         else if (!knot.inContact() && kq != -1 && kp == -1) {
            kp = k;
         }
      }
      System.out.println ("kq=" + kq + " kp=" + kp);
      for (int k=kq+1; k<kp; k++) {
         Point3d p = seg.getKnot(k).myPos;
         double ang0 = Math.atan2 (p.z, p.x);
         p = seg.getKnot(k+1).myPos;
         double ang1 = Math.atan2 (p.z, p.x);
         double theta = Math.toDegrees (ang1-ang0);
         double d = seg.getKnot(k).getContactDistance();
         if (theta < 0) {
            theta += 360.0;
         }
         if (k<kp-1) {
            System.out.printf (
               "  theta=%g dist=%8.5f d=%g\n",
               theta, d, 2*myRadius*Math.sin(DTOR*theta/2));
         }
         else {
            System.out.printf ("                d=%g\n", d);
         }
      }
      
      double abErrQ = seg.getABPoint(0).getPosition().y+myRadius*Math.PI/2;
      double abErrP = seg.getABPoint(1).getPosition().y-myRadius*Math.PI/2;
      
      int numk = seg.numKnots();
      double dq = (seg.getKnot(kq).myPos.z - myP0.getPosition().z)/(kq+1);
      double dp = (seg.getKnot(kp).myPos.z - myP1.getPosition().z)/(numk-kp);
      double c = 0;
      if (dq > dp) {
         c = 2*Math.toDegrees(Math.acos(dp/dq));
      }
      else {
         c = 2*Math.toDegrees(Math.acos(dq/dp));
      }
      WrapKnot knotq = seg.getKnot(kq+1);
      WrapKnot knotp = seg.getKnot(kp-1);
      System.out.printf ("  cfq0=%g\n", getContactForce(seg,kq));
      System.out.printf ("  cfq1=%g\n", getContactForce(seg,kq+1));
      System.out.printf ("  cfp1=%g\n", getContactForce(seg,kp-1));
      System.out.printf ("  cfp0=%g\n", getContactForce(seg,kp));
      System.out.printf (
         "dq=%g dp=%g c=%g  aberrq=%g aberrp=%g\n", dq, dp, c, abErrQ, abErrP);

      if (t0 < 0.02) {
         dprev = y-yprev;
         yprev = y;
         rprev = r;
         turncnt = 0;
         maxdev = 0;
      }
      else {
         double d = y-yprev;
         if (dprev*d < 0) {
            if (turncnt++ > 1) {
               double dev = Math.abs(yprev-ystart);
               if (dev > maxdev) {
                  maxdev = dev;
                  maxang = Math.toDegrees(contactAngle(seg));
                  maxr0 = rstart;
                  maxr1 = rprev;
               }
            }
            ystart = yprev;
            rstart = rprev;
         }
         dprev = d;
         yprev = y;
         rprev = r;
      }
      if (t1 == 4.0) {
         System.out.printf (
            "%3d maxdev=%7.5f ang=%6.3f r0=%7.5f r1=%7.5f\n",
            seg.numKnots(), maxdev, maxang, maxr0, maxr1);
      }
      return sa;
   }


}
