#include <math.h>
#include <fenv.h>
#include <stdio.h>
#include "geoPred.h"
#include "predicatesC.h" // Shewchuck's implementations and macros

#ifdef CYGWIN
#pragma fenv_access (on)
#endif

/* Calculate determinant
 * [a0 a1 1
 *  b0 b1 1
 *  c0 c1 1 ] */
double exactOrient2d(double *a0, double *a1, double *b0, double *b1, double *c0, double *c1, int *err) {
   double a[2], b[2], c[2];
   a[0] = *a0; a[1] = *a1;
   b[0] = *b0; b[1] = *b1;
   c[0] = *c0; c[1] = *c1;
   
   return orient2d(a, b, c); // Call Shewchuck's implementation
}

double exactOrient1d(double *a, double *b) {
   return (*a - *b);
}

/*
 * This is the fallback routine for cases where orient3d really returns 0
 * See Edelsbrunner & Mucke "Simulation of Simplicity: A Technique ..."
 *
 * Also summarized in Appendix A.1 of Aftosmis "Robust and Efficient ..."
 */
int sosOrient3d(int i, double *p, int i1, double *p1, int i2, double *p2, int i3, double *p3, int *err) {
	double *a = p;
	double *b = p1;
	double *c = p2;
	double *d = p3;
	int ai = i;
	int bi = i1;
	int ci = i2;
	int di = i3;
	int sign = 1;
	double *tmp;
	int tmpi;

	if (ai > bi) {
		tmp=b; b=a; a=tmp;
		tmpi=bi; bi=ai; ai=tmpi;
		sign = -sign;
	}
	if (bi > ci) {
		tmp=c; c=b; b=tmp;
		tmpi=ci; ci=bi; bi=tmpi;
		sign = -sign;
	}
	if (ci > di) {
           //tmp=c; c=b; b=tmp;
           //tmpi=ci; ci=bi; bi=tmpi;
                tmp=d; d=c; c=tmp;
                tmpi=di; di=ci; ci=tmpi;
		sign = -sign;
	}
	if (ai > bi) {
		tmp=b; b=a; a=tmp;
		tmpi=bi; bi=ai; ai=tmpi;
		sign = -sign;
	}
	if (bi > ci) {
		tmp=c; c=b; b=tmp;
		tmpi=ci; ci=bi; bi=tmpi;
		sign = -sign;
	}
	if (ai > bi) {
		tmp=b; b=a; a=tmp;
		tmpi=bi; bi=ai; ai=tmpi;
		sign = -sign;
	}

	double v;

/* e^{1/8} */
	v = exactOrient2d(b, b+1, c, c+1, d, d+1, err);
	if (v > 0) return sign > 0;
	if (v < 0) return sign < 0;

/* e^{1/4} */
	v = -exactOrient2d(b, b+2, c, c+2, d, d+2, err);
	if (v > 0) return sign > 0;
	if (v < 0) return sign < 0;

/* e^{1/2} */
	v = exactOrient2d(b+1, b+2, c+1, c+2, d+1, d+2, err);
	if (v > 0) return sign > 0;
	if (v < 0) return sign < 0;

/* e^1 */
	v = -exactOrient2d(a, a+1, c, c+1, d, d+1, err);
	if (v > 0) return sign > 0;
	if (v < 0) return sign < 0;

/* e^{5/4} */
	v = exactOrient1d(c, d);
	if (v > 0) return sign > 0;
	if (v < 0) return sign < 0;

/* e^{3/2} */
	v = -exactOrient1d(c+1, d+1);
	if (v > 0) return sign > 0;
	if (v < 0) return sign < 0;

/* e^2 */
	v = exactOrient2d(a, a+2, c, c+2, d, d+2, err);
	if (v > 0) return sign > 0;
	if (v < 0) return sign < 0;

/* e^{5/2} */
	v = exactOrient1d(c+2, d+2);
	if (v > 0) return sign > 0;
	if (v < 0) return sign < 0;

/* e^4 */
	v = -exactOrient2d(a+1, a+2, c+1, c+2, d+1, d+2, err);
	if (v > 0) return sign > 0;
	if (v < 0) return sign < 0;

/* e^8 */
	v = exactOrient2d(a, a+1, b, b+1, d, d+1, err);
	if (v > 0) return sign > 0;
	if (v < 0) return sign < 0;

/* e^{33/4} */
	v = -exactOrient1d(b, d);
	if (v > 0) return sign > 0;
	if (v < 0) return sign < 0;

/* e^{17/2} */
	v = exactOrient1d(b+1, d+1);
	if (v > 0) return sign > 0;
	if (v < 0) return sign < 0;

/* e^10 */
	v = exactOrient1d(a, d);
	if (v > 0) return sign > 0;
	if (v < 0) return sign < 0;

	return sign > 0;
}

