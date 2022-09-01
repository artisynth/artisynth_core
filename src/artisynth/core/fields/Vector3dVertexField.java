package artisynth.core.fields;

import artisynth.core.mechmodels.MeshComponent;
import maspack.matrix.Vector3d;
import maspack.geometry.PolygonalMesh;

/**
 * A vector field, for vectors of type {@link Vector3d}, defined over a
 * triangular polygonal mesh, using values set at the mesh's vertices. Values
 * at other points are obtained by barycentric interpolation on the faces
 * nearest to those points. Values at vertices for which no explicit value has
 * been set are given by the field's <i>default value</i>.
 */
public class Vector3dVertexField extends VectorVertexField<Vector3d> {

   /**
    * This constructor should not be called by applications, unless {@link
    * #scan} is called immediately after.
    */
   public Vector3dVertexField () {
      super (Vector3d.class);
   }

   /**
    * Constructs a field for a given mesh, with a default value of 0.
    * At present, the mesh must be a triangular {@link PolygonalMesh}.
    *
    * @param mcomp component containing the mesh associated with the field
    */   
   public Vector3dVertexField (MeshComponent mcomp) {
      super (Vector3d.class, mcomp);
   }

   /**
    * Constructs a field for a given mesh and default value.
    * At present, the mesh must be a triangular {@link PolygonalMesh}.
    * 
    * @param mcomp component containing the mesh associated with the field
    * @param defaultValue default value for vertices which don't have
    * explicitly set values
    */
   public Vector3dVertexField (MeshComponent mcomp, Vector3d defaultValue) {
      super (Vector3d.class, mcomp, defaultValue);
   }

   /**
    * Constructs a named field for a given mesh, with a default value of 0.
    * At present, the mesh must be a triangular {@link PolygonalMesh}.
    * 
    * @param name name of the field
    * @param mcomp component containing the mesh associated with the field
    */
   public Vector3dVertexField (String name, MeshComponent mcomp) {
      super (name, Vector3d.class, mcomp);
   }

   /**
    * Constructs a named field for a given mesh and default value.
    * At present, the mesh must be a triangular {@link PolygonalMesh}.
    *
    * @param name name of the field
    * @param mcomp component containing the mesh associated with the field
    * @param defaultValue default value for vertices which don't have
    * explicitly set values
    */
   public Vector3dVertexField (
      String name, MeshComponent mcomp, Vector3d defaultValue) {
      super (name, Vector3d.class, mcomp, defaultValue);
   }

   /**
    * {@inheritDoc}
    */
   public boolean hasParameterizedType() {
      return false;
   }
}
