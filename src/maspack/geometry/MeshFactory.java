/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.PriorityQueue;

import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.matrix.Vector3i;
import maspack.util.InternalErrorException;
import quickhull3d.QuickHull3D;

/**
 * Creates specific instances of polygonal meshes.
 */
public class MeshFactory {

   private static final double EPS = 1e-15;
   public static boolean DEFAULT_ADD_NORMALS = false;
   
   /**
    * Used by some factory methods to specify the face type
    */
   public enum FaceType {
      /**
       * Quadrilateral faces
       */
      QUAD,
      /**
       * Triangular faces
       */
      TRI,
      /**
       * Triangular faces, using alternating directions when appropriate
       */
      ALT_TRI
   }

   private static class GriddedPoint {

      long x;
      long y;
      long z;

      public GriddedPoint (Point3d pnt, double res) {
         set (pnt.x, pnt.y, pnt.z, res);
      }

      public GriddedPoint (double x, double y, double z, double res) {
         set (x, y, z, res);
      }

      public void set (double x, double y, double z, double res) {
         this.x = Math.round (x/res);
         this.y = Math.round (y/res);
         this.z = Math.round (z/res);
      }

      @Override
      public boolean equals (Object obj) {
         if (obj instanceof GriddedPoint) {
            GriddedPoint gpnt = (GriddedPoint)obj;
            return gpnt.x == x && gpnt.y == y && gpnt.z == z;
         }
         else {
            return false;
         }
      }

      @Override
      public int hashCode () {
         // the values 23 and 31 are significant only in that they are prime
         int hash = 23;
         hash = hash*31 + (int)x;
         hash = hash*31 + (int)y;
         hash = hash*31 + (int)z;
         return hash;
      }
   }

   public static class VertexMap extends HashMap<GriddedPoint,Vertex3d> {
      private static final long serialVersionUID = 134233432L;
      double myTol;

      public VertexMap (double tol) {
         super();
         myTol = tol;
      }

      public void put (Point3d pnt, Vertex3d vtx) {
         put (new GriddedPoint(pnt, myTol), vtx);
      }
      
      public Vertex3d get (Point3d pnt) {
         return get (new GriddedPoint(pnt, myTol));
      }

//      public void put (double x, double y, double z, Vertex3d vtx) {
//         put (new GriddedPoint(x, y, z, myTol), vtx);
//      }
//      
//      public Vertex3d get (double x, double y, double z) {
//         return get (new GriddedPoint(x, y, z, myTol));
//      }

      public Vertex3d getOrCreate (
         PolygonalMesh mesh, double x, double y, double z, RigidTransform3d XLM) {
         
         Point3d pm = new Point3d (x, y, z);
         if (XLM != null) {
            pm.transform (XLM);
         }
         Vertex3d vtx = (Vertex3d)get (pm);
         if (vtx == null) {
            vtx = new Vertex3d (pm);
            put (pm, vtx);
            mesh.addVertex (vtx);
         }
         return vtx;
      }

      public Vertex3d getOrCreate (
         PolygonalMesh mesh, Point3d pos, RigidTransform3d XLM) {

         return getOrCreate (mesh, pos.x, pos.y, pos.z, XLM);
      }
   }

   /**
    * Adds a quad rectangle to an existing mesh. The rectangle is centered on
    * the x-y plane of a local coordinate frame L, with face normals directed
    * along the z axis. A transform XLM is supplied to map L into the mesh
    * coordinate frame M.
    *
    * @param mesh existing mesh to add the rectangle to
    * @param wx width of the rectangle along the local x axis
    * @param wy width of the rectangle along the local y axis
    * @param nx number of subdivisions along the local x axis
    * @param ny number of subdivisions along the local y axis
    * @param XLM transform from the local coordinate frame L to mesh
    * coordinates M.
    * @param vtxMap spatial hash map storing all existing vertices in the
    * mesh. New vertices will be created as needed and added to both this
    * map and to the mesh.
    */
   public static void addQuadRectangle (
      PolygonalMesh mesh, double wx, double wy, int nx, int ny,
      RigidTransform3d XLM, VertexMap vtxMap) {

      if (nx <= 0) {
         throw new IllegalArgumentException ("nx must be positive");
      }
      if (ny <= 0) {
         throw new IllegalArgumentException ("ny must be positive");
      }

      // create faces starting in the lower left corner.
      double dx = wx/nx;
      double dy = wy/ny;
      for (int i=0; i<nx; i++) {
         double x = -wx/2 + i*dx;
         for (int j=0; j<ny; j++) {
            double y = -wy/2 + j*dy;
            Vertex3d v0 = vtxMap.getOrCreate (mesh, x, y, 0, XLM);
            Vertex3d v1 = vtxMap.getOrCreate (mesh, x+dx, y, 0, XLM);
            Vertex3d v2 = vtxMap.getOrCreate (mesh, x+dx, y+dy, 0, XLM);
            Vertex3d v3 = vtxMap.getOrCreate (mesh, x, y+dy, 0, XLM);
            Face f = mesh.addFace (v0, v1, v2, v3);
            if (i == 0) {
               f.firstHalfEdge ().setHard (true);
            } 
            if (i == nx-1) {
               f.firstHalfEdge ().getNext ().getNext ().setHard (true);
            }
            if (j == 0) {
               f.firstHalfEdge ().getNext ().getNext ().getNext ().setHard (true);
            }
            if (j == ny-1) {
               f.firstHalfEdge ().getNext ().setHard (true);
            }
         }
      }
   }

   /**
    * Create a quad rectangle mesh. The rectangle is centered on the x-y plane
    * of a local coordinate frame L, with face normals directed along the z
    * axis. 
    *
    * @param wx width of the rectangle along the local x axis
    * @param wy width of the rectangle along the local y axis
    * @param nx number of subdivisions along the local x axis
    * @param ny number of subdivisions along the local y axis
    */
   public static PolygonalMesh createQuadRectangle (
      double wx, double wy, int nx, int ny) {

      // set map tolerance to be 0.01 times smallest spacing between vertices
      VertexMap vtxMap = new VertexMap(Math.min(wx/nx*1e-2, wy/ny*1e-2));
      PolygonalMesh mesh = new PolygonalMesh();
      addQuadRectangle (mesh, wx, wy, nx, ny, RigidTransform3d.IDENTITY, vtxMap);
      return mesh;
   }

   /**
    * Adds an annular sector to an existing mesh. The sector is centered on the
    * x-y plane of a local coordinate frame L, with the bisecting axis parallel
    * to x and face normals directed along the z axis. It extends for
    * +/-<code>ang/2</code> on either side of the x axis, and from an inner
    * radius <code>r0</code> to an outer radius <code>r1</code>; if
    * <code>ang</code> is 2 PI and <code>r0</code> is 0 then the sector becomes
    * a circle with radius <code>r1</code>. The mesh is made of quads
    * except for those faces connected to the origin. A transform XLM is
    * supplied to map L into the mesh coordinate frame M.
    *
    * @param mesh existing mesh to add the annular sector to
    * @param r0 inner radius of the sector
    * @param r1 outer radius of the sector
    * @param ang total angular extent of the sector
    * @param nr number of radial slices
    * @param nang number of angular slices
    * @param XLM transform from the local coordinate frame L to mesh
    * coordinates M.
    * @param vtxMap spatial hash map storing all existing vertices in the
    * mesh. New vertices will be created as needed and added to both this
    * map and to the mesh.
    */
   public static void addQuadAnnularSector (
      PolygonalMesh mesh, double r0, double r1, double ang, int nr, int nang,
      RigidTransform3d XLM, VertexMap vtxMap) {

      if (r0 < 0) {
         throw new IllegalArgumentException (
            "inner radius r0="+r0+" must be positive");
      }
      if (r1 <= r0) {
         throw new IllegalArgumentException (
            "outer radius r1="+r1+" must exceed inner radius r0="+r0);
      }
      if (nr <= 0) {
         throw new IllegalArgumentException ("nr must be positive");
      }
      if (nang <= 0) {
         throw new IllegalArgumentException ("nang must be positive");
      }

      // create faces starting from the origin
      double dr = (r1-r0)/nr;
      double dang = ang/nang;
      double ra = r0;
      for (int i=0; i<nr; i++) {
         double rb = r0 + (i+1)*dr;
         double c0 = Math.cos (-ang/2);
         double s0 = Math.sin (-ang/2);
         for (int j=0; j<nang; j++) {
            double c1 = Math.cos (-ang/2 + (j+1)*dang);
            double s1 = Math.sin (-ang/2 + (j+1)*dang);
            Vertex3d v0 = vtxMap.getOrCreate (mesh, ra*c0, ra*s0, 0, XLM);
            Vertex3d v1 = vtxMap.getOrCreate (mesh, rb*c0, rb*s0, 0, XLM);
            Vertex3d v2 = vtxMap.getOrCreate (mesh, rb*c1, rb*s1, 0, XLM);
            Vertex3d v3 = vtxMap.getOrCreate (mesh, ra*c1, ra*s1, 0, XLM);
            if (ra == 0) {
               mesh.addFace (v0, v1, v2);
            }
            else {
               mesh.addFace (v0, v1, v2, v3);
            }
            c0 = c1;
            s0 = s1;
         }
         ra = rb;
      }
   }

   /**
    * Creates an annular sector. The sector is centered on the
    * x-y plane of a local coordinate frame L, with the bisecting axis parallel
    * to x and face normals directed along the z axis. For more details,
    * see the documentation for {@link #addQuadAnnularSector}.
    *
    * @param r0 inner radius of the annular sector
    * @param r1 outer radius of the annular sector
    * @param ang total angular extent of the annular sector
    * @param nr number of radial slices
    * @param nang number of angular slices
    */
   public static PolygonalMesh createQuadAnnularSector (
      double r0, double r1, double ang, int nr, int nang) {

      double rmin = (r0 == 0 ? r1/nr : r0);
      // set map tolerance to be 0.01 times smallest spacing between vertices
      double tol = Math.min(rmin*1e-2, rmin*Math.sin(ang/nang)*1e-2);
      VertexMap vtxMap = new VertexMap(tol);
      PolygonalMesh mesh = new PolygonalMesh();
      addQuadAnnularSector (
         mesh, r0, r1, ang, nr, nang, RigidTransform3d.IDENTITY, vtxMap);
      return mesh;
   }

   /**
    * Adds a cylindrical section to an existing mesh. The section is centered
    * on the z axis of a local coordinate frame L, extending for a length
    * +/-<code>h/2</code> above and below the origin. The angular extent of the
    * section is +/-<code>ang/2</code> on either side of the x axis; if
    * <code>ang</code> is 2 PI then the section will become a whole cylinder.
    * Normals are oriented to face outwards. The sector is made of quads.  A
    * transform XLM is supplied to map L into the mesh coordinate frame M.
    *
    * @param mesh existing mesh to add the section to
    * @param r radius of the cylinder
    * @param h height of the cylinder
    * @param ang total angular extent of the section
    * @param nh number of height slices
    * @param nang number of angular slices
    * @param outward if <code>true</code>, the normals are facing outward
    * @param XLM transform from the local coordinate frame L to mesh
    * coordinates M.
    * @param vtxMap spatial hash map storing all existing vertices in the
    * mesh. New vertices will be created as needed and added to both this
    * map and to the mesh.
    */
   public static void addQuadCylindricalSection (
      PolygonalMesh mesh, double r, double h, double ang, int nh, int nang,
      boolean outward, RigidTransform3d XLM, VertexMap vtxMap) {

      if (nh <= 0) {
         throw new IllegalArgumentException ("nh must be positive");
      }
      if (nang <= 0) {
         throw new IllegalArgumentException ("nang must be positive");
      }
      // create faces starting from the bottom
      double dh = h/nh;
      double dang = ang/nang;
      double h0 = -h/2;
      for (int i=0; i<nh; i++) {
         double h1 = -h/2+(i+1)*dh;
         double c0 = Math.cos (-ang/2);
         double s0 = Math.sin (-ang/2);
         for (int j=0; j<nang; j++) {
            double c1 = Math.cos (-ang/2 + (j+1)*dang);
            double s1 = Math.sin (-ang/2 + (j+1)*dang);
            Vertex3d v0 = vtxMap.getOrCreate (mesh, r*c0, r*s0, h0, XLM);
            Vertex3d v1 = vtxMap.getOrCreate (mesh, r*c1, r*s1, h0, XLM);
            Vertex3d v2 = vtxMap.getOrCreate (mesh, r*c1, r*s1, h1, XLM);
            Vertex3d v3 = vtxMap.getOrCreate (mesh, r*c0, r*s0, h1, XLM);
            if (outward) {
               mesh.addFace (v0, v1, v2, v3);
            }
            else {
               mesh.addFace (v0, v3, v2, v1);
            }
            c0 = c1;
            s0 = s1;
         }
         h0 = h1;
      }
   }

   /**
    * Creates a cylindrical section. The section is centered on the z axis of a
    * local coordinate frame L, extending for a length +/-<code>h/2</code>
    * above and below the origin. The angular extent of the section is
    * +/-<code>ang/2</code> on either side of the x axis; if <code>ang</code>
    * is 2 PI then the section will become a whole cylinder.  Normals are
    * oriented to face outwards. The sector is made of quads.
    *
    * @param r radius of the cylinder
    * @param h height of the cylinder
    * @param ang total angular extent of the section
    * @param nh number of height slices
    * @param nang number of angular slices
    */
   public static PolygonalMesh createQuadCylindricalSection (
      double r, double h, double ang, int nh, int nang) {

      // set map tolerance to be 0.01 times smallest spacing between vertices
      double tol = Math.min(h/nh*1e-2, r*Math.sin(ang/nang)*1e-2);
      VertexMap vtxMap = new VertexMap(tol);
      PolygonalMesh mesh = new PolygonalMesh();
      addQuadCylindricalSection (
         mesh, r, h, ang, nh, nang, /*outward=*/true,
         RigidTransform3d.IDENTITY, vtxMap);
      return mesh;
   }

   /**
    * Adds a spherical section to an existing mesh. The section is centered on
    * the origin of a local coordinate frame L, with the poles located on the z
    * axis. A point on the sphere is defined using spherical coordinates r,
    * theta, and phi, with theta giving the azimuth angle with respect to the x
    * axis and phi giving the elevation angle with respect to the z axis.  The
    * x, y, z and values are hence
    * <pre>
    * r sin(phi) cos(theta), r sin(phi) sin(theta), r cos(phi)
    * </pre> The extent of phi is given by [0, maxphi],
    * while the extend of theta is given by [-maxthe, maxthe]. Specifying
    * maxphi and maxthe as both PI results in a complete sphere. Normals are
    * oriented to face outwards. The section is made of quads, except at the
    * poles. A transform XLM is supplied to map L into the mesh coordinate
    * frame M.
    *
    * <p>Note: to avoid degenarte quads near the south pole of the section,
    * <code>maxphi</code> is rounded to PI if it is within 1e-4 of PI.
    *
    * @param mesh existing mesh to add the section to
    * @param r radius of the section
    * @param maxthe maximum azimuth angle about either side of the x axis
    * @param maxphi maximum elevation angle with respect to the z axis
    * @param nthe number of slices for the azimuth
    * @param nphi number of slices for the elevation
    * @param XLM transform from the local coordinate frame L to mesh
    * coordinates M.
    * @param vtxMap spatial hash map storing all existing vertices in the
    * mesh. New vertices will be created as needed and added to both this
    * map and to the mesh.
    */
   public static void addQuadSphericalSection (
      PolygonalMesh mesh, double r, double maxthe, double maxphi,
      int nthe, int nphi, RigidTransform3d XLM, VertexMap vtxMap) {

      if (nthe <= 0) {
         throw new IllegalArgumentException ("nthe must be positive");
      }
      if (nphi <= 0) {
         throw new IllegalArgumentException ("nphi must be positive");
      }
      if (Math.abs(maxphi-Math.PI) < 0.0001) {
         maxphi = Math.PI;
      }
      if (Math.abs(maxthe-Math.PI) < 0.0001) {
         maxthe = Math.PI;
      }

      // create faces starting from the pole down
      double dthe = 2*maxthe/nthe;
      double dphi = maxphi/nphi;
      double cp0 = 1;
      double sp0 = 0;
      
      for (int i=0; i<nphi; i++) {
         
         double p = (i+1)*dphi;
         double cp1 = Math.cos (p);
         double sp1 = Math.sin (p);
         // snap to pi
         if (Math.abs(p-Math.PI) < 0.0001) {
            p = Math.PI;
            cp1 = -1;
            sp1 = 0;
         }
         
         double ct0 = Math.cos (-maxthe);
         double st0 = Math.sin (-maxthe);
         if (Math.abs(maxthe-Math.PI) < 0.0001) {
            ct0 = -1;
            st0 = 0;
         }
         
         for (int j=0; j<nthe; j++) {
            Vertex3d v0, v1, v2, v3;
            double t = -maxthe + (j+1)*dthe;
            double ct1 = Math.cos (t);
            double st1 = Math.sin (t);
            // snap to pi
            if (Math.abs(t-Math.PI) < 0.0001) {
               t = Math.PI;
               ct1 = -1;
               st1 = 0;
            }
            
            v0 = vtxMap.getOrCreate (mesh, r*sp1*ct0, r*sp1*st0, r*cp1, XLM);
            v1 = vtxMap.getOrCreate (mesh, r*sp1*ct1, r*sp1*st1, r*cp1, XLM);
            v2 = vtxMap.getOrCreate (mesh, r*sp0*ct1, r*sp0*st1, r*cp0, XLM);
            v3 = vtxMap.getOrCreate (mesh, r*sp0*ct0, r*sp0*st0, r*cp0, XLM);

            if (v2 == v3) {
               mesh.addFace (v0, v1, v3);
            }
            else if (v0 == v1) {
               mesh.addFace(v0, v2, v3);
            }
            else {
               if (Math.abs(sp0) < 1e-5) {
                  if (cp0 > 0) {
                     mesh.addFace (v0, v1, v3);
                  }
                  else {
                     mesh.addFace (v0, v2, v3);
                  }
               }
               else if (Math.abs(sp1) < 1e-5){
                  if (cp1 > 0) {
                     mesh.addFace (v0, v1, v3);
                  }
                  else {
                     mesh.addFace (v0, v2, v3);
                  }
               }
               else {
                  mesh.addFace (v0, v1, v2, v3);
               }
            }
            ct0 = ct1;
            st0 = st1;
         }
         cp0 = cp1;
         sp0 = sp1;
      }
   }
 
