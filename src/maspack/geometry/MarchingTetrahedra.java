package maspack.geometry;

import java.util.*;
import maspack.util.*;
import maspack.matrix.*;
import maspack.geometry.*;

/**
 * Implements MarchingTetrahedra to generate a mesh from a scalar field defined
 * on a grid. Code provided by http://paulbourke.net/geometry/polygonise.
 */
public class MarchingTetrahedra {

   private boolean myMultilinearInterp = true;

   // Edge codes describing all possible vertex connections for a cube. Codes
   // are arranged in increasing upper triangular order, according to the
   // formula
   //
   // e_i_j = i*w - (i*(i+1))/2 + j - i - 1
   //
   // where w = 8 and i and j are vertex indices in the range [0,7]

   private static final int EDGE_0_1 = 0;
   private static final int EDGE_0_2 = 1;
   private static final int EDGE_0_3 = 2;
   private static final int EDGE_0_4 = 3;
   private static final int EDGE_0_5 = 4;
   private static final int EDGE_0_6 = 5;
   private static final int EDGE_0_7 = 6;

   private static final int EDGE_1_2 = 7;
   private static final int EDGE_1_3 = 8;
   private static final int EDGE_1_4 = 9;
   private static final int EDGE_1_5 = 10;
   private static final int EDGE_1_6 = 11;
   private static final int EDGE_1_7 = 12;

   private static final int EDGE_2_3 = 13;
   private static final int EDGE_2_4 = 14;
   private static final int EDGE_2_5 = 15;
   private static final int EDGE_2_6 = 16;
   private static final int EDGE_2_7 = 17;

   private static final int EDGE_3_4 = 18;
   private static final int EDGE_3_5 = 19;
   private static final int EDGE_3_6 = 20;
   private static final int EDGE_3_7 = 21;

   private static final int EDGE_4_5 = 22;
   private static final int EDGE_4_6 = 23;
   private static final int EDGE_4_7 = 24;

   private static final int EDGE_5_6 = 25;
   private static final int EDGE_5_7 = 26;

   private static final int EDGE_6_7 = 27;


   // For each edge, this table gives the additional vertices that are required
   // to interpolate values along its length, using multilinear interpolation
   // on within the cube. Edges that are aligned with the axes are interpolated
   // linearly and do not require additional vertices.  Edges that are
   // diagonals of a cube face require the other two vertices of that face.
   // Finally, edges that are

   private static int[][] interpVertices = new int[28][];

   static {
      // Edges that correspond to diagonals of a cube face are interpolated
      // using bilinear interpolation involving all 4 face vertices. The
      // additional vertices are the additional face vertices.
      interpVertices[EDGE_0_2] = new int[] {1, 3};
      interpVertices[EDGE_0_5] = new int[] {1, 4};
      interpVertices[EDGE_0_7] = new int[] {3, 4};
      interpVertices[EDGE_1_6] = new int[] {2, 5};
      interpVertices[EDGE_3_6] = new int[] {2, 7};
      interpVertices[EDGE_4_6] = new int[] {5, 7};
      interpVertices[EDGE_1_3] = new int[] {0, 2};
      interpVertices[EDGE_1_4] = new int[] {0, 5};
      interpVertices[EDGE_2_5] = new int[] {1, 6};
      interpVertices[EDGE_2_7] = new int[] {3, 6};
      interpVertices[EDGE_3_4] = new int[] {0, 7};
      interpVertices[EDGE_5_7] = new int[] {4, 6};

      // Edges that correspond to antipodal diagonals of the cube are
      // interpolated using trilinear interpolation involving all 8 cube
      // vertices. For edge (i,j), the additional vertices are the 3 vertices
      // nearest to i followed by the 3 vertices nearest to j.

      interpVertices[EDGE_0_6] = new int[] {1, 3, 4, 2, 5, 7};
      interpVertices[EDGE_1_7] = new int[] {0, 2, 5, 3, 4, 6};
      interpVertices[EDGE_2_4] = new int[] {1, 3, 6, 0, 5, 7};
      interpVertices[EDGE_3_5] = new int[] {0, 2, 7, 1, 4, 6};
   }
      

