package artisynth.core.materials;

import maspack.matrix.*;

// Utility methods to assist in computing tensor quantities
public class TensorUtils {
   
   /** 
    * Adds the scaled tensor product
    * <pre>
    *  S (X) I + I (X) S
    * </pre>
    * to a symmetric 4th order constitutive tensor represented as a
    * 6 x 6 matrix. Here, S is a symmetric 3 x 3 matrix, I is the
    * 3 x 3 identity matrix, and <code>X</code>
    * denotes a tensor product.
    * 
    * @param D 6 x 6 matrix representation of the tensor to which
    * the product is added
    * @param S matrix for forming the product. This is assumed to be symmetric
    * and only the upper triangular components are used.
    */
   public static void addSymmetricIdentityProduct (Matrix6d D, Matrix3dBase S) {

      double s00 = S.m00;
      double s11 = S.m11;
      double s22 = S.m22;
      double s01 = S.m01;
      double s02 = S.m02;
      double s12 = S.m12;

      D.m00 += 2*s00;
      D.m01 += (s00+s11); 
      D.m02 += (s00+s22); 
      D.m03 += s01;
      D.m04 += s12;
      D.m05 += s02;

      D.m10 += (s11+s00);
      D.m11 += 2*s11;
      D.m12 += (s11+s22); 
      D.m13 += s01;
      D.m14 += s12;
      D.m15 += s02;

      D.m20 += (s22+s00);
      D.m21 += (s22+s11);
      D.m22 += 2*s22;
      D.m23 += s01;
      D.m24 += s12;
      D.m25 += s02;

      D.m30 += s01;
      D.m31 += s01;
      D.m32 += s01;

      D.m40 += s12;
      D.m41 += s12;
      D.m42 += s12;

      D.m50 += s02;
      D.m51 += s02;
      D.m52 += s02;
   }

   /** 
    * Adds the symmetric tensor product
    * <pre>
    *  A (X) B + B (X) A
    * </pre>
    * to a symmetric 4th order constitutive tensor represented as a
    * 6 x 6 matrix. Here, A and B are 3 x 3 matrices (assumed to be symmetric),
    * and <code>(X)</code> denotes a tensor product.
    *
    * <p>Note that this method sets only the upper triangular components of D.
    * 
    * @param D 6 x 6 matrix representation of the tensor to which
    * the product is added
    * @param A first matrix for forming the product, assumed to be
    * symmetric with only the upper triangular components used.
    * @param B second matrix for forming the product, assumed to be
    * symmetric with only the upper triangular components used.
    */
   public static void addSymmetricTensorProduct (
      Matrix6d D, Matrix3dBase A, Matrix3dBase B) {

      double a00 = A.m00;
      double a11 = A.m11;
      double a22 = A.m22;
      double a01 = A.m01;
      double a02 = A.m02;
      double a12 = A.m12;

      double b00 = B.m00;
      double b11 = B.m11;
      double b22 = B.m22;
      double b01 = B.m01;
      double b02 = B.m02;
      double b12 = B.m12;

      D.m00 += 2*a00*b00;
      D.m01 += a00*b11 + b00*a11;
      D.m02 += a00*b22 + b00*a22;
      D.m03 += a00*b01 + b00*a01;
      D.m04 += a00*b12 + b00*a12;
      D.m05 += a00*b02 + b00*a02;
	
      D.m11 += 2*a11*b11;
      D.m12 += a11*b22 + b11*a22;
      D.m13 += a11*b01 + b11*a01;
      D.m14 += a11*b12 + b11*a12;
      D.m15 += a11*b02 + b11*a02;
	
      D.m22 += 2*a22*b22;
      D.m23 += a22*b01 + b22*a01;
      D.m24 += a22*b12 + b22*a12;
      D.m25 += a22*b02 + b22*a02;
	
      D.m33 += 2*a01*b01;
      D.m34 += a01*b12 + b01*a12;
      D.m35 += a01*b02 + b01*a02;
	
      D.m44 += 2*a12*b12;
      D.m45 += a12*b02 + b12*a02;
	
      D.m55 += 2*a02*b02;      
   }

