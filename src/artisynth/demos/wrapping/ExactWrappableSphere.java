package artisynth.demos.wrapping;

import java.util.ArrayList;

import artisynth.core.mechmodels.Frame;
import artisynth.core.mechmodels.Point;
import maspack.geometry.QuadraticUtils;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;

/**
 * A sphere used for excat wrapping. 
 * @author Omar
 */
public class ExactWrappableSphere extends ExactWrappableGeometry 
   implements ParametricEntity {
   protected double radius;             // Radius of this sphere.
   protected double rsq;                // Radius squared.
   protected double threshRat;          // Threshold ratio for breaking contact.
   protected int NUM_DRAW_POINTS = 50;  // Number of surface points to create
                                        // for drawing. 
   
   /**
    * Create a new wrapping sphere in the given parent frame, with the given
    * center location and radius.
    * @param iparent    parent frame
    * @param cInParent  location of the center (parent frame)
    * @param R          radius of this sphere
    */
   public ExactWrappableSphere(Frame iparent, Point3d cInParent, double R) {
      parent = iparent;
      toParent = new RigidTransform3d(cInParent.x, cInParent.y, cInParent.z);
      radius = R;
      rsq = R * R;
      threshRat = 0.95;
   }
   
   /**
    * Set the threshold ratio for breaking contact.
    * @param tr         new ratio for breaking contact
    */
   public void setThreshRat(double tr) {
      threshRat = tr;
   }
   
   /**
    * Get the threshold ratio for breaking contact.
    * @return           threshold ratio for breaking contact
    */
   public double getThreshRat() {
      return threshRat;
   }

   /**
    * Find a path between the two points that wraps around this object.
    * See documentation for explanation of algorithm.
    * @param fm1        first point
    * @param fm2        second point
    * @return           the path
    */
   public ExactWrapPath wrap (Point fm1, Point fm2) {      
      return wrap (fm1, fm2, null);
   }

   double angularDistance (double ang1, double ang2) {
      double dist;
      if (ang1 > 0) {
         // ang2 will be negative
         dist = ((ang2+2*Math.PI) - ang1);
      }
      else {
         // ang2 will be positive
         dist = (ang1 - (ang2-2*Math.PI));
      }
      if (dist < 0) {
         dist += 2*Math.PI;
      }
      return dist;
   }

   double computeAngle (Point3d pt, Point3d px) {
      double ang = Math.atan2 (pt.y, pt.x);
      Vector3d pxt = new Vector3d();
      pxt.sub (pt, px);
      // set sign of angle to match the desired wrapping direction
      if (pt.x*pxt.y - pt.y*pxt.x >= 0) {
         // angle should be positive
         if (ang < 0) {
            ang += 2*Math.PI;
         }
      }
      else {
         // angle should be negative
         if (ang > 0) {
            ang -= 2*Math.PI;
         }
      }
      return ang;      
   }

   double[] computeTangents (Point3d[] pts, Point3d px, double r) {
      pts[0] = new Point3d();
      pts[1] = new Point3d();
      QuadraticUtils.circleTangentPoints (pts[0], pts[1], px, r);
      double[] angs = new double[2];
      angs[0] = computeAngle (pts[0], px);
      angs[1] = computeAngle (pts[1], px);
      return angs;
   }

   Point3d computeMidAnglePoint (double ang1, double ang2) {
      double ang = (ang1 + ang2)/2 + Math.PI;
      return new Point3d (radius*Math.cos(ang), radius*Math.sin(ang), 0);
   }

   private double dist (
      ArrayList<Point3d> pnts, Point3d p1, Point3d p2) {
      return (pnts.get(0).distance(p1) + pnts.get(1).distance(p2));
   }
   
   public ExactWrapPath wrap (Point fm1, Point fm2, ExactWrapPath prev) {
      // Copy the points
      RigidTransform3d TSW = getTransformToWorld();
      Point3d p1 = new Point3d(fm1.getPosition());
      Point3d p2 = new Point3d(fm2.getPosition());
      // transform everything into a frame whose x/y axis aligns
      // with the plane formed by p1, p2 and the sphere center
      p1.sub (TSW.p);
      p2.sub (TSW.p);
      Vector3d zdir = new Vector3d();
      zdir.cross (p1, p2);
      RigidTransform3d TPW = new RigidTransform3d();
      TPW.R.setZDirection (zdir);
      TPW.p.set (TSW.p);
      p1.inverseTransform(TPW.R);
      p2.inverseTransform(TPW.R);

      double mag;
      // project points to the surface if necessary
      if ((mag=p1.norm()) < radius) {
         p1.scale (radius/mag);
      }
      if ((mag=p2.norm()) < radius) {
         p2.scale (radius/mag);
      }

      // compute tangent points and angles
      Point3d[] pnts1 = new Point3d[2];
      double[] angs1 = computeTangents (pnts1, p1, radius);
      Point3d[] pnts2 = new Point3d[2];
      double[] angs2 = computeTangents (pnts2, p2, radius);

      // matching angles must be opposite
      if (angs1[0]*angs2[0] > 0) {
         // switch
         double atmp  = angs2[0]; angs2[0] = angs2[1]; angs2[1] = atmp;
         Point3d ptmp = pnts2[0]; pnts2[0] = pnts2[1]; pnts2[1] = ptmp;
      }
      double[] adist = new double[] {
         angularDistance (angs1[0], angs2[0]),
         angularDistance (angs1[1], angs2[1]) 
      };

      ArrayList<Point3d> prevPnts = null;
      if (prev != null) {
         ArrayList<Point3d> pnts = prev.getAllABPoints();
         if (pnts.size() == 0) {
            pnts.add (prev.getRefPoint());
            pnts.add (prev.getRefPoint());
         }
         prevPnts = new ArrayList<Point3d>();
         for (Point3d p : pnts) {
            Point3d px = new Point3d(p);
            px.inverseTransform (TPW);
            prevPnts.add (px);
         }
      }

      int tidx = 0;

      if (adist[0] < Math.PI && adist[1] < Math.PI) {
         //System.out.println ("wrap 0, wrap 1");
         if (prevPnts != null) {
            // System.out.println (
            //    "dist=" +
            //    dist (prevPnts, pnts1[0], pnts2[0]) + " " +
            //    dist (prevPnts, pnts1[1], pnts2[1]));
            // System.out.println ("prevP1=    "+prevPnts.get(0).toString("%10.5f"));
            // System.out.println ("prevP2=    "+prevPnts.get(1).toString("%10.5f"));
            // System.out.println ("pnts1[0]=  "+pnts1[0].toString("%10.5f"));
            // System.out.println ("pnts2[0]=  "+pnts2[0].toString("%10.5f"));
            // System.out.println ("pnts1[1]=  "+pnts1[1].toString("%10.5f"));
            // System.out.println ("pnts2[1]=  "+pnts2[1].toString("%10.5f"));
            if (dist (prevPnts, pnts1[0], pnts2[0]) >
                dist (prevPnts, pnts1[1], pnts2[1])) {
               tidx = 1;
            }
         }
         else {
            if (adist[0] > adist[1]) {
               tidx = 1;
            }
         }
      }
      else if (adist[0] < adist[1]) {
         //System.out.println ("wrap 0, line 1");
         // assume wrapping for index 0, straight line for 1
         Point3d midPnt = computeMidAnglePoint (angs1[1], angs2[1]);
         if (prevPnts != null) {
            if (dist (prevPnts, pnts1[0], pnts2[0]) >
                dist (prevPnts, midPnt, midPnt)) {
               tidx = 1;
            } 
         }
         else {
            tidx = 1;
         }
         if (tidx == 1) {
            return createLinePath (fm1, fm2, midPnt, TPW);
         }
      }
      else {
         //System.out.println ("wrap 1, line 0");
         // assume wrapping for index 1, straight line for 0
         Point3d midPnt = computeMidAnglePoint (angs1[0], angs2[0]);
         if (prevPnts != null) {
            if (dist (prevPnts, midPnt, midPnt) >
                dist (prevPnts, pnts1[1], pnts2[1])) {
               tidx = 1;
            } 
         }
         else {
            tidx = 0;
         }
         if (tidx == 0) {
            return createLinePath (fm1, fm2, midPnt, TPW);
         }
      }

      double len0 = pnts1[tidx].distance(p1);
      double len1 = adist[tidx]*radius;
      double len2 = pnts2[tidx].distance(p2);
      
      // Transform the points to their parents' frames.
      Point3d pathPoints[] = new Point3d[4];
      pathPoints[0] = new Point3d(fm1.getPosition());
      pathPoints[1] = new Point3d(pnts1[tidx]);
      pathPoints[1].transform (TPW);
      pathPoints[2] = new Point3d(pnts2[tidx]);        
      pathPoints[2].transform (TPW);
      pathPoints[3] = new Point3d(fm2.getPosition());

      // Compute the tangents.
      Vector3d tangents[] = new Vector3d[4];
      tangents[0] = new Vector3d(pathPoints[1]);
      tangents[0].sub(pathPoints[0]);
      tangents[0].normalize();
      tangents[1] = new Vector3d(tangents[0]);
      tangents[2] = new Vector3d(pathPoints[3]);
      tangents[2].sub(pathPoints[2]);
      tangents[2].normalize();
      tangents[3] = new Vector3d(tangents[2]);

       // Create the wrap path.
      ExactWrapPath path = new ExactWrapPath(pathPoints, tangents, 4, false);
      path.getSegment(1).setLength(len1);
      path.getSegment(1).setStraight(false);
      path.getPoint(1).setCoordinates(
         new double[] {angs1[tidx], pathPoints[1].z});
      path.getPoint(2).setCoordinates(
         new double[] {angs2[tidx], pathPoints[2].z});
      
      // Set straight segment lengths
      path.getSegment(0).setLength (len0);
      path.getSegment(2).setLength (len2);
      
      // Add the points to draw.
      path.addDrawPoint(fm1.getPosition());
      double delAng = adist[tidx] / NUM_DRAW_POINTS;
      if (angs1[tidx] < 0) {
         delAng = -delAng;
      }
      for(int i=0; i <= NUM_DRAW_POINTS; i++) {
         double ang = angs1[tidx] + i*delAng;
         Point3d p = new Point3d (radius*Math.cos(ang), radius*Math.sin(ang), 0);
         p.transform(TPW);
         path.addDrawPoint(p);
      }
      path.addDrawPoint(fm2.getPosition());      

      return path;
   }

   ExactWrapPath createLinePath (
      Point fm1, Point fm2, Point3d midPnt, RigidTransform3d TCW) {

      Point3d pathPoints[] = new Point3d[2];
      pathPoints[0] = new Point3d(fm1.getPosition());
      pathPoints[1] = new Point3d(fm2.getPosition());

      // Compute the tangents.
      Vector3d tangents[] = new Vector3d[2];
      tangents[0] = new Vector3d(pathPoints[1]);
      tangents[0].sub(pathPoints[0]);
      tangents[0].normalize();
      tangents[1] = new Vector3d(tangents[0]);

       // Create the wrap path.
      ExactWrapPath path = new ExactWrapPath(pathPoints, tangents, 2, false);
      Point3d midPntW = new Point3d();
      midPntW.transform (TCW, midPnt);
      path.setRefPoint (midPntW);
      
      // Set straight segment lengths
      path.getSegment(0).setLength (pathPoints[0].distance (pathPoints[1]));
      
      // Add the points to draw.
      path.addDrawPoint(fm1.getPosition());
      path.addDrawPoint(fm2.getPosition());      

      return path;
   }

   /**
    * Get location of the point described by the given parameters, in some body
    * coordinate frame.
    * @param params     parameters
    * @return           location of the point with the given parameters
    */
   public Point3d getLocation(double[] params) {
      Point3d toRet = new Point3d();
      double temp = Math.cos(params[1]);
      toRet.x = radius * temp * Math.cos(params[0]);
      toRet.y = radius * temp * Math.sin(params[0]);
      toRet.z = radius * Math.sin(params[1]);
      return toRet;
   }

   /**
    * Get position of the point described by the given parameters, in world
    * coordinates.
    * @param params     parameters
    * @return           position of the point with the given parameters.
    */
   public Point3d getPosition(double[] params) {
      Point3d loc = getLocation(params);
      loc.transform(toParent);
      loc.transform(parent.getPose());
      return loc;
   }
}
