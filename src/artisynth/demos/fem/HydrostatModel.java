package artisynth.demos.fem;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;

import maspack.matrix.AxisAngle;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.render.Renderer.FaceStyle;
import maspack.render.Renderer.LineStyle;
import maspack.widgets.LabeledComponentBase;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.femmodels.FemElement3d;
import artisynth.core.femmodels.FemFactory;
import artisynth.core.femmodels.FemMarker;
import artisynth.core.femmodels.FemNode;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.femmodels.MuscleBundle;
import artisynth.core.materials.ConstantAxialMuscle;
import artisynth.core.mechmodels.Muscle;
import artisynth.core.femmodels.FemMuscleModel;
import artisynth.core.gui.ControlPanel;
import artisynth.core.mechmodels.ExcitationComponent;
import artisynth.core.mechmodels.Muscle;
import artisynth.core.mechmodels.MuscleExciter;
import artisynth.core.mechmodels.Point;
import artisynth.core.mechmodels.MechSystemSolver.Integrator;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.probes.PlotTraceInfo;
import artisynth.core.util.TimeBase;

public class HydrostatModel extends FemMuscleModel {
   double l, r, rin;

   int nl, nr, nt;

   double eps = 1e-10;

   double muscleForce = 1000.0; // 1 Newton

   double toldist = 1e-10; // tolerance used for point closeness

   double tolangle = 1e-2; // tolerance used for fibre direction check

   boolean periphLongP = true; // only create peripheral longitudnal fibres

   boolean periphCircP = true; // only create peripheral circular fibres

   boolean useExcitersP = false; // segment with exciters instead of bundles --
   // see addMuscles()

   boolean simpleMuscleGroupingsP = true;
   
   boolean useFullAnteriorPosteriorMuscles = true;

   Shape myShape;

   ArrayList<Muscle> muscleFibres = new ArrayList<Muscle>();
   
   public enum Element {
      Tet, Hex, Quadtet,
   }

   public enum Shape {
      Beam, Tube, Tentacle
   }

   public enum Axis {
      X ("long", 3, new Vector3d (1, 0, 0)), Y ("horz", 2, new Vector3d (
         0, 1, 0)), Z ("vert", 2, new Vector3d (0, 0, 1));

      String name;

      int numSegments;

      Vector3d dir = new Vector3d();

      Axis (String name, int numSegments, Vector3d dir) {
         this.name = name;
         this.numSegments = numSegments;
         this.dir.set (dir);
      }

      public Vector3d getDir() {
         return dir;
      }

      public String getDirName() {
         return name;
      }
   }
   
   public enum Hemisphere {
      R ("right", Vector3d.Y_UNIT, Vector3d.Z_UNIT), 
      L ("left", new Vector3d(0,-1,0), Vector3d.Z_UNIT),
      S ("superior", Vector3d.Z_UNIT, Vector3d.Y_UNIT),
      I ("inferior", new Vector3d(0,0,-1), Vector3d.Y_UNIT);
      
      String name;
      Vector3d hemidir = new Vector3d();
      Vector3d transdir = new Vector3d();

      Hemisphere (String name, Vector3d dir, Vector3d tdir) {
         this.name = name;
         this.hemidir.set (dir);
         this.transdir.set (tdir);
      }

      public boolean check(Point p) {
	 return hemidir.dot(p.getPosition()) > 0;
      }
      
      public Vector3d getDir() {
         return hemidir;
      }

      public Vector3d getTransDir() {
	 return transdir;
      }
      
      public String getName() {
         return name;
      }
   }

   public enum TubeAxis {
      LONG, RADIAL, CIRC
   };

   public HydrostatModel() {
      super (null);
   }

   
   public HydrostatModel (String name) throws IOException {
      this (name, true, Shape.Beam);
   }
   
   public HydrostatModel (String name, boolean fullMusclesAP, Shape shape) throws IOException {
      this (name, Element.Hex, shape, 100, 40, 7, 3, false, fullMusclesAP);
   }

   public HydrostatModel (String name, Element elem, Shape shape, double l,
	   double r, int nl, int nr, boolean simpleExciters) throws IOException {
      this(name, elem, shape, l, r, nl, nr, simpleExciters, true);
   
   }

   public HydrostatModel (String name, Element elem, Shape shape, double l,
   double r, int nl, int nr, boolean simpleExciters, boolean fullMusclesAP) throws IOException {
      super (name);
      this.l = l;
      this.r = r;
      this.rin = r / 10.0;
      this.nl = nl;
      this.nr = nr;
      this.nt = 2 * nl;
      this.simpleMuscleGroupingsP = simpleExciters;
      this.useFullAnteriorPosteriorMuscles = fullMusclesAP;
      this.myShape = shape;
      createGeometry (elem, shape);
      clampEnd();
      addMuscles (shape);
      setupRenderProps();
   }