   /** 
    * Adds a scaled symmetric tensor product
    * <pre>
    *  s (A (X) B + B (X) A)
    * </pre>
    * to a symmetric 4th order constitutive tensor represented as a
    * 6 x 6 matrix. Here, A and B are 3 x 3 matrices (assumed to be symmetric),
    * and <code>(X)</code> denotes a tensor product.
    * 
    * <p>Note that this method sets only the upper triangular components of D.
    *
    * @param D 6 x 6 matrix representation of the tensor to which
    * the product is added
    * @param s scale factor
    * @param A first matrix for forming the product, assumed to be
    * symmetric with only the upper triangular components used.
    * @param B second matrix for forming the product, assumed to be
    * symmetric with only the upper triangular components used.
    */
   public static void addSymmetricTensorProduct (
      Matrix6d D, double s, Matrix3dBase A, Matrix3dBase B) {

      double a00 = s*A.m00;
      double a11 = s*A.m11;
      double a22 = s*A.m22;
      double a01 = s*A.m01;
      double a02 = s*A.m02;
      double a12 = s*A.m12;

      double b00 = B.m00;
      double b11 = B.m11;
      double b22 = B.m22;
      double b01 = B.m01;
      double b02 = B.m02;
      double b12 = B.m12;

      D.m00 += 2*a00*b00;
      D.m01 += a00*b11 + b00*a11;
      D.m02 += a00*b22 + b00*a22;
      D.m03 += a00*b01 + b00*a01;
      D.m04 += a00*b12 + b00*a12;
      D.m05 += a00*b02 + b00*a02;
	
      D.m11 += 2*a11*b11;
      D.m12 += a11*b22 + b11*a22;
      D.m13 += a11*b01 + b11*a01;
      D.m14 += a11*b12 + b11*a12;
      D.m15 += a11*b02 + b11*a02;
	
      D.m22 += 2*a22*b22;
      D.m23 += a22*b01 + b22*a01;
      D.m24 += a22*b12 + b22*a12;
      D.m25 += a22*b02 + b22*a02;
	
      D.m33 += 2*a01*b01;
      D.m34 += a01*b12 + b01*a12;
      D.m35 += a01*b02 + b01*a02;
	
      D.m44 += 2*a12*b12;
      D.m45 += a12*b02 + b12*a02;
	
      D.m55 += 2*a02*b02;      

     // symmetry: just set
   }

   /** 
    * Adds the tensor product
    * <pre>
    *  A (X) A
    * </pre>
    * to a symmetric 4th order constitutive tensor represented as a
    * 6 x 6 matrix. Here, A is a 3 x 3 matrix (assumed to be symmetric),
    * and <code>(X)</code> denotes a tensor product.
    *
    * <p>Note that this method sets only the upper triangular components of D.
    * 
    * @param D 6 x 6 matrix representation of the tensor to which
    * the product is added
    * @param A matrix for forming the product, assumed to be
    * symmetric with only the upper triangular components used.
    */
   public static void addTensorProduct (Matrix6d D, Matrix3dBase A) {

      double a00 = A.m00;
      double a11 = A.m11;
      double a22 = A.m22;
      double a01 = A.m01;
      double a02 = A.m02;
      double a12 = A.m12;

      D.m00 += a00*a00;
      D.m01 += a00*a11;
      D.m02 += a00*a22;
      D.m03 += a00*a01;
      D.m04 += a00*a12;
      D.m05 += a00*a02;

      D.m11 += a11*a11;
      D.m12 += a11*a22;
      D.m13 += a11*a01;
      D.m14 += a11*a12;
      D.m15 += a11*a02;

      D.m22 += a22*a22;
      D.m23 += a22*a01;
      D.m24 += a22*a12;
      D.m25 += a22*a02;

      D.m33 += a01*a01;
      D.m34 += a01*a12;
      D.m35 += a01*a02;

      D.m44 += a12*a12;
      D.m45 += a12*a02;

      D.m55 += a02*a02;
   }

