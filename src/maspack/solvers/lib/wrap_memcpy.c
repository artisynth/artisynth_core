/* This is intended to be used on Linux systems only. It provides a
   wrapper for memcpy that brings in an earlier version of memcpy from
   glibc, which would otherwise depend on GLIBC_2.14.

   The linker argument -wrap=memcpy will cause the linker to replace
   called to memcpy with wrap_memcpy.
*/
#include <string.h>

__asm__(".symver memcpy,memcpy@GLIBC_2.2.5");

void *__wrap_memcpy(void *dest, const void *src, size_t n)
{
   return memcpy(dest, src, n);
}
