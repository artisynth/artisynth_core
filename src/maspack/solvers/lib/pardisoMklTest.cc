#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <time.h>
#ifndef WINDOWS_COMPILER
#include <sys/time.h>
#endif
#include "pardisoMkl.h"
#if !defined(CYGWIN) && !defined(WINDOWS_COMPILER)
#include <pthread.h>
#endif

#define ABS(x) ((x) < 0 ? -(x) : (x))

#if defined(CYGWIN) || defined(WINDOWS_COMPILER)
#define random rand
#endif

typedef struct {
   double *vals;
   int* rowOffs;
   int* colIdxs;
   int size;
   int nvals;
   int type;
} MatrixInfo;

int readSymmetricMatrixFromFile (
   char *filename, double* vals, int* rows, int* cols)
{
   FILE * fin;
   int i, j;
   double val;

   /* open the file */
   fin = fopen(filename, "r");
   if (!fin) 
    { fprintf (stderr, "Unable to open file %s\n", filename);    
      exit (1);
    }

   int ri = 0;
   int vi = 0;
   int lasti = -1;
   while (fscanf (fin, "%d %d %lf", &i, &j, &val) != EOF)
    { // printf ("%d %d %a\n", i, j, val);
      if (j >= i)
       { vals[vi] = val;
         cols[vi] = j+1;
         if (i != lasti)
          { rows[ri++] = vi+1;
          }
         vi++;
         lasti = i;
       }
    }
   rows[ri] = vi+1;
   printf ("num rows=%d\n", ri);
   return vi;
}

int readVectorFromFile (char *filename, double* vals)
{
   FILE * fin;
   double val;

   /* open the file */
   fin = fopen(filename, "r");
   if (!fin) 
    { fprintf (stderr, "Unable to open file %s\n", filename);    
      exit (1);
    }

   int vi = 0;
   while (fscanf (fin, "%lf", &val) != EOF)
    { vals[vi++] = val;
    }
   return vi;
}

#ifndef WINDOWS_COMPILER
unsigned long currentTimeUsec()
{
   struct timeval tv;
   gettimeofday (&tv, (struct timezone*)0);
   return (tv.tv_sec*1000000 + tv.tv_usec);
}
#endif

void mulSymmetric (double* res, double* vals, int* rows, int* cols,
		   int size, double* b)
{
   int i;
   for (i=0; i<size; i++)
    { res[i] = 0; 
    }
   for (i=0; i<size; i++)
    { int vi_start = rows[i]-1;
      int vi_end = rows[i+1]-1;
      int vi;
      for (vi=vi_start; vi<vi_end; vi++)
       { int j = cols[vi]-1;
         double mij = vals[vi];
         res[i] += mij*b[j];
         if (i != j)
          { res[j] += mij*b[i];
          }
       }	   
    }
}

void sub (double *res, double *vec1, double* vec2, int size)
{
   int i;
   for (i=0; i<size; i++)
    { res[i] = vec1[i] - vec2[i];
    }
}

void addToAll (double *res, double *vec1, double value, int size)
{
   int i;
   for (i=0; i<size; i++)
    { res[i] = vec1[i] + value;
    }
}

void perturb (double *res, double *vec2, double range, int size)
{
   int i;
   for (i=0; i<size; i++)
    { double eps = random()/((double)RAND_MAX)*range;
      res[i] = vec2[i] + eps;
    }
}

void scale (double *res, double s, double *vec1, int size)
{
   int i;
   for (i=0; i<size; i++)
    { res[i] = s*vec1[i];
    }
}

double norm (double* vec, int size)
{
   double sumSqr = 0;
   int i;
   for (i=0; i<size; i++)
    { sumSqr += vec[i]*vec[i];
    }
   return sqrt(sumSqr);
}

double infinityNorm (double* vec, int size)
{
   double max = 0;
   int i;
   for (i=0; i<size; i++)
    { if (ABS(vec[i]) > max)
       { max = ABS(vec[i]); 
       }
    }
   return max;
}

