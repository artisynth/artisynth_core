package artisynth.demos.mech;

import java.io.*;
import java.awt.Color;
import java.util.ArrayList;

import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.*;
import artisynth.core.workspace.RootModel;
import maspack.geometry.*;
import maspack.matrix.*;
import maspack.render.*;

public class ColoredMeshDemo extends RootModel {

   public ColoredMeshDemo() {
      super (null);
   }

   public ColoredMeshDemo (String name) {
      this();
      setName (name);

      MechModel msmod = new MechModel ("msmod");
      // PolygonalMesh mesh = MeshFactory.createTube (2, 4, 6, 20, 2, 6);
      PolygonalMesh mesh = 
         //MeshFactory.createIcosahedralSphere (/*radius=*/2.0, /*divisions=*/2);
         MeshFactory.createSphere (/*radius=*/2.0, /*nsegs=*/24);

      ArrayList<float[]> colors = new ArrayList<float[]>();
      colors.add (new float[] { 1.0f, 0.0f, 0.0f });
      colors.add (new float[] { 0.0f, 0.5f, 0.0f });
      colors.add (new float[] { 0.0f, 0.0f, 0.8f });
      // int[] indices = mesh.getFeatureIndices();
      // for (int i=0; i<indices.length/3; i++) {
      //    int k = i%3;
      //    indices[3*i  ] = k;
      //    indices[3*i+1] = k;
      //    indices[3*i+2] = k;
      // }
      // mesh.setColors (colors, indices);

      mesh.setFeatureColoringEnabled();
      for (int i=0; i<mesh.numFaces(); i++) {
         mesh.setColor (i, colors.get(i%3));
      }

      FixedMeshBody meshBody = new FixedMeshBody (mesh);
      msmod.addMeshBody (meshBody);
      addModel (msmod);
   }
}
