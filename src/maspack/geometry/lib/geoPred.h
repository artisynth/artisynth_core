#ifndef _GEOPRED_H_
#define _GEOPRED_H_

/*
 * This def'n is req'd by shewchuk
 * May be either double or float
 */
#ifndef REAL
#define REAL double
#endif

#define MAX(a,b) ((a)>(b)?(a):(b))
#define MIN(a,b) ((a)<(b)?(a):(b))
#define ABS(a) ((a)<0?-(a):(a))

typedef enum {
   D_SHEWCHUK,
   D_SOS,
   D_COPLANAR
} depthST;

typedef enum {
   D_INTERVAL,
   D_EXACT
} depthCI;

// Flags returned by the intersectSegmentTriangle methods

// segment and triangle intersect
#define GP_INTERSECTS   0x01 
// segment point 0 is outside the (counterclockwise) triangle, according to
// tirebreaking rules
#define GP_S0_OUTSIDE   0x02
// segment point 0 is on the triangle plane, according to exact arithmetic
#define GP_S0_COPLANAR  0x04
// segment point 1 is on the triangle plane, according to exact arithmetic
#define GP_S1_COPLANAR  0x08
// triangle edge 01 is on the segment, according to exact arithmetic
#define GP_E01_ON_SEGMENT  0x10
// triangle edge 12 is on the segment, according to exact arithmetic
#define GP_E12_ON_SEGMENT  0x20
// triangle edge 20 is on the segment, according to exact arithmetic
#define GP_E20_ON_SEGMENT  0x40
// triangle vertex 0 is on the segment, according to exact arithmetic
#define GP_V0_ON_SEGMENT   (GP_E20_ON_SEGMENT|GP_E01_ON_SEGMENT)
// triangle vertex 0 is on the segment, according to exact arithmetic
#define GP_V1_ON_SEGMENT   (GP_E01_ON_SEGMENT|GP_E12_ON_SEGMENT)
// triangle vertex 0 is on the segment, according to exact arithmetic
#define GP_V2_ON_SEGMENT   (GP_E12_ON_SEGMENT|GP_E20_ON_SEGMENT)

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

// Same as nasaOrient3d_d but returns the volume as a two-double
// precision value
int nasaOrient3d_vol(
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

double computeSegmentScale (double *v0, double *v1);

#endif // #define _GEOPRED_H_
