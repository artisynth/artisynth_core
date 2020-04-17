/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.gui.selectionManager;

import java.awt.event.MouseEvent;
import java.util.*;

import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

import maspack.render.IsSelectable;
import maspack.render.ViewerSelectionEvent;
import maspack.render.ViewerSelectionFilter;
import maspack.render.ViewerSelectionListener;
import maspack.util.*;
import artisynth.core.driver.Main;
import artisynth.core.driver.Main.SelectionMode;
import artisynth.core.gui.navpanel.NavPanelNode;
import artisynth.core.gui.navpanel.NavigationPanel;
import artisynth.core.modelbase.*;

/**
 * @author John E Lloyd, after Chad Decker
 */
public class SelectionManager {
   
   protected ViewerSelectionHandler myViewerSelectionHandler;
   protected MyViewerSelectionFilter myViewerSelectionFilter;
   protected LinkedList<ModelComponent> mySelectedItems;
   // Cached selection list, expanded to include dependencies.
   protected LinkedList<ModelComponent> myDependencyExpandedSelection;
   // Cached selection list, expanded to include references.
   protected LinkedList<ModelComponent> myCopyExpandedSelection;
   // Indicates if the copy expanded selection is valid. Should
   // be set to false every time the selection changes. We use a
   // boolean because myCopyExpandedSelection can itself be null.
   protected boolean myCopyExpandedP = false;
   
   protected LinkedList<SelectionListener> mySelectionListeners;
   protected boolean myPopupMenuEnabledP = true;
   protected int myMaxSelections = -1;

   protected NavigationPanel myNavPanel;
   protected NavPanelSelector myNavPanelSelectionHandler;
   protected ArrayList<SelectionFilter> myFilters;
   //   private TimelineSelectionHandler myTimelineSelectionHandler;

   protected void clearCachedLists() {
      myDependencyExpandedSelection = null;
      myCopyExpandedP = false;
   }

   public SelectionManager() {
      myViewerSelectionHandler = new ViewerSelectionHandler();
      myViewerSelectionFilter = new MyViewerSelectionFilter();
      mySelectedItems = new LinkedList<ModelComponent>();
      mySelectionListeners = new LinkedList<SelectionListener>();
      myFilters = new ArrayList<SelectionFilter>();
      //myTimelineSelectionHandler = new TimelineSelectionHandler();
   }

   public void setNavPanel (NavigationPanel navpanel) {
      // paranoid: setNavPanel should only be called once at startup
      if (myNavPanel != null) {
         myNavPanel.removeSelectionListener (myNavPanelSelectionHandler);
      }
      myNavPanel = navpanel;
      myNavPanelSelectionHandler = new NavPanelSelector();
      myNavPanel.addSelectionListener (myNavPanelSelectionHandler);
   }

   public ViewerSelectionHandler getViewerSelectionHandler() {
      return myViewerSelectionHandler;
   }
   
   public MyViewerSelectionFilter getViewerSelectionFilter() {
      return myViewerSelectionFilter;
   }
   
   public void addFilter (SelectionFilter filter) {
      myFilters.add (filter);
   }

   public boolean removeFilter (SelectionFilter filter) {
      return myFilters.remove (filter);
   }

   public SelectionFilter[] getFilters() {
      return myFilters.toArray (new SelectionFilter[0]);
   }

   public void filterSelections (SelectionFilter filter) {
      LinkedList<ModelComponent> removed = new LinkedList<ModelComponent>();
      for (ModelComponent c : mySelectedItems) {
         if (!filter.objectIsValid (c, mySelectedItems)) {
            if (c.isSelected()) {
               c.setSelected (false);
               removed.add (c);
            }
         }
      }
      finishAddAndRemove (null, removed, /* notifyNavPanel= */true);
   }

   /**
    * Removes excess selections from the selection list, so that list is kept
    * within it's maximum size. When this method is called, the selection list
    * should be in it's updated state with ModelComponent.isSelected() returning
    * the correct value for each component.
    */
   private LinkedList<ModelComponent> removeExcessSelections() {
      if (myMaxSelections != -1 && mySelectedItems.size() > myMaxSelections) {
         LinkedList<ModelComponent> excess = new LinkedList<ModelComponent>();
         while (mySelectedItems.size() > myMaxSelections) {
            ModelComponent c = mySelectedItems.removeFirst();
            c.setSelected (false);
            excess.add (c);
         }
         return excess;
      }
      else {
         return null;
      }
   }

