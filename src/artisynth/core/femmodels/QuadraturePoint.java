/**
 * Copyright (c) 2020, by the Authors: Fabien Pean
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.matrix.Vector4d;

public class QuadraturePoint extends Point3d {
   public double w;

   public QuadraturePoint(double[] coords) {
      if(coords.length < 3 || coords.length > 4)
         throw new IllegalArgumentException("Integration coordinates should be of dimension 3 or 4");

      this.x = coords[0];
      this.y = coords[1];
      this.z = coords[2];
      if(coords.length == 4) {
         this.w = coords[3];
      }
      else {
         this.w = 1;
      }
   };

   public QuadraturePoint(double r, double s, double t) {
      this.x = r;
      this.y = s;
      this.z = t;
      this.w = 1;
   };

   public QuadraturePoint(double[] coords, double weight) {
      if(coords.length >= 3)
         throw new IllegalArgumentException("Integration coordinates should be of dimension 3");

      this.x = coords[0];
      this.y = coords[1];
      this.z = coords[2];
      this.w = weight;
   };

   public QuadraturePoint(double r, double s, double t, double w) {
      this.x = r;
      this.y = s;
      this.z = t;
      this.w = w;
   };

   public QuadraturePoint(Vector3d coords, double w) {
      this.x = coords.x;
      this.y = coords.y;
      this.z = coords.z;
      this.w = w;
   };

   public QuadraturePoint(Vector4d coords) {
      this.x = coords.x;
      this.y = coords.y;
      this.z = coords.z;
      this.w = coords.w;
   };
   
   public QuadraturePoint(QuadraturePoint qp) {
      this.x = qp.x;
      this.y = qp.y;
      this.z = qp.z;
      this.w = qp.w;
   };
   
   @Override
   public QuadraturePoint clone() {
      return new QuadraturePoint(x, y, z, w);
   }
   
   @Override
   public String toString() {
      return super.toString() + " " + w;
   }
   
   static public QuadraturePoint[] convert(double[][] coords) {
      QuadraturePoint[] qps = new QuadraturePoint[coords.length];
      for(int i = 0; i < coords.length; i++) {
         qps[i] = new QuadraturePoint(coords[i]);
      }
      return qps;
   }
   
   static public double[] toArray(QuadraturePoint qp) {
      return new double[]{qp.x,qp.y,qp.z,qp.w};
   }
}
