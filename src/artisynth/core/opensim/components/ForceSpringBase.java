package artisynth.core.opensim.components;

import java.util.ArrayList;
import java.io.File;

import artisynth.core.materials.AxialMaterial;
import artisynth.core.mechmodels.FrameMarker;
import artisynth.core.mechmodels.Marker;
import artisynth.core.mechmodels.MultiPointSpring;
import artisynth.core.mechmodels.PointSpringBase;
import artisynth.core.mechmodels.PointList;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.Wrappable;
import artisynth.core.mechmodels.*;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.RenderableComponentList;
import artisynth.core.opensim.OpenSimParser;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.render.RenderProps;
import maspack.util.InternalErrorException;

public abstract class ForceSpringBase extends ForceBase {

   public static boolean useMuscleComponents = false;
   public static boolean useOldWrapPathInitialization = false;
   
   // Stores points through which actuator passes
   private GeometryPath geometryPath; 
   
   public ForceSpringBase() {
      geometryPath = null;
   }
   
   public GeometryPath getGeometryPath() {
      return geometryPath;
   }
   
   public void setGeometryPath(GeometryPath gp) {
      geometryPath = gp;
      geometryPath.setParent (this);
   }

   @Override
   public ForceSpringBase clone () {
      ForceSpringBase forcePath = (ForceSpringBase) super.clone ();
      if (geometryPath != null) {
         forcePath.setGeometryPath (geometryPath.clone ());
      }
      return forcePath;
   }
   
   protected abstract AxialMaterial createMaterial();
   
   protected MultiPointSpring createDefaultMultiSpring (String name) {
      //return new OpenSimMultiSpring (name);
      if (OpenSimParser.getMusclesContainPathPoints()) {
         return new MultiPointMuscleOsim (name);
      }
      else {
         return new MultiPointSpring (name);         
      }
   }
   
   protected AxialSpring createDefaultSpring (String name) {
      return new AxialSpring (name);
   }
   
   private static int getNumWrapPoints(FrameMarker p0, FrameMarker p1) {
      double d = p0.getPosition ().distance (p1.getPosition ());
      return (int)Math.round (d/0.002); // XXX set density automatically
   }

   @Override
   public ModelComponent createComponent (
      File geometryPath, ModelComponentMap componentMap) {
      
      if (OpenSimParser.getMusclesContainPathPoints()) {
         return createComponentWithOsimMuscles (geometryPath, componentMap);
      }
      else {
         return createComponentLegacy (geometryPath, componentMap);
      }
   }
   
