/**
 * Copyright (c) 2014, by the Authors: Johnty Wang (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.gui.probeEditor;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;

import maspack.properties.HasProperties;
import maspack.properties.Property;
import maspack.widgets.StringField;
import maspack.widgets.ValueChangeEvent;
import maspack.widgets.ValueChangeListener;
import artisynth.core.driver.Main;
import artisynth.core.gui.widgets.PropertyField;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.workspace.RootModel;

/**
 * @author Johnty
 * @version 0.1 Class for representing a dialog box that allows the selection of
 * a property from the selection manager
 */

public class AddPropertyPane extends JPanel implements ActionListener,
MouseListener, ValueChangeListener {
   static final long serialVersionUID = 1L;

   private Color normalColor;
   private String oldPropName;
   private boolean myDisposed = false;

   private PropertyField myPropField;
   // private ComponentNameClass compNameField;
   // private JComboBox propSelectBox;
   // private ComboListener myListener;
   private NumericProbeEditor myParent;
   private JCheckBox editCheck; // todo: remove
   private StringField propNameField;
   private JLabel propDimLabel;
   // private PopupListener myMouseListener;
   private JPopupMenu myPopup;
   private Color DEFAULT_COMPBG;
   private Border DEFAULT_BORDER;

   // private int propIndex;
   private boolean isEditable;
   private boolean isComplete = false;
   private boolean isHighlighted = false;

   private String propertyPath;

   /**
    * constructor for AddPropertyPane class.
    * 
    * @param parent
    * reference to parent window; used for sending events to.
    */
   public AddPropertyPane (NumericProbeEditor parent) {
      myParent = parent;
      this.setAlignmentX (LEFT_ALIGNMENT);
      this.setLayout (new BoxLayout (this, BoxLayout.X_AXIS));
      this.setBorder (BorderFactory.createEtchedBorder());
      this.setMinimumSize (new Dimension (30, 35));
      this.setMaximumSize (new Dimension (Integer.MAX_VALUE, 35));

      myPropField = new PropertyField ("", 30, Main.getMain());
      DEFAULT_COMPBG = myPropField.getTextField().getBackground();
      DEFAULT_BORDER = myPropField.getTextField().getBorder();
      myPropField.setAlignmentX (Component.CENTER_ALIGNMENT);
      myPropField.addValueChangeListener (this);
      myPropField.setIgnoreDeselection (true);
      myPropField.setNumericOnly (true);
      myPropField.setWidgetableOnly (false);

      // compNameField = new ComponentNameClass();
      // compNameField.addValueChangeListener(this);
      // compNameField.setMaximumSize(new Dimension(1500, 25));
      // DEFAULT_COMPBG = compNameField.getTextField().getBackground();
      // DEFAULT_BORDER = compNameField.getTextField().getBorder();

      // addCompFieldVCheck();

      propNameField = new StringField ("", 1);
      oldPropName = propNameField.getStringValue();
      propNameField.addValueChangeListener (this);
      propNameField.setPreferredSize (new Dimension (35, 25));

      // propSelectBox = new JComboBox();
      // propSelectBox.setEnabled(false);
      // propSelectBox.setPreferredSize(new Dimension(150, 25));
      // propSelectBox.setMaximumSize(propSelectBox.getPreferredSize());
      // myListener = new ComboListener(this);

      propDimLabel = new JLabel ("");
      propDimLabel.setPreferredSize (new Dimension (30, 25));
      propDimLabel.setMaximumSize (new Dimension (30, 25));
      // propDimLabel.setMaximumSize(propSelectBox.getPreferredSize());

      editCheck = new JCheckBox ("Edit");
      editCheck.addActionListener (this);

      isEditable = true;
      isComplete = false;
      normalColor = NumericProbeEditor.activeColor;

      // this.setBorder(BorderFactory.createLineBorder(parent.getBackground(),
      // 1));
      this.setBorder (BorderFactory.createEtchedBorder());

      this.add (Box.createRigidArea (new Dimension (5, 0)));
      this.add (propNameField);
      // this.add(Box.createRigidArea(new Dimension(5, 0)));

      this.add (myPropField);

      // this.add(compNameField);
      // this.add(Box.createRigidArea(new Dimension(5, 0)));
      // this.add(propSelectBox);
      this.add (Box.createRigidArea (new Dimension (5, 0)));
      this.add (propDimLabel);
      this.add (Box.createHorizontalGlue());
      // this.add(editCheck);

      createPopupMenu();
      this.addMouseListener (this);


      final JPanel thisPane = this;
      this.addComponentListener (new java.awt.event.ComponentAdapter() {
         public void componentResized (ComponentEvent event) {
            resizeComponents (thisPane.getWidth());
         }
      });

      addListeners();

   }

   public void setOldPropName (String name) {
      oldPropName = name;
   }

   public String getOldPropName() {
      return oldPropName;
   }

   public void setPropNameField (String name) {
      // System.out.println(oldPropName+"->"+name);
      if (name != oldPropName) {
         propNameField.removeValueChangeListener (this);
         propNameField.setValue (name);
         oldPropName = name;
         // System.out.println("renamed in field");
         propNameField.addValueChangeListener (this);
      }
      else {
         // System.out.println("not renamed: same as before");
      }
   }

   public String getPropNameFieldText() {
      return propNameField.getStringValue();
   }

   public void setPropNameEnabled (boolean enabled) {
      propNameField.getTextField().setEnabled (enabled);
   }

   public void removePropNameField() {
      this.remove (propNameField);
   }

   private JPopupMenu createPopupMenu() {
      // Create the popup menu.
      myPopup = new JPopupMenu();
      JMenuItem menuItem;
      menuItem = new JMenuItem ("Delete Property");
      menuItem.addActionListener (this);
      myPopup.add (menuItem);
      return myPopup;
   }

   public boolean isComplete() {
      return isComplete;
   }

   public void refreshContents() {
      resizeComponents (this.getWidth());
      // System.out.println(this.getWidth() + "x" + this.getHeight());
   }

   private void resizeComponents (int totalWidth) {
      int width = totalWidth - 15;

      editCheck.setPreferredSize (new Dimension (50, 25)); // this no
      // longer needed
      // at all
      editCheck.setMaximumSize (editCheck.getPreferredSize());
      this.revalidate();
   }

   public void setEditable (boolean editable) {
      myPropField.setSelectionManagementEnabled (editable);
      editCheck.setSelected (editable);
      isEditable = editable;
      setActive (editable);
   }

   private void addListeners() {
      myPropField.getTextField().addMouseListener (new MouseAdapter() {
         public void mouseClicked (MouseEvent e) {
            if (!isEditable) {
               setEditable (true);
            }
            sendFocusMeEvent();
            // compNameField.getTextField().requestFocus();
            // System.out.println("clicked on compNameField");
         }
      });
      myPropField.getTextField().addFocusListener (new FocusListener() {
         public void focusGained (FocusEvent e) {
            // System.out.println("focus gained by txt box");
         }

         public void focusLost (FocusEvent e) {
            // System.out.println("focus lost by txt box");
            // setEditable(false);
            updateAppearance();
         }

      }

      );
      JComboBox propBox = myPropField.getPropertySelector().getComboBox();
      propBox.addMouseListener (new MouseAdapter() {
         public void mouseClicked (MouseEvent e) {
            // System.out.println("clicked on propSelectBox");
            sendFocusMeEvent();
         }
      });
   }

   /**
    * Called when either the property name field (for output probes only) or the
    * component field changes
    * 
    */
   public void valueChange (ValueChangeEvent v) {
      if (v.getSource() == propNameField) {
         // System.out.println("property renamed...");
         myParent.actionPerformed (
            new ActionEvent (this, 0, "Property Renamed"));
      }
      else if (v.getSource() == myPropField) {
         // System.out.println("component path changed:
         // "+compNameField.getText());
         readComponentField();
      }

   }

   /**
    * called when the component string field changes
    * 
    */
   private void readComponentField() {
      isComplete = false;
      clearDimensionLabel();
      // System.out.println("SET FALSE");
      myParent.actionPerformed (new ActionEvent (this, 0, "Invalidate"));
      // System.out.println("new value "+compNameField.getText());
      Object compOrProp = myPropField.getValue();
      RootModel root = Main.getMain().getRootModel();
      if (compOrProp instanceof ModelComponent) {
         propertyPath =
            ComponentUtils.getPathName (root, (ModelComponent)compOrProp);
         myParent.actionPerformed (new ActionEvent (this, 0, "blank"));
      }
      else if (compOrProp instanceof Property) {
         Property prop = (Property)compOrProp;
         propertyPath = 
            ComponentUtils.getPropertyPathName (
               prop, root, /*excludeLeaf=*/false);

         if (!HasProperties.class.isAssignableFrom (prop.get().getClass())) {
            isComplete = true;
            int dim = NumericProbeEditor.GetPropDim (propertyPath);
            setDimensionLabel (dim);
            myParent.actionPerformed (
               new ActionEvent (this, 0, "PropSelected"));
            normalColor = NumericProbeEditor.completedColor;
         }
         else {
            myParent.actionPerformed (new ActionEvent (this, 0, "blank"));
         }
      }
      else {
         propertyPath = "";
         myParent.actionPerformed (new ActionEvent (this, 0, "blank"));
      }
      updateAppearance();
      // readFullPath(compNameField.getText());
   }

   public void actionPerformed (ActionEvent e) {
      clearDimensionLabel();
      // System.out.println("AddPropPane event: " + e.getActionCommand());
      if (e.getActionCommand() == "Edit") {
         JCheckBox chk = (JCheckBox)e.getSource();
         if (chk != null) { // TODO: delete this section
            // compNameField.attachSelectionListener(chk.isSelected());
            // compNameField.setEnabled(chk.isSelected());
            // propSelectBox.setEnabled(chk.isSelected());
            // setEditable(chk.isSelected());
         }
      }
      else if (e.getActionCommand() == "Delete Property") {
         myParent.actionPerformed (new ActionEvent (this, 0, "Delete Property"));
      }
      else if (e.getActionCommand() == "PropSelected") {
         System.out.println ("PropSelected event; prop path is " + propertyPath);
         Property prop = getPropFromString (propertyPath);
         if (prop != null) {
            if (HasProperties.class.isAssignableFrom (prop.get().getClass()) == false) {
               isComplete = true;
               int dim = NumericProbeEditor.GetPropDim (propertyPath);
               setDimensionLabel (dim);
               myParent.actionPerformed (new ActionEvent (
                  this, 0, "PropSelected")); // the parent can call
               // getPropertyPath to
               // find the path
               // setEditable(false);
               normalColor = NumericProbeEditor.completedColor;
               updateAppearance();
               // System.out.println("SET TRUE");
            }
         }
         else { // shouldn't really reach here, EVER.
            normalColor = NumericProbeEditor.inactiveColor;
            // generate error?
         }
      }
      else {

      }
   }

   /**
    * see if a full path entered into the component box is valid. could be the
    * absolute full path that includes the property as well.
    * 
    * @param fullPath string defining the full path
    * @return true if the full path is valid
    */
   public boolean isFullPathValid (String fullPath) {
      if (getCompFromStr (fullPath) != null) {
         return true;
      } // TODO: more checks for prop!
      else if (getPropFromString (fullPath) != null) {
         return true;
      }
      else {
         return false;
      }
   }

   private ModelComponent getCompFromStr (String pathName) {
      if (pathName.indexOf (".") != -1) {
         return null;
      }
      RootModel root = Main.getMain().getRootModel();
      try {
         ModelComponent component = root.findComponent (pathName);
         if (component != null) {
            return component;
         }
         else {
            // System.out.println("Component not found!");
         }
      }
      catch (Exception e) {
         System.out.println (e.getMessage());
      }

      return null;
   }

   /**
    * finds a property as defined by the input string. Everything after the "."
    * should be property names.
    * 
    * @param pathName
    * @return true if property can be found from the string input.
    */
   private Property getPropFromString (String pathName) {
      if (pathName == null) {
         return null;
      }
      // System.out.println("looking for Property path: " + pathName);
      if (pathName.indexOf (".") != -1) // the path could contain properties,
                                          // or
      // even sub properties
      {
         String compName = pathName.substring (0, pathName.indexOf ("."));
         String propPath =
            pathName.substring (pathName.indexOf (".") + 1, pathName.length());
         System.out.println ("comp: " + compName + "; prop: " + propPath);
         // propPath is everything after the . - e.g. renderProps.visible
         ModelComponent component = getCompFromStr (compName);
         if (component != null) {
            return RecursePropString (propPath, component);
         }
         else {
            return null;
         }
      }
      else {
         return null;
      }
   }

   private Property RecursePropString (String propPath, Object parent) {
      // System.out.println("recurse path: " + propPath);
      if (propPath.indexOf (".") > 0) // if there is a . in the path, then we
      // need to go down further
      {
         String propName = propPath.substring (0, propPath.indexOf ("."));
         String rest =
            propPath.substring (propPath.indexOf (".") + 1, propPath.length());
         Property prop = ((HasProperties)parent).getProperty (propName);
         if (prop != null) {
            Object obj = prop.get();
            return RecursePropString (rest, obj);
         }
         else {
            return (Property)parent;
         }
      }
      else { // if there are no more "."s in the name, then just
         // simply get all the properties contained within this property
         System.out.println (
            "no more .'s... just list the properties at this level");
         Property prop = ((HasProperties)parent).getProperty (propPath);
         if (prop != null) {
            // System.out.println("bottom prop found");
            return prop;
         }
         else {
            // System.out.println("bottom prop NOT found");
            return null;
         }
      }
   }

   public String getPropPath() {
      return propertyPath;
   }

   public void setDimensionLabel (int dim) {
      propDimLabel.setText (Integer.toString (dim));
   }

   public void clearDimensionLabel() {
      propDimLabel.setText ("");
   }

   public void setCompPath (String path) {
      myPropField.setValue (ComponentUtils.findComponentOrProperty (
                               Main.getMain().getRootModel(), path));
      // validCompField = path;
      // compNameField.attachSelectionListener(false);
      // System.out.println("setting comp path: " + path);
      // readFullPath(path);
      // compNameField.attachSelectionListener(true);

   }

   // public void setPropSelection(String path)
   // {
   // propSelectBox.setSelectedItem(path);
   // }

   private void sendFocusMeEvent() {
      myParent.actionPerformed (new ActionEvent (this, 0, "Change Focus"));
   }

   // ***START OF MOUSE HANDLING-RELATED CODE
   public void mouseEntered (MouseEvent e) {
      // myParent.actionPerformed(new ActionEvent(this, 0, "MouseEnteredProp"));
      // setHighlight(true);
   }

   public void mouseExited (MouseEvent e) {
      // myParent.actionPerformed(new ActionEvent(this, 0, "MouseExitedProp"));
      // setHighlight(false);
   }

   public void mouseClicked (MouseEvent e) {
      if (!isEditable) {
         setEditable (true);
      }
      updateAppearance();
      sendFocusMeEvent();
   }

   public void mousePressed (MouseEvent e) {
      maybeShowPopup (e);
   }

   public void mouseReleased (MouseEvent e) {
      // maybeShowPopup(e);
   }

   private void maybeShowPopup (MouseEvent e) {
      if (e.isPopupTrigger()) {
         myPopup.show (e.getComponent(), e.getX(), e.getY());
      }
   }

   void updateAppearance() {
      Color color =
         NumericProbeEditor.getBuildComponentColor (
            this, isComplete, isHighlighted);
      if ((color == null && isBackgroundSet()) ||
          (color != null && (!isBackgroundSet() || 
                             !color.equals (getBackground())))) {
         setBackground (color);
         myPropField.setBackgroundAll (color);
         propNameField.setBackground (color);
      }
   }

   public void setHighlight (boolean highlight) {
      isHighlighted = highlight;
      updateAppearance();
   }

   private void setActive (boolean active) {
      // System.out.println("setActive called..."+active);
      if (active == true) {
         // compNameField.getTextField().setBorder(
         // BorderFactory.createBevelBorder(BevelBorder.LOWERED,
         // Color.RED.darker() , Color.RED.darker()));
         myPropField.getTextField().setBorder (
            BorderFactory.createBevelBorder (
               BevelBorder.LOWERED, Color.BLUE.darker(), Color.BLUE));
         // (BorderFactory.createBevelBorder(BevelBorder.LOWERED));
      }
      else {
         // compNameField.getTextField().setBorder(DEFAULT_BORDER);
         myPropField.getTextField().setBorder (DEFAULT_BORDER);
      }
   }

   // ***END OF MOUSE HANDLING-RELATED CODE

   public void dispose() {
      if (!myDisposed) {
         myPropField.dispose();
         propNameField.dispose();
         myDisposed = true;
      }
   }
}
