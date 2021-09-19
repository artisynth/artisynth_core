package artisynth.core.opensim.components;

import org.w3c.dom.Element;

public class Millard2012EquilibriumMuscleFactory extends MuscleBaseFactory<Millard2012EquilibriumMuscle> {

   public Millard2012EquilibriumMuscleFactory() {
      super(Millard2012EquilibriumMuscle.class);
   }
   
   protected Millard2012EquilibriumMuscleFactory (Class<? extends Millard2012EquilibriumMuscle> instanceClass) {
      super (instanceClass);
   }
   
   @Override
   public boolean canParse (Element elem) {
      String name = getNodeName (elem);
      
      // allow muscle or deprecated
      boolean valid = false;
      if ("Millard2012EquilibriumMuscle".equals (name)) {
         valid = true;
      }
      return valid;
   }
   
   @Override
   protected boolean parseChild (Millard2012EquilibriumMuscle comp, Element child) {
      
      boolean success = true;
      
      String name = getNodeName (child);
      
      if ("fiber_damping".equals (name)) {
         comp.setFiberDamping (parseDoubleValue (child));
      } else if ("default_activation".equals (name)) {
         comp.setDefaultActivation (parseDoubleValue (child));
      } else if ("default_fiber_length".equals(name)) {
         comp.setDefaultFiberLength (parseDoubleValue (child));
      } else if ("activation_time_constant".equals(name)) {
         comp.setActivationTimeConstant (parseDoubleValue (child));
      } else if ("deactivation_time_constant".equals(name)) {
         comp.setDeactivationTimeConstant (parseDoubleValue (child));
      } else if ("minimum_activation".equals(name)) {
         comp.setMinimumActivation (parseDoubleValue (child));
      } else if ("maximum_pennation_angle".equals(name)) {
         comp.setMaximumPennationAngle (parseDoubleValue (child));
      } else if ("ActiveForceLengthCurve".equals(name)) {
         ActiveForceLengthCurveFactory cf = new ActiveForceLengthCurveFactory();
         if (cf != null) {
            comp.activeForceLengthCurve = cf.parse(child);
         }
         else {
            success = false;
         }
      } else if ("ForceVelocityCurve".equals(name)) {
         ForceVelocityCurveFactory cf = new ForceVelocityCurveFactory();
         if (cf != null) {
            comp.forceVelocityCurve = cf.parse(child);
         }
         else {
            success = false;
         }
      } else if ("FiberForceLengthCurve".equals(name)) {
         FiberForceLengthCurveFactory cf = new FiberForceLengthCurveFactory();
         if (cf != null) {
            comp.fiberForceLengthCurve = cf.parse(child);
         }
         else {
            success = false;
         }
      } else if ("TendonForceLengthCurve".equals(name)) {
         TendonForceLengthCurveFactory cf = new TendonForceLengthCurveFactory();
         if (cf != null) {
            comp.tendonForceLengthCurve = cf.parse(child);
         }
         else {
            success = false;
         }
      } else {
         success = super.parseChild (comp, child);
      }
      
      return success;
   }

}