   public ModelComponent createComponentLegacy (
      File geometryPath, ModelComponentMap componentMap) {
     
      GeometryPath path = getGeometryPath ();
      PathPointSet pps = path.getPathPointSet ();
      
      RenderProps grprops = path.createRenderProps (); // geometry render props
    
      String mname = getName();
      RenderableComponentList<ModelComponent> ff =
         new RenderableComponentList<ModelComponent>(ModelComponent.class, mname);
      componentMap.put (this, ff);
      
      String pathname = "path";
      if (mname != null) {
         pathname = mname + "_path";
      }
      PointList<Marker> markers = new PointList<>(Marker.class, pathname);
      ff.add (markers);
      
      // create markers from path points
      for (PathPoint pp : pps) {
         String bodyOrSocketParentFrame = pp.getBodyOrSocketParentFrame ();
         Point3d loc = pp.getLocation ();
         String name = pp.getName ();
         
         // get rigid body
         PhysicalFrame body = componentMap.findObjectByPathOrName (
            Body.class, this, bodyOrSocketParentFrame);
         if (body == null) { // try ground
            body = componentMap.findObjectByPathOrName (
               Ground.class, this, bodyOrSocketParentFrame);
         }
         RigidBody rb = (RigidBody)componentMap.get (body);
         if (rb == null) {
            System.err.println(
               "OpenSimParser: failed to find body " + bodyOrSocketParentFrame);
            return null;
         }
         
         // make sure marker name is unique
         if (name != null) {
            int idx = 0;
            String pname = name;
            Marker marker = markers.get (name);
            while (marker != null) {
               ++idx;
               pname = name + idx;               
               marker = markers.get (pname);
            }
            name = pname;
         }
         
         // add frame marker
         FrameMarker fm = null;
         if (pp instanceof MovingPathPoint) {
            MovingPathPoint mpp = (MovingPathPoint)pp;
            //fm = MovingFrameMarker.create (name, mpp, mpp, componentMap);
            fm = mpp.createComponent (geometryPath, componentMap);
            fm.setName (name);
         }
         else if (pp instanceof ConditionalPathPoint) {
            ConditionalPathPoint cpp = (ConditionalPathPoint)pp;
            //fm = ConditionalFrameMarker.create (name, cpp, this, componentMap);
            fm = cpp.createComponent (geometryPath, componentMap);
            fm.setName (name);
            if (fm != null) {
               fm.setLocation (loc);
            }
         }
         if (fm == null) {
            fm = new FrameMarker (name);
            fm.setLocation (loc);
         }
         fm.setFrame (rb);
         
         markers.add (fm);
      }

      // get wrap path, if any
      PathWrapSet wrapPath = path.getPathWrapSet ();
      if (useMuscleComponents && wrapPath != null && wrapPath.size() == 0) {
         // will force using Muscle if no wrap path and only two markers
         wrapPath = null;
      }

      // create the muscle or spring
      PointSpringBase spr;
      if (wrapPath != null || markers.size() > 2) {
         // need a multipoint spring or muscle
         MultiPointSpring mps = createDefaultMultiSpring (getName());
         ff.add (mps);

         // add wrappables, if any
         if (wrapPath != null) {
            for (PathWrap pw : wrapPath) {
               String wrapObject = pw.getWrapObject ();
               WrapObject wo = componentMap.findObjectByName (
                  WrapObject.class, wrapObject);
               RigidBody wrappable = (RigidBody)componentMap.get (wo);
               int pntIdx0 = (int)pw.range.getLowerBound();
               int pntIdx1 = (int)pw.range.getUpperBound();
               if (pntIdx0 != -1) {
                  pntIdx0--; // zero indiced
               }
               if (pntIdx1 != -1) {
                  pntIdx1--; // zero indiced
               }
               mps.addWrappable (wrappable, pntIdx0, pntIdx1);
            }
         }
         mps.setDrawABPoints (true);
         mps.setDrawKnots (false);

         // add markers to multipoint spring
         FrameMarker mprev = null;
         for (int i=0; i<markers.size(); ++i) {
            // add wrap segment if wrappables are present and frame markers are
            // on different bodies or are movable
            FrameMarker mi = (FrameMarker)markers.get(i);
            if (mi instanceof JointBasedMovingMarker) {
               ((JointBasedMovingMarker)mi).updateMarkerLocation();
               JointBasedMovingMarker mm = (JointBasedMovingMarker)mi;
               // if (getName().equals ("DELT2") && mm.getName().equals ("default")) {
               //    System.out.println (
               //       "updated default: " + mm.getLocation());
               // }
            }
            if (mps.numWrappables() > 0 && mprev != null &&
                (mprev.getFrame () != mi.getFrame() ||
                 mi instanceof JointBasedMovingMarker)) {
               int numknots = getNumWrapPoints(mprev, mi);
               Point3d[] initialPnts = computeInitialPoints (
                  mprev.getPosition(), mi.getPosition(), mps, i, numknots);
               // if (mps.getName().equals ("LAT3")) {
               //    if (initialPnts != null) {
               //       System.out.println ("initial points for "+ i);               
               //       for (Point3d p : initialPnts) {
               //          System.out.println ("  " + p.toString("%8.3"));
               //       }
               //    }
               //    else {
               //       System.out.println ("initial points for "+ i + " NULL");
               //    }
               // }
               mps.setSegmentWrappable (numknots, initialPnts);
            }
            mps.addPoint (mi);
            mprev = mi;
         }
         //mps.updateStructure();
         if (wrapPath != null) {
            mps.updateWrapSegments();
         }
         spr = mps;
      }
      else {
         // use a two-point spring or muscle
         AxialSpring axs = createDefaultSpring (getName());
         ff.add (axs);

         axs.setFirstPoint (markers.get(0));
         axs.setSecondPoint (markers.get(1));

         spr = axs;
      }
      
      spr.setRestLengthFromPoints ();
      spr.setMaterial (createMaterial ());
      
      markers.setRenderProps (grprops);
      spr.setRenderProps (createRenderProps());
     
      return ff;
   }

