/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render.GL;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;

import javax.media.opengl.GL2;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GLAutoDrawable;

import maspack.matrix.Vector2d;
import maspack.render.IsRenderable;
import maspack.render.IsSelectable;
import maspack.render.ViewerSelectionListener;

/**
 * GLSelector that works using the traditional GL_SELECT mechanism (now
 * deprecated).
 */
public class GLOcclusionSelector extends GLSelector {

   public static final int MAX_OCCLUSION_QUERIES = 1<<12; 
   
   GL2GL3 myGl;

   int myTotalMaxQ;
   int myCurrentMaxQ;
   int myCurrentIdx;
   int myIdxBase;
   int[] myQuerySamples; // number of samples passed for each query
   
   int[] myGLQueries;  // GL query ids
   int[] myGLQueryIds; // renderer query ids (associated with begin/endSelectionQuery(...))
   int myGLQueryCount; // number of queries issued
   int myGLQueryTotal; // number of queries issued in total
   
   Deque<Integer> myMaxQStack;
   Deque<Integer> myIdxBaseStack;

   boolean changeViewport = true;

   int[] mySavedViewport = new int[4];

   public GLOcclusionSelector (GLViewer viewer) {
      super (viewer);
      myMaxQStack = new ArrayDeque<Integer>();
      myIdxBaseStack = new ArrayDeque<Integer>();
   }

   private class HitRecord implements Comparable<HitRecord> {
      int size;
      LinkedList<Object> objs;

      HitRecord (int s) {
         size = s;
         objs = new LinkedList<Object>();
      }

      // Implement comparison so that records with smaller size are *greater*
      // than those with larger size. That way, standard sorting will put
      // records with larger sizes first.
      public int compareTo (HitRecord obj) {
         HitRecord rec = (HitRecord)obj;
         if (size < rec.size) {
            return 1;
         }
         else if (size > rec.size) {
            return -1;
         }
         else {
            return 0;
         }
      }
   }

   public void setupSelection (GLAutoDrawable drawable) {

      if (!(myViewer instanceof GLViewer)) {
         return;
      }
      
      GLViewer viewer = (GLViewer)myViewer;
      GL2GL3 gl = (GL2GL3)drawable.getGL();
      myGl = gl;
      
      myTotalMaxQ = myViewer.numSelectionQueriesNeeded();

      // nothing to select
      if (myTotalMaxQ == 0) {
         return;
      }
      
      myCurrentMaxQ = myTotalMaxQ;
      myCurrentIdx = -1;
      myIdxBase = 0;
      myMaxQStack.clear();     // paranoid ...
      myIdxBaseStack.clear();  // paranoid ...
      myQuerySamples = new int[myTotalMaxQ];  // number of samples each query has passed
      
      // allocate query handles for the max number of queries
      int maxGLQueries = Math.min (myTotalMaxQ, MAX_OCCLUSION_QUERIES);
      myGLQueries = new int[maxGLQueries];
      myGLQueryIds = new int[maxGLQueries];
      myGLQueryCount = 0;
      myGLQueryTotal = 0;
      
      gl.glGenQueries (myTotalMaxQ, myGLQueries, 0);

      // restrict the viewport to the specified selection region
      mySavedViewport = viewer.getViewport(gl);
      
      // apply pick
      if (changeViewport) {
         viewer.setPickMatrix((float)myRectX, (float)(mySavedViewport[3] - myRectY),
            (float)myRectW, (float)myRectH, mySavedViewport);
         viewer.setViewport(gl, 0, 0, (int)Math.ceil(myRectW), (int)Math.ceil(myRectH));
         
      }
      else{
         viewer.setPickMatrix((float)myRectX,  (float)(mySavedViewport[3] - myRectY), 
            (float)myRectW, (float)myRectH, mySavedViewport);
      }

      Vector2d zRange = new Vector2d();
      myViewer.getZRange(zRange);
      myViewer.setViewVolume(/*near=*/zRange.x, /*far=*/zRange.y);
      
      // disable lighting
      viewer.setLightingEnabled(false);
      // disable writing to the framebuffer
      viewer.setColorEnabled(false);
      // disable using the depth buffer
      viewer.setDepthEnabled(false);
      
   }
   
   private void flushQueries(GL2GL3 gl) {
      
      if (myGLQueryCount == 0) {
         return;
      }
      
      // make sure queries are ready
      int[] result = new int[1];
      do {
         gl.glGetQueryObjectiv (
            myGLQueries[myGLQueryCount-1], GL2.GL_QUERY_RESULT_AVAILABLE, result, 0);
         if (result[0] == 0) {
            waitMsec (1);
         }
      }
      while (result[0] == 0);
      
      // fill in occlusion counts
      for (int i=0; i<myGLQueryCount; ++i) {
         gl.glGetQueryObjectuiv (
            myGLQueries[i], GL2.GL_QUERY_RESULT, result, 0);
         myQuerySamples[myGLQueryIds[i]] += result[0];
         myGLQueryIds[i] = -1;
      }
      
      myGLQueryTotal += myGLQueryCount;
      myGLQueryCount = 0;
      
   }

