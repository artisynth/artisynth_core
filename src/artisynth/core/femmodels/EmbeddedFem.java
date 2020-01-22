package artisynth.core.femmodels;

import java.util.ArrayList;

import artisynth.core.femmodels.FemDeformedPoint;
import artisynth.core.femmodels.FemElement3d;
import artisynth.core.femmodels.FemFactory;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.femmodels.HexElement;
import artisynth.core.femmodels.IntegrationData3d;
import artisynth.core.femmodels.IntegrationPoint3d;
import artisynth.core.femmodels.MaterialBundle;
import artisynth.core.femmodels.PyramidElement;
import artisynth.core.femmodels.ScalarElementField;
import artisynth.core.femmodels.ScalarFemField;
import artisynth.core.femmodels.ScalarNodalField;
import artisynth.core.femmodels.ScalarSubElemField;
import artisynth.core.femmodels.TetElement;
import artisynth.core.materials.FemMaterial;
import artisynth.core.materials.ScaledFemMaterial;
import artisynth.core.modelbase.ScalarField;
import artisynth.core.modelbase.ScalarFieldPointFunction;
import artisynth.core.femmodels.integration.FemElementIntegrator;
import artisynth.core.femmodels.integration.IPointFemElementIntegrator;
import artisynth.core.femmodels.integration.MonteCarloFemElementIntegrator;
import artisynth.core.femmodels.integration.EulerianFemElementSampler;
import artisynth.core.femmodels.integration.FemElementSampler;
import artisynth.core.femmodels.integration.LagrangianFemElementSampler;
import maspack.function.Function3x1;
import maspack.function.Function3x1Base;
import maspack.geometry.DistanceGrid;
import maspack.geometry.OBB;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.Vertex3d;
import maspack.matrix.AxisAngle;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.SparseMatrixNd;
import maspack.matrix.Vector3d;
import maspack.matrix.Vector3i;
import maspack.matrix.VectorNd;
import maspack.solvers.DirectSolver;
import maspack.solvers.PardisoSolver;
import maspack.util.FunctionTimer;
import maspack.util.Logger;

/**
 * Useful functions for creating embedded finite element models
 *
 */
public class EmbeddedFem {
   
   public static int DEFAULT_SAMPLES = 1000;
   private static Class<? extends DirectSolver> mySolverClass = PardisoSolver.class;
   
   /**
    * Set sparse matrix solver for use in computing best-fitting node values
    * @param clazz solver class type
    */
   public static void setSolverClass(Class<? extends DirectSolver> clazz) {
      mySolverClass = clazz;
   }
   
   /**
    * Check sparse matrix solver type for use in computing best-fitting node values
    * @return solver class type
    */
   public static Class<? extends DirectSolver> getSolverClass() {
      return mySolverClass;
   }
   
   /**
    * Creates an instance of a matrix solver using the type defined by {@link #getSolverClass()}
    * @return matrix solver
    */
   private static DirectSolver createSolver() {
      DirectSolver solver = null;
      try {
         solver = mySolverClass.newInstance ();
      } catch (Exception e) {
         e.printStackTrace();
      }
      return solver;
   }

   /**
    * Numerically integrates the density function over each element to compute
    * masses concentrated at the nodes.
    * 
    * @param fem model
    * @param density density function
    * @param integrator integrator object for integrating density over elements
    */
   public static void setMasses(
      FemModel3d fem, Function3x1 density, FemElementIntegrator integrator) {

      // reset node masses to zero
      for (FemNode3d node : fem.getNodes()) {
         node.setExplicitMass (0);
      }

      for (FemElement3d elem : fem.getElements()) {
         elem.setExplicitMass(0);
      }

      VectorNd v = new VectorNd();
      for (FemElement3d elem : fem.getElements()) {
         v.setSize(elem.numNodes());
         integrator.integrateShapeFunctionProductRest(elem, density, v);
         FemNode3d[] nodes = elem.getNodes();
         double emass = 0;
         for (int i = 0; i < elem.numNodes(); ++i) {
            emass += v.get(i);
            nodes[i].addMass(v.get(i));
         }
         elem.setMass(emass);
      }
   }
   
   /**
    * Sets node and element masses based on a supplied density function by integrating over each 
    * element to compute masses concentrated at the nodes.
    * @param fem model
    * @param density density function
    * @param scale scale for density function
    */
   public static void setMasses(
      FemModel3d fem, ScalarFemField density, double scale) {

      // reset node masses to zero
      for (FemNode3d node : fem.getNodes()) {
         node.setExplicitMass (0);
      }

      for (FemElement3d elem : fem.getElements()) {
         elem.setExplicitMass(0);
      }
      
      ScalarFieldPointFunction func = density.createFieldFunction (true);

      VectorNd v = new VectorNd();
      VectorNd ivals = new VectorNd();
      FemDeformedPoint dpnt = new FemDeformedPoint();
      for (FemElement3d elem : fem.getElements()) {
         
         v.setSize(elem.numNodes());
         ivals.setSize(elem.numIntegrationPoints());
         IntegrationPoint3d[] ipnts = elem.getIntegrationPoints();
         IntegrationData3d[] idat = elem.getIntegrationData ();

         for (int k = 0; k < ipnts.length; ++k) {
            dpnt.setFromIntegrationPoint (ipnts[k], idat[k], null, elem, k);
            double val = func.eval (dpnt);
            ivals.set(k, val*scale);
         }

         IPointFemElementIntegrator.integrateShapeFunctionProduct(elem, ivals, v);

         FemNode3d[] nodes = elem.getNodes();
         double emass = 0;
         for (int i = 0; i < elem.numNodes(); ++i) {
            emass += v.get(i);
            nodes[i].addMass(v.get(i));
         }

         elem.setMass(emass);
      }
   }
   
