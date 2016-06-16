/**
 * Copyright (c) 2014, by the Authors: John Lloyd (UBC), Tracy Wilkinson (UBC) and
 * ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.gui.editorManager;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.MouseInputAdapter;

import maspack.properties.EditingProperty;
import maspack.properties.HasProperties;
import maspack.properties.HostList;
import maspack.properties.PropTreeCell;
import maspack.properties.Property;
import maspack.render.MouseRayEvent;
import maspack.render.GL.GLViewer;
import maspack.util.InternalErrorException;
import maspack.widgets.ExpandablePropertyPanel;
import maspack.widgets.GuiUtils;
import maspack.widgets.PropertyPanel;
import maspack.widgets.StringField;
import maspack.widgets.StringSelector;
import maspack.widgets.ValueChangeEvent;
import maspack.widgets.ValueChangeListener;
import artisynth.core.driver.Main;
import artisynth.core.driver.ViewerManager;
import artisynth.core.gui.selectionManager.SelectionEvent;
import artisynth.core.gui.selectionManager.SelectionListener;
import artisynth.core.modelbase.ComponentChangeEvent;
import artisynth.core.modelbase.ComponentChangeListener;
import artisynth.core.modelbase.ComponentList;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.CopyableComponent;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.StructureChangeEvent;

/**
 * A base class used to create agents for adding components.
 */
