/**
 * Copyright (c) 2017, by the Authors: John E Lloyd (UBC). Elliptic selection
 * added by Doga Tekin (ETH).
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render.GL;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

import com.jogamp.opengl.GL;

import maspack.render.IsRenderable;
import maspack.render.IsSelectable;
import maspack.util.BufferUtilities;
import maspack.matrix.Vector2d;

/**
 * GLSelector that works using the traditional GL_SELECT mechanism (now
 * deprecated).
 */
public class GLColorSelector extends GLSelector {
   
   private static int ID_OFFSET = 0x00000001;
   private static int ID_STEP = 1;

   public static final int MAX_COLOR_QUERIES = 1<<24;  // r,g,b

   int[] myQueryBuffer;
   int myQueryCount;
   int myQueryMax;
   int myQueryTotal;
   int myQueryBase;

   public GLColorSelector (GLViewer viewer) {
      super (viewer);
   }

   @Override
   public void setupSelection (GL gl) {
      
      super.setupSelection (gl);
      if (myTotalMaxQ == 0) {
         return;
      }
      
      myQueryBuffer = new int[fbo.getWidth ()*fbo.getHeight ()];
      myQueryCount = 0;
      myQueryMax = Math.min (myTotalMaxQ, MAX_COLOR_QUERIES);
      myQueryBase = 0;
      myQueryTotal = 0;
   }

   private static final double sqr (double x) {
      return x*x;
   }
   
   private void flushQueries(GL gl) {

      if (myQueryCount == 0) {
         return;
      }

      // check if any valid ids
      gl.glFlush ();
      ByteBuffer pixels = fbo.getPixels(myGl, GL.GL_RGBA);
      int w = fbo.getWidth ();
      int h = fbo.getHeight ();
            
      double centerX = (w-1.0)/2.0;
      double centerY = (h-1.0)/2.0;
      
      boolean badIdWarningIssued = false;
      int idx = 0;
      for (int i=0; i<h; i++) {
         for (int j=0; j<w; j++) {
            int r = 0xff & pixels.get();
            int g = 0xff & pixels.get();
            int b = 0xff & pixels.get();
            int a = 0xff & pixels.get();
            int lcolorId = a;
            lcolorId <<= 8;
            lcolorId += b;
            lcolorId <<= 8;
            lcolorId += g;
            lcolorId <<= 8;
            lcolorId += r;
            int colorId = (int)(lcolorId & (0xFFFFFFFF));

            Vector2d ellipticSize = myViewer.getEllipticCursorSize();
            
            if (myViewer.getEllipticSelection()) {
               double x = (h-i-1) - centerX;
               double y = j - centerY;
               if (sqr(x/ellipticSize.x) + sqr(y/ellipticSize.y) > 1) {
                  colorId = 0;
               }
            }
            
            if (colorId != 0) {
               int id = colorId/ID_STEP-ID_OFFSET+ myQueryBase; // color id are incremented by 1
               if (id < 0 || id > myTotalMaxQ) {
                  if (!badIdWarningIssued) {
                     System.out.printf (
                        "Warning: Color selection id 0x%x out of range; "+
                        "was GL_LIGHTING enabled or glColor() called during selection?\n", id);
                     badIdWarningIssued = true;
                  }
               } else {
                  myQueryBuffer[idx] = id;
               }
            }
            ++idx;
         }
      }
      BufferUtilities.freeDirectBuffer (pixels);
      
      // clear color FBO without writing depth
      gl.glClear (GL.GL_COLOR_BUFFER_BIT);
      
      myQueryBase += myQueryCount;
      myQueryTotal += myQueryCount;
      myQueryCount = 0;

   }

   @Override
   public void processSelection (GL gl) {

      if (myTotalMaxQ == 0) {
         super.processSelection(gl);
         return;
      }
      
      flushQueries (myGl);

      HitRecord[] hits = null;

      for (int id : myQueryBuffer) {
         if (id != 0) {
            --id;  // subtract the 1 offset
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
       
      myViewer.selectionEvent.setSelectedObjects (null);

      if (hits == null) {
         // then no queries were issued, so nothing to do ...
         myViewer.selectionEvent.setSelectedObjects (null);
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
         ArrayList<LinkedList<?>> selObjs = new ArrayList<>(records.size());
         for (int i=0; i<records.size(); i++) {
            selObjs.add (records.get(i).objs);
         }
         myViewer.selectionEvent.setSelectedObjects (selObjs);         
      }

      super.processSelection (gl);
   }

   public void beginSelectionQuery (int idx) {
      if (myCurrentIdx != -1) {
         throw new IllegalStateException (
         "beginSelectionQuery() can't be called recursively");
      }
      if (idx < 0 || idx >= myCurrentMaxQ) {
         throw new IllegalArgumentException (
            "index "+idx+" out of range; max=" + (myCurrentMaxQ-1));
      }

      // flush queries if we need room
      if (myQueryCount == myQueryMax) {
         flushQueries (myGl);
      }
      
      int colorId = (myIdxBase+idx+ID_OFFSET+1-myQueryBase)*ID_STEP; //
      
      int r = 0xff & colorId;
      colorId >>= 8;
      int g = 0xff & colorId;
      colorId >>= 8;
      int b = 0xff & colorId;
      colorId >>= 8;
      int a = 0xff & colorId;

      // XXX perhaps better to let viewer decide color?
      myViewer.setSelectingColor (r/255f, g/255f, b/255f, a/255f);
      myCurrentIdx = idx;
      ++myQueryCount;
   }

   public void endSelectionQuery () {
      if (myCurrentIdx == -1) {
         throw new IllegalStateException (
            "endSelectionQuery() called without previous call to "+
         "beginSelectionQuery()");
      }
      myCurrentIdx = -1;
   }


}
