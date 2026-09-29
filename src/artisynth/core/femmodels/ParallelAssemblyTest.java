package artisynth.core.femmodels;

import java.util.*;

import artisynth.core.mechmodels.*;
import artisynth.core.materials.*;
import artisynth.core.fields.*;
import artisynth.core.femmodels.FemModel.IncompMethod;
import maspack.matrix.*;
import maspack.util.*;
import maspack.geometry.*;
import maspack.concurrency.ParallelLoop;

/**
 * Tests the parallel (colored) computation of element stresses and
 * stiffnesses in FemModel3d. Results are compared with the serial
 * computation, and checked to be bitwise identical for different numbers of
 * threads. Also tests the validity of the element coloring and the
 * invalidation of the cached decision about whether the parallel computation
 * can be used.
 */
public class ParallelAssemblyTest extends UnitTest {

   // relative tolerance for serial vs. parallel results
   static final double TOL = 1e-12;
   // number of threads used for the parallel computation
   static final int NUM_THREADS = 4;

   /**
    * Results of a stress and stiffness computation.
    */
   static class Result {
      VectorNd f;        // nodal forces, including back node forces
      VectorNd sig;      // nodal stresses
      SparseNumberedBlockMatrix S; // solve matrix
      double energy;     // strain energy
      boolean parallel;  // true if the colored loop was used
      long hash;         // hash of the bits of all the above values
   }

   /**
    * Computes stresses and stiffnesses, either serially, or in parallel using
    * a specified number of threads.
    */
   Result compute (
      MechModel mech, FemModel3d fem, boolean parallel, int nthreads) {
      return compute (mech, fem, parallel, nthreads, /*stiffness=*/true);
   }

   /**
    * Computes stresses and optionally stiffnesses, either serially, or in
    * parallel using a specified number of threads. If {@code stiffness} is
    * {@code false}, only the stresses are computed initially, and the
    * stiffness is computed on demand when the solve matrix is assembled.
    */
   Result compute (
      MechModel mech, FemModel3d fem, boolean parallel, int nthreads,
      boolean stiffness) {
      FemModel3d.useParallelAssembly = parallel;
      ParallelLoop.setNumThreads (nthreads);
      fem.invalidateStressAndStiffness();
      if (stiffness) {
         fem.updateStressAndStiffness();
      }
      else {
         fem.updateStress();
      }
      Result r = new Result();
      r.parallel = fem.myElementStressParallelP;
      ArrayList<Double> fvals = new ArrayList<>();
      ArrayList<Double> svals = new ArrayList<>();
      for (FemNode3d n : fem.getNodes()) {
         Vector3d f = n.getInternalForce();
         fvals.add (f.x); fvals.add (f.y); fvals.add (f.z);
         if (n.getBackNode() != null) {
            f = n.getBackNode().getInternalForce();
            fvals.add (f.x); fvals.add (f.y); fvals.add (f.z);
         }
         SymmetricMatrix3d s = n.getStress();
         svals.add (s.m00); svals.add (s.m11); svals.add (s.m22);
         svals.add (s.m01); svals.add (s.m02); svals.add (s.m12);
      }
      r.f = new VectorNd (fvals.size());
      for (int i=0; i<fvals.size(); i++) {
         r.f.set (i, fvals.get(i));
      }
      r.sig = new VectorNd (svals.size());
      for (int i=0; i<svals.size(); i++) {
         r.sig.set (i, svals.get(i));
      }
      r.S = new SparseNumberedBlockMatrix();
      mech.buildSolveMatrix (r.S);
      fem.addVelJacobian (r.S, -0.01);
      fem.addPosJacobian (r.S, -0.0001);
      r.energy = fem.getStrainEnergy();
      long h = 17;
      for (int i=0; i<r.f.size(); i++) {
         h = 31*h + Double.doubleToLongBits (r.f.get(i));
      }
      for (int i=0; i<r.sig.size(); i++) {
         h = 31*h + Double.doubleToLongBits (r.sig.get(i));
      }
      double[] vals = new double[(int)r.S.numNonZeroVals()];
      r.S.getCRSValues (vals, Matrix.Partition.Full);
      for (double v : vals) {
         h = 31*h + Double.doubleToLongBits (v);
      }
      h = 31*h + Double.doubleToLongBits (r.energy);
      r.hash = h;
      return r;
   }

   double relDiff (VectorNd a, VectorNd b) {
      VectorNd d = new VectorNd (a);
      d.sub (b);
      double na = a.norm();
      return (na == 0 ? d.norm() : d.norm()/na);
   }

