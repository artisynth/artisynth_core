package artisynth.demos.test;

import java.awt.Color;
import java.io.*;
import java.util.*;

import artisynth.core.workspace.*;
import artisynth.core.mechmodels.*;
import artisynth.core.mechmodels.PointPlaneForce.*;
import artisynth.core.femmodels.*;
import artisynth.core.materials.*;
import artisynth.core.probes.*;

import maspack.util.*;
import maspack.matrix.*;
import maspack.geometry.*;
import maspack.render.*;
import maspack.render.Renderer.*;
import maspack.properties.*;

public class PointPlaneForceTest extends RootModel {

   public void build (String[] args) {
      ForceType ftype = ForceType.LINEAR;
      double stiffness = 1000;

      for (int i=0; i<args.length; i++) {
         if (args[i].equals ("-quadratic")) {
            ftype = ForceType.QUADRATIC;
            stiffness *= 10.0;
         }
         else {
            System.out.println ("WARNING: unknown argument "+args[i]);
         }
      }

      MechModel mech = new MechModel ("mech");
      addModel (mech);

      Particle p = new Particle ("part", 1, 1.0, 0, 2.0);
      mech.addParticle (p);
      PointPlaneForce ppf0 =
         new PointPlaneForce (p, new Vector3d (1, 0, 1), Point3d.ZERO);
      ppf0.setForceType (ftype);
      ppf0.setStiffness (stiffness);
      ppf0.setPlaneSize (5.0);
      ppf0.setUnilateral (true);
      PointPlaneForce ppf1 =
         new PointPlaneForce (p, new Vector3d (-1, 0, 1), Point3d.ZERO);
      ppf1.setForceType (ftype);
      mech.addForceEffector (ppf0);
      mech.addForceEffector (ppf1);
      ppf1.setStiffness (stiffness);
      ppf1.setPlaneSize (5.0);
      ppf1.setUnilateral (true);

      RenderProps.setSphericalPoints (mech, 0.1, Color.RED);
   }

}