   /**
    * Creates a material bundle based on a supplied 
    * material and a volume density function.  The material's stress/stiffness
    * contributions will be scaled by the density
    * 
    * @param fem model
    * @param mat material to use for bundle
    * @param density density function
    * @return generated material bundle
    */
   public static MaterialBundle createMaterialBundle(
      FemModel3d fem, FemMaterial mat, ScalarField density) {
      
      MaterialBundle bundle = new MaterialBundle();
      bundle.setUseAllElements (true);
      ScaledFemMaterial smat = new ScaledFemMaterial (mat, 1.0);
      smat.setScalingField (density, true);
        
      return bundle;
   }
   
   /**
    * Computes a best-fitting scalar nodal field, computing node values by minimizing the squared error between
    * a sampled function, and the one interpolated via nodes and shape functions.
    * 
    * @param fem model containing nodes
    * @param func function to sample
    * @param sampler method of sampling from elements
    * @param nSamplesPerElement number of samples per element
    * @return best-fitting nodal field
    */
   public static ScalarNodalField computeNodalField(FemModel3d fem, Function3x1 func, FemElementSampler sampler, 
      int nSamplesPerElement) {
      
      // set up matrix system
      DirectSolver solver = createSolver ();
      
      int nnodes = fem.numNodes ();
      SparseMatrixNd A = new SparseMatrixNd (nnodes, nnodes);
      VectorNd b = new VectorNd(nnodes);  // rhs
      VectorNd x = new VectorNd(nnodes);  // node values
      
      Point3d pnt = new Point3d();        // world point
      Point3d coord = new Point3d();      // natural coord
      
      for (FemElement3d elem : fem.getElements ()) {
         sampler.setElement (elem);
         FemNode3d[] nodes = elem.getNodes ();
         VectorNd a = new VectorNd(nodes.length);
         
         for (int i=0; i<nSamplesPerElement; ++i) {
            sampler.sample (pnt);
            
            // approximate function by interpolating from nodes
            sampler.sample(coord, pnt);
            for (int j=0; j<nodes.length; ++j) {
               a.set(j, elem.getN(j, coord));
            }
            double c = func.eval(pnt);
            
            // add to matrix and vector
            for (int j = 0; j<nodes.length; ++j) {
               double aj = a.get (j);
               int nodej = nodes[j].getNumber ();
               for (int k=0; k<nodes.length; ++k) {
                  double ak = a.get (k);
                  int nodek = nodes[k].getNumber ();
                  double v = A.get (nodej, nodek) + aj*ak;
                  A.set(nodej, nodek, v);
               } // each row in A
               
               b.add (nodej, aj*c);
            } // each col in A, b
            
         } // each sample per element
         
      } // each element
      
      solver.analyzeAndFactor (A);
      solver.solve (x, b);
      
      // set node values
      ScalarNodalField field = new ScalarNodalField (fem, 0);
      for (FemNode3d node : fem.getNodes ()) {
         field.setValue (node, x.get (node.getNumber ()));
      }
      
      return field;
   }

   /**
    * Compute a field by sampling a function over the FEM model and using
    * least-squares to fit the FEM element shape functions 
    * independently over each element.
    * 
    * @param fem finite element model over which to create field
    * @param func function to sample
    * @param sampler sampling object
    * @param nSamplesPerElement number of samples to take per element
    * @return computed field
    */
   public static ScalarSubElemField computeSubElemField(
      FemModel3d fem, Function3x1 func, FemElementSampler sampler,
      int nSamplesPerElement) {

      FemFieldApproximation approx = new FemFieldApproximation(sampler);
      approx.setNumLSQSamples(nSamplesPerElement);

      ScalarSubElemField field = new ScalarSubElemField(fem, 0);

      VectorNd nvals = new VectorNd();
      VectorNd ivals = new VectorNd();
      for (FemElement3d elem : fem.getElements()) {
         nvals.setSize(elem.numNodes());
         approx.computeLeastSquaresNodeValues(elem, func, nvals);

         // compute integration point values
         FemNode3d[] nodes = elem.getNodes();

         ivals.setSize(elem.numAllIntegrationPoints());
         IntegrationPoint3d[] ipnts = elem.getAllIntegrationPoints();

         boolean hasNonZero = false;
         for (int k = 0; k < ipnts.length; ++k) {
            Vector3d icoords = ipnts[k].getCoords();
            double val = 0;
            for (int j = 0; j < nodes.length; ++j) {
               double n = elem.getN(j, icoords);
               val += n * nvals.get(j);
            }
            ivals.set(k, val);
            if (val != 0) {
               hasNonZero = true;
            }
         }

         // only set if has non-zero entry
         if (hasNonZero) {
            for (int k = 0; k < ipnts.length; ++k) {
               field.setValue(elem, k, ivals.get(k));
            }
         }
      }

      return field;
   }
   
