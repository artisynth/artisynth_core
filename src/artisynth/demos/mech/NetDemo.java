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
import artisynth.core.materials.*;
import artisynth.core.probes.NumericInputProbe;
import artisynth.core.probes.NumericOutputProbe;
import artisynth.core.util.*;
import artisynth.core.workspace.DriverInterface;
import artisynth.core.workspace.RootModel;

import java.io.*;
import java.awt.Color;

import javax.media.opengl.*;

import maspack.render.*;

//import artisynth.core.mechmodels.DynamicMechComponent.Activity;

public class NetDemo extends RootModel {

   private double stiffness = 1000.0;
   private double damping = 10.0;
   private double maxForce = 5000.0;
   private double pmass = 1.0;
   private double widthx = 20.0;
   private double widthy = 20.0;
   private int nx = 8;
   private int ny = 8;

   public NetDemo() {
      super (null);
   }

   private AxialSpring createSpring (
      PointList<Particle> parts, int pidx0, int pidx1) {
      Particle p0 = parts.get(pidx0);
      Particle p1 = parts.get(pidx1);
      Muscle spr = new Muscle (p0, p1);
      //spr.setFirstPoint (p0);
      //spr.setSecondPoint (p1);
      spr.setMaterial (new SimpleAxialMuscle (stiffness, damping, maxForce));
      return spr;
   }

   public NetDemo (String name) {
      this();
      setName (name);

      MechModel mech = new MechModel ("mech");
      // PropertyInfoList list = mech.getAllPropertyInfo();
      // for (PropertyInfo info : list)
      // { System.out.println (info.getName());
      // }
      mech.setGravity (0, 0, -980.0);
      //mech.setGravity (0, 0, 0);
      mech.setPointDamping (1.0);

      RenderProps.setPointStyle (mech, RenderProps.PointStyle.SPHERE);
      RenderProps.setPointRadius (mech, widthx/50.0);
      RenderProps.setLineRadius (mech, widthx/100.0);
      RenderProps.setLineColor (mech, Color.BLUE);
      RenderProps.setLineStyle (mech, RenderProps.LineStyle.ELLIPSOID);

      int nump = (nx+1)*(ny+1);

      PointList<Particle> redParticles =
         new PointList<Particle> (Particle.class, "redParticles");
      RenderProps.setPointColor (redParticles, Color.RED);

      ComponentList<ModelComponent> springs =
         new ComponentList<ModelComponent>(ModelComponent.class, "springs");

      RenderableComponentList<AxialSpring> greenSprings =
         new RenderableComponentList<AxialSpring> (
            AxialSpring.class, "greenSprings");
      RenderProps.setLineColor (greenSprings, new Color(0f, 0.5f, 0f));

      RenderableComponentList<AxialSpring> blueSprings =
         new RenderableComponentList<AxialSpring> (
            AxialSpring.class, "blueSprings");
      RenderProps.setLineColor (blueSprings, Color.BLUE);

      // ReferenceList<ModelComponent> refs =
      //    new ReferenceList<ModelComponent>("refs");

      for (int i=0; i<=nx; i++) {
         for (int j=0; j<=ny; j++) {
            Particle p = new Particle (
               pmass, widthx*(-0.5+i/(double)nx), widthy*(-0.5+j/(double)ny), 0);
            redParticles.add (p);
            if (i == 0 || i == nx) {
               p.setDynamic (false);
            }
         }
      }

      // create springs along y
      for (int i=0; i<=nx; i++) {
         for (int j=0; j<ny; j++) {
            greenSprings.add (createSpring (redParticles, i*(ny+1)+j, i*(ny+1)+j+1));
         }
      }
      for (int j=0; j<=ny; j++) {
         for (int i=0; i<nx; i++) {
            blueSprings.add (createSpring (redParticles, i*(ny+1)+j, (i+1)*(ny+1)+j));
         }
      }

      springs.add (greenSprings);
      springs.add (blueSprings);

      mech.add (redParticles);
      mech.add (springs);

      ReferenceList greenMid = new ReferenceList ("middleGreenSprings");
      ReferenceList blueMid = new ReferenceList ("middleBlueSprings");

      // refs.add (redParticles.get(0));
      // refs.add (redParticles.get(1));
      // refs.add (greenSprings.get(0));
      // refs.add (greenSprings.get(1));
      // refs.add (blueSprings.get(0));
      // refs.add (blueSprings.get(1));

      for (int i=0; i<8; i++) {
         blueMid.addReference (blueSprings.get(32+i));
      }
      for (int i=0; i<8; i++) {
         greenMid.addReference (greenSprings.get(32+i));
      }
      mech.add (greenMid);
      mech.add (blueMid);

      addModel (mech);
      //addPanProbe ();
   }


   public void attach (DriverInterface driver) {
      setViewerCenter (new Point3d (0.255913, -0.427015, -0.672117));
      setViewerEye (new Point3d (0, -38, 24));
      // System.out.println();
   }

   private double[] computePanData (
      double y0, double z0, double totalDegrees, double time, int nsegs) {

      double[] data = new double[4*(nsegs+1)];
      for (int i=0; i<=nsegs; i++) {
         double ang = i*Math.toRadians(totalDegrees)/(nsegs-1);
         double s = Math.sin(ang);
         double c = Math.cos(ang);
         data[i*4+0] = i*(time/nsegs);
         data[i*4+1] = s*y0;
         data[i*4+2] = c*y0;
         data[i*4+3] = z0;
      }
      return data;
   }


   public void addPanProbe () {
      try {
         NumericInputProbe inprobe =
            new NumericInputProbe (
               this, "viewerEye", 0, 6);
         double z = 0.8;
         inprobe.addData (
            computePanData (-38, 24, 180.0, 6.0, 16),
            NumericInputProbe.EXPLICIT_TIME);
         addInputProbe (inprobe);
      }
      catch (Exception e) {
         e.printStackTrace();
      }
   }

   ControlPanel myControlPanel;

   private void addControlPanel() {
      myControlPanel = new ControlPanel ("options", "");
      myControlPanel.addWidget (this, "models/mech:integrator");
      myControlPanel.addWidget (this, "models/mech:maxStepSize");
      addControlPanel (myControlPanel);
   }

}
