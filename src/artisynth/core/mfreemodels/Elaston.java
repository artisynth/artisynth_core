/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mfreemodels;

import maspack.matrix.Vector3d;
import artisynth.core.materials.LinearMaterial;
import artisynth.core.materials.MaterialBase;
import artisynth.core.mechmodels.Point;

public class Elaston extends Point {

   protected double myMass;
   protected Vector3d[] myPointTangents;
   MaterialBase myMaterial;
   
   public Elaston() {
      super();
      init();
   }
   
   private void init() {
      myPointTangents = new Vector3d[3];
      for (int i=0; i<3; i++) {
         myPointTangents[i] = new Vector3d(0,0,0);
      }
      myMaterial = new LinearMaterial();
   }
   
   public void setMass(double mass) {
      myMass = mass;
   }
   
   public double getMass() {
      return myMass;
   }
   
}
