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

import maspack.interpolation.Interpolation;
import maspack.interpolation.NumericList;
import maspack.interpolation.NumericListKnot;
import maspack.properties.Property;
import maspack.properties.PropertyInfo;
import artisynth.core.gui.ControlPanel;
import artisynth.core.mechmodels.Frame;
import artisynth.core.mechmodels.MotionTargetComponent;
import artisynth.core.mechmodels.Point;
import artisynth.core.modelbase.Controller;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.ComponentList;
import artisynth.core.probes.NumericInputProbe;
import artisynth.core.probes.NumericOutputProbe;
import artisynth.core.probes.NumericProbeBase;
import artisynth.core.probes.Probe;
import artisynth.core.probes.WayPoint;
import artisynth.core.workspace.RootModel;

/**
 * Helper class for creating probes and a control panel for use with a
 * TrackingController. The two primary methods are
 * <pre>
 *  addProbes (rootModel, controller, duration, interval)
 *
 *  addInversePanel (rootModel, controller)
 * </pre>
 * which create the probes or panel for the indicated controller
 * and add them to the specified root model. The probes
 * start at time 0 and extend for {@code duration} seconds,
 * while {@code interval} specifies the update interval for the
 * output probes. Specifying {@code interval} as {@code -1} will cause
 * the output probes to update at the current simulation sample rate.
 * An additional method,
 * <pre>
 *  resetProbes (rootModel, controller, duration, interval)
 * </pre>
 * behaves identically to {@link #addProbes}, but also clears
 * all existing probes first.
 *
 * <p>{@link #addProbes} adds up to three input and three output probes to the
 * timeline, each identified by an enumerated type {@link ProbeID}. The
 * input probes are:
 * <pre>
 * TARGET_POSITIONS     # desired trajectory for all motion targets
 * TARGET_FORCES        # desired trajectory for all constraint force targets
 * INPUT_EXCITATIONS    # input excitations to used for forward simulation
 * </pre>
 *
 * {@code TARGET_FORCES} is added only if a force target term has been added to
 * the controller. Applications can use the {@code TARGET_POSITIONS} and {@code
 * TARGET_FORCES} probes to supply reference trajectories for the motion and
 * constraint force targets; each supplies a vector controlling all the degrees
 * of freedom for the motion and force targets. The {@code INPUT_EXCITATIONS}
 * probe is disabled by default, but can be used to supply excitations to run a
 * forward simulation to generate a reference trajectory that can be later
 * tracked by the controller, as described below. Once created, the inout
 * probes may be located within the root model using the method {@link
 * #findInputProbe}, and data for them may be set using {@link
 * #setInputProbeData}.
 *
 * <p>The output probes are:
 * <pre>
 * TRACKED_POSITIONS    # current motion targets trajectory
 * SOURCE_POSITIONS     # actual trajectory for all motion sources
 * COMPUTED_EXCITATIONS # excitations computed by the inverse solver
 * </pre>
 *
 * The {@code TRACKED_POSITIONS} and {@code COMPUTED_EXCITATIONS} record the
 * reference target positions and computed excitations as the inverse
 * simulation proceeds, while {@code SOURCE_POSITIONS} records the actual
 * resulting motion trajectory. Once created, these output probes may be
 * located within the root model using the method {@link #findOutputProbe}.
 *
 * <p>Each probe is assigned a name and an attached file name based on
 * converted its {@link ProbeID} name to lower case. For example, {@code
 * TARGET_POSITIONS} is given the name {@code "target positions"} and attached
 * file name {@code "target_positions.txt"}. A probe's attached file name
 * can be changed using {@link #setProbeFileName}.
 *
 * <p>To generate a reference trajectory using a forward simulation, one should
 * disable the tracking controller (by setting its {@code enabled} property to
 * {@code false}), and then enable the {@code INPUT_EXCITATIONS} probe and
 * use it to supply excitations to drive the underlying model. The resulting
 * motion trajectory is stored in the {@code SOURCE_POSITIONS} probe.  The
 * contents of the {@code SOURCE_POSITIONS} probe can then be transferred to
 * the {@code TARGET_POSITIONS} input probe, using the method {@link
 * #syncTargetProbes}, to be used as a control input for inverse simulation
 * when the controller is reenabled. The ability of the controller to then
 * follow the prescribed trajectory can then be used as a test of its
 * capability. Likewise, the contents of the {@code COMPUTED_EXCITATIONS}
 * probe can be transferred to the {@code INPUT_EXCITATIONS} probe using
 * {@link #syncExcitationProbes}.
 *
 * <p>The control panel created by {@link #addInversePanel} allows a user to
 * interactively adjust properties of the controller and its subcomponents, and
 * provides three buttons ({@code reset probes}, {@code sync excitations},
 * and {@code syncTargets}) that call the methods {@link #resetProbes},
 * {@link #syncExcitationProbes}, and {@link #syncTargetProbes}.
 * 
 * @author Ian Stavness, John E Lloyd
 */