   public class GridPointData {
      Point3d myPos;
      int myIdx;
      double myVal;

      GridPointData (Point3d pos, int idx, double val) {
         myPos = new Point3d(pos);
         myIdx = idx;
         myVal = val;
      }
   }

   public class GridPointGenerator {

      Vector3d myMinCoord;
      Vector3d myWidths;
      int myNumVX;
      int myNumVY;
      int myNumVZ;

      GridPointData[] myData;

      GridPointGenerator (Vector3d minCoord, Vector3d widths, Vector3i res) {
         myNumVX = res.x+1;
         myNumVY = res.y+1;
         myNumVZ = res.z+1;
         myMinCoord = minCoord;
         myWidths = widths;
         myData = new GridPointData[maxGridPoints()];
      }

      int maxGridPoints() {
         return myNumVX*myNumVY*myNumVZ;
      }        

      int getGridPointIndex (int i, int j, int k) {
         return i + j*myNumVX + k*myNumVX*myNumVY;
      }

      GridPointData getGridPointData (
         int idx, double[] vals, int i, int j, int k) {

         GridPointData data = myData[idx];
         if (data == null) {
            Point3d pos = new Point3d();
            pos.x = i * myWidths.x + myMinCoord.x;
            pos.y = j * myWidths.y + myMinCoord.y;
            pos.z = k * myWidths.z + myMinCoord.z;
            data = new GridPointData (pos, idx, vals[idx]);
            myData[idx] = data;
         }
         return data;
      }

      boolean updateCellData (
         GridPointData[] gdata, double[] vals, int i, int j, int k, double iso) {

         int i0 = getGridPointIndex (i, j, k);
         int i1 = getGridPointIndex (i+1, j, k);
         int i2 = getGridPointIndex (i+1, j, k+1);
         int i3 = getGridPointIndex (i, j, k+1);
         int i4 = getGridPointIndex (i, j+1, k);
         int i5 = getGridPointIndex (i+1, j+1, k);
         int i6 = getGridPointIndex (i+1, j+1, k+1);
         int i7 = getGridPointIndex (i, j+1, k+1);

         int nneg = 0;
         if (vals[i0] < iso) nneg++;
         if (vals[i1] < iso) nneg++;
         if (vals[i2] < iso) nneg++;
         if (vals[i3] < iso) nneg++;
         if (vals[i4] < iso) nneg++;
         if (vals[i5] < iso) nneg++;
         if (vals[i6] < iso) nneg++;
         if (vals[i7] < iso) nneg++;
         if (nneg == 0 || nneg == 8) {
            return false;
         }
         gdata[0] = getGridPointData (i0, vals, i, j, k);
         gdata[1] = getGridPointData (i1, vals, i+1, j, k);
         gdata[2] = getGridPointData (i2, vals, i+1, j, k+1);
         gdata[3] = getGridPointData (i3, vals, i, j, k+1);
         gdata[4] = getGridPointData (i4, vals, i, j+1, k);
         gdata[5] = getGridPointData (i5, vals, i+1, j+1, k);
         gdata[6] = getGridPointData (i6, vals, i+1, j+1, k+1);
         gdata[7] = getGridPointData (i7, vals, i, j+1, k+1);
         return true;
      }
   };

   boolean debug = false;

