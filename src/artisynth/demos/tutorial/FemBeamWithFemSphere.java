package artisynth.demos.tutorial;

import java.io.IOException;

import maspack.matrix.RigidTransform3d;
import artisynth.core.femmodels.*;
import artisynth.core.materials.LinearMaterial;
import maspack.util.PathFinder;

public class FemBeamWithFemSphere extends FemBeam {

   public void build (String[] args) throws IOException {

      // Build simple FemBeam
      super.build (args);

      // Create a FEM sphere
      FemModel3d femSphere = new FemModel3d("sphere");
      mech.addModel(femSphere);
      // Read from TetGen file
      TetGenReader.read(femSphere, 
         PathFinder.getSourceRelativePath(FemModel3d.class, "meshes/sphere2.1.node"),
         PathFinder.getSourceRelativePath(FemModel3d.class, "meshes/sphere2.1.ele"));
      femSphere.scaleDistance(0.22);
      // FEM properties
      femSphere.setDensity(10);
      femSphere.setParticleDamping(2);
      femSphere.setMaterial(new LinearMaterial(4000, 0.33));
      
      // Reposition FEM to side of beam 
      femSphere.transformGeometry( new RigidTransform3d(length/2+width/2, 0, 0) );
      
      // Attach sphere nodes that are inside beam
      for (FemNode3d node : femSphere.getNodes()) {
         // Find element containing node (if exists) 
         FemElement3d elem = fem.findContainingElement(node.getPosition());
         // Add attachment if node is inside "fem"
         if (elem != null) {
            mech.attachPoint(node, elem);
         }
      }
      
      // Set render properties
      setRenderProps(femSphere);
      
   }

}