double checkSymmetricResult (
   double* x, double* vals, int* rows, int* cols, double* b, int size)
{
   int i;
   double* check = new double[size];
   
   mulSymmetric (check, vals, rows, cols, size, x);
   sub (check, check, b, size);
   return infinityNorm (check, size);   
}

void solveSymmetricMatrix (void *ptr) {
   
   MatrixInfo *infop = (MatrixInfo*)ptr;
   Pardiso4* pardiso = new Pardiso4();

   int size = infop->size;
   // set test symmetric matrix:
   // M = [3 1 2 0 0
   //      1 0 1 2 0
   //      2 1 4 1 0 
   //      0 2 1 0 6
   //      0 0 0 6 2]
   double *b = (double*)malloc(size*sizeof(double));
   double *x = (double*)malloc(size*sizeof(double));
   for (int i=0; i<size; i++) {
     b[i] = 1;
   }
   pardiso->setMatrix (infop->vals, infop->rowOffs, infop->colIdxs,
		       size, infop->nvals, infop->type);
   pardiso->factorMatrix();
   pardiso->solveMatrix (x, b);
   double tol = checkSymmetricResult (
      x, infop->vals, infop->rowOffs, infop->colIdxs, b, size);
   if (tol > 1e-8) {
     printf ("Error, solve error=%g\n", tol);
     exit (1);
   }
   free (b);
   free (x);
}

void printUsage () {
   printf ("Usage: pardiso4Test [-timingCnt <cnt>]\n");
}

