package artisynth.core.fields;

import maspack.matrix.Vector3d;
import maspack.matrix.Point3d;
import maspack.geometry.PolylineMesh;
import artisynth.core.femmodels.FemElement3dBase;
import artisynth.core.femmodels.FemModel3d;
import maspack.geometry.PolylineInterpolator;

/**
 * A vector field, for vectors of type {@link Vector3d}, defined over an FEM
 * model, using values set at the elements.  Values at other points are
 * obtained by finding the elements nearest to those points. Values at element
 * for which no explicit value has been set are given by the field's <i>default
 * value</i>. Since values are assumed to be constant over a given element,
 * this field is not continuous.
 */
public class Vector3dElementField extends VectorElementField<Vector3d> {

   /**
    * This constructor should not be called by applications, unless {@link
    * #scan} is called immediately after.
    */
   public Vector3dElementField () {
      super (Vector3d.class);
   }

   /**
    * Constructs a field for a given FEM model, with a default value of 0.
    *
    * @param fem FEM model over which the field is defined
    */
   public Vector3dElementField (FemModel3d fem) {
      super (Vector3d.class, fem);
   }

   /**
    * Constructs a field for a given FEM model and default value.
    * 
    * @param fem FEM model over which the field is defined
    * @param defaultValue default value for elements which don't have
    * explicitly set values
    */
   public Vector3dElementField (FemModel3d fem, Vector3d defaultValue) {
      super (Vector3d.class, fem, defaultValue);
   }

   /**
    * Constructs a named field for a given FEM model, with a default value of 0.
    * 
    * @param name name of the field
    * @param fem FEM model over which the field is defined
    */
   public Vector3dElementField (String name, FemModel3d fem) {
      super (name, Vector3d.class, fem);
   }

   /**
    * Constructs a named field for a given FEM model and default value.
    *
    * @param name name of the field
    * @param fem FEM model over which the field is defined
    * @param defaultValue default value for elements which don't have
    * explicitly set values
    */
   public Vector3dElementField (
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
    * direction field. For each element, the mesh point nearest to the element
    * centroid is found and then used to compute the vector value by averaging
    * directions within a given radius.
    *
    * @param mesh polyline mesh giving the direction field
    * @param rad direction averaging radius
    */
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
