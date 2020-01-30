package artisynth.core.opensim.components;

import maspack.matrix.Vector3d;

public class TransformAxis extends OpenSimObject {
   
   private Vector3d axis;
   private String[] coordinates;
   private FunctionBase function;
   
   public TransformAxis() {
      axis = null;
      coordinates = null;
      function = null;
   }
   
   public Vector3d getAxis () {
      return axis;
   }
   
   public void setAxis (Vector3d axis) {
      this.axis = axis;
   }
   
   public String[] getCoordinates () {
      return coordinates;
   }
   
   public void setCoordinates (String[] coordinates) {
      this.coordinates = coordinates;
   }
   
   public FunctionBase getFunction () {
      return function;
   }
   
   public void setFunction (FunctionBase function) {
      this.function = function;
   }
   
   @Override
   public TransformAxis clone () {
      TransformAxis taxis = (TransformAxis)super.clone ();
      if (axis != null) {
         taxis.setAxis (axis.clone ());
      }
      taxis.setCoordinates (coordinates);
      if (function != null) {
         taxis.setFunction (function.clone ());
      }
      return taxis;
   }

}
