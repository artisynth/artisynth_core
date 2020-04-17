/**
 * Copyright (c) 2014, by the Authors: John Lloyd (UBC), Tracy Wilkinson (UBC) and
 * ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */package artisynth.core.gui.editorManager;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;

import artisynth.core.driver.Main;
import artisynth.core.gui.SelectableComponentPanel;
import artisynth.core.gui.selectionManager.SelectionEvent;
import artisynth.core.gui.selectionManager.SelectionFilter;
import artisynth.core.gui.selectionManager.SelectionListener;
import artisynth.core.inverse.TrackingController;
import artisynth.core.mechmodels.MotionTargetComponent;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.ModelComponent;
import maspack.properties.EditingProperty;
import maspack.properties.HasProperties;
import maspack.properties.HostList;
import maspack.properties.PropTreeCell;
import maspack.util.InternalErrorException;
import maspack.util.ListView;
import maspack.widgets.ButtonMasks;
import maspack.widgets.DoubleField;
import maspack.widgets.GuiUtils;
import maspack.widgets.PropertyPanel;
import maspack.widgets.ValueChangeEvent;
import maspack.widgets.ValueChangeListener;

public class MotionTargetComponentAgent extends FrameBasedEditingAgent implements
SelectionListener {
   private TrackingController myController;
   private PropertyPanel myPropertyPanel;
   private PropTreeCell myPropTree;
   protected SelectableComponentPanel myTargetPanel;
   protected JScrollPane myListScroller;
   // protected HashSet<MotionTargetComponent> mySelectedTargets;
   CompositeComponent myAncestor;

   private State myState = State.Complete;

   private enum State {
      SelectingTargets, Complete
   };

   private boolean isValidTarget (ModelComponent c) {
      return (c instanceof MotionTargetComponent &&
              ComponentUtils.withinHierarchy (c, myAncestor));
   }

   private class ExcitationFilter implements SelectionFilter {
      public boolean objectIsValid (
         ModelComponent c, java.util.List<ModelComponent> currentSelections) {
         return isValidTarget (c);
      }
   }

   JPopupMenu createListPopup() {
      JPopupMenu menu = new JPopupMenu();
      JMenuItem removeItem;
      if (myTargetPanel.numSelectedWidgets() > 1) {
         removeItem = new JMenuItem ("remove targets");
      }
      else {
         removeItem = new JMenuItem ("remove target");
      }
      removeItem.addActionListener (this);
      removeItem.setActionCommand ("remove target");
      menu.add (removeItem);
      return menu;
   }

   private class ListMouseHandler extends MouseAdapter {
      public void mousePressed (MouseEvent e) {
         System.out.println ("pressed");
         if (e.getModifiersEx() == ButtonMasks.getContextMenuMask()) {
            if (myTargetPanel.numSelectedWidgets() > 0) {
               JPopupMenu popup = createListPopup();
               System.out.println (" popup");
               popup.show (e.getComponent(), e.getX(), e.getY());
            }
         }
      }
   }

   private void setState (State state) {
      switch (state) {
         case SelectingTargets: {
            myInstructionBox.setText ("Select motion targets to add");
            myAddButton.setText ("Stop");
            myAddButton.setActionCommand ("Stop");
            installSelectionFilter (new ExcitationFilter());
            // myContinuousAddToggle.setEnabled(false);
            // myTmpList.clear();
            break;
         }
         case Complete: {
            myInstructionBox.setText (" ");
            myAddButton.setText ("Add");
            myAddButton.setActionCommand ("Add");
            uninstallSelectionFilter();
            // myContinuousAddToggle.setEnabled(true);
            // myPointA = null;
            // myPointB = null;
            // myTmpList.clear();
            break;
         }
         default: {
            throw new InternalErrorException ("Unhandled state " + state);
         }
      }
      myState = state;
   }

   public MotionTargetComponentAgent (Main main, TrackingController controller) {
      super (main);
      myController = controller;
      myAncestor = ComponentUtils.nearestEncapsulatingAncestor (controller);
   }

   public void show (Rectangle popupBounds) {
      super.show (popupBounds);
      setState (State.SelectingTargets);
      mySelectionManager.addSelectionListener (this);
   }

   public void dispose() {
      super.dispose();
      myEditManager.releaseEditLock();
      mySelectionManager.removeSelectionListener (this);
      myTargetPanel.setSelectionManager (null);
      uninstallSelectionFilter();
   }

   protected void createDisplay() {
      createDisplayFrame ("Edit InverseController");
      // createPropertyPanel();
      // createSeparator();
      createTargetList();
      createInstructionBox();
      createOptionPanel ("Add Done");
   }

   protected void createPropertyPanel() {
      HostList hostList = new HostList (new HasProperties[] { myController });
      myPropTree = hostList.commonProperties (null, false);

      // remove properties which are to be excluded
      String[] excludedProps = new String[] { "excitation" };
      for (int i = 0; i < excludedProps.length; i++) {
         myPropTree.removeDescendant (excludedProps[i]);
      }
      hostList.saveBackupValues (myPropTree);
      hostList.getCommonValues (myPropTree, /* live= */true);

      myPropertyPanel =
         new PropertyPanel (EditingProperty.createProperties (
            myPropTree, hostList, /* isLive= */false));

      myPropertyPanel.setAlignmentX (Component.LEFT_ALIGNMENT);
      addWidget (myPropertyPanel);
   }

   private class WeightHandler implements ValueChangeListener {
      MotionTargetComponent myTarget;

      WeightHandler (MotionTargetComponent target) {
         myTarget = target;
      }

      public void valueChange (ValueChangeEvent e) {
         double newGain = (Double)e.getValue();
         myController.setMotionTargetWeight (myTarget, newGain);
      }
   }

   DoubleField createTargetWidget (MotionTargetComponent target, double gain) {
      String name = ComponentUtils.getPathName (myAncestor, target);
      DoubleField field = new DoubleField (name, gain);
      field.addValueChangeListener (new WeightHandler (target));
      field.setLabelStretchable (true);
      field.setStretchable (true);
      return field;
   }

   protected void createTargetList() {
      myTargetPanel = new SelectableComponentPanel();
      myTargetPanel.setSelectionManager (mySelectionManager);
      myTargetPanel.setSpacing (0);
      for (int i = 0; i < myController.getMotionSources ().size (); i++) {
         DoubleField widget =
            createTargetWidget (myController.getMotionSources().get (i), 
               myController.getMotionTargetWeights().get (i));
         myTargetPanel.addWidget (widget);
         myTargetPanel.mapWidgetToComponent (widget, myController.getMotionSources().get (i));
      }
      myTargetPanel.addMouseListener (new ListMouseHandler());
      JLabel label = new JLabel ("Existing targets and weights:");
      GuiUtils.setItalicFont (label);
      addToContentPane (label);
      myListScroller = new JScrollPane (myTargetPanel);
      myListScroller.setPreferredSize (new Dimension (480, 150));
      myListScroller.setMinimumSize (new Dimension (480, 150));
      addToContentPane (myListScroller);
   }

   public void selectionChanged (SelectionEvent e) {
      ModelComponent c = e.getLastAddedComponent();
      if (isValidTarget (c)) {
         MotionTargetComponent comp = (MotionTargetComponent)c;
         if (myState == State.SelectingTargets && 
             !myController.getMotionSources ().contains (comp) &&
             !myController.getMotionTargets ().contains (comp)) {
            myController.addMotionTarget (comp, 1.0);
            DoubleField widget = createTargetWidget (comp, 1.0);
            myTargetPanel.addWidget (widget);
            myTargetPanel.mapWidgetToComponent (widget, comp);
            myTargetPanel.revalidate();
            myTargetPanel.repaint();
         }
         // else if (myExciter.findTarget(ex) != -1)
         // { mySelectedTargets.add (ex);
         // }
      }
   }

   public void objectDeselected (SelectionEvent e) {
      // ModelComponent c = e.getComponent();
      // if (c instanceof MotionTargetComponent)
      // { MotionTargetComponent ex = (MotionTargetComponent)c;
      // mySelectedTargets.remove (ex);
      // }
   }

   public void actionPerformed (ActionEvent e) {
      String cmd = e.getActionCommand();
      if (cmd.equals ("Add")) {
         mySelectionManager.clearSelections();
         myMain.rerender();
         setState (State.SelectingTargets);
      }
      else if (cmd.equals ("Stop")) {
         mySelectionManager.clearSelections();
         setState (State.Complete);
         myMain.rerender();
      }
      else if (cmd.equals ("remove target")) {
         JComponent[] widgets = myTargetPanel.getSelectedWidgets();
         for (int i = 0; i < widgets.length; i++) {
            MotionTargetComponent target =
               (MotionTargetComponent)myTargetPanel.getComponent (widgets[i]);
            myController.removeMotionTarget (target);
            myTargetPanel.removeWidget (widgets[i]);
         }
         myTargetPanel.revalidate();
         myTargetPanel.repaint();
         myMain.rerender ();
      }
      else {
         super.actionPerformed (e);
      }
   }
}
