package artisynth.demos.tutorial;

import java.awt.Color;
import java.util.LinkedHashSet;

import artisynth.core.femmodels.FemElement3d;
import artisynth.core.femmodels.FemFactory;
import artisynth.core.femmodels.FemMeshComp;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.materials.LinearMaterial;
import artisynth.core.mechmodels.CollisionBehavior;
import artisynth.core.mechmodels.CollisionManager;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.workspace.RootModel;
import maspack.matrix.RigidTransform3d;
import maspack.render.RenderProps;

public class FemSelfCollide extends RootModel {

   public void build (String[] args) {
      MechModel mech = new MechModel ("mech");
      addModel (mech);

      // create FEM model based on a partial (open) torus, with an
      // opening (gap) of angle of PI/4.
      FemModel3d ptorus = new FemModel3d("ptorus");
      FemFactory.createPartialHexTorus (
         ptorus, 0.15, 0.03, 0.06, 10, 20, 2, 7*Math.PI/4);
      // rotate the model so that the gap is at the botom
      ptorus.transformGeometry (
         new RigidTransform3d (0, 0, 0, 0, 3*Math.PI/8, 0));
      // set material and particle damping
      ptorus.setMaterial (new LinearMaterial (5e4, 0.45));
      ptorus.setParticleDamping (1.0);
      mech.addModel (ptorus);   

      // anchor the FEM by fixing the top center nodes
      for (FemNode3d n : ptorus.getNodes()) {
         if (Math.abs(n.getPosition().x) < 1e-15) {
            n.setDynamic(false);
         }
      }

      // Create sub-meshes to resist collison at the left and right ends of the
      // open torus. At each end, create a mesh component, and use its
      // createVolumetricSurface() method to create the mesh from the
      // elements near the end.
      LinkedHashSet<FemElement3d> elems =
         new LinkedHashSet<>(); // elements for mesh bulding
      FemMeshComp leftMesh = new FemMeshComp (ptorus, "leftMesh");
      // elements near the left end have numbers in the range 180 - 199
      for (int n=180; n<200; n++) {
         elems.add (ptorus.getElementByNumber(n));
      }
      leftMesh.createVolumetricSurface (elems);
      ptorus.addMeshComp (leftMesh);

      FemMeshComp rightMesh = new FemMeshComp (ptorus, "rightMesh");
      elems.clear();
      // elements at the right end have numbers in the range 0 - 19
      for (int n=0; n<20; n++) {
         elems.add (ptorus.getElementByNumber(n));
      }
      rightMesh.createVolumetricSurface (elems);
      ptorus.addMeshComp (rightMesh);

      // Create a collision behavior and use it to enable self collisions for
      // the FEM model. Since the model has low resolution and sharp edges, use
      // VERTEX_EDGE_PENETRATION, which requires the AJL_CONTOUR collider type.
      CollisionBehavior behav = new CollisionBehavior (true, 0);
      behav.setMethod (CollisionBehavior.Method.VERTEX_EDGE_PENETRATION);
      behav.setColliderType (CollisionManager.ColliderType.AJL_CONTOUR);
      mech.setCollisionBehavior (ptorus, ptorus, behav);

      // render properties: render the torus using element widgets
      ptorus.setElementWidgetSize (0.8);
      RenderProps.setFaceColor (ptorus, new Color(.4f, 1f, .6f));
      // enable rendering of the left and right contact meshes
      leftMesh.setSurfaceRendering (SurfaceRender.Shaded);
      rightMesh.setSurfaceRendering (SurfaceRender.Shaded);
      RenderProps.setFaceColor (leftMesh, new Color(.78f, .78f, 1f));
      RenderProps.setFaceColor (rightMesh, new Color(.78f, .78f, 1f));
   }
}
