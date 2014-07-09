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

import com.jogamp.common.nio.Buffers;

import maspack.matrix.Vector2d;
import maspack.util.IntHolder;
import maspack.util.InternalErrorException;

/**
 * GLSelector that works using the traditional (now deprecated) GL_SELECT
 * mechanism.
 */
public class GLSelectSelector extends GLSelector {

   GL2 myGl;

   int myTotalMaxQ;
   int myCurrentMaxQ;
   int myCurrentIdx;
   int myIdxBase;
   Deque<Integer> myMaxQStack;
   Deque<Integer> myIdxBaseStack;

   private final int selectBufferSize = 0x10000;
   IntBuffer selectBuffer;   

   public GLSelectSelector (GLViewer viewer) {
      super (viewer);
      myMaxQStack = new ArrayDeque<Integer>();
      myIdxBaseStack = new ArrayDeque<Integer>();
   }

   private class HitRecord implements Comparable<HitRecord> {
      long z1;
      long z2;
      LinkedList<Object> objs = new LinkedList<Object>();

      public int compareTo (HitRecord obj) {
         HitRecord rec = (HitRecord)obj;
         if (z1 > rec.z1) {
            return 1;
         }
         else if (z1 < rec.z1) {
            return -1;
         }
         else if (z2 > rec.z2) {
            return 1;
         } else if (z2 < rec.z2) {
            return -1;
         } else {
            return 0;
         }
      }
   }

   public void setupSelection (GLAutoDrawable drawable) {

      GL2 gl = myViewer.getGL().getGL2();
      GLU glu = myViewer.getGLU();

      myGl = gl;

      myTotalMaxQ = myViewer.numSelectionQueriesNeeded();

      myCurrentMaxQ = myTotalMaxQ;
      myCurrentIdx = -1;
      myIdxBase = 0;
      myMaxQStack.clear(); // paranoid ...
      myIdxBaseStack.clear();  // paranoid ...

      selectBuffer = Buffers.newDirectIntBuffer(selectBufferSize);
      gl.glSelectBuffer (selectBufferSize, selectBuffer);
      gl.glRenderMode (GL2.GL_SELECT);
      int[] viewport = new int[4];
      gl.glGetIntegerv (GL2.GL_VIEWPORT, viewport, 0);
      gl.glMatrixMode (GL2.GL_PROJECTION);
      gl.glPushMatrix();
      gl.glLoadIdentity();

      glu.gluPickMatrix (
         myRectX, viewport[3] - myRectY, myRectW, myRectH, viewport, 0);

      Vector2d zRange = new Vector2d();
      myViewer.getZRange(zRange);
      myViewer.setViewVolume(/*near=*/zRange.x, /*far=*/zRange.y);

      // setViewVolume (width, height);
      gl.glMatrixMode (GL2.GL_MODELVIEW);

      gl.glDisable (GL2.GL_LIGHTING);

      gl.glInitNames();
      gl.glPushName (-1);

   }

   public void processSelection (GLAutoDrawable drawable) {

      GL2 gl = myViewer.getGL().getGL2();
      gl.glEnable (GL2.GL_LIGHTING);

      int hits = gl.glRenderMode (GL2.GL_RENDER);
      gl.glMatrixMode (GL2.GL_PROJECTION);
      gl.glPopMatrix();
      gl.glMatrixMode (GL2.GL_MODELVIEW);

      if (!myMaxQStack.isEmpty()) {
         throw new IllegalStateException (
            "Calls to begin/endSelectionForObject() not balanced");
      }

      if (hits > 0) {
         myViewer.selectionEvent.mySelectedObjects =
            processHits (hits, selectBuffer);
      }
      else {
         myViewer.selectionEvent.mySelectedObjects =
            new LinkedList[0];
      }

      GLSelectionListener[] listeners = myViewer.getSelectionListeners();
      for (int i=0; i<listeners.length; i++) {
         listeners[i].itemsSelected (myViewer.selectionEvent);
      }
      myViewer.getCanvas().repaint();
   }

   String toString (int[] array) {
      String str = "[ ";
      for (int i=0; i<array.length; i++) {
         str += (array[i] + " ");
      }
      str += "]";
      return str;
   }

