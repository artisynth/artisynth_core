#include <math.h>
#include <fenv.h>
#include <stdio.h>
#include "geoPred.h"
#include "exact.h"
#include "floatStatus.h"

/*
 * dummy implementation to match shewchuck's init
 */
double exactinit() {
   return 0;
}

/* Answer the determinant
 * | p0x p0y p0z 1 |
 * | p1x p1y p1z 1 |
 * | p2x p2y p2z 1 |
 * | p3x p3y p3z 1 |
 * Calculate using a sum of three-term products of the original coordinates,
 * where the final multiplication in each product is by a positive number,
 * and the terms are summed using only + (not -).
 * This ensures that all rounding occurs in the same direction
 * when rounding to - or + infinity is used. */

double intervalOrient3d(double p0[3], double p1[3], double p2[3], double p3[3]) {
/*
FILE *fout;
fout = fopen("geoPred.txt", "a+");
fprintf(fout, "p0=%.18e %.18e %.18e\n", p0[0], p0[1], p0[2]);
fprintf(fout, "p1=%.18e %.18e %.18e\n", p1[0], p1[1], p1[2]);
fprintf(fout, "p2=%.18e %.18e %.18e\n", p2[0], p2[1], p2[2]);
fprintf(fout, "p3=%.18e %.18e %.18e\n", p3[0], p3[1], p3[2]);
fclose(fout);
*/
	double r=0;
	volatile double p01n = -p0[1];
	volatile double p11n = -p1[1];
	volatile double p21n = -p2[1];
	volatile double p31n = -p3[1];
	if (p0[0] < 0) {
		volatile double p00n = -p0[0];
		r += p00n * (
			(p11n * p2[2]) + (p21n * p3[2]) + (p31n * p1[2])
			 + (p1[1] * p3[2]) + (p2[1] * p1[2]) + (p3[1] * p2[2])
		);
	} else {
		r += p0[0] * (
			(p1[1] * p2[2]) + (p2[1] * p3[2]) + (p3[1] * p1[2])
			 + (p11n * p3[2]) + (p21n * p1[2]) + (p31n * p2[2])
		);
	}

	if (p1[0] < 0) {
		volatile double p10n = -p1[0];
		r += p10n * (
			(p01n * p3[2]) + (p21n * p0[2]) + (p31n * p2[2])
			+ (p0[1] * p2[2]) + (p2[1] * p3[2]) + (p3[1] * p0[2])
		);
	} else {
		r += p1[0] * (
			(p0[1] * p3[2]) + (p2[1] * p0[2]) + (p3[1] * p2[2])
			+ (p01n * p2[2]) + (p21n * p3[2]) + (p31n * p0[2])
		);
	}

	if (p2[0] < 0) {
		volatile double p20n = -p2[0];
		r += p20n * (
			(p01n * p1[2]) + (p11n * p3[2]) + (p31n * p0[2])
			+ (p0[1] * p3[2]) + (p1[1] * p0[2]) + (p3[1] * p1[2])
		);
	} else {
		r += p2[0] * (
			(p0[1] * p1[2]) + (p1[1] * p3[2]) + (p3[1] * p0[2])
			+ (p01n * p3[2]) + (p11n * p0[2]) + (p31n * p1[2])
		);
	}

	if (p3[0] < 0) {
		volatile double p30n = -p3[0];
		r += p30n * (
			(p01n * p2[2]) + (p11n * p0[2]) + (p21n * p1[2])
			+ (p0[1] * p1[2]) + (p1[1] * p2[2]) + (p2[1] * p0[2])
		);
	} else {
		r += p3[0] * (
			(p0[1] * p2[2]) + (p1[1] * p0[2]) + (p2[1] * p1[2])
			+ (p01n * p1[2]) + (p11n * p2[2]) + (p21n * p0[2])
		);
	}
	return r;
}

/* Calculate determinant of 
 * [ p0x-p3x, p0y-p3y p0z-p3z
 * p1x-p3x, p1y-p3y p1z-p3z
 * p2x-p3x, p2y-p3y p2z-p3z ] */