extern int nasaOrient3d(int i, double *p, int i1, double *p1, int i2, double *p2, int i3, double *p3, double *volume ) 
{
   depthST dummy;
   return nasaOrient3d_d(i, p, i1, p1,
      i2, p2, i3, p3, volume, &dummy);
}

/*
 * Returns 1 if the point p is above the plane formed by p1, p2 and p3
 * (oriented counterclockwise). If p is coplanar, SOS tie-breaking is
 * used to create a answer based on a virtual perturbation, using the
 * unique index values i, i1, i2, i3 for each point.
 */
extern int nasaOrient3d_d(int i, double *p, int i1, double *p1, int i2, double *p2, int i3, double *p3, double *volume, depthST *depth) {
   (*depth)=D_SHEWCHUK;
   (*volume) = 0;
   (*volume) = orient3d(p,p1,p2,p3); // Call Shewchuck's orient3d
   if(*volume != 0)
      return (*volume) > 0;

   (*depth)=D_SOS;
   int err;
   int answer = sosOrient3d(i, p, i1, p1, i2, p2, i3, p3, &err);
   return answer;
}

/*
 * Version of nasaOrient3d_d that returns the volume as a two-double
 * precision value.
 */
extern int nasaOrient3d_vol(int i, double *p, int i1, double *p1, int i2, double *p2, int i3, double *p3, double *volume, depthST *depth) {
   (*depth)=D_SHEWCHUK;
   
   // we can call either orient3dDet() or orient3dexactDet(). The
   // former is faster, but may not compute the volume to full
   // precision if the method reaches a "decision" before this needed.
   orient3dexactDet(p,p1,p2,p3,volume);
   if(volume[0] != 0) {
      return (volume[0] > 0);
   }
   (*depth)=D_SOS;
   int err;
   int answer = sosOrient3d(i, p, i1, p1, i2, p2, i3, p3, &err);
   return answer;
}

/* Answer the perpendicular distance from point a to the triangle c0, c1, c2.
 * Calculate using a sum of three-term products of the original coordinates,
 * where the final multiplication in each product is by a positive number,
 * and the terms are summed using only + (not -).
 * This ensures that all rounding occurs in the same direction
 * when rounding to - or + infinity is used. */
