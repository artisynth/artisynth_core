package artisynth.demos.fem;

import java.awt.Color;
import java.awt.Point;

import javax.swing.*;

import maspack.render.*;
import maspack.render.Renderer.Shading;
import maspack.geometry.*;
import maspack.spatialmotion.*;
import maspack.matrix.*;
import maspack.properties.*;
import maspack.util.*;
import maspack.widgets.DoubleFieldSlider;
import artisynth.core.driver.Main;
import artisynth.core.util.TimeBase;
import artisynth.core.workspace.DriverInterface;
import artisynth.core.workspace.RootModel;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.femmodels.*;
import artisynth.core.gui.*;
import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.*;
import artisynth.core.mechmodels.MechSystemSolver.Integrator;
import artisynth.core.probes.InputProbe;

public class TubeDemo extends RootModel {
   FemModel3d tetMod;

   FemModel3d hexMod;

   FemModel3d quadMod;

   MechModel mechMod;

   boolean warping = true;

   double youngsModulus;

   double poissonsRatio;

   double myAmplitude = 0.60;

   double myFrequency = 1.0;

   boolean myAddProbes = true;

   public enum ElementType {
      Tet, Hex, Quadtet, TetAndQuadtet
   }

   public static PropertyList myProps =
      new PropertyList (TubeDemo.class, RootModel.class);

   // add addtional properties to the list:
   static {
      myProps.add ("youngsModulus", "stiffness of the FEMs", 200000);
      myProps.add ("poissonsRatio", "stiffness of the FEMs", 0.33);
      myProps.add ("warping", "element warping", true);
      myProps.add ("amplitude", "vibration amplitude", 0.25);
      myProps.add ("frequency", "vibration frequency", 1);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }
   
   private void updateMaterials () {
      tetMod.setLinearMaterial (youngsModulus, poissonsRatio, warping);
      hexMod.setLinearMaterial (youngsModulus, poissonsRatio, warping);
      quadMod.setLinearMaterial (youngsModulus, poissonsRatio, warping);
   }
   
   public void setYoungsModulus (double E) {
      youngsModulus = E;
      updateMaterials();
   }

   public double getYoungsModulus() {
      return youngsModulus;
   }

   public void setPoissonsRatio (double nu) {
      poissonsRatio = nu;
      updateMaterials();
   }

   public double getPoissonsRatio() {
      return poissonsRatio;
   }

   public void setWarping (boolean enable) {
      warping = enable;
      updateMaterials();
   }

   public boolean getWarping() {
      return warping;
   }

   public double getAmplitude() {
      return myAmplitude;
   }

   public void setAmplitude (double amp) {
      myAmplitude = amp;
   }

   public double getFrequency() {
      return myFrequency;
   }

   public void setFrequency (double freq) {
      myFrequency = freq;
   }
   
   public TubeDemo(String name) {
      this(name, ElementType.Quadtet, 7, 6, 3);  // default to hex tube
   }

