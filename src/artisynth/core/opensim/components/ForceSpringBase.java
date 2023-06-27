package artisynth.core.opensim.components;

import java.io.File;

import artisynth.core.materials.AxialMaterial;
import artisynth.core.mechmodels.FrameMarker;
import artisynth.core.mechmodels.MultiPointSpring;
import artisynth.core.mechmodels.PointSpringBase;
import artisynth.core.mechmodels.PointList;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.Wrappable;
import artisynth.core.mechmodels.*;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.RenderableComponentList;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.render.RenderProps;

public abstract class ForceSpringBase extends ForceBase {

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
      return new MultiPointSpring (name);
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
      PointList<FrameMarker> markers =
         new PointList<>(FrameMarker.class, pathname);
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
            System.err.println("Failed to find body " + bodyOrSocketParentFrame);
            return null;
         }
         
         // make sure marker name is unique
         if (name != null) {
            int idx = 0;
            String pname = name;
            FrameMarker marker = markers.get (name);
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
               mps.addWrappable (wrappable);
               System.out.println (mps.getName() + " " + wrappable.getName());
            }
         }
         mps.setDrawABPoints (true);
         mps.setDrawKnots (false);

         // add markers to multipoint spring
         FrameMarker mprev = null;
         for (int i=0; i<markers.size(); ++i) {
            // add wrap segment if wrappables are present and frame markers are
            // on different bodies or are movable
            FrameMarker mi = markers.get(i);
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
                  mprev.getPosition(), mi.getPosition(), mps, numknots);
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

   /**
    * Test a wrap segment to see if its initial straight line between p0 and p1
    * penetrates any of the wrappables. If it does, return some initialization
    * points to insert between p0 and p1 to create a piecewise linear path that
    * reduces or eliminates the penetration. If the segment does not penetrate
    * any of the wrappables, return null.
    */
   Point3d[] computeInitialPoints (
      Point3d p0, Point3d p1, MultiPointSpring mps, int numknots) {

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
         p.scaledAdd ((k+1)/(numknots+1.0), u, p0);
         for (int i=0; i<mps.numWrappables(); i++) {
            Wrappable wrappable = mps.getWrappable(i);
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
      if (mind < 0) {
         Point3d px = new Point3d();
         px.scaledAdd (-mind, mindNrml, mindPos);
         return new Point3d[] { px };
      }
      else {
         return null;
      }
   }

}
