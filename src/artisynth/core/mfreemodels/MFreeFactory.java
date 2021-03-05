/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mfreemodels;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import artisynth.core.femmodels.FemElement;
import artisynth.core.femmodels.FemElement3d;
import artisynth.core.femmodels.FemMeshComp;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.FemNode;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.femmodels.IntegrationData3d;
import artisynth.core.femmodels.IntegrationPoint3d;
import artisynth.core.femmodels.PointFem3dAttachment;
import artisynth.core.mechmodels.DynamicComponent;
import artisynth.core.mechmodels.PointAttachment;
import artisynth.core.mechmodels.PointParticleAttachment;
import artisynth.core.mfreemodels.RadialWeightFunction.RadialWeightFunctionType;
import maspack.geometry.AABBTree;
import maspack.geometry.BVNode;
import maspack.geometry.BVTree;
import maspack.geometry.Boundable;
import maspack.geometry.DistanceGrid;
import maspack.geometry.Face;
import maspack.geometry.KDComparator;
import maspack.geometry.KDTree;
import maspack.geometry.KDTree3d;
import maspack.geometry.MeshBase;
import maspack.geometry.MeshFactory;
import maspack.geometry.MeshFactory.FaceType;
import maspack.geometry.OBB;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.TetgenTessellator;
import maspack.geometry.Vertex3d;
import maspack.matrix.Matrix3d;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.matrix.Vector3i;
import maspack.matrix.VectorNd;
import maspack.util.DynamicArray;
import maspack.util.FastRadialMarcher;
import maspack.util.FunctionTimer;
import maspack.util.IndexedBinaryHeap;
import maspack.util.Logger;
import maspack.util.Point3dGridUtility;

public class MFreeFactory {

   public static double DEFAULT_TOLERANCE = 1e-10;
   public static int DEFAULT_IPNT_FACTOR = 3;      // for every node, pick N ipnts along each dimension
   public static int DEFAULT_MINIMUM_DEPENDENCIES = 4;
   
   public static RadialWeightFunctionType DEFAULT_RADIAL_KERNEL_TYPE = 
      RadialWeightFunctionType.SPLINE;
   
   public static MFreeModel3d createBeam(MFreeModel3d model,
      double[] size, int res[]) {
      return createBeam(model, DEFAULT_RADIAL_KERNEL_TYPE, 
         size, res);
   }
   
   public static MFreeModel3d createBeam(MFreeModel3d model,
      RadialWeightFunctionType fType,
      double[] size, int res[]) {

      // 4 ipnts along each dimension for every node 
      int ires[] = new int[3];
      for (int i = 0; i < 3; i++) {
         ires[i] = res[i] * DEFAULT_IPNT_FACTOR;
      }
      return createBeam(model, fType, size, res, ires);
   }
   
   public static MFreeModel3d createBeam(MFreeModel3d model,
      RadialWeightFunctionType fType,
      double[] size, int res[], int[] ipntRes) {

      // nodes
      double x, y, z;
      double dx = size[0] / res[0];
      double dy = size[1] / res[1];
      double dz = size[2] / res[2];

      Point3d[] nodeLocs = new Point3d[(res[0]+1)*(res[1]+1)*(res[2]+1)];
      int nidx = 0;
         
      // nodes
      for (int i = 0; i <= res[0]; i++) {
         for (int j = 0; j <= res[1]; j++) {
            for (int k = 0; k <= res[2]; k++) {
               x = -size[0] / 2 + dx * i;
               y = -size[1] / 2 + dy * j;
               z = -size[2] / 2 + dz * k;

               nodeLocs[nidx] = new Point3d(x,y,z);
               ++nidx;
            }
         }
      }

      // surface mesh
      PolygonalMesh surface =
         MeshFactory.createBox(
            size[0], size[1], size[2], Point3d.ZERO, 
            res[0], res[1], res[2], 
            true, FaceType.TRI);

      // offset ipnts from border
      dx = size[0] / ipntRes[0];
      dy = size[1] / ipntRes[1];
      dz = size[2] / ipntRes[2];
      double[] ipntSize = { size[0] - dx, size[1] - dy, size[2] - dz };
      Point3d[] pnts = PointDistributor.getGrid(ipntSize, ipntRes, Point3d.ZERO);

      //      CubaturePoint3d[] cpnts = new CubaturePoint3d[pnts.length];
      //      double wp = surface.computeVolume() / pnts.length; // equal weighting
      //      for (int i = 0; i < pnts.length; i++) {
      //         cpnts[i] = new CubaturePoint3d(pnts[i], wp);
      //      }
      return createModel(model, nodeLocs, pnts, surface, fType);

   }
   
   public static MFreeModel3d createModel(MFreeModel3d model, PolygonalMesh surface, 
      RigidTransform3d pos, int nNodes, int nIPnts) {
      return createModel(model, surface, pos, nNodes, nIPnts, null, DEFAULT_RADIAL_KERNEL_TYPE);
   }
   
   public static MFreeModel3d createModel(MFreeModel3d model, PolygonalMesh surface, 
      RigidTransform3d transform, int nNodes, int nIPnts, int[] gridRes, RadialWeightFunctionType fType) {
      
      if (model == null) {
         model = new MFreeModel3d();
      }
      if (transform != null) {
         surface.transform(transform);   
      }
      
      if (gridRes == null) {
         OBB meshObb = new OBB (surface);
         Vector3d hw = new Vector3d(meshObb.getHalfWidths());
         double vol = hw.x*hw.y*hw.z*8;
         double ipntVol = vol/nIPnts;
         
         // find rough dimensions of one ipnt, same proportions
         // as root OBB
         double scale = Math.pow(ipntVol/vol, 0.333);
         
         // we want at least 20? points along each dimension,
         // find minimum distance
         double min = hw.minElement();
         double minFactor = 1.0/scale*15;
         int nx = (int)Math.ceil(minFactor*hw.x/min);
         int ny = (int)Math.ceil(minFactor*hw.y/min);
         int nz = (int)Math.ceil(minFactor*hw.z/min);
         
         gridRes = new int[]{nx,ny,nz};
      }
      
      surface.triangulate();

      Point3d[] ipntLocs = generatePointLocations(surface, gridRes, nIPnts);
      Point3d[] nodeLocs = Arrays.copyOf(ipntLocs, nNodes); // cut first section

      createModel(model, nodeLocs, ipntLocs, surface, fType);
      
      return model;
   }
   
   public static Point3d[] generatePointLocations(PolygonalMesh mesh, int[] res, int nPoints) {

      Vector3d centroid = new Vector3d();
      mesh.computeCentroid(centroid);
      RigidTransform3d trans = new RigidTransform3d();
      trans.setTranslation(centroid);

      OBB obb = PointDistributor.getTightOBB(mesh, trans);
      Vector3d widths = new Vector3d();
      obb.getWidths(widths);
      obb.getTransform(trans);

      int nx = res[0];
      int ny = res[1];
      int nz = res[2];

      double dx, dy, dz;
      double xOffset, yOffset, zOffset;
      if (nx == 1) {
         widths.x = 0;
         dx = 0;
      } else {
         dx = widths.x / (nx - 1);
      }
      if (ny == 1) {
         widths.y = 0;
         dy = 0;
      } else {
         dy = widths.y / (ny - 1);
      }
      if (nz == 1) {
         widths.z = 0;
         dz = 0;
      } else {
         dz = widths.z / (nz - 1);
      }
      xOffset = -widths.x / 2;
      yOffset = -widths.y / 2;
      zOffset = -widths.z / 2;

      // generate a grid of points that fall inside domain
      Point3d[][][] pnts = new Point3d[nx][ny][nz];

      // BVFeatureQuery query = new BVFeatureQuery();
      Vector3i sdres = new Vector3i(2*nx, 2*ny, 2*nz);
      sdres.x = Math.min(sdres.x, 30);
      sdres.y = Math.min(sdres.y, 30);
      sdres.z = Math.min(sdres.z, 30);
      
      Logger.getSystemLogger().debug("Creating signed distance grid");
      List<Face> faces = mesh.getFaces();
      DistanceGrid sdgrid = new DistanceGrid(faces, 0.1, sdres, true);
      Logger.getSystemLogger().debug("done");
      double tol = 1e-15;

      Logger.getSystemLogger().debug("Generating " + nx + "x" + ny + "x" + nz + " points");
      double x, y, z;
      for (int i = 0; i < nx; i++) {
         x = xOffset + i * dx;
         for (int j = 0; j < ny; j++) {
            y = yOffset + j * dy;
            for (int k = 0; k < nz; k++) {
               z = zOffset + k * dz;
               Point3d pnt = new Point3d(x, y, z);
               pnt.transform(trans);
               
               double d = sdgrid.getLocalDistanceAndNormal(null, null, pnt);
               if (d < tol) {
                  pnts[i][j][k] = pnt;
               } else {
                  pnts[i][j][k] = null;
               }

               //               InsideQuery rayTest = query.isInsideMesh(mesh, pnt, tol);
               //               if (rayTest == InsideQuery.INSIDE) {
               //                  pnts[i][j][k] = pnt;
               //               } else if (rayTest == InsideQuery.OUTSIDE) {
               //                  pnts[i][j][k] = null;
               //               } else {
               //                  System.out.println("unsure");
               //               }
            }
         }
      }
      Logger.getSystemLogger().debug("done.");

      Logger.getSystemLogger().debug("Farthest point sampling...");
      Point3dGridUtility pgu = new Point3dGridUtility(pnts);
      FastRadialMarcher marcher = new FastRadialMarcher(nx * ny * nz, pgu);

      marcher.initializeArrays();
      double[] dists = marcher.getDistance();
      // mark null points as having distance of -1 (never to be selected)
      for (int i = 0; i < nx; i++) {
         for (int j = 0; j < ny; j++) {
            for (int k = 0; k < nz; k++) {
               if (pnts[i][j][k] == null) {
                  dists[k + j * nz + i * ny * nz] = -1;
               }
            }
         }
      }
      
      // initialize heap to include all indices
      marcher.getDistanceHeap().setAll();

      // farthest-point sampling
      Point3d[] out = new Point3d[nPoints];
      int i, j, k;
      for (int idx = 0; idx < nPoints; idx++) {
         int farthest = 0;
         IndexedBinaryHeap dheap = marcher.getDistanceHeap();
         farthest = dheap.peek();
         int nextSample = farthest;  // get next furthest

         // idx = i*ny*nz+j*nz+k
         k = nextSample % nz;
         j = ((nextSample - k) / nz) % ny;
         i = (nextSample - k - j * nz) / (ny * nz);

         out[idx] = pnts[i][j][k];
         marcher.march(nextSample);  // update distances to first point
      }
      Logger.getSystemLogger().debug("done.");
      
      return out;
   }

