package artisynth.core.opensim.components;

import org.w3c.dom.Element;

public class VisibleObjectFactory extends VisibleBaseFactory<VisibleObject>{
  
   public VisibleObjectFactory () {
      super(VisibleObject.class);
   }
   
   protected VisibleObjectFactory(Class<? extends VisibleObject> voClass) {
      super(voClass);
   }
   
   @Override
   protected boolean parseChild (VisibleObject comp, Element child) {
      
      String name = getNodeName (child);
      
      boolean success = true;
      if ("GeometrySet".equals(name)) {
         OpenSimObjectFactory<? extends GeometrySet> factory = getFactory (GeometrySet.class);
         if (factory != null) {
            GeometrySet set = factory.parse (child);
            comp.setGeometrySet (set);
         } else {
            success = false;
         }
      } else if ("geometry_files".equals(name)) {
         // create a new DisplayGeometry for each
         GeometrySet set = new GeometrySet();
         String[] files = parseTextArrayValue (child);
         for (String file : files) {
            DisplayGeometry dg = new DisplayGeometry (file);
            set.add (dg);
         }
         comp.setGeometrySet (set);
      } else if ("scale_factors".equals (name)) {
         comp.setScaleFactors (parseVector3dValue(child));
      } else if ("show_axes".equals(name)) {
         comp.setShowAxes (parseBooleanValue(child));
      } else if ("transform".equals(name)) {
         comp.setTransform (parseTransformValue(child));
      } else {
         success = super.parseChild (comp, child);
      }
      
      return success;
   }
   
}
