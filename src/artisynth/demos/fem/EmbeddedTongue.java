package artisynth.demos.fem;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import artisynth.core.femmodels.FemElement3d;
import artisynth.core.femmodels.FemMuscleModel;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.femmodels.IntegrationPoint3d;
import artisynth.core.femmodels.MuscleBundle;
import artisynth.core.femmodels.MuscleBundle.DirectionRenderType;
import artisynth.core.femmodels.MuscleElementDesc;
import artisynth.core.femmodels.EmbeddedFem;
import artisynth.core.gui.ControlPanel;
import artisynth.core.materials.BlemkerMuscle;
import artisynth.core.materials.LinearMaterial;
import artisynth.core.materials.MooneyRivlinMaterial;
import artisynth.core.mechmodels.ExcitationComponent;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.MuscleExciter;
import artisynth.core.modelbase.RenderableComponentList;
import artisynth.core.probes.NumericInputProbe;
import artisynth.core.renderables.HudPrintStream;
//import artisynth.core.util.ArtisynthDataManager;
import artisynth.core.util.ArtisynthPath;
import artisynth.core.workspace.DriverInterface;
import artisynth.core.workspace.RootModel;
//import artisynth.tools.data.ResearchCloudDataManager;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.Polyline;
import maspack.geometry.PolylineMesh;
import maspack.geometry.io.WavefrontReader;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.properties.Property;
import maspack.render.RenderProps;
import maspack.render.Renderer.FaceStyle;
import maspack.render.Renderer.LineStyle;
import maspack.render.Renderer.PointStyle;
import maspack.render.Renderer.Shading;
import maspack.render.GL.GLViewer;
import maspack.util.RandomGenerator;
import maspack.widgets.DoubleFieldSlider;

/**
 * Demonstrates an embedded tongue model with muscle activations
 *
 * @author Antonio
 */
public class EmbeddedTongue extends RootModel {

    public String[] muscleExciters = { "GGP", "GGM", "GGA", "STY", "GH", "MH",
                                     "HG", "VERT", "TRANS", "IL", "SL" };

   private static final Color[] myTongueMuscleColors =
      new Color[] {
                   new Color(255, 0, 0), new Color(0f, 0.5f, 0f),
                   new Color(0, 0, 255),
                   new Color(0, 255, 255), new Color(255, 0, 255),
                   new Color(255, 140, 0), new Color(255, 150, 150),
                   new Color(138, 43, 226), new Color(255, 222, 173),
                   new Color(85, 107, 47), new Color(205, 92, 92),
                   new Color(255, 218, 185), };

   protected FemMuscleModel tongue = new FemMuscleModel("tongue");
   protected MechModel model = new MechModel("tongueModel");

   @Override
   public void build (String[] args) throws IOException {
      super.build (args);
   
      RandomGenerator.setSeed (0x1234);

      String localDir = ArtisynthPath.getSrcRelativePath(
         this, "data/tongue/");
      String tongueMeshFilename = "tongueGeometry.obj";
      String tongueFibresFilename = "tongueFibres.obj";
      String activationFilename = "tongue_act_in.txt";
      
      //ArtisynthDataManager grabber = ResearchCloudDataManager.createDataManager ("tgz:artisynth/models/embedded/demos/tongue/tongue.tar.gz!", localDir);
      HudPrintStream hud = new HudPrintStream();
      hud.locateTopLeft();
      hud.setNumDisplayLines(1);
      addRenderable(hud);

      try {
         File tongueMeshFile = new File (localDir + tongueMeshFilename);
         PolygonalMesh tongueMesh = new PolygonalMesh(tongueMeshFile);
         MeshFactory.fillHoles(tongueMesh);
         tongueMesh.triangulate();
         
         EmbeddedFem.createVoxelizedFem(tongue, tongueMesh, RigidTransform3d.IDENTITY, 20, -1);
         tongue.addMesh("embedded_surface", tongueMesh);
         tongue.setName("tongue");

         MooneyRivlinMaterial moonMat =
            new MooneyRivlinMaterial(1500, 0, 0, 0, 0, 150000);
         LinearMaterial linMat = new LinearMaterial(6912, 0.49);
         linMat.setCorotated(true);

         tongue.setMaterial(moonMat);
         tongue.setStiffnessDamping(0);
         tongue.setParticleDamping(10);

         BlemkerMuscle mmat = new BlemkerMuscle();
         // taken from StableFemMuscleTongue
         mmat.setOptLambda(1);
         mmat.setMaxLambda(1.5);
         mmat.setMaxStress(1.5e4);
         tongue.setMuscleMaterial(mmat);

         File tongueFibreFile = new File (localDir + tongueFibresFilename);
         WavefrontReader wfr = new WavefrontReader(tongueFibreFile);

         wfr.parse();

         String[] bundleNames = wfr.getPolylineGroupNames();
         ArrayList<PolylineMesh> fibreMeshes =
            new ArrayList<PolylineMesh>(bundleNames.length);

         for (String bundleName : bundleNames) {
            PolylineMesh bundleMesh = new PolylineMesh();

            wfr.setMesh (bundleMesh, bundleName);
            //bundleMesh.setName(bundleName);

            MuscleBundle bundle = new MuscleBundle(bundleName);
            addMuscleBundleElems(tongue, bundleMesh, 0.05, bundle);
            bundle.setMaxForce(0.1);

            tongue.addMuscleBundle(bundle);
            fibreMeshes.add(bundleMesh);
         }
         addBilateralExciters(tongue);

         model.addModel(tongue);

         for (FemNode3d node : tongue.getNodes()) {
            if (node.getRestPosition().z < 0.0677) {
               node.setDynamic(false);
            }
         }

      } catch (IOException e) {
         e.printStackTrace();
      }

      addModel(model);
      
      File actFile = new File (localDir + activationFilename);
      addInputProbes(actFile);

      setMyRenderProps();

   }
   