   /**
    * Creates a cylinder by distributing nodes and points radially
    * 
    * @param model model to which the elements should be added, or
    * <code>null</code> if the model is to be created from scratch.
    * @param h length along the z axis
    * @param r radius in the x-y plane
    * @param nh node resolution along the length
    * @param nr node resolution along the radius (including center)
    * @return created FEM model
    */
   public static MFreeModel3d createCylinder(
      MFreeModel3d model, double h, double r, int nh, int nr) {
      
      int nt1 = 6;
      
      PolygonalMesh surface = MeshFactory.createCylinder (r, h, nt1*(nr-1), nr-1, nh-1);
      
      double dr = r/(nr-1);
      double zmin = -h/2;
      double dh = h/(nh-1);
      
      // axis nodes
      int nidx = 0;
      int nnodes = nh*(1 + nt1*nr*(nr-1)/2);
      Point3d[] nodeLocs = new Point3d[nnodes];
      for (int k=0; k<nh; ++k) {
         nodeLocs[nidx++] = new Point3d(0,0,zmin+k*dh);
      }
      
      // circle nodes
      for (int i=1; i<nr; ++i) {
         // next layer of nodes
         double rr = dr*i;
         int nt = nt1*i;
         double dt = 2*Math.PI/nt;
         for (int k=0; k<nh; ++k) {
            double z = zmin+k*dh;
            for (int j=0; j<nt; ++j) {
               double theta = j*dt;
               double x = rr*Math.cos (theta);
               double y = rr*Math.sin (theta);
               nodeLocs[nidx++] = new Point3d(x,y,z);
            }
         }
      }
      
      // generate ipnts
      
      int ipntFactor = DEFAULT_IPNT_FACTOR;
      int nih = ipntFactor*(nh-1);
      // int nir = ipntFactor*(nr-1)+1;
      int nir = ipntFactor*(nr-1);
      
      double dih = h/(nih);
      zmin += dih/2;
      // double dir = r/(nir-1);
      double dir = r/nir;
      double rmin = dir/2;
      
      int iidx = 0;
      // int nipnts = nih*(1 + nt1*nir*(nir-1)/2);
      int nipnts = nih*(nt1*(nir+1)*(nir)/2);
      CubaturePoint3d[] cpnts = new CubaturePoint3d[nipnts];
      
      //      for (int k=0; k<nih; ++k) {
      //         cpnts[iidx++] = new CubaturePoint3d(0,0,zmin+k*dih, Math.PI*dir*dir*dih);
      //      }
      
      // circle pnts
      for (int i=0; i<nir; ++i) {
         // next layer of pnts
         double rr = dir*i + rmin;
         int nit = nt1*(i+1);
         double dt = 2*Math.PI/nit;
         
         // area of circular section
         double iv = rr*dir*dt*dih;
         //         if (i == (nir-1)) {
         //            iv = (rr*dir/2 - dir*dir/4)*dt*dih;
         //         }
        
         for (int k=0; k<nih; ++k) {
            double z = zmin+k*dih;
            for (int j=0; j<nit; ++j) {
               double theta = j*dt;
               double x = rr*Math.cos (theta);
               double y = rr*Math.sin (theta);
               cpnts[iidx++] = new CubaturePoint3d(x,y,z,iv);
            }
         }
      }
      
      //      CubaturePoint3d[] cpnts2 = computeVoronoiCubature (cpnts, surface);
      //      for (int i=0; i<cpnts2.length; ++i) {
      //         double d = cpnts2[i].w - cpnts[i].w;
      //         if (Math.abs(d) > 1e-18) {
      //            System.out.println ("debug...");
      //         }
      //      }
      double vol = surface.computeVolume ();
      double cvol = 0;
      for (CubaturePoint3d cpnt : cpnts) {
         cvol += cpnt.w;
      }
      if (Math.abs (cvol-vol) > 1e-18) {
         System.out.println ("Volume error: " + vol + " vs " + cvol);
      }
      
      
      return createModel(model, nodeLocs, cpnts, surface);
   }
   
   /**
    * Generate layers of divided octadecahedral spheres
    * @param r radius of outer sphere
    * @param nr number of radial layers
    * @param ngt number of latitude groups
    * @param ngs number of longitude groups
    * @param pnts output populated locations
    * @param idx initial offset into output array
    * @return next index in array to add to (after last added point)
    */
   private static int generateOctadecahedralLayers(double r, int nr, int ngt, int ngs, Point3d[] pnts, int idx) {
      
      // center
      pnts[idx++] = new Point3d(0,0,0);
            
      // distribute nodes radially
      double dr = r/nr;

      // radial layers
      for (int k = 1; k<=nr; ++k) {
         int nt = ngt*k;
         double rr = k*dr;
         double dphi = Math.PI/nt;

         // north pole
         pnts[idx++] = new Point3d(0,0,rr);

         // first latitude group grows
         {
            int gt = 0;
            for (int i=1; i<k; ++i) {
               int t = gt*k+i;
               int ns = ngs*i;

               double phi = t*dphi;
               double z = rr*Math.cos (phi);
               double xy = rr*Math.sin (phi);
               double dtheta = 2*Math.PI/ns;  

               // longitude groups
               for (int gs=0; gs<ngs; ++gs) {
                  for (int j=0; j<i; ++j) {
                     int s = gs*i+j;
                     double theta = s*dtheta;
                     double x = xy*Math.cos (theta);
                     double y = xy*Math.sin (theta);
                     pnts[idx++] = new Point3d(x,y,z);
                  }
               }
            }
         }

         // middle groups constant width
         for (int gt=1; gt<ngt-1; ++gt) {
            for (int i=0; i<k; ++i) {
               int t = gt*k+i;
               int ns = ngs*k;

               double phi = t*dphi;
               double z = rr*Math.cos (phi);
               double xy = rr*Math.sin (phi);
               double dtheta = 2*Math.PI/ns;  

               // longitude groups
               for (int gs=0; gs<ngs; ++gs) {
                  for (int j=0; j<k; ++j) {
                     int s = gs*k+j;
                     double theta = s*dtheta;
                     double x = xy*Math.cos (theta);
                     double y = xy*Math.sin (theta);
                     pnts[idx++] = new Point3d(x,y,z);
                  }
               }
            }
         }

         // last group shrinks
         {
            int gt = ngt-1;
            for (int i=0; i<k; ++i) {
               int t = gt*k+i;
               int ns = ngs*(k-i);

               double phi = t*dphi;
               double z = rr*Math.cos (phi);
               double xy = rr*Math.sin (phi);
               double dtheta = 2*Math.PI/ns;  

               // longitude groups
               for (int gs=0; gs<ngs; ++gs) {
                  for (int j=0; j<k-i; ++j) {
                     int s = gs*(k-i)+j;
                     double theta = s*dtheta;
                     double x = xy*Math.cos (theta);
                     double y = xy*Math.sin (theta);
                     pnts[idx++] = new Point3d(x,y,z);
                  }
               }
            }
         }

         // south pole
         pnts[idx++] = new Point3d(0,0,-rr);
      }
      
      return idx;
   }
   
