package artisynth.core.opensim.components;

import org.w3c.dom.Element;

public class MeshFactory extends GeometryFactory<Mesh> {
   
   public MeshFactory() {
      super(Mesh.class);
   }
   
   protected MeshFactory(Class<? extends Mesh> instanceClass) {
      super(instanceClass);
   }
   
   @Override
   protected boolean parseChild (Mesh fg, Element child) {
      boolean success = true;

      String cname = getNodeName(child);
      
      if ("mesh_file".equals(cname)) {
         fg.setMeshFile (parseTextValue(child));
      } else {
         success = super.parseChild (fg, child);
      }

      return success;
   }
   
}
