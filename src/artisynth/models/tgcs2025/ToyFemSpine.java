package artisynth.models.tgcs2025;

import java.awt.Color;
import java.io.IOException;

import artisynth.core.femmodels.FemFactory;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.materials.MooneyRivlinMaterial;
import artisynth.core.materials.SimpleAxialMuscle;
import artisynth.core.mechmodels.RigidBody;
import maspack.geometry.PolygonalMesh;
import maspack.render.RenderProps;

/**
 * Extends ToySpine and replaces the joint connections with FEM models of the
 * intravertebral discs. Data courtesy of Benedikt Sagl, Thomas Holzinger, and
 * Dario Cazzola.
 */
public class ToyFemSpine extends ToySpine {

   /**
    * creates a FEM model of a disc by applying Tetgen to surface mesh
    * geometry.
    */
   FemModel3d addFem (String name) throws IOException {
      //// surface mesh path is inferred from the disc name
      PolygonalMesh surface = new PolygonalMesh (geodir + name + ".obj");
      FemModel3d fem = FemFactory.createFromMesh (null, surface, /*quality*/2);
      //// set material (value is lower than used in studies)
      fem.setMaterial (
         new MooneyRivlinMaterial (
            120000.0, 5000.0, 0, 0, 0, /*kappa*/1.5e7));
      //// rendering properties: render surface mesh blue-gray, element lines
      //// light blue, and nodes as small light blue spheres
      fem.setSurfaceRendering (SurfaceRender.Shaded);
      RenderProps.setFaceColor (fem, new Color (0.8f, 0.8f, 1f));
      RenderProps.setLineColor (fem, new Color (0.6f, 0.6f, 1f));
      RenderProps.setSphericalPoints (
         fem.getNodes(), 0.0002, new Color (0.6f, 0.6f, 1f));
      myMech.addModel (fem); //// add FEM to MechModel
      return fem;
   }

   /**
    * Connect a FEM to a rigid body by attaching nodes within a specified
    * tolerance of its surface mesh.
    */
   public void attachFemToBody (FemModel3d fem, RigidBody body, double tol) {

      PolygonalMesh mesh = body.getSurfaceMesh();
      for (FemNode3d node : fem.getNodes()) {
         if (mesh.distanceToPoint (node.getPosition()) < tol ||
             mesh.pointIsInside (node.getPosition()) == 1) {
            myMech.attachPoint (node, body);
            RenderProps.setPointColor (node, Color.RED);
         }
      }
   }

   public void build (String[] args) throws IOException {
      super.build (args);

      //// remove all frame springs and body connectors
      myMech.frameSprings().clear();
      myMech.bodyConnectors().clear();

      //// create the FE models for the discs
      FemModel3d disc32 = addFem ("disc32");
      FemModel3d disc43 = addFem ("disc43");
      FemModel3d disc54 = addFem ("disc54");

      //// attach the FEM models to the bodies
      // double tol = 1e-4;
      // attachFemToBody (disc32, myCerv2, tol);
      // attachFemToBody (disc32, myCerv3, tol);
      // attachFemToBody (disc43, myCerv3, tol);
      // attachFemToBody (disc43, myCerv4, tol);
      // attachFemToBody (disc54, myCerv4, tol);
      // attachFemToBody (disc54, myCerv5, tol);

      //// boost the muscle forces so we can still move the model!
      // ((SimpleAxialMuscle)myMuscleL.getMaterial()).setMaxForce (100);
      // ((SimpleAxialMuscle)myMuscleR.getMaterial()).setMaxForce (100);
   }
}
