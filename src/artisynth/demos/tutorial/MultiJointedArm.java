package artisynth.demos.tutorial;

import java.awt.Color;
import java.io.IOException;

import artisynth.core.mechmodels.FixedMeshBody;
import artisynth.core.mechmodels.HingeJoint;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.UniversalJoint;
import artisynth.core.workspace.RootModel;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.render.RenderProps;
import maspack.util.DoubleInterval;
import maspack.util.PathFinder;

/**
 * A multi-jointed arm, suspended from above, with three degrees of freedom
 * formed from a universal joint and a hinge joint.
 */
public class MultiJointedArm extends RootModel {
   // 'geodir' is folder in which to locate mesh data:
   protected String geodir = PathFinder.getSourceRelativePath (this, "data/");
   protected double density = 1000.0;   // default density
   protected double scale = 1.0;        // scale factor for meshes
   protected MechModel myMech;          // mech model

   public void build (String[] args) throws IOException {
      myMech = new MechModel ("mech");
      // add damping: inertial, plus rotary to lessen spining about the base
      myMech.setInertialDamping (1.0);
      //myMech.setRotaryDamping (50.0);
      addModel (myMech);

      // add a decorative FixedMeshBody depicting a mount plate
      FixedMeshBody mountPlate = new FixedMeshBody (
         MeshFactory.createCylinder (
            /*radius*/0.45, /*height*/0.1, /*nsides*/64));
      // offset plate from the origin along z
      mountPlate.setPosition (new Point3d(0, 0, 0.20));
      myMech.addMeshBody (mountPlate);

      // create first main link from a rounded box mesh
      PolygonalMesh mesh = MeshFactory.createRoundedBox (
         /*wz*/0.6, /*wx*/0.2, /*wy*/0.2, /*nslices*/16);
      // rotate mesh about y axis to align long dimension with x
      mesh.transform (new RigidTransform3d (0, 0, 0, 0, Math.PI/2, 0));
      RigidBody link0 = RigidBody.createFromMesh ("link0", mesh, density, scale);
      // translate link to align end with the origin, and rotate to point down
      link0.setPose (new RigidTransform3d (0, 0, -0.3, 0, Math.PI/2, 0));
      myMech.addRigidBody (link0);

      // attach link0 to ground with a universal joint, whose D frame is set
      // at the origin and aligned so that the "zero" position points down
      RigidTransform3d TDW = new RigidTransform3d (0, 0, 0, 0, 0, Math.PI);
      UniversalJoint ujoint = new UniversalJoint (link0, null, TDW);
      ujoint.setName ("ujoint");
      // add rendering geometry to the universal joint
      mesh = new PolygonalMesh (geodir+"ujointBracket.obj");
      ujoint.setRenderMesh (mesh);      
      ujoint.setRollRange (new DoubleInterval (-240, 240));
      myMech.addBodyConnector (ujoint);

      // create second main link from a rounded box
      mesh = MeshFactory.createRoundedBox (
         /*wz*/0.8, /*wx*/0.15, /*wy*/0.15, /*nslices*/16);
      // rotate mesh about y axis to align long dimension with x
      mesh.transform (new RigidTransform3d (0, 0, 0, 0, Math.PI/2, 0));
      RigidBody link1 = RigidBody.createFromMesh ("link1", mesh, density, scale);
      myMech.addRigidBody (link1);
      // translate link1 to align end of link0, and rotate to point down
      link1.setPose (new RigidTransform3d (0, 0, -1.0, 0, Math.PI/2, 0));

      // attach link1 to link0 with a hinge joint
      HingeJoint hinge = new HingeJoint (
         link1, link0, new Point3d(0, 0, -0.6), Vector3d.Y_UNIT);
      // make the shaft of the hinge visible
      hinge.setShaftLength (0.25);
      hinge.setName ("hinge");
      myMech.addBodyConnector (hinge);

      // add some markers to the links:
      myMech.addFrameMarker (link0, new Point3d(0.1, -0.1, 0));
      myMech.addFrameMarker (link0, new Point3d(0.1,  0.1, 0));
      myMech.addFrameMarker (link1, new Point3d(0.2, -0.075, 0));
      myMech.addFrameMarker (link1, new Point3d(0.2,  0.075, 0));
      myMech.addFrameMarker (link1, new Point3d(0.475, 0.0, 0));

      // set the initial configuration of the arm by setting joint coordinates
      ujoint.setPitch (-40);
      hinge.setTheta (-120);

      // other render properties:
      // joints pale blue, other bodies blue-gray, markers white
      RenderProps.setFaceColor (
         myMech.bodyConnectors(), new Color (0.6f, 0.6f, 1f));
      RenderProps.setFaceColor (myMech, new Color (0.71f, 0.71f, 0.85f));
      RenderProps.setSphericalPoints (myMech, /*radius*/0.02, Color.WHITE);
   }
}
