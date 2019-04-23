/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render.GL;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2GL3;

import maspack.render.IsRenderable;
import maspack.render.IsSelectable;

/**
 * GLSelector that uses occlusion queries, so will select everything,
 * including occluded objects within the supplied range.
 */
public class GLOcclusionSelector extends GLSelector {

   public static final int MAX_OCCLUSION_QUERIES = 1<<16; 

   int[] myQuerySamples; // number of samples passed for each query
   int[] myGLQueries;  // GL query ids
   int[] myGLQueryIds; // renderer query ids (associated with begin/endSelectionQuery(...))
   int myGLQueryCount; // number of queries issued
   int myGLQueryTotal; // number of queries issued in total
   
   boolean savedDepth;

   public GLOcclusionSelector (GLViewer viewer) {
      super (viewer);
   }

   @Override
   public void setupSelection (GL gl) {
      super.setupSelection (gl);

      myViewer.setSelectingColor (1, 1, 1, 1);
      
      // disable using the depth buffer
      savedDepth = myViewer.setDepthEnabled(false);

      // allocate query handles for the max number of queries
      myQuerySamples = new int[myTotalMaxQ];  // number of samples each query has passed
      int maxGLQueries = Math.min (myTotalMaxQ, MAX_OCCLUSION_QUERIES);
      myGLQueries = new int[maxGLQueries];
      myGLQueryIds = new int[maxGLQueries];
      myGLQueryCount = 0;
      myGLQueryTotal = 0;

      if (myTotalMaxQ > 0) {
         ((GL2GL3)myGl).glGenQueries (myGLQueries.length, myGLQueries, 0);  
      }
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

   @Override
   public void processSelection (GL gl) {
      
      if (myTotalMaxQ == 0) {
         super.processSelection(gl);
         return;
      }
      
      // finish remaining queries
      flushQueries ((GL2GL3)gl);
      
      // re-enable depth buffer
      myViewer.setDepthEnabled(savedDepth);

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
                     if (myQuerySamples.length <= qid+i) break;
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
      ((GL2GL3)myGl).glDeleteQueries (myGLQueries.length, myGLQueries, 0);
      myGLQueries = null;
      myGLQueryIds = null;

      super.processSelection (gl);
   }

   public void beginSelectionQuery (int idx) {
      if (myCurrentIdx != -1) {
         throw new IllegalStateException (
         "beginSelectionQuery() can't be called recursively");
      }
      if (idx < 0 || idx >= myCurrentMaxQ) {
         throw new IllegalArgumentException (
            "index "+idx+" out of range; max=" + myCurrentMaxQ);
      }

      // flush queries if we need room
      if (myGLQueryCount == myGLQueries.length) {
         flushQueries ((GL2GL3)myGl);
      }
      ((GL2GL3)myGl).glBeginQuery (GL2.GL_SAMPLES_PASSED, myGLQueries[myGLQueryCount]);
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
      ((GL2GL3)myGl).glEndQuery (GL2.GL_SAMPLES_PASSED);
      myCurrentIdx = -1;
   }  

}
