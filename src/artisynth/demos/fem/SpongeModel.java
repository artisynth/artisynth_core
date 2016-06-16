package artisynth.demos.fem;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import maspack.geometry.PolygonalMesh;
import maspack.matrix.Point3d;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.render.Renderer.LineStyle;
import artisynth.core.femmodels.FemElement3d;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.femmodels.HexElement;
import artisynth.core.femmodels.MuscleBundle;
import artisynth.core.mechmodels.Muscle;
import artisynth.core.femmodels.FemMuscleModel;
import artisynth.core.femmodels.TetElement;
import artisynth.core.gui.NumericProbePanel;
import artisynth.core.mechmodels.Muscle;
import artisynth.core.mechmodels.MechSystemSolver.Integrator;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.util.TimeBase;

public class SpongeModel extends FemMuscleModel {
   int nn = 1;

   int numX = nn * 4;

   int numY = nn * 2;

   int numZ = nn * 2;

   double len = 0.1;

   double thining = 1;

   double stiffness = 10.0;

   public double maxMuscleForce = len * 100; // Newtons

   public int numMuscles;

   public SpongeModel() throws IOException {
      super (null);
   }

   public SpongeModel (String name) throws IOException {
      super (name);
      initializeModel();
      fixNodes();
      addMuscles();
   }

   public ArrayList<ModelComponent> createTargetList() {
      int targetIndices[] = new int[] { 4, 9, 14, 19, 24, 29, 34, 39, 44 }; // 4x2x2
      // sponge
      // 1,7}; // 1x1x1 sponge
      // 8, 11}; // 2x1x1 sponge

      ArrayList<ModelComponent> targets = new ArrayList<ModelComponent>();
      for (int i = 0; i < targetIndices.length; i++)
         targets.add (myNodes.get (targetIndices[i]));

      return targets;
   }

   protected void addMuscles() {
      // add muscles in x-dir
      numMuscles = (numY + 1) * (numZ + 1);

      for (int i = 0; i < numMuscles; i++) {
         addMuscleGroup ("g" + String.valueOf (i), (i * (numX + 1)));
      }
   }

   public RenderProps getNewMuscleProps() {
      RenderProps props = createRenderProps();
      int idx =
         (getMuscleBundles().size() - 1) % NumericProbePanel.colorList.length;
      props.setLineColor (NumericProbePanel.colorList[idx]);
      props.setLineRadius (len / 20);
      props.setLineStyle (LineStyle.SPINDLE);
      return props;
   }

   Random r = new Random();

   int muscleCount = 0;

   protected void addMuscleGroup (String name, int origin) {
      MuscleBundle b = new MuscleBundle (name);
      for (int i = 0; i < numX; i++) {
         addMuscle (b, origin + i, origin + i + 1);
      }
      b.setFibresActive (true);
      addMuscleBundle (b);
      b.setRenderProps (getNewMuscleProps());
   }

   protected void addMuscle (MuscleBundle b, int i0, int i1) {
      Muscle m = new Muscle();
      m.setFirstPoint (getNode (i0));
      m.setSecondPoint (getNode (i1));
      m.setConstantMuscleMaterial(maxMuscleForce, 1);
      b.addFibre (m);
   }

   protected void addSingleMuscle() {
      Muscle m = new Muscle();
      m.setConstantMuscleMaterial(1, 1);
      m.setFirstPoint (getNode (r.nextInt (numNodes())));
      m.setSecondPoint (getNode (r.nextInt (numNodes())));

      MuscleBundle b = new MuscleBundle ();
      b.addFibre (m);
      b.setFibresActive (true);
      addMuscleBundle (b);

      RenderProps props = createRenderProps();
      props.setLineColor (NumericProbePanel.colorList[getMuscleBundles().size()]);
      props.setLineRadius (len / 20);
      props.setLineStyle (LineStyle.SPINDLE);
      m.setRenderProps (props);
   }

   protected void fixNodes() {
      double leftmostPos = myNodes.get (0).getPosition().x;
      for (FemNode3d n : myNodes) {
         if (n.getPosition().x == leftmostPos)
            n.setDynamic (false);
      }
   }