   double relDiff (SparseBlockMatrix A, SparseBlockMatrix B) {
      SparseBlockMatrix D = A.clone();
      D.sub (B);
      double na = A.frobeniusNorm();
      return (na == 0 ? D.frobeniusNorm() : D.frobeniusNorm()/na);
   }

   /**
    * Compares the serial and parallel computations, and checks that the
    * parallel computation gives bitwise identical results for 1 and {@code
    * NUM_THREADS} threads. Also checks the element coloring.
    */
   void checkSerialVsParallel (
      String name, MechModel mech, FemModel3d fem, boolean expectParallel) {

      Result rs = compute (mech, fem, /*parallel=*/false, NUM_THREADS);
      Result rp = compute (mech, fem, /*parallel=*/true, NUM_THREADS);
      Result r1 = compute (mech, fem, /*parallel=*/true, 1);
      checkEquals (name+": parallel used", rp.parallel, expectParallel);
      checkEquals (name+": serial not parallel", rs.parallel, false);
      double err = relDiff (rs.f, rp.f);
      err = Math.max (err, relDiff (rs.sig, rp.sig));
      err = Math.max (err, relDiff (rs.S, rp.S));
      if (rs.energy != 0) {
         err = Math.max (err, Math.abs (rs.energy-rp.energy)/Math.abs(rs.energy));
      }
      check (name+": NaN in result", !Double.isNaN (err));
      if (err > TOL) {
         throw new TestException (
            name+": serial and parallel results differ by " + err);
      }
      if (expectParallel) {
         check (name+": result depends on number of threads",
                rp.hash == r1.hash);
         checkColoring (name, fem);
      }
   }

   /**
    * Checks that every volumetric and shell element appears exactly once in
    * the element coloring, and that blocks of the same color share no
    * nodes.
    */
   void checkColoring (String name, FemModel3d fem) {
      FemModel3d.ElementColoring coloring = fem.getElementColoring();
      int nvol = fem.numElements();
      int nelems = nvol + fem.numShellElements();
      checkEquals (name+": coloring numElems", coloring.numElems, nelems);
      checkEquals (name+": coloring numVolElems", coloring.numVolElems, nvol);
      int[] elemCount = new int[nelems];
      int nblocks = 0;
      for (int c=0; c<coloring.numColors(); c++) {
         // block that last touched each node, for this color
         HashMap<FemNode3d,Integer> nodeBlocks = new HashMap<>();
         int[] elems = coloring.elems[c];
         int[] offs = coloring.offs[c];
         for (int b=0; b<coloring.numBlocks(c); b++) {
            for (int k=offs[b]; k<offs[b+1]; k++) {
               int idx = elems[k];
               elemCount[idx]++;
               FemElement3dBase e = fem.getColoredElement (idx, nvol);
               for (FemNode3d n : e.getNodes()) {
                  Integer prev = nodeBlocks.put (n, b);
                  if (prev != null && prev != b) {
                     throw new TestException (
                        name+": node "+n.getNumber()+
                        " shared by two blocks of color "+c);
                  }
               }
            }
         }
         nblocks += coloring.numBlocks(c);
      }
      checkEquals (name+": coloring numBlocks", coloring.numBlocks, nblocks);
      for (int k=0; k<nelems; k++) {
         checkEquals (name+": element "+k+" count in coloring", elemCount[k], 1);
      }
   }

   /**
    * Adds a FEM model to a new MechModel, fixes some of its nodes, and
    * applies a pseudo-random deformation.
    */
   MechModel createMech (FemModel3d fem, boolean nodalStress) {
      if (nodalStress) {
         fem.setComputeNodalStress (true);
         fem.setComputeNodalStrain (true);
         fem.setComputeStrainEnergy (true);
      }
      MechModel mech = new MechModel ("mech");
      int k = 0;
      for (FemNode3d n : fem.getNodes()) {
         if (k++ % 17 == 0) {
            n.setDynamic (false);
         }
      }
      mech.addModel (fem);
      mech.initialize (0);
      deform (fem, 0x1234);
      return mech;
   }

   /**
    * Applies a pseudo-random deformation plus a rotation to a FEM model.
    */
   void deform (FemModel3d fem, int seed) {
      Random rand = new Random (seed);
      RigidTransform3d T = new RigidTransform3d (0, 0, 0, 0.3, 0.2, 0.1);
      for (FemNode3d n : fem.getNodes()) {
         Point3d p = new Point3d (n.getPosition());
         p.transform (T);
         p.x += 0.004*(rand.nextDouble()-0.5);
         p.y += 0.004*(rand.nextDouble()-0.5);
         p.z += 0.004*(rand.nextDouble()-0.5) + 0.02*p.x*p.x;
         n.setPosition (p);
         if (n.getBackNode() != null) {
            Point3d b = new Point3d (n.getBackPosition());
            b.transform (T);
            b.z += 0.001*(rand.nextDouble()-0.5);
            n.setBackPosition (b);
         }
      }
   }

