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

public class EllipsoidWrapTest extends TwoStrandWrapBase {

   public void build (String[] args) {
      super.build (args);

      RigidEllipsoid ellipsoid = new RigidEllipsoid (
         "ellipsoid", size, 2.5*size, size/2, myDensity, 50);
      ellipsoid.setPose (new RigidTransform3d (0, 0, 1.5*size, 0, 0, 0));
      // RigidEllipsoid ellipsoid = new RigidEllipsoid (
      //    "ellipsoid", 2*size, 4*size, 2*size, myDensity/10, 50);
      // ellipsoid.setPose (new RigidTransform3d (0, 0, 3*size, 0, 0, 0));
      //ellipsoid.setDynamic (false);

      myMech.addRigidBody (ellipsoid);
      mySpring.addWrappable (ellipsoid);
   }
}