int main (int argc, char** argv) {

   int threadTestCnt = 0;
   int timingCnt = 0;

   while (argc > 1) {
     argc--;
     argv++;
     if (strcmp (*argv, "-help") == 0) {
	 printUsage ();
	 exit (0);
     }
     else if (strcmp (*argv, "-timingCnt") == 0) {
       argc--;
       argv++;
       if (argc <= 0) {
	 printUsage ();
	 exit (1);
       }
       timingCnt = atoi(*argv);
     }
     else {
       printUsage ();
       exit (1);
     }
   }

   int i;
   Pardiso4* pardiso = new Pardiso4();

   // set test symmetric matrix:
   // M = [3 1 2 0 0
   //      1 0 1 2 0
   //      2 1 4 1 0 
   //      0 2 1 0 6
   //      0 0 0 6 2]
   double vals3[] = { 3, 1, 2, 0, 1, 2, 4, 1, 0, 6, 2 };
   int rows3[] = { 1, 4, 7, 9, 11, 12 };
   int cols3[] =    { 1, 2, 3, 2, 3, 4, 3, 4, 4, 5, 5 };
   double b3[] = { 1, 2, 3, 4, 5 };
   double x3[5];
   printf ("setting first matrix:\n");
   pardiso->setMatrix (vals3, rows3, cols3, 5, 11, REAL_SYMMETRIC);
   printf ("factoring first matrix:\n");
   pardiso->factorMatrix();
   printf ("solving first matrix:\n");
   pardiso->solveMatrix (x3, b3);
   printf ("Sparse symmetric:\n");
   for (i=0; i<5; i++)
    { printf ("%8.3f\n", x3[i]); 
    }
   printf ("residual=%g\n", checkSymmetricResult (
              x3, vals3, rows3, cols3, b3, 5));
   // Now change matrix but keep topology:
   // M = [3 1 2 0 0
   //      1 10 1 2 0
   //      2 1 4 1 0 
   //      0 2 1 10 5
   //      0 0 0 5 2]
   double vals4[] = { 3, 1, 2, 10, 1, 2, 4, 1, 10, 5, 2 };
   pardiso->factorMatrix(vals4);
   pardiso->solveMatrix (x3, b3);
   printf ("Sparse symmetric, different values:\n");
   for (i=0; i<5; i++)
    { printf ("%8.3f\n", x3[i]); 
    }
   printf ("residual=%g\n", checkSymmetricResult (
              x3, vals4, rows3, cols3, b3, 5));

   double vals[] = 
      { 1, 2, 3, 0, 4, 0, 5, 0, 6
      };
   int rows[] = 
      { 1, 4, 7, 10
      };
   int cols[] = 
      { 1, 2, 3, 1, 2, 3, 1, 2, 3
      };
   double x[3];
   double b1[] = { 1, 2, 3 };


   pardiso->setMatrix (vals, rows, cols, 3, 9, REAL_UNSYMMETRIC);
   pardiso->factorMatrix();
   pardiso->solveMatrix (x, b1);
   printf ("Dense unsymmetric:\n");
   for (i=0; i<3; i++)
    { printf ("%8.3f\n", x[i]); 
    }
   printf ("Num factors=%d\n", pardiso->getNumNonZerosInFactors());

   double b2[] = { 4, 5, 6 };
   pardiso->solveMatrix (x, b2);
   printf ("Dense unsymmetric, second solution:\n");
   for (i=0; i<3; i++)
    { printf ("%8.3f\n", x[i]); 
    }

   double vals2[] =
      { 26, 2, 33, 20, 6, 45 };
   int rows2[] = 
      { 1, 4, 6, 7
      };
   int cols2[] = 
      { 1, 2, 3, 2, 3, 3
      };

   pardiso->setMatrix (vals2, rows2, cols2, 3, 6, REAL_SYMMETRIC);
   pardiso->factorMatrix();
   pardiso->solveMatrix (x, b1);
   printf ("Dense symmetric:\n");
   for (i=0; i<3; i++)
    { printf ("%8.3f\n", x[i]); 
    }
   printf ("Num factors=%d\n", pardiso->getNumNonZerosInFactors());
   printf ("residual=%g\n", checkSymmetricResult (
              x, vals2, rows2, cols2, b1, 3));

   double vals6[] =
      { 3.2443,   2.6518,   2.3555,   1.4758,   2.1490,   2.6638, 
	          2.8509,   2.2282,   1.7568,   2.1782,   2.1157,
	                    2.6323,   1.4811,   1.6396,   2.6423,
	                              1.9426,   1.0814,   1.2941,
	                                        1.7627,   1.5862,
	                                                  3.3699,
      };

   int rows6[] = 
      { 1, 7, 12, 16, 19, 21, 22
      };
   int cols6[] = 
      { 1, 2, 3, 4, 5, 6,  2, 3, 4, 5, 6,   3, 4, 5, 6,  4, 5, 6,  5, 6,  6
      };
   double b6[] = { 1, 2, 3, 4, 5, 6 };

   pardiso->setMatrix (vals6, rows6, cols6, 6, 21, REAL_SYMMETRIC);
   pardiso->factorMatrix();
   pardiso->solveMatrix (x, b6);
   printf ("Dense symmetric:\n");
   for (i=0; i<6; i++)
    { printf ("%8.3f\n", x[i]); 
    }
   printf ("Num factors=%d\n", pardiso->getNumNonZerosInFactors());
   printf ("residual=%g\n", checkSymmetricResult (
              x, vals6, rows6, cols6, b6, 6));


   // the bug case: KKT system
   // 0.064   0.0003  -0.001  -0.01   1.0
   // 0.0003  0.064   0.0009  1.31     0
   // -0.001  0.0009   0.059   1.0   -1.0
   // -0.01   1.31     1.0     55    -23
   // 1.0      0       -1.0   -23     0

   double vals5[] =
      { 0.064, 0.0003, -0.001, -0.01, 1.0, 
               0.064, 0.0009, 1.31, 
                      0.059,   1.0, -1.0,
                               55, -23,
        0,
      };
   int rows5[] = 
      { 1, 6, 9, 12, 14, 15
      };
   int cols5[] = 
      { 1, 2, 3, 4, 5,  2, 3, 4,  3, 4, 5,  4, 5, 5
      };
   double b5[] = 
      { 1, 2, 3, 4, 5
      };
        
   pardiso->setMatrix (vals5, rows5, cols5, 5, 14, REAL_SYMMETRIC);
   pardiso->factorMatrix();
   pardiso->solveMatrix (x, b5);
   printf ("KKT symmetric:\n");
   for (i=0; i<5; i++)
    { printf ("%8.3f\n", x[i]); 
    }
   printf ("residual=%g\n", checkSymmetricResult (
              x, vals5, rows5, cols5, b5, 5));
   

#define MAXVALS  35000
#define MAXSIZE   1000
   double* valsx = new double[MAXVALS];
   double* bvecx = new double[MAXVALS];
   double* xvecx = new double[MAXVALS];
   double* yvecx = new double[MAXVALS];
   int* colsx = new int[MAXVALS];
   int* rowsx = new int[MAXSIZE];

//   int matSize = readVectorFromFile ("bvec325.txt", bvecx);
//   int numVals = readSymmetricMatrixFromFile (
//      "Xmat325.txt", valsx, rowsx, colsx);

   char* matFileName = "Xmat325.txt";
   char* vecFileName = "bvec325.txt";

   int matSize = readVectorFromFile (vecFileName, bvecx);
   int numVals = readSymmetricMatrixFromFile (
      matFileName, valsx, rowsx, colsx);

   printf ("size=%d\n", matSize);
   printf ("nvals=%d\n", numVals);

// 	printf ("rows=[ ");
// 	for (int i=0; i<matSize; i++)
// 	 { printf ("%d ", rowsx[i]); 
// 	 }
// 	printf ("]\n");

// 	printf ("cols=[ ");
// 	for (int i=0; i<numVals; i++)
// 	 { printf ("%d ", colsx[i]); 
// 	 }
// 	printf ("]\n");

   int status;
   status = pardiso->setMatrix (valsx, rowsx, colsx,
                                matSize, numVals, REAL_SYMMETRIC);

   printf ("status=%d\n", status);
   double maxResidual = 0;

#ifndef WINDOWS_COMPILER
   if (timingCnt > 0) {
     printf ("Testing factor+solve time for matrix of size %d:\n", matSize);
     long totalTime = 0;
     for (int i=0; i<timingCnt; i++)
      { 
	long t0 = currentTimeUsec();
	// change vals slightly each step:
	if (i > 0)
	 { perturb (valsx, valsx, 1e-7, numVals-1);
	 }
	status = pardiso->factorMatrix(valsx);
	if (status != 0)
	 { printf ("Factor failed, code %d\n", status); 
	 }
	status = pardiso->solveMatrix (xvecx, bvecx);
	if (status != 0)
	 { printf ("Solve failed, code %d\n", status); 
	 }
	long t1 = currentTimeUsec();
	totalTime += (t1-t0);
	double residual = checkSymmetricResult (
	   xvecx, valsx, rowsx, colsx, bvecx, matSize);
	if (residual > maxResidual)
	 { maxResidual = residual; 
	 }
      }
     printf ("max residual = %g\n", maxResidual);
     printf ("average time = %g usec\n", totalTime/(double)timingCnt);
   }
#endif

   Pardiso4* pardiso2 = new Pardiso4();

#if !defined(CYGWIN) && !defined(WINDOWS_COMPILER)
   if (threadTestCnt > 0) {
     for (int i=0; i<threadTestCnt; i++) {
       MatrixInfo info;
       info.vals = valsx;
       info.rowOffs = rowsx;
       info.colIdxs = colsx;
       info.size = matSize;
       info.nvals = numVals;
       info.type = REAL_SYMMETRIC;
       pthread_t thread1, thread2;
       pthread_create(&thread1, NULL, solveSymmetricMatrix, (void*)&info);
       pthread_create(&thread2, NULL, solveSymmetricMatrix, (void*)&info);
       pthread_join(thread1, NULL);
       pthread_join(thread2, NULL);
     }
   }
#endif	
}
