package artisynth.core.femmodels.integration;

import maspack.matrix.Point3d;
import maspack.util.RandomGenerator;

/**
 * Helper class for uniformly sampling from some basic shapes
 */
public class MonteCarloSampler {

   /**
    * Generates a uniform random sample from the canonical tetrahedron.
    *  
    * @param pnt point to populate
    */
   public static void sampleTet(Point3d pnt) {

      // Uses the folding technique of:
      // Generating Random Points in a Tetrahedron, by C. Rocchini and
      //     P. Cignoni, 2001
      double s = RandomGenerator.nextDouble (0, 1);
      double t = RandomGenerator.nextDouble (0, 1);
      double u = RandomGenerator.nextDouble (0, 1);

      // fold into lower triangular prism
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
      pnt.set (sx, tx, ux);
   }

   /**
    * Generates a uniform random sample from the canonical tetrahedron.
    *  
    * @param pnt point to populate
    */
   public static void sampleTet2(Point3d pnt) {

      // Uses modified folding technique by Sanchez 2016? ;)
      // adapted based on folding of a pyramid 
      double s = RandomGenerator.nextDouble (0, 1);
      double t = RandomGenerator.nextDouble (0, 1);
      double u = RandomGenerator.nextDouble (0, 1);

      // fold into pyramid in first quadrant
      if ( (s+u > 1) || (t+u > 1)) {
         if (s < t) {
            // base on y=1 plane
            // (s,t,u) = (1-u, s, 1-t)
            double w = t;
            t = s;
            s = 1-u;
            u = 1-w;
         }
         else {
            // base on x=1 plane
            // (s,t,u) = (t, 1-u, 1-s)
            double w = s;
            s = t;
            t = 1-u;
            u = 1-w;
         }
      }

      // fold pyramid in half and skew
      if (t > s) {
         double w = s;
         s = 1-u-t;
         t = w;
      } else {
         double w = t;
         t = 1-u-s;
         s = w;
      }

      pnt.set (s, t, u);
   }

   /**
    * Generates a uniform random sample from the canonical hexahedron.
    * @param pnt point to populate
    */
   public static void sampleHex(Point3d pnt) {
      pnt.x = RandomGenerator.nextDouble (-1, 1);
      pnt.y = RandomGenerator.nextDouble (-1, 1);
      pnt.z = RandomGenerator.nextDouble (-1, 1);
   }

   /**
    * Generates a uniform random sample from the canonical wedge.
    * @param pnt point to populate
    */
   public static void sampleWedge(Point3d pnt) {
      // simple folding
      double s = RandomGenerator.nextDouble (0, 1);
      double t = RandomGenerator.nextDouble (0, 1);
      double u = RandomGenerator.nextDouble (-1, 1);
      if (s+t > 1) {
         s = 1-s;
         t = 1-t;
      }
      pnt.x = s;
      pnt.y = t;
      pnt.z = u;
   }

   /**
    * Generates a uniform random sample from the canonical pyramid,
    * @param pnt sampled point
    */
   public static void samplePyramid(Point3d pnt) {
      double s = RandomGenerator.nextDouble (-1, 1);
      double t = RandomGenerator.nextDouble (-1, 1);
      double u = RandomGenerator.nextDouble (0, 1);  // initially squeeze into +z

      // pyramid folding using four quadrants
      // take positive s, t, putting in first quadrant
      double sx = s < 0 ? -s : s;
      double tx = t < 0 ? -t : t;

      if ( (sx+u > 1) || (tx+u > 1)) {
         if (sx < tx) {
            // base on y=1 plane
            // (s,t,u) = (1-u, s, 1-t)
            double wx = tx;
            tx = sx;
            sx = 1-u;
            u = 1-wx;
         }
         else {
            // base on x=1 plane
            // (s,t,u) = (t, 1-u, 1-s)
            double wx = sx;
            sx = tx;
            tx = 1-u;
            u = 1-wx;
         }
      }

      // fold back to correct quadrant
      if (s < 0) {
         sx = -sx;
      }
      if (t < 0) {
         tx = -tx;
      }
      
      // stretch down so u from [-1,1]
      u = 2*u-1;
      pnt.set (sx, tx, u);

   }
   
