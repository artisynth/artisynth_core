/* -------------------------------------------------------------------- */
/*      Example program to show the use of the "PARDISO" routine        */
/*      on symmetric linear systems                                     */
/* -------------------------------------------------------------------- */
/*      This program can be downloaded from the following site:         */
/*      http://www.pardiso-project.org                                  */
/*                                                                      */
/*  (C) Olaf Schenk, Department of Computer Science,                    */
/*      University of Basel, Switzerland.                               */
/*      Email: olaf.schenk@unibas.ch                                    */
/* -------------------------------------------------------------------- */

#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include <time.h>

/* PARDISO prototype. */
#if defined(_WIN32) || defined(_WIN64)
#define pardiso_ PARDISO
#else
#define PARDISO pardiso_
#endif
#if defined(MKL_ILP64)
#define MKL_INT long long
#else
#define MKL_INT int
#endif
extern MKL_INT PARDISO
	(void *, MKL_INT *, MKL_INT *, MKL_INT *, MKL_INT *, MKL_INT *,
	double *, MKL_INT *, MKL_INT *, MKL_INT *, MKL_INT *, MKL_INT *,
	MKL_INT *, double *, double *, MKL_INT *);

typedef struct {
   int isSymmetric;
   int nrows;
   int ncols;
   int nvals;
   int *rowOffs;
   int *colIdxs;
   double *vals;
} CRS_MATRIX;

double currentTimeUsec()
{
   struct timeval tv;
   gettimeofday (&tv, (struct timezone*)0);
   return (tv.tv_sec*1000000 + tv.tv_usec);
}

CRS_MATRIX *readCRSMatrix (FILE *fp, int symmetric) {
   CRS_MATRIX *mat = (CRS_MATRIX*)malloc(sizeof(CRS_MATRIX));
   int nrows, ncols, nvals;
   int i;

   int c;

   // skip leading comment
   if ((c = fgetc(fp)) == '#') {
     while (fgetc(fp) != '\n')
   	;
   }
   else {
     ungetc (c, fp);
   }
   if (fscanf (fp, "%d", &nrows) != 1) {
     fprintf (stderr, "Error scanning nrows\n");
     exit (1);
   }
   mat->nrows = nrows;
   mat->ncols = nrows;
   mat->rowOffs = (int*)malloc((nrows+1)*sizeof(int));
   for (i=0; i<nrows; i++) {
     if (fscanf (fp, "%d", &mat->rowOffs[i]) != 1) {
       fprintf (stderr, "Error scanning row offset, i=%d\n", i);    
       exit (1);
     }
     //mat->rowOffs[i]++;
   }
   if (fscanf (fp,"%d", &nvals) != 1) {
     fprintf (stderr, "Error scanning nvals");
     exit (1);
   }
   nvals--;
   printf ("nvals=%d\n", nvals);
   mat->rowOffs[nrows] = nvals+1;
   mat->nvals = nvals;
   mat->colIdxs = (int*)malloc((nvals)*sizeof(int));
   mat->vals = (double*)malloc((nvals)*sizeof(double));
   for (i=0; i<nvals; i++) {
     if (fscanf (fp, "%d", &mat->colIdxs[i]) != 1) {
       fprintf (stderr, "Error scanning col index, i=%d\n", i);    
       exit (1);
     }
     //mat->colIdxs[i]++;
   }
   for (i=0; i<nvals; i++) {
     if (fscanf (fp, "%lg", &mat->vals[i]) != 1) {
       fprintf (stderr, "Error scanning value, i=%d\n", i);    
       exit (1);
     }
   }
   mat->isSymmetric = symmetric;
   return mat;
}

double* readVector (FILE *fp, int size) {
   double *vec = (double*)malloc(size*sizeof(double));
   int i;

   for (i=0; i<size; i++) {
     if (fscanf (fp, "%lg", &vec[i]) != 1) {
       fprintf (stderr, "Error scanning vector value, i=%d\n", i);    
       exit (1);
     }
   }
   return vec;
}

double normVector (double *vec, int size) {
   double sum = 0;
   int i;
   for (i=0; i<size; i++) {
     sum += vec[i]*vec[i];
   }
   return sqrt(sum);
}

void addVector (double *res, double *vec1, double *vec2, int size) {
   int i;
   for (i=0; i<size; i++) {
     res[i] = vec1[i] + vec2[i];
   }
}

void setVector (double *res, double *vec, int size) {
   int i;
   for (i=0; i<size; i++) {
     res[i] = vec[i];
   }
}

void subVector (double *res, double *vec1, double *vec2, int size) {
   int i;
   for (i=0; i<size; i++) {
     res[i] = vec1[i] - vec2[i];
   }
}

