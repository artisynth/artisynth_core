/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mfreemodels;

import java.util.ArrayList;

import maspack.geometry.BVFeatureQuery;
import maspack.geometry.BVTree;
import maspack.geometry.OBB;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.Matrix3d;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.SymmetricMatrix3d;
import maspack.matrix.Vector2d;
import maspack.matrix.Vector3d;
import maspack.util.SortUtilitities;

public class PointDistributor {

   public static int DEFAULT_MAX_ITERS = 20;
   public static double DEFAULT_THRESHOLD = 1e-10;
   
   // create a grid of points
   public static Point3d[] getGrid(double[] size, int[] res, Point3d centre) {
      
      int nPnts = res[0]*res[1]*res[2];
      Point3d[] pnts = new Point3d[nPnts];
      
      double dx = size[0]/(res[0]-1);
      double dy = size[1]/(res[1]-1);
      double dz = size[2]/(res[2]-1);
      
      double xoff = -size[0]/2 + centre.x;
      double yoff = -size[1]/2 + centre.y;
      double zoff = -size[2]/2 + centre.z;;
      
      int idx = 0;
      double x,y,z;
      for (int i=0; i<res[0]; i++) {
         x = xoff + i*dx;
         for (int j=0; j<res[1]; j++) {
            y = yoff + j*dy;
            for (int k=0; k<res[2]; k++) {
               z = zoff + k*dz;
               pnts[idx++] = new Point3d(x,y,z);
            }
         }
      }
      return pnts;
      
   }
   
   // face-centered cubic sphere packing
   private static int getFCCsize(int nx, int ny, int nz) {
      return 4*nx*ny*nz-2*(nx*ny+nx*nz+ny*nz)+nz+ny+nz;
   }
   
   // face-centered cubic sphere packing
   public static Point3d[] getFCC(double[] size, int[] res, Point3d centre) {
      
      int nPnts = getFCCsize(res[0],res[1],res[2]);
      Point3d[] pnts = new Point3d[nPnts];
      
      double dx = size[0]/(res[0]-1);
      double dy = size[1]/(res[1]-1);
      double dz = size[2]/(res[2]-1);
      
      double xoff = -size[0]/2 + centre.x;
      double yoff = -size[1]/2 + centre.y;
      double zoff = -size[2]/2 + centre.z;;
      
      int idx = 0;
      double x,y,z;
      for (int i=0; i<res[0]; i++) {
         x = xoff + i*dx;
         for (int j=0; j<res[1]; j++) {
            y = yoff + j*dy;
            for (int k=0; k<res[2]; k++) {
               z = zoff + k*dz;
               pnts[idx++] = new Point3d(x,y,z);
               
               if (i<(res[0]-1) && j<(res[1]-1)) {
                  pnts[idx++] = new Point3d(x+dx/2,y+dy/2,z);
               }
               if (i<(res[0]-1) && k<(res[2]-1)) {
                  pnts[idx++] = new Point3d(x+dx/2,y,z+dz/2);
               }
               if (k<(res[2]-1) && j<(res[1]-1)) {
                  pnts[idx++] = new Point3d(x,y+dy/2,z+dx/2);
               }
            }
         }
      }
      
      return pnts;
      
   }
   
