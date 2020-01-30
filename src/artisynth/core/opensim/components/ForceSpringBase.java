package artisynth.core.opensim.components;

import java.io.File;
import java.util.HashSet;

import artisynth.core.materials.AxialMaterial;
import artisynth.core.mechmodels.FrameMarker;
import artisynth.core.mechmodels.MultiPointSpring;
import artisynth.core.mechmodels.PointList;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.RenderableComponentList;
import maspack.matrix.Point3d;

public abstract class ForceSpringBase extends ForceBase {

   private GeometryPath geometryPath;           // Stores points through which actuator passes
   
   public ForceSpringBase() {
      setGeometryPath (new GeometryPath ());
   }
   
   public GeometryPath getGeometryPath() {
      return geometryPath;
   }
   
   public void setGeometryPath(GeometryPath gp) {
      geometryPath = gp;
      geometryPath.setParent (this);
   }
   
   public HashSet<String> getAttachedBodies() {
      HashSet<String> out = new HashSet<String>();
      
      for (PathPoint c : getGeometryPath ().pathPointSet.objects()) {
         out.add (c.getBody ());
      }
      
      return out;
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
    
      String mname = getName();
      RenderableComponentList<ModelComponent> ff = new RenderableComponentList<ModelComponent>(ModelComponent.class, mname);
      
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
         String bodyName = pp.getBody ();
         Point3d loc = pp.getLocation ();
         String name = pp.getName ();
         
         // get rigid body
         Body body = componentMap.findObjectByName (Body.class, bodyName);
         RigidBody rb = (RigidBody)componentMap.get (body);
         
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
      
      mps.setRenderProps (createRenderProps());
      
      componentMap.put (this, ff);
      
      return ff;
   }
}
