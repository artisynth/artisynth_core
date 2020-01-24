package artisynth.demos.inverse;

import java.io.IOException;


public class PointModel3d extends PointModel {

   // need this because PointModel sets omitFromMenu=true
   public static boolean omitFromMenu = false;

   public void build(String[] args) throws IOException {
      build (DemoType.Point3d);
   }


}
