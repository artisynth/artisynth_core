package artisynth.core.opensim.components;

import org.w3c.dom.Element;

public abstract class JointBaseFactory<E extends JointBase> extends OpenSimObjectFactory<E> {

   protected JointBaseFactory (Class<? extends E> instanceClass) {
      super (instanceClass);
   }
   
   @Override
   protected boolean parseChild (E comp, Element child) {
      
      boolean success = true;
      
      String name = getNodeName (child);
      
      if ("CoordinateSet".equals(name)) {
         OpenSimObjectFactory<? extends CoordinateSet> factory = getFactory (CoordinateSet.class);
         if (factory != null) {
            CoordinateSet cs = factory.parse (child);
            comp.setCoordinateSet (cs);
         } else {
            success = false;
         }
      } else if ("parent_body".equals(name)) {
         comp.setParentBody (parseTextValue (child));
      } else if ("location_in_parent".equals(name)) {
         comp.setLocationInParent (parsePoint3dValue (child));
      } else if ("orientation_in_parent".equals(name)) {
         comp.setOrientationInParent (parseOrientationValue(child));
      }  else if ("location".equals(name)) {
         comp.setLocation (parsePoint3dValue (child));
      } else if ("orientation".equals(name)) {
         comp.setOrientation (parseOrientationValue(child));
      } else if ("reverse".equals(name)) {
         comp.setReverse (parseBooleanValue (child));
      } else {
         success = super.parseChild (comp, child);
      }
      
      return success;
   }

}