   /**
    * Creates an MFree model of a sphere, distributing nodes and integration points
    * using subdivided octadecahedrals.
    * 
    * @param model model to populate, created if null
    * @param r radius
    * @param nr number of radial layers, including the center
    * @return created model
    */
   public static MFreeModel3d createSphere(MFreeModel3d model, double r, int nr) {
      
      --nr; // exclude center
      
      // distribute nodes and ipnts using uniform hexagonal distribution
      if (nr < 1) {
         r = 1;
      }
         
      int ngs = 6;
      int ngt = 3;
      
      int nnodes = nr*(nr+1)/2*(2*nr+1)/3*ngs*(ngt-1)+2*nr+1;
      Point3d[] nodeLocs = new Point3d[nnodes];
      generateOctadecahedralLayers (r, nr, ngt, ngs, nodeLocs, 0);
            
      // integration points
      int ipntFactor = DEFAULT_IPNT_FACTOR;
      int nir = ipntFactor*nr;
      
      int nipnts = nir*(nir+1)/2*(2*nir+1)/3*ngs*(ngt-1)+2*nir+1;
      Point3d[] ipntLocs = new Point3d[nipnts];
      generateOctadecahedralLayers (r, nir, ngt, ngs, ipntLocs, 0);
            
      PolygonalMesh surface = MeshFactory.createOctadecahedralSphere(r, nr);
      
      return createModel(model, nodeLocs, ipntLocs, surface);
      
   }
   
   public static MFreeModel3d createModel(MFreeModel3d model,
      Point3d[] nodeLocs, Point3d[] ipntLoc, PolygonalMesh surface) {
   
      return createModel(model, nodeLocs, ipntLoc, 
         surface, DEFAULT_RADIAL_KERNEL_TYPE);
      
   }
   
