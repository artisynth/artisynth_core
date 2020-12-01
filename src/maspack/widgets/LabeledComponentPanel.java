/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import maspack.properties.HasProperties;
import maspack.properties.HostList;
import maspack.util.Disposable;
import maspack.util.InternalErrorException;

public class LabeledComponentPanel extends JPanel
   implements MouseListener, MouseMotionListener, ActionListener,
              PopupMenuListener, Disposable, HasAlignableLabels {

   protected LinkedHashSet<JComponent> mySelectedWidgets;
   private static final long serialVersionUID = 1L;
   protected ArrayList<Component> myWidgets;
   private boolean myWidgetsSelectable = true;
   private boolean myWidgetsDraggable = true;

   protected int myNumBasicWidgets = 0;
   
   //protected ArrayList<Component> myComponents;
   private LinkedList<ListSelectionListener> mySelectionListeners;
   
   LabeledComponentLayout myLayout;

   public boolean areWidgetsDraggable() {
      return myWidgetsDraggable;
   }

   public void setWidgetsDraggable (boolean enable) {
      myWidgetsDraggable = enable;
   }

   public boolean areWidgetsSelectable() {
      return myWidgetsSelectable;
   }

   public void setWidgetsSelectable (boolean enable) {
      myWidgetsSelectable = enable;
   }

   public void addSelectionListener (ListSelectionListener l) {
      if (mySelectionListeners == null) {
         mySelectionListeners = new LinkedList<ListSelectionListener>();
      }
      mySelectionListeners.add (l);
   }

   public void removeSelectionListener (ListSelectionListener l) {
      mySelectionListeners.remove (l);
      if (mySelectionListeners.isEmpty()) {
         mySelectionListeners = null;
      }
   }

   public ListSelectionListener[] getSelectionListeners() {
      return mySelectionListeners.toArray (new ListSelectionListener[0]);
   }

   protected void fireSelectionListeners (
      int firstIdx, int lastIdx, boolean adjusting) {
      ListSelectionEvent event = null;
      for (ListSelectionListener l : mySelectionListeners) {
         if (event == null) {
            event = new ListSelectionEvent (this, firstIdx, lastIdx, adjusting);
         }
         l.valueChanged (event);
      }
   }

   private LabelSpacing myLabelSpacing = new LabelSpacing();

   public int getSpacing() {
      return myLayout.getSpacing();
   }

   public void setSpacing (int spacing) {
      myLayout.setSpacing (spacing);
   }

   public boolean isStretchable() {
      return myLayout.isStretchable();
   }

   public void setStretchable (boolean enable) {
      myLayout.setStretchable (enable);
   }

   public void getLabelSpacing (LabelSpacing spacing) {
      spacing.set (myLabelSpacing);
   }

   public void setLabelSpacing (LabelSpacing spacing) {
      myLabelSpacing.set (spacing);
      LabelSpacing subspacing = new LabelSpacing();
      subspacing.set (spacing);
      subspacing.labelWidth -= LabeledComponentBase.getLeftInset(this);
      for (int i=0; i<myWidgets.size(); i++) {
         if (myWidgets.get(i) instanceof HasAlignableLabels) {
            ((HasAlignableLabels)myWidgets.get(i)).setLabelSpacing(subspacing);
         }
      }
   }

   public void getPreferredLabelSpacing (LabelSpacing spacing) {
      LabelSpacing subspacing = new LabelSpacing();
      spacing.set (0, 0);
      for (int i=0; i<myWidgets.size(); i++) {
         if (myWidgets.get(i) instanceof HasAlignableLabels) {;
            HasAlignableLabels acomp = (HasAlignableLabels)myWidgets.get(i);
            acomp.getPreferredLabelSpacing(subspacing);
            if (subspacing.labelWidth > spacing.labelWidth) {
               spacing.labelWidth = subspacing.labelWidth; 
            }
            if (subspacing.preSpacing > spacing.preSpacing) {
               spacing.preSpacing = subspacing.preSpacing; 
            }
         }
      }
      spacing.labelWidth += LabeledComponentBase.getLeftInset (this);
   }

   public void resetLabelAlignment () {

      boolean repack = false;

      LabelSpacing pref = new LabelSpacing();
      LabelSpacing spc = new LabelSpacing();
      getPreferredLabelSpacing (pref);
      getLabelSpacing (spc);
      if (!spc.isEqual (pref)) {
         setLabelSpacing (pref);
         repack = true;
      }
      if (repack) {
         Window win = SwingUtilities.windowForComponent (this);
         if (win != null && win.isVisible()) {
            win.pack();
         }
      }
   }

   public static LabeledComponentPanel getTopPanel (Component comp) {
      LabeledComponentPanel panel = null;
      if (comp instanceof LabeledComponentPanel) {
         panel = (LabeledComponentPanel)comp;
      }
      else if (comp.getParent() instanceof LabeledComponentPanel) {
         panel = (LabeledComponentPanel)comp.getParent();
      }
      else {
         return null;
      }
      while (panel.getParent() instanceof LabeledComponentPanel) {
          panel = (LabeledComponentPanel)panel.getParent();
      }
      return panel;      
   }

   public int getLeftInsetToTop () {
      int leftInset = 0;
      JComponent comp = this;
      while (comp instanceof LabeledComponentPanel) {
         leftInset += LabeledComponentBase.getLeftInset (comp);
         comp = (JComponent)comp.getParent();
      }
      return leftInset;
   }

   public LabeledComponentPanel() {
      super();
      myLayout = new LabeledComponentLayout();
      setLayout (myLayout);
      addMouseListener (this);
      addMouseMotionListener (this);
      myWidgets = new ArrayList<Component>();
      mySelectedWidgets = new LinkedHashSet<JComponent>();
   }

   public int getComponentIndex (Component comp) {
      int numc = getComponentCount();
      for (int i = 0; i < numc; i++) {
         if (getComponent (i) == comp) {
            return i;
         }
      }
      return -1;
   }

   public Component[] getWidgets() {
      return myWidgets.toArray(new Component[0]);
   }

   public Component getWidget (int idx) {
      return myWidgets.get (idx);
   }

   public int numSelectedWidgets() {
      return mySelectedWidgets.size();
   }

   public JComponent[] getSelectedWidgets() {
      JComponent[] widgets = new JComponent[mySelectedWidgets.size()];
      Component[] comps = getComponents();
      int k = 0;
      for (int i = 0; i < comps.length; i++) {
         if (mySelectedWidgets.contains (comps[i])) {
            widgets[k++] = (JComponent)comps[i];
         }
      }
      if (k != widgets.length) {
         throw new InternalErrorException (
            "number of widgets inconsistent with map size");
      }
      return widgets;
   }

   public int numBasicWidgets() {
      return myNumBasicWidgets;
   }

   public Component addWidget (String labelText, LabeledComponentBase comp) {
      comp.setLabelText (labelText);
      addWidget (comp, myNumBasicWidgets);
      return comp;
   }

   public Component addWidget (Component comp) {
      addWidget (comp, myNumBasicWidgets);
      return comp;
   }

   public Component addWidget (Component comp, int idx) {
      if (idx > myNumBasicWidgets) {
         throw new IllegalArgumentException (
            "idx "+idx+" exceeds component count " + myNumBasicWidgets);
      }
      if (myWidgets.size() < myNumBasicWidgets) {
         throw new InternalErrorException (
            "widget list size < num basic widgets");
      }
      doAddWidget (comp, idx);
      myNumBasicWidgets++;
      add (comp, idx);
      return comp;
   }

   protected void doAddWidget (Component comp, int idx) {
      boolean resetNeeded = false;
      LabeledComponentPanel topPanel = null;
      if (comp instanceof HasAlignableLabels) {
         if (comp instanceof LabeledComponent) {
            ((LabeledComponent)comp).setBorder (
               BorderFactory.createEmptyBorder (1, 1, 1, 1));
         }
         else if (comp instanceof LabeledPanel) {
            ((LabeledPanel)comp).setBorder (
               BorderFactory.createLineBorder (Color.LIGHT_GRAY, 1));
         }
         topPanel = getTopPanel(this);

         HasAlignableLabels lcomp = (HasAlignableLabels)comp;
         LabelSpacing spc = new LabelSpacing();
         LabelSpacing top = new LabelSpacing();
         lcomp.getPreferredLabelSpacing (spc);
         topPanel.getLabelSpacing (top);
         top.labelWidth -= getLeftInsetToTop();

         if (spc.labelWidth > top.labelWidth || spc.preSpacing > top.preSpacing) {
            resetNeeded = true;
         }
         else {
            if (spc.expand (top)) {
               lcomp.setLabelSpacing (spc);
            }

         }
      }
      if (comp instanceof JComponent) {
         JComponent jcomp = (JComponent)comp;
         jcomp.setAlignmentX (Component.LEFT_ALIGNMENT);
         // Prevent JSeparators from acting as stretchable components:
         if (jcomp instanceof JSeparator) {
            Dimension maxsize = new Dimension(jcomp.getMaximumSize());
            maxsize.height = jcomp.getPreferredSize().height;
            jcomp.setMaximumSize (maxsize);
         }
      }
      myWidgets.add (idx, comp);
      if (resetNeeded) {
         topPanel.resetLabelAlignment ();
      }
      // Add the panel's mouse listeners to the widgets. This is necessary
      // because if the widgets have their own mouseListeners (which they will
      // for instance if they have toolTipText set), then they will intercept
      // mouse events.
      for (MouseListener l : getMouseListeners()) {
         comp.addMouseListener (l);
      }
      for (MouseMotionListener l : getMouseMotionListeners()) {
         comp.addMouseMotionListener (l);
      }
   }

   public boolean removeWidget (int idx) {
      if (idx < 0 || idx >= myWidgets.size()) {
         throw new ArrayIndexOutOfBoundsException (
            "widget "+idx+" does not exist");
      }
      return removeWidget (myWidgets.get(idx));
   }

   public boolean removeWidget (Component comp) {
      // find the index of the component in question
      if (doRemoveWidget (comp)) {
         myNumBasicWidgets--;
         remove (comp);
         return true;
      }
      else {
         return false;
      }
   }

   protected boolean doRemoveWidget (Component comp) {
      if (myWidgets.remove (comp)) {
         for (MouseListener l : getMouseListeners()) {
            comp.removeMouseListener (l);
         }
         for (MouseMotionListener l : getMouseMotionListeners()) {
            comp.removeMouseMotionListener (l);
         }
         if (comp instanceof JComponent) {
            mySelectedWidgets.remove ((JComponent)comp);
         }
         return true;
      }
      else {
         return false;
      }
   }

   public Component[] removeAllWidgets () {
      Component[] widgets = getWidgets();
      myWidgets.clear();
      myNumBasicWidgets = 0;
      mySelectedWidgets.clear();
      removeAll();
      return widgets;
   }

   public void dispose() {
      Component[] widgets = removeAllWidgets();
      for (int i=0; i<widgets.length; i++) {
         if (widgets[i] instanceof Disposable) {
            ((Disposable)widgets[i]).dispose();
         }
      }
   }

   protected void finalize() throws Throwable {
      dispose();
      super.finalize();
   }

   protected void highlightComponent (Component comp) {
      if (comp instanceof LabeledComponentBase) {
         ((LabeledComponentBase)comp).setSelected (true);
      }
      else {
         comp.setBackground (comp.getBackground().darker());
      }
      comp.repaint();
   }

   protected void dehighlightComponent (Component comp, Color color) {
      if (comp instanceof LabeledComponentBase) {
         ((LabeledComponentBase)comp).setSelected (false);
      }
      else {
         comp.setBackground (color);
      }
      comp.repaint();
   }

   protected ArrayList<String> getMenuActionCommands() {
      ArrayList<String> actions = new ArrayList<String>();

      if (mySelectedWidgets.size() == 1) {
         actions.add ("add separator");
      }
      boolean propertiesPresent = false;
      boolean slidersPresent = false;
      // add "set properties" if some components have properties
      // add "autofit sliderRange" if some components are sliders
      for (JComponent comp : mySelectedWidgets) {
         if (comp instanceof LabeledComponentBase) {
            propertiesPresent = true;
         }
         if (comp instanceof NumericSlider) {
            slidersPresent = true;
         }
      }
      if (propertiesPresent) {
         actions.add ("set properties");
      }
      if (slidersPresent) {
         actions.add ("reset sliderRange");
      }
      if (mySelectedWidgets.size() > 0) {
         actions.add ("delete");
      }
      return actions;
   }

   private void addMenuItem (JPopupMenu menu, String cmd) {
      JMenuItem item = new JMenuItem (cmd);
      item.addActionListener (this);
      item.setActionCommand (cmd);
      menu.add (item);
   }

   protected JPopupMenu createComponentPopup() {
      JPopupMenu popup = new JPopupMenu();
      popup.addPopupMenuListener (this);
      if (mySelectedWidgets.size() == 1) {
         JComponent comp = mySelectedWidgets.iterator().next();
         if (comp instanceof LabeledComponentBase) {
            // add actions that are specific to the component
            LabeledComponentBase lcomp = (LabeledComponentBase)comp;
            ArrayList<String> compActions = lcomp.getActions();
            if (compActions != null && compActions.size() > 0) {
               for (String cmd : compActions) {
                  addMenuItem (popup, cmd);
               }
               popup.add (new JSeparator());
            }
         }
      }
      // add generic actions to the popup
      ArrayList<String> actions = getMenuActionCommands();
      if (actions != null) {
         for (String cmd : actions) {
            addMenuItem (popup, cmd);
         }
      }
      return popup;
   }

   public void selectWidget (JComponent comp) {
      mySelectedWidgets.add (comp);
      highlightComponent (comp);
   }

   public void deselectWidget (JComponent comp) {
      dehighlightComponent (comp, null);
      mySelectedWidgets.remove (comp);
   }

   public void deselectAllWidgets() {
      for (JComponent comp : mySelectedWidgets) {
         dehighlightComponent (comp, null);
      }
      mySelectedWidgets.clear();
   }

   public JComponent findWidget (MouseEvent e) {
      if (myWidgets.contains (e.getSource())) {
         return (JComponent)e.getSource();
      }
      // Find widgets for mouse events on the JPanel itself. Code will reach
      // here if the widget does not have its own mouse listeners.
      Component comp = getComponentAt (e.getX(), e.getY());
      if (comp instanceof JComponent && myWidgets.contains (comp)) {
         return (JComponent)comp;
      }
      else {
         return null;
      }
   }

   public JComponent findWidgetAtCursor (MouseEvent e) {
      int x = e.getX();
      int y = e.getY();
      if (e.getSource() != this && e.getSource() instanceof Component) {
         Rectangle bounds = ((Component)e.getSource()).getBounds();
         x += bounds.x;
         y += bounds.y;
      }
      Component comp = getComponentAt (x, y);
      if (comp instanceof JComponent && myWidgets.contains (comp)) {
         return (JComponent)comp;
      }
      else {
         return null;
      }
   }

   public int numWidgets() {
      return myWidgets.size();
   }

   private Border mySavedBorder;
   private boolean myDraggingP = false;
   private JComponent myTargetComponent;
   private JComponent myDragDestination;

   public void mouseClicked (MouseEvent e) {
   }

   public void mouseEntered (MouseEvent e) {
   }

   public void mouseExited (MouseEvent e) {
   }

   private boolean selectionIsDraggable() {
      if (!myWidgetsDraggable) {
         return false;
      }
      // make sure that the widget selection is contiguous
      if (mySelectedWidgets.size() == 1) {
         return true;
      }
      else if (mySelectedWidgets.size() > 1) {
         Component[] widgets = getWidgets();
         int first = -1;
         int last = -1;
         for (int i = 0; i < widgets.length; i++) {
            if (mySelectedWidgets.contains (widgets[i])) {
               if (first == -1) {
                  first = i;
               }
               last = i;
            }
         }
         return (last - first + 1) == mySelectedWidgets.size();
      }
      else {
         return false;
      }
   }

   public void mousePressed (MouseEvent e) {
      if (!myWidgetsSelectable) {
         return;
      }
      if (e.getButton() == MouseEvent.BUTTON1) {
         JComponent comp = findWidget (e);
         if (comp != null) {
            myTargetComponent = comp;
            if (mySelectedWidgets.contains (comp) && selectionIsDraggable()) {
               myDraggingP = true;
            }
         }
      }
      else if (e.getModifiersEx() == ButtonMasks.getContextMenuMask()) {
         JPopupMenu popup = createComponentPopup();
         if (popup.getComponentCount() > 0) {
            popup.show (e.getComponent(), e.getX(), e.getY());
         }
      }
   }

   public void mouseReleased (MouseEvent e) {
      if (!myWidgetsSelectable) {
         return;
      }
      if (e.getButton() == MouseEvent.BUTTON1) {
         JComponent comp = findWidgetAtCursor (e);
         if (myDragDestination != null) {
            moveWidgets (myDragDestination);
            myDragDestination.setBorder (mySavedBorder);
            myDragDestination = null;
         }
         else if (comp != null && comp == myTargetComponent) {
            int mods = e.getModifiersEx();
            int mask = (InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
            if ((mods & mask) == InputEvent.CTRL_DOWN_MASK) {
               if (mySelectedWidgets.contains (comp)) {
                  deselectWidget (comp);
               }
               else {
                  selectWidget (comp);
               }
            }
            else if ((mods & mask) == 0) {
               boolean wasSelected = mySelectedWidgets.contains (comp);
               deselectAllWidgets();
               if (!wasSelected) {
                  selectWidget (comp);
               }
            }
         }
         myDraggingP = false;
      }
   }

   protected void moveWidgets (JComponent destComp) {
      Component[] comps = getComponents();
      int firstIdx = -1;
      int lastIdx = -1;
      int destIdx = getComponentIndex (destComp);
      for (int i = 0; i < comps.length; i++) {
         if (mySelectedWidgets.contains (comps[i])) {
            if (firstIdx == -1) {
               firstIdx = i;
            }
            lastIdx = i;
         }
      }
      int idx = getComponentIndex (destComp);
      if (destIdx < firstIdx) {
         for (int i = firstIdx; i <= lastIdx; i++) {
            remove (comps[i]);
            add (comps[i], idx++);
         }
      }
      else // destIdx > lastIdx
      {
         for (int i = lastIdx; i >= firstIdx; i--) {
            remove (comps[i]);
            add (comps[i], idx--);
         }
      }
      repackContainingWindow();
   }

   public void mouseDragged (MouseEvent e) {
      if (myDraggingP && myTargetComponent != null) {
         JComponent comp = findWidgetAtCursor (e);
         if (comp != null && comp != myDragDestination) {
            if (myDragDestination != null) {
               myDragDestination.setBorder (mySavedBorder);
            }
            if (mySelectedWidgets.contains (comp)) {
               myDragDestination = null;
            }
            else {
               mySavedBorder = comp.getBorder();
               comp.setBorder (BorderFactory.createLineBorder (Color.black));
               myDragDestination = comp;
            }
         }
      }
   }

   public void mouseMoved (MouseEvent e) {
   }

   public void repackContainingWindow() {
      revalidate();
      Window w = SwingUtilities.windowForComponent (this);
      if (w != null) {
         w.pack();
      }
   }

   public void actionPerformed (ActionEvent e) {
      String command = e.getActionCommand();

      if (command.equals ("delete")) {
         LinkedList<JComponent> list = new LinkedList<JComponent>();
         list.addAll (mySelectedWidgets);
         for (JComponent comp : list) {
            removeWidget (comp);
         }
         repackContainingWindow();
      }
      else if (command.equals ("add separator")) {
         JComponent comp = mySelectedWidgets.iterator().next();
         addWidget (new JSeparator(), getComponentIndex (comp) + 1);
         repackContainingWindow();
      }
      else if (command.equals ("set properties")) {
         Window win = SwingUtilities.getWindowAncestor (this);
         if (win != null) {
            LinkedList<HasProperties> hosts = new LinkedList<HasProperties>();
            JComponent lastComp = null;
            for (JComponent comp : mySelectedWidgets) {
               if (comp instanceof HasProperties) {
                  hosts.add ((HasProperties)comp);
               }
               lastComp = comp;
            }
            HostList hostList = new HostList (hosts);
            PropertyDialog dialog =
               PropertyDialog.createDialog (
                  win, "Edit properties", hostList, "OK Cancel");

            // PropertyDialog dialog = PropertyDialog.createDialog (
            // win, "Edit properties", new PropertyPanel (comp), "OK Cancel");
            GuiUtils.locateVertically (
               dialog, lastComp, GuiUtils.BELOW);
            GuiUtils.locateHorizontally (dialog, this, GuiUtils.CENTER);
            dialog.setModal (true);
            dialog.setVisible (true);
         }
      }
      else if (command.equals ("reset sliderRange")) {
         for (JComponent comp : mySelectedWidgets) {
            if (comp instanceof NumericFieldSlider) {
               // fix this for regular sliders too?
               NumericFieldSlider nslider = (NumericFieldSlider)comp;
               nslider.setSliderRange (
                  SliderRange.estimateBoundsIfNecessary (
                     nslider.getRange(), nslider.getDoubleValue()));
            }
         }
      }
      else if (mySelectedWidgets.size() == 1) {
         JComponent comp = mySelectedWidgets.iterator().next();
         if (comp instanceof LabeledComponentBase) {
            ((LabeledComponentBase)comp).actionPerformed (e);
         }
      }
      else {
         throw new InternalErrorException ("Unexpected action: " + e);
      }
   }

   public void popupMenuCanceled (PopupMenuEvent e) {
      // System.out.println ("canceled");
      // if (myTargetComponent != null)
      // { dehighlightComponent (myTargetComponent, mySavedComponentColor);
      // }
   }

   public void popupMenuWillBecomeInvisible (PopupMenuEvent e) {
   }

   public void popupMenuWillBecomeVisible (PopupMenuEvent e) {

   }
}
