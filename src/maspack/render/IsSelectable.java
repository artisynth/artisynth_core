/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

import java.util.LinkedList;

public interface IsSelectable extends IsRenderable {

   /**
    * Returns true if this object is in fact selectable.
    * 
    * @return true if this object is selectable
    */
   public boolean isSelectable();

   /**
    * If this selectable manages its own selection (by issuing selection
    * queries within its <code>render</code> method), then this method should
    * return the maximum number of selection queries that will be
    * required. Otherwise, this method should return -1.
    *
    * @return maximum number of selection queries needed by this component, or
    * -1 if this component does not manage its own selection.
    */
   public int numSelectionQueriesNeeded();
   
   /**
    * Append to <code>list</code> the component (or components) associated with
    * the <code>qid</code>-th selection query issued by this component's render
    * method. This will only be called if this component manages its own
    * selection (i.e., the number <code>nums</code> returned by {@link
    * #numSelectionQueriesNeeded()} is positive), and <code>qid</code> will in
    * turn be a number between 0 and <code>nums</code>-1.
    *
    * @param list
    * selected objects are appended to the end of this list
    * @param qid index of the selection query
    */   
   public void getSelection (LinkedList<Object> list, int qid);

}