   /**
    * Computes cubature points at a set of provided locations, assigning a weight equal to
    * the volume of the voronoi region about each point
    * @param locs original locations
    * @param surface mesh surface
    * @return cubature points
    */
   public static CubaturePoint3d[] computeVoronoiCubature(Point3d[] locs, PolygonalMesh surface) {
      
      CubaturePoint3d[] cpnts = new CubaturePoint3d[locs.length];
      for (int i=0; i<locs.length; ++i) {
         cpnts[i] = new CubaturePoint3d (locs[i], 0);
      }
      
      Point3d pmin = new Point3d(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
      Point3d pmax = new Point3d(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
      surface.updateBounds (pmin, pmax);
      double eps = pmax.distance (pmin)*1e-18;
      
      // compute tetrahedralization
      TetgenTessellator tesselator = new TetgenTessellator ();
      tesselator.buildFromMeshAndPoints (surface, -1, locs);
      int[] tets = tesselator.getTets ();
      Point3d[] pnts = tesselator.getPoints ();
      
      // kdtree for nearest ipnt look-up
      KDTree3d kdtree = new KDTree3d (Arrays.asList (cpnts));
      
      // map tet points to locs
      CubaturePoint3d[] pntMap = new CubaturePoint3d[pnts.length];
      for (int i=0; i<pnts.length; ++i) {
         CubaturePoint3d nearest = (CubaturePoint3d)kdtree.nearestNeighbourSearch (pnts[i], eps);
         if (nearest.distance (pnts[i]) < eps) {
            pntMap[i] = nearest;
         }
      }
      
      // assign cubature weights
      int tet[] = new int[4];
      for (int i=0; i<tets.length; i+= 4) {
         for (int j=0; j<4; ++j) {
            tet[j] = tets[i+j];
         }
         
         // tet volume
         Vector3d a = new Vector3d();
         a.sub (pnts[tet[0]], pnts[tet[3]]);
         Vector3d b = new Vector3d();
         b.sub (pnts[tet[1]], pnts[tet[3]]);
         Vector3d c = new Vector3d();
         c.sub (pnts[tet[2]], pnts[tet[3]]);
         double v = a.dot (b.cross (c))/6;
         
         // distribute volume
         double evol = 0;  // "extra" volume not from a cubature point
         double nc = 0;    // number of cubature points in tet
         for (int j=0; j<4; ++j) {
            CubaturePoint3d cpnt = pntMap[tet[j]];
            if (cpnt != null) {
               cpnt.w += v/4;
               ++nc;
            } else {
               evol += v/4;
            }
         }
         
         // distribute extra volume
         if (evol != 0) {
            for (int j = 0; j < 4; ++j) {
               CubaturePoint3d cpnt = pntMap[tet[j]];
               if (cpnt != null) {
                  cpnt.w += evol/nc;
               }
            }
         }   
      }
      
      
      return cpnts;
   }
   
   public static MFreeModel3d createModel(MFreeModel3d model,
      Point3d[] nodeLocs, CubaturePoint3d[] ipnts, 
      PolygonalMesh surface, RadialWeightFunctionType fType) {

      if (model == null) {
         model = new MFreeModel3d();
      }

      MFreeNode3d[] nodes = new MFreeNode3d[nodeLocs.length];
      
      // determine best node-radii to use
      double[] nodeRad = computeNodeRadii(nodeLocs, ipnts, surface, 
         DEFAULT_MINIMUM_DEPENDENCIES, 1.01);

      for (int i=0; i<nodeLocs.length; ++i) {
         Point3d pnt = nodeLocs[i];
         MFreeNode3d node = createNode(pnt.x, pnt.y, pnt.z, nodeRad[i], fType);
         nodes[i] = node;
      }
      
      return createModel(model, nodes, surface, ipnts);
   }
   
   public static MFreeModel3d createModel(MFreeModel3d model,
      Point3d[] nodeLocs, Point3d[] ipntLocs, 
      PolygonalMesh surface, RadialWeightFunctionType fType) {

      if (model == null) {
         model = new MFreeModel3d();
      }

      MFreeNode3d[] nodes = new MFreeNode3d[nodeLocs.length];
      
      // determine best node-radii to use
      double[] nodeRad = computeNodeRadii(nodeLocs, ipntLocs, surface, 
         DEFAULT_MINIMUM_DEPENDENCIES, 1.1);

      for (int i=0; i<nodeLocs.length; ++i) {
         Point3d pnt = nodeLocs[i];
         MFreeNode3d node = createNode(pnt.x, pnt.y, pnt.z, nodeRad[i], fType);
         nodes[i] = node;
      }
      
      // XXX assumes equal volume contribution of each ipnt
      CubaturePoint3d[] cpnts = new CubaturePoint3d[ipntLocs.length];
      double wp = surface.computeVolume() / ipntLocs.length;
      for (int i = 0; i < ipntLocs.length; i++) {
         cpnts[i] = new CubaturePoint3d(ipntLocs[i], wp);
      }

      return createModel(model, nodes, surface, cpnts);
   }
   
   //   public static MFreeModel3d createPairedModel(MFreeModel3d model,
   //      Point3d[] nodeLocs, double nodeRad,
   //      Point3d[] ipntLoc, RadialWeightFunctionType fType,
   //      MFreeShapeFunctionType sType, PolygonalMesh surface) {
   //
   //      if (model == null) {
   //         model = new MFreeModel3d();
   //      }
   //
   //      ArrayList<MFreeNode3d> nodeList = new ArrayList<MFreeNode3d>();
   //
   //      for (Point3d pnt : nodeLocs) {
   //         MFreeNode3d node =
   //            createNode(pnt.x, pnt.y, pnt.z, nodeRad, fType);
   //         nodeList.add(node);
   //      }
   //      surface.triangulate();
   //
   //      CubaturePoint3d[] cpnts = new CubaturePoint3d[ipntLoc.length];
   //      double wp = surface.computeVolume() / ipntLoc.length;
   //      for (int i = 0; i < ipntLoc.length; i++) {
   //         cpnts[i] = new CubaturePoint3d(ipntLoc[i], wp);
   //      }
   //
   //      return createPairedModel(model, nodeList, surface, cpnts);
   //
   //   }
   //
   //   public static MFreeModel3d createPairedModel(MFreeModel3d model,
   //      List<MFreeNode3d> nodes, PolygonalMesh surface,
   //      CubaturePoint3d[] cpnts) {
   //
   //      if (model == null) {
   //         model = new MFreeModel3d();
   //      }
   //      
   //      MFreeShapeFunction func = new MLSShapeFunction();
   //
   //      MFreeNode3d[] nodeArray = nodes.toArray(new MFreeNode3d[nodes.size()]);
   //      boolean[][] iChart = buildIntersectionChart(nodeArray);
   //
   //      AABBTree tree = new AABBTree();
   //      tree.build(nodeArray, nodeArray.length);
   //
   //      // compute node coordinates
   //      for (MFreeNode3d node : nodes) {
   //         MFreeNode3d[] deps =
   //            findNodesContaining(node.getRestPosition(), tree, 0);
   //         VectorNd coords = new VectorNd();
   //         getShapeCoords(func, coords, node.getRestPosition(), deps);
   //         node.setDependentNodes(deps, coords);
   //      }
   //      model.addNodes(nodes);
   //
   //      ArrayList<MFreeElement3d> elemList = createPairedElements(func, nodes, iChart);
   //
   //      MFreeIntegrationPoint3d[] ipnts = createIntegrationPoints(func, cpnts, tree);
   //
   //      MFreeElement3d[] elemArray =
   //         elemList.toArray(new MFreeElement3d[elemList.size()]);
   //      AABBTree elemTree = new AABBTree();
   //      elemTree.build(elemArray, elemArray.length);
   //
   //      distributePairedIPoints(elemArray, ipnts, null, elemTree, 0);
   //      trimEmptyElements(elemList);
   //
   //      addWarpingPoints(elemList, tree);
   //
   //      surface = (PolygonalMesh)convertToMFreeMesh(surface, tree, DEFAULT_TOLERANCE);
   //
   //      model.addElements(elemList);
   //      model.setSurfaceMesh(surface);
   //      model.updateNodeMasses(-1, null);
   //
   //      return model;
   //
   //   }
   
   private static class IndexedPoint3d {
      public Point3d pnt;
      public int idx;
      public IndexedPoint3d(Point3d pnt, int idx) {
         this.pnt = pnt;
         this.idx = idx;
      }
   }
   
   private static class MyKdComparator implements KDComparator<IndexedPoint3d> {

      @Override
      public int compare(IndexedPoint3d a, IndexedPoint3d b, int dim) {
         double x = a.pnt.get(dim);
         double y = b.pnt.get(dim);
         
         if ( x < y) {
            return -1;
         } else if ( x > y) {
            return 1;
         }
         return 0;
      }

      @Override
      public double distance(IndexedPoint3d a, IndexedPoint3d b) {
         return a.pnt.distanceSquared(b.pnt);
      }

      @Override
      public double distance(IndexedPoint3d a, IndexedPoint3d b, int dim) {
         double d = a.pnt.get(dim)-b.pnt.get(dim);
         return d*d;
      }
      
   }
   
   private static boolean isCoplanar(List<IndexedPoint3d> pnts) {
      if (pnts.size() <= 3) {
         return true;
      }

      // assumes points do not overlap
      Vector3d v1 = new Vector3d(pnts.get(0).pnt);
      v1.sub(pnts.get(1).pnt);
      double v1n = v1.norm();
      double eps = v1n*1e-5;  // epsilon for plane projection
      v1.scale(1.0/v1n); // normalize
      
      // find non-colinear point
      Vector3d v2 = new Vector3d(pnts.get(0).pnt);
      Vector3d normal = new Vector3d();
      int jl = -1;
      for (int j=2; j<pnts.size(); ++j) {
         v2.sub(pnts.get(0).pnt, pnts.get(j).pnt);
         v2.normalize();
         normal.cross(v1, v2);
         double v3n = normal.norm();
         if (v3n  > 1e-10) {
            normal.scale(1.0/v3n);
            jl = j;
            break;
         }
      }
      
      // colinear
      if (jl < 0) {
         return true;
      }
      
      double d = normal.dot(pnts.get(0).pnt);
      for (int k=jl+1; k<pnts.size(); ++k) {
         double dd = normal.dot(pnts.get(k).pnt)-d;
         if (Math.abs(dd) > eps) {
            return false;
         }
      }
      
      return true;
   }
   
   private static double[] computeNodeRadii(Point3d[] nodes, Point3d[] ipnts, 
      MeshBase mesh, int minK, double marginScale) {
      
      Point3d min = new Point3d(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
      Point3d max = new Point3d(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
      
      for (Point3d node : nodes) {
         node.updateBounds(min, max);
      }
      for (Point3d ipnt : ipnts) {
         ipnt.updateBounds(min, max);
      }
      Vector3d widths = new Vector3d(max);
      widths.sub(min);
      
      IndexedPoint3d[] inodes = new IndexedPoint3d[nodes.length];
      for (int i=0; i<nodes.length; ++i) {
         inodes[i] = new IndexedPoint3d(nodes[i], i);
      }
      KDTree<IndexedPoint3d> kdtree = new KDTree<IndexedPoint3d>(3, Arrays.asList(inodes), new MyKdComparator());
      
      //      KPointDistanceGrid grid = new KPointDistanceGrid(minK, trans, new int[] {30, 30, 30}, widths);
      //      for (Point3d node : nodes) {
      //         grid.addPoint(node);
      //      }
      
      double[] r = new double[nodes.length];
      
      //      int[] closest = new int[minK];
      //      double[] dists = new double[minK];
      
      IndexedPoint3d idpnt = new IndexedPoint3d(null, 0);
      
      for (Point3d node : nodes) {
         idpnt.pnt = node;
         
         List<IndexedPoint3d> nearest = kdtree.nearestNeighbourSearch(idpnt, minK, 0);
         // check for coplanarity
         int kk = minK;
         while (isCoplanar(nearest)) {
            kk += 1;
            nearest = kdtree.nearestNeighbourSearch(idpnt,  kk, 0);
         }
         
         // grid.getClosest(node, minK, closest, dists);
         //         for (int k=0; k<minK; ++k) {
         //            //            if (r[closest[k]] < dists[k]) {
         //            //               r[closest[k]] = dists[k];
         //            //            }
         //         }
         
         for (IndexedPoint3d nbr : nearest) {
            double rl = nbr.pnt.distance(node)*marginScale;
            if (rl > r[nbr.idx]) {
               r[nbr.idx] = rl;
            }
         }
      }
      
      for (Point3d ipnt : ipnts) {
         idpnt.pnt = ipnt;
         List<IndexedPoint3d> nearest = kdtree.nearestNeighbourSearch(idpnt, minK, 0);
         // check for coplanarity
         int kk = minK;
         while (isCoplanar(nearest)) {
            kk += 1;
            nearest = kdtree.nearestNeighbourSearch(idpnt,  kk, 0);
         }
         for (IndexedPoint3d nbr : nearest) {
            double rl = nbr.pnt.distance(ipnt)*marginScale;
            if (rl > r[nbr.idx]) {
               r[nbr.idx] = rl;
            }
         }
      }
      
      if (mesh != null) {
         for (Vertex3d vtx : mesh.getVertices()) {
            idpnt.pnt = vtx.getWorldPoint();
            List<IndexedPoint3d> nearest = kdtree.nearestNeighbourSearch(idpnt, minK, 0);
            // check for coplanarity
            int kk = minK;
            while (isCoplanar(nearest)) {
               kk += 1;
               nearest = kdtree.nearestNeighbourSearch(idpnt,  kk, 0);
            }
            for (IndexedPoint3d nbr : nearest) {
               double rl = nbr.pnt.distance(idpnt.pnt)*marginScale;
               if (rl > r[nbr.idx]) {
                  r[nbr.idx] = rl;
               }
            }
         }
      }
      
      return r;
   }

   private static class RestNode implements Boundable {

      MFreeNode3d node;
      Point3d[] pnts;
      
      public RestNode(MFreeNode3d node, double r) {
         this.node = node;
         pnts = new Point3d[2];
         Point3d pos = node.getPosition();
         pnts[0] = new Point3d(pos.x+r, pos.y+r, pos.z+r);
         pnts[1] = new Point3d(pos.x-r, pos.y-r, pos.z-r);
      }
      
      public MFreeNode3d getNode() {
         return node;
      }
      
      @Override
      public int numPoints() {
         return 2;
      }

      @Override
      public Point3d getPoint(int idx) {
         return pnts[idx];
      }

      @Override
      public void computeCentroid(Vector3d centroid) {
         centroid.set(node.getPosition());
      }

      @Override
      public void updateBounds(Vector3d min, Vector3d max) {
         pnts[0].updateBounds(min, max);
         pnts[1].updateBounds(min, max);
      }

      @Override
      public double computeCovariance(Matrix3d C) {
         return -1;
      }
      
   }
   
   public static MFreeModel3d createModel(MFreeModel3d model,
      MFreeNode3d[] nodes, PolygonalMesh surface,
      CubaturePoint3d[] cpnts) {

      if (model == null) {
         model = new MFreeModel3d();
      }
      model.addNodes(Arrays.asList(nodes));

      FunctionTimer timer = new FunctionTimer();

      // timer.start();
      // boolean[][] iChart = buildIntersectionChart(nodeArray);
      // timer.stop();
      // System.out.println("Intersection chart: " + timer.getTimeUsec()/1000 +
      // " ms");

      AABBTree nodeTree = new AABBTree();
      RestNode[] rnodes = new RestNode[nodes.length];
      for (int i=0; i<nodes.length; ++i) {
         rnodes[i] = new RestNode(nodes[i], nodes[i].getInfluenceRadius());
      }
      
      timer.start();
      nodeTree.build(rnodes, nodes.length);
      timer.stop();
      System.out.println("Node BVTree: " + timer.getTimeUsec() / 1000 + " ms");

      Comparator<FemNode3d> byNumber = new Comparator<FemNode3d>() {
         @Override
         public int compare (FemNode3d n0, FemNode3d n1) {
            return Integer.compare (n0.getNumber(), n1.getNumber());
         }
      };
      
      
      // compute node dependencies and coordinates
      timer.start();
      MFreeShapeFunction func = new MLSShapeFunction();
      for (MFreeNode3d node : nodes) {
         MFreeNode3d[] deps = findNodesContaining(node.getRestPosition(), nodeTree, 0);
         
         // sort nodes by number
         Arrays.sort (nodes, byNumber);
         
         VectorNd coords = new VectorNd();
         getShapeCoords(func, coords, node.getRestPosition(), deps);
         node.setDependentNodes(deps, coords);
      }
      timer.stop();
      System.out.println("Node coordinates: " + timer.getTimeUsec() / 1000
         + " ms");

      // timer.start();
      // DirectedGraph<int[], Integer> connectivityGraph =
      // IntersectionFactory.buildConnectivityGraph(iChart);
      // timer.stop();
      // System.out.println("Connectivity graph: " + timer.getTimeUsec()/1000 +
      // " ms");
      // timer.start();
      // ArrayList<MFreeElement3d> elemList = createPartitionedElements(nodes,
      // connectivityGraph);
      // timer.stop();
      // System.out.println("Partitioned elements: " + timer.getTimeUsec()/1000
      // + " ms");

      timer.start();
      MFreeIntegrationPoint3d[] ipnts = createIntegrationPoints(func, cpnts, nodeTree);
      timer.stop();
      System.out.println("Integration points: " + timer.getTimeUsec() / 1000
         + " ms");

      HashMap<MFreeIntegrationPoint3d,MFreeElement3d> pntMap = new HashMap<MFreeIntegrationPoint3d,MFreeElement3d>(ipnts.length);
      timer.start();
      ArrayList<MFreeElement3d> elemList = createPartitionedElementsFromPoints(ipnts, pntMap);
      timer.stop();
      System.out.println("Building elements from points: "
         + timer.getTimeUsec() / 1000 + " ms");

      timer.start();
      distributeIPointsFromMap(pntMap);
      timer.stop();
      System.out.println("Distributing integration Points: "
         + timer.getTimeUsec() / 1000 + " ms");

      // trimEmptyElements(elemList);
      addWarpingPoints(elemList, nodeTree);

      // surface = (PolygonalMesh)convertToMFreeMesh(surface, nodeTree, DEFAULT_TOLERANCE);

      model.addElements(elemList);
      
      if (surface != null) {
         model.setSurfaceMesh(surface);
      }
      
      model.updateNodeMasses(-1, null);

      return model;

   }

   private static ArrayList<MFreeElement3d> trimEmptyElements(
      List<MFreeElement3d> elemList) {

      ArrayList<MFreeElement3d> removed = new ArrayList<MFreeElement3d>();
      Iterator<MFreeElement3d> elemIt = elemList.iterator();

      while (elemIt.hasNext()) {
         MFreeElement3d elem = elemIt.next();
         if (elem.numIntegrationPoints() == 0 || elem.numNodes() == 0) {
            elemIt.remove();
            removed.add(elem);
         }
      }

      return removed;

   }

   private static MFreeIntegrationPoint3d[] createIntegrationPoints (
      MFreeShapeFunction fun,
      CubaturePoint3d[] cpnts, 
      BVTree nodeTree) {

      MFreeIntegrationPoint3d[] ipnts =
         new MFreeIntegrationPoint3d[cpnts.length];

      Comparator<FemNode3d> byNumber = new Comparator<FemNode3d>() {
         @Override
         public int compare (FemNode3d n0, FemNode3d n1) {
            return Integer.compare (n0.getNumber(), n1.getNumber());
         }
      };
      
      for (int i = 0; i < cpnts.length; i++) {
         MFreeNode3d[] deps = findNodesContaining(cpnts[i], nodeTree, 0);
         // sort nodes by number
         Arrays.sort (deps, byNumber);
         
         VectorNd coords = new VectorNd(deps.length);
         ArrayList<Vector3d> grad = new ArrayList<Vector3d>(deps.length);
         getShapeCoordsAndGradients(fun, coords, grad, cpnts[i], deps);
         ipnts[i] = MFreeIntegrationPoint3d.create(deps, coords, grad, cpnts[i].w);
         ipnts[i].setNumber(i);
      }

      return ipnts;
   }

   //   public static void distributePairedIPoints(MFreeElement3d[] elemList,
   //      MFreeIntegrationPoint3d[] ipnts,
   //      IntegrationData3d[] idata, BVTree elemTree, double tol) {
   //
   //      for (int i = 0; i < ipnts.length; i++) {
   //         MFreeIntegrationPoint3d ipnt = ipnts[i];
   //         IntegrationData3d idat = null;
   //         if (idata != null) {
   //            idat = idata[i];
   //         } else {
   //            idat = new IntegrationData3d();
   //            idat.setRestInverseJacobian(new Matrix3d(Matrix3d.IDENTITY), 1);
   //         }
   //
   //         ArrayList<MFreeElement3d> celems =
   //            findPairedElementsContaining(ipnt.getPosition(), elemTree, tol);
   //         for (MFreeElement3d elem : celems) {
   //            elem.addIntegrationPoint(ipnt, idat, ipnt.getWeight(), false);
   //         }
   //      }
   //
   //      for (MFreeElement3d elem : elemList) {
   //         elem.updateAllVolumes();
   //      }
   //
   //   }

   private static void distributeIPointsFromMap(
      HashMap<MFreeIntegrationPoint3d,MFreeElement3d> pntMap) {

      // Matrix3d invJ0 = new Matrix3d();
      for (MFreeIntegrationPoint3d ipnt : pntMap.keySet()) {
         IntegrationData3d idat = new IntegrationData3d();
         idat.setRestInverseJacobian(new Matrix3d(Matrix3d.IDENTITY), 1);
         MFreeElement3d elem = pntMap.get(ipnt);
         // IntegrationData3d.computeRestJacobian (invJ0, ipnt.getGNs (), elem.getNodes ());
         
         elem.addIntegrationPoint(ipnt, idat, ipnt.getWeight(), false);
      }
   }

   //   public static void distributePartitionedIPoints(
   //      DirectedGraph<MFreeElement3d,MFreeNode3d> connectivity,
   //      MFreeIntegrationPoint3d[] ipnts,
   //      IntegrationData3d[] idata, BVTree elemTree, double tol) {
   //
   //      for (int i = 0; i < ipnts.length; i++) {
   //         MFreeIntegrationPoint3d ipnt = ipnts[i];
   //         IntegrationData3d idat = null;
   //         if (idata != null) {
   //            idat = idata[i];
   //         } else {
   //            idat = new IntegrationData3d();
   //            idat.setRestInverseJacobian(new Matrix3d(Matrix3d.IDENTITY), 1);
   //         }
   //
   //         ArrayList<MFreeElement3d> celems =
   //            findPartitionedElementsContaining(
   //               ipnt.getPosition(), connectivity, elemTree, tol);
   //         for (MFreeElement3d elem : celems) {
   //            elem.addIntegrationPoint(ipnt, idat, ipnt.getWeight(), false);
   //         }
   //      }
   //
   //      for (Vertex<MFreeElement3d,MFreeNode3d> vtx : connectivity.getVertices()) {
   //         MFreeElement3d elem = vtx.getData();
   //         if (elem != null) {
   //            elem.updateAllVolumes();
   //         }
   //      }
   //
   //   }

   private static void addWarpingPoints(List<MFreeElement3d> elems,
      BVTree nodeTree) {
      for (MFreeElement3d elem : elems) {
         MFreeIntegrationPoint3d wpnt =
            createWarpingPoint(elem, null, nodeTree);
         IntegrationData3d wdat = new IntegrationData3d();
         wdat.setRestInverseJacobian(new Matrix3d(Matrix3d.IDENTITY), 1);
         elem.setWarpingPoint(wpnt, wdat);
      }
   }

   private static MFreeIntegrationPoint3d createWarpingPoint(
      MFreeElement3d elem, Point3d pos, BVTree nodeTree) {
      if (pos == null) {
         pos = new Point3d();
         elem.computeCentroid(pos);
      }

      FemNode3d[] enodes = elem.getNodes ();
      MFreeNode3d[] deps = new MFreeNode3d[enodes.length];
      for (int i=0; i<enodes.length; ++i) {
         deps[i] = (MFreeNode3d)enodes[i];
      }
      VectorNd coords = new VectorNd(deps.length);
      ArrayList<Vector3d> grad = new ArrayList<Vector3d>(deps.length);
      getShapeCoordsAndGradients(elem.getShapeFunction(), coords, grad, pos, deps);
      return MFreeIntegrationPoint3d.create(deps, coords, grad, 1);

   }

   //   public static ArrayList<MFreeElement3d> createPartitionedElements(
   //      MFreeShapeFunction fun,
   //      List<MFreeNode3d> nodes, DirectedGraph<int[],Integer> connectivityGraph) {
   //
   //      ArrayList<MFreeElement3d> elems =
   //         new ArrayList<MFreeElement3d>(connectivityGraph.numVertices());
   //
   //      for (Vertex<int[],Integer> vtx : connectivityGraph.getVertices()) {
   //
   //         int[] idxs = vtx.getData();
   //         if (idxs.length > 0) {
   //            MFreeNode3d[] enodes = new MFreeNode3d[idxs.length];
   //            for (int i = 0; i < idxs.length; i++) {
   //               enodes[i] = nodes.get(idxs[i]);
   //            }
   //            MFreeElement3d elem = new MFreeElement3d(fun, enodes);
   //            elem.setAllTermsActive(true);
   //            elems.add(elem);
   //         }
   //      }
   //
   //      return elems;
   //
   //   }

   //   public static ArrayList<MFreeElement3d> createPairedElements(
   //      MFreeShapeFunction fun, 
   //      List<MFreeNode3d> nodes, boolean[][] iChart) {
   //      // elements from node pairs
   //      ArrayList<MFreeElement3d> elemList = new ArrayList<MFreeElement3d>();
   //      for (int i = 0; i < nodes.size(); i++) {
   //         MFreeNode3d nodeA = nodes.get(i);
   //         MFreeElement3d e = new MFreeElement3d(fun, new MFreeNode3d[] { nodeA });
   //         e.setTermActive(0, 0, true);
   //         elemList.add(e);
   //         for (int j = i + 1; j < nodes.size(); j++) {
   //            if (iChart[i][j]) {
   //               e =
   //                  new MFreeElement3d(fun, new MFreeNode3d[] { nodeA, nodes.get(j) });
   //               e.setTermActive(0, 1, true);
   //               e.setTermActive(1, 0, true);
   //               e.setTermActive(0, 0, false);
   //               e.setTermActive(1, 1, false);
   //               elemList.add(e);
   //            }
   //         }
   //      }
   //
   //      return elemList;
   //   }
//
//   public static void createNodeMeshes(MFreeModel3d model,
//      Collection<MFreeNode3d> nodes, PolygonalMesh surface) {
//      
//      // set nodal influence regions
//      PolygonalMesh icoSphere = MeshFactory.createIcosahedralSphere(1, 2);
//
//      if (nodes == null) {
//         nodes = model.getNodes();
//      }
//      
//      HashMap<MFreeNode3d,PolygonalMesh> meshMap = new HashMap<MFreeNode3d,PolygonalMesh>();
//      AffineTransform3d trans = new AffineTransform3d();
//      for (MFreeNode3d node : nodes) {
//         PolygonalMesh nmesh = null;
//         if (node.isRadial()) {
//            nmesh = new PolygonalMesh(icoSphere);
//            double r = node.getInfluenceRadius();
//            trans.setIdentity();
//            trans.setTranslation(node.getRestPosition());
//            trans.applyScaling(r, r, r);
//            nmesh.transform(trans);
//            
//            if (surface != null) {
//               nmesh = MeshFactory.getIntersection(nmesh, surface);
//            }
//            meshMap.put(node, nmesh);
//         }
//      }
//      
//      // I do this after generating all meshes so that isInDomain doesn't start
//      // using the meshes before all are ready (speed issue)
//      for (MFreeNode3d node : nodes) {
//         PolygonalMesh nmesh = meshMap.get(node);
//         if (nmesh != null) {
//            node.setBoundaryMesh(nmesh);
//         }
//         // model.addMesh("node_" + node.getNumber(), nmesh);
//      }
//   }

   //   public static void createPairedElemMeshes(List<MFreeElement3d> elemList,
   //      BVTree nodeTree) {
   //
   //      // only intersections
   //      for (MFreeElement3d elem : elemList) {
   //         if (elem.numNodes() == 1) {
   //            elem.setBoundaryMesh(elem.getNode(0).getBoundaryMesh());
   //         } else {
   //            PolygonalMesh mesh =
   //               new PolygonalMesh(elem.getNode(0).getBoundaryMesh());
   //            for (int i = 1; i < elem.numNodes(); i++) {
   //               mesh =
   //                  MeshFactory.getIntersection(mesh, elem
   //                     .getNode(i).getBoundaryMesh());
   //            }
   //            mesh = (PolygonalMesh)convertToMFreeMesh(mesh, nodeTree, DEFAULT_TOLERANCE);
   //            elem.setBoundaryMesh(mesh);
   //         }
   //      }
   //
   //   }

   //   public static void createElemMeshes(
   //      MFreeModel3d model, Collection<MFreeElement3d> elemList, PolygonalMesh surface) {
   //
   //      if (elemList == null) {
   //        elemList = model.getElements();
   //      }
   //      
   //      // pre-build BSP trees for all nodes
   //      HashMap<Integer,BSPTree> meshMap = new HashMap<Integer,BSPTree>();
   //      HashMap<BSPTree,MFreeNode3d> meshMapInv = new HashMap<BSPTree,MFreeNode3d>();
   //
   //      HashSet<MFreeNode3d> nodeset = new HashSet<>();
   //      for (MFreeElement3d elem : elemList) {
   //         for (MFreeNode3d node : elem.getNodes()) {
   //            nodeset.add(node);
   //         }
   //      }
   //      ArrayList<MFreeNode3d> nodes = new ArrayList<>(nodeset);
   //      
   //      boolean[][] connectivityChart = buildIntersectionChart(nodes); 
   //      DirectedGraph<int[],Integer> connectivityGraph = IntersectionFactory.buildConnectivityGraph(connectivityChart);
   //      
   //      HashMap<int[],BSPTree> nullMap = new HashMap<int[],BSPTree>(1);
   //      for (int i = 0; i < nodes.size(); i++) {
   //         MFreeNode3d node = nodes.get(i);
   //         BSPTree tree = new BSPTree(node.getBoundaryMesh());
   //         meshMap.put(i, tree);
   //         meshMapInv.put(tree, node);
   //      }
   //      
   //      DirectedGraph<BSPTree,BSPTree> meshGraph = connectivityGraph.exchangeData(nullMap, meshMap);
   //      IntersectionFactory.buildSpatialPartition(meshGraph, null);
   //      DirectedGraph<BSPTree,MFreeNode3d> nodeGraph = meshGraph.exchangeEdgeData(meshMapInv);
   //      Vertex<BSPTree,MFreeNode3d> root = nodeGraph.getVertex(0);
   //
   //      for (MFreeElement3d elem : elemList) {
   //         Vertex<BSPTree,MFreeNode3d> vtx = root;
   //         for (MFreeNode3d node : elem.getNodes()) {
   //            for (DirectedEdge<BSPTree,MFreeNode3d> edge : vtx.getForwardEdges()) {
   //               if (edge.getData() == node) {
   //                  vtx = edge.traverseForwards();
   //                  break;
   //               }
   //            }
   //         }
   //
   //         PolygonalMesh mesh = vtx.getData().generateMesh();
   //         if (mesh.numFaces() > 0) {
   //            elem.setBoundaryMesh(mesh);
   //            // model.addMesh("elem_" + elem.getNumber(), mesh);
   //         }
   //
   //      }
   //   }

   //   public static ArrayList<MFreeElement3d> findPairdElementsContaining(
   //      Point3d pnt, BVTree bvtree, double tol) {
   //
   //      ArrayList<MFreeElement3d> deps = new ArrayList<MFreeElement3d>();
   //      ArrayList<BVNode> bvNodes = new ArrayList<BVNode>(16);
   //      bvtree.intersectPoint(bvNodes, pnt);
   //
   //      if (bvNodes.size() == 0) {
   //         return deps;
   //      }
   //
   //      for (BVNode n : bvNodes) {
   //         Boundable[] elements = n.getElements();
   //         for (int i = 0; i < elements.length; i++) {
   //            MFreeElement3d elem = (MFreeElement3d)elements[i];
   //
   //            boolean isInside = true;
   //            for (MFreeNode3d node : elem.getNodes()) {
   //               if (!node.isInDomain(pnt, tol)) {
   //                  isInside = false;
   //                  break;
   //               }
   //            }
   //            if (isInside) {
   //               deps.add(elem);
   //            }
   //         }
   //      }
   //      return deps;
   //   }

   //   public static ArrayList<MFreeElement3d> findPartitionedElementsContaining(
   //      Point3d pnt, DirectedGraph<MFreeElement3d,MFreeNode3d> connectivity,
   //      BVTree bvtree, double tol) {
   //
   //      ArrayList<MFreeElement3d> deps = new ArrayList<MFreeElement3d>();
   //      ArrayList<BVNode> bvNodes = new ArrayList<BVNode>(16);
   //      bvtree.intersectPoint(bvNodes, pnt);
   //
   //      if (bvNodes.size() == 0) {
   //         return deps;
   //      }
   //
   //      for (BVNode n : bvNodes) {
   //         Boundable[] elements = n.getElements();
   //         for (int i = 0; i < elements.length; i++) {
   //            MFreeElement3d elem = (MFreeElement3d)elements[i];
   //
   //            boolean isInside = true;
   //            for (MFreeNode3d node : elem.getNodes()) {
   //               if (!node.isInDomain(pnt, tol)) {
   //                  isInside = false;
   //                  break;
   //               }
   //            }
   //
   //            // exclude deeper intersections
   //            if (isInside) {
   //               Vertex<MFreeElement3d,MFreeNode3d> vtx =
   //                  connectivity.findVertex(elem);
   //               for (DirectedEdge<MFreeElement3d,MFreeNode3d> edge : vtx
   //                  .getForwardEdges()) {
   //                  MFreeNode3d node = edge.getData();
   //                  if (node.isInDomain(pnt, tol)) {
   //                     isInside = false;
   //                     break;
   //                  }
   //               }
   //            }
   //            if (isInside) {
   //               deps.add(elem);
   //            }
   //         }
   //      }
   //      return deps;
   //   }

//   public static DirectedGraph<MFreeElement3d,MFreeNode3d> convertConnectivity(
//      List<MFreeNode3d> nodes, List<MFreeElement3d> elems,
//      DirectedGraph<int[],Integer> graph) {
//
//      DirectedGraph<MFreeElement3d,MFreeNode3d> out = graph.cloneStructure();
//
//      // copy over nodes
//      for (int i = 0; i < graph.numDirectedEdges(); i++) {
//         DirectedEdge<int[],Integer> idxEdge = graph.getDirectedEdge(i);
//         DirectedEdge<MFreeElement3d,MFreeNode3d> nodeEdge =
//            out.getDirectedEdge(i);
//         nodeEdge.setData(nodes.get(idxEdge.getData()));
//      }
//
//      // fill in vertices by traversing along edges
//      Vertex<MFreeElement3d,MFreeNode3d> base = out.getVertex(0);
//      for (MFreeElement3d elem : elems) {
//         MFreeNode3d[] nodeArray = elem.getNodes();
//         Vertex<MFreeElement3d,MFreeNode3d> elemVtx =
//            out.traverseEdgesForward(base, nodeArray);
//         elemVtx.setData(elem);
//      }
//
//      return out;
//
//   }

   //   public static ArrayList<MFreeElement3d> findPairedElementsContaining(
   //      Point3d pnt, BVTree bvtree, double tol) {
   //
   //      ArrayList<MFreeElement3d> deps = new ArrayList<MFreeElement3d>();
   //      ArrayList<BVNode> bvNodes = new ArrayList<BVNode>(16);
   //      bvtree.intersectPoint(bvNodes, pnt);
   //
   //      if (bvNodes.size() == 0) {
   //         return deps;
   //      }
   //
   //      for (BVNode n : bvNodes) {
   //         Boundable[] elements = n.getElements();
   //         for (int i = 0; i < elements.length; i++) {
   //            MFreeElement3d elem = (MFreeElement3d)elements[i];
   //
   //            boolean isInside = true;
   //            for (MFreeNode3d node : elem.getNodes()) {
   //               if (!node.isInDomain(pnt, tol)) {
   //                  isInside = false;
   //                  break;
   //               }
   //            }
   //            if (isInside) {
   //               deps.add(elem);
   //            }
   //         }
   //      }
   //      return deps;
   //   }

   private static MFreeNode3d[] findNodesContaining(Point3d pnt,
      BVTree bvtree, double tol) {

      DynamicArray<MFreeNode3d> deps = new DynamicArray<>(MFreeNode3d.class);
      ArrayList<BVNode> bvNodes = new ArrayList<BVNode>(16);
      bvtree.intersectPoint(bvNodes, pnt);

      if (bvNodes.size() == 0) {
         return deps.getArray();
      }

      for (BVNode n : bvNodes) {
         Boundable[] elements = n.getElements();
         for (int i = 0; i < elements.length; i++) {
            RestNode rnode = (RestNode)elements[i];
            MFreeNode3d node = rnode.getNode();
            if (node.isInDomain(pnt, tol)) {
               deps.add(node);
            }
         }
      }
      
      return deps.getArray();
   }

   private static MFreeNode3d createNode(double x, double y, double z,
      double rad, RadialWeightFunctionType wType) {
      MFreeNode3d node = new MFreeNode3d(x, y, z);
      RadialWeightFunction ffun = RadialWeightFunction.createWeightFunction(
            wType, node.getRestPosition(), rad);
      node.setWeightFunction(ffun);
      return node;
   }

   //   private static void updatePointCoordinates(
   //      MFreeShapeFunction fun, 
   //      List<? extends MFreePoint3d> pnts,
   //      BVTree nodeTree, double tol) {
   //
   //      VectorNd coords = new VectorNd();
   //      for (MFreePoint3d pnt : pnts) {
   //         MFreeNode3d[] deps =
   //            findNodesContaining(pnt.getRestPosition(), nodeTree, tol);
   //         getShapeCoords(fun, coords, pnt.getRestPosition(), deps);
   //         pnt.setDependentNodes(deps, coords);
   //      }
   //   }

   //   public static MeshBase convertToMFreeMesh(MeshBase orig,
   //      BVTree nodeTree, double tol) {
   //
   //      VectorNd coords = new VectorNd();
   //      MFreeShapeFunction func = new MLSShapeFunction();
   //
   //      HashMap<Vertex3d,MFreeVertex3d> vtxMap =
   //         new HashMap<Vertex3d,MFreeVertex3d>();
   //
   //      ArrayList<MFreeVertex3d> vtxs = 
   //         new ArrayList<MFreeVertex3d>(orig.numVertices());
   //      for (Vertex3d vtx : orig.getVertices()) {
   //         MFreeNode3d[] deps =
   //            findNodesContaining(vtx.getPosition(), nodeTree, tol);
   //         getShapeCoords(func, coords, vtx.getPosition(), deps);
   //         MFreeVertex3d nvtx = new MFreeVertex3d(deps, coords);
   //         vtxMap.put(vtx, nvtx);
   //         vtxs.add(nvtx);
   //      }
   //      MeshBase out = orig.copy();
   //      out.replaceVertices (vtxs);
   //      out.setFixed(false);
   //      out.setColorsFixed(false);
   //      
   //      // copy other properties
   //      out.setName(orig.getName());
   //      out.setRenderProps(orig.getRenderProps());
   //
   //      return out;
   //
   //   }

   private static void getShapeCoords(MFreeShapeFunction fun, VectorNd coords, Point3d pnt,
      MFreeNode3d[] deps) {

      coords.setSize(deps.length);
      fun.setNodes (deps);
      fun.setCoordinate (pnt);
      fun.eval (coords);

   }

   private static int getShapeCoordsAndGradients(
      MFreeShapeFunction fun,
      VectorNd coords,
      ArrayList<Vector3d> grad,
      Point3d pnt, MFreeNode3d[] deps) {

      int nDeps = deps.length;
      grad.clear();
      grad.ensureCapacity(nDeps);
      coords.setSize(deps.length);

      fun.setNodes (deps);
      fun.setCoordinate (pnt);

      fun.eval (coords);
      for (int i = 0; i < nDeps; i++) {
         Vector3d nodegrad = new Vector3d();
         fun.evalDerivative(i, nodegrad);
         grad.add(nodegrad);
      }

      return nDeps;
   }

   public static MFreeModel3d cloneFem(MFreeModel3d model, FemModel3d fem) {

      if (model == null) {
         model = new MFreeModel3d();
      }

      HashMap<FemNode3d,MFreeNode3d> nodeMap = new HashMap<FemNode3d,MFreeNode3d>();
      HashMap<MFreeNode3d,FemNode3d> nodeMapInv = new HashMap<MFreeNode3d,FemNode3d>();
      ArrayList<MFreeNode3d> nodeList = new ArrayList<MFreeNode3d>();

      // duplicate nodes
      for (FemNode3d node : fem.getNodes()) {
         MFreeNode3d mnode = new MFreeNode3d(node.getRestPosition());
         // explicit node masses
         mnode.setExplicitMass(node.getMass());
         MFreeNode3d[] deps = new MFreeNode3d[1];
         deps[0] = mnode;
         VectorNd coords = new VectorNd(new double[] { 1 });
         mnode.setDependentNodes(deps, coords);
         nodeMap.put(node, mnode);
         nodeMapInv.put(mnode, node);
         nodeList.add(mnode);
      }

      // convert surface mesh
      FemMeshComp surfaceFem = fem.getSurfaceMeshComp ();
      PolygonalMesh mesh = (PolygonalMesh)surfaceFem.getMesh ();
      
      // build mesh
      PolygonalMesh mesh2 = mesh.copy();
      // index vertices
      int idx = 0;
      for (Vertex3d vtx : mesh.getVertices()) {
         vtx.setIndex(idx++);
      }
      idx = 0;
      for (Vertex3d vtx : mesh2.getVertices()) {
         vtx.setIndex(idx++);
      }
      
      MFreeMeshComp surfaceMFree = new MFreeMeshComp(model, "surface");
      surfaceMFree.setMesh(mesh2);
      
      // manually build surface attachments
      for (Vertex3d vtx : mesh.getVertices()) {
         
         PointAttachment pa = surfaceFem.getVertexAttachment(vtx.getIndex());
         
         if (pa instanceof PointFem3dAttachment) {
            PointFem3dAttachment pfa = (PointFem3dAttachment)pa;
            FemNode[] masters = pfa.getNodes();
            
            MFreeNode3d[] deps = new MFreeNode3d[masters.length];
            VectorNd coords = new VectorNd(masters.length);
            
            for (int j=0; j<masters.length; j++) {
               //mlist.add (new CompContactMaster (masters[j], pfa.getCoordinate(j)));
               deps[j] = nodeMap.get (masters[j]);
               coords.set (j, pfa.getCoordinate (j));
            }
            
            surfaceMFree.setVertexAttachment(vtx.getIndex(), coords.getBuffer(), deps);
            
         }
         else {
            PointParticleAttachment ppa = (PointParticleAttachment)pa;
            DynamicComponent[] masters = ppa.getMasters ();

            MFreeNode3d[] deps = new MFreeNode3d[1];
            deps[0] = nodeMap.get (masters[0]);
            VectorNd coords = new VectorNd(new double[] { 1 });
            
            surfaceMFree.setVertexAttachment(vtx.getIndex(),  coords.getBuffer(), deps);
            
         }
      }
      
      // integration regions by copying elements
      ArrayList<MFreeElement3d> elemList = new ArrayList<MFreeElement3d>(fem.numElements());
      HashMap<FemElement3d,MFreeElement3d> elemMap = new HashMap<FemElement3d,MFreeElement3d>(fem.numElements());
      
      for (FemElement3d elem : fem.getElements()) {
         
         MFreeNode3d[] elemNodes = new MFreeNode3d[elem.numNodes()];
         FemNode3d[] fnodes = elem.getNodes();
         for (int i = 0; i < elem.numNodes(); i++) {
            elemNodes[i] = nodeMap.get(fnodes[i]);
         }

         MFreeElement3d region = new MFreeElement3d(null, elemNodes);
         // region.setAllTermsActive(true);

         MFreeIntegrationPoint3d[] mpnts = new MFreeIntegrationPoint3d[elem.numIntegrationPoints()];
         IntegrationData3d[] mdata = new IntegrationData3d[elem.numIntegrationPoints()];

         IntegrationPoint3d[] ipnts = elem.getIntegrationPoints();
         IntegrationData3d[] idata = elem.getIntegrationData();

         for (int i = 0; i < ipnts.length; i++) {
            Point3d pos = new Point3d();
            ipnts[i].computePosition(pos, elem.getNodes());
            Vector3d[] gradU = ipnts[i].getGNs();
            ArrayList<Vector3d> grads = new ArrayList<Vector3d>();
            for (Vector3d g : gradU) {
               grads.add(g);
            }

            MFreeIntegrationPoint3d mpnt = MFreeIntegrationPoint3d.create(elemNodes,
               ipnts[i].getShapeWeights(), grads, ipnts[i].getWeight());
            IntegrationData3d mdat = new IntegrationData3d();
            mdat.setRestInverseJacobian(idata[i].getInvJ0(), idata[i].getDetJ0());

            mpnts[i] = mpnt;
            mdata[i] = mdat;
         }

         // set warping point
         if (region.getWarpingPoint() == null) {
            IntegrationPoint3d wpnt = elem.getWarpingPoint();
            IntegrationData3d wdat = elem.getWarpingData();
            Point3d pos = new Point3d();
            wpnt.computePosition(pos, elem.getNodes());
            // Vector3d [] gradU = wpnt.updateShapeGradient(wdat.getInvJ0());
            Vector3d[] gradU = wpnt.getGNs();
            ArrayList<Vector3d> grads = new ArrayList<Vector3d>();
            for (Vector3d g : gradU) {
               grads.add(g);
            }
            // MFreeIntegrationPoint3d mpnt =
            // MFreeIntegrationPoint3d.create(pos, deps,
            // wpnt.getShapeWeights(), grads, wpnt.getWeight()*wdat.getDetJ0());
            MFreeIntegrationPoint3d mpnt = MFreeIntegrationPoint3d.create(elemNodes,
               wpnt.getShapeWeights(), grads, wpnt.getWeight());
            region.setWarpingPoint(mpnt);
            IntegrationData3d wdata = new IntegrationData3d();
            wdata.setRestInverseJacobian(wdat.getInvJ0(), wdat.getDetJ0());
            region.setWarpingPoint(mpnt, wdata);
         }
         region.setIntegrationPoints(mpnts, mdata);
         
         elemList.add(region);
         elemMap.put(elem, region);

      }

      // add everything to model
      model.addNodes(nodeList);
      model.addElements(elemList);
      model.setSurfaceMeshComp(surfaceMFree);
      
      // copy properties
      model.setDensity(fem.getDensity());
      model.setParticleDamping(fem.getParticleDamping());
      model.setStiffnessDamping(fem.getStiffnessDamping());

      // copy over all masses
      for (FemNode3d node : fem.getNodes()) {
         nodeMap.get(node).setMass(node.getMass());
      }

      for (FemElement3d elem : fem.getElements()) {
         elemMap.get(elem).setMass(elem.getMass());
      }
      
      model.setMaterial(fem.getMaterial());

      return model;
   }

   private static boolean[][] buildIntersectionChart(List<MFreeNode3d> nodeList) {

      int n = nodeList.size();
      boolean[][] out = new boolean[n][n];

      for (int i = 0; i < n; i++) {
         out[i][i] = true;
         MFreeNode3d node1 = nodeList.get(i);
         for (int j = i + 1; j < n; j++) {
            boolean intersects = node1.intersects(nodeList.get(j));
            out[i][j] = intersects;
            out[j][i] = intersects;
         }
      }
      return out;

   }

   private static boolean[][] buildIntersectionChart(MFreeNode3d[] nodeArray) {
      int n = nodeArray.length;
      boolean out[][] = new boolean[n][n];

      for (int i = 0; i < n; i++) {
         out[i][i] = true;
         MFreeNode3d node1 = nodeArray[i];
         for (int j = i + 1; j < n; j++) {
            boolean intersects = node1.intersects(nodeArray[j]);
            out[i][j] = intersects;
            out[j][i] = intersects;
         }
      }
      return out;

   }

   private static <A extends MFreePoint3d> ArrayList<MFreeElement3d> 
      createPartitionedElementsFromPoints(A[] pnts, HashMap<A,MFreeElement3d> pntMap) {
      
      ArrayList<MFreeElement3d> elems = new ArrayList<MFreeElement3d>();
      FemElementTreeNode etree = new FemElementTreeNode();
      
      MFreeShapeFunction fun = new MLSShapeFunction();
      
      for (A pnt : pnts) {
         FemNode3d[] nodes = pnt.getDependentNodes();      
         
         FemElementTreeNode leaf = etree.findOrCreate (nodes);
         
         MFreeElement3d elem = (MFreeElement3d)leaf.getElement ();
         if (elem == null) {
            elem = new MFreeElement3d(fun, Arrays.copyOf(nodes, nodes.length));
            elems.add(elem);
            leaf.setElement (elem);
         }
         pntMap.put(pnt, elem);
      }

      return elems;

   }
   
   /**
    * Class for storing node make-up of elements
    */
   public static class FemElementTreeNode {
      FemElement elem;
      HashMap<FemNode3d,FemElementTreeNode> children;
      
      public FemElementTreeNode() {
         elem = null;
         children = new HashMap<> ();
      }
      
      public FemElement getElement() {
         return elem;
      }
      
      public void setElement(FemElement elem) {
         this.elem = elem;
      }
      
      /**
       * Appends a set of nodes to the tree
       * @param nodes FEM nodes making up element, must be sorted by number
       * @param offset offset into the nodes list
       * @return created leaf node
       */
      protected FemElementTreeNode append(FemNode3d[] nodes, int offset) {
         if (offset == nodes.length) {
            return this;
         }
         FemElementTreeNode child = new FemElementTreeNode();
         FemElementTreeNode leaf = child.append (nodes, offset+1);
         children.put (nodes[offset], child);
         return leaf;
      }
      
      /**
       * Finds an element tree node for the following FEM nodes
       * @param nodes nodes making up element, sorted by number
       * @return tree node that contains element, null if not found
       */
      public FemElementTreeNode find(FemNode3d[] nodes) {
         return find(nodes, 0);
      }
      
      /**
       * Finds an element tree node for the following FEM nodes
       * @param nodes nodes making up element, sorted by number
       * @param offset offset within nodes array
       * @return tree node that contains element
       */
      protected FemElementTreeNode find(FemNode3d[] nodes, int offset) {
         if (offset == nodes.length) {
            return this;
         }
         
         FemElementTreeNode child = children.get (nodes[offset]);
         if (child == null) {
            return null;            // not found
         }
         return child.find (nodes, offset+1);
      }
      
      /**
       * Finds an element tree node for the following FEM nodes
       * @param nodes nodes making up element, sorted by number
       * @return tree node that should contain element if it exists
       */
      public FemElementTreeNode findOrCreate(FemNode3d[] nodes) {
         return findOrCreate(nodes, 0);
      }
      
      /**
       * Finds an element tree node for the following FEM nodes
       * @param nodes nodes making up element, sorted by number
       * @param offset offset within nodes array
       * @return tree node that should contain element if it exists
       */
      protected FemElementTreeNode findOrCreate(FemNode3d[] nodes, int offset) {
         if (offset == nodes.length) {
            return this;
         }
         
         FemElementTreeNode child = children.get (nodes[offset]);
         if (child == null) {
            child = new FemElementTreeNode();
            FemElementTreeNode leaf = child.append(nodes, offset+1);
            children.put (nodes[offset], child);
            return leaf;
         }
         return child.findOrCreate (nodes, offset+1);
      }
   }
   
}