   public static void addMuscleBundleElems(FemMuscleModel muscle,
      PolylineMesh bundleMesh, double tol, MuscleBundle bundle) {

      Point3d proj = new Point3d();
      Vector3d dir = new Vector3d();
      Point3d pos = new Point3d();

      for (FemElement3d elem : muscle.getElements()) {
         boolean add = false;
         Vector3d dirs[] = new Vector3d[elem.numIntegrationPoints()];
         IntegrationPoint3d[] ipnts = elem.getIntegrationPoints();
         for (int i = 0; i < elem.numIntegrationPoints(); i++) {
            ipnts[i].computePosition(pos, elem);
            double d = projectToPolyline(pos, proj, dir, bundleMesh);
            if (d < tol) {
               add = true;
               dirs[i] = dir;
               dirs[i].normalize();
            }
         }

         if (add) {
            MuscleElementDesc desc =
               new MuscleElementDesc(elem, null);
            desc.setDirections(dirs);
            bundle.addElement(desc);
         }
      }

   }

   private static double projectToPolyline(Point3d pos, Point3d proj,
      Vector3d dir,
      PolylineMesh poly) {

      double dMin = Double.POSITIVE_INFINITY;
      Point3d p1, p2;
      Vector3d v1 = new Vector3d();
      Vector3d v2 = new Vector3d();
      double l2 = 0;
      double t = 0;

      for (Polyline line : poly.getLines()) {
         for (int i = 0; i < line.numVertices() - 1; i++) {
            // project point onto line segment
            p1 = line.getVertex(i).getPosition();
            p2 = line.getVertex(i + 1).getPosition();
            v1.sub(p1, p2);
            v2.sub(pos, p2);
            l2 = v1.normSquared();
            t = v1.dot(v2) / l2;

            if (t < 0) {
               l2 = pos.distance(p2);
               if (l2 < dMin) {
                  dMin = l2;
                  proj.set(p2);
                  dir.set(v1);
               }
            } else if (t > 1) {
               l2 = pos.distance(p1);
               if (l2 < dMin) {
                  dMin = l2;
                  proj.set(p1);
                  dir.set(v1);
               }
            } else {
               v2.scaledAdd(t, v1, p2);
               l2 = pos.distance(v2);
               if (l2 < dMin) {
                  dMin = l2;
                  proj.set(v2);
                  dir.set(v1);
               }
            }
         }
      }
      return Math.sqrt(dMin);
   }

   private void setMyRenderProps() {

      RenderableComponentList<MuscleBundle> bundles =
         ((FemMuscleModel)tongue).getMuscleBundles();

      RenderProps.setPointRadius(tongue.getNodes(), 0.001);
      RenderProps.setPointStyle(tongue.getNodes(), PointStyle.SPHERE);

      RenderProps.setLineWidth(bundles, 2);
      RenderProps.setLineStyle(bundles, LineStyle.LINE);
      RenderProps.setLineWidth(bundles, 3);
      RenderProps.setVisible(bundles, false);
      
      RenderProps.setVisible(tongue.getElements(), true);
      RenderProps.setVisible(tongue.getNodes(), false);

      tongue.setDirectionRenderLen(0.001);
      tongue.setDirectionRenderType(DirectionRenderType.INTEGRATION_POINT);
      RenderProps.setAlpha(tongue, 1.0);
      RenderProps.setAlpha(tongue.getNodes(), 1);
      RenderProps.setShading(tongue, Shading.SMOOTH);

      // make muscle bundle visibility inherited
      for (MuscleBundle b : bundles) {
         RenderProps.setLineColor(b, getTongueMuscleColor(b.getNumber()));
         b.setElementWidgetSize(0);
      }
      RenderProps tongueProps = new RenderProps();
      tongueProps.setFaceColor(new Color(.8f, .5f, .5f));
      tongueProps.setLineColor(new Color(.2f, .2f, .2f)); // dark grey
      tongueProps.setFaceStyle(FaceStyle.FRONT_AND_BACK);
      tongueProps.setDrawEdges(false);
      RenderProps.setFaceColor(tongue, tongueProps.getFaceColor());
      tongue.getMeshComp("embedded_surface").setRenderProps(tongueProps);
   }

