/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import maspack.matrix.*;

public class DistanceRecord {
   Feature feature0;
   Feature feature1;
   Point3d pnt0;
   Point3d pnt1;
   Vector3d nrml;
   double dist;

   public DistanceRecord() {
      pnt0 = new Point3d();
      pnt1 = new Point3d();
      nrml = new Vector3d();
   }

   public String featureTypeName (int num) {
      if (num == 0) {
         return feature0 != null ? feature0.getTypeName() : "null";
      }
      else if (num == 1) {
         return feature1 != null ? feature1.getTypeName() : "null";
      }
      else {
         throw new ArrayIndexOutOfBoundsException ("num must be 0 or 1");
      }
   }

   public int featureType (int num) {
      if (num == 0) {
         return feature0 != null ? feature0.getType() : Feature.UNKNOWN;
      }
      else if (num == 1) {
         return feature1 != null ? feature1.getType() : Feature.UNKNOWN;
      }
      else {
         throw new ArrayIndexOutOfBoundsException ("num must be 0 or 1");
      }
   }

   public DistanceRecord (DistanceRecord rec) {
      this();
      set (rec);
   }

   public void transform (RigidTransform3d X) {
      pnt0.transform (X);
      pnt1.transform (X);
      nrml.transform (X);
   }

   public void inverseTransform (RigidTransform3d X) {
      pnt0.inverseTransform (X);
      pnt1.inverseTransform (X);
      nrml.inverseTransform (X);
   }

   void setPoints (Point3d p0, Point3d p1) {
      pnt0.set (p0);
      pnt1.set (p1);
   }

   void setFeatures (Feature f0, Feature f1) {
      feature0 = f0;
      feature1 = f1;
   }

   void set (DistanceRecord rec) {
      nrml.set (rec.nrml);
      setPoints (rec.pnt0, rec.pnt1);
      setFeatures (rec.feature0, rec.feature1);
      dist = rec.dist;
   }

   public boolean epsilonEquals (DistanceRecord rec, double eps) {
      return (feature0 == rec.feature0 && feature1 == rec.feature1 &&
              pnt0.epsilonEquals (rec.pnt0, eps) &&
              pnt1.epsilonEquals (rec.pnt1, eps) &&
              nrml.epsilonEquals (rec.nrml, eps) &&
              Math.abs (dist - rec.dist) <= eps);
   }

   public String toString() {
      String fname0 = "null";
      String fname1 = "null";
      if (feature0 != null) {
         fname0 = feature0.getTypeName();
      }
      if (feature1 != null) {
         fname1 = feature1.getTypeName();
      }
      return (
         "pnt0: " + pnt0 + "\n" + "pnt1: " + pnt1 + "\n" + "nrml: " + nrml +
         "\n" + "dist: " + dist + "\n" + "pair: " + fname0 + "-" + fname1);
   }

   void clear() {
      dist = Double.POSITIVE_INFINITY;
   }

   void computeDistanceAndNormal() {
      nrml.sub (pnt1, pnt0);
      dist = nrml.norm();
      if (dist != 0) {
         nrml.scale (1 / dist);
      }
   }

   void computeDistance() {
      dist = pnt0.distance (pnt1);
   }

   void setDistanceAndNormal (double d, Vector3d n) {
      dist = d;
      nrml.set (n);
   }
}