double exactOrient3d(double p0[3], double p1[3], double p2[3], double p3[3], int *err) {
	exactFloat a, b, c, m21, m22, m23, m31, m32, m33, r, r1;
	double w0 = -p3[0];		// m1 = p0 - p3
	addDoubles(p1, &w0, &m21);	// m2 = p1 - p3
	addDoubles(p2, &w0, &m31);	// m3 = p2 - p3
	double w1 = -p3[1];
	addDoubles(p1+1, &w1, &m22);
	addDoubles(p2+1, &w1, &m32);
	double w2 = -p3[2];
	addDoubles(p1+2, &w2, &m23);
	addDoubles(p2+2, &w2, &m33);

	multiplyExacts(&m22, &m33, &a, err);	// (m11 * ((m22 * m33) - (m23 * m32)))
	multiplyExacts(&m23, &m32, &b, err);
	subtractExacts(&a, &b, &c, err);
	addDoubles(p0, &w0, &a);
	multiplyExacts(&a, &c, &r, err);

	multiplyExacts(&m23, &m31, &a, err);	// + (m12 * ((m23 * m31) - (m21 * m33)))
	multiplyExacts(&m21, &m33, &b, err);
	subtractExacts(&a, &b, &c, err);
	addDoubles(p0+1, &w1, &a);
	multiplyExacts(&a, &c, &b, err);
	addExacts(&b, &r, &r1, err);

	multiplyExacts(&m21, &m32, &a, err);	// + (m13 * ((m21 * m32) - (m22 * m31)))
	multiplyExacts(&m22, &m31, &b, err);
	subtractExacts(&a, &b, &c, err);
	addDoubles(p0+2, &w2, &a);
	multiplyExacts(&a, &c, &b, err);
	addExacts(&b, &r1, &r, err);

	double est = estimateExact(&r);
	return est;
}

/* Calculate determinant
 * [a0 a1 1
 *  b0 b1 1
 *  c0 c1 1 ] */
double exactOrient2d(double *a0, double *a1, double *b0, double *b1, double *c0, double *c1, int *err) {
	exactFloat q, r, s, t;
	subtractDoubles(b1, c1, &q);
	scaleExpansion(&q, a0, &r, err);
	subtractDoubles(c0, b0, &q);
	scaleExpansion(&q, a1, &s, err);
	addExacts(&r, &s, &t, err);
	multiplyDoubles(b0, c1, &q);
	addExacts(&q, &t, &r, err);
	double b1n = -*b1;
	multiplyDoubles(c0, &b1n, &q);
	addExacts(&r, &q, &s, err);
	return estimateExact(&s);
}

double exactOrient1d(double *a, double *b) {
	double x, y;
	twoDiff(*a, *b, x, y);
	return x;
}

/*
 * This is the fallback routine for cases where orient3d really returns 0
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
		tmp=c; c=b; b=tmp;
		tmpi=ci; ci=bi; bi=tmpi;
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

extern int nasaOrient3d(int i, double *p, int i1, double *p1, int i2, double *p2, int i3, double *p3, double *volume) {
   depthST dummy;
   return nasaOrient3d_d(
      i, p, i1, p1,
      i2, p2, i3, p3, 
      volume, &dummy);

}

extern int nasaOrient3d_d(int i, double *p, int i1, double *p1, int i2, double *p2, int i3, double *p3, double *volume, depthST *depth) {
   (*depth) = D_SHEWCHUK;
	int answer;
   FloatSaveStatus
	FloatRoundDown
	double ww1 = intervalOrient3d(p, p1, p2, p3);
	FloatRoundUp
	double ww2 = intervalOrient3d(p, p1, p2, p3);
	FloatRestoreStatus

	if ((ww1 < 0 & ww2 < 0) | (ww1 > 0 & ww2 > 0)) {
		*volume = (ww1 + ww2) * 0.5;
		answer = *volume > 0;
	} else {
		XMMSetup
		int err = 0;
/* FILE *fout; */
/* fout = fopen("geoPred.txt", "a+"); */
/* fprintf(fout, "exact\n"); */ /* fclose(fout); */
		double ww3 = exactOrient3d(p, p1, p2, p3, &err);
		if (err != 0) {
         printf("\nMaxDoubles not set high enough\n");
			answer = -1;	/* MaxDoubles not set high enough */
		} else {
			if (ww3 < ww1 | ww3 > ww2) {
				answer = -3;
			} else {
				*volume = ww3;
				if (*volume != 0) {
					answer = *volume > 0;
				} else {
/* fout = fopen("geoPred.txt", "a+"); */
/* fprintf(fout, "sos\n"); */
/* fclose(fout); */
               (*depth) = D_SOS;
					answer = sosOrient3d(i, p, i1, p1, i2, p2, i3, p3, &err);
				}
			}
		}
		XMMRestore
	}
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
	FloatSaveStatus
	FloatRoundDown
	double dcaMin = perpendicularDistance(a, c0, c1, c2);
	double dcbMin = perpendicularDistance(b, c0, c1, c2);
	double ddaMin = perpendicularDistance(a, d0, d1, d2);
	double ddbMin = perpendicularDistance(b, d0, d1, d2);
	FloatRoundUp
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
		FloatRestoreStatus
		return 0;	/* interval arithmetic was inconclusive */
	}

	double tmp;
	if (dcaMax < 0) { tmp = -dcaMax; dcaMax = -dcaMin; dcaMin = tmp; }
	if (dcbMax < 0) { tmp = -dcbMax; dcbMax = -dcbMin; dcbMin = tmp; }
	if (ddaMax < 0) { tmp = -ddaMax; ddaMax = -ddaMin; ddaMin = tmp; }
	if (ddbMax < 0) { tmp = -ddbMax; ddbMax = -ddbMin; ddbMin = tmp; }

	FloatRoundUp
	double tMax1 = (dcaMax + dcbMax) * ddaMax;  /* still rounding up */
	double tMax2 = (ddaMax + ddbMax) * dcaMax;
	FloatRoundDown
	double tMin1 = (dcaMin + dcbMin) * ddaMin;
	double tMin2 = (ddaMin + ddbMin) * dcaMin;
	double qMin = tMin2 - tMax1;
	FloatRoundUp
	double qMax = tMax2 - tMin1;
	FloatRestoreStatus

	if ((qMin < 0) != (qMax < 0)) return 0;
	if (qMin == 0) return 0;
	if (qMax == 0) return 0;
	*answer = qMin;
	return 1;
}

