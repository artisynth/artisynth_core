package artisynth.core.femmodels;

import maspack.matrix.Vector3d;
import maspack.geometry.PolylineMesh;
import maspack.geometry.PolylineInterpolator;

public class Vector3dNodalField extends VectorNodalField<Vector3d> {

   /**
    * This constructor should not be called by applications, unless {@link
    * #scan} is called immediately after.
    */
   public Vector3dNodalField () {
      super (Vector3d.class);
   }

   public Vector3dNodalField (FemModel3d fem) {
      super (Vector3d.class, fem);
   }

   public Vector3dNodalField (FemModel3d fem, Vector3d defaultValue) {
      super (Vector3d.class, fem, defaultValue);
   }

   public Vector3dNodalField (String name, FemModel3d fem) {
      super (name, Vector3d.class, fem);
   }

   public Vector3dNodalField (
      String name, FemModel3d fem, Vector3d defaultValue) {
      super (name, Vector3d.class, fem, defaultValue);
   }

   public boolean hasParameterizedType() {
      return false;
   }

   public Vector3d getValue (int[] nodeNums, double[] weights) {
      Vector3d vec = new Vector3d();
      for (int i=0; i<nodeNums.length; i++) {
         vec.scaledAdd (weights[i], getValue(nodeNums[i]));
      }
      return vec;
   }

   public void setFromPolylines (PolylineMesh mesh, double rad) {
      PolylineInterpolator interp = new PolylineInterpolator(mesh);
      Vector3d dir = new Vector3d();
      for (FemNode3d n : myFem.getNodes()) {
         int nsegs =
            interp.computeAverageDirection(dir, n.getPosition(), rad);
         if (nsegs > 0) {
            setValue (n, dir);
         }
         else {
            setValue (n, myDefaultValue);
         }
      }
   }

}
