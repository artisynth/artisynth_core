/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.gui.selectionManager;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import maspack.properties.HasProperties;
import maspack.properties.HostList;
import maspack.properties.PropTreeCell;
import maspack.properties.PropertyInfo;
import maspack.render.RenderProps;
import maspack.render.Renderable;
import maspack.util.InternalErrorException;
import maspack.widgets.GuiUtils;
import maspack.widgets.OptionPanel;
import maspack.widgets.PropertyDialog;
import maspack.widgets.RenderPropsDialog;
import artisynth.core.driver.Main;
import artisynth.core.gui.editorManager.DuplicateAgent;
import artisynth.core.gui.editorManager.EditActionMap;
import artisynth.core.gui.editorManager.EditorBase;
import artisynth.core.gui.editorManager.EditorManager;
import artisynth.core.gui.editorManager.EditorUtils;
import artisynth.core.gui.editorManager.RemoveComponentsCommand;
import artisynth.core.gui.editorManager.TracingProbePanel;
import artisynth.core.modelbase.*;
import artisynth.core.probes.TracingProbe;
import artisynth.core.workspace.RootModel;

public class SelectionPopup extends JPopupMenu implements ActionListener {
   private static final long serialVersionUID = -8708147234861383471L;
   private Component myParentGUIComponent;
   private SelectionManager mySelectionManager;
   private EditActionMap myEditActionMap;

   public static boolean useRefListExpansion = false;

   private JMenuItem myTraceItem;
   @SuppressWarnings("unused")
   private Point myTraceItemLoc;

   // Cached list of items to be used for property changing-type commands.
   // This may be the current selection list, or it may be an expanded
   // version containing the expanded contents of all reference lists
   LinkedList<ModelComponent> myPropertyEditSelection;

   // Cached list of reference components that are currently selected.
   LinkedList<ReferenceComponent> myRefComponentSelection;

   // last screen bounds are recorded when the popup is set invisible, so that
   // any subsequently generated panels can be located close by
   private Rectangle myLastBounds;

   private static boolean myLocatePropEditClose = true;
   private static boolean myLocateRenderPropEditClose = true;

   private void addSeparatorIfNecessary() {
      int numc = getComponentCount();
      if (numc > 0 &&
          !(getComponent (numc - 1) instanceof JPopupMenu.Separator)) {
         addSeparator();
      }
   }

   private boolean isVisible (RenderableComponent rcomp) {
      RenderProps props = rcomp.getRenderProps();
      if (props != null) {
         return props.isVisible();
      }
      CompositeComponent parent = rcomp.getParent();
      if (parent instanceof RenderableComponentList) {
         return ((RenderableComponentList<?>)parent).rendersSubComponents();
      }
      return false;
   }

   private void setVisible (RenderableComponent rcomp, boolean enable) {
      if (isVisible (rcomp) != enable) {
         RenderProps.setVisible (rcomp, enable);
      }
   }

   private boolean componentsAreDeletable (
      Collection<? extends ModelComponent> comps) {
      for (ModelComponent c : comps) {
         CompositeComponent parent = c.getParent();
         if (parent == null ||
             c.isFixed() ||
             !(parent instanceof MutableCompositeComponent<?>) ||
             ComponentUtils.isAncestorSelected(c)) {
            return false;
         }
      }
      return true;
   }

   private void addMenuItem (String cmd) {
      JMenuItem item = new JMenuItem (cmd);
      item.addActionListener (this);
      add (item);
   }

