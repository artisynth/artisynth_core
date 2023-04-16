package artisynth.demos.test;

import java.io.*;
import java.awt.Color;
import java.util.ArrayList;

import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.*;
import artisynth.core.util.*;
import artisynth.core.workspace.RootModel;
import artisynth.core.gui.*;
import maspack.geometry.*;
import maspack.geometry.io.*;
import maspack.matrix.*;
import maspack.util.*;
import maspack.render.*;
import maspack.render.Renderer.Shading;
import maspack.properties.*;

public class MeshViewer extends RootModel {

   public void build (String[] args) {

      MechModel mech = new MechModel ("msmod");
      addModel (mech);

      ArrayList<String> meshFileNames = new ArrayList<>();

      for (int i=0; i<args.length; i++) {
         if (!args[i].startsWith ("-")) {
            meshFileNames.add (args[i]);
         }
         else {
            System.out.println ("Warning: unrecognized model option "+args[i]);
            System.out.println ("Usage: MeshViewer <meshFile> ...");
         }
      }

      if (meshFileNames.size() > 0) {
         for (String fileName : meshFileNames) {
            try {
               MeshReader reader = GenericMeshReader.createReader (fileName);
               MeshBase mesh = reader.readMesh(null);
               if (reader instanceof WavefrontReader) {
                  RenderProps props = ((WavefrontReader)reader).getRenderProps();
                  if (props != null) {
                     mesh.setRenderProps (props);
                  }
                  else {
                     System.out.println ("No render props specified");
                  }
               }
               mech.addMeshBody (new FixedMeshBody (mesh));
            }
            catch (Exception e) {
               System.out.println ("Can't read mesh " + fileName);
               e.printStackTrace(); 
            }
         }
      }
      else {
         PolygonalMesh mesh = MeshFactory.createIcosahedralSphere (1.0, 1);
         FixedMeshBody mbody = new FixedMeshBody ("mesh", mesh);
         mech.addMeshBody (mbody);
      }
   }
}