double perpendicularDistance(double *a, double *c0, double *c1, double *c2) {
	volatile double c00n = -c0[0];
	volatile double c01n = -c0[1];
	volatile double c02n = -c0[2];
	double r=0;
	if (a[0] < 0) {
		volatile double a0n = -a[0];
		volatile double c12n = -c1[2];
		r += a0n * (
			(c12n * c2[1])
			+ (c02n * c1[1])
			+ (c01n * c2[2])
			+ (c1[1] * c2[2])
			+ (c0[2] * c2[1])
			+ (c0[1] * c1[2])
		);
	} else {
		volatile double c11n = -c1[1];
		r += a[0] * (
			(c1[2] * c2[1])
			+ (c0[2] * c1[1])
			+ (c0[1] * c2[2])
			+ (c11n * c2[2])
			+ (c02n * c2[1])
			+ (c01n * c1[2])
		);
	}
	if (a[1] < 0) {
		volatile double a1n = -a[1];
		volatile double c10n = -c1[0];
		r += a1n * (
			(c10n * c2[2])
			+ (c02n * c2[0])
			+ (c00n * c1[2])
			+ (c1[2] * c2[0])
			+ (c0[2] * c1[0])
			+ (c0[0] * c2[2])
		);
	} else {
		volatile double c12n = -c1[2];
		r += a[1] * (
			(c1[0] * c2[2])
			+ (c0[2] * c2[0])
			+ (c0[0] * c1[2])
			+ (c12n * c2[0])
			+ (c02n * c1[0])
			+ (c00n * c2[2])
		);
	}
	if (a[2] < 0) {
		volatile double a2n = -a[2];
		volatile double c11n = -c1[1];
		r += a2n * (
			(c00n * c2[1])
			+ (c01n * c1[0])
			+ (c11n * c2[0])
			+ (c0[0] * c1[1])
			+ (c0[1] * c2[0])
			+ (c1[0] * c2[1])
		);
	} else {
		volatile double c10n = -c1[0];
		r += a[2] * (
			(c0[0] * c2[1])
			+ (c0[1] * c1[0])
			+ (c1[1] * c2[0])
			+ (c00n * c1[1])
			+ (c01n * c2[0])
			+ (c10n * c2[1])
		);
	}
	if (c0[0] < 0) {
		volatile double c11n = -c1[1];
		r += c00n * (
			(c11n * c2[2])
			+ (c1[2] * c2[1])
		);
	} else {
		volatile double c12n = -c1[2];
		r += c0[0] * (
			(c1[1] * c2[2])
			+ (c12n * c2[1])
		);
	}
	if (c0[1] < 0) {
		volatile double c12n = -c1[2];
		r += c01n * (
			(c12n * c2[0])
			+ (c1[0] * c2[2])
		);
	} else {
		volatile double c10n = -c1[0];
		r += c0[1] * (
			(c1[2] * c2[0])
			+ (c10n * c2[2])
		);
	}
	if (c0[2] < 0) {
		volatile double c10n = -c1[0];
		r += c02n * (
			(c10n * c2[1])
			+ (c1[1] * c2[0])
		);
	} else {
		volatile double c11n = -c1[1];
		r += c0[2] * (
			(c1[0] * c2[1])
			+ (c11n * c2[0])
		);
	}
	return r;
}
/* If interval arithmetic is successful, put result in *answer and return 1.
 *  Otherwise return 0 */

int intervalClosestIntersection(
	double a[3], double b[3],
	double c0[3], double c1[3], double c2[3], 
	double d0[3], double d1[3], double d2[3],
	double *answer
) {
   int feSavedRound = fegetround();
   if( fesetround(FE_DOWNWARD) ) // returns 0 on success
      printf("\nERROR! fesetround returned error %i", fesetround(FE_DOWNWARD));

	double dcaMin = perpendicularDistance(a, c0, c1, c2);
	double dcbMin = perpendicularDistance(b, c0, c1, c2);
	double ddaMin = perpendicularDistance(a, d0, d1, d2);
	double ddbMin = perpendicularDistance(b, d0, d1, d2);
   if( fesetround(FE_UPWARD) )
      printf("\nERROR! fesetround returned error %i", fesetround(FE_UPWARD));

	double dcaMax = perpendicularDistance(a, c0, c1, c2);
	double dcbMax = perpendicularDistance(b, c0, c1, c2);
	double ddaMax = perpendicularDistance(a, d0, d1, d2);
	double ddbMax = perpendicularDistance(b, d0, d1, d2);
	if (
		((dcaMin < 0) != (dcaMax < 0)) |
		((dcbMin < 0) != (dcbMax < 0)) |
		((ddaMin < 0) != (ddaMax < 0)) |
		((ddbMin < 0) != (ddbMax < 0))
	) {
      if( fesetround(feSavedRound) )
         printf("\nERROR! fesetround returned error %i", fesetround(feSavedRound));
		return 0;	/* interval arithmetic was inconclusive */
	}

	double tmp;
	if (dcaMax < 0) { tmp = -dcaMax; dcaMax = -dcaMin; dcaMin = tmp; }
	if (dcbMax < 0) { tmp = -dcbMax; dcbMax = -dcbMin; dcbMin = tmp; }
	if (ddaMax < 0) { tmp = -ddaMax; ddaMax = -ddaMin; ddaMin = tmp; } 
   if (ddbMax < 0) { tmp = -ddbMax; ddbMax = -ddbMin; ddbMin = tmp; }

#if 0
	double tMax1 = (dcaMax + dcbMax) * ddaMax;  /* still rounding up */
	double tMax2 = (ddaMax + ddbMax) * dcaMax;
   if( fesetround(FE_DOWNWARD) )
      printf("\nERROR! fesetround returned error %i", fesetround(FE_DOWNWARD));

	double tMin1 = (dcaMin + dcbMin) * ddaMin;
	double tMin2 = (ddaMin + ddbMin) * dcaMin;
	double qMin = tMin2 - tMax1;
   if( fesetround(FE_UPWARD) )
      printf("\nERROR! fesetround returned error %i", fesetround(FE_UPWARD));

	double qMax = tMax2 - tMin1;
   if( fesetround(feSavedRound) )
      printf("\nERROR! fesetround returned error %i", fesetround(feSavedRound));

	if ((qMin < 0) != (qMax < 0)) return 0;
	if (qMin == 0) return 0;
	if (qMax == 0) return 0;
	*answer = qMin;
#else
	double tMax1 = dcbMax * ddaMax;  /* still rounding up */
	double tMax2 = dcaMax * ddbMax;
   if( fesetround(FE_DOWNWARD) )
      printf("\nERROR! fesetround returned error %i", fesetround(FE_DOWNWARD));

	double tMin1 = dcbMin * ddaMin;  
	double tMin2 = dcaMin * ddbMin;
	double qMin = tMin2 - tMax1;
   if( fesetround(FE_UPWARD) )
      printf("\nERROR! fesetround returned error %i", fesetround(FE_UPWARD));

	double qMax = tMax2 - tMin1;
   if( fesetround(feSavedRound) )
      printf("\nERROR! fesetround returned error %i", fesetround(feSavedRound));

	if ((qMin < 0) != (qMax < 0)) return 0;
	if (qMin == 0) return 0;
	if (qMax == 0) return 0;
	*answer = qMin;
#endif
	return 1;
}
/*
 * AJL's code for multiplying to exactFloats (expansions in Shewchuck lingo)
 * the scaleExpansion and addExacts have been replaced with Shewchuck's
 */
