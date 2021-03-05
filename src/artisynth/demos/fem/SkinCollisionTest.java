package artisynth.demos.fem;

import java.awt.Color;

import maspack.matrix.AxisAngle;
import maspack.matrix.RigidTransform3d;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.render.Renderer.FaceStyle;
import maspack.render.Renderer.PointStyle;
import artisynth.core.driver.Main;
import artisynth.core.femmodels.AnsysReader;
import artisynth.core.femmodels.SkinMeshBody;
import artisynth.core.gui.ControlPanel;
import artisynth.core.mechmodels.CollisionManager;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.workspace.DriverInterface;

public class SkinCollisionTest extends FemSkinDemo {

   double friction = 0;

   public void build(String[] args) {
      super.build (args);
      
      // add table underneath
      CollisionManager cm = myMech.getCollisionManager();
      cm.setReduceConstraints (true);

      setAdaptiveStepping (false);
      myMech.setMaxStepSize (0.01);
      myMech.setPenetrationTol (1e-3);
      myMech.setGravity (0, 0, -9);

      RigidBody table = RigidBody.createBox ("table", 4, 2, 0.5, 1000);
      table.setDynamic (false);
      RigidTransform3d X = new RigidTransform3d();
      X.p.set (0, 0, -1);
      X.setRotation(new AxisAngle(0,1,0,Math.toRadians(4.1222)));
      table.setPose (X);
      addBox (table, Color.GRAY);
      
      SkinMeshBody skinBody = (SkinMeshBody)myMech.meshBodies().get("skin");
      myMech.setCollisionBehavior(table, skinBody, true, friction);



      myMech.clearBodyConnectors();
      //myMech.setProfiling (true);
      createControlPanel (myMech);
      setCollisionProperties (myMech);
   }

   public void createControlPanel (MechModel mech) {
      ControlPanel panel = new ControlPanel();
      panel.addWidget (mech, "integrator");
      panel.addWidget (mech, "maxStepSize");
      addControlPanel (panel);
      Main.getMain().arrangeControlPanels (this);
   }

   public void setCollisionProperties (MechModel mech) {

      CollisionManager collisions = mech.getCollisionManager();
      RenderProps.setVisible (collisions, true);
      RenderProps.setLineWidth (collisions, 3);      
      RenderProps.setLineColor (collisions, Color.RED);
      collisions.setContactNormalLen (0.5);
      collisions.setDrawContactNormals (true);
      collisions.setDrawIntersectionContours(true);
      collisions.setDrawIntersectionPoints(true);

      RenderProps.setPointColor(collisions, Color.CYAN);
      RenderProps.setPointStyle(collisions, PointStyle.SPHERE);
      RenderProps.setPointRadius(collisions, 0.02);
      RenderProps.setEdgeColor(collisions, Color.BLUE);
      RenderProps.setFaceColor(collisions, Color.YELLOW);
      RenderProps.setFaceStyle(collisions, FaceStyle.FRONT_AND_BACK);
   }

   @Override
   public void attach (DriverInterface driver) {
      super.attach (driver);
      // for (int i=1; i<=10; i++) {
      //    addWayPoint (i*0.1);
      // }
      // setWaypointChecking (true);

      //addBreakPoint (0.30);
   }

   void addBox (RigidBody box, Color col) {
      myMech.addRigidBody (box);
      RenderProps.setFaceColor (box, col);
      RenderProps.setFaceStyle (box, Renderer.FaceStyle.FRONT);
      RenderProps.setDrawEdges (box, false);
      RenderProps.setAlpha(box, 0.2);
   }
}
