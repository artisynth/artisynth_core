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

public class PointPlaneForceTest extends RootModel {

   public static boolean omitFromMenu = true;

   public void build (String[] args) {
      MechModel mech = new MechModel ("mech");
      addModel (mech);

      Particle p = new Particle ("part", 1, 1.0, 0, 2.0);
      mech.addParticle (p);
      PointPlaneForce ppf0 =
         new PointPlaneForce (p, new Vector3d (1, 0, 1), Point3d.ZERO);
      ppf0.setStiffness (1000);
      ppf0.setPlaneSize (5.0);
      ppf0.setUnilateral (true);
      PointPlaneForce ppf1 =
         new PointPlaneForce (p, new Vector3d (-1, 0, 1), Point3d.ZERO);
      mech.addForceEffector (ppf0);
      mech.addForceEffector (ppf1);
      ppf1.setStiffness (1000);
      ppf1.setPlaneSize (5.0);
      ppf1.setUnilateral (true);

      RenderProps.setSphericalPoints (mech, 0.1, Color.RED);
   }

}