   /**
    * Sets the maximum number of components that may be concurrently selected. A
    * value of -1 indicates that there is no limit.
    * 
    * @param max
    * maximum number of concurrently selectable components
    */
   public void setMaximumSelections (int max) {
      myMaxSelections = max;
      LinkedList<ModelComponent> excess = removeExcessSelections();
      if (excess != null) {
         updateNavPanel (null, excess);
         fireSelectionListeners (null, excess);
         clearCachedLists();
         requestViewerUpdate();
      }
   }

   /**
    * Returns the maximum number of components that may be concurrently
    * selected. A value of -1 indicates that there is no limit.
    * 
    * @return maximum number of concurrently selectable components
    */
   public int getMaximumSelections() {
      return myMaxSelections;
   }

   /**
    * Returns a list of the currently selected components. This list must
    * <i>not</i> be modified by the caller.
    * 
    * @return currently selected components
    */
   public LinkedList<ModelComponent> getCurrentSelection() {
      return mySelectedItems;
   }

   LinkedList<ModelComponent> computeReferenceExpandedSelection() {
      LinkedList<ModelComponent> expanded = new LinkedList<ModelComponent>();

      for (ModelComponent c : mySelectedItems) {
         if (c instanceof ReferenceList) {
            ReferenceList list = (ReferenceList)c;
            for (int i=0; i<list.size(); i++) {
               ModelComponent r = list.getReference(i);
               if (!r.isMarked()) {
                  r.setMarked (true);
                  expanded.add (r);            
               }
            }
         }
         else if (!c.isMarked()) {
            c.setMarked (true);
            expanded.add (c);            
         }
      }
      for (ModelComponent c : expanded) {      
         c.setMarked(false);
      }
      return expanded;
   }

   // /**
   //  * Returns a list of the currently selected components, expanded to include
   //  * components which also depend on the current selection. This list must
   //  * <i>not</i> be modified by the caller.
   //  * 
   //  * @return dependency expansion of the current selection
   //  */
   // public LinkedList<ModelComponent> getDependencyExpandedSelection() {
   //    if (myDependencyExpandedSelection == null) {
   //       myDependencyExpandedSelection =
   //          ComponentUtils.findDependentComponents (getCurrentSelection());
   //    }
   //    return myDependencyExpandedSelection;
   // }

   /**
    * Returns a list of the currently selected components, expanded to include
    * components which also referenced by the current selection. This list must
    * <i>not</i> be modified by the caller.
    * 
    * @return reference expansion of the current selection
    */
   public LinkedList<ModelComponent> getCopyExpandedSelection() {
      if (!myCopyExpandedP) {
         myCopyExpandedSelection =
            ComponentUtils.findCopyComponents (getCurrentSelection ());
         myCopyExpandedP = true;
      }
      return myCopyExpandedSelection;
   }

   public ModelComponent getLastSelected() {
      if (mySelectedItems.isEmpty()) {
         return null;
      }
      else {
         return mySelectedItems.getLast();
      }
   }

   public int getNumSelected() {
      return mySelectedItems.size();
   }

   private boolean objectIsValid (ModelComponent c) {
      for (int i=0; i<myFilters.size(); i++) {
         if (!myFilters.get(i).objectIsValid (c, mySelectedItems)) {
            return false;
         }
      }
      return true;
   }

   /**
    * Updates the selections in the NavPanel to reflect the current state of the
    * selection. The parameters <code>added</code> and <code>removed</code>
    * describe the components that have been added and removed from the
    * selection.
    * 
    * <p>
    * When this method is called, the selection list should be in it's updated
    * state with ModelComponent.isSelected() returning the correct value for
    * each component. This information may therefore be used instead of the
    * <code>added</code> and <code>removed</code> parameters to determine
    * the necessary NavPanel updates.
    */
   private void updateNavPanel (
      LinkedList<ModelComponent> added, LinkedList<ModelComponent> removed) {

      myNavPanel.removeSelectionListener (myNavPanelSelectionHandler);
      int REDO_LIMIT = 1;
      if (mySelectedItems.size() <= REDO_LIMIT) { // faster to do it this way:
         myNavPanel.unselectAllPaths();
         for (ModelComponent c : mySelectedItems) {
            myNavPanel.selectPath (c);
         }
      }
      else {
         if (removed != null) {
            for (ModelComponent c : removed) {
               myNavPanel.unselectPath (c);
            }
         }
         if (added != null) {
            for (ModelComponent c : added) {
               myNavPanel.selectPath (c);
            }
         }
      }
      myNavPanel.addSelectionListener (myNavPanelSelectionHandler);
   }

