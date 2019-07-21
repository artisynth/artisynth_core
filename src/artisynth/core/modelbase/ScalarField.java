package artisynth.core.modelbase;

import maspack.matrix.Point3d;

public interface ScalarField extends Field {

   public double getValue (Point3d pos);

   public ScalarFieldPointFunction createFieldFunction (boolean useRestPos);

}
