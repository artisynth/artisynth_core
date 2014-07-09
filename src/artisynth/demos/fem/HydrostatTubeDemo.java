package artisynth.demos.fem;

import java.io.IOException;
import artisynth.demos.fem.HydrostatModel.Element;
import artisynth.demos.fem.HydrostatModel.Shape;

public class HydrostatTubeDemo extends HydrostatDemo {

   public HydrostatTubeDemo() {
      super();
   }

   public HydrostatTubeDemo (String name) throws IOException {
      this();
      setName (name);

      if (simple) {
         hydro =
            new HydrostatModel (
               "hydrostat", Element.Tet, Shape.Tube, 100, 40, 4, 2, true);
      }
      else {
         hydro =
            new HydrostatModel (
               "hydrostat", Element.Tet, Shape.Tube, 100, 30, 6, 3, false);
      }
      addModel (hydro);

      workspaceDirname = "hydrostat_tube";

      panelNames =
         new String[] { "hydrostat_fem", "hydrostat_tube_groups",
                       "hydrostat_tube_exciters" };

   }

}
