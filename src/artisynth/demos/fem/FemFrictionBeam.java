package artisynth.demos.fem;

import java.awt.Color;

import artisynth.core.modelbase.*;
import artisynth.core.femmodels.*;
import artisynth.core.mechmodels.*;
import artisynth.core.workspace.RootModel;
import artisynth.core.gui.ControlPanel;
import artisynth.core.driver.Main;
import maspack.render.*;
import maspack.matrix.*;
import maspack.properties.*;

public class FemFrictionBeam extends RootModel {
   
   MechModel myMech;
   FemModel3d myFem;
   RigidBody myPlate;

   double myAngle = 30;
   double myMu = 0.3;

   public static PropertyList myProps =
      new PropertyList (FemFrictionBeam.class, RootModel.class);

   static {
      myProps.add ("friction", "friction coefficient", 0);
      myProps.add ("angle", "inclination angle", 0);
   }

   public void setFriction (double mu) {
      MechModel mechMod = (MechModel)models().get (0);
      myMu = mu;
      if (mechMod != null) {
         mechMod.setFriction (mu);
      }
   }

   public double getFriction() {
      return myMu;
   }

   public void setAngle (double ang) {
      double delAng = ang-myAngle;

      MechModel mechMod = (MechModel)models().get (0);
      mechMod.transformGeometry (
         new RigidTransform3d (0, 0, 0, 0, 1, 0, Math.toRadians(delAng)));
      myAngle = ang;
   }

   public double getAngle () {
      return myAngle;
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public FemFrictionBeam () {
      super (null);
   }

   public FemFrictionBeam (String name) {
      super (name);

      double feml = 0.1;
      double femw = 0.05;

      double plateh = 0.01;

      myMech = new MechModel ("mech");
      myFem = FemModel3d.createHexGrid (
         "fem", feml, femw, femw, 6, 3, 3, 1000);
      myMech.addModel (myFem);
      myPlate = RigidBody.createBox (
         "plate", 2*feml, feml, plateh, 1000);

      myMech.addRigidBody (myPlate);

      myPlate.setDynamic (false);

      myPlate.setPose (new RigidTransform3d (0, 0, -(femw+plateh)/2));

      RenderProps.setPointStyle (myFem, RenderProps.PointStyle.SPHERE);
      RenderProps.setPointRadius (myFem, 0.001);
      RenderProps.setPointColor (myFem, Color.GREEN);

      myMech.setCollisionBehavior (myFem, myPlate, true, myMu);

      myMech.transformGeometry (
         new RigidTransform3d (0, 0, 0, 0, 1, 0, Math.toRadians (myAngle)));

      addModel (myMech);

      ControlPanel panel = new ControlPanel("controls");
      panel.addWidget (this, "friction");
      panel.addWidget (this, "angle");
      panel.addWidget (myMech, "integrator");
      panel.addWidget (myMech, "maxStepSize");
      addControlPanel (panel);

      Main.getMain().arrangeControlPanels (this);
      
   }
   
   

}