   FemModel3d createVolumetric (String type) {
      switch (type) {
         case "hex": return FemFactory.createHexGrid (null, 1, 1, 0.5, 8, 8, 4);
         case "tet": return FemFactory.createTetGrid (null, 1, 1, 0.5, 6, 6, 3);
         case "wedge": return FemFactory.createWedgeGrid (null, 1, 1, 0.5, 6, 6, 3);
         case "pyramid": return FemFactory.createPyramidGrid (null, 1, 1, 0.5, 4, 4, 2);
         case "quadhex": return FemFactory.createQuadhexGrid (null, 1, 1, 0.5, 4, 4, 2);
         case "quadtet": return FemFactory.createQuadtetGrid (null, 1, 1, 0.5, 4, 4, 2);
         case "sphere": {
            // unstructured tetgen mesh, whose element numbering is not
            // spatially coherent
            return FemFactory.createFromMesh (
               null, MeshFactory.createIcosahedralSphere (0.5, 3), 2.0);
         }
         default: throw new IllegalArgumentException ("unknown type "+type);
      }
   }

   FemNode3d getOrCreateNode (FemModel3d fem, double x, double y, double z) {
      Point3d pos = new Point3d (x, y, z);
      for (FemNode3d n : fem.getNodes()) {
         if (n.getPosition().distance (pos) < 1e-8) {
            return n;
         }
      }
      FemNode3d n = new FemNode3d (pos);
      fem.addNode (n);
      return n;
   }

   FemModel3d createShell (String type) {
      switch (type) {
         case "quad": {
            return FemFactory.createShellQuadGrid (
               null, 1, 1, 12, 12, 0.01, /*membrane=*/false);
         }
         case "quadMembrane": {
            return FemFactory.createShellQuadGrid (
               null, 1, 1, 12, 12, 0.01, /*membrane=*/true);
         }
         case "tri": {
            return FemFactory.createShellTriGrid (
               null, 1, 1, 10, 10, 0.01, /*membrane=*/false);
         }
         case "triMembrane": {
            return FemFactory.createShellTriGrid (
               null, 1, 1, 10, 10, 0.01, /*membrane=*/true);
         }
         case "mixed": {
            // hex grid with shell elements attached to its top surface, and
            // extending beyond it
            int nx = 6, ny = 6, nz = 2;
            double wx = 1, wy = 1, wz = 0.25;
            FemModel3d fem =
               FemFactory.createHexGrid (null, wx, wy, wz, nx, ny, nz);
            int m = 2;
            double dx = wx/nx, dy = wy/ny;
            int sx = nx+2*m, sy = ny+2*m;
            for (int i=0; i<sx; i++) {
               for (int j=0; j<sy; j++) {
                  double x0 = i*dx - sx*dx/2, x1 = x0+dx;
                  double y0 = j*dy - sy*dy/2, y1 = y0+dy;
                  double z = wz/2;
                  fem.addShellElement (
                     new ShellQuadElement (
                        getOrCreateNode (fem, x0, y0, z),
                        getOrCreateNode (fem, x1, y0, z),
                        getOrCreateNode (fem, x1, y1, z),
                        getOrCreateNode (fem, x0, y1, z), 0.01));
               }
            }
            return fem;
         }
         default: throw new IllegalArgumentException ("unknown type "+type);
      }
   }

   /**
    * Tests volumetric element types with basic materials.
    */
   void testVolumetricElements() {
      String[] types = {
         "hex", "tet", "wedge", "pyramid", "quadhex", "quadtet", "sphere" };
      for (String type : types) {
         FemModel3d fem = createVolumetric (type);
         fem.setMaterial (new NeoHookeanMaterial (50000, 0.33));
         MechModel mech = createMech (fem, /*nodalStress=*/true);
         checkSerialVsParallel (type+" neo +stress", mech, fem, true);

         fem = createVolumetric (type);
         fem.setMaterial (new LinearMaterial (50000, 0.33));
         mech = createMech (fem, /*nodalStress=*/false);
         checkSerialVsParallel (type+" linear", mech, fem, true);
      }
   }