   /**
    * Creates an isosurface mesh from a 3D grid of values. The grid has a
    * resolution of <code>res.x</code> X <code>res.y</code> X
    * <code>res.z</code> cells, with the x, y, and z widths of each cell given
    * by <code>cellWidths</code>, and a minimum vertex located at
    * <code>minCoord</code>. The number of vertices along the x, y, and z axes
    * is hence <code>numVX = res.x+1</code>, <code>numVY = res.y+1</code>, and
    * <code>numVZ = res.z+1</code>. The values at each vertex are stored in the
    * array <code>vals</code>, and arranged, for the vertex at <code>(i, j,
    * k)</code> by
    * <pre>
    *   val[vertex(i,j,k)] = [i + j*numVX + k*numVX*numVY];
    * </pre>
    *
    * @param vals contains grid values at each vertex
    * @param minCoord value of minimum vertex at (0, 0, 0)
    * @param cellWidths widths of each grid cell along the x, y, and z
    * axes
    * @param res number of cells along each of the x, y, and z axes
    * @param iso value to be used to create the isosurface
    * @return mesh corresponding to the iso surface
    */
   public PolygonalMesh createMesh (
      double[] vals, Vector3d minCoord, Vector3d cellWidths,
      Vector3i res, double iso) {
      
      GridPointGenerator vgen = new GridPointGenerator (minCoord, cellWidths, res);

      if (vals.length < vgen.maxGridPoints()) {
         throw new IllegalArgumentException (
            "vals insufficiently long; should have length of "+
            vgen.maxGridPoints());
      }
      LinkedHashMap<VertexId,Vertex3d> vertexMap =
         new LinkedHashMap<VertexId,Vertex3d>();
      ArrayList<int[]> triangles = new ArrayList<int[]>();

      GridPointData[] gdata = new GridPointData[8];
      int[] idxs = new int[8];

      for (int i=0; i<res.x; i++) {
         for (int j=0; j<res.y; j++) {
            for (int k=0; k<res.z; k++) {
               //debug = (i==1 && j==0 && k==1);
               if (vgen.updateCellData (gdata, vals, i, j, k, iso)) {
                  if (debug) {
                     for (int l=0; l<gdata.length; l++) {
                        GridPointData gd = gdata[l];
                        System.out.println (
                           ""+l+"  "+gd.myPos.toString("%8.3f")+" "+gd.myVal);
                     }
                  }
                  polygonizeTet (triangles, gdata, 0, 3, 7, 6, iso, vertexMap);
                  polygonizeTet (triangles, gdata, 0, 7, 4, 6, iso, vertexMap);
                  polygonizeTet (triangles, gdata, 0, 4, 5, 6, iso, vertexMap);
                  polygonizeTet (triangles, gdata, 0, 5, 1, 6, iso, vertexMap);
                  polygonizeTet (triangles, gdata, 0, 1, 2, 6, iso, vertexMap);
                  polygonizeTet (triangles, gdata, 0, 2, 3, 6, iso, vertexMap);
               }
            }
         }
      }

      // int bad = 0;
      // for (int i=0; i<32; i++) {
      //    if (badTris[i] > 0) {
      //       System.out.printf (
      //          "bad tri 0x%x %d  %d/%d\n", i/2, i%2, badTris[i], allTris[i]);
      //       bad += badTris[i];
      //    }
      // }
      // System.out.println ("bad tris=" + bad + "/" + triangles.size());
      PolygonalMesh mesh = new PolygonalMesh();
      for (Vertex3d v : vertexMap.values()) {
         mesh.addVertex (v);
      }
      for (int[] tri : triangles) {
         mesh.addFace (tri);
      }
      System.out.println ("closed=" + mesh.isClosed());
      return mesh;      
   }

   // private int[] badTris  = new int[32];      
   // private int[] allTris  = new int[32];      

   void printTriangle (int[] tri) {
      Point3d p0 = myVerts.get(tri[0]).pnt;
      Point3d p1 = myVerts.get(tri[1]).pnt;
      Point3d p2 = myVerts.get(tri[2]).pnt;
      System.out.println ("  "+p0.toString("%8.3f"));
      System.out.println ("  "+p1.toString("%8.3f"));
      System.out.println ("  "+p2.toString("%8.3f"));
   }         

