/**
 * Copyright (c) 2014, by the Authors: John Lloyd (UBC), Tracy Wilkinson (UBC) and
 * ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */package artisynth.core.gui.editorManager;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Component;
import java.awt.Font;
import java.awt.Cursor;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;

import maspack.render.*;
import maspack.render.GL.GLViewer;
import maspack.geometry.*;
import maspack.matrix.*;
import maspack.util.*;
import maspack.properties.*;
import artisynth.core.driver.*;
import artisynth.core.gui.*;
import artisynth.core.gui.selectionManager.*;
import artisynth.core.mechmodels.*;
import artisynth.core.modelbase.*;
import artisynth.core.workspace.RootModel;

/**
 * Responsible for adding Particles to a MechModel.
 */
public class ParticleAgent extends AddComponentAgent<Particle> {
   private MechModel myModel;

   private static HashMap<Class,ModelComponent> myPrototypeMap;
   private static RootModel myLastRootModel = null;

   private JCheckBox myUsePlaneToggle;

   protected void initializePrototype (ModelComponent comp, Class type) {
      if (type == Particle.class) {
         Particle mkr = (Particle)comp;
         RenderProps.setPointRadius (mkr, getDefaultPointRadius());
      }
      else {
         throw new InternalErrorException ("unimplemented type " + type);
      }
   }

   protected void setInitialState() {
      setState (State.SelectingLocation);
   }

   protected void resetState() {
      setState (State.SelectingLocation);
   }

   private enum State {
      SelectingLocation,
   };

   private State myState = State.SelectingLocation;

   private void setState (State state) {
      switch (state) {
         case SelectingLocation: {
            myInstructionBox.setText ("Pick location in viewer");
            installLocationListener();
            break;
         }
         default: {
            throw new InternalErrorException ("Unhandled state " + state);
         }
      }
      myState = state;
   }

   public ParticleAgent (Main main, MechModel model) {
      super (main, (ComponentList<Particle>)model.particles(), model);
      myModel = model;
   }

   protected void createDisplay() {
      createDisplayFrame ("Add Particles");

      addComponentType (Particle.class, new String[] {
            "name", "position", "velocity", "externalForce",
         });
      addBasicProps (Particle.class, new String[] {
            "renderProps", "mass"
         });
         
      createComponentList (
         "Existing particles:",
         new ComponentListWidget<Particle> (
            myModel.particles(), myModel));
      // createSeparator();
      // createTypeSelector();
      createNameField();
      createPropertyFrame("Default particle properties:");
      // createSeparator();
      // createProgressBox();
      createInstructionBox();
      myUsePlaneToggle = new JCheckBox ("constrain to plane");
      addWidget (myUsePlaneToggle);
      createOptionPanel ("Done");
   }

   protected HashMap<Class,ModelComponent> getPrototypeMap() {
      RootModel root = myMain.getRootModel();
      if (root != null && root != myLastRootModel) {
         myPrototypeMap = new HashMap<Class,ModelComponent>();
         myLastRootModel = root;
      }
      return myPrototypeMap;
   }

   public void handleLocationEvent (GLViewer viewer, MouseRayEvent rayEvent) {
      Point3d isect;

      if (myUsePlaneToggle.isSelected() && viewer.getNumClipPlanes() > 0) {
         isect =
            intersectClipPlane (rayEvent.getRay(), viewer.getClipPlane (0));
      }
      else {
         isect =
            intersectViewPlane (rayEvent.getRay(), getCenter (myModel), viewer);
      }
      if (isect != null) {
         createAndAddParticle (isect);
      }
   }

   private void createAndAddParticle (Point3d pnt) {
      Particle particle = new Particle (1, pnt);
      particle.setName (getNameFieldValue());
      setProperties (particle, getPrototypeComponent (myComponentType));
      // update properties in the prototype as well ...
      setProperties (myPrototype, myPrototype);

      addComponent (new AddComponentsCommand (
         "add Particle", particle, (MutableCompositeComponent<?>)myModel.particles()));

      setState (State.SelectingLocation);
   }

   protected boolean isContextValid() {
      return (ComponentUtils.withinHierarchy (myModel, myMain.getRootModel()));
   }

}
