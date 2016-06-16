package artisynth.demos.mech;

import java.awt.Color;
import java.io.IOException;

import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.render.Renderer.LineStyle;
import maspack.render.Renderer.PointStyle;
import artisynth.core.mechmodels.FrameMarker;
import artisynth.core.mechmodels.PointForce;

public class PointForceDemo extends FrameSpringDemo {

   public void build (String[] args) throws IOException {
      super.build (args);

      if (myMechMod == null || myHand2 == null) {
         System.err.println ("null mechmodel of hand2");
         return;
      }

      FrameMarker m = new FrameMarker ("ee");
      myMechMod.addFrameMarker (m, myHand2,
                                new Point3d (0, 0, myHand2.getPose().p.z / 3));

      RenderProps.setPointStyle (m, Renderer.PointStyle.SPHERE);
      RenderProps.setPointColor (m, Color.RED);
      RenderProps.setPointRadius (m, 0.01);

      double magnitude = 0.001;
      Vector3d fext = new Vector3d (-1, 0, 0);
      // m.setExternalForce (fext);
      PointForce pf = new PointForce (fext, m);
      pf.setMagnitude (magnitude);
      pf.setForceScaling(1000);  // mm spatial units
      pf.setAxisLength (0.1);
      RenderProps.setLineStyle (pf, LineStyle.CYLINDER);
      RenderProps.setLineRadius (pf, pf.getAxisLength() / 20);
      RenderProps.setLineColor (pf, Color.GREEN);
      myMechMod.addForceEffector (pf);
   }

}