   public ModelComponent createComponentWithOsimMuscles (
      File geometryPath, ModelComponentMap componentMap) {
     
      GeometryPath path = getGeometryPath ();
      PathPointSet pps = path.getPathPointSet ();
      
      RenderProps grprops = path.createRenderProps (); // geometry render props
    
      String mname = getName();
      MultiPointMuscleOsim mps =
         (MultiPointMuscleOsim)createDefaultMultiSpring (getName());
      componentMap.put (this, mps);

      PointList<Point> points = mps.getPathPoints();
      
      // create markers from path points
      for (PathPoint pp : pps) {
         String bodyOrSocketParentFrame = pp.getBodyOrSocketParentFrame ();
         Point3d loc = pp.getLocation ();
         String name = pp.getName ();
         
         // get rigid body
         PhysicalFrame body = componentMap.findObjectByPathOrName (
            Body.class, this, bodyOrSocketParentFrame);
         if (body == null) { // try ground
            body = componentMap.findObjectByPathOrName (
               Ground.class, this, bodyOrSocketParentFrame);
         }
         RigidBody rb = (RigidBody)componentMap.get (body);
         if (rb == null) {
            System.err.println(
               "OpenSimParser: failed to find body " + bodyOrSocketParentFrame);
            return null;
         }
         
         // make sure marker name is unique
         if (name != null) {
            int idx = 0;
            String pname = name;
            Point marker = points.get (name);
            while (marker != null) {
               ++idx;
               pname = name + idx;               
               marker = points.get (pname);
            }
            name = pname;
         }
         
         // add frame marker
         FrameMarker fm = null;
         if (pp instanceof MovingPathPoint) {
            MovingPathPoint mpp = (MovingPathPoint)pp;
            //fm = MovingFrameMarker.create (name, mpp, mpp, componentMap);
            fm = mpp.createComponent (geometryPath, componentMap);
            fm.setName (name);
         }
         else if (pp instanceof ConditionalPathPoint) {
            ConditionalPathPoint cpp = (ConditionalPathPoint)pp;
            //fm = ConditionalFrameMarker.create (name, cpp, this, componentMap);
            fm = cpp.createComponent (geometryPath, componentMap);
            fm.setName (name);
            if (fm != null) {
               fm.setLocation (loc);
            }
         }
         if (fm == null) {
            fm = new FrameMarker (name);
            fm.setLocation (loc);
         }
         fm.setFrame (rb);
         
         mps.addPathPoint (fm);
      }

      // get wrap path, if any
      PathWrapSet wrapPath = path.getPathWrapSet ();
      if (useMuscleComponents && wrapPath != null && wrapPath.size() == 0) {
         // will force using Muscle if no wrap path and only two markers
         wrapPath = null;
      }

      // add wrappables, if any
      if (wrapPath != null) {
         for (PathWrap pw : wrapPath) {
            String wrapObject = pw.getWrapObject ();
            WrapObject wo = componentMap.findObjectByName (
               WrapObject.class, wrapObject);
            RigidBody wrappable = (RigidBody)componentMap.get (wo);
            int pntIdx0 = (int)pw.range.getLowerBound();
            int pntIdx1 = (int)pw.range.getUpperBound();
            if (pntIdx0 != -1) {
               pntIdx0--; // zero indiced
            }
            if (pntIdx1 != -1) {
               pntIdx1--; // zero indiced
            }
            mps.addWrappable (wrappable, pntIdx0, pntIdx1);
         }
      }
      mps.setDrawABPoints (true);
      mps.setDrawKnots (false);

      // add markers to multipoint spring
      FrameMarker mprev = null;
      for (int i=0; i<points.size(); ++i) {
         // add wrap segment if wrappables are present and frame markers are
         // on different bodies or are movable
         FrameMarker mi = (FrameMarker)points.get(i);
         if (mi instanceof JointBasedMovingMarker) {
            ((JointBasedMovingMarker)mi).updateMarkerLocation();
            JointBasedMovingMarker mm = (JointBasedMovingMarker)mi;
         }
         if (mps.numWrappables() > 0 && mprev != null &&
             (mprev.getFrame () != mi.getFrame() ||
              mi instanceof JointBasedMovingMarker)) {
            int numknots = getNumWrapPoints(mprev, mi);
            Point3d[] initialPnts = computeInitialPoints (
               mprev.getPosition(), mi.getPosition(), mps, i, numknots);
            mps.setSegmentWrappable (numknots, initialPnts);
         }
         mps.addPoint (mi);
         mprev = mi;
      }
      //mps.updateStructure();
      if (wrapPath != null) {
         mps.updateWrapSegments();
      }
      
      mps.setRestLengthFromPoints ();
      mps.setMaterial (createMaterial ());
      
      points.setRenderProps (grprops);
      mps.setRenderProps (createRenderProps());
     
      return mps;
   }

