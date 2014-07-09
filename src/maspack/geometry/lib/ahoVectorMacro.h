//#include <stdio.h>

#define SUBVEC(A,B,Y) \
   Y[0] = A[0] - B[0];\
   Y[1] = A[1] - B[1];\
   Y[2] = A[2] - B[2]

#define ADDVEC(A,B,Y) \
   Y[0] = A[0] + B[0];\
   Y[1] = A[1] + B[1];\
   Y[2] = A[2] + B[2]
  
#define CROSS(A,B,Y) \
  Y[0] = A[1]*B[2] - A[2]*B[1]; \
  Y[1] = A[2]*B[0] - A[0]*B[2]; \
  Y[2] = A[0]*B[1] - A[1]*B[0]

#define NORM_SQUARE(a) \
   a[0]*a[0] + a[1]*a[1] + a[2]*a[2]

double determ(double* col0, double* col1, double* col2) {
   double temprow1[3];
   double temprow2[3];
   double temprowcross[3];
   temprow1[0] = col0[1];
   temprow1[1] = col1[1];
   temprow1[2] = col2[1];
   temprow2[0] = col0[2];
   temprow2[1] = col1[2];
   temprow2[2] = col2[2];

   CROSS(temprow1,temprow2, temprowcross);

   return col0[0]*temprowcross[0] + 
         col1[0]*temprowcross[1] +
         col2[0]*temprowcross[2];
}

/*
 * Solve Ax = b using Cramer's rule
 * A must be 3x3 matrix, b 3x1, result is stored in res
 * A contains 3 pointers to 3x1 COLUMN VECTORS!!
 */
void Solve3x3(double* A[3], double *b, double *res) {
   double detA;
   double overDetA;

   detA = determ(A[0], A[1], A[2]);
   if(0 != detA)
      overDetA = 1/detA;
   else
      return;
   
   /*printf("\nb: (%e, %e, %e), A[0]: (%e, %e, %e), A[1]: (%e, %e, %e), A[2]: (%e, %e, %e)",
      b[0],b[1],b[2], A[0][0], A[0][1], A[0][2], A[1][0], A[1][1], A[1][2], A[2][0], A[2][1], A[2][2]);
   */
   res[0] = determ(b   ,A[1],A[2])*overDetA;
   res[1] = determ(A[0],b   ,A[2])*overDetA;
   res[2] = determ(A[0],A[1],b   )*overDetA;
  
   return;
}
