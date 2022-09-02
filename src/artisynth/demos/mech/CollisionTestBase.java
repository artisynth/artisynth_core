package artisynth.demos.mech;

import java.awt.Color;

import maspack.matrix.RigidTransform3d;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.render.Renderer.FaceStyle;
import maspack.render.Renderer.PointStyle;
import artisynth.core.driver.Main;
import artisynth.core.gui.ControlPanel;
import artisynth.core.mechmodels.CollisionManager;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.modelbase.NumericState;
import artisynth.core.workspace.DriverInterface;
import artisynth.core.workspace.RootModel;

public class CollisionTestBase extends RootModel {
   
   protected MechModel mech;
   protected RigidBody table;
   protected boolean wireframe = true;
   
   public CollisionTestBase () {
      super();
   }
   
   public CollisionTestBase(String name) {
      super(name);
      
      mech = new MechModel();
      addModel (mech);
      
      setAdaptiveStepping (false);
      mech.setMaxStepSize (0.01);
      mech.setPenetrationTol (1e-3);
      mech.setGravity (1, 0, -9);
      
      table = RigidBody.createBox ("table", 4, 2, 0.5, 1000);
      table.setDynamic (false);
      RigidTransform3d X = new RigidTransform3d();
      X.p.set (0, 0, -0.25);
      table.setPose (X);
      addBox (table, Color.GRAY);

      createControlPanel (mech);
      setCollisionProperties (mech);
      for (int i=1; i<=10; i++) {
         addWayPoint (i*0.1);
      }
      setWayPointChecking (true);
   }

   void createControlPanel (MechModel mech) {
      
      ControlPanel panel = new ControlPanel();
      panel.addWidget (mech, "integrator");
      panel.addWidget (mech, "maxStepSize");
      addControlPanel (panel);
      Main.getMain().arrangeControlPanels (this);
   }

   void setCollisionProperties (MechModel mech) {
      
      CollisionManager collisions = mech.getCollisionManager();
      RenderProps.setVisible (collisions, true);
      RenderProps.setLineWidth (collisions, 3);      
      RenderProps.setLineColor (collisions, Color.RED);
      collisions.setDrawContactNormals (true);

      collisions.setDrawIntersectionContours(true);
      collisions.setDrawIntersectionPoints(true);
      collisions.setDrawIntersectionFaces(true);
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
   }
   
   void addBox (RigidBody box, Color col) {
      mech.addRigidBody (box);
      RenderProps.setFaceColor (box, col);
      if (wireframe) {
         RenderProps.setFaceStyle (box, Renderer.FaceStyle.NONE);
         RenderProps.setDrawEdges (box, true);
      }
   }
}
