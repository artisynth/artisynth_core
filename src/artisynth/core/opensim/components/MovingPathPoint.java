package artisynth.core.opensim.components;

import java.io.*;
import artisynth.core.mechmodels.*;
import maspack.function.*;

public class MovingPathPoint extends PathPoint {

   String x_coordinate;
   FunctionBase x_location;
   String y_coordinate;
   FunctionBase y_location;
   String z_coordinate;
   FunctionBase z_location;
   
   public MovingPathPoint() {
      super();
      x_coordinate = null;
      x_location = null;
      y_coordinate = null;
      y_location = null;
      z_coordinate = null;
      z_location = null;
   }
   
   public String getXCoordinate () {
      return x_coordinate;
   }

   public void setXCoordinate (String x_coordinate) {
      this.x_coordinate = x_coordinate;
   }

   public FunctionBase getXLocation () {
      return x_location;
   }

   public void setXLocation (FunctionBase x_location) {
      this.x_location = x_location;
      this.x_location.setParent (this);
   }

   public String getYCoordinate () {
      return y_coordinate;
   }

   public void setYCoordinate (String y_coordinate) {
      this.y_coordinate = y_coordinate;
   }

   public FunctionBase getYLocation () {
      return y_location;
   }

   public void setYLocation (FunctionBase y_location) {
      this.y_location = y_location;
      this.y_location.setParent (this);
   }

   public String getZCoordinate () {
      return z_coordinate;
   }

   public void setZCoordinate (String z_coordinate) {
      this.z_coordinate = z_coordinate;
   }

   public FunctionBase getZLocation () {
      return z_location;
   }

   public void setZLocation (FunctionBase z_location) {
      this.z_location = z_location;
      this.z_location.setParent (this);
   }

   Diff1Function1x1 getFunction (String fname, FunctionBase fbase) {
      Diff1FunctionNx1 fxn = fbase.getFunction();
      if (fxn instanceof Diff1Function1x1) {
         return (Diff1Function1x1)fxn;
      }
      else {
         System.out.println (
            "WARNING: MovingPathPoint " + getName() +
            ": " + fname + " function is not 1x1");
         return null;
      }
   }

   public JointBasedMovingMarker createComponent (
      File geometryPath, ModelComponentMap componentMap) {

      JointCoordinateHandle xch =
         CoordinateHandle.createJCH (x_coordinate, this, componentMap);
      JointCoordinateHandle ych =
         CoordinateHandle.createJCH (y_coordinate, this, componentMap);
      JointCoordinateHandle zch =
         CoordinateHandle.createJCH (z_coordinate, this, componentMap);
      if (xch == null || ych == null || zch == null) {
         return null;
      }
      Diff1Function1x1 xfxn = getFunction ("x_location", x_location);
      Diff1Function1x1 yfxn = getFunction ("y_location", y_location);
      Diff1Function1x1 zfxn = getFunction ("z_location", z_location);
      if (xfxn == null || yfxn == null || zfxn == null) {
         return null;
      }
      return new JointBasedMovingMarker (
         getName(), xfxn, xch, yfxn, ych, zfxn, zch);
   }

   @Override
   public MovingPathPoint clone () {
      MovingPathPoint mpp =  (MovingPathPoint)super.clone ();
      if (x_location != null) {
         mpp.setXLocation (x_location.clone ());
      }
      if (y_location != null) {
         mpp.setYLocation (y_location.clone ());
      }
      if (z_location != null) {
         mpp.setZLocation (z_location.clone ());
      }
      
      return mpp;
   }
   
}
