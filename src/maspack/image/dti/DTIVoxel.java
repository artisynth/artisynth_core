package maspack.image.dti;

import maspack.matrix.AffineTransform3d;
import maspack.matrix.EigenDecomposition;
import maspack.matrix.Matrix2d;
import maspack.matrix.Matrix3d;
import maspack.matrix.MatrixNd;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.SVDecomposition;
import maspack.matrix.SymmetricMatrix3d;
import maspack.matrix.Vector3d;
import maspack.util.Clonable;

public class DTIVoxel implements Clonable {
   
   SymmetricMatrix3d D; // diffusion tensor
   // eigen-vectors, values
   Matrix3d evecs;
   Vector3d evals;
   boolean evecsComputed;
   
   public DTIVoxel(double d00, double d11, double d22, double d01, double d02, double d12) {
      D = new SymmetricMatrix3d(d00, d11, d22, d01, d02, d12);
      evecs = null;
      evals = null;
      evecsComputed = false;
   }
   
   private static void swapEigenvalues(Matrix3d vecs, Vector3d vals, int i, int j) {
      double tmp;
      for (int k=0; k<3; ++k) {
         tmp = vecs.get(k, i);
         vecs.set(k, i, vecs.get(k, j));
         vecs.set(k, j, tmp);
      }
      tmp = vals.get(i);
      vals.set(i, vals.get(j));
      vals.set(j, tmp);
   }
   //   
   //   private static Vector3d computeEvals(SymmetricMatrix3d D) {
   //      Vector3d evals = null;
   //      
   //      double d01 = D.m01;
   //      double d02 = D.m02;
   //      double d12 = D.m12;
   //      double p1 = d01*d01+d02*d02+d12*d12;
   //      if (p1 == 0)  {
   //         //  D is diagonal, sort
   //         double eig1 = D.m00;
   //         double eig2 = D.m11;
   //         double eig3 = D.m22;
   //         if (eig1 < eig2) {
   //            double tmp = eig1;
   //            eig1 = eig2;
   //            eig2 = tmp;
   //         }
   //         if (eig2 < eig3) {
   //            double tmp = eig2;
   //            eig2 = eig3;
   //            eig3 = tmp;
   //            
   //            if (eig1 < eig2) {
   //               tmp = eig1;
   //               eig1 = eig2;
   //               eig2 = tmp;
   //            }
   //         }
   //         evals = new Vector3d(eig1, eig2, eig3);
   //      } else {
   //         double q = D.trace()/3;
   //         double d00q = D.m00-q;
   //         double d11q = D.m11-q;
   //         double d22q = D.m22-q;
   //         double p2 = d00q*d00q+d11q*d11q+d22q*d22q+2*p1;
   //         double p = Math.sqrt(p2 / 6);
   //         Matrix3d B = new Matrix3d(D); 
   //         B.scaledAdd(-q, Matrix3d.IDENTITY);
   //         B.scale(1.0/p);
   //         double r = B.determinant() / 2;
   //
   //         // In exact arithmetic for a symmetric matrix  -1 <= r <= 1
   //         // but computation error can leave it slightly outside this range.
   //         double phi;
   //         if (r <= -1) { 
   //            phi = Math.PI / 3;
   //         } else if (r >= 1) {
   //            phi = 0;
   //         } else {
   //            phi = Math.acos(r) / 3;
   //         }
   //
   //         // the eigenvalues satisfy eig3 <= eig2 <= eig1
   //         double eig1 = q + 2 * p * Math.cos(phi);
   //         double eig3 = q + 2 * p * Math.cos(phi + (2*Math.PI/3));
   //         double eig2 = 3 * q - eig1 - eig3;     // since trace(A) = eig1 + eig2 + eig3
   //         evals = new Vector3d(eig1, eig2, eig3);
   //      }
   //      
   //      return evals;
   //   }
   