   /**
    * Compute a field by simply evaluating the function at integration points
    * 
    * @param fem finite element model over which to create field
    * @param func function to sample
    * @return computed field
    */
   public static ScalarSubElemField computeSubElemField(FemModel3d fem, Function3x1 func) {

      ScalarSubElemField field = new ScalarSubElemField(fem, 0);

      VectorNd nvals = new VectorNd();
      VectorNd ivals = new VectorNd();
      for (FemElement3d elem : fem.getElements()) {
         nvals.setSize(elem.numNodes());

         // compute integration point values
         ivals.setSize(elem.numAllIntegrationPoints());
         IntegrationPoint3d[] ipnts = elem.getAllIntegrationPoints();

         Point3d coord = new Point3d();
         boolean hasNonZero = false;
         for (int k = 0; k < ipnts.length; ++k) {
            ipnts[k].computeRestPosition (coord, elem);
            double val = func.eval (coord);
            ivals.set(k, val);
            if (val != 0) {
               hasNonZero = true;
            }
         }

         // only set if has non-zero entry
         if (hasNonZero) {
            for (int k = 0; k < ipnts.length; ++k) {
               field.setValue(elem, k, ivals.get(k));
            }
         }
      }

      return field;
   }
  
   /**
    * Is-inside function using a signed-distance grid
    */
   private static class IsInsideFunctionSD extends Function3x1Base {
      PolygonalMesh mesh;
      DistanceGrid sdgrid;
      Point3d pnt;

      public IsInsideFunctionSD(PolygonalMesh mesh) {
         this.mesh = mesh;
         sdgrid = new DistanceGrid(mesh.getFaces(), 0.1, new Vector3i(50, 50, 50), true);
         pnt = new Point3d();
      }

      @Override
      public double eval(double x, double y, double z) {
         pnt.set(x, y, z);
         return eval(pnt);
      }

      @Override
      public double eval(Point3d pnt) {
         this.pnt.inverseTransform(mesh.getMeshToWorld(), pnt);
         double dist = sdgrid.getLocalDistanceAndNormal(null, this.pnt);
         boolean inside = (dist < 1e-5);
         return inside ? 1 : 0;
      }

   }

   /**
    * Adjusts mass and stiffness of a FEM model based on
    * a given surface mesh.  Does this by adjusting the node/
    * element masses based on integrating the FEM's
    * density within the mesh volume, and adjusts
    * stiffness by creating a scalar field to modulate
    * the stiffness/tangent contributions.
    * 
    * @param fem model to adjust
    * @param surface surface-mesh for determining inside-outside
    * @param nSamplesPerElement number of samples per element for numerical
    * integration
    */
   public static void adjustMassAndStiffness(
      FemModel3d fem, PolygonalMesh surface, int nSamplesPerElement) {

      if (surface == null) {
         surface = fem.getSurfaceMesh();
      }
      if (nSamplesPerElement <= 0) {
         nSamplesPerElement = DEFAULT_SAMPLES;
      }

      double expectedMass = surface.computeVolume() * fem.getDensity();

      // signed-distance
      Function3x1 inside = new IsInsideFunctionSD(surface); 
      
      // Lagrangian sampling with monte-carlo integration
      LagrangianFemElementSampler sampler = new LagrangianFemElementSampler();
      MonteCarloFemElementIntegrator integrator =
         new MonteCarloFemElementIntegrator(sampler);
      integrator.setMaxSamples(nSamplesPerElement);

      FunctionTimer timer = new FunctionTimer();
      timer.start();

      // compute inside/outside field
      
      ScalarFemField density = EmbeddedFem.computeNodalField(fem, inside, sampler, nSamplesPerElement);
      density.setName ("volume_density");
      fem.addField (density);
      
      // prevent values less than zero
      adjustFieldMinimum (fem, density, 0);

      // compute node masses from field
      EmbeddedFem.setMasses(fem, density, fem.getDensity ());
      
      //      // adjust zero stiffness
      //      adjustFieldMinimum (fem, density, 0.01);

      // replace material with a wrapped scaled version
      FemMaterial oldMat = fem.getMaterial();
      
      // replace base material with scaled version
      ScaledFemMaterial smat = new ScaledFemMaterial (oldMat, 1.0);
      smat.setScalingField (density, true);
      fem.setMaterial (smat);
      
      // compute node mass to check error
      double nodemass = 0;
      for (FemNode3d node : fem.getNodes()) {
         nodemass += node.getMass();
      }
      double elemmass = fem.getMass();

      timer.stop();
      Logger.getSystemLogger().debug("time=" + timer.result(1));
      Logger.getSystemLogger().debug(
         "Mass: " + nodemass + " vs expected " + expectedMass + " vs element " + elemmass);

   }
   
