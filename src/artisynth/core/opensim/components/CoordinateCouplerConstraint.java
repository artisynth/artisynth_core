package artisynth.core.opensim.components;

import java.io.*;
import java.util.ArrayList;

import artisynth.core.modelbase.ModelComponent;
import artisynth.core.mechmodels.*;
import maspack.util.DoubleInterval;

public class CoordinateCouplerConstraint extends ConstraintBase {

   FunctionBase coupled_coordinates_function;
   String dependent_coordinate_name;
   String[] independent_coordinate_names;
   double scale_factor;
   
   public FunctionBase getCoupledCoordinatesFunction () {
      return coupled_coordinates_function;
   }

   public void setCoupledCoordinatesFunction (FunctionBase fxn) {
      this.coupled_coordinates_function = fxn;
   }

   public String getDependentCoordinateName () {
      return dependent_coordinate_name;
   }

   public void setDependentCoordinateName (String name) {
      this.dependent_coordinate_name = name;
   }

   public String[] getIndependentCoordinateNames () {
      return independent_coordinate_names;
   }

   public void setIndependentCoordinateNames (String[] names) {
      this.independent_coordinate_names = names;
   }

   public double getScaleFactor () {
      return scale_factor;
   }

   public void setScaleFactor (double s) {
      this.scale_factor = s;
   }

   public CoordinateCouplerConstraint() {
   }

   public JointCoordinateCoupling createComponent (
      File geometryPath, ModelComponentMap componentMap) {

      ArrayList<JointCoordinateHandle> coords = new ArrayList<>();

      JointCoordinateHandle ch = CoordinateHandle.createJCH (
         dependent_coordinate_name, this, componentMap);
      if (ch == null) {
         return null;
      }
      coords.add (ch);
      int ni = independent_coordinate_names.length;
      for (int i=0; i<ni; i++) {
         ch = CoordinateHandle.createJCH (
            independent_coordinate_names[i], this, componentMap);
         if (ch == null) {
            return null;
         }
         coords.add (ch);
      }
      JointCoordinateCoupling jcc =
         new JointCoordinateCoupling (
            getName(), coords, getCoupledCoordinatesFunction().getFunction());
      jcc.setScaleFactor (getScaleFactor());
      return jcc;
   }

   @Override
   public CoordinateCouplerConstraint clone () {
      CoordinateCouplerConstraint cpp =
         (CoordinateCouplerConstraint)super.clone ();
      return cpp;
   }
}
