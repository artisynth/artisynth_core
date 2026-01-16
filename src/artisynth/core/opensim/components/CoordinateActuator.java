package artisynth.core.opensim.components;

import java.io.*;

import artisynth.core.modelbase.ModelComponent;
import artisynth.core.mechmodels.JointActuator;
import artisynth.core.mechmodels.JointCoordinateHandle;

public class CoordinateActuator extends ForceBase {

   String coordinate;
   double optimal_force;
   double min_control;
   double max_control;

   public CoordinateActuator() {
      coordinate = null;
   }

   public String getCoordinate() {
      return coordinate;
   }
   
   public void setCoordinate(String coord) {
      coordinate = coord;
   }

   public JointActuator createComponent (
      File geometryPath, ModelComponentMap componentMap) {

      JointCoordinateHandle ch =
         CoordinateHandle.createJCH (coordinate, this, componentMap);
      if (ch == null) {
         return null;
      }
      JointActuator actuator = new JointActuator (getName(), ch, optimal_force);
      return actuator;
   }

}
      