   /**
    * Computes an oriented principal vector, where the orientation
    * is nearest to the provided.  If the principal eigenvalue is
    * distinct, then simply flips the sign to maximize p.dot(o).
    * Otherwise if it is repeated, finds the vector in the 
    * eigenspace (plane or sphere) that is closest to o. 
    * 
    * @param o orientation to match
    * @param p principal vector
    */
   public void getOrientedPrincipalEigenvector(Vector3d o, Vector3d p) {
      if (!evecsComputed) {
         computeEigenvectors();
      }
      
      // potentially orient principal eigenvector to align o
      double emax = Math.abs(evals.x);
      double tol = 1e-5;
      if (evals.x-evals.z <= 2*tol*emax){
         // all equal, free to orient perfectly
         p.set(o);
      } else if (evals.x-evals.y <= tol*emax) {
         // first and second equal, nearest is projection onto plane spanned by v1, v2
         double d = o.x*evecs.m02+o.y*evecs.m12+o.z*evecs.m22;
         p.set(o.x-d*evecs.m02, o.y-d*evecs.m12, o.z-d*evecs.m22);
         double nrm = p.norm();
         if (nrm > 0) {
            p.scale(1.0/nrm);
         } else {
            // keep original principal vector
            p.set(evecs.m00, evecs.m10, evecs.m20);
         }
      } else {
         // flip columns to fit o
         double d = o.x*evecs.m00 + o.y*evecs.m10 + o.z*evecs.m20;
         if (d < 0) {
            p.x = -evecs.m00;
            p.y = -evecs.m10;
            p.z = -evecs.m20;
         } else {
            p.x = evecs.m00;
            p.y = evecs.m10;
            p.z = evecs.m20;
         }
      }
   }
   
   public void invalidateEigenvectors() {
      evecs =  null;
      evals = null;
      evecsComputed = false;
   }
   
   public void setZero() {
      D.setZero();
      if (evecs == null) {
         evecs = new Matrix3d();
      }
      evecs.setIdentity();
      
      if (evals == null) {
         evals = new Vector3d();
      }
      evals.setZero();
      
      evecsComputed = true;
   }
   
   /**
    * Uniform scaling of diffusion tensor, diffusivity scales with distance-squared
    * @param s scale parameter
    */
   public void scale(double s) {
      D.scale (s*s);
      if (evecsComputed) {
         evals.scale (s*s);
      }
   }
   
   /**
    * Non-uniform scaling of diffusion tensor, e.g. if original image has non-uniform voxel spacing 
    * <br>
    * 
    * Uses change of coordinates formula, and definition of diffusion via Fick's law  
    * 
    * @param sx x-scale
    * @param sy y-scale
    * @param sz z-scale
    */
   public void scale(double sx, double sy, double sz) {
      D.m00 *= sx*sx;
      D.m01 *= sx*sy;
      D.m02 *= sx*sz;
      D.m11 *= sy*sy;
      D.m12 *= sy*sz;
      D.m22 *= sz*sz;
      
      D.m10 = D.m01;
      D.m20 = D.m02;
      D.m21 = D.m12;
      
      invalidateEigenvectors ();
   }
   
   /**
    * Rotation of diffusion tensor
    * @param trans rotation
    */
   public void transform(RigidTransform3d trans) {
      D.mulLeftAndTransposeRight (trans.R);
      if (evecsComputed) {
         evecs.mul (trans.R, evecs);
      }
   }
   
   /**
    * <p>
    * Affine transform of diffusion tensor by performing a change of coordinates and
    * definition of diffusivity tensor in Fick's law
    * </p>
    * 
    * @param trans transform
    */
   public void transform(AffineTransform3d trans) {
      D.mulLeftAndTransposeRight (trans.A);
      invalidateEigenvectors ();
   }
   
