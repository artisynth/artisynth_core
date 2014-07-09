/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import java.awt.Component;

import maspack.properties.InheritableProperty;
import maspack.properties.Property;
import maspack.properties.PropertyMode;

public class ExpandablePropertyPanel extends PropertyPanel
   implements ValueChangeListener {
   private static final long serialVersionUID = 3795282939442055050L;
   protected int myNumExtraWidgets = 0;
   protected LabeledToggleButton myExpandButton;
   protected boolean myExpandedP;

   public static LabeledToggleButton createExpandButton () {
      LabeledToggleButton button =
         new LabeledToggleButton (
            "more ...", false, 
            GuiUtils.loadIcon (
               ExpandablePropertyPanel.class, "icons/ExpandIcon.png"), 
            GuiUtils.loadIcon (
               ExpandablePropertyPanel.class, "icons/ContractIcon.png"));
      return button;
   }

   public ExpandablePropertyPanel() {
      super();
      myExpandButton = createExpandButton();
      myExpandButton.addValueChangeListener (this);
      doAddWidget (myExpandButton, 0);
      add (myExpandButton);
   }

   public ExpandablePropertyPanel (Iterable<? extends Property> props) {
      this();
      addWidgets (props);
   }

   public ExpandablePropertyPanel (
      Iterable<? extends Property> props, Iterable<? extends Property> extras) {
      this();
      addWidgets (props);
      addExtraWidgets (extras);
   }

   public void addExtraWidgets (Iterable<?> items) {
      // removeAll();

      for (Object item : items) { // ignore inactive properties
         if (item instanceof Property) {
            Property prop = (Property)item;

            if (isInheritableProperty (prop) &&
                ((InheritableProperty)prop).getMode() == PropertyMode.Inactive) {
               continue;
            }
            addExtraWidget (prop);
         }
         else if (item instanceof Component) {
            addExtraWidget ((Component)item);
         }
      }
   }

   private void addExtraPropertyWidget (
      Property prop, LabeledComponentBase widget) {
      addExtraPropertyWidget (prop, widget, myNumExtraWidgets);
   }

   private void addExtraPropertyWidget (
      Property prop, LabeledComponentBase widget, int idx) {
      processPropertyWidget (prop, widget);
      addExtraWidget (widget, idx);
   }

   public LabeledComponentBase addExtraWidget (
      Property prop, double min, double max) {
      LabeledComponentBase widget = PropertyWidget.create (prop, min, max);
      if (widget != null) {
         addExtraPropertyWidget (prop, widget);
      }
      return widget;
   }

   public LabeledComponentBase addExtraWidget (Property prop) {
      LabeledComponentBase widget = PropertyWidget.create (prop);
      if (widget != null) {
         addExtraPropertyWidget (prop, widget);
      }
      return widget;
   }

   public Component addExtraWidget (String labelText, LabeledComponentBase comp) {
      comp.setLabelText (labelText);
      addExtraWidget (comp, myNumExtraWidgets);
      return comp;
   }

   public Component addExtraWidget (Component comp) {
      addExtraWidget (comp, myNumExtraWidgets);
      return comp;
   }

   public Component addExtraWidget (Component comp, int idx) {
      if (idx > myNumExtraWidgets) {
         throw new IllegalArgumentException (
            "idx "+idx+" exceeds number of extra widgets " + myNumExtraWidgets);
      }
      if (comp instanceof LabeledControl) {
         accomodateNewControl ((LabeledControl)comp);
      }      
      doAddWidget (comp, myNumBasicWidgets+1+idx);
      myNumExtraWidgets++;
      if (myExpandedP) {
         add (comp, myNumBasicWidgets+1+idx);
      }
      return comp;
   }

   public boolean removeWidget (Component comp) {
      int idx = myWidgets.indexOf (comp);
      if (idx == -1) {
         return false;
      }
      if (idx < myNumBasicWidgets) {
         super.removeWidget (comp);
      }
      else {
         doRemoveWidget (comp);
         myNumExtraWidgets--;
         if (myExpandedP) {
            remove (comp);
         }
      }
      return true;
   }

   public Component[] removeAllWidgets () {
      myNumExtraWidgets = 0;
      return super.removeAllWidgets();
   }

   protected void doSetExpanded (boolean expanded) {
      if (expanded != myExpandedP) {
         remove (myExpandButton);
         if (expanded) {
            for (int i=0; i<myNumExtraWidgets; i++) {
               int k = myNumBasicWidgets+1+i;
               add (myWidgets.get(k));
            }
         }
         else {
            for (int i=0; i<myNumExtraWidgets; i++) {
               int k = myNumBasicWidgets+1+i;
               remove (myWidgets.get(k));
            }
         }
         add (myExpandButton);
         myExpandedP = expanded;
         updateExpandButton(expanded);
         repackContainingWindow();
      }
   }
   
   protected void updateExpandButton(boolean expanded) {
      if (expanded) {
         myExpandButton.setLabelText ("less ...");
      } else {
         myExpandButton.setLabelText ("more ...");
      }
   }

   public void valueChange (ValueChangeEvent evt) {
      Object source = evt.getSource();

      if (source == myExpandButton) {
         doSetExpanded (myExpandButton.getBooleanValue());
      }
   }

   public static void main (String[] args) {
      PropertyFrame frame = new PropertyFrame ("ExpandableTest");
      ExpandablePropertyPanel panel = new ExpandablePropertyPanel();

      panel.addWidget (new StringField ("name", 20));
      panel.addWidget (new DoubleField ("theta"));
      panel.addExtraWidget (new DoubleField ("gamma"));

      frame.setPanel (panel);
      frame.pack();
      frame.setVisible(true);
   }
}
