/**
 * Copyright (c) 2014, by the Authors: John Lloyd (UBC), Tracy Wilkinson (UBC) and
 * ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */package artisynth.core.gui.editorManager;

import java.awt.event.ActionEvent;
import java.util.HashMap;

import maspack.properties.EditingProperty;
import maspack.properties.HasProperties;
import maspack.properties.HostList;
import maspack.properties.PropTreeCell;
import maspack.util.InternalErrorException;
import maspack.widgets.DoubleField;
import maspack.widgets.OptionPanel;
import maspack.widgets.PropertyDialog;
import maspack.widgets.PropertyPanel;
import maspack.widgets.StringField;
import maspack.widgets.StringSelector;
import maspack.widgets.ValueChangeEvent;
import maspack.widgets.ValueChangeListener;
import artisynth.core.driver.Main;
import artisynth.core.modelbase.CopyableComponent;
import artisynth.core.modelbase.Traceable;
import artisynth.core.probes.Probe;
import artisynth.core.probes.TracingProbe;
import artisynth.core.workspace.RootModel;

public class TracingProbePanel extends PropertyDialog implements
ValueChangeListener {
   Double foo;
   Traceable myTraceable;
   String myTraceableName;
   TracingProbe myPrototype;
   PropTreeCell myPropTree;
   String[] myExcludedProps =
      { "attachedFile", "name", "format", "interpolationOrder", "showTime",
        "showHeader", "renderProps", "updateInterval" };
   int myNumFixedWidgets = 0;

   StringSelector myTraceSelector;
   StringField myNameField;
   DoubleField myDurationField;
   DoubleField myStartTimeField;
   DoubleField myUpdateIntervalField;
   Main myMain;

   private static double myDefaultDuration = 5.0;

   static HashMap<Class<?>,TracingProbe> myPrototypeMap =
      new HashMap<Class<?>,TracingProbe>();

   protected void setProperties (HasProperties dst, HasProperties src) {
      myPropTree.setTreeValuesInHost (dst, src);
   }

   protected TracingProbe getPrototypeProbe (String traceableName) {
      TracingProbe tmpProbe = 
         TracingProbe.create (myTraceable, traceableName);
      Class<?> type = tmpProbe.getClass();
      TracingProbe comp = myPrototypeMap.get (type);
      if (comp == null) { // look for prototype as the last instance in the
                          // container
         RootModel root = myMain.getRootModel();
         for (Probe probe : root.getOutputProbes()) {
            if (type.isAssignableFrom (probe.getClass())) {
               comp = (TracingProbe)probe;
            }
         }
         if (comp == null || !(comp instanceof CopyableComponent)) {
            comp = tmpProbe;
//            try {
//               comp = (TracingProbe)type.newInstance();
//            }
//            catch (Exception e) {
//               throw new InternalErrorException (
//                  "cannot create no-args instance of " + type);
//            }
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
         myMain.getRootModel().getMaxStepSize());
   }

   protected void setTracingProbe (String traceableName) {
      if (traceableName.equals (myTraceableName)) {
         return;
      }
      myTraceableName = traceableName;
      // if (myPrototype != null)
      // { setProperties (myPrototype, myPrototype);
      // }
      myPrototype = getPrototypeProbe (traceableName);
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
            myPanel.removeWidget (myNumFixedWidgets);
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
    * Called when traceable is changed.
    */
   public void valueChange (ValueChangeEvent e) {
      setTracingProbe ((String)myTraceSelector.getValue());
   }

   public TracingProbePanel (Traceable tcomp, String[] traceables) {
      super ("Select tracing", new PropertyPanel(), "OK Cancel");
      myTraceable = tcomp;

      myMain = Main.getMain();
      String defaultName = null;
      double defaultStartTime = myMain.getTime();

      myTraceSelector =
         new StringSelector ("traceable", traceables[0], traceables);
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

      setTracingProbe (traceables[0]);
      setModal (true);
   }

   public TracingProbe createProbe (Traceable tcomp) {
      String traceableName = (String)myTraceSelector.getValue();
      TracingProbe probe = TracingProbe.create (tcomp, traceableName);
      if (getProbeName() != null) {
         probe.setName (getProbeName());
      }
      probe.setStartTime (getStartTime());
      probe.setStopTime (getStartTime() + getDuration());
      probe.setUpdateInterval (getUpdateInterval());
      setProperties (probe, myPrototype);
      return probe;
   }

   public String getTraceable() {
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