   /**
    * Tests other materials, including incompressible ones.
    */
   void testMaterials() {
      IncompMethod[] methods = {
         IncompMethod.ELEMENT, IncompMethod.NODAL, IncompMethod.FULL };
      for (IncompMethod method : methods) {
         for (String type : new String[] { "hex", "tet" }) {
            FemModel3d fem = createVolumetric (type);
            fem.setMaterial (
               new MooneyRivlinMaterial (10000, 2000, 0, 0, 0, 1e6));
            fem.setSoftIncompMethod (method);
            MechModel mech = createMech (fem, /*nodalStress=*/false);
            checkSerialVsParallel (
               type+" mooney "+method, mech, fem, true);
         }
      }
      FemMaterial[] mats = new FemMaterial[] {
         new StVenantKirchoffMaterial (50000, 0.33),
         new OgdenMaterial(),
         new FungOrthotropicMaterial(),
         new ScaledFemMaterial (new OgdenMaterial(), 0.7),
         new ViscoelasticMaterial (
            new NeoHookeanMaterial (50000, 0.33), new QLVBehavior()),
         new TransverseLinearMaterial (
            new Vector2d (10000, 100000), 5000, new Vector2d (0.3, 0.2), true),
      };
      for (FemMaterial mat : mats) {
         FemModel3d fem = createVolumetric ("hex");
         fem.setMaterial (mat);
         MechModel mech = createMech (fem, /*nodalStress=*/true);
         checkSerialVsParallel (
            "hex "+mat.getClass().getSimpleName(), mech, fem, true);
      }
   }

   /**
    * Tests elements with auxiliary muscle materials, and augmenting
    * materials.
    */
   void testAuxiliaryMaterials() {
      FemMuscleModel fem = new FemMuscleModel ("fem");
      FemFactory.createHexGrid (fem, 1, 1, 0.5, 8, 8, 4);
      fem.setMaterial (new NeoHookeanMaterial (50000, 0.33));
      MuscleBundle bundle = new MuscleBundle ("bundle");
      bundle.setMuscleMaterial (new GenericMuscle());
      fem.addMuscleBundle (bundle);
      for (int i=0; i<fem.numElements(); i+=3) {
         bundle.addElement (fem.getElements().get(i), new Vector3d (1, 0.2, 0));
      }
      bundle.setExcitation (0.3);
      MaterialBundle mbundle = new MaterialBundle (
         "mb", new MooneyRivlinMaterial (5000, 0, 0, 0, 0, 0), false);
      fem.addMaterialBundle (mbundle);
      for (int i=0; i<fem.numElements(); i+=5) {
         mbundle.addElement (fem.getElements().get(i));
      }
      MechModel mech = createMech (fem, /*nodalStress=*/true);
      checkSerialVsParallel ("hex muscle+augmenting", mech, fem, true);
   }

   /**
    * Tests shell, membrane, and mixed volumetric/shell models.
    */
   void testShellElements() {
      String[] types = {
         "quad", "quadMembrane", "tri", "triMembrane", "mixed" };
      for (String type : types) {
         FemModel3d fem = createShell (type);
         fem.setMaterial (new NeoHookeanMaterial (1e6, 0.33));
         MechModel mech = createMech (fem, /*nodalStress=*/true);
         checkSerialVsParallel (type+" neo +stress", mech, fem, true);

         fem = createShell (type);
         fem.setMaterial (new LinearMaterial (1e6, 0.33));
         mech = createMech (fem, /*nodalStress=*/false);
         checkSerialVsParallel (type+" linear", mech, fem, true);
      }
   }

