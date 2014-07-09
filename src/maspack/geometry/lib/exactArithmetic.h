/* The following are straightforward implementations of algorithms from
 * Jonathan Richard Shewchuk, Adaptive Precision Floating-Point Arithmetic
 * and Fast Robust Geometric Predicates, Discrete & Computational Geometry 18:305-363, 1997. 
 *
 * These are the primitive operations on pairs of double-precision floating point values
 * used by exact.c.
 * 
 * Use of XMM registers enforces double-precision rounding with every operation,
 * as required.
 *
 * XMM control register must be set to round-to-even while using these operations via the macros
 * XMMSetup and XMMRestore.  Underflow and overflow exceptions are unmasked as they
 * will invalidate the result.  */

#ifndef INCL_EXACTARITHMETIC
#define INCL_EXACTARITHMETIC

#define XMM_PRECISIONMASK 4096
#define XMM_ROUNDNEAREST 0

#define XMMSetup \
	unsigned int volatile xcwSave; \
	__asm volatile ("stmxcsr %0" :"=m" (xcwSave): ); \
	unsigned int volatile xcw = XMM_PRECISIONMASK + XMM_ROUNDNEAREST; \
	__asm volatile ("ldmxcsr %0" ::"m" (xcw) );

#define XMMRestore \
	__asm volatile ("ldmxcsr %0" ::"m" (xcwSave) );



#define fastTwoSum(aArg, bArg, xArg, yArg) \
	__asm volatile ( \
		"movsd %2, %%xmm0\n\t" \
		"movsd %%xmm0, %%xmm2\n\t" \
		"movsd %3, %%xmm1\n\t" \
		"addsd %%xmm1, %%xmm2\n\t" \
		"movsd %%xmm2, %0\n\t" \
		"subsd %%xmm0, %%xmm2\n\t" \
		"subsd %%xmm2, %%xmm1\n\t" \
		"movsd %%xmm1, %1\n\t" \
		: "=m" (xArg), "=m" (yArg) \
		: "m" (aArg), "m" (bArg) \
		: "%xmm0", "%xmm1", "%xmm2" \
	);

/*
a equ <xmm0>
av equ <xmm1>
b equ <xmm2>
bv equ <xmm3>
x equ <xmm4>

x = a + b
bv = x - a
av = x - bv
br = b - bv
ar = a - av
y = ar + br  */

#define twoSum(aArg, bArg, xArg, yArg) \
	__asm volatile ( \
		"movsd %2, %%xmm0\n\t" \
		"movsd %%xmm0, %%xmm4\n\t" \
		"movsd %3, %%xmm2\n\t" \
		"addsd %%xmm2, %%xmm4\n\t" \
		"movsd %%xmm4, %0\n\t" \
		"movsd %%xmm4, %%xmm3\n\t" \
		"subsd %%xmm0, %%xmm3\n\t" \
		"movsd %%xmm4, %%xmm1\n\t" \
		"subsd %%xmm3, %%xmm1\n\t" \
		"subsd %%xmm3, %%xmm2\n\t" \
		"subsd %%xmm1, %%xmm0\n\t" \
		"addsd %%xmm2, %%xmm0\n\t" \
		"movsd %%xmm0, %1" \
		: "=m" (xArg), "=m" (yArg) \
		: "m" (aArg), "m" (bArg) \
		: "%xmm0", "%xmm1", "%xmm2", "%xmm3", "%xmm4" \
	);

/*
a equ <xmm0>
av equ <xmm1>
b equ <xmm2>
bv equ <xmm3>
x equ <xmm4>

x = a - b
bv = a - x
av = x + bv
br = bv - b
ar = a - av
y = ar + br */

#define twoDiff(aArg, bArg, xArg, yArg) \
	__asm volatile ( \
		"movsd %2, %%xmm0\n" \
		"movsd %%xmm0, %%xmm4\n" \
		"movsd %3, %%xmm2\n" \
		"subsd %%xmm2, %%xmm4\n" \
		"movsd %%xmm4, %0\n" \
		"movsd %%xmm0, %%xmm3\n" \
		"subsd %%xmm4, %%xmm3\n" \
		"movsd %%xmm4, %%xmm1\n" \
		"addsd %%xmm3, %%xmm1\n" \
		"subsd %%xmm2, %%xmm3\n" \
		"subsd %%xmm1, %%xmm0\n" \
		"addsd %%xmm3, %%xmm0\n" \
		"movsd %%xmm0, %1\n" \
		: "=m" (xArg), "=m" (yArg) \
		: "m" (aArg), "m" (bArg) \
		: "%xmm0", "%xmm1", "%xmm2", "%xmm3", "%xmm4" \
	);

/*
$134217729xReg equ <xmm0>
splitter equ <xmm1>
aHi equ <xmm2>
aLo equ <xmm3>
bHi equ <xmm4>
bLo equ <xmm5>
work equ <xmm6>

split:
aBig = c = a * b
a = c
aBig = c - a
a = aHi = c - aBig
b = aLo = a - aHi  */