public class InverseManager {

   // set to true to use older legacy names for probes and their files
   public static boolean useLegacyNames = false;

   /**
    * Identifiers for the probes created by this InverseManager
    */
   public enum ProbeID {
      TARGET_POSITIONS,
      TARGET_FORCES,
      INPUT_EXCITATIONS,
      
      TRACKED_POSITIONS,
      SOURCE_POSITIONS,
      COMPUTED_EXCITATIONS;

      public String getName() {
         if (useLegacyNames) {
            switch (this) {
               case TARGET_POSITIONS: 
                  return "target positions";
               case TARGET_FORCES:
                  return "target forces";
               case INPUT_EXCITATIONS:
                  return "input excitations";
               case TRACKED_POSITIONS:
                  return "target positions";
               case SOURCE_POSITIONS:
                  return "model positions";
               case COMPUTED_EXCITATIONS: {
                  return "computed excitations";
               }
               default: {
                  throw new UnsupportedOperationException (
                     "no legacy name implemented for ProbeId " + this);
               }
            }
         }
         else {
            return toString().toLowerCase().replace ('_', ' ');
         }
      }

      public String getFileName() {
         if (useLegacyNames) {
            switch (this) {
               case TARGET_POSITIONS: 
                  return "ref_targetPos_input.txt";
               case TARGET_FORCES:
                  return "ref_targetForce_input.txt";
               case INPUT_EXCITATIONS:
                  return "excitations_input.txt";
               case TRACKED_POSITIONS:
                  return "ref_target_position.txt";
               case SOURCE_POSITIONS:
                  return "model_target_position.txt";
               case COMPUTED_EXCITATIONS: {
                  return "excitations.txt";
               }
               default: {
                  throw new UnsupportedOperationException (
                     "no legacy file name implemented for ProbeId " + this);
               }
            }
         }
         else {
            return toString().toLowerCase() + ".txt";
         }
      }
   }

   /**
    * Searches for an inverse controller within a root model. If it is
    * not found, returns {@code null}.
    * 
    * @param root root model in which to search for the controller
    * @return located inverse controller, or {@code null}
    */
   public static TrackingController findInverseController (RootModel root) {
      for (Controller c : root.getControllers()) {
         if (c instanceof TrackingController) {
            return (TrackingController)c;
         }
      }
      return null;
   }

   /**
    * Creates a control panel for the specified inverse tracking controller.
    *
    * @param controller controller to create the panel for 
    * @return created control panel
    */
   public static ControlPanel createInversePanel (
      TrackingController controller) {
      return new InverseControlPanel (controller);
   }