   public TubeDemo (String name, ElementType type, int nt, int nl, int nr) {
      super (name);

      int nn = 1;
      double myDensity = 1000;

      // createTube(double l, double rin, double rout, int nt, int nl, int nr)

      double length = 1.0;

      tetMod = new FemModel3d();
      FemFactory.createTetTube (tetMod, length, 0.05, 0.1, 6, 7, 2);
      
      hexMod = new FemModel3d();
      FemFactory.createHexTube (hexMod, length, 0.05, 0.08, 7, 5, 2);
      
      quadMod = new FemModel3d ();
      FemFactory.createQuadtetTube (quadMod, length, 0.05, 0.1, 4, 3, 1);

      mechMod = new MechModel ("mech");

      RigidBody plate0 = new RigidBody ("plate0");
      RigidBody plate1 = new RigidBody ("plate1");

      double height = 0.05;
      double width = 0.4;

      RigidTransform3d X = new RigidTransform3d();
      X.R.setAxisAngle (0, 0, 1, Math.toRadians (30));
      X.p.y = width / 2;
      tetMod.transformGeometry (X);
      hexMod.transformGeometry (X);
      X.p.y = -width / 2;
      quadMod.transformGeometry (X);

      plate0.setMesh (
         MeshFactory.createBox (width, width, height), null);
      plate0.setInertia (SpatialInertia.createBoxInertia (
         1, width, width, height));
      X.setIdentity();
      X.p.z = (length + height) / 2;
      X.p.y = width / 2;
      plate0.setPose (X);
      plate0.setDynamic (false);

      plate1.setMesh (
         MeshFactory.createBox (width, width, height), null);
      plate1.setInertia (SpatialInertia.createBoxInertia (
         1, width, width, height));
      X.setIdentity();
      X.p.z = (length + height) / 2;
      X.p.y = -width / 2;
      plate1.setPose (X);
      plate1.setDynamic (false);

      double tetMass = 0;
      for (FemNode3d n : tetMod.getNodes()) {
         // System.out.println("node mass="+n.getMass());
         tetMass += n.getMass();
      }
      double quadMass = 0;
      for (FemNode3d n : quadMod.getNodes()) {
         quadMass += n.getMass();
      }
      double hexMass = 0;
      for (FemNode3d n : hexMod.getNodes()) {
         hexMass += n.getMass();
      }

      System.out.println ("tet mod: " + tetMass);
      System.out.println ("hex mod: " + hexMass);
      System.out.println ("quad mod: " + quadMass);

      double topZ = length / 2 - 1e-6;

      int numActiveHexNodes = hexMod.numNodes();
      int numActiveTetNodes = tetMod.numNodes();
      int numActiveQuadNodes = quadMod.numNodes();
      for (FemNode3d n : tetMod.getNodes()) {
         if (n.getPosition().z >= topZ) {
            n.setDynamic (false);
            numActiveTetNodes--;
         }
      }
      for (FemNode3d n : hexMod.getNodes()) {
         if (n.getPosition().z >= topZ) {
            n.setDynamic (false);
            numActiveHexNodes--;
         }
      }
      for (FemNode3d n : quadMod.getNodes()) {
         if (n.getPosition().z >= topZ) {
            n.setDynamic (false);
            numActiveQuadNodes--;
         }
      }

      System.out.println (
         "tet elements/nodes: " + tetMod.numElements() +
         " " + tetMod.numNodes());
      System.out.println (
         "hex elements/nodes: " + hexMod.numElements() +
         " " + hexMod.numNodes());
      System.out.println (
         "quad elements/nodes: " + quadMod.numElements() +
         " " + quadMod.numNodes());

      setRenderProps();

      // mechMod.addRigidBody (plate0);
      // mechMod.addRigidBody (plate1);

      setPoissonsRatio (0.33);
      setYoungsModulus (500000);

      tetMod.setSurfaceRendering (SurfaceRender.Shaded);

      hexMod.setSurfaceRendering (SurfaceRender.Shaded);

      quadMod.setSurfaceRendering (SurfaceRender.Shaded);

      switch (type) {
         case Tet: {
            mechMod.addRigidBody (plate0);
            mechMod.addModel (tetMod);
            for (FemNode3d n : tetMod.getNodes()) {
               if (!n.isDynamic()) {
                  mechMod.attachPoint (n, plate0);
               }
            }
            break;
         }
         case Hex: {
            mechMod.addRigidBody (plate0);
            mechMod.addModel (hexMod);
            for (FemNode3d n : hexMod.getNodes()) {
               if (!n.isDynamic()) {
                  mechMod.attachPoint (n, plate0);
               }
            }
            break;
         }
         case Quadtet: {
            mechMod.addRigidBody (plate1);
            mechMod.addModel (quadMod);
            for (FemNode3d n : quadMod.getNodes()) {
               if (!n.isDynamic()) {
                  mechMod.attachPoint (n, plate1);
               }
            }
            break;
         }
         case TetAndQuadtet: {
            mechMod.addRigidBody (plate0);
            mechMod.addModel (tetMod);
            mechMod.addRigidBody (plate1);
            mechMod.addModel (quadMod);
            for (FemNode3d n : tetMod.getNodes()) {
               if (!n.isDynamic()) {
                  mechMod.attachPoint (n, plate0);
               }
            }
            for (FemNode3d n : quadMod.getNodes()) {
               if (!n.isDynamic()) {
                  mechMod.attachPoint (n, plate1);
               }
            }
            break;
         }
      }

      mechMod.setProfiling (true);

      mechMod.setProfiling (true);

      mechMod.setIntegrator (MechSystemSolver.Integrator.BackwardEuler);
      addModel (mechMod);

      addControlPanel();
      if (myAddProbes) {
         addProbes();
      }

   }

