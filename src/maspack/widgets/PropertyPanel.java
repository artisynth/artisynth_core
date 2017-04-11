/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import java.awt.Component;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JSeparator;

import maspack.properties.EditingProperty;
import maspack.properties.HasProperties;
import maspack.properties.InheritableProperty;
import maspack.properties.Property;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;
import maspack.util.Range;

public class PropertyPanel extends LabeledComponentPanel {
   private static final long serialVersionUID = 2247494936388449756L;
   
   // by default, organize properties by group name
   public static boolean defaultOrganize = true;
   public static boolean defaultExpandable = false;

   String myFormatStr = "%8.3f";

   protected boolean myHasModeControlsP = false;
   protected HashMap<LabeledComponentBase,Property> myWidgetPropMap =
      new HashMap<LabeledComponentBase,Property>();

   private LinkedList<ValueChangeListener> myGlobalValueChangeListeners =
      new LinkedList<ValueChangeListener>();
   private Object mySyncObj;

   public PropertyPanel() {
      super();
      setSpacing (2);
      setStretchable (false);
      // setLayout (new BoxLayout (this, BoxLayout.Y_AXIS));
   }

   public PropertyPanel (Iterable<? extends Property> props) {
      this();
      addWidgets (props);
   }

   public PropertyPanel (HasProperties host) {
      this();
      addWidgets (PropertyUtils.createProperties (host));
   }

   protected boolean isInheritableProperty (Object obj) {
      return (obj instanceof Property &&
              ((Property)obj).getInfo().isInheritable());
   }

   public void addWidgets(Iterable<?> items) {
      addWidgets (items, defaultOrganize, defaultExpandable);
   }
   
   /**
    * Add widgets to the panel.  If {@code organize} is true, then
    * groups properties by their associated "groups" if any.
    */
   public void addWidgets (
      Iterable<?> items, boolean organized, boolean expandable) {
      // removeAll();

      if (!organized) {
         doAddWidgetsFlat(items);
      } else {
         doAddWidgetsFlat(items);
         // doAddWidgetsGrouped(items, expandable);
      }

   }
   
   protected LabeledComponentBase maybeAddWidget (
      List<Component> widgets, Property p, Range range) {
      if (p != null) {
         LabeledComponentBase w = PropertyWidget.create (p);
         if (w == null) {
            //            throw new InternalErrorException (
            //               "Cannot create widget for property " + p.getName());
            System.out.println("Warning: cannot create widget for property " + p.getName());
            return null;
         }
         putWidgetPropMap (w, p);
         widgets.add (w);
         return w;
      }
      else {
         return null;
      }
   }
   
   protected static <E> void addIfUnique(List<E> src, List<E> dest) {
      for (E obj : src) {
         if (!dest.contains(obj)) {
            dest.add(obj);
         }
      }
   }
   
   protected void addSeparator(List<Component> widgets) {
      JSeparator sep = new JSeparator();
      sep.setAlignmentX (JLabel.LEFT_ALIGNMENT);
      widgets.add(sep);
   }
   
   protected void addSection(List<Component> widgets, String name) {
      if (widgets.size() > 0) {
         addSeparator(widgets);
      }
      JLabel label = new JLabel (name);
      label.setHorizontalTextPosition (JLabel.LEFT);
      label.setAlignmentX (JLabel.LEFT_ALIGNMENT);
      GuiUtils.setItalicFont (label);
      widgets.add(label);
   }
   
   protected boolean addSeparator(LinkedList<Component> widgets, int idx) {
      if (idx > 0) {
         JSeparator sep = new JSeparator();
         sep.setAlignmentX (JLabel.LEFT_ALIGNMENT);
         widgets.add(idx, sep);
         return true;
      }
      return false;
   }
   