   /**
    * Creates a control panel for the specified inverse tracking controller,
    * and then adds it to a root model and positions it to the side of the main
    * frame.
    *
    * @param controller controller to create the panel for 
    * @param root root model to add the panel to
    * @return created control panel
    */
   public static ControlPanel addInversePanel (
      RootModel root,TrackingController controller) {
      ControlPanel panel = null;
      if (root.getMainFrame() != null) {
         panel = createInversePanel (controller);
         root.addControlPanel (panel);
         Dimension d = root.getMainFrame().getSize();
         java.awt.Point pos = root.getMainFrame().getLocation();
         panel.getFrame().setLocation(pos.x+d.width, pos.y+d.height);
      }
      return panel;
   }

   /**
    * Searches for an inverse control panel within a root model. If it is
    * not found, returns {@code null}.
    * 
    * @param root root model in which to search for the panel
    * @return located inverse panel, or {@code null}
    */
   public static ControlPanel findInversePanel (RootModel root) {
      for (ControlPanel panel : root.getControlPanels()) {
         if (panel instanceof InverseControlPanel) {
            return (InverseControlPanel)panel;
         }
      }
      return null;
   }

   /**
    * Adds a set of input and output probes to a root model, for use in
    * controlling inverse simulation. A description of the created probes is
    * given in the documentation header for this class.  The probes start at
    * time 0 and stop at the time indicated by {@code duration}. The output
    * probes are updated at the specified {@code interval}; if specified as
    * {@code -1}, then the output probes are updated at the current simulation
    * sample rate.
    * 
    * @param root root model to which the probes should be added
    * @param controller inverse controller for which the probes should be
    * created
    * @param duration time duration for each probe 
    * @param interval update interval for the output probes
    */
   public static void addProbes (
      RootModel root, TrackingController controller, 
      double duration, double interval) {

      ForceTargetTerm foterm = findForceTargetTerm (controller);      

      if (foterm != null) {
         ProbeID pid = ProbeID.TARGET_FORCES;
         NumericInputProbe refTargetForceInProbe =
            findOrCreateInputProbe (root, pid, duration, interval);
         configureTargetForceProbe (
            refTargetForceInProbe, foterm.getForceTargets(), pid);
      }
      
      MotionTargetTerm moterm = findMotionTargetTerm (controller);      
      if (moterm != null) {
         ProbeID pid = ProbeID.TARGET_POSITIONS;
         NumericInputProbe refTargetMotionInProbe =
            findOrCreateInputProbe (root, pid, duration, interval);
         configureTargetMotionProbe (
            refTargetMotionInProbe, moterm.getTargets(), pid);

         pid = ProbeID.TRACKED_POSITIONS;
         NumericOutputProbe refTargetMotionOutProbe =
            findOrCreateOutputProbe (root, pid, duration, interval);
         configureTargetMotionProbe (
            refTargetMotionOutProbe, moterm.getTargets(), pid);
         
         pid = ProbeID.SOURCE_POSITIONS;
         NumericOutputProbe modelTargetMotionOutProbe =
            findOrCreateOutputProbe (root, pid, duration, interval);
         configureTargetMotionProbe (
            modelTargetMotionOutProbe, moterm.getSources(), pid);
      }
      
      Property[] props = new Property[controller.getExciters().size()];
      for (int i = 0; i < controller.getExciters().size(); i++) {
         // XXX how to handle nested excitations?
         props[i] = controller.getExciters().get(i).getProperty("excitation"); 
      }      

      ProbeID pid = ProbeID.COMPUTED_EXCITATIONS;
      NumericOutputProbe excitationOutProbe =
         findOrCreateOutputProbe (root, pid, duration, interval);
      excitationOutProbe.setModel(controller.getMech());
      excitationOutProbe.setOutputProperties(props);
      excitationOutProbe.setAttachedFileName(pid.getFileName());

      pid = ProbeID.INPUT_EXCITATIONS;
      NumericInputProbe excitationInput =
         findOrCreateInputProbe (root, pid, duration, interval);
      excitationInput.setModel(controller.getMech());
      excitationInput.setInputProperties(props);
      excitationInput.setAttachedFileName(pid.getFileName());
      excitationInput.setActive(false);

      setLoneBreakPoint(root, duration);
   }
   
