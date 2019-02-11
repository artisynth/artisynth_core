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

      String meshFileName = null;

      for (int i=0; i<args.length; i++) {
         if (args[i].equals ("-mesh")) {
            if (++i == args.length) {
               System.out.println (
                  "Warning: -mesh needs an extra argument; ignoring");
            }
            else {
               meshFileName = args[i];
            }
         }
         else {
            System.out.println ("Warning: unrecognized model option "+args[i]);
         }
      }

      MeshBase mesh = null;
      if (meshFileName != null) {
         try {
            mesh = GenericMeshReader.readMesh (meshFileName);
         }
         catch (Exception e) {
            System.out.println ("Can't read mesh " + meshFileName);
            e.printStackTrace(); 
         }
      }
      else {
         mesh = MeshFactory.createIcosahedralSphere (1.0, 1);
      }
      FixedMeshBody mbody = new FixedMeshBody ("mesh", mesh);
      mech.addMeshBody (mbody);
   }
}