   /**
    * <p>
    * Affine transform of diffusion tensor using Preservation of Principal Direction method of 
    * </p>
    * 
    * <p>
    * D. C. Alexander, C. Pierpaoli, P. J. Basser and J. C. Gee, 
    * "Spatial transformations of diffusion tensor magnetic resonance images," 
    * in IEEE Transactions on Medical Imaging, vol. 20, no. 11, pp. 1131-1139, Nov. 2001.
    * {@code http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.160.9369&rep=rep1&type=pdf}
    * </p>
    * 
    * <p>
    * Note: this definition is inconsistent with change of coordinates formulation and Fick's law
    * </p>
    * 
    * 
    * @param trans transform
    */
   public void transformPPD(AffineTransform3d trans) {
      computeEigenvectors ();
      
      // transform evecs and renormalize
      Vector3d n1 = new Vector3d();
      evecs.getColumn (0, n1);
      n1.transform (trans);
      n1.normalize ();
      
      Vector3d n2 = new Vector3d();
      evecs.getColumn (1, n2);
      n2.transform (trans);
      n2.normalize ();
      
      // R1 rotates e1 to n1
      Vector3d axis = new Vector3d();
      evecs.getColumn (0, axis);
      double cost = axis.dot (n1);
      axis.cross (n1);
      double sint = axis.norm ();
      if (sint != 0) {
         axis.scale (1.0/sint);
      }
      double theta = Math.atan2 (sint, cost);
      RotationMatrix3d R1 = new RotationMatrix3d (axis, theta);
      
      // rotate second e-vector
      Vector3d R1e2 = new Vector3d();
      evecs.getColumn (1, R1e2);
      R1e2.transform (R1);
      
      // gram-schmidt renormalization
      n2.scaledAdd (-n1.dot (n2), n1);
      n2.normalize ();
      
      cost = R1e2.dot (n2);
      axis.cross (R1e2, n2);
      sint = axis.norm ();
      if (sint != 0) {
         axis.scale (1.0/sint);
      }
      theta = Math.atan2 (sint, cost);
      RotationMatrix3d R2 = new RotationMatrix3d (axis, theta);
      
      // transform diffusion
      R2.mul (R1);
      D.mulLeftAndTransposeRight (R2);
      
      invalidateEigenvectors ();
   }
   
   /**
    * Rotates/flips u1, u2 on plane spanned by the two vectors in order to best fit v1, v2
    * @param u1
    * @param u2
    * @param v1
    * @param v2
    */
   private void fitCoplanar(Vector3d u1, Vector3d u2, Vector3d v1, Vector3d v2) {
      
      Matrix2d A = new Matrix2d();
      
      // U^T*v1
      A.m00 = u1.dot(v1);
      A.m10 = u2.dot(v1);
      // U^T*v2
      A.m01 = u1.dot(v2);
      A.m11 = u2.dot(v2);
      
      SVDecomposition svd = new SVDecomposition();
      svd.factor(A);
      
      MatrixNd X = svd.getU();
      MatrixNd Y = svd.getV();
      
      // A = UV^T, rotation+flips
      A.m00 = X.get(0, 0)*Y.get(0, 0) + X.get(0, 1)*Y.get(0, 1);
      A.m01 = X.get(0, 0)*Y.get(1, 0) + X.get(0, 1)*Y.get(1, 1);
      A.m10 = X.get(1, 0)*Y.get(0, 0) + X.get(1, 1)*Y.get(0, 1);
      A.m11 = X.get(1, 0)*Y.get(1, 0) + X.get(1, 1)*Y.get(1, 1);
      
      // best fitting U is then UA
      double u00 = u1.x*A.m00+u2.x*A.m10;
      double u10 = u1.y*A.m00+u2.y*A.m10;
      double u20 = u1.z*A.m00+u2.z*A.m10;
      double u01 = u1.x*A.m01+u2.x*A.m11;
      double u11 = u1.y*A.m01+u2.y*A.m11;
      double u21 = u1.z*A.m01+u2.z*A.m11;
      
      u1.x = u00;
      u1.y = u10;
      u1.z = u20;
      
      u2.x = u01;
      u2.y = u11;
      u2.z = u21;
   }
   
