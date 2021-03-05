package artisynth.demos.mech;

import maspack.geometry.*;
import maspack.spatialmotion.*;
import maspack.matrix.*;
import maspack.render.*;
import maspack.render.Renderer;
import maspack.util.*;
import artisynth.core.mechmodels.*;
import artisynth.core.femmodels.*;
import artisynth.core.mechmodels.MechSystemSolver.Integrator;
import artisynth.core.materials.*;
import artisynth.core.modelbase.*;
import artisynth.core.probes.WayPoint;
import artisynth.core.driver.*;
import artisynth.core.util.*;
import artisynth.core.workspace.DriverInterface;
import artisynth.core.workspace.RootModel;
import artisynth.core.gui.*;
import maspack.render.*;

import java.awt.Color;
import java.io.*;
import java.util.*;

import javax.swing.*;

/**
 * Demonstrates a rigid body being connected to another using a
 * SolidJoint.
 */
public class BodyBodyJoint extends RootModel {

   Color myLinkColor = new Color (228/255f, 115/255f, 33/255f);
   Color myJointColor = new Color (93/255f, 93/255f, 168/255f);

   RigidBody createLink (
      String name, double lx, double ly, double lz, Color faceColor) {
      double density = 1.0;
      int nslices = 16; // number of slices for approximating a circle
      PolygonalMesh mesh = MeshFactory.createRoundedBox (
         lx, ly, lz, 20, 4, 4, nslices / 2);
      RigidTransform3d ROT_90_Y = new RigidTransform3d (0, 0, 0, 0, Math.PI/2, 0);
      mesh.transform (ROT_90_Y);
      RigidBody link = RigidBody.createFromMesh (name, mesh, density, 1.0);
      RenderProps.setFaceColor ((Renderable)link, faceColor);
      return link;
   }

   public void build (String[] args) {

      MechModel mech = new MechModel ("mech");
      addModel (mech);
      mech.setGravity (0, 0, -9.8);
      mech.setFrameDamping (0.1);
      mech.setRotaryDamping (0.1);

      // first link
      double lx1 = 2;
      double ly1 = 0.4;
      double lz1 = 0.6;
      RigidBody link1 = createLink ("link1", lx1, ly1, lz1, myLinkColor);
      mech.add (link1);

      // hinge joint 
      RigidTransform3d TJW =
         new RigidTransform3d(-lx1/2, 0, 0,  0, 0, Math.toRadians(90));
      HingeJoint rjoint =
         new HingeJoint (link1, (ConnectableBody)null, TJW);
      rjoint.setName ("joint1");
      rjoint.setShaftLength (0.8);
      RenderProps.setFaceColor (rjoint, myJointColor);
      mech.addBodyConnector (rjoint);

      // second link
      double lx2 = 2;
      double ly2 = 0.4;
      double lz2 = 0.4;
      RigidBody link2 = createLink ("link2", lx2, ly2, lz2, myLinkColor.darker());
      RigidTransform3d XLW = new RigidTransform3d (lx1/2+lx2/2-0.5, 0, 0);
      link2.setPose (XLW);
      mech.add (link2);

      RigidTransform3d XSW = new RigidTransform3d (lx1/2, 0, 0);
      SolidJoint sjoint = new SolidJoint(link2, link1, XSW);
      mech.addBodyConnector (sjoint);
   }
}
