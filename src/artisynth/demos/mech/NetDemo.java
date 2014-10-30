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

public class NetDemo extends RootModel {

   private double stiffness = 1000.0;
   private double damping = 10.0;
   private double maxForce = 5000.0;
   private double pmass = 1.0;
   private double widthx = 20.0;
   private double widthy = 20.0;
   private int nx = 8;
   private int ny = 8;

   private AxialSpring createSpring (
      PointList<Particle> parts, int pidx0, int pidx1) {
      Particle p0 = parts.get(pidx0);
      Particle p1 = parts.get(pidx1);
      Muscle spr = new Muscle (p0, p1);
      spr.setMaterial (new SimpleAxialMuscle (stiffness, damping, maxForce));
      return spr;
   }

   public void build (String[] args) {

      MechModel mech = new MechModel ("mech");
      mech.setGravity (0, 0, -980.0);
      mech.setPointDamping (1.0);
      addModel (mech);

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

      // create green springs along y
      for (int i=0; i<=nx; i++) {
         for (int j=0; j<ny; j++) {
            greenSprings.add (
               createSpring (redParticles, i*(ny+1)+j, i*(ny+1)+j+1));
         }
      }
      // create blue springs along x
      for (int j=0; j<=ny; j++) {
         for (int i=0; i<nx; i++) {
            blueSprings.add (
               createSpring (redParticles, i*(ny+1)+j, (i+1)*(ny+1)+j));
         }
      }

      springs.add (greenSprings);
      springs.add (blueSprings);

      mech.add (redParticles);
      mech.add (springs);

      setPointRenderProps (mech);
      setLineRenderProps (mech);

      ReferenceList greenMid = new ReferenceList ("middleGreenSprings");
      ReferenceList blueMid = new ReferenceList ("middleBlueSprings");

      for (int i=0; i<8; i++) {
         blueMid.addReference (blueSprings.get(32+i));
      }
      for (int i=0; i<8; i++) {
         greenMid.addReference (greenSprings.get(32+i));
      }
      mech.add (greenMid);
      mech.add (blueMid);

      mech.setProfiling (true);
      //addPanProbe ();
   }

  protected void setPointRenderProps (Renderable r) {
      RenderProps.setPointColor (r, Color.RED);
      RenderProps.setPointStyle (r, RenderProps.PointStyle.SPHERE);
      RenderProps.setPointRadius (r, widthx/50.0);
   }

   protected void setLineRenderProps (Renderable r) {
      RenderProps.setLineColor (r, Color.BLUE);
      RenderProps.setLineStyle (r, RenderProps.LineStyle.ELLIPSOID);
      RenderProps.setLineRadius (r, widthx/100.0);
   }

   public void attach (DriverInterface driver) {
      setViewerCenter (new Point3d (0.255913, -0.427015, -0.672117));
      setViewerEye (new Point3d (0, -38, 24));
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
