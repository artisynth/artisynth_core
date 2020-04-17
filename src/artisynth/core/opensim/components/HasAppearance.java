package artisynth.core.opensim.components;

import maspack.render.RenderProps;

/**
 * Base class for objects containing a visible object
 * @author antonio
 *
 */
public class HasAppearance extends OpenSimObject {

   Appearance appearance;
   
   public HasAppearance() {
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
    * @return appearance
    */
   public Appearance getAppearance() {
      return appearance;
   }
   
   @Override
   public HasAppearance clone () {
      // TODO Auto-generated method stub
      HasAppearance obj = (HasAppearance) super.clone ();
      
      if (appearance != null) {
         obj.setAppearance (appearance.clone ());
      }
      
      return obj;
   }
   
   protected void updateRenderProps (RenderProps rprops) {
      // overwrite with visible object properties
      if (appearance != null) {
         appearance.updateRenderProps (rprops);
      }
   }
   
   public RenderProps createRenderProps () {
      RenderProps rprops = new RenderProps();
      updateRenderProps (rprops);
      return rprops;
   }

}
