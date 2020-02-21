package artisynth.core.opensim.components;

import org.w3c.dom.Element;

public class GeometryPathFactory extends HasVisibleObjectOrAppearanceFactory<GeometryPath> {

   public GeometryPathFactory() {
      super(GeometryPath.class);
   }
   
   protected GeometryPathFactory (Class<? extends GeometryPath> instanceClass) {
      super (instanceClass);
   }
   
   @Override
   protected boolean parseChild (GeometryPath comp, Element child) {
      
      boolean success = true;
      String name = getNodeName (child);
      
      if ("PathPointSet".equals(name)) {
         OpenSimObjectFactory<? extends PathPointSet> factory = getFactory (PathPointSet.class);
         if (factory != null) {
            PathPointSet pps = factory.parse (child);
            comp.setPathPointSet (pps);
         } else {
            success = false;
         }
      } else if ("PathWrapSet".equals(name)) {
         OpenSimObjectFactory<? extends PathWrapSet> factory = getFactory (PathWrapSet.class);
         if (factory != null) {
            PathWrapSet pps = factory.parse (child);
            comp.setPathWrapSet (pps);
         } else {
            success = false;
         }
      } else {
         success = super.parseChild (comp, child);
      }
      
      return success;
   }
   

}