void mulVector (double *res, CRS_MATRIX *mat, double *vec) {
   int i, j;
   
   for (i=0; i<mat->nrows; i++) {
     res[i] = 0;
   }
   for (i=0; i<mat->nrows; i++) {
     int off = mat->rowOffs[i]-1;
     int offEnd = mat->rowOffs[i+1]-1;
     double sum = 0;
     while (off < offEnd) {
       int j = mat->colIdxs[off]-1;
       sum += mat->vals[off]*vec[j];
       if (mat->isSymmetric && j > i) {
	 res[j] += mat->vals[off]*vec[i];
       }
       off++;
     }
     res[i] += sum;
   }
}

void printVector (char *fmt, double *vec, int size) {
   int i;

   for (i=0; i<size; i++) {
     printf (fmt, vec[i]);
     printf (" ");
   }
   printf ("\n");
}

void printIVector (int *vec, int size) {
   int i;

   for (i=0; i<size; i++) {
     printf ("%d ", vec[i]);
   }
   printf ("\n");
}

int main (int argc, char **argv) 
{
    /* Matrix data. */

   CRS_MATRIX *M1, *M2;
   double *b1, *x1, *b2, *x2, *r;
   int n;
   int nvals;

   FILE *fp;

   if (argc == 1) {
     fp = stdin;
   }
   else if (argc == 2) {
     fp = fopen (*argv, "r");
   }
   else {
     fprintf (stderr, "Usage: pardisoTestExample [<fileName>]\n");
     exit (1);
   }

   M1 = readCRSMatrix (fp, /*symmetric=*/1);
   n = M1->nrows;
   b1 = readVector (fp, n);
   x1 = (double*)malloc(n*sizeof(double));
   r = (double*)malloc(n*sizeof(double));

   // ja -> M1->colIdxs;
   // a -> M1->vals;
   // ia -> M1->rowOffs

   int      mtype = -2;        /* Real symmetric matrix */
   int      nrhs = 1;          /* Number of right hand sides. */

   /* Internal solver memory pointer pt,                  */
   /* 32-bit: int pt[64]; 64-bit: long int pt[64]         */
   /* or void *pt[64] should be OK on both architectures  */ 
   void    *pt[64]; 

   /* Pardiso control parameters. */
   int      iparm[64];
   int      maxfct, mnum, phase, error, msglvl;

   /* Number of processors. */
   int      num_procs;

   /* Auxiliary variables. */
   char    *var;
   int      i;

   double   ddum;              /* Double dummy */
   int      idum;              /* Integer dummy. */

   double t0, t1;
   
/* -------------------------------------------------------------------- */
/* ..  Setup Pardiso control parameters.                                */
/* -------------------------------------------------------------------- */

   error = 0;
   //solver = 0; /* use sparse direct solver */
   // do we need this, or will init take care of it?
   for (i=0; i<64; i++) {
     pt[i] = (void*)0;
   }
   for (i=0; i<64; i++) {
     iparm[i] = 0;
   }
   iparm[0] = 1; /* Don't use solver default values */
   iparm[1] = 3; /* Fill-in reordering from OpenMP METIS */
   /* Numbers of processors; if 0, defaults to max number or MKL_NUM_THREADS */
   iparm[2] = 0;
   iparm[3] = 0; /* No iterative-direct algorithm */
   iparm[4] = 0; /* No user fill-in reducing permutation */
   iparm[5] = 0; /* Write solution into x */
   iparm[6] = 0; /* Not in use */
   iparm[7] = 0; /* Max numbers of iterative refinement steps */
   iparm[8] = 0; /* Not in use */
   iparm[9] = 13; /* Perturb the pivot elements with 1E-13 */
   iparm[10] = 1; /* Use nonsymmetric permutation and scaling MPS */
   iparm[11] = 0; /* Not in use */
   iparm[12] = 0; /* Maximum weighted matching algorithm is switched-off (default for symmetric). Try iparm[12] = 1 in case of inappropriate accuracy */
   iparm[13] = 0; /* Output: Number of perturbed pivots */
   iparm[14] = 0; /* Not in use */
   iparm[15] = 0; /* Not in use */
   iparm[16] = 0; /* Not in use */
   iparm[17] = -1; /* Output: Number of nonzeros in the factor LU */
   iparm[18] = -1; /* Output: Mflops for LU factorization */
   iparm[19] = 0; /* Output: Numbers of CG Iterations */
   iparm[20] = 1; /* use 1x1 and 2x2 pivoting

   //F77_FUNC(pardisoinit) (pt,  &mtype, &solver, iparm, &error); 

   if (error != 0) 
    {
      if (error == -10 )
	 printf("No license file found \n");
      if (error == -11 )
	 printf("License is expired \n");
      if (error == -12 )
	 printf("Wrong username or hostname \n");
      return 1; 
    }
   else
    { printf("PARDISO license check was successful ... \n");
    }

   /* Numbers of processors, value of OMP_NUM_THREADS */
   var = getenv("OMP_NUM_THREADS");
   if(var != NULL)
      sscanf( var, "%d", &num_procs );
   else {
     printf("Set environment OMP_NUM_THREADS to 1");
     exit(1);
   }
   iparm[2]  = num_procs;
   
   // other special settings
   iparm[1] = 3; // 2 for metis, 0 for AMD
   iparm[9] = 12;
   iparm[10] = 1;
   iparm[12] = 1; // setting this to 2 can cause errors sometimes
   
   maxfct = 1;		/* Maximum number of numerical factorizations.  */
   mnum   = 1;         /* Which factorization to use. */
    
   msglvl = 0;         /* Print statistical information  */
   error  = 0;         /* Initialize error flag */

/* -------------------------------------------------------------------- */
/* ..  Reordering and Symbolic Factorization.  This step also allocates */
/*     all memory that is necessary for the factorization.              */
/* -------------------------------------------------------------------- */
   phase = 11; 

   t0 = currentTimeUsec();
   PARDISO (pt, &maxfct, &mnum, &mtype, &phase,
		      &n, M1->vals, M1->rowOffs, M1->colIdxs, &idum, &nrhs,
		      iparm, &msglvl, &ddum, &ddum, &error);
   t1 = currentTimeUsec();
    
   if (error != 0) {
     printf("ERROR during symbolic factorization: %d\n", error);
     exit(1);
   }
   printf("Analyze: msec=%8.1f\n", (t1-t0)/1000.0);
   printf("Number of nonzeros in factors  = %d\n", iparm[17]);
   printf("Number of factorization MFLOPS = %d\n", iparm[18]);
   
/* -------------------------------------------------------------------- */
/* ..  Numerical factorization.                                         */
/* -------------------------------------------------------------------- */    
   phase = 22;

   t0 = currentTimeUsec();
   PARDISO (pt, &maxfct, &mnum, &mtype, &phase,
		      &n, M1->vals, M1->rowOffs, M1->colIdxs, &idum, &nrhs,
		      iparm, &msglvl, &ddum, &ddum, &error);
   t1 = currentTimeUsec();   

   if (error != 0) {
     printf("ERROR during numerical factorization: %d\n", error);
     exit(2);
   }
   printf("Factor:  msec=%8.1f\n", (t1-t0)/1000.0);

/* -------------------------------------------------------------------- */    
/* ..  Back substitution and iterative refinement.                      */
/* -------------------------------------------------------------------- */    
   phase = 33;

   iparm[7] = 1;       /* Max numbers of iterative refinement steps. */

   t0 = currentTimeUsec();
   PARDISO (pt, &maxfct, &mnum, &mtype, &phase,
		      &n, M1->vals, M1->rowOffs, M1->colIdxs, &idum, &nrhs,
		      iparm, &msglvl, b1, x1, &error);
   t1 = currentTimeUsec();   
   if (error != 0) {
     printf("ERROR during solution: %d\n", error);
     exit(3);
   }

   mulVector (r, M1, x1);
   subVector (r, r, b1, n);
   printf("Solve:   msec=%8.1f\n\n", (t1-t0)/1000.0);
   /* for (i=0; i<n; i++) { */
   /*   printf ("%g ", x1[i]); */
   /* } */
   /* printf ("\n"); */
   printf("residual=%g\n", normVector(r, n));

#if 0
   M2 = readCRSMatrix (fp, /*symmetric=*/1);
   n = M2->nrows;
   b2 = readVector (fp, n);
   x2 = (double*)malloc(n*sizeof(double));

   setVector (x2, x1, n);

   iparm[7] = 1;       /* Max numbers of iterative refinement steps. */
   iparm[3] = 102;

   printf ("maxfct=%d\n", maxfct);
   printf ("mnum=%d\n", mnum);
   printf ("mtype=%d\n", mtype);
   printf ("phase=%d\n", phase);
   printf ("idum=%d\n", idum);
   for (i=0; i<64; i++) {
     printf ("%d ", iparm[i]);
   }
   printf ("\n");

   t0 = currentTimeUsec();
   PARDISO (pt, &maxfct, &mnum, &mtype, &phase,
		      &n, M2->vals, M2->rowOffs, M2->colIdxs, &idum, &nrhs,
		      iparm, &msglvl, b2, x2, &error);
   t1 = currentTimeUsec();   
   if (error != 0) {
     printf("ERROR during solution: %d\n", error);
     exit(3);
   }
   printf ("num iterations=%d\n", iparm[19]);

   for (i=0; i<n; i++) {
     printf ("%g ", x2[i]);
   }
   printf ("\n");
   printf("Solve:   msec=%8.1f\n\n", (t1-t0)/1000.0);

   mulVector (r, M2, x2);
   subVector (r, r, b2, n);
   printf("residual=%g\n", normVector(r, n));
#endif

/* -------------------------------------------------------------------- */    
/* ..  Termination and release of memory.                               */
/* -------------------------------------------------------------------- */    
   phase = -1;                 /* Release internal memory. */
    
   PARDISO (pt, &maxfct, &mnum, &mtype, &phase,
		      &n, &ddum, M1->rowOffs, M1->colIdxs, &idum, &nrhs,
		      iparm, &msglvl, &ddum, &ddum, &error);

   return 0;
} 