   /**
    * Orients principal eigenvector to align with the given vector,
    * flipping or rotating eigenspaces if need be
    * @param o orientation vector
    */
   public void orientPrincipalEigenvector(Vector3d o) {
      if (!evecsComputed) {
         computeEigenvectors();
      }
      
      // potentially orient principal eigenvector to align o
      double emax = Math.abs(evals.x);
      double tol = 1e-5;
      if (evals.x-evals.z <= 2*tol*emax){
         // all equal, free to orient perfectly
         RotationMatrix3d R = new RotationMatrix3d();
         R.rotateZDirection(o);
         evecs.m00 = R.m02;
         evecs.m10 = R.m12;
         evecs.m20 = R.m22;
         
         evecs.m01 = R.m00;
         evecs.m11 = R.m10;
         evecs.m21 = R.m20;
         
         evecs.m02 = R.m01;
         evecs.m12 = R.m11;
         evecs.m22 = R.m21;
      } else if (evals.x-evals.y <= tol*emax) {
         // first and second equal, nearest is projection onto plane spanned by v1, v2
         double d = o.x*evecs.m02+o.y*evecs.m12+o.z*evecs.m22;
         Vector3d p = new Vector3d();
         p.set(o.x-d*evecs.m02, o.y-d*evecs.m12, o.z-d*evecs.m22);
         double nrm = p.norm();
         if (nrm > 0) {
            p.scale(1.0/nrm);
            // rotate plane
            Vector3d n = new Vector3d(evecs.m02, evecs.m12, evecs.m22);
            n.cross(p);     // get one perpendicular to p and n
            n.normalize();  // should be normalized, but make sure
            evecs.m00 = p.x;
            evecs.m10 = p.y;
            evecs.m20 = p.z;
            evecs.m01 = n.x;
            evecs.m11 = n.y;
            evecs.m21 = n.z;
         } else {
            // keep original principal vector
         }
      } else {
         // flip column to fit o
         double d = o.x*evecs.m00 + o.y*evecs.m10 + o.z*evecs.m20;
         if (d < 0) {
            evecs.m00 = -evecs.m00;
            evecs.m10 = -evecs.m10;
            evecs.m20 = -evecs.m20;
         }
      }
   }
   
   /**
    * Orients eigenvectors to align with the given ordered set, flipping
    * signs if need be, or rotating for repeated eigenvalues.  Attempts
    * to maximize correlation between provided ordered eigenvectors.
    * @param O ordered eigenvectors
    * @param lambda ordered eigenvalues
    */
   public void orientEigenvectors(Matrix3d O, Vector3d lambda) {
      if (!evecsComputed) {
         computeEigenvectors();
      }
      
      // potentially orient eigenvectors to align with another set
      double emax = Math.abs(evals.x);
      double tol = 1e-5;
      if (evals.x-evals.z <= 2*tol*emax) {
         // all equal, free to orient perfectly
         evecs.set(O);
      } else if (evals.x-evals.y <= tol*emax) {
         // first and second equal, attempt to rotate and flip to best fit O1, O2
         Vector3d u1 = new Vector3d();
         Vector3d u2 = new Vector3d();
         evecs.getColumn(0, u1);
         evecs.getColumn(1, u2);
         Vector3d v1 = new Vector3d();
         Vector3d v2 = new Vector3d();
         O.getColumn(0, v1);
         O.getColumn(1, v2);
         v1.scale(lambda.x);
         v2.scale(lambda.y);
         
         fitCoplanar(u1, u2, v1, v2);
         evecs.setColumn(0, u1);
         evecs.setColumn(1, u2);

         // maybe flip third column
         double d = O.m02*evecs.m02 + O.m12*evecs.m12 + O.m22*evecs.m22;
         if (d < 0) {
            evecs.m02 = -evecs.m02;
            evecs.m12 = -evecs.m12;
            evecs.m22 = -evecs.m22;
         }
      } else if (evals.y-evals.z <= tol*emax) {

         // flip column 1 to fit O
         double d = O.m00*evecs.m00 + O.m10*evecs.m10 + O.m20*evecs.m20;
         if (d < 0) {
            evecs.m00 = -evecs.m00;
            evecs.m10 = -evecs.m10;
            evecs.m20 = -evecs.m20;
         }
               
         // second and third equal, attempt to rotate and flip to best fit O2, O3
         Vector3d u1 = new Vector3d();
         Vector3d u2 = new Vector3d();
         evecs.getColumn(1, u1);
         evecs.getColumn(2, u2);
         Vector3d v1 = new Vector3d();
         Vector3d v2 = new Vector3d();
         O.getColumn(1, v1);
         O.getColumn(2, v2);
         v1.scale(lambda.y);
         v2.scale(lambda.z);
         
         fitCoplanar(u1, u2, v1, v2);
         evecs.setColumn(1, u1);
         evecs.setColumn(2, u2);
      } else {
         // flip columns to fit O
         double d = O.m00*evecs.m00 + O.m10*evecs.m10 + O.m20*evecs.m20;
         if (d < 0) {
            evecs.m00 = -evecs.m00;
            evecs.m10 = -evecs.m10;
            evecs.m20 = -evecs.m20;
         }

         d = O.m01*evecs.m01 + O.m11*evecs.m11 + O.m21*evecs.m21;
         if (d < 0) {
            evecs.m01 = -evecs.m01;
            evecs.m11 = -evecs.m11;
            evecs.m21 = -evecs.m21;
         }

         d = O.m02*evecs.m02 + O.m12*evecs.m12 + O.m22*evecs.m22;
         if (d < 0) {
            evecs.m02 = -evecs.m02;
            evecs.m12 = -evecs.m12;
            evecs.m22 = -evecs.m22;
         }
      }
      
   }
   