   /**
    * Used to truncate field values to a supplied minimum
    * @param fem model
    * @param field to adjust
    * @param min minimum field value
    */
   private static void adjustFieldMinimum(FemModel3d fem, ScalarFemField field, double min) {
      if (field instanceof ScalarElementField) {
         ScalarElementField efield = (ScalarElementField)field;
         for (FemElement3d elem : fem.getElements ()) {
            double val = efield.getValue (elem);
            if (val < min) {
               efield.setValue (elem, min);
            }
         }
      } else if (field instanceof ScalarSubElemField) {
         ScalarSubElemField efield = (ScalarSubElemField)field;
         for (FemElement3d elem : fem.getElements ()) {
            for (int k=0; k<elem.numAllIntegrationPoints (); ++k) {
               double val = efield.getValue (elem, k);
               if (val < min) {
                  efield.setValue (elem, k, min);
               }
            }
         }
      } else if (field instanceof ScalarNodalField) {
         ScalarNodalField nfield = (ScalarNodalField)field;
         for (FemNode3d node : fem.getNodes ()) {
            double val = nfield.getValue (node);
            if (val < min) {
               nfield.setValue (node, min);
            }
         }
      } else {
         throw new IllegalArgumentException ("Unknown field type " + field.getClass ().getName ());
      }
   }
   
   /**
    * Adjusts mass and stiffness of a FEM model based on
    * a given surface mesh.  Does this by adjusting the node/
    * element masses based on integrating the FEM's
    * density within the mesh volume, and adjusts
    * stiffness by creating a scalar field to modulate
    * the stiffness/tangent contributions.
    * 
    * @param fem model to adjust integration
    */
   public static void adjustMassAndStiffness(FemModel3d fem, PolygonalMesh surface) {

      if (surface == null) {
         surface = fem.getSurfaceMesh();
      }
      
      double expectedMass = surface.computeVolume() * fem.getDensity();

      // signed-distance
      Function3x1 inside = new IsInsideFunctionSD(surface); 
      
      FunctionTimer timer = new FunctionTimer();
      timer.start();

      // compute inside/outside field
      ScalarFemField density = EmbeddedFem.computeNodalField(fem, inside, 
         new LagrangianFemElementSampler (), DEFAULT_SAMPLES);
      
      density.setName ("volume_density");
      fem.addField (density);
      
      // prevent values less than zero
      adjustFieldMinimum (fem, density, 0.0);

      // compute node masses from field
      EmbeddedFem.setMasses(fem, density, fem.getDensity ());
      
      //      // prevent zero stiffness
      //      adjustFieldMinimum (fem, density, 0.01);

      // replace material with a wrapped scaled version
      FemMaterial oldMat = fem.getMaterial();
      
      // replace base material with scaled version
      ScaledFemMaterial smat = new ScaledFemMaterial (oldMat, 1.0);
      smat.setScalingField (density, true);
      fem.setMaterial (smat);

      // compute node mass to check error
      double nodemass = 0;
      for (FemNode3d node : fem.getNodes()) {
         nodemass += node.getMass();
      }
      double elemmass = fem.getMass();

      timer.stop();
      Logger.getSystemLogger().debug("time=" + timer.result(1));
      Logger.getSystemLogger().debug(
         "Mass: " + nodemass + " vs expected " + expectedMass + " vs element " + elemmass);

   }
   
   /**
    * Creates a regular finite element grid that bounds a supplied surface
    * @param fem model to populated, created if null
    * @param mesh surface to bound
    * @param trans orientation of grid
    * @param minRes minimum number of elements along each dimension
    * @param maxElemWidth maximum element width along any dimension
    * @return populated or created model
    */
   public static FemModel3d createBoundingFem(
      FemModel3d fem,
      PolygonalMesh mesh, RigidTransform3d trans, int minRes,
      double maxElemWidth) {
      return createBoundingFem(fem, mesh, trans, minRes, maxElemWidth, 0);
   }
   
   /**
    * Creates a FEM model that bounds a given surface
    * @param fem model to populate, one is created if null
    * @param mesh surface to bound
    * @param trans orientation of bounding model (or uses OBB of mesh if null)
    * @param minRes minimum number of elements along any direction
    * @param maxElemWidth maximum width of an element along any direction
    * @param margin extra space in model to surround mesh
    * @return created or populated model
    */
   public static FemModel3d createBoundingFem(
      FemModel3d fem, PolygonalMesh mesh, RigidTransform3d trans, int minRes,
      double maxElemWidth, double margin) {
      
      if (fem == null) {
         fem = new FemModel3d();
      }

      if (trans == null) {
         OBB obb = new OBB(mesh);
         trans = obb.getTransform();
      }

      Point3d min =
         new Point3d(
            Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
            Double.POSITIVE_INFINITY);
      Point3d max =
         new Point3d(
            Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY,
            Double.NEGATIVE_INFINITY);
      Point3d pnt = new Point3d();
      for (Vertex3d vtx : mesh.getVertices()) {
         vtx.getWorldPoint(pnt);
         pnt.inverseTransform(trans);
         pnt.updateBounds(min, max);
      }
      Vector3d widths = new Vector3d();
      widths.sub(max, min);
      Vector3d offset = new Vector3d();
      offset.interpolate(max, 0.5, min);

      double mini = widths.minElement();

      double dd = mini / minRes;

      if (maxElemWidth > 0 && dd > maxElemWidth) {
         dd = maxElemWidth;
      }
      
      if (margin < 0) {
         margin = dd / 2;
      }

      // add a little bit
      widths.add(2*margin, 2*margin, 2*margin);

      int[] res = new int[3];
      res[0] = (int)(Math.round(widths.x / dd));
      res[1] = (int)(Math.round(widths.y / dd));
      res[2] = (int)(Math.round(widths.z / dd));

      FemFactory.createHexGrid(
         fem, widths.x, widths.y, widths.z, res[0], res[1], res[2]);
      // transform to put origin back
      fem.transformGeometry(new RigidTransform3d(offset, AxisAngle.IDENTITY));
      fem.transformGeometry(trans);
      return fem;
   }
   