   /**
    * Returns true if a new section was actually added (non-empty set of widgets)
    */
   protected boolean addSection (
      LinkedList<Component> widgets, int idx, String name) {

      JSeparator sep = new JSeparator();
      sep.setAlignmentX (JLabel.LEFT_ALIGNMENT);
      
      JLabel label = new JLabel (name);
      label.setHorizontalTextPosition (JLabel.LEFT);
      label.setAlignmentX (JLabel.LEFT_ALIGNMENT);
      GuiUtils.setItalicFont (label);
      
      boolean addedSection = false;
      if (addSeparator(widgets, idx)) {
         idx++;
         addedSection = true;
      }
      
      if (name != null && !"".equals(name)) {
         widgets.add (idx, label);
         addedSection = true;
      }
      
      return addedSection;
   }

   public void addWidgets(List<Component> widgets) {
      for (Component comp : widgets) {
         addWidget(comp);
      }
   }
   
   private void doAddWidgetsFlat(Iterable<?> items) {
      for (Object item : items) { // ignore inactive properties
         if (item instanceof Property) {
            Property prop = (Property)item;

            if (isInheritableProperty (prop) &&
                ((InheritableProperty)prop).getMode() == PropertyMode.Inactive) {
               continue;
            }
            addWidget (prop);
         }
         else if (item instanceof Component) {
            addWidget ((Component)item);
         }
      }
   }

   protected void addModeButtonSpaces() {
      for (int i=0; i<myWidgets.size(); i++) {
         if (myWidgets.get(i) instanceof LabeledComponentBase) {
            LabeledComponentBase widget = (LabeledComponentBase)myWidgets.get(i);
            PropertyWidget.addModeButtonSpace (widget);
            widget.revalidate();
         }
      }
   }

   protected void accomodateNewControl (LabeledControl ctrl) {
      PropertyModeButton b = PropertyWidget.getModeButton (ctrl);
      if (b != null) {
         if (!myHasModeControlsP) {
            myHasModeControlsP = true;
            addModeButtonSpaces();
         }
         PropertyWidget.setModeAppearance (
            (LabeledControl)ctrl, this, b.getProperty().getMode());
      }
      for (ValueChangeListener l : myGlobalValueChangeListeners) {
         addValueChangeListener (ctrl, l);
      }
   }
   
   protected void accomodateNewPanel (PropertyPanel panel) {
      for (ValueChangeListener l : myGlobalValueChangeListeners) {
         panel.addGlobalValueChangeListener(l);
      }
   }

   public Component addWidget (Component comp, int idx) {
      if (comp instanceof LabeledControl) {
         accomodateNewControl ((LabeledControl)comp);
      } else if (comp instanceof PropertyPanel) {
         accomodateNewPanel((PropertyPanel)comp);
      }
      return super.addWidget (comp, idx);
   }

   public void addPropertyWidget (Property prop, LabeledComponentBase widget) {
      addPropertyWidget (prop, widget, myNumBasicWidgets);
   }

   public void processPropertyWidget (
      Property prop, LabeledComponentBase widget) {
      if (prop.getInfo().isInheritable()) {
         if (!myHasModeControlsP) {
            myHasModeControlsP = true;
            addModeButtonSpaces();
         }
      }
      myWidgetPropMap.put (widget, prop);
      if (myHasModeControlsP) {
         PropertyWidget.addModeButtonOrSpace (widget, prop);
      }      
   }      

   public void addPropertyWidget (
      Property prop, LabeledComponentBase widget, int idx) {
      processPropertyWidget (prop, widget);
      addWidget (widget, idx);
   }

   public LabeledComponentBase getPropertyWidget (String propName) {
      for (Map.Entry<LabeledComponentBase,Property> ent :
              myWidgetPropMap.entrySet()) {
         if (ent.getValue().getName().equals (propName)) {
            return ent.getKey();
         }
      }
      return null;
   }

   public LabeledComponentBase addWidget (
      Property prop, double min, double max) {
      LabeledComponentBase widget = PropertyWidget.create (prop, min, max);
      if (widget != null) {
         addPropertyWidget (prop, widget);
      }
      return widget;
   }

   public LabeledComponentBase addWidget (Property prop) {
      LabeledComponentBase widget = PropertyWidget.create (prop);
      if (widget != null) {
         addPropertyWidget (prop, widget);
      }
      return widget;
   }

