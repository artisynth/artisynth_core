package artisynth.demos.tutorial;

import java.awt.Color;

import artisynth.core.femmodels.FemFactory;
import artisynth.core.femmodels.FemModel;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.femmodels.SkinMeshBody.*;
import artisynth.core.femmodels.SkinMeshBody;
import artisynth.core.femmodels.SkinWeightingFunction;
import artisynth.core.femmodels.SkinMarker;
import artisynth.core.materials.LinearMaterial;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.Point;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.workspace.RootModel;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.render.RenderProps;
import maspack.render.Renderer.FaceStyle;

public class AllBodySkinning extends RootModel {

   private static double EPS = 1e-12;

   public void build (String[] args) {
      MechModel mech = new MechModel ("mech");
      addModel (mech);

      // size and density parameters
      double len = 1.0;
      double rad = 0.15;
      double density = 1000.0;

      // create a tubular FEM model, and rotate it so it lies along the x axis
      FemModel3d fem = FemFactory.createHexTube (
         null, len, rad/3, rad, 8, 8, 2);
      fem.transformGeometry (new RigidTransform3d (0, 0, 0, 0, Math.PI/2, 0));
      mech.addModel (fem);

      // create two rigid body blocks
      RigidBody block0 =
         RigidBody.createBox ("block0", len/2, 2*rad, 2*rad, density);
      block0.setPose (new RigidTransform3d (-3*len/4, 0, 0));
      mech.addRigidBody (block0);
      block0.setDynamic (false);
                         
      RigidBody block1 =
         RigidBody.createBox ("block1", len/2, 2*rad, 2*rad, density);
      block1.setPose (new RigidTransform3d (3*len/4, 0, 0));
      mech.addRigidBody (block1);

      // attach the blocks to each end of the FEM model
      for (FemNode3d n : fem.getNodes()) {
         if (Math.abs(n.getPosition().x-len/2) < EPS) {
            mech.attachPoint (n, block1);
         }
         if (Math.abs(n.getPosition().x+len/2) < EPS) {
            mech.attachPoint (n, block0);
         }
      }
      fem.setMaterial (new LinearMaterial (500000.0, 0.49));
      
      // create base mesh to be skinned
      PolygonalMesh mesh = 
         MeshFactory.createRoundedCylinder (
            /*r=*/0.4, 2*len, /*nslices=*/16, /*nsegs=*/15, /*flatbotton=*/false);
      // rotate mesh so its long axis lies along the x axis
      mesh.transform (new RigidTransform3d (0, 0, 0, 0, Math.PI/2, 0));

      // create the skinBody, with the FEM model and blocks as master bodies
      SkinMeshBody skinBody = new SkinMeshBody ("skin", mesh);
      skinBody.addMasterBody (fem);
      skinBody.addMasterBody (block0);
      skinBody.addMasterBody (block1);
      skinBody.computeAllVertexConnections();
      mech.addMeshBody (skinBody);

      // add a marker point to the end of the skin mesh
      SkinMarker marker = 
         skinBody.addMarker ("marker", new Point3d(1.4, 0.000, 0.000));

      // set up rendering properties
      RenderProps.setFaceStyle (skinBody, FaceStyle.NONE);
      RenderProps.setDrawEdges (skinBody, true);
      RenderProps.setLineColor (skinBody, Color.CYAN);
      fem.setSurfaceRendering (FemModel.SurfaceRender.Shaded);
      RenderProps.setFaceColor (fem, new Color (0.5f, 0.5f, 1f));
      RenderProps.setSphericalPoints (marker, 0.05, Color.RED);
   }
}
