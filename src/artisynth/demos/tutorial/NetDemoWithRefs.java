package artisynth.demos.tutorial;

import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.AxialSpring;

public class NetDemoWithRefs extends NetDemo {

   public void build (String[] args) {

      super.build (args);

      // create reference lists for both the green and blue springs
      // in the middle of the net and add these to the model
      ReferenceList<AxialSpring> greenMid = 
         new ReferenceList<> ("middleGreenSprings");
      ReferenceList<AxialSpring> blueMid = 
         new ReferenceList<> ("middleBlueSprings");

      for (int i=0; i<8; i++) {
         blueMid.addReference (blueSprings.get(32+i));
      }
      for (int i=0; i<8; i++) {
         greenMid.addReference (greenSprings.get(32+i));
      }
      mech.add (greenMid);
      mech.add (blueMid);
   }
}
