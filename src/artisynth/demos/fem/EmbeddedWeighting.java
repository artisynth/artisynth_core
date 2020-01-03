package artisynth.demos.fem;

import java.awt.Color;

import artisynth.core.femmodels.FemElement3d;
import artisynth.core.femmodels.FemFactory;
import artisynth.core.femmodels.FemFactory.FemElementType;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.femmodels.ScalarSubElemField;
import artisynth.core.femmodels.TetElement;
import artisynth.core.femmodels.EmbeddedFem;
import artisynth.core.materials.FemMaterial;
import artisynth.core.materials.LinearMaterial;
import artisynth.core.materials.ScaledFemMaterial;
import artisynth.core.mechmodels.MechModel;

import maspack.geometry.BVFeatureQuery;
import maspack.geometry.DistanceGrid;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.Vertex3d;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.matrix.Vector3i;
import maspack.render.RenderProps;
import maspack.util.FunctionTimer;
import maspack.util.Logger;
import maspack.util.Logger.LogLevel;
import maspack.util.RandomGenerator;

/**
 * Demonstrates adjusting for empty mass and stiffness within an embedded model 
 *
 * @author Antonio
 */
public class EmbeddedWeighting extends FemBeam3d {

   private static final double EPS = 1e-10;

   private double taper (double z, double height) {
      double x = z/height+0.5;
      
      double a = 1.5;
      double b = -2.0;
      double c =  0;
      double d =  1.0;

      return ((a*x+b)*x+c)*x + d;
   }

   private void getRandomTetCoords (double[] wgts, Vector3d coords) {

      // From Generating Random Points in a Tetrahedron, by C. Rocchini and
      // P. Cignoni, 2001

      double s = RandomGenerator.nextDouble (0, 1);
      double t = RandomGenerator.nextDouble (0, 1);
      double u = RandomGenerator.nextDouble (0, 1);

      if (s+t > 1) {
         s = 1-s;
         t = 1-t;
      }
      double sx = s;
      double tx = t;
      double ux = u;
      if (s+t+u > 1) {
         if (t+u > 1) {
            tx = 1-u;
            ux = 1-s-t;
         }
         else {
            sx = 1-t-u;
            ux = s+t+u-1;
         }
      }
      coords.set (sx, tx, ux);
      wgts[0] = 1-sx-tx-ux;
      wgts[1] = sx;
      wgts[2] = tx;
      wgts[3] = ux;
   }

   /**
    * Accounts for missing mass and stiffness
    * @param tet element to adjust
    * @param mesh mesh that determine inside/outside if sdgrid not available
    * @param fquery query object for inside/outside tests
    * @param sdgrid signed distance grid if available
    * @param nsamps number of samples to use
    */
   public void adjustMassAndStiffness (
      ScalarSubElemField density,
      TetElement tet, PolygonalMesh mesh,
      BVFeatureQuery fquery, DistanceGrid sdgrid, int nsamps) {

      Vector3d coords = new Vector3d();
      Point3d pos = new Point3d();
      double frac = 0;
      double msamp = tet.getDensity()*tet.getRestVolume()/nsamps;
      double[] wgts = new double[4];

      FemNode3d n0 = tet.getNodes()[0];
      FemNode3d n1 = tet.getNodes()[1];
      FemNode3d n2 = tet.getNodes()[2];
      FemNode3d n3 = tet.getNodes()[3];

      for (int i=0; i<nsamps; i++) {
         getRandomTetCoords (wgts, coords);

         pos.setZero();
         pos.scaledAdd (wgts[0], n0.getRestPosition());
         pos.scaledAdd (wgts[1], n1.getRestPosition());
         pos.scaledAdd (wgts[2], n2.getRestPosition());
         pos.scaledAdd (wgts[3], n3.getRestPosition());

         boolean inside = false;
         if (sdgrid != null) {
            pos.inverseTransform (mesh.getMeshToWorld());
            inside =  sdgrid.getLocalDistanceAndNormal (null, pos) < 0;
         }
         else {
            inside = BVFeatureQuery.isInsideOrientedMesh (mesh, pos);
         }
         if (inside) {
            frac += 1;
            n0.setMass (msamp*wgts[0] + n0.getMass());
            n1.setMass (msamp*wgts[1] + n1.getMass());
            n2.setMass (msamp*wgts[2] + n2.getMass());
            n3.setMass (msamp*wgts[3] + n3.getMass());
         }
      }
      frac /= nsamps;
      frac = Math.max (frac, 0.01);
      
      for (int k=0; k<tet.numAllIntegrationPoints (); ++k) {
         density.setValue (tet, k, frac);
      }
   }

