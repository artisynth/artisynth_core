/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.gui.widgets;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JOptionPane;

import artisynth.core.driver.Main;
import artisynth.core.gui.Displayable;
import artisynth.core.gui.selectionManager.SelectionEvent;
import artisynth.core.gui.selectionManager.SelectionListener;
import artisynth.core.gui.selectionManager.SelectionManager;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.workspace.RootModel;
import maspack.properties.CompositeProperty;
import maspack.properties.HasProperties;
import maspack.properties.NumericConverter;
import maspack.properties.Property;
import maspack.properties.PropertyInfo;
import maspack.properties.PropertyInfoList;
import maspack.properties.PropertyUtils;
import maspack.util.BooleanHolder;
import maspack.util.InternalErrorException;
import maspack.util.StringHolder;
import maspack.widgets.ButtonCreator;
import maspack.widgets.GuiUtils;
import maspack.widgets.LabeledTextField;
import maspack.widgets.PropertyWidget;
import maspack.widgets.StringSelector;
import maspack.widgets.ValueChangeEvent;
import maspack.widgets.ValueChangeListener;

/**
 * Base class for ComponentField, which selects component paths, and
 * PropertyFields, which manages property paths. The two classes are almost
 * identical except that the latter also contains a property selection field.
 * The code for managing the property selection field is contained in this base
 * class, because of it's tight coupling with the component text field.
 */
