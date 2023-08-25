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
import maspack.render.*;
import maspack.render.Renderer.*;
import maspack.properties.*;

public class EquilibriumMuscleTest extends RootModel {

   private double DTOR = Math.PI/180;

   public static boolean omitFromMenu = true;
   Particle myPE0;
   Particle myPE1;
   Particle myPF0;
   Particle myPF1;
   Particle myPM;
   Muscle myMus0;
   Muscle myMus1;

   private boolean myUseForce = true;
   private boolean myUseMillard = true;
   private double myForceTime = 2;
   private double myMaxAppliedForce = 10;
   private double mySprK = 50;
   private double mySprL = 0.2;
   private double myPointDamping = 10.0;

   private double myLength0;
   
   private double myPennationAngleDeg = 20.0;
   private double myFibreDamping = 0; //0.1;
   private double myMaxIsoForce = 10.0;
   private boolean myIgnoreForceVel = false;
   private boolean myComputeLmDotFromLDot = false;
   private double myTendonSlackLength = 0.5;
   private double myOptFibreLength = 0.5;

   private double myExcitation = 0;
   private double mySpringPos = 0;

   public class LengthController extends ControllerBase {

      public void apply (double t0, double t1) {
         double speed = myUseMillard ? 1.0 : 0.8;
         double xvel = speed;
         double h = t1-t0;
         if (t1 <= 0.75) {
            xvel = speed;
         }
         else if (t1 <= 1.5) {
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
         "whether to compute lmdot from ldot in equilibirbrium muscle", false);
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
            System.out.println (
               "Options are '-force', '-length', '-millard', '-thelen'");
         }
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
      AxialSpring lig = new AxialSpring();

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
      mat0.setOptFibreLength(0.5);
      mat0.setRigidTendon (true);
      mat0.setFibreDamping (myFibreDamping);
      mat0.setIgnoreForceVelocity (myIgnoreForceVel);
      mat0.setTendonSlackLength(0.0);
      mat0.setMaxIsoForce(10.0);
      myMus0.setMaterial (mat0);
      tmat.setMaxIsoForce(10.0);
      tmat.setTendonSlackLength(0.5);
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
      mat1.setOptFibreLength(0.5);
      mat1.setFibreDamping (myFibreDamping);
      mat1.setIgnoreForceVelocity (myIgnoreForceVel);
      mat1.setTendonSlackLength(0.5);
      mat1.setMaxIsoForce(10.0);
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
         new NumericInputProbe(this, "excitation", 0, 6);
      probe.addData (
         new double[] { 0, 0,  1, 1,  5, 1,  6, 0 },
         NumericInputProbe.EXPLICIT_TIME);
      addInputProbe (probe);
      return probe;
   }

   NumericInputProbe addSpringProbe () {
      NumericInputProbe probe =
         new NumericInputProbe(this, "springPos", 0, 6);
      double x0 = getSpringPos();
      double delx = myUseMillard ? 0.8 : 0.6;
      probe.addData (
         new double[] { 0,x0, 1,x0, 1.5,1, 3,x0+delx, 4.5,1, 5,x0, 6,x0 },
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
         t1 = 6;
      }
      else {
         t1 = 2;
      }
      addOutputProbe ("force 0", "force0", t1);
      addOutputProbe ("force 1", "force1", t1);
      addOutputProbe ("muscle length 0", "muscleLen0", t1);
      addOutputProbe ("muscle length 1", "muscleLen1", t1);
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

   public StepAdjustment advance (double t0, double t1, int flags) {
      if (t0 == 0) {
         setExcitation (getExcitation());
      }
      return super.advance (t0, t1, flags);
   }

}
