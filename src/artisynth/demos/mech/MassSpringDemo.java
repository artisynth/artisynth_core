package artisynth.demos.mech;

import maspack.geometry.*;
import maspack.matrix.*;
import maspack.properties.*;
import maspack.render.*;
import maspack.util.*;
import artisynth.core.driver.*;
import artisynth.core.gui.*;
import artisynth.core.mechmodels.*;
import artisynth.core.materials.*;
import artisynth.core.modelbase.*;
import artisynth.core.probes.NumericInputProbe;
import artisynth.core.probes.NumericOutputProbe;
import artisynth.core.util.*;
import artisynth.core.workspace.DriverInterface;
import artisynth.core.workspace.RootModel;

import java.io.*;
import java.awt.Color;

import maspack.render.*;

public class MassSpringDemo extends RootModel {

   MechModel msmod;

   class NonlinearMaterial extends LinearAxialMaterial {

      NonlinearMaterial (double k, double d) {
         super (k, d);
      }

      public double computeF (
         double l, double ldot, double l0, double excitaion) {
         return myStiffness * l * l;
      }

      public double computeDFdl (
         double l, double ldot, double l0, double excitaion) {
         return 2 * myStiffness * l;
      } 
      
      public double computeDFdldot (
         double l, double ldot, double l0, double excitaion) {
         return 0;
      }  
   }

   public void build (String[] args) {

      msmod = new MechModel ("massSpring");

      msmod.setGravity (0, 0, 0);
      msmod.setPointDamping (0);

      RenderProps.setPointStyle (msmod, Renderer.PointStyle.SPHERE);
      RenderProps.setPointRadius (msmod, 2);
      RenderProps.setPointColor (msmod, Color.GREEN);
      RenderProps.setLineRadius (msmod, 0.5);
      RenderProps.setLineStyle (msmod, Renderer.LineStyle.CYLINDER);

      Particle p0 = new Particle (5, -10, 0, 0);
      Particle p1 = new Particle (5, 10, 0, 0);

      AxialSpring spring = new AxialSpring();
      //spring.setMaterial (new NonlinearMaterial (50, 20));
      spring.setMaterial (new LinearAxialMaterial (50, 20));

      msmod.addParticle (p0);
      msmod.addParticle (p1);

      msmod.attachAxialSpring (p0, p1, spring);

      addModel (msmod);
      addControlPanel();
      addOutputProbe();

   }

   private void addControlPanel() {
      ControlPanel panel = new ControlPanel ("options", "");
      panel.addWidget (this, "models/massSpring:integrator");
      panel.addWidget (this, "models/massSpring:maxStepSize");
      addControlPanel (panel);
   }

   protected void addOutputProbe() {
      try {
         NumericOutputProbe collector =
            new NumericOutputProbe (
               this, "models/massSpring/particles/0:position",
               null, 0.01);
         collector.setDefaultDisplayRange (-40, 20);
         collector.setStopTime (10);
         addOutputProbe (collector);
      }
      catch (Exception e) {
         e.printStackTrace();
      }
   }      

}
