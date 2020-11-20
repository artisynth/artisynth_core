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
   public static void rotateTangentOld (Matrix6d DR, Matrix6d D1, Matrix3dBase R) {

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
         DX.m00 = rotationSum (DT, idx, 0, 0, 0, 0, R);
         DX.m01 = rotationSum (DT, idx, 0, 0, 1, 1, R);
         DX.m02 = rotationSum (DT, idx, 0, 0, 2, 2, R);
         DX.m03 = rotationSum (DT, idx, 0, 0, 0, 1, R);
         DX.m04 = rotationSum (DT, idx, 0, 0, 1, 2, R);
         DX.m05 = rotationSum (DT, idx, 0, 0, 0, 2, R);

         DX.m11 = rotationSum (DT, idx, 1, 1, 1, 1, R);
         DX.m12 = rotationSum (DT, idx, 1, 1, 2, 2, R);
         DX.m13 = rotationSum (DT, idx, 1, 1, 0, 1, R);
         DX.m14 = rotationSum (DT, idx, 1, 1, 1, 2, R);
         DX.m15 = rotationSum (DT, idx, 1, 1, 0, 2, R);

         DX.m22 = rotationSum (DT, idx, 2, 2, 2, 2, R);
         DX.m23 = rotationSum (DT, idx, 2, 2, 0, 1, R);
         DX.m24 = rotationSum (DT, idx, 2, 2, 1, 2, R);
         DX.m25 = rotationSum (DT, idx, 2, 2, 0, 2, R);

         DX.m33 = rotationSum (DT, idx, 0, 1, 0, 1, R);
         DX.m34 = rotationSum (DT, idx, 0, 1, 1, 2, R);
         DX.m35 = rotationSum (DT, idx, 0, 1, 0, 2, R);

         DX.m44 = rotationSum (DT, idx, 1, 2, 1, 2, R);
         DX.m45 = rotationSum (DT, idx, 1, 2, 0, 2, R);

         DX.m55 = rotationSum (DT, idx, 0, 2, 0, 2, R);

         // take advantage of symmetry
         DX.setLowerToUpper();

         // for (int i=0; i<3; i++) {
         //    for (int j=0; j<3; j++) {
         //       for (int k=0; k<3; k++) {
         //          for (int l=0; l<3; l++) {
         //             double sum = 0;
         //             for (int a=0; a<3; a++) {
         //                switch (idx) {
         //                   case 0: {
         //                      sum += R.get(i,a)*getTanMat(DT,a*3+j,k*3+l);
         //                      break;
         //                   }
         //                   case 1:{
         //                      sum += R.get(j,a)*getTanMat(DT,i*3+a,k*3+l);  
         //                      break;
         //                   }
         //                   case 2: {
         //                      sum += R.get(k,a)*getTanMat(DT,i*3+j,a*3+l);  
         //                      break;
         //                   }
         //                   case 3:{
         //                      sum += R.get(l,a)*getTanMat(DT,i*3+j,k*3+a);  
         //                      break;
         //                   }
         //                }
         //             }
         //             setTanMat (DX, i*3+j, k*3+l, sum);
         //          }
         //       }
         //    } 
         // }
      }

      if (DR == D1) {
         DR.set (DX);
      }
   }
   
   /**
    * Creates a 6x6 rotation matrix for transforming the elasticity tensor
    * From <a href=http://www.brown.edu/Departments/Engineering/Courses/EN224/anis_general/anis_general.htm>Brown University Notes</a>
    * @param T output 6x6 matrix
    * @param R 3D rotation matrix
    */
   public static void createElasticityRotationOld(Matrix6d T, Matrix3dBase R) {
      
      // non-standard Vogt notation 00, 11, 22, 01, 12, 02
      // top left
      T.m00 = R.m00*R.m00;
      T.m01 = R.m01*R.m01;
      T.m02 = R.m02*R.m02;
      T.m10 = R.m10*R.m10;
      T.m11 = R.m11*R.m11;
      T.m12 = R.m12*R.m12;
      T.m20 = R.m20*R.m20;
      T.m21 = R.m21*R.m21;
      T.m22 = R.m22*R.m22;
      
      // top right
      
      T.m05 = 2*R.m01*R.m02;
      T.m03 = 2*R.m02*R.m00;
      T.m04 = 2*R.m00*R.m01;
      T.m15 = 2*R.m11*R.m12;
      T.m13 = 2*R.m12*R.m10;
      T.m14 = 2*R.m10*R.m11;
      T.m25 = 2*R.m21*R.m22;
      T.m23 = 2*R.m22*R.m20;
      T.m24 = 2*R.m20*R.m21;
      
      // bottom left
      T.m50 = R.m10*R.m20;
      T.m51 = R.m11*R.m21;
      T.m52 = R.m12*R.m22;
      T.m30 = R.m20*R.m00;
      T.m31 = R.m21*R.m01;
      T.m32 = R.m22*R.m02;
      T.m40 = R.m00*R.m10;
      T.m41 = R.m01*R.m11;
      T.m42 = R.m02*R.m12;
      
      // bottom right
      T.m55 = R.m11*R.m22+R.m12*R.m21;
      T.m53 = R.m12*R.m20+R.m10*R.m22;
      T.m54 = R.m10*R.m21+R.m11*R.m20;
      T.m35 = R.m21*R.m02+R.m22*R.m01;
      T.m33 = R.m22*R.m00+R.m20*R.m02;
      T.m34 = R.m20*R.m01+R.m21*R.m00;
      T.m45 = R.m01*R.m12+R.m02*R.m11;
      T.m43 = R.m02*R.m10+R.m00*R.m12;
      T.m44 = R.m00*R.m11+R.m01*R.m10;
   }

   public static void createElasticityRotationBrown(Matrix6d T, Matrix3dBase R) {
      // top left
      T.m00 = R.m00*R.m00;
      T.m01 = R.m01*R.m01;
      T.m02 = R.m02*R.m02;
      T.m10 = R.m10*R.m10;
      T.m11 = R.m11*R.m11;
      T.m12 = R.m12*R.m12;
      T.m20 = R.m20*R.m20;
      T.m21 = R.m21*R.m21;
      T.m22 = R.m22*R.m22;

      // top right
      T.m03 = 2*R.m01*R.m02;
      T.m04 = 2*R.m02*R.m00;
      T.m05 = 2*R.m00*R.m01;
      T.m13 = 2*R.m11*R.m12;
      T.m14 = 2*R.m12*R.m10;
      T.m15 = 2*R.m10*R.m11;
      T.m23 = 2*R.m21*R.m22;
      T.m24 = 2*R.m22*R.m20;
      T.m25 = 2*R.m20*R.m21;

      // bottom left
      T.m30 = R.m10*R.m20;
      T.m31 = R.m11*R.m21;
      T.m32 = R.m12*R.m22;
      T.m40 = R.m20*R.m00;
      T.m41 = R.m21*R.m01;
      T.m42 = R.m22*R.m02;
      T.m50 = R.m00*R.m10;
      T.m51 = R.m01*R.m11;
      T.m52 = R.m02*R.m12;

      // bottom right
      T.m33 = R.m11*R.m22 + R.m12*R.m21;
      T.m34 = R.m12*R.m20 + R.m10*R.m22;
      T.m35 = R.m10*R.m21 + R.m11*R.m20;
      T.m43 = R.m21*R.m02 + R.m22*R.m01;
      T.m44 = R.m22*R.m00 + R.m20*R.m02;
      T.m45 = R.m20*R.m01 + R.m21*R.m00;
      T.m53 = R.m01*R.m12 + R.m02*R.m11;
      T.m54 = R.m02*R.m10 + R.m00*R.m12;
      T.m55 = R.m00*R.m11 + R.m01*R.m10;
   }

   public static Matrix6d regularToBrown () {
      Matrix6d RtoB = new Matrix6d();
      RtoB.m00 = 1;
      RtoB.m11 = 1;
      RtoB.m22 = 1;
      RtoB.m34 = 1;
      RtoB.m45 = 1;
      RtoB.m53 = 1;
      return RtoB;
   }

   public static Matrix6d brownToRegular () {
      Matrix6d BtoR = new Matrix6d();
      BtoR.m00 = 1;
      BtoR.m11 = 1;
      BtoR.m22 = 1;
      BtoR.m35 = 1;
      BtoR.m43 = 1;
      BtoR.m54 = 1;
      return BtoR;      
   }

   /**
    * Creates a 6x6 rotation matrix for transforming the elasticity tensor
    * From <a href=http://www.brown.edu/Departments/Engineering/Courses/EN224/anis_general/anis_general.htm>Brown University Notes</a>
    * @param T output 6x6 matrix
    * @param R 3D rotation matrix
    */
   public static void createElasticityRotation(Matrix6d T, Matrix3dBase R) {

      // The Brown reference above uses Vogt notation 00, 11, 22, 12, 02, 01
      //
      // This needs to be mapped to our Vogt notation 00, 11, 22, 01, 12, 02
      //
      // If TB is the 6x6 matrix described in Brown, then our matrix T
      // created from T = P TB P^T, where P is the permutation matrix
      //
      //     [ 1 0 0 0 0 0 ]
      //     [ 0 1 0 0 0 0 ]
      // P = [ 0 0 1 0 0 0 ]
      //     [ 0 0 0 0 0 1 ]
      //     [ 0 0 0 1 0 0 ]
      //     [ 0 0 0 0 1 0 ]

      // top left
      T.m00 = R.m00*R.m00;
      T.m01 = R.m01*R.m01;
      T.m02 = R.m02*R.m02;
      T.m10 = R.m10*R.m10;
      T.m11 = R.m11*R.m11;
      T.m12 = R.m12*R.m12;
      T.m20 = R.m20*R.m20;
      T.m21 = R.m21*R.m21;
      T.m22 = R.m22*R.m22;
      
      // top right
      T.m04 = 2*R.m01*R.m02;
      T.m05 = 2*R.m02*R.m00;
      T.m03 = 2*R.m00*R.m01;
      T.m14 = 2*R.m11*R.m12;
      T.m15 = 2*R.m12*R.m10;
      T.m13 = 2*R.m10*R.m11;
      T.m24 = 2*R.m21*R.m22;
      T.m25 = 2*R.m22*R.m20;
      T.m23 = 2*R.m20*R.m21;

      // bottom left
      T.m40 = R.m10*R.m20;
      T.m41 = R.m11*R.m21;
      T.m42 = R.m12*R.m22;
      T.m50 = R.m20*R.m00;
      T.m51 = R.m21*R.m01;
      T.m52 = R.m22*R.m02;
      T.m30 = R.m00*R.m10;
      T.m31 = R.m01*R.m11;
      T.m32 = R.m02*R.m12;

      // bottom right
      T.m44 = R.m11*R.m22 + R.m12*R.m21;
      T.m45 = R.m12*R.m20 + R.m10*R.m22;
      T.m43 = R.m10*R.m21 + R.m11*R.m20;
      T.m54 = R.m21*R.m02 + R.m22*R.m01;
      T.m55 = R.m22*R.m00 + R.m20*R.m02;
      T.m53 = R.m20*R.m01 + R.m21*R.m00;
      T.m34 = R.m01*R.m12 + R.m02*R.m11;
      T.m35 = R.m02*R.m10 + R.m00*R.m12;
      T.m33 = R.m00*R.m11 + R.m01*R.m10;

   }
   
   /**
    * Creates a 6x6 "unrotation" matrix for transforming the elasticity tensor (i.e. uses R')
    * From <a href=http://www.brown.edu/Departments/Engineering/Courses/EN224/anis_general/anis_general.htm>Brown University Notes</a>
    * @param T output 6x6 matrix
    * @param R 3D rotation matrix
    */
   public static void createElasticityUnrotation(Matrix6d T, Matrix3dBase R) {
      
      // This matrix is the same as that produced by
      // createElasticityRotation(), only using R^T instead of R

      // top left
      T.m00 = R.m00*R.m00;
      T.m01 = R.m10*R.m10;
      T.m02 = R.m20*R.m20;
      T.m10 = R.m01*R.m01;
      T.m11 = R.m11*R.m11;
      T.m12 = R.m21*R.m21;
      T.m20 = R.m02*R.m02;
      T.m21 = R.m12*R.m12;
      T.m22 = R.m22*R.m22;
      
      // top right
      T.m04 = 2*R.m10*R.m20;
      T.m05 = 2*R.m20*R.m00;
      T.m03 = 2*R.m00*R.m10;
      T.m14 = 2*R.m11*R.m21;
      T.m15 = 2*R.m21*R.m01;
      T.m13 = 2*R.m01*R.m11;
      T.m24 = 2*R.m12*R.m22;
      T.m25 = 2*R.m22*R.m02;
      T.m23 = 2*R.m02*R.m12;

      // bottom left
      T.m40 = R.m01*R.m02;
      T.m41 = R.m11*R.m12;
      T.m42 = R.m21*R.m22;
      T.m50 = R.m02*R.m00;
      T.m51 = R.m12*R.m10;
      T.m52 = R.m22*R.m20;
      T.m30 = R.m00*R.m01;
      T.m31 = R.m10*R.m11;
      T.m32 = R.m20*R.m21;

      // bottom right
      T.m44 = R.m11*R.m22 + R.m21*R.m12;
      T.m45 = R.m21*R.m02 + R.m01*R.m22;
      T.m43 = R.m01*R.m12 + R.m11*R.m02;
      T.m54 = R.m12*R.m20 + R.m22*R.m10;
      T.m55 = R.m22*R.m00 + R.m02*R.m20;
      T.m53 = R.m02*R.m10 + R.m12*R.m00;
      T.m34 = R.m10*R.m21 + R.m20*R.m11;
      T.m35 = R.m20*R.m01 + R.m00*R.m21;
      T.m33 = R.m00*R.m11 + R.m10*R.m01;
   }
   
   /**
    * Rotates a 6x6 material tangent matrix into a new coordinate system.
    * The rotation is specified by a rotation matrix R that transforms
    * from the new coordinate system into the current one.
    *
    * @param DR result is returned here
    * @param D1 original tangent matrix to rotate
    * @param R rotation matrix
    */
   public static void rotateTangent (Matrix6d DR, Matrix6d D1, Matrix3dBase R) {
      Matrix6d T = new Matrix6d();
      createElasticityRotation(T, R);
      DR.mul(T, D1);
      DR.mulTranspose(T);

      // Matrix6d T = new Matrix6d();

      // Matrix6d B = new Matrix6d();
      // Matrix6d RtoB = regularToBrown();
      // Matrix6d BtoR = brownToRegular();
      // B.mul (D1, BtoR);
      // B.mul (RtoB, B);
      // createElasticityRotationBrown(T, R);
      // B.mul(T, B);
      // B.mulTranspose(T);
      // DR.mul (B, RtoB);
      // DR.mul (BtoR, DR);
   }
   
   /**
    * Rotates a 6x6 material tangent matrix into a new coordinate system.
    * The rotation is specified by the transpose of rotation matrix R
    *
    * @param DR result is returned here
    * @param D1 original tangent matrix to rotate
    * @param R rotation to "unrotate"
    */
   public static void unrotateTangent (Matrix6d DR, Matrix6d D1, Matrix3dBase R) {
      Matrix6d T = new Matrix6d();
      createElasticityUnrotation(T, R);
      DR.mul(T, D1);
      DR.mulTranspose(T);
   }

   public static void mulTangent (
      SymmetricMatrix3d sig, Matrix6d D, SymmetricMatrix3d eps) {

      double e00 = eps.m00;
      double e11 = eps.m11;
      double e22 = eps.m22;
      double e01 = eps.m01;
      double e02 = eps.m02;
      double e12 = eps.m12;

      // perform multiplication
      double s00 = D.m00*e00 + D.m01*e11 + D.m02*e22 +
         2*D.m03*e01 + 2*D.m04*e12 + 2*D.m05*e02;
      double s11 = D.m10*e00 + D.m11*e11 + D.m12*e22 +
         2*D.m13*e01 + 2*D.m14*e12 + 2*D.m15*e02;
      double s22 = D.m20*e00 + D.m21*e11 + D.m22*e22 +
         2*D.m23*e01 + 2*D.m24*e12 + 2*D.m25*e02;
      double s01 = D.m30*e00 + D.m31*e11 + D.m32*e22 +
         2*D.m33*e01 + 2*D.m34*e12 + 2*D.m35*e02;
      double s12 = D.m40*e00 + D.m41*e11 + D.m42*e22 +
         2*D.m43*e01 + 2*D.m44*e12 + 2*D.m45*e02;
      double s02 = D.m50*e00 + D.m51*e11 + D.m52*e22 +
         2*D.m53*e01 + 2*D.m54*e12 + 2*D.m55*e02;

      sig.set(s00, s11, s22, s01, s02, s12);

   }
   
   public static void main(String[] args) {
      
      Matrix6d D = new Matrix6d();
      LinearMaterial lmat = new LinearMaterial(150, 0.25, false);

      //lmat.computeTangent(D, null, null, null, null);

      // Just create a general symmetric D for testing
      D.setRandom();
      Matrix6d DT = new Matrix6d();
      DT.transpose (D);
      D.add (DT);
      
      RotationMatrix3d R = new RotationMatrix3d();
      R.setRandom();
      //R.set(RotationMatrix3d.ROT_X_90);
      RotationMatrix3d Rinv = new RotationMatrix3d();
      Rinv.transpose(R);
      
      // rotate D
      Matrix6d Dr = new Matrix6d();
      Matrix6d Dur = new Matrix6d();
      TensorUtils.rotateTangent(Dr, D, R);
      TensorUtils.rotateTangent(Dur, Dr, Rinv);
      if (!D.epsilonEquals(Dur, 1e-5)) {
         System.err.println("Rotated tangents not equal");
         Matrix6d Derr = new Matrix6d();
         Derr.sub (D, Dur);
         System.out.println ("D=\n" + D.toString("%12.8f"));
         System.out.println ("Dr=\n" + Dr.toString("%12.8f"));
         System.out.println ("Err=\n" + Derr.toString("%12.8f"));
      } else {
         System.out.println("Rotated tangents are equal");
      }
     
      TensorUtils.unrotateTangent(Dur, Dr, R);
      if (!D.epsilonEquals(Dur, 1e-5)) {
         System.err.println("Rotated tangents not equal");
         Matrix6d Derr = new Matrix6d();
         Derr.sub (D, Dur);
         System.out.println ("D=\n" + D.toString("%12.8f"));
         System.out.println ("Dr=\n" + Dr.toString("%12.8f"));
         System.out.println ("Err=\n" + Derr.toString("%12.8f"));
      } else {
         System.out.println("Rotated tangents are equal");
      }


      // System.out.println ("// top left");
      // for (int i=0; i<3; i++) {
      //    for (int j=0; j<3; j++) {
      //       System.out.println (
      //          "T.m"+i+""+j+ " = R.m"+i+""+j+"*R.m"+i+""+j+ ";");
      //    }
      // }
      // System.out.println ("");
      // System.out.println ("// top right");
      // for (int i=0; i<3; i++) {
      //    for (int j=0; j<3; j++) {
      //       System.out.println (
      //          "T.m"+i+""+(j+3)+ " = 2*R.m"+i+""+(j+1)%3+"*R.m"+i+""+(j+2)%3+";");
      //    }
      // }
      // System.out.println ("");
      // System.out.println ("// bottom left");
      // for (int i=0; i<3; i++) {
      //    for (int j=0; j<3; j++) {
      //       System.out.println (
      //          "T.m"+(i+3)+""+j+ " = R.m"+(i+1)%3+""+j+"*R.m"+(i+2)%3+""+j+ ";");
      //    }
      // }
      // System.out.println ("");
      // System.out.println ("// bottom right");
      // for (int i=0; i<3; i++) {
      //    for (int j=0; j<3; j++) {
      //       System.out.println (
      //          "T.m"+(i+3)+""+(j+3)+ " = " +
      //          "R.m"+(i+1)%3+""+(j+1)%3+"*R.m"+(i+2)%3+""+(j+2)%3+ " + " +
      //          "R.m"+(i+1)%3+""+(j+2)%3+"*R.m"+(i+2)%3+""+(j+1)%3+ ";");
      //    }
      // }

      // System.out.println ("ALT");

      // System.out.println ("// top left");
      // for (int i=0; i<3; i++) {
      //    for (int j=0; j<3; j++) {
      //       System.out.println (
      //          "T.m"+i+""+j+ " = R.m"+i+""+j+"*R.m"+i+""+j+ ";");
      //    }
      // }
      // System.out.println ("");
      // System.out.println ("// top right");
      // for (int i=0; i<3; i++) {
      //    for (int j=0; j<3; j++) {
      //       int jj = (j+1)%3;
      //       System.out.println (
      //          "T.m"+i+""+(jj+3)+
      //          " = 2*R.m"+i+""+(j+1)%3+"*R.m"+i+""+(j+2)%3 + ";");
      //    }
      // }
      // System.out.println ("");
      // System.out.println ("// bottom left");
      // for (int i=0; i<3; i++) {
      //    for (int j=0; j<3; j++) {
      //       int ii = (i+2)%3;
      //       int jj = j;
      //       System.out.println (
      //          "T.m"+(ii+3)+""+jj+
      //          " = R.m"+(i+1)%3+""+j+"*R.m"+(i+2)%3+""+j + ";");
      //    }
      // }
      // System.out.println ("");
      // System.out.println ("// bottom right");
      // for (int i=0; i<3; i++) {
      //    for (int j=0; j<3; j++) {
      //       int ii = (i+2)%3;
      //       int jj = (j+1)%3;
      //       System.out.println (
      //          "T.m"+(ii+3)+""+(jj+3)+ " = " +
      //          "R.m"+(i+1)%3+""+(j+1)%3+"*R.m"+(i+2)%3+""+(j+2)%3+ " + " +
      //          "R.m"+(i+1)%3+""+(j+2)%3+"*R.m"+(i+2)%3+""+(j+1)%3+ ";");
      //    }
      // }
      
      
   }

   /**
    * Computes the following formula:
    *   v3a . T4S . v3b
    * 
    * where v3a and v3b are both vector-3, T4S is a symmetrical 4th order 
    * tensor, and . is the dot product symbol.
    * 
    * FEBio: tens4ds::vdotTdotv
    * 
    * @param out
    * Result to be stored in. 3x3 matrix.
    * 
    * @param a
    * Left vector3
    * 
    * @param T
    * Symmetrical 4th order tensor. E.g. material stress matrix.
    * 
    * @param b
    * Right vector3
    */
   public static void v3DotTens4sDotv3(
      Matrix3d out, Vector3d a, Matrix6d T, Vector3d b) {
      
      out.m00 = a.x*(b.x*T.m00 + b.y*T.m03 + b.z*T.m05) + 
                a.y*(b.x*T.m03 + b.y*T.m33 + b.z*T.m35) + 
                a.z*(b.x*T.m05 + b.y*T.m35 + b.z*T.m55);
      
      out.m01 = a.x*(b.y*T.m01 + b.x*T.m03 + b.z*T.m04) + 
                a.y*(b.y*T.m13 + b.x*T.m33 + b.z*T.m34) +
                a.z*(b.y*T.m15 + b.x*T.m35 + b.z*T.m45);
      
      out.m02 = a.x*(b.z*T.m02 + b.y*T.m04 + b.x*T.m05) +
                a.y*(b.z*T.m23 + b.y*T.m34 + b.x*T.m35) + 
                a.z*(b.z*T.m25 + b.y*T.m45 + b.x*T.m55);
      
      out.m10 = a.y*(b.x*T.m01 + b.y*T.m13 + b.z*T.m15) + 
                a.x*(b.x*T.m03 + b.y*T.m33 + b.z*T.m35) + 
                a.z*(b.x*T.m04 + b.y*T.m34 + b.z*T.m45);
      
      out.m11 = a.y*(b.y*T.m11 + b.x*T.m13 + b.z*T.m14) +
                a.x*(b.y*T.m13 + b.x*T.m33 + b.z*T.m34) +
                a.z*(b.y*T.m14 + b.x*T.m34 + b.z*T.m44);
      
      out.m12 = a.y*(b.z*T.m12 + b.y*T.m14 + b.x*T.m15) + 
                a.x*(b.z*T.m23 + b.y*T.m34 + b.x*T.m35) + 
                a.z*(b.z*T.m24 + b.y*T.m44 + b.x*T.m45);
      
      out.m20 = a.z*(b.x*T.m02 + b.y*T.m23 + b.z*T.m25) + 
                a.y*(b.x*T.m04 + b.y*T.m34 + b.z*T.m45) + 
                a.x*(b.x*T.m05 + b.y*T.m35 + b.z*T.m55);
      
      out.m21 = a.z*(b.y*T.m12 + b.x*T.m23 + b.z*T.m24) +
                a.y*(b.y*T.m14 + b.x*T.m34 + b.z*T.m44) + 
                a.x*(b.y*T.m15 + b.x*T.m35 + b.z*T.m45);
      
      out.m22 = a.z*(b.z*T.m22 + b.y*T.m24 + b.x*T.m25) + 
                a.y*(b.z*T.m24 + b.y*T.m44 + b.x*T.m45) + 
                a.x*(b.z*T.m25 + b.y*T.m45 + b.x*T.m55);
   }
   

}