   /** 
    * Adds the scaled tensor product
    * <pre>
    *  s (A (X) A)
    * </pre>
    * to a symmetric 4th order constitutive tensor represented as a
    * 6 x 6 matrix. Here, A is a 3 x 3 matrix (assumed to be symmetric),
    * and <code>(X)</code> denotes a tensor product.
    *
    * <p>Note that this method sets only the upper triangular components of D.
    * 
    * @param D 6 x 6 matrix representation of the tensor to which
    * the product is added
    * @param s scale factor
    * @param A matrix for forming the product, assumed to be
    * symmetric with only the upper triangular components used.
    */
   public static void addTensorProduct (Matrix6d D, double s, Matrix3dBase A) {

      double a00 = A.m00;
      double a11 = A.m11;
      double a22 = A.m22;
      double a01 = A.m01;
      double a02 = A.m02;
      double a12 = A.m12;

      double s00 = s*a00;
      double s11 = s*a11;
      double s22 = s*a22;
      double s01 = s*a01;
      double s02 = s*a02;
      double s12 = s*a12;

      D.m00 += s00*a00;
      D.m01 += s00*a11;
      D.m02 += s00*a22;
      D.m03 += s00*a01;
      D.m04 += s00*a12;
      D.m05 += s00*a02;

      D.m11 += s11*a11;
      D.m12 += s11*a22;
      D.m13 += s11*a01;
      D.m14 += s11*a12;
      D.m15 += s11*a02;

      D.m22 += s22*a22;
      D.m23 += s22*a01;
      D.m24 += s22*a12;
      D.m25 += s22*a02;

      D.m33 += s01*a01;
      D.m34 += s01*a12;
      D.m35 += s01*a02;

      D.m44 += s12*a12;
      D.m45 += s12*a02;

      D.m55 += s02*a02;
   }

   /** 
    * Adds the scaled tensor product
    * <pre>
    *  s (A (X) B)
    * </pre>
    * to a 4th order constitutive tensor represented as a
    * 6 x 6 matrix. Here, A and B are 3 x 3 matrices (assumed to be symmetric),
    * and <code>(X)</code> denotes a tensor product.
    *
    * @param D 6 x 6 matrix representation of the tensor to which
    * the product is added
    * @param s scale factor
    * @param A matrix for forming the product, assumed to be
    * symmetric with only the upper triangular components used.
    * @param B matrix for forming the product, assumed to be
    * symmetric with only the upper triangular components used.
    */
   public static void addTensorProduct (Matrix6d D, double s, Matrix3dBase A, Matrix3dBase B) {

      double a00 = s*A.m00;
      double a11 = s*A.m11;
      double a22 = s*A.m22;
      double a01 = s*A.m01;
      double a02 = s*A.m02;
      double a12 = s*A.m12;

      double b00 = B.m00;
      double b11 = B.m11;
      double b22 = B.m22;
      double b01 = B.m01;
      double b02 = B.m02;
      double b12 = B.m12;

      D.m00 += a00*b00;
      D.m01 += a00*b11;
      D.m02 += a00*b22;
      D.m03 += a00*b01;
      D.m04 += a00*b12;
      D.m05 += a00*b02;
	
      D.m10 += a11*b00;
      D.m11 += a11*b11;
      D.m12 += a11*b22;
      D.m13 += a11*b01;
      D.m14 += a11*b12;
      D.m15 += a11*b02;
	
      D.m20 += a22*b00;
      D.m21 += a22*b11;
      D.m22 += a22*b22;
      D.m23 += a22*b01;
      D.m24 += a22*b12;
      D.m25 += a22*b02;
	
      D.m30 += a01*b00;
      D.m31 += a01*b11;
      D.m32 += a01*b22;
      D.m33 += a01*b01;
      D.m34 += a01*b12;
      D.m35 += a01*b02;
	
      D.m40 += a12*b00;
      D.m41 += a12*b11;
      D.m42 += a12*b22;
      D.m43 += a12*b01;
      D.m44 += a12*b12;
      D.m45 += a12*b02;
	
      D.m50 += a02*b00;
      D.m51 += a02*b11;
      D.m52 += a02*b22;
      D.m53 += a02*b01;
      D.m54 += a02*b12;
      D.m55 += a02*b02;
   }

