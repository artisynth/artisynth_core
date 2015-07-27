package artisynth.demos.tutorial;

import maspack.properties.PropertyList;
import maspack.render.Renderer;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import artisynth.core.mechmodels.CollisionHandler;
import artisynth.core.modelbase.MonitorBase;

public class PenetrationRenderer extends MonitorBase {

   CollisionHandler myCollisionHandler;
   
   public PenetrationRenderer (CollisionHandler handler) {
      super ();
      myCollisionHandler = handler;
      setRenderProps (createRenderProps ());
   }

   protected RenderProps myRenderProps = null;

   public static PropertyList myProps = new PropertyList (
      PenetrationRenderer.class, MonitorBase.class);

   static private RenderProps defaultRenderProps = new RenderProps ();

   static {
      myProps.add (
         "renderProps * *", "render properties for this constraint",
         defaultRenderProps);
   }

   public PropertyList getAllPropertyInfo () {
      return myProps;
   }

   @Override
   public void apply (double t0, double t1) {
      // TODO Auto-generated method stub

   }

   @Override
   public void prerender (RenderList list) {
      super.prerender (list);
      System.out.println ("PR - prerender");
   }

   @Override
   public void render (Renderer gl, int flags) {
      super.render (gl, flags);
      System.out.println ("PR - render");

   }

   @Override
   public RenderProps createRenderProps () {
      return super.createRenderProps ();
   }

   @Override
   public boolean isSelectable () {
      return true;
   }

}
