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

public class PrismaticJointDemo extends RootModel {

   private static double inf = Double.POSITIVE_INFINITY;

   public void build (String[] args) throws IOException {

      MechModel mech = new MechModel ("mech");
      mech.setGravity (0, 0, -9.8);
      mech.setFrameDamping (50.0);
      mech.setRotaryDamping (1.0);
      addModel (mech);

      double size = 1.0;

      RigidBody base = RigidBody.createCylinder (
         "base", 0.125*size, size, /*density=*/1000.0, /*nsides=*/25);
      base.setDynamic (false);
      PolygonalMesh mesh = MeshFactory.createTube (
         0.1*size, 0.2*size, 0.25*size, 25, 1, 1);
      mesh.transform (new RigidTransform3d (0, 0, 0.65*size, 0, Math.PI/2, 0));
      base.addMesh (mesh);
      mech.addRigidBody (base);

      RigidBody tip = RigidBody.createCylinder (
         "tip", 0.1*size, size, /*density=*/1000.0, /*nsides=*/25);
      tip.setPose (new RigidTransform3d (0, 0, 0.65*size, 0, Math.PI/2, 0));
      mech.addRigidBody (tip);

      PrismaticJoint joint = new PrismaticJoint (
         base, tip, new Point3d (0, 0, 0.65*size), Vector3d.X_UNIT);
      mech.addBodyConnector (joint);
      joint.setMaxZ (size/2);
      joint.setMinZ (-size/2);


      ControlPanel panel = new ControlPanel();
      panel.addWidget (joint, "z");
      panel.addWidget (joint, "zRange");
      addControlPanel (panel);

      joint.setAxisLength (1.5*size);
      RenderProps.setCylindricalLines (joint, 0.02*size, Color.BLUE);
      RenderProps.setFaceColor (tip, new Color (0.5f, 1f, 1f));
   }
}