   /** 
    * Adds the scaled tensor product
    * <pre>
    *  s ( I (X) I )
    * </pre>
    * to a symmetric 4th order constitutive tensor represented as a
    * 6 x 6 matrix. Here, s is a scalar, I is the
    * 3 x 3 identity matrix, and <code>X</code>
    * denotes a tensor product.
    * 
    * @param D 6 x 6 matrix representation of the tensor to which
    * the product is added
    * @param s scaling factor for the product
    */
   public static void addScaledIdentityProduct (Matrix6d D, double s) {

      D.m00 += s;
      D.m01 += s;
      D.m02 += s;

      D.m10 += s;
      D.m11 += s;
      D.m12 += s;

      D.m20 += s;
      D.m21 += s;
      D.m22 += s;
   }


   /**
    * Adds a scaled identity to a 4th order constitutive tensor represented as a
    * 6 x 6 matrix.
    *
    * @param s scaling factor for the identity
    */
   public static void addScaledIdentity (Matrix6d D, double s) {

      double halfs = s*0.5;

      D.m00 += s;
      D.m11 += s;
      D.m22 += s;
      D.m33 += halfs;
      D.m44 += halfs;
      D.m55 += halfs;
   }

   /** 
    * Adds the scaled tensor product
    * <pre>
    *  s ( a X a X a X a )
    * </pre>
    * to a symmetric 4th order constitutive tensor represented as a
    * 6 x 6 matrix. Here, s is a scalar, a is a 3-vector,
    * and <code>X</code> denotes a tensor product.
    * 
    * @param D 6 x 6 matrix representation of the tensor to which
    * the product is added
    * @param s scaling factor for the product
    * @param a vector defining the product
    */
   public static void addScaled4thPowerProduct (
      Matrix6d D, double s, Vector3d a) {

      double xx = a.x*a.x;
      double yy = a.y*a.y;
      double zz = a.z*a.z;
      double xy = a.x*a.y;
      double yz = a.y*a.z;
      double xz = a.x*a.z;
 
      double sxxxy = s*xx*xy;
      double sxxxz = s*xx*xz;
      double sxxyy = s*xx*yy;
      double sxxyz = s*xx*yz;
      double sxxzz = s*xx*zz;
      double sxyyy = s*xy*yy;
      double sxyyz = s*xy*yz;
      double sxyzz = s*xy*zz;
      double sxzzz = s*xz*zz;
      double syyyz = s*yy*yz;
      double syyzz = s*yy*zz;
      double syzzz = s*yz*zz;
 
      D.m00 += s*xx*xx; // x x x x
      D.m01 += sxxyy;   // x x y y
      D.m02 += sxxzz;   // x x z z
      D.m03 += sxxxy;   // x x x y
      D.m04 += sxxyz;   // x x y z
      D.m05 += sxxxz;   // x x x z

      D.m10 += sxxyy;   // y y x x
      D.m11 += s*yy*yy; // y y y y
      D.m12 += syyzz;   // y y z z
      D.m13 += sxyyy;   // y y x y
      D.m14 += syyyz;   // y y y z
      D.m15 += sxyyz;   // y y x z

      D.m20 += sxxzz;   // z z x x
      D.m21 += syyzz;   // z z y y
      D.m22 += s*zz*zz; // z z z z
      D.m23 += sxyzz;   // z z x y
      D.m24 += syzzz;   // z z y z
      D.m25 += sxzzz;   // z z x z

      D.m30 += sxxxy;   // x y x x
      D.m31 += sxyyy;   // x y y y
      D.m32 += sxyzz;   // x y z z
      D.m33 += sxxyy;   // x y x y
      D.m34 += sxyyz;   // x y y z
      D.m35 += sxxyz;   // x y x z

      D.m40 += sxxyz;   // y z x x
      D.m41 += syyyz;   // y z y y
      D.m42 += syzzz;   // y z z z
      D.m43 += sxyyz;   // y z x y
      D.m44 += syyzz;   // y z y z
      D.m45 += sxyzz;   // y z x z

      D.m50 += sxxxz;   // x z x x
      D.m51 += sxyyz;   // x z y y
      D.m52 += sxzzz;   // x z z z
      D.m53 += sxxyz;   // x z x y
      D.m54 += sxyzz;   // x z y z
      D.m55 += sxxzz;   // x z x z
   }

