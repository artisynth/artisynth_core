package artisynth.demos.fem;

import java.awt.Color;

import maspack.matrix.*;
import maspack.geometry.*;
import maspack.util.*;
import maspack.render.RenderProps;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.femmodels.*;
import artisynth.core.mechmodels.Collidable.Collidability;
import artisynth.core.mechmodels.*;
import artisynth.core.workspace.*;
import artisynth.demos.fem.FemBeam3d;

public class EmbeddedEmbeddedCollide extends RootModel {

   public void build (String[] args) {
      double friction = 0;

      for (int i=0; i<args.length; i++) {
         if (args[i].equals ("-friction")) {
            if (i == args.length-1) {
               System.out.println (
                  "WARNING: option -friction requires a numeric argument");
            }
            else {
               friction = Double.valueOf (args[++i]);
            }
         }
         else {
            System.out.println ("WARNING: unknown option "+args[i]);
         }
      }

      MechModel mech = new MechModel ("mech");
      addModel (mech);

      double size = 1.0;

      // create grid for the bowl
      FemModel3d bowlFem = FemFactory.createHexGrid (
         null, size, size, size/2, 8, 4, 4);
      // fix the lower nodes
      for (FemNode3d n : bowlFem.getNodes()) {
         if (Math.abs(n.getPosition().z+size/4) < 1e-8) {
            n.setDynamic (false);
         }
      }
      // add the bowl mesh
      String meshFileName = PathFinder.expand (
         "$ARTISYNTH_HOME/src/maspack/geometry/sampleData/bowl.obj");
      PolygonalMesh bowl = null;
      try {
         bowl = new PolygonalMesh (meshFileName);
      }
      catch (Exception e) {
         System.out.println (e);
      }
      bowl.transform (new RigidTransform3d (0, -size/2, 0, 0, 0, 0));
      bowl.transform (new RigidTransform3d (0, 0, 0, 0, 0, Math.PI/2));
      bowl.scale (0.5);
      bowl.triangulate();
      bowlFem.addMesh (bowl);
      mech.addModel(bowlFem);

      // create grid for the sphere
      FemModel3d sphereFem = FemFactory.createHexGrid (
         null, size/2, size/2, size/2, 4, 4, 4);
      // add the sphere mesh
      PolygonalMesh sphere = MeshFactory.createIcosahedralSphere (size/6, 1);
      sphereFem.addMesh (sphere);
      sphereFem.transformGeometry (new RigidTransform3d (0, 0, 0.75*size));
      mech.addModel(sphereFem);

      FemMeshComp sphereComp = sphereFem.getMeshComp(1);
      FemMeshComp bowlComp = bowlFem.getMeshComp(1);

      sphereComp.setCollidable (Collidability.ALL);
      bowlComp.setCollidable (Collidability.ALL);

      mech.setCollisionBehavior (sphereComp, bowlComp, true, friction);
      //mech.getCollisionManager().setReduceConstraints(true);

      RenderProps.setLineColor (mech, Color.BLUE);
      RenderProps.setSphericalPoints (mech, 0.005, Color.GREEN);

      if (mech.getUseImplicitFriction()) {
         // need compliant contact if implicit friction is set
         mech.setCompliantContact();
      }
   }
   
}

