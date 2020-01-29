package artisynth.core.opensim.components;

import java.util.ArrayList;

public class SpatialTransform extends OpenSimObject {

   ArrayList<TransformAxis> transformAxes;
   
   public SpatialTransform() {
      transformAxes = new ArrayList<>(6);
   }
   
   public void addTransformAxis(TransformAxis axis) {
      transformAxes.add (axis);
      axis.setParent (this);
   }
   
   public ArrayList<TransformAxis> getTransformAxes() {
      return transformAxes;
   }
   
   @Override
   public SpatialTransform clone () {
      SpatialTransform st = (SpatialTransform)super.clone ();
      st.transformAxes = new ArrayList<>();
      for (TransformAxis taxis : transformAxes) {
         st.addTransformAxis (taxis.clone ());
      }
      return st;
   }

   public TransformAxis[] getTransformAxisArray () {
      return transformAxes.toArray (new TransformAxis[transformAxes.size ()]);
   }
   
}
