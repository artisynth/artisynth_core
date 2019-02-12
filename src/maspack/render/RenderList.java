/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC), Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

import java.util.ArrayList;
import java.util.Collection;

import maspack.matrix.Vector3d;

//import maspack.render.GL.*;

/**
 * Maintains a list of renderable objects for use by GLViewer. As renderables
 * are added to this list, they are sorted into four sublists depending on
 * whether they are <i>opaque</i>, <i>transparent</i>, <i>2d opaque</i>, and
 * <i>2d transparent</i>. As renderables are added to the list, their {@link
 * IsRenderable#prerender} method is also called.
 * 
 * <p>The sublist to which each renderable is added is determined based on
 * whether the flags returned by the its {@link IsRenderable#getRenderHints()}
 * method contains the settings {@link IsRenderable#TRANSPARENT} and {@link
 * IsRenderable#TWO_DIMENSIONAL}. Renderables are also added to their
 * appropriate sublist in increasing order according to a <i>z-order</i> value,
 * where z denotes the viewing axis and a higher z-order indicates an object
 * which is ``closer to the eye''. If the renderable is an instance of {@link
 * HasRenderProps} and its {@link HasRenderProps#getRenderProps} methods
 * returns a non-<code>null</code> set of render properties, then the z-order
 * is given by {@link RenderProps#getZOrder()} for those properties. Otherwise,
 * the z-order is assumed to be 0.
 * 
 * <p>Sorting the renderables is done because it allows some renderers to
 * render the scene more realistically. For example, in OpenGL, better results
 * are obtained in opaque objects are drawn before transparent ones, and
 * transparent objects are drawn in increasing z-order.
 */
public class RenderList {

   protected SortedRenderableList myOpaque = new SortedRenderableList();
   protected SortedRenderableList myTransparent =  new SortedRenderableList();
   protected SortedRenderableList myOpaque2d = new SortedRenderableList();
   protected SortedRenderableList myTransparent2d = new SortedRenderableList();

   // renderables that were originally added to this list, before expansion
   private ArrayList<IsRenderable> myUnexpanded =
      new ArrayList<IsRenderable>();

   private int myLevel = 0;