   double interpVertex (Point3d p, GridPointData[] gdata) {
      Point3d p0 = gdata[0].myPos;
      double wx = gdata[1].myPos.x - p0.x;
      double wy = gdata[4].myPos.y - p0.y;
      double wz = gdata[3].myPos.z - p0.z;

      boolean fail = false;
         // find interpolated value at p and make sure it is 0

      double sx = (p.x - p0.x) / wx;
      double sy = (p.y - p0.y) / wy;
      double sz = (p.z - p0.z) / wz;
      
      double w000 = (1-sx)*(1-sy)*(1-sz);
      double w001 = (1-sx)*(1-sy)*sz;
      double w010 = (1-sx)*sy*(1-sz);
      double w011 = (1-sx)*sy*sz;
      double w100 = sx*(1-sy)*(1-sz);
      double w101 = sx*(1-sy)*sz;
      double w110 = sx*sy*(1-sz);
      double w111 = sx*sy*sz;
      
      double val =
         w000*gdata[0].myVal +
         w001*gdata[3].myVal +
         w010*gdata[4].myVal +
         w011*gdata[7].myVal +
         w100*gdata[1].myVal +
         w101*gdata[2].myVal +
         w110*gdata[5].myVal +
         w111*gdata[6].myVal;
      
      return val;
   }

   // boolean testVertices (int[] tri, GridPointData[] gdata) {
   //    Point3d p0 = gdata[0].myPos;
   //    double wx = gdata[1].myPos.x - p0.x;
   //    double wy = gdata[4].myPos.x - p0.y;
   //    double wz = gdata[3].myPos.x - p0.z;

   //    boolean fail = false;
   //    for (int i=0; i<3; i++) {
   //       double val = interpVertex (myVerts.get(tri[i]).pnt, gdata);
   //       if (Math.abs(val) > 1e-8) {
   //          valOK++;
   //       }
   //       else {
   //          fail = true;
   //          valBad++;
   //       }
   //    }
   //    return fail;
   // }

   // boolean testTriangle (int num, int[] tri, int code, Point3d px) {

   //    Point3d p0 = myVerts.get(tri[0]).pnt;
   //    Point3d p1 = myVerts.get(tri[1]).pnt;
   //    Point3d p2 = myVerts.get(tri[2]).pnt;

   //    boolean inside = ((code & 0x1) != 0); // px should be inside the triangle
      
   //    Vector3d d01 = new Vector3d();
   //    Vector3d d02 = new Vector3d();
   //    Vector3d d0x = new Vector3d();
   //    Vector3d nrm = new Vector3d();
   //    d01.sub (p1, p0);
   //    d02.sub (p2, p0);
   //    d0x.sub (px, p0);
   //    nrm.cross (d01, d02);
   //    nrm.normalize();
   //    boolean fail = false;
   //    if (inside && nrm.dot(d0x) > 0) {
   //       badTris[2*code+num]++;
   //       fail = true;
   //    }
   //    else if (!inside && nrm.dot(d0x) < 0) { 
   //       badTris[2*code+num]++;
   //       fail = true;
   //    }
   //    allTris[2*code+num]++;
   //    if (fail) {
   //       int tmp = tri[1];
   //       tri[1] = tri[2];
   //       tri[2] = tmp;
   //    }
   //    return fail;
   // }

