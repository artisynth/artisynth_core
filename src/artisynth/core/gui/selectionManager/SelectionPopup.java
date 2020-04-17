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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ArrayList;

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
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.CopyableComponent;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.MutableCompositeComponent;
import artisynth.core.modelbase.ReferenceComp;
import artisynth.core.modelbase.ReferenceList;
import artisynth.core.modelbase.RenderableComponent;
import artisynth.core.modelbase.RenderableComponentBase;
import artisynth.core.modelbase.RenderableComponentList;
import artisynth.core.modelbase.Traceable;
import artisynth.core.probes.TracingProbe;
import artisynth.core.workspace.RootModel;

public class SelectionPopup extends JPopupMenu implements ActionListener {
   private static final long serialVersionUID = -8708147234861383471L;
   private Component myParentGUIComponent;
   private SelectionManager mySelectionManager;
   private Main myMain;
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
   LinkedList<ReferenceComp> myRefComponentSelection;
   
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

//   private boolean isVisible (RenderableComponent rcomp) {
//      RenderProps props = rcomp.getRenderProps();
//      if (props != null) {
//         return props.isVisible();
//      }
//      CompositeComponent parent = rcomp.getParent();
//      if (parent instanceof RenderableComponentList) {
//         return ((RenderableComponentList<?>)parent).rendersSubComponents();
//      }
//      return false;
//   }
//
//   private void setVisible (RenderableComponent rcomp, boolean enable) {
//      if (isVisible (rcomp) != enable) {
//         RenderProps.setVisible (rcomp, enable);
//      }
//   }

   /**
    * Returns true if a composite component is editable, meaning that its
    * subcomponents can be removed via the GUI.
    *
    * @param parent component to check for editability
    * @param ignoreEditable if {@code true}, parent need only be an instance of
    * {@link MutableCompositeComponent}, and the result of its {@link
    * MutableCompositeComponent#isEditable} method is ignored.
    */
   private boolean isEditable (
      CompositeComponent parent, boolean ignoreEditable) {
      if (parent instanceof MutableCompositeComponent<?>) {
         if (ignoreEditable) {
            return true;
         }
         else {
            return ((MutableCompositeComponent<?>)parent).isEditable();
         }
      }
      else {
         return false;
      }
   }