void multiplyExacts(exactFloat *a, exactFloat *b, exactFloat *c, int *err) {
/* Might be faster if a distillation tree was used. */
	exactFloat tmpSum, work, *ca, *cb, *tmp;
	int aEnd = a->end;
	if (aEnd == -1 | b->end == -1) {
		c->end = -1;
	} else {
      /*
       * There exist two bins for storing sums, c and tmpSum
       * We scale_expansion the entire b expansion
       * for each double in a->doubles. ca and cb point to 
       * tmpSum and c. If we will sum an odd number of times, 
       * we should begin by pointing ca at c (so the answer ends
       * up there)
       * 
       * However, Shewchuck's predicates allow you to do 
       * in place adding and scaling. Perhaps we can clean this
       * function up, and make it more readable.
       */
		if (aEnd & 1) {
			ca = c;
			cb = &tmpSum;
		} else {
			ca = &tmpSum;
			cb = c;
		}
      ca->end = scale_expansion_zeroelim( b->end, b->doubles, a->doubles[0], ca->doubles);
      int i;
		for (i=1; i<aEnd; i++) {
         work.end = scale_expansion_zeroelim( b->end, b->doubles, a->doubles[i], work.doubles);
         cb->end = fast_expansion_sum_zeroelim(ca->end, ca->doubles, work.end, work.doubles, cb->doubles);
         //cb->end = expansion_sum_zeroelim1(ca->end, ca->doubles, work.end, work.doubles, cb->doubles);
			tmp = ca;
			ca = cb;
			cb = tmp;
		}
	}
}

/*
 * !Remember! AJL's a->end is off by one
 */
void negate(exactFloat *a) {
	if (a->end <= 0) 
      return;
	for (int i=0; i<a->end; i++) {
      a->doubles[i] = -a->doubles[i];
   }
}

/*
 * copy of AJL's subtraction routine
 * computes e = a-b;
 */
void subtractExacts(exactFloat *a, exactFloat *b, exactFloat *e, int *err) {
   exactFloat negative;
   negative = (*b);
   negate(&negative); // This will not increase the size of the expansion, whereas scale_expansion will double it
   e->end = fast_expansion_sum_zeroelim(negative.end, negative.doubles, a->end, a->doubles, e->doubles);
   //e->end = expansion_sum_zeroelim1(negative.end, negative.doubles, a->end, a->doubles, e->doubles);
   //e->end = expansion_sum(negative.end, negative.doubles, a->end, a->doubles, e->doubles);
}

