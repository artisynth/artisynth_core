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

/**
 * Demo of two rigid bodies connected by a revolute joint
 */
public class RigidBodyJoint extends RootModel {

   MechModel mech;
   RigidBody body1;
   RigidBody body2;
   
   // dimensions for first body
   double lenx1 = 10;   
   double leny1 = 2;
   double lenz1 = 3;

   // dimensions for second body
   double lenx2 = 10;
   double leny2 = 2;
   double lenz2 = 2;

   public void build (String[] args) {

      // create mech model and set it's properties
      mech = new MechModel ("mech");
      mech.setGravity (0, 0, -98);
      mech.setFrameDamping (1.0);
      mech.setRotaryDamping (4.0);
      addModel (mech);

      PolygonalMesh mesh;  // bodies will be defined using a mesh

      // create first body and set its pose
      mesh = MeshFactory.createRoundedBox (lenx1, leny1, lenz1, /*nslices=*/8);
      RigidTransform3d XMB = new RigidTransform3d (0, 0, 0, 1, 1, 1, 2*Math.PI/3);
      mesh.transform (XMB);
      body1 = RigidBody.createFromMesh ("body1", mesh, 0.2, 1.0);
      body1.setPose (new RigidTransform3d (0, 0, 1.5*lenx1, 1, 0, 0, Math.PI/2));
      body1.setDynamic (false);

      // create second body and set its pose
      mesh = MeshFactory.createRoundedCylinder (
         leny2/2, lenx2, /*nslices=*/16, /*nsegs=*/1, /*flatBottom=*/false);
      mesh.transform (XMB);
      body2 = RigidBody.createFromMesh ("body2", mesh, 0.2, 1.0);
      body2.setPose (new RigidTransform3d (
                        (lenx1+lenx2)/2, 0, 1.5*lenx1, 1, 0, 0, Math.PI/2));

      // create the joint      
      RigidTransform3d XCA = new RigidTransform3d (-lenx2/2, 0, 0);
      RigidTransform3d XCB = new RigidTransform3d();
      XCB.mulInverseLeft (body1.getPose(), body2.getPose());
      XCB.mul (XCA);
      RevoluteJoint joint = new RevoluteJoint (body2, XCA, body1, XCB);

      // add components to the mech model
      mech.addRigidBody (body1);
      mech.addRigidBody (body2);
      mech.addRigidBodyConnector (joint);

      joint.setTheta (35);  // set joint position

      // set render properties for components
      joint.setAxisLength (4);
      RenderProps.setLineColor (joint, Color.BLUE);
      RenderProps.setLineRadius (joint, 0.2);
   }
}