   /**
    * Creates a spherical section. The section is centered on the origin of a
    * local coordinate frame L, with the poles located on the z axis. A point
    * on the sphere is defined using spherical coordinates r, theta, and phi,
    * with theta giving the azimuth angle with respect to the x axis and phi
    * giving the elevation angle with respect to the z axis. For additional
    * information, see the documentation for {@link #addQuadSphericalSection}.
    *
    * @param r radius of the section
    * @param maxthe maximum azimuth angle about either side of the x axis
    * @param maxphi maximum elevation angle with respect to the z axis
    * @param nthe number of slices for the azimuth
    * @param nphi number of slices for the elevation
    */
   public static PolygonalMesh createQuadSphericalSection (
      double r, double maxthe, double maxphi, int nthe, int nphi) {

      if (Math.abs(maxphi-Math.PI) < 0.0001) {
         maxphi = Math.PI;
      }
      double tol = computeSphericalPointTolerance (r, maxthe, maxphi, nthe, nphi);
      VertexMap vtxMap = new VertexMap (tol);
      PolygonalMesh mesh = new PolygonalMesh();
      addQuadSphericalSection (
         mesh, r, maxthe, maxphi, nthe, nphi, RigidTransform3d.IDENTITY, vtxMap);
      return mesh;
   }

   private static double computeSphericalPointTolerance (
      double r, double maxthe, double maxphi, int nthe, int nphi) {
      double minsp = Math.sin(maxphi/nphi);
      if (maxphi != Math.PI) {
         minsp = Math.min (minsp, Math.sin(maxphi));
      }
      return Math.min (0.01*r*minsp*Math.sin(maxthe/nthe), 0.01*r*minsp);
   }

   /**
    * Adds a toroidal section to an existing mesh. The section lies in the
    * x-y plane and curves about the z-axis for an angular distance of
    * <code>maxthe</code>, with the starting end aligned with the x-z plane.
    * The major radius from the z axis to the center of the tube is given by
    * <code>rmajor</code>, and the minor radius of the tube section itself is
    * given by <code>rminor</code>.
    *
    * <p>The section made out of quads. It is closed into a torus if
    * <code>maxthe</code> is equal to <code>2*Math.PI</code>.
    *
    * @param mesh existing mesh to add the section to
    * @param rmajor major radius of bending about the z axis
    * @param rminor minor radius of the tube
    * @param maxthe maximum bending angle about the z axis
    * @param nmajor number of slices along the major radius
    * @param nminor number of slices along the minor radius
    * @param XLM transform from the local coordinate frame L to mesh
    * coordinates M.
    * @param vtxMap spatial hash map storing all existing vertices in the
    * mesh. New vertices will be created as needed and added to both this
    * map and to the mesh.
    */
   public static void addQuadToroidalSection (
      PolygonalMesh mesh, double rmajor, double rminor, double maxthe,
      int nmajor, int nminor, RigidTransform3d XLM, VertexMap vtxMap) {

      double PI_TIMES_2 = 2*Math.PI;

      if (nmajor <= 0) {
         throw new IllegalArgumentException ("nmajor must be positive");
      }
      if (nminor < 3) {
         throw new IllegalArgumentException ("nminor must be at least 3");
      }
      if (rmajor <= rminor) {
         throw new IllegalArgumentException ("rmajor must be greater than rminor");
      }
      if (Math.abs(maxthe-PI_TIMES_2) < EPS) {
         maxthe = PI_TIMES_2;
      }

      double ct0 = 1.0;
      double st0 = 0.0;
      for (int i=1; i<=nmajor; i++) {
         double ct1 = Math.cos (i*maxthe/nmajor);
         double st1 = Math.sin (i*maxthe/nmajor);

         double cp0 = 1.0;
         double sp0 = 0.0;
         for (int j=1; j<=nminor; j++) {

            double cp1 = Math.cos (j*PI_TIMES_2/nminor);
            double sp1 = Math.sin (j*PI_TIMES_2/nminor);

            double r0 = rmajor+cp0*rminor;
            double r1 = rmajor+cp1*rminor;

            Vertex3d v0, v1, v2, v3;

            v0 = vtxMap.getOrCreate (mesh, r0*ct0, r0*st0, sp0*rminor, XLM);
            v1 = vtxMap.getOrCreate (mesh, r1*ct0, r1*st0, sp1*rminor, XLM);
            v2 = vtxMap.getOrCreate (mesh, r1*ct1, r1*st1, sp1*rminor, XLM);
            v3 = vtxMap.getOrCreate (mesh, r0*ct1, r0*st1, sp0*rminor, XLM);
            
            mesh.addFace (v3, v2, v1, v0);

            cp0 = cp1;
            sp0 = sp1;
         }
         ct0 = ct1;
         st0 = st1;
      }
   }

   /**
    * Creates a toroidal section. The section lies in the x-y plane and
    * curves about the z-axis for an angular distance of <code>maxthe</code>,
    * with the starting end aligned with the x-z plane. For additional
    * information, see the documentation for {@link #addQuadToroidalSection}.
    *
    * @param rmajor major radius of bending about the z axis
    * @param rminor minor radius of the tube
    * @param maxthe maximum bending angle about the z axis
    * @param nmajor number of slices along the major radius
    * @param nminor number of slices along the minor radius
    */
   public static PolygonalMesh createQuadToroidalSection (
      double rmajor, double rminor, double maxthe, int nmajor, int nminor) {

      double tol = computeToroidalTolerance (
         rmajor, rminor, maxthe, nmajor, nminor);

      VertexMap vtxMap = new VertexMap (tol);
      PolygonalMesh mesh = new PolygonalMesh();
      addQuadToroidalSection (
         mesh, rmajor, rminor, maxthe, nmajor, nminor,
         RigidTransform3d.IDENTITY, vtxMap);
      return mesh;
   }

   /**
    * Creates a quad torus mesh in the x-y plane.
    *
    * @param rmajor major radius of the torus
    * @param rminor minor radius of the torus
    * @param nmajor number of slices along the major radius
    * @param nminor number of slices along the minor radius
    */
   public static PolygonalMesh createQuadTorus (
      double rmajor, double rminor, int nmajor, int nminor) {

      return createQuadToroidalSection (
         rmajor, rminor, 2*Math.PI, nmajor, nminor);
   }

   /**
    * Creates a triangular torus mesh in the x-y plane.
    *
    * @param rmajor major radius of the torus
    * @param rminor minor radius of the torus
    * @param nmajor number of slices along the major radius
    * @param nminor number of slices along the minor radius
    */
   public static PolygonalMesh createTorus (
      double rmajor, double rminor, int nmajor, int nminor) {

      PolygonalMesh mesh = createQuadTorus (
         rmajor, rminor, nmajor, nminor);
      mesh.triangulate();
      return mesh;
   }

   /**
    * Creates a quad extended torus mesh in the x-y plane.  An extended torus
    * is a torus with straight sections added in the middle, giving a
    * shape similar to a chain link. The straight sections are parallel
    * to the x-axis.
    *
    * @param rmajor major radius of the torus
    * @param rminor minor radius of the torus
    * @param xlen length of the straight sections
    * @param nmajor number of slices along the major radius
    * @param nminor number of slices along the minor radius
    */
   public static PolygonalMesh createQuadExtendedTorus (
      double rmajor, double rminor, double xlen, int nmajor, int nminor) {

      double tol = computeToroidalTolerance (
         rmajor, rminor, 2*Math.PI, nmajor, nminor);
      VertexMap vtxMap = new VertexMap (tol);
      PolygonalMesh mesh = new PolygonalMesh();
      RigidTransform3d XLM = new RigidTransform3d();
      XLM.p.set (xlen/2, 0, 0);
      XLM.R.setRpy (-Math.PI/2, 0, 0);
      addQuadToroidalSection (
         mesh, rmajor, rminor, Math.PI, nmajor, nminor, XLM, vtxMap);
      XLM.p.set (-xlen/2, 0, 0);
      XLM.R.setRpy (Math.PI/2, 0, 0);
      addQuadToroidalSection (
         mesh, rmajor, rminor, Math.PI, nmajor, nminor, XLM, vtxMap);

      double outerChordLen = 2*(rmajor+rminor)*Math.sin(Math.PI/(nmajor));
      int nx = (int)Math.ceil(xlen/outerChordLen);

      XLM.p.set (0, rmajor, 0);
      XLM.R.setRpy (0, Math.PI/2, 0);
      addQuadCylindricalSection (
         mesh, rminor, xlen, 2*Math.PI, nx, nminor,
         /*outward=*/true, XLM, vtxMap);

      XLM.p.set (0, -rmajor, 0);
      XLM.R.setRpy (0, Math.PI/2, 0);
      addQuadCylindricalSection (
         mesh, rminor, xlen, 2*Math.PI, nx, nminor,
         /*outward=*/true, XLM, vtxMap);

      return mesh;
   }

   /**
    * Creates a quad extended torus mesh in the x-y plane.  An extended torus
    * is a torus with straight sections added in the middle, giving a
    * shape similar to a chain link. The straight sections are parallel
    * to the x-axis.
    *
    * @param rmajor major radius of the torus
    * @param rminor minor radius of the torus
    * @param xlen length of the straight sections
    * @param nmajor number of slices along the major radius
    * @param nminor number of slices along the minor radius
    */
   public static PolygonalMesh createExtendedTorus (
      double rmajor, double rminor, double xlen, int nmajor, int nminor) {

      PolygonalMesh mesh = createQuadExtendedTorus (
         rmajor, rminor, xlen, nmajor, nminor);
      mesh.triangulate();
      return mesh;
   }

   private static double computeToroidalTolerance (
      double rmajor, double rminor, double maxthe, int nmajor, int nminor) {

      if (rmajor <= rminor) {
         throw new IllegalArgumentException (
            "rmajor must be greater than rminor");
      }
      return (
         0.01*Math.min (2*(rmajor-rminor)*Math.sin(maxthe/(2*nmajor)),
                        2*rminor*Math.sin(Math.PI/nminor)));
   }

   /**
    * Legacy method to create a rectangle with texture coordinates;
    * calls {@link #createRectangle(double,double,boolean)
    * createRectangle(wx,wy,true)}.
    * 
    * @param wx width in the x direction
    * @param wy width in the y direction
    * @return created rectangular mesh
    */
   public static PolygonalMesh createPlane (double wx, double wy) {
      return createRectangle (wx, wy, /*addTextureCoords=*/true);
   }
   
   /**
    * Create a open rectanglar mesh, composed of two triangles, in the x-y
    * plane, centered on the origin and with normals directed along the z axis. 
    * Texture coordinates can optionally be created for each triangle so that
    * (0,0) and (1,1) correspond to the lower left and upper right corners.
    *
    * @param wx width in the x direction
    * @param wy width in the y direction
    * @param addTextureCoords if <code>true</code>, generates texture 
    * coordinates
    */
   public static PolygonalMesh createRectangle (
      double wx, double wy, boolean addTextureCoords) {
      Point3d[] plist = new Point3d[4];
      plist[0] = new Point3d(wx / 2, wy / 2, 0);
      plist[1] = new Point3d(-wx / 2, wy / 2, 0);
      plist[2] = new Point3d(-wx / 2, -wy / 2, 0);
      plist[3] = new Point3d(wx / 2, -wy / 2, 0);
      int[][] faceIndices = new int[][] { { 0, 1, 2 }, { 0, 2, 3 } };

      PolygonalMesh mesh = new PolygonalMesh();
      mesh.set(plist, faceIndices);

      if (addTextureCoords) {
         ArrayList<Vector3d> vt = new ArrayList<Vector3d>();
         vt.add(new Point3d(1, 1, 0));
         vt.add(new Point3d(0, 1, 0));
         vt.add(new Point3d(0, 0, 0));
         vt.add(new Point3d(1, 0, 0));         
         mesh.setTextureCoords (vt, mesh.createVertexIndices());
      }
      return mesh;
   }

   /**
    * Legacy method to create a rectangle with texture coordinates;
    * calls {@link #createRectangle(double,double,int,int,boolean)
    * createRectangle(wx,wy,xdiv,ydiv,true)}.
    * 
    * @param wx width in the x direction
    * @param wy width in the y direction
    * @param xdiv number of divisions in x (&gt;=1)
    * @param ydiv number of divisions in y (&gt;=1)
    * @return created rectangular mesh
    */
   public static PolygonalMesh createPlane (
      double wx, double wy, int xdiv, int ydiv) {
      return createRectangle (
         wx, wy, xdiv, ydiv, DEFAULT_ADD_NORMALS, /*addTextureCoords=*/true);
   }
   
   /**
    * Create a open rectangular mesh, composed of triangles, in the x-y
    * plane, centered on the origin and with normals directed along the z axis. 
    * Texture coordinates can optionally be created created for each triangle
    * so that (0,0) and (1,1) correspond to the lower left and upper right 
    * corners.
    *
    * @param wx width in the x direction
    * @param wy width in the y direction
    * @param xdiv number of divisions in x (&gt;=1)
    * @param ydiv number of divisions in y (&gt;=1)
    * @param addTextureCoords if <code>true</code>, generates texture 
    * coordinates
    * @return created mesh
    */
   public static PolygonalMesh createRectangle (
      double wx, double wy, int xdiv, int ydiv,
      boolean addTextureCoords) {
      return createRectangle (
         wx, wy, xdiv, ydiv, DEFAULT_ADD_NORMALS, addTextureCoords);
   }
   
   /**
    * Create a open rectangular mesh, composed of triangles, in the x-y
    * plane, centered on the origin and with normals directed along the z axis. 
    * Texture coordinates can optionally be created created for each triangle
    * so that (0,0) and (1,1) correspond to the lower left and upper right 
    * corners.
    *
    * @param wx width in the x direction
    * @param wy width in the y direction
    * @param xdiv number of divisions in x (&gt;=1)
    * @param ydiv number of divisions in y (&gt;=1)
    * @param addNormals if <code>true</code>, generates normals in
    * the positive z direction
    * @param addTextureCoords if <code>true</code>, generates texture 
    * coordinates
    * @return created mesh
    */
   public static PolygonalMesh createRectangle (
      double wx, double wy, int xdiv, int ydiv,
      boolean addNormals, boolean addTextureCoords) {
      
      Point3d[] plist = new Point3d[(xdiv+1)*(ydiv+1)];
      int[][] faceIndices = new int[xdiv*ydiv*2][];
      ArrayList<Vector3d> vt = new ArrayList<Vector3d>();
      
      double xoffset = -wx/2;
      double yoffset = -wy/2;
      double dx = wx/xdiv;
      double dy = wy/ydiv;
      double dxt = 1.0/xdiv;
      double dyt = 1.0/ydiv;
      
      for (int j=0; j<=ydiv; j++) {
         for (int i=0; i<=xdiv; i++) {
            plist[i+j*(xdiv+1)] = new Point3d(xoffset+i*dx, yoffset + j*dy, 0);
            if (addTextureCoords) {
               vt.add(new Point3d(i*dxt,j*dyt, 0));
            }
              
            if (i < xdiv && j < ydiv) {
               int idx1 = i+j*(xdiv+1);
               int idx2 = (i+1)+j*(xdiv+1);
               int idx3 = (i+1)+(j+1)*(xdiv+1);
               int idx4 = i + (j+1)*(xdiv+1);
               int istart = 2*(i+j*xdiv);
               if ( (i+j) % 2 == 0) {
                  faceIndices[istart] = new int[] {idx1, idx2, idx4};
                  faceIndices[istart+1] = new int[] {idx2, idx3, idx4};
               } else {
                  faceIndices[istart] = new int[] {idx1, idx2, idx3};
                  faceIndices[istart+1] = new int[] {idx1, idx3, idx4};
               }
            }
         }
      }
      
      PolygonalMesh mesh = new PolygonalMesh();
      mesh.set(plist, faceIndices);
      
      if (addTextureCoords) {
         mesh.setTextureCoords (vt, mesh.createVertexIndices());
      }
      
      if (addNormals) {
         ArrayList<Vector3d> normals = new ArrayList<>();
         normals.add(new Vector3d(0,0,1));
         int[] indices = mesh.createVertexIndices();
         for (int i=0; i<indices.length; ++i) {
            indices[i] = 0;
         }
         mesh.setNormals(normals, indices);
      }
      return mesh;
   }