   public void addPropertyEditMenuItems (
      LinkedList<ModelComponent> selection) {
      
      // parse the selection list.
      boolean allSelectedHaveProperties = false;
      boolean allSelectedAreTracable = false;
      int tracingCnt = 0;
      boolean oneSelectedIsRenderable = false;
      boolean oneSelectedHasRenderProps = false;
      boolean oneSelectedHasFixedRenderProps = false;
      boolean oneSelectedIsVisible = false;
      boolean oneSelectedIsInvisible = false;
      boolean oneSelectedHasRenderPropsProperty = false;

      if (selection.size() > 0) {
         allSelectedHaveProperties = true;
         allSelectedAreTracable = true;
         for (ModelComponent c : selection) {
            if (c instanceof RenderableComponent) {
                              
               oneSelectedIsRenderable = true;
               RenderableComponent rcomp = (RenderableComponent)c;
               if (isVisible (rcomp)) {
                  oneSelectedIsVisible = true;
               }
               else {
                  oneSelectedIsInvisible = true;
               }
               
               // RenderableComponent > HasProperties, but may not have
               //     renderProps property
               PropertyInfo rinfo =
                  rcomp.getAllPropertyInfo().get ("renderProps");
               if (rinfo != null) {
                  oneSelectedHasRenderPropsProperty = true;
               }
               
               if (rcomp.getRenderProps() != null) {
                  oneSelectedHasRenderProps = true;
                  // If still undecided, check to see if the render props can
                  // be set to null ...
                  if (!oneSelectedHasFixedRenderProps) {
                     if (!rinfo.getNullValueOK()) {
                        oneSelectedHasFixedRenderProps = true;
                     }
                  }
               }
            }
            if (!(c instanceof HasProperties)) {
               allSelectedHaveProperties = false;
            }
            if (!(c instanceof Tracable)) {
               allSelectedAreTracable = false;
            }
         }
      }
      Collection<Tracable> traceSet = Main.getRootModel().getTraceSet();
      for (Tracable tr : traceSet) {
         if (tr instanceof ModelComponent &&
             ((ModelComponent)tr).isSelected()) {
            tracingCnt++;
         }
      }

      if (allSelectedHaveProperties) {
         addMenuItem ("Edit properties ...");
      }
      if (oneSelectedIsRenderable && oneSelectedHasRenderPropsProperty) {
         if (oneSelectedHasRenderProps) {
            addMenuItem ("Edit render props ...");

            if (!oneSelectedHasFixedRenderProps) {
               addMenuItem ("Clear render props");
            }
         }
         else {
            addMenuItem ("Set render props ...");
         }
      }

      if (oneSelectedIsInvisible) {
         addMenuItem ("Set visible");
      }

      if (oneSelectedIsVisible) {
         addMenuItem ("Set invisible");
      }

      if (allSelectedAreTracable) {
         if (selection.size() - tracingCnt > 0) {
            addMenuItem ("Enable tracing");
         }
         if (tracingCnt > 0) {
            addMenuItem ("Disable tracing");
         }
         JMenuItem menuItem = new JMenuItem ("Clear trace");
         menuItem.addActionListener (this);
         String[] commonTracables = getCommonTracables (selection);
         if (commonTracables.length > 0) {
            menuItem = new JMenuItem ("Add tracing");
            myTraceItem = menuItem;
         }
         menuItem.addActionListener (this);
         add (menuItem);
      }
   }

