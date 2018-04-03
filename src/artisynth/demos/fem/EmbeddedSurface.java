package artisynth.demos.fem;

import java.awt.Color;

import maspack.geometry.MeshBase;
import maspack.geometry.MeshFactory;
import maspack.render.RenderProps;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.demos.fem.FemBeam3d;

public class EmbeddedSurface extends FemBeam3d {

   public void build (String[] args) {

      // NORMAL:
      build ("hex", 1.0, 0.5, 4, 2, /*options=*/0);
      
      myMechMod.setGravity(0,0,-9.8);
      myFemMod.setElementWidgetSize (0);
      myFemMod.setSurfaceRendering (SurfaceRender.Shaded);
      RenderProps.setVisible (myFemMod.getMeshComp("surface"), false);
      RenderProps.setFaceColor (myFemMod, new Color (1f, 153/255f, 153/255f));
         
      MeshBase mesh;
      mesh = MeshFactory.createSphere (0.2, 24, 24);
      mesh.scale (2, 1, 1);

      // PolylineMesh lineMesh = new PolylineMesh();
      // lineMesh.addVertex (0.1, 0, 0.3);
      // lineMesh.addVertex (0.2, 0, 0);
      // lineMesh.addVertex (0.1, 0, -0.3);

      // lineMesh.addVertex (-0.1, 0, 0.3);
      // lineMesh.addVertex (-0.2, 0, 0);
      // lineMesh.addVertex (-0.1, 0, -0.3);

      // lineMesh.addLine (new int[] {0, 1, 2});
      // lineMesh.addLine (new int[] {3, 4, 5});
      // mesh = lineMesh;

      myFemMod.addMesh (mesh);

      //myFemMod.setMaterial (
      //   new MooneyRivlinMaterial (50000.0, 0, 0, 0, 0, 5000000.0));
      //myFemMod.setIncompressible (FemModel.IncompMethod.AUTO);
   }
   
}

