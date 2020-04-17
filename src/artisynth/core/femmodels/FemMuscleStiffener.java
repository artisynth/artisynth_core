/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import java.util.LinkedList;

import maspack.matrix.VectorNd;
import artisynth.core.materials.LinearMaterial;
import artisynth.core.materials.MooneyRivlinMaterial;

/**
 * Helper class to implement fem element stiffening with muscle activation
 * 
 * NOTE: at present, the stiffening effects apply only to volumetric elements
 * 
 * @author stavness
 */
public class FemMuscleStiffener {
   
   FemMuscleModel myFemMuscle;

   public VectorNd elemAct = new VectorNd ();
   boolean myActStiffnessP = false;
   public static final double stiffnessActFactor = 10.0-1;
   boolean warnedP = false;
   
   public FemMuscleStiffener(FemMuscleModel fem) {
      myFemMuscle = fem;
      
      /*
       * remove element specific material; stiffening used model material
       */
      for (FemElement e : myFemMuscle.getElements()) {
         e.setMaterial (null);
      }
   }

   public void updateElemStiffnesses () {
      
      // update activation stiffness
      if (elemAct.size () != myFemMuscle.numElements())
         elemAct = new VectorNd (myFemMuscle.numElements());
      else
         elemAct.setZero ();

      // an element can be associated with multiple bundles,
      // therefore we need to accumulate activations
      for (int i = 0; i < myFemMuscle.getMuscleBundles().size (); i++) {
         MuscleBundle b = myFemMuscle.getMuscleBundles().get (i);
         double act = b.getNetExcitation ();
         for (int j = 0; j < b.getElements ().size (); j++) {
            FemElement elem = b.getElements ().get (j).getElement();
            elemAct.add (myFemMuscle.getElements().indexOf (elem), act);
         }
      }

      // stiffen elements based on activation
      warnedP = false;
      for (int i = 0; i < elemAct.size(); i++) {
         scaleElemStiffness(myFemMuscle.getElement(i), elemAct.get(i));
      }
   }

   private void scaleElemStiffness (FemElement elem, double activation) {
      double validAct = activation;
      validAct = (validAct > 1.0) ? 1.0 : validAct;
      validAct = (validAct < 0.0) ? 0.0 : validAct;
      if (validAct == 0.0) {
         elem.setMaterial (null);
         return;
      }

      double s = 1 + stiffnessActFactor * validAct;
      
      if (myFemMuscle.getMaterial() instanceof LinearMaterial) {
         stiffenLinMat (elem, (LinearMaterial)myFemMuscle.getMaterial(), s);
      }
      else if (myFemMuscle.getMaterial() instanceof MooneyRivlinMaterial) {
         stiffenMRMat (elem, (MooneyRivlinMaterial)myFemMuscle.getMaterial(), s);
      }
      else if (!warnedP) {
         System.out
            .println ("activation stiffness scaling not implemented for material, "
            + myFemMuscle.getMaterial().getClass ().toString ());
         warnedP = true;
      }
   }

   private void stiffenLinMat (FemElement elem, LinearMaterial restMat, double s) {
      LinearMaterial linMat;
      if (elem.getMaterial () != null
      && elem.getMaterial () instanceof LinearMaterial) {
         linMat = (LinearMaterial)elem.getMaterial ();
      }
      else {
         linMat = new LinearMaterial ();
         elem.setMaterial (linMat);
      }
      linMat.setYoungsModulus (((LinearMaterial)restMat).getYoungsModulus () * s);
   }

   private void stiffenMRMat (FemElement elem, MooneyRivlinMaterial restMat, double s) {
      MooneyRivlinMaterial elemMat;
      if (elem.getMaterial () != null
      && elem.getMaterial () instanceof MooneyRivlinMaterial) {
         elemMat = (MooneyRivlinMaterial)elem.getMaterial ();
      }
      else {
         elemMat = new MooneyRivlinMaterial ();
      }
      elemMat.setC10 (restMat.getC10 () * s);
      elemMat.setC20 (restMat.getC20 () * s);
      elemMat.setBulkModulus (restMat.getBulkModulus () * s);
      if (elem.getMaterial () == null) {
         elem.setMaterial (elemMat);
      }
    }
 
   
   public void validateElementList(FemModel3d fem, MuscleBundle b) {
      LinkedList<MuscleElementDesc> toRemove = new LinkedList<MuscleElementDesc>();
      for (MuscleElementDesc desc : b.getElements()) {
	 FemElement elem = desc.getElement();
	 if (!fem.getElements().contains(elem))
	    toRemove.add(desc);
      }
      b.getElements().removeAll(toRemove);
   }
}