   /**
    * Creates a FEM model that bounds a given surface
    * @param fem model to populate, one is created if null
    * @param mesh surface to bound
    * @param trans orientation of bounding model (or uses OBB of mesh if null)
    * @param res element resolution
    * @return created or populated model
    */
   public static FemModel3d createBoundingFem(
      FemModel3d fem, PolygonalMesh mesh, RigidTransform3d trans, Vector3i res) {
      return createBoundingFem(fem, mesh, trans, res, -1);
   }
   
   /**
    * Creates a FEM model that bounds a given surface
    * @param fem model to populate, one is created if null
    * @param mesh surface to bound
    * @param trans orientation of bounding model (or uses OBB of mesh if null)
    * @param res element resolution
    * @param margin extra space in model to surround mesh
    * @return created or populated model
    */
   public static FemModel3d createBoundingFem(
      FemModel3d fem, PolygonalMesh mesh, RigidTransform3d trans, 
      Vector3i res, double margin) {
    
      if (fem == null) {
         fem = new FemModel3d();
      }

      if (trans == null) {
         OBB obb = new OBB(mesh);
         trans = obb.getTransform();
      }

      Point3d min =
         new Point3d(
            Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
            Double.POSITIVE_INFINITY);
      Point3d max =
         new Point3d(
            Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY,
            Double.NEGATIVE_INFINITY);
      Point3d pnt = new Point3d();
      for (Vertex3d vtx : mesh.getVertices()) {
         vtx.getWorldPoint(pnt);
         pnt.inverseTransform(trans);
         pnt.updateBounds(min, max);
      }
      Vector3d widths = new Vector3d();
      widths.sub(max, min);
      Vector3d offset = new Vector3d();
      offset.interpolate(max, 0.5, min);

      if (margin < 0) {
         // half a diagonal
         double dx = widths.x/res.x;
         double dy = widths.y/res.y;
         double dz = widths.z/res.z;
         double dr = Math.sqrt (dx*dx+dy*dy+dz*dz);
         margin = dr / 2;
      }

      // add a little bit
      widths.add(2*margin, 2*margin, 2*margin);

      FemFactory.createHexGrid(
         fem, widths.x, widths.y, widths.z, res.x, res.y, res.z);
      // transform to put origin back
      fem.transformGeometry(new RigidTransform3d(offset, AxisAngle.IDENTITY));
      fem.transformGeometry(trans);
      return fem;
   }

   /**
    * Creates a FEM model that surrounds a given surface, using a 
    * lego-brick-like pattern (like voxels). Note that properties
    * of the FEM are NOT adjusted to account for empty space along
    * the boundary elements.  For that, use {@link #adjustMassAndStiffness(FemModel3d, PolygonalMesh, int)}
    * 
    * @param fem model to populate (one is created if null)
    * @param mesh mesh to surround/voxelize
    * @param trans orientation for voxels (uses OBB of mesh if null)
    * @param minRes minimum number of elements along any direction
    * @param maxElemWidth maximum element with along any direction
    * @return populated or created FEM
    */
   public static FemModel3d createVoxelizedFem(
      FemModel3d fem, PolygonalMesh mesh,
      RigidTransform3d trans, int minRes, double maxElemWidth) {
      return createVoxelizedFem(
         fem, mesh, trans, minRes, maxElemWidth, 0);
   }

   /**
    * Creates a FEM model that surrounds a given surface, using a 
    * lego-brick-like pattern (like voxels). Note that properties
    * of the FEM are NOT adjusted to account for empty space along
    * the boundary elements.  For that, use {@link #adjustMassAndStiffness(FemModel3d, PolygonalMesh, int)}
    * 
    * @param fem model to populate (one is created if null)
    * @param mesh mesh to surround/voxelize
    * @param trans orientation for voxels (uses OBB of mesh if null)
    * @param minRes minimum number of elements along any direction
    * @param maxElemWidth maximum element with along any direction
    * @param margin margin for surrounding mesh
    * @return populated or created FEM
    */
   public static FemModel3d createVoxelizedFem(
      FemModel3d fem, PolygonalMesh mesh,
      RigidTransform3d trans, int minRes, double maxElemWidth, double margin) {

      fem = createBoundingFem(fem, mesh, trans, minRes, maxElemWidth, margin);
      removeOutsideElements (fem, mesh, DEFAULT_SAMPLES, margin);
      
      return fem;
   }
   
