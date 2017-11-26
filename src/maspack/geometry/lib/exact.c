#include <stdio.h>
#include "exact.h"

void copyExact(exactFloat *a, exactFloat *b) {
	int i;
	b->end = a->end;
	for (i=0; i<=a->end; i++) b->doubles[i] = a->doubles[i];
}

void compressExact(exactFloat *a, exactFloat *h) {
	int i;
	h->end = -1;
	int b = a->end;
	if (b == -1) return;
	double aQ = a->doubles[b];
	double aq;
	double g[MaxDoubles];
	for (i=b-1; i>= 0; i--) {
		fastTwoSum(aQ, a->doubles[i], aQ, aq);
		if (aq != 0) {
			g[b--] = aQ;
			aQ = aq;
		}
	}
	g[b] = aQ;
	for (i=b+1; i<=a->end; i++) {
		fastTwoSum(g[i], aQ, aQ, aq);
		if (aq != 0) h->doubles[++h->end] = aq;
	}
	h->doubles[++h->end] = aQ;
}

double estimateExact(exactFloat *a) {
	exactFloat b;
	compressExact(a, &b);
	if (b.end == -1) return 0;
	return b.doubles[b.end];
}

void printExact(exactFloat *e) {
	int i;
	FILE *fout;
	fout = fopen("geoPred.txt", "a+");
	fprintf(fout, "est=%.18e end=%d doubles=", estimateExact(e), e->end);
	for (i=0; i<=e->end; i++) fprintf(fout, " %.18e", e->doubles[i]);
	fprintf(fout, "\n");
	fclose(fout);
}

int isNegative(exactFloat *a) {
	if (a->end == -1) return 0;
	return a->doubles[a->end] < 0;
}

void negate(exactFloat *a) {
	int i;
	for (i=0; i<=a->end; i++) a->doubles[i] = -a->doubles[i];
}


void growExpansion(exactFloat *e, double *b, exactFloat *c, int *err) {
	double q, hi;
	if (e->end == -1) {
		c->end = 0;
		c->doubles[0] = *b;
	} else {
		c->end = -1;
		twoSum(e->doubles[0], *b, q, hi);
		if (hi != 0) {
			ExactFloatAdd(c, hi)
		}
		for (int i=1; i<=e->end; i++) {
			twoSum(q, e->doubles[i], q, hi);
			if (hi != 0) {
				ExactFloatAdd(c, hi)
			}
		}
		if (q != 0) {
			ExactFloatAdd(c, q)
		}
	}
}

void scaleExpansion(exactFloat *e, double *b, exactFloat *c, int *err) {
	double q, h, bT, bt;
	c->end = -1;
	if (e->end==-1) return;
	/*printf ("%lx\n", e->doubles);*/
	twoProduct(e->doubles[0], *b, q, h);
	if (h != 0) {
		ExactFloatAdd(c, h)
	}
	for (int i=1; i<=e->end; i++) {
		twoProduct(e->doubles[i], *b, bT, bt);
		twoSum(q, bt, q, h);
		if (h != 0) {
			ExactFloatAdd(c, h)
		}
		fastTwoSum(bT, q, q, h);
		if (h != 0) {
			ExactFloatAdd(c, h)
		}
	}
	if (q != 0) {
		ExactFloatAdd(c, q)
	}
}

void addDoubles(double *a, double *b, exactFloat *e) {
	twoSum(*a, *b, e->doubles[1], e->doubles[0]);
	e->end = 1;
}

void subtractDoubles(double *a, double *b, exactFloat *e) {
	twoDiff(*a, *b, e->doubles[1], e->doubles[0]);
	e->end = 1;
}

void multiplyDoubles(double *a, double *b, exactFloat *e) {
	twoProduct(*a, *b, e->doubles[1], e->doubles[0]);
	e->end = 1;
}

void addExacts(exactFloat *a, exactFloat *b, exactFloat *c, int *err) {
	if (b->end == -1) {
		copyExact(a, c);
	} else {
		if (a->end == -1) {
			copyExact(b, c);
		} else {
			exactFloat work, *ca, *cb, *tmp;
			if (b->end & 1) {
				ca = &work;
				cb = c;
			} else {
				ca = c;
				cb = &work;
			}
			growExpansion(a, &(b->doubles[0]), ca, err);
			for (int i=1; i<=b->end; i++) {
				growExpansion(ca, &(b->doubles[i]), cb, err);
				tmp = ca;
				ca = cb;
				cb = tmp;
			}
		}
	}
}

void subtractExacts(exactFloat *a, exactFloat *b, exactFloat *c, int *err) {
/* Improvement candidate. */
	exactFloat work;
	double m1 = -1;
	scaleExpansion(b, &m1, &work, err);
	addExacts(a, &work, c, err);
}

void multiplyExacts(exactFloat *a, exactFloat *b, exactFloat *c, int *err) {
/* Might be faster if a distillation tree was used. */
	exactFloat tmpSum, work, *ca, *cb, *tmp;
	int aEnd = a->end;
	if (aEnd == -1 | b->end == -1) {
		c->end = -1;
	} else {
		if (aEnd & 1) {
			ca = &tmpSum;
			cb = c;
		} else {
			ca = c;
			cb = &tmpSum;
		}
		scaleExpansion(b, &(a->doubles[0]), ca, err);
		for (int i=1; i<=aEnd; i++) {
			scaleExpansion(b, &(a->doubles[i]), &work, err);
			addExacts(ca, &work, cb, err);
			tmp = ca;
			ca = cb;
			cb = tmp;
		}
	}
}
