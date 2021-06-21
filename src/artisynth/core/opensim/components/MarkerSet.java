package artisynth.core.opensim.components;

import java.io.File;

import artisynth.core.mechmodels.FrameMarker;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.PointList;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.RenderableComponentList;

public class MarkerSet extends SetBase<Marker>
   implements ModelComponentGenerator<PointList<FrameMarker>>{
   
   @Override
   public MarkerSet clone () {
      return (MarkerSet)super.clone ();
   }

   @Override
   public PointList<FrameMarker> createComponent (
      File geometryPath, ModelComponentMap componentMap) {


      String name = getName();
      if (name == null) {
         name = "markerset";
      }
      PointList<FrameMarker> markers = new PointList<> (FrameMarker.class, name);
      
      // add all markers
      for (Marker marker : objects()) {
         
         // create markers
         FrameMarker m = marker.createComponent(geometryPath, componentMap);
         markers.add (m);
         
      }
    
      
      componentMap.put (this, markers);
      return markers;
   }
   
}
