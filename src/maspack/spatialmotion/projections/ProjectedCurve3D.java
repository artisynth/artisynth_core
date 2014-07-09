package maspack.spatialmotion.projections;


public class ProjectedCurve3D {

   private static double EPSILON = 1e-15;
   private static int MAX_ITERS = 1000;
   private double myLength;
   private boolean lengthValid = false;
   
   private SphericalProjector projector;
   private BoundaryCurve2D curve;

   public ProjectedCurve3D(SphericalProjector projector, BoundaryCurve2D curve) {
      this.projector=projector;
      this.curve=curve;
   }

   public void getPoint(double t, double[] out) {

      double coord2d[] = new double[2];
      curve.getPoint(t, coord2d);
      projector.planeToSphere(coord2d[0], coord2d[1], out);

   }

   public void getTangent(double t, double[] out) {

      double coord2d[] = new double[2];
      double tangent2d[] = new double[2];
      double jacob[] = new double[6];

      curve.getTangent(t, tangent2d);
      curve.getPoint(t, coord2d);
      projector.getJacobian(coord2d[0], coord2d[1], jacob);

      out[0] = jacob[0]*tangent2d[0]+jacob[1]*tangent2d[1];
      out[1] = jacob[2]*tangent2d[0]+jacob[3]*tangent2d[1];
      out[2] = jacob[4]*tangent2d[0]+jacob[5]*tangent2d[1];

   }

   private static double chordLength(double [] p1, double [] p2) {
      double a = p1[0]-p2[0];
      double b = p1[1]-p2[1];
      double c = p1[2]-p2[2];

      return Math.sqrt(a*a+b*b+c*c);

   }

   private static void projectToTangentPlane(double [] vec, double [] p, double [] tangent) {

      // unit normal at p is p
      // project chord onto p
      double s = dot3(p, vec);

      // subtract normal component
      tangent[0] = vec[0]-s*p[0]; 
      tangent[1] = vec[1]-s*p[1];
      tangent[2] = vec[2]-s*p[2];
      // now, the chord is projected to the tangent plane at p

   }

   //   private static double normsq3(double [] a) {
   //      return a[0]*a[0]+a[1]*a[1]+a[2]*a[2];
   //   }

   private static double norm3(double [] a) {
      return Math.sqrt(a[0]*a[0]+a[1]*a[1]+a[2]*a[2]);
   }

   //   private static double normalize3(double [] a) {
   //      double norm = Math.sqrt(a[0]*a[0]+a[1]*a[1]+a[2]*a[2]);
   //      a[0] = a[0]/norm;
   //      a[1] = a[1]/norm;
   //      a[2] = a[2]/norm;
   //      return norm;
   //   }

   private static double dot3(double [] a, double [] b) {
      return a[0]*b[0] + a[1]*b[1] + a[2]*b[2]; 
   }

   private static void sub3(double [] a, double[] b, double[] out) {
      out[0] = a[0]-b[0];
      out[1] = a[1]-b[1];
      out[2] = a[2]-b[2];
   }

   private static void normalize3(double [] in, double [] out) {

      double norm = Math.sqrt(in[0]*in[0]+in[1]*in[1]+in[2]*in[2]);
      out[0] = in[0]/norm;
      out[1] = in[1]/norm;
      out[2] = in[2]/norm;

   }

   //   private static double cosAngle(double[] a, double [] b) {
   //      return dot3(a,b)/Math.sqrt(normsq3(a)*normsq3(b));
   //   }

   //   private double clip(double t, double min, double max) {
   //      if (t < min) {
   //         return min;
   //      } else if(t > max) {
   //         return max;
   //      }
   //      return t;
   //   }

