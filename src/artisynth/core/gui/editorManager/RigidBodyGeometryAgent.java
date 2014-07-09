/**
 * Copyright (c) 2014, by the Authors: John Lloyd (UBC), Tracy Wilkinson (UBC) and
 * ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */package artisynth.core.gui.editorManager;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.filechooser.*;

import java.io.File;

import maspack.geometry.*;
import maspack.spatialmotion.*;
import maspack.widgets.DoubleField;
import maspack.widgets.FileNameField;
import maspack.widgets.LabeledComponent;
import maspack.widgets.ValueChangeEvent;
import maspack.widgets.ValueChangeListener;
import artisynth.core.driver.*;
import artisynth.core.gui.*;
import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.*;
import artisynth.core.util.ArtisynthPath;

public class RigidBodyGeometryAgent extends FrameBasedEditingAgent {
   RigidBody myBody;
   GeometryInertiaPanel myPanel;

   public RigidBodyGeometryAgent (Main main, RigidBody body) {
      super (main);
      myBody = body;
   }

   protected void createDisplay() {
      createDisplayFrame ("Set geometry");

      myPanel = new GeometryInertiaPanel (null, myBody, /* editing= */true);
      addWidget (myPanel);
      createOptionPanel ("Reset Done");
   }

   public void actionPerformed (ActionEvent e) {
      String cmd = e.getActionCommand();
      if (cmd.equals ("Reset")) {
         myPanel.resetGeometryAndInertia();
      }
      else if (cmd.equals ("Done")) {
         myPanel.setBodyInertia (myBody);
         myDisplay.setVisible (false);
         myMain.rerender();
         dispose();
      }
      else {
         super.actionPerformed (e);
      }
   }

   public void dispose() {
      super.dispose();
      myEditManager.releaseEditLock();
   }

   protected boolean isContextValid() {
      return (ComponentUtils.withinHierarchy (myBody, myMain.getRootModel()));
   }

}
