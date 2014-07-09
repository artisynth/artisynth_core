/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import maspack.properties.*;
import maspack.render.*;
import maspack.util.InternalErrorException;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import java.util.*;

public class CompositePropertyWidget extends LabeledComponent {
   private static int CLEAR = 1;
   private static int NULL = 2;

   private Property myProp;
   private JButton myClearButton;
   private JButton mySetButton;
   private JLabel myNullLabel;
   private int myClearButtonFace = CLEAR;
   private boolean myClearButtonInstalled = false;
   private PropertyDialog myDialog;

   private LinkedList<ValueChangeListener> myValueChangeListeners = 
      new LinkedList<ValueChangeListener>();

   /**
    * Creates a default CompositePropertyWidget with an empty label and null
    * property.
    */
   public CompositePropertyWidget() {
      this ("", null);
   }

   public CompositePropertyWidget (String name, Property prop) {
      super (name);
      createButtons();
      setProperty (prop);
   }

   public void setProperty (Property prop) {
      myProp = prop;

      if (prop != null) {
         installClearButton (prop.getInfo().getNullValueOK());
         setClearButtonFace (prop.get() != null ? CLEAR : NULL);
      }
   }

   public Property getProperty() {
      return myProp;
   }

   public void addValueChangeListener(ValueChangeListener l) {
      myValueChangeListeners.add(l);
   }
   
   public void removeValueChangeListener(ValueChangeListener l) {
      myValueChangeListeners.remove(l);
   }

   /**
    * This is called whenever we clear or set the composite property.  At the
    * time of this writing (Dec 13, 2013), the main listener in question
    * is the RerenderListener, which is needed if the composite property
    * controls any aspect of the rendering.
    */
   public void fireValueChangeListeners (ValueChangeEvent vce) {
      for (ValueChangeListener vcl : myValueChangeListeners) {
         vcl.valueChange(vce);
      }
   }

   protected void clearAction() {
      if (myProp != null) {
         // prop value is being set to null, so fire change listeners
         fireValueChangeListeners (new ValueChangeEvent (this, null));
         myProp.set (null);
      }
      setClearButtonFace (NULL);
   }

   protected void installClearButton (boolean install) {
      if (install != myClearButtonInstalled) {
         if (install) {
            int idx = indexOfMajor (mySetButton);
            if (myClearButtonFace == CLEAR) {
               addMajorComponent (myClearButton, idx);
            }
            else {
               addMajorComponent (myNullLabel, idx);
            }
         }
         else {
            if (myClearButtonFace == CLEAR) {
               removeMajorComponent (myClearButton);
            }
            else {
               removeMajorComponent (myNullLabel);
            }
         }
         revalidate();
         repaint();
         myClearButtonInstalled = install;
      }
   }

   protected void setClearButtonFace (int face) {
      if (myClearButtonInstalled) {
         if (myClearButtonFace != face) {
            if (face == CLEAR) {
               int idx = removeMajorComponent (myNullLabel);
               addMajorComponent (myClearButton, idx);
            }
            else if (face == NULL) {
               int idx = removeMajorComponent (myClearButton);
               addMajorComponent (myNullLabel, idx);
            }
            else {
               throw new InternalErrorException ("Unknown face spec: " + face);
            }
            revalidate();
            repaint();
            myClearButtonFace = face;
         }
      }
   }

   private void setProperties (HasProperties dst, HasProperties src) {
      for (PropertyInfo info : src.getAllPropertyInfo()) {
         if (info.isInheritable()) {
            InheritableProperty dstProp =
               (InheritableProperty)dst.getProperty (info.getName());
            InheritableProperty srcProp =
               (InheritableProperty)info.createHandle (src);
            dstProp.set (srcProp.get());
            dstProp.setMode (srcProp.getMode());
         }
         else if (!info.isReadOnly()) {
            Property dstProp = dst.getProperty (info.getName());
            Property srcProp = info.createHandle (src);
            dstProp.set (srcProp.get());
         }
      }
   }

   protected void setAction (PropTreeCell cell, HostList hostList) {
      if (!cell.hasChildren()) {
         cell.addChildren (hostList.commonProperties (cell, false));
         hostList.saveBackupValues (cell);
         hostList.addSubHostsIfNecessary (cell);
      }
      else if (cell.getValue() == null) {
         hostList.addSubHostsIfNecessary (cell);
      }
      hostList.getCommonValues (cell, /* live= */true);
      cell.setValue (CompositeProperty.class);

      LinkedList<Property> props =
         EditingProperty.createProperties (cell, hostList, /* isLive= */true);

   }

   private void onDialogClose() {
      mySetButton.setEnabled (true);
      myClearButton.setEnabled (true);
      setClearButtonFace (CLEAR);
      // property value been (most likely) changed, so fire change listeners.
      // Not sure we need to do this here though, since this handler is only
      // called in 'live' mode and so any value change should already have
      // resulted in the listeners firing.
      fireValueChangeListeners (new ValueChangeEvent (this, myProp.get()));
   }

