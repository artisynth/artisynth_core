/**
 * Copyright (c) 2017, by the Authors: Bruce Haines (UBC), Antonio Sanchez
 * (UBC), John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.util.Arrays;

import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.matrix.Vector3i;
import maspack.render.Renderable;
import maspack.util.Logger;

/**
 * Implements a signed distance field for fixed triangular mesh. One common
 * use of such a field is to detect point penetration distances and normals for
 * use in collision handling.
 *
 * <p>The field is implemented using a regular 3D grid composed of
 * <code>numVX</code> X <code>numVY</code> X <code>numVZ</code> vertices along
 * the x, y and z directions, dividing the volume into <code>(numVX-1)</code> X
 * <code>(numVY-1)</code> X <code>(numVZ-1)</code> cells. For vertices close to
 * the mesh, nearby faces are examined to determine the distance from the mesh
 * to the vertex. A sweep method and ray-casting are then used to propogate
 * distance values throughout the volume and determine whether vertices are
 * inside or outside. The algorithm is based on C++ code provided by Robert
 * Bridson at UBC.
 *
 * <p>The grid is constructed in local mesh coordinates, and all queries are
 * assumed to be performed in local mesh coordinates. The mesh is assumed to be
 * closed; if it is not, the results are undefined. The distance field is
 * defined so as to be negative within the mesh and positive outside
 * it. Normals are computed at each vertex using numeric differentation of the
 * distances associated with the surrounding vertices. Trilinear interpolation
 * of vertex values across each cell is used to compute the distance and normal
 * for a general point within the grid volume.
 */
public class SignedDistanceGrid extends DistanceGrid implements Renderable {

