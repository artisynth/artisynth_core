/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import java.awt.Component;

import javax.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import maspack.properties.*;
import maspack.util.Disposable;
import maspack.util.InternalErrorException;

/**
 * A container class which arranges its components horizontally using a box
 * layout, and has an optional label component at the left edge.
 * 
 * <p>
 * Other components can be added to this container as usual. However, components
 * which are added using {@link #addMajorComponent addMajorComponent} will be
 * accompanied by extra horizontal spacing (equal to the amount specified by
 * {@link #setSpacing setSpacing}.
 */
public class LabeledComponent extends LabeledComponentBase {
   private static final long serialVersionUID = 1L;
   private boolean myStretchableP = false;
   private int mySpacing = 4;
   private boolean myLabelWidthStretchableP = false;

   protected LabelSpacing myLabelSpacing = new LabelSpacing();

   public static PropertyList myProps =
      new PropertyList (LabeledComponent.class, LabeledComponentBase.class);


   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public boolean isLabelStretchable() {
      return myLabelWidthStretchableP;
   }

   public void setLabelStretchable (boolean flexible) {
      if (myLabelWidthStretchableP != flexible) {
         myLabelWidthStretchableP = flexible;
         respaceComponents();
      }
   }

   SizableLabel myLabel;
   ArrayList<Component> myComponents = new ArrayList<Component>();

   /**
    * Creates a new LabledContainer, with a label containing the specified text.
    * If the label text is null, then no label is created.
    * 
    * @param labelText
    * optional text for this component's label
    */
   public LabeledComponent (String labelText) {
      super();
      setLayout (new BoxLayout (this, BoxLayout.X_AXIS));
      add (Box.createRigidArea (new Dimension (mySpacing, 0)));
      if (labelText != null) {
         myLabel = new SizableLabel();
         myLabel.setFocusable (false);
         // myLabel.setBorder (BorderFactory.createLineBorder(Color.RED));
         // setBorder (BorderFactory.createLineBorder(Color.BLUE));

         addMajorComponent (myLabel);
         myLabel.setText (labelText);
      }
   }

   /**
    * Creates a new LabledContainer, with a label containing the specified text
    * and an additional major component. Spacing is inserted between the label
    * and the component. If the label text is null, then no label is created.
    * 
    * @param labelText
    * optional text for this component's label
    * @param comp
    * major component to add
    */
   public LabeledComponent (String labelText, Component comp) {
      this (labelText);
      addMajorComponent (comp);
   }

   /**
    * Returns true if this component is horizontally stretchable.
    * 
    * @return true if this component is stretchable
    * @see #setStretchable
    */
   public boolean isStretchable() {
      return myStretchableP;
   }

   /**
    * Specifies whether or not this component is horizontally stretchable. By
    * default, labeled components are not stretchable, which implies that their
    * sizes are fixed at their preferred size.
    * 
    * @param enable
    * if true, makes this component horizontally stretchable
    * @see #isStretchable
    */
   public void setStretchable (boolean enable) {
      myStretchableP = enable;
   }

   /**
    * Sets the spacing associated with this component. Spacing is inserted
    * between the label and all components added using {@link #addMajorComponent
    * addMajorComponent}.
    * 
    * @param m
    * new spacing value, in pixels
    * @see #getSpacing
    */
   public void setSpacing (int m) {
      mySpacing = m;
      // remove all components and add them back with new
      // margin space in between
      respaceComponents();
   }

   private void respaceComponents() {
      removeAll();
      add (Box.createRigidArea (new Dimension (mySpacing, 0)));
      for (Iterator<Component> it = myComponents.iterator(); it.hasNext();) {
         Component c = it.next();
         add (c);
         add (createSpacer (c));
      }
   }

   /**
    * Gets the spacing associated with this component.
    * 
    * @return spacing (in pixels) for this component
    * @see #setSpacing
    */
   public int getSpacing() {
      return mySpacing;
   }

   /**
    * Gets the major component located at a specific index.
    * 
    * @param idx
    * index of the major component
    * @return major component at idx
    * @see #addMajorComponent
    */
   public Component getMajorComponent (int idx) {
      return myComponents.get (idx);
   }

   /**
    * Gets an interator through all the major components.
    * 
    * @return major component iterator
    * @see #addMajorComponent
    */
   Iterator<Component> getMajorComponents() {
      return myComponents.iterator();
   }

   private Component createSpacer (Component comp) {
      if (comp == myLabel && myLabelWidthStretchableP) {
         return new Box.Filler (new Dimension (mySpacing, 0), new Dimension (
            mySpacing, 0), new Dimension (Integer.MAX_VALUE, 0));
      }
      else {
         return Box.createRigidArea (new Dimension (mySpacing, 0));
      }
   }

   /**
    * Adds a major component to this labeled component. A major component
    * differs from other components in that it is separated from other
    * components by some horizontal spacing (see {@link #getSpacing
    * getSpacing}).
    * 
    * @param comp
    * component to add
    */
   public void addMajorComponent (Component comp) {
      add (comp);
      add (createSpacer (comp));
      myComponents.add (comp);
   }

   /**
    * Adds a major component to this labeled component at a specified location.
    * A major component differs from other components in that it is separated
    * from other components by some horizontal spacing (see {@link #getSpacing
    * getSpacing}).
    * 
    * @param comp
    * component to add
    * @param idx
    * location within the major component list
    */
   public void addMajorComponent (Component comp, int idx) {
      if (idx > myComponents.size()) {
         throw new IllegalArgumentException ("specified component location "
         + idx + " is out of range");
      }
      myComponents.add (idx, comp);
      if (getComponentCount() == 0) {
         System.out.println (getClass()+""+hashCode());
      }
      add (comp, 2 * idx + 1);
      add (createSpacer (comp), 2 * idx + 2);
   }

