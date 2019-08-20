package artisynth.demos.test;

import java.awt.Color;
import java.io.*;
import java.util.*;

import artisynth.core.workspace.*;
import artisynth.core.mechmodels.*;
import artisynth.core.materials.*;
import artisynth.core.femmodels.*;

import maspack.util.*;
import maspack.matrix.*;
import maspack.geometry.*;
import maspack.render.*;
import maspack.render.Renderer.*;
import maspack.properties.*;

public class ShellVolumeAttach extends RootModel {

   public void build (String[] args) {

      boolean membrane = false;

      for (int i=0;i<args.length; i++) {
         if (args[i].equals ("-membrane")) {
            membrane = true;
         }
         else {
            System.out.println ("Warning: unrecognized argument "+args[i]);
         }
      }

      MechModel mech = new MechModel ("mech");
      addModel (mech);

      FemModel3d beam = FemFactory.createHexGrid (null, 1.0, 0.4, 0.4, 5, 3, 3);
      beam.setDensity (1000);
      beam.setMaterial (new NeoHookeanMaterial (500000.0, 0.45));
      mech.addModel (beam);

      FemModel3d sheet = FemFactory.createShellQuadGrid (
         null, 1.0, 0.4, 5, 2, 0.01, membrane);
      sheet.transformGeometry (new RigidTransform3d (1.0, 0, 0));
      sheet.setDensity (1000);
      if (membrane) {
         sheet.setMaterial (new NeoHookeanMaterial (500000.0, 0.45));
      }
      else {
         sheet.setMaterial (new NeoHookeanMaterial (5000000.0, 0.45));
      }
      sheet.setDirectorRenderLen (10.0);
      mech.addModel (sheet);

      beam.setSurfaceRendering (FemModel.SurfaceRender.Shaded);
      sheet.setSurfaceRendering (FemModel.SurfaceRender.Shaded);
      RenderProps.setFaceColor (sheet, new Color (0.6f, 1f, 0.6f));
      RenderProps.setFaceColor (beam, new Color (0.6f, 0.6f, 1f));
      RenderProps.setFaceStyle (sheet, FaceStyle.FRONT_AND_BACK);

      double EPS = 1e-8;
      for (FemNode3d n : beam.getNodes()) {
         if (Math.abs(n.getPosition().x-(-0.5)) <= EPS) {
            n.setDynamic (false);
         }
      }
      
      for (FemNode3d n : sheet.getNodes()) {
         if (Math.abs(n.getPosition().x-(0.5)) <= EPS) {
            mech.addAttachment (new ShellNodeFem3dAttachment (n, beam));
         }
      }
      
   }

}