   // fills a mesh with spheres based on face-centered cubic packing
   public static Point3d[] sphereFCCFill(PolygonalMesh mesh, double r) {
      
      ArrayList<Point3d> pnts = new ArrayList<Point3d>();
      RigidTransform3d trans = getPrincipalAxes(mesh);
      Point3d[] box = getTightBox(mesh,trans);
      
      Point3d center = new Point3d(box[0]);
      center.add(box[6]);
      center.scale(0.5);
      trans.setTranslation(center);
      
      Vector3d l = new Vector3d(box[0]);
      l.sub(box[6]);
      l.inverseTransform(trans);
      
      double alpha = 2*Math.sqrt(2)*r;
      int nx = (int)Math.ceil(l.x/alpha)+1;
      int ny = (int)Math.ceil(l.y/alpha)+1;
      int nz = (int)Math.ceil(l.z/alpha)+1;
      
      double xoffset = -(nx-1)*alpha/2;
      double yoffset = -(ny-1)*alpha/2;
      double zoffset = -(nz-1)*alpha/2;
      
      
      BVTree bvh = mesh.getBVTree();
      Vector2d coords = new Vector2d();
      Point3d nearest = new Point3d();
      BVFeatureQuery query = new BVFeatureQuery();
      
      Point3d p;
      for (int i=0; i<nx; i++) {
         for (int j=0; j<ny; j++) {
            for (int k=0; k<nz; k++) {
               double x = i*alpha+xoffset;
               double y = j*alpha+yoffset;
               double z = k*alpha+zoffset;
               p = new Point3d(x,y,z);
               p.transform(trans);
               addIfIntersects(pnts, p, r, bvh, nearest, coords, query);
               
               // face centers
               if (i < nx-1 && k<nz-1) {
                  p = new Point3d(x+alpha/2,y,z+alpha/2);
                  p.transform(trans);
                  addIfIntersects(pnts, p, r, bvh, nearest, coords, query);
               }
               
               if (j<ny-1 && k<nz-1) {
                  p = new Point3d(x,y+alpha/2,z+alpha/2);
                  p.transform(trans);
                  addIfIntersects(pnts, p, r, bvh, nearest, coords, query);
               }
               
               if (i<nx-1 && j<ny-1) {
                  p = new Point3d(x+alpha/2,y+alpha/2,z);
                  p.transform(trans);
                  addIfIntersects(pnts, p, r, bvh, nearest, coords, query);
               }
            }
         }
      }
      
      return pnts.toArray(new Point3d[pnts.size()]);
   }
   
   // fills a mesh based on a regular grid of points
   public static Point3d[] sphereGridFill(PolygonalMesh mesh, double r) {
      
      ArrayList<Point3d> pnts = new ArrayList<Point3d>();
      RigidTransform3d trans = getPrincipalAxes(mesh);
      Point3d[] box = getTightBox(mesh,trans);
      
      Point3d center = new Point3d(box[0]);
      center.add(box[6]);
      center.scale(0.5);
      trans.setTranslation(center);
      
      Vector3d l = new Vector3d(box[0]);
      l.sub(box[6]);
      l.inverseTransform(trans);
      
      double alpha = 2*Math.sqrt(2)*r;
      int nx = (int)Math.ceil(l.x/alpha)+1;
      int ny = (int)Math.ceil(l.y/alpha)+1;
      int nz = (int)Math.ceil(l.z/alpha)+1;
      
      double xoffset = -(nx-1)*alpha/2;
      double yoffset = -(ny-1)*alpha/2;
      double zoffset = -(nz-1)*alpha/2;
      
      BVTree bvh = mesh.getBVTree();
      Vector2d coords = new Vector2d();
      Point3d nearest = new Point3d();
      BVFeatureQuery query = new BVFeatureQuery();
      
      Point3d p;
      for (int i=0; i<nx; i++) {
         for (int j=0; j<ny; j++) {
            for (int k=0; k<nz; k++) {
               double x = i*alpha+xoffset;
               double y = j*alpha+yoffset;
               double z = k*alpha+zoffset;
               p = new Point3d(x,y,z);
               p.transform(trans);
               addIfIntersects(pnts, p, r, bvh, nearest, coords, query);
            }
         }
      }
      
      return pnts.toArray(new Point3d[pnts.size()]);
   }
   
   private static boolean addIfIntersects(
      ArrayList<Point3d> list, Point3d p, double r, BVTree bvh, 
      Point3d nearest, Vector2d coords, BVFeatureQuery query) {
      
      if (query.isInsideOrientedMesh (bvh, p, 1e-10)) {
         list.add(p);
         return true;
      }
      query.nearestFaceToPoint (nearest, coords, bvh, p);
      if (nearest.distance(p) <= r) {
         r = r-nearest.distance(p);
         p.set(nearest);
         list.add(p);
         return true;
      }
      return false;
   }
   
