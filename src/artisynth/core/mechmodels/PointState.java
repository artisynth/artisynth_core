/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import artisynth.core.modelbase.*;
import artisynth.core.util.ScalableUnits;
import artisynth.core.util.TransformableGeometry;
import maspack.matrix.*;

import java.io.*;

public class PointState implements TransformableGeometry,
ScalableUnits {
   Point3d pos;
   Vector3d vel;

   public PointState() {
      pos = new Point3d();
      vel = new Vector3d();
   }


   public void set (PointState pstate) {
      pos.set (pstate.pos);
      vel.set (pstate.vel);
   }

   public int set (VectorNd x, int idx) {
      double[] buf = x.getBuffer();
      pos.x = buf[idx++];
      pos.y = buf[idx++];
      pos.z = buf[idx++];
      vel.x = buf[idx++];
      vel.y = buf[idx++];
      vel.z = buf[idx++];
      return idx;
   }

   public int get (VectorNd x, int idx) {
      double[] buf = x.getBuffer();
      buf[idx++] = pos.x;
      buf[idx++] = pos.y;
      buf[idx++] = pos.z;
      buf[idx++] = vel.x;
      buf[idx++] = vel.y;
      buf[idx++] = vel.z;
      return idx;
   }

   public int getPos (double[] buf, int idx) {
      buf[idx++] = pos.x;
      buf[idx++] = pos.y;
      buf[idx++] = pos.z;
      return idx;
   }

   public int getVel (double[] buf, int idx) {
      buf[idx++] = vel.x;
      buf[idx++] = vel.y;
      buf[idx++] = vel.z;
      return idx;
   }

   public int setPos (double[] buf, int idx) {
      pos.x = buf[idx++];
      pos.y = buf[idx++];
      pos.z = buf[idx++];
      return idx;
   }
   
   public void setPos(Point3d pnt) {
      pos.set(pnt);
   }

   public int setVel (double[] buf, int idx) {
      vel.x = buf[idx++];
      vel.y = buf[idx++];
      vel.z = buf[idx++];
      return idx;
   }
   
   public void setVel(Vector3d v) {
      vel.set(v);
   }
   
   public void addPos(Point3d p) {
      pos.add(p);
   }
   
   public void scaledAddPos(double s, Point3d p) {
      pos.scaledAdd(s,  p);
   }
   
   public void addVel(Vector3d v) {
      vel.add(v);
   }
   
   public void scaledAddVel(double s, Vector3d v) {
      vel.scaledAdd(s, v);
   }

   public void scaleDistance (double s) {
      pos.scale (s);
      vel.scale (s);
   }

   public void scaleMass (double s) {
   }

   public void transformGeometry (AffineTransform3dBase X) {
      transformGeometry (X, this, 0);
   }

   public void transformGeometry (
      AffineTransform3dBase X, TransformableGeometry topObject, int flags) {
      // should we transform vel too ???
      pos.transform (X);
   }

   public boolean equals (ComponentState state) {
      if (state instanceof PointState) {
         PointState otherState = (PointState)state;
         return (pos.equals (otherState.pos) || 
                 vel.equals (otherState.vel));
      }
      else {
         return false;
      }
   }   
   
   public Point3d getPos() {
      return pos;
   }
   
   public Vector3d getVel() {
      return vel;
   }

}