   public ArrayList<ModelComponent> createTargetList(int[] nodeNums) {
      ArrayList<ModelComponent> targetNodes = new ArrayList<ModelComponent>(nodeNums.length);

      for (int nodeNum : nodeNums) {
	 targetNodes.add(getNode(nodeNum));
      }
      return targetNodes;
   }
   
   private double getXdirLength() {
      Point3d pmin = new Point3d(Double.MAX_VALUE,Double.MAX_VALUE,Double.MAX_VALUE);
      Point3d pmax = new Point3d(-Double.MAX_VALUE,-Double.MAX_VALUE,-Double.MAX_VALUE);
      
      updateBounds(pmin, pmax);
      pmax.sub(pmin);
      return pmax.x;
   }
   
   public ArrayList<ModelComponent> createTargetList() {
      double l = getXdirLength();
      ArrayList<ModelComponent> targetNodes = new ArrayList<ModelComponent>();

      for (FemNode n : myNodes) {
         if (n.getPosition().x < -l / 2 + eps) {
            if (myShape == Shape.Tube) {
               double r2 =
                  n.getPosition().y * n.getPosition().y + n.getPosition().z
                  * n.getPosition().z;
               if (Math.sqrt (r2) <= 2 * rin)
                  continue;
            }
            targetNodes.add (n);
            if (simpleMuscleGroupingsP)
               break;
         }
      }

      return targetNodes;
   }

   public void setupRenderProps() {
      setSurfaceRendering (SurfaceRender.Shaded);
      // RenderProps.setAlpha (this, 0.5);
      RenderProps.setFaceStyle (this, Renderer.FaceStyle.NONE);
      RenderProps.setLineWidth (this, 1);

      RenderProps.setPointSize (getNodes(), 4);
      RenderProps.setPointColor (getNodes(), Color.BLUE);
      // RenderProps.setFaceStyle (getNodes(), Faces.BACK);

      for (MuscleBundle b : getMuscleBundles()) {
         RenderProps.setLineStyle (b, Renderer.LineStyle.SPINDLE);
         RenderProps.setLineRadius (b, 0.5);
      }

   }

   public void initialize (double t) {
      super.initialize (t);
   }

   public void clampEnd() {
      for (artisynth.core.femmodels.FemNode n : getNodes()) {
         if (n.getPosition().x > (l / 2.0) - eps)
            n.setDynamic (false);
      }
   }

   public void createBeam (Element e) {
      switch (e) {
         case Tet:
            FemFactory.createTetGrid (this, l, r, r, nl, nr, nr);
            break;
         case Hex:
            FemFactory.createHexGrid (this, l, r, r, nl, nr, nr);
            break;
         case Quadtet:
            FemFactory.createQuadtetGrid (this, l, r, r, nl, nr, nr);
            break;
         default:
            System.err.println ("HydrostatDemo unknown element type "
            + e.toString());
            break;
      }
   }

   public void createTube (Element e) {
      switch (e) {
         case Tet:
            FemFactory.createTetTube (this, l, rin, r, nt, nl, nr);
            break;
         case Hex:
            FemFactory.createHexTube (this, l, rin, r, nt, nl, nr);
            break;
         case Quadtet:
            FemFactory.createQuadtetTube (this, l, rin, r, nt, nl, nr);
            break;
         default:
            System.err.println ("HydrostatDemo unknown element type "
            + e.toString());
            break;
      }
      transformGeometry (new RigidTransform3d (new Vector3d(), new AxisAngle (
         0, 1, 0, Math.PI / 2.0)));
   }

   public void createGeometry (Element e, Shape s) {

      switch (s) {
      	 case Tentacle:
         case Beam:
            createBeam (e);
            break;
         case Tube:
            createTube (e);
            break;
         default:
            System.err.println ("HydrostatDemo unknown shape " + s.toString());
      }

      setGravity (0, 0, 0);

      // mechanical props from tongue (need to scale distance from m to mm)
      double scaling = 1000;
      setDensity (1000 / (scaling * scaling * scaling));
      setLinearMaterial (6912 / scaling, 0.49, true);
      setIncompressible (FemModel3d.IncompMethod.OFF);

      setParticleDamping (1.22);
      setStiffnessDamping (0.05);

      setImplicitIterations (100);
      setImplicitPrecision (0.001);
      setMaxStepSize (0.01);
      setIntegrator (Integrator.BackwardEuler);

   }

   public void addMuscles (Shape s) {
      switch (s) {
      	 case Tentacle:
            addTentacleMuscles();
            addTentacleExciters();      
            break;
         case Beam:
            addBeamMuscles();
            break;
         case Tube:
            addTubeMuscles();
            break;
         default:
            System.err.println ("HydrostatDemo unknown shape " + s.toString());
      }
      
      for (MuscleBundle b : myMuscleList) {
	 b.setFibresActive(true);
      }
   }
   
