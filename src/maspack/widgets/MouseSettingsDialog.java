/**
 * Copyright (c) 2017, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import java.awt.event.ActionEvent;
import java.util.LinkedList;
import java.util.List;

import maspack.util.InternalErrorException;

public class MouseSettingsDialog
   extends PropertyDialog implements ValueChangeListener {

   protected LinkedList<ValueChangeListener> myChangeListeners =
      new LinkedList<ValueChangeListener>();

   protected void fireValueChangeListeners (Object value) {
      for (ValueChangeListener l : myChangeListeners) {
         l.valueChange (new ValueChangeEvent (this, value));
      }
   }

   public MouseSettingsDialog (
      String title, MouseBindings bindings, List<MouseBindings> allBindings,
      double zoomScale) {

      super();
      setTitle (title);
      initialize ("Done");
      MouseSettingsPanel panel =
         new MouseSettingsPanel (bindings, allBindings, zoomScale);
      setPanel (panel);
      PropertyPanel.addValueChangeListener (
         panel.getWheelZoomField(), this);
      PropertyPanel.addValueChangeListener (
         panel.getBindingsField(), this);
      setDefaultCloseOperation (DISPOSE_ON_CLOSE);
      pack();    
   }
   
   public MouseBindings getBindings() {
      return ((MouseSettingsPanel)myPanel).getBindings();
   }

   public double getWheelZoom() {
      return ((MouseSettingsPanel)myPanel).getWheelZoom();
   }

   public void actionPerformed (ActionEvent e) {
      String actionCmd = e.getActionCommand();
      if (actionCmd.equals ("Done")) {
         myReturnValue = OptionPanel.OK_OPTION;
         setVisible (false);
         dispose();
      }
      else {
         throw new InternalErrorException ("Unimplemented action command "
         + actionCmd);
      }
   }

   /**
    * Adds a ValueChangeListener to this dialog.
    * 
    * @param l
    * listener to add
    */
   public void addValueChangeListener (ValueChangeListener l) {
      myChangeListeners.add (l);
   }

   /**
    * Removes a ValueChangeListener from this dialog.
    * 
    * @param l
    * listener to remove
    */
   public void removeValueChangeListener (ValueChangeListener l) {
      myChangeListeners.remove (l);
   }

   /**
    * Returns a list of all the ValueChangeListeners currently held by this
    * dialog.
    * 
    * @return list of ValueChangeListeners
    */
   public ValueChangeListener[] getValueChangeListeners() {
      return myChangeListeners.toArray (new ValueChangeListener[0]);
   }

   /**
    * Releases all the resources held by this control.
    */
   public void dispose() {
      super.dispose();
      myPanel.dispose();
      myChangeListeners.clear();
   }

   protected void finalize() throws Throwable {
      dispose();
      super.finalize();
   }

   public void valueChange (ValueChangeEvent evt) {
      Object source = evt.getSource();
      fireValueChangeListeners (evt.getValue());
   }

}
