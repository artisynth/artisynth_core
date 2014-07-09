/**
 * Copyright (c) 2014, by the Authors: John Lloyd (UBC), Tracy Wilkinson (UBC) and
 * ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */package artisynth.core.gui.editorManager;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import java.util.*;

import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.*;
import artisynth.core.driver.*;
import artisynth.core.gui.*;
import artisynth.core.gui.widgets.*;
import artisynth.core.gui.selectionManager.*;
import maspack.util.*;
import maspack.widgets.ButtonMasks;
import maspack.widgets.DoubleField;
import maspack.widgets.GuiUtils;
import maspack.widgets.PropertyPanel;
import maspack.widgets.ValueChangeEvent;
import maspack.widgets.ValueChangeListener;
import maspack.properties.*;

public class ExcitationTargetAgent extends FrameBasedEditingAgent implements
SelectionListener {
   private MuscleExciter myExciter;
   private PropertyPanel myPropertyPanel;
   private PropTreeCell myPropTree;
   protected SelectableComponentPanel myTargetPanel;
   private ListView<ExcitationComponent> myTargetView;
   protected JScrollPane myListScroller;
   // protected HashSet<ExcitationComponent> mySelectedTargets;
   CompositeComponent myAncestor;

   private State myState = State.Complete;

   private enum State {
      SelectingExciters, Complete
   };

   private boolean isValidTarget (ModelComponent c) {
      return (c instanceof ExcitationComponent &&
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
         case SelectingExciters: {
            myInstructionBox.setText ("Select excitation targets to add");
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

   public ExcitationTargetAgent (Main main, MuscleExciter exciter) {
      super (main);
      myExciter = exciter;
      myTargetView = myExciter.getTargetView();
      // mySelectedTargets = new HashSet<ExcitationComponent>();
      myAncestor = ComponentUtils.nearestEncapsulatingAncestor (exciter);
   }

   public void show (Rectangle popupBounds) {
      super.show (popupBounds);
      setState (State.SelectingExciters);
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
      createDisplayFrame ("Edit MuscleExciter");
      // createPropertyPanel();
      // createSeparator();
      createTargetList();
      createInstructionBox();
      createOptionPanel ("Add Done");
   }

   protected void createPropertyPanel() {
      HostList hostList = new HostList (new HasProperties[] { myExciter });
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

   private class GainHandler implements ValueChangeListener {
      ExcitationComponent myTarget;

      GainHandler (ExcitationComponent target) {
         myTarget = target;
      }

      public void valueChange (ValueChangeEvent e) {
         double newGain = (Double)e.getValue();
         myExciter.setGain (myTarget, newGain);
      }
   }

   DoubleField createTargetWidget (ExcitationComponent target, double gain) {
      String name = ComponentUtils.getPathName (myAncestor, target);
      DoubleField field = new DoubleField (name, gain);
      field.addValueChangeListener (new GainHandler (target));
      field.setLabelStretchable (true);
      field.setStretchable (true);
      return field;
   }

   protected void createTargetList() {
      myTargetPanel = new SelectableComponentPanel();
      myTargetPanel.setSelectionManager (mySelectionManager);
      myTargetPanel.setSpacing (0);
      for (int i = 0; i < myExciter.numTargets(); i++) {
         DoubleField widget =
            createTargetWidget (myExciter.getTarget (i), myExciter.getGain (i));
         myTargetPanel.addWidget (widget);
         myTargetPanel.mapWidgetToComponent (widget, myExciter.getTarget (i));
      }
      myTargetPanel.addMouseListener (new ListMouseHandler());
      JLabel label = new JLabel ("Existing targets and gains:");
      GuiUtils.setItalicFont (label);
      addToContentPane (label);
      myListScroller = new JScrollPane (myTargetPanel);
      myListScroller.setPreferredSize (new Dimension (280, 150));
      myListScroller.setMinimumSize (new Dimension (280, 150));
      addToContentPane (myListScroller);
   }

   public void selectionChanged (SelectionEvent e) {
      ModelComponent c = e.getLastAddedComponent();
      if (isValidTarget (c)) {
         ExcitationComponent ex = (ExcitationComponent)c;
         if (myState == State.SelectingExciters && ex != myExciter
         && myExciter.findTarget (ex) == -1) {
            myExciter.addTarget (ex, 1.0);
            DoubleField widget = createTargetWidget (ex, 1.0);
            myTargetPanel.addWidget (widget);
            myTargetPanel.mapWidgetToComponent (widget, ex);
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
      // if (c instanceof ExcitationComponent)
      // { ExcitationComponent ex = (ExcitationComponent)c;
      // mySelectedTargets.remove (ex);
      // }
   }

   public void actionPerformed (ActionEvent e) {
      String cmd = e.getActionCommand();
      if (cmd.equals ("Add")) {
         mySelectionManager.clearSelections();
         myMain.rerender();
         setState (State.SelectingExciters);
      }
      else if (cmd.equals ("Stop")) {
         mySelectionManager.clearSelections();
         setState (State.Complete);
         myMain.rerender();
      }
      else if (cmd.equals ("remove target")) {
         JComponent[] widgets = myTargetPanel.getSelectedWidgets();
         for (int i = 0; i < widgets.length; i++) {
            ExcitationComponent target =
               (ExcitationComponent)myTargetPanel.getComponent (widgets[i]);
            myExciter.removeTarget (target);
            myTargetPanel.removeWidget (widgets[i]);
         }
         myTargetPanel.revalidate();
         myTargetPanel.repaint();
      }
      else {
         super.actionPerformed (e);
      }
   }
}