public abstract class AddComponentAgent<E extends ModelComponent> extends
FrameBasedEditingAgent implements SelectionListener, ComponentChangeListener,
ValueChangeListener {
   protected ComponentList<?> myContainer;
   protected CompositeComponent myAncestor;

   protected ComponentListWidget<?> myComponentList;
   protected JScrollPane myListScroller;
   protected StringField myNameField; // optional name field

   StringSelector myTypeSelector;
   protected Map<String,Class> myNameTypeMap;
   protected Class myComponentType;
   protected ModelComponent myPrototype;

   protected PropertyPanel myPropertyPanel;
   protected String myPropertyPanelBorderTitle;
   protected LinkedList<Property> myDefaultProps;
   protected int myPropertyPanelIdx = -1;
   protected PropTreeCell myPropTree;
   protected HostList myHostList;

   private static boolean myLocateNearPopup = true;
   protected LocationListener myLocationListener = null;

   private final int EXCLUDE_PROP = 1;
   private final int BASIC_PROP = 2;

   private class PropName {
      String myName;
      int myDesc;

      public PropName (String name, int desc) {
         myName = name;
         myDesc = desc;
      }
   }

   private LinkedHashMap<Class,ArrayList<PropName>> myTypePropertyMap;

   protected ModelComponent getPrototypeComponent (Class type) {
      Map<Class,ModelComponent> map = getPrototypeMap();
      if (map != null) {
         ModelComponent comp = map.get (type);
         if (comp == null) { // look for prototype as the last instance in the
                             // container
            for (int i = myContainer.size() - 1; i >= 0; i--) {
               if (type.isAssignableFrom (myContainer.get (i).getClass())) {
                  comp = myContainer.get (i);
               }
            }
            if (comp == null || !(comp instanceof CopyableComponent)) {
               try {
                  comp = (ModelComponent)type.newInstance();
               }
               catch (Exception e) {
                  throw new InternalErrorException (
                     "cannot create no-args instance of " + type);
               }
               initializePrototype (comp, type);
            }
            else {
               comp = ((CopyableComponent)comp).copy (0, null);
            }
            // set properties to the inherited values they would have if
            // plugged into the hierarchy now
            
            map.put (type, comp);
         }
         return comp;
      }
      else {
         return null;
      }
   }

   /**
    * Puts this agent into its normal default state
    */
   protected abstract void resetState();

   /**
    * Puts this agent into the state which it is intended to begin with.
    */
   protected abstract void setInitialState();

   /**
    * Initializes a prototype class instance. In particular, the property values
    * are expected to be initialized.
    */
   protected abstract void initializePrototype (ModelComponent comp, Class type);

   public AddComponentAgent (Main main, ComponentList<?> container,
                             CompositeComponent ancestor) {
      super (main);
      myContainer = container;
      myAncestor = ancestor;
      myTypePropertyMap = new LinkedHashMap<Class,ArrayList<PropName>>();
   }

   /**
    * Connects the agent to the ArtiSynth infrastructure and makes it visible.
    */
   public void show (Rectangle popupBounds) {
      if (myDisplay == null) {
         createDisplay();
      }
      myContentPane.addWidget (Box.createVerticalGlue());
      Iterator<Class> typeIt = myTypePropertyMap.keySet().iterator();
      if (!typeIt.hasNext()) {
         throw new IllegalStateException (
            "no component types have been specified");
      }
      setComponentType (typeIt.next());

      myDisplay.pack();
      if (myLocateNearPopup && popupBounds != null) {
         GuiUtils.locateRelative (myDisplay, popupBounds, 0.5, 0.5, 0, 0.5);
      }
      else {
         GuiUtils.locateRight (myDisplay, myMain.getFrame());
      }
      myRootModel = myMain.getRootModel();
      myRootModel.addComponentChangeListener (this);
      if (myComponentList != null) {
         myComponentList.setSelectionManager (mySelectionManager);
      }
      mySelectionManager.addSelectionListener (this);
      myMain.getWorkspace().registerDisposable (this);
      setInitialState();
      myDisplay.setVisible (true);
   }

   /**
    * Disconnects the agent from the ArtiSynth infrastructure and disposes of
    * its resources.
    */
   public void dispose() {
      myEditManager.releaseEditLock();
      // dispose might be called more than once again; check to
      // so that we execute cleanup only once
      if (myMain.getWorkspace().deregisterDisposable (this)) {
         if (myMain.getRootModel() == myRootModel) {
            myRootModel.removeComponentChangeListener (this);
         }
         else {
            throw new InternalErrorException (
               "Root model has changed unexpectedly from "+myRootModel+" to "
               + myMain.getRootModel());
         }
         mySelectionManager.removeSelectionListener (this);
         if (myComponentList != null) {
            myComponentList.setSelectionManager (null);
         }
         if (myDisplay != null) {
            myDisplay.dispose();
            myDisplay = null;
         }

         // prototype is now updated after a component is added ...
         // if (myPrototype != null)
         // { setProperties (myPrototype, myPrototype);
         // }
         uninstallLocationListener();
         uninstallSelectionFilter();
      }
   }

   protected void createTypeSelector (String label) {
      createTypeSelector (label, null);
   }

   protected void createNameField () {
      myNameField = new StringField ("name", 20);
      addWidget (myNameField);
   }

   protected String getNameFieldValue() {
      return (String)myNameField.getValue();
   }

   protected void clearNameField() {
      myNameField.setValue (null);
   }

   /**
    * Called by subclasses inside {@link #createDisplay createDisplay} to add a
    * type selector in the display. The type selector changes the specific
    * component type that this agent adds. The available component types consist
    * of those already been specified using {@link #addComponentType
    * addComponentType}. Text strings to choose these types are provided by the
    * argument <code>names</code>; if this is <code>null</code>, then the text
    * strings are generated automatically. When the selection is changed,
    * {@link #setComponentType setComponentType} is called with the new class
    * type.
    */
   protected void createTypeSelector (String label, String[] names) {
      if (myTypePropertyMap == null) {
         throw new IllegalStateException (
            "no component types have been specified");
      }
      if (names == null) {
         names = new String[myTypePropertyMap.size()];
         int k = 0;
         for (Class type : myTypePropertyMap.keySet()) {
            String name = type.getName();
            int dotIdx;
            if ((dotIdx = name.lastIndexOf ('.')) != -1) {
               name = name.substring (dotIdx + 1);
            }
            names[k++] = name;
         }
      }
      else {
         if (names.length < myTypePropertyMap.size()) {
            throw new IllegalArgumentException (
               "Number of names is less than the number of component types");
         }
      }
      myNameTypeMap = new HashMap<String,Class>();
      int k = 0;
      for (Class type : myTypePropertyMap.keySet()) {
         myNameTypeMap.put (names[k++], type);
      }
      myTypeSelector = new StringSelector (label, names);
      myTypeSelector.addValueChangeListener (this);
      addToContentPane (myTypeSelector);
   }

   /**
    * Called by subclasses inside {@link #createDisplay createDisplay} to add a
    * property frame to the display. This presents the default properties
    * associated with the current component class type and allows them to be
    * modfied by the user. Most property modifications are ``sticky'' and will
    * persist for future invocations of this agent.
    */
   protected void createPropertyFrame (String title) {
      // JPanel is only a stub for the property frame, which will be added
      // later at the location indicated by myPropertyPanelIdx.
      myContentPane.addWidget (new JPanel());
      myPropertyPanelIdx = myContentPane.getComponentCount() - 1;
      myPropertyPanelBorderTitle = title;
   }

   /**
    * Called by subclasses inside {@link #createDisplay createDisplay} to add a
    * ComponentListWidget to the display. This provides a window onto the parent
    * ComponentList which contains the components being added by this agent.
    */
   protected void createComponentList (
      String labelText, ComponentListWidget<?> list) {
      myComponentList = list;
      myComponentList.update();
      myListScroller = new JScrollPane (myComponentList.getJList());
      myListScroller.setPreferredSize (new Dimension (280, 150));
      myListScroller.setMinimumSize (new Dimension (280, 150));
      JLabel label = new JLabel (labelText);
      GuiUtils.setItalicFont (label);
      addToContentPane (label);
      addToContentPane (myListScroller);
   }

   protected void createComponentList (ComponentListWidget<?> list) {
      createComponentList ("Component list:", list);
   }

   /**
    * Installs a location listener in the viewers. The cursor is set to a cross
    * hair, viewer selections are disabled, and a left mouse click will produce
    * a call to {@link #handleLocationEvent handleLocationEvent}.
    */
   void installLocationListener() {
      if (myLocationListener == null) {
         myLocationListener = new LocationListener();
         myViewerManager.addMouseListener (myLocationListener);
         myViewerManager.setCursor (
            Cursor.getPredefinedCursor (Cursor.CROSSHAIR_CURSOR));
         myViewerManager.setSelectionEnabled (false);
      }
   }

   /**
    * Removes any installed location listener from the viewers.
    */
   void uninstallLocationListener() {
      if (myLocationListener != null) {
         myViewerManager.setCursor (Cursor.getDefaultCursor());
         myViewerManager.removeMouseListener (myLocationListener);
         myViewerManager.setSelectionEnabled (true);
         myLocationListener = null;
      }
   }

   public void selectionChanged (SelectionEvent e) {
   }

   /**
    * Overriden by subclasses to returns a map from component class type to
    * prototype components used to store the default properties for instances of
    * those class types. The associated prototype component may be null, in
    * which case an instance will be created and stored back into the map. It is
    * the responsiblity of the subclass to ensure that the map has the
    * appropriate persistence between instantiations of the subclass.
    */
   protected abstract Map<Class,ModelComponent> getPrototypeMap();

   protected String[] getExcludedPropNames (Class type) {
      ArrayList<String> excluded = new ArrayList<String>();
      for (PropName pname : myTypePropertyMap.get (type)) {
         if (pname.myDesc == EXCLUDE_PROP) {
            excluded.add (pname.myName);
         }
      }
      return excluded.toArray (new String[0]);
   }

   protected LinkedList<Property> getBasicProps (
      Class type, LinkedList<Property> props) {

      LinkedList<Property> basicList = new LinkedList<Property>();
      for (PropName pname : myTypePropertyMap.get (type)) {
         if (pname.myDesc == BASIC_PROP) {
            for (Property p : props) {
               if (p.getName().equals (pname.myName)) {
                  basicList.add (p);
               }
            }
         }
      }
      return basicList;
   }

   /**
    * Called whenever the specific class type of the components being added by
    * this agent is changed.
    */
   public void setComponentType (Class type) {
      if (type == myComponentType) {
         return;
      }
      myComponentType = type;
      if (myPrototype != null) {
         setProperties (myPrototype, myPrototype);
      }
      myPrototype = getPrototypeComponent (type);
      if (myPrototype != null) {
         myHostList = new HostList (new HasProperties[] { myPrototype });
         myPropTree = myHostList.commonProperties (null, false);

         // remove properties which are to be excluded
         String[] excludedPropNames = getExcludedPropNames (type);
         for (int i = 0; i < excludedPropNames.length; i++) {
            myPropTree.removeDescendant (excludedPropNames[i]);
         }

         myHostList.saveBackupValues (myPropTree);
         myHostList.getCommonValues (myPropTree, /* live= */true);

         if (myPropertyPanelIdx != -1) {
            myDefaultProps =
               EditingProperty.createProperties (
                  myPropTree, myHostList, /* isLive= */ true);
            LinkedList<Property> basicProps =
               getBasicProps (type, myDefaultProps);
            if (basicProps.size() > 0) {
               LinkedList<Property> extraProps = new LinkedList<Property>();
               extraProps.addAll (myDefaultProps);
               extraProps.removeAll (basicProps);
               myPropertyPanel =
                  new ExpandablePropertyPanel (basicProps, extraProps);
            }
            else {
               myPropertyPanel = new PropertyPanel (myDefaultProps);
            }
            if (myPropertyPanelBorderTitle != null) {
               String title = myPropertyPanelBorderTitle;
               if (title.indexOf ("TYPE") != -1) {
                  title = title.replaceAll (
                     "TYPE", (String)myTypeSelector.getValue());
               }
               myPropertyPanel.setBorder (
                  GuiUtils.createTitledPanelBorder (title));
            }
            
            resetDefaultProperties();
            myPropertyPanel.updateWidgetValues(/* updateFromSource= */false);
            myPropertyPanel.setAlignmentX (Component.LEFT_ALIGNMENT);

            myContentPane.removeWidget (myPropertyPanelIdx);
            myContentPane.addWidget (myPropertyPanel, myPropertyPanelIdx);
            myContentPane.validate();
            //System.out.println ("reseting alignment");
            //myContentPane.resetLabelAlignment();
            if (myDisplay.isVisible()) {
               myDisplay.pack();
            }
         }
      }
   }

   public void valueChange (ValueChangeEvent e) {
      if (e.getSource() == myTypeSelector) {
         String typeName = (String)myTypeSelector.getValue();
         setComponentType (myNameTypeMap.get (typeName));
      }
   }

   public void actionPerformed (ActionEvent e) {
      String cmd = e.getActionCommand();
      if (cmd.equals ("Add")) {
         mySelectionManager.clearSelections();
         myMain.setSelectionMode (Main.SelectionMode.Select);
         myMain.rerender();
         setInitialState();
      }
      else if (cmd.equals ("Stop")) {
         mySelectionManager.clearSelections();
         resetState();
         myMain.rerender();
      }
      else if (cmd.equals ("Configure Properties")) {
         JOptionPane.showOptionDialog (
            myContentPane, myPropertyPanel, "Default Properties", 
            JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, 
            null, null, null);
      }
      else {
         super.actionPerformed (e);
      }
   }

   /**
    * Returns a default value for the cylinder radius render property.
    */
   protected double getDefaultLineRadius() {
      GLViewer viewer = myViewerManager.getViewer (0);
      double r = viewer.estimateRadiusAndCenter (null);
      // return 2*viewer.distancePerPixel (viewer.getCenter());
      return 0.01 * r;
   }

   /**
    * Returns a default value for the sphere radius render property.
    */
   protected double getDefaultPointRadius() {
      GLViewer viewer = myViewerManager.getViewer (0);
      double r = viewer.estimateRadiusAndCenter (null);
      // return 3*viewer.distancePerPixel (viewer.getCenter());
      return 0.015 * r;
   }

   /**
    * Returns a default value for the length of an axis.
    */
   protected double getDefaultAxisLength() {
      GLViewer viewer = myViewerManager.getViewer (0);
      double r = viewer.estimateRadiusAndCenter (null);
      // return viewer.distancePerPixel(viewer.getCenter()) *
      // viewer.getWidth()/5;
      return 0.05 * r;
   }

   protected void setProperties (HasProperties dst, HasProperties src) {
      myPropTree.setTreeValuesInHost (dst, src);
   }

   protected void setDefaultProperty (String name, Object value) {
      for (Property prop : myDefaultProps) {
         if (prop.getName().equals (name)) {
            prop.set (value);
            break;
         }
      }
   }

   protected Object getDefaultProperty (String name) {
      for (Property prop : myDefaultProps) {
         if (prop.getName().equals (name)) {
            return prop.get ();
         }
      }
      return Property.VoidValue;
   }

   /**
    * Subclasses can override this method to reset selected default properties
    * to a canonical value. It is called at initialization time, and
    * immediately after a component is added. Default property values
    * should be reset by calling {@link #setDefaultProperty setDefaultProperty},
    * as in 
    * <pre> 
    *    setDefaultProperty ("name", null);
    * </pre>
    * The reason for doing this is that some default properties should
    * not be preserved across component add actions. For example, a
    * compoment name property should always be cleared after a component
    * is added, or there is a great risk that the user will not change
    * the name before adding another component and we will then end
    * up with two components having the same name - probably not 
    * what the usre intended.
    */
   protected void resetDefaultProperties() {
   }
    
   protected void addComponent (AddComponentsCommand cmd) {
      myUndoManager.saveStateAndExecute (cmd);
      mySelectionManager.clearSelections();
      mySelectionManager.addAndRemoveSelected (cmd.getComponents(), null);
      boolean setDefaults = myPropTree.setSingleEditDefaultValues();
      resetDefaultProperties();
      if (myNameField != null) {
         clearNameField();
      }
      if (setDefaults && myPropertyPanel != null) {
         myPropertyPanel.updateWidgetValues(/* updateFromSource= */false);
      }
   }

   /**
    * Called after a left mouse press in one of the viewers when a location
    * listener is installed.
    */
   public void handleLocationEvent (GLViewer viewer, MouseRayEvent rayEvent) {}

   public void componentChanged (ComponentChangeEvent e) {
      if (!isContextValid()) {
         dispose();
         return;
      }
      if (myComponentList != null && e instanceof StructureChangeEvent) {
         if (e.getComponent() == myContainer) {
            myComponentList.update();
            int lastIndex = myComponentList.getSize() - 1;
            myComponentList.getJList().ensureIndexIsVisible (lastIndex);
         }
      }
   }

   public void addComponentType (Class type) {
      myTypePropertyMap.put (type, new ArrayList<PropName>());
   }

   public void addComponentType (Class type, String[] excludedProperties) {
      ArrayList<PropName> list = new ArrayList<PropName>();
      for (int i=0; i<excludedProperties.length; i++) {
         list.add (new PropName(excludedProperties[i], EXCLUDE_PROP));
      }
      myTypePropertyMap.put (type, list);
   }

   public void addBasicProps (Class type, String[] basicProperties) {
      ArrayList<PropName> list = myTypePropertyMap.get (type);
      if (list == null) {
         list = new ArrayList<PropName>();
         myTypePropertyMap.put (type, list);
      }
      for (int i=0; i<basicProperties.length; i++) {
         list.add (new PropName(basicProperties[i], BASIC_PROP));
      }
   }

   private class LocationListener extends MouseInputAdapter {
      public void mouseClicked (MouseEvent e) {
         if (e.getButton() == MouseEvent.BUTTON1) {
            GLViewer viewer =
               ViewerManager.getViewerFromComponent (e.getComponent());
            if (viewer != null) {
               handleLocationEvent (viewer, MouseRayEvent.create (e, viewer));
            }
         }
      }
   }
}
