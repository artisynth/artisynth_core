/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import maspack.properties.CompositeProperty;
import maspack.properties.EditingProperty;
import maspack.properties.HostList;
import maspack.properties.InheritableProperty;
import maspack.properties.PropTreeCell;
import maspack.properties.Property;
import maspack.properties.PropertyInfo;
import maspack.properties.PropertyInfo.ExpandState;
import maspack.properties.PropertyList;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;
import maspack.util.Clonable;
import maspack.util.InternalErrorException;

/** 
 * A composite property panel is a widget that
 *
 * (a) exposes all the properties of a CompositeProperty in a single panel, and
 * 
 * (b) allows the type of CompositeProperty itself to be selected by a
 * selection widget. When the type changes, the panel's widgets are changed to
 * reflect the new properties.
 */
public class CompositePropertyPanel extends LabeledPanel
   implements ValueChangeListener {

   /**
      The updating procedure for this widget is complicated when it
      controls the composite property of several hosts via an EditingWidget.
      The several cases then work as follows:

      (1) creating panel from scratch

      myCpropProperty = current composite property
      determine myCpropType
      determine status of null allowed
      rebuildPanel (setFromWidgets=false):
          myWidgetProps = createWidgetProps():
              Extend prop cell tree for children
              use prop cell tree to get common and backup values in hostList
              create EditingProperties from prop cell tree and host list
          store widget props 

      (2) updating widget values within a panel

      if (myCpropType has changed) 
          rebuild whole panel
      else
          reset source hosts in case those have been reset by another party
          doUpdateWidgets():
             for each widget property pair
                if (widget is a labeled component base) 
                    ((EditingProperty)prop).updateValue();
                    PropertyWidget.updateValue (ctrl, prop);
                else if widget is a CompositePropertyPanel
                    cpanel.updateWidgetValues (updateFromSource);
   */

   /**
    * 
    */
   private static final long serialVersionUID = -8250743828009978845L;
   /**
      A possibly undesirable behavior can occur as follows:

      1. Select a composite panel that has a common type across several hosts.
         Assume that one or more fields has a Void value.

      2. Set a new type for the composite value. All hosts now have this type.

      3. Revert to the original type. Fields that did not have a void type
         are set to based on the stored widget value. However, fields that
         did have a void type now have the default value for the field,
         since all values get some sort of setting when we set the type.  It
         might be possible to prevent this using the hostBackup values, but
         it is not clear is this is necessary.
   */
   
   // Widget to select the composite property type.
   StringSelector myCpropSelector = null;
   // Hide cprop selector - if only one class is available
   boolean myHideCpropSelector = false;
   // Property handle to the composite property
   protected Property myCpropProperty = null;
   // Current type of the composite property
   protected Class<?> myCpropType = Property.VoidValue;
   // myCrop stores underlying CompositeProperty used to build widgets when
   // myCpropProperty is not an EditingProperty
   CompositeProperty myCprop = null; 

   // Current set of widgets for the composite property's properties.
   protected LabeledComponentBase[] myWidgets = null;
   // Properties associated with the widgets
   protected LinkedList<Property> myWidgetProps = null;

   // Hash map storing the composite property types allowed by this
   // panel, and the string names identifying them.
   private HashMap<String,Class<?>> myCpropTypes;
   // Hash map storing pre-allocated widgets for specific composite
   // property types.
   private HashMap<String,LabeledComponentBase[]> myWidgetSets;
   // A listener that causes rerendering whenever a widget's value is changed
   private LinkedList<ValueChangeListener> myGlobalValueChangeListeners = new LinkedList<ValueChangeListener>();

   private ExpandState myExpandState = ExpandState.Unexpandable;
   private LabeledToggleButton myExpandButton = null;

   public static PropertyList myProps =
      new PropertyList (CompositePropertyPanel.class, LabeledPanel.class);

   static {
      myProps.add (
         "expandState", "set expanded, contracted, or unexpandable",
         ExpandState.Unexpandable);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }   

//    public void initializeSelection (Class[] classes, boolean nullAllowed) {
//       if (classes == null || classes.length < 1) {
//          throw new IllegalArgumentException (
//             "Argument 'classes' is null or empty");
//       }
//       for (Class cls : classes ) {
//          if (!(CompositeProperty.class.isAssignableFrom(cls))) {
//             throw new IllegalStateException (
//                "Class "+cls+" not instance of CompositeProperty");
//          }
//       }
//       myWidgetSets = new LinkedHashMap<String,LabeledComponentBase[]>();
//       if (classes.length > 1 || nullAllowed) {
//          myCpropSelector = createCpropSelector (name, nullAllowed);
//          myCpropSelector.addValueChangeListener (this);
//       }
//       else {
//          myCpropSelector = null;
//       }
//       setMainWidget (myCpropSelector);   
//    }

   public ExpandState getExpandState () {
      return myExpandState;
   }

   public void setExpandState (ExpandState state) {
      if (myExpandState != state) {
         myExpandState = state;
         if (state == ExpandState.Unexpandable) {
            myExpandButton = null;
         }
         else {
            if (myExpandButton == null) {
               myExpandButton = ExpandablePropertyPanel.createExpandButton();
               myExpandButton.addValueChangeListener (this);
            }
            if (state == ExpandState.Expanded) {
               myExpandButton.setLabelText ("less ...");
            }
            else {
               myExpandButton.setLabelText ("more ..."); 
            }
         }
         rebuildPanel(/*setFromWidgets=*/false);         
         if (isVisible()) {
            GuiUtils.repackComponentWindow (this);
            fireGlobalValueChangeListeners(new ValueChangeEvent (this, null));
            //Main.getWorkspace().rerender();
         }
      }
   }

   private Class<?>[] findSelectableClasses (Property prop) {
      PropertyInfo info = prop.getInfo();
      // try to determine the selectable classes by querying the property's
      // main value class
      Class<?>[] classes = PropertyUtils.findCompositePropertySubclasses(info);
      if (classes == null) {
         classes = new Class[] { info.getValueClass() };
      }
      return classes;
   }

   private boolean isNullAllowed (Property prop) {
      
      if (prop instanceof EditingProperty) {
         // then null values are allowed only if they are permitted
         // on all the hosts
         EditingProperty eprop = (EditingProperty)prop;
         HostList hostList = eprop.getHostList();
         PropTreeCell cell = eprop.getCell();         
         PropertyInfo[] infos = hostList.getAllInfos (cell);
         boolean nullAllowed = true;
         for (PropertyInfo info : infos) {
            if (!info.getNullValueOK()) {
               nullAllowed = false;
            }
         }
         return nullAllowed;
      }
      else {
         return prop.getInfo().getNullValueOK();
      }
   }

   public void initializeSelection (Property prop) {
      Class<?>[] classes = findSelectableClasses (prop);
      initializeSelection (classes, prop);
   }

   public void initializeSelection (
      Class<?>[] selectableClasses, Property prop) {

      Class<?> primaryClass =  prop.getInfo().getValueClass();
      if (selectableClasses == null || selectableClasses.length < 1) {
         throw new IllegalArgumentException (
            "Argument 'selectableClasses' is null or empty");
      }
      LinkedHashMap<String,Class<?>> typeMap =
         new LinkedHashMap<String,Class<?>>();
      for (Class<?> cls : selectableClasses ) {
         if (!(primaryClass.isAssignableFrom(cls))) {
            throw new IllegalStateException (
               "Class "+cls+" not instance of "+primaryClass);
         }
         typeMap.put (cls.getSimpleName(), cls);
      }
      myCpropTypes = typeMap;
      myWidgetSets = new LinkedHashMap<String,LabeledComponentBase[]>();
      String[] selections = createCpropSelections (isNullAllowed(prop));
      myCpropSelector.setSelections (selections);
      //myCpropSelector = createCpropSelector ("xxx", isNullAllowed(prop));
      //myCpropSelector.addValueChangeListener (this);
      //setMainWidget (myCpropSelector);   
      myCpropProperty = prop;
      myCpropType = getCpropTypeFromProperty(prop);
      if (!isNullAllowed(prop) && 
          selectableClasses.length == 1 &&
          selectableClasses[0] == primaryClass) {
         // If there is only one selection available, and it is the main
         // class, no need for CpropSelector so replace it with a simple 
         // LabeledComponent
         setMainWidget (new LabeledComponent(prop.getName()+":"));
      }
      rebuildPanel (/*setValuesFromWidgets=*/false);
   }

   // private void initialize (String name, boolean nullAllowed) {
   //    myCpropTypes = initializeCpropTypes();
   //    if (myCpropTypes.size() < 1) {
   //       throw new IllegalStateException (
   //          "Table returned by initializeCprops() is empty");
   //    }
   //    for (Class type : myCpropTypes.values()) {
   //       if (!(CompositeProperty.class.isAssignableFrom(type))) {
   //          throw new IllegalStateException (
   //             "Type "+type+" specified by initializeCprops() not"+
   //             "instance of CompositeProperty");
   //       }
   //    }
   //    myWidgetSets = new LinkedHashMap<String,LabeledComponentBase[]>();
   //    myCpropSelector = createCpropSelector (name, nullAllowed);
   //    myCpropSelector.addValueChangeListener (this);
   //    setMainWidget (myCpropSelector);
   // }

   //   protected abstract LinkedHashMap<String,Class> initializeCpropTypes ();

   public CompositePropertyPanel () {
      //super ();
      //initialize ("", /*nullAllowed=*/true);
      myCpropSelector = new StringSelector ("", new String[] {});
      //myCpropSelector.setVoidValueEnabled (true);
      myCpropSelector.addValueChangeListener (this);
      setMainWidget (myCpropSelector);
      myPanel.addWidget (myCpropSelector);
   }

   /** 
    * Create a new CompositePropertyPanel.
    * 
    * @param name name for the composite property type selector field
    * @param nullAllowed if <code>true</code>, indicates that the
    * composite property is allowed to assume <code>null</code> values.
    */
   public CompositePropertyPanel (String name, boolean nullAllowed) {
      this();
      setLabelText (name);
//       super ();
//       initialize (name, nullAllowed);
   }

   /** 
    * Create a new CompositePropertyPanel.
    * 
    * @param name name for the composite property type selector field
    * @param prop property handle used to access the composite property itself.
    */
   public CompositePropertyPanel (String name, Property prop) {
      this();
      setLabelText (name);
      initializeSelection (prop);      
//       super ();
//       initialize (name, prop.getInfo().getNullValueOK());
//       setProperty (prop);
   }

   /** 
    * Sets whether or not the composite property associated with
    * this panel is allowed to assume a null value.
    */
   private void setNullAllowed (boolean nullAllowed) {
      if (myCpropSelector.containsSelection ("null") != nullAllowed) {
         myCpropSelector.setSelections (
            createCpropSelections (nullAllowed));
      }
   }

   /** 
    * Returns the type of the current composite property (or null if
    * the property is null).
    */
   private Class<?> getCpropType (Object value) {
      if (value == null) {
         return null;
      }
      for (Class<?> cls : myCpropTypes.values()) {
         if (cls == value.getClass()) {
            return cls;
         }
      }
      return Property.VoidValue;
   }

   /** 
    * Returns the current composite property.
    */
   private Object getCurrentCprop () {
      if (myCpropProperty instanceof EditingProperty) {
         EditingProperty eprop = (EditingProperty)myCpropProperty;
         HostList hostList = eprop.getHostList();
         Object[] values = hostList.getAllValues (eprop.getCell());
         Class<?> cpropType = getCpropType (values[0]);
         if (cpropType == Property.VoidValue) {
            return Property.VoidValue;
         }
         for (int i=1; i<values.length; i++) {
            Class<?> nextType = getCpropType (values[i]);
            if (nextType == Property.VoidValue || nextType != cpropType) {
               return Property.VoidValue;
            }
         }
         return values[0];
      }
      else {
         return myCpropProperty.get();
      }
   }

   /** 
    * Get the type of the current composite property from its property
    * handle. If the property handle is an EditingProperty, then it may refer
    * to several instances on several hosts, and if the type is not the same
    * across those hosts, the type is considered to be Property.VoidValue.
    */   
   private Class<?> getCpropTypeFromProperty (Property cprop) {
      if (cprop instanceof EditingProperty) {
         EditingProperty eprop = (EditingProperty)cprop;
         HostList hostList = eprop.getHostList();
         Object[] values = hostList.getAllValues (eprop.getCell());
         Class<?> cpropType = getCpropType (values[0]);
         if (cpropType == Property.VoidValue) {
            return Property.VoidValue;
         }
         for (int i=1; i<values.length; i++) {
            Class<?> nextType = getCpropType (values[i]);
            if (nextType == Property.VoidValue || nextType != cpropType) {
               return Property.VoidValue;
            }
         }
         return cpropType;
      }
      else {
         return getCpropType (cprop.get());
      }
   }

   /** 
    * Returns the property handle for thecomposite property.
    */
   public Property getProperty() {
      return myCpropProperty;
   }

   /** 
    * Uses the name,type table to create a set of names for the composite
    * property type selector widget.
    */
   private String[] createCpropSelections (boolean nullAllowed) {
      ArrayList<String> matNames = new ArrayList<String>();
      for (String name : myCpropTypes.keySet()) {
         matNames.add (name);
      }
      if (nullAllowed) {
         matNames.add ("null");
      }
      return matNames.toArray(new String[0]);    
   }

   /** 
    * Creates an instance of a composite property type.
    */
   private CompositeProperty createCprop (Class<?> type) {
      if (type != null) {
         try {
            return (CompositeProperty)type.newInstance();
         }
         catch (Exception e) {
            throw new InternalErrorException (
               "Cannot create composite property "+getCpropName(type)+
               " with a no-args constructor");
         }
      }
      else {
         return null;
      }
   }

   /** 
    * Gets the name for a given composite property type, or <code>null</code>
    * if the type is <code>null</code>.
    */
    private String getCpropName (Class<?> type) {
      if (type == null) {
         return "null";
      }
      for (Map.Entry<String,Class<?>> e : myCpropTypes.entrySet()) {
         if (e.getValue() == type) {
            return e.getKey();
         }
      }
      throw new InternalErrorException (
         "Unknown composite property " + type);
   }

   /** 
    * Sets the composite property selector in a way that does not create a
    * <code>valueChange</code> event.
    */
   private void setSelectorValue (Object value) {
      myCpropSelector.maskValueChangeListeners (true);
      myCpropSelector.setValue (value);
      myCpropSelector.maskValueChangeListeners (false);      
   }

   /** 
    * Try to create a prototype for a particular type of composite property by
    * cloning an instance (if there is one) in a host list.
    */
   private CompositeProperty tryCreatingCpropFromPrototype (Class<?> type) {

      CompositeProperty protoCprop = null;
      EditingProperty eprop = (EditingProperty)myCpropProperty;
      HostList hostList = eprop.getHostList();
      Object[] values = hostList.getAllValues (eprop.getCell());
      for (int i=0; i<values.length; i++) {
         if (type.isInstance (values[i]) &&
             values[i] instanceof Clonable) {
            try {
               protoCprop = (CompositeProperty)((Clonable)values[i]).clone();
            }
            catch (Exception e) {
               System.out.println (
                  "Warning: clone failed for "+values[i].getClass());
            }
            if (protoCprop != null) {
               break;
            }
         }
      }
      return protoCprop;
   }

   /** 
    * Called when the composite property type is changed via the composite
    * property type selector.
    */
   public void valueChange (ValueChangeEvent e) {
      if (e.getSource() == myCpropSelector) {
         String newCpropName = (String)myCpropSelector.getValue();
         Class<?> newCpropType = myCpropTypes.get(newCpropName);
         CompositeProperty protoCprop = null;

         if (myCpropType == Property.VoidValue &&
             newCpropType != null &&
             myCpropProperty instanceof EditingProperty) {
            // if there is an existing composite property of this type among the
            // hosts, try to create the prototype by cloning it so
            // that we start with it's properties
            protoCprop = tryCreatingCpropFromPrototype (newCpropType);
         }
         if (protoCprop == null) {
            protoCprop = createCprop (newCpropType);
         }         
         CompositeProperty prevCprop = (CompositeProperty)getCurrentCprop();
         //CompositeProperty prevCprop = (CompositeProperty)myCpropProperty.get();
         myCpropProperty.set (protoCprop);
         myCpropType = newCpropType;
         String[] initializedValues = null;

         // if the new CompositeProperty class contains an
         // initializePropertyValues() method, this may be used to set selected
         // property values based on the previous CompositeProperty
         if (prevCprop != null) {
            initializedValues = PropertyUtils.maybeInitializeValues (
               myCpropProperty, prevCprop);
         }
         // rebuild the panel, setting values from previous widget values,
         // except for previously initialized values
         rebuildPanel (/*setValuesFromWidgets=*/true, initializedValues);
         GuiUtils.repackComponentWindow (this);
         fireGlobalValueChangeListeners(new ValueChangeEvent(this, e));
      }
      else if (e.getSource() == myExpandButton) {
         if (myExpandButton.getBooleanValue()) {
            setExpandState (ExpandState.Expanded);
         }
         else {
            setExpandState (ExpandState.Contracted);
         }
      } else {
         throw new InternalErrorException (
            "Unexpected valueChange event: " + e);
         //fireGlobalValueChangeListeners(new ValueChangeEvent(this, e));
      }
   }

   /** 
    * Sets the current composite property for a CompositePropertyPanel to the
    * type currently stored by that panel.
    */
   private void setCpropValueFromWidget (
      Property cprop, CompositePropertyPanel cpanel) {

      cpanel.myCpropProperty = cprop;
      if (getCpropTypeFromProperty (cprop) != cpanel.myCpropType) {
         if (cpanel.myCpropType == Property.VoidValue) {
            System.out.println (
               "Warning: void widget value for "+cprop.getName()+", ignoring");
         }
         else {
            CompositeProperty protoCprop = createCprop (cpanel.myCpropType);
            //PropertyInfo info = cpanel.myCpropProperty.getInfo();
            cpanel.myCpropProperty.set (protoCprop);
         }
      }
      cpanel.rebuildPanel (/*setValuesFromWidgets=*/true);
   }

   /** 
    * Sets the value for property to reflect the value currently stored by a
    * widget.
    */
   private void setValueFromWidget (
      Property prop, LabeledComponentBase widget) {
      PropertyMode mode = PropertyMode.Explicit;
      if (prop.getInfo().isInheritable()) {
         PropertyModeButton button = PropertyWidget.getModeButton (widget);
         if (button != null) {
            if (!button.isSelected()) {
               mode = PropertyMode.Inherited;
            }
            ((InheritableProperty)prop).setMode (mode);
         }
      }
      if (widget instanceof LabeledControl) {
         LabeledControl ctrl = (LabeledControl)widget;
         if (mode == PropertyMode.Explicit) {
            prop.set (ctrl.getValue());
         }
      }
   }

   /** 
    * Create properties for the widgets controlling the property values of the
    * current composite property.
    */
   private LinkedList<Property> createWidgetProps () {

      if (myCpropProperty instanceof EditingProperty) {
         EditingProperty eprop = (EditingProperty)myCpropProperty;
         HostList hostList = eprop.getHostList();
         PropTreeCell cell = eprop.getCell();
         
         cell.removeAllChildren();
         CompositeProperty cprop = createCprop (myCpropType);
         LinkedList<Property> props = PropertyUtils.createProperties(cprop);
         for (Property prop : props) {
            cell.addChild (new PropTreeCell (prop.getInfo(), prop.get()));
         }
         
         hostList.setSubHostsFromValue (cell);         
         hostList.saveBackupValues (cell);
         hostList.getCommonValues (cell, /*live=*/true);
         cell.setValue (CompositeProperty.class);

         return EditingProperty.createProperties (cell, hostList, /*live=*/true);
      }
      else {
         myCprop = (CompositeProperty)myCpropProperty.get();
         return PropertyUtils.createProperties(myCprop);
      }
   }

   /** 
    * Called when the composite property is set to <code>null</code>.
    * If the property accessing the composite property is an EditingProperty,
    * then its property tree and hostList are updated to reflect the
    * fact that there are no properties associoated with the composite property.
    */
   private void updateEditingPropertyForNullCprop (EditingProperty eprop) {
      HostList hostList = eprop.getHostList();
      PropTreeCell cell = eprop.getCell();
         
      cell.removeAllChildren();
         
      hostList.setSubHostsFromValue (cell);         
      hostList.saveBackupValues (cell);
      //hostList.getCommonValues (cell, /*live=*/true);
      cell.setValue (null);
   }

   private boolean valueCanBeSet (String name, String[] valuesToNotSet) {
      if (valuesToNotSet != null) {
         for (String dontSet : valuesToNotSet) {
            if (name.equals (dontSet)) {
               return false;
            }
         }
      }
      return true;
   }

   /** 
    * Attaches a newly created set of properties to their associated
    * widgets.
    *
    * @param widgets list of widgets
    * @param props newly created properties
    * @param setValuesFromWidgets if <code>true</code>, then the properties
    * should be set to assume the values currently stored in the widgets.
    * @param valuesToNotSet if not {@code null}, contains a list of
    * property names whose values should <i>not</i> be set from widgets
    * if {@code setValuesFromWidgets} is {@code true}.
    * 
    */
   private void attachPropsToWidgets (
      LabeledComponentBase[] widgets, LinkedList<Property> props,
      boolean setValuesFromWidgets, String[] valuesToNotSet) {

      if (widgets.length != props.size()) {
         throw new InternalErrorException (
            "Number of widgets (" + widgets.length +
            ") != number of properties (" + props.size() + ")");
      }
      int i = 0;
      for (Property prop : props) {
         LabeledComponentBase widget = widgets[i++];
         boolean setFromWidget =
            (setValuesFromWidgets &&
             valueCanBeSet (prop.getName(), valuesToNotSet));

         if (!setFromWidget || !(widget instanceof CompositePropertyPanel)) {
            PropertyWidget.initializeWidget (
               widget, prop);               
         }
         if (setFromWidget &&
             valueCanBeSet (prop.getName(), valuesToNotSet)) {
                
            if (widget instanceof CompositePropertyPanel) {
               CompositePropertyPanel cpanel = (CompositePropertyPanel)widget;
               if (cpanel.myCpropType != Property.VoidValue) {
                  setCpropValueFromWidget (
                     prop, (CompositePropertyPanel)widget);
               }
               else {
                  cpanel.myCpropProperty = prop;
                  cpanel.updateWidgetValues (/*updateFromSource=*/true);
               }
            }
            else if (widget instanceof LabeledControl) {
               LabeledControl ctrl = (LabeledControl)widget;
               if (ctrl.getValue() != Property.VoidValue) {
                  setValueFromWidget (prop, ctrl);
               }
               else {
                  if (prop instanceof EditingProperty) {
                     EditingProperty eprop = (EditingProperty)prop;
                     eprop.updateValue();
                  }
                  PropertyWidget.updateValue (ctrl, prop);  
               }
            }
         }
         PropertyWidget.finishWidget (widget, prop); // will set value
         if (prop instanceof InheritableProperty) {
            PropertyModeButton button =
               PropertyWidget.getModeButton ((LabeledComponentBase)widget);
            if (button != null) {
               button.setProperty ((InheritableProperty)prop);
            }
         }
      }
   }

   // For debugging only ...
   void printWidgetProps(LinkedList<Property> props) {
      if (myCpropProperty instanceof EditingProperty) {
         EditingProperty eprop = (EditingProperty)myCpropProperty;
         // HostList hostList = eprop.getHostList();
         PropTreeCell cell = eprop.getCell();
         System.out.println ("parent cell=@" + cell.hashCode());
         for (Property p : props) {
            EditingProperty ep = (EditingProperty)p;
            ep.getCell().getIndex();
            System.out.println (" "+ep.getName()+"=@" + ep.getCell().hashCode());
            if (ep.getCell().getParent() != cell) {
               System.out.println ("BAD");
            }
         }
         System.out.println ("OK");
      }
   }

   private void rebuildPanel (boolean setValuesFromWidgets) {
      rebuildPanel (setValuesFromWidgets, /*valuesToNotSet=*/null);
   }

   /** 
    * Rebuilds the panel. This is called when the panel is first created,
    * or when the set of widgets changes (due to a change in the composite
    * property type).
    */
   private void rebuildPanel (
      boolean setValuesFromWidgets, String[] valuesToNotSet) {

      myPanel.removeAllWidgets();
      myPanel.addWidget (getMainWidget());
      myWidgetProps = null;
      myWidgets = null;

      if (myCpropType == Property.VoidValue) {
         if (myCpropSelector.getValue() != Property.VoidValue) {
            setSelectorValue (Property.VoidValue);
         }
      }
      else {
         String matName = getCpropName(myCpropType);
         if (!PropertyUtils.equalValues (matName, myCpropSelector.getValue())) {
            setSelectorValue (matName);
         }
         if (myCpropType != null) {
            if (myExpandState == ExpandState.Contracted) {
               myPanel.addWidget (myExpandButton);
            }
            myWidgets = myWidgetSets.get (matName);
            if (myWidgets == null) {
               myWidgetProps = createWidgetProps ();
               ArrayList<LabeledComponentBase> widgets =
                  new ArrayList<LabeledComponentBase>();
               for (Property prop : myWidgetProps) {
                  LabeledComponentBase w = PropertyWidget.create (prop);
                  if (w != null) {
                     widgets.add (w);
                     myPanel.processPropertyWidget (prop, w);
                     if (w instanceof LabeledControl) {
                        LabeledControl ctrl = (LabeledControl)w;
                        // pass on global value change listeners
                        for (ValueChangeListener l : 
                           myGlobalValueChangeListeners) {
                           ctrl.addValueChangeListener (l);
                        }
                        // notify this panel of a change
                        //ctrl.addValueChangeListener(this);  
                     }
                  }                    
               }
               myWidgets = widgets.toArray (new LabeledComponentBase[0]);
               myWidgetSets.put (matName, myWidgets);
            }
            else {
               myWidgetProps = createWidgetProps ();
               attachPropsToWidgets (
                  myWidgets, myWidgetProps,
                  setValuesFromWidgets, valuesToNotSet);
            }
            for (LabeledComponentBase widget : myWidgets) {
               if (myExpandState != ExpandState.Contracted) {
                  myPanel.addWidget (widget);
               }
               else {
                  myPanel.doAddWidget (widget, myPanel.numWidgets());
               }
            }
            if (myExpandState == ExpandState.Expanded) {
               myPanel.addWidget (myExpandButton);
            }
         }
         else {
            if (myCpropProperty instanceof EditingProperty) {
               updateEditingPropertyForNullCprop (
                  (EditingProperty)myCpropProperty);
            }
         }
      }
   }

   private void doUpdateWidgets (boolean updateFromSource) {
      int i = 0;
      for (Property prop : myWidgetProps) {
         LabeledComponentBase widget = myWidgets[i++];
         if (widget instanceof LabeledControl) {
            LabeledControl ctrl = (LabeledControl)widget;
            if (prop instanceof EditingProperty && updateFromSource) {
               ((EditingProperty)prop).updateValue();
            }
            PropertyWidget.updateValue (ctrl, prop);
         }
         else if (widget instanceof CompositePropertyPanel) {
            CompositePropertyPanel cpanel = (CompositePropertyPanel)widget;
            cpanel.updateWidgetValues (updateFromSource);
         }
      }   
   }

   /** 
    * Updates the current set of widgets in this panel so that their
    * values reflect the underlying property values.
    * 
    * @param updateFromSource if <code>false</code>, do not update the values
    * of EditingProperties from their underlying source component(s).
    */   
   public void updateWidgetValues (boolean updateFromSource) {
      Class<?> cpropType = getCpropTypeFromProperty(myCpropProperty);
      if (myCpropType != cpropType) {
         // composite property type has changed, and hence so have the widgets
         myCpropType = cpropType;
         rebuildPanel(/*setFromWidgets=*/false);
         GuiUtils.repackComponentWindow (this);
      }
      else {
         // widgets are the same but need to update their values
         if (myWidgets != null && myWidgets.length > 0) {
            if (myCpropProperty instanceof EditingProperty) {
               if (updateFromSource) {
                  // reset source hosts in case those have been reset by
                  // another party
                  EditingProperty eprop = (EditingProperty)myCpropProperty;
                  HostList hostList = eprop.getHostList();
                  hostList.setSubHostsFromValue (eprop.getCell());
               }
               doUpdateWidgets (updateFromSource);
            }
            else {
               if (myCprop != getCurrentCprop()) {
                  myWidgetProps = createWidgetProps ();
                  attachPropsToWidgets (
                     myWidgets, myWidgetProps,
                     /*setValuesFromWidgets=*/false, /*valuesToNotSet=*/null);
                  // widgets will be updated in the above code
               }
               else {
                  doUpdateWidgets (updateFromSource);
               }
            }
         }
      }
   }
   
   public void addGlobalValueChangeListener(ValueChangeListener l) {
      myGlobalValueChangeListeners.add(l);
      if (myWidgets != null) {
         for (LabeledComponentBase comp : myWidgets) {
            if (comp instanceof LabeledControl) {
               LabeledControl ctrl = (LabeledControl)comp;
               //ctrl.addValueChangeListener (l);
               PropertyPanel.addValueChangeListener (ctrl, l);
            }
         }
      }
   }
   
   public void removeGlobalValueChangeListener(ValueChangeListener l) {
      myGlobalValueChangeListeners.remove(l);
      if (myWidgets != null) {
         for (LabeledComponentBase comp : myWidgets) {
            if (comp instanceof LabeledControl) {
               LabeledControl ctrl = (LabeledControl)comp;
               ctrl.removeValueChangeListener (l);
               //PropertyPanel.removeValueChangeListener (ctrl, l);
            }
         }
      }
   }
   
   public void fireGlobalValueChangeListeners(ValueChangeEvent vce) {
      for (ValueChangeListener vcl : myGlobalValueChangeListeners) {
         vcl.valueChange(vce);
      }
   }
   

   public boolean containsWidget(Object obj) {
      if (obj == myMainWidget ) {
         return true;
      }
      for (LabeledComponentBase cmp : myWidgets) {
         if (cmp == obj) {
            return true;
         }
      }
      return false;
   }
   
   @Override
   public void dispose() {
      super.dispose();
      myGlobalValueChangeListeners.clear();
   }
}
