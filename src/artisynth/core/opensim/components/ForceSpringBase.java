package artisynth.core.opensim.components;

import java.io.File;

import artisynth.core.materials.AxialMaterial;
import artisynth.core.mechmodels.FrameMarker;
import artisynth.core.mechmodels.MultiPointSpring;
import artisynth.core.mechmodels.PointList;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.RenderableComponentList;
import maspack.matrix.Point3d;
import maspack.render.RenderProps;

public abstract class ForceSpringBase extends ForceBase {

   private GeometryPath geometryPath;           // Stores points through which actuator passes
   
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
   
   protected MultiPointSpring createDefaultSpring() {
      return new MultiPointSpring ();
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
      RenderableComponentList<ModelComponent> ff = new RenderableComponentList<ModelComponent>(ModelComponent.class, mname);
      componentMap.put (this, ff);
      
      String pathname = "path";
      if (mname != null) {
         pathname = mname + "_path";
      }
      PointList<FrameMarker> markers = new PointList<>(FrameMarker.class, pathname);
      ff.add (markers);
      
      MultiPointSpring mps = createDefaultSpring();
      
      if (mname != null) {
         mps.setName (mname);
      } else {
         mps.setName ("force");
      }
      ff.add (mps);
      
      for (PathPoint pp : pps) {
         String bodyOrSocketParentFrame = pp.getBodyOrSocketParentFrame ();
         Point3d loc = pp.getLocation ();
         String name = pp.getName ();
         
         // get rigid body
         Body body = componentMap.findObjectByPathOrName (Body.class, this, bodyOrSocketParentFrame);
         RigidBody rb = (RigidBody)componentMap.get (body);
         
         if (rb == null) {
            System.err.println("Failed to find body " + bodyOrSocketParentFrame);
            return null;
         }
         
         // add frame marker
         FrameMarker fm = new FrameMarker (name);
         fm.setFrame (rb);
         fm.setLocation (loc);
         // XXX deal with duplicate names
         
         if (name != null) {
            int idx = 0;
            String pname = name;
            FrameMarker marker = markers.get (name);
            while (marker != null) {
               ++idx;
               pname = name + idx;               
               marker = markers.get (pname);
            }
            fm.setName (pname);
         }
         markers.add (fm);
         
         // add point to muscle
         mps.addPoint (fm);
      }
      
      // add wrap segments
      PathWrapSet wrapPath = path.getPathWrapSet ();
      if (wrapPath != null) {
         for (PathWrap pw : wrapPath) {
            String wrapObject = pw.getWrapObject ();
            WrapObject wo = componentMap.findObjectByName (WrapObject.class, wrapObject);
            RigidBody wrappable = (RigidBody)componentMap.get (wo);
            wrappable.setRenderProps (grprops);
            
            for (int i=0; i<markers.size ()-1; ++i) {
               // add wrap segment if frame markers are on different bodies
               FrameMarker mi = markers.get (i);
               FrameMarker mj = markers.get (i+1);
               if (mi.getFrame () != mj.getFrame ()) {
                  int numknots = getNumWrapPoints(mi, mj);
                  mps.setSegmentWrappable (numknots);
                  mps.setDrawABPoints (true);
                  mps.setDrawKnots (false);
                  mps.addWrappable (wrappable);
               }
            }
            
         }
      }
      
      mps.setRestLengthFromPoints ();
      mps.setMaterial (createMaterial ());
      
      markers.setRenderProps (grprops);
      mps.setRenderProps (createRenderProps());
     
      
      return ff;
   }
}