   /**
    * Creates a quad-based box mesh, with one quad per side, centered on the
    * origin.
    *
    * @param wx width in the x direction
    * @param wy width in the y direction
    * @param wz width in the z direction
    */
   public static PolygonalMesh createQuadBox (
      double wx, double wy, double wz) {
      
      return createBox (
         wx, wy, wz, Point3d.ZERO, 1, 1, 1, DEFAULT_ADD_NORMALS, FaceType.QUAD);
   }
   
   /**
    * Creates a quad-based box mesh, with one quad per side, centered on the
    * origin.
    *
    * @param wx width in the x direction
    * @param wy width in the y direction
    * @param wz width in the z direction
    * @param addNormals if <code>true</code>, generates normals perpendicular
    * to each side
    */
   public static PolygonalMesh createQuadBox (
      double wx, double wy, double wz, boolean addNormals) {
      
      return createBox (
         wx, wy, wz, Point3d.ZERO, 1, 1, 1, addNormals, FaceType.QUAD);
   }

   /**
    * Creates a quad-based box mesh, with one quad per side, centered at
    * a prescribed location.
    *
    * @param wx width in the x direction
    * @param wy width in the y direction
    * @param wz width in the z direction
    * @param x center location along the x axis
    * @param y center location along the y axis
    * @param z center location along the z axis
    */
   public static PolygonalMesh createQuadBox (
      double wx, double wy, double wz, double x, double y, double z) {
      return createQuadBox(wx, wy, wz, x, y, z, DEFAULT_ADD_NORMALS);
   }
   
   /**
    * Creates a quad-based box mesh, with one quad per side, centered at
    * a prescribed location.
    *
    * @param wx width in the x direction
    * @param wy width in the y direction
    * @param wz width in the z direction
    * @param x center location along the x axis
    * @param y center location along the y axis
    * @param z center location along the z axis
    * @param addNormals if <code>true</code>, generates normals perpendicular
    * to each side
    */
   public static PolygonalMesh createQuadBox (
      double wx, double wy, double wz,
      double x, double y, double z, boolean addNormals) {

      return createBox (
         wx, wy, wz, new Point3d (x, y, z), 1, 1, 1, addNormals, FaceType.QUAD);
   }
   
   /**
    * Creates a triangle-based box mesh, with two triangles per side, centered
    * on the origin.
    *
    * @param wx width in the x direction
    * @param wy width in the y direction
    * @param wz width in the z direction
    */
   public static PolygonalMesh createBox (
      double wx, double wy, double wz) {
      return createBox(wx, wy, wz, DEFAULT_ADD_NORMALS);
   }

   /**
    * Creates a triangle-based box mesh, with two triangles per side, centered
    * on the origin.
    *
    * @param wx width in the x direction
    * @param wy width in the y direction
    * @param wz width in the z direction
    * @param addNormals if <code>true</code>, generates normals perpendicular
    * to each side
    */
   public static PolygonalMesh createBox (
      double wx, double wy, double wz, boolean addNormals) {
      PolygonalMesh mesh = createBox (
         wx, wy, wz, Point3d.ZERO, 1, 1, 1, addNormals, FaceType.TRI);
      return mesh;
   }

   /**
    * Creates a triangle-based box mesh, with two triangles per side, centered
    * at a prescribed location.
    *
    * @param wx width in the x direction
    * @param wy width in the y direction
    * @param wz width in the z direction
    * @param x center location along the x axis
    * @param y center location along the y axis
    * @param z center location along the z axis
    */
   public static PolygonalMesh createBox (
      double wx, double wy, double wz, double x, double y, double z) {
      return createBox(wx, wy, wz, x, y, z, DEFAULT_ADD_NORMALS);
   }
   
   /**
    * Creates a triangle-based box mesh, with two triangles per side, centered
    * at a prescribed location.
    *
    * @param wx width in the x direction
    * @param wy width in the y direction
    * @param wz width in the z direction
    * @param x center location along the x axis
    * @param y center location along the y axis
    * @param z center location along the z axis
    * @param addNormals if <code>true</code>, generates normals perpendicular
    * to each side
    */
   public static PolygonalMesh createBox (
      double wx, double wy, double wz, double x, double y, double z,
      boolean addNormals) {

      PolygonalMesh mesh = createBox (
         wx, wy, wz, new Point3d(x, y, z), 1, 1, 1, addNormals, FaceType.TRI);
      return mesh;
   }
      
   /**
    * Creates a triangle-based box mesh, with a specified mesh resolution in
    * each direction, and centered at a defined center point.
    *
    * @param wx width in the x direction
    * @param wy width in the y direction
    * @param wz width in the z direction
    * @param center center of the box
    * @param nx number of subdivisions along x
    * @param ny number of subdivisions along y
    * @param nz number of subdivisions along z
    */
   public static PolygonalMesh createBox(double wx, double wy, double wz,
      Point3d center, int nx, int ny, int nz) {
      return createBox(wx,  wy,  wz, center, nx, ny, nz, DEFAULT_ADD_NORMALS);
   }
   

   /**
    * Creates a triangle-based box mesh, with a specified mesh resolution in
    * each direction, and centered at a defined center point.
    *
    * @param wx width in the x direction
    * @param wy width in the y direction
    * @param wz width in the z direction
    * @param center center of the box
    * @param nx number of subdivisions along x
    * @param ny number of subdivisions along y
    * @param nz number of subdivisions along z
    * @param addNormals if <code>true</code>, generates normals perpendicular
    * to each side
    */
   public static PolygonalMesh createBox (
      double wx, double wy, double wz,
      Point3d center, int nx, int ny, int nz, boolean addNormals) {
      PolygonalMesh mesh =
         createBox (
            wx, wy, wz, center, nx, ny, nz, addNormals, FaceType.TRI);
      return mesh;
   }


   //   private static class MidEdgeVertexList extends ArrayList<MidEdgeVertex> {
   //
   //      private static final long serialVersionUID = -1887188834611352010L;
   //
   //      public MidEdgeVertex get(Vertex3d a, Vertex3d b) {
   //         for (MidEdgeVertex vtx : this) {
   //            if (vtx.a == a && vtx.b == b) {
   //               return vtx;
   //            } else if (vtx.a == b && vtx.b == a) {
   //               return vtx;
   //            }
   //         }
   //         return null;
   //      }
   //
   //      public MidEdgeVertex getOrCreate(Vertex3d a, Vertex3d b) {
   //         MidEdgeVertex vtx = get(a, b);
   //         if (vtx == null) {
   //            vtx = new MidEdgeVertex(a, b);
   //            add(vtx);
   //         }
   //         return vtx;
   //      }
   //   }

   //   private static class MidEdgeVertex {
   //      public Vertex3d a = null;
   //      public Vertex3d b = null;
   //      public Vertex3d mid = null;
   //
   //      public MidEdgeVertex (Vertex3d a, Vertex3d b) {
   //         this.a = a;
   //         this.b = b;
   //         Point3d pnt = new Point3d(a.getPosition());
   //         pnt.add(b.getPosition());
   //         pnt.scale(0.5);
   //
   //         mid = new Vertex3d(pnt);
   //      }
   //   }

   public static PolygonalMesh subdivide(PolygonalMesh orig, int numIters) {
      PolygonalMesh out = orig;
      for (int i = 0; i < numIters; i++) {
         out = subdivide(out);
      }
      return out;
   }
   
   /**
    * Wrap all features of a mesh with a polygonal mesh
    * 
    * @param mesh mesh to wrap
    * @param res resolution for evaluating distances
    * @param dist level-set of distances to consider
    * @return wrapped mesh
    */
   public static PolygonalMesh wrapMesh(MeshBase mesh, Vector3i res, double margin, double dist) {
      
      ArrayList<Feature> features = new ArrayList<>();
      
      features.addAll (mesh.getVertices ());
      
      if (mesh instanceof PolylineMesh) {
         PolylineMesh lmesh = (PolylineMesh)mesh;
         for (Polyline line : lmesh.getLines ()) {
            features.addAll (Arrays.asList(line.getSegments ()));
         }
      }
      
      DistanceGrid dg = new DistanceGrid (features, margin, res, false);
      double[] phi = dg.getVertexDistances ();
      
      if (mesh instanceof PolygonalMesh) {
         PolygonalMesh pmesh = (PolygonalMesh)mesh;
         DistanceGrid sdg = new DistanceGrid (pmesh.getFaces (), margin, res, true);
         double[] phi2 = sdg.getVertexDistances ();
         
         // take lower of distances
         for (int i=0; i<phi2.length; ++i) {
            if (phi2[i] < phi[i]) {
               phi[i] = phi2[i];
            }
         }
      }
      
      
      MarchingTetrahedra marcher = new MarchingTetrahedra ();
      PolygonalMesh out = marcher.createMesh (
         phi, Vector3d.ZERO, new Vector3d(1,1,1), res, dist);
      out.transform (dg.getGridToLocalTransformer ());
      
      out.setMeshToWorld (mesh.getMeshToWorld ());
      
      return out;
   }

   public static PolygonalMesh subdivide(PolygonalMesh orig) {

      PolygonalMesh mesh = new PolygonalMesh();
      HashMap<Vertex3d,Vertex3d> newVtxMap = new HashMap<Vertex3d,Vertex3d>();

      for (Vertex3d vtx : orig.getVertices()) {
         Vertex3d newVtx = new Vertex3d(new Point3d(vtx.getPosition()));
         newVtxMap.put(vtx, newVtx);
         mesh.addVertex(newVtx);
      }

      HashMap<HalfEdge,Vertex3d> vtxList = new HashMap<>();

      Vertex3d vtxo1, vtxo2, vtxo3, vtxo4;
      Vertex3d vtxc1, vtxc2, vtxc3, vtxc4, vtxmid;
      //Point3d centroid = new Point3d();
      
      /// create mid-edge nodes
      for (Face face : orig.getFaces ()) {
         HalfEdge he0 = face.firstHalfEdge ();
         HalfEdge he = he0;
         do {
            if (he .isPrimary ()) {                  
               Point3d npos = new Point3d();
               npos.interpolate (he.getHead ().getPosition (), 0.5, he.getTail ().getPosition ());
               Vertex3d vtx = mesh.addVertex (npos);
               vtxList.put (he, vtx);
            }
            he = he.getNext ();
         } while (he != he0);
      }

      // add new faces
      for (Face face : orig.getFaces()) {
         if (face.numVertices() == 3) {

            HalfEdge he = face.firstHalfEdge ();
            HalfEdge hep = (he.isPrimary () ? he : he.opposite);
            vtxo1 = newVtxMap.get(he.head);
            vtxc3 = vtxList.get (hep);
            he = he.next;
            hep = (he.isPrimary () ? he : he.opposite);
            vtxo2 = newVtxMap.get(he.head);
            vtxc1 = vtxList.get (hep);
            he = he.next;
            hep = (he.isPrimary () ? he : he.opposite);
            vtxo3 = newVtxMap.get(he.head);
            vtxc2 = vtxList.get(hep);
            
            mesh.addFace(vtxo1, vtxc1, vtxc3);
            mesh.addFace(vtxc1, vtxo2, vtxc2);
            mesh.addFace(vtxc3, vtxc2, vtxo3);
            mesh.addFace(vtxc1, vtxc2, vtxc3);

         } else if (face.numVertices() == 4) {

            HalfEdge he = face.firstHalfEdge ();
            HalfEdge hep = (he.isPrimary () ? he : he.opposite);
            vtxo1 = newVtxMap.get(he.head);
            vtxc4 = vtxList.get (hep);
            he = he.next;
            hep = (he.isPrimary () ? he : he.opposite);
            vtxo2 = newVtxMap.get(he.head);
            vtxc1 = vtxList.get (hep);
            he = he.next;
            hep = (he.isPrimary () ? he : he.opposite);
            vtxo3 = newVtxMap.get(he.head);
            vtxc2 = vtxList.get(hep);
            he = he.next;
            hep = (he.isPrimary () ? he : he.opposite);
            vtxo4 = newVtxMap.get(he.head);
            vtxc3 = vtxList.get(hep);
            
            Point3d centroid = new Point3d();
            face.computeCentroid(centroid);
            vtxmid = new Vertex3d(centroid);
            mesh.addVertex(vtxmid);

            mesh.addFace(vtxo1, vtxc1, vtxmid, vtxc4);
            mesh.addFace(vtxc1, vtxo2, vtxc2, vtxmid);
            mesh.addFace(vtxmid, vtxc2, vtxo3, vtxc3);
            mesh.addFace(vtxc4, vtxmid, vtxc3, vtxo4);

         } else {
            // triangulate face using centroid and divide edges
            Point3d centroid = new Point3d();
            face.computeCentroid(centroid);
            vtxmid = new Vertex3d(centroid);
            mesh.addVertex(vtxmid);

            HalfEdge he0 = face.firstHalfEdge ();
            HalfEdge he = he0;
            do {
               vtxo1 = newVtxMap.get (he.tail);
               vtxo2 = newVtxMap.get (he.head);
               HalfEdge hep = (he.isPrimary () ? he : he.opposite);
               vtxc1 = vtxList.get (hep);
               
               mesh.addFace(vtxo1, vtxc1, vtxmid);
               mesh.addFace(vtxc1, vtxo2, vtxmid);
               
               he = he.next;
            } while (he != he0);
            
         }
      }

      return mesh;

   }

   /**
    * Creates a quad-based box mesh, with a specified mesh resolution in
    * each direction, and centered at a defined center point.
    *
    * @param wx width in the x direction
    * @param wy width in the y direction
    * @param wz width in the z direction
    * @param center center of the box
    * @param nx number of subdivisions along x
    * @param ny number of subdivisions along y
    * @param nz number of subdivisions along z
    */
   public static PolygonalMesh createQuadBox (
      double wx, double wy, double wz,
      Point3d center, int nx, int ny, int nz) {
      return createBox (
         wx, wy, wz, center, nx, ny, nz, DEFAULT_ADD_NORMALS, FaceType.QUAD);
   }

   private static void setHardEdges (Face face, boolean[] hard) {
      for (int i=0; i<hard.length; i++) {
         if (hard[i]) {
            face.getEdge(i).setHard(true);
         }
      }
   }

   private static void addFaces (
      PolygonalMesh mesh, Vertex3d vtxs[], boolean[] hardEdges,
      int nrmIdx, int[] nrmlIdxs, int sum, FaceType faceType) {

      int nverts; // number of faces * vertices per face
      int nrmlOff; // offset within normalIdxs for adding normals

      if (faceType == FaceType.QUAD) {
         nverts = 4;
         nrmlOff = mesh.numFaces()*4;
         Face f = mesh.addFace(vtxs);
         setHardEdges (f, hardEdges);
      }
      else {
         Face f0, f1;
         nverts = 6;
         nrmlOff = mesh.numFaces()*3;
         if (faceType == FaceType.TRI || (sum%2) == 0) {
            // even parity
            f0 = mesh.addFace (vtxs[0], vtxs[1], vtxs[2]);
            f1 = mesh.addFace (vtxs[0], vtxs[2], vtxs[3]);

            setHardEdges (f0, new boolean[] {false, hardEdges[1], hardEdges[2]});
            setHardEdges (f1, new boolean[] {hardEdges[0], false, hardEdges[3]});
         }
         else {
            // odd parity
            f0 = mesh.addFace (vtxs[0], vtxs[1], vtxs[3]);
            f1 = mesh.addFace (vtxs[1], vtxs[2], vtxs[3]);

            setHardEdges (f0, new boolean[] {hardEdges[0], hardEdges[1], false});
            setHardEdges (f1, new boolean[] {false, hardEdges[2], hardEdges[3]});
         }
      }
      if (nrmlIdxs != null) {
         for (int l=0; l<nverts; ++l) {
            nrmlIdxs[nrmlOff++] = nrmIdx;
         }
      }
   }
   