   private int[] getHitRecordFromBuffer (
      HitRecord rec, IntBuffer buf, IntHolder offset) {

      int off = offset.value;
      int numNames = buf.get(off++);
      int[] names = new int[numNames];
      rec.z1 = (buf.get(off++) & 0xffffffffL);
      rec.z2 = (buf.get(off++) & 0xffffffffL);
      for (int k = 0; k < numNames; k++) {
         names[k] = buf.get(off++);
         //System.out.print (" " + names[k]);
      }      
      offset.value = off;
      return names;
   }

//   @SuppressWarnings("unchecked")
//   LinkedList<GLRenderable>[] processHitsOld (int hits, IntBuffer buf) {
//
//      ArrayList<HitRecord> records = new ArrayList<HitRecord> (hits);
//      IntHolder offset = new IntHolder();
//
//      // int numSelectables = selectables.size();
//      for (int i = 0; i < hits; i++) {
//         HitRecord rec = new HitRecord();
//         int[] names = getHitRecordFromBuffer (rec, buf, offset);
//         int id = names[0];
//         if (id >= myViewer.numRenderables()) {
//            System.out.println ("warning: unknown id " + names[0]);
//         }
//         else if (id >= 0) {
//            GLSelectable selObj = (GLSelectable)myViewer.getSelected (id);
//            System.out.println ("names=" + toString(names));
//            System.out.println ("selObj=" + selObj);
//            selObj.handleSelection (rec.objs, names, 1);
//            // if (selObj instanceof Dragger3d) {
//            //    System.out.println ("dragger");
//            //    if (myViewer.myDraggerSelectionEvent == null) {
//            //       throw new IllegalStateException (
//            //          "myDraggerSelectionEvent should be set");
//            //    }
//            //    Dragger3d dragger = (Dragger3d)selObj;
//            //    if (dragger.isSelected()) {
//            //       dragger.draggerSelected (myViewer.myDraggerSelectionEvent);
//            //    }
//            // }
//            if (rec.objs.size() > 0) {
//               records.add (rec);
//            }
//         }
//      }
//      Collections.sort (records);
//      LinkedList<GLRenderable>[] selObjs = new LinkedList[records.size()];
//      for (int i=0; i<records.size(); i++) {
//         selObjs[i] = records.get(i).objs;
//      }
//      return selObjs;
//   }

   @SuppressWarnings("unchecked")
   LinkedList<Object>[] processHits (int numHits, IntBuffer buf) {

      ArrayList<HitRecord> records = new ArrayList<HitRecord> (numHits);
      IntHolder offset = new IntHolder();

      Iterator<GLRenderable> it = myViewer.renderIterator();
      GLSelectable selObj = null;
      GLSelectable lastSelObj = null;
      int objId = -1;

      it = myViewer.renderIterator();
      // int numSelectables = selectables.size();
      for (int i = 0; i < numHits; i++) {
         HitRecord rec = new HitRecord();
         int[] names = getHitRecordFromBuffer (rec, buf, offset);
         int id = names[0];
         if (id >= myTotalMaxQ) {
            System.out.println ("warning: unknown id " + names[0]);
         }
         else if (id >= 0) {
            //System.out.println ("names=" + toString(names));
            if (objId < id) {
               // advance 
               while (it.hasNext()) {
                  GLRenderable r = it.next();
                  if (r instanceof GLSelectable) {
                     if (selObj != null) {
                        int numq = selObj.numSelectionQueriesNeeded();
                        objId += (numq >= 0 ? numq : 1);
                     }
                     else {
                        objId += 1;
                     }
                     selObj = (GLSelectable)r;
                  }
                  if (objId >= id) {
                     break;
                  }
               }
            }
            if (objId != id) {
               throw new InternalErrorException (
                  "Selected object not found for id=" + id);
            }
            //GLSelectable selObj = (GLSelectable)myViewer.getSelected (id);
            //System.out.println ("selObj=" + selObj);
            if (selObj.numSelectionQueriesNeeded() == -1) {
               rec.objs.add (selObj);
            }
            else {
               selObj.getSelection (rec.objs, names[names.length-1]-objId);
               //System.out.println ("getSelection=" + rec.objs.getLast());
            }
            if (rec.objs.size() > 0) {
               records.add (rec);
            }
         }
      }
      Collections.sort (records);
      LinkedList<Object>[] selObjs = new LinkedList[records.size()];
      for (int i=0; i<records.size(); i++) {
         selObjs[i] = records.get(i).objs;
      }
      return selObjs;
   }

   public void beginSelectionQuery (int idx) {
      if (myCurrentIdx != -1) {
         throw new IllegalStateException (
            "beginSelectionQuery() can't be called recursively");
      }
      // if (idx < 0 || idx >= myCurrentMaxQ) {
      //    throw new IllegalArgumentException ("index out of range");
      // }
      //myGl.glLoadName (idx);
      myGl.glLoadName (myIdxBase+idx);
      myCurrentIdx = idx;
   }

   public void endSelectionQuery () {
      if (myCurrentIdx == -1) {
         throw new IllegalStateException (
            "endSelectionQuery() called without previous call to "+
            "beginSelectionQuery()");
      }
      myGl.glLoadName (-1);
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
      myGl.glLoadName (idx);
      myGl.glPushName (-1);      
   }

   public void endSelectionForObject () {
      if (myCurrentIdx != -1) {
         throw new IllegalStateException (
            "missing call to endSelectionQuery()");
      }
      myCurrentMaxQ = myMaxQStack.removeFirst();
      myIdxBase = myIdxBaseStack.removeFirst();
      myGl.glPopName ();      
      myGl.glLoadName (-1);
   }   

   // public void beginSelectionQuery (int idx) {
   //    myGl.glLoadName (idx);
   // }

   // public void endSelectionQuery () {
   //    myGl.glLoadName (-1);
   // }

   // public void beginSelectionForObject (GLSelectable s, int idx) {
   //    myGl.glLoadName (idx);
   //    myGl.glPushName (-1);
   // }

   // public void endSelectionForObject () {
   //    myGl.glPopName();
   //    myGl.glLoadName (-1);
   // }
   
}