   /**
    * Add and remove components from the selection list. When this method is
    * called, it is assumed that the selection state of the components has
    * already been set (i.e., ModelComponent.isSelected() will return
    * <code>true</code> for every component in <code>added</code> and
    * <code>false</code> for every component in <code>removed</code>).
    */
   void finishAddAndRemove (
      LinkedList<ModelComponent> added, LinkedList<ModelComponent> removed,
      boolean notifyNavPanel) {
      if ((added != null && added.size() > 0) ||
          (removed != null && removed.size() > 0)) {
         if (removed != null && removed.size() > 0) {
            Iterator<ModelComponent> it = mySelectedItems.iterator();
            while (it.hasNext()) {
               ModelComponent c = it.next();
               if (!c.isSelected()) {
                  it.remove();
               }
            }
         }
         if (added != null && added.size() > 0) {
            if (myMaxSelections != -1) {
               while (added.size() > myMaxSelections) {
                  ModelComponent c = added.removeFirst();
                  c.setSelected (false);
               }
            }
            mySelectedItems.addAll (added);
         }
         LinkedList<ModelComponent> excess = removeExcessSelections();
         if (excess != null) {
            removed.addAll (excess);
         }
         if (excess != null || notifyNavPanel) {
            if (notifyNavPanel) {
               updateNavPanel (added, removed);
            }
            else {
               updateNavPanel (null, excess);
            }
         }
         fireSelectionListeners (added, removed);
         clearCachedLists();
         requestViewerUpdate();
      }
   }

   public void addSelected (List<ModelComponent> addList) {
      addAndRemoveSelected (addList, null);
   }

   public void removeSelected (List<ModelComponent> removeList) {
      addAndRemoveSelected (null, removeList);
   }

   public void addAndRemoveSelected (
      List<ModelComponent> addList, List<ModelComponent> removeList) {
      LinkedList<ModelComponent> added = new LinkedList<ModelComponent>();
      if (addList != null) {
         for (ModelComponent c : addList) {
            if (!c.isSelected()) {
               c.setSelected (true);
               added.add (c);
            }
         }
      }
      LinkedList<ModelComponent> removed = new LinkedList<ModelComponent>();
      if (removeList != null) {
         for (ModelComponent c : removeList) {
            if (c.isSelected()) {
               c.setSelected (false);
               removed.add (c);
            }
         }
      }
      finishAddAndRemove (added, removed, /* notifyNavPanel= */true);
   }

   public boolean addSelected (ModelComponent c) {
      if (!c.isSelected()) {
         LinkedList<ModelComponent> added = new LinkedList<ModelComponent>();
         c.setSelected (true);
         added.add (c);
         finishAddAndRemove (added, null, /* notifyNavPanel= */true);
         return true;
      }
      else {
         return false;
      }
   }

   public boolean removeSelected (ModelComponent c) {
      if (c.isSelected()) {
         LinkedList<ModelComponent> removed = new LinkedList<ModelComponent>();
         c.setSelected (false);
         removed.add (c);
         finishAddAndRemove (null, removed, /* notifyNavPanel= */true);
         return true;
      }
      else {
         return false;
      }
   }

   public void clearSelections() {
      // A little inefficient, but not a big deal ...
      LinkedList<ModelComponent> removed = new LinkedList<ModelComponent>();
      for (ModelComponent c : mySelectedItems) {
         c.setSelected (false);
         removed.add (c);
      }
      finishAddAndRemove (null, removed, /* notifyNavPanel= */true);
   }

   public void addSelectionListener (SelectionListener l) {
      mySelectionListeners.add (l);
   }

   public boolean removeSelectionListener (SelectionListener l) {
      return mySelectionListeners.remove (l);
   }

   protected void fireSelectionListeners (
      LinkedList<ModelComponent> added, LinkedList<ModelComponent> removed) {
      SelectionEvent e = new SelectionEvent (added, removed);
      for (SelectionListener l : mySelectionListeners) {
         l.selectionChanged (e);
      }
   }

   /**
    * Remove all selections that the NavPanel does not know about.
    */
   private int deselectNavPanelUnknowns (LinkedList<ModelComponent> list) {
      int cnt = 0;
      for (ModelComponent c : mySelectedItems) {
         if (!myNavPanel.hasExpandedPath (c)) {
            c.setSelected (false);
            list.add (c);
            cnt++;
         }
      }
      return cnt;
   }

