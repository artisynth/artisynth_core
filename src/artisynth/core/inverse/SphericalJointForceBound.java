package artisynth.core.inverse;

import java.util.ArrayList;

import artisynth.core.workspace.RootModel;
import maspack.matrix.MatrixNd;
import maspack.matrix.Vector3d;
import maspack.properties.PropertyList;

public class SphericalJointForceBound extends LeastSquaresTermBase {
   
   protected TrackingController myController;

   ArrayList<Vector3d> bounds = new ArrayList<Vector3d> ();
   MatrixNd N = new MatrixNd ();
   
   public SphericalJointForceBound () {
   }

   public SphericalJointForceBound (double weight, TrackingController con) {
      super (weight);
      myController = con;
      P.setZero ();
   }

   @Override
   public int getRowSize () {
      return bounds.size ();
   }

   @Override
   protected void compute (double t0, double t1) {
//      System.out.println("N="+N.toString ("%3.1f"));
//      System.out.println("Hc="+myController.getData ().getHc ().toString ("%3.1f"));
      H.mul (N, myController.getData ().getHc ()); // assumes Hc targets one spherical joint
      f.mul (N, myController.getData ().getC0 ());
      f.negate ();
//      System.out.println("H="+H.toString ("%g"));
//      System.out.println("f="+f.toString ("%g"));      
   }
   
   public void addHalfspaceBound(Vector3d n) {
      bounds.add (n);
      N = new MatrixNd (bounds.size (), 3);
      for (int i = 0; i < bounds.size (); ++i) {
         N.setRow (i, bounds.get (i));
      }
      H.setSize (bounds.size (), 3);
   }

   public ArrayList<Vector3d> getBoundNormals() {
      return bounds;
   }
   
   public MatrixNd getMatrixOfBoundNormals() {
      return N;
   }
   

}
