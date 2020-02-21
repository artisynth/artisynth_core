package artisynth.core.opensim;

import java.io.File;
import java.io.IOException;

import artisynth.core.mechmodels.MechModel;
import artisynth.core.util.ArtisynthPath;
import artisynth.core.workspace.DriverInterface;
import artisynth.core.workspace.RootModel;
import maspack.matrix.AxisAngle;
import maspack.render.RenderProps;
import maspack.render.Renderer.Shading;

public class OpenSimTest extends RootModel {


    File osimFile = ArtisynthPath.getSrcRelativeFile (this, "osim/arm26_v4.osim");
   // File osimFile = ArtisynthPath.getSrcRelativeFile (this, "osim/arm26.osim");
    
   @Override
   public void build (String[] args) throws IOException {
      super.build (args);
      
      RenderProps.setShading(this, Shading.SMOOTH);
      
      MechModel mech = new MechModel("mech");
      OpenSimParser parser = new OpenSimParser (osimFile);
      
      parser.createModel (mech);
      addModel(mech);
      
   }
   
   @Override
   public void attach (DriverInterface driver) {
      super.attach (driver);
      
      setDefaultViewOrientation (AxisAngle.ROT_Y_90);
   }
   
}