   public static RigidTransform3d getPrincipalAxes(PolygonalMesh mesh) {

      Vector3d mov1 = new Vector3d();
      Vector3d mov2 = new Vector3d();
      Vector3d pov = new Vector3d();

      double vol = mesh.computeVolumeIntegrals(mov1, mov2, pov);
      double mass = vol;

      Point3d cov = new Point3d();
      cov.scale(1.0 / vol, mov1); // center of volume

      // [c], skew symmetric
      Matrix3d covMatrix = new Matrix3d(
         0, -cov.z, cov.y,
         cov.z, 0, -cov.x,
         -cov.y, cov.x, 0);
      // J
      Matrix3d J = new Matrix3d(
         (mov2.y + mov2.z), -pov.z, -pov.y,
         -pov.z, (mov2.x + mov2.z), -pov.x,
         -pov.y, -pov.x, (mov2.x + mov2.y));
      
      // Jc = J + m[c][c]
      Matrix3d Jc = new Matrix3d();
      Jc.mul(covMatrix, covMatrix);
      Jc.scale(mass);
      Jc.add(J);

      // Compute eigenvectors and eigenvlaues of Jc
      SymmetricMatrix3d JcSymmetric = new SymmetricMatrix3d(Jc);
      Vector3d lambda = new Vector3d();
      Matrix3d U = new Matrix3d();
      JcSymmetric.getEigenValues(lambda, U);

      // Construct the rotation matrix
      RotationMatrix3d R = new RotationMatrix3d();
      R.set(U);

      lambda.absolute();

      if (lambda.x > lambda.y && lambda.z > lambda.y) {
         R.rotateZDirection(new Vector3d(R.m01, R.m11, R.m21));
      } else if (lambda.x > lambda.z && lambda.y > lambda.z) {
         R.rotateZDirection(new Vector3d(R.m00, R.m10, R.m20));
      }

      return (new RigidTransform3d(cov, R));
   }
   
   public static Point3d[] getTightBox(PolygonalMesh mesh, RigidTransform3d principal) {
      Point3d[] pnts = new Point3d[mesh.numVertices()];
      
      for (int i=0; i<mesh.numVertices(); i++) {
         pnts[i] = mesh.getVertex(i).getPosition();
      }
      if (principal == null) {
         principal = getPrincipalAxes(mesh);
      }
      return getTightBox(pnts, principal);
   }
   
   public static OBB getTightOBB (
      PolygonalMesh mesh, RigidTransform3d principal) {
      Point3d[] pnts = new Point3d[mesh.numVertices()];
      
      for (int i=0; i<mesh.numVertices(); i++) {
         pnts[i] = mesh.getVertex(i).getPosition();
      }
      if (principal == null) {
         principal = getPrincipalAxes(mesh);
      }
      return getTightOBB(pnts, principal);
   }
   
   
   
   public static Point3d[] getTightBox(Point3d[] pnts, RigidTransform3d principal) {
      
      Vector3d [] axis = new Vector3d[3];
      double [][] projBounds = new double[3][2];
      
      for (int i=0; i<3; i++) {
         axis[i] = new Vector3d();
         principal.R.getColumn(i, axis[i]);   
      }
      Vector3d p = principal.p;

      // loop through all nodes, projecting onto axes
      for (int i=0; i<pnts.length; i++) {
         
         for (int j=0; j<3; j++) {
            
            Vector3d vec = new Vector3d(); 
            vec.sub(pnts[i], p);
            
            double d =vec.dot(axis[j]);
            if (d < projBounds[j][0]) {
               projBounds[j][0] = d;
            } else if (d > projBounds[j][1]) {
               projBounds[j][1] = d;
            }
         } // end looping through axes
      } // end looping over vertices
      
      // construct bounds
      
      Point3d [] bounds = new Point3d[8];
      int [][] coords = {{1,1,1}, {1,1,0}, {1,0,0}, {1,0,1}, 
                           {0,1,1}, {0,1,0}, {0,0,0}, {0,0,1}};
      
      // create 8 corners
      for (int i=0; i<8; i++) {
         bounds[i] = new Point3d(p);
         for (int j=0; j<3; j++) {
            bounds[i].scaledAdd(projBounds[j][coords[i][j]], axis[j]);
         }
      }
      
      return bounds;
      
   }
   