   /**
    * Generates a uniform random sample from inside a unit sphere
    * @param pnt sampled point
    */
   public static void sampleSphere(Point3d pnt) {
      
      double u = RandomGenerator.nextDouble(0, 1);
      double n = Math.cbrt(u);
      
      double x = RandomGenerator.nextGaussian();
      double y = RandomGenerator.nextGaussian();
      double z = RandomGenerator.nextGaussian();
      
      double d = Math.sqrt(x*x+y*y+z*z);
      double r = 0;
      if (d > 0) {
         r = n/d;
      }
      pnt.x = x*r;
      pnt.y = y*r;
      pnt.z = z*r;
      
   }

   // verify uniform sampling
   public static void tetTest(int nsamples) {

      // split tet into eight equal volumes for verification
      int[] counts = new int[8];

      Point3d s = new Point3d();
      for (int i=0; i<nsamples; ++i) {
         sampleTet(s);

         // separate into equal subvolumes
         if (s.x > 0.5) {
            ++counts[0];
         } else if (s.y > 0.5) {
            ++counts[1];
         } else if (s.z > 0.5) {
            ++counts[2];
         } else if (s.y + s.z < s.x) {
            ++counts[3];
         } else if (s.x + s.z < s.y) {
            ++counts[4];
         } else if (s.x + s.y < s.z) {
            ++counts[5];
         } else if (s.x + s.y > 0.5){
            ++counts[6];
         } else {
            ++counts[7];
         }
      }

      System.out.println("Tet volumes:");
      for (int i=0; i<counts.length; ++i) {
         System.out.println("Volume " + i + ": = " + 1.0*counts[i]/nsamples);
      }

   }
   
   public static void pyramidTest(int nsamples) {

      // split pyramid into eight equal volumes for verification
      int[] counts = new int[8];

      Point3d s = new Point3d();
      for (int i=0; i<nsamples; ++i) {
         samplePyramid(s);

         // separate into equal subvolumes
         if (s.x >= 0 && s.y >= 0 && s.x > s.z && s.y > s.z) {
            ++counts[0];
         } else if (s.x >= 0 && s.y < 0  && s.x > s.z && s.y < -s.z) {
            ++counts[1];
         } else if (s.y >= 0 && s.x < -s.z && s.y > s.z) {
            ++counts[2];
         } else if (s.x < -s.z && s.y < -s.z){
            ++counts[3];
         } else if (s.z > 0.5) {
            ++counts[4];
         } else if (s.x <= s.z && s.y <= s.z && s.x > -s.z && s.y > -s.z) {
            ++counts[5];
         } else if (s.x <= s.z && s.x > -s.z) {
            ++counts[6];
         } else {
            ++counts[7];
         }
      }

      System.out.println("Pyramid volumes:");
      for (int i=0; i<counts.length; ++i) {
         System.out.println("Volume " + i + ": = " + 1.0*counts[i]/nsamples);
      }
   }

   public static void wedgeTest(int nsamples) {

      // split pyramid into eight equal volumes for verification
      int[] counts = new int[8];

      Point3d s = new Point3d();
      for (int i=0; i<nsamples; ++i) {
         sampleWedge(s);

         // separate into equal subvolumes
         if (s.z < 0) {
            if (s.x + s.y < 0.5) {
               ++counts[0];
            } else if (s.x > 0.5) {
               ++counts[1];
            } else if (s.y > 0.5) {
               ++counts[2];
            } else {
               ++counts[3];
            }
         } else {
            if (s.x + s.y < 0.5) {
               ++counts[4];
            } else if (s.x > 0.5) {
               ++counts[5];
            } else if (s.y > 0.5) {
               ++counts[6];
            } else {
               ++counts[7];
            }
         }
      }

      System.out.println("Wedge volumes:");
      for (int i=0; i<counts.length; ++i) {
         System.out.println("Volume " + i + ": = " + 1.0*counts[i]/nsamples);
      }
   }
   
   public static void main(String[] args) {
      tetTest(1000000);
      pyramidTest(1000000);
      wedgeTest(1000000);
   }

}
