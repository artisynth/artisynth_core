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
import maspack.render.*;
import maspack.properties.*;

public class PolygonalMeshTest extends MeshTestBase {

   public static PropertyList myProps =
      new PropertyList (PolygonalMeshTest.class, MeshTestBase.class);

   @Override
   public void setHasNormals (boolean enabled) {
      if (enabled != myMesh.hasNormals()) {
         if (enabled) {
            myMesh.clearNormals();
         }
         else {
            myMesh.setNormals (null, null);
         }
      }
   }

   void addControlPanel (MeshComponent meshBody) {
      ControlPanel panel = createControlPanel (meshBody);
      panel.addWidget (meshBody, "renderProps.faceStyle");
      panel.addWidget (meshBody, "renderProps.faceColor");
      panel.addWidget (meshBody, "renderProps.backColor");
      panel.addWidget (meshBody, "renderProps.drawEdges");
      panel.addWidget (meshBody, "renderProps.edgeColor");
      panel.addWidget (meshBody, "renderProps.edgeWidth");
      panel.addWidget (meshBody, "renderProps.lineColor");
      panel.addWidget (meshBody, "renderProps.lineWidth");
      addControlPanel (panel);
   }

   public void build (String[] args) {

      MechModel msmod = new MechModel ("msmod");
      PolygonalMesh polyMesh;

      // PolygonalMesh mesh = MeshFactory.createTube (2, 4, 6, 20, 2, 6);
      polyMesh = 
         //MeshFactory.createIcosahedralSphere (/*radius=*/2.0, /*divisions=*/2);
         MeshFactory.createSphere (/*radius=*/2.0, /*nsegs=*/24);
      myMesh = polyMesh;

      FixedMeshBody meshBody = new FixedMeshBody (polyMesh);
      msmod.addMeshBody (meshBody);
      addModel (msmod);
      addControlPanel (meshBody);
   }
}
