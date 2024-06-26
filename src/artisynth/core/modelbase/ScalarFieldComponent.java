package artisynth.core.modelbase;

import maspack.matrix.Point3d;

/**
 * Base definition for scalar field components.
 */
public interface ScalarFieldComponent extends FieldComponent {

   /**
    * Returns the value of the this field at a specified spatial position.
    * 
    * @param pos position at which value is requested
    * @return value at the position
    */
   public double getValue (Point3d pos);
   
   /**
    * Returns the value of the this field at a specified FEM field point.
    * 
    * @param fp point at which value is requested
    * @return value at the point
    */ 
   public double getValue (FemFieldPoint fp);

   /**
    * Returns the value of the this field at a specified mesh field point.
    * 
    * @param fp point at which value is requested
    * @return value at the point
    */  
   public double getValue (MeshFieldPoint fp);

}
