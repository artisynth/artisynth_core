package artisynth.demos.inverse;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.SparseNumberedBlockMatrix;
import maspack.matrix.Vector3d;
import maspack.render.RenderProps;
import maspack.render.RenderProps.LineStyle;
import artisynth.core.driver.Main;
import artisynth.core.gui.ControlPanel;
import artisynth.core.inverse.L2RegularizationTerm;
import artisynth.core.inverse.TrackingController;
import artisynth.core.materials.ConstantAxialMuscle;
import artisynth.core.materials.LinearAxialMuscle;
import artisynth.core.materials.PeckAxialMuscle;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.AxialSpring;
import artisynth.core.mechmodels.MechSystemSolver.Integrator;
import artisynth.core.mechmodels.Muscle;
import artisynth.core.mechmodels.Particle;
import artisynth.core.mechmodels.Point;
import artisynth.core.util.ArtisynthIO;
import artisynth.core.util.ArtisynthPath;
import artisynth.core.workspace.DriverInterface;
import artisynth.core.workspace.RootModel;

public class PointModel extends RootModel
{
   
   public String getAbout() {
      return"A point-mass model being controlled by the inverse tracking controller.\n\n"+
	    "The model and controller were developed by Ian Stavness, please cite: \n" +
	    "Ian Stavness, John E Lloyd, and Sidney Fels. "+
	    "Inverse-Dynamics Simulation of Muscular-Hydrostat FEM Models. " + 
	    "International Society of Biomechanics Congress (ISB '11), Talk, July 2011.";
   }
   
//   public static final Vector3d disturbance = new Vector3d(0,10,0);
   public static final Vector3d zero = new Vector3d();
   Vector3d disturbance = new Vector3d();
   
   boolean applyDisturbance = false;

   public enum DemoType {
      Point1d,
      Point2d,
      Point3d
   }
   
   
   public static DemoType defaultDemoType = DemoType.Point3d; 
   protected DemoType myDemoType; 
   
   /*
    * test simulations for stiffness control
    */
//   public synchronized void advance(long t0, long t1) {
//
//      if (applyDisturbance) {
//	 disturbance.setRandom(-MainInv.Z, MainInv.Z);
//	 // disturbance.set(MainInv.Z, 0, -MainInv.Z);
//	 disturbance.y = 0; // only inplace disturbance
//
//	 if (center != null) center.setExternalForce(disturbance);
//	 super.advance(t0, t1);
//	 if (center != null) center.setExternalForce(zero);
//      } else {
//	 super.advance(t0, t1);
//      }
//   }


   protected MechModel model;
   protected Particle center;
   
// String[] labels = new String[]{
//       "n","e","s","w"
//};

 String[] labels = new String[]{
         "n","nne", "ne", "ene",
         "e", "ese", "se", "sse",
         "s", "ssw", "sw", "wsw",
         "w", "wnw", "nw", "nnw"
 };
   
   double mass = 0.001; //kg
   double len = 10.0;
   double springK = 10.0;
   double springD = 0.1;
   double springRestLen = len*0.5;

   double muscleF = 1.0;
   double passiveFraction = 0.1;//1e-9;
   double muscleOptLen = len*0.5; //len*1.5); 
   double muscleMaxLen = labels.length;//len*2;
   double muscleD = 0.001;
   double muscleScaleFactor = 1000;
   double pointDamping = 0.1;

   
//   labelslong = {
//	 'North';
//	 'NorthNorthEast';
//	 'NorthEast';
//	 'EastNorthEast';
//	 'East';
//	 'EastSouthEast';
//	 'SouthEast';
//	 'SouthSouthEast';
//	 'South';
//	 'SouthSouthWest';
//	 'SouthWest';
//	 'WestSouthWest';
//	 'West';
//	 'WestNorthWest';
//	 'NorthWest';
//	 'NorthNorthWest'}
   
   public PointModel() throws IOException
   {
      super();
   }
   
   public PointModel(String name) throws IOException
   {
      this(name, defaultDemoType);
   }
   