   /**
    * Tests the stress-only computation performed by updateStress(): serial
    * and parallel results should agree, the parallel results should not
    * depend on the number of threads, the forces should match those
    * computed along with the stiffness, and the stiffness should be computed
    * correctly when later required for the solve matrix.
    */
   void testStressOnly() {
      String[] types = { "hex", "quadtet", "sphere" };
      FemMaterial[] mats = new FemMaterial[] {
         new NeoHookeanMaterial (50000, 0.33),
         new LinearMaterial (50000, 0.33),
         new MooneyRivlinMaterial (10000, 2000, 0, 0, 0, 1e6),
      };
      ArrayList<FemModel3d> fems = new ArrayList<>();
      ArrayList<String> names = new ArrayList<>();
      for (String type : types) {
         for (FemMaterial mat : mats) {
            FemModel3d fem = createVolumetric (type);
            fem.setMaterial (mat);
            if (mat instanceof MooneyRivlinMaterial) {
               fem.setSoftIncompMethod (IncompMethod.NODAL);
            }
            fems.add (fem);
            names.add (type+" "+mat.getClass().getSimpleName());
         }
      }
      FemModel3d fem = createShell ("mixed");
      fem.setMaterial (new NeoHookeanMaterial (1e6, 0.33));
      fems.add (fem);
      names.add ("mixed NeoHookeanMaterial");

      for (int i=0; i<fems.size(); i++) {
         fem = fems.get(i);
         String name = "stress only: "+names.get(i);
         MechModel mech = createMech (fem, /*nodalStress=*/true);
         // compute the full stress and stiffness, then deform the model so
         // that stiffness left over from this computation is stale
         compute (mech, fem, true, NUM_THREADS, true);
         deform (fem, 0x5678);
         Result rs = compute (mech, fem, false, NUM_THREADS, false);
         Result rp = compute (mech, fem, true, NUM_THREADS, false);
         Result r1 = compute (mech, fem, true, 1, false);
         Result full = compute (mech, fem, true, NUM_THREADS, true);
         checkEquals (name+": parallel used", rp.parallel, true);
         double err = relDiff (rs.f, rp.f);
         err = Math.max (err, relDiff (rs.sig, rp.sig));
         if (rs.energy != 0) {
            err = Math.max (
               err, Math.abs (rs.energy-rp.energy)/Math.abs(rs.energy));
         }
         check (name+": NaN in result", !Double.isNaN (err));
         if (err > TOL) {
            throw new TestException (
               name+": serial and parallel results differ by " + err);
         }
         check (name+": result depends on number of threads",
                rp.hash == r1.hash);
         // forces and stresses should match those computed with stiffness
         err = Math.max (relDiff (full.f, rp.f), relDiff (full.sig, rp.sig));
         if (err > TOL) {
            throw new TestException (
               name+": forces differ from full computation by " + err);
         }
         // stiffness computed on demand should match the full computation
         err = relDiff (full.S, rp.S);
         if (err > TOL) {
            throw new TestException (
               name+": on-demand stiffness differs by " + err);
         }
      }
   }

   /**
    * Tests materials whose properties are bound to fields, including
    * checking that changes to field values made after the field caches have
    * been built take effect.
    */
   void testFields() {
      // nodal field
      FemModel3d fem = createVolumetric ("tet");
      ScalarNodalField nfield = new ScalarNodalField (fem, 50000);
      for (FemNode3d n : fem.getNodes()) {
         nfield.setValue (n, 30000 + 40000*(n.getPosition().x+0.5));
      }
      fem.addField (nfield);
      NeoHookeanMaterial neo = new NeoHookeanMaterial (50000, 0.33);
      neo.setYoungsModulusField (nfield);
      fem.setMaterial (neo);
      MechModel mech = createMech (fem, /*nodalStress=*/false);
      checkSerialVsParallel ("tet neo nodal field", mech, fem, true);

      // change field values after the caches have been built, and compare
      // with a model created with the new values
      Result r0 = compute (mech, fem, true, NUM_THREADS);
      int k = 0;
      for (FemNode3d n : fem.getNodes()) {
         if (k++ % 3 == 0) {
            nfield.setValue (n, 2*(30000 + 40000*(n.getRestPosition().x+0.5)));
         }
      }
      Result r1 = compute (mech, fem, true, NUM_THREADS);
      check ("nodal field change has no effect",
             relDiff (r0.f, r1.f) > 1e-3);
      FemModel3d fem2 = createVolumetric ("tet");
      ScalarNodalField nfield2 = new ScalarNodalField (fem2, 50000);
      k = 0;
      for (FemNode3d n : fem2.getNodes()) {
         double E = 30000 + 40000*(n.getPosition().x+0.5);
         nfield2.setValue (n, (k++ % 3 == 0) ? 2*E : E);
      }
      fem2.addField (nfield2);
      neo = new NeoHookeanMaterial (50000, 0.33);
      neo.setYoungsModulusField (nfield2);
      fem2.setMaterial (neo);
      MechModel mech2 = createMech (fem2, /*nodalStress=*/false);
      Result rchk = compute (mech2, fem2, true, NUM_THREADS);
      check ("stale nodal field cache", relDiff (rchk.f, r1.f) <= TOL);

      // element field
      fem = createVolumetric ("hex");
      ScalarElementField efield = new ScalarElementField (fem, 50000);
      for (FemElement3d e : fem.getElements()) {
         Point3d c = new Point3d();
         e.computeCentroid (c);
         efield.setValue (e, 30000 + 40000*(c.x+0.5));
      }
      fem.addField (efield);
      neo = new NeoHookeanMaterial (50000, 0.33);
      neo.setYoungsModulusField (efield);
      fem.setMaterial (neo);
      mech = createMech (fem, /*nodalStress=*/false);
      checkSerialVsParallel ("hex neo element field", mech, fem, true);

      // grid field
      fem = createVolumetric ("hex");
      ScalarGrid grid = new ScalarGrid (
         new Vector3d (1.6, 1.6, 1.6), new Vector3i (6, 6, 6));
      for (int vi=0; vi<grid.numVertices(); vi++) {
         grid.setVertexValue (vi, 5000 + 50*vi);
      }
      ScalarGridField gfield = new ScalarGridField (grid);
      fem.addField (gfield);
      MooneyRivlinMaterial mooney =
         new MooneyRivlinMaterial (10000, 2000, 0, 0, 0, 1e6);
      mooney.setC10Field (gfield);
      fem.setMaterial (mooney);
      mech = createMech (fem, /*nodalStress=*/false);
      checkSerialVsParallel ("hex mooney grid field", mech, fem, true);

      // vector nodal field for a muscle rest direction
      fem = createVolumetric ("hex");
      Vector3dNodalField dfield = new Vector3dNodalField (fem);
      for (FemNode3d n : fem.getNodes()) {
         Point3d p = n.getPosition();
         Vector3d d = new Vector3d (1, 0.5*p.y, 0.3*p.z);
         d.normalize();
         dfield.setValue (n, d);
      }
      fem.addField (dfield);
      GenericMuscle gmat = new GenericMuscle();
      gmat.setExcitation (0.3);
      gmat.setRestDirField (dfield);
      fem.setMaterial (gmat);
      mech = createMech (fem, /*nodalStress=*/false);
      checkSerialVsParallel ("hex muscle direction field", mech, fem, true);
   }

