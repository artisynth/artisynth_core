package artisynth.demos.mech;

import java.awt.Color;

import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.*;
import artisynth.core.util.*;
import artisynth.core.workspace.RootModel;
import artisynth.core.gui.*;
import artisynth.core.driver.*;
import maspack.geometry.*;
import maspack.spatialmotion.*;
import maspack.matrix.*;
import maspack.render.*;
import maspack.util.*;

import java.io.*;
import java.net.*;
import java.util.*;

public class RevoluteJointDemo extends RootModel {

   private static double inf = Double.POSITIVE_INFINITY;

   public void build (String[] args) throws IOException {

      MechModel mech = new MechModel ("mech");
      mech.setGravity (0, 0, -9.8);
      mech.setFrameDamping (50.0);
      mech.setRotaryDamping (1.0);
      addModel (mech);

      double size = 1.0;

      PolygonalMesh mesh = MeshFactory.createRoundedBox (
         size, size/2, size/4, 2, 1, 1, 12, /*flatBottom=*/true);
      RigidBody base = RigidBody.createFromMesh (
         "base", mesh, /*density=*/1000.0, 1.0);
      base.setDynamic (false);
      mech.addRigidBody (base);

      mesh = MeshFactory.createRoundedBox (
         size, size/3, size/3, 2, 1, 1, 12);
      RigidBody tip = RigidBody.createFromMesh (
         "tip", mesh, /*density=*/1000.0, 1.0);
      tip.setPose (new RigidTransform3d (0, 0, size));
      mech.addRigidBody (tip);

      RevoluteJoint joint = new RevoluteJoint (
         base, tip, new Point3d (0, 0, size/2), Vector3d.Y_UNIT);
      mech.addBodyConnector (joint);
      joint.setMaxTheta (135.0);
      joint.setMinTheta (-135.0);
      joint.setTheta (45);

      ControlPanel panel = new ControlPanel();
      panel.addWidget (joint, "theta");
      panel.addWidget (joint, "thetaRange");
      addControlPanel (panel);

      joint.setAxisLength (size/2);
      RenderProps.setCylindricalLines (joint, 0.05*size, Color.BLUE);
      RenderProps.setFaceColor (tip, new Color (0.5f, 1f, 1f));

   }
}
