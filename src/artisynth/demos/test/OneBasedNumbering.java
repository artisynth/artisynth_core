package artisynth.demos.test;

import java.awt.Color;

import maspack.matrix.Point3d;
import maspack.render.RenderProps;
import artisynth.core.workspace.*;
import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.*;

public class OneBasedNumbering extends RootModel {

   public void build (String[] args) {
      MechModel mech = new MechModel ("mech");
      addModel (mech);

      PointList points = new PointList(Point.class, "xpoints");
      points.setZeroBasedNumbering (false);
      mech.add (points);

      points.add (new Particle (1, new Point3d (0, 0, 0)));
      points.add (new Particle (1, new Point3d (1, 0, 0)));
      points.add (new Particle (1, new Point3d (2, 0, 0)));
      points.add (new Particle (1, new Point3d (3, 0, 0)));

      RenderProps.setSphericalPoints (mech, 0.05, Color.GREEN);
   }

}
