/**
 * Copyright (c) 2014, by the Authors: Ian Stavness (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.inverse;

import java.awt.Color;
import java.awt.Dimension;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import javax.swing.JLabel;
import javax.swing.JSeparator;

import maspack.matrix.MatrixNd;
import maspack.matrix.VectorNd;
import maspack.properties.PropertyInfo;
import maspack.properties.PropertyList;
import maspack.render.GLRenderer;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.Renderable;
import maspack.render.RenderProps.PointStyle;
import maspack.util.ListView;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import artisynth.core.driver.Main;
import artisynth.core.femmodels.FemModel;
import artisynth.core.gui.ControlPanel;
import artisynth.core.mechmodels.CollisionHandler;
import artisynth.core.mechmodels.ExcitationComponent;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.MechSystem;
import artisynth.core.mechmodels.MechSystemBase;
import artisynth.core.mechmodels.MechSystemModel;
import artisynth.core.mechmodels.MechSystemSolver.Integrator;
import artisynth.core.mechmodels.MotionTargetComponent;
import artisynth.core.mechmodels.MultiPointMuscle;
import artisynth.core.mechmodels.Muscle;
import artisynth.core.mechmodels.MuscleExciter;
import artisynth.core.mechmodels.PointList;
import artisynth.core.modelbase.ComponentList;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.ComponentChangeEvent;
import artisynth.core.modelbase.ComponentListImpl;
import artisynth.core.modelbase.ControllerBase;
import artisynth.core.modelbase.ReferenceList;
import artisynth.core.modelbase.RenderableComponent;
import artisynth.core.modelbase.RenderableComponentList;
import artisynth.core.modelbase.ScanWriteUtils;
import artisynth.core.modelbase.StepAdjustment;
import artisynth.core.util.*;

/**
 * "Inverse" controller for computing muscle activations based on
 * a set of kinematic target trajectories.
 * <p>
 * 
 * Terminology: <br>
 * <table>
 * 
 * <tbody>
 *    <tr><td>"Targets"</td><td>are trajectories of points/frames that we wish our model to follow</td></tr>
 *    <tr><td>"Markers" or "Sources"</td><td>are the points/frames in the model we want to follow the target trajectories</td></tr>
 *    <tr><td>"Excitation"</td><td>we actually mean, "activation", since we are currently ignoring the excitation dynamics</td></tr>
 * </tbody>
 * </table>
 * <p>         
 *              
 * Papers:<br>
 * - Ian Stavness, JE Lloyd, and SS Fels. Automatic Prediction of Tongue Muscle 
 * Activations Using a Finite Element Model. Journal of Biomechanics, 
 * 45(16):2841-2848, 2012.<br>
 * 
 * - Ian Stavness, AG Hannam, JE Lloyd, and SS Fels. Predicting muscle patterns 
 * for hemimandibulectomy models. Computer Methods in Biomechanics and 
 * Biomedical Engineering, 13(4):483-491, 2010.
 * 
 * 
 * @author Ian Stavness, with modifications by Antonio Sanchez
 *
 */