   /**
    * Output the triangles needed to polygonize a given tet.  The nodes of the
    * tet are indicated by the indices k0, k1, k2, k3 with respect to the nodes
    * given in gdata. The tet should be oriented so that nodes k0, k1, k2 are
    * oriented counter-clockwise with respect to the outer facing normal, and
    * k3 is hence "inside".
    */
   int polygonizeTet (
      ArrayList<int[]> triangles, GridPointData[] gdata,
      int k0, int k1, int k2, int k3, 
      double iso, HashMap<VertexId,Vertex3d> vertexMap) {

      int nprev = triangles.size();
      int tricode;

      /*
        Determine which of the 16 cases we have given which vertices
        are above or below the isosurface
      */
      tricode = 0;
      if (gdata[k0].myVal < iso) tricode |= 1;
      if (gdata[k1].myVal < iso) tricode |= 2;
      if (gdata[k2].myVal < iso) tricode |= 4;
      if (gdata[k3].myVal < iso) tricode |= 8;

      // if (!codesPrinted[tricode]) {
      //    System.out.printf ("code %x\n", tricode);
      //    codesPrinted[tricode] = true;
      // }

      int t0 = 0;
      int t1 = 1;
      int t2 = 2;

      /* Form the vertices of the triangles for each case */
      switch (tricode) {
         case 0x00:
         case 0x0F: {
            break;
         }
         case 0x01: {
            t1 = 2; t2 = 1; // flip triangle
         }
         case 0x0E: {
            int[] tr0 = new int[3]; 
            tr0[t0] = getOrCreateVertex(gdata,k0,k1,iso,vertexMap);
            tr0[t1] = getOrCreateVertex(gdata,k0,k2,iso,vertexMap);
            tr0[t2] = getOrCreateVertex(gdata,k0,k3,iso,vertexMap);
            triangles.add (tr0);
            break;
         }
         case 0x02: {
            t1 = 2; t2 = 1; // flip triangle
         }
         case 0x0D: {
            int[] tr0 = new int[3];
            tr0[t0] = getOrCreateVertex(gdata,k1,k0,iso,vertexMap);
            tr0[t1] = getOrCreateVertex(gdata,k1,k3,iso,vertexMap);
            tr0[t2] = getOrCreateVertex(gdata,k1,k2,iso,vertexMap);
            triangles.add (tr0);
            break;
         }
         case 0x03: {
            t1 = 2; t2 = 1; // flip triangle
         }
         case 0x0C: {
            int[] tr0 = new int[3];
            tr0[t0] = getOrCreateVertex(gdata,k0,k3,iso,vertexMap);
            tr0[t1] = getOrCreateVertex(gdata,k1,k3,iso,vertexMap);
            tr0[t2] = getOrCreateVertex(gdata,k0,k2,iso,vertexMap);
            triangles.add (tr0);

            int[] tr1 = new int[3];
            tr1[t0] = tr0[t1];
            tr1[t1] = getOrCreateVertex(gdata,k1,k2,iso,vertexMap);
            tr1[t2] = tr0[t2];
            triangles.add (tr1);
            break;
         }
         case 0x04: {
            t1 = 2; t2 = 1; // flip triangles
         }
         case 0x0B: {
            int[] tr0 = new int[3];
            tr0[t0] = getOrCreateVertex(gdata,k2,k0,iso,vertexMap);
            tr0[t1] = getOrCreateVertex(gdata,k2,k1,iso,vertexMap);
            tr0[t2] = getOrCreateVertex(gdata,k2,k3,iso,vertexMap);
            triangles.add (tr0);
            break;
         }
         case 0x05: {
            t1 = 2; t2 = 1; // flip triangles
         }
         case 0x0A: {
            int[] tr0 = new int[3];
            tr0[t0] = getOrCreateVertex(gdata,k0,k1,iso,vertexMap);
            tr0[t1] = getOrCreateVertex(gdata,k2,k3,iso,vertexMap);
            tr0[t2] = getOrCreateVertex(gdata,k0,k3,iso,vertexMap);
            triangles.add (tr0);
            int[] tr1 = new int[3];
            tr1[t0] = tr0[t0];
            tr1[t1] = getOrCreateVertex(gdata,k1,k2,iso,vertexMap);
            tr1[t2] = tr0[t1];
            triangles.add (tr1);
            break;
         }
         case 0x06: {
            t1 = 2; t2 = 1; // flip triangles
         }
         case 0x09: {
            int[] tr0 = new int[3];
            tr0[t0] = getOrCreateVertex(gdata,k0,k1,iso,vertexMap);
            tr0[t1] = getOrCreateVertex(gdata,k1,k3,iso,vertexMap);
            tr0[t2] = getOrCreateVertex(gdata,k2,k3,iso,vertexMap);
            triangles.add (tr0);
            int[] tr1 = new int[3];
            tr1[t0] = tr0[t2];
            tr1[t1] = getOrCreateVertex(gdata,k0,k2,iso,vertexMap);
            tr1[t2] = tr0[t0];
            triangles.add (tr1);
            break;
         }
         case 0x08: {
            t1 = 2; t2 = 1; // flip triangles
         }
         case 0x07: {
            int[] tr0 = new int[3];
            tr0[t0] = getOrCreateVertex(gdata,k3,k0,iso,vertexMap);
            tr0[t1] = getOrCreateVertex(gdata,k3,k2,iso,vertexMap);
            tr0[t2] = getOrCreateVertex(gdata,k3,k1,iso,vertexMap);
            triangles.add (tr0);
            break;
         }
      }
      int ntri = triangles.size()-nprev;
      if (debug && ntri > 0) {
         System.out.printf (
            "added %d, code=%x, tet=%d %d %d %d\n",
            ntri, tricode, k0, k1, k2, k3);
         for (int i=nprev; i<triangles.size(); i++) {
            System.out.println ("----");
            printTriangle (triangles.get(i));
         }
      }
      // for (int i=nprev; i<triangles.size(); i++) {
      //    testVertices (triangles.get(i), gdata);
      // }

      
      return(triangles.size()-nprev);
   }


