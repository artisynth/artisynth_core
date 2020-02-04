package artisynth.demos.fem;

import java.awt.Color;

import maspack.geometry.MeshBase;
import maspack.matrix.*;
import maspack.geometry.MeshFactory;
import maspack.render.RenderProps;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.femmodels.FemMeshComp;
import artisynth.core.mechmodels.Collidable.Collidability;
import artisynth.core.mechmodels.*;
import artisynth.demos.fem.FemBeam3d;

public class EmbeddedSurfaceCollide extends FemBeam3d {

   public void build (String[] args) {

      // NORMAL:
      build ("hex", 1.0, 0.5, 4, 2, /*options=*/NO_FIXED_NODES | VERTICAL);
      
      myMechMod.setGravity(0,0,-9.8);
      myFemMod.setElementWidgetSize (0);
      myFemMod.setSurfaceRendering (SurfaceRender.Shaded);
      RenderProps.setVisible (myFemMod.getMeshComp("surface"), false);
      RenderProps.setFaceColor (myFemMod, new Color (1f, 153/255f, 153/255f));
         
      MeshBase mesh;
      mesh = MeshFactory.createSphere (0.2, 24, 24);
      mesh.scale (1, 1, 2);

      FemMeshComp meshComp = myFemMod.addMesh (mesh);

      RigidBody table =
         RigidBody.createCylinder ("table", 0.25, 0.05, 1000.0, 64);
      table.setDynamic (false);
      RenderProps.setFaceColor (table, new Color (0.7f, 0.7f, 1f));
      table.transformGeometry (new RigidTransform3d (0, 0, -1.0, 0, 0.03, 0));
      myMechMod.addRigidBody (table);

      meshComp.setCollidable (Collidability.EXTERNAL);
      myMechMod.setCollisionBehavior (meshComp, table, true, 0.25);
      CollisionManager cm = myMechMod.getCollisionManager();
      cm.setReduceConstraints (true);
      

   }
   
}