   /*
    * testing for tentacle simulations
    */
   public void addTentacleMuscles() {

      periphLongP = false; // don't just create peripheral long fibers
      for (Axis a : Axis.values())
	 createMuscles(a, a.getDirName());

      for (Axis a : Axis.values()) {
	 MuscleBundle b = new MuscleBundle(a.getDirName());
	 for (Muscle m : muscleFibres) {
	    if (Math.abs(m.getDir().dot(a.getDir())-1.0) < 1e-4) {
//	    if (m.getName().startsWith(a.getDirName())) {
	       b.addFibre(m);
	    }
	 }
	 addMuscleBundle(b);
	 RenderProps.setLineStyle(b, Renderer.LineStyle.SPINDLE);
	 RenderProps.setLineColor(b, PlotTraceInfo.getPaletteColors()[b
	       .getNumber()]);

      }
   }
   
   public void addTentacleExciters() {
      
      for (Axis a : Axis.values()) {
	 MuscleBundle b = myMuscleList.get(a.getDirName());
	 for (Hemisphere hemi : Hemisphere.values()) {
	    if (Math.abs(hemi.getDir().dot(a.getDir())) == 1.0)
	       continue;
	   
	    MuscleExciter ex = new MuscleExciter(a.getDirName() + "_"
		  + hemi.getName());
	    
	    for (Muscle m : b.getFibres()) {
	       if (hemi.check(m.getFirstPoint())
		     && hemi.check(m.getSecondPoint())) {
		  ex.addTarget(m, 1.0);
	       }
	    }
	    addMuscleExciter(ex);
	 }
      }
   }
   
   public void addTentacleExcitersX() {
      
      for (Axis a : Axis.values()) {
	 for (Hemisphere hemi : Hemisphere.values()) {
	    MuscleExciter ex = new MuscleExciter(a.getDirName() + "_"
		  + hemi.getName());
	    for (Muscle m : muscleFibres) {
	       if (Math.abs(m.getDir().dot(a.getDir())) == 1.0
		     && hemi.check(m.getFirstPoint())
		     && hemi.check(m.getSecondPoint())) {
		  ex.addTarget(m, 1.0);
	       }
	    }
	    addMuscleExciter(ex);
//	    RenderProps.setLineStyle(b, LineStyle.ELLIPSOID);
//	    RenderProps.setLineColor(b, PlotTraceInfo.getPaletteColors()[b
//		  .getNumber()]);
	 }
      }
   }

   public void addBeamMuscles() {

      /*
       * Two alternative strategies for defining muscle groups
       * 
       * SegmentedExciters -- add Fibres into principle bundles
       * createMuscles(Axis, Bundle) - Segment bundles into smallest functional
       * group exciters -- createExciters() NOTE -- if functional exciter groups
       * overlap muscle fibres get double excitation
       * 
       * SegmentedBundles -- name fibres by principle axis --
       * createMuscles(Axis, PrefixName) - Add fibres into smallest functional
       * group bundles -- createBundles() NOTE -- functional group bundles
       * cannot overlap
       */

      if (useExcitersP) {
         for (Axis a : Axis.values()) {
            MuscleBundle b =
               new MuscleBundle (a.getDirName());
            addMuscleBundle (b);
            createMuscles (a, b);
         }

         // colorBundles (new String[]{"long", "vert","horz"}, true);

         if (simpleMuscleGroupingsP) {
            addSimpleExciters();
            // colorBundles (new String[]{"vert","horz"}, false);
            colorExciters (
               new String[] { "vert", "horz", "long_s", "long_i" }, false);
         }
         else {
            addExciters();

            colorExciters (
               new String[] { "long_rp", "long_rm", "long_ra", "long_lp",
                             "long_lm", "long_la", "long_sp", "long_sm",
                             "long_sa", "long_ip", "long_im", "long_ia",
                             "vert_a", "vert_p", "horz_a", "horz_p" }, false);
            // "vert_a", "vert_m", "vert_p", "horz_a", "horz_m", "horz_p"});
         }
      }
      else {
         for (Axis a : Axis.values())
            createMuscles (a, a.getDirName());
         addBundles();

	 if (useFullAnteriorPosteriorMuscles) {
	    colorBundles(new String[] { "long_rp",
	       "long_rm", "long_ra", "long_lp", "long_lm", "long_la",
	       "long_sp", "long_sm", "long_sa", "long_ip", "long_im",
	       "long_ia", "vert_a", "vert_am", "vert_ma", "vert_m", "vert_mp",
	       "vert_pm", "vert_p", "vert_fixed", "horz_a", "horz_am",
	       "horz_ma", "horz_m", "horz_mp", "horz_pm", "horz_p",
	       "horz_fixed", }, false);
	 }
	 else {
	    colorBundles(new String[] { "long_rp", "long_rm", "long_ra",
		  "long_lp", "long_lm", "long_la", "long_sp", "long_sm",
		  "long_sa", "long_ip", "long_im", "long_ia", "vert_a",
		  "vert_p", "horz_a", "horz_p" }, false);
	 }
      }
   }

