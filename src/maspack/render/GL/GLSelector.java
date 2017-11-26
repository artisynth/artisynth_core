/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render.GL;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedList;

import com.jogamp.opengl.GL;

import maspack.matrix.Vector2d;
import maspack.render.IsSelectable;
import maspack.render.ViewerSelectionListener;

/**
 * Base class for helper classes that enable GLViewer to perform selection
 * operations.
 */
public abstract class GLSelector extends GLResourceBase {

   protected double myRectX;
   protected double myRectY;
   protected double myRectW;
   protected double myRectH;

   protected GLViewer myViewer;
   
   protected FrameBufferObject fbo;
   protected FrameBufferObject hackFbo;  // for restoring multisampling on some machines

   GL myGl;

   int myTotalMaxQ;
   int myCurrentMaxQ;
   int myCurrentIdx;
   int myIdxBase;
   Deque<Integer> myMaxQStack;
   Deque<Integer> myIdxBaseStack;
   
   int myViewW;
   int myViewH;

   int[] mySavedViewport = new int[4];
   float[] savedBackgroundColor = new float[4];
   float[] savedColor = new float[4];
   boolean savedBlend;
   boolean savedMultisampled;
   boolean savedResize;
   boolean savedViewport;
   boolean savedLighting;
   
   protected static class HitRecord implements Comparable<HitRecord> {
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
   
   public GLSelector (GLViewer viewer) {
      myViewer = viewer;
      myMaxQStack = new ArrayDeque<Integer>();
      myIdxBaseStack = new ArrayDeque<Integer>();
   }

   public void setRectangle (double x, double y, double w, double h) {
      myRectX = x;
      myRectY = y;
      myRectW = w;
      myRectH = h;
   }

   public void setupSelection (GL gl) {
      
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

      // restrict the viewport to the specified selection region
      mySavedViewport = myViewer.getViewport(gl);
      
      // apply pick
      int myViewW = mySavedViewport[2];
      int myViewH = mySavedViewport[3];
      
      myViewW = (int)Math.ceil (myRectW);
      myViewH = (int)Math.ceil (myRectH);

      float x = (float)myRectX;
      float y = (float)(mySavedViewport[3] - myRectY);
      myViewer.setPickMatrix(x, y,
         (float)myRectW, (float)myRectH, mySavedViewport);
      
      Vector2d zRange = new Vector2d();
      myViewer.getZRange(zRange);
      myViewer.pushProjectionMatrix();
      myViewer.setViewVolume(/*near=*/zRange.x, /*far=*/zRange.y);
      
      savedResize = myViewer.isAutoResizeEnabled ();
      myViewer.setAutoResizeEnabled (false);
      savedViewport = myViewer.isAutoViewportEnabled ();
      myViewer.setAutoViewportEnabled (false);
      
      myViewer.getBackgroundColor(savedBackgroundColor);
      myViewer.setBackgroundColor(0, 0, 0, 0);
      
      savedBlend = myViewer.isBlendingEnabled ();
      if (savedBlend) {
         myViewer.setBlendingEnabled (false);
      }
      savedMultisampled = myViewer.isMultiSampleEnabled ();
      if (savedMultisampled) {
         myViewer.setMultiSampleEnabled (false);
      }
      
      // set the initial color to be 0
      myViewer.getFrontColor (savedColor);
      
      activateFBO (gl, myViewW, myViewH);
      myViewer.setViewport(gl, 0, 0, myViewW, myViewH);

      // disable lighting
      savedLighting = myViewer.setLightingEnabled(false);

   }

   public void processSelection (GL gl) {
      
      if (myTotalMaxQ == 0) {
         myViewer.getCanvas().repaint();
         return;
      }

      myViewer.setBackgroundColor(savedBackgroundColor[0],
         savedBackgroundColor[1], savedBackgroundColor[2], 
         savedBackgroundColor[3]);
      
      myViewer.setBlendingEnabled (savedBlend);
      myViewer.setMultiSampleEnabled (savedMultisampled);
      myViewer.setColor (savedColor);
      myViewer.setLightingEnabled(savedLighting);
      myViewer.setMultiSampleEnabled (savedMultisampled);
      
      deactivateFBO (gl);
      
      myViewer.setViewport(gl, mySavedViewport[0], mySavedViewport[1], 
         mySavedViewport[2], mySavedViewport[3]);
      myViewer.setAutoResizeEnabled (savedResize);
      myViewer.setAutoViewportEnabled (savedViewport);

      myViewer.popProjectionMatrix();
      myViewer.clearPickMatrix();
      
      if (!myMaxQStack.isEmpty()) {
         throw new IllegalStateException (
            "Calls to begin/endSelectionForObject() not balanced");
      }
      
      ViewerSelectionListener[] listeners = myViewer.getSelectionListeners();
      for (int i=0; i<listeners.length; i++) {
         listeners[i].itemsSelected (myViewer.getSelectionEvent());
      }
      myViewer.getCanvas().repaint();
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
   
   
   public abstract void beginSelectionQuery (int idx);

   public abstract void endSelectionQuery ();

   public FrameBufferObject getFBO() {
      return fbo;
   }
   
   public void activateFBO(GL gl, int width, int height) {
      if (fbo == null) {
         fbo = new FrameBufferObject (width, height, 1);
      } else {
         fbo.configure (gl, width, height, 1);
      }
      fbo.activate (gl);
   }
   
   public void deactivateFBO(GL gl) {
      fbo.deactivate (gl);
      // use hack FBO to restore multisampling
      if (hackFbo == null) {
         hackFbo = new FrameBufferObject (myViewer.getScreenWidth (), myViewer.getScreenHeight (), 
            myViewer.numMultiSamples ());
      } else {
         hackFbo.configure (gl, myViewer.getScreenWidth (), myViewer.getScreenHeight (), 
            myViewer.numMultiSamples ());
      }
      hackFbo.activate (gl);
      hackFbo.deactivate (gl);
   }
   
   @Override
   public GLSelector acquire () {
      return (GLSelector)super.acquire ();
   }
   
   @Override
   public boolean isDisposed () {
      return (fbo == null);
   }
   
   /**
    * Clean up any resources
    * @param gl context
    */
   public void dispose(GL gl) {
      if (fbo != null) {
         fbo.dispose (gl);
         fbo = null;
      }
      if (hackFbo != null) {
         hackFbo.dispose (gl);
         hackFbo = null;
      }
   }
}

