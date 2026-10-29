package artisynth.demos.test;

import java.awt.Color;
import java.io.*;
import java.util.*;

import artisynth.core.workspace.*;
import artisynth.core.mechmodels.*;
import artisynth.core.mechmodels.MechSystemSolver.*;
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

/**
 * Model used to generate constrained FE solve data for analyzing solver
 * performance. Consists of a partial tubular FE wrapped around a cylinder.
 * Nodes adjacent to the cylinder at attached to it via bilateral constraints
 * enforced by a ParticleMeshConstraint.
 */
public class ConstrainedTubeFem extends RootModel {

   public void build (String[] args) {

      int nl = 30;
      int ntheta = 30;
      int nr = 4;
      double rin = 0.5;
      double rout = 0.65;
      double len = 1.0;

      String size = "medium"; // problem size
      boolean writeData = false; // write solve data to file
      boolean murtySolve = false; // use the MurtySolver

      for (int i=0; i<args.length; i++) {
         switch (args[i]) {
            case "-small": {
               nl = 15;
               ntheta = 15;
               nr = 3;
               size = "small";
               break;
            }
            case "-medium": {
               size = "medium";
               break;
            }
            case "-large": {
               nl = 60;
               ntheta = 60;
               nr = 5;
               size = "large";
               break;
            }
            case "-writeData": {
               writeData = true;
               break;
            }
            case "-murtySolve": {
               murtySolve = true;
               break;
            }
            default: {
               System.out.println ("WARNING: unknown argument '"+args[i]+"'");
            }
         }
      }

      MechModel mech = new MechModel ("mech");
      mech.setStabilization (PosStabilization.GlobalStiffness);
      addModel (mech);

      FemModel3d fem = FemFactory.createPartialHexTube (         
         null, len, rin, rout, /*theta*/3*Math.PI/4, nl, nr, ntheta);

      PolygonalMesh cylinder =
         MeshFactory.createCylinder (rin, 1.2*len, 64);

      FixedMeshBody mbod = new FixedMeshBody (cylinder);

      mech.addMeshBody (mbod);
      mech.addModel (fem);
      
      mech.transformGeometry (
         new RigidTransform3d (0, 0, 0, 0, -Math.PI/4, Math.PI/2));

      ParticleMeshConstraint pcons = new ParticleMeshConstraint();
      pcons.setMesh (cylinder, null, null);
      for (FemNode3d n : fem.getNodes()) {
         Point3d p = n.getPosition();
         if (Math.abs(Math.sqrt(p.x*p.x+p.z*p.z)-rin) < 1e-8) {
            pcons.addParticle (n);
         }
      }
      mech.addConstrainer (pcons);

      System.out.println (size + " problem size, "+fem.numNodes()+" nodes");

      if (writeData) {
         mech.getSolver().setCrsFileName ("ctube_"+size+".txt");
         addBreakPoint (1.0);
      }
      if (murtySolve) {
         mech.setUseImplicitFriction (true);
      }
   }

}
