package artisynth.demos.fem;

import java.awt.Color;
import java.awt.Point;

import javax.swing.*;

import maspack.render.*;
import maspack.render.Renderer.Shading;
import maspack.geometry.*;
import maspack.matrix.*;
import maspack.properties.*;
import maspack.util.*;
import artisynth.core.workspace.RootModel;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.femmodels.*;
import artisynth.core.gui.*;
import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.*;
import artisynth.core.mechmodels.MechSystemSolver.Integrator;

public class HollowSphere extends RootModel {
   
   public void build (String[] args) {
      MechModel mech = new MechModel();
      addModel (mech);

      FemModel3d fem = new FemModel3d();

      PolygonalMesh mesh = MeshFactory.createIcosahedralSphere (10, 1);
      FemFactory.createTetExtrusion (fem, 1, 4, 0, mesh);

      mech.addModel (fem);

      RenderProps.setLineWidth (fem.getElements(), 2);
      RenderProps.setLineColor (fem.getElements(), Color.RED);
      RenderProps.setFaceColor (fem, new Color (153, 153, 255));
      RenderProps.setShading (fem, Shading.SMOOTH);

      fem.setSurfaceRendering (SurfaceRender.Shaded);

   }
}