   public static OBB getTightOBB(
      Point3d[] pnts, RigidTransform3d principal) {
      
      Vector3d [] axis = new Vector3d[3];
      double [][] projBounds = new double[3][2];
      
      for (int i=0; i<3; i++) {
         axis[i] = new Vector3d();
         principal.R.getColumn(i, axis[i]);   
      }
      Vector3d p = principal.p;

      // loop through all nodes, projecting onto axes
      for (int i=0; i<pnts.length; i++) {
         
         for (int j=0; j<3; j++) {
            
            Vector3d vec = new Vector3d(); 
            vec.sub(pnts[i], p);
            
            double d =vec.dot(axis[j]);
            if (d < projBounds[j][0]) {
               projBounds[j][0] = d;
            } else if (d > projBounds[j][1]) {
               projBounds[j][1] = d;
            }
         } // end looping through axes
      } // end looping over vertices
      
      // construct bounds
      Point3d c = new Point3d(0,0,0);
      Vector3d widths = new Vector3d();
      for (int i=0; i<3; i++) {
         c.set(i, 0);
         widths.set(i, projBounds[i][1] - projBounds[i][0]);
         c.set(i, p.get(i) + (projBounds[i][0]+projBounds[i][1])/2);
      }
      RigidTransform3d trans = new RigidTransform3d(c, principal.R);
      OBB obb = new OBB(widths, trans);
      return obb;
      
   }
   
   // Lloyd sampling (relaxation)
   public static void lloydSample(Point3d[] out, Point3d[] in) {
      lloydSample(out, in, DEFAULT_THRESHOLD, DEFAULT_MAX_ITERS);
   }
   
   public static void lloydSample(Point3d[] out, Point3d[] in, double eps, int maxIters) {
      Point3d[] subs = new Point3d[out.length];
      farthestPointSampling(subs, in);
      
      for (int i=0; i<out.length; i++) {
         if (out[i] == null) {
            out[i] = (Point3d)subs[i].clone();
         } else {
            out[i].set(subs[i]);
         }
      }
      lloydRelaxation(out, in, eps, maxIters);
   }
   
   public static void farthestPointSampling(Point3d[] out, Point3d[] in, int offset) {
      
      int idxMax = 0;
      double dMin = 0;
      double dMax = -1;
      
      int [] idxs = new int[in.length];
      for (int i=0; i<in.length; i++) {
         idxs[i] = i;
      }
      
      int nIn = in.length;
            
      for (int i=offset; i<out.length; i++) {
         
         dMax = -1;
         idxMax = 0;
         
         for (int j=0; j<nIn; j++) {
            dMin = Double.MAX_VALUE;
            for (int k=0; k<i; k++) {
               double dist = in[idxs[j]].distance(out[k]);
               if (dist < dMin) {
                  dMin = dist;
               }
            }
            if (dMin > dMax) {
               dMax = dMin; 
               idxMax  = j;
            }
         }
         
         out[i] = in[idxs[idxMax]];
         nIn--;
         in[idxs[idxMax]] = in[idxs[nIn]];
      }
      
   }
   
   public static void farthestPointSampling(Point3d[] out, Point3d[] in) {
      out[0] = in[0];
      farthestPointSampling(out, in, 1);
   }
   
   public static void lloydRelaxation(Point3d[] controls, Point3d[] points, double threshold, int iterMax) {
      
      Integer[] cells = new Integer[points.length];
      
      int iters = 0;
      double err = 0;
      Point3d center = new Point3d();
      int cell,nCellPoints;
      
      do {
         discreteVoronoi(cells,controls,points);
         iters++;
         
         int[] idxs = SortUtilitities.sortIndices(cells);
      
         cell = -1;
         nCellPoints = 0;
         
         for (int i=0; i<points.length; i++) {
            if (cell == cells[idxs[i]]) {
               nCellPoints++;
               center.add(points[idxs[i]]);
            } else {
               if (cell > -1) {
                  center.scale(1.0/nCellPoints);
                  err+=center.distance(controls[cell]);
                  controls[cell].set(center);
               }
               cell = cells[idxs[i]];
               nCellPoints=1;
               center.set(points[idxs[i]]);
            }
         }
         
         // last one
         center.scale(1.0/nCellPoints);
         err += center.distance(controls[cell]);
         controls[cell].set(center);
         
         iters++;
      } while (iters < iterMax && err > threshold);
      
      
      
   }
   