   private void sortEigenvalues(Vector3d evals, Matrix3d evecs) {
      // bubble-sort
      if (Math.abs (evals.get (0)) < Math.abs (evals.get (1))) {
         swapEigenvalues (evecs, evals, 0, 1);
      }
      if (Math.abs (evals.get (1)) < Math.abs (evals.get (2))) {
         swapEigenvalues (evecs, evals, 1, 2);
      }
      if (Math.abs (evals.get (0)) < Math.abs (evals.get (1))) {
         swapEigenvalues (evecs, evals, 0, 1);
      }
   }
   
   private void computeEigenvectors() {
      if (!evecsComputed) {
         evals = new Vector3d();
         evecs = new Matrix3d();
         EigenDecomposition.factorSymmetric (evals, evecs, D);
         sortEigenvalues(evals, evecs);
         evecsComputed = true;
      }
   }
   
   public void getV1(Vector3d v1) {
      if (!evecsComputed) {
         computeEigenvectors();
      }
      evecs.getColumn(0, v1);
   }
   
   public double getE1() {
      if (!evecsComputed) {
         computeEigenvectors();
      }
      return evals.x;
   }
   
   public void getV2(Vector3d v2) {
      if (!evecsComputed) {
         computeEigenvectors();
      }
      evecs.getColumn(1, v2);
   }
   
   public double getE2() {
      if (!evecsComputed) {
         computeEigenvectors();
      }
      return evals.y;
   }
   
   public void getV3(Vector3d v3) {
      if (!evecsComputed) {
         computeEigenvectors();
      }
      evecs.getColumn(2, v3);
   }
   
   public double getE3() {
      if (!evecsComputed) {
         computeEigenvectors();
      }
      return evals.z;
   }
   
   /**
    * Fractional anisotropy
    * @return fractional anisotropy
    */
   public double getFA() {
      if (!evecsComputed) {
         computeEigenvectors();
      }
      double e12 = evals.x-evals.y;
      double e23 = evals.y-evals.z;
      double e13 = evals.x-evals.z;
      if (evals.norm() == 0) {
         return 0;
      }
      return Math.sqrt( (e12*e12+e23*e23+e13*e13)/(2*(evals.normSquared())));
   }
   
   /**
    * Mean diffusivity
    * @return mean diffusivity
    */
   public double getMD() {
      if (!evecsComputed) {
         computeEigenvectors();
      }
      return (evals.x+evals.y+evals.z)/3;
   }
   
   /**
    * Radial diffusivity
    * @return radial diffusivity
    */
   public double getRD() {
      if (!evecsComputed) {
         computeEigenvectors();
      }
      return (evals.y+evals.z)/2;
   }
   
   /**
    * Axial diffusivity
    * @return axial diffusivity
    */
   public double getAD() {
      if (!evecsComputed) {
         computeEigenvectors();
      }
      return evals.x;
   }

   public Matrix3d getV() {
      if (!evecsComputed) {
         computeEigenvectors();
      }
      return evecs;
   }

   public SymmetricMatrix3d getD() {
      return D;
   }
   
   @Override
   public DTIVoxel clone()  {
      DTIVoxel copy;
      try {
         copy = (DTIVoxel)super.clone();
      } catch (CloneNotSupportedException e) {
         throw new RuntimeException(e);
      }
      
      copy.D = (SymmetricMatrix3d)D.clone();
      copy.evecs = null;
      copy.evals = null;
      copy.evecsComputed = false;
      
      return copy;
   }
}
