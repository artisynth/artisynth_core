package artisynth.demos.test;

import java.awt.Color;
import java.io.IOException;

import artisynth.core.femmodels.AuxMaterialBundle;
import artisynth.core.femmodels.AuxMaterialElementDesc;
import artisynth.core.femmodels.FemElement3d;
import artisynth.core.femmodels.FemFactory;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.femmodels.StiffnessWarper3d;
import artisynth.core.materials.LinearMaterial;
import artisynth.core.materials.NullMaterial;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.modelbase.MonitorBase;
import artisynth.core.workspace.RootModel;
import maspack.matrix.AxisAngle;
import maspack.matrix.Matrix3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.render.RenderProps;

public class LinearAuxiliaryTest extends RootModel {

   @Override
   public void build(String[] args) throws IOException {
      super.build(args);

      MechModel mech = new MechModel("mech");
      addModel(mech);

      double h = 0.1;
      double r = 0.005;

      LinearMaterial lmat = new LinearMaterial(50000, 0.45, true);

      FemModel3d fem1 = createCylinder(h, r);
      fem1.setName("auxiliary");
      mech.addModel(fem1);
      fem1.setMaterial(new NullMaterial());
      AuxMaterialBundle bundle = new AuxMaterialBundle("mat");
      for (FemElement3d elem : fem1.getElements()) {
         bundle.addElement(new AuxMaterialElementDesc(elem));
      }
      fem1.addAuxMaterialBundle(bundle);
      bundle.setMaterial(lmat);


      FemModel3d fem2 = createCylinder(h, r);
      fem2.setName("linear");
      fem2.setMaterial(lmat);
      mech.addModel(fem2);

      RigidTransform3d rot = new RigidTransform3d(Vector3d.ZERO, AxisAngle.ROT_Y_90);
      fem1.transformGeometry(rot);
      fem2.transformGeometry(rot);
      fem2.transformGeometry(new RigidTransform3d(new Vector3d(0, 2*r, 0), AxisAngle.IDENTITY));
      RenderProps.setFaceColor(fem2, Color.MAGENTA);


      addMonitor(new StiffnessErrorMonitor(fem1, fem2));

   }

   private static class StiffnessErrorMonitor extends MonitorBase {

      FemModel3d fem1, fem2;

      public StiffnessErrorMonitor(FemModel3d fem1, FemModel3d fem2) {
         this.fem1 = fem1;
         this.fem2 = fem2;
      }

      @Override
      public void apply(double t0, double t1) {

         double eps = 1e-8;

         // compare stiffness matrices
         Matrix3d K1 = new Matrix3d();
         Matrix3d K2 = new Matrix3d();
         Vector3d f1 = new Vector3d();
         Vector3d f2 = new Vector3d();
         Matrix3d Kd = new Matrix3d();
         Vector3d fd = new Vector3d();
         for (int eidx=0; eidx<fem1.numElements(); ++eidx) {
            FemElement3d e1 = fem1.getElement(eidx);
            FemElement3d e2 = fem2.getElement(eidx);
            StiffnessWarper3d w1 = e1.getStiffnessWarper(/*weight=*/1.0);
            StiffnessWarper3d w2 = e2.getStiffnessWarper(/*weight=*/1.0);

            FemNode3d[] nodes1 = e1.getNodes();
            FemNode3d[] nodes2 = e1.getNodes();
            for (int i=0; i<nodes1.length; ++i) {
               for (int j=0; j<nodes1.length; ++j) {
                  K1.setZero();
                  K2.setZero();
                  w1.addNodeStiffness(K1, i, j);
                  w2.addNodeStiffness(K2, i, j);
                  Kd.sub(K1, K2);
                  double err = Kd.maxNorm();
                  if (err > eps) {
                     System.out.println("Stiffness error: ");
                     System.out.println(K1);
                     System.out.println(" vs ");
                     System.out.println(K2);
                     System.out.println("  error: " + err);
                  }
               }

               f1.setZero();
               f2.setZero();
               w1.addNodeForce(f1, i, nodes1);
               w2.addNodeForce(f2, i, nodes2);
               fd.sub(f1, f2);
               double err = fd.infinityNorm();
               if (err > eps) {
                  System.out.println("Force error: ");
                  System.out.println(f1);
                  System.out.println(" vs ");
                  System.out.println(f2);
                  System.out.println("  error: " + err);
               }
            }
         }
      }

   }

   static FemModel3d createHexGrid(double wx, double wy, double wz, int nx, int ny, int nz) {
      FemModel3d fem = FemFactory.createHexGrid(null, wx, wy, wz, nx, ny, nz);
      fem.setDensity(1000);

      fem.setSurfaceRendering(SurfaceRender.Shaded);
      RenderProps.setFaceColor(fem, Color.ORANGE);
      RenderProps.setVisible(fem.getElements(), false);

      double eps = 1e-10;
      for (FemNode3d node : fem.getNodes()) {
         if (node.getPosition().x < -wx/2+eps) {
            node.setDynamic(false);
         }
      }

      return fem;
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