   private void waitMsec (int msec) {
      try {
         Thread.sleep (msec);
      }
      catch (Exception e) {
         //
      }
   }

   public void processSelection (GLAutoDrawable drawable) {

      if (!(myViewer instanceof GLViewer)) {
         return;
      }
      
      GLViewer viewer = (GLViewer)myViewer;
      GL2GL3 gl = (GL2GL3)(drawable.getGL());

      viewer.setColorEnabled(true);
      viewer.setDepthEnabled(true);
      viewer.setLightingEnabled(true);

      if (changeViewport) {
         gl.glViewport (mySavedViewport[0], mySavedViewport[1], 
                        mySavedViewport[2], mySavedViewport[3]);
      }

      viewer.clearPickMatrix();
      
      if (!myMaxQStack.isEmpty()) {
         throw new IllegalStateException (
            "Calls to begin/endSelectionForObject() not balanced");
      }

      // finish remaining queries
      flushQueries (myGl);
      
      if (myGLQueryTotal == 0) {
         // then no queries were issued, so nothing to do ...
         myViewer.setSelected(null);
      } else {
         int qid = 0;
         LinkedList<HitRecord> records = new LinkedList<HitRecord>();
         Iterator<IsRenderable> it = myViewer.renderIterator();
         while (it.hasNext()) {
            IsRenderable r = it.next();
            if (r instanceof IsSelectable) {
               IsSelectable s = (IsSelectable)r;
               int numq = s.numSelectionQueriesNeeded();
               int nums = (numq >= 0 ? numq : 1);
               if (s.isSelectable()) {
                  for (int i=0; i<nums; i++) {
                     if (myQuerySamples[qid+i] > 0) {
                        HitRecord rec = new HitRecord(myQuerySamples[qid+i]);
                        if (numq < 0) {
                           rec.objs.add (s);
                        } else {
                           s.getSelection (rec.objs, i);
                        }
                        if (rec.objs.size() > 0) {
                           records.add (rec);
                        }
                     }
                  }
               }
               qid += nums;
            }
         }
         Collections.sort (records);
         ArrayList<LinkedList<?>> selObjs = new ArrayList<>(records.size());
         for (int i=0; i<records.size(); i++) {
            selObjs.add (records.get(i).objs);
         }
         myViewer.setSelected(selObjs);         
      }
      
      // delete queries
      gl.glDeleteQueries (myTotalMaxQ, myGLQueries, 0);
      myGLQueries = null;
      myGLQueryIds = null;
      myGl = null;

      ViewerSelectionListener[] listeners = myViewer.getSelectionListeners();
      for (int i=0; i<listeners.length; i++) {
         listeners[i].itemsSelected (myViewer.getSelectionEvent());
      }
      myViewer.getCanvas().repaint();
   }

   public void beginSelectionQuery (int idx) {
      if (myCurrentIdx != -1) {
         throw new IllegalStateException (
            "beginSelectionQuery() can't be called recursively");
      }
      if (idx < 0 || idx >= myCurrentMaxQ) {
         throw new IllegalArgumentException ("index out of range");
      }
      
      // flush queries if we need room
      if (myGLQueryCount == myGLQueries.length) {
         flushQueries (myGl);
      }
      myGl.glBeginQuery (GL2.GL_SAMPLES_PASSED, myGLQueries[myGLQueryCount]);
      myGLQueryIds[myGLQueryCount] = myIdxBase+idx;
      myCurrentIdx = idx;
      ++myGLQueryCount;
   }

   public void endSelectionQuery () {
      if (myCurrentIdx == -1) {
         throw new IllegalStateException (
            "endSelectionQuery() called without previous call to "+
            "beginSelectionQuery()");
      }
      myGl.glEndQuery (GL2.GL_SAMPLES_PASSED);
      myCurrentIdx = -1;
   }

   public void beginSelectionForObject (IsSelectable s, int idx) {
      if (myCurrentIdx != -1) {
         throw new IllegalStateException (
            "missing call to endSelectionQuery()");
      }
      myMaxQStack.addFirst (myCurrentMaxQ);
      myIdxBaseStack.addFirst (myIdxBase);
      int numq = s.numSelectionQueriesNeeded();
      if (numq < 0) {
         throw new IllegalArgumentException (
            "numRequiredSelectionQueries() for selectable returns "+numq);
      }
      myCurrentMaxQ = numq;
      myIdxBase += idx;
   }

   public void endSelectionForObject () {
      if (myCurrentIdx != -1) {
         throw new IllegalStateException (
            "missing call to endSelectionQuery()");
      }
      myCurrentMaxQ = myMaxQStack.removeFirst();
      myIdxBase = myIdxBaseStack.removeFirst();
   }   
}
