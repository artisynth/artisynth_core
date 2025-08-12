package artisynth.demos.test;

import java.awt.Color;
import java.io.*;
import java.util.*;
import artisynth.core.workspace.*;
import artisynth.core.mechmodels.*;
import artisynth.core.modelbase.*;
import artisynth.core.femmodels.*;


import artisynth.core.materials.*;
import artisynth.core.probes.*;
import artisynth.core.gui.*;

import maspack.util.*;
import maspack.matrix.*;
import maspack.geometry.*;
import maspack.interpolation.*;
import maspack.render.*;
import maspack.render.Renderer.*;
import maspack.properties.*;

public class EquilibriumMuscleTest extends RootModel {

   private double INF = Double.POSITIVE_INFINITY;
   private double DTOR = Math.PI/180;

   public static boolean omitFromMenu = true;
   Particle myPE0;
   Particle myPE1;
   Particle myPF0;
   Particle myPF1;
   Particle myPM;
   Muscle myMus0;
   Muscle myMus1;

   double myRunTime = -1;

   private boolean myUseForce = true;
   private boolean myUseMillard = true;
   private double myForceTime = 2;
   private double myMaxAppliedForce = 10;
   private double mySprK = 50;
   private double mySprL = 0.2;
   private double myPointDamping = 10.0;

   private double myLength0;
   
   private double myPennationAngleDeg = 20.0;
   private double myFibreDamping = 0.0;
   private double myMaxIsoForce = 10.0;
   private boolean myIgnoreForceVel = false;
   private boolean myComputeLmDotFromLDot = true;
   private double myTendonSlackLength = 0.5;
   private double myOptFibreLength = 0.5;

   private double myExcitation = 1;
   private double mySpringPos = 0;

   public class LengthController extends ControllerBase {

      public void apply (double t0, double t1) {
         double speed = myUseMillard ? 1.0 : 0.8;
         speed *= 2/myRunTime;
         double xvel = speed;
         double h = t1-t0;
         double tmid = 0.75/2*myRunTime;
         double tmax = 2*tmid;
         if (t1 <= tmid) {
            xvel = speed;
         }
         else if (t1 <= tmax) {
            xvel = -speed;
         }
         else {
            xvel = 0;
         }
         setLength (getLength() + xvel*h);
         myPE1.setVelocity (new Vector3d (xvel, 0, 0));
      }
   }

   public static PropertyList myProps =
      new PropertyList (EquilibriumMuscleTest.class, RootModel.class);