   public SelectionPopup (
      SelectionManager selManager, Component parentGUIComponent) {
      super();

      // create an empty menu item
      JMenuItem menuItem = null;

      mySelectionManager = selManager;
      LinkedList<ModelComponent> selection;

      myParentGUIComponent = parentGUIComponent;
      boolean allSelectedAreRefLists = false;

      // See if the selection contains all ref listsand if so, are they
      // deletable. Use unexpanded selection for this
      selection = selManager.getCurrentSelection ();

      myRefComponentSelection = new LinkedList<ReferenceComponent>();
      if (selection.size() > 0) {
         allSelectedAreRefLists = true;
         for (ModelComponent c : selection) {
            if (!(c instanceof ReferenceList)) {
               allSelectedAreRefLists = false;
            }
            if (c instanceof ReferenceComponent) {
               myRefComponentSelection.add ((ReferenceComponent)c);
            }
         }
         if (allSelectedAreRefLists) {
            addMenuItem ("Edit reference list properties ...");
            addSeparatorIfNecessary();
            JLabel label = new JLabel ("  For reference list components:");
            GuiUtils.setItalicFont (label);
            add (label);
            myPropertyEditSelection =
               selManager.computeReferenceExpandedSelection ();
         }
         else {
            if (myRefComponentSelection.size() > 0) {
               addMenuItem ("Edit reference properties ...");
               addSeparatorIfNecessary();
            }
            myPropertyEditSelection = selection;
         }
         addPropertyEditMenuItems (myPropertyEditSelection);
      }

      EditorManager editManager = Main.getEditorManager();
      myEditActionMap = editManager.getActionMap (selManager);
      if (myEditActionMap.size() > 0) {
         addSeparatorIfNecessary();
         for (String name : myEditActionMap.getActionNames()) {
            menuItem = new JMenuItem (name);
            menuItem.addActionListener (this);
            int flags = myEditActionMap.getFlags (name);
            if ((flags & EditorBase.EXCLUSIVE) != 0) {
               if (editManager.isEditLocked()) {
                  menuItem.setEnabled (false);
               }
            }
            if ((flags & EditorBase.DISABLED) != 0) {
               menuItem.setEnabled (false);
            }
            add (menuItem);
         }
      }

      // LinkedList<ModelComponent> dependentSelection =
      //    selManager.getDependencyExpandedSelection ();

      LinkedList<ModelComponent> copySelection =
         selManager.getCopyExpandedSelection ();

      boolean deleteOK = false;
      boolean deleteRefsOK = false;
      boolean duplicateOK = false;

      if (selection.size() > 0) {
         deleteOK = componentsAreDeletable (selection);
      }
      if (myRefComponentSelection.size() > 0) {
         deleteRefsOK = componentsAreDeletable (myRefComponentSelection);
      }
      
      if (copySelection != null && copySelection.size() > 0) {
         duplicateOK = true;
         for (ModelComponent c : copySelection) {
            CompositeComponent parent = c.getParent();
            if (parent == null ||
                !(parent instanceof MutableCompositeComponent<?>) ||
                ComponentUtils.isAncestorSelected(c) || 
                !(c instanceof CopyableComponent)) {
               duplicateOK = false;
               break;
            }
         }
      }

      if (selection.size() == 1 || duplicateOK || deleteOK || deleteRefsOK) {
         addSeparatorIfNecessary();
      }

      if (selection.size() == 1) {
         addMenuItem ("Save as ...");
      }
      if (duplicateOK) {
         addMenuItem ("Duplicate");
      }
      if (deleteRefsOK) {
         addMenuItem ("Delete reference(s)");
      }
      if (deleteOK) {
         addMenuItem ("Delete");
      }
   }

   public void show (Component comp, int x, int y) {
      super.show (comp, x, y);
   }

   public void setVisible (boolean enable) {
      if (!enable) {
         if (myTraceItem != null) {
            myTraceItemLoc =
               GuiUtils.getScreenLocation (myTraceItem, 1.0, 0.0);
         }
         myLastBounds = GuiUtils.getScreenBounds (this);
      }
      super.setVisible (enable);
   }

   String[] getCommonTracables (LinkedList<ModelComponent> selection) {
      LinkedList<String> list = null;
      for (ModelComponent comp : selection) {
         if (comp instanceof Tracable) {
            String[] tracables = ((Tracable)comp).getTracables();
            if (list == null) {
               list = new LinkedList<String>();
               for (int i = 0; i < tracables.length; i++) {
                  list.add (tracables[i]);
               }
            }
            else {
               for (String tname : list) {
                  boolean found = false;
                  for (int i = 0; i < tracables.length; i++) {
                     if (tracables[i].equals (tname)) {
                        found = true;
                        break;
                     }
                  }
                  if (!found) {
                     list.remove (tname);
                  }
               }
               if (list.isEmpty()) {
                  break;
               }
            }
         }
      }
      return list.toArray (new String[0]);
   }

   // /**
   // * do all selected components have render properties
   // * @param selection
   // */
   // private boolean allSelectedHaveProperties (
   // LinkedList<ModelComponent> selection)
   // {
   // for (ModelComponent comp : selection)
   // { if (!(comp instanceof HasProperties))
   // { return false;
   // }
   // }
   // return true;
   // }

   // /**
   // * at least one selected component has render properties
   // * @param selection
   // */
   // private boolean oneSelectedHasRenderProps (
   // LinkedList<ModelComponent> selection)
   // {
   // for (ModelComponent comp : selection)
   // { if (comp instanceof Renderable)
   // { return true;
   // }
   // }
   // return false;
   // }

   // Used by getNameForSelection
   private static String getNameOrNumber (ModelComponent comp) {
      if (comp.getName() != null) {
         return comp.getName();
      }
      else {
         return Integer.toString(comp.getNumber());
      }
   }

   // Used by getNameForSelection
   private static String getParentPrefix (ModelComponent comp) {
      ModelComponent parent = comp.getParent();
      if (parent == null) {
         return "";
      }
      else if (parent.getParent() == null) {
         return getNameOrNumber(parent) + "/";
      }
      else {
         return "*/" + getNameOrNumber(parent) + "/";
      }
   }

