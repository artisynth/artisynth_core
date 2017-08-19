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

   /*
    * The vectors n representing bounds in addHalfSpaceBound()
    * are given in global coordinates. If the spherical joint
    * is attached to a rotating object, the vectors n should
    * rotate as well. If this frame is set to the object's frame,
    * that behaviour can be achieved.
    * 
    * If no frame is specified in a constructor, a default frame
    * will be used. That frame will not be attached to any rotating body,
    * so the joint will behave as though it is fixed to the global
    * coordinate system.
    */
   protected Frame frame;
   
   /*
    * Representation of the bound vectors in frame coordinates.
    * These do not change as the frame rotates: the frame coordinates
    * remain the same throughout the movement.
    */
   protected MatrixNd NFrame = new MatrixNd();
   
   /*
    * Representation of the bound vectors in global coordinates.
    * These values change as the frame rotates and need to be
    * recalculated at each time step.
    */
   MatrixNd N = new MatrixNd ();

   public SphericalJointForceBound () {
   }

   public SphericalJointForceBound (double weight, TrackingController con) {
      super (weight);
      myController = con;
      P.setZero ();
      frame = new Frame(); // no frame specified; use default
   }
   
   public SphericalJointForceBound (
      double weight, TrackingController con, Frame f) {
      super (weight);
      myController = con;
      P.setZero ();
      if (f == null) {
         frame = new Frame(); // no frame specified, use default
      } else {
         frame = f;
      }
   }

   /**
    * Computes the bound vectors into the joint's frame of reference.
    */
   protected void globalToFrame() {
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

   /**
    * Transforms the bound vectors back into global coordinates.
    */
   protected void frameToGlobal() {
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

   @Override
   public int getRowSize () {
      return bounds.size ();
   }

   @Override
   protected void compute (double t0, double t1) {
      frameToGlobal(); // solve the system in global coordinates
      
      //      System.out.println("N="+N.toString ("%3.1f"));
      //      System.out.println("Hc="+myController.getData ().getHc ().toString ("%3.1f"));
      
      H.mul (N, myController.getData ().getHc ()); // assumes Hc targets one spherical joint
      f.mul (N, myController.getData ().getC0 ());
      f.negate ();
      //      System.out.println("H="+H.toString ("%g"));
      //      System.out.println("f="+f.toString ("%g"));      
   }

   /**
    * Add a half-space bound to the spherical joint
    * This vector is specified relative to a global coordinate frame
    * @param n vector specifying the half-space bound
    */
   public void addHalfspaceBound(Vector3d n) {
      bounds.add (n);
      
      N = new MatrixNd (bounds.size (), 3);
      for (int i = 0; i < bounds.size (); ++i) {
         N.setRow (i, bounds.get (i));
      }
      
      H.setSize (bounds.size (), 3);
      globalToFrame(); // represent the vector in frame coordinates
   }

   /**
    * Returns a list of all the vectors forming the spherical
    * joint force bound, in global coordinates. (read-only)
    * @return list of the bound normals
    */
   public ArrayList<Vector3d> getBoundNormals() {
      ArrayList<Vector3d> ret = new ArrayList<Vector3d>();
      for (int i = 0; i < N.rowSize (); i++) {
         double[] d = new double[3];
         N.getRow (i, d);
         ret.add (new Vector3d(d));
      }
      return ret;
   }

   public MatrixNd getMatrixOfBoundNormals() {
      return N;
   }
   
   /**
    * Returns the frame used by the spherical joint force bound
    * This frame should not be directly modified as it is used to
    * track the movement of the body that the joint is attached to.
    * @return the spherical joint's frame
    */
   public Frame getFrame() {
      return frame;
   }


}