   /**
    * Creates a FEM model that surrounds a given surface, using a 
    * lego-brick-like pattern (like voxels). Note that properties
    * of the FEM are NOT adjusted to account for empty space along
    * the boundary elements.  For that, use {@link #adjustMassAndStiffness(FemModel3d, PolygonalMesh, int)}
    * 
    * @param fem model to populate (one is created if null)
    * @param mesh mesh to surround/voxelize
    * @param trans orientation for voxels (uses OBB of mesh if null)
    * @param res element resolution for original grid
    * @return populated or created FEM
    */
   public static FemModel3d createVoxelizedFem(
      FemModel3d fem, PolygonalMesh mesh,
      RigidTransform3d trans, Vector3i res) {
      return createVoxelizedFem (fem, mesh, trans, res, -1);
   }
   
   /**
    * Creates a FEM model that surrounds a given surface, using a 
    * lego-brick-like pattern (like voxels). Note that properties
    * of the FEM are NOT adjusted to account for empty space along
    * the boundary elements.  For that, use {@link #adjustMassAndStiffness(FemModel3d, PolygonalMesh, int)}
    * 
    * @param fem model to populate (one is created if null)
    * @param mesh mesh to surround/voxelize
    * @param trans orientation for voxels (uses OBB of mesh if null)
    * @param res element resolution for original grid
    * @param margin margin for surrounding mesh
    * @return populated or created FEM
    */
   public static FemModel3d createVoxelizedFem(
      FemModel3d fem, PolygonalMesh mesh,
      RigidTransform3d trans, Vector3i res, double margin) {

      fem = createBoundingFem(fem, mesh, trans, res, margin);
      removeOutsideElements (fem, mesh, DEFAULT_SAMPLES, margin);
      
      return fem;
   }
   
   /**
    * Removes elements outside a given mesh plus margin
    * @param fem model to remove elements from
    * @param mesh surface outside which to remove elements
    * @param margin margin distance outside mesh to consider (if {@code < 0},
    * once is computed based on a half element size)
    */
   public static void removeOutsideElements (
      FemModel3d fem, PolygonalMesh mesh, double margin) {
      removeOutsideElements (fem, mesh, DEFAULT_SAMPLES, margin);
   }
   
