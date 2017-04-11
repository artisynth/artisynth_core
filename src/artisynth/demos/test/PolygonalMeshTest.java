package artisynth.demos.test;

import java.io.*;
import java.awt.Color;
import java.util.ArrayList;

import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.*;
import artisynth.core.workspace.RootModel;
import artisynth.core.gui.*;
import artisynth.core.util.*;
import maspack.geometry.*;
import maspack.matrix.*;
import maspack.render.*;
import maspack.collision.*;
import maspack.properties.*;

public class PolygonalMeshTest extends MeshTestBase {

   public static String rbpath =
      ArtisynthPath.getHomeRelativePath (
         "src/maspack/geometry/sampleData/", ".");

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

   PolygonalMesh readBoxMesh() {
      PolygonalMesh mesh = null;
      try {
         mesh = new PolygonalMesh (new File (rbpath + "box.obj"));
      }
      catch (Exception e) {
         System.out.println ("Can't read 'box.obj':");
         e.printStackTrace(); 
         System.exit(1); 
      }
      return mesh;
   }

   public void build (String[] args) {

      MechModel msmod = new MechModel ("msmod");
      PolygonalMesh mesh = null;

      //mesh = MeshFactory.createSphere (/*radius=*/2.0, /*nsegs=*/24);
      
      //mesh = readBoxMesh();
      mesh = MeshFactory.createSkylineMesh (
         1.0, 1.0, 0.1, 10, 10,
         "1111111111",
         "1        1",
         "1 111111 1",
         "1 1    1 1",
         "1 1 11 1 1",
         "1 1    1 1",
         "1 111111 1",
         "1        1",
         "1        1",
         "1111111111");

      myMesh = mesh;

      FixedMeshBody meshBody = new FixedMeshBody (mesh);
      msmod.addMeshBody (meshBody);
      addModel (msmod);
      addControlPanel (meshBody);
   }
}
