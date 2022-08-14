package artisynth.core.modelbase;

import maspack.matrix.Point3d;
import maspack.matrix.VectorObject;

/**
 * Base definition for fields that define vector values at type {@code T},
 * where {@code T} is an instance of {@link VectorObject}.
 */
public interface VectorField<T extends VectorObject<T>> {

   /**
    * Returns the value of the this field at a specified spatial position.
    * 
    * @param pos position at which value is requested
    * @return value at the position
    */
   public T getValue (Point3d pos);

   /**
    * Returns the value of the this field at a specified FEM field point.
    * 
    * @param fp point at which value is requested
    * @return value at the point
    */ 
   public T getValue (FieldPoint fp);

   /**
    * Returns the value of the this field at a specified mesh field point.
    * 
    * @param fp point at which value is requested
    * @return value at the point
    */
   public T getValue (MeshFieldPoint fp);

}
