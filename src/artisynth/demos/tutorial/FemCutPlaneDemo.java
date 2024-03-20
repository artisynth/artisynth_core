package artisynth.demos.tutorial;

import java.awt.Color;

import artisynth.core.femmodels.FemCutPlane;
import artisynth.core.femmodels.FemFactory;
import artisynth.core.femmodels.FemModel.Ranging;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.gui.ControlPanel;
import artisynth.core.materials.LinearMaterial;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.workspace.RootModel;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.render.RenderProps;
import maspack.render.Renderer.FaceStyle;
import maspack.util.DoubleInterval;

/**
 * Demonstrates use of a plane to visualize stress/strain within an FEM model
 */
public class FemCutPlaneDemo extends RootModel {

   public void build (String[] args) {
      // create a MechModel to contain the FEM model
      MechModel mech = new MechModel ("mech");
      addModel (mech);

      // create a half-torus shaped FEM to illustrate the cut plane
      FemModel3d fem = FemFactory.createPartialHexTorus (
        null, 0.1, 0.0, 0.05, 8, 16, 3, Math.PI);
      fem.setMaterial (new LinearMaterial (20000, 0.49));
      fem.setName ("fem");
      mech.addModel (fem);
      // fix the bottom nodes, which lie on the z=0 plane, to support it
      for (FemNode3d n : fem.getNodes()) {
         Point3d pos = n.getPosition();
         if (Math.abs(pos.z) < 1e-8) {
            n.setDynamic (false);
         }
      }

      // create a cut plane, with stress rendering enabled and a pose that
      // situates it in the z-x plane
      FemCutPlane cplane = 
         new FemCutPlane (new RigidTransform3d (0,0,0.03,  0,0,Math.PI/2));
      fem.addCutPlane (cplane);

      // set stress rendering with a fixed range of (0, 1500)
      cplane.setSurfaceRendering (SurfaceRender.Stress);
      cplane.setStressPlotRange (new DoubleInterval (0, 1500.0));
      cplane.setStressPlotRanging (Ranging.Fixed);

      // create a panel to control cut plane properties
      ControlPanel panel = new ControlPanel();
      panel.addWidget (cplane, "squareSize");
      panel.addWidget (cplane, "axisLength");
      panel.addWidget (cplane, "stressPlotRanging");
      panel.addWidget (cplane, "stressPlotRange");
      panel.addWidget (cplane, "colorMap");
      addControlPanel (panel);

      // set other render properites ...
      // make FEM line color white:
      RenderProps.setLineColor (fem, Color.WHITE);
      // make FEM elements invisible so they're not in the way:
      RenderProps.setVisible (fem.getElements(), false);
      // render FEM using a wireframe surface mesh so we can see through it:
      fem.setSurfaceRendering (SurfaceRender.Shaded);
      RenderProps.setDrawEdges (fem.getSurfaceMeshComp(), true);
      RenderProps.setFaceStyle (fem.getSurfaceMeshComp(), FaceStyle.NONE);
      RenderProps.setEdgeWidth (fem.getSurfaceMeshComp(), 2);
      // render cut plane using both square outline and its axes:
      cplane.setSquareSize (0.12); // size of the square
      cplane.setAxisLength (0.08); // length of the axes
      RenderProps.setLineWidth (cplane, 2); // boost line width for visibility
   }
}
