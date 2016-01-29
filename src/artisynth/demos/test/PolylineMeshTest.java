package artisynth.demos.test;

import java.io.*;
import java.awt.Color;
import java.util.ArrayList;

import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.*;
import artisynth.core.workspace.RootModel;
import artisynth.core.gui.*;
import maspack.geometry.*;
import maspack.matrix.*;
import maspack.util.*;
import maspack.render.*;

public class PolylineMeshTest extends MeshTestBase {

   void addControlPanel (MeshComponent meshBody) {
      ControlPanel panel = createControlPanel (meshBody);
      panel.addWidget (meshBody, "renderProps.lineStyle");
      panel.addWidget (meshBody, "renderProps.lineColor");
      panel.addWidget (meshBody, "renderProps.lineRadius");
      panel.addWidget (meshBody, "renderProps.lineWidth");
      addControlPanel (panel);
   }

   public void build (String[] args) {

      RandomGenerator.setSeed (0x1234);

      MechModel msmod = new MechModel ("msmod");
      // PolygonalMesh mesh = MeshFactory.createTube (2, 4, 6, 20, 2, 6);
      PolylineMesh mesh = 
         MeshFactory.createSphericalPolyline (2.0, 50, 10);

      myMesh = mesh;
      setHasNormals (true);

      //System.out.println ("num vertices: " + mesh.numVertices());
      FixedMeshBody meshBody = new FixedMeshBody (mesh);
      RenderProps.setLineRadius (meshBody, 0.02);

      msmod.addMeshBody (meshBody);
      addModel (msmod);
      addControlPanel (meshBody);
   }
}
