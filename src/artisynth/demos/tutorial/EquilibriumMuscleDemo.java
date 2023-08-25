package artisynth.demos.tutorial;

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

public class EquilibriumMuscleDemo extends RootModel {

   private double DTOR = Math.PI/180;

   Particle myPE0;
   Particle myPE1;
   Particle myPM;
   Muscle myMus0;
   AxialSpring myTendon0;
   Muscle myMus1;

   private double myLength0;
   
   private double myOptPennationAng = DTOR*20.0;
   private double myMaxIsoForce = 10.0;
   private double myTendonSlackLen = 0.5;
   private double myOptFibreLen = 0.5;

   Millard2012AxialMuscle createMuscleMat() {
      return new Millard2012AxialMuscle (
         myMaxIsoForce, myOptFibreLen, myTendonSlackLen, myOptPennationAng);
   }

   public class LengthController extends ControllerBase {
      
      double myRunTime = 1.5;
      double mySpeed = 1.0;

      public LengthController() {
      }

      public void apply (double t0, double t1) {
         double xlen = 0;
         double xvel = 0;
         if (t1 <= myRunTime/2) {
            xlen = mySpeed*t0;
            xvel = mySpeed;
         }
         else if (t1 <= myRunTime) {
            xlen = mySpeed*(2*myRunTime/2 - t1);
            xvel = -mySpeed;
         }
         myPE0.setPosition (new Point3d (xlen, 0, 0));
         myPE1.setPosition (new Point3d (xlen, 0, -0.5));
         myPE1.setVelocity (new Vector3d (xvel, 0, 0));
      }
   }

   private double sqr (double x) {
      return x*x;
   }

   public void build (String[] args) {
      MechModel mech = new MechModel ("mech");
      addModel (mech);

      mech.setGravity (0, 0, 0);
      double H = Math.sin(myOptPennationAng)*myOptFibreLen;
      double lm0 = Math.sqrt(sqr(0.6*myOptFibreLen) - H*H);

      myLength0 = lm0 + myTendonSlackLen;

      // build first muscle

      Particle p0 = new Particle (1.0, 0.0, 0, 0);
      p0.setDynamic (false);
      mech.addParticle (p0);

      myPM = new Particle ("pm", 1e-5, lm0, 0, 0);
      mech.addParticle (myPM);

      myPE0 = new Particle ("pe0", 1.0, myLength0, 0, 0);
      myPE0.setDynamic (false);
      mech.addParticle (myPE0);

      myMus0 = new Muscle("mus0");
      myTendon0 = new AxialSpring();

      EquilibriumAxialMuscle mat0;
      EquilibriumAxialMuscle mat1;
      AxialTendonBase tmat;
      
      mat0 = createMuscleMat();
      mat0.setRigidTendon (true);
      mat0.setTendonSlackLength (0);

      mat1 = createMuscleMat();
      tmat = new Millard2012AxialTendon (myMaxIsoForce, myTendonSlackLen);

      myMus0.setMaterial (mat0);
      myTendon0.setMaterial (tmat);

      mech.attachAxialSpring (p0, myPM, myMus0);
      mech.attachAxialSpring (myPM, myPE0, myTendon0);

      // build second muscle

      Particle p1 = new Particle (1.0, 0, 0, -0.5);
      p1.setDynamic (false);
      mech.addParticle (p1);

      myPE1 = new Particle ("pe1", 1.0, myLength0, 0, -0.5);
      myPE1.setDynamic (false);
      mech.addParticle (myPE1);

      myMus1 = new Muscle("mus1");

      myMus1.setMaterial (mat1);
      mech.attachAxialSpring (p1, myPE1, myMus1);

      // render properties
      
      RenderProps.setSphericalPoints (mech, 0.03, Color.WHITE);
      RenderProps.setSpindleLines (mech, 0.02, Color.RED);
      RenderProps.setLineColor (myTendon0, Color.BLUE);
      RenderProps.setPointColor (myPM, Color.CYAN);

      myMus0.setExcitation (1.0);
      myMus1.setExcitation (1.0);
      initializeMuscleLength (myLength0, 1.0);

      // control panel

      ControlPanel panel = new ControlPanel();
      panel.addWidget ("material.optPennationAngle", myMus0, myMus1);
      panel.addWidget ("material.fibreDamping", myMus0, myMus1);
      panel.addWidget ("material.ignoreForceVelocity", myMus0, myMus1);
      panel.addWidget ("excitation", myMus0, myMus1);
      addControlPanel (panel);

      addController (new LengthController());
      
      addForceProbe ("force 0", myMus0, 2);
      addForceProbe ("force 1", myMus1, 2);
   }

   void addForceProbe (String name, Muscle mus, double stopTime) {
      try {
         NumericOutputProbe probe =
            new NumericOutputProbe (mus, "forceNorm", 0, stopTime, -1);
         probe.setName (name);
         addOutputProbe (probe);
      }
      catch (Exception e) {
         e.printStackTrace();
      }
   }

   void initializeMuscleLength (double l, double excitation) {
      double lm = 0;

      EquilibriumAxialMuscle mat1 =
         (EquilibriumAxialMuscle)myMus1.getMaterial();
      lm = mat1.computeLmWithConstantVm (l, 0, excitation);
      System.out.println ("setting mat1.muscleLength to " + lm);
      mat1.setMuscleLength (lm);         

      myPM.setPosition (new Point3d (lm, 0, 0));
   }

   public void postscanInitialize() {
      if (myPE0 == null) {
         MechModel mech = (MechModel)findComponent("models/mech");
         myPE0 = mech.particles().get("pe0");
         myPE1 = mech.particles().get("pe1");
         myPM = mech.particles().get("pm");
         myMus0 = (Muscle)mech.axialSprings().get("mus0");
         myMus1 = (Muscle)mech.axialSprings().get("mus1");
      }
   }
}
