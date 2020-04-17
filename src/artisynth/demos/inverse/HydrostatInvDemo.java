package artisynth.demos.inverse;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import maspack.properties.PropertyMode;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.render.Renderer.FaceStyle;
import maspack.util.PathFinder;
import artisynth.core.femmodels.MuscleBundle;
import artisynth.core.gui.FemControlPanel;
import artisynth.core.inverse.TrackingController;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.ExcitationComponent;
import artisynth.core.mechmodels.MechSystemSolver.Integrator;
import artisynth.core.mechmodels.MotionTargetComponent;
import artisynth.core.mechmodels.Muscle;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.probes.OutputProbe;
import artisynth.core.probes.Probe;
import artisynth.core.util.ArtisynthPath;
import artisynth.core.workspace.DriverInterface;
import artisynth.demos.fem.HydrostatDemo;
import artisynth.demos.fem.HydrostatModel.Shape;

public class HydrostatInvDemo extends HydrostatDemo
{
   
   public String getAbout() {
      return"A 3D FEM idealized muscular-hydrostat model being controlled by the inverse tracking controller.\n\n"+
	    "The model and controller were developed by Ian Stavness, please cite: \n" +
	    "Ian Stavness, John E Lloyd, and Sidney Fels. "+
	    "Inverse-Dynamics Simulation of Muscular-Hydrostat FEM Models. " + 
	    "International Society of Biomechanics Congress (ISB '11), Talk, July 2011.";
   }

   protected MechModel mech; 
   double s = 1;
   boolean useBundlesForInverse = true;
   double stepsize = 0.01;

   public void build(String[] args) throws IOException
   {
      build (Shape.Beam);
      addWayPoint (0.3);
   }
   
   public void build (Shape shape) throws IOException {
      super.build (shape);
      hydro.setMaxStepSize(stepsize);
      hydro.setIntegrator(Integrator.Trapezoidal);
      this.setMaxStepSize (stepsize);
      
      mech = new MechModel("mech");
      mech.setIntegrator(hydro.getIntegrator());
      mech.setMaxStepSize(hydro.getMaxStepSize());
      mech.setGravity(hydro.getGravity());
      removeModel(hydro);
      mech.addModel(hydro);
      addModel(mech);

      probeFilename = null;
      
      setupRenderProps();
      
//      for (MuscleBundle b : hydro.getMuscleBundles()) {
//	 RenderProps.setLineColor(b, Color.RED);
//      }
      if (useBundlesForInverse) {
	 FemControlPanel.createMuscleBundlesPanel(this, hydro);
      }
      else {
	 FemControlPanel.createMuscleExcitersPanel(this, hydro);
      } 
      
      File workingDir = new File(
         PathFinder.getSourceRelativePath (
            HydrostatInvDemo.class, "data/hydrostat/complex"));
      ArtisynthPath.setWorkingDir(workingDir);
//      loadProbes (probeFilename);
      
      System.out.println("num act = " + hydro.numActivations ());

      addInverseController();
   }
   
   public void attach(DriverInterface driver)
   {
//      super.attach(driver);
//      loadControlPanels (driver.getFrame());
   }
   
   protected TrackingController trackingController;
   
   protected void addInverseController() {
      trackingController = new TrackingController(mech, "tcon");
      for (ModelComponent comp : hydro.createTargetList()) {
	 trackingController.addMotionTarget((MotionTargetComponent)comp);
      }
      
      if (useBundlesForInverse) {
	 for (MuscleBundle b : hydro.getMuscleBundles()) {
	    trackingController.addExciter(b);
	    Color exColor = b.getRenderProps().getLineColor();
	    b.setExcitationColor(exColor);
	    RenderProps.setLineColor(b, Color.WHITE);
	 }
      } else {
	 for (ExcitationComponent exciter : hydro.getMuscleExciters()) {
	    trackingController.addExciter(exciter);
	 }
      }
      
      trackingController.addL2RegularizationTerm(100*100);
      trackingController.setProbeDuration (5.0);
      trackingController.setProbeUpdateInterval (stepsize);
      trackingController.createProbesAndPanel (this);
      addController(trackingController);
   }
   
   public void setupRenderProps() {
      
      RenderProps.setFaceStyle(hydro, FaceStyle.FRONT);
      RenderProps.setFaceColor(hydro, new Color(0.8f, 0.8f, 1f));
      RenderProps.setVisible(hydro.getElements(), false);
      RenderProps.setPointSize(hydro.getNodes(), 0);
      
      ArrayList<Muscle> visibleMuscles = new ArrayList<Muscle>();
      
      for (MuscleBundle b : hydro.getMuscleBundles()) {
	 String name = b.getName();
	 RenderProps.setLineRadiusMode(b, PropertyMode.Inherited);
//	 if (name.startsWith("horz") || name.startsWith("vert")) {	   
//	    for (Muscle m : b)
//	       visibleMuscles.add(m);
//	 }
//	 else
//	    RenderProps.setVisible(b, false);
      }
      RenderProps.setLineRadius(hydro.getMuscleBundles(), s*0.75);
      
//      float h = 1f / (float)visibleMuscles.size();
//      for (int i = 0; i < visibleMuscles.size(); i++) {
//	 System.out.println("h = "+i*h);
//	 RenderProps.setLineColor(visibleMuscles.get(i), Color.getHSBColor (i*h, 1f, 1f));
//      }
   }
   
//   public void printActivationLabels() {
//      System.out.println("\n\nlabels = {");
//      for (String label : hydro.labels) {
//	 System.out.println("'"+label+"';");
//      }
//      System.out.println("};\n\n");
//   }
}
