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

public class ShellShellAttach extends RootModel {

   public void build (String[] args) {
      MechModel mech = new MechModel ("mech");
      addModel (mech);

      FemModel3d sheet1 = FemFactory.createShellQuadGrid (
         null, 1.0, 0.4, 5, 2, 0.01, false);
      sheet1.setDensity (1000);
      sheet1.setMaterial (new LinearMaterial (5000000.0, 0.45));
      //sheet1.setMaterial (new NeoHookeanMaterial (5000000.0, 0.45));
      sheet1.setDirectorRenderLen (10.0);
      mech.addModel (sheet1);

      FemModel3d sheet2 = FemFactory.createShellQuadGrid (
         null, 1.0, 0.4, 5, 2, 0.01, false);
      sheet2.transformGeometry (new RigidTransform3d (1.0, 0, 0));
      sheet2.setDensity (1000);
      sheet2.setMaterial (new LinearMaterial (5000000.0, 0.45));
      //sheet2.setMaterial (new NeoHookeanMaterial (5000000.0, 0.45));
      sheet2.setDirectorRenderLen (10.0);
      mech.addModel (sheet2);

      sheet1.setSurfaceRendering (FemModel.SurfaceRender.Shaded);
      sheet2.setSurfaceRendering (FemModel.SurfaceRender.Shaded);
      RenderProps.setFaceColor (sheet2, new Color (0.6f, 1f, 0.6f));
      RenderProps.setFaceColor (sheet1, new Color (0.6f, 0.6f, 1f));
      RenderProps.setFaceStyle (sheet2, FaceStyle.FRONT_AND_BACK);
      RenderProps.setFaceStyle (sheet1, FaceStyle.FRONT_AND_BACK);
      RenderProps.setSphericalPoints (sheet2, 0.01, Color.GREEN);
      RenderProps.setSphericalPoints (sheet1, 0.01, Color.BLUE);


      double EPS = 1e-8;
      for (FemNode3d n : sheet1.getNodes()) {
         if (Math.abs(n.getPosition().x-(-0.5)) <= EPS) {
            n.setDynamic (false);
         }
      }
      
      for (FemNode3d n : sheet2.getNodes()) {
         if (Math.abs(n.getPosition().x-(0.5)) <= EPS) {
            mech.addAttachment (new ShellNodeFem3dAttachment (n, sheet1));
         }
      }
      
   }

}