   /**
    * Test a wrap segment to see if its initial straight line between p0 and p1
    * penetrates any of the wrappables. If it does, return some initialization
    * points to insert between p0 and p1 to create a piecewise linear path that
    * reduces or eliminates the penetration. If the segment does not penetrate
    * any of the wrappables, return null.
    */
   Point3d[] computeInitialPointsOld (
      Point3d p0, Point3d p1, MultiPointSpring mps, int segIdx, int numknots) {

      Vector3d u = new Vector3d(); // direction vector from p0 to p1
      u.sub (p1, p0);
      u.normalize();

      // Interpolate along p0-p1 at a resolution given by the number of knots.

      // At present, just find the point of maximum penetration, and create a
      // single initial point based on trying to move that point out past the
      // surface based on the distance to the surface and the corresponding
      // normal
      Point3d p = new Point3d();
      Vector3d nrml = new Vector3d();
      // maximum penetration occurs at the minimum penetration distance < 0
      double mind = 0;
      Vector3d mindNrml = new Vector3d();
      Point3d mindPos = new Point3d();
      for (int k=0; k<numknots; k++) {
         double s = (k+1)/(numknots+1.0);
         p.combine (1-s, p0, s, p1);
         for (int i=0; i<mps.numWrappables(); i++) {
            Wrappable wrappable = mps.getWrappable(i);
            int[] pntIdxs = mps.getWrappableRange(i);
            // check if this wrappable actually applies to this segment
            if (pntIdxs[0] == -1 ||
                (pntIdxs[0] < segIdx && segIdx <= pntIdxs[1])) {
               double d = wrappable.penetrationDistance (nrml, null, p);
               if (d < 0) {
                  if (wrappable instanceof RigidTorus) {
                     // special case: set the target point to the center of the
                     // torus to make sure we thread it
                     nrml.sub (wrappable.getPose().p, p);
                     d = -nrml.norm();
                     nrml.normalize();
                  }
                  if (d < mind) {
                     mind = d;
                     mindNrml.set (nrml);
                     mindPos.set (p);
                  }
               }
            }
         }
      }
      if (mind < 0) {
         Point3d px = new Point3d();
         px.scaledAdd (-mind, mindNrml, mindPos);
         return new Point3d[] { px };
      }
      else {
         return null;
      }
   }

   /**
    * If an interection location range overlaps the interval [0,1], return the
    * location in [0,1] that (likely) corresponds to the deepest point of
    * intersection.  Otherwise, return -1.
    */
   private double findDeepestIsectLocation (double[] srng) {
      if (srng[0] < 1 && srng[1] > 0) {
         // overlap. find deepest intersection location on the interval
         if (srng[0] > 0 || srng[1] < 1) {
            // proper overlap - return midpoint
            return (srng[0] + srng[1])/2;
         }
         else if (srng[0] > 0) {
            return srng[1];
         }
         else if (srng[1] < 1) {
            return srng[0];
         }
         else {
            // really shouldn't happen - just return midpoint
            return 0.5;
         }
      }
      return -1;
   }