public abstract class ComponentPropertyField extends LabeledTextField implements
   SelectionListener {
   
   private static final long serialVersionUID = 1L;
   
   Object myValue = Property.VoidValue;
   
   SelectionManager mySelectionManager;
   boolean mySelectionEnabled = true;
   boolean mySelectionListenerInstalled = false;
   boolean mySelectionListenerMasked = false;
   boolean myPropertiesAllowed = false;
   StringSelector myPropertySelector;
   String nullString = "                ";
   boolean myAddParentButton = true;
   JButton myParentButton;
   HasProperties myLastSelectorHost = null;
   Main myMain;
   String myLastPropName = null;
   private boolean myIgnoreDeselection = false;
   protected boolean myNumericOnly = false;
   protected boolean myWidgetableOnly = true;

   protected boolean myPropertyMask = false;
   
   static ImageIcon myParentButtonIcon =
      GuiUtils.loadIcon (Displayable.class, "icon/upArrow.png");

   /**
    * Creates a ComponentPropertyField with specified label text.
    * 
    * @param labelText
    * text for the control label
    * @param ncols
    * approximate width of the text field in columns
    */
   public ComponentPropertyField (String labelText, int ncols, Main main) {
      super (labelText, ncols);
      
      myMain = main;
      myAlwaysParseText = true;
      mySelectionManager = main.getSelectionManager();
      mySelectionManager.addSelectionListener (this);
      mySelectionListenerInstalled = true;
      
      setStretchable (true);
      setGUIVoidEnabled (true);
      setVoidValueEnabled (true);
      updateDisplay();

      // addParentButton();
      // addPropertySelector();

      // setValue (mySelectionManager.getLastSelected());
      // updateDisplay();
   }

   /**
    * Sets this control to ignore deselection events. This means that any
    * selected component/property will persist until the next selection event,
    * or the contents of the text box are changed.
    * 
    * @param ignore
    * if true, ignore deselection events
    */
   public void setIgnoreDeselection (boolean ignore) {
      myIgnoreDeselection = ignore;
   }

   /**
    * Returns true if deselection events are ignored.
    * 
    * @return true if deselection events are ignored.
    */
   public boolean getIgnoreDeselection() {
      return myIgnoreDeselection;
   }

   protected void updateValueAndDisplay (Object obj) {
      if (obj instanceof Property) {
         myLastPropName = ((Property)obj).getName();
      }
      if (updateValue (obj)) {
         updateDisplay();
      }
   }
   
   @Override
   public String getText() {
      String textFieldText = myTextField.getText();
      String property = null;
      if (myPropertySelector != null) {
         property = (String)(myPropertySelector.getValue());
         if (property != null) {
            property = property.trim();
         }
      }
      StringBuilder out = new StringBuilder(textFieldText);
      if (!textFieldText.contains(":") && property != null && !"".equals(property)) {
         out.append(':');
         out.append(property);
      }
      return out.toString();
   }
   
   protected void setValueFromDisplay() {
      if (myAlwaysParseText || !myLastText.equals (myTextField.getText())
         || (myPropertySelector != null 
           && !((String)(myPropertySelector.getValue())).equals(myLastPropName))) {
         
         StringHolder errMsg = new StringHolder();
         // we explicitly call fireValueCheckListeners instead of
         // checkValue since textToValue may itself throw an error
         // and we want to handle that in the same way
         BooleanHolder corrected = new BooleanHolder();
         Object value = textToValue (getText(), corrected, null);
         if (value != Property.IllegalValue) {
            value = validateValue (value, errMsg);
         }
         if (value == Property.IllegalValue) {
            focusListenerMasked = true;
            GuiUtils.showError (this, errMsg.value);
            focusListenerMasked = false;
            myTextField.setText (myLastText);
            updateDisplay();
            myLastEntryAccepted = false;
            return;
         }
         updateValue (value);
         updateDisplay();
      }
      else {
         setReverseTextBackground (false);
      }
      myLastEntryAccepted = true;
   }
   
   /**
    * Returns true if two control values are equal, allowing for values to be
    * null and void, as well as various vector, matrix, and object values. This
    * method is used by updateInternalValue method of the specific control. Care
    * should be taken when using this method to verify that its equality checks
    * are appropriate.
    */
   protected boolean valuesEqual (Object obj1, Object obj2) {
      // return super.valuesEqual(obj1, obj2);
      if (obj1 == null && obj2 == null) {
         return true;
      } else if (obj1 == null || obj2 == null) {
         return false;
      } else if (obj1.getClass() != obj2.getClass()) {
         return false;
      } else  {
         return obj1.equals(obj2);
      }
   }
   
   /**
    * Override to account for property selector
    */
   protected void updateDisplay (boolean forceUpdate) {
      String [] newText = null;
      
      Object value = getInternalValue();
      BooleanHolder corrected = new BooleanHolder();
      if (value == Property.VoidValue) {
         newText = new String[]{"",""};
      }
      else {

         Object textValue = textToValue (getText(), corrected, null);
         if (forceUpdate ||
             textValue == Property.IllegalValue ||
             corrected.value ||
             !valuesEqual (textValue, value)) {
            
            // update
            newText = valueToTextArray(value);
            
         }
      }
      if (newText != null) {
         myLastText = newText[0];
         myTextField.setText (newText[0]);
         // mask property selector
         
         if (myPropertiesAllowed) {
            myPropertyMask = true;
            myLastPropName = newText[1];
            if ("".equals(newText[1])) {
                myPropertySelector.setValue(nullString);
            } else {
               myPropertySelector.setValue(newText[1]);
            }
            myPropertyMask = false;
         } else {
            if (newText[1] != null && !"".equals(newText[1])) {
               myLastText = newText[0] + ":" + newText[1];
               myTextField.setText(myLastText);
            }
         }
      }
      else {
         myLastText = myTextField.getText();
         if (myPropertySelector != null) {
            String propValue = (String)(myPropertySelector.getValue());
            myLastPropName = propValue;
         }
      }
      setReverseTextBackground (false);
   }

   protected void addParentButton() {
      myParentButton =
         ButtonCreator.createIconicButton (
            myParentButtonIcon, "Select parent",
            "Select component or property parent", true, false,
            new ActionListener() {
               public void actionPerformed (ActionEvent e) {
                  Object parent = getHostParent();
                  if (parent != null) {
                     updateValueAndDisplay (parent);
                  }
               }
            });
      GuiUtils.setFixedSize (myParentButton, 20, 20);
      addMajorComponent (myParentButton, 1);
   }

   protected void addPropertySelector() {
      myPropertySelector = new StringSelector ("", new String[] { nullString });
      myPropertySelector.setValue (nullString);
      myPropertySelector.addValueChangeListener (new ValueChangeListener() {
         public void valueChange (ValueChangeEvent e) {
            setValueFromPropertySelector();
         }
      });
      myPropertiesAllowed = true;
      addMajorComponent (myPropertySelector);
   }

   private Property getPropertyForComposite (CompositeProperty cprop) {
      PropertyInfo info = cprop.getPropertyInfo();
      HasProperties host = cprop.getPropertyHost();
      Property prop = host.getProperty (info.getName());
      if (prop == null) {
         throw new InternalErrorException (
            "Property not found in composite property host");
      }
      return prop;
   }

   private Object getHostParent() {
      HasProperties host = getHost();
      if (host == null) {
         return null;
      }
      else if (host instanceof ModelComponent) {
         return ((ModelComponent)host).getParent();
      }
      else if (host instanceof CompositeProperty) {
         HasProperties superHost = ((CompositeProperty)host).getPropertyHost();
         if (superHost instanceof ModelComponent) {
            return superHost;
         }
         else if (superHost instanceof CompositeProperty) {
            return getPropertyForComposite ((CompositeProperty)superHost);
         }
      }
      else {
         throw new InternalErrorException ("Host is " + host.getClass()
         + " instead of ModelComponent or CompositeProperty");
      }
      return null;
   }

   private void setValueFromPropertySelector() {
      if (!myPropertyMask) {
         String propName = (String)myPropertySelector.getValue();
         HasProperties host = getHost();
         if (propName.equals (nullString)) {
            Object value = getValueForHost();
            System.out.println ("new value");
            updateValueAndDisplay (value);
            return;
         }
         if (host == null) {
            throw new InternalErrorException ("Current property host is null");
         }
         Property prop = host.getProperty (propName);
         if (prop == null) {
            throw new InternalErrorException (
               "Current property host does not contain property " + propName);
         }
         updateValueAndDisplay (prop);
      }
   }

   protected Object textToValue (
      String text, BooleanHolder corrected, StringHolder errMsg) {
      
      corrected.value = false;
      RootModel root = myMain.getRootModel();
      Object compOrProp = null;
      if (root == null || text == null || text.equals ("")) {
         return validValue (null, errMsg);
      }
      if (!myPropertiesAllowed) {
         compOrProp = root.findComponent (text);
         if (compOrProp == null) {
            return illegalValue ("No such component", errMsg);
         }
      }
      else {
         boolean colonInText = (text.indexOf (':') != -1);
         if (!colonInText) {
            compOrProp = root.findComponent (text);
            // if (compOrProp != null) { 
            // // match with last property if possible
            //    if (myLastPropName != null) {
            //       Property prop =
            //          ((ModelComponent)compOrProp).getProperty (myLastPropName);
            //       if (prop != null) {
            //          compOrProp = prop;
            //       }
            // }
         }
         if (compOrProp == null) {
            compOrProp = root.getProperty (text);
         }
         if (compOrProp == null) {
            if (colonInText) {
               return illegalValue (
                  "Property does not exist or is inaccessible", errMsg);
            }
            else {
               return illegalValue ("No such component or property", errMsg);
            }
         }
      }
      return validValue (compOrProp, errMsg);
   }
   
   protected String[] valueToTextArray(Object value) {
      ModelComponent comp = getComponent (value);
      RootModel root = myMain.getRootModel();
      
      String compStr = "/";
      String propStr = "";
      
      if (root == null || comp == null) {
         return new String[]{"",""};
      }
      if (value instanceof ModelComponent) {
         compStr = ComponentUtils.getPathName (root, comp);
      }
      else if (value instanceof Property) {
         Property prop = (Property)value;
         boolean excludeLeaf =
            (myPropertySelector != null &&
             !(prop.get() instanceof CompositeProperty));
         String path = ComponentUtils.getPropertyPathName (prop, root, false);
         
         int idx = path.indexOf(':');
         // default to root
         propStr = path;
         if (idx >= 0) {
            compStr = path.substring(0, idx);
            if (idx < path.length()-1) {
               propStr = path.substring(idx+1);
            }
         }
      }
      else {
         throw new InternalErrorException ("Unknown value type: "
         + value.getClass());
      }
      return new String[]{compStr, propStr};
   }

   protected String valueToText (Object value) {
      ModelComponent comp = getComponent (value);
      RootModel root = myMain.getRootModel();
      if (root == null || comp == null) {
         return "";
      }
      if (value instanceof ModelComponent) {
         return ComponentUtils.getPathName (root, comp);
      }
      else if (value instanceof Property) {
         Property prop = (Property)value;
         boolean excludeLeaf =
            (myPropertySelector != null &&
             !(prop.get() instanceof CompositeProperty));
         String path = ComponentUtils.getPropertyPathName (prop, root, false);
         if (!path.contains(":")) {
            path = "/:" + path;  // root component + property
         }
         return path;
      }
      else {
         throw new InternalErrorException ("Unknown value type: "
         + value.getClass());
      }
      // String text = ComponentUtils.getPathName(root, comp);
      // if (value instanceof Property)
      // { Property prop = (Property)value;
      // String propPath = getPropertyPath (prop, myPropertySelector != null);
      // if (propPath.length() > 0)
      // { text += "/" + propPath;
      // }
      // }
      // return text;
   }

   public String getLeafPropName() {
      if (myValue instanceof Property) {
         Property prop = (Property)myValue;
         if (prop.get() instanceof CompositeProperty) {
            return nullString;
         }
         else {
            return prop.getName();
         }
      }
      else {
         return nullString;
      }
   }

   public HasProperties getHost() {
      if (myValue instanceof ModelComponent) {
         return (HasProperties)myValue;
      }
      else if (myValue instanceof Property) {
         Property prop = (Property)myValue;
         Object value = prop.get();
         if (value instanceof CompositeProperty) {
            return (CompositeProperty)value;
         }
         else {
            return prop.getHost();
         }
      }
      else if (myValue == null || myValue == Property.VoidValue) {
         return null;
      }
      else {
         throw new InternalErrorException ("internal value is an instance of "
         + myValue.getClass());
      }
   }

   public Object getValueForHost() {
      if (myValue instanceof ModelComponent) {
         return myValue;
      }
      else if (myValue instanceof Property) {
         Property prop = (Property)myValue;
         Object value = prop.get();
         if (value instanceof CompositeProperty) {
            return prop;
         }
         else {
            HasProperties host = prop.getHost();
            if (host instanceof ModelComponent) {
               return host;
            }
            else if (host instanceof CompositeProperty) {
               CompositeProperty cprop = (CompositeProperty)host;
               return cprop.getPropertyHost().getProperty (
                  cprop.getPropertyInfo().getName());
            }
            else {
               throw new InternalErrorException ("host is an instance of "
               + host.getClass());
            }
         }
      }
      else {
         return null;
      }
   }

   private ModelComponent getComponent (Object compOrProp) {
      if (compOrProp == null || compOrProp == Property.VoidValue) {
         return null;
      }
      else if (compOrProp instanceof ModelComponent) {
         return (ModelComponent)compOrProp;
      }
      else if (compOrProp instanceof Property) {
         return ComponentUtils.getPropertyComponent ((Property)compOrProp);
      }
      else {
         throw new IllegalArgumentException (
            "object must be a ModelComponent or a Property");
      }
   }

   public ModelComponent getModelComponent() {
      return getComponent (myValue);
   }

   public Property getProperty() {
      if (myValue instanceof Property) {
         return (Property)myValue;
      }
      else {
         return null;
      }
   }

   public PropertyInfo getPropertyInfo() {
      if (myValue instanceof Property) {
         return ((Property)myValue).getInfo();
      }
      else {
         return null;
      }
   }

   protected boolean updateInternalValue (Object value) {
      ModelComponent newComp = getComponent (value);
      if (mySelectionEnabled) {
         if (newComp != null &&
             mySelectionManager.getLastSelected() != newComp) {
            mySelectionListenerMasked = true;
            mySelectionManager.clearSelections();
            mySelectionManager.addSelected (newComp);
            mySelectionListenerMasked = false;
         }
      }
      if (!valuesEqual (value, myValue)) {
//          ModelComponent oldComp = getComponent (myValue);
//          ModelComponent newComp = getComponent (value);
         myValue = value;
//          if (mySelectionEnabled) {
//             if (oldComp != newComp &&
//                 newComp != null &&
//                 mySelectionManager.getLastSelected() != newComp) {
//                mySelectionListenerMasked = true;
//                mySelectionManager.clearSelections();
//                mySelectionManager.addSelected (newComp);
//                mySelectionListenerMasked = false;
//             }
//          }
         if (myPropertySelector != null) {
            updatePropertySelector();
         }
         return true;
      }
      else {
         return false;
      }
   }

   protected boolean isWithinHierarchy (ModelComponent comp) {
      RootModel root = myMain.getRootModel();
      return (root != null && ComponentUtils.withinHierarchy (comp, root));
   }

   public String getPropertyPath() {
      if (myValue instanceof Property) {
         Property prop = (Property)myValue;
         return ComponentUtils.getPropertyPathName (
            prop, getComponent (prop), /* excludeLeaf= */false);
      }
      else {
         return null;
      }
   }

   // protected String getPropertyPath (Property prop, boolean showOnlyHosts)
   // {
   // StringBuilder bld = new StringBuilder();
   // HasProperties host = prop.getHost();
   // if (!showOnlyHosts || prop.get() instanceof CompositeProperty)
   // { bld.append (prop.getName());
   // }
   // while (host != null && !(host instanceof ModelComponent))
   // { if (!(host instanceof CompositeProperty))
   // { throw new IllegalArgumentException ("Property is inaccessible");
   // }
   // CompositeProperty cprop = (CompositeProperty)host;
   // HasProperties superHost = cprop.getPropertyHost();
   // if (superHost == null)
   // { throw new IllegalArgumentException ("Property is inaccessible");
   // }
   // if (bld.length() != 0)
   // { bld.insert (0, '.');
   // }
   // bld.insert (0, cprop.getPropertyInfo().getName());
   // host = superHost;
   // }
   // return bld.toString();
   // }

   protected Object validateValue (Object value, StringHolder errMsg) {
      if (value != null && value != Property.VoidValue) {
         if (value instanceof ModelComponent) {
            ModelComponent comp = (ModelComponent)value;
            if (!isWithinHierarchy (comp)) {
               return illegalValue (
                  "component not contained within model hierarchy", errMsg);
            }
         }
         else if (myPropertiesAllowed && value instanceof Property) {
            Property prop = (Property)value;
            ModelComponent comp = ComponentUtils.getPropertyComponent (prop);
            if (comp == null) {
               return PropertyUtils.illegalValue (
                  "property does not have an associated component", errMsg);
            }
            if (!isWithinHierarchy (comp)) {
               return PropertyUtils.illegalValue (
                  "associated component not within model hierarchy", errMsg);
            }
            if (!PropertyUtils.isConnectedToHierarchy (prop)) {
               return PropertyUtils.illegalValue (
                  "property is not accesible", errMsg);
            }
            if (myNumericOnly && !isNumeric (prop.getInfo())) {
               return PropertyUtils.illegalValue (
                  "property is not numeric", errMsg);
            }
         }
         else if (myPropertiesAllowed) {
            return PropertyUtils.illegalValue (
               "value must be a ModelComponent or a Property", errMsg);
         }
         else {
            return PropertyUtils.illegalValue (
               "value must be a ModelComponent", errMsg);
         }
      }
      return validValue (value, errMsg);
   }

   protected Object getInternalValue() {
      return myValue;
   }

   public void dispose() {
      if (mySelectionManager != null) {
         mySelectionManager.removeSelectionListener (this);
      }
   }

   /**
    * A new component is specified. If it is compatible with the existing
    * property selection, keep that selection.
    */
   private void setValueFromComponent (ModelComponent comp) {
      if (comp != null && myPropertySelector != null) {
         if (myLastPropName != null) {
            Property prop = comp.getProperty (myLastPropName);
            if (prop != null) {
               updateValueAndDisplay (prop);
               return;
            }
            else { // might not want to clear this; might want to make
               // the context persistent
               myLastPropName = null;
            }
         }
      }
      updateValueAndDisplay (comp);
   }

   public void selectionChanged (SelectionEvent e) {
      if (!mySelectionListenerMasked) {
         ModelComponent c = null;
         if (e.numAddedComponents() > 0) {
            c = e.getLastAddedComponent();
         }
         else if (!myIgnoreDeselection) {
            c = mySelectionManager.getLastSelected();
         }
         if (c != null && c != getComponent (myValue)) {
            setValueFromComponent (c);
         }
      }
   }

   protected void updatePropertySelector() {
      // mask value change listeners since othewise the
      // property selector will fire one to announce the new
      // value we already know about ...
      myPropertySelector.maskValueChangeListeners (true);
      HasProperties host = getHost();
      if (myLastSelectorHost != host) {
         if (host != null) {
            PropertyInfoList list = host.getAllPropertyInfo();
            LinkedList<String> nameList = new LinkedList<String>();
            nameList.add (nullString);
            for (PropertyInfo info : list) { // break the loop out in case we
                                             // need to prune
               if (myNumericOnly && !isNumeric (info)) {
                  continue;
               }
               if (myWidgetableOnly && !PropertyWidget.canCreate (info)) {
                  continue;
               }
               nameList.add (info.getName());
            }
            myPropertySelector.setSelections (
               nameList.toArray (new String[0]), nullString);
         }
         else {
            myPropertySelector.setSelections (
               new String[] { nullString }, nullString);
         }
         myLastSelectorHost = host;
      }
      myPropertySelector.setValue (getLeafPropName());
      myPropertySelector.maskValueChangeListeners (false);
   }

   public StringSelector getPropertySelector() {
      return myPropertySelector;
   }

   public boolean isNumeric (PropertyInfo info) {
      return NumericConverter.isNumeric (info.getValueClass());
   }

   /**
    * Enables the selection manager. If the selection manager is enabled, then
    * the selection manager and this widget are tied together: set a new
    * ModelComponent value for this widget will cause the component to be
    * selected, and selecting a ModelComponent in the selection manager will
    * cause it to set as a value in this widget.
    * 
    * @param enable
    * if <code>true</code>, enables selection management
    */
   public void setSelectionManagementEnabled (boolean enable) {
      mySelectionEnabled = enable;
      if (enable && mySelectionManager == null) {
         throw new IllegalStateException ("Selection manager is null");
      }
      if (enable && !mySelectionListenerInstalled) {
         mySelectionManager.addSelectionListener (this);
         mySelectionListenerInstalled = true;
      }
      else if (!enable && mySelectionListenerInstalled) {
         mySelectionManager.removeSelectionListener (this);
         mySelectionListenerInstalled = false;
      }
   }

   /**
    * Returns true if selection management is enabled.
    * 
    * @return true if selection management is enabled
    */
   public boolean isSelectionManagementEnabled() {
      return mySelectionEnabled;
   }

}
