/**
 * Copyright (c) 2014, by the Authors: Ian Stavness (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.inverse;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import maspack.interpolation.NumericList;
import maspack.interpolation.NumericListKnot;
import maspack.properties.Property;
import maspack.properties.PropertyInfo;
import artisynth.core.driver.Main;
import artisynth.core.gui.ControlPanel;
import artisynth.core.mechmodels.Frame;
import artisynth.core.mechmodels.MotionTargetComponent;
import artisynth.core.mechmodels.Point;
import artisynth.core.modelbase.Controller;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.probes.NumericInputProbe;
import artisynth.core.probes.NumericOutputProbe;
import artisynth.core.probes.NumericProbeBase;
import artisynth.core.probes.Probe;
import artisynth.core.probes.WayPoint;
import artisynth.core.workspace.RootModel;

public class InverseManager {

   private Main myMain;
   public ControlPanel inverseControlPanel = null;

   private NumericInputProbe refTargetPosInProbe;
   private NumericInputProbe excitationInput = null;
   private NumericOutputProbe excitationOutProbe;
   private NumericOutputProbe modelTargetPosOutProbe;
   private NumericOutputProbe refTargetPosOutProbe;

   public InverseManager (Main main) {
      myMain = main;
   }

//   public void setController(TrackingController controller, RootModel root) {
//      if (controller == null || root == null) {
//         return;
//      }
//      myController = controller;
//      myRoot = root;
//      if (myController.isManaged()) {
//         findOrCreateProbes();
//         configureProbes();
//      }
//      if (myMain.getMainFrame() != null) {
//         showInversePanel();
//      }
//   }


   public void showInversePanel(RootModel root, TrackingController controller) {
      hideInversePanel();
      createInverseControlPanel(root, controller);
      if (!RootModel.isFocusable()) {
         inverseControlPanel.setFocusableWindowState(false);
      }
       inverseControlPanel.setVisible(true);
   }

   public void hideInversePanel() {
      if (inverseControlPanel != null) {
         myMain.deregisterWindow(inverseControlPanel.getFrame());
         inverseControlPanel.dispose();
         inverseControlPanel = null;
      }
   }

   private void createInverseControlPanel(RootModel root, TrackingController controller) {
      inverseControlPanel = new InverseControlPanel (root, controller);

      Dimension d = myMain.getMainFrame().getSize();
      java.awt.Point pos = myMain.getMainFrame().getLocation();
      inverseControlPanel.getFrame().setLocation(pos.x+d.width, pos.y+d.height);

      myMain.registerWindow(inverseControlPanel.getFrame());
   }

   private void findOrCreateProbes(RootModel root, TrackingController controller) {
      // root.clearInputProbes();
      // root.clearOutputProbes();

      refTargetPosInProbe = findOrCreateInputProbe(root, "target positions");
      refTargetPosOutProbe = findOrCreateOutputProbe(root, "target positions");
      modelTargetPosOutProbe = findOrCreateOutputProbe(root, "model positions");
      excitationOutProbe =
         findOrCreateOutputProbe(root, "computed excitations");
      excitationInput = findOrCreateInputProbe(root, "input excitations");

      setLoneBreakpoint(root, controller.getProbeDuration());
      setProbeDuration(controller.getProbeDuration());
      setProbeUpdateInterval(controller.getProbeUpdateInterval());
      
      if (myMain.getTimeline() != null && myMain.getRootModel() != null) {
         myMain.getTimeline().requestResetAll();
      }
   }

   public static NumericOutputProbe findOrCreateOutputProbe(RootModel root,
      String name) {
      NumericOutputProbe outProbe;
      Probe p = root.getOutputProbes().get(name);
      if (p != null && p instanceof NumericOutputProbe) {
         outProbe = (NumericOutputProbe)p;
      }
      else {
         outProbe = new NumericOutputProbe();
         outProbe.setName(name);
         root.addOutputProbe(outProbe);
      }
      return outProbe;
   }

   public static NumericInputProbe
      findOrCreateInputProbe(RootModel root, String name) {
      NumericInputProbe inProbe;
      Probe p = root.getInputProbes().get(name);
      if (p != null && p instanceof NumericInputProbe) {
         inProbe = (NumericInputProbe)p;
      }
      else {
         inProbe = new NumericInputProbe();
         inProbe.setName(name);
         root.addInputProbe(inProbe);
      }
      return inProbe;
   }

   private void configureProbes(TrackingController controller) {
      configureExcitationProbe(controller);
   
      MotionTargetTerm moterm = null;
      for (QPTerm term : controller.getCostTerms()) {
         if (term instanceof MotionTargetTerm) {
            moterm = (MotionTargetTerm)term;
            break;
         }
      }
      for (LeastSquaresTerm term : controller.getConstraintTerms()) {
         if (term instanceof MotionTargetTerm) {
            moterm = (MotionTargetTerm)term;
            break;
         }
      }
      if (moterm != null) {
         configureTargetProbes(
            refTargetPosInProbe, moterm.getTargets(),
            "ref_targetPos_input.txt");
         configureTargetProbes(
            modelTargetPosOutProbe, moterm.getSources(),
            "model_target_position.txt");
         configureTargetProbes(
            refTargetPosOutProbe, moterm.getTargets(),
            "ref_target_position.txt");
      }
      
   }

   private void configureExcitationProbe(TrackingController myController) {
      Property[] props = new Property[myController.getExciters().size()];
      for (int i = 0; i < myController.getExciters().size(); i++) {
         // XXX how to handle nested excitations?
         props[i] = myController.getExciters().get(i).getProperty("excitation"); 
      }
      excitationOutProbe.setModel(myController.getMech());
      excitationOutProbe.setOutputProperties(props);
      excitationOutProbe.setAttachedFileName("excitations.txt");

      if (excitationInput != null) {
         excitationInput.setModel(myController.getMech());
         excitationInput.setInputProperties(props);
         excitationInput.setAttachedFileName("excitations_input.txt");
         excitationInput.setActive(false);
      }
   }

   private void configureTargetProbes(NumericProbeBase probe,
      ArrayList<MotionTargetComponent> targets, String filename) {
      ArrayList<Property> props = new ArrayList<Property>();
      for (ModelComponent target : targets) {
         if (target instanceof Point) {
            props.add(target.getProperty("position"));
         }
         else if (target instanceof Frame) {
            props.add(target.getProperty("position"));
            props.add(target.getProperty("orientation"));
         }
         else {
            System.err.println("Unknown target component type: "
               + target.getClass().toString());
         }
      }
//      probe.setModel(myController.getMech());
      probe.setAttachedFileName(filename);

      if (probe instanceof NumericInputProbe) {
         ((NumericInputProbe)probe).setInputProperties(props
            .toArray(new Property[props.size()]));
      }
      else if (probe instanceof NumericOutputProbe) {
         ((NumericOutputProbe)probe).setOutputProperties(props
            .toArray(new Property[props.size()]));
      }

      if (probe instanceof NumericInputProbe) {
         File file = probe.getAttachedFile ();
         if (file == null || !file.exists ()) {
            ((NumericInputProbe)probe).loadEmpty ();
            probe.setActive (false);
         }
         else {
            try {
               probe.load ();
               probe.setActive (true);
            }
            catch (IOException e) {
               e.printStackTrace ();
            }
         }
      }
   }

//   public class SyncTargetListener implements ActionListener {
//
//      @Override
//      public void actionPerformed (ActionEvent e) {
//         // TODO Auto-generated method stub
//         
//      }
//      
//   }
   
   private void syncTargetProbes() {
         syncProbes(refTargetPosInProbe, modelTargetPosOutProbe);
   }

   private void syncExcitationProbes() {
         syncProbes(excitationInput, excitationOutProbe);
   }

   private void syncProbes(NumericProbeBase dest, NumericProbeBase source)
   {
      if (dest == null || source == null)
         return;
      syncData(dest.getNumericList(), source.getNumericList());
   }

   private void syncData(NumericList dest, NumericList source)
   {
      if (dest.getVectorSize() != source.getVectorSize())
      {
         System.err.println("syncData - probe data not same length\n source = "
            +
            source.getVectorSize() + ", dest = " + dest.getVectorSize());
         return;
      }
      dest.clear();
      for (Iterator<NumericListKnot> iter = source.iterator(); iter.hasNext();)
      {
         dest.add(new NumericListKnot(iter.next()));
      }
   }


   public static void setAllProbeDuration(RootModel root, double t) {
      for (Probe p : root.getInputProbes ()) {
         p.setStopTime (t);
      }
      for (Probe p : root.getOutputProbes ()) {
         p.setStopTime (t);       
      }
   }
   
   private void setProbeDuration(double t) {
      refTargetPosInProbe.setStopTime (t);
      excitationInput.setStopTime (t);
      excitationOutProbe.setStopTime (t);
      modelTargetPosOutProbe.setStopTime (t);
      refTargetPosOutProbe.setStopTime (t);
   }
   
   
   public static void setAllProbeUpdateInterval(RootModel root, double h) {
      for (Probe p : root.getInputProbes ()) {
         p.setUpdateInterval (h);
      }
      for (Probe p : root.getOutputProbes ()) {
         p.setUpdateInterval (h);         
      }
   }
   
   private void setProbeUpdateInterval(double h) {
      refTargetPosInProbe.setUpdateInterval (h);
      excitationInput.setUpdateInterval (h);
      excitationOutProbe.setUpdateInterval (h);
      modelTargetPosOutProbe.setUpdateInterval (h);
      refTargetPosOutProbe.setUpdateInterval (h);
   }
   
   public static void replaceBreakpoint(RootModel root, double oldt, double newt) {
      WayPoint waypoint = root.getWayPoint(oldt);
      if (waypoint != null && waypoint.isBreakPoint()) {
         root.removeWayPoint(waypoint);
      }
      root.addBreakPoint (newt);
   }
   
   public static void setLoneBreakpoint(RootModel root, double t) {
      for (WayPoint wp : root.getWayPoints ().getPoints ()) {
         if (wp.isBreakPoint ())
            wp.setBreakPoint (false);
      }
      root.addBreakPoint (t);
   }
   
   public void resetProbes(RootModel root, TrackingController controller) {
      if (root != null) {
         findOrCreateProbes (root, controller);
         configureProbes (controller);
      }
   }

   public static boolean inverseControllerExists() {
      return (findInverseController() != null);
   }

   public static TrackingController findInverseController() {
      TrackingController ic = null;
      RootModel root = Main.getMain().getRootModel();
      for (Controller c : root.getControllers()) {
         if (c instanceof TrackingController) {
            ic = (TrackingController)c;
            break;
         }
      }
      return ic;
   }

   public static boolean isInversePanelVisible() {
      Main main = Main.getMain();
      ControlPanel inversePanel = main.getInverseManager().inverseControlPanel;
      return (inversePanel != null);
   }
   
   
   public class InverseControlPanel extends ControlPanel {
      TrackingController myController;
      RootModel myRoot;

      public InverseControlPanel (RootModel root, TrackingController controller) {
         super("Inverse Control Panel");
         addButtons();
         addDefaultWidgets(controller);
         myController = controller;
         myRoot = root;
      }
      
      private void addButtons() {
         JPanel buttons = new JPanel ();
         
         JButton rp = new JButton ("reset probes");
         rp.addActionListener (new ActionListener() {
            @Override
            public void actionPerformed (ActionEvent e) {
               resetProbes (myRoot, myController);
            }
         });
         buttons.add (rp);
         
         JButton se = new JButton ("sync excitations");
         se.addActionListener (new ActionListener() {
            @Override
            public void actionPerformed (ActionEvent e) {
               syncExcitationProbes ();
            }
         });
         buttons.add (se);

         JButton st = new JButton ("sync targets");
         st.addActionListener (new ActionListener() {
            @Override
            public void actionPerformed (ActionEvent e) {
               syncTargetProbes ();
            }
         });
         buttons.add (st);
         

         
         addWidget (buttons);
         addWidget (new JSeparator ());
      }

      private void addDefaultWidgets(TrackingController tc) {
         
         for (PropertyInfo propinfo : tc.getAllPropertyInfo())
            addWidget(tc, propinfo.getName());

         for (QPTerm term : tc.getCostTerms()) {
            addWidget(new JSeparator());
            addWidget(new JLabel(term.getClass().getSimpleName()));
            for (PropertyInfo propinfo : term.getAllPropertyInfo ()) {
               addWidget(term,propinfo.getName ());
            }
         }
         
         for (LeastSquaresTerm term : tc.getConstraintTerms()) {
            if (term instanceof LeastSquaresTermBase) {
               addWidget(new JSeparator());
               addWidget(new JLabel(term
                  .getClass().getSimpleName()));
               for (PropertyInfo propinfo : ((LeastSquaresTermBase)term)
                  .getAllPropertyInfo())
                  addWidget(
                     (LeastSquaresTermBase)term, propinfo.getName());
            }
         }
         setScrollable(false);
      }
      
      

   }

}
