#ifndef _FLOATSTATUS_H_
#define _FLOATSTATUS_H_
#define FloatSaveStatus \
	unsigned short int cw, cwSave; \
	__asm("fstcw %0" :"=m" (cwSave): );\
   unsigned int mxcwSave, mxcw;\
   __asm("stmxcsr %0" :"=m" (mxcwSave): );

#ifndef FE_PRECISIONEXTDOUBLE 
#define FE_PRECISIONEXTDOUBLE 0x300
#endif
#ifndef FE_DENORMAL
#define FE_DENORMAL	0x02
#endif
#ifndef FE_INEXACT
#define FE_INEXACT	0x20
#endif
#ifndef FE_UNDERFLOW
#define FE_UNDERFLOW	0x10
#endif
#ifndef FE_DOWNWARD
#define FE_DOWNWARD	0x0400
#endif
#ifndef FE_UPWARD
#define FE_UPWARD	0x0800
#endif

#define FloatRoundDown \
	cw = FE_INEXACT + FE_DENORMAL + FE_UNDERFLOW + FE_PRECISIONEXTDOUBLE + FE_DOWNWARD; \
   cw = cw + FE_OVERFLOW + FE_DIVBYZERO + FE_INVALID; \
	__asm("fldcw %0" :: "m" (cw)); \
   /*__asm("stmxcsr %0" : "=m" (mxcw):); */ \
   /*mxcw = mxcw & 0x00009fff; */ \
   /* mxcw = 0x2000 | mxcw; */ \
   mxcw = 0x3100; \
   __asm("ldmxcsr %0" :: "m"(mxcw));

#define FloatRoundUp \
	cw = FE_INEXACT + FE_DENORMAL + FE_UNDERFLOW + FE_PRECISIONEXTDOUBLE + FE_UPWARD; \
   cw = cw + FE_OVERFLOW + FE_DIVBYZERO + FE_INVALID; \
	__asm("fldcw %0" :: "m" (cw)); \
   /*__asm("stmxcsr %0" : "=m" (mxcw):); */ \
   /*mxcw = mxcw & 0x00009fff; */ \
   /* mxcw = 0x2000 | mxcw; */ \
   mxcw = 0x5100; \
   __asm("ldmxcsr %0" :: "m"(mxcw));

#define FloatRestoreStatus \
	__asm("fldcw %0" :: "m" (cwSave)); \
   __asm("ldmxcsr %0" :: "m" (mxcwSave));

#endif // #ifndef _FLOATSTATUS_H_