// splitter = 134217729 = 2^27 + 1, where 27 = 53/2 rounded up
#ifdef AMD_64
#define twoProduct(aArg, bArg, xArg, yArg) \
	__asm volatile( \
	"movq $134217729, %%rax\n" \
		"cvtsi2sd %%rax, %%xmm1\n" \
		"movsd %2, %%xmm2\n" \
		"movsd %%xmm2, %%xmm0\n" \
		"movsd %3, %%xmm4\n" \
		"movsd %%xmm2, %%xmm3\n" \
		"movsd %%xmm2, %%xmm6\n" \
		"mulsd %%xmm1, %%xmm6\n" \
		"movsd %%xmm6, %%xmm2\n" \
		"subsd %%xmm3, %%xmm6\n" \
		"subsd %%xmm6, %%xmm2\n" \
		"subsd %%xmm2, %%xmm3\n" \
		"mulsd %%xmm4, %%xmm0\n" \
		"movsd %%xmm4, %%xmm5\n" \
		"movsd %%xmm4, %%xmm6\n" \
		"mulsd %%xmm1, %%xmm6\n" \
		"movsd %%xmm6, %%xmm4\n" \
		"subsd %%xmm5, %%xmm6\n" \
		"subsd %%xmm6, %%xmm4\n" \
		"subsd %%xmm4, %%xmm5\n" \
		"movsd %%xmm0, %0\n" \
		"movsd %%xmm2, %%xmm6\n" \
		"mulsd %%xmm4, %%xmm6\n" \
		"subsd %%xmm6, %%xmm0\n" \
		"movsd %%xmm3, %%xmm6\n" \
		"mulsd %%xmm4, %%xmm6\n" \
		"subsd %%xmm6, %%xmm0\n" \
		"mulsd %%xmm5, %%xmm2\n" \
		"subsd %%xmm2, %%xmm0\n" \
		"subsd %%xmm6, %%xmm6\n" \
		"mulsd %%xmm5, %%xmm3\n" \
		"subsd %%xmm3, %%xmm0\n" \
		"subsd %%xmm0, %%xmm6\n" \
		"movsd %%xmm6, %1\n" \
		: "=m" (xArg), "=m" (yArg) \
		: "m" (aArg), "m" (bArg) \
		: "%rax", "%xmm0", "%xmm1", "%xmm2", "%xmm3", "%xmm4", "%xmm5", "%xmm6" \
	);
#else
#define twoProduct(aArg, bArg, xArg, yArg) \
	__asm volatile( \
		"push $134217729\n" \
		"cvtsi2sd (%%esp), %%xmm1\n" \
		"add $4, %%esp\n" \
		"movsd %2, %%xmm2\n" \
		"movsd %%xmm2, %%xmm0\n" \
		"movsd %3, %%xmm4\n" \
		"movsd %%xmm2, %%xmm3\n" \
		"movsd %%xmm2, %%xmm6\n" \
		"mulsd %%xmm1, %%xmm6\n" \
		"movsd %%xmm6, %%xmm2\n" \
		"subsd %%xmm3, %%xmm6\n" \
		"subsd %%xmm6, %%xmm2\n" \
		"subsd %%xmm2, %%xmm3\n" \
		"mulsd %%xmm4, %%xmm0\n" \
		"movsd %%xmm4, %%xmm5\n" \
		"movsd %%xmm4, %%xmm6\n" \
		"mulsd %%xmm1, %%xmm6\n" \
		"movsd %%xmm6, %%xmm4\n" \
		"subsd %%xmm5, %%xmm6\n" \
		"subsd %%xmm6, %%xmm4\n" \
		"subsd %%xmm4, %%xmm5\n" \
		"movsd %%xmm0, %0\n" \
		"movsd %%xmm2, %%xmm6\n" \
		"mulsd %%xmm4, %%xmm6\n" \
		"subsd %%xmm6, %%xmm0\n" \
		"movsd %%xmm3, %%xmm6\n" \
		"mulsd %%xmm4, %%xmm6\n" \
		"subsd %%xmm6, %%xmm0\n" \
		"mulsd %%xmm5, %%xmm2\n" \
		"subsd %%xmm2, %%xmm0\n" \
		"subsd %%xmm6, %%xmm6\n" \
		"mulsd %%xmm5, %%xmm3\n" \
		"subsd %%xmm3, %%xmm0\n" \
		"subsd %%xmm0, %%xmm6\n" \
		"movsd %%xmm6, %1\n" \
		: "=m" (xArg), "=m" (yArg) \
		: "m" (aArg), "m" (bArg) \
		: "%xmm0", "%xmm1", "%xmm2", "%xmm3", "%xmm4", "%xmm5", "%xmm6" \
	);
#endif

#endif

