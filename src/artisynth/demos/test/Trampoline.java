package artisynth.demos.test;

import java.awt.Color;
import java.io.*;
import java.util.*;

import artisynth.core.workspace.*;
import artisynth.core.mechmodels.*;
import artisynth.core.materials.*;
import artisynth.core.femmodels.*;
import artisynth.core.femmodels.FemModel.SurfaceRender;

import maspack.util.*;
import maspack.matrix.*;
import maspack.geometry.*;
import maspack.render.*;
import maspack.properties.*;

public class Trampoline extends RootModel {

   public void build (String[] args) {
      MechModel mech = new MechModel ("mech");
      addModel (mech);

      FemModel3d fem1 = FemFactory.createShellQuadGrid (
         null, 1.0, 1.0, 10, 10, 0.01, false);
      fem1.setSurfaceRendering (SurfaceRender.Shaded);
      fem1.setMaterial (new LinearMaterial (7e6, 0.45));
      mech.addModel (fem1);

      FemModel3d fem2 = new FemModel3d();
      fem2.setDensity (5000);
      FemFactory.createHexTorus (
         fem2, 0.15, 0.03, 0.06, 10, 20, 2);
      fem2.setSurfaceRendering (SurfaceRender.Shaded);
      fem2.setMaterial (new LinearMaterial (3.5e5, 0.45));
      fem2.setParticleDamping (1.0);
      mech.addModel (fem2);

      fem2.transformGeometry (new RigidTransform3d (0, 0, 0.5));
      mech.setDefaultCollisionBehavior (true, 0.0);

      for (FemNode3d n : fem1.getNodes()) {
         Point3d pos = n.getPosition();
         if (Math.abs(pos.x) == 0.5 && Math.abs(pos.y) == 0.5) {
            n.setDynamic (false);
         }
      }
      
      RenderProps.setFaceColor (fem1, new Color(153, 249, 212));
      RenderProps.setFaceColor (fem2, new Color(102, 255, 153));

      Vector3d eye0 = new Vector3d (0.0, -1.26906, 1.089);
      Vector3d center = new Vector3d (-0.0237671, -0.115134, 0.0881356);
      addController (
         new PanController (this, eye0, center, 10.0, Vector3d.Z_UNIT, 0, 10));
                        
   }

}
