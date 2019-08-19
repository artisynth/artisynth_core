package artisynth.demos.test;

import java.awt.Color;
import java.util.HashSet;
import java.util.LinkedHashSet;

import maspack.matrix.RigidTransform3d;
import maspack.matrix.Point3d;
import maspack.matrix.VectorNd;
import maspack.render.RenderProps;
import artisynth.core.femmodels.*;
import artisynth.core.materials.LinearMaterial;
import artisynth.core.mechmodels.*;
import artisynth.core.workspace.RootModel;

public class LinearPointConstraintTest extends RootModel {

   public void build (String[] args) {

      MechModel mech = new MechModel ("mech");
      addModel (mech);

      // create and add FEM beam 
      FemModel3d fem = FemFactory.createHexGrid (null, 1.0, 0.2, 0.2, 4, 1, 1);
      fem.setMaterial (new LinearMaterial (500000, 0.33));
      RenderProps.setLineColor (fem, Color.BLUE);
      RenderProps.setLineWidth (mech, 3);
      mech.addModel (fem);

      // fix leftmost nodes of the FEM
      for (FemNode3d n : fem.getNodes()) {
         if ((n.getPosition().x-(-0.5)) < 1e-8) {
            n.setDynamic (false);
         }
      }

      // create and add rigid body box
      RigidBody body = RigidBody.createEllipsoid (
         "body", 0.15, 0.15, 0.2, /*density=*/200, 40);
      body.setPose (new RigidTransform3d (0.35, 0, -0.25));
      mech.add (body);

      FrameMarker mkr = mech.addFrameMarker (
         body, new Point3d (0, 0, 0.2));
      RenderProps.setSphericalPoints (mkr, 0.015, Color.RED);
      RenderProps.setSphericalPoints (fem, 0.015, Color.GREEN);

      FemElement3d elem = fem.getElement (3);
      
      VectorNd coords = new VectorNd (elem.numNodes());
      elem.getMarkerCoordinates (coords, null, mkr.getPosition(), false);

      double[] wgts = new double[elem.numNodes()+1];
      Point[] pnts = new Point[elem.numNodes()+1];
      for (int i=0; i<elem.numNodes(); i++) {
         pnts[i] = elem.getNodes()[i];
         wgts[i] = coords.get(i);
      }
      pnts[elem.numNodes()] = mkr;
      wgts[elem.numNodes()] = -1;

      LinearPointConstraint cons = new LinearPointConstraint (pnts, wgts);
      mech.addConstrainer (cons);
   }
}