   private static NumericOutputProbe findOutputProbe (
      RootModel root, String name) {
      Probe p = root.getOutputProbes().get(name);
      if (p instanceof NumericOutputProbe) {
         return (NumericOutputProbe)p;
      }
      else {
         return null;
      }
   }

   /**
    * Searches for a specified output probe within a root model.  If
    * it is not found, returns {@code null}.
    *
    * @param root root model in which to search for the probe
    * @param pid probe identifier
    * @return located probe, or {@code null}
    */
   public static NumericOutputProbe findOutputProbe (
      RootModel root, ProbeID pid) {
      Probe p = root.getOutputProbes().get(pid.getName());
      if (p instanceof NumericOutputProbe) {
         return (NumericOutputProbe)p;
      }
      else {
         return null;
      }
   }

   private static NumericOutputProbe findOrCreateOutputProbe (
      RootModel root, ProbeID pid, double duration, double interval) {
      NumericOutputProbe outProbe;
      Probe p = findOutputProbe (root, pid);
      if (p != null && p instanceof NumericOutputProbe) {
         outProbe = (NumericOutputProbe)p;
      }
      else {
         outProbe = new NumericOutputProbe();
         outProbe.setName(pid.getName());
         outProbe.setStopTime (duration);
         outProbe.setUpdateInterval (interval);
         root.addOutputProbe(outProbe);
      }
      return outProbe;
   }

   /**
    * Searches for a specified probe within a root model.  If it is not found,
    * returns {@code null}.
    *
    * @param root root model in which to search for the probe
    * @param pid probe identifier
    * @return located probe, or {@code null}
    */
   public static Probe findProbe (RootModel root, ProbeID pid) {
      Probe p = findInputProbe (root, pid);
      if (p != null) {
         return p;
      }
      p = findOutputProbe (root, pid);
      if (p != null) {
         return p;
      }
      return null;
   }

   /**
    * Searches for a specified input probe within a root model.  If it is
    * not found, returns {@code null}.
    *
    * @param root root model in which to search for the probe
    * @param pid probe identifier
    * @return located probe, or {@code null}
    */
   public static NumericInputProbe findInputProbe (
      RootModel root, ProbeID pid) {
      Probe p = root.getInputProbes().get(pid.getName());
      if (p instanceof NumericInputProbe) {
         return (NumericInputProbe)p;
      }
      else {
         return null;
      }
   }

   /**
    * Sets the attached file name for a specified probe within a root model.
    * The method searches for the probe, and if it doesn't find it,
    * returns {@code false}.
    *
    * @param root root model in which to search for the probe
    * @param pid probe identifier
    * @return {@code true} if the probe was found and the file was renamed
    */
   public static boolean setProbeFileName (
      RootModel root, ProbeID pid, String name) {
      NumericProbeBase probe = findInputProbe (root, pid);
      if (probe != null) {
         probe.setAttachedFileName (name);
         return true;
      }
      probe = findOutputProbe (root, pid);
      if (probe != null) {
         probe.setAttachedFileName (name);
         return true;
      }
      else{
         System.err.print (
            "InverseManager.setProbeFileName: probe '" + pid.getName() +
            "' not found - have probes been created?");
         return false;
      }
   }

   /**
    * Sets the data for a specified input probe within a root model, using the
    * {@code NumericInputProbe} method {@link NumericInputProbe#addData}.  The
    * probe is also set be ``active'', and its interpolation order is set to
    * {@code CubicStep}. The method searches for the probe, and if it
    * doesn't find it, returns {@code false}.
    *
    * @param root root model in which to search for the probe
    * @param pid probe identifier
    * @param data array specifiying the input data
    * @param timeStep time step associated with the data; if set
    * to {@link NumericInputProbe#EXPLICIT_TIME}, the times are
    * set explicitly within the data
    * @return {@code true} if the probe was found and the data was set
    */
   public static boolean setInputProbeData (
      RootModel root, ProbeID pid, double[] data, double timeStep) {
      NumericInputProbe p = findInputProbe (root, pid);
      if (p == null) {
         System.err.print (
            "InverseManager.setInputProbeData: probe '" + pid.getName() +
            "' not found - have probes been created?");
         return false;
      }
      else {
         p.addData (data, timeStep);
         p.setInterpolationOrder (Interpolation.Order.CubicStep);
         p.setActive (true);
         return true;
      }
   }
   