/*
 * copy of AJL's negative test routine
 * returns true if a is negative
 * !Remember!: AJL's end is off by one
 */
int isNegative(exactFloat *a) {
	if (a->end <= 0) 
      return 0;
   
   return a->doubles[a->end-1] < 0;
}

int exactPerpendicularDistances(
   double c0[3], double c1[3], double c2[3], double a[3], exactFloat *da, double b[3], exactFloat *db, int *err
) {
	/* Compute the 2 edge vectors of the triangle: c1-c0, c2-c0 */
	exactFloat e1x, e1y, e1z, e2x, e2y, e2z;
   // Shewchuck's macro's require these (see predicatesC.h:65)
   INEXACT REAL bvirt;
   REAL bround, around, avirt;
   
   Two_Diff(c1[0],c0[0],e1x.doubles[1],e1x.doubles[0]);
   e1x.end=2;
   Two_Diff(c1[1],c0[1],e1y.doubles[1],e1y.doubles[0]);
   e1y.end=2;
   Two_Diff(c1[2],c0[2],e1z.doubles[1],e1z.doubles[0]);
   e1z.end=2;
   // ---
   Two_Diff(c2[0],c0[0],e2x.doubles[1],e2x.doubles[0]);
   e2x.end=2;
   Two_Diff(c2[1],c0[1],e2y.doubles[1],e2y.doubles[0]);
   e2y.end=2;
   Two_Diff(c2[2],c0[2],e2z.doubles[1],e2z.doubles[0]);
   e2z.end=2;

	/* Compute each component e of the triangle normal in turn,
	 * and the dot product of the normal with c0-a, c0-b */
	exactFloat e, p1, p2, d, da1, da2, db1, db2;
   exactFloat negative;
	// ex = (e1y * e2z) - (e1z * e2y)
	multiplyExacts(&e1y, &e2z, &p1, err);
	multiplyExacts(&e1z, &e2y, &p2, err);
	subtractExacts(&p1, &p2, &e, err);
   Two_Diff(c0[0], a[0], d.doubles[1], d.doubles[0]);
   d.end = 2;
	multiplyExacts(&d, &e, &da1, err);
   Two_Diff(c0[0], b[0], d.doubles[1], d.doubles[0]);
   d.end = 2;
	multiplyExacts(&d, &e, &db1, err);

	// ey = (e1z * e2x) - (e1x * e2z)
	multiplyExacts(&e1z, &e2x, &p1, err);
	multiplyExacts(&e1x, &e2z, &p2, err);
	subtractExacts(&p1, &p2, &e, err);
   Two_Diff(c0[1], a[1], d.doubles[1], d.doubles[0]);
   d.end = 2;
	multiplyExacts(&d, &e, &da2, err);
   da->end = fast_expansion_sum_zeroelim( da1.end, da1.doubles, da2.end, da2.doubles, da->doubles);
   //da->end = expansion_sum_zeroelim1( da1.end, da1.doubles, da2.end, da2.doubles, da->doubles);
   Two_Diff(c0[1], b[1], d.doubles[1], d.doubles[0]);
   d.end = 2;
	multiplyExacts(&d, &e, &db2, err);
   db->end = fast_expansion_sum_zeroelim(db1.end, db1.doubles, db2.end, db2.doubles, db->doubles);
   //db->end = expansion_sum_zeroelim1(db1.end, db1.doubles, db2.end, db2.doubles, db->doubles);

	// ez = (e1x * e2y) - (e1y * e2x)
	multiplyExacts(&e1x, &e2y, &p1, err);
	multiplyExacts(&e1y, &e2x, &p2, err);
	subtractExacts(&p1, &p2, &e, err);
   Two_Diff(c0[2], a[2], d.doubles[1], d.doubles[0]);
   d.end = 2;
	multiplyExacts(&d, &e, &da2, err);
   da1.end = fast_expansion_sum_zeroelim( da2.end, da2.doubles, da->end, da->doubles, da1.doubles);
   //da1.end = expansion_sum_zeroelim1( da2.end, da2.doubles, da->end, da->doubles, da1.doubles);
   Two_Diff(c0[2], b[2], d.doubles[1], d.doubles[0]);
   d.end = 2;
	multiplyExacts(&d, &e, &db2, err);
   db1.end = fast_expansion_sum_zeroelim( db2.end, db2.doubles, db->end, db->doubles, db1.doubles);
   //db1.end = expansion_sum_zeroelim1( db2.end, db2.doubles, db->end, db->doubles, db1.doubles);

   da->end = compress_expansion(da1.end, da1.doubles, da->doubles);
   if (isNegative(da)) {
      negate(da);
   }

   db->end = compress_expansion(db1.end, db1.doubles, db->doubles);
   if (isNegative(db)) {
      negate(db);
   }
   return 0; // return value not used
}

