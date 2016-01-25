package artisynth.demos.tutorial;

import artisynth.core.workspace.FemModelDeformer;

/**
 * Demo of jointed rigid bodies colliding with a base plate
 */
public class DeformedJointedCollide extends JointedCollide {

   public void build (String[] args) {

      super.build (args);
      FemModelDeformer deformer =
         new FemModelDeformer ("deformer", this, 2);
      addModel (deformer);
      addControlPanel (deformer.createControlPanel());
   }

}