   public static Integer[] discreteVoronoi(Integer[] cells, Point3d[] controls, Point3d[] points) {
      
      if (cells == null) {
         cells = new Integer[points.length];
      }
      
      for (int i=0; i<points.length; i++) {
         double minD = Double.MAX_VALUE;
         double d;
         for (int j=0; j<controls.length; j++) {
            d = controls[j].distance(points[i]);
            if (d < minD) {
               minD = d;
               cells[i] = j;
            }
         }
      }
      return cells;
   }
   
   public static CubaturePoint3d[] getSphericalMidpointCubature(Point3d center, double radius, int nR, int nPhi, int nTheta, Vector3d axis) {
      
      CubaturePoint3d[] pnts = new CubaturePoint3d[nR*nPhi*nTheta];
      
      double dr = radius/nR;
      double dphi = 2*Math.PI/nPhi;
      double dtheta = Math.PI/nTheta;
      
      RigidTransform3d trans = new RigidTransform3d();      
      if (axis != null) {
         RotationMatrix3d R = new RotationMatrix3d();
         R.rotateZDirection(axis);
         trans.setRotation(R);
      }
      trans.setTranslation(center);
      
      double r,phi,theta;
      int idx = 0;
      for (int i=0; i<nR; i++) {
         for (int j=0; j<nPhi; j++) {
            for (int k=0; k<nTheta; k++) {
               r = i*dr+dr/2;
               phi = j*dphi+(i+k)*dphi/2;
               theta = k*dtheta+dtheta/2;
               
               pnts[idx] = new CubaturePoint3d();
               pnts[idx].x = r*Math.cos(phi)*Math.sin(theta);
               pnts[idx].y = r*Math.sin(phi)*Math.sin(theta);
               pnts[idx].z = r*Math.cos(theta);
               
               pnts[idx].w = spherePartialVolume(radius,r,dr,phi,dphi,theta,dtheta);
               
               // transform
               pnts[idx].transform(trans);
               
               idx++;
            }
         }
      }
      return pnts;
   }
   
   private static double spherePartialVolume(double radius, double r, double dr, double phi, double dphi, double theta, double dtheta) {
      double v = (4.0*r*r*dr+dr*dr*dr/3)*dphi*dtheta/(2*Math.PI);
      return v;
   }
   
   public static CubaturePoint3d[] getSphericalMidpointCubature(Point3d center, double radius, int nR, int nPhi, int nTheta) {
      return getSphericalMidpointCubature(center, radius, nR, nPhi, nTheta, null);
   }
   
   public static CubaturePoint3d[] getSphericalMidpointCubature(Point3d center, double radius, int n) {
      return getSphericalMidpointCubature(center, radius, n, 4*n, 2*n, null);
   }
   
   public static CubaturePoint3d[] getSphericalCapMidpointCubature(Point3d center, double radius, double height, int nR, int nTheta, int nH, Vector3d axis) {
      
      CubaturePoint3d[] pnts = new CubaturePoint3d[nR*nTheta*nH];
      
      double dr = radius/nR;
      double dtheta = 2*Math.PI/nTheta;
      double dh = height/nH;
      
      RigidTransform3d trans = new RigidTransform3d();      
      if (axis != null) {
         RotationMatrix3d R = new RotationMatrix3d();
         R.rotateZDirection(axis);
         trans.setRotation(R);
      }
      Vector3d t = new Vector3d(axis);
      t.normalize();
      t.scale(radius-height);
      t.add(center);
      trans.setTranslation(t);
      
      double r,theta,h,s;
      int idx = 0;
      for (int i=0; i<nR; i++) {
         for (int j=0; j<nTheta; j++) {
            for (int k=0; k<nH; k++) {
               h = k*dh+dh/2;
               theta = j*dtheta+(i+k)*dtheta/2;
               r = i*dr+dr/2;
               s = Math.sqrt(-h*h+2*h*height-2*h*radius-height*height+2*height*radius)/radius;
               
               pnts[idx] = new CubaturePoint3d();
               pnts[idx].x = r*s*Math.cos(theta);
               pnts[idx].y = r*s*Math.sin(theta);
               pnts[idx].z = h;
               pnts[idx].w = sphereCapPartialVolume(height, radius, r, dr, h, dh, theta, dtheta);
               
               pnts[idx].transform(trans);
               
               idx++;
            }
         }
      }
      
      return pnts;
      
   }
   