   /**
    * Checks that a TransverseLinearMaterial whose direction is given by an
    * element field gives the same forces as per-element materials with the
    * same directions.
    */
   void testTransverseDirectionField() {
      VectorNd[] forces = new VectorNd[2];
      for (int pass=0; pass<2; pass++) {
         FemModel3d fem = createVolumetric ("hex");
         Vector3dElementField field = new Vector3dElementField (fem);
         int k = 0;
         for (FemElement3d e : fem.getElements()) {
            Vector3d dir = (k++ % 2 == 0 ?
               new Vector3d (0, 0, 1) : new Vector3d (1, 1, 0));
            dir.normalize();
            if (pass == 0) {
               field.setValue (e, dir);
            }
            else {
               TransverseLinearMaterial mat = createTransverseMaterial();
               mat.setDirection (dir);
               e.setMaterial (mat);
            }
         }
         TransverseLinearMaterial mat = createTransverseMaterial();
         if (pass == 0) {
            fem.addField (field);
            mat.setDirectionField (field);
         }
         fem.setMaterial (mat);
         MechModel mech = createMech (fem, /*nodalStress=*/false);
         forces[pass] = compute (mech, fem, true, NUM_THREADS).f;
      }
      check ("transverse direction field does not match element materials",
             relDiff (forces[1], forces[0]) <= TOL);
   }

   TransverseLinearMaterial createTransverseMaterial() {
      return new TransverseLinearMaterial (
         new Vector2d (10000, 100000), 5000, new Vector2d (0.3, 0.2), true);
   }

   /**
    * Checks that initializing a model with a material that has state works
    * even though the element state objects have not yet been created.
    */
   void testStateObjectsAtInitialize() {
      for (boolean parallel : new boolean[] { false, true }) {
         FemModel3d.useParallelAssembly = parallel;
         MechModel mech = new MechModel ("mech");
         FemModel3d fem = createVolumetric ("hex");
         fem.setMaterial (
            new ViscoelasticMaterial (
               new NeoHookeanMaterial (50000, 0.33), new QLVBehavior()));
         mech.addModel (fem);
         mech.initialize (0);
      }
   }

   /**
    * Scalar nodal field that allows queries of whether its integration point
    * cache is filled.
    */
   static class TestNodalField extends ScalarNodalField {

      TestNodalField (FemModel3d fem, double defaultValue) {
         super (fem, defaultValue);
      }

      boolean isCacheFilled() {
         if (myVolumetricValues == null) {
            return false;
         }
         for (FemElement3d e : getFemModel().getElements()) {
            if (myVolumetricValues.get (e.getNumber()) == null) {
               return false;
            }
         }
         return true;
      }
   }

   /**
    * Anonymous subclass of NeoHookeanMaterial, which is not thread safe.
    */
   static FemMaterial createUnsafeNeo() {
      return new NeoHookeanMaterial (50000, 0.33) {};
   }

