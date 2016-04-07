/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render.GL;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;

import javax.media.opengl.GL;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GLAutoDrawable;

import maspack.render.IsRenderable;
import maspack.render.IsSelectable;
import maspack.render.ViewerSelectionListener;
import maspack.util.BufferUtilities;

/**
 * GLSelector that works using the traditional GL_SELECT mechanism (now
 * deprecated).
 */
public class GLColorSelector extends GLSelector {

   private static int ID_OFFSET = 0;
   private static int ID_STEP = 1;
   
   GL2GL3 myGl;

   int myTotalMaxQ;
   int myCurrentMaxQ;
   int myCurrentIdx;
   int myIdxBase;
   Deque<Integer> myMaxQStack;
   Deque<Integer> myIdxBaseStack;

   int myViewW;
   int myViewH;

   FrameBufferObject fbo = null;

   int[] savedViewport = new int[4];
   float[] savedBackgroundColor = new float[4];
   boolean savedBlend;
   boolean savedMulti;

   public GLColorSelector (GLViewer viewer) {
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

      GL2GL3 gl = (GL2GL3)(drawable.getGL ());
      myGl = gl;

      myTotalMaxQ = myViewer.numSelectionQueriesNeeded();
    
      myCurrentMaxQ = myTotalMaxQ;
      myCurrentIdx = -1;
      myIdxBase = 0;
      myMaxQStack.clear();     // paranoid ...
      myIdxBaseStack.clear();  // paranoid ...

      // restrict the viewport to the specified selection region
      savedViewport = myViewer.getViewport(gl);
 
      // Pick matrix
      //      myViewW = savedViewport[2];
      //      myViewH = savedViewport[3];
      myViewW = (int)Math.ceil (myRectW);
      myViewH = (int)Math.ceil (myRectH);
      myViewer.setPickMatrix(
         (float)(myRectX), 
         (float)(savedViewport[3] - myRectY), 
         (float)myRectW, (float)myRectH, savedViewport);
      myViewer.setViewport(gl, 0, 0, myViewW, myViewH);
      
      // disable lighting
      myViewer.setLightingEnabled(false);
      
      // zero-out clear color
      myViewer.getBackgroundColor(savedBackgroundColor);
      myViewer.setBackgroundColor(0, 0, 0, 0);
      
      savedBlend = gl.glIsEnabled(GL.GL_BLEND);
      if (savedBlend) {
         gl.glDisable(GL.GL_BLEND);
      }
      savedMulti = gl.glIsEnabled(GL.GL_MULTISAMPLE);
      if (savedMulti) {
         gl.glDisable(GL.GL_MULTISAMPLE);
      }
      
      // set the initial color to be 0
      myViewer.setColor(0, 0, 0, 0);

      //gl.glDisable (GL2.GL_TEXTURE_2D);
      //gl.glDisable (GL2.GL_FOG);
      
      // fcap = new GLFrameCapture(myViewW, myViewH, 1, new File("selection.png"), "png");
      fbo = new FrameBufferObject(myViewW, myViewH, 1);
      fbo.activate(gl);
   }

