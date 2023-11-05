/**
 * Copyright (c) 2014, by the Authors: John Lloyd (UBC), Tracy Wilkinson (UBC) and
 * ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.gui.editorManager;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.io.*;

import javax.swing.*;

import artisynth.core.driver.Main;
import artisynth.core.gui.SelectableComponentPanel;
import artisynth.core.gui.selectionManager.SelectionEvent;
import artisynth.core.gui.selectionManager.SelectionFilter;
import artisynth.core.gui.selectionManager.SelectionListener;
import artisynth.core.gui.widgets.NodeNumberFileChooser;
import artisynth.core.util.ExtensionFileFilter;
import artisynth.core.inverse.TrackingController;
import artisynth.core.mechmodels.*;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.*;
import artisynth.core.femmodels.*;
import maspack.properties.EditingProperty;
import maspack.properties.HasProperties;
import maspack.properties.HostList;
import maspack.properties.PropTreeCell;
import maspack.util.InternalErrorException;
import maspack.util.ListView;
import maspack.widgets.*;
import maspack.geometry.*;
import maspack.widgets.DoubleField;
import maspack.widgets.GuiUtils;
import maspack.widgets.PropertyPanel;
import maspack.widgets.ValueChangeEvent;
import maspack.widgets.ValueChangeListener;

public class FemNodeSelectionAgent extends FrameBasedEditingAgent implements
SelectionListener {
   static double DTOR = Math.PI/180;

   private PropertyPanel myPropertyPanel;
   protected EnumSelector myModeSelector;

   public static final SelectionMode DEFAULT_MODE = SelectionMode.INDIVIDUAL;
   static SelectionMode myDefaultMode = DEFAULT_MODE;

   public static final double DEFAULT_MAX_BEND_ANG = 30;
   static double myDefaultMaxBendAng = DEFAULT_MAX_BEND_ANG;

   public static final double DEFAULT_MIN_BEND_ANG = 60;
   static double myDefaultMinBendAng = DEFAULT_MIN_BEND_ANG;

   public static final double DEFAULT_MAX_EDGE_ANG = 90;
   static double myDefaultMaxEdgeAng = DEFAULT_MAX_EDGE_ANG;

   public static final boolean DEFAULT_ALLOW_BRANCHING = false;
   static boolean myDefaultAllowBranching = DEFAULT_ALLOW_BRANCHING;

   public static final double DEFAULT_DISTANCE = 1.0;
   static double myDefaultDistance = DEFAULT_DISTANCE;

   public static final boolean DEFAULT_SURFACE_NODES_ONLY = false;
   static boolean myDefaultSurfaceNodesOnly = DEFAULT_SURFACE_NODES_ONLY;

   public static final boolean DEFAULT_USE_SIGNED_DISTANCE = true;
   static boolean myDefaultUseSignedDistance = DEFAULT_USE_SIGNED_DISTANCE;

   public static final int DEFAULT_MAX_COLS = 10;
   static int myDefaultMaxCols = DEFAULT_MAX_COLS;

   public static final int DEFAULT_WRITE_FLAGS = 0;
   static int myDefaultWriteFlags = DEFAULT_WRITE_FLAGS;

   static File myNumberFile;

   protected boolean myMaskSelectionChanged = false;
   protected boolean myAttachingP = false;
   protected String mySavedInstructions = null;

   protected DoubleField myMaxBendAngField;
   protected DoubleField myMinBendAngField;
   protected DoubleField myMaxEdgeAngField;
   protected BooleanSelector myBranchingSelector;
   protected DoubleField myDistanceField;
   protected BooleanSelector mySurfaceNodesSelector;
   protected BooleanSelector mySignedDistanceSelector;
   protected JButton myClearButton;
   protected JButton mySaveButton;
   protected JButton mySaveAsButton;
   protected JButton myLoadButton;
   protected JButton myAttachButton;
   protected JButton myDetachButton;
   // protected HashSet<MotionTargetComponent> mySelectedTargets;
   FemModel3d myFemModel;
   MechModel myMechModel;

   protected Component[] myCurrentWidgets;
   protected Component[] myPatchWidgets;
   protected Component[] myRidgeWidgets;
   protected Component[] myDistanceWidgets;

   private State myState = State.Complete;

   public enum SelectionMode {
      INDIVIDUAL,
      PATCH,
      PATCH_BORDER,
      EDGE_LINE,
      DISTANCE,
   }

   private enum State {
      SelectingTargets, Complete
   };

   private RigidBody getSelectedBody (ModelComponent selComp) {
      if (selComp instanceof RigidBody) {
         return (RigidBody)selComp;
      }
      else if (selComp instanceof RigidMeshComp) {
         return ((RigidMeshComp)selComp).getRigidBody();
      }
      else {
         return null;
      }
   }

   private FemModel3d getSelectedFem (ModelComponent selComp) {
      if (selComp instanceof FemModel3d) {
         return (FemModel3d)selComp;
      }
      else if (selComp instanceof FemMeshComp) {
         return ((FemMeshComp)selComp).getFem();
      }
      else {
         return null;
      }
   }

   private boolean isValidMeshComponent (ModelComponent selComp) {
      if (selComp instanceof RigidBody) {
         return true;
      }
      else if (selComp instanceof MeshComponent) {
         if (selComp instanceof FemMeshComp) {
            return ((FemMeshComp)selComp).getFem() != myFemModel;
         }
         else {
            return true;
         }
      }
      else {
         return false;
      }
   }

   private class FemNodeFilter implements SelectionFilter {
      public boolean objectIsValid (
         ModelComponent c, java.util.List<ModelComponent> currentSelections) {
         if (myAttachingP) {
            FemModel3d fem = getSelectedFem(c);
            return ((fem != null && fem != myFemModel) ||
                    getSelectedBody(c) != null);
         }
         else {
            switch (getSelectionMode()) {
               case DISTANCE: {
                  return isValidMeshComponent (c);
               }
               default: {
                  return (c instanceof FemNode3d);
               }
            }
         }
      }
   }

   public FemNodeSelectionAgent (Main main, FemModel3d fem) {
      super (main);
      myFemModel = fem;
      myMechModel = MechModel.nearestMechModel (fem);
   }

   public void show (Rectangle popupBounds) {
      super.show (popupBounds);
      mySelectionManager.addSelectionListener (this);
   }

   public void dispose() {
      super.dispose();
      myModeSelector.clearValueChangeListeners();
      myEditManager.releaseEditLock();
      mySelectionManager.removeSelectionListener (this);
      uninstallSelectionFilter();
      mySelectionManager.setViewerMultiSelection (false);
   }

   public SelectionMode getSelectionMode() {
      return (SelectionMode)myModeSelector.getValue();
   }

   private void setNumberFile (File file) {
      myNumberFile = file;
      mySaveButton.setEnabled (file != null);
   }

   protected boolean updatePropertyWidgets() {
      if (myPropertyPanel.numWidgets() == 0) {
         myPropertyPanel.addWidget (myModeSelector);
      }
      Component[] widgets;
      switch (getSelectionMode()) {
         case PATCH_BORDER:
         case PATCH: {
            widgets = myPatchWidgets;
            break;
         }
         case EDGE_LINE: {
            widgets = myRidgeWidgets;
            break;
         }
         case DISTANCE: {
            widgets = myDistanceWidgets;
            break;
         }
         case INDIVIDUAL: {
            widgets = null;
            break;
         }
         default: {
            throw new UnsupportedOperationException (
               "Unimplemented selection mode " + getSelectionMode());
         }
      }
      if (widgets != myCurrentWidgets) {
         while (myPropertyPanel.numWidgets() > 1) {
            myPropertyPanel.removeWidget (1);
         }
         if (widgets != null) {
            for (Component comp : widgets) {
               myPropertyPanel.addWidget (comp);
            }
         }
         myCurrentWidgets = widgets;
         return true;
      }
      else {
         return false;
      }
   }

   protected void handleModeChange() {
      updateInstructions();
      if (updatePropertyWidgets()) {
         myDisplay.pack();
         myDisplay.repaint();
      }
      myDefaultMode = getSelectionMode();
   }

   protected void initializePropertyWidgets() {
      myModeSelector =
         new EnumSelector (
            "selection mode",
            myDefaultMode, SelectionMode.values());
      myModeSelector.addValueChangeListener (e->handleModeChange());

      myMaxBendAngField = new DoubleField ("maxBendAngle", myDefaultMaxBendAng);
      myMaxBendAngField.addValueChangeListener (
         e -> myDefaultMaxBendAng = (Double)e.getValue());

      myPatchWidgets = new Component[1];
      myPatchWidgets[0] = myMaxBendAngField;

      myMinBendAngField =
         new DoubleField ("minBendAngle", myDefaultMinBendAng);
      myMinBendAngField.addValueChangeListener (
         e -> myDefaultMinBendAng = (Double)e.getValue());

      myMaxEdgeAngField = 
         new DoubleField ("maxEdgeAngle", myDefaultMaxEdgeAng);
      myMaxEdgeAngField.addValueChangeListener (
         e -> myDefaultMaxEdgeAng = (Double)e.getValue());

      myBranchingSelector = 
         new BooleanSelector ("allowBranching", myDefaultAllowBranching);
      myBranchingSelector.addValueChangeListener (
         e -> myDefaultAllowBranching = (Boolean)e.getValue());

      myRidgeWidgets = new Component[3];
      myRidgeWidgets[0] = myMinBendAngField;
      myRidgeWidgets[1] = myMaxEdgeAngField;
      myRidgeWidgets[2] = myBranchingSelector;

      myDistanceField = new DoubleField ("distance", myDefaultDistance);
      myDistanceField.addValueChangeListener (
         e -> myDefaultDistance = (Double)e.getValue());

      mySurfaceNodesSelector =
         new BooleanSelector ("surfaceNodesOnly", myDefaultSurfaceNodesOnly);
      mySurfaceNodesSelector.addValueChangeListener (
         e -> myDefaultSurfaceNodesOnly = (Boolean)e.getValue());

      mySignedDistanceSelector =
         new BooleanSelector ("useSignedDistance", myDefaultUseSignedDistance);
      mySignedDistanceSelector.addValueChangeListener (
         e -> myDefaultUseSignedDistance = (Boolean)e.getValue());

      myDistanceWidgets = new Component[3];
      myDistanceWidgets[0] = myDistanceField;
      myDistanceWidgets[1] = mySurfaceNodesSelector;
      myDistanceWidgets[2] = mySignedDistanceSelector;

      // XXX Hack: add and remove widget with widest label to preset the
      // overall label width
      myContentPane.addWidget (mySignedDistanceSelector);
      myContentPane.removeWidget (mySignedDistanceSelector);

      updatePropertyWidgets();
   }

   private JPanel createButtonPair (Component c0, Component c1) {
      JPanel panel =  new JPanel();
      panel.setLayout (new BoxLayout (panel, BoxLayout.LINE_AXIS));
      panel.add (c0);
      panel.add (Box.createHorizontalGlue());
      panel.add (Box.createRigidArea (new Dimension(4, 0)));
      panel.add (Box.createHorizontalGlue());
      panel.add (c1);
      return panel;
   }

   protected void createButtons() {
      JPanel panel = new JPanel();
      panel.setLayout (new BoxLayout (panel, BoxLayout.PAGE_AXIS));

      //panel.add (Box.createVerticalGlue());
      ArrayList<JButton> buttons = new ArrayList<>();

      myClearButton = createVerticalButton (
         "Clear", "Clear selection");
      buttons.add (myClearButton);

      mySaveButton = createVerticalButton (
         "Save", "Save node numbers to a file");
      mySaveButton.setEnabled (myNumberFile !=null);
      buttons.add (mySaveButton);

      mySaveAsButton = createVerticalButton (
         "Save as ...", "Save node numbers to a selected file");
      buttons.add (mySaveAsButton);

      myLoadButton = createVerticalButton (
         "Load ...", "Load node numbers from a selected file");
      buttons.add (myLoadButton);

      if (myMechModel != null) {
         myAttachButton = createVerticalButton (
            "Attach nodes", "Attach nodes to a selected body");
         buttons.add (myAttachButton);
         myAttachButton.setEnabled (false);

         myDetachButton = createVerticalButton (
            "Detach nodes", "Detach nodes from whatever they are attached to");
         buttons.add (myDetachButton);
         myDetachButton.setEnabled (false);
      }

      // resize buttons
      Dimension maxSize = new Dimension(myAttachButton.getPreferredSize());
      for (JButton b : buttons) {
         double width = b.getPreferredSize().getWidth();
         if (width > maxSize.getWidth()) {
            maxSize.setSize (width, maxSize.getHeight());
         }
      }
      for (JButton b : buttons) {
         b.setMaximumSize (maxSize);
         b.setMinimumSize (maxSize);
         b.setPreferredSize (maxSize);
      }

      Component attachComp = myAttachButton;
      Component detachComp = myDetachButton;
      if (attachComp == null) {
         attachComp = Box.createRigidArea (maxSize);
         detachComp = Box.createRigidArea (maxSize);
      }
      panel.add (Box.createRigidArea (new Dimension(0, 2)));
      panel.add (createButtonPair (myClearButton, mySaveButton));
      panel.add (Box.createRigidArea (new Dimension(0, 2)));

      panel.add (createButtonPair (attachComp, mySaveAsButton));
      panel.add (Box.createRigidArea (new Dimension(0, 2)));

      panel.add (createButtonPair (detachComp, myLoadButton));
      panel.add (Box.createRigidArea (new Dimension(0, 2)));

      myContentPane.addWidget (panel);
   }

   private JButton createVerticalButton (
      String cmd, String toolTip) {
      JButton button = new JButton (cmd);
      button.setActionCommand (cmd);
      if (toolTip != null) {
         button.setToolTipText (toolTip);
      }
      button.addActionListener (this);
      button.setAlignmentX (Component.LEFT_ALIGNMENT);
      Dimension size = button.getPreferredSize();
      button.setHorizontalAlignment (SwingConstants.LEFT);
      //button.setMargin (new Insets (5, 10, 5, 10));
      return button;
   }

   private void updateInstructions() {
      String text;
      switch (getSelectionMode()) {
         case INDIVIDUAL: {
            text = "Selected desired nodes";
            break;
         }
         case PATCH:
         case PATCH_BORDER: {
            text = "Select node inside the patch";
            break;
         }
         case EDGE_LINE: {
            text = "Select node on the edge";
            break;
         }
         case DISTANCE: {
            text = "Select mesh or body";
            break;
         }
         default: {
            throw new UnsupportedOperationException (
               "Unimplemented selection mode " + getSelectionMode());
         }
      }
      setInstructions (text);
   }

   protected void createDisplay() {
      createDisplayFrame ("Select FEM nodes");
      myPropertyPanel = new PropertyPanel();
      initializePropertyWidgets();
      addWidget (myPropertyPanel);
      addWidget (new JSeparator());
      createInstructionBox();
      updateInstructions();
      addWidget (new JSeparator());
      createButtons();      
      addWidget (new JSeparator());
      createOptionPanel ("Done");
      installSelectionFilter (new FemNodeFilter());
      mySelectionManager.filterSelections (new FemNodeFilter());
      mySelectionManager.setViewerMultiSelection (true);
   }

   public void selectionChanged (SelectionEvent e) {
      if (myMaskSelectionChanged) {
         return;
      }
      ModelComponent selComp = e.getLastAddedComponent();
      if (myAttachingP) {
         doAttachNodes (selComp);
         return;
      }
      ArrayList<FemNode3d> nodes = null;
      SelectionMode mode = (SelectionMode)myModeSelector.getValue();
      if (mode == SelectionMode.DISTANCE) {
         PolygonalMesh pmesh = null;
         if (selComp instanceof RigidBody) {         
            pmesh = ((RigidBody)selComp).getSurfaceMesh();
         }
         else if (selComp instanceof MeshComponent) {
            MeshBase mesh = ((MeshComponent)selComp).getMesh();
            if (mesh instanceof PolygonalMesh) {
               pmesh = (PolygonalMesh)mesh;
            }
         }
         if (pmesh != null) {
            boolean surfaceOnly = mySurfaceNodesSelector.getBooleanValue();
            nodes = new ArrayList<>();
            double distThresh = myDistanceField.getDoubleValue();
            boolean useSignedDist = mySignedDistanceSelector.getBooleanValue();
            for (FemNode3d n : myFemModel.getNodes()) {
               if (surfaceOnly && !myFemModel.isSurfaceNode (n)) {
                  continue;
               }
               double d = pmesh.distanceToPoint (n.getPosition());
               if (useSignedDist && pmesh.isClosed()) {
                  if (pmesh.pointIsInside (n.getPosition()) == 1) {
                     d = -d;
                  }
               }
               if (d <= distThresh) {
                  nodes.add (n);
               }
            }
         }
         removeSelected (selComp); // don't want the body or mesh selected
      }
      else {
         if (selComp instanceof FemNode3d) {
            FemNode3d node = (FemNode3d)selComp;
            switch (mode) {
               case INDIVIDUAL: {
                  break;
               }
               case EDGE_LINE: {
                  double minBendAng = DTOR*myMinBendAngField.getDoubleValue();
                  double maxEdgeAng = DTOR*myMaxEdgeAngField.getDoubleValue();
                  boolean branching = myBranchingSelector.getBooleanValue();
                  nodes = myFemModel.findEdgeLineNodes (
                     node, minBendAng, maxEdgeAng, branching);
                  break;
               }
               case PATCH: {
                  double maxBendAng = DTOR*myMaxBendAngField.getDoubleValue();
                  nodes = myFemModel.findPatchNodes (node, maxBendAng);
                  break;
               }
               case PATCH_BORDER: {
                  double maxBendAng = DTOR*myMaxBendAngField.getDoubleValue();
                  nodes = myFemModel.findPatchBorderNodes (node, maxBendAng);
                  removeSelected (node); // node may not be on the border
                  break;
               }
               default: {
                  throw new UnsupportedOperationException (
                     "Unimplemented selection mode " + mode);
               }
            }
         }
      }
      if (nodes != null) {
         addSelected (nodes);
         myMain.rerender();
      }
      updateAttachButtons();
   }

   private void addSelected (List<FemNode3d> nodes) {
      myMaskSelectionChanged = true;
      mySelectionManager.addSelected (nodes);
      myMaskSelectionChanged = false;
   }

   private void removeSelected (ModelComponent c) {
      myMaskSelectionChanged = true;
      mySelectionManager.removeSelected (c);
      myMaskSelectionChanged = false;
   }

   private void clearSelected () {
      myMaskSelectionChanged = true;
      mySelectionManager.clearSelections();
      myMaskSelectionChanged = false;
      myAttachButton.setEnabled (false);      
      myDetachButton.setEnabled (false);      
   }

   private void updateAttachButtons() {
      boolean hasUnattached = false;
      boolean hasAttached = false;
      for (ModelComponent c : mySelectionManager.getCurrentSelection()) {
         if (c instanceof FemNode3d) {
            FemNode3d node = (FemNode3d)c;
            if (node.getAttachment() == null) {
               hasUnattached = true;
            }
            else if (node.getAttachment() instanceof ModelComponent) {
               hasAttached = true;
            }
            if (hasAttached && hasUnattached) {
               break;
            }
         }
      }
      myAttachButton.setEnabled (hasUnattached);
      myDetachButton.setEnabled (hasAttached);
   }

   public void objectDeselected (SelectionEvent e) {
   }

   private boolean saveNodes (File file) {
      ArrayList<FemNode3d> nodes = new ArrayList<>();
      for (ModelComponent c : mySelectionManager.getCurrentSelection()) {
         if (c instanceof FemNode3d) {
            // Paranoid
            nodes.add ((FemNode3d)c);
         }
      }
      try {
         NodeNumberWriter.write (
            file, nodes, myDefaultMaxCols, myDefaultWriteFlags);
         setNumberFile (file);
         return true;
      }
      catch (Exception e) {
         GuiUtils.showError (myDisplay, "Error writing file "+file, e);
         return false;
      }
   }

   private void loadNodes (File file) {
      ArrayList<FemNode3d> nodes = null;
      NodeNumberReader reader = null;
      try {
         reader = new NodeNumberReader(file);
         nodes = reader.read (myFemModel);
         if (reader.nodesWereBracketed()) {
            myDefaultWriteFlags |= NodeNumberWriter.USE_BRACKETS;
         }
         else {
            myDefaultWriteFlags &= (~NodeNumberWriter.USE_BRACKETS);
         }
         setNumberFile (file);
         clearSelected();
         addSelected (nodes);
         myMain.rerender();  
         updateAttachButtons();
      }
      catch (Exception e) {
         GuiUtils.showError (myDisplay, "Error writing file "+file, e);
      }
      finally {
         if (reader != null) {
            reader.close();
         }
      }
   }

   private void startAttachNodes() {
      myAttachingP = true;
      mySavedInstructions = getInstructions();
      setInstructions ("Select body or associated mesh");
      myAttachButton.setText ("Cancel attach");
      myAttachButton.setActionCommand ("Cancel attach");
   }

   private void endAttachNodes() {
      setInstructions (mySavedInstructions);
      myAttachButton.setText ("Attach nodes");
      myAttachButton.setActionCommand ("Attach nodes");
      myAttachingP = false;
   }

   private void doAttachNodes (ModelComponent selComp) {
      PointAttachable body = getSelectedFem (selComp);
      if (body == null) {
         body = getSelectedBody (selComp);
      }
      if (body != null) {
         LinkedList<PointAttachment> attachments = new LinkedList<>();
         for (ModelComponent c : mySelectionManager.getCurrentSelection()) {
            if (c instanceof FemNode3d) {
               FemNode3d node = (FemNode3d)c;
               if (node.getAttachment() == null) {
                  attachments.add (body.createPointAttachment (node));
               }
            }
         }
         if (attachments.size() > 0) {
            AddComponentsCommand cmd = 
               new AddComponentsCommand (
                  "attach nodes", attachments,
                  (MutableCompositeComponent<?>)myMechModel.attachments());
            myUndoManager.saveStateAndExecute (cmd);
         }
      }
      removeSelected (selComp);
      endAttachNodes();
      updateAttachButtons();
   }

   private void detachNodes () {
      LinkedList<ModelComponent> attachments = new LinkedList<>();
      for (ModelComponent c : mySelectionManager.getCurrentSelection()) {
         if (c instanceof FemNode3d) {
            FemNode3d node = (FemNode3d)c;
            if (node.getAttachment() instanceof ModelComponent) {
               attachments.add ((ModelComponent)node.getAttachment());
            }
         }
      }
      if (attachments.size() > 0) {
         RemoveComponentsCommand cmd = 
            new RemoveComponentsCommand ("detach nodes", attachments);
         myUndoManager.saveStateAndExecute (cmd);
      }
      updateAttachButtons();
   }

   private NodeNumberFileChooser createNumberFileChooser (boolean save) {
      NodeNumberFileChooser chooser;
      if (save) {
         chooser = new NodeNumberFileChooser (
            myNumberFile, myDefaultMaxCols, myDefaultWriteFlags);
      }
      else {
         chooser = new NodeNumberFileChooser (myNumberFile);
      }
      if (myNumberFile == null) {
         System.out.println (
            "setting dir: " + myMain.getModelDirectory());
         chooser.setCurrentDirectory (myMain.getModelDirectory());
         System.out.println (
            "getting dir: " + chooser.getCurrentDirectory());
      }
      return chooser;
   }

   private void loadNodes() {
      NodeNumberFileChooser chooser = createNumberFileChooser (/*save=*/false);
      int retVal = chooser.showDialog (myDisplay, "Load");
      if (retVal == JFileChooser.APPROVE_OPTION) {
         loadNodes (chooser.getSelectedFileWithExtension());
      }
   }

   private void saveNodesAs() {
      NodeNumberFileChooser chooser = createNumberFileChooser (/*save=*/true);
      if (myNumberFile == null) {
         chooser.setCurrentDirectory (myMain.getModelDirectory());
      }
      int retVal = chooser.showDialog (myDisplay, "Save as");
      myDefaultMaxCols = chooser.getMaxColumns();
      myDefaultWriteFlags = chooser.getFlags();
      if (retVal == JFileChooser.APPROVE_OPTION) {
         saveNodes (chooser.getSelectedFileWithExtension());
      }
   }

   public void actionPerformed (ActionEvent e) {
      String cmd = e.getActionCommand();
      if (cmd.equals ("Clear")) {
         clearSelected();
         myMain.rerender();
      }
      else if (cmd.equals ("Save")) {
         if (myNumberFile != null) {
            saveNodes (myNumberFile);
         }
      }
      else if (cmd.equals ("Save as ...")) {
         saveNodesAs();
      }
      else if (cmd.equals ("Load ...")) {
         loadNodes();
      }
      else if (cmd.equals ("Attach nodes")) {
         startAttachNodes();
      }
      else if (cmd.equals ("Detach nodes")) {
         detachNodes();
      }
      else if (cmd.equals ("Cancel attach")) {
         endAttachNodes();
      }
      else {
         super.actionPerformed (e);
      }
   }
}