   private static Color getTongueMuscleColor(int idx) {
      int valididx = idx % myTongueMuscleColors.length;
      // 21 muscle bundles, therefore use bright/dark colors for right/left pair
      if (valididx % 2 == 1) { // left side muscle, return color
         return myTongueMuscleColors[(valididx - 1) / 2];
      } else { // right-side, return darker color
         return myTongueMuscleColors[valididx / 2].darker();
      }
   }

   protected void addExciters(FemMuscleModel muscle) {
      addBilateralExciters(muscle);
   }

   public static void addBilateralExciters(FemMuscleModel muscle) {

      // add exciters if 21 muscle groups: 20 paired, 1 unpaired
      if (muscle.getMuscleBundles().size() == 21) {
         for (int i = 0; i < 20; i += 2) {
            MuscleBundle left = muscle.getMuscleBundles().get(i);
            MuscleBundle right = muscle.getMuscleBundles().get(i + 1);
            String[] name = left.getName().split("_");
            MuscleExciter ex = new MuscleExciter(name[0]);
            ex.addTarget(left, 1.0);
            ex.addTarget(right, 1.0);
            muscle.addMuscleExciter(ex);
         }
         // add exciter for unpaired muscle group (SL)
         MuscleBundle unpaired = muscle.getMuscleBundles().get(20);
         MuscleExciter ex = new MuscleExciter(unpaired.getName());
         ex.addTarget(unpaired, 1.0);
         muscle.addMuscleExciter(ex);
      }

   }

   @Override
   public void attach(DriverInterface driver) {

      if (myControlPanels.size() == 0 && tongue != null) {
         createMusclePanel(this, tongue);
      }
      
      GLViewer viewer = driver.getViewer ();
      if (viewer != null) {
         viewer.setBackgroundColor (Color.WHITE);
      }
         

   }

   public static ControlPanel createMusclePanel(RootModel root,
      FemMuscleModel muscle) {
      ControlPanel controlPanel =
         new ControlPanel(muscle.getName() + " Muscles",
            "LiveUpdate");
      controlPanel.setScrollable(true);

      addExcitersToPanel(controlPanel, muscle);

      // controlPanel.setVisible(true); -- set be set visible by addControlPanel
      root.addControlPanel(controlPanel);
      return controlPanel;
   }

   private static void
      addExcitersToPanel(ControlPanel panel, FemMuscleModel muscle) {
      for (MuscleExciter mex : muscle.getMuscleExciters()) {
         DoubleFieldSlider slider = (DoubleFieldSlider)panel.addWidget(mex
            .getName(), muscle, "exciters/" + mex.getNumber() + ":excitation",
            0, 1);
         slider.setRoundingTolerance(0.00001);
         slider.getLabel().setForeground(
            getTongueMuscleExciterColor(mex.getNumber()));
      }

   }

   private static Color getTongueMuscleExciterColor(int idx) {
      /* assume exciters are bilateral muscle groups */
      return getTongueMuscleColor(idx * 2);
   }

   public void addInputProbes(File actFile) {

      NumericInputProbe inProbe;

      // activations
      if (actFile != null) {
         inProbe = createTongueMuscleInputProbe(
            muscleExciters, actFile.getAbsolutePath());
         inProbe.setActive(true);
         addInputProbe(inProbe);
      }
   }

   public NumericInputProbe createTongueMuscleInputProbe(String[] muscles,
      String fileName) {

      NumericInputProbe inProbe = new NumericInputProbe();
      inProbe.setAttachedFileName(fileName);
      inProbe.setModel(this);

      Property[] props = new Property[muscles.length];

      for (int i = 0; i < muscles.length; i++) {
         ExcitationComponent ex = tongue.getMuscleExciters().get(muscles[i]);
         props[i] = ex.getProperty("excitation");
      }
      inProbe.setInputProperties(props);
      inProbe.setName("tongue muscles");
      inProbe.setDefaultDisplayRange(0, 1);
      inProbe.setUpdateInterval(0.01);
      inProbe.setStartTime(0);
      inProbe.setStopTime(10);
      inProbe.setActive(false); // default to inactive
      try {
         inProbe.load();
      } catch (IOException e) {
         e.printStackTrace();
      }

      return inProbe;

   }

}
