package artisynth.demos.test;

import java.awt.Color;
import java.io.*;
import java.util.*;

import artisynth.core.workspace.*;
import artisynth.core.mechmodels.*;
import artisynth.core.gui.*;
import artisynth.core.modelbase.*;
import artisynth.core.femmodels.*;
import artisynth.core.materials.*;
import artisynth.core.probes.*;

import maspack.util.*;
import maspack.matrix.*;
import maspack.geometry.*;
import maspack.render.*;
import maspack.render.Renderer.*;
import maspack.properties.*;

public class Template extends RootModel {

   public static boolean omitFromMenu = true;

   public static PropertyList myProps =
      new PropertyList (Template.class, RootModel.class);

   static {
      //myProps.add ("attachment * *", "point constraint enabled", false);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public void build (String[] args) {
      MechModel mech = new MechModel ("mech");
      addModel (mech);

      String dataDir = PathFinder.getSourceRelativePath (this, "elahehMesh/");
      
      PolygonalMesh mesh = MeshFactory.createSphere (1.0, 12);
      mech.addMeshBody (new FixedMeshBody (mesh));
   }

   public StepAdjustment advance (double t0, double t1, int flags) {
      StepAdjustment sa = super.advance (t0, t1, flags);
      return sa;
   }

}