   /**
    * Calls the {@link IsRenderable#prerender} method for a specified 
    * renderable, and then adds it to this list if it is also visible.
    * The renderable is considered to be visible if either
    * 
    * <ol>
    * <li>it is not an instance of {@link HasRenderProps}, or
    * <li>it is an instance of {@link HasRenderProps} and its
    * {@link HasRenderProps#getRenderProps()} method returns a 
    * non-<code>null</code> value for which 
    * {@link RenderProps#isVisible()} returns <code>true</code>. 
    * </ol>
    * 
    * <p>The renderable is added to either the <i>opaque</i>, 
    * <i>transparent</i>, <i>2d opaque</i>, or <i>2d transparent</i>
    * sublist, according to increasing z-order, as described in the
    * class documentation.
    * 
    * @param r renderable to maybe add to this list
    * @return <code>true</code> if <code>c</code> was added.
    */
   public <C extends IsRenderable> boolean addIfVisible (C r) {
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
         if ( (r.getRenderHints() & IsRenderable.TWO_DIMENSIONAL) != 0) {
            if ((r.getRenderHints() & IsRenderable.TRANSPARENT) != 0) {
               insertRenderable(r, myTransparent2d);
            }
            else {
               insertRenderable(r, myOpaque2d);
            }
         } else {
            if ((r.getRenderHints() & IsRenderable.TRANSPARENT) != 0) {
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
   
//   public <C extends IsRenderableExtended> boolean addIfVisible (
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
//         if ((r.getRenderHints() & IsRenderable.TWO_DIMENSIONAL) != 0) {
//            if ((r.getRenderHints() & IsRenderable.TRANSPARENT) != 0) {
//               insertRenderable(r, myTransparent2d);
//            }
//            else {
//               insertRenderable(r, myOpaque2d);
//            }
//         } else {
//            if ((r.getRenderHints() & IsRenderable.TRANSPARENT) != 0) {
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
   
   private static int getZOrderKey (IsRenderable r) {
      if (r instanceof HasRenderProps) {
         RenderProps props = ((HasRenderProps)r).getRenderProps();
         if (props != null) {
            return props.getZOrder();
         }
      }
      return 0;
   }
   
   private void insertRenderable(IsRenderable r, SortedRenderableList list) {
      // maintain order for equal indices
      int ri = getZOrderKey(r);
      list.add(r, ri);
   }

   /**
    * Calls {@link #addIfVisible} for every renderable in a specified
    * collection.
    * 
    * @param renderables collection of renderables to maybe add to this list.
    */
   public void addIfVisibleAll (Collection<? extends IsRenderable> renderables) {
      for (IsRenderable r : renderables) {
         addIfVisible (r);
      }
   }
   
//   public void addIfVisibleAll (
//      Collection<? extends IsRenderable> renderables, int prerenderFlags) {
//      for (IsRenderable r : renderables) {
//         if (r instanceof IsRenderableExtended) {
//            addIfVisible ((IsRenderableExtended)r, prerenderFlags);
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
   // LinkedList<IsRenderable> copy = (LinkedList<IsRenderable>)myList.clone();
   // for (IsRenderable r : copy)
   // { r.prerender (this);
   // }
   // }

   // public int getTransparent (IsRenderable[] array, int idx)
   // {
   // for (int i=0; i<myTransparent.size(); i++)
   // { array[idx++] = myTransparent.get(i);
   // }
   // return idx;
   // }

   // public int getOpaque (IsRenderable[] array, int idx)
   // {
   // for (int i=0; i<myOpaque.size(); i++)
   // { array[idx++] = myOpaque.get(i);
   // }
   // return idx;
   // }

   /**
    * Returns the <code>i</code>-th opaque renderable in this list.
    * 
    * @param i index of the renderable
    * @return <code>i</code>-th opaque renderable 
    */
   public IsRenderable getOpaque (int i) {
      return myOpaque.get (i);
   }

   public SortedRenderableList getOpaque() {
      return myOpaque;
   }
   
   /**
    * Returns the <code>i</code>-th transparent renderable in this list.
    * 
    * @param i index of the renderable
    * @return <code>i</code>-th transparent renderable 
    */
   public IsRenderable getTransparent (int i) {
      return myTransparent.get (i);
   }
   
   public SortedRenderableList getTransparent() {
      return myTransparent;
   }
   
   /**
    * Returns the <code>i</code>-th 2d opaque renderable in this list.
    * 
    * @param i index of the renderable
    * @return <code>i</code>-th 2d opaque renderable 
    */
   public IsRenderable getOpaque2d(int i) {
      return myOpaque2d.get(i);
   }
   
   public SortedRenderableList getOpaque2d() {
      return myOpaque2d;
   }
   
   /**
    * Returns the <code>i</code>-th 2d transparent renderable in this list.
    * 
    * @param i index of the renderable
    * @return <code>i</code>-th 2d transparent renderable 
    */
   public IsRenderable getTransparent2d(int i) {
      return myTransparent2d.get(i);
   }
   public SortedRenderableList getTransparent2d() {
      return myTransparent2d;
   }

   // public int get (IsRenderable[] array, int idx)
   // {
   // idx = getOpaque (array, idx);
   // idx = getTransparent (array, idx);
   // return idx;
   // }

   /**
    * Clears all renderables in this list.
    */
   public void clear() {
      myTransparent.clear();
      myOpaque.clear();
      myTransparent2d.clear();
      myOpaque2d.clear();
      myUnexpanded.clear();
   }

   // public Iterator<IsRenderable> iterator()
   // {
   // return myList.iterator();
   // }

   // public ListIterator<IsRenderable> listIterator()
   // {
   // return myList.listIterator();
   // }

   // public ListIterator<IsRenderable> listIterator(int idx)
   // {
   // return myList.listIterator(idx);
   // }

   /**
    * Returns the number of transparent objects in this list.
    * 
    * @return number of transparent objects
    */
   public int numTransparent() {
      return myTransparent.size();
   }

   /**
    * Returns the number of opaque objects in this list.
    * 
    * @return number of opaque objects
    */
   public int numOpaque() {
      return myOpaque.size();
   }
   
   /**
    * Returns the number of 2d transparent objects in this list.
    * 
    * @return number of 2d transparent objects
    */
   public int numTransparent2d() {
      return myTransparent2d.size();
   }
   
   /**
    * Returns the number of 2d opaque objects in this list.
    * 
    * @return number of 2d opaque objects
    */
   public int numOpaque2d() {
      return myOpaque2d.size();
   }

   protected int numTransparentSelQueries() {
      return myTransparent.numSelectionQueriesNeeded();
   }

   protected int numOpaqueSelQueries() {
      return myOpaque.numSelectionQueriesNeeded();
   }

   protected int numTransparent2dSelQueries() {
      return myTransparent2d.numSelectionQueriesNeeded();
   }

   protected int numOpaque2dSelQueries() {
      return myOpaque2d.numSelectionQueriesNeeded();
   }

   public int numSelectionQueriesNeeded() {
      return (myTransparent.numSelectionQueriesNeeded() +
              myOpaque.numSelectionQueriesNeeded() +
              myTransparent2d.numSelectionQueriesNeeded() +
              myOpaque2d.numSelectionQueriesNeeded());
   }
   
   public void printOpaqueSelectionQueries() {
      myOpaque.printSelectionQueriesNeeded();
   }

   /**
    * Returns the total number of renderables in this list.
    * 
    * @return total number of renderables
    */
   public int size() {
      return myTransparent.size() + myOpaque.size() 
         + myTransparent2d.size() + myOpaque2d.size();
   }

   public void updateBounds (Vector3d pmin, Vector3d pmax) {
      for (int i = 0; i < myUnexpanded.size(); i++) {
         myUnexpanded.get (i).updateBounds (pmin, pmax);
      }
   }

   private int renderList (
      Renderer renderer, SortedRenderableList list, int qid, int flags) {

      boolean selecting = renderer.isSelecting();

      for (int i = 0; i < list.size(); i++) {
         try {
            IsRenderable r = list.get (i);
            if (selecting && r instanceof IsSelectable) {
               IsSelectable s = (IsSelectable)r;
               
               try {
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
               } catch (Exception e) {
                  e.printStackTrace ();
               }
            } else if (selecting) {
               // don't render if not selectable (saves mucho time when lots of
               // non-selectable renderables, such as text labels)
            } 
            else {
               r.render (renderer, flags);
               // Uncomment to enable per-renderable isolation of GL errors:
               // if (renderer instanceof GLViewer) {
               //    GLViewer glv = (GLViewer)renderer;
               //    if (!GLSupport.checkAndPrintGLError(glv.getGL())) {
               //       System.out.println ("Error rendering " + r);
               //    }
               // }
            }
            renderer.restoreDefaultState(/*strictChecking=*/true);
         }
         catch (Exception e) {
            renderer.restoreDefaultState(/*strictChecking=*/false);
            throw e;
         }
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

   /**
    * Returns the <code>idx</code>-th renderable in this list.
    * 
    * @param idx index of the desired renderable
    * @return <code>idx</code>-th renderable 
    * @throws IndexOutOfBoundsException if <code>idx</code> is out of bounds.
    */
   public IsRenderable get (int idx) {
      if (idx < 0) {
         throw new IndexOutOfBoundsException ("index "+idx+" is negative");
      }
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
      nextList = myOpaque2d.size();
      if (idx < nextList) {
         return myTransparent2d.get (idx);
      }
      throw new IndexOutOfBoundsException (
         "index "+idx+" is out of bounds; list size is " + size());
   }

}
