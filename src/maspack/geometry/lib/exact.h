// The maximum size of a compressed expansion is 2^11 / 53 = 39 doubles.
// More than that will result in a under/overflow.
#define MaxDoubles 100

#ifndef INCL_EXACT
#define INCL_EXACT

#include "exactArithmetic.h"

typedef struct {
	int end;
	double doubles[MaxDoubles];
} exactFloat;

#define ExactFloatAdd(anExact, aDouble) \
	if (++anExact->end >= MaxDoubles) { \
		*err = 1; \
	} else { \
		anExact->doubles[anExact->end] = aDouble; \
	} \

#define ExactFloatSet(anExact, aDouble) \
	anExact->end = 0; \
	anExact->doubles[0] = aDouble;

#define ExactFloatSetDot(anExact, aDouble) \
	anExact.end = 0; \
	anExact.doubles[0] = aDouble;

void copyExact(exactFloat *a, exactFloat *b);
void compressExact(exactFloat *a, exactFloat *h);
double estimateExact(exactFloat *a);
void printExact(exactFloat *e);
int isNegative(exactFloat *a);
void negate(exactFloat *a);
void growExpansion(exactFloat *e, double *b, exactFloat *c, int *err);
void scaleExpansion(exactFloat *e, double *b, exactFloat *c, int *err);
void addDoubles(double *a, double *b, exactFloat *e);
void subtractDoubles(double *a, double *b, exactFloat *e);
void multiplyDoubles(double *a, double *b, exactFloat *e);
void addExacts(exactFloat *a, exactFloat *b, exactFloat *c, int *err);
void subtractExacts(exactFloat *a, exactFloat *b, exactFloat *c, int *err);
void multiplyExacts(exactFloat *a, exactFloat *b, exactFloat *c, int *err);

#endif