   public void adjustMassAndStiffness (
      FemModel3d fem, int nsamps) {

      PolygonalMesh mesh = (PolygonalMesh)fem.getMeshComps().get(1).getMesh();

      BVFeatureQuery fquery = new BVFeatureQuery();

      DistanceGrid sdgrid =
         new DistanceGrid (mesh.getFaces(), 0.1, new Vector3i (10, 10, 20), true);   
      ScalarSubElemField density = new ScalarSubElemField ("volume_density", fem, 1.0);
      fem.addField (density);

      FunctionTimer timer = new FunctionTimer();
      timer.start();
      double baseMass = 0.01*fem.getMass()/fem.numNodes();
      for (FemNode3d n : fem.getNodes()) {
         n.setExplicitMass (baseMass);
      }
      for (FemElement3d e : fem.getElements()) {
         adjustMassAndStiffness (density, (TetElement)e, mesh, fquery, sdgrid, nsamps);
      }
      
      FemMaterial mat = fem.getMaterial ();
      ScaledFemMaterial smat = new ScaledFemMaterial (mat, 1.0);
      smat.setScalingField (density, true);
      fem.setMaterial (smat);
      
      timer.stop();
      System.out.println ("time=" + timer.result(1));
   }

   private FemModel3d createFem (FemElementType type) {

      double height = 1.0;
      double width = 0.5;
      double rad = 0.2;
      int nz = 8;
      int nx = 4;

      FemModel3d fem = new FemModel3d();
      fem.setDensity (1000.0);
      FemFactory.createGrid (
         fem, type, height, width, width, nz, nx, nx);
      
      fem.setStiffnessDamping (0.002);

      fem.setMaterial (new LinearMaterial (10000.0, 0.33));

      PolygonalMesh mesh;
      mesh = MeshFactory.createRoundedCylinder (
         rad, height-2*rad, 24, 16, /*flatBotttom=*/false);
      for (Vertex3d v : mesh.getVertices()) {
         double s = taper (v.pnt.z, height);
         v.pnt.x *= s;
         v.pnt.y *= s;
      }
      mesh.notifyVertexPositionsModified();

      for (FemNode3d n : fem.getNodes()) {
         double x = n.getPosition().x;
         if (Math.abs (x+height/2) < EPS) {
            n.setDynamic(false);
         }
      }

      fem.transformGeometry (
         new RigidTransform3d (0, 0, 0, 0, Math.PI/2, 0));

      fem.addMesh (mesh);
      setRenderProperties (fem, height);

      RenderProps.setVisible (fem.getMeshComp("surface"), false);
      RenderProps.setFaceColor (fem, new Color (1f, 153/255f, 153/255f));

      return fem;
   }

