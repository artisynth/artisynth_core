/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.glu.GLU;

import maspack.matrix.Vector2d;

/**
 * GLSelector that works using the traditional GL_SELECT mechanism (now
 * deprecated).
 */
public class GLColorSelector extends GLSelector {

   GL2 myGl;

   int myTotalMaxQ;
   int myCurrentMaxQ;
   int myCurrentIdx;
   int myIdxBase;
   Deque<Integer> myMaxQStack;
   Deque<Integer> myIdxBaseStack;

   int myViewW;
   int myViewH;

   int[] frameBufferId = new int[1];
   int[] renderBufferId = new int[1];
   int[] depthBufferId = new int[1];
   
   FrameBufferObject fbo = null;
   
   boolean savedBlend;
   boolean savedMulti;
   
   int[] mySavedViewport = new int[4];

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

      // restrict the viewport to the specified selection region
      
      gl.glGetIntegerv (GL2.GL_VIEWPORT, mySavedViewport, 0);
      gl.glMatrixMode (GL2.GL_PROJECTION);
      gl.glPushMatrix();
      gl.glLoadIdentity();

      myViewW = (int)Math.ceil (myRectW);
      myViewH = (int)Math.ceil (myRectH);

      int[] viewport =
         //new int[] {0, 0, (int)Math.ceil(myRectW), (int)Math.ceil(myRectH) };
         new int[] {0, 0, myViewW, myViewH };

      glu.gluPickMatrix (
         myRectX, mySavedViewport[3] - myRectY, myRectW, myRectH,
         mySavedViewport, 0);
      gl.glViewport (viewport[0], viewport[1], viewport[2], viewport[3]);

      Vector2d zRange = new Vector2d();
      myViewer.getZRange(zRange);
      myViewer.setViewVolume(/*near=*/zRange.x, /*far=*/zRange.y);
      // setViewVolume (width, height);
      gl.glMatrixMode (GL2.GL_MODELVIEW);

      gl.glDisable (GL2.GL_LIGHTING);
      // set the initial color to be 0
      gl.glColor4f (0, 0, 0, 0);

      //gl.glDisable (GL2.GL_TEXTURE_2D);
      //gl.glDisable (GL2.GL_FOG);

      // create offscreen buffer in which to render
      fbo = new FrameBufferObject(0, 0, myViewW, myViewH, 1, null, null, gl);
      fbo.setupFBO();
      fbo.activate();
            
      // disable blending and multisample
      savedBlend = gl.glIsEnabled(GL.GL_BLEND);
      if (savedBlend) {
         gl.glDisable(GL.GL_BLEND);
      }
      savedMulti = gl.glIsEnabled(GL.GL_MULTISAMPLE);
      if (savedMulti) {
         gl.glDisable(GL.GL_MULTISAMPLE);
      }
   }

   public void processSelection (GLAutoDrawable drawable) {

      GL2 gl = myGl;

      //gl.glEnable (GL2.GL_FOG);
      //gl.glEnable (GL2.GL_TEXTURE_2D);
      gl.glEnable (GL2.GL_LIGHTING);
      if (savedBlend) {
         gl.glEnable(GL.GL_BLEND);
      }
      if (savedMulti) {
         gl.glEnable(GL.GL_MULTISAMPLE);
      }

      if (!myMaxQStack.isEmpty()) {
         throw new IllegalStateException (
            "Calls to begin/endSelectionForObject() not balanced");
      }
      
      int w = myViewW;
      int h = myViewH;

      ByteBuffer pixels = ByteBuffer.allocateDirect(4*w*h);
      pixels.order(ByteOrder.nativeOrder());
      fbo.getPixelsRGBA(pixels);
      fbo.deactivate();
      fbo.cleanup();
      fbo = null;

      HitRecord[] hits = null;

      boolean badIdWarningIssued = false;
      for (int i=0; i<w; i++) {
         for (int j=0; j<h; j++) {
            int r = 0xff & pixels.get();
            int g = 0xff & pixels.get();
            int b = 0xff & pixels.get();
            int a = 0xff & pixels.get();
            int colorId = r;
            colorId <<= 8;
            colorId += g;
            colorId <<= 8;
            colorId += b;
            // colorId <<= 8;
            // colorId += a;
            if (colorId != 0) {
               int id = colorId-1; // color id are incremented by 1
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
      pixels.clear();
      
      // XXX dummy multi-sample FBO hack for some Mac graphics drivers
      if (myViewer.isMultiSampleEnabled()) {
         
         FrameBufferObject dummy = new FrameBufferObject(0, 0, myViewer.getWidth(), myViewer.getHeight(),
            8, null, null, gl);
         dummy.setupFBO();
         dummy.activate();

         // now de-activate and delete immediately
         dummy.deactivate();
         dummy.cleanup();
         
      }
      

      // restore view
      gl.glViewport (mySavedViewport[0], mySavedViewport[1], 
                     mySavedViewport[2], mySavedViewport[3]);

      gl.glMatrixMode (GL2.GL_PROJECTION);
      gl.glPopMatrix();
      gl.glMatrixMode (GL2.GL_MODELVIEW);

      myViewer.selectionEvent.mySelectedObjects = new LinkedList[0];

      if (hits == null) {
         // then no queries were issued, so nothing to do ...
         myViewer.selectionEvent.mySelectedObjects = new LinkedList[0];
      }
      else {
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
         myViewer.selectionEvent.mySelectedObjects = selObjs;         
      }
      
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
      int colorId = myIdxBase+idx+1;
      // int a = 0xff & colorId;
      // colorId >>= 8;
      int b = 0xff & colorId;
      colorId >>= 8;
      int g = 0xff & colorId;
      colorId >>= 8;
      int r = 0xff & colorId;
      //myGl.glColor4f (r/255f, g/255f, b/255f, a/255f);
      myViewer.setGLColor(myGl, r/255f, g/255f, b/255f, 1);
      myCurrentIdx = idx;
   }

   public void endSelectionQuery () {
      if (myCurrentIdx == -1) {
         throw new IllegalStateException (
            "endSelectionQuery() called without previous call to "+
            "beginSelectionQuery()");
      }
      // myGl.glColor4f (0f, 0f, 0f, 1f);
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