   private static double sphereCapPartialVolume(double height, double radius, double r, double dr, 
      double h, double dh, double theta, double dtheta) {
      
      double v = -(1.0/12.0)*dtheta*r*dr*dh*(12*h*h+dh*dh+24*h*radius
         -24*height*h+12*height*height-24*height*radius)/(radius*radius);
      return v;
   }

   public static CubaturePoint3d[] getSphericalCapMidpointCubature(Point3d center, double radius, double height, int nR, int nTheta, int nH) {
      return getSphericalCapMidpointCubature(center, radius, height, nR, nTheta, nH, null);
   }
   
   public static CubaturePoint3d[] getSphericalCapMidpointCubature(Point3d center, double radius, double height, int n) {
      return getSphericalCapMidpointCubature(center, radius, height, n, 4*n, (int)Math.ceil(2*n*(height/2*radius)));
   }
   
   public static CubaturePoint3d[] getSphericalLensMidpointCubature(Point3d c1, double r1, Point3d c2, double r2, int nR, int nTheta, int nH1, int nH2) {
      
      double d = c2.distance(c1);
      if (d > r1 + r2) {
         return null;
      }
      
      // c2 -> c1
      Vector3d axis = new Vector3d(c2);
      axis.sub(c1);
      if (d > 1e-10) {
         axis.normalize();
      } else {
         axis.set(0,0,1);
      }
      
      if (d <= r2 - r1) {
         // r1 is inside r2
         return getSphericalMidpointCubature(c1, r1, nR, nTheta, nH1, axis);
      } else if (d <= r1-r2) {
         // r2 is inside r1
         return getSphericalMidpointCubature(c2, r2, nR, nTheta, nH2, axis);
      }
      
      double h1 = (r2-r1+d)*(r2+r1-d)/(2*d);
      double h2 = (r1-r2+d)*(r1+r2-d)/(2*d);
         
      
      CubaturePoint3d[] out = new CubaturePoint3d[(nH1+nH2)*nR*nTheta];
      CubaturePoint3d[] cap1 = getSphericalCapMidpointCubature(c1, r1, h1, nR, nTheta, nH1, axis);
      axis.negate();
      CubaturePoint3d[] cap2 = getSphericalCapMidpointCubature(c2, r2, h2, nR, nTheta, nH2, axis);
      System.arraycopy(cap1, 0, out, 0, cap1.length);
      System.arraycopy(cap2, 0, out, cap1.length, cap2.length);
      
      return out;
      
   }
   
   public static CubaturePoint3d[] getSphericalLensMidpointCubature(Point3d c1, double r1, Point3d c2, double r2, int n) {
      
      double d = c2.distance(c1);
      if (d > r1 + r2) {
         return null;
      }
      
      // c1 -> c2
      Vector3d axis = new Vector3d(c2);
      axis.sub(c1);
      if (d > 1e-10) {
         axis.normalize();
      } else {
         axis.set(0,0,1);
      }

      if (d <= r2 - r1) {
         // r1 is inside r2
         return getSphericalMidpointCubature(c1, r1, n, 4*n, 2*n, axis);
      } else if (d <= r1-r2) {
         // r2 is inside r1
         return getSphericalMidpointCubature(c2, r2, n, 4*n, 2*n, axis);
      }
      
      double h1 = (r2-r1+d)*(r2+r1-d)/(2*d);
      double h2 = (r1-r2+d)*(r1+r2-d)/(2*d);

      int nR = n;
      int nTheta = 4*n;
      int nH1 = (int)Math.ceil(h1/(r1+r2)*2*n);
      int nH2 = (int)Math.ceil(h2/(r1+r2)*2*n);
      
      CubaturePoint3d[] out = new CubaturePoint3d[(nH1+nH2)*nR*nTheta];
      CubaturePoint3d[] cap1 = getSphericalCapMidpointCubature(c1, r1, h1, nR, nTheta, nH1, axis);
      axis.negate();
      CubaturePoint3d[] cap2 = getSphericalCapMidpointCubature(c2, r2, h2, nR, nTheta, nH2, axis);
      System.arraycopy(cap1, 0, out, 0, cap1.length);
      System.arraycopy(cap2, 0, out, cap1.length, cap2.length);
      
      return out;
   }
   
}
