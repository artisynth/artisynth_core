package artisynth.core.femmodels;

import java.io.*;
import java.util.*;

import artisynth.core.util.ScalableUnits;
import artisynth.core.util.ScanToken;
import artisynth.core.modelbase.*;
import artisynth.core.femmodels.SkinMeshBody.NearestPoint;
import maspack.properties.*;
import maspack.matrix.Point3d;
import maspack.util.*;

/**
 * Class that computes the connection weights for a SkinMeshBody point
 * attachment.
 */
public abstract class SkinWeightingFunction extends CompositePropertyBase
   implements ScalableUnits, PropertyChangeListener  {

   /**
    * Computes skinning connection weights for a point located
    * at {@code pos}, given information about the nearest points
    * on each of the master bodies.
    * 
    * <p>The weights are returned in {@code weights}. There is
    * one weight for each master bodies, and so {@code weights}
    * and {@code nearestPnts} have the same length.
    *
    * @param weights returns the computed weights
    * @param pos position of the point to be skinned
    * @param nearestPnts nearest point information for each of the 
    * master bodies
    */
   public abstract void computeWeights (
      double[] weights, Point3d pos, NearestPoint[] nearestPnts);

   static DynamicArray<Class<?>> mySubclasses = new DynamicArray<>(
      new Class<?>[] {
      InverseSquareWeighting.class,
      GaussianWeighting.class,
   });

   /**
    * Allow adding of classes (for use in control panels)
    * @param cls class to register
    */
   public static void registerSubclass(Class<? extends SkinWeightingFunction> cls) {
      if (!mySubclasses.contains(cls)) {
         mySubclasses.add(cls);
      }
   }

   public static Class<?>[] getSubClasses() {
      return mySubclasses.getArray();
   }

   public void scaleDistance (double s) {
      // TODO material specific scaling in sub-classes
   }

   public void scaleMass (double s) {
      // TODO material specific scaling in sub-classes
   }

   public SkinWeightingFunction clone() {
      return (SkinWeightingFunction)super.clone();
   }

   protected void notifyHostOfPropertyChange (String name) {
      if (myPropHost instanceof PropertyChangeListener) {
         ((PropertyChangeListener)myPropHost).propertyChanged (
            new PropertyChangeEvent (this, name));
      }
   }

   public void propertyChanged (PropertyChangeEvent e) {
      // pass on property change events from subcomponents
      if (myPropHost instanceof PropertyChangeListener) {
         ((PropertyChangeListener)myPropHost).propertyChanged (e);
      }
   }

}

