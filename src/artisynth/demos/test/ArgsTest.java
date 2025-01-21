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


      double stiffness = 10;
      double damping = 1;
      for (int i=0; i<args.length; i++) {
         if (args[i].equals ("stiffness")) {
            if (++i == args.length) {
               System.out.println (
                  "WARNING: option 'stiffness' needs another argument");
            }
            stiffness = Double.valueOf (args[i]);
         }
         else if (args[i].equals ("damping")) {
            if (++i == args.length) {
               System.out.println (
                  "WARNING: option 'damping' needs another argument");
            }
            damping = Double.valueOf (args[i]);
         }
         else {
            System.out.println (
               "WARNING: unknown argument '"+args[i]+"'; ignoring");
         }
      }

      PolygonalMesh mesh = MeshFactory.createSphere (1.0, 12);
      mech.addMeshBody (new FixedMeshBody (mesh));
   }

}
