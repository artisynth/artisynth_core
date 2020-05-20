/**
 * Copyright (c) 2020, by the Authors: Fabien Pean
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.demos.fem;

import java.awt.Color;
import java.io.IOException;

import artisynth.core.driver.Main;
import artisynth.core.femmodels.FemModel.IncompMethod;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.femmodels.HexElement;
import artisynth.core.femmodels.SplineBasis;
import artisynth.core.femmodels.BSpline3dElement;
import artisynth.core.materials.MooneyRivlinMaterial;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.workspace.RootModel;
import maspack.render.RenderProps;

public class BSpline3dElementTest extends RootModel {
   @Override
   public void build(String[] args) throws IOException {
      Main.getMain().getScheduler().setRealTimeAdvance(true);
      MechModel m = new MechModel();
      addModel(m);

      {// Hex
         FemModel3d fem = new FemModel3d();
         m.addModel(fem);
         fem.setMaterial(new MooneyRivlinMaterial(1000, 0, 0, 0, 0, 1000));

         BSpline3dElement element = new BSpline3dElement();
         element.uvw[0] = new SplineBasis(1, new double[] { 0, 0, 1, 1 });
         element.uvw[1] = new SplineBasis(1, new double[] { 0, 0, 1, 1 });
         element.uvw[2] = new SplineBasis(1, new double[] { 0, 0, 1, 1 });
         int nNodes = element.uvw[0].getKnotVectorSize() - element.uvw[0].getDegree() - 1;
         nNodes *= element.uvw[1].getKnotVectorSize() - element.uvw[1].getDegree() - 1;
         nNodes *= element.uvw[2].getKnotVectorSize() - element.uvw[2].getDegree() - 1;

         {
            FemNode3d[] nodes = new FemNode3d[nNodes];

            nodes[0] = new FemNode3d(0, 0, -2);
            nodes[1] = new FemNode3d(0, 0, 0);
            nodes[2] = new FemNode3d(0, 2, -2);
            nodes[3] = new FemNode3d(0, 2, 0);
            nodes[4] = new FemNode3d(2, 0, -2);
            nodes[5] = new FemNode3d(2, 0, 0);
            nodes[6] = new FemNode3d(2, 2, -2);
            nodes[7] = new FemNode3d(2, 2, 0);

            nodes[1].setDynamic(false);
            nodes[3].setDynamic(false);
            nodes[5].setDynamic(false);
            nodes[7].setDynamic(false);

            for(FemNode3d n : nodes) {
               fem.addNode(n);
            }
            element.setNodes(nodes);
         }
         fem.addElement(element);
         fem.setIncompressible(IncompMethod.ELEMENT);
         fem.setSoftIncompMethod(IncompMethod.FULL);
         fem.setIncompCompliance(0);
         fem.setDensity(100);

         RenderProps.setSphericalPoints(fem.getNodes(), 0.05, Color.blue);
      }

      {// Bspline p1
         FemModel3d fem = new FemModel3d();
         m.addModel(fem);
         fem.setMaterial(new MooneyRivlinMaterial(1000, 0, 0, 0, 0, 1000));

         FemNode3d[] nodes = new FemNode3d[8];

         nodes[0] = new FemNode3d(4, 0, 0);
         nodes[1] = new FemNode3d(6, 0, 0);
         nodes[2] = new FemNode3d(6, 2, 0);
         nodes[3] = new FemNode3d(4, 2, 0);
         nodes[4] = new FemNode3d(4, 0, -2);
         nodes[5] = new FemNode3d(6, 0, -2);
         nodes[6] = new FemNode3d(6, 2, -2);
         nodes[7] = new FemNode3d(4, 2, -2);

         nodes[0].setDynamic(false);
         nodes[1].setDynamic(false);
         nodes[2].setDynamic(false);
         nodes[3].setDynamic(false);
         for(FemNode3d n : nodes) {
            fem.addNode(n);
         }
         HexElement element = new HexElement(nodes);
         fem.addElement(element);
         fem.setIncompressible(IncompMethod.ELEMENT);
         fem.setSoftIncompMethod(IncompMethod.FULL);
         fem.setIncompCompliance(0);
         fem.setDensity(100);

         RenderProps.setSphericalPoints(fem.getNodes(), 0.05, Color.red);
      }

      {// Bspline p2
         FemModel3d fem = new FemModel3d();
         m.addModel(fem);
         fem.setMaterial(new MooneyRivlinMaterial(1000, 0, 0, 0, 0, 1000));

         BSpline3dElement element = new BSpline3dElement();
         element.uvw[0] = new SplineBasis(1, new double[] { 0, 0, 1, 1 });
         element.uvw[1] = new SplineBasis(1, new double[] { 0, 0, 1, 1 });
         element.uvw[2] = new SplineBasis(2, new double[] { 0, 0, 0, 1, 1, 1 });
         int nNodes = element.uvw[0].getKnotVectorSize() - element.uvw[0].getDegree() - 1;
         nNodes *= element.uvw[1].getKnotVectorSize() - element.uvw[1].getDegree() - 1;
         nNodes *= element.uvw[2].getKnotVectorSize() - element.uvw[2].getDegree() - 1;

         {
            FemNode3d[] nodes = new FemNode3d[nNodes];

            nodes[0] = new FemNode3d(-2, 0, 0);
            nodes[1] = new FemNode3d(-2, 0, -1);
            nodes[2] = new FemNode3d(-2, 0, -2);
            nodes[3] = new FemNode3d(-2, 2, 0);
            nodes[4] = new FemNode3d(-2, 2, -1);
            nodes[5] = new FemNode3d(-2, 2, -2);
            nodes[6] = new FemNode3d(-4, 0, 0);
            nodes[7] = new FemNode3d(-4, 0, -1);
            nodes[8] = new FemNode3d(-4, 0, -2);
            nodes[9] = new FemNode3d(-4, 2, 0);
            nodes[10] = new FemNode3d(-4, 2, -1);
            nodes[11] = new FemNode3d(-4, 2, -2);

            for(FemNode3d n : nodes) {
               fem.addNode(n);
               if(n.getPosition().z == 0)
                  n.setDynamic(false);
            }
            element.setNodes(nodes);
         }
         fem.addElement(element);
         fem.setIncompressible(IncompMethod.ELEMENT);
         fem.setSoftIncompMethod(IncompMethod.FULL);
         fem.setIncompCompliance(0);
         fem.setDensity(100);

         RenderProps.setSphericalPoints(fem.getNodes(), 0.05, Color.green);
      }
   }
}
