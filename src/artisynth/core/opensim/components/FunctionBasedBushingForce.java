package artisynth.core.opensim.components;

import java.io.File;
import java.util.ArrayList;

import artisynth.core.mechmodels.FrameSpring;
import artisynth.core.mechmodels.Frame;
import artisynth.core.materials.FunctionFrameMaterial;
import artisynth.core.materials.FrameMaterial;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.RenderableComponentList;
import artisynth.core.opensim.components.JointBase.BodyAndTransform;
import maspack.matrix.*;
import maspack.function.*;

public class FunctionBasedBushingForce extends BushingForceBase {

   FunctionBase m_x_theta_x_function;
   FunctionBase m_y_theta_y_function;
   FunctionBase m_z_theta_z_function;
   FunctionBase f_x_delta_x_function;
   FunctionBase f_y_delta_y_function;
   FunctionBase f_z_delta_z_function;

   public FunctionBasedBushingForce() {
      super();
   }
   
   @Override
   public FunctionBasedBushingForce clone () {
      
      FunctionBasedBushingForce bushing =
         (FunctionBasedBushingForce)super.clone ();
      
      if (m_x_theta_x_function != null) {
         bushing.m_x_theta_x_function = m_x_theta_x_function.clone();
      }
      if (m_y_theta_y_function != null) {
         bushing.m_y_theta_y_function = m_y_theta_y_function.clone();
      }
      if (m_z_theta_z_function != null) {
         bushing.m_z_theta_z_function = m_z_theta_z_function.clone();
      }
      if (f_x_delta_x_function != null) {
         bushing.f_x_delta_x_function = f_x_delta_x_function.clone();
      }
      if (f_y_delta_y_function != null) {
         bushing.f_y_delta_y_function = f_y_delta_y_function.clone();
      }
      if (f_z_delta_z_function != null) {
         bushing.f_z_delta_z_function = f_z_delta_z_function.clone();
      }
      return bushing;
   }

   
   private Diff1Function1x1Base getFxn (FunctionBase fbase) {
      Diff1FunctionNx1 fxn = fbase.getFunction();
      if (fxn instanceof Diff1Function1x1Base) {
         return (Diff1Function1x1Base)fxn;
      }
      else {
         return new ConstantFunction1x1(0);
      }
   }

   protected FrameMaterial createMaterial() {
      FunctionFrameMaterial mat = new FunctionFrameMaterial();
      mat.setTranslationalFunction (0, getFxn(f_x_delta_x_function));
      mat.setTranslationalFunction (1, getFxn(f_y_delta_y_function));
      mat.setTranslationalFunction (2, getFxn(f_z_delta_z_function));
      mat.setDamping (translational_damping);
      mat.setRotaryFunction (0, getFxn(m_x_theta_x_function));
      mat.setRotaryFunction (1, getFxn(m_y_theta_y_function));
      mat.setRotaryFunction (2, getFxn(m_z_theta_z_function));
      mat.setRotaryDamping (rotational_damping);
      return mat;
   }

}