   public PointModel(String name, DemoType demoType) {
      super(name);
      myDemoType = demoType;
      
      model = new MechModel("point");
      model.setGravity(0, 0, 0);
      model.setIntegrator (Integrator.Trapezoidal);
      model.setMaxStepSize (0.01);
      
      createModel(myDemoType);
      
      setupRenderProps();

      addTrackingController();      

//      MotionTarget targetModel = new MotionTarget("target");
//      ArrayList<ModelComponent> target = new ArrayList<ModelComponent>();
//      target.add (center);
//      targetModel.setTargets (target);
//      targetModel.setTransparent(true);
//      targetModel.setTargetPointRadius(2*center.getRenderProps().getPointRadius());
//      addModel (targetModel);
//
//      
//      if (MainInv.getWorkspace () != null)
//      { MainInv.getWorkspace ().getInvSolver().setW2norm (0.01);
//        MainInv.getWorkspace().setProbeDuration(1);
//      }
      

   }
   
   public void printType() {
      System.out.println("myType = "+myDemoType.toString());
   }
   
   public void createModel(DemoType demoType) {
      switch (demoType) {
      case Point1d: {
	 add1dMuscles();
	 break;
      }
      case Point2d: {
	 addCenter();
	 add2dLabeledMuscles(labels);
	 break;
      }
      case Point3d: {
	 addCenter();
	 add3dMuscles();
	 break;
      }
      default: {
	 System.err.println("PointModel, unknown demo type: "
	       + myDemoType.toString());
      }
      }
    addModel(model);
   }

   public void setupRenderProps()
   {
      // set render properties for model
      
      RenderProps rp = new RenderProps();
      rp.setPointStyle(RenderProps.PointStyle.SPHERE);
      rp.setPointColor(Color.LIGHT_GRAY);
      rp.setPointRadius(len/30);
      rp.setLineStyle(RenderProps.LineStyle.ELLIPSOID);
      rp.setLineColor(Color.RED);
      rp.setLineRadius(len/25);
      model.setRenderProps(rp);
      
      RenderProps.setPointColor(center, Color.WHITE);
      RenderProps.setPointRadius(center, len/25);
   }
   
   public void addCenter()
   {
      center = new Particle(mass);
      center.setDynamic(true);
      center.setName("center");
      center.setPointDamping(pointDamping);
      model.addParticle(center);
   }
   
   public void add2dLabeledMuscles(String[] labels) {
      
      double[] muscleFs;
      double muscleFmult;

      if (applyDisturbance) {
	 muscleFs = new double[] { 
	       1, 1, 1, 1, 
	       1, 1, 1, 1, 
	       1, 1, 1, 1, 
	       1, 1, 1, 1 };
	 muscleFmult = 100;
      } else {
	 muscleFs = new double[] { 
	       5, 1, 2, 1, 
	       5, 1, 2, 1, 
	       5, 1, 2, 1, 
	       5, 1, 2, 1 };
	 muscleFmult = 10;
      }
      
      addMuscles(new RigidTransform3d(), labels.length, 0.0);
      int i = 0;
      for (AxialSpring s : model.axialSprings()) {
	 if (s instanceof Muscle) {
	    s.setName(labels[i]);
//	    ((Muscle) s).setMaxForce(muscleFmult * muscleFs[i]);
//	    RenderProps.setLineRadius(s, 0.1 * muscleFs[i]);
	    i += 1;
	 }
      }
   }
   
   public void add3dMuscles() {
      int[] x = new int[]{-1,0,1};
      int[] y = new int[]{-1,0,1};
      int[] z = new int[]{-1,0,1};
      double eps = 1e-4;
      
      for (int i = 0; i < x.length; i++) {
	 for (int j = 0; j < y.length; j++) {
	    for (int k = 0; k < z.length; k++) {
	       Point3d pnt = new Point3d(x[i], y[j], z[k]);
	       if (pnt.x==0 || pnt.y==0 || pnt.z==0)
		  continue;
//	       if (pnt.norm() < 1e-4 || pnt.norm() > Math.sqrt(2))
//		  continue;
//	       if (pnt.norm() < 1e-4 || pnt.norm() > 1.0)
//		  continue;
	       pnt.normalize();
	       pnt.scale(len);
	       Particle endPt = new Particle(mass, pnt);
	       endPt.setDynamic(false);
	       model.addParticle(endPt);
	       Muscle m = addMuscle(endPt);
	       m.setName(String.format("x%dy%dz%d",x[i],y[j],z[k]));
	       RenderProps.setLineColor(m, Color.RED);
	    }
	 }
      }
      
   }
   
