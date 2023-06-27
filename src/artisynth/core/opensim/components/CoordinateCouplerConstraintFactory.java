package artisynth.core.opensim.components;

import org.w3c.dom.Element;

public class CoordinateCouplerConstraintFactory
   extends ConstraintBaseFactory<CoordinateCouplerConstraint>{
   
   public CoordinateCouplerConstraintFactory () {
      super(CoordinateCouplerConstraint.class);
   }
   
   protected CoordinateCouplerConstraintFactory (
      Class<? extends CoordinateCouplerConstraint> clfClass) {
      super(clfClass);
   }

   @Override
   protected boolean parseChild (
      CoordinateCouplerConstraint comp, Element child) {

      boolean success = true;
      
      String name = getNodeName (child);

      if ("coupled_coordinates_function".equals (name)) {
         comp.setCoupledCoordinatesFunction (parseFunctionValue (child));
      }
      else if ("independent_coordinate_names".equals(name)) {
         comp.setIndependentCoordinateNames (parseTextArrayValue (child));
      }
      else if ("dependent_coordinate_name".equals(name)) {
         comp.setDependentCoordinateName (parseTextValue (child));
      }
      else if ("scale_factor".equals (name)) {
         comp.setScaleFactor (parseDoubleValue (child));
      }
      else {
         success = super.parseChild (comp, child);
      }
      return success;
   }
   
}
