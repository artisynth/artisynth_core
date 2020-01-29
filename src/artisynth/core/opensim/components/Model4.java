package artisynth.core.opensim.components;

import java.io.File;

import artisynth.core.mechmodels.MechModel;

public class Model4 extends ModelBase {
   
   // private Ground ground;
   private JointSet jointSet;
   
   public Model4() {
      jointSet = null;
   }
   
   public JointSet getJointSet() {
      return jointSet;
   }
   
   public void setJointSet(JointSet joints) {
      jointSet = joints;
      jointSet.setParent (this);
   }
   
   @Override
   public Model4 clone () {
      Model4 model = (Model4)super.clone ();
      
      if (jointSet != null) {
         model.setJointSet (jointSet.clone ());
      }
      
      return model;
   }

   /**
    * TODO: build model
    */
   public MechModel createModel(MechModel mech, File geometryFile, ModelComponentMap componentMap) {
      return mech;
   }
   
}
