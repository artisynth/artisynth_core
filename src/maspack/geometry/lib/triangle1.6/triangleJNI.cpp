/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

#include <stdlib.h>
#include <stdio.h>

#include "triangleJNI.h"
#include "maspack_geometry_TriangleTessellator.h"

void freeIO(triangulateio *io) {

   if (io->pointlist != NULL) {
      free(io->pointlist);
      io->pointlist = NULL;
   }
   io->numberofpoints = 0;
   if (io->pointattributelist != NULL) {
      free(io->pointattributelist);
      io->pointattributelist = NULL;
   }
   io->numberofpointattributes = 0;
   if (io->pointmarkerlist != NULL) {
      free(io->pointmarkerlist);
      io->pointmarkerlist = NULL;
   }
   if (io->trianglelist != NULL) {
      free(io->trianglelist);
      io->trianglelist = NULL;
   }
   io->numberoftriangles = 0;
   if (io->triangleattributelist != NULL) {
      free(io->triangleattributelist);
      io->triangleattributelist = NULL;
   }
   io->numberoftriangleattributes = 0;
   if (io->trianglearealist != NULL) {
      free(io->trianglearealist);
      io->trianglearealist = NULL;
   }
   io->numberofcorners = 0;
   if (io->neighborlist != NULL) {
      free(io->neighborlist);
      io->neighborlist = NULL;
   }

   if (io->segmentlist != NULL) {
      free(io->segmentlist);
      io->segmentlist = NULL;
   }
   io->numberofsegments = 0;
   if (io->segmentmarkerlist != NULL) {
      free(io->segmentmarkerlist);
      io->segmentmarkerlist = NULL;
   }

   if (io->holelist != NULL) {
      free(io->holelist);
      io->holelist = NULL;
   }
   io->numberofholes = 0;

   if (io->regionlist != NULL) {
      free(io->regionlist);
      io->regionlist = NULL;
   }
   io->numberofregions = 0;

   if (io->edgelist != NULL) {
      free(io->edgelist);
      io->edgelist = NULL;
   }
   io->numberofedges = 0;
   if (io->edgemarkerlist != NULL) {
      free(io->edgemarkerlist);
      io->edgemarkerlist = NULL;
   }
   if (io->normlist != NULL) {
      free(io->normlist);
      io->normlist = NULL;
   }
}

void initIO(triangulateio *io) {
   io->pointlist = NULL;
   io->numberofpoints = 0;
   io->pointattributelist = NULL;
   io->numberofpointattributes = 0;
   io->pointmarkerlist = NULL;

   io->trianglelist = NULL;
   io->numberoftriangles = 0;
   io->triangleattributelist = NULL;
   io->numberoftriangleattributes = 0;
   io->trianglearealist = NULL;
   io->numberofcorners = 0;
   io->neighborlist = NULL;

   io->segmentlist = NULL;
   io->numberofsegments = 0;
   io->segmentmarkerlist = NULL;

   io->holelist = NULL;
   io->numberofholes = 0;

   io->regionlist = NULL;
   io->numberofregions = 0;

   io->edgelist = NULL;
   io->numberofedges = 0;
   io->edgemarkerlist = NULL;
   io->normlist = NULL;
}

TriangleTessellator::TriangleTessellator() {
   in = new triangulateio();
   initIO(in);
   out = new triangulateio();
   initIO(out);
   vorout = NULL;
}

TriangleTessellator::~TriangleTessellator() {
   if (in != NULL) {
      freeIO(in);
      delete in;
      in = NULL;
   }
   if (out != NULL) {
      freeIO(out);
      delete out;
      out = NULL;
   }
   if (vorout != NULL) {
      freeIO(vorout);
      delete vorout;
      vorout = NULL;
   }
}

void TriangleTessellator::buildFromPoints(double *coords, int numPnts,
      double minAngle) {

   if (in != NULL) {
      freeIO(in);
   } else {
      in = new triangulateio();
      initIO(in);
   }

   if (vorout != NULL) {
      freeIO(vorout);
   } else {
      vorout = new triangulateio();
      initIO(vorout);
   }

   in->numberofpoints = numPnts;
   in->numberofpointattributes = 0;
   in->pointlist = (REAL *) malloc(numPnts * 2 * sizeof(REAL));
   for (int i = 0; i < numPnts * 2; i++) {
      in->pointlist[i] = coords[i];
   }
   in->pointattributelist = NULL;
   in->pointmarkerlist = NULL;
   in->numberofsegments = 0;
   in->numberofholes = 0;
   in->numberofregions = 0;
   in->regionlist = NULL;

   if (out != NULL) {
      freeIO(out);
   } else {
      out = new triangulateio();
      initIO(out);
   }

   char switches[50]; // max
   switches[0] = 0;

   if (minAngle > 0) {
      sprintf(switches, "%sq%4.2f", "zQYY", minAngle);
   } else {
      sprintf(switches, "%s", "zQYY");
   }

   triangulate(switches, in, out, vorout);

}

