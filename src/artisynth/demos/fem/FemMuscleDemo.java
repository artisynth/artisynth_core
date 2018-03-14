package artisynth.demos.fem;

import java.awt.Color;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.widgets.BooleanSelector;
import maspack.widgets.DoubleFieldSlider;
import maspack.widgets.GuiUtils;
import maspack.widgets.PropertyWidget;
import maspack.widgets.ValueChangeEvent;
import maspack.widgets.ValueChangeListener;
import artisynth.core.driver.Main;
import artisynth.core.femmodels.FemElement3d;
import artisynth.core.femmodels.FemFactory;
import artisynth.core.femmodels.FemMarker;
import artisynth.core.femmodels.FemMuscleModel;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.femmodels.MuscleBundle;
import artisynth.core.femmodels.MuscleElementDesc;
import artisynth.core.gui.ControlPanel;
import artisynth.core.gui.FemControlPanel;
import artisynth.core.materials.BlemkerMuscle;
import artisynth.core.materials.GenericMuscle;
import artisynth.core.materials.MooneyRivlinMaterial;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.MechSystemSolver.Integrator;
import artisynth.core.mechmodels.Muscle;
import artisynth.core.mechmodels.Point;
import artisynth.core.modelbase.ComponentList;
import artisynth.core.probes.NumericInputProbe;
import artisynth.core.workspace.RootModel;

public class FemMuscleDemo extends RootModel {

   protected MechModel myModel = new MechModel("FemBeam");
   protected FemMuscleModel tissue;
   protected ControlPanel myControlPanel;
   private double midQuadTerm = 12;

   private static int NONE = 0;
   private static int MIDDLE = 1;
   private static int ALL = 2;

   private int defaultMidElements = NONE;
   private double addMidElementsWithin = 0.01;
   private boolean autoComputeMidDirections = false;
   private boolean addMidMuscle = false;

   public void build (String[] args) throws IOException {
      int xn = 1;
      for (int i=0; i<args.length; i++) {
         if (args[i].equals ("-xn")) {
            if (i == args.length-1) {
               System.out.println (
                  "Warning: option '-xn' needs another argument");
            }
            xn = Integer.valueOf(args[++i]);
         }
      }
      initializeModel(xn);
   }

   protected MuscleBundle createBundle(String name, List<FemElement3d> elems) {

      MuscleBundle bundle = new MuscleBundle(name);
      if (elems != null) {
         for (FemElement3d e : elems) {
            MuscleElementDesc desc = new MuscleElementDesc();
            desc.setElement(e);
            desc.setDirection(Vector3d.X_UNIT);
            bundle.addElement(desc);
         }
      }
      return bundle;
   }

   protected Color getMuscleColor(int i) {
      switch (i) {
         case 0:
            return new Color(0f, 1f, 1f);
         case 1:
            return new Color(0f, .8f, 0f);
         default:
            return new Color(1f, 0, 1f);
      }
   }

