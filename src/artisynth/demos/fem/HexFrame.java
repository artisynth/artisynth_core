package artisynth.demos.fem;

import java.awt.Color;

import artisynth.core.femmodels.*;
import artisynth.core.mechmodels.*;
import artisynth.core.modelbase.*;
import artisynth.core.workspace.RootModel;
import maspack.matrix.*;
import maspack.render.*;

public class HexFrame extends RootModel {

   FemModel3d myFem;
   MechModel myMech;

   static double DENSITY = 1000;
   static double TOL = 1e-6;

   private void addFem (FemModel3d fem, RigidTransform3d X) {
      fem.transformGeometry (X);
      FemFactory.addFem (myFem, fem, TOL);
   }

   public void build (String[] args) {

      // /*flags=*/VERTICAL|ADD_DISPLACEMENT);
      //super (name, "hex", 1.0, 0.2, 25, 10, 0);
      // NORMAL:
      myMech = new MechModel ("mech");
      myFem = new FemModel3d ("frame");
      myFem.setDensity (DENSITY);
      RigidTransform3d X = new RigidTransform3d ();

      int n = 8; // number of cells per side
      double w = 1.0/n; // width of each cell

      FemModel3d feml = new FemModel3d ();
      FemModel3d fems = new FemModel3d ();
      FemFactory.createHexGrid (feml, 1.0, w, w, n, 1, 1);
      FemFactory.createHexGrid (fems, 1.0-2*w, w, w, n-2, 1, 1);

      double off = 0.5-w/2;

      addFem (fems, new RigidTransform3d (0, off, off));
      addFem (fems, new RigidTransform3d (0, -2*off, 0));
      addFem (fems, new RigidTransform3d (0, 0, -2*off));
      addFem (fems, new RigidTransform3d (0, 2*off, 0));

      addFem (fems, new RigidTransform3d (0, 0, 0, 0, 1, 0, Math.PI/2));
      addFem (fems, new RigidTransform3d (2*off, 0, 0));
      addFem (fems, new RigidTransform3d (0, -2*off, 0));
      addFem (fems, new RigidTransform3d (-2*off, 0, 0));

      addFem (feml, new RigidTransform3d (off, 0, off, 0, 0, 1, Math.PI/2));
      addFem (feml, new RigidTransform3d (-2*off, 0, 0));
      addFem (feml, new RigidTransform3d (0, 0, -2*off));
      addFem (feml, new RigidTransform3d (2*off, 0, 0));

      myFem.setSurfaceRendering (FemModel.SurfaceRender.Shaded);
      RenderProps.setFaceColor (myFem, new Color (0.7f, 0.7f, 0.9f));

      // fix left edge
      for (FemNode3d node : myFem.getNodes()) {
         Point3d p = node.getPosition();
         if (Math.abs (p.x-(-0.5)) < TOL) {
            node.setDynamic (false);
         }
      }

      myMech.addModel (myFem);

      addModel (myMech);
   }
}