   static {
      myProps.add (
         "pennationAngle", "penneation angle for the muscles", 0);
      myProps.add (
         "fibreDamping", "fibre damping", 0);
      myProps.add (
         "ignoreForceVelocity", "ignore the force velocity curve", true);
      myProps.add (
         "excitation", "muscle excitation", 1.0);
      myProps.add (
         "springPos", "x position of spring control points", 0);
      myProps.add (
         "computeLmDotFromLDot",
         "whether to compute lmdot from ldot in equilibirbrium muscle", true);
      myProps.add ("length", "overall length of the spring", 0);
      myProps.addReadOnly ("force0", "force for first muscle");
      myProps.addReadOnly ("force1", "force for second muscle");
      myProps.addReadOnly ("muscleLen0", "muscle length for first muscle");
      myProps.addReadOnly ("muscleLen1", "muscle length for second muscle");
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   private double sqr (double x) {
      return x*x;
   }

   public void build (String[] args) {
      MechModel mech = new MechModel ("mech");
      addModel (mech);
      
      for (int i=0; i<args.length; i++) {
         if (args[i].equals ("-force")) {
            myUseForce = true;
         }
         else if (args[i].equals ("-runTime")) {
            if (++i == args.length) {
               System.out.println (
                  "ERROR: Option -runTime needs another argument");
            }
            myRunTime = Double.valueOf(args[i]);
         }
         else if (args[i].equals ("-penAngle")) {
            if (++i == args.length) {
               System.out.println (
                  "ERROR: Option -penAngle needs another argument");
            }
            myPennationAngleDeg = Double.valueOf(args[i]);
         }
         else if (args[i].equals ("-tendonRatio")) {
            if (++i == args.length) {
               System.out.println (
                  "ERROR: Option -tendonRatio needs another argument");
            }
            double ratio = Double.valueOf(args[i]);
            myTendonSlackLength = ratio;
            myOptFibreLength = 1-ratio;
         }
         else if (args[i].equals ("-stepSize")) {
            if (++i == args.length) {
               System.out.println (
                  "ERROR: Option -stepSize needs another argument");
            }
            double stepSize = Double.valueOf(args[i]);
            setMaxStepSize (stepSize);
         }
         else if (args[i].equals ("-damping")) {
            if (++i == args.length) {
               System.out.println (
                  "ERROR: Option -damping needs another argument");
            }
            myFibreDamping = Double.valueOf(args[i]);
         }
         else if (args[i].equals ("-length")) {
            myUseForce = false;
         }
         else if (args[i].equals ("-millard")) {
            myUseMillard = true;
         }
         else if (args[i].equals ("-thelen")) {
            myUseMillard = false;
         }
         else {
            System.out.println (
               "WARNING: unrecognized option '"+args[i]+"'");
            System.out.println ("Options are:");
            System.out.println (" -force");
            System.out.println (" -length");
            System.out.println (" -millard");
            System.out.println (" -thelen");
            System.out.println (" -runTime <time>");
            System.out.println (" -tendonRatio <ratio>");
            System.out.println (" -penAngle <degrees>");
            System.out.println (" -damping <d>");
            System.out.println (" -stepSize <sec>");
         }
      }
      if (myRunTime == -1) {
         myRunTime = myUseForce ? 6 : 2;
      }

      mech.setGravity (0, 0, 0);
      //double lm0 = 0.28819722; // for ang = 0
      //double lm0 = 0.4698463103; // for excitation = 0
      double lm0;
      if (myUseForce) {
         lm0 = Math.cos(DTOR*myPennationAngleDeg)*myOptFibreLength;
      }
      else {
         double H = Math.sin(DTOR*myPennationAngleDeg)*myOptFibreLength;
         lm0 = Math.sqrt(sqr(0.6*myOptFibreLength) - H*H);
      }
      myLength0 = lm0 + myTendonSlackLength;
      //double lm0 = 0.28380107; // for ang = 20

      // build first muscle

      Particle p0 = new Particle (1.0, 0.0, 0, 0);
      p0.setDynamic (false);
      mech.addParticle (p0);

      myPM = new Particle ("pm", 1e-5, lm0, 0, 0);
      mech.addParticle (myPM);

      myPE0 = new Particle ("pe0", 1.0, myLength0, 0, 0);
      if (!myUseForce) {
         myPE0.setDynamic (false);
      }
      mech.addParticle (myPE0);

      myMus0 = new Muscle("mus0");
      Muscle lig = new Muscle();

      EquilibriumAxialMuscle mat0;
      EquilibriumAxialMuscle mat1;
      AxialTendonBase tmat;
      if (myUseMillard) {
         mat0 = new Millard2012AxialMuscle();
         mat1 = new Millard2012AxialMuscle();
         tmat = new Millard2012AxialTendon();
      }
      else {
         mat0 = new Thelen2003AxialMuscle();
         mat1 = new Thelen2003AxialMuscle();
         tmat = new Thelen2003AxialTendon();
      }

      mat0.setOptPennationAngle (Math.toRadians(myPennationAngleDeg));
      mat0.setOptFibreLength (myOptFibreLength);
      mat0.setRigidTendon (true);
      mat0.setFibreDamping (myFibreDamping);
      mat0.setIgnoreForceVelocity (myIgnoreForceVel);
      mat0.setTendonSlackLength(0.0);
      mat0.setMaxIsoForce(10.0);
      myMus0.setMaterial (mat0);
      tmat.setMaxIsoForce(10.0);
      tmat.setTendonSlackLength(myTendonSlackLength);
      lig.setMaterial (tmat);

      mech.attachAxialSpring (p0, myPM, myMus0);
      mech.attachAxialSpring (myPM, myPE0, lig);

      // build second muscle

      Particle p1 = new Particle (1.0, 0, 0, -0.5);
      p1.setDynamic (false);
      mech.addParticle (p1);

      myPE1 = new Particle ("pe1", 1.0, myLength0, 0, -0.5);
      if (!myUseForce) {
         myPE1.setDynamic (false);
      }
      mech.addParticle (myPE1);

      myMus1 = new Muscle("mus1");

      if (myUseForce) {
         myPF0 = new Particle ("pf0", 1.0, 0, 0, 0);
         mech.addParticle (myPF0);
         myPF0.setDynamic (false);
         myPE0.setPointDamping (myPointDamping);
         AxialSpring spr0 = new AxialSpring (mySprK, 0, mySprL);
         RenderProps.setSpindleLines (spr0, 0.02, Color.GREEN);
         mech.attachAxialSpring (myPE0, myPF0, spr0);
         myPF1 = new Particle ("pf1", 1.0, 0, 0, -0.5);
         mech.addParticle (myPF1);
         myPF1.setDynamic (false);
         myPE1.setPointDamping (myPointDamping);
         AxialSpring spr1 = new AxialSpring (mySprK, 0, mySprL);
         RenderProps.setSpindleLines (spr1, 0.02, Color.GREEN);
         mech.attachAxialSpring (myPE1, myPF1, spr1);
         setSpringPos (myLength0+mySprL);
      }
      
      mat1.setOptPennationAngle (Math.toRadians(myPennationAngleDeg));
      mat1.setOptFibreLength(myOptFibreLength);
      mat1.setFibreDamping (myFibreDamping);
      mat1.setIgnoreForceVelocity (myIgnoreForceVel);
      mat1.setTendonSlackLength(myTendonSlackLength);
      mat1.setMaxIsoForce(10.0);
      mat1.setComputeLmDotFromLDot (myComputeLmDotFromLDot);
      //mat1.setMuscleLength (lm0);
      myMus1.setMaterial (mat1);

      mech.attachAxialSpring (p1, myPE1, myMus1);

      // render properties
      
      RenderProps.setSphericalPoints (mech, 0.03, Color.WHITE);
      RenderProps.setSpindleLines (mech, 0.02, Color.RED);
      RenderProps.setLineColor (lig, Color.BLUE);
      RenderProps.setPointColor (myPM, Color.CYAN);

      if (myUseForce) {
         setExcitation (0.0);
      }
      else {
         setExcitation (1.0);
      }

      initializeMuscleLength (myLength0);

      // control panel

      ControlPanel panel = new ControlPanel();
      panel.addWidget (this, "pennationAngle");
      panel.addWidget (this, "ignoreForceVelocity");
      panel.addWidget (this, "fibreDamping");
      panel.addWidget (this, "excitation");
      panel.addWidget (this, "springPos");
      panel.addWidget (this, "length");
      panel.addWidget (this, "computeLmDotFromLDot");

      addControlPanel (panel);

      if (!myUseForce) {
         addController (new LengthController());
      }
      addProbes();
      //addBreakPoint (1.5);
   }

   public void postscanInitialize() {
      initRefsIfNecessary();
   }

   void initRefsIfNecessary() {
      if (myPE0 == null) {
         MechModel mech = (MechModel)findComponent("models/mech");
         myPE0 = mech.particles().get("pe0");
         myPE1 = mech.particles().get("pe1");
         myPF0 = mech.particles().get("pf0");
         myPF1 = mech.particles().get("pf1");
         myPM = mech.particles().get("pm");
         myMus0 = (Muscle)mech.axialSprings().get("mus0");
         myMus1 = (Muscle)mech.axialSprings().get("mus1");
      }
   }

   NumericInputProbe addExcitationProbe () {
      NumericInputProbe probe =
         new NumericInputProbe(this, "excitation", 0, myRunTime);
      probe.setName ("excitation");
      double te = myRunTime;
      probe.addData (
         new double[] { 0, 0,  te/6, 1,  te*5/6, 1,  te, 0 },
         NumericInputProbe.EXPLICIT_TIME);
      addInputProbe (probe);
      return probe;
   }

   NumericInputProbe addSpringProbe () {
      NumericInputProbe probe =
         new NumericInputProbe(this, "springPos", 0, myRunTime);
      probe.setName ("spring position");
      double x0 = getSpringPos();
      double delx = myUseMillard ? 0.8 : 0.6;
      double te = myRunTime;
      probe.addData (
         new double[] { 0,x0, te/6,x0, te*1.5/6,1, te/2,x0+delx,
                        3*te/4,1, 5*te/6,x0, te,x0 },
         NumericInputProbe.EXPLICIT_TIME);
      addInputProbe (probe);
      return probe;
   }

   void addOutputProbe (String name, String propName, double stopTime) {
      try {
         NumericOutputProbe probe =
            new NumericOutputProbe (this, propName, 0, stopTime, -1);
         probe.setName (name);
         addOutputProbe (probe);
      }
      catch (Exception e) {
         e.printStackTrace();
      }
   }

   void addProbes() {
      double t1;
      if (myUseForce) {
         addExcitationProbe();
         addSpringProbe();
      }
      addOutputProbe ("force 0", "force0", myRunTime);
      addOutputProbe ("force 1", "force1", myRunTime);
      addOutputProbe ("muscle length 0", "muscleLen0", myRunTime);
      addOutputProbe ("muscle length 1", "muscleLen1", myRunTime);

      Property prop0 = myMus0.getProperty ("forceNorm");
      Property prop1 = myMus1.getProperty ("forceNorm");

      NumericOutputProbe probe =
         new NumericOutputProbe (new Property[] { prop0, prop1 }, -1);
      probe.setStopTime (myRunTime);
      probe.setName ("computed forces");
      addOutputProbe (probe);
   }
   
   public void setIgnoreForceVelocity (boolean enable) {
      if (enable != myIgnoreForceVel) {
         myIgnoreForceVel = enable;

         EquilibriumAxialMuscle mat0 =
            (EquilibriumAxialMuscle)myMus0.getMaterial();
         EquilibriumAxialMuscle mat1 =
            (EquilibriumAxialMuscle)myMus1.getMaterial();
         mat0.setIgnoreForceVelocity (enable);
         mat1.setIgnoreForceVelocity (enable);
      }
   }

   public boolean getIgnoreForceVelocity() {
      return myIgnoreForceVel;
   }

   public void setExcitation (double e) {
      myExcitation = e;
      if (myMus0 != null) {
         myMus0.setExcitation (e);
         myMus1.setExcitation (e);
      }
   }

   public double getExcitation() {
      return myExcitation;
   }

   public void setSpringPos (double x) {
      if (x != mySpringPos) {
         mySpringPos = x;
         if (myPF0 != null) {
            myPF0.setPosition (new Point3d(x, 0, 0));
            myPF1.setPosition (new Point3d(x, 0, -0.5));
         }
      }
   }

   public double getSpringPos() {
      return mySpringPos;
   }

   public boolean getComputeLmDotFromLDot() {
      return myComputeLmDotFromLDot;
   }

   public void setComputeLmDotFromLDot (boolean enable) {
      if (enable != myComputeLmDotFromLDot) {
         myComputeLmDotFromLDot = enable;

         EquilibriumAxialMuscle mat1 =
            (EquilibriumAxialMuscle)myMus1.getMaterial();
         mat1.setComputeLmDotFromLDot (enable);
      }
   }

   public void setPennationAngle (double deg) {
      if (deg != myPennationAngleDeg) {
         myPennationAngleDeg = deg;

         if (myMus0 != null) {
            double ang = Math.toRadians(deg);

            EquilibriumAxialMuscle mat0 =
               (EquilibriumAxialMuscle)myMus0.getMaterial();
            EquilibriumAxialMuscle mat1 =
               (EquilibriumAxialMuscle)myMus1.getMaterial();
            mat0.setOptPennationAngle (ang);
            mat1.setOptPennationAngle (ang);
         }
      }
   }

   void initializeMuscleLength (double l) {
      double lm = 0;

      EquilibriumAxialMuscle mat1 =
         (EquilibriumAxialMuscle)myMus1.getMaterial();
      lm = mat1.computeLmWithConstantVm (l, 0, getExcitation());
      mat1.setMuscleLength (lm);         

      myPM.setPosition (new Point3d (lm, 0, 0));
   }

   public double getPennationAngle () {
      return myPennationAngleDeg;
   }

   public void setFibreDamping (double d) {
      if (myFibreDamping != d) {
         myFibreDamping = d;

         EquilibriumAxialMuscle mat0 =
            (EquilibriumAxialMuscle)myMus0.getMaterial();
         EquilibriumAxialMuscle mat1 =
            (EquilibriumAxialMuscle)myMus1.getMaterial();
         mat0.setFibreDamping (d);
         mat1.setFibreDamping (d);
      }
   }

   public double getFibreDamping () {
      return myFibreDamping;
   }

   public void setXforce (double fx) {
      if (myPE0 != null) {
         myPE0.setExternalForce (new Vector3d (fx, 0, 0));
      }
   }

   public double getXforce () {
      if (myPE0 != null) {
         return myPE0.getExternalForce().x;
      }
      else {
         return 0;
      }
   }

   public double getForce0() {
      return myMus0 != null ? myMus0.getForceNorm() : 0;
   }

   public double getForce1() {
      return myMus1 != null ? myMus1.getForceNorm() : 0;
   }

   public double getMuscleLen0() {
      return myPM != null ? myPM.getPosition().x : 0;
   }

   public double getMuscleLen1() {
      if (myMus1 != null) {
         AxialMaterial mat = myMus1.getMaterial();
         return ((EquilibriumAxialMuscle)mat).getMuscleLength();
      }
      else {
         return 0;
      }
   }

   public double getLength() {
      return myPE0 != null ? myPE0.getPosition().x : 0;
   }

   public void setLength (double l) {
      if (l != getLength() && myPE0 != null) {
         myPE0.setPosition (new Point3d (l, 0, 0));
         myPE1.setPosition (new Point3d (l, 0, -0.5));
      }
   }

   public void render (Renderer r, int flags) {
      super.render (r, flags);
      double len = getMuscleLen1();
      if (len != 0) {
         Point3d pe1 = new Point3d (len, 0, -0.5);
         r.setColor (Color.CYAN);
         r.drawSphere (pe1, 0.03);
      }
   }

   public double[] computeRelError() {
      NumericOutputProbe force0 =
         (NumericOutputProbe)getOutputProbes().get("force 0");
      NumericOutputProbe force1 =
         (NumericOutputProbe)getOutputProbes().get("force 1");
      NumericList nlist0 = force0.getNumericList();
      NumericList nlist1 = force1.getNumericList();
      VectorNd refvals = new VectorNd(1);
      double maxf = nlist0.getMaxValues().get(0);
      double maxerr = 0;
      double avgerr = 0;
      for (int k=0; k<nlist1.getNumKnots(); k++) {
         NumericListKnot knot = nlist1.getKnot(k);
         nlist0.interpolate(refvals, knot.t);
         double fref = refvals.get(0);
         double f = knot.v.get(0);
         double relerr = Math.abs((f-fref)/maxf);
         if (relerr > maxerr) {
            maxerr = relerr;
         }
         avgerr += relerr;
      }
      avgerr /= nlist1.getNumKnots();
      return new double[] { maxerr, avgerr };
   }

   double myVnMin, myVnMax;

   public StepAdjustment advance (double t0, double t1, int flags) {
      if (t0 == 0) {
         myVnMin = INF;
         myVnMax = -INF;
         setExcitation (getExcitation());
      }
      StepAdjustment sa = super.advance (t0, t1, flags);
      EquilibriumAxialMuscle mat1 =
         (EquilibriumAxialMuscle)myMus1.getMaterial();

      double vn = mat1.getNormalizedFibreVelocity();
      if (vn < myVnMin) {
         myVnMin = vn;
      }
      if (vn > myVnMax) {
         myVnMax = vn;
      }
      if (t1 == myRunTime) {
         double maxAvgErr[] = computeRelError();
         System.out.printf (
            "RES: tratio=%4.2f ang=%04.1f time=%3.1f %s step=%5.3f "+
            "vnrange=[ %6.3f %5.3f ] maxerr: %7.5f avgerr: %7.5f\n",
            myTendonSlackLength, myPennationAngleDeg, myRunTime, 
            (myUseMillard ? "Millard" : "Thelen_"),
            getMaxStepSize(),
            myVnMin, myVnMax,
            maxAvgErr[0], maxAvgErr[1]);
      }
      return sa;
   }

}