JNIEXPORT jint JNICALL Java_maspack_geometry_TriangleTessellator_doBuildFromPoints(
      JNIEnv *env, jobject obj, jlong handle, jdoubleArray pntCoords,
      jint numPnts, jdouble minAngle) {

   TriangleTessellator *tt = (TriangleTessellator*) handle;
   int len = env->GetArrayLength(pntCoords);
   if (len < numPnts * 2) {
      return -1;
   }
   jboolean isCopy;
   double *coords = env->GetDoubleArrayElements(pntCoords, &isCopy);

   try {
      tt->buildFromPoints(coords, numPnts, minAngle);
   } catch (int e) {
      printf("Triangle encountered an error, threw exception %d\n", e);
      return -1;
   } catch (...) {
      printf("Triangle encountered an error, threw unknown exception\n");
      return -1;
   }

   env->ReleaseDoubleArrayElements(pntCoords, coords, JNI_ABORT);

   return 0;
}

// Allow 'add-in' points to refine the mesh
void TriangleTessellator::buildFromSegmentsAndPoints(double *coords,
      int numPnts, int *segments, int numSegments, double minAngle) {

   if (in != NULL) {
      freeIO(in);
   } else {
      in = new triangulateio();
      initIO(in);
   }

   if (vorout != NULL) {
      freeIO(vorout);
   } else {
      vorout = new triangulateio();
      initIO(vorout);
   }

   in->numberofpoints = numPnts;
   in->numberofpointattributes = 0;
   in->pointlist = (REAL *) malloc(numPnts * 2 * sizeof(REAL));
   for (int i = 0; i < numPnts * 2; i++) {
      in->pointlist[i] = coords[i];
   }
   in->pointattributelist = NULL;
   in->pointmarkerlist = NULL;

   in->numberofholes = 0;
   in->numberofregions = 0;
   in->regionlist = NULL;

   in->numberofsegments = numSegments;
   in->segmentlist = (int *) malloc(numSegments * 2 * sizeof(int));
   in->segmentmarkerlist = (int *) malloc(numSegments * sizeof(int));
   for (int i = 0; i < numSegments; i++) {
      in->segmentmarkerlist[i] = 0;
   }
   for (int i = 0; i < 2 * numSegments; i++) {
      in->segmentlist[i] = segments[i];
   }

   if (out != NULL) {
      freeIO(out);
   } else {
      out = new triangulateio();
      initIO(out);
   }

   char switches[50];
   switches[0] = 0;

   if (minAngle > 0) {
      sprintf(switches, "%sq%4.2f", "zQYYp", minAngle);
   } else {
      sprintf(switches, "%s", "zQYYp");
   }
   triangulate(switches, in, out, vorout);
}

JNIEXPORT jint JNICALL Java_maspack_geometry_TriangleTessellator_doBuildFromSegmentsAndPoints(
      JNIEnv *env, jobject obj, jlong handle, jdoubleArray pntCoords,
      jint numPnts, jintArray segmentIndices, jint numSegments,
      jdouble minAngle) {

   TriangleTessellator *tt = (TriangleTessellator*) handle;
   int len = env->GetArrayLength(pntCoords);
   if (len < numPnts * 2) {
      return -1;
   }
   len = env->GetArrayLength(segmentIndices);
   if (len < 2 * numSegments) {
      return -1;
   }

   jboolean isCopy;
   double *coords = env->GetDoubleArrayElements(pntCoords, &isCopy);
   int *indices = (int*) env->GetIntArrayElements(segmentIndices, &isCopy);

   try {
      tt->buildFromSegmentsAndPoints(coords, numPnts, indices, numSegments,
            minAngle);
   } catch (int e) {
      printf("Triangle encountered an error, threw exception %d\n", e);
      return -1;
   } catch (...) {
      printf("Triangle encountered an error, threw unknown exception\n");
      return -1;
   }

   env->ReleaseDoubleArrayElements(pntCoords, coords, JNI_ABORT);
   env->ReleaseIntArrayElements(segmentIndices, (jint*) indices, JNI_ABORT);

   return 0;
}