   /**
    * Checks if a wrap segment intersects a given wrappable, and if it does,
    * returns an initializing point to help the segment avoid the wrappable.
    */
   private InitializingPoint wrappableIntersectsSegment (
      Point3d p0, Point3d p1, int numknots, Wrappable wrappable) {

      // Line segment is parameterized by s, such that points on the segment
      // are given by p = (1-s)*p0 + s*p1. Method works by finding the interval
      // srng = [s0, s1] over which s intersects the wrappable. If this
      // interval overlaps [0, 1], then we find the value of s that likely
      // gives the deepest penetration that we care about, and use that to
      // compute the initializing point.

      double[] srng = new double[2];
      boolean intersects = false;

      if (wrappable instanceof RigidTorus) {
         Point3d ps = new Point3d();
         for (int k=0; k<=numknots+1; k++) {
            double s = k/(numknots+1.0);
            ps.combine (1-s, p0, s, p1);
            if (wrappable.penetrationDistance (null, null, ps) < 0) {
               if (!intersects) {
                  // first intersect point
                  srng[0] = s;
               }
               srng[1] = s;
               intersects = true;
            }
         }
         if (intersects) {
            double s = findDeepestIsectLocation (srng);
            // use the center of the torus as the initialize point
            return new InitializingPoint (wrappable.getPose().p, s);
         }
      }
      else {
         if (wrappable instanceof RigidSphere) {
            intersects = ((RigidSphere)wrappable).intersectLine (srng, p0, p1);
         }
         else if (wrappable instanceof RigidCylinder) {
            intersects = ((RigidCylinder)wrappable).intersectLine (srng, p0, p1);
         }
         else if (wrappable instanceof RigidEllipsoid) {
            intersects = ((RigidEllipsoid)wrappable).intersectLine (srng, p0, p1);
         }
         if (intersects) {
            // Line defined by the segment intersects the wrappable.  Find the
            // s value likely associated with deepest penetration point.
            double s = findDeepestIsectLocation (srng);
            if (s != -1) {
               Vector3d nrml = new Vector3d();
               Point3d ps = new Point3d();
               ps.combine (1-s, p0, s, p1);
               double d = wrappable.penetrationDistance (nrml, null, ps);
               if (d <= 0) {
                  ps.scaledAdd (-d, nrml);
                  return new InitializingPoint (ps, s);
               }
               else {
                  throw new InternalErrorException (
                     "penetration distance is "+d+"; should be negative");
               }
            }
         }
      }
      return null;
   }

   /**
    * Stores an initializing point for a wrap segment, together with its
    * parametric location s along the segment.
    */
   private class InitializingPoint {
      
      Point3d pnt; // location of the point
      double s;    // place along the line segment, in the range [0,1]

      InitializingPoint (Vector3d pnt, double s) {
         this.s = s;
         this.pnt = new Point3d(pnt);
      }
   }

   /**
    * Test a wrap segment to see if its initial straight line between p0 and p1
    * penetrates any of the wrappables. If it does, return some initialization
    * points to insert between p0 and p1 to create a piecewise linear path that
    * reduces or eliminates the penetration. If the segment does not penetrate
    * any of the wrappables, return null.
    *
    * <p>At present, one initializing point is computed for each wrappable that
    * the wrap segment intersects. No attempt is made to ensure that the
    * piecewise linear curve formed from the initial points does not itself
    * interset the wrappables; it is assumed that the wrap segment collision
    * detection will resolve this correctly.
    */
   Point3d[] computeInitialPoints (
      Point3d p0, Point3d p1, MultiPointSpring mps, int segIdx, int numknots) {

      if (useOldWrapPathInitialization) {
         return computeInitialPointsOld (p0, p1, mps, segIdx, numknots);
      }

      Point3d[] ipnts = null; // no initialization points by default

      boolean printInitPoints = false;
      // For each wrappable that the line segment p0-p1 intersects, add an
      // initialization point to help avoid the wrappable.
      ArrayList<InitializingPoint> ipoints = new ArrayList<>();
      for (int wi=0; wi<mps.numWrappables(); wi++) {
         // check if the wrappable actually applies to this segment
         int[] pntIdxs = mps.getWrappableRange(wi);
         if (pntIdxs[0] == -1 ||
             (pntIdxs[0] < segIdx && segIdx <= pntIdxs[1])) {
            Wrappable wrappable = mps.getWrappable(wi);
            // get initializing point if wrappable intersects segment
            InitializingPoint ipnt =
               wrappableIntersectsSegment (p0, p1, numknots, wrappable);
            if (ipnt != null) {
               // add the initializing point at location ordered by its s value
               if (printInitPoints) {
                  System.out.printf (
                     "spring %s, adding initial point for %s at s=%g\n",
                     mps.getName(), ((RigidBody)wrappable).getName(), ipnt.s);
               }
               int i;
               for (i=0; i<ipoints.size(); i++) {
                  if (ipnt.s < ipoints.get(i).s) {
                     ipoints.add (i, ipnt);
                     break;
                  }
               }
               if (i == ipoints.size()) {
                  ipoints.add (ipnt);
               }
            }
         }
      }
      if (ipoints.size() > 0) {
         ipnts = new Point3d[ipoints.size()];
         for (int i=0; i<ipoints.size(); i++) {
            ipnts[i] = ipoints.get(i).pnt;
         }
      }
      return ipnts;
   }

}
