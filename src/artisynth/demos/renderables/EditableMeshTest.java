package artisynth.demos.renderables;

import java.io.IOException;

import artisynth.core.renderables.EditablePolygonalMeshComp;
import artisynth.core.workspace.RootModel;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;

public class EditableMeshTest extends RootModel {

   
   @Override
   public void build (String[] args) throws IOException {
      super.build (args);
      
      PolygonalMesh mesh = MeshFactory.createIcosahedralSphere (1, 0);
      EditablePolygonalMeshComp comp = new EditablePolygonalMeshComp (mesh);
      
      addRenderable (comp);
      
   }
   
}