   private static final int NAME_LIMIT = 50;
   /** 
    * Creates a short name for the current selection by appending
    * the tails of path names for the first few components. Components
    * which share parents are grouped together.
    */
   private String getNameForSelection (
      Collection<? extends ModelComponent> sel) {
      StringBuilder sbuf = new StringBuilder(80);

      if (sel.size() == 0) {
         // paranoid
         return "";
      }

      ModelComponent next;
      boolean samePrevParent = false;
      boolean sameNextParent;
      Iterator<? extends ModelComponent> it = sel.iterator();
      for (ModelComponent comp=it.next(); comp!=null; comp=next) {
         next = it.hasNext() ? it.next() : null;

         sameNextParent = (next != null && next.getParent() != null &&
                           next.getParent() == comp.getParent());

         if (comp.getParent() != null && !samePrevParent) {
            sbuf.append (getParentPrefix (comp));
            if (sameNextParent) {
               sbuf.append ('{');
            }
         }
         sbuf.append (getNameOrNumber(comp));
         if (samePrevParent && !sameNextParent) {
            sbuf.append ('}');
         }
         if (next != null) {
            sbuf.append (',');
            if (sbuf.length() > NAME_LIMIT) {
               sbuf.append (" ...");
               break;
            }
         }
         samePrevParent = sameNextParent;
      }
      return sbuf.toString();
   }

   public void createPropertyDialog (
      Collection<? extends ModelComponent> selectedItems) {
      HostList hostList = new HostList (selectedItems);
      PropTreeCell tree =
         hostList.commonProperties (null, /* allowReadonly= */true);
      tree.removeDescendant ("renderProps");
      if (tree.numChildren() == 0) {
         JOptionPane.showMessageDialog (
            myParentGUIComponent,
            "No common properties for selected components",
            "no common properties", JOptionPane.INFORMATION_MESSAGE);
      }
      else {
         PropertyDialog propDialog =
            new PropertyDialog (
               "Edit properties", tree, hostList, "OK Cancel LiveUpdate");
         propDialog.setScrollable (true);
         if (myLocatePropEditClose) {
            GuiUtils.locateRelative (
               propDialog, myLastBounds, 0.5, 0.5, 0, 0.5);
         }
         else {
            propDialog.locateRight (Main.getMain().getFrame());
         }
         //propDialog.setSynchronizeObject (Main.getRootModel());
         Main.getMain().registerWindow (propDialog);
         propDialog.setTitle (
            "Properties for "+getNameForSelection (selectedItems));
         propDialog.setVisible (true);
      }
   }

