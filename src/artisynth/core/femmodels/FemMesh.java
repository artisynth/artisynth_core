package artisynth.core.femmodels;

import artisynth.core.modelbase.*;
import maspack.geometry.*;

/**
 * Interface intended to combine FemMeshComp and FemCutPlane
 */
public interface FemMesh extends RenderableComponent {

   public MeshBase getMesh();

   public FemModel3d getFem();

   public boolean isMeshPolygonal();
}