   /**
    * Creates a box mesh, with a specified mesh resolution in each direction, 
    * and centered at a defined center point. The faces type is specified
    * by <code>faceType</code>
    *
    * @param wx width in the x direction
    * @param wy width in the y direction
    * @param wz width in the z direction
    * @param center center of the box
    * @param nx number of subdivisions along x
    * @param ny number of subdivisions along y
    * @param nz number of subdivisions along z
    * @param addNormals if <code>true</code>, generates normals perpendicular
    * to each side
    * @param faceType specifies the face type to be either quads, triangles,
    * or triangles with alternating diagonals
    */
   public static PolygonalMesh createBox (
      double wx, double wy, double wz,
      Point3d center, int nx, int ny, int nz,
      boolean addNormals, FaceType faceType) {

      PolygonalMesh mesh = new PolygonalMesh();
      Vertex3d[][][] vtxs = new Vertex3d[nx + 1][ny + 1][nz + 1];

      Vertex3d faceVtxs[] = new Vertex3d[4];
      
      ArrayList<Vector3d> nrmls = null;
      int[] nrmlIdxs = null;
      if (addNormals) {
         nrmls = new ArrayList<Vector3d>();
         nrmls.add(new Vector3d(1,0,0));
         nrmls.add(new Vector3d(0,1,0));
         nrmls.add(new Vector3d(0,0,1));
         nrmls.add(new Vector3d(-1,0,0));
         nrmls.add(new Vector3d(0,-1,0));
         nrmls.add(new Vector3d(0,0,-1));
         int nindices;
         if (faceType == FaceType.QUAD) {
            nindices = 8*(nx*ny+nx*nz+ny*nz);
         }
         else {
            // trianglar
            nindices = 12*(nx*ny+nx*nz+ny*nz);
         }
         nrmlIdxs = new int[nindices];
      }

      Vector3d dx = new Vector3d(wx / (nx), wy / (ny), wz / (nz));
      Point3d offset = new Point3d(-wx / 2, -wy / 2, -wz / 2);
      boolean[] hardEdges;

      // bottom/top (sides in x/y plane)
      for (int i = 0; i < nx; i++) {
         for (int j = 0; j < ny; j++) {
            faceVtxs[0] = getOrCreateVertex(i, j, 0, vtxs, offset, dx, mesh);
            faceVtxs[1] =
               getOrCreateVertex(i, j+1, 0, vtxs, offset, dx, mesh);
            faceVtxs[2] =
               getOrCreateVertex(i+1, j+1, 0, vtxs, offset, dx, mesh);
            faceVtxs[3] =
               getOrCreateVertex(i+1, j, 0, vtxs, offset, dx, mesh);

            // notes: edge(i) appears *before* vertex(i).
            hardEdges = new boolean[] {j == 0, i == 0, j == ny-1, i == nx-1 };
            addFaces (mesh, faceVtxs, hardEdges, 5, nrmlIdxs, i+j, faceType);

            faceVtxs[0] = getOrCreateVertex(i, j, nz, vtxs, offset, dx, mesh);
            faceVtxs[3] =
               getOrCreateVertex(i, j+1, nz, vtxs, offset, dx, mesh);
            faceVtxs[2] =
               getOrCreateVertex(i+1, j+1, nz, vtxs, offset, dx, mesh);
            faceVtxs[1] =
               getOrCreateVertex(i+1, j, nz, vtxs, offset, dx, mesh);

            hardEdges = new boolean[] {i == 0, j == 0, i == nx-1, j == ny-1 };
            addFaces (mesh, faceVtxs, hardEdges, 2, nrmlIdxs, i+j, faceType);
         }
      }

      // back/front (sides in z/x plane)
      for (int i = 0; i < nx; i++) {
         for (int k = 0; k < nz; k++) {
            faceVtxs[0] = getOrCreateVertex(i, 0, k, vtxs, offset, dx, mesh);
            faceVtxs[3] =
               getOrCreateVertex(i, 0, k+1, vtxs, offset, dx, mesh);
            faceVtxs[2] =
               getOrCreateVertex(i+1, 0, k+1, vtxs, offset, dx, mesh);
            faceVtxs[1] =
               getOrCreateVertex(i+1, 0, k, vtxs, offset, dx, mesh);

            hardEdges = new boolean[] {i == 0, k == 0, i == nx-1, k == nz-1 };
            addFaces (mesh, faceVtxs, hardEdges, 4, nrmlIdxs, i+k, faceType);
            
            faceVtxs[0] = getOrCreateVertex(i, ny, k, vtxs, offset, dx, mesh);
            faceVtxs[1] =
               getOrCreateVertex(i, ny, k+1, vtxs, offset, dx, mesh);
            faceVtxs[2] =
               getOrCreateVertex(i+1, ny, k+1, vtxs, offset, dx, mesh);
            faceVtxs[3] =
               getOrCreateVertex(i+1, ny, k, vtxs, offset, dx, mesh);

            hardEdges = new boolean[] {k == 0, i == 0, k == nz-1, i == nx-1 };
            addFaces (mesh, faceVtxs, hardEdges, 1, nrmlIdxs, i+k, faceType);
         }
      }

      // left/right (sides in y/z plane)
      for (int j = 0; j < ny; j++) {
         for (int k = 0; k < nz; k++) {
            faceVtxs[0] = getOrCreateVertex(0, j, k, vtxs, offset, dx, mesh);
            faceVtxs[3] =
               getOrCreateVertex(0, j+1, k, vtxs, offset, dx, mesh);
            faceVtxs[2] =
               getOrCreateVertex(0, j+1, k+1, vtxs, offset, dx, mesh);
            faceVtxs[1] =
               getOrCreateVertex(0, j, k+1, vtxs, offset, dx, mesh);

            hardEdges = new boolean[] {k == 0, j == 0, k == nz-1, j == ny-1 };
            addFaces (mesh, faceVtxs, hardEdges, 3, nrmlIdxs, j+k, faceType);

            faceVtxs[0] = getOrCreateVertex(nx, j, k, vtxs, offset, dx, mesh);
            faceVtxs[1] =
               getOrCreateVertex(nx, j+1, k, vtxs, offset, dx, mesh);
            faceVtxs[2] =
               getOrCreateVertex(nx, j+1, k+1, vtxs, offset, dx, mesh);
            faceVtxs[3] =
               getOrCreateVertex(nx, j, k+1, vtxs, offset, dx, mesh);

            hardEdges = new boolean[] {j == 0, k == 0, j == ny-1, k == nz-1 };
            addFaces (mesh, faceVtxs, hardEdges, 0, nrmlIdxs, j+k, faceType);
         }
      }
      
      if (addNormals) {
         mesh.setNormals(nrmls, nrmlIdxs);
      }
      
      if (center != null) {
         mesh.transform(new RigidTransform3d(center.x,center.y, center.z));
      }

      return mesh;
   }

   public static PolygonalMesh createQuadBoxNew (
      double wx, double wy, double wz,
      Point3d center, int nx, int ny, int nz) {

      double tol = Math.min (wx/nx, wy/ny);
      tol = Math.min (tol, wz/nz);
      VertexMap vtxMap = new VertexMap (tol);
      PolygonalMesh mesh = new PolygonalMesh();
      RigidTransform3d XLM = new RigidTransform3d ();
      XLM.p.set (center);
      XLM.p.z = center.z + wz/2;
      addQuadRectangle (mesh, wx, wy, nx, ny, XLM, vtxMap);
      XLM.p.z = center.z - wz/2;
      XLM.R.set (1, 0, 0,   0, -1, 0,  0, 0, -1);
      addQuadRectangle (mesh, wx, wy, nx, ny, XLM, vtxMap);

      XLM.p.z = 0;
      XLM.p.x = center.x + wx/2;
      XLM.R.set (0, 0, -1,   0, 1, 0,  1, 0, 0);
      addQuadRectangle (mesh, wz, wy, nz, ny, XLM, vtxMap);
      XLM.p.x = center.x - wx/2;
      XLM.R.set (0, 0, 1,   0, 1, 0,  -1, 0, 0);
      addQuadRectangle (mesh, wz, wy, nz, ny, XLM, vtxMap);

      XLM.p.x = 0;
      XLM.p.y = center.y + wy/2;
      XLM.R.set (1, 0, 0,   0, 0, -1,  0, 1, 0);
      addQuadRectangle (mesh, wz, wy, nz, ny, XLM, vtxMap);
      XLM.p.y = center.y - wy/2;
      XLM.R.set (1, 0, 0,   0, 0, 1,  0, -1, 0);
      addQuadRectangle (mesh, wz, wy, nz, ny, XLM, vtxMap);
      return mesh;
   }

   private static Vertex3d getOrCreateVertex(int i, int j, int k,
      Vertex3d[][][] vtxArray, Point3d offset, Vector3d dx, PolygonalMesh mesh) {

      if (vtxArray[i][j][k] != null) {
         return vtxArray[i][j][k];
      }

      double x = offset.x + i * dx.x;
      double y = offset.y + j * dx.y;
      double z = offset.z + k * dx.z;
      vtxArray[i][j][k] = new Vertex3d(x, y, z, -1);
      mesh.addVertex(vtxArray[i][j][k]);
      return vtxArray[i][j][k];
   }

   public static void triangulateFaceCentroid(PolygonalMesh mesh) {
      
      ArrayList<Face> oldFaces = new ArrayList<Face>(mesh.getFaces());
      
      for (Face face : oldFaces) {
         triangulateFaceCentroid(face);
      }
      
   }
   
   /**
    * Creates a triangular cylindrical mesh centered on the origin with
    * the main axis aligned with the z axis.
    *
    * @param r outer radius of the cylinder
    * @param h height of the cylinder
    * @param nslices number of segments about the z axis
    */
   public static PolygonalMesh createCylinder(double r, double h, int nslices) {
      return createCylinder (r, h, nslices, 1, 1);
   }

   /**
    * Creates a triangular cylindrical mesh centered on the origin with
    * the main axis aligned with the z axis.
    *
    * @param r outer radius of the cylinder
    * @param h height of the cylinder
    * @param nslices number of segments about the z axis
    * @param nr number of radial segments on each end cap
    * @param nh number of height segments along the z axis 
    */
   public static PolygonalMesh createCylinder (
      double r, double h, int nslices, int nr, int nh) {

      PolygonalMesh mesh = createQuadCylinder (r, h, nslices, nr, nh);
      mesh.triangulate();
      return mesh;
   }

   public static void triangulateFaceCentroid(Face face) {
      
      PolygonalMesh mesh = (PolygonalMesh)face.getMesh();
      Point3d centroid = new Point3d();
      face.computeCentroid(centroid);
      Vertex3d midVtx = mesh.addVertex(centroid);
      
      mesh.removeFace(face);
      
      Vertex3d vtxs[] = face.getVertices();
      for (int i=0; i<vtxs.length; i++) {
         int next = (i+1) % vtxs.length;
         mesh.addFace(vtxs[i], vtxs[next], midVtx);
      }
      
   }

//   // XXX should remove - now obsolete
//   public static PolygonalMesh createTriangularCylinder(
//      double r, double h, int nsides) {
//      PolygonalMesh mesh = createCylinder(r, h, nsides);
//      mesh.triangulate();
//      return mesh;
//   }

   /**
    * Creates an open cylindrical mesh centered on the origin with the
    * main axis aligned with the z axis. All faces are triangles.
    *
    * @param r outer radius of the cylinder
    * @param h height of the cylinder
    * @param nslices number of segments about the z axis
    * @param nh number of height segments along the z axis 
    */
   public static PolygonalMesh createOpenCylinder (
      double r, double h, int nslices, int nh) {

      PolygonalMesh mesh = createOpenQuadCylinder (r, h, nslices, nh);
      mesh.triangulate();
      return mesh;
   }

   /**
    * Creates an open quad cylindrical mesh centered on the origin with the
    * main axis aligned with the z axis. All faces are quads.
    *
    * @param r outer radius of the cylinder
    * @param h height of the cylinder
    * @param nslices number of segments about the z axis
    * @param nh number of height segments along the z axis 
    */
   public static PolygonalMesh createOpenQuadCylinder (
      double r, double h, int nslices, int nh) {

      if (nslices < 3) {
         throw new IllegalArgumentException(
            "argument nslices must be at least 3");
      }
      // set map tolerance to be 0.01 times smallest spacing between vertices
      double tol = Math.min(0.01*h/nh, 0.01*r*Math.sin(2*Math.PI/nslices));
      VertexMap vtxMap = new VertexMap (tol);
      PolygonalMesh mesh = new PolygonalMesh();
      RigidTransform3d XLM = new RigidTransform3d();
      addQuadCylindricalSection (
         mesh, r, h, 2*Math.PI, nh, nslices, /*outward=*/true, XLM, vtxMap);
      return mesh;
   }

   /**
    * Creates a quad cylindrical mesh centered on the origin with the main axis
    * aligned with the z axis. All faces are quads except for those at the
    * center of each end cap.
    *
    * @param r outer radius of the cylinder
    * @param h height of the cylinder
    * @param nslices number of segments about the z axis
    * @param nr number of radial segments on each end cap
    * @param nh number of height segments along the z axis 
    */
   public static PolygonalMesh createQuadCylinder (
      double r, double h, int nslices, int nr, int nh) {

      if (nslices < 3) {
         throw new IllegalArgumentException(
            "argument nslices must be at least 3");
      }
      // set map tolerance to be 0.01 times smallest spacing between vertices
      double tol = Math.min(0.01*h/nh, 0.01*r/nr*Math.sin(2*Math.PI/nslices));
      VertexMap vtxMap = new VertexMap (tol);
      PolygonalMesh mesh = new PolygonalMesh();
      RigidTransform3d XLM = new RigidTransform3d();
      addQuadCylindricalSection (
         mesh, r, h, 2*Math.PI, nh, nslices, /*outward=*/true, XLM, vtxMap);
      XLM.p.set (0, 0, h/2);
      addQuadAnnularSector (mesh, 0, r, 2*Math.PI, nr, nslices, XLM, vtxMap);
      XLM.p.set (0, 0, -h/2);
      XLM.R.mulRpy (0, 0, Math.PI); // flip 180 about x
      addQuadAnnularSector (mesh, 0, r, 2*Math.PI, nr, nslices, XLM, vtxMap);
      return mesh;
   }

   /**
    * Creates a quad tube mesh centered on the origin with the main axis
    * aligned with the z axis.
    *
    * @param r0 inner radius of the tube
    * @param r1 outer radius of the tube
    * @param h height of the tube
    * @param nslices number of segments about the z axis
    * @param nr number of radial segments on each end
    * @param nh number of height segments along the z axis 
    */
   public static PolygonalMesh createQuadTube (
      double r0, double r1, double h, int nslices, int nr, int nh) {

      // set map tolerance to be 0.01 times smallest spacing between vertices
      double tol = Math.min(0.01*h/nh, 0.01*r0*Math.sin(2*Math.PI/nslices));
      VertexMap vtxMap = new VertexMap (tol);
      PolygonalMesh mesh = new PolygonalMesh();
      RigidTransform3d XLM = new RigidTransform3d();
      addQuadCylindricalSection (
         mesh, r0, h, 2*Math.PI, nh, nslices, /*outward=*/false, XLM, vtxMap);
      addQuadCylindricalSection (
         mesh, r1, h, 2*Math.PI, nh, nslices, /*outward=*/true, XLM, vtxMap);
      XLM.p.set (0, 0, h/2);
      addQuadAnnularSector (mesh, r0, r1, 2*Math.PI, nr, nslices, XLM, vtxMap);
      XLM.p.set (0, 0, -h/2);
      XLM.R.mulRpy (0, 0, Math.PI); // flip 180 about x
      addQuadAnnularSector (mesh, r0, r1, 2*Math.PI, nr, nslices, XLM, vtxMap);
      return mesh;
   }

   /**
    * Creates a triangular tube mesh centered on the origin with the main axis
    * aligned with the z axis.
    *
    * @param r0 inner radius of the tube
    * @param r1 outer radius of the tube
    * @param h height of the tube
    * @param nslices number of segments about the z axis
    * @param nr number of radial segments on each end
    * @param nh number of height segments along the z axis 
    */
   public static PolygonalMesh createTube (
      double r0, double r1, double h, int nslices, int nr, int nh) {

      PolygonalMesh mesh = createQuadTube (r0, r1, h, nslices, nr, nh);
      mesh.triangulate();
      return mesh;
   }

   public static PolygonalMesh createPointedCylinder(
      double r, double h, double tiph, int nsides) {
      // start by creating a mesh.
      PolygonalMesh mesh = createCylinder(r, h, nsides);
      // and then adjust the top vertex XXXX
      double EPS = 1e-12;
      for (Vertex3d vtx : mesh.getVertices()) {
         Point3d p = vtx.pnt;
         if (Math.abs(p.x) < EPS && Math.abs(p.y) < EPS && p.z > 0) {
            vtx.pnt.set (0, 0, p.z+tiph);
            mesh.notifyVertexPositionsModified();
            break;
         }
      }
      return mesh;
   }
   
   public static void relax(PolygonalMesh mesh) {
      
      Point3d [] pnts = new Point3d[mesh.numVertices ()];
      
      for (int i=0; i<pnts.length; i++) {
         pnts[i] = new Point3d();
         
         Vertex3d vtx = mesh.getVertex (i);
         
         // get all neighbours
         int nbs = 0;
         HalfEdgeNode hen = vtx.getIncidentHedges();
         while (hen != null) {
            pnts[i].add (hen.he.tail.getPosition ());
            hen = hen.next;
            nbs++;
         }
         pnts[i].scale (1.0/nbs);
      }
      
      for (int i=0; i<pnts.length; i++) {
         mesh.getVertex (i).setPosition (pnts[i]);
      }
      
   }

   public static void projectToSphere(double r, Point3d c, PolygonalMesh mesh) {
      if (c == null) {
         c = new Point3d(0, 0, 0);
      }

      for (Vertex3d vtx : mesh.getVertices()) {
         Point3d pos = vtx.getPosition();
         double d = r / pos.distance(c);
         pos.interpolate(c, d, pos);
      }
   }

   public static PolygonalMesh createOctahedralSphere(double r, int divisions) {
      PolygonalMesh mesh = createOctahedron(r);
      for (int i = 0; i < divisions; i++) {
         mesh = subdivide(mesh);
         // XXX important to project each time!
         projectToSphere(r, null, mesh);  
      }
      return mesh;
   }

   public static PolygonalMesh createOctahedralSphere(double r, Point3d c,
      int divisions) {
      PolygonalMesh mesh = createOctahedralSphere(r, divisions);
      mesh.transform(new RigidTransform3d(c.x, c.y, c.z));
      return mesh;
   }

   public static PolygonalMesh createIcosahedralSphere(double r, int divisions) {
      PolygonalMesh mesh = createIcosahedron(r);
      for (int i = 0; i < divisions; i++) {
         mesh = subdivide(mesh);
         projectToSphere(r, null, mesh);  
      }
      return mesh;
   }
   
   public static PolygonalMesh createIcosahedralSphere(double r, Point3d c,
      int divisions) {
      PolygonalMesh mesh = createIcosahedralSphere (r, divisions);
      mesh.transform(new RigidTransform3d(c.x, c.y, c.z));
      return mesh;
   }