   private class PlateProbe extends InputProbe {
      RigidBody myPlate;

      PlateProbe (RigidBody plate) {
         super (plate.getParent().getParent());
         myPlate = plate;
      }

      public void apply (double t) {
         RigidTransform3d X = new RigidTransform3d();
         myPlate.getPose (X);
         // X.p.x = myAmplitude*Math.sin (2*Math.PI*myFrequency*t0);
         // saw tooth:
         double x = (myFrequency * t + 0.25) % 1;
         if (x < 0.5) {
            X.p.x = myAmplitude * (4 * x - 1);
         }
         else {
            X.p.x = myAmplitude * (3 - 4 * x);
         }
         myPlate.setPose (X);
      }
   };

   protected void addControlPanel () { //MechModel mechMod, FemModel3d femMod) {
      ControlPanel myControlPanel = new ControlPanel ("options", "");
      DoubleFieldSlider ymSlider =
         (DoubleFieldSlider)myControlPanel.addWidget (
            this, "youngsModulus", 1000, 1600000);
      ymSlider.setRoundingTolerance (10000);
      myControlPanel.addWidget (this, "poissonsRatio", -1, 0.5);
      // myControlPanel.addWidget (mod, "density", 100, 5000);
      // myControlPanel.addWidget (mod, "particleDamping", 0, 200);
      // myControlPanel.addWidget (mod, "stiffnessDamping", 0, 1);
      // myControlPanel.addWidget (mod, "integrator");
      // myControlPanel.addWidget (mod, "matrixSolver");
      // myControlPanel.addWidget (mod, "maxStepSize");
      myControlPanel.addWidget (this, "warping");
      myControlPanel.addWidget (this, "amplitude");
      myControlPanel.addWidget (this, "frequency");

      addControlPanel (myControlPanel);
   }

   protected void addProbes() {
      RigidBody plate0 =
         (RigidBody)findComponent ("models/mech/rigidBodies/plate0");
      if (plate0 != null) {
         InputProbe probe0 = new PlateProbe (plate0);
         probe0.setStartTime (2);
         probe0.setStopTime (3.5);
         addInputProbe (probe0);
      }

      RigidBody plate1 =
         (RigidBody)findComponent ("models/mech/rigidBodies/plate1");
      if (plate1 != null) {
         InputProbe probe1 = new PlateProbe (plate1);
         probe1.setStartTime (2);
         probe1.setStopTime (3.5);
         addInputProbe (probe1);
      }
   }

   private void setRenderProps() {
      RenderProps.setDrawEdges (tetMod, true);
      RenderProps.setLineColor (tetMod, Color.BLUE);
      RenderProps.setFaceColor (tetMod, new Color (153, 153, 255));
      RenderProps.setLineWidth (tetMod, 2);
      RenderProps.setShading (tetMod, Shading.SMOOTH);

      RenderProps.setLineWidth (hexMod.getElements(), 2);
      RenderProps.setLineColor (hexMod.getElements(), Color.GREEN);
      RenderProps.setShading (hexMod, Shading.SMOOTH);

      RenderProps.setLineWidth (quadMod.getElements(), 2);
      RenderProps.setLineColor (quadMod.getElements(), Color.RED);
      RenderProps.setFaceColor (quadMod, new Color (153, 153, 255));
      RenderProps.setShading (quadMod, Shading.SMOOTH);
   }

}
