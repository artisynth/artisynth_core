package artisynth.core.opensim.components;

import java.io.File;

import artisynth.core.mechmodels.FrameMarker;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.PointList;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.RenderableComponentList;

public class MarkerSet extends SetBase<Marker>
   implements ModelComponentGenerator<
      PointList<artisynth.core.mechmodels.Marker>>{
   
   @Override
   public MarkerSet clone () {
      return (MarkerSet)super.clone ();
   }

   @Override
   public PointList<artisynth.core.mechmodels.Marker> createComponent (
      File geometryPath, ModelComponentMap componentMap) {


      String name = getName();
      if (name == null) {
         name = "markerset";
      }
      PointList<artisynth.core.mechmodels.Marker> markers =
         new PointList<> (artisynth.core.mechmodels.Marker.class, name);
      
      // add all markers
      for (Marker marker : objects()) {
         // create markers
         artisynth.core.mechmodels.Marker m =
            marker.createComponent(geometryPath, componentMap);
         if (m != null) {
            markers.add (m);
         }
      }

      componentMap.put (this, markers);
      return markers;
   }
   
}