   protected boolean setAction() {
      if (myProp == null) {
         return false;
      }
      else if (myProp instanceof EditingProperty) {
         // Live Mode:
         //
         // The tree expresses the maximum expansion of property values
         // obtained by GUI interaction, and its values are the common values
         // (as best determined) of all the components. In places where a
         // composite component has been cleared, the tree value is set to null
         // but the children remain intact.
         //
         // When this node is expanded for the first time, we need to
         // (1) create composite components if necessary
         // (2) store the actual composite component in the backup
         // (3) find the common properties
         // (4) store all original property values in the backup
         // (5) set the tree values to the common property values
         //
         // When this node is expanded for subsequent times:
         // If the composite componet has been set to null,
         // (1) re-add composite components from the backup

         EditingProperty pprop = (EditingProperty)myProp;
         HostList hostList = pprop.getHostList();
         PropTreeCell cell = pprop.getCell();

         // expand the cell if this the first expansion
         if (!cell.hasChildren()) {
            cell.addChildren (hostList.commonProperties (cell, false));
            hostList.saveBackupValues (cell);
            hostList.addSubHostsIfNecessary (cell);
            // CompositeProperty has been changed from null to non-null,
            // so fire the change listeners
            fireValueChangeListeners (new ValueChangeEvent (this, myProp.get()));
         }
         // add sub hosts if the current value is null
         else if (cell.getValue() == null) {
            hostList.addSubHostsIfNecessary (cell);
            // CompositeProperty has been changed from null to non-null,
            // so fire the change listeners
            fireValueChangeListeners (new ValueChangeEvent (this, myProp.get()));
         }
         hostList.getCommonValues (cell, /* live= */true);
         cell.setValue (CompositeProperty.class);

         LinkedList<Property> propList =
            EditingProperty.createProperties (cell, hostList, /* isLive= */true);

         PropertyPanel panel;
         if (RenderProps.class.isAssignableFrom (
                myProp.getInfo().getValueClass())) {
            panel = new RenderPropsPanel (propList);
         }
         else {
            panel = new PropertyPanel (propList);
         }
         Window owner = SwingUtilities.windowForComponent (this);

         PropertyDialog dialog = null;
         if (owner instanceof Frame) {
            dialog =
               new PropertyDialog (
                  (Frame)owner, myProp.getName(), panel, "Done");
         }
         else {
            dialog =
               new PropertyDialog (
                  (Dialog)owner, myProp.getName(), panel, "Done");
         }

         dialog.setModal (false);
         dialog.locateRight (this);
         myClearButton.setEnabled (false);
         mySetButton.setEnabled (false);
         dialog.addWindowListener (new WindowAdapter() {
            public void windowClosed (WindowEvent e) {
               onDialogClose();
            }
         });
         dialog.setVisible (true);
      }
      else {
         CompositeProperty newHost = null;

         if ((newHost =
            PropertyUtils.createInstance (
               myProp.getInfo(), myProp.getHost())) == null) {
            return false;
         }

         CompositeProperty oldHost = (CompositeProperty)myProp.get();
         if (oldHost != null) {
            setProperties (newHost, oldHost);
         }
         else {
            PropertyUtils.updateInheritedProperties (
               newHost, myProp.getHost(), myProp.getName());
         }
         PropertyPanel panel;
         if (RenderProps.class.isAssignableFrom (
                myProp.getInfo().getValueClass())) {
            panel =
               new RenderPropsPanel (PropertyUtils.createProperties (newHost));
         }
         else {
            LinkedList<Property> props = PropertyUtils.createProperties (newHost);
            panel = new PropertyPanel (props);
         }
         PropertyDialog dialog =
            new PropertyDialog (myProp.getName(), panel, "OK Cancel");

         dialog.setModal (true);
         dialog.locateRight (this);
         dialog.setVisible (true);
         if (dialog.getReturnValue() == OptionPanel.OK_OPTION) {
            myProp.set (newHost);
            if (oldHost == null) {
               setClearButtonFace (CLEAR);
            }
         }
         // property value been (most likely) changed, so fire change listeners
         fireValueChangeListeners (new ValueChangeEvent (this, myProp.get()));
         dialog.dispose();
      }
      return true;
   }

   private void createButtons() {
      myClearButton = new JButton ("clear");
      mySetButton = new JButton ("set ...");

      Insets margin = new Insets (4, 4, 4, 4);

      Dimension size = new Dimension (50, 20);
      myClearButton.setPreferredSize (size);
      myClearButton.setMinimumSize (size);
      myClearButton.setMaximumSize (size);
      myClearButton.setMargin (margin);
      myClearButton.setActionCommand ("clear");
      myClearButton.addActionListener (new ActionListener() {
         public void actionPerformed (ActionEvent e) {
            if (e.getActionCommand().equals ("clear")) {
               clearAction();
            }
         }
      });

      mySetButton.setPreferredSize (size);
      mySetButton.setMinimumSize (size);
      mySetButton.setMaximumSize (size);
      mySetButton.setMargin (margin);
      mySetButton.setActionCommand ("set");
      mySetButton.addActionListener (new ActionListener() {
         public void actionPerformed (ActionEvent e) {
            if (e.getActionCommand().equals ("set")) {
               setAction();
            }
         }
      });

      myNullLabel = new JLabel ("null");
      myNullLabel.setPreferredSize (size);
      myNullLabel.setMinimumSize (size);
      myNullLabel.setMaximumSize (size);
      myNullLabel.setHorizontalAlignment (SwingConstants.CENTER);
      myNullLabel.setBorder (BorderFactory.createLineBorder (Color.darkGray));

      // myNullLabel.setMargin (margin);

      // just install the set button for now ...
      addMajorComponent (mySetButton);
   }

   public void dispose() {
      super.dispose();
      myValueChangeListeners.clear();
      myProp = null;
   }
}
