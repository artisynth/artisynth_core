#ifndef _TETGEN_JNI_H_
#define _TETGEN_JNI_H_

#include "tetgen.h"

class TetgenTessellator {
  public:
   
   tetgenio *in;
   tetgenio *out;
   tetgenio *addin;
 
   TetgenTessellator();
   ~TetgenTessellator();

   void buildFromMesh (
      double *coords, int numPnts, int *indices,
      int numFaces, int numIndices, double quality);

   void buildFromMeshAndPoints (
		double *coords, int numPoints, int *faceIndices,
		int numFaces, int numFaceIndices, double quality,
		double *includeCoords, int numIncludePoints);

   void refineMesh(
      double *coords, int numNodes, int *tetIndices,
      int numTets, double quality,
      double *addCoords, int numAddPoints);

   void buildFromPoints (double *coords, int numPnts);

};


#endif