   /**
    * Removes elements outside a given mesh plus margin
    * @param fem model to remove elements from
    * @param mesh surface outside which to remove elements
    * @param nsamples number of samples to choose from within the element
    * @param margin margin distance outside mesh to consider (if {@code < 0},
    * once is computed based on a half element size)
    */
   public static void removeOutsideElements(FemModel3d fem, PolygonalMesh mesh, int nsamples, double margin) {
      
      // sample to see if element is in volume
      EulerianFemElementSampler sampler = new EulerianFemElementSampler();
      DistanceGrid sd = new DistanceGrid(mesh.getFaces(), 0.1, 50, true);
      Point3d spnt = new Point3d();

      if (margin < 0) {
         if (fem.numElements() > 0) {
            FemElement3d elem = fem.getElement(0);
            Point3d pmin = new Point3d(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
            Point3d pmax = new Point3d(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
            elem.updateBounds(pmin, pmax);
            margin = pmin.distance(pmax)/2; // half-distance
         }
      }
      
      ArrayList<FemElement3d> toRemove = new ArrayList<FemElement3d>();
      for (FemElement3d elem : fem.getElements()) {
         
         // check nodes
         boolean inside = false;
         for (FemNode3d node : elem.getNodes ()) {
            double ds = sd.getLocalDistanceAndNormal(null, node.getPosition ());
            if (ds <= margin) {
               inside = true;
               break;
            }
         }
         
         // check ipnts
         if (!inside) {
            Point3d pos = new Point3d();
            for (IntegrationPoint3d ipnt : elem.getIntegrationPoints ()) {
               ipnt.computePosition (pos, elem);
               double ds = sd.getLocalDistanceAndNormal(null, pos);
               if (ds <= margin) {
                  inside = true;
                  break;
               }
            }
         }
         
         // take a few samples from the element
         if (!inside) {
            sampler.setElement(elem);
            for (int i = 0; i < nsamples; ++i) {
               sampler.sample(spnt);
               double ds = sd.getLocalDistanceAndNormal(null, spnt);
               if (ds <= margin) {
                  inside = true;
                  break;
               }
            }
         }
         
         if (!inside) {
            toRemove.add(elem);
         }
      }
      for (FemElement3d elem : toRemove) {
         fem.removeElement(elem);
      }

      ArrayList<FemNode3d> deleteThese = new ArrayList<>();
      for (FemNode3d node : fem.getNodes()) {
         if (node.numAdjacentElements() == 0) {
            deleteThese.add(node);
         }
      }
      for (FemNode3d node : deleteThese) {
         fem.removeNode(node);
      }

      // re-number nodes and elements
      int idx = 0;
      for (FemNode3d node : fem.getNodes()) {
         node.setNumber(idx++);
      }
      fem.getNodes().invalidateNumbers();

      idx = 0;
      for (FemElement3d elem : fem.getElements()) {
         elem.setNumber(idx++);
      }
      fem.getElements().invalidateNumbers();

   }
   
   
   /**
    * Removes elements containing less than the given fraction of volume
    * @param fem model to modify
    * @param dq distance grid
    * @param margin margin to keep outside distance field
    * @param frac fraction below which to remove elements
    */
   public static void removeFractionalElements(
      FemModel3d fem, DistanceGrid dq, double margin, double frac) {
      
      // sample to see if element is in volume
      EulerianFemElementSampler sampler = new EulerianFemElementSampler();
      int nsamples = DEFAULT_SAMPLES;
      Point3d spnt = new Point3d();

      if (margin < 0) {
         if (fem.numElements() > 0) {
            FemElement3d elem = fem.getElement(0);
            Point3d pmin = new Point3d(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
            Point3d pmax = new Point3d(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
            elem.updateBounds(pmin, pmax);
            margin = pmin.distance(pmax)/2; // half-distance
         }
      }
      
      ArrayList<FemElement3d> toRemove = new ArrayList<FemElement3d>();
      int thresh = (int)Math.ceil(frac*nsamples);
      for (FemElement3d elem : fem.getElements()) {
         int in = 0;
         sampler.setElement(elem);
         boolean keep = false;
         
         for (int i = 0; i < nsamples; ++i) {
            sampler.sample(spnt);
            double ds = dq.getLocalDistance(spnt);
            if (ds <= margin) {
               ++in;
               if (in > thresh) {
                  keep = true;
                  break;
               }
            }
         }

         if (!keep) {
            toRemove.add(elem);
         }
      }
      
      for (FemElement3d elem : toRemove) {
         fem.removeElement(elem);
      }

      ArrayList<FemNode3d> deleteThese = new ArrayList<>();
      for (FemNode3d node : fem.getNodes()) {
         if (node.numAdjacentElements() == 0) {
            deleteThese.add(node);
         }
      }
      for (FemNode3d node : deleteThese) {
         fem.removeNode(node);
      }

      // re-number nodes and elements
      int idx = 0;
      for (FemNode3d node : fem.getNodes()) {
         node.setNumber(idx++);
      }
      fem.getNodes().invalidateNumbers();

      idx = 0;
      for (FemElement3d elem : fem.getElements()) {
         elem.setNumber(idx++);
      }
      fem.getElements().invalidateNumbers();

   }
   
   /**
    * Divide boundary hex elements and remove portions unused
    * @param fem model to trim
    * @param mesh surface for determining inside/outside
    * @param margin margin to preserve around model
    */
   public static void trimBoundaryHexes (
      FemModel3d fem, PolygonalMesh mesh, double margin) {
      
      DistanceGrid dg = mesh.getSignedDistanceGrid ();
      if (dg == null) {
         Vector3i cellDivisions = new Vector3i (32, 32, 32);
         double gridMargin = 0.1;
         dg = mesh.getSignedDistanceGrid (gridMargin, cellDivisions);
      }   
      trimBoundaryHexes(fem, dg, margin);
   }
   
   private static boolean hasTriFace(FemNode3d n0, FemNode3d n1, FemNode3d n2) {
      for (FemElement3d elem : n0.getAdjacentVolumeElements ()) {
         if (!(elem instanceof HexElement)) {
            int[] faces = elem.getFaceIndices ();
            FemNode3d[] nodes = elem.getNodes ();
            for (int i=0; i<faces.length;) {
               int nverts = faces[i];
               if (nverts == 3) {
                  int v0 = faces[i+1];
                  int v1 = faces[i+2];
                  int v2 = faces[i+3];
                  
                  if (  (nodes[v0] == n0 && nodes[v1] == n1 && nodes[v2] == n2)
                     || (nodes[v0] == n1 && nodes[v1] == n2 && nodes[v2] == n0)
                     || (nodes[v0] == n2 && nodes[v1] == n0 && nodes[v2] == n1)){
                     return true;
                  }
               }
               
               i += (nverts+1);
            }
         }
      }
      
      return false;
   }
   
   /**
    * Divide boundary hex elements and remove portions unused
    * @param fem model to trim
    * @param sd signed-distance field for determining inside/outside
    * @param margin margin to preserve around model
    */
   public static void trimBoundaryHexes(
      FemModel3d fem, DistanceGrid sd, double margin) {
      
      ArrayList<FemElement3d> toRemove = new ArrayList<FemElement3d>();
      ArrayList<FemElement3d> toAdd = new ArrayList<FemElement3d>();
      
      for (FemElement3d elem : fem.getElements()) {
         if (elem instanceof HexElement) {
            if (HexTrimmer.trim((HexElement)elem, margin, sd, toAdd)) {
               toRemove.add(elem);
            }
         }
      }
      
      for (FemElement3d elem : toRemove) {
         fem.removeElement(elem);
      }
      toRemove.clear();
      
      for (FemElement3d elem : toAdd) {
         fem.addElement(elem);
      }
      toAdd.clear();

      ArrayList<FemNode3d> deleteThese = new ArrayList<>();
      for (FemNode3d node : fem.getNodes()) {
         if (node.numAdjacentElements() == 0) {
            deleteThese.add(node);
         }
      }
      for (FemNode3d node : deleteThese) {
         fem.removeNode(node);
      }
      deleteThese.clear();

      // re-number nodes and elements
      int idx = 0;
      for (FemNode3d node : fem.getNodes()) {
         node.setNumber(idx++);
      }
      fem.getNodes().invalidateNumbers();

      idx = 0;
      for (FemElement3d elem : fem.getElements()) {
         elem.setNumber(idx++);
      }
      fem.getElements().invalidateNumbers();
      
      // check nodes
      int nidx = 0;
      for (FemNode3d node : fem.getNodes ()) {
         idx = node.getNumber ();
         if (idx != nidx) {
            System.out.println ("funky node");
         }
         ++nidx;
      }
      
      // check elements
      nidx = 0;
      for (FemElement3d elem : fem.getElements ()) {
         idx = elem.getNumber ();
         if (idx != nidx) {
            System.out.println ("funky element");
         }
         for (FemNode3d node : elem.getNodes ()) {
            if (node.getNumber () == -1) {
               System.out.println ("broken element");
            }
         }
         ++nidx;
      }
      
   }
   
   /**
    * Restores element face-face compatibility by inserting pyramid and tet elements
    * @param fem model to fix
    */
   public static void restoreFaceCompatibility(FemModel3d fem) {

      ArrayList<FemElement3d> toRemove = new ArrayList<>();
      ArrayList<FemElement3d> toAdd = new ArrayList<> ();
      
      for (FemElement3d elem : fem.getElements ()) {
         if (elem instanceof HexElement) {
            // check opposite all 8 faces, look for triangles
            int[] faces = elem.getFaceIndices ();
            FemNode3d[] nodes = elem.getNodes ();
            int ntris = 0;
            for (int i=0; i<faces.length;) {
               int nverts = faces[i];
               if (nverts == 4) {
                  int v0 = faces[i+1];
                  int v1 = faces[i+2];
                  int v2 = faces[i+3];
                  int v3 = faces[i+4];
                  
                  // check triangles
                  if (hasTriFace (nodes[v0], nodes[v2], nodes[v1]) 
                     || hasTriFace (nodes[v0], nodes[v3], nodes[v2])) {
                     ++ntris;
                  } else if (hasTriFace (nodes[v0], nodes[v3], nodes[v1]) 
                      || hasTriFace (nodes[v1], nodes[v3], nodes[v2])) {
                     ++ntris;
                  }
                  
               }
               i += (nverts+1);
            }
            
            if (ntris > 0) {
               // split
               Point3d c = new Point3d();
               elem.computeCentroid (c);
               toRemove.add (elem);
               FemNode3d cnode = new FemNode3d(c);
               fem.addNode (cnode);
               
               for (int i=0; i<faces.length;) {
                  int nverts = faces[i];
                  if (nverts == 4) {
                     int v0 = faces[i+1];
                     int v1 = faces[i+2];
                     int v2 = faces[i+3];
                     int v3 = faces[i+4];
                     
                     // check triangles
                     if (hasTriFace (nodes[v0], nodes[v2], nodes[v1]) 
                        || hasTriFace (nodes[v0], nodes[v3], nodes[v2])) {
                        toAdd.add (new TetElement (nodes[v0], nodes[v2], nodes[v1], cnode));
                        toAdd.add (new TetElement (nodes[v0], nodes[v3], nodes[v2], cnode));
                     } else if (hasTriFace (nodes[v0], nodes[v3], nodes[v1]) 
                         || hasTriFace (nodes[v1], nodes[v3], nodes[v2])) {
                        toAdd.add (new TetElement (nodes[v0], nodes[v3], nodes[v1], cnode));
                        toAdd.add (new TetElement (nodes[v1], nodes[v3], nodes[v2], cnode));
                     } else {
                        toAdd.add (new PyramidElement (nodes[v0], nodes[v3], nodes[v2], nodes[v1], cnode));
                     }
                     
                  } // 4 vertices
                  i += (nverts+1);
               } // faces
            } // splitting element 
         } // hex element
      } // elements
      
      for (FemElement3d elem : toRemove) {
         fem.removeElement (elem);
      }
      fem.addElements (toAdd);
      
      // check nodes
      int nidx = 0;
      for (FemNode3d node : fem.getNodes ()) {
         int idx = node.getNumber ();
         if (idx != nidx) {
            System.out.println ("funky node");
         }
         ++nidx;
      }
      
      nidx = 0;
      for (FemElement3d elem : fem.getElements ()) {
         //         int idx = elem.getNumber ();
         //         if (idx != nidx) {
         //            System.out.println ("funky element");
         //         }
         for (FemNode3d node : elem.getNodes ()) {
            if (node.getNumber () == -1) {
               System.out.println ("broken element");
            }
         }
         ++nidx;
      }
   }

}