   /** 
    * Adds the scaled symmetric tensor product
    * <pre>
    *  s (A_ik A_jl + A_il A_jk)/2 
    * </pre>
    * to a symmetric 4th order constitutive tensor represented as a
    * 6 x 6 matrix. Here, s is a scalar and A is a 3 x 3 matrix (assumed
    * to be symmetric).
    * 
    * <p>Note that this method sets only the upper triangular components of D.
    * 
    * @param D 6 x 6 matrix representation of the tensor to which
    * the product is added
    * @param s scaling factor for the product
    * @param A matrix used to form the product (assumed symmetric with
    * only the upper triangular part used)
    */
   public static void addTensorProduct4 (Matrix6d D, double s, Matrix3dBase A) {

      double a00 = A.m00;
      double a11 = A.m11;
      double a22 = A.m22;
      double a01 = A.m01;
      double a02 = A.m02;
      double a12 = A.m12;

      double s00 = s*a00;
      double s11 = s*a11;
      double s22 = s*a22;
      double s01 = s*a01;
      double s02 = s*a02;
      double s12 = s*a12;

      D.m00 += s00*a00; // 0000 -> 00x00
      D.m01 += s01*a01; // 0011 -> 01x01
      D.m02 += s02*a02; // 0022 -> 02x02
      D.m03 += s00*a01; // 0001 -> 01x00
      D.m04 += s01*a02; // 0012 -> 01x02
      D.m05 += s00*a02; // 0002 -> 00x02
	
      D.m11 += s11*a11; // 1111 -> 11x11
      D.m12 += s12*a12; // 1122 -> 12x12
      D.m13 += s01*a11; // 1101 -> 11x01
      D.m14 += s11*a12; // 1112 -> 11x12
      D.m15 += s01*a12; // 1102 -> 12x01
	
      D.m22 += s22*a22; // 2222 -> 22x22
      D.m23 += s02*a12; // 2201 -> 12x02
      D.m24 += s12*a22; // 2212 -> 22x12
      D.m25 += s02*a22; // 2202 -> 22x02
	
      D.m33 += (s00*a11 + s01*a01)*0.5; // 0101 -> 0.5*(00x11 + 01x01)
      D.m34 += (s01*a12 + s02*a11)*0.5; // 0112 -> 0.5*(01x12 + 02x11)
      D.m35 += (s00*a12 + s02*a01)*0.5; // 0102 -> 0.5*(00x12 + 02x01)

      D.m44 += (s11*a22 + s12*a12)*0.5; // 1212 -> 0.5*(11x22 + 12x12)
      D.m45 += (s01*a22 + s02*a12)*0.5; // 1202 -> 0.5*(01x22 + 12x02)

      D.m55 += (s00*a22 + s02*a02)*0.5; // 0202 -> 0.5*(00x22 + 02x02)
   }

   /** 
    * Adds the scaled symmetric tensor product
    * <pre>
    * s (A_ik B_jl + A_il B_jk)/2 + (B_ik A_jl + B_il A_jk)/2
    * </pre>
    * to a symmetric 4th order constitutive tensor represented as a
    * 6 x 6 matrix. Here, s is a scalar and A is a 3 x 3 matrix (assumed
    * to be symmetric).
    * 
    * <p>Note that this method sets only the upper triangular components of D.
    * 
    * @param D 6 x 6 matrix representation of the tensor to which
    * the product is added
    * @param s scaling factor for the product
    * @param A first matrix in the product (assumed symmetric with
    * only the upper triangular part used)
    * @param B second matrix in the product (assumed symmetric with
    * only the upper triangular part used)
    */
   public static void addSymmetricTensorProduct4 (
      Matrix6d D, double s, Matrix3dBase A, Matrix3dBase B) {

      double a00 = s*A.m00;
      double a11 = s*A.m11;
      double a22 = s*A.m22;
      double a01 = s*A.m01;
      double a02 = s*A.m02;
      double a12 = s*A.m12;

      double b00 = B.m00;
      double b11 = B.m11;
      double b22 = B.m22;
      double b01 = B.m01;
      double b02 = B.m02;
      double b12 = B.m12;

      D.m00 += 2*a00*b00;
      D.m01 += 2*a01*b01;
      D.m02 += 2*a02*b02;
      D.m03 += a01*b00 + a00*b01;
      D.m04 += a02*b01 + a01*b02;
      D.m05 += a02*b00 + a00*b02;

      D.m11 += 2*a11*b11;
      D.m12 += 2*a12*b12;
      D.m13 += a11*b01 + a01*b11;
      D.m14 += a12*b11 + a11*b12;
      D.m15 += a12*b01 + a01*b12;

      D.m22 += 2*a22*b22;
      D.m23 += a12*b02 + a02*b12;
      D.m24 += a22*b12 + a12*b22;
      D.m25 += a22*b02 + a02*b22;

      D.m33 += 0.5*(a11*b00 + 2*a01*b01 + a00*b11);
      D.m34 += 0.5*(a12*b01 + a11*b02 + a02*b11 + a01*b12);
      D.m35 += 0.5*(a12*b00 + a02*b01 + a01*b02 + a00*b12);

      D.m44 += 0.5*(a22*b11 + 2*a12*b12 + a11*b22);
      D.m45 += 0.5*(a22*b01 + a12*b02 + a02*b12 + a01*b22);

      D.m55 += 0.5*(a22*b00 + 2*a02*b02 + a00*b22);
   }