   public static PolygonalMesh createOctahedron(double r) {

      Point3d[] pnts = new Point3d[] {
         new Point3d(0, 0, -r),
         new Point3d(-r, 0, 0),
         new Point3d(0, -r, 0),
         new Point3d(r, 0, 0),
         new Point3d(0, r, 0),
         new Point3d(0, 0, r)
      };

      int[][] faceIndices = new int[][] {
         { 0, 1, 4 },
         { 0, 4, 3 },
         { 0, 3, 2 },
         { 0, 2, 1 },
         { 5, 4, 1 },
         { 5, 3, 4 },
         { 5, 2, 3 },
         { 5, 1, 2 }
      };

      PolygonalMesh mesh = new PolygonalMesh();
      mesh.set(pnts, faceIndices);

      return mesh;
   }

   public static PolygonalMesh createIcosahedron(double r) {

      double t = (1 + Math.sqrt(5)) / 2;
      double d = Math.sqrt(1 + t * t);
      double a = t / d;
      double b = 1 / d;

      Point3d[] pnts = new Point3d[] {
         new Point3d(a, b, 0),
         new Point3d(-a, b, 0),
         new Point3d(a, -b, 0),
         new Point3d(-a, -b, 0),
         new Point3d(b, 0, a),
         new Point3d(b, 0, -a),
         new Point3d(-b, 0, a),
         new Point3d(-b, 0, -a),
         new Point3d(0, a, b),
         new Point3d(0, -a, b),
         new Point3d(0, a, -b),
         new Point3d(0, -a, -b)
      };

      int[][] faceIndices = new int[][] {
         { 0, 8, 4 },
         { 0, 5, 10 },
         { 2, 4, 9 },
         { 2, 11, 5 },
         { 1, 6, 8 },
         { 1, 10, 7 },
         { 3, 9, 6 },
         { 3, 7, 11 },
         { 0, 10, 8 },
         { 1, 8, 10 },
         { 2, 9, 11 },
         { 3, 11, 9 },
         { 4, 2, 0 },
         { 5, 0, 2 },
         { 6, 1, 3 },
         { 7, 3, 1 },
         { 8, 6, 4 },
         { 9, 4, 6 },
         { 10, 5, 7 },
         { 11, 7, 5 }
      };

      PolygonalMesh mesh = new PolygonalMesh();
      mesh.set(pnts, faceIndices);

      return mesh;

   }

   /**
    * Creates a sphere out of approximately uniform triangles by separating the sphere into a
    * hexagonal prism with hexagonal pyramid caps, and dividing each edge into k segments.
    * 
    * @param r radius of sphere
    * @param k number of divisions
    * @return sphere mesh
    */
   public static PolygonalMesh createOctadecahedralSphere(double r, int k) {
      
      PolygonalMesh mesh = new PolygonalMesh();
      
      int ngs = 6;
      int ngt = 3;
      
      if (k < 1) {
         r = 1;
      }
      
      // distribute nodes radially
      double dr = r/k;
      
      // radial layers

      int nt = ngt*k;
      double rr = k*dr;
      double dphi = Math.PI/nt;
      
      Vertex3d[][] layer = new Vertex3d[nt+1][];

      // distribute nodes

      // north pole
      {
         layer[0] = new Vertex3d[2];
         Vertex3d node = new Vertex3d(0,0,rr);
         mesh.addVertex (node);
         layer[0][0] = node;
         layer[0][1] = node;
      }

      // first latitude group grows
      {
         int gt = 0;
         for (int i=1; i<k; ++i) {
            int t = gt*k+i;
            int ns = ngs*i;
            layer[t] = new Vertex3d[ns+1];

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
                  Vertex3d node = new Vertex3d(x,y,z);
                  layer[t][s] = node;
                  mesh.addVertex (node);
               }
            }
            layer[t][ns] = layer[t][0]; // wrap around
         }
      }

      // middle groups constant width
      for (int gt=1; gt<ngt-1; ++gt) {
         for (int i=0; i<k; ++i) {
            int t = gt*k+i;
            int ns = ngs*k;
            layer[t] = new Vertex3d[ns+1];

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
                  Vertex3d node = new Vertex3d(x,y,z);
                  layer[t][s] = node;
                  mesh.addVertex (node);
               }
            }
            layer[t][ns] = layer[t][0]; // wrap around
         }
      }

      // last group shrinks
      {
         int gt = ngt-1;
         for (int i=0; i<k; ++i) {
            int t = gt*k+i;
            int ns = ngs*(k-i);
            layer[t] = new Vertex3d[ns+1];

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
                  Vertex3d node = new Vertex3d(x,y,z);
                  layer[t][s] = node;
                  mesh.addVertex (node);
               }
            }
            layer[t][ns] = layer[t][0]; // wrap around
         }
      }

      // south pole
      {
         layer[nt] = new Vertex3d[2];
         Vertex3d node = new Vertex3d(0,0,-rr);
         mesh.addVertex (node);
         layer[nt][0] = node;
         layer[nt][1] = node;
      }

      // generate faces, move in groups

      // first latitude group
      {
         int gt = 0;
         for (int i=0; i<k; ++i) {
            int t = gt*k+i;

            // longitude groups
            for (int gs=0; gs<ngs; ++gs) {
               for (int j=0; j<i; ++j) {
                  int s = gs*i+j;
                  int sdown = s+gs;
                  
                  // triangle
                  mesh.addFace (layer[t][s], layer[t+1][sdown], layer[t+1][sdown+1]);

                  // flipped triangle
                  mesh.addFace (layer[t+1][sdown+1], layer[t][s+1], layer[t][s]);
               }

               // trailing triangle
               int s = gs*i + i;  // current s
               int sdown = s+gs;
               mesh.addFace (layer[t][s], layer[t+1][sdown], layer[t+1][sdown+1]);
            }
         }
      }

      // middle groups constant width
      for (int gt=1; gt<ngt-1; ++gt) {
         for (int i=0; i<k; ++i) {
            int t = gt*k+i;

            // longitude groups
            for (int gs=0; gs<ngs; ++gs) {

               // first k-1 columns
               for (int j=0; j<k; ++j) {
                  int s = gs*k+j;

                  // parity
                  boolean even = ((j + t) % 2) == 0;

                  // squares
                  if (even) {
                     mesh.addFace (layer[t][s], layer[t+1][s+1], layer[t][s+1]);
                     mesh.addFace (layer[t][s], layer[t+1][s], layer[t+1][s+1]);
                  } else {
                     mesh.addFace (layer[t][s], layer[t+1][s], layer[t][s+1]);
                     mesh.addFace (layer[t][s+1], layer[t+1][s], layer[t+1][s+1]);
                  }
               }
            }
         }
      }

      // last group shrinks
      {
         int gt = ngt-1;
         for (int i=0; i<k; ++i) {
            int t = gt*k+i;

            // longitude groups
            for (int gs=0; gs<ngs; ++gs) {
               for (int j=0; j<k-i-1; ++j) {
                  int s = gs*(k-i)+j;

                  int sdown = s-gs;

                  mesh.addFace(layer[t][s], layer[t+1][sdown], layer[t][s+1]);
                  mesh.addFace (layer[t][s+1], layer[t+1][sdown], layer[t+1][sdown+1]);
               }
               int s = (gs+1)*(k-i)-1;
               int sdown = s-gs;

               // last triangle
               mesh.addFace(layer[t][s], layer[t+1][sdown], layer[t][s+1]);
            }
         }
      }
      
      return mesh;
   }

   public static PolygonalMesh createQuadSphere(double r, int nslices) {
      return createQuadSphere(r, nslices, 0, 0, 0);
   }

   /**
    * Creates a spherical triangular mesh, centered on the origin,
    * with a radius <code>r</code>. The mesh is constructed using spherical
    * coordinates, with a resolution of <code>nslices</code> about the
    * equator and <code>nslices/2</code> longitudinally.
    * 
    * @param r radius of the sphere
    * @param nslices mesh resolution
    * @return created spherical mesh
    */
   public static PolygonalMesh createSphere(double r, int nslices) {
      PolygonalMesh mesh = createQuadSphere(r, nslices);
      mesh.triangulate();
      return mesh;
   }

   /**
    * Creates a spherical triangular mesh, centered on the origin,
    * with a radius <code>r</code>. The mesh is constructed using spherical
    * coordinates, with a resolution of <code>nslices</code> about the
    * equator and <code>nlevels</code> longitudinally.
    * 
    * @param r radius of the sphere
    * @param nslices equatorial mesh resolution
    * @param nlevels longitudinal mesh resolution
    * @return created spherical mesh
    */
   public static PolygonalMesh createSphere(
      double r, int nslices, int nlevels) {
      return createSphere (r, nslices, nlevels, false);
   }

   /**
    * Creates a spherical triangular mesh, centered on the origin,
    * with a radius <code>r</code>. The mesh is constructed using spherical
    * coordinates, with a resolution of <code>nslices</code> about the
    * equator and <code>nlevels</code> longitudinally. Texture coordinates
    * can optionally be created, where (0,0) and (1,1) map
    * to the spherical coordinates (-PI,PI) (south pole) and (PI,0)
    * (north pole). 
    * 
    * @param r radius of the sphere
    * @param nslices equatorial mesh resolution
    * @param nlevels longitudinal mesh resolution
    * @param addTextureCoords if <code>true</code>, generates texture
    * coordinates
    * @return created spherical mesh
    */
   public static PolygonalMesh createSphere(
      double r, int nslices, int nlevels, boolean addTextureCoords) {
      PolygonalMesh mesh = 
         createQuadSphere(r, nslices, nlevels, 0, 0, 0, addTextureCoords);
      mesh.triangulate();
      return mesh;
   }

   /**
    * Creates an triangular ellipsoid mesh, centered on the origin.  The mesh
    * is constructed using spherical coordinates, with a resolution of
    * <code>nslices</code> about the equator and <code>nslices/2</code>
    * longitudinally.
    *
    * @param a semi-axis length along x
    * @param b semi-axis length along y
    * @param c semi-axis length along z
    * @param nslices mesh resolution
    * @return created ellipsoidal mesh
    */
   public static PolygonalMesh createEllipsoid (
      double a, double b, double c, int nslices) {
      PolygonalMesh mesh = createSphere (1.0, nslices, nslices/2, false);
      mesh.scale (a, b, c);
      return mesh;
   }

   /**
    * Creates an triangular ellipsoid mesh, centered on the origin.  The mesh
    * is constructed using spherical coordinates, with a resolution of
    * <code>nslices</code> about the equator and <code>nlevels</code>
    * longitudinally.
    *
    * @param a semi-axis length along x
    * @param b semi-axis length along y
    * @param c semi-axis length along z
    * @param nslices equatorial mesh resolution
    * @param nlevels longitudinal mesh resolution
    * @return created ellipsoidal mesh
    */
   public static PolygonalMesh createEllipsoid (
      double a, double b, double c, int nslices, int nlevels) {

      PolygonalMesh mesh = createSphere (1.0, nslices, nlevels, false);
      mesh.scale (a, b, c);
      return mesh;
   }

   /**
    * Creates an open triangular hemispherical mesh, centered on the origin,
    * with a radius <code>r</code>. The mesh is constructed using spherical
    * coordinates, with a resolution of <code>nslices</code> about the equator
    * and <code>nlevels</code> longitudinally.
    * 
    * @param r radius of the sphere
    * @param nslices equatorial mesh resolution
    * @param nlevels longitudinal mesh resolution
    * @return created spherical mesh
    */
   public static PolygonalMesh createHemisphere (
      double r, int nslices, int nlevels) {
      PolygonalMesh mesh = 
         createQuadHemisphere(r, nslices, nlevels, 0, 0, 0);
      mesh.triangulate();
      return mesh;
   }

   public static PolygonalMesh createQuadSphere(
      double r, int nslices, double x, double y, double z) {
      return createQuadSphere(r, nslices, nslices / 2, x, y, z, false);
   }
   
   private static double clamp01 (double x) {
      if (x < 0) {
         return 0;
      }
      else if (x > 1) {
         return 1;
      }
      else {
         return x;
      }
   }
   
   private static void computeThetaPair (
      double[] theta, Vertex3d v0, Vertex3d v1, Point3d origin) {
      
      double EPS = 1e-8;
      theta[0] = Math.atan2 (v0.pnt.y-origin.y, v0.pnt.x-origin.x);
      if (theta[0] >= Math.PI-EPS) {
         theta[0] = -Math.PI;
      }
      theta[1] = Math.atan2 (v1.pnt.y-origin.y, v1.pnt.x-origin.x);
      if (theta[1] <= -Math.PI+EPS) {
         theta[1] = Math.PI;
      }
   }
   
   private static double computePhi (Vertex3d v, Point3d origin, double r) {
      return Math.acos ((v.pnt.z-origin.z)/r);
   }
   
   private static Vector3d createSphereTexel (double the, double phi) {
      return new Vector3d (
         clamp01((Math.PI+the)/(2*Math.PI)), clamp01(1.0-phi/Math.PI), 0);
   }
   
   public static void computeTextureCoordsForSphere (
      PolygonalMesh mesh, Point3d origin, double r, double tol) {
      
      ArrayList<Vector3d> vt = new ArrayList<Vector3d>();
      double[] theta = new double[2];
      for (int i=0; i<mesh.numFaces(); i++) {
         Face face = mesh.getFace(i);
         Vertex3d vtxs[] = face.getVertices();
      
         if (vtxs.length == 4) {
            computeThetaPair (theta, vtxs[0], vtxs[1], origin);
            double phi0 = computePhi (vtxs[0], origin, r);
            double phi2 = computePhi (vtxs[2], origin, r);
            vt.add (createSphereTexel (theta[0], phi0));
            vt.add (createSphereTexel (theta[1], phi0));
            vt.add (createSphereTexel (theta[1], phi2));
            vt.add (createSphereTexel (theta[0], phi2));
         }
         else if (Math.abs(vtxs[0].pnt.z-vtxs[1].pnt.z) <= tol) {
            // vtxs[2] corresponds to the north pole
            computeThetaPair (theta, vtxs[0], vtxs[1], origin);
            double phi0 = computePhi (vtxs[0], origin, r);
            double phi2 = 0;
            vt.add (createSphereTexel (theta[0], phi0));
            vt.add (createSphereTexel (theta[1], phi0));
            vt.add (createSphereTexel ((theta[0]+theta[1])/2, phi2));
         }
         else {
            // vtxs[0] corresponds to the south pole
            computeThetaPair (theta, vtxs[2], vtxs[1], origin);
            double phi2 = computePhi (vtxs[2], origin, r);
            double phi0 = Math.PI;
            vt.add (createSphereTexel ((theta[0]+theta[1])/2, phi0));
            vt.add (createSphereTexel (theta[1], phi2));
            vt.add (createSphereTexel (theta[0], phi2));
         }
      }
      int[] indices = new int[vt.size()];
      for (int i=0; i<indices.length; i++) {
         indices[i] = i;
      }
      mesh.setTextureCoords (vt, indices);
   }
   
   public static PolygonalMesh createQuadSphere(
      double r, int nslices, int nlevels, 
      double x, double y, double z, boolean addTextureCoords) {
      
      double tol = computeSphericalPointTolerance (
         r, 2*Math.PI, Math.PI, nslices, nlevels);
      PolygonalMesh mesh = new PolygonalMesh();
      VertexMap vtxMap = new VertexMap (tol);
      RigidTransform3d XLM = new RigidTransform3d (x, y, z);
      addQuadSphericalSection (
         mesh, r, Math.PI, Math.PI, nslices, nlevels, XLM, vtxMap);
      if (addTextureCoords) {
         Point3d origin = new Point3d (x, y, z);
         computeTextureCoordsForSphere (mesh, origin, r, tol);
      }
      return mesh;
   }

   public static PolygonalMesh createQuadHemisphere (
      double r, int nslices, int nlevels, 
      double x, double y, double z) {
      
      double tol = computeSphericalPointTolerance (
         r, 2*Math.PI, Math.PI, nslices, nlevels);
      PolygonalMesh mesh = new PolygonalMesh();
      VertexMap vtxMap = new VertexMap (tol);
      RigidTransform3d XLM = new RigidTransform3d (x, y, z);
      addQuadSphericalSection (
         mesh, r, Math.PI, Math.PI/2, nslices, nlevels, XLM, vtxMap);
      return mesh;
   }

   public static PolygonalMesh createSphere(
      double r, int nslices, double x, double y, double z) {
      PolygonalMesh mesh = createQuadSphere(r, nslices, x, y, z);
      mesh.triangulate();
      return mesh;
   }

   /**
    * Creates a box with rounded ends on the z-axis ends. The rounding is done
    * circularly, in the z-x plane. The z width (specified by wz) gives the
    * distance between the centers of each semi-circle which is used to create
    * the rounding. The mesh is quad-based, except at the center of the
    * semi-circles.
    * 
    * @param wx width along the x axis
    * @param wy width along the y axis
    * @param wz width along the z axis
    * @param nslices gives the number of sides used to approximate each
    * semi-circlar end
    */
   public static PolygonalMesh createQuadRoundedBox(
      double wx, double wy, double wz, int nslices) {

      return createQuadRoundedBox (wx, wy, wz, 1, 1, 1, nslices, false);
   }

   /**
    * Creates a box with rounded ends on the z-axis ends, and with a specified
    * mesh resolution in the x, y, and z directions. The rounding is done
    * circularly, in the z-x plane. The z width (specified by wz) gives the
    * distance between the centers of each semi-circle which is used to create
    * the rounding. The mesh is quad based, except at the center of the
    * semi-circles.
    * 
    * @param wx width along the x axis
    * @param wy width along the y axis
    * @param wz width along the z axis
    * @param nx number of mesh divisions along x between the rounding centers
    * @param ny number of mesh divisions along y between the rounding centers
    * @param nz number of mesh divisions along z between the rounding centers
    * @param nslices gives the number of sides used to approximate each
    * semi-circlar end
    * @param flatBottom if true, make the bottom flat instead of
    * rounded
    * @return created mesh
    */
   public static PolygonalMesh createQuadRoundedBox(
      double wx, double wy, double wz,
      int nx, int ny, int nz, int nslices, boolean flatBottom) {

      if (nx < 1) {
         throw new IllegalArgumentException("nx must be at least 1");
      }
      if (ny < 1) {
         throw new IllegalArgumentException("ny must be at least 1");
      }
      if (nz < 1) {
         throw new IllegalArgumentException("nz must be at least 1");
      }
      if (nslices < 1) {
         throw new IllegalArgumentException("nslices must be at least 1");
      }

      // ensure ny is even so that the annular vertex is fits with
      // the z-x side faces
      if (ny%2 != 0) {
         ny++;
      }
         
      // set map tolerance to be 0.01 times smallest spacing between vertices
      double tol = Math.min (0.01*wx/nx, 0.01*wy/ny);
      tol = Math.min (tol, 0.01*wz/nz);
      tol = Math.min (tol, 0.01*wx/nx*Math.sin(Math.PI/nslices));
      
      double r = wy/2;
      int nr = Math.max(ny/2, 1);

      VertexMap vtxMap = new VertexMap (tol);
      PolygonalMesh mesh = new PolygonalMesh();
      RigidTransform3d XLM = new RigidTransform3d();
      XLM.R.setRpy (Math.PI/2, -Math.PI/2, 0);
      XLM.p.set (0, -wz/2, 0);
      addQuadRectangle (mesh, wx, wy, nx, ny, XLM, vtxMap);
      XLM.p.set (0, -wz/2, wx/2);
      addQuadAnnularSector (mesh, 0, r, Math.PI, nr, nslices, XLM, vtxMap);
      XLM.p.set (0, 0, wx/2);
      addQuadCylindricalSection (
         mesh, r, wz, Math.PI, nz, nslices, /*outward=*/true, XLM, vtxMap);
      XLM.R.mulRpy (Math.PI, 0, 0); // flip 180 about z
      if (!flatBottom) {
         XLM.p.set (0, -wz/2, -wx/2);
         addQuadAnnularSector (mesh, 0, r, Math.PI, nr, nslices, XLM, vtxMap);
         XLM.p.set (0, 0, -wx/2);
         addQuadCylindricalSection (
            mesh, r, wz, Math.PI, nz, nslices, /*outward=*/true, XLM, vtxMap);
      }
      XLM.R.mulRpy (0, 0, Math.PI); // flip 180 about x
      if (!flatBottom) {
         XLM.p.set (0, wz/2, -wx/2);
         addQuadAnnularSector (mesh, 0, r, Math.PI, nr, nslices, XLM, vtxMap);
      }
      XLM.R.mulRpy (Math.PI, 0, 0); // flip 180 about z
      XLM.p.set (0, wz/2, wx/2);
      addQuadAnnularSector (mesh, 0, r, Math.PI, nr, nslices, XLM, vtxMap);
      XLM.p.set (0, wz/2, 0);
      addQuadRectangle (mesh, wx, wy, nx, ny, XLM, vtxMap);
      XLM.R.mulRpy (0, 0, Math.PI/2); // flip 90 about x
      XLM.p.set (-wy/2, 0, 0);
      addQuadRectangle (mesh, wx, wz, nx, nz, XLM, vtxMap);
      XLM.R.mulRpy (0, 0, Math.PI); // flip 180 about x    
      XLM.p.set (wy/2, 0, 0);
      addQuadRectangle (mesh, wx, wz, nx, nz, XLM, vtxMap);  
      if (flatBottom) {
         XLM.R.setRpy (0, 0, Math.PI);
         XLM.p.set (0, 0, -wx/2);
         addQuadRectangle (mesh, wy, wz, ny, nz, XLM, vtxMap);  
      }
      return mesh;
   }

   /**
    * Creates a triangular box mesh with rounded ends on the z-axis ends, and
    * with a specified mesh resolution in the x, y, and z directions. The
    * rounding is done circularly, in the z-x plane. The z width (specified by
    * wz) gives the distance between the centers of each semi-circle which is
    * used to create the rounding.
    * 
    * @param wz width along the z axis
    * @param wx width along the x axis
    * @param wy width along the y axis
    * @param nz number of mesh divisions along z between the rounding centers
    * @param nx number of mesh divisions along x between the rounding centers
    * @param ny number of mesh divisions along y between the rounding centers
    * @param nslices gives the number of sides used to approximate each
    * semi-circlar end
    */
   public static PolygonalMesh createRoundedBox(
      double wz, double wx, double wy, 
      int nz, int nx, int ny, int nslices) {

      PolygonalMesh mesh = 
         createQuadRoundedBox (wz, wx, wy, nz, nx, ny, nslices, false);
      mesh.triangulate();
      return mesh;
   }
      
   /**
    * Creates a triangular box mesh with rounded ends on the z-axis ends, and
    * with a specified mesh resolution in the x, y, and z directions. The
    * rounding is done circularly, in the z-x plane. The z width (specified by
    * wy) gives the distance between the centers of each semi-circle which is
    * used to create the rounding.
    * 
    * @param wz width along the z axis
    * @param wx width along the x axis
    * @param wy width along the y axis
    * @param nz number of mesh divisions along z between the rounding centers
    * @param nx number of mesh divisions along x between the rounding centers
    * @param ny number of mesh divisions along y between the rounding centers
    * @param nslices gives the number of sides used to approximate each
    * semi-circlar end
    * @param flatBottom if true, make the bottom flat instead of rounded
    */
   public static PolygonalMesh createRoundedBox(
      double wz, double wx, double wy, 
      int nz, int nx, int ny, int nslices, boolean flatBottom) {

      PolygonalMesh mesh = 
         createQuadRoundedBox (wz, wx, wy, nz, nx, ny, nslices, flatBottom);
      mesh.triangulate();
      return mesh;
   }
      
   /**
    * Creates a triangular box mesh with rounded ends on the z-axis ends. The
    * rounding is done circularly, in the z-x plane. The z width (specified by
    * wy) gives the distance between the centers of each semi-circle which is
    * used to create the rounding.
    * 
    * @param wz width along the z axis
    * @param wx width along the x axis
    * @param wy width along the y axis
    * @param nslices gives the number of sides used to approximate each
    * semi-circlar end
    */
   public static PolygonalMesh createRoundedBox(
      double wz, double wx, double wy, int nslices) {
      PolygonalMesh mesh = createQuadRoundedBox(wz, wx, wy, nslices);
      mesh.triangulate();
      return mesh;
   }

   /**
    * Creates a cylinder with rounded (spherical) ends on the z-axis ends. This
    * is really just a sphere with a cylinder inserted along the equator. The
    * cylinder portion is divided into a prescribed number of segments (nsegs).
    * The mesh is made of quads except at the poles of the rounded ends.
    * An argument <code>flatBottom</code> can be used to request that
    * the bottom of cylinder is flat instead of rounded.
    * 
    * @param r
    * radius of the cylinder
    * @param h
    * height of the cylinder along the z axis (measured as the distance between
    * the centers of the rounded ends.
    * @param nslices
    * number of sides used to form the cylinder. This will be rounded up to a
    * multiple of 4.
    * @param nsegs
    * number of segments along the length of the cylinder. This must be greater
    * than 0.
    * @param flatBottom if <code>true</code>, indicates that the bottom
    * of the cylinder should be flat instead of rounded.
    */
   public static PolygonalMesh createQuadRoundedCylinder(
      double r, double h, int nslices, int nsegs, boolean flatBottom) {

      if (nsegs < 1) {
         throw new IllegalArgumentException ("nsegs must be positive");
      }
      if (nslices < 1) {
         throw new IllegalArgumentException ("nsclices must be positive");
      }
      // round nslices to a multiple of 4
      if ((nslices % 4) != 0) {
         nslices = (nslices / 4 + 1) * 4;
      }

      double tol = computeSphericalPointTolerance (
         r, Math.PI, Math.PI/2, nslices, nslices);
      
      PolygonalMesh mesh = new PolygonalMesh();
      RigidTransform3d XLM = new RigidTransform3d();
      VertexMap vtxMap = new VertexMap (tol);

      addQuadCylindricalSection (
         mesh, r, h, 2*Math.PI, nsegs, nslices, /*outward=*/true, XLM, vtxMap);
      XLM.p.set (0, 0, h/2);
      addQuadSphericalSection (
         mesh, r, Math.PI, Math.PI/2, nslices, nslices/4, XLM, vtxMap);
      XLM.p.set (0, 0, -h/2);
      XLM.R.mulRpy (0, 0, Math.PI); // flip about x axis
      if (flatBottom) {
         addQuadAnnularSector (
            mesh, 0, r, 2*Math.PI, nslices/4, nslices, XLM, vtxMap);
      }
      else {
         addQuadSphericalSection (
            mesh, r, Math.PI, Math.PI/2, nslices, nslices/4, XLM, vtxMap);
      }
      return mesh;
   }

   /**
    * Creates a triangular cylinder mesh with rounded (spherical) ends on the 
    * z-axis ends. This is really just a sphere with a cylinder inserted along
    * the equator. The cylinder portion is divided into a prescribed number of
    * segments (nsegs). The mesh is made of triangles.  An argument 
    * <code>flatBottom</code> can be used to request that the bottom of 
    * cylinder is flat instead of rounded.
    * 
    * @param r
    * radius of the cylinder
    * @param h
    * height of the cylinder along the z axis (measured as the distance between
    * the centers of the rounded ends.
    * @param nslices
    * number of sides used to form the cylinder. This will be rounded up to a
    * multiple of 4.
    * @param nsegs
    * number of segments along the length of the cylinder. This must be greater
    * than 0.
    * @param flatBottom if <code>true</code>, indicates that the bottom
    * of the cylinder should be flat instead of rounded.
    */
   public static PolygonalMesh createRoundedCylinder (
      double r, double h, int nslices, int nsegs, boolean flatBottom) {

      PolygonalMesh mesh = createQuadRoundedCylinder (
         r, h, nslices, nsegs, flatBottom);
      mesh.triangulate();
      return mesh;      
   }