/**
 * Computes the segment intersection parameter
 *
 *       |v0]
 * s = ---------
 *     |v0|+|v1|
 *
 * where v0 and v1 are both high resolution numbers stored as two
 * doubles, with the high resolution component stored at index 0
 * (which is the opposite of exactFloat and the Shewchuk numbers).
 */
double computeSegmentScale (double *v0, double *v1) {
   INEXACT REAL avirt, bround, around, bvirt;
   int err;
   exactFloat vol0, vol1, volt;
   exactFloat scale; 
   exactFloat tmp; 
   exactFloat remainder;
   exactFloat quot;
   tmp.doubles[1] = v0[0];
   tmp.doubles[0] = v0[1];
   vol0.end = compress_expansion (2, tmp.doubles, vol0.doubles);
   tmp.doubles[1] = v1[0];
   tmp.doubles[0] = v1[1];
   vol1.end = compress_expansion (2, tmp.doubles, vol1.doubles);
   if (isNegative (&vol0)) {
      subtractExacts (&vol1, &vol0, &volt, &err);
   }
   else {
      subtractExacts (&vol0, &vol1, &volt, &err);
   }
   double voltHigh = volt.doubles[volt.end-1];
   double q0 = v0[1]/voltHigh;
   scale.doubles[0] = q0;
   scale.end = 1;
   multiplyExacts (&scale, &volt, &tmp, &err);
   subtractExacts (&vol0, &tmp, &remainder, &err);
   double q1 = estimate_expansion (remainder.end, remainder.doubles)/voltHigh;
   Two_Sum (q0, q1, quot.doubles[1], quot.doubles[0]);
   quot.end = 2;
   double s = estimate_expansion (quot.end, quot.doubles);
   if (isNegative (&vol0)) {
      return -s;
   }
   else {
      return s;
   }
}

/* Returns 1 if *answer contains sign-accurate estimate of ((dda + ddb) * dca) - ((dca + dcb) * dda)
 * which is negative if int(ab, c) is closer to a than int(ab, d) 
 * Returns 0 if an error was encountered.  */

int exactClosestIntersection(
	double a[3], double b[3],
	double c0[3], double c1[3], double c2[3], 
	double d0[3], double d1[3], double d2[3],
	double *answer
) {
	exactFloat dca, dcb, dda, ddb;
	int err = 0;
	exactPerpendicularDistances(c0, c1, c2, a, &dca, b, &dcb, &err);
	exactPerpendicularDistances(d0, d1, d2, a, &dda, b, &ddb, &err);

	exactFloat p, q, r, p1, p2;
#if 0
   // q = (dda + ddb) * dca
   p.end = fast_expansion_sum_zeroelim(dda.end, dda.doubles, ddb.end, ddb.doubles, p.doubles);
   //p.end = expansion_sum_zeroelim1(dda.end, dda.doubles, ddb.end, ddb.doubles, p.doubles);
	multiplyExacts( &p, &dca, &q, &err );

   // r = (dca + dcb) * dda
   p.end = fast_expansion_sum_zeroelim(dca.end, dca.doubles, dcb.end, dcb.doubles, p.doubles);
   //p.end = expansion_sum_zeroelim1(dca.end, dca.doubles, dcb.end, dcb.doubles, p.doubles);
	multiplyExacts( &p, &dda, &r, &err );

   // p = q - r
	subtractExacts(&q, &r, &p, &err); 

	*answer = estimate_expansion(p.end,p.doubles);
#else
   multiplyExacts( &dca, &ddb, &p1, &err );
   multiplyExacts( &dcb, &dda, &p2, &err );

   subtractExacts( &p1, &p2, &p, &err );
   (*answer) = estimate_expansion( p.end, p.doubles );
#endif

	return err == 0;
}

