package artisynth.core.femmodels;

import maspack.matrix.Vector3d;
import maspack.matrix.Point3d;
import maspack.geometry.PolylineMesh;
import maspack.geometry.PolylineInterpolator;

public class Vector3dSubElemField extends VectorSubElemField<Vector3d> {

   /**
    * This constructor should not be called by applications, unless {@link
    * #scan} is called immediately after.
    */
   public Vector3dSubElemField () {
      super (Vector3d.class);
   }

   public Vector3dSubElemField (FemModel3d fem) {
      super (Vector3d.class, fem);
   }

   public Vector3dSubElemField (FemModel3d fem, Vector3d defaultValue) {
      super (Vector3d.class, fem, defaultValue);
   }

   public Vector3dSubElemField (String name, FemModel3d fem) {
      super (name, Vector3d.class, fem);
   }

   public Vector3dSubElemField (
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
         IntegrationPoint3d ipnts[] = e.getIntegrationPoints();
         Point3d pos = new Point3d();
         for (int k=0; k<ipnts.length; k++) {
            ipnts[k].computePosition (pos, e.getNodes());
            int nsegs = interp.computeAverageDirection(dir, pos, rad);
            if (nsegs > 0) {
               setValue (e, k, dir);
            }
         }
      }
   }

}
