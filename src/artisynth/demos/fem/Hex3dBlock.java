package artisynth.demos.fem;

import java.awt.Color;

import maspack.matrix.*;
import maspack.render.*;
import artisynth.core.util.*;
import artisynth.core.femmodels.*;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.mechmodels.*;
import artisynth.core.modelbase.*;

public class Hex3dBlock extends Fem3dBlock {

   // delay start is used when making an instability video
   double startDelay = 0.0;

   public void build (String[] args) {
      boolean reduced = false;
      for (int i=0; i<args.length; i++) {
         if (args[i].equals ("-reduced")) {
            reduced = true;
         }
         else {
            System.out.println (
               "WARNING: unrecognized model option '" + args[i] + "'");
         }
      }
      if (reduced) {
         build ("hex", 3, 1, 0);
      }
      else {
         build ("hex", 9, 3, 0);
      }
      FemModel3d fem = (FemModel3d)findComponent ("models/mech/models/fem");
      fem.setSurfaceRendering (SurfaceRender.Shaded);
      RenderProps.setFaceColor (fem, new Color (0.8f, 0.8f, 1.0f));
   }

   public StepAdjustment advance (double t0, double t1, int flags) {
      MechModel mechMod = (MechModel)findComponent ("models/mech");
      if (startDelay > 0) {
         mechMod.setDynamicsEnabled (t0 >= startDelay);
      }
      return super.advance (t0, t1, flags);
   }   

}
