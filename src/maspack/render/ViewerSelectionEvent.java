/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

import java.util.*;

/**
 * Event returned whenever items are selected inside a Viewer.
 * 
 * @version 1.2 Modified by Chad to enable a multiple selection type event.
 */
public class ViewerSelectionEvent {
   int myModifiersEx;
   LinkedList<Object>[] mySelectedObjects;

   /**
    * Flag indicating that multiple object selection is desired.
    */
   public static final int MULTIPLE = 0x01;

   /**
    * Flag indicating that drag selection was performed.
    */
   public static final int DRAG = 0x02;

   // default selection type
   private int myFlags = 0;

   public ViewerSelectionEvent() {
   }

   /**
    * Returns the extended keyboard modifiers that were in play at the
    * time the selection was invoked.
    *
    * @return extended keyboard modifiers
    */
   public int getModifiersEx() {
      return myModifiersEx;
   }

   /**
    * Sets the extended keyboard modifiers that were in play at the
    * time the selection was invoked. For internal use only.
    */
   public void setModifiersEx (int modifiersEx) {
      myModifiersEx = modifiersEx;
   }

   /**
    * Returns an array of object lists for each selection query that resulted
    * in a selection.
    *
    * @return array of object lists for each selected query
    */
   public LinkedList<Object>[] getSelectedObjects() {
      return mySelectedObjects;
   }

   /**
    * Sets the value returned by {@link #getSelectedObjects}.  The value is set
    * by reference. For internal use only.
    *
    * @param lists array of object lists for each selected query
    */
   public void setSelectedObjects (LinkedList<Object>[] lists) {
      mySelectedObjects = lists;
   }

   /**
    * Returns the number of selection queries that resulted in a selection.
    *
    * @return number of selected queries
    */
   public int numSelectedQueries() {
      if (mySelectedObjects != null) {
         return mySelectedObjects.length;
      }
      else {
         return 0;
      }
   }

   /**
    * Returns the mode flags associated with the selection. Currently,
    * these include {@link #MULTIPLE} and {@link #DRAG}.
    *
    * @return selection mode flags
    */
   public int getFlags() {
      return myFlags;
   }

   public void setFlags (int flags) {
      this.myFlags = flags;
   }
}
