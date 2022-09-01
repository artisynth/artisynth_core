package artisynth.demos.tutorial;

import java.awt.Color;
import java.io.IOException;

import artisynth.core.fields.ScalarVertexField;
import artisynth.core.gui.ControlPanel;
import artisynth.core.materials.LinearElasticContact;
import artisynth.core.mechmodels.CollisionBehavior;
import artisynth.core.mechmodels.CollisionBehavior.ColorMapType;
import artisynth.core.mechmodels.CollisionBehavior.Method;
import artisynth.core.mechmodels.CollisionManager;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.workspace.RootModel;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.Vertex3d;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.render.RenderProps;
import maspack.render.Renderer.FaceStyle;
import maspack.util.PathFinder;

public class VariableElasticContact extends RootModel {

   public void build (String[] args) throws IOException {
      MechModel mech = new MechModel ("mech");
      mech.setGravity (0, 0, -9.8);
      addModel (mech);

      // read in the mesh for the top cylinder
      PolygonalMesh mesh = new PolygonalMesh (
         PathFinder.getSourceRelativePath (
            VariableElasticContact.class, "data/beveledCylinder.obj"));
      // create the cylinder from the mesh
      RigidBody cylinder =
         RigidBody.createFromMesh (
            "cylinder", mesh, /*density=*/1000.0, /*scale=*/1.0);
      mech.addRigidBody (cylinder);

      // create a plate from a box mesh and make it non-dynamic
      RigidBody plate = 
         // box has widths 1.5 x 1.5 x 0.5 and mesh resolutions 10 x 10 x 4
          RigidBody.createBox (
             "plate", 1.5, 1.5, 0.5, 10, 10, 4, /*density=*/1000.0, false);
      plate.setPose (new RigidTransform3d (0, 0, -0.75));
      plate.setDynamic (false);
      mech.addRigidBody (plate);

      // enable vertex penetrations (required by EFC)
      CollisionManager cm = mech.getCollisionManager();
      cm.setMethod (Method.VERTEX_PENETRATION);
      // create the EFC
      double thickness = 0.1;
      LinearElasticContact efc =
         new LinearElasticContact (
            /*E=*/100000.0, /*nu=*/0.4, /*damping=*/0.1, thickness);

      // Create a thickness field for the cylinder mesh. Use a scalar vertex
      // field, with the thickness value h defined by h = thickness * (1 + r),
      // where r is the radial distance from the cylinder axis.
      ScalarVertexField field =
         new ScalarVertexField (cylinder.getSurfaceMeshComp());
      for (Vertex3d vtx : mesh.getVertices()) {
         Point3d pos = vtx.getPosition();
         double r = Math.hypot (pos.x, pos.y);
         field.setValue (vtx, thickness*(1+r));
      }
      // add the field to the MechModel, and bind the EFC thickness property
      mech.addField (field);
      efc.setThicknessField (field);

      // create a collision behavior that uses the EFC
      CollisionBehavior behav = new CollisionBehavior (true, /*friction=*/0.1);
      // Important: call setForceBehavior() *after* properties have been bound
      behav.setForceBehavior (efc);
      // enable cylinder/plate collisions using the behavior
      mech.setCollisionBehavior (cylinder, plate, behav);

      // contact rendering: render contact pressures
      cm.setDrawColorMap (ColorMapType.CONTACT_PRESSURE);
      RenderProps.setVisible (cm, true);
      // mesh rendering: render only edges of the bowl so we can see through it
      RenderProps.setFaceStyle (plate, FaceStyle.NONE); 
      RenderProps.setLineColor (plate, new Color (0.8f, 1f, 0.8f));
      RenderProps.setFaceColor (cylinder, new Color (0.8f, 0.8f, 1f));
      RenderProps.setDrawEdges (mech, true); // draw edges for all meshes

      // create a panel to allow control over some of the force behavior
      // parameters and rendering properties
      ControlPanel panel = new ControlPanel("options");
      panel.addWidget (behav, "vertexPenetrations");
      panel.addWidget (behav, "forceBehavior");
      panel.addWidget (behav, "friction");
      panel.addWidget (cm, "colorMapRange");
      addControlPanel (panel);
   }
}
