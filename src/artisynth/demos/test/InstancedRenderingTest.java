package artisynth.demos.test;

import java.io.IOException;

import artisynth.core.workspace.RootModel;
import maspack.matrix.Vector3d;
import maspack.render.RenderInstances;
import maspack.render.RenderList;
import maspack.render.RenderObject;
import maspack.render.RenderObjectFactory;
import maspack.render.Renderer;

public class InstancedRenderingTest extends RootModel {
   
   RenderObject robj;
   RenderInstances rinst;
   
   @Override
   public void build(String[] args) throws IOException {
      super.build(args);
   }
   
   @Override
   public void prerender(RenderList list) {
      super.prerender(list);
      
      if (robj == null) {
         robj = RenderObjectFactory.createOctohedralSphere(1);
         robj.setTransient(true);
         
         rinst = new RenderInstances();
         rinst.addInstance(new Vector3d(-2, -2, -2));
         rinst.addInstance(new Vector3d(-2,  2,  2));
         rinst.addInstance(new Vector3d(-2,  2, -2));
         rinst.addInstance(new Vector3d(-2, -2,  2));
         rinst.addInstance(new Vector3d( 2, -2, -2));
         rinst.addInstance(new Vector3d( 2,  2,  2));
         rinst.addInstance(new Vector3d( 2,  2, -2));
         rinst.addInstance(new Vector3d( 2, -2,  2));
      }
   }
   
   @Override
   public void render(Renderer renderer, int flags) {
      super.render(renderer, flags);
      
      renderer.drawTriangles(robj, 0, rinst);
      
   }

}
