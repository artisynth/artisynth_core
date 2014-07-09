/**
 * Copyright (c) 2014, by the Authors: John Lloyd (UBC), Tracy Wilkinson (UBC) and
 * ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */package artisynth.core.gui.editorManager;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;

import javax.swing.JMenuItem;

import artisynth.core.driver.Main;
import artisynth.core.femmodels.FemElement;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.FemElement3d;
import artisynth.core.femmodels.MuscleBundle;
import artisynth.core.femmodels.MuscleElementDesc;
import artisynth.core.femmodels.FemMuscleModel;
import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.*;
import artisynth.core.gui.selectionManager.SelectionManager;
import maspack.properties.PropertyMode;
import maspack.render.RenderProps;
import maspack.util.InternalErrorException;
import maspack.widgets.DoubleField;
import maspack.widgets.GuiUtils;
import maspack.widgets.OptionPanel;
import maspack.widgets.WidgetDialog;

public class MuscleBundleEditor extends EditorBase {

   private static double myLastElementFibreDist = 0.0;

   public MuscleBundleEditor (Main main, EditorManager editManager) {
      super (main, editManager);
   }

   private boolean containsBundleAndFibres (LinkedList<ModelComponent> selection) {
      boolean bundlePresent = false;
      boolean fibrePresent = false;
      for (ModelComponent c : selection) {
         if (c instanceof MuscleBundle) {
            if (bundlePresent) {
               return false;
            }
            bundlePresent = true;
         }
         else if (c instanceof Muscle) {
            fibrePresent = true;
         }
         else {
            return false;
         }
      }
      return fibrePresent && bundlePresent;
   }

   public FemModel3d getBundleAncestor (MuscleBundle bundle) {
      CompositeComponent gparent = bundle.getGrandParent();
      if (gparent instanceof FemModel3d) {
         return (FemModel3d)gparent;
      }
      else {
         return null;
      }
   } 

   private MuscleBundle getEditableBundle (LinkedList<ModelComponent> selection) {
      if (!containsSingleSelection (selection, MuscleBundle.class)) {
         return null;
      }
      MuscleBundle bundle = (MuscleBundle)selection.get (0);
      if (getBundleAncestor (bundle) != null) {
         return bundle;
      }
      else {
         return null;
      }            
   }

   public void addActions (EditActionMap actions, SelectionManager selManager) {
      LinkedList<ModelComponent> selection = selManager.getCurrentSelection();

      MuscleBundle bundle;
      if ((bundle = getEditableBundle (selection)) != null) {
         actions.add (this, "Edit fibres ...", EXCLUSIVE);
         actions.add (this, "Edit elements ...", EXCLUSIVE);
         if (bundle.getFibres().size() > 0) {
            actions.add (this, "Compute element directions");
            actions.add (this, "Add elements near fibres ...");
            actions.add (this, "Delete elements");
         }
      }
      else if (containsBundleAndFibres (selection)) {
         actions.add (this, "Move fibres to bundle");
      }
   }

   public void applyAction (
      String actionCommand, LinkedList<ModelComponent> selection,
      Rectangle popupBounds) {

      MuscleBundle bundle;      
      if ((bundle = getEditableBundle (selection)) != null) {
         if (actionCommand == "Edit fibres ...") {
            if (myEditManager.acquireEditLock()) {
               MuscleFibreAgent agent =
                  new MuscleFibreAgent (
                     myMain, bundle, getBundleAncestor (bundle));
               agent.setContinuousAdd (true);
               agent.show (popupBounds);
            }
         }
         else if (actionCommand == "Edit elements ...") {
            if (myEditManager.acquireEditLock()) {
               MuscleElementAgent agent =
                  new MuscleElementAgent (
                     myMain, bundle, getBundleAncestor (bundle));
               agent.show (popupBounds);
            }
         }
         else if (actionCommand == "Compute element directions") {
            bundle.computeElementDirections();
         }
         else if (actionCommand == "Add elements near fibres ...") {
            addElementsNearFibres (bundle);
         }
         else if (actionCommand == "Delete elements") {
            deleteElements (bundle);
         }
      }
      else if (containsBundleAndFibres (selection)) {
         LinkedList<Muscle> fibres = new LinkedList<Muscle>();
         bundle = null;
         for (ModelComponent c : selection) {
            if (bundle == null && c instanceof MuscleBundle) {
               bundle = (MuscleBundle)c;
            }
            else if (c instanceof Muscle) {
               fibres.add ((Muscle)c);
            }
            else {
               throw new InternalErrorException (
                  "Unexepected item in selection list: " + c.getClass());
            }
         }
         MoveFibresCommand command =
            new MoveFibresCommand ("move Muscles", bundle, fibres);
         myMain.getUndoManager().saveStateAndExecute (command);
      }
   }

   private void addElementsNearFibres (MuscleBundle bundle) {
      WidgetDialog dialog =
         WidgetDialog.createDialog (
            myMain.getFrame(), "Specify fibre distance", "Set");
      DoubleField widget =
         new DoubleField ("distance:", myLastElementFibreDist);
      widget.setRange (0, Double.POSITIVE_INFINITY);
      dialog.addWidget (widget);
      GuiUtils.locateCenter (dialog, myMain.getFrame());
      dialog.setVisible (true);
      if (dialog.getReturnValue() == OptionPanel.OK_OPTION) {
         double dist = widget.getDoubleValue();
         LinkedList<MuscleElementDesc> list =
            bundle.getNewElementsNearFibres (dist);
         AddComponentsCommand cmd = new AddComponentsCommand (
            "Add near elements", list, bundle.getElements());
         myMain.getUndoManager().saveStateAndExecute (cmd);
         myLastElementFibreDist = dist;
      }
   }

   private void deleteElements (MuscleBundle bundle) {
      LinkedList<ModelComponent> deleteList =
         new LinkedList<ModelComponent>();
      deleteList.addAll (bundle.getElements());
      RemoveComponentsCommand cmd =
         new RemoveComponentsCommand ("remove elements", deleteList);
      myMain.getUndoManager().saveStateAndExecute (cmd);
   }
}
