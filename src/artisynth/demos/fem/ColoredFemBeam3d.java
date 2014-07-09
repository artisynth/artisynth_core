package artisynth.demos.fem;

import maspack.geometry.PolygonalMesh;
import maspack.matrix.Point3d;
import maspack.render.color.RainbowColorMap;

public class ColoredFemBeam3d extends FemBeam3d {

   public ColoredFemBeam3d () {
      this("ColoredFemBeam3d");
   }
   
   public ColoredFemBeam3d(String name) {
      super(name);
      
      PolygonalMesh surf = myFemMod.getSurfaceMesh();
      surf.setUseVertexColoring(true);
      
      RainbowColorMap rcm = new RainbowColorMap();
      Point3d pmin = new Point3d(Point3d.POSITIVE_INFINITY);
      Point3d pmax = new Point3d(Point3d.NEGATIVE_INFINITY);
      myFemMod.updateBounds(pmin, pmax);
      for (int i=0; i<surf.getNumVertices(); i++) {
         Point3d vpos = surf.getVertex(i).getPosition();
         surf.setVertexColor(i, rcm.getColor((vpos.x-pmin.x)/(pmax.x-pmin.x)));
      }
      
      
   }
   
}
