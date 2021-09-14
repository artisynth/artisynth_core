package artisynth.demos.test;

import java.awt.Color;
import java.io.*;
import java.util.*;

import artisynth.core.workspace.*;
import artisynth.core.mechmodels.*;
import artisynth.core.femmodels.*;
import artisynth.core.materials.*;
import artisynth.core.probes.*;

import maspack.util.*;
import maspack.matrix.*;
import maspack.geometry.*;
import maspack.render.*;
import maspack.render.Renderer.*;
import maspack.properties.*;

public class ArgsTest extends RootModel {

   public static boolean omitFromMenu = true;

   public void build (String[] args) {
      MechModel mech = new MechModel ("mech");
      addModel (mech);

      System.out.println ("Num args=" + args.length);
      for (int i=0; i<args.length; i++) {
         System.out.println (" "+args[i]);
      }
      
      PolygonalMesh mesh = MeshFactory.createSphere (1.0, 12);
      mech.addMeshBody (new FixedMeshBody (mesh));
   }

}
