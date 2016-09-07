package artisynth.demos.tutorial;

import java.io.IOException;

import maspack.render.RenderList;
import maspack.util.DoubleInterval;
import artisynth.core.femmodels.FemModel.Ranging;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.renderables.ColorBar;

public class FemBeamColored extends FemBeam {
   
   @Override
   public void build(String[] args) throws IOException {
      super.build(args);
      
      // Show stress on the surface
      fem.setSurfaceRendering(SurfaceRender.Stress);
      fem.setStressPlotRanging(Ranging.Auto);
    
      // Create a colorbar
      ColorBar cbar = new ColorBar();
      cbar.setName("colorBar");
      cbar.setNumberFormat("%.2f");      // 2 decimal places
      cbar.populateLabels(0.0, 1.0, 10); // Start with range [0,1], 10 ticks
      cbar.setLocation(-100, 0.1, 20, 0.8);
      addRenderable(cbar);
      
   }
   
   @Override
   public void prerender(RenderList list) {
      // Synchronize color bar/values in case they are changed
      ColorBar cbar = (ColorBar)(renderables().get("colorBar"));
      cbar.setColorMap(fem.getColorMap());
      DoubleInterval range = fem.getStressPlotRange();
      cbar.updateLabels(range.getLowerBound(), range.getUpperBound());
      
      super.prerender(list);
   }

}