   public void build (String[] args) {

      MechModel mech = new MechModel();
      addModel (mech);

      RandomGenerator.setSeed (0x1234);

      FemModel3d fem0 = createFem(FemElementType.Tet);
      FemModel3d fem1 = createFem(FemElementType.Tet);
      
      // will use embedded utilities to replace material
      FemModel3d fem2 = createFem(FemElementType.Hex);
      
      mech.addModel (fem0);
      mech.addModel (fem1);
      mech.addModel (fem2);

      mech.setGravity(0,0,-9.8);
      
      int nsamps = 10000;
      // manual
      adjustMassAndStiffness (fem0, nsamps);
      // using utilities
      PolygonalMesh mesh2 = (PolygonalMesh)fem2.getMeshComps().get(1).getMesh();
      Logger.getSystemLogger ().setLogLevel (LogLevel.DEBUG);
      EmbeddedFem.adjustMassAndStiffness (fem2, mesh2, nsamps);
      
      double m0 = fem0.getNodeMass ();
      double m1 = fem1.getNodeMass ();
      double m2 = fem2.getNodeMass ();
      double expMass = fem0.getDensity ()*mesh2.computeVolume ();
      System.out.println ("Node masses: " + m0 + ", " + m1 + ", " + m2 + " (expected: " + expMass + ")");
      
      
      fem0.transformGeometry (
         new RigidTransform3d (0.60, 0, 0, 0, -Math.PI/4, 0));

      fem1.transformGeometry (
         new RigidTransform3d (-0.60, 0, 0, 0,-Math.PI/4, 0));

      fem2.transformGeometry (
         new RigidTransform3d (1.80, 0, 0, 0,-Math.PI/4, 0));
      
      fem0.setElementWidgetSize (0);
      fem1.setElementWidgetSize (0);
      fem2.setElementWidgetSize (0);
      
      RenderProps.setVisible (fem0.getSurfaceMeshComp (), false);
      RenderProps.setVisible (fem1.getSurfaceMeshComp (), false);
      RenderProps.setVisible (fem2.getSurfaceMeshComp (), false);
      fem0.setSurfaceRendering (SurfaceRender.Shaded);
      fem1.setSurfaceRendering (SurfaceRender.Shaded);
      fem2.setSurfaceRendering (SurfaceRender.Shaded);

   }

   private FemNode3d createRandomNode() {
      Point3d pos = new Point3d();
      pos.setRandom();
      return new FemNode3d(pos);
   }

   private double checkVolumeForRandomTet(int nsamps) {

      FemNode3d n0 = createRandomNode();
      FemNode3d n1 = createRandomNode();
      FemNode3d n2 = createRandomNode();
      FemNode3d n3 = createRandomNode();

      Point3d pos = new Point3d();

      // make sure tet is clockwise about n0, n1, n2
      Vector3d dir01 = new Vector3d();
      Vector3d dir02 = new Vector3d();
      Vector3d dir03 = new Vector3d();
      Vector3d nrm = new Vector3d();
      dir01.sub (n1.getPosition(), n0.getPosition());
      dir02.sub (n2.getPosition(), n0.getPosition());
      dir03.sub (n3.getPosition(), n0.getPosition());
      nrm.cross (dir01, dir02);
      double faceArea = nrm.norm()/2;
      nrm.normalize();
      double height = nrm.dot (dir03);
      if (height < 0) {
         dir03.scaledAdd (-2*nrm.dot (dir03), nrm);
         height = -height;
         pos.add (dir03, n0.getPosition());
         n3.setRestPosition (pos);
         n3.setPosition (pos);
      }
      double fullVolume = faceArea*height/3;

      double[] wgts = new double[4];
      Vector3d coords = new Vector3d();

      int cnt = 0;
      for (int i=0; i<nsamps; i++) {
         getRandomTetCoords (wgts, coords);

         pos.setZero();
         pos.scaledAdd (wgts[0], n0.getRestPosition());
         pos.scaledAdd (wgts[1], n1.getRestPosition());
         pos.scaledAdd (wgts[2], n2.getRestPosition());
         pos.scaledAdd (wgts[3], n3.getRestPosition());
         
         pos.sub (n0.getPosition());
         if (pos.dot (nrm) < height/2) {
            cnt++;
         }
      }

      double partVolume = (double)cnt/nsamps*fullVolume;
      return Math.abs((partVolume-7*fullVolume/8)/fullVolume);

   }

   public static void main (String[] args) {

      EmbeddedWeighting tester = new EmbeddedWeighting();

      RandomGenerator.setSeed (0x1234);

      int cnt = 100;
      double err = 0;
      for (int i=0; i<cnt; i++) {
         err += tester.checkVolumeForRandomTet(10000);
      }
      System.out.println ("avg err 10000 = " + err/cnt);
      err = 0;
      for (int i=0; i<cnt; i++) {
         err += tester.checkVolumeForRandomTet(100000);
      }
      System.out.println ("avg err 100000 = " + err/cnt);
      err = 0;
      for (int i=0; i<cnt; i++) {
         err += tester.checkVolumeForRandomTet(1000000);
      }
      System.out.println ("avg err 1000000 = " + err/cnt);
      
   }
   
}

