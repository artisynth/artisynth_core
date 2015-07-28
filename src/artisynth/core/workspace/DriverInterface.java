/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.workspace;

import maspack.render.GL.GLViewer;
import maspack.widgets.PropertyWindow;

import javax.swing.JFrame;

import artisynth.core.driver.ViewerManager;
import artisynth.core.gui.selectionManager.SelectionListener;

/**
 * Provide the root model with services associated with the Artisynth driver
 * program.
 */

public interface DriverInterface {
   GLViewer getViewer();

   JFrame getFrame();

   ViewerManager getViewerManager();

   void addSelectionListener (SelectionListener l);

   void removeSelectionListener (SelectionListener l);

   public void registerWindow (PropertyWindow w);

   public void deregisterWindow (PropertyWindow w);

}