   private static double getTanMat (Matrix6d D, int i, int j) {

      // locations in D for a given indices in stiffness tensor
      int[] DtoC = new int[] { 0, 3, 5, 3, 1, 4, 5, 4, 2 };
      return D.get (DtoC[i], DtoC[j]);
   }

   private static void setTanMat (Matrix6d D, int i, int j, double value) {

      // locations in D for a given indices in stiffness tensor
      int[] DtoC = new int[] { 0, 3, 5, 3, 1, 4, 5, 4, 2 };
      D.set (DtoC[i], DtoC[j], value);
   }

   private static double rotationSum (
      Matrix6d DT, int idx, int i, int j, int k, int l, Matrix3dBase R) {

      double sum = 0;
      double rx0 = 0;
      double rx1 = 0;
      double rx2 = 0;
      int row = -1;
      switch (idx) {
         case 0: row = i; break;
         case 1: row = j; break;
         case 2: row = k; break;
         case 3: row = l; break;
      }
      switch (row) {
         case 0: rx0 = R.m00; rx1 = R.m01; rx2 = R.m02; break;
         case 1: rx0 = R.m10; rx1 = R.m11; rx2 = R.m12; break;
         case 2: rx0 = R.m20; rx1 = R.m21; rx2 = R.m22; break;
      }
      switch (idx) {
         case 0: {
            sum = (rx0*getTanMat(DT,0*3+j,k*3+l) + 
                   rx1*getTanMat(DT,1*3+j,k*3+l) +
                   rx2*getTanMat(DT,2*3+j,k*3+l));
            break;
         }
         case 1: {
            sum = (rx0*getTanMat(DT,i*3+0,k*3+l) + 
                   rx1*getTanMat(DT,i*3+1,k*3+l) +
                   rx2*getTanMat(DT,i*3+2,k*3+l));
            break;
         }
         case 2: {
            sum = (rx0*getTanMat(DT,i*3+j,0*3+l) + 
                   rx1*getTanMat(DT,i*3+j,1*3+l) +
                   rx2*getTanMat(DT,i*3+j,2*3+l));
            break;
         }
         case 3: {
            sum = (rx0*getTanMat(DT,i*3+j,k*3+0) + 
                   rx1*getTanMat(DT,i*3+j,k*3+1) +
                   rx2*getTanMat(DT,i*3+j,k*3+2));
            break;
         }
      }
      return sum;
   }