   /**
    * Returns true if all components in a selection can be deleted.
    *
    * @param comps list of components to check
    * @param ignoreEditable if {@code true}, each component's parent need only
    * be an instance of {@link MutableCompositeComponent}, and the result of
    * its {@link MutableCompositeComponent#isEditable} method is ignored.
    * @return {@code true} if all components can be deleted
    */
   private boolean componentsAreDeletable (
      Collection<? extends ModelComponent> comps, boolean ignoreEditable) {
      for (ModelComponent c : comps) {
         CompositeComponent parent = c.getParent();
         if (parent == null ||
             c.isFixed() ||
             !isEditable(parent, ignoreEditable) ||
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
      boolean allSelectedAreTraceable = false;
      int tracingCnt = 0;
      boolean oneSelectedIsRenderable = false;
      boolean oneSelectedHasRenderProps = false;
      boolean oneSelectedHasFixedRenderProps = false;
      boolean oneSelectedIsVisible = false;
      boolean oneSelectedIsInvisible = false;
      boolean oneSelectedHasRenderPropsProperty = false;
      
      if (selection.size() > 0) {
         allSelectedHaveProperties = true;
         allSelectedAreTraceable = true;
          for (ModelComponent c : selection) {
            if (c instanceof RenderableComponent) {
               oneSelectedIsRenderable = true;
               RenderableComponent rcomp = (RenderableComponent)c;
               if (RenderableComponentBase.isVisible (rcomp)) {
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
            if (!(c instanceof Traceable)) {
               allSelectedAreTraceable = false;
            }
         }
      }
      Collection<Traceable> traceSet = myMain.getRootModel().getTraceSet();
      for (Traceable tr : traceSet) {
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

      if (allSelectedAreTraceable) {
         if (selection.size() - tracingCnt > 0) {
            addMenuItem ("Enable tracing");
         }
         if (tracingCnt > 0) {
            addMenuItem ("Disable tracing");
         }
         JMenuItem menuItem = new JMenuItem ("Clear trace");
         menuItem.addActionListener (this);
         String[] commonTraceables = getCommonTraceables (selection);
         if (commonTraceables.length > 0) {
            menuItem = new JMenuItem ("Add tracing probe");
            myTraceItem = menuItem;
         }
         menuItem.addActionListener (this);
         add (menuItem);
      }

      addMenuItem ("Save component names ...");
   }

   public SelectionPopup (
      SelectionManager selManager, Component parentGUIComponent) {
      super();
      
      // create an empty menu item
      JMenuItem menuItem = null;

      mySelectionManager = selManager; 
      myMain = Main.getMain();
      LinkedList<ModelComponent> selection;

      myParentGUIComponent = parentGUIComponent;
      boolean allSelectedAreRefLists = false;

      // See if the selection contains all ref listsand if so, are they
      // deletable. Use unexpanded selection for this
      selection = selManager.getCurrentSelection ();

      myRefComponentSelection = new LinkedList<ReferenceComp>();
      if (selection.size() > 0) {
         allSelectedAreRefLists = true;
         for (ModelComponent c : selection) {
            if (!(c instanceof ReferenceList)) {
               allSelectedAreRefLists = false;
            }
            if (c instanceof ReferenceComp) {
               myRefComponentSelection.add ((ReferenceComp)c);
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

      EditorManager editManager = myMain.getEditorManager();
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
         deleteOK = componentsAreDeletable (
            selection, /*ignoreEditable=*/false);
      }
      if (myRefComponentSelection.size() > 0) {
         deleteRefsOK = componentsAreDeletable (
            myRefComponentSelection, /*ignoreEditable=*/false);
      }
      
      if (copySelection != null && copySelection.size() > 0) {
         duplicateOK = true;
         for (ModelComponent c : copySelection) {
            CompositeComponent parent = c.getParent();
            if (parent == null ||
                !isEditable(parent, /*ignoreEditable=*/false) ||
                ComponentUtils.isAncestorSelected(c) || 
                !(c instanceof CopyableComponent)) {
               System.out.println ("failed here "+c);
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

   String[] getCommonTraceables (LinkedList<ModelComponent> selection) {
      LinkedList<String> list = null;
      for (ModelComponent comp : selection) {
         if (comp instanceof Traceable) {
            String[] traceables = ((Traceable)comp).getTraceables();
            if (list == null) {
               list = new LinkedList<String>();
               for (int i = 0; i < traceables.length; i++) {
                  list.add (traceables[i]);
               }
            }
            else {
               for (String tname : list) {
                  boolean found = false;
                  for (int i = 0; i < traceables.length; i++) {
                     if (traceables[i].equals (tname)) {
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
         GuiUtils.showNotice (
            myParentGUIComponent, "No common properties for selected components");
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
            propDialog.locateRight (myMain.getFrame());
         }
         //propDialog.setSynchronizeObject (myMain.getRootModel());
         myMain.registerWindow (propDialog);
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
               if (!(c instanceof ReferenceComp)) {
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
            GuiUtils.showNotice (
               myParentGUIComponent,
               "No common render properties for selected components");
         }
         else {
            if (myLocateRenderPropEditClose) {
               GuiUtils.locateRelative (
                  dialog, myLastBounds, 0.5, 0.5, 0, 0.5);
            }
            else {
               dialog.locateRight (myMain.getFrame());
            }
            myMain.registerWindow (dialog);
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
               RenderableComponentBase.setVisible (
                  (RenderableComponent)c, true);
            }
         }
         requestViewerUpdate();
      }
      else if (command.equals ("Set invisible")) {
         for (ModelComponent c : myPropertyEditSelection) {
            if (c instanceof RenderableComponent) {
               RenderableComponentBase.setVisible (
                  (RenderableComponent)c, false);
            }
         }
         requestViewerUpdate();
      }
      else if (command.equals ("Enable tracing")) {
         RootModel rootModel = myMain.getRootModel();
         for (ModelComponent c : myPropertyEditSelection) {
            if (c instanceof Traceable) {
               Traceable tr = (Traceable)c;
               if (!rootModel.isTracing (tr)) {
                  rootModel.enableTracing (tr);
               }
            }
         }
      }
      else if (command.equals ("Disable tracing")) {
         RootModel rootModel = myMain.getRootModel();
         for (ModelComponent c : myPropertyEditSelection) {
            if (c instanceof Traceable) {
               Traceable tr = (Traceable)c;
               if (rootModel.isTracing (tr)) {
                  rootModel.disableTracing (tr);
               }
            }
         }
      }
      else if (command.equals ("Clear trace")) {
         RootModel rootModel = myMain.getRootModel();
         for (ModelComponent c : myPropertyEditSelection) {
            if (c instanceof Traceable) {
               Traceable tr = (Traceable)c;
               if (rootModel.isTracing (tr)) {
                  rootModel.clearTracing (tr);
               }
            }
         }
         myMain.rerender();
      }
      else if (command.equals ("Add tracing probe")) {
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
      else if (command.equals ("Save component names ...")) {
         EditorUtils.saveComponentNames (selectedItems);
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
//      RootModel root = myMain.getRootModel();
//      String name = null;
//      double startTime = myMain.getTimeSec();
//      double updateInterval = root.getMaxStepSizeSec();

      Traceable firstTraceable = null;
      for (ModelComponent comp : selectedItems) {
         if (comp instanceof Traceable) {
            firstTraceable = (Traceable)comp;
         }
      }
      if (firstTraceable == null) {
         throw new InternalErrorException (
            "'Add tracing' called with no traceabled in selection");
      }

      TracingProbePanel panel =
         new TracingProbePanel (
            firstTraceable, getCommonTraceables (selectedItems));
      panel.pack();
      GuiUtils.locateRelative (panel, myLastBounds, 0.5, 0.5, 0, 0.5);
      // if (myTraceItemLoc != null)
      // { panel.setLocation (
      // myTraceItemLoc.x, myTraceItemLoc.y);
      // }
      panel.setVisible (true);
      if (panel.getReturnValue() == OptionPanel.OK_OPTION) {
         for (ModelComponent comp : selectedItems) {
            if (comp instanceof Traceable) {
               TracingProbe probe = panel.createProbe ((Traceable)comp);
               myMain.getRootModel().addOutputProbe (probe);
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
      myMain.getUndoManager().saveStateAndExecute (cmd);
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
         if (!componentsAreDeletable (delete, /*ignoreEditable=*/true)) {
            GuiUtils.showNotice (
               myParentGUIComponent,
               "Selection refers to additional components that can't be deleted");
            return;
         }
         else {
            boolean needConfirmation = false;
            ArrayList<ModelComponent> dependents = new ArrayList<>();
            for (ModelComponent c : delete) {
               if (!c.isSelected()) {
                  needConfirmation = true;
                  dependents.add (c);
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
                     myMain.getFrame(), "confirm deletion");
               // GuiUtils.locateHorizontally (
               //    dialog, myParentGUIComponent, GuiUtils.CENTER);
               // GuiUtils.locateVertically (
               //    dialog, myParentGUIComponent, GuiUtils.BELOW);
               dialog.setVisible (true);
               if ((confirmDialog.getValue() == null) ||
                   (confirmDialog.getValue().equals (JOptionPane.NO_OPTION))) {
                  for (ModelComponent c : dependents) {
                     mySelectionManager.removeSelected (c);
                  }
                  return;
               }
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
      myMain.getUndoManager().saveStateAndExecute (cmd);
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
         new DuplicateAgent (myMain, copyList, parentList);
      widget.show();
   }

   private void requestViewerUpdate() {
      myMain.rerender();
   }
}
