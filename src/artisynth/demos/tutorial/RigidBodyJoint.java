package artisynth.demos.tutorial;

import maspack.geometry.*;
import maspack.spatialmotion.*;
import maspack.matrix.*;
import maspack.render.*;
import maspack.util.*;
import artisynth.core.mechmodels.*;
import artisynth.core.modelbase.*;
import artisynth.core.probes.WayPoint;
import artisynth.core.driver.*;
import artisynth.core.util.*;
import artisynth.core.workspace.RootModel;
import artisynth.core.gui.*;
import maspack.render.*;

import java.awt.Color;
import java.io.*;

import javax.swing.*;

public class RigidBodyJoint extends RootModel {

   MechModel mech;
   RigidBody link1;
   RigidBody link2;
   
   // dimensions for first link
   double lenx1 = 10;   
   double leny1 = 2;
   double lenz1 = 3;

   // dimensions for second link
   double lenx2 = 10;
   double leny2 = 2;
   double lenz2 = 2;

   public void build (String[] args) {

      mech = new MechModel ("mech");
      mech.setGravity (0, 0, -98);
      mech.setFrameDamping (1.0);
      mech.setRotaryDamping (4.0);
      PolygonalMesh mesh;

      // create first link
      mesh = MeshFactory.createRoundedBox (lenx1, leny1, lenz1, /*nslices=*/8);
      RigidTransform3d XMB = new RigidTransform3d (0, 0, 0, 1, 1, 1, 2*Math.PI/3);
      mesh.transform (XMB);
      link1 = RigidBody.createFromMesh ("link1", mesh, 0.2, 1.0);
      link1.setPose (new RigidTransform3d (0, 0, 1.5*lenx1, 1, 0, 0, Math.PI/2));
      link1.setDynamic (false);
      mech.addRigidBody (link1);

      // create second link
      mesh = MeshFactory.createRoundedCylinder (
         leny2/2, lenx2, /*nslices=*/16, /*nsegs=*/1, /*flatBottom=*/false);
      mesh.transform (XMB);
      link2 = RigidBody.createFromMesh ("link2", mesh, 0.2, 1.0);
      link2.setPose (new RigidTransform3d (
                        (lenx1+lenx2)/2, 0, 1.5*lenx1, 1, 0, 0, Math.PI/2));
      mech.addRigidBody (link2);
      
      RigidTransform3d XCA = new RigidTransform3d (-lenx2/2, 0, 0);
      RigidTransform3d XCB = new RigidTransform3d();
      XCB.mulInverseLeft (link1.getPose(), link2.getPose());
      XCB.mul (XCA);
      RevoluteJoint joint = new RevoluteJoint (link2, XCA, link1, XCB);

      joint.setAxisLength (4);
      RenderProps.setLineRadius (joint, 0.2);

      mech.addRigidBodyConnector (joint);
      joint.setTheta (35);
      addModel (mech);

   }

}