   public void actionPerformed (ActionEvent e) {
      String command = e.getActionCommand();

      LinkedList<ModelComponent> selectedItems = 
         mySelectionManager.getCurrentSelection ();

      if (command.equals ("Edit reference list properties ...")) {
         createPropertyDialog (selectedItems);
      }
      else if (command.equals ("Edit reference properties ...")) {
         createPropertyDialog (myRefComponentSelection);
      }

      // if (command.equals ("Delete reference list(s)")) {
      //    deleteSelection (/*expandRefLists=*/false);         
      // }

      if (command.equals ("Edit properties ...")) {
         LinkedList<ModelComponent> selection = myPropertyEditSelection;
         if (myRefComponentSelection.size() > 0 &&
             myRefComponentSelection.size() < selection.size()) {
            selection = new LinkedList<ModelComponent>();
            for (ModelComponent c : myPropertyEditSelection) {
               if (!(c instanceof ReferenceComponent)) {
                  selection.add (c);
               }
            }
         }
         createPropertyDialog (selection);
      }
      else if (command.equals ("Edit render props ...") ||
               command.equals ("Set render props ...")) {
         LinkedList<ModelComponent> renderables =
            new LinkedList<ModelComponent>();
         for (ModelComponent c : myPropertyEditSelection) {
            if (c instanceof Renderable) {
               renderables.add (c);
            }
         }
         RenderPropsDialog dialog =
            new RenderPropsDialog ("Edit render properties", renderables);

         if (dialog.numProperties() == 0) {
            JOptionPane.showMessageDialog (
               myParentGUIComponent,
               "No common render properties for selected components",
               "no common render properties", JOptionPane.INFORMATION_MESSAGE);
         }
         else {
            if (myLocateRenderPropEditClose) {
               GuiUtils.locateRelative (
                  dialog, myLastBounds, 0.5, 0.5, 0, 0.5);
            }
            else {
               dialog.locateRight (Main.getMain().getFrame());
            }
            Main.getMain().registerWindow (dialog);
            dialog.setTitle (
               "RenderProps for "+getNameForSelection (myPropertyEditSelection));
            dialog.setVisible (true);
         }
      }
      else if (command.equals ("Clear render props")) {
         for (ModelComponent c : myPropertyEditSelection) {
            if (c instanceof Renderable) {
               ((Renderable)c).setRenderProps (null);
            }
         }
         requestViewerUpdate();
      }
      else if (command.equals ("Set visible")) {
         for (ModelComponent c : myPropertyEditSelection) {
            if (c instanceof RenderableComponent) {
               setVisible ((RenderableComponent)c, true);
            }
         }
         requestViewerUpdate();
      }
      else if (command.equals ("Set invisible")) {
         for (ModelComponent c : myPropertyEditSelection) {
            if (c instanceof RenderableComponent) {
               setVisible ((RenderableComponent)c, false);
            }
         }
         requestViewerUpdate();
      }
      else if (command.equals ("Enable tracing")) {
         RootModel rootModel = Main.getRootModel();
         for (ModelComponent c : myPropertyEditSelection) {
            if (c instanceof Tracable) {
               Tracable tr = (Tracable)c;
               if (!rootModel.isTracing (tr)) {
                  rootModel.enableTracing (tr);
               }
            }
         }
      }
      else if (command.equals ("Disable tracing")) {
         RootModel rootModel = Main.getRootModel();
         for (ModelComponent c : myPropertyEditSelection) {
            if (c instanceof Tracable) {
               Tracable tr = (Tracable)c;
               if (rootModel.isTracing (tr)) {
                  rootModel.disableTracing (tr);
               }
            }
         }
      }
      else if (command.equals ("Clear trace")) {
         RootModel rootModel = Main.getRootModel();
         for (ModelComponent c : myPropertyEditSelection) {
            if (c instanceof Tracable) {
               Tracable tr = (Tracable)c;
               if (rootModel.isTracing (tr)) {
                  rootModel.clearTracing (tr);
               }
            }
         }
         Main.rerender();
      }
      else if (command.equals ("Add tracing")) {
         addTracing (myPropertyEditSelection);
      }
      else if (command.equals ("Save as ...")) {
         EditorUtils.saveComponent (selectedItems.get (0));
      }
      else if (command == "Delete") {
         deleteSelection ();
      }
      else if (command.equals ("Delete reference(s)")) {
         deleteRefComponentSelection ();         
      }
      else if (command == "Duplicate") {
         duplicateSelection();
      }
      else {
         // EditorBase editor = myEditActionMap.get (command);
         // if (editor != null) {
         //    editor.applyAction (command, selectedItems, myLastBounds);
         // }
         EditorBase editor = myEditActionMap.getEditor (command);
         if (editor != null) {
            editor.applyAction (command, selectedItems, myLastBounds);
         }
      }
   }

   private void addTracing (LinkedList<ModelComponent> selectedItems) {
//      RootModel root = Main.getRootModel();
//      String name = null;
//      double startTime = Main.getTimeSec();
//      double updateInterval = root.getMaxStepSizeSec();

      Tracable firstTracable = null;
      for (ModelComponent comp : selectedItems) {
         if (comp instanceof Tracable) {
            firstTracable = (Tracable)comp;
         }
      }
      if (firstTracable == null) {
         throw new InternalErrorException (
            "'Add tracing' called with no tracabled in selection");
      }

      TracingProbePanel panel =
         new TracingProbePanel (
            firstTracable, getCommonTracables (selectedItems));
      panel.pack();
      GuiUtils.locateRelative (panel, myLastBounds, 0.5, 0.5, 0, 0.5);
      // if (myTraceItemLoc != null)
      // { panel.setLocation (
      // myTraceItemLoc.x, myTraceItemLoc.y);
      // }
      panel.setVisible (true);
      if (panel.getReturnValue() == OptionPanel.OK_OPTION) {
         for (ModelComponent comp : selectedItems) {
            if (comp instanceof Tracable) {
               TracingProbe probe = panel.createProbe ((Tracable)comp);
               Main.getRootModel().addOutputProbe (probe);
            }
         }
      }
   }

