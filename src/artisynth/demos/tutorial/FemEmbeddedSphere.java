package artisynth.demos.tutorial;

import java.awt.Color;
import java.io.IOException;

import maspack.geometry.*;
import maspack.render.RenderProps;
import artisynth.core.mechmodels.Collidable.Collidability;
import artisynth.core.femmodels.*;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.materials.LinearMaterial;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.workspace.RootModel;

public class FemEmbeddedSphere extends RootModel {

   // Internal components
   protected MechModel mech;
   protected FemModel3d fem;
   protected FemMeshComp sphere;
   
   @Override
   public void build(String[] args) throws IOException {
      super.build(args);
      
      mech = new MechModel("mech");
      addModel(mech);
      
      fem = new FemModel3d("fem");
      mech.addModel(fem);
      
      // Build hex beam and set properties
      double[] size = {0.4, 0.4, 0.4};
      int[] res = {4, 4, 4};
      FemFactory.createHexGrid (fem, 
         size[0], size[1], size[2], res[0], res[1], res[2]);
      fem.setParticleDamping(2);
      fem.setDensity(10);
      fem.setMaterial(new LinearMaterial(4000, 0.33));
      
      // Add an embedded sphere mesh
      PolygonalMesh sphereSurface = MeshFactory.createOctahedralSphere(0.15, 3);
      sphere = fem.addMesh("sphere", sphereSurface);
      sphere.setCollidable (Collidability.EXTERNAL);
      
      // Boundary condition: fixed LHS
      for (FemNode3d node : fem.getNodes()) {
         if (node.getPosition().x == -0.2) {
            node.setDynamic(false);
         }
      }
      
      // Set rendering properties
      setFemRenderProps (fem);
      setMeshRenderProps (sphere);
   }
   
   // FEM render properties
   protected void setFemRenderProps ( FemModel3d fem ) {
      fem.setSurfaceRendering (SurfaceRender.Shaded);
      RenderProps.setLineColor (fem, Color.BLUE);
      RenderProps.setFaceColor (fem, new Color (0.5f, 0.5f, 1f));
      RenderProps.setAlpha(fem, 0.2);   // transparent
   }
   
   // FemMeshComp render properties
   protected void setMeshRenderProps ( FemMeshComp mesh ) {
      mesh.setSurfaceRendering( SurfaceRender.Shaded );
      RenderProps.setFaceColor (mesh, new Color (1f, 0.5f, 0.5f));
      RenderProps.setAlpha (mesh, 1.0);   // opaque
   }
   
}
