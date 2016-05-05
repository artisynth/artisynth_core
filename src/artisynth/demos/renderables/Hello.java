package artisynth.demos.renderables;

import java.awt.Color;
import java.io.IOException;

import artisynth.core.mechmodels.FixedMeshBody;
import artisynth.core.modelbase.StepAdjustment;
import artisynth.core.renderables.HudPrintStream;
import artisynth.core.util.TimeBase;
import artisynth.core.workspace.RootModel;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.Point3d;
import maspack.render.RenderProps;

public class Hello extends RootModel {
   
   HudPrintStream hud;
   
   String lyrics = "Hello, it's me\n" +
   "I was wondering if after all these years you'd like to meet\n" +
   "To go over everything\n" +
   "They say that time's supposed to heal ya\n" +
   "But I ain't done much healing\n" +
   "\n" +
   "Hello, can you hear me?\n" +
   "I'm in California dreaming about who we used to be\n" +
   "When we were younger and free\n" +
   "I've forgotten how it felt before the world fell at our feet\n" +
   "\n" +
   "There's such a difference between us\n" +
   "And a million miles\n" +
   "\n" +
   "Hello from the other side\n" +
   "I must have called a thousand times\n" +
   "To tell you I'm sorry for everything that I've done\n" +
   "But when I call you never seem to be home\n" +
   "Hello from the outside\n" +
   "At least I can say that I've tried\n" +
   "To tell you I'm sorry for breaking your heart\n" +
   "But it don't matter. It clearly doesn't tear you apart anymore\n";
   String[] lyricsSplit = lyrics.split ("\n");
   
   @Override
   public void build (String[] args) throws IOException {
      super.build (args);
      
      hud = new HudPrintStream ();
      hud.setNumDisplayLines (5);
      hud.setTextSize (12);
      RenderProps.setFaceColor (hud, Color.GREEN);
      addRenderable (hud);
      
      PolygonalMesh mesh = MeshFactory.createBox (1, 1, 1, Point3d.ZERO, 5, 5, 5);
      FixedMeshBody fmb = new FixedMeshBody (mesh);
      addRenderable (fmb);

   }

   @Override
   public StepAdjustment advance (double t0, double t1, int flags) {
      if (TimeBase.modulo (t0, 1) == 0) {
         hud.println (lyricsSplit[(int)t0 % lyricsSplit.length]);
      }
      return super.advance (t0, t1, flags);
   }
   
}