   // use gradient decent to find bounds on curve parameter "t"
   // for closest point
   private void boundT(double [] in, double t, double [] tBounds) {

      double dtMaxStep = 0.1;

      double [] coords = new double[3];
      double [] tangent = new double[3];
      double [] disp = new double[3];
      double dd = 0;
      double d = 0;
      int iter=0;

      getPoint(t,coords);
      getTangent(t, tangent);
      sub3(coords, in, disp);
      dd = dot3(disp,tangent);   // d/dt ( || in - p(t) ||^2 )
      d = norm3(disp);
      double dMax = d;

      int a = 1;
      if (dd < 0) {
         tBounds[0]=t;
      } else {
         tBounds[1]=t;
         a=-1;
      }

      // cosine of intersection angle
      double cosa = dd/(norm3(tangent)*norm3(disp));

//      System.out.println("_");
//      System.out.printf("t=%f, d=%f, d/dt=%f  tang=(%f,%f,%f)\n",t,d,dd,tangent[0],tangent[1],tangent[2]);
      
      // find another bound
      while ( a*dd < 0 && iter < MAX_ITERS && Math.abs(dd) > EPSILON) {
         
         t -= dtMaxStep*cosa; // step less than distance
         getPoint(t,coords);
         getTangent(t, tangent);
         sub3(coords, in, disp);
         d = norm3(disp);
         iter++;
         
         // if we've jumped too far, backtrack and correct
         if (d > dMax) {
            t += dtMaxStep*cosa;
            dtMaxStep = dtMaxStep/2;
            continue;
         } else {
            dMax = d;
         }
         dd = dot3(disp,tangent);
         cosa = dd/(norm3(tangent)*norm3(disp));

//         System.out.printf("t=%f, d=%f, d/dt=%f  tang=(%f,%f,%f)\n",t,d,dd,tangent[0],tangent[1],tangent[2]);
      }

      if (Math.abs(d)<EPSILON) {
         tBounds[0] = t-EPSILON;
         tBounds[1] = t+EPSILON;
         return;
      }
      
      if (a > 0) {
         tBounds[1] = t;
      } else {
         tBounds[0] = t;
      }

   }

   public void getTangentDirection(double [] p1, double [] p2, double [] out) {
      sub3(p2, p1, out);
      projectToTangentPlane(out, p1, out);
   }

   public double getProjection(double [] in, double [] out, double[] axis) {

      double tangent[] = new double[3];
      findClosestPoint(in, out, tangent, axis);
      projectToTangentPlane(tangent, out, axis);
      cross3(out,in,axis);
      normalize3(axis,axis);

      return sphericalDistance(in, out);

   }

   private void cross3(double[] v1, double [] v2, double [] out) {
      double a = v1[1]*v2[2]-v1[2]*v2[1];
      double b = v1[2]*v2[0]-v1[0]*v2[2];
      double c = v1[0]*v2[1]-v1[1]*v2[0];
      out[0] = a;
      out[1] = b;
      out[2] = c;
   }

   public double findClosestPoint(double [] in, double [] out) {
      double tangent[] = new double[3];
      double normal[] = new double[3];
      findClosestPoint(in, out, tangent, normal);
      return sphericalDistance(in, out);
   }

   private double distsq3(double [] pnt1, double [] pnt2) {
      double a=pnt1[0]-pnt2[0];
      double b=pnt1[1]-pnt2[1];
      double c=pnt1[2]-pnt2[2];
      return a*a+b*b+c*c;
   }