   private class VertexId {
      
      int myIdx0;
      int myIdx1;

      VertexId (int idx0) {
         this (-1, idx0);
      }

      VertexId (int idx0, int idx1) {
         if (idx0 < idx1) {
            myIdx0 = idx0;
            myIdx1 = idx1;
         }
         else {
            myIdx0 = idx1;
            myIdx1 = idx0;           
         }
      }

      public boolean equals (Object obj) {
         if (obj instanceof VertexId) {
            VertexId vid = (VertexId)obj;
            return (vid.myIdx0 == myIdx0 && vid.myIdx1 == myIdx1);
         }
         else {
            return false;
         }
      }

      public int hashCode() {
         return myIdx0 + 63719*myIdx1;
      }
   }
         
   double EPS = 1e-10;

   ArrayList<Vertex3d> myVerts = new ArrayList<Vertex3d>();

   double solveLinear (
      GridPointData[] gdata, int k0, int k1, double iso) {
      
      double v0 = gdata[k0].myVal;
      double v1 = gdata[k1].myVal;
      double mu = (iso-v0)/(v1-v0);
      if (mu < EPS) {
         mu = 0;
      }
      else if (mu > 1-EPS) {
         mu = 1;
      }
      return mu;
   }

   double solveQuadratic (
      GridPointData[] gdata,
      int k00, int k11, int k01, int k10, double iso) {

      double v00 = gdata[k00].myVal;
      double v11 = gdata[k11].myVal;
      double v01 = gdata[k01].myVal;
      double v10 = gdata[k10].myVal;

      double a = v00-v01-v10+v11;
      double b = v01+v10-2*v00;
      double c = v00 - iso;

      double mu = 0;
      if (a == 0) {
         // turns into a linear equation
         mu = (iso-v00)/(v11-v00);
      }
      else {
         // More stable formula from Numerical Recipes:
         double d = b*b - 4*a*c;
         if (d < 0) {
            d = 0; // clamp to 0, since there should be at least one root
         }
         double q = Math.sqrt(d);
         double r0, r1;
         if (b >= 0) {
            r0 = (-b-q)/(2*a);
            r1 = (2*c)/(-b-q);
         }
         else {
            r0 = (2*c)/(-b+q);
            r1 = (-b+q)/(2*a);
         }
         // r0 and r1 are roots in ascending order. Find the one in the
         // interval [0,1]. Clamp results if we have to
         if (r0 >= 0 && r0 <= 1) {
            mu = r0;
         }
         else if (r1 >= 0 && r1 <= 1) {
            mu = r1;
         }
         else {
            // shouldn't happen; pick nearest root
            if (r1 < 0) {
               mu = 0;
            }
            else if (r0 > 1) {
               mu = 1;
            }
            else if (r1-1 < -r0) {
               mu = 1;
            }
            else {
               mu = 0;
            }
         }
      }
      if (mu < EPS) {
         mu = 0;
      }
      else if (mu > 1-EPS) {
         mu = 1;
      }
      return mu;
   }

