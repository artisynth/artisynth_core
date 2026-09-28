package artisynth.demos.test;

import java.awt.Color;
import java.io.IOException;

import artisynth.core.gui.ControlPanel;
import artisynth.demos.tutorial.ElasticFoundationContact;
import artisynth.core.materials.LinearElasticContact;
import artisynth.core.mechmodels.CollisionBehavior;
import artisynth.core.femmodels.*;
import artisynth.core.materials.*;
import artisynth.core.mechmodels.CollisionBehavior.ColorMapType;
import artisynth.core.mechmodels.CollisionBehavior.Method;
import artisynth.core.mechmodels.CollisionBehavior.VertexPenetrations;
import artisynth.core.mechmodels.CollisionManager;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.MechSystemSolver.PosStabilization;
import artisynth.core.workspace.RootModel;
import maspack.geometry.MeshFactory;
import maspack.geometry.Face.FaceFilter;
import maspack.geometry.*;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.*;
import maspack.render.RenderProps;
import maspack.render.Renderer.FaceStyle;
import maspack.util.PathFinder;

public class ThinFemContact extends RootModel {

   double innerBowlRadius = 0.85;
   double outerBowlRadius = 1.0;
   double ballRadius = 0.7;
   int ballDivisions = 3;
   int bowlDivisions = 4;

   double bowlFemOffset = -0.1;
   double[] bowlFemDepths = new double[] { -bowlFemOffset, 0.04 };
   double ballFemOffset = -0.1;
   double[] ballFemDepths = new double[] { -ballFemOffset, 0.04 };

   /**
    * Chooses faces whose z coordinates are all outside a prescribed plane
    */
   class PlaneFaceFilter implements FaceFilter {

      Plane myPlane;

      PlaneFaceFilter (Plane plane) {
         myPlane = plane;
      }

      public boolean faceIsValid(Face f) {
         for (Vertex3d vtx : f.getVertices()) {
            if (myPlane.distance (vtx.pnt) <= 0) {
               return false;
            }
         }
         return true;
      }
   }

   public void build (String[] args) throws IOException {
      MechModel mech = new MechModel ("mech");
      mech.setGravity (0, 0, -9.8);
      mech.setStabilization (PosStabilization.GlobalMass);
      addModel (mech);
      
      // default fem material and damping properties
      double massDamping = 1.0;
      double stiffnessDamping = 0.0;
      FemMaterial mat = new LinearMaterial();
      //FemMaterial mat = new MooneyRivlinMaterial (500000, 0, 0, 0, 0, 5000000);

      // create the mesh for the bowl
      PolygonalMesh mesh = MeshFactory.createIcosahedralBowl (
         innerBowlRadius, outerBowlRadius, bowlDivisions);
      // create the bowl from the mesh and make it non-dynamic
      RigidBody bowl =
         RigidBody.createFromMesh (
            "bowl", mesh, /*density=*/1000.0, /*scale=*/1.0);
      mech.addRigidBody (bowl);
      bowl.setDynamic (false);

      // create another spherical mesh to define the ball            
      mesh = MeshFactory.createIcosahedralSphere (ballRadius, ballDivisions);
      // create the ball from the mesh
      RigidBody ball =
         RigidBody.createFromMesh (
            "ball", mesh, /*density=*/1000.0, /*scale=*/1.0);
      // move the ball into an appropriate "drop" position
      ball.setPose (new RigidTransform3d (0.05, 0, 0.0));
      mech.addRigidBody (ball);     

      // Create FEM layer for the ball
      FemModel3d ballFem = FemFactory.createExtrusion (
         null, ballFemDepths, ballFemOffset, ball.getSurfaceMesh(), 
         new PlaneFaceFilter (new Plane (0, 0, -1, 0)));
      ballFem.setMaterial (mat);
      ballFem.setMassDamping (massDamping);
      ballFem.setStiffnessDamping (stiffnessDamping);
      // Transform FEM to match ball pose
      ballFem.transformGeometry (ball.getPose());
      mech.addModel (ballFem);

      // Attach ballFem base nodes to the ball. By the way the extrusion
      // mesh is constucted, these will be the first half of the nodes.
      for (int i=0; i<ballFem.numNodes()/2; i++) {
         mech.attachPoint (ballFem.getNode(i), ball);
      }

      // Create FEM layer for the bowl, and attach the base nodes
      // to the bowl
      // Create the bowl Fem by extruding a hemispherical mesh that 
      // matches its inner surface
      mesh = MeshFactory.createIcosahedralHemisphere (
         innerBowlRadius, bowlDivisions);
      mesh.flip(); // flip mesh orientation
      FemModel3d bowlFem = FemFactory.createExtrusion (
         null, bowlFemDepths, bowlFemOffset, mesh, /*faceFilter*/null);
      bowlFem.setMaterial (mat);
      bowlFem.setMassDamping (massDamping);
      bowlFem.setStiffnessDamping (stiffnessDamping);
      mech.addModel (bowlFem);
      for (int i=0; i<2*bowlFem.numNodes()/3; i++) {
         bowlFem.getNode(i).setDynamic (false);
         //mech.attachPoint (bowlFem.getNode(i), bowl);
      }

      // set collisions between the ball and bowl
      mech.setCollisionBehavior (ballFem, bowlFem, true);

      // contact rendering: render contact pressures
      CollisionManager cm = mech.getCollisionManager();
      cm.setDrawColorMap (ColorMapType.CONTACT_PRESSURE);
      cm.setVertexPenetrations (VertexPenetrations.FIRST_COLLIDABLE);
      cm.setContactForceLenScale (0.001);
      RenderProps.setSolidArrowLines (cm, 0.002, new Color (0.2f, 0.4f, 1f));
      RenderProps.setEdgeWidth (cm, 2);
      RenderProps.setEdgeColor (cm, Color.GREEN);
      RenderProps.setVisible (cm, true);
      // mesh rendering: render only edges of the bowl so we can see through it
      RenderProps.setFaceStyle (bowl, FaceStyle.NONE); 
      RenderProps.setLineColor (bowl, new Color (0.8f, 1f, 0.8f));
      RenderProps.setDrawEdges (mech, true); // draw edges for all meshes
      RenderProps.setFaceColor (ball, new Color (0.8f, 0.8f, 1f));

      // create a panel to allow control over some of the force behavior
      // parameters and rendering properties
      ControlPanel panel = new ControlPanel("options");
      panel.addWidget (cm, "colorMapRange");
      panel.addWidget ("material", ballFem, bowlFem);
      panel.addWidget ("particleDamping", ballFem, bowlFem);
      panel.addWidget ("stiffnessDamping", ballFem, bowlFem);
      addControlPanel (panel);
   }

// may need to add this the build() method:
//    if (mech.getUseImplicitFriction()) {
//       mech.setCompliantContact();
//    }
}


