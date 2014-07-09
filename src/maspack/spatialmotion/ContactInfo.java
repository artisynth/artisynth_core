/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.spatialmotion;

import maspack.matrix.*;
import maspack.spatialmotion.*;
import maspack.util.NumberFormat;

// import vclipx.ClosestPointPair;

public class ContactInfo {
   /**
    * contact point, in body coordinates
    */
   public Point3d pnt;

   /**
    * contact normal, in body coordinates (and normalized)
    */
   public Vector3d nrm;

   /**
    * Friction coefficient
    */
   public double mu;

   /**
    * velocity limit along the contact normal
    */
   public double normalVelocityLimit;

   /**
    * velocity of the other body associated with this contact, in the
    * coordinates of this body.
    */
   public Twist otherBodyVelocity;

   public ContactInfo() {
      pnt = new Point3d();
      nrm = new Vector3d();
      otherBodyVelocity = new Twist();
      mu = 0;
      normalVelocityLimit = 0;
   }

   public ContactInfo (Point3d pnt, Vector3d nrm) {
      this();
      set (pnt, nrm);
   }

   public ContactInfo (ContactInfo contact) {
      this();
      set (contact);
   }

   public void set (Point3d pnt, Vector3d nrm) {
      this.pnt.set (pnt);
      this.nrm.set (nrm);
   }

   public void setMu (double mu) {
      this.mu = mu;
   }

   public double getMu() {
      return mu;
   }

   public void setNormalVelocityLimit (double v) {
      normalVelocityLimit = v;
   }

   public double getNormalVelocityLimit() {
      return normalVelocityLimit;
   }

   public void set (ContactInfo c) {
      this.pnt.set (c.pnt);
      this.nrm.set (c.nrm);
      mu = c.mu;
      normalVelocityLimit = c.normalVelocityLimit;
      otherBodyVelocity.set (c.otherBodyVelocity);
   }

   // public double dotVelocity (Twist vel)
   // {
   // Vector3d w = vel.w;

   // double vx = w.y*pnt1.z - w.z*pnt1.y + vel.v.x;
   // double vy = w.z*pnt1.x - w.x*pnt1.z + vel.v.y;
   // double vz = w.x*pnt1.y - w.y*pnt1.x + vel.v.z;
   // return nrml.x*vx + nrml.y*vy + nrml.z*vz;
   // }

   public String toString() {
      return toString ("%g");
   }

   public String toString (String fmtStr) {
      return toString (new NumberFormat (fmtStr));
   }

   public String toString (NumberFormat fmt) {
      return (pnt.toString (fmt) + "\n" + nrm.toString (fmt) + "\n" +
              otherBodyVelocity.toString (fmt) + "\n" + fmt.format (mu) + "  " +
              fmt.format (normalVelocityLimit));
   }
}
