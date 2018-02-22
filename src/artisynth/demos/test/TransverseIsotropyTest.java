package artisynth.demos.test;

import java.awt.Color;
import java.io.IOException;

import artisynth.core.femmodels.FemFactory;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.materials.LinearMaterial;
import artisynth.core.materials.TransverseLinearMaterial;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.PointAttachment;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.workspace.RootModel;
import maspack.matrix.AxisAngle;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.render.RenderProps;

public class TransverseIsotropyTest extends RootModel {
   
   @Override
   public void build(String[] args) throws IOException {
      super.build(args);
      
      MechModel mech = new MechModel("mech");
      addModel(mech);
      
      double h = 0.1;
      double r = 0.005;
      
      FemModel3d fem = createCylinder(h, r);
      fem.setName("transverse");
      mech.addModel(fem);
      
      TransverseLinearMaterial mat = new TransverseLinearMaterial();
      mat.setYoungsModulus(50000, 50000);
      mat.setPoissonsRatio(0.45, 0.45);
      double G = 50000/(2*(1+0.45));
      mat.setShearModulus(G);
      mat.setCorotated(true);
      fem.setMaterial(mat);
      
      FemModel3d fem2 = createCylinder(h, r);
      fem2.setName("linear");
      LinearMaterial lmat = new LinearMaterial(50000, 0.45, true);
      fem2.setMaterial(lmat);
      mech.addModel(fem2);
      
      RigidTransform3d rot = new RigidTransform3d(Vector3d.ZERO, AxisAngle.ROT_Y_90);
      fem.transformGeometry(rot);
      fem2.transformGeometry(rot);
      fem2.transformGeometry(new RigidTransform3d(new Vector3d(0, 2*r, 0), AxisAngle.IDENTITY));
      RenderProps.setFaceColor(fem2, Color.MAGENTA);
      
   }
   
   FemModel3d createCylinder(double h, double r) {
      FemModel3d fem = FemFactory.createCylinder(null, h, r, 24, 40, 4);
      fem.setDensity(1000);
      
      fem.setSurfaceRendering(SurfaceRender.Shaded);
      RenderProps.setFaceColor(fem, Color.ORANGE);
      RenderProps.setVisible(fem.getElements(), false);
      
      double eps = 1e-10;
      for (FemNode3d node : fem.getNodes()) {
         if (node.getPosition().z < -h/2+eps) {
            node.setDynamic(false);
         }
      }
      
      return fem;
   }

}