   // private class NavPanelSelectorOld implements TreeSelectionListener {
   //    public void valueChanged (TreeSelectionEvent e) {
   //       LinkedList<ModelComponent> added = new LinkedList<ModelComponent>();
   //       LinkedList<ModelComponent> removed = new LinkedList<ModelComponent>();

   //       TreePath[] paths = e.getPaths();
   //       deselectNavPanelUnknowns (removed);

   //       // The nav panel can have multiple paths pointing to the same
   //       // component. Therefore, any path that is removed needs to
   //       // be checked to see if there are other selection paths that
   //       // point to the same component. Only if is this is not the
   //       // case will the component be deselected.
   //       HashSet<ModelComponent> maybeDeselect = new HashSet<ModelComponent>();
   //       for (int i=0; i<paths.length; i++) {
   //          ModelComponent c = NavPanelNode.getNodeComponent (
   //             paths[i].getLastPathComponent());
            
   //          if (c != null) {
   //             if (e.isAddedPath (i)) {
   //                if (!c.isSelected()) {
   //                   c.setSelected (true);
   //                   added.add (c);
   //                }
   //             }
   //             else {
   //                // might remove from selection; will check below
   //                maybeDeselect.add (c);
   //             }
   //          }
   //       }
   //       if (maybeDeselect.size() > 0) {
   //          // check to see if any other navpanel selection paths point to
   //          // these components
   //          paths = myNavPanel.getTree().getSelectionPaths();
   //          if (paths != null) {
   //             for (int i=0; i<paths.length; i++) {
   //                ModelComponent c = NavPanelNode.getNodeComponent (
   //                   paths[i].getLastPathComponent());              
   //                maybeDeselect.remove (c);
   //             }
   //          }
   //       }
   //       for (ModelComponent c : maybeDeselect) {
   //          if (c.isSelected()) {
   //             c.setSelected (false);
   //             removed.add (c);
   //          }
   //       }
   //       finishAddAndRemove (added, removed, /* notifyNavPanel= */false);
   //    }
   // }

   private class NavPanelSelector implements TreeSelectionListener {
      public void valueChanged (TreeSelectionEvent e) {
         LinkedList<ModelComponent> added = new LinkedList<ModelComponent>();
         LinkedList<ModelComponent> removed = new LinkedList<ModelComponent>();

         HashSet<ModelComponent> navpanelSelections =
            new LinkedHashSet<ModelComponent>();

         TreePath[] paths = myNavPanel.getTree().getSelectionPaths();
         if (paths != null) {
            for (int i=0; i<paths.length; i++) {
               ModelComponent c = NavPanelNode.getNodeComponent (
                  paths[i].getLastPathComponent());              
               if (c != null) {
                  navpanelSelections.add (c);
               }
               if (c instanceof ReferenceComp) {
                  ModelComponent cref = ((ReferenceComp)c).getReference();
                  if (cref != null) {
                     navpanelSelections.add (cref);
                  }
               }
            }
         }

         for (ModelComponent c : mySelectedItems) {
            if (!c.isSelected()) {
               throw new InternalErrorException (
                  "Component "+ComponentUtils.getDiagnosticName(c)+
                  " not selected but is on the selection list");
            }
            if (navpanelSelections.contains (c)) {
               navpanelSelections.remove (c);
            }
            else {
               c.setSelected (false);
               removed.add (c);
            }
         }
         added.addAll (navpanelSelections);
         for (ModelComponent c : added) {
            if (c.isSelected()) {
               throw new InternalErrorException (
                  "Component "+ComponentUtils.getDiagnosticName(c)+
                  " is selected but is not on the selection list");
            }
            c.setSelected (true);
         }
         finishAddAndRemove (added, removed, /* notifyNavPanel= */false);
      }
   }

   private HashMap<ModelComponent,Integer> myRefCountMap =
      new HashMap<ModelComponent,Integer>();

   private void incRefCount (ModelComponent c) {
      Integer cnt = myRefCountMap.get(c);
      if (cnt == null) {
         myRefCountMap.put(c, 1);
      }
      else {
         myRefCountMap.put(c, cnt+1);
      }
   }

   private int decRefCount (ModelComponent c) {
      Integer cnt = myRefCountMap.get(c);
      if (cnt == null) {
         return -1;
      }
      else {
         int newcnt = cnt-1;
         if (newcnt == 0) {
            myRefCountMap.remove (c);
         }
         else {
            myRefCountMap.put(c, newcnt);
         }
         return newcnt;
      }
   }

   private void requestViewerUpdate() {
      Main.getMain().rerender();
   }

