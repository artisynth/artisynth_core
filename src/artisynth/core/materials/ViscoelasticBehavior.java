package artisynth.core.materials;

import maspack.matrix.Matrix6d;
import maspack.matrix.SymmetricMatrix3d;

public abstract class ViscoelasticBehavior extends MaterialBase {
   
   static Class<?>[] mySubClasses = new Class[] {
      QLVBehavior.class
   };

   public static Class<?>[] getSubClasses() {
      return mySubClasses;
   }

   protected void notifyHostOfPropertyChange() {
      if (myPropHost instanceof FemMaterial) {
         ((FemMaterial)myPropHost).notifyHostOfPropertyChange ("viscoBehavior");
      }
      // System.out.println ("myPropHost=" + myPropHost);
      // if (myPropHost instanceof FemModel) {
      //    ((FemModel)myPropHost).invalidateStressAndStiffness();
      //    ((FemModel)myPropHost).invalidateRestData();
      // }
      // else if (myPropHost instanceof FemElement) {
      //    ((FemElement)myPropHost).invalidateRestData();
      // }
   }

   public abstract ViscoelasticState createState();
   
   public abstract void computeTangent (Matrix6d D, ViscoelasticState state);
  
   public abstract void computeStress (
      SymmetricMatrix3d sigma, DeformedPoint def, ViscoelasticState state);

   public abstract void computeStressAndTangent (
      SymmetricMatrix3d sigma, Matrix6d D, DeformedPoint def, 
      ViscoelasticState state);
   
   public abstract void advanceState (
      ViscoelasticState state, double t0, double t1);

   public boolean equals (ViscoelasticBehavior veb) {
      return true;
   }

   public boolean equals (Object obj) {
      if (obj instanceof ViscoelasticBehavior) {
         return equals ((ViscoelasticBehavior)obj);
      }
      else {
         return false;
      }
   }

   public ViscoelasticBehavior clone() {
      ViscoelasticBehavior veb = (ViscoelasticBehavior)super.clone();
      return veb;
   }

   public abstract MaterialStateObject createStateObject();
}
   
   