   double solveCubic (
      GridPointData[] gdata,
      int k000, int k111, int k001, int k010, int k100,
      int k011, int k101, int k110, double iso) {

      double v000 = gdata[k000].myVal;
      double v111 = gdata[k111].myVal;
      double v001 = gdata[k001].myVal;
      double v010 = gdata[k010].myVal;
      double v100 = gdata[k100].myVal;
      double v011 = gdata[k011].myVal;
      double v101 = gdata[k101].myVal;
      double v110 = gdata[k110].myVal;

      double va = gdata[k001].myVal + gdata[k010].myVal + gdata[k100].myVal;
      double vb = gdata[k011].myVal + gdata[k101].myVal + gdata[k110].myVal;

      double a = va-v000-vb+v111;
      double b = 3*v000-2*va+vb;
      double c = va-3*v000;
      double d = v000 - iso;

      double[] roots = new double[3];
      int numr = CubicSolver.getRoots (roots, a, b, c, d, 0, 1);
      double mu;
      if (numr == 1 || numr == 3) {
         // numr == 3 is unlikely; default to first root in this case
         mu = roots[0];
      }
      else {
         // this shouldn't happen - there should be an odd number of roots
         mu = roots[0];
      }
      if (mu < EPS) {
         mu = 0;
      }
      else if (mu > 1-EPS) {
         mu = 1;
      }
      return mu;
   }

   int getOrCreateVertex (
      GridPointData[] gdata, int k0, int k1, double iso,
      HashMap<VertexId,Vertex3d> vertexMap) {

      if (k0 > k1) {
         int kt = k0; k0 = k1; k1 = kt;
      }

      double mu = 0;
      int[] xv = null;
      int edgeCode = 0;
      if (!myMultilinearInterp) {
         mu = solveLinear (gdata, k0, k1, iso);
      }
      else {
         edgeCode = k0*8 - (k0*(k0+1))/2 + k1 - k0 - 1;
         xv = interpVertices[edgeCode];
         // for the edge, find the extra vertices needed for interpolation, if
         // any

         if (xv == null) {
            mu = solveLinear (gdata, k0, k1, iso);
         }
         else if (xv.length == 2) {
            mu = solveQuadratic (
               gdata, k0, k1, xv[0], xv[1], iso);
         }
         else {
            mu = solveCubic (
               gdata, k0, k1, xv[0], xv[1], xv[2], xv[3], xv[4], xv[5], iso);
         }
      }
      Point3d p = new Point3d();
      VertexId vid;
      if (mu == 0) {
         vid = new VertexId (gdata[k0].myIdx);
         p.set (gdata[k0].myPos);
      }
      else if (mu == 1) {
         vid = new VertexId (gdata[k1].myIdx);
         p.set (gdata[k1].myPos);
      }
      else {
         vid = new VertexId (gdata[k0].myIdx, gdata[k1].myIdx);
         p.combine (1-mu, gdata[k0].myPos, mu, gdata[k1].myPos);
      }
      if (myMultilinearInterp) {
         double val = interpVertex (p, gdata);
         if (Math.abs(val-iso) > 1e-8) {
            System.out.println (
               "bad vertex val=" + val +
               " mu=" + mu + " nx=" + (xv == null ? 0 : xv.length) +
               " k0=" + k0 + " k1=" + k1 + " edgeCode=" + edgeCode);
         }
      }
      
      Vertex3d vtx = vertexMap.get (vid);
      if (vtx == null) {
         vtx = new Vertex3d (p);
         vtx.setIndex (vertexMap.size());
         myVerts.add (vtx);
         vertexMap.put (vid, vtx);
      }
      return vtx.getIndex();
   }

}
