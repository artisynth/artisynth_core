package artisynth.demos.fem;

import maspack.geometry.PolygonalMesh;
import maspack.matrix.Point3d;
import maspack.render.color.RainbowColorMap;

public class ColoredFemBeam3d extends FemBeam3d {

   
   public void build(String[] args) {
      
      super.build (args);

      PolygonalMesh surf = myFemMod.getSurfaceMesh();
      surf.setVertexColoringEnabled ();
      
      RainbowColorMap rcm = new RainbowColorMap();
      Point3d pmin = new Point3d(Point3d.POSITIVE_INFINITY);
      Point3d pmax = new Point3d(Point3d.NEGATIVE_INFINITY);
      myFemMod.updateBounds(pmin, pmax);
      for (int i=0; i<surf.numVertices(); i++) {
         Point3d vpos = surf.getVertex(i).getPosition();
         surf.setColor(i, rcm.getColor((vpos.x-pmin.x)/(pmax.x-pmin.x)));
      }
   }
   
}
