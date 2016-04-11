package maspack.render.GL.GL3;

import maspack.render.GL.GLPipelineRendererFactory;

public class GL3PipelineRendererFactory implements GLPipelineRendererFactory {

   int ploc;
   int nloc;
   int cloc;
   int tloc;
   
   public GL3PipelineRendererFactory (int normalAttribLocation, int colorAttribLocation,
      int texcoordAttribLocation, int positionAttribLocation) {
      ploc = positionAttribLocation;
      nloc = normalAttribLocation;
      cloc = colorAttribLocation;
      tloc = texcoordAttribLocation;
   }
   
   @Override
   public GL3PipelineRenderer generate () {
      return new GL3PipelineRenderer (nloc, cloc, tloc, ploc);
   }

}
