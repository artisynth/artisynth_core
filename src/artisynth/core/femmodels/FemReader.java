package artisynth.core.femmodels;

import java.io.IOException;

public interface FemReader {

   public FemModel3d readFem(FemModel3d fem) throws IOException;
   
}
