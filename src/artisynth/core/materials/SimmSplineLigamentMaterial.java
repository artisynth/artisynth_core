package artisynth.core.materials;

import artisynth.core.materials.AxialMaterial;

public class SimmSplineLigamentMaterial extends AxialMaterial {
   /* Interpolation parameters. */
   protected int n;
   protected double magnitude;
   protected double _x[];
   protected double _y[];
   protected double _b[];
   protected double _c[];
   protected double _d[];
   
   /**
    * Create a new ligament material. x and y must have the same number of
    * elements (no less than 4). 
    * @param x          array of lengths
    * @param y          array of forces
    * @param mag        multiplier for the forces
    */
   public SimmSplineLigamentMaterial(double x[], double y[], double mag) {
      n = x.length;
      _x = x;
      _y = y;
      computeCoefficients();
      magnitude = mag;
   }
   
   /**
    * Evaluate the spline at the given point.
    * Implementation taken from OpenSim's SimmSpline.
    */
   private double evaluate(double x) {
      if(x <= _x[0]) {
         return _y[0] + (x - _x[0]) * _b[0];
      } else if(x >= _x[n - 1]) {
         return _y[n - 1] + (x - _x[0]) * _b[n - 1];
      }
      
      int i = 0;
      int j = n;
      int k;
      while(true) {
         k  = (i + j) / 2;
         if(x < _x[k]) {
            j = k;
         } else if(x > _x[k + 1]) {
            i = k;
         } else {
            break;
         }
      }
      
      double dx = x - _x[k];
      return _y[k] + dx * (_b[k] + dx * (_c[k] + dx * _d[k]));
   }
   
   /**
    * Evaluate the derivative of the spline at the given point.
    */
   private double evaluateDer(double x) {
      if(x <= _x[0]) {
         return _b[0];
      } else if(x >= _x[n - 1]) {
         return _b[n - 1];
      }
      
      int i = 0;
      int j = n;
      int k;
      while(true) {
         k  = (i + j) / 2;
         if(x < _x[k]) {
            j = k;
         } else if(x > _x[k + 1]) {
            i = k;
         } else {
            break;
         }
      }
      
      double dx = x - _x[k];
      return _b[k] + dx * (2 * _c[k] + 3 * dx * _d[k]);
   }
   
   /**
    * Calculate the coefficients for each cubic.
    * Implementation taken from OpenSim's SimmSpline.
    */
   private void computeCoefficients() {
      _b = new double[n];
      _c = new double[n];
      _d = new double[n];
      
      int nm1 = n - 1;
      int nm2 = n - 2;
      
      _d[0] = _x[1] - _x[0];
      _c[1] = (_y[1]-_y[0])/_d[0];
      for(int i = 1; i < nm1; i++)
      {
         _d[i] = _x[i+1] - _x[i];
         _b[i] = 2 * (_d[i-1] + _d[i]);
         _c[i+1] = (_y[i+1] - _y[i]) / _d[i];
         _c[i] = _c[i+1] - _c[i];
      }
      _b[0] = -_d[0];
      _b[nm1] = -_d[nm2];
      _c[0] = 0;
      _c[nm1] = 0;
      
      double d31 = _x[3] - _x[1];
      double d20 = _x[2] - _x[0];
      double d1 = _x[nm1] - _x[n-3];
      double d2 = _x[nm2] - _x[n-4];
      double d30 = _x[3] - _x[0];
      double d3 = _x[nm1] - _x[n-4];
      _c[0] = _c[2] / d31 - _c[1] / d20;
      _c[nm1] = _c[nm2] / d1 - _c[n-3] / d2;
      _c[0] = _c[0] * _d[0] * _d[0] / d30;
      _c[nm1] = -_c[nm1] * _d[nm2] * _d[nm2] / d3;
      
      for(int i = 1; i < n; i++)
      {
         double t = _d[i-1] / _b[i-1];
         _b[i] -= t * _d[i-1];
         _c[i] -= t * _c[i-1];
      }
      _c[nm1] /= _b[nm1];
      for(int j = 0; j < nm1; j++)
      {
         int i = nm2 - j;
         _c[i] = (_c[i] - _d[i] * _c[i+1]) / _b[i];
      }
      
      _b[nm1] =
         (_y[nm1] - _y[nm2]) / _d[nm2] + _d[nm2] * (_c[nm2] + 2 * _c[nm1]);
      for(int i = 0; i < nm1; i++)
      {
         _b[i] = (_y[i+1] - _y[i]) / _d[i] - _d[i] * (_c[i+1] + 2 * _c[i]);
         _d[i] = (_c[i+1] - _c[i]) / _d[i];
         _c[i] *= 3;
      }
      _c[nm1] *= 3;
      _d[nm1] = _d[nm2];
   }
   
   public double computeF(
      double l, double ldot, double l0, double excitation) {
      if(l <= l0) {
         return 0;
      }
      
      return magnitude * evaluate(l / l0);
   }

   public double computeDFdl(
      double l, double ldot, double l0, double excitation) {
      if(l <= l0) {
         return 0;
      }
      
      return magnitude * evaluateDer(l / l0) / l0;
   }

   public double computeDFdldot(
      double l, double ldot, double l0, double excitation) {
      return 0;
   }

   public boolean isDFdldotZero() {
      return true;
   }
}
