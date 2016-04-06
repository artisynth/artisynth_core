/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC), Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

import java.util.ArrayList;
import java.util.Collection;

import maspack.matrix.Point3d;
import maspack.render.GL.GLRenderable;
import maspack.render.GL.GLSelectable;

/**
 * Maintains a list of renderable objects for use by GLViewer.
 */
public class RenderList {

   protected SortedRenderableList myOpaque = new SortedRenderableList();
   protected SortedRenderableList myTransparent =  new SortedRenderableList();
   protected SortedRenderableList myOpaque2d = new SortedRenderableList();
   protected SortedRenderableList myTransparent2d = new SortedRenderableList();

   // renderables that were originally added to this list, before expansion
   private ArrayList<GLRenderable> myUnexpanded =
      new ArrayList<GLRenderable>();

   private int myLevel = 0;

   public <C extends GLRenderable> boolean addIfVisible (
      C r) {
      boolean add = true;
      if (r instanceof HasRenderProps) {
         RenderProps props = ((HasRenderProps)r).getRenderProps();
         if (props == null || !props.isVisible()) {
            add = false;
         }
      }
      // myLevel++;
      // r.prerender (this, 0);
      // if (--myLevel == 0) {
      //    myUnexpanded.add (r);
      // }
      if (add) {
         if ( (r.getRenderHints() & GLRenderable.TWO_DIMENSIONAL) != 0) {
            if ((r.getRenderHints() & GLRenderable.TRANSLUCENT) != 0) {
               insertRenderable(r, myTransparent2d);
            }
            else {
               insertRenderable(r, myOpaque2d);
            }
         } else {
            if ((r.getRenderHints() & GLRenderable.TRANSLUCENT) != 0) {
               insertRenderable(r, myTransparent);
            }
            else {
               insertRenderable(r, myOpaque);
            }
         }
      }
      myLevel++;
      r.prerender (this); // prerenderFlags);
      if (--myLevel == 0) {
         myUnexpanded.add (r);
      }
      return add;
   }
   
//   public <C extends GLRenderableExtended> boolean addIfVisible (
//      C r, int prerenderFlags) {
//      boolean add = true;
//      if (r instanceof HasRenderProps) {
//         RenderProps props = ((HasRenderProps)r).getRenderProps();
//         if (props == null || !props.isVisible()) {
//            add = false;
//         }
//      }
//      if (add) {
//         
//         if ((r.getRenderHints() & GLRenderable.TWO_DIMENSIONAL) != 0) {
//            if ((r.getRenderHints() & GLRenderable.TRANSLUCENT) != 0) {
//               insertRenderable(r, myTransparent2d);
//            }
//            else {
//               insertRenderable(r, myOpaque2d);
//            }
//         } else {
//            if ((r.getRenderHints() & GLRenderable.TRANSLUCENT) != 0) {
//               insertRenderable(r, myTransparent);
//            }
//            else {
//               insertRenderable(r, myOpaque);
//            }
//         }
//      }
//      myLevel++;
//      r.prerenderx (this, prerenderFlags);
//      if (--myLevel == 0) {
//         myUnexpanded.add (r);
//      }
//      return add;
//   }
   
   private static int getZOrderKey (GLRenderable r) {
      if (r instanceof HasRenderProps) {
         RenderProps props = ((HasRenderProps)r).getRenderProps();
         if (props != null) {
            return props.getZOrder();
         }
      }
      return 0;
   }
   
   private void insertRenderable(GLRenderable r, SortedRenderableList list) {
      // maintain order for equal indices
      int ri = getZOrderKey(r);
      list.add(r, ri);
   }

   public void addIfVisibleAll (Collection<? extends GLRenderable> renderables) {
      for (GLRenderable r : renderables) {
         addIfVisible (r);
      }
   }
   
//   public void addIfVisibleAll (
//      Collection<? extends GLRenderable> renderables, int prerenderFlags) {
//      for (GLRenderable r : renderables) {
//         if (r instanceof GLRenderableExtended) {
//            addIfVisible ((GLRenderableExtended)r, prerenderFlags);
//         } else {
//            addIfVisible (r);
//         }
//      }
//   }

   // /**
   // * Invoke prerender on every renderable initially contained in this list.
   // * While this may cause additional renderables to be recursively added to
   // * the list, prerender will not be invoked on these by this method.
   // */
   // public void prerender()
   // {
   // LinkedList<GLRenderable> copy = (LinkedList<GLRenderable>)myList.clone();
   // for (GLRenderable r : copy)
   // { r.prerender (this);
   // }
   // }

   // public int getTransparent (GLRenderable[] array, int idx)
   // {
   // for (int i=0; i<myTransparent.size(); i++)
   // { array[idx++] = myTransparent.get(i);
   // }
   // return idx;
   // }

   // public int getOpaque (GLRenderable[] array, int idx)
   // {
   // for (int i=0; i<myOpaque.size(); i++)
   // { array[idx++] = myOpaque.get(i);
   // }
   // return idx;
   // }

