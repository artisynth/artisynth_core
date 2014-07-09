package maspack.geometry;

import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;

/**
 * Constructus a triangular patch continuous with respect to vertex normals.
 * 
 * @author elliote
 * 
 */
public class TrianglePatch {
   Point3d[][] b = new Point3d[4][4];

   // void interpolate(Point3d p, double u, double v, double w)
   // {
   // for(int i = 0; i <= 3; i++)
   // {
   // for(int j = 0; j <= (3-i); j++)
   // {
   // int k = 3 - (i + j);
   // double B = factorial(3)/(factorial(i) * factorial(j) * factorial(k)) *
   // Math.pow(u, i) * Math.pow(v, j) * Math.pow(w, k);
   //				
   // p.scaledAdd(B, b[i][j], p);
   // }
   // }
   // }
   //		
   // int factorial(int n)
   // {
   // int f = 1;
   // for(int i = 2; i <= n; i++)
   // f *= i;
   // return f;
   // }

   // fast expanded formula
   Vector3d f = new Vector3d();

   /**
    * Interpolate the triangle patch using barycentric coordinates.
    * 
    * @param p
    * The resulting point.
    * @param u
    * The first vertex weight.
    * @param v
    * The second vertex weight.
    * @param w
    * The third vertex weight.
    */
   void interpolate (Point3d p, double u, double v, double w) {
      f.scale (u, b[3][0]);
      f.scaledAdd (3 * v, b[2][1], f);
      f.scaledAdd (3 * w, b[2][0], f);
      p.scale (u * u, f);

      f.scale (v, b[0][3]);
      f.scaledAdd (3 * u, b[1][2], f);
      f.scaledAdd (3 * w, b[0][2], f);
      p.scaledAdd (v * v, f, p);

      f.scale (w, b[0][0]);
      f.scaledAdd (3 * u, b[1][0], f);
      f.scaledAdd (3 * v, b[0][1], f);
      p.scaledAdd (w * w, f, p);

      p.scaledAdd (6 * v * w * u, b[1][1], p);
   }

   /**
    * Create a triangular patch tangential with respect to vertex normals.
    * 
    * @param p0
    * The first vertex.
    * @param n0
    * The normal of the first vertex.
    * @param p1
    * @param n1
    * @param p2
    * @param n2
    */
   TrianglePatch (Point3d p0, Vector3d n0, Point3d p1, Vector3d n1, Point3d p2,
   Vector3d n2) {
      double a = 0.33;

      b[3][0] = new Point3d (p0);
      b[0][3] = new Point3d (p1);
      b[0][0] = new Point3d (p2);

      Vector3d p0p1 = new Vector3d(), p1p2 = new Vector3d(), p2p0 =
         new Vector3d();
      p0p1.sub (p1, p0);
      p1p2.sub (p2, p1);
      p2p0.sub (p0, p2);

      double p0p1d = p0p1.norm(), p1p2d = p1p2.norm(), p2p0d = p2p0.norm();

      Point3d p;

      p = b[2][1] = new Point3d();
      p.scaledAdd (-n0.dot (p0p1), n0, p0p1);
      p.scaledAdd (a * p0p1d / p.norm(), p, p0);

      p = b[1][2] = new Point3d();

      p.scaledAdd (-n1.dot (p0p1), n1, p0p1);
      p.scaledAdd (-a * p0p1d / p.norm(), p, p1);

      p = b[0][2] = new Point3d();
      p.scaledAdd (-n1.dot (p1p2), n1, p1p2);
      p.scaledAdd (a * p1p2d / p.norm(), p, p1);

      p = b[0][1] = new Point3d();
      p.scaledAdd (-n2.dot (p1p2), n2, p1p2);
      p.scaledAdd (-a * p1p2d / p.norm(), p, p2);

      p = b[1][0] = new Point3d();
      p.scaledAdd (-n2.dot (p2p0), n2, p2p0);
      p.scaledAdd (a * p2p0d / p.norm(), p, p2);

      p = b[2][0] = new Point3d();
      p.scaledAdd (-n0.dot (p2p0), n0, p2p0);
      p.scaledAdd (-a * p2p0d / p.norm(), p, p0);

      p = b[1][1] = new Point3d();
      Point3d E = new Point3d();
      E.add (b[2][1]);
      E.add (b[1][2]);
      E.add (b[0][2]);
      E.add (b[0][1]);
      E.add (b[1][0]);
      E.add (b[2][0]);
      E.scale (1.0 / 6.0);

      Point3d V = new Point3d();
      V.add (p0);
      V.add (p1);
      V.add (p2);
      V.scale (1.0 / 3.0);

      p.scale (1.5, E);
      p.scaledAdd (-0.5, V, p);
   }
}
