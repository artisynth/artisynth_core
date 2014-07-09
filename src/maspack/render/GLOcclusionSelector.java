/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

import java.util.*;
import java.nio.IntBuffer;
import javax.media.opengl.*;
import javax.media.opengl.glu.*;

import maspack.matrix.Vector2d;
import maspack.util.ArraySupport;

/**
 * GLSelector that works using the traditional GL_SELECT mechanism (now
 * deprecated).
 */
public class GLOcclusionSelector extends GLSelector {

   GL2 myGl;

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

      GL2 gl = myViewer.getGL().getGL2();
      GLU glu = myViewer.getGLU();
      myGl = gl;

      myTotalMaxQ = myViewer.numSelectionQueriesNeeded();

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

      gl.glGetIntegerv (GL2.GL_VIEWPORT, mySavedViewport, 0);
      gl.glMatrixMode (GL2.GL_PROJECTION);
      gl.glPushMatrix();
      gl.glLoadIdentity();

      int[] viewport;
      if (changeViewport) {
         viewport =
            new int[] {0, 0, (int)Math.ceil(myRectW), (int)Math.ceil(myRectH) };

         glu.gluPickMatrix (
            myRectX, mySavedViewport[3] - myRectY, myRectW, myRectH,
            mySavedViewport, 0);
         gl.glViewport (viewport[0], viewport[1], viewport[2], viewport[3]);
      }
      else{
         viewport = ArraySupport.copy (mySavedViewport);
         glu.gluPickMatrix (
            myRectX, viewport[3] - myRectY, myRectW, myRectH, viewport, 0);
      }

      Vector2d zRange = new Vector2d();
      myViewer.getZRange(zRange);
      myViewer.setViewVolume(/*near=*/zRange.x, /*far=*/zRange.y);
      // setViewVolume (width, height);
      gl.glMatrixMode (GL2.GL_MODELVIEW);

      gl.glDisable (GL2.GL_LIGHTING);
      // disable writing to the framebuffer 
      gl.glColorMask (false, false, false, false);
      // disable writing to the depth buffer
      gl.glDepthMask (false);
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

      GL2 gl = myGl;

      gl.glEnable (GL2.GL_LIGHTING);
      gl.glDepthMask (true);
      gl.glColorMask (true, true, true, true);

      if (changeViewport) {
         gl.glViewport (mySavedViewport[0], mySavedViewport[1], 
                        mySavedViewport[2], mySavedViewport[3]);
      }

      gl.glMatrixMode (GL2.GL_PROJECTION);
      gl.glPopMatrix();
      gl.glMatrixMode (GL2.GL_MODELVIEW);

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
         myViewer.selectionEvent.mySelectedObjects = new LinkedList[0];
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
         myViewer.selectionEvent.mySelectedObjects = selObjs;         
      }
      
      // delete queries
      gl.glDeleteQueries (myTotalMaxQ, myQueries, 0);
      myQueries = null;
      myQueryWasUsed = null;

      GLSelectionListener[] listeners = myViewer.getSelectionListeners();
      for (int i=0; i<listeners.length; i++) {
         listeners[i].itemsSelected (myViewer.selectionEvent);
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
