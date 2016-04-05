/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render.GL;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;

import javax.media.opengl.GL2;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GLAutoDrawable;

import maspack.matrix.Vector2d;

/**
 * GLSelector that works using the traditional GL_SELECT mechanism (now
 * deprecated).
 */
public class GLOcclusionSelector extends GLSelector {

   GL2GL3 myGl;

   int myTotalMaxQ;
   int myCurrentMaxQ;
   int myCurrentIdx;
   int myIdxBase;
   int[] myQueries;
   boolean[] myQueryWasUsed;
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
      // // find out how many queries we'll need
      // myTotalMaxQ = 0;
      // Iterator<GLRenderable> it = myViewer.renderIterator();

      // while (it.hasNext()) {
      //    GLRenderable r = it.next();
      //    GLSelectable s;
      //    if (r instanceof GLSelectable && (s = (GLSelectable)r).isSelectable()) {
      //       int numq = s.numSelectionQueriesNeeded();
      //       if (numq > 0) {
      //          myTotalMaxQ += numq;
      //       }
      //       else {
      //          myTotalMaxQ += 1;
      //       }
      //    }
      // }

      myCurrentMaxQ = myTotalMaxQ;
      myCurrentIdx = -1;
      myIdxBase = 0;
      myMaxQStack.clear(); // paranoid ...
      myIdxBaseStack.clear();  // paranoid ...

      // allocate query handles for the max number of queries
      myQueries = new int[myTotalMaxQ];
      myQueryWasUsed = new boolean[myTotalMaxQ];
      gl.glGenQueries (myTotalMaxQ, myQueries, 0);

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
      // disable writing to the depth buffer
      viewer.setDepthEnabled(false);
      
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

      // find the last query that was issued ...
      int lastQidx = -1;
      for (int i=myTotalMaxQ-1; i>=0; i--) {
         if (myQueryWasUsed[i]) {
            lastQidx = i;
            break;
         }
      }

      if (lastQidx == -1) {
         // then no queries were issued, so nothing to do ...
         myViewer.setSelected(new LinkedList[0]);
      }
      else {
         // make sure queries are ready
         int[] available = new int[1];
         do {
            gl.glGetQueryObjectiv (
               myQueries[lastQidx], GL2.GL_QUERY_RESULT_AVAILABLE, available, 0);
            if (available[0] == 0) {
               waitMsec (1);
            }
         }
         while (available[0] == 0);

         int qid = 0;
         LinkedList<HitRecord> records = new LinkedList<HitRecord>();
         int[] result = new int[1];
         Iterator<GLRenderable> it = myViewer.renderIterator();
         while (it.hasNext()) {
            GLRenderable r = it.next();
            if (r instanceof GLSelectable) {
               GLSelectable s = (GLSelectable)r;
               int numq = s.numSelectionQueriesNeeded();
               int nums = (numq >= 0 ? numq : 1);
               if (s.isSelectable()) {
                  for (int i=0; i<nums; i++) {
                     if (myQueryWasUsed[qid+i]) {
                        gl.glGetQueryObjectuiv (
                           myQueries[qid+i], GL2.GL_QUERY_RESULT, result, 0);
                        if (result[0] > 0) {
                           HitRecord rec = new HitRecord(result[0]);
                           if (numq < 0) {
                              rec.objs.add (s);
                           }
                           else {
                              s.getSelection (rec.objs, i);
                           }
                           if (rec.objs.size() > 0) {
                              records.add (rec);
                           }
                        }
                     }
                  }
               }
               qid += nums;
               if (qid > lastQidx) {
                  // no need to explore beyond the last query
                  break;
               }
            }
         }
         Collections.sort (records);
         LinkedList<Object>[] selObjs = new LinkedList[records.size()];
         for (int i=0; i<records.size(); i++) {
            selObjs[i] = records.get(i).objs;
         }
         myViewer.setSelected(selObjs);         
      }
      
      // delete queries
      gl.glDeleteQueries (myTotalMaxQ, myQueries, 0);
      myQueries = null;
      myQueryWasUsed = null;

      GLSelectionListener[] listeners = myViewer.getSelectionListeners();
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
      if (myQueryWasUsed[myIdxBase+idx]) {
         throw new IllegalStateException (
            "query for idx=" + idx + " is already in use");
      }
      myGl.glBeginQuery (
         GL2.GL_SAMPLES_PASSED, myQueries[myIdxBase+idx]);
      myQueryWasUsed[myIdxBase+idx] = true;
      myCurrentIdx = idx;
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

   public void beginSelectionForObject (GLSelectable s, int idx) {
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
