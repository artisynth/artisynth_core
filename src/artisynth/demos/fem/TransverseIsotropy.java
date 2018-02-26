package artisynth.demos.fem;

import java.awt.Color;
import java.io.IOException;

import artisynth.core.femmodels.FemFactory;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.materials.TransverseLinearMaterial;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.PointAttachment;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.workspace.RootModel;
import maspack.matrix.AxisAngle;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.render.RenderProps;

public class TransverseIsotropy extends RootModel {
   
   @Override
   public void build(String[] args) throws IOException {
      super.build(args);
      
      MechModel mech = new MechModel("mech");
      addModel(mech);
      
      double h = 0.1;
      double r = 0.005;
      
      RigidBody rb = RigidBody.createBox("box", 2*r, 2*r, 2*r, 100, true);
      mech.addRigidBody(rb);
      rb.transformGeometry(new RigidTransform3d(new Vector3d(0,0,-h/2-r),AxisAngle.IDENTITY));
      
      FemModel3d fem = FemFactory.createCylinder(null, h, r, 24, 40, 4);
      fem.setDensity(1000);
      mech.addModel(fem);
      
      TransverseLinearMaterial mat = new TransverseLinearMaterial();
      mat.setYoungsModulus(50000, 50000);
      mat.setPoissonsRatio(0.45, 0.45);
      double G = 50000/(2*(1+0.45));
      mat.setShearModulus(G);
      fem.setMaterial(mat);
      fem.setName("fem");
      fem.setSurfaceRendering(SurfaceRender.Shaded);
      RenderProps.setFaceColor(fem, Color.ORANGE);
      RenderProps.setVisible(fem.getElements(), false);
      
      double eps = 1e-10;
      for (FemNode3d node : fem.getNodes()) {
         if (node.getPosition().z > h/2-eps) {
            node.setDynamic(false);
         } else if (node.getPosition().z < -h/2+eps) {
            PointAttachment pa = rb.createPointAttachment(node);
            mech.addAttachment(pa);
         }
      }
      
      
   }

}
