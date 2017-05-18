package artisynth.core.inverse;

import java.util.ArrayList;

import artisynth.core.mechmodels.Frame;
import artisynth.core.workspace.RootModel;
import maspack.matrix.MatrixNd;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.properties.PropertyList;

public class SphericalJointForceBound extends LeastSquaresTermBase {

   protected TrackingController myController;

   ArrayList<Vector3d> bounds = new ArrayList<Vector3d> ();
   MatrixNd N = new MatrixNd ();

   /*
    * The vectors n representing bounds in addHalfSpaceBound()
    * are given in global coordinates. If the spherical joint
    * is attached to a rotating object, the vectors n should
    * rotate as well. If this frame is set to the object's frame,
    * that behaviour can be achieved.
    * 
    * By default, it is initialized to whatever the default orientation
    * is. If no frame is specified in a constructor, switching back and 
    * forth between global and frame representations will have no effect 
    * because this dummy frame will not have been rotated.
    */
   protected Frame frame;
   protected MatrixNd NFrame = null;

   public SphericalJointForceBound () {
   }

   public SphericalJointForceBound (double weight, TrackingController con) {
      super (weight);
      myController = con;
      P.setZero ();
   }

   public SphericalJointForceBound (
      double weight, TrackingController con, Frame f) {
      this(weight, con);
      if (f == null) {
         frame = new Frame();
      } else {
         frame = f;
      }
   }

   /**
    * Computes the rows of Matrix N from the bounds, relative to the frame.
    * The bounds are given globally, and this function represents them in
    * terms of the Spherical Joint's frame.
    */
   public void globalToFrame() {
      if (NFrame == null) {
         NFrame = new MatrixNd(bounds.size (), 3);
         RigidTransform3d R = frame.getPose ();
         for (int i = 0; i < bounds.size (); i++) {
            //Make a copy of vector at bounds(i)
            Vector3d vFrame = new Vector3d(bounds.get (i));
            //Transform it into the frame's coordinates
            vFrame.inverseTransform (R);
            //Add it to NFrame
            NFrame.setRow (i, vFrame);
         }
      }
   }

   /**
    * Changes the Matrix N's representation from the Spherical Joint's frame
    * to a global representation.
    */
   public void frameToGlobal() {
      if (NFrame != null) {
         N = new MatrixNd(NFrame.rowSize (), 3);
         RigidTransform3d R = frame.getPose ();
         for (int i = 0; i < NFrame.rowSize (); i++) {
            //Get the values from the i'th row of NFrame
            double[] d = new double[3];
            NFrame.getRow (i, d);
            //Create a vector from the row and transform it back to global
            Vector3d vGlobal = new Vector3d(d);
            vGlobal.transform (R);
            //Add it to N (which is used to solve)
            N.setRow (i, vGlobal);
         }
      }
   }

   @Override
   public int getRowSize () {
      return bounds.size ();
   }

   @Override
   protected void compute (double t0, double t1) {
      frameToGlobal();
      
      /*
      // DEBUG
      if (t0 % 0.1 < 0.01) {
         Vector3d v1 = bounds.get(1);
         double[] d = new double[3];
         N.getRow (1, d);
         Vector3d v2 = new Vector3d(d);
         System.out.println("bounds: " + v1 + " N: " + v2 + " " + (v2.dot (v2))/(v1.dot (v1)));
      }
      */
      
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
      ArrayList<Vector3d> ret = new ArrayList<Vector3d>();
      for (int i = 0; i < N.rowSize (); i++) {
         double[] d = new double[3];
         N.getRow (i, d);
         ret.add (new Vector3d(d));
      }
      return ret;
      //return bounds;
   }

   public MatrixNd getMatrixOfBoundNormals() {
      return N;
   }


}
