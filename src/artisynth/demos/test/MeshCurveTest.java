package artisynth.demos.test;

import java.awt.Color;
import java.io.*;
import java.util.*;

import artisynth.core.workspace.*;
import artisynth.core.mechmodels.*;
import artisynth.core.renderables.*;
import artisynth.core.modelbase.*;
import artisynth.core.femmodels.*;
import artisynth.core.femmodels.FemModel.*;
import artisynth.core.materials.*;
import artisynth.core.probes.*;

import maspack.util.*;
import maspack.matrix.*;
import maspack.geometry.*;
import maspack.render.*;
import maspack.render.Renderer.*;
import maspack.properties.*;

public class MeshCurveTest extends RootModel {

   public static boolean omitFromMenu = true;

   public void build (String[] args) {
      MechModel mech = new MechModel ("mech");
      addModel (mech);
      
      PolygonalMesh mesh = MeshFactory.createIcosahedralSphere (1.0, 3);
      RigidBody ball = RigidBody.createFromMesh ("balll",
         mesh, /*density*/1000, /*scale*/1.0);
      mech.addRigidBody (ball);
      MeshComponent mcomp = ball.getSurfaceMeshComp();


      FemModel3d fem = FemFactory.createHexGrid (null, 1.0, 1.0, 2.0, 3, 3, 9);
      fem.setSurfaceRendering (SurfaceRender.Shaded);
      fem.transformGeometry (new RigidTransform3d (0, 0, 1.9));
      RenderProps.setFaceColor (fem, new Color (0.8f, 0.8f, 1f));

      double tol = 1e-8;
      for (FemNode3d n : fem.getNodes()) {
         Point3d p = n.getPosition();
         if (Math.abs(p.z-2.9) < tol) {
            n.setDynamic (false);
         }
         else if (Math.abs(p.z-0.9) < tol) {
            mech.attachPoint (n, ball);
         }
      }
      mech.addModel (fem);

      mcomp.addMeshMarker (new Point3d (0, -1, 0));
      RenderProps.setSphericalPoints (mcomp.getMeshMarkers(), 0.05, Color.GREEN);

      MeshCurve curve = mcomp.addCurve();
      curve.addMarker (new Point3d (0.5, -0.7, 0));
      curve.addMarker (new Point3d (0, -1, 0.5));
      curve.addMarker (new Point3d (-0.5, -0.7, 0));
      curve.addMarker (new Point3d (0, -1, -0.5));
      curve.setInterpolation (MeshCurve.Interpolation.NATURAL_SPLINE);
      curve.setClosed (true);
      RenderProps.setSphericalPoints (mcomp.getCurves(), 0.05, Color.RED);
      RenderProps.setCylindricalLines (mcomp.getCurves(), 0.02, Color.WHITE);

      mcomp = fem.getSurfaceMeshComp();
      mcomp.addMeshMarker (new Point3d (0, -1, 1.9));
      RenderProps.setSphericalPoints (
         mcomp.getMeshMarkers(), 0.05, Color.MAGENTA);

      curve = mcomp.addCurve();
      curve.addMarker (new Point3d (0.25, -1, 1.6));
      curve.addMarker (new Point3d (0, -1, 2.9));
      curve.addMarker (new Point3d (-0.25, -1, 1.6));
      curve.setInterpolation (MeshCurve.Interpolation.B_SPLINE);
      RenderProps.setSphericalPoints (mcomp.getCurves(), 0.05, Color.GREEN);
      RenderProps.setCylindricalLines (mcomp.getCurves(), 0.02, Color.BLUE);
   }

}