   public int numMajorComponents() {
      return myComponents.size();
   }

   /**
    * Removes the specified major component from this labeled component.
    * 
    * @param comp
    * component to remove
    * @see #addMajorComponent
    */
   public int removeMajorComponent (Component comp) {
      int idx = myComponents.indexOf (comp);
      if (idx != -1) {
         myComponents.remove (comp);
         Component[] clist = getComponents();
         for (int i = 0; i < clist.length; i++) {
            if (clist[i] == comp) {
               remove (i);
               remove (i);
               break;
            }
         }
         if (comp == myLabel) {
            myLabel = null;
         }
      }
      return idx;
   }

   /**
    * Gets the index of a specified major component, or -1 if the component is
    * not present.
    */
   public int indexOfMajor (Component comp) {
      return myComponents.indexOf (comp);
   }

   /**
    * Returns the JLabel associated with this control.
    * 
    * @return label component for this control
    */
   public JLabel getLabel() {
      return myLabel;
   }

   public Component getMainComponent() {
      return this;
   }


   /**
    * {@inheritDoc}
    */
   public void setLabelText (String text) {
      if (myLabel == null) {
         myLabel = new SizableLabel (text);
         myLabel.setFocusable (false);
         addMajorComponent (myLabel, 0);
      }
      super.setLabelText (text);
   }

   public Dimension getMinimumSize() {
      if (!myStretchableP) {
         return getPreferredSize();
      }
      else {
         Dimension psize = getPreferredSize();
         Dimension msize = super.getMinimumSize();
         msize.height = psize.height;
         return msize;
      }
   }

   /**
    * {@inheritDoc}
    */
   public Dimension getMaximumSize() {
      if (!myStretchableP) {
         return getPreferredSize();
      }
      else {
         Dimension psize = getPreferredSize();
         Dimension msize = super.getMaximumSize();
         msize.height = psize.height;
         msize.width = Integer.MAX_VALUE;
         return msize;
      }
   }

   public void getLabelSpacing (LabelSpacing spacing) {
      spacing.set (myLabelSpacing);
   }

   public void setLabelSpacing (LabelSpacing spacing) {
      if (spacing.labelWidth != myLabelSpacing.labelWidth) {
         if (myLabel != null) {
            int w = spacing.labelWidth - LabeledComponentBase.getLeftInset(this);
            myLabel.setMinimumWidth (w);
            revalidate();
         }
         myLabelSpacing.labelWidth = spacing.labelWidth;
      }
      if (spacing.preSpacing != myLabelSpacing.preSpacing) {
         if (myLabelSpacing.preSpacing != 0) {
            removeMajorComponent (getMajorComponent(0));
         }
         if (spacing.preSpacing != 0 && !PropertyWidget.hasModeButton (this)) {
            addMajorComponent (
               Box.createRigidArea (new Dimension (spacing.preSpacing, 0)), 0);
         }
         myLabelSpacing.preSpacing = spacing.preSpacing;
      }      
   }

   public void getPreferredLabelSpacing (LabelSpacing spacing) {
      spacing.labelWidth = LabeledComponentBase.getLeftInset (this);
      if (myLabel != null) {
         spacing.labelWidth += myLabel.getPreferredWidth();
      }
      spacing.preSpacing = myLabelSpacing.preSpacing;
   }

//    /**
//     * Returns the width, in pixels, of the label associated with this component.
//     * 
//     * @return label width
//     * @see #setLabelWidth
//     */
//    public int getLabelWidth() {
//       return myLabelSpacing.labelWidth;
//    }

//    /** 
//     * Returns the spacing, if any, before the label. If there is
//     * no spacing, zero is returned.
//     * 
//     * @return spacing before the label.
//     */
//    public int getPrelabelSpacing () {
//       return myLabelSpacing.preSpacing;
//    }


//    /**
//     * {@inheritDoc}
//     */
//    public void setLabelWidth (int w) {
//       myLabelSpacing.labelWidth = w;
//       if (getLabelText().equals ("renderProps")) {
//          System.out.println ("renderProps: " + w);
//       }
//       if (myLabel != null) {
//          w -= LabeledComponentBase.getLeftInset(this);
//          myLabel.setMinimumWidth (w);
//          revalidate();
//       }

//    }

//    /**
//     * {@inheritDoc}
//     */
//    public void setPrelabelSpacing (int spacing) {
//       if (spacing < 0) {
//          throw new IllegalArgumentException ("Spacing must not be negative");
//       }
//       if (spacing != myLabelSpacing.preSpacing) {
//          if (myLabelSpacing.preSpacing != 0) {
//             removeMajorComponent (getMajorComponent(0));
//          }
//          if (spacing != 0 && !PropertyWidget.hasModeButton (this)) {
//             addMajorComponent (
//                Box.createRigidArea (new Dimension (spacing, 0)), 0);
//          }
//          myLabelSpacing.preSpacing = spacing;
//       }
//    }


//    public int getPreferredLabelWidth() {
//       int prefWidth = LabeledComponentBase.getLeftInset (this);
//       if (myLabel != null) {
//          prefWidth += myLabel.getPreferredWidth();
//       }
//       return prefWidth;
//    }

//    public int getPreferredPrelabelSpacing() {
//       return myLabelSpacing.preSpacing;
//    }

   public void dispose() {
      Component[] comps = getComponents();
      for (int i = 0; i < comps.length; i++) {
         if ((comps[i] instanceof LabeledComponent)) {
            ((LabeledComponent)comps[i]).dispose();
         }
      }
      removeAll();
   }

   public ArrayList<String> getActions() {
      return new ArrayList<String>();
   }

   public void actionPerformed (ActionEvent e) {
   }

}
