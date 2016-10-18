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

public class LabeledPanel extends LabeledComponentBase {
   
   protected PropertyPanel myPanel;
   protected LabeledComponent myMainWidget;

   private void addHorizontalGlue () {
      add (new Box.Filler (
              new Dimension (2, 0),
              new Dimension (2, 0),
              new Dimension (Integer.MAX_VALUE, 0)));
   }

   public LabeledPanel () {
      myPanel = new PropertyPanel();
      setLayout (new BoxLayout(this, BoxLayout.X_AXIS));
      add (myPanel);
      addHorizontalGlue();
      //      myPanel.setBorder (BorderFactory.createLineBorder (Color.RED));
   }

   protected void setMainWidget (LabeledComponent widget) {
      myMainWidget = widget;
   }
   
   protected LabeledComponent getMainWidget() {
      return myMainWidget;
   }

   public JLabel getLabel() {
      return myMainWidget.getLabel();
   }

   public Component getMainComponent() {
      return this;
   }

   public void getLabelSpacing (LabelSpacing spacing) {
      myPanel.getLabelSpacing (spacing);
   }
   
   public void setLabelSpacing (LabelSpacing spacing) {
      LabelSpacing subspacing = new LabelSpacing();
      subspacing.set (spacing);
      subspacing.labelWidth -= LabeledComponentBase.getLeftInset(this);
      myPanel.setLabelSpacing (subspacing);
   }
   
   public void getPreferredLabelSpacing (LabelSpacing spacing) {
      myPanel.getPreferredLabelSpacing (spacing);
      spacing.labelWidth += LabeledComponentBase.getLeftInset (this);
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
   }
   
                               
}
