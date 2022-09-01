package artisynth.core.fields;

import maspack.matrix.Vector3d;
import maspack.geometry.PolylineMesh;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.FemNode3d;
import maspack.geometry.PolylineInterpolator;

/**
 * A vector field, for vectors of type {@link Vector3d}, defined over an FEM
 * model, using values set at the nodes. Values at other points are obtained by
 * nodal interpolation on the elements nearest to those points. Values at nodes
 * for which no explicit value has been set are given by the field's <i>default
 * value</i>.
 */
public class Vector3dNodalField extends VectorNodalField<Vector3d> {

   /**
    * This constructor should not be called by applications, unless {@link
    * #scan} is called immediately after.
    */
   public Vector3dNodalField () {
      super (Vector3d.class);
   }

   /**
    * Constructs a field for a given FEM model, with a default value of 0.
    *
    * @param fem FEM model over which the field is defined
    */
   public Vector3dNodalField (FemModel3d fem) {
      super (Vector3d.class, fem);
   }

   /**
    * Constructs a field for a given FEM model and default value.
    * 
    * @param fem FEM model over which the field is defined
    * @param defaultValue default value for nodes which don't have
    * explicitly set values
    */
   public Vector3dNodalField (FemModel3d fem, Vector3d defaultValue) {
      super (Vector3d.class, fem, defaultValue);
   }

   /**
    * Constructs a named field for a given FEM model, with a default value of 0.
    * 
    * @param name name of the field
    * @param fem FEM model over which the field is defined
    */
   public Vector3dNodalField (String name, FemModel3d fem) {
      super (name, Vector3d.class, fem);
   }

   /**
    * Constructs a named field for a given FEM model and default value.
    *
    * @param name name of the field
    * @param fem FEM model over which the field is defined
    * @param defaultValue default value for nodes which don't have
    * explicitly set values
    */
   public Vector3dNodalField (
      String name, FemModel3d fem, Vector3d defaultValue) {
      super (name, Vector3d.class, fem, defaultValue);
   }

   /**
    * {@inheritDoc}
    */
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

   /**
    * Sets the values for this field from a polyline mesh which specifies a
    * direction field. For each node, the nearest mesh point is found and then
    * used to compute the vector value by averaging directions within a given
    * radius.
    *
    * @param mesh polyline mesh giving the direction field
    * @param rad direction averaging radius
    */
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