   private void swap2(double [] a, double [] b) {
      double tmp = a[1];
      a[1]=a[0];
      a[0]=tmp;
      tmp = b[1];
      b[1]=b[0];
      b[0]=tmp;
   }

//   public double minNelderMead(double [] in, double [] out, double [] t) {
//
//      double [] f = new double[2];
//      int iter = 0;
//      double tr = 0;
//      double fr = 0;
//      double te = 0;
//      double fe = 0;
//
//      // compute squared distances and arrange in increasing order
//      getPoint(t[0], out);
//      f[0] = distsq3(in, out);
//      getPoint(t[1], out);
//      f[1] = distsq3(in,out);
//      if (f[0]>f[1]) {
//         swap2(t,f);
//      }
//
//
//      while ( Math.abs(t[0]-t[1]) > EPSILON && iter < MAX_ITERS) {
//         // reflection
//         tr = 2*t[0]-t[1];
//         getPoint(tr, out);
//         fr = distsq3(in,out);
//
//         // NOTE:  in 1D, no reflection stage      
//         //      if (fr >= f[0] && fr < f[0]) {
//         //         f[1] = fr;
//         //         t[1] = tr;
//         //         // continue;
//         //      } else 
//         if (fr < f[0]) {
//            // expansion         
//            te = 3*t[0]-2*t[1];
//            getPoint(te, out);
//            fe = distsq3(in,out);
//
//            f[1] = f[0];
//            t[1] = t[0];
//
//            if (fe < fr) {
//               f[0] = fe;
//               t[0] = te;
//            } else {
//               f[0] = fr;
//               t[0] = tr;
//            }
//         } else {
//
//            // contraction
//            if (fr < f[1]) {
//               te= (t[0] + tr)/2;
//            } else {
//               te = (t[0] + t[1])/2;
//               fr = f[1];
//            }
//            getPoint(te, out);
//            fe = distsq3(in,out);
//
//            if (fe < fr) {
//               f[1]=fe;
//               t[1]=te;
//               if (fe<f[0]) {
//                  swap2(t,f);
//               }
//            } else {
//               // shrink
//               t[1] = (t[0]+t[1])/2;
//               getPoint(t[1], out);
//               f[1]=distsq3(in,out);
//            }
//         }
//      }
//      getPoint(t[0], out);
//
//      return t[0];
//   }

   // returns great circle distance
   // falls into local minimum trap
   public void findClosestPoint(double [] in, double[] out, double [] tangent, double [] normal) {

      double [] tBounds = new double[2];

      // get an initial guess and the corresponding tangent 
      projector.sphereToPlane(in[0], in[1], in[2], out);
      curve.projectToBoundary(out, out);
      double t = curve.getTVar(out[0], out[1]);

      boundT(in,t, tBounds);

      // bisection method
      t = (tBounds[0]+tBounds[1])/2;
      getPoint(t,out);
      getTangent(t, tangent);      
      sub3(out, in, normal);
      double dd = dot3(normal,tangent);
      int iter = 0;

      while ( Math.abs(dd) > EPSILON && (tBounds[1]-tBounds[0]) >EPSILON && iter < MAX_ITERS) {
         t = (tBounds[0]+tBounds[1])/2;
         getPoint(t,out);
         getTangent(t, tangent);      
         sub3(out, in, normal);
         dd = dot3(normal,tangent);

         if (dd <= 0) {
            tBounds[0] = t;
         } else {
            tBounds[1] = t;
         }

         iter++;
      }

      // project normal to tangent plane
      projectToTangentPlane(normal, out, normal);

   }

   public boolean isWithin(double [] in) {
      double [] coords2d = new double[2];
      projector.sphereToPlane(in[0], in[1], in[2], coords2d);
      return curve.isWithin(coords2d[0], coords2d[1]);
   }

   public static double sphericalDistance(double[] pnt1, double[] pnt2) {

      double c = chordLength(pnt1, pnt2)/2;
      // trim for stability
      if (c > 1) {
         c = 1;
      }
      return 2*Math.asin(c);

   }

   public void setCurve(BoundaryCurve2D curve) {
      this.curve = curve;
      lengthValid = false;
   }

   public BoundaryCurve2D getCurve() {
      return curve;
   }

   public void setProjector(SphericalProjector projector) {
      this.projector=projector;
      lengthValid = false;
   }

   public SphericalProjector getProjector() {
      return projector;
   }
   
   public double computeLength() {
      
      double dt = 0.01;
      double [] p1 = new double[3];
      double [] p2 = new double[3];
      myLength = 0;
      
      getPoint(0,p1);
      for (double t=0; t<1; t=t+dt) {
         getPoint(t+dt,p2);
         myLength += Math.sqrt(distsq3(p1, p2));
         p1[0]=p2[0];
         p1[1]=p2[1];
         p1[2]=p2[2];
      }
      return myLength;
   }
   
   // might be impossible to tell if length is valid
   public double getLength() {
      
      if (lengthValid) {
         return computeLength();
      }
      return myLength;
   }

}