   protected void initializeModel(int xn) throws IOException {

      double widthX = 0.09;
      double widthY = 0.03;
      double widthZ = 0.03;
      int numX = xn * 9;
      int numY = xn * 3;
      int numZ = xn * 3;

      tissue = new FemMuscleModel("fem");
      FemFactory.createHexGrid(
         tissue, widthX, widthY, widthZ,
         numX, numY, numZ);
      tissue.setBounds(new Point3d(-widthX, 0, 0), new Point3d(widthX, 0, 0));

      // XXX fix the leftmost nodes
      double EPS = 1e-9;
      double xmin = Double.POSITIVE_INFINITY;
      for (FemNode3d n : tissue.getNodes()) {
         if (n.getPosition().x < xmin) {
            xmin = n.getPosition().x;
         }
      }
      for (FemNode3d n : tissue.getNodes()) {
         if (Math.abs(n.getPosition().x - xmin) < 1e-10) {
            n.setDynamic(false);
         }
      }

      LinkedList<FemElement3d> topElems = new LinkedList<FemElement3d>();
      LinkedList<FemElement3d> midElems = new LinkedList<FemElement3d>();
      LinkedList<FemElement3d> botElems = new LinkedList<FemElement3d>();

      for (FemElement3d e : tissue.getElements()) {
         if (defaultMidElements == ALL) {
            midElems.add(e);
         }
         for (FemNode3d n : e.getNodes()) {
            if (Math.abs(n.getPosition().z - widthZ / 2) < EPS) {
               topElems.add(e);
               break;
            }
            else if (Math.abs(n.getPosition().z + widthZ / 2) < EPS) {
               botElems.add(e);
               break;
            }
            else if (defaultMidElements == MIDDLE &&
               Math.abs(n.getPosition().z - widthZ / (2 * numZ)) < EPS) {
               midElems.add(e);
               break;
            }
         }
      }

      RenderProps.setLineWidth(tissue, 2);
      RenderProps.setLineColor(tissue, Color.PINK);
      RenderProps.setPointStyle(tissue, Renderer.PointStyle.SPHERE);
      RenderProps.setPointRadius(tissue, 0.03);
      RenderProps.setPointColor(tissue, Color.PINK);
      RenderProps.setFaceColor(tissue, Color.PINK.darker());

      GenericMuscle mm = new GenericMuscle();
      mm.setMaxStress(5000);
      BlemkerMuscle bm = new BlemkerMuscle();
      // bm.setMaxStress (5000);
      tissue.setMuscleMaterial(bm);

      MuscleBundle top, mid, bot;

      top = createBundle("top", topElems);
      mid = createBundle("mid", midElems);
      bot = createBundle("bot", botElems);

      int k = 0;
      setBundleRenderProps(top, getMuscleColor(k++));
      if (addMidMuscle) {
         setBundleRenderProps(mid, getMuscleColor(k++));
      }
      setBundleRenderProps(bot, getMuscleColor(k));

      tissue.addMuscleBundle(top);
      if (addMidMuscle) {
         tissue.addMuscleBundle(mid);
      }
      tissue.addMuscleBundle(bot);

      double qt = midQuadTerm;

      addStrand(top, -0.035, -0.005, 0.015, 0.035, -0.005, 0.015, 0, 8);
      addStrand(top, -0.035, 0.005, 0.015, 0.035, 0.005, 0.015, 0, 8);

      if (addMidMuscle) {
         addStrand(mid, -0.035, -0.005, 0.000, 0.035, -0.005, 0.000, qt, 8);
         addStrand(mid, -0.035, 0.005, 0.000, 0.035, 0.005, 0.000, qt, 8);
      }

      addStrand(bot, -0.035, -0.005, -0.015, 0.035, -0.005, -0.015, 0, 8);
      addStrand(bot, -0.035, 0.005, -0.015, 0.035, 0.005, -0.015, 0, 8);

      if (addMidMuscle) {
         if (addMidElementsWithin > 0) {
            mid.addElementsNearFibres(addMidElementsWithin);
         }
         if (autoComputeMidDirections) {
            mid.computeElementDirections();
         }
      }

      tissue.setDirectionRenderLen(0.5);

      RenderProps.setPointRadius(tissue, 0.001);

      tissue.setGravity(0, 0, 0);
      tissue.setDensity(1000);

      tissue.setMaterial(new MooneyRivlinMaterial(1037, 0, 0, 486, 0, 10000));
      // tissue.setPoissonsRatio (0.499);
      // tissue.setYoungsModulus (6912);
      tissue.setParticleDamping(6.22);
      // more stable with 0 stiffness damping ...
      tissue.setStiffnessDamping(0.01); 

      // tissue.setMaxStepSize(100*TimeBase.USEC);
      tissue.setMaxStepSize(0.01);
      tissue.setIntegrator(Integrator.ConstrainedBackwardEuler);
      
      myModel.addModel(tissue);
      addModel(myModel);

//      if (addMidMuscle) {
//         mid.getMacroFibres();
//      }

      for (MuscleBundle b : tissue.getMuscleBundles()) {
         RenderProps.setVisible(b.getFibres(), false);
      }
      for (FemMarker m : tissue.markers()) {
         RenderProps.setVisible(m, false);
      }

      addProbes(tissue);
      createMusclePanel ();

      // int numWays = 20;
      // double res = 0.1;
      // for (int i = 0; i < numWays; i++) {
      // addWayPoint (new WayPoint (TimeBase.secondsToTicks ((i + 1) * res)));
      // }
   }