   /**
    * Means by which the GLViewer interacts with the selection manager
    */
   private class ViewerSelectionHandler implements ViewerSelectionListener {
      /**
       * Means by which the GLViewer interacts with the selection manager
       */
      public void itemsSelected (ViewerSelectionEvent e) {
         LinkedList<ModelComponent> added = new LinkedList<ModelComponent>();
         LinkedList<ModelComponent> removed = new LinkedList<ModelComponent>();
         // HashSet is used to make sure each component is processed only once,
         // even if it occurs several times in the set of selected items (which
         // can occur if a component is rendered in pieces).
         HashSet<ModelComponent> handled = new HashSet<ModelComponent>();
         
         int selOp = (e.getFlags() & ViewerSelectionEvent.OP_MASK);

         // multiple selection mode
         boolean dragging = ((e.getFlags() & ViewerSelectionEvent.DRAG) != 0);
         boolean done = false;

         // In the documentation below, let V be the set of items picked in the
         // viewer and S the set of selected items.
         if (e != null && e.numSelectedQueries() > 0) {
            List<LinkedList<?>> itemPaths = e.getSelectedObjects();
            for (LinkedList<?> path : itemPaths) {
               // loop backwards through items
               Iterator<?> pit = path.descendingIterator();
               while (pit.hasNext()) {
                  Object item = pit.next();
                  if (item instanceof ModelComponent) {
                     ModelComponent c = (ModelComponent)item;
                     
                     if (objectIsValid (c) && !handled.contains(c)) {
                        switch (selOp) {
                           case ViewerSelectionEvent.SET: {
                              //   added   = V minus S
                              //   removed = S minus V
                              //   S       = V                            
                              if (!c.isSelected()) {
                                 added.add (c);
                                 c.setSelected (true);
                              }
                              else {
                                 c.setSelected (false);
                              }                             
                              // NOTE: we finish updating 'removed' and
                              // 'S' below
                              break;
                           }
                           case ViewerSelectionEvent.ADD: {
                              //   added   = V minus S
                              //   removed = null
                              //   S       = V union S
                              if (!c.isSelected()) {
                                 added.add (c);
                                 c.setSelected (true);
                              }
                              break;
                           }
                           case ViewerSelectionEvent.SUBTRACT: {
                              //   added   = null
                              //   removed = V intersect S
                              //   S       = S minus V
                              if (c.isSelected()) {
                                 removed.add (c);
                                 c.setSelected (false);
                              }                             
                              break;
                           }
                           case ViewerSelectionEvent.XADD: {
                              //   added   = V minus S
                              //   removed = V intersect S
                              //   S       = V minus S
                              if (!c.isSelected()) {
                                 added.add (c);
                                 c.setSelected (true);
                              }
                              else {
                                 removed.add (c);
                                 c.setSelected (false);
                              }                             
                              break;
                           }
                           default: {
                              throw new InternalErrorException (
                                 "Unknown selection operation: " + selOp);
                           }
                        }
                        handled.add (c);
                        // if not dragging, should be only one item handled in
                        // viewer.
                        if (!dragging) {
                           done = true;
                        }
                        break;   // we handled one item from this path
                     } // done checking if valid
                     
                  }  // done checking if component
               }// done looping for items on path
               
               if (done) {
                  break;
               }
               
            } // done looping through paths
         }
         
         if (selOp == ViewerSelectionEvent.SET) {
            // finish updating 'removed' and the current selection set
            for (ModelComponent c : mySelectedItems) {
               if (!c.isSelected()) {
                  c.setSelected (true);
               }
               else {
                  c.setSelected (false);
                  removed.add (c);
               }
            }
         }
         finishAddAndRemove (added, removed, /* notifyNavPanel= */true);
      }
   }

   /**
    * Means by which the GLViewer prefilters selectable objects.
    */
   private class MyViewerSelectionFilter implements ViewerSelectionFilter {
      public boolean isSelectable (IsSelectable s) {
         if (s instanceof ModelComponent) {
            return objectIsValid ((ModelComponent)s);
         }
         else {
            return true;
         }
      }
   }

   public boolean isPopupMenuEnabled() {
      return myPopupMenuEnabledP;
   }

   public void setPopupMenuEnabled (boolean enable) {
      myPopupMenuEnabledP = enable;
   }

   public void displayPopup (MouseEvent evt) {
      if (myPopupMenuEnabledP && mySelectedItems.size() > 0) {
         SelectionPopup popup = new SelectionPopup (this, evt.getComponent());
         popup.setLightWeightPopupEnabled (false);
         popup.show (evt.getComponent(), evt.getX(), evt.getY());
      }
   }

}
