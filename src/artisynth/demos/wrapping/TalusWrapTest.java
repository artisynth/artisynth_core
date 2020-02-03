package artisynth.demos.wrapping;

import artisynth.core.workspace.DriverInterface;

/**
 * Model that demonstrates general wrapping around a Talus bone
 */
public class TalusWrapTest extends GeneralWrapTest {

   public void build (String[] args) {
      myGeometryType = GeometryType.TALUS;
      super.build (args);
      removeWayPoint (getWayPoint(10.0));
   }

   public void attach (DriverInterface driver) {
      super.attach (driver);
      // disable pan controller
      PanController controller = (PanController)getControllers().get(0);
      controller.setActive (false);
   }
}
