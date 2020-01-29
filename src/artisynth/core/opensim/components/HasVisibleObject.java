package artisynth.core.opensim.components;

import maspack.render.RenderProps;

/**
 * Base class for objects containing a visible object
 * @author antonio
 *
 */
public class HasVisibleObject extends VisibleBase {

   VisibleObject visibleObject;
   
   public HasVisibleObject() {
      visibleObject = null;
   }
   
   /**
    * Sets visibility properties pre OpenSim 4.0
    * @param vo visible object
    */
   public void setVisibleObject(VisibleObject vo) {
      visibleObject = vo;
      visibleObject.setParent (this);
   }
   
   /**
    * Returns visibility properties pre OpenSim 4.0
    * @return visible object
    */
   public VisibleObject getVisibleObject() {
      return visibleObject;
   }
   
   @Override
   public HasVisibleObject clone () {
      // TODO Auto-generated method stub
      HasVisibleObject obj = (HasVisibleObject) super.clone ();
      
      if (visibleObject != null) {
         obj.setVisibleObject (visibleObject.clone ());
      }
      
      return obj;
   }
   
   @Override
   protected void updateRenderProps (RenderProps rprops) {
      // first this
      super.updateRenderProps (rprops);
      // overwrite with visible object properties
      if (visibleObject != null) {
         visibleObject.updateRenderProps (rprops);
      }
   }
   
   @Override
   public RenderProps createRenderProps () {
      RenderProps rprops = new RenderProps();
      updateRenderProps (rprops);
      return rprops;
   }

}
