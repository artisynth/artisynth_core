package artisynth.demos.fem;

import java.awt.Color;

import maspack.matrix.*;
import maspack.render.*;
import artisynth.core.util.*;
import artisynth.core.femmodels.*;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.mechmodels.*;
import artisynth.core.modelbase.*;

public class Hex3dBlockStability extends Fem3dBlock {

   public void build (String[] args) {
      build ("hex", 9, 3, 0);
      //super (name, "hex", 1, 1, 0);
      MechModel mechMod = (MechModel)findComponent ("models/mech");
      FemModel3d fem = (FemModel3d)mechMod.findComponent ("models/fem");
      mechMod.setDynamicsEnabled (false);
      RenderProps.setFaceColor (fem, new Color (0.8f, 0.8f, 1.0f));
      fem.setSurfaceRendering (SurfaceRender.Shaded);
   }

   public StepAdjustment advance (double t0, double t1, int flags) {
      MechModel mechMod = (MechModel)findComponent ("models/mech");
      if (t1 == 0.5) {
         mechMod.setDynamicsEnabled (true);
      }
      return super.advance (t0, t1, flags);
   }   
}