   private void disconnectControl(LabeledControl comp) {
      for (ValueChangeListener l : myGlobalValueChangeListeners) {
         removeValueChangeListener(comp, l);
      }
   }
   
   private void disconnectPanel(CompositePropertyPanel comp) {
      for (ValueChangeListener l : myGlobalValueChangeListeners) {
         removeValueChangeListener(comp, l);
      }
   }
   
   private void disconnectPanel(PropertyPanel panel) {
      for (ValueChangeListener l : myGlobalValueChangeListeners) {
         removeValueChangeListener(panel, l);
      }
   }
   
   public boolean removeWidget (Component comp) {
      if (super.removeWidget (comp)) {
         if (comp instanceof LabeledComponentBase) {
            myWidgetPropMap.remove ((LabeledComponentBase)comp);
         }
         if (comp instanceof LabeledControl) {
            disconnectControl((LabeledControl)comp);
         } else if (comp instanceof CompositePropertyPanel) {
            disconnectPanel((CompositePropertyPanel)comp);
         } else if (comp instanceof PropertyPanel) {
            disconnectPanel((PropertyPanel)comp);
         }
         return true;
      }
      else {
         return false;
      }
   }

   public Component[] removeAllWidgets () {
      myWidgetPropMap.clear();
      return super.removeAllWidgets();
   }

   protected void putWidgetPropMap (LabeledComponentBase ctrl, Property prop) {
      myWidgetPropMap.put (ctrl, prop);
   }

   protected void removeWidgetPropMap (LabeledComponentBase ctrl) {
      myWidgetPropMap.remove (ctrl);
   }

   public void updateWidgetValues() {
      updateWidgetValues (true);
   }

   /** 
    * Update the widgets in this panel so that they reflect the values of the
    * underlying properties.
    *
    * <p>Underlying properties which are instances of EditingProperty will
    * first normally update their own values from their source component(s).
    * In some cases it may be desirable to suppress this behavior, which can be
    * done by setting <code>updateFromSource</code> to <code>false</code>.
    * 
    * @param updateFromSource if <code>false</code>, do not update the values
    * of EditingProperties from their underlying source component(s).
    */
   public void updateWidgetValues (boolean updateFromSource) {
      for (int i=0; i<myWidgets.size(); i++) {
         if (myWidgets.get(i) instanceof LabeledControl) {
            LabeledControl widget = (LabeledControl)myWidgets.get(i);
            Property prop = myWidgetPropMap.get (widget);
            if (prop != null) { 
               // Editting properties keep cached copies of
               // their values, which need to be updated from the source
               if (prop instanceof EditingProperty && updateFromSource) {
                  ((EditingProperty)prop).updateValue();
               }
               PropertyWidget.updateValue (widget, prop);
            }
         }
         else if (myWidgets.get(i) instanceof LabeledPanel) {
            ((LabeledPanel)myWidgets.get(i)).updateWidgetValues (updateFromSource);
         }
      }
   }

//   private boolean isConnectedToRoot (ModelComponent comp) {
//      while (comp != null) {
//         if (comp instanceof RootModel) {
//            return true;
//         }
//         comp = comp.getParent();
//      }
//      return false;
//   }

//   private boolean isStale (Property prop) {
//      ModelComponent comp = ComponentUtils.getPropertyComponent (prop);
//      if (comp == null) {
//         System.out.println ("no comp");
//         return true;
//      }
//      if (!PropertyUtils.isConnectedToHierarchy (prop)) {
//         System.out.println ("not connected");
//         return true;
//      }
//      if (!isConnectedToRoot (comp)) {
//         System.out.println ("not in root");
//         return true;
//      }
//      return false;
//   }