   protected void initializeModel() throws IOException {

      createGeometry ("spongeMesh", numX * len, numY * len, numZ * len
      / thining, numX, numY, numZ, 1);

      RenderProps props = createRenderProps();
      props.setLineWidth (2);
      props.setLineColor (Color.GRAY);
      props.setPointStyle (Renderer.PointStyle.SPHERE);
      props.setAlpha (1);
      props.setPointRadius (len / 20);
      props.setPointColor (Color.GREEN);
      setRenderProps (props);

      setGravity (0, 0, 0);
      setDensity (100);
//      setPoissonsRatio (0.499);
//      setYoungsModulus (4000);
      setLinearMaterial (4000, 0.499, true);
      setParticleDamping (4);
      setStiffnessDamping (0.2);

      setImplicitIterations (100);
      setImplicitPrecision (0.001);
      setMaxStepSize (0.01);
      setIntegrator (Integrator.BackwardEuler);
   }

   public void createGeometry (
      String name, double widthX, double widthY, double widthZ, int numX,
      int numY, int numZ, double nodeMass) {
      if (numX < 1 || numY < 1 || numZ < 1) {
         throw new IllegalArgumentException (
            "number of elements in each direction must be >= 1");
      }
      // create all the particles
      double dx = 1.0 / numX;
      double dy = 1.0 / numY;
      double dz = 1.0 / numZ;
      Point3d p = new Point3d();

      for (int k = 0; k <= numZ; k++) {
         for (int j = 0; j <= numY; j++) {
            for (int i = 0; i <= numX; i++) {
               p.x = widthX * (-0.5 + i * dx);
               p.y = widthY * (-0.5 + j * dy);
               p.z = widthZ * (-0.5 + k * dz);
               addNode (new FemNode3d (p));
               // getNode(numNodes()-1).setName(Integer.toString(numNodes()-1));
            }
         }
      }
      // PolygonalMesh mesh = new PolygonalMesh();

      // create all the elements
      int wk = (numX + 1) * (numY + 1);
      int wj = (numX + 1);
      FemNode3d n0, n1, n2, n3, n4, n5, n6, n7;
      for (int i = 0; i < numX; i++) {
         for (int j = 0; j < numY; j++) {
            for (int k = 0; k < numZ; k++) {
               n0 = getNodes().get ((k + 1) * wk + j * wj + i);
               n1 = getNodes().get ((k + 1) * wk + j * wj + i + 1);
               n2 = getNodes().get ((k + 1) * wk + (j + 1) * wj + i + 1);
               n3 = getNodes().get ((k + 1) * wk + (j + 1) * wj + i);
               n4 = getNodes().get (k * wk + j * wj + i);
               n5 = getNodes().get (k * wk + j * wj + i + 1);
               n6 = getNodes().get (k * wk + (j + 1) * wj + i + 1);
               n7 = getNodes().get (k * wk + (j + 1) * wj + i);
               TetElement[] elems =
                  TetElement.createCubeTesselation (
                     n0, n1, n2, n3, n4, n5, n6, n7,
                     /* even= */(i + j + k) % 2 == 0);
               //HexElement he = new HexElement (n0, n1, n2, n3, n4, n5, n6, n7);
               for (FemElement3d e : elems) {
                  addElement (e);
                  //he.addTetElement ((TetElement)e);
               }
               // add hex element
               // he.setTetElements(elems);
               //hexElements.add (he);
            }
         }
      }
      // TODO create outer Mesh here.
      // mesh.setFixed(false);
      // setSurfaceMesh(mesh);

      RenderProps.setShading (this, Renderer.Shading.SMOOTH);
      RenderProps.setFaceStyle (this, Renderer.FaceStyle.FRONT_AND_BACK);
      RenderProps.setFaceColor (this, Color.BLUE);
      RenderProps.setAlpha (this, 0.9);
      RenderProps.setVisible (this, true);

      invalidateStressAndStiffness();
   }

   private static int addVertex (PolygonalMesh mesh, FemNode3d n) {
      int index = -1;
      index = mesh.getVertices().indexOf (n.getPosition());
      if (index == -1) {
         index =
            mesh.getVertices().indexOf (
               mesh.addVertex (n.getPosition(), true));
      }
      return index;
   }

}
