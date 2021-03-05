package artisynth.core.workspace;

import artisynth.core.driver.Main;
import artisynth.core.femmodels.FemMarker;
import artisynth.core.femmodels.FemMeshComp;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.SkinMeshBody;
import artisynth.core.mechmodels.Frame;
import artisynth.core.mechmodels.GenericMarker;
import artisynth.core.mechmodels.HasSurfaceMesh;
import artisynth.core.mechmodels.IsMarkable;
import artisynth.core.mechmodels.Marker;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.Point;
import artisynth.core.mechmodels.PointAttachable;
import artisynth.core.mechmodels.PointAttachment;
import artisynth.core.mechmodels.PointList;
import artisynth.core.mechmodels.RigidMeshComp;
import artisynth.core.modelbase.ModelComponent;
import maspack.geometry.BVFeatureQuery;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.Line;
import maspack.matrix.Point3d;

public class AddMarkerHandler {

   /**
    * Intersects a component containing surface meshes with a ray
    * @param nearest nearest point on surfaces
    * @param comp component to intersect
    * @param ray ray to intersect with
    * @return true if intersects
    */
   private boolean computeRayIntersection (
      Point3d nearest, HasSurfaceMesh comp, Line ray) {
      
      PolygonalMesh[] meshes = comp.getSurfaceMeshes();
      if (meshes == null || meshes.length == 0) {
         return false;
      }
      
      double nearestDistance = Double.POSITIVE_INFINITY;
      for (PolygonalMesh mesh : meshes) {
         Point3d pos = BVFeatureQuery.nearestPointAlongRay (
            mesh, ray.getOrigin(), ray.getDirection());
         if (pos != null) {
            double d = pos.distance(ray.getOrigin());
            if (d < nearestDistance) {
               nearestDistance = d;
               nearest.set (pos);
            }
         }
      }
      return nearestDistance != Double.POSITIVE_INFINITY;
   }

   private Point3d computeMarkPosition (ModelComponent comp, Line ray) {
     
      Point3d out = null;
      if (comp instanceof HasSurfaceMesh) {
         Point3d isect = new Point3d();
         if (computeRayIntersection (isect, (HasSurfaceMesh)comp, ray)) {
            out = isect;
         }
      }
      else if (comp instanceof Point) {
         out = ((Point)comp).getPosition ();
      }
      else if (comp instanceof Frame) {
         Frame frame = (Frame)comp;
         out = new Point3d(frame.getPose().p);    
      }
      
      return out;
      
   }
   
   private MechModel findMechModel(ModelComponent c) {
      ModelComponent parent = c;
      while (parent != null && !(parent instanceof MechModel)) {
         parent = parent.getParent ();
      }
      
      return (MechModel)parent;
   }
   
   public Marker addMarker (ModelComponent comp, Line ray) {
      
      Marker marker = null;
      
      if (comp instanceof IsMarkable) {
         IsMarkable markable = (IsMarkable)comp;
         marker = markable.createMarker (ray);
         if (!markable.addMarker (marker)) {
            // put into a root list of markers
            PointList<Marker> markers = getDefaultMarkerList ();
            if (markers != null) {
               markers.add (marker);
            }
         }
      } else {
         
         Point3d isect = computeMarkPosition (comp, ray);
         
         // add marker to selected component
         if (isect != null) {
            System.out.println ("Add marker: " + (comp.getName () != null ? comp.getName () : comp) + " @ " + isect.toString ());
            
            if (comp instanceof Frame) {
               Frame frame = (Frame)comp;
               MechModel mech = findMechModel (frame);
               if (mech != null) {
                  mech.addFrameMarkerWorld (frame, isect);
               }
            } else if (comp instanceof RigidMeshComp) {
               RigidMeshComp rmc = (RigidMeshComp)comp;
               Frame rb = rmc.getRigidBody ();
               MechModel mech = findMechModel(rb);
               if (mech != null) {
                  mech.addFrameMarkerWorld (rb, isect);
               }
            } else if (comp instanceof FemModel3d) {
               FemModel3d fem = (FemModel3d)comp;
               FemMarker mkr = new FemMarker (isect);
               mkr.setFromFem (fem);
               fem.addMarker (mkr);
            } else if (comp instanceof FemMeshComp) {
               FemMeshComp fmc = (FemMeshComp)comp;
               FemModel3d fem = fmc.getFem ();
               FemMarker mkr = new FemMarker (isect);
               mkr.setFromFem (fem);
               fem.addMarker (mkr);
            }
            else if (comp instanceof SkinMeshBody) {
               SkinMeshBody skin = (SkinMeshBody)comp;
               skin.addMeshMarker (null, isect);
            } else if (comp instanceof PointAttachable) {
               PointAttachable pa = (PointAttachable)comp;
               
               marker = new GenericMarker(isect);
               PointAttachment attach = pa.createPointAttachment (marker);
               if (attach != null) {
                  marker.setAttached (attach);
                  
                  PointList<Marker> markers = getDefaultMarkerList ();
                  if (markers != null) {
                     markers.add (marker);
                  }
               }
            } else {
               System.err.println("ERROR: cannot add marker to component " + comp.toString ());
            }
         }
      }
      
      return marker;
   }
   
   public PointList<Marker> getDefaultMarkerList() {
      
      RootModel root = Main.getMain ().getRootModel ();
      
      if (root != null) {
         @SuppressWarnings("unchecked")
         PointList<Marker> markers = (PointList<Marker>)root.get ("markers");
         if (markers == null) {
            markers = new PointList<Marker>(Marker.class, "markers");
            root.add (markers);
         }
         return markers;
      }
      return null;
   }

}