   public void createMuscles (Axis axis, String prefix) {
      Point3d p0 = new Point3d();
      Point3d p1 = new Point3d();
      int cnt = 0;
      for (double i = 0; i < (axis == Axis.X ? nl : nl + 1); i++)
         for (double j = 0; j < (axis == Axis.Y ? nr : nr + 1); j++)
            for (double k = 0; k < (axis == Axis.Z ? nr : nr + 1); k++) {
               if (periphLongP && axis == Axis.X
               && ((j != 0 && j != nr) && (k != 0 && k != nr)))
                  continue;

               p0.set (l * (i / nl - 0.5), r * (j / nr - 0.5), r
               * (k / nr - 0.5));
               p1.set (l * ((axis == Axis.X ? i + 1 : i) / (nl) - 0.5), r
               * ((axis == Axis.Y ? j + 1 : j) / nr - 0.5), r
               * ((axis == Axis.Z ? k + 1 : k) / nr - 0.5));

               Muscle f = new Muscle(findPoint (p0), findPoint (p1));
               f.setConstantMuscleMaterial(muscleForce, 1);
               f.setName (prefix + (cnt++)); // f.getNumber());
               muscleFibres.add (f);
            }
   }

   public void createMuscles (Axis axis, MuscleBundle bundle) {
      RenderProps.setLineWidth (bundle, 4);
      Point3d p0 = new Point3d();
      Point3d p1 = new Point3d();
      for (double i = 0; i < (axis == Axis.X ? nl : nl + 1); i++)
         for (double j = 0; j < (axis == Axis.Y ? nr : nr + 1); j++)
            for (double k = 0; k < (axis == Axis.Z ? nr : nr + 1); k++) {
               if (periphLongP && axis == Axis.X
               && ((j != 0 && j != nr) && (k != 0 && k != nr)))
                  continue;

               p0.set (l * (i / nl - 0.5), r * (j / nr - 0.5), r
               * (k / nr - 0.5));
               p1.set (l * ((axis == Axis.X ? i + 1 : i) / (nl) - 0.5), r
               * ((axis == Axis.Y ? j + 1 : j) / nr - 0.5), r
               * ((axis == Axis.Z ? k + 1 : k) / nr - 0.5));

               Muscle f = new Muscle(findPoint (p0), findPoint (p1));
               f.setConstantMuscleMaterial(muscleForce, 1);
               bundle.addFibre (f);
            }

   }

   public void addTubeMuscles() {
      // createTubeLongMuscles();
      // createTubeHelixMuscles();
      for (TubeAxis a : TubeAxis.values()) {
         MuscleBundle b =
            new MuscleBundle (a.name().toLowerCase());
         addMuscleBundle (b);
         createTubeMuscles (a, b);
      }

      if (simpleMuscleGroupingsP) {
         addSimpleTubeExciters();
         colorExciters (
            new String[] { "radial", "circ", "long_s", "long_i" }, false);
      }
      else {
         addTubeExciters();
         colorExciters (new String[] { "long_rp", "long_rm", "long_ra",
                                      "long_lp", "long_lm", "long_la",
                                      "long_sp", "long_sm", "long_sa",
                                      "long_ip", "long_im", "long_ia",
                                      "radial_a", "radial_p", "circ_a",
                                      "circ_p" }, false);
      }

   }

   public void createTubeLongMuscles() {
      MuscleBundle bundle = new MuscleBundle ("long");
      addMuscleBundle (bundle);
      RenderProps.setLineWidth (bundle, 4);
      RenderProps.setLineColor (bundle, Color.RED);
      Point3d p0 = new Point3d();
      Point3d p1 = new Point3d();

      double dl = l / (nl - 1);
      double dt = 2 * Math.PI / nt;
      double dr = (r - rin) / (nr - 1);

      for (double i = 0; i < nt; i++)
         for (double j = (periphLongP ? nr - 1 : 0); j < nr; j++) {
            for (double k = 0; k < nl - 1; k++) {
               p0.set (
                  -l / 2 + k * dl, -(rin + dr * j) * Math.cos (dt * i),
                  (rin + dr * j) * Math.sin (dt * i));
               p1.set (-l / 2 + (k + 1) * dl, -(rin + dr * j)
               * Math.cos (dt * i), (rin + dr * j) * Math.sin (dt * i));

               Muscle f = new Muscle(findPoint (p0), findPoint (p1));
               f.setConstantMuscleMaterial(muscleForce, 1);
               bundle.addFibre (f);
            }
         }

   }

