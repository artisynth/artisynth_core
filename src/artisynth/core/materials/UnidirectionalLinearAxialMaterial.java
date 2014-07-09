package artisynth.core.materials;

/**
 * If distance is less than rest length, applies no force
 * @author "Antonio Sanchez"
 * Creation date: 25 Jan 2013
 *
 */
public class UnidirectionalLinearAxialMaterial extends LinearAxialMaterial {

   public UnidirectionalLinearAxialMaterial (){
      super();
   }

   public UnidirectionalLinearAxialMaterial (double k, double d) {
      super(k,d);
   }
   
   public double computeF (
      double l, double ldot, double l0, double excitation) {
      
      double d = Math.max(l-l0,0);
      return myStiffness*d + myDamping*ldot;
   }

   public double computeDFdl (
      double l, double ldot, double l0, double excitation) {
      if (l<l0) {
         return 0;
      }
      return myStiffness; 
   }

   public boolean isDFdldotZero() {
      return myDamping == 0;
   }

   public boolean equals (AxialMaterial mat) {
      if (!(mat instanceof UnidirectionalLinearAxialMaterial)) {
         return false;
      }
      return super.equals(mat);
   }

   public LinearAxialMaterial clone() {
      UnidirectionalLinearAxialMaterial mat = (UnidirectionalLinearAxialMaterial)super.clone();
      return mat;
   }
   
}