   public Property getWidgetProperty (LabeledComponentBase comp) {
      return myWidgetPropMap.get (comp);
   }

//   public boolean removeStalePropertyWidgets() {
//      LinkedList<LabeledComponentBase> removeList =
//         new LinkedList<LabeledComponentBase>();
//      for (int i=0; i<myWidgets.size(); i++) {
//         if (myWidgets.get(i) instanceof LabeledComponentBase) {
//            LabeledComponentBase widget = (LabeledComponentBase)myWidgets.get(i);
//            Property prop = myWidgetPropMap.get (widget);
//            if (prop != null && !(prop instanceof EditingProperty)) {
//               if (isStale (prop)) {
//                  removeList.add (widget);
//               }
//            }
//         }
//      }
//      if (removeList.size() > 0) {
//         for (LabeledComponentBase widget : removeList) {
//            removeWidget (widget);
//            //myWidgetPropMap.remove (widget);
//         }
//         return true;
//      }
//      else {
//         return false;
//      }
//   }

   public static void addValueChangeListener (
      LabeledControl ctrl, ValueChangeListener l) {
      ctrl.addValueChangeListener (l);
      PropertyModeButton b = PropertyWidget.getModeButton (ctrl);
      if (b != null) {
         b.addValueChangeListener (l);
      }
   }

   public void addGlobalValueChangeListener (ValueChangeListener l) {
      myGlobalValueChangeListeners.add (l);
      for (int i=0; i<myWidgets.size(); i++) {
         if (myWidgets.get(i) instanceof LabeledControl) {
            addValueChangeListener ((LabeledControl)myWidgets.get(i), l);
         } else if (myWidgets.get(i) instanceof CompositePropertyPanel) {
            // descend
            CompositePropertyPanel cp = (CompositePropertyPanel)myWidgets.get(i);
            cp.addGlobalValueChangeListener(l);
         } else if (myWidgets.get(i) instanceof CompositePropertyWidget) {
            CompositePropertyWidget cw =
               (CompositePropertyWidget)myWidgets.get(i);
            cw.addValueChangeListener(l);
         } else if (myWidgets.get(i) instanceof PropertyPanel) {
            PropertyPanel panel = (PropertyPanel)myWidgets.get(i);
            panel.addGlobalValueChangeListener(l);
         }
      }
   }

   public static void removeValueChangeListener (
      LabeledControl ctrl, ValueChangeListener l) {
      ctrl.removeValueChangeListener (l);
      PropertyModeButton b = PropertyWidget.getModeButton (ctrl);
      if (b != null) {
         b.removeValueChangeListener (l);
      }
   }
   
   private void removeValueChangeListener (
      CompositePropertyPanel ctrl, ValueChangeListener l) {
      ctrl.removeGlobalValueChangeListener (l);
   }
   
   private void removeValueChangeListener ( PropertyPanel panel,
      ValueChangeListener l) {
      panel.removeGlobalValueChangeListener(l);
   }

   public boolean removeGlobalValueChangeListener (ValueChangeListener l) {
      if (myGlobalValueChangeListeners.remove (l)) {
         for (int i=0; i<myWidgets.size(); i++) {
            if (myWidgets.get(i) instanceof LabeledControl) {
               removeValueChangeListener ((LabeledControl)myWidgets.get(i), l);
            } else if (myWidgets.get(i) instanceof CompositePropertyPanel) {
               removeValueChangeListener ((CompositePropertyPanel)myWidgets.get(i), l);
            } else if (myWidgets.get(i) instanceof PropertyPanel) {
              removeValueChangeListener((PropertyPanel)myWidgets.get(i), l);
            }
         }
         return true;
      }
      else {
         return false;
      }
   }

   public Collection<ValueChangeListener> getGlobalValueChangeListeners() {
      return myGlobalValueChangeListeners;
   }

   void fireGlobalValueChangeListeners (ValueChangeEvent ev) {
      for (ValueChangeListener l : myGlobalValueChangeListeners) {
         l.valueChange (ev);
      }
   }

   public Object getSynchronizeObject() {
      return mySyncObj;
   }

   public void setSynchronizeObject (Object syncObj) {
      mySyncObj = syncObj;
      for (int i=0; i<myWidgets.size(); i++) {
         if (myWidgets.get(i) instanceof LabeledControl) {
            PropertyWidget.setSynchronizeObject (
               (LabeledControl)myWidgets.get(i), syncObj);
         }
      }
   }

}
