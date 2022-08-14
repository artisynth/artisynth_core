package artisynth.core.femmodels;

import maspack.matrix.Vector3d;
import maspack.matrix.Point3d;
import maspack.geometry.PolylineMesh;
import maspack.geometry.PolylineInterpolator;

/**
 * A vector field, for vectors of type {@link Vector3d}, defined over an FEM
 * model, using values set at the element integration points. Values at other
 * points are obtained by interpolation within the elements nearest to those
 * points. Values at elements for which no explicit values have been set are
 * given by the field's <i>default value</i>.
 *
 * <p> For a given element {@code elem}, values should be specified for
 * <i>all</i> integration points, as returned by {@link
 * FemElement3dBase#getAllIntegrationPoints}. This includes the regular
 * integration points, as well as the <i>warping</i> point, which is located at
 * the element center and is used by corotated linear materials. Integration
 * point indices should be in the range {@code 0} to {@link
 * FemElement3dBase#numAllIntegrationPoints} - 1.
 */
public class Vector3dSubElemField extends VectorSubElemField<Vector3d> {

   /**
    * This constructor should not be called by applications, unless {@link
    * #scan} is called immediately after.
    */
   public Vector3dSubElemField () {
      super (Vector3d.class);
   }

   /**
    * Constructs a field for a given FEM model, with a default value of 0.
    *
    * @param fem FEM model over which the field is defined
    */
   public Vector3dSubElemField (FemModel3d fem) {
      super (Vector3d.class, fem);
   }

   /**
    * Constructs a field for a given FEM model and default value.
    * 
    * @param fem FEM model over which the field is defined
    * @param defaultValue default value for integration points which don't have
    * explicitly set values
    */
   public Vector3dSubElemField (FemModel3d fem, Vector3d defaultValue) {
      super (Vector3d.class, fem, defaultValue);
   }

   /**
    * Constructs a named field for a given FEM model, with a default value of 0.
    * 
    * @param name name of the field
    * @param fem FEM model over which the field is defined
    */
   public Vector3dSubElemField (String name, FemModel3d fem) {
      super (name, Vector3d.class, fem);
   }

   /**
    * Constructs a named field for a given FEM model and default value.
    *
    * @param name name of the field
    * @param fem FEM model over which the field is defined
    * @param defaultValue default value for integration points which don't have
    * explicitly set values
    */
   public Vector3dSubElemField (
      String name, FemModel3d fem, Vector3d defaultValue) {
      super (name, Vector3d.class, fem, defaultValue);
   }

  /**
    * {@inheritDoc}
    */
   public boolean hasParameterizedType() {
      return false;
   }

   /**
    * Sets the values for this field from a polyline mesh which specifies a
    * direction field. For each element integration, the mesh point nearest to
    * the point is found and then used to compute the vector value by averaging
    * directions within a given radius.
    *
    * @param mesh polyline mesh giving the direction field
    * @param rad direction averaging radius
    */
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