   public void createTubeMuscles (TubeAxis axis, MuscleBundle bundle) {
      addMuscleBundle (bundle);
      RenderProps.setLineWidth (bundle, 4);
      RenderProps.setLineColor (bundle, Color.WHITE);
      Point3d p0 = new Point3d();
      Point3d p1 = new Point3d();

      double dl = l / (nl - 1);
      double dt = 2 * Math.PI / nt;
      double dr = (r - rin) / (nr - 1);

      int init_j =
         (periphLongP && axis == TubeAxis.LONG || periphCircP
         && axis == TubeAxis.CIRC ? nr - 1 : 0);
      for (double i = 0; i < nt; i++)
         for (double j = init_j; j < (axis == TubeAxis.RADIAL ? nr - 1 : nr); j++)
            for (double k = 0; k < (axis == TubeAxis.LONG ? nl - 1 : nl); k++) {
               p0.set (
                  -l / 2 + k * dl, -(rin + dr * j) * Math.cos (dt * i),
                  (rin + dr * j) * Math.sin (dt * i));

               double radius =
                  (rin + dr * (axis == TubeAxis.RADIAL ? j + 1 : j));
               double angle = dt * (axis == TubeAxis.CIRC ? i + 1 : i);
               p1.set (
                  -l / 2 + (axis == TubeAxis.LONG ? k + 1 : k) * dl, -radius
                  * Math.cos (angle), radius * Math.sin (angle));

               Muscle f = new Muscle (findPoint (p0), findPoint (p1));
               f.setConstantMuscleMaterial(muscleForce, 1);
               bundle.addFibre (f);
            }
   }

   public void createTubeHelixMuscles() {

   }

   public void addTubeExciters() {
      double min = Double.NEGATIVE_INFINITY;
      double max = Double.POSITIVE_INFINITY;
      double d = -l / 2;
      String[] long_ap_segments = new String[] { "a", "m", "p" };
      double[][] ap_dists;

      if (nl == 6) {
         double dd = l / (nl - 1) / 2;
         ap_dists =
            new double[][] { { d, d + 4 * dd + eps },
                            { d + 4 * dd - eps, d + 6 * dd + eps },
                            { d + 6 * dd - eps, -d } };
      }
      else {
         ap_dists = new double[long_ap_segments.length][2];
         for (int i = 0; i < long_ap_segments.length; i++) {
            ap_dists[i][0] = d;
            d -= (double)l / nl;
            ap_dists[i][1] = d;
         }
      }

      int i = 0;
      for (String ap : long_ap_segments) {
         createExciter (
            "long_l" + ap, new Point3d (ap_dists[i][0], min, min), new Point3d (
               ap_dists[i][1], -15 - eps, max), myMuscleList.get ("long"));
         createExciter ("long_r" + ap, new Point3d (
            ap_dists[i][0], 15 + eps, min), new Point3d (
            ap_dists[i][1], max, max), myMuscleList.get ("long"));
         createExciter (
            "long_i" + ap, new Point3d (ap_dists[i][0], min, min), new Point3d (
               ap_dists[i][1], max, -15 - eps), myMuscleList.get ("long"));
         createExciter ("long_s" + ap, new Point3d (
            ap_dists[i][0], min, 15 + eps), new Point3d (
            ap_dists[i][1], max, max), myMuscleList.get ("long"));
         // createExciter ("radial_"+ap, new Point3d(ap_dists[i][0],-r,-r), new
         // Point3d(ap_dists[i][1],r,r), myMuscleList.get ("radial"));
         // createExciter ("circ_"+ap, new Point3d(ap_dists[i][0],-r,-r), new
         // Point3d(ap_dists[i][1],r,r), myMuscleList.get ("circ"));
         i++;
      }

      String[] trans_ap_segments = new String[] { "a", "p" };
      d = -l / 2;
      double d_inc = l / (double)trans_ap_segments.length;
      for (String ap : trans_ap_segments) {
         createExciter (
            "radial_" + ap, new Point3d (d - eps, min, min), new Point3d (d
            + d_inc + eps, max, max), myMuscleList.get ("radial"));
         createExciter (
            "circ_" + ap, new Point3d (d - eps, min, min), new Point3d (d
            + d_inc + eps, max, max), myMuscleList.get ("circ"));
         d += d_inc;
      }

      createGroup ("long", "_l", new String[] { "_lp", "_lm", "_la" });
      createGroup ("long", "_r", new String[] { "_rp", "_rm", "_ra" });
      createGroup ("long", "_i", new String[] { "_ip", "_im", "_ia" });
      createGroup ("long", "_s", new String[] { "_sp", "_sm", "_sa" });
      createGroup ("long", "", new String[] { "" });
      createGroup ("radial", "", new String[] { "" });
      createGroup ("circ", "", new String[] { "" });
   }

