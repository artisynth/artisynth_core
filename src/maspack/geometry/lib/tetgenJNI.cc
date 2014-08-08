/**
 * Copyright (c) 2014, by the Authors: John Lloyd (UBC), Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

#include <stdlib.h>
#include <stdio.h>

#include "tetgenJNI.h"
#include "maspack_geometry_TetgenTessellator.h"

TetgenTessellator::TetgenTessellator() {
   in = new tetgenio();
   out = new tetgenio();
   addin = NULL;
}

TetgenTessellator::~TetgenTessellator() {
   if (in != NULL) {
      delete in;
      in = NULL;
   }
   if (out != NULL) {
      delete out;
      out = NULL;
   }
   if (addin != NULL) {
      delete addin;
      addin = NULL;
   }
}

void TetgenTessellator::buildFromPoints (double *coords, int numPnts) {

   if (in != NULL) {
      in->deinitialize();
      in->initialize();
   } else {
      in = new tetgenio();
   }

   in->firstnumber = 0;
   in->numberofpoints = numPnts;
   in->pointlist = new REAL[numPnts*3];
   for (int i=0; i<numPnts*3; i++) {
      in->pointlist[i] = coords[i];
   }
   in->numberoffacets = 0;

   if (out != NULL) {
      out->deinitialize();
      out->initialize();
   } else {
      out = new tetgenio();
   }
   char *switches = (char*)"Q";
   tetrahedralize (switches, in, out);
}

JNIEXPORT jint JNICALL Java_maspack_geometry_TetgenTessellator_doBuildFromPoints
(JNIEnv *env, jobject obj, jlong handle,
      jdoubleArray pntCoords, jint numPnts) {

   TetgenTessellator *tt = (TetgenTessellator*)handle;
   int len = env->GetArrayLength(pntCoords);
   if (len < numPnts*3) {
      return -1;
   }
   jboolean isCopy;
   double *coords = env->GetDoubleArrayElements (pntCoords, &isCopy);

   try {
      tt->buildFromPoints (coords, numPnts);
   }
   catch (int e) {
      printf ("Tetgen encountered an error, threw exception %d\n", e);
      return -1;
   }
   catch (...) {
      printf ("Tetgen encountered an error, threw unknown exception\n");
      return -1;
   }

   env->ReleaseDoubleArrayElements (pntCoords, coords, JNI_ABORT);

   return 0;
}

// Allow 'add-in' points to refine the mesh
void TetgenTessellator::buildFromMeshAndPoints (
      double *coords, int numPnts, int *indices,
      int numFaces, int numIndices, double quality,
      double *includeCoords, int numIncludePnts) {

   if (in != NULL) {
      in->deinitialize();
      in->initialize();
   } else {
      in = new tetgenio();
   }

   in->firstnumber = 0;
   in->numberofpoints = numPnts;
   in->pointlist = new REAL[numPnts * 3];
   for (int i = 0; i < numPnts * 3; i++) {
      in->pointlist[i] = coords[i];
   }

   in->numberoffacets = numFaces;
   in->facetlist = new tetgenio::facet[numFaces];
   in->facetmarkerlist = new int[numFaces];
   for (int i = 0; i < numFaces; i++) {
      in->facetmarkerlist[i] = 0;
   }

   int k = 0;
   int faceNum = 0;
   int klimit = numIndices + numFaces;
   while (k < klimit) {
      int nv = indices[k++];
      tetgenio::facet *f = &(in->facetlist[faceNum++]);
      f->polygonlist = new tetgenio::polygon[1];
      f->numberofpolygons = 1;
      f->holelist = (REAL *) NULL;
      f->numberofholes = 0;
      tetgenio::polygon *p = &(f->polygonlist[0]);
      p->numberofvertices = nv;
      p->vertexlist = new int[nv];
      for (int i = 0; i < nv; i++) {
         if (k == klimit) {
            break; // shouldn't happen
         }
         p->vertexlist[i] = indices[k++];
      }
   }

   // add-in points
   tetgenio *laddin = NULL; // local pointer to add-in
   if (numIncludePnts > 0) {
      if (addin != NULL) {
         delete addin;
      }
      addin = new tetgenio();
      addin->numberofpoints = numIncludePnts;
      addin->mesh_dim = 3;
      addin->numberofpointattributes = 0;
      addin->pointlist = new REAL[numIncludePnts*3];
      for (int i=0; i<numIncludePnts*3; i++) {
         addin->pointlist[i] = includeCoords[i];
      }
      addin->numberoffacets = 0;

      laddin = addin;
   }

   if (out != NULL) {
      out->deinitialize();
      out->initialize();
   } else {
      out = new tetgenio();
   }

   //    tetgenbehavior *behavior = new tetgenbehavior();
   //    behavior->quiet = 0;
   //    behavior->plc = 1;
   //    behavior->nofacewritten = 1;
   char switches[50];
   switches[0] = 0;
   if (quality > 0) {
      sprintf(switches, "Qpiq%4.2f", quality);
   } else {
      sprintf(switches, "Qpi");
   }
   tetrahedralize(switches, in, out, laddin);
}

JNIEXPORT jint JNICALL Java_maspack_geometry_TetgenTessellator_doBuildFromMeshAndPoints
(JNIEnv *env, jobject obj, jlong handle,
      jdoubleArray pntCoords, jint numPnts,
      jintArray faceIndices, jint numFaces, jint numIndices, jdouble quality,
      jdoubleArray includeCoords, jint includeCoordsOffset, jint numIncludePnts) {

   TetgenTessellator *tt = (TetgenTessellator*)handle;
   int len = env->GetArrayLength(pntCoords);
   if (len < numPnts*3) {
      return -1;
   }
   len = env->GetArrayLength(faceIndices);
   if (len < numIndices + numFaces) {
      return -1;
   }
   len = env->GetArrayLength(includeCoords);
   if (len < numIncludePnts*3) {
      return -1;
   }

   jboolean isCopy;
   double *coords = env->GetDoubleArrayElements (pntCoords, &isCopy);
   int *indices = (int*)env->GetIntArrayElements (faceIndices, &isCopy);
   double *addCoords = env->GetDoubleArrayElements (includeCoords, &isCopy);

   try {
      tt->buildFromMeshAndPoints (coords, numPnts, indices, numFaces, numIndices, quality,
            &addCoords[includeCoordsOffset], numIncludePnts);
   }
   catch (int e) {
      printf ("Tetgen encountered an error, threw exception %d\n", e);
      return -1;
   }
   catch (...) {
      printf ("Tetgen encountered an error, threw unknown exception\n");
      return -1;
   }

   env->ReleaseDoubleArrayElements (pntCoords, coords, JNI_ABORT);
   env->ReleaseIntArrayElements (faceIndices, (jint*)indices, JNI_ABORT);
   env->ReleaseDoubleArrayElements (includeCoords, addCoords, JNI_ABORT);

   return 0;
}

void TetgenTessellator::buildFromMesh (
      double *coords, int numPnts, int *indices,
      int numFaces, int numIndices, double quality) {

   if (in != NULL) {
      in->deinitialize();
      in->initialize();
   } else {
      in = new tetgenio();
   }

   in->firstnumber = 0;
   in->numberofpoints = numPnts;
   in->pointlist = new REAL[numPnts*3];
   for (int i=0; i<numPnts*3; i++) {
      in->pointlist[i] = coords[i];
   }

   in->numberoffacets = numFaces;
   in->facetlist = new tetgenio::facet[numFaces];
   in->facetmarkerlist = new int[numFaces];
   for (int i=0; i<numFaces; i++) {
      in->facetmarkerlist[i] = 0;
   }

   int k = 0;
   int faceNum = 0;
   int klimit = numIndices + numFaces;
   while (k < klimit) {
      int nv = indices[k++];
      tetgenio::facet *f = &(in->facetlist[faceNum++]);
      f->polygonlist = new tetgenio::polygon[1];
      f->numberofpolygons = 1;
      f->holelist = (REAL *) NULL;
      f->numberofholes = 0;
      tetgenio::polygon *p = &(f->polygonlist[0]);
      p->numberofvertices = nv;
      p->vertexlist = new int[nv];
      for (int i=0; i<nv; i++) {
         if (k == klimit) {
            break; // shouldn't happen
         }
         p->vertexlist[i] = indices[k++];
      }
   }

   if (out != NULL) {
      out->deinitialize();
      out->initialize();
   } else {
      out = new tetgenio();
   }

   //    tetgenbehavior *behavior = new tetgenbehavior();
   //    behavior->quiet = 0;
   //    behavior->plc = 1;
   //    behavior->nofacewritten = 1;
   char switches[50];
   switches[0] = 0;
   if (quality > 0) {
      sprintf(switches, "Qpq%4.2f", quality);
   } else {
      sprintf(switches, "Qp");
   }
   tetrahedralize (switches, in, out);
   //delete behavior;
}

JNIEXPORT jint JNICALL Java_maspack_geometry_TetgenTessellator_doBuildFromMesh
(JNIEnv *env, jobject obj, jlong handle,
      jdoubleArray pntCoords, jint numPnts,
      jintArray faceIndices, jint numFaces, jint numIndices, jdouble quality) {

   TetgenTessellator *tt = (TetgenTessellator*)handle;
   int len = env->GetArrayLength(pntCoords);
   if (len < numPnts*3) {
      return -1;
   }
   len = env->GetArrayLength(faceIndices);
   if (len < numIndices + numFaces) {
      return -1;
   }
   jboolean isCopy;
   double *coords = env->GetDoubleArrayElements (pntCoords, &isCopy);
   int *indices = (int*)env->GetIntArrayElements (faceIndices, &isCopy);

   try {
      tt->buildFromMesh (coords, numPnts, indices, numFaces, numIndices, quality);
   }
   catch (int e) {
      printf ("Tetgen encountered an error, threw exception %d\n", e);
      return -1;
   }
   catch (...) {
      printf ("Tetgen encountered an error, threw unknown exception\n");
      return -1;
   }

   env->ReleaseDoubleArrayElements (pntCoords, coords, JNI_ABORT);
   env->ReleaseIntArrayElements (faceIndices, (jint*)indices, JNI_ABORT);

   return 0;   
}

void TetgenTessellator::refineMesh(
      double *coords, int numNodes, int *tetIndices,
      int numTets, double quality,
      double *addCoords, int numAddPnts) {

   if (in != NULL) {
      in->deinitialize();
      in->initialize();
   } else {
      in = new tetgenio();
   }

   // read in nodes
   in->firstnumber = 0;
   in->numberofpoints = numNodes;
   in->numberofpointattributes = 0;
   in->pointlist = new REAL[numNodes * 3];
   for (int i = 0; i < numNodes * 3; i++) {
      in->pointlist[i] = coords[i];
   }

   // no faces
   in->numberoffacets = 0;

   // read in tets
   in->numberoftetrahedra = numTets;
   in->numberofcorners = 4;
   in->numberoftetrahedronattributes = 0;
   in->tetrahedronlist = new int[numTets*4];

   for (int i=0; i<4*numTets; i++) {
      in->tetrahedronlist[i] = tetIndices[i];
   }

   tetgenio *laddin = NULL; // local pointer to add-in
   if (numAddPnts > 0) {

      // add-in points
      if (addin != NULL) {
         addin->deinitialize();
         addin->initialize();
      } else {
         addin = new tetgenio();
      }

      addin->numberofpoints = numAddPnts;
      addin->mesh_dim = 3;
      addin->numberofpointattributes = 0;
      addin->pointlist = new REAL[numAddPnts*3];
      for (int i=0; i<numAddPnts*3; i++) {
         addin->pointlist[i] = addCoords[i];
      }
      addin->numberoffacets = 0;

      laddin = addin;
   }

   if (out != NULL) {
      out->deinitialize();
      out->initialize();
   } else {
      out = new tetgenio();
   }

   char switches[50];
   switches[0] = 0;
   if (quality > 0) {
      sprintf(switches, "Qriq%4.2f", quality);
   } else {
      sprintf(switches, "Qri");
   }
   tetrahedralize(switches, in, out, laddin);

}

JNIEXPORT jint JNICALL Java_maspack_geometry_TetgenTessellator_doRefineMeshAddPoints
(JNIEnv *env, jobject obj, jlong handle,
      jdoubleArray nodeCoords, jint numNodes,
      jintArray tetIndices, jint numTets, jdouble quality,
      jdoubleArray addCoords, jint numAddPnts) {

   TetgenTessellator *tt = (TetgenTessellator*)handle;
   int len = env->GetArrayLength(nodeCoords);
   if (len < numNodes*3) {
      return -1;
   }
   len = env->GetArrayLength(tetIndices);
   if (len < numTets*4) {
      return -1;
   }
   len = env->GetArrayLength(addCoords);
   if (len < numAddPnts*3) {
      return -1;
   }

   jboolean isCopy;
   double *coords = env->GetDoubleArrayElements (nodeCoords, &isCopy);
   int *indices = (int*)env->GetIntArrayElements (tetIndices, &isCopy);
   double *addedCoords = env->GetDoubleArrayElements (addCoords, &isCopy);

   try {
      tt->refineMesh (coords, numNodes, indices, numTets,
            quality, addedCoords, numAddPnts);
   }
   catch (int e) {
      printf ("Tetgen encountered an error, threw exception %d\n", e);
      return -1;
   }
   catch (...) {
      printf ("Tetgen encountered an error, threw unknown exception\n");
      return -1;
   }

   env->ReleaseDoubleArrayElements (nodeCoords, coords, JNI_ABORT);
   env->ReleaseIntArrayElements (tetIndices, (jint*)indices, JNI_ABORT);
   env->ReleaseDoubleArrayElements (addCoords, addedCoords, JNI_ABORT);

   return 0;

}

JNIEXPORT jint JNICALL Java_maspack_geometry_TetgenTessellator_doRefineMesh
(JNIEnv *env, jobject obj, jlong handle,
      jdoubleArray nodeCoords, jint numNodes,
      jintArray tetIndices, jint numTets, jdouble quality) {

   TetgenTessellator *tt = (TetgenTessellator*)handle;
   int len = env->GetArrayLength(nodeCoords);
   if (len < numNodes*3) {
      return -1;
   }
   len = env->GetArrayLength(tetIndices);
   if (len < numTets*4) {
      return -1;
   }

   jboolean isCopy;
   double *coords = env->GetDoubleArrayElements (nodeCoords, &isCopy);
   int *indices = (int*)env->GetIntArrayElements (tetIndices, &isCopy);

   try {
      tt->refineMesh (coords, numNodes, indices, numTets,
            quality, NULL, 0);
   }
   catch (int e) {
      printf ("Tetgen encountered an error, threw exception %d\n", e);
      return -1;
   }
   catch (...) {
      printf ("Tetgen encountered an error, threw unknown exception\n");
      return -1;
   }

   env->ReleaseDoubleArrayElements (nodeCoords, coords, JNI_ABORT);
   env->ReleaseIntArrayElements (tetIndices, (jint*)indices, JNI_ABORT);

   return 0;

}

JNIEXPORT jint JNICALL Java_maspack_geometry_TetgenTessellator_doGetNumHullFaces
(JNIEnv *env, jobject obj, jlong handle) {

   TetgenTessellator *tt = (TetgenTessellator*)handle;
   return tt->out->numberoftrifaces;
}

JNIEXPORT void JNICALL Java_maspack_geometry_TetgenTessellator_doGetHullFaces
(JNIEnv *env, jobject obj, jlong handle, jintArray faces) {

   TetgenTessellator *tt = (TetgenTessellator*)handle;
   tetgenio *out = tt->out;
   int len = env->GetArrayLength (faces);
   int nfaces = tt->out->numberoftrifaces;
   if (len < nfaces*3) {
      return;
   }
   jboolean isCopy;
   int *idxs = (int*)env->GetIntArrayElements (faces, &isCopy);
   for (int i=0; i<len; i++)
   { idxs[i] = out->trifacelist[i];
   }
   env->ReleaseIntArrayElements (faces, (jint*)idxs, 0);
}

JNIEXPORT jint JNICALL Java_maspack_geometry_TetgenTessellator_doGetNumTets 
(JNIEnv *env, jobject obj, jlong handle) {

   TetgenTessellator *tt = (TetgenTessellator*)handle;
   return tt->out->numberoftetrahedra;
}

JNIEXPORT void JNICALL Java_maspack_geometry_TetgenTessellator_doGetTets
(JNIEnv *env, jobject obj, jlong handle, jintArray tets) {

   TetgenTessellator *tt = (TetgenTessellator*)handle;
   tetgenio *out = tt->out;
   int len = env->GetArrayLength (tets);
   int ntets = tt->out->numberoftetrahedra;
   if (len < ntets*4) {
      return;
   }
   jboolean isCopy;
   int *idxs = (int*)env->GetIntArrayElements (tets, &isCopy);
   for (int i=0; i<len; i++)
   { idxs[i] = out->tetrahedronlist[i];
   }
   env->ReleaseIntArrayElements (tets, (jint*)idxs, 0);
}

JNIEXPORT jint JNICALL Java_maspack_geometry_TetgenTessellator_doGetNumPoints
(JNIEnv *env, jobject obj, jlong handle) {

   TetgenTessellator *tt = (TetgenTessellator*)handle;
   return tt->out->numberofpoints;
}

JNIEXPORT void JNICALL Java_maspack_geometry_TetgenTessellator_doGetPoints
(JNIEnv *env, jobject obj, jlong handle, jdoubleArray pntCoords) {

   TetgenTessellator *tt = (TetgenTessellator*)handle;
   tetgenio *out = tt->out;
   int len = env->GetArrayLength (pntCoords);
   int npnts = tt->out->numberofpoints;
   if (len < npnts*3) {
      return;
   }
   jboolean isCopy;
   double *coords = env->GetDoubleArrayElements (pntCoords, &isCopy);
   for (int i=0; i<len; i++)
   { coords[i] = out->pointlist[i];
   }
   env->ReleaseDoubleArrayElements (pntCoords, coords, 0);
}

JNIEXPORT jlong JNICALL Java_maspack_geometry_TetgenTessellator_doCreateTessellator 
(JNIEnv *env, jobject obj) {

   TetgenTessellator *tt = new TetgenTessellator();
   return (jlong)tt;
}

JNIEXPORT void JNICALL Java_maspack_geometry_TetgenTessellator_doDeleteTessellator
(JNIEnv *env, jobject obj, jlong handle) {

   delete (TetgenTessellator*)handle;   
}



