/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

#ifndef _TRIANGLE_JNI_H_
#define _TRIANGLE_JNI_H_

#ifdef SINGLE
#define REAL float
#else /* not SINGLE */
#define REAL double
#endif /* not SINGLE */
#define VOID int

#include "triangle.h"

class TriangleTessellator {
  public:
   
   triangulateio *in;
   triangulateio *out;
   triangulateio *vorout;
 
   TriangleTessellator();
   ~TriangleTessellator();

   void buildFromPoints (
      double *coords, int numPnts, double minAngle);
   
   void buildFromSegments (double *coords, int numPnts, int *edgeIndices,
      int numEdges, double minAngle);
      
   void buildFromSegmentsAndPoints (
      double *coords, int numPnts, int *edgeIndices,
      int numEdges, double minAngle);
   
};


#endif