   private static NumericInputProbe findInputProbe (
      RootModel root, String name) {
      Probe p = root.getInputProbes().get(name);
      if (p instanceof NumericInputProbe) {
         return (NumericInputProbe)p;
      }
      else {
         return null;
      }
   }

   private static NumericInputProbe findOrCreateInputProbe (
      RootModel root, ProbeID pid, double duration, double interval) {
      NumericInputProbe inProbe;
      Probe p = findInputProbe (root, pid);
      if (p != null && p instanceof NumericInputProbe) {
         inProbe = (NumericInputProbe)p;
      }
      else {
         inProbe = new NumericInputProbe();
         inProbe.setName (pid.getName());
         inProbe.setStopTime (duration);
         inProbe.setUpdateInterval (interval);
         root.addInputProbe(inProbe);
      }
      return inProbe;
   }
   
   private static MotionTargetTerm findMotionTargetTerm (
      TrackingController controller) {
      for (QPCostTerm term : controller.getCostTerms()) {
         if (term instanceof MotionTargetTerm) {
            return (MotionTargetTerm)term;
         }
      }
      for (QPConstraintTerm term : controller.getEqualityConstraints()) {
         if (term instanceof MotionTargetTerm) {
            return (MotionTargetTerm)term;
         }
      }
      return null;
   }
   
   private static ForceTargetTerm findForceTargetTerm (
      TrackingController controller) {
      for (QPCostTerm term : controller.getCostTerms()) {
         if (term instanceof ForceTargetTerm) {
            return (ForceTargetTerm)term;
         }
      }
      for (QPConstraintTerm term : controller.getEqualityConstraints()) {
         if (term instanceof ForceTargetTerm) {
            return (ForceTargetTerm)term;
         }
      }
      return null;
   }