   public void addSimpleTubeExciters() {
      // createExciter ("long_l", new Point3d(-l/2,-r/2,-r), new
      // Point3d(l/2,-r/2,r), myMuscleList.get ("long"));
      // createExciter ("long_r", new Point3d(-l/2,r/2,-r), new
      // Point3d(l/2,r/2,r), myMuscleList.get ("long"));
      createExciter ("long_i", new Point3d (-l / 2, -r, -r), new Point3d (
         l / 2, r, -r / 2), myMuscleList.get ("long"));
      createExciter ("long_s", new Point3d (-l / 2, -r, r / 2), new Point3d (
         l / 2, r, r), myMuscleList.get ("long"));

      // createGroup ("long", "", new String[]{""});
      createGroup ("radial", "", new String[] { "" });
      createGroup ("circ", "", new String[] { "" });

   }

   private Point findPoint (Point3d pt) {
      for (FemNode3d n : getNodes()) {
         if (isClose (pt, n))
            return n;
      }
      for (FemMarker m : markers()) {
         if (isClose (pt, m))
            return m;
      }
      return createAndAddMarker (pt);
   }

   int numCreatedMarkers = 0;

   private Point createAndAddMarker (Point3d pnt) {
      numCreatedMarkers++;
      // add the marker to the model
      FemElement3d elem = findContainingElement (pnt);
      if (elem == null) {
         /*
          * project pnt to nearest fem element -- not used b/c of styloglossus
          * System.out.println("containing element null"); Point3d newLoc = new
          * Point3d(); elem = tongue.findNearestElement (newLoc, pnt); pnt.set
          * (newLoc); FemMarker marker = new FemMarker (elem, pnt);
          * tongue.addMarker (marker, elem); return marker;
          */

         FemNode3d fixedNode = new FemNode3d (pnt);
         fixedNode.setDynamic (false);
         addNode (fixedNode);
         return fixedNode;
      }
      else {
         FemMarker marker = new FemMarker (elem, pnt);
         addMarker (marker, elem);
         return marker;
      }
   }

   Vector3d dist = new Vector3d();

   public boolean isClose (Point3d pos, Point pt) {
      dist.sub (pos, pt.getPosition());
      return (dist.norm() < toldist);
   }

