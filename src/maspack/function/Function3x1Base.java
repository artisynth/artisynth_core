package maspack.function;

import maspack.matrix.Point3d;

public abstract class Function3x1Base implements Function3x1 {

   @Override
   public double eval(double[] in) {
      return eval(in[0], in[1], in[2]);
   }

   @Override
   public int getInputSize() {
      return 3;
   }

   @Override
   public abstract double eval(double x, double y, double z);

   @Override
   public double eval(Point3d in) {
      return eval(in.x, in.y, in.z);
   }

}