   static NeoHookeanMaterial createNeo() {
      return new NeoHookeanMaterial (50000, 0.33);
   }

   /**
    * Checks that the parallel computation is used as expected, and that its
    * results match the serial computation.
    */
   void checkCache (
      String name, MechModel mech, FemModel3d fem, boolean expectParallel) {
      checkSerialVsParallel ("cache: "+name, mech, fem, expectParallel);
   }

   /**
    * Tests that the cached decision about whether the parallel computation
    * can be used is invalidated when materials are changed through various
    * paths.
    */
   void testCacheInvalidation() {
      FemMuscleModel fem = new FemMuscleModel ("fem");
      FemFactory.createHexGrid (fem, 1, 1, 0.5, 8, 8, 4);
      fem.setMaterial (createNeo());
      MechModel mech = createMech (fem, /*nodalStress=*/false);
      FemElement3d e0 = fem.getElements().get (fem.numElements()/2);

      checkCache ("baseline", mech, fem, true);

      // model material
      fem.setMaterial (createUnsafeNeo());
      checkCache ("model: unsafe material", mech, fem, false);
      fem.setMaterial (createNeo());
      checkCache ("model: safe material", mech, fem, true);

      // element material
      e0.setMaterial (createUnsafeNeo());
      checkCache ("element: unsafe material", mech, fem, false);
      e0.setMaterial (createNeo());
      checkCache ("element: safe material", mech, fem, true);
      TestNodalField field = new TestNodalField (fem, 50000);
      for (FemNode3d n : fem.getNodes()) {
         field.setValue (n, 40000 + 20000*n.getPosition().x);
      }
      fem.addField (field);
      ((NeoHookeanMaterial)e0.getMaterial()).setYoungsModulusField (field);
      checkCache ("element: field bound after check", mech, fem, true);
      checkFieldCacheFilled ("element: field bound after check", field);
      e0.setMaterial (null);
      checkCache ("element: material removed", mech, fem, true);

      // base material of a composite material
      fem.setMaterial (new ScaledFemMaterial (createNeo(), 1.2));
      ScaledFemMaterial smat = (ScaledFemMaterial)fem.getMaterial();
      checkCache ("scaled: safe base", mech, fem, true);
      smat.setBaseMaterial (createUnsafeNeo());
      checkCache ("scaled: unsafe base", mech, fem, false);
      smat.setBaseMaterial (createNeo());
      checkCache ("scaled: safe base again", mech, fem, true);
      ((NeoHookeanMaterial)smat.getBaseMaterial()).setYoungsModulusField (field);
      checkCache ("scaled: field bound to base after check", mech, fem, true);
      checkFieldCacheFilled ("scaled: field bound to base after check", field);

      // viscoelastic behavior
      fem.setMaterial (
         new ViscoelasticMaterial (createNeo(), new QLVBehavior()));
      checkCache ("visco: QLV", mech, fem, true);
      ViscoelasticMaterial vmat = (ViscoelasticMaterial)fem.getMaterial();
      vmat.setViscoBehavior (new QLVBehavior() {});
      checkCache ("visco: QLV subclass", mech, fem, false);
      vmat.setViscoBehavior (new QLVBehavior());
      checkCache ("visco: QLV again", mech, fem, true);
      fem.setMaterial (createNeo());

      // material bundles
      MaterialBundle mb = new MaterialBundle ("mb", createNeo(), true);
      fem.addMaterialBundle (mb);
      checkCache ("matBundle(all): safe", mech, fem, true);
      mb.setMaterial (createUnsafeNeo());
      checkCache ("matBundle(all): unsafe", mech, fem, false);
      mb.setUseAllElements (false);
      checkCache ("matBundle: useAllElements=false", mech, fem, true);
      mb.addElement (e0);
      checkCache ("matBundle: add element", mech, fem, false);
      mb.removeElement (e0);
      checkCache ("matBundle: remove element", mech, fem, true);
      mb.addElement (e0);
      checkCache ("matBundle: add element again", mech, fem, false);
      mb.setMaterial (createNeo());
      checkCache ("matBundle: safe material", mech, fem, true);
      ((NeoHookeanMaterial)mb.getMaterial()).setYoungsModulusField (field);
      checkCache ("matBundle: field bound after check", mech, fem, true);
      checkFieldCacheFilled ("matBundle: field bound after check", field);
      mb.setMaterial (createUnsafeNeo());
      checkCache ("matBundle: unsafe material", mech, fem, false);
      fem.removeMaterialBundle (mb);
      checkCache ("matBundle: removed", mech, fem, true);

      // muscle bundles
      MuscleBundle bundle = new MuscleBundle ("bundle");
      bundle.setMuscleMaterial (new GenericMuscle());
      fem.addMuscleBundle (bundle);
      for (int i=0; i<fem.numElements(); i+=7) {
         bundle.addElement (fem.getElements().get(i), new Vector3d (1, 0, 0));
      }
      bundle.setExcitation (0.2);
      checkCache ("muscle: GenericMuscle", mech, fem, true);
      bundle.setMuscleMaterial (new GenericMuscle() {});
      checkCache ("muscle: unsafe material", mech, fem, false);
      bundle.setMuscleMaterial (new GenericMuscle());
      checkCache ("muscle: safe material", mech, fem, true);
      ((GenericMuscle)bundle.getMuscleMaterial()).setMaxStressField (field);
      checkCache ("muscle: field bound after check", mech, fem, true);
      checkFieldCacheFilled ("muscle: field bound after check", field);
      fem.setMuscleMaterial (new GenericMuscle() {});
      bundle.setMuscleMaterial (null);
      checkCache ("muscle: unsafe model muscle material", mech, fem, false);
      fem.setMuscleMaterial (new GenericMuscle());
      checkCache ("muscle: safe model muscle material", mech, fem, true);
      fem.removeMuscleBundle (bundle);
      checkCache ("muscle: bundle removed", mech, fem, true);

      // auxiliary material bundles
      AuxMaterialBundle ab = new AuxMaterialBundle ("ab");
      ab.setMaterial (createNeo());
      fem.addAuxMaterialBundle (ab);
      for (int i=0; i<fem.numElements(); i+=5) {
         ab.addElement (fem.getElements().get(i), 0.5);
      }
      checkCache ("auxBundle: safe", mech, fem, true);
      ab.setMaterial (createUnsafeNeo());
      checkCache ("auxBundle: unsafe", mech, fem, false);
      ab.setMaterial (createNeo());
      checkCache ("auxBundle: safe again", mech, fem, true);

      // the cached decision should persist across ordinary updates
      FemNode3d n0 = fem.getNodes().get(3);
      Point3d p = new Point3d (n0.getPosition());
      p.x += 1e-4;
      n0.setPosition (p);
      fem.setExcitation (0.1);
      mech.advance (0, 0.01, 0);
      FemModel3d.useParallelAssembly = true;
      fem.invalidateStressAndStiffness();
      fem.updateStressAndStiffness();
      check ("cache not persistent across updates", fem.myParallelCheckValid);
   }

