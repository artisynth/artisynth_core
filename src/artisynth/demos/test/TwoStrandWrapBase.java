package artisynth.demos.test;

import java.awt.Color;

import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.RigidTransform3d;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import artisynth.core.driver.Main;
import artisynth.core.gui.ControlPanel;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.MultiPointSpring;
import artisynth.core.mechmodels.Particle;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.RigidEllipsoid;
import artisynth.core.workspace.RootModel;

//import artisynth.core.mechmodels.DynamicMechComponent.Activity;

public class TwoStrandWrapBase extends RootModel {

   protected boolean pointsAttached = false;
   protected boolean collisionEnabled = false;
   protected double planeZ = -20;
   protected double myDensity = 0.2;
   protected static double size = 1.0;
   protected static double myGap = 2*size;
   protected MechModel myMech;
   protected MultiPointSpring mySpring;

   Particle myP0;
   Particle myP1;
   Particle myP2;
   Particle myP3;

   public void build (String[] args) {
      myMech = new MechModel ("mechMod");

      myMech.setGravity (0, 0, -9.8);
      myMech.setFrameDamping (1.0);
      myMech.setRotaryDamping (0.1);

      myP0 = new Particle (0.1, size*3, -myGap/2, size / 2);
      myP0.setDynamic (false);
      myMech.addParticle (myP0);

      myP1 = new Particle (0.1, -size*3, -myGap/2, size / 2);
      myP1.setDynamic (false);
      myMech.addParticle (myP1);

      myP2 = new Particle (0.1, -size*3, myGap/2, size / 2);
      myP2.setDynamic (false);
      myMech.addParticle (myP2);

      myP3 = new Particle (0.1, size*3, myGap/2, size / 2);
      myP3.setDynamic (false);
      myMech.addParticle (myP3);

      mySpring = new MultiPointSpring ("spring", 1, 0.1, 0);
      mySpring.addPoint (myP0);
      mySpring.setSegmentWrappable (50);
      mySpring.addPoint (myP1);
      mySpring.addPoint (myP2);
      mySpring.setSegmentWrappable (50);
      mySpring.addPoint (myP3);
      //mySpring.setWrapDamping (1.0);
      //mySpring.setWrapStiffness (10);
      //mySpring.setWrapH (0.01);
      myMech.addMultiPointSpring (mySpring);

      mySpring.setDrawKnots (false);
      mySpring.setDrawABPoints (true);
      mySpring.setWrapDamping (100);
      mySpring.setMaxWrapIterations (10);

      addModel (myMech);

      RenderProps.setPointStyle (myMech, Renderer.PointStyle.SPHERE);
      RenderProps.setPointRadius (myMech, size / 10);
      RenderProps.setLineStyle (myMech, Renderer.LineStyle.CYLINDER);
      RenderProps.setLineRadius (myMech, size / 30);
      RenderProps.setLineColor (myMech, Color.red);

      createControlPanel (myMech);
   }

   private void createControlPanel (MechModel mech) {
      ControlPanel panel = new ControlPanel ("options", "");
      panel.addWidget (mech, "integrator");
      panel.addWidget (mech, "maxStepSize");
      panel.addWidget (mech, "gravity");
      panel.addWidget (mech, "multiPointSprings/spring:drawKnots");
      panel.addWidget (mech, "multiPointSprings/spring:drawABPoints");
      panel.addWidget (mech, "multiPointSprings/spring:wrapDamping");
      panel.addWidget (mech, "multiPointSprings/spring:maxWrapIterations");
      addControlPanel (panel);
      Main.getMain().arrangeControlPanels (this);
   }
}