   public void add1dMuscles() {
      boolean[] dyn = new boolean[]{false,true,false};
      int[] x = new int[]{-1,0,1};
      
//      boolean[] dyn = new boolean[]{false,true, true, true,false};
//      int[] x = new int[]{-2,-1,0,1,2};
//      double eps = 1e-4;
      
      ArrayList<Point> pts = new ArrayList<Point>(x.length);
      for (int i = 0; i < x.length; i++) {
	 Point3d pnt = new Point3d(x[i], 0, 0);

//	 pnt.normalize();
	 pnt.scale(len);
	 Particle pt = new Particle(mass, pnt);
	 pt.setPointDamping(pointDamping);
	 pt.setDynamic(dyn[i]);
	 model.addParticle(pt);
	 pts.add(pt);
	 
	 if (x[i] == 0) {
	    center = pt;
	 }
      }
      
      for (int i = 1; i < pts.size(); i++) {
	 AxialSpring m;
	 Point p0 = pts.get(i-1);
	 Point p1 = pts.get(i);
//	 if (p0==center || p1==center)
//	    m = addAxialSpring(p0, p1);
//	 else
	    m = addMuscle(p0, p1);
	 m.setName("m"+Integer.toString(m.getNumber()));
      }
      
   }
   
   public void addFullMuscles()
   {
      RigidTransform3d X = new RigidTransform3d();
      int num = 2;
      addMuscles(X, num, len/2.0); // x-z plane

      X.R.setAxisAngle(1,0,0,Math.PI/2.0);
      addMuscles(X,num, 0.0);
      
      X.R.setAxisAngle(0,1,0,Math.PI/2.0);
      addMuscles(X,num, -len/2.0);
      
   }
   
   public void addMuscles()
   {
      addMuscles(new RigidTransform3d(), 2, 0.0);
   }
   
   public void addMuscles(RigidTransform3d X, int num, double offset)
   {
      
      for (int i = 0; i < num; i++)
      {
         double a = 2*Math.PI*((double)i/num);

         Point3d pnt = new Point3d(len*Math.sin(a),0.0,len*Math.cos(a));
         pnt.transform(X.R);
         Particle fixed = new Particle(mass, pnt);
         fixed.setDynamic(false);
         model.addParticle(fixed);
         
         addMuscle(fixed);
      }
   }
   
   private Muscle addMuscle(Point endPt) {
      return addMuscle(endPt, center);
   }
    
   private Muscle addMuscle(Point p0, Point p1) {
//      Muscle m = Muscle.createLinear(muscleF, muscleMaxLen);
      Muscle m = new Muscle();
//      ConstantAxialMuscleMaterial mat = new ConstantAxialMuscleMaterial();
      LinearAxialMuscle mat = new LinearAxialMuscle();
//      PeckAxialMuscleMaterial mat = new PeckAxialMuscleMaterial();
      mat.setMaxForce(muscleF);
      mat.setMaxLength(muscleMaxLen);
      mat.setDamping(muscleD);
      mat.setOptLength(muscleOptLen);
      mat.setPassiveFraction(passiveFraction);
      mat.setForceScaling(muscleScaleFactor);
      m.setMaterial(mat);
      m.setRestLength (len);
      m.setFirstPoint(p0);
      m.setSecondPoint(p1);
      model.addAxialSpring(m);
      RenderProps.setLineColor(m, Color.RED);
      return m;
   }
   
  
   public void addHorizontalSprings()
   {
      RigidTransform3d X = new RigidTransform3d();
      int num = 2;

      X.R.setAxisAngle(1,0,0,Math.PI/2.0);
      addSprings(X,num, 0.0);
      
      X.R.setAxisAngle(0,1,0,Math.PI/2.0);
      addSprings(X,num, 0.0);

   }
   