void TriangleTessellator::buildFromSegments(double *coords, int numPnts,
      int *segments, int numSegments, double minAngle) {

   if (in != NULL) {
      freeIO(in);
   } else {
      in = new triangulateio();
      initIO(in);
   }

   if (vorout != NULL) {
      freeIO(vorout);
   } else {
      vorout = new triangulateio();
      initIO(vorout);
   }

   in->numberofpoints = numPnts;
   in->numberofpointattributes = 0;
   in->pointlist = (REAL *) malloc(numPnts * 2 * sizeof(REAL));
   for (int i = 0; i < numPnts * 2; i++) {
      in->pointlist[i] = coords[i];
   }
   in->pointattributelist = NULL;
   in->pointmarkerlist = NULL;

   in->numberofholes = 0;
   in->numberofregions = 0;
   in->regionlist = NULL;

   in->numberofsegments = numSegments;
   in->segmentlist = (int *) malloc(numSegments * 2 * sizeof(int));
   in->segmentmarkerlist = (int *) malloc(numSegments * sizeof(int));
   for (int i = 0; i < numSegments; i++) {
      in->segmentmarkerlist[i] = 0;
   }
   for (int i = 0; i < 2 * numSegments; i++) {
      in->segmentlist[i] = segments[i];
   }

   if (out != NULL) {
      freeIO(out);
   } else {
      out = new triangulateio();
      initIO(out);
   }

   char switches[50];
   switches[0] = 0;
   if (minAngle > 0) {
      sprintf(switches, "%sq%4.2f", "zQYYp", minAngle);
   } else {
      sprintf(switches, "%s", "zQYYp");
   }
   triangulate(switches, in, out, vorout);
}

JNIEXPORT jint JNICALL Java_maspack_geometry_TriangleTessellator_doBuildFromSegments(
      JNIEnv *env, jobject obj, jlong handle, jdoubleArray pntCoords,
      jint numPnts, jintArray segmentIndices, jint numSegments,
      jdouble minAngle) {

   TriangleTessellator *tt = (TriangleTessellator*) handle;
   int len = env->GetArrayLength(pntCoords);
   if (len < numPnts * 2) {
      return -1;
   }
   len = env->GetArrayLength(segmentIndices);
   if (len < 2 * numSegments) {
      return -1;
   }
   jboolean isCopy;
   double *coords = env->GetDoubleArrayElements(pntCoords, &isCopy);
   int *indices = (int*) env->GetIntArrayElements(segmentIndices, &isCopy);

   try {
      tt->buildFromSegments(coords, numPnts, indices, numSegments, minAngle);
   } catch (int e) {
      printf("Tetgen encountered an error, threw exception %d\n", e);
      return -1;
   } catch (...) {
      printf("Tetgen encountered an error, threw unknown exception\n");
      return -1;
   }

   env->ReleaseDoubleArrayElements(pntCoords, coords, JNI_ABORT);
   env->ReleaseIntArrayElements(segmentIndices, (jint*) indices, JNI_ABORT);

   return 0;
}

JNIEXPORT jint JNICALL Java_maspack_geometry_TriangleTessellator_doGetNumTriangles(
      JNIEnv *env, jobject obj, jlong handle) {

   TriangleTessellator *tt = (TriangleTessellator*) handle;
   return tt->out->numberoftriangles;
}

JNIEXPORT void JNICALL Java_maspack_geometry_TriangleTessellator_doGetTriangles
(JNIEnv *env, jobject obj, jlong handle, jintArray triangles) {

   TriangleTessellator *tt = (TriangleTessellator*)handle;
   triangulateio *out = tt->out;
   int len = env->GetArrayLength (triangles);
   int ntris = tt->out->numberoftriangles;
   if (len < ntris*3) {
      return;
   }
   jboolean isCopy;
   int *idxs = (int*)env->GetIntArrayElements (triangles, &isCopy);
   for (int i=0; i<3*ntris; i++) {  
      idxs[i] = out->trianglelist[i];
   }
   env->ReleaseIntArrayElements (triangles, (jint*)idxs, 0);
}

JNIEXPORT jint JNICALL Java_maspack_geometry_TriangleTessellator_doGetNumPoints(
      JNIEnv *env, jobject obj, jlong handle) {

   TriangleTessellator *tt = (TriangleTessellator*) handle;
   return tt->out->numberofpoints;
}

JNIEXPORT void JNICALL Java_maspack_geometry_TriangleTessellator_doGetPoints
(JNIEnv *env, jobject obj, jlong handle, jdoubleArray pntCoords) {

   TriangleTessellator *tt = (TriangleTessellator*)handle;
   triangulateio *out = tt->out;
   int len = env->GetArrayLength (pntCoords);
   int npnts = tt->out->numberofpoints;
   if (len < npnts*2) {
      return;
   }
   jboolean isCopy;
   double *coords = env->GetDoubleArrayElements (pntCoords, &isCopy);
   for (int i=0; i<2*npnts; i++) {  
      coords[i] = out->pointlist[i];
   }
   env->ReleaseDoubleArrayElements (pntCoords, coords, 0);
}

JNIEXPORT jlong JNICALL Java_maspack_geometry_TriangleTessellator_doCreateTessellator(
      JNIEnv *env, jobject obj) {

   TriangleTessellator *tt = new TriangleTessellator();
   return (jlong) tt;
}

JNIEXPORT void JNICALL Java_maspack_geometry_TriangleTessellator_doDeleteTessellator
(JNIEnv *env, jobject obj, jlong handle) {
   delete (TriangleTessellator*)handle;
}