   public void processSelection (GLAutoDrawable drawable) {
      
      GL2GL3 gl = myGl;
      GLSupport.checkAndPrintGLError(gl);
      
      int w = myViewW;
      int h = myViewH;

      fbo.deactivate(gl);
      //      fcap.capture(gl);
      //      fcap = null;
      
      ByteBuffer pixels = fbo.getPixels(gl, GL.GL_RGBA);
      fbo.dispose(gl); // clean up FBO
      fbo = null;

      myViewer.setLightingEnabled(true);
      myViewer.setBackgroundColor(savedBackgroundColor[0],
         savedBackgroundColor[1], savedBackgroundColor[2], 
         savedBackgroundColor[3]);
      if (savedBlend) {
         gl.glEnable(GL.GL_BLEND);
      }
      if (savedMulti) {
         gl.glEnable(GL.GL_MULTISAMPLE);
      }
      
      myViewer.setViewport(gl, savedViewport[0], savedViewport[1], 
         savedViewport[2], savedViewport[3]);
      
      myViewer.clearPickMatrix();
      
      if (!myMaxQStack.isEmpty()) {
         throw new IllegalStateException (
            "Calls to begin/endSelectionForObject() not balanced");
      }
      
      HitRecord[] hits = null;

      boolean badIdWarningIssued = false;
      for (int i=0; i<w; i++) {
         for (int j=0; j<h; j++) {
            int r = 0xff & pixels.get();
            int g = 0xff & pixels.get();
            int b = 0xff & pixels.get();
            int a = 0xff & pixels.get();
            int lcolorId = a;
            lcolorId = 0;  // ignore alpha (currently returns 255)
            lcolorId <<= 8;
            lcolorId += b;
            lcolorId <<= 8;
            lcolorId += g;
            lcolorId <<= 8;
            lcolorId += r;
            int colorId = (int)(lcolorId & (0xFFFFFFFF));
            if (colorId != 0) {
               int id = colorId/ID_STEP-1-ID_OFFSET; // color id are incremented by 1
               if (id < 0 || id >= myTotalMaxQ) {
                  if (!badIdWarningIssued) {
                     System.out.printf (
"Warning: Color selection id 0x%x out of range; "+
"was GL_LIGHTING enabled or glColor() called during selection?\n", id);
                     badIdWarningIssued = true;
                  }
               }
               else {
                  if (hits == null) {
                     hits = new HitRecord[myTotalMaxQ];
                  }
                  HitRecord rec = hits[id];
                  if (rec == null) {
                     rec = new HitRecord (0);
                     hits[id] = rec;
                  }
                  rec.size++;
               }
            }
         }
      }
      BufferUtilities.freeDirectBuffer (pixels);
     
      myViewer.selectionEvent.setSelectedObjects (new LinkedList[0]);

      if (hits == null) {
         // then no queries were issued, so nothing to do ...
         myViewer.selectionEvent.setSelectedObjects (new LinkedList[0]);
      }
      else {
         int qid = 0;
         LinkedList<HitRecord> records = new LinkedList<HitRecord>();
         // int[] result = new int[1];
         Iterator<IsRenderable> it = myViewer.renderIterator();
         while (it.hasNext()) {
            IsRenderable r = it.next();
            if (r instanceof IsSelectable) {
               IsSelectable s = (IsSelectable)r;
               int numq = s.numSelectionQueriesNeeded();
               int nums = (numq >= 0 ? numq : 1);
               if (s.isSelectable()) {
                  for (int i=0; i<nums; i++) {
                     HitRecord rec;
                     if ((rec = hits[qid+i]) != null) {
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
               qid += nums;
            }
         }
         Collections.sort (records);
         LinkedList<Object>[] selObjs = new LinkedList[records.size()];
         for (int i=0; i<records.size(); i++) {
            selObjs[i] = records.get(i).objs;
         }
         myViewer.selectionEvent.setSelectedObjects (selObjs);         
      }
      
      ViewerSelectionListener[] listeners = myViewer.getSelectionListeners();
      for (int i=0; i<listeners.length; i++) {
         listeners[i].itemsSelected (myViewer.selectionEvent);
      }
      myViewer.repaint();
   }

   public void beginSelectionQuery (int idx) {
      if (myCurrentIdx != -1) {
         throw new IllegalStateException (
            "beginSelectionQuery() can't be called recursively");
      }
      if (idx < 0 || idx >= myCurrentMaxQ) {
         throw new IllegalArgumentException ("index out of range");
      }
      
      int colorId = (myIdxBase+idx+1+ID_OFFSET)*ID_STEP;
      int r = 0xff & colorId;
      colorId >>= 8;
      int g = 0xff & colorId;
      colorId >>= 8;
      int b = 0xff & colorId;
      colorId >>= 8;
      int a = 0xff & colorId;
      // a = 255;

      // XXX perhaps better to let viewer decide color?
      myViewer.setSelectingColor (r/255f, g/255f, b/255f, a/255f);
      myCurrentIdx = idx;
   }

   public void endSelectionQuery () {
      if (myCurrentIdx == -1) {
         throw new IllegalStateException (
            "endSelectionQuery() called without previous call to "+
            "beginSelectionQuery()");
      }
      myViewer.setSelectingColor(0f, 0f, 0f, 0f);
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