   public void addSprings(RigidTransform3d X, int num, double offset) {
      for (int i = 0; i < num; i++) {
	 double a = 2 * Math.PI * ((double) i / num);

	 Point3d pnt = new Point3d(len * Math.sin(a), 0.0, len * Math.cos(a));
	 pnt.transform(X.R);
	 Particle fixed = new Particle(1.0, pnt);
	 fixed.setDynamic(false);
	 model.addParticle(fixed);
	 addAxialSpring(fixed, center);
      }
   }
   
   private AxialSpring addAxialSpring(Point p0, Point p1) {
      AxialSpring s = new AxialSpring(springK, springD, springRestLen);
      s.setFirstPoint(p0);
      s.setSecondPoint(p1);
      model.addAxialSpring(s);
      RenderProps.setLineColor(s, Color.GRAY);
      RenderProps.setLineStyle(s, LineStyle.LINE);
      RenderProps.setLineWidth(s, 4);
      return s;
   }
      
   public MechModel getMechModel()
   {
      return model;
   }

   
   @Override
   public void attach(DriverInterface driver)
   {
      super.attach(driver);
      
      if (getControlPanels().size() == 0)
       { 
         ControlPanel panel = new ControlPanel("activations", "");
         for (AxialSpring s : model.axialSprings ())
          {
            if (s instanceof Muscle) {
               Muscle m = (Muscle)s;
               String name = (m.getName()==null?"m"+m.getNumber():m.getName().toUpperCase());
               panel.addWidget(name, m, "excitation", 0.0, 1.0);
            }
          }
//         panel.addWidget (model, "integrator");
         panel.pack();
         // panel.setVisible(true) -- will be set visible in addControlPanel:
         panel.setLocation(driver.getFrame().getLocation().x +
                           driver.getFrame().getWidth(), 
                           driver.getFrame().getLocation().y);
         addControlPanel (panel);
       }
      
      String wdir = "east/";
      
      // set default working directory to repository location
      File workingDir = new File(ArtisynthPath.getSrcRelativePath(
	    PointModel.class, "data/point/"+wdir));
      ArtisynthPath.setWorkingDir(workingDir);
      System.out.println("Set working directory to " + 
         ArtisynthPath.getWorkingDir().getAbsolutePath());
      
//      loadProbes();
      
//      addTrackingController();
   }
   
   
   public void addTrackingController() {
      TrackingController myTrackingController = new TrackingController(model, "tcon");
      for (AxialSpring s : model.axialSprings()) {
	 if (s instanceof Muscle) {
	    myTrackingController.addExciter((Muscle)s);
	 }
      }
//      myTrackingController.addTerm(new StiffnessTerm(model, center));
//      StiffnessTerm kTerm = TrackingController.addStiffnessTarget(center, new int[]{0,2});
//      kTerm.setStiffnessTargetType(StiffnessTargetType.DIAG);
      
//      ComplianceTerm2d cterm = new ComplianceTerm2d(TrackingController, center, new int[]{0,2});
//      cterm.setComplianceTargetType(StiffnessTargetType.DIAG);
//      TrackingController.addTerm(cterm);
      
      myTrackingController.addL2RegularizationTerm();
//      myTrackingController.addTerm(new DampingTerm(TrackingController));

//      myTrackingController.addTerm(new StaticMotionTargetTerm(TrackingController));
      myTrackingController.addMotionTarget(center);
      
//      myTrackingController.getSolver().setBounds(0.01, 0.99);
      addController(myTrackingController);
   }
   
   
   public void loadProbes()
   {

      String probeFileFullPath = ArtisynthPath.getWorkingDir().getPath() + "/0probes.art";
      System.out.println("Loading Probes from File: " + probeFileFullPath);

      try
      {
         Main.getWorkspace().scanProbes(
                  ArtisynthIO.newReaderTokenizer(probeFileFullPath), this);
      }
      catch (Exception e)
      {
         System.out.println("Error reading probe file");
         e.printStackTrace();
      }
   }
   
   
   public SparseBlockMatrix getK() {
      return model.getActiveStiffness();
   }

}