   SignedDistanceGrid () {
      super();
   }

//   /**
//    * Creates a new signed distance grid for a specified mesh. The mesh is
//    * created as for {@lin//   /**
// * Creates a new signed distance grid for a specified mesh. The grid is
// * aligned with the x, y, z axes and centered on the mesh. Its width along
// * each axis is computed from the mesh's maximum and minimum bounds, and
// * then enlarged by a margin computed by multiplying the width by
// * <code>marginFraction</code>. The width along each axis is therefore
// * <pre>
// *   width = (1 + 2*marginFraction)*boundsWidth
// * </pre>
// * where <code>boundsWidth</code> is the width determined from the mesh
// * bounds.
// * 
// * @param mesh mesh for which the grid should be created
// * @param marginFraction multiplied by the width in each direction
// * to determine the margin for that direction
// * @param resolution number of grid cells that should be used along
// * each axis
// */
//public SignedDistanceGrid (
//   PolygonalMesh mesh, double marginFraction, Vector3i resolution) {
//   super(mesh.getFaces(), marginFraction, resolution, /*signed=*/true);
//   setWorldTransform(mesh.getMeshToWorld());
//}k #SignedDistanceGrid(PolygonalMesh,double,Vector3i)},
//    * with the number of cells along each axis given by 25.
//    *
//    * @param mesh mesh for which the grid should be created
//    * @param marginFraction multiplied by the width in each direction to
//    * determine the margin for that direction
//    */
//   public SignedDistanceGrid (PolygonalMesh mesh, double marginFraction) {
//      this (mesh, marginFraction, new Vector3i (25, 25, 25));
//   }

//   /**
//    * Creates a new signed distance grid for a specified mesh. The grid is
//    * aligned with the x, y, z axes, is centered on the mesh, and has cells of
//    * uniform size. Its widths are first determined using the mesh bounds and
//    * <code>marginFraction</code> as described for {@link
//    * #SignedDistanceGrid(PolygonalMesh,double,Vector3i)}. The axis with maximum
//    * width is then divided into <code>maxResolution</code> cells, while the
//    * other axes are divided into a number of cells {@code <= maxResolution}
//    * with the same cell w//   /**
// * Creates a new signed distance grid for a specified mesh. The grid is
// * aligned with the x, y, z axes and centered on the mesh. Its width along
// * each axis is computed from the mesh's maximum and minimum bounds, and
// * then enlarged by a margin computed by multiplying the width by
// * <code>marginFraction</code>. The width along each axis is therefore
// * <pre>
// *   width = (1 + 2*marginFraction)*boundsWidth
// * </pre>
// * where <code>boundsWidth</code> is the width determined from the mesh
// * bounds.
// * 
// * @param mesh mesh for which the grid should be created
// * @param marginFraction multiplied by the width in each direction
// * to determine the margin for that direction
// * @param resolution number of grid cells that should be used along
// * each axis
// */
//public SignedDistanceGrid (
//   PolygonalMesh mesh, double marginFraction, Vector3i resolution) {
//   super(mesh.getFaces(), marginFraction, resolution, /*signed=*/true);
//   setWorldTransform(mesh.getMeshToWorld());
//}idth, with the overall axis widths grown as necessary
//    * to accommodate this.
//    * 
//    * @param mesh mesh for which the grid should be created
//    * @param marginFraction multiplied by the width in each direction
//    * to determine the (initial) margin for that direction
//    * @param maxResolution number of grid cells along the axis of maximum
//    * width
//    */
//   public SignedDistanceGrid (
//      PolygonalMesh mesh, double marginFraction, int maxResolution) {
//      super(mesh.getFaces(), marginFraction, maxResolution, /*signed=*/true);
//      setWorldTransform(mesh.getMeshToWorld());
//   }
   
//   /**
//    * Creates a new signed distance grid for a specified mesh. The grid is
//    * aligned with the x, y, z axes and centered on the mesh. Its width along
//    * each axis is computed from the mesh's maximum and minimum bounds, and
//    * then enlarged by a margin computed by multiplying the width by
//    * <code>marginFraction</code>. The width along each axis is therefore
//    * <pre>
//    *   width = (1 + 2*marginFraction)*boundsWidth
//    * </pre>
//    * where <code>boundsWidth</code> is the width determined from the mesh
//    * bounds.
//    * 
//    * @param mesh mesh for which the grid should be created
//    * @param marginFraction multiplied by the width in each direction
//    * to determine the margin for that direction
//    * @param resolution number of grid cells that should be used along
//    * each axis
//    */
//   public SignedDistanceGrid (
//      PolygonalMesh mesh, double marginFraction, Vector3i resolution) {
//      super(mesh.getFaces(), marginFraction, resolution, /*signed=*/true);
//      setWorldTransform(mesh.getMeshToWorld());
//   }

//   /**
//    * Returns the mesh associated with this grid.
//    * 
//    * @return mesh associated with this grid
//    */
//   public PolygonalMesh getMesh() {
//      return myMesh;
//   }
 
//  /** 
//   * Calculates the signed distance field.
//   */
//  protected void calculatePhi (
//     Feature[] features, double marginDist) {
//     //Logger logger = Logger.getSystemLogger();
//     //logger.info ("Calculating Signed Distance Field...");
//
//     System.out.println ("HERE");
//
//     myFeatures = features;
//
//     double maxDist = myMaxCoord.distance (myMinCoord);
//     int numV = numVX*numVY*numVZ;
//
//     myPhi = new double [numV];
//     myNormals = new Vector3d [numV];
//     myColorIndices = new int [numV];
//
//     for (int p = 0; p < myPhi.length; p++) {
//        myPhi[p] = maxDist;
//     }
//     // The index of closestFace matches with phi.
//     // Each entry in closestFace is the index of the closest 
//     // face to the grid vertex.
//     myClosestFeatureIdxs = new int[myPhi.length];
//     int zIntersectionCount[] = new int[myPhi.length];
//     
//     for (int i = 0; i < myPhi.length; i++) {
//        myClosestFeatureIdxs[i] = -1;
//        zIntersectionCount[i] = 0;
//     }
//     Point3d faceMin          = new Point3d();
//     Point3d faceMax          = new Point3d();
//     Point3d nearPntLoc     = new Point3d();
//     // Point3d currentPoint     = new Point3d();
//     Point3d facePntLoc = new Point3d();
//     Point3d bot = new Point3d();
//     Point3d top = new Point3d();
//     
//     // For every triangle...
//     for (int t = 0; t < myFeatures.length; t++) {
//        Face face = (Face)myFeatures[t];
//        faceMin.set (INF, INF, INF);
//        faceMax.set (-INF, -INF, -INF);
//        face.updateBounds (faceMin, faceMax);
//        // Converting mesh min/max to grid coordinates.
//        int faceMinX = (int)((faceMin.x - myMinCoord.x) / myCellWidths.x);
//        int faceMinY = (int)((faceMin.y - myMinCoord.y) / myCellWidths.y);
//        int faceMinZ = (int)((faceMin.z - myMinCoord.z) / myCellWidths.z);
//        if (faceMinX < 0) {
//           faceMinX = 0;
//        }
//        if (faceMinY < 0) {
//           faceMinY = 0;
//        }
//        if (faceMinZ < 0) {
//           faceMinZ = 0;
//        }
//        int faceMaxX = (int)((faceMax.x - myMinCoord.x) / myCellWidths.x) + 1;
//        int faceMaxY = (int)((faceMax.y - myMinCoord.y) / myCellWidths.y) + 1;
//        int faceMaxZ = (int)((faceMax.z - myMinCoord.z) / myCellWidths.z) + 1;
//        if (faceMaxX > numVX - 1) {
//           faceMaxX = numVX - 1;
//        }
//        if (faceMaxY > numVY - 1) {
//           faceMaxY = numVY - 1;
//        }
//        if (faceMaxZ > numVZ - 1) {
//           faceMaxZ = numVZ - 1;
//        }
//
//        // Now go through the entire parallelpiped. Calculate distance and
//        // closestFace.
//        for (int z = faceMinZ; z <= faceMaxZ; z++) {
//           for (int y = faceMinY; y <= faceMaxY; y++) {
//              for (int x = faceMinX; x <= faceMaxX; x++) {
//                 // Get mesh coordinates
//                 facePntLoc.set (x, y, z);
//                 myGridToLocal.transformPnt (facePntLoc, facePntLoc);
//                 //getLocalVertexCoords (facePntLoc, x, y, z);
//                 // Get the distance from this point to the face.
//                 face.nearestPoint (nearPntLoc, facePntLoc);
//                 double distance = facePntLoc.distance (nearPntLoc);
//                 int index = xyzIndicesToVertex (x, y, z);
//                 if (distance < myPhi[index]) {
//                    myPhi[index] = distance;
//                    myClosestFeatureIdxs [index] = t;
//                 }
//              }
//           }
//        }
//        
//        // Ray-casts from bottom x-y plane, upwards, counting intersections.
//        // We're building intersectionCount[] to use in ray casting below.
//        for (int y = faceMinY; y <= faceMaxY; y++) {
//           facePntLoc.y = y * myCellWidths.y + myMinCoord.y;
//           for (int x = faceMinX; x <= faceMaxX; x++) {
//              facePntLoc.x = x * myCellWidths.x + myMinCoord.x;
//
//              bot.x = facePntLoc.x;
//              bot.y = facePntLoc.y;
//              bot.z = myMinCoord.z-1;
//              top.x = facePntLoc.x;
//              top.y = facePntLoc.y;
//              top.z = myMaxCoord.z+1;
//
//              Point3d ipnt = new Point3d();
//              int res = RobustPreds.intersectSegmentTriangle (
//                 ipnt, bot, top, face, maxDist, /*worldCoords=*/false);
//
//              if (res > 0) {
//                 facePntLoc.z = ipnt.z;
//                 // We should now use the z value in grid coordinates.
//                 // Extract it from facePntLoc
//                 double currentPointZ =
//                    (facePntLoc.z - myMinCoord.z) / myCellWidths.z;
//                 int zInterval = (int)currentPointZ + 1;
//                 // intersection counted in next grid square
//                 if (zInterval < 0) {
//                    ++zIntersectionCount [xyzIndicesToVertex (x, y, 0)];
//                 } else if (zInterval < numVZ) {
//                    ++zIntersectionCount[xyzIndicesToVertex(x, y, zInterval)];
//                 }
//              } // point in triangle
//           } // x
//        } // y
//     }
//
//     // Done all triangles.
//     // Sweep, propagating values throughout the grid volume.
//     for (int pass = 0; pass < 2; pass++) {
//        sweep(+1, +1, +1);
//        sweep(-1, -1, -1);
//        sweep(+1, +1, -1);
//        sweep(-1, -1, +1);
//        sweep(+1, -1, +1);
//        sweep(-1, +1, -1);
//        sweep(+1, -1, -1);
//        sweep(-1, +1, +1);
//     }
//     
//     // This is a ray-casting implementation to find the sign of each vertex in
//     // the grid.
//     for (int x = 0; x < numVX; x++) {
//        for (int y = 0; y < numVY; y++) {
//           int total_count = 0;
//           //Count the intersections of the x axis
//           for (int z = 0; z < numVZ; z++) {
//              int index = xyzIndicesToVertex (x, y, z);
//              total_count += zIntersectionCount [index];
//
//              // If parity of intersections so far is odd, we are inside the 
//              // mesh.
//              if (total_count % 2 == 1) {
//                 myPhi[index] =- myPhi[index];
//              }
//           }
//        }
//     }
//     mySignedP = true;
//     //logger.println ("done.");
//  }


//   /** 
//    * Calculates the signed distance field, using bounding volume
//    * hierarchy queries at each vertex. This method can be considerably
//    * slower than the default approach and so is not currently used.
//    */
//   private void calculatePhiBVH (Feature[] features, double marginDist) {
//      Logger logger = Logger.getSystemLogger();
//      logger.info ("Calculating Signed Distance Field...");
//
//      // gridSize represents the maximum point in grid coordinates, rounded
//      // up to the nearest full cell.
//      // +1 for rounding up after casting to an integer.
//      // +1 because it is a node count, not cell count
//      // numVX = (int)(Math.ceil((myMaxCoord.x-myMinCoord.x) / myCellWidths.x))+1; 
//      // numVY = (int)(Math.ceil((myMaxCoord.y-myMinCoord.y) / myCellWidths.y))+1;
//      // numVZ = (int)(Math.ceil((myMaxCoord.z-myMinCoord.z) / myCellWidths.z))+1;
//      // numVXxVY = numVX*numVY;
//      
//      int numV = numVX*numVY*numVZ;
//
//      myPhi = new double [numV];
//      myNormals = new Vector3d [numV];
//      myColorIndices = new int [numV];
//      myFeatures = features;
//
//      // The index of closestFace matches with phi.
//      // Each entry in closestFace is the index of the closest 
//      // face to the grid vertex.
//      myClosestFeatureIdxs = new int[myPhi.length];
//
//      BVFeatureQuery query = new BVFeatureQuery();
//      // create our own bvtree for the mesh, since we can then dispose of it
//      // after we are down building the SD field
//      AABBTree bvtree = new AABBTree ();  
//      bvtree.setMaxLeafElements(2);
//      bvtree.setMargin(marginDist);
//      bvtree.build(Arrays.asList(myFeatures));
//      System.out.println ("tree done");
//
//      // Now go through the entire parallelpiped. Calculate distance and
//      // closestFace.
//      Vector3i vxyz = new Vector3i();
//      Point3d near = new Point3d();
//      Point3d coords = new Point3d();
//      double tol = 1e-12*myMaxCoord.distance(myMinCoord);
//
//      for (int idx=0; idx<myPhi.length; idx++) {
//         vertexToXyzIndices (vxyz, idx);
//         getLocalVertexCoords (coords, vxyz);
//         // Get the distance from this point to the face.
//         boolean inside = query.isInsideOrientedMesh (bvtree, coords, tol);
//         Face face = query.getFaceForInsideOrientedTest (near, null);
//         
//         Vector3d diff = new Vector3d();
//         Vector3d normal = new Vector3d();
//         diff.sub (coords, near);
//         double dist = diff.norm();
//         if (dist < tol) {
//            face.computeNormal (normal);
//            normal.negate();
//            dist = normal.dot (diff);
//         }
//         else {
//            normal.normalize(diff);
//            if (inside) {
//               normal.negate();
//               dist = -dist;
//            }
//         }
//         myPhi[idx] = dist;
//         //myNormals[idx] = normal;
//         myClosestFeatureIdxs[idx] = face.getIndex();
//      }
//      logger.println ("done.");
//   }
   
 
   /** 
    * Returns the closest face to the vertex indexed by <code>idx</code>.
    *
    * @param idx vertex index
    * @return nearest face to the vertex
    */
   public Face getClosestFace(int idx) {
      return (Face)getClosestFace(idx);
   }

}
