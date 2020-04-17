package artisynth.core.opensim.components;

import maspack.render.RenderProps;

/**
 * Base class for objects containing a visible object
 * @author antonio
 *
 */
public class HasVisibleObjectOrAppearance extends HasVisibleObject {

   Appearance appearance;
   
   public HasVisibleObjectOrAppearance() {
      appearance = null;
   }
   
   /**
    * Sets visibility properties in OpenSim 4.0
    */
   public void setAppearance(Appearance a) {
      appearance = a;
      appearance.setParent (this);
   }
   
   /**
    * Returns visibility properties in OpenSim 4.0
    */
   public Appearance getAppearance() {
      return appearance;
   }
   
   @Override
   public HasVisibleObjectOrAppearance clone () {
      // TODO Auto-generated method stub
      HasVisibleObjectOrAppearance obj = (HasVisibleObjectOrAppearance) super.clone ();
      
      if (appearance != null) {
         obj.setAppearance (appearance.clone ());
      }
      
      return obj;
   }
   
   protected void updateRenderProps (RenderProps rprops) {
      // overwrite with visible object properties
      if (appearance != null) {
         appearance.updateRenderProps (rprops);
      } else {
         super.updateRenderProps (rprops);
      }
   }
   
   public RenderProps createRenderProps () {
      RenderProps rprops = new RenderProps();
      updateRenderProps (rprops);
      return rprops;
   }

}