public class TrackingController extends ControllerBase
   implements CompositeComponent, RenderableComponent {

   // set to handle the motion term as a hard constraint
   boolean handleMotionTargetAsConstraint = false;

   // By default, use the inverse manager to control input/output
   // probes.
   public static final boolean DEFAULT_USE_INVERSE_MANAGER = true;

   // For Governor, to limit the maximum jump in excitation value
   //      (defaults to 1, since excitation is [0,1]) 
   public static double DEFAULT_MAX_EXCITATION_JUMP = 1;
   
   protected boolean debug = false;     // prints debug statements
   boolean enabled = true;

   protected boolean targetsVisible = true;
   protected boolean sourcesVisible = true;
   protected double targetsPointRadius = 1d;
   boolean useInverseManager = DEFAULT_USE_INVERSE_MANAGER; 

   protected MechSystemBase myMech;
   protected LeastSquaresSolver mySolver;
   protected ArrayList<LeastSquaresTerm> myCostTerms;
   protected ArrayList<LeastSquaresTerm> myConstraintTerms;
   // protected ComponentList<OptimizationTerm> myOptimizationTerms;
   protected MotionTargetTerm myMotionTerm = null;  // contains target information
   protected L2RegularizationTerm myRegularizationTerm = null;
   // contains child components and implements CompositeComponent methods
   protected ComponentListImpl<ModelComponent> myComponents;
   
   // component and reference lists for associated targets/sources/exciters
   protected PointList<TargetPoint> targetPoints;
   protected RenderableComponentList<TargetFrame> targetFrames;
   protected ComponentList<ExcitationComponent> exciters;
   protected ReferenceList sourcePoints;
   protected ReferenceList sourceFrames;

   /*
    * for debugging:
    */
   // ComplianceTerm cTerm = null;
   // StiffnessTerm kTerm = null;

   protected MatrixNd H = new MatrixNd();
   protected VectorNd f = new VectorNd();
   
   protected MatrixNd A = new MatrixNd();
   protected VectorNd b = new VectorNd();


   protected MuscleExciter myExciters;  // list of inputs
   protected VectorNd myExcitations = new VectorNd();    // computed excitatios
   protected VectorNd prevExcitations = new VectorNd();  // previous, for damping terms
   protected VectorNd initExcitations = new VectorNd();  // initial, in case non-zero start (again, for damping)

   protected double maxExcitationJump = DEFAULT_MAX_EXCITATION_JUMP; // limit jump in activations

   protected int exSize;   // number of excitations
   protected int costRowSize;  // number of rows in H
   protected int conRowSize;  // number of rows in A

   public static PropertyList myProps =
      new PropertyList(TrackingController.class, ControllerBase.class);

   static {
      myProps.add("renderProps * *", "render properties", null);
      myProps.add("enabled isEnabled *", "enable/disable controller", true);
      myProps.add(
         "maxExcitationJump * *", "maximum excitation step",
         DEFAULT_MAX_EXCITATION_JUMP);
      myProps.add(
         "targetsVisible * *", "allow showing or hiding of motion targets",
         true);
      myProps.add(
         "sourcesVisible * *", "allow showing or hiding of motion markers",
         true);
      myProps.add(
         "usePDControl * *", "use PD controller for motion term",
         MotionTargetTerm.DEFAULT_USE_PD_CONTROL);
      // myProps.add("debug * *", "print target stuff", false);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public TrackingController () {
      this(null, null);
   }

   /**
    * Creates a tracking controller for a given mech system
    * @param m mech system, typically your "MechModel"
    */
   public TrackingController (MechSystemBase m) {
      this(m, null);
   }

   /**
    * Creates and names a tracking controller for a given mech system
    * @param m mech system, typically your "MechModel"
    * @param name name to give the controller
    */
   public TrackingController (MechSystemBase m, String name) {
      super();
      setMech(m);
      setName(name);

      mySolver = new LeastSquaresSolver();
      myCostTerms = new ArrayList<LeastSquaresTerm>();
      myConstraintTerms = new ArrayList<LeastSquaresTerm> ();
      
      myComponents = new ComponentListImpl (ModelComponent.class, this);

      // list of target points that store/show the location of motion targets for points
      targetPoints =
         new PointList<TargetPoint> (TargetPoint.class, "targetPoints");
      // always show this component, even if it's empty:
      targetPoints.setNavpanelVisibility (NavpanelVisibility.ALWAYS);
      add (targetPoints);

      // list of target points that store/show the location of motion targets for bodies
      targetFrames =
         new RenderableComponentList<TargetFrame> (TargetFrame.class, "targetFrames");
      // always show this component, even if it's empty:
      targetFrames.setNavpanelVisibility (NavpanelVisibility.ALWAYS);
      add (targetFrames);

      // list of excitations that store the computed excitations from the tracking simulation
      exciters =
         new ComponentList<ExcitationComponent> (ExcitationComponent.class, "excitationSources");
      // always show this component, even if it's empty:
      exciters.setNavpanelVisibility (NavpanelVisibility.ALWAYS);
      add (exciters);
      
      // reference lists to point to the various dynamic components that are sources
      // these components are all expected to be of type MotionTargetComponent
      sourcePoints = new ReferenceList ("sourcePoints");
      sourceFrames = new ReferenceList ("sourceFrames");
      add(sourcePoints);
      add(sourceFrames);

      
      // myOptimizationTerms = new ComponentList<OptimizationTerm>(
      // OptimizationTerm.class, "optTerms", "ot", this);
      myExciters = new MuscleExciter ("exciters");
//      if (myMech instanceof MechModel) {
//         ((MechModel)myMech).addMuscleExciter (myExciters);
//      }
      add (myExciters);
      
      myMotionTerm = new MotionTargetTerm (this);
      if (handleMotionTargetAsConstraint) {
         // add a constraint on the motion target
         myConstraintTerms.add (myMotionTerm);
         
         // need a regularization term in the cost function if the motion term is constraint
         myRegularizationTerm = new L2RegularizationTerm (this);
         myCostTerms.add (myRegularizationTerm);
      }
      else {
         // just add motion target to cost function
         myCostTerms.add (myMotionTerm);
      }
   }

   /**
    * Returns the "motion" term, responsible for tracking error
    * @return motion term for tracking error
    */
   public MotionTargetTerm getMotionTerm() {
      return myMotionTerm;
   }

   public L2RegularizationTerm getRegularizationTerm() {
      return myRegularizationTerm;
   }
   
   /**
    * Applies the controller, estimating and setting the next
    * set of muscle activations
    */
   public void apply(double t0, double t1) {

      if (!isEnabled()) {
         return;
      }

      checksize();
      if (costRowSize <= 0 || exSize <= 0) {
         return;
      }

      if (t0 == 0) { // XXX need better way to zero excitations on reset
         myExcitations.set(initExcitations);
      }

      int rowoff = 0;
      for (LeastSquaresTerm term : myCostTerms) {
         rowoff = term.getTerm(H, f, rowoff, t0, t1);
      }      
      
      rowoff = 0;
      for (LeastSquaresTerm term : myConstraintTerms) {
         rowoff = term.getTerm(A, b, rowoff, t0, t1);
      }
      //System.out.println ("H=\n" + H.toString ("%10.5f"));

      prevExcitations.set(myExcitations);
      mySolver.solve(myExcitations, H, f, prevExcitations, A, b);

      //System.out.println ("excitations="+myExcitations);
      NumberFormat f4 = new NumberFormat("%.4f");
      for (int j = 0; j < myExcitations.size(); j++) {
         double e = myExcitations.get(j);
         double preve = prevExcitations.get(j);
         //System.out.println("Activation value for exciter number "+ j +" is: "+f4.format(e));
        
         if (e - preve > maxExcitationJump) {
            System.out.println("Activation jump surpassed limit: "
               + f4.format(e) + "-"
               + f4.format(preve) + ">" + maxExcitationJump);
            e = preve + maxExcitationJump;
         } else if (preve - e > maxExcitationJump) {
            System.out.println("Activation jump surpassed limit: "
               + f4.format(preve)
               + "-" + f4.format(e) + ">" + maxExcitationJump);
            e = preve - maxExcitationJump;
         }
         myExcitations.set(j, e);
      }

      setExcitations(myExcitations, 0);

      // if (kTerm != null) {
      // System.out.println("K* = "+kTerm.getStiffnessTargetVec().toString("%8.2f"));
      // System.out.println("K  = "+kTerm.getStiffnessVec().toString("%8.2f")+"\n");
      // }

      // if (cTerm != null) {
      // System.out.println("C* = "+cTerm.getComplianceTargetVec().toString("%8.6f"));
      // System.out.println("C  = "+cTerm.getComplianceVec().toString("%8.6f")+"\n");
      // }
   }

   /**
    * Clears all terms and disposes storage
    */
   public void dispose() {
      System.out.println("optimization controller dispose()");
      mySolver.dispose();
      for (LeastSquaresTerm term : myCostTerms) {
         term.dispose();
      }
      targetPoints.clear ();
      remove (targetPoints);
      targetFrames.clear ();
      remove (targetFrames);
      sourcePoints.clear ();
      remove (sourcePoints);
      sourceFrames.clear ();
      remove (sourceFrames);
      myExciters.removeAllTargets ();
      remove (myExciters);
   }

   private void checksize() {
      int nCost = 0;
      for (LeastSquaresTerm term : myCostTerms) {
         nCost += term.getTargetSize();
      }
      
      int nCon = 0;
      for (LeastSquaresTerm term : myConstraintTerms) {
         nCon += term.getTargetSize();
      }

      int m = myExciters.numTargets ();

      if (nCost != costRowSize || nCon != conRowSize || m != exSize)
         resize(nCost, nCon, m);
   }

   private void resize(int rows, int conRows, int excitations) {
      H.setSize(rows, excitations);
      f.setSize(rows);
      A.setSize(conRows, excitations);
      b.setSize(conRows);
      myExcitations.setSize(excitations);
      prevExcitations.setSize(excitations);
      initExcitations.setSize(excitations);
      costRowSize = rows;
      conRowSize = conRows;
      exSize = excitations;
   }

   /**
    * Add another term to the optimization, typically 
    * for regularization
    * @param term the term to add
    */
   public void addCostTerm(LeastSquaresTerm term) {
      myCostTerms.add(term);
      resize(costRowSize + term.getTargetSize(), conRowSize, exSize);

      /*
       * for debugging:
       */
      // if (term instanceof StiffnessTerm) {
      // kTerm = (StiffnessTerm)term;
      // }
      // else if (term instanceof ComplianceTerm) {
      // cTerm = (ComplianceTerm)term;
      // }

   }

   /**
    * Retrieves a list of all terms, including the tracking error
    * and any regularization terms
    */
   public ArrayList<LeastSquaresTerm> getCostTerms() {
      return myCostTerms;
   }
   
   /**
    * Retrieves a list of all terms, including the tracking error
    * and any regularization terms
    */
   public ArrayList<LeastSquaresTerm> getConstraintTerms() {
      return myConstraintTerms;
   }

   /**
    * Returns whether or not the controller is enabled
    */
   public boolean isEnabled() {
      return enabled;
   }

   /**
    * Enable or disable the controller
    */
   public void setEnabled(boolean enabled) {
      this.enabled = enabled;
      // if (myMech != null && myMech instanceof MechModel) {
      // ((MechModel)myMech).setDynamicsEnabled(enabled);
      // }
   }

   /**
    * Set the mech system, used by the solver to compute forces
    */
   public void setMech(MechSystemBase m) {
      setModel(m);
      myMech = m;
   }

   /**
    * Gets the mechanical system used for computing forces
    */
   public MechSystemBase getMech() {
      return myMech;
   }

   /**
    * Returns the integrator used by the mech system
    */
   public Integrator getIntegrator() {
      if (myMech instanceof MechModel)
         return ((MechModel)myMech).getIntegrator();
      else
         return null;
   }

   /**
    * Gets the solver object
    */
   public LeastSquaresSolver getSolver() {
      return mySolver;
   }

   /**
    * Sets bounds for the excitations
    * @param lower lower-bound (typically 0)
    * @param upper upper-bound (typically 1)
    */
   public void setExcitationBounds(double lower, double upper) {
      mySolver.setBounds(lower, upper);
   }

   /**
    * Determines the set of forces given a set of excitations.
    * Note: this actually sets the excitations in the model
    * 
    * @param forces output from the mech system
    * @param excitations input muscle excitations
    * 
    */
   public void getForces(VectorNd forces, VectorNd excitations) {
      setExcitations(excitations, /* idx= */0);
      myMech.getActiveForces(forces);
   }

   /**
    * Updates constraints in the mech system at time t, including
    * contacts
    */
   public void updateConstraints(double t) {
      myMech.updateConstraints(t, null, MechSystem.UPDATE_CONTACTS);
   }

   /**
    * Sets excitations provided in the <code>ex</code> vector
    * starting at index <code>idx</code>. 
    * @param ex vector of excitations to use
    * @param idx start index
    * @return current index in the <code>ex</code> buffer <code>idx+numExcitations()</code>
    */
   public int setExcitations(VectorNd ex, int idx) {
      double[] buf = ex.getBuffer();
      for (int i = 0; i < myExciters.numTargets (); i++) {
         myExciters.getTarget(i).setExcitation(buf[idx++]);
      }
      // for FemMuscleMaterial, need to invalidate fem stress before
      // updateForces()
      invalidateStressIfFem(myMech);
      myMech.updateForces(/* t= */0);
      return idx;
   }

   /**
    * Gets the list of excitators used as free variables in the inverse routine
    */
   public ListView<ExcitationComponent> getExciters() {
      return myExciters.getTargetView ();
   }
   
   public MuscleExciter getMuscleExciter() {
      return myExciters;
   }

   /**
    * Adds an exciter to be used as a free variable in the inverse routine
    * @param ex
    */
   public void addExciter(ExcitationComponent ex) {
      addExciter (ex, 1d);
   }
   
   public void addExciter(ExcitationComponent ex, double gain) {
      myExciters.addTarget(ex, gain);
      resize(costRowSize, conRowSize, myExciters.numTargets());
      if (ex instanceof MultiPointMuscle) {
         MultiPointMuscle m = (MultiPointMuscle)ex;
         if (m.getExcitationColor() == null) {
            RenderProps.setLineColor(m, Color.WHITE);
            m.setExcitationColor(Color.RED);
         }
      }
      else if (ex instanceof Muscle) {
         Muscle m = (Muscle)ex;
         if (m.getExcitationColor() == null) {
            RenderProps.setLineColor(m, Color.WHITE);
            m.setExcitationColor(Color.RED);
         }
      }
   }

   /**
    * Clears all exciters
    */
   public void clearExciters() {
//      myExciters.clear();
      myExciters.removeAllTargets ();
      resize(costRowSize, conRowSize, 0);
   }

   /**
    * Number of exciters controlled by this controller
    */
   public int numExcitations() {
      return myExciters.numTargets();
   }

   /**
    * Fills the supplied <code>ex</code> vector with current excitation
    * values starting at index <code>idx</code>
    * @param ex vector of excitations to fill
    * @param idx starting index
    * @return next index to use <code>idx+numExcitations()</code>
    */
   public int getExcitations(VectorNd ex, int idx) {
      double[] buf = ex.getBuffer();
      for (int i = 0; i < myExciters.numTargets(); i++) {
         buf[idx++] = myExciters.getTarget(i).getNetExcitation();
      }
      return idx;
   }

   private void invalidateStressIfFem(MechSystemModel model) {
      if (model instanceof MechModel) {
         for (MechSystemModel subModel : ((MechModel)model).models()) {
            invalidateStressIfFem(subModel);
         }
      }
      else if (model instanceof FemModel) {
         ((FemModel)model).invalidateStressAndStiffness();
      }
   }

   /**
    * Adds a source for the tracking and creates a corresponding target point
    * or frame object
    * @param source point or frame to track
    * @return the created target point/frame
    */
   public MotionTargetComponent addMotionTarget(MotionTargetComponent source) {
      return myMotionTerm.addTarget(source);
   }
   
   public void removeMotionTarget(MotionTargetComponent source) {
      myMotionTerm.removeTarget(source);
   }

   /**
    * Adds a source for the tracking and creates a corresponding target point
    * or frame object
    * @param source point or frame to track
    * @param weight the weight by which to scale this target's contribution
    * @return the created target point/frame
    */
   public MotionTargetComponent addMotionTarget (
      MotionTargetComponent source, double weight) {
      return myMotionTerm.addTarget(source, weight);
   }

   /**
    * Adds and returns a standard L2 regularization term on excitations
    */
   public L2RegularizationTerm addL2RegularizationTerm() {
      if (myRegularizationTerm == null) {
         myRegularizationTerm = new L2RegularizationTerm(this); 
         addCostTerm(myRegularizationTerm);
      }
      return myRegularizationTerm;
   }
   
   /**
    * Adds and returns a standard L2 regularization term on excitations,
    * weighted by the supplied value
    */
   public L2RegularizationTerm addL2RegularizationTerm(double w) {
      L2RegularizationTerm l2reg = addL2RegularizationTerm ();
      l2reg.setWeight (w);
      return l2reg;
   }

   /**
    * Adds and returns a standard damping term on excitations
    */
   public DampingTerm addDampingTerm() {
      DampingTerm damp = new DampingTerm(this);
      addCostTerm(damp);
      return damp;
   }

   /**
    * Adds and returns a standard damping term on excitations,
    * weighted by the supplied value
    */
   public DampingTerm addDampingTerm(double w) {
      DampingTerm dampingTerm = new DampingTerm(this);
      dampingTerm.setWeight(w);
      addCostTerm(dampingTerm);
      return dampingTerm;
   }

   /**
    * Adds both L2 and damping regularization terms with supplied weights
    */
   public void addRegularizationTerms(Double w_l2norm, Double w_damping) {
      if (w_l2norm != null) {
         addL2RegularizationTerm(w_l2norm);
      }

      if (w_damping != null) {
         addDampingTerm(w_damping);
      }
   }

   /**
    * For the governor, limits jump in excitation for all exciters to the
    * supplied delta
    */
   public void setMaxExcitationJump(double j) {
      maxExcitationJump = j;
   }

   /**
    * Gets the governor's maximum excitation jump
    */
   public double getMaxExcitationJump() {
      return maxExcitationJump;
   }

   /**
    * Show or hide the targets
    */
   public void setTargetsVisible(boolean show) {
      ArrayList<MotionTargetComponent> moTargetParticles =
         myMotionTerm.getTargets();
      for (MotionTargetComponent p : moTargetParticles) {
         if (p instanceof RenderableComponent) {
            RenderProps.setVisible((RenderableComponent)p, show);
         }
      }
      targetsVisible = show;
   }

   /**
    * Show or hide the sources
    */
   public void setSourcesVisible(boolean show) {
      ArrayList<MotionTargetComponent> moTargetParticles =
         myMotionTerm.getSources();
      for (MotionTargetComponent p : moTargetParticles) {
         if (p instanceof RenderableComponent) {
            RenderProps.setVisible((RenderableComponent)p, show);
         }
      }
      sourcesVisible = show;
   }
   
   /**
    * Sets targets to shows a spheres with given radius
    */
   public void setTargetsPointRadius(double radius) {
      if (myMotionTerm != null) {
         RenderProps.setPointRadius (targetPoints, radius);
      }
      targetsPointRadius = radius;
   }

   public boolean getSourcesVisible() {
      return sourcesVisible;
   }

   public boolean getTargetsVisible() {
      return targetsVisible;
   }

   /**
    * Puts controller into debug mode, printing messages
    */
   public void setDebug(boolean b) {
      debug = b;
   }

   public boolean getDebug() {
      return debug;
   }

   /**
    * Flag used by InverseManager telling it whether or not
    * to set up and control the targets with probes
    */
   public boolean isManaged() {
      return useInverseManager;
   }

   /**
    * Sets a flag used by InverseManager telling it whether or not
    * to set up and control the targets with probes.  If true,
    * the InverseManager expects certain probes to be defined.
    * Otherwise, you must manually control target positions.
    * 
    */
   public void setManaged(boolean set) {
      useInverseManager = set;
   }
   
   public void setUsePDControl(boolean usePD) {
      myMotionTerm.usePDControl = usePD;
   }

   public boolean getUsePDControl() {
      return myMotionTerm.usePDControl;
   }
   
   /**
    * Creates a control panel for this inverse controller
    */
   public ControlPanel createInverseControlPanel() {
      ControlPanel inverseControlPanel =
         new ControlPanel("Inverse Control Panel");

      for (PropertyInfo propinfo : getAllPropertyInfo())
         inverseControlPanel.addWidget(this, propinfo.getName());

      for (LeastSquaresTerm term : getCostTerms()) {
         if (term instanceof LeastSquaresTermBase) {
            inverseControlPanel.addWidget(new JSeparator());
            inverseControlPanel.addWidget(
               new JLabel(term.getClass().getSimpleName()));

            PropertyList propList =
               ((LeastSquaresTermBase)term).getAllPropertyInfo();
            for (PropertyInfo propinfo : propList) {
               if (term instanceof MotionTargetTerm) {
                  inverseControlPanel.addWidget(
                     (MotionTargetTerm)term, propinfo.getName());
               } else {
                  inverseControlPanel.addWidget(
                     (LeastSquaresTermBase)term, propinfo.getName());
               }
            }
         }
      }

      Dimension d = Main.getMainFrame().getSize();
      java.awt.Point pos = Main.getMainFrame().getLocation();
      inverseControlPanel.getFrame().setLocation(d.width + pos.x, pos.y);

      inverseControlPanel.setScrollable(false);
      Main.getWorkspace().registerWindow(inverseControlPanel.getFrame());
      inverseControlPanel.setVisible(true);
      
      return inverseControlPanel;
   }

   /**
    * Returns non-editable ListView of motion sources
    */
   
   private class TargetIterator implements Iterator<MotionTargetComponent> {
      Iterator<MotionTargetComponent> myIterator;

      TargetIterator() {
         myIterator = myMotionTerm.getSources ().iterator();
      }

      public boolean hasNext() {
         return myIterator.hasNext();
      }

      public MotionTargetComponent next() {
         return myIterator.next();
      }

      public void remove() {
         throw new UnsupportedOperationException();
      }
   }
   
   private class TargetView implements ListView<MotionTargetComponent> {
      public Iterator<MotionTargetComponent> iterator() {
         return new TargetIterator();
      }

      public MotionTargetComponent get (int idx) {
         return myMotionTerm.getSources().get (idx);
      }

      public int size() {
         return myMotionTerm.getSources().size();
      }

      public int indexOf (Object elem) {
         for (int i = 0; i < myMotionTerm.getSources().size(); i++) {
            if (myMotionTerm.getSources().get (i) == elem) {
               return i;
            }
         }
         return -1;
      }

      public boolean contains (Object elem) {
         return indexOf (elem) != -1;
      }
   }
   
   public ListView<MotionTargetComponent> getTargetView() {
      return new TargetView();
   }
   
   /**
    * Returns the motion target term, responsible for trajectory error
    */
   public MotionTargetTerm getMotionTargetTerm() {
      return myMotionTerm;
   }

   /**
    * Returns the set of targets
    */
   public ArrayList<MotionTargetComponent> getMotionTargets() {
      if (myMotionTerm == null) {
         return null;
      }
      return myMotionTerm.getTargets();
   }

   /**
    * Returns the set of sources
    */
   public ArrayList<MotionTargetComponent> getMotionSources() {
      if (myMotionTerm == null) {
         return null;
      }
      return myMotionTerm.getSources();
   }

   /**
    * Sets render properties for targets and sources
    * @param targets
    * @param sources
    */
   public void setMotionRenderProps(RenderProps targets, RenderProps sources) {
      if (myMotionTerm != null) {
         myMotionTerm.setTargetRenderProps(targets);
         myMotionTerm.setSourceRenderProps(sources);
      }
   }

   /**
    * Returns the set of target weights
    */
   public VectorNd getMotionTargetWeights() {
      if (myMotionTerm == null) {
         return null;
      }
      return myMotionTerm.getTargetWeights ();
   }
   
   public void setMotionTargetWeight(MotionTargetComponent comp, double w) {
      if (myMotionTerm == null) {
         return;
      }
      VectorNd wvec = myMotionTerm.getTargetWeights ();
      int idx = myMotionTerm.getTargets ().indexOf (comp);
      if (idx == -1) {
         idx = myMotionTerm.getSources ().indexOf (comp);
      }
      if (idx != -1 && idx < wvec.size ()) {
         wvec.set (idx, w);
         myMotionTerm.setTargetWeights (wvec);
      }
   }

   
   /**
    * Initialize the controller with the current excitations in the
    * model, allows for non-zero starting excitations
    */
   public void initializeExcitations() {

      for (int i=0; i<myExciters.numTargets(); i++) {
         double val = myExciters.getTarget(i).getExcitation();
         prevExcitations.set(i,val);
         myExcitations.set(i,val);
         initExcitations.set(i,val);
      }
   }
   
   /**
    * Sets initial excitations to the supplied values
    */
   public void setInitialExcitations(VectorNd init) {
      if (init.size() != myExcitations.size()) {
         throw new IllegalArgumentException("Wrong number of excitations");
      }
      initExcitations.set(init);
   }

   
   // ========== Begin Rendering ==========

   @Override
   public void prerender (RenderList list) {
      super.prerender (list);
      recursivelyPrerender (this, list);
   }
   
   protected void recursivelyPrerender (
      CompositeComponent comp, RenderList list) {

       for (int i=0; i<comp.numComponents(); i++) {
         ModelComponent c = comp.get (i);
         if (c instanceof Renderable) {
            list.addIfVisible ((Renderable)c);
         }
         else if (c instanceof CompositeComponent) {
            recursivelyPrerender ((CompositeComponent)c, list);
         }
      }     
   }
   
   // ========== Begin CompositeComponent implementation ==========

   /**
    * {@inheritDoc}
    */
   public ModelComponent get (String nameOrNumber) {
      return myComponents.get (nameOrNumber);
   }

   /**
    * {@inheritDoc}
    */
   public ModelComponent get (int idx) {
      return myComponents.get (idx);
   } 

   /**
    * {@inheritDoc}
    */
   public ModelComponent getByNumber (int num) {
      return myComponents.getByNumber (num);
   }

   /**
    * {@inheritDoc}
    */
   public int numComponents() {
      return myComponents.size();
   }

   /**
    * {@inheritDoc}
    */
   public int indexOf (ModelComponent comp) {
      return myComponents.indexOf (comp);
   }

   /**
    * {@inheritDoc}
    */
   public ModelComponent findComponent (String path) {
      return ComponentUtils.findComponent (this, path);
   }

   /**
    * {@inheritDoc}
    */
   public int getNumberLimit() {
      return myComponents.getNumberLimit();
   }

   /**
    * {@inheritDoc}
    */
   public NavpanelDisplay getNavpanelDisplay() {
      return NavpanelDisplay.NORMAL;
   }

   /**
    * {@inheritDoc}
    */
   public void componentChanged (ComponentChangeEvent e) {
      myComponents.componentChanged (e);
      notifyParentOfChange (e);
   }

   /**
    * {@inheritDoc}
    */
   public void updateNameMap (
      String newName, String oldName, ModelComponent comp) {
      myComponents.updateNameMap (newName, oldName, comp);
   }

   /**
    * {@inheritDoc}
    */
   public boolean hierarchyContainsReferences() {
      return false;
   }

   protected void add (ModelComponent comp) {
      myComponents.add (comp);
   }
   
   protected boolean remove (ModelComponent comp) {
      return myComponents.remove (comp);
   }

   // write/scan implementation assuming a known set of child components with
   // set names and types:

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      super.writeItems (pw, fmt, ancestor);
      myComponents.writeComponentsByName (pw, fmt, ancestor);
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (myComponents.scanAndStoreComponentByName (rtok, tokens)) {
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   public void scan (
         ReaderTokenizer rtok, Object ref) throws IOException {
      //
      // any required prescan code goes here  ...
      //
      super.scan (rtok, ref);
   }

   protected boolean postscanItem (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
      
      if (myComponents.postscanComponent (tokens, ancestor)) {
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }

   public void postscan (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
      super.postscan (tokens, ancestor);
      //
      // any required postscan code goes here ...
      //
      myMech = (MechModel)getModel();
   }


   // write/scan implementation assuming a variable set of child components
   // with unknown names and types:

   // protected void writeItems (
   //    PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
   //    throws IOException {

   //    super.writeItems (pw, fmt, ancestor);
   //    myComponents.writeComponents (pw, fmt, ancestor);
   // }

   // protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
   //    throws IOException {

   //    if (super.scanItem (rtok, tokens)) {
   //       return true;
   //    }
   //    rtok.nextToken();
   //    return myComponents.scanAndStoreComponent (rtok, tokens);
   // }

   // @Override
   //    public void scan (
   //       ReaderTokenizer rtok, Object ref) throws IOException {
   //    //
   //    // put any required prescan code here ....
   //    //
   //    myComponents.scanBegin();
   //    super.scan (rtok, ref);
   // }

   // protected boolean postscanItem (
   //    Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
      
   //    if (myComponents.postscanComponent (tokens, ancestor)) {
   //       return true;
   //    }
   //    return super.postscanItem (tokens, ancestor);
   // }

   // @Override
   // public void postscan (
   // Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
   //    super.postscan (tokens, ancestor);
   //    myComponents.scanEnd();
   //    //
   //    // any required postscan code goes here ...
   //    //
   //    myMech = (MechModel)getModel();
   // }

   // ========== End CompositeComponent implementation ==========

   
}
