/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mfreemodels;

import maspack.matrix.Point3d;

public class MaterialPoint extends Point3d {
   private static final long serialVersionUID = -5519894752791685874L;
   protected double myRadius;
   protected double myMass;
   
   /**
    * Point for describing material
    * @param pos Position
    * @param r Radius
    * @param m Mass
    */
   public MaterialPoint(Point3d pos, double r, double m) {
      set(pos);
      this.myRadius = r;
      this.myMass = m;
   }
   
   public MaterialPoint(double x, double y, double z, double r) {
      set(x,y,z);
      myRadius = r;
      myMass=0;
   }
   
   public void setRadius(double r) {
      myRadius = r;
   }
   
   public double getRadius() {
      return myRadius;
   }
   
   public void setMass(double m) {
      myMass = m;
   }
   
   public double getMass() {
      return myMass;
   }

}
