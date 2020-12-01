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

public class PlanarJointDemo extends RootModel {

   private static double inf = Double.POSITIVE_INFINITY;

   public void build (String[] args) throws IOException {

      MechModel mech = new MechModel ("mech");
      mech.setGravity (0, 0, -9.8);
      mech.setFrameDamping (50.0);
      mech.setRotaryDamping (10.0);
      addModel (mech);

      double size = 1.0;

      RigidBody base = RigidBody.createCylinder (
         "base", size, size/8, /*density=*/1000.0, /*nsides=*/100);
      base.setDynamic (false);
      mech.addRigidBody (base);

      RigidBody box = RigidBody.createBox (
         "box", size/2, size/2, size/8, /*density=*/1000.0);
      box.setPose (new RigidTransform3d (0, 0, size/8));
      mech.addRigidBody (box);

      PlanarJoint joint = new PlanarJoint (
         base, box, new Point3d (0, 0, size/8), Vector3d.Z_UNIT);
      mech.addBodyConnector (joint);
      joint.setMaxX (size/2);
      joint.setMinX (-size/2);
      joint.setMaxY (size/2);
      joint.setMinY (-size/2);
      joint.setMinTheta (-90);
      joint.setMaxTheta (90);

      ControlPanel panel = new ControlPanel();
      panel.addWidget (joint, "x");
      panel.addWidget (joint, "xRange");
      panel.addWidget (joint, "y");
      panel.addWidget (joint, "yRange");
      panel.addWidget (joint, "theta");
      panel.addWidget (joint, "thetaRange");
      addControlPanel (panel);

      joint.setAxisLength (0.2*size);
      RenderProps.setCylindricalLines (joint, 0.02*size, Color.BLUE);
      RenderProps.setFaceColor (box, new Color (0.5f, 1f, 1f));
      setDefaultViewOrientation (AxisAlignedRotation.X_Y);
   }
}
