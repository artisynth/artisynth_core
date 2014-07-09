/**
 * Copyright (c) 2014, by the Authors: John Lloyd (UBC), Tracy Wilkinson (UBC) and
 * ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */package artisynth.core.gui.editorManager;

import java.awt.event.*;
import java.util.*;

import artisynth.core.driver.*;
import artisynth.core.gui.*;
import artisynth.core.gui.widgets.*;
import artisynth.core.util.TimeBase;
import artisynth.core.workspace.RootModel;
import artisynth.core.modelbase.*;
import artisynth.core.probes.*;
import maspack.util.*;
import maspack.widgets.DoubleField;
import maspack.widgets.OptionPanel;
import maspack.widgets.PropertyDialog;
import maspack.widgets.PropertyPanel;
import maspack.widgets.StringField;
import maspack.widgets.StringSelector;
import maspack.widgets.ValueChangeEvent;
import maspack.widgets.ValueChangeListener;
import maspack.properties.*;

public class TracingProbePanel extends PropertyDialog implements
ValueChangeListener {
   Double foo;
   Tracable myTracable;
   String myTracableName;
   TracingProbe myPrototype;
   PropTreeCell myPropTree;
   String[] myExcludedProps =
      { "attachedFile", "name", "format", "interpolationOrder", "showTime",
       "showHeader", "renderProps" };
   int myNumFixedWidgets = 0;

   StringSelector myTraceSelector;
   StringField myNameField;
   DoubleField myDurationField;
   DoubleField myStartTimeField;
   DoubleField myUpdateIntervalField;

   private static double myDefaultDuration = 5.0;

   static HashMap<Class,TracingProbe> myPrototypeMap =
      new HashMap<Class,TracingProbe>();

   protected void setProperties (HasProperties dst, HasProperties src) {
      myPropTree.setTreeValuesInHost (dst, src);
   }

   protected TracingProbe getPrototypeProbe (String tracableName) {
      TracingProbe tmpProbe = myTracable.getTracingProbe (tracableName);
      Class type = tmpProbe.getClass();
      TracingProbe comp = myPrototypeMap.get (type);
      if (comp == null) { // look for prototype as the last instance in the
                          // container
         RootModel root = Main.getRootModel();
         for (Probe probe : root.getOutputProbes()) {
            if (type.isAssignableFrom (probe.getClass())) {
               comp = (TracingProbe)probe;
            }
         }
         if (comp == null || !(comp instanceof CopyableComponent)) {
            try {
               comp = (TracingProbe)type.newInstance();
            }
            catch (Exception e) {
               throw new InternalErrorException (
                  "cannot create no-args instance of " + type);
            }
            initializePrototype (comp);
         }
         else {
            comp = (TracingProbe)((CopyableComponent)comp).copy (0, null);
         }
         myPrototypeMap.put (type, comp);
      }
      return comp;
   }

   private void initializePrototype (TracingProbe probe) {
      probe.getProperty ("updateInterval").set (
         Main.getRootModel().getMaxStepSize());
   }

   protected void setTracingProbe (String tracableName) {
      if (tracableName.equals (myTracableName)) {
         return;
      }
      myTracableName = tracableName;
      // if (myPrototype != null)
      // { setProperties (myPrototype, myPrototype);
      // }
      myPrototype = getPrototypeProbe (tracableName);
      if (myPrototype != null) {
         HostList hostList = new HostList (new HasProperties[] { myPrototype });
         myPropTree = hostList.commonProperties (null, false);

         // remove properties which are to be excluded
         for (int i = 0; i < myExcludedProps.length; i++) {
            myPropTree.removeDescendant (myExcludedProps[i]);
         }
         hostList.saveBackupValues (myPropTree);
         hostList.getCommonValues (myPropTree, /* live= */true);

         // update fixed widgets
         myUpdateIntervalField.setValue (
            myPrototype.getProperty ("updateInterval").get());

         // remove non-fixed widgets
         int deleteCnt = myPanel.getComponentCount() - myNumFixedWidgets;
         for (int i = 0; i < deleteCnt; i++) {
            myPanel.remove (myNumFixedWidgets);
         }
         // add the new widgets
         myPanel.addWidgets (EditingProperty.createProperties (
            myPropTree, hostList, /* isLive= */false));

         validate();
         if (isVisible()) {
            pack();
         }
      }
   }

   /**
    * Called when tracable is changed.
    */
   public void valueChange (ValueChangeEvent e) {
      setTracingProbe ((String)myTraceSelector.getValue());
   }

   public TracingProbePanel (Tracable tcomp, String[] tracables) {
      super ("Select tracing", new PropertyPanel(), "OK Cancel");
      myTracable = tcomp;

      String defaultName = null;
      double defaultStartTime = Main.getTime();

      myTraceSelector =
         new StringSelector ("tracable", tracables[0], tracables);
      myTraceSelector.addValueChangeListener (this);
      addWidget (myTraceSelector);

      myNameField = new StringField ("name", defaultName, 10);
      addWidget (myNameField);
      myStartTimeField =
         new DoubleField ("startTime", defaultStartTime, "%7.4f");
      addWidget (myStartTimeField);
      myDurationField =
         new DoubleField ("duration", myDefaultDuration, "%7.4f");
      addWidget (myDurationField);
      myUpdateIntervalField = new DoubleField ("updateInterval", 0.01, "%7.4f");
      addWidget (myUpdateIntervalField);

      myNumFixedWidgets = myPanel.getComponentCount();

      setTracingProbe (tracables[0]);
      setModal (true);
   }

   public TracingProbe createProbe (Tracable tcomp) {
      TracingProbe probe =
         tcomp.getTracingProbe ((String)myTraceSelector.getValue());
      if (getProbeName() != null) {
         probe.setName (getProbeName());
      }
      probe.setStartTime (getStartTime());
      probe.setStopTime (getStartTime() + getDuration());
      probe.setUpdateInterval (getUpdateInterval());
      setProperties (probe, myPrototype);
      return probe;
   }

   public String getTracable() {
      return (String)myTraceSelector.getValue();
   }

   public double getDuration() {
      return myDurationField.getDoubleValue();
   }

   public double getStartTime() {
      return myStartTimeField.getDoubleValue();
   }

   public double getUpdateInterval() {
      return myUpdateIntervalField.getDoubleValue();
   }

   public String getProbeName() {
      return (String)myNameField.getValue();
   }

   public void actionPerformed (ActionEvent e) {
      String actionCmd = e.getActionCommand();
      if (actionCmd.equals ("OK")) {
         myReturnValue = OptionPanel.OK_OPTION;
         myDefaultDuration = getDuration();
         setProperties (myPrototype, myPrototype);
      }
      else if (actionCmd.equals ("Cancel")) {
         myReturnValue = OptionPanel.CANCEL_OPTION;
      }
      setVisible (false);
      dispose();
   }
}