//   public static PolygonalMesh createTriangularRoundedCylinder(
//      double r, double h, int nslices) {
//      PolygonalMesh mesh = createRoundedCylinder(r, h, nslices);
//      mesh.triangulate();
//      return mesh;
//   }

   public static PolygonalMesh createQuadCone(
      double rtop, double rbot, double h, int nsides, int nh) {
      double[] xyTop;
      double[] xyBot;
      
      if (nsides < 3) {
         throw new IllegalArgumentException(
            "argument nsides must be at least 3");
      }
      if (rtop == 0) {
         xyTop = new double[] { 0, 0 };
      }
      else {
         xyTop = new double[2 * nsides];
      }
      if (rbot == 0) {
         xyBot = new double[] { 0, 0 };
      }
      else {
         xyBot = new double[2 * nsides];
      }
      for (int i = 0; i < nsides; i++) {
         double c = Math.cos(i * 2 * Math.PI / nsides);
         double s = Math.sin(i * 2 * Math.PI / nsides);
         if (rtop != 0) {
            xyTop[2 * i + 0] = c * rtop;
            xyTop[2 * i + 1] = s * rtop;
         }
         if (rbot != 0) {
            xyBot[2 * i + 0] = c * rbot;
            xyBot[2 * i + 1] = s * rbot;
         }
      }
      return createQuadPrism(xyTop, xyBot, h, nh);
   }
   
   public static PolygonalMesh createQuadCone(
      double rtop, double rbot, double h, int nsides) {
      double[] xyTop;
      double[] xyBot;

      if (nsides < 3) {
         throw new IllegalArgumentException(
            "argument nsides must be at least 3");
      }
      if (rtop == 0) {
         xyTop = new double[] { 0, 0 };
      }
      else {
         xyTop = new double[2 * nsides];
      }
      if (rbot == 0) {
         xyBot = new double[] { 0, 0 };
      }
      else {
         xyBot = new double[2 * nsides];
      }
      for (int i = 0; i < nsides; i++) {
         double c = Math.cos(i * 2 * Math.PI / nsides);
         double s = Math.sin(i * 2 * Math.PI / nsides);
         if (rtop != 0) {
            xyTop[2 * i + 0] = c * rtop;
            xyTop[2 * i + 1] = s * rtop;
         }
         if (rbot != 0) {
            xyBot[2 * i + 0] = c * rbot;
            xyBot[2 * i + 1] = s * rbot;
         }
      }
      return createQuadPrism(xyTop, xyBot, h);
   }
   
   public static PolygonalMesh createCone(
      double rtop, double rbot, double h, int nsides) {
      PolygonalMesh mesh = createQuadCone(rtop, rbot, h, nsides);
      mesh.triangulate();
      return mesh;
   }

   public static PolygonalMesh createQuadPrism(double[] xy, double h) {
      if (xy.length / 2 < 3) {
         throw new IllegalArgumentException(
            "argument xy must have at least 2*3 elements");
      }
      return createQuadPrism(xy, xy, h);
   }

   public static PolygonalMesh createPrism(double[] xy, double h) {
      PolygonalMesh mesh = createQuadPrism(xy, h);
      mesh.triangulate();
      return mesh;
   }

   // For a prism with a non-degenerate top and bottom,
   // with n sides, the faces are indexed as followes:
   //
   // sides: 0 ... n-1 counter-clockwise about the z axies
   // top: n
   // bottom: n+1
   //
   // The first n vertices surround the top and next n vertices surround
   // the bottom.
   //
   public static PolygonalMesh createQuadPrism(
      double[] xyTop, double[] xyBot, double h) {
      int nsides = Math.max(xyTop.length / 2, xyBot.length / 2);
      Point3d[] vlist;
      int[][] faces;

      if (nsides < 3) {
         throw new IllegalArgumentException(
            "either xyTop or xyBot must have at least 2*3 elements");
      }
      if (xyTop.length / 2 < 3 || xyBot.length / 2 < 3) {
         vlist = new Point3d[nsides + 1];
         faces = new int[nsides + 1][];
         faces[nsides] = new int[nsides];
      }
      else {
         vlist = new Point3d[2 * nsides];
         faces = new int[nsides + 2][];
         faces[nsides] = new int[nsides];
         faces[nsides + 1] = new int[nsides];
      }

      if (xyTop.length / 2 < 3) { // top has a single point
         vlist[0] = new Point3d(xyTop[0], xyTop[1], h / 2);
         for (int i = 0; i < nsides; i++) {
            vlist[i + 1] = new Point3d(xyBot[i * 2], xyBot[i * 2 + 1], -h / 2);
            int i_next = (i + 1) % nsides;
            faces[i] = new int[] { 0, i + 1, i_next + 1 };
            faces[nsides][i] = nsides - i;
         }
      }
      else if (xyBot.length / 2 < 3) { // bottom has a single point
         vlist[nsides] = new Point3d(xyBot[0], xyBot[1], -h / 2);
         for (int i = 0; i < nsides; i++) {
            vlist[i] = new Point3d(xyTop[i * 2], xyTop[i * 2 + 1], h / 2);
            int i_next = (i + 1) % nsides;
            faces[i] = new int[] { i_next, i, nsides };
            faces[nsides][i] = i;
         }
      }
      else {
         for (int i = 0; i < nsides; i++) {
            vlist[i] = new Point3d(xyTop[i * 2], xyTop[i * 2 + 1], h / 2);
            vlist[i + nsides] =
               new Point3d(xyBot[i * 2], xyBot[i * 2 + 1], -h / 2);
            int i_next = (i + 1) % nsides;
            faces[i] = new int[] { i, i + nsides, i_next + nsides, i_next };
            faces[nsides][i] = i;
            faces[nsides + 1][i] = 2 * nsides - 1 - i;
         }
      }
      PolygonalMesh mesh = new PolygonalMesh();
      mesh.set(vlist, faces);
      return mesh;
   }
   
   public static PolygonalMesh createQuadPrism(
      double[] xyTop, double[] xyBot, double h, int nH) {
      int nsides = Math.max(xyTop.length / 2, xyBot.length / 2);
      
      Point3d[] vlist;
      int[][] faces;
      int nFaces;
      int nVertices;
      
      
      if (nsides < 3) {
         throw new IllegalArgumentException(
            "either xyTop or xyBot must have at least 2*3 elements");
      }
      if (xyTop.length / 2 < 3 || xyBot.length / 2 < 3) {
         nFaces = nsides*nH+1;
         nVertices = nsides*nH+1;
         vlist = new Point3d[nVertices];
         faces = new int[nFaces][];
         faces[nFaces-1] = new int[nsides];
      }
      else {
         nFaces = nsides*nH+2;
         nVertices = nsides*(nH+1);
         
         vlist = new Point3d[nVertices];
         faces = new int[nFaces][];
         faces[nFaces-2] = new int[nsides];
         faces[nFaces-1] = new int[nsides];
      }

      if (xyTop.length / 2 < 3) { // top has a single point
         
         // build top-down
         vlist[0] = new Point3d(xyTop[0], xyTop[1], h / 2);
         double f = 1.0/nH; 
         
         for (int i = 0; i < nsides; i++) {
            vlist[i + 1] = new Point3d(f*xyBot[i * 2], f*xyBot[i * 2 + 1], h / 2 - f*h);
            int i_next = (i + 1) % nsides;
            faces[i] = new int[] { 0, i + 1, i_next + 1 };
         }
         
         for (int j=1; j<nH; j++) {
            f = 1.0/nH*(j+1);
            double hf = h/2-f*h;
            int offs = j*nsides+1;
            int poffs = (j-1)*nsides+1;
            
            for (int i = 0; i < nsides; i++) {
               vlist[i + offs] =
                  new Point3d(f*xyBot[i * 2], f*xyBot[i * 2 + 1], hf);
               int i_next = (i + 1) % nsides;
               
               faces[i+j*nsides] = new int[] { i+poffs, i + offs, i_next + offs, i_next + poffs };
            }
         }
         
         // last face
         for (int i=0; i<nsides; i++) {
            faces[nFaces-1][i] = nVertices-i-1;
         }
         
         
      }
      else if (xyBot.length / 2 < 3) { // bottom has a single point
         
         // build bottom-up
         vlist[0] = new Point3d(xyBot[0], xyBot[1], -h / 2);
         double f = 1.0/nH; 
         
         for (int i = 0; i < nsides; i++) {
            vlist[i + 1] = new Point3d(f*xyTop[i * 2], f*xyTop[i * 2 + 1], -h / 2 + f*h);
            int i_next = (i + 1) % nsides;
            faces[i] = new int[] { 0, i_next + 1, i + 1 };
         }
         
         for (int j=1; j<nH; j++) {
            f = 1.0/nH*(j+1);
            double hf = -h/2+f*h;
            int offs = j*nsides+1;
            int poffs = (j-1)*nsides+1;
            
            for (int i = 0; i < nsides; i++) {
               vlist[i + offs] =
                  new Point3d(f*xyTop[i * 2], f*xyTop[i * 2 + 1], hf);
               int i_next = (i + 1) % nsides;
               
               faces[i+j*nsides] = new int[] { i+poffs, i_next + poffs, i_next + offs, i + offs};
            }
         }
         
         // last face
         for (int i=0; i<nsides; i++) {
            faces[nFaces-1][i] = nVertices-nsides+i;
         }
      }
      else {
         
         // bottom-up
         for (int i=0; i<nsides; i++) {
            vlist[i] = new Point3d(xyBot[i * 2], xyBot[i * 2 + 1], -h / 2);
         }
         
         for (int j=0; j<nH; j++) {
            double f = 1.0/nH*(j+1);
            double hf = -h/2+f*h;
            
            int offs = (j+1)*nsides;
            int poffs = j*nsides;
            
            for (int i = 0; i < nsides; i++) {
               vlist[i + offs] =
                  new Point3d((1-f)*xyBot[i*2]+f*xyTop[i * 2], (1-f)*xyBot[i*2+1] + f*xyTop[i * 2 + 1], hf);
               int i_next = (i + 1) % nsides;
               
               faces[i+j*nsides] = new int[] { i+poffs, i_next + poffs, i_next + offs, i + offs};
            }
            
            // last faces
            for (int i=0; i<nsides; i++) {
               faces[nFaces-2][i] = nsides-i-1;
               faces[nFaces-1][i] = nVertices-nsides+i;
            }
            
         }
         
      }
      PolygonalMesh mesh = new PolygonalMesh();
      mesh.set(vlist, faces);
      return mesh;
   }

   public static PolygonalMesh createPrism(
      double[] xyTop, double[] xyBot, double h) {
      PolygonalMesh mesh = createQuadPrism(xyTop, xyBot, h);
      mesh.triangulate();
      return mesh;
   }

   private static void addQuadTriangles(
      PolygonalMesh mesh, int vidx0, int vidx1, int vidx2, int vidx3) {

      Vertex3d v0 = mesh.getVertices().get(vidx0);
      Vertex3d v1 = mesh.getVertices().get(vidx1);
      Vertex3d v2 = mesh.getVertices().get(vidx2);
      Vertex3d v3 = mesh.getVertices().get(vidx3);

      mesh.addFace(v0, v1, v2);
      mesh.addFace(v0, v2, v3);
   }
 
   /**
    * Creates a box with a cylindrical indentation in the top, parallel to the y
    * axis, with a radius of r and approximated by nsegs segments.
    */
   public static PolygonalMesh createHollowedBox(
      double wx, double wy, double wz, double r, int nsegs) {

      // make sure nsegs is divisible by 4:
      if ((nsegs % 4) != 0) {
         nsegs = (nsegs / 4 + 1) * 4;
      }

      PolygonalMesh mesh = new PolygonalMesh();

      int numv = 4 * (nsegs + 1);

      // create and add the vertices first
      for (int k = 0; k < 2; k++) {
         double y = (k == 0 ? -wy / 2 : wy / 2);
         for (int i = 0; i <= nsegs; i++) {
            double ang = Math.PI + (Math.PI * i) / nsegs;
            double s = Math.sin(ang);
            double c = Math.cos(ang);
            mesh.addVertex(new Point3d(c * r, y, s * r));
            if (i <= nsegs / 4) {
               mesh.addVertex(new Point3d(-wx / 2, y, -s / c * wx / 2));
            }
            else if (i <= 3 * nsegs / 4) {
               mesh.addVertex(new Point3d(-c / s * wx / 2, y, -wz));
            }
            else {
               mesh.addVertex(new Point3d(wx / 2, y, s / c * wx / 2));
            }
         }
      }

      // create and add the faces
      for (int i = 0; i < nsegs; i++) {
         int j = 2 * i;
         int k = j + numv / 2;
         addQuadTriangles(mesh, j, j + 1, j + 3, j + 2);
         addQuadTriangles(mesh, k, k + 2, k + 3, k + 1);
         addQuadTriangles(mesh, j, j + 2, k + 2, k);
         addQuadTriangles(mesh, j + 1, k + 1, k + 3, j + 3);
      }
      addQuadTriangles(mesh, 0, numv / 2, numv / 2 + 1, 1);
      int i = 2 * (nsegs);
      int k = i + numv / 2;
      addQuadTriangles(mesh, i, i + 1, k + 1, k);
      return mesh;
   }

   public static PolylineMesh createSphericalPolyline(double r, int nslices,
      int nlevels) {

      PolylineMesh mesh = new PolylineMesh();
      int[] lineIdxs = new int[nlevels - 1];
      int vtxIdx = 0;
      for (int j = 0; j < nslices; j++) {
         double the = 2 * Math.PI * (j / (double)nslices);
         double cthe = Math.cos(the);
         double sthe = Math.sin(the);
         for (int i = 1; i < nlevels; i++) {
            double phi = Math.PI / 2 - Math.PI * (i / (double)(nlevels));
            double cphi = Math.cos(phi);
            double sphi = Math.sin(phi);
            mesh.addVertex(
               new Point3d(r * cphi * cthe, r * cphi * sthe, r * sphi));
            lineIdxs[i - 1] = vtxIdx++;
         }
         mesh.addLine(lineIdxs);
      }
      return mesh;
   }

   /**
    * Creates a "skyline" mesh, which is a mesh defined over a rectangular
    * region in the x-y plane. The rectangle is divided into an nx X ny grid,
    * with the depth of each grid cell in z direction defined by a discrete
    * depth field specified by an array of strings. The depth field is given
    * by an array of ny strings, each containing nx characters giving the
    * depth for a specific row of grid cells. Depth field strings are ordered
    * from higher to lower y values. Each each character should be a digit in
    * the range '0' to '9', or ' ' which is equivalent to '0'. If d is the
    * numeric value of the character, then the depth is given by d*wz.
    *
    * <p>The mesh is centered on the origin, with the bottom face set at z =
    * -wz and a depth of 0 corresponding to z = 0.  If the number of strings is
    * less then ny, or the number of characters in any given string is less
    * then nx, the missing digits are assumed to be '0'. Extra strings or
    * characters are ignored.
    *
    * @param wx width of the mesh along the x axis
    * @param wy height of the mesh along the y axis.
    * @param wz basic unit of depth
    * @param nx number of grid cells in the x direction
    * @param ny number of grid cells in the y direction
    * @param depthField character strings giving the depth of each grid cell
    */
   public static PolygonalMesh createSkylineMesh (
      double wx, double wy, double wz, int nx, int ny, String... depthField) {

      int[] depth = new int[nx*ny];
      // set default depth
      for (int k=0; k<nx*ny; k++) {
         depth[k] = 0;
      }
      for (int j=0; j<ny && j<depthField.length; j++) {
         String str = depthField[j];
         for (int i=0; i<nx && i<str.length(); i++) {
            char c = str.charAt(i);
            if (c == ' ') {
               c = '0';
            }
            if (c < '0' || c > '9') {
               throw new IllegalArgumentException (
                  "Depth field characters must be '0'-'9' or ' '");
            }
            depth[(ny-1-j)*nx + i] = (c-'0');
         }
      }

      PolygonalMesh mesh = new PolygonalMesh();

      // set map tolerance to be 0.01 times smallest spacing between vertices
      VertexMap vmap =
         new VertexMap(Math.min(Math.min(wx/nx*1e-2, wy/ny*1e-2), wz/2*1e-2));

      double delx = wx/nx;
      double dely = wy/ny;

      for (int j=0; j<ny; j++) {
         for (int i=0; i<nx; i++) {
            double x = i*delx-wx/2;
            double y = j*dely-wy/2;
            // add bottom 
            addSquare (mesh, x, y, -wz, delx, dely, 0, i, j, true, vmap);
            int dg = depth[j*nx+i];
            // add top 
            addSquare (mesh, x, y, wz*dg, delx, dely, 0, i, j, false, vmap);

            // add side squares:
            int da; // adjacent depth

            // top
            if (j == ny-1) {
               da = -1;
            }
            else {
               da = depth[(j+1)*nx+i];
            }
            for (int d=da; d<dg; d++) {
               addSquare (mesh, x, y+dely, d*wz, delx, 0, wz, i, d, false, vmap);
            }

            // bottom
            if (j == 0) {
               da = -1;
            }
            else {
               da = depth[(j-1)*nx+i];
            }
            for (int d=da; d<dg; d++) {
               addSquare (mesh, x, y, d*wz, delx, 0, wz, i, d, true, vmap);
            }
            
            // left
            if (i == 0) {
               da = -1;
            }
            else {
               da = depth[j*nx+i-1];
            }
            for (int d=da; d<dg; d++) {
               addSquare (mesh, x, y, d*wz, 0, dely, wz, j, d, true, vmap);
            }
            
            // right
            if (i == nx-1) {
               da = -1;
            }
            else {
               da = depth[j*nx+i+1];
            }
            for (int d=da; d<dg; d++) {
               addSquare (mesh, x+delx, y, d*wz, 0, dely, wz, j, d, false, vmap);
            }
         }
      }
      return mesh;
   }

   private static void addSquare (
      PolygonalMesh mesh, double x, double y, double z,
      double dx, double dy, double dz,
      int xidx, int yidx, boolean flip, VertexMap vmap) {

      Vertex3d v0, v1, v2, v3;

      // use the xidx and yidx to decide whether or not to split at v0
      boolean splitAtV0 = ((xidx+yidx)%2 == 0);
      splitAtV0 = true;

      if (dz == 0) {
         // create in x-y plane
         v0 = vmap.getOrCreate (mesh, x, y, z, null);
         v1 = vmap.getOrCreate (mesh, x+dx, y, z, null);
         v2 = vmap.getOrCreate (mesh, x+dx, y+dy, z, null);
         v3 = vmap.getOrCreate (mesh, x, y+dy, z, null);
      }
      else if (dy == 0) {
         // create in z-x plane         
         v0 = vmap.getOrCreate (mesh, x, y, z, null);
         v1 = vmap.getOrCreate (mesh, x, y, z+dz, null);
         v2 = vmap.getOrCreate (mesh, x+dx, y, z+dz, null);
         v3 = vmap.getOrCreate (mesh, x+dx, y, z, null);
      }
      else if (dx == 0) {
         // create in y-z plane
         v0 = vmap.getOrCreate (mesh, x, y, z, null);
         v1 = vmap.getOrCreate (mesh, x, y+dy, z, null);
         v2 = vmap.getOrCreate (mesh, x, y+dy, z+dz, null);
         v3 = vmap.getOrCreate (mesh, x, y, z+dz, null);
      }
      else {
         throw new IllegalArgumentException ("square is not planar");
      }
      if (flip) {
         if (splitAtV0) {
            mesh.addFace (v0, v2, v1);
            mesh.addFace (v0, v3, v2);
         }
         else {
            mesh.addFace (v1, v3, v2);
            mesh.addFace (v1, v0, v3);
         }
      }
      else {
         if (splitAtV0) {
            mesh.addFace (v0, v1, v2);
            mesh.addFace (v0, v2, v3);
         }
         else {
            mesh.addFace (v1, v2, v3);
            mesh.addFace (v1, v3, v0);
         }
      }
   }

   /**
    * Creates a random point mesh, centered on the orgin, with a
    * specified number of vertices and width.
    */
   public static PointMesh createRandomPointMesh (int numv, double width) {
      
      PointMesh mesh = new PointMesh();
      Point3d pnt = new Point3d();
      for (int i=0; i<numv; i++) {
         pnt.setRandom();
         pnt.scale (width);
         mesh.addVertex (pnt);
      }
      return mesh;
   }

   // Used to collect and store vertices for a sub-face. Vertices
   // are stored in the upper-triangular portion of a 2D array.
   public static class VertexSet {
      Vertex3d[] myVerts;
      int myNumv;

      public VertexSet (int numv) {
         myVerts = new Vertex3d[numv * numv];
         myNumv = numv;
      }

      public Vertex3d get(int i, int j) {
         return myVerts[i * myNumv + j];
      }

      public void set(int i, int j, Vertex3d vtx) {
         myVerts[i * myNumv + j] = vtx;
      }

      public void check() {
         for (int i = 0; i < myNumv; i++) {
            for (int j = i; j < myNumv; j++) {
               if (get(i, j) == null) {
                  System.out.println("WARNING:  blank at " + i + " " + j);
               }
            }
         }
      }
   }

   private static Vertex3d[] collectEdgeVertices(
      HashMap<HalfEdge,Vertex3d[]> edgeVertices, PolygonalMesh mesh,
      Face face, int edgeNum, int res, NagataInterpolator interp) {

      int numv = res + 1;
      HalfEdge he = face.getEdge(edgeNum);
      Vertex3d[] vtxs = edgeVertices.get(he.opposite);

      if (vtxs == null) {
         // need to create the vertices
         vtxs = new Vertex3d[numv];
         vtxs[0] = he.getTail();
         vtxs[numv - 1] = he.getHead();
         // create vertices along edge
         for (int i = 1; i < numv - 1; i++) {
            Point3d pnt = new Point3d();
            double eta, zeta;
            double s = i / (double)res;
            switch (edgeNum) {
               case 1: {
                  eta = s;
                  zeta = 0;
                  break;
               }
               case 2: {
                  eta = 1;
                  zeta = s;
                  break;
               }
               case 0: {
                  eta = 1 - s;
                  zeta = 1 - s;
                  break;
               }
               default: {
                  throw new InternalErrorException("Illegal edgeNum " + edgeNum);
               }
            }
            interp.interpolateVertex(pnt, eta, zeta);
            // pnt.combine (0.5, vtxs[0].pnt, 0.5, vtxs[numv-1].pnt);
            vtxs[i] = new Vertex3d(pnt);
            mesh.addVertex(vtxs[i]);
         }
         edgeVertices.put(he, vtxs);
         return vtxs;
      }
      else {
         // vertices in the wrong order; return a reversed list of them.
         Vertex3d[] vtxsRev = new Vertex3d[numv];
         for (int i = 0; i < numv; i++) {
            vtxsRev[i] = vtxs[numv - 1 - i];
         }
         return vtxsRev;
      }
   }

   /**
    * Creates a refined version of a polygonal mesh, based on Nagata patches
    * applied with a given resolution. The supplied mesh must be triangular, and
    * its normals will be computed automatically.
    */
   public static PolygonalMesh createRefinedMesh(PolygonalMesh orig, int res) {

      if (!orig.isTriangular()) {
         throw new IllegalArgumentException("Input mesh must be triangular");
      }
      // if (!orig.isClosed()) {
      // throw new IllegalArgumentException ("Input mesh must be closed");
      // }
      ArrayList<Vector3d> nrmls = orig.getNormals();
      if (nrmls == null) {
         throw new IllegalArgumentException("Mesh does not have normals");
      }
      int[] nidxs = orig.getNormalIndices();
      PolygonalMesh mesh = new PolygonalMesh();
      for (Vertex3d vtx : orig.getVertices()) {
         mesh.addVertex(vtx.copy());
      }
      NagataInterpolator interp = new NagataInterpolator();

      int numv = res + 1; // num vertices along the edge of each sub face
      HashMap<HalfEdge,Vertex3d[]> edgeVertices =
         new HashMap<HalfEdge,Vertex3d[]>();

      int k = 0;
      for (Face face : orig.getFaces()) {

         // store sub vertices for the face in the upper triangular half of
         // subv.
         MeshFactory.VertexSet subv = new MeshFactory.VertexSet(numv);

         interp.setFace(
            face, 
            nrmls.get(nidxs[k++]), 
            nrmls.get(nidxs[k++]), 
            nrmls.get(nidxs[k++]));

         HalfEdge he = face.firstHalfEdge();
         Vertex3d v0 = he.getHead();
         he = he.getNext();
         Vertex3d v1 = he.getHead();
         he = he.getNext();
         Vertex3d v2 = he.getHead();

         subv.set(0, 0, mesh.getVertex(v0.getIndex()));
         subv.set(0, numv - 1, mesh.getVertex(v1.getIndex()));
         subv.set(numv - 1, numv - 1, mesh.getVertex(v2.getIndex()));

         Vertex3d[] vtxs20 =
            collectEdgeVertices(edgeVertices, mesh, face, 0, res, interp);
         for (int i = 1; i < numv - 1; i++) {
            subv.set(numv - 1 - i, numv - 1 - i, vtxs20[i]);
         }
         Vertex3d[] vtxs01 =
            collectEdgeVertices(edgeVertices, mesh, face, 1, res, interp);
         for (int j = 1; j < numv - 1; j++) {
            subv.set(0, j, vtxs01[j]);
         }
         Vertex3d[] vtxs12 =
            collectEdgeVertices(edgeVertices, mesh, face, 2, res, interp);
         for (int i = 1; i < numv - 1; i++) {
            subv.set(i, numv - 1, vtxs12[i]);
         }

         for (int i = 1; i < numv - 1; i++) {
            for (int j = i + 1; j < numv - 1; j++) {
               double eta = j / (double)res;
               double zeta = i / (double)res;
               Point3d pnt = new Point3d();
               interp.interpolateVertex(pnt, eta, zeta);
               Vertex3d vtx = new Vertex3d(pnt);
               mesh.addVertex(vtx);
               subv.set(i, j, vtx);
            }
         }

         subv.check();
         for (int i = 0; i < res; i++) {
            for (int j = i; j < res; j++) {
               mesh.addFace(
                  subv.get(i, j), subv.get(i, j + 1), subv.get(i + 1, j + 1));
               if (i != j) {
                  mesh
                     .addFace(
                        subv.get(i, j), subv.get(i + 1, j + 1),
                        subv.get(i + 1, j));
               }
            }
         }
      }
      return mesh;
   }

   public static PolygonalMesh getIntersection(PolygonalMesh mesh1,
      PolygonalMesh mesh2) {
      CSG csg = new CSG();
      return csg.getIntersection(mesh1, mesh2);
   }

   public static PolygonalMesh getUnion(PolygonalMesh mesh1, 
      PolygonalMesh mesh2) {
      CSG csg = new CSG();
      return csg.getUnion(mesh1, mesh2);
   }
   
   public static PolygonalMesh getSubtraction(PolygonalMesh mesh1, 
      PolygonalMesh mesh2) {
      CSG csg = new CSG();
      return csg.getSubtraction(mesh1, mesh2);
   }

   public static ArrayList<HalfEdge> findBorderEdges(PolygonalMesh mesh) {

      // collect single half-edges
      ArrayList<HalfEdge> heList = new ArrayList<HalfEdge>();

      for (Face face : mesh.getFaces()) {
         HalfEdge he = face.he0;
         do {
            if (he.opposite == null) {
               heList.add(he);
            } else if (he.opposite.face == null) {
               heList.add(he);
            }
            he = he.next;
         } while (he != face.he0);
      }
      return heList;

   }
   
   public static boolean fillHoles(PolygonalMesh mesh) {

      ArrayList<HalfEdge> heList = findBorderEdges(mesh);

      while (heList.size() > 0) {
         ArrayList<Vertex3d> vtxList = new ArrayList<Vertex3d>();

         // get last half edge and remove from list
         HalfEdge he = heList.get(heList.size() - 1);
         heList.remove(heList.size() - 1);

         Vertex3d head = he.getHead();
         Vertex3d tail = he.getTail();
         vtxList.add(tail);

         boolean found = true;
         while (head != tail && found == true) {
            Iterator<HalfEdge> it = tail.getIncidentHalfEdges();
            found = false;
            while (it.hasNext()) {
               HalfEdge next = it.next();
               if (heList.contains(next)) {
                  he = next;
                  tail = next.tail;
                  vtxList.add(tail);
                  heList.remove(he);
                  found = true;
                  break;
               }
            }
         }

         if (head == tail) {
            Vertex3d[] vtxArray = new Vertex3d[vtxList.size()];
            for (int i = 0; i < vtxArray.length; i++) {
               vtxArray[vtxArray.length - 1 - i] = vtxList.get(i);
            }
            mesh.addFace(vtxList.toArray(new Vertex3d[vtxList.size()]));
         } else {
            return false;
         }

      }
      return mesh.isClosed();
   }

   private static class ProjectionInfo implements Comparable<ProjectionInfo> {
      public Point3d proj = null;
      public double dist = Double.POSITIVE_INFINITY;
      public HalfEdge he = null;
      public Vertex3d vtx = null;

      @Override
      public int compareTo(ProjectionInfo o) {
         if (dist < o.dist) {
            return -1;
         } else if (dist > o.dist) {
            return 1;
         }
         return 0;
      }
   }
  
   public static void closeSeams(PolygonalMesh mesh) {

      ArrayList<HalfEdge> heList = findBorderEdges(mesh);

      // collect border vertices
      ArrayList<Vertex3d> vtxList = new ArrayList<Vertex3d>();
      for (HalfEdge edge : heList) {
         if (!vtxList.contains(edge.head)) {
            vtxList.add(edge.head);
         }
         if (!vtxList.contains(edge.tail)) {
            vtxList.add(edge.tail);
         }
      }

      // project all vertices to closest edges, not including its own
      PriorityQueue<ProjectionInfo> projInfoQueue =
         new PriorityQueue<ProjectionInfo>();

      // initial closest distances
      for (int i = 0; i < vtxList.size(); i++) {
         ProjectionInfo projInfo = new ProjectionInfo();
         updateProjInfo(projInfo, vtxList.get(i), heList, mesh);
         projInfoQueue.add(projInfo);
      }

      while (projInfoQueue.size() > 0) {
         // check if close to endpoint
         ProjectionInfo next = projInfoQueue.remove();

         if (next.dist < Double.MAX_VALUE) {
            if (next.vtx.getPosition().distance(next.he.head.getPosition()) < 1e-10) {
               // replace vertex
               mergeVertices(mesh, next.vtx, next.he.head);
            } else if (next.vtx.getPosition().distance(
               next.he.tail.getPosition()) < 1e-10) {
               mergeVertices(mesh, next.vtx, next.he.tail);
            } else {
               // split edge
               Face oldFace = next.he.face;
               mesh.removeFace(oldFace);
               
               next.proj.add(next.vtx.getPosition());
               next.proj.scale(0.5);
               next.vtx.setPosition(next.proj);

               Vertex3d[] vtxs = oldFace.getVertices();
               
               HalfEdge face2he = next.he.next;
               Face face1 = mesh.addFace(next.vtx, next.he.head, face2he.head);
               for (int i=0; i<vtxs.length; i++) {
                  if (vtxs[i]==next.he.head){
                     vtxs[i] = next.vtx;
                     break;
                  }
               }
               Face face2 = mesh.addFace(vtxs);
               
               // update half-edge list
               HalfEdge it = oldFace.he0;
               do {
                  heList.remove(it);
                  it = it.next;
               } while (it != oldFace.he0);
               
               it = face1.he0;
               do {
                  if (it.opposite == null || it.opposite.face == null) {
                     if (!heList.contains(it)) {
                        heList.add(it);
                     }
                  }
                  it = it.next;
               } while (it != face1.he0);
               
               it = face2.he0;
               do {
                  if (it.opposite == null || it.opposite.face == null) {
                     if (!heList.contains(it)) {
                        heList.add(it);
                     }
                  }
                  it = it.next;
               } while (it != face2.he0);
               
               
            }
         }
         
         // recompute some half edge stuff
         Iterator<ProjectionInfo> pit = projInfoQueue.iterator();
         while (pit.hasNext()) {
            ProjectionInfo projInfo = pit.next();
            if (!isBorderVertex(projInfo.vtx, mesh)) {
               pit.remove();
            } else if (!isBorderEdge(projInfo.he, mesh)) {
               heList.remove(projInfo.he);
               updateProjInfo(projInfo, projInfo.vtx, heList, mesh);
            }
         }
         
            
      }

   }

   private static boolean isBorderEdge(HalfEdge edge, PolygonalMesh mesh) {
      if (edge.opposite == null || edge.opposite.face == null) {
         return mesh.getFaces().contains(edge.face);
      }
      return false;
   }
   
   private static boolean isBorderVertex(Vertex3d vtx, PolygonalMesh mesh) {
      
      Iterator<HalfEdge> it = vtx.getIncidentHalfEdges();
      while (it.hasNext()) {
         HalfEdge he = it.next();
         if (he.opposite == null || he.opposite.face == null) {
            return (mesh.getVertices().contains(vtx));
         }
      }
      return false;
   }
   
   private static void updateProjInfo(ProjectionInfo projInfo, Vertex3d vtx,
      ArrayList<HalfEdge> heList,  PolygonalMesh mesh) {

      Point3d proj = new Point3d();
      projInfo.dist = Double.POSITIVE_INFINITY;
      projInfo.vtx = vtx;
      
      for (HalfEdge he : heList) {
         
         if (isBorderEdge(he, mesh)) {
            if (vtx != he.head && vtx != he.tail) {
               double d = projectToSegment(proj, vtx.getPosition(), he, 1e-10);
               if (d < projInfo.dist) {
                  projInfo.he = he;
                  projInfo.proj = new Point3d(proj);
                  projInfo.dist = d;
               }
               if (d < 1e-10) {
                  break;
               }
            }
         }
      }

   }

   public static void mergeVertices(PolygonalMesh mesh, Vertex3d vtx1,
      Vertex3d vtx2) {

      ArrayList<HalfEdge> hes = new ArrayList<>();
      Iterator<HalfEdge> it = vtx1.getIncidentHalfEdges();
      while (it.hasNext()) {
         hes.add(it.next());
      }
      
      for (HalfEdge he : hes) {
         mesh.removeFace(he.face);
         Vertex3d[] vtxs = he.face.getVertices();
         for (int i = 0; i < vtxs.length; i++) {
            if (vtxs[i] == vtx1) {
               vtxs[i] = vtx2;
            }
         }
         mesh.addFace(vtxs);
      }

   }

   private static double projectToSegment(Point3d proj, Point3d pnt,
      HalfEdge he, double tol) {

      Point3d A = he.head.getPosition();
      Point3d B = he.tail.getPosition();

      Vector3d AB = new Vector3d(B);
      AB.sub(A);
      double ab2 = AB.dot(AB);
      if (ab2 < tol * tol) {
         proj.set(he.head.getPosition());
         return 0;
      }

      Vector3d AC = new Vector3d(pnt);
      AC.sub(A);
      double t = AC.dot(AB) / ab2;
      if (t <= 0) {
         proj.set(A);
      } else if (t >= 1) {
         proj.set(B);
      } else {
         proj.interpolate(A, t, B);
      }

      return proj.distance(pnt);
   }
   
   public static PolygonalMesh getFlipped(PolygonalMesh mesh) {
      
      PolygonalMesh out = new PolygonalMesh();
      
      Point3d[] pnts = new Point3d[mesh.numVertices()];
      int[][] faces = new int[mesh.numFaces()][];
      
      for (int i=0; i<pnts.length; i++ ) {
         pnts[i] = new Point3d(mesh.getVertex(i).getPosition());
      }
      
      ArrayList<Face> faceList = mesh.getFaces();
      for (int i=0; i<faces.length; i++) {
         Face face = faceList.get(i);
         int n = face.numVertices();
         faces[i] = new int[n];
         int[] fidxs = face.getVertexIndices();
         for (int j=0; j<n; j++) {
            faces[i][j] = fidxs[n-j-1]; 
         }
      }
      
      out.set(pnts, faces);
      return out;
   }
   
   /**
    * Create a new PolygonalMesh from a subset of faces.  Vertices are
    * copied, so no longer reference the original mesh.
    * @param faces set of faces
    * @return new mesh containing copy
    */
   public static PolygonalMesh createFromFaces(Iterable<Face> faces) {
      PolygonalMesh out = new PolygonalMesh();
      HashMap<Vertex3d, Vertex3d> vtxMap = new HashMap<>();
      
      for (Face f : faces) {
         Vertex3d[] ovtxs = f.getVertices();
         for (int i=0; i<ovtxs.length; ++i) {
            Vertex3d nvtx = vtxMap.get(ovtxs[i]);
            if (nvtx == null) {
               nvtx = out.addVertex(ovtxs[i].getPosition());
               vtxMap.put(ovtxs[i], nvtx);
            }
            ovtxs[i] = nvtx;
         }
         out.addFace(ovtxs);
      }
      
      return out;
   }
   
   /**
    * Traverses faces, collected all that are connected within a certain angle limit
    * @param f starting face
    * @param visited list of attached faces
    * @param minCosLimit cosine of limit angle, all cosines above this are considered
    */
   private static void traverseAndAdd(Face f, ArrayList<Face> visited, double minCosLimit) {

      ArrayDeque<Face> queue = new ArrayDeque<>();
      f.setVisited();
      queue.add(f);

      while (!queue.isEmpty()) {
         f = queue.remove();
         visited.add(f);

         HalfEdge he0 = f.firstHalfEdge();
         HalfEdge he = he0;
         do {
            if (he.opposite != null) {
               Face oface = he.opposite.getFace();
               if (oface != null && !oface.isVisited()) {
                  double c = oface.getNormal().dot(f.getNormal());
                  if (c >= minCosLimit) {
                     oface.setVisited();
                     queue.add(oface);
                  }
               }
            }
            he = he.getNext();
         } while (he != he0);
      }
   }
   
   /**
    * Splits a mesh into several pieces based on connectivity and angle between faces
    * @param splitme mesh to split
    * @param minCosLimit cosine of limit angle, if neighbour's cosine is above this, considered connected
    * @param minSize minimum number of faces to consider as a new piece (groups of few faces are discarded)
    * @return set of split meshes
    */
   public static ArrayList<PolygonalMesh> splitMesh(PolygonalMesh splitme, double minCosLimit, int minSize) {

      ArrayList<PolygonalMesh> out = new ArrayList<>();

      // clear visited flag
      for (Face f : splitme.getFaces()) {
         f.clearVisited();
      }
      // start with a face, move to neighbors as long as within given angle
      boolean done = false;
      while (!done) {
         done = true;
         for (Face f : splitme.getFaces()) {
            if (!f.isVisited()) {
               done = false;
               ArrayList<Face> nfaces = new ArrayList<Face>();
               traverseAndAdd(f, nfaces, minCosLimit);
               if (nfaces.size() >= minSize) {
                  PolygonalMesh splitted = MeshFactory.createFromFaces(nfaces);
                  out.add(splitted);
               }
            }
         }
      }
      return out;
   }

   /**
    * Creates a convex hull from the vertex positions of an input mesh.
    *
    * @param mesh input mesh providing the vertex positions
    * @return convex hull mesh
    */
   public static PolygonalMesh createConvexHull (MeshBase mesh) {
      quickhull3d.Point3d[] pnts = new quickhull3d.Point3d[mesh.numVertices()];
      for (int i=0; i<pnts.length; i++) {
         Point3d pos = mesh.getVertex(i).getPosition();
         pnts[i] = new quickhull3d.Point3d(pos.x, pos.y, pos.z);
      }
      QuickHull3D chull = new QuickHull3D();
      chull.build (pnts);
      PolygonalMesh hull = new PolygonalMesh();
      for (quickhull3d.Point3d pnt : chull.getVertices()) {
         hull.addVertex (pnt.x, pnt.y, pnt.z);
      }
      for (int[] faceIdxs : chull.getFaces()) {
         hull.addFace (faceIdxs);
      }
      hull.triangulate();
      return hull;
   }

   /**
    * Splits a mesh into several pieces based on connectivity
    * @param splitme mesh to split
    * @return set of split meshes
    */
   public static ArrayList<PolygonalMesh> splitMesh(PolygonalMesh splitme) {
      return splitMesh(splitme, -2, -1);
   }
   
   /**
    * Builds a convex hull around a set of mesh vertices
    * 
    * Internally relies on tetgen to generate a Delaunay tetrahedral mesh around the points
    * @param mesh mesh with vertices
    * @return convex hull surface
    */
   public static PolygonalMesh computeConvexHull(MeshBase mesh) {
      
      TetgenTessellator tt = new TetgenTessellator ();
      double[] coords = new double[mesh.numVertices ()*3];
      
      // build tet mesh from vertex locations
      int cidx = 0;
      for (Vertex3d vtx : mesh.getVertices ()) {
         Point3d pnt = vtx.pnt;
         coords[cidx++] = pnt.x;
         coords[cidx++] = pnt.y;
         coords[cidx++] = pnt.z;
      }
      tt.buildFromPoints (coords);
      
      // get node locations and hull faces
      Point3d[] pnts = tt.getPoints();
      int[] hullFaces = tt.getHullFaces ();
      
      // build hull
      PolygonalMesh hull = new PolygonalMesh ();
      for (int i=0; i<pnts.length; i++) {
         hull.addVertex (pnts[i]);
      }
      int[] idxs = new int[3];
      for (int k=0; k<hullFaces.length; k += 3) {
         idxs[0] = hullFaces[k];
         idxs[1] = hullFaces[k+1];
         idxs[2] = hullFaces[k+2];
         hull.addFace (idxs);
      }
      
      hull.setMeshToWorld (mesh.getMeshToWorld ());
      
      return hull;
   }
}
