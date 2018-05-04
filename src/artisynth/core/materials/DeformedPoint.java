package artisynth.core.materials;

import maspack.matrix.*;
import artisynth.core.modelbase.FieldPoint;

/**
 * Extends FieldPoint to include information about the deformation gradient.
 */
public interface DeformedPoint extends FieldPoint {
   
   public Matrix3d getF();
   
   public double getDetF();
   
   public double getAveragePressure();
   
   public RotationMatrix3d getR();
}
