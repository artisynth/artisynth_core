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
}
