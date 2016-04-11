package maspack.render.GL.GL2;

import maspack.render.GL.GLPipelineRendererFactory;

public class GL2PipelineRendererFactory implements GLPipelineRendererFactory {

   @Override
   public GL2PipelineRenderer generate () {
      return new GL2PipelineRenderer ();
   }
   
   

}