   public GLRenderable getOpaque (int i) {
      return myOpaque.get (i);
   }

   public SortedRenderableList getOpaque() {
      return myOpaque;
   }
   
   public GLRenderable getTransparent (int i) {
      return myTransparent.get (i);
   }
   
   public SortedRenderableList getTransparent() {
      return myTransparent;
   }
   
   public GLRenderable getOpaque2d(int i) {
      return myOpaque2d.get(i);
   }
   
   public SortedRenderableList getOpaque2d() {
      return myOpaque2d;
   }
   
   public GLRenderable getTransparent2d(int i) {
      return myTransparent2d.get(i);
   }
   public SortedRenderableList getTransparent2d() {
      return myTransparent2d;
   }

   // public int get (GLRenderable[] array, int idx)
   // {
   // idx = getOpaque (array, idx);
   // idx = getTransparent (array, idx);
   // return idx;
   // }

   public void clear() {
      myTransparent.clear();
      myOpaque.clear();
      myTransparent2d.clear();
      myOpaque2d.clear();
      myUnexpanded.clear();
   }

   // public Iterator<GLRenderable> iterator()
   // {
   // return myList.iterator();
   // }

   // public ListIterator<GLRenderable> listIterator()
   // {
   // return myList.listIterator();
   // }

   // public ListIterator<GLRenderable> listIterator(int idx)
   // {
   // return myList.listIterator(idx);
   // }

   public int numTransparent() {
      return myTransparent.size();
   }

   public int numOpaque() {
      return myOpaque.size();
   }
   
   public int numTransparent2d() {
      return myTransparent2d.size();
   }
   
   public int numOpaque2d() {
      return myOpaque2d.size();
   }

   public int numTransparentSelQueries() {
      return myTransparent.numSelectionQueriesNeeded();
   }

   public int numOpaqueSelQueries() {
      return myOpaque.numSelectionQueriesNeeded();
   }

   public int numTransparent2dSelQueries() {
      return myTransparent2d.numSelectionQueriesNeeded();
   }

   public int numOpaque2dSelQueries() {
      return myOpaque2d.numSelectionQueriesNeeded();
   }

   public int numSelectionQueriesNeeded() {
      return (myTransparent.numSelectionQueriesNeeded() +
              myOpaque.numSelectionQueriesNeeded() +
              myTransparent2d.numSelectionQueriesNeeded() +
              myOpaque2d.numSelectionQueriesNeeded());
   }         

   public int size() {
      return myTransparent.size() + myOpaque.size() 
         + myTransparent2d.size() + myOpaque2d.size();
   }

   public void updateBounds (Point3d pmin, Point3d pmax) {
      for (int i = 0; i < myUnexpanded.size(); i++) {
         myUnexpanded.get (i).updateBounds (pmin, pmax);
      }
   }

   private int renderList (
      Renderer renderer, SortedRenderableList list, int qid, int flags) {

      boolean selecting = renderer.isSelecting();

      for (int i = 0; i < list.size(); i++) {
         GLRenderable r = list.get (i);
         if (selecting && r instanceof GLSelectable) {
            GLSelectable s = (GLSelectable)r;

            int numq = s.numSelectionQueriesNeeded();
            if (renderer.isSelectable(s)) {
               if (numq >= 0) {
                  renderer.beginSubSelection (s, qid);
               }
               else {
                  renderer.beginSelectionQuery (qid);
               }
               r.render (renderer, flags);
               if (numq >= 0) {
                  renderer.endSubSelection ();
               }
               else {
                  renderer.endSelectionQuery ();
               }
            }
            qid += (numq >= 0 ? numq : 1);
         } else if (selecting) {
            // don't render if not selectable (saves mucho time when lots of
            // non-selectable renderables, such as text labels)
         } 
         else {
            r.render (renderer, flags);
         }
         // XXX maybe disable highlighting?
         renderer.setSelectionHighlighting (false);
      }
      return qid;
   }

   public int renderOpaque (Renderer renderer, int qid, int flags) {
      return renderList (renderer, myOpaque, qid, flags);
   }

   public int renderTransparent (Renderer renderer, int qid, int flags) {
      return renderList (renderer, myTransparent, qid, flags);
   }
   
   public int renderOpaque2d (Renderer renderer, int qid, int flags) {
      return renderList (renderer, myOpaque2d, qid, flags);
   }

   public int renderTransparent2d (Renderer renderer, int qid, int flags) {
      return renderList (renderer, myTransparent2d, qid, flags);
   }

   public GLRenderable get (int idx) {
      int nextList = myOpaque.size();
      if (idx < nextList) {
         return myOpaque.get (idx);
      }
      idx = idx - nextList;
      nextList = myTransparent.size();
      if (idx < nextList) {
         return myTransparent.get(idx);
      }
      idx = idx-nextList;
      nextList = myOpaque2d.size();
      if (idx < nextList) {
         return myOpaque2d.get(idx);
      }
      idx = idx-nextList;
      return myTransparent2d.get (idx);
   }

}
