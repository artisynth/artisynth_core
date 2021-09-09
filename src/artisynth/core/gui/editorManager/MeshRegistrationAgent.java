/**
 * Copyright (c) 2014, by the Authors: John Lloyd (UBC), Tracy Wilkinson (UBC) and
 * ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */package artisynth.core.gui.editorManager;

import java.awt.*;
import java.util.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.filechooser.*;

import java.io.File;

import maspack.geometry.*;
import maspack.matrix.*;
import maspack.geometry.ICPRegistration.*;
import maspack.spatialmotion.*;
import maspack.properties.*;
import maspack.widgets.*;
import maspack.widgets.PropertyPanel;
import maspack.widgets.FileNameField;
import maspack.widgets.LabeledComponent;
import maspack.widgets.ValueChangeEvent;
import maspack.widgets.ValueChangeListener;
import artisynth.core.driver.*;
import artisynth.core.gui.*;
import artisynth.core.gui.selectionManager.*;
import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.*;
import artisynth.core.util.ArtisynthPath;

public class MeshRegistrationAgent
   extends FrameBasedEditingAgent implements HasProperties, SelectionListener {

   public enum Method {
      ICP 
   };

   StringField mySourceField;

   ModelComponent[] myComps = new MeshComponent[2];
   int mySourceIdx = 0;

   PropertyPanel myPanel;

   static Method DEFAULT_METHOD = Method.ICP;
   Method myMethod = DEFAULT_METHOD;

   static PropertyList myProps = new PropertyList (MeshRegistrationAgent.class);

   static {
      myProps.add ("method", "registration method", DEFAULT_METHOD);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public Property getProperty (String name) {
      return myProps.get(name).createHandle (this);
   }

   public Method getMethod() {
      return myMethod;
   }

   public void setMethod (Method method) {
      myMethod = method;
   }

   PolygonalMesh getMesh (int idx) {
      if (myComps[idx] instanceof RigidBody) {
         return ((RigidBody)myComps[idx]).getSurfaceMesh();
      }
      else {
         return null;
      }
   }

   PolygonalMesh getTransformedMesh (int idx) {
      if (myComps[idx] instanceof RigidBody) {
         RigidBody body = (RigidBody)myComps[idx];
         PolygonalMesh mesh = body.getSurfaceMesh().clone();
         mesh.transform (body.getPose());
         return mesh;
      }
      else {
         return null;
      }
   }

   void applyTransformToSource (AffineTransform3d X) {
      RigidTransform3d T = new RigidTransform3d();
      T.R.set (X.A);
      T.R.normalize();
      T.p.set (X.p);
      if (myComps[mySourceIdx] instanceof RigidBody) {
         RigidBody source = (RigidBody)myComps[mySourceIdx];
         RigidTransform3d TSW = new RigidTransform3d(source.getPose());
         System.out.println ("TSW=\n" + TSW.toString("%10.5f"));
         TSW.mul (T, TSW);
         source.setPose (TSW);
      }
   }

   public MeshRegistrationAgent (
      Main main, ModelComponent source, ModelComponent target) {
      super (main);
      myComps = new ModelComponent[2];
      myComps[0] = source;
      myComps[1] = target;
      mySourceIdx = 0;
   }

   protected void createDisplay() {
      createDisplayFrame ("Set geometry");

      myPanel = new PropertyPanel (this);
      mySourceField =
         new StringField ("target", 80);
      mySourceField.setValue (ComponentUtils.getPathName(myComps[0]));
      mySourceField.setEnabledAll (false);
      mySourceField.setStretchable (true);
      myPanel.addWidget (mySourceField);
      addWidget (myPanel);
      createOptionPanel ("Cancel Register Done");
   }

   public void actionPerformed (ActionEvent e) {
      String cmd = e.getActionCommand();
      if (cmd.equals ("Register")) {
         ICPRegistration icp = new ICPRegistration();
         AffineTransform3d X = new AffineTransform3d();
         PolygonalMesh source = null;
         PolygonalMesh target = null;
         source = getTransformedMesh (mySourceIdx);
         target = getMesh ((mySourceIdx+1)%2);
         icp.registerICP (
            X, target, source, 3, 6);
         //Prealign.NONE, new int[] {3, 6}); 
         System.out.println ("X=\n" + X.toString("%10.5f"));
         applyTransformToSource (X);
         myMain.rerender();
      }
      else if (cmd.equals ("Cancel")) {
         myDisplay.setVisible (false);
         myMain.rerender();
         dispose();
      }
      else if (cmd.equals ("Done")) {
         myDisplay.setVisible (false);
         myMain.rerender();
         dispose();
      }
      else {
         super.actionPerformed (e);
      }
   }

   public void selectionChanged (SelectionEvent e) {
      LinkedList<ModelComponent> added = e.getAddedComponents();
      if (added != null && added.size() == 1) {
         if (added.get(0) == myComps[0]) {
            mySourceField.setValue (ComponentUtils.getPathName(myComps[0]));
            mySourceIdx = 0;
         }
         else if (added.get(0) == myComps[1]) {
            mySourceField.setValue (ComponentUtils.getPathName(myComps[1]));
            mySourceIdx = 1;
         }
      }
   }

   public void show (Rectangle popupBounds) {
      super.show (popupBounds);
      mySelectionManager.addSelectionListener (this);
   }

   public void dispose() {
      super.dispose();
      myEditManager.releaseEditLock();
      mySelectionManager.removeSelectionListener (this);
   }

   protected boolean isContextValid() {
      return true;
      //return (ComponentUtils.withinHierarchy (myBody, myMain.getRootModel()));
   }
   
}