   protected void addProbes(FemMuscleModel fem) {
      try {

         String[] exciters;
         if (addMidMuscle) {
            exciters = new String[] {
               "bundles/0:excitation",
               "bundles/1:excitation",
               "bundles/2:excitation" };
         }
         else {
            exciters = new String[] {
               "bundles/0:excitation",
               "bundles/1:excitation" };
         }
         NumericInputProbe inprobe =
            new NumericInputProbe(fem, exciters, 0, 5);

         if (addMidMuscle) {
            inprobe.addData(
               new double[] {
                  0, 0, 0, 0, 2.5, 0.1, 0, 0.1, 3.5, 0, 0, 0, 5, 0, 0, 0 },
               NumericInputProbe.EXPLICIT_TIME);
         }
         else {
            inprobe.addData(
               new double[] {
                  0, 0, 0, 2.5, 0.1, 0, 3.5, 0, 0, 5, 0, 0 },
               NumericInputProbe.EXPLICIT_TIME);
         }

         addInputProbe(inprobe);
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   protected void setBundleRenderProps(MuscleBundle bundle, Color color) {
      RenderProps.setLineColor(bundle, color);
      RenderProps.setLineStyle(bundle, Renderer.LineStyle.SPINDLE);
      RenderProps.setLineRadius(bundle, 0.001);
      RenderProps
         .setLineStyle(bundle.getElements(), Renderer.LineStyle.LINE);
      RenderProps.setLineWidth(bundle.getElements(), 3);
      RenderProps.setLineRadius(bundle.getElements(), 0.0010);
      RenderProps.setFaceColor(bundle, color);
   }

   FemNode3d findNearestNode(Point3d pnt) {
      FemNode3d node = null;
      double mind = Double.MAX_VALUE;
      for (FemNode3d n : tissue.getNodes()) {
         double d = n.distance(pnt);
         if (d < mind) {
            mind = d;
            node = n;
         }
      }
      return node;
   }

   /**
    * Adds a strand of muscles interpolated between two points, using FemNodes
    * if they are close enough, or otherwise inserting markers.
    */
   protected void addStrand(
      MuscleBundle bundle,
      double p0x, double p0y, double p0z,
      double p1x, double p1y, double p1z,
      double quadTerm, int numPnts) {

      Point pnt;
      Point prevPnt = null;

      Point3d pos0 = new Point3d(p0x, p0y, p0z);
      Point3d pos1 = new Point3d(p1x, p1y, p1z);

      Point3d pos = new Point3d();
      for (int i = 0; i < numPnts; i++) {
         double s = i / (numPnts - 1.0);
         pos.combine(1 - s, pos0, s, pos1);

         double r = Math.sqrt(pos.x * pos.x + pos.y * pos.y);
         pos.z += quadTerm * r * r;

         pnt = findNearestNode(pos);
         if (pnt.distance(pos) > 0.0001) {
            FemMarker mkr = new FemMarker(pos);
            tissue.addMarker(mkr);
            pnt = mkr;
         }
         if (prevPnt != null) {
            Muscle muscle = new Muscle();
            muscle.setConstantMuscleMaterial(1, 1);
            muscle.setFirstPoint(prevPnt);
            muscle.setSecondPoint(pnt);
            bundle.addFibre(muscle);
         }
         prevPnt = pnt;
      }
   }

   public void createMusclePanel () {
      ControlPanel myControlPanel = new ControlPanel("options", "LiveUpdate");
      FemControlPanel.addMuscleControls(myControlPanel, tissue, myModel);
      
      myControlPanel.addWidget(tissue, "profile");
      
      ComponentList<MuscleBundle> muscles =
         ((FemMuscleModel)tissue).getMuscleBundles();
      
      for (int i = 0; i < muscles.size(); ++i) {
         DoubleFieldSlider slider =
            (DoubleFieldSlider)myControlPanel.addWidget(
               "activation [N per Muscle]", this,
               "models/FemBeam/models/fem/bundles/" + i + ":excitation", 0, 1);
         slider.setRoundingTolerance(0.00001);
         slider.getLabel().setForeground(getMuscleColor(i));
         BooleanSelector checkBox =
            (BooleanSelector)PropertyWidget.create(
               "", muscles.get(i), "renderProps.visible");
         checkBox.addValueChangeListener(new ValueChangeListener() {
            public void valueChange(ValueChangeEvent e) {
               rerender();
            }
         });
         slider.add(checkBox);
      }
      for (int i = 0; i < muscles.size(); ++i) {
         BooleanSelector selector =
            (BooleanSelector)myControlPanel.addWidget(
               "fibres active", this,
               "models/FemBeam/models/fem/bundles/" + i + ":fibresActive");
         selector.getLabel().setForeground(getMuscleColor(i));
         BooleanSelector checkBox =
            (BooleanSelector)PropertyWidget.create(
               "", muscles.get(i).getFibres(), "renderProps.visible");
         checkBox.addValueChangeListener(new ValueChangeListener() {
            public void valueChange(ValueChangeEvent e) {
               rerender();
            }
         });
         selector.add(checkBox);
      }

      addControlPanel(myControlPanel);
   }

   /**
    * {@inheritDoc}
    */
   public String getAbout() {
      return "Demo showing muscle activated elements";
   }

}