   private void deleteRefComponentSelection () {
      mySelectionManager.clearSelections();
      @SuppressWarnings("unchecked")
      RemoveComponentsCommand cmd =
         new RemoveComponentsCommand (
            "delete",
            myRefComponentSelection);
      Main.getUndoManager().saveStateAndExecute (cmd);
      requestViewerUpdate();
   }

   private void deleteSelection () {
      LinkedList<ModelComponent> selection =
         mySelectionManager.getCurrentSelection ();

      LinkedList<ModelComponent> update = new LinkedList<ModelComponent>();
      LinkedList<ModelComponent> delete = 
         ComponentUtils.findDependentComponents (update, selection);

      if (delete.size() > selection.size()) {
         // first, see if we can actually delete:
         if (!componentsAreDeletable (delete)) {
            JOptionPane.showMessageDialog (
               myParentGUIComponent,
               "Selection refers to additional components that can't be deleted", 
               "selection not deletable", JOptionPane.INFORMATION_MESSAGE);
         }

         boolean needConfirmation = false;
         for (ModelComponent c : delete) {
            if (!c.isSelected()) {
               // XXX Big Hack. Need a more general way to check
               // if delete confirmation is needed
               if (!(c instanceof artisynth.core.mechmodels.CollisionHandler)) {
                  needConfirmation = true;
               }
               mySelectionManager.addSelected (c);
            }
         }
         requestViewerUpdate();
         if (needConfirmation) {
            JOptionPane confirmDialog =
               new JOptionPane (
                  "Dependent components will be deleted also. Proceed?",
                  JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_OPTION);
            JDialog dialog =
               confirmDialog.createDialog (
                  myParentGUIComponent, "confirm deletion");
            GuiUtils.locateHorizontally (
               dialog, myParentGUIComponent, GuiUtils.CENTER);
            GuiUtils.locateVertically (
               dialog, myParentGUIComponent, GuiUtils.BELOW);
            dialog.setVisible (true);
            if ((confirmDialog.getValue() == null) ||
                (confirmDialog.getValue().equals (JOptionPane.NO_OPTION))) {
               return;
            }
         }
      }
      // components are deselected before they are removed. This
      // will not affect dependentSelection because it is a (possibly
      // extended) copy of the selection.
      mySelectionManager.clearSelections();
      @SuppressWarnings("unchecked")
      RemoveComponentsCommand cmd =
         new RemoveComponentsCommand ("delete", delete, update);
      Main.getUndoManager().saveStateAndExecute (cmd);
      requestViewerUpdate();
   }

   private void duplicateSelection() {
      LinkedList<ModelComponent> selection =
         mySelectionManager.getCurrentSelection ();
      LinkedList<ModelComponent> copySelection =
         mySelectionManager.getCopyExpandedSelection ();

      LinkedList<ModelComponent> copyList = new LinkedList<ModelComponent>();

      HashMap<ModelComponent,ModelComponent> copyMap =
         new HashMap<ModelComponent,ModelComponent> (copySelection.size());

      if (copySelection.size() > selection.size()) {
         for (ModelComponent c : copySelection) {
            if (!c.isSelected()) {
               mySelectionManager.addSelected (c);
            }
         }
      }

      // component are ordered with referenced components first, so we want to
      // reverse this ordering when building the add list
      LinkedList<MutableCompositeComponent<?>> parentList =
         new LinkedList<MutableCompositeComponent<?>>();

      for (ModelComponent c : copySelection) {
         ModelComponent copy;

         if ((copy = copyMap.get (c)) == null) {
            copy = ((CopyableComponent)c).copy (
               CopyableComponent.COPY_REFERENCES, copyMap);
            copyMap.put (c, copy);
            System.out.println ("copying " + c);
         }
         parentList.addFirst ((MutableCompositeComponent<?>)c.getParent());
         copyList.addFirst (copy);
      }
      DuplicateAgent widget =
         new DuplicateAgent (Main.getMain(), copyList, parentList);
      widget.show();
   }

   private void requestViewerUpdate() {
      Main.getMain();
      Main.rerender();
   }
}
