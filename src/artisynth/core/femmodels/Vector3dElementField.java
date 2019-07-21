package artisynth.core.femmodels;

import maspack.matrix.Vector3d;
import maspack.matrix.Point3d;
import maspack.geometry.PolylineMesh;
import maspack.geometry.PolylineInterpolator;

public class Vector3dElementField extends VectorElementField<Vector3d> {

   /**
    * This constructor should not be called by applications, unless {@link
    * #scan} is called immediately after.
    */
   public Vector3dElementField () {
      super (Vector3d.class);
   }

   public Vector3dElementField (FemModel3d fem) {
      super (Vector3d.class, fem);
   }

   public Vector3dElementField (FemModel3d fem, Vector3d defaultValue) {
      super (Vector3d.class, fem, defaultValue);
   }

   public Vector3dElementField (String name, FemModel3d fem) {
      super (name, Vector3d.class, fem);
   }

   public Vector3dElementField (
      String name, FemModel3d fem, Vector3d defaultValue) {
      super (name, Vector3d.class, fem, defaultValue);
   }

   public boolean hasParameterizedType() {
      return false;
   }

   public void setFromPolylines (PolylineMesh mesh, double rad) {
      PolylineInterpolator interp = new PolylineInterpolator(mesh);
      Vector3d dir = new Vector3d();
      for (FemElement3dBase e : myFem.getAllElements()) {
         Point3d centroid = new Point3d();
         e.computeCentroid (centroid);
         int nsegs = interp.computeAverageDirection(dir, centroid, rad);
         if (nsegs > 0) {
            setValue (e, dir);
         }
      }
   }

}