   public void createBundle (
      String name, Point3d emin, Point3d emax, String prefix) {
      Point3d pmin = new Point3d();
      Point3d pmax = new Point3d();

      MuscleBundle newbundle = new MuscleBundle (name);
      addMuscleBundle (newbundle);
      RenderProps.setLineWidth (newbundle, 4);

      emin.negate(); // great than check -- use minus minimum

      for (Muscle f : muscleFibres) {
         pmin.set (Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
         pmax.set (-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE);
         f.updateBounds (pmin, pmax);

         pmin.negate(); // great than check -- use minus minimum
         if (f.getName().startsWith (prefix) && emax.greaterEquals (pmax)
         && emin.greaterEquals (pmin))
            newbundle.addFibre (f);
      }

   }

   public void addBundles() {
      double d_inc, d = -l / 2;
      String[] long_ap_segments = new String[] { "a", "m", "p" };
      double[][] ap_dists =
         new double[][] {
                         { d, d + (2 * (double)l / nl) },
                         { d + (2 * (double)l / nl) - eps,
                          d + (5 * (double)l / nl) },
                         { d + (5 * (double)l / nl) - eps, -d } };

      int i = 0;
      for (String ap : long_ap_segments) {
         createBundle ("long_l" + ap, new Point3d (ap_dists[i][0], -r / 2, -r
         / 2 + eps), new Point3d (ap_dists[i][1], -r / 2, r / 2 - eps), "long");
         createBundle ("long_r" + ap, new Point3d (ap_dists[i][0], r / 2, -r
         / 2 + eps), new Point3d (ap_dists[i][1], r / 2, r / 2 - eps), "long");
         i++;
      }

      i = 0;
      for (String ap : long_ap_segments) {
         createBundle ("long_i" + ap, new Point3d (
            ap_dists[i][0], -r / 2, -r / 2), new Point3d (
            ap_dists[i][1], r / 2, -r / 2), "long");
         createBundle ("long_s" + ap, new Point3d (
            ap_dists[i][0], -r / 2, r / 2), new Point3d (
            ap_dists[i][1], r / 2, r / 2), "long");
         i++;
      }

      String[] trans_ap_segments;
      if (useFullAnteriorPosteriorMuscles)
      	trans_ap_segments = new String[] { "a", "am","ma", "m", "mp","pm","p","fixed"};
      else
	 trans_ap_segments = new String[] { "a", "p" };
      
      d = -l / 2;
      d_inc = l / (double)trans_ap_segments.length;
      for (String ap : trans_ap_segments) {
         createBundle ("vert_" + ap, new Point3d (d, -r, -r), new Point3d (d
         + d_inc, r, r), "vert");
         createBundle ("horz_" + ap, new Point3d (d, -r, -r), new Point3d (d
         + d_inc, r, r), "horz");
         d += d_inc;
      }

      createGroup ("long", "_l", new String[] { "_lp", "_lm", "_la" });
      createGroup ("long", "_r", new String[] { "_rp", "_rm", "_ra" });
      createGroup ("long", "_i", new String[] { "_ip", "_im", "_ia" });
      createGroup ("long", "_s", new String[] { "_sp", "_sm", "_sa" });
      createGroup ("long", "", new String[] { "_l", "_r", "_i", "_s" });
      createGroup ("vert", "", new String[] { "_p", "_a" });
      createGroup ("horz", "", new String[] { "_p", "_a" });

   }

   public void createExciter (
      String name, Point3d emin, Point3d emax, MuscleBundle b) {
      Point3d pmin = new Point3d();
      Point3d pmax = new Point3d();

      MuscleExciter ex = new MuscleExciter (name);
      addMuscleExciter (ex);

      emin.negate(); // great than check -- use minus minimum

      for (Muscle f : b.getFibres()) {
         pmin.set (Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
         pmax.set (-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE);
         f.updateBounds (pmin, pmax);

         pmin.negate(); // great than check -- use minus minimum
         if (emax.greaterEquals (pmax) && emin.greaterEquals (pmin))
            ex.addTarget (f, 1.0);
      }

   }

   public void addExciters() {
      double d_inc, d = -l / 2;
      String[] long_ap_segments = new String[] { "a", "m", "p" };
      double[][] ap_dists;

      if (nl == 7)
         ap_dists =
            new double[][] {
                            { d, d + (2 * (double)l / nl) },
                            { d + (2 * (double)l / nl) - eps,
                             d + (5 * (double)l / nl) },
                            { d + (5 * (double)l / nl) - eps, -d } };
      else if (nl == 3)
         ap_dists =
            new double[][] {
                            { d, d + ((double)l / nl) },
                            { d + ((double)l / nl) - eps,
                             d + (2 * (double)l / nl) },
                            { d + (2 * (double)l / nl) - eps, -d } };
      else {
         ap_dists = new double[long_ap_segments.length][2];
         for (int i = 0; i < long_ap_segments.length; i++) {
            ap_dists[i][0] = d;
            d -= (double)l / nl;
            ap_dists[i][1] = d;
         }
      }

      int i = 0;
      for (String ap : long_ap_segments) {
         createExciter (
            "long_l" + ap, new Point3d (ap_dists[i][0], -r / 2, -r),
            new Point3d (ap_dists[i][1], -r / 2, r), myMuscleList.get ("long"));
         createExciter (
            "long_r" + ap, new Point3d (ap_dists[i][0], r / 2, -r),
            new Point3d (ap_dists[i][1], r / 2, r), myMuscleList.get ("long"));
         createExciter (
            "long_i" + ap, new Point3d (ap_dists[i][0], -r, -r / 2),
            new Point3d (ap_dists[i][1], r, -r / 2), myMuscleList.get ("long"));
         createExciter (
            "long_s" + ap, new Point3d (ap_dists[i][0], -r, r / 2),
            new Point3d (ap_dists[i][1], r, r / 2), myMuscleList.get ("long"));
         // createExciter ("vert_"+ap, new Point3d(ap_dists[i][0],-r,-r), new
         // Point3d(ap_dists[i][1],r,r), myMuscleList.get ("vert"));
         // createExciter ("horz_"+ap, new Point3d(ap_dists[i][0],-r,-r), new
         // Point3d(ap_dists[i][1],r,r), myMuscleList.get ("horz"));
         i++;
      }

      String[] trans_ap_segments = new String[] { "a", "p" };
      d = -l / 2;
      d_inc = l / (double)trans_ap_segments.length;
      for (String ap : trans_ap_segments) {
         createExciter ("vert_" + ap, new Point3d (d, -r, -r), new Point3d (d
         + d_inc, r, r), myMuscleList.get ("vert"));
         createExciter ("horz_" + ap, new Point3d (d, -r, -r), new Point3d (d
         + d_inc, r, r), myMuscleList.get ("horz"));
         d += d_inc;
      }

      createGroup ("long", "_l", new String[] { "_lp", "_lm", "_la" });
      createGroup ("long", "_r", new String[] { "_rp", "_rm", "_ra" });
      createGroup ("long", "_i", new String[] { "_ip", "_im", "_ia" });
      createGroup ("long", "_s", new String[] { "_sp", "_sm", "_sa" });
      createGroup ("long", "", new String[] { "" });
      createGroup ("vert", "", new String[] { "" });
      createGroup ("horz", "", new String[] { "" });
   }

   public void addSimpleExciters() {
      // createExciter ("long_l", new Point3d(-l/2,-r/2,-r), new
      // Point3d(l/2,-r/2,r), myMuscleList.get ("long"));
      // createExciter ("long_r", new Point3d(-l/2,r/2,-r), new
      // Point3d(l/2,r/2,r), myMuscleList.get ("long"));
      createExciter ("long_i", new Point3d (-l / 2, -r, -r / 2), new Point3d (
         l / 2, r, -r / 2), myMuscleList.get ("long"));
      createExciter ("long_s", new Point3d (-l / 2, -r, r / 2), new Point3d (
         l / 2, r, r / 2), myMuscleList.get ("long"));

      // createGroup ("long", "", new String[]{""});
      createGroup ("vert", "", new String[] { "" });
      createGroup ("horz", "", new String[] { "" });
   }

   public void createGroup (String name, String[] exTargets) {
      createGroup ("", name, exTargets);
   }

   public void createGroup (String prefix, String name, String[] exTargets) {
      MuscleExciter ex = new MuscleExciter (prefix + name);
      for (String targetName : exTargets) {
         ExcitationComponent target =
            getMuscleExciters().get (prefix + targetName);
         if (target == null) // target not an exciter -- search bundles
            target = myMuscleList.get (prefix + targetName);

         if (target != null) {
            ex.addTarget (target, 1.0);
         }

      }
      if (ex.getTargetView().size() > 0)
         addMuscleExciter (ex);
   }

   public ControlPanel exciterPanel;

   public void createExcitersPanel (String names[]) {
      exciterPanel = new ControlPanel ("Exciters", "LiveUpdate");
      for (int i = 0; i < names.length; i++) {
         LabeledComponentBase c =
            exciterPanel.addWidget (names[i], this, "exciters/" + names[i]
            + ":excitation", 0.0, 1.0);
         if (c != null)
            c.setLabelFontColor (Color.getHSBColor (
               (float)i / names.length, 1f, 1f));
      }

      exciterPanel.pack();
      exciterPanel.setVisible (true);
   }

   public void colorExciters (String names[], boolean createPanel) {
      for (int i = 0; i < names.length; i++) {
         MuscleExciter e = getMuscleExciters().get (names[i]);
         if (e != null) {
            colorExciter (
               e, Color.getHSBColor ((float)i / names.length, 1f, 1f));
         }
      }

      if (createPanel)
         createExcitersPanel (names);
   }

   public void colorExciter (MuscleExciter e, Color color) {
      for (ExcitationComponent target : e.getTargetView()) {
         if (MuscleBundle.class.isAssignableFrom (target.getClass())) {
            RenderProps.setLineColor ((MuscleBundle)target, color);
         }
         else if (Muscle.class.isAssignableFrom (target.getClass())) {
            RenderProps.setLineColor ((Muscle)target, color);
         }
         else if (MuscleExciter.class.isAssignableFrom (target.getClass())) {
            colorExciter ((MuscleExciter)target, color);
         }
      }
   }

   public ControlPanel bundlesPanel;

   public void createBundlesPanel (String names[]) {
      bundlesPanel = new ControlPanel ("Bundles", "LiveUpdate");
      for (int i = 0; i < names.length; i++) {
         LabeledComponentBase c =
            bundlesPanel.addWidget (names[i], this, "bundles/" + names[i]
            + ":excitation", 0.0, 1.0);
         if (c != null)
            c.setLabelFontColor (Color.getHSBColor (
               (float)i / names.length, 1f, 1f));
      }
      bundlesPanel.pack();
      bundlesPanel.setVisible (true);
   }

   public ArrayList<String> labels = new ArrayList<String>();
   public void colorBundles (String names[], boolean createPanel) {
      for (int i = 0; i < names.length; i++) {
         MuscleBundle b = myMuscleList.get (names[i]);
         if (b != null) {
            RenderProps.setLineColor (b, Color.getHSBColor ((float)i
            / names.length, 1f, 1f));
            labels.add(names[i]);
         }
      }

      if (createPanel)
         createBundlesPanel (names);
   }

   public ControlPanel getMusclePanel() {
      return bundlesPanel;
      // return exciterPanel;
   }

   public boolean isUsingExciters() {
      return useExcitersP;
   }

   public double getTargetPointRadius() {
      double l = getXdirLength();
      return l / nl / 10.0;
   }

}