   /**
    * Checks that the integration point cache of a nodal field was filled
    * for all elements by the last parallel stress computation. The cache is
    * cleared first and the computation redone, so that the field must have
    * been prepared again.
    */
   void checkFieldCacheFilled (String name, TestNodalField field) {
      FemModel3d fem = field.getFemModel();
      field.clearCacheIfNecessary();
      FemModel3d.useParallelAssembly = true;
      ParallelLoop.setNumThreads (NUM_THREADS);
      fem.invalidateStressAndStiffness();
      fem.updateStressAndStiffness();
      check ("cache: "+name+": parallel not used",
             fem.myElementStressParallelP);
      check ("cache: "+name+": field not prepared",
             field.isCacheFilled());
   }

   public void test() {
      boolean saveParallel = FemModel3d.useParallelAssembly;
      double saveMinWork = FemModel3d.MIN_PARALLEL_ELEM_WORK;
      double saveChunkWork = FemModel3d.ELEM_CHUNK_WORK;
      int saveNumThreads = ParallelLoop.getNumThreads();
      try {
         // force the parallel computation to be used for small models
         FemModel3d.MIN_PARALLEL_ELEM_WORK = 0;
         FemModel3d.ELEM_CHUNK_WORK = 1;
         testVolumetricElements();
         testMaterials();
         testAuxiliaryMaterials();
         testShellElements();
         testStressOnly();
         testFields();
         testTransverseDirectionField();
         testStateObjectsAtInitialize();
         testCacheInvalidation();
      }
      finally {
         FemModel3d.useParallelAssembly = saveParallel;
         FemModel3d.MIN_PARALLEL_ELEM_WORK = saveMinWork;
         FemModel3d.ELEM_CHUNK_WORK = saveChunkWork;
         ParallelLoop.setNumThreads (saveNumThreads);
      }
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);
      ParallelAssemblyTest tester = new ParallelAssemblyTest();
      tester.runtest();
   }
}