   /**
    * Rotates a 6x6 material tangent matrix into a new coordinate system.
    * The rotation is specified by a rotation matrix R that transforms
    * from the new coordinate system into the current one.
    *
    * The transformation is based on the general 4th order elasticity tensor
    * rotation formula
    * <p>
    * A'_ijkl = sum R_ai R_bj R_ck R_dl A_abcd
    * </p>
    * but is optimized and also accomodates the fact that the 6x6 tangent
    * matrix is a reduced version of the general elasticity tensor. It
    * also assumes that the elasticity tensor is symmetric. Each
    * entry in the tangent matrix corresponds to ijkl indices of the
    * elasticity tensor as follows:
    * <p>
    *  [  0000 0011 0022 0001 0012 0002
    *     1100 1111 1122 1101 1112 1102
    *     2200 2211 2222 2201 2212 2202
    *     0100 0111 0122 0101 0112 0102
    *     1200 1211 1222 1201 1212 1202
    *     0200 0211 0222 0201 0212 0202
    *  ]
    * </p>
    * Note that this indexing convention follows that used in FEBio but is
    * not standard; in particular, the lower and right entries are sometimes
    * interchanged depending on how the stress/strain tensors are vectorized.
    * 
    * @param DR result is returned here
    * @param D1 original tangent matrix to rotate
    * @param R rotation matrix
    */
   public static void rotateTangent (Matrix6d DR, Matrix6d D1, Matrix3dBase R) {

      Matrix6d DX;
      if (DR == D1) {
         DX = new Matrix6d ();
      }
      else {
         DX = DR;
      }
      Matrix6d DT = new Matrix6d(); // stores intermediate values

      DX.set (D1);
      for (int idx=0; idx<4; idx++) {
         DT.set (DX);
         // DX.m00 = rotationSum (DT, idx, 0, 0, 0, 0, R);
         // DX.m01 = rotationSum (DT, idx, 0, 0, 1, 1, R);
         // DX.m02 = rotationSum (DT, idx, 0, 0, 2, 2, R);
         // DX.m03 = rotationSum (DT, idx, 0, 0, 0, 1, R);
         // DX.m04 = rotationSum (DT, idx, 0, 0, 1, 2, R);
         // DX.m05 = rotationSum (DT, idx, 0, 0, 0, 2, R);

         // DX.m11 = rotationSum (DT, idx, 1, 1, 1, 1, R);
         // DX.m12 = rotationSum (DT, idx, 1, 1, 2, 2, R);
         // DX.m13 = rotationSum (DT, idx, 1, 1, 0, 1, R);
         // DX.m14 = rotationSum (DT, idx, 1, 1, 1, 2, R);
         // DX.m15 = rotationSum (DT, idx, 1, 1, 0, 2, R);

         // DX.m22 = rotationSum (DT, idx, 2, 2, 2, 2, R);
         // DX.m23 = rotationSum (DT, idx, 2, 2, 0, 1, R);
         // DX.m24 = rotationSum (DT, idx, 2, 2, 1, 2, R);
         // DX.m25 = rotationSum (DT, idx, 2, 2, 0, 2, R);

         // DX.m33 = rotationSum (DT, idx, 0, 1, 0, 1, R);
         // DX.m34 = rotationSum (DT, idx, 0, 1, 1, 2, R);
         // DX.m35 = rotationSum (DT, idx, 0, 1, 0, 2, R);

         // DX.m44 = rotationSum (DT, idx, 1, 2, 1, 2, R);
         // DX.m45 = rotationSum (DT, idx, 1, 2, 0, 2, R);

         // DX.m55 = rotationSum (DT, idx, 0, 2, 0, 2, R);

         // // take advantage of symmetry
         // DX.setLowerToUpper();

         for (int i=0; i<3; i++) {
            for (int j=0; j<3; j++) {
               for (int k=0; k<3; k++) {
                  for (int l=0; l<3; l++) {
                     double sum = 0;
                     for (int a=0; a<3; a++) {
                        switch (idx) {
                           case 0: {
                              sum += R.get(i,a)*getTanMat(DT,a*3+j,k*3+l);
                              break;
                           }
                           case 1:{
                              sum += R.get(j,a)*getTanMat(DT,i*3+a,k*3+l);  
                              break;
                           }
                           case 2: {
                              sum += R.get(k,a)*getTanMat(DT,i*3+j,a*3+l);  
                              break;
                           }
                           case 3:{
                              sum += R.get(l,a)*getTanMat(DT,i*3+j,k*3+a);  
                              break;
                           }
                        }
                     }
                     setTanMat (DX, i*3+j, k*3+l, sum);
                  }
               }
            } 
         }
      }

      if (DR == D1) {
         DR.set (DX);
      }
   }


}
