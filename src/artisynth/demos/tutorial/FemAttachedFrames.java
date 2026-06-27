package artisynth.demos.tutorial;

import java.awt.Color;
import java.io.IOException;

import artisynth.core.femmodels.FemAttachedFrame;
import maspack.matrix.RigidTransform3d;
import maspack.render.RenderProps;
import maspack.render.Renderer.AxisDrawStyle;

public class FemAttachedFrames extends FemBeam {

   public void build (String[] args) throws IOException {

      // build the base FE beam model
      super.build (args);

      // Add FemAttachedFrames at evenly-spaced positions along the beam
      int numFrames = 5;
      for (int i = 0; i < numFrames; i++) {
         double x = -length/2 + (i + 0.5) * length / numFrames;
         RigidTransform3d TFW = new RigidTransform3d (x, 0, 0);
         FemAttachedFrame frm = fem.addAttachedFrame (TFW);
         frm.setName ("frame" + i);
         frm.setAxisLength (0.12);
         frm.setAxisDrawStyle (AxisDrawStyle.ARROW);
      }
      // Make the FE model surface transparent so we can see the frames
      RenderProps.setAlpha (fem, 0.4);
   }
}
