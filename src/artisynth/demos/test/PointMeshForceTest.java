package artisynth.demos.test;

import java.awt.Color;
import java.io.*;
import java.util.*;

import artisynth.core.workspace.*;
import artisynth.core.mechmodels.*;
import artisynth.core.mechmodels.PointMeshForce.*;
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

/**
 * A simple model demonstrating a small FEM model colliding with a bowl shaped
 * mesh using a PointMeshForce.
 */
public class PointMeshForceTest extends RootModel {

   private String bowlMeshPath = PathFinder.expand (
      "$ARTISYNTH_HOME/src/maspack/geometry/sampleData/bowl.obj");

   public void build (String[] args) throws IOException {

      // use a linear force type by default:
      ForceType ftype = ForceType.LINEAR;
      // stiffness and damping values for the contact forces:
      double contactStiffness = 100000;
      double contactDamping = 10;

      // parse arguments to see if quadratic forces are requested
      for (int i=0; i<args.length; i++) {
         if (args[i].equals ("-quadratic")) {
            ftype = ForceType.QUADRATIC;
            contactStiffness *= 10.0;
         }
         else {
            System.out.println ("WARNING: unknown argument "+args[i]);
         }
      }

      // create a MechModel and add it to the root model
      MechModel mech = new MechModel ("mech");
      addModel (mech);

      // create the bowl for the FEM to drop into
      PolygonalMesh mesh = new PolygonalMesh (bowlMeshPath);
      mesh.triangulate(); // mesh must be triangulated
      FixedMeshBody mcomp = new FixedMeshBody (mesh);
      // orient the bowl properly
      mcomp.setPose (new RigidTransform3d (0, 0, 0, 0, 0, Math.PI/2));
      mech.addMeshBody (mcomp);

      // Create a small FEM model consisting of single hex element
      FemModel3d fem = FemFactory.createHexGrid (
         null, 0.5, 0.5, 0.5, 1, 1, 1);
      fem.setName ("cube");
      fem.setMaterial (new LinearMaterial (50000.0, 0.4));
      fem.setParticleDamping (1.0);
      fem.setSurfaceRendering (SurfaceRender.Shaded);
      RenderProps.setFaceColor (fem, new Color (0.7f, 0.7f, 1f));
      mech.addModel (fem);

      // Add an FEM marker close to each FEM node. We do this to demonstrate
      // that markers can be used instead of nodes to provide collision
      // interactions with the mesh.
      for (FemNode3d n : fem.getNodes()) {
         Point3d pos = new Point3d(n.getPosition());
         pos.scale (0.9);
         fem.addMarker (pos);
      }
      RenderProps.setSphericalPoints (fem.markers(), 0.04, Color.RED);
      // position the FEM to drop into the bowl
      fem.transformGeometry (new RigidTransform3d (0.3, 0, 1.5));      

      // Create a PointMeshForce to produce collision interaction between the
      // bowl and the FEM model. Use the FEM markers as the mesh-colliding
      // points.
      PointMeshForce pmf =
         new PointMeshForce ("pmf", mcomp, contactStiffness, contactDamping);
      for (FemMarker m : fem.markers()) {
         pmf.addPoint (m);
      }
      pmf.setForceType (ftype);
      mech.addForceEffector (pmf);
   }

}
