package artisynth.demos.renderables;

import java.awt.Color;
import java.io.IOException;

import artisynth.core.renderables.EditablePolygonalMeshComp;
import artisynth.core.workspace.RootModel;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;

public class EditableMeshTest extends RootModel {

   
   @Override
   public void build (String[] args) throws IOException {
      super.build (args);
      
      
      Color colors[] = {Color.RED, Color.GREEN, Color.BLUE, Color.CYAN,
                        Color.GRAY, Color.MAGENTA, Color.ORANGE,
                        Color.PINK, Color.YELLOW,
                        Color.RED.darker().darker(), Color.GREEN.darker().darker(), 
                        Color.BLUE.darker().darker(), Color.CYAN.darker().darker(),
                        Color.GRAY.darker().darker(), Color.MAGENTA.darker().darker(), 
                        Color.ORANGE.darker().darker(), Color.PINK.darker().darker(), 
                        Color.YELLOW.darker().darker(),
                        Color.RED.brighter().brighter(), Color.GREEN.brighter().brighter(), 
                        Color.BLUE.brighter().brighter(), Color.CYAN.brighter().brighter(),
                        Color.GRAY.brighter().brighter(), Color.MAGENTA.brighter().brighter(), 
                        Color.ORANGE.brighter().brighter(), Color.PINK.brighter().brighter(), 
                        Color.YELLOW.brighter().brighter()};
      
      PolygonalMesh mesh = MeshFactory.createIcosahedralSphere (1, 0);
      mesh.setFeatureColoringEnabled ();
      for (int i=0; i<mesh.numColors (); ++i) {
         mesh.setColor (i, colors[i % colors.length]);
      }
      
      EditablePolygonalMeshComp comp = new EditablePolygonalMeshComp (mesh);
      
      addRenderable (comp);
      
   }
   
}