int exactPerpendicularDistances(
	double c0[3], double c1[3], double c2[3], double a[3], exactFloat *da, double b[3], exactFloat *db, int *err
) {
	/* Compute the 2 edge vectors of the triangle: c1-c0, c2-c0 */
	exactFloat e1x, e1y, e1z, e2x, e2y, e2z;
	subtractDoubles(c1, c0, &e1x);
	subtractDoubles(c1+1, c0+1, &e1y);
	subtractDoubles(c1+2, c0+2, &e1z);
	subtractDoubles(c2, c0, &e2x);
	subtractDoubles(c2+1, c0+1, &e2y);
	subtractDoubles(c2+2, c0+2, &e2z);

	/* Compute each component e of the triangle normal in turn,
	 * and the dot product of the normal with c0-a, c0-b */
	exactFloat e, p1, p2, d, da1, da2, db1, db2;
	// ex = (e1y * e2z) - (e1z * e2y)
	multiplyExacts(&e1y, &e2z, &p1, err);
	multiplyExacts(&e1z, &e2y, &p2, err);
	subtractExacts(&p1, &p2, &e, err);
	subtractDoubles(c0, a, &d);
	multiplyExacts(&d, &e, &da1, err);
	subtractDoubles(c0, b, &d);
	multiplyExacts(&d, &e, &db1, err);

	// ey = (e1z * e2x) - (e1x * e2z)
	multiplyExacts(&e1z, &e2x, &p1, err);
	multiplyExacts(&e1x, &e2z, &p2, err);
	subtractExacts(&p1, &p2, &e, err);
	subtractDoubles(c0+1, a+1, &d);
	multiplyExacts(&d, &e, &da2, err);
	addExacts(&da1, &da2, da, err);
	subtractDoubles(c0+1, b+1, &d);
	multiplyExacts(&d, &e, &db2, err);
	addExacts(&db1, &db2, db, err);

	// ez = (e1x * e2y) - (e1y * e2x)
	multiplyExacts(&e1x, &e2y, &p1, err);
	multiplyExacts(&e1y, &e2x, &p2, err);
	subtractExacts(&p1, &p2, &e, err);
	subtractDoubles(c0+2, a+2, &d);
	multiplyExacts(&d, &e, &da2, err);
	addExacts(&da2, da, &da1, err);
	subtractDoubles(c0+2, b+2, &d);
	multiplyExacts(&d, &e, &db2, err);
	addExacts(&db2, db, &db1, err);

	compressExact(&da1, da);
	if (isNegative(da)) negate(da);

	compressExact(&db1, db);
	if (isNegative(db)) negate(db);
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
   
	exactFloat p, q, r;
	addExacts(&dda, &ddb, &p, &err);
	multiplyExacts(&p, &dca, &q, &err);
	addExacts(&dca, &dcb, &p, &err);
	multiplyExacts(&p, &dda, &r, &err);
	subtractExacts(&q, &r, &p, &err);
	*answer = estimateExact(&p);
	return err == 0;
}

extern int geoPredTest() {
	double a=1;
	double b=1e-17;
	double c=2;
	exactFloat e, e1, e2, e3;
	subtractDoubles(&a, &b, &e);
	printExact(&e);
	int err=0;
	scaleExpansion(&e, &c, &e1, &err);
	printExact(&e1);
	subtractExacts(&e1, &e, &e2, &err);
	printExact(&e2);
	subtractExacts(&e2, &e, &e3, &err);
	printExact(&e3);
}

/*

FILE *fout;
fout = fopen("geoPred.txt", "a+");
fprintf(fout, "b1=%.18e c1=%.18e\n", b1, c1);
fclose(fout);
fout = fopen("geoPred.txt", "a+");
fprintf(fout, "c0=%.18e b0=%.18e\n", c0, b0);
fclose(fout);
FILE *fout;
fout = fopen("geoPred.txt", "a+");
fprintf(fout, "eo1d a=%.18e b=%.18e\n", *a, *b);
fclose(fout);

*/

