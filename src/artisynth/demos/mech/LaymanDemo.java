package artisynth.demos.mech;

import java.awt.Color;

import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.*;
import artisynth.core.mechmodels.MechSystemSolver.Integrator;
import artisynth.core.util.*;
import artisynth.core.workspace.RootModel;
import artisynth.core.gui.*;
import artisynth.core.driver.Main;
import maspack.geometry.*;
import maspack.properties.*;
import maspack.spatialmotion.*;
import maspack.matrix.*;
import maspack.render.*;
import maspack.render.Renderer.LineStyle;
import maspack.util.*;

import java.io.*;
import java.net.*;
import java.util.*;

public class LaymanDemo extends RootModel {

   protected MechModel myMechMod;
   RigidBody myBin;
   LaymanModel myLayman;

   public static PropertyList myProps =
      new PropertyList (LaymanDemo.class, RootModel.class);

   static {
      myProps.add ("friction", "friction coefficient", 0);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   private double myMu = 0.2;

   public double getFriction() {
      return myMu;
   }

   public void setFriction (double mu) {
      myMu = mu;
      for (Model m : myModels) {
         if (m instanceof MechModel) {
            ((MechModel)m).setFriction (mu);
         }
      }
   }

   public void build (String[] args) throws IOException {

      myMechMod = new MechModel ("twoPuppets");
      myMechMod.setProfiling (false);
      myMechMod.setGravity (0, 0, -9.8);

      myMechMod.setIntegrator (MechSystemSolver.Integrator.BackwardEuler);

      myLayman = new LaymanModel("Daniel");
      myMechMod.addModel (myLayman);

      myBin = RigidBody.createFromMesh (
         "bin", LaymanDemo.class, "geometry/taperedBin.obj", 1000, 0.1);
      myBin.setDynamic (false);
      myBin.setPose (0, 0, -2, 0, 0, 0);
      RenderProps.setFaceColor (myBin, Color.GRAY);
      myMechMod.addRigidBody (myBin);

      CollisionManager cm = myMechMod.getCollisionManager();
      cm.setRigidPointTol (1e-2);
      cm.setRigidRegionTol (1e-1);
      myMechMod.setFriction (0.2);
      myMechMod.setDefaultCollisionBehavior (true, 0.2);
      myMechMod.setPenetrationTol (1e-3);

//       RigidBody head = (RigidBody)myLayman.findComponent ("rigidBodies/head");
//       head.setDynamic (false);

      addModel (myMechMod);

      ControlPanel panel = new ControlPanel();
      panel.addWidget (myMechMod, "integrator");
      panel.addWidget (this, "friction");
      addControlPanel (panel);
      Main.getMain().arrangeControlPanels (this);

      // myMechMod.setProfiling (true);
      myMechMod.setIntegrator (Integrator.ConstrainedBackwardEuler);
      //addBreakPoint (0.90);

      // try {
      //    String name = "old.txt";
      //    if (MechModel.useNewCollisionManager) {
      //       name = "new.txt";
      //    }
      //    MechSystemSolver.setLogWriter (
      //       ArtisynthIO.newIndentingPrintWriter (name));
      // }
      // catch (Exception e)  {
      // }
   }
}
