package artisynth.demos.mech;

import javax.swing.JFrame;

import maspack.geometry.*;
import maspack.matrix.*;
import maspack.properties.*;
import maspack.render.*;
import maspack.util.*;
import artisynth.core.driver.*;
import artisynth.core.gui.*;
import artisynth.core.mechmodels.*;
import artisynth.core.modelbase.*;
import artisynth.core.probes.NumericInputProbe;
import artisynth.core.probes.NumericOutputProbe;
import artisynth.core.util.*;
import artisynth.core.workspace.RootModel;

import java.io.*;
import java.awt.Color;

import maspack.render.*;

//import artisynth.core.mechmodels.DynamicMechComponent.Activity;

public class MultiSpringDemo extends RootModel {
   private boolean pointsAttached = false;

   private boolean collisionEnabled = false;

   private double planeZ = -20;

   protected static double size = 1.0;

   public void build (String[] args) {
      MechModel mechMod = new MechModel ("mechMod");

      mechMod.setGravity (0, 0, -9.8);
      mechMod.setFrameDamping (1.0);
      mechMod.setRotaryDamping (0.1);

      RigidBody block = new RigidBody ("block");
      PolygonalMesh mesh = MeshFactory.createBox (size, size, size);
      block.setMesh (mesh, null);
      block.setInertiaFromDensity (1);
      mechMod.addRigidBody (block);

      Particle p0 = new Particle (0.1, -size * 3, 0, size / 2);
      p0.setDynamic (false);
      mechMod.addParticle (p0);

      Particle p1 = new Particle (0.1, size * 3, 0, size / 2);
      p1.setDynamic (false);
      mechMod.addParticle (p1);

      FrameMarker mkr0 = new FrameMarker();
      mechMod.addFrameMarker (mkr0, block, new Point3d (-size / 2, 0, size / 2));

      FrameMarker mkr1 = new FrameMarker();
      mechMod.addFrameMarker (mkr1, block, new Point3d (size / 2, 0, size / 2));

      MultiPointSpring spring = new MultiPointSpring (1, 0.1, 0);
      spring.addPoint (p0);
      spring.addPoint (mkr0);
      spring.addPoint (mkr1);
      spring.addPoint (p1);
      mechMod.addMultiPointSpring (spring);
      mySpr = spring;

      addModel (mechMod);

      RenderProps.setPointStyle (mechMod, Renderer.PointStyle.SPHERE);
      RenderProps.setPointRadius (mechMod, size / 10);
      RenderProps.setLineStyle (mechMod, Renderer.LineStyle.CYLINDER);
      RenderProps.setLineRadius (mechMod, size / 30);
      RenderProps.setLineColor (mechMod, Color.red);

      createControlPanel();
   }

   private void createControlPanel() {
      ControlPanel panel = new ControlPanel ("options", "");
      panel.addWidget (this, "models/mechMod:integrator");
      panel.addWidget (this, "models/mechMod:maxStepSize");
      panel.addWidget (this, "models/mechMod:gravity");
      addControlPanel (panel);
      Main.getMain().arrangeControlPanels (this);
   }

   // values of l and ldot for the previous two time steps
   double myL0, myL1;
   double myLdot0, myLdot1;
   MultiPointSpring mySpr;

   int myStepCnt = 0; // simulation step counter

   public StepAdjustment advance (double t0, double t1, int flags) {

      double h = t1-t0;
      
      if (t0 == 0) {
         myStepCnt = 0;
      }

      // // get spring l and ldot values
      // double l = mySpr.getLength();
      // double ldot = mySpr.getLengthDot();

      // // do comparison if we have enough data and until cylinder breaks contact
      // if (myStepCnt > 2 && t1 <= 1.26) {
      //    // comparison is made with myLdot1, against numeric derivative
      //    // based on l and myL0
      //    double ldotNum = (l-myL0)/(2*h);
      //    //System.out.printf ("%8.4f %8.4f\n", myLdot1, ldotNum);
      // }
      // myStepCnt++;

      // // save l and lot history
      // myL0 = myL1;
      // myL1 = l;
      // myLdot0 = myLdot1;
      // myLdot1 = ldot;

      StepAdjustment sa = super.advance (t0, t1, flags);
      return sa;
   }


}
