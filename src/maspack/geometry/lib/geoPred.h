#ifndef _GEOPRED_H_
#define _GEOPRED_H_

/*
 * This def'n is req'd by shewchuk
 * May be either double or float
 */
#ifndef REAL
#define REAL double
#endif

#define MAX(a,b) a>b?a:b

typedef enum {
   D_SHEWCHUK,
   D_SOS,
   D_COPLANAR
} depthST;

typedef enum {
   D_INTERVAL,
   D_EXACT
} depthCI;

/*
 * exactinit() must be called before using intersectSegmentTriangle
 * or closestIntersection
 * 
 * The other functions declared in this .h file are called from
 * intersectSegmentTriangle and closestIntersection
 * They should not be called directly
 */
REAL exactinit();

int intersectSegmentTriangle_d(
	int is0, double s0[3], int is1, double s1[3],
	int it0, double t0[3], int it1, double t1[3], int it2, double t2[3],
	double point[3], depthST *depth
);

int closestIntersection_d(
	double a[3], double b[3],
	double c0[3], double c1[3], double c2[3], 
	double d0[3], double d1[3], double d2[3], depthCI *depth
);

int intersectSegmentTriangle(
	int is0, double s0[3], int is1, double s1[3],
	int it0, double t0[3], int it1, double t1[3], int it2, double t2[3],
	double point[3]
);

int closestIntersection(
	double a[3], double b[3],
	double c0[3], double c1[3], double c2[3], 
	double d0[3], double d1[3], double d2[3]
);

// Some internal functions

int nasaOrient3d_d(
	int i, double *p,
	int i1, double *p1,
	int i2, double *p2,
	int i3, double *p3,
	double *volume, depthST *depth
);
int nasaOrient3d(
	int i, double *p,
	int i1, double *p1,
	int i2, double *p2,
	int i3, double *p3,
	double *volume
);

int intervalClosestIntersection( double a[3], double b[3], double c0[3], double c1[3], double c2[3], double d0[3], double d1[3], double d2[3], double *answer);

int exactClosestIntersection( double a[3], double b[3], double c0[3], double c1[3], double c2[3], double d0[3], double d1[3], double d2[3], double *answer);

#endif // #define _GEOPRED_H_
