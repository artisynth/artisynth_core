/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import maspack.util.Disposable;

/**
 * A window that contains property widgets within its component hierarchy.
 */
public interface PropertyWindow extends Disposable {
   /**
    * Update the value of all property widgets in this window and its owned
    * windows.
    */
   public void updateWidgetValues();

   /**
    * Add a value change listener to all property widgets in this window and its
    * owned windows.
    */
   public void addGlobalValueChangeListener (ValueChangeListener l);

   /**
    * Remove a value change listener from all property widgets in this window
    * and its owned windows.
    */
   public void removeGlobalValueChangeListener (ValueChangeListener l);

   /**
    * Get an array of all the global value change listeners defined for this
    * window.
    */
   public ValueChangeListener[] getGlobalValueChangeListeners();

   /**
    * Gets the object (if any) with which property widget value changes are
    * synchronized.
    * 
    * @return synchronization object for this window
    */
   public Object getSynchronizeObject();

   /**
    * Sets an object with which property widget value changes are synchronized.
    * The synchronization is used for all property widgets in this window and
    * its owned windows.
    * 
    * @param syncObj
    * new synchronization object for this window
    */
   public void setSynchronizeObject (Object syncObj);

   /**
    * Dispose of all resources used by this object.
    */
   public void dispose();

   /**
    * If true, then the workspace should continuously update the property values
    * whenever the model is rerendered.
    */
   public boolean isLiveUpdatingEnabled();
}
