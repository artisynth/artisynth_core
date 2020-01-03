package artisynth.demos.test;

import java.awt.Color;

import artisynth.core.femmodels.EmbeddedFem;
import artisynth.core.femmodels.FemFactory;
import artisynth.core.femmodels.FemFactory.FemElementType;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.femmodels.FemMeshComp;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.materials.LinearMaterial;
import artisynth.core.mechmodels.MechModel;
import artisynth.demos.fem.FemBeam3d;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.Vertex3d;
import maspack.matrix.AxisAngle;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.matrix.Vector3i;
import maspack.render.RenderProps;
import maspack.util.RandomGenerator;

/**
 * Tests embedded FEM models against a tetgen reference model. From left to
 * right, the three embedded models are (a) created with hex elements,
 * (b) created with tet elements with mass and stiffness adjustment,
 * (c) created with hex elements with mass and stiffness adjustment.
 * 
 * @author Antonio
 */
public class EmbeddedTest extends FemBeam3d {

   private double taper (double z, double height) {
      double x = z/height+0.5;

      double a = 1.5;
      double b = -2.0;
      double c =  0;
      double d =  1.0;

      return ((a*x+b)*x+c)*x + d;
   }

   private FemModel3d createFem (FemElementType type) {

      double height = 1.0;
      double rad = 0.2;
      int nx = 3;

      FemModel3d fem = new FemModel3d();

      PolygonalMesh mesh;
      mesh = MeshFactory.createRoundedCylinder (
         rad, height-2*rad, 24, 16, /*flatBotttom=*/false);

      for (Vertex3d v : mesh.getVertices()) {
         double s = taper (v.pnt.z, height);
         v.pnt.x *= s;
         v.pnt.y *= s;
      }
      mesh.notifyVertexPositionsModified();

      Vector3d pmin = new Vector3d(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
      Vector3d pmax = new Vector3d(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
      mesh.updateBounds (pmin, pmax);
      Vector3d widths = new Vector3d();
      widths.sub (pmax, pmin);
      
      Vector3i res = new Vector3i(nx, nx, 0);
      double dd = widths.x/nx;
      res.z = (int)Math.round (widths.z/dd);
      double margin = dd/2;
      
      switch (type) {
         case Tet: {
            FemFactory.createTetGrid (fem, widths.x, widths.y, widths.z, res.x, res.y, res.z);
            Vector3d c = new Vector3d();
            c.interpolate (pmin, 0.5, pmax);
            fem.transformGeometry (new RigidTransform3d(c, AxisAngle.IDENTITY));
            EmbeddedFem.removeOutsideElements (fem, mesh, margin);
            break;
         }
         case Hex:
         default: {
            EmbeddedFem.createVoxelizedFem(fem, mesh, RigidTransform3d.IDENTITY, res, margin);            
            break;
         }
      }
      

      fem.setDensity (1000.0);
      fem.setStiffnessDamping (0.002);
      fem.setMaterial(new LinearMaterial(30000, 0.33));

      for (FemNode3d n : fem.getNodes()) {
         double z = n.getPosition().z;
         if (z-height/2 > -0.025) {
            n.setDynamic(false);
         }
      }

      FemMeshComp comp = fem.addMesh (mesh);
      comp.setName("uvula");
      setRenderProperties (fem, height);
      fem.setSurfaceRendering (SurfaceRender.Shaded);
      
      RenderProps.setVisible (fem.getSurfaceMeshComp (), false);
      RenderProps.setFaceColor (fem, new Color (1f, 153/255f, 153/255f));
      fem.setElementWidgetSize (0);

      return fem;
   }

   private FemModel3d createTetGen () {

      double height = 1.0;
      double rad = 0.2;

      FemModel3d fem = new FemModel3d();
      fem.setDensity (1000.0);
      fem.setStiffnessDamping (0.002);
      fem.setMaterial(new LinearMaterial(30000, 0.33));

      PolygonalMesh mesh;
      mesh = MeshFactory.createRoundedCylinder (
         rad, height-2*rad, 24, 16, /*flatBottom=*/false);
      mesh.triangulate();

      for (Vertex3d v : mesh.getVertices()) {
         double s = taper (v.pnt.z, height);
         v.pnt.x *= s;
         v.pnt.y *= s;
      }
      mesh.notifyVertexPositionsModified();

      FemFactory.createFromMesh(fem, mesh, 1.4);

      for (FemNode3d n : fem.getNodes()) {
         double z = n.getPosition().z;
         if ( z-height/2 > -0.035) {
            n.setDynamic(false);
         }
      }

      setRenderProperties (fem, height);
      // RenderProps.setVisible (fem.getMeshComp("surface"), false);
      RenderProps.setFaceColor (fem, new Color (1f, 153/255f, 153/255f));

      return fem;
   }

   public void build (String[] args) {

      MechModel mech = new MechModel();
      addModel (mech);

      // for consistent integral sampling when debugging
      RandomGenerator.setSeed (0x1234);

      FemModel3d ideal = createTetGen();
      FemModel3d fem0 = createFem(FemElementType.Hex);
      FemModel3d fem1 = createFem(FemElementType.Tet);
      FemModel3d fem2 = createFem(FemElementType.Hex);

      ideal.transformGeometry (
         new RigidTransform3d ( 0.0, 0, 0, 0, -Math.PI/4, 0));

      fem0.transformGeometry (
         new RigidTransform3d (0.6, 0, 0, 0, -Math.PI/4, 0));

      fem1.transformGeometry (
         new RigidTransform3d (1.20, 0, 0, 0,-Math.PI/4, 0));

      fem2.transformGeometry (
           new RigidTransform3d (1.8, 0, 0, 0,-Math.PI/4, 0));

      mech.addModel(ideal);
      mech.addModel (fem0);
      mech.addModel (fem1);
      mech.addModel (fem2);

      mech.setGravity(0,0,-9.8);

      // adjust mass and stiffness of embedded models
      int nsamps = 10000;
      EmbeddedFem.adjustMassAndStiffness (fem1, (PolygonalMesh)(fem1.getMeshComp("uvula").getMesh()), nsamps);
      EmbeddedFem.adjustMassAndStiffness (fem2, (PolygonalMesh)(fem2.getMeshComp("uvula").getMesh()), nsamps);

   }
}