   private static void configureTargetMotionProbe (NumericProbeBase probe,
      ArrayList<MotionTargetComponent> targets, ProbeID pid) {
//      System.out.println ("configuring motion probe");
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
      probe.setAttachedFileName(pid.getFileName());

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

   private static void configureTargetForceProbe(
      NumericProbeBase probe,
      ComponentList<ForceTarget> targets, ProbeID pid) {
//      System.out.println ("configuring force probe");
      ArrayList<Property> props = new ArrayList<Property>();
      for (ForceTarget target : targets) {
         props.add(target.getProperty("targetLambda"));
      }

      //      probe.setModel(myController.getMech());
      probe.setAttachedFileName(pid.getFileName());

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
   
   /**
    * Locates the {@link ProbeID#TARGET_POSITIONS} probe and
    * {@link ProbeID#SOURCE_POSITIONS} probe
    * within a root model and copies the contents of the latter to the former.
    * If either probe is not found, the method does nothing and returns false.
    *
    * @param root root model in which probes should be located
    * @return {@code true} if the probes were found and synchronized
    */
   public static boolean syncTargetProbes (RootModel root) {
      return syncProbes (
         root, ProbeID.TARGET_POSITIONS, ProbeID.SOURCE_POSITIONS);
   }

   /**
    * Locates the {@link ProbeID#INPUT_EXCITATIONS} probe and
    * {@link ProbeID#COMPUTED_EXCITATIONS} probe
    * within a root model and copies the contents of the latter to the
    * former.  If either probe is not found, the method does nothing and
    * returns false.
    *
    * @param root root model in which probes should be located
    * @return {@code true} if the probes were found and synchronized
    */
   public static boolean syncExcitationProbes (RootModel root) {
      return syncProbes (
         root, ProbeID.INPUT_EXCITATIONS, ProbeID.COMPUTED_EXCITATIONS);
   }

   private static boolean syncProbes (
      RootModel root, ProbeID destID, ProbeID sourceID) {

      NumericInputProbe dest = findInputProbe (root, destID);
      if (dest == null) {
         System.out.println (
            "Probe sync failed: destination probe " + destID + " not found");
         return false;
      }
      NumericOutputProbe source = findOutputProbe (root, sourceID);
      if (source == null) {
         System.out.println (
            "Probe sync failed: source probe " + sourceID + " not found");
         return false;
      }
      syncData (dest.getNumericList(), source.getNumericList());
      root.rerender();
      return true;
   }

   private static void syncData(NumericList dest, NumericList source)
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

   private static void setLoneBreakPoint(RootModel root, double t) {
      for (WayPoint wp : root.getWayPoints ()) {
         if (wp.isBreakPoint ())
            wp.setBreakPoint (false);
      }
      root.addBreakPoint (t);
   }
   
   /**
    * Clears all probes in a root model and then adds a set of probes for
    * controlling the inverse simulation, as described for {@link #addProbes}.
    * 
    * @param root root model to which the probes should be added
    * @param controller inverse controller for which the probes should be
    * created
    * @param duration time duration for each probe 
    * @param interval update interval for the output probes
    */
   public static void resetProbes (
      RootModel root, TrackingController controller,
      double duration, double interval) {
      if (root != null) {
         root.removeAllInputProbes ();
         root.removeAllOutputProbes ();
         addProbes (root, controller, duration, interval);
      }
   }
   
   public static class InverseControlPanel extends ControlPanel {

      public InverseControlPanel() {
      }

      public InverseControlPanel (TrackingController controller) {
         super("Inverse Control Panel");
         addButtons();
         addDefaultWidgets(controller);
         this.setScrollable (true);
      }

      RootModel getRoot() {
         CompositeComponent comp = getParent();
         while (comp != null) {
            if (comp instanceof RootModel) {
               return (RootModel)comp;
            }
            comp = comp.getParent();
         }
         return null;
      }
      
      private void addButtons() {
         JPanel buttons = new JPanel ();
         
         JButton rp = new JButton ("reset probes");
         rp.addActionListener (new ActionListener() {
            @Override
            public void actionPerformed (ActionEvent e) {
               RootModel root = getRoot();
               if (root != null) {
                  TrackingController controller =
                     InverseManager.findInverseController (root);
                  if (controller != null) {
                     resetProbes (
                        root, controller, 
                        controller.getProbeDuration(),
                        controller.getProbeUpdateInterval());
                  }
               }
            }
         });
         buttons.add (rp);
         
         JButton se = new JButton ("sync excitations");
         se.addActionListener (new ActionListener() {
            @Override
            public void actionPerformed (ActionEvent e) {
               syncExcitationProbes (getRoot());
            }
         });
         buttons.add (se);

         JButton st = new JButton ("sync targets");
         st.addActionListener (new ActionListener() {
            @Override
            public void actionPerformed (ActionEvent e) {
               syncTargetProbes (getRoot());
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
         
         for (QPConstraintTerm term : tc.getEqualityConstraints ()) {
            addConstraintTermBaseWidgets (term);
         }
         for (QPConstraintTerm term : tc.getInequalityConstraints()) {
            addConstraintTermBaseWidgets (term);
         }
         setScrollable(false);
      }
      
      private void addConstraintTermBaseWidgets (QPConstraintTerm term) {
         if (term instanceof QPCostTermBase) {
            addWidget (new JSeparator ());
            addWidget (new JLabel (term.getClass ().getSimpleName ()));
            for (PropertyInfo propinfo : ((QPCostTermBase)term)
               .getAllPropertyInfo ())
               addWidget ((QPCostTermBase)term, propinfo.getName ());
         }
      }
      
      

   }

}